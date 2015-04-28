package com.caucho.lucene;

import com.caucho.vfs.Vfs;
import io.baratine.core.Modify;
import io.baratine.core.OnDestroy;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFile;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LuceneIndexImpl implements LuceneIndex
{
  private static Logger log
    = Logger.getLogger(LuceneIndexImpl.class.getName());

  private ServiceManager _manager;

  private Directory _directory;
  private IndexWriter _writer;
  private DirectoryReader _reader;
  private IndexSearcher _searcher;
  private QueryParser _queryParser;
  private String _address;

  private String _indexDirectory;

  public LuceneIndexImpl(String address, String indexDirectory)
    throws IOException
  {
    _address = address;
    _manager = Services.getCurrentManager();
    _indexDirectory = indexDirectory;

    log.finer("creating new " + this);
  }

  @Modify
  @Override
  public void indexFile(final String path, Result<Boolean> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexFile('%s')", path));

    BfsFile file = _manager.lookup(path).as(BfsFile.class);

    Field id = new StringField(_address, path, Field.Store.YES);

    Term key = new Term(_address, path);

    file.openRead(result.from(i -> indexStream(i, id, key)));
  }

  public boolean indexStream(InputStream in, Field id, Term key)
  {
    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(id);

      AutoDetectParser parser = new AutoDetectParser();
      Metadata metadata = new Metadata();
      LuceneContentHandler handler = new LuceneContentHandler(document);

      parser.parse(in, handler, metadata);

      for (String name : metadata.names()) {
        for (String value : metadata.getValues(name)) {
          document.add(new TextField(name, value, Field.Store.NO));
        }
      }

      writer.updateDocument(key, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%s') complete",
                                id.stringValue()));

      return true;
    } catch (IOException | SAXException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", id.stringValue()), e);
      throw new LuceneException(e);
    } catch (TikaException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", id.stringValue()), e);

      LuceneException le = LuceneException.create(e);

      throw le;
    }
  }

  private IndexWriter getIndexWriter() throws IOException
  {
    if (_writer != null)
      return _writer;

    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

    _writer = new IndexWriter(getDirectory(), iwc);

    return _writer;
  }

  private Directory getDirectory() throws IOException
  {
    if (_directory == null)
      _directory = createDirectory();

    return _directory;
  }

  private Directory createDirectory() throws IOException
  {
    return MMapDirectory.open(getPath());
  }

  private Path getPath()
  {
    return FileSystems.getDefault().getPath(_indexDirectory,
                                            "index",
                                            _address);
  }

  @Override
  @Modify
  public void indexText(String id, String text, Result<Boolean> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexText('%s')", id));

    Field idField = new StringField(_address, id, Field.Store.YES);

    Term key = new Term(_address, id);

    ByteArrayInputStream stream
      = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

    result.complete(indexStream(stream, idField, key));
  }

  @Override
  @Modify
  public void indexMap(String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    if (map.isEmpty()) {
      result.complete(true);

      log.fine(String.format("indexMap('%s') empty map", id));

      return;
    }

    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexMap('%1$s') %2$s", id, map));

    Field idField = new StringField(_address, id, Field.Store.YES);

    Term key = new Term(_address, id);

    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(idField);

      for (Map.Entry<String,Object> entry : map.entrySet()) {
        document.add(makeIndexableField(entry.getKey(), entry.getValue()));
      }

      writer.updateDocument(key, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%s') complete", id));

      result.complete(true);
    } catch (IOException e) {
      log.log(Level.WARNING, String.format("indexing ('%s') failed", id), e);
      throw new LuceneException(e);
    }
  }

  private IndexableField makeIndexableField(String name, Object obj)
  {
    IndexableField field = null;

    if (name == null) {
    }
    else if (obj == null) {
    }
    else {
      field = new TextField(name, String.valueOf(obj), Field.Store.NO);
    }

    return field;
  }

  @Override
  public void search(String query, int limit, Result<LuceneEntry[]> result)
  {
    Objects.requireNonNull(query);

    if (log.isLoggable(Level.FINER))
      log.finer(String.format("search('%s')", query));

    searchAfter(query, null, limit, result);
  }

  @Override
  public void searchAfter(String query,
                          LuceneEntry afterEntry,
                          int limit,
                          Result<LuceneEntry[]> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(
        String.format("search('%1$s', %2$s, %3$d)",
                      query,
                      afterEntry,
                      limit));

    try {
      IndexWriter writer = getIndexWriter();

      if (writer.hasUncommittedChanges())
        writer.commit();

      List<LuceneEntry> temp = new ArrayList<>();

      IndexSearcher searcher = getIndexSearcher();

      Query q = getQueryParser().parse(query);

      TopDocs docs;
      if (afterEntry != null) {
        ScoreDoc after = new ScoreDoc(afterEntry.getId(),
                                      afterEntry.getScore());

        docs = searcher.searchAfter(after, q, limit);
      }
      else {
        docs = searcher.search(q, null, limit);
      }

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(_address));

        LuceneEntry entry = new LuceneEntry(doc.doc,
                                            doc.score,
                                            d.get(_address));

        temp.add(entry);
      }

      if (log.isLoggable(Level.FINER))
        log.finer(
          String.format("search('%1$s', %2$s, %3$d with %4$d results)",
                        query,
                        afterEntry,
                        limit,
                        temp.size()));

      LuceneEntry[] entries
        = temp.toArray(new LuceneEntry[temp.size()]);

      result.complete(entries);
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);

      throw LuceneException.create(t);
    }
  }

  @Override
  @Modify
  public void delete(final String id, Result<Boolean> result)
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%s')", id));

      IndexWriter writer = getIndexWriter();

      Term idTerm = new Term(_address, id);
      writer.deleteDocuments(idTerm);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%s') complete",
                                id));
      result.complete(true);
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw new LuceneException(e);
    }
  }

  private IndexSearcher getIndexSearcher() throws IOException
  {
    DirectoryReader newReader = null;
    IndexWriter writer = getIndexWriter();

    if (_reader != null)
      newReader = DirectoryReader.openIfChanged(_reader, writer, true);

    if (newReader == null)
      newReader = DirectoryReader.open(writer, true);

    IndexSearcher newSearcher = _searcher;

    if (newReader != _reader) {
      newSearcher = new IndexSearcher(newReader);
    }

    _reader = newReader;
    _searcher = newSearcher;

    return _searcher;
  }

  private QueryParser getQueryParser()
  {
    if (_queryParser != null)
      return _queryParser;

    Analyzer analyzer = new StandardAnalyzer();

    _queryParser = new QueryParser("text", analyzer);

    return _queryParser;
  }

  @OnSave
  protected void checkpoint() throws IOException
  {
    if (_writer != null)
      _writer.commit();
  }

  @OnDestroy
  public void destroy() throws Exception
  {
    log.info("destroying " + this);

    clear(Result.ignore());
  }

  public void clear(Result<Void> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("lucene-plugin#clear()"));

    _searcher = null;

    Exception exception = null;

    try {
      if (_reader != null) {
        _reader.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    } finally {
      _reader = null;
    }

    try {
      if (_writer != null) {
        _writer.rollback();
        _writer.close();
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    } finally {
      _writer = null;
    }

    try {
      if (_directory != null)
        _directory.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    } finally {
      _directory = null;
    }

    _reader = null;
    _writer = null;
    _searcher = null;

    File path = getPath().toFile();

    try {
      if (path.exists() && (!Vfs.lookup(path.getAbsolutePath()).removeAll()))
        result.fail(new IOException());
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    }

    if (log.isLoggable(Level.FINER) && exception == null)
      log.finer(String.format("clear complete"));

    if (exception != null) {
      throw new LuceneException(exception);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _manager + _address + ']';
  }
}

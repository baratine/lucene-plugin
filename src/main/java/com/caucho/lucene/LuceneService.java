package com.caucho.lucene;

import com.caucho.vfs.Vfs;
import io.baratine.core.Modify;
import io.baratine.core.OnDestroy;
import io.baratine.core.OnSave;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;
import io.baratine.files.BfsFile;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LuceneService
{
  private static Logger log
    = Logger.getLogger(LuceneService.class.getName());

  private ServiceManager _manager;

  private Directory _directory;
  private IndexWriter _writer;
  private DirectoryReader _reader;
  private IndexSearcher _searcher;
  private QueryParser _queryParser;
  private String _address;

  public LuceneService(String address, ServiceManager manager)
    throws IOException
  {
    _address = address;
    _manager = manager;

    log.finer("creating new " + this);
  }

  public LuceneEntry[] search(String query) throws ParseException, IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("lucene-plugin#search('%s')", query));

    List<LuceneEntry> result = new ArrayList<>();

    try {
      IndexSearcher searcher = getIndexSearcher();

      Query q = getQueryParser().parse(query);

      int docsLimit = Integer.MAX_VALUE;

      TopDocs docs = searcher.search(q, null, docsLimit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(_address));

        LuceneEntry rDoc = new LuceneEntry(doc.doc, doc.score, d.get(_address));

        result.add(rDoc);
      }

      if (log.isLoggable(Level.FINER))
        log.finer(String.format(
          "lucene-plugin#search('%1$s') complete with %2$d results",
          query,
          result.size()));

      return result.toArray(new LuceneEntry[result.size()]);
    } catch (IOException | ParseException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public LuceneEntry[] searchAfter(String query,
                                   LuceneEntry afterEntry,
                                   int limit) throws IOException, ParseException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("lucene-plugin#search('%1$s', %2$s, %3$d )",
                              query,
                              afterEntry,
                              limit));

    List<LuceneEntry> result = new ArrayList<>();

    try {
      IndexReader reader = DirectoryReader.open(getDirectory());

      IndexSearcher searcher = new IndexSearcher(reader);

      Query q = getQueryParser().parse(query);

      ScoreDoc after = new ScoreDoc(afterEntry.getId(), afterEntry.getScore());

      TopDocs docs = searcher.searchAfter(after, q, limit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(_address));

        LuceneEntry rdoc = new LuceneEntry(doc.doc, doc.score, d.get(_address));

        result.add(rdoc);
      }

      if (log.isLoggable(Level.FINER))
        log.finer(String.format(
          "lucene-plugin#search('%1$s', %2$s, %3$d with %4$d results)",
          query,
          afterEntry,
          limit,
          result.size()));

      return result.toArray(new LuceneEntry[result.size()]);
    } catch (IOException | ParseException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  @Modify
  public boolean update(final String path)
    throws IOException, TikaException, SAXException
  {
    InputStream in = null;
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("lucene-plugin#update('%s')", path));

      _writer = getIndexWriter();

      Document document = new Document();

      Field bfsPath = new StringField(_address, path, Field.Store.YES);
      document.add(bfsPath);

      BfsFile file = _manager.lookup(path).as(BfsFile.class);

      AutoDetectParser parser = new AutoDetectParser();
      Metadata metadata = new Metadata();
      LuceneContentHandler handler = new LuceneContentHandler(document);

      parser.parse(in = file.openRead(), handler, metadata);

      for (String name : metadata.names()) {
        for (String value : metadata.getValues(name)) {
          document.add(new TextField(name, value, Field.Store.NO));
        }
      }

      _writer.addDocument(document);

      _writer.commit();

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("lucene-plugin#update('%s') complete",
                                path));

      return true;
    } catch (IOException | SAXException | TikaException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    } finally {
      if (in != null)
        in.close();
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

  private IndexSearcher getIndexSearcher() throws IOException
  {
    DirectoryReader newReader;
    IndexWriter writer = getIndexWriter();

    if (_reader != null)
      newReader = DirectoryReader.openIfChanged(_reader, writer, true);
    else
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

  @Modify
  public boolean delete(final String path) throws IOException
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("lucene-plugin#delete('%s')", path));

      IndexWriter writer = getIndexWriter();

      Term bfsPath = new Term(_address, path);
      writer.deleteDocuments(bfsPath);

      writer.commit();

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("lucene-plugin#delete('%s') complete",
                                path));

      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  @OnDestroy
  public void destroy() throws Exception
  {
    log.info("destroying " + this);

    new File(("/tmp/xxx" + new Date()).replace(":", "-")).createNewFile();

    clear();
  }

  public void clear() throws Exception
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

    if (path.exists() && (!Vfs.lookup(path.getAbsolutePath()).removeAll()))
      throw new IOException();

    if (log.isLoggable(Level.FINER) && exception != null)
      log.finer(String.format("lucene-plugin#clear() complete"));

    if (exception != null)
      throw exception;
  }

  private Directory createDirectory() throws IOException
  {
    return FSDirectory.open(getPath());
  }

  private Path getPath()
  {
    return FileSystems.getDefault().getPath("/tmp",
                                            "bfs-lucene-index",
                                            _address);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _manager + _address + ']';
  }
}

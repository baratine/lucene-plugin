package com.caucho.lucene;

import com.caucho.env.system.RootDirectorySystem;
import com.caucho.vfs.Vfs;
import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFileSync;
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
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Startup
//@ApplicationScoped
public class LuceneIndexBean
{
  private static final String exteralKey = "__extKey__";
  private static final String collectionKey = "__collectionKey__";

  private static Logger log = Logger.getLogger(LuceneIndexBean.class.getName());

  private Directory _directory;
  private IndexWriter _writer;
  private DirectoryReader _reader;
  private IndexSearcher _searcher;

  private AutoDetectParser _parser;

  private String _indexDirectory;

  private final static LuceneIndexBean bean = new LuceneIndexBean();

  @Inject
  ServiceManager _manager;
  private StandardAnalyzer _analyzer;

  public LuceneIndexBean()
  {
    init();
    _parser = new AutoDetectParser();
  }

  public final static LuceneIndexBean getInstance()
  {
    return bean;
  }

  private ServiceManager getManager()
  {
    if (_manager == null)
      _manager = Services.getCurrentManager();

    return _manager;
  }

  @PostConstruct
  public void init()
  {
    String baratineData
      = RootDirectorySystem.getCurrentDataDirectory().getFullPath();

    _indexDirectory = baratineData + File.separatorChar + "lucene";

    log.finer("creating new " + this);
  }

  private Term createPK(String collection, String externalId)
  {
    return new Term("__primary_key__", collection + ':' + externalId);
  }

  private StringField createIdField(String collection, String externalId)
  {
    return new StringField("__primary_key__",
                           collection + ':' + externalId,
                           Field.Store.YES);
  }

  public boolean indexFile(final String collection,
                           final String path)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexFile('%s')", path));

    BfsFileSync file = getManager().lookup(path).as(BfsFileSync.class);

    Field id = new StringField(exteralKey, path, Field.Store.YES);

    StringField idField = createIdField(collection, path);

    Term pk = createPK(collection, path);

    try (InputStream in = file.openRead()) {
      indexStream(in, id, idField, pk);

      return true;
    } catch (IOException e) {
      String message = String.format("failed to index file %1$s due to %2$s",
                                     file.toString(),
                                     e.toString());
      log.log(Level.FINER, message, e);

      throw LuceneException.create(e);
    }
  }

  public boolean indexStream(InputStream in,
                             Field id,
                             Field idField,
                             Term pk)
  {
    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(id);
      document.add(idField);

      Metadata metadata = new Metadata();
      LuceneContentHandler handler = new LuceneContentHandler(document);

      _parser.parse(in, handler, metadata);

      for (String name : metadata.names()) {
        for (String value : metadata.getValues(name)) {
          document.add(new TextField(name, value, Field.Store.NO));
        }
      }

      writer.updateDocument(pk, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%1$s')->('%2$s') complete",
                                pk, id.stringValue()));

      return true;
    } catch (IOException | SAXException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", id.stringValue()), e);
      throw LuceneException.create(e);
    } catch (TikaException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", id.stringValue()), e);

      LuceneException le = LuceneException.create(e);

      throw le;
    }
  }

  public boolean indexText(String collection,
                           String externalId,
                           String text)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexText('%s')", externalId));

    Field id = new StringField(exteralKey, externalId, Field.Store.YES);

    Field idField = createIdField(collection, externalId);

    Term pk = createPK(collection, externalId);

    ByteArrayInputStream stream
      = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

    boolean result = indexStream(stream, id, idField, pk);

    return result;
  }

  public boolean indexMap(String collection,
                          String externalKey,
                          Map<String,Object> map) throws LuceneException
  {
    if (map.isEmpty()) {
      log.fine(String.format("indexMap('%s') empty map", externalKey));

      return true;
    }

    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexMap('%1$s') %2$s", externalKey, map));

    Field id = new StringField(exteralKey, externalKey, Field.Store.YES);
    Field idField = createIdField(collection, externalKey);

    Term pk = createPK(collection, externalKey);

    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(id);
      document.add(idField);

      for (Map.Entry<String,Object> entry : map.entrySet()) {
        document.add(makeIndexableField(entry.getKey(), entry.getValue()));
      }

      writer.updateDocument(pk, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%s') complete", id));

      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, String.format("indexing ('%s') failed", id), e);

      throw LuceneException.create(e);
    }
  }

  public LuceneEntry[] search(String collection,
                              String query,
                              int limit)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(
        String.format("search('%1$s', %2$s, %3$d)",
                      query,
                      null,
                      limit));

    try {
      IndexSearcher searcher = getIndexSearcher();

      Query q = getQueryParser().parse(query);

      TopDocs docs = searcher.search(q, limit);

      ScoreDoc[] scoreDocs = docs.scoreDocs;

      LuceneEntry[] results = new LuceneEntry[scoreDocs.length];

      for (int i = 0; i < scoreDocs.length; i++) {
        ScoreDoc doc = scoreDocs[i];
        Document d = searcher.doc(doc.doc, Collections.singleton(exteralKey));

        LuceneEntry entry = new LuceneEntry(doc.doc,
                                            doc.score,
                                            d.get(exteralKey));

        results[i] = entry;
      }

      if (log.isLoggable(Level.FINER))
        log.finer(
          String.format("search('%1$s', %2$d with %3$d results)",
                        query,
                        limit,
                        results.length));

      return results;
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);

      throw LuceneException.create(t);
    }
  }

  public boolean delete(String collection, final String externalId)
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%s')", externalId));

      IndexWriter writer = getIndexWriter();

      Term pk = createPK(collection, externalId);

      writer.deleteDocuments(pk);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%1$s'->'%2$s') complete",
                                pk, externalId));
      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw LuceneException.create(e);
    }
  }

  public void commit() throws IOException
  {
    if (_writer != null && _writer.hasUncommittedChanges()) {
      _writer.commit();
    }
  }

  public boolean clear(String collection)
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
        throw new IOException();
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    }

    if (log.isLoggable(Level.FINER) && exception == null)
      log.finer(String.format("clear complete"));

    if (exception != null) {
      throw LuceneException.create(exception);
    }

    return true;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _manager + ']';
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

  private IndexWriter getIndexWriter() throws IOException
  {
    if (_writer != null)
      return _writer;

    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

    iwc.setMaxBufferedDocs(16);

    iwc.setRAMBufferSizeMB(64);

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
    Directory directory = MMapDirectory.open(getPath());

    directory = new NRTCachingDirectory(directory, 4.0, 16.0);

    return directory;
  }

  private Path getPath()
  {
    return FileSystems.getDefault().getPath(_indexDirectory,
                                            "index");
  }

  private QueryParser getQueryParser()
  {
    if (_analyzer == null)
      _analyzer = new StandardAnalyzer();

    QueryParser queryParser = new QueryParser("text", _analyzer);

    return queryParser;
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
}

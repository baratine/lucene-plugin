package com.caucho.lucene;

import com.caucho.vfs.Vfs;
import io.baratine.core.OnCheckpoint;
import io.baratine.core.OnDestroy;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("public:///lucene")
public class LuceneService
{
  public static final String bfsId = "bfspath";

  public Logger _logger = Logger.getLogger(LuceneService.class.getName());

  @Inject ServiceManager _manager;

  private Directory _directory;
  private IndexWriter _writer;
  private DirectoryReader _reader;
  private IndexSearcher _searcher;
  private QueryParser _queryParser;

  public LuceneService() throws IOException
  {
    _directory = createDirectory();
  }

  public LuceneEntry[] search(String query) throws ParseException, IOException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#search('%s')", query));

    List<LuceneEntry> result = new ArrayList<>();

    try {
      IndexSearcher searcher = getIndexSearcher();

      Query q = getQueryParser().parse(query);

      int docsLimit = Integer.MAX_VALUE;

      TopDocs docs = searcher.search(q, null, docsLimit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(bfsId));

        LuceneEntry rDoc = new LuceneEntry(doc.doc, doc.score, d.get(bfsId));

        result.add(rDoc);
      }

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format(
          "lucene-plugin#search('%1$s') complete with %2$d results",
          query,
          result.size()));

      return result.toArray(new LuceneEntry[result.size()]);
    } catch (IOException | ParseException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public LuceneEntry[] searchAfter(String query,
                                   LuceneEntry afterEntry,
                                   int limit) throws IOException, ParseException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#search('%1$s', %2$s, %3$d )",
                                  query,
                                  afterEntry,
                                  limit));

    List<LuceneEntry> result = new ArrayList<>();

    try {
      IndexReader reader = DirectoryReader.open(_directory);

      IndexSearcher searcher = new IndexSearcher(reader);

      Query q = getQueryParser().parse(query);

      ScoreDoc after = new ScoreDoc(afterEntry.getId(), afterEntry.getScore());

      TopDocs docs = searcher.searchAfter(after, q, limit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(bfsId));

        LuceneEntry rdoc = new LuceneEntry(doc.doc, doc.score, d.get(bfsId));

        result.add(rdoc);
      }

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format(
          "lucene-plugin#search('%1$s', %2$s, %3$d with %4$d results)",
          query,
          afterEntry,
          limit,
          result.size()));

      return result.toArray(new LuceneEntry[result.size()]);
    } catch (IOException | ParseException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public boolean update(final String path)
    throws IOException, TikaException, SAXException
  {
    try {
      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#update('%s')", path));

      _writer = getIndexWriter();

      Document document = new Document();

      Field bfsPath = new StringField(bfsId, path, Field.Store.YES);
      document.add(bfsPath);

      FileService file = _manager.lookup(path).as(FileService.class);

      AutoDetectParser parser = new AutoDetectParser();
      Metadata metadata = new Metadata();
      LuceneContentHandler handler = new LuceneContentHandler(document);

      parser.parse(file.openRead(), handler, metadata);

      for (String name : metadata.names()) {
        for (String value : metadata.getValues(name)) {
          document.add(new TextField(name, value, Field.Store.NO));
        }
      }

      _writer.addDocument(document);

      _writer.commit();

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#update('%s') complete",
                                    path));

      return true;
    } catch (IOException | SAXException | TikaException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  private IndexWriter getIndexWriter() throws IOException
  {
    if (_writer != null)
      return _writer;

    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer.getVersion(),
                                                  analyzer);

    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

    _writer = new IndexWriter(_directory, iwc);

    return _writer;
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

  @OnCheckpoint
  protected void checkpoint() throws IOException
  {
    if (_writer != null)
      _writer.commit();
  }

  public boolean delete(final String path) throws IOException
  {
    try {
      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#delete('%s')", path));

      IndexWriter writer = getIndexWriter();

      Term bfsPath = new Term(bfsId, path);
      writer.deleteDocuments(bfsPath);

      writer.commit();

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#delete('%s') complete",
                                    path));

      return true;
    } catch (IOException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  @OnDestroy
  public void destroy() throws IOException
  {
    clear();
  }

  public void clear() throws IOException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#clear()"));

    _searcher = null;

    Exception exception;

    try {
      if (_reader != null) {
        _reader.close();
      }
    } catch (Exception e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    } finally {
      _reader = null;
    }

    try {
      if (_writer != null) {
        _writer.commit();
        _writer.close();
      }
    } catch (IOException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      exception = e;
    } finally {
      _writer = null;
    }

    try {
      _directory.close();
    } catch (IOException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);
      exception = e;
    } finally {
      _directory = null;
    }

    _reader = null;
    _writer = null;
    _searcher = null;

    Vfs.lookup("/tmp/bfs-lucene-index").removeAll();

    _directory = createDirectory();

    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#clear() complete"));
  }

  private Directory createDirectory() throws IOException
  {
    //return MMapDirectory.open(new File("/tmp/bfs-lucene-index"));
    return FSDirectory.open(new File("/tmp/bfs-lucene-index"));
  }
}

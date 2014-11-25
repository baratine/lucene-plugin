package com.caucho.lucene;

import com.caucho.vfs.Vfs;
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

  Directory _directory;

  public LuceneService() throws IOException
  {
    _directory = createDirectory();
  }

  public RDoc[] search(String query) throws ParseException, IOException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#search('%s')", query));

    List<RDoc> result = new ArrayList<>();

    try {
      IndexReader reader = DirectoryReader.open(_directory);

      IndexSearcher searcher = new IndexSearcher(reader);
      Analyzer analyzer = new StandardAnalyzer();

      QueryParser parser = new QueryParser("text", analyzer);

      Query q = parser.parse(query);

      int docsLimit = Integer.MAX_VALUE;

      TopDocs docs = searcher.search(q, null, docsLimit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(bfsId));

        RDoc rDoc = new RDoc(doc.doc, doc.score, d.get(bfsId));

        result.add(rDoc);
      }

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format(
          "lucene-plugin#search('%1$s') complete with %2$d results",
          query,
          result.size()));

      return result.toArray(new RDoc[result.size()]);
    } catch (IOException | ParseException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public String[] searchInc(String query,
                            int offset,
                            int limit) throws IOException, ParseException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#search('%1$s', %2$d, %3$d )",
                                  query,
                                  offset,
                                  limit));

    List<String> result = new ArrayList<>();

    try {
      IndexReader reader = DirectoryReader.open(_directory);

      IndexSearcher searcher = new IndexSearcher(reader);
      Analyzer analyzer = new StandardAnalyzer();

      QueryParser parser = new QueryParser("text", analyzer);

      Query q = parser.parse(query);

      TopDocs docs = searcher.search(q, null, limit);

      for (ScoreDoc doc : docs.scoreDocs) {
        Document d = searcher.doc(doc.doc, Collections.singleton(bfsId));

        result.add(d.get(bfsId));
      }

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format(
          "lucene-plugin#search('%1$s', %2$d, %3$d with %4$d results)",
          query,
          offset,
          limit,
          result.size()));

      return result.toArray(new String[result.size()]);
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

      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer.getVersion(),
                                                    analyzer);

      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

      IndexWriter writer = new IndexWriter(_directory, iwc);

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

      writer.addDocument(document);

      writer.commit();

      writer.close();

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#update('%s') complete",
                                    path));

      return true;
    } catch (IOException | SAXException | TikaException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public boolean delete(final String path) throws IOException
  {
    try {
      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#delete('%s')", path));

      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer.getVersion(),
                                                    analyzer);

      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

      IndexWriter writer = new IndexWriter(_directory, iwc);

      Term bfsPath = new Term(bfsId, path);
      writer.deleteDocuments(bfsPath);

      writer.commit();

      writer.close();

      if (_logger.isLoggable(Level.FINER))
        _logger.finer(String.format("lucene-plugin#delete('%s') complete",
                                    path));

      return true;
    } catch (IOException e) {
      _logger.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  public void clear() throws IOException
  {
    if (_logger.isLoggable(Level.FINER))
      _logger.finer(String.format("lucene-plugin#clear()"));

    _directory.close();

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

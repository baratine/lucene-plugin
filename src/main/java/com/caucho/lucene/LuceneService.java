package com.caucho.lucene;

import io.baratine.core.Service;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("public:///lucene")
public class LuceneService
{
  public static final String bfsPath = "bfs.path";

  public Logger _logger = Logger.getLogger(LuceneService.class.getName());

  @Inject ServiceManager _manager;

  RAMDirectory _directory = new RAMDirectory();

  public String[] search(String query) throws IOException, ParseException
  {
    _logger.finer(String.format("lucene-plugin#search('%s')", query));

    IndexReader reader = DirectoryReader.open(_directory);

    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();

    QueryParser parser = new QueryParser("text", analyzer);

    Query q = parser.parse(query);

    int docsLimit = 10;

    TopDocs docs = searcher.search(q, null, docsLimit);

    List<String> result = new ArrayList<>();
    for (ScoreDoc doc : docs.scoreDocs) {
      Document d = searcher.doc(doc.doc, Collections.singleton("bfs.path"));

      result.add(d.get(bfsPath));
    }

    return result.toArray(new String[result.size()]);
  }

  public boolean update(String path) throws IOException
  {
    try {
      _logger.finer(String.format("lucene-plugin#update('%s')", path));

      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer.getVersion(),
                                                    analyzer);

      iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

      IndexWriter writer = new IndexWriter(_directory, iwc);

      Document document = new Document();

      Field bfsPath = new StoredField("bfs.path", path);
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

      writer.close();

    } catch (Exception e) {
      _logger.log(Level.WARNING, e.getMessage(), e);
    }

    return true;
  }

  public void clear()
  {
    _directory = new RAMDirectory();
  }
}

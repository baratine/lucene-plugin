package tests;

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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Ignore
public class Tz00
{
  Directory _dir;

  @Before
  public void setup() throws IOException
  {
    Directory dir = new RAMDirectory();
    _dir = dir;
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer.getVersion(),
                                                  analyzer);

    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

    IndexWriter writer = new IndexWriter(dir, iwc);

    Document doc = new Document();

    Field pathField = new StringField("id", "#1", Field.Store.YES);
    doc.add(pathField);
    doc.add(new TextField("text", "Merry had a little lamb", Field.Store.YES));

    writer.addDocument(doc);

    writer.close();
  }

  public void test() throws IOException, ParseException
  {
    IndexReader reader = DirectoryReader.open(_dir);
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();

    QueryParser parser = new QueryParser("text", analyzer);

    Query query = parser.parse("text:lamb (date > 0)");

    BooleanQuery bq = (BooleanQuery) query;

    List<BooleanClause> clauses = bq.clauses();

    int i = 0;
    for (BooleanClause clause : clauses) {
      System.out.println("Tz00.test["
                         + i
                         + "]: "
                         + clause
                         + " "
                         + clause.getClass());
      System.out.println("Tz00.test[" + i + "]: " + clause.getQuery());

      i++;
    }

    System.out.println("Tz00.test: " + query.getClass());

    TopDocs docs = searcher.search(query, null, 10);
    for (ScoreDoc doc : docs.scoreDocs) {
      Document d = searcher.doc(doc.doc, Collections.singleton("id"));

      System.out.println(d.get("id"));
    }
  }

  @After
  public void tearDown()
  {

  }
}

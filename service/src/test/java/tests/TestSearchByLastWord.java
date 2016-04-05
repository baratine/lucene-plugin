package tests;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * title: test pdf, ms-word, txt, xml, json with last word
 */

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class,
              LuceneFacadeImpl.class, SearcherUpdateServiceImpl.class}
)
public class TestSearchByLastWord extends Base
{
  @Test(timeout = 2000)
  public void testText()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.txt");
  }

  private void test(String fileName)
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] result = uploadAndSearch(fileName, "versions");
    Assert.assertEquals(1, result.length);
    Assert.assertEquals(makeBfsPath(fileName), result[0].getExternalId());
  }

  @Test(timeout = 2000)
  public void testXml()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.xml");
  }

  @Test(timeout = 2000)
  public void testJson()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.json");
  }

  @Test(timeout = 5000)
  public void testPdf()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.pdf");
  }

  @Test(timeout = 5000)
  public void testWord()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.docx");
  }
}


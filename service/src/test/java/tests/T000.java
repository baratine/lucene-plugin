package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneScheme;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * title: test pdf, ms-word, txt, xml, json with first word
 */

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneScheme.class},
  logs = {@Log(name = "com.caucho", level = "FINER")},
  testTime = 0)
public class T000 extends BaseTest
{
  @Test(timeout = 2000)
  public void testText()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.txt");
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

  private void test(String fileName)
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] result = uploadAndSearch(fileName, "Lorem");
    Assert.assertEquals(1, result.length);
    Assert.assertEquals(makeBfsPath(fileName), result[0].getExternalId());
  }
}

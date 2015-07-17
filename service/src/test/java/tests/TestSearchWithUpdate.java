package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * title: tests text update
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class TestSearchWithUpdate extends BaseTest
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    update("0", "mary had a little lamb");
    update("0", "mary had a little dog");

    LuceneEntry[] result = search("dog");

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("0", result[0].getExternalId());

    result = search("lamb");

    Assert.assertEquals(0, result.length);
  }
}

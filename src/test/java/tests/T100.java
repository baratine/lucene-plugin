package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(testTime = 0,
  logs = {@ConfigurationBaratine.Log(name = "com.caucho",
    level = "FINER")})
public class T100 extends BaseTest
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    update("0", "mary had a little lamb");
    update("0", "mary had a little dog");
    update("1", "mary had a little lamb");

    LuceneEntry[] result = search("lamb");

    for (LuceneEntry luceneEntry : result) {

      System.out.println("T100.test " + luceneEntry);
    }

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("1", result[0].getExternalId());
  }
}

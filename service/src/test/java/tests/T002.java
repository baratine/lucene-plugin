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
 * title: test delete
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(testTime = 0,
  services = {LuceneScheme.class},
  logs = {@Log(name = "com.caucho", level = "FINER")})
public class T002 extends BaseTest
{
  @Test(timeout = 2000)
  public void testDelete()
    throws InterruptedException, IOException, ExecutionException
  {
    test("test-00.txt");
  }

  private void test(String fileName)
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] result = uploadAndSearch(fileName, "Lorem");
    Assert.assertEquals(1, result.length);
    Assert.assertEquals(makeBfsPath(fileName), result[0].getExternalId());

    delete(fileName);

    result = search("Lorem");

    Assert.assertEquals(0, result.length);
  }
}


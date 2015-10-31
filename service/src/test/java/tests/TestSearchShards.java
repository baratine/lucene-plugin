package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
import com.caucho.v5.bartender.pod.PodBartender;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * title: tests text
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class,SearcherUpdateServiceImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene", podType = PodBartender.PodType.triad)
public class TestSearchShards extends Base
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    update("0", "mary had a little lamb");
    update("0", "mary had a little dog");
    update("1", "mary had a little lamb");

    LuceneEntry[] result = search("lamb");

    System.out.println("TestSearchShards.test " + Arrays.asList(result));

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("1", result[0].getExternalId());
  }
}

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
 * title: tests collection
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class T400 extends BaseTest
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    update("foo", "foo", "mary had a little lamb");
    update("bar", "foo", "mary had a little lamb");

    LuceneEntry[] fooResult = search("foo", "lamb");
    LuceneEntry[] barResult = search("bar", "lamb");

    Assert.assertEquals(1, fooResult.length);
    Assert.assertEquals(1, barResult.length);

    Assert.assertEquals("foo", fooResult[0].getExternalId());
    Assert.assertEquals("foo", barResult[0].getExternalId());

    this.clear("bar");

    fooResult = search("foo", "lamb");
    barResult = search("bar", "lamb");

    Assert.assertEquals(1, fooResult.length);
    Assert.assertEquals(0, barResult.length);

    Assert.assertEquals("foo", fooResult[0].getExternalId());
  }
}

package tests;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.junit.ServiceTest;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * title: tests collection
 */
@RunWith(RunnerBaratine.class)
@ServiceTest(LuceneWriterImpl.class)
@ServiceTest(LuceneReaderImpl.class)
@ServiceTest(LuceneFacadeImpl.class)
@ServiceTest(SearcherUpdateServiceImpl.class)
public class TestSearchCollection extends Base
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

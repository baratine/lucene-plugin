package tests;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.caucho.junit.ConfigurationBaratine2;
import com.caucho.junit.RunnerBaratine2;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * title: tests text
 */
@RunWith(RunnerBaratine2.class)
@ConfigurationBaratine2(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class,
              LuceneFacadeImpl.class, SearcherUpdateServiceImpl.class})
public class TestSearchText extends Base
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    update("0", "mary had a little lamb");
    update("0", "mary had a little dog");
    update("1", "mary had a little lamb");

    LuceneEntry[] result = search("lamb");

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("1", result[0].getExternalId());
  }
}

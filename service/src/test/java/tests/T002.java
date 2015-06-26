package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * title: test delete
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(testTime = 0,
                       services = {LuceneWriterImpl.class, LuceneReaderImpl.class},
                       logLevel = "FINER",
                       logs = {@Log(name = "com.caucho", level = "FINER")},
                       pod = "lucene")
public class T002 extends BaseTest
{
  @Test(timeout = 2000)
  public void testFile()
    throws InterruptedException, IOException, ExecutionException
  {
    String fileName = "test-00.txt";
    LuceneEntry[] result = uploadAndSearch(fileName, "Lorem");
    Assert.assertEquals(1, result.length);
    Assert.assertEquals(makeBfsPath(fileName), result[0].getExternalId());

    deleteFile(fileName);

    result = search("Lorem");

    Assert.assertEquals(0, result.length);
  }

  @Test(timeout = 2000)
  public void testText()
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] result
      = updateAndSearchText("foo", "mary had a little lamb", "lamb");

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("foo", result[0].getExternalId());

    delete("foo");

    result = search("lamb");

    Assert.assertEquals(0, result.length);
  }

  @Test(timeout = 2000)
  public void testMap()
    throws InterruptedException, IOException, ExecutionException
  {
    Map<String,Object> map = new HashMap<>();
    map.put("data", "mary had a little lamb");

    LuceneEntry[] result = updateAndSearchMap("foo", map, "data:lamb");

    Assert.assertEquals(1, result.length);
    Assert.assertEquals("foo", result[0].getExternalId());

    delete("foo");

    result = search("data:lamb");

    Assert.assertEquals(0, result.length);
  }
}


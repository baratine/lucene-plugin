package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * title: tests map indexing
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class,SearcherUpdateServiceImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class TestSearchMap extends Base
{
  @Test
  public void test()
    throws InterruptedException, ExecutionException, IOException
  {
    Map<String,Object> map = new HashMap<>();

    map.put("foo", "mary had a little lamb");
    map.put("bar", "mary had two little lamb");
    map.put("zoo", "rose had three little lamb");

    map.put("age", 23);
    map.put("count", 32);

    update("map", map);

    LuceneEntry[] result = search("foo:lamb");
    Assert.assertEquals(1, result.length);

    result = search("bar:two");
    Assert.assertEquals(1, result.length);

    result = search("age:[23 TO 23]");
    Assert.assertEquals(1, result.length);

    result = search("count:32");
    Assert.assertEquals(1, result.length);

    result = search("count:[33 TO 34]");
    Assert.assertEquals(0, result.length);
  }
}

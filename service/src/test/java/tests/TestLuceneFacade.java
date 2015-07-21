package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneFacadeSync;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import io.baratine.core.Lookup;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * title: tests LuceneFacade methods
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(
  services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class TestLuceneFacade
{
  @Inject
  @Lookup("public://lucene/service")
  LuceneFacadeSync _lucene;

  @Test
  public void testText()
  {
    _lucene.indexText("foo", "foo", "mary had a little lamb");

    List<LuceneEntry> result = _lucene.search("foo", "mary", 255);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals("foo", result.get(0).getExternalId());
  }

  @Test
  public void testDelete()
  {
    _lucene.indexText("foo", "foo", "mary had a little lamb");
    _lucene.delete("foo", "foo");

    List<LuceneEntry> result = _lucene.search("foo", "mary", 255);

    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testMap()
  {
    Map<String,Object> map = new HashMap<>();

    map.put("foo", "mary had a little lamb");
    map.put("bar", "mary had two little lamb");
    map.put("zoo", "rose had three little lamb");

    map.put("age", 23);
    map.put("count", 32);

    _lucene.indexMap("foo", "map", map);

    List<LuceneEntry> result = _lucene.search("foo", "foo:lamb", 255);
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "bar:two", 255);
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "age:[23 TO 23]", 255);
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "count:32", 255);
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "count:[33 TO 34]", 255);
    Assert.assertEquals(0, result.size());
  }

  @Before
  public final void baseBefore()
  {
    clear();
  }

  final protected void clear()
  {
    _lucene.clear("foo");
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}

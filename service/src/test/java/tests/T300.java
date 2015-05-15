package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneIndexImpl;
import com.caucho.lucene.LuceneSessionImpl;
import com.caucho.lucene.LuceneWorkerImpl;
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
 * title: tests LuceneSession methods
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneWorkerImpl.class, LuceneIndexImpl.class, LuceneSessionImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class T300
{
  @Inject @Lookup("session://lucene/session/foo")
  LuceneSessionSync _lucene;

  @Test
  public void testText()
  {
    _lucene.indexText("foo", "foo", "mary had a little lamb");

    List<LuceneEntry> result = _lucene.search("foo", "mary");

    Assert.assertEquals(1, result.size());
    Assert.assertEquals("foo", result.get(0).getExternalId());
  }

  @Test
  public void testDelete()
  {
    _lucene.indexText("foo", "foo", "mary had a little lamb");
    _lucene.delete("foo", "foo");

    List<LuceneEntry> result = _lucene.search("foo", "mary");

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

    List<LuceneEntry> result = _lucene.search("foo", "foo:lamb");
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "bar:two");
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "age:[23 TO 23]");
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "count:32");
    Assert.assertEquals(1, result.size());

    result = _lucene.search("foo", "count:[33 TO 34]");
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

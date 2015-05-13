package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneIndexImpl;
import io.baratine.core.Lookup;
import io.baratine.stream.StreamBuilder;
import io.baratine.stream.SupplierSync;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * title:
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneIndexImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class T400
{
  public static final String DEFAULT = "default";

  @Inject @Lookup("/index")
  LuceneIndexSync _lucene;

  @Test
  public void testText()
  {
    _lucene.indexText(DEFAULT, "foo", "mary had a little lamb");

    StreamBuilder<String> streamBuilder = _lucene.search2(DEFAULT, "lamb");

    SupplierSync<List<String>> supplier = ArrayList::new;

    List<String> result
      = streamBuilder.collect(supplier, (l, e) -> (l).add(e),
                              (l, r) -> l.addAll(r));

    System.out.println("T400.testText " + result);
  }

  public static List<String> get()
  {
    return new ArrayList<>();
  }

  @Before
  public final void baseBefore()
  {
    clear();
  }

  final protected void clear()
  {
    _lucene.clear(DEFAULT);
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}

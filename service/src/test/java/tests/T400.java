package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneManagerImpl;
import io.baratine.core.Lookup;
import io.baratine.stream.StreamBuilder;
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
@ConfigurationBaratine(services = {LuceneManagerImpl.class},
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "FINER")},
  testTime = 0, pod = "lucene")
public class T400
{
  @Inject @Lookup("/lucene-manager/foo")
  LuceneIndexSync _lucene;

  @Test
  public void testText()
  {
    _lucene.indexText("foo", "mary had a little lamb");

    StreamBuilder streamBuilder = _lucene.search2("lamb");

    Object o = streamBuilder.collect(ArrayList<String>::new, (a, b) -> {
                                     },
                                     (a, b) -> {
                                     });

    System.out.println("T400.testText " + o);
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
    _lucene.clear();
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}

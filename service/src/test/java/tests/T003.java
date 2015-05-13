package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneIndexImpl;
import com.caucho.lucene.LuceneManagerImpl;
import io.baratine.files.BfsFileSync;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

/**
 * test: paging
 */
@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(testTime = 0,
  services = {LuceneIndexImpl.class},
  logs = {@Log(name = "com.caucho", level = "FINER")}, pod = "lucene")
public class T003 extends BaseTest
{
  @Before
  public void setUp()
    throws IOException, ExecutionException, InterruptedException
  {
    for (int i = 0; i < 11; i++) {
      String fileName = makeFileName(i);
      BfsFileSync file = lookup(fileName);
      try (OutputStream out = file.openWrite()) {
        if (i < 10)
          out.write("hit".getBytes());
        else
          out.write("miss".getBytes());
      }

      update(fileName);
    }
  }

  private String makeFileName(int k)
  {
    return String.format("bfs:///tmp/test-%d", k);
  }

  @Test
  public void testAll()
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] results = search("hit", new LuceneEntry(0), 100);
    Assert.assertEquals(10, results.length);
  }

  @Test
  public void testNext()
    throws InterruptedException, IOException, ExecutionException
  {
    LuceneEntry[] results = search("hit", new LuceneEntry(0), 100);
    Assert.assertEquals(10, results.length);

    results = search("hit", results[3], 3);
    Assert.assertEquals(3, results.length);

    Assert.assertEquals(4, results[0].getId());
    Assert.assertEquals(5, results[1].getId());
    Assert.assertEquals(6, results[2].getId());

    results = search("hit", results[2], 3);
    Assert.assertEquals(3, results.length);

    results = search("hit", results[2], 3);
    Assert.assertEquals(0, results.length);
  }
}


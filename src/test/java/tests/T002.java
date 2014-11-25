package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneService;
import com.caucho.lucene.RDoc;
import io.baratine.files.FileService;
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
@ConfigurationBaratine(services = {LuceneService.class},
                       testTime = 0,
                       logs = {@Log(name = "com.caucho",
                                    level = "FINER")})
public class T002 extends BaseTest
{
  @Before
  public void setUp()
    throws IOException, ExecutionException, InterruptedException
  {
    clear();

    for (int i = 0; i < 11; i++) {
      String fileName = makeFileName(i);
      FileService file = _serviceManager.lookup(fileName).as(FileService.class);
      try (OutputStream out = file.openWrite()) {
        if (i < 10)
          out.write("hit".getBytes());
        else
          out.write("miss".getBytes());
      }

      update(fileName);
    }

    Thread.sleep(100);
  }

  private String makeFileName(int k)
  {
    return String.format("bfs:///tmp/test-%d", k);
  }

  @Test
  public void test()
    throws InterruptedException, IOException, ExecutionException
  {
    RDoc[] results = search("hit", new RDoc(0, 0), 9);
    Assert.assertEquals(10, results.length);
  }
}


package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneService;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * title: test update index
 */

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneService.class},
                       testTime = 0,
                       logs = {@Log(name = "com.caucho",
                                    level = "FINER")})
public class T003 extends BaseTest
{
  @Before
  public void setUp() throws ExecutionException, InterruptedException
  {
    clear();
  }

  @Test
  public void test()
    throws InterruptedException, IOException, ExecutionException
  {
    String[] files = new String[]{"test-00.txt", "test-00.pdf", "test-00.docx"};

    for (int i = 0; i < files.length; i++) {
      String file = files[i];

      LuceneEntry[] result = uploadAndSearch(file, "Lorem");

      Assert.assertEquals(i + 1, result.length);
      for (int j = 0; j < result.length; j++) {
        Assert.assertEquals(makeBfsPath(files[j]), result[j].getBfsPath());
      }
    }
  }
}


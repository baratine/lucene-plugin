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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

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
    List<String> files = Arrays.asList("test-00.txt",
                                       "test-00.pdf",
                                       "test-00.docx");

    for (int i = 0; i < files.size(); i++) {
      String file = files.get(i);

      LuceneEntry[] result = uploadAndSearch(file, "Lorem");

      Arrays.sort(result,
                  (a, b) -> files.indexOf(Stream.of(a.getBfsPath()
                                                     .split("/"))
                                                .reduce((c, d) -> d).get()) -
                            files.indexOf(Stream.of(b.getBfsPath()
                                                     .split("/"))
                                                .reduce((c, d) -> d).get()));

      Assert.assertEquals(i + 1, result.length);
      for (int j = 0; j < result.length; j++) {
        Assert.assertEquals(makeBfsPath(files.get(j)), result[j].getBfsPath());
      }
    }
  }
}


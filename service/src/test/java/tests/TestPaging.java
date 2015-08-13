package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeImpl;
import com.caucho.lucene.LuceneReaderImpl;
import com.caucho.lucene.LuceneWriterImpl;
import com.caucho.lucene.SearcherUpdateServiceImpl;
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
                       services = {LuceneWriterImpl.class, LuceneReaderImpl.class, LuceneFacadeImpl.class,SearcherUpdateServiceImpl.class},
                       logs = {@Log(name = "com.caucho", level = "FINER")},
                       pod = "lucene")
public class TestPaging extends Base
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
    LuceneEntry[] results = search("hit");
    Assert.assertEquals(10, results.length);
  }
}


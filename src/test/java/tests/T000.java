package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneService;
import com.caucho.lucene.LuceneServiceClient;
import com.caucho.lucene.Monitor;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneService.class},
                       testTime = 0,
                       logLevel = "OFF",
                       logs = {@Log(name = "com.caucho.lucene",
                                    level = "FINER")})
public class T000
{
  @Inject @Lookup ServiceManager _serviceManager;

  @Inject @Lookup("/lucene") LuceneServiceClient _lucene;

  @Test(timeout = 2000)
  public void testText() throws InterruptedException
  {
    test("test-00.txt");
  }

  @Test(timeout = 5000)
  public void testPdf() throws InterruptedException
  {
    test("test-00.pdf");
  }

  @Test(timeout = 5000)
  public void testWord() throws InterruptedException
  {
    test("test-00.docx");
  }

  private void test(String fileName) throws InterruptedException
  {
    List<String> result = uploadAndSearch(fileName, "Lorem");
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("bfs:///tmp/" + fileName, result.get(0));
  }

  private List<String> uploadAndSearch(String fileName, String query)
    throws InterruptedException
  {
    _lucene.clear(v->{});

    FileService file =
      _serviceManager.lookup("bfs:///tmp/" + fileName).as(FileService.class);

    try (OutputStream out = file.openWrite()) {
      Vfs.openRead("src/test/resources/" + fileName).writeToStream(out);
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Monitor monitor = new Monitor();
    _lucene.update(file.getStatus().getPath(), v -> {
      monitor.toComplete();
    });

    monitor.waitForComplete();

    List<String> documents = new ArrayList<>();

    monitor.toWaiting();
    _lucene.search(query, docs -> {
      for (String doc : docs) {
        documents.add(doc);
      }

      monitor.toComplete();
    });
    monitor.waitForComplete();

    return documents;
  }

}


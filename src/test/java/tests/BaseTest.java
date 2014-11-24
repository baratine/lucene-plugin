package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneService;
import com.caucho.lucene.LuceneServiceClient;
import com.caucho.lucene.Monitor;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LuceneService.class},
                       testTime = 0,
                       logs = {@Log(name = "com.caucho.lucene",
                                    level = "FINER")})
public abstract class BaseTest
{
  @Inject @Lookup
  protected ServiceManager _serviceManager;

  @Inject @Lookup("/lucene")
  protected LuceneServiceClient _lucene;

  final protected void delete(String fileName) throws InterruptedException
  {
    Monitor monitor = new Monitor();

    _lucene.delete(makeBfsPath(fileName), r -> {
      monitor.toComplete();
    });

    monitor.waitForComplete();
  }

  final protected FileService upload(String fileName)
    throws IOException, InterruptedException
  {
    FileService file =
      _serviceManager.lookup(makeBfsPath(fileName)).as(FileService.class);

    String localFile = "src/test/resources/" + fileName;
    try (OutputStream out = file.openWrite();
         ReadStream in = Vfs.openRead(localFile)) {
      in.writeToStream(out);
    }

    final Monitor monitor = new Monitor();
    _lucene.update(file.getStatus().getPath(), v -> {
      monitor.toComplete();
    });

    monitor.waitForComplete();

    return file;
  }

  final protected List<String> search(String query)
    throws IOException, InterruptedException
  {
    Monitor monitor = new Monitor();

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

  final protected List<String> uploadAndSearch(String fileName, String query)
    throws InterruptedException, IOException
  {
    upload(fileName);

    List<String> result = search(query);

    return result;
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
  }
}


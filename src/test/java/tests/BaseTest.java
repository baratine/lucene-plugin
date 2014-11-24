package tests;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.ConfigurationBaratine.Log;
import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneService;
import com.caucho.lucene.LuceneServiceClient;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

  final protected boolean delete(String fileName)
    throws InterruptedException, ExecutionException
  {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    _lucene.delete(makeBfsPath(fileName), r -> {
      future.complete(true);
    });

    return future.get();
  }

  final protected FileService upload(String fileName)
    throws IOException, InterruptedException, ExecutionException
  {
    FileService file =
      _serviceManager.lookup(makeBfsPath(fileName)).as(FileService.class);

    String localFile = "src/test/resources/" + fileName;
    try (OutputStream out = file.openWrite();
         ReadStream in = Vfs.openRead(localFile)) {
      in.writeToStream(out);
    }

    CompletableFuture<Boolean> future = new CompletableFuture<>();
    _lucene.update(file.getStatus().getPath(), v -> {
      future.complete(true);
    });

    future.get();

    return file;
  }

  final protected String[] search(String query)
    throws IOException, InterruptedException, ExecutionException
  {
    CompletableFuture<String[]> future = new CompletableFuture<>();

    _lucene.search(query, docs -> {
      future.complete(docs);
    });

    return future.get();
  }

  final protected String[] uploadAndSearch(String fileName, String query)
    throws InterruptedException, IOException, ExecutionException
  {
    upload(fileName);

    String[] result = search(query);

    return result;
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
  }
}


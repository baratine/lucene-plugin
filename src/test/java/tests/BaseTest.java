package tests;

import com.caucho.lucene.LuceneServiceClient;
import com.caucho.lucene.RDoc;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    update(file.getStatus().getPath());

    return file;
  }

  final protected boolean update(String fileName)
    throws ExecutionException, InterruptedException
  {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    _lucene.update(fileName, v -> {
      future.complete(true);
    });

    return future.get();
  }

  final protected RDoc[] search(String query)
    throws IOException, InterruptedException, ExecutionException
  {
    CompletableFuture<RDoc[]> future = new CompletableFuture<>();

    _lucene.search(query, docs -> {
      future.complete(docs);
    });

    return future.get();
  }

  final protected String[] search(String query, int offSet, int limit)
    throws IOException, InterruptedException, ExecutionException
  {
    CompletableFuture<String[]> future = new CompletableFuture<>();

    _lucene.searchInc(query, offSet, limit, docs -> {
      future.complete(docs);
    });

    return future.get();
  }

  final protected void clear() throws ExecutionException, InterruptedException
  {
    CompletableFuture<Void> future = new CompletableFuture<>();

    _lucene.clear(f -> {
      future.complete(null);
    });

    future.get();
  }

  final protected RDoc[] uploadAndSearch(String fileName, String query)
    throws InterruptedException, IOException, ExecutionException
  {
    upload(fileName);

    RDoc[] result = search(query);

    return result;
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
  }
}


package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneServiceClient;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ResultFuture;
import io.baratine.core.ServiceManager;
import io.baratine.files.FileService;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public abstract class BaseTest
{
  @Inject @Lookup
  private ServiceManager _serviceManager;

  @Inject @Lookup("/lucene")
  private LuceneServiceClient _lucene;

  protected FileService lookup(String path)
  {
    return _serviceManager.lookup(path).as(FileService.class);
  }

  final protected boolean delete(String fileName)
    throws InterruptedException, ExecutionException
  {
    ResultFuture<Boolean> future = new ResultFuture<>();

    _lucene.delete(makeBfsPath(fileName), future);

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
    ResultFuture<Boolean> future = new ResultFuture<>();
    _lucene.update(fileName, future);

    return future.get();
  }

  final protected LuceneEntry[] search(String query)
    throws IOException, InterruptedException, ExecutionException
  {
    ResultFuture<LuceneEntry[]> future = new ResultFuture<>();

    _lucene.search(query, future);

    return future.get();
  }

  final protected LuceneEntry[] search(String query,
                                       LuceneEntry after,
                                       int limit)
    throws IOException, InterruptedException, ExecutionException
  {
    ResultFuture<LuceneEntry[]> future = new ResultFuture<>();

    _lucene.searchAfter(query, after, limit, future);

    return future.get();
  }

  final protected void clear() throws ExecutionException, InterruptedException
  {
    ResultFuture<Void> future = new ResultFuture<>();

    _lucene.clear(future);

    future.get();
  }

  final protected LuceneEntry[] uploadAndSearch(String fileName, String query)
    throws InterruptedException, IOException, ExecutionException
  {
    upload(fileName);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
  }
}


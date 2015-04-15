package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ResultFuture;
import io.baratine.core.ServiceManager;
import io.baratine.files.BfsFile;
import io.baratine.files.BfsFileSync;
import org.junit.After;
import org.junit.Before;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public abstract class BaseTest
{
  @Inject
  private ServiceManager _serviceManager;

  @Inject @Lookup("/lucene-manager/bfs")
  private SyncLuceneIndex _lucene;

  protected BfsFileSync lookup(String path)
  {
    return _serviceManager.lookup(path).as(BfsFileSync.class);
  }

  final protected boolean delete(String fileName)
  {
    return _lucene.delete(makeBfsPath(fileName));
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
  }

  final protected LuceneEntry[] search(String query,
                                       LuceneEntry after,
                                       int limit)
  {
    return _lucene.searchAfter(query, after, limit);
  }

  final protected LuceneEntry[] uploadAndSearch(String fileName, String query)
    throws IOException
  {
    upload(fileName);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected BfsFile upload(String fileName) throws IOException
  {
    BfsFileSync file =
      _serviceManager.lookup(makeBfsPath(fileName)).as(BfsFileSync.class);

    String localFile = "src/test/resources/" + fileName;

    try (OutputStream out = file.openWrite();
         ReadStream in = Vfs.openRead(localFile)) {
      in.writeToStream(out);
    }

    update(file.getStatus().getPath());

    return file;
  }

  final protected boolean update(String fileName)
  {
    ResultFuture<Boolean> future = new ResultFuture<>();
    _lucene.indexFile(fileName, future);

    return future.get();
  }

  final protected LuceneEntry[] search(String query)
  {
    return _lucene.search(query, 256);
  }

  final protected LuceneEntry[] updateAndSearch(String id,
                                                String data,
                                                String query)
  {
    update(id, data);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected boolean update(String id, String text)
  {
    return _lucene.indexText(id, text);
  }

  final protected boolean update(String id, Map<String,Object> map)
  {
    return _lucene.indexMap(id, map);
  }

  @Before
  public final void baseBefore()
  {
    clear();
  }

  final protected void clear()
  {
    _lucene.clear();
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}


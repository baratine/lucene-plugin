package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeSync;
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
import java.util.List;
import java.util.Map;

public abstract class BaseTest
{
  private static final String DEFAULT = "default";
  @Inject
  private ServiceManager _serviceManager;

  @Inject
  @Lookup("public://lucene/service")
  private LuceneFacadeSync _index;

  protected BfsFileSync lookup(String path)
  {
    return _serviceManager.lookup(path).as(BfsFileSync.class);
  }

  final protected boolean delete(String id)
  {
    return _index.delete(DEFAULT, id);
  }

  final protected boolean deleteFile(String fileName)
  {
    return _index.delete(DEFAULT, makeBfsPath(fileName));
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs://cluster_hub/tmp/" + file;
  }

  final protected LuceneEntry[] uploadAndSearch(String fileName, String query)
    throws IOException
  {
    upload(fileName);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected LuceneEntry[] search(String query)
  {
    return search(DEFAULT, query);
  }

  final protected LuceneEntry[] search(String collection, String query)
  {
    List<LuceneEntry> temp = _index.search(collection, query, 255);

    return temp.toArray(new LuceneEntry[temp.size()]);
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
    _index.indexFile(DEFAULT, fileName, future);

    return future.get();
  }

  final protected LuceneEntry[] updateAndSearchText(String id,
                                                    String data,
                                                    String query)
  {
    update(id, data);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected boolean update(String id, String text)
  {
    return _index.indexText(DEFAULT, id, text);
  }

  final protected LuceneEntry[] updateAndSearchMap(String id,
                                                   Map<String,Object> data,
                                                   String query)
  {
    update(id, data);

    LuceneEntry[] result = search(query);

    return result;
  }

  final protected boolean update(String id, Map<String,Object> map)
  {
    return _index.indexMap(DEFAULT, id, map);
  }

  final protected boolean update(String collection, String id, String text)
  {
    return _index.indexText(collection, id, text);
  }

  @Before
  public final void baseBefore()
  {
    clear();
  }

  final protected void clear()
  {
    _index.clear(DEFAULT);
  }

  final protected void clear(String collection)
  {
    _index.clear(collection);
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}


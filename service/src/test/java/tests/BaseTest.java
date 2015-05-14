package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.Lookup;
import io.baratine.core.ResultFuture;
import io.baratine.core.ServiceManager;
import io.baratine.files.BfsFile;
import io.baratine.files.BfsFileSync;
import io.baratine.stream.StreamBuilder;
import org.junit.After;
import org.junit.Before;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseTest
{
  private static final String DEFAULT = "default";
  @Inject
  private ServiceManager _serviceManager;

  @Inject @Lookup("pod://lucene/index")
  private LuceneIndexSync _index;

  protected BfsFileSync lookup(String path)
  {
    return _serviceManager.lookup(path).as(BfsFileSync.class);
  }

  final protected boolean delete(String fileName)
  {
    return _index.delete(DEFAULT, makeBfsPath(fileName));
  }

  final protected String makeBfsPath(String file)
  {
    return "bfs:///tmp/" + file;
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
    StreamBuilder<LuceneEntry> stream = _index.search(DEFAULT, query);
    List<LuceneEntry> list
      = stream.collect(ArrayList<LuceneEntry>::new,
                       (l, e) -> l.add(e),
                       (a, b) -> a.addAll(b));

    return list.toArray(new LuceneEntry[list.size()]);
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
    return _index.indexText(DEFAULT, id, text);
  }

  final protected boolean update(String id, Map<String,Object> map)
  {
    return _index.indexMap(DEFAULT, id, map);
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

  @After
  public final void baseAfter()
  {
    clear();
  }
}


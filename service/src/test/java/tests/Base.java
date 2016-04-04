package tests;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.caucho.junit.RunnerBaratine;
import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneFacadeSync;
import com.caucho.v5.io.Vfs;
import com.google.common.io.Files;
import io.baratine.files.BfsFile;
import io.baratine.files.BfsFileSync;
import io.baratine.service.ResultFuture;
import io.baratine.service.Service;
import io.baratine.service.Services;
import org.junit.After;
import org.junit.Before;

public abstract class Base
{
  private static final String DEFAULT = "default";

  @Inject
  private Services _services;

  @Inject
  @Service("service")
  private LuceneFacadeSync _index;

  @Inject
  RunnerBaratine _testContext;

  protected BfsFileSync lookup(String path)
  {
    return _services.service(path).as(BfsFileSync.class);
  }

  final protected boolean delete(String id)
  {
    boolean result = _index.delete(DEFAULT, id);

    applyChanges();

    return result;
  }

  final protected boolean deleteFile(String fileName)
  {
    boolean result = _index.delete(DEFAULT, makeBfsPath(fileName));

    applyChanges();

    return result;
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

  public void applyChanges()
  {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    _testContext.addTime(2, TimeUnit.SECONDS);

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
      _services.service(makeBfsPath(fileName)).as(BfsFileSync.class);

    String localFile = "src/test/resources/" + fileName;

    try (OutputStream out = file.openWrite()) {
      Path path = Vfs.path(localFile);
      Files.copy(path.toFile(), out);
    }

    file.getStatus();

    update(file.getStatus().getPath());

    return file;
  }

  final protected boolean update(String fileName)
  {
    ResultFuture<Boolean> future = new ResultFuture<>();

    _index.indexFile(DEFAULT, fileName, future);

    boolean result = future.get();

    applyChanges();

    return result;
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
    boolean result = _index.indexText(DEFAULT, id, text);

    applyChanges();

    return result;
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
    boolean result = _index.indexMap(DEFAULT, id, map);

    applyChanges();

    return result;
  }

  final protected boolean update(String collection, String id, String text)
  {
    boolean result = _index.indexText(collection, id, text);

    applyChanges();

    return result;
  }

  @Before
  public final void baseBefore()
  {
    clear();
  }

  final protected void clear()
  {
    _index.clear(DEFAULT);
    applyChanges();
  }

  final protected void clear(String collection)
  {
    _index.clear(collection);

    applyChanges();
  }

  @After
  public final void baseAfter()
  {
    clear();
  }
}


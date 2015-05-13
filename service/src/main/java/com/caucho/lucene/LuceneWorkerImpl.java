package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.core.ResultSink;
import io.baratine.core.Service;
import io.baratine.core.Workers;

import java.util.Map;

@Workers(5)
@Service("/lucene-worker")
public class LuceneWorkerImpl implements LuceneWorker
{
  //@Inject
  private LuceneIndexBean _bean = LuceneIndexBean.getInstance();

  @Override
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_bean.indexFile(collection, path));
  }

  @Override
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    result.complete(_bean.indexText(collection, id, text));
  }

  @Override
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    result.complete(_bean.indexMap(collection, id, map));
  }

  @Override
  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> result)
  {
    LuceneEntry[] entries = _bean.search(collection, query, 255);

    result.complete(entries);
  }

  @Override
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_bean.delete(collection, id));
  }

  @Override
  public void clear(String collection, Result<Void> result)
    throws LuceneException
  {
    _bean.clear(collection);
  }
}

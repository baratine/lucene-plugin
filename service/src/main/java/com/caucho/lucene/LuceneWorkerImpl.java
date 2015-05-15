package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

import java.util.Map;

@Workers(5)
@Service("/lucene-worker")
public class LuceneWorkerImpl implements LuceneWorker
{
  //@Inject
  private LuceneIndexBean _bean = LuceneIndexBean.getInstance();

  final static int warmup = 100;
  final static int cutOff = 400;

  int _indexCounter = 0;
  long _indexTime = 0;
  int _searchCounter = 0;
  long _searchTime = 0;

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
    long start = System.currentTimeMillis();

    result.complete(_bean.indexText(collection, id, text));

    if (_indexCounter++ > warmup) {
      _indexTime += (System.currentTimeMillis() - start);
    }

    if (_indexCounter == cutOff)
      System.out.println("LuceneIndexBean.indexText: " + ((float) _indexTime
                                                          / (cutOff - warmup)));

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
    long start = System.currentTimeMillis();

    LuceneEntry[] entries = _bean.search(collection, query, 255);

    result.complete(entries);

    if (_searchCounter++ > warmup)
      _searchTime += (System.currentTimeMillis() - start);

    if (_searchCounter == cutOff)
      System.out.println("LuceneIndexBean.search " + ((float) _searchTime
                                                      / (cutOff - warmup)));

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

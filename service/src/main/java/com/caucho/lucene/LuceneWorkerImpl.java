package com.caucho.lucene;

import io.baratine.core.Modify;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Workers(20)
@Service("/lucene-worker")
public class LuceneWorkerImpl implements LuceneWorker
{
  private final static Logger log
    = Logger.getLogger(LuceneWorkerImpl.class.getName());

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();

  final static int warmup = 100;
  final static int cutOff = 400;

  int _indexCounter = 0;
  long _indexTime = 0;
  int _searchCounter = 0;
  long _searchTime = 0;

  @Override
  @Modify
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.indexFile(collection, path));
  }

  @Override
  @Modify
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    long start = System.currentTimeMillis();

    result.complete(_luceneBean.indexText(collection, id, text));

    if (_indexCounter++ > warmup) {
      _indexTime += (System.currentTimeMillis() - start);
    }

    if (_indexCounter == cutOff)
      System.out.println("LuceneIndexBean.indexText: " + ((float) _indexTime
                                                          / (cutOff - warmup)));

  }

  @Override
  @Modify
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    result.complete(_luceneBean.indexMap(collection, id, map));
  }

  @Override
  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> result)
  {
    long start = System.currentTimeMillis();

    LuceneEntry[] entries = _luceneBean.search(collection, query, 255);

    result.complete(entries);

    if (_searchCounter++ > warmup)
      _searchTime += (System.currentTimeMillis() - start);

    if (_searchCounter == cutOff)
      System.out.println("LuceneIndexBean.search " + ((float) _searchTime
                                                      / (cutOff - warmup)));
  }

  @OnSave
  public void save()
  {
    try {
      _luceneBean.commit();
    } catch (IOException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @Override
  @Modify
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.delete(collection, id));
  }

  @Override
  @Modify
  public void clear(String collection, Result<Void> result)
    throws LuceneException
  {
    _luceneBean.clear(collection);
  }
}

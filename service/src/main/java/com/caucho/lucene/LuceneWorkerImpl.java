package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Workers(20)
@Service("/lucene-writer")
public class LuceneWorkerImpl implements LuceneIndexWriter
{
  private final static Logger log
    = Logger.getLogger(LuceneWorkerImpl.class.getName());

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();

  @Override
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.indexFile(collection, path));
  }

  @Override
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    result.complete(_luceneBean.indexText(collection, id, text));
  }

  @Override
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    result.complete(_luceneBean.indexMap(collection, id, map));
  }

  @Override
  public void commit(Result<Boolean> result)
  {
    try {
      _luceneBean.commit();

      result.complete(true);
    } catch (IOException e) {
      log.log(Level.SEVERE, e.getMessage(), e);

      result.complete(false);
    }
  }

  @Override
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.delete(collection, id));
  }

  @Override
  public void clear(String collection, Result<Void> result)
    throws LuceneException
  {
    _luceneBean.clear(collection);
  }
}

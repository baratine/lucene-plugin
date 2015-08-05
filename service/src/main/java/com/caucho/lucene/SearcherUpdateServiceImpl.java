package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/searcher-update-service")
public class SearcherUpdateServiceImpl implements SearcherUpdateService
{
  private static final Logger log
    = Logger.getLogger(SearcherUpdateServiceImpl.class.getName());

  LuceneIndexBean _luceneIndexBean;

  @OnInit
  public void init()
  {
    _luceneIndexBean = LuceneIndexBean.getInstance();
  }

  @Override
  public void sync(Result<Boolean> result)
  {
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch()
  {
    try {
      long start = System.currentTimeMillis();
      _luceneIndexBean.commit();
      _luceneIndexBean.updateSearcher();

      float time = (float) (System.currentTimeMillis() - start) / 1000;

      log.warning(String.format("commit + updateSearcher took: %1$fs", time));
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);
    }
  }
}

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
  public void updateSearcher(Result<Boolean> result)
  {
    log.log(Level.WARNING, "update searcher");
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch(Result<Boolean> result)
  {
    try {
      _luceneIndexBean.commit();
      _luceneIndexBean.updateSearcher();
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);
    }

    result.complete(true);
  }
}

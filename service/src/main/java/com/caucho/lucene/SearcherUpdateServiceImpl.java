package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;

import java.util.concurrent.atomic.AtomicLong;
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
  }

  @Override
  public void updateSearcher(Result<Boolean> result)
  {
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch(Result<Boolean> result)
  {
    _luceneIndexBean.updateSearcher();
  }
}

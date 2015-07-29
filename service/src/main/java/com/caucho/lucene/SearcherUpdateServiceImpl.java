package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/searcher-update-service")
public class SearcherUpdateServiceImpl implements SearcherUpdateService
{
  private static final Logger log
    = Logger.getLogger(SearcherUpdateServiceImpl.class.getName());

  //@Inject
  LuceneIndexBean _luceneIndexBean;

  private AtomicLong _updateRequestCounter = new AtomicLong();

  @OnInit
  public void init()
  {
    if (_luceneIndexBean == null)
      _luceneIndexBean = LuceneIndexBean.getInstance();
  }

  @Override
  public void updateSearcher(Result<Boolean> result)
  {
    _updateRequestCounter.incrementAndGet();
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch(Result<Boolean> result)
  {
    if (_updateRequestCounter.get() > 0) {
      _luceneIndexBean.updateSearcher();

      long x = _updateRequestCounter.getAndSet(0);

      if (x > 1)
        log.log(Level.WARNING, "Searcher Update Service After Batch: " + x);
    }

    result.complete(true);
  }
}

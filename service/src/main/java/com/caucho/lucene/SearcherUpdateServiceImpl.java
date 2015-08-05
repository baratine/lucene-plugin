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

  private LuceneIndexBean _luceneIndexBean;
  private long _syncCallsCounter = 0;
  private long _afterBatchSequence = 0;

  @OnInit
  public void init()
  {
    _luceneIndexBean = LuceneIndexBean.getInstance();
  }

  @Override
  public void sync(Result<Boolean> result)
  {
    _syncCallsCounter++;
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch()
  {
    try {
      long start = System.currentTimeMillis();

      log.warning(String.format(
        "update-searcher-after-batch: %1$d sync-calls: %2$d",
        _afterBatchSequence,
        _syncCallsCounter));

      _luceneIndexBean.commit();
      _luceneIndexBean.updateSearcher();

      float time = (float) (System.currentTimeMillis() - start) / 1000;

      log.warning(String.format("update-searcher-after-batch: %1$d took: %2$f",
                                _afterBatchSequence,
                                time));

      _syncCallsCounter = 0;
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);
    }
  }
}

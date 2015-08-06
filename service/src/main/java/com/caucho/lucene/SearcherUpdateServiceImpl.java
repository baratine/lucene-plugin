package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.CancelHandle;
import io.baratine.core.Lookup;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceRef;
import io.baratine.timer.TimerService;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
  private boolean _isRefreshing = false;

  @Inject
  @Lookup("timer:///")
  private TimerService _timer;
  private Consumer<CancelHandle> _alarm;

  private SearcherUpdateService _self;

  @OnInit
  public void init()
  {
    _luceneIndexBean = LuceneIndexBean.getInstance();
    _self = ServiceRef.getCurrent().as(SearcherUpdateService.class);
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
    refresh(false);
  }

  private boolean isTimerSet()
  {
    return _alarm != null;
  }

  private void setTimer()
  {
    _timer.runAfter(_alarm = h -> onTimer(),
                    1000,
                    TimeUnit.MILLISECONDS,
                    Result.<CancelHandle>ignore());
  }

  private void clearTimer()
  {
    _alarm = null;
  }

  public void onTimer()
  {
    if (_alarm != null) {
      _self.refresh(true);
    }
  }

  public void refresh(boolean isTimer)
  {
    if (_isRefreshing) {
    }
    else if (isTimer) {
      clearTimer();

      refreshImpl();
    }
    else if (_luceneIndexBean.getUpdatesCount() >= 16) {
      refreshImpl();
    }
    else if (!isTimerSet()) {
      setTimer();
    }
  }

  public void refreshImpl()
  {
    _isRefreshing = true;

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
    } finally {
      _afterBatchSequence++;
      _isRefreshing = false;
    }
  }
}

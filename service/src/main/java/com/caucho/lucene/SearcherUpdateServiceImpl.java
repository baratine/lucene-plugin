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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/searcher-update-service")
public class SearcherUpdateServiceImpl implements SearcherUpdateService
{
  private static final Logger log
    = Logger.getLogger(SearcherUpdateServiceImpl.class.getName());

  private LuceneIndexBean _luceneIndexBean;
  private AtomicLong _refreshSequence = new AtomicLong();

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
    result.complete(true);
  }

  @AfterBatch
  public void afterBatch()
  {
    log.finer("@AfterBatch received");

    refresh(false);
  }

  private boolean isTimerSet()
  {
    return _alarm != null;
  }

  private void setTimer()
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("setting timer for %1$d ms",
                                _luceneIndexBean.getSoftCommitMaxAge()));

      _timer.runAfter(_alarm = h -> onTimer(_refreshSequence.get()),
                      _luceneIndexBean.getSoftCommitMaxAge(),
                      TimeUnit.MILLISECONDS,
                      Result.<CancelHandle>ignore());
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  private void clearTimer()
  {
    _alarm = null;
  }

  public void onTimer(long seq)
  {
    log.finer("on timer, updates-count: " + _luceneIndexBean.getUpdatesCount());

    if (seq == _refreshSequence.get()) {
      _self.refresh(true);
    }
    else if (_luceneIndexBean.getUpdatesCount() > 0) {
      setTimer();
    }
    else {
      clearTimer();
    }
  }

  public void refresh(boolean isTimer)
  {
    if (isTimer) {
      clearTimer();

      refreshImpl();
    }
    else if (_luceneIndexBean.getUpdatesCount()
             >= _luceneIndexBean.getSoftCommitMaxDocs()) {
      refreshImpl();
    }
    else if (!isTimerSet() && _luceneIndexBean.getUpdatesCount() > 0) {
      setTimer();
    }
  }

  public void refreshImpl()
  {
    try {
      _refreshSequence.incrementAndGet();

      _luceneIndexBean.commit();
      _luceneIndexBean.updateSearcher();
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);
    }
  }
}

package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.Lookup;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.timer.TimerService;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service("/searcher-update-service")
public class SearcherUpdateServiceImpl implements SearcherUpdateService
{
  private static final Logger log
    = Logger.getLogger(SearcherUpdateServiceImpl.class.getName());

  LuceneIndexBean _luceneIndexBean;

  @Inject
  @Lookup("timer:///")
  private TimerService _timerService;

  @OnInit
  public void init()
  {
    _luceneIndexBean = LuceneIndexBean.getInstance();

    _timerService.runEvery(x -> updateSearcher(Result.ignore()), 100,
                           TimeUnit.MILLISECONDS, Result.ignore());
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

    result.complete(true);
  }
}

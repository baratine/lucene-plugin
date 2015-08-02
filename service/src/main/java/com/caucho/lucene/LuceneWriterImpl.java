package com.caucho.lucene;

import io.baratine.core.CancelHandle;
import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.OnInit;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceRef;
import io.baratine.timer.TimerService;
import org.apache.lucene.queryparser.classic.QueryParser;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-writer")
public class LuceneWriterImpl implements LuceneIndexWriter
{
  private final static Logger log
    = Logger.getLogger(LuceneWriterImpl.class.getName());

  private final static long commitInterval = TimeUnit.SECONDS.toMillis(15);

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();
  private QueryParser _queryParser;

  private Consumer<CancelHandle> _isCommitTimer;

  @Inject
  @Lookup("/searcher-update-service")
  private SearcherUpdateService _searcherUpdateService;

  @Inject
  @Lookup("timer:///")
  private TimerService _timerService;

  private ServiceRef _self;

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.INFO, this + " init()");

    try {
      _luceneBean.init();

      _queryParser = _luceneBean.createQueryParser();

      _self = ServiceRef.getCurrent();

      result.complete(true);
    } catch (Throwable t) {
      log.log(Level.WARNING, "error creating query parser", t);

      result.fail(t);
    }
  }

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
    result.complete(_luceneBean.indexText(collection, id, text));
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
  @OnSave
  public void commit(Result<Boolean> result)
  {
    if (_isCommitTimer == null) {
      _timerService.runAfter(_isCommitTimer = h -> executeCommit(),
                             100,
                             //2000,
                             TimeUnit.MILLISECONDS,
                             Result.ignore());
      log.warning("LuceneWriterImpl.commit: " + _isCommitTimer);
    }

    result.complete(true);
  }

  private void executeCommit()
  {
    log.warning(String.format("executeCommit() self: %1$s, timer %2$s",
                              _self,
                              _isCommitTimer));

    _searcherUpdateService.updateSearcher(Result.<Boolean>ignore());

    ServiceRef.flushOutbox();

    log.warning(String.format("executeCommit(): after-flushOutbox self: %1$s",
                              _self));

    _isCommitTimer = null;
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
    _luceneBean.clear(_queryParser, collection);
  }
}


package com.caucho.lucene;

import io.baratine.core.CancelHandle;
import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.OnInit;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.timer.TimerService;
import org.apache.lucene.queryparser.classic.QueryParser;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

  private long _lastCommit = -1;

  @Inject
  @Lookup("timer:///")
  private TimerService _timerService;

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.INFO, this + " init()");

    try {
      _luceneBean.init();
      _queryParser = _luceneBean.createQueryParser();

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
    long now = System.currentTimeMillis();

    if (_lastCommit == -1 || now > (_lastCommit + commitInterval)) {
      _lastCommit = now;

      _timerService.runAfter(h -> executeCommit(h),
                             commitInterval,
                             TimeUnit.MILLISECONDS);
    }

    result.complete(true);
  }

  void logWarning(Throwable e)
  {
    log.log(Level.WARNING, e.getMessage(), e);
  }

  private void executeCommit(CancelHandle handle)
  {
    try {
      _luceneBean.commit();
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
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


package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.OnInit;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;
import io.baratine.timer.TaskInfo;
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

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();
  private QueryParser _queryParser;

  private Runnable _commitTask;

  @Inject
  @Lookup("timer:///")
  private TimerService _timerService;

  @Inject
  private ServiceManager _manager;

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.WARNING, this + " init()");

    _commitTask = new CommitTask(_manager.currentService()
                                         .as(Committable.class));

    try {
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
/*
    if (true) {
      try {
        _luceneBean.commit();
      } catch (IOException e) {
        e.printStackTrace();
      }

      result.complete(true);

      return;
    }

*/
    _timerService.getTask(_commitTask, Result.make(i -> scheduleCommit(i),
                                                   e -> logWarning(e)));

    result.complete(true);
  }

  void logWarning(Throwable e)
  {
    log.log(Level.WARNING, e.getMessage(), e);
  }

  private void scheduleCommit(TaskInfo task)
  {
    if (task == null) {
      _timerService.runAfter(_commitTask, 5, TimeUnit.SECONDS);
    }
  }

  public void commitInternal(Result<Boolean> result)
  {
    try {
      _luceneBean.commit();
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }

    result.complete(true);
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

class CommitTask implements Runnable
{
  private static final Logger log
    = Logger.getLogger(CommitTask.class.getName());
  private Committable _committable;

  public CommitTask(Committable committable)
  {
    _committable = committable;
  }

  @Override
  public void run()
  {
    _committable.commitInternal(Result.ignore());
  }
}

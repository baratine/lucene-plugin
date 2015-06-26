package com.caucho.lucene;

import io.baratine.core.OnDestroy;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.ResultSink;
import io.baratine.core.Service;
import io.baratine.core.Workers;
import io.baratine.stream.StreamBuilder;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-reader")
@Workers(20)
public class LuceneReaderImpl implements LuceneReader
{
  private final static AtomicLong sequence = new AtomicLong();

  private final static Logger log
    = Logger.getLogger(LuceneReaderImpl.class.getName());

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();

  private QueryParser _queryParser;
  private long _n;

  public LuceneReaderImpl()
  {
    _n = sequence.getAndIncrement();
  }

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.INFO, this + " init()");

    try {
      _queryParser = _luceneBean.createQueryParser();

      result.complete(true);
    } catch (Throwable t) {
      log.log(Level.WARNING, "error creating query parser", t);

      result.fail(t);
    }
  }

  private QueryParser getQueryParser()
  {
    if (_queryParser == null)
      _queryParser = _luceneBean.createQueryParser();

    return _queryParser;
  }

  @Override
  public StreamBuilder search(String collection, String query)
  {
    throw new AbstractMethodError();
  }

  public void search(String collection,
                     String query,
                     ResultSink<LuceneEntry> results)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("search('%1$s', %2$s)", collection, query));

    LuceneEntry[] entries = searchImpl(collection, query);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s accepting %2$d lucene entries",
                                         results,
                                         entries.length));

    for (LuceneEntry entry : entries) {
      results.accept(entry);
    }

    results.end();
  }

  public LuceneEntry[] searchImpl(String collection,
                                  String query)
  {
    QueryParser queryParser = getQueryParser();
    LuceneEntry[] entries = _luceneBean.search(queryParser,
                                               collection,
                                               query,
                                               255);

    return entries;
  }

  @OnDestroy
  public void onDestroy()
  {
    log.log(Level.INFO, String.format("%1$s destroy", this));
  }

  @Override
  public String toString()
  {
    return LuceneReaderImpl.class.getSimpleName() + '[' + _n + ']';
  }
}

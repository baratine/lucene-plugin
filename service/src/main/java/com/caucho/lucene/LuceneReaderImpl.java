package com.caucho.lucene;

import io.baratine.core.OnDestroy;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.ResultStream;
import io.baratine.core.Service;
import io.baratine.core.Workers;
import io.baratine.stream.ResultStreamBuilder;
import org.apache.lucene.queryparser.classic.QueryParser;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-reader")
//`@Workers(20)
public class LuceneReaderImpl implements LuceneReader
{
  private final static AtomicLong sequence = new AtomicLong();

  private final static Logger log
    = Logger.getLogger(LuceneReaderImpl.class.getName());

  @Inject
  private LuceneIndexBean _luceneBean;// = LuceneIndexBean.getInstance();

  private BaratineIndexSearcher _searcher;

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
      _luceneBean.init();

      result.complete(true);
    } catch (Throwable t) {
      log.log(Level.WARNING, "error creating query parser", t);

      result.fail(t);
    }
  }

  private QueryParser getQueryParser()
  {
    return _queryParser;
  }

  @Override
  public ResultStreamBuilder search(String collection, String query)
  {
    throw new AbstractMethodError();
  }

  public void search(String collection,
                     String query,
                     ResultStream<LuceneEntry> results) throws IOException
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

    results.complete();
  }

  public LuceneEntry[] searchImpl(String collection,
                                  String query) throws IOException
  {
    QueryParser queryParser = getQueryParser();

    try {
      if (_searcher == null) {
        _searcher = _luceneBean.acquireSearcher();
      }
      else if (_searcher.getVersion()
               < _luceneBean.getSearcherSequence().get()) {
        _luceneBean.release(_searcher);

        _searcher = _luceneBean.acquireSearcher();
      }

      LuceneEntry[] entries = _luceneBean.search(_searcher,
                                                 queryParser,
                                                 collection,
                                                 query,
                                                 255);

      return entries;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  //@AfterBatch
  public void afterBatch(Result<Boolean> result) throws IOException
  {
    try {
      if (_searcher != null) {
        _luceneBean.release(_searcher);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    } finally {
      _searcher = null;
      result.complete(true);
    }
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

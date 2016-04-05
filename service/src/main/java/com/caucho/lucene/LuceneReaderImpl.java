package com.caucho.lucene;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import io.baratine.service.AfterBatch;
import io.baratine.service.Api;
import io.baratine.service.OnDestroy;
import io.baratine.service.OnInit;
import io.baratine.service.Result;
import io.baratine.service.Service;
import org.apache.lucene.queryparser.classic.QueryParser;

@Service("/lucene-reader")
@Api(LuceneReader.class)
//@Workers(20)
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

      result.ok(true);
    } catch (Throwable t) {
      log.log(Level.WARNING, "error creating query parser", t);

      result.fail(t);
    }
  }

  private QueryParser getQueryParser()
  {
    return _queryParser;
  }

  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> results)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("search('%1$s', %2$s, %3$tH:%3$tM:%3$tS,:%3$tL)",
                              collection,
                              query,
                              new Date()));

    LuceneEntry[] entries = searchImpl(collection, query);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s accepting %2$d lucene entries",
                                         results,
                                         entries.length));

    results.ok(entries);
  }

  public LuceneEntry[] searchImpl(String collection,
                                  String query)
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

      if (e instanceof RuntimeException)
        throw (RuntimeException) e;
      else
        throw new RuntimeException(e);
    }
  }

  @AfterBatch
  public void afterBatch() throws IOException
  {
    try {
      if (_searcher != null) {
        _luceneBean.release(_searcher);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    } finally {
      _searcher = null;
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

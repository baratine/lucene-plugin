package com.caucho.lucene;

import io.baratine.core.OnDestroy;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-reader")
@Workers(20)
public class LuceneReaderImpl implements LuceneReader
{
  private final static Logger log
    = Logger.getLogger(LuceneReaderImpl.class.getName());

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();

  private XIndexSearcher _searcher;
  private QueryParser _queryParser;

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

  private QueryParser getQueryParser() {
    if (_queryParser == null)
      _queryParser = _luceneBean.createQueryParser();

    return _queryParser;
  }

  @Override
  /*synchronized works around worker called by multiple threads*/
  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> result)
  {
    try {
      if (_searcher == null) {
        _searcher = _luceneBean.createSearcher();

        log.log(Level.WARNING, "create new searcher " + this);
      }
      else if (_searcher.getSequence() < _luceneBean.getSequence()) {
        _searcher.release();
        _searcher = _luceneBean.createSearcher();
      }
    } catch (IOException e) {
      log.log(Level.WARNING, "error creating IndexSearcher", e);

      throw LuceneException.create(e);
    }

    QueryParser queryParser = getQueryParser();
    LuceneEntry[] entries = _luceneBean.search(_searcher,
                                               queryParser,
                                               collection,
                                               query,
                                               255);

    result.complete(entries);
  }

  @OnDestroy
  public void onDestroy()
  {
    XIndexSearcher searcher = _searcher;

    _searcher = null;

    if (searcher == null)
      return;

    try {
      searcher.release();
    } catch (Throwable t) {
      log.log(Level.WARNING,
              String.format("error releasing %1$s", _searcher),
              t);
    }
  }
}

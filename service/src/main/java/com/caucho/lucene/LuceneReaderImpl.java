package com.caucho.lucene;

import io.baratine.core.OnDestroy;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

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

  @Override
  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> result)
  {
    try {
      if (_searcher == null) {
        _searcher = _luceneBean.createSearcher();
      }
      else if (_searcher.getSequence() < _luceneBean.getSequence()) {
        _searcher.release();
        _searcher = _luceneBean.createSearcher();
      }
    } catch (IOException e) {
      log.log(Level.WARNING, "error creating IndexSearcher", e);

      throw LuceneException.create(e);
    }

    LuceneEntry[] entries = _luceneBean.search(_searcher,
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

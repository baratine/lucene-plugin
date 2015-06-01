package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.Workers;

import java.util.logging.Logger;

@Service("/lucene-reader")
@Workers(20)
public class LuceneReaderImpl implements LuceneReader
{
  private final static Logger log
    = Logger.getLogger(LuceneReaderImpl.class.getName());

  //@Inject
  private LuceneIndexBean _luceneBean = LuceneIndexBean.getInstance();

  @Override
  public void search(String collection,
                     String query,
                     Result<LuceneEntry[]> result)
  {
    LuceneEntry[] entries = _luceneBean.search(collection, query, 255);

    result.complete(entries);
  }
}

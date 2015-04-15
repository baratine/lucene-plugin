package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.Result;
import io.baratine.core.ServiceRef;
import io.baratine.core.SessionService;

import javax.inject.Inject;
import java.util.Map;

@SessionService("session://")
public class LuceneSessionImpl implements LuceneSession
{
  @Inject @Lookup("")
  ServiceRef _luceneManager;

  private LuceneIndex getLuceneIndex(String collection)
  {
    _luceneManager.
  }

  @Override
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {

  }

  @Override public void indexText(String collection,
                                  String id,
                                  String text,
                                  Result<Boolean> result) throws LuceneException
  {

  }

  @Override public void indexMap(String collection,
                                 String id,
                                 Map<String,Object> map,
                                 Result<Boolean> result) throws LuceneException
  {

  }

  @Override public void search(String collection,
                               String query,
                               int limit,
                               Result<LuceneEntry[]> result)
    throws LuceneException
  {

  }

  @Override public void searchAfter(String collection,
                                    String query,
                                    LuceneEntry after,
                                    int limit,
                                    Result<LuceneEntry[]> result)
    throws LuceneException
  {

  }

  @Override
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {

  }

  @Override public void clear(String collection, Result<Void> result)
    throws LuceneException
  {

  }
}

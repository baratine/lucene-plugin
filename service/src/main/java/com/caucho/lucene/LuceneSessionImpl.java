package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.Result;
import io.baratine.core.ServiceRef;
import io.baratine.core.SessionService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@SessionService("session://lucene/lucene")
public class LuceneSessionImpl implements LuceneSession
{
  @Inject @Lookup("pod://lucene/lucene")
  ServiceRef _luceneManager;

  private Map<String,LuceneIndex> _luceneIndexMap = new HashMap<>();

  private LuceneIndex getLuceneIndex(String collection)
  {
    LuceneIndex lucene = _luceneIndexMap.get(collection);

    if (lucene == null) {
      lucene = _luceneManager.lookup("/" + collection).as(LuceneIndex.class);

      _luceneIndexMap.put(collection, lucene);
    }

    return lucene;
  }

  @Override
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    getLuceneIndex(collection).indexFile(path, result);
  }

  @Override
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    getLuceneIndex(collection).indexText(id, text, result);
  }

  @Override public void indexMap(String collection,
                                 String id,
                                 Map<String,Object> map,
                                 Result<Boolean> result) throws LuceneException
  {
    getLuceneIndex(collection).indexMap(id, map, result);
  }

  @Override
  public void search(String collection,
                     String query,
                     int limit,
                     Result<LuceneEntry[]> result)
    throws LuceneException
  {
    getLuceneIndex(collection).search(query, limit, result);
  }

  @Override
  public void searchAfter(String collection,
                          String query,
                          LuceneEntry after,
                          int limit,
                          Result<LuceneEntry[]> result)
    throws LuceneException
  {
    getLuceneIndex(collection).searchAfter(query, after, limit, result);
  }

  @Override
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    getLuceneIndex(collection).delete(id, result);
  }

  @Override
  public void clear(String collection, Result<Void> result)
    throws LuceneException
  {
    getLuceneIndex(collection).clear(result);
  }
}

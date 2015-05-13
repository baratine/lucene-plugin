package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.Result;
import io.baratine.core.SessionService;
import io.baratine.session.SessionScoped;

import javax.inject.Inject;
import java.util.Map;

@SessionService("session://lucene/session/{_id}")
public class LuceneSessionImpl implements LuceneSession
{
  @SessionScoped
  private String _id;

  @Inject @Lookup("pod://lucene/index")
  private LuceneIndex _index;

  @Override
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    _index.indexFile(collection, path, result);
  }

  @Override
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    _index.indexText(collection, id, text, result);
  }

  @Override
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    _index.indexMap(collection, id, map, result);
  }

  @Override
  public void search(String collection,
                     String query,
                     int limit,
                     Result<LuceneEntry[]> result)
    throws LuceneException
  {
    _index.search(collection, query, limit, result);
  }

  @Override
  public void searchAfter(String collection,
                          String query,
                          LuceneEntry after,
                          int limit,
                          Result<LuceneEntry[]> result)
    throws LuceneException
  {
    _index.searchAfter(collection,
                       query,
                       after,
                       limit,
                       result);
  }

  @Override
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    _index.delete(collection, id, result);
  }

  @Override
  public void clear(String collection, Result<Void> result)
    throws LuceneException
  {
    _index.clear(collection, result);
  }
}

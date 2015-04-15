package com.caucho.lucene;

import io.baratine.core.Result;

import java.util.Map;

public interface LuceneSession
{
  /**
   * Updates lucene index for item at bfsPath
   *
   * @param collection - collection name
   * @param path       - BFS path e.g. bfs:///tmp/test.txt
   * @param result
   */
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException;

  /**
   * Updates lucene index for data specified in <code>stream</code> parameter.
   *
   * @param collection - collection name
   * @param id         - id that will be associated with the data
   * @param text       - text
   * @param result
   */
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result)
    throws LuceneException;

  /**
   * Updates lucene index for data specified in a <code>map</code> parameter.
   * Each key in the map becomes a searchable field
   *
   * @param collection - collection name
   * @param id         - should uniquely identify map
   * @param map        - map of values
   * @param result
   * @throws LuceneException
   */
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result)
    throws LuceneException;

  /**
   * Executes lucene search
   *
   * @param collection - collection name
   * @param query      - query that is passed to lucene QueryParser
   * @param limit      - specifies upper limit on the result set
   * @param result
   */
  public void search(String collection,
                     String query,
                     int limit,
                     Result<LuceneEntry[]> result)
    throws LuceneException;

  /**
   * Executes lucene search returning results after a known entry passed in
   * parameter <code>after</code>.
   *
   * @param collection - collection name
   * @param query      - query that is passed to lucene QueryParser
   * @param after      - specifies last collected entry or null
   * @param limit
   * @param result
   */
  public void searchAfter(String collection,
                          String query,
                          LuceneEntry after,
                          int limit,
                          Result<LuceneEntry[]> result) throws LuceneException;

  /**
   * Deletes document from an index
   *
   * @param collection - collection name
   * @param id         - BFS path or id of a string
   * @see #indexFile
   * @see #indexText
   */
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException;

  /**
   * Delete all indexes
   *
   * @param collection - collection name
   * @param result
   */
  public void clear(String collection, Result<Void> result)
    throws LuceneException;
}

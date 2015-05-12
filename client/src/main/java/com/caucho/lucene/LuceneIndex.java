package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.stream.StreamBuilder;

import java.util.Map;

public interface LuceneIndex
{
  /**
   * Updates lucene index for item at bfsPath
   *
   * @param path   BFS path e.g. bfs:///tmp/test.txt
   * @param result
   */
  void indexFile(String path, Result<Boolean> result)
    throws LuceneException;

  /**
   * Updates lucene index for data specified in <code>stream</code> parameter.
   *
   * @param id     - id that will be associated with the data
   * @param text   - text
   * @param result
   */
  void indexText(String id, String text, Result<Boolean> result)
    throws LuceneException;

  /**
   * Updates lucene index for data specified in a <code>map</code> parameter.
   * Each key in the map becomes a searchable field
   *
   * @param id
   * @param map
   * @param result
   * @throws LuceneException
   */
  void indexMap(String id,
                Map<String,Object> map,
                Result<Boolean> result)
    throws LuceneException;

  /**
   * Executes lucene search
   *
   * @param query  query that is passed to lucene QueryParser
   * @param limit  specifies upper limit on the result set
   * @param result files
   */
  void search(String query, int limit, Result<LuceneEntry[]> result)
    throws LuceneException;

  /**
   * Executes lucene search returning results after a known entry passed in
   * parameter <code>after</code>.
   *
   * @param query  query that is passed to lucene QueryParser
   * @param after  specifies last collected entry or null
   * @param limit
   * @param result
   */
  void searchAfter(String query,
                   LuceneEntry after,
                   int limit,
                   Result<LuceneEntry[]> result) throws LuceneException;

  /**
   * Deletes document from an index
   *
   * @param id BFS path or id of a string
   * @see #indexFile
   * @see #indexText
   */
  void delete(String id, Result<Boolean> result)
    throws LuceneException;

  /**
   * Delete all indexes
   *
   * @param result
   */
  void clear(Result<Void> result) throws LuceneException;

  StreamBuilder search2(String query);
}

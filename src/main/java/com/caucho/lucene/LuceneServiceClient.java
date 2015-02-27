package com.caucho.lucene;

import io.baratine.core.Result;

public interface LuceneServiceClient
{
  /**
   * Updates lucene index for item at bfsPath
   *
   * @param path   BFS path e.g. bfs:///tmp/test.txt
   * @param result
   */
  public void updateBfs(String path, Result<Boolean> result);

  /**
   * Updates lucene index for data specified in <code>data</code> parameter.
   *
   * @param id     - id that will be associated with the data
   * @param data   - text
   * @param result
   */
  public void update(String id, String data, Result<Boolean> result);

  /**
   * Queries
   *
   * @param query  query that is passed to lucene QueryParser
   * @param result files
   */
  public void search(String query, Result<LuceneEntry[]> result);

  /**
   * @param query  query that is passed to lucene QueryParser
   * @param after
   * @param limit
   * @param result
   */
  public void searchAfter(String query,
                          LuceneEntry after,
                          int limit,
                          Result<LuceneEntry[]> result);

  /**
   * Deletes document from an index
   *
   * @param path BFS path
   * @see #updateBfs
   */
  public void delete(String path, Result<Boolean> result);

  /**
   * Delete all indexes
   *
   * @param result
   */
  public void clear(Result<Void> result);
}

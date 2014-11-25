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
  public void update(String path, Result<Boolean> result);

  /**
   * Queries
   *
   * @param query  query that is passed to lucene QueryParser
   * @param result files
   */
  public void search(String query, Result<RDoc[]> result);

  /**
   *
   * @param query  query that is passed to lucene QueryParser
   * @param offset
   * @param limit
   * @param result
   */
  public void searchInc(String query, int offset, int limit, Result<String[]> result);

  /**
   * Deletes document from an index
   *
   * @param path BFS path
   * @see #update
   */
  public void delete(String path, Result<Boolean> result);

  /**
   * Delete all indexes
   *
   * @param result
   */
  public void clear(Result<Void> result);
}

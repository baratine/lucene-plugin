package com.caucho.lucene;

import io.baratine.core.Result;

public interface LuceneIndex
{
  /**
   * Updates lucene index for item at bfsPath
   *
   * @param path   BFS path e.g. bfs:///tmp/test.txt
   * @param result
   */
  public void indexFile(String path, Result<Boolean> result)
    throws LuceneException;

  /**
   * Updates lucene index for data specified in <code>data</code> parameter.
   *
   * @param id     - id that will be associated with the data
   * @param data   - text
   * @param result
   */
  public void indexString(String id, String data, Result<Boolean> result)
    throws LuceneException;

  /**
   * Queries                                             `
   *
   * @param query  query that is passed to lucene QueryParser
   * @param result files
   */
  public void search(String query, int limit, Result<LuceneEntry[]> result)
    throws LuceneException;

  /**
   * @param query  query that is passed to lucene QueryParser
   * @param after
   * @param limit
   * @param result
   */
  public void searchAfter(String query,
                          LuceneEntry after,
                          int limit,
                          Result<LuceneEntry[]> result) throws LuceneException;

  /**
   * Deletes document from an index
   *
   * @param id BFS path or id of a string
   * @see #indexFile
   * @see #indexString
   */
  public void delete(String id, Result<Boolean> result)
    throws LuceneException;

  /**
   * Delete all indexes
   *
   * @param result
   */
  public void clear(Result<Void> result) throws LuceneException;
}

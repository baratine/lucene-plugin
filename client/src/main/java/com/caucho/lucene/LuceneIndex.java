package com.caucho.lucene;

import io.baratine.core.Result;

import java.io.InputStream;

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
   * Updates lucene index for data specified in <code>stream</code> parameter.
   *
   * @param id     - id that will be associated with the data
   * @param text - text
   * @param result
   */
  public void indexText(String id, String text, Result<Boolean> result)
    throws LuceneException;

  /**
   * Executes lucene search
   *
   * @param query  query that is passed to lucene QueryParser
   * @param limit  specifies upper limit on the result set
   * @param result files
   */
  public void search(String query, int limit, Result<LuceneEntry[]> result)
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
  public void searchAfter(String query,
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
  public void delete(String id, Result<Boolean> result)
    throws LuceneException;

  /**
   * Delete all indexes
   *
   * @param result
   */
  public void clear(Result<Void> result) throws LuceneException;
}

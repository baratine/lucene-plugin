package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneException;
import com.caucho.lucene.LuceneIndex;

public interface SyncLuceneIndex extends LuceneIndex
{
  boolean indexFile(String path) throws LuceneException;

  boolean indexString(String id, String data) throws LuceneException;

  LuceneEntry[] search(String query, int limit) throws LuceneException;

  LuceneEntry[] searchAfter(String query, LuceneEntry after, int limit)
    throws LuceneException;

  boolean delete(String id) throws LuceneException;

  void clear() throws LuceneException;
}

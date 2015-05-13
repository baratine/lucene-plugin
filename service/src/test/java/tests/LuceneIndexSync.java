package tests;

import com.caucho.lucene.LuceneEntry;
import com.caucho.lucene.LuceneException;
import com.caucho.lucene.LuceneIndex;

import java.util.Map;

public interface LuceneIndexSync extends LuceneIndex
{
  boolean indexFile(String collection, String path) throws LuceneException;

  boolean indexText(String collection, String id, String text) throws LuceneException;

  boolean indexMap(String collection, String id, Map<String,Object> text) throws LuceneException;

  LuceneEntry[] search(String collection, String query, int limit) throws LuceneException;

  LuceneEntry[] searchAfter(String collection,
                            String query,
                            LuceneEntry after,
                            int limit)
    throws LuceneException;

  boolean delete(String collection, String id) throws LuceneException;

  void clear(String collection) throws LuceneException;
}

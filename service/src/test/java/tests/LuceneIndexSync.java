package tests;

import com.caucho.lucene.LuceneException;

import java.util.Map;

public interface LuceneIndexSync
{
  boolean indexFile(String collection, String path) throws LuceneException;

  boolean indexText(String collection, String id, String text)
    throws LuceneException;

  boolean indexMap(String collection, String id, Map<String,Object> text)
    throws LuceneException;

  boolean delete(String collection, String id) throws LuceneException;

  void clear(String collection) throws LuceneException;
}

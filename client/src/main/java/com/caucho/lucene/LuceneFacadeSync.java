package com.caucho.lucene;

import java.util.List;
import java.util.Map;

public interface LuceneFacadeSync extends LuceneFacade
{
  boolean indexFile(String collection, String path)
    throws LuceneException;

  boolean indexText(String collection, String id, String text)
    throws LuceneException;

  boolean indexMap(String collection, String id, Map<String,Object> map)
    throws LuceneException;

  boolean delete(String collection, String id)
    throws LuceneException;

  List<LuceneEntry> search(String collection, String query, int limit)
    throws LuceneException;

  void clear(String collection)
    throws LuceneException;
}

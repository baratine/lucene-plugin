package com.caucho.lucene;

import io.baratine.core.Result;
import io.baratine.core.ResultSink;

import java.util.Map;

public interface LuceneWorker
{
  void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException;

  void indexText(String collection,
                 String id,
                 String text,
                 Result<Boolean> result)
    throws LuceneException;

  void indexMap(String collection,
                String id,
                Map<String,Object> map,
                Result<Boolean> result)
    throws LuceneException;

  void search(String collection, String query, Result<LuceneEntry[]> result);

  void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException;

  void clear(String collection, Result<Void> result) throws LuceneException;

  void commit(Result<Boolean> result);
}

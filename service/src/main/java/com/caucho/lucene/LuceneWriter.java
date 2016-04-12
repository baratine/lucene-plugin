package com.caucho.lucene;

import io.baratine.service.Result;
import io.baratine.stream.ResultStreamBuilder;
import io.baratine.web.Form;

import java.util.Map;

public interface LuceneWriter
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
                Form map,
                Result<Boolean> result)
    throws LuceneException;

  void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException;

  ResultStreamBuilder<Void> clear(String collection) throws LuceneException;

  void save(Result<Boolean> result);
}

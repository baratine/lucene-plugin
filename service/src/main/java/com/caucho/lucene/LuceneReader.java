package com.caucho.lucene;

import io.baratine.core.Result;

public interface LuceneReader
{
  void search(String collection, String query, Result<LuceneEntry[]> result);
}

package com.caucho.lucene;

import io.baratine.core.Result;

public interface LuceneIndexReader
{
  void search(String collection, String query, Result<LuceneEntry[]> result);
}

package com.caucho.lucene;

import io.baratine.stream.ResultStreamBuilder;

public interface LuceneReader
{
  ResultStreamBuilder search(String collection, String query);
}

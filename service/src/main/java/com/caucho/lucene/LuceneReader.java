package com.caucho.lucene;

import io.baratine.stream.StreamBuilder;

public interface LuceneReader
{
  StreamBuilder search(String collection, String query);
}

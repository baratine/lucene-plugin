package com.caucho.lucene;

import io.baratine.core.Result;

public interface SearcherUpdateService
{
  void updateSearcher(Result<Boolean> result);
}

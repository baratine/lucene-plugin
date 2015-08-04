package com.caucho.lucene;

import io.baratine.core.Result;

public interface SearcherUpdateService
{
  void sync(Result<Boolean> result);
}

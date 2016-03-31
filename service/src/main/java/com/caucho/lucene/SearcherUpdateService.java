package com.caucho.lucene;

import io.baratine.service.Result;

public interface SearcherUpdateService
{
  void sync(Result<Boolean> result);

  void refresh(boolean isTimer);
}

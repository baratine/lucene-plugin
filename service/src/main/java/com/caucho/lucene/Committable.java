package com.caucho.lucene;

import io.baratine.core.Result;

public interface Committable
{
  void commitInternal(Result<Boolean> committ);
}

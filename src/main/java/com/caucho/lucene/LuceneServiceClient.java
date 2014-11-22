package com.caucho.lucene;

import io.baratine.core.Result;

public interface LuceneServiceClient
{
  public void update(String path, Result<Boolean> result);

  public void search(String query, Result<String[]> result);

  public void clear(Result<Void> result);
}

package com.caucho.lucene;

import io.baratine.core.AfterBatch;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;

@Service("/searcher-update-service")
public class SearcherUpdateServiceImpl implements SearcherUpdateService
{
  //@Inject
  LuceneIndexBean _luceneIndexBean;

  private boolean _isUpdate = false;

  @OnInit
  public void init()
  {
    if (_luceneIndexBean == null)
      _luceneIndexBean = LuceneIndexBean.getInstance();
  }

  @Override
  public void updateSearcher(Result<Boolean> result)
  {
    _isUpdate = true;
  }

  @AfterBatch
  public void afterBatch(Result<Boolean> result)
  {
    if (_isUpdate)
      _luceneIndexBean.updateSearcher();

    _isUpdate = false;

    result.complete(true);
  }
}

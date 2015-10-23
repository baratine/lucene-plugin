package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.OnInit;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.ResultStream;
import io.baratine.core.Service;
import io.baratine.stream.ResultStreamBuilder;
import org.apache.lucene.queryparser.classic.QueryParser;

import javax.inject.Inject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-writer")
public class LuceneWriterImpl implements LuceneWriter
{
  private final static Logger log
    = Logger.getLogger(LuceneWriterImpl.class.getName());

  @Inject
  private LuceneIndexBean _luceneBean;// = LuceneIndexBean.getInstance();
  private QueryParser _queryParser;

  @Inject
  @Lookup("/searcher-update-service")
  private SearcherUpdateService _searcherUpdateService;

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.INFO, this + " init()");

    try {
      _luceneBean.init();

      _queryParser = _luceneBean.createQueryParser();

      result.complete(true);
    } catch (Throwable t) {
      log.log(Level.WARNING, "error creating query parser", t);

      result.fail(t);
    }
  }

  @Override
  @Modify
  public void indexFile(String collection, String path, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.indexFile(collection, path));
  }

  @Override
  @Modify
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    boolean isSuccess = _luceneBean.indexText(collection, id, text);

    result.complete(isSuccess);
  }

  @Override
  @Modify
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    result.complete(_luceneBean.indexMap(collection, id, map));
  }

  @Override
  @OnSave
  public void save(Result<Boolean> result)
  {
    sync();

    result.complete(true);
  }

  private void sync()
  {
    _searcherUpdateService.sync(Result.<Boolean>ignore());
  }

  @Override
  @Modify
  public void delete(String collection, String id, Result<Boolean> result)
    throws LuceneException
  {
    result.complete(_luceneBean.delete(collection, id));
  }

  @Override
  public ResultStreamBuilder<Void> clear(String collection)
    throws LuceneException
  {
    throw new AbstractMethodError();
  }

  @Modify
  public void clear(String collection, ResultStream<Boolean> result)
    throws LuceneException
  {
    _luceneBean.clear(_queryParser, collection);

    result.complete();
  }
}


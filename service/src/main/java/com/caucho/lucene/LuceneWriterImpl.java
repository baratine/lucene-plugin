package com.caucho.lucene;

import io.baratine.service.Api;
import io.baratine.service.Service;
import io.baratine.service.Modify;
import io.baratine.service.OnInit;
import io.baratine.service.OnSave;
import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.stream.ResultStream;
import io.baratine.stream.ResultStreamBuilder;
import io.baratine.web.Form;
import org.apache.lucene.queryparser.classic.QueryParser;

import javax.inject.Inject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("/lucene-writer")
@Api(LuceneWriter.class)
public class LuceneWriterImpl implements LuceneWriter
{
  private final static Logger log
    = Logger.getLogger(LuceneWriterImpl.class.getName());

  @Inject
  private LuceneIndexBean _luceneBean;// = LuceneIndexBean.getInstance();
  private QueryParser _queryParser;

  @Inject
  @Service("/searcher-update-service")
  private SearcherUpdateService _searcherUpdateService;

  @OnInit
  public void init(Result<Boolean> result)
  {
    log.log(Level.INFO, this + " init()");

    try {
      _luceneBean.init();

      _queryParser = _luceneBean.createQueryParser();

      result.ok(true);
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
    result.ok(_luceneBean.indexFile(collection, path));
  }

  @Override
  @Modify
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result) throws LuceneException
  {
    boolean isSuccess = _luceneBean.indexText(collection, id, text);

    result.ok(isSuccess);
  }

  @Override
  @Modify
  public void indexMap(String collection,
                       String id,
                       Form map,
                       Result<Boolean> result) throws LuceneException
  {
    result.ok(_luceneBean.indexMap(collection, id, map));
  }

  @Override
  @OnSave
  public void save(Result<Boolean> result)
  {
    sync();

    result.ok(true);
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
    result.ok(_luceneBean.delete(collection, id));
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

    result.ok();
  }
}


package com.caucho.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import io.baratine.service.Api;
import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.stream.ResultStreamBuilder;
import io.baratine.web.Body;
import io.baratine.web.Form;
import io.baratine.web.Get;
import io.baratine.web.Path;
import io.baratine.web.Post;
import io.baratine.web.Query;

@Service("service")
@Api(LuceneFacade.class)
@Path("/lucene")
public class LuceneFacadeImpl implements LuceneFacade
{
  private static final Logger log
    = Logger.getLogger(LuceneFacadeImpl.class.getName());

  @Inject
  @Service("/lucene-writer")
  private LuceneWriter _indexWriter;

  @Inject
  @Service("/lucene-reader")
  private LuceneReader _indexReader;

  private LuceneWriter getLuceneWriter(String id)
  {
    return _indexWriter;
  }

  @Override
  @Post("/index-file")
  public void indexFile(@Body("collection") String collection,
                        @Body("path") String path,
                        Result<Boolean> result)
    throws LuceneException
  {
    checkCollection(collection);
    checkId(path);

    getLuceneWriter(path).indexFile(collection, path, result);
  }

  @Override
  @Post("/index-text")
  public void indexText(@Body("collection") String collection,
                        @Body("id") String id,
                        @Body("text") String text,
                        Result<Boolean> result) throws LuceneException
  {
    checkCollection(collection);
    checkId(id);
    checkText(text);

    getLuceneWriter(id).indexText(collection, id, text, result);
  }

  @Override
  //@Post("/index-map")
  public void indexMap(@Body("collection") String collection,
                       @Body("id") String id,
                       @Body Form form,
                       Result<Boolean> result) throws LuceneException
  {
    checkCollection(collection);
    checkId(id);
    checkMap(form);

    getLuceneWriter(id).indexMap(collection, id, form, result);
  }

  @Override
  @Get("/search")
  public void search(@Query("collection") String collection,
                     @Query("query") String query,
                     @Query("limit") int limit,
                     Result<List<LuceneEntry>> result)
    throws LuceneException
  {
    checkCollection(collection);
    checkQuery(query);

    _indexReader.search(collection, query, result.of(x -> Arrays.asList(x)));
  }

  private void checkCollection(String collection)
  {
    if (collection == null || collection.isEmpty()) {
      throw new LuceneException("collection should have a value");
    }
  }

  private void checkId(String id)
  {
    if (id == null || id.isEmpty()) {
      throw new LuceneException("id|path should have a value");
    }
  }

  private void checkText(String text)
  {
    if (text == null) {
      throw new LuceneException("text should not be null");
    }
  }

  private void checkQuery(String query)
  {
    if (query == null || query.isEmpty()) {
      throw new LuceneException("query should have a value");
    }
  }

  private void checkMap(Form map)
  {
    if (map == null) {
      throw new LuceneException("map should not be null");
    }
  }

  @Override
  @Post("/delete")
  public void delete(@Body("collection") String collection,
                     @Body("id") String id,
                     Result<Boolean> result)
    throws LuceneException
  {
    getLuceneWriter(id).delete(collection, id, result);
  }

  @Override
  @Post
  public void clear(@Body("collection") String collection, Result<Void> result)
    throws LuceneException
  {
    ResultStreamBuilder<Void> builder = _indexWriter.clear(collection);

    builder.collect(ArrayList<Void>::new, (a, b) -> {}, (a, b) -> {
    }).result(result.of((c -> null)));
  }
}

package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.OnDestroy;
import io.baratine.core.Result;
import io.baratine.core.ResultSink;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;
import io.baratine.stream.StreamBuilder;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("pod://lucene/index")
public class LuceneIndexImpl implements LuceneIndex
{
  private static Logger log
    = Logger.getLogger(LuceneIndexImpl.class.getName());

  private ServiceManager _manager;

  private long _commitRate = TimeUnit.SECONDS.toMillis(10);

  @Inject
  @Lookup("/lucene-reader")
  private LuceneReader _indexReader;

  @Inject
  @Lookup("/lucene-writer")
  private LuceneIndexWriter _indexWriter;

  public LuceneIndexImpl()
    throws IOException
  {
  }

  @Override
  public void indexFile(final String collection,
                        final String path,
                        Result<Boolean> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexFile('%s')", path));

    _indexWriter.indexFile(collection, path, result);
  }

  @Override
  public void indexText(String collection,
                        String id,
                        String text,
                        Result<Boolean> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexText('%s')", id));

    _indexWriter.indexText(collection, id, text, result);
  }

  @Override
  public void indexMap(String collection,
                       String id,
                       Map<String,Object> map,
                       Result<Boolean> result) throws LuceneException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexMap('%1$s') %2$s", id, map));

    _indexWriter.indexMap(collection, id, map, result);
  }

  @Override
  public StreamBuilder search(String collection, String query)
  {
    throw new AbstractMethodError();
  }

  public void search(String collection,
                     String query,
                     ResultSink<LuceneEntry> results)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("search('%1$s', %2$s)", collection, query));

    _indexReader.search(collection, query, results.from((e, r) -> add(r, e)));
  }

  private void add(Result<LuceneEntry> result, LuceneEntry[] entries)
  {
    ResultSink<LuceneEntry> sink = (ResultSink<LuceneEntry>) result;

    for (LuceneEntry entry : entries) {
      sink.accept(entry);
    }

    sink.end();
  }

  @Override
  public void delete(String collection, final String id, Result<Boolean> result)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("delete('%s')", id));

    _indexWriter.delete(collection, id, result);
  }

  public void commit()
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("commit('%1$s')", this));

    _indexWriter.commit(Result.<Boolean>ignore());
  }

  @OnDestroy
  public void destroy() throws Exception
  {
    log.info("destroying " + this);
  }

  @Override
  public void clear(String collection, Result<Void> result)
  {
    log.finer(String.format("clear collection %1$s", collection));

    _indexWriter.clear(collection, result);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _manager + ']';
  }
}

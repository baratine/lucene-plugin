package com.caucho.lucene;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.caucho.lucene.bfs.BfsDirectory;
import com.caucho.v5.subsystem.RootDirectorySystem;
import com.caucho.v5.util.LruCache;
import io.baratine.files.BfsFileSync;
import io.baratine.service.Services;
import io.baratine.web.Form;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.SimpleMergedSegmentWarmer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.InfoStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.SAXException;

@Singleton
public final class LuceneIndexBean extends SearcherFactory
{
  private static final String externalKey = "__extKey__";
  private static final String collectionKey = "__collectionKey__";
  private static final String primaryKey = "__primary_key__";
  private static final Set<String> fieldSet
    = Collections.singleton(externalKey);

  private static Logger log = Logger.getLogger(LuceneIndexBean.class.getName());

  private final static long softCommitMaxDocs = 16;
  private final static long softCommitMaxAge = TimeUnit.SECONDS.toMillis(1);

  private Directory _directory;

  private IndexWriter _writer;

  private AutoDetectParser _parser;

  private String _indexDirectory;

  @Inject
  Services _manager;

  private StandardAnalyzer _analyzer;

  private double _maxMergeSizeMb = 4;
  private double _maxCacheMb = 48;

  private int _maxMergeAtOnce = 10;
  private double _segmentsPerTier = 10;

  private SearcherManager _searcherManager;

  private AtomicBoolean _isInitialized = new AtomicBoolean(false);

  private LruCache<String,Query> _queryCache = new LruCache<>(1024);

  private AtomicLong _updateSequence = new AtomicLong();
  private AtomicLong _searcherSequence = new AtomicLong();

  private AtomicLong _notFoundCounter = new AtomicLong();
  private long _lastUpdateSequence;

  private long _softCommitMaxDocs = softCommitMaxDocs;
  private long _softCommitMaxAge = softCommitMaxAge;
  private boolean _isUseBfs = !true;

  public LuceneIndexBean()
  {
    _parser = new AutoDetectParser();
  }

  private Services getManager()
  {
    if (_manager == null) {
      _manager = Services.current();
    }

    return _manager;
  }

  @PostConstruct
  public void init() throws IOException
  {
    log.finer(String.format("init("
                            + System.identityHashCode(this)
                            + ") "
                            + _manager));

    if (!_isInitialized.compareAndSet(false, true))
      return;

    String baratineData
      = RootDirectorySystem.getCurrentDataDirectory()
                           .toAbsolutePath()
                           .toString();

    int pod = 0;//getManager().getNode().getNodeIndex();

    _indexDirectory = baratineData
                      + File.separatorChar
                      + "lucene"
                      + File.separatorChar
                      + "node-" + pod;

    initIndexWriter();

    _searcherManager = new SearcherManager(getIndexWriter(), true, this);

    log.finer(String.format("%1$s init()", this));
  }

  public boolean isUseBfs()
  {
    return _isUseBfs;
  }

  public void setIsUseBfs(boolean isUseBfs)
  {
    _isUseBfs = isUseBfs;
  }

  @Override
  public IndexSearcher newSearcher(IndexReader reader,
                                   IndexReader previousReader)
    throws IOException
  {
    long v = _searcherSequence.incrementAndGet();

    BaratineIndexSearcher searcher = new BaratineIndexSearcher(reader, v);

    return searcher;
  }

  public AtomicLong getSearcherSequence()
  {
    return _searcherSequence;
  }

  private Term createPkTerm(String collection, String externalId)
  {
    return new Term(primaryKey, collection + ':' + externalId);
  }

  private StringField createPkField(String collection, String externalId)
  {
    return new StringField(primaryKey,
                           collection + ':' + externalId,
                           Field.Store.YES);
  }

  public boolean indexFile(String collection,
                           final String path)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexFile('%s')", path));

    collection = escape(collection);

    BfsFileSync file = getManager().service(path).as(BfsFileSync.class);

    Field extId = new StringField(externalKey, path, Field.Store.YES);

    StringField pkField = createPkField(collection, path);

    Term pkTerm = createPkTerm(collection, path);

    try (InputStream in = file.openRead()) {
      indexStream(collection, in, extId, pkField, pkTerm);

      return true;
    } catch (IOException e) {
      String message = String.format("failed to index file %1$s due to %2$s",
                                     file.toString(),
                                     e.toString());
      log.log(Level.FINER, message, e);

      throw LuceneException.create(e);
    }
  }

  private boolean indexStream(String collection,
                              InputStream in,
                              Field extId,
                              Field pkField,
                              Term pkTerm)
  {
    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(extId);
      document.add(pkField);
      document.add(new StringField(collectionKey, collection, Field.Store.YES));

      Metadata metadata = new Metadata();
      LuceneContentHandler handler = new LuceneContentHandler(document);

      _parser.parse(in, handler, metadata);

      for (String name : metadata.names()) {
        for (String value : metadata.getValues(name)) {
          document.add(new TextField(name, value, Field.Store.NO));
        }
      }
      //log.warning(String.format("indexing ('%1$s') ", pkTerm));
      writer.updateDocument(pkTerm, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%1$s')->('%2$s') complete",
                                pkTerm, extId.stringValue()));

      updateSequence();

      return true;
    } catch (IOException | SAXException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", extId.stringValue()), e);
      throw LuceneException.create(e);
    } catch (TikaException e) {
      log.log(Level.WARNING,
              String.format("indexing ('%s') failed", extId.stringValue()), e);

      LuceneException le = LuceneException.create(e);

      throw le;
    }
  }

  public boolean indexText(String collection,
                           String id,
                           String text)
  {
    int len = Integer.min(16, text.length());

    if (log.isLoggable(Level.FINER))
      log.finer(String.format("%1$s indexText('%2$s', '%3$s', '%4$s...')",
                              this,
                              collection,
                              id,
                              text.substring(0, len)));

    collection = escape(collection);

    Field extId = new StringField(externalKey, id, Field.Store.YES);

    Field pkField = createPkField(collection, id);

    Term pkTerm = createPkTerm(collection, id);

    ByteArrayInputStream stream
      = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

    boolean result = indexStream(collection, stream, extId, pkField, pkTerm);

    return result;
  }

  public boolean indexMap(String collection,
                          String id,
                          Form map) throws LuceneException
  {
    if (map.isEmpty()) {
      log.fine(String.format("indexMap('%s') empty map", id));

      return true;
    }

    collection = escape(collection);

    if (log.isLoggable(Level.FINER))
      log.finer(String.format("indexMap('%1$s') %2$s", id, map));

    Field extId = new StringField(externalKey, id, Field.Store.YES);

    Field pkField = createPkField(collection, id);

    Term pkTerm = createPkTerm(collection, id);

    try {
      IndexWriter writer = getIndexWriter();

      Document document = new Document();

      document.add(extId);
      document.add(pkField);
      document.add(new StringField(collectionKey, collection, Field.Store.YES));

      for (Map.Entry<String,List<String>> entry : map.entrySet()) {
        document.add(makeIndexableField(entry.getKey(),
                                        entry.getValue().get(0)));
      }

      writer.updateDocument(pkTerm, document);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("indexing ('%s') complete", extId));

      updateSequence();

      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, String.format("indexing ('%s') failed", extId), e);

      throw LuceneException.create(e);
    }
  }

  public LuceneEntry[] search(BaratineIndexSearcher searcher,
                              QueryParser queryParser,
                              String collection,
                              String query,
                              int limit)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(
        String.format("%1$s search('%2$s', %3$s, %4$d)",
                      this,
                      collection,
                      query,
                      limit));

    try {
      final String cacheKey = query + "::" + collection;

      LuceneEntry[] results = searcher.get(cacheKey);

      if (results != null)
        return results;

      Query q = _queryCache.get(cacheKey);

      if (q == null) {
        Query userQuery = queryParser.parse(query);

        BooleanQuery bq = new BooleanQuery();
        bq.add(userQuery, BooleanClause.Occur.MUST);

        collection = escape(collection);

        bq.add(new TermQuery(new Term(collectionKey, collection)),
               BooleanClause.Occur.MUST);

        q = bq;

        _queryCache.put(cacheKey, q);
      }

      TopDocs docs = searcher.search(q, limit);

      ScoreDoc[] scoreDocs = docs.scoreDocs;

      results = new LuceneEntry[scoreDocs.length];

      for (int i = 0; i < scoreDocs.length; i++) {
        ScoreDoc doc = scoreDocs[i];

        String key = searcher.externalId(doc.doc);

        if (key == null) {
          Document d = searcher.doc(doc.doc, fieldSet);
          key = d.get(externalKey);

          searcher.cacheExternalId(doc.doc, key);
        }

        LuceneEntry entry = new LuceneEntry(doc.doc,
                                            doc.score,
                                            key);

        results[i] = entry;
      }

      if (results.length > 0) {
        searcher.put(cacheKey, results);
      }
      else {
        long notFound = _notFoundCounter.incrementAndGet();

        if (notFound % 100 == 0) { //XXX: debug
          //log.warning(String.format("search not found %1$d", notFound));
        }
      }

      if (log.isLoggable(Level.FINER))
        log.finer(
          String.format("search('%1$s', %2$d with results %3$s)",
                        query,
                        limit,
                        Arrays.asList(results)));

      if (log.isLoggable(Level.FINEST))
        log.finest(
          String.format("search('%1$s', %2$d with results %3$s, %4$s)",
                        query,
                        limit,
                        Arrays.asList(results),
                        searcher + "@" + System.identityHashCode(searcher))
          + ": ");

      return results;
    } catch (AlreadyClosedException e) {
      log.log(Level.WARNING, String.format("%1$s searcher %2$s", e, searcher));
      throw LuceneException.create(e);
    } catch (Throwable t) {
      log.log(Level.WARNING, t.getMessage(), t);

      throw LuceneException.create(t);
    } finally {
    }
  }

  public boolean delete(String collection, final String id)
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%s')", id));

      collection = escape(collection);

      IndexWriter writer = getIndexWriter();

      Term pk = createPkTerm(collection, id);

      writer.deleteDocuments(pk);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("delete('%1$s'->'%2$s') complete",
                                pk, id));

      updateSequence();

      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw LuceneException.create(e);
    }
  }

  public void commit() throws IOException
  {
    if (_writer != null && _writer.hasUncommittedChanges()) {
      long delta = _lastUpdateSequence;

      _lastUpdateSequence = _updateSequence.get();

      delta = _lastUpdateSequence - delta;

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER,
                String.format(
                  "commit delta: [%1$d], updateSequence [%2$s], searcherSequence [%3$s]",
                  delta,
                  _updateSequence,
                  _searcherSequence));

      _writer.commit();
    }
  }

  public boolean clear(QueryParser queryParser, String collection)
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.finer(String.format("clear('%s')", collection));

      collection = escape(collection);

      IndexWriter writer = getIndexWriter();

      Query query = queryParser.parse(collectionKey + ':' + collection);

      writer.deleteDocuments(query);

      if (log.isLoggable(Level.FINER))
        log.finer(String.format("clear('%1$s') complete", query));

      updateSequence();

      return true;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw LuceneException.create(e);
    } catch (ParseException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw LuceneException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()
           + '['
           + _directory
           + ']'
           + "@"
           + System.identityHashCode(this);
  }

  private String escape(String collection)
  {
    StringBuilder builder = new StringBuilder();

    char[] buffer = collection.toCharArray();
    for (char c : buffer) {
      switch (c) {
      case '+':
      case '-':
      case '!':
      case '(':
      case ')':
      case '{':
      case '}':
      case '[':
      case ']':
      case '^':
      case '"':
      case '~':
      case '*':
      case '?':
      case ':':
      case '\\': {
        //XXX: escaping doesn't really work for some reason
        break;
      }
      default: {
        builder.append(c);
      }
      }
    }

    return builder.toString();
  }

  void release(BaratineIndexSearcher searcher) throws IOException
  {
    try {
      _searcherManager.release(searcher);
    } catch (NullPointerException e) {
      e.printStackTrace();
      log.warning(String.format("searcher manager is %1$s in %2$d %3$s",
                                _searcherManager,
                                System.identityHashCode(this),
                                this));

      throw e;
    }
  }

  private void updateSequence()
  {
    _updateSequence.incrementAndGet();
  }

  AtomicBoolean _isUpdating = new AtomicBoolean();

  public AtomicLong getUpdateSequence()
  {
    return _updateSequence;
  }

  public long getUpdatesCount()
  {
    return _updateSequence.get() - _lastUpdateSequence;
  }

  public BaratineIndexSearcher acquireSearcher() throws IOException
  {
    if (_searcherSequence.get() % 100 == 0)
      log.log(Level.INFO, "acquireSearcher: " + _searcherSequence.get());

    updateSearcher();

    return (BaratineIndexSearcher) _searcherManager.acquire();
  }

  public void updateSearcher()
  {
    try {
      _searcherManager.maybeRefresh();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
  }

  public long getSoftCommitMaxDocs()
  {
    return _softCommitMaxDocs;
  }

  public void setSoftCommitMaxDocs(long softCommitMaxDocs)
  {
    _softCommitMaxDocs = softCommitMaxDocs;
  }

  public long getSoftCommitMaxAge()
  {
    return _softCommitMaxAge;
  }

  public void setSoftCommitMaxAge(long softCommitMaxAge)
  {
    _softCommitMaxAge = softCommitMaxAge;
  }

  private IndexWriter getIndexWriter() throws IOException
  {
    return _writer;
  }

  private void initIndexWriter() throws IOException
  {
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

    iwc.setMergedSegmentWarmer(
      new SimpleMergedSegmentWarmer(new LoggingInfoStream(Level.FINER)));

    iwc.setReaderPooling(true);

//    iwc.setMergeScheduler(new SerialMergeScheduler());

    ConcurrentMergeScheduler mergeScheduler = new ConcurrentMergeScheduler();
    iwc.setMergeScheduler(mergeScheduler);

    TieredMergePolicy mergePolicy = new TieredMergePolicy();

    mergePolicy.setMaxMergeAtOnce(_maxMergeAtOnce);
    mergePolicy.setSegmentsPerTier(_segmentsPerTier);

    iwc.setMergePolicy(mergePolicy);

    iwc.setInfoStream(new LoggingInfoStream(Level.FINER));

    _writer = new IndexWriter(getDirectory(), iwc);
  }

  private Directory getDirectory() throws IOException
  {
    if (_directory == null)
      _directory = createDirectory();

    return _directory;
  }

  private Directory createDirectory() throws IOException
  {
    log.log(Level.FINER, "create new BfsDirectory");

    Directory directory;

    if (_isUseBfs) {
      directory = new BfsDirectory();
    }
    else {
      directory = MMapDirectory.open(getPath());
    }

    directory = new NRTCachingDirectory(directory,
                                        _maxMergeSizeMb,
                                        _maxCacheMb);
    return directory;
  }

  private Path getPath() throws IOException
  {
    Path path = FileSystems.getDefault().getPath(_indexDirectory, "index");

    return Files.createDirectories(path);
  }

  QueryParser createQueryParser()
  {
    if (_analyzer == null)
      _analyzer = new StandardAnalyzer();

    QueryParser queryParser = new QueryParser("text", _analyzer);

    return queryParser;
  }

  private IndexableField makeIndexableField(String name, Object obj)
  {
    IndexableField field = null;

    if (name == null) {
    }
    else if (obj == null) {
    }
    else {
      field = new TextField(name, String.valueOf(obj), Field.Store.NO);
    }

    return field;
  }
}

class LoggingInfoStream extends InfoStream
{
  private final static Logger log
    = Logger.getLogger(LoggingInfoStream.class.getName());

  private final Level level;

  public LoggingInfoStream(Level level)
  {
    this.level = level;
  }

  @Override
  public void message(String component, String message)
  {
    log.log(level, component + ": " + message);
  }

  @Override
  public boolean isEnabled(String component)
  {
    return log.isLoggable(level);
  }

  @Override
  public void close() throws IOException
  {

  }
}

class BaratineIndexSearcher extends IndexSearcher
{
  private final static Logger log
    = Logger.getLogger(BaratineIndexSearcher.class.getName());

  private LruCache<Integer,String> _keysCache
    = new LruCache<>(8 * 1024);

  private LruCache<String,LuceneEntry[]> _resultsCache = new LruCache<>(512);

  private final long _version;

  public BaratineIndexSearcher(IndexReader reader, long version)
  {
    super(reader);
    setQueryCache(null);
    _version = version;
  }

  public long getVersion()
  {
    return _version;
  }

  public String externalId(Integer key)
  {
    return _keysCache.get(key);
  }

  public void cacheExternalId(Integer key, String value)
  {
    _keysCache.put(key, value);
  }

  @Override
  public String toString()
  {
    IndexReader reader = getIndexReader();
    return "XIndexSearcher ["
           + reader.getClass().getSimpleName()
           + '@'
           + System.identityHashCode(reader)
           +
           ']';
  }

  public LuceneEntry[] get(String cacheKey)
  {
    return _resultsCache.get(cacheKey);
  }

  public void put(String cacheKey, LuceneEntry[] results)
  {
    _resultsCache.put(cacheKey, results);
  }
}

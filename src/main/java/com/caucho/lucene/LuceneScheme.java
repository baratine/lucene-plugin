package com.caucho.lucene;

import io.baratine.core.OnDestroy;
import io.baratine.core.OnLookup;
import io.baratine.core.Service;
import io.baratine.core.ServiceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class LuceneScheme
{
  private static Logger log
    = Logger.getLogger(LuceneScheme.class.getName());

  private Map<String,LuceneService> _map = new HashMap<>();

  private ServiceManager _manager;

  public LuceneScheme(ServiceManager manager)
  {
    _manager = manager;
  }

  @OnLookup
  public Object lookup(final String path) throws IOException
  {
    LuceneService lucene = _map.get(path);

    if (lucene == null) {
      String address = path.substring(path.lastIndexOf("///") + 3);

      lucene = new LuceneService(address, _manager);

      _map.put(path, lucene);
    }

    return lucene;
  }

  @OnDestroy
  public void destroy()
  {
    log.finer("destroying " + this);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _manager + ']';
  }
}

package com.caucho.lucene;

import io.baratine.core.ServiceManager;
import io.baratine.spi.ServiceProvider;

import java.util.logging.Logger;

public class LuceneServiceBindingProvider implements ServiceProvider
{
  private final static Logger log
    = Logger.getLogger(LuceneServiceBindingProvider.class.getName());

  @Override
  public void init(ServiceManager manager)
  {
    LuceneScheme scheme = new LuceneScheme(manager);

    log.finer("binding scheme " + scheme);

    manager.service(scheme).bind("lucene:");
  }
}

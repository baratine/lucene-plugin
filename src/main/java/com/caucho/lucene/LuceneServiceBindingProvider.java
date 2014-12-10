package com.caucho.lucene;

import io.baratine.core.ServiceManager;
import io.baratine.spi.ServiceBindingProvider;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LuceneServiceBindingProvider extends ServiceBindingProvider
{
  private final static Logger log
    = Logger.getLogger(LuceneServiceBindingProvider.class.getName());

  @Override
  public void init(ServiceManager manager)
  {
/*
    try {
      manager.service(new LuceneService()).bind("lucene:");
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
*/
  }
}

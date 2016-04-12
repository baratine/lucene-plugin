package com.caucho.lucene;

import static io.baratine.web.Web.include;
import static io.baratine.web.Web.start;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main
{
  public static void main(String[] args)
  {
    Logger.getLogger("com.caucho").setLevel(Level.FINER);
    include(LuceneWriterImpl.class);
    include(LuceneReaderImpl.class);
    include(LuceneFacadeImpl.class);
    include(SearcherUpdateServiceImpl.class);

    Logger.getLogger("com.caucho").setLevel(Level.FINER);
    start();
  }
}

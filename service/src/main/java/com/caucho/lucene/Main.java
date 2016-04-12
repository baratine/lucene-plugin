package com.caucho.lucene;

import static io.baratine.web.Web.include;
import static io.baratine.web.Web.start;

public class Main
{
  public static void main(String[] args)
  {
    include(LuceneWriterImpl.class);
    include(LuceneReaderImpl.class);
    include(LuceneFacadeImpl.class);
    include(SearcherUpdateServiceImpl.class);

    start();
  }
}

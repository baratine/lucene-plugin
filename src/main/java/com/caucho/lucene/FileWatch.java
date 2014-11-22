package com.caucho.lucene;

import io.baratine.core.Lookup;
import io.baratine.core.OnInit;
import io.baratine.core.ServiceManager;
import io.baratine.core.Startup;
import io.baratine.files.FileService;
import io.baratine.files.Watch;

import javax.inject.Inject;

@Startup
public class FileWatch implements Watch
{
  @Inject @Lookup("public:///lucene") LuceneService _searchService;
  @Inject ServiceManager _manager;

  private final String _root = "bfs:///indexed";

  @OnInit
  public void init()
  {
    System.out.println("FileWatch.init +");
    FileService f = _manager.lookup(_root).as(FileService.class);

    f.registerWatch(this);

    System.out.println("FileWatch.init -");
  }

  @Override
  public void onUpdate(String s)
  {
    try {
      //_searchService.update(s);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

package com.caucho.lucene;

import com.caucho.env.system.RootDirectorySystem;
import io.baratine.core.Journal;
import io.baratine.core.OnLookup;
import io.baratine.core.Service;
import io.baratine.core.Startup;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Service("pod://lucene/lucene-manager")
@Journal
@Startup
public class LuceneManagerImpl
{
  private static Logger log
    = Logger.getLogger(LuceneManagerImpl.class.getName());

  private String _indexDirectory;

  public LuceneManagerImpl()
  {
    String baratineData = RootDirectorySystem.getCurrentDataDirectory()
                                             .getFullPath();
    _indexDirectory = baratineData + File.separatorChar + "lucene";
  }

  public void setIndexDirectory(String indexDirectory)
  {
    indexDirectory = indexDirectory.replace('\\', '/');

    if (isAbsolute(indexDirectory)) {
      _indexDirectory = indexDirectory;
    }
    else if (_indexDirectory.endsWith(File.separator)) {
      _indexDirectory = _indexDirectory + indexDirectory;
    }
    else {
      _indexDirectory = _indexDirectory + File.separatorChar + indexDirectory;
    }
  }

  private boolean isAbsolute(String path)
  {

    if (path.indexOf('/') == 0
        || (Character.isLetter(path.charAt(0))
            && path.charAt(1) == ':'
            && path.charAt(2) == '/')) {
      return true;
    }

    return false;
  }

  @OnLookup
  public Object lookup(final String path) throws IOException
  {
    String address = path.substring(path.lastIndexOf("///") + 3);

    return new LuceneIndexImpl(address, _indexDirectory);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}

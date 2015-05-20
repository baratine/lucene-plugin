package com.caucho.lucene.bfs;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;

import java.io.IOException;
import java.util.Collection;

public class BFSDirectory extends Directory
{
  @Override
  public String[] listAll() throws IOException
  {
    return new String[0];
  }

  @Override
  public void deleteFile(String s) throws IOException
  {

  }

  @Override
  public long fileLength(String s) throws IOException
  {
    return 0;
  }

  @Override
  public IndexOutput createOutput(String s, IOContext ioContext)
    throws IOException
  {
    return null;
  }

  @Override
  public void sync(Collection<String> collection) throws IOException
  {

  }

  @Override
  public void renameFile(String s, String s1) throws IOException
  {

  }

  @Override
  public IndexInput openInput(String s, IOContext ioContext)
    throws IOException
  {
    return null;
  }

  @Override
  public Lock makeLock(String s)
  {
    return null;
  }

  @Override
  public void close() throws IOException
  {

  }
}

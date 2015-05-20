package com.caucho.lucene.bfs;

import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFileSync;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class BFSDirectory extends Directory
{

  BfsFileSync _root;

  public BFSDirectory()
  {
    ServiceManager manager = Services.getCurrentManager();

    int pod = manager.getPodNode().getNodeIndex();

    String path = String.format("bfs:///usr/lib/lucene/index/node-%1$s", pod);

    _root = manager.lookup(path).as(BfsFileSync.class);
  }

  @Override
  public String[] listAll() throws IOException
  {
    return _root.list();
  }

  @Override
  public void deleteFile(String s) throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    file.remove();
  }

  @Override
  public long fileLength(String s) throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    return file.getStatus().getLength();
  }

  @Override
  public IndexOutput createOutput(String s, IOContext ioContext)
    throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    return new BfsIndexOutput(s, file);
  }

  @Override
  public void sync(Collection<String> collection) throws IOException
  {
    System.out.println("BFSDirectory.sync " + collection);
  }

  @Override
  public void renameFile(String from, String to) throws IOException
  {
    throw new IOException(String.format("can't move file %1$s %2$s", from, to));
  }

  @Override
  public IndexInput openInput(String s, IOContext ioContext)
    throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    return new BfsIndexInput(s, file);
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

class BfsIndexOutput extends IndexOutput
{
  InputStream _in;

  BfsFileSync _file;

  public BfsIndexOutput(String resourceDescription, BfsFileSync file)
  {
    super(resourceDescription);
    _file = file;
  }

  @Override
  public void close() throws IOException
  {
    System.out.println("BfsIndexOutput.close");
  }

  @Override
  public long getFilePointer()
  {
    return 0;
  }

  @Override
  public long getChecksum() throws IOException
  {
    return 0;
  }

  @Override
  public void writeByte(byte b) throws IOException
  {
    System.out.println("BfsIndexOutput.writeByte " + b);
  }

  @Override
  public void writeBytes(byte[] bytes, int i, int i1)
    throws IOException
  {
    System.out.println("BfsIndexOutput.writeBytes " + bytes);
  }
}

class BfsIndexInput extends IndexInput
{
  BfsFileSync _file;

  public BfsIndexInput(String resourceDescription,
                       BfsFileSync file)
  {
    super(resourceDescription);
    _file = file;
  }

  @Override
  public void close() throws IOException
  {
    System.out.println("BfsIndexInput.close");
  }

  @Override
  public long getFilePointer()
  {
    return 0;
  }

  @Override
  public void seek(long l) throws IOException
  {

  }

  @Override
  public long length()
  {
    return 0;
  }

  @Override
  public IndexInput slice(String s, long l, long l1)
    throws IOException
  {
    return null;
  }

  @Override
  public byte readByte() throws IOException
  {
    return 0;
  }

  @Override
  public void readBytes(byte[] bytes, int i, int i1)
    throws IOException
  {

  }
}

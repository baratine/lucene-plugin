package com.caucho.lucene.bfs;

import com.caucho.lucene.LuceneException;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFileSync;
import io.baratine.files.Status;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.OutputStreamIndexOutput;
import org.apache.lucene.store.SingleInstanceLockFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BfsDirectory extends BaseDirectory
{
  private final static Logger log
    = Logger.getLogger(BfsDirectory.class.getName());

  private BfsFileSync _root;

  private Map<String,IndexOutput> _outputMap = new HashMap<>();

  public BfsDirectory()
  {
    super(new SingleInstanceLockFactory());

    ServiceManager manager = Services.getCurrentManager();

    int pod = manager.getPodNode().getNodeIndex();

    String path = String.format("bfs:///usr/lib/lucene/index/node-%1$s", pod);

    _root = manager.lookup(path).as(BfsFileSync.class);
  }

  @Override
  public String[] listAll() throws IOException
  {
    String[] list = _root.list();

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s listAll() -> %2$s", this,
                                         Arrays.asList(list)));

    return list;
  }

  @Override
  public void deleteFile(String s) throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, this + " delete " + s);

    BfsFileSync file = _root.lookup(s);

    file.remove();
  }

  @Override
  public long fileLength(String name) throws IOException
  {
    BfsFileSync file = _root.lookup(name);

    Status status = file.getStatus();

    long len = -1;

    if (status != null)
      len = status.getLength();

    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER,
              String.format("%1$s fileLength('%2$s') : %3$d", this, name, len));
    }

    return len;
  }

  @Override
  public IndexOutput createOutput(String s, IOContext ioContext)
    throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    IndexOutput out = new OutputStreamIndexOutput(s,
                                                  file.openWrite(),
                                                  1);// = new BfsIndexOutput(s, file);

    _outputMap.put(s, out);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s createOutput() -> %2$s",
                                         this, out));

    return out;
  }

  @Override
  public void sync(Collection<String> collection) throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s sync(%2$s)", this, collection));

    for (String s : collection) {
      _outputMap.get(s).getChecksum();
    }
  }

  @Override
  public void renameFile(String from, String to) throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s rename() %2$s -> %3$s",
                                         this, from, to));

    BfsFileSync fromFile = _root.lookup(from);
    BfsFileSync toFile = _root.lookup(to);

    try (ReadStream in = Vfs.openRead(fromFile.openRead());
         OutputStream out = toFile.openWrite()) {
      in.writeToStream(out);
    }

    fromFile.remove();
  }

  @Override
  public IndexInput openInput(String s, IOContext ioContext)
    throws IOException
  {
    BfsFileSync file = _root.lookup(s);

    long length = file.getStatus().getLength();

    IndexInput in = new BfsIndexInput(s, file, 0, length);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s openInput() -> %2$s", this, in));

    return in;
  }

  @Override
  public void close() throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s close()", this));

    this.isOpen = false;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()
           + '['
           + _root.getStatus().getPath()
           + ']';
  }
}

class BfsIndexInput extends IndexInput
{
  private final static Logger log
    = Logger.getLogger(BfsIndexInput.class.getName());

  private InputStream _in;
  private BfsFileSync _file;

  private final long _offset;
  private final long _length;
  private long _pointer = 0;

  public BfsIndexInput(String resourceDescription,
                       BfsFileSync file,
                       long offset,
                       long length) throws IOException
  {
    super(resourceDescription);
    _file = file;
    _offset = offset;
    _length = length;

    _in = file.openRead();
    _in.skip(_offset);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("new %1$s %2$d %3$s",
                                         this,
                                         _offset,
                                         length));
  }

  @Override
  public void close() throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s close()", this));

    _in.close();
  }

  @Override
  public long getFilePointer()
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s getFilePointer() %2$d",
                                         this,
                                         _pointer));

    return _pointer;
  }

  @Override
  public void seek(long pos) throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s seek(%2$d) %3$d",
                                         this,
                                         pos,
                                         _pointer));

    if (_pointer > pos) {
      _in.close();
      _in = _file.openRead();
      _in.skip(_offset + pos);
    }
    else {
      _in.skip(pos - _pointer);
    }

    _pointer = pos;
  }

  @Override
  public long length()
  {
    return _length;
  }

  @Override
  public IndexInput slice(String s, long offset, long length)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s slice %2$d:%3$d",
                                         this,
                                         offset,
                                         length));

    String description = String.format("%1$s | slice %2$d:%3$d",
                                       this,
                                       offset,
                                       length);

    BfsIndexInput slice
      = new BfsIndexInput(description, _file, _offset + offset, length);

    return slice;
  }

  @Override
  public byte readByte() throws IOException
  {
    byte b = (byte) _in.read();

    _pointer++;

    return b;
  }

  @Override
  public void readBytes(byte[] bytes, int offset, int len)
    throws IOException
  {
    _pointer += len;

    _in.read(bytes, offset, len);
  }

  @Override
  public IndexInput clone()
  {
    String description = String.format("%1$s | clone %2$d:%3$d:%4$d)",
                                       this, _offset, _pointer, _length);
    try {
      BfsIndexInput clone
        = new BfsIndexInput(description, _file, _offset, _length);

      //clone._pointer = _pointer;
      //clone._in.skip(_pointer);

      return clone;
    } catch (IOException e) {
      throw LuceneException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + super.toString() + ']';
  }
}

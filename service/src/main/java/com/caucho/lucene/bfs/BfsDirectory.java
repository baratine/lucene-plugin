package com.caucho.lucene.bfs;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFileSync;
import io.baratine.files.Status;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.BufferedIndexInput;
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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.baratine.files.WriteOption.Standard.CLOSE_WAIT_FOR_PUT;
import static io.baratine.files.WriteOption.Standard.OVERWRITE;

public class BfsDirectory extends BaseDirectory
{
  private final static Logger log
    = Logger.getLogger(BfsDirectory.class.getName());

  private BfsFileSync _root;

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

    OutputStream out = file.openWrite(OVERWRITE, CLOSE_WAIT_FOR_PUT);

    IndexOutput indexOut = new OutputStreamIndexOutput(s, out, 256);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s createOutput() -> %2$s",
                                         this, indexOut));

    return indexOut;
  }

  @Override
  public void sync(Collection<String> collection) throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s sync(%2$s)", this, collection));
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

    IndexInput in = new BfsIndexInput(s, ioContext, file, 0, length);

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER,
              String.format("%1$s openInput() -> %2$s", this, in));

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

class BfsIndexInput extends BufferedIndexInput
{
  private final static Logger log
    = Logger.getLogger(BfsIndexInput.class.getName());

  private InputStream _in;
  private BfsFileSync _file;
  private IOContext _context;

  private final long _offset;
  private final long _length;
  private long _pointer = 0;

  public BfsIndexInput(String resourceDescription,
                       IOContext context,
                       BfsFileSync file,
                       long offset,
                       long length) throws IOException
  {
    super(resourceDescription, context);
    _context = context;
    _file = file;
    _offset = offset;
    _length = length;

    _in = file.openRead();

    Objects.requireNonNull(_in, String.format("_in should not be null for %1$s",
                                              file));

    if (_in == null) {
      log.log(Level.WARNING, "");
    }

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
  protected void readInternal(byte[] bytes, int offset, int len)
    throws IOException
  {
    _pointer += len;
    int l;

    while ((l = _in.read(bytes, offset, len)) > 0) {
      offset = offset + l;
      len = len - l;
    }
  }

  @Override
  protected void seekInternal(long pos) throws IOException
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

    BfsIndexInput slice = new BfsIndexInput(description,
                                            _context,
                                            _file,
                                            _offset + offset,
                                            length);

    return slice;
  }

  @Override
  public BfsIndexInput clone()
  {
    try {
      BfsIndexInput clone;

      clone = (BfsIndexInput) super.clone();
      clone._in = _file.openRead();

      clone._in.skip(_offset);

      clone._pointer = 0;

      return clone;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + super.toString() + ']';
  }
}

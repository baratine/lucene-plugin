package com.caucho.lucene.bfs;

import io.baratine.core.ServiceManager;
import io.baratine.core.Services;
import io.baratine.files.BfsFileSync;
import io.baratine.files.Status;
import io.baratine.files.WriteOption;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.baratine.files.WriteOption.Standard.OVERWRITE;

public class BfsDirectory extends BaseDirectory
{
  private final static Logger log
    = Logger.getLogger(BfsDirectory.class.getName());

  private static int bufferSize = 8 * 1024;

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

    OutputStream out = file.openWrite(OVERWRITE);

    IndexOutput indexOut = new OutputStreamIndexOutput(s, out, bufferSize);

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

    fromFile.renameTo(to, WriteOption.Standard.OVERWRITE);
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

  private static Map<String,Integer> _counter = new HashMap<>();

  private InputStream _in;
  private BfsFileSync _file;
  private IOContext _context;

  private final long _offset;
  private final long _length;
  private long _pos = 0;
  private String _toString;

  private int _n;

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

    initCounter();

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("new %1$s %2$d %3$s",
                                         this,
                                         _offset,
                                         length));
  }

  void initCounter()
  {
    String path = _file.getStatus().getPath();
    Integer n = _counter.get(path);
    if (n == null)
      _counter.put(path, n = new Integer(0));
    else
      _counter.put(path, n = new Integer(n.intValue() + 1));

    _n = n;
  }

  @Override
  public void close() throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("%1$s close()", this));

    if (_in != null)
      _in.close();
  }

  @Override
  protected void readInternal(byte[] bytes, int offset, int len)
    throws IOException
  {
    skipOpen(_offset, 0);

    _pos += len;
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
      log.log(Level.FINER, String.format("seek %1$s pos: %2$d -> %3$d %4$s",
                                         this,
                                         _pos,
                                         pos,
                                         Thread.currentThread()));

    if (pos == _pos) {
    }
    else if (_pos > pos) {
      if (_in != null)
        _in.close();

      _in = null;

      skipOpen(_offset + pos, 0);
    }
    else {
      skipOpen(_offset, pos - _pos);
    }

    _pos = pos;
  }

  private void skipOpen(long inOffset, long offset) throws IOException
  {
    if (_in == null) {
      _in = _file.openRead();
      _in.skip(inOffset + offset);
    }
    else if (offset > 0) {
      _in.skip(offset);
    }
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
      log.log(Level.FINER, String.format("%1$s pos: %2$d slice %3$d:%4$d %5$s",
                                         this,
                                         _pos,
                                         offset,
                                         length,
                                         Thread.currentThread()));

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
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, String.format("clone %1$s pos: %2$d %3$s",
                                         this,
                                         _pos,
                                         Thread.currentThread()));

    BfsIndexInput clone;

    clone = (BfsIndexInput) super.clone();

    clone._in = null;

    clone._pos = 0;

    clone._toString = null;

    clone.initCounter();

    return clone;
  }

  @Override
  public String toString()
  {
    if (_toString == null)
      _toString = this.getClass().getSimpleName()
                  + '['
                  + super.toString()
                  + ':' + _n
                  + ']';

    return _toString;
  }
}

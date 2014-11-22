package com.caucho.lucene;

import java.util.concurrent.atomic.AtomicReference;

public class Monitor
{
  private AtomicReference<Status> _status
    = new AtomicReference<>(Status.WAITING);

  public void toComplete()
  {
    _status.set(Status.COMPLETE);
  }

  public void toWaiting()
  {
    _status.set(Status.WAITING);
  }

  public void waitForComplete() throws InterruptedException
  {
    while (!_status.get().isComplete()) {
      Thread.sleep(100);
    }
  }

  static enum Status
  {
    COMPLETE
      {
        @Override boolean isComplete()
        {
          return true;
        }
      },
    WAITING
      {
        @Override boolean isComplete()
        {
          return false;
        }
      };

    abstract boolean isComplete();
  }
}

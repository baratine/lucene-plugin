package com.caucho.lucene;

public class LuceneException extends RuntimeException
{
  public LuceneException()
  {
    super();
  }

  public LuceneException(String message)
  {
    super(message);
  }

  public LuceneException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public LuceneException(Throwable cause)
  {
    super(cause);
  }

  protected LuceneException(String message,
                            Throwable cause,
                            boolean enableSuppression,
                            boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public static LuceneException create(Throwable t)
  {
    return new LuceneException(t);
  }
}

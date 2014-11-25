package com.caucho.lucene;

public class RDoc
{
  private int _id;
  private float _score;
  private String _bfsPath;

  public RDoc()
  {
  }

  public RDoc(int id, float score)
  {
    this(id, score, null);
  }

  public RDoc(int id, float score, String bfsPath)
  {
    _id = id;
    _score = score;
    _bfsPath = bfsPath;
  }

  public int getId()
  {
    return _id;
  }

  public void setId(int id)
  {
    _id = id;
  }

  public float getScore()
  {
    return _score;
  }

  public void setScore(float score)
  {
    _score = score;
  }

  public String getBfsPath()
  {
    return _bfsPath;
  }

  public void setBfsPath(String bfsPath)
  {
    _bfsPath = bfsPath;
  }
}

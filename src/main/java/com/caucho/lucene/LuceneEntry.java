package com.caucho.lucene;

public class LuceneEntry
{
  private int _id;
  private float _score;
  private String _externalId;

  public LuceneEntry()
  {
  }

  public LuceneEntry(int id)
  {
    this(0, Float.MAX_VALUE);
  }

  public LuceneEntry(int id, float score)
  {
    this(id, score, null);
  }

  public LuceneEntry(int id, float score, String externalId)
  {
    _id = id;
    _score = score;
    _externalId = externalId;
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

  public String getExternalId()
  {
    return _externalId;
  }

  public void setExternalId(String externalId)
  {
    _externalId = externalId;
  }

  @Override public String toString()
  {
    return "LuceneEntry["
           + _id
           + ", "
           + _score
           + ", '"
           + _externalId
           + '\''
           + ']';
  }
}

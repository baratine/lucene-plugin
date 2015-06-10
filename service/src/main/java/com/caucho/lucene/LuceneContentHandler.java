package com.caucho.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LuceneContentHandler extends DefaultHandler
{
  private Document _document;

  private String _fieldName = "text";

  public LuceneContentHandler(Document document)
  {
    _document = document;
  }

  @Override
  public void characters(char[] ch, int start, int length)
    throws SAXException
  {
    _document.add(
      new TextField(_fieldName, new String(ch, start, length), Field.Store.NO));
  }
}

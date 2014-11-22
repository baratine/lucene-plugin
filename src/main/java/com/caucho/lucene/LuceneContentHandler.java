package com.caucho.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LuceneContentHandler extends DefaultHandler
{
  Document _document;

  public LuceneContentHandler(Document document)
  {
    _document = document;
  }

  @Override
  public void characters(char[] ch, int start, int length)
    throws SAXException
  {
    _document.add(new TextField("text", new String(ch, start, length),
                                Field.Store.NO));
  }
}

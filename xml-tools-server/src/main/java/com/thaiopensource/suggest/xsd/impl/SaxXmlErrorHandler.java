package com.thaiopensource.suggest.xsd.impl;

import org.apache.xerces.util.ErrorHandlerWrapper;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.ErrorHandler;

class SaxXmlErrorHandler extends ErrorHandlerWrapper {
  private boolean hadError = false;

  SaxXmlErrorHandler(ErrorHandler errorHandler) {
    super(errorHandler);
  }

  void reset() {
    hadError = false;
  }

  public void error(String domain, String key,
                    XMLParseException exception) throws XNIException {
    hadError = true;
    if (fErrorHandler == null)
      return;
    super.error(domain, key, exception);
  }

  public void warning(String domain, String key,
                      XMLParseException exception) throws XNIException {
    if (fErrorHandler == null)
      return;
    super.warning(domain, key, exception);
  }

  public void fatalError(String domain, String key,
                         XMLParseException exception) throws XNIException {
    hadError = true;
    if (fErrorHandler == null)
      return;
    super.fatalError(domain, key, exception);
  }

  boolean getHadError() {
    return hadError;
  }
}

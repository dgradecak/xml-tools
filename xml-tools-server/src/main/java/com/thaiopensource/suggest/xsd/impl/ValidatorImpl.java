package com.thaiopensource.suggest.xsd.impl;

import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.impl.XMLErrorReporter;
import org.apache.xerces.impl.msg.XMLMessageFormatter;
import org.apache.xerces.impl.validation.EntityState;
import org.apache.xerces.impl.validation.ValidationManager;
import org.apache.xerces.impl.xs.XMLSchemaValidator;
import org.apache.xerces.impl.xs.XSMessageFormatter;
import org.apache.xerces.util.*;
import org.apache.xerces.xni.*;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.parser.*;
import org.xml.sax.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class ValidatorImpl extends ParserConfigurationSettings implements Validator, ContentHandler, DTDHandler, XMLLocator, XMLEntityResolver, EntityState {

  private final XMLSchemaValidator schemaValidator = new XMLSchemaValidator();
  private final XMLErrorReporter errorReporter = new XMLErrorReporter();
  private final XMLEntityManager entityManager = new XMLEntityManager();
  private final ValidationManager validationManager = new ValidationManager();
  private final NamespaceContext namespaceContext = new NamespaceSupport();
  private final XMLAttributes attributes = new XMLAttributesImpl();
  private final SymbolTable symbolTable;
  private final XMLComponent[] components;
  private Locator locator;
  private final Set<String> entities = new HashSet<String>();
  private boolean pushedContext = false;

  // XXX deal with baseURI

  static private final String[] recognizedFeatures = {
    com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_AUGMENT_PSVI,
    com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_FULL_CHECKING,
    com.thaiopensource.suggest.xsd.impl.Features.VALIDATION,
    com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_VALIDATION,
  };

  static private final String[] recognizedProperties = {
    com.thaiopensource.suggest.xsd.impl.Properties.XMLGRAMMAR_POOL,
    com.thaiopensource.suggest.xsd.impl.Properties.SYMBOL_TABLE,
    com.thaiopensource.suggest.xsd.impl.Properties.ERROR_REPORTER,
    com.thaiopensource.suggest.xsd.impl.Properties.ERROR_HANDLER,
    com.thaiopensource.suggest.xsd.impl.Properties.VALIDATION_MANAGER,
    com.thaiopensource.suggest.xsd.impl.Properties.ENTITY_MANAGER,
    com.thaiopensource.suggest.xsd.impl.Properties.ENTITY_RESOLVER,
  };

  ValidatorImpl(SymbolTable symbolTable, XMLGrammarPool grammarPool, PropertyMap properties) {
    this.symbolTable = symbolTable;

    XMLErrorHandler errorHandlerWrapper = new ErrorHandlerWrapper(properties.get(ValidateProperty.ERROR_HANDLER));
    components = new XMLComponent[] { errorReporter, schemaValidator, entityManager };
    for (XMLComponent component : components) {
      addRecognizedFeatures(component.getRecognizedFeatures());
      addRecognizedProperties(component.getRecognizedProperties());
    }

    // addition provided by edankert@gmail.com at https://github.com/relaxng/jing-trang/issues/161
    if (errorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
      XMLMessageFormatter xmft = new XMLMessageFormatter();
      errorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
      errorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
    }
    if (errorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) ==
        null) {
      XSMessageFormatter xmft = new XSMessageFormatter();
      errorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, xmft);
    }

    addRecognizedFeatures(recognizedFeatures);
    addRecognizedProperties(recognizedProperties);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_AUGMENT_PSVI, false);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_FULL_CHECKING, true);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.VALIDATION, true);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.SCHEMA_VALIDATION, true);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.ID_IDREF_CHECKING, true);
    setFeature(com.thaiopensource.suggest.xsd.impl.Features.IDC_CHECKING, true);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.XMLGRAMMAR_POOL, grammarPool);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.SYMBOL_TABLE, symbolTable);
    errorReporter.setDocumentLocator(this);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.ERROR_REPORTER, errorReporter);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.ERROR_HANDLER, errorHandlerWrapper);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.VALIDATION_MANAGER, validationManager);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.ENTITY_MANAGER, entityManager);
    setProperty(com.thaiopensource.suggest.xsd.impl.Properties.ENTITY_RESOLVER, this);
    reset();
  }

  public void reset() {
    validationManager.reset();
    namespaceContext.reset();
    for (XMLComponent component : components) component.reset(this);
    validationManager.setEntityState(this);
  }

  public ContentHandler getContentHandler() {
    return this;
  }

  public DTDHandler getDTDHandler() {
    return this;
  }

  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  public void notationDecl(String name,
                           String publicId,
                           String systemId) {
    // nothing needed
  }

  public void unparsedEntityDecl(String name,
                                 String publicId,
                                 String systemId,
                                 String notationName) {
    entities.add(name);
  }

  public boolean isEntityDeclared(String name) {
    return entities.contains(name);
  }

  public boolean isEntityUnparsed(String name) {
    return entities.contains(name);
  }

  public void startDocument()
          throws SAXException {
    try {
      schemaValidator.startDocument(locator == null ? null : this, null, namespaceContext, null);
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void endDocument()
          throws SAXException {
    try {
      schemaValidator.endDocument(null);
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes atts)
          throws SAXException {
    try {
      if (!pushedContext)
        namespaceContext.pushContext();
      else
        pushedContext = false;
      for (int i = 0, len = atts.getLength(); i < len; i++)
        attributes.addAttribute(makeQName(atts.getURI(i), atts.getLocalName(i), atts.getQName(i)),
                                symbolTable.addSymbol(atts.getType(i)),
                                atts.getValue(i));
      schemaValidator.startElement(makeQName(namespaceURI, localName, qName), attributes, null);
      attributes.removeAllAttributes();
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void endElement(String namespaceURI, String localName,
                         String qName)
          throws SAXException {
    try {
      schemaValidator.endElement(makeQName(namespaceURI, localName, qName), null);
      namespaceContext.popContext();
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void startPrefixMapping(String prefix, String uri)
          throws SAXException {
    try {
      if (!pushedContext) {
        namespaceContext.pushContext();
        pushedContext = true;
      }
      if (prefix == null)
        prefix = XMLSymbols.EMPTY_STRING;
      else
        prefix = symbolTable.addSymbol(prefix);
      if (uri != null) {
        if (uri.equals(""))
          uri = null;
        else
          uri = symbolTable.addSymbol(uri);
      }
      namespaceContext.declarePrefix(prefix, uri);
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void endPrefixMapping(String prefix)
          throws SAXException {
    // do nothing
  }

  public void characters(char ch[], int start, int length)
          throws SAXException {
    try {
      schemaValidator.characters(new XMLString(ch, start, length), null);
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void ignorableWhitespace(char ch[], int start, int length)
          throws SAXException {
    try {
      schemaValidator.ignorableWhitespace(new XMLString(ch, start, length), null);
    }
    catch (XNIException e) {
      throw toSAXException(e);
    }
  }

  public void processingInstruction(String target, String data)
          throws SAXException {
    // do nothing
  }

  public void skippedEntity(String name)
          throws SAXException {
    // do nothing
  }

  private QName makeQName(String namespaceURI, String localName, String qName) {
    localName = symbolTable.addSymbol(localName);
    String prefix;
    if (namespaceURI.equals("")) {
      namespaceURI = null;
      prefix = XMLSymbols.EMPTY_STRING;
      qName = localName;
    }
    else {
      namespaceURI = symbolTable.addSymbol(namespaceURI);
      if (qName.equals("")) {
        prefix = namespaceContext.getPrefix(namespaceURI);
        if (prefix == XMLSymbols.EMPTY_STRING)
          qName = localName;
        else if (prefix == null)
          qName = localName; // XXX what to do?
        else
          qName = symbolTable.addSymbol(prefix + ":" + localName);
      }
      else {
        qName = symbolTable.addSymbol(qName);
        int colon = qName.indexOf(':');
        if (colon > 0)
          prefix = symbolTable.addSymbol(qName.substring(0, colon));
        else
          prefix = XMLSymbols.EMPTY_STRING;
      }
    }
    return new QName(prefix, localName, qName, namespaceURI);
  }

  public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
          throws XNIException, IOException {
    return null;
  }

  public String getPublicId() {
    return locator.getPublicId();
  }

  public String getEncoding() {
    return null;
  }

  public String getBaseSystemId() {
    return null;
  }

  public String getLiteralSystemId() {
    return null;
  }

  public String getExpandedSystemId() {
    return locator.getSystemId();
  }

  public int getLineNumber() {
    return locator.getLineNumber();
  }

  public int getColumnNumber() {
    return locator.getColumnNumber();
  }

  public int getCharacterOffset() {
    return -1;
  }

  public String getXMLVersion() {
    return "1.0";
  }
  
  static SAXException toSAXException(XNIException e) {
    if (e instanceof XMLParseException) {
      XMLParseException pe = (XMLParseException)e;
      return new SAXParseException(pe.getMessage(),
                                   pe.getPublicId(),
                                   pe.getExpandedSystemId(),
                                   pe.getLineNumber(),
                                   pe.getColumnNumber(),
                                   pe.getException());
    }
    Exception nested = e.getException();
    if (nested == null)
      return new SAXException(e.getMessage());
    if (nested instanceof SAXException)
      return (SAXException)nested;
    if (nested instanceof RuntimeException)
      throw (RuntimeException)nested;
    return new SAXException(nested);
  }
}

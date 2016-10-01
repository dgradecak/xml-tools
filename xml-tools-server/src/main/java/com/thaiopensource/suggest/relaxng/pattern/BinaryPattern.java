package com.thaiopensource.suggest.relaxng.pattern;

import org.xml.sax.SAXException;

abstract class BinaryPattern extends com.thaiopensource.suggest.relaxng.pattern.Pattern {
  final com.thaiopensource.suggest.relaxng.pattern.Pattern p1;
  final com.thaiopensource.suggest.relaxng.pattern.Pattern p2;

  BinaryPattern(boolean nullable, int hc, com.thaiopensource.suggest.relaxng.pattern.Pattern p1, com.thaiopensource.suggest.relaxng.pattern.Pattern p2) {
    super(nullable, Math.max(p1.getContentType(), p2.getContentType()), hc);
    this.p1 = p1;
    this.p2 = p2;
  }

  public void checkRecursion(int depth) throws SAXException {
    p1.checkRecursion(depth);
    p2.checkRecursion(depth);
  }

  public void checkRestrictions(int context, DuplicateAttributeDetector dad, com.thaiopensource.suggest.relaxng.pattern.Alphabet alpha)
    throws RestrictionViolationException {
    p1.checkRestrictions(context, dad, alpha);
    p2.checkRestrictions(context, dad, alpha);
  }

  boolean samePattern(com.thaiopensource.suggest.relaxng.pattern.Pattern other) {
    if (getClass() != other.getClass())
      return false;
    BinaryPattern b = (BinaryPattern)other;
    return p1 == b.p1 && p2 == b.p2;
  }

  com.thaiopensource.suggest.relaxng.pattern.Pattern getOperand1() {
    return p1;
  }

  Pattern getOperand2() {
    return p2;
  }
}

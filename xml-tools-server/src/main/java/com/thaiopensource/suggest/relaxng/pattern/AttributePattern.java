package com.thaiopensource.suggest.relaxng.pattern;

import com.thaiopensource.relaxng.edit.SourceLocation;
import org.xml.sax.SAXException;

class AttributePattern extends com.thaiopensource.suggest.relaxng.pattern.Pattern {
  private final com.thaiopensource.suggest.relaxng.pattern.NameClass nameClass;
  private final com.thaiopensource.suggest.relaxng.pattern.Pattern p;
  private final SourceLocation loc;

  AttributePattern(com.thaiopensource.suggest.relaxng.pattern.NameClass nameClass, com.thaiopensource.suggest.relaxng.pattern.Pattern value, SourceLocation loc) {
    super(false,
	  EMPTY_CONTENT_TYPE,
	  combineHashCode(ATTRIBUTE_HASH_CODE,
			  nameClass.hashCode(),
			  value.hashCode()));
    this.nameClass = nameClass;
    this.p = value;
    this.loc = loc;
  }

    public com.thaiopensource.suggest.relaxng.pattern.Pattern expand(SchemaPatternBuilder b) {
    com.thaiopensource.suggest.relaxng.pattern.Pattern ep = p.expand(b);
    if (ep != p) {
      com.thaiopensource.suggest.relaxng.pattern.Pattern expanded = b.makeAttribute(nameClass, ep, loc);
      expanded.setChildElementAnnotations(this.getChildElementAnnotations());
      expanded.setFollowingElementAnnotations(this.getFollowingElementAnnotations());
      return expanded;
    }
    else
      return this;
  }

  public void checkRestrictions(int context, DuplicateAttributeDetector dad, com.thaiopensource.suggest.relaxng.pattern.Alphabet alpha)
    throws RestrictionViolationException {
    switch (context) {
    case START_CONTEXT:
      throw new RestrictionViolationException("start_contains_attribute");
    case ELEMENT_CONTEXT:
      if (nameClass.isOpen())
	throw new RestrictionViolationException("open_name_class_not_repeated");
      break;
    case ELEMENT_REPEAT_GROUP_CONTEXT:
      throw new RestrictionViolationException("one_or_more_contains_group_contains_attribute");
    case ELEMENT_REPEAT_INTERLEAVE_CONTEXT:
      throw new RestrictionViolationException("one_or_more_contains_interleave_contains_attribute");
    case LIST_CONTEXT:
      throw new RestrictionViolationException("list_contains_attribute");
    case ATTRIBUTE_CONTEXT:
      throw new RestrictionViolationException("attribute_contains_attribute");
    case DATA_EXCEPT_CONTEXT:
      throw new RestrictionViolationException("data_except_contains_attribute");
    }
    dad.addAttribute(nameClass);
    try {
      p.checkRestrictions(ATTRIBUTE_CONTEXT, null, null);
    }
    catch (RestrictionViolationException e) {
      e.maybeSetLocator(loc);
      throw e;
    }
  }

  boolean samePattern(com.thaiopensource.suggest.relaxng.pattern.Pattern other) {
    if (!(other instanceof AttributePattern))
      return false;
    AttributePattern ap = (AttributePattern)other;
    return nameClass.equals(ap.nameClass)&& p == ap.p &&
        ap.getChildElementAnnotations() == getChildElementAnnotations() &&
        ap.getFollowingElementAnnotations() == getFollowingElementAnnotations();
  }

  public void checkRecursion(int depth) throws SAXException {
    p.checkRecursion(depth);
  }

  <T> T apply(com.thaiopensource.suggest.relaxng.pattern.PatternFunction<T> f) {
    return f.caseAttribute(this);
  }

  Pattern getContent() {
    return p;
  }

  NameClass getNameClass() {
    return nameClass;
  }

  SourceLocation getLocator() {
    return loc;
  }
}

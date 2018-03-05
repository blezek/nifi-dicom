/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * <p>An abstract class for representing a node in an internal representation of a structured reporting
 * tree (an instance of {@link com.pixelmed.dicom.StructuredReport StructuredReport}).</p>
 *
 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
 *
 * @see com.pixelmed.dicom.ContentItem
 * @see com.pixelmed.dicom.ContentItemFactory
 * @see com.pixelmed.dicom.ContentItemWithReference
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public abstract class ContentItemWithValue extends ContentItem {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItemWithValue.java,v 1.5 2017/01/24 10:50:36 dclunie Exp $";
	
	/***/
	protected String valueType;
	/***/
	protected CodedSequenceItem conceptName;
	/***/
	protected String referencedContentItemIdentifier;
	
	// Methods specific to this kind of node ...
	
	private void extractContentItemWithValueCommonAttributes() {
		valueType=Attribute.getSingleStringValueOrNull(list,TagFromName.ValueType);						// NB. Use null rather than default "" to make symmetric with de novo constructor
		conceptName=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
	}
	
	/**
	 * <p>Construct a content item for a list of attributes, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p	the parent
	 * @param	l	the list of attributes
	 */
	protected ContentItemWithValue(ContentItem p,AttributeList l) {
		super(p,l);
		extractContentItemWithValueCommonAttributes();
	}
	
	/**
	 * <p>Construct a content item of a specified type and relationship, creating a new {@link com.pixelmed.dicom.AttributeList AttributeList}, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p					the parent
	 * @param	valueType			value type
	 * @param	relationshipType	added only if not null or zero length
	 * @param	conceptName			coded concept name
	 * @throws	DicomException		if error in DICOM encoding
	 */
	protected ContentItemWithValue(ContentItem p,String valueType,String relationshipType,CodedSequenceItem conceptName) throws DicomException {
		super(p,relationshipType);
		this.valueType = valueType;
		{ Attribute a = new CodeStringAttribute(TagFromName.ValueType); a.addValue(valueType); list.put(a); }
		this.conceptName = conceptName;
		if (conceptName != null) {
			SequenceAttribute a = new SequenceAttribute(TagFromName.ConceptNameCodeSequence); a.addItem(conceptName.getAttributeList()); list.put(a);
		}
	}
	
	/**
	 * <p>Get the value type of this content item.</p>
	 *
	 * @return	the value type (the string used in the DICOM standard in the Value Type attribute)
	 */
	public String getValueType()                { return valueType; }
	
	/**
	 * <p>Get a string representation of the concept name and the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	public String getConceptNameAndValue() {
		return getConceptNameCodeMeaning()+" "+getConceptValue();
	}
	
	/**
	 * <p>Get a string representation of the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	abstract public String getConceptValue();
	
	/**
	 * <p>Get the Concept Name.</p>
	 *
	 * @return	the Concept Name
	 */
	public CodedSequenceItem getConceptName()      { return conceptName; }
	
	/**
	 * <p>Get the value of the code meaning of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code meaning of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeMeaning()      { return conceptName == null ? "" : conceptName.getCodeMeaning(); }
	
	/**
	 * <p>Get the value of the code value of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code value of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeValue()      { return conceptName == null ? "" : conceptName.getCodeValue(); }
	
	/**
	 * <p>Get the value of the coding scheme designator of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the coding scheme designator of the Concept Name, or an empty string
	 */
	public String getConceptNameCodingSchemeDesignator()      { return conceptName == null ? "" : conceptName.getCodingSchemeDesignator(); }
	
	/**
	 * <p>Get a human-readable string representation of the content item.</p>
	 *
	 * @return	the string representation of the content item
	 */
	public String toString() {
		return
		  (relationshipType == null ? "" : relationshipType) + ": "
		+ (valueType == null || valueType.length() == 0 ? "" : (valueType + ": "))
		+ (conceptName == null ? "" : conceptName.getCodeMeaning())
		;
	}

	// Convenience methods
		
	public boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (conceptName != null) {
			String csd = conceptName.getCodingSchemeDesignator();
			String cv = conceptName.getCodeValue();
			if (csd != null && csd.trim().equals(csdWanted.trim()) && cv != null && cv.trim().equals(cvWanted.trim())) {
				isMatch = true;
			}
		}
		return isMatch;
	}
}




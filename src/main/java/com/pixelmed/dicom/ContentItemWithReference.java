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
 * @see com.pixelmed.dicom.ContentItemWithValue
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public class ContentItemWithReference extends ContentItem {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItemWithReference.java,v 1.5 2017/01/24 10:50:36 dclunie Exp $";
	
	protected String referencedContentItemIdentifier;
	
	// Methods specific to this kind of node ...
	
	private void extractContentItemWithReferenceCommonAttributes() {
		referencedContentItemIdentifier=Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.ReferencedContentItemIdentifier).replace('\\','.');
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
	protected ContentItemWithReference(ContentItem p,AttributeList l) {
		super(p,l);
		extractContentItemWithReferenceCommonAttributes();
	}
	
	/**
	 * <p>Construct a content item of a specified type and relationship, creating a new {@link com.pixelmed.dicom.AttributeList AttributeList}, and add it as a child of the specified parent.</p>
	 *
	 * @param	p								the parent
	 * @param	relationshipType				added only if not null or zero length
	 * @param	referencedContentItemIdentifier	identifier of reference content item
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ContentItemWithReference(ContentItem p,String relationshipType,String referencedContentItemIdentifier) throws DicomException {
		super(p,relationshipType);
		this.referencedContentItemIdentifier = referencedContentItemIdentifier;
		if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
			Attribute a = new UnsignedLongAttribute(TagFromName.ReferencedContentItemIdentifier);
			list.put(a);
			int[] values = getReferencedContentItemIdentifierArray();
			for (int value : values) {
				a.addValue(value);
			}
		}
	}
	
	/**
	 * <p>Get a string representation of the value of the concept.</p>
	 *
	 * <p>Always returns an empty string for a {@link ContentItemWithReference ContentItemWithReference}.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	public String getConceptValue() { return ""; }
	
	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	the period (not backslash) delimited item references, or an empty string
	 */
	public String getReferencedContentItemIdentifier()      { return referencedContentItemIdentifier == null ? "" : referencedContentItemIdentifier; }
	
	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	an array of integers representing the separated components of the Referenced Content Item Identifier, including the first (root) identifier of 1, or null if none or empty
	 */
	public int[] getReferencedContentItemIdentifierArray() {
		int[] intArray = null;
		if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
			//String[] itemNumbers = referencedContentItemIdentifier.split("\\\\");
			String[] stringArray = referencedContentItemIdentifier.split("[.]");
			if (stringArray != null && stringArray.length > 0) {
				intArray = new int[stringArray.length];
				for (int i=0; i<stringArray.length; i++) {
					intArray[i] = Integer.parseInt(stringArray[i]);
				}
			}
		}
		return intArray;
	}
	
	/**
	 * <p>Get a human-readable string representation of the content item.</p>
	 *
	 * @return	the string representation of the content item
	 */
	public String toString() {
		return (referencedContentItemIdentifier == null || referencedContentItemIdentifier.length() == 0 ? "" : "R-")
		+ (relationshipType == null ? "" : relationshipType) + ": "
		+ (referencedContentItemIdentifier == null || referencedContentItemIdentifier.length() == 0 ? "" : referencedContentItemIdentifier)
		;
	}
	
	// Convenience methods
	
	public boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted) { return false; }
}




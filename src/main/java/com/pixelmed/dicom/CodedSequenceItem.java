/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A class to encapsulate the attributes contained within a Sequence Item that represents
 * a Coded Sequence item.</p>
 *
 * @author	dclunie
 */
public class CodedSequenceItem {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CodedSequenceItem.java,v 1.16 2017/01/24 10:50:36 dclunie Exp $";

	protected AttributeList list;
	
	/**
	 *
	 */
	public boolean equals(Object o) {
//System.err.println("CodedSequenceItem.equals():");
		boolean match = false;
		if (o instanceof CodedSequenceItem) {
//System.err.println("CodedSequenceItem.equals(): is CodedSequenceItem");
			match = list.equals(((CodedSequenceItem)o).getAttributeList());
		}
//System.err.println("CodedSequenceItem.equals(): match = "+match);
		return match;
	}
	
	/**
	 *
	 */
	public int hashCode() {
		return list.hashCode();
	}

	/**
	 * <p>Construct a <code>CodedSequenceItem</code> from a list of attributes.</p>
	 *
	 * @param	l	the list of attributes to include in the item
	 */
	public CodedSequenceItem(AttributeList l) {
		list=l;
	}

	/**
	 * <p>Construct a <code>CodedSequenceItem</code> from string values for code value, scheme and meaning.</p>
	 *
	 * @param	codeValue				the code value
	 * @param	codingSchemeDesignator	the coding scheme designator
	 * @param	codeMeaning				the code meaning
	 * @throws	DicomException			if error in DICOM encoding
	 */
	public CodedSequenceItem(String codeValue,String codingSchemeDesignator,String codeMeaning) throws DicomException {
		list = new AttributeList();
		{ Attribute a = new ShortStringAttribute(TagFromName.CodeValue); a.addValue(codeValue); list.put(a); }
		{ Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeDesignator); a.addValue(codingSchemeDesignator); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.CodeMeaning); a.addValue(codeMeaning); list.put(a); }
	}

	/**
	 * <p>Construct a <code>CodedSequenceItem</code> from string values for code value, scheme, version and meaning.</p>
	 *
	 * @param	codeValue				the code value
	 * @param	codingSchemeDesignator	the coding scheme designator
	 * @param	codingSchemeVersion		the coding scheme version
	 * @param	codeMeaning				the code meaning
	 * @throws	DicomException			if error in DICOM encoding
	 */
	public CodedSequenceItem(String codeValue,String codingSchemeDesignator,String codingSchemeVersion,String codeMeaning) throws DicomException {
		list = new AttributeList();
		{ Attribute a = new ShortStringAttribute(TagFromName.CodeValue); a.addValue(codeValue); list.put(a); }
		{ Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeDesignator); a.addValue(codingSchemeDesignator); list.put(a); }
		{ Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeVersion); a.addValue(codingSchemeVersion); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.CodeMeaning); a.addValue(codeMeaning); list.put(a); }
	}

	/**
	 * <p>Construct a <code>CodedSequenceItem</code> from a single string representation of the tuple enclosed in parentheses.</p>
	 *
	 * <p>I.e., "(cv,csd,cm)" or "(cv,csd,csv,cm)".</p>
	 *
	 * <p>The supplied tuple is expected to be enclosed in parentheses.</p>
	 *
	 * <p>Any items of the tuple may be enclosed in double quotes.</p>
	 *
	 * <p>White space is ignored (outside quoted strings".</p>
	 *
	 * @param	tuple			single string representation of the tuple enclosed in parentheses.
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public CodedSequenceItem(String tuple) throws DicomException {
		String codeValue = "";
		String codingSchemeDesignator = "";
		String codingSchemeVersion = "";
		String codeMeaning = "";
		
		Pattern pThreeTuple = Pattern.compile("[ ]*[(][ ]*\"?([^,\"]+)\"?[ ]*,[ ]*\"?([^,\"]+)\"?[ ]*,[ ]*\"?([^,\"]+)\"?[ ]*[)][ ]*");
		Matcher mThreeTuple = pThreeTuple.matcher(tuple);
		if (mThreeTuple.matches()) {
			int groupCount = mThreeTuple.groupCount();
			if (groupCount >= 3) {
				codeValue = mThreeTuple.group(1);				// NB. starts from 1 not 0
				codingSchemeDesignator = mThreeTuple.group(2);
				codeMeaning = mThreeTuple.group(3);
			}
		}
		else {
			Pattern pFourTuple = Pattern.compile("[ ]*[(][ ]*\"?([^,\"]+)\"?[ ]*,[ ]*\"?([^,\"]+)\"?[ ]*,[ ]*\"?([^,\"]+)\"?[ ]*,[ ]*\"?([^,\"]+)\"?[ ]*[)][ ]*");
			Matcher mFourTuple = pFourTuple.matcher(tuple);
			if (mFourTuple.matches()) {
				int groupCount = mFourTuple.groupCount();
				if (groupCount >= 4) {
					codeValue = mFourTuple.group(1);				// NB. starts from 1 not 0
					codingSchemeDesignator = mFourTuple.group(2);
					codingSchemeVersion = mFourTuple.group(3);
					codeMeaning = mFourTuple.group(4);
				}
			}
		}
		
		if (codeValue.length() > 0 && codingSchemeDesignator.length() > 0 && codeMeaning.length() > 0) {
			list = new AttributeList();
			{ Attribute a = new ShortStringAttribute(TagFromName.CodeValue); a.addValue(codeValue); list.put(a); }
			{ Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeDesignator); a.addValue(codingSchemeDesignator); list.put(a); }
			if (codingSchemeVersion.length() > 0) { Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeVersion); a.addValue(codingSchemeVersion); list.put(a); }
			{ Attribute a = new LongStringAttribute(TagFromName.CodeMeaning); a.addValue(codeMeaning); list.put(a); }
		}
		else {
			throw new DicomException("Unable to recognize pattern of tuple");
		}
	}

	/**
	 * <p>Get the list of attributes in the <code>CodedSequenceItem</code>.</p>
	 *
	 * @return	all the attributes in the <code>CodedSequenceItem</code>
	 */
	public AttributeList getAttributeList() { return list; }

	/**
	 * <p>Get the code value.</p>
	 *
	 * @return	a string containing the code value, or an empty string if none
	 */
	public String getCodeValue() {
		return Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodeValue);
	}

	/**
	 * <p>Get the coding scheme designator.</p>
	 *
	 * @return	a string containing the coding scheme designator, or an empty string if none
	 */
	public String getCodingSchemeDesignator() {
		return Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeDesignator);
	}

	/**
	 * <p>Get the coding scheme version.</p>
	 *
	 * @return	a string containing the coding scheme version, or an empty string if none
	 */
	public String getCodingSchemeVersion() {
		return Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeVersion);
	}

	/**
	 * <p>Get the code meaning.</p>
	 *
	 * @return	a string containing the code meaning, or an empty string if none
	 */
	public String getCodeMeaning() {
		return Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodeMeaning);
	}

	/**
	 * <p>Get a {@link java.lang.String String} representation of the contents of the <code>CodedSequenceItem</code>.</p>
	 *
	 * @return	a string containing the code value, coding scheme designator, coding scheme version (if present) and code meaning values
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("(");
		str.append(getCodeValue());
		str.append(",");
		str.append(getCodingSchemeDesignator());
		String version = getCodingSchemeVersion();
		if (version != null && version.length() > 0) {
			str.append(",");
			str.append(version);
		}
		str.append(",\"");
		str.append(getCodeMeaning());
		str.append("\")");
		return str.toString();
	}

	// Some static convenience methods ...

	/**
	 * <p>Extract the first (hopefully only) item of a coded sequence attribute contained
	 * within a list of attributes.</p>
	 *
	 * @param	list	the list in which to look for the Sequence attribute
	 * @param	tag	the tag of the Sequence attribute to extract
	 * @return		the (first) coded sequence item if found, otherwise null
	 */
	public static CodedSequenceItem getSingleCodedSequenceItemOrNull(AttributeList list,AttributeTag tag) {
		CodedSequenceItem value = null;
		if (list != null) {
			value = getSingleCodedSequenceItemOrNull(list.get(tag));
		}
		return value;
	}

	/**
	 * <p>Extract the first (hopefully only) item of a coded sequence attribute.</p>
	 *
	 * @param	a	the attribute
	 * @return		the (first) coded sequence item if found, otherwise null
	 */
	public static CodedSequenceItem getSingleCodedSequenceItemOrNull(Attribute a) {
		CodedSequenceItem value = null;
		if (a != null && a instanceof SequenceAttribute) {
			SequenceAttribute sa = (SequenceAttribute)(a);
			Iterator i = sa.iterator();
			if (i.hasNext()) {
				SequenceItem item = ((SequenceItem)i.next());
				if (item != null) value=new CodedSequenceItem(item.getAttributeList());
			}
		}
		return value;
	}

	/**
	 * <p>Extract the items of a coded sequence attribute contained
	 * within a list of attributes.</p>
	 *
	 * @param	list	the list in which to look for the Sequence attribute
	 * @param	tag	the tag of the Sequence attribute to extract
	 * @return		the coded sequence items if found, otherwise null
	 */
	public static CodedSequenceItem[] getArrayOfCodedSequenceItemsOrNull(AttributeList list,AttributeTag tag) {
		CodedSequenceItem[] values = null;
		if (list != null) {
			values = getArrayOfCodedSequenceItemsOrNull(list.get(tag));
		}
		return values;
	}

	/**
	 * <p>Extract the items of a coded sequence attribute.</p>
	 *
	 * @param	a	the attribute
	 * @return		the coded sequence items if found, otherwise null
	 */
	public static CodedSequenceItem[] getArrayOfCodedSequenceItemsOrNull(Attribute a) {
		ArrayList listOfItems = new ArrayList();
		if (a != null && a instanceof SequenceAttribute) {
			SequenceAttribute sa = (SequenceAttribute)(a);
			Iterator i = sa.iterator();
			if (i.hasNext()) {
				SequenceItem item = ((SequenceItem)i.next());
				if (item != null) {
					listOfItems.add(new CodedSequenceItem(item.getAttributeList()));
				}
			}
		}
		return listOfItems.size() == 0 ? null : (CodedSequenceItem[])(listOfItems.toArray());
	}
}


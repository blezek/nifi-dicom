/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * <p>The {@link com.pixelmed.dicom.DicomDictionaryBase DicomDictionaryBase} class
 * is an abstract class for creating and accessing a dictionary of DICOM
 * attributes and associated information.</p>
 *
 * <p>Defines methods for creating a dictionary of DICOM
 * attributes and associated information, and implements methods for accessing
 * that information.</p>
 *
 * @author	dclunie
 */
public abstract class DicomDictionaryBase {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDictionaryBase.java,v 1.11 2017/01/24 10:50:36 dclunie Exp $";

	protected TreeSet tagList;
	protected HashMap valueRepresentationsByTag;
	protected HashMap informationEntityByTag;
	protected HashMap nameByTag;
	protected HashMap tagByName;
	protected HashMap fullNameByTag;

	/**
	 * <p>Concrete sub-classes implement this method to create a list of all tags in the dictionary.</p>
	 */
	protected abstract void createTagList();

	/**
	 * <p>Concrete sub-classes implement this method to create a map of value representations for each tag in the dictionary.</p>
	 */
	protected abstract void createValueRepresentationsByTag();

	/**
	 * <p>Concrete sub-classes implement this method to create a map of information entities for each tag in the dictionary.</p>
	 */
	protected abstract void createInformationEntityByTag();

	/**
	 * <p>Concrete sub-classes implement this method to create a map of tags from attribute names for each tag in the dictionary.</p>
	 */
	protected abstract void createTagByName();
	
	/**
	 * <p>Concrete sub-classes implement this method to create a map of attribute names from tags for each tag in the dictionary.</p>
	 */
	protected abstract void createNameByTag();

	/**
	 * <p>Concrete sub-classes implement this method to create a map of attribute full names from tags for each tag in the dictionary.</p>
	 */
	protected abstract void createFullNameByTag();

	/**
	 * <p>Instantiate a dictionary by calling all create methods of the concrete sub-class.</p>
	 */
	public DicomDictionaryBase() {
//System.err.println("DicomDictionaryBase: constructing");
		createTagList();
		createValueRepresentationsByTag();
		createInformationEntityByTag();
		createNameByTag();
		createTagByName();
		createFullNameByTag();
	}

	/**
	 * <p>Get the value representation of an attribute.</p>
	 *
	 * @param	tag	the tag of the attribute
	 * @return		the value representation of the attribute as an array of two bytes
	 */
	public byte[] getValueRepresentationFromTag(AttributeTag tag) {
		byte[] vr = (byte[])valueRepresentationsByTag.get(tag);
//System.err.println("DicomDictionaryBase.getValueRepresentationFromTag: "+tag+" returns "+vr);
		if (vr == null) {
			if (tag.isRepeatingGroup()) {
				vr = (byte[])valueRepresentationsByTag.get(tag.getTagWithRepeatingGroupBase());
			}
		}
		return vr;
	}

	/**
	 * <p>Get the information entity (patient, study, and so on) of an attribute.</p>
	 *
	 * @param	tag	the tag of the attribute
	 * @return		the information entity of the attribute
	 */
	public InformationEntity getInformationEntityFromTag(AttributeTag tag) {
		return (InformationEntity)informationEntityByTag.get(tag);
	}

	/**
	 * <p>Get the tag of an attribute from its string name.</p>
	 *
	 * <p>Though the DICOM standard does not formally define names to be used as
	 * keys for attributes, the convention used here is to use the name from
	 * the PS 3.6 Name field and remove spaces, apostrophes, capitalize first
	 * letters of words and so on to come up with a unique name for each
	 * attribute.</p>
	 *
	 * @param	name	the string name of the attribute
	 * @return		the tag of the attribute
	 */
	public AttributeTag getTagFromName(String name) {
		return (AttributeTag)tagByName.get(name);
	}

	/**
	 * <p>Get the string name of an attribute from its tag.</p>
	 *
	 * @see #getTagFromName(String)
	 *
	 * @param	tag	the tag of the attribute
	 * @return		the string name of the attribute
	 */
	public String getNameFromTag(AttributeTag tag) {
		return (String)nameByTag.get(tag);
	}

	/**
	 * <p>Get the string full name of an attribute from its tag.</p>
	 *
	 * <p>The full name may not be unique, so do not use it as a key (e.g., "Group Length").</p>
	 *
	 * @param	tag	the tag of the attribute
	 * @return		the string full name of the attribute
	 */
	public String getFullNameFromTag(AttributeTag tag) {
		String fullName = (String)fullNameByTag.get(tag);
		if (fullName == null || fullName.length() == 0) {
			if (tag.isGroupLength()) {		// i.e., unless overridden by an actual dictionary entry
				fullName="Group Length";
			}
			else if (tag.isRepeatingGroup()) {
				fullName =(String)fullNameByTag.get(tag.getTagWithRepeatingGroupBase());
			}
		}
		return fullName;
	}

	/**
	 * <p>Get an {@link java.util.Iterator Iterator} to iterate through every tag in the dictionary.</p>
	 *
	 * <p>The order in which the dictionary attributes are returned is by ascending tag value.</p>
	 *
	 * @see com.pixelmed.dicom.AttributeTag#compareTo(Object)
	 *
	 * @return		an iterator
	 */
	public Iterator getTagIterator() { return tagList.iterator(); }

	/**
	 * <p>Unit test.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {

		try {
			DicomDictionaryBase dictionary = new DicomDictionary();
			System.err.println(new String(dictionary.getValueRepresentationFromTag(TagFromName.PixelRepresentation)));	// no need to use SLF4J since command line utility/test
			System.err.println(new String(dictionary.getValueRepresentationFromTag(new AttributeTag(0x0028,0x0103))));
			
			System.err.println(dictionary.getInformationEntityFromTag(TagFromName.PatientName));
			System.err.println(dictionary.getInformationEntityFromTag(TagFromName.StudyDate));
			System.err.println(dictionary.getInformationEntityFromTag(TagFromName.PixelRepresentation));

			System.err.println(dictionary.getNameFromTag(TagFromName.PatientName));
			System.err.println(dictionary.getNameFromTag(TagFromName.StudyDate));
			System.err.println(dictionary.getNameFromTag(TagFromName.PixelRepresentation));

			System.err.println(dictionary.getFullNameFromTag(TagFromName.PatientName));
			System.err.println(dictionary.getFullNameFromTag(TagFromName.StudyDate));
			System.err.println(dictionary.getFullNameFromTag(TagFromName.PixelRepresentation));
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}

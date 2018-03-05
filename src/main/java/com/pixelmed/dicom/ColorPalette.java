/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.ByteArray;

import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to encapsulate color palettes, including serialization and deserialization to and from standard DICOM color palette IODs.</p>
 *
 * <p>May be used as a base class for specific standard or private color palettes.</p>
 *
 * @author	dclunie
 */
public class ColorPalette {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ColorPalette.java,v 1.11 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ColorPalette.class);
	
	protected String sopInstanceUID = null;
	protected String contentLabel = null;
	protected String contentDescription = null;
	protected String contentCreatorName = null;
	protected String referenceEncodedInstanceURL = null;

	protected String[] alternateContentDescription = null;
	protected String[] alternateContentLanguageCodeValue = null;
	protected String[] alternateContentLanguageCodeMeaning = null;

	protected byte[] red;
	protected byte[] green;
	protected byte[] blue;
	
	protected byte[] iccProfile;
	
	protected AttributeList list = null;
	
	protected ColorPalette() {
	}
	
	public String getSOPInstanceUID() { return sopInstanceUID; }
	
	public String getContentLabel() { return contentLabel; }
	
	public String getContentDescription() { return contentDescription; }
	
	public String getReferenceEncodedInstanceURL() { return referenceEncodedInstanceURL; }
	
	public byte[] getICCProfile() { return iccProfile; }
	
	public void setICCProfileFromFile(String filename) throws IOException {
		iccProfile = ByteArray.readFully(filename);
	}
	
	public AttributeList getAttributeList() throws DicomException {
//System.err.println("ColorPalette.getAttributeList():");
		if (list == null) {
//System.err.println("ColorPalette.getAttributeList(): no existing list");
			if (red != null && green != null && blue != null) {
//System.err.println("ColorPalette.getAttributeList(): have byte arrays");
				list = new AttributeList();
				{
					Attribute a = new UnsignedShortAttribute(TagFromName.RedPaletteColorLookupTableDescriptor);
					a.addValue(red.length);	// number of entries
					a.addValue(0);	// first value mapped
					a.addValue(8);	// number of bits per entry
					list.put(a);
				}
				{
					Attribute a = new UnsignedShortAttribute(TagFromName.GreenPaletteColorLookupTableDescriptor);
					a.addValue(green.length);	// number of entries
					a.addValue(0);	// first value mapped
					a.addValue(8);	// number of bits per entry
					list.put(a);
				}
				{
					Attribute a = new UnsignedShortAttribute(TagFromName.BluePaletteColorLookupTableDescriptor);
					a.addValue(blue.length);	// number of entries
					a.addValue(0);	// first value mapped
					a.addValue(8);	// number of bits per entry
					list.put(a);
				}
				{
					Attribute a = new OtherWordAttribute(TagFromName.RedPaletteColorLookupTableData);
					a.setValues(ArrayCopyUtilities.packByteArrayIntoShortArrayLittleEndian(red));
					list.put(a);
				}
				{
					Attribute a = new OtherWordAttribute(TagFromName.GreenPaletteColorLookupTableData);
					a.setValues(ArrayCopyUtilities.packByteArrayIntoShortArrayLittleEndian(green));
					list.put(a);
				}
				{
					Attribute a = new OtherWordAttribute(TagFromName.BluePaletteColorLookupTableData);
					a.setValues(ArrayCopyUtilities.packByteArrayIntoShortArrayLittleEndian(blue));
					list.put(a);
				}
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.ColorPaletteStorage); list.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(sopInstanceUID); list.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.PaletteColorLookupTableUID); a.addValue(sopInstanceUID); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.ContentLabel); a.addValue(contentLabel); list.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.ContentDescription); a.addValue(contentDescription); list.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue("1"); list.put(a); }
				{ Attribute a = new PersonNameAttribute(TagFromName.ContentCreatorName); a.addValue(contentCreatorName); list.put(a); }
				{
					java.util.Date currentDateTime = new java.util.Date();
					{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate); a.addValue(new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime)); list.put(a); }
					{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime); a.addValue(new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime)); list.put(a); }
					{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC); a.addValue(DateTimeAttribute.getTimeZone(java.util.TimeZone.getDefault(),currentDateTime)); list.put(a); }
				}
				if (iccProfile != null && iccProfile.length > 0) {
//System.err.println("ColorPalette.getAttributeList(): adding ICCProfile");
					Attribute a = new OtherByteAttribute(TagFromName.ICCProfile);
					a.setValues(iccProfile);
					list.put(a);
				}
				if (alternateContentDescription != null && alternateContentLanguageCodeValue != null && alternateContentLanguageCodeMeaning != null) {
					SequenceAttribute acds = null;
					for (int i=0; i < alternateContentDescription.length && i < alternateContentLanguageCodeValue.length && i < alternateContentLanguageCodeMeaning.length; ++i) {
						if (acds == null) {
							acds = new SequenceAttribute(TagFromName.AlternateContentDescriptionSequence);
							list.put(acds);
						}
						AttributeList acdsItemList = new AttributeList();
						{ Attribute a = new LongStringAttribute(TagFromName.ContentDescription); a.addValue(alternateContentDescription[i]); acdsItemList.put(a); }
						{
							SequenceAttribute alcs = new SequenceAttribute(TagFromName.LanguageCodeSequence);
							CodedSequenceItem langCSI = new CodedSequenceItem(alternateContentLanguageCodeValue[i],"RFC3066",alternateContentLanguageCodeMeaning[i]);
							alcs.addItem(langCSI.getAttributeList());
							acdsItemList.put(alcs);
						}
						acds.addItem(acdsItemList);
					}
				}
				list.insertSuitableSpecificCharacterSetForAllStringValues();
			}
		}
		return list;
	}
	
	/**
	 * <p>Create a DICOM color palette storage instance from the palette characteristics.</p>
	 *
	 * @param	dicomFileName		to write
	 * @param	iccProfileFileName	to read
	 * @param	aet					our Application Entity Title to include in the metaheader
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	protected void createDICOMInstance(String dicomFileName,String iccProfileFileName,String aet) throws IOException, DicomException {
		if (iccProfileFileName != null) {
//System.err.println("ColorPalette.createDICOMInstance(): Reading ICC profile from file "+iccProfileFileName);
			setICCProfileFromFile(iccProfileFileName);		// Need to do this BEFORE getting AttributeList
		}
		AttributeList list = getAttributeList();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,aet);
//System.err.println(list);
		slf4jlogger.info("createDICOMInstance(): Writing palette to file {}",dicomFileName);
		list.write(dicomFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
}

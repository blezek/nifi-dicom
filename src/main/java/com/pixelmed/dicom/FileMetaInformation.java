/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.HexDump;

import java.io.*;

/**
 * <p>A class to abstract the contents of a file meta information header as used for a
 * DICOM PS 3.10 file, with additional static methods to add to and extract from an
 * existing list of attributes.</p>
 *
 * @author	dclunie
 */
public class FileMetaInformation {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/FileMetaInformation.java,v 1.15 2017/01/24 10:50:37 dclunie Exp $";
	
	private static final AttributeTag groupLengthTag = new AttributeTag(0x0002,0x0000);

	private AttributeList list;
	
	/**
	 * <p>Construct an instance of the  file meta information from the specified parameters.</p>
	 *
	 * @param	mediaStorageSOPClassUID			the SOP Class UID of the dataset to which the file meta information will be prepended
	 * @param	mediaStorageSOPInstanceUID		the SOP Instance UID of the dataset to which the file meta information will be prepended
	 * @param	transferSyntaxUID				the transfer syntax UID that will be used to write the dataset
	 * @param	sourceApplicationEntityTitle	the source AE title of the dataset (may be null)
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public FileMetaInformation(String mediaStorageSOPClassUID,String mediaStorageSOPInstanceUID,String transferSyntaxUID,String sourceApplicationEntityTitle) throws DicomException {
		list=new AttributeList();
		addFileMetaInformation(list,mediaStorageSOPClassUID,mediaStorageSOPInstanceUID,transferSyntaxUID,sourceApplicationEntityTitle);
	}
	
	/**
	 * <p>Add the file meta information attributes to an existing list, using
	 * only the parameters supplied.</p>
	 *
	 * <p>Note that the appropriate (mandatory) file meta information group length tag is also computed and added.</p>
	 *
	 * @param	list							the list to be extended with file meta information attributes
	 * @param	mediaStorageSOPClassUID			the SOP Class UID of the dataset to which the file meta information will be prepended
	 * @param	mediaStorageSOPInstanceUID		the SOP Instance UID of the dataset to which the file meta information will be prepended
	 * @param	transferSyntaxUID				the transfer syntax UID that will be used to write the dataset
	 * @param	sourceApplicationEntityTitle	the source AE title of the dataset (may be null)
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public static void addFileMetaInformation(AttributeList list,
			String mediaStorageSOPClassUID,String mediaStorageSOPInstanceUID,String transferSyntaxUID,String sourceApplicationEntityTitle) throws DicomException {
		int gl = 0;
		int shortVL = (4+2+2);	// Two byte VL in EVR
		int longVL = (4+4+4);	// Four byte VL in EVR (OB)

		{ AttributeTag t = TagFromName.FileMetaInformationVersion;   Attribute a = new OtherByteAttribute(t);         byte[] b=new byte[2]; b[0]=0x00; b[1]=0x01; a.setValues(b); list.put(t,a); gl+=a.getPaddedVL(); gl += longVL; }
		{ AttributeTag t = TagFromName.MediaStorageSOPClassUID;      Attribute a = new UniqueIdentifierAttribute(t);  a.addValue(mediaStorageSOPClassUID);                        list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL; }
		{ AttributeTag t = TagFromName.MediaStorageSOPInstanceUID;   Attribute a = new UniqueIdentifierAttribute(t);  a.addValue(mediaStorageSOPInstanceUID);                     list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL; }
		{ AttributeTag t = TagFromName.TransferSyntaxUID;            Attribute a = new UniqueIdentifierAttribute(t);  a.addValue(transferSyntaxUID);                              list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL; }
		{ AttributeTag t = TagFromName.ImplementationClassUID;       Attribute a = new UniqueIdentifierAttribute(t);  a.addValue(VersionAndConstants.implementationClassUID);     list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL; }
		{ AttributeTag t = TagFromName.ImplementationVersionName;    Attribute a = new ShortStringAttribute(t,null);  a.addValue(VersionAndConstants.implementationVersionName);  list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL; }
		if (sourceApplicationEntityTitle != null && sourceApplicationEntityTitle.length() > 0) {
			AttributeTag t = TagFromName.SourceApplicationEntityTitle; Attribute a = new ApplicationEntityAttribute(t); a.addValue(sourceApplicationEntityTitle);                 list.put(t,a); gl+=a.getPaddedVL(); gl += shortVL;
		}

		{ AttributeTag t = groupLengthTag; Attribute a = new UnsignedLongAttribute(t); a.addValue(gl); list.put(t,a); }
	}
	
	
	/**
	 * <p>Add the file meta information attributes to an existing list, extracting
	 * the known UIDs from that list, and adding the additional parameters supplied.</p>
	 *
	 * @param	list							the list to be extended with file meta information attributes
	 * @param	transferSyntaxUID				the transfer syntax UID that will be used to write this list
	 * @param	sourceApplicationEntityTitle	the source AE title of the dataset in the list (may be null)
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public static void addFileMetaInformation(AttributeList list,String transferSyntaxUID,String sourceApplicationEntityTitle) throws DicomException {
		String mediaStorageSOPClassUID = null;
		Attribute aSOPClassUID = list.get(TagFromName.SOPClassUID);
		if (aSOPClassUID != null) {
			mediaStorageSOPClassUID = aSOPClassUID.getSingleStringValueOrNull();
		}
		
		String mediaStorageSOPInstanceUID = null;
		Attribute aSOPInstanceUID = list.get(TagFromName.SOPInstanceUID);
		if (aSOPInstanceUID != null) {
			mediaStorageSOPInstanceUID = aSOPInstanceUID.getSingleStringValueOrNull();
		}
		if (mediaStorageSOPClassUID == null && mediaStorageSOPInstanceUID == null && list.get(TagFromName.DirectoryRecordSequence) != null) {
			// is a DICOMDIR, so use standard SOP Class and make up a UID
			mediaStorageSOPClassUID=SOPClass.MediaStorageDirectoryStorage;
			mediaStorageSOPInstanceUID=new UIDGenerator().getNewUID();
		}
		
		if (mediaStorageSOPClassUID == null) {
			throw new DicomException("Could not add File Meta Information - missing or empty SOPClassUID and not a DICOMDIR");
		}
		if (mediaStorageSOPInstanceUID == null) {
			throw new DicomException("Could not add File Meta Information - missing or empty SOPInstanceUID and not a DICOMDIR");
		}
		
		addFileMetaInformation(list,mediaStorageSOPClassUID,mediaStorageSOPInstanceUID,transferSyntaxUID,sourceApplicationEntityTitle);
	}

	/**
	 * <p>Get the attribute list in this instance of the file meat information.</p>
	 *
	 * @return	the attribute list
	 */
	public AttributeList getAttributeList() { return list; }
	
	/**
	 * <p>For testing.</p>
	 *
	 * <p>Generate a dummy file meta information header and test reading and writing it.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {

		try {
			AttributeList list = new FileMetaInformation("1.2.3.44","1.2",TransferSyntax.Default,"MYAE").getAttributeList();
			System.err.println("As constructed:");	// no need to use SLF4J since command line utility/test
			System.err.print(list);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			list.write(new DicomOutputStream(bout,TransferSyntax.ExplicitVRLittleEndian,null));
			byte[] b = bout.toByteArray();
			System.err.print(HexDump.dump(b));
			AttributeList rlist = new AttributeList();
			rlist.read(new DicomInputStream(new ByteArrayInputStream(b),TransferSyntax.ExplicitVRLittleEndian,true));
			System.err.println("As read:");
			System.err.print(rlist);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}

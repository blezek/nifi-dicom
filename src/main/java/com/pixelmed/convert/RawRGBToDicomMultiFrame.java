/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFilesOnDisk;
import com.pixelmed.dicom.OtherByteAttributeMultipleFilesOnDisk;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.UnsignedShortAttribute;
import com.pixelmed.dicom.VersionAndConstants;

//import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.FilenameFilterByCaseInsensitiveSuffix;

//import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.Arrays;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting raw RGB color single frame image input format files (such as from Visible Human) into one or more DICOM multi-frame secondary capture images.</p>
 *
 * @author	dclunie
 */

public class RawRGBToDicomMultiFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/RawRGBToDicomMultiFrame.java,v 1.10 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RawRGBToDicomMultiFrame.class);
	
	/**
	 * <p>Read a per-frame and shared functional group sequences for the geometry defined in a raw image file description.</p>
	 *
	 * @param	rawRGBInfo		description of the size of the raw image
	 * @param	list			an existing (possibly empty) attribute list, if null, a new one will be created; may already shared and per-frame functional group sequences or they will be added
	 * @param	numberOfFrames
	 * return					attribute list with per-frame and shared functional group sequences for geometry added
	 * @throws				DicomException
	 */
	public static AttributeList generateGeometryFunctionalGroupsFromRawRGBInformation(RawRGBInformation rawRGBInfo,AttributeList list,int numberOfFrames) throws DicomException {
		if (list == null) {
			list = new AttributeList();
		}

		SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
		if (aSharedFunctionalGroupsSequence == null) {
			aSharedFunctionalGroupsSequence = new SequenceAttribute(TagFromName.SharedFunctionalGroupsSequence);
			list.put(aSharedFunctionalGroupsSequence);
			aSharedFunctionalGroupsSequence.addItem(new AttributeList());
		}
		AttributeList sharedFunctionalGroupsSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(aSharedFunctionalGroupsSequence);

		SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
		if (aPerFrameFunctionalGroupsSequence == null) {
			aPerFrameFunctionalGroupsSequence = new SequenceAttribute(TagFromName.PerFrameFunctionalGroupsSequence);
			list.put(aPerFrameFunctionalGroupsSequence);
			for (int f=0; f<numberOfFrames; ++f) {
				aPerFrameFunctionalGroupsSequence.addItem(new AttributeList());
			}
		}
		{
			SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
			sharedFunctionalGroupsSequenceList.put(aPixelMeasuresSequence);
			AttributeList itemList = new AttributeList();
			aPixelMeasuresSequence.addItem(itemList);

			// note that order in DICOM in PixelSpacing is "adjacent row spacing", then "adjacent column spacing" ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(rawRGBInfo.pixelHeightInMillimetres); a.addValue(rawRGBInfo.pixelWidthInMillimetres); itemList.put(a); }
			// note that slice spacing == slice thickness ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.SliceThickness); a.addValue(rawRGBInfo.pixelSeparationInMillimetres); itemList.put(a); }
		}

		{
			SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
			sharedFunctionalGroupsSequenceList.put(aPlaneOrientationSequence);
			AttributeList itemList = new AttributeList();
			aPlaneOrientationSequence.addItem(itemList);
			{
				Attribute a = new DecimalStringAttribute(TagFromName.ImageOrientationPatient);
				// assume always axial LA+ (prone viewed from top) because that's what VHM images are
				a.addValue(1);
				a.addValue(0);
				a.addValue(0);

				a.addValue(0);
				a.addValue(-1);
				a.addValue(0);
			
				itemList.put(a);
			}
		}
		
		{
			for (int f=0; f<numberOfFrames; ++f) {
				SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
				SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,f).put(aPlanePositionSequence);

				AttributeList itemList = new AttributeList();
				aPlanePositionSequence.addItem(itemList);
				Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
					
				// assume always axial LAI+ (prone viewed from top starting from top slice) because that's what VHM images are
				a.addValue(0);
				a.addValue(0);
				a.addValue(-rawRGBInfo.pixelSeparationInMillimetres * f);
				
				itemList.put(a);
			}
		}
		return list;
	}
	
	/**
	 * <p>Create DICOM Pixel Data Module attributes.</p>
	 *
	 * @param	inputFileNames		the single frame raw RGB format image files sorted in the correct order
	 * @param	inputFileNameSuffix	so we can tell whether or not the input files are compressed
	 * @param	rawRGBInfo			description of the size of the raw image already read from the inputFile
	 * @param	list				an existing (possibly empty) attribute list, if null, a new one will be created; may already include "better" image pixel module attributes to use
	 * return						attribute list with Image Pixel Module (including Pixel Data) and other attributes added
	 * @throws					IOException
	 * @throws					DicomException
	 */
	public static AttributeList generateDICOMPixelDataModuleAttributesFromRawRGBFiles(String[] inputFileNames,String inputFileNameSuffix,RawRGBInformation rawRGBInfo,AttributeList list) throws IOException, DicomException {
		if (list == null) {
			list = new AttributeList();
		}
		
		int numberOfInputFiles = inputFileNames.length;
		
		int columns = rawRGBInfo.imageWidthInPixels;
		int rows = rawRGBInfo.imageHeightInPixels;
		int numberOfFrames = numberOfInputFiles;
		
		String photometricInterpretation = rawRGBInfo.photometricInterpretation;
		int samplesPerPixel = 3;
		int depth = rawRGBInfo.bitsPerPixel / samplesPerPixel;
		int planarConfiguration = rawRGBInfo.planarConfiguration.equals("NON INTERLEAVED") ? 1 : 0;
		boolean signed = false;
		
		{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a); }

		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(depth); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(depth); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(depth-1); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Rows); a.addValue(rows); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Columns); a.addValue(columns); list.put(a); }
			
		{ Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation); a.addValue(signed ? 1 : 0); list.put(a); }

		list.remove(TagFromName.NumberOfFrames);
		if (numberOfFrames > 1) {
			Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); list.put(a);
		}
			
		{ Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel); a.addValue(samplesPerPixel); list.put(a); }
						
		{ Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue(planarConfiguration); list.put(a); }	// always chunky pixel

		{
			Attribute aPixelData = null;
			long unpaddedValueLength = (long)numberOfFrames * rows * columns *samplesPerPixel;	// NB. Must upcast to long before calculating, else performed as int!
			long paddedValueLength = (unpaddedValueLength%2 != 0) ? (unpaddedValueLength+1): unpaddedValueLength;
			slf4jlogger.info("Value length unpadded = {}, padded = {}",unpaddedValueLength,paddedValueLength);
			if (paddedValueLength > 0xfffffffel) {
				throw new DicomException("Value length of pixel data ("+paddedValueLength+") exceeds maximum encodable ("+0xfffffffel+")");
			}
			String suffix = inputFileNameSuffix.toUpperCase();
			if (suffix.endsWith(".GZ")
			 || suffix.endsWith(".Z")
			 || suffix.endsWith(".BZ2")
			) {
				aPixelData = new OtherByteAttributeMultipleCompressedFilesOnDisk(TagFromName.PixelData,unpaddedValueLength,inputFileNames);
				// will puke later on write() if codec not available ...
			}
			else {
				aPixelData = new OtherByteAttributeMultipleFilesOnDisk(TagFromName.PixelData,inputFileNames);
				long encodedValueLength = aPixelData.getVL();
				if (encodedValueLength != paddedValueLength) {
					throw new DicomException("Uncompressed input files contain insufficient bytes ("+encodedValueLength+") for numberOfFrames * rows * columns * samplesPerPixel +/- padding ("+unpaddedValueLength+")");
				}
			}
			list.put(aPixelData);
		}

		return list;
	}
	
	protected static String[] getSortedInputFileNames(String inputFolderName,String inputFileNameSuffix) throws IOException {
		String[] inputFileNames = new File(inputFolderName).list(new FilenameFilterByCaseInsensitiveSuffix(inputFileNameSuffix));
		Arrays.sort(inputFileNames);	// since the only source of information about the order of the frames is by their name
		slf4jlogger.info("Number of files = {}",inputFileNames.length);
		for (int i=0; i<inputFileNames.length; ++i) {
			inputFileNames[i] = new File(inputFolderName,inputFileNames[i]).getCanonicalPath();		// NB. Need to have fully qualified names for OtherByteAttributeMultipleFilesOnDisk to work
			slf4jlogger.info("File: {}",inputFileNames[i]);
		}
		return inputFileNames;
	}

	/**
	 * <p>Read raw RGB color single frame image input format files (such as from Visible Human) into one or more DICOM multi-frame secondary capture images.</p>
	 *
	 * <p>Will obtain the description of the geometry of the raw RGB files, e.g., a Visible Human README file, from the specified format description file.</p>
	 *
	 * <p>The order of the frames is assumed to be by their lexicographically sorted file name (hence numeric file names need to be the same length, e.g., zero-padded.</p>
	 *
	 * @param	formatFileName
	 * @param	inputFolderName
	 * @param	inputFileNameSuffix
	 * @param	outputFolderName
	 * @param	patientName
	 * @param	patientID
	 * @param	studyID
	 * @param	seriesNumber
	 * @throws			IOException
	 * @throws			DicomException
	 */
	public RawRGBToDicomMultiFrame(String formatFileName,String inputFolderName,String inputFileNameSuffix,String outputFolderName,String patientName,String patientID,String studyID,String seriesNumber)
			throws IOException, DicomException {
			
		String modality = "GM";		// gross microscopy
		String sopClassUID = SOPClass.MultiframeTrueColorSecondaryCaptureImageStorage;
		
		String instanceNumber = "1";

		RawRGBInformation rawRGBInfo = new RawRGBInformation(formatFileName);

		String[] inputFileNames = getSortedInputFileNames(inputFolderName,inputFileNameSuffix);
		
		int numberOfFrames = inputFileNames.length;

		AttributeList list = generateDICOMPixelDataModuleAttributesFromRawRGBFiles(inputFileNames,inputFileNameSuffix,rawRGBInfo,null/*AttributeList*/);
		

		UIDGenerator u = new UIDGenerator();
		
		String sopInstanceUID = u.getAnotherNewUID();

		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(sopInstanceUID); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.FrameOfReferenceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }

		{ Attribute a = new PersonNameAttribute(TagFromName.PatientName); a.addValue(patientName); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.PatientID); a.addValue(patientID); list.put(a); }
		{ Attribute a = new DateAttribute(TagFromName.PatientBirthDate); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.PatientSex); list.put(a); }
		{ Attribute a = new ShortStringAttribute(TagFromName.StudyID); a.addValue(studyID); list.put(a); }
		{ Attribute a = new PersonNameAttribute(TagFromName.ReferringPhysicianName); a.addValue("^^^^"); list.put(a); }
		{ Attribute a = new ShortStringAttribute(TagFromName.AccessionNumber); list.put(a); }
		{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue(seriesNumber); list.put(a); }
		{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue(instanceNumber); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.Manufacturer); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.PatientOrientation); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.Laterality); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation); a.addValue("NO"); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.ImageType); a.addValue("DERIVED"); a.addValue("SECONDARY"); list.put(a); }

		{ Attribute a = new LongStringAttribute(TagFromName.PositionReferenceIndicator); list.put(a); }
		
		{
			java.util.Date currentDateTime = new java.util.Date();
			String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime);
			String currentTime = new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime);
			{ Attribute a = new DateAttribute(TagFromName.StudyDate);						a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.StudyTime);						a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.SeriesDate);						a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.SeriesTime);						a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.ContentDate);						a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.ContentTime);						a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate);			a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime);			a.addValue(currentTime); list.put(a); }
			{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC);	a.addValue(DateTimeAttribute.getTimeZone(java.util.TimeZone.getDefault(),currentDateTime)); list.put(a); }
		}
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }
		
		{
			generateGeometryFunctionalGroupsFromRawRGBInformation(rawRGBInfo,list,numberOfFrames);
			{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FrameIncrementPointer); a.addValue(TagFromName.PerFrameFunctionalGroupsSequence); list.put(a); }
		}

		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(sopClassUID); list.put(a); }
		
		{ Attribute a = new CodeStringAttribute(TagFromName.ConversionType); a.addValue("WSD"); list.put(a); }

		{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue(modality); list.put(a); }
			
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		
		File outputFile = new File(outputFolderName,sopInstanceUID+".dcm");
//System.err.print(list);
		list.write(outputFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read raw RGB color single frame image input format files (such as from Visible Human) into one or more DICOM multi-frame secondary capture images.</p>
	 *
	 * <p>Will obtain the description of the geometry of the raw RGB files, e.g., a Visible Human README file, from the specified format description file.</p>
	 *
	 * @param	arg	eight parameters, the format description file, inputFolder, inputFileNameSuffix, outputFolder, patientName, patientID, studyID, seriesNumber
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 8) {
				new RawRGBToDicomMultiFrame(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],arg[7]);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: RawRGBToDicomMultiFrame formatFile inputFolder inputFileNameSuffix outputFolder patientName patientID studyID seriesNumber");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

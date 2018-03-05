/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.*;

import com.pixelmed.utils.CopyStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting Analyze image input format files into images of a specified or appropriate SOP Class.</p>
 *
 * @author	dclunie
 */

public class AnalyzeToDicom {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/AnalyzeToDicom.java,v 1.8 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AnalyzeToDicom.class);

	/**
	 * <p>Read a per-frame and shared functional group sequences for the geometry defined in a Analyze file header.</p>
	 *
	 * @param	analyze			an Analyze header
	 * @param	list			an existing (possibly empty) attribute list, if null, a new one will be created; may already shared and per-frame functional group sequences or they will be added
	 * @param	numberOfFrames
	 * return					attribute list with per-frame and shared functional group sequences for geometry added
	 * @throws				DicomException
	 */
	public static AttributeList generateGeometryFunctionalGroupsFromAnalyzeHeader(AnalyzeHeader analyze,AttributeList list,int numberOfFrames) throws DicomException {
		list = FunctionalGroupUtilities.createFunctionalGroupsIfNotPresent(list,numberOfFrames);
		SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
		SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
		AttributeList sharedFunctionalGroupsSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(aSharedFunctionalGroupsSequence);

		{
			SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
			sharedFunctionalGroupsSequenceList.put(aPixelMeasuresSequence);
			AttributeList itemList = new AttributeList();
			aPixelMeasuresSequence.addItem(itemList);

			double columnSpacing = analyze.pixdim[1];
			double rowSpacing    = analyze.pixdim[2];
			double sliceSpacing  = analyze.pixdim[3];
			
			// note that order in DICOM in PixelSpacing is "adjacent row spacing", then "adjacent column spacing" ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(rowSpacing); a.addValue(columnSpacing); itemList.put(a); }
			// note that Analyze does not distinguish slice spacing from slice thickness (i.e., no overlap or gap description) ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.SliceThickness); a.addValue(sliceSpacing); itemList.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.SpacingBetweenSlices); a.addValue(sliceSpacing); itemList.put(a); }
		}

		{
			SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
			sharedFunctionalGroupsSequenceList.put(aPlaneOrientationSequence);
			AttributeList itemList = new AttributeList();
			aPlaneOrientationSequence.addItem(itemList);
			Attribute aImageOrientationPatient = new DecimalStringAttribute(TagFromName.ImageOrientationPatient);
			
			// see useful explanation at "http://eeg.sourceforge.net/mri_orientation_notes.html":
			// where * follows the slice dimension and letters indicate +XYZ orientations (L left, R right, A anterior, P posterior, I inferior, & S superior).
			
			// BUT seems to be often gratuitously different :(
			// what is below is the "strict" definition (see "http://www.grahamwideman.com/gw/brain/analyze/formatdoc.htm") except concern that ordering of voxels is actually different for conronal and sagittal ???? :(
			// but see also "http://www.grahamwideman.com/gw/brain/analyze/" and "http://www.grahamwideman.com/gw/brain/orientation/orientterms.htm"
			
			// DICOM is LPS+
			
			switch (analyze.orient) {
				case TRANSVERSE_UNFLIPPED:					//  LAS*
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(-1);
					aImageOrientationPatient.addValue(0);
					break;
				case TRANSVERSE_FLIPPED:					//  LPS*
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					break;
				case CORONAL_UNFLIPPED:						//  LA*S
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(1);
					break;
				case CORONAL_FLIPPED:						//  LA*I
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(-1);
					break;
				case SAGITTAL_UNFLIPPED:					//  L*AS
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(-1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(1);
					break;
				case SAGITTAL_FLIPPED:						//  L*AI
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(1);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(0);
					aImageOrientationPatient.addValue(-1);
					break;
				default:			throw new DicomException("Orientation of "+analyze.orient+" not supported");
			}
			itemList.put(aImageOrientationPatient);
		}
		
		{
			int framesInStack = analyze.dim[0] >= 3 ? (analyze.dim[3] > 0 ? analyze.dim[3] : 1) : 1;
				
			{
				for (int f=0; f<numberOfFrames; ++f) {
					SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
					SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,f).put(aPlanePositionSequence);

					AttributeList itemList = new AttributeList();
					aPlanePositionSequence.addItem(itemList);
					Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
					
					int frameOffsetInStack = f % framesInStack;
					
					switch (analyze.orient) {
						case TRANSVERSE_UNFLIPPED:					//  LAS*
						case TRANSVERSE_FLIPPED:					//  LPS*
							a.addValue(0);
							a.addValue(0);
							a.addValue(analyze.pixdim[3] * frameOffsetInStack);
							break;
						case CORONAL_UNFLIPPED:						//  LA*S
						case CORONAL_FLIPPED:						//  LA*I
							a.addValue(0);
							a.addValue(analyze.pixdim[2] * frameOffsetInStack);
							a.addValue(0);
							break;
						case SAGITTAL_UNFLIPPED:					//  L*AS
						case SAGITTAL_FLIPPED:						//  L*AI
							a.addValue(analyze.pixdim[1] * frameOffsetInStack);
							a.addValue(0);
							a.addValue(0);
							break;
						default:			throw new DicomException("Orientation of "+analyze.orient+" not supported");
					}
					
					itemList.put(a);
				}
			}
			
			
		}
		return list;
	}
	
	/**
	 * <p>Using an Analyze image input file and header, create DICOM Pixel Data Module attributes.</p>
	 *
	 * @param	inputFile	an Analyze format image file
	 * @param	analyze		an Analyze header already read from the inputFile
	 * @param	list		an existing (possibly empty) attribute list, if null, a new one will be created; may already include "better" image pixel module attributes to use
	 * return				attribute list with Image Pixel Module (including Pixel Data) and other attributes added
	 * @throws			IOException
	 * @throws			DicomException
	 * @throws			AnalyzeException
	 */
	public static AttributeList generateDICOMPixelDataModuleAttributesFromAnalyzeFile(File inputFile,AnalyzeHeader analyze,AttributeList list) throws IOException, DicomException, AnalyzeException {
		if (list == null) {
			list = new AttributeList();
		}
		
		int numberOfDimensions = (int)(analyze.dim[0])&0xffff;
		if (numberOfDimensions < 2) {
			throw new DicomException("Cannot convert if less than two dimensions");
		}
		int columns = (int)(analyze.dim[1])&0xffff;
		int rows = (int)(analyze.dim[2])&0xffff;
		int numberOfFrames = 1;
		if (numberOfDimensions > 2) {
			for (int d=3; d<=numberOfDimensions; ++d) {
				numberOfFrames *= (int)(analyze.dim[d])&0xffff;
			}
		}
		
		String photometricInterpretation = null;
		int samplesPerPixel = 0;
		int depth = 0;
		int pixelRepresentation = 0;
		Attribute aPixelData = null;
		boolean sendBitsStored = true;
		boolean sendHighBit = true;
		boolean sendPixelRepresentation = true;
		switch(analyze.datatype) {
			case BINARY:		throw new DicomException("Conversion of "+analyze.datatype+" not supported");
			case UNSIGNED_CHAR:	aPixelData = new OtherByteAttribute(TagFromName.PixelData);
								pixelRepresentation = 0;
								depth = 8;
								samplesPerPixel = 1;
								photometricInterpretation = "MONOCHROME2";
								break;
			case SIGNED_SHORT:	aPixelData = new OtherWordAttribute(TagFromName.PixelData);
								pixelRepresentation = 1;
								depth = 16;
								samplesPerPixel = 1;
								photometricInterpretation = "MONOCHROME2";
								break;
			case SIGNED_INT:	throw new DicomException("Conversion of "+analyze.datatype+" not supported");
			case FLOAT:			aPixelData = new OtherFloatAttribute(TagFromName.FloatPixelData);
								sendPixelRepresentation = false;
								pixelRepresentation = 0;	// ignored
								depth = 32;
								sendBitsStored = false;
								sendHighBit = false;
								samplesPerPixel = 1;
								photometricInterpretation = "MONOCHROME2";
								break;
			case COMPLEX:		throw new DicomException("Conversion of "+analyze.datatype+" not supported");
			case DOUBLE:		aPixelData = new OtherDoubleAttribute(TagFromName.DoubleFloatPixelData);
								sendPixelRepresentation = false;
								pixelRepresentation = 0;	// ignored
								depth = 64;
								sendBitsStored = false;
								sendHighBit = false;
								samplesPerPixel = 1;
								photometricInterpretation = "MONOCHROME2";
								break;
			case RGB:			aPixelData = new OtherByteAttribute(TagFromName.PixelData);
								pixelRepresentation = 0;
								depth = 8;
								samplesPerPixel = 3;
								photometricInterpretation = "RGB";
								break;
			default:			throw new DicomException("Conversion of "+analyze.datatype+" not supported");
		}
		
		// really could do better than reading everything into memory, but expedient and handles byte ordering on input and output ...
		
		double minPixelValue = 0;
		double maxPixelValue = 0;
		boolean insertWindowValues = false;
		{
			File imageDataFile = analyze.getImageDataFile(inputFile);
			BinaryInputStream analyzePixelData = new BinaryInputStream(imageDataFile,analyze.bigEndian);
			analyzePixelData.skipInsistently((long)analyze.vox_offset);		// usually 352; no idea why they specified it as a float !; will be 0 (?) if separate image data file
			int numberOfPixels = rows * columns * numberOfFrames * samplesPerPixel;
			if (aPixelData instanceof OtherByteAttribute) {
				byte[] values = new byte[numberOfPixels];
				analyzePixelData.readInsistently(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherWordAttribute) {
				short[] values = new short[numberOfPixels];
				analyzePixelData.readUnsigned16(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherFloatAttribute) {
				float[] values = new float[numberOfPixels];
				analyzePixelData.readFloat(values,numberOfPixels);
				aPixelData.setValues(values);
				float[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			else if (aPixelData instanceof OtherDoubleAttribute) {
				double[] values = new double[numberOfPixels];
				analyzePixelData.readDouble(values,numberOfPixels);
				aPixelData.setValues(values);
				double[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			
			analyzePixelData.close();
		}
		
		list.put(aPixelData);

		{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a); }

		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(depth); list.put(a); }
		if (sendBitsStored) { Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(depth); list.put(a); }
		if (sendHighBit)    { Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(depth-1); list.put(a); }
		
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Rows); a.addValue(rows); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Columns); a.addValue(columns); list.put(a); }
			
		if (sendPixelRepresentation) { Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation); a.addValue(pixelRepresentation); list.put(a); }

		list.remove(TagFromName.NumberOfFrames);
		if (numberOfFrames > 1) {
			Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); list.put(a);
		}
			
		{ Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel); a.addValue(samplesPerPixel); list.put(a); }
						
		list.remove(TagFromName.PlanarConfiguration);
		if (samplesPerPixel > 1) {
				Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue(0); list.put(a);	// always chunky pixel
		}
		
		if (samplesPerPixel == 1) {
			double rescaleScale = 1;
			double rescaleIntercept = 0;

			{ Attribute a = new CodeStringAttribute(TagFromName.PresentationLUTShape); a.addValue("IDENTITY"); list.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.RescaleSlope); a.addValue(rescaleScale); list.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.RescaleIntercept); a.addValue(rescaleIntercept); list.put(a); }
			{ Attribute a = new LongStringAttribute(TagFromName.RescaleType); a.addValue("US"); list.put(a); }

			{ Attribute a = new CodeStringAttribute(TagFromName.VOILUTFunction); a.addValue(aPixelData instanceof OtherFloatAttribute || aPixelData instanceof OtherDoubleAttribute ? "LINEAR_EXACT" : "LINEAR"); list.put(a); }
			
			if (insertWindowValues) {
				double windowWidth = (maxPixelValue - minPixelValue);
				double windowCenter = (maxPixelValue + minPixelValue)/2;
				{ Attribute a = new DecimalStringAttribute(TagFromName.WindowWidth); a.addValue(windowWidth); list.put(a); }
				{ Attribute a = new DecimalStringAttribute(TagFromName.WindowCenter); a.addValue(windowCenter); list.put(a); }
			}
		}

		return list;
	}

	/**
	 * <p>Read an Analyze image input format files and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	inputFileName
	 * @param	outputFileName
	 * @param	patientName
	 * @param	patientID
	 * @param	studyID
	 * @param	seriesNumber
	 * @param	instanceNumber
	 * @throws			IOException
	 * @throws			DicomException
	 * @throws			AnalyzeException
	 */
	public AnalyzeToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber)
			throws IOException, DicomException, AnalyzeException {
		this(inputFileName,outputFileName,patientName,patientID,studyID,seriesNumber,instanceNumber,null,null);
	}

	/**
	 * <p>Read an Analyze image input format files and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	inputFileName
	 * @param	outputFileName
	 * @param	patientName
	 * @param	patientID
	 * @param	studyID
	 * @param	seriesNumber
	 * @param	instanceNumber
	 * @param	modality	may be null
	 * @param	sopClass	may be null
	 * @throws			IOException
	 * @throws			DicomException
	 * @throws			AnalyzeException
	 */
	public AnalyzeToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber,String modality,String sopClass)
			throws IOException, DicomException, AnalyzeException {

		File inputFile = new File(inputFileName);
		AnalyzeHeader analyze = new AnalyzeHeader(inputFile);

		AttributeList list = generateDICOMPixelDataModuleAttributesFromAnalyzeFile(inputFile,analyze,null/*AttributeList*/);
		
		CommonConvertedAttributeGeneration.generateCommonAttributes(list,patientName,patientID,studyID,seriesNumber,instanceNumber,modality,sopClass);

		sopClass = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isEnhancedMultiframeImageStorage(sopClass)) {
			int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
			generateGeometryFunctionalGroupsFromAnalyzeHeader(analyze,list,numberOfFrames);		// regardless of whether parametric map, modality specific, legacy or secondary capture
		}
		
		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
		
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read an Analyze image input format files and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	arg	seven, eight or nine parameters, the inputFile, outputFile, patientName, patientID, studyID, seriesNumber, instanceNumber, and optionally the modality, and SOP Class
	 */
	public static void main(String arg[]) {
		String modality = null;
		String sopClass = null;
		try {
			if (arg.length == 7) {
			}
			else if (arg.length == 8) {
				modality = arg[7];
			}
			else if (arg.length == 9) {
				modality = arg[7];
				sopClass = arg[8];
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: AnalyzeToDicom inputFile outputFile patientName patientID studyID seriesNumber instanceNumber [modality [SOPClass]]");
				System.exit(1);
			}
			new AnalyzeToDicom(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],modality,sopClass);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.*;

import com.pixelmed.utils.CopyStream;

//import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Arrays;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting raw image input format files into images of a specified or appropriate SOP Class.</p>
 *
 * @author	dclunie
 */

public class RawToDicomMultiFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/RawToDicomMultiFrame.java,v 1.2 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RawToDicomMultiFrame.class);
	
	private static boolean preSpatialDimensionsLeastRapidlyVaryingInOutput = false;

	/**
	 * <p>Using a raw image input file and header, create DICOM Pixel Data Module attributes.</p>
	 *
	 * @param	inputFile		a raw format image file
	 * @param	rawImageDesc	a raw image description already read from the inputFile
	 * @param	list			an existing (possibly empty) attribute list, if null, a new one will be created; may already include "better" image pixel module attributes to use
	 * return					attribute list with Image Pixel Module (including Pixel Data) and other attributes added
	 * @throws					IOException
	 * @throws					DicomException
	 * @throws					NumberFormatException
	 */
	public static AttributeList generateDICOMPixelDataModuleAttributesFromNRRDFile(File inputFile,RawImageDescription rawImageDesc,AttributeList list) throws IOException, DicomException, NumberFormatException {
		if (list == null) {
			list = new AttributeList();
		}
		
		int columns = rawImageDesc.columns;
		int rows = rawImageDesc.rows;
		int numberOfFrames = rawImageDesc.frames;
		
		slf4jlogger.info("columns = {}",columns);
		slf4jlogger.info("rows = {}",rows);
		slf4jlogger.info("numberOfFrames = {}",numberOfFrames);
		
		String photometricInterpretation = null;
		int samplesPerPixel = 0;
		int depth = 0;
		int pixelRepresentation = 0;
		Attribute aPixelData = null;
		boolean sendBitsStored = true;
		boolean sendHighBit = true;
		boolean sendPixelRepresentation = true;
		RawImageDescription.Type type = rawImageDesc.getType();
		switch(type) {
			case INT8:		aPixelData = new OtherByteAttribute(TagFromName.PixelData);
							pixelRepresentation = 1;
							depth = 8;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case UINT8:		aPixelData = new OtherByteAttribute(TagFromName.PixelData);
							pixelRepresentation = 0;
							depth = 8;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case INT16:		aPixelData = new OtherWordAttribute(TagFromName.PixelData);
							pixelRepresentation = 1;
							depth = 16;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case UINT16:	aPixelData = new OtherWordAttribute(TagFromName.PixelData);
							pixelRepresentation = 0;
							depth = 16;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case INT32:		throw new DicomException("Conversion of "+type+" not supported");
			case UINT32:	throw new DicomException("Conversion of "+type+" not supported");
			case INT64:		throw new DicomException("Conversion of "+type+" not supported");
			case UINT64:	throw new DicomException("Conversion of "+type+" not supported");
			case FLOAT32:	aPixelData = new OtherFloatAttribute(TagFromName.FloatPixelData);
							sendPixelRepresentation = false;
							pixelRepresentation = 0;	// ignored
							depth = 32;
							sendBitsStored = false;
							sendHighBit = false;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case FLOAT64:	aPixelData = new OtherDoubleAttribute(TagFromName.DoubleFloatPixelData);
							sendPixelRepresentation = false;
							pixelRepresentation = 0;	// ignored
							depth = 64;
							sendBitsStored = false;
							sendHighBit = false;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			default:		throw new DicomException("Conversion of "+type+" not supported");
		}
		
		// really could do better than reading everything into memory, but expedient and handles byte ordering on input and output ...
		
		double minPixelValue = 0;
		double maxPixelValue = 0;
		boolean insertWindowValues = false;
		
		{
			InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
			
			BinaryInputStream rawImageDescPixelData = new BinaryInputStream(in,rawImageDesc.isDataBigEndian);
			
			int numberOfPixels = rows * columns * numberOfFrames * samplesPerPixel;
			if (aPixelData instanceof OtherByteAttribute) {
				byte[] values = new byte[numberOfPixels];
				rawImageDescPixelData.readInsistently(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherWordAttribute) {
				short[] values = new short[numberOfPixels];
				rawImageDescPixelData.readUnsigned16(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherFloatAttribute) {
				float[] values = new float[numberOfPixels];
				rawImageDescPixelData.readFloat(values,numberOfPixels);
				aPixelData.setValues(values);
				float[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			else if (aPixelData instanceof OtherDoubleAttribute) {
				double[] values = new double[numberOfPixels];
				rawImageDescPixelData.readDouble(values,numberOfPixels);
				aPixelData.setValues(values);
				double[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			
			rawImageDescPixelData.close();
			in.close();
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
	 * <p>Read a raw image input format files and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	formatFileName
	 * @param	inputFileName
	 * @param	outputFileName
	 * @param	patientName
	 * @param	patientID
	 * @param	studyID
	 * @param	seriesNumber
	 * @param	instanceNumber
	 * @throws			IOException
	 * @throws			DicomException
	 */
	public RawToDicomMultiFrame(String formatFileName,String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber)
			throws IOException, DicomException {
		this(formatFileName,inputFileName,outputFileName,patientName,patientID,studyID,seriesNumber,instanceNumber,null,null);
	}

	/**
	 * <p>Read a raw image input format file and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	formatFileName
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
	 * @throws			NumberFormatException
	 */
	public RawToDicomMultiFrame(String formatFileName,String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber,String modality,String sopClass)
			throws IOException, DicomException, NumberFormatException {

		RawImageDescription rawImageDesc = new RawImageDescription(new File(formatFileName));

		File inputFile = new File(inputFileName);
		
		AttributeList list = generateDICOMPixelDataModuleAttributesFromNRRDFile(inputFile,rawImageDesc,null/*AttributeList*/);
			
		CommonConvertedAttributeGeneration.generateCommonAttributes(list,patientName,patientID,studyID,seriesNumber,instanceNumber,modality,sopClass);
		
		sopClass = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isEnhancedMultiframeImageStorage(sopClass)) {
			//generateGeometryFunctionalGroupsFromRawImageDescription(rawImageDesc,list);
			//generateDimensions(list);
		}

		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a raw multiframe image input format file and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * <p>If multiple single frame raw images are to be converted, first cat them together in the correct order.</p>
	 *
	 * @param	arg	eight, nine or ten parameters, the JSON formatFile, inputFile, outputFile, patientName, patientID, studyID, seriesNumber, instanceNumber, and optionally the modality, and SOP Class
	 */
	public static void main(String arg[]) {
		String modality = null;
		String sopClass = null;
		try {
			if (arg.length == 8) {
			}
			else if (arg.length == 9) {
				modality = arg[8];
			}
			else if (arg.length == 10) {
				modality = arg[8];
				sopClass = arg[9];
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: RawToDicomMultiFrame formatFile inputFile outputFile patientName patientID studyID seriesNumber instanceNumber [modality [SOPClass]]");
				System.exit(1);
			}
			new RawToDicomMultiFrame(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],arg[7],modality,sopClass);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

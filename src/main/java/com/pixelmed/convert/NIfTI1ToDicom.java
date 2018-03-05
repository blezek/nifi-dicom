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
 * <p>A class for converting NIfTI-1 image input format files into images of a specified or appropriate SOP Class.</p>
 *
 * @author	dclunie
 */

public class NIfTI1ToDicom {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/NIfTI1ToDicom.java,v 1.25 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NIfTI1ToDicom.class);

	/**
	 * <p>Read a per-frame and shared functional group sequences for the geometry defined in a NIfTI-1 file header.</p>
	 *
	 * @param	nifti			a NIfTI-1 header
	 * @param	list			an existing (possibly empty) attribute list, if null, a new one will be created; may already shared and per-frame functional group sequences or they will be added
	 * @param	numberOfFrames
	 * return					attribute list with per-frame and shared functional group sequences for geometry added
	 * @throws				DicomException
	 */
	public static AttributeList generateGeometryFunctionalGroupsFromNIfTI1Header(NIfTI1Header nifti,AttributeList list,int numberOfFrames) throws DicomException {
		list = FunctionalGroupUtilities.createFunctionalGroupsIfNotPresent(list,numberOfFrames);
		SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
		SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
		AttributeList sharedFunctionalGroupsSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(aSharedFunctionalGroupsSequence);

		double unitsMultiplier = 1;
		if (nifti.xyzt_units_spatial == NIfTI1Header.Units.METER) {
			unitsMultiplier = 1000;
		}
		else if (nifti.xyzt_units_spatial == NIfTI1Header.Units.MICRON) {
			unitsMultiplier = 1.0/1000;
		}
		// else assume NIfTI1Header.Units.MM

		{
			SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
			sharedFunctionalGroupsSequenceList.put(aPixelMeasuresSequence);
			AttributeList itemList = new AttributeList();
			aPixelMeasuresSequence.addItem(itemList);

			double columnSpacing = nifti.pixdim[1] * unitsMultiplier;
			double rowSpacing    = nifti.pixdim[2] * unitsMultiplier;
			double sliceSpacing  = nifti.pixdim[3] * unitsMultiplier;
			
			// note that order in DICOM in PixelSpacing is "adjacent row spacing", then "adjacent column spacing" ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(rowSpacing); a.addValue(columnSpacing); itemList.put(a); }
			// note that NIfTI does not distinguish slice spacing from slice thickness (i.e., no overlap or gap description) ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.SliceThickness); a.addValue(sliceSpacing); itemList.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.SpacingBetweenSlices); a.addValue(sliceSpacing); itemList.put(a); }
		}

		double nx = 0;	// normal from quaternion derived once and saved for position calculation
		double ny = 0;
		double nz = 0;
		float qfac = 0;
		{
			SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
			sharedFunctionalGroupsSequenceList.put(aPlaneOrientationSequence);
			AttributeList itemList = new AttributeList();
			aPlaneOrientationSequence.addItem(itemList);
			Attribute aImageOrientationPatient = new DecimalStringAttribute(TagFromName.ImageOrientationPatient);
			
			// For production, prefer sform over qform if present; uncomment preference for qform for testing/comparison (there will be floating point differences after about the 4th decimal place)
			if (/*nifti.qform == NIfTI1Header.CoordinateTransform.UNKNOWN && */nifti.sform != NIfTI1Header.CoordinateTransform.UNKNOWN) {
				slf4jlogger.info("Method 3: orientation");
				aImageOrientationPatient.addValue( - nifti.srow_x[0] / nifti.pixdim[1]);		// NIfTI is RAS+, DICOM is LPS+
				aImageOrientationPatient.addValue( - nifti.srow_x[1] / nifti.pixdim[2]);		// NIfTI is RAS+, DICOM is LPS+
				aImageOrientationPatient.addValue(   nifti.srow_x[2] / nifti.pixdim[3]);

				aImageOrientationPatient.addValue( - nifti.srow_y[0] / nifti.pixdim[1]);		// NIfTI is RAS+, DICOM is LPS+
				aImageOrientationPatient.addValue( - nifti.srow_y[1] / nifti.pixdim[2]);		// NIfTI is RAS+, DICOM is LPS+
				aImageOrientationPatient.addValue(   nifti.srow_y[2] / nifti.pixdim[3]);
			}
			else if (nifti.qform != NIfTI1Header.CoordinateTransform.UNKNOWN) {
				slf4jlogger.info("Method 2: orientation");
				// See "http://nifti.nimh.nih.gov/nifti-1/documentation/nifti1fields/nifti1fields_pages/quatern.html"
				//
				//          [ a*a+b*b-c*c-d*d   2*b*c-2*a*d       2*b*d+2*a*c     ]
				//      R = [ 2*b*c+2*a*d       a*a+c*c-b*b-d*d   2*c*d-2*a*b     ]
				//          [ 2*b*d-2*a*c       2*c*d+2*a*b       a*a+d*d-c*c-b*b ]
				//
				//          [ R11               R12               R13             ]
				//        = [ R21               R22               R23             ]
				//          [ R31               R32               R33             ]
				
				double b = nifti.quatern_b;
				double c = nifti.quatern_c;
				double d = nifti.quatern_d;
				double a = Math.sqrt(1.0-(b*b+c*c+d*d));
				
				double rx = - (a*a+b*b-c*c-d*d);
				double ry = - (2*b*c+2*a*d);
				double rz =   (2*b*d-2*a*c);
				
				double cx = - (2*b*c-2*a*d);
				double cy = - (a*a+c*c-b*b-d*d);
				double cz =   (2*c*d+2*a*b);

				// normal from quaternion derived once and saved for position calculation ... do not handle qfac here ... do it later
				nx = - (2*b*d-2*a*c);		// NIfTI is RAS+, DICOM is LPS+
				ny = - (2*c*d+2*a*b);		// NIfTI is RAS+, DICOM is LPS+
				nz =   (a*a+d*d-c*c-b*b);
				slf4jlogger.info("Method 2: slice direction (normal) = ({},{},{})",nx,ny,nz);

				qfac = nifti.pixdim[0] == 0 ? 1 : nifti.pixdim[0];	// should be 1 or -1 (reflection), but sometimes zero (so assume 1) ... per nifti1.h only applies to Z
				slf4jlogger.info("Method 2: use qfac {}",qfac);
		 
				aImageOrientationPatient.addValue(rx);
				aImageOrientationPatient.addValue(ry);
				aImageOrientationPatient.addValue(rz);
				aImageOrientationPatient.addValue(cx);
				aImageOrientationPatient.addValue(cy);
				aImageOrientationPatient.addValue(cz);
			}
			else {
				slf4jlogger.info("Method 1: orientation");
				// translation only ... make axial to be consistent with position using slice dimension as Z
				slf4jlogger.warn("no quaterinion or affine transform to Patient Coordinate System specified ... assuming axial images and translation only");

				aImageOrientationPatient.addValue(1);
				aImageOrientationPatient.addValue(0);
				aImageOrientationPatient.addValue(0);
				aImageOrientationPatient.addValue(0);
				aImageOrientationPatient.addValue(1);
				aImageOrientationPatient.addValue(0);
			}
			itemList.put(aImageOrientationPatient);
		}
		
		{
			// per "http://nifti.nimh.nih.gov/pub/dist/src/niftilib/nifti1.h", "NIFTI-1 reserves dimensions 1,2,3 for space (x,y,z), 4 for time (t), and 5,6,7 for anything else needed"
			// therefore, for > 3 dimensions, need to repeat 3rd spatial (dimension) after every dim[3] frames ...
			int framesInStack = nifti.dim[0] >= 3 ? (nifti.dim[3] > 0 ? nifti.dim[3] : 1) : 1;
				
			// For production, prefer sform over qform if present; uncomment preference for qform for testing/comparison (there will be floating point differences after about the 4th decimal place)
			if (/*nifti.qform == NIfTI1Header.CoordinateTransform.UNKNOWN && */nifti.sform != NIfTI1Header.CoordinateTransform.UNKNOWN) {
//System.err.println("Method 3: position");
				for (int f=0; f<numberOfFrames; ++f) {
					SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
					SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,f).put(aPlanePositionSequence);

					AttributeList itemList = new AttributeList();
					aPlanePositionSequence.addItem(itemList);
					Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
					
					int frameOffsetInStack = f % framesInStack;
					
					a.addValue( - (nifti.srow_x[2] * frameOffsetInStack + nifti.srow_x[3]));		// NIfTI is RAS+, DICOM is LPS+
					a.addValue( - (nifti.srow_y[2] * frameOffsetInStack + nifti.srow_y[3]));		// NIfTI is RAS+, DICOM is LPS+
					a.addValue(   (nifti.srow_z[2] * frameOffsetInStack + nifti.srow_z[3]));
					
					itemList.put(a);
				}
			}
			else if (nifti.qform != NIfTI1Header.CoordinateTransform.UNKNOWN) {
//System.err.println("Method 2: position");
				for (int f=0; f<numberOfFrames; ++f) {
					SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
					SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,f).put(aPlanePositionSequence);

					AttributeList itemList = new AttributeList();
					aPlanePositionSequence.addItem(itemList);
					Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
					
					int frameOffsetInStack = f % framesInStack;
										
					a.addValue(nx *        nifti.pixdim[1] * frameOffsetInStack - nifti.qoffset_x);		// NIfTI is RAS+, DICOM is LPS+
					a.addValue(ny *        nifti.pixdim[2] * frameOffsetInStack - nifti.qoffset_y);		// NIfTI is RAS+, DICOM is LPS+
					a.addValue(nz * qfac * nifti.pixdim[3] * frameOffsetInStack + nifti.qoffset_z);
					
					itemList.put(a);
				}
			}
			else {
//System.err.println("Method 1: position");
				for (int f=0; f<numberOfFrames; ++f) {
					SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
					SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,f).put(aPlanePositionSequence);

					AttributeList itemList = new AttributeList();
					aPlanePositionSequence.addItem(itemList);
					Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
					
					int frameOffsetInStack = f % framesInStack;

					a.addValue( - nifti.qoffset_x);							// NIfTI is RAS+, DICOM is LPS+
					a.addValue( - nifti.qoffset_y);							// NIfTI is RAS+, DICOM is LPS+
					a.addValue(   nifti.qoffset_z + nifti.pixdim[3] * frameOffsetInStack);
					
					itemList.put(a);
				}
			}
			
			
		}
		return list;
	}
	
	/**
	 * <p>Using a NIfTI-1 image input file and header, create DICOM Pixel Data Module attributes.</p>
	 *
	 * @param	inputFile	a NIfTI-1 format image file
	 * @param	nifti		a NIfTI-1 header already read from the inputFile
	 * @param	list		an existing (possibly empty) attribute list, if null, a new one will be created; may already include "better" image pixel module attributes to use
	 * return				attribute list with Image Pixel Module (including Pixel Data) and other attributes added
	 * @throws			IOException
	 * @throws			DicomException
	 * @throws			NIfTI1Exception
	 */
	public static AttributeList generateDICOMPixelDataModuleAttributesFromNIfTI1File(File inputFile,NIfTI1Header nifti,AttributeList list) throws IOException, DicomException, NIfTI1Exception {
		if (list == null) {
			list = new AttributeList();
		}
		
		int numberOfDimensions = (int)(nifti.dim[0])&0xffff;
		if (numberOfDimensions < 2) {
			throw new DicomException("Cannot convert if less than two dimensions");
		}
		int columns = (int)(nifti.dim[1])&0xffff;
		int rows = (int)(nifti.dim[2])&0xffff;
		int numberOfFrames = 1;
		if (numberOfDimensions > 2) {
			for (int d=3; d<=numberOfDimensions; ++d) {
				numberOfFrames *= (int)(nifti.dim[d])&0xffff;
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
		switch(nifti.datatype) {
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
			case INT32:		throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case FLOAT32:	aPixelData = new OtherFloatAttribute(TagFromName.FloatPixelData);
							sendPixelRepresentation = false;
							pixelRepresentation = 0;	// ignored
							depth = 32;
							sendBitsStored = false;
							sendHighBit = false;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case COMPLEX64:	throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case FLOAT64:	aPixelData = new OtherDoubleAttribute(TagFromName.DoubleFloatPixelData);
							sendPixelRepresentation = false;
							pixelRepresentation = 0;	// ignored
							depth = 64;
							sendBitsStored = false;
							sendHighBit = false;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case RGB24:		aPixelData = new OtherByteAttribute(TagFromName.PixelData);
							pixelRepresentation = 0;
							depth = 8;
							samplesPerPixel = 3;
							photometricInterpretation = "RGB";
							break;
			case INT8:		aPixelData = new OtherByteAttribute(TagFromName.PixelData);
							pixelRepresentation = 1;
							depth = 8;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case UINT16:	aPixelData = new OtherWordAttribute(TagFromName.PixelData);
							pixelRepresentation = 0;
							depth = 16;
							samplesPerPixel = 1;
							photometricInterpretation = "MONOCHROME2";
							break;
			case INT64:		throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case UINT64:	throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case FLOAT128:	throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case COMPLEX128:throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case COMPLEX256:throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			case RGBA32:	throw new DicomException("Conversion of "+nifti.datatype+" not supported");
			default:		throw new DicomException("Conversion of "+nifti.datatype+" not supported");
		}
		
		// really could do better than reading everything into memory, but expedient and handles byte ordering on input and output ...
		
		double minPixelValue = 0;
		double maxPixelValue = 0;
		boolean insertWindowValues = false;
		{
			File imageDataFile = nifti.isNIfTI1DualFileMagicNumber() ? nifti.getImageDataFile(inputFile) : inputFile;
			BinaryInputStream niftiPixelData = new BinaryInputStream(imageDataFile,nifti.bigEndian);
			niftiPixelData.skipInsistently((long)nifti.vox_offset);		// usually 352; no idea why they specified it as a float !; will be 0 (?) if separate image data file
			int numberOfPixels = rows * columns * numberOfFrames * samplesPerPixel;
			if (aPixelData instanceof OtherByteAttribute) {
				byte[] values = new byte[numberOfPixels];
				niftiPixelData.readInsistently(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherWordAttribute) {
				short[] values = new short[numberOfPixels];
				niftiPixelData.readUnsigned16(values,0,numberOfPixels);
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherFloatAttribute) {
				float[] values = new float[numberOfPixels];
				niftiPixelData.readFloat(values,numberOfPixels);
				aPixelData.setValues(values);
				float[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			else if (aPixelData instanceof OtherDoubleAttribute) {
				double[] values = new double[numberOfPixels];
				niftiPixelData.readDouble(values,numberOfPixels);
				aPixelData.setValues(values);
				double[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			
			niftiPixelData.close();
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
			double rescaleScale = nifti.scl_slope;
			double rescaleIntercept = nifti.scl_inter;
			if (rescaleScale == 0) {	// i.e., not populated
				rescaleScale = 1;
				rescaleIntercept = 0;
			}
			{ Attribute a = new CodeStringAttribute(TagFromName.PresentationLUTShape); a.addValue("IDENTITY"); list.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.RescaleSlope); a.addValue(rescaleScale); list.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.RescaleIntercept); a.addValue(rescaleIntercept); list.put(a); }
			{ Attribute a = new LongStringAttribute(TagFromName.RescaleType); a.addValue(nifti.intent_name[0] == 0 ? "US" : new String(nifti.intent_name,Charset.forName("US-ASCII"))); list.put(a); }

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
	 * <p>Read a NIfTI-1 image input format files and create an image of a specified or appropriate SOP Class.</p>
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
	 * @throws			NIfTI1Exception
	 */
	public NIfTI1ToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber)
			throws IOException, DicomException, NIfTI1Exception {
		this(inputFileName,outputFileName,patientName,patientID,studyID,seriesNumber,instanceNumber,null,null);
	}

	/**
	 * <p>Read a NIfTI-1 image input format files and create an image of a specified or appropriate SOP Class.</p>
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
	 * @throws			NIfTI1Exception
	 */
	public NIfTI1ToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber,String modality,String sopClass)
			throws IOException, DicomException, NIfTI1Exception {

		File inputFile = new File(inputFileName);
		NIfTI1Header nifti = new NIfTI1Header(inputFile);

		AttributeList list = generateDICOMPixelDataModuleAttributesFromNIfTI1File(inputFile,nifti,null/*AttributeList*/);
			
		CommonConvertedAttributeGeneration.generateCommonAttributes(list,patientName,patientID,studyID,seriesNumber,instanceNumber,modality,sopClass);
		
		sopClass = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isEnhancedMultiframeImageStorage(sopClass)) {
			int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
			generateGeometryFunctionalGroupsFromNIfTI1Header(nifti,list,numberOfFrames);
		}
		
		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a NIfTI-1 image input format files and create an image of a specified or appropriate SOP Class.</p>
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
				System.err.println("Usage: NIfTI1ToDicom inputFile outputFile patientName patientID studyID seriesNumber instanceNumber [modality [SOPClass]]");
				System.exit(1);
			}
			new NIfTI1ToDicom(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],modality,sopClass);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

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

import java.util.zip.GZIPInputStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting NRRD image input format files into images of a specified or appropriate SOP Class.</p>
 *
 * @author	dclunie
 */

public class NRRDToDicom {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/NRRDToDicom.java,v 1.17 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NRRDToDicom.class);
	
	private static boolean preSpatialDimensionsLeastRapidlyVaryingInOutput = false;

	/**
	 * <p>Read a per-frame and shared functional group sequences for the geometry defined in a NRRD file header.</p>
	 *
	 * @param	nrrd		an NRRD header
	 * @param	list		an existing (possibly empty) attribute list, if null, a new one will be created; may already shared and per-frame functional group sequences or they will be added
	 * return				attribute list with per-frame and shared functional group sequences for geometry added
	 * @throws			DicomException
	 * @throws			NRRDException
	 * @throws			NumberFormatException
	 */
	public static AttributeList generateGeometryFunctionalGroupsFromNRRDHeader(NRRDHeader nrrd,AttributeList list) throws DicomException, NRRDException, NumberFormatException {
		String space = nrrd.getSpace();
		double[] directionCorrection = new double[3];
		// assume same as DICOM (LPS+) unless we know it is different
		directionCorrection[0] = 1;
		directionCorrection[1] = 1;
		directionCorrection[2] = 1;
		if (space != null) {
			if (space.equals("right-anterior-superior")      || space.equals("RAS")
			 || space.equals("right-anterior-superior-time") || space.equals("RAST")
			) {
				directionCorrection[0] = -1;
				directionCorrection[1] = -1;
				directionCorrection[2] =  1;
			}
			else if (space.equals("left-anterior-superior")      || space.equals("LAS")
			      || space.equals("left-anterior-superior-time") || space.equals("LAST")
			) {
				directionCorrection[0] =  1;
				directionCorrection[1] = -1;
				directionCorrection[2] =  1;
			}
			else if (space.equals("left-posterior-superior")      || space.equals("LPS")
			      || space.equals("left-posterior-superior-time") || space.equals("LPST")
			) {
				directionCorrection[0] =  1;
				directionCorrection[1] =  1;
				directionCorrection[2] =  1;
			}
			else {
				// else gantry relative or something weird so out of luck :(
				slf4jlogger.info("Warning: non-patient-relative coordinate space {} so position and coordinates in DICOM images may be incorrect",space);
			}
		}

		String[] spaceOrigin = nrrd.getSpaceOrigin();
		slf4jlogger.info("spaceOrigin array = {}",Arrays.toString(spaceOrigin));
		double[] origin = new double[3];
		origin[0] = Double.parseDouble(spaceOrigin[0]);
		origin[1] = Double.parseDouble(spaceOrigin[1]);
		origin[2] = Double.parseDouble(spaceOrigin[2]);
		
		double[] spacing = new double[3];
		double[][] rowColumnAndSliceDirectionVectors     = new double[3][];
		double[][] rowColumnAndSliceDirectionUnitVectors = new double[3][];

		int numberOfSlicesInVolume = 1;
		int numberOfScalarsPerVoxel = 1;
		int numberOfSpatialVolumes = 1;
		int numberOfFrames = 0;

		int[] sizes = nrrd.getSizes();
		String[] spaceDirections = nrrd.getSpaceDirections();
		if (spaceDirections != null) {
			int columnRowOrSlice = -1;
			for (int d=0; d<spaceDirections.length; ++d) {
				if (spaceDirections[d].equals("none")) {
					if (columnRowOrSlice == -1) {
						numberOfScalarsPerVoxel *= sizes[d];
					}
					else {
						numberOfSpatialVolumes *= sizes[d];
					}
				}
				else {
					++columnRowOrSlice;
					if (columnRowOrSlice == 2) {
						numberOfSlicesInVolume = sizes[d];
					}
					String[] vector = NRRDHeader.getVectorTripleValuesFromString("space directions["+d+"]",spaceDirections[d]);
					slf4jlogger.info("space directions[{}] vector array = {}",d,Arrays.toString(vector));
					rowColumnAndSliceDirectionVectors[columnRowOrSlice] = new double[3];
					double x = Double.parseDouble(vector[0]);
					double y = Double.parseDouble(vector[1]);
					double z = Double.parseDouble(vector[2]);
					rowColumnAndSliceDirectionVectors[columnRowOrSlice][0] = x;
					rowColumnAndSliceDirectionVectors[columnRowOrSlice][1] = y;
					rowColumnAndSliceDirectionVectors[columnRowOrSlice][2] = z;
					slf4jlogger.info("rowColumnAndSliceDirectionVectors[{}][0] = {}",columnRowOrSlice,rowColumnAndSliceDirectionVectors[columnRowOrSlice][0]);
					slf4jlogger.info("rowColumnAndSliceDirectionVectors[{}][1] = {}",columnRowOrSlice,rowColumnAndSliceDirectionVectors[columnRowOrSlice][1]);
					slf4jlogger.info("rowColumnAndSliceDirectionVectors[{}][2] = {}",columnRowOrSlice,rowColumnAndSliceDirectionVectors[columnRowOrSlice][2]);
					
					double magnitude = Math.sqrt(x*x+y*y+z*z);		// the spacing is recovered as the magnitude of the vector
					
					spacing[columnRowOrSlice] = magnitude;
					slf4jlogger.info("spacing[{}] = {}",columnRowOrSlice,spacing[columnRowOrSlice]);

					rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice] = new double[3];
					rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][0] = x/magnitude;	// DICOM wants unit vectors
					rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][1] = y/magnitude;
					rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][2] = z/magnitude;
					slf4jlogger.info("rowColumnAndSliceDirectionUnitVectors[{}][0] = {}",columnRowOrSlice,rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][0]);
					slf4jlogger.info("rowColumnAndSliceDirectionUnitVectors[{}][1] = {}",columnRowOrSlice,rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][1]);
					slf4jlogger.info("rowColumnAndSliceDirectionUnitVectors[{}][2] = {}",columnRowOrSlice,rowColumnAndSliceDirectionUnitVectors[columnRowOrSlice][2]);
				}
			}
			numberOfFrames = numberOfScalarsPerVoxel * numberOfSlicesInVolume * numberOfSpatialVolumes;
		}

		list = FunctionalGroupUtilities.createFunctionalGroupsIfNotPresent(list,numberOfFrames);
		SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
		SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
		AttributeList sharedFunctionalGroupsSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(aSharedFunctionalGroupsSequence);

		{
			SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
			sharedFunctionalGroupsSequenceList.put(aPixelMeasuresSequence);
			AttributeList itemList = new AttributeList();
			aPixelMeasuresSequence.addItem(itemList);

			// note that order in DICOM in PixelSpacing is "adjacent row spacing", then "adjacent column spacing" ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(spacing[1]); a.addValue(spacing[0]); itemList.put(a); }
			// note that NRRD does not distinguish slice spacing from slice thickness (i.e., no overlap or gap description) ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.SliceThickness); a.addValue(spacing[2]); itemList.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.SpacingBetweenSlices); a.addValue(spacing[2]); itemList.put(a); }
		}

		{
			SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
			sharedFunctionalGroupsSequenceList.put(aPlaneOrientationSequence);
			AttributeList itemList = new AttributeList();
			aPlaneOrientationSequence.addItem(itemList);
			Attribute a = new DecimalStringAttribute(TagFromName.ImageOrientationPatient);
			
			a.addValue(directionCorrection[0] * rowColumnAndSliceDirectionUnitVectors[0][0]);
			a.addValue(directionCorrection[1] * rowColumnAndSliceDirectionUnitVectors[0][1]);
			a.addValue(directionCorrection[2] * rowColumnAndSliceDirectionUnitVectors[0][2]);
			
			a.addValue(directionCorrection[0] * rowColumnAndSliceDirectionUnitVectors[1][0]);
			a.addValue(directionCorrection[1] * rowColumnAndSliceDirectionUnitVectors[1][1]);
			a.addValue(directionCorrection[2] * rowColumnAndSliceDirectionUnitVectors[1][2]);
			
			itemList.put(a);
		}
		
		{
			// Follow exactly the same pattern as we used translating the input pixels to the output organization ...
			for (int spatialVolume=0; spatialVolume<numberOfSpatialVolumes; ++spatialVolume) {
				for (int sliceWithinVolume=0; sliceWithinVolume<numberOfSlicesInVolume; ++sliceWithinVolume) {
					for (int scalarWithinVoxel=0; scalarWithinVoxel<numberOfScalarsPerVoxel; ++scalarWithinVoxel) {
						int dstFrameNumber = preSpatialDimensionsLeastRapidlyVaryingInOutput
												? (spatialVolume*numberOfScalarsPerVoxel + scalarWithinVoxel)*numberOfSlicesInVolume  + sliceWithinVolume
												: (spatialVolume*numberOfSlicesInVolume  + sliceWithinVolume)*numberOfScalarsPerVoxel + scalarWithinVoxel;

						{
							SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
							SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,dstFrameNumber).put(aPlanePositionSequence);
		
							AttributeList itemList = new AttributeList();
							aPlanePositionSequence.addItem(itemList);
							
							Attribute a = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
				
							a.addValue(directionCorrection[0] * (rowColumnAndSliceDirectionVectors[2][0] * sliceWithinVolume + origin[0]));
							a.addValue(directionCorrection[1] * (rowColumnAndSliceDirectionVectors[2][1] * sliceWithinVolume + origin[1]));
							a.addValue(directionCorrection[2] * (rowColumnAndSliceDirectionVectors[2][2] * sliceWithinVolume + origin[2]));
				
							itemList.put(a);
						}
						{
							SequenceAttribute aFrameContentSequence = new SequenceAttribute(TagFromName.FrameContentSequence);
							SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(aPerFrameFunctionalGroupsSequence,dstFrameNumber).put(aFrameContentSequence);

							AttributeList itemList = new AttributeList();
							aFrameContentSequence.addItem(itemList);
							
							{ Attribute a = new ShortStringAttribute(TagFromName.StackID); a.addValue(spatialVolume+1); itemList.put(a); }
							{ Attribute a = new UnsignedLongAttribute(TagFromName.InStackPositionNumber); a.addValue(sliceWithinVolume+1); itemList.put(a); }
							{ Attribute a = new UnsignedLongAttribute(TagFromName.DimensionIndexValues); a.addValue(spatialVolume+1); a.addValue(sliceWithinVolume+1); itemList.put(a); }
						}
					}
				}
			}
		}
		return list;
	}

	/**
	 * <p>Create a Dimensions Module.</p>
	 *
	 * @param	list
	 * return				attribute list with Dimensions Module added
	 * @throws			DicomException
	 */
	public static AttributeList generateDimensions(AttributeList list) throws DicomException {
		UIDGenerator u = new UIDGenerator();
		String dimensionOrganizationUID = u.getAnotherNewUID();
		{
			SequenceAttribute saDimensionOrganizationSequence = new SequenceAttribute(TagFromName.DimensionOrganizationSequence);
			list.put(saDimensionOrganizationSequence);
			AttributeList itemList = new AttributeList();
			saDimensionOrganizationSequence.addItem(itemList);
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
		}
		// specify order of dimension indices by StackID first, then by InStackPositionNumber
		{
			SequenceAttribute saDimensionIndexSequence = new SequenceAttribute(TagFromName.DimensionIndexSequence);
			list.put(saDimensionIndexSequence);
			{
				AttributeList itemList = new AttributeList();
				saDimensionIndexSequence.addItem(itemList);
				{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.StackID); itemList.put(a); }
				{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.FrameContentSequence); itemList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("StackID"); itemList.put(a); }
			}
			{
				AttributeList itemList = new AttributeList();
				saDimensionIndexSequence.addItem(itemList);
				{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.InStackPositionNumber); itemList.put(a); }
				{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.FrameContentSequence); itemList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("InStackPositionNumber"); itemList.put(a); }
			}
		}
		return list;
	}
		
	/**
	 * <p>Using an NRRD image input file and header, create DICOM Pixel Data Module attributes.</p>
	 *
	 * @param	inputFile	an NRRD format image file
	 * @param	nrrd		an NRRD header already read from the inputFile
	 * @param	list		an existing (possibly empty) attribute list, if null, a new one will be created; may already include "better" image pixel module attributes to use
	 * return				attribute list with Image Pixel Module (including Pixel Data) and other attributes added
	 * @throws			IOException
	 * @throws			DicomException
	 * @throws			NRRDException
	 * @throws			NumberFormatException
	 */
	public static AttributeList generateDICOMPixelDataModuleAttributesFromNRRDFile(File inputFile,NRRDHeader nrrd,AttributeList list) throws IOException, DicomException, NRRDException, NumberFormatException {
		if (list == null) {
			list = new AttributeList();
		}
		
		int numberOfDimensions = nrrd.getDimension();
		if (numberOfDimensions < 2) {
			throw new DicomException("Cannot convert if less than two dimensions");
		}
		int[] sizes = nrrd.getSizes();
		if (numberOfDimensions != sizes.length) {
			throw new DicomException("Inconsistent number of dimensions = "+ numberOfDimensions+" and length of size array = "+sizes.length);
		}
		
		int columns = 0;
		int rows = 0;
		int numberOfFrames = 0;
		
		int numberOfSlicesInVolume = 1;
		int numberOfScalarsPerVoxel = 1;
		int numberOfSpatialVolumes = 1;

		String[] spaceDirections = nrrd.getSpaceDirections();
		if (spaceDirections == null) {
			if (numberOfDimensions <= 3) {
				columns = sizes[0];
				rows = sizes[1];
				numberOfFrames = numberOfDimensions == 3 ? sizes[2] : 1;
			}
			else {
				throw new DicomException("Number of dimensions is greater than 3 ("+ numberOfDimensions+") and no information about which dimensions are space");
			}
		}
		else {
			if (numberOfDimensions == spaceDirections.length) {
				// e.g. "space directions: none (-1.015395,0.016303,0.012305) (-0.012305,-0.976496,0.278831) (0.096341,1.646072,5.768977)"
				int spatialDimensionIndex = -1;
				for (int d=0; d<numberOfDimensions; ++d) {
					if (spaceDirections[d].equals("none")) {
						if (spatialDimensionIndex == -1) {
							numberOfScalarsPerVoxel *= sizes[d];
						}
						else {
							numberOfSpatialVolumes *= sizes[d];
						}
					}
					else {
						// otherwise assume is value is a vector without checking; later we will also assume spatial dimensions are together and not separated :(
						++spatialDimensionIndex;
						if (spatialDimensionIndex == 0) {
							columns = sizes[d];
						}
						else if (spatialDimensionIndex == 1) {
							rows = sizes[d];
						}
						else if (spatialDimensionIndex == 2) {
							numberOfSlicesInVolume = sizes[d];
						}
						else {
							throw new DicomException("More than three spatial dimensions in space directions");
						}
					}
				}
				numberOfFrames = numberOfScalarsPerVoxel * numberOfSlicesInVolume * numberOfSpatialVolumes;
			}
			else {
				throw new DicomException("Inconsistent number of dimensions = "+ numberOfDimensions+" and length of space directions array = "+spaceDirections.length);
			}
		}
		slf4jlogger.info("numberOfScalarsPerVoxel = {}",numberOfScalarsPerVoxel);
		slf4jlogger.info("columns = {}",columns);
		slf4jlogger.info("rows = {}",rows);
		slf4jlogger.info("numberOfSlicesInVolume = {}",numberOfSlicesInVolume);
		slf4jlogger.info("numberOfSpatialVolumes = {}",numberOfSpatialVolumes);
		slf4jlogger.info("numberOfFrames = {}",numberOfFrames);
		
		String photometricInterpretation = null;
		int samplesPerPixel = 0;
		int depth = 0;
		int pixelRepresentation = 0;
		Attribute aPixelData = null;
		boolean sendBitsStored = true;
		boolean sendHighBit = true;
		boolean sendPixelRepresentation = true;
		NRRDHeader.Type type = nrrd.getType();
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
			case BLOCK:		throw new DicomException("Conversion of "+type+" not supported");
			default:		throw new DicomException("Conversion of "+type+" not supported");
		}
		
		// really could do better than reading everything into memory, but expedient and handles byte ordering on input and output ...
		
		long useOffset = 0;
		String useDataFileName = nrrd.getDataFile();
		File useDataFile = null;
		if (useDataFileName != null) {
			// need to make sure that we build correct path, since header just specifies local name
			useDataFile = new File(inputFile.getParentFile(),useDataFileName);
		}
		else if (nrrd.byte_offset_of_binary > 0) {
			useDataFile = inputFile;
			useOffset = (long)nrrd.byte_offset_of_binary;
		}
		
		double minPixelValue = 0;
		double maxPixelValue = 0;
		boolean insertWindowValues = false;
		if (useDataFile != null) {
			InputStream in = new BufferedInputStream(new FileInputStream(useDataFile));
			if (useOffset > 0) {
				CopyStream.skipInsistently(in,useOffset);
			}
			if (nrrd.isDataGZIPEncoded()) {
				in = new GZIPInputStream(in);
			}
			
			BinaryInputStream nrrdPixelData = new BinaryInputStream(in,nrrd.isDataBigEndian());
			
			int numberOfPixels = rows * columns * numberOfFrames * samplesPerPixel;
			if (aPixelData instanceof OtherByteAttribute) {
				byte[] values = new byte[numberOfPixels];
				nrrdPixelData.readInsistently(values,0,numberOfPixels);
				if (numberOfScalarsPerVoxel > 1) {	// no generic operations on basic data types in Java :( repeat this code for each type :(
					byte[] reorganizedValues = new byte[numberOfPixels];
					// iterate through dimensions in order in which they are supplied in source values
					// outer loop is least rapidly varying dimension in source values
					int srcIndex = 0;
					for (int spatialVolume=0; spatialVolume<numberOfSpatialVolumes; ++spatialVolume) {
						for (int sliceWithinVolume=0; sliceWithinVolume<numberOfSlicesInVolume; ++sliceWithinVolume) {
							for (int row=0; row<rows; ++row) {
								for (int column=0; column<columns; ++column) {
									for (int scalarWithinVoxel=0; scalarWithinVoxel<numberOfScalarsPerVoxel; ++scalarWithinVoxel) {
										int dstFrameNumber = preSpatialDimensionsLeastRapidlyVaryingInOutput
																? (spatialVolume*numberOfScalarsPerVoxel + scalarWithinVoxel)*numberOfSlicesInVolume  + sliceWithinVolume
																: (spatialVolume*numberOfSlicesInVolume  + sliceWithinVolume)*numberOfScalarsPerVoxel + scalarWithinVoxel;
										int dstPixel = (dstFrameNumber*rows + row)*columns + column;
										reorganizedValues[dstPixel] = values[srcIndex++];
									}
								}
							}
						}
					}
					values = reorganizedValues;
				}
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherWordAttribute) {
				short[] values = new short[numberOfPixels];
				nrrdPixelData.readUnsigned16(values,0,numberOfPixels);
				if (numberOfScalarsPerVoxel > 1) {	// no generic operations on basic data types in Java :( repeat this code for each type :(
					short[] reorganizedValues = new short[numberOfPixels];
					// iterate through dimensions in order in which they are supplied in source values
					// outer loop is least rapidly varying dimension in source values
					int srcIndex = 0;
					for (int spatialVolume=0; spatialVolume<numberOfSpatialVolumes; ++spatialVolume) {
						for (int sliceWithinVolume=0; sliceWithinVolume<numberOfSlicesInVolume; ++sliceWithinVolume) {
							for (int row=0; row<rows; ++row) {
								for (int column=0; column<columns; ++column) {
									for (int scalarWithinVoxel=0; scalarWithinVoxel<numberOfScalarsPerVoxel; ++scalarWithinVoxel) {
										int dstFrameNumber = preSpatialDimensionsLeastRapidlyVaryingInOutput
																? (spatialVolume*numberOfScalarsPerVoxel + scalarWithinVoxel)*numberOfSlicesInVolume  + sliceWithinVolume
																: (spatialVolume*numberOfSlicesInVolume  + sliceWithinVolume)*numberOfScalarsPerVoxel + scalarWithinVoxel;
										int dstPixel = (dstFrameNumber*rows + row)*columns + column;
										reorganizedValues[dstPixel] = values[srcIndex++];
									}
								}
							}
						}
					}
					values = reorganizedValues;
				}
				aPixelData.setValues(values);
			}
			else if (aPixelData instanceof OtherFloatAttribute) {
				float[] values = new float[numberOfPixels];
				nrrdPixelData.readFloat(values,numberOfPixels);
				if (numberOfScalarsPerVoxel > 1) {	// no generic operations on basic data types in Java :( repeat this code for each type :(
					float[] reorganizedValues = new float[numberOfPixels];
					// iterate through dimensions in order in which they are supplied in source values
					// outer loop is least rapidly varying dimension in source values
					int srcIndex = 0;
					for (int spatialVolume=0; spatialVolume<numberOfSpatialVolumes; ++spatialVolume) {
						for (int sliceWithinVolume=0; sliceWithinVolume<numberOfSlicesInVolume; ++sliceWithinVolume) {
							for (int row=0; row<rows; ++row) {
								for (int column=0; column<columns; ++column) {
									for (int scalarWithinVoxel=0; scalarWithinVoxel<numberOfScalarsPerVoxel; ++scalarWithinVoxel) {
										int dstFrameNumber = preSpatialDimensionsLeastRapidlyVaryingInOutput
																? (spatialVolume*numberOfScalarsPerVoxel + scalarWithinVoxel)*numberOfSlicesInVolume  + sliceWithinVolume
																: (spatialVolume*numberOfSlicesInVolume  + sliceWithinVolume)*numberOfScalarsPerVoxel + scalarWithinVoxel;
										int dstPixel = (dstFrameNumber*rows + row)*columns + column;
										reorganizedValues[dstPixel] = values[srcIndex++];
									}
								}
							}
						}
					}
					values = reorganizedValues;
				}
				aPixelData.setValues(values);
				float[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			else if (aPixelData instanceof OtherDoubleAttribute) {
				double[] values = new double[numberOfPixels];
				nrrdPixelData.readDouble(values,numberOfPixels);
				if (numberOfScalarsPerVoxel > 1) {	// no generic operations on basic data types in Java :( repeat this code for each type :(
					double[] reorganizedValues = new double[numberOfPixels];
					// iterate through dimensions in order in which they are supplied in source values
					// outer loop is least rapidly varying dimension in source values
					int srcIndex = 0;
					for (int spatialVolume=0; spatialVolume<numberOfSpatialVolumes; ++spatialVolume) {
						for (int sliceWithinVolume=0; sliceWithinVolume<numberOfSlicesInVolume; ++sliceWithinVolume) {
							for (int row=0; row<rows; ++row) {
								for (int column=0; column<columns; ++column) {
									for (int scalarWithinVoxel=0; scalarWithinVoxel<numberOfScalarsPerVoxel; ++scalarWithinVoxel) {
										int dstFrameNumber = preSpatialDimensionsLeastRapidlyVaryingInOutput
																? (spatialVolume*numberOfScalarsPerVoxel + scalarWithinVoxel)*numberOfSlicesInVolume  + sliceWithinVolume
																: (spatialVolume*numberOfSlicesInVolume  + sliceWithinVolume)*numberOfScalarsPerVoxel + scalarWithinVoxel;
										int dstPixel = (dstFrameNumber*rows + row)*columns + column;
										reorganizedValues[dstPixel] = values[srcIndex++];
									}
								}
							}
						}
					}
					values = reorganizedValues;
				}
				aPixelData.setValues(values);
				double[] minMixValues = ArrayCopyUtilities.minMax(values);
				minPixelValue = minMixValues[0];
				maxPixelValue = minMixValues[1];
				insertWindowValues = true;
			}
			
			nrrdPixelData.close();
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
	 * <p>Read an NRRD image input format files and create an image of a specified or appropriate SOP Class.</p>
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
	 * @throws			NRRDException
	 */
	public NRRDToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber)
			throws IOException, DicomException, NRRDException {
		this(inputFileName,outputFileName,patientName,patientID,studyID,seriesNumber,instanceNumber,null,null);
	}

	/**
	 * <p>Read an NRRD image input format files and create an image of a specified or appropriate SOP Class.</p>
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
	 * @throws			NRRDException
	 * @throws			NumberFormatException
	 */
	public NRRDToDicom(String inputFileName,String outputFileName,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber,String modality,String sopClass)
			throws IOException, DicomException, NRRDException, NumberFormatException {

		File inputFile = new File(inputFileName);
		NRRDHeader nrrd = new NRRDHeader(inputFile);
		
		AttributeList list = generateDICOMPixelDataModuleAttributesFromNRRDFile(inputFile,nrrd,null/*AttributeList*/);
			
		CommonConvertedAttributeGeneration.generateCommonAttributes(list,patientName,patientID,studyID,seriesNumber,instanceNumber,modality,sopClass);
		
		sopClass = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isEnhancedMultiframeImageStorage(sopClass)) {
			generateGeometryFunctionalGroupsFromNRRDHeader(nrrd,list);
			generateDimensions(list);
		}

		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read an NRRD image input format files and create an image of a specified or appropriate SOP Class.</p>
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
				System.err.println("Usage: NRRDToDicom inputFile outputFile patientName patientID studyID seriesNumber instanceNumber [modality [SOPClass]]");
				System.exit(1);
			}
			new NRRDToDicom(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],modality,sopClass);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

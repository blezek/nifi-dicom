/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.OtherByteAttribute;
import com.pixelmed.dicom.OtherWordAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.UnsignedLongAttribute;
import com.pixelmed.dicom.UnsignedShortAttribute;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.geometry.GeometryOfVolume;

import com.pixelmed.utils.ColorUtilities;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.rmi.dgc.VMID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class IndexedLabelMapToSegmentation {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/IndexedLabelMapToSegmentation.java,v 1.28 2017/02/28 14:57:35 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(IndexedLabelMapToSegmentation.class);
	
	protected String getStringNoLongerThan64TruncatedIfNecessary(String s) {
		return s.length() > 64 ? s.substring(0,64) : s;
	}
	
	protected class LabelInformation {
		String codeMeaning;
		String side;					// L, R or U
		int indexValue;					// what is found in the pixels
		String conceptUniqueIdentifier;	// UMLS CUI
		String conceptIdentifier;		// SNOMED ConceptID
		String snomedCodeValue;
		String fmaCodeValue;
		String dcmCodeValue;
		String red;
		String green;
		String blue;

		public LabelInformation(String codeMeaning,String side,int indexValue,String conceptUniqueIdentifier,String conceptIdentifier,String snomedCodeValue,String fmaCodeValue,String dcmCodeValue,
				String red,String green,String blue) {
			this.codeMeaning = codeMeaning;
			this.side = side;
			this.indexValue = indexValue;
			this.conceptUniqueIdentifier = conceptUniqueIdentifier;
			this.conceptIdentifier = conceptIdentifier;
			this.snomedCodeValue = snomedCodeValue;
			this.fmaCodeValue = fmaCodeValue;
			this.dcmCodeValue = dcmCodeValue;
			this.red = red;
			this.green = green;
			this.blue = blue;
//System.err.println("Adding "+this);
		}
		
		//public LabelInformation(String codeMeaning,String side,int indexValue,String conceptUniqueIdentifier,String conceptIdentifier,String snomedCodeValue,String fmaCodeValue,String dcmCodeValue) {
		//	this(codeMeaning,side,indexValue,conceptUniqueIdentifier,conceptIdentifier,snomedCodeValue,fmaCodeValue,dcmCodeValue,""/*red*/,""/*green*/,""/*blue*/);
		//}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("["+indexValue+"]");
			buf.append(" UMLS CUI = "+conceptUniqueIdentifier);
			buf.append(" SNOMED ConceptID = "+conceptIdentifier);
			buf.append(" ("+snomedCodeValue+",SRT,\""+codeMeaning+"\")");
			buf.append(" ("+fmaCodeValue+",FMA,\""+codeMeaning+"\")");
			buf.append(" ("+dcmCodeValue+",DCM,\""+codeMeaning+"\")");
			buf.append(" rgb = ("+red+","+green+","+blue+")");
			return buf.toString();
		}
		
		public boolean hasCode() {
			return snomedCodeValue.length() > 0 || fmaCodeValue.length() > 0 || dcmCodeValue.length() > 0 || conceptUniqueIdentifier.length() > 0;
		}
		
		public CodedSequenceItem getCodedSequenceItem() throws DicomException {
			String codingSchemeDesignator = "";
			String codeValue = "";
			if (snomedCodeValue.length() > 0) {
				codingSchemeDesignator = "SRT";
				codeValue = snomedCodeValue;
			}
			else if (fmaCodeValue.length() > 0) {
				codingSchemeDesignator = "FMA";
				codeValue = fmaCodeValue;
			}
			else if (dcmCodeValue.length() > 0) {
				codingSchemeDesignator = "DCM";
				codeValue = dcmCodeValue;
			}
			else if (conceptUniqueIdentifier.length() > 0) {
				codingSchemeDesignator = "UMLS";
				codeValue = conceptUniqueIdentifier;
			}
			return new CodedSequenceItem(codeValue,codingSchemeDesignator,getStringNoLongerThan64TruncatedIfNecessary(codeMeaning));
		}
		
		public boolean hasColor() {
			return red.length() > 0 && green.length() > 0 && blue.length() > 0;
		}

		public Attribute getRecommendedDisplayCIELabValue() throws DicomException {
			Attribute a = null;
			if (hasColor()) {
				int[] rgb = { Integer.parseInt(red),Integer.parseInt(green),Integer.parseInt(blue) };
				int[] cieLabScaled = ColorUtilities.getIntegerScaledCIELabPCSFromSRGB(rgb);	// per PS 3.3 C.10.7.1.1 ... scale same way as ICC profile encodes them
				a = new UnsignedShortAttribute(TagFromName.RecommendedDisplayCIELabValue);
				a.addValue(cieLabScaled[0]);
				a.addValue(cieLabScaled[1]);
				a.addValue(cieLabScaled[2]);
			}
			return a;
		}
	}

	protected Map<Integer,LabelInformation> readLabelMapFile(String filename) throws IOException, NumberFormatException {
		Map<Integer,LabelInformation> labelMap = new HashMap<Integer,LabelInformation>();
		LineNumberReader r = new LineNumberReader(new FileReader(filename));
		String line = null;
		while ((line=r.readLine()) != null) {
			String[] values = line.split(",");
//System.err.println("values.length = "+values.length);
			if (values.length >= 7 && !values[0].equals("Original Description")) {	// skip header row
				//Original Description,Meaning,Side,Index Value,UMLS CUID,SNOMED CONCEPTID,SNOMEDID,FMA,Notes,R,G,B,DCM
				Integer index = new Integer(values[3].trim());
//System.err.println("Adding index "+index);
				labelMap.put(index,new LabelInformation(values[1].trim(),values[2].trim(),index.intValue(),values[4].trim(),values[5].trim(),values[6].trim(),
					(values.length > 7 ? values[7].trim() : ""),
					(values.length > 12 ? values[12].trim() : ""),
					(values.length > 11 ? values[9].trim() : ""),
					(values.length > 11 ? values[10].trim() : ""),
					(values.length > 11 ? values[11].trim() : "")
				));
			}
			else {
//System.err.println("Not adding line = "+line);
			}
		}
		r.close();
		return labelMap;
	}
	
	protected static final Set<String> spatialAndRelationalConcepts = new HashSet<String>();
	{
		spatialAndRelationalConcepts.add("C1706907");		// background
	}
	
	protected static final Set<String> tissueConcepts = new HashSet<String>();
	{
		tissueConcepts.add("C1123023");		// skin
	}
	
	protected void addAppropriateSegmentedPropertyCategoryCodeSequence(AttributeList list,LabelInformation labelinfo) throws DicomException {
		CodedSequenceItem category = null;
		if (spatialAndRelationalConcepts.contains(labelinfo.conceptUniqueIdentifier)) {
			category = new CodedSequenceItem("R-42018","SRT","Spatial and Relational Concept");
		}
		else if (tissueConcepts.contains(labelinfo.conceptUniqueIdentifier)) {
			category = new CodedSequenceItem("T-D0050","SRT","Tissue"); 
		}
		else {
			category = new CodedSequenceItem("T-D000A","SRT","Anatomical Structure");		// default to anatomy since that is by far the most common
		}
		addCodedSequenceAttribute(list,TagFromName.SegmentedPropertyCategoryCodeSequence,category);
	}
	
	protected static SequenceAttribute newCodedSequenceAttribute(AttributeTag tag,CodedSequenceItem csi) {
		SequenceAttribute sa = new SequenceAttribute(tag);
		AttributeList itemList = new AttributeList();
		sa.addItem(itemList);
		itemList.putAll(csi.getAttributeList());
		return sa;
	}
	
	protected static SequenceAttribute addCodedSequenceAttribute(AttributeList list,AttributeTag tag,CodedSequenceItem csi) {
		SequenceAttribute sa = newCodedSequenceAttribute(tag,csi);
		list.put(sa);
		return sa;
	}
	
	protected void addSegmentSequenceItem(SequenceAttribute saSegmentSequence,int index,LabelInformation labelinfo) throws DicomException {
		AttributeList segmentSequenceItemList = new AttributeList();
		saSegmentSequence.addItem(segmentSequenceItemList);
			
		{ Attribute a = new UnsignedShortAttribute(TagFromName.SegmentNumber); a.addValue(index+1); segmentSequenceItemList.put(a); }				// use index+1, since a value of 0 is encountered but not permitted
		{ Attribute a = new LongStringAttribute(TagFromName.SegmentLabel); a.addValue(labelinfo == null ? "Topography unknown" : getStringNoLongerThan64TruncatedIfNecessary(labelinfo.codeMeaning)); segmentSequenceItemList.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.SegmentAlgorithmType); a.addValue("MANUAL"); segmentSequenceItemList.put(a); }			// should parameterize this :(
		
		SequenceAttribute saSegmentedPropertyTypeCodeSequence = null;
		if (labelinfo != null && labelinfo.hasCode()) {
			addAppropriateSegmentedPropertyCategoryCodeSequence(segmentSequenceItemList,labelinfo);

			saSegmentedPropertyTypeCodeSequence = addCodedSequenceAttribute(segmentSequenceItemList,TagFromName.SegmentedPropertyTypeCodeSequence,labelinfo.getCodedSequenceItem());
		}
		else {
			addCodedSequenceAttribute(segmentSequenceItemList,TagFromName.SegmentedPropertyCategoryCodeSequence,new CodedSequenceItem("R-42018","SRT","Spatial and Relational Concept"));
			saSegmentedPropertyTypeCodeSequence = addCodedSequenceAttribute(segmentSequenceItemList,TagFromName.SegmentedPropertyTypeCodeSequence,new CodedSequenceItem("T-D0001","SRT","Topography unknown"));
		}

		if (labelinfo != null && saSegmentedPropertyTypeCodeSequence != null) {		// add side if present even if Topography unknown
			if (labelinfo.side.equals("R")) {
				saSegmentedPropertyTypeCodeSequence.getItem(0).getAttributeList().put(newCodedSequenceAttribute(TagFromName.SegmentedPropertyTypeModifierCodeSequence,new CodedSequenceItem("G-A100","SRT","Right")));
			}
			else if (labelinfo.side.equals("L")) {
				saSegmentedPropertyTypeCodeSequence.getItem(0).getAttributeList().put(newCodedSequenceAttribute(TagFromName.SegmentedPropertyTypeModifierCodeSequence,new CodedSequenceItem("G-A101","SRT","Left")));
			}
		}
		
		if (labelinfo != null && labelinfo.hasColor()) {
			Attribute a = labelinfo.getRecommendedDisplayCIELabValue();
			segmentSequenceItemList.put(a);
		}
		else {
//System.err.println("No RecommendedDisplayCIELabValue for "+index);
		}
	}

	
	protected void addSegmentSequenceItem(SequenceAttribute saSegmentSequence,LabelInformation labelinfo) throws DicomException {
		addSegmentSequenceItem(saSegmentSequence,labelinfo.indexValue,labelinfo);
	}
	
	// C1706907 means "NCI: Existing conditions, especially those that would be confused with the phenomenon to be observed or measured."
	protected LabelInformation backgroundLabel =  new LabelInformation("Background","U",0,"C1706907"/*not ideal choice*/,""/*conceptIdentifier*/,""/*snomedCodeValue*/,""/*fmaCodeValue*/,"125040","0","0","0");
	protected Integer  backgroundIndex = new Integer(backgroundLabel.indexValue);
	
	protected Map<Integer,Integer> addSegmentSequence(AttributeList list,Set<Integer>usedLabels,Map<Integer,LabelInformation> labelMap,Set<Integer>unrecognizedLabels) throws DicomException {
		SequenceAttribute asSegmentSequence = new SequenceAttribute(TagFromName.SegmentSequence);
		list.put(asSegmentSequence);
		
		Map<Integer,Integer> segmentDimensionIndexValueByLabelMapIndex = new HashMap<Integer,Integer>();
		int countSegmentSequenceItems = 0;
		
		for (Integer index : labelMap.keySet()) {
			if (usedLabels.contains(index)) {
				LabelInformation label = labelMap.get(index);
				if (index.equals(backgroundIndex) && !label.hasCode() && backgroundLabel.codeMeaning.toLowerCase().equals(label.codeMeaning.toLowerCase())) {
					slf4jlogger.info("Replacing supplied background with standard background label because no code available");
					label = backgroundLabel;
				}
				addSegmentSequenceItem(asSegmentSequence,label);
				++countSegmentSequenceItems;
				segmentDimensionIndexValueByLabelMapIndex.put(index,new Integer(countSegmentSequenceItems));	// note that DimensionIndexValue starts from 1, not 0
			}
		}
		for (Integer index : unrecognizedLabels) {
			if (index.equals(backgroundIndex)) {
				slf4jlogger.info("Adding standard background label");
				addSegmentSequenceItem(asSegmentSequence,backgroundLabel);
				++countSegmentSequenceItems;
				segmentDimensionIndexValueByLabelMapIndex.put(index,new Integer(countSegmentSequenceItems));
			}
			else {
				addSegmentSequenceItem(asSegmentSequence,index.intValue(),null/*LabelInformation*/);
				++countSegmentSequenceItems;
				segmentDimensionIndexValueByLabelMapIndex.put(index,new Integer(countSegmentSequenceItems));
			}
		}

		return segmentDimensionIndexValueByLabelMapIndex;
	}
	
	protected void setBit(byte[] pixelData,int f,int r,int c,int rows,int columns) {
		long pixelsPerFrame = rows * columns;
		long pixelOffset = pixelsPerFrame * f + columns * r + c;
		int byteOffset = (int)(pixelOffset/8);
		int bitOffset = (int)(pixelOffset%8);
		int selector = 1<<bitOffset;	// fill from low end
		pixelData[byteOffset] = (byte)(pixelData[byteOffset] | selector);
	}
	
	public IndexedLabelMapToSegmentation(String inputFilename,String labelFilename,String outputFilename,String referenceImageFilename,
				String seriesNumber,String seriesDescription,
				String contentLabel,String contentDescription,String contentCreatorName
			) throws IOException, NumberFormatException, DicomException {
		Map<Integer,LabelInformation> labelMap = readLabelMapFile(labelFilename);
		
		UIDGenerator u = new UIDGenerator();
		
		String referenceImageSOPClassUID = "";
		String referenceImageSOPInstanceUID = "";
		String referenceStudyInstanceUID = "";
		String referenceSeriesInstanceUID = "";
		String referenceFrameOfReferenceUID = "";
		String referenceStudyDate = "";
		String referenceStudyTime = "";
		if (referenceImageFilename.length() > 0) {
			AttributeList referenceImageList = new AttributeList();
			referenceImageList.read(referenceImageFilename);
			   referenceImageSOPClassUID = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.SOPClassUID);
			referenceImageSOPInstanceUID = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.SOPInstanceUID);
			   referenceStudyInstanceUID = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.StudyInstanceUID);
			  referenceSeriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.SeriesInstanceUID);
			referenceFrameOfReferenceUID = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.FrameOfReferenceUID);
					  referenceStudyDate = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.StudyDate);
					  referenceStudyTime = Attribute.getSingleStringValueOrEmptyString(referenceImageList,TagFromName.StudyTime);
		}

		AttributeList list = new AttributeList();
		list.read(inputFilename);
		int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.SamplesPerPixel,1);
		int bitsStored = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsStored,1);
		int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsAllocated,1);
		
		short[] srcPixelDataShort = null;
		byte[] srcPixelDataByte = null;
		
		if (samplesPerPixel == 1) {
			if (bitsAllocated == 8) {
				srcPixelDataByte = list.getPixelData().getByteValues();
			}
			else if (bitsAllocated == 16) {
				srcPixelDataShort = list.getPixelData().getShortValues();
			}
		}
		
		if (srcPixelDataByte == null && srcPixelDataShort == null) {
			throw new DicomException("Only single channel input images 8 or 16 bit BitsAllocated supported - got SamplesPerPixel="+samplesPerPixel+" BitsStored="+bitsStored+" BitsAllocated="+bitsAllocated);
		}
		
		int srcNumberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
		int srcRows = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);
		int srcColumns = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		int srcPixelsPerFrame = srcRows * srcColumns;
		
		// prepare the spatial dimension information
		Map <Double,Integer> positionDimensionIndexValueByDistanceFromOrigin = new HashMap <Double,Integer>();
		GeometryOfVolume srcGeometry = new GeometryOfVolumeFromAttributeList(list);
		{
			double[] distances = srcGeometry.getDistanceAlongNormalFromOrigin();
			Arrays.sort(distances);
			int dimensionIndexValue = 0;;
			double lastDistance = Double.NaN;
			for (double distance : distances) {
				if (distance != lastDistance) {
					++dimensionIndexValue;		// start from 1, not 0
					positionDimensionIndexValueByDistanceFromOrigin.put(new Double(distance),new Integer(dimensionIndexValue));
//System.err.println("position dimension index "+dimensionIndexValue+" at position "+distance);
					lastDistance = distance;
				}
			}
		}
		
		// 1st pass through the pixel data values in order to:
		// - check which indices are used and whether they have labels or not
		// - build a data structure describing which indices are actually used in which frame, to drive the 2nd pass
		// - build the Dimensions module to reference later with the DimensionIndexValues in the FrameContent functional group

		Set<Integer> unrecognizedLabels = new HashSet<Integer>();
		Set<Integer> usedLabels = new HashSet<Integer>();
		Map<Integer,Set<Integer>> encounteredIndicesBySrcFrame = new HashMap<Integer,Set<Integer>>();	// could have used array of size srcNumberOfFrames, but no generic arrays in Java :(
		
		for (int srcFrameNumber=0; srcFrameNumber<srcNumberOfFrames; ++srcFrameNumber) {
			Set<Integer> encounteredIndexInSrcFrame = new HashSet<Integer>();
			encounteredIndicesBySrcFrame.put(new Integer(srcFrameNumber),encounteredIndexInSrcFrame);
			int frameOffset = srcPixelsPerFrame * srcFrameNumber;
			for (int r=0; r<srcRows; ++r) {
				int rowOffset = frameOffset + srcColumns * r;
				for (int c=0; c<srcColumns; ++c) {
					int srcOffset = rowOffset + c;										// could just assume ++ increment from 0 rather than recalculate :(
					int srcIndex = (srcPixelDataShort == null) ? (srcPixelDataByte[srcOffset] & 0xff) : (srcPixelDataShort[srcOffset] & 0xffff);
					Integer srcIndexKey = new Integer(srcIndex);
					encounteredIndexInSrcFrame.add(srcIndexKey);						// regardless of whether it has corresponding label information (is recognized) or not
					LabelInformation labelinfo = labelMap.get(srcIndexKey);
					if (labelinfo == null) {
						if (!unrecognizedLabels.contains(srcIndexKey)) {
							unrecognizedLabels.add(srcIndexKey);
							slf4jlogger.warn("No label information for pixel index value {}",srcIndex);
						}
					}
					else {
						if (!usedLabels.contains(srcIndexKey)) {
							usedLabels.add(srcIndexKey);
							if (!labelinfo.hasCode()) {
								slf4jlogger.warn("Label information without code for pixel index value {}",srcIndex);
							}
						}
					}
				}
			}
		}

		for (Integer index : labelMap.keySet()) {
			if (!usedLabels.contains(index)) {
				slf4jlogger.warn("Label index value {} is not used",index);
			}
		}
		
		// count the number of destination frames so that we can preallocate a fixed sized array ... (could find a way to do this in one pass :()

		int dstNumberOfFrames = 0;
		for (int srcFrameNumber=0; srcFrameNumber<srcNumberOfFrames; ++srcFrameNumber) {
//System.err.print("Frame "+srcFrameNumber+": ");
			Set<Integer> encounteredIndexInSrcFrame = encounteredIndicesBySrcFrame.get(new Integer(srcFrameNumber));
			if (encounteredIndexInSrcFrame.size() == 1 && encounteredIndexInSrcFrame.contains(backgroundIndex)) {
//System.err.print(" only background - ignoring");
			}
			else {
				for (Integer indexValue : encounteredIndexInSrcFrame) {
//System.err.print(" "+indexValue);
					++dstNumberOfFrames;	// i.e., for each index value in each source frame that is not entirely background, there will be a destination frame (including one for the background)
				}
			}
//System.err.println();
		}
//System.err.println("dstNumberOfFrames = "+dstNumberOfFrames);

		Map<Integer,Integer> segmentDimensionIndexValueByLabelMapIndex = addSegmentSequence(list,usedLabels,labelMap,unrecognizedLabels);

		{
			String dimensionOrganizationUID = u.getAnotherNewUID();
			{
				SequenceAttribute saDimensionOrganizationSequence = new SequenceAttribute(TagFromName.DimensionOrganizationSequence);
				list.put(saDimensionOrganizationSequence);
				AttributeList itemList = new AttributeList();
				saDimensionOrganizationSequence.addItem(itemList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
			}
			// specify order of dimension indices by segment first, then by position, since this is opposite the encoded order of the frames (and hence useful)
			{
				SequenceAttribute saDimensionIndexSequence = new SequenceAttribute(TagFromName.DimensionIndexSequence);
				list.put(saDimensionIndexSequence);
				{
					AttributeList itemList = new AttributeList();
					saDimensionIndexSequence.addItem(itemList);
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.ReferencedSegmentNumber); itemList.put(a); }
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.SegmentIdentificationSequence); itemList.put(a); }
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
					{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("SegmentNumber"); itemList.put(a); }
				}
				{
					AttributeList itemList = new AttributeList();
					saDimensionIndexSequence.addItem(itemList);
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.ImagePositionPatient); itemList.put(a); }
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.PlanePositionSequence); itemList.put(a); }
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
					{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("ImagePosition(Patient)"); itemList.put(a); }
				}
			}
		}
		
		long pixelsPerFrame = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0) * Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		long dstPixelCount = dstNumberOfFrames*pixelsPerFrame;
//System.err.println("dstPixelCount = "+dstPixelCount);
		long sizeOfDstPixelData = dstPixelCount/8;
		if (dstPixelCount % 8 > 0) {		// must be room for last few single bit pixels even if they do not fill a whole byte
			++sizeOfDstPixelData;
		}
		if (sizeOfDstPixelData % 2 > 0) {	// must be even length
			++sizeOfDstPixelData;
		}
//System.err.println("sizeOfDstPixelData = "+sizeOfDstPixelData);
		if (sizeOfDstPixelData > Integer.MAX_VALUE) {
			throw new DicomException("Requested PixelData byte array too large "+sizeOfDstPixelData);
		}
		byte[] dstPixelData = new byte[(int)sizeOfDstPixelData];
		{ Attribute a = new OtherByteAttribute(TagFromName.PixelData); a.setValues(dstPixelData); list.put(a); }
		
		// 2nd pass ... create the destination frame content and per-frame functional groups sequence by walking the data structure created in the 1st pass ...
		
		SequenceAttribute srcPerFrameFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.PerFrameFunctionalGroupsSequence));
		SequenceAttribute srcSharedFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.SharedFunctionalGroupsSequence));
		
		SequenceAttribute dstPerFrameFunctionalGroupsSequence = new SequenceAttribute(TagFromName.PerFrameFunctionalGroupsSequence);
		list.put(dstPerFrameFunctionalGroupsSequence);
		for (int f=0; f<dstNumberOfFrames; ++f) {
			dstPerFrameFunctionalGroupsSequence.addItem(new AttributeList());
		}
		
		// same sized frame for now (until we crop to bounding box)
		int dstRows = srcRows;
		int dstColumns = srcColumns;

		int dstFrameNumber = 0;
		for (int srcFrameNumber=0; srcFrameNumber<srcNumberOfFrames; ++srcFrameNumber) {
			Set<Integer> encounteredIndexInSrcFrame = encounteredIndicesBySrcFrame.get(new Integer(srcFrameNumber));
			if (!(encounteredIndexInSrcFrame.size() == 1 && encounteredIndexInSrcFrame.contains(backgroundIndex))) {	// same order and selection criteria as when precomputing dstNumberOfFrames
				for (Integer index : encounteredIndexInSrcFrame) {
					int indexValue = index.intValue();
					AttributeList dstPerFrameList = SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(dstPerFrameFunctionalGroupsSequence,dstFrameNumber);
					
					// for this destination frame, create a reference to the segment information using the indexValue+1 as the ReferencedSegmentNumber in the SegmentIdentificationSequence ...
					{
						SequenceAttribute asSegmentIdentificationSequence = new SequenceAttribute(TagFromName.SegmentIdentificationSequence);
						dstPerFrameList.put(asSegmentIdentificationSequence);
						AttributeList itemList = new AttributeList();
						asSegmentIdentificationSequence.addItem(itemList);
						{ Attribute a = new UnsignedShortAttribute(TagFromName.ReferencedSegmentNumber); a.addValue(indexValue+1); itemList.put(a); }
					}
					
					// re-use the source PlanePositionSequence for this destination frame ...
					{
						Attribute aPlanePositionSequence = SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(srcPerFrameFunctionalGroupsSequence,srcFrameNumber).get(TagFromName.PlanePositionSequence);
						dstPerFrameList.put(aPlanePositionSequence);
					}
					
					// create a FrameContentSequence for this destination frame and put the DimensionIndexValues in it ...
					{
						SequenceAttribute asFrameContentSequence = new SequenceAttribute(TagFromName.FrameContentSequence);
						dstPerFrameList.put(asFrameContentSequence);
						AttributeList itemList = new AttributeList();
						asFrameContentSequence.addItem(itemList);
						{
							Attribute a = new UnsignedLongAttribute(TagFromName.DimensionIndexValues);
							double position = srcGeometry.getGeometryOfSlice(srcFrameNumber).getDistanceAlongNormalFromOrigin();
							a.addValue(segmentDimensionIndexValueByLabelMapIndex.get(index));
							a.addValue(positionDimensionIndexValueByDistanceFromOrigin.get(new Double(position)));
							itemList.put(a);
						}
					}
					
					// now populate the PixelData for this frame ...
					int srcFrameOffset = srcPixelsPerFrame * srcFrameNumber;
					for (int r=0; r<srcRows; ++r) {
						int srcRowOffset = srcFrameOffset + srcColumns * r;
						for (int c=0; c<srcColumns; ++c) {
							int srcOffset = srcRowOffset + c;
							int srcIndex = (srcPixelDataShort == null) ? (srcPixelDataByte[srcOffset] & 0xff) : (srcPixelDataShort[srcOffset] & 0xffff);
							if (srcIndex == indexValue) {
								setBit(dstPixelData,dstFrameNumber,r,c,dstRows,dstColumns);		// same row and column since same sized frame for now (until we crop to bounding box)
							}
							// else leave as allocated, which will be zero
						}
					}
					
					++dstFrameNumber;
				}
			}
		}
		assert(dstFrameNumber == dstNumberOfFrames);
		
		// re-use existing shared functional groups ... will contain PixelMeasures and PlaneOrientation Sequences; remove other stuff
		
		{
			AttributeList sharedList = SequenceAttribute.getAttributeListFromSelectedItemWithinSequence(srcSharedFunctionalGroupsSequence,0);
			
			// remove everything except PixelMeasures and PlaneOrientation (e.g., to get rid of PixelValueTransformationSequence, etc.
			{
				Iterator<Attribute> i = sharedList.values().iterator();
				while (i.hasNext()) {
					Attribute a = i.next();
					AttributeTag t = a.getTag();
					if (!t.equals(TagFromName.PixelMeasuresSequence)
					 && !t.equals(TagFromName.PlaneOrientationSequence)) {
						i.remove();
					}
					
				}
			}
			
			{
				SequenceAttribute asDerivationImageSequence = new SequenceAttribute(TagFromName.DerivationImageSequence);
				sharedList.put(asDerivationImageSequence);
				AttributeList derivationImageSequenceItemList = new AttributeList();
				asDerivationImageSequence.addItem(derivationImageSequenceItemList);
				{
					SequenceAttribute asDerivationCodeSequence = new SequenceAttribute(TagFromName.DerivationCodeSequence);
					derivationImageSequenceItemList.put(asDerivationCodeSequence);
					AttributeList derivationCodeSequenceItemList = new AttributeList();
					asDerivationCodeSequence.addItem(derivationCodeSequenceItemList);
					derivationCodeSequenceItemList.putAll(new CodedSequenceItem("113076","DCM","Segmentation").getAttributeList());
				}
				{
					SequenceAttribute asSourceImageSequence = new SequenceAttribute(TagFromName.SourceImageSequence);
					derivationImageSequenceItemList.put(asSourceImageSequence);
					AttributeList sourceImageSequenceItemList = new AttributeList();
					asSourceImageSequence.addItem(sourceImageSequenceItemList);
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referenceImageSOPClassUID); sourceImageSequenceItemList.put(a); }
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referenceImageSOPInstanceUID); sourceImageSequenceItemList.put(a); }
					{
						SequenceAttribute asPurposeOfReferenceCodeSequence = new SequenceAttribute(TagFromName.PurposeOfReferenceCodeSequence);
						sourceImageSequenceItemList.put(asPurposeOfReferenceCodeSequence);
						AttributeList purposeOfReferenceCodeSequenceItemList = new AttributeList();
						asPurposeOfReferenceCodeSequence.addItem(purposeOfReferenceCodeSequenceItemList);
						purposeOfReferenceCodeSequenceItemList.putAll(new CodedSequenceItem("121322","DCM","Source image for image processing operation").getAttributeList());
					}
				}
			}
		}
		
		{
			// Add Common Instance Reference Module
			SequenceAttribute asReferencedSeriesSequence = new SequenceAttribute(TagFromName.ReferencedSeriesSequence);
			list.put(asReferencedSeriesSequence);
			AttributeList referencedSeriesSequenceItemList = new AttributeList();
			asReferencedSeriesSequence.addItem(referencedSeriesSequenceItemList);
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(referenceSeriesInstanceUID); referencedSeriesSequenceItemList.put(a); }
			{
				SequenceAttribute asReferencedInstanceSequence = new SequenceAttribute(TagFromName.ReferencedInstanceSequence);
				referencedSeriesSequenceItemList.put(asReferencedInstanceSequence);
				AttributeList referencedInstanceSequenceItemList = new AttributeList();
				asReferencedInstanceSequence.addItem(referencedInstanceSequenceItemList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referenceImageSOPClassUID); referencedInstanceSequenceItemList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referenceImageSOPInstanceUID); referencedInstanceSequenceItemList.put(a); }
			}
		}
		
		{ Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(dstNumberOfFrames); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(1); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(1); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(0); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation); a.addValue(0); list.put(a); }
		
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.SegmentationStorage); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue("SEG"); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.ImageType); a.addValue("DERIVED"); a.addValue("PRIMARY"); list.put(a); }
		
		list.remove(TagFromName.RescaleIntercept);
		list.remove(TagFromName.RescaleSlope);
		list.remove(TagFromName.RescaleType);
		list.remove(TagFromName.ConversionType);
		list.remove(TagFromName.FrameIncrementPointer);
		list.remove(TagFromName.VOILUTFunction);
		
		{ Attribute a = new CodeStringAttribute(TagFromName.SegmentationType); a.addValue("BINARY"); list.put(a); }
		
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		
		if (referenceStudyInstanceUID.length() > 0)    { Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(referenceStudyInstanceUID); list.put(a); }
		if (referenceFrameOfReferenceUID.length() > 0) { Attribute a = new UniqueIdentifierAttribute(TagFromName.FrameOfReferenceUID); a.addValue(referenceFrameOfReferenceUID); list.put(a); }
		if (referenceStudyDate.length() > 0)           { Attribute a = new DateAttribute(TagFromName.StudyDate); a.addValue(referenceStudyDate); list.put(a); }
		if (referenceStudyTime.length() > 0)           { Attribute a = new TimeAttribute(TagFromName.StudyTime); a.addValue(referenceStudyTime); list.put(a); }

		{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue(seriesNumber); list.put(a); }
		// leave InstanceNumber the same
		
		{ Attribute a = new LongStringAttribute(TagFromName.Manufacturer); a.addValue("PixelMed"); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.DeviceSerialNumber); a.addValue(new VMID().toString()); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.SoftwareVersions); a.addValue(VersionAndConstants.getBuildDate()); list.put(a); }
		
		{ Attribute a = new LongStringAttribute(TagFromName.SeriesDescription); a.addValue(seriesDescription); list.put(a); }

		{ Attribute a = new CodeStringAttribute(TagFromName.ContentLabel); a.addValue(contentLabel.toUpperCase()); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.ContentDescription); a.addValue(contentDescription); list.put(a); }
		{ Attribute a = new PersonNameAttribute(TagFromName.ContentCreatorName); a.addValue(contentCreatorName); list.put(a); }
		
		{ Attribute a = new CodeStringAttribute(TagFromName.LossyImageCompression); a.addValue("00"); list.put(a); }
		
		{
			java.util.Date currentDateTime = new java.util.Date();
			String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime);
			String currentTime = new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime);
			{ Attribute a = new DateAttribute(TagFromName.SeriesDate); a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.SeriesTime); a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.ContentDate); a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.ContentTime); a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate); a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime); a.addValue(currentTime); list.put(a); }

			{
				java.util.TimeZone currentTz = java.util.TimeZone.getDefault();
				String currentTzInDICOMFormat = DateTimeAttribute.getTimeZone(currentTz,currentDateTime);	// use this rather than DateTimeAttribute.getCurrentTimeZone() because already have currentDateTime and currentTz
				String timezoneOffsetFromUTC = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TimezoneOffsetFromUTC);
				if (timezoneOffsetFromUTC.length() > 0) {
					if (!currentTzInDICOMFormat.equals(timezoneOffsetFromUTC)) {	// easier to compare DICOM strings than figure out offsets vs. raw offsets etc. from java.util.TimeZone
						// different timezone now than in images :(
						// need to fix up any existing dates and times :(
slf4jlogger.info("IndexedLabelMapToSegmentation(): Warning - TimezoneOffsetFromUTC from images "+timezoneOffsetFromUTC+" is different from current timezone "+currentTzInDICOMFormat+" - removing and not adding current");
						list.remove(TagFromName.TimezoneOffsetFromUTC);
					}
					// else good to go ... already in list and already correct (same for source images and our new instance)
				}
				else  {
//System.err.println("IndexedLabelMapToSegmentation(): adding TimezoneOffsetFromUTC "+currentTzInDICOMFormat);
					{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC); a.addValue(currentTzInDICOMFormat); list.put(a); }
				}
			}
		}
		
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }

		list.removePrivateAttributes();
        list.removeGroupLengthAttributes();
        list.removeMetaInformationHeaderAttributes();
        list.remove(TagFromName.DataSetTrailingPadding);
        FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
        list.write(outputFilename,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}

	/**
	 * <p>Read a DICOM image containing pixel values that are indices into a label map and the corresponding map and convert to a DICOM Segmentation object.</p>
	 *
	 * @param	arg	four or more parameters, the inputFile, the CSV file containing a list of labels and their coded values, the outputFile, the reference image file, and optionally, the series number, series description, content label, content description and content creator
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 4) {
				new IndexedLabelMapToSegmentation(arg[0],arg[1],arg[2],arg[3],
					arg.length >= 5 ? arg[4] : "5634",
					arg.length >= 6 ? arg[5] : "Converted from Indexed Label Map",
					arg.length >= 7 ? arg[6] : "SEGMENTATION",
					arg.length >= 8 ? arg[7] : "Converted from Indexed Label Map",
					arg.length >= 9 ? arg[8] : "PixelMed^Tester"
				);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: IndexedLabelMapToSegmentation inputFile labelFile outputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}

}


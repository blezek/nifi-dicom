/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.geometry.GeometryOfSlice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to describe a set of frames sharing common characteristics suitable for display or analysis as an entity.</p>
 *
 * <p>There is no constructor or factory method, since one or more {@link com.pixelmed.dicom.FrameSet FrameSet}s is created by using {@link com.pixelmed.dicom.SetOfFrameSets SetOfFrameSets}.</p>
 *
 * <p> The list of "distinguishing" attributes that are used to determine commonality is currently fixed,
 * and includes the unique identifying attributes at the Patient, Study, Equipment levels, the Modality and SOP Class, and ImageType
 * as well as the characteristics of the Pixel Data, and those attributes that for cross-sectional
 * images imply consistent sampling, such as ImageOrientationPatient, PixelSpacing and SliceThickness,
 * and in addition AcquisitionContextSequence and BurnedInAnnotation.</p>
 *
 * <p>Note that Series identification, specifically SeriesInstanceUID is NOT a distinguishing attribute; i.e.,
 * {@link com.pixelmed.dicom.FrameSet FrameSet}s may span Series.</p>
 *
 * @author	dclunie
 */
public class FrameSet {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/FrameSet.java,v 1.26 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FrameSet.class);
	
	private AttributeList distinguishingAttributes;
	private Map<String,AttributeList> perFrameAttributesIndexedBySOPInstanceUID;
	private Set<AttributeTag> perFrameAttributesPresentInAnyFrame;
	private AttributeList sharedAttributes;
	private Map<AttributeTag,Integer> sharedAttributesFrameCount;
	private Set<AttributeTag> alreadyRemovedFromSharedAttributesBecausePreviouslyFoundToBeUnequal;
	private List<String> sopInstanceUIDsSortedByFrameOrder;
	private int numberOfFrames;
	private boolean partitioned;
	
	private static Set<AttributeTag> distinguishingAttributeTags = new HashSet<AttributeTag>();
	{
		distinguishingAttributeTags.add(TagFromName.PatientID);
		distinguishingAttributeTags.add(TagFromName.PatientName);

		distinguishingAttributeTags.add(TagFromName.StudyInstanceUID);

		distinguishingAttributeTags.add(TagFromName.FrameOfReferenceUID);

		distinguishingAttributeTags.add(TagFromName.Manufacturer);
		distinguishingAttributeTags.add(TagFromName.InstitutionName);
		distinguishingAttributeTags.add(TagFromName.InstitutionAddress);
		distinguishingAttributeTags.add(TagFromName.StationName);
		distinguishingAttributeTags.add(TagFromName.InstitutionalDepartmentName);
		distinguishingAttributeTags.add(TagFromName.ManufacturerModelName);
		distinguishingAttributeTags.add(TagFromName.DeviceSerialNumber);
		distinguishingAttributeTags.add(TagFromName.SoftwareVersions);
		distinguishingAttributeTags.add(TagFromName.GantryID);
		distinguishingAttributeTags.add(TagFromName.PixelPaddingValue);		// sad but true :(

		distinguishingAttributeTags.add(TagFromName.Modality);

		distinguishingAttributeTags.add(TagFromName.ImageType);
		distinguishingAttributeTags.add(TagFromName.BurnedInAnnotation);
		distinguishingAttributeTags.add(TagFromName.SOPClassUID);

		distinguishingAttributeTags.add(TagFromName.Rows);
		distinguishingAttributeTags.add(TagFromName.Columns);
		distinguishingAttributeTags.add(TagFromName.BitsStored);
		distinguishingAttributeTags.add(TagFromName.BitsAllocated);
		distinguishingAttributeTags.add(TagFromName.HighBit);
		distinguishingAttributeTags.add(TagFromName.PixelRepresentation);
		distinguishingAttributeTags.add(TagFromName.PhotometricInterpretation);
		distinguishingAttributeTags.add(TagFromName.PlanarConfiguration);
		distinguishingAttributeTags.add(TagFromName.SamplesPerPixel);
		
		//distinguishingAttributeTags.add(TagFromName.BodyPartExamined);
		
		distinguishingAttributeTags.add(TagFromName.ProtocolName);						// For MR, do not want to merge images with different protocols
		
		distinguishingAttributeTags.add(TagFromName.ImageOrientationPatient);
		distinguishingAttributeTags.add(TagFromName.PixelSpacing);
		distinguishingAttributeTags.add(TagFromName.SliceThickness);
		
		//distinguishingAttributeTags.add(TagFromName.SeriesNumber);
		
		distinguishingAttributeTags.add(TagFromName.AcquisitionContextSequence);		// unlikely to be encountered, but do not want to have to handle per-frame if present
		
		// do NOT use ContributingEquipmentSequence here, because may include timestamps that vary per instance
	}
	
	private static Set<AttributeTag> excludeFromGeneralPerFrameProcessingTags = new HashSet<AttributeTag>();
	{
		excludeFromGeneralPerFrameProcessingTags.addAll(distinguishingAttributeTags);
		excludeFromGeneralPerFrameProcessingTags.add(TagFromName.AcquisitionDateTime);
		excludeFromGeneralPerFrameProcessingTags.add(TagFromName.AcquisitionDate);
		excludeFromGeneralPerFrameProcessingTags.add(TagFromName.AcquisitionTime);
	}

	private static java.text.NumberFormat scientificFormatter = new java.text.DecimalFormat("0.###E0");		// want 3 digit precision only to allow floating point jitter, e.g., in ImageOrientationPatient
	
	private String getDelimitedStringValuesAllowingForFloatingPointJitter(Attribute a) {
		String s = "";
		if (a == null || a.getVM() == 0) {
		}
		else if (a instanceof DecimalStringAttribute || a instanceof FloatSingleAttribute || a instanceof FloatDoubleAttribute) {
			StringBuffer buf =  new StringBuffer();
			String prefix = "";
			try {
				double[] vs = a.getDoubleValues();
				for (double v : vs) {
					buf.append(prefix);
					buf.append(scientificFormatter.format(v));
					prefix = "\\";
				}
			}
			catch (DicomException e) {		// folow same pattern as Attribute.getDelimitedStringValuesOrEmptyString() and ignore exceptions
			}
			s = buf.toString();
		}
		else {
			s = a.getDelimitedStringValuesOrEmptyString();
		}
		return s;
	}
	
	private String getDelimitedStringValuesAllowingForFloatingPointJitter(AttributeList list,AttributeTag tag) {
		return getDelimitedStringValuesAllowingForFloatingPointJitter(list.get(tag));
	}
	
	private boolean equalsAllowingForFloatingPointJitter(AttributeList list1,AttributeList list2) {
//System.err.println("FrameSet.equalsAllowingForFloatingPointJitter():");
		if (list1.size() == list2.size()) {
			Iterator<Attribute> i = list1.values().iterator();
			while (i.hasNext()) {
				Attribute a1 = i.next();
				Attribute a2 = list2.get(a1.getTag());
				// ideally would have Attribute.equals() available to us, but don't at this time :(
				String a1s = getDelimitedStringValuesAllowingForFloatingPointJitter(a1).trim();		// otherwise trailing spaces may cause mismatch, e.g., padded "DCM " versus unpadded "DCM"
				String a2s = getDelimitedStringValuesAllowingForFloatingPointJitter(a2).trim();
//System.err.println("FrameSet.equalsAllowingForFloatingPointJitter(): comparing trimmed string "+as);
//System.err.println("FrameSet.equalsAllowingForFloatingPointJitter():      with trimmed string "+oas);
				if (!a1s.equals(a2s)) {
					return false;
				}
			}
			return true;
		}
		else {
//System.err.println("FrameSet.equalsAllowingForFloatingPointJitter(): different sizes");
			return false;
		}
	}
	
	/**
	 * <p>Extract the attributes and values that are required to be common to all members of this {@link com.pixelmed.dicom.FrameSet FrameSet}s
	 * and for which different values will create distinct {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 *
	 * @param	list	a list of DICOM attributes
	 * @return			a list of the attributes with values required to be the same for membership in this {@link com.pixelmed.dicom.FrameSet FrameSet}s
	 */
	private AttributeList extractDistinguishingAttributes(AttributeList list) {
		AttributeList distinguishingAttributes = new AttributeList();
		for (AttributeTag tag : distinguishingAttributeTags) {
			Attribute a = list.get(tag);
			if (a == null) {
				try {
					a = AttributeFactory.newAttribute(tag);
				}
				catch (DicomException e) {
					slf4jlogger.error("Internal Error: Could not create Distinguishing Attribute for tag {} - ignoring it",tag,e);
				}
			}
			if (a != null) {
				distinguishingAttributes.put(a);
			}
		}
		return distinguishingAttributes;
	}
	
	/**
	 * <p>Extract the attributes and values that are potentially different for all members of this {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 *
	 * @param	list	a list of DICOM attributes
	 * @return			a list of the attributes with values that are potentially different for each member of this {@link com.pixelmed.dicom.FrameSet FrameSet}s
	 */
	private AttributeList extractPerFrameAttributes(AttributeList list) {
		AttributeList perFrameAttributes = new AttributeList();
		
		for (AttributeTag tag : list.keySet()) {
			if (! tag.isPrivate() && ! tag.isRepeatingGroup() && ! tag.isFileMetaInformationGroup() && ! tag.isGroupLength() && ! excludeFromGeneralPerFrameProcessingTags.contains(tag)) {
				Attribute a = list.get(tag);
				{
					perFrameAttributes.put(a);
					if (!tag.equals(TagFromName.SOPInstanceUID)) {
						addToSharedAttributesIfEqualValuesAndNotPreviouslyFoundToBeUnequal(a);
					}
					// if there was only one frame in the FrameSet, SOPInstanceUID would become shared, but need to leave SOPInstanceUID in per-frame group else sorting does not work
				}
			}
		}
		
		String useAcquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDateTime);
		if (useAcquisitionDateTime.length() == 0) {
			// Follow the pattern of com.pixelmed.dicom.DateTimeAttribute.getDateFromFormattedString(AttributeList,AttributeTag,AttributeTag)
			String dateValue = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDate);
			if (dateValue.length() > 0) {
				useAcquisitionDateTime = dateValue
									   + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionTime);		// assume hh is zero padded if less than 10, which should be true, but should check :(
									   // do NOT try to guess and add time zone ... not needed here if same for all frames
			}
		}
		try {
			Attribute aAcquisitionDateTime = new DateTimeAttribute(TagFromName.AcquisitionDateTime);
			aAcquisitionDateTime.addValue(useAcquisitionDateTime);
			perFrameAttributes.put(aAcquisitionDateTime);				// even if empty, still add it
			addToSharedAttributesIfEqualValuesAndNotPreviouslyFoundToBeUnequal(aAcquisitionDateTime);
		}
		catch (DicomException e) {
			slf4jlogger.error("Could not create AcquisitionDateTime - not added",e);
		}

		return perFrameAttributes;
	}
	
	private void addToSharedAttributesIfEqualValuesAndNotPreviouslyFoundToBeUnequal(Attribute a) {
		AttributeTag tag = a.getTag();
		Attribute sharedAttribute = sharedAttributes.get(tag);
		if (sharedAttribute == null) {
			if (!alreadyRemovedFromSharedAttributesBecausePreviouslyFoundToBeUnequal.contains(tag)) {
				// may be first frame, which is OK, or may not have been in previous frames, which will be detected and remove later when checking frame counts
				sharedAttributes.put(a);
				sharedAttributesFrameCount.put(tag,new Integer(1));
			}
			// else ignore it, regardless of value
		}
		else {
			String sharedValue = getDelimitedStringValuesAllowingForFloatingPointJitter(sharedAttribute);
			String value = getDelimitedStringValuesAllowingForFloatingPointJitter(a);
			if (sharedValue.equals(value)) {
				sharedAttributesFrameCount.put(tag,new Integer(sharedAttributesFrameCount.get(tag).intValue()+1));	// need to check later that was present for every frame
			}
			else {
				sharedAttributes.remove(tag);
				alreadyRemovedFromSharedAttributesBecausePreviouslyFoundToBeUnequal.add(tag);
			}
		}
	}
	
	private void removeSharedAttributesThatAreNotInEveryFrame() {
		// standard pattern using Iterator.remove() to avoid ConcurrentModificationException
		Iterator<Map.Entry<AttributeTag,Attribute>> it = sharedAttributes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<AttributeTag,Attribute> entry = it.next();
			AttributeTag tag = entry.getKey();
			int count = sharedAttributesFrameCount.get(tag).intValue();
			if (count < numberOfFrames) {
//System.err.println("FrameSet.removeSharedAttributesThatAreNotInEveryFrame(): removing "+tag+" since only present in "+count+" frames");
				it.remove();
			}
		}
	}
	
	private void removeSharedAttributesFromPerFrameAttributes() {
		for (AttributeTag tag : sharedAttributes.keySet()) {
			for (AttributeList perFrameAttributes : perFrameAttributesIndexedBySOPInstanceUID.values()) {
				perFrameAttributes.remove(tag);
			}
		}
	}
	
	private void extractPerFrameAttributesPresentInAnyFrame() {
		perFrameAttributesPresentInAnyFrame = new TreeSet<AttributeTag>();												// want to keep sorted for output as toString()
		for (AttributeList perFrameAttributes : perFrameAttributesIndexedBySOPInstanceUID.values()) {					// traversal order doesn't matter
			perFrameAttributesPresentInAnyFrame.addAll(getAttributeTagsInAttributeListWithValues(perFrameAttributes));	// only add them IF THEY HAVE VALUES to match distinguished and shared behavior
		}
	}
	
	private class FrameSortKey implements Comparable {
	
		String sopInstanceUID;
		int seriesNumber;
		int instanceNumber;
	
		public int compareTo(Object o) {
			String oSOPInstanceUID = ((FrameSortKey)o).sopInstanceUID;
			int oInstanceNumber = ((FrameSortKey)o).instanceNumber;
			int oSeriesNumber = ((FrameSortKey)o).seriesNumber;

			return seriesNumber == oSeriesNumber
				? (
					instanceNumber == oInstanceNumber
					? (sopInstanceUID.equals(oSOPInstanceUID) ? 0 : (sopInstanceUID.hashCode() < oSOPInstanceUID.hashCode() ? -1 : 1))
					: (instanceNumber < oInstanceNumber ? -1 : 1)
				  )
				: (seriesNumber < oSeriesNumber ? -1 : 1)
				;
		}

		public boolean equals(Object o) {
			return sopInstanceUID.equals(((FrameSortKey)o).sopInstanceUID);
		}

		public int hashCode() {
			return sopInstanceUID.hashCode();
		}
		
		FrameSortKey(AttributeList list) {
			sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
			
			seriesNumber = -1;
			String seriesNumberAsString = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber);
			if (seriesNumberAsString.length() > 0) {
				try {
					seriesNumber = Integer.parseInt(seriesNumberAsString);
				}
				catch (NumberFormatException e) {
					slf4jlogger.error("Could not parse SeriesNumber as integer {}",seriesNumberAsString,e);
				}
			}
			
			instanceNumber = -1;
			String instanceNumberAsString = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstanceNumber);
			if (instanceNumberAsString.length() > 0) {
				try {
					instanceNumber = Integer.parseInt(instanceNumberAsString);
				}
				catch (NumberFormatException e) {
					slf4jlogger.error("Could not parse InstanceNumber as integer {}",instanceNumberAsString,e);
				}
			}
		}
	}
	
	private void extractFrameSortOrderFromPerFrameAttributes() {
		SortedSet<FrameSortKey> frameSortOrder = new TreeSet<FrameSortKey>();
		for (AttributeList perFrameAttributes : perFrameAttributesIndexedBySOPInstanceUID.values()) {
			frameSortOrder.add(new FrameSortKey(perFrameAttributes));
		}

		sopInstanceUIDsSortedByFrameOrder = new ArrayList(frameSortOrder.size());
		for (FrameSortKey frame : frameSortOrder) {
			sopInstanceUIDsSortedByFrameOrder.add(frame.sopInstanceUID);
		}
	}
	
	/**
	 * <p>Partition the {@link com.pixelmed.dicom.FrameSet FrameSet}s into shared and per-frame attributes, if not already done.</p>
	 *
	 * <p>Automatically called when accessor or toString() methods are invoked.</p>
	 *
	 */
	private void partitionPerFrameIntoSharedAttributes() {
		if (!partitioned) {
			removeSharedAttributesThatAreNotInEveryFrame();
			removeSharedAttributesFromPerFrameAttributes();
			extractPerFrameAttributesPresentInAnyFrame();
			extractFrameSortOrderFromPerFrameAttributes();
			partitioned = true;
		}
	}
		
	/**
	 * <p>Check to see if a single frame object is a potential member of the current {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 *
	 * @param	list	a list of DICOM attributes for the object to be checked
	 * @return			true if the attribute list matches the criteria for membership in this {@link com.pixelmed.dicom.FrameSet FrameSet}s
	 */
	boolean eligible(AttributeList list) {
		AttributeList tryList = extractDistinguishingAttributes(list);
		boolean isEligible = equalsAllowingForFloatingPointJitter(distinguishingAttributes,tryList);
//System.err.println("FrameSet.eligible(): "+isEligible);
		return isEligible;
	}
	
	/**
	 * <p>Insert the single frame object into the current {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 *
	 * <p>It is assumed that the object has already been determined to be eligible.</p>
	 *
	 * @param		list			a list of DICOM attributes for the object to be inserted
	 * @throws	DicomException	if no SOP Instance UID
	 */
	void insert(AttributeList list) throws DicomException {
		++numberOfFrames;
		String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
		if (sopInstanceUID.length() > 0) {
			AttributeList perFrameAttributesForThisInstance = extractPerFrameAttributes(list);
			perFrameAttributesIndexedBySOPInstanceUID.put(sopInstanceUID,perFrameAttributesForThisInstance);
		}
		else {
			throw new DicomException("Missing SOP Instance UID");
		}
		partitioned = false;
	}
	
	/**
	 * <p>Create a new {@link com.pixelmed.dicom.FrameSet FrameSet} using the single frame object.</p>
	 *
	 * @param		list			a list of DICOM attributes for the object from which the {@link com.pixelmed.dicom.FrameSet FrameSet} is to be created
	 * @throws	DicomException	if no SOP Instance UID
	 */
	FrameSet(AttributeList list) throws DicomException {
		distinguishingAttributes = extractDistinguishingAttributes(list);
		perFrameAttributesIndexedBySOPInstanceUID = new TreeMap<String,AttributeList>();
		perFrameAttributesPresentInAnyFrame = null;
		sharedAttributes = new AttributeList();				// want to keep sorted for output as toString()
		sharedAttributesFrameCount = new HashMap<AttributeTag,Integer>();	// count is used to (later) clean up tags that are not in every frame
		alreadyRemovedFromSharedAttributesBecausePreviouslyFoundToBeUnequal = new HashSet<AttributeTag>();
		sopInstanceUIDsSortedByFrameOrder = null;
		numberOfFrames = 0;
		insert(list);
	}
	
	/**
	 * <p>Get a sorted list of the frames.</p>
	 *
	 * @return	a sorted list of SOP Instance UIDs
	 */
	public List<String> getSOPInstanceUIDsSortedByFrameOrder() {
		partitionPerFrameIntoSharedAttributes();	// includes performing the sorting step
		return sopInstanceUIDsSortedByFrameOrder;
	}
	
	/**
	 * <p>Given a list of DICOM attributes, return only those with values or one or more sequence items.</p>
	 *
	 * @param	list	a list of DICOM attributes
	 * @return			a new {@link java.util.Set Set} of {@link com.pixelmed.dicom.AttributeTag AttributeTag}s
	 */
	static public Set<AttributeTag> getAttributeTagsInAttributeListWithValues(AttributeList list) {
		Set<AttributeTag> tagsWithValues = new TreeSet<AttributeTag>();
		for (Attribute a : list.values()) {
			if (a.getVM() > 0 || (a instanceof SequenceAttribute && ((SequenceAttribute)a).getNumberOfItems() > 0)) {
				tagsWithValues.add(a.getTag());
			}
		}
		return tagsWithValues;
	}
	
	/**
	 * <p>Get the distinguishing AttributeTags used in this {@link com.pixelmed.dicom.FrameSet FrameSet} that are present with values.</p>
	 *
	 * @return	a set of distinguishing AttributeTags
	 */
	public Set<AttributeTag> getDistinguishingAttributeTags() {
		partitionPerFrameIntoSharedAttributes();
		return getAttributeTagsInAttributeListWithValues(distinguishingAttributes);
	}
	
	/**
	 * <p>Get the shared AttributeTags used in this {@link com.pixelmed.dicom.FrameSet FrameSet} that are present with values.</p>
	 *
	 * @return	a set of shared AttributeTags
	 */
	public Set<AttributeTag> getSharedAttributeTags() {
		partitionPerFrameIntoSharedAttributes();
		return getAttributeTagsInAttributeListWithValues(sharedAttributes);
	}
	
	/**
	 * <p>Get the per-frame varying AttributeTags used in this {@link com.pixelmed.dicom.FrameSet FrameSet} that are present with values.</p>
	 *
	 * <p>This is the set used in any frame (not necessarily all frames).</p>
	 *
	 * @return	a set of per-frame varying AttributeTags
	 */
	public Set<AttributeTag> getPerFrameAttributeTags() {
		partitionPerFrameIntoSharedAttributes();
		return perFrameAttributesPresentInAnyFrame;
	}
	
	/**
	 * <p>Get the number of frames in this {@link com.pixelmed.dicom.FrameSet FrameSet}.</p>
	 *
	 * @return	the number of frames in this {@link com.pixelmed.dicom.FrameSet FrameSet}
	 */
	public int size() {
		partitionPerFrameIntoSharedAttributes();
		return sopInstanceUIDsSortedByFrameOrder == null ? 0 : sopInstanceUIDsSortedByFrameOrder.size();
	}

	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		partitionPerFrameIntoSharedAttributes();
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("\tNumber of frames: ");
		strbuf.append(numberOfFrames);
		strbuf.append("\n");
		if (distinguishingAttributes != null) {
			strbuf.append("\tDistinguishing:\n");
			for (AttributeTag tag : distinguishingAttributes.keySet()) {
				strbuf.append("\t\t");
				strbuf.append(distinguishingAttributes.get(tag).toString(AttributeList.getDictionary()));
				strbuf.append("\n");
			}
		}
		strbuf.append("\tShared:\n");
		if (sharedAttributes != null) {
			for (AttributeTag tag : sharedAttributes.keySet()) {
				strbuf.append("\t\t\t");
				strbuf.append(sharedAttributes.get(tag).toString(AttributeList.getDictionary()));
				strbuf.append("\n");
			}
		}
		
		strbuf.append("\tPer-Frame:\n");
		if (perFrameAttributesPresentInAnyFrame != null) {
			for (AttributeTag tag : perFrameAttributesPresentInAnyFrame) {
				strbuf.append("\t\t");
				strbuf.append(tag);
				strbuf.append(" ");
				strbuf.append(AttributeList.getDictionary().getNameFromTag(tag));
				strbuf.append("\n");
			}
		}
		
		if (perFrameAttributesIndexedBySOPInstanceUID != null) {
			int j = 0;
			for (String sopInstanceUID : sopInstanceUIDsSortedByFrameOrder) {
			//for (AttributeList map : perFrameAttributesIndexedBySOPInstanceUID.values()) {
//System.err.println("FrameSet.toString(): sopInstanceUID = "+sopInstanceUID);
				if (sopInstanceUID != null) {
					AttributeList perFrameAttributes = perFrameAttributesIndexedBySOPInstanceUID.get(sopInstanceUID);
					strbuf.append("\tFrame [");
					strbuf.append(Integer.toString(j));
					strbuf.append("]:\n");
					if (perFrameAttributes != null) {
						for (AttributeTag tag : perFrameAttributes.keySet()) {
							strbuf.append("\t\t\t");
							strbuf.append(perFrameAttributes.get(tag).toString(AttributeList.getDictionary()));
							strbuf.append("\n");
						}
					}
					++j;
				}
			}
		}
		if (sopInstanceUIDsSortedByFrameOrder != null) {
			strbuf.append("\tFrame order:\n");
			int j = 0;
			for (String sopInstanceUID : sopInstanceUIDsSortedByFrameOrder) {
				strbuf.append("\t\tFrame [");
				strbuf.append(Integer.toString(j));
				strbuf.append("]: ");
				strbuf.append(sopInstanceUID);
				strbuf.append("\n");
				++j;
			}
		}
		return strbuf.toString();
	}
}


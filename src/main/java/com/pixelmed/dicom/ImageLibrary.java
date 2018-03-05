/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class ImageLibrary {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ImageLibrary.java,v 1.14 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageLibrary.class);
	
	public static class ImageLibraryEntry {
	
		protected HierarchicalImageReference hierarchicalImageReference;
		
		protected ImageLibraryEntry() {
		}
		
		public ImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {					
			String sopInstanceUID = imageContentItem.getReferencedSOPInstanceUID();
			String sopClassUID = imageContentItem.getReferencedSOPClassUID();
			HierarchicalSOPInstanceReference hierarchicalSOPInstanceReference = hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID.get(sopInstanceUID);
			HierarchicalImageReference hierarchicalImageReference = null;
			if (hierarchicalSOPInstanceReference == null) {
				// this should never happen, but just in case ...
				hierarchicalImageReference = new HierarchicalImageReference(null/*studyInstanceUID*/,null/*seriesInstanceUID*/,sopInstanceUID,sopClassUID);
			}
			else {
				hierarchicalImageReference = new HierarchicalImageReference(hierarchicalSOPInstanceReference);
			}
			constructImageLibraryEntry(hierarchicalImageReference);
		}

		public ImageLibraryEntry(AttributeList list) {
			String     studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
			String    seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
			String       sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
			String          sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);

			if (studyInstanceUID.length() > 0
			 && seriesInstanceUID.length() > 0
			 && sopInstanceUID.length() > 0
			 && sopClassUID.length() > 0
			) {
				HierarchicalImageReference hierarchicalImageReference = new HierarchicalImageReference(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID);
				constructImageLibraryEntry(hierarchicalImageReference);
			}
		}
		
		public ImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference) {
			constructImageLibraryEntry(hierarchicalImageReference);
		}
		
		protected void constructImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference) {
			this.hierarchicalImageReference = hierarchicalImageReference;
		}

		/**
		 * <p>Get a new ImageContentItem made from this ImageLibraryEntry to use in an SR tree.</p>
		 *
		 * <p>Has side effect of adding the fragment to the parent if the parent is not null.</p>
		 *
		 * @param cif				the ContentItemFactory to use
		 * @param parent			the parent content item (usually the Image Library CONTAINER content item)
		 * @return					the new ImageContentItem
		 * @throws DicomException	if error in DICOM encoding
		 */
		public ContentItem getImageContentItem(ContentItemFactory cif,ContentItem parent) throws DicomException {
			ContentItem image = cif.new ImageContentItem(parent,"CONTAINS",null/*conceptName*/,
				hierarchicalImageReference.getSOPClassUID(),hierarchicalImageReference.getSOPInstanceUID(),
				0/*referencedFrameNumber*/,0/*referencedSegmentNumber*/,
				null/*presentationStateSOPClassUID*/,null/*presentationStateSOPInstanceUID*/,
				null/*realWorldValueMappingSOPClassUID*/,null/*realWorldValueMappingSOPInstanceUID*/);
			return image;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("Entry:\n");
			
			buf.append(hierarchicalImageReference);
			buf.append("\n");
			
			return buf.toString();
		}
		
	}

	public ImageLibraryEntry makeImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {
//System.err.println("ImageLibraryEntry.makeImageLibraryEntry(ContentItemFactory.ImageContentItem,Map<String,HierarchicalSOPInstanceReference>)");
		return new ImageLibraryEntry(imageContentItem,hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID);
	}
	
	public ImageLibraryEntry makeImageLibraryEntry(AttributeList list) throws DicomException {
//System.err.println("ImageLibraryEntry.makeImageLibraryEntry(AttributeList)");
		return new ImageLibraryEntry(list);
	}
		
	
	protected Map<String,ImageLibraryEntry> entriesIndexedBySOPInstanceUID = new HashMap<String,ImageLibraryEntry>();
	
	protected CompositeInstanceContext compositeInstanceContext;
		
	//public ImageLibraryEntry getImageLibraryEntry(String sopInstanceUID) {
	//	return entriesIndexedBySOPInstanceUID.get(sopInstanceUID);
	//}
		
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (ImageLibraryEntry entry : entriesIndexedBySOPInstanceUID.values()) {
			buf.append(entry);
		}
		return buf.toString();
	}
	
	public SequenceAttribute getCommonInstanceReferenceModuleReferencedSeriesSequence(String studyInstanceUID) throws DicomException {
		// copied from guts of getHierarchicalEvidenceSequence() - should refactor :(
		Set<String> seriesInStudy = new HashSet<String>();
		Map<String,Set<HierarchicalSOPInstanceReference>> mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference = new HashMap<String,Set<HierarchicalSOPInstanceReference>>();
		for (ImageLibraryEntry entry : entriesIndexedBySOPInstanceUID.values()) {
			HierarchicalSOPInstanceReference instanceReference = entry.hierarchicalImageReference;
			String referencedStudyInstanceUID  = instanceReference.getStudyInstanceUID();
			if (studyInstanceUID.equals(referencedStudyInstanceUID)) {
				String referencedSOPInstanceUID    = instanceReference.getSOPInstanceUID();
				String referencedSOPClassUID       = instanceReference.getSOPClassUID();
				String referencedSeriesInstanceUID = instanceReference.getSeriesInstanceUID();
				if (referencedSOPInstanceUID    != null && referencedSOPInstanceUID.length() > 0
				 && referencedSOPClassUID       != null && referencedSOPClassUID.length() > 0
				 && referencedSeriesInstanceUID != null && referencedSeriesInstanceUID.length() > 0
				 && referencedStudyInstanceUID  != null && referencedStudyInstanceUID.length() > 0) {
												
					seriesInStudy.add(referencedSeriesInstanceUID);
						
					Set<HierarchicalSOPInstanceReference> instanceReferencesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
					if (instanceReferencesInSeries == null) {
						instanceReferencesInSeries = new HashSet<HierarchicalSOPInstanceReference>();
						mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.put(referencedSeriesInstanceUID,instanceReferencesInSeries);
					}
					instanceReferencesInSeries.add(instanceReference);
				}
				else {
					System.err.println("Cannot find hierarchical information for reference to SOP Instance UID "+referencedSOPInstanceUID);
				}
			}
		}
		SequenceAttribute aReferencedSeriesSequence = new SequenceAttribute(TagFromName.ReferencedSeriesSequence);
		for (String referencedSeriesInstanceUID : seriesInStudy) {
//System.err.println("\tSERIES "+referencedSeriesInstanceUID);
			AttributeList referencedSeriesList = new AttributeList();
			aReferencedSeriesSequence.addItem(referencedSeriesList);
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(referencedSeriesInstanceUID); referencedSeriesList.put(a); }
			SequenceAttribute aReferencedInstanceSequence = new SequenceAttribute(TagFromName.ReferencedInstanceSequence);	// NB. use ReferencedInstanceSequence in CommonInstanceReferenceModule, NOT ReferenceSOPSequence, which is used in SR HierarchicalSOPInstanceReferenceMacro in CurrentRequestedProcedureEvidenceSequence
			referencedSeriesList.put(aReferencedInstanceSequence);

			Set<HierarchicalSOPInstanceReference> instancesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
			for (HierarchicalSOPInstanceReference instanceReference : instancesInSeries) {
				AttributeList referencedInstanceList = new AttributeList();
				aReferencedInstanceSequence.addItem(referencedInstanceList);

				String referencedSOPInstanceUID = instanceReference.getSOPInstanceUID();
//System.err.println("\t\tINSTANCE SOP Instance "+referencedSOPInstanceUID);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referencedSOPInstanceUID); referencedInstanceList.put(a); }
				String referencedSOPClassUID    = instanceReference.getSOPClassUID();
//System.err.println("\t\tINSTANCE SOP Class "+referencedSOPClassUID);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referencedSOPClassUID); referencedInstanceList.put(a); }
			}
		}
		return aReferencedSeriesSequence;
	}
	
	/**
	 * <p>Build a hierarchical evidence sequence for the current Image Library content.</p>
	 *
	 * <p>This contains the Study and Series Instance UIDs and is required for most SR IODs.</p>
	 *
	 * @return					a CurrentRequestedProcedureEvidenceSequence listing all the referenced instances
	 * @throws DicomException	if error in DICOM encoding
	 */
	public SequenceAttribute getHierarchicalEvidenceSequence() throws DicomException {
		// the following are modified from com.pixelmed.apps.AddHierarchicalEvidenceSequenceToStructuredReports; should refactor :(
		Map<String,Set<String>> mapOfStudyInstanceUIDToSetOfSeriesInstanceUID = new HashMap<String,Set<String>>();
		Map<String,Set<HierarchicalSOPInstanceReference>> mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference = new HashMap<String,Set<HierarchicalSOPInstanceReference>>();
		for (ImageLibraryEntry entry : entriesIndexedBySOPInstanceUID.values()) {
			HierarchicalSOPInstanceReference instanceReference = entry.hierarchicalImageReference;
			String referencedSOPInstanceUID    = instanceReference.getSOPInstanceUID();
			String referencedSOPClassUID       = instanceReference.getSOPClassUID();
			String referencedSeriesInstanceUID = instanceReference.getSeriesInstanceUID();
			String referencedStudyInstanceUID  = instanceReference.getStudyInstanceUID();
						
			if (referencedSOPInstanceUID    != null && referencedSOPInstanceUID.length() > 0
			 && referencedSOPClassUID       != null && referencedSOPClassUID.length() > 0
			 && referencedSeriesInstanceUID != null && referencedSeriesInstanceUID.length() > 0
			 && referencedStudyInstanceUID  != null && referencedStudyInstanceUID.length() > 0) {
												
				Set<String> seriesInStudy = mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.get(referencedStudyInstanceUID);
				if (seriesInStudy == null) {
					seriesInStudy = new HashSet<String>();
					mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.put(referencedStudyInstanceUID,seriesInStudy);
				}
				seriesInStudy.add(referencedSeriesInstanceUID);
						
				Set<HierarchicalSOPInstanceReference> instanceReferencesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
				if (instanceReferencesInSeries == null) {
					instanceReferencesInSeries = new HashSet<HierarchicalSOPInstanceReference>();
					mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.put(referencedSeriesInstanceUID,instanceReferencesInSeries);
				}
				instanceReferencesInSeries.add(instanceReference);
			}
			else {
				System.err.println("Cannot find hierarchical information for reference to SOP Instance UID "+referencedSOPInstanceUID);
			}
		}
		SequenceAttribute aCurrentRequestedProcedureEvidenceSequence = new SequenceAttribute(TagFromName.CurrentRequestedProcedureEvidenceSequence);
		for (String referencedStudyInstanceUID : mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.keySet()) {
//System.err.println("STUDY "+referencedStudyInstanceUID);
			AttributeList referencedStudyList = new AttributeList();
			aCurrentRequestedProcedureEvidenceSequence.addItem(referencedStudyList);
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(referencedStudyInstanceUID); referencedStudyList.put(a); }
			SequenceAttribute aReferencedSeriesSequence = new SequenceAttribute(TagFromName.ReferencedSeriesSequence);
			referencedStudyList.put(aReferencedSeriesSequence);

			Set<String> seriesInStudy = mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.get(referencedStudyInstanceUID);
			for (String referencedSeriesInstanceUID : seriesInStudy) {
//System.err.println("\tSERIES "+referencedSeriesInstanceUID);
				AttributeList referencedSeriesList = new AttributeList();
				aReferencedSeriesSequence.addItem(referencedSeriesList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(referencedSeriesInstanceUID); referencedSeriesList.put(a); }
				SequenceAttribute aReferencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);	// NOT ReferencedInstanceSequence !
				referencedSeriesList.put(aReferencedSOPSequence);

				Set<HierarchicalSOPInstanceReference> instancesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
				for (HierarchicalSOPInstanceReference instanceReference : instancesInSeries) {
					AttributeList referencedSOPList = new AttributeList();
					aReferencedSOPSequence.addItem(referencedSOPList);

					String referencedSOPInstanceUID = instanceReference.getSOPInstanceUID();
//System.err.println("\t\tINSTANCE SOP Instance "+referencedSOPInstanceUID);
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referencedSOPInstanceUID); referencedSOPList.put(a); }
					String referencedSOPClassUID    = instanceReference.getSOPClassUID();
//System.err.println("\t\tINSTANCE SOP Class "+referencedSOPClassUID);
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referencedSOPClassUID); referencedSOPList.put(a); }
				}
			}
		}
		return aCurrentRequestedProcedureEvidenceSequence;
	}
	
	/**
	 * <p>Find the Image Library contained within an SR content tree.</p>
	 *
	 * <p>The Image Library is identified by the ("111028","DCM","Image Library") concept name for the content item.</p>
	 *
	 * @param	node	the content item to being the search at (e.g., the root node)
	 * @return			the content item that is the Image Library CONTAINER
	 */
	public static ContentItem findImageLibraryContainer(ContentItem node) {
		ContentItem found = null;
		if (node != null) {
			if (node.contentItemNameMatchesCodeValueAndCodingSchemeDesignator("111028","DCM")) {	// "Image Library"
				found = node;
			}
			else {
				int n = node.getChildCount();
				for (int i=0; found == null && i<n; ++i) {
					ContentItem child = ((ContentItem)node.getChildAt(i));
					found = findImageLibraryContainer(child);
				}
			}
		}
		return found;
	}
	
	/**
	 * <p>Construct an empty Image Library.</p>
	 */
	public ImageLibrary() {
	}
	
	/**
	 * <p>Construct an Image Library by extracting it from an existing DICOM SR instance.</p>
	 *
	 * <p>Searches the content tree for the Image Library CONTAINER content item.</p>
	 *
	 * @param	list			the attributes comprising the DICOM SR instance containing the Image Library
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public ImageLibrary(AttributeList list) throws DicomException {
		StructuredReport sr = new StructuredReport(list);
		ContentItem imageLibraryContainer = findImageLibraryContainer((ContentItem)(sr.getRoot()));
		if (imageLibraryContainer != null) {
			constructImageLibrary(imageLibraryContainer,list);
		}
		else {
			throw new DicomException("No Image Library CONTAINER content item in SR");
		}
	}
		
	/**
	 * <p>Construct an Image Library by extracting it from an existing DICOM SR instance.</p>
	 *
	 * @param	imageLibraryContainer	the content item that is the Image Library CONTAINER
	 * @param	list	the attributes comprising the DICOM SR instance containing the Image Library
	 */
	public ImageLibrary(ContentItem imageLibraryContainer,AttributeList list) {
		constructImageLibrary(imageLibraryContainer,list);
	}
	
	protected Map<String,HierarchicalSOPInstanceReference> getHierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID(AttributeList list) {
		Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID = new HashMap<String,HierarchicalSOPInstanceReference>();
		Attribute aCurrentRequestedProcedureEvidenceSequence = list.get(TagFromName.CurrentRequestedProcedureEvidenceSequence);
		if (aCurrentRequestedProcedureEvidenceSequence != null && aCurrentRequestedProcedureEvidenceSequence instanceof SequenceAttribute) {
			Iterator<SequenceItem> iStudy = ((SequenceAttribute)aCurrentRequestedProcedureEvidenceSequence).iterator();
			while (iStudy.hasNext()) {
				AttributeList referencedStudyList = iStudy.next().getAttributeList();
				String referencedStudyInstanceUID = Attribute.getSingleStringValueOrEmptyString(referencedStudyList,TagFromName.StudyInstanceUID);
//System.err.println("STUDY "+referencedStudyInstanceUID);
				Attribute aReferencedSeriesSequence = referencedStudyList.get(TagFromName.ReferencedSeriesSequence);
				if (aReferencedSeriesSequence != null && aReferencedSeriesSequence instanceof SequenceAttribute) {
					Iterator<SequenceItem> iSeries = ((SequenceAttribute)aReferencedSeriesSequence).iterator();
					while (iSeries.hasNext()) {
						AttributeList referencedSeriesList = iSeries.next().getAttributeList();
						String referencedSeriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(referencedSeriesList,TagFromName.SeriesInstanceUID);
//System.err.println("\tSERIES "+referencedSeriesInstanceUID);
						Attribute aReferencedSOPSequence = referencedSeriesList.get(TagFromName.ReferencedSOPSequence);
						if (aReferencedSOPSequence != null && aReferencedSOPSequence instanceof SequenceAttribute) {
							Iterator<SequenceItem> iSOP = ((SequenceAttribute)aReferencedSOPSequence).iterator();
							while (iSOP.hasNext()) {
								AttributeList referencedSOPList = iSOP.next().getAttributeList();
								String referencedSOPInstanceUID = Attribute.getSingleStringValueOrEmptyString(referencedSOPList,TagFromName.ReferencedSOPInstanceUID);
//System.err.println("\t\tINSTANCE SOP Instance "+referencedSOPInstanceUID);
								String referencedSOPClassUID = Attribute.getSingleStringValueOrEmptyString(referencedSOPList,TagFromName.ReferencedSOPClassUID);
//System.err.println("\t\tINSTANCE SOP Class "+referencedSOPClassUID);
								HierarchicalSOPInstanceReference hierarchicalSOPInstanceReference = new HierarchicalSOPInstanceReference(referencedStudyInstanceUID,referencedSeriesInstanceUID,referencedSOPInstanceUID,referencedSOPClassUID);
								hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID.put(referencedSOPInstanceUID,hierarchicalSOPInstanceReference);
							}
						}
					}
				}
			}
		}
		return hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID;
	}

	/**
	 * <p>Construct an Image Library by extracting it from an existing DICOM SR instance.</p>
	 *
	 * @param	imageLibraryContainer	the content item that is the Image Library CONTAINER
	 * @param	list	the attributes comprising the DICOM SR instance containing the Image Library
	 */
	protected void constructImageLibrary(ContentItem imageLibraryContainer,AttributeList list) {
		Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID = getHierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID(list);
		if (imageLibraryContainer != null) {
			int nChildren = imageLibraryContainer.getChildCount();
			for (int i=0; i<nChildren; ++i) {
				ContentItem item = (ContentItem)(imageLibraryContainer.getChildAt(i));
				if (item != null && item instanceof ContentItemFactory.ImageContentItem) {
					ContentItemFactory.ImageContentItem imageContentItem = (ContentItemFactory.ImageContentItem)item;
					String sopInstanceUID = imageContentItem.getReferencedSOPInstanceUID();
					ImageLibraryEntry entry = makeImageLibraryEntry(imageContentItem,hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID);
					entriesIndexedBySOPInstanceUID.put(sopInstanceUID,entry);
				}
			}
		}
	}

	/**
	 * <p>Get the Image Library as an SR fragment.</p>
	 *
	 * <p>Has side effect of adding the fragment to the parent if the parent is not null.</p>
	 *
	 * @param	parent			the parent CONTAINER content item (usually the root content item)
	 * @return					a new ContainerContentItem with ImageContentItem children
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public ContentItem getStructuredReportFragment(ContentItem parent) throws DicomException {
		ContentItem contentItemFragment;
		{
			ContentItemFactory cif = new ContentItemFactory();
			contentItemFragment = cif.new ContainerContentItem(parent,(parent == null ? null : "CONTAINS"),new CodedSequenceItem("111028","DCM","Image Library"),true/*continuityOfContentIsSeparate*/);
			
			for (ImageLibraryEntry entry : entriesIndexedBySOPInstanceUID.values()) {
				/*ContentItem image = */entry.getImageContentItem(cif,contentItemFragment);
			}
		}
		return contentItemFragment;
	}

	public StructuredReport getStructuredReport() throws DicomException {
		ContentItemFactory cif = new ContentItemFactory();
		ContentItem root = getStructuredReportFragment(null);
		return new StructuredReport(root);
	}
				
	public AttributeList getAttributeList() throws DicomException {
		StructuredReport sr = getStructuredReport();
		AttributeList list = sr.getAttributeList();
		
		SequenceAttribute aHierarchicalEvidenceSequence = getHierarchicalEvidenceSequence();
		if (aHierarchicalEvidenceSequence != null) {
			list.put(aHierarchicalEvidenceSequence);					
		}
		
		if (compositeInstanceContext != null) {
			list.putAll(compositeInstanceContext.getAttributeList());					
		}
		UIDGenerator u = new UIDGenerator();
		
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getAnotherNewUID()); list.put(a); }
		{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue("3254"); list.put(a); }	// arbitrary ... should do better :(
		{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue("1"); list.put(a); }
		{ Attribute a = new LongStringAttribute(TagFromName.SeriesDescription); a.addValue("Image Library"); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.CompletionFlag); a.addValue("COMPLETE"); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.VerificationFlag); a.addValue("UNVERIFIED"); list.put(a); }

		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.EnhancedSRStorage); list.put(a); }
		{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue("SR"); list.put(a); }

		{
			java.util.Date currentDateTime = new java.util.Date();
			String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime);
			String currentTime = new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime);
			{ Attribute a = new DateAttribute(TagFromName.SeriesDate          ); a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.SeriesTime          ); a.addValue(currentTime); list.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.ContentDate         ); a.addValue(currentDate); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.ContentTime         ); a.addValue(currentTime); list.put(a); }
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
						slf4jlogger.info("getAttributeList(): Warning - TimezoneOffsetFromUTC from images {} is different from current timezone {} - removing and not adding current",timezoneOffsetFromUTC,currentTzInDICOMFormat);
						list.remove(TagFromName.TimezoneOffsetFromUTC);
					}
					// else good to go ... already in list and already correct (same for source images and our new instance)
				}
				else  {
//System.err.println("IndexedLabelMapToSegmentation(): adding TimezoneOffsetFromUTC "+currentTzInDICOMFormat);
					{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC); a.addValue(currentTzInDICOMFormat); list.put(a); }
				}
			}
			
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }
			
		}
		
		{
			Attribute a = list.get(TagFromName.ReferencedPerformedProcedureStepSequence);
			if (a == null) {
				a = new SequenceAttribute(TagFromName.ReferencedPerformedProcedureStepSequence);
			}
			list.put(a);
		}
		{
			Attribute a = list.get(TagFromName.PerformedProcedureCodeSequence);
			if (a == null) {
				a = new SequenceAttribute(TagFromName.PerformedProcedureCodeSequence);
			}
			list.put(a);
		}

		{ Attribute a = new LongStringAttribute(TagFromName.Manufacturer); a.addValue("PixelMed"); list.put(a); }

		ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
			true,																						// replace existing
			new CodedSequenceItem("250100","99PMP","Creation of Image Library SR"),						// PurposeOfReference :(
			"PixelMed",																					// Manufacturer
			null,																						// Institution Name
			null,																						// Institutional Department Name
			null,																						// Institution Address
			"OURAETITLE",																				// Station Name
			this.getClass().getCanonicalName(),															// Manufacturer's Model Name
			null,																						// Device Serial Number
			VersionAndConstants.getBuildDate(),															// Software Version(s)
			"Creation of Image Library SR"																// ContributionDescription
		);
		list.insertSuitableSpecificCharacterSetForAllStringValues();
		
		return list;
	}
		
	public void write(String filename) throws DicomException, IOException {
		AttributeList list = getAttributeList();
		list.removeMetaInformationHeaderAttributes();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
        list.write(filename);
		
		// test round trip
		//{
		//	ImageLibrary roundTrip = new ImageLibrary(list);
//System.err.println("RoundTrip =\n"+roundTrip);
		//}
	}
	
	/**
	 * <p>Extract the Image Library from a DICOM SR instance.</p>
	 *
	 * @param	filename		the DICOM SR instance containing the Image Library
	 * @return					an instance of the ImageLibrary class representing the Image Library content encoded in the DICOM SR file
	 * @throws	DicomException	if error in DICOM encoding
	 * @throws	IOException		if error reading the file
	 */
	public static ImageLibrary read(String filename) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		list.read(filename);
		return new ImageLibrary(list);
	}
						
	/**
	 * <p>Create and add an SR Image Library entry derived from a DICOM image instance.</p>
	 *
	 * <p>Requires that a SOP Instance UID be present in the supplied image instance, to be used to inxed the entries.</p>
	 *
	 * @param	list			the list of attributes of the image instance
	 * @throws	DicomException	if error in DICOM encoding
	 * @return					the new ImageLibraryEntry added to this ImageLibrary, or null if no SOP Instance UID is present in the supplied image instance
	 */
	public ImageLibraryEntry addImage(AttributeList list) throws DicomException {
		ImageLibraryEntry entry = null;
		String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
		if (sopInstanceUID.length() > 0) {
			entry = makeImageLibraryEntry(list);
			entriesIndexedBySOPInstanceUID.put(sopInstanceUID,entry);
		}
		return entry;
	}
	
	/**
	 * <p>Create an SR Image Library from a bunch of DICOM image instances.</p>
	 *
	 * <p>Adds a new series (instance UID) to the existing study (instance UID).</p>
	 *
	 * @param	files			the filenames and/or folder names of files containing the input image files
	 * @throws	DicomException	if error in DICOM encoding
	 * @throws	IOException		if error reading a file
	 */
	public ImageLibrary(Set<File> files) throws IOException, DicomException {
		for (File f : files) {
			if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
//System.err.println("ImageLibrary(): file = "+f);
				AttributeList list = new AttributeList();
				try {
					list.read(f);
					String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
					if (list.isImage()) {
						addImage(list);
						if (compositeInstanceContext == null) {
							compositeInstanceContext = new CompositeInstanceContext(list,true/*forSR*/);
							// ReferencedPerformedProcedureStepSequence will be removed from cic by removeSeries(), so put it back if present
							AttributeList cicList = compositeInstanceContext.getAttributeList();
							Attribute aReferencedPerformedProcedureStepSequence = cicList.get(TagFromName.ReferencedPerformedProcedureStepSequence);
							Attribute aTimezoneOffsetFromUTC = cicList.get(TagFromName.TimezoneOffsetFromUTC);	// this is instance level, so will otherwise be removed
							compositeInstanceContext.removeAllButPatientAndStudy();
							if (aReferencedPerformedProcedureStepSequence != null) {
								cicList.put(aReferencedPerformedProcedureStepSequence);
							}
							if (aTimezoneOffsetFromUTC != null) {
								cicList.put(aTimezoneOffsetFromUTC);
							}
						}
					}
				}
				catch (Exception e) {
					slf4jlogger.error("While reading file \"{}\"",f,e);	// do NOT call f.getCanonicalPath(), since may throw Exception !
				}
			}
		}
	}
	

	/**
	 * <p>Create an SR Image Library from a bunch of DICOM image instances.</p>
	 *
	 * <p>Adds a new series (instance UID) to the existing study (instance UID).</p>
	 *
	 * @param	arg	the path for the SR Image Library output, then the filenames and/or folder names of files containing the input image files
	 */
	public static void main(String arg[]) {
		try {
			String outputPath = arg[0];
			Set<File> inputFiles = new HashSet<File>();
			for (int i=1; i<arg.length; ++i) {
				Collection<File> more = FileUtilities.listFilesRecursively(new File(arg[i]));
				inputFiles.addAll(more);
			}
			ImageLibrary library = new ImageLibrary(inputFiles);
//System.err.println("ImageLibrary.main(): Result");
//System.err.println(library.toString());
			library.write(outputPath);
			
			// test round trip
			//{
			//	ImageLibrary roundTrip = read(outputPath);
			//	System.err.println("RoundTrip =\n"+roundTrip);
			//}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

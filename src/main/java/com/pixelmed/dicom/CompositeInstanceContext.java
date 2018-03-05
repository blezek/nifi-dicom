/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.ArrayList;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for representing the attributes of general modules that describe the patient, study, series, instance
 * and related "context" of the payload of a composite DICOM instance.</p>
 *
 * <p>The purpose is to allow the context to be extracted from an existing object, stored, and then reused in
 * new objects, either wholly or partially, by selectively removing modules for lower level information
 * entities as appropriate. E.g.:</p>
 *
 * <pre>
 *  CompositeInstanceContext cic = new CompositeInstanceContext(srcList,false);
 *  cic.removeInstance();
 *  cic.removeSeries();
 *  cic.removeEquipment();
 *  dstList.putAll(cic.getAttributeList());
 * </pre>
 *
 * <p>Static methods are also provided for operating directly on an {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
 *
 * @author	dclunie
 */
public class CompositeInstanceContext {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CompositeInstanceContext.java,v 1.17 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompositeInstanceContext.class);
	
	protected AttributeList list;
	
	/**
	 * <p>Return the {@link com.pixelmed.dicom.AttributeList AttributeList} of all the {@link com.pixelmed.dicom.Attribute Attribute}s in the context.</p>
	 *
	 * @return		the {@link com.pixelmed.dicom.AttributeList AttributeList} of all the {@link com.pixelmed.dicom.Attribute Attribute}s in the context
	 */
	public AttributeList getAttributeList() { return list; }
	
	public boolean equals(Object o) {
		if (o instanceof CompositeInstanceContext) {
			return list.equals(((CompositeInstanceContext)o).getAttributeList());
		}
		else {
			return false;
		}
	}
	
	public int hashCode() {
		return list.hashCode();
	}
	
	protected void addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(AttributeList srcList,AttributeTag tag) {
		Attribute a = srcList.get(tag);
		if (a != null) {
			if (a.getVM() > 0 || (a instanceof SequenceAttribute && ((SequenceAttribute)a).getNumberOfItems() > 0)) {
				if (list.get(tag) == null) {
					list.put(tag,a);	// make sure that an empty attribute is add if not already there
				}
				// else leave existing (possibly empty) value alone
			}
			else {
				list.put(tag,a);	// adds, or replaces existing
			}
		}
	}
	
	public CompositeInstanceContext() {
		list = new AttributeList();
	}
	
	/**
	 * <p>Create the composite context module {@link com.pixelmed.dicom.Attribute Attribute}s with values from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * @param	forSR		true if need to populate the SR Document General Module specific {@link com.pixelmed.dicom.Attribute Attribute}s from their image equivalents
	 * @param	srcList		the list of attributes to use as the source
	 */
	public CompositeInstanceContext(AttributeList srcList,boolean forSR) {
		list = new AttributeList();
		updateFromSource(srcList,forSR);
	}
	
	/**
	 * <p>Create the composite context module {@link com.pixelmed.dicom.Attribute Attribute}s with values from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * <p>Also populates the SR Document General Module specific {@link com.pixelmed.dicom.Attribute Attribute}s from their image equivalents.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.dicom.CompositeInstanceContext#CompositeInstanceContext(AttributeList,boolean) CompositeInstanceContext(AttributeList,boolean)} instead
	 *
	 * @param	srcList		the list of attributes to use as the source
	 */
	public CompositeInstanceContext(AttributeList srcList) {
		this(srcList,true/*forSR*/);
	}
	
	protected static AttributeTag[] patientModuleAttributeTags = {
		TagFromName.PatientName,
		TagFromName.PatientID,
		//Macro IssuerOfPatientIDMacro
		TagFromName.IssuerOfPatientID,
		TagFromName.IssuerOfPatientIDQualifiersSequence,
		//EndMacro IssuerOfPatientIDMacro
		TagFromName.PatientBirthDate,
		TagFromName.PatientSex,
		TagFromName.ReferencedPatientPhotoSequence,	// CP 1343
		TagFromName.QualityControlSubject,
		TagFromName.PatientBirthTime,
		TagFromName.ReferencedPatientSequence,
		TagFromName.OtherPatientIDs,
		TagFromName.OtherPatientIDsSequence,
		TagFromName.OtherPatientNames,
		TagFromName.EthnicGroup,
		TagFromName.PatientComments,
		TagFromName.PatientSpeciesDescription,
		TagFromName.PatientSpeciesCodeSequence,
		TagFromName.PatientBreedDescription,
		TagFromName.PatientBreedCodeSequence,
		TagFromName.BreedRegistrationSequence,
		TagFromName.ResponsiblePerson,
		TagFromName.ResponsiblePersonRole,
		TagFromName.ResponsibleOrganization,
		TagFromName.PatientIdentityRemoved,
		TagFromName.DeidentificationMethod,
		TagFromName.DeidentificationMethodCodeSequence
	};
	
	protected static AttributeTag[] clinicalTrialSubjectModuleAttributeTags = {
		TagFromName.ClinicalTrialSubjectID,
		TagFromName.ClinicalTrialSponsorName,
		TagFromName.ClinicalTrialProtocolID,
		TagFromName.ClinicalTrialProtocolName,
		TagFromName.ClinicalTrialSiteID,
		TagFromName.ClinicalTrialSiteName,
		TagFromName.ClinicalTrialSubjectID,
		TagFromName.ClinicalTrialSubjectReadingID
	};
	
	protected static AttributeTag[] generalStudyModuleAttributeTags = {
		TagFromName.StudyInstanceUID,
		TagFromName.StudyDate,
		TagFromName.StudyTime,
		TagFromName.ReferringPhysicianName,
		TagFromName.ReferringPhysicianIdentificationSequence,
		TagFromName.StudyID,
		TagFromName.AccessionNumber,
		TagFromName.IssuerOfAccessionNumberSequence,
		TagFromName.StudyDescription,
		TagFromName.PhysiciansOfRecord,
		TagFromName.PhysiciansOfRecordIdentificationSequence,
		TagFromName.NameOfPhysiciansReadingStudy,
		TagFromName.PhysiciansReadingStudyIdentificationSequence,
		TagFromName.RequestingServiceCodeSequence,
		TagFromName.ReferencedStudySequence,
		TagFromName.ProcedureCodeSequence,
		TagFromName.ReasonForPerformedProcedureCodeSequence
	};

	protected static AttributeTag[] patientStudyModuleAttributeTags = {
		TagFromName.AdmittingDiagnosesDescription,
		TagFromName.AdmittingDiagnosesCodeSequence,
		TagFromName.PatientAge,
		TagFromName.PatientSize,
		TagFromName.PatientWeight,
		TagFromName.PatientSizeCodeSequence,
		TagFromName.Occupation,
		TagFromName.AdditionalPatientHistory,
		TagFromName.AdmissionID,
		TagFromName.IssuerOfAdmissionID,
		TagFromName.IssuerOfAdmissionIDSequence,
		TagFromName.ServiceEpisodeID,
		TagFromName.IssuerOfServiceEpisodeIDSequence,
		TagFromName.ServiceEpisodeDescription,
		TagFromName.PatientSexNeutered
	};
	
	protected static AttributeTag[] generalSeriesModuleAttributeTags = {
		TagFromName.Modality,
		TagFromName.SeriesInstanceUID,
		TagFromName.SeriesNumber,
		TagFromName.Laterality,
		TagFromName.SeriesDate,
		TagFromName.SeriesTime,
		TagFromName.PerformingPhysicianName,
		TagFromName.PerformingPhysicianIdentificationSequence,
		TagFromName.ProtocolName,
		TagFromName.SeriesDescription,
		TagFromName.SeriesDescriptionCodeSequence,
		TagFromName.OperatorsName,
		TagFromName.OperatorIdentificationSequence,
		TagFromName.ReferencedPerformedProcedureStepSequence,
		TagFromName.RelatedSeriesSequence,
		TagFromName.BodyPartExamined,
		TagFromName.PatientPosition,
		//TagFromName.SmallestPixelValueInSeries,
		//TagFromName.LargestPixelValueInSeries,
		TagFromName.RequestAttributesSequence,
		//Macro PerformedProcedureStepSummaryMacro
		TagFromName.PerformedProcedureStepID,
		TagFromName.PerformedProcedureStepStartDate,
		TagFromName.PerformedProcedureStepStartTime,
		TagFromName.PerformedProcedureStepDescription,
		TagFromName.PerformedProtocolCodeSequence,
		TagFromName.CommentsOnThePerformedProcedureStep,
		//EndMacro PerformedProcedureStepSummaryMacro
		TagFromName.AnatomicalOrientationType
	};
	
	protected static AttributeTag[] generalEquipmentModuleAttributeTags = {
		TagFromName.Manufacturer,
		TagFromName.InstitutionName,
		TagFromName.InstitutionAddress,
		TagFromName.StationName,
		TagFromName.InstitutionalDepartmentName,
		TagFromName.ManufacturerModelName,
		TagFromName.DeviceSerialNumber,
		TagFromName.SoftwareVersions,
		TagFromName.GantryID,
		TagFromName.SpatialResolution,
		TagFromName.DateOfLastCalibration,
		TagFromName.TimeOfLastCalibration,
		TagFromName.PixelPaddingValue
	};
	
	protected static AttributeTag[] frameOfReferenceModuleAttributeTags = {
		TagFromName.FrameOfReferenceUID,
		TagFromName.PositionReferenceIndicator
	};
	
	protected static AttributeTag[] sopCommonModuleAttributeTags = {
		TagFromName.SOPClassUID,
		TagFromName.SOPInstanceUID,
		//TagFromName.SpecificCharacterSet,
		TagFromName.InstanceCreationDate,
		TagFromName.InstanceCreationTime,
		TagFromName.InstanceCreatorUID,
		TagFromName.RelatedGeneralSOPClassUID,
		TagFromName.OriginalSpecializedSOPClassUID,
		TagFromName.CodingSchemeIdentificationSequence,
		TagFromName.TimezoneOffsetFromUTC,
		TagFromName.ContributingEquipmentSequence,
		TagFromName.InstanceNumber,
		TagFromName.SOPInstanceStatus,
		TagFromName.SOPAuthorizationDateTime,
		TagFromName.SOPAuthorizationComment,
		TagFromName.AuthorizationEquipmentCertificationNumber,
		//Macro DigitalSignaturesMacro
		//TagFromName.MACParametersSequence,
		//TagFromName.DigitalSignaturesSequence,
		//EndMacro DigitalSignaturesMacro
		//TagFromName.EncryptedAttributesSequence,
		TagFromName.OriginalAttributesSequence,
		TagFromName.HL7StructuredDocumentReferenceSequence
	};
	
	protected static AttributeTag[] generalImageModuleAttributeTags = {
		TagFromName.ContentDate,
		TagFromName.ContentTime
	};
	
	protected static AttributeTag[] srDocumentGeneralModuleAttributeTags = {
		TagFromName.ReferencedRequestSequence,		// cw. RequestAttributesSequence in GeneralSeries
		TagFromName.PerformedProcedureCodeSequence	// cw. ProcedureCodeSequence in GeneralStudy
	};
	
	protected void createReferencedRequestSequenceIfAbsent(AttributeList srcList) {
		try {
			Attribute referencedRequestSequence = list.get(TagFromName.ReferencedRequestSequence);
			Attribute requestAttributesSequence = list.get(TagFromName.RequestAttributesSequence);
			if (referencedRequestSequence == null || !(referencedRequestSequence instanceof SequenceAttribute) || ((SequenceAttribute)referencedRequestSequence).getNumberOfItems() == 0) {
				if (requestAttributesSequence != null && requestAttributesSequence instanceof SequenceAttribute) {
					SequenceAttribute sRequestAttributesSequence = (SequenceAttribute)requestAttributesSequence;
					int nItems = sRequestAttributesSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sReferencedRequestSequence = new SequenceAttribute(TagFromName.ReferencedRequestSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sRequestAttributesSequence.getItem(i);
//System.err.println("CompositeInstanceContext.createReferencedRequestSequenceIfAbsent(): copying RequestAttributesSequence to ReferencedRequestSequence item "+item);
							// copy only what is relevant and required ...
							AttributeList requestAttributesSequenceItemList = item.getAttributeList();
							AttributeList referencedRequestSequenceItemList = new AttributeList();

							{
								AttributeTag tag = TagFromName.StudyInstanceUID;
								// Type 1 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new UniqueIdentifierAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.ReferencedStudySequence;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a == null) {
									a = new SequenceAttribute(TagFromName.ReferencedStudySequence);
								}
								referencedRequestSequenceItemList.put(a);
							}
							{
								AttributeTag tag = TagFromName.AccessionNumber;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new ShortStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.IssuerOfAccessionNumberSequence;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.PlacerOrderNumberImagingServiceRequest;
								// Type 2 in ReferencedRequestSequence			Not in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.OrderPlacerIdentifierSequence;
								// Type 3 in ReferencedRequestSequence			Not in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.FillerOrderNumberImagingServiceRequest;
								// Type 2 in ReferencedRequestSequence			Not in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.OrderFillerIdentifierSequence;
								// Type 3 in ReferencedRequestSequence			Not in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureID;
								// Type 2 in ReferencedRequestSequence			Type 1C in RequestAttributesSequence (if procedure was scheduled)
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new ShortStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureDescription;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								{ Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a); }
							}
							{
								AttributeTag tag = TagFromName.RequestedProcedureCodeSequence;
								// Type 2 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a == null) {
									a = new SequenceAttribute(TagFromName.ReferencedStudySequence);
								}
								referencedRequestSequenceItemList.put(a);
							}
							{
								AttributeTag tag = TagFromName.ReasonForTheRequestedProcedure;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								String s = Attribute.getSingleStringValueOrEmptyString(requestAttributesSequenceItemList,tag); 
								if (s.length() > 0) {
									Attribute a = new LongStringAttribute(tag); a.addValue(s); referencedRequestSequenceItemList.put(a);
								}
							}
							{
								AttributeTag tag = TagFromName.ReasonForRequestedProcedureCodeSequence;
								// Type 3 in ReferencedRequestSequence			Type 3 in RequestAttributesSequence
								Attribute a = requestAttributesSequenceItemList.get(tag);
								if (a != null) {
									referencedRequestSequenceItemList.put(a);
								}
							}
							
							sReferencedRequestSequence.addItem(referencedRequestSequenceItemList);
						}
						list.put(sReferencedRequestSequence);
					}
				}
			}
		}
		catch (DicomException e) {
			// trap the exception, since not a big deal if we fail
			slf4jlogger.error("Ignoring exception", e);
		}
	}

	protected void createPerformedProcedureCodeSequenceIfAbsent(AttributeList srcList) {
		{
			Attribute performedProcedureCodeSequence = list.get(TagFromName.PerformedProcedureCodeSequence);
			Attribute procedureCodeSequence = list.get(TagFromName.ProcedureCodeSequence);
			if (performedProcedureCodeSequence == null || !(performedProcedureCodeSequence instanceof SequenceAttribute) || ((SequenceAttribute)performedProcedureCodeSequence).getNumberOfItems() == 0) {
				if (procedureCodeSequence != null && procedureCodeSequence instanceof SequenceAttribute) {
					SequenceAttribute sProcedureCodeSequence = (SequenceAttribute)procedureCodeSequence;
					int nItems = sProcedureCodeSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sPerformedProcedureCodeSequence = new SequenceAttribute(TagFromName.PerformedProcedureCodeSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sProcedureCodeSequence.getItem(i);
//System.err.println("CompositeInstanceContext.createPerformedProcedureCodeSequenceIfAbsent(): copying ProcedureCodeSequence to PerformedProcedureCodeSequence item "+item);
							sPerformedProcedureCodeSequence.addItem(item);			// re-use of same item without cloning it is fine
						}
						list.put(sPerformedProcedureCodeSequence);
					}
				}
			}
		}
	}

	/**
	 * <p>Add or replace all of the composite context module {@link com.pixelmed.dicom.Attribute Attribute}s with values from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * <p>If an {@link com.pixelmed.dicom.Attribute Attribute} is empty or missing in the supplied list, the existing value in the context is left unchanged (not removed or emptied).</p>
	 *
	 * <p>This is useful when building composite context from multiple input composite instances, in which optional {@link com.pixelmed.dicom.Attribute Attribute}s are filled in some,
	 * but not others, in order to accumulate the most information available.</p>
	 *
	 * @param	srcList		the list of attributes to use as the source
	 * @param	forSR		true if need to populate the SR Document General Module specific {@link com.pixelmed.dicom.Attribute Attribute}s from their image equivalents
	 */
	public void updateFromSource(AttributeList srcList,boolean forSR) {
		for (AttributeTag t : patientModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : clinicalTrialSubjectModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalSeriesModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : sopCommonModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalImageModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }

		if (forSR) {
			createReferencedRequestSequenceIfAbsent(srcList);
			createPerformedProcedureCodeSequenceIfAbsent(srcList);
		}
		
		list.removeGroupLengthAttributes();		// may be present within in Sequences that have been copied
	}

	/**
	 * <p>Add or replace all of the composite context module {@link com.pixelmed.dicom.Attribute Attribute}s with values from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * <p>If an {@link com.pixelmed.dicom.Attribute Attribute} is empty or missing in the supplied list, the existing value in the context is left unchanged (not removed or emptied).</p>
	 *
	 * <p>This is useful when building composite context from multiple input composite instances, in which optional {@link com.pixelmed.dicom.Attribute Attribute}s are filled in some,
	 * but not others, in order to accumulate the most information available.</p>
	 *
	 * <p>Also populates the SR Document General Module specific {@link com.pixelmed.dicom.Attribute Attribute}s from their image equivalents.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.dicom.CompositeInstanceContext#updateFromSource(AttributeList,boolean) updateFromSource(AttributeList,boolean)} instead
	 *
	 * @param	srcList		the list of attributes to use as the source
	 */
	public void updateFromSource(AttributeList srcList) {
		updateFromSource(srcList,true/*forSR*/);
	}

	/**
	 * <p>Remove the Patient and Clinical Trial Subject module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removePatient(AttributeList list) {
		for (AttributeTag t : patientModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : clinicalTrialSubjectModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the study, series, equipment, frame of reference and instance level module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeAllButPatient(AttributeList list) {
		removeStudy(list);
		removeSeries(list);
		removeEquipment(list);
		removeFrameOfReference(list);
		removeInstance(list);
		removeSRDocumentGeneral(list);
	}
	
	/**
	 * <p>Remove the series, equipment, frame of reference and instance level module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeAllButPatientAndStudy(AttributeList list) {
		removeSeries(list);
		removeEquipment(list);
		removeFrameOfReference(list);
		removeInstance(list);
		removeSRDocumentGeneral(list);
	}
	
	/**
	 * <p>Remove the General Study and Patient Study module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeStudy(AttributeList list) {
		for (AttributeTag t : generalStudyModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the General Series module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeSeries(AttributeList list) {
		for (AttributeTag t : generalSeriesModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the General Equipment module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeEquipment(AttributeList list) {
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the Frame of Reference module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeFrameOfReference(AttributeList list) {
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the SOP Common and General Image module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeInstance(AttributeList list) {
		for (AttributeTag t : sopCommonModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : generalImageModuleAttributeTags) { list.remove(t); }
	}
	
	/**
	 * <p>Remove the SR Document General Image module {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list	the list of attributes to edit
	 */
	public static void removeSRDocumentGeneral(AttributeList list) {
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { list.remove(t); }
	}

	/**
	 * <p>A class to select which entities are copied or propagated or removed or not during operations on CompositeInstanceContext.</p>
	*/

	public static class Selector {
		public boolean patient;
		public boolean study;
		public boolean equipment;
		public boolean frameOfReference;
		public boolean series;
		public boolean instance;
		public boolean srDocumentGeneral;
		
		/**
		 * <p>Construct a selector with all modules selected or not selected.</p>
		 *
		 * @param	allSelected		true if all modules are selected rather than not selected on construction
		 */
		public Selector(boolean allSelected) {
			if (allSelected) {
				patient = true;
				study = true;
				equipment = true;
				frameOfReference = true;
				series = true;
				instance = true;
				srDocumentGeneral = true;
			}
		}
		
		/**
		 * <p>Construct a selector with only modules named in arguments selected.</p>
		 *
		 * <p>Used to decode selectors from command line arguments.</p>
		 *
		 * <p>Strings recognized are -patient|-study|-equipment|-frameofreference|-series|-instance|srdocumentgeneral.</p>
		 *
		 * @param	arg			command line arguments
		 * @param	remainder	empty list to add remaining command line arguments after anything used was removed
		 */
		public Selector(String[] arg,ArrayList<String> remainder) {
			for (String sa : arg) {
				String s = sa.toLowerCase().trim();
				if (s.equals("-patient")) {
					patient = true;
				}
				else if (s.equals("-study")) {
					study = true;
				}
				else if (s.equals("-equipment")) {
					equipment = true;
				}
				else if (s.equals("-frameofreference")) {
					frameOfReference = true;
				}
				else if (s.equals("-series")) {
					series = true;
				}
				else if (s.equals("-instance")) {
					instance = true;
				}
				else if (s.equals("-srdocumentgeneral")) {
					srDocumentGeneral = true;
				}
				else {
					remainder.add(sa);
				}
			}
		}
		
	}

	/**
	 * <p>Remove the unselected modules {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list		the list of attributes to edit
	 * @param	selector	the modules to keep
	 */
	public static void removeAllButSelected(AttributeList list,Selector selector) {
		if (!selector.patient) removePatient(list);
		if (!selector.study) removeStudy(list);
		if (!selector.equipment) removeEquipment(list);
		if (!selector.frameOfReference) removeFrameOfReference(list);
		if (!selector.series) removeSeries(list);
		if (!selector.instance) removeInstance(list);
		if (!selector.srDocumentGeneral) removeSRDocumentGeneral(list);
	}

	/**
	 * <p>Remove the selected modules {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * @param	list		the list of attributes to edit
	 * @param	selector	the modules to remove
	 */
	public static void removeAllSelected(AttributeList list,Selector selector) {
		if (selector.patient) removePatient(list);
		if (selector.study) removeStudy(list);
		if (selector.equipment) removeEquipment(list);
		if (selector.frameOfReference) removeFrameOfReference(list);
		if (selector.series) removeSeries(list);
		if (selector.instance) removeInstance(list);
		if (selector.srDocumentGeneral) removeSRDocumentGeneral(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removePatient(AttributeList) removePatient(AttributeList)}.
	 *
	 */
	public void removePatient() {
		removePatient(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeAllButPatient(AttributeList) removeAllButPatient}.
	 *
	 */
	public void removeAllButPatient() {
		removeAllButPatient(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeAllButPatientAndStudy(AttributeList) removeAllButPatientAndStudy}.
	 *
	 */
	public void removeAllButPatientAndStudy() {
		removeAllButPatientAndStudy(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeStudy(AttributeList) removeStudy}.
	 *
	 */
	public void removeStudy() {
		removeStudy(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeSeries(AttributeList) removeSeries}.
	 *
	 */
	public void removeSeries() {
		removeSeries(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeEquipment(AttributeList) removeEquipment}.
	 *
	 */
	public void removeEquipment() {
		removeEquipment(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeFrameOfReference(AttributeList) removeFrameOfReference}.
	 *
	 */
	public void removeFrameOfReference() {
		removeFrameOfReference(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeInstance(AttributeList) removeInstance}.
	 *
	 */
	public void removeInstance() {
		removeInstance(list);
	}
	
	/**
	 * See {@link CompositeInstanceContext#removeSRDocumentGeneral(AttributeList) removeSRDocumentGeneral}.
	 *
	 */
	public void removeSRDocumentGeneral() {
		removeSRDocumentGeneral(list);
	}


	/**
	 * <p>Remove the unselected modules {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * See {@link CompositeInstanceContext#removeAllButSelected(AttributeList,Selector) removeAllButSelected}.
	 *
	 * @param	selector	the modules to keep
	 */
	public void removeAllButSelected(Selector selector) {
		if (!selector.patient) removePatient(list);
		if (!selector.study) removeStudy(list);
		if (!selector.equipment) removeEquipment(list);
		if (!selector.frameOfReference) removeFrameOfReference(list);
		if (!selector.series) removeSeries(list);
		if (!selector.instance) removeInstance(list);
		if (!selector.srDocumentGeneral) removeSRDocumentGeneral(list);
	}

	/**
	 * <p>Remove the selected modules {@link com.pixelmed.dicom.Attribute Attribute}s.</p>
	 *
	 * See {@link CompositeInstanceContext#removeAllSelected(AttributeList,Selector) removeAllSelected}.
	 *
	 * @param	selector	the modules to remove
	 */
	public void removeAllSelected(Selector selector) {
		if (selector.patient) removePatient(list);
		if (selector.study) removeStudy(list);
		if (selector.equipment) removeEquipment(list);
		if (selector.frameOfReference) removeFrameOfReference(list);
		if (selector.series) removeSeries(list);
		if (selector.instance) removeInstance(list);
		if (selector.srDocumentGeneral) removeSRDocumentGeneral(list);
	}

	
	public void put(Attribute a) {
		list.put(a);
	}
	
	public void putAll(AttributeList srcList) {
		list.putAll(srcList);
	}
	
	public String toString() {
		return list.toString();
	}

}


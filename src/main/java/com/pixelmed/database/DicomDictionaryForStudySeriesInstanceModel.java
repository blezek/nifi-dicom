/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.util.TreeSet;

import com.pixelmed.dicom.*;

/**
 * <p>The {@link com.pixelmed.database.DicomDictionaryForStudySeriesInstanceModel DicomDictionaryForStudySeriesInstanceModel} class
 * supports a minimal DICOM Study/Series/Instance model.</p>
 *
 * <p>Attributes of the DICOM Patient entity are included at the Study level.</p>
 *
 * <p>Attributes of the DICOM Procedure Step entity are included at the Series level.</p>
 *
 * <p>Attributes of the DICOM Concatenation entity are included at the Instance level.</p>
 *
 * <p>The subset of the DICOM standard dictionary elements that is included in this dictionary
 * (and hence in the database underlying any {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}
 * that uses this dictionary) consists of the following:</p>
 *
 * <ul>
 * <li>SOPClassUID</li>
 * <li>SOPInstanceUID</li>
 * <li>StudyDate</li>
 * <li>SeriesDate</li>
 * <li>ContentDate</li>
 * <li>AcquisitionDate</li>
 * <li>StudyTime</li>
 * <li>SeriesTime</li>
 * <li>ContentTime</li>
 * <li>AcquisitionTime</li>
 * <li>AcquisitionDateTime</li>
 * <li>AccessionNumber</li>
 * <li>Modality</li>
 * <li>Manufacturer</li>
 * <li>InstitutionName</li>
 * <li>StudyDescription</li>
 * <li>SeriesDescription</li>
 * <li>PatientName</li>
 * <li>PatientID</li>
 * <li>PatientBirthDate</li>
 * <li>PatientSex</li>
 * <li>PatientAge</li>
 * <li>PatientComments</li>
 * <li>StudyInstanceUID</li>
 * <li>SeriesInstanceUID</li>
 * <li>StudyID</li>
 * <li>SeriesNumber</li>
 * <li>AcquisitionNumber</li>
 * <li>InstanceNumber</li>
 * <li>DerivationDescription</li>
 * <li>LossyImageCompression</li>
 * <li>LossyImageCompressionRatio</li>
 * <li>LossyImageCompressionMethod</li>
 * <li>ClinicalTrialProtocolID</li>
 * <li>ClinicalTrialSiteID</li>
 * <li>ClinicalTrialSubjectID</li>
 * <li>ClinicalTrialTimePointID</li>
 * <li>ClinicalTrialTimePointDescription</li>
 * <li>TransferSyntaxUID</li>
 * <li>SourceApplicationEntityTitle</li>
 * </ul>
 *
 * @see com.pixelmed.database.StudySeriesInstanceModel
 * @see com.pixelmed.dicom.InformationEntity
 *
 * @author	dclunie
 */
public class DicomDictionaryForStudySeriesInstanceModel extends DicomDictionary {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDictionaryForStudySeriesInstanceModel.java,v 1.11 2017/01/24 10:50:35 dclunie Exp $";

	// N.B. requires that AttributeTag implement hashCode() method

	/***/
	protected void createTagList() {

		tagList = new TreeSet();	// sorted, based on AttributeTag's implementation of Comparable

                tagList.add(TagFromName.TransferSyntaxUID);
                tagList.add(TagFromName.SourceApplicationEntityTitle);

		tagList.add(TagFromName.SOPClassUID);
		tagList.add(TagFromName.SOPInstanceUID);
		tagList.add(TagFromName.StudyDate);
		tagList.add(TagFromName.SeriesDate);
		tagList.add(TagFromName.ContentDate);
		tagList.add(TagFromName.AcquisitionDate);
		tagList.add(TagFromName.StudyTime);
		tagList.add(TagFromName.SeriesTime);
		tagList.add(TagFromName.ContentTime);
		tagList.add(TagFromName.AcquisitionTime);
		tagList.add(TagFromName.AcquisitionDateTime);
		tagList.add(TagFromName.AccessionNumber);
		tagList.add(TagFromName.Modality);
		tagList.add(TagFromName.Manufacturer);
		tagList.add(TagFromName.InstitutionName);
		tagList.add(TagFromName.StudyDescription);
		tagList.add(TagFromName.SeriesDescription);
		tagList.add(TagFromName.PatientName);
		tagList.add(TagFromName.PatientID);
		tagList.add(TagFromName.PatientBirthDate);
		tagList.add(TagFromName.PatientSex);
		tagList.add(TagFromName.PatientAge);
		tagList.add(TagFromName.PatientComments);
		tagList.add(TagFromName.StudyInstanceUID);
		tagList.add(TagFromName.SeriesInstanceUID);
		tagList.add(TagFromName.StudyID);
		tagList.add(TagFromName.SeriesNumber);
		tagList.add(TagFromName.AcquisitionNumber);
		tagList.add(TagFromName.InstanceNumber);
		tagList.add(TagFromName.DerivationDescription);
		tagList.add(TagFromName.LossyImageCompression);
		tagList.add(TagFromName.LossyImageCompressionRatio);
		tagList.add(TagFromName.LossyImageCompressionMethod);

		tagList.add(TagFromName.ClinicalTrialProtocolID);
		tagList.add(TagFromName.ClinicalTrialSiteID);
		tagList.add(TagFromName.ClinicalTrialSubjectID);
		tagList.add(TagFromName.ClinicalTrialTimePointID);
		tagList.add(TagFromName.ClinicalTrialTimePointDescription);
	}

	/**
	 * @param	tag
	 */
	public InformationEntity getInformationEntityFromTag(AttributeTag tag) {
		InformationEntity ie = super.getInformationEntityFromTag(tag);
		if      (ie == InformationEntity.PATIENT)       return InformationEntity.STUDY;
		else if (ie == InformationEntity.PROCEDURESTEP) return InformationEntity.SERIES;
		else if (ie == InformationEntity.CONCATENATION) return InformationEntity.INSTANCE;
		else return ie;
	}
}

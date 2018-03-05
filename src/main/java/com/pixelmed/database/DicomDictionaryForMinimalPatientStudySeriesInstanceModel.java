/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.util.TreeSet;

import com.pixelmed.dicom.*;


/**
 * <p>The {@link com.pixelmed.database.DicomDictionaryForMinimalPatientStudySeriesInstanceModel DicomDictionaryForMinimalPatientStudySeriesInstanceModel} class
 * supports a simple DICOM Patient/Study/Series/Concatenation/Instance model.</p>
 *
 * <p>The subset of the DICOM standard dictionary elements that is included in this dictionary
 * (and hence in the database underlying any {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}
 * that uses this dictionary) consists of the following:</p>
 *
 * <ul>
 * <li>TransferSyntaxUID</li>
 * <li>SourceApplicationEntityTitle</li>
 * <li>SpecificCharacterSet</li>
 * <li>ImageType</li>
 * <li>SOPClassUID</li>
 * <li>SOPInstanceUID</li>
 * <li>Manufacturer</li>
 * <li>PatientName</li>
 * <li>PatientID</li>
 * <li>PatientBirthDate</li>
 * <li>PatientSex</li>
 * <li>StudyInstanceUID</li>
 * <li>SeriesInstanceUID</li>
 * <li>StudyID</li>
 * <li>SeriesNumber</li>
 * <li>SeriesDescription</li>
 * <li>Modality</li>
 * <li>InstanceNumber</li>
 * <li>InstanceCreatorUID</li>
 * </ul>
 *
 * @see com.pixelmed.database.MinimalPatientStudySeriesInstanceModel
 * @see com.pixelmed.dicom.InformationEntity
 *
 * @author	dclunie
 */
public class DicomDictionaryForMinimalPatientStudySeriesInstanceModel extends DicomDictionary {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDictionaryForMinimalPatientStudySeriesInstanceModel.java,v 1.5 2017/01/24 10:50:35 dclunie Exp $";

	// N.B. requires that AttributeTag implement hashCode() method

	/***/
	protected void createTagList() {
//System.err.println("DicomDictionaryForMinimalPatientStudySeriesInstanceModel.createTagList():");
		tagList = new TreeSet();	// sorted, based on AttributeTag's implementation of Comparable

		tagList.add(TagFromName.TransferSyntaxUID);
		tagList.add(TagFromName.SourceApplicationEntityTitle);

		tagList.add(TagFromName.SpecificCharacterSet);
		tagList.add(TagFromName.ImageType);
		tagList.add(TagFromName.SOPClassUID);
		tagList.add(TagFromName.SOPInstanceUID);
		tagList.add(TagFromName.StudyDate);
		tagList.add(TagFromName.Manufacturer);
		tagList.add(TagFromName.PatientName);
		tagList.add(TagFromName.PatientID);
		tagList.add(TagFromName.PatientBirthDate);
		tagList.add(TagFromName.PatientSex);
		tagList.add(TagFromName.StudyInstanceUID);
		tagList.add(TagFromName.SeriesInstanceUID);
		tagList.add(TagFromName.StudyID);
		tagList.add(TagFromName.SeriesNumber);
		tagList.add(TagFromName.SeriesDescription);
		tagList.add(TagFromName.Modality);
		tagList.add(TagFromName.InstanceNumber);
		tagList.add(TagFromName.InstanceCreatorUID);
	}
}

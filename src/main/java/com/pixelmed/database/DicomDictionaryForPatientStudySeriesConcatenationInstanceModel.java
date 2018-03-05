/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.util.TreeSet;

import com.pixelmed.dicom.*;


/**
 * <p>The {@link com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel DicomDictionaryForPatientStudySeriesConcatenationInstanceModel} class
 * supports a simple DICOM Patient/Study/Series/Concatenation/Instance model.</p>
 *
 * <p>Attributes of the DICOM Procedure Step entity are included at the Series level.</p>
 *
 * <p>The subset of the DICOM standard dictionary elements that is included in this dictionary
 * (and hence in the database underlying any {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}
 * that uses this dictionary) consists of the following:</p>
 *
 * <ul>
 * <li>SpecificCharacterSet</li>
 * <li>ImageType</li>
 * <li>SOPClassUID</li>
 * <li>SOPInstanceUID</li>
 * <li>StudyDate</li>
 * <li>SeriesDate</li>
 * <li>ContentDate</li>
 * <li>AcquisitionDateTime</li>
 * <li>StudyTime</li>
 * <li>SeriesTime</li>
 * <li>AccessionNumber</li>
 * <li>Modality</li>
 * <li>ConversionType</li>
 * <li>PresentationIntentType</li>
 * <li>Manufacturer</li>
 * <li>InstitutionName</li>
 * <li>ReferringPhysicianName</li>
 * <li>StudyDescription</li>
 * <li>SeriesDescription</li>
 * <li>InstitutionalDepartmentName</li>
 * <li>PhysiciansOfRecord</li>
 * <li>PerformingPhysicianName</li>
 * <li>NameOfPhysiciansReadingStudy</li>
 * <li>OperatorsName</li>
 * <li>AdmittingDiagnosesDescription</li>
 * <li>AdditionalPatientHistory</li>
 * <li>DerivationDescription</li>
 * <li>PixelPresentation</li>
 * <li>VolumetricProperties</li>
 * <li>VolumeBasedCalculationTechnique</li>
 * <li>ComplexImageComponent</li>
 * <li>AcquisitionContrast</li>
 * <li>PatientName</li>
 * <li>PatientID</li>
 * <li>PatientBirthDate</li>
 * <li>PatientBirthTime</li>
 * <li>PatientSex</li>
 * <li>PatientAge</li>
 * <li>PatientSize</li>
 * <li>PatientWeight</li>
 * <li>PatientComments</li>
 * <li>EthnicGroup</li>
 * <li>Occupation</li>
 * <li>OtherPatientIDs</li>
 * <li>OtherPatientNames</li>
 * <li>ContrastBolusAgent</li>
 * <li>BodyPartExamined</li>
 * <li>ProtocolName</li>
 * <li>PulseSequenceName</li>
 * <li>StudyInstanceUID</li>
 * <li>SeriesInstanceUID</li>
 * <li>StudyID</li>
 * <li>OtherStudyNumbers</li>
 * <li>InterpretationAuthor</li>
 * <li>PerformedProcedureStepID</li>
 * <li>PerformedProcedureStepStartDate</li>
 * <li>PerformedProcedureStepStartTime</li>
 * <li>SeriesNumber</li>
 * <li>AcquisitionNumber</li>
 * <li>InstanceNumber</li>
 * <li>ImagePositionPatient</li>
 * <li>ImageOrientationPatient</li>
 * <li>Laterality</li>
 * <li>ImageLaterality</li>
 * <li>ImageComments</li>
 * <li>ConcatenationUID</li>
 * <li>InConcatenationNumber</li>
 * <li>InConcatenationTotalNumber</li>
 * <li>NumberOfFrames</li>
 * <li>QualityControlImage</li>
 * <li>BurnedInAnnotation</li>
 * <li>LossyImageCompression</li>
 * <li>LossyImageCompressionRatio</li>
 * <li>LossyImageCompressionMethod</li>
 * <li>PhotometricInterpretation</li>
 * <li>BitsStored</li>
 * <li>BitsAllocated</li>
 * <li>PixelRepresentation</li>
 * <li>WindowCenter</li>
 * <li>WindowWidth</li>
 * <li>Rows</li>
 * <li>Columns</li>
 * </ul>
 *
 * @see com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel
 * @see com.pixelmed.dicom.InformationEntity
 *
 * @author	dclunie
 */
public class DicomDictionaryForPatientStudySeriesConcatenationInstanceModel extends DicomDictionary {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDictionaryForPatientStudySeriesConcatenationInstanceModel.java,v 1.13 2017/01/24 10:50:35 dclunie Exp $";

	// N.B. requires that AttributeTag implement hashCode() method

	/***/
	protected void createTagList() {
//System.err.println("DicomDictionaryForPatientStudySeriesConcatenationInstanceModel.createTagList():");
		tagList = new TreeSet();	// sorted, based on AttributeTag's implementation of Comparable

		tagList.add(TagFromName.TransferSyntaxUID);
		tagList.add(TagFromName.SourceApplicationEntityTitle);

		tagList.add(TagFromName.SpecificCharacterSet);
		tagList.add(TagFromName.ImageType);
		tagList.add(TagFromName.SOPClassUID);
		tagList.add(TagFromName.SOPInstanceUID);
		tagList.add(TagFromName.FrameOfReferenceUID);
		tagList.add(TagFromName.StudyDate);
		tagList.add(TagFromName.SeriesDate);
		tagList.add(TagFromName.ContentDate);
		tagList.add(TagFromName.AcquisitionDateTime);
		tagList.add(TagFromName.StudyTime);
		tagList.add(TagFromName.SeriesTime);
		tagList.add(TagFromName.AccessionNumber);
		tagList.add(TagFromName.Modality);
		tagList.add(TagFromName.ConversionType);
		tagList.add(TagFromName.PresentationIntentType);
		tagList.add(TagFromName.Manufacturer);
		tagList.add(TagFromName.InstitutionName);
		tagList.add(TagFromName.ReferringPhysicianName);
		tagList.add(TagFromName.StudyDescription);
		tagList.add(TagFromName.SeriesDescription);
		tagList.add(TagFromName.InstitutionalDepartmentName);
		tagList.add(TagFromName.PhysiciansOfRecord);
		tagList.add(TagFromName.PerformingPhysicianName);
		tagList.add(TagFromName.NameOfPhysiciansReadingStudy);
		tagList.add(TagFromName.OperatorsName);
		tagList.add(TagFromName.AdmittingDiagnosesDescription);
		tagList.add(TagFromName.AdditionalPatientHistory);
		tagList.add(TagFromName.DerivationDescription);
		tagList.add(TagFromName.PixelPresentation);
		tagList.add(TagFromName.VolumetricProperties);
		tagList.add(TagFromName.VolumeBasedCalculationTechnique);
		tagList.add(TagFromName.ComplexImageComponent);
		tagList.add(TagFromName.AcquisitionContrast);
		tagList.add(TagFromName.PatientName);
		tagList.add(TagFromName.PatientID);
		tagList.add(TagFromName.PatientBirthDate);
		tagList.add(TagFromName.PatientBirthTime);
		tagList.add(TagFromName.PatientSex);
		tagList.add(TagFromName.PatientAge);
		tagList.add(TagFromName.PatientSize);
		tagList.add(TagFromName.PatientWeight);
		tagList.add(TagFromName.PatientComments);
		tagList.add(TagFromName.EthnicGroup);
		tagList.add(TagFromName.Occupation);
		tagList.add(TagFromName.OtherPatientIDs);
		tagList.add(TagFromName.OtherPatientNames);
		tagList.add(TagFromName.ContrastBolusAgent);
		tagList.add(TagFromName.BodyPartExamined);
		tagList.add(TagFromName.ProtocolName);
		tagList.add(TagFromName.PulseSequenceName);
		tagList.add(TagFromName.StudyInstanceUID);
		tagList.add(TagFromName.SeriesInstanceUID);
		tagList.add(TagFromName.StudyID);
		tagList.add(TagFromName.OtherStudyNumbers);
		tagList.add(TagFromName.InterpretationAuthor);
		tagList.add(TagFromName.PerformedProcedureStepID);
		tagList.add(TagFromName.PerformedProcedureStepStartDate);
		tagList.add(TagFromName.PerformedProcedureStepStartTime);
		tagList.add(TagFromName.SeriesNumber);
		tagList.add(TagFromName.AcquisitionNumber);
		tagList.add(TagFromName.InstanceNumber);
		tagList.add(TagFromName.ImagePositionPatient);
		tagList.add(TagFromName.ImageOrientationPatient);
		tagList.add(TagFromName.Laterality);
		tagList.add(TagFromName.ImageLaterality);
		tagList.add(TagFromName.ImageComments);
		tagList.add(TagFromName.ConcatenationUID);
		tagList.add(TagFromName.InConcatenationNumber);
		tagList.add(TagFromName.InConcatenationTotalNumber);
		tagList.add(TagFromName.NumberOfFrames);
		tagList.add(TagFromName.QualityControlImage);
		tagList.add(TagFromName.BurnedInAnnotation);
		tagList.add(TagFromName.LossyImageCompression);
		tagList.add(TagFromName.LossyImageCompressionRatio);
		tagList.add(TagFromName.LossyImageCompressionMethod);
		tagList.add(TagFromName.PhotometricInterpretation);
		tagList.add(TagFromName.BitsStored);
		tagList.add(TagFromName.BitsAllocated);
		tagList.add(TagFromName.PixelRepresentation);
		tagList.add(TagFromName.WindowCenter);			// added to support WADO ... is Instance Level in dictionary
		tagList.add(TagFromName.WindowWidth);
		tagList.add(TagFromName.Rows);
		tagList.add(TagFromName.Columns);
	}
}

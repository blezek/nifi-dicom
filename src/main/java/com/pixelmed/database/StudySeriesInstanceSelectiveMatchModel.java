/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.*;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>The {@link com.pixelmed.database.StudySeriesInstanceSelectiveMatchModel StudySeriesInstanceSelectiveMatchModel} class
 * supports a minimal DICOM Study/Series/Instance model.</p>
 *
 * <p>Matching of each information entity is performed by all appropriate attributes at
 * that level, not just the instance UIDs alone that are used in {@link com.pixelmed.database.StudySeriesInstanceModel StudySeriesInstanceModel}.</p>
 *
 * <p>Attributes of other DICOM entities than Study, Series and Instance are included at the appropriate lower level entity.</p>
 *
 * @see com.pixelmed.database.StudySeriesInstanceModel
 *
 * @author	dclunie
 */
public class StudySeriesInstanceSelectiveMatchModel extends StudySeriesInstanceModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/StudySeriesInstanceSelectiveMatchModel.java,v 1.11 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Construct a model with the attributes from the default dictionary.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForStudySeriesInstanceModel DicomDictionaryForStudySeriesInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @throws	DicomException
	 */
	public StudySeriesInstanceSelectiveMatchModel(String databaseFileName) throws DicomException {
		super(databaseFileName);
	}

	/**
	 * <p>Construct a model with the attributes from the default dictionary allowing external SQL access.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForStudySeriesInstanceModel DicomDictionaryForStudySeriesInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @throws	DicomException
	 */
	public StudySeriesInstanceSelectiveMatchModel(String databaseFileName,String databaseServerName) throws DicomException {
		super(databaseFileName,databaseServerName);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary.</p>
	 *
	 * @param	databaseFileName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public StudySeriesInstanceSelectiveMatchModel(String databaseFileName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,dictionary);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary allowing external SQL access.</p>
	 *
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public StudySeriesInstanceSelectiveMatchModel(String databaseFileName,String databaseServerName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,databaseServerName,dictionary);
	}

	/**
	 * @param	b
	 * @param	list
	 * @param	ie
	 * @throws	DicomException
	 */
	protected void extendStatementStringWithMatchingAttributesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {

		// two possibilities ...
		// 1. iterate through whole list of attributes and insist on match for all present for that IE
		// 2. be more selective ... consider match only on "unique key(s)" for a particular level and ignore others
		//
		// adopt the former approach ...

		// also need to escape wildcards and so on, but ignore for now ...

		if      (ie == InformationEntity.STUDY) {
			// no AND since would be no parent reference preceding
			b.append("STUDY.STUDYINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyInstanceUID)));
			b.append(" AND ");
			b.append("STUDY.STUDYID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyID)));
			b.append(" AND ");
			b.append("STUDY.STUDYDATE");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyDate)));
			b.append(" AND ");
			b.append("STUDY.STUDYTIME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyTime)));
			b.append(" AND ");
			b.append("STUDY.STUDYDESCRIPTION");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyDescription)));
			b.append(" AND ");
			b.append("STUDY.ACCESSIONNUMBER");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.AccessionNumber)));
			b.append(" AND ");
			b.append("STUDY.PATIENTNAME");
			{
				// (000675) Need to remove trailing empty name component delimiters, so that they will match same name without them, to match what
				// INSERT statement that uses com.pixelmed.database.DicomDatabaseInformationModel.extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity()
				String value = getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientName));
				if (value != null && value.contains("^")) {
					value = value.replaceFirst("\\^+\'","\'");		// Trailing empty components are of no significance so should be treated as absent so that will match whether present or not
				}
				appendExactOrIsNullMatch(b,value);
			}
			b.append(" AND ");
			b.append("STUDY.PATIENTID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientID)));
			b.append(" AND ");
			b.append("STUDY.PATIENTBIRTHDATE");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientBirthDate)));
			b.append(" AND ");
			b.append("STUDY.PATIENTSEX");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientSex)));
		}
		else if (ie == InformationEntity.SERIES) {
			b.append(" AND ");
			b.append("SERIES.SERIESINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesInstanceUID)));
			b.append(" AND ");
			b.append("SERIES.SERIESDATE");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesDate)));
			b.append(" AND ");
			b.append("SERIES.SERIESTIME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesTime)));
			b.append(" AND ");
			b.append("SERIES.SERIESNUMBER");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesNumber)));
			b.append(" AND ");
			b.append("SERIES.SERIESDESCRIPTION");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesDescription)));
			b.append(" AND ");
			b.append("SERIES.MODALITY");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.Modality)));
			b.append(" AND ");
			b.append("SERIES.MANUFACTURER");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.Manufacturer)));
			b.append(" AND ");
			b.append("SERIES.INSTITUTIONNAME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.InstitutionName)));
		}
		else if (ie == InformationEntity.INSTANCE) {
			b.append(" AND ");
			b.append("INSTANCE.SOPINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SOPInstanceUID)));
			b.append(" AND ");
			b.append("INSTANCE.CONTENTDATE");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.ContentDate)));
			b.append(" AND ");
			b.append("INSTANCE.CONTENTTIME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.ContentTime)));
			b.append(" AND ");
			b.append("INSTANCE.ACQUISITIONDATE");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.AcquisitionDate)));
			b.append(" AND ");
			b.append("INSTANCE.ACQUISITIONTIME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.AcquisitionTime)));
			b.append(" AND ");
			b.append("INSTANCE.ACQUISITIONDATETIME");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.AcquisitionDateTime)));
			b.append(" AND ");
			b.append("INSTANCE.INSTANCENUMBER");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.InstanceNumber)));
		}
	}
}


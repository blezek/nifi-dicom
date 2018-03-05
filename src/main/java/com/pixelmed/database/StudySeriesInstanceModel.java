/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.*;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>The {@link com.pixelmed.database.StudySeriesInstanceModel StudySeriesInstanceModel} class
 * supports a minimal DICOM Study/Series/Instance model.</p>
 *
 * <p>Matching of each information entity is performed by using only the
 * instance UIDs, not the list of attributes that are used in {@link com.pixelmed.database.StudySeriesInstanceSelectiveMatchModel StudySeriesInstanceSelectiveMatchModel}.</p>
 *
 * <p>Attributes of other DICOM entities than Study, Series and Instance are included at the appropriate lower level entity.</p>
 *
 * @see com.pixelmed.database.StudySeriesInstanceSelectiveMatchModel
 * @see com.pixelmed.database.DicomDictionaryForStudySeriesInstanceModel
 *
 * @author	dclunie
 */
public class StudySeriesInstanceModel extends DicomDatabaseInformationModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/StudySeriesInstanceModel.java,v 1.16 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Construct a model with the attributes from the default dictionary.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForStudySeriesInstanceModel DicomDictionaryForStudySeriesInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @throws	DicomException
	 */
	public StudySeriesInstanceModel(String databaseFileName) throws DicomException {
		super(databaseFileName,InformationEntity.STUDY,new DicomDictionaryForStudySeriesInstanceModel());
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
	public StudySeriesInstanceModel(String databaseFileName,String databaseServerName) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.STUDY,new DicomDictionaryForStudySeriesInstanceModel());
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary.</p>
	 *
	 * @param	databaseFileName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public StudySeriesInstanceModel(String databaseFileName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,InformationEntity.STUDY,dictionary);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary allowing external SQL access.</p>
	 *
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public StudySeriesInstanceModel(String databaseFileName,String databaseServerName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.STUDY,dictionary);
	}

	/**
	 * @param	ie	the information entity
	 * @return		true if the information entity is in the model
	 */
	protected boolean isInformationEntityInModel(InformationEntity ie) {
		return ie == InformationEntity.STUDY
		    || ie == InformationEntity.SERIES
		    || ie == InformationEntity.INSTANCE
		    ;
	}

	/**
	 * @param	ie			the parent information entity
	 * @param	concatenation		true if concatenations are to be considered in the model
	 * @return				the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie,boolean concatenation) {
		if      (ie == InformationEntity.STUDY)         return InformationEntity.SERIES;
		else if (ie == InformationEntity.SERIES)        return InformationEntity.INSTANCE;
		else return null;
	}
	
	/**
	 * @param	ie			the parent information entity
	 * @param	concatenationUID	the ConcatenationUID, if present, else null, as a flag to use concatenations in the model or not
	 * @return				the child information entity
	 */
	private InformationEntity getChildTypeForParent(InformationEntity ie,String concatenationUID) {
		return getChildTypeForParent(ie,concatenationUID != null);
	}

	/**
	 * @param	ie	the parent information entity
	 * @return		the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie) {
		return getChildTypeForParent(ie,true);		// this method is called e.g. when creating tables, so assume concatenation, if allowed
	}

	/**
	 * @param	ie	the parent information entity
	 * @param	list	an AttributeList, in which ConcatenationUID may or may not be present,as a flag to use concatenations in the model or not
	 * @return		the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie,AttributeList list) {
		String concatenationUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ConcatenationUID);
		return getChildTypeForParent(ie,concatenationUID);
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of a column in the table that describes the instance of the information entity
	 */
	public String getDescriptiveColumnName(InformationEntity ie) {
		if      (ie == InformationEntity.STUDY)         return "STUDYID";
		else if (ie == InformationEntity.SERIES)        return "SERIESNUMBER";
		else if (ie == InformationEntity.INSTANCE)      return "INSTANCENUMBER";
		else return null;
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of another column in the table that describes the instance of the information entity
	 */
	public String getOtherDescriptiveColumnName(InformationEntity ie) {
		return null;
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of yet another column in the table that describes the instance of the information entity
	 */
	public String getOtherOtherDescriptiveColumnName(InformationEntity ie) {
		if      (ie == InformationEntity.STUDY)         return "STUDYDESCRIPTION";
		else if (ie == InformationEntity.SERIES)        return "SERIESDESCRIPTION";
		else if (ie == InformationEntity.INSTANCE)      return "IMAGECOMMENTS";
		else return null;
	}


	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of the column in the table that describes the UID of the information entity, or null if none
	 */
	public String getUIDColumnNameForInformationEntity(InformationEntity ie) {
		if      (ie == InformationEntity.STUDY)         return "STUDYINSTANCEUID";
		else if (ie == InformationEntity.SERIES)        return "SERIESINSTANCEUID";
		else if (ie == InformationEntity.INSTANCE)      return "SOPINSTANCEUID";
		else return null;
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
		// adopt the latter approach ...

		// also need to escape wildcards and so on, but ignore for now ...

		if      (ie == InformationEntity.STUDY) {
			// no AND since would be no parent reference preceding
			b.append("STUDY.STUDYINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyInstanceUID)));
		}
		else if (ie == InformationEntity.SERIES) {
			b.append(" AND ");
			b.append("SERIES.SERIESINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesInstanceUID)));
		}
		else if (ie == InformationEntity.INSTANCE) {
			b.append(" AND ");
			b.append("INSTANCE.SOPINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SOPInstanceUID)));
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeFactory;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SetOfDicomFiles;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.ValueRepresentation;

import com.pixelmed.network.ResponseStatus;

import com.pixelmed.query.RetrieveResponseGenerator;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

class DicomDatabaseRetrieveResponseGenerator implements RetrieveResponseGenerator {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDatabaseRetrieveResponseGenerator.java,v 1.8 2017/01/24 10:50:35 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomDatabaseRetrieveResponseGenerator.class);
	
	/***/
	private DatabaseInformationModel databaseInformationModel;
	/***/
	private AttributeList requestIdentifier;
	
	/***/
	private SetOfDicomFiles dicomFiles = null;
	/***/
	private int status = ResponseStatus.Success;
	/***/
	private AttributeTagAttribute offendingElement = null;
	/***/
	private String errorComment = null;

	protected void setErrorStatus(int status,AttributeTag offendingElementValue,String errorComment) {
		this.status = status;
		if (offendingElement == null) {
			offendingElement = new AttributeTagAttribute(TagFromName.OffendingElement);
		}
		if (offendingElementValue != null) {
			try {
				offendingElement.addValue(offendingElementValue);
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
		this.errorComment = errorComment;
	}

	private String                  patientTableName;
	private String                    studyTableName;
	private String                   seriesTableName;
	private String                 instanceTableName;
	private String       patientPrimaryKeyColumnName;
	private String         studyPrimaryKeyColumnName;
	private String        seriesPrimaryKeyColumnName;
	private String    studyParentReferenceColumnName;
	private String   seriesParentReferenceColumnName;
	private String instanceParentReferenceColumnName;
	private String             sopClassUIDColumnName;
	private String        studyInstanceUIDColumnName;
	private String       seriesInstanceUIDColumnName;
	private String          sopInstanceUIDColumnName;
	private String       transferSyntaxUIDColumnName;

	DicomDatabaseRetrieveResponseGenerator(DatabaseInformationModel databaseInformationModel) {
//System.err.println("DicomDatabaseRetrieveResponseGenerator():");
		this.databaseInformationModel=databaseInformationModel;

		dicomFiles = null;
		
		// the following are extracted here and cached to avoid looking them up repeatedly on every query response ...
		
				 patientTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.PATIENT);
		                   studyTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.STUDY);
		                  seriesTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.SERIES);
		                instanceTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.INSTANCE);
		      patientPrimaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.PATIENT);
		        studyPrimaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY);
		       seriesPrimaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.SERIES);
		   studyParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.STUDY);
		  seriesParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.SERIES);
		instanceParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.INSTANCE);
		            sopClassUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPClassUID);
		       studyInstanceUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.StudyInstanceUID);
		      seriesInstanceUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SeriesInstanceUID);
		         sopInstanceUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPInstanceUID);
		      transferSyntaxUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.TransferSyntaxUID);
	}
	
	private static void addToJoinClause(StringBuffer b,String wantedTableName,String parentTableName,String primaryKeyColumnName,String parentReferenceColumnName) {
		if (b.length() != 0) {
			b.append(" AND ");
		}
		b.append(parentTableName);
		b.append(".");
		b.append(primaryKeyColumnName);
		b.append(" = ");
		b.append(wantedTableName);
		b.append(".");
		b.append(parentReferenceColumnName);
	}
	
	private boolean addToSelectClause(StringBuffer b,AttributeList requestIdentifier,AttributeTag tag,String tableName,AttributeTagAttribute offendingElement) {
		String columnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(tag);
		boolean success = true;
		if (b.length() != 0) {
			b.append(" AND ");
		}
		Attribute a = requestIdentifier.get(tag);
		if (a == null || a.getVL() == 0) {
			success = false;
		}
		else {
			b.append(tableName);
			b.append(".");
			b.append(columnName);
			b.append(" = ");
			try {
				b.append(DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(a));
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
				success = false;		// partially extended buffer will be invalid at this point
			}
		}
		if (success == false) {
			setErrorStatus(ResponseStatus.IdentifierDoesNotMatchSOPClass,tag,"Could not extract element for query");
		}
		return success;
	}
	
	private String buildSelectStatementForLevelAndModelFromUniqueKeysInRequestIdentifier(String retrieveSOPClassUID,String queryRetrieveLevel,AttributeList requestIdentifier,
			AttributeTagAttribute offendingElement) {
		boolean success = true;
		StringBuffer b = new StringBuffer();
		if (queryRetrieveLevel == null) {
			slf4jlogger.debug("buildSelectStatementForLevelAndModelFromUniqueKeysInRequestIdentifier(): missing required QueryRetrieveLevel in C-MOVE request identifier");
			success=false;
			setErrorStatus(ResponseStatus.IdentifierDoesNotMatchSOPClass,TagFromName.QueryRetrieveLevel,"Missing retrieve level");
		}
		else {
			b.append("SELECT ");
			b.append(databaseInformationModel.localFileName);
			b.append(",");
			b.append(sopInstanceUIDColumnName);
			b.append(",");
			b.append(sopClassUIDColumnName);
			b.append(",");
			b.append(transferSyntaxUIDColumnName);
			b.append(" ");
			StringBuffer selectClause = new StringBuffer();
			if (SOPClass.isStudyRootCompositeInstanceRetrieve(retrieveSOPClassUID)) {
				b.append("FROM ");
				b.append(studyTableName);
				b.append(",");
				b.append(seriesTableName);
				b.append(",");
				b.append(instanceTableName);
				b.append(" WHERE ");
				if (queryRetrieveLevel.equals("STUDY")) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
				else if (queryRetrieveLevel.equals(seriesTableName)) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SeriesInstanceUID,seriesTableName,offendingElement);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
				else if (queryRetrieveLevel.equals("IMAGE")) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SeriesInstanceUID,seriesTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SOPInstanceUID,instanceTableName,offendingElement);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
			}
			else if (SOPClass.isPatientRootCompositeInstanceRetrieve(retrieveSOPClassUID)) {
				b.append("FROM ");
				b.append(patientTableName);
				b.append(",");
				b.append(studyTableName);
				b.append(",");
				b.append(seriesTableName);
				b.append(",");
				b.append(instanceTableName);
				b.append(" WHERE ");
				if (queryRetrieveLevel.equals("PATIENT")) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.PatientID,patientTableName,offendingElement);
					addToJoinClause(selectClause,studyTableName,patientTableName,
						patientPrimaryKeyColumnName,
						studyParentReferenceColumnName);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
				else if (queryRetrieveLevel.equals("STUDY")) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.PatientID,patientTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					addToJoinClause(selectClause,studyTableName,patientTableName,
						patientPrimaryKeyColumnName,
						studyParentReferenceColumnName);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
				else if (queryRetrieveLevel.equals(seriesTableName)) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.PatientID,patientTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SeriesInstanceUID,seriesTableName,offendingElement);
					addToJoinClause(selectClause,studyTableName,patientTableName,
						patientPrimaryKeyColumnName,
						studyParentReferenceColumnName);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
				else if (queryRetrieveLevel.equals("IMAGE")) {
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.PatientID,patientTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.StudyInstanceUID,studyTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SeriesInstanceUID,seriesTableName,offendingElement);
					success = success && addToSelectClause(selectClause,requestIdentifier,TagFromName.SOPInstanceUID,instanceTableName,offendingElement);
					addToJoinClause(selectClause,studyTableName,patientTableName,
						patientPrimaryKeyColumnName,
						studyParentReferenceColumnName);
					addToJoinClause(selectClause,seriesTableName,studyTableName,
						studyPrimaryKeyColumnName,
						seriesParentReferenceColumnName);
					// what about concatenation in the way ? :(
					addToJoinClause(selectClause,instanceTableName,seriesTableName,
						seriesPrimaryKeyColumnName,
						instanceParentReferenceColumnName);
				}
			}
			b.append(selectClause);
		}
		return success ? b.toString() : null;
	}
	
	public void performRetrieve(String retrieveSOPClassUID,AttributeList requestIdentifier,boolean relational) {
		slf4jlogger.debug("performRetrieve(): request:\n"+requestIdentifier.toString());
		this.requestIdentifier=requestIdentifier;
		String queryRetrieveLevel =  Attribute.getSingleStringValueOrNull(requestIdentifier,TagFromName.QueryRetrieveLevel);
		String query = buildSelectStatementForLevelAndModelFromUniqueKeysInRequestIdentifier(retrieveSOPClassUID,queryRetrieveLevel,requestIdentifier,offendingElement);
		slf4jlogger.debug("performRetrieve(): query: {}",query);
		if (query != null) {
			try {
				Statement databaseStatement = databaseInformationModel.createStatement();
				ResultSet resultSet = databaseStatement.executeQuery(query);
				dicomFiles = new SetOfDicomFiles();
				if (resultSet != null) {
					while (resultSet.next()) {
						dicomFiles.add(
							resultSet.getString(databaseInformationModel.localFileName),
							resultSet.getString(sopClassUIDColumnName),
							resultSet.getString(sopInstanceUIDColumnName),
							resultSet.getString(transferSyntaxUIDColumnName)
						);
					}
				}
				databaseStatement.close();
				status = ResponseStatus.Success;	// success
			}
			catch (SQLException e) {
				slf4jlogger.error("",e);
				dicomFiles = null;
				setErrorStatus(ResponseStatus.UnableToProcess,null,e.getMessage());
			}
		}
		else {
			dicomFiles = null;
			if (status == ResponseStatus.Success) {
				status = ResponseStatus.IdentifierDoesNotMatchSOPClass;	// should already have been set earlier together with offending element and error comment already set earlier
			}
		}
		slf4jlogger.debug("performRetrieve(): query performed - status 0x"+Integer.toHexString(status));
	}
	
	public SetOfDicomFiles getDicomFiles() { return dicomFiles; }
	
	public int getStatus() { return status; }
	
	public AttributeTagAttribute getOffendingElement() { return offendingElement; }
	
	public String getErrorComment() { return errorComment; }

	public void close() {
	}
}



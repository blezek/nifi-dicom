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
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SpecificCharacterSet;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.ValueRepresentation;

import com.pixelmed.network.ResponseStatus;

import com.pixelmed.query.QueryResponseGenerator;

import com.pixelmed.utils.StringUtilities;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

class DicomDatabaseQueryResponseGenerator implements QueryResponseGenerator {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDatabaseQueryResponseGenerator.java,v 1.24 2017/01/24 10:50:35 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomDatabaseQueryResponseGenerator.class);
	
	boolean usePhoneticCanonicalPersonNameMatch = true;	// this takes priority, unless wildcards are present
	boolean useCanonicalPersonNameMatch = true;
	boolean useSwappedPersonNameMatch = true;

	/***/
	private static final boolean includeUnsupportedOptionalKeysInResponseWithZeroLength = false;	// set this to true for non-standard behavior
	
	/***/
	private static final boolean includeModalitiesInStudyIfRequested = true;
	
	/***/
	private static final boolean includeSOPClassesInStudyIfRequested = true;
	
	/***/
	private static final boolean includeNumberOfStudyRelatedInstancesIfRequested = true;
	
	/***/
	private static final boolean includeNumberOfStudyRelatedSeriesIfRequested = true;
	
	/***/
	private static final boolean includeNumberOfSeriesRelatedInstancesIfRequested = true;
	
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

	/***/
	private DatabaseInformationModel databaseInformationModel;
	/***/
	private DicomDictionary dictionary;
	/***/
	private AttributeList requestIdentifier;
	/***/
	private Statement databaseStatement;
	/***/
	private ResultSet resultSet;
	/***/
	private ResultSetMetaData resultSetMetaData;
	/***/
	private String queryRetrieveLevel;
	/***/
	private boolean unsupportedOptionalKeysPresent;
	/***/
	private AttributeList additionalKeysToReturnAsZeroLength;
	
							
	private String                    studyTableName;
	private String                   seriesTableName;
	private String                 instanceTableName;
	private String         studyPrimaryKeyColumnName;
	private String        seriesPrimaryKeyColumnName;
	private String   seriesParentReferenceColumnName;
	private String instanceParentReferenceColumnName;
	private String                modalityColumnName;
	private String             sopClassUIDColumnName;
	private String        studyInstanceUIDColumnName;
	private String       seriesInstanceUIDColumnName;

	DicomDatabaseQueryResponseGenerator(DatabaseInformationModel databaseInformationModel) {
//System.err.println("DicomDatabaseQueryResponseGenerator():");
		this.databaseInformationModel=databaseInformationModel;
		requestIdentifier = null;
		databaseStatement = null;
		resultSet = null;
		resultSetMetaData = null;
		queryRetrieveLevel = null;
		unsupportedOptionalKeysPresent = false;
		additionalKeysToReturnAsZeroLength = null;

		// the following are extracted here and cached to avoid looking them up repeatedly on every query response ...
		
		                       dictionary = databaseInformationModel.getDicomDictionary();
		                   studyTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.STUDY);
		                  seriesTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.SERIES);
		                instanceTableName = databaseInformationModel.getTableNameForInformationEntity(InformationEntity.INSTANCE);
		        studyPrimaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY);
		       seriesPrimaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.SERIES);
		  seriesParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.SERIES);
		instanceParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.INSTANCE);
		               modalityColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.Modality);
		            sopClassUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPClassUID);
		       studyInstanceUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.StudyInstanceUID);
		      seriesInstanceUIDColumnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SeriesInstanceUID);
	}
	
	/**
	 * @param	querySOPClassUID	the SOP Class representing query information model
	 * @return				the root information entity
	 */
	private InformationEntity getRootInformationEntity(String querySOPClassUID) {
		InformationEntity root = null;
		if (SOPClass.isStudyRootCompositeInstanceQuery(querySOPClassUID)) {
			root = InformationEntity.STUDY;
		}
		else if (SOPClass.isPatientRootCompositeInstanceQuery(querySOPClassUID)) {
			root = InformationEntity.PATIENT;
		}
		return root;
	}

	/**
	 * @param	querySOPClassUID	the SOP Class representing query information model
	 * @param	ie			the parent information entity
	 * @return				the child information entity
	 */
	private InformationEntity getChildTypeForParent(String querySOPClassUID,InformationEntity ie) {
		InformationEntity child = null;
		if (SOPClass.isStudyRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ie == InformationEntity.STUDY) {
				child = InformationEntity.SERIES;
			}
			else if (ie == InformationEntity.SERIES) {
				child = InformationEntity.INSTANCE;
			}
			else if (ie == InformationEntity.INSTANCE) {
				child = null;
			}
		}
		else if (SOPClass.isPatientRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ie == InformationEntity.PATIENT) {
				child = InformationEntity.STUDY;
			}
			else if (ie == InformationEntity.STUDY) {
				child = InformationEntity.SERIES;
			}
			else if (ie == InformationEntity.SERIES) {
				child = InformationEntity.INSTANCE;
			}
			else if (ie == InformationEntity.INSTANCE) {
				child = null;
			}
		}
		return child;
	}

	/**
	 * @param	querySOPClassUID	the SOP Class representing query information model
	 * @param	ie			the parent information entity
	 * @return				the child information entity
	 */
	private AttributeTag getUniqueKeyForQueryInformationModel(String querySOPClassUID,InformationEntity ie) {
		AttributeTag tag = null;
		if (SOPClass.isStudyRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ie == InformationEntity.STUDY) {
				tag = TagFromName.StudyInstanceUID;
			}
			else if (ie == InformationEntity.SERIES) {
				tag = TagFromName.SeriesInstanceUID;
			}
			else if (ie == InformationEntity.INSTANCE) {
				tag = TagFromName.SOPInstanceUID;
			}
		}
		else if (SOPClass.isPatientRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ie == InformationEntity.PATIENT) {
				tag = TagFromName.PatientID;
			}
			else if (ie == InformationEntity.STUDY) {
				tag = TagFromName.StudyInstanceUID;
			}
			else if (ie == InformationEntity.SERIES) {
				tag = TagFromName.SeriesInstanceUID;
			}
			else if (ie == InformationEntity.INSTANCE) {
				tag = TagFromName.SOPInstanceUID;
			}
		}
		return tag;
	}

	/**
	 * @param	querySOPClassUID	the SOP Class representing query information model
	 * @param	ieTest			the information entity to test
	 * @param	ieLevel			the information entity representing the query level
	 * @return				true if the test information entity is at the same level as the query level information entity for the specified model
	 */
	private boolean isWithinQueryLevelForModel(String querySOPClassUID,InformationEntity ieTest,InformationEntity ieLevel) {
//slf4jlogger.info("performQuery(): isWithinQueryLevelForModel ieTest = {} ieLevel = {}",ieTest,ieLevel);
		boolean result = false;
		if (SOPClass.isStudyRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ieLevel == InformationEntity.STUDY) {
				result = ieTest.compareTo(InformationEntity.STUDY) <= 0;
			}
			else if (ieLevel == InformationEntity.SERIES) {
				result = ieTest.compareTo(InformationEntity.STUDY) > 0 && ieTest.compareTo(InformationEntity.SERIES) <= 0;
			}
			else if (ieLevel == InformationEntity.INSTANCE) {
				result = ieTest.compareTo(InformationEntity.SERIES) > 0 && ieTest.compareTo(InformationEntity.INSTANCE) <= 0;
			}
		}
		else if (SOPClass.isPatientRootCompositeInstanceQuery(querySOPClassUID)) {
			if (ieLevel == InformationEntity.PATIENT) {
				result = ieTest.compareTo(InformationEntity.PATIENT) <= 0;
			}
			else if (ieLevel == InformationEntity.STUDY) {
				result = ieTest.compareTo(InformationEntity.PATIENT) > 0 && ieTest.compareTo(InformationEntity.STUDY) <= 0;
			}
			else if (ieLevel == InformationEntity.SERIES) {
				result = ieTest.compareTo(InformationEntity.STUDY) > 0 && ieTest.compareTo(InformationEntity.SERIES) <= 0;
			}
			else if (ieLevel == InformationEntity.INSTANCE) {
				result = ieTest.compareTo(InformationEntity.SERIES) > 0 && ieTest.compareTo(InformationEntity.INSTANCE) <= 0;
			}
		}
//slf4jlogger.info("DicomDatabaseQueryResponseGenerator.performQuery(): isWithinQueryLevelForModel result = {}",result);
		return result;
	}
	
	/**
	 * @param	queryRetrieveLevel	the query retrieve level as specified in the identifier
	 * @return				the corresponding information entity
	 */
	private InformationEntity getInformationEntityForQueryRetieveLevel(String queryRetrieveLevel) {
		if (queryRetrieveLevel == null) {
			return null;
		}
		else if (queryRetrieveLevel.equals("PATIENT")) {
			return InformationEntity.PATIENT;
		}
		else if (queryRetrieveLevel.equals("STUDY")) {
			return InformationEntity.STUDY;
		}
		else if (queryRetrieveLevel.equals("SERIES")) {
			return InformationEntity.SERIES;
		}
		else if (queryRetrieveLevel.equals("IMAGE")) {
			return InformationEntity.INSTANCE;
		}
		else {
			return null;
		}
	}
	
	private static void addToJoinClause(StringBuffer b,String wantedTableName,String parentTableName,String primaryKeyColumnName,String parentReferenceColumnName) {
		if (b.length() != 0) {
			b.append(" AND ");
		}
		b.append(wantedTableName);
		b.append(".");
		b.append(parentReferenceColumnName);
		b.append(" = ");
		b.append(parentTableName);
		b.append(".");
		b.append(primaryKeyColumnName);
	}
	
	private static void addToMatchClause(StringBuffer b,String tableName,String columnName,String value,String booleanOperator) {
		if (b.length() != 0) {
			b.append(" ");
			b.append(booleanOperator);
			b.append(" ");
		}
		b.append(tableName);
		b.append(".");
		b.append(columnName);
		if (value.indexOf('*') == -1 && value.indexOf('?')  == -1) {
			b.append(" = ");
			b.append(value);						// is already quoted
		}
		else {
			b.append(" LIKE ");
			String sqlValue = new String(value);		// take care not to replace characters in original string, but in copy
			sqlValue = sqlValue.replace("%","\\%")	// replace any uses of the SQL wildcard characters with escaped versions
							   .replace("_","\\_")
							   .replace('*','%')		// replace DICOM wildcard characters with (unescaped obviously) SQL wildcard characters
							   .replace('?','_');
			b.append(sqlValue);							// is already quoted
			b.append(" ESCAPE '\\'");					// need to specify explicitly what escape character use is, but only do it when LIKE clause is used
		}
	}
	
	private static void addToMatchClause(StringBuffer b,String tableName,String columnName,String value) {
		addToMatchClause(b,tableName,columnName,value,"AND");
	}
	
	private static void addToSelectClause(StringBuffer b,String tableName,String columnName) {
		if (b.length() != 0) {
			b.append(",");
		}
		b.append(tableName);
		b.append(".");
		b.append(columnName);
	}
	
	private static void addToFromClause(StringBuffer b,String tableName) {
		if (b.length() == 0) {
			b.append(tableName);
		}
		else {
			if (b.indexOf(tableName) == -1) {
				b.append(",");
				b.append(tableName);
			}
		}
	}

	public void performQuery(String querySOPClassUID,AttributeList requestIdentifier,boolean relational) {
		slf4jlogger.debug("performQuery(): request:\n{}",requestIdentifier.toString());
		this.requestIdentifier=requestIdentifier;
		databaseStatement = null;
		resultSet = null;
		resultSetMetaData = null;
		queryRetrieveLevel =  Attribute.getSingleStringValueOrNull(requestIdentifier,TagFromName.QueryRetrieveLevel);
		additionalKeysToReturnAsZeroLength = null;	// filled in on first next() and used on subsequent next()'s
		unsupportedOptionalKeysPresent = false;		// filled in on first next() and used on subsequent next()'
		
		slf4jlogger.trace("performQuery(): queryRetrieveLevel = {}",queryRetrieveLevel);
		InformationEntity ieWanted = getInformationEntityForQueryRetieveLevel(queryRetrieveLevel);
		slf4jlogger.trace("performQuery(): ieWanted = {}",ieWanted);
		InformationEntity ieDatabase = databaseInformationModel.getRootInformationEntity();
		InformationEntity ieQuery = getRootInformationEntity(querySOPClassUID);
		InformationEntity ieDatabaseParent = null;
		
		StringBuffer joinBuffer = new StringBuffer();
		StringBuffer matchBuffer = new StringBuffer();
		StringBuffer selectBuffer = new StringBuffer();
		StringBuffer fromBuffer = new StringBuffer();

		if (ieWanted == null) {
			setErrorStatus(ResponseStatus.IdentifierDoesNotMatchSOPClass,TagFromName.QueryRetrieveLevel,"QueryRetrieveLevel is missing or invalid");
		}
		else if (ieQuery == null) {
			setErrorStatus(ResponseStatus.UnableToProcess,null,"Unrecognized or unsupported query model for SOP Class");
		}
		else {
			// assert ieQuery != null && ieDatabase != null && ieWanted != null
			
			while (ieQuery != null && ieQuery.compareTo(ieWanted) <= 0) {
				slf4jlogger.trace("performQuery(): ieQuery = {}",ieQuery);
				AttributeTag uniqueKey = getUniqueKeyForQueryInformationModel(querySOPClassUID,ieQuery);
				slf4jlogger.trace("performQuery(): uniqueKey at this level = {}",uniqueKey);
				while (ieDatabase != null && ieDatabase.compareTo(ieQuery) <= 0) {
					slf4jlogger.trace("performQuery(): ieDatabase = {}",ieDatabase);
					// extend select statement with match on:
					// - relational: any attribute in identifier for ieDatabase
					// - hierarchical: unique key attribute in identifier for ieDatabase ???
					
					String tableName = databaseInformationModel.getTableNameForInformationEntity(ieDatabase);
					
					if (ieDatabaseParent != null) {
						String parentTableName = databaseInformationModel.getTableNameForInformationEntity(ieDatabaseParent);
						addToJoinClause(joinBuffer,
							tableName,
							parentTableName,
							databaseInformationModel.getLocalPrimaryKeyColumnName(ieDatabase),
							databaseInformationModel.getLocalParentReferenceColumnName(ieDatabase)
						);
						addToFromClause(fromBuffer,parentTableName);		// since may not be any keys at this level otherwise
						addToFromClause(fromBuffer,tableName);
					}
										
					// find all attributes in identifier that are at this database ie level
					
					Set dateTimeRangeMatchAlreadyDone = new HashSet();
					Iterator i = requestIdentifier.values().iterator();
					while (i.hasNext()) {
						Attribute a = (Attribute)(i.next());
						AttributeTag tag = a.getTag();
						InformationEntity ieAttribute = databaseInformationModel.getInformationEntityFromTag(tag);
						String columnName = databaseInformationModel.getDatabaseColumnNameFromDicomTag(tag);
						String value = null;
						try {
							value = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(a);
						}
						catch (DicomException e) {
							setErrorStatus(ResponseStatus.UnableToProcess,tag,"Unable to convert into value for SQL query");
							return;
						}
						slf4jlogger.trace("performQuery(): checking attribute {} with value {}",columnName,value);

						if (columnName != null			// columnName could be null if attribute in identifier is unrecognized (e.g. private) 
						    /*&& ieAttribute == ieDatabase */	// don't check, otherwise won't pick up InstanceNumber whose IE is concatentation :(
						    && databaseInformationModel.isAttributeUsedInTable(tableName,columnName)) {
							if (isWithinQueryLevelForModel(querySOPClassUID,ieAttribute,ieWanted) || relational) {
								slf4jlogger.trace("performQuery(): using as matching key {} entity for identifier attribute {} with value {}",ieDatabase,columnName,value);
								if (value != null && value.length() > 0 && !value.equals("NULL")) {
									boolean used = false;
									byte[] vr = a.getVR();
									if (vr != null) {
										if (/*value.indexOf("-") != -1 && */	// always use range match even if not range
										   (ValueRepresentation.isDateVR(vr)
										 || ValueRepresentation.isTimeVR(vr)
										 || ValueRepresentation.isDateTimeVR(vr))
										) {
											if (DateTimeRangeMatch.addToMatchClause(
												matchBuffer,tableName,columnName,tag,requestIdentifier,dateTimeRangeMatchAlreadyDone)) {
												used = true;
											}
										}
										else if (ValueRepresentation.isPersonNameVR(vr)) {
											StringBuffer nameMatchBuffer = new StringBuffer();
											String canonicalValue = null;
											if (usePhoneticCanonicalPersonNameMatch || useCanonicalPersonNameMatch) {
												canonicalValue = PersonNameAttribute.getCanonicalForm(value,true);
											}
											if (usePhoneticCanonicalPersonNameMatch && canonicalValue != null && canonicalValue.length() > 0) {
												String newColumnName =
													  DatabaseInformationModel.personNamePhoneticCanonicalColumnNamePrefix
													+ columnName
													+ DatabaseInformationModel.personNamePhoneticCanonicalColumnNameSuffix;
												if (databaseInformationModel.isAttributeUsedInTable(tableName,newColumnName)) {
													String phoneticCanonicalValue = PersonNameAttribute.getPhoneticName(canonicalValue,true);
													addToMatchClause(nameMatchBuffer,tableName,newColumnName,
														DicomDatabaseInformationModel.getQuotedValueOrNULL(phoneticCanonicalValue),
														"OR");
													if (useSwappedPersonNameMatch) {
														String swappedPhoneticCanonicalValue = PersonNameAttribute.swap(phoneticCanonicalValue);
														if (!phoneticCanonicalValue.equals(swappedPhoneticCanonicalValue)) {
															addToMatchClause(nameMatchBuffer,tableName,newColumnName,
																DicomDatabaseInformationModel.getQuotedValueOrNULL(
																	swappedPhoneticCanonicalValue),
																"OR");
														}
													}
												}
											}
											if (useCanonicalPersonNameMatch && canonicalValue != null && canonicalValue.length() > 0) {
												String newColumnName =
													  DatabaseInformationModel.personNameCanonicalColumnNamePrefix
													+ columnName
													+ DatabaseInformationModel.personNameCanonicalColumnNameSuffix;
												if (databaseInformationModel.isAttributeUsedInTable(tableName,newColumnName)) {
													addToMatchClause(nameMatchBuffer,tableName,newColumnName,
														DicomDatabaseInformationModel.getQuotedValueOrNULL(canonicalValue),
														"OR");
													if (useSwappedPersonNameMatch) {
														String swappedCanonicalValue = PersonNameAttribute.swap(canonicalValue);
														if (!canonicalValue.equals(swappedCanonicalValue)) {
															addToMatchClause(nameMatchBuffer,tableName,newColumnName,
																DicomDatabaseInformationModel.getQuotedValueOrNULL(swappedCanonicalValue),
																"OR");
														}
													}
												}
											}
											addToMatchClause(nameMatchBuffer,tableName,columnName,value,"OR");
											if (nameMatchBuffer.length() > 0) {
												if (matchBuffer.length() > 0) {
													matchBuffer.append(" AND ");
												}
												matchBuffer.append("(");
												matchBuffer.append(nameMatchBuffer);
												matchBuffer.append(")");
												used = true;
											}
										}
									}
									if (!used && !tag.equals(TagFromName.SpecificCharacterSet)) {	// Do NOT match on Specific Character Set; fixes [bugs.mrmf] (000220) Instance level query failing because matching on Specific Character Set
										addToMatchClause(matchBuffer,tableName,columnName,value);
									}
								}
							}
							else {
								slf4jlogger.trace("performQuery(): not within query level and not relational for identifier attribute {} with value {}",columnName,value);
								// above the level we want ... use unique key
								if (tag.equals(uniqueKey)) {
									slf4jlogger.trace("performQuery(): using as unique key {} entity for identifier attribute {} with value {}",ieDatabase,columnName,value);
									if (value != null && value.length() > 0 && !value.equals("NULL")) {
										addToMatchClause(matchBuffer,tableName,columnName,value);
									}
									else {
										setErrorStatus(ResponseStatus.IdentifierDoesNotMatchSOPClass,tag,"Unique key required above query level");
										return;
									}
								}
							}
							if (!tag.equals(TagFromName.SpecificCharacterSet)) {	// handled specially elsewhere ... not needed in result set and only at instance level anyway
								slf4jlogger.trace("performQuery(): using as return key {} entity for identifier attribute {} with value {}",ieDatabase,columnName,value);
								addToSelectClause(selectBuffer,tableName,columnName);
							}
							//else {
							slf4jlogger.trace("performQuery(): excluding as return key {} entity for identifier attribute {}",ieDatabase,columnName);
							//}
							addToFromClause(fromBuffer,tableName);
						}
					}
					ieDatabaseParent = ieDatabase;
					// ignore concatenations, otherwise returns no instances that are not concatentations :(
					ieDatabase = databaseInformationModel.getChildTypeForParent(ieDatabase,false/*ignore concatenations even if present in the model*/);
				}
				ieQuery = getChildTypeForParent(querySOPClassUID,ieQuery);
			}
			slf4jlogger.trace("performQuery(): join clause {}",joinBuffer);
			slf4jlogger.trace("performQuery(): match clause {}",matchBuffer);
			slf4jlogger.trace("performQuery(): selection clause {}",selectBuffer);
			slf4jlogger.trace("performQuery(): from clause {}",fromBuffer);
			StringBuffer b = new StringBuffer();
			b.append("SELECT ");
			b.append(selectBuffer);
			b.append(" FROM ");
			b.append(fromBuffer);
			if (joinBuffer.length() > 0 && matchBuffer.length() > 0) {
				b.append(" WHERE ");
				b.append(joinBuffer);
				b.append(" AND ");
				b.append(matchBuffer);
			}
			else if (joinBuffer.length() > 0) {
				b.append(" WHERE ");
				b.append(joinBuffer);
			}
			else if (matchBuffer.length() > 0) {
				b.append(" WHERE ");
				b.append(matchBuffer);
			}
			b.append(";");
		slf4jlogger.debug("performQuery(): query {}",b);
			try {
				databaseStatement = databaseInformationModel.createStatement();
				resultSet = databaseStatement.executeQuery(b.toString());
				resultSetMetaData = resultSet.getMetaData();
			}
			catch (SQLException e) {
				slf4jlogger.error("",e);
				setErrorStatus(ResponseStatus.UnableToProcess,null,e.getMessage());
			}
		}
	}
	
	private AttributeList makeDicomAttributeListFromResultSetRow(ResultSet r,ResultSetMetaData md) throws SQLException, DicomException {
		int numberOfColumns = md.getColumnCount();
		// First pass ... find a SpecificCharacterSet that can encode all the characters used in the entire response
		StringBuffer buf = new StringBuffer();
		for (int column=1; column<=numberOfColumns; ++column) {
			String value = r.getString(column);
			if (value != null && value.length() > 0) {
				buf.append(value);
			}
		}
		SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet(SpecificCharacterSet.getSetOfUnicodeBlocksUsedBy(buf.toString()));
		// Second pass ... build the attributes and attribute list
		AttributeList list = new AttributeList();
		{
			String valueToUse = specificCharacterSet.getValueToUseInSpecificCharacterSetAttribute();
			if (valueToUse != null && valueToUse.length() > 0) {
				Attribute aSpecificCharacterSet = new CodeStringAttribute(TagFromName.SpecificCharacterSet);
				aSpecificCharacterSet.addValue(valueToUse);
				list.put(aSpecificCharacterSet);
			}
			// else do NOT add SpecificCharacterSet if no character set extensions are in use, i.e., ASCII, as per PS 3.4
		}
		for (int column=1; column<=numberOfColumns; ++column) {
		slf4jlogger.trace("makeDicomAttributeListFromResultSetRow(): [{}] key = {} value = {}",r.getRow(),md.getColumnName(column),r.getString(column));
			Attribute attribute = makeDicomAttributeFromResultSetColumn(r,md,column,specificCharacterSet);
			if (attribute != null) {
				list.put(attribute);
			}
			// else there is no DICOM attribute in the information model's dictionary corresponding to the column in the table
			// e.g. ModalitiesInStudy
		}
		slf4jlogger.trace("makeDicomAttributeListFromResultSetRow(): {}",list.toString());
		return list;
	}
	
	private Attribute makeDicomAttributeFromResultSetColumn(ResultSet r,ResultSetMetaData md,int column,SpecificCharacterSet specificCharacterSet) throws SQLException, DicomException {
		String columnName = md.getColumnName(column);
		AttributeTag tag = databaseInformationModel.getAttributeTagFromDatabaseColumnName(columnName);
		Attribute attribute = null;
		if (tag != null) {
			byte[] vr = dictionary.getValueRepresentationFromTag(tag);
			slf4jlogger.trace("makeDicomAttributeFromResultSet(): {} {} sqlType = {} ({})",tag,databaseInformationModel.getDicomNameFromDatabaseColumnName(columnName),md.getColumnType(column),md.getColumnTypeName(column));
			slf4jlogger.trace("makeDicomAttributeFromResultSet(): specificCharacterSet = {}",specificCharacterSet);
			attribute = AttributeFactory.newAttribute(tag,vr,specificCharacterSet,true,2);
			String value = r.getString(column);
			if (value != null && value.length() > 0) {
				slf4jlogger.trace("makeDicomAttributeFromResultSet(): value={}",value);
				slf4jlogger.trace("makeDicomAttributeFromResultSet(): value={}",com.pixelmed.utils.HexDump.dump(value.getBytes()));
				attribute.addValue(value);		
			}
			slf4jlogger.trace("makeDicomAttributeFromResultSet(): {}",attribute);
		}
		return attribute;
	}

	private String makeForAllInstancesInSeriesStatement(AttributeTag wanted,String seriesInstanceUID) {
		StringBuffer b = new StringBuffer();
		b.append("SELECT ");
		b.append(instanceTableName);
		b.append(".");
		b.append(databaseInformationModel.getDatabaseColumnNameFromDicomTag(wanted));
		b.append(" FROM ");
		b.append(seriesTableName);
		b.append(",");
		b.append(instanceTableName);
		b.append(" WHERE ");
		b.append(instanceTableName);
		b.append(".");
		b.append(instanceParentReferenceColumnName);
		b.append(" = ");
		b.append(seriesTableName);
		b.append(".");
		b.append(seriesPrimaryKeyColumnName);
		b.append(" AND ");
		b.append(seriesTableName);
		b.append(".");
		b.append(seriesInstanceUIDColumnName);
		b.append(" = ");
		b.append(seriesInstanceUID);	// is already quoted
		b.append(";");
		return b.toString();
	}

	private String makeForAllSeriesInStudyStatement(AttributeTag wanted,String studyInstanceUID) {
		StringBuffer b = new StringBuffer();
		b.append("SELECT ");
		b.append(seriesTableName);
		b.append(".");
		b.append(databaseInformationModel.getDatabaseColumnNameFromDicomTag(wanted));
		b.append(" FROM ");
		b.append(studyTableName);
		b.append(",");
		b.append(seriesTableName);
		b.append(" WHERE ");
		b.append(seriesTableName);
		b.append(".");
		b.append(seriesParentReferenceColumnName);
		b.append(" = ");
		b.append(studyTableName);
		b.append(".");
		b.append(studyPrimaryKeyColumnName);
		b.append(" AND ");
		b.append(studyTableName);
		b.append(".");
		b.append(studyInstanceUIDColumnName);
		b.append(" = ");
		b.append(studyInstanceUID);	// is already quoted
		b.append(";");
		return b.toString();
	}

	private String makeForAllInstancesInStudyStatement(AttributeTag wanted,String studyInstanceUID) {
		StringBuffer b = new StringBuffer();
		b.append("SELECT ");
		b.append(instanceTableName);
		b.append(".");
		b.append(databaseInformationModel.getDatabaseColumnNameFromDicomTag(wanted));
		b.append(" FROM ");
		b.append(studyTableName);
		b.append(",");
		b.append(seriesTableName);
		b.append(",");
		b.append(instanceTableName);
		b.append(" WHERE ");
		b.append(seriesTableName);
		b.append(".");
		b.append(seriesParentReferenceColumnName);
		b.append(" = ");
		b.append(studyTableName);
		b.append(".");
		b.append(studyPrimaryKeyColumnName);
		b.append(" AND ");
		b.append(instanceTableName);
		b.append(".");
		b.append(instanceParentReferenceColumnName);
		b.append(" = ");
		b.append(seriesTableName);
		b.append(".");
		b.append(seriesPrimaryKeyColumnName);
		b.append(" AND ");
		b.append(studyTableName);
		b.append(".");
		b.append(studyInstanceUIDColumnName);
		b.append(" = ");
		b.append(studyInstanceUID);	// is already quoted
		b.append(";");
		return b.toString();
	}
	
	private Set findStringValuesInSet(String [] testValues,Set setOfValues) {
		Set returnValues = new TreeSet();
		if (testValues != null && setOfValues != null) {
			for (int i=0; i<testValues.length; ++i) {
				String value = testValues[i];
				if (setOfValues.contains(value)) {
					returnValues.add(value);
				}
			}
		}
		return returnValues;
	}

	public AttributeList next() {
		// should check that we never add the same attribute more than once (e.g. if occurs in more than one table ?) :(
		AttributeList responseIdentifier = null;
		try {
			while (responseIdentifier == null && resultSet != null && resultSet.next()) {		// loop in case we have discarded this match for some reason
			//if (resultSet != null && resultSet.next()) {
				responseIdentifier = makeDicomAttributeListFromResultSetRow(resultSet,resultSetMetaData);	// already includes SpecificCharacterSet as required
				Attribute aQueryRetrieveLevel = AttributeFactory.newAttribute(TagFromName.QueryRetrieveLevel,ValueRepresentation.CS,true,2);
				aQueryRetrieveLevel.addValue(queryRetrieveLevel);
				responseIdentifier.put(aQueryRetrieveLevel);
				if (additionalKeysToReturnAsZeroLength == null) {			// lazy instantiation on first next()
					additionalKeysToReturnAsZeroLength = new AttributeList();
					Iterator i = requestIdentifier.values().iterator();
					while (i.hasNext()) {
						Attribute a = (Attribute)(i.next());
						AttributeTag tag = a.getTag();
						if (responseIdentifier.get(tag) == null
						 && !tag.equals(TagFromName.SpecificCharacterSet)	// should be handled specially elsewhere
						 && tag.getElement() != 0x0000				// no group lengths; seen in query identifiers from GE AW
						 && !tag.equals(TagFromName.LengthToEnd)		// also seen in query identifiers from GE AW
						) {
							unsupportedOptionalKeysPresent = true;		// need this for correct status value, regardless
							if (includeUnsupportedOptionalKeysInResponseWithZeroLength) {
								byte[] vr = a.getVR();			// use actual VR supplied rather than dictionary.getValueRepresentationFromTag(tag)
								additionalKeysToReturnAsZeroLength.put(AttributeFactory.newAttribute(tag,vr,true,2));
							}
						}
					}
				}
				if (includeUnsupportedOptionalKeysInResponseWithZeroLength) {		// would not normally do this; unsupported optional keys shall not be in response
					// assert additionalKeysToReturnAsZeroLength != null;
					Iterator i = additionalKeysToReturnAsZeroLength.values().iterator();
					while (i.hasNext()) {
						Attribute a = (Attribute)(i.next());
						AttributeTag tag = a.getTag();
						// assert responseIdentifier.get(tag) == null;		// should never have got in additionalKeysToReturnAsZeroLength otherwise
						if (responseIdentifier.get(tag) == null) {
							responseIdentifier.put(a);
						}
					}
				}
				if (includeModalitiesInStudyIfRequested) {									// invokes both matching and return key semantics
					if (requestIdentifier.get(TagFromName.ModalitiesInStudy) != null		// i.e., only if requested
					 && responseIdentifier.get(TagFromName.ModalitiesInStudy) == null		// i.e., isn't in database table already (which it normally isn't)
					 && queryRetrieveLevel.equals("STUDY")) {
						try {
							Attribute aStudyInstanceUID = responseIdentifier.get(TagFromName.StudyInstanceUID);
							String vStudyInstanceUID = null;
							try {
								vStudyInstanceUID = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(aStudyInstanceUID);
							}
							catch (DicomException e) {
							}
							String query = makeForAllSeriesInStudyStatement(TagFromName.Modality,vStudyInstanceUID);
							slf4jlogger.debug("next(): ModalitiesInStudy query {}",query);
							Statement s = databaseInformationModel.createStatement();
							ResultSet r = databaseStatement.executeQuery(query);
							Set responseValues = new TreeSet();
							if (r != null) {
								while (r.next()) {
									String value = r.getString(modalityColumnName);
									slf4jlogger.trace("next(): for ModalitiesInStudy response, got Modality = {}",value);
									if (value != null && value.length() > 0/* && !value.equals("NULL")*/) {
										responseValues.add(value);
									}
								}
							}
							s.close();
							slf4jlogger.trace("next(): for ModalitiesInStudy, before matching responseValues = {}",responseValues);
							// perform matching, but only if necessary (that is, if a value rather than zero length was supplied in the request)
							String[] requestValues = Attribute.getStringValues(requestIdentifier,TagFromName.ModalitiesInStudy);
							if (requestValues != null && requestValues.length > 0) {
								if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("next(): matching request for ModalitiesInStudy = {}",StringUtilities.toString(requestValues));
								Set matchedResponseValues = findStringValuesInSet(requestValues,responseValues);
								slf4jlogger.trace("next(): matching result for ModalitiesInStudy = {}",matchedResponseValues);
								if (matchedResponseValues == null || matchedResponseValues.size() == 0) {
									responseIdentifier = null;					// no match, discard this response
									continue;									// and loop again
								}
								//else return all values, per PS 3.4 C.2.2.3, rather than returning only matched response values (i.e., do not set responseValues = matchedResponseValues)
							}
							// for matching or return key, add to response
							if (responseValues.size() > 0) {
								Attribute a = new CodeStringAttribute(TagFromName.ModalitiesInStudy);
								Iterator i = responseValues.iterator();
								while (i.hasNext()) {
									String value = (String)(i.next());
									a.addValue(value);
								}
								responseIdentifier.put(a);
							}
						}
						catch (SQLException e) {
							slf4jlogger.error("",e);
						}
					}
				}
				if (includeSOPClassesInStudyIfRequested) {									// invokes both matching and return key semantics
					assert responseIdentifier != null;
					if (requestIdentifier.get(TagFromName.SOPClassesInStudy) != null		// i.e., only if requested
					 && responseIdentifier.get(TagFromName.SOPClassesInStudy) == null		// i.e., isn't in database table already (which it normally isn't)
					 && queryRetrieveLevel.equals("STUDY")) {
						try {
							Attribute aStudyInstanceUID = responseIdentifier.get(TagFromName.StudyInstanceUID);
							String vStudyInstanceUID = null;
							try {
								vStudyInstanceUID = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(aStudyInstanceUID);
							}
							catch (DicomException e) {
							}
							String query = makeForAllInstancesInStudyStatement(TagFromName.SOPClassUID,vStudyInstanceUID);
							slf4jlogger.debug("next(): SOPClassesInStudy query {}",query);
							Statement s = databaseInformationModel.createStatement();
							ResultSet r = databaseStatement.executeQuery(query);
							Set responseValues = new TreeSet();
							if (r != null) {
								while (r.next()) {
									String value = r.getString(sopClassUIDColumnName);
									slf4jlogger.trace("next(): got SOP Class {}",value);
									if (value != null && value.length() > 0/* && !value.equals("NULL")*/) {
										responseValues.add(value);
									}
								}
							}
							s.close();
							slf4jlogger.trace("next(): for SOPClassesInStudy, before matching responseValues = {}",responseValues);
							// perform matching, but only if necessary (that is, if a value rather than zero length was supplied in the request)
							String[] requestValues = Attribute.getStringValues(requestIdentifier,TagFromName.SOPClassesInStudy);
							if (requestValues != null && requestValues.length > 0) {
								if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("next(): matching request for SOPClassesInStudy = {}",StringUtilities.toString(requestValues));	// defer call to StringUtilities.toString()
								Set matchedResponseValues = findStringValuesInSet(requestValues,responseValues);
								slf4jlogger.trace("next(): matching result for SOPClassesInStudy = {}",matchedResponseValues);
								if (matchedResponseValues == null || matchedResponseValues.size() == 0) {
									responseIdentifier = null;					// no match, discard this response
									continue;									// and loop again
								}
								//else return all values, per PS 3.4 C.2.2.3, rather than returning only matched response values (i.e., do not set responseValues = matchedResponseValues)
							}
							// for matching or return key, add to response
							if (responseValues.size() > 0) {
								Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassesInStudy);
								Iterator i = responseValues.iterator();
								while (i.hasNext()) {
									String value = (String)(i.next());
									a.addValue(value);
								}
								responseIdentifier.put(a);
							}
						}
						catch (SQLException e) {
							slf4jlogger.error("",e);
						}
					}
				}
				if (includeNumberOfStudyRelatedInstancesIfRequested) {								// return key; no matching
					if (requestIdentifier.get(TagFromName.NumberOfStudyRelatedInstances) != null	// i.e., only if requested
					 && responseIdentifier.get(TagFromName.NumberOfStudyRelatedInstances) == null	// i.e., isn't in database table already (which it normally isn't)
					 && queryRetrieveLevel.equals("STUDY")) {
						try {
							Attribute aStudyInstanceUID = responseIdentifier.get(TagFromName.StudyInstanceUID);
							String vStudyInstanceUID = null;
							try {
								vStudyInstanceUID = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(aStudyInstanceUID);
							}
							catch (DicomException e) {
							}
							String query = makeForAllInstancesInStudyStatement(TagFromName.SOPInstanceUID,vStudyInstanceUID);
							slf4jlogger.debug("next(): NumberOfStudyRelatedInstances query {}",query);
							Statement s = databaseInformationModel.createStatement();
							ResultSet r = databaseStatement.executeQuery(query);
							if (r != null) {
								//r.last();			// fails if result set type is not forward
								//int count = r.getRow();
								int count = 0;			// so keep it simple and do it the (perhaps) slow way
								while (r.next()) {
									++count;
								}
								Attribute a = new IntegerStringAttribute(TagFromName.NumberOfStudyRelatedInstances);
								a.addValue(count);
								responseIdentifier.put(a);
							}
							s.close();
						}
						catch (SQLException e) {
							slf4jlogger.error("",e);
						}
					}
				}
				if (includeNumberOfStudyRelatedSeriesIfRequested) {								// return key; no matching
					if (requestIdentifier.get(TagFromName.NumberOfStudyRelatedSeries) != null	// i.e., only if requested
					 && responseIdentifier.get(TagFromName.NumberOfStudyRelatedSeries) == null	// i.e., isn't in database table already (which it normally isn't)
					 && queryRetrieveLevel.equals("STUDY")) {
						try {
							Attribute aStudyInstanceUID = responseIdentifier.get(TagFromName.StudyInstanceUID);
							String vStudyInstanceUID = null;
							try {
								vStudyInstanceUID = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(aStudyInstanceUID);
							}
							catch (DicomException e) {
							}
							String query = makeForAllSeriesInStudyStatement(TagFromName.SeriesInstanceUID,vStudyInstanceUID);
							slf4jlogger.debug("next(): NumberOfStudyRelatedSeries query {}",query);
							Statement s = databaseInformationModel.createStatement();
							ResultSet r = databaseStatement.executeQuery(query);
							if (r != null) {
								//r.last();			// fails if result set type is not forward
								//int count = r.getRow();
								int count = 0;			// so keep it simple and do it the (perhaps) slow way
								while (r.next()) {
									++count;
								}
								Attribute a = new IntegerStringAttribute(TagFromName.NumberOfStudyRelatedSeries);
								a.addValue(count);
								responseIdentifier.put(a);
							}
							s.close();
						}
						catch (SQLException e) {
							slf4jlogger.error("",e);
						}
					}
				}
				if (includeNumberOfSeriesRelatedInstancesIfRequested) {								// return key; no matching
					if (requestIdentifier.get(TagFromName.NumberOfSeriesRelatedInstances) != null	// i.e., only if requested
					 && responseIdentifier.get(TagFromName.NumberOfSeriesRelatedInstances) == null	// i.e., isn't in database table already (which it normally isn't)
					 && queryRetrieveLevel.equals("SERIES")) {
						try {
							Attribute aSeriesInstanceUID = responseIdentifier.get(TagFromName.SeriesInstanceUID);
							String vSeriesInstanceUID = null;
							try {
								vSeriesInstanceUID = DicomDatabaseInformationModel.getQuotedEscapedSingleStringValueOrNull(aSeriesInstanceUID);
							}
							catch (DicomException e) {
							}
							String query = makeForAllInstancesInSeriesStatement(TagFromName.SOPInstanceUID,vSeriesInstanceUID);
							slf4jlogger.debug("next(): NumberOfSeriesRelatedInstances query {}",query);
							Statement s = databaseInformationModel.createStatement();
							ResultSet r = databaseStatement.executeQuery(query);
							if (r != null) {
								//r.last();			// fails if result set type is not forward
								//int count = r.getRow();
								int count = 0;			// so keep it simple and do it the (perhaps) slow way
								while (r.next()) {
									++count;
								}
								Attribute a = new IntegerStringAttribute(TagFromName.NumberOfSeriesRelatedInstances);
								a.addValue(count);
								responseIdentifier.put(a);
							}
							s.close();
						}
						catch (SQLException e) {
							slf4jlogger.error("",e);
						}
					}
				}
				slf4jlogger.debug("next(): response:\n{}",responseIdentifier.toString());
			}
		}
		catch (SQLException e) {
			slf4jlogger.error("",e);
		}
		catch (DicomException e) {
			slf4jlogger.error("",e);
		}
		return responseIdentifier;
	}
	
	public int getStatus() { return status; }
	
	public AttributeTagAttribute getOffendingElement() { return offendingElement; }
	
	public String getErrorComment() { return errorComment; }
	
	public void close() {
		resultSet=null;
		resultSetMetaData=null;
		if (databaseStatement != null) {
			try {
				databaseStatement.close();
			}
			catch (SQLException e) {
				slf4jlogger.error("",e);
			}
			databaseStatement=null;
		}
	}
	
	public boolean allOptionalKeysSuppliedWereSupported() {
		return !unsupportedOptionalKeysPresent;
	}
}



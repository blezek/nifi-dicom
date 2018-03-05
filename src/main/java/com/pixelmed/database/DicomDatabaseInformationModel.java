/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.*;
import com.pixelmed.query.QueryResponseGeneratorFactory;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.database.DicomDatabaseInformationModel DicomDatabaseInformationModel} class
 * is an abstract class that specializes {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}
 * by adding methods specific to a typical DICOM composite object information model.</p>
 *
 * <p>These methods include support for naming the standard DICOM information entities,
 * building insert statements using all the dictionary attributes for a
 * partcular DICOM information entity, and generating local primary keys.</p>
 *
 * @author	dclunie
 */
public abstract class DicomDatabaseInformationModel extends DatabaseInformationModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDatabaseInformationModel.java,v 1.35 2017/04/06 19:30:07 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomDatabaseInformationModel.class);

	private static final long SQL_INTEGER_MINIMUM = java.lang.Integer.MIN_VALUE;		// the hsqldb INTEGER type is allegedly a Java int
	private static final long SQL_INTEGER_MAXIMUM = java.lang.Integer.MAX_VALUE;

	protected static final String derivedStudyDateTimeColumnName = "PM_STUDYDATETIME";	// needs to be upper case
	protected static final String derivedSeriesDateTimeColumnName = "PM_SERIESDATETIME";	// needs to be upper case
	protected static final String derivedContentDateTimeColumnName = "PM_CONTENTDATETIME";	// needs to be upper case
	protected static final String derivedAcquisitionDateTimeColumnName = "PM_ACQUISITIONDATETIME";	// needs to be upper case
	protected static final String derivedLossyImageCompressionColumnName = "PM_LOSSYIMAGECOMPRESSION";	// needs to be upper case

	/**
	 * @param	databaseFileName
	 * @param	rootInformationEntity
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public DicomDatabaseInformationModel(String databaseFileName,InformationEntity rootInformationEntity,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,rootInformationEntity,dictionary);
	}

	/**
	 * @param	databaseFileName
	 * @param	rootInformationEntity
	 * @param	dictionary
	 * @param	databaseRootName
	 * @throws	DicomException
	 */
	public DicomDatabaseInformationModel(String databaseFileName,InformationEntity rootInformationEntity,DicomDictionary dictionary,String databaseRootName) throws DicomException {
		super(databaseFileName,rootInformationEntity,dictionary,databaseRootName);
	}

	/**
	 * @param	databaseFileName
	 * @param	rootInformationEntity
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public DicomDatabaseInformationModel(String databaseFileName,String databaseServerName,InformationEntity rootInformationEntity,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,databaseServerName,rootInformationEntity,dictionary);
	}

	/**
	 * @param	databaseFileName
	 * @param	rootInformationEntity
	 * @param	dictionary
	 * @param	databaseRootName
	 * @throws	DicomException
	 */
	public DicomDatabaseInformationModel(String databaseFileName,String databaseServerName,InformationEntity rootInformationEntity,DicomDictionary dictionary,String databaseRootName) throws DicomException {
		super(databaseFileName,databaseServerName,rootInformationEntity,dictionary,databaseRootName);
	}

	/**
	 * @param	ie
	 * @param	returnedAttributes
	 */
	public String getNametoDescribeThisInstanceOfInformationEntity(InformationEntity ie,Map returnedAttributes) {
		String s = ie.toString();
		if (ie == InformationEntity.INSTANCE) {
			String sopClassUID = (String)(returnedAttributes.get("SOPCLASSUID"));
			if (sopClassUID != null) {
				if      (SOPClass.isImageStorage(sopClassUID)) s="Image";
				else if (SOPClass.isStructuredReport(sopClassUID)) s="SR Document";
				else if (SOPClass.isWaveform(sopClassUID)) s="Waveform";
				else if (SOPClass.isSpectroscopy(sopClassUID)) s="Spectra";
				else if (SOPClass.isRawData(sopClassUID)) s="Raw Data";
			}
		}
		return s;
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the additional search columns derived
	 * from person name attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a select statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithPersonNameSearchColumnsForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
		TreeSet whatHasBeenAdded = new TreeSet();
		String tableName = getTableNameForInformationEntity(ie);
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			if (ie == dictionary.getInformationEntityFromTag(tag)) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				byte[] vr = a.getVR();										// use actual, not dictionary VR, in case was explicit
				if (columnName != null && vr != null && ValueRepresentation.isPersonNameVR(vr)) {
					String newColumnName = personNameCanonicalColumnNamePrefix + columnName + personNameCanonicalColumnNameSuffix;
					if (isAttributeUsedInTable(tableName,newColumnName) && !whatHasBeenAdded.contains(newColumnName)) {
						b.append(",");
						b.append(newColumnName);
						whatHasBeenAdded.add(newColumnName);
					}
					newColumnName = personNamePhoneticCanonicalColumnNamePrefix + columnName + personNamePhoneticCanonicalColumnNameSuffix;
					if (isAttributeUsedInTable(tableName,newColumnName) && !whatHasBeenAdded.contains(newColumnName)) {
						b.append(",");
						b.append(newColumnName);
						whatHasBeenAdded.add(newColumnName);
					}
				}
			}
		}
	}
	
	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the additional search columns derived
	 * from person name attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a select statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithPersonNameSearchValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
		TreeSet whatHasBeenAdded = new TreeSet();
		String tableName = getTableNameForInformationEntity(ie);
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			if (ie == dictionary.getInformationEntityFromTag(tag)) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				byte[] vr = a.getVR();										// use actual, not dictionary VR, in case was explicit
				if (columnName != null && vr != null && ValueRepresentation.isPersonNameVR(vr)) {
					String originalValue = a.getSingleStringValueOrNull();
					String canonicalValue = originalValue == null ? null : PersonNameAttribute.getCanonicalForm(originalValue);
					String phoneticCanonicalValue = canonicalValue == null ? null : PersonNameAttribute.getPhoneticName(canonicalValue);

					String newColumnName = personNameCanonicalColumnNamePrefix + columnName + personNameCanonicalColumnNameSuffix;
					if (isAttributeUsedInTable(tableName,newColumnName) && !whatHasBeenAdded.contains(newColumnName)) {
						b.append(",");
						String quotedValue = getQuotedValueOrNULL(canonicalValue);
						b.append(quotedValue);
						whatHasBeenAdded.add(newColumnName);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithPersonNameSearchValuesForSelectedInformationEntity(): "+newColumnName+"="+quotedValue);
					}

					newColumnName = personNamePhoneticCanonicalColumnNamePrefix + columnName + personNamePhoneticCanonicalColumnNameSuffix;
					if (isAttributeUsedInTable(tableName,newColumnName) && !whatHasBeenAdded.contains(newColumnName)) {
						b.append(",");
						String quotedValue = getQuotedValueOrNULL(phoneticCanonicalValue);
						b.append(quotedValue);
						whatHasBeenAdded.add(newColumnName);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithPersonNameSearchValuesForSelectedInformationEntity(): "+newColumnName+"="+quotedValue);
					}
				}
			}
		}
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>The default implementation adds values for all attributes that are present in the database tables
	 * and present in the instance.</p>
	 *
	 * <p>At the INSTANCE level, InstanceNumber is always added.</p>
	 *
	 * <p>At the INSTANCE level, ImageOrientationPatient within PlaneOrientationSequence within SharedFunctionalGroupsSequence is always added, if present.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithAttributeNamesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
		TreeSet whatHasBeenAdded = new TreeSet();
		if (ie == InformationEntity.INSTANCE) {
			b.append(",");
			b.append(localFileName);
			whatHasBeenAdded.add(localFileName);
			b.append(",");
			b.append(localFileReferenceTypeColumnName);
			whatHasBeenAdded.add(localFileReferenceTypeColumnName);
		}
		String tableName = getTableNameForInformationEntity(ie);
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			if (ie == dictionary.getInformationEntityFromTag(tag)) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				String columnType = getSQLTypeFromDicomValueRepresentation(a.getVR());					// use actual, not dictionary VR, in case was explicit
				if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)		// column type check is to be consistent with value list
				 && !whatHasBeenAdded.contains(columnName)) {								// do not add same attribute twice to same table
					b.append(",");
					b.append(columnName);
					whatHasBeenAdded.add(columnName);
				}
			}
		}
		if (ie == InformationEntity.INSTANCE) {
			AttributeTag tag = TagFromName.InstanceNumber;
			Attribute a = list.get(tag);
			if (a != null) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				String columnType = getSQLTypeFromDicomValueRepresentation(a.getVR());					// use actual, not dictionary VR, in case was explicit
				if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)		// column type check is to be consistent with value list
				 && !whatHasBeenAdded.contains(columnName)) {								// do not add same attribute twice to same table
					b.append(",");
					b.append(columnName);
					whatHasBeenAdded.add(columnName);
				}
			}
		}
		// special case ... in MR or CT multi-frame ImageOrientation is nested deeply ... pretend it is at the top level iff shared
		if (ie == InformationEntity.INSTANCE) {
			String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
			if (sopClassUID != null && SOPClass.isImageStorage(sopClassUID)) {
				SequenceAttribute aPlaneOrientationSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,
					TagFromName.SharedFunctionalGroupsSequence,TagFromName.PlaneOrientationSequence));
				Attribute aImageOrientationPatient = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
					aPlaneOrientationSequence,TagFromName.ImageOrientationPatient);
				if (aImageOrientationPatient != null && aImageOrientationPatient.getVM() == 6) {
					String columnName = getDatabaseColumnNameFromDicomTag(TagFromName.ImageOrientationPatient);
					String columnType = getSQLTypeFromDicomValueRepresentation(aImageOrientationPatient.getVR());		// use actual, not dictionary VR, in case was explicit
					if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)		// column type check is to be consistent with value list
					 && !whatHasBeenAdded.contains(columnName)) {
						b.append(",");
						b.append(columnName);
						whatHasBeenAdded.add(columnName);
					}
				}
			}
		}
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>The default implementation adds values for all attributes that are present in the database tables
	 * and present in the instance.</p>
	 *
	 * <p>At the INSTANCE level, InstanceNumber is always added.</p>
	 *
	 * <p>At the INSTANCE level, ImageOrientationPatient within PlaneOrientationSequence within SharedFunctionalGroupsSequence is always added, if present.</p>
	 *
	 * <p>At the INSTANCE level, the file reference type is left empty since unknown.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.database.DicomDatabaseInformationModel#extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer,AttributeList,InformationEntity,String,String) extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity()} instead
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @param	fileName	the local filename, which may be non-null for <code>INSTANCE</code> level insertions
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie,String fileName) throws DicomException {
		extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(b,list,ie,fileName,""/*fileReferenceType*/);
	}
	
	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>The default implementation adds values for all attributes that are present in the database tables
	 * and present in the instance.</p>
	 *
	 * <p>At the INSTANCE level, InstanceNumber is always added.</p>
	 *
	 * <p>At the INSTANCE level, ImageOrientationPatient within PlaneOrientationSequence within SharedFunctionalGroupsSequence is always added, if present.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @param	fileName	the local filename, which may be non-null for <code>INSTANCE</code> level insertions
	 * @param	fileReferenceType	"C" for copied (i.e., delete on purge), "R" for referenced (i.e., do not delete on purge)
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie,String fileName,String fileReferenceType) throws DicomException {
		TreeSet whatHasBeenAdded = new TreeSet();
		if (ie == InformationEntity.INSTANCE) {
			b.append(",\'");
			{
				// characters that need escaping may occur in file names, e.g., French names with apostrophes (001001)
				String escapedFileName = new String(fileName.trim());	// take care not to replace characters in original string, but in copy
				// only need to escape internal single-quote, which is the value string literal delimeter, NOT percent and underscore wildcard characters, since this is not a LIKE
				escapedFileName = escapedFileName.replace("\'","\'\'");
				b.append(escapedFileName);
			}
			b.append("\'");
			whatHasBeenAdded.add(localFileName);						// the name not the value
			b.append(",\'");
			b.append(fileReferenceType);
			b.append("\'");
			whatHasBeenAdded.add(localFileReferenceTypeColumnName);		// the name not the value
		}
		String tableName = getTableNameForInformationEntity(ie);
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			if (ie == dictionary.getInformationEntityFromTag(tag)) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				byte[] vr = a.getVR();
				String columnType = getSQLTypeFromDicomValueRepresentation(vr);					// use actual, not dictionary VR, in case was explicit
				if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)
				 && !whatHasBeenAdded.contains(columnName)) {								// do not add same attribute twice to same table
					b.append(",");
					String value = getQuotedEscapedSingleStringValueOrNull(a);
					if (ValueRepresentation.isPersonNameVR(vr)) {
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(): PN: "+columnName+" value was "+value);
						if (value != null && value.contains("^")) {
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(): PN: "+columnName+" value was "+value);
							value = value.replaceFirst("\\^+\'","\'");		// Trailing empty components are of no significance so should be treated as absent so that will match whether present or not
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(): PN: "+columnName+" value now "+value);
						}
					}
					b.append(value);
					whatHasBeenAdded.add(columnName);
				}
			}
		}
		if (ie == InformationEntity.INSTANCE) {
			AttributeTag tag = TagFromName.InstanceNumber;
			Attribute a = list.get(tag);
			if (a != null) {
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				String columnType = getSQLTypeFromDicomValueRepresentation(a.getVR());					// use actual, not dictionary VR, in case was explicit
				if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)
				 && !whatHasBeenAdded.contains(columnName)) {								// do not add same attribute twice to same table
					b.append(",");
					b.append(getQuotedEscapedSingleStringValueOrNull(a));
					whatHasBeenAdded.add(columnName);
				}
			}
		}
		// special case ... in MR multi-frame ImageOrientation is nested deeply ... pretend it is at the top level iff shared
		if (ie == InformationEntity.INSTANCE) {
			String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
			if (sopClassUID != null && SOPClass.isImageStorage(sopClassUID)) {
				SequenceAttribute aPlaneOrientationSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,
					TagFromName.SharedFunctionalGroupsSequence,TagFromName.PlaneOrientationSequence));
				Attribute aImageOrientationPatient = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
					aPlaneOrientationSequence,TagFromName.ImageOrientationPatient);
				if (aImageOrientationPatient != null && aImageOrientationPatient.getVM() == 6) {
					String columnName = getDatabaseColumnNameFromDicomTag(TagFromName.ImageOrientationPatient);
					String columnType = getSQLTypeFromDicomValueRepresentation(aImageOrientationPatient.getVR());		// use actual, not dictionary VR, in case was explicit
					if (columnName != null && columnType != null && isAttributeUsedInTable(tableName,columnName)
					 && !whatHasBeenAdded.contains(columnName)) {								// do not add same attribute twice to same table
					 	b.append(",");
						b.append(getQuotedEscapedSingleStringValueOrNull(aImageOrientationPatient));
						whatHasBeenAdded.add(columnName);
					}
				}
			}
		}
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the derived attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithDerivedAttributeNamesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
		if (ie == InformationEntity.STUDY) {
			b.append(",");
			b.append(derivedStudyDateTimeColumnName);
		}
		else if (ie == InformationEntity.SERIES) {
			b.append(",");
			b.append(derivedSeriesDateTimeColumnName);
		}
		else if (ie == InformationEntity.INSTANCE) {
			b.append(",");
			b.append(derivedContentDateTimeColumnName);
			b.append(",");
			b.append(derivedAcquisitionDateTimeColumnName);
			b.append(",");
			b.append(derivedLossyImageCompressionColumnName);
		}
	}
	
	public java.sql.Timestamp getTimestampFromDate(java.util.Date javaDate) {
		return new java.sql.Timestamp(javaDate.getTime());	// intermediary is "number of milliseconds since January 1, 1970, 00:00:00 GMT"
	}
	
	public java.util.Date getDateFromDicomDateAndTime(String date,String time,String timezone) {
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): date="+date+" time="+time+" timezone="+timezone);
		java.util.Date value = null;
		String dateFormatString = null;
		String dateTime = date;
		if (date != null) {
			if (date.length() == 8) {
				dateFormatString = "yyyyMMdd";
			}
			else if (date.length() == 10 && date.indexOf('.') != -1) {
				dateFormatString = "yyyy.MM.dd";
			}
		}
		String timeFormatString = "";
		String timezoneFormatString = "";
		int l;
		if (time != null && (l=time.length()) > 0) {
			if (time.indexOf(':') == -1) {
				//timeFormatString="HHmmss.SSSSSS";
				if (l >= 2) timeFormatString="HH";
				if (l >= 4) timeFormatString=timeFormatString+"mm";
				if (l >= 6) timeFormatString=timeFormatString+"ss";
				if (l > 7 && time.indexOf('.') == 6) {
					// the SimpleDateFormat expects exactly 3 digits for milliseconds when S is used
					// whereas DICOM may have 0 to 6
					int numberOfFractionalSecondDigits = l-7;
					if (numberOfFractionalSecondDigits < 3) {
						while (numberOfFractionalSecondDigits++ < 3) time=time+"0";
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): padded time to="+time);
					}
					else if (numberOfFractionalSecondDigits > 3) {
						time = time.substring(0,10);						// i.e., just truncate
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): truncated time to="+time);
					}
					timeFormatString=timeFormatString+".SSS";
				}
			}
			else {
				//timeFormatString="HH:mm:ss.SSSSSS";
				if (l >= 2) timeFormatString="HH";
				if (l >= 5) timeFormatString=timeFormatString+":mm";
				if (l >= 7) timeFormatString=timeFormatString+":ss";
				if (l > 9 && time.indexOf('.') == 8) {
					// the SimpleDateFormat expects exactly 3 digits for milliseconds when S is used
					// whereas DICOM may have 0 to 6
					int numberOfFractionalSecondDigits = l-9;
					if (numberOfFractionalSecondDigits < 3) {
						while (numberOfFractionalSecondDigits++ < 3) time=time+"0";
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): padded time to="+time);
					}
					else if (numberOfFractionalSecondDigits > 3) {
						time = time.substring(0,12);						// i.e., just truncate
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): truncated time to="+time);
					}
					timeFormatString=timeFormatString+".SSS";
				}
			}
			dateTime=dateTime+time;					// delay concatenation until this point, since time may have changed to fix milliseconds
			if (timezone != null && timezone.length() > 0) {
				dateTime=dateTime+timezone;
				timezoneFormatString = "Z";
			}
		}
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): dateTime="+dateTime);
		if (dateFormatString != null) {
			String formatString = dateFormatString+timeFormatString+timezoneFormatString;
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): formatString="+formatString);
			DateFormat formatter = new SimpleDateFormat(formatString);
			formatter.setLenient(false);	// This is important, othwerwise bad date like "99999999" will parse OK, return ridiculous value, and SQL insert statements will fail later (000679)
			value = formatter.parse(dateTime,new ParsePosition(0));
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): value="+value);
		}
		return value;
	}

	public java.util.Date getDateFromDicomDateAndTime(String dateTime) {
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(dateTime): attribute value dateTime="+dateTime);
		java.util.Date value = null;
		String formatString = null;
		int l;
		if (dateTime != null && (l=dateTime.length()) > 0) {
			if (l >= 4) formatString="yyyy";
			if (l >= 6) formatString=formatString+"MM";
			if (l >= 8) formatString=formatString+"dd";
			if (l >= 10) formatString=formatString+"HH";
			if (l >= 12) formatString=formatString+"mm";
			if (l >= 14) formatString=formatString+"ss";
			if (l > 15) {
				String base = dateTime.substring(0,15);
				int fractionalSecondStart = dateTime.indexOf('.');
				int timezoneStart = dateTime.indexOf('+');
				if (timezoneStart == -1) timezoneStart = dateTime.indexOf('-');
				String fractionalSecondDigits = "";
				if (fractionalSecondStart == 14) {
					// the SimpleDateFormat expects exactly 3 digits for milliseconds when S is used
					// whereas DICOM may have 0 to 6
					int endOfFractionalSecondDigits = timezoneStart > 15 ? timezoneStart : l;
					fractionalSecondDigits = dateTime.substring(15,endOfFractionalSecondDigits);
					int numberOfFractionalSecondDigits = fractionalSecondDigits.length();
					if (numberOfFractionalSecondDigits < 3) {
						while (numberOfFractionalSecondDigits++ < 3) fractionalSecondDigits=fractionalSecondDigits+"0";
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): padded fractionalSecondDigits to="+fractionalSecondDigits);
					}
					else {
						fractionalSecondDigits = fractionalSecondDigits.substring(0,3);
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): truncated fractionalSecondDigits to="+fractionalSecondDigits);
					}
					formatString=formatString+".SSS";
				}
				String timezone = "";					// timezone may be present even if fractional seconds are not
				if (timezoneStart != -1) {
					timezone = dateTime.substring(timezoneStart);
					formatString = formatString+"Z";
				}
				dateTime=base+fractionalSecondDigits+timezone;		// reassemble
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(date,time,timezone): truncated reassembled dateTime="+dateTime);
			}
//System.err.println("DicomDatabaseInformationModel.getDateFromDicomDateAndTime(dateTime): formatString="+formatString);
			DateFormat formatter = new SimpleDateFormat(formatString);
			formatter.setLenient(false);	// This is important, othwerwise bad date like "99999999" will parse OK, return ridiculous value, and SQL insert statements will fail later (000679)
			value = formatter.parse(dateTime,new ParsePosition(0));
		}
		return value;
	}

	public java.sql.Timestamp getTimestampFromDicomDateAndTime(String date,String time,String timezone) {
		java.util.Date dateTime = getDateFromDicomDateAndTime(date,time,timezone);
//System.err.println("DicomDatabaseInformationModel.getTimestampFromDicomDateAndTime(date,time,timezone): java.util.Date="+dateTime);
		java.sql.Timestamp timestamp = dateTime == null ? null : getTimestampFromDate(dateTime);
//System.err.println("DicomDatabaseInformationModel.getTimestampFromDicomDateAndTime(date,time,timezone): java.sql.Timestamp="+timestamp);
		return timestamp;
	}

	public java.sql.Timestamp getTimestampFromDicomDateAndTime(String sdatetime) {
		java.util.Date dateTime = getDateFromDicomDateAndTime(sdatetime);
//System.err.println("DicomDatabaseInformationModel.getTimestampFromDicomDateAndTime(sdatetime): java.util.Date="+dateTime);
		java.sql.Timestamp timestamp = dateTime == null ? null : getTimestampFromDate(dateTime);
//System.err.println("DicomDatabaseInformationModel.getTimestampFromDicomDateAndTime(sdatetime): java.sql.Timestamp="+timestamp);
		return timestamp;
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the derived attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected void extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
		String vTimezoneOffsetFromUTC = Attribute.getSingleStringValueOrNull(list,TagFromName.TimezoneOffsetFromUTC);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vTimezoneOffsetFromUTC="+vTimezoneOffsetFromUTC);
		// add study date and time as PM_STUDYDATETIME
		if (ie == InformationEntity.STUDY) {
			String vStudyDate = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vStudyDate="+vStudyDate);
			String vStudyTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyTime);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vStudyTime="+vStudyTime);
			java.sql.Timestamp studyDateTime = getTimestampFromDicomDateAndTime(vStudyDate,vStudyTime,vTimezoneOffsetFromUTC);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): studyDateTime="+studyDateTime);
			if (studyDateTime == null) {
				b.append(",NULL");
			}
			else {
				b.append(",\'");
				b.append(studyDateTime);
				b.append("\'");
			}
		}
		else if (ie == InformationEntity.SERIES) {
			String vSeriesDate = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesDate);
			String vSeriesTime = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesTime);
			java.sql.Timestamp seriesDateTime = getTimestampFromDicomDateAndTime(vSeriesDate,vSeriesTime,vTimezoneOffsetFromUTC);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): seriesDateTime="+seriesDateTime);
			if (seriesDateTime == null) {
				b.append(",NULL");
			}
			else {
				b.append(",\'");
				b.append(seriesDateTime);
				b.append("\'");
			}
		}
		else if (ie == InformationEntity.INSTANCE) {
			String vContentDate = Attribute.getSingleStringValueOrNull(list,TagFromName.ContentDate);
			String vContentTime = Attribute.getSingleStringValueOrNull(list,TagFromName.ContentTime);
			java.sql.Timestamp contentDateTime = getTimestampFromDicomDateAndTime(vContentDate,vContentTime,vTimezoneOffsetFromUTC);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): contentDateTime="+contentDateTime);
			if (contentDateTime == null) {
				b.append(",NULL");
			}
			else {
				b.append(",\'");
				b.append(contentDateTime);
				b.append("\'");
			}

			String vAcquisitionDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.AcquisitionDateTime);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vAcquisitionDateTime="+vAcquisitionDateTime);
			String vAcquisitionDate = Attribute.getSingleStringValueOrNull(list,TagFromName.AcquisitionDate);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vAcquisitionDate="+vAcquisitionDate);
			String vAcquisitionTime = Attribute.getSingleStringValueOrNull(list,TagFromName.AcquisitionTime);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): vAcquisitionTime="+vAcquisitionTime);
			java.sql.Timestamp acquisitionDateTime = vAcquisitionDateTime == null
				? getTimestampFromDicomDateAndTime(vAcquisitionDate,vAcquisitionTime,vTimezoneOffsetFromUTC)
				: getTimestampFromDicomDateAndTime(vAcquisitionDateTime);
//System.err.println("DicomDatabaseInformationModel.extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(): acquisitionDateTime="+acquisitionDateTime);
			if (acquisitionDateTime == null) {
				b.append(",NULL");
			}
			else {
				b.append(",\'");
				b.append(acquisitionDateTime);
				b.append("\'");
			}
			
			{
				b.append(",\'");
				b.append(LossyImageCompression.hasEverBeenLossyCompressed(list) ? 1 : 0);
				b.append("\'");
			}	
		}
	}
	
	/**
	 * <p>Extend a SQL CREATE TABLE statement in the process of being constructed with any derived attributes (columns) that the model requires.</p>
	 *
	 * <p>Called when creating the tables for a new database.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.
	 * Defaults to adding no extra columns if not overridden (i.e. it is not abstract).</p>
	 *
	 * <p> For example, there may be dates and times derived from DICOM attributes.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a create table statement is being constructed
	 */
	protected void extendCreateStatementStringWithDerivedAttributes(StringBuffer b,InformationEntity ie) {
		if (ie == InformationEntity.STUDY) {
			b.append(",");
			b.append(derivedStudyDateTimeColumnName);
			b.append(" TIMESTAMP(6)");		// hsqldb supports precisions of 0 or 6 only, with 6 the default if unspecified
		}
		else if (ie == InformationEntity.SERIES) {
			b.append(",");
			b.append(derivedSeriesDateTimeColumnName);
			b.append(" TIMESTAMP(6)");
		}
		else if (ie == InformationEntity.INSTANCE) {
			b.append(",");
			b.append(derivedContentDateTimeColumnName);
			b.append(" TIMESTAMP(6)");
			b.append(",");
			b.append(derivedAcquisitionDateTimeColumnName);
			b.append(" TIMESTAMP(6)");
			b.append(",");
			b.append(derivedLossyImageCompressionColumnName);
			b.append(" INTEGER");
		}
	}

	/**
	 * @param	ie
	 */
	protected String createPrimaryKeyForSelectedInformationEntity(InformationEntity ie) {
		//return new java.rmi.server.ObjID().toString();
		return Long.toString(new java.util.Date().getTime()) + Long.toString((long)(Math.random()*100000000));
	}

	/**
	 * <p>Make a quoted string value suitable for using in a SQL statement from a (possibly null or empty) string.</p>
	 *
	 * <p>No attempt at escaping specially characters is made ... this is assumed already done.</p>
	 *
	 * <p>Leading or trailing whitespace is trimmed.</p>
	 *
	 * @param	value	the string, which may be null or zero length
	 * @return			the quoted string value of the attribute, or the (unquoted) string NULL if the value is null or zero length
	 */
	public static String getQuotedValueOrNULL(String value) {
		StringBuffer b = new StringBuffer();
		if (value == null || value.trim().length() == 0) {
			b.append("NULL");
		}
		else {
			b.append("\'");
			b.append(value.trim());
			b.append("\'");
		}
		return b.toString();
	}

	/**
	 * <p>Make a quoted string value suitable for using in a SQL statement from a DICOM attribute.</p>
	 *
	 * <p>Special characters should be escaped (actually they are just replaced with a hyphen).</p>
	 *
	 * <p>Multiple values are collapsed and separated by the usual DICOM backslash delimiter character (which doesn't bother SQL).</p>
	 *
	 * @param	a	the DICOM attribute, which may be null, zero length or multi-valued
	 * @return		the quoted string value of the attribute, or the (unquoted) string NULL if attribute is absent, has no value or is zero length
	 * @throws		DicomException
	 */
	public static String getQuotedEscapedSingleStringValueOrNull(Attribute a) throws DicomException {
		return getQuotedSingleStringValueOrNull(a,true/*escapeSpecialCharacters*/);
	}

	/**
	 * <p>Make a quoted string value suitable for using in a SQL statement from a DICOM attribute.</p>
	 *
	 * <p>Does NOT escape special characters.</p>
	 *
	 * <p>Multiple values are collapsed and separated by the usual DICOM backslash delimiter character (which doesn't bother SQL).</p>
	 *
	 * @param	a									the DICOM attribute, which may be null, zero length or multi-valued
	 * @return		the quoted string value of the attribute, or the (unquoted) string NULL if attribute is absent, has no value or is zero length
	 * @throws		DicomException
	 */
	public static String getQuotedUnescapedSingleStringValueOrNull(Attribute a) throws DicomException {
		return getQuotedSingleStringValueOrNull(a,false/*escapeSpecialCharacters*/);
	}
	
	/**
	 * <p>Make a quoted string value suitable for using in a SQL statement from a DICOM attribute.</p>
	 *
	 * <p>Special characters may be escaped (actually they are just replaced with a hyphen).</p>
	 *
	 * <p>Multiple values are collapsed and separated by the usual DICOM backslash delimiter character (which doesn't bother SQL).</p>
	 *
	 * <p>Leading or trailing whitespace in each value is trimmed.</p>
	 *
	 * @param	a							the DICOM attribute, which may be null, zero length or multi-valued
	 * @param	escapeSpecialCharacters		whether or not to escape special characters (currently only single quote)
	 * @return								the quoted string value of the attribute, or the (unquoted) string NULL if attribute is absent, has no value or is zero length
	 * @throws							DicomException
	 */
	public static String getQuotedSingleStringValueOrNull(Attribute a,boolean escapeSpecialCharacters) throws DicomException {
		StringBuffer b = new StringBuffer();
		if (a == null) {		// do not need to check for zero length equivalent to null, since will fix at end, and need to handle whitespace trimmed values the same as empty values anyway
			b.append("NULL");
		}
		else {
			String sqlType = getSQLTypeFromDicomValueRepresentation(a.getVR());		// will be null if SQ (000671)
			if (sqlType != null) {
				String v[]=a.getStringValues();
				if (v == null || v.length == 0) {
					b.append("NULL");
				}
				else if (sqlType.equals("INTEGER")) {		// only VM of 1 allowed - use 1st if multiple values; also check range (e.g. some Philips Series Numbers violate IS range)
					if (v[0] == null || v[0].trim().length() == 0) {
						b.append("NULL");		// need to check string, since a string numeric that is empty will return 0 in Attribute.getLongValues(a), and we prefer NULL to 0
					}
					else {
						long[] iv = Attribute.getLongValues(a);
						if (iv != null && iv.length > 0) {
							if (iv[0] < SQL_INTEGER_MINIMUM || iv[0] > SQL_INTEGER_MAXIMUM) {
//System.err.println("Suppressing too long integer value "+Long.toString(iv[0]));
								b.append("NULL");
							}
							else {
								String s = Long.toString(iv[0]);
								b.append(s);
							}
						}
						else {
							b.append("NULL");	// no (valid) numeric value
						}
					}
				}
				else if (sqlType.equals("REAL")) {		// only VM of 1 allowed - use 1st if multiple values
					if (v[0] == null || v[0].trim().length() == 0) {
						b.append("NULL");		// need to check string, since a string numeric that is empty will return 0 in Attribute.getLongValues(a), and we prefer NULL to 0
					}
					else {
						double[] dv = Attribute.getDoubleValues(a);
						if (dv != null && dv.length > 0) {
							String s = Double.toString(dv[0]);
							b.append(s);
						}
						else {
							b.append("NULL");	// no (valid) numeric value
						}
					}
				}
				else {
					b.append("\'");
					for (int i=0; i<v.length; ++i) {
						if (i > 0) b.append("\\");
						String s = new String(v[i]).trim();	// take care not to replace characters in original string, but in copy
//System.err.println("DicomDatabaseInformationModel.getQuotedSingleStringValueOrNull(): processing trimmed string value"+s);
						// escape internal single-quote, percent and underscore ... :(
						if (s != null) {
							if (escapeSpecialCharacters) {
//System.err.println("DicomDatabaseInformationModel.getQuotedSingleStringValueOrNull(): replacing single quotes");
								s = s.replace("\'","\'\'");
								// do not escape special matching characters ... this is handled in LIKE clause only when needed (000575)
							}
							b.append(s);
						}
					}
					b.append("\'");
				}
			}
		}
		String str = b.toString();
		if (str.equals("\'\'")) {
//System.err.println("DicomDatabaseInformationModel.getQuotedSingleStringValueOrNull(): replacing empty quoted trimmed value with NULL");
			str = "NULL";
		}
//System.err.println("DicomDatabaseInformationModel.getQuotedSingleStringValueOrNull(): returning "+str);
		return str;
	}

	/**
	 * <p>Get a factory to manufacture a query response generator capable of performing a query and returning the results.</p>
	 *
	 * @return			the response generator factory
	 */
	public QueryResponseGeneratorFactory getQueryResponseGeneratorFactory() {
//System.err.println("DicomDatabaseInformationModel.getQueryResponseGenerator():");
		return new DicomDatabaseQueryResponseGeneratorFactory(this);
	}

	/**
	 * <p>Get a factory to manufacture a query response generator capable of performing a query and returning the results.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getQueryResponseGeneratorFactory()} instead.
	 * @param	debugLevel		ignored
	 * @return			the response generator factory
	 */
	public QueryResponseGeneratorFactory getQueryResponseGeneratorFactory(int debugLevel) {
		slf4jlogger.warn("getQueryResponseGeneratorFactory(): Debug level supplied as argument ignored");
		return getQueryResponseGeneratorFactory();
	}
	
	/**
	 * <p>Get a factory to manufacture a retrieve response generator capable of performing a retrieve and returning the results.</p>
	 *
	 * @return					the response generator factory
	 */
	public RetrieveResponseGeneratorFactory getRetrieveResponseGeneratorFactory() {
//System.err.println("DicomDatabaseInformationModel.getRetrieveResponseGenerator():");
		return new DicomDatabaseRetrieveResponseGeneratorFactory(this);
	}
	
	/**
	 * <p>Get a factory to manufacture a retrieve response generator capable of performing a retrieve and returning the results.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getRetrieveResponseGeneratorFactory()} instead.
	 * @param	debugLevel		ignored
	 * @return					the response generator factory
	 */
	public RetrieveResponseGeneratorFactory getRetrieveResponseGeneratorFactory(int debugLevel) {
//System.err.println("DicomDatabaseInformationModel.getRetrieveResponseGenerator():");
		slf4jlogger.warn("getRetrieveResponseGeneratorFactory(): Debug level supplied as argument ignored");
		return getRetrieveResponseGeneratorFactory();
	}

}


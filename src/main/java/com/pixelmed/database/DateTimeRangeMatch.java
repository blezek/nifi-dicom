/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.dicom.ValueRepresentation;

import java.util.HashSet;
import java.util.Set;

public class DateTimeRangeMatch {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DateTimeRangeMatch.java,v 1.6 2017/01/24 10:50:34 dclunie Exp $";

	private static final String earliestPossibleSQLDate = "19000101";
	private static final String latestPossibleSQLDate = "20990101";
	private static final String earliestPossibleSQLTime = "000000";
	private static final String latestPossibleSQLTime = "235959.999999";
	private static final String earliestPossibleSQLDateTime = "19000101000000";
	private static final String latestPossibleSQLDateTime = "20990101235959.999999";
	
	private static final String convertToSQLTimestampFormat(String value) {
		// given yyyymmddhh[mm[ss[fffffffff]]]
		// make 'yyyy-mm-dd hh:mm:ss.fffffffff'
		StringBuffer b = new StringBuffer();
			b.append("\'");
		int length = value.length();
		if (length >= 4) {
			b.append(value.substring(0,4));
		}
		if (length >= 6) {
			b.append("-");
			b.append(value.substring(4,6));
		}
		if (length >= 8) {
			b.append("-");
			b.append(value.substring(6,8));
		}
		if (length >= 10) {
			b.append(" ");
			b.append(value.substring(8,10));
		}
		if (length >= 12) {
			b.append(":");
			b.append(value.substring(10,12));
		}
		if (length >= 14) {
			b.append(":");
			b.append(value.substring(12,14));
		}
		if (length >= 14) {
			// the decimal point is already there
			b.append(value.substring(14,length));
		}
		b.append("\'");
		return b.toString();
	}

	private class RangeMatch {
		String lowerValue;
		String upperValue;
		
		RangeMatch(String value) {
			lowerValue = null;
			upperValue = null;
		
			if (value != null) {
				int indexOfHyphen = value.indexOf("-");
				int length = value.length();
				if (indexOfHyphen == 0) {
					upperValue = value.substring(1,length);
				}
				else if (indexOfHyphen == length-1) {
					lowerValue = value.substring(0,length-1);
				}
				else if (indexOfHyphen != -1) {
					lowerValue = value.substring(0,indexOfHyphen);
					upperValue = value.substring(indexOfHyphen+1,length);
				}
				
				if (lowerValue != null && lowerValue.length() == 0) {
					lowerValue = null;
				}
				if (upperValue != null && upperValue.length() == 0) {
					upperValue = null;
				}
				
				if (lowerValue == null && upperValue == null) {
					lowerValue = upperValue = value;	// i.e., exact match
				}
			}
		}
		
		RangeMatch(RangeMatch dateRangeMatch,RangeMatch timeRangeMatch) {
			if (dateRangeMatch == null || dateRangeMatch.lowerValue == null) {
				if (timeRangeMatch == null || timeRangeMatch.lowerValue == null) {
					lowerValue = earliestPossibleSQLDate + earliestPossibleSQLTime;
				}
				else {
					lowerValue = earliestPossibleSQLDate + timeRangeMatch.lowerValue;
				}
			}
			else {
				// assert dateRangeMatch.lowerValue.length() == 8
				if (timeRangeMatch == null || timeRangeMatch.lowerValue == null) {
					lowerValue = dateRangeMatch.lowerValue + earliestPossibleSQLTime;
				}
				else {
					lowerValue = dateRangeMatch.lowerValue + timeRangeMatch.lowerValue;
				}
			}
			
			if (dateRangeMatch == null || dateRangeMatch.upperValue == null) {
				if (timeRangeMatch == null || timeRangeMatch.upperValue == null) {
					upperValue = latestPossibleSQLDate + latestPossibleSQLTime;
				}
				else {
					upperValue = latestPossibleSQLDate + timeRangeMatch.upperValue;
				}
			}
			else {
				// assert dateRangeMatch.upperValue.length() == 8
				if (timeRangeMatch == null || timeRangeMatch.upperValue == null) {
					upperValue = dateRangeMatch.upperValue + latestPossibleSQLTime;
				}
				else {
					upperValue = dateRangeMatch.upperValue + timeRangeMatch.upperValue;
				}
			}
		}
	}
	
	boolean useTimeStampRatherThanDicomAttribute;
	
	RangeMatch rangeMatch;

	String matchColumnName;
	
	Set alreadyUsed;

	private DateTimeRangeMatch(AttributeTag tag,AttributeList requestIdentifier,String columnName) {
		AttributeTag dateMatchTag = null;
		AttributeTag timeMatchTag = null;
		AttributeTag datetimeMatchTag = null;
		
		matchColumnName = null;
		
		alreadyUsed = new HashSet();
		
		if (tag.equals(TagFromName.StudyDate) || tag.equals(TagFromName.StudyTime)) {
			dateMatchTag = TagFromName.StudyDate;
			timeMatchTag = TagFromName.StudyTime;
			matchColumnName = DicomDatabaseInformationModel.derivedStudyDateTimeColumnName;
			alreadyUsed.add(TagFromName.StudyDate);
			alreadyUsed.add(TagFromName.StudyTime);
		}
		else if (tag.equals(TagFromName.SeriesDate) || tag.equals(TagFromName.SeriesTime)) {
			dateMatchTag = TagFromName.SeriesDate;
			timeMatchTag = TagFromName.SeriesTime;
			matchColumnName = DicomDatabaseInformationModel.derivedSeriesDateTimeColumnName;
			alreadyUsed.add(TagFromName.SeriesDate);
			alreadyUsed.add(TagFromName.SeriesTime);
		}
		else if (tag.equals(TagFromName.ContentDate) || tag.equals(TagFromName.ContentTime)) {
			dateMatchTag = TagFromName.ContentDate;
			timeMatchTag = TagFromName.ContentTime;
			matchColumnName = DicomDatabaseInformationModel.derivedContentDateTimeColumnName;
			alreadyUsed.add(TagFromName.ContentDate);
			alreadyUsed.add(TagFromName.ContentTime);
		}
		else if (tag.equals(TagFromName.AcquisitionDateTime)) {
			datetimeMatchTag = TagFromName.AcquisitionDateTime;
			matchColumnName = DicomDatabaseInformationModel.derivedAcquisitionDateTimeColumnName;
			alreadyUsed.add(TagFromName.AcquisitionDateTime);
			alreadyUsed.add(TagFromName.AcquisitionDate);
			alreadyUsed.add(TagFromName.AcquisitionTime);
		}
		else if (tag.equals(TagFromName.AcquisitionDate) || tag.equals(TagFromName.AcquisitionTime)) {
			dateMatchTag = TagFromName.AcquisitionDate;
			timeMatchTag = TagFromName.AcquisitionTime;
			matchColumnName = DicomDatabaseInformationModel.derivedAcquisitionDateTimeColumnName;
			alreadyUsed.add(TagFromName.AcquisitionDateTime);
			alreadyUsed.add(TagFromName.AcquisitionDate);
			alreadyUsed.add(TagFromName.AcquisitionTime);
		}
		
		String dateValue     = dateMatchTag     == null ? null : Attribute.getSingleStringValueOrNull(requestIdentifier,dateMatchTag);
		String timeValue     = timeMatchTag     == null ? null : Attribute.getSingleStringValueOrNull(requestIdentifier,timeMatchTag);
		String datetimeValue = datetimeMatchTag == null ? null : Attribute.getSingleStringValueOrNull(requestIdentifier,datetimeMatchTag);

		if (datetimeValue != null && datetimeValue.length() > 0) {
			rangeMatch = new RangeMatch(datetimeValue);
			useTimeStampRatherThanDicomAttribute = true;
		}
		else {
			if (dateValue != null) {
				if (timeValue != null) {
					useTimeStampRatherThanDicomAttribute = true;
					rangeMatch = new RangeMatch(new RangeMatch(dateValue),new RangeMatch(timeValue));
					rangeMatch.lowerValue = convertToSQLTimestampFormat(rangeMatch.lowerValue);
					rangeMatch.upperValue = convertToSQLTimestampFormat(rangeMatch.upperValue);
				}
				else {
					useTimeStampRatherThanDicomAttribute = false;
					rangeMatch = new RangeMatch(dateValue);
					if (rangeMatch.lowerValue == null) {
						rangeMatch.lowerValue = earliestPossibleSQLDate;
					}
					if (rangeMatch.upperValue == null) {
						rangeMatch.upperValue = latestPossibleSQLDate;
					}
					matchColumnName = columnName;
				}
			}
			else {
				if (timeValue != null) {
					useTimeStampRatherThanDicomAttribute = false;
					rangeMatch = new RangeMatch(timeValue);
					if (rangeMatch.lowerValue == null) {
						rangeMatch.lowerValue = earliestPossibleSQLTime;
					}
					if (rangeMatch.upperValue == null) {
						rangeMatch.upperValue = latestPossibleSQLTime;
					}
					matchColumnName = columnName;
				}
				else {
					// don't recognize the attribute ... just go on the basis of its VR
					useTimeStampRatherThanDicomAttribute = false;
					Attribute a = requestIdentifier.get(tag);
//System.err.println("DateTimeRangeMatch(): don't recognize the attribute "+a+"... just go on the basis of its VR");
					if (a != null) {
						String value = a.getSingleStringValueOrNull();
						if (value != null && value.length() > 0) {
							byte[] vr = a.getVR();
							rangeMatch = new RangeMatch(value);
							if (rangeMatch.lowerValue == null) {
								if (ValueRepresentation.isDateVR(vr)) {
									rangeMatch.lowerValue = earliestPossibleSQLDate;
								}
								else if (ValueRepresentation.isTimeVR(vr)) {
									rangeMatch.lowerValue = earliestPossibleSQLTime;
								}
								else {
									// assert ValueRepresentation.isDateTimeVR(vr)
									rangeMatch.lowerValue = earliestPossibleSQLDateTime;
								}
							}
							if (rangeMatch.upperValue == null) {
								if (ValueRepresentation.isDateVR(vr)) {
									rangeMatch.lowerValue = latestPossibleSQLDate;
								}
								else if (ValueRepresentation.isTimeVR(vr)) {
									rangeMatch.lowerValue = latestPossibleSQLTime;
								}
								else {
									// assert ValueRepresentation.isDateTimeVR(vr)
									rangeMatch.lowerValue = latestPossibleSQLDateTime;
								}
							}
							matchColumnName = columnName;
						}
						// else do nothing since atrribute present but with no value (universal match)
					}
					// else do nothing since tag not in requestIdentifier
				}
			}
		}
	}

	static boolean addToMatchClause(StringBuffer b,String tableName,String columnName,AttributeTag tag,AttributeList requestIdentifier,Set alreadyUsed) {
		// we keep track of alreadyUsed, since the first encountered tag of a Date/Time pair
		// causes both to be matched, and don't want to repeat on encountering the other
		boolean found = false;
		if (!alreadyUsed.contains(tag)) {
			DateTimeRangeMatch match = new DateTimeRangeMatch(tag,requestIdentifier,columnName);
			if (match.matchColumnName != null) {
				found = true;
				alreadyUsed.addAll(match.alreadyUsed);
				if (b.length() != 0) {
					b.append(" AND ");
				}
				b.append(tableName);
				b.append(".");
				b.append(match.matchColumnName);
				b.append(" >= ");
				b.append(match.rangeMatch.lowerValue);
				b.append(" AND ");
				b.append(tableName);
				b.append(".");
				b.append(match.matchColumnName);
				b.append(" <= ");
				b.append(match.rangeMatch.upperValue);
			}
		}
		return found;
	}
	
	public static void main(String[] arg) {
		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030728"); list.put(t,a); }
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyTime,list,alreadyUsed);
			
			System.err.println(b.toString());	// no need to use SLF4J since command line utility/test
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030728-"); list.put(t,a); }
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("173500-"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyTime,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("-20030728"); list.put(t,a); }
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("-173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyTime,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030701-20030728"); list.put(t,a); }
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("010101-173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyTime,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030728"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030728-"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("-20030728"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); a.addValue("20030701-20030728"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYDATE",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("173500-"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("-173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			StringBuffer b = new StringBuffer();
			Set alreadyUsed = new HashSet();
			AttributeList list = new AttributeList();
			
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); a.addValue("010101-173500"); list.put(t,a); }
			
			addToMatchClause(b,"STUDY","STUDYTIME",TagFromName.StudyDate,list,alreadyUsed);
			
			System.err.println(b.toString());
			System.err.println(list);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


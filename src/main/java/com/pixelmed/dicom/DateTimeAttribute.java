/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.util.TimeZone;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Date Time (DT) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class DateTimeAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DateTimeAttribute.java,v 1.21 2017/01/24 10:50:36 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 26;	// assuming not being used for query range matching, in which case it would be 54 :(
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public DateTimeAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DateTimeAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DateTimeAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (DT).</p>
	 *
	 * @return	'D','T' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.DT; }
	
	protected final boolean allowRepairOfIncorrectLength() { return false; }				// do not allow truncation
	
	protected final boolean allowRepairOfInvalidCharacterReplacement() { return false; }
	
	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isDigit(c) || c == ' ' || c == '+' || c =='-' || c == '.');
	}
	
	// public boolean areValuesWellFormed() throws DicomException {}	// should implement this to check position of period and +/- :(

	/**
	 * <p>Get a DICOM format DT {@link java.lang.String String} value from a Java {@link java.util.Date Date}.</p>
	 *
	 * <p>Will format the Date for the specified timezone.</p>
	 *
	 * @param	date				the Java {@link java.util.Date Date} to format
	 * @param	timezone			the Java {@link java.util.TimeZone TimeZone} to use
	 * @param	tzSuffix			whether or not to append the time zone suffix
	 * @return						a DICOM formatted DT value
	 */
	public static String getFormattedString(java.util.Date date,TimeZone timezone,boolean tzSuffix) {
		java.text.SimpleDateFormat dateFormatterOutput = new java.text.SimpleDateFormat(tzSuffix ? "yyyyMMddHHmmss.SSSZ" : "yyyyMMddHHmmss.SSS");
		dateFormatterOutput.setTimeZone(timezone);
		return dateFormatterOutput.format(date);
	}

	/**
	 * <p>Get a DICOM format DT {@link java.lang.String String} value from a Java {@link java.util.Date Date}.</p>
	 *
	 * <p>Will format the Date for the specified timezone.</p>
	 *
	 * <p>Will include the timezone suffix.</p>
	 *
	 * @param	date			the Java {@link java.util.Date Date} to format
	 * @param	timezone		the Java {@link java.util.TimeZone TimeZone} to use
	 * @return					a DICOM formatted DT value
	 */
	public static String getFormattedString(java.util.Date date,TimeZone timezone) {
		return getFormattedString(date,timezone,true/*tzSuffix*/);
	}
	
	/**
	 * <p>Get a DICOM format DT {@link java.lang.String String} value from a Java {@link java.util.Date Date}.</p>
	 *
	 * <p>Will format the Date for the UTC timezone, converting from whatever timezone is specified in the supplied {@link java.util.Date Date} if not UTC.</p>
	 *
	 * @param	date			the Java {@link java.util.Date Date} to format
	 * @return					a DICOM formatted DT value
	 */
	public static String getFormattedStringUTC(java.util.Date date) {
		return getFormattedString(date,TimeZone.getTimeZone("GMT"));
	}

	/**
	 * <p>Get a DICOM format DT {@link java.lang.String String} value from a Java {@link java.util.Date Date}.</p>
	 *
	 * <p>Will format the Date for the default timezone, converting from whatever timezone is specified in the supplied {@link java.util.Date Date} if not the default.</p>
	 *
	 * @param	date			the Java {@link java.util.Date Date} to format
	 * @return					a DICOM formatted DT value
	 */
	public static String getFormattedStringDefaultTimeZone(java.util.Date date) {
		return getFormattedString(date,TimeZone.getDefault());
	}

	/**
	 * <p>Get a DICOM format DT {@link java.lang.String String} value from a Java {@link java.util.Date Date}.</p>
	 *
	 * <p>Will format the Date for the default timezone, converting from whatever timezone is specified in the supplied {@link java.util.Date Date} if not the default.</p>
	 *
	 * @deprecated	use {@link com.pixelmed.dicom.DateTimeAttribute#getFormattedStringDefaultTimeZone(java.util.Date) getFormattedStringDefaultTimeZone()} instead
	 *
	 * @param	date			the Java {@link java.util.Date Date} to format
	 * @return					a DICOM formatted DT value
	 */
	public static String getFormattedString(java.util.Date date) {
		return getFormattedStringDefaultTimeZone(date);
	}
	
	/**
	 * <p>Get a Java {@link java.util.Date Date} from a DICOM format DT {@link java.lang.String String} value.</p>
	 *
	 * @param		dateString					the date to parse
	 * @return									a Java {@link java.util.Date Date}
	 * @throws		java.text.ParseException	if incorrectly encoded
	 */
	public static java.util.Date getDateFromFormattedString(String dateString) throws java.text.ParseException {
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): given "+dateString);
		int l = dateString.length();
		java.util.Date date = null;
		StringBuffer format = new StringBuffer();
		boolean sawTimeZone = false;
		int p = 0;
		int fractionalDigitsStart = 0;
		int fractionalDigitsCount = 0;
		if (l >= 4) {
			format.append("yyyy");
			p = 4;
			if (l >= 6 && Character.isDigit(dateString.charAt(p))) {
				format.append("MM");
				p = 6;
				if (l >= 8 && Character.isDigit(dateString.charAt(p))) {
					format.append("dd");
					p = 8;
					if (l >= 10 && Character.isDigit(dateString.charAt(p))) {
						format.append("HH");
						p = 10;
						if (l >= 12 && Character.isDigit(dateString.charAt(p))) {
							format.append("mm");
							p = 12;
							if (l >= 14 && Character.isDigit(dateString.charAt(p))) {
								format.append("ss");
								p = 14;
								if (l > 14) {
									if (dateString.charAt(p) == '.') {
										// have a fraction
										format.append(".");
										while (++p < l && Character.isDigit(dateString.charAt(p))) {
											++fractionalDigitsCount;
											if (fractionalDigitsStart == 0) {
												fractionalDigitsStart = p;
											}
										}
										if (fractionalDigitsCount > 0) {
											format.append("SSS");	// java.util.Date is number of milliseconds, not a fraction at all ... so we will always send three (no more, no less)
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		if (p < l) {
			int c = dateString.charAt(p);
			if (c == '+' || c == '-') {
				// have a timezone
				format.append("Z");
				sawTimeZone = true;
			}
		}
		
		// would be better to round rather than truncate to milliseconds (which is all Java supports in Date and Calendar), but this is expedient :(
		if (fractionalDigitsCount > 0) {
			if (fractionalDigitsCount < 3) {
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): fractionalDigits lengthen before = "+dateString);
				StringBuffer trailingZeroPadding = new StringBuffer();
				for (int i=fractionalDigitsCount; i<3; ++i) {
					trailingZeroPadding.append("0");
				}
				dateString = dateString.substring(0,fractionalDigitsStart+fractionalDigitsCount) + trailingZeroPadding.toString() + dateString.substring(fractionalDigitsStart+fractionalDigitsCount);
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): fractionalDigits lengthen after = "+dateString);
			}
			else if (fractionalDigitsCount > 3) {
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): fractionalDigits truncate before = "+dateString);
				dateString = dateString.substring(0,fractionalDigitsStart+3) + dateString.substring(fractionalDigitsStart+fractionalDigitsCount);
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): fractionalDigits truncate after = "+dateString);
			}
			// else was exactly three, which is OK
		}
		
		if (!sawTimeZone) {
			// assume UTC, else behavior of Date will depend on application local time zone, which we do not want
			format.append("Z");
			dateString = dateString + "+0000";
		}
		
		String formatString = format.toString();
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): formatString = "+formatString);
		java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat(formatString);
		date = dateFormatter.parse(dateString);
//System.err.println("DateTimeAttribute.getDateFromFormattedString(): given "+dateString+" return "+date.toGMTString());
		return date;
	}
	
	/**
	 * <p>Get a Java {@link java.util.Date Date} from a DICOM format DT {@link java.lang.String String} value.</p>
	 *
	 * <p>Will use the TimezoneOffsetFromUTC if present in the AttributeList, else will assume UTC (not whatever the local time zone happens to be). </p>
	 *
	 * @param	list						the list containing the attributes
	 * @param	dateTag						the tag of the DA attribute
	 * @param	timeTag						the tag of the TM attribute
	 * @return								a Java {@link java.util.Date Date}
	 * @throws	java.text.ParseException	if incorrectly encoded
	 * @throws	DicomException				if date attribute is missing or empty
	 */
	public static java.util.Date getDateFromFormattedString(AttributeList list,AttributeTag dateTag,AttributeTag timeTag) throws java.text.ParseException, DicomException {
		String dateValue = Attribute.getSingleStringValueOrEmptyString(list,dateTag);
		String timeValue = Attribute.getSingleStringValueOrEmptyString(list,timeTag);
		if (dateValue.length() == 0) {
			throw new DicomException("Missing date attribute or value for "+dateTag);
		}
		// missing or empty time is OK
		return getDateFromFormattedString(
				dateValue
			  + timeValue
			  + Attribute.getSingleStringValueOrDefault(list,TagFromName.TimezoneOffsetFromUTC,"+0000")
			);
	}
	
	/**
	 * <p>Get a Java {@link java.util.TimeZone TimeZone} from a DICOM format {@link java.lang.String String} time zone.</p>
	 *
	 * <p>E.g. from +0500 or -0700, the last component of a DateTime attribute value, or the value of the DICOM attribute TimezoneOffsetFromUTC. </p>
	 *
	 * @param		dicomTimeZone	the {@link java.lang.String String} DICOM format time zone
	 * @return						a Java {@link java.util.TimeZone TimeZone} representing the supplied time zone, or the GMT zone if it cannot be understood
	 */
	public static TimeZone getTimeZone(String dicomTimeZone) {
		String useTimeZone = "";
		if (dicomTimeZone.length() == 5 && (dicomTimeZone.startsWith("+") || dicomTimeZone.startsWith("-"))) {
			useTimeZone = "GMT" + dicomTimeZone.substring(0,3) + ":" + dicomTimeZone.substring(3);
//System.err.println("DateTimeAttribute.getTimeZone(): useTimeZone = "+useTimeZone);
		}
		return TimeZone.getTimeZone(useTimeZone);
	}
	
	/**
	 * <p>Get a DICOM format {@link java.lang.String String} time zone from a Java {@link java.util.TimeZone TimeZone} on a particular Java {@link java.util.Date Date} .</p>
	 *
	 * <p>E.g. from +0500 or -0700, the last component of a DateTime attribute value, or the value of the DICOM attribute TimezoneOffsetFromUTC. </p>
	 *
	 * @param		javaTimeZone	the {@link java.util.TimeZone TimeZone} time zone
	 * @param		javaDate		the {@link java.util.Date Date} used to establish whether daylight savings is in effect or not
	 * @return						a DICOM format {@link java.lang.String String} time zone representing the supplied time zone on the supplied date
	 */
	public static String getTimeZone(java.util.TimeZone javaTimeZone,java.util.Date javaDate) {
//System.err.println("DateTimeAttribute.getTimeZone(): javaTimeZone = "+javaTimeZone);
//System.err.println("DateTimeAttribute.getTimeZone(): javaDate = "+javaDate);
		int offset = javaTimeZone.getOffset(javaDate.getTime());	// returns the amount of time in milliseconds to add to UTC to get local time
					
		boolean isNegative = false;
		if (offset < 0) {
			isNegative = true;
			offset = -offset;
		}

		int offsetTotalMinutes = offset / 1000 / 60;
		int offsetHoursPart = offsetTotalMinutes / 60;
		int offsetMinutesPart = offsetTotalMinutes % 60;

		String tzInDICOMFormat =
						(isNegative ? "-" : "+")
						+ (offsetHoursPart > 9 ? "" : "0") + offsetHoursPart
						+ (offsetMinutesPart > 9 ? "" : "0") + offsetMinutesPart
					;
//System.err.println("DateTimeAttribute.getTimeZone(): tzInDICOMFormat = "+tzInDICOMFormat);
		return tzInDICOMFormat;
	}
	
	/**
	 * <p>Get a DICOM format {@link java.lang.String String} time zone representation of the current timezone.</p>
	 *
	 * <p>E.g. from +0500 or -0700, the last component of a DateTime attribute value, or the value of the DICOM attribute TimezoneOffsetFromUTC. </p>
	 *
	 * @return						a DICOM format {@link java.lang.String String} time zone representing the current time zone on the current date
	 */
	public static String getCurrentTimeZone() {
		return getTimeZone(java.util.TimeZone.getDefault(),new java.util.Date());
	}
	
	/**
	 * <p>Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the DT value. </p>
	 *
	 * @param		dateTime	the string to parse
	 * @return					the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this date; may be a ludicrous value if string not formatted correctly
	 * @throws		java.text.ParseException	if incorrectly encoded
	 */
	public static long getTimeInMilliSecondsSinceEpoch(String dateTime) throws java.text.ParseException {
		java.util.Date date = getDateFromFormattedString(dateTime);
//System.err.println("DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(): given "+dateTime+" Date  is "+date.toGMTString());
		long time = date.getTime();
		//long time = dateFormatterParseWithoutTimeZoneOrFraction.parse(dateTime).getTime();
//System.err.println("DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(): given "+dateTime+" return "+time);
		return time;
	}
	
	/**
	 * <p>Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the combination of the DA and TM values of the specified pair of attributes. </p>
	 *
	 * <p>Will use the TimezoneOffsetFromUTC if present in the AttributeList, else will assume UTC (not whatever the local time zone happens to be). </p>
	 *
	 * @param		list		the list containing the attributes
	 * @param		dateTag		the tag of the DA attribute
	 * @param		timeTag		the tag of the TM attribute
	 * @return					the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this date
	 * @throws		java.text.ParseException	if incorrectly encoded
	 * @throws				DicomException	if date attribute is missing or empty
	 */
	public static long getTimeInMilliSecondsSinceEpoch(AttributeList list,AttributeTag dateTag,AttributeTag timeTag) throws java.text.ParseException, DicomException {
		String dateValue = Attribute.getSingleStringValueOrEmptyString(list,dateTag);
		String timeValue = Attribute.getSingleStringValueOrEmptyString(list,timeTag);
		if (dateValue.length() == 0) {
			throw new DicomException("Missing date attribute or value for "+dateTag);
		}
		// missing or empty time is OK
		return getTimeInMilliSecondsSinceEpoch(
				dateValue
			  + timeValue
			  + Attribute.getSingleStringValueOrDefault(list,TagFromName.TimezoneOffsetFromUTC,"+0000")
			);
	}
}


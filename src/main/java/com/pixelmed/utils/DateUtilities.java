/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * <p>Various static methods helpful for handling dates.</p>
 *
 * @author	dclunie
 */
public class DateUtilities {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/DateUtilities.java,v 1.7 2017/01/24 10:50:51 dclunie Exp $";

	private DateUtilities() {}
	
	public static SimpleDateFormat yyyymmddFormat = new SimpleDateFormat("yyyyMMdd");
	
	public static DecimalFormat threeDigitZeroPaddedFormat = new DecimalFormat("000");
	
	/**
	 * <p>Get a DICOM Age String (AS) VR form age between two dates.</p>
	 *
	 * <p>Uses UK (not US) convention for leap year birthdays (earlierDate).</p>
	 *
	 * @param	earlierDate						for example, the date of birth
	 * @param	laterDate						for example, the current date
	 * @throws	ParseException				if one of the dates is not in the correct form
	 * @throws	IllegalArgumentException	if the later date is earlier than the earlier date
	 */
	public static String getAgeBetweenAsDICOMAgeString(String earlierDate,String laterDate) throws ParseException, IllegalArgumentException {
		yyyymmddFormat.setLenient(false);
		Date earlier = yyyymmddFormat.parse(earlierDate);
		Date later = yyyymmddFormat.parse(laterDate);
		return getAgeBetweenAsDICOMAgeString(earlier,later);
	}
	
	/**
	 * <p>Get a DICOM Age String (AS) VR form age between two dates.</p>
	 *
	 * <p>Uses UK (not US) convention for leap year birthdays (earlierDate).</p>
	 *
	 * @param	earlierDate						for example, the date of birth
	 * @param	laterDate						for example, the current date
	 * @throws	IllegalArgumentException	if the later date is earlier than the earlier date
	 */
	public static String getAgeBetweenAsDICOMAgeString(Date earlierDate,Date laterDate) throws IllegalArgumentException {
		Calendar earlier = new GregorianCalendar();
		earlier.setTime(earlierDate);
		Calendar later = new GregorianCalendar();
		later.setTime(laterDate);
		return getAgeBetweenAsDICOMAgeString(earlier,later);
	}
	
	/**
	 * <p>Get a DICOM Age String (AS) VR form age between two dates.</p>
	 *
	 * <p>Uses UK (not US) convention for leap year birthdays (earlierDate).</p>
	 *
	 * @param	earlierDate						for example, the date of birth
	 * @param	laterDate						for example, the current date
	 * @throws	IllegalArgumentException	if the later date is earlier than the earlier date
	 */
	public static String getAgeBetweenAsDICOMAgeString(Calendar earlierDate,Calendar laterDate) throws IllegalArgumentException {
		if (laterDate.before(earlierDate)) {
			throw new IllegalArgumentException("Age cannot be negative");
		}
		
		// See algorithm in "http://thisiswhatiknowabout.blogspot.com/2012/01/how-to-calculate-age-in-java.html"
		
		int years = laterDate.get(Calendar.YEAR) - earlierDate.get(Calendar.YEAR);
		
		int earlierMonth = earlierDate.get(Calendar.MONTH);
		int laterMonth = laterDate.get(Calendar.MONTH);
		int months = laterMonth - earlierMonth;
		
		int earlierDay = earlierDate.get(Calendar.DAY_OF_MONTH);
		int laterDay = laterDate.get(Calendar.DAY_OF_MONTH);
		int days = laterDay - earlierDay;

		if (months < 0) {
			--years;
			months = 12 - earlierMonth + laterMonth;
			if (days < 0) {
				--months;
			}
		}
		else if (months == 0 && days < 0) {
			--years;
			months = 11;
		}
			
		if (years > 0) {
			return threeDigitZeroPaddedFormat.format(years)+"Y";
		}
		else if (months > 1 || (months == 1 && days >= 0)) {
			return threeDigitZeroPaddedFormat.format(months)+"M";
		}
		else {
			if (days < 0) {		// spans month
				Calendar laterDatePreviousMonth = (Calendar)(laterDate.clone());
				laterDatePreviousMonth.add(Calendar.MONTH,-1);
				int laterDateLastDayOfPreviousMonth = laterDatePreviousMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
				days = laterDateLastDayOfPreviousMonth - earlierDay + laterDay;
			}

			if (days >= 7) {
				int weeks = days/7;		// do NOT add 1, i.e., 7 days is 1 week, as is 13 days
				return threeDigitZeroPaddedFormat.format(weeks)+"W";
			}
			else {
				return threeDigitZeroPaddedFormat.format(days)+"D";
			}
		}
	}

}



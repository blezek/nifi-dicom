/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.util.ArrayList;
import java.util.Locale;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Various static methods helpful for formatting floating point values.</p>
 *
 * @author	dclunie
 */
public class FloatFormatter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/FloatFormatter.java,v 1.12 2017/01/24 10:50:51 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FileUtilities.class);
	
	public static String stringValueForNaN = "NaN";
	public static String stringValueForNegativeInfinity = "-Infinity";
	public static String stringValueForPositiveInfinity = "+Infinity";

	private static final int precisionToDisplayDouble = 4;
	private static final int maximumIntegerDigits = 8;
	private static final int maximumMaximumFractionDigits = 6;
	private static final String scientificPattern = ".####E00";

	private FloatFormatter() {}

	/**
	 * <p>Given a double value, return a string representation without too many decimal places.</p>
	 *
	 * <p>Uses the default Locale for formatting, e.g., if the default is Locale.FRENCH, decimal point will be ",".</p>
	 *
	 * <p>Do NOT use this method for formatting strings that always need to have a period for a decimal point (such as DICOM DS values)
	 * but rather {@link #toString(double,Locale) toString(double,Locale)}
	 * and explictly specify the Locale to be Locale.US.</p>
	 *
	 * @param	value		the value to format into a string
	 * @return			the formatted string
	 */
	public static String toString(double value) {
		return toString(value,Locale.getDefault());
	}
	
	/**
	 * <p>Given a double value, return a string representation without too many decimal places.</p>
	 *
	 * @param	value		the value to format into a string
	 * @param	locale		locale to use when formatting (must be explicitly set to Locale.US when creating DICOM DS)
	 * @return			the formatted string
	 */
	public static String toString(double value,Locale locale) {
		String sValue=null;
		if (Double.isNaN(value)) {
			sValue = stringValueForNaN;
		}
		else if (value == Double.NEGATIVE_INFINITY) {
			sValue = stringValueForNegativeInfinity;
		}
		else if (value == Double.POSITIVE_INFINITY) {
			sValue = stringValueForPositiveInfinity;
		}
		else {
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(locale));
			formatter.setGroupingUsed(false);
			int numberOfIntegerDigits=(int)(Math.log10(Math.abs(value)))+1;
			int maximumFractionDigits=precisionToDisplayDouble-numberOfIntegerDigits;
			if (numberOfIntegerDigits > maximumIntegerDigits || maximumFractionDigits > maximumMaximumFractionDigits) {
				//sValue=Double.toString(value);   // does scientific notation as required
				formatter.applyPattern(scientificPattern);
			}
			else {
				if (maximumFractionDigits < 0) {
					maximumFractionDigits=0;
				}
				formatter.setMaximumFractionDigits(maximumFractionDigits);
			}
			sValue=formatter.format(value);
		}
//System.err.println("FloatFormatter.toString(): value="+value+" numberOfIntegerDigits="+numberOfIntegerDigits+" maximumFractionDigits="+maximumFractionDigits+" sValue="+sValue);
		return sValue;
	}
	
	/**
	 * <p>Given a double value, return a string representation that fits in a fixed length.</p>
	 *
	 * <p>Uses the default Locale for formatting, e.g., if the default is Locale.FRENCH, decimal point will be ",".</p>
	 *
	 * <p>Do NOT use this method for formatting strings that always need to have a period for a decimal point (such as DICOM DS values)
	 * but rather {@link #toStringOfFixedMaximumLength(double,int,boolean,Locale) toStringOfFixedMaximumLength(double,int,boolean,Locale)}
	 * and explictly specify the Locale to be Locale.US.</p>
	 *
	 * @param	value			the value to format into a string
	 * @param	maxLength		the maximum length of the string
	 * @param	allowNonNumbers	whether to return NaN and infinity as string values (true), or as zero length string (false)
	 * @return					the formatted string
	 */
	public static String toStringOfFixedMaximumLength(double value,int maxLength,boolean allowNonNumbers) {
		return toStringOfFixedMaximumLength(value,maxLength,allowNonNumbers,Locale.getDefault());
	}
	
	/**
	 * <p>Given a double value, return a string representation that fits in a fixed length.</p>
	 *
	 * @param	value			the value to format into a string
	 * @param	maxLength		the maximum length of the string
	 * @param	allowNonNumbers	whether to return NaN and infinity as string values (true), or as zero length string (false)
	 * @param	locale			locale to use when formatting (must be explicitly set to Locale.US when creating DICOM DS)
	 * @return					the formatted string
	 */
	public static String toStringOfFixedMaximumLength(double value,int maxLength,boolean allowNonNumbers,Locale locale) {
		String sValue = null;
		//String sValue = Double.toString(value);
		//if (sValue.length() > maxLength) {
		if (value == 0) {
			sValue = "0";	// need to treat as a special case, since the logarithm of zero is undefined; Double.toString(value) returns "0.0" but see no need for this.
		}
		else if (Double.isNaN(value)) {
			sValue = allowNonNumbers ? stringValueForNaN : "";
		}
		else if (value == Double.NEGATIVE_INFINITY) {
			sValue = allowNonNumbers ? stringValueForNegativeInfinity : "";
		}
		else if (value == Double.POSITIVE_INFINITY) {
			sValue = allowNonNumbers ? stringValueForPositiveInfinity : "";
		}
		else {
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(locale));
			formatter.setGroupingUsed(false);

			int numberOfIntegerDigits = (int)(Math.log10(Math.abs(value))) + 1;		// will be -ve if small fraction, will be 1 if "0.x" or "9.x"
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): numberOfIntegerDigits = "+numberOfIntegerDigits);
			int numberOfSignBytes = value < 0 ? 1 : 0;
			
			if (numberOfIntegerDigits + numberOfSignBytes > maxLength) {
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): too large for non-scientific notation");
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): too large - exponent digits = " + numberOfIntegerDigits);
				// too large, but exponent will never be negative
				if (Math.abs(numberOfIntegerDigits) > 99) {
					if (numberOfSignBytes == 0) {
						formatter.applyPattern(".###########E000");
					}
					else {
						formatter.applyPattern(".##########E000");
					}
				}
				else if (Math.abs(numberOfIntegerDigits) > 9) {
					if (numberOfSignBytes == 0) {
						formatter.applyPattern(".############E00");
					}
					else {
						formatter.applyPattern(".###########E00");
					}
				}
				else {
					if (numberOfSignBytes == 0) {
						formatter.applyPattern(".#############E0");
					}
					else {
						formatter.applyPattern(".############E0");
					}
				}
			}
			else {
				boolean isZeroWithoutFraction = Math.round(value) == 0;		// want to know this to supress leading zero in "0.x", as opposed to "9.x"
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): isZeroWithoutFraction = "+isZeroWithoutFraction);
				if (isZeroWithoutFraction) {
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): supressing leading zero");
					--numberOfIntegerDigits;
				}
				int numberOfFractionDigitsAvailable = maxLength - numberOfSignBytes - numberOfIntegerDigits;
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): numberOfFractionDigitsAvailable = "+numberOfFractionDigitsAvailable);
				if (numberOfFractionDigitsAvailable == 0 || numberOfFractionDigitsAvailable == 1) {		// since we need space for a decimal point, we have to sacrifice fraction
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): formatting without fraction");
					formatter.setMaximumFractionDigits(0);
				}
				else if (numberOfFractionDigitsAvailable > 1 && numberOfIntegerDigits >= -4) {		// need space for decimal point, and to account for small numbers where E-nn would sacrifice precision
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): formatting with fraction");
					if (numberOfIntegerDigits >= 0) {
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): formatting with number and fraction");
						formatter.setMaximumIntegerDigits(numberOfIntegerDigits);					// this supresses the leading "0" in "0.", which wastes one byte
						formatter.setMaximumFractionDigits(numberOfFractionDigitsAvailable - 1);
					}
					else {
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): formatting with only fraction");
						formatter.setMaximumIntegerDigits(0);
						formatter.setMaximumFractionDigits(maxLength - numberOfSignBytes - 1);		// do not use numberOfFractionDigitsAvailable since it does not account for leading zeroes
					}
				}
				else {
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): too small for non-scientific notation");
					// Too small, and need to allow space for minus sign in negative exponent
//System.err.println("FloatFormatter.toStringOfFixedMaximumLength(): too small - exponent digits = " + numberOfIntegerDigits);
					if (Math.abs(numberOfIntegerDigits) > 99) {
						if (numberOfSignBytes == 0) {
							formatter.applyPattern(".##########E000");
						}
						else {
							formatter.applyPattern(".#########E000");
						}
					}
					else if (Math.abs(numberOfIntegerDigits) > 9) {
						if (numberOfSignBytes == 0) {
							formatter.applyPattern(".###########E00");
						}
						else {
							formatter.applyPattern(".##########E00");
						}
					}
					else {
						if (numberOfSignBytes == 0) {
							formatter.applyPattern(".############E0");
						}
						else {
							formatter.applyPattern(".###########E0");
						}
					}
				}
			}
			
			sValue=formatter.format(value);
		}
		return sValue;
	}
	
	/**
	 * <p>Extract a specified number of delimited numeric values from a string into an array of doubles.</p>
	 *
	 * @param	s		the string containing delimited double values
	 * @param	wanted		the number of double values wanted
	 * @param	delimChar	the delimiter character
	 * @return			an array of doubles of the size wanted containing the values, else null
	 */
	public static final double[] fromString(String s,int wanted,char delimChar) {
		double[] values = new double[wanted];
		int count=0;
		try {
			int start=0;
			int delim=0;
			int l=s.length();
			while (count < wanted) {
				if (delim >= l || s.charAt(delim) == delimChar) {
					values[count++] = Double.parseDouble(s.substring(start,delim));
					++delim;
					start=delim;
					if (delim >= l) break;
				}
				else {
					++delim;
				}
			}
		}
		catch (NumberFormatException e) {
			slf4jlogger.error("", e);
			count=0;			// discard any intermediate results
		}
		return count != wanted ? null : values;
	}
	
	/**
	 * <p>Extract an arbitrary number of delimited numeric values from a string into an array of doubles.</p>
	 *
	 * @param	s		the string containing delimited double values
	 * @param	delimChar	the delimiter character
	 * @return			an array of doubles of the size wanted containing the values, else null
	 */
	public static final double[] fromString(String s,char delimChar) {
		// could do this more tidily with StringTokenizer :(
		ArrayList valueList = new ArrayList();
		int count=0;
		try {
			int start=0;
			int delim=0;
			int l=s.length();
			while (start < l) {
				if (delim >= l || s.charAt(delim) == delimChar) {
					valueList.add(new Double(Double.parseDouble(s.substring(start,delim))));
					++count;
					++delim;
					start=delim;
					if (delim >= l) break;
				}
				else {
					++delim;
				}
			}
		}
		catch (NumberFormatException e) {
			slf4jlogger.error("", e);
			count=0;			// discard any intermediate results
		}
		double[] values = null;
		if (count > 0) {
			values = new double[count];
			for (int i=0; i<count; ++i) {
				values[i]=((Double)valueList.get(i)).doubleValue();
			}
		}
		return values;
	}

	
	private static double[] testDoubleValues = {
		0,
		1.1,
		0.11,
		0.1133408781152648,
		-0.1133408781152648,
		0.01133408781152648,
		-0.01133408781152648,
		0.001133408781152648,
		-0.001133408781152648,
		0.0001133408781152648,
		-0.0001133408781152648,
		0.00001133408781152648,
		-0.00001133408781152648,
		0.000001133408781152648,
		-0.000001133408781152648,
		0.000000000001133408781152648,
		-0.000000000001133408781152648,
		113340878115264.8,
		-113340878115264.8,
		1133408781152648.0,
		-1133408781152648.0,
		99999.999,
		-99999.999,
		99999.999999999999999,
		-99999.999999999999999,
		Double.NEGATIVE_INFINITY,
		Double.POSITIVE_INFINITY,
		Double.NaN,
		Double.MAX_VALUE,
		Double.MIN_VALUE,
		Float.MAX_VALUE,
		Float.MIN_VALUE,
		Long.MAX_VALUE,
		Long.MIN_VALUE,
		Integer.MAX_VALUE,
		Integer.MIN_VALUE,
		Short.MAX_VALUE,
		Short.MIN_VALUE
	};
	
	private static String[] testDoubleStringSupplied = {
		"0",
		"1.1",
		"0.11",
		"0.1133408781152648",
		"-0.1133408781152648",
		"0.01133408781152648",
		"-0.01133408781152648",
		"0.001133408781152648",
		"-0.001133408781152648",
		"0.0001133408781152648",
		"-0.0001133408781152648",
		"0.00001133408781152648",
		"-0.00001133408781152648",
		"0.000001133408781152648",
		"-0.000001133408781152648",
		"0.000000000001133408781152648",
		"-0.000000000001133408781152648",
		"113340878115264.8",
		"-113340878115264.8",
		"1133408781152648.0",
		"-1133408781152648.0",
		"99999.999",
		"-99999.999",
		"99999.999999999999999",
		"-99999.999999999999999",
		"Double.NEGATIVE_INFINITY",
		"Double.POSITIVE_INFINITY",
		"Double.NaN",
		"Double.MAX_VALUE",
		"Double.MIN_VALUE",
		"Float.MAX_VALUE",
		"Float.MIN_VALUE",
		"9223372036854775807",
		"-9223372036854775808",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768"
	};
		
	private static String[] testDoubleStringExpectedForToString = {
		"0",
		"1.1",
		"0.11",
		"0.113",
		"-0.113",
		"0.0113",
		"-0.0113",
		"0.00113",
		"-0.00113",
		"0.000113",
		"-0.000113",
		".1133E-04",
		"-.1133E-04",
		".1133E-05",
		"-.1133E-05",
		".1133E-11",
		"-.1133E-11",
		".1133E15",
		"-.1133E15",
		".1133E16",
		"-.1133E16",
		"100000",
		"-100000",
		"100000",
		"-100000",
		"-Infinity",
		"+Infinity",
		"NaN",
		".1798E309",
		".49E-323",
		".3403E39",
		".1401E-44",
		".9223E19",
		"-.9223E19",
		".2147E10",
		"-.2147E10",
		"32767",
		"-32768"
	};
	
	private static String[] testDoubleStringExpectedForFixedMaximumLength16 = {
		"0",
		"1.1",
		".11",
		".113340878115265",
		"-.11334087811526",
		".011334087811526",
		"-.01133408781153",
		".001133408781153",
		"-.00113340878115",
		".000113340878115",
		"-.00011334087812",
		".000011334087812",
		"-.00001133408781",
		".113340878115E-5",
		"-.11334087812E-5",
		".11334087812E-11",
		"-.1133408781E-11",
		"113340878115265",
		"-113340878115265",
		"1133408781152648",
		"-.11334087812E16",
		"99999.999",
		"-99999.999",
		"100000",
		"-100000",
		"-Infinity",
		"+Infinity",
		"NaN",
		".17976931349E309",
		".49E-323",
		".340282346639E39",
		".14012984643E-44",
		".922337203685E19",
		"-.92233720369E19",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768"
	};
	
	public static void main(String arg[]) {
		slf4jlogger.info("Test of FloatFormatter.toString():");
		for (int i=0; i< testDoubleValues.length; ++i) {
			String sv = FloatFormatter.toString(testDoubleValues[i]);
			System.err.println("\t"+(sv.equals(testDoubleStringExpectedForToString[i]) ? "PASS" : "FAIL")+": Supplied <"+testDoubleStringSupplied[i]+">\t Got <"+sv+">\t Expected <"+testDoubleStringExpectedForToString[i]+">");	// No need for SLF4J since test
		}
		slf4jlogger.info("Test of FloatFormatter.toStringOfFixedMaximumLength(double,16):");
		for (int i=0; i< testDoubleValues.length; ++i) {
			String sv = FloatFormatter.toStringOfFixedMaximumLength(testDoubleValues[i],16,true);
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testDoubleStringExpectedForFixedMaximumLength16[i]) && svl <= 16 ? "PASS" : "FAIL")+": Supplied <"+testDoubleStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testDoubleStringExpectedForFixedMaximumLength16[i]+">\t Double.toString() <"+Double.toString(testDoubleValues[i])+">");	// No need for SLF4J since test
		}
		
	}
}

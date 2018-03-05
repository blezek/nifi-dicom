/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.text.Collator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * <p>Various static methods helpful for comparing and manipulating strings.</p>
 *
 * @author	dclunie
 */
public class StringUtilities {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/StringUtilities.java,v 1.17 2017/01/24 10:50:52 dclunie Exp $";

	private static Collator ourCollator = Collator.getInstance();
	static { ourCollator.setStrength(Collator.IDENTICAL); ourCollator.setDecomposition(Collator.FULL_DECOMPOSITION); }
	
	private StringUtilities() {}
	
	/**
	 * <p>Replace all listed characters in a string with those listed as replacements.</p>
	 *
	 * <p>If newchars is null or shorter than oldchars, the character will be deleted.</p>
	 *
	 * @param	string		the String to replace characters within
	 * @param	oldChars	a String containing characters to be replaced
	 * @param	newChars	a String containing corresponding characters to use as replacements (may be null)
	 * @return				a String with the characters replaced
	 */
	static public final String replaceAllInWith(String string,String oldChars,String newChars) {
		String newString = string;
		if (string != null && oldChars != null && oldChars.length() > 0) {
			int newCharsLength = newChars == null ? 0 : newChars.length();
			for (int i=0; i<oldChars.length(); ++i) {
				char oldChar = oldChars.charAt(i);
				if (i < newCharsLength) {
					char newChar = newChars.charAt(i);
					newString.replace(oldChar,newChar);
				}
				else {
					newString.replace(Character.toString(oldChar),"");
				}
			}
		}
		return newString;
	}
	
	/**
	 * <p>Does a string contain any one of an array of strings ?</p>
	 *
	 * @param	string		the string to test
	 * @param	substrings	an array of strings to look for within string
	 * @return				true if any string in substrings is found within string
	 */
	static public final boolean contains(String string,String[] substrings) {
		if (string != null && substrings != null) {
			for (String substring : substrings) {
				if (string.contains(substring)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * <p>Does a string contain any one of an array of strings  regardless of case ?</p>
	 *
	 * @param	string		the string to test
	 * @param	substrings	an array of strings to look for within string
	 * @return				true if any string in substrings is found within string
	 */
	static public final boolean containsRegardlessOfCase(String string,String[] substrings) {
		if (string != null && substrings != null) {
			string = string.toLowerCase(java.util.Locale.US);
			for (String substring : substrings) {
				if (string.contains(substring.toLowerCase(java.util.Locale.US))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * <p>Get delimited values from a string.</p>
	 *
	 * <p>Consecutive delimiters result in an empty (zero length not null) string value.</p>
	 *
	 * <p>Hence always returns a Vector one longer than the number of delimiters present.</p>
	 *
	 * @param	string		the string containing the delimited values
	 * @param	delimiter	the delimiter
	 * @return			a Vector of String values
	 */
	static public final Vector getDelimitedValues(String string,String delimiter) {
		Vector r = new Vector();
		StringTokenizer t = new StringTokenizer(string,delimiter,true);
		boolean lastWasDelimiter = true;
		while (t.hasMoreTokens()) {
			String v = t.nextToken();
			if (v.equals(delimiter)) {		// consecutive delimiters (and start) means null value
				if (lastWasDelimiter) {
					r.add("");
				}
				lastWasDelimiter = true;
			}
			else {
				r.add(v);
				lastWasDelimiter = false;
			}
		}
		if (lastWasDelimiter) {				// handle empty last value
			r.add("");
		}
		return r;
	}

	/**
	 * <p>Remove any trailing instances of a particular character from a string.</p>
	 *
	 * @param	src	the string that may have trailing characters
	 * @param	rmchar	the character, all trailing instances of which are to be removed
	 * @return		the value of the string argument with any instances of the trailing character removed
	 */
	static public final String removeTrailingCharacter(String src,char rmchar) {
		char [] c = src.toCharArray();
		int l = c.length;
		int n = l;
		while (n > 0 && c[n-1] == rmchar) --n;
		return n == l ? src : (n > 0 ? new String(c,0,n) : "");
	}

	/**
	 * <p>Remove any trailing instances of whitespace or control characters from a string.</p>
	 *
	 * @param	src	the string that may have trailing characters
	 * @return		the value of the string argument with any instances of trailing whitespace or control characters removed
	 */
	static public final String removeTrailingWhitespaceOrISOControl(String src) {
		char [] c = src.toCharArray();
		int l = c.length;
		int n = l;
		while (n > 0 && (Character.isWhitespace(c[n-1]) || Character.isISOControl(c[n-1]))) --n;
		return n == l ? src : (n > 0 ? new String(c,0,n) : "");
	}

	/**
	 * <p>Remove any leading instances of a particular character from a string.</p>
	 *
	 * @param	src	the string that may have leading characters
	 * @param	rmchar	the character, all leading instances of which are to be removed
	 * @return		the value of the string argument with any instances of the leading character removed
	 */
	static public final String removeLeadingCharacter(String src,char rmchar) {
		char [] c = src.toCharArray();
		int l = c.length;
		int i = 0;
		while (i < l && c[i] == rmchar) ++i;
		return i == 0 ? src : (i < l ? new String(c,i,l-i) : "");
	}

	/**
	 * <p>Remove any leading instances of whitespace or control characters from a string.</p>
	 *
	 * @param	src	the string that may have trailing characters
	 * @return		the value of the string argument with any instances of trailing whitespace or control characters removed
	 */
	static public final String removeLeadingWhitespaceOrISOControl(String src) {
		char [] c = src.toCharArray();
		int l = c.length;
		int i = 0;
		while (i < l && (Character.isWhitespace(c[i]) || Character.isISOControl(c[i]))) ++i;
		return i == 0 ? src : (i < l ? new String(c,i,l-i) : "");
	}

	/**
	 * <p>Remove any trailing spaces from a string.</p>
	 *
	 * @param	src	the string that may have trailing spaces
	 * @return		the value of the string argument with any trailing spaces removed
	 */
	static public final String removeTrailingSpaces(String src) {
		return removeTrailingCharacter(src,' ');
	}
	
	/**
	 * <p>Remove any leading spaces from a string.</p>
	 *
	 * @param	src	the string that may have leading spaces
	 * @return		the value of the string argument with any leading spaces removed
	 */
	static public final String removeLeadingSpaces(String src) {
		return removeLeadingCharacter(src,' ');
	}
	
	/**
	 * <p>Remove any leading or trailing padding from a string.</p>
	 *
	 * <p>Padding in this context means leading or trailing white space of any kind or null characters.</p>
	 *
	 * @param	src	the string that may have padding
	 * @return		the value of the string argument with any padding removed
	 */
	static public final String removeLeadingOrTrailingWhitespaceOrISOControl(String src) {
		return removeTrailingWhitespaceOrISOControl(removeLeadingWhitespaceOrISOControl(src));
	}
	
	/**
	 * <p>Compare strings based on their integer value of they are both integers,
	 * otherwise their lexicographic order.</p>
	 *
	 * <p>For example,
	 * <code>"001"</code> and<code>"1"</code> would be treated as equal, whilst
	 * <code>"1"</code> would be considered as occuring before <code>"10"</code>,
	 * which would not be the case with a simple lexicographic ordering.
	 * </p>
	 *
	 * @param	s1	the first of two strings to be compared
	 * @param	s2	the first of two strings to be compared
	 * @return		the value 0 if the first string is equal to the second string; a value less than 0 if the first string
	 *			is less than the second string; and a value greater than 0 if the first string
	 *			is greater than the second string
	 */
	static public final int compareStringsThatMayBeIntegers(String s1,String s2) {
		try {
			return Integer.parseInt(s1) - Integer.parseInt(s2);
		}
		catch (NumberFormatException e) {
//System.err.println("compareStringsThatMayBeIntegers: falling back to string");
			return ourCollator.compare(s1,s2);
			//return s1.compareTo(s2);
		}
	}
	
	/**
	 * <p>Compare strings based on the lexicographic order of their values, but accounting for non-zero padded integers.</p>
	 *
	 * <p>Note that the comparison is more complex than a simple lexicographic comparison
	 * of strings (as described in the definition of {@link java.lang.String#compareTo(String) java.lang.String.compareTo(String)}
	 * but rather accounts for embedded non-zero padded integers by treating occurrences of space
	 * delimited integers as integer values rather than strings. For example,
	 * <code>"a 001 b"</code> and<code>"a 1 b"</code> would be treated as equal, whilst
	 * <code>"a 1 b"</code> would be considered as occuring before <code>"a 10 b"</code>,
	 * which would not be the case with a simple lexicographic ordering.
	 * </p>
	 *
	 * @param	s1	the first of two strings to be compared
	 * @param	s2	the first of two strings to be compared
	 * @return		the value 0 if the first string is equal to the second string; a value less than 0 if the first string
	 *			is lexicographically less than the second string; and a value greater than 0 if the first string
	 *			is lexicographically greater than the second string
	 */
	static public final int compareStringsWithEmbeddedNonZeroPaddedIntegers(String s1,String s2) {
		StringTokenizer st1 = new StringTokenizer(s1);
		StringTokenizer st2 = new StringTokenizer(s2);
		int c = 0;
		while (st1.hasMoreElements() && st2.hasMoreElements()) {
			String t1 = st1.nextToken();
			String t2 = st2.nextToken();
			c = compareStringsThatMayBeIntegers(t1,t2);
//System.err.println("compareStringsWithEmbeddedNonZeroPaddedIntegers: looping with <"+t1+"> and <"+t2+"> c="+c);
			if (c != 0) return c;
		}
		c = st1.hasMoreElements() ? 1 : (st1.hasMoreElements() ? -1 : 0);
		return c;
	}

	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values and printable string values of a String.</p>
	 *
	 * @param	s	the String to be dumped as if it were an array of char
	 * @return		a string containing the multiline result
	 */
	public static String dump(String s) {
		return dump(s.toCharArray());
	}

	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values and printable string values of an array of char.</p>
	 *
	 * @param	chars	the array of char to be dumped
	 * @return		a string containing the multiline result
	 */
	public static String dump(char[] chars) {
		int offset = 0;
		int lng = chars == null ? 0 : chars.length;
		StringBuffer sb = new StringBuffer();
		if (chars != null && lng > 0) {
			int i=0;
			int stringStart=0;
			int stringCount=0;
			while (i < lng) {
				int position = i+offset;
				if (i%16 == 0) {
					if (i != 0) sb.append("\n");
					sb.append(HexDump.intToPaddedDecimalString(position));
					sb.append(" (");
					sb.append(HexDump.intToPaddedHexStringWith0x(position));
					sb.append("):");
					stringStart=position;
					stringCount=0;
				}
				sb.append(" ");
				sb.append(HexDump.shortToPaddedHexString((short)(chars[position])));
				++i;
				++stringCount;
				if (i%16 == 0 || i == lng) {
					sb.append(" ");
					sb.append(new String(chars,stringStart,stringCount));
				}
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	/*
	 * <p>Dump an array as a human-readable string.</p>
	 *
	 * @param	doubleArray	to dump
	 * @return			a string representation
	 */
	public static String toString(double[] doubleArray) {
		if (doubleArray == null) {
			return null;
		}
		else {
			if (doubleArray.length == 0) {
				return "";
			}
			else {
				String delimiter="";
				StringBuffer strbuf = new StringBuffer();
				for (int i=0; i<doubleArray.length; ++i) {
					strbuf.append(delimiter);
					strbuf.append("[");
					strbuf.append(i);
					strbuf.append("]=");
					strbuf.append(doubleArray[i]);
					delimiter=" ";
				}
				return strbuf.toString();
			}
		}
	}

	/*
	 * <p>Dump an array as a human-readable string.</p>
	 *
	 * @param	stringArray	to dump
	 * @return			a string representation
	 */
	public static String toString(String[] stringArray) {
		if (stringArray == null) {
			return null;
		}
		else {
			if (stringArray.length == 0) {
				return "";
			}
			else {
				String delimiter="";
				StringBuffer strbuf = new StringBuffer();
				for (int i=0; i<stringArray.length; ++i) {
					strbuf.append(delimiter);
					strbuf.append("[");
					strbuf.append(i);
					strbuf.append("]=");
					strbuf.append(stringArray[i]);
					delimiter=" ";
				}
				return strbuf.toString();
			}
		}
	}

	/*
	 * <p>Dump an array of arrays as a human-readable string.</p>
	 *
	 * @param	stringArrays	to dump
	 * @return			a string representation
	 */
	public static String toString(String[][] stringArrays) {
		if (stringArrays == null) {
			return null;
		}
		else {
			if (stringArrays.length == 0) {
				return "";
			}
			else {
				String delimiter="";
				StringBuffer strbuf = new StringBuffer();
				for (int i=0; i<stringArrays.length; ++i) {
					strbuf.append(delimiter);
					strbuf.append("[");
					strbuf.append(i);
					strbuf.append("]={");
					strbuf.append(toString(stringArrays[i]));
					strbuf.append("}");
					delimiter=" ";
				}
				return strbuf.toString();
			}
		}
	}
	
	/*
	 * <p>Pad a positive integer to make it a specified length.</p>
	 *
	 * @param	str	the string to pad
	 * @param	length	the length required
	 * @param	pad	the pad character
	 * @return			the padded string
	 */
	public static String padPositiveInteger(String str,int length,Character pad) {
		while (str.length() < length) str = pad + str;
		return str;
	}
	
	/*
	 * <p>Zero pad a positive integer to make it a specified length.</p>
	 *
	 * @param	str	the string to pad
	 * @param	length	the length required
	 * @return			the padded string
	 */
	public static String zeroPadPositiveInteger(String str,int length) {
		return padPositiveInteger(str,length,'0');
	}
	
	/**
	 * <p>Unit testing.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {
		String s;
		s="1234";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="1234  ";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="12  34  ";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="  1234";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="  1234  ";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="1";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s=" ";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="    ";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		s="";
		System.err.println("src <"+s+"> dst <"+removeTrailingSpaces(s)+">");
		
		String s1;
		String s2;
		
		s1 = "this is 2 way";
		s2 = "this is 2 way";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

		s1 = "this is 2 way";
		s2 = "this is 10 way";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

		s1 = "this is 10 way";
		s2 = "this is 2 way";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

		s1 = "this is 2 way";
		s2 = "this is 2 way plus";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

		s1 = "this is 2 way";
		s2 = "this is 10 way plus";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

		s1 = "this is 10 way";
		s2 = "this is 2 way plus";
		System.err.println("s1 <"+s1+"> s2 <"+s2+"> : compare ="+compareStringsWithEmbeddedNonZeroPaddedIntegers(s1,s2));

	}
}
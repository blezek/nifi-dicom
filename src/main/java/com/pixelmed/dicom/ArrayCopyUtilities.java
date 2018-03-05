/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class of static methods for copying data between arrays of different types,
 * expanding arrays, and comparing them, and removing padding from strings.</p>
 *
 * @author	dclunie
 */
abstract public class ArrayCopyUtilities {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ArrayCopyUtilities.java,v 1.17 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Pack an array of byte values into an array of short.</p>
	 *
	 * <p>Order is little endian (first byte into low order byte of short.</p>
	 *
	 * @param	src	an array of byte
	 * @return		an array of short, half the length of the src
	 */
	static public short[] packByteArrayIntoShortArrayLittleEndian(byte[] src) {
		if (src == null || src.length == 0) return null;
		int n=src.length;
		short[] dst = new short[(n-1)/2+1];
		for (int j=0; j<n; ++j) {
			if (j%2 == 0) {
				dst[j/2] = (short)(src[j]&0x00ff);
			}
			else {
				dst[j/2] = (short)(dst[j/2] | ((src[j]<<8)&0xff00));
			}
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in short, into an array of int.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as unsigned
	 * @return		an array of int
	 */
	static public int[] copyUnsignedShortToIntArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		int[] dst = new int[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j]&0xffff;		// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in short, into an array of int.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as signed
	 * @return		an array of int
	 */
	static public int[] copySignedShortToIntArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		int[] dst = new int[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in short, into an array of long.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as unsigned
	 * @return		an array of long
	 */
	static public long[] copyUnsignedShortToLongArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		long[] dst = new long[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j]&0xffff;		// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in short, into an array of long.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as signed
	 * @return		an array of long
	 */
	static public long[] copySignedShortToLongArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		long[] dst = new long[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in short, into an array of float.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as unsigned
	 * @return		an array of float
	 */
	static public float[] copyUnsignedShortToFloatArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		float[] dst = new float[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j]&0xffff;		// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in short, into an array of float.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as signed
	 * @return		an array of float
	 */
	static public float[] copySignedShortToFloatArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		float[] dst = new float[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in short, into an array of double.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as unsigned
	 * @return		an array of double
	 */
	static public double[] copyUnsignedShortToDoubleArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		double[] dst = new double[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j]&0xffff;		// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in short, into an array of double.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of short, whose values are interpreted as signed
	 * @return		an array of double
	 */
	static public double[] copySignedShortToDoubleArray(short[] src) {
		if (src == null) return null;
		int n=src.length;
		double[] dst = new double[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in int, into an array of long.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as unsigned
	 * @return		an array of long
	 */
	static public long[] copyUnsignedIntToLongArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		long[] dst = new long[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j]&0x00000000ffffffffl;	// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in int, into an array of double.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as signed
	 * @return		an array of double
	 */
	static public double[] copySignedIntToDoubleArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		double[] dst = new double[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in int, into an array of double.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as unsigned
	 * @return		an array of double
	 */
	static public double[] copyUnsignedIntToDoubleArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		double[] dst = new double[n];
		for (int j=0; j<n; ++j) {
			dst[j]=(double)(src[j]&0x00000000ffffffffl);	// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in int, into an array of float.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as signed
	 * @return		an array of float
	 */
	static public float[] copySignedIntToFloatArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		float[] dst = new float[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in int, into an array of float.</p>
	 *
	 * <p>Sign extension is prevented.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as unsigned
	 * @return		an array of float
	 */
	static public float[] copyUnsignedIntToFloatArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		float[] dst = new float[n];
		for (int j=0; j<n; ++j) {
			dst[j]=(float)(src[j]&0x00000000ffffffffl);	// make sure of no sign extension
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in int, into an array of long.</p>
	 *
	 * <p>Sign extension is performed.</p>
	 *
	 * @param	src	an array of int, whose values are interpreted as signed
	 * @return		an array of long
	 */
	static public long[] copySignedIntToLongArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		long[] dst = new long[n];
		for (int j=0; j<n; ++j) {
			dst[j]=src[j];			// allow sign extension
//System.err.println("ArrayCopyUtilities.copySignedIntToLongArray(): ["+j+"] dst = "+dst[j]+" src = "+src[j]);
		}
		return dst;
	}

	/**
	 * <p>Copy an array of unsigned values in int, into an array of short.</p>
	 *
	 * <p>The value is truncated as necessary.</p>
	 *
	 * @param	src	an array of int
	 * @return		an array of short
	 */
	static public short[] copyUnsignedIntToShortArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		short[] dst = new short[n];
		for (int j=0; j<n; ++j) {
			dst[j]=(short)(src[j]);		// truncates
		}
		return dst;
	}

	/**
	 * <p>Copy an array of signed values in int, into an array of short.</p>
	 *
	 * <p>The value is truncated as necessary.</p>
	 *
	 * @param	src	an array of int
	 * @return		an array of short
	 */
	static public short[] copySignedIntToShortArray(int[] src) {
		if (src == null) return null;
		int n=src.length;
		short[] dst = new short[n];
		for (int j=0; j<n; ++j) {
			dst[j]=(short)(src[j]);		// truncates
		}
		return dst;
	}

	/**
	 * <p>Is this character a valid leading padding character in a DICOM string value ?</p>
	 *
	 * @param	c	the character to test
	 */
	static private boolean isLeadingPadding(char c) {
		return c == ' ';
	}

	/**
	 * <p>Is this character a valid trailing padding character in a DICOM string value ?</p>
	 *
	 * @param	c	the character to test
	 */
	static private boolean isTrailingPadding(char c) {
		return c == ' ' || c == '\0';
	}

	/**
	 * <p>Copy a string removing leading and trailing padding.</p>
	 *
	 * @param	src	the padded value
	 * @return		the unpadded value
	 */
	static public String[] copyStringArrayRemovingLeadingAndTrailingPadding(String[] src) {
		if (src == null) return null;
		int n=src.length;
		String[] dst = new String[n];
		for (int j=0; j<n; ++j) {
			if (src[j] == null) {
				dst[j]=null;
			}
			else {
				int lng=src[j].length();
				int start=0;
				while (start < lng && isLeadingPadding(src[j].charAt(start))) ++start;
				int end=lng;
				while (end > start && isTrailingPadding(src[j].charAt(end-1))) --end;
				dst[j]=src[j].substring(start,end);
			}
		}
		return dst;
	}

	/**
	 * <p>Extract short values from an array of strings into an array of short.</p>
	 *
	 * <p>Exceptions in the format of the string are trapped and 0 value(s) returned.</p>
	 *
	 * @param	src	an array of strings, each of which should be a short numeric value
	 * @return		an array of short
	 */
	static public short[] copyStringToShortArray(String[] src) {
		if (src == null) return null;
		int n=src.length;
		short[] dst = new short[n];
		for (int j=0; j<n; ++j) {
			short value=0;
			try {
				value=Short.valueOf(src[j]).shortValue();
			}
			catch (NumberFormatException e) {
			}
			catch (NullPointerException e) {
			}
			dst[j]=value;
		}
		return dst;
	}

	/**
	 * <p>Extract integer values from an array of strings into an array of int.</p>
	 *
	 * <p>Exceptions in the format of the string are trapped and 0 value(s) returned.</p>
	 *
	 * @param	src	an array of strings, each of which should be an integer numeric value
	 * @return		an array of int
	 */
	static public int[] copyStringToIntArray(String[] src) {
		if (src == null) return null;
		int n=src.length;
		int[] dst = new int[n];
		for (int j=0; j<n; ++j) {
			int value=0;
			try {
				value=Integer.valueOf(src[j]).intValue();
			}
			catch (NumberFormatException e) {
			}
			catch (NullPointerException e) {
			}
			dst[j]=value;
		}
		return dst;
	}

	/**
	 * <p>Extract long values from an array of strings into an array of int.</p>
	 *
	 * <p>Exceptions in the format of the string are trapped and 0 value(s) returned.</p>
	 *
	 * @param	src	an array of strings, each of which should be an long numeric value
	 * @return		an array of long
	 */
	static public long[] copyStringToLongArray(String[] src) {
		if (src == null) return null;
		int n=src.length;
		long[] dst = new long[n];
		for (int j=0; j<n; ++j) {
			long value=0;
			try {
				value=Long.valueOf(src[j]).longValue();
			}
			catch (NumberFormatException e) {
			}
			catch (NullPointerException e) {
			}
			dst[j]=value;
		}
		return dst;
	}

	/**
	 * <p>Extract decimal values from an array of strings into an array of float.</p>
	 *
	 * <p>Exceptions in the format of the string are trapped and 0 value(s) returned.</p>
	 *
	 * @param	src	an array of strings, each of which should be a decimal numeric value
	 * @return		an array of float
	 */
	static public float[] copyStringToFloatArray(String[] src) {
		if (src == null) return null;
		int n=src.length;
		float[] dst = new float[n];
		for (int j=0; j<n; ++j) {
			float value=0;
			try {
				value=Float.valueOf(src[j]).floatValue();
			}
			catch (NumberFormatException e) {
			}
			catch (NullPointerException e) {
			}
			dst[j]=value;
		}
		return dst;
	}

	/**
	 * <p>Extract decimal values from an array of strings into an array of double.</p>
	 *
	 * <p>Exceptions in the format of the string are trapped and 0 value(s) returned.</p>
	 *
	 * @param	src	an array of strings, each of which should be a decimal numeric value
	 * @return		an array of double
	 */
	static public double[] copyStringToDoubleArray(String[] src) {
		if (src == null) return null;
		int n=src.length;
		double[] dst = new double[n];
		for (int j=0; j<n; ++j) {
			double value=0;
			try {
				value=Double.valueOf(src[j]).doubleValue();
			}
			catch (NumberFormatException e) {
			}
			catch (NullPointerException e) {
			}
			dst[j]=value;
		}
		return dst;
	}

	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public short[] expandArray(short[] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public short[] expandArray(short[] src,int expandBy) {
		short[] dst;
		if (src == null) {
			dst=new short[expandBy];
		}
		else {
			int n=src.length;
			dst = new short[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}


	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public short[][] expandArray(short[][] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public short[][] expandArray(short[][] src,int expandBy) {
		short[][] dst;
		if (src == null) {
			dst=new short[expandBy][];
		}
		else {
			int n=src.length;
			dst = new short[n+expandBy][];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}
	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public int[] expandArray(int[] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public int[] expandArray(int[] src,int expandBy) {
		int[] dst;
		if (src == null) {
			dst=new int[expandBy];
		}
		else {
			int n=src.length;
			dst = new int[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}

	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public long[] expandArray(long[] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public long[] expandArray(long[] src,int expandBy) {
		long[] dst;
		if (src == null) {
			dst=new long[expandBy];
		}
		else {
			int n=src.length;
			dst = new long[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}
	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public float[] expandArray(float[] src) {
		return expandArray(src,1);
	}


	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public float[] expandArray(float[] src,int expandBy) {
		float[] dst;
		if (src == null) {
			dst=new float[expandBy];
		}
		else {
			int n=src.length;
			dst = new float[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}

	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public double[] expandArray(double[] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public double[] expandArray(double[] src,int expandBy) {
		double[] dst;
		if (src == null) {
			dst=new double[expandBy];
		}
		else {
			int n=src.length;
			dst = new double[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}

	/**
	 * <p>Expand an array by adding one element to the end.</p>
	 *
	 * @param	src	an array
	 * @return		an array that is one element longer
	 */
	static public String[] expandArray(String[] src) {
		return expandArray(src,1);
	}

	/**
	 * <p>Expand an array by adding elements to the end.</p>
	 *
	 * @param	src		an array
	 * @param	expandBy	the number of elements to add
	 * @return			an array that is longer
	 */
	static public String[] expandArray(String[] src,int expandBy) {
		String[] dst;
		if (src == null) {
			dst=new String[expandBy];
		}
		else {
			int n=src.length;
			dst = new String[n+expandBy];
			for (int j=0; j<n; ++j) {
				dst[j]=src[j];
			}
		}
		return dst;
	}

	/**
	 * <p>Compare two double arrays and return true if both not null, and are of equal length and contain equal values.</p>
	 *
	 * @param	a1	first array
	 * @param	a2	second array
	 * @return		true if equal
	 */
	static public boolean arraysAreEqual(double[] a1,double[] a2) {
//System.err.println("ArrayCopyUtilities.arraysAreEqual(): a1 = "+a1);
//System.err.println("ArrayCopyUtilities.arraysAreEqual(): a2 = "+a2);
		boolean result = true;
		if (a1 == null || a2 == null || a1.length != a2.length) {
			result = false;
		}
		else {
			for (int i=0; i<a1.length; ++i) {
				//if (a1[i] != a2[i]) {
				if (Math.abs(a1[i] - a2[i]) > 0.000001) {
					result = false;
					break;
				}
			}
		}
//System.err.println("ArrayCopyUtilities.arraysAreEqual(): "+result);
		return result;
	}

	/**
	 * <p>Find the minimum and maximum values in an array.</p>
	 *
	 * @param	array	the array
	 * @return			an array of two values, the first being the minimum and the second the maximum values found
	 */
	static public float[] minMax(float[] array) {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for (float v : array) {
			if (v < min) min = v;
			if (v > max) max = v;
		}
		float[] returnValues = { min, max };
		return returnValues;
	}

	/**
	 * <p>Find the minimum and maximum values in an array.</p>
	 *
	 * @param	array	the array
	 * @return			an array of two values, the first being the minimum and the second the maximum values found
	 */
	static public double[] minMax(double[] array) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (double v : array) {
			if (v < min) min = v;
			if (v > max) max = v;
		}
		double[] returnValues = { min, max };
		return returnValues;
	}
}


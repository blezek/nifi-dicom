/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Integer String (IS) attributes.</p>
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
public class IntegerStringAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/IntegerStringAttribute.java,v 1.18 2017/01/24 10:50:37 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 12;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public IntegerStringAttribute(AttributeTag t) {
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
	public IntegerStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public IntegerStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl.longValue(),i);
	}

	/**
	 * <p>Get the value representation of this attribute (IS).</p>
	 *
	 * @return	'I','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.IS; }

	public String[] getStringValues(NumberFormat format) throws DicomException {
		String sv[] = null;
		if (format == null) {
			sv=super.getStringValues((NumberFormat)null);
		}
		else {
			long[] v = getLongValues();
			if (v != null) {
				sv=new String[v.length];
				for (int j=0; j<v.length; ++j) {
					sv[j] = format.format(v[j]);
				}
			}
		}
		return sv;
	}

	// do not need to override addValue() for shorter binary integer arguments; super-class methods will never exceed 12 bytes or range -2^31 <= n <= (2^31 -  1).

	public void addValue(long v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}

	public void addValue(float v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}

	public void addValue(double v) throws DicomException {
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new DicomException("Value "+v+" out of range for Integer String");
		}
		else {
			addValue((int)v);
		}
	}
	
	//protected final boolean allowRepairOfIncorrectLength() { return true; }				// moot, since we do not check this in repairValues(), i.e., hard-coded
	
	//protected final boolean allowRepairOfInvalidCharacterReplacement() { return false; }	// moot, since we do not check this in repairValues(), i.e., hard-coded

	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isDigit(c) || c == ' ' || c == '+' || c == '-');
	}

	public boolean areValuesWellFormed() throws DicomException {
		boolean good = true;
		{
			long[] longValues = getLongValues();
			for (int i=0; i<longValues.length; ++i) {
				long v = longValues[i];
//System.err.println("IntegerStringAttribute.isValid(): Checking value ="+v+" (from String value "+originalValues[i]+", long value "+Long.parseLong(originalValues[i])+")");
				if (v < -2147483648l || v > 2147483647l) {
					good = false;
					break;
				}
			}
		}
		return good;
	}
	 
	public boolean repairValues() throws DicomException {
//System.err.println("IntegerStringAttribute.repairValues():");
		if (!isValid()) {
//System.err.println("IntegerStringAttribute.repairValues(): Invalid so attempting repair");
			flushCachedCopies();
			originalByteValues=null;
			if (originalValues != null && originalValues.length > 0) {
//System.err.println("IntegerStringAttribute.repairValues(): Attempting trim");
				// removing padding is the best we can do without loosing the meaning of the value ... may still be invalid :(
				// do not just use ArrayCopyUtilities.copyStringArrayRemovingLeadingAndTrailingPadding(originalValues), since it only handle space character
				for (int i=0; i<originalValues.length; ++i) {
					String v = originalValues[i];
					if (v != null && v.length() > 0) {
//System.err.println("IntegerStringAttribute.repairValues(): Attempting value \""+v+"\"");
						originalValues[i] = v.trim();
//System.err.println("IntegerStringAttribute.repairValues(): Result is \""+originalValues[i]+"\"");
					}
				}
			}
		}
		return isValid();
	}

	private static double[] testValues = {
		0,
		1,
		Double.MAX_VALUE,
		Double.MIN_VALUE,
		Float.MAX_VALUE,
		Float.MIN_VALUE,
		Long.MAX_VALUE,
		Long.MIN_VALUE,
		Integer.MAX_VALUE,
		Integer.MIN_VALUE,
		Short.MAX_VALUE,
		Short.MIN_VALUE,
		Byte.MAX_VALUE,
		Byte.MIN_VALUE
	};
	
	private static String[] testStringSupplied = {
		"0",
		"1",
		"Double.MAX_VALUE",
		"Double.MIN_VALUE",
		"Float.MAX_VALUE",
		"Float.MIN_VALUE",
		"9223372036854775807",
		"-9223372036854775808",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768",
		"127",
		"-128"
	};
		
	private static String[] testStringExpected = {
		"0",
		"1",
		"exception",
		"0",
		"exception",
		"0",
		"exception",
		"exception",
		"2147483647",
		"-2147483648",
		"32767",
		"-32768",
		"127",
		"-128"
	};
		
	/**
	 * <p>Test.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {
		System.err.println("Test IntegerString.addValue(double):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue(testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
		System.err.println("Test IntegerString.addValue(float):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue((float)testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
		System.err.println("Test IntegerString.addValue(long):");
		for (int i=0; i< testValues.length; ++i) {
			IntegerStringAttribute a = new IntegerStringAttribute(TagFromName.InstanceNumber);
			String sv = "";
			try {
				a.addValue((long)testValues[i]);
				sv = Attribute.getSingleStringValueOrEmptyString(a);
			}
			catch (DicomException e) {
				//System.err.println(e);
				sv = "exception";
			}
			int svl = sv.length();
			System.err.println("\t"+(sv.equals(testStringExpected[i]) && svl <= 12 ? "PASS" : "FAIL")+": Supplied <"+testStringSupplied[i]+">\t Got <"+sv+"> (length="+svl+")\t Expected <"+testStringExpected[i]+">\t Double.toString() <"+Double.toString(testValues[i])+">");
		}
	}
}


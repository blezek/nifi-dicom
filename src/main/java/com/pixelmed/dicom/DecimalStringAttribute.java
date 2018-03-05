/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Decimal String (DS) attributes.</p>
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
public class DecimalStringAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DecimalStringAttribute.java,v 1.18 2017/01/24 10:50:36 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 16;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public DecimalStringAttribute(AttributeTag t) {
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
	public DecimalStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public DecimalStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (DS).</p>
	 *
	 * @return	'D','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.DS; }

	/**
	 * @param	format			the format to use for each numerical or decimal value
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public String[] getStringValues(NumberFormat format) throws DicomException {
		String sv[] = null;
		if (format == null) {
			sv=super.getStringValues((NumberFormat)null);
		}
		else {
			double[] v = getDoubleValues();
			if (v != null) {
				sv=new String[v.length];
				for (int j=0; j<v.length; ++j) {
					sv[j] = format.format(v[j]);
				}
			}
		}
		return sv;
	}

	// do not need to override addValue() for shorter binary integer arguments; super-class methods will never exceed 16 bytes

	public void addValue(long v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,MAX_LENGTH_SINGLE_VALUE,false/*allowNonNumbers*/,Locale.US));
	}

	public void addValue(float v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,MAX_LENGTH_SINGLE_VALUE,false/*allowNonNumbers*/,Locale.US));
	}

	public void addValue(double v) throws DicomException {
		// need to make sure we create the highest precision value that is no more than 16 bytes (limit on DS)
		addValue(com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(v,MAX_LENGTH_SINGLE_VALUE,false/*allowNonNumbers*/,Locale.US));
	}

	//protected final boolean allowRepairOfIncorrectLength() { return true; }				// moot, since we do not check this in repairValues(), i.e., hard-coded
	
	//protected final boolean allowRepairOfInvalidCharacterReplacement() { return false; }	// moot, since we do not check this in repairValues(), i.e., hard-coded

	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isDigit(c) || c == ' ' || c == '+' || c == '-' || c == '.' || c == 'e' || c == 'E');
	}

	public boolean repairValues() throws DicomException {
//System.err.println("DecimalStringAttribute.repairValues():");
		if (!isValid()) {
			flushCachedCopies();
			originalByteValues=null;
			if (originalValues != null && originalValues.length > 0) {
				// do not just use ArrayCopyUtilities.copyStringArrayRemovingLeadingAndTrailingPadding(), since it only handle space character
				// do not just use copyStringToDoubleArray(), since if it fails on bad character it will return (valid) 0 value, rather than original bad value
				for (int i=0; i<originalValues.length; ++i) {
					String v = originalValues[i];
//System.err.println("DecimalStringAttribute.repairValues(): Attempting string value \""+v+"\"");
					if (v != null && v.length() > 0) {
						originalValues[i] = v.trim();
						try {
							double d = Double.parseDouble(originalValues[i]);
							originalValues[i] = com.pixelmed.utils.FloatFormatter.toStringOfFixedMaximumLength(d,MAX_LENGTH_SINGLE_VALUE,false/*allowNonNumbers*/,Locale.US);
						}
						catch (NumberFormatException e) {
						}
						catch (NullPointerException e) {
						}
					}
				}
			}
		}
		return isValid();
	}
	
	// for unit tests of addValue(), see com.pixelmed.utils.FloatFormatter.main()
}


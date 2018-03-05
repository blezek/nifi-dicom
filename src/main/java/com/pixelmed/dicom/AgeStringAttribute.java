/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Age String (AS) attributes.</p>
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
public class AgeStringAttribute extends StringAttribute {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AgeStringAttribute.java,v 1.16 2017/01/24 10:50:35 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 4;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public AgeStringAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	public AgeStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	public AgeStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (AS).</p>
	 *
	 * @return	'A','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.AS; }

	//protected final boolean allowRepairOfIncorrectLength() { return true; }				// moot, since we do not check this in repairValues(), i.e., hard-coded
	
	//protected final boolean allowRepairOfInvalidCharacterReplacement() { return false; }	// moot, since we do not check this in repairValues(), i.e., hard-coded

	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isDigit(c) || c == 'D' || c == 'W' || c == 'M' || c == 'Y');
	}
	
	public boolean areValuesWellFormed() throws DicomException {
		boolean good = true;
		if (originalValues != null && originalValues.length > 0) {
			for (int i=0; i<originalValues.length; ++i) {
				String v = originalValues[i];
				if (v != null && v.length() > 0) {
					if (v.length() != 4) {
						good = false;
						break;
					}
					else if (!v.endsWith("D") && !v.endsWith("W") &&!v.endsWith("M") &&!v.endsWith("Y")) {
						good = false;
						break;
					}
					else {
						String digits = v.substring(0,3);
						for (int j=0; j<digits.length(); ++j) {
							int c = v.codePointAt(j);
							if (!Character.isDigit(c)) {
								good = false;
								break;
							}
						}
						if (!good) {
							break;
						}
					}
				}
			}
		}
		return good;
	}
	 
	public boolean repairValues() throws DicomException {
		if (!isValid()) {
			flushCachedCopies();
			originalByteValues=null;
			if (originalValues != null && originalValues.length > 0) {
				// removing padding is the best we can do without loosing the meaning of the value ... may still be invalid :(
				// do not just use ArrayCopyUtilities.copyStringArrayRemovingLeadingAndTrailingPadding(originalValues), since it only handle space character
				for (int i=0; i<originalValues.length; ++i) {
					String v = originalValues[i];
					if (v != null && v.length() > 0) {
						originalValues[i] = v.trim();
					}
				}
			}
		}
		return isValid();
	}

}


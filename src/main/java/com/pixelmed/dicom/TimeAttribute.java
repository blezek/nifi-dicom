/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Time (TM) attributes.</p>
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
public class TimeAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/TimeAttribute.java,v 1.14 2017/01/24 10:50:39 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 14;	// actually 13, but allow even padding (standard incorrectly says 16); assuming not being used for query range matching, in which case it would be 28 :(
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public TimeAttribute(AttributeTag t) {
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
	public TimeAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public TimeAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl.longValue(),i);
	}

	/**
	 * <p>Get the value representation of this attribute (TM).</p>
	 *
	 * @return	'T','M' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.TM; }

	protected final boolean allowRepairOfIncorrectLength() { return false; }				// do not allow truncation
	
	protected final boolean allowRepairOfInvalidCharacterReplacement() { return true; }		// do not check this in repairValues(), i.e., hard-coded
	
	protected char getInvalidCharacterReplacement() { return 0; }							// remove invalid characters; this may allow old ACR-NEMA use of periods, or non-standard use of forward slash or hyphen to work

	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isDigit(c) || c == '.' || c == ' ');
	}

	public boolean areValuesWellFormed() throws DicomException {
		boolean good = true;
		if (originalValues != null && originalValues.length > 0) {
			for (int i=0; i<originalValues.length; ++i) {
				String v = originalValues[i];
				if (v != null && v.length() > 0) {
					if (v.length() < 2) {		// HH nust be present
						good = false;
						break;
					}
				}
			}
		}
		return good;
	}

}


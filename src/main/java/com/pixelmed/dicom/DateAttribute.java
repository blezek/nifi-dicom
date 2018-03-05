/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Date (DT) attributes.</p>
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
public class DateAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DateAttribute.java,v 1.14 2017/01/24 10:50:36 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 8;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public DateAttribute(AttributeTag t) {
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
	public DateAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public DateAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (DA).</p>
	 *
	 * @return	'D','A' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.DA; }

	protected final boolean allowRepairOfIncorrectLength() { return false; }				// do not allow truncation
	
	protected final boolean allowRepairOfInvalidCharacterReplacement() { return true; }		// do not check this in repairValues(), i.e., hard-coded
	
	protected char getInvalidCharacterReplacement() { return 0; }							// remove invalid characters; this may allow old ACR-NEMA use of periods, or non-standard use of forward slash or hyphen to work

	public final boolean isCharacterInValueValid(int c) throws DicomException {
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && Character.isDigit(c);
	}

	public boolean areValuesWellFormed() throws DicomException {
		boolean good = true;
		if (originalValues != null && originalValues.length > 0) {
			for (int i=0; i<originalValues.length; ++i) {
				String v = originalValues[i];
				if (v != null && v.length() > 0) {
					if (v.length() != 8) {
						good = false;
						break;
					}
				}
			}
		}
		return good;
	}
	
}


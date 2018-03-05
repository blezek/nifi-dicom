/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Code String (CS) attributes.</p>
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
public class CodeStringAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CodeStringAttribute.java,v 1.14 2017/01/24 10:50:36 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 16;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public CodeStringAttribute(AttributeTag t) {
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
	public CodeStringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public CodeStringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (CS).</p>
	 *
	 * @return	'C','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.CS; }

	protected final boolean allowRepairOfIncorrectLength() {
//System.err.println("CodeStringAttribute.allowRepairOfIncorrectLength():");
		return false;
	}
	
	protected final boolean allowRepairOfInvalidCharacterReplacement() {
//System.err.println("CodeStringAttribute.allowRepairOfInvalidCharacterReplacement():");
		return false;
	}

	public final boolean isCharacterInValueValid(int c) throws DicomException {
//System.err.println("CodeStringAttribute.isCharacterInValueValid(): c = "+c);
		return c < 0x7f /* ASCII only to limit Character.isXX() tests */ && (Character.isUpperCase(c) || Character.isDigit(c) || c == ' ' || c == '_');
	}

}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Short String (SH) attributes.</p>
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
public class ShortStringAttribute extends StringAttributeAffectedBySpecificCharacterSet {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ShortStringAttribute.java,v 1.16 2017/01/24 10:50:38 dclunie Exp $";

	protected static final int MAX_LENGTH_SINGLE_VALUE = 16;
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t			the tag of the attribute
	 */
	public ShortStringAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	public ShortStringAttribute(AttributeTag t,SpecificCharacterSet specificCharacterSet) {
		super(t,specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public ShortStringAttribute(AttributeTag t,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t,vl,i,specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public ShortStringAttribute(AttributeTag t,Long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t,vl.longValue(),i,specificCharacterSet);
	}

	/**
	 * <p>Get the value representation of this attribute (SH).</p>
	 *
	 * @return	'S','H' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.SH; }

}



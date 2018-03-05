/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Long (OL) attributes.</p>
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
public class OtherLongAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherLongAttribute.java,v 1.3 2017/01/24 10:50:38 dclunie Exp $";

	private int[] values;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherLongAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherLongAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherLongAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i);
	}

	/**
	 * @param	vl
	 * @param	i
	 * @throws	IOException
	 * @throws	DicomException
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i) throws IOException, DicomException {
		values=null;
		valueLength=vl;

		if (vl > 0) {
			int len = (int)(vl/4);
			int buffer[] = new int[len];
			i.readUnsigned32(buffer,len);
			setValues(buffer);
		}
	}

	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		if (values != null && values.length > 0) {
			o.writeUnsigned32(values,values.length);
			if (getVL() != values.length*4) {
				throw new DicomException("Internal error - int array length ("+values.length*4+") not equal to expected VL("+getVL()+")");
			}
		}
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" []");		// i.e. don't really dump values ... too many
		return str.toString();
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void setValues(int[] v) throws DicomException {
		values=v;
		valueMultiplicity=1;		// different from normal value types where VM is size of array
		valueLength=v.length*4;
	}

	/**
	 */
	public void removeValues() {
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * @throws	DicomException
	 */
	public int[] getIntegerValues() throws DicomException { return values; }

	/**
	 * <p>Get the value representation of this attribute (OL).</p>
	 *
	 * @return	'O','L' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OL; }

}


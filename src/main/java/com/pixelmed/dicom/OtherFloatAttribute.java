/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Float (OF) attributes.</p>
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
public class OtherFloatAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherFloatAttribute.java,v 1.12 2017/01/24 10:50:38 dclunie Exp $";

	private float[] values;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherFloatAttribute(AttributeTag t) {
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
	public OtherFloatAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public OtherFloatAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
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
			float buffer[] = new float[len];
			i.readFloat(buffer,len);
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
			o.writeFloat(values,values.length);
			if (getVL() != values.length*4) {
				throw new DicomException("Internal error - float array length ("+values.length*2+") not equal to expected VL("+getVL()+")");
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
	public void setValues(float[] v) throws DicomException {
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
	public float[] getFloatValues() throws DicomException { return values; }

	/**
	 * <p>Get the value representation of this attribute (OF).</p>
	 *
	 * @return	'O','F' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OF; }

}


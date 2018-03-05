/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;
import java.util.Locale;

import com.pixelmed.utils.FloatFormatter;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Float Single (FL) attributes.</p>
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
public class FloatSingleAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/FloatSingleAttribute.java,v 1.25 2017/01/24 10:50:37 dclunie Exp $";

	float[] values;

	static int bytesPerValue=4;

	private void flushCachedCopies() {
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public FloatSingleAttribute(AttributeTag t) {
		super(t);
		flushCachedCopies();
		values=null;
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
	public FloatSingleAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i);
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
	public FloatSingleAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i);
	}

	/**
	 * @param	vl
	 * @param	i
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i) throws IOException, DicomException {
		flushCachedCopies();
		if (vl%bytesPerValue != 0) {
			throw new DicomException("incorrect value length ("+vl+" dec) for VR "+getVRAsString()+" - caller will need to skip value length bytes to get to next data element");
		}
		else {
			int vm=(int)(vl/bytesPerValue);
			values=null;
			for (int j=0; j<vm; ++j) addValue(i.readFloat());
		}
	}

	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		float[] v = getFloatValues();
		if (v != null) {
			for (int j=0; j<v.length; ++j) {
				o.writeFloat(v[j]);
			}
		}
	}
	
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" [");
		try {
			float[] v = getFloatValues();
			if (v != null) {
				for (int j=0; j<v.length; ++j) {
					if (j > 0) str.append(",");
					str.append(v[j]);
				}
			}
		}
		catch (DicomException e) {
			str.append("XXXX");
		}
		str.append("]");
		return str.toString();

	}


	public String[] getStringValues(NumberFormat format) throws DicomException {
		String sv[] = null;
		float[] v = getFloatValues();
		if (v != null) {
			sv=new String[v.length];
			for (int j=0; j<v.length; ++j) {
				sv[j] = (format == null) ? FloatFormatter.toString(v[j],Locale.US) : format.format(v[j]);
			}
		}
		return sv;
	}

	public float[] getFloatValues() throws DicomException {
		return values;
	}

	public void addValue(float v) throws DicomException {
		flushCachedCopies();
		values=ArrayCopyUtilities.expandArray(values);
		values[valueMultiplicity++]=v;
		valueLength+=4;
	}

	public void addValue(double v) throws DicomException {
		addValue((float)v);
	}

	public void addValue(short v) throws DicomException {
		addValue((float)v);
	}

	public void addValue(int v) throws DicomException {
		addValue((float)v);
	}

	public void addValue(long v) throws DicomException {
		addValue((float)v);
	}

	public void addValue(String v) throws DicomException {
		float floatValue = 0;
		try {
			floatValue=(float)Double.parseDouble(v);
		}
		catch (NumberFormatException e) {
			throw new DicomException(e.toString());
		}
		addValue(floatValue);
	}

	public void removeValues() {
		flushCachedCopies();
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * <p>Get the value representation of this attribute (FL).</p>
	 *
	 * @return	'F','L' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.FL; }

}


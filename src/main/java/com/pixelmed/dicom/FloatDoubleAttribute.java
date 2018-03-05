/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import com.pixelmed.utils.FloatFormatter;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Float Double (FD) attributes.</p>
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
public class FloatDoubleAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/FloatDoubleAttribute.java,v 1.25 2017/01/24 10:50:37 dclunie Exp $";

	double[] values;

	static int bytesPerValue=8;

	private void flushCachedCopies() {
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public FloatDoubleAttribute(AttributeTag t) {
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
	public FloatDoubleAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public FloatDoubleAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
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
			for (int j=0; j<vm; ++j) addValue(i.readDouble());
		}
	}

	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		double[] v = getDoubleValues();
		if (v != null) {
			for (int j=0; j<v.length; ++j) {
				o.writeDouble(v[j]);
			}
		}
	}
	
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" [");
		try {
			double[] v = getDoubleValues();
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
		double[] v = getDoubleValues();
		if (v != null) {
			sv=new String[v.length];
			for (int j=0; j<v.length; ++j) {
				sv[j] = (format == null) ? FloatFormatter.toString(v[j],Locale.US) : format.format(v[j]);
			}
		}
		return sv;
	}

	public double[] getDoubleValues() throws DicomException {
		return values;
	}

	public void addValue(double v) throws DicomException {
		flushCachedCopies();
		values=ArrayCopyUtilities.expandArray(values);
		values[valueMultiplicity++]=v;
		valueLength+=8;
	}

	public void addValue(float v) throws DicomException {
		addValue((double)v);
	}

	public void addValue(short v) throws DicomException {
		addValue((double)v);
	}

	public void addValue(int v) throws DicomException {
		addValue((double)v);
	}

	public void addValue(long v) throws DicomException {
		addValue((double)v);
	}

	public void addValue(String v) throws DicomException {
		flushCachedCopies();
		double doubleValue = 0;
		try {
			doubleValue=Double.parseDouble(v);
		}
		catch (NumberFormatException e) {
			throw new DicomException(e.toString());
		}
		addValue(doubleValue);
	}

	public void removeValues() {
		flushCachedCopies();
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * <p>Get the value representation of this attribute (FD).</p>
	 *
	 * @return	'F','D' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.FD; }

}


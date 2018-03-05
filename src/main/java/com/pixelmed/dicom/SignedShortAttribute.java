/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Signed Short (SS) attributes.</p>
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
public class SignedShortAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SignedShortAttribute.java,v 1.23 2017/01/24 10:50:38 dclunie Exp $";

	short[] values;
	int[] cachedIntegerCopy;
	long[] cachedLongCopy;
	float[] cachedFloatCopy;
	double[] cachedDoubleCopy;

	static int bytesPerValue=2;

	private void flushCachedCopies() {
		cachedIntegerCopy=null;
		cachedLongCopy=null;
		cachedFloatCopy=null;
		cachedDoubleCopy=null;
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public SignedShortAttribute(AttributeTag t) {
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
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public SignedShortAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public SignedShortAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
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
		flushCachedCopies();
		if (vl%bytesPerValue != 0) {
			throw new DicomException("incorrect value length ("+vl+" dec) for VR "+getVRAsString()+" - caller will need to skip value length bytes to get to next data element");
		}
		else {
			int vm=(int)(vl/bytesPerValue);
			values=null;
			for (int j=0; j<vm; ++j) addValue((short)(i.readSigned16()));
		}
	}

	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		short[] v = getShortValues();
		if (v != null) {
			for (int j=0; j<v.length; ++j) {
				o.writeSigned16(v[j]);
			}
		}
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" [");
		try {
			//short[] v = getShortValues();
			int[] v = getIntegerValues();
			if (v != null) {
				for (int j=0; j<v.length; ++j) {
					if (j > 0) str.append(",");
					str.append("0x");
					str.append(Integer.toHexString(v[j]));
				}
			}
		}
		catch (DicomException e) {
			str.append("XXXX");
		}
		str.append("]");
		return str.toString();
	}

         /**
	 * @param	format		the format to use for each numerical or decimal value
         * @throws	DicomException
         */
        public String[] getStringValues(NumberFormat format) throws DicomException {
		String sv[] = null;
		int[] v = getIntegerValues();
		if (v != null) {
			sv=new String[v.length];
			for (int j=0; j<v.length; ++j) {
				sv[j] = (format == null) ? Integer.toString(v[j]) : format.format(v[j]);
			}
		}
		return sv;
	}

	/**
	 * @throws	DicomException
	 */
	public short[] getShortValues() throws DicomException {
		return values;
	}

	/**
	 * @throws	DicomException
	 */
	public int[] getIntegerValues() throws DicomException {
		if (cachedIntegerCopy == null) cachedIntegerCopy=ArrayCopyUtilities.copySignedShortToIntArray(values);
		return cachedIntegerCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public long[] getLongValues() throws DicomException {
		if (cachedLongCopy == null) cachedLongCopy=ArrayCopyUtilities.copySignedShortToLongArray(values);
		return cachedLongCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public float[] getFloatValues() throws DicomException {
		if (cachedFloatCopy == null) cachedFloatCopy=ArrayCopyUtilities.copySignedShortToFloatArray(values);
		return cachedFloatCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public double[] getDoubleValues() throws DicomException {
		if (cachedDoubleCopy == null) cachedDoubleCopy=ArrayCopyUtilities.copySignedShortToDoubleArray(values);
		return cachedDoubleCopy;
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(short v) throws DicomException {
		flushCachedCopies();
		values=ArrayCopyUtilities.expandArray(values);
		values[valueMultiplicity++]=v;
		valueLength+=2;
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(int v) throws DicomException {
		addValue((short)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(long v) throws DicomException {
		addValue((short)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(float v) throws DicomException {
		addValue((short)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(double v) throws DicomException {
		addValue((short)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(String v) throws DicomException {
		short shortValue = 0;
		try {
			shortValue=(short)Integer.parseInt(v);
		}
		catch (NumberFormatException e) {
			throw new DicomException(e.toString());
		}
		addValue(shortValue);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void setValues(short[] v) throws DicomException {
		values=v;
		valueMultiplicity=v.length;
		valueLength=v.length*2;
		flushCachedCopies();
	}

	/**
	 */
	public void removeValues() {
		flushCachedCopies();
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}
	
	/**
	 * <p>Get the value representation of this attribute (SS).</p>
	 *
	 * @return	'S','S' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.SS; }

}


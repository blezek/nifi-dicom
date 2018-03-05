/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Unsigned Long (UL) attributes.</p>
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
public class UnsignedLongAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/UnsignedLongAttribute.java,v 1.24 2017/01/24 10:50:39 dclunie Exp $";

	short[] cachedShortCopy;
	int[] values;
	long[] cachedLongCopy;
	float[] cachedFloatCopy;
	double[] cachedDoubleCopy;

	static int bytesPerValue=4;

	private void flushCachedCopies() {
		cachedShortCopy=null;
		cachedLongCopy=null;
		cachedFloatCopy=null;
		cachedDoubleCopy=null;
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public UnsignedLongAttribute(AttributeTag t) {
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
	public UnsignedLongAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
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
	public UnsignedLongAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
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
			for (int j=0; j<vm; ++j) addValue((int)(i.readUnsigned32()));
		}
	}

	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		int[] v = getIntegerValues();
		if (v != null) {
			for (int j=0; j<v.length; ++j) {
				o.writeUnsigned32(v[j]);
			}
		}
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" [");
		try {
			long[] v = getLongValues();
			if (v != null) {
				for (int j=0; j<v.length; ++j) {
					if (j > 0) str.append(",");
					str.append("0x");
					str.append(Long.toHexString(v[j]));
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
		long[] v = getLongValues();
		if (v != null) {
			sv=new String[v.length];
			for (int j=0; j<v.length; ++j) {
				sv[j] = (format == null) ? Long.toString(v[j]) : format.format(v[j]);
			}
		}
		return sv;
	}

	/**
	 * @throws	DicomException
	 */
	public short[] getShortValues() throws DicomException {
		if (cachedShortCopy == null) cachedShortCopy=ArrayCopyUtilities.copyUnsignedIntToShortArray(values);
		return cachedShortCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public int[] getIntegerValues() throws DicomException {
		return values;
	}

	/**
	 * @throws	DicomException
	 */
	public long[] getLongValues() throws DicomException {
		if (cachedLongCopy == null) cachedLongCopy=ArrayCopyUtilities.copyUnsignedIntToLongArray(values);
		return cachedLongCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public float[] getFloatValues() throws DicomException {
		if (cachedFloatCopy == null) cachedFloatCopy=ArrayCopyUtilities.copyUnsignedIntToFloatArray(values);
		return cachedFloatCopy;
	}

	/**
	 * @throws	DicomException
	 */
	public double[] getDoubleValues() throws DicomException {
		if (cachedDoubleCopy == null) cachedDoubleCopy=ArrayCopyUtilities.copyUnsignedIntToDoubleArray(values);
		return cachedDoubleCopy;
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(short v) throws DicomException {
		addValue((int)v);		// should we sign extend or not ? :(
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(int v) throws DicomException {
		flushCachedCopies();
		values=ArrayCopyUtilities.expandArray(values);
		values[valueMultiplicity++]=v;
		valueLength+=4;
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(long v) throws DicomException {
		addValue((int)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(float v) throws DicomException {
		addValue((int)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(double v) throws DicomException {
		addValue((int)v);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void addValue(String v) throws DicomException {
		int intValue = 0;
		try {
			intValue=(int)Long.parseLong(v);
		}
		catch (NumberFormatException e) {
			throw new DicomException(e.toString());
		}
		addValue(intValue);
	}

	/**
	 * @param	v
	 * @throws	DicomException
	 */
	public void setValues(int[] v) throws DicomException {
		values=v;
		valueMultiplicity=v.length;
		valueLength=v.length*4;
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
	 * <p>Get the value representation of this attribute (UL).</p>
	 *
	 * @return	'U','L' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.UL; }

}


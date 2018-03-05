/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Word (OW) attributes whose values are an array of shorts per frame rather than a single
 * contiguous array containing all frames.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class OtherWordAttributeMultipleFrameArrays extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherWordAttributeMultipleFrameArrays.java,v 1.3 2017/01/24 10:50:38 dclunie Exp $";
	
	private short[][] values;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherWordAttributeMultipleFrameArrays(AttributeTag t) {
		super(t);
	}

	/***/
	public long getPaddedVL() {
		long vl = getVL();
		if (vl%2 != 0) ++vl;
		return vl;
	}
	
	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		if (values != null && values.length > 0) {
//System.err.println("OtherWordAttributeMultipleFrameArrays.write(): number of frames = "+values.length);
			for (int f=0; f<values.length; ++f) {
//System.err.println("OtherWordAttributeMultipleFrameArrays.write(): frame = "+f);
//System.err.println("OtherWordAttributeMultipleFrameArrays.write(): values["+f+"] length = "+values[f].length+" words");
				o.writeUnsigned16(values[f],values[f].length);
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
	public void setValuesPerFrame(short[][] v) throws DicomException {
//System.err.println("OtherWordAttributeMultipleFrameArrays.setValuesPerFrame(): number of frames = "+(v == null ? 0 : v.length));
		if (v == null) {
			values=null;
			valueMultiplicity=0;
			valueLength=0;
		}
		else {
			int framesize = 0;
			// actually copy the array - caller might null it's contents later :(
			values = new short[v.length][];
			for (int f=0; f<v.length; ++f) {
				values[f] = v[f];
//System.err.println("OtherWordAttributeMultipleFrameArrays.setValuesPerFrame(): values["+f+"] = "+values[f]);
				if (framesize == 0) {
					framesize = v[f].length;
				}
				else if (framesize != v[f].length) {
					throw new DicomException("Frame short arrays are not same length - have "+v[f].length+" dec shorts for frame "+f+" does not match earlier frame "+framesize+" dec shorts");
				}
			}
			values=v;
			valueMultiplicity=1;				// different from normal value types where VM is size of array
			valueLength=v.length * framesize * 2;
		}
	}

	/**
	 * <p>Get the values of this attribute as multiple short arrays, one per frame.</p>
	 *
	 * <p>Caller does not need to supply the number for frames since known when this attribute was created.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the short values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @return					the values as an array of arrays of shorts
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[][] getShortValuesPerFrame() throws DicomException {
//System.err.println("OtherWordAttributeMultipleFrameArrays.getShortValuesPerFrame()");
		// actually copy the array - caller might null it's contents later :(
		short[][] v = new short[values.length][];
		for (int f=0; f<values.length; ++f) {
			v[f] = values[f];
		}
		return v;
	}

	/**
	 */
	public void removeValues() {
//System.err.println("OtherWordAttributeMultipleFrameArrays.removeValues()");
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * <p>Get the value representation of this attribute (OW).</p>
	 *
	 * @return	'O','W' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OW; }
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Byte (OB) attributes whose values are an array of bytes per frame rather than a single
 * contiguous array containing all frames.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class OtherByteAttributeMultipleFrameArrays extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherByteAttributeMultipleFrameArrays.java,v 1.4 2017/01/24 10:50:38 dclunie Exp $";
	
	private byte[][] values;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherByteAttributeMultipleFrameArrays(AttributeTag t) {
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
//System.err.println("OtherByteAttributeMultipleFrameArrays.write(): number of frames = "+values.length);
			long countBytesWritten = 0;
			for (int f=0; f<values.length; ++f) {
//System.err.println("OtherByteAttributeMultipleFrameArrays.write(): frame = "+f);
//System.err.println("OtherByteAttributeMultipleFrameArrays.write(): values["+f+"] = "+values[f]);
				o.write(values[f]);
				countBytesWritten+=values[f].length;
			}
			long npad = getPaddedVL() - countBytesWritten;
			while (npad-- > 0) o.write(0x00);
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
	public void setValuesPerFrame(byte[][] v) throws DicomException {
//System.err.println("OtherByteAttributeMultipleFrameArrays.setValuesPerFrame(): number of frames = "+(v == null ? 0 : v.length));
		if (v == null) {
			values=null;
			valueMultiplicity=0;
			valueLength=0;
		}
		else {
			int framesize = 0;
			// actually copy the array - caller might null it's contents later :(
			values = new byte[v.length][];
			for (int f=0; f<v.length; ++f) {
				values[f] = v[f];
//System.err.println("OtherByteAttributeMultipleFrameArrays.setValuesPerFrame(): values["+f+"] = "+values[f]);
				if (framesize == 0) {
					framesize = v[f].length;
				}
				else if (framesize != v[f].length) {
					throw new DicomException("Frame byte arrays are not same length - have "+v[f].length+" dec bytes for frame "+f+" does not match earlier frame "+framesize+" dec bytes");
				}
			}
			values=v;
			valueMultiplicity=1;				// different from normal value types where VM is size of array
			valueLength=v.length * framesize;
		}
	}

	/**
	 * <p>Get the values of this attribute as multiple byte arrays, one per frame.</p>
	 *
	 * <p>Caller does not need to supply the number for frames since known when this attribute was created.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the byte values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @return					the values as an array of arrays of bytes
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public byte[][] getByteValuesPerFrame() throws DicomException {
//System.err.println("OtherByteAttributeMultipleFrameArrays.getByteValuesPerFrame()");
		// actually copy the array - caller might null it's contents later :(
		byte[][] v = new byte[values.length][];
		for (int f=0; f<values.length; ++f) {
			v[f] = values[f];
		}
		return v;
	}

	/**
	 */
	public void removeValues() {
//System.err.println("OtherByteAttributeMultipleFrameArrays.removeValues()");
		values=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * <p>Get the value representation of this attribute (OB).</p>
	 *
	 * @return	'O','B' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OB; }
}


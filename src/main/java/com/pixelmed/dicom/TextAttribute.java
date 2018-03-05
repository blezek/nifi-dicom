/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>An abstract class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * the family of text attributes.</p>
 *
 * @author	dclunie
 */
abstract public class TextAttribute extends Attribute {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/TextAttribute.java,v 1.22 2017/01/24 10:50:39 dclunie Exp $";

	public abstract int getMaximumLengthOfEntireValue();

	/***/
	protected SpecificCharacterSet specificCharacterSet;

	/***/
	String values[];

	/**
	 * <p>Decode a byte array into a string.</p>
	 *
	 * @param	bytes	the byte buffer in which the encoded string is located
	 * @param	offset	the offset into the buffer
	 * @param	length	the number of bytes to be decoded
	 * @return		the string decoded according to the specified or default specific character set
	 */
	protected String translateByteArrayToString(byte[] bytes,int offset,int length) {	// NOT static
		return specificCharacterSet == null ? new String(bytes,0,length) : specificCharacterSet.translateByteArrayToString(bytes,0,length);
	}

	/**
	 * <p>Encode a string into a byte array.</p>
	 *
	 * @param	string							the string to be encoded
	 * @return									the byte array encoded according to the specified or default specific character set
	 * @throws	UnsupportedEncodingException	if the encoding is not supported by the host platform
	 */
	protected byte[] translateStringToByteArray(String string) throws UnsupportedEncodingException {	// NOT static
		return specificCharacterSet == null ? string.getBytes() : specificCharacterSet.translateStringToByteArray(string);
	}

	/**
	 * <p>Construct an (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	protected TextAttribute(AttributeTag t) {
		super(t);
		doCommonConstructorStuff(null);
	}

	/**
	 * <p>Construct an (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	protected TextAttribute(AttributeTag t,SpecificCharacterSet specificCharacterSet) {
		super(t);
		doCommonConstructorStuff(specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	protected TextAttribute(AttributeTag t,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i,specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	protected TextAttribute(AttributeTag t,Long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i,specificCharacterSet);
	}

	/**
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	private void doCommonConstructorStuff(SpecificCharacterSet specificCharacterSet) {
		values=null;
		this.specificCharacterSet=specificCharacterSet;
	}
	
	/**
	 * @param	vl
	 * @param	i
	 * @param	specificCharacterSet
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		doCommonConstructorStuff(specificCharacterSet);
		if (vl > 0) {
			byte[] buffer = new byte[(int)vl];
			try {
				i.readInsistently(buffer,0,(int)vl);
			}
			catch (IOException e) {
				throw new DicomException("Failed to read value (length "+vl+" dec) in "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
			String sbuf = translateByteArrayToString(buffer,0,(int)vl);
			vl=sbuf.length();	// NB. this only makes a difference for multi-byte character sets
			addValue(sbuf);
		}
	}

	public long getPaddedVL() {
		long vl = getVL();
		if (vl%2 != 0) ++vl;
		return vl;
	}
	
	/**
	 * <p>Get the appropriate byte for padding a string to an even length.</p>
	 *
	 * @return	the byte pad value appropriate to the VR
	 */
	private byte getPadByte() { return 0x20; }

	/**
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private byte[] getPaddedByteValues() throws DicomException {
		String[] v = getStringValues();
		//byte[] b = v == null ? null : v[0].getBytes();
		byte[] b = null;
		try {
			if (v != null) b = translateStringToByteArray(v[0]);
		}
		catch (UnsupportedEncodingException e) {
			throw new DicomException("Unsupported encoding:"+e);
		}
		// should padding take into account character set, i.e. could the pad character be different ? :(
		if (b != null) {
			int bl = b.length;
			if (bl%2 != 0) {
				byte[] b2 = new byte[bl+1];
				System.arraycopy(b,0,b2,0,bl);
				b2[bl]=getPadByte();
				b=b2;
			}
			if (getPaddedVL() != b.length) {
				throw new DicomException("Internal error - byte array length not equal to expected padded VL");
			}
		}
		return b;
	}

	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		byte b[] = getPaddedByteValues();
		if (b != null && b.length > 0) o.write(b);
	}
	
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" <");
		try {
			String[] v = getStringValues();
			if (v != null) str.append(v[0]);
		}
		catch (DicomException e) {
			str.append("XXXX");
		}
		str.append(">");
		return str.toString();
	}

	public byte[]   getByteValues() throws DicomException {
		//return originalByteValues == null ? getPaddedByteValues() : originalByteValues;
		return getPaddedByteValues();
	}

	public String[] getStringValues(NumberFormat format) throws DicomException {
		// ignore number format for generic text attributes
		return values;
	}

	public void addValue(String v) throws DicomException {
		if (values != null || valueMultiplicity > 0) throw new DicomException("No more than one value allowed for text attributes");
		values=new String[1];
		values[0]=v;
		try {
			valueLength=translateStringToByteArray(v).length;		// note that this is inefficient, and the translated bytes should be cached, but is expedient to avoid including padding at this moment :(
		}
		catch (UnsupportedEncodingException e) {
			throw new DicomException(e.toString());
		}
		++valueMultiplicity;
	}

	public void removeValues() throws DicomException {
		valueLength=0;
		valueMultiplicity=0;
		values=null;
	}

	public boolean isValid() throws DicomException {
		boolean good = true;
		if (values != null && values.length > 0) {
			if (values.length > 1) {
				throw new DicomException("Internal error - no more than one value allowed for text attributes");	// should never happen
			}
			String v = values[0];
			if (v != null && v.length() > getMaximumLengthOfEntireValue()) {
				good = false;
			}
		}
		return good;
	}
	 
	public boolean repairValues() throws DicomException {
		if (!isValid()) {
			if (values != null && values.length > 0) {
				String v = values[0];
				if (v != null && v.length() > getMaximumLengthOfEntireValue()) {
					v = v.substring(0,getMaximumLengthOfEntireValue()).trim();	// trim it because truncation may expose embedded spaces that are now trailing
					values[0] = v;
				}
			}
		}
		return isValid();
	}
}


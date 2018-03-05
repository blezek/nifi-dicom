/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.text.NumberFormat;

/**
 * <p>An abstract class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * the family of string attributes.</p>
 *
 * @author	dclunie
 */
abstract public class StringAttribute extends Attribute {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StringAttribute.java,v 1.31 2017/01/24 10:50:39 dclunie Exp $";

	public abstract int getMaximumLengthOfSingleValue();

	/***/
	protected SpecificCharacterSet specificCharacterSet;	// always null except for derived classes

	/**
	 * <p>Get the specific character set for this attribute.</p>
	 *
	 * @return		the specific character set, or null if none
	 */
	public SpecificCharacterSet getSpecificCharacterSet() { return specificCharacterSet; }

	/**
	 * <p>Set the specific character set for this attribute.</p>
	 *
	 * @param	specificCharacterSet	the specific character set, or null if none
	 */
	public void setSpecificCharacterSet(SpecificCharacterSet specificCharacterSet) { this.specificCharacterSet = specificCharacterSet; }

	/***/
	byte[] originalByteValues;
	/***/
	String originalValues[];
	/***/
	String cachedUnpaddedStringCopy[];
	/***/
	short[] cachedShortCopy;
	/***/
	int[] cachedIntegerCopy;
	/***/
	long[] cachedLongCopy;
	/***/
	float[] cachedFloatCopy;
	/***/
	double[] cachedDoubleCopy;
	/***/
	byte[] cachedPaddedByteValues;

	/***/
	protected void flushCachedCopies() {
		cachedUnpaddedStringCopy=null;
		cachedShortCopy=null;
		cachedIntegerCopy=null;
		cachedLongCopy=null;
		cachedFloatCopy=null;
		cachedDoubleCopy=null;
		cachedPaddedByteValues=null;
	}

	/**
	 * <p>Decode a byte array into a string.</p>
	 *
	 * @param	bytes	the byte buffer in which the encoded string is located
	 * @param	offset	the offset into the buffer
	 * @param	length	the number of bytes to be decoded
	 * @return		the string decoded according to the specified or default specific character set
	 */
	protected String translateByteArrayToString(byte[] bytes,int offset,int length) {	// NOT static
//System.err.println("StringAttribute.translateByteArrayToString()");
//System.err.println("StringAttribute.translateByteArrayToString() - specificCharacterSet is "+specificCharacterSet);
		return specificCharacterSet == null ? new String(bytes,0,length) : specificCharacterSet.translateByteArrayToString(bytes,0,length);
	}

	/**
	 * <p>Encode a string into a byte array.</p>
	 *
	 * @param	string				the string to be encoded
	 * @return					the byte array encoded according to the specified or default specific character set
	 * @throws	UnsupportedEncodingException	if the encoding is not supported
	 */
	protected byte[] translateStringToByteArray(String string) throws UnsupportedEncodingException {	// NOT static
//System.err.println("StringAttribute.translateStringToByteArray() - string is <"+string+">");
//System.err.println("StringAttribute.translateStringToByteArray() - string is "+com.pixelmed.utils.StringUtilities.dump(string));
//System.err.println("StringAttribute.translateStringToByteArray() - specificCharacterSet is "+specificCharacterSet);
		byte[] b = specificCharacterSet == null ? string.getBytes() : specificCharacterSet.translateStringToByteArray(string);
//System.err.println("StringAttribute.translateStringToByteArray(): return byte array is:\n"+com.pixelmed.utils.HexDump.dump(b));
		return b;
	}

	/**
	 * <p>Construct an (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	protected StringAttribute(AttributeTag t) {
		super(t);
		doCommonConstructorStuff(null);
	}

	/**
	 * <p>Construct an (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	protected StringAttribute(AttributeTag t,SpecificCharacterSet specificCharacterSet) {
		super(t);
		doCommonConstructorStuff(specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	protected StringAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i,null);
	}


	/**
	 * <p>Read an attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	protected StringAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i,null);
	}
	
	/**
	 * <p>Read an attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	protected StringAttribute(AttributeTag t,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
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
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	protected StringAttribute(AttributeTag t,Long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i,specificCharacterSet);
	}

	/**
	 * <p>Flesh out a constructed (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	private void doCommonConstructorStuff(SpecificCharacterSet specificCharacterSet) {
		flushCachedCopies();
		this.specificCharacterSet=specificCharacterSet;
		originalValues=null;
		originalByteValues=null;
	}

	/**
	 * <p>Read a constructed attribute from an input stream; called only by concrete sub-classes.</p>
	 *
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		doCommonConstructorStuff(specificCharacterSet);
		originalValues=null;
		originalByteValues=null;
		if (vl > 0) {
			originalByteValues = new byte[(int)vl];
			try {
				i.readInsistently(originalByteValues,0,(int)vl);
			}
			catch (IOException e) {
				throw new DicomException("Failed to read value (length "+vl+" dec) in "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
			String sbuf = translateByteArrayToString(originalByteValues,0,(int)vl);		// may fail due to unsuported encoding and return null, though should not happen since should use default (000307)
			if (sbuf != null) {
				vl=sbuf.length();	// NB. this only makes a difference for multi-byte character sets
				int start=0;
				int delim=0;
				while (true) {
					if (delim >= vl || sbuf.charAt(delim) == '\\') {
						addValue(sbuf.substring(start,delim));
						++delim;
						start=delim;
						if (delim >= vl) break;
					}
					else {
					++delim;
					}
				}
			}
			// else do not add values since translateByteArrayToString failed (probably unsuported encoding), but leave VL alone (>0) in case untranslated original bytes are useful (000307) :(
		}
	}
	
	/***/
	public long getPaddedVL() {
		byte[] b = null;
		try {
			b = getPaddedByteValues();
		}
		catch (DicomException e) {
			b = null;
		}
		return b == null ? 0 : b.length;
	}
	
	/**
	 * <p>Get the appropriate byte for padding a string to an even length.</p>
	 *
	 * @return	the byte pad value appropriate to the VR
	 */
	protected byte getPadByte() { return 0x20; }	// space for most everything, UI will override to 0x00

	/**
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	private byte[] getPaddedByteValues() throws DicomException {
		if (cachedPaddedByteValues == null) {
			cachedPaddedByteValues = extractPaddedByteValues();
		}
		return cachedPaddedByteValues;
	}
	
	/**
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	private byte[] extractPaddedByteValues() throws DicomException {
		StringBuffer sb = new StringBuffer();
		String[] v = getOriginalStringValues();
		if (v != null) {
			for (int j=0; j<v.length; ++j) {
				if (j > 0) sb.append("\\");
				sb.append(v[j]);
			}
		}
		//byte[] b = sb.toString().getBytes();
		byte[] b = null;
		try {
			b = translateStringToByteArray(sb.toString());
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
			//if (getPaddedVL() != b.length) {
			//	throw new DicomException("Internal error - "+this+" - byte array length ("+b.length+") not equal to expected padded VL("+getPaddedVL()+")");
			//}
		}
		return b;
	}

	/**
	 * @param	o					the output stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if a DICOM encoding error occurs
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);
		byte b[] = getPaddedByteValues();
		if (b != null && b.length > 0) o.write(b);
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" <");
		try {
			//String[] v = getStringValues();
			String[] v = getOriginalStringValues();
			if (v != null) {
				for (int j=0; j<v.length; ++j) {
					if (j > 0) str.append("\\");
					str.append(v[j]);
				}
			}
		}
		catch (DicomException e) {
			str.append("XXXX");
		}
		str.append(">");
		return str.toString();
	}

	/**
	 * <p>Get the values of this attribute as a byte array.</p>
	 *
	 * <p>Returns the originally read byte values, if read from a stream, otherwise converts the string to bytes and pads them.</p>
	 *
	 * @return			the values as an array of bytes
	 * @throws	DicomException	if a DICOM parsing error occurs
	 */
	public byte[]   getByteValues() throws DicomException {
		return originalByteValues == null ? getPaddedByteValues() : originalByteValues;
	}

	/**
	 * <p>Get the values of this attribute as strings.</p>
	 *
	 * <p>The strings are first cleaned up into a canonical form, to remove leading and trailing padding.</p>
	 *
	 * @param	format		the format to use for each numerical or decimal value
	 * @return			the values as an array of {@link java.lang.String String}
	 * @throws	DicomException	not thrown
	 */
	public String[] getStringValues(NumberFormat format) throws DicomException {
		// ignore number format for generic string attributes
		if (cachedUnpaddedStringCopy == null) cachedUnpaddedStringCopy=ArrayCopyUtilities.copyStringArrayRemovingLeadingAndTrailingPadding(originalValues);
		return cachedUnpaddedStringCopy;
	}

	/**
	 * <p>Get the values of this attribute as strings, the way they were originally inserted or read.</p>
	 *
	 * @return						the values as an array of {@link java.lang.String String}
	 * @throws	DicomException	not thrown
	 */
	public String[] getOriginalStringValues() throws DicomException {
		return originalValues;
	}

	/**
	 * @return						the values as an array of short
	 * @throws	DicomException	not thrown
	 */
	public short[] getShortValues() throws DicomException {
		if (cachedShortCopy == null) cachedShortCopy=ArrayCopyUtilities.copyStringToShortArray(getStringValues());	// must be unpadded
		return cachedShortCopy;
	}

	/**
	 * @return						the values as an array of int
	 * @throws	DicomException	not thrown
	 */
	public int[] getIntegerValues() throws DicomException {
		if (cachedIntegerCopy == null) cachedIntegerCopy=ArrayCopyUtilities.copyStringToIntArray(getStringValues());	// must be unpadded
		return cachedIntegerCopy;
	}

	/**
	 * @return						the values as an array of long
	 * @throws	DicomException	not thrown
	 */
	public long[] getLongValues() throws DicomException {
		if (cachedLongCopy == null) cachedLongCopy=ArrayCopyUtilities.copyStringToLongArray(getStringValues());		// must be unpadded
		return cachedLongCopy;
	}

	/**
	 * @return						the values as an array of float
	 * @throws	DicomException	not thrown
	 */
	public float[] getFloatValues() throws DicomException {
		if (cachedFloatCopy == null) cachedFloatCopy=ArrayCopyUtilities.copyStringToFloatArray(getStringValues());	// must be unpadded
		return cachedFloatCopy;
	}

	/**
	 * @return						the values as an array of double
	 * @throws	DicomException	not thrown
	 */
	public double[] getDoubleValues() throws DicomException {
		if (cachedDoubleCopy == null) cachedDoubleCopy=ArrayCopyUtilities.copyStringToDoubleArray(getStringValues());	// must be unpadded
		return cachedDoubleCopy;
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	if unsupported encoding
	 */
	public void addValue(String v) throws DicomException {
		flushCachedCopies();
		originalValues=ArrayCopyUtilities.expandArray(originalValues);
		try {
			valueLength+=translateStringToByteArray(v).length;	// note that this is inefficient, and the translated bytes should be cached, but is expedient to avoid including padding at this moment :(
		}
		catch (UnsupportedEncodingException e) {
			throw new DicomException(e.toString());
		}
		if (valueMultiplicity > 0) ++valueLength;				// for the delimiter (NB. delimiters for all characters sets are known to be a single byte)
		originalValues[valueMultiplicity++]=v;
	}
	

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(byte v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., IS, 12 bytes only)
		addValue(Short.toString((short)(((int)v)&0xff)));	// don't ask !
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(short v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., IS, 12 bytes only)
		addValue(Short.toString(v));
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(int v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., IS, 12 bytes only)
		addValue(Integer.toString(v));
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(long v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., IS, 12 bytes only)
		addValue(Long.toString(v));
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(float v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., DS, 16 bytes only)
		addValue(Float.toString(v));
	}

	/**
	 * @param	v					value to add
	 * @throws	DicomException	not thrown
	 */
	public void addValue(double v) throws DicomException {
		// will be overridden by more constrained methods that take into account length and format limitations (e.g., DS, 16 bytes only)
		addValue(Double.toString(v));
	}

	/**
	 * @throws	DicomException	not thrown
	 */
	public void removeValues() throws DicomException {
		valueLength=0;
		valueMultiplicity=0;
		originalValues=null;
		flushCachedCopies();
	}

	public boolean areLengthsOfValuesValid() throws DicomException {
//System.err.println("StringAttribute.areLengthsOfValuesValid():");
		boolean good = true;
		if (originalValues != null && originalValues.length > 0) {
			for (String v : originalValues) {
				if (v != null && v.length() > getMaximumLengthOfSingleValue()) {
//System.err.println("StringAttribute.areLengthsOfValuesValid(): invalid length of value got "+(v == null ? "null" : v.length())+" want "+getMaximumLengthOfSingleValue());
					good = false;
				}
			}
		}
		return good;
	}

	public boolean isCharacterInValueValid(int c) throws DicomException {
//System.err.println("StringAttribute.isCharacterInValueValid(): c = "+(char)c+" ("+c+" dec)");
		return !Character.isISOControl(c) && c != '\\';	// ESC is theoretically permitted, but we clean up ISO 2022 in ingestion, so should not occur internally
	}

	public boolean areCharactersInValuesValid() throws DicomException {
//System.err.println("StringAttribute.areCharactersInValuesValid():");
		boolean good = true;
		if (originalValues != null && originalValues.length > 0) {
			for (String v : originalValues) {
				if (v != null) {
					for (int i=0; i<v.length(); ++i) {
						int c = v.codePointAt(i);
						if (!isCharacterInValueValid(c)) {
//System.err.println("StringAttribute.isCharacterInValueValid(): invalid character c = "+c);
							good = false;
							break;
						}
					}
					if (!good) break;
				}
			}
		}
		return good;
	}
	
	public boolean areValuesWellFormed() throws DicomException {
//System.err.println("StringAttribute.areValuesWellFormed():");
		return true;
	}

	public boolean isValid() throws DicomException {
//System.err.println("StringAttribute.isValid():");
		return areLengthsOfValuesValid()
		    && areCharactersInValuesValid()
			&& areValuesWellFormed();
	}
	
	protected boolean allowRepairOfIncorrectLength() {				// if overridden in sub-class to be false, will not repair by truncating (though may stll trim())
//System.err.println("StringAttribute.allowRepairOfIncorrectLength():");
		return true;
	}
	
	protected boolean allowRepairOfInvalidCharacterReplacement() {	// if overridden in sub-class to be false, will not repair (replace or remove) bad characters
//System.err.println("StringAttribute.allowRepairOfInvalidCharacterReplacement():");
		return true;
	}
	
	protected char getInvalidCharacterReplacement() {					// if overridden in sub-class to be 0, will remove rather than replace character
//System.err.println("StringAttribute.getInvalidCharacterReplacement():");
		return ' ';
	}
	 
	public boolean repairValues() throws DicomException {
//System.err.println("StringAttribute.repairValues():");
		if (!isValid()) {
//System.err.println("StringAttribute.repairValues(): not valid so attempting repair");
			flushCachedCopies();
			originalByteValues=null;
			if (originalValues != null && originalValues.length > 0) {
				originalValues=ArrayCopyUtilities.copyStringArrayRemovingLeadingAndTrailingPadding(originalValues);
				for (int i=0; i<originalValues.length; ++i) {
					String v = originalValues[i];
					if (v != null && v.length() > 0) {
						StringBuffer buf = new StringBuffer(v.length());
						for (int j=0; j<v.length(); ++j) {
							int cp = v.codePointAt(j);
							if (allowRepairOfInvalidCharacterReplacement() && !isCharacterInValueValid(cp)) {
								char rc = getInvalidCharacterReplacement();
//System.err.println("StringAttribute.repairValues(): repairing invalid character by "+(rc == 0 ? "removing it" : ("replacing it with '"+rc+"'")));
								if (rc != 0) {
									buf.append(rc);
								}
							}
							else {
								buf.appendCodePoint(cp);
							}
						}
						v = buf.toString();
						if (allowRepairOfIncorrectLength() && v.length() > getMaximumLengthOfSingleValue()) {
							v = v.substring(0,getMaximumLengthOfSingleValue()).trim();	// trim it because truncation may expose embedded spaces that are now trailing
						}
						originalValues[i] = v;
					}
				}
			}
		}
		return isValid();
	}
}


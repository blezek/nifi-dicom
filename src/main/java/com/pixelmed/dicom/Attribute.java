/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.IOException;
import java.text.NumberFormat;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.dicom.Attribute Attribute} class is an abstract class that contains the core
 * functionality for storing, accessing and maintaining values of a DICOM Attribute.</p>
 *
 * <p>Though instances of concrete sub-classes of this abstract class may be created
 * using their constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
abstract public class Attribute {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/Attribute.java,v 1.36 2017/01/24 10:50:35 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(Attribute.class);

	private AttributeTag tag;

	/***/
	protected long valueLength;
	/***/
	protected int  valueMultiplicity=0;

	/**
	 * <p>Construct an (empty) attribute; called only by concrete sub-classes.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	protected Attribute(AttributeTag t) {
		tag=t;
		valueLength=0;
		valueMultiplicity=0;
	}

	/**
	 * <p>Get the tag of this attribute.</p>
	 *
	 * @return	the tag
	 */
	public AttributeTag getTag()   { return tag; }

	/**
	 * <p>Get the group of the tag of this attribute.</p>
	 *
	 * @return	the group of the tag
	 */
	public int getGroup()          { return tag.getGroup(); }

	/**
	 * <p>Get the element of the tag of this attribute.</p>
	 *
	 * @return	the element of the tag
	 */
	public int getElement()        { return tag.getElement(); }

	/**
	 * <p>Get the value length of this attribute.</p>
	 *
	 * @return	the value length (does not include need for even-length padding, hence may be odd length)
	 */
	public long getVL()		{ return valueLength; }

	/**
	 * <p>Get the value multiplicity of this attribute.</p>
	 *
	 * @return	the value multiplicity
	 */
	public int  getVM()		{ return valueMultiplicity; }

	/**
	 * <p>Get the value representation of this attribute.</p>
	 *
	 * @return	the value representation as a two byte array
	 */
	public byte[] getVR()		{ return ValueRepresentation.UN; }

	/**
	 * <p>Get the value representation of this attribute.</p>
	 *
	 * @return	the value representation as a {@link java.lang.String String}
	 */
	public String getVRAsString()	{ return ValueRepresentation.getAsString(getVR()); }

	/**
	 * <p>Get the value length of this attribute, accounting for the need for even-length padding.</p>
	 *
	 * @return	the value length (padded to an even length)
	 */
	public long getPaddedVL()	{ return valueLength; }		// Needs to be overridden esp. in String attributes

	/**
	 * <p>Get the values of this attribute as strings, the way they were originally inserted or read.</p>
	 *
	 * @return			the values as an array of {@link java.lang.String String}
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public String[] getOriginalStringValues()    throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as strings.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding.</p>
	 *
	 * @return			the values as an array of {@link java.lang.String String}
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public String[] getStringValues()            throws DicomException { return getStringValues((NumberFormat)null); }

	/**
	 * <p>Get the values of this attribute as strings.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding as well as numbers formatted.</p>
	 *
	 * @param	format		the format to use for each numerical or decimal value
	 * @return			the values as an array of {@link java.lang.String String}
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public String[] getStringValues(NumberFormat format)            throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as a byte array.</p>
	 *
	 * @return			the values as an array of bytes
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public byte[]   getByteValues()              throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as a short array.</p>
	 *
	 * @return			the values as an array of short
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public short[]  getShortValues()             throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as an int array.</p>
	 *
	 * @return			the values as an array of int
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public int[]    getIntegerValues()           throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as a long array.</p>
	 *
	 * @return			the values as an array of long
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public long[]   getLongValues()              throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as a float array.</p>
	 *
	 * @return			the values as an array of float
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public float[]  getFloatValues()             throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Get the values of this attribute as a double array.</p>
	 *
	 * @return			the values as an array of double
	 * @throws	DicomException	thrown if values are not available (such as not supported for this concrete attribute class)
	 */
	public double[] getDoubleValues()            throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Add a(nother) {@link java.lang.String String} value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(String v) throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single {@link java.lang.String String} value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(String v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) byte value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(byte v)   throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single byte value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(byte v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) short value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(short v)  throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single short value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(short v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) int value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(int v)    throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single int value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(int v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) long value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(long v)   throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single long value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(long v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) float value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(float v)  throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single float value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(float v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Add a(nother) double value after any existing values of this attribute.
	 *
	 * @param	v		value to add
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void addValue(double v) throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Set a single double value after any existing values of this attribute.
	 *
	 * @param	v		value to set
	 * @throws	DicomException	thrown if value of this type is not valid for this concrete attribute class
	 */
	public void setValue(double v) throws DicomException {
		removeValues();
		addValue(v);
	}

	/**
	 * Replace any existing values with the supplied array of byte.
	 *
	 * @param	v		the array of new values
	 * @throws	DicomException	thrown if values of this type are not valid for this concrete attribute class
	 */
	public void setValues(byte[] v)   throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Replace any existing values with the supplied array of short.
	 *
	 * @param	v		the array of new values
	 * @throws	DicomException	thrown if values of this type are not valid for this concrete attribute class
	 */
	public void setValues(short[] v)  throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Replace any existing values with the supplied array of float.
	 *
	 * @param	v		the array of new values
	 * @throws	DicomException	thrown if values of this type are not valid for this concrete attribute class
	 */
	public void setValues(float[] v)  throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * Replace any existing values with the supplied array of double.
	 *
	 * @param	v		the array of new values
	 * @throws	DicomException	thrown if values of this type are not valid for this concrete attribute class
	 */
	public void setValues(double[] v)  throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag+" "+getClass().getName()); }

	/**
	 * <p>Remove any existing values, making the attribute empty (zero length).</p>
	 *
	 * @throws	DicomException	thrown if not supported for concrete attribute class
	 */
	public abstract void removeValues() throws DicomException;

	/**
	 * <p>Write the common preamble of an attribute to the output stream.</p>
	 *
	 * <p>Writes the tag, VR (if explicit in the output stream) and the value length in the appropriate form.</p>
	 *
	 * <p>Implements the CP 1066 proposal to handle values too long to fit in Explicit VR by writing a UN rather than the actual VR.</p>
	 *
	 * <p>Called by a concrete base class prior to writing the values themselves.</p>
	 *
	 * @param		o			the output stream
	 * @throws	DicomException	if the VL is too long to be written in the Transfer Syntax of the DicomOutputStream (Explicit VR)
	 * @throws	IOException		if an I/O error occurs
	 */
	protected void writeBase(DicomOutputStream o) throws DicomException, IOException {
		o.writeUnsigned16(getGroup());
		o.writeUnsigned16(getElement());
		long vlToWrite = getPaddedVL();
		if (o.isExplicitVR()) {
			byte[] vr = getVR();
			boolean tooBigForShortValueLengthVR = vlToWrite > AttributeList.maximumShortVRValueLength;
			if (ValueRepresentation.isShortValueLengthVR(vr)
			 && tooBigForShortValueLengthVR
			 && (o.isLittleEndian() || ValueRepresentation.getWordLengthOfValueAffectedByEndianness(vr) == 1)	// have not implemented a means to force child classes to swap endianness during write() :(
			) {
				// we are screwed ... cannot write in specified VR without truncation ... use CP 1066 UN VR instead
				slf4jlogger.warn("Using UN rather than {} because VL ({} dec, 0x{}) is too long to fit in 16 bits for {}",ValueRepresentation.getAsString(vr),vlToWrite,Long.toHexString(vlToWrite),tag);
				vr = ValueRepresentation.UN;
			}
			o.write(vr,0,2);
			if (ValueRepresentation.isShortValueLengthVR(vr)) {
				if (!tooBigForShortValueLengthVR) {
					o.writeUnsigned16((int)vlToWrite);
				}
				else {
					throw new DicomException("VL ("+vlToWrite+" dec, 0x"+Long.toHexString(vlToWrite)+") for "+tag+" is too long to write in 16 bits for Explicit VR");
				}
			}
			else {
				o.writeUnsigned16(0);		// reserved bytes
				o.writeUnsigned32(vlToWrite);
			}
		}
		else {
			o.writeUnsigned32(vlToWrite);
		}
	}
	
	/**
	 * <p>Write the entire attribute (including values) to the output stream.</p>
	 *
	 * @param		o				the output stream
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	abstract public void write(DicomOutputStream o) throws DicomException, IOException;

	/**
	 * Do the values of this attribute comply with standard VR requirements?
	 *
	 * E.g., Are only valid characters are present (for string and text attributes) and length is within limits?
	 *
	 * @return                      true if valid, false if invalid or validation not supported for the attribute type
	 * @throws	DicomException	if error in DICOM value extraction
	 */
	public boolean isValid() throws DicomException { return false; }

	/**
	 * Repair any existing values of this attribute to make them comply with standard VR requirements.
	 *
	 * E.g., truncate them if they are too long.
	 *
	 * N.B., Not all types of attribute support repair.
	 *
	 * @return                      true if successfully repaired or was already valid
	 * @throws	DicomException	if error in DICOM value extraction
	 */
	public boolean repairValues() throws DicomException { return isValid(); }
	

	/**
	 * <p>Dump the contents of the attribute as a human-readable string.</p>
	 *
	 * <p>No new line is appended.</p>
	 *
	 * <p>The result is of the form:</p>
	 * <pre>
	 * (0xgggg,0xeeee) Name VR=&lt;XX&gt; VL=&lt;0xnnnn&gt; &lt;...&gt;
	 * </pre>
	 * <p>For example:</p>
	 * <pre>
	 * (0x0018,0x0020) ScanningSequence VR=&lt;CS&gt; VL=&lt;0x2&gt; &lt;GR&gt;
	 * </pre>
	 *
	 * @param       dictionary      the dictionary to use to look up the name
	 * @return                      a single {@link java.lang.String String} value
	 */
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(tag.toString());
		if (dictionary != null) {
			String name = dictionary.getNameFromTag(tag);
			if (name != null) {
				str.append(" ");
				str.append(name);
			}
		}
		str.append(" VR=<");
		str.append(getVRAsString());
		str.append("> VL=<0x");
		str.append(Long.toHexString(getVL()));
		str.append(">");
		return str.toString();
	}

	/**
	 * <p>Dump the contents of the attribute as a human-readable string.</p>
	 *
	 * <p>No new line is appended.</p>
	 *
	 * <p>The result is of the form:</p>
	 * <pre>
	 * (0xgggg,0xeeee) VR=&lt;XX&gt; VL=&lt;0xnnnn&gt; &lt;...&gt;
	 * </pre>
	 * <p>For example:</p>
	 * <pre>
	 * (0x0018,0x0020) VR=&lt;CS&gt; VL=&lt;0x2&gt; &lt;GR&gt;
	 * </pre>
	 * @return		a single {@link java.lang.String String} value
	 */
	public String toString() {
		return toString(null);
	}
	
	// Some convenience methods ...

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) string value
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrDefault(String dflt) {
		return getSingleStringValueOrDefault(dflt,null);
	}

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) string value
	 * @param	format	the format to use for each numerical or decimal value (null if none)
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrDefault(String dflt,NumberFormat format) {
		String value = dflt;
		try {
			String[] sv = getStringValues(format);
			//String[] sv = getOriginalStringValues();
			if (sv != null && sv.length > 0) {
				String v = sv[0];
				if (v != null) value = v;
			}
		}
		catch (DicomException e) {
		}
		return value;
	}

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrEmptyString() { return getSingleStringValueOrDefault(""); }

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrEmptyString(NumberFormat format) { return getSingleStringValueOrDefault("",format); }

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrNull() { return getSingleStringValueOrDefault(null); }

	/**
	 * <p>Get a single string value for the attribute.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public String getSingleStringValueOrNull(NumberFormat format) { return getSingleStringValueOrDefault(null,format); }

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @param	dflt	what to return if there are no (valid) string values
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrDefault(String dflt) {
		return getDelimitedStringValuesOrDefault(dflt,null);
	}

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @param	dflt	what to return if there are no (valid) string values
	 * @param	format	the format to use for each numerical or decimal value (null if none)
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrDefault(String dflt,NumberFormat format) {
		String value = dflt;
		try {
			String[] sv = getStringValues(format);
			if (sv != null) {
				StringBuffer str = new StringBuffer();
				for (int i=0; i< sv.length; ++i) {
					if (i > 0) str.append("\\");
					String v = sv[i];
					if (v != null) str.append(v);
				}
				value=str.toString();
			}
		}
		catch (DicomException e) {
		}
		return value;
	}

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrEmptyString() { return getDelimitedStringValuesOrDefault(""); }

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrEmptyString(NumberFormat format) { return getDelimitedStringValuesOrDefault("",format); }

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrNull() { return getDelimitedStringValuesOrDefault(null); }

	/**
	 * <p>Get all the string values for the attribute, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrNull(NumberFormat format) { return getDelimitedStringValuesOrDefault(null,format); }

	/**
	 * <p>Get a single int value for the attribute.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single int value
	 */
	public int getSingleIntegerValueOrDefault(int dflt) {
		int value = dflt;
		try {
			int[] v = getIntegerValues();
			if (v != null && v.length > 0) {
				value = v[0];
			}
		}
		catch (DicomException e) {
		}
		return value;
	}

	/**
	 * <p>Get a single double value for the attribute.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single double value
	 */
	public double getSingleDoubleValueOrDefault(double dflt) {
		double value = dflt;
		try {
			double[] v = getDoubleValues();
			if (v != null && v.length > 0) {
				value = v[0];
			}
		}
		catch (DicomException e) {
		}
		return value;
	}

	/**
	 * <p>Get a single float value for the attribute.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single float value
	 */
	public float getSingleFloatValueOrDefault(float dflt) {
		float value = dflt;
		try {
			float[] v = getFloatValues();
			if (v != null && v.length > 0) {
				value = v[0];
			}
		}
		catch (DicomException e) {
		}
		return value;
	}


	/**
	 * <p>Get a single long value for the attribute.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single long value
	 */
	public long getSingleLongValueOrDefault(long dflt) {
		long value = dflt;
		try {
			long[] v = getLongValues();
			if (v != null && v.length > 0) {
				value = v[0];
			}
		}
		catch (DicomException e) {
		}
		return value;
	}










	// Some static convenience methods to extract values from a supplied attribute that may be null ...

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) string value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrDefault(Attribute a,String dflt) {
		String value = dflt;
		if (a != null) {
			value = a.getSingleStringValueOrDefault(dflt);
		}
		return value;
	}

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) string value
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrDefault(Attribute a,String dflt,NumberFormat format) {
		String value = dflt;
		if (a != null) {
			value = a.getSingleStringValueOrDefault(dflt,format);
		}
		return value;
	}

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrEmptyString(Attribute a) {
		return getSingleStringValueOrDefault(a,"");
	}

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrEmptyString(Attribute a,NumberFormat format) {
		return getSingleStringValueOrDefault(a,"",format);
	}

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrNull(Attribute a) {
		return getSingleStringValueOrDefault(a,null);
	}

	/**
	 * <p>Get a single string value for a supplied attribute that may be null.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrNull(Attribute a,NumberFormat format) {
		return getSingleStringValueOrDefault(a,null,format);
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * <p>Sequences and nested sequence item lists are recursively included with appropriate delimiters.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there are no (valid) string values
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrDefault(Attribute a,String dflt) {
		String value = dflt;
		if (a != null) {
			value = a.getDelimitedStringValuesOrDefault(dflt);
		}
		return value;
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there are no (valid) string values
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrDefault(Attribute a,String dflt,NumberFormat format) {
		String value = dflt;
		if (a != null) {
			value = a.getDelimitedStringValuesOrDefault(dflt,format);
		}
		return value;
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrEmptyString(Attribute a) {
		return getDelimitedStringValuesOrDefault(a,"");
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrEmptyString(Attribute a,NumberFormat format) {
		return getDelimitedStringValuesOrDefault(a,"",format);
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrNull(Attribute a) {
		return getDelimitedStringValuesOrDefault(a,null);
	}

	/**
	 * <p>Get all the string values for a supplied attribute that may be null, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrNull(Attribute a,NumberFormat format) {
		return getDelimitedStringValuesOrDefault(a,null,format);
	}

	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of strings.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as an array of {@link java.lang.String String}
	 */
	public static String[] getStringValues(Attribute a) {
		String[] values = null;
		if (a != null) {
			try {
				values = a.getStringValues();
			}
			catch (DicomException e) {
			}
		}
		return values;
	}

	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of strings.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as an array of {@link java.lang.String String}
	 */
	public static String[] getStringValues(Attribute a,NumberFormat format) {
		String[] values = null;
		if (a != null) {
			try {
				values = a.getStringValues(format);
			}
			catch (DicomException e) {
			}
		}
		return values;
	}

	/**
	 * <p>Get a single int value of a supplied attribute that may be null.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single int value
	 */
	public static int getSingleIntegerValueOrDefault(Attribute a,int dflt) {
		int value = dflt;
		if (a != null) {
			value = a.getSingleIntegerValueOrDefault(dflt);
		}
		return value;
	}


	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of int.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as an array of int
	 */
	public static int[] getIntegerValues(Attribute a) {
		int[] values = null;
		if (a != null) {
			try {
				values = a.getIntegerValues();
			}
			catch (DicomException e) {
			}
		}
		return values;
	}

	/**
	 * <p>Get a single long value of a supplied attribute that may be null.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single long value
	 */
	public static long getSingleLongValueOrDefault(Attribute a,long dflt) {
		long value = dflt;
		if (a != null) {
			value = a.getSingleLongValueOrDefault(dflt);
		}
		return value;
	}


	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of long.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as an array of long
	 */
	public static long[] getLongValues(Attribute a) {
		long[] values = null;
		if (a != null) {
			try {
					values = a.getLongValues();
			}
			catch (DicomException e) {
			}
		}
		return values;
	}

	/**
	 * <p>Get a single double value of a supplied attribute that may be null.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single double value
	 */
	public static double getSingleDoubleValueOrDefault(Attribute a,double dflt) {
		double value = dflt;
		if (a != null) {
			value = a.getSingleDoubleValueOrDefault(dflt);
		}
		return value;
	}

	/**
	 * <p>Get a single float value of a supplied attribute that may be null.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single float value
	 */
	public static float getSingleFloatValueOrDefault(Attribute a,float dflt) {
		float value = dflt;
		if (a != null) {
			value = a.getSingleFloatValueOrDefault(dflt);
		}
		return value;
	}

	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of double.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as an array of double
	 */
	public static double[] getDoubleValues(Attribute a) {
		double[] values = null;
		if (a != null) {
			try {
					values = a.getDoubleValues();
			}
			catch (DicomException e) {
			}
		}
		return values;
	}

	/**
	 * <p>Get the values of a supplied attribute that may be null, as an array of float.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	a	the attribute, which may be null
	 * @return		the values as an array of double
	 */
	public static float[] getFloatValues(Attribute a) {
		float[] values = null;
		if (a != null) {
			try {
					values = a.getFloatValues();
			}
			catch (DicomException e) {
			}
		}
		return values;
	}












	// Some static convenience methods to extract values from named attributes in a list ...

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) string value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrDefault(AttributeList list,AttributeTag tag,String dflt) {
		return list == null ? dflt : getSingleStringValueOrDefault(list.get(tag),dflt);
	}

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) string value
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrDefault(AttributeList list,AttributeTag tag,String dflt,NumberFormat format) {
		return list == null ? dflt : getSingleStringValueOrDefault(list.get(tag),dflt,format);
	}

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrEmptyString(AttributeList list,AttributeTag tag) {
		return getSingleStringValueOrDefault(list,tag,"");
	}

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrEmptyString(AttributeList list,AttributeTag tag,NumberFormat format) {
		return getSingleStringValueOrDefault(list,tag,"",format);
	}

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrNull(AttributeList list,AttributeTag tag) {
		return getSingleStringValueOrDefault(list,tag,null);
	}

	/**
	 * <p>Get a single string value for a named attribute in an attribute list.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is more than one string value, only the first is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		a single {@link java.lang.String String} value
	 */
	public static String getSingleStringValueOrNull(AttributeList list,AttributeTag tag,NumberFormat format) {
		return getSingleStringValueOrDefault(list,tag,null,format);
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there are no (valid) string values
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrDefault(AttributeList list,AttributeTag tag,String dflt) {
		return list == null ? dflt : getDelimitedStringValuesOrDefault(list.get(tag),dflt);
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, the supplied default is returned.</p>
	 *
	 * <p>If there is no string value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, the supplied default is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there are no (valid) string values
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrDefault(AttributeList list,AttributeTag tag,String dflt,NumberFormat format) {
		return list == null ? dflt : getDelimitedStringValuesOrDefault(list.get(tag),dflt,format);
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrEmptyString(AttributeList list,AttributeTag tag) {
		return getDelimitedStringValuesOrDefault(list,tag,"");
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, an empty string is returned.</p>
	 *
	 * <p>If there is no string value, an empty string is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, an empty string is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrEmptyString(AttributeList list,AttributeTag tag,NumberFormat format) {
		return getDelimitedStringValuesOrDefault(list,tag,"",format);
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrNull(AttributeList list,AttributeTag tag) {
		return getDelimitedStringValuesOrDefault(list,tag,null);
	}

	/**
	 * <p>Get all the string values for a named attribute in an attribute list, separated by the appropriate delimiter.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is no string value, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned, not the original string.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as a delimited {@link java.lang.String String}
	 */
	public static String getDelimitedStringValuesOrNull(AttributeList list,AttributeTag tag,NumberFormat format) {
		return getDelimitedStringValuesOrDefault(list,tag,null,format);
	}

	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of strings.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as an array of {@link java.lang.String String}
	 */
	public static String[] getStringValues(AttributeList list,AttributeTag tag) {
		return list == null ? null : getStringValues(list.get(tag)); 
	}

	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of strings.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * <p>The strings may have been cleaned up into a canonical form, such as to remove padding.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	format	the format to use for each numerical or decimal value
	 * @return		the values as an array of {@link java.lang.String String}
	 */
	public static String[] getStringValues(AttributeList list,AttributeTag tag,NumberFormat format) {
		return list == null ? null : getStringValues(list.get(tag),format); 
	}

	/**
	 * <p>Get a single int value of a named attribute in an attribute list.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single int value
	 */
	public static int getSingleIntegerValueOrDefault(AttributeList list,AttributeTag tag,int dflt) {
		return list == null ? dflt : getSingleIntegerValueOrDefault(list.get(tag),dflt); 
	}


	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of int.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as an array of int
	 */
	public static int[] getIntegerValues(AttributeList list,AttributeTag tag) {
		return list == null ? null : getIntegerValues(list.get(tag)); 
	}

	/**
	 * <p>Get a single long value of a named attribute in an attribute list.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single long value
	 */
	public static long getSingleLongValueOrDefault(AttributeList list,AttributeTag tag,long dflt) {
		return list == null ? dflt : getSingleLongValueOrDefault(list.get(tag),dflt); 
	}

	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of long.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as an array of long
	 */
	public static long[] getLongValues(AttributeList list,AttributeTag tag) {
		return list == null ? null : getLongValues(list.get(tag)); 
	}

	/**
	 * <p>Get a single double value of a named attribute in an attribute list.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single double value
	 */
	public static double getSingleDoubleValueOrDefault(AttributeList list,AttributeTag tag,double dflt) {
		return list == null ? dflt : getSingleDoubleValueOrDefault(list.get(tag),dflt); 
	}

	/**
	 * <p>Get a single float value of a named attribute in an attribute list.</p>
	 *
	 * <p>If there is more than one value, only the first is returned.</p>
	 *
	 * <p>If there is no value, the supplied default is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the value, the supplied default is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @param	dflt	what to return if there is no (valid) value
	 * @return		a single float value
	 */
	public static float getSingleFloatValueOrDefault(AttributeList list,AttributeTag tag,float dflt) {
		return list == null ? dflt : getSingleFloatValueOrDefault(list.get(tag),dflt); 
	}

	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of double.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as an array of double
	 */
	public static double[] getDoubleValues(AttributeList list,AttributeTag tag) {
		return list == null ? null : getDoubleValues(list.get(tag)); 
	}

	/**
	 * <p>Get the values of a named attribute in an attribute list, as an array of float.</p>
	 *
	 * <p>If there is no such attribute, <code>null</code> is returned.</p>
	 *
	 * <p>If there is an exception trying to fetch the values, <code>null</code> is returned.</p>
	 *
	 * @param	list	the list of attributes in which to look for the attribute
	 * @param	tag	the tag of the attribute to find
	 * @return		the values as an array of float
	 */
	public static float[] getFloatValues(AttributeList list,AttributeTag tag) {
		return list == null ? null : getFloatValues(list.get(tag)); 
	}
}



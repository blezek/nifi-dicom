/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * <p>A class to provide support for the contents of an individual item of a DICOM Sequence (SQ)
 * attribute, each of which consists of an entire dataset (list of attributes).</p>
 *
 * @see com.pixelmed.dicom.SequenceAttribute
 *
 * @author	dclunie
 */
public class SequenceItem {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SequenceItem.java,v 1.14 2017/01/24 10:50:38 dclunie Exp $";

	private AttributeList list;
	protected long byteOffset;		// value of 0 is flag that it is not set

	/**
	 * <p>Construct a sequence attribute item with a list of attributes.</p>
	 *
	 * @param	l	the list of attributes that comprise the item
	 */
	public SequenceItem(AttributeList l) {
		list=l;
		byteOffset=0;
	}

	/**
	 * <p>Construct a sequence attribute item with a list of attributes,
	 * additionally keeping track of where in the byte stream that the
	 * attributes were read from the item starts, for use in supporting
	 * DICOM Directory Records which are indexed by physical byte offset
	 * (see {@link com.pixelmed.dicom.DicomDirectory DicomDirectory}).</p>
	 *
	 * @param	l	the list of attributes that comprise the item
	 * @param	offset
	 */
	public SequenceItem(AttributeList l,long offset) {
		list=l;
		byteOffset=offset;
	}

	/**
	 * <p>Get the list of attributes in this item.</p>
	 *
	 * @return	the attribute list
	 */
	public AttributeList getAttributeList() { return list; }

	/**
	 * <p>Get the byte offset of the start of this item recorded when the item was read.</p>
	 *
	 * @return	the byte offset
	 */
	public long getByteOffset() { return byteOffset; }

	/**
	 * <p>Write the item (with appropriate delimiter tags) to the output stream.</p>
	 *
	 * <p>Always written in undefined length form.</p>
	 *
	 * @param	o		the output stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		byteOffset = o.getByteOffset();
		
		o.writeUnsigned16(0xfffe);		// Item
		o.writeUnsigned16(0xe000);
		o.writeUnsigned32(0xffffffffl);		// undefined length
		
		getAttributeList().writeFragment(o);
		
		o.writeUnsigned16(0xfffe);		// Item Delimiter
		o.writeUnsigned16(0xe00d);
		o.writeUnsigned32(0);			// dummy length
	}
	
	/**
	 * <p>Dump the item in a human readable form, list the contained attributes.</p>
	 *
	 * @param	dictionary
	 * @return			the string representing the content of the item
	 */
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append("%item");
		if (byteOffset != 0) {
			str.append(" [starts at 0x");
			str.append(Long.toHexString(byteOffset));
			str.append("]");
		}
		str.append("\n");
		str.append(list.toString(dictionary));
		str.append("%enditem");
		return str.toString();
	}
	
	/**
	 * <p>Dump the item in a human readable form, list the contained attributes.</p>
	 *
	 * @return	the string representing the content of the item
	 */
	public String toString() {
		return toString(null);
	}

	/**
	 * <p>Get all the string values for the item, separated by the specified delimiter.</p>
	 *
	 * <p>If there is no string value for an individual value or an exception trying to fetch it, the supplied default is returned for each Attribute.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned for each Attribute value, not the original string.</p>
	 *
	 * @param	dflt		what to return if there are no (valid) string values
	 * @param	format		the format to use for each numerical or decimal value (null if none)
	 * @param	delimiter	the delimiter to use between each value
	 * @return				the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrDefault(String dflt,NumberFormat format,String delimiter) {
		StringBuffer str = new StringBuffer();
		str.append("{");
		str.append(list.getDelimitedStringValuesOrDefault(dflt,format,delimiter));
		str.append("}");
		return str.toString();
	}

}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Sequence (SQ) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.SequenceItem
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class SequenceAttribute extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SequenceAttribute.java,v 1.27 2017/01/24 10:50:38 dclunie Exp $";

	private LinkedList<SequenceItem> itemList;		// each member is a SequenceItem

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public SequenceAttribute(AttributeTag t) {
		super(t);
		itemList=new LinkedList<SequenceItem>();
		valueLength=0xffffffffl;	// for the benefit of writebase();
	}

	// no constructor for input stream ... done manually elsewhere

	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		writeBase(o);			// valueLength should be 0xffffffff from constructor

		Iterator<SequenceItem> i = iterator();
		while (i.hasNext()) {
			SequenceItem item = i.next();
			item.write(o);
		}
		
		o.writeUnsigned16(0xfffe);	// Sequence Delimiter
		o.writeUnsigned16(0xe0dd);
		o.writeUnsigned32(0);		// dummy length
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append("\n%seq\n");
		Iterator<SequenceItem> i = iterator();
		while (i.hasNext()) {
			str.append(i.next().toString(dictionary));
			str.append("\n");
		}
		str.append("%endseq");
		return str.toString();
	}

	/**
	 */
	public void removeValues() {
		itemList=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * Add an item to the sequence (after any existing items).
	 *
	 * @param	item
	 */
	public void addItem(SequenceItem item) {
		itemList.addLast(item);
	}

	/**
	 * Add an item to the sequence (after any existing items).
	 *
	 * @param	item	the list of attributes that comprise the item
	 */
	public void addItem(AttributeList item) {
		itemList.addLast(new SequenceItem(item));
	}

	/**
	 * Add an item to the sequence (after any existing items), keeping tracking of input byte offsets.
	 *
	 * @param	item		the list of attributes that comprise the item
	 * @param	byteOffset	the byte offset in the input stream of the start of the item
	 */
	public void addItem(AttributeList item,long byteOffset) {
		itemList.addLast(new SequenceItem(item,byteOffset));
	}

	/**
	 * Get an {@link java.util.Iterator Iterator} of the items in the sequence.
	 *
	 * @return	a {@link java.util.Iterator Iterator} of items, each encoded as an {@link com.pixelmed.dicom.SequenceItem SequenceItem}
	 */
	public Iterator<SequenceItem> iterator() {
		return itemList.listIterator(0);
	}

	/**
	 * Get the number of items in the sequence.
	 *
	 * @return	the number of items
	 */
	public int getNumberOfItems() {
		return itemList.size();
	}

	/**
	 * Get particular item in the sequence.
	 *
	 * @param	index	which item to return, numbered from zero
	 * @return		a {@link com.pixelmed.dicom.SequenceItem SequenceItem}, null if no items or no such item
	 */
	public SequenceItem getItem(int index) {
		return (itemList == null || index >= itemList.size()) ? null : itemList.get(index);
	}

	/**
	 * <p>Get the value representation of this attribute (SQ).</p>
	 *
	 * @return	'S','Q' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.SQ; }

	/**
	 * <p>Extract the AttributeList of the first item from a sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @return						the AttributeList if found else null
	 */
	public static AttributeList getAttributeListFromWithinSequenceWithSingleItem(SequenceAttribute sequenceAttribute) {
		AttributeList slist = null;
		if (sequenceAttribute != null) {
			// assert sequenceAttribute.getNumberOfItems() >= 1
			// assert sequenceAttribute.getNumberOfItems() == 1
			Iterator<SequenceItem> sitems = sequenceAttribute.iterator();
			if (sitems.hasNext()) {
				SequenceItem sitem = sitems.next();
				if (sitem != null) {
					slist = sitem.getAttributeList();
				}
			}
		}
		return slist;
	}
	
	/**
	 * <p>Extract the AttributeList of the particular item in the sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @param	index	which item to return, numbered from zero
	 * @return						the AttributeList if found else null
	 */
	public static AttributeList getAttributeListFromSelectedItemWithinSequence(SequenceAttribute sequenceAttribute,int index) {
		AttributeList slist = null;
		if (sequenceAttribute != null) {
			SequenceItem sitem = sequenceAttribute.getItem(index);
			if (sitem != null) {
				slist = sitem.getAttributeList();
			}
		}
		return slist;
	}
	
	/**
	 * <p>Extract the specified attribute from within the particular item in the sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @param	index				which item to return, numbered from zero
	 * @param	namedTag			the tag of the attribute within the item of the sequence
	 * @return						the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSelectedItemWithinSequence(SequenceAttribute sequenceAttribute,int index,AttributeTag namedTag) {
		Attribute a = null;
		if (sequenceAttribute != null) {
			SequenceItem sitem = sequenceAttribute.getItem(index);
			if (sitem != null) {
				AttributeList slist = sitem.getAttributeList();
				if (slist != null) {
					a=slist.get(namedTag);
				}
			}
		}
		return a;
	}

	/**
	 * <p>Extract the specified attribute from within the particular item of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	index		which item to return, numbered from zero
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSelectedItemWithinSequence(AttributeList list,AttributeTag sequenceTag,int index,AttributeTag namedTag) {
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		return getNamedAttributeFromWithinSelectedItemWithinSequence(sequenceAttribute,index,namedTag);
	}

	/**
	 * <p>Extract the AttributeList of the first item from a specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @return				the AttributeList if found else null
	 */
	public static AttributeList getAttributeListFromWithinSequenceWithSingleItem(AttributeList list,AttributeTag sequenceTag) {
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		return getAttributeListFromWithinSequenceWithSingleItem(sequenceAttribute);
	}
	
	/**
	 * <p>Extract the specified attribute from the first item of the specified sequence.</p>
	 *
	 * @param	sequenceAttribute	the sequence attribute that has one item (may be null in which case returns null)
	 * @param	namedTag		the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSequenceWithSingleItem(SequenceAttribute sequenceAttribute,AttributeTag namedTag) {
		Attribute a = null;
		if (sequenceAttribute != null) {
			AttributeList slist = getAttributeListFromWithinSequenceWithSingleItem(sequenceAttribute);
			if (slist != null) {
				a=slist.get(namedTag);
			}
		}
		return a;
	}

	/**
	 * <p>Extract the specified attribute from within the first item of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else null
	 */
	public static Attribute getNamedAttributeFromWithinSequenceWithSingleItem(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag) {
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		return getNamedAttributeFromWithinSequenceWithSingleItem(sequenceAttribute,namedTag);
	}

	/**
	 * <p>Extract the specified attribute from within the first item of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @param	dflt		what to return if there is no such sequence attribute or it is empty or the attribute is not found
	 * @return				the attribute if found else the dflt
	 */
	public static String getSingleStringValueOfNamedAttributeFromWithinSequenceWithSingleItemOrDefault(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag,String dflt) {
		String value=dflt;
		SequenceAttribute sequenceAttribute = (SequenceAttribute)list.get(sequenceTag);
		Attribute a=getNamedAttributeFromWithinSequenceWithSingleItem(sequenceAttribute,namedTag);
		if (a != null) {
			value = a.getSingleStringValueOrDefault(dflt);
		}
		return value;
	}

	/**
	 * <p>Extract the specified attribute from within the first item of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @return				the attribute if found else empty string
	 */
	public static String getSingleStringValueOfNamedAttributeFromWithinSequenceWithSingleItemOrEmptyString(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag) {
		return getSingleStringValueOfNamedAttributeFromWithinSequenceWithSingleItemOrDefault(list,sequenceTag,namedTag,"");
	}
	
	/**
	 * <p>Extract the code meaning attribute value from within the first item of the specified code sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the code sequence (may not be null)
	 * @param	sequenceTag	the tag of the code sequence attribute that has one item
	 * @param	dflt		what to return if there is no such sequence attribute or it is empty or has no code meaning attribute
	 * @return				the code meaning if found else the dflt
	 */
	public static String getMeaningOfCodedSequenceAttributeOrDefault(AttributeList list,AttributeTag sequenceTag,String dflt) {
		return getSingleStringValueOfNamedAttributeFromWithinSequenceWithSingleItemOrDefault(list,sequenceTag,TagFromName.CodeMeaning,dflt);
	}
	
	/**
	 * <p>Extract the code meaning attribute value from within the first item of the specified code sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the code sequence (may not be null)
	 * @param	sequenceTag	the tag of the code sequence attribute that has one item
	 * @return				the code meaning if found else empty string
	 */
	public static String getMeaningOfCodedSequenceAttributeOrEmptyString(AttributeList list,AttributeTag sequenceTag) {
		return getMeaningOfCodedSequenceAttributeOrDefault(list,sequenceTag,"");
	}
	
	/**
	 * <p>Get all the string values for all the items.</p>
	 *
	 * <p>If there is no string value for an individual value or an exception trying to fetch it, the supplied default is returned for each Attribute.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned for each Attribute value, not the original string.</p>
	 *
	 * @param	dflt		what to return if there are no (valid) string values
	 * @param	format		the format to use for each numerical or decimal value (null if none)
	 * @return				the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrDefault(String dflt,NumberFormat format) {
		StringBuffer str = new StringBuffer();
		str.append("<");
		String separator = "";
		Iterator i = iterator();
		while (i.hasNext()) {
			str.append(separator);
			str.append(((SequenceItem)i.next()).getDelimitedStringValuesOrDefault(dflt,format,","));
			separator=",";
		}
		str.append(">");
		return str.toString();
	}
	
	/**
	 * <p>Extract the string values of the specified attribute from within all the items of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @param	dflt		what to return if there is no (valid) string value
	 * @param	format		the format to use for each numerical or decimal value (null if none)
	 * @return				an array of String with one element per Sequence item, zero length if no items or absent
	 */
	public static String[] getArrayOfSingleStringValueOrDefaultOfNamedAttributeWithinSequenceItems(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag,String dflt,NumberFormat format) {
		String[] values = null;
		Attribute a = list.get(sequenceTag);
		if (a != null && a instanceof SequenceAttribute) {
			SequenceAttribute sa = (SequenceAttribute)a;
			int n = sa.getNumberOfItems();
			values = new String[n];
			for (int i=0;i<n;++i) {
				values[i] = Attribute.getSingleStringValueOrDefault(sa.getItem(i).getAttributeList(),namedTag,dflt,format);
			}
		}
		return values == null ? new String[0] : values;
	}
	
	/**
	 * <p>Extract the string values of the specified attribute from within all the items of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @param	dflt		what to return if there is no (valid) string value
	 * @return				an array of String with one element per Sequence item, zero length if no items or absent
	 */
	public static String[] getArrayOfSingleStringValueOrDefaultOfNamedAttributeWithinSequenceItems(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag,String dflt) {
		return getArrayOfSingleStringValueOrDefaultOfNamedAttributeWithinSequenceItems(list,sequenceTag,namedTag,dflt,null);
	}
	
	/**
	 * <p>Extract the string values of the specified attribute from within all the items of the specified sequence from within a list of attributes.</p>
	 *
	 * @param	list		the list that contains the sequence (may not be null)
	 * @param	sequenceTag	the tag of the sequence attribute that has one item
	 * @param	namedTag	the tag of the attribute within the item of the sequence
	 * @return				an array of String with one element per Sequence item, zero length if no items or absent
	 */
	public static String[] getArrayOfSingleStringValueOrEmptyStringOfNamedAttributeWithinSequenceItems(AttributeList list,AttributeTag sequenceTag,AttributeTag namedTag) {
		return getArrayOfSingleStringValueOrDefaultOfNamedAttributeWithinSequenceItems(list,sequenceTag,namedTag,"");
	}

}


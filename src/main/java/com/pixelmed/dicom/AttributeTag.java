/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>An individual DICOM data element (attribute) tag that
 * includes a group and element (each a 16 bit unsigned binary).</p>
 *
 * <p>Implements {@link java.lang.Comparable Comparable} in order to facilitate sorting (e.g. in lists
 * which are indexed by AttributeTag).</p>
 * 
 * <p>Safe to use in hashed collections such as {@link java.util.Hashtable Hashtable} and {@link java.util.HashMap HashMap}
 * (i.e. it takes care to implement {@link java.lang.Object#hashCode() hashCode()} and {@link java.lang.Object#equals(Object) equals()} consistently).</p>
 * 
 * @author	dclunie
 */
public class AttributeTag implements Comparable {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTag.java,v 1.22 2017/01/24 10:50:35 dclunie Exp $";

	/***/
	private int group;
	/***/
	private int element;

	/**
	 * <p>Construct a DICOM data element (attribute) tag.</p>
	 *
	 * @param	group	the 16 bit unsigned binary group
	 * @param	element	the 16 bit unsigned binary element
	 */
	public AttributeTag(int group,int element) {
		this.group=group;
		this.element=element;
	}

	/**
	 * <p>Construct a DICOM data element (attribute) tag from its string representation.</p>
	 *
	 * @param	s	a String of the form returned by {@link #toString() toString()}, i.e., "(0xgggg,0xeeee)" where gggg and eeee are the zero-padded hexadecimal representations of the group and element respectively
	 * @throws	DicomException	if String is not a valid representation of a DICOM tag
	 */
	public AttributeTag(String s) throws DicomException {
		if (s == null) {
			throw new DicomException("Null string supplied to AttributeTag constructor \""+s+"\"");
		}
		String[] ss = s.trim().replaceFirst("^[(]","").replaceFirst("[)]$","").split(",");
		if (ss == null || ss.length != 2) {
			throw new DicomException("String without pair of group and element numbers supplied to AttributeTag constructor \""+s+"\"");
		}
		try {
			this.group=Integer.decode(ss[0]);
		}
		catch (NumberFormatException e) {
			throw new DicomException("String supplied to AttributeTag constructor contains non-numeric group \""+s+"\"");
		}
		try {
			this.element=Integer.decode(ss[1]);
		}
		catch (NumberFormatException e) {
			throw new DicomException("String supplied to AttributeTag constructor contains non-numeric element \""+s+"\"");
		}
	}

	/**
	 * <p>Get the group value.</p>
	 *
	 * @return	the 16 bit unsigned binary group
	 */
	public int getGroup()   { return group; }

	/**
	 * <p>Get the element value.</p>
	 *
	 * @return	the 16 bit unsigned binary element
	 */
	public int getElement() { return element; }

	/**
	 * <p>Is the tag a private tag ?</p>
	 *
	 * <p>Private tags are those with odd-numbered groups.</p>
	 *
	 * @return	true if private
	 */
	public boolean isPrivate() { return group%2 != 0; }

	/**
	 * <p>Is the tag a private creator tag ?</p>
	 *
	 * <p>Private creator tags are those with odd-numbered groups with elements between 0x0001 and 0x00ff.</p>
	 *
	 * @return	true if private creator
	 */
	public boolean isPrivateCreator() { return group%2 != 0 && element > 0x0000 && element <= 0x00ff; }

	/**
	 * <p>Is the tag a group length tag ?</p>
	 *
	 * <p>Group length tags are those with a zero element number.</p>
	 *
	 * @return	true if group length
	 */
	public boolean isGroupLength() { return element == 0; }

	/**
	 * <p>Is the tag a File Meta Information tag ?</p>
	 *
	 * <p>Group length tags are those with a group of 0x0002.</p>
	 *
	 * @return	true if is File Meta Information tag
	 */
	public boolean isFileMetaInformationGroup() { return group == 0x0002; }

	/**
	 * <p>Is the tag a member of an overlay group ?</p>
	 *
	 * <p>Overlay groups are even 6000-601e (overlays).</p>
	 *
	 * @return	true if overlay group
	 */
	public boolean isOverlayGroup() {
		int groupBase = group&0xff00;
		int groupItem = group&0x00ff;
		return (groupBase == 0x6000) && groupItem%2 == 0 && groupItem >= 0x0000 && groupItem <= 0x001e;
	}

	/**
	 * <p>Is the tag a member of a curve group ?</p>
	 *
	 * <p>Curve groups are even 5000-501e (curves) groups.</p>
	 *
	 * @return	true if curve group
	 */
	public boolean isCurveGroup() {
		int groupBase = group&0xff00;
		int groupItem = group&0x00ff;
		return (groupBase == 0x5000) && groupItem%2 == 0 && groupItem >= 0x0000 && groupItem <= 0x001e;
	}

	/**
	 * <p>Is the tag a member of a repeating group ?</p>
	 *
	 * <p>Repeating groups are even 6000-601e (overlays) and 5000-501e (curves) groups.</p>
	 *
	 * @return	true if repeating group
	 */
	public boolean isRepeatingGroup() {
		int groupBase = group&0xff00;
		int groupItem = group&0x00ff;
		return (groupBase == 0x6000 || groupBase == 0x5000) && groupItem%2 == 0 && groupItem >= 0x0000 && groupItem <= 0x001e;
	}

	/**
	 * <p>Make a new tag with the same element but with the base group of the repeating group.</p>
	 *
	 * <p>Helps with dictionary lookup of VR, etc.</p>
	 *
	 * <p>E.g., any group 6000-601e would be converted to 6000</p>
	 *
	 * @return	true if repeating group
	 */
	public AttributeTag getTagWithRepeatingGroupBase() {
		return new AttributeTag(group&0xff00,element);
	}

	/**
	 * <p>Get a human-readable rendering of the tag.</p>
	 *
	 * <p>This takes the form "(0xgggg,0xeeee)" where gggg and eeee are the zero-padded
	 * hexadecimal representations of the group and element respectively.</p>
	 *
	 * @return	the string rendering of the tag
	 */
	public String toString() {
		return toString(group,element);
	}

	/**
	 * <p>Get a human-readable rendering of the tag.</p>
	 *
	 * <p>This takes the form "(0xgggg,0xeeee)" where gggg and eeee are the zero-padded
	 * hexadecimal representations of the group and element respectively.</p>
	 *
	 * @param	group	the 16 bit unsigned binary group
	 * @param	element	the 16 bit unsigned binary element
	 * @return	the string rendering of the tag
	 */
	public static String toString(int group,int element) {
		StringBuffer str = new StringBuffer();
		str.append("(0x");
		String groupString = Integer.toHexString(group);
		for (int i=groupString.length(); i<4; ++i) str.append("0");
		str.append(groupString);
		str.append(",0x");
		String elementString = Integer.toHexString(element);
		for (int i=elementString.length(); i<4; ++i) str.append("0");
		str.append(elementString);
		str.append(")");
		return str.toString();
	}

	/**
	 * <p>Compare tags based on the numeric order of their group and then element values.</p>
	 *
	 * @param	o	the {@link com.pixelmed.dicom.AttributeTag AttributeTag} to compare this {@link com.pixelmed.dicom.AttributeTag AttributeTag} against
	 * @return		the value 0 if the argument tag is equal to this object; a value less than 0 if this tag is
	 *			less than the argument tag; and a value greater than 0 if this tag is greater than the argument tag
	 */
	public int compareTo(Object o) {
		return group == ((AttributeTag)o).group
			? (element == ((AttributeTag)o).element ? 0 : (element < ((AttributeTag)o).element ? -1 : 1))
			: (group < ((AttributeTag)o).group ? -1 : 1)
			;
	}

	/**
	 * <p>Compare tags based on their group and element values.</p>
	 *
	 * @param	o	the {@link com.pixelmed.dicom.AttributeTag AttributeTag} to compare this {@link com.pixelmed.dicom.AttributeTag AttributeTag} against
	 * @return		true if the same group and element number
	 */
	public boolean equals(Object o) {
//System.err.println("AttributeTag.equals: "+this+" vs. "+(AttributeTag)o+" = "+(group == ((AttributeTag)o).getGroup() && element == ((AttributeTag)o).getElement()));
		return group == ((AttributeTag)o).group && element == ((AttributeTag)o).element;
	}

	/**
	 * <p>Get a hash value which represents the tag.</p>
	 *
	 * <p>This method is implemented to override {@link java.lang.Object#hashCode() java.lang.Object.hashCode()}
	 * so as to comply with the contract that two tags that return true for equals()
	 * will return the same value for hashCode(), which would not be the case
	 * unless overridden (i.e. two allocations of a tag with the same group and
	 * element would be equal but the default implementation would return different hash values).</p>
	 *
	 * @return	a hash value representing the tag
	 */
	public int hashCode() {
		return (group<<16)+(element&0xffff);
	}

	public static void main(String arg[]) {
		int group = 0x0010;
		int element = 0x0020;
		AttributeTag t1 = new AttributeTag(group,element);
		System.err.println("Test numeric constructor: "+(t1.getGroup() == group && t1.getElement() == element ? "PASS" : "FAIL"));
		String t1s = t1.toString();
		try {
			AttributeTag t2 = new AttributeTag(t1s);
			System.err.println("Test string round trip:   "+(t1.getGroup() == group && t1.getElement() == element ? "PASS" : "FAIL"));
		}
		catch (Exception e) {
			System.err.println("Test string round trip:   FAIL with exception "+e);
		}
	}
	
}




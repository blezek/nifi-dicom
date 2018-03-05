/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * <p>Instances of the {@link com.pixelmed.dicom.AttributeTreeRecord AttributeTreeRecord} class represent
 * nodes in a tree of the {@link com.pixelmed.dicom.AttributeTree AttributeTree} class, which in
 * turn is used by the {@link com.pixelmed.dicom.AttributeTreeBrowser AttributeTreeBrowser} class.</p>
 *
 * <p>Each record represents a single attribute.</p>
 *
 * @author	dclunie
 */
public class AttributeTreeRecord implements Comparable, TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTreeRecord.java,v 1.15 2017/01/24 10:50:36 dclunie Exp $";

	AttributeTreeRecord parent;
	Collection children;
	TreeNode[] array;
	Attribute attribute;		// will have a value for actual attributes
	int itemCount;			// otherwise will be an item, in which case this count will be valid
	DicomDictionary dictionary;
	boolean sortByName;		// true if sort by name, else sort by element number

	/**
	 * <p>Dump the record as a string.</p>
	 *
	 * @return		the attribute tag, name and value(s) as a string
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		if (attribute != null) {
			AttributeTag tag = attribute.getTag();
			str.append(tag.toString());
			if (dictionary != null) {
				String dictName=dictionary.getNameFromTag(tag);
				if (dictName != null) {
					str.append(" ");
					str.append(dictName);
				}
			}
			String value = attribute.getDelimitedStringValuesOrEmptyString();
			if (value != null && value.length() > 0) {
				str.append(" = "); 
				str.append(value);
			}
		}
		else if (itemCount > 0) {		// zero is not valid, e.g. root not an item
			str.append("Item ");
			str.append(Integer.toString(itemCount));
		}
		return str.toString();
	}
	
	// Methods to implement Comparable (allows parent to sort)

	public int compareTo(Object o) {
		AttributeTreeRecord cf = (AttributeTreeRecord)o;
		Attribute cfAttribute = cf.getAttribute();
		int cfItemCount = cf.getItemCount();
		if (attribute != null && cfAttribute != null) {
			AttributeTag tag = attribute.getTag();
			AttributeTag cfTag = cfAttribute.getTag();
			if (sortByName && dictionary != null) {
				String attributeName = dictionary.getNameFromTag(tag);
				String cfAttributeName = dictionary.getNameFromTag(cfTag);
				if (attributeName != null && cfAttributeName != null) {         // WARNING: two different tags with the same name will match and the 2nd won't
					return attributeName.compareTo(cfAttributeName);        // get added to the TreeSet of children of this node :(
				}
				else if (attributeName != null && cfAttributeName == null) {	// those with missing names always sort after those with names
					return -1;
				}
				else if (attributeName == null && cfAttributeName != null) {
					return 1;
				}
				else {
					return tag.compareTo(cfTag);
				}
			}
			else {
				return tag.compareTo(cfTag);
			}
		}
		else {
			return itemCount - cfItemCount;	// will appear equal if both 0 (even though 0 is not a valid value)
		}
	}

	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}

	// Methods to help with Comparable support

	//protected String getStringValue(AttributeTreeRecord record) {
	//	return "BAD";
	//}

	//protected int getIntegerValue(AttributeTreeRecord record) {
	//	return 0;
	//}

	// Methods to implement TreeNode ...

	/**
	 * <p>Returns the parent node of this node.</p>
	 *
	 * @return	the parent node, or null if the root
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * <p>Returns the child at the specified index.</p>
	 *
	 * @param	index	the index of the child to be returned, numbered from 0
	 * @return		the child <code>TreeNode</code> at the specified index
	 */
	public TreeNode getChildAt(int index) {
		int n=children.size();
		if (array == null) {
			array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
		}
		return index < n ? array[index] : null;
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		int n=children.size();
		if (array == null) {
			array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
		}
		for (int i=0; i<n; ++i) {
			if (getChildAt(i).equals(child)) {	// expensive comparison ? :(
//System.err.println("getIndexOfChild: found "+child);
				return i;
			}
		}
		return -1;
	}

	/**
	 * <p> Always returns true, since children may always be added.</p>
	 *
	 * @return	always true
	 */
	public boolean getAllowsChildren() {
		return true;
	}

	/**
	 * <p> Returns true if the receiver is a leaf (has no children).</p>
	 *
	 * @return	true if the receiver is a leaf
	 */
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	/**
	 * <p>Return the number of children that this node contains.</p>
	 *
	 * @return	the number of children, 0 if none
	 */
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	/**
	 * <p>Returns the children of this node as an {@link java.util.Enumeration Enumeration}.</p>
	 *
	 * @return	the children of this node
	 */
	public Enumeration children() {
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...

	/**
	 * <p>Construct a record for an attribute.</p>
	 *
	 * @param	p	parent record
	 * @param	a	attribute to add
	 * @param	d	dictionary for looking up the name
	 */
	public AttributeTreeRecord(AttributeTreeRecord p,Attribute a,DicomDictionary d) {
		dictionary = (d == null) ? new DicomDictionary() : d;
		parent=p;
		attribute=a;
		sortByName=true;
	}

	/**
	 * <p>Construct a record for an item of a sequence attribute.</p>
	 *
	 * @param	p	parent record
	 * @param	ic	which item (numbered from 0)
	 */
	public AttributeTreeRecord(AttributeTreeRecord p,int ic) {
		dictionary=null;
		parent=p;
		attribute=null;
		itemCount=ic;
		sortByName=true;
	}

	/**
	 * <p>Add a child node to the current node, keeping the children sorted.</p>
	 *
	 * @param	child	the child to add
	 */
	public void addChild(AttributeTreeRecord child) {
		if (children == null) children=new TreeSet();	// is sorted
		children.add(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	child	the child to remove
	 */
	public void removeChild(AttributeTreeRecord child) {
		children.remove(child);
		array=null;					// cache is dirty
	}

	/**
	 * <p>Remove all child nodes.</p>
	 */
	public void removeAllChildren() {
		children=null;
		array=null;					// cache is dirty
	}

	/**
	 * <p>Add a sibling to the current node, keeping the children sorted.</p>
	 *
	 * @param		sibling				the sibling to add
	 * @throws	DicomException		if attempt to add sibling to node without parent
	 */
	public void addSibling(AttributeTreeRecord sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Get the attribute corresponding to this record.</p>
	 *
	 * @return	the attribute
	 */
	public Attribute getAttribute() { return attribute; }
	
	/**
	 * <p>Get the number of items in a Sequence attribute record.</p>
	 *
	 * @return	the number of items
	 */
	public int getItemCount() { return itemCount; }

	/**
	 * <p>Set the sort order to be alphabetical by attribute name, or numerical by group and element tag.</p>
	 *
	 * @param	sortByName	true if sort alphabetically by attribute name
	 */
	public void setSortByName(boolean sortByName) {
		this.sortByName=sortByName;
	}
}




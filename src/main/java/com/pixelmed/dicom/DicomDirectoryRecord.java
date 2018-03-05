/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * @author	dclunie
 */
public abstract class DicomDirectoryRecord implements Comparable, TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryRecord.java,v 1.23 2017/01/24 10:50:36 dclunie Exp $";

	DicomDirectoryRecord parent;
	Collection children;
	TreeNode[] array;
	AttributeList list;
	
	protected String uid;
	protected String stringValue;
	protected int integerValue;

	// Methods to implement Comparable (allows parent to sort)

	public int compareTo(Object o) {
//System.err.println("DicomDirectoryRecord.compareTo(): comparing classes "+this.getClass()+" with "+o.getClass());
		return compareToThatReturnsZeroOnlyIfSameObject(o);
	}
	
	// Establish that the natural ordering is NOT consistent with equals,
	// otherwise when two records are read that have the same Attributes
	// but different children in the DICOMDIR, one will be overwritten if
	// added to a set (e.g., if the DICOMDIR contains "duplicate"
	// PATIENT records, one for each STUDY, rather than having merged
	// them during creation
	
	// the consequences of natural ordering not being consistent with equals
	// is discussed in the JavaDoc for java.lang.Comparable
	
	// also, be sure that none of the sub-classes of DicomDirectoryRecord in
	// DicomDirectoryRecordFactory do NOT override equals() (though they may
	// override compareTo()

	public boolean equals(Object o) {
		boolean areEqual = super.equals(o);
//System.err.println("DicomDirectoryRecord.equals(): comparing classes "+this.getClass()+" with "+o.getClass()+" with result "+areEqual);
		return areEqual;
		//return compareTo(o) == 0;
	}

	// Methods to help with Comparable support
	
	/**
	 * <p>Make the value that will be retured on a call to {@link com.pixelmed.dicom.DicomDirectoryRecord#getStringValue() getStringValue()}.</p>
	 */
	abstract protected void makeStringValue();

	/**
	 * <p>Make the value that will be retured on a call to {@link com.pixelmed.dicom.DicomDirectoryRecord#getIntegerValue() getIntegerValue()}.</p>
	 */
	abstract protected void makeIntegerValue();

	/**
	 * @return	a {@link java.lang.String String} describing this directory record containing identifiers, dates, etc.
	 */
	protected String getStringValue() {
		return stringValue;
	}

	/**
	 * @return	an integer describing this directory record derived from an appropriate number for the entity that the record represents
	 */
	protected int getIntegerValue() {
		return integerValue;
	}

	/**
	 * @return	the uid for the entity that the directory record represents
	 */
	protected final String getUIDForComparison() { return uid; }

	/**
	 * <p>Compares this directory record with the specified directory record for order based on whether they are the same Java object.</p>
	 *
	 * @param	o	the directory record to compare with
	 * @return		zero if equals() otherwise 1
	 */
	private int compareToThatReturnsZeroOnlyIfSameObject(Object o) {	// private so that this cannot be overridden
//System.err.println("DicomDirectoryRecord.compareToThatReturnsZeroOnlyIfSameObject(): comparing classes "+this.getClass()+" with "+o.getClass());
//new Throwable().printStackTrace();
//System.err.println("DicomDirectoryRecord.compareToThatReturnsZeroOnlyIfSameObject(): super.equals(o) = "+super.equals(o));
		return super.equals(o) ? 0 : 1;	// no particular order unless class is specialized, but can return 0 when identical objects (not otherwise else conflict in Set)
	}
	
	/**
	 * <p>Compares this object with the specified directory record for order based on string value.</p>
	 *
	 * <p>Considers whether same record type, same string value, and if so, orders by UID.</p>
	 *
	 * @param	record						the directory record to compare with
	 * @param	mustBeSameObjectToBeEqual	if true requires them to be the same Java object, not just the same UID
	 * @return								a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the directory record.
	 */
	protected final int compareToByStringValue(DicomDirectoryRecord record,boolean mustBeSameObjectToBeEqual) {
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): comparing classes "+this.getClass()+" with "+record.getClass());
		if (this.getClass().equals(record.getClass())) {
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): same class");
			int strComparison = toString().compareTo(record.toString());
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): strComparison = "+strComparison);
			if (strComparison == 0) {
				int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
//System.err.println("DicomDirectoryRecord.compareToByStringValue(): uidComparison = "+uidComparison);
				if (uidComparison == 0) {
					// same UIDs (or no UIDs)
					return mustBeSameObjectToBeEqual ? compareToThatReturnsZeroOnlyIfSameObject(record) : 0;
				}
				else {
					return uidComparison;	// same string but different UID; distinguish and order consistently
				}
			}
			else {
				return strComparison;
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type, hence will always be different and no need for compareToThatReturnsZeroOnlyIfSameObject() check
		}
	}

	/**
	 * <p>Compares this object with the specified directory record for order based on integer value.</p>
	 *
	 * <p>Considers whether same record type, same integer value, and if so orders by string value, then by UID.</p>
	 *
	 * @param	record						the directory record to compare with
	 * @param	mustBeSameObjectToBeEqual	if true requires them to be the same Java object, not just the same UID
	 * @return								a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the directory record.
	 */
	protected final int compareToByIntegerValue(DicomDirectoryRecord record,boolean mustBeSameObjectToBeEqual) {
//System.err.println("DicomDirectoryRecord.compareToByIntegerValue(): comparing classes "+this.getClass()+" with "+record.getClass());
		if (this.getClass().equals(record.getClass())) {
			int intComparison = getIntegerValue() - record.getIntegerValue();
			if (intComparison == 0) {
				int strComparison = toString().compareTo(record.toString());
				if (strComparison == 0) {
					int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
					if (uidComparison == 0) {
						// same UIDs (or no UIDs)
						return mustBeSameObjectToBeEqual ? compareToThatReturnsZeroOnlyIfSameObject(record) : 0;
					}
					else {
						return uidComparison;	// same integer and string but different UID; distinguish and order consistently
					}
				}
				else {
					return strComparison;		// same integer values but different string; distinguish and order consistently
				}
			}
			else {
				return intComparison;
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type, hence will always be different and no need for compareToThatReturnsZeroOnlyIfSameObject() check
		}
	}

	// Methods to implement TreeNode ...

	public TreeNode getParent() {
		return parent;
	}

	public TreeNode getChildAt(int index) {
		int n=children.size();
		if (array == null) {
			array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
		}
		return index < n ? array[index] : null;
	}

	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		if (children != null) {
			int n=children.size();
			if (array == null) {
				array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
			}
			for (int i=0; i<n; ++i) {
				if (((DicomDirectoryRecord)getChildAt(i)).compareToByStringValue((DicomDirectoryRecord)child,false/*mustBeSameObjectToBeEqual*/) == 0) {	// expensive comparison ? :(; just require string, not object, match
//System.err.println("getIndexOfChild: found "+child);
					return i;
				}
			}
		}
		return -1;
	}

	public boolean getAllowsChildren() {
		return true;
	}

	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	public Enumeration children() {
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...

	/**
	 * @param	p	directory record
	 * @param	l	list of attributes
	 */
	public DicomDirectoryRecord(DicomDirectoryRecord p,AttributeList l) {
		parent=p;
		list=l;
		makeIntegerValue();
		makeStringValue();
	}

	/**
	 * @param	child	child directory record to add
	 */
	public void addChild(DicomDirectoryRecord child) {
		if (children == null) children=new TreeSet();	// is sorted
		children.add(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	child	child directory record to remove
	 */
	public void removeChild(DicomDirectoryRecord child) {
		children.remove(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	sibling			sibling to add
	 * @throws	DicomException	if no parent
	 */
	public void addSibling(DicomDirectoryRecord sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Set the parent node of this node.</p>
	 *
	 * @param	parent	parent directory record
	 */
	public void setParent(DicomDirectoryRecord parent) {
		this.parent = parent;
	}

	/**
	 * @return a list of attributes for this directory record
	 */
	public AttributeList getAttributeList() { return list; }
}




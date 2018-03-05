/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.util.*;
import javax.swing.tree.*;

import com.pixelmed.dicom.*;
import com.pixelmed.utils.StringUtilities;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Instances of the {@link com.pixelmed.database.DatabaseTreeRecord DatabaseTreeRecord} class represent
 * nodes in a tree of the {@link com.pixelmed.database.DatabaseTreeModel DatabaseTreeModel} class, which in
 * turn is used by the {@link com.pixelmed.database.DatabaseTreeBrowser DatabaseTreeBrowser} class.</p>
 *
 * <p>This class is publically visible primarily so that selection change listeners can be
 * constructed for {@link com.pixelmed.database.DatabaseTreeBrowser DatabaseTreeBrowser}, since
 * the user's selection is returned as a path of {@link com.pixelmed.database.DatabaseTreeRecord DatabaseTreeRecord}
 * instances, which need to be cast accordingly.</p>
 *
 * @author	dclunie
 */
public class DatabaseTreeRecord implements Comparable, MutableTreeNode {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseTreeRecord.java,v 1.28 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DatabaseTreeRecord.class);

	private DatabaseInformationModel d;
	private DatabaseTreeRecord parent;
	private List children;
	private InformationEntity ie;
	private String localPrimaryKeyValue;
	private String localFileNameValue;
	private String localFileReferenceTypeValue;
	private String value;
	
	private boolean childrenPopulated;

	/**
	 * <p>Dump the contents of the node.</p>
	 *
	 * @return	the contents of this node
	 */
	public String dump() {
		StringBuffer buf = new StringBuffer();
		buf.append(ie);
		buf.append(" (localPrimaryKeyValue=\"");
		buf.append(localPrimaryKeyValue);
		buf.append("\", localFileNameValue=\"");
		buf.append(localFileNameValue);
		buf.append("\", localFileReferenceTypeValue=\"");
		buf.append(localFileReferenceTypeValue);
		buf.append("\", value=\"");
		buf.append(value);
		buf.append("\")");
		return buf.toString();
	}
	
	/**
	 * <p>Return the string value of the node.</p>
	 *
	 * @return	the string value of this node
	 */
	public String toString() {
		return value == null ? "" : value;
	}
		
	// Methods to implement Comparable (allows parent to sort)

	/**
	 * <p>Compare nodes based on the lexicographic order of their string values.</p>
	 *
	 * <p>Note that the comparison is more complex than a simple lexicographic comparison
	 * of strings (as described in the definition of {@link java.lang.String#compareTo(String) java.lang.String.compareTo(String)}
	 * but rather accounts for embedded non-zero padded integers. See {@link com.pixelmed.utils.StringUtilities#compareStringsWithEmbeddedNonZeroPaddedIntegers(String,String) com.pixelmed.utils.compareStringsWithEmbeddedNonZeroPaddedIntegers(String,String)}
	 * </p>
	 *
	 * <p>If the string values are equal but they are not the same database record, then an arbitrary but consistent order is return.</p>
	 *
	 * @param	o	the {@link com.pixelmed.database.DatabaseTreeRecord DatabaseTreeRecord}
	 *			to compare this {@link com.pixelmed.database.DatabaseTreeRecord DatabaseTreeRecord} against
	 * @return		the value 0 if the argument is equal to this object; a value less than 0 if this object
	 *			is lexicographically less than the argument; and a value greater than 0 if this object
	 *			is lexicographically greater than the argument
	 */
	public int compareTo(Object o) {
		DatabaseTreeRecord otherRecord = (DatabaseTreeRecord)o;
		String otherLocalPrimaryKeyValue = otherRecord.getLocalPrimaryKeyValue();
//System.err.println("DatabaseTreeRecord.compareTo(): our   primary key = <"+getLocalPrimaryKeyValue()+"> for <"+getValue()+">");
//System.err.println("DatabaseTreeRecord.compareTo(): other primary key = <"+otherLocalPrimaryKeyValue+"> for <"+otherRecord.getValue()+">");
		if (getLocalPrimaryKeyValue() == null && otherLocalPrimaryKeyValue == null) {
			return 0;	// only occurs for top level database root node
		}
		int primaryKeyComparison = getLocalPrimaryKeyValue().compareTo(otherLocalPrimaryKeyValue);
		//if (primaryKeyComparison == 0) {
		//	return 0;				// always equal if same primary key
		//}
		//else {
		{
			int strComparison = StringUtilities.compareStringsWithEmbeddedNonZeroPaddedIntegers(getValue(),otherRecord.getValue());
			if (strComparison == 0) {
				return primaryKeyComparison;	// same string but different primary key; distinguish and order consistently
			}
			else {
				return strComparison;
			}
		}
	}

	/**
	 * @param	o
	 */
	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}

	// Additional methods to implement MutableTreeNode ...

	/**
	 * <p>Sets the parent of the node to newParent.</p>
	 *
	 * param		newParent the new parent node, or null if the root
	 */
	public void setParent(MutableTreeNode newParent) {
		parent = (DatabaseTreeRecord)newParent;
	}
	
	/**
	 * <p>Removes the node from its parent.</p>
	 *
	 */
	public void removeFromParent() {
		if (parent != null) {
			parent.remove(this);
		}
	}
	
	/**
	 * <p>Removes the specified child from this node.</p>
	 *
	 * <p>node.setParent(null) will be called.</p>
	 *
	 * <p>The database entry corresponding to the node will actually be deleted.</p>
	 *
	 * @param	node
	 */
	public void remove(MutableTreeNode node) {
		if (node != null) {
			int n=getChildCount();			// not children.size(), to force population of children (from actual database) if not already done
			if (children != null && n > 0) {
				Vector newChildren = new Vector(n-1);
				for (int i=0; i<n; ++i) {
					DatabaseTreeRecord current = (DatabaseTreeRecord)(children.get(i));
					if (current.equals(node)) {
						try {
							d.deleteRecord(current.getInformationEntity(),current.getLocalPrimaryKeyValue());	// take care to use IE d key of child not parent (this) node !
						}
						catch (DicomException e) {
							slf4jlogger.error("Ignoring exception during record deletion",e);
						}
					}
					else {
						newChildren.add(current);
					}
				}
			}
			node.setParent(null);
		}
	}
	
	
	/**
	 * <p>Removes the child at index from this node.</p>
	 *
	 * <p>node.setParent(null) will be called.</p>
	 *
	 * <p>The database entry corresponding to the node will actually be deleted.</p>
	 *
	 * @param	index
	 */
	public void remove(int index) {
		if (index > 0) {
			int n=getChildCount();			// not children.size(), to force population of children (from actual database) if not already done
			if (children != null && n > 0 && index < n) {
				MutableTreeNode child = (MutableTreeNode)(children.get(index));
				if (child != null) {
					remove(child);
				}
			}
		}
	}
	
	/**
	 * <p>Should add child to the node at index - but is not implemented.</p>
	 *
	 * <p>child.setParent(null) would be called if implemented</p>
	 *
	 * <p>The database entries corresponding to the child and node would need to be updated.</p>
	 *
	 * @param	child
	 * @param	index
	 */
	public void insert(MutableTreeNode child,int index) {
		assert(false);
	}

	/**
	 * <p>Resets the user object of the node to object - but is not implemented since user objects are not required.</p>
	 *
	 * @param	object	ignored
	 */
	public void setUserObject(Object object) {
		assert(false);
	}

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
		return index < getChildCount() ? (TreeNode)(children.get(index)) : null;	// side effect of getChildCount() is to force population of children if required
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		int n=getChildCount();				// rather than children.size(), to force population if necessary
		for (int i=0; i<n; ++i) {
			if (children.get(i).equals(child)) {	// expensive comparison ? :(
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
//System.err.println("DatabaseTreeRecord.getChildCount() for "+this);
		if (!childrenPopulated) {
			populateChildren();
			childrenPopulated = true;
		}
		return children == null ? 0 : children.size();
	}

	/**
	 * <p>Returns the children of this node as an {@link java.util.Enumeration Enumeration}.</p>
	 *
	 * @return	the children of this node
	 */
	public Enumeration children() {
		getChildCount();			// to force population of children if required
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...
	
	private void populateChildren() {
//System.err.println("DatabaseTreeRecord.populateChildren() for "+this);
		InformationEntity childIE = ie == null ? d.getRootInformationEntity() : d.getChildTypeForParent(ie);

		// all column name get() methods return upper case, as do names to match against in result set returned records
		String descriptiveColumnName = d.getDescriptiveColumnName(childIE);
		String otherColumnName = d.getOtherDescriptiveColumnName(childIE);
		String otherOtherColumnName = d.getOtherOtherDescriptiveColumnName(childIE);
		String localPrimaryKeyColumnName = d.getLocalPrimaryKeyColumnName(childIE);
		String localFileNameColumnName = d.getLocalFileNameColumnName(childIE);
		String localFileReferenceTypeColumnName = d.getLocalFileReferenceTypeColumnName(childIE);
//System.err.println("DatabaseTreeRecord.populateChildren(): childIE="+childIE+" descriptiveColumnName="+descriptiveColumnName+" otherColumnName="+otherColumnName+" =localPrimaryKeyColumnName"+localPrimaryKeyColumnName);
//System.err.println("DatabaseTreeRecord.populateChildren(): localPrimaryKeyValue "+localPrimaryKeyValue);
		DatabaseTreeRecord first = null;
		ArrayList returnedRecords = null;
		try {
			returnedRecords = localPrimaryKeyValue == null
				? d.findAllAttributeValuesForAllRecordsForThisInformationEntity(childIE)
				: d.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(childIE,localPrimaryKeyValue);
			if (childIE != null && localPrimaryKeyValue != null && returnedRecords == null || returnedRecords.size() == 0) {
				// recurse for next lower down type of child IE (e.g. we may be skipping over concatenation from series to instance)
				childIE = d.getChildTypeForParent(childIE);
//System.err.println("DatabaseTreeRecord.populateChildren(): skipping empty ie "+childIE+" to get to "+childIE);
				if (childIE != null) {
					descriptiveColumnName = d.getDescriptiveColumnName(childIE);
					otherColumnName = d.getOtherDescriptiveColumnName(childIE);
					otherOtherColumnName = d.getOtherOtherDescriptiveColumnName(childIE);
					localPrimaryKeyColumnName = d.getLocalPrimaryKeyColumnName(childIE);
					localFileNameColumnName = d.getLocalFileNameColumnName(childIE);
					localFileReferenceTypeColumnName = d.getLocalFileReferenceTypeColumnName(childIE);
					returnedRecords = d.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(childIE,localPrimaryKeyValue);
				}
			}
		}
		catch (DicomException e) {
			slf4jlogger.error("", e);;
			returnedRecords = null;
		}

//System.err.println("DatabaseTreeRecord.populateChildren(): returnedRecords.size() = "+(returnedRecords == null ? 0 : returnedRecords.size()));
		if (returnedRecords != null && returnedRecords.size() > 0) {
			for (int i=0; i<returnedRecords.size(); ++i) {
//System.err.println("DatabaseTreeRecord.populateChildren(): record "+i);
				String value = null;
				Map returnedAttributes = (Map)(returnedRecords.get(i));
				if (returnedAttributes != null) {
//System.err.println("DatabaseTreeRecord.populateChildren(): returnedAttributes "+returnedAttributes);
					// NB. keep the name lower case in the returned map, but be sure to check upper case in what came back from database
					String descriptiveColumnValue = descriptiveColumnName == null ? null : (String)(returnedAttributes.get(descriptiveColumnName));
					String otherColumnValue = otherColumnName == null ? null : (String)(returnedAttributes.get(otherColumnName));
					String otherOtherColumnValue = otherOtherColumnName == null ? null : (String)(returnedAttributes.get(otherOtherColumnName));
					String localPrimaryKeyColumnValue = localPrimaryKeyColumnName == null ? null : (String)(returnedAttributes.get(localPrimaryKeyColumnName));
					String localFileNameColumnValue = localFileNameColumnName == null ? null : (String)(returnedAttributes.get(localFileNameColumnName));
					String localFileReferenceTypeColumnValue = localFileReferenceTypeColumnName == null ? null : (String)(returnedAttributes.get(localFileReferenceTypeColumnName));
					StringBuffer buf = new StringBuffer();
					buf.append(d.getNametoDescribeThisInstanceOfInformationEntity(childIE,returnedAttributes));
					String sopClassUID = (String)(returnedAttributes.get("SOPCLASSUID"));
					if (childIE == InformationEntity.INSTANCE && sopClassUID != null && SOPClass.isImageStorage(sopClassUID)) {
						buf.append(" ");
						buf.append(DescriptionFactory.makeImageDescription(returnedAttributes));
					}
					else if (childIE == InformationEntity.SERIES) {
						buf.append(" ");
						buf.append(DescriptionFactory.makeSeriesDescription(returnedAttributes));
					}
					else if (childIE == InformationEntity.PATIENT) {
						buf.append(" ");
						buf.append(DescriptionFactory.makePatientDescription(returnedAttributes));
					}
					else {
						if (descriptiveColumnValue != null) {
							buf.append(" ");
							buf.append(descriptiveColumnValue);
						}
						if (otherColumnValue != null) {
							buf.append(" ");
							buf.append(otherColumnValue);
						}
						if (otherOtherColumnValue != null) {
							buf.append(" ");
							buf.append(otherOtherColumnValue);
						}
					}
					value=buf.toString();
//System.err.println("DatabaseTreeRecord.populateChildren(): value "+value);
					DatabaseTreeRecord node = new DatabaseTreeRecord(d,this,value,childIE,localPrimaryKeyColumnValue,localFileNameColumnValue,localFileReferenceTypeColumnValue);
					addChild(node);
				}
			}
		}
	}

	/**
	 * <p>Make a new node in a
	 * tree.</p>
	 *
	 * @param	d			the database
	 * @param	parent			the parent of this node
	 * @param	value			a string value which is used primarily to sort siblings into lexicographic order
	 * @param	ie			the entity in the database information model that the constructed node is an instance of
	 * @param	localPrimaryKeyValue	the local primary key of the database record corresponding to this node
	 * @param	localFileNameValue	the file name that the database record points to (meaningful only for instance (image) level nodes)
	 */
	public DatabaseTreeRecord(DatabaseInformationModel d,DatabaseTreeRecord parent,String value,InformationEntity ie,String localPrimaryKeyValue,String localFileNameValue) {
		this(d,parent,value,ie,localPrimaryKeyValue,localFileNameValue,""/*localFileReferenceTypeValue*/);
	}

	/**
	 * <p>Make a new node in a
	 * tree.</p>
	 *
	 * @param	d			the database
	 * @param	parent			the parent of this node
	 * @param	value			a string value which is used primarily to sort siblings into lexicographic order
	 * @param	ie			the entity in the database information model that the constructed node is an instance of
	 * @param	localPrimaryKeyValue	the local primary key of the database record corresponding to this node
	 * @param	localFileNameValue	the file name that the database record points to (meaningful only for instance (image) level nodes)
	 * @param	localFileReferenceTypeValue	"C" for copied (i.e., delete on purge), "R" for referenced (i.e., do not delete on purge)
	 */
	public DatabaseTreeRecord(DatabaseInformationModel d,DatabaseTreeRecord parent,String value,InformationEntity ie,String localPrimaryKeyValue,String localFileNameValue,String localFileReferenceTypeValue) {
		this.d = d;
		this.parent=parent;
		this.value=value;
		this.ie=ie;
		this.localPrimaryKeyValue=localPrimaryKeyValue;
		this.localFileNameValue=localFileNameValue;
		this.localFileReferenceTypeValue=localFileReferenceTypeValue;
		childrenPopulated = false;
	}

	/**
	 * <p>Add a child to this nodes sorted collection of children.</p>
	 *
	 * @param	child	the child node to be added
	 */
	public void addChild(DatabaseTreeRecord child) {
		if (children == null) {
			children=new ArrayList();	// LinkedList seems to be about the same speed
		}
		// Next is from "http://javaalmanac.com/egs/java.util/coll_InsertInList.html?l=rel"
		// and is way faster than children.add(child) followed by Collections.sort(children)
		int index = Collections.binarySearch(children,child);
		if (index < 0) {
			children.add(-index-1,child);
		}
	}

	/**
	 * <p>Add a sibling to this node,
	 * that is add a child to this
	 * node's parent's sorted collection of children.</p>
	 *
	 * @param	sibling		the sibling node to be added
	 */
	public void addSibling(DatabaseTreeRecord sibling) {
		parent.addChild(sibling);
	}

	/**
	 * <p>Get the string value of the node which is used for sorting and human-readable rendering.</p>
	 *
	 * @return	the string value of this node
	 */
	public String getValue() { return value; }
	
	/**
	 * <p>Get the information entity that this node represents.</p>
	 *
	 * @return	information entity that this node represents
	 */
	public InformationEntity getInformationEntity() { return ie; }
	
	/**
	 * <p>Get the string value of the local primary key of the database record corresponding to this node.</p>
	 *
	 * @return	the string value of the local primary key
	 */
	public String getLocalPrimaryKeyValue() { return localPrimaryKeyValue; }
	
	/**
	 * <p>Get the file name that the database record points to (meaningful only for instance (image) level nodes).</p>
	 *
	 * @return	the file name
	 */
	public String getLocalFileNameValue() { return localFileNameValue; }
	
	/**
	 * <p>Get the type of reference to the file that the database record points to (meaningful only for instance (image) level nodes).</p>
	 *
	 * @return	the file reference type; "C" for copied (i.e., delete on purge), "R" for referenced (i.e., do not delete on purge)
	 */
	public String getLocalFileReferenceTypeValue() { return localFileReferenceTypeValue; }
	
	/**
	 * <p>Get the DatabaseInformationModel that the database record is used in.</p>
	 *
	 * @return	the DatabaseInformationModel
	 */
	public DatabaseInformationModel getDatabaseInformationModel() { return d; }
	
}

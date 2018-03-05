/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.lang.Comparable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.swing.tree.TreeNode;

/**
 * <p>Instances of the {@link com.pixelmed.scpecg.SCPTreeRecord SCPTreeRecord} class represent
 * nodes in a tree of the {@link com.pixelmed.scpecg.SCPTree SCPTree} class, which in
 * turn is used by the {@link com.pixelmed.scpecg.SCPTreeBrowser SCPTreeBrowser} class.</p>
 *
 * <p>Each record represents a single name-value pair, with the value potentially empty (e.g. a container).</p>
 *
 * @author	dclunie
 */
public class SCPTreeRecord implements Comparable, TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/SCPTreeRecord.java,v 1.8 2017/01/24 10:50:47 dclunie Exp $";

	private SCPTreeRecord parent;
	private List children;
	private String name;		// if null, flags that iname is used instead of name
	private int iname;
	private String value;

	/**
	 * <p>Dump the record as a string.</p>
	 *
	 * @return		name and value as a string
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(name == null ? Integer.toString(iname) : name);
		if (value != null) {
			str.append(" = "); 
			str.append(value);
		}
		return str.toString();
	}
	
	// Methods to implement Comparable (allows parent to sort)

	/**
	 * @param	o
	 */
	public int compareTo(Object o) {
//System.err.println("SCPTreeRecord.compareTo(): name="+name);
		SCPTreeRecord cf = (SCPTreeRecord)o;
//System.err.println("SCPTreeRecord.compareTo(): cf="+cf);
		if (name == null && cf.name == null) {
			return iname-cf.iname;			// sort by numeric name
		}
		else {
			String   useName =    name == null ? Integer.toString(   iname) :    name;
			String useCFName = cf.name == null ? Integer.toString(cf.iname) : cf.name;
			return useName.compareTo(useCFName);
		}
	}

	/**
	 * @param	o
	 */
	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}

	// Methods to help with Comparable support

	//protected String getStringValue(SCPTreeRecord record) {
	//	return "BAD";
	//}

	//protected int getIntegerValue(SCPTreeRecord record) {
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
		return (TreeNode)(children.get(index));
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
	 * <p>Construct a record for a name-value pair linking to the specified parent.</p>
	 *
	 * <p>Adds the new node to the children of the parent node, keeping them sorted.</p>
	 *
	 * @param	parent	parent record
	 * @param	name	the <code>String</code> name of this node (by which they are sorted)
	 * @param	value	the <code>String</code> value of this node
	 */
	public SCPTreeRecord(SCPTreeRecord parent,String name,String value) {
		this.parent=parent;
		this.name = name == null ? "-Unknown-" : name;
		this.value=value;
		if (parent != null) {
			parent.addChild(this);
		}
//System.err.println("SCPTreeRecord.SCPTreeRecord(): name="+name+" value="+value);
	}

	/**
	 * <p>Construct a record for a named container, linking to the specied parent.</p>
	 *
	 * <p>Adds the new node to the children of the parent node, keeping them sorted.</p>
	 *
	 * @param	parent	parent record
	 * @param	name	the <code>String</code> name of this node (by which they are sorted)
	 */
	public SCPTreeRecord(SCPTreeRecord parent,String name) {
		this.parent=parent;
		this.name = name == null ? "-Unknown-" : name;
		this.value=null;
		if (parent != null) {
			parent.addChild(this);
		}
//System.err.println("SCPTreeRecord.SCPTreeRecord(): name="+name);
	}

	/**
	 * <p>Construct a record for a name-value pair linking to the specified parent.</p>
	 *
	 * <p>Adds the new node to the children of the parent node, keeping them sorted.</p>
	 *
	 * @param	parent	parent record
	 * @param	iname	the numeric name of this node (by which they are sorted)
	 * @param	value	the <code>String</code> value of this node
	 */
	public SCPTreeRecord(SCPTreeRecord parent,int iname,String value) {
		this.parent=parent;
		this.name = null;	// flags that it is numeric
		this.iname = iname;
		this.value=value;
		if (parent != null) {
			parent.addChild(this);
		}
//System.err.println("SCPTreeRecord.SCPTreeRecord(): name="+name+" value="+value);
	}

	/**
	 * <p>Construct a record for a named container, linking to the specied parent.</p>
	 *
	 * <p>Adds the new node to the children of the parent node, keeping them sorted.</p>
	 *
	 * @param	parent	parent record
	 * @param	iname	the numeric name of this node (by which they are sorted)
	 */
	public SCPTreeRecord(SCPTreeRecord parent,int iname) {
		this.parent=parent;
		this.name = null;	// flags that it is numeric
		this.iname = iname;
		this.value=null;
		if (parent != null) {
			parent.addChild(this);
		}
//System.err.println("SCPTreeRecord.SCPTreeRecord(): name="+name);
	}

	/**
	 * <p>Add a child node to the current node, keeping the children sorted.</p>
	 *
	 * @param	child	the child to add
	 */
	private void addChild(SCPTreeRecord child) {
		if (children == null) children=new ArrayList();
		children.add(child);
		Collections.sort(children);
	}

	/**
	 * <p>Add a sibling to the current node, keeping the children sorted..</p>
	 *
	 * @param	sibling		the sibling to add
	 * @throws	DicomException
	 */
	//public void addSibling(SCPTreeRecord sibling) throws Exception {
	//	if (parent == null) {
	//		throw new Exception("Internal error - root node with sibling");
	//	}
	//	else {
	//		parent.addChild(sibling);
	//	}
	//}
}




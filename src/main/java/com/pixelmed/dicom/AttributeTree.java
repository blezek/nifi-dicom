/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.File;

/**
 * <p>The {@link com.pixelmed.dicom.AttributeTree AttributeTree} class implements a
 * {@link javax.swing.tree.TreeModel TreeModel} to abstract the contents of a list of attributes as
 * a tree in order to provide support for a {@link com.pixelmed.dicom.AttributeTreeBrowser AttributeTreeBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.tree.TreeModel javax.swing.tree.TreeModel}.</p>
 *
 * @see com.pixelmed.dicom.AttributeTreeBrowser
 * @see com.pixelmed.dicom.AttributeTreeRecord
 *
 * @author	dclunie
 */
public class AttributeTree implements TreeModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTree.java,v 1.11 2017/01/24 10:50:35 dclunie Exp $";

	// Our nodes are all instances of AttributeTreeRecord ...

	/***/
	private AttributeTreeRecord root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	public Object getChild(Object node,int index) {
		return ((AttributeTreeRecord)node).getChildAt(index);
	}

	public int getIndexOfChild(Object parent, Object child) {
		return ((AttributeTreeRecord)parent).getIndex((AttributeTreeRecord)child);
	}

	public Object getRoot() { return root; }

	public int getChildCount(Object parent) {
		return ((AttributeTreeRecord)parent).getChildCount();
	}

	public boolean isLeaf(Object node) {
		return ((AttributeTreeRecord)node).getChildCount() == 0;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	public void addTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners = new Vector();
		listeners.addElement(tml);
	}

	public void removeTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners.removeElement(tml);
	}

	// Methods specific to AttributeTree

	/**
	 * <p>Add all attributes of a list as sibling nodes of a tree.</p>
	 *
	 * <p>Attributes are added in the natural order of the {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * <p>Calls itself recursively to add attribute lists in items of any sequence attributes encountered.</p>
	 *
	 * @param	parent				the parent to which the new nodes are added as children
	 * @param	list				the list whose attributes to add
	 * @param	dictionary			the dictionary in which to look up the attribute names
	 * @return						the first node at this level of the tree added (if any)
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private AttributeTreeRecord processAttributeList(AttributeTreeRecord parent,AttributeList list,DicomDictionary dictionary) throws DicomException {
		AttributeTreeRecord first = null;
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTreeRecord node = new AttributeTreeRecord(parent,a,dictionary);
			if (first == null) {
				first=node;
				if (parent != null) parent.addChild(node);
			}
			else {
				node.addSibling(node);
			}
			if (ValueRepresentation.isSequenceVR(a.getVR())) {
				SequenceAttribute sa = (SequenceAttribute)a;
				int itemCount = 0;
				Iterator items = sa.iterator();
				while (items.hasNext()) {
					SequenceItem item = (SequenceItem)items.next();
					AttributeTreeRecord itemnode = new AttributeTreeRecord(node,++itemCount);
					node.addChild(itemnode);
					processAttributeList(itemnode,item.getAttributeList(),dictionary);
				}
			}
		}
		return first;
	}

	/**
	 * <p>Construct an entire tree of attributes from an attribute list.</p>
	 *
	 * @param	list		the list whose attributes to add
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public AttributeTree(AttributeList list) throws DicomException {
		if (list != null) {
			DicomDictionary dictionary = list.getDictionary();
			root = new AttributeTreeRecord(null,null,dictionary);			// we create our own (empty) root on top
			processAttributeList(root,list,dictionary);
		}
	}

	/**
	 * <p>Walk the sub-tree starting at the specified node and dump as a string.</p>
	 *
	 * @param	node	the node that roots the sub-tree
	 * @return		the attributes in the tree as a string
	 */
	private String walkTree(AttributeTreeRecord node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(node.toString());
		buffer.append("\n");

		int n = getChildCount(node);
		for (int i=0; i<n; ++i) buffer.append(walkTree((AttributeTreeRecord)getChild(node,i)));

		return buffer.toString();
	}

	/**
	 * <p>Walk the entire tree and dump as a string.</p>
	 *
	 * @return		the attributes in the tree as a string
	 */
	public String toString() {
		return walkTree(root);
	}

	/**
	 * <p>Walk the sub-tree starting at the specified node and set the sort order.</p>
	 *
	 * @param	node		the node that roots the sub-tree
	 * @param	sortByName	the sort order
	 */
	private void walkTreeAndSetSortOrder(AttributeTreeRecord node,boolean sortByName) {
		node.setSortByName(sortByName);
		Vector copy = new Vector();
		int n = node.getChildCount();
		for (int i=0; i<n; ++i) {
			AttributeTreeRecord child = (AttributeTreeRecord)node.getChildAt(i);
			walkTreeAndSetSortOrder(child,sortByName);
			copy.add(child);
		}
		node.removeAllChildren();
		Enumeration e = copy.elements();
		while (e.hasMoreElements()) {
			AttributeTreeRecord child = (AttributeTreeRecord)e.nextElement();
			node.addChild(child);
		}
	}

	/**
	 * <p>Set the sort order to be alphabetical by attribute name, or numerical by group and element tag.</p>
	 *
	 * @param	sortByName	true if sort alphabetically by attribute name
	 */
	public void setSortByName(boolean sortByName) {
		walkTreeAndSetSortOrder(root,sortByName);
		if (listeners != null) {
			for (int i=0; i<listeners.size(); ++i) {
				((TreeModelListener)(listeners.get(i))).treeStructureChanged(new TreeModelEvent(this,new TreePath(root)));
			}
		}
	}
}






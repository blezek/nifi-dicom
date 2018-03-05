/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Iterator;
import java.util.Vector;


/**
 * <p>The {@link com.pixelmed.scpecg.SCPTree SCPTree} class implements a
 * {@link javax.swing.tree.TreeModel TreeModel} to abstract the contents of a list of attributes as
 * a tree in order to provide support for a {@link com.pixelmed.scpecg.SCPTreeBrowser SCPTreeBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.tree.TreeModel javax.swing.tree.TreeModel}.</p>
 *
 * @see com.pixelmed.scpecg.SCPTreeBrowser
 * @see com.pixelmed.scpecg.SCPTreeRecord
 *
 * @author	dclunie
 */
public class SCPTree implements TreeModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/SCPTree.java,v 1.6 2017/01/24 10:50:46 dclunie Exp $";

	// Our nodes are all instances of SCPTreeRecord ...

	/***/
	private SCPTreeRecord root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	/**
	 * @param	node
	 * @param	index
	 */
	public Object getChild(Object node,int index) {
		return ((SCPTreeRecord)node).getChildAt(index);
	}

	/**
	 * @param	parent
	 * @param	child
	 */
	public int getIndexOfChild(Object parent, Object child) {
		return ((SCPTreeRecord)parent).getIndex((SCPTreeRecord)child);
	}

	/***/
	public Object getRoot() { return root; }

	/**
	 * @param	parent
	 */
	public int getChildCount(Object parent) {
		return ((SCPTreeRecord)parent).getChildCount();
	}

	/**
	 * @param	node
	 */
	public boolean isLeaf(Object node) {
		return ((SCPTreeRecord)node).getChildCount() == 0;
	}

	/**
	 * @param	path
	 * @param	newValue
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	/**
	 * @param	tml
	 */
	public void addTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners = new Vector();
		listeners.addElement(tml);
	}

	/**
	 * @param	tml
	 */
	public void removeTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners.removeElement(tml);
	}

	// Methods specific to SCPTree

	/**
	 * <p>Construct an entire tree of attributes from an SCP-ECG instance.</p>
	 *
	 * @param	scpecg		an SCP-ECG instance
	 * @throws	Exception
	 */
	public SCPTree(SCPECG scpecg) throws Exception {
		if (scpecg != null) {
			root = new SCPTreeRecord(null,"SCPECG");
			SCPTreeRecord parent = root;
			//SCPTreeRecord first = null;
			Iterator i = scpecg.getSectionIterator();
			if (i != null) {
				while (i.hasNext()) {
					Section section = (Section)(i.next());
					if (section != null) {
						section.getTree(parent);
					}
				}
			}
		}
	}

	/**
	 * <p>Walk the sub-tree starting at the specified node and dump as a string.</p>
	 *
	 * @param	node	the node that roots the sub-tree
	 * @return		the attributes in the tree as a string
	 */
	private String walkTree(SCPTreeRecord node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(node.toString());
		buffer.append("\n");

		int n = getChildCount(node);
		for (int i=0; i<n; ++i) buffer.append(walkTree((SCPTreeRecord)getChild(node,i)));

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
}






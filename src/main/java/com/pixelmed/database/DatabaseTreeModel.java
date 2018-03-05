/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.File;

import com.pixelmed.dicom.*;

/**
 * <p>The {@link DatabaseTreeModel DatabaseTreeModel} class implements a
 * {@link javax.swing.tree.TreeModel TreeModel} to abstract the contents of a database as
 * a tree in order to provide support for a {@link com.pixelmed.database.DatabaseTreeBrowser DatabaseTreeBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.tree.TreeModel javax.swing.tree.TreeModel}.</p>
 *
 * @author	dclunie
 */
public class DatabaseTreeModel implements TreeModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseTreeModel.java,v 1.17 2017/01/24 10:50:34 dclunie Exp $";

	// Our nodes are all instances of DatabaseTreeRecord ...

	/***/
	private DatabaseTreeRecord root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	/**
	 * @param	node
	 * @param	index
	 */
	public Object getChild(Object node,int index) {
		return ((DatabaseTreeRecord)node).getChildAt(index);
	}

	/**
	 * @param	parent
	 * @param	child
	 */
	public int getIndexOfChild(Object parent, Object child) {
		return ((DatabaseTreeRecord)parent).getIndex((DatabaseTreeRecord)child);
	}

	/***/
	public Object getRoot() { return root; }

	/**
	 * @param	parent
	 */
	public int getChildCount(Object parent) {
//System.err.println("DatabaseTreeModel.getChildCount(): for "+parent);
		return ((DatabaseTreeRecord)parent).getChildCount();
	}

	/**
	 * @param	node
	 */
	public boolean isLeaf(Object node) {
		return ((DatabaseTreeRecord)node).getChildCount() == 0;
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
		if (listeners == null) {
			listeners = new Vector();
		}
		listeners.addElement(tml);
	}

	/**
	 * @param	tml
	 */
	public void removeTreeModelListener(TreeModelListener tml) {
		if (listeners != null) {
			listeners.removeElement(tml);
		}
	}

	// Methods specific to DatabaseTreeModel

	/**
	 * <p>Construct a tree model of the supplied database.</p>
	 *
	 * @param	d		the database information model to build the tree from
	 * @throws	DicomException	thrown if there are problems accessing the database
	 */
	public DatabaseTreeModel(DatabaseInformationModel d) throws DicomException {
		if (d != null) {
//long startTime=System.currentTimeMillis();
			root = new DatabaseTreeRecord(d,null,d.getDatabaseRootName(),null,null,null);			// we create our own (empty) root on top
//System.err.println("DatabaseTreeModel() construct time "+(System.currentTimeMillis()-startTime)+" milliseconds");
		}
	}

	/**
	 * @param	node
	 */
	private String walkTree(DatabaseTreeRecord node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(node.toString());
		buffer.append("\n");

		int n = getChildCount(node);
		for (int i=0; i<n; ++i) {
			buffer.append(walkTree((DatabaseTreeRecord)getChild(node,i)));
		}

		return buffer.toString();
	}

	/**
	 * <p>Dump the entire tree to a string.</p>
	 *
	 * <p>Performs a top-down traversal.</p>
	 *
	 * @see DatabaseTreeRecord#toString()
	 *
	 * @return	a multiline string with one line per node in the tree
	 */
	public String toString() {
		return walkTree(root);
	}
}






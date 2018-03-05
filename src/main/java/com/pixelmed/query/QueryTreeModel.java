/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.Vector;
import java.io.File;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.query.QueryTreeModel QueryTreeModel} class implements a
 * {@link javax.swing.tree.TreeModel TreeModel} to abstract the contents of a query response as
 * a tree in order to provide support for a {@link com.pixelmed.query.QueryTreeBrowser QueryTreeBrowser}.</p>
 *
 * @see javax.swing.tree.TreeModel
 *
 * @author	dclunie
 */
public class QueryTreeModel implements TreeModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/query/QueryTreeModel.java,v 1.13 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(QueryTreeModel.class);

	// Our nodes are all instances of QueryTreeRecord ...

	/***/
	private QueryTreeRecord root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	/**
	 * @param	node
	 * @param	index
	 */
	public Object getChild(Object node,int index) {
		return ((QueryTreeRecord)node).getChildAt(index);
	}

	/**
	 * @param	parent
	 * @param	child
	 */
	public int getIndexOfChild(Object parent, Object child) {
		return ((QueryTreeRecord)parent).getIndex((QueryTreeRecord)child);
	}

	/***/
	public Object getRoot() { return root; }

	/**
	 * @param	parent
	 */
	public int getChildCount(Object parent) {
		return ((QueryTreeRecord)parent).getChildCount();
	}

	/**
	 * @param	node
	 */
	public boolean isLeaf(Object node) {
		return ((QueryTreeRecord)node).getChildCount() == 0;
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

	// Methods specific to QueryTreeModel

	/**
	 * <p>Construct a tree model with a root node on top.</p>
	 *
	 * <p>The root node is the name of the called AET in the query information model.</p>
	 *
	 * <p>The contents are added as required by actually performing queries as nodes are expanded.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #QueryTreeModel(QueryInformationModel,AttributeList)} instead.
	 * @param	q				the query information model to build the tree from
	 * @param	filter			the query request identifier as a list of DICOM attributes
	 * @param	debugLevel		unused
	 * @throws	DicomException	thrown if there are problems building the tree
	 */
	public QueryTreeModel(QueryInformationModel q,AttributeList filter,int debugLevel) throws DicomException {
		this(q,filter);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	/**
	 * <p>Construct a tree model with a root node on top.</p>
	 *
	 * <p>The root node is the name of the called AET in the query information model.</p>
	 *
	 * <p>The contents are added as required by actually performing queries as nodes are expanded.</p>
	 *
	 * @param	q				the query information model to build the tree from
	 * @param	filter			the query request identifier as a list of DICOM attributes
	 * @throws	DicomException	thrown if there are problems building the tree
	 */
	public QueryTreeModel(QueryInformationModel q,AttributeList filter) throws DicomException {
		if (q != null) {
			String aet = q.getCalledAETitle();
			root = new QueryTreeRecord(q,filter,null,(aet == null ? "Remote database" : aet),null,null,null);	// we create our own (empty) root on top
		}
	}

	/**
	 * @param	node
	 */
	private String walkTree(QueryTreeRecord node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(node.toString());
		buffer.append("\n");

		int n = getChildCount(node);
		for (int i=0; i<n; ++i) buffer.append(walkTree((QueryTreeRecord)getChild(node,i)));

		return buffer.toString();
	}

	/**
	 * <p>Dump the entire tree to a string.</p>
	 *
	 * <p>Performs a top-down traversal.</p>
	 *
	 * @see QueryTreeRecord#toString()
	 *
	 * @return	a multiline string with one line per node in the tree
	 */
	public String toString() {
		return walkTree(root);
	}
}






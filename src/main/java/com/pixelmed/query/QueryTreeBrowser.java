/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

import com.pixelmed.dicom.*;
import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

/**
 * <p>The {@link com.pixelmed.query.QueryTreeBrowser QueryTreeBrowser} class implements a Swing graphical user interface
 * to browse the contents of {@link com.pixelmed.query.QueryTreeModel QueryTreeModel}.</p>
 *
 * <p>The browser is rendered as a tree view of the returned query identifier and a one row tabular representation of the
 * contents of any level that the user selects in the tree. Constructors are provided to either add
 * the browser to a frame and creating the tree and table, or to make use of a pair of existing scrolling
 * panes.</p>
 *
 * <p>Though a functional browser can be built using this class, to add application-specific behavior
 * to be applied when a user selects from the tree, a sub-class inheriting
 * from this class should be constructed that overrides the
 * {@link #buildTreeSelectionListenerToDoSomethingWithSelectedLevel() buildTreeSelectionListenerToDoSomethingWithSelectedLevel}
 * method The default implementation is as follows:</p>
 *
 * <pre>
 * 	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedLevel() {
 * 		return new TreeSelectionListener() {
 * 			public void valueChanged(TreeSelectionEvent tse) {
 * 				TreePath tp = tse.getNewLeadSelectionPath();
 * 				if (tp != null) {
 * 					Object lastPathComponent = tp.getLastPathComponent();
 * 					if (lastPathComponent instanceof QueryTreeRecord) {
 * 						QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
 * 							System.err.println("TreeSelectionListener.valueChanged: "+r.getUniqueKeys());
 * 					}
 * 				}
 * 			}
 * 		};
 * 	}
 * </pre>
 *
 * @see com.pixelmed.query
 * @see com.pixelmed.query.QueryTreeRecord
 * @see com.pixelmed.query.QueryTreeModel
 * @see javax.swing.tree.TreePath
 * @see javax.swing.event.TreeSelectionListener
 *
 * @author	dclunie
 */
public class QueryTreeBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/query/QueryTreeBrowser.java,v 1.18 2017/01/24 10:50:46 dclunie Exp $";

	private JTree tree;
	private QueryTreeModel treeModel;
	private QueryInformationModel queryInformationModel;

	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	q				the query information model
	 * @param	m				the query tree model (i.e. the results returned from an actual query)
	 * @param	treeBrowserScrollPane		the scrolling pane in which the tree view of the query results will be rendered
	 * @param	attributeBrowserScrollPane	the scrolling pane in which the tabular view of the currently selected level will be rendered
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public QueryTreeBrowser(QueryInformationModel q,QueryTreeModel m,JScrollPane treeBrowserScrollPane,JScrollPane attributeBrowserScrollPane) throws DicomException {
		queryInformationModel=q;
		treeModel=m;
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedLevel());
	}

	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	q				the query information model
	 * @param	m				the query tree model (i.e. the results returned from an actual query)
	 * @param	content			content pane will to add scrolling panes containing tree and tabular selection views
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public QueryTreeBrowser(QueryInformationModel q,QueryTreeModel m,Container content) throws DicomException {
		queryInformationModel=q;
		treeModel=m;
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		JScrollPane treeBrowserScrollPane = new JScrollPane(tree);
		JScrollPane attributeBrowserScrollPane = new JScrollPane();
		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeBrowserScrollPane,attributeBrowserScrollPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.7);
		//splitPane.setDividerLocation(1.0);	// setDividerLocation(1.0) to collapse bottom (attribute) pane doesn't work until split pane is actually shown ... 
		// based on jaydsa's suggestion at "http://java.itags.org/java-swing/43801/"  but use ComponentListener instead ofHierarchyListener() ...
		splitPane.addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {
//System.err.println("DoseUtility.OurSourceDatabaseTreeBrowser.componentResized(): event = "+e);
				splitPane.setDividerLocation(1.0);
			}
			public void componentShown(ComponentEvent e) {}
		});
		content.add(splitPane);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedLevel());
	}

	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	q				the query information model
	 * @param	m				the query tree model (i.e. the results returned from an actual query)
	 * @param	frame				a frame to whose content pane will be added scrolling panes containing tree and tabular selection views
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public QueryTreeBrowser(QueryInformationModel q,QueryTreeModel m,JFrame frame) throws DicomException {
		this(q,m,frame.getContentPane());
	}
	
	/**
	 * <p>Return the records currently selected.</p>
	 *
	 * @return	the records currently selected
	 */
	public QueryTreeRecord[] getSelectionPaths() {
		ArrayList<QueryTreeRecord> records = new ArrayList<QueryTreeRecord>();
		TreeSelectionModel selectionModel = tree.getSelectionModel();
		if (selectionModel != null) {
			TreePath[] paths = selectionModel.getSelectionPaths();
			if (paths != null && paths.length > 0) {
				for (TreePath path : paths) {
					if (path != null) {
						Object lastPathComponent = path.getLastPathComponent();
						if (lastPathComponent instanceof QueryTreeRecord) {
							QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
//System.err.println("QueryTreeBrowser.getSelectionPaths(): "+r.getUniqueKeys());
							records.add(r);
						}
					}
				}
			}
		}
		QueryTreeRecord[] returnValues = new QueryTreeRecord[records.size()];
		return records.toArray(returnValues);
	}

	/**
	 * <p>Override this method to perform application-specific behavior when an entity is selected in the tree browser.</p>
	 *
	 * <p>By default this method dumps the string values of the unique keys to the console for level selection,
	 * which is pretty useless.</p>
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedLevel() {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof QueryTreeRecord) {
						QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
//System.err.println("TreeSelectionListener.valueChanged: "+r.getUniqueKeys());
					}
				}
			}
		};
	}

	/**
	 * <p>By default this method populates the tabular attribute browser when an entity is selected in the tree browser.</p>
	 *
	 * <p>Override this method to perform application-specific behavior, perhaps if not all attributes
	 * in the query identifer for the selected level are to be displayed, or their values are to be rendered
	 * specially. The default implementation renders everything as strings.</p>
	 *
	 * @param	attributeBrowserScrollPane	the tabular attribute browser
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(final JScrollPane attributeBrowserScrollPane) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
//System.err.println("QueryTreeBrowser.TreeSelectionListener.valueChanged(): "+tp);
				if (tp != null) {
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof QueryTreeRecord) {
//System.err.println("QueryTreeBrowser.TreeSelectionListener.valueChanged(): lastPathComponent="+lastPathComponent);
						QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
						AttributeList identifier = r.getAllAttributesReturnedInIdentifier();
//System.err.println("QueryTreeBrowser.TreeSelectionListener.valueChanged(): identifier="+identifier);
						if (identifier != null) {
							HashSet includeList = null;
							HashSet excludeList = null;
							AttributeListTableBrowser table = new AttributeListTableBrowser(identifier,includeList,excludeList);
							table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		// Otherwise horizontal scroll doesn't work
							table.setColumnWidths();
							attributeBrowserScrollPane.setViewportView(table);
						}
					}
				}
			}
		};
	}
}

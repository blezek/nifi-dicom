/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

import com.pixelmed.dicom.*;
import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link DatabaseTreeBrowser DatabaseTreeBrowser} class implements a Swing graphical user interface
 * to browse the contents of {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel}.</p>
 *
 * <p>The browser is rendered as a tree view of the entire database and a one row tabular representation of the
 * contents of any entity (record) that the user selects in the tree. Constructors are provided to either add
 * the browser to a frame and creating the tree and table, or to make use of a pair of existing scrolling
 * panes.</p>
 *
 * <p>Only selection of single nodes is permitted by default.</p>
 *
 * <p>Though a functional browser can be built using this class, to add application-specific behavior
 * to be applied when a user selects one or more instance entities in the tree, a sub-class inheriting
 * from this class should be constructed that overrides the
 * {@link #buildTreeSelectionListenerToDoSomethingWithSelectedFiles() buildTreeSelectionListenerToDoSomethingWithSelectedFiles}
 * method. The default implementation, which handles single and multiple selections is as follows:</p>
 *
 * <pre>
 * 	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedFiles() {
 * 		return new TreeSelectionListener() {
 * 			public void valueChanged(TreeSelectionEvent tse) {
 *				DatabaseTreeRecord[] records = getSelectionPaths();
 *				if (!doSomethingWithSelections(records)) {
 *					Vector names = new Vector();
 *					for (DatabaseTreeRecord r : records) {
 *						recurseThroughChildrenGatheringFileNames(r,names);
 *					}
 *				doSomethingWithSelectedFiles(names);
 * 			}
 * 		};
 * 	}
 * </pre>
 *
 * <p>Note that this allows you to take the simple approach of overriding the protected method
 * {@link #doSomethingWithSelectedFiles(Vector) doSomethingWithSelectedFiles}, the default
 * implementation of which is just to print the file name.</p>
 *
 * @see com.pixelmed.database
 * @see com.pixelmed.database.DatabaseTreeRecord
 * @see com.pixelmed.database.DatabaseInformationModel
 * @see javax.swing.tree.TreePath
 * @see javax.swing.event.TreeSelectionListener
 *
 * @author	dclunie
 */
public class DatabaseTreeBrowser {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseTreeBrowser.java,v 1.26 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DatabaseTreeBrowser.class);

	private JTree tree;
	private DatabaseTreeModel treeModel;
	private DatabaseInformationModel databaseInformationModel;
	private Map descriptiveNameMap;
	
	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	d				the instance of the database (information model)
	 * @param	treeBrowserScrollPane		the scrolling pane in which the tree view of the entire model (database) will be rendered
	 * @param	attributeBrowserScrollPane	the scrolling pane in which the tabular view of the currently selected entity (record) will be rendered
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public DatabaseTreeBrowser(DatabaseInformationModel d,JScrollPane treeBrowserScrollPane,JScrollPane attributeBrowserScrollPane) throws DicomException {
		databaseInformationModel=d;
		descriptiveNameMap=(d == null) ? null : d.getDescriptiveNameMap();
		treeModel=new DatabaseTreeModel(d);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		treeBrowserScrollPane.setViewportView(tree);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles());
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
	}

	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	d				the instance of the database (information model)
	 * @param	frame				a frame to whose content pane will be added scrolling panes containing tree and tabular selection views
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public DatabaseTreeBrowser(DatabaseInformationModel d,JFrame frame) throws DicomException {
		this(d,frame.getContentPane());
	}

	/**
	 * <p>Build and display a graphical user interface view of a database information model.</p>
	 *
	 * @param	d				the instance of the database (information model)
	 * @param	content			a container to which will be added scrolling panes containing tree and tabular selection views
	 * @throws	DicomException			thrown if the information cannot be extracted
	 */
	public DatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
		databaseInformationModel=d;
		descriptiveNameMap=(d == null) ? null : d.getDescriptiveNameMap();
		treeModel=new DatabaseTreeModel(d);
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
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles());
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
	}

	/**
	 * <p>Return the records currently selected.</p>
	 *
	 * @return	the records currently selected
	 */
	public DatabaseTreeRecord[] getSelectionPaths() {
		ArrayList<DatabaseTreeRecord> records = new ArrayList<DatabaseTreeRecord>();
		TreeSelectionModel selectionModel = tree.getSelectionModel();
		if (selectionModel != null) {
			TreePath[] paths = selectionModel.getSelectionPaths();
			if (paths != null && paths.length > 0) {
				for (TreePath path : paths) {
					if (path != null) {
						Object lastPathComponent = path.getLastPathComponent();
						if (lastPathComponent instanceof DatabaseTreeRecord) {
							DatabaseTreeRecord r = (DatabaseTreeRecord)lastPathComponent;
							//System.err.println("QueryTreeBrowser.getSelectionPaths(): "+r.getUniqueKeys());
							records.add(r);
						}
					}
				}
			}
		}
		DatabaseTreeRecord[] returnValues = new DatabaseTreeRecord[records.size()];
		return records.toArray(returnValues);
	}

	/**
	 * <p>Override this method to perform application-specific behavior when an entity is selected in the tree browser.</p>
	 *
	 * <p>By default, this method calls {@link #doSomethingWithSelections(DatabaseTreeRecord[]) doSomethingWithSelections}, then if that returns false,
	 * for each of the selected records (and there may be more than one), adds to a <code>Vector</code> of file names (paths) of that record and the subtrees below it, then calls
	 * {@link #doSomethingWithSelectedFiles(Vector) doSomethingWithSelectedFiles}.</p>
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedFiles() {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				DatabaseTreeRecord[] records = getSelectionPaths();
				if (!doSomethingWithSelections(records)) {
					Vector names = new Vector();
					for (DatabaseTreeRecord r : records) {
						recurseThroughChildrenGatheringFileNames(r,names);
					}
					doSomethingWithSelectedFiles(names);
				}
			}
		};
	}

	/**
	 * <p>Recursively process the specified DatabaseTreeRecord and all its children finding file paths at the instance level.</p>
	 *
	 * <p>A static helper method, that is public so that it can be used in subclasses that override
	 * {@link #buildTreeSelectionListenerToDoSomethingWithSelectedFiles() buildTreeSelectionListenerToDoSomethingWithSelectedFiles}.</p>
	 *
	 * @param	r		the current DatabaseTreeRecord to process
	 * @param	names	the file names (paths) to add to
	 */
	public static void recurseThroughChildrenGatheringFileNames(DatabaseTreeRecord r,Vector names) {
		InformationEntity ie = r.getInformationEntity();
		if (ie == InformationEntity.INSTANCE) {
			String fileName = r.getLocalFileNameValue();
			if (fileName != null) {
				names.add(fileName);
			}

		}
		Enumeration children = r.children();
		if (children != null) {
			while (children.hasMoreElements()) {
				DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
				if (child != null) {
					recurseThroughChildrenGatheringFileNames(child,names);
				}
			}
		}
	}

	/**
	 * <p>By default this method populates the tabular attribute browser when an entity is selected in the tree browser.</p>
	 *
	 * <p>Override this method to perform application-specific behavior, perhaps if not all attributes
	 * in the database for the selected entity are to be displayed, or their values are to be rendered
	 * specially. The default implementation renders everything as strings except those local database
	 * administrative attributes normally excluded.</p>
	 *
	 * @param	attributeBrowserScrollPane	the tabular attribute browser
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(final JScrollPane attributeBrowserScrollPane) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof DatabaseTreeRecord) {
						DatabaseTreeRecord r = (DatabaseTreeRecord)lastPathComponent;
						InformationEntity ie = r.getInformationEntity();
						String localPrimaryKeyValue = r.getLocalPrimaryKeyValue();
						if (ie != null && localPrimaryKeyValue != null) {
							Map map = null;
							try {
								map = databaseInformationModel.findAllAttributeValuesForSelectedRecord(ie,localPrimaryKeyValue);
								HashSet includeList = null;
								//HashSet excludeList = null;
								HashSet excludeList = databaseInformationModel.getLocalColumnExcludeList();
								MapTableBrowser table = new MapTableBrowser(map,descriptiveNameMap,includeList,excludeList);
								table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		// Otherwise horizontal scroll doesn't work
								table.setColumnWidths();
								attributeBrowserScrollPane.setViewportView(table);
							} catch (Exception e) {
								slf4jlogger.error("",e);
							}
						}
					}
				}
			}
		};
	}


	/**
	 */
	protected MouseListener buildMouseListenerToDetectDoubleClickEvents() {
		return new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				if (me != null) {
					if (me.getClickCount() == 2) {
//System.err.println("DatabaseTreeBrowser.MouseAdapter.mousePressed(): Detected double-click");
						doSomethingMoreWithWhateverWasSelected();
					}
				}
			}
		};
	}
	
	// Override these next methods in derived classes to do something useful

	/**
	 * <p>Will be called when a selection is made or changed.</p>
	 *
	 * <p>Note that during multi-selection, or deselection, this may be called multiple times,
	 * so use this method to update cache of what is selected rather than initiating action on
	 * the selections, which should be done by some other means.</p>
	 *
	 * @param	selections
	 * return				true if did something and hence should do no more
	 */
	protected boolean doSomethingWithSelections(DatabaseTreeRecord[] selections) {
		if (selections != null) {
			for (DatabaseTreeRecord selection : selections) {
				if (selection != null) {
					System.err.println("DatabaseTreeBrowser.doSomethingWithSelection(): "+selection);
				}
			}
		}
		return false;
	}

	/**
	 * <p>Will be called when a selection is made or changed and {@link #doSomethingWithSelections(DatabaseTreeRecord[]) doSomethingWithSelections()} returns false and not otherwise.</p>
	 *
	 * <p>Note that during multi-selection, or deselection, this may be called multiple times,
	 * so use this method to update cache of what is selected rather than initiating action on
	 * the selections, which should be done by some other means.</p>
	 *
	 * @param	paths
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		if (paths != null) {
			Iterator i = paths.iterator();
			while (i.hasNext()) {
				System.err.println("DatabaseTreeBrowser.doSomethingWithSelectedFiles(): "+(String)i.next());
			}
		}
	}

	/**
	 * <p>Will be called when a double-click event is detected unless {@link #buildMouseListenerToDetectDoubleClickEvents() buildMouseListenerToDetectDoubleClickEvents} is overriden to do something else.</p>
	 */
	protected void doSomethingMoreWithWhateverWasSelected() {
		System.err.println("DatabaseTreeBrowser.doSomethingMoreWithWhateverWasSelected(): Double click on current selection");
	}

}







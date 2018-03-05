/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

/**
 * @author	dclunie
 */
public class DicomDirectoryBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryBrowser.java,v 1.25 2017/01/24 10:50:36 dclunie Exp $";

	private JTree tree;
	private DicomDirectory treeModel;
	private String parentFilePath;

	private HashSet<AttributeTag> defaultExcludeList;
	private HashSet<AttributeTag> patientExcludeList;
	private HashSet<AttributeTag> studyExcludeList;
	private HashSet<AttributeTag> seriesExcludeList;
	private HashSet<AttributeTag> imageExcludeList;
	private HashSet<AttributeTag> srExcludeList;

	/**
	 * @param	list			a list of attributes describing a DICOMDIR instance
	 * @param	parentFilePath	the path to which all ReferencedFileIDs in the DICOMDIR are relative (i.e., the folder in which the DICONDIR is/will be stored)
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath) throws DicomException {
//long startTime = System.currentTimeMillis();
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}

	/**
	 * @param	list						a list of attributes describing a DICOMDIR instance
	 * @param	parentFilePath				the path to which all ReferencedFileIDs in the DICOMDIR are relative (i.e., the folder in which the DICONDIR is/will be stored)
	 * @param	treeBrowserScrollPane		where to put the tree browser for the directory
	 * @param	attributeBrowserScrollPane	where to put the attribute browser for a selected record
	 * @throws	DicomException				if error in DICOM encoding
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath,JScrollPane treeBrowserScrollPane,JScrollPane attributeBrowserScrollPane) throws DicomException {
//long startTime = System.currentTimeMillis();
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		createExcludeLists();
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}

	/**
	 * @param	list			a list of attributes describing a DICOMDIR instance
	 * @param	parentFilePath	the path to which all ReferencedFileIDs in the DICOMDIR are relative (i.e., the folder in which the DICONDIR is/will be stored)
	 * @param	frame			where to put the browsers
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DicomDirectoryBrowser(AttributeList list,String parentFilePath,JFrame frame) throws DicomException {
//long startTime = System.currentTimeMillis();
		Attribute a = list.get(TagFromName.FileSetID);
		if (a != null) {
			String descriptors[] = a.getStringValues();
			if (descriptors != null && descriptors.length > 0) frame.setTitle(descriptors[0]);
		}
		this.parentFilePath=parentFilePath;
		treeModel=new DicomDirectory(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		
		Container content = frame.getContentPane();
		JScrollPane treeScrollPane = new JScrollPane(tree);
		JScrollPane attributeBrowserScrollPane = new JScrollPane();
		//JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScrollPane,attributeBrowserScrollPane);
		//splitPane.setResizeWeight(0.7);
		//content.add(splitPane);
		attributeBrowserScrollPane.setPreferredSize(new Dimension(0,76));	// width is irrelevant
		JPanel panel=new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(treeScrollPane,BorderLayout.CENTER);
		panel.add(attributeBrowserScrollPane,BorderLayout.SOUTH);
		content.add(panel);

		tree.addTreeSelectionListener(buildTreeSelectionListenerToDoSomethingWithSelectedFiles(parentFilePath));
		createExcludeLists();
		tree.addTreeSelectionListener(buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(attributeBrowserScrollPane));
		tree.addMouseListener(buildMouseListenerToDetectDoubleClickEvents());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectoryBrowser(): took = "+(currentTime-startTime)+" ms");
	}
	
	//protected void setCellRenderer(TreeCellRenderer cellRenderer) {
	//	tree.setCellRenderer(cellRenderer);
	//}
	
	/**
	 * @param	font	font to use
	 */
	protected void setFont(Font font) {
		tree.setFont(font);
	}

	/**
	 * @param	parentFilePath	parent directory at which DICOMDIR file paths are rooted
	 * @return					a TreeSelectionListener
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedFiles(final String parentFilePath) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
//System.err.println("DicomDirectoryBrowser.TreeSelectionListener.valueChanged(): Selected: "+tp.getLastPathComponent());
//System.err.println("DicomDirectoryBrowser.TreeSelectionListener.valueChanged(): Selected: "+tp);
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof DicomDirectoryRecord) {
						Vector names=DicomDirectory.findAllContainedReferencedFileNames((DicomDirectoryRecord)lastPathComponent,parentFilePath);
						if (names != null) doSomethingWithSelectedFiles(names);
					}
				}
			}
		};
	}
	
	/**
	 * @param	attributeBrowserScrollPane	where to display the attributes of the selected record
	 * @return								a TreeSelectionListener
	 */
	protected TreeSelectionListener buildTreeSelectionListenerToDisplayAttributesOfSelectedRecord(final JScrollPane attributeBrowserScrollPane) {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
//System.err.println("Selected: "+tp.getLastPathComponent());
//System.err.println("Selected: "+tp);
					Object lastPathComponent = tp.getLastPathComponent();
					if (lastPathComponent instanceof DicomDirectoryRecord) {
						DicomDirectoryRecord dirRecord = (DicomDirectoryRecord)lastPathComponent;
						HashSet<AttributeTag> includeList = null;
						HashSet<AttributeTag> excludeList = chooseExcludeList(dirRecord);
						AttributeListTableBrowser table = new AttributeListTableBrowser(dirRecord.getAttributeList(),includeList,excludeList);
						table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		// Otherwise horizontal scroll doesn't work
						table.setColumnWidths();
						attributeBrowserScrollPane.setViewportView(table);
					}
				}
			}
		};
	}
	

	/**
	 * @return	a MouseListener to detect double click events
	 */
	protected MouseListener buildMouseListenerToDetectDoubleClickEvents() {
		return new MouseAdapter() {
			public void mousePressed(MouseEvent me) {
				if (me != null) {
					if (me.getClickCount() == 2) {
//System.err.println("DicomDirectoryBrowser.MouseAdapter.mousePressed(): Detected double-click");
						doSomethingMoreWithWhateverWasSelected();
					}
				}
			}
		};
	}


	/**
	 * @param	dirRecord	the directory record for which we need to select an exclude list based on its DirectoryRecordType
	 * @return				the appropriate list of attributes to exclude from display based on DirectoryRecordType, or the defaultExcludeList if DirectoryRecordType unrecognized
	 */
	protected HashSet<AttributeTag> chooseExcludeList(DicomDirectoryRecord dirRecord) {
		HashSet<AttributeTag> excludeList=null;
		AttributeList list = dirRecord.getAttributeList();
		String directoryRecordType = Attribute.getSingleStringValueOrNull(list,TagFromName.DirectoryRecordType);
		if (directoryRecordType == null) {
			excludeList = defaultExcludeList;
		}
		else if (directoryRecordType.equals("PATIENT")) {
			excludeList = patientExcludeList;
		}
		else if (directoryRecordType.equals("STUDY")) {
			excludeList = studyExcludeList;
		}
		else if (directoryRecordType.equals("SERIES")) {
			excludeList = seriesExcludeList;
		}
		else if (directoryRecordType.equals("IMAGE")) {
			excludeList = imageExcludeList;
		}
		else if (directoryRecordType.equals("SR DOCUMENT")) {
			excludeList = srExcludeList;
		}
		return excludeList;
	}
	
	/***/
	protected void createExcludeLists() {
		defaultExcludeList = new HashSet<AttributeTag>();
		defaultExcludeList.add(TagFromName.OffsetOfTheNextDirectoryRecord);
		defaultExcludeList.add(TagFromName.RecordInUseFlag);
		defaultExcludeList.add(TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity);
		defaultExcludeList.add(TagFromName.DirectoryRecordType);
		
		patientExcludeList =  new HashSet<AttributeTag>(defaultExcludeList);
		
		studyExcludeList =  new HashSet<AttributeTag>(defaultExcludeList);
		
		seriesExcludeList =  new HashSet<AttributeTag>(defaultExcludeList);
		
		imageExcludeList =  new HashSet<AttributeTag>(defaultExcludeList);
		//imageExcludeList.add(TagFromName.ReferencedFileID);
		//imageExcludeList.add(TagFromName.ReferencedSOPClassUIDInFile);
		//imageExcludeList.add(TagFromName.ReferencedSOPInstanceUIDInFile);
		//imageExcludeList.add(TagFromName.ReferencedTransferSyntaxUIDInFile);
		
		srExcludeList =  new HashSet<AttributeTag>(defaultExcludeList);
	}

	/**
	 *	<p>Get this directory, initializing any structures necessary.</p>
	 *
	 * @return		this directory
	 */
	public DicomDirectory getDicomDirectory() {
		treeModel.getMapOfSOPInstanceUIDToReferencedFileName(this.parentFilePath);	// initializes map using parentFilePath, ignore return value
		return treeModel;
	}

	/**
	 * @return		the parent file path
	 */
	public String getParentFilePath() { return parentFilePath; }

	// Override these next methods in derived classes to do something useful

	/**
	 * @param	paths	the file paths selected
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		if (paths != null) {
			Iterator i = paths.iterator();
			while (i.hasNext()) {
				System.err.println((String)i.next());
			}
		}
	}

	/**
	 */
	protected void doSomethingMoreWithWhateverWasSelected() {
		System.err.println("DicomDirectoryBrowser.doSomethingMoreWithWhateverWasSelected(): Double click on current selection");
	}

	/**
	 * @param	arg	a DICOMDIR file
	 */
	public static void main(String arg[]) {
		AttributeList list = new AttributeList();
		try {
			final String suppliedFileName=arg[0];
			final String parentFilePath = new File(suppliedFileName).getParent();

			System.err.println("test reading DICOMDIR");
//long startTime = System.currentTimeMillis();
			list.read(suppliedFileName);
//long currentTime = System.currentTimeMillis();
//System.err.println("read took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
			final JFrame frame = new JFrame();
			frame.setSize(400,800);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					frame.dispose();
					System.exit(0);
				}
			});

			System.err.println("building tree");
			DicomDirectoryBrowser tree = new DicomDirectoryBrowser(list,parentFilePath,frame);
//currentTime = System.currentTimeMillis();
//System.err.println("tree building took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
			System.err.println("display tree");
			frame.setVisible(true);
//currentTime = System.currentTimeMillis();
//System.err.println("tree display took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}







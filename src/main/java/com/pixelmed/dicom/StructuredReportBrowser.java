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
 * <p>The {@link com.pixelmed.dicom.StructuredReportBrowser StructuredReportBrowser} class implements a Swing graphical user interface
 * to browse the contents of a {@link com.pixelmed.dicom.StructuredReport StructuredReport}.</p>
 *
 * @deprecated use  {@link com.pixelmed.dicom.StructuredReportTreeBrowser StructuredReportTreeBrowser} instead
 *
 * @see com.pixelmed.dicom.StructuredReportTreeBrowser
 * @see com.pixelmed.dicom.AttributeTreeBrowser
 *
 * @author	dclunie
 */
public class StructuredReportBrowser extends JFrame {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StructuredReportBrowser.java,v 1.15 2017/01/24 10:50:39 dclunie Exp $";

	private JTree tree;
	private StructuredReport treeModel;

	/**
	 * <p>Build and display a graphical user interface view of a tree representing a structured reports.</p>
	 *
	 * <p>Implicitly builds a tree from the attribute list.</p>
	 *
	 * @param	list				the list of attributes in which the structured report is encoded
	 * @throws	DicomException
	 */
	public StructuredReportBrowser(AttributeList list) throws DicomException {
		this(list,"SR Tree");
	}

	/**
	 * <p>Build and display a graphical user interface view of a tree representing a structured reports.</p>
	 *
	 * <p>Implicitly builds a tree from the attribute list.</p>
	 *
	 * @param	list				the list of attributes in which the structured report is encoded
	 * @param	title
	 * @throws	DicomException
	 */
	public StructuredReportBrowser(AttributeList list,String title) throws DicomException {
		super(title);

		setSize(400,800);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				//System.exit(0);
			}
		});

		treeModel=new StructuredReport(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);

		tree.addTreeSelectionListener(buildTreeSelectionListener());

		JScrollPane scrollPane = new JScrollPane(tree);
		getContentPane().add(scrollPane,BorderLayout.CENTER);
	}

	private TreeSelectionListener buildTreeSelectionListener() {
		return new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				TreePath tp = tse.getNewLeadSelectionPath();
				if (tp != null) {
//System.err.println("Selected: "+tp.getLastPathComponent());
//System.err.println("Selected: "+tp);
					Object rootComponent = tp.getPathComponent(0);				// root is need to find by reference rather than by value nodes
					Object lastPathComponent = tp.getLastPathComponent();
					if (rootComponent instanceof ContentItem && lastPathComponent instanceof ContentItem) {
						Vector instances=StructuredReport.findAllContainedSOPInstances((ContentItem)rootComponent,(ContentItem)lastPathComponent);
						if (instances != null) doSomethingWithSelectedSOPInstances(instances);
					}
				}
			}
		};
	}

	// 

	/**
	 * <p>Do something when the user selects a node of the tree.</p>
	 *
	 * <p>Override this method in derived classes to do something useful.</p>
	 *
	 * @param	instances
	 */
	protected void doSomethingWithSelectedSOPInstances(Vector instances) {
		Iterator i = instances.iterator();
		while (i.hasNext()) {
			System.err.println((SpatialCoordinateAndImageReference)i.next());
		}
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Display the SR encoded in the file name on the command line as a tree.</p>
	 *
	 * @param	arg
	 */
	public static void main(String arg[]) {

		AttributeList list = new AttributeList();

		try {
			final String suppliedFileName=arg[0];

			System.err.println("test reading SR Document");
			list.read(suppliedFileName);

			System.err.println("building tree");
			StructuredReportBrowser tree = new StructuredReportBrowser(list);

			System.err.println("display tree");
			tree.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}






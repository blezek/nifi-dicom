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

import com.pixelmed.display.ApplicationFrame;	// for main() test

/**
 * <p>The {@link com.pixelmed.dicom.StructuredReportTreeBrowser StructuredReportTreeBrowser} class implements a Swing graphical user interface
 * to browse the contents of a {@link com.pixelmed.dicom.StructuredReport StructuredReport}.</p>
 *
 * <p>A main() method is provided for testing and as a utility that reads a DICOM SR file and displays it as a tree of content items.</p>
 *
 * @see com.pixelmed.dicom.StructuredReportBrowser
 * @see com.pixelmed.dicom.AttributeTreeBrowser
 *
 * @author	dclunie
 */
public class StructuredReportTreeBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StructuredReportTreeBrowser.java,v 1.6 2017/01/24 10:50:39 dclunie Exp $";

	private JTree tree;
	private StructuredReport treeModel;

	/**
	 * <p>Build and display a graphical user interface view of a tree of attributes.</p>
	 *
	 * <p>Implicitly builds a tree from the SR attribute list.</p>
	 *
	 * @param	list				the list whose attributes to browse
	 * @param	treeBrowserScrollPane		the scrolling pane in which the tree view of the attributes will be rendered
	 * @throws	DicomException
	 */
	public StructuredReportTreeBrowser(AttributeList list,JScrollPane treeBrowserScrollPane) throws DicomException {
		treeModel=new StructuredReport(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Displays an SR tree browser built from the attributes in the file named on the command line.</p>
	 *
	 * @param	arg
	 */
	public static void main(String arg[]) {
		AttributeList list = new AttributeList();
		try {
			list.read(arg[0]);
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
		
		ApplicationFrame af = new ApplicationFrame();
		JScrollPane scrollPane = new JScrollPane();
		try {
			StructuredReportTreeBrowser browser = new StructuredReportTreeBrowser(list,scrollPane);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
		af.getContentPane().add(scrollPane);
		af.pack();
		af.setVisible(true);
	}
}







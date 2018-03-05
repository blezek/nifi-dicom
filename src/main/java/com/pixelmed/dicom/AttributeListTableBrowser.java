/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.io.*;

import com.pixelmed.display.*;	// for ApplicationFrame for main() test

/**
 * <p>The {@link com.pixelmed.dicom.AttributeListTableBrowser AttributeListTableBrowser} class implements a Swing graphical user interface
 * to browse the contents of an {@link com.pixelmed.dicom.AttributeListTableModel AttributeListTableModel}.</p>
 *
 * @author	dclunie
 */
public class AttributeListTableBrowser extends JTable {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeListTableBrowser.java,v 1.16 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Build and display a graphical user interface view of a table model.</p>
	 *
	 * @param	model	the instance of the table model
	 */
	public AttributeListTableBrowser(AttributeListTableModel model) {
		super();
		setModel(model);
		setColumnWidths();
	}
	
	/**
	 * <p>Build and display a graphical user interface view of a table of attributes.</p>
	 *
	 * <p>Implicitly builds a model from the attribute list.</p>
	 *
	 * @param	list	an attribute list
	 */
	public AttributeListTableBrowser(AttributeList list) {
		super();
		setModel(new AttributeListTableModel(list));
		setColumnWidths();
	}
	
	/**
	 * <p>Build and display a graphical user interface view of a table of attributes.</p>
	 *
	 * <p>Implicitly builds a model from the attribute list.</p>
	 *
	 * @param	list	an attribute list
	 * @param	includeList	the list of attributes to include
	 * @param	excludeList	the list of attributes to exclude
	 */
	public AttributeListTableBrowser(AttributeList list,HashSet<AttributeTag> includeList,HashSet<AttributeTag> excludeList) {
		super();
		setModel(new AttributeListTableModel(list,includeList,excludeList));
		setColumnWidths();
	}
	
	/**
	 * <p>Called after setting the model to make sure that the cells (columns)
	 * are rendered with an appropriate width, with fudge factors to handle
	 * different platforms.</p>
	 */
	public void setColumnWidths() {			// See "http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#custom"
							// and "http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/TableRenderDemo.java"
		int n = getModel().getColumnCount();
		for (int j=0; j<n; ++j) {
			TableColumn column = getColumnModel().getColumn(j);
			TableCellRenderer headerRenderer = column.getHeaderRenderer();
			if (headerRenderer == null) headerRenderer = getTableHeader().getDefaultRenderer();	// the new 1.3 way
			Component columnComponent = headerRenderer.getTableCellRendererComponent(this,column.getHeaderValue(),false,false,-1,j);
			Component   cellComponent = getDefaultRenderer(getColumnClass(j)).getTableCellRendererComponent(this,getModel().getValueAt(0,j),false,false,0,j);
			int wantWidth = Math.max(
				columnComponent.getPreferredSize().width+10,	// fudge factor ... seems to always be too small otherwise (on x86 Linux)
				cellComponent.getPreferredSize().width+10	// fudge factor ... seems to always be too small otherwise (on Mac OS X)
			);
			column.setPreferredWidth(wantWidth);
		}
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Displays a table built from the attributes in the file named on the command line.</p>
	 *
	 * @param	arg	DICOM filename
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
		
		//JTable table = new AttributeListTableBrowser(list);
		//JTable table = new AttributeListTableBrowser(new AttributeListTableModel(list));
		JTable table;
		{
			HashSet<AttributeTag> theList = new HashSet<AttributeTag>();
			theList.add(TagFromName.FileMetaInformationGroupLength);
			theList.add(TagFromName.ImplementationVersionName);
			theList.add(TagFromName.SourceApplicationEntityTitle);
			//table = new AttributeListTableBrowser(new AttributeListTableModel(list,theList,null));	// include
			table = new AttributeListTableBrowser(new AttributeListTableModel(list,null,theList));		// exclude
			//table = new AttributeListTableBrowser(new AttributeListTableModel(null,null,theList));	// exclude, null list
		}
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		// Otherwise horizontal scroll doesn't work
		JScrollPane scrollPane = new JScrollPane(table);
		//table.setPreferredScrollableViewportSize(new Dimension(400, 100));
		//af.getContentPane().add(scrollPane,BorderLayout.CENTER);	// why BorderLayout.CENTER ?
		af.getContentPane().add(scrollPane);
		af.pack();
		af.setVisible(true);
	}
}




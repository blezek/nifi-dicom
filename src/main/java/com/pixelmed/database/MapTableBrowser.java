/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.io.*;

/**
 * <p>The {@link com.pixelmed.database.MapTableBrowser MapTableBrowser} class extends a
 * {@link javax.swing.JTable JTable} to browse a list of attributes and their values
 * as a single row table with columns headed by the attribute descriptions (if
 * supplied) or their names.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.JTable javax.swing.JTable}.</p>
 *
 * @author	dclunie
 */
public class MapTableBrowser extends JTable {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/MapTableBrowser.java,v 1.7 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Build and display a graphical user interface view of an existing table model.</p>
	 *
	 * @param	model
	 */
	public MapTableBrowser(MapTableModel model) {
		super();
		setModel(model);
		setColumnWidths();
	}
	
	/**
	 * <p>Build and display a graphical user interface view of a new table model constructed from the supplied attributes and values.</p>
	 *
	 * @param	map			a map of string names for attributes to their string values
	 * @param	descriptiveNameMap	a map of string names for attributes to descriptions for use as column titles (may be null)
	 */
	public MapTableBrowser(Map map,Map descriptiveNameMap) {
		super();
		setModel(new MapTableModel(map,descriptiveNameMap));
		setColumnWidths();
	}
	
	/**
	 * <p>Build and display a graphical user interface view of a new table model constructed from the supplied attributes and values.</p>
	 *
	 * @param	map			a map of string names for attributes to their string values
	 * @param	descriptiveNameMap	a map of string names for attributes to descriptions for use as column titles (may be null)
	 * @param	includeList		a set of upper case string names for suitable attributes (may be null)
	 * @param	excludeList		a set of upper case string names for unsuitable attributes (may be null)
	 */
	public MapTableBrowser(Map map,Map descriptiveNameMap,HashSet includeList,HashSet excludeList) {
		super();
		setModel(new MapTableModel(map,descriptiveNameMap,includeList,excludeList));
		setColumnWidths();
	}
	
	/***/
	public void setColumnWidths() {			// See "http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#custom"
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
}




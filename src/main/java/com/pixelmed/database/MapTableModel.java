/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;

/**
 * <p>The {@link com.pixelmed.database.MapTableModel MapTableModel} class extends a
 * {@link javax.swing.table.AbstractTableModel AbstractTableModel} to abstract the contents of a database as
 * a tree in order to provide support for a {@link com.pixelmed.database.MapTableBrowser MapTableBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.table.AbstractTableModel javax.swing.table.AbstractTableModel}.</p>
 *
 * @author	dclunie
 */
public class MapTableModel extends AbstractTableModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/MapTableModel.java,v 1.11 2017/01/24 10:50:35 dclunie Exp $";
	
	/***/
	protected int columnCount;
	/***/
	protected int rowCount;
	/***/
	protected String[] columnNames;
	/***/
	protected Object[][] data;
	
	/***/
	protected HashSet includeList;
	/***/
	protected HashSet excludeList;
		
	// Methods specific to MapTableModel ...
		
	/**
	 * <p>Check whether or not the named attribute is acceptable for inclusion as a column in the table.</p>
	 *
	 * @param	includeList	a set of upper case string names for suitable attributes (currently ignored)
	 * @param	excludeList	a set of upper case string names for unsuitable attributes
	 * @param	name		the name of the attribute to be checked (case insensitive)
	 * @return			true if the attribute is acceptable
	 */
	protected boolean isAcceptable(HashSet includeList,HashSet excludeList,String name) {
//System.err.println("MapTableModel.isAcceptable(): checking "+name+" is in exclude list "+excludeList.contains(name.toUpperCase(java.util.Locale.US)));
		return name != null && (excludeList == null || !excludeList.contains(name.toUpperCase(java.util.Locale.US)));
	}
	
	/**
	 * <p>Construct an empty single row table model.</p>
	 */
	public MapTableModel() {
		includeList=null;
		excludeList=null;
		initializeModelFromMap(null,null);
	}
	
	/**
	 * <p>Construct a single row table model filled with the supplied attributes and values.</p>
	 *
	 * @param	map			a map of string names for attributes to their string values
	 * @param	descriptiveNameMap	a map of string names for attributes to descriptions for use as column titles (may be null)
	 */
	public MapTableModel(Map map,Map descriptiveNameMap) {
		includeList=null;
		excludeList=null;
		initializeModelFromMap(map,descriptiveNameMap);
	}
	
	/**
	 * <p>Construct a single row table model filled with the supplied attributes and values.</p>
	 *
	 * @param	map			a map of string names for attributes to their string values
	 * @param	descriptiveNameMap	a map of string names for attributes to descriptions for use as column titles (may be null)
	 * @param	includeList		a set of upper case string names for suitable attributes (may be null)
	 * @param	excludeList		a set of upper case string names for unsuitable attributes (may be null)
	 */
	public MapTableModel(Map map,Map descriptiveNameMap,HashSet includeList,HashSet excludeList) {
		this.includeList=includeList;
		this.excludeList=excludeList;
		initializeModelFromMap(map,descriptiveNameMap);
	}

	/**
	 * <p>Initialize a single row table model filled with the supplied attributes and values.</p>
	 *
	 * @param	map			a map of string names for attributes to their string values
	 * @param	descriptiveNameMap	a map of string names for attributes to descriptions for use as column titles (may be null)
	 */
	public void initializeModelFromMap(Map map,Map descriptiveNameMap) {
		rowCount = 1;
		//columnCount = map.size();
		columnCount = 0;
		
		if (map == null) {
			columnNames = null;
			data = null;
		}
		else {
			// Pass 1 ... find the acceptable keys that actually have values, and build a sorted set of their names ...
			
			TreeSet set = new TreeSet();
			Iterator i = map.keySet().iterator();
			while (i.hasNext()) {
				String name = (String)i.next();
				if (isAcceptable(includeList,excludeList,name) && map.get(name) != null)
					set.add(name);
			}

			// Pass 2 ... extract the acceptable keys in sorted order of name of the attributes ...
			
			columnCount = set.size();
			columnNames = new String[columnCount];
			data = new Object[1][];
			data[0] = new String[columnCount];
			int j=0;
			i = set.iterator();
			while (i.hasNext() && j<columnCount) {
				String name = (String)i.next();
				if (isAcceptable(includeList,excludeList,name)) {
					String descriptiveName = descriptiveNameMap == null ? null : (String)(descriptiveNameMap.get(name));
					String useName = descriptiveName == null ? name : descriptiveName;
					String useValue = (String)(map.get(name));
					if (useValue != null) {
						columnNames[j] = useName;
						data[0][j] = useValue;
						++j;
					}
				}
			}
		}
		fireTableChanged(null);
	}
	
	// Implement methods of base class ...
	
	/***/
	public int getColumnCount() { return columnCount; }
	/***/
	public int getRowCount() { return rowCount;}
	/**
	 * @param	row
	 * @param	col
	 */
	public Object getValueAt(int row, int col) { return data[row][col]; }
	/**
	 * @param	row
	 * @param	col
	 */
	public boolean isCellEditable(int row, int col) { return false; }
	/**
	 * @param	col
	 */
	public String getColumnName(int col) { return columnNames[col]; }
}

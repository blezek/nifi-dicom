/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;

/**
 * <p>The {@link com.pixelmed.dicom.AttributeListTableModel AttributeListTableModel} class extends a
 * {@link javax.swing.table.AbstractTableModel AbstractTableModel} to abstract the contents of a list of attributes as
 * a single row table in order to provide support for a {@link com.pixelmed.dicom.AttributeListTableBrowser AttributeListTableBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.table.AbstractTableModel javax.swing.table.AbstractTableModel}.</p>
 *
 * @author	dclunie
 */
public class AttributeListTableModel extends AbstractTableModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeListTableModel.java,v 1.17 2017/01/24 10:50:35 dclunie Exp $";
	
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
		
	// Methods specific to AttributeListTableModel ...
		
	/**
	 * <p>Is an attribute acceptable for inclusion?</p>
	 *
	 * <p>Attributes with a VR of SQ, OB, OW and private tags are always excluded.</p>
	 *
	 * @param	includeList	the list of attributes to include
	 * @param	excludeList	the list of attributes to exclude
	 * @param	t		the tag of the attribute to test
	 * @param	vr		the VR of the attribute to test
	 * @return			true if attribute is acceptable for inclusion
	 */
	protected boolean isAcceptable(HashSet includeList,HashSet excludeList,AttributeTag t,byte[] vr) {
		return     (excludeList == null || !excludeList.contains(t))
			&& (includeList == null ||  includeList.contains(t))
			&& !ValueRepresentation.isSequenceVR(vr)
			//&& !ValueRepresentation.isUniqueIdentifierVR(vr) 
			&& !ValueRepresentation.isOtherByteOrWordVR(vr) 
			&& !ValueRepresentation.isOtherUnspecifiedVR(vr)
			//&& !ValueRepresentation.isAttributeTagVR(vr)
			&& t.getGroup()%2 == 0					// no private tags
			&& t.getElement() != 0					// no group lengths
			;
	}
	
	/**
	 * <p>Construct an empty table model.</p>
	 */
	public AttributeListTableModel() {
		includeList=null;
		excludeList=null;
		initializeModelFromAttributeList(null);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 */
	public AttributeListTableModel(AttributeList list) {
		includeList=null;
		excludeList=null;
		initializeModelFromAttributeList(list);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 * @param	includeList	attributes to include
	 * @param	excludeList	attributes to exclude
	 */
	public AttributeListTableModel(AttributeList list,HashSet includeList,HashSet excludeList) {
		this.includeList=includeList;
		this.excludeList=excludeList;
		initializeModelFromAttributeList(list);
	}

	/**
	 * <p>Populate the table model from an attribute list.</p>
	 *
	 * @param	list	the attributes whose values to use
	 */
	public void initializeModelFromAttributeList(AttributeList list) {
		rowCount = 1;
		//columnCount = list.size();
		columnCount = 0;
		
		if (list == null) {
			columnNames = null;
			data = null;
		}
		else {
			DicomDictionary dict = list.getDictionary();

			// Pass 1 ... find the acceptable attributes and build a sorted set of their names ...
			
			TreeSet set = new TreeSet();
			Iterator i = list.values().iterator();
			while (i.hasNext()) {
				Attribute a = (Attribute)i.next();
				if (isAcceptable(includeList,excludeList,a.getTag(),a.getVR())) {
					String name = dict.getNameFromTag(a.getTag());
					if (name != null) set.add(dict.getNameFromTag(a.getTag()));
				}
			}

			// Pass 2 ... extract the acceptable attributes in sorted order of name of the attributes ...
			
			columnCount = set.size();
			columnNames = new String[columnCount];
			data = new Object[1][];
			data[0] = new String[columnCount];
			int j=0;
			i = set.iterator();
			while (i.hasNext() && j<columnCount) {
				String name = (String)i.next();
				AttributeTag t = dict.getTagFromName(name);
				if (isAcceptable(includeList,excludeList,t,dict.getValueRepresentationFromTag(t))) {
					columnNames[j] = name;
					data[0][j] = Attribute.getDelimitedStringValuesOrEmptyString(list,t);
//System.err.println("Adding "+name+" tag "+t+" with value "+data[0][j]);
					++j;
				}
			}
		}
		fireTableChanged(null);
	}
	
	// Implement methods of base class ...
	
	public int getColumnCount() { return columnCount; }

	public int getRowCount() { return rowCount;}

	public Object getValueAt(int row, int col) { return data[row][col]; }

	public boolean isCellEditable(int row, int col) { return false; }

	public String getColumnName(int col) { return columnNames[col]; }
}

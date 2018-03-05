/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;

/**
 * <p>The abstract {@link com.pixelmed.dicom.AttributeListFunctionalGroupsTableModelAllFrames AttributeListFunctionalGroupsTableModelAllFrames} class extends a
 * {@link com.pixelmed.dicom.AttributeListFunctionalGroupsTableModel AttributeListFunctionalGroupsTableModel} to abstract the contents of a list of attributes
 * containing shared and per-frame functional groups for multi-frame objects as
 * a table with a row for each frame in order to provide support for a {@link com.pixelmed.dicom.AttributeListTableBrowser AttributeListTableBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.table.AbstractTableModel javax.swing.table.AbstractTableModel}.</p>
 *
 * @author	dclunie
 */
public class AttributeListFunctionalGroupsTableModelAllFrames extends AttributeListFunctionalGroupsTableModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeListFunctionalGroupsTableModelAllFrames.java,v 1.10 2017/01/24 10:50:35 dclunie Exp $";

	// Methods specific to AttributeListTableModel ...
		
	/**
	 * <p>Construct an empty table model.</p>
	 */
	public AttributeListFunctionalGroupsTableModelAllFrames() {
		super(null);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 */
	public AttributeListFunctionalGroupsTableModelAllFrames(AttributeList list) {
		super(list);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 * @param	includeList	attributes to include
	 * @param	excludeList	attributes to exclude
	 */
	public AttributeListFunctionalGroupsTableModelAllFrames(AttributeList list,HashSet includeList,HashSet excludeList) {
		super(list,includeList,excludeList);
	}
	
	private TreeSet set;
	private HashMap values;		// each value is either a String or an ArrayList (when value varies per-frame)

	/**
	 * <p>Populate the table model from an attribute list.</p>
	 *
	 * <p>Only include attributes that vary per-frame.</p>
	 *
	 * @param	list	the attributes whose values to use
	 */
	public void initializeModelFromAttributeList(AttributeList list) {
//System.err.println("AttributeListFunctionalGroupsTableModelAllFrames.initializeModelFromAttributeList:");
		rowCount = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
//System.err.println("AttributeListFunctionalGroupsTableModelAllFrames.initializeModelFromAttributeList: rowCount="+rowCount);
		//columnCount = list.size();
		columnCount = 0;
		
		if (list == null) {
			columnNames = null;
			data = null;
		}
		else {
			DicomDictionary dict = list.getDictionary();

			// Pass 1 ... find the acceptable attributes and build a sorted set of their names, caching their values ...
			
			set = new TreeSet();
			values = new HashMap();
			
			// ONLY things that vary per frame ...
			
			SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (perFrameFunctionalGroupsSequence != null) {
				addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMapForAllFrames(set,perFrameFunctionalGroupsSequence,values);
			}

			// Pass 2 ... extract the acceptable attribute values in sorted order of name of the attributes ...
			data = new Object[rowCount][];
			for (int row=0; row<rowCount; ++row) {	
				columnCount = set.size()+1;
				columnNames = new String[columnCount+1];
				data[row] = new String[columnCount+1];
				columnNames[0] = "Frame #";
				data[row][0] = Integer.toString(row+1);				// row order is same as frame order (no sort by dimension yet) 
				int j=1;
				Iterator i = set.iterator();
				while (i.hasNext() && j<columnCount) {
					String name = (String)i.next();
					columnNames[j] = name;
					Object value = values.get(name);
					if (value instanceof String) {
						data[row][j] = (String)value;
					}
					else if (value instanceof String[] && ((String[])value).length > row) {
						data[row][j] = ((String[])value)[row];		// row order is same as frame order (no sort by dimension yet) 
					}
					++j;
				}
			}
		}
		fireTableChanged(null);
	}
}

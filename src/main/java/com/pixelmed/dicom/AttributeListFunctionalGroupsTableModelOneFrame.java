/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;

/**
 * <p>The abstract {@link com.pixelmed.dicom.AttributeListFunctionalGroupsTableModelOneFrame AttributeListFunctionalGroupsTableModelOneFrame} class extends a
 * {@link com.pixelmed.dicom.AttributeListFunctionalGroupsTableModel AttributeListFunctionalGroupsTableModel} to abstract the contents of a list of attributes
 * containing shared and per-frame functional groups for multi-frame objects as
 * a table with a single row for a single frame in order to provide support for a {@link com.pixelmed.dicom.AttributeListTableBrowser AttributeListTableBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.table.AbstractTableModel javax.swing.table.AbstractTableModel}.</p>
 *
 * @author	dclunie
 */
public class AttributeListFunctionalGroupsTableModelOneFrame extends AttributeListFunctionalGroupsTableModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeListFunctionalGroupsTableModelOneFrame.java,v 1.11 2017/01/24 10:50:35 dclunie Exp $";

	// Methods specific to AttributeListFunctionalGroupsTableModelOneFrame ...
		
	/***/
	public AttributeListFunctionalGroupsTableModelOneFrame() {
		super(null);
	}
	
	/**
	 * @param	list		the list of attributes whose values to use
	 */
	public AttributeListFunctionalGroupsTableModelOneFrame(AttributeList list) {
		super(list);
	}
	
	/**
	 * @param	list		the list of attributes whose values to use
	 * @param	includeList	attributes to include
	 * @param	excludeList	attributes to exclude
	 */
	public AttributeListFunctionalGroupsTableModelOneFrame(AttributeList list,HashSet includeList,HashSet excludeList) {
		super(list,includeList,excludeList);
	}
	
	private TreeSet set;
	private HashMap values;		// each value is either a String or an ArrayList (when value varies per-frame)

	/**
	 * <p>Populate the table model from an attribute list.</p>
	 *
	 * @param	list	the attributes whose values to use
	 */
	public void initializeModelFromAttributeList(AttributeList list) {
//System.err.println("AttributeListFunctionalGroupsTableModelOneFrame.initializeModelFromAttributeList:");
		rowCount = 1;
//System.err.println("AttributeListFunctionalGroupsTableModelOneFrame.initializeModelFromAttributeList: rowCount="+rowCount);
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
			addAllAcceptableAttributesToSetAndValuesToMap(set,list,values);
			
			SequenceAttribute sharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
			if (sharedFunctionalGroupsSequence != null && sharedFunctionalGroupsSequence.getNumberOfItems() >= 1) { // should be 1
				SequenceItem item = (SequenceItem)sharedFunctionalGroupsSequence.getItem(0);
				addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMap(set,item.getAttributeList(),values);
			}

			SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (perFrameFunctionalGroupsSequence != null) {
				addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMapForAllFrames(set,perFrameFunctionalGroupsSequence,values);
			}

			// Pass 2 ... extract the acceptable attribute values in sorted order of name of the attributes ...
			columnCount = set.size()+1;
			columnNames = new String[columnCount+1];
			data = new Object[1][];
			data[0] = new String[columnCount+1];
			columnNames[0] = "Frame #";
			data[0][0] = Integer.toString(1); 
			int j=1;
			Iterator i = set.iterator();
			while (i.hasNext() && j<columnCount) {
				String name = (String)i.next();
				columnNames[j] = name;
				Object value = values.get(name);
				if (value instanceof String) {
					data[0][j] = (String)value;
				}
				//else if (value instanceof String[]) {
				//	data[0][j] = ((String[])value)[0];		// default to first frame
				//}
				++j;
			}
		}
		fireTableChanged(null);
	}

	/**
	 * <p>Update anything that varies on a per-frame basis to the values for the specified frame.</p>
	 *
	 * @param	frameNumber	the selected frame, numbered from 0
	 */
	public void selectValuesForDifferentFrame(int frameNumber) {
//System.err.println("AttributeListFunctionalGroupsTableModelOneFrame.selectValuesForDifferentFrame: frame "+frameNumber);
		{
			data[0][0] = Integer.toString(frameNumber+1); 
			int j=1;
			Iterator i = set.iterator();
			while (i.hasNext() && j<columnCount) {
				String name = (String)i.next();
//System.err.println("AttributeListFunctionalGroupsTableModelOneFrame.selectValuesForDifferentFrame: trying name "+name);
				Object value = values.get(name);
				// leave stuff that doesn't vary per-frame alone
				if (value instanceof String[] && ((String[])value).length > frameNumber) {
					String v = ((String[])value)[frameNumber];
					data[0][j] = v;
//System.err.println("AttributeListFunctionalGroupsTableModelOneFrame.selectValuesForDifferentFrame: changing value of name "+name+" to "+v);
				}
				++j;
			}
		}
		fireTableChanged(null);
	}
}

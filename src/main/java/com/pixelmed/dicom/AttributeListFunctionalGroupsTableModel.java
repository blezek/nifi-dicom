/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.io.*;

/**
 * <p>The abstract {@link com.pixelmed.dicom.AttributeListFunctionalGroupsTableModel AttributeListFunctionalGroupsTableModel} class extends a
 * {@link com.pixelmed.dicom.AttributeListTableModel AttributeListTableModel} to abstract the contents of a list of attributes
 * containing shared and per-frame functional groups for multi-frame objects as
 * a table in order to provide support for a {@link com.pixelmed.dicom.AttributeListTableBrowser AttributeListTableBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.table.AbstractTableModel javax.swing.table.AbstractTableModel}.</p>
 *
 * @author	dclunie
 */
public abstract class AttributeListFunctionalGroupsTableModel extends AttributeListTableModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeListFunctionalGroupsTableModel.java,v 1.20 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Extract from the attribute list all the attributes and values to be included in the table model.</p>
	 *
	 * @param	set		a sorted set of the names of acceptable attributes
	 * @param	list		the list in which to find the attribute values
	 * @param	values		a map to which is added a String or an ArrayList (when value varies per-frame) for each attribute name found
	 * @param	prefixForName	a string with which to prefix the attribute name (e.g. "f." might indicate a frame-varying attribute)
	 */
	protected void addAllAcceptableAttributesToSetAndValuesToMap(TreeSet set,AttributeList list,HashMap values,String prefixForName) {
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesToSetAndValuesToMap:");
		if (list != null) {
			DicomDictionary dict = list.getDictionary();
			Iterator i = list.values().iterator();
			while (i.hasNext()) {
				Attribute a = (Attribute)i.next();
				if (isAcceptable(includeList,excludeList,a.getTag(),a.getVR())) {
					String name = dict.getNameFromTag(a.getTag());
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesToSetAndValuesToMap: set.add tag "+a.getTag()+" name "+name);
					if (name != null) {
						name=prefixForName+name;
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesToSetAndValuesToMap: refixed name "+prefixForName+name);
						set.add(name);
						//String value = a.getSingleStringValueOrEmptyString();
						String value = a.getDelimitedStringValuesOrEmptyString();
						values.put(name,value);
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesToSetAndValuesToMap: put "+name+" tag "+a.getTag()+" with value "+value);

					}
					// could be null even for standard, e.g. retired (0020,31xx) SourceImageID
				}
			}
		}
	}

	/**
	 * <p>Extract from the attribute list all the attributes and values to be included in the table model.</p>
	 *
	 * @param	set		a sorted set of the names of acceptable attributes
	 * @param	list		the list in which to find the attribute values
	 * @param	values		a map to which is added a String or an ArrayList (when value varies per-frame) for each attribute name found
	 */
	protected void addAllAcceptableAttributesToSetAndValuesToMap(TreeSet set,AttributeList list,HashMap values) {
		addAllAcceptableAttributesToSetAndValuesToMap(set,list,values,"");
	}

	/**
	 * <p>Extract from the attribute list all attributes within sequence attributes, and their values to be included in the table model.</p>
	 *
	 * @param	set		a sorted set of the names of acceptable attributes
	 * @param	list		the list in which to find the sequence attributes whose items are examined for attributes to add
	 * @param	values		a map to which is added a String or an ArrayList (when value varies per-frame) for each attribute name found
	 */
	protected void addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMap(TreeSet set,AttributeList list,HashMap values) {
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMap:");
		if (list != null) {
			Iterator i = list.values().iterator();
			while (i.hasNext()) {
				Attribute atry = (Attribute)i.next();
				if (atry != null && atry instanceof SequenceAttribute) {
					SequenceAttribute a = (SequenceAttribute)atry;
					if (a != null && a.getNumberOfItems() >= 1) { 		// should be 1
//System.err.println("AttributeListFunctionalGroupsTableModel.addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMap: adding items contents");
						SequenceItem item = (SequenceItem)a.getItem(0);
						addAllAcceptableAttributesToSetAndValuesToMap(set,item.getAttributeList(),values,"s.");
					}
				}
			}
		}
	}

	/**
	 * <p>Extract from the Per-frame Functional Groups Sequence all attributes within frame items and their values to be included in the table model.</p>
	 *
	 * @param	set		a sorted set of the names of acceptable attributes
	 * @param	pfa		the sequence attribute that is the Per-frame Functional Groups Sequence
	 * @param	values		a map to which is added a String or an ArrayList (when value varies per-frame) for each attribute name found
	 */
	protected void addAllAcceptableAttributesWithinSequenceAttributesToSetAndValuesToMapForAllFrames(TreeSet set,SequenceAttribute pfa,HashMap values) {
		int sizeToAllocateForArraysOfValues = pfa.getNumberOfItems();
//System.err.println("sizeToAllocateForArraysOfValues = "+sizeToAllocateForArraysOfValues);
		int frameNumber = 0;
		Iterator pfitems = pfa.iterator();
		while (pfitems.hasNext()) {
			SequenceItem fitem = (SequenceItem)pfitems.next();
			AttributeList flist = fitem.getAttributeList();
			if (flist != null) {
				//DicomDictionary dict = flist.getDictionary();
				Iterator fi = flist.values().iterator();
				while (fi.hasNext()) {
					Attribute atry = (Attribute)fi.next();
					SequenceAttribute fgma;
					if (atry != null && atry instanceof SequenceAttribute && (fgma=(SequenceAttribute)atry) != null && fgma.getNumberOfItems() >= 1) { 		// should be 1
						SequenceItem fgmitem = (SequenceItem)fgma.getItem(0);
						AttributeList fgmlist = fgmitem.getAttributeList();
						if (fgmlist != null) {
							DicomDictionary dict = fgmlist.getDictionary();
							Iterator i = fgmlist.values().iterator();
							while (i.hasNext()) {
								Attribute a = (Attribute)i.next();
								if (isAcceptable(includeList,excludeList,a.getTag(),a.getVR())) {
									String name = "f."+dict.getNameFromTag(a.getTag());
//System.err.println("Frame: "+frameNumber+" acceptable name ="+name);
									if (!set.contains(name)) {
//System.err.println("Frame: "+frameNumber+" not in set so adding ="+name);
										// first time encountered (should be, but may not be, first frame)
										set.add(name);
										values.put(name,new ArrayList(sizeToAllocateForArraysOfValues));
										values.put(name,new String[sizeToAllocateForArraysOfValues]);
									}
									//String value = a.getSingleStringValueOrEmptyString();
									String value = a.getDelimitedStringValuesOrEmptyString();
									String[] array = (String[])values.get(name);
//System.err.println("Frame: "+frameNumber+" values array for name "+name+" length "+array.length);
									//array.set(frameNumber,value);
									array[frameNumber]=value;
//System.err.println("Frame: "+frameNumber+" value for name "+name+" = "+value);
								}
							}
						}
					}
				}
			}
			++frameNumber;
		}
	}

	// Methods specific to AttributeListTableModel ...
		
	/**
	 * <p>Construct an empty table model.</p>
	 */
	public AttributeListFunctionalGroupsTableModel() {
		super(null);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 */
	public AttributeListFunctionalGroupsTableModel(AttributeList list) {
		super(list);
	}
	
	/**
	 * <p>Construct the table model from an attribute list.</p>
	 *
	 * @param	list		the list of attributes whose values to use
	 * @param	includeList	attributes to include
	 * @param	excludeList	attributes to exclude
	 */
	public AttributeListFunctionalGroupsTableModel(AttributeList list,HashSet includeList,HashSet excludeList) {
		super(list,includeList,excludeList);
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

//import java.util.Iterator;
//import java.util.TreeSet;
//import java.util.Vector;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
//import com.pixelmed.dicom.AttributeTag;
//import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.geometry.GeometryOfVolume;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;

/**
 * <p>A class to extract selected DICOM annotative attributes that describe the orientation of an image or frame.</p>
 *
 * @author	dclunie
 */
class OrientationAnnotations {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/OrientationAnnotations.java,v 1.6 2017/01/24 10:50:40 dclunie Exp $";
	
	/***/
	private GeometryOfVolume imageGeometry;
	/***/
	private String rowOrientationFromPatientOrientation;
	/***/
	private String columnOrientationFromPatientOrientation;
	/***/
	private boolean quadruped;
	
	/**
	 * @param	rowOrientation
	 * @param	columnOrientation
	 */
	public OrientationAnnotations(String rowOrientation, String columnOrientation) {
		rowOrientationFromPatientOrientation = rowOrientation;
		columnOrientationFromPatientOrientation = columnOrientation;
		imageGeometry = null;
		quadruped = false;
	}
	
	/**
	 * @param	list		the DICOM attributes of a single or multi-frame image
	 * @param	imageGeometry	the geometry of a single or multi-frame image
	 */
	public OrientationAnnotations(AttributeList list,GeometryOfVolume imageGeometry) {
		doCommonConstructorStuff(list,imageGeometry);
	}
	
	/**
	 * @param	list		the DICOM attributes of a single or multi-frame image
	 */
	public OrientationAnnotations(AttributeList list) {
		GeometryOfVolume imageGeometry = null;
		try {
			imageGeometry = new GeometryOfVolumeFromAttributeList(list);
		}
		catch (DicomException e) {
		}
		doCommonConstructorStuff(list,imageGeometry);
	}
	
	/**
	 * @param	list		the DICOM attributes of a single or multi-frame image
	 * @param	imageGeometry	the geometry of a single or multi-frame image
	 */
	private void doCommonConstructorStuff(AttributeList list,GeometryOfVolume imageGeometry) {
		this.imageGeometry=imageGeometry;
		quadruped = Attribute.getSingleStringValueOrDefault(list,TagFromName.AnatomicalOrientationType,"BIPED").equals("QUADRUPED");
		rowOrientationFromPatientOrientation="";
		columnOrientationFromPatientOrientation="";
		if (list != null) {
			Attribute aPatientOrientation = list.get(TagFromName.PatientOrientation);
			if (aPatientOrientation != null && aPatientOrientation.getVM() == 2) {
				try {
					String[] vPatientOrientation = aPatientOrientation.getStringValues();
					if (vPatientOrientation != null && vPatientOrientation.length == 2) {
						rowOrientationFromPatientOrientation=vPatientOrientation[0];
						columnOrientationFromPatientOrientation=vPatientOrientation[1];
					}
				}
				catch (DicomException e) {
				}
			}
		}
	}

	/**
	 * <p>Get the letter representation of the orientation of the rows of this frame.</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return	a string rendering of the row orientation, L or R, A or P, H or F,
	 *		more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getRowOrientation(int frame) {
		String rowOrientation = null;
		if (imageGeometry != null) {
			rowOrientation = imageGeometry.getRowOrientation(frame,quadruped);
		}
		if (rowOrientation == null || rowOrientation.length() == 0) {
			rowOrientation=rowOrientationFromPatientOrientation;
		}
		return rowOrientation;
	}

	/**
	 * <p>Get the letter representation of the orientation of the columns of this frame.</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return	a string rendering of the column orientation, L or R, A or P, H or F,
	 *		more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getColumnOrientation(int frame) {
		String columnOrientation = null;
		if (imageGeometry != null) {
			columnOrientation = imageGeometry.getColumnOrientation(frame,quadruped);
		}
		if (columnOrientation == null || columnOrientation.length() == 0) {
			columnOrientation=columnOrientationFromPatientOrientation;
		}
		return columnOrientation;
	}
}


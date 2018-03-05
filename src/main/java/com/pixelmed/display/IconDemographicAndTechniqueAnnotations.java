/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

/**
 * <p>A class to extract selected DICOM annotative attributes into defined displayed area relative positions for icons.</p>
 *
 * @author	dclunie
 */
public class IconDemographicAndTechniqueAnnotations extends DemographicAndTechniqueAnnotations {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/IconDemographicAndTechniqueAnnotations.java,v 1.5 2017/01/24 10:50:40 dclunie Exp $";
	
	protected void initializeDefaultLayout() {
		layout=new Vector();
		
		// top
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"PT ",true,true,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientID,null,null,null,true,true,0,1,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientName,null,null,null,true,true,1,0,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"ST ",true,true,2,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyID,null,null,null,true,true,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,2,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyDate,null,null,null,true,true,2,3,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyDescription,null,null,null,true,true,3,0,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"SE ",true,true,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesNumber,null,null,null,true,true,4,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesDate,null,null,null,true,true,4,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Modality,null,null,null,true,true,4,5,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesDescription,null,null,null,true,true,5,0,null,NOSPECIAL));
	}


	/**
	 * @param	list			the DICOM attributes of a single or multi-frame image
	 */
	public IconDemographicAndTechniqueAnnotations(AttributeList list) {
		super(list);
	}
}


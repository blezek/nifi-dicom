/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.util.*;

import com.pixelmed.dicom.*;

/**
 * @author	dclunie
 */
public class DisplayStructuredReportBrowser extends StructuredReportBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DisplayStructuredReportBrowser.java,v 1.9 2017/01/24 10:50:40 dclunie Exp $";

	private int frameWidthWanted;
	private int frameHeightWanted;

	private Map mapOfSOPInstanceUIDToReferencedFileName;

	/**
	 * @param	list
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @throws	DicomException
	 */
	public DisplayStructuredReportBrowser(AttributeList list,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted) throws DicomException {
		super(list);
		this.mapOfSOPInstanceUIDToReferencedFileName=mapOfSOPInstanceUIDToReferencedFileName;
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	list
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @param	title
	 * @throws	DicomException
	 */
	public DisplayStructuredReportBrowser(AttributeList list,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted,String title) throws DicomException {
		super(list,title);
		this.mapOfSOPInstanceUIDToReferencedFileName=mapOfSOPInstanceUIDToReferencedFileName;
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	instances
	 */
	protected void doSomethingWithSelectedSOPInstances(Vector instances) {
		DicomBrowser.loadAndDisplayImagesFromSOPInstances(instances,mapOfSOPInstanceUIDToReferencedFileName,
			frameWidthWanted,frameHeightWanted);
	}
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.dicom.*;

/**
 * @author	dclunie
 */
class DisplayDicomDirectoryBrowser extends DicomDirectoryBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DisplayDicomDirectoryBrowser.java,v 1.9 2017/01/24 10:50:40 dclunie Exp $";

	private int frameWidthWanted;
	private int frameHeightWanted;

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @param	frame
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @throws	DicomException
	 */
	public DisplayDicomDirectoryBrowser(AttributeList list,String parentFilePath,JFrame frame,
			int frameWidthWanted,int frameHeightWanted) throws DicomException {
		super(list,parentFilePath,frame);
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	paths
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		DicomBrowser.loadAndDisplayImagesFromDicomFiles(paths,
			getDicomDirectory().getMapOfSOPInstanceUIDToReferencedFileName(getParentFilePath()),
			frameWidthWanted,frameHeightWanted);
	}
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.SetOfDicomFiles;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.utils.CopyStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to process a set of multiple files and check that all referenced SOP Instances are present within the set.</p>
 *
 * @author	dclunie
 */
public class CheckAllUIDReferencesResolve {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/CheckAllUIDReferencesResolve.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CheckAllUIDReferencesResolve.class);
	
	private SetOfDicomFiles dicomFilesRead = new SetOfDicomFiles();

	private Map<String,SetOfDicomFiles.DicomFile> mapOfSOPInstanceUIDToDicomFile = new HashMap<String,SetOfDicomFiles.DicomFile>();
	
	protected class OurMediaImporter extends MediaImporter {
	
		public OurMediaImporter() {
			super(null);
			
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
//System.err.println("Doing "+mediaFileName);
			try {
				SetOfDicomFiles.DicomFile dicomFile = dicomFilesRead.add(mediaFileName,true/*keepList*/,false/*keepPixelData*/);
				AttributeList list = dicomFile.getAttributeList();
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					mapOfSOPInstanceUIDToDicomFile.put(sopInstanceUID,dicomFile);
				}
				else {
					throw new DicomException("No SOP Instance UID in file "+mediaFileName);
				}
			}
			catch (Exception e) {
				slf4jlogger.error("File {} exception ",mediaFileName,e);
			}
		}
	}

	/**
	 * <p>Process a set of multiple files and check that all referenced SOP Instances are present within the set.</p>
	 *
	 * @param	srcs
	 */
	public CheckAllUIDReferencesResolve(String[] srcs) throws FileNotFoundException, IOException, DicomException {
		
		// 1st stage ... read all the files  ...
		OurMediaImporter importer = new OurMediaImporter();
		for (String src : srcs) {
			importer.importDicomFiles(src);
		}
		
		// 2nd stage ... process what was read to find the any references and see if they resolve ...
		{
			Iterator i = dicomFilesRead.iterator();
			while (i.hasNext()) {
				SetOfDicomFiles.DicomFile dicomFile = (SetOfDicomFiles.DicomFile)i.next();
				AttributeList list = dicomFile.getAttributeList();
				Set<String> setOfReferencedSOPInstanceUIDs = list.findAllNestedReferencedSOPInstanceUIDs();
				for (String referencedSOPInstanceUID : setOfReferencedSOPInstanceUIDs) {
					if (mapOfSOPInstanceUIDToDicomFile.get(referencedSOPInstanceUID) ==  null) {
						slf4jlogger.info("In file {} was a reference to SOP Instance UID {} that is not present in the set of files",dicomFile.getFileName(),referencedSOPInstanceUID);
					}
					//else {
					//	System.err.println("In file "+dicomFile.getFileName()+" was a reference to SOP Instance UID "+referencedSOPInstanceUID+" that is present in the set of files");
					//}
				}
			}
		}
	}
	
	/**
	 * <p>Process a set of multiple files and check that all referenced SOP Instances are present within the set.</p>
	 *
	 * @param	arg		one or more source folders or DICOMDIRs
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length > 0) {
				new CheckAllUIDReferencesResolve(arg);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.CheckAllUIDReferencesResolve srcdir|DICOMDIR [srcdir|DICOMDIR]*");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;

import java.util.HashSet;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class copies a set of DICOM files, if they match specified criteria.</p>
 *
 * @author	dclunie
 */
public class FindAndCopySelectedDicomFiles extends MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/FindAndCopySelectedDicomFiles.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FindAndCopySelectedDicomFiles.class);

	protected boolean exact;

	protected Set<String> sopClasses;

	protected String outputPath;
	
	public FindAndCopySelectedDicomFiles(MessageLogger logger) {
		super(logger);
	}
	
	/**
	 * <p>Check for valid information, and that the file is not compressed or not a suitable storage object for import.</p>
	 *
	 * @param	sopClassUID
	 * @param	transferSyntaxUID
	 */
	protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
//System.err.println("isOKToImport(): sopClassUID = \""+sopClassUID+"\"");
//System.err.println("isOKToImport(): sopClasses.contains(sopClassUID) = "+sopClasses.contains(sopClassUID));
		return sopClasses.contains(sopClassUID);
	}

	/**
	 * <p>Do something with the referenced DICOM file that has been encountered.</p>
	 *
	 * <p>This method needs to be implemented in a sub-class to do anything useful.
	 * The default method does nothing.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
		//logLn("MediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
		slf4jlogger.info("MediaImporter.doSomethingWithDicomFile(): {}",mediaFileName);
		try {
			AttributeList list = new AttributeList();
			list.readOnlyMetaInformationHeader(mediaFileName);
			String outputFileName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.MediaStorageSOPInstanceUID);
			if (outputFileName.length() > 0) {
				CopyStream.copy(new File(mediaFileName),new File(outputPath,outputFileName));
			}
			else {
				throw new DicomException("Cannot extract SOP Instance UID from \""+mediaFileName+"\" to create output file name - ignoring");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * <p>Copy a set of DICOM files, if they match specified criteria.</p>
	 *
	 * <p>Does not actually check the Modality value in the file, but matches the SOP Class against what is returned from {@link com.pixelmed.dicom.SOPClass#getPlausibleStandardSOPClassUIDsForModality(String) SOPClass.getPlausibleStandardSOPClassUIDsForModality(String)}.</p>
	 *
	 * @param	arg	array of four strings - the input path, the output path, and the SOP Class UID or Modality
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 3) {
				FindAndCopySelectedDicomFiles importer = new FindAndCopySelectedDicomFiles(new PrintStreamMessageLogger(System.err));
				importer.outputPath = arg[1];
				importer.sopClasses = new HashSet();
				if (arg[2].startsWith("1")) {
//System.err.println("main(): importer.sopClasses.add = \""+arg[2]+"\"");
					importer.sopClasses.add(arg[2]);
				}
				else {
					String[] sopClasses = SOPClass.getPlausibleStandardSOPClassUIDsForModality(arg[2]);
					if (sopClasses == null) {
						throw new DicomException("Cannot find plausible SOP Standard Classes for modality \""+arg[2]+"\"");
					}
					else {
						for (String sopClass : sopClasses) {
//System.err.println("main(): importer.sopClasses.add = \""+sopClass+"\"");
						importer.sopClasses.add(sopClass);
						}
					}
				}
				importer.importDicomFiles(arg[0]);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.FindAndCopySelectedDicomFiles srcdir dstdir sopclass|modality");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}



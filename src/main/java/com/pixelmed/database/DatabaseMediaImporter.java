/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

//import java.util.Iterator;
//import java.util.Vector;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.MessageLogger;

/**
 * @author	dclunie
 */
public class DatabaseMediaImporter extends MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseMediaImporter.java,v 1.8 2017/01/24 10:50:34 dclunie Exp $";

	protected File savedInstancesFolder;
	protected DatabaseInformationModel databaseInformationModel;
	protected StoredFilePathStrategy storedFilePathStrategy;

	public DatabaseMediaImporter(String mediaDirectoryPath,File savedInstancesFolder,DatabaseInformationModel databaseInformationModel,MessageLogger logger) {
		super(mediaDirectoryPath,logger);
		this.savedInstancesFolder=savedInstancesFolder;
		this.databaseInformationModel=databaseInformationModel;
		this.storedFilePathStrategy = StoredFilePathStrategy.getDefaultStrategy();
	}

	public DatabaseMediaImporter(String mediaDirectoryPath,File savedInstancesFolder,StoredFilePathStrategy storedFilePathStrategy,DatabaseInformationModel databaseInformationModel,MessageLogger logger) {
		super(mediaDirectoryPath,logger);
		this.savedInstancesFolder=savedInstancesFolder;
		this.databaseInformationModel=databaseInformationModel;
		this.storedFilePathStrategy = storedFilePathStrategy;
	}

	/**
	 * <p>Makes a copy of the DICOM file in the temporary folder, reads the DICOM attributes to
	 * extract the necessary information and inserts a reference to the copied file in the database.</p>
	 *
	 * <p>If any errors are encountered during this process, the exceptions
	 * are caught, logged to stderr, and no further action is taken. Copying is not
	 * performed until after the DICOM attributes are successfully extracted, but
	 * orphan temporary copies could exist if the database insertion fails.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
//System.err.println("DatabaseMediaImporter:doSomethingWithDicomFile(): "+mediaFileName);
		try {
			DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(mediaFileName)));
			AttributeList list = new AttributeList();
			list.read(i,TagFromName.PixelData);	// do not need to read the whole file
			i.close();				// N.B.
									
			String sopInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
			if (sopInstanceUID == null) {
				throw new DicomException("Cannot get SOP Instance UID to make file name for local copy when inserting into database");
			}
			String localCopyFileName=storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(savedInstancesFolder,sopInstanceUID).getPath();
//System.err.println("DatabaseMediaImporter.doSomethingWithDicomFileOnMedia(): uid = "+sopInstanceUID+" path ="+localCopyFileName);
			CopyStream.copy(mediaFileName,localCopyFileName);
			databaseInformationModel.insertObject(list,localCopyFileName,DatabaseInformationModel.FILE_COPIED);
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}




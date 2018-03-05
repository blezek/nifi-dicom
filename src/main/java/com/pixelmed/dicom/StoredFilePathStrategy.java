/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.File;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This is an abstract class to support creating path names for how to organize the folders and files for stored composite instances based on their SOP Instance UID.</p>
 *
 * <p>Concrete subclasses implement various different strategies, which may be instantiated themselves, or accessed by the enumerated fields in this class.</p>
 *
 * <p>The choices may be passed as arguments to constructors of {@link com.pixelmed.network.StorageSOPClassSCPDispatcher StorageSOPClassSCPDispatcher}.</p>
 *
 * <p>Methods are provided to generate pathnames based on the supplied UID, as well as to create any sub-folders required and
 * generate altrernative path names if the existing path name is alreayd in use for some other purpose.
 * 
 * @author	dclunie
 * @author	jimirrer
 */
public abstract class StoredFilePathStrategy {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StoredFilePathStrategy.java,v 1.6 2017/01/24 10:50:39 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(StoredFilePathStrategy.class);

	protected StoredFilePathStrategy() {}

	/**
	 * <p>Store all the stored files in a single folder, using the SOP Instance UID as the filename.</p>
	 *
	 * @see com.pixelmed.dicom.StoredFilePathStrategySingleFolder
	 */
	public static final StoredFilePathStrategy BYSOPINSTANCEUIDINSINGLEFOLDER = new StoredFilePathStrategySingleFolder();

	/**
	 * <p>Store all the stored files in a hierarchy of folders using successive numeric components
	 * of the SOP Instance UID as the folder name and the SOP Instance UID as the filename within the most deeply nested folder.</p>
	 *
	 * @see com.pixelmed.dicom.StoredFilePathStrategyComponentFolders
	 */
	public static final StoredFilePathStrategy BYSOPINSTANCEUIDCOMPONENTFOLDERS = new StoredFilePathStrategyComponentFolders();

	/**
	 * <p>Store all the stored files in a hierarchy of folders using successive decimal digits of the hashcode
	 * of the SOP Instance UID as the folder name and the SOP Instance UID as the filename within the most deeply nested folder.</p>
	 *
	 * @see com.pixelmed.dicom.StoredFilePathStrategyHashSubFolders
	 */
	public static final StoredFilePathStrategy BYSOPINSTANCEUIDHASHSUBFOLDERS = new StoredFilePathStrategyHashSubFolders();
	
	/**
	 * <p>Get the default strategy.</p>
	 * @return		the default strategy (which is BYSOPINSTANCEUIDINSINGLEFOLDER)
	 */
	public static final StoredFilePathStrategy getDefaultStrategy() { return BYSOPINSTANCEUIDINSINGLEFOLDER; }

	/**
	 * <p>Generate a path to where to store a file based on its SOP Instance UID.</p>
	 *
	 * @param	sopInstanceUID			the SOP Instance UID of the instance to be saved
	 * @return							the path to the file, which may contain nested sub-folders
	 */
	public String makeStoredFilePath(String sopInstanceUID) {
		return null;					// not an abstract method as it should be, since we want to allow legacy use of ReceivedFilePathStrategy, which extends this class
	}

	/**
	 * <p>Generate a path to where to store a file based on its SOP Instance UID.</p>
	 *
	 * @param	savedInstancesFolder	the folder in which to save the instance
	 * @param	sopInstanceUID			the SOP Instance UID of the instance to be saved
	 * @return							the path to the file in the specified folder, which may contain nested sub-folders
	 */
	public File makeStoredFilePath(File savedInstancesFolder,String sopInstanceUID) {
		return new File(savedInstancesFolder,makeStoredFilePath(sopInstanceUID));
	}
	
	/**
	 * <p>Generate an alternative path to where to store a file based on its SOP Instance UID.</p>
	 *
	 * <p>Use when the normal path is already occupied by something other than a file (such as a folder).</p>
	 *
	 * @param	savedInstancesFolder	the folder in which to save the instance
	 * @param	alternativeSubfolder	the alternate sub-folder with the saved instance folder in which to save the instance
	 * @param	sopInstanceUID			the SOP Instance UID of the instance to be saved
	 * @return							the path to the file in the specified folder and alternate sub-folder
	 */
	public File makeAlternativeStoredFilePath(File savedInstancesFolder,String alternativeSubfolder,String sopInstanceUID) {
		// use the BYSOPINSTANCEUIDINSINGLEFOLDER strategy but within the alternativeSubfolder - cannot fail, but may impact performance if happens a lot due to too many files in one folder
		return StoredFilePathStrategy.BYSOPINSTANCEUIDINSINGLEFOLDER.makeStoredFilePath(new File(savedInstancesFolder,alternativeSubfolder),sopInstanceUID);
	}

	/**
	 * <p>Generate a path to where to store a file based on its SOP Instance UID and assure its reliability.</p>
	 *
	 * <p>Includes creating any necessary parent folders if they do not already exist,
	 * and using an alternate path or name if a desired file name already exists as something else (such as a folder).</p>
	 *
	 * @param	savedInstancesFolder	the folder in which to save the instance
	 * @param	alternativeSubfolder	the alternate sub-folder with the saved instance folder in which to save the instance
	 * @param	sopInstanceUID			the SOP Instance UID of the instance to be saved
	 * @return							the path to the file in the specified folder, which may contain nested sub-folders
	 */
	public File makeReliableStoredFilePathWithFoldersCreated(File savedInstancesFolder,String alternativeSubfolder,String sopInstanceUID) {
		File storedFile = makeStoredFilePath(savedInstancesFolder,sopInstanceUID);
		if (storedFile.exists()) {
			if (storedFile.isFile()) {
				slf4jlogger.debug("makeReliableStoredFilePathWithFoldersCreated(): Deleting pre-existing file for same SOPInstanceUID");
				storedFile.delete();		// prior to rename of temporary file, in case might cause renameTo() fail
			}
			else {
				slf4jlogger.debug("makeReliableStoredFilePathWithFoldersCreated(): use an alternative file name, since {} already used as other than a file (presumably a directory)",storedFile);
				storedFile = makeAlternativeStoredFilePath(savedInstancesFolder,alternativeSubfolder,sopInstanceUID);
			}
		}
		File parentOfStoredFile = storedFile.getParentFile();
		if (parentOfStoredFile != null) {
			if (parentOfStoredFile.exists()) {
				if (!parentOfStoredFile.isDirectory()) {
					slf4jlogger.debug("makeReliableStoredFilePathWithFoldersCreated(): use an alternative file name, since {} already used as a something other than a directory (presumably a file)",storedFile);
					storedFile = makeAlternativeStoredFilePath(savedInstancesFolder,alternativeSubfolder,sopInstanceUID);
				}
			}
			else {
				parentOfStoredFile.mkdirs();
				if (!parentOfStoredFile.isDirectory()) {
					slf4jlogger.debug("makeReliableStoredFilePathWithFoldersCreated(): use an alternative file name, since cannot make parent directories {} for some (unanticipated) reason",parentOfStoredFile);
					storedFile = makeAlternativeStoredFilePath(savedInstancesFolder,alternativeSubfolder,sopInstanceUID);
					parentOfStoredFile = storedFile.getParentFile();
					if (parentOfStoredFile != null) {
						parentOfStoredFile.mkdirs();
					}
				}
			}
		}
		return storedFile;
	}
	
	protected static String defaultAlternativeSubfolder = "ALTERNATIVE";

	/**
	 * <p>Generate a path to where to store a file based on its SOP Instance UID and assure its reliability.</p>
	 *
	 * <p>Includes creating any necessary parent folders if they do not already exist,
	 * and using an alternate path or name (in a default alternative sub-folder) if a desired file name already exists as something else (such as a folder).</p>
	 *
	 * @param	savedInstancesFolder	the folder in which to save the instance
	 * @param	sopInstanceUID			the SOP Instance UID of the instance to be saved
	 * @return							the path to the file in the specified folder, which may contain nested sub-folders
	 */
	public File makeReliableStoredFilePathWithFoldersCreated(File savedInstancesFolder,String sopInstanceUID) {
		return makeReliableStoredFilePathWithFoldersCreated(savedInstancesFolder,defaultAlternativeSubfolder,sopInstanceUID);
	}
	
	/**
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging
	 * @param	debugLevel	ignored
	 */
	public void setDebugLevel(int debugLevel) {
		slf4jlogger.warn("setDebugLevel(): Debug level ignored");
	}
	
	protected void test(String arg[]) {
		System.err.println(this);
		String testUIDs[] = { "1.2.3.4.5", "123.456.789", "123", "123.456", "5", ".1..2...3..." };
		if (arg.length != 0) {
		    testUIDs = arg;
		}
		File dir = new File("savedInstancesHome");
		for (int tu = 0; tu < testUIDs.length; tu++) {
			System.err.println("uid: " + testUIDs[tu] + "   file path: " + makeStoredFilePath(dir,testUIDs[tu]));
		}
	}
}
		

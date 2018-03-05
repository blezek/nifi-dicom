/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.utils.FileUtilities;

import java.io.File; 
import java.io.IOException; 
import java.util.Properties; 

/**
 * <p>This class provides common support to applications requiring properties related to database services.</p>
 *
 * <p>The following properties are supported:</p>
 *
 * <p><code>Application.DatabaseFileName</code> - where to save the database files</p>
 * <p><code>Application.SavedImagesFolderName</code> - where to save incoming images referenced by the database</p>
 * <p><code>Application.DatabaseServerName</code> - name to use for external TCP access to database (such a server will not be started if this property is absent)</p>
 *
 * @author	dclunie
 */
public class DatabaseApplicationProperties {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseApplicationProperties.java,v 1.10 2017/01/24 10:50:34 dclunie Exp $";

	private static final String defaultDatabaseFileName  = ".com.pixelmed.display.DicomImageViewer.database";
	private static final String defaultSavedImagesFolderName  = ".com.pixelmed.display.DicomImageViewer.images";
	private static final String defaultDatabaseServerName  = null;	// i.e., do not start external access server

	public static final String propertyName_DatabaseFileName = "Application.DatabaseFileName";
	public static final String propertyName_SavedImagesFolderName = "Application.SavedImagesFolderName";
	public static final String propertyName_DatabaseServerName = "Application.DatabaseServerName";
	
	private String dataBaseFileName = defaultDatabaseFileName;
	private String savedImagesFolderName = defaultSavedImagesFolderName;
	private String databaseServerName = defaultDatabaseServerName;

	/**
	 * <p>Extract the DICOM network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public DatabaseApplicationProperties(Properties properties) {
		dataBaseFileName=properties.getProperty(propertyName_DatabaseFileName);
		if (dataBaseFileName == null) {
			dataBaseFileName=defaultDatabaseFileName;
		}
		savedImagesFolderName=properties.getProperty(propertyName_SavedImagesFolderName);
		if (savedImagesFolderName == null) {
			savedImagesFolderName=defaultSavedImagesFolderName;
		}
		databaseServerName=properties.getProperty(propertyName_DatabaseServerName);
		if (databaseServerName == null) {
			databaseServerName=defaultDatabaseServerName;
		}
	}
	
	/**
	 * <p>Return the database file name.</p>
	 *
	 * @return	the database file name
	 */
	public String getDatabaseFileName() { return dataBaseFileName; }
	
	/**
	 * <p>Return the saved images folder name.</p>
	 *
	 * @return	the saved images folder name
	 */
	public String getSavedImagesFolderName() { return savedImagesFolderName; }
	
	/**
	 * <p>Return the saved images folder, creating it if necessary.</p>
	 *
	 * <p>If not an absolute path, will be sought or created relative to the current user's home directory.</p>
	 *
	 * @return	the saved images folder
	 */
	public File getSavedImagesFolderCreatingItIfNecessary() throws IOException {
//System.err.println("DatabaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary(): requesting savedImagesFolderName = "+savedImagesFolderName);
		File savedImagesFolder = new File(savedImagesFolderName);
		if (savedImagesFolder.isAbsolute()) {
			if (!savedImagesFolder.isDirectory() && !savedImagesFolder.mkdirs()) {
				throw new IOException("Cannot find or create absolute path "+savedImagesFolder);
			}
		}
		else {
			savedImagesFolder = new File(FileUtilities.makePathToFileInUsersHomeDirectory(savedImagesFolderName));
			if (!savedImagesFolder.isDirectory() && !savedImagesFolder.mkdirs()) {
				throw new IOException("Cannot find or create home directory relative path "+savedImagesFolder);
			}
		}
//System.err.println("DatabaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary(): using savedImagesFolder = "+savedImagesFolder);
		return savedImagesFolder;
	}

	/**
	 * <p>Return the database server name for external access.</p>
	 *
	 * @return	the database server name
	 */
	public String getDatabaseServerName() { return databaseServerName; }
}


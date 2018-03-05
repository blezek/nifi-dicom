/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseInformationModel;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.SOPClass;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class copies a set of DICOM files, if they match specified criteria.</p>
 *
 * @author	dclunie
 */
public class FindAndCopySelectedDicomFilesUsingDatabase {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/FindAndCopySelectedDicomFilesUsingDatabase.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FindAndCopySelectedDicomFilesUsingDatabase.class);
	
	public FindAndCopySelectedDicomFilesUsingDatabase(DatabaseInformationModel databaseInformationModel,Set<String> sopClasses,String outputPath) {
		String filenameColumnKey = databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE);
		for (String sopClass : sopClasses) {
			slf4jlogger.info("Doing SOP Class {}",sopClass);
			try {
				ArrayList<TreeMap<String,String>> records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(InformationEntity.INSTANCE,"SOPCLASSUID",sopClass);
				for (TreeMap<String,String> record : records) {
					String srcfilename = record.get(filenameColumnKey);
					if (srcfilename != null && srcfilename.length() > 0) {
						String sopInstanceUID = record.get("SOPINSTANCEUID");
						if (sopInstanceUID != null && sopInstanceUID.length() > 0) {
							File outputFile = new File(outputPath,sopInstanceUID+".dcm");
							slf4jlogger.info("Copying srcfilename \"{}\" to \"{}\"",srcfilename,outputFile);
							CopyStream.copy(new File(srcfilename),outputFile);
						}
						else {
							slf4jlogger.warn("Cannot extract SOP Instance UID for \"{}\" to create output file name - ignoring",srcfilename);
						}
					}
					else {
						slf4jlogger.warn("Record missing filename - ignoring");
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			catch (IOException e) {
				slf4jlogger.error("",e);
			}
		}
	}
		
	/**
	 * <p>Copy a set of DICOM files, if they match specified criteria.</p>
	 *
	 * <p>Does not actually check the Modality value in the file, but matches the SOP Class against what is returned from {@link com.pixelmed.dicom.SOPClass#getPlausibleStandardSOPClassUIDsForModality(String) SOPClass.getPlausibleStandardSOPClassUIDsForModality(String)}.</p>
	 *
	 * @param	arg	array of four strings - the class name of the database model, the fully qualified path of the database file prefix, the output path, and the SOP Class UID or Modality
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 4) {
				String databaseModelClassName = arg[0];
				String databaseFileName = arg[1];
		
				if (databaseModelClassName.indexOf('.') == -1) {					// not already fully qualified
					databaseModelClassName="com.pixelmed.database."+databaseModelClassName;
				}
//System.err.println("Class name = "+databaseModelClassName);

				//DatabaseInformationModel databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(makePathToFileInUsersHomeDirectory(dataBaseFileName));
				DatabaseInformationModel databaseInformationModel = null;
				try {
					Class classToUse = Thread.currentThread().getContextClassLoader().loadClass(databaseModelClassName);
					Class[] parameterTypes = { databaseFileName.getClass() };
					Constructor constructorToUse = classToUse.getConstructor(parameterTypes);
					Object[] args = { databaseFileName };
					databaseInformationModel = (DatabaseInformationModel)(constructorToUse.newInstance(args));
				}
				catch (Exception e) {
					slf4jlogger.error("",e);
					System.exit(0);
				}
				
				String requestedSOPClass = arg[3];
				String outputPath = arg[2];

				Set<String> sopClasses = new HashSet<String>();
				if (requestedSOPClass.startsWith("1")) {
//System.err.println("main(): importer.sopClasses.add = \""+arg[2]+"\"");
					sopClasses.add(requestedSOPClass);
				}
				else {
					for (String sopClass : SOPClass.getPlausibleStandardSOPClassUIDsForModality(requestedSOPClass)) {
//System.err.println("main(): importer.sopClasses.add = \""+sopClass+"\"");
						sopClasses.add(sopClass);
					}
				}
				new FindAndCopySelectedDicomFilesUsingDatabase(databaseInformationModel,sopClasses,outputPath);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.FindAndCopySelectedDicomFilesUsingDatabase databaseModelClassName databaseFileName dstdir sopclass|modality");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}



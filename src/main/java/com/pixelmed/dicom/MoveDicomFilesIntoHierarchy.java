/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * <p>This class provides a main method that recursively searches the supplied paths for DICOM files
 * and moves them into a folder hierarchy based on their attributes.</p>
 *
 * <p>Various static utility methods that assist in this operation are also provided, such as to
 * create the hierarchical path name from the attributes, etc., since these may be useful in their
 * own right.</p>
 *
 * @author	dclunie
 */
public class MoveDicomFilesIntoHierarchy {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/MoveDicomFilesIntoHierarchy.java,v 1.16 2017/01/24 10:50:37 dclunie Exp $";

	static protected String defaultHierarchicalFolderName = "Sorted";
	static protected String defaultDuplicatesFolderNamePrefix = "Duplicates";

	static protected void processFilesRecursively(File file,String suffix) throws SecurityException, IOException, DicomException, NoSuchAlgorithmException {
		if (file != null && file.exists()) {
			if (file.isFile() && (suffix == null || suffix.length() == 0 || file.getName().endsWith(suffix))) {
				renameFileWithHierarchicalPathFromAttributes(file);
			}
			else if (file.isDirectory()) {
				{
					File[] filesAndDirectories = file.listFiles();
					if (filesAndDirectories != null && filesAndDirectories.length > 0) {
						for (int i=0; i<filesAndDirectories.length; ++i) {
							processFilesRecursively(filesAndDirectories[i],suffix);
						}
					}
				}
			}
			// else what else could it be
		}
	}
	
	/**
	 * <p>Create a folder structure based on the DICOM attributes of the form:</p>
	 *
	 * <pre>PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the SOPInstanceUID is missing, an empty String is returned.</p>
	 *
	 * @param	list	list of attributes
	 * @return			the folder structure as a path
	 */
	static public String makeHierarchicalPathFromAttributes(AttributeList list) {
		String hierarchicalPathName = "";
		{
			String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID).replaceAll("[^0-9.]","").trim();
			if (sopInstanceUID.length() > 0) {
				String patientID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientID)
					.replaceAll("[^A-Za-z0-9 -]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]"," ");
				if (patientID.length() == 0) { patientID = "NOID"; }
				
				String patientName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientName)
					.replaceAll("[^A-Za-z0-9 ^=,.-]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]"," ").replaceAll("^[.]","_");
				if (patientName.length() == 0) { patientName = "NONAME"; }
				
				String studyDate = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate).replaceAll("[^0-9]","").trim();
				if (studyDate.length() == 0) { studyDate = "19000101"; }
				while (studyDate.length() < 8) { studyDate = studyDate + "0"; }
				
				String studyTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime).replaceFirst("[.].*$","").replaceAll("[^0-9]","");
				while (studyTime.length() < 6) { studyTime = studyTime + "0"; }
				
				String studyID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyID)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");
				
				String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");

				String seriesNumber = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).replaceAll("[^0-9]","");
				while (seriesNumber.length() < 3) { seriesNumber = "0" + seriesNumber; }
				
				String seriesDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDescription)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");
				
				String modality = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ")
					.toUpperCase(java.util.Locale.US);
								
				String studyLabel = "";
				if (studyID.length() == 0) {
					if (studyDescription.length() == 0) {
						studyLabel = studyDate + " " + studyTime;
					}
					else {
						studyLabel = studyDate + " " + studyTime + " [ - " + studyDescription + "]";
					}
				}
				else {
					if (studyDescription.length() == 0) {
						studyLabel = studyDate + " " + studyTime + " [" + studyID + "]";
					}
					else {
						studyLabel = studyDate + " " + studyTime + " [" + studyID + " - " + studyDescription + "]";
					}
				}
				
				String seriesLabel = "";
				if (modality.length() == 0) {
					if (seriesDescription.length() == 0) {
						seriesLabel = "Series " + seriesNumber + " []";
					}
					else {
						seriesLabel = "Series " + seriesNumber + " [ - " + seriesDescription + "]";
					}
				}
				else {
					if (seriesDescription.length() == 0) {
						seriesLabel = "Series " + seriesNumber + " [" + modality + "]";
					}
					else {
						seriesLabel = "Series " + seriesNumber + " [" + modality + " - " + seriesDescription + "]";
					}
				}
				
				hierarchicalPathName =
					  patientName + " [" + patientID + "]"
					+ "/" + studyLabel
					+ "/" + seriesLabel
					+ "/" + sopInstanceUID + ".dcm";
//System.err.println(hierarchicalPathName);
			}
		}
		return hierarchicalPathName;
	}
	
	/**
	 * <p>Rename a DICOM file into a folder hierarchy based on its attributes that are already available.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>hierarchicalFolderName/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and
	 * the duplicate is moved into a separate duplicatesFolderNamePrefix_n folder.</p>
	 *
	 * @param	file						the DICOM file
	 * @param	list						the attributes of the file (already read in)
	 * @param	hierarchicalFolderName		where to store the renamed file
	 * @param	duplicatesFolderNamePrefix	where to store the renamed file if it is a non-identical duplicate of the existing file
	 * @return								the path to the new file if successful, null if not
	 * @throws	IOException					if an error occurs renaming the files
	 * @throws	DicomException				if there is an error parsing the attribute list
	 * @throws	NoSuchAlgorithmException	if there is an error checking duplicate files contain identical content caused by absence of a hash algorithm
	 */
	static public String renameFileWithHierarchicalPathFromAttributes(File file,AttributeList list,String hierarchicalFolderName,String duplicatesFolderNamePrefix)
			throws IOException, DicomException, NoSuchAlgorithmException {
		boolean success = false;
		File newFile = null;
		{
			String newFileName = makeHierarchicalPathFromAttributes(list);
			if (newFileName.length() > 0) {
				newFile = new File(hierarchicalFolderName,newFileName);
				if (file.getCanonicalPath().equals(newFile.getCanonicalPath())) {		// Note that file.equals(newFile) is NOT sufficient, and if used will lead to deletion when hash values match below
					System.err.println("\""+file+"\": source and destination same - doing nothing");
				}
				else {
					int duplicateCount=0;
					boolean proceed = false;
					boolean skipMove = false;
					while (!proceed) {
						File newParentDirectory = newFile.getParentFile();
						if (newParentDirectory != null && !newParentDirectory.exists()) {
							if (!newParentDirectory.mkdirs()) {
								System.err.println("\""+file+"\": parent directory creation failed for \""+newFile+"\"");
								// don't suppress move; might still succeed
							}
						}
						if (newFile.exists()) {
							if (FileUtilities.md5(file.getCanonicalPath()).equals(FileUtilities.md5(newFile.getCanonicalPath()))) {
								System.err.println("\""+file+"\": destination exists and is identical - not overwriting - removing original \""+newFile+"\"");
								if (!file.delete()) {
									System.err.println("\""+file+"\": deletion of duplicate original unsuccessful");
								}
								skipMove=true;
								proceed=true;
							}
							else {
								System.err.println("\""+file+"\": destination exists and is different - not overwriting - move duplicate elsewhere \""+newFile+"\"");
								boolean foundNewHome = false;
								newFile = new File(duplicatesFolderNamePrefix+"_"+Integer.toString(++duplicateCount),newFileName);
								// loop around rather than proceed
							}
						}
						else {
							proceed=true;
						}
					}
					if (!skipMove) {
						if (file.renameTo(newFile)) {
							success = true;
							System.err.println("\""+file+"\" moved to \""+newFile+"\"");
						}
						else {
							System.err.println("\""+file+"\": move attempt failed to \""+newFile+"\"");
						}
					}
				}
			}
			else {
				System.err.println("\""+file+"\": no SOP Instance UID - doing nothing");
			}
		}
		return success ? (newFile == null ? null : newFile.getCanonicalPath()) : null;
	}
	
	protected static class OurReadTerminationStrategy implements AttributeList.ReadTerminationStrategy {
		public boolean terminate(AttributeList attributeList,AttributeTag tag,long byteOffset) {
			return tag.getGroup() > 0x0020;
		}
	}
	
	protected final static AttributeList.ReadTerminationStrategy terminateAfterRelationshipGroup = new OurReadTerminationStrategy();
	
	/**
	 * <p>Rename a DICOM file into a folder hierarchy based on its attributes.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>hierarchicalFolderName/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and
	 * the duplicate is moved into a separate duplicatesFolderNamePrefix_n folder.</p>
	 *
	 * @param	file						the DICOM file
	 * @param	hierarchicalFolderName		where to store the renamed file
	 * @param	duplicatesFolderNamePrefix	where to store the renamed file if it is a non-identical duplicate of the existing file
	 * @return								the path to the new file if successful, null if not (e.g., if not a DICOM file)
	 * @throws	IOException					if an error occurs renaming the files
	 * @throws	DicomException				if there is an error parsing the attribute list
	 * @throws	NoSuchAlgorithmException	if there is an error checking duplicate files contain identical content caused by absence of a hash algorithm
	 */
	static public String renameFileWithHierarchicalPathFromAttributes(File file,String hierarchicalFolderName,String duplicatesFolderNamePrefix) throws IOException, DicomException, NoSuchAlgorithmException {
		String newFileName = null;
		if (DicomFileUtilities.isDicomOrAcrNemaFile(file)) {
			AttributeList list = new AttributeList();
			list.read(file,terminateAfterRelationshipGroup);
			newFileName = renameFileWithHierarchicalPathFromAttributes(file,list,hierarchicalFolderName,duplicatesFolderNamePrefix);
		}
		else {
			System.err.println("\""+file+"\": not a DICOM file - doing nothing");
		}
		return newFileName;
	}

	/**
	 * <p>Rename a DICOM file into a folder hierarchy based on its attributes.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>Sorted/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and
	 * the duplicate is moved into a separate Duplicates_n folder.</p>
	 *
	 * @param	file						the DICOM file
	 * @return								the path to the new file if successful, null if not (e.g., if not a DICOM file)
	 * @throws	IOException					if an error occurs renaming the files
	 * @throws	DicomException				if there is an error parsing the attribute list
	 * @throws	NoSuchAlgorithmException	if there is an error checking duplicate files contain identical content caused by absence of a hash algorithm
	 */
	static public String renameFileWithHierarchicalPathFromAttributes(File file) throws IOException, DicomException, NoSuchAlgorithmException {
		return renameFileWithHierarchicalPathFromAttributes(file,defaultHierarchicalFolderName,defaultDuplicatesFolderNamePrefix);
	}
	
	/**
	 * <p>Rename a DICOM file into a folder hierarchy based on its attributes.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>Sorted/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and
	 * the duplicate is moved into a separate Duplicates_n folder.</p>
	 *
	 * @param	fileName					the DICOM file
	 * @return								the path to the new file if successful, null if not (e.g., if not a DICOM file)
	 * @throws	IOException					if an error occurs renaming the files
	 * @throws	DicomException				if there is an error parsing the attribute list
	 * @throws	NoSuchAlgorithmException	if there is an error checking duplicate files contain identical content caused by absence of a hash algorithm
	 */
	static public String renameFileWithHierarchicalPathFromAttributes(String fileName) throws IOException, DicomException, NoSuchAlgorithmException {
		return renameFileWithHierarchicalPathFromAttributes(new File(fileName),defaultHierarchicalFolderName,defaultDuplicatesFolderNamePrefix);
	}
	
	/**
	 * <p>Recursively search the supplied paths for DICOM files and move them into a folder hierarchy based on their attributes.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>Sorted/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and the duplicate is moved into a separate Duplicates_n folder.</p>
	 *
	 * @param	arg	array of one or more file or directory names
	 */
	public static void main(String[] arg) {
		try {
			for (int i=0; i<arg.length; ++i) {
				processFilesRecursively(new File(arg[i]),null);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}	
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to read a set of DICOM files and replace the Frame of Reference UIDs with a common value for the specified scope.</p>
 *
 * <p>Useful, for example, when Frame of Reference UIDs have been incorrectly changed during de-identification and are inconsistent within a set.</p>
 *
 * @author	dclunie
 */
public class RemapFrameOfReferenceUIDs {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/RemapFrameOfReferenceUIDs.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RemapFrameOfReferenceUIDs.class);
	
	protected String ourAETitle = "OURAETITLE";

	private Map<String,String> frameOfReferenceUIDIndexedByCommonUID = new HashMap<String,String>();
	
	private UIDGenerator u = new UIDGenerator();
	
	private String scope;
	private String srcFolderName;
	private String dstFolderName;
	
	protected class OurMediaImporter extends MediaImporter {
	
		public OurMediaImporter() {
			super(null);
			
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			System.err.println("Doing "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				String commonUID = null;
				if (scope.equals("STUDY")) {
					commonUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
				}
				else if (scope.equals("SERIES")) {
					commonUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
				}
				
				if (commonUID == null || commonUID.length() == 0) {
					throw new DicomException("\""+mediaFileName+"\": cannot get common UID - wrong scope "+scope+" specified perhaps ?");
				}
				
				String existingFrameofReferenceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.FrameOfReferenceUID);
				
				String useFrameofReferenceUIDForScope = frameOfReferenceUIDIndexedByCommonUID.get(commonUID);
				if (useFrameofReferenceUIDForScope == null) {
					if (existingFrameofReferenceUID == null || existingFrameofReferenceUID.length() == 0) {
						useFrameofReferenceUIDForScope = u.getAnotherNewUID();
					}
					else {
						useFrameofReferenceUIDForScope = existingFrameofReferenceUID;
					}
					frameOfReferenceUIDIndexedByCommonUID.put(commonUID,useFrameofReferenceUIDForScope);
				}
				
				if (!useFrameofReferenceUIDForScope.equals(existingFrameofReferenceUID)) {
					slf4jlogger.info("\"{}\": replacing old FoRUID {} with common {} for {} {}",mediaFileName,existingFrameofReferenceUID,useFrameofReferenceUIDForScope,scope,commonUID);
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.FrameOfReferenceUID); a.addValue(useFrameofReferenceUIDForScope); list.put(a); }
				}
				
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.RemapFrameOfReferenceUIDs",									// Manufacturer's Model Name
					null,															// Device Serial Number
					"Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
					"Remapped UIDs");
								
				list.removeGroupLengthAttributes();
				list.removeMetaInformationHeaderAttributes();
				list.remove(TagFromName.DataSetTrailingPadding);
				FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);

				//File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list));
				File dstFile = FileUtilities.makeSameRelativePathNameInDifferentFolder(srcFolderName,dstFolderName,mediaFileName);
//System.err.println("\""+mediaFileName+"\": dstFile =  "+dstFile);
				if (dstFile.exists()) {
					throw new DicomException("\""+mediaFileName+"\": new file \""+dstFile+"\" already exists - not overwriting");
				}
				else {
					File dstParentDirectory = dstFile.getParentFile();
					if (!dstParentDirectory.exists()) {
						if (!dstParentDirectory.mkdirs()) {
							throw new DicomException("\""+mediaFileName+"\": parent directory creation failed for \""+dstFile+"\"");
						}
					}
					list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}

	/**
	 * <p>Read a set of DICOM files and replace the Frame of Reference UIDs with a common value for the specified scope.</p>
	 *
	 * <p>Uses the same sub-folder and file names in the destination folder as supplied in the source folder.</p>
	 *
	 * @param	scope
	 * @param	srcFolderName
	 * @param	dstFolderName
	 */
	public RemapFrameOfReferenceUIDs(String scope,String srcFolderName,String dstFolderName) throws FileNotFoundException, IOException, DicomException {
		this.scope = scope.toUpperCase();
		this.srcFolderName = srcFolderName;
		this.dstFolderName = dstFolderName;
		OurMediaImporter importer = new OurMediaImporter();
		importer.importDicomFiles(srcFolderName);
	}
	
	/**
	 * <p>Read a set of DICOM files and replace the Frame of Reference UIDs with a common value for the specified scope.</p>
	 *
	 * <p>Uses the same sub-folder and file names in the destination folder as supplied in the source folder.</p>
	 *
	 * @param	arg		[SERIES|STUDY] srcFolderName dstFolderName
	 */
	public static void main(String arg[]) {
		try {
			new RemapFrameOfReferenceUIDs(arg[0],arg[1],arg[2]);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


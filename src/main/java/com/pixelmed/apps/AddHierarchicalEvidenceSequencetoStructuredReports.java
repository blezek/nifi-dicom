/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.HierarchicalSOPInstanceReference;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UniqueIdentifierAttribute;

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
 * <p>A class to process multiple SR files and their referenced instances (like Images, Presentation States and Segmentations)
 * and build Hierarchical SOP Instance Reference Macros with which to (re-)populate CurrentRequestedProcedureEvidenceSequence.</p>
 *
 * @author	dclunie
 */
public class AddHierarchicalEvidenceSequencetoStructuredReports {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/AddHierarchicalEvidenceSequencetoStructuredReports.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AddHierarchicalEvidenceSequencetoStructuredReports.class);
	
	protected String ourAETitle = "OURAETITLE";

	private Set<String> srFileNamesToCopy = new HashSet<String>();
	
	private Map<String,String> mapOfSOPInstanceUIDToSOPClassUID         = new HashMap<String,String>();
	private Map<String,String> mapOfSOPInstanceUIDToSeriesInstanceUID   = new HashMap<String,String>();
	private Map<String,String> mapOfSOPInstanceUIDToStudyInstanceUID    = new HashMap<String,String>();

	//private Set<String> srSOPClassInstances    = new HashSet<String>();
	//private Set<String> otherSOPClassInstances = new HashSet<String>();

	//private Map<String,SetOfDicomFiles.DicomFile> mapOfSOPInstanceUIDToDicomFile = new HashMap<String,SetOfDicomFiles.DicomFile>();
	
	//private Map<String,Set<String>> mapOfSRSOPInstanceUIDToPredecessorSRSOPInstanceUIDs = new HashMap<String,Set<String>>();
	
	private Map<String,Set<String>> mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced = new HashMap<String,Set<String>>();

	private String dstFolderName;
	
	// this will include references to other SRs, which does not worry us because we will check later
	private void extractAllSOPInstancesReferencedWithinSR(AttributeList list,String sopInstanceUID) {
		Set<String> setOfReferencedSOPInstanceUIDs = mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced.get(sopInstanceUID);
		if (setOfReferencedSOPInstanceUIDs == null) {
			setOfReferencedSOPInstanceUIDs = new HashSet<String>();
			mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced.put(sopInstanceUID,setOfReferencedSOPInstanceUIDs);
		}
		list.findAllNestedReferencedSOPInstanceUIDs(setOfReferencedSOPInstanceUIDs);
//System.err.println("extractAllSOPInstancesReferencedWithinSR(): setOfReferencedSOPInstanceUIDs now = "+setOfReferencedSOPInstanceUIDs);
	}
	
	protected class OurMediaImporter extends MediaImporter {
	
		public OurMediaImporter() {
			super(null);
			
		}
	
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
//System.err.println("Doing "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i,TagFromName.PixelData);
				i.close();
				
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (SOPClass.isStructuredReport(sopClassUID)) {
//System.err.println("Is an SR "+sopInstanceUID+" in file "+mediaFileName);
					srFileNamesToCopy.add(mediaFileName);
					extractAllSOPInstancesReferencedWithinSR(list,sopInstanceUID);	// populates mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced
				}
				else {
					String seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
					String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);

					{
						String existingSOPClassUID = mapOfSOPInstanceUIDToSOPClassUID.get(sopInstanceUID);
						if (existingSOPClassUID == null) {
							mapOfSOPInstanceUIDToSOPClassUID.put(sopInstanceUID,sopClassUID);
						}
						else if (!existingSOPClassUID.equals(sopClassUID)) {
							slf4jlogger.error("File {} SOP Instance UID {} contains different SOPClassUID {} than in current file {} - ignoring it",mediaFileName,sopInstanceUID,existingSOPClassUID,sopClassUID);
						}
					}
					{
						String existingSeriesInstanceUID = mapOfSOPInstanceUIDToSeriesInstanceUID.get(sopInstanceUID);
						if (existingSeriesInstanceUID == null) {
							mapOfSOPInstanceUIDToSeriesInstanceUID.put(sopInstanceUID,seriesInstanceUID);
						}
						else if (!existingSeriesInstanceUID.equals(seriesInstanceUID)) {
							slf4jlogger.error("File {} SOP Instance UID {} contains different SeriesInstanceUID {} than in current file {} - ignoring it",mediaFileName,sopInstanceUID,existingSeriesInstanceUID,seriesInstanceUID);
						}
					}
					{
						String existingStudyInstanceUID = mapOfSOPInstanceUIDToStudyInstanceUID.get(sopInstanceUID);
						if (existingStudyInstanceUID == null) {
							mapOfSOPInstanceUIDToStudyInstanceUID.put(sopInstanceUID,studyInstanceUID);
						}
						else if (!existingStudyInstanceUID.equals(studyInstanceUID)) {
							slf4jlogger.error("File {} SOP Instance UID {} contains different StudyInstanceUID {} than in current file {} - ignoring it",mediaFileName,sopInstanceUID,existingStudyInstanceUID,studyInstanceUID);
						}
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}

	/**
	 * <p>Copy only the most recent SR files and their references.</p>
	 *
	 * @param	srcs
	 * @param	dstFolderName
	 */
	public AddHierarchicalEvidenceSequencetoStructuredReports(String[] srcs,String dstFolderName) throws FileNotFoundException, IOException, DicomException {
		this.dstFolderName = dstFolderName;
		
		// 1st stage ... read all the files  ...
		OurMediaImporter importer = new OurMediaImporter();
		for (String src : srcs) {
			importer.importDicomFiles(src);
		}
		
		// 2nd stage ... copy only the files to be retained ...
		for (String srcFileName : srFileNamesToCopy) {
			try {
				DicomInputStream i = new DicomInputStream(new File(srcFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				// add CurrentRequestedProcedureEvidenceSequence
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				Set<String> allSOPInstancesReferenced = mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced.get(sopInstanceUID);
				if (allSOPInstancesReferenced != null && allSOPInstancesReferenced.size() > 0) {
					SequenceAttribute aCurrentRequestedProcedureEvidenceSequence = new SequenceAttribute(TagFromName.CurrentRequestedProcedureEvidenceSequence);
					list.put(aCurrentRequestedProcedureEvidenceSequence);	// overwrite the one already there, if present
					
					Map<String,Set<String>> mapOfStudyInstanceUIDToSetOfSeriesInstanceUID = new HashMap<String,Set<String>>();
					Map<String,Set<HierarchicalSOPInstanceReference>> mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference = new HashMap<String,Set<HierarchicalSOPInstanceReference>>();
					for (String referencedSOPInstanceUID : allSOPInstancesReferenced) {
						String referencedSOPClassUID       = mapOfSOPInstanceUIDToSOPClassUID.get(referencedSOPInstanceUID);
						String referencedSeriesInstanceUID = mapOfSOPInstanceUIDToSeriesInstanceUID.get(referencedSOPInstanceUID);
						String referencedStudyInstanceUID  = mapOfSOPInstanceUIDToStudyInstanceUID.get(referencedSOPInstanceUID);
						
						if (referencedSOPInstanceUID    != null && referencedSOPInstanceUID.length() > 0
						 && referencedSOPClassUID       != null && referencedSOPClassUID.length() > 0
						 && referencedSeriesInstanceUID != null && referencedSeriesInstanceUID.length() > 0
						 && referencedStudyInstanceUID  != null && referencedStudyInstanceUID.length() > 0) {
						
							HierarchicalSOPInstanceReference instanceReference = new HierarchicalSOPInstanceReference(referencedStudyInstanceUID,referencedSeriesInstanceUID,referencedSOPInstanceUID,referencedSOPClassUID);
						
							Set<String> seriesInStudy = mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.get(referencedStudyInstanceUID);
							if (seriesInStudy == null) {
								seriesInStudy = new HashSet<String>();
								mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.put(referencedStudyInstanceUID,seriesInStudy);
							}
							seriesInStudy.add(referencedSeriesInstanceUID);
						
							Set<HierarchicalSOPInstanceReference> instanceReferencesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
							if (instanceReferencesInSeries == null) {
								instanceReferencesInSeries = new HashSet<HierarchicalSOPInstanceReference>();
								mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.put(referencedSeriesInstanceUID,instanceReferencesInSeries);
							}
							instanceReferencesInSeries.add(instanceReference);
						}
						else {
							slf4jlogger.warn("Cannot find hierarchical information for reference to SOP Instance UID {} (referenced instance not amongst supplied files)",referencedSOPInstanceUID);
						}
					}
					
					for (String referencedStudyInstanceUID : mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.keySet()) {
						slf4jlogger.info("STUDY {}",referencedStudyInstanceUID);
						AttributeList referencedStudyList = new AttributeList();
						aCurrentRequestedProcedureEvidenceSequence.addItem(referencedStudyList);
						{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(referencedStudyInstanceUID); referencedStudyList.put(a); }
						SequenceAttribute aReferencedSeriesSequence = new SequenceAttribute(TagFromName.ReferencedSeriesSequence);
						referencedStudyList.put(aReferencedSeriesSequence);

						Set<String> seriesInStudy = mapOfStudyInstanceUIDToSetOfSeriesInstanceUID.get(referencedStudyInstanceUID);
						for (String referencedSeriesInstanceUID : seriesInStudy) {
							slf4jlogger.info("\tSERIES {}",referencedSeriesInstanceUID);
							AttributeList referencedSeriesList = new AttributeList();
							aReferencedSeriesSequence.addItem(referencedSeriesList);
							{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(referencedSeriesInstanceUID); referencedSeriesList.put(a); }
							SequenceAttribute aReferencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);
							referencedSeriesList.put(aReferencedSOPSequence);

							Set<HierarchicalSOPInstanceReference> instancesInSeries = mapOfSeriesInstanceUIDToSetOfHierarchicalSOPInstanceReference.get(referencedSeriesInstanceUID);
							for (HierarchicalSOPInstanceReference instanceReference : instancesInSeries) {
								AttributeList referencedSOPList = new AttributeList();
								aReferencedSOPSequence.addItem(referencedSOPList);

								String referencedSOPInstanceUID = instanceReference.getSOPInstanceUID();
								slf4jlogger.info("\t\tINSTANCE SOP Instance {}",referencedSOPInstanceUID);
								{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referencedSOPInstanceUID); referencedSOPList.put(a); }
								String referencedSOPClassUID    = instanceReference.getSOPClassUID();
								slf4jlogger.info("\t\tINSTANCE SOP Class {}",referencedSOPClassUID);
								{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referencedSOPClassUID); referencedSOPList.put(a); }
							}
						}
					}
				
					list.removeGroupLengthAttributes();
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
				
					File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list));
					if (dstFile.exists()) {
						throw new DicomException("\""+srcFileName+"\": new file \""+dstFile+"\" already exists - not overwriting");
					}
					else {
						File dstParentDirectory = dstFile.getParentFile();
						if (!dstParentDirectory.exists()) {
							if (!dstParentDirectory.mkdirs()) {
								throw new DicomException("\""+srcFileName+"\": parent directory creation failed for \""+dstFile+"\"");
							}
						}
						slf4jlogger.info("Copying from \"{}\" to \"{}\"",srcFileName,dstFile);
						list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("File {}",srcFileName,e);
			}
		}
	}
	
	/**
	 * <p>Examine a set of SR and referenced files and copy SR files adding evidence sequence.</p>
	 *
	 * @param	arg		array of 2 or more strings - one or more source folder or DICOMDIR and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 2) {
				int nSrcs = arg.length-1;
				String[] srcs = new String[nSrcs];
				System.arraycopy(arg,0,srcs,0,nSrcs);
				new AddHierarchicalEvidenceSequencetoStructuredReports(srcs,arg[nSrcs]);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.AddHierarchicalEvidenceSequencetoStructuredReports srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


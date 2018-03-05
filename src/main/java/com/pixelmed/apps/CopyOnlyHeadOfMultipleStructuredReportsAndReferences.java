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
 * <p>A class to process multiple SR files and their referenced instances (like Presentation States and Segmentations)
 * and copy only the head (most recent) of the Predecessor Documents sequence chain and its references, ignoring
 * earlier (obsolete) SR files and earlier references.</p>
 *
 * @author	dclunie
 */
public class CopyOnlyHeadOfMultipleStructuredReportsAndReferences {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/CopyOnlyHeadOfMultipleStructuredReportsAndReferences.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DoseReporterWithLegacyOCRAndAutoSendToRegistry.class);
	
	protected String ourAETitle = "OURAETITLE";

	private SetOfDicomFiles dicomFilesRead   = new SetOfDicomFiles();
	private SetOfDicomFiles dicomFilesToCopy = new SetOfDicomFiles();
	
	private Set<String> srSOPClassInstances    = new HashSet<String>();
	private Set<String> otherSOPClassInstances = new HashSet<String>();

	private Map<String,SetOfDicomFiles.DicomFile> mapOfSOPInstanceUIDToDicomFile = new HashMap<String,SetOfDicomFiles.DicomFile>();
	
	private Map<String,Set<String>> mapOfSRSOPInstanceUIDToPredecessorSRSOPInstanceUIDs = new HashMap<String,Set<String>>();
	
	private Map<String,Set<String>> mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced = new HashMap<String,Set<String>>();

	private String dstFolderName;
	
	private void extractPredecessorDocumentsSequence(AttributeList list,String sopInstanceUID) {
		Attribute a = list.get(TagFromName.PredecessorDocumentsSequence);
		if (a != null && a instanceof SequenceAttribute) {
			Set<String> setOfReferencedSOPInstanceUIDs = mapOfSRSOPInstanceUIDToPredecessorSRSOPInstanceUIDs.get(sopInstanceUID);
			if (setOfReferencedSOPInstanceUIDs == null) {
				setOfReferencedSOPInstanceUIDs = new HashSet<String>();
				mapOfSRSOPInstanceUIDToPredecessorSRSOPInstanceUIDs.put(sopInstanceUID,setOfReferencedSOPInstanceUIDs);
			}
			SequenceAttribute aPredecessorDocumentsSequence = (SequenceAttribute)a;
			Iterator<SequenceItem> i = aPredecessorDocumentsSequence.iterator();
			while (i.hasNext()) {
//System.err.println("SOP Instance "+sopInstanceUID+" has an item in PredecessorDocumentsSequence");
				AttributeList itemList = i.next().getAttributeList();
				itemList.findAllNestedReferencedSOPInstanceUIDs(setOfReferencedSOPInstanceUIDs);	// this works because the Hierarchical Macro is is used for the PredecessorDocumentsSequence, and what we want is not at the top level of each item
//System.err.println("extractPredecessorDocumentsSequence(): setOfReferencedSOPInstanceUIDs now = "+setOfReferencedSOPInstanceUIDs);
			}
		}
	}
	
	// hasSuccessor means that UID occurs in somebody else's predecessor chain
	private boolean hasSuccessor(String sopInstanceUID) {
//System.err.println("Checking if SOP Instance "+sopInstanceUID+" has a successor");
		for (Set<String> trySet : mapOfSRSOPInstanceUIDToPredecessorSRSOPInstanceUIDs.values()) {
//System.err.println("Checking set of predecessors "+trySet);
			for (String tryUID : trySet) {
//System.err.println("Checking UID in set "+tryUID);
				if (sopInstanceUID.equals(tryUID)) {
//System.err.println("Has successor - found sopInstanceUID in set "+sopInstanceUID);
					return true;
				}
			}
		}
//System.err.println("Has no successor - did not find sopInstanceUID "+sopInstanceUID);
		return false;
	}
	
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
				SetOfDicomFiles.DicomFile dicomFile = dicomFilesRead.add(mediaFileName,true/*keepList*/,false/*keepPixelData*/);
				AttributeList list = dicomFile.getAttributeList();
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					mapOfSOPInstanceUIDToDicomFile.put(sopInstanceUID,dicomFile);
					
					String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
					if (SOPClass.isStructuredReport(sopClassUID)) {
//System.err.println("Is an SR "+sopInstanceUID+" in file "+mediaFileName);
						srSOPClassInstances.add(sopInstanceUID);
						extractPredecessorDocumentsSequence(list,sopInstanceUID);
						extractAllSOPInstancesReferencedWithinSR(list,sopInstanceUID);
					}
					else {
//System.err.println("Is not an SR "+sopInstanceUID+" in file "+mediaFileName);
						otherSOPClassInstances.add(sopInstanceUID);
					}
				}
				else {
					throw new DicomException("No SOP Instance UID in file "+mediaFileName);
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
	public CopyOnlyHeadOfMultipleStructuredReportsAndReferences(String[] srcs,String dstFolderName) throws FileNotFoundException, IOException, DicomException {
		this.dstFolderName = dstFolderName;
		
		// 1st stage ... read all the files  ...
		OurMediaImporter importer = new OurMediaImporter();
		for (String src : srcs) {
			importer.importDicomFiles(src);
		}
		
		// 2nd stage ... process what was read to find the heads of the predecessor chains ...
		
//System.err.println("Have "+srSOPClassInstances.size()+" SR SOP Class Instances");
		for (String srSOPInstanceUID : srSOPClassInstances) {
			if (hasSuccessor(srSOPInstanceUID)) {
//System.err.println("Has successor - not copying - SR SOP Instance "+srSOPInstanceUID+" in file "+mapOfSOPInstanceUIDToDicomFile.get(srSOPInstanceUID).getFileName());
			}
			else {
				SetOfDicomFiles.DicomFile srDicomFile = mapOfSOPInstanceUIDToDicomFile.get(srSOPInstanceUID);
//System.err.println("Has no successor - will copy - SR SOP Instance "+srSOPInstanceUID+" in file "+srDicomFile.getFileName());
				dicomFilesToCopy.add(srDicomFile);
				Set<String> setOfReferencedSOPInstanceUIDs = mapOfSRSOPInstanceUIDToAllSOPInstancesReferenced.get(srSOPInstanceUID);
				if (setOfReferencedSOPInstanceUIDs != null) {
					for (String referencedSOPInstanceUID : setOfReferencedSOPInstanceUIDs) {
						if (otherSOPClassInstances.contains(referencedSOPInstanceUID)) {
							// i.e., it is not an SR, and we have encountered (read) it
							SetOfDicomFiles.DicomFile referencedDicomFile = mapOfSOPInstanceUIDToDicomFile.get(referencedSOPInstanceUID);
//System.err.println("Is referenced from SR with no successor - will copy - referenced SOP Instance "+referencedSOPInstanceUID+" in file "+referencedDicomFile.getFileName());
							dicomFilesToCopy.add(referencedDicomFile);
						}
					}
				}
			}
		}
		
		// 3rd stage ... copy only the files to be retained ...
		//for (SetOfDicomFiles.DicomFile srcDicomFile : dicomFilesToCopy) {
		{
			Iterator i = dicomFilesToCopy.iterator();
			while (i.hasNext()) {
				SetOfDicomFiles.DicomFile srcDicomFile = (SetOfDicomFiles.DicomFile)i.next();
				try {
					File srcFile = new File(srcDicomFile.getFileName());
					File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(srcDicomFile.getAttributeList()));
					if (dstFile.exists()) {
						throw new DicomException("\""+srcFile+"\": new file \""+dstFile+"\" already exists - not overwriting");
					}
					else {
						File dstParentDirectory = dstFile.getParentFile();
						if (!dstParentDirectory.exists()) {
							if (!dstParentDirectory.mkdirs()) {
								throw new DicomException("\""+srcFile+"\": parent directory creation failed for \""+dstFile+"\"");
							}
						}
						CopyStream.copy(srcFile,dstFile);
					}
				}
				catch (Exception e) {
					slf4jlogger.error("File {}",srcDicomFile.getFileName(),e);
				}
			}
		}
		
		// 4th stage ... summarize the files we did or did not copy ...
		{
			Iterator i = dicomFilesRead.iterator();
			while (i.hasNext()) {
				SetOfDicomFiles.DicomFile srcDicomFile = (SetOfDicomFiles.DicomFile)i.next();
				if (dicomFilesToCopy.contains(srcDicomFile)) {
					slf4jlogger.info("Copied - SOP Instance {} in file {}",srcDicomFile.getSOPInstanceUID(),srcDicomFile.getFileName());
				}
				else {
					slf4jlogger.info("Not copied - SOP Instance {} in file {}",srcDicomFile.getSOPInstanceUID(),srcDicomFile.getFileName());
				}
			}
		}
		
	}
	
	/**
	 * <p>Copy only the most recent SR files and their references.</p>
	 *
	 * @param	arg		array of 2 or more strings - one or more source folder or DICOMDIR and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 2) {
				int nSrcs = arg.length-1;
				String[] srcs = new String[nSrcs];
				System.arraycopy(arg,0,srcs,0,nSrcs);
				new CopyOnlyHeadOfMultipleStructuredReportsAndReferences(srcs,arg[nSrcs]);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.CopyOnlyHeadOfMultipleStructuredReportsAndReferences srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodingSchemeIdentification;
import com.pixelmed.dicom.CompositeInstanceContext;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for merging the patient composite context of multiple instances for consistency.</p>
 *
 * <p>Patient identity is determined by being within the same study (having the same Study Instance UID), or referencing each others SOP Instance UIDs.</p>
 *
 * <p>It is assumed that one patient's instances can only cross-reference those of the same patient
 * and not other patients. If there are no instance cross-references, then no commonality can be established
 * across studies and no contexts are merged across studies.</p>
 *
 * <p>There is no assumption that any particular patient entity level key is the same (e.g., Patient ID).</p>
 *
 * <p>Various known dummy values are treated as if they were zero length or absent if conflicting with non-dummy values.</p>
 *
 * @see com.pixelmed.apps.MergeCompositeContextForOneEntitySelectively
 *
 * @author	dclunie
 */
public class MergeCompositeContext {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/MergeCompositeContext.java,v 1.16 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MergeCompositeContext.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	
	protected int nextSequenceNumber = 0;
	
	public class Group extends TreeSet<String> {
		String identity;
		CompositeInstanceContext context;
		int sequenceNumber;

		Group() {
			super();
			identity = UUID.randomUUID().toString();
			context = null;
			sequenceNumber = nextSequenceNumber++;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Group ");
			buffer.append(identity);
			buffer.append(" seq ");
			buffer.append(sequenceNumber);
			buffer.append(":\n");
			for (String sopInstanceUID : this) {
				buffer.append("\t");
				buffer.append(sopInstanceUID);
				buffer.append("\n");
			}
			buffer.append(context.toString());
			return buffer.toString();
		}
	}
	
	protected Set<Group> groups = new HashSet<Group>();
	
	
	protected String dumpGroups() {
		StringBuffer buffer = new StringBuffer();
		int count = 0;
		for (Group group : groups) {
			buffer.append(group.toString());
		}
		return buffer.toString();
	}

	protected Map<String,String> mapOfSOPInstanceUIDToStudyInstanceUID = new HashMap<String,String>();
	protected Map<String,Group> mapOfSOPInstanceUIDToGroup = new HashMap<String,Group>();
	protected Map<String,Group> mapOfStudyInstanceUIDToGroup = new HashMap<String,Group>();
	
	protected boolean isNonZeroLengthDummyValue(String value) {
		return value.equals("DUMMY");
	}
	
	// should really take steps to also merge all available PatientIDs into OtherPatientIDs, etc. ... :(
	
	protected CompositeInstanceContext mergePatientContext(Group group,CompositeInstanceContext newContext) {
		if (group.context == null) {
//System.err.println("mergePatientContext(): creating new context for group");
			group.context = newContext; 
		}
		else {
			AttributeList groupList = group.context.getAttributeList();
			Iterator<Attribute> newListIterator = newContext.getAttributeList().values().iterator();
			while (newListIterator.hasNext()) {
				Attribute a = newListIterator.next();
				AttributeTag tag = a.getTag();
				String groupValue = Attribute.getSingleStringValueOrEmptyString(groupList,tag);
				String newValue = a.getSingleStringValueOrEmptyString();
				if (!newValue.equals(groupValue)) {
					String describeTag = tag + " " + groupList.getDictionary().getFullNameFromTag(tag);
slf4jlogger.info("mergePatientContext(): in group "+group.identity+" for "+describeTag+" values differ between existing group value <"+groupValue+"> and new value <"+newValue+">");
					if (newValue.length() > 0 && (groupValue.length() == 0 || isNonZeroLengthDummyValue(groupValue))) {
slf4jlogger.info("mergePatientContext(): in group "+group.identity+" for "+describeTag+" replacing absent/empty/dummy existing group value with new value <"+newValue+">");
						groupList.put(a);
					}
				}
			}
		}
		return group.context;
	}
	
	protected void mergeGroups(Group g1,Group g2) {
slf4jlogger.info("mergeGroups(): seq "+g1.identity+" and "+g2.identity);
//System.err.println("mergeGroups(): seq "+g1.sequenceNumber+" and "+g2.sequenceNumber);
		if (g1.sequenceNumber > g2.sequenceNumber) {
//System.err.println("mergeGroups(): swapping before merging into "+g2.sequenceNumber);
			// swap them, since earliest encountered context takes priority
			Group gt = g1;
			g1=g2;
			g2=gt;
		}
		mergePatientContext(g1,g2.context);
		for (String sopInstanceUID : g2) {
			g1.add(sopInstanceUID);
			assert(mapOfSOPInstanceUIDToGroup.get(sopInstanceUID) == g2);
			mapOfSOPInstanceUIDToGroup.put(sopInstanceUID,g1);
		}
		for (String studyInstanceUID : mapOfStudyInstanceUIDToGroup.keySet()) {
			Group g = mapOfStudyInstanceUIDToGroup.get(studyInstanceUID);
			if (g == g2) {
				mapOfStudyInstanceUIDToGroup.put(studyInstanceUID,g1);
			}
		}
		groups.remove(g2);
	}
	
	protected Group addToGroups(AttributeList list) throws DicomException {
		Group group = null;
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
		if (sopInstanceUID.length() > 0) {
//System.err.println("addToGroups(): checking "+sopInstanceUID);
			group = mapOfSOPInstanceUIDToGroup.get(sopInstanceUID);
//System.err.println("addToGroups(): mapOfSOPInstanceUIDToGroup.get() = "+group);
			Set<String> referencedSOPInstanceUIDs = list.findAllNestedReferencedSOPInstanceUIDs();
			if (group == null) {
				// not already there, so before creating a new group ...
				if (studyInstanceUID.length() > 0) {
					group = mapOfStudyInstanceUIDToGroup.get(studyInstanceUID);
//System.err.println("addToGroups(): mapOfStudyInstanceUIDToGroup.get() = "+group);
					if (group == null) {
						// no group with instance in same study, so now try to put it in the same group as any referenced instances
						if (referencedSOPInstanceUIDs != null) {
							for (String referencedSOPInstanceUID : referencedSOPInstanceUIDs) {
								group = mapOfSOPInstanceUIDToGroup.get(referencedSOPInstanceUID);
//System.err.println("addToGroups(): mapOfSOPInstanceUIDToGroup.get() = "+group);
								if (group != null) {
slf4jlogger.info("Adding SOP Instance UID "+sopInstanceUID+" of StudyInstanceUID "+studyInstanceUID+" based on contained Referenced SOP Instance UID "+referencedSOPInstanceUID+" to group "+group.identity);
									break;
								}
							}
						}
					}
					else {
slf4jlogger.info("Adding SOP Instance UID "+sopInstanceUID+" based on StudyInstanceUID "+studyInstanceUID+" to group "+group.identity);
					}
				}
				else {
					throw new DicomException("Missing StudyInstanceUID");
				}

				if (group == null) {				// i.e., no references or did not find any of the references in existing groups
//System.err.println("addToGroups(): creating new group");
					group = new Group();
slf4jlogger.info("Creating new group for SOP Instance UID "+sopInstanceUID+" based on StudyInstanceUID "+studyInstanceUID+" group "+group.identity);
					groups.add(group);
				}
			}

			if (studyInstanceUID.length() > 0) {
				Group studyGroup = mapOfStudyInstanceUIDToGroup.get(studyInstanceUID);
				if (studyGroup == null) {
slf4jlogger.info("addToGroups(): mapOfStudyInstanceUIDToGroup.put(studyInstanceUID "+studyInstanceUID+")");
					mapOfStudyInstanceUIDToGroup.put(studyInstanceUID,group);
				}
				else if (studyGroup != group) {
slf4jlogger.info("addToGroups(): mapOfStudyInstanceUIDToGroup.get(studyInstanceUID "+studyInstanceUID+") != null and != current group");
					mergeGroups(group,studyGroup);
				}
			}
			
			group.add(sopInstanceUID);
			Group instanceGroup = mapOfSOPInstanceUIDToGroup.get(sopInstanceUID);
			if (instanceGroup == null) {
				mapOfSOPInstanceUIDToGroup.put(sopInstanceUID,group);
			}
			else if (instanceGroup != group) {
slf4jlogger.info("addToGroups(): mapOfSOPInstanceUIDToGroup.get(sopInstanceUID "+sopInstanceUID+") != null and != current group");
				mergeGroups(group,instanceGroup);
			}
			
			if (referencedSOPInstanceUIDs != null) {
				//group.addAll(referencedSOPInstanceUIDs);
				for (String referencedSOPInstanceUID : referencedSOPInstanceUIDs) {
					group.add(referencedSOPInstanceUID);
					instanceGroup = mapOfSOPInstanceUIDToGroup.get(referencedSOPInstanceUID);
					if (instanceGroup == null) {
						mapOfSOPInstanceUIDToGroup.put(referencedSOPInstanceUID,group);
					}
					else if (instanceGroup != group) {
slf4jlogger.info("addToGroups(): mapOfSOPInstanceUIDToGroup.get(referencedSOPInstanceUID "+referencedSOPInstanceUID+") != null and != current group");
						mergeGroups(group,instanceGroup);
					}
				}
			}
		}
		else {
			throw new DicomException("Missing SOPInstanceUID");
		}
		return group;
	}
	
	protected class OurFirstPassMediaImporter extends MediaImporter {
		
		public OurFirstPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
					if (studyInstanceUID.length() > 0) {
						mapOfSOPInstanceUIDToStudyInstanceUID.put(sopInstanceUID,studyInstanceUID);
					}
					else {
						throw new DicomException("Missing StudyInstanceUID");
					}

					{
						CompositeInstanceContext cic = new CompositeInstanceContext(list,false/*forSR*/);
						// remove all except patient context ...
						cic.removeStudy();
						cic.removeSeries();
						cic.removeEquipment();
						cic.removeFrameOfReference();
						cic.removeInstance();
						cic.removeSRDocumentGeneral();

						Group group = addToGroups(list);
//System.err.println("group = "+group);
						mergePatientContext(group,cic);
					}
				}
				else {
					throw new DicomException("Missing SOPInstanceUID");
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	
	protected class OurSecondPassMediaImporter extends MediaImporter {
		public OurSecondPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected Group[] singleGroupArray = new Group[1];
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					Group group = mapOfSOPInstanceUIDToGroup.get(sopInstanceUID);

//System.err.println("group = "+group);
//System.err.println("Groups size = "+groups.size());
					if (group == null) {
						if (groups.size() == 1) {
							group = groups.toArray(singleGroupArray)[0];
						}
						else {
							throw new DicomException("Cannot merge context for second set if more than one group");
						}
					}
					
					if (group != null) {
						logLn("In group "+group.identity);
						if (group.context != null) {
							CompositeInstanceContext.removePatient(list);					// remove anything hanging around, such as empty attributes
							list.putAll(group.context.getAttributeList());					// overwrite all patient context in list that was read in
						}
						else {
							throw new DicomException("Missing group context for SOPInstanceUID on second pass");	// should not be possible
						}
						
						ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
							"PixelMed",														// Manufacturer
							"PixelMed",														// Institution Name
							"Software Development",											// Institutional Department Name
							"Bangor, PA",													// Institution Address
							null,															// Station Name
							"com.pixelmed.apps.MergeCompositeContext",						// Manufacturer's Model Name
							null,															// Device Serial Number
							"Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
							"Merged patient context");
								
						CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

						list.removeGroupLengthAttributes();
						list.removeMetaInformationHeaderAttributes();
						list.remove(TagFromName.DataSetTrailingPadding);
						FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);

						File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list));
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
							logLn("Writing with new context file "+dstFile);
							list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
						}
					}
					else {
						throw new DicomException("Missing group for SOPInstanceUID on second pass");	// should not be possible for single set case
					}
				}
				else {
					throw new DicomException("Missing SOPInstanceUID");
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	/**
	 * <p>Merge the patient composite context of multiple instances for consistency.</p>
	 *
	 * @param	src				source folder or DICOMDIR
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeCompositeContext(String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
//System.err.println("MergeCompositeContext(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		MediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		firstPassImporter.importDicomFiles(src);
		slf4jlogger.info(dumpGroups());
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		secondPassImporter.importDicomFiles(src);

	}
	
	/**
	 * <p>Merge the patient composite context of multiple instances for consistency.</p>
	 *
	 * @param	srcs			source folders or DICOMDIRs
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeCompositeContext(String[] srcs,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
//System.err.println("MergeCompositeContext(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		OurFirstPassMediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		for (String src : srcs) {
			firstPassImporter.importDicomFiles(src);
		}
		slf4jlogger.info(dumpGroups());
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		for (String src : srcs) {
			secondPassImporter.importDicomFiles(src);
		}
	}
	
	/**
	 * <p>Merge the patient composite context of multiple instances for consistency.</p>
	 *
	 * <p>The files are processed in the order in which they are specified on the command line, and when there
	 * is a conflict, the first values are used. This can be used to make sure that all PatientIDs, for example
	 * are coerced to those first specified.</p>
	 *
	 * <p>For example, if a folder of images for multiple patients is specified first, and then a folder of
	 * structured reports or presentation states referencing (some of) those images but with different patient
	 * level identifiers is specified second, then the latter (the reports or presentation states) will be cooerced
	 * to have the same patient context as the former (the images).</p>
	 *
	 * @param	arg		array of 2 or more strings - one or more source folder or DICOMDIR (to merge and use as a source of context),
	 *                  and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new MergeCompositeContext(arg[0],arg[1],logger);
			}
			else if (arg.length > 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				int nSrcs = arg.length-1;
				String[] srcs = new String[nSrcs];
				System.arraycopy(arg,0,srcs,0,nSrcs);
				new MergeCompositeContext(srcs,arg[nSrcs],logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.MergeCompositeContext srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


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
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to read a map of Study, Series, SOP Instance and Frame of Reference UIDs pairs, and then remap occurences in a set of DICOM files to the other member of the pair.</p>
 *
 * <p>Useful, for example, when UIDs have been changed, annotations made, and there is a need to apply the annotations back to the originals.</p>
 *
 * @see	com.pixelmed.apps.UIDMapByMatchingPixelData
 * @see	com.pixelmed.apps.MergeCompositeContext
 *
 * @author	dclunie
 */
public class RemapUIDs {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/RemapUIDs.java,v 1.7 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DoseReporterWithLegacyOCRAndAutoSendToRegistry.class);
	
	protected String ourAETitle = "OURAETITLE";

	private Set<String>        setOfCanonicalUIDs          = new HashSet<String>();
	private Map<String,String> mapOfOtherUIDToCanonicalUID = new HashMap<String,String>();
	
	private String dstFolderName;
	
	/**
	 * <p>Change all non-canonical UIDs that can be mapped to a canonical UID.</p>
	 *
	 * <p>Leaves canonical UIDs, SOP Classes and anything unrecognized alone.</p>
	 *
	 * <p>Recursively descends into sequences to process nested UIDs too.</p>
	 *
	 * @param	list
	 * @param	setOfCanonicalUIDs
	 * @param	mapOfOtherUIDToCanonicalUID
	 */
	public static void remapUIDs(AttributeList list,Set<String> setOfCanonicalUIDs,Map<String,String> mapOfOtherUIDToCanonicalUID) {
		Iterator<Attribute> i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a instanceof UniqueIdentifierAttribute) {
				try {
					String[] values = a.getStringValues();
					if (values != null && values.length > 0) {
						AttributeTag tag = a.getTag();
						String attributeName = list.getDictionary().getNameFromTag(tag);
						a.removeValues();
						for (int j=0; j<values.length; ++ j) {
							String value = values[j];
							if (value != null && value.length() > 0) {
								if (setOfCanonicalUIDs.contains(value)) {
									System.err.println("Already canonical "+tag+" "+attributeName+" "+value);
								}
								else if (tag.equals(TagFromName.TransferSyntaxUID)) {
									System.err.println("Not mapping Transfer Syntax UID "+tag+" "+attributeName+" "+value);
								}
								else if (tag.equals(TagFromName.SOPClassUID) || tag.equals(TagFromName.ReferencedSOPClassUID) || SOPClass.getSetOfStorageSOPClasses().contains(value)) {
									System.err.println("Not mapping SOP Class UID "+tag+" "+attributeName+" "+value);
								}
								else {
									String newValue = mapOfOtherUIDToCanonicalUID.get(value);
									if (newValue != null && newValue.length() > 0) {
										System.err.println("Mapping "+tag+" "+attributeName+" "+value+" to canonical "+newValue);
										value = newValue;
									}
									else {
										System.err.println("Not mapping "+tag+" "+attributeName+" "+value);
									}
								}
							}
							a.addValue(value);
						}
					}
				}
				catch (DicomException e) {
					slf4jlogger.error("",e);
				}
			}
			else if (a instanceof SequenceAttribute) {
				Iterator<SequenceItem> i2 = ((SequenceAttribute)a).iterator();
				while (i2.hasNext()) {
					SequenceItem item = i2.next();
					if (item != null) {
						AttributeList itemList = item.getAttributeList();
						if (itemList != null) {
							remapUIDs(itemList,setOfCanonicalUIDs,mapOfOtherUIDToCanonicalUID);
						}
					}
				}
			}
		}
	}

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
				
				remapUIDs(list,setOfCanonicalUIDs,mapOfOtherUIDToCanonicalUID);
						
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.RemapUIDs",									// Manufacturer's Model Name
					null,															// Device Serial Number
					"Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
					"Remapped UIDs");
								
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
					list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}

	private void reapMapFile(String uidmapFileName) throws FileNotFoundException, IOException {
		LineNumberReader in = new LineNumberReader(new FileReader(uidmapFileName));
		String line = null;
		while ((line=in.readLine()) != null && line.trim().length() > 0) {
//System.err.println(line);
			String[] columns = line.split("\t");
//System.err.println("columns.length = "+columns.length);
//System.err.println("columns[0] = "+columns[0]);
//System.err.println("columns[1] = "+columns[1]);
			if (columns.length >= 2) {
				setOfCanonicalUIDs.add(columns[0]);
				for (int i=1; i<columns.length; ++i) {
					mapOfOtherUIDToCanonicalUID.put(columns[i],columns[0]);
				}
			}
			else {
				slf4jlogger.warn("Ignoring too few or too many UIDs (pair of 2 required): {}",line);
			}
		}
		in.close();
	}

	/**
	 * <p>Change UIDs based on UID map file.</p>
	 *
	 * <p>The order of UIDs in the map file is important, the first of tab-separated multiple UIDs in one line being the canonical UID
	 * to which the others on the line are remapped.</p>
	 *
	 * @param	uidmapFileName
	 * @param	srcFolderName
	 * @param	dstFolderName
	 */
	public RemapUIDs(String uidmapFileName,String srcFolderName,String dstFolderName) throws FileNotFoundException, IOException, DicomException {
		this.dstFolderName = dstFolderName;
		reapMapFile(uidmapFileName);
		OurMediaImporter importer = new OurMediaImporter();
		importer.importDicomFiles(srcFolderName);
	}
	
	/**
	 * <p>Change UIDs based on UID map file.</p>
	 *
	 * <p>The order of UIDs in the map file is important, the first of tab-separated multiple UIDs in one line being the canonical UID
	 * to which the others on the line are remapped.</p>
	 *
	 * @param	arg		uidmapFileName srcFolderName dstFolderName
	 */
	public static void main(String arg[]) {
		try {
			new RemapUIDs(arg[0],arg[1],arg[2]);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


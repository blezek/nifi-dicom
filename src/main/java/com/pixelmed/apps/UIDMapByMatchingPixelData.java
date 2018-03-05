/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.OtherByteAttribute;
import com.pixelmed.dicom.OtherWordAttribute;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to identify duplicate images based on having the same pixel data hash and constructing collections of their duplicate Study, Series, SOP Instance and Frame of Reference UIDs.</p>
 *
 * @author	dclunie
 */
public class UIDMapByMatchingPixelData {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/UIDMapByMatchingPixelData.java,v 1.7 2017/01/24 10:50:34 dclunie Exp $";
	
	private static final Logger slf4jlogger = LoggerFactory.getLogger(UIDMapByMatchingPixelData.class);

	private Map<String,List<String>> mapOfStudyInstanceUIDsByPixelDataHash    = new TreeMap<String,List<String>>();
	private Map<String,List<String>> mapOfSeriesInstanceUIDsByPixelDataHash   = new TreeMap<String,List<String>>();
	private Map<String,List<String>> mapOfSOPInstanceUIDsByPixelDataHash      = new TreeMap<String,List<String>>();
	private Map<String,List<String>> mapOfFrameOfReferenceUIDsByPixelDataHash = new TreeMap<String,List<String>>();
	
	public Collection<List<String>> getDuplicateStudyInstanceUIDs()		{ return mapOfStudyInstanceUIDsByPixelDataHash.values(); }
	public Collection<List<String>> getDuplicateSeriesInstanceUIDs()		{ return mapOfSeriesInstanceUIDsByPixelDataHash.values(); }
	public Collection<List<String>> getDuplicateSOPInstanceUIDs()			{ return mapOfSOPInstanceUIDsByPixelDataHash.values(); }
	public Collection<List<String>> getDuplicateFrameOfReferenceUIDs()	{ return mapOfFrameOfReferenceUIDsByPixelDataHash.values(); }
	
	static private void addUIDToMapIndexedByHash(Map<String,List<String>> map,AttributeList list,AttributeTag tag,String hash) {
		String uid = Attribute.getSingleStringValueOrEmptyString(list,tag);
		if (uid.length() > 0) {
			List<String> uids = map.get(hash);
			if (uids == null) {
				uids = new LinkedList<String>();
				map.put(hash,uids);
			}
			// append the UID to the end of the list (preserving the order in which they were encountered in the input files)
			if (!uids.contains(uid)) {
				uids.add(uid);
			}
		}
	}
	
	protected class OurMediaImporter extends MediaImporter {
		
		public OurMediaImporter() {
			super(null);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				Attribute aPixelData = list.getPixelData();
				if (aPixelData != null && aPixelData.getVL() > 0) {
					byte[] byteValues = null;
					if (aPixelData instanceof OtherByteAttribute) {
						byteValues = aPixelData.getByteValues();
					}
					else if (aPixelData instanceof OtherWordAttribute) {
						short[] shortValues = aPixelData.getShortValues();
						byteValues = new byte[shortValues.length*2];
						int j=0;
						for (int k=0; k<shortValues.length; ++k) {
							byteValues[j++] = (byte)((shortValues[k] >>> 8) &0xff);
							byteValues[j++] = (byte) (shortValues[k]        &0xff);
						}
					}
					if (byteValues != null) {
						String hash = FileUtilities.md5(new ByteArrayInputStream(byteValues));

						addUIDToMapIndexedByHash(mapOfStudyInstanceUIDsByPixelDataHash,   list,TagFromName.StudyInstanceUID,   hash);
						addUIDToMapIndexedByHash(mapOfSeriesInstanceUIDsByPixelDataHash,  list,TagFromName.SeriesInstanceUID,  hash);
						addUIDToMapIndexedByHash(mapOfSOPInstanceUIDsByPixelDataHash,     list,TagFromName.SOPInstanceUID,     hash);
						addUIDToMapIndexedByHash(mapOfFrameOfReferenceUIDsByPixelDataHash,list,TagFromName.FrameOfReferenceUID,hash);
					}
				}
			}
			catch (Exception e) {
				System.err.println("Error: File "+mediaFileName+" exception "+e);
			}
		}
	}
	
	private static String toString(Map<String,List<String>> mapOfUIDsByPixelDataHash,String uidType) {
		StringBuffer buf = new StringBuffer();
		for (String hash : mapOfUIDsByPixelDataHash.keySet()) {
			//buf.append(hash);
			//buf.append("\t");
			//buf.append(uidType);
			//String prefix="\t";
			String prefix="";
			List<String> uids = mapOfUIDsByPixelDataHash.get(hash);
			for (String uid: uids) {
				buf.append(prefix);
				buf.append(uid);
				prefix="\t";
			}
			buf.append("\n");
		}
		return buf.toString();
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(toString(mapOfStudyInstanceUIDsByPixelDataHash,   "STUDY"));
		buf.append(toString(mapOfSeriesInstanceUIDsByPixelDataHash,  "SERIES"));
		buf.append(toString(mapOfSOPInstanceUIDsByPixelDataHash,     "INSTANCE"));
		buf.append(toString(mapOfFrameOfReferenceUIDsByPixelDataHash,"FOR"));
		return buf.toString();
	}
	
	private static void cullUIDsThatAreNotDuplicatedInMultipleImages(Map<String,List<String>> mapOfUIDsByPixelDataHash) {
		Iterator<List<String>> i = mapOfUIDsByPixelDataHash.values().iterator();
		while (i.hasNext()) {
			List<String> uids = i.next();
			if (uids.size() < 2) {
				i.remove();
			}
		}
	}

	private void cullUIDsThatAreNotDuplicatedInMultipleImages() {
		cullUIDsThatAreNotDuplicatedInMultipleImages(mapOfStudyInstanceUIDsByPixelDataHash);
		cullUIDsThatAreNotDuplicatedInMultipleImages(mapOfSeriesInstanceUIDsByPixelDataHash);
		cullUIDsThatAreNotDuplicatedInMultipleImages(mapOfSOPInstanceUIDsByPixelDataHash);
		cullUIDsThatAreNotDuplicatedInMultipleImages(mapOfFrameOfReferenceUIDsByPixelDataHash);
	}

	/**
	 * <p>Identify different UIDs of duplicate images by using a hash of pixel data values.</p>
	 *
	 * @param	srcs		one or more source folders or DICOMDIRs
	 */
	public UIDMapByMatchingPixelData(String[] srcs) throws IOException, DicomException {
		OurMediaImporter importer = new OurMediaImporter();
		for (String src : srcs) {
			importer.importDicomFiles(src);
		}
		cullUIDsThatAreNotDuplicatedInMultipleImages();
	}
	
	/**
	 * <p>Identify different UIDs of duplicate images by using a hash of pixel data values.</p>
	 *
	 * <p>The duplicate UIDs will be listed in the order in which they are encountered, so if
	 * one UID is the canonical UID to which the others are to be matched, order the input
	 * paths accordingly.</p>
	 *
	 * @param	arg		one or more source folders or DICOMDIRs
	 */
	public static void main(String arg[]) {
		try {
			System.err.println(new UIDMapByMatchingPixelData(arg));	// no need to use SLF4J since command line utility/test
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


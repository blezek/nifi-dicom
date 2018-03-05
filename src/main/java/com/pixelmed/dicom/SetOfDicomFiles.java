/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to describe a set of DICOM files and their features such as SOP Class, Instance and Transfer Syntax UIDs.</p>
 *
 * @author	dclunie
 */
public class SetOfDicomFiles extends HashSet<SetOfDicomFiles.DicomFile> {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SetOfDicomFiles.java,v 1.16 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SetOfDicomFiles.class);
	
	private HashSet setOfSOPClassUIDs = new HashSet();
	
	public Set getSetOfSOPClassUIDs() {return setOfSOPClassUIDs; }
	
	public class DicomFile implements Comparable {
		private String fileName;
		private String sopClassUID;
		private String sopInstanceUID;
		private String transferSyntaxUID;
		private AttributeList list;
		
		public String getFileName() {return fileName; }
		public String getSOPClassUID() {return sopClassUID; }
		public String getSOPInstanceUID() {return sopInstanceUID; }
		public String getTransferSyntaxUID() {return transferSyntaxUID; }
		public AttributeList getAttributeList() {return list; }
		
		public int compareTo(Object o) {
			return fileName == null ? (((DicomFile)o).fileName == null ? 0 : -1 ) : fileName.compareTo(((DicomFile)o).fileName);
		}

		public int hashCode() {
			return fileName == null ? 0 : fileName.hashCode();
		}
		
		public DicomFile(String fileName,String sopClassUID,String sopInstanceUID,String transferSyntaxUID) {
			this.fileName=fileName;
			this.sopClassUID=sopClassUID;
			this.sopInstanceUID=sopInstanceUID;
			this.transferSyntaxUID=transferSyntaxUID;
			this.list=null;
		}

		public DicomFile(String fileName,AttributeList list) {
			this(fileName,list,false);
		}

		public DicomFile(String fileName,AttributeList list,boolean keepList) {
			this.fileName=fileName;
			sopClassUID=Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPClassUIDInFile);
			if (sopClassUID == null) {
				sopClassUID=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
			}
			sopInstanceUID=Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPInstanceUIDInFile);
			if (sopInstanceUID == null) {
				sopInstanceUID=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
			}
			transferSyntaxUID=Attribute.getSingleStringValueOrNull(list,TagFromName.TransferSyntaxUID);
			if (keepList) {
				this.list=list;
			}
			else {
				this.list=null;
			}
		}
		
		/**
		 * <p>Store a description a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
		 *
		 * @param	fileName	a DICOM file
		 */
		public DicomFile(String fileName) {
			this(fileName,false);
		}

		/**
		 * <p>Store a description a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
		 *
		 * @param	fileName	a DICOM file
		 * @param	keepList	whether or not to keep the entire attribute list (including pixel data) memory resident
		 */
		public DicomFile(String fileName,boolean keepList) {
			this(fileName,keepList,keepList);
		}
		
		/**
		 * <p>Store a description a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
		 *
		 * @param	fileName	a DICOM file
		 * @param	keepList	whether or not to keep the entire attribute list (excluding pixel data unless requested) memory resident
		 * @param	keepPixelData	whether or not to keep the pixel data memory resident as well
		 */
		public DicomFile(String fileName,boolean keepList,boolean keepPixelData) {
			this.fileName=fileName;
			try {
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(fileName)));
				list = new AttributeList();
				boolean fullListRead = false;
				if (keepList) {
					if (keepPixelData) {
						list.read(i);
					}
					else {
						list.read(i,TagFromName.PixelData);
					}
					fullListRead=true;
				}
				else {
					list.readOnlyMetaInformationHeader(i);
				}
				sopClassUID=Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPClassUIDInFile);
				sopInstanceUID=Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPInstanceUIDInFile);
				transferSyntaxUID=Attribute.getSingleStringValueOrNull(list,TagFromName.TransferSyntaxUID);
				if (sopClassUID == null || sopInstanceUID == null) {
					if (!fullListRead) {
						list.read(i,TagFromName.PixelData);
					}
					sopClassUID=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
					sopInstanceUID=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
				}
				if (!keepList) {
					list=null;
				}
				i.close();
			}
			catch (Exception e) {
				slf4jlogger.error("While reading \"{}\"",fileName,e);
			}
		}

		/**
		 * <p>Return a String representing this object's value.</p>
		 *
		 * @return	a string representation of the value of this object
		 */
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("file=");
			strbuf.append(fileName);
			strbuf.append(", sopClassUID=");
			strbuf.append(sopClassUID);
			strbuf.append(", sopInstanceUID=");
			strbuf.append(sopInstanceUID);
			strbuf.append(", transferSyntaxUID=");
			strbuf.append(transferSyntaxUID);
			return strbuf.toString();
		}
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		int j = 0;
		Iterator i =iterator();
		while (i.hasNext()) {
			strbuf.append("DicomFile [");
			strbuf.append(Integer.toString(j));
			strbuf.append("]:\n");
			strbuf.append(((SetOfDicomFiles.DicomFile)i.next()).toString());
			strbuf.append("\n");
			++j;
		}
		return strbuf.toString();
	}

	/**
	 * <p>Add a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * <p>Keeps only the minimal descriptive attributes, and not the entire attribute list (including pixel data) memory resident.</p>
	 *
	 * @param	fileName	a DICOM file
	 * @return				the DicomFile added
	 */
	public DicomFile add(String fileName) {
		return add(fileName,false);
	}

	/**
	 * <p>Add a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * <p>Keeps only the minimal descriptive attributes, and not the entire attribute list (including pixel data) memory resident.</p>
	 *
	 * @param	file	a DICOM file
	 * @return				the DicomFile added
	 * @throws	IOException
	 */
	public DicomFile add(File file) throws IOException {
		return add(file.getCanonicalPath(),false);
	}

	/**
	 * <p>Add a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * @param	fileName	a DICOM file
	 * @param	keepList	whether or not to keep the entire attribute list memory resident
	 * @return				the DicomFile added
	 */
	public DicomFile add(String fileName,boolean keepList) {
		DicomFile dicomFile = new DicomFile(fileName,keepList);
		add(dicomFile);
		setOfSOPClassUIDs.add(dicomFile.sopClassUID);
		return dicomFile;
	}

	/**
	 * <p>Add a DICOM file by reading its metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * @param	fileName	a DICOM file
	 * @param	keepList	whether or not to keep the entire attribute list (excluding pixel data unless requested) memory resident
	 * @param	keepPixelData	whether or not to keep the pixel data memory resident as well
	 * @return				the DicomFile added
	 */
	public DicomFile add(String fileName,boolean keepList,boolean keepPixelData) {
		DicomFile dicomFile = new DicomFile(fileName,keepList,keepPixelData);
		add(dicomFile);
		setOfSOPClassUIDs.add(dicomFile.sopClassUID);
		return dicomFile;
	}

	/**
	 * <p>Add a DICOM file with the specified attributes.</p>
	 *
	 * @param	fileName	a DICOM file
	 * @return				the DicomFile added
	 */
	public DicomFile add(String fileName,String sopClassUID,String sopInstanceUID,String transferSyntaxUID) {
		setOfSOPClassUIDs.add(sopClassUID);
		DicomFile dicomFile = new DicomFile(fileName,sopClassUID,sopInstanceUID,transferSyntaxUID);
		add(dicomFile);
		return dicomFile;
	}

	/**
	 * <p>Get the attribute lists for all files, if they were kept during creation.</p>
	 *
	 * @return		an array of attribute lists, each of which will be null unless keeplists was true when created
	 */
	public AttributeList[] getAttributeLists() {
		AttributeList lists[] = new AttributeList[size()];
		int j=0;
		Iterator i =iterator();
		while (i.hasNext()) {
			lists[j] = ((SetOfDicomFiles.DicomFile)i.next()).getAttributeList();
			++j;
		}
		return lists;
	}
	
	/**
	 * <p>Construct a set of DICOM files from a list of String path names by reading each file's metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * @param	paths	a list of String DICOM file names (e.g., a Vector or an ArrayList)
	 */
	public SetOfDicomFiles(AbstractList<String> paths) {
		for (int j=0; j< paths.size(); ++j) {
			String dicomFileName = paths.get(j);
			if (dicomFileName != null) {
				add(dicomFileName);
			}
		}
	}
	
	/**
	 * <p>Construct a set of DICOM files from a list of String path names by reading each file's metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * @param	paths			a list of String DICOM file names (e.g., a Vector or an ArrayList)
	 * @param	keepList		whether or not to keep the entire attribute list (excluding pixel data unless requested) memory resident
	 * @param	keepPixelData	whether or not to keep the pixel data memory resident as well
	 */
	public SetOfDicomFiles(AbstractList<String> paths,boolean keepList,boolean keepPixelData) {
		for (int j=0; j< paths.size(); ++j) {
			String dicomFileName = paths.get(j);
			if (dicomFileName != null) {
				add(dicomFileName,keepList,keepPixelData);
			}
		}
	}
	
	/**
	 * <p>Construct a set of DICOM files from an array of String path names by reading each file's metaheader, +/- entire attribute list, as necessary.</p>
	 *
	 * @param	paths	an array of String DICOM file names
	 */
	public SetOfDicomFiles(String[] paths) {
		for (String dicomFileName : paths) {
			if (dicomFileName != null) {
				add(dicomFileName);
			}
		}
	}
	
	/**
	 * <p>Construct a set of DICOM files from an array of String path names by reading each file's metaheader, +/- entire attribute list, as necessary</p>
	 *
	 * @param	paths			an array of String DICOM file names
	 * @param	keepList		whether or not to keep the entire attribute list (excluding pixel data unless requested) memory resident
	 * @param	keepPixelData	whether or not to keep the pixel data memory resident as well
	 */
	public SetOfDicomFiles(String[] paths,boolean keepList,boolean keepPixelData) {
		for (String dicomFileName : paths) {
			if (dicomFileName != null) {
				add(dicomFileName,keepList,keepPixelData);
			}
		}
	}
	
	/**
	 * <p>Construct an empty set of DICOM files.</p>
	 */
	public SetOfDicomFiles() {
		super();
	}

	/**
	 * <p>For testing, read all DICOM files and build a set of them.</p>
	 *
	 * @param	arg	the filenames
	 */
	public static void main(String arg[]) {
		{
			SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(arg);
			System.err.println(setOfDicomFiles.toString());		// no need to use SLF4J since command line utility/test
		}
		{
			ArrayList<String> arrayList = new ArrayList<String>(arg.length);
			for (String f : arg) {
				arrayList.add(f);
			}
			SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(arrayList);
			System.err.println(setOfDicomFiles.toString());
		}
		{
			Vector<String> vector = new Vector<String>(arg.length);
			for (String f : arg) {
				vector.add(f);
			}
			SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(vector);
			System.err.println(setOfDicomFiles.toString());
		}
	}
}


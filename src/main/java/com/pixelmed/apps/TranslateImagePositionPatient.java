/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DecimalStringAttribute;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to read a set of DICOM files and translate the Image Position (Patient) by a fixed offset.</p>
 *
 * @author	dclunie
 */
public class TranslateImagePositionPatient {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/TranslateImagePositionPatient.java,v 1.5 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(TranslateImagePositionPatient.class);
	
	protected String ourAETitle = "OURAETITLE";

	private String srcFolderName;
	private String dstFolderName;
	
	//private double normalDistance;
	private double x;
	private double y;
	private double z;
	
	public static Attribute translateImagePositionPatient(Attribute aImagePositionPatient,double x,double y,double z) throws DicomException {
//System.err.println("TranslateImagePositionPatient.translateImagePositionPatient(): was "+aImagePositionPatient);
		if (aImagePositionPatient != null) {
			double[] vImagePositionPatient = aImagePositionPatient.getDoubleValues();
			vImagePositionPatient[0] += x;
			vImagePositionPatient[1] += y;
			vImagePositionPatient[2] += z;
			aImagePositionPatient = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
			aImagePositionPatient.addValue(vImagePositionPatient[0]);
			aImagePositionPatient.addValue(vImagePositionPatient[1]);
			aImagePositionPatient.addValue(vImagePositionPatient[2]);
//System.err.println("TranslateImagePositionPatient.translateImagePositionPatient(): now "+aImagePositionPatient);
		}
		return aImagePositionPatient;
	}
	
	public static void translateImagePositionPatientInPlanePositionSequence(SequenceAttribute planePositionSequence,double x,double y,double z) throws DicomException {
		AttributeList planePositionList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(planePositionSequence);
		Attribute aImagePositionPatient = planePositionList.get(TagFromName.ImagePositionPatient);
		if (aImagePositionPatient != null) {
			planePositionList.put(translateImagePositionPatient(aImagePositionPatient,x,y,z));
		}
	}
	
	public static void translateImagePositionPatient(AttributeList list,double x,double y,double z) throws DicomException {
		// derived extraction logic from GeometryOfVolumeFromAttributeList()
		int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,0);
		SequenceAttribute sharedFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.SharedFunctionalGroupsSequence));
		if (numberOfFrames == 1 && sharedFunctionalGroupsSequence == null) {
			// old-fashioned single frame DICOM image
			Attribute aImagePositionPatient = list.get(TagFromName.ImagePositionPatient);
			if (aImagePositionPatient != null) {
				list.put(translateImagePositionPatient(aImagePositionPatient,x,y,z));
			}
		}
		else if (numberOfFrames > 0) {
			if (sharedFunctionalGroupsSequence != null) {
				SequenceAttribute planePositionSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(sharedFunctionalGroupsSequence,TagFromName.PlanePositionSequence));
				if (planePositionSequence != null) {
					translateImagePositionPatientInPlanePositionSequence(planePositionSequence,x,y,z);
				}
				else {
					SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.PerFrameFunctionalGroupsSequence));
					if (perFrameFunctionalGroupsSequence != null) {
						for (int i=0; i<numberOfFrames; ++i) {
							Attribute aImagePositionPatient = null;
							planePositionSequence = (SequenceAttribute)(perFrameFunctionalGroupsSequence.getItem(i).getAttributeList().get(TagFromName.PlanePositionSequence));
							if (planePositionSequence != null) {
								translateImagePositionPatientInPlanePositionSequence(planePositionSequence,x,y,z);
							}
						}
					}
				}
			}
		}
	}
	
	public static void translateImagePositionPatient(AttributeList list,double normalDistance) throws DicomException {
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
				
				//if (normalDistance == 0) {
					translateImagePositionPatient(list,x,y,z);
				//}
				//else {
				//	translateImagePositionPatient(list,normalDistance);
				//}
				
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.TranslateImagePositionPatient",									// Manufacturer's Model Name
					null,															// Device Serial Number
					"Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
					"Translated Image Position (Patient) values by ("+x+","+y+","+z+")");
								
				list.removeGroupLengthAttributes();
				list.removeMetaInformationHeaderAttributes();
				list.remove(TagFromName.DataSetTrailingPadding);
				FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);

				//File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list));
				File dstFile = FileUtilities.makeSameRelativePathNameInDifferentFolder(srcFolderName,dstFolderName,mediaFileName);
				slf4jlogger.info("\"{}\":  =  \"{}\"",mediaFileName,dstFile);
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
				slf4jlogger.error("File {} exception ",mediaFileName,e);
			}
		}
	}

	/**
	 * <p>Read a set of DICOM files and translate the Image Position (Patient) by the specified distance along the normal to the orientation.</p>
	 *
	 * <p>Uses the same sub-folder and file names in the destination folder as supplied in the source folder.</p>
	 *
	 * @param	srcFolderName
	 * @param	dstFolderName
	 * @param	normalDistance
	 */
	//public TranslateImagePositionPatient(String srcFolderName,String dstFolderName,double normalDistance) throws FileNotFoundException, IOException, DicomException {
	//	this.srcFolderName = srcFolderName;
	//	this.dstFolderName = dstFolderName;
	//	this.normalDistance = normalDistance;
	//	OurMediaImporter importer = new OurMediaImporter();
	//	importer.importDicomFiles(srcFolderName);
	//}
	
	/**
	 * <p>Read a set of DICOM files and translate the Image Position (Patient) by the specified 3D offset.</p>
	 *
	 * <p>Uses the same sub-folder and file names in the destination folder as supplied in the source folder (or parent folder of single source file).</p>
	 *
	 * @param	srcPathName
	 * @param	dstFolderName
	 * @param	x
	 * @param	y
	 * @param	z
	 */
	public TranslateImagePositionPatient(String srcPathName,String dstFolderName,double x,double y,double z) throws FileNotFoundException, IOException, DicomException {
		File srcFile = new File(srcPathName);
		this.srcFolderName = srcFile.isFile() ? srcFile.getParent() : srcFolderName;
		this.dstFolderName = dstFolderName;
		//this.normalDistance = 0;	// signal to check that to use the x, y and z values
		this.x = x;
		this.y = y;
		this.z = z;
		OurMediaImporter importer = new OurMediaImporter();
		importer.importDicomFiles(srcPathName);
	}
	
	/**
	 * <p>Read a set of DICOM files and translate the Image Position (Patient) by a fixed offset.</p>
	 *
	 * <p>Uses the same sub-folder and file names in the destination folder as supplied in the source folder.</p>
	 *
	 * @param	arg		[XYZ x y z|NORMALDISTANCE d] srcPathName dstFolderName
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 6 && arg[0].toUpperCase().equals("XYZ")) {
				new TranslateImagePositionPatient(arg[4],arg[5],Double.parseDouble(arg[1]),Double.parseDouble(arg[2]),Double.parseDouble(arg[3]));
			}
			//else if (arg.length == 4 && arg[0].toUpperCase().equals("NORMALDISTANCE")) {
			//	new TranslateImagePositionPatient(arg[2],arg[3],Double.parseDouble(arg[1]));
			//}
			else {
				System.err.println("Error: Usage: TranslateImagePositionPatient [XYZ x y z|NORMALDISTANCE d] srcPathName dstFolderName");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


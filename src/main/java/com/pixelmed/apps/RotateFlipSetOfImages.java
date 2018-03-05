/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.ShortTextAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;
import com.pixelmed.dicom.UniqueIdentifierAttribute;

import com.pixelmed.display.ImageEditUtilities;
import com.pixelmed.display.SourceImage;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for rotating and/or flipping a set of images and updating the other attributes accordingly.</p>
 *
 * @author	dclunie
 */
public class RotateFlipSetOfImages {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/RotateFlipSetOfImages.java,v 1.8 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RotateFlipSetOfImages.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	protected int rotation;
	protected boolean horizontal_flip;
	protected boolean update_orientation;
		
	public static void setDerived(AttributeList list,int rotation,boolean horizontal_flip,boolean update_orientation) throws DicomException {
		{
			String vDerivationDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.DerivationDescription);
			if (vDerivationDescription.length() > 0) {
				vDerivationDescription = vDerivationDescription + "\\";
			}
			vDerivationDescription = vDerivationDescription + "Rotated " + Integer.toString(rotation) + " degrees" + (horizontal_flip ? ", flipped horizontally" : "") + ", " + (update_orientation ? "upated orientation" : "orientation untouched");
			list.remove(TagFromName.DerivationDescription);
			{ Attribute a = new ShortTextAttribute(TagFromName.DerivationDescription); a.addValue(vDerivationDescription); list.put(a); }
		}
		{
			Attribute aImageType = list.get(TagFromName.ImageType);
			if (aImageType != null && aImageType.getVM() > 0) {
				String[] vImageType = aImageType.getStringValues();
				if (vImageType != null && vImageType.length > 0) {
					vImageType[0] = "DERIVED";
					aImageType.removeValues();
					for (int i=0; i<vImageType.length; ++i) {
						aImageType.addValue(vImageType[i]);
					}
				}
			}
		}
		{
			String vSOPInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
			String vSOPClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
			if (vSOPInstanceUID != null && vSOPInstanceUID.length() > 0 && vSOPClassUID != null && vSOPClassUID.length() > 0) {
				SequenceAttribute aSourceImageSequence = (SequenceAttribute)list.get(TagFromName.SourceImageSequence);
				if (aSourceImageSequence == null) {
					aSourceImageSequence = new SequenceAttribute(TagFromName.SourceImageSequence);
				}
				AttributeList iSourceImageSequence = new AttributeList();
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(vSOPInstanceUID); iSourceImageSequence.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(vSOPClassUID); iSourceImageSequence.put(a); }
				aSourceImageSequence.addItem(iSourceImageSequence);
				list.remove(TagFromName.SourceImageSequence);
				list.put(aSourceImageSequence);
			}
		}
		{
			list.remove(TagFromName.SOPInstanceUID);
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(new UIDGenerator().getAnotherNewUID()); list.put(a); }
		}
	}
	
	public static void correctPatientOrientation(AttributeList list,int rotation,boolean horizontal_flip) throws DicomException {
		Attribute aPatientOrientation = list.get(TagFromName.PatientOrientation);
		if (aPatientOrientation != null && aPatientOrientation.getVM() > 0) {
			aPatientOrientation.removeValues();		// best we can do, since we don't know the view, and the impact the rotation or flip has on the view
		}
	}
	
	public static double[] swapRowAndColumnVectors(double[] vImageOrientationPatient) {
		double x = vImageOrientationPatient[0];
		double y = vImageOrientationPatient[1];
		double z = vImageOrientationPatient[2];
		vImageOrientationPatient[0] = vImageOrientationPatient[3];
		vImageOrientationPatient[1] = vImageOrientationPatient[4];
		vImageOrientationPatient[2] = vImageOrientationPatient[5];
		vImageOrientationPatient[3] = x;
		vImageOrientationPatient[4] = y;
		vImageOrientationPatient[5] = z;
		return vImageOrientationPatient;
	}
	
	public static double[] invertDirectionOfRowVector(double[] vImageOrientationPatient) {
		vImageOrientationPatient[0] = -vImageOrientationPatient[0];
		vImageOrientationPatient[1] = -vImageOrientationPatient[1];
		vImageOrientationPatient[2] = -vImageOrientationPatient[2];
		return vImageOrientationPatient;
	}
	
	public static double[] invertDirectionOfColumnVector(double[] vImageOrientationPatient) {
		vImageOrientationPatient[3] = -vImageOrientationPatient[3];
		vImageOrientationPatient[4] = -vImageOrientationPatient[4];
		vImageOrientationPatient[5] = -vImageOrientationPatient[5];
		return vImageOrientationPatient;
	}

	public static void correctImageOrientationPatient(AttributeList list,int rotation,boolean horizontal_flip) throws DicomException {
		Attribute aImageOrientationPatient = list.get(TagFromName.ImageOrientationPatient);
		if (aImageOrientationPatient != null && aImageOrientationPatient.getVM() > 0) {
			double[] vImageOrientationPatient = aImageOrientationPatient.getDoubleValues();
			if (vImageOrientationPatient != null && vImageOrientationPatient.length == 6) {
				if (rotation == 90) {
					swapRowAndColumnVectors(vImageOrientationPatient);
					invertDirectionOfRowVector(vImageOrientationPatient);
				}
				else if (rotation == 180) {
					invertDirectionOfRowVector(vImageOrientationPatient);
					invertDirectionOfColumnVector(vImageOrientationPatient);
				}
				else if (rotation == 270) {
					swapRowAndColumnVectors(vImageOrientationPatient);
					invertDirectionOfColumnVector(vImageOrientationPatient);
				}
				// else do nothing for 0 degrees
				// now flip AFTER correcting for rotation
				if (horizontal_flip) {
					invertDirectionOfRowVector(vImageOrientationPatient);
				}
				aImageOrientationPatient.removeValues();
				for (int i=0; i<vImageOrientationPatient.length; ++i) {
					aImageOrientationPatient.addValue(vImageOrientationPatient[i]);
				}
			}
			else {
				throw new DicomException("Invalid ImageOrientationPatient - cannot correct for rotation/flip");
			}
		}
	}

	protected class OurMediaImporter extends MediaImporter {
		public OurMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (SOPClass.isImageStorage(sopClassUID)) {
					SourceImage sImg = new SourceImage(list);
					
					ImageEditUtilities.rotateAndFlip(sImg,list,rotation,horizontal_flip);
					if (update_orientation) {
						correctPatientOrientation(list,rotation,horizontal_flip);
						correctImageOrientationPatient(list,rotation,horizontal_flip);
					}
					setDerived(list,rotation,horizontal_flip,update_orientation);

					ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
						"PixelMed",														// Manufacturer
						"PixelMed",														// Institution Name
						"Software Development",											// Institutional Department Name
						"Bangor, PA",													// Institution Address
						null,															// Station Name
						"com.pixelmed.apps.RotateFlipSetOfImages.main()",				// Manufacturer's Model Name
						null,															// Device Serial Number
						"Vers. 20090421",												// Software Version(s)
						"Rotated and/or Flipped");
								
					list.correctDecompressedImagePixelModule();
					list.removeGroupLengthAttributes();
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
					
					String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);	// will be the NEW SOP Instance UID created by setDerived()
					File dstFile = new File(dstFolderName,sopInstanceUID+".dcm");
					list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
				}
				else {
					//logLn("Error: File "+mediaFileName+" is an unsupported SOP Class "+sopClassUID);
					slf4jlogger.error("File {} is an unsupported SOP Class {}",mediaFileName,sopClassUID);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	public RotateFlipSetOfImages(int rotation,boolean horizontal_flip,boolean update_orientation,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.dstFolderName = dstFolderName;
		if (rotation % 90 != 0) {
			throw new DicomException("Rotation of "+rotation+" not supported");
		}
		while (rotation >= 360) {
			rotation-= 360;
		}
		while (rotation < 0) {
			rotation+= 360;
		}
		this.rotation = rotation;
		this.horizontal_flip = horizontal_flip;
		this.update_orientation = update_orientation;
		MediaImporter importer = new OurMediaImporter(logger);
		importer.importDicomFiles(src);
	}

	/**
	 * <p>Rotating and/or flipping a set of images and updating the other attributes accordingly.</p>
	 *
	 * @param	arg		array of 5 strings - rotation (0, 90, 180 or 270 degrees), horizontal flip (Y or N), update orientation (Y or N), source folder or DICOMDIR, destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 5) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new RotateFlipSetOfImages(Integer.parseInt(arg[0]),arg[1].toUpperCase(java.util.Locale.US).contains("Y"),arg[2].toUpperCase(java.util.Locale.US).contains("Y"),arg[3],arg[4],logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.RotateFlipSetOfImages rotation horizontal_flip update_orientation srcdir|DICOMDIR dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}

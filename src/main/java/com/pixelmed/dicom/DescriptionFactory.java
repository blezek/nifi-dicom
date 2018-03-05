/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.lang.*;
import java.util.*;

/**
 * <p>A class of static methods to provide descriptions of images, including image orientation
 * relative to the patient from the mathematical position and orientation attributes,
 * and including other descriptive attributes such as from dicom directory records and
 * images using multi-frame functional groups.</p>
 *
 * @author	dclunie
 */
abstract public class DescriptionFactory {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DescriptionFactory.java,v 1.17 2017/01/24 10:50:36 dclunie Exp $";
	
	// 0.5477 would be the square root of 1 (unit vector sum of squares) divided by 3 (oblique axes - a "double" oblique)
	// 0.7071 would be the square root of 1 (unit vector sum of squares) divided by 2 (oblique axes)
	/***/
	private static final double obliquityThresholdCosineValue = 0.8;
	
	/**
	 * <p>Get a label describing the major axis from a unit vector (direction cosine) as found in ImageOrientationPatient.</p>
	 *
	 * <p>Some degree of deviation from one of the standard orthogonal axes is allowed before deciding no major axis applies and returning null.</p>
	 *
	 * @param	x	x component between -1 and 1
	 * @param	y	y component between -1 and 1
	 * @param	z	z component between -1 and 1
	 * @return	the string describing the orientation of the vector, or null if oblique
	 */
	public static final String getMajorAxisFromPatientRelativeDirectionCosine(double x,double y,double z) {
		String axis = null;
		
		String orientationX = x < 0 ? "R" : "L";
		String orientationY = y < 0 ? "A" : "P";
		String orientationZ = z < 0 ? "F" : "H";

		double absX = Math.abs(x);
		double absY = Math.abs(y);
		double absZ = Math.abs(z);

		// The tests here really don't need to check the other dimensions,
		// just the threshold, since the sum of the squares should be == 1.0
		// but just in case ...
		
		if (absX>obliquityThresholdCosineValue && absX>absY && absX>absZ) {
			axis=orientationX;
		}
		else if (absY>obliquityThresholdCosineValue && absY>absX && absY>absZ) {
			axis=orientationY;
		}
		else if (absZ>obliquityThresholdCosineValue && absZ>absX && absZ>absY) {
			axis=orientationZ;
		}
		return axis;
	}
	
	public static final String AXIAL_LABEL = "AXIAL";
	public static final String CORONAL_LABEL = "CORONAL";
	public static final String SAGITTAL_LABEL = "SAGITTAL";
	public static final String OBLIQUE_LABEL = "OBLIQUE";

	/**
	 * <p>Get a label describing the axial, coronal or sagittal plane from row and column unit vectors (direction cosines) as found in ImageOrientationPatient.</p>
	 *
	 * <p>Some degree of deviation from one of the standard orthogonal planes is allowed before deciding the plane is OBLIQUE.</p>
	 *
	 * @param	rowX	row x component between -1 and 1
	 * @param	rowY	row y component between -1 and 1
	 * @param	rowZ	row z component between -1 and 1
	 * @param	colX	column x component between -1 and 1
	 * @param	colY	column y component between -1 and 1
	 * @param	colZ	column z component between -1 and 1
	 * @return		the string describing the plane of orientation, AXIAL, CORONAL, SAGITTAL or OBLIQUE, or null if undetermined
	 */
	public static final String makeImageOrientationLabelFromImageOrientationPatient(
			double rowX,double rowY,double rowZ,
			double colX,double colY,double colZ) {
		String label = null;
		String rowAxis = getMajorAxisFromPatientRelativeDirectionCosine(rowX,rowY,rowZ);
		String colAxis = getMajorAxisFromPatientRelativeDirectionCosine(colX,colY,colZ);
		if (rowAxis != null && colAxis != null) {
			if      ((rowAxis.equals("R") || rowAxis.equals("L")) && (colAxis.equals("A") || colAxis.equals("P"))) label=AXIAL_LABEL;
			else if ((colAxis.equals("R") || colAxis.equals("L")) && (rowAxis.equals("A") || rowAxis.equals("P"))) label=AXIAL_LABEL;
		
			else if ((rowAxis.equals("R") || rowAxis.equals("L")) && (colAxis.equals("H") || colAxis.equals("F"))) label=CORONAL_LABEL;
			else if ((colAxis.equals("R") || colAxis.equals("L")) && (rowAxis.equals("H") || rowAxis.equals("F"))) label=CORONAL_LABEL;
		
			else if ((rowAxis.equals("A") || rowAxis.equals("P")) && (colAxis.equals("H") || colAxis.equals("F"))) label=SAGITTAL_LABEL;
			else if ((colAxis.equals("A") || colAxis.equals("P")) && (rowAxis.equals("H") || rowAxis.equals("F"))) label=SAGITTAL_LABEL;
		}
		else {
			label=OBLIQUE_LABEL;
		}
		return label;
	}

	/**
	 * <p>Get a label describing the axial, coronal or sagittal plane from row and column unit vectors (direction cosines) in ImageOrientationPatient.</p>
	 *
	 * <p>Some degree of deviation from one of the standard orthogonal planes is allowed before deciding the plane is OBLIQUE.</p>
	 *
	 * @param	aImageOrientationPatient	attribute containing the patient-relative orientation
	 * @return								the string describing the plane of orientation, AXIAL, CORONAL, SAGITTAL or OBLIQUE, or null if undetermined
	 */
	public static final String makeImageOrientationLabelFromImageOrientationPatient(Attribute aImageOrientationPatient) {
		String label = null;
		if (aImageOrientationPatient != null) {
			double[] vImageOrientationPatient = null;
			try {
				vImageOrientationPatient = aImageOrientationPatient.getDoubleValues();
			}
			catch (DicomException e) {
			}
			if (vImageOrientationPatient != null) {
				if (vImageOrientationPatient.length == 6) {
					label = makeImageOrientationLabelFromImageOrientationPatient(
						vImageOrientationPatient[0],vImageOrientationPatient[1],vImageOrientationPatient[2],
						vImageOrientationPatient[3],vImageOrientationPatient[4],vImageOrientationPatient[5]);
				}
			}
		}
		return label;
	}

	/**
	 * <p>Get a label describing the axial, coronal or sagittal plane from row and column unit vectors (direction cosines) found in ImageOrientationPatient in the supplied AttributeList.</p>
	 *
	 * <p>Some degree of deviation from one of the standard orthogonal planes is allowed before deciding the plane is OBLIQUE.</p>
	 *
	 * @param	list	list of attributes containing ImageOrientationPatient
	 * @return			the string describing the plane of orientation, AXIAL, CORONAL, SAGITTAL or OBLIQUE, or null if undetermined
	 */
	public static final String makeImageOrientationLabelFromImageOrientationPatient(AttributeList list) {
		Attribute aImageOrientationPatient = list.get(TagFromName.ImageOrientationPatient);
		return makeImageOrientationLabelFromImageOrientationPatient(aImageOrientationPatient);
	}
	
	/**
	 * <p>Get a PatientOrientation style string from a unit vector (direction cosine) as found in ImageOrientationPatient.</p>
	 *
	 * <p>Returns letters representing R (right) or L (left), A (anterior) or P (posterior), F (feet) or H (head).</p>
	 *
	 * <p>If the orientation is not precisely orthogonal to one of the major axes,
	 * more than one letter is returned, from major to minor axes, with up to three
	 * letters in the case of a "double oblique".</p>
	 *
	 * @param	x	x component between -1 and 1
	 * @param	y	y component between -1 and 1
	 * @param	z	z component between -1 and 1
	 * @return		the string describing the orientation of the vector
	 */
	public static final String makePatientOrientationFromPatientRelativeDirectionCosine(double x,double y,double z) {
		StringBuffer buffer = new StringBuffer();
		
		String orientationX = x < 0 ? "R" : "L";
		String orientationY = y < 0 ? "A" : "P";
		String orientationZ = z < 0 ? "F" : "H";

		double absX = Math.abs(x);
		double absY = Math.abs(y);
		double absZ = Math.abs(z);

		for (int i=0; i<3; ++i) {
			if (absX>.0001 && absX>absY && absX>absZ) {
				buffer.append(orientationX);
				absX=0;
			}
			else if (absY>.0001 && absY>absX && absY>absZ) {
				buffer.append(orientationY);
				absY=0;
			}
			else if (absZ>.0001 && absZ>absX && absZ>absY) {
				buffer.append(orientationZ);
				absZ=0;
			}
			else break;
		}
		return buffer.toString();
	}

	/**
	 * <p>Get a PatientOrientation style string from row and column unit vectors (direction cosines) as found in ImageOrientationPatient.</p>
	 *
	 * <p>Returns letters representing R (right) or L (left), A (anterior) or P (posterior), F (feet) or H (head).</p>
	 *
	 * <p>If the orientation is not precisely orthogonal to one of the major axes,
	 * more than one letter is returned, from major to minor axes, with up to three
	 * letters in the case of a "double oblique".</p>
	 *
	 * <p>The row and column letters returned are separated by the usual DICOM string delimiter, a backslash.</p>
	 *
	 * @param	rowX	row x component between -1 and 1
	 * @param	rowY	row y component between -1 and 1
	 * @param	rowZ	row z component between -1 and 1
	 * @param	colX	column x component between -1 and 1
	 * @param	colY	column y component between -1 and 1
	 * @param	colZ	column z component between -1 and 1
	 * @return	the string describing the row and then the column
	 */
	public static final String makePatientOrientationFromImageOrientationPatient(
			double rowX,double rowY,double rowZ,
			double colX,double colY,double colZ) {
		return	 makePatientOrientationFromPatientRelativeDirectionCosine(rowX,rowY,rowZ)
			+"\\"
			+makePatientOrientationFromPatientRelativeDirectionCosine(colX,colY,colZ);
	}
	
	/**
	 * <p>Get a human readable string meaningfully describing an image from an attribute list such as from a directory record.</p>
	 *
	 * <p>This version is heavily tuned to describe MR multiframe images thoroughly.</p>
	 *
	 * @param	list	the list of attributes (such as from an IMAGE level directory record)
	 * @return		a human readable string meaningfully describing the image
	 */
	public static final String makeImageDescription(AttributeList list) {
//System.err.println("DescriptionFactory.makeImageDescription(AttributeList)");
		StringBuffer buffer = new StringBuffer();
		String useNumber=Attribute.getSingleStringValueOrNull(list,TagFromName.InConcatenationNumber);
		if (useNumber == null) useNumber=Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstanceNumber);
		buffer.append(useNumber);
		{
			String vNumberOfFrames=Attribute.getSingleStringValueOrNull(list,TagFromName.NumberOfFrames);
			if (vNumberOfFrames != null && !vNumberOfFrames.equals("1")) {
				buffer.append(" [");
				buffer.append(vNumberOfFrames);
				buffer.append(" frames]");
			}
		}
		{
			buffer.append(" {");
			String prefix = "";
			String sopClassAbbreviation = null;
			{
				String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
				if (sopClassUID == null) {
					sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPClassUIDInFile);
				}
				if (sopClassUID != null) {
					sopClassAbbreviation = SOPClassDescriptions.getAbbreviationFromUID(sopClassUID);
					buffer.append(prefix); prefix=",";
					buffer.append(sopClassAbbreviation);
				}
			}
			String rememberImageTypeValue3 = null;
			Attribute aImageType = list.get(TagFromName.ImageType);
			if (aImageType != null) {
				String[] vImageType = null;
				try {
					vImageType = aImageType.getStringValues();
				}
				catch (DicomException e) {
				}
				if (vImageType != null) {
					if (vImageType.length > 0) {
						if (vImageType[0].equals("DERIVED")) {	// do not bother appending ORIGINAL which is more common
							buffer.append(prefix); prefix=",";
							buffer.append(vImageType[0]);	
						}
						if (vImageType.length > 2) {
							rememberImageTypeValue3=vImageType[2];
							if (!vImageType[2].equals("OTHER") && !vImageType[2].equals("MIXED")) {
								buffer.append(prefix); prefix=",";	
								buffer.append(vImageType[2]);
							}
							if (vImageType.length > 3) {
								if (!vImageType[3].equals("NONE") && !vImageType[3].equals("UNKNOWN") && !vImageType[3].equals("MIXED")) {
									buffer.append(prefix); prefix=",";	
									buffer.append(vImageType[3]);
								}
							}
						}
					}
				}
			}
			String vAcquisitionContrast=Attribute.getSingleStringValueOrNull(list,TagFromName.AcquisitionContrast);
			if (vAcquisitionContrast != null && !vAcquisitionContrast.equals("UNKNOWN")
			 && rememberImageTypeValue3 != null && !rememberImageTypeValue3.equals(vAcquisitionContrast)) {		// it is common for these to be the same
				buffer.append(prefix); prefix=",";	
				if (vAcquisitionContrast.equals("MIXED"))	{
					buffer.append("MIXED CONTRAST");
				}
				else {
					buffer.append(vAcquisitionContrast);
				}
			}
			String vVolumeBasedCalculationTechnique=Attribute.getSingleStringValueOrNull(list,TagFromName.VolumeBasedCalculationTechnique);
			if (vVolumeBasedCalculationTechnique != null && !vVolumeBasedCalculationTechnique.equals("NONE") && !vVolumeBasedCalculationTechnique.equals("MIXED")) {
				buffer.append(prefix); prefix=",";	
				buffer.append(vVolumeBasedCalculationTechnique);
			}
			String vPhotometricInterpretation=Attribute.getSingleStringValueOrNull(list,TagFromName.PhotometricInterpretation);
			if (vPhotometricInterpretation == null || vPhotometricInterpretation.equals("MONOCHROME1") || vPhotometricInterpretation.equals("MONOCHROME2")) {
				String vPixelPresentation=Attribute.getSingleStringValueOrNull(list,TagFromName.PixelPresentation);
				if (vPixelPresentation != null && !vPixelPresentation.equals("MONOCHROME")) {
					buffer.append(prefix); prefix=",";
					if (vPixelPresentation.equals("MIXED"))	{
						buffer.append("COLOR & MONOCHROME");
					}
					else {
						buffer.append(vPixelPresentation);
					}
				}
				// else say nothing for monochrome
			}
			else {
				buffer.append(prefix); prefix=",";
				buffer.append(vPhotometricInterpretation);	// e.g. RGB or PALETTE COLOR
			}
			
			buffer.append("}");	
		}
		{
			Attribute aImageOrientationPatient = list.get(TagFromName.ImageOrientationPatient);
			if (aImageOrientationPatient != null) {
				double[] vImageOrientationPatient = null;
				try {
					vImageOrientationPatient = aImageOrientationPatient.getDoubleValues();
				}
				catch (DicomException e) {
				}
				if (vImageOrientationPatient != null) {
					if (vImageOrientationPatient.length == 6) {
						buffer.append(" <");
						buffer.append(makeImageOrientationLabelFromImageOrientationPatient(
							vImageOrientationPatient[0],vImageOrientationPatient[1],vImageOrientationPatient[2],
							vImageOrientationPatient[3],vImageOrientationPatient[4],vImageOrientationPatient[5]));
						buffer.append(" ");	
						buffer.append(makePatientOrientationFromImageOrientationPatient(
							vImageOrientationPatient[0],vImageOrientationPatient[1],vImageOrientationPatient[2],
							vImageOrientationPatient[3],vImageOrientationPatient[4],vImageOrientationPatient[5]));
						buffer.append(">");	
					}
				}
			}
		}
		{
			String vImageComments=Attribute.getSingleStringValueOrNull(list,TagFromName.ImageComments);
			if (vImageComments != null) {
				buffer.append(" - ");	
				buffer.append(vImageComments);
			}
		}
		return buffer.toString();
	}
	

	/**
	 * <p>Get a human readable string meaningfully describing an image from a map of names and values such as from a database.</p>
	 *
	 * <p>This version is heavily tuned to describe MR multiframe images thoroughly.</p>
	 *
	 * @param	attributes	the map of upper case named attributes and their values (such as from a database query)
	 * @return			a human readable string meaningfully describing the image
	 */
	public static final String makeImageDescription(Map attributes) {
//System.err.println("DescriptionFactory.makeImageDescription(Map)");
		StringBuffer buffer = new StringBuffer();
		String useNumber=(String)(attributes.get("INCONCATENATIONNUMBER"));
		if (useNumber == null) useNumber=(String)(attributes.get("INSTANCENUMBER"));
		if (useNumber == null) useNumber="";
		buffer.append(useNumber);
		{
			String vNumberOfFrames=(String)(attributes.get("NUMBEROFFRAMES"));
			if (vNumberOfFrames != null && !vNumberOfFrames.equals("1")) {
				buffer.append(" [");
				buffer.append(vNumberOfFrames);
				buffer.append(" frames]");
			}
		}
		{
			buffer.append(" {");
			String prefix = "";
			String sopClassAbbreviation = null;
			{
				String sopClassUID = (String)(attributes.get("SOPCLASSUID"));
				if (sopClassUID != null) {
					sopClassAbbreviation = SOPClassDescriptions.getAbbreviationFromUID(sopClassUID);
					buffer.append(prefix); prefix=",";
					buffer.append(sopClassAbbreviation);
				}
			}
			String rememberImageTypeValue3 = null;
			String vImageType=(String)(attributes.get("IMAGETYPE"));
			if (vImageType != null) {
				StringTokenizer tokenizer = new StringTokenizer(vImageType,"\\");
				if (tokenizer.hasMoreTokens()) {
					String imageTypeValue1 = tokenizer.nextToken();
					if (imageTypeValue1.equals("DERIVED")) {		// do not bother appending ORIGINAL which is more common
						buffer.append(prefix); prefix=",";
						buffer.append(imageTypeValue1);	
					}
					if (tokenizer.hasMoreTokens()) {
						String imageTypeValue2 = tokenizer.nextToken();
						if (tokenizer.hasMoreTokens()) {
							rememberImageTypeValue3=tokenizer.nextToken();
							if (!rememberImageTypeValue3.equals("OTHER") && !rememberImageTypeValue3.equals("MIXED")) {
								buffer.append(prefix); prefix=",";	
								buffer.append(rememberImageTypeValue3);
							}
							if (tokenizer.hasMoreTokens()) {
								String imageTypeValue4 = tokenizer.nextToken();
								if (!imageTypeValue4.equals("NONE") && !imageTypeValue4.equals("UNKNOWN") && !imageTypeValue4.equals("MIXED")) {
									buffer.append(prefix); prefix=",";	
									buffer.append(imageTypeValue4);
								}
							}
						}
					}
				}
			}
			String vAcquisitionContrast=(String)(attributes.get("ACQUISITIONCONTRAST"));
			if (vAcquisitionContrast != null && !vAcquisitionContrast.equals("UNKNOWN")
			 && rememberImageTypeValue3 != null && !rememberImageTypeValue3.equals(vAcquisitionContrast)) {		// it is common for these to be the same
				buffer.append(prefix); prefix=",";	
				if (vAcquisitionContrast.equals("MIXED"))	{
					buffer.append("MIXED CONTRAST");
				}
				else {
					buffer.append(vAcquisitionContrast);
				}
			}
			String vVolumeBasedCalculationTechnique=(String)(attributes.get("VOLUMEBASEDCALCULATIONTECHNIQUE"));
			if (vVolumeBasedCalculationTechnique != null && !vVolumeBasedCalculationTechnique.equals("NONE") && !vVolumeBasedCalculationTechnique.equals("MIXED")) {
				buffer.append(prefix); prefix=",";	
				buffer.append(vVolumeBasedCalculationTechnique);
			}
			String vPhotometricInterpretation=(String)(attributes.get("PHOTOMETRICINTERPRETATION"));
			if (vPhotometricInterpretation == null || vPhotometricInterpretation.equals("MONOCHROME1") || vPhotometricInterpretation.equals("MONOCHROME2")) {
				String vPixelPresentation=(String)(attributes.get("PIXELPRESENTATION"));
				if (vPixelPresentation != null && !vPixelPresentation.equals("MONOCHROME")) {
					buffer.append(prefix); prefix=",";
					if (vPixelPresentation.equals("MIXED"))	{
						buffer.append("COLOR & MONOCHROME");
					}
					else {
						buffer.append(vPixelPresentation);
					}
				}
				// else say nothing for monochrome
			}
			else {
				buffer.append(prefix); prefix=",";
				buffer.append(vPhotometricInterpretation);	// e.g. RGB or PALETTE COLOR
			}
			
			buffer.append("}");	
		}
		{
			String vImageOrientationPatient=(String)(attributes.get("IMAGEORIENTATIONPATIENT"));
//System.err.println("vImageOrientationPatient = "+vImageOrientationPatient);
			if (vImageOrientationPatient != null) {
				try {
					StringTokenizer tokenizer = new StringTokenizer(vImageOrientationPatient,"\\");
					if (tokenizer.hasMoreTokens()) {
						double rowX = Double.parseDouble(tokenizer.nextToken());
						if (tokenizer.hasMoreTokens()) {
							double rowY = Double.parseDouble(tokenizer.nextToken());
							if (tokenizer.hasMoreTokens()) {
								double rowZ = Double.parseDouble(tokenizer.nextToken());
								if (tokenizer.hasMoreTokens()) {
									double colX = Double.parseDouble(tokenizer.nextToken());
									if (tokenizer.hasMoreTokens()) {
										double colY = Double.parseDouble(tokenizer.nextToken());
										if (tokenizer.hasMoreTokens()) {
											double colZ = Double.parseDouble(tokenizer.nextToken());
											buffer.append(" <");
											buffer.append(makeImageOrientationLabelFromImageOrientationPatient(
												rowX,rowY,rowZ,colX,colY,colZ));
											buffer.append(" ");	
											buffer.append(makePatientOrientationFromImageOrientationPatient(
												rowX,rowY,rowZ,colX,colY,colZ));
											buffer.append(">");	
										}
									}	
								}
							}
						}
					}
				}
				catch (NumberFormatException e) {
				}
			}
		}
		{
			String vImageComments=(String)(attributes.get("IMAGECOMMENTS"));
			if (vImageComments != null) {
				buffer.append(" - ");	
				buffer.append(vImageComments);
			}
		}
		return buffer.toString();
	}
	
	/**
	 * <p>Get a human readable string meaningfully describing a series from an attribute list such as from a directory record.</p>
	 *
	 * @param	list	the list of attributes (such as from a SERIES level directory record)
	 * @return		a human readable string meaningfully describing the series
	 */
	public static final String makeSeriesDescription(AttributeList list) {
//System.err.println("DescriptionFactory.makeSeriesDescription(AttributeList)");
		StringBuffer buffer = new StringBuffer();
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber));
		buffer.append(" ");
		String modality = Attribute.getSingleStringValueOrNull(list,TagFromName.Modality);
		if (modality != null ) {
			buffer.append("{");
			buffer.append(modality);
			buffer.append("} ");
		}
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDescription));
		return buffer.toString();
	}
	

	/**
	 * <p>Get a human readable string meaningfully describing a series from an attribute list such as from a directory record.</p>
	 *
	 * @param	attributes	the map of upper case named attributes and their values (such as from a database query)
	 * @return			a human readable string meaningfully describing the series
	 */
	public static final String makeSeriesDescription(Map attributes) {
//System.err.println("DescriptionFactory.makeSeriesDescription(Map)");
		StringBuffer buffer = new StringBuffer();
		String seriesNumber = (String)(attributes.get("SERIESNUMBER"));
		if (seriesNumber != null) {
			buffer.append(seriesNumber);
			buffer.append(" ");
		}
		String modality = (String)(attributes.get("MODALITY"));
		if (modality != null ) {
			buffer.append("{");
			buffer.append(modality);
			buffer.append("} ");
		}
		String seriesDescription = (String)(attributes.get("SERIESDESCRIPTION"));
		if (seriesDescription != null) {
			buffer.append(seriesDescription);
		}
		return buffer.toString();
	}

	/**
	 * <p>Get a human readable string meaningfully describing a patient from an attribute list such as from a database or directory record.</p>
	 *
	 * @param	attributes	the map of upper case named attributes and their values (such as from a database query)
	 * @return			a human readable string meaningfully describing the patient
	 */
	public static final String makePatientDescription(Map attributes) {
//System.err.println("DescriptionFactory.makePatientDescription(Map)");
		StringBuffer buffer = new StringBuffer();
		{
			String //patientName = (String)(attributes.get("PM_PATIENTNAME_CANONICAL"));
			//if (patientName == null) {
				patientName = (String)(attributes.get("PATIENTNAME"));
			//}
			if (patientName == null) {
				patientName = "";					// want to treat null and empty patient name the same way
			}
			else {
				if (patientName.contains("^")) {
//System.err.println("DescriptionFactory.makePatientDescription(Map): patientName was "+patientName);
					patientName = patientName.replaceFirst("\\^+$","");	// Trailing empty components are of no significance so should be treated as absent
//System.err.println("DescriptionFactory.makePatientDescription(Map): patientName now "+patientName);
				}
				patientName = patientName.trim();	// want to treat empty patient name, patient name with spaces the same way
			}
			if (patientName.length() > 0) {
				buffer.append(patientName);
				buffer.append(" ");
			}
		}
		{
			String patientID = (String)(attributes.get("PATIENTID"));
			if (patientID == null) {
				patientID = "";
			}
			else {
				patientID = patientID.trim();
			}
			if (patientID.length() > 0) {
				buffer.append(patientID);
				buffer.append(" ");
			}
		}
		return buffer.toString();
	}
	
}



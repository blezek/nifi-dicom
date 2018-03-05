/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.*;
import com.pixelmed.display.*;

import java.io.*;
import java.awt.*; 
import java.awt.color.*; 
import java.awt.image.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class of static methods to convert non-RGB (e.g., YBR_FULL) to RGB true color images.</p>
 *
 * @author	dclunie
 */
public class ConvertNonRGBToRGB {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/ConvertNonRGBToRGB.java,v 1.9 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ConvertNonRGBToRGB.class);

	/**
	 * <p>Read a DICOM color image input format file and change the photometric interpretation of the the encoded pixel data to RGB.</p>
	 *
	 * @param	inputFileName	the input file name
	 * @param	outputFileName	the output file name
	 */
	public ConvertNonRGBToRGB(String inputFileName,String outputFileName) throws DicomException, FileNotFoundException, IOException {
		AttributeList list = new AttributeList();
		DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
		list.read(in);
		in.close();
		
		int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsAllocated,0);
		if (bitsAllocated != 8) {
			throw new DicomException("Input image does not have 8 bits per sample");
		}
		
		int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.SamplesPerPixel,0);
		if (samplesPerPixel != 3) {
			throw new DicomException("Input image does not have three samples per pixel");
		}
		
		int planarConfiguration = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PlanarConfiguration,-1);
		if (planarConfiguration < 0 || planarConfiguration > 1) {
			throw new DicomException("Input image has missing or invalid planar configuration");
		}
		boolean srcIsByPixel = planarConfiguration == 0;
		
		String photometricInterpretation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PhotometricInterpretation);
		if (!photometricInterpretation.equals("YBR_FULL")) {
			throw new DicomException("Input image is not YBR_FULL");
		}
		
		int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
		int columns = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		int rows = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);

		int pixelsPerFrame = rows*columns;
		int samplesPerFrame = pixelsPerFrame*samplesPerPixel;
		int samplesLength = samplesPerFrame*numberOfFrames;
		//if (samplesLength%2 != 0) {
		//	++samplesLength;
		//}

		// fetching of bytes from PixelData is copied from SourceImage class ...
		byte srcSamples[] = null;
		{
			Attribute a = list.getPixelData();
			if (ValueRepresentation.isOtherByteVR(a.getVR())) {
				srcSamples = a.getByteValues();
			}
			else {
				short sdata[] = a.getShortValues();
				srcSamples = new byte[samplesLength];
				int slen=samplesLength/2;
				int scount=0;
				int count=0;
				while (scount<slen) {
					int value=((int)sdata[scount++])&0xffff;	// the endianness of the TS has already been accounted for
					int value1=value&0xff;						// now just unpack from low part of word first
					srcSamples[count++]=(byte)value1;
					int value2=(value>>8)&0xff;
					srcSamples[count++]=(byte)value2;
				}
			}
		}
		
		byte[] dstSamples = new byte[samplesLength];
		
		int[] indexesForOnePixel = new int[samplesPerPixel];
		
		byte[] convertedRGB = new byte[3];
		
		int frameOffset = 0;
		for (int frame=0; frame<numberOfFrames; ++frame) {
			for (int pixel=0; pixel<pixelsPerFrame; ++pixel) {
				for (int sample=0; sample<samplesPerPixel; ++sample) {
					int byPixelIndex = frameOffset + pixel * samplesPerPixel +                  sample;
					int byPlaneIndex = frameOffset + pixel                   + pixelsPerFrame * sample;
					indexesForOnePixel[sample] = srcIsByPixel ? byPixelIndex : byPlaneIndex;
				}
				
				BufferedImageUtilities.convertYBRToRGB(srcSamples[indexesForOnePixel[0]],srcSamples[indexesForOnePixel[1]],srcSamples[indexesForOnePixel[2]],convertedRGB);
								
				dstSamples[indexesForOnePixel[0]] = convertedRGB[0];
				dstSamples[indexesForOnePixel[1]] = convertedRGB[1];
				dstSamples[indexesForOnePixel[2]] = convertedRGB[2];
			}
			frameOffset += samplesPerFrame;
		}
				
		Attribute pixelData = new OtherByteAttribute(TagFromName.PixelData);
		pixelData.setValues(dstSamples);
		list.put(pixelData);

		{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue("RGB"); list.put(a); }
				
		ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true/*retainExistingItems*/,
					new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.ConvertNonRGBToRGB.main()",			// Manufacturer's Model Name
					null,															// Device Serial Number
					VersionAndConstants.getBuildDate(),								// Software Version(s)
					"Converted "+photometricInterpretation+" to RGB");

		list.removeMetaInformationHeaderAttributes();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a DICOM image input format file with a Photometric Interpretation of non-RGB (e.g., YBR_FULL), and from it create a DICOM image of Photometric Interpretation RGB.</p>
	 *
	 * @param	arg	two parameters, the inputFile, outputFile
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				new ConvertNonRGBToRGB(arg[0],arg[1]);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: ConvertNonRGBToRGB inputFile outputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

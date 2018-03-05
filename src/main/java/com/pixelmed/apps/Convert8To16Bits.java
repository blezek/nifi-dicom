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
 * <p>A class of methods to convert 8 to 16 bit gray scale images.</p>
 *
 * @author	dclunie
 */
public class Convert8To16Bits {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/Convert8To16Bits.java,v 1.6 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(Convert8To16Bits.class);

	/**
	 * <p>Read a DICOM 8 bit grayscale image input format file and change the bit depth.</p>
	 *
	 * @param	inputFileName		the input file name
	 * @param	outputFileName		the output file name
	 * @param	outputBitsStored	less than or equal to 16
	 */
	public Convert8To16Bits(String inputFileName,String outputFileName,int outputBitsStored) throws DicomException, FileNotFoundException, IOException {
		if (outputBitsStored > 16) {
			throw new DicomException("Specified output BitsStored is > 16 bits");
		}
				
		AttributeList list = new AttributeList();
		DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
		list.read(in);
		in.close();
		
		int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsAllocated,8);
		if (bitsAllocated != 8) {
			throw new DicomException("Input image does not have 8 bits per sample");
		}
				
		int bitsStored = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsStored,8);
		if (bitsStored > bitsAllocated) {
			throw new DicomException("Input image has BitsStored > BitsAllocated");
		}
				
		int highBit = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.HighBit,7);
		if (highBit != bitsStored - 1) {
			throw new DicomException("Input image does not have HighBit == BitsStored - 1");
		}
				
		int pixelRepresentation = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0);
		if (pixelRepresentation != 0) {
			throw new DicomException("Input image is signed (PixelRepresentation != 0) - not supported");
		}
				
		int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.SamplesPerPixel,1);
		
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
		
		short[] dstSamples = new short[samplesLength];
		
		for (int i=0; i<samplesLength; ++i) {
			dstSamples[i] = (short)(srcSamples[i] & 0xff);
		}
				
		Attribute pixelData = new OtherWordAttribute(TagFromName.PixelData);
		pixelData.setValues(dstSamples);
		list.put(pixelData);

		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(16); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(outputBitsStored); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(outputBitsStored-1); list.put(a); }
				
		ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true/*retainExistingItems*/,
					new CodedSequenceItem("109103","DCM","Modifying Equipment"),
					"PixelMed",														// Manufacturer
					"PixelMed",														// Institution Name
					"Software Development",											// Institutional Department Name
					"Bangor, PA",													// Institution Address
					null,															// Station Name
					"com.pixelmed.apps.Convert8To16Bits.main()",					// Manufacturer's Model Name
					null,															// Device Serial Number
					VersionAndConstants.getBuildDate(),								// Software Version(s)
					"Converted 8 to 16 bits");

		list.removeMetaInformationHeaderAttributes();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a DICOM a image input format file with a BitsAllocated of 8, and from it create a DICOM image with a BitsAllocated of 16.</p>
	 *
	 * @param	arg	three parameters, the inputFile, outputFile, and the bitsStored value to use in the output
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 3) {
				new Convert8To16Bits(arg[0],arg[1],Integer.parseInt(arg[2]));
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: Convert8To16Bits inputFile outputFile bitsStored");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class of static methods to copy a DICOM image and retain only enough to describe pixels.</p>
 *
 * <p>Retains the Pixel Data Module and whatever else is barely enough to describe the image (Number of Frames, SOP Class and Instance UIDs).</p>
 *
 * @author	dclunie
 */
public class KeepOnlyImagePixelModule {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/KeepOnlyImagePixelModule.java,v 1.6 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(KeepOnlyImagePixelModule.class);

	protected static AttributeTag[] relevantImagePixelModuleAttributeTags = {
		TagFromName.SamplesPerPixel,
		TagFromName.PhotometricInterpretation,
		TagFromName.Rows,
		TagFromName.Columns,
		TagFromName.BitsAllocated,
		TagFromName.BitsStored,
		TagFromName.HighBit,
		TagFromName.PixelRepresentation,
		TagFromName.PixelData,
		TagFromName.PlanarConfiguration,
		TagFromName.PixelAspectRatio,
		TagFromName.SmallestImagePixelValue,
		TagFromName.LargestImagePixelValue,
		TagFromName.RedPaletteColorLookupTableDescriptor,
		TagFromName.GreenPaletteColorLookupTableDescriptor,
		TagFromName.BluePaletteColorLookupTableDescriptor,
		TagFromName.RedPaletteColorLookupTableData,
		TagFromName.GreenPaletteColorLookupTableData,
		TagFromName.BluePaletteColorLookupTableData,
		TagFromName.ICCProfile,
		TagFromName.PixelDataProviderURL, 
		TagFromName.PixelPaddingRangeLimit

	};

	protected static AttributeTag[] relevantMultiFrameModuleAttributeTags = {
		TagFromName.NumberOfFrames
	};

	protected static AttributeTag[] relevantGeneralSeriesModuleAttributeTags = {
		TagFromName.PixelPaddingValue
	};

	protected static AttributeTag[] relevantSOPCommonModuleAttributeTags = {
		TagFromName.SOPClassUID,
		TagFromName.SOPInstanceUID
	};
	
	protected static AttributeTag[][] relevantModules = {
		relevantImagePixelModuleAttributeTags,
		relevantMultiFrameModuleAttributeTags,
		relevantGeneralSeriesModuleAttributeTags,
		relevantSOPCommonModuleAttributeTags
	};
	
	/**
	 * <p>Read a DICOM image input file and discard everything except what is required to describe the pixels</p>
	 *
	 * @param	inputFileName	the input file name
	 * @param	outputFileName	the output file name
	 */
	public KeepOnlyImagePixelModule(String inputFileName,String outputFileName) throws DicomException, FileNotFoundException, IOException {
		AttributeList list = new AttributeList();
		DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
		list.read(in);
		in.close();
		
		AttributeList newList = new AttributeList();
		for (AttributeTag[] tags : relevantModules) {
			for (AttributeTag tag : tags) {
				Attribute a = list.get(tag);
				if (a != null) {
					newList.put(a);
				}
			}
		}
		FileMetaInformation.addFileMetaInformation(newList,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		newList.write(outputFileName,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a DICOM image input file and discard everything except what is required to describe the pixels.</p>
	 *
	 * @param	arg	two parameters, the inputFile, outputFile
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				new KeepOnlyImagePixelModule(arg[0],arg[1]);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: KeepOnlyImagePixelModule inputFile outputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

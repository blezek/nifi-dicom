/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import java.io.FileOutputStream;

import java.text.DecimalFormat;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to read a DICOM image input format file encoded in a compressed transfer syntax and extract the compressed bitstreams into a file for each frame.</p>
 *
 * @author	dclunie
 */
public class UnencapsulateCompressedPixelData {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/UnencapsulateCompressedPixelData.java,v 1.6 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(UnencapsulateCompressedPixelData.class);

	private static String getFileNameExtensionForCompressedPixelData(AttributeList list) {
		String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
		TransferSyntax ts = new TransferSyntax(transferSyntaxUID);
		return ts.getFileNameExtension();
	}

	/**
	 * <p>Read a DICOM image input format file encoded in a compressed transfer syntax and extract the compressed bitstreams into a file for each frame.</p>
	 *
	 * <p>The output file will be named with the specified prefix to which will be appended a 6 digit zero padded frame number starting from 1, followed by an appropriate file name extension for the transfer syntax.</p>
	 *
	 * @param	arg	two parameters, the inputFile, the outputFile prefix
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				DecimalFormat sixDigitZeroPaddedFormat = new DecimalFormat("000000");
				AttributeList list = new AttributeList();
				list.setDecompressPixelData(false);
				list.read(arg[0]);
				String fileNameExtension = getFileNameExtensionForCompressedPixelData(list);
				OtherByteAttributeMultipleCompressedFrames aPixelData = (OtherByteAttributeMultipleCompressedFrames)(list.getPixelData());
				byte[][] frames = aPixelData.getFrames();
				for (int f=0; f<frames.length; ++f) {
					String outputFilename = arg[1] + sixDigitZeroPaddedFormat.format(f+1) + "." + fileNameExtension;
					slf4jlogger.info("Writing {}",outputFilename);
					FileOutputStream o = new FileOutputStream(outputFilename);
					o.write(frames[f]);
					o.close();
				}
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: UnencapsulateCompressedPixelData inputFile outputFilePrefix");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}

}


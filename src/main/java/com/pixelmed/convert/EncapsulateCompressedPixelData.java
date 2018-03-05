package com.pixelmed.convert;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import java.io.File;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to create a DICOM image format file encoded in a compressed transfer syntax with the compressed bitstreams supplied from files.</p>
 *
 * @author	dclunie
 */
public class EncapsulateCompressedPixelData {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/EncapsulateCompressedPixelData.java,v 1.2 2016/02/04 08:59:59 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(EncapsulateCompressedPixelData.class);

	/**
	 * <p>Create a DICOM image format file encoded in a compressed transfer syntax with the compressed bitstreams supplied from files.</p>
	 *
	 * <p>The frames will be created in the order of the input file names on the command line.</p>
	 *
	 * <p>The input DICOM file with the header is expected to have the correct Transfer Syntax UID and Pixel Data Module attribute values; any PixelData is discarded.</p>
	 *
	 * @param	arg	three or more parameters, the outputFile, an input DICOM file with the header, and one or more input files each containing a compressed bit stream
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 3) {
				AttributeList list = new AttributeList();
				list.read(arg[1]);
				list.remove(TagFromName.PixelData);		// just in case
				
				int numberOfFrames = arg.length - 2;
				File[] frameFiles = new File[numberOfFrames];
				for (int f=0; f<numberOfFrames; ++f) {
					frameFiles[f] = new File(arg[f+2]);
				}
				
				String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
				if (transferSyntaxUID.length() == 0) {
					throw new Exception("Missing TransferSyntaxUID in input file");
				}
				
				list.removeGroupLengthAttributes();
				// do NOT removeMetaInformationHeaderAttributes() ... else missing TransferSyntaxUID (do not know why list.write() does not add it)
				list.remove(TagFromName.DataSetTrailingPadding);
				
 				OtherByteAttributeMultipleCompressedFrames aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,frameFiles);
				list.put(aPixelData);
				
				list.write(arg[0],transferSyntaxUID,true/*useMeta*/,true/*useBufferedStream*/);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: EncapsulateCompressedPixelData outputFile dicomHeaderSourceFile inputFrame1 [inputFrame2 ...]");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}

}


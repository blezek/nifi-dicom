/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * <p>A class to categorize DICOM images as having been lossy compressed or not.</p>
 *
 * @author	dclunie
 */
public class LossyImageCompression {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/LossyImageCompression.java,v 1.9 2017/01/24 10:50:37 dclunie Exp $";
	
	/**
	 * <p>determine if an image has ever been lossy compressed.</p>
	 *
	 * @param	list	list of attributes representing a DICOM image
	 * @return		true if has ever been lossy compressed
	 */
	public static boolean hasEverBeenLossyCompressed(AttributeList list) {
		// ignore the fact that the LossyImageCompression is supposed to be a string with values "00" or "01" ... works this way even if (incorrectly) a numeric
		// ignore the fact that LossyImageCompressionRatio may be multi-valued
		// ignore the fact that LossyImageCompressionMethod may be multi-valued
//System.err.println("Checking LossyImageCompression "+Attribute.getSingleIntegerValueOrDefault(list,TagFromName.LossyImageCompression,-1));
//System.err.println("Checking LossyImageCompressionRatio "+Attribute.getSingleDoubleValueOrDefault(list,TagFromName.LossyImageCompressionRatio,-1));
//System.err.println("Checking LossyImageCompressionMethod length "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.LossyImageCompressionMethod).length());
//System.err.println("Checking DerivationDescription "+Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.DerivationDescription).toLowerCase(java.util.Locale.US).indexOf("lossy"));
//System.err.println("Checking TransferSyntaxUID = "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID));
//System.err.println("Checking TransferSyntaxUID lossy "+new TransferSyntax(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID)).isLossy());
		return Attribute.getSingleIntegerValueOrDefault(list,TagFromName.LossyImageCompression,-1) > 0
		    || Attribute.getSingleDoubleValueOrDefault(list,TagFromName.LossyImageCompressionRatio,-1) > 1.0
		    || Attribute.getSingleStringValueOrEmptyString(list,TagFromName.LossyImageCompressionMethod).length() > 0
		    || Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.DerivationDescription).toLowerCase(java.util.Locale.US).indexOf("lossy") > -1
		    || new TransferSyntax(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID)).isLossy()
		    ;
	}

	/**
	 * <p>Describe the nature of lossy compressed that has ever been applied to an image.</p>
	 *
	 * @param	list	list of attributes representing a DICOM image	
	 * @return		a string describing the compression, including method and ratio if possible, or a zero length string if never lossy compressed
	 */
	public static String describeLossyCompression(AttributeList list) {
		String value;
		if (LossyImageCompression.hasEverBeenLossyCompressed(list)) {
			String ratio = Attribute.getSingleStringValueOrNull(list,TagFromName.LossyImageCompressionRatio);
			if (ratio != null) {
				ratio = ratio.replaceFirst("0*$","");
				ratio = ratio.replaceFirst("[.]$","");
			}
			String method = Attribute.getSingleStringValueOrNull(list,TagFromName.LossyImageCompressionMethod);
			if (method != null) {
				if (method.equals("ISO_10918_1")) {
					method="JPEG";
				}
				else if (method.equals("ISO_14495_1")) {
					method="JLS";
				}
				else if (method.equals("ISO_15444_1")) {
					method="J2K";
				}
				else if (method.equals("ISO_13818_2")) {
					method="MPEG2";
				}
			}
			if (method == null && ratio == null) {
				value = "Lossy";
			}
			else if (ratio == null) {
				value = "Lossy "+method;
			}
			else if (method == null) {
				value = "Lossy "+ratio+":1";
			}
			else {
				value = method+" "+ratio+":1";
			}
		}
		else {
			value = "";
		}
		return value;
	}

	/**
	 * <p>Read a DICOM image input file, and determine if it has ever been lossy compressed.</p>
	 *
	 * @param	arg	one required parameters, the input file name
	 */
	public static void main(String arg[]) {
		String dicomFileName = arg[0];
		try {
			AttributeList list = new AttributeList();
			DicomInputStream in = new DicomInputStream(new BufferedInputStream(new FileInputStream(dicomFileName)));
			list.read(in,TagFromName.PixelData);
			in.close();
			System.out.println(hasEverBeenLossyCompressed(list));	// no need to use SLF4J since command line utility/test
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


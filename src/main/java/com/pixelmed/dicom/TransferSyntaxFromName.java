/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>A class to lookup the DICOM Transfer Syntax UID from a string name.</p>
 *
 * @author	dclunie
 */
public class TransferSyntaxFromName {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/TransferSyntaxFromName.java,v 1.8 2017/01/24 10:50:39 dclunie Exp $";
	
	static protected Map map = new HashMap();

	static {
		map.put("ImplicitVRLittleEndian",TransferSyntax.ImplicitVRLittleEndian);
		map.put("ExplicitVRLittleEndian",TransferSyntax.ExplicitVRLittleEndian);
		map.put("ExplicitVRBigEndian",TransferSyntax.ExplicitVRBigEndian);
		map.put("Default",TransferSyntax.Default);
		map.put("DeflatedExplicitVRLittleEndian",TransferSyntax.DeflatedExplicitVRLittleEndian);
		map.put("JPEGBaseline",TransferSyntax.JPEGBaseline);
		map.put("JPEGExtended",TransferSyntax.JPEGExtended);
		map.put("JPEGLossless",TransferSyntax.JPEGLossless);
		map.put("JPEGLosslessSV1",TransferSyntax.JPEGLosslessSV1);
		map.put("JPEGLS",TransferSyntax.JPEGLS);
		map.put("JPEGNLS",TransferSyntax.JPEGNLS);
		map.put("JPEG2000Lossless",TransferSyntax.JPEG2000Lossless);
		map.put("JPEG2000",TransferSyntax.JPEG2000);
		map.put("MPEG2MPML",TransferSyntax.MPEG2MPML);
		map.put("MPEG2MPHL",TransferSyntax.MPEG2MPHL);
		map.put("MPEG4HP41",TransferSyntax.MPEG4HP41);
		map.put("MPEG4HP41BD",TransferSyntax.MPEG4HP41BD);
		map.put("MPEG4HP422D",TransferSyntax.MPEG4HP422D);
		map.put("MPEG4HP423D",TransferSyntax.MPEG4HP423D);
		map.put("MPEG4HP42ST",TransferSyntax.MPEG4HP42ST);
		map.put("RLE",TransferSyntax.RLE);
		map.put("PixelMedBzip2ExplicitVRLittleEndian",TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian);
		map.put("PixelMedEncapsulatedRawLittleEndian",TransferSyntax.PixelMedEncapsulatedRawLittleEndian);
		map.put("Papyrus3ImplicitVRLittleEndian",TransferSyntax.Papyrus3ImplicitVRLittleEndian);
	}
	
	private TransferSyntaxFromName() {
	}
	
	/**
	 * <p>Get the Transfer Syntax UID from the name.</p>
	 *
	 * @param		name	a string name of the transfer syntax
	 * @return				the UID if found, else the supplied argument if of UID form, else null
	 */
	static public String getUID(String name)	{
		String uid = null;
		if (name != null) {
			uid = (String)(map.get(name));
			if (uid == null) {
				// if string is a UID form, just return itself
				if (name.matches("[0-9.][0-9.]*")) {
					uid=name;
				}
			}
		}
		return uid;
	}
	
	/**
	 * <p>Test.</p>
	 *
	 * @param	arg	none
	 */
	public static void main(String arg[]) {
		System.err.println("Default from name: "+(TransferSyntaxFromName.getUID("Default").equals(TransferSyntax.Default)));	// no need to use SLF4J since command line utility/test
		System.err.println("Default from uid : "+(TransferSyntaxFromName.getUID(TransferSyntax.Default).equals(TransferSyntax.Default)));
		System.err.println("Dummy from name  : "+(TransferSyntaxFromName.getUID("Dummy") == null));
	}

}


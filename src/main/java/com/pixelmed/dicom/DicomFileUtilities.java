/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>Various static methods helpful for handling DICOM files.</p>
 *
 * @author	dclunie
 */
public class DicomFileUtilities {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomFileUtilities.java,v 1.7 2017/01/24 10:50:37 dclunie Exp $";

	private DicomFileUtilities() {}
	
	// derived from BinaryInputStream.extractUnsigned16()
	final static int extractUnsigned16(byte[] buffer,int offset,boolean bigEndian) {
			short v1 =  (short)(buffer[offset+0]&0xff);
			short v2 =  (short)(buffer[offset+1]&0xff);
			return (short) (bigEndian
				? (v1 << 8) | v2
				: (v2 << 8) | v1);
	}

	// derived from BinaryInputStream.extractUnsigned32()
	final static long extractUnsigned32(byte[] buffer,int offset,boolean bigEndian) {
			long v1 =  ((long)buffer[offset+0])&0xff;
			long v2 =  ((long)buffer[offset+1])&0xff;
			long v3 =  ((long)buffer[offset+2])&0xff;
			long v4 =  ((long)buffer[offset+3])&0xff;
			return bigEndian
				? (((((v1 << 8) | v2) << 8) | v3) << 8) | v4
				: (((((v4 << 8) | v3) << 8) | v2) << 8) | v1;
	}

	/**
	 * <p>Does the file contain a DICOM (or DICOM-like ACR-NEMA) dataset ?</p>
	 *
	 * <p>Any exceptions during attempts to read are caught and false returned.</p>
	 *
	 * @param	filename	the file
	 * @return				true if file exists, can be read, and seems to contain a DICOM or ACR-NEMA dataset (with or without a PS 3.10 preamble and meta information header)
	 */
	public static boolean isDicomOrAcrNemaFile(String filename) {
		return isDicomOrAcrNemaFile(new File(filename));
	}
	
	/**
	 * <p>Does the file contain a DICOM (or DICOM-like ACR-NEMA) dataset ?</p>
	 *
	 * <p>Any exceptions during attempts to read are (silently) caught and false returned.</p>
	 *
	 * <p>Note that this method may return true but {@link com.pixelmed.dicom.DicomInputStream DicomInputStream} and {@link com.pixelmed.dicom.AttributeList AttributeList} may fail
	 * to read the file, since it may be "bad" ways that are not supported, e.g., missing TransferSyntaxUID in meta information header, etc.</p>
	 *
	 * <p>Will detect files with:</p>
	 * <ul>
	 * <li>PS 3.10 meta-header (even if in invalid big-endian transfer syntax and even if 1st meta information header attribute is not group length)</li>
	 * <li>no meta-header but in little or big endian and explicit or implicit VR starting with a group 0x0008 attribute element &lt;= 0x0018 (SOPInstanceUID)</li>
	 * </ul>
	 * <p>Will reject everything else, including files with:</p>
	 * <ul>
	 * <li>no meta-header and not starting with a group 0x0008 attribute, e.g. that have command (group 0x0000) elements at the start of the dataset</li>
	 * </ul>
	 *
	 * @param	file	the file
	 * @return			true if file exists, can be read, and seems to contain a DICOM or ACR-NEMA dataset (with or without a PS 3.10 preamble and meta information header)
	 */
	public static boolean isDicomOrAcrNemaFile(File file) {
		boolean success = false;
		FileInputStream fi = null;
		try {
			fi = new FileInputStream(file);
			InputStream in = new BufferedInputStream(fi);
			byte[] b = new byte[160];
			int length = 0;
			{											// derived from BinaryInputStream.readInsistently()
				int wanted = 160;
				while (wanted > 0) {
					int got = in.read(b,length,wanted);
					if (got == -1) break;
					wanted-=got;
					length+=got;
				}
			}
			fi.close();
			fi = null;

			if (length >= 136 && new String(b,128,4).equals("DICM") && extractUnsigned16(b,132,false) == 0x0002 /*&& extractUnsigned16(b,134,false) == 0x0000*/) {	// do NOT insist on group length (bad example dicomvl.imagAAAa0005r.dc3)
				success = true;
			}
			else if (length >= 136 && new String(b,128,4).equals("DICM") && extractUnsigned16(b,132,true) == 0x0002 /*&& extractUnsigned16(b,134,true) == 0x0000*/) {	// big endian metaheader is illegal but allow it (bad example dicomvl.fich1.dcm)
				success = true;
			}
			else if (length >= 8
			  && extractUnsigned16(b,0,false) == 0x0008
			  && extractUnsigned16(b,2,false) <= 0x0018 /* SOPInstanceUID */
			  && (extractUnsigned32(b,4,false) <= 0x0100 /* sane VL */ || (Character.isUpperCase((char)(b[4])) && Character.isUpperCase((char)(b[5]))) /* EVR */)
			) {
				success = true;
			}
			else if (length >= 8
			  && extractUnsigned16(b,0,true) == 0x0008
			  && extractUnsigned16(b,2,true) <= 0x0018 /* SOPInstanceUID */
			  && (extractUnsigned32(b,4,true) <= 0x0100 /* sane VL */ || (Character.isUpperCase((char)(b[4])) && Character.isUpperCase((char)(b[5]))) /* EVR */)
			) {
				success = true;
			}
			// do not check for start with command group (e.g. acrnema/xpress/test.inf)
		}
		catch (Exception e) {
			success = false;
			try {
				if (fi != null) {
					fi.close();
				}
			}
			catch (IOException ioe) {
			}
		}
		return success;
	}

	
	/**
	 * <p>Iterate through a list of files and report which are DICOM and which are not.</p>
	 *
	 * @param	arg	a list of files to test
	 */
	public static void main(String arg[]) {
		try {
			for (String filename : arg) {
				System.err.println("File "+filename+" isDicomOrAcrNemaFile() = "+isDicomOrAcrNemaFile(filename));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class copies a set of DICOM files, decompressing them if compressed.</p>
 *
 * @author	dclunie
 */
public class DecompressDicomFiles extends MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/DecompressDicomFiles.java,v 1.7 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DecompressDicomFiles.class);

	protected String outputPath;
	
	public DecompressDicomFiles(MessageLogger logger) {
		super(logger);
	}
	
	/**
	 * <p>Check for valid information, and that the file is not compressed or not a suitable storage object for import.</p>
	 *
	 * @param	sopClassUID
	 * @param	transferSyntaxUID
	 */
	protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
		return sopClassUID != null
		    && (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
		    && transferSyntaxUID != null;
	}

	/**
	 * <p>Do something with the referenced DICOM file that has been encountered.</p>
	 *
	 * <p>This method needs to be implemented in a sub-class to do anything useful.
	 * The default method does nothing.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
		//logLn("MediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
		slf4jlogger.info("MediaImporter.doSomethingWithDicomFile(): {}",mediaFileName);
		try {
			AttributeList list = new AttributeList();
			list.read(mediaFileName);
			list.removeGroupLengthAttributes();
			list.removeMetaInformationHeaderAttributes();
			list.remove(TagFromName.DataSetTrailingPadding);
			list.correctDecompressedImagePixelModule();
			list.insertLossyImageCompressionHistoryIfDecompressed();
			FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
			String outputFileName = Attribute.getSingleStringValueOrDefault(list,TagFromName.SOPInstanceUID,"NONAME");
			list.write(new File(outputPath,outputFileName),TransferSyntax.ExplicitVRLittleEndian,true,true);
		}
		catch (Exception e) {
			slf4jlogger.error("While processing "+mediaFileName+" ",e);
		}
	}
	
	/**
	 * <p>Copy a set of DICOM files, decompressing them if compressed.</p>
	 *
	 * @param	arg	array of two strings - the input path and the output path
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				DecompressDicomFiles importer = new DecompressDicomFiles(new PrintStreamMessageLogger(System.err));
				importer.outputPath = arg[1];
				importer.importDicomFiles(arg[0]);
			}
			else {
				throw new Exception("Argument list must be zero or one value");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.BinaryOutputStream;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CompressedFrameDecoder;
import com.pixelmed.dicom.CompressedFrameEncoder;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UnsignedShortAttribute;

import com.pixelmed.display.SourceImage;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class copies a set of DICOM image files, compressing them losslessly with JPEG 2000 (default), JPEG 10918-1 Lossless Huffman SV1, JPEG-LS or RLE.</p>
 *
 * @author	dclunie
 */
public class CompressDicomFiles extends MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/CompressDicomFiles.java,v 1.17 2017/03/09 15:40:13 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompressDicomFiles.class);

	protected String outputPath;
	protected String outputFormat;
	protected String transferSyntaxUID;
	protected boolean reuseSameBaseFileName;
	
	public CompressDicomFiles(MessageLogger logger) {
		super(logger);
		{
			String osname = System.getProperty("os.name");
			if (osname != null && osname.toLowerCase().startsWith("windows")) {
				slf4jlogger.info("disabling memory mapping for SourceImage on Windows platform");
				SourceImage.setAllowMemoryMapping(false);	// otherwise problems with redacting large
			}
		}
	}
	
	/**
	 * <p>Is the DICOM file OK to compress?</p>
	 *
	 * @param	sopClassUID			the SOP Class UID of the file
	 * @param	transferSyntaxUID	the Transfer Syntax UID of the file
	 * @return						true if is suitable
	 */
	protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
		return sopClassUID != null
		    && SOPClass.isImageStorage(sopClassUID)
		    && transferSyntaxUID != null;
	}

	/**
	 * <p>Log that file cannot be compressed.</p>
	 *
	 * <p>A subclass could do something more creative, like copy the file to a new file without recompressing it to make sure the set of files remains complete.</p>
	 *
	 * @param	mediaFileName		the fully qualified path name to a DICOM file
	 * @param	transferSyntaxUID	the Transfer Syntax of the Data Set if a DICOM file, from the DICOMDIR or Meta Information Header
	 * @param	sopClassUID			the SOP Class of the Data Set if a DICOM file, from the DICOMDIR or Meta Information Header
	 */
	protected void doSomethingWithUnwantedFileOnMedia(String mediaFileName,String transferSyntaxUID,String sopClassUID) {
		logLn("Not a DICOM file, not a DICOM PS 3.10 file or not an image so cannot compress: "+mediaFileName);
	}
	
	/**
	 * <p>Compress the DICOM file.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
		//logLn("MediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
		slf4jlogger.info("doSomethingWithDicomFileOnMedia(): {} outputFormat = {} transferSyntaxUID = {}",mediaFileName,outputFormat,transferSyntaxUID);
		try {
			boolean deferredDecompression = CompressedFrameDecoder.canDecompress(mediaFileName);
			slf4jlogger.info("doSomethingWithDicomFileOnMedia(): deferredDecompression {}",deferredDecompression);
			AttributeList list = new AttributeList();
			list.setDecompressPixelData(!deferredDecompression);		// we don't want to decompress it during read if we can decompress it on the fly during recompression (000784)
			list.read(mediaFileName);
			list.removeGroupLengthAttributes();
			list.remove(TagFromName.DataSetTrailingPadding);
			
			// NB. do NOT remove meta information until AFTER deferred decompression of pixel data for recompression, else SourceImage will fail to find the necessary TransferSyntax during deferred decompression
			
			SourceImage sImg = new SourceImage(list);
			if (sImg == null) {
				throw new DicomException("Could not get images for frames from"+mediaFileName);
			}
			
			int numberOfFrames = sImg.getNumberOfFrames();
			File[] frameFiles = new File[numberOfFrames];
			for (int f=0; f<numberOfFrames; ++f) {
				BufferedImage renderedImage = sImg.getBufferedImage(f);
				if (renderedImage == null) {
					throw new DicomException("Could not get image for frame "+f+" from"+mediaFileName);
				}
				File tmpFrameFile = File.createTempFile("CompressDicomFiles_tmp",".tmp");
				tmpFrameFile.deleteOnExit();
				frameFiles[f] = CompressedFrameEncoder.getCompressedFrameAsFile(list,renderedImage,outputFormat,tmpFrameFile);
			}
			
			OtherByteAttributeMultipleCompressedFrames aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,frameFiles);
			list.put(aPixelData);

			list.correctDecompressedImagePixelModule(deferredDecompression);					// make sure to correct even if decompression was deferred
			list.insertLossyImageCompressionHistoryIfDecompressed(deferredDecompression);
			
			// set compressed pixel data characteristics AFTER correctDecompressedImagePixelModule() ...
			String photometricInterpretation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PhotometricInterpretation);
			
			if (photometricInterpretation.equals("YBR_FULL_422")) {
				// not converted during reading of lossy JPEG, e.g., really was uncompressed YBR_FULL_422
				// will be upsampled (but not color space converted) by SourceImage.getBufferedImage() called during frame compression so say so ... (000997)
				photometricInterpretation = "YBR_FULL";
				Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a);
			}
			
			if (outputFormat.equals("jpeg2000")) {
				if (photometricInterpretation.equals("RGB")) {
					photometricInterpretation = "YBR_RCT";
					Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a);
				}
				else {
					// (000981)
					// would be better to throw this earlier :(
					throw new DicomException("Cannot encode "+photometricInterpretation+" using JPEG 2000, only RGB transformed to YBR_RCT is permitted");
				}
			}
			// else leave it alone, since neither JPEG lossless nor JPEG-LS codec nor RLE does a color space transformation
			
			if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.SamplesPerPixel,0) > 1) {
				// when RLE, always seperate bands; decompressed input may not have been
				// when JPEG family, output of JIIO codecs is always interleaved; decompressed RLE input may (should?) have been 1 though sometimes isn't, and uncompressed input could have been either
				{ Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue(outputFormat.equals("rle") ? 1 : 0); list.put(a); }
			}
			else {
				list.remove(TagFromName.PlanarConfiguration);	// just in case, since it shouldn't be there
			}
			
			list.removeMetaInformationHeaderAttributes();
			FileMetaInformation.addFileMetaInformation(list,transferSyntaxUID,"OURAETITLE");
			
			String outputFileName = reuseSameBaseFileName
				? new File(mediaFileName).getName()
				: Attribute.getSingleStringValueOrDefault(list,TagFromName.SOPInstanceUID,"NONAME");
			
			File outputFile = new File(outputPath,outputFileName);
			if (outputFile.exists()) {
				throw new DicomException("Not overwriting output file that already exists "+outputFile);
			}
			slf4jlogger.info("doSomethingWithDicomFileOnMedia(): writing compressed file {}",outputFile);
			list.write(outputFile,transferSyntaxUID,true,true);
			
			for (int f=0; f<numberOfFrames; ++f) {
				frameFiles[f].delete();
				frameFiles[f] = null;
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			slf4jlogger.error("While processing "+mediaFileName+" ",e);
		}
	}
	
	protected static String chooseOutputFormatForTransferSyntax(String transferSyntaxUID) {
		String outputFormat = null;
		if (transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless)) {
			outputFormat = "jpeg2000";
		}
		else if (transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1)) {
			outputFormat = "jpeg-lossless";
		}
		else if (transferSyntaxUID.equals(TransferSyntax.JPEGLS)) {
			outputFormat = "jpeg-ls";
		}
		else if (transferSyntaxUID.equals(TransferSyntax.RLE)) {
			outputFormat = "rle";
		}
		return outputFormat;
	}
	
	protected static String chooseTransferSyntaxForOutputFormat(String outputFormat) {
		String transferSyntaxUID = null;
		if (outputFormat.equals("jpeg2000")) {
			transferSyntaxUID = TransferSyntax.JPEG2000Lossless;
		}
		else if (outputFormat.equals("jpeg-lossless")) {
			transferSyntaxUID = TransferSyntax.JPEGLosslessSV1;
		}
		else if (outputFormat.equals("jpeg-ls")) {
			transferSyntaxUID = TransferSyntax.JPEGLS;
		}
		else if (outputFormat.equals("rle")) {
			transferSyntaxUID = TransferSyntax.RLE;
		}
		return transferSyntaxUID;
	}
	
	/**
	 * <p>Copy a set of DICOM image files, compressing them losslessly with JPEG 2000 (default), JPEG 10918-1 Lossless Huffman SV1, JPEG-LS or RLE.</p>
	 *
	 * <p>Non-image files are ignored (not copied).</p>
	 *
	 * @param	arg	array of two or three strings - the input path and the output path and optionally the requested compressed transfer syntax [1.2.840.10008.1.2.4.90|1.2.840.10008.1.2.4.70|1.2.840.10008.1.2.4.80|1.2.840.10008.1.2.5] or output format string [jpeg2000|jpeg-lossless|jpeg-ls|rle]
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				CompressDicomFiles importer = new CompressDicomFiles(new PrintStreamMessageLogger(System.err));
				importer.outputPath = arg[1];
				importer.outputFormat = "jpeg2000";
				importer.transferSyntaxUID = TransferSyntax.JPEG2000Lossless;
				importer.reuseSameBaseFileName = false;
				importer.importDicomFiles(arg[0]);
			
			}
			else if (arg.length == 3) {
				CompressDicomFiles importer = new CompressDicomFiles(new PrintStreamMessageLogger(System.err));
				importer.outputPath = arg[1];
				if (arg[2].startsWith("1.")) {
					importer.transferSyntaxUID = arg[2].trim();
					importer.outputFormat = chooseOutputFormatForTransferSyntax(importer.transferSyntaxUID);
				}
				else {
					importer.outputFormat = arg[2].toLowerCase().trim();
					importer.transferSyntaxUID = chooseTransferSyntaxForOutputFormat(importer.outputFormat);
				}
				if (importer.outputFormat == null || importer.transferSyntaxUID == null) {
					throw new Exception("Unsupported output format or Transfer Syntax UID");
				}
				importer.reuseSameBaseFileName = false;
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



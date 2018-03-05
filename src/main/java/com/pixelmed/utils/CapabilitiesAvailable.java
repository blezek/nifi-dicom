/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.io.PrintStream;

import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Various capability detection methods.</p>
 *
 * @author	dclunie
 */
public class CapabilitiesAvailable {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/CapabilitiesAvailable.java,v 1.12 2017/01/24 10:50:51 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CapabilitiesAvailable.class);
	
	/**
	 * <p>Is the selective JPEG block redaction codec available for the baseline 8 bit process?</p>
	 *
	 * @return	 true if available
	 */
	public static boolean haveJPEGBaselineSelectiveBlockRedaction() {
		boolean result=true;
		try {
			Class classToUse = Class.forName("com.pixelmed.codec.jpeg.Parse");
			slf4jlogger.debug("CapabilitiesAvailable.haveJPEGBaselineSelectiveBlockRedaction(): Found redaction codec");
		}
		catch (ClassNotFoundException e) {
			slf4jlogger.debug("CapabilitiesAvailable.haveJPEGBaselineSelectiveBlockRedaction(): No redaction codec");
			result=false;
		}
		return result;
	}

	/**
	 * <p>Is a bzip2 codec available?</p>
	 *
	 * @return	true if available
	 */
	public static boolean haveBzip2Support() {
		boolean result=true;
		try {
			Class classToUse = Class.forName("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream");
		}
		catch (ClassNotFoundException e) {
			result=false;
		}
		return result;
	}

	/**
	 * <p>Is a unix compress codec available?</p>
	 *
	 * @return	true if available
	 */
	public static boolean haveUnixCompressSupport() {
		boolean result=true;
		try {
			Class classToUse = Class.forName("org.apache.commons.compress.compressors.z.ZCompressorInputStream");
		}
		catch (ClassNotFoundException e) {
			result=false;
		}
		return result;
	}

	private static boolean haveScannedForCodecs = false;

	private static boolean haveCheckedForJPEGLosslessCodec = false;
	private static boolean haveFoundJPEGLosslessCodec = false;
	
	/**
	 * <p>Is a lossless JPEG codec available?</p>
	 *
	 * @return	true if available
	 */
	public static boolean haveJPEGLosslessCodec() {
		if (!haveCheckedForJPEGLosslessCodec) {
			if (!haveScannedForCodecs) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLosslessCodec(): Scanning for ImageIO plugin codecs");
				ImageIO.scanForPlugins();
				haveScannedForCodecs=true;
			}
			haveFoundJPEGLosslessCodec = false;
			String readerWanted="jpeg-lossless";
			try {
				ImageReader reader =  (ImageReader)(ImageIO.getImageReadersByFormatName(readerWanted).next());
				if (reader != null) {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLosslessCodec(): Found jpeg-lossless reader");
					haveFoundJPEGLosslessCodec = true;
					try {
						slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLosslessCodec(): Calling dispose() on reader");
						reader.dispose();
					}
					catch (Exception e) {
						slf4jlogger.error("", e);
					}
				}
				else {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLosslessCodec(): No jpeg-lossless reader");
				}
			}
			catch (Exception e) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLosslessCodec(): No jpeg-lossless reader");
				haveFoundJPEGLosslessCodec = false;
			}
			haveCheckedForJPEGLosslessCodec = true;
		}
		return haveFoundJPEGLosslessCodec;
	}
	
	private static boolean haveCheckedForJPEG2000Part1Codec = false;
	private static boolean haveFoundJPEG2000Part1Codec = false;
	
	/**
	 * <p>Is a JPEG 2000 Part 1 codec available?</p>
	 *
	 * @return	true if available
	 */
	public static boolean haveJPEG2000Part1Codec() {
		if (!haveCheckedForJPEG2000Part1Codec) {
			if (!haveScannedForCodecs) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEG2000Part1Codec(): Scanning for ImageIO plugin codecs");
				ImageIO.scanForPlugins();
				haveScannedForCodecs=true;
			}
			haveFoundJPEG2000Part1Codec = false;
			String readerWanted="JPEG2000";
			try {
				ImageReader reader =  (ImageReader)(ImageIO.getImageReadersByFormatName(readerWanted).next());
				if (reader != null) {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEG2000Part1Codec(): Found JPEG2000 reader");
					haveFoundJPEG2000Part1Codec = true;
					try {
						slf4jlogger.debug("CapabilitiesAvailable.haveJPEG2000Part1Codec(): Calling dispose() on reader");
						reader.dispose();
					}
					catch (Exception e) {
						slf4jlogger.error("", e);
					}
				}
				else {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEG2000Part1Codec(): No JPEG2000 reader");
				}
			}
			catch (Exception e) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEG2000Part1Codec(): No JPEG2000 reader");
				haveFoundJPEG2000Part1Codec = false;
			}
			haveCheckedForJPEG2000Part1Codec = true;
		}
		return haveFoundJPEG2000Part1Codec;
	}
	
	private static boolean haveCheckedForJPEGLSCodec = false;
	private static boolean haveFoundJPEGLSCodec = false;
	
	/**
	 * <p>Is a JPEG-LS codec available?</p>
	 *
	 * @return	true if available
	 */
	public static boolean haveJPEGLSCodec() {
		if (!haveCheckedForJPEGLSCodec) {
			if (!haveScannedForCodecs) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLSCodec(): Scanning for ImageIO plugin codecs");
				ImageIO.scanForPlugins();
				haveScannedForCodecs=true;
			}
			haveFoundJPEGLSCodec = false;
			String readerWanted="jpeg-ls";
			try {
				ImageReader reader =  (ImageReader)(ImageIO.getImageReadersByFormatName(readerWanted).next());
				if (reader != null) {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLSCodec(): Found JPEG-LS reader");
					haveFoundJPEGLSCodec = true;
					try {
						slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLSCodec(): Calling dispose() on reader");
						reader.dispose();
					}
					catch (Exception e) {
						slf4jlogger.error("", e);
					}
				}
				else {
					slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLSCodec(): No JPEG-LS reader");
				}
			}
			catch (Exception e) {
				slf4jlogger.debug("CapabilitiesAvailable.haveJPEGLSCodec(): No JPEG-LS reader");
				haveFoundJPEGLSCodec = false;
			}
			haveCheckedForJPEGLSCodec = true;
		}
		return haveFoundJPEGLSCodec;
	}

	public static void dumpListOfAllAvailableReaders(PrintStream out) {
		String[] formats=ImageIO.getReaderFormatNames();
		for (int i=0; formats != null && i<formats.length; ++i) {
			out.println(formats[i]+":");
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formats[i]);
			while (readers.hasNext()) {
				ImageReader reader = readers.next();
				ImageReaderSpi spi = reader.getOriginatingProvider();
				out.println("\t"+spi.getDescription(Locale.US)+" "+spi.getVendorName()+" "+spi.getVersion());
			}
		}
	}
	
	public static void main (String arg[]) {
		dumpListOfAllAvailableReaders(System.err);	// no need to use SLF4J since command line utility/test
		System.err.println("CapabilitiesAvailable.haveBzip2Support(): "+(haveBzip2Support() ? "yes" : "no"));
		System.err.println("CapabilitiesAvailable.haveJPEGLosslessCodec(): "+(haveJPEGLosslessCodec() ? "yes" : "no"));
		System.err.println("CapabilitiesAvailable.haveJPEG2000Part1Codec(): "+(haveJPEG2000Part1Codec() ? "yes" : "no"));
		System.err.println("CapabilitiesAvailable.haveJPEGLSCodec(): "+(haveJPEGLSCodec() ? "yes" : "no"));
		System.err.println("CapabilitiesAvailable.haveJPEGBaselineSelectiveBlockRedaction(): "+(haveJPEGBaselineSelectiveBlockRedaction() ? "yes" : "no"));
	}
	
}

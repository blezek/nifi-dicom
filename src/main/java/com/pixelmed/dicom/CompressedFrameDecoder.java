/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.awt.Point;
import java.awt.Transparency;

import java.awt.color.ColorSpace;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.Locale;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.spi.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.metadata.IIOMetadata;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.dicom.CompressedFrameDecoder CompressedFrameDecoder} class implements decompression of selected frames
 * in various supported Transfer Syntaxes once already extracted from DICOM encapsulated images.</p>
 *
 *
 * @author	dclunie
 */
public class CompressedFrameDecoder {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CompressedFrameDecoder.java,v 1.17 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompressedFrameDecoder.class);
	
	private String transferSyntaxUID;
	private byte[][] frames;
	private int bytesPerSample;
	private int width;		// needed for RLE
	private int height;		// needed for RLE
	private int samples;	// needed for RLE
	private ColorSpace colorSpace;
	
	private static boolean haveScannedForCodecs;

	public static void scanForCodecs() {
		slf4jlogger.trace("scanForCodecs(): Scanning for ImageIO plugin codecs");
		ImageIO.scanForPlugins();
		ImageIO.setUseCache(false);		// disk caches are too slow :(
		haveScannedForCodecs=true;
	}
	
	//private boolean pixelDataWasLossy = false;			// set if decompressed from lossy transfer syntax during reading of Pixel Data attribute in this AttributeList instance
	//private String lossyMethod = null;
	private IIOMetadata[] iioMetadata = null;			// will be set during compression if reader is capable of it
	private boolean colorSpaceWillBeConvertedToRGBDuringDecompression = false;	// set if color space will be converted to RGB during compression
	
	private String readerWanted;

	private boolean isJPEGFamily = false;
	private boolean isRLE = false;
	
	private ImageReader reader = null;
	
	private int lastFrameDecompressed = -1;
	private IIOMetadata iioMetadataForLastFrameDecompressed = null;
	
	/**
	 * <p>Returns a whether or not a DICOM file contains pixel data that can be decompressed using this class.</p>
	 *
	 * @param	file	the file
	 * @return	true if file can be decompressed using this class
	 */
	public static boolean canDecompress(File file) {
		slf4jlogger.trace("canDecompress(): file "+file);
		boolean canDecompressPixelData = false;
		AttributeList list = new AttributeList();
		try {
			list.readOnlyMetaInformationHeader(file);
			String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
			slf4jlogger.debug("canDecompress(): transferSyntaxUID {}",transferSyntaxUID);
			if (transferSyntaxUID.equals(TransferSyntax.RLE)) {
				canDecompressPixelData = true;				// (000787)
			}
			else {
				TransferSyntax ts = new TransferSyntax(transferSyntaxUID);
				slf4jlogger.trace("canDecompress(): {}",ts.dump());
				int maximumBytesPerSampleNeededForTransferSyntax = 2;
				if (ts.isJPEGFamily()) {
					slf4jlogger.trace("canDecompress(): transferSyntaxUID is JPEG family");
					String readerWanted = null;
					if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)) {
						readerWanted="JPEG";
						maximumBytesPerSampleNeededForTransferSyntax = 1;
					}
					if (transferSyntaxUID.equals(TransferSyntax.JPEGExtended)) {
						readerWanted="JPEG";
					}
					else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000) || transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless)) {
						readerWanted="JPEG2000";
					}
					else if (transferSyntaxUID.equals(TransferSyntax.JPEGLossless) || transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1)) {
						readerWanted="jpeg-lossless";
					}
					else if (transferSyntaxUID.equals(TransferSyntax.JPEGLS) || transferSyntaxUID.equals(TransferSyntax.JPEGNLS)) {
						readerWanted="jpeg-ls";
					}
					if (readerWanted != null) {
						slf4jlogger.trace("canDecompress(): readerWanted {}",readerWanted);
						try {
							ImageReader reader = selectReaderFromCodecsAvailable(readerWanted,transferSyntaxUID,maximumBytesPerSampleNeededForTransferSyntax);
							if (reader != null) {
								canDecompressPixelData = true;
							}
						}
						catch (Exception e) {
							slf4jlogger.debug("ignore any exception at this point, since harmless", e);
						}
					}
				}
			}
		}
		catch (DicomException e) {
			slf4jlogger.error("", e);
		}
		catch (IOException e) {
			slf4jlogger.error("", e);
		}
		slf4jlogger.trace("canDecompress(): returns {}",canDecompressPixelData);
		return canDecompressPixelData;
	}
	
	/**
	 * <p>Returns a whether or not a DICOM file contains pixel data that can be decompressed using this class.</p>
	 *
	 * @param	filename	the file
	 * @return	true		if file can be decompressed using this class
	 */
	public static boolean canDecompress(String filename) {
		return canDecompress(new File(filename));
	}
	
	/**
	 * <p>Returns a reference to the {@link javax.imageio.metadata.IIOMetadata IIOMetadata} object for the selected frame, or null if none was available during reading. </p>
	 *
	 * @param	frame	the frame number, from 0
	 * @return	an {@link javax.imageio.metadata.IIOMetadata IIOMetadata} object, or null.
	 */
	public IIOMetadata getIIOMetadata(int frame) {
		return frame == lastFrameDecompressed ? iioMetadataForLastFrameDecompressed : null;
	}

	/**
	 * <p>Returns a whether or not the color space will be converted to RGB during compression if it was YBR in the first place.</p>
	 *
	 * @return	true if RGB after compression
	 */
	public boolean getColorSpaceConvertedToRGBDuringDecompression() {
		return colorSpaceWillBeConvertedToRGBDuringDecompression;
	}
	
	// compare this to AttributeList.extractCompressedPixelDataCharacteristics(), which handles RLE too, whereas here we handle JPEG as well
	private void chooseReaderWantedBasedOnTransferSyntax() {
		TransferSyntax ts = new TransferSyntax(transferSyntaxUID);
		isJPEGFamily = ts.isJPEGFamily();
		isRLE = transferSyntaxUID.equals(TransferSyntax.RLE);

		colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// do not set this generally ... be specific to each scheme (00704)
		//pixelDataWasLossy=false;
		//lossyMethod=null;
		readerWanted = null;
		slf4jlogger.trace("chooseReader(): TransferSyntax = {}",transferSyntaxUID);
		if (isRLE) {
			// leave colorSpaceWillBeConvertedToRGBDuringDecompression false;	// (000832)
			slf4jlogger.trace("Undefined length encapsulated Pixel Data in RLE");
		}
		else if (isJPEGFamily) {
			if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline) || transferSyntaxUID.equals(TransferSyntax.JPEGExtended)) {
				readerWanted="JPEG";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				//pixelDataWasLossy=true;
				//lossyMethod="ISO_10918_1";
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG Lossy");
			}
			else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000)) {
				readerWanted="JPEG2000";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				//pixelDataWasLossy=true;
				//lossyMethod="ISO_15444_1";
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG 2000");
			}
			else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless)) {
				readerWanted="JPEG2000";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG 2000");
			}
			else if (transferSyntaxUID.equals(TransferSyntax.JPEGLossless) || transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1)) {
				readerWanted="jpeg-lossless";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// NB. (00704)
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG Lossless");
			}
			else if (transferSyntaxUID.equals(TransferSyntax.JPEGLS)) {
				readerWanted="jpeg-ls";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// Hmmm :(
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG-LS");
			}
			else if (transferSyntaxUID.equals(TransferSyntax.JPEGNLS)) {
				readerWanted="jpeg-ls";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// Hmmm :(
				//pixelDataWasLossy=true;
				//lossyMethod="ISO_14495_1";
				slf4jlogger.trace("chooseReader(): Undefined length encapsulated Pixel Data in JPEG-LS");
			}
			else {
				readerWanted="JPEG";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				slf4jlogger.warn("Unrecognized JPEG family Transfer Syntax {} for encapsulated PixelData - guessing {}",transferSyntaxUID,readerWanted);
			}
		}
		else {
			slf4jlogger.error("Unrecognized Transfer Syntax {} for encapsulated PixelData - cannot find reader",transferSyntaxUID);
		}
		slf4jlogger.trace("chooseReader(): Based on Transfer Syntax, colorSpaceWillBeConvertedToRGBDuringDecompression = {}",colorSpaceWillBeConvertedToRGBDuringDecompression);
	}
	
	public static boolean isStandardJPEGReader(ImageReader reader) {
		return reader.getOriginatingProvider().getDescription(Locale.US).equals("Standard JPEG Image Reader") && (reader.getOriginatingProvider().getVendorName().equals("Sun Microsystems, Inc.") || reader.getOriginatingProvider().getVendorName().equals("Oracle Corporation"));
	}
	
	public static boolean isPixelMedLosslessJPEGReader(ImageReader reader) {
		return reader.getOriginatingProvider().getDescription(Locale.US).equals("PixelMed JPEG Lossless Image Reader");		// cannot reference com.pixelmed.imageio.JPEGLosslessImageReaderSpi.getDescription() because may not be available at compile time
	}
	
	public static ImageReader selectReaderFromCodecsAvailable(String readerWanted,String transferSyntaxUID,int bytesPerSample) throws DicomException {
		ImageReader reader = null;
		// Do NOT assume that first reader found is the best one ... check them all and make explicit choices ...
		// Cannot assume that they are returned in any particular order ...
		// Cannot assume that there are only two that match ...
		// Cannot assume that all of them are available on any platform or configuration ...
		//try {
		Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(readerWanted);
		while (it.hasNext()) {
			if (reader == null) {
				reader = it.next();
				slf4jlogger.info("selectReaderFromCodecsAvailable(): First reader found is {} {} {}",reader.getOriginatingProvider().getDescription(Locale.US),reader.getOriginatingProvider().getVendorName(),reader.getOriginatingProvider().getVersion());
			}
			else {
				ImageReader otherReader = it.next();
				slf4jlogger.info("selectReaderFromCodecsAvailable(): Found another reader {} {} {}",otherReader.getOriginatingProvider().getDescription(Locale.US),otherReader.getOriginatingProvider().getVendorName(),otherReader.getOriginatingProvider().getVersion());
				
				if (isStandardJPEGReader(reader)) {
					// prefer any other reader to the standard one, since the standard one is limited, and any other is most likely JAI JIIO
					reader = otherReader;
					slf4jlogger.info("selectReaderFromCodecsAvailable(): Choosing reader {} {} {} over Standard JPEG Image Reader",reader.getOriginatingProvider().getDescription(Locale.US),reader.getOriginatingProvider().getVendorName(),reader.getOriginatingProvider().getVersion());
				}
				else if (isPixelMedLosslessJPEGReader(reader)) {
					slf4jlogger.info("selectReaderFromCodecsAvailable(): Choosing reader {} {} {} over any other reader",reader.getOriginatingProvider().getDescription(Locale.US),reader.getOriginatingProvider().getVendorName(),reader.getOriginatingProvider().getVersion());
					break;
				}
				else if (isPixelMedLosslessJPEGReader(otherReader)) {
					reader = otherReader;
					slf4jlogger.info("selectReaderFromCodecsAvailable(): Choosing reader {} {} {} over any other reader",reader.getOriginatingProvider().getDescription(Locale.US),reader.getOriginatingProvider().getVendorName(),reader.getOriginatingProvider().getVersion());
					break;
				}
			}
		}
		if (reader != null) {
			// The JAI JIIO JPEG reader is OK since it handles both 8 and 12 bit JPEGExtended, but the "standard" reader that comes with the JRE only supports 8 bit
			// Arguably 8 bits in JPEGExtended is not valid (PS3.5 10.2) but since it is sometimes encountered, deal with it if we can, else throw specific exception ...
			if (transferSyntaxUID.equals(TransferSyntax.JPEGExtended) && bytesPerSample > 1 && reader.getOriginatingProvider().getDescription(Locale.US).equals("Standard JPEG Image Reader") && (reader.getOriginatingProvider().getVendorName().equals("Sun Microsystems, Inc.") || reader.getOriginatingProvider().getVendorName().equals("Oracle Corporation"))) {
				throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()+" does not support extended lossy JPEG Transfer Syntax "+transferSyntaxUID+" other than for 8 bit data");
			}
			slf4jlogger.info("selectReaderFromCodecsAvailable(): Using reader {} {} {}",reader.getOriginatingProvider().getDescription(Locale.US),reader.getOriginatingProvider().getVendorName(),reader.getOriginatingProvider().getVersion());
		}
		else {
			//CapabilitiesAvailable.dumpListOfAllAvailableReaders(System.err);
			throw new DicomException("No reader for "+readerWanted+" available for Transfer Syntax "+transferSyntaxUID);
		}
		//}
		//catch (Exception e) {
		//	slf4jlogger.error("", e);
		//	CapabilitiesAvailable.dumpListOfAllAvailableReaders(System.err);
		//	throw new DicomException("No reader for "+readerWanted+" available for Transfer Syntax "+transferSyntaxUID+"\nCaused by: "+e);
		//}
		return reader;
	}
	
	public CompressedFrameDecoder(String transferSyntaxUID,byte[][] frames,int bytesPerSample,int width,int height,int samples,ColorSpace colorSpace) throws DicomException {
		if (frames == null)  {
			throw new DicomException("no array of compressed data per frame supplied to decompress");
		}
		this.transferSyntaxUID = transferSyntaxUID;
		slf4jlogger.trace("CompressedFrameDecoder(): transferSyntaxUID = {}",transferSyntaxUID);
		this.frames = frames;
		this.bytesPerSample = bytesPerSample;
		this.width = width;
		this.height = height;
		this.samples = samples;
		this.colorSpace = colorSpace;

		scanForCodecs();
		
		chooseReaderWantedBasedOnTransferSyntax();
		slf4jlogger.trace("CompressedFrameDecoder(): Based on Transfer Syntax, colorSpaceWillBeConvertedToRGBDuringDecompression = {}",colorSpaceWillBeConvertedToRGBDuringDecompression);
		if (readerWanted != null) {
			reader = selectReaderFromCodecsAvailable(readerWanted,transferSyntaxUID,bytesPerSample);
		}
		else if (!isRLE) {
			slf4jlogger.trace("CompressedFrameDecoder(): Unrecognized Transfer Syntax {} for encapsulated PixelData",transferSyntaxUID);
			throw new DicomException("Unrecognized Transfer Syntax "+transferSyntaxUID+" for encapsulated PixelData");
		}
	}

	public BufferedImage getDecompressedFrameAsBufferedImage(int f) throws DicomException, IOException {
		slf4jlogger.trace("getDecompressedFrameAsBufferedImage(): Starting frame "+f);
		BufferedImage image = null;
		if (isRLE) {
			image = getDecompressedFrameAsBufferedImageUsingRLE(f);
		}
		else {
			image = getDecompressedFrameAsBufferedImageUsingImageReader(f);
		}
		return image;
	}
	
	protected class ByteArrayInputStreamWithOffsetCounterAndOurMethods extends InputStream {
		protected byte[] buf;
		protected int pos;
		protected int count;
		
		public ByteArrayInputStreamWithOffsetCounterAndOurMethods(byte[] buf) {
			super();
			this.buf = buf;
			pos = 0;
			count = buf.length;
		}
		
		public int read() {
			int i = -1;
			if (pos < count) {
				i = ((int)buf[pos++]) & 0xff;
			}
			return i;
		}
		
		public int read(byte[] b,int off,int len) {
			int remaining = count - pos;
			if (remaining < 0) {
				len = -1;
			}
			else {
				if (len > remaining) {
					len = remaining;
				}
				System.arraycopy(buf,pos,b,off,len);
				pos+=len;
			}
			return len;
		}
		
		public long skip(long n) {
			long remaining = (long)count - pos;
			if (remaining < 0) {
				n = 0;
			}
			else if (n > remaining) {
				n = remaining;
			}
			pos = pos + (int)n;
			return n;
		}
		
		public int available() {
			return count - pos;
		}
		
		public boolean markSupported() {
			return false;
		}
		
		// these are copied from EncapsulatedInputStream ...
		
		public final long readUnsigned32LittleEndian() {
			byte  b[] = new byte[4];
			read(b,0,4);
			long v1 =  ((long)b[0])&0xff;
			long v2 =  ((long)b[1])&0xff;
			long v3 =  ((long)b[2])&0xff;
			long v4 =  ((long)b[3])&0xff;
			return (((((v4 << 8) | v3) << 8) | v2) << 8) | v1;
		}

		public final void readUnsigned32LittleEndian(long[] w,int offset,int len) throws IOException {
			int blen = len*4;
			byte  b[] = new byte[blen];
			read(b,0,blen);
			int bcount=0;
			int wcount=0;
			{
				for (;wcount<len;++wcount) {
					long v1 =  ((long)b[bcount++])&0xff;
					long v2 =  ((long)b[bcount++])&0xff;
					long v3 =  ((long)b[bcount++])&0xff;
					long v4 =  ((long)b[bcount++])&0xff;
					w[offset+wcount]=(((((v4 << 8) | v3) << 8) | v2) << 8) | v1;
				}
			}
		}

		public void skipInsistently(long length) throws IOException {
			long remaining = length;
			while (remaining > 0) {
//System.err.println("skipInsistently(): looping remaining="+remaining);
				long bytesSkipped = skip(remaining);
//System.err.println("skipInsistently(): asked for ="+remaining+" got="+bytesSkipped);
				if (bytesSkipped <= 0) throw new IOException("skip failed with "+remaining+" bytes remaining to be skipped, wanted "+length);
				remaining-=bytesSkipped;
			}
		}
		
		public int getOffsetOfNextByteToReadFromStartOfFragment() {
			return pos;
		}

	}
	
	// (000787)
	public BufferedImage getDecompressedFrameAsBufferedImageUsingRLE(int f) throws DicomException, IOException {
	slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Starting frame {}",f);
		BufferedImage image = null;
		
		ByteArrayInputStreamWithOffsetCounterAndOurMethods bi = new ByteArrayInputStreamWithOffsetCounterAndOurMethods(frames[f]);

		// copied from AttributeList.read() ... should refactor and share code except that input is ByteArrayInputStreamWithOffsetCounterAndOurMethods not EncapsulatedInputStream, and output is BufferedImage not Attribute :(
		int pixelsPerFrame = height*width;
		if (bytesPerSample == 1) {
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): bytesPerSample = 1");
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): pixelsPerFrame = {}",pixelsPerFrame);
			byte[] bytePixelData = new byte[pixelsPerFrame*samples];
			{
				// The RLE "header" consists of 16 long values
				// the 1st value is the number of segments
				// the remainder are the byte offsets of each of up to 15 segments
				int numberofSegments = (int)(bi.readUnsigned32LittleEndian());
				slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Number of segments = {}",numberofSegments);
				long[] segmentOffsets = new long[15];
				bi.readUnsigned32LittleEndian(segmentOffsets,0,15);
				for (int soi=0; soi<15; ++soi) {
					slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Segment [{}] offset = {}",soi,segmentOffsets[soi]);
					if (segmentOffsets[soi]%2 != 0) {
						System.err.println("Error: fragment offset for segment "+soi+" is not even length ("+segmentOffsets[soi]+") but ignoring and using odd offset anyway");
					}
				}
				// does not matter whether DICOM AttributeList contained PixelRepresentation that was color-by-plane or -pixel, since RLE is always by plane and we just make that kind of BufferedImage */
				{
					for (int s=0; s < samples; ++s) {
						slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Doing sample = {}",s);
						int currentOffset = bi.getOffsetOfNextByteToReadFromStartOfFragment();
						slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): At fragment offset {}",currentOffset);
						int bytesToSkipToStartOfSegment = (int)(segmentOffsets[s]) - currentOffset;
						if (bytesToSkipToStartOfSegment > 0) {
							bi.skipInsistently(bytesToSkipToStartOfSegment);
							slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Skipped {} to segment offset {}",bytesToSkipToStartOfSegment,segmentOffsets[s]);
						}
						else if (bytesToSkipToStartOfSegment < 0) {
							throw new DicomException("Already read past start of next segment "+s+" - at "+currentOffset+" need to be at "+segmentOffsets[s]);
						}
						slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Now at fragment offset {}",bi.getOffsetOfNextByteToReadFromStartOfFragment());
						// else right on already
						int got = UnPackBits.decode(bi,bytePixelData,pixelsPerFrame*s/*offset*/,pixelsPerFrame);	// entire planes of samples
						slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): got = {} pixels",got);
					}
				}
				if (samples == 1) {
					ComponentColorModel cm=new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
																   new int[] {8},
																   false,		// has alpha
																   false,		// alpha premultipled
																   Transparency.OPAQUE,
																   DataBuffer.TYPE_BYTE
																   );
					
					ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,
																	   width,
																	   height,
																	   1,
																	   width,
																	   new int[] {0}
																	   );
					
					DataBuffer buf = new DataBufferByte(bytePixelData,width,0);
					WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
					image = new BufferedImage(cm,wr,true,null);	// no properties hash table
				}
				else if (samples == 3) {
					ComponentColorModel cm=new ComponentColorModel(colorSpace,
																   new int[] {8,8,8},
																   false,		// has alpha
																   false,		// alpha premultipled
																   Transparency.OPAQUE,
																   DataBuffer.TYPE_BYTE
																   );
					
					ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,
																	   width,
																	   height,
																	   1,
																	   width,
																	   new int[] {0,pixelsPerFrame,pixelsPerFrame*2}
																	   );
					
					DataBuffer buf = new DataBufferByte(bytePixelData,width,0);
					WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
					image = new BufferedImage(cm,wr,true,null);	// no properties hash table
				}
				else {
					throw new DicomException("Creation of BufferedImage for RLE compressed frame of more samples other than 1 or 3 not supported yet (got "+samples+")");
				}
			}
		}
		else if (bytesPerSample == 2) {
			// for each frame, have to read all high bytes first for a sample, then low bytes :(
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): bytesPerSample = 2");
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): pixelsPerFrame = {}",pixelsPerFrame);
			short[] shortPixelData = new short[pixelsPerFrame*samples];
			{
				// The RLE "header" consists of 16 long values
				// the 1st value is the number of segments
				// the remainder are the byte offsets of each of up to 15 segments
				int numberofSegments = (int)(bi.readUnsigned32LittleEndian());
				slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Number of segments = {}",numberofSegments);
				long[] segmentOffsets = new long[15];
				bi.readUnsigned32LittleEndian(segmentOffsets,0,15);
				for (int soi=0; soi<15; ++soi) {
					slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingRLE(): Segment [{}] offset = {}",soi,segmentOffsets[soi]);
					if (segmentOffsets[soi]%2 != 0) {
						System.err.println("Error: fragment offset for segment "+soi+" is not even length ("+segmentOffsets[soi]+") but ignoring and using odd offset anyway");
					}
				}
				// does not matter whether DICOM AttributeList contained PixelRepresentation that was color-by-plane or -pixel, since RLE is always by plane and we just make that kind of BufferedImage */
				{
					int sampleOffset = 0;
					int segment = 0;
					for (int s=0; s < samples; ++s) {
						slf4jlogger.trace("Doing sample = {}",s);
						slf4jlogger.trace("Doing firstsegment");
						byte[] firstsegment = new byte[pixelsPerFrame];
						{
							int currentOffset = bi.getOffsetOfNextByteToReadFromStartOfFragment();
							slf4jlogger.trace("At fragment offset {}",currentOffset);
							int bytesToSkipToStartOfSegment = (int)(segmentOffsets[segment]) - currentOffset;
							if (bytesToSkipToStartOfSegment > 0) {
								bi.skipInsistently(bytesToSkipToStartOfSegment);
								slf4jlogger.trace("Skipped {} to segment offset {}",bytesToSkipToStartOfSegment,segmentOffsets[s]);
							}
							else if (bytesToSkipToStartOfSegment < 0) {
								throw new DicomException("Already read past start of next segment "+s+" - at "+currentOffset+" need to be at "+segmentOffsets[s]);
							}
							slf4jlogger.trace("Now at fragment offset {}",bi.getOffsetOfNextByteToReadFromStartOfFragment());
							// else right on already
						}
						int got = UnPackBits.decode(bi,firstsegment,pixelsPerFrame*s/*offset*/,pixelsPerFrame);	// entire planes of samples
						slf4jlogger.trace("got = {} bytes for first segment",got);
						slf4jlogger.trace("Doing secondsegment");
						++segment;
						byte[] secondsegment = new byte[pixelsPerFrame];
						{
							int currentOffset = bi.getOffsetOfNextByteToReadFromStartOfFragment();
							slf4jlogger.trace("At fragment offset {}",currentOffset);
							int bytesToSkipToStartOfSegment = (int)(segmentOffsets[segment]) - currentOffset;
							if (bytesToSkipToStartOfSegment > 0) {
								bi.skipInsistently(bytesToSkipToStartOfSegment);
								slf4jlogger.trace("Skipped {} to segment offset {}",bytesToSkipToStartOfSegment,segmentOffsets[s]);
							}
							else if (bytesToSkipToStartOfSegment < 0) {
								throw new DicomException("Already read past start of next segment "+s+" - at "+currentOffset+" need to be at "+segmentOffsets[s]);
							}
							slf4jlogger.trace("Now at fragment offset {}",bi.getOffsetOfNextByteToReadFromStartOfFragment());
							// else right on already
						}
						got = UnPackBits.decode(bi,secondsegment,pixelsPerFrame*s/*offset*/,pixelsPerFrame);	// entire planes of samples
						slf4jlogger.trace("got = {} bytes for second segment",got);
						for (int p=0; p<pixelsPerFrame; ++p) {
							shortPixelData[sampleOffset + p] = (short)( ((firstsegment[p]&0xff) << 8) + (secondsegment[p]&0xff));
						}
						sampleOffset+=pixelsPerFrame;
						++segment;
					}
				}
				if (samples == 1) {
					ComponentColorModel cm=new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
																   new int[] {16},
																   false,		// has alpha
																   false,		// alpha premultipled
																   Transparency.OPAQUE,
																   DataBuffer.TYPE_USHORT
																   );
					
					ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_USHORT,
																	   width,
																	   height,
																	   1,
																	   width,
																	   new int[] {0}
																	   );
					
					DataBuffer buf = new DataBufferUShort(shortPixelData,width,0);
					WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
					image = new BufferedImage(cm,wr,true,null);	// no properties hash table
				}
				else if (samples == 3) {
					ComponentColorModel cm=new ComponentColorModel(colorSpace,
																   new int[] {16,16,16},
																   false,		// has alpha
																   false,		// alpha premultipled
																   Transparency.OPAQUE,
																   DataBuffer.TYPE_USHORT
																   );
					
					ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_USHORT,
																	   width,
																	   height,
																	   1,
																	   width,
																	   new int[] {0,pixelsPerFrame,pixelsPerFrame*2}
																	   );
					
					DataBuffer buf = new DataBufferUShort(shortPixelData,width,0);
					WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
					image = new BufferedImage(cm,wr,true,null);	// no properties hash table
				}
				else {
					throw new DicomException("Creation of BufferedImage for RLE compressed frame of more samples other than 1 or 3 not supported yet (got "+samples+")");
				}
			}
		}
		else {
			throw new DicomException("RLE of more than 2 bytes per sample not supported (got "+bytesPerSample+")");
		}
		return image;
	}
	
	public BufferedImage getDecompressedFrameAsBufferedImageUsingImageReader(int f) throws DicomException, IOException {
		slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): Starting frame {}",f);
		BufferedImage image = null;
		ImageInputStream iiois = ImageIO.createImageInputStream(new ByteArrayInputStream(frames[f]));
		reader.setInput(iiois,true/*seekForwardOnly*/,true/*ignoreMetadata*/);
										
		slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): Calling reader.readAll()");
		//IIOImage iioImage = null;		// (000911) don't use this until Oracle fixes bug in readAll()
		try {
			//iioImage = reader.readAll(0,null/*ImageReadParam*/);
			image = reader.read(0);
		}
		catch (IIOException e) {
			slf4jlogger.error("", e);
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): \"{}\"",e.toString());
			//if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline) && reader.getOriginatingProvider().getDescription(Locale.US).equals("Standard JPEG Image Reader") && (reader.getOriginatingProvider().getVendorName().equals("Sun Microsystems, Inc.") || reader.getOriginatingProvider().getVendorName().equals("Oracle Corporation"))
			// && e.toString().equals("javax.imageio.IIOException: Inconsistent metadata read from stream")) {
			//	throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()+" does not support JPEG images with components numbered from 0");
			//}
		}
		slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): Back from frame reader.readAll()");
		//if (iioImage == null) {
		if (image == null) {
			throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()
				+" returned null image for Transfer Syntax "+transferSyntaxUID);
		}
		else {
			lastFrameDecompressed = f;
			//iioMetadataForLastFrameDecompressed = iioImage.getMetadata();
			//image = (BufferedImage)(iioImage.getRenderedImage());
			slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): Back from frame {} reader.read(), BufferedImage={}",f,image);
			//if (image == null) {
			//	throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()
			//		+" returned null image for Transfer Syntax "+transferSyntaxUID);
			//}
			//else {
				image = makeNewBufferedImageIfNecessary(image,colorSpace);		// not really sure why we have to do this, but works around different YBR result from standard versus native JPEG codec (000785) :(
			//}
		}
		slf4jlogger.trace("getDecompressedFrameAsBufferedImageUsingImageReader(): returning image = {}",image);
		return image;
	}
	
	private BufferedImage makeNewBufferedImageIfNecessary(BufferedImage image,ColorSpace colorSpace) {
//System.err.print("CompressedFrameDecoder.makeNewBufferedImage(): starting with BufferedImage: ");
//com.pixelmed.display.BufferedImageUtilities.describeImage(image,System.err);
		BufferedImage newImage = null;
		Raster raster = image.getData();
		if (raster.getTransferType() == DataBuffer.TYPE_BYTE && raster.getNumBands() > 1) {		// we only need to do this for color not grayscale, and the models we are about to create contain 3 bands
			int w = raster.getWidth();
			int h = raster.getHeight();
			byte[] data = (byte[])(raster.getDataElements(0,0,w,h,null));	// do NOT use form without width and height
			if (data != null) {
				ComponentColorModel cm=new ComponentColorModel(colorSpace,
															   new int[] {8,8,8},
															   false,		// has alpha
															   false,		// alpha premultipled
															   Transparency.OPAQUE,
															   DataBuffer.TYPE_BYTE
															   );
				
				// pixel interleaved
				ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,
																   w,
																   h,
																   3,
																   w*3,
																   new int[] {0,1,2}
																   );

				// band interleaved
				//ComponentSampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_BYTE,
				//												   w,
				//												   h,
				//												   1,
				//												   w,
				//												   new int[] {0,w*h,w*h*2}
				//											   );
																
				DataBuffer buf = new DataBufferByte(data,w,0/*offset*/);
				
				WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
				
				newImage = new BufferedImage(cm,wr,true,null);	// no properties hash table
//System.err.print("CompressedFrameDecoder.makeNewBufferedImage(): returns new BufferedImage: ");
//com.pixelmed.display.BufferedImageUtilities.describeImage(newImage,System.err);
			}
		}
		return newImage == null ? image : newImage;
	}

		public void dispose() throws Throwable {
		slf4jlogger.trace("dispose()");
			if (reader != null) {
				try {
		slf4jlogger.trace("dispose(): Calling dispose() on reader");
					reader.dispose();	// http://info.michael-simons.eu/2012/01/25/the-dangers-of-javas-imageio/
				}
				catch (Exception e) {
					slf4jlogger.error("", e);
				}
			}
		}

}

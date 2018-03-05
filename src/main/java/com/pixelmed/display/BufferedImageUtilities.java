/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.io.PrintStream;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BandCombineOp;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class BufferedImageUtilities {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/BufferedImageUtilities.java,v 1.49 2017/02/28 16:12:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(BufferedImageUtilities.class);
	
	// The following image description stuff is extracted from Greg Guerin's
	// ImagerTrials stuff at "http://www.amug.org/~glguerin/other/index.html#ImagerTrials"

	protected static final String[] imageTypeNames =
	{
		"TYPE_CUSTOM",
		"TYPE_INT_RGB",
		"TYPE_INT_ARGB",
		"TYPE_INT_ARGB_PRE",
		"TYPE_INT_BGR",
		"TYPE_3BYTE_BGR",
		"TYPE_4BYTE_ABGR",
		"TYPE_4BYTE_ABGR_PRE",
		"TYPE_USHORT_565_RGB",  // 1.3.1 agony
		"TYPE_USHORT_555_RGB",
		"TYPE_BYTE_GRAY",
		"TYPE_USHORT_GRAY",  // 1.3.1 agony
		"TYPE_BYTE_BINARY",  // 1.3.1 agony
		"TYPE_BYTE_INDEXED",  // 1.3.1 agony
	 };

	/** Indexes correspond to DataBuffer.TYPE_xxx values, except TYPE_UNDEFINED. */
	protected static final String[] bufferTypeNames =
	{  "UBYTE", "USHORT", "short", "int", "float", "double"  };


	/** Indexes correspond to some ColorSpace.TYPE_xxx values. */
	protected static final String[] spaceTypeNames =
	{  "XYZ", "Lab", "Luv", "YCbCr", "Yxy", "RGB", "Grayscale", "HSV", "HLS", "CMYK", "type-10", "CMY"  };

	/** Return name for given BufferedImage type. */
	public static String typeName(int imageType) {  return imageTypeNames[imageType];  }

	/**
	 * <p>Return name for integer type and/or for BufferedImage's actual type.</p>
	 * <p>If image is null, then name is for imageType alone.</p>
	 * <p>If image is non-null, and its type matches imageType, then name is for imageType alone.</p>
	 * <p>If image's type doesn't match imageType, then name is first for imageType,
	 * followed by image's actual type name in parentheses.</p>
	 *
	 * @param	imageType
	 * @param	image
	 * @return			String name
	 */
	public static String typeName(int imageType,BufferedImage image) {
		String typeName = typeName(imageType);
		if (image == null) {
			return typeName;
		}
		// Evaluate image's actual type.
		int actualType = image.getType();
		if (actualType == imageType) {
			return typeName;
		}
		return typeName + " (" + typeName(actualType) + ")";
	}

	/*
	 * <p>Return name for DataBuffer or other transfer-type.</p>
	 *
	 * @param	bufferType
	 * @return			String name
	 */
	public static String transferTypeName(int bufferType) {
		if ( bufferType < 0  || bufferType >= bufferTypeNames.length )
			return "UNKNOWN";
		else
			return bufferTypeNames[bufferType];
	}

	/**
	 * <p>Return name for its color-space type.</p>
	 *
	 * @param	space
	 * @return		String name
	 */
	public static String typeName(ColorSpace space) {
		if (space == null) {
			return "NULL";
		}
		int type = space.getType();
		if (type < 0  ||  type >= spaceTypeNames.length) {
			return "UNKNOWN";
		}
		if (type >= spaceTypeNames.length) {
			return String.valueOf( type - 10 ) + "-color space";
		}
		return spaceTypeNames[type];
	}

	/**
	 * <p>Describe characteristics of BufferedImage's Raster, SampleModel, ColorModel, etc.</p>
	 *
	 * @param	image
	 * @param	out
	 */
	public static void describeImage(BufferedImage image,PrintStream out) {
		if (image == null) {
			out.println("Image: null");
		}
		else {
			out.println("Image: "+image);
			out.println("Image: width "+image.getWidth());
			out.println("Image: height "+image.getHeight());
			describeRaster(image.getRaster(),out);
			describeColorModel(image.getColorModel(),out);
		}
	}

	/**
	 * <p>Describe characteristics of Raster.</p>
	 *
	 * @param	raster
	 * @param	out
	 */
	public static void describeRaster(Raster raster,PrintStream out) {
		if (raster == null) {
			out.println("    **** Raster: null");
		}
		else {
			out.println("    **** Raster: "+raster);
			out.println( "    **** Raster: " + raster.getClass().getName());
			SampleModel model = raster.getSampleModel();
			if (model == null) {
				out.println("    SampleModel: null");
			}
			else {
				out.println("    SampleModel: "+model);
				out.println("    SampleModel: " + model.getClass().getName() + " -- "
					+ model.getNumDataElements() + " " 
					+ transferTypeName(model.getTransferType()) + "s/pixel, "
					+ model.getNumBands() + " bands" );
			}
			DataBuffer buffer = raster.getDataBuffer();
			if (buffer == null) {
				out.println("     DataBuffer: null");
			}
			else {
				out.println("     DataBuffer: "+buffer);
				out.println("     DataBuffer: " + buffer.getClass().getName() + " -- "
					+ buffer.getNumBanks() + " " 
					+ transferTypeName(buffer.getDataType()) + " banks" );
			}
		}
	}

	/**
	 * <p>Describe characteristics of ColorModel.</p>
	 *
	 * @param	model
	 * @param	out
	 */
	public static void describeColorModel(ColorModel model,PrintStream out) {
		if (model == null) {
			out.println("     ColorModel: null");
		}
		else {
			out.println("     ColorModel: ="+model);
			ColorSpace space = model.getColorSpace();
			String alpha = "no alpha";
			if (model.hasAlpha()) {
				if (model.isAlphaPremultiplied()) {
					alpha = "premult-alpha";
				}
				else {
					alpha = "alpha";
				}
			}
			int comp = model.getNumComponents();
			int ccomp = model.getNumColorComponents();
			String parts = String.valueOf(ccomp);
			if (comp != ccomp) {
				parts = String.valueOf(ccomp) + ":" + comp;
			}

			out.println("     ColorModel: " + model.getClass().getName() + " -- "
				+ model.getPixelSize() + " bits/" + parts + "-part "
				+ transferTypeName(model.getTransferType()) + " pixel, " + alpha );

			out.println("     ColorSpace: " + space.getClass().getName() + " -- "
				+ typeName(space)  + " space" );
		}
	}
	
	// End of Greg Guerin's stuff

	private static GraphicsConfiguration defaultGraphicsConfiguration = initializeGraphicsConfiguration();

	/**
	 * @return	the GraphicsConfiguration that is likely to perform most efficiently on this host
	 */
	public static GraphicsConfiguration getDefaultGraphicsConfiguration() { return defaultGraphicsConfiguration; }
	
	/**
	 * @return	the GraphicsConfiguration that is likely to perform most efficiently on this host
	 */
	private static GraphicsConfiguration initializeGraphicsConfiguration() {
		GraphicsConfiguration graphicsConfiguration = null;
		if (System.getProperty("java.awt.headless","false").equals("false")) {
			try {
				graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
			}
			catch (java.awt.HeadlessException e) {
				slf4jlogger.error("",e);
			}
		}
		return graphicsConfiguration;
	}
	
	private static ColorModel mostFavorableColorModel = initializeMostFavorableColorModel();

	/**
	 * @return	the ColorModel that is likely to perform most efficiently on this host
	 */
	public static ColorModel getMostFavorableColorModel() { return mostFavorableColorModel; }
	
	/**
	 * @return	the ColorModel that is likely to perform most efficiently on this host
	 */
	private static ColorModel initializeMostFavorableColorModel() {
		ColorModel colorModel = null;
		GraphicsConfiguration graphicsConfiguration = getDefaultGraphicsConfiguration();
		if (graphicsConfiguration != null) {
			colorModel = graphicsConfiguration.getColorModel();
		}
		if (colorModel == null) {
		slf4jlogger.debug("initializeMostFavorableColorModel(): no model from getLocalGraphicsEnvironment; perhaps headless");
			if (System.getProperty("os.name","").equals("Mac OS X")) {
		slf4jlogger.debug("initializeMostFavorableColorModel(): on Mac OS X, so assume 32 bit ARGB");
				colorModel = new DirectColorModel(
					ColorSpace.getInstance(ColorSpace.CS_sRGB),
					32,		// bits
					0x00ff0000,	// rmask
					0x0000ff00,	// gmask
					0x000000ff,	// bmask
					0xff000000,	// amask
					true,		// alpha premultipled
					DataBuffer.TYPE_INT
				);
			}
			else {
		slf4jlogger.debug("initializeMostFavorableColorModel(): not on Mac OS X, so assume  24 bit RGB");
				colorModel = new DirectColorModel(
					ColorSpace.getInstance(ColorSpace.CS_sRGB),
					24,		// bits
					0x00ff0000,	// rmask
					0x0000ff00,	// gmask
					0x000000ff,	// bmask
					0x00000000,	// amask
					false,		// alpha not premultipled (no alpha)
					DataBuffer.TYPE_INT
				);
			}
		}
		slf4jlogger.debug("initializeMostFavorableColorModel():");
//describeColorModel(colorModel,System.err);
		return colorModel;
	}

	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToMostFavorableImageTypeWithPixelCopy(BufferedImage srcImage) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): start");
		long startTime = System.currentTimeMillis();
		//ColorModel dstColorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		ColorModel dstColorModel = getMostFavorableColorModel();
		if (dstColorModel == null) {
			slf4jlogger.info("convertToMostFavorableImageTypeWithPixelCopy(): no mostFavorableColorModel - doing nothing");
			return srcImage;
		}
		
		ColorModel srcColorModel = srcImage.getColorModel();
		if (dstColorModel.equals(srcColorModel)) {
			slf4jlogger.info("convertToMostFavorableImageTypeWithPixelCopy(): already mostFavorableColorModel - doing nothing");
			return srcImage;
		}
				
		int width = srcImage.getWidth();
		int height = srcImage.getHeight();
        
		SampleModel srcSampleModel = srcImage.getSampleModel();
		
		if (srcSampleModel instanceof java.awt.image.MultiPixelPackedSampleModel) {
			// This is encountered with single bit images
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): not attempting to convert MultiPixelPackedSampleModel this way");
			return null;
		}

		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcNumBands = srcRaster.getNumBands();

		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): srcImage ={}",srcImage);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): srcSampleModel ={}",srcSampleModel);
		{
			if (srcSampleModel instanceof ComponentSampleModel) {
				slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): srcSampleModel.getPixelStride() ={}",((ComponentSampleModel)srcSampleModel).getPixelStride());
			}
			else {
				slf4jlogger.info("convertToMostFavorableImageTypeWithPixelCopy(): srcSampleModel is not ComponentSampleModel but is {}",srcSampleModel.getClass().getName());
			}
		}

		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(width,height);
		DataBuffer dstDataBuffer = dstRaster.getDataBuffer();
		BufferedImage dstImage = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);

		SampleModel dstSampleModel = dstImage.getSampleModel();
		int dstNumBands = dstRaster.getNumBands();
        
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): dstImage ={}",dstImage);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): dstSampleModel ={}",dstSampleModel);

		int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		srcPixels = srcSampleModel.getPixels(0,0,width,height,srcPixels,srcDataBuffer);
		int srcPixelsLength = srcPixels.length;

		int dstPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		dstPixels = dstSampleModel.getPixels(0,0,width,height,dstPixels,dstDataBuffer);
		int dstPixelsLength = dstPixels.length;
        
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy() after getPixels, elapsed: {} ms",(System.currentTimeMillis()-startTime));

		if (srcNumBands == 1 && dstNumBands == 4 && srcPixelsLength*4 == dstPixelsLength) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): converting gray to RGBA");
		int dstIndex=0;
		for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
			dstPixels[dstIndex++]=srcPixels[srcIndex];
			dstPixels[dstIndex++]=srcPixels[srcIndex];
			dstPixels[dstIndex++]=srcPixels[srcIndex];
			dstPixels[dstIndex++]=-1;
		}
		dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 1 && dstNumBands == 3 && srcPixelsLength*3 == dstPixelsLength) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): converting gray to RGB");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
				dstPixels[dstIndex++]=srcPixels[srcIndex];
				dstPixels[dstIndex++]=srcPixels[srcIndex];
				dstPixels[dstIndex++]=srcPixels[srcIndex];
			}
			dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 3 && dstNumBands == 4 && srcPixelsLength*4 == dstPixelsLength*3) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): converting pixel or band interleaved 3 band to RGBA");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength;) {
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
				dstPixels[dstIndex++]=-1;
			}
			dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 3 && dstNumBands == 3 && srcPixelsLength == dstPixelsLength) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): converting pixel or band interleaved 3 band to RGB");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength;) {
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
				dstPixels[dstIndex++]=srcPixels[srcIndex++];
			}
			dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else {
			slf4jlogger.info("convertToMostFavorableImageTypeWithPixelCopy(): No conversion supported");
			dstImage=srcImage;
		}
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertToMostFavorableImageTypeWithPixelCopy(): done = {}",dstImage);
		return dstImage;
	}

	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToMostFavorableImageTypeWithDataBufferCopy(BufferedImage srcImage) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): start");
		long startTime = System.currentTimeMillis();
		ColorModel dstColorModel = getMostFavorableColorModel();
		if (dstColorModel == null) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): no mostFavorableColorModel - doing nothing");
			return srcImage;
		}
		ColorModel srcColorModel = srcImage.getColorModel();
		if (dstColorModel.equals(srcColorModel)) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): already mostFavorableColorModel - doing nothing");
			return srcImage;
		}

		int srcColorModelNumComponents = srcColorModel.getNumComponents();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstColorModelNumComponents = {}",srcColorModelNumComponents);
		int dstColorModelNumComponents = dstColorModel.getNumComponents();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstColorModelNumComponents = {}",dstColorModelNumComponents);

		if (srcColorModelNumComponents != dstColorModelNumComponents) {
			return null;	// bail out before wasting time allocating dstRaster, which takes a while
		}

		if (slf4jlogger.isDebugEnabled()) {
			ColorSpace srcColorSpace = srcColorModel.getColorSpace();
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcColorSpace = {}",srcColorSpace);
			int srcColorSpaceType =srcColorSpace.getType();
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcColorSpaceType = {}",srcColorSpaceType);
			ColorSpace dstColorSpace = dstColorModel.getColorSpace();
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstColorSpace = {}",dstColorSpace);
			int dstColorSpaceType = dstColorSpace.getType();
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstColorSpaceType = {}",dstColorSpaceType);
		}
		
		int columns = srcImage.getWidth();
		int rows = srcImage.getHeight();

		SampleModel srcSampleModel = srcImage.getSampleModel();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcSampleModel = {}",srcSampleModel);
		int srcDataType = srcSampleModel.getDataType();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcDataType = {}",srcDataType);
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcDataBufferType = srcDataBuffer.getDataType();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcDataBufferType = {}",srcDataBufferType);
		int srcNumBands = srcRaster.getNumBands();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcNumBands = {}",srcNumBands);
		int srcPixelStride = srcNumBands;
		int srcScanlineStride = columns*srcNumBands;
		if (srcSampleModel instanceof ComponentSampleModel) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcSampleModel is instanceof ComponentSampleModel");
			ComponentSampleModel srcComponentSampleModel = (ComponentSampleModel)srcSampleModel;
			srcPixelStride = srcComponentSampleModel.getPixelStride();			// should be either srcNumBands if color-by-pixel, or 1 if color-by-plane
			srcScanlineStride = srcComponentSampleModel.getScanlineStride();	// should be either columns*srcNumBands if color-by-pixel, or columns if color-by-plane
		}
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcPixelStride = {}",srcPixelStride);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcScanlineStride = {}",srcScanlineStride);
		int srcDataBufferOffset = srcDataBuffer.getOffset();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcDataBufferOffset = {}",srcDataBufferOffset);
		int srcFrameLength = rows*columns*srcNumBands;
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcFrameLength = {}",srcFrameLength);
		int srcDataBufferNumBanks = srcDataBuffer.getNumBanks();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcDataBufferNumBanks = {}",srcDataBufferNumBanks);


		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy() before creating dstRaster - elapsed: {} ms",(System.currentTimeMillis()-startTime));
		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(columns,rows);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy() before creating dstImage - elapsed: {} ms",(System.currentTimeMillis()-startTime));
		BufferedImage dstImage = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);
		SampleModel dstSampleModel = dstImage.getSampleModel();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstSampleModel = {}",dstSampleModel);
		int dstDataType = dstSampleModel.getDataType();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstDataType = {}",dstDataType);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy() before getting dstDataBuffer - elapsed: {} ms",(System.currentTimeMillis()-startTime));
		DataBuffer dstDataBuffer = dstRaster.getDataBuffer();
		int dstDataBufferType = dstDataBuffer.getDataType();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstDataBufferType = {}",dstDataBufferType);
		int dstNumBands = dstRaster.getNumBands();
		int dstPixelStride = dstNumBands;
		int dstScanlineStride = columns*dstNumBands;
		if (dstSampleModel instanceof ComponentSampleModel) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstSampleModel is instanceof ComponentSampleModel");
			ComponentSampleModel dstComponentSampleModel = (ComponentSampleModel)dstSampleModel;
			dstPixelStride = dstComponentSampleModel.getPixelStride();			// should be either dstNumBands if color-by-pixel, or 1 if color-by-plane
			dstScanlineStride = dstComponentSampleModel.getScanlineStride();	// should be either columns*dstNumBands if color-by-pixel, or columns if color-by-plane
		}
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstPixelStride = {}",dstPixelStride);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstScanlineStride = {}",dstScanlineStride);
		int dstDataBufferOffset = dstDataBuffer.getOffset();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstDataBufferOffset = {}",dstDataBufferOffset);
		int dstFrameLength = rows*columns*dstNumBands;
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstFrameLength = {}",dstFrameLength);
		int dstDataBufferNumBanks = dstDataBuffer.getNumBanks();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstDataBufferNumBanks = {}",dstDataBufferNumBanks);
		
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy() before attempting copy - elapsed: {} ms",(System.currentTimeMillis()-startTime));

		if (srcDataBufferNumBanks == 1 && dstDataBufferNumBanks == 1
		 && srcSampleModel instanceof ComponentSampleModel && dstSampleModel instanceof SinglePixelPackedSampleModel
		 && srcDataBufferType == DataBuffer.TYPE_BYTE && dstDataBufferType == DataBuffer.TYPE_INT
		 && srcDataBuffer instanceof DataBufferByte && dstDataBuffer instanceof DataBufferInt
		 && srcPixelStride == srcNumBands
		 && srcNumBands == dstNumBands) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): converting {} band interleaved byte ComponentSampleModel to {} band int SinglePixelPackedSampleModel",srcNumBands,dstNumBands);
			byte[][] srcPixelBanks = ((DataBufferByte)srcDataBuffer).getBankData();
			byte[] srcPixelBank = srcPixelBanks[0];
			int srcPixelBankLength = srcPixelBank.length;
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): srcPixelBankLength = {}",srcPixelBankLength);
			int[][] dstPixelBanks = ((DataBufferInt)dstDataBuffer).getBankData();
			int[] dstPixelBank = dstPixelBanks[0];
			int dstPixelBankLength = dstPixelBank.length;
			slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): dstPixelBankLength = {}",dstPixelBankLength);

			int[] dstBitMasks = ((SinglePixelPackedSampleModel)dstSampleModel).getBitMasks();
			int[] dstBitOffsets = ((SinglePixelPackedSampleModel)dstSampleModel).getBitOffsets();
			
			int srcIndex = srcDataBufferOffset;
			int dstIndex = dstDataBufferOffset;
			for (int row=0; row<rows; ++row) {
				for (int column=0; column<columns; ++column) {
					for (int band=0; band<srcNumBands; ++band) {
						dstPixelBank[dstIndex] = dstPixelBank[dstIndex] | ((srcPixelBank[srcIndex++] << dstBitOffsets[band]) & dstBitMasks[band]);
					}
					++dstIndex;
				}
			}
		}
		else {
			dstImage=null;
		}
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertToMostFavorableImageTypeWithDataBufferCopy(): done = {}",dstImage);
		return dstImage;
	}

	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToMostFavorableImageTypeWithBandCombineOp(BufferedImage srcImage) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithBandCombineOp(): start");
		long startTime = System.currentTimeMillis();
		//ColorModel dstColorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		ColorModel dstColorModel = getMostFavorableColorModel();
		if (dstColorModel == null) {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithBandCombineOp(): no mostFavorableColorModel - doing nothing");
			return srcImage;
		}
		
		int dstNumBands = dstColorModel.getNumComponents();	// NB. not getNumColorComponents()
		int srcNumBands = srcImage.getRaster().getNumBands();
		float [][] bandCombine = null;
		if (dstColorModel.getNumComponents() == 4) {	// NB. not getNumColorComponents()
			if (srcNumBands == 1) {
				float [][] combine = {
					{ 1, 0 },
					{ 1, 0 },
					{ 1, 0 },
					{ 0, 0xffff }
				};
				bandCombine = combine;
			}
			else if (srcNumBands == 3) {
				float [][] combine = {
					{ 1, 0, 0, 0 },
					{ 0, 1, 0, 0 },
					{ 0, 0, 1, 0 },
					{ 0, 0, 0, 0xffff }
				};
				bandCombine = combine;
			}
		}
		else if (dstColorModel.getNumComponents() == 3) {
			if (srcNumBands == 1) {
				float [][] combine = {
					{ 1, 0 },
					{ 1, 0 },
					{ 1, 0 }
				};
				bandCombine = combine;
			}
			else if (srcNumBands == 3) {
				float [][] combine = {
					{ 1, 0, 0, 0 },
					{ 0, 1, 0, 0 },
					{ 0, 0, 1, 0 }
				};
				bandCombine = combine;
			}
		}
		else {
			slf4jlogger.debug("convertToMostFavorableImageTypeWithBandCombineOp(): mostFavorableColorModel does not have 4 components - doing nothing");
			return null;
		}
		
		BandCombineOp bandCombineOp = new BandCombineOp(bandCombine,null);
			
		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(srcImage.getWidth(),srcImage.getHeight());
		BufferedImage dstImage = new BufferedImage(dstColorModel,dstRaster,dstColorModel.isAlphaPremultiplied(),null);
		bandCombineOp.filter(srcImage.getRaster(),dstRaster);
		slf4jlogger.debug("convertToMostFavorableImageTypeWithBandCombineOp() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertToMostFavorableImageTypeWithBandCombineOp(): done = {}",dstImage);
		return dstImage;
	}

	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToMostFavorableImageTypeWithGraphicsDraw(BufferedImage srcImage) {
		slf4jlogger.debug("convertToMostFavorableImageTypeWithGraphicsDraw(): start");
		long startTime = System.currentTimeMillis();

	// See "http://forums.java.net/jive/thread.jspa?messageID=180964"
	
		BufferedImage dstImage = null;
		GraphicsConfiguration graphicsConfiguration = getDefaultGraphicsConfiguration();
		slf4jlogger.debug("convertToMostFavorableImageTypeWithGraphicsDraw(): graphicsConfiguration = {}",graphicsConfiguration);
		if (graphicsConfiguration != null) {
			dstImage = graphicsConfiguration.createCompatibleImage(srcImage.getWidth(),srcImage.getHeight());
			if (dstImage != null) {
				Graphics2D g2 = dstImage.createGraphics();
				g2.setComposite(AlphaComposite.Src);
				g2.drawImage(srcImage,0,0,null);
				g2.dispose();
			}
		}
		slf4jlogger.debug("convertToMostFavorableImageTypeWithGraphicsDraw() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertToMostFavorableImageTypeWithGraphicsDraw(): done = {}",dstImage);
		return dstImage;
	}

	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToMostFavorableImageType(BufferedImage srcImage) {
		BufferedImage dstImage = null;
		if (srcImage.getColorModel().equals(getMostFavorableColorModel())) {
		slf4jlogger.debug("convertToMostFavorableImageType(): do nothing since same ColorModel");
			dstImage = srcImage;
		}
		else {
			//if (dstImage == null) {
				dstImage = convertToMostFavorableImageTypeWithDataBufferCopy(srcImage);
			//}
			if (dstImage == null) {
		slf4jlogger.debug("convertToMostFavorableImageType(): convertToMostFavorableImageTypeWithDataBufferCopy failed");
				dstImage = convertToMostFavorableImageTypeWithPixelCopy(srcImage);
			}
			if (dstImage == null) {
		slf4jlogger.debug("convertToMostFavorableImageType(): convertToMostFavorableImageTypeWithPixelCopy failed");
				dstImage = convertToMostFavorableImageTypeWithGraphicsDraw(srcImage);
			}
			if (dstImage == null) {
		slf4jlogger.debug("convertToMostFavorableImageType(): convertToMostFavorableImageTypeWithGraphicsDraw failed");
		slf4jlogger.debug("convertToMostFavorableImageType(): returning srcImage");
				dstImage=srcImage;	// do no conversion and hope for the best (performance) :(
			}
		}
		return dstImage;
	}
	
	/**
	 * @param	srcImage
	 */
	public static final BufferedImage convertToThreeChannelImageTypeIfFour(BufferedImage srcImage) {
		slf4jlogger.debug("convertToThreeChannelImageType(): start");
		long startTime = System.currentTimeMillis();
		int srcNumBands = srcImage.getRaster().getNumBands();
		if (srcNumBands != 4) {
			return srcImage;	// do nothing
		}

		// This color model is what we use in SourceImage when reading RGB images, and seems to work with JPEG encoder
		ColorModel dstColorModel = new ComponentColorModel(
		ColorSpace.getInstance(ColorSpace.CS_sRGB),
			new int[] {8,8,8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		
		float [][] collapseToThreeBands = {
			{ 1, 0, 0, 0 },
			{ 0, 1, 0, 0 },
			{ 0, 0, 1, 0 },
		};
		BandCombineOp collapseToThreeBandsOp = new BandCombineOp(collapseToThreeBands,null);
			
		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(srcImage.getWidth(),srcImage.getHeight());
		BufferedImage dstImage = new BufferedImage(dstColorModel,dstRaster,dstColorModel.isAlphaPremultiplied(),null);
		collapseToThreeBandsOp.filter(srcImage.getRaster(),dstRaster);
		slf4jlogger.debug("convertToThreeChannelImageType() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertToThreeChannelImageType(): done");
		return dstImage;
	}
	
	// Image resampling stuff ...

	private class ResamplingVector {
		//double [][] arrayOfWeights;
		int [][]    arrayOfWeights;
		int [][] arrayOfSrcIndices;
		int []     numberOfEntries;
		//double [] sumOfWeights;
		int [] sumOfWeights;
		int maxNumberOfSrcSamplesPerDst;
		int divisor;

		ResamplingVector(int nSrcSamples,int nDstSamples) {
			//divisor = nDstSamples*nSrcSamples;
			divisor = 1000;
			slf4jlogger.debug("ResamplingVector(): divisor ={}",divisor);
			double ratioSrcToDst = ((double)nSrcSamples)/nDstSamples;
			maxNumberOfSrcSamplesPerDst = (int)java.lang.Math.ceil(ratioSrcToDst)+1;
			double srcOffset = 0;
			int srcIndex = 0;
			//   arrayOfWeights = new double [nDstSamples][];
			   arrayOfWeights = new int [nDstSamples][];
			arrayOfSrcIndices = new int [nDstSamples][];
			  numberOfEntries = new int [nDstSamples];
			//   sumOfWeights = new double [nDstSamples];
			     sumOfWeights = new int [nDstSamples];
			for (int dstIndex=0; dstIndex<nDstSamples; ++dstIndex) {
				//arrayOfWeights[dstIndex] = new double[maxNumberOfSrcSamplesPerDst];
				arrayOfWeights[dstIndex] = new int[maxNumberOfSrcSamplesPerDst];
				arrayOfSrcIndices[dstIndex] = new int[maxNumberOfSrcSamplesPerDst];
				double stillNeedFromSrcForCurrentDst = ratioSrcToDst;
				for (int i=0; stillNeedFromSrcForCurrentDst > 0.0001; ++i) {		// test against zero fails sometimes and srcIndex exceeds nSrcSamples
					double weightForThisSrc=java.lang.Math.min(1.0-srcOffset,stillNeedFromSrcForCurrentDst);
					stillNeedFromSrcForCurrentDst-=weightForThisSrc;
					srcOffset+=weightForThisSrc;
					slf4jlogger.trace("dstIndex ={} srcIndex={} weightForThisSrc={} *divisor={}",dstIndex,srcIndex,weightForThisSrc,(int)(weightForThisSrc*divisor));
					//arrayOfWeights[dstIndex][i]=weightForThisSrc;
					arrayOfWeights[dstIndex][i]=(int)(weightForThisSrc*divisor);
					//sumOfWeights[dstIndex]+=weightForThisSrc;
					sumOfWeights[dstIndex]+=(int)(weightForThisSrc*divisor);
					arrayOfSrcIndices[dstIndex][i]=srcIndex;
					numberOfEntries[dstIndex]=i+1;
					if (srcOffset >= 1.0) {
						++srcIndex;
						srcOffset=0;
					}
				}
			}
			if (slf4jlogger.isDebugEnabled()) dump();
		}
		
		void dump() {
			for (int dstIndex=0; dstIndex<arrayOfSrcIndices.length; ++dstIndex) {
				for (int i=0; i<numberOfEntries[dstIndex]; ++i) {
					slf4jlogger.debug("dstIndex ={} srcIndex={} weight={}",dstIndex,arrayOfSrcIndices[dstIndex][i],arrayOfWeights[dstIndex][i]);
				}
			}
		}
	}
	
//	public final BufferedImage resample(BufferedImage srcImage,int dstWidth,int dstHeight,boolean signed) {
//		return resample(srcImage,dstWidth,dstHeight,signed,0);
//	}

	public final BufferedImage resample(BufferedImage srcImage,int dstWidth,int dstHeight,boolean signed,int backgroundValue) {
		return resample(srcImage,srcImage.getWidth(),srcImage.getHeight(),0,0,dstWidth,dstHeight,signed,backgroundValue);
	}

//	public final BufferedImage resample(BufferedImage srcImage,int selectionWidth,int selectionHeight,int selectionXOffset,int selectionYOffset,int dstWidth,int dstHeight,boolean signed) {
//		return resample(srcImage,selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight,signed,0);
//	}

	public final BufferedImage resample(BufferedImage srcImage,int selectionWidth,int selectionHeight,int selectionXOffset,int selectionYOffset,int dstWidth,int dstHeight,boolean signed,int backgroundValue) {
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();
		slf4jlogger.debug("resample():");
		slf4jlogger.debug("resample(): srcWidth = {}",srcWidth);
		slf4jlogger.debug("resample(): srcHeight = {}",srcHeight);
		slf4jlogger.debug("resample(): selectionWidth = {}",selectionWidth);
		slf4jlogger.debug("resample(): selectionHeight = {}",selectionHeight);
		slf4jlogger.debug("resample(): selectionXOffset = {}",selectionXOffset);
		slf4jlogger.debug("resample(): selectionYOffset = {}",selectionYOffset);
		slf4jlogger.debug("resample(): dstWidth = {}",dstWidth);
		slf4jlogger.debug("resample(): dstHeight = {}",dstHeight);
		slf4jlogger.debug("resample(): signed = {}",signed);

		// Do not resample if not needed ...
		if (srcWidth == dstWidth && srcHeight == dstHeight
		 && selectionWidth == srcWidth && selectionHeight == dstHeight
		 && selectionXOffset == 0 && selectionYOffset == 0) {
			return srcImage;
		}

		slf4jlogger.debug("resample(): start");
		long startTime = System.currentTimeMillis();
       
		ColorModel srcColorModel = srcImage.getColorModel();
		slf4jlogger.debug("resample(): srcColorModel.getPixelSize() = {}",srcColorModel.getPixelSize());
		SampleModel srcSampleModel = srcImage.getSampleModel();
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcDataBufferOffset = srcDataBuffer.getOffset();
		slf4jlogger.debug("resample(): srcDataBuffer is {}",srcDataBuffer.getClass().getName());
		slf4jlogger.debug("resample(): srcDataBuffer.getOffset() is {}",srcDataBufferOffset);
		int srcNumBands = srcRaster.getNumBands();
		slf4jlogger.debug("resample(): srcNumBands = {}",srcNumBands);

		// DataBufferShort will not be encountered ... see comments in SourceImage.java
		if (srcNumBands != 1 || !(srcDataBuffer instanceof DataBufferUShort /*|| srcDataBuffer instanceof DataBufferShort*/ || srcDataBuffer instanceof DataBufferByte || srcDataBuffer instanceof DataBufferFloat || srcDataBuffer instanceof DataBufferDouble) || srcColorModel.getPixelSize() == 1) {
			slf4jlogger.debug("resample(): not doing our own resampling");
			slf4jlogger.debug("resample(): before resampleWithGraphicsDraw elapsed = {} ms",(System.currentTimeMillis()-startTime));
			BufferedImage dstImage = null;
			try {
				//dstImage = resampleWithAffineTransformOp(srcImage,dstWidth,dstHeight);	// just doesn't work ... always throws exceptions; also doesn't address offset  :(
				dstImage = resampleWithGraphicsDraw(srcImage,selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight);		// need to use this for 16 bit color ? performance impact on Mac with other image types ? :( (000984); will cause color windowing to fail on single bit images (000996)
				//dstImage = resampleWithGraphicsDraw(convertToMostFavorableImageType(srcImage),selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight);	// was using this before but will NOT work for 16 bit color (000984)
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
				dstImage = null;
			}
			slf4jlogger.debug("resample(): after resampleWithGraphicsDraw elapsed = {} ms",(System.currentTimeMillis()-startTime));
			//return dstImage == null ? srcImage : dstImage;
			return dstImage;
		}

		BufferedImage dstImage = null;
		slf4jlogger.debug("resample(): hVector");
		ResamplingVector hVector =  new ResamplingVector(selectionWidth,dstWidth);
		slf4jlogger.debug("resample(): vVector");
		ResamplingVector vVector =  new ResamplingVector(selectionHeight,dstHeight);
		
		int leftSideLimit = 0;										// prevents horizontal resampling from wrapping to previous or next line
		int rightSideLimit = srcWidth - 1;							// NOT selectionWidth
		slf4jlogger.debug("resample(): leftSideLimit = {}",leftSideLimit);
		slf4jlogger.debug("resample(): rightSideLimit = {}",rightSideLimit);

		int topSideLimit = selectionYOffset < 0 ? -selectionYOffset : 0;
		int bottomSideLimit = topSideLimit + srcHeight - 1;
		slf4jlogger.debug("resample(): topSideLimit = {}",topSideLimit);
		slf4jlogger.debug("resample(): bottomSideLimit = {}",bottomSideLimit);


		if (srcDataBuffer instanceof DataBufferUShort) {
			slf4jlogger.debug("resample(): DataBufferUShort");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {16},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_USHORT
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_USHORT,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			slf4jlogger.debug("resample(): got info = {} ms",(System.currentTimeMillis()-startTime));

			int srcmask=signed ? 0xffffffff : 0x0000ffff;
		
			short srcPixels[];
			//if (srcDataBuffer instanceof DataBufferUShort) {
				srcPixels = ((DataBufferUShort)srcDataBuffer).getData();
			//}
			//else {
			//	srcPixels = ((DataBufferShort)srcDataBuffer).getData();
			//}
			int srcPixelsLength = srcPixels.length;
			slf4jlogger.debug("resample(): got srcPixels = {} ms",(System.currentTimeMillis()-startTime));

			int dstPixelsLength = dstWidth*dstHeight;
			short dstPixels[] = new short[dstPixelsLength];
			slf4jlogger.debug("resample(): got dstPixels = {} ms",(System.currentTimeMillis()-startTime));

			slf4jlogger.debug("resample(): single band");
			slf4jlogger.debug("resample(): single band = {} ms",(System.currentTimeMillis()-startTime));
			int dstRowBuffers[] = new int[selectionHeight*dstWidth];
			int bufferRowIndex[] = new int[selectionHeight];			// saves slow multiplication of selectionHeight*dstWidth
			int srcPixelOffset=srcDataBufferOffset;						// not zero, since may be later in shared buffer of multiple frames
			int dstPixelOffset=0;
			int lastBufferRowIndex=0;
			srcPixelOffset+=selectionYOffset*srcWidth;
			slf4jlogger.debug("resample(): srcPixelOffset = {}",srcPixelOffset);
			for (int srcY=0; srcY<selectionHeight; ++srcY,srcPixelOffset+=srcWidth/*,dstPixelOffset+=dstWidth*/) {
				bufferRowIndex[srcY]=lastBufferRowIndex;
				lastBufferRowIndex+=dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int arrayOfSrcIndices[] = hVector.arrayOfSrcIndices[dstX];
					int arrayOfWeights[] = hVector.arrayOfWeights[dstX];
					int     sumOfWeights = hVector.sumOfWeights[dstX];
					int     numberOfEntries = hVector.numberOfEntries[dstX];
					int dstPixel=0;
					for (int x=0; x<numberOfEntries; ++x) {
						int srcX=arrayOfSrcIndices[x]+selectionXOffset;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcX = "+srcX); }
						int weightX=arrayOfWeights[x];
						if (srcX >= leftSideLimit && srcX <= rightSideLimit) {
							int srcIndex = srcPixelOffset+srcX;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcIndex = "+srcIndex); }
							if (srcIndex > 0 && srcIndex < srcPixelsLength) {
								dstPixel+=((int)srcPixels[srcIndex]&srcmask)*weightX;	// tested for DataBufferUShort only
							}
						}
						else {
							dstPixel+=(backgroundValue*weightX);
						}
					}
					dstRowBuffers[dstPixelOffset++]=dstPixel/sumOfWeights;
				}
			}

			for (int dstY=0; dstY<dstHeight; ++dstY) {
				int arrayOfSrcIndices[] = vVector.arrayOfSrcIndices[dstY];
				int arrayOfWeights[] = vVector.arrayOfWeights[dstY];
				int     sumOfWeights = vVector.sumOfWeights[dstY];
				int     numberOfEntries = vVector.numberOfEntries[dstY];
				int pixelOffset = dstY*dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int dstPixel=0;
					for (int y=0; y<numberOfEntries; ++y) {
						int srcY=arrayOfSrcIndices[y];
						int weightY=arrayOfWeights[y];
						if (srcY >= topSideLimit && srcY <= bottomSideLimit) {
							dstPixel+=(int)dstRowBuffers[bufferRowIndex[srcY]+dstX]*weightY;
						}
						else {
							dstPixel+=(backgroundValue*weightY);
						}
					}
					dstPixels[pixelOffset+dstX]=(short)(dstPixel/sumOfWeights);			// this is faster than single increment
				}
			}
				
			slf4jlogger.debug("resample(): done with pixel copy = {} ms",(System.currentTimeMillis()-startTime));
			DataBuffer dstDataBuffer = new DataBufferUShort(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
			slf4jlogger.debug("resample(): done with creating dstImage = {} ms",(System.currentTimeMillis()-startTime));
		}
		else if (srcDataBuffer instanceof DataBufferByte) {
			slf4jlogger.debug("resample(): DataBufferByte");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {8},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_BYTE,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			slf4jlogger.debug("resample(): got info = {} ms",(System.currentTimeMillis()-startTime));

			int srcmask=signed ? 0xffffffff : 0x000000ff;
		
			byte srcPixels[];
			srcPixels = ((DataBufferByte)srcDataBuffer).getData();
			int srcPixelsLength = srcPixels.length;
			slf4jlogger.debug("resample(): got srcPixels = {} ms",(System.currentTimeMillis()-startTime));

			int dstPixelsLength = dstWidth*dstHeight;
			byte dstPixels[] = new byte[dstPixelsLength];
			slf4jlogger.debug("resample(): got dstPixels = {} ms",(System.currentTimeMillis()-startTime));
       

			slf4jlogger.debug("resample(): single band");
			slf4jlogger.debug("resample(): single band = {} ms",(System.currentTimeMillis()-startTime));
			int dstRowBuffers[] = new int[selectionHeight*dstWidth];
			int bufferRowIndex[] = new int[selectionHeight];			// saves slow multiplication of selectionHeight*dstWidth
			int srcPixelOffset=srcDataBufferOffset;						// not zero, since may be later in shared buffer of multiple frames
			int dstPixelOffset=0;
			int lastBufferRowIndex=0;
			srcPixelOffset+=selectionYOffset*srcWidth;
			for (int srcY=0; srcY<selectionHeight; ++srcY,srcPixelOffset+=srcWidth/*,dstPixelOffset+=dstWidth*/) {
				bufferRowIndex[srcY]=lastBufferRowIndex;
				lastBufferRowIndex+=dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int arrayOfSrcIndices[] = hVector.arrayOfSrcIndices[dstX];
					int arrayOfWeights[] = hVector.arrayOfWeights[dstX];
					int     sumOfWeights = hVector.sumOfWeights[dstX];
					int     numberOfEntries = hVector.numberOfEntries[dstX];
					int dstPixel=0;
					for (int x=0; x<numberOfEntries; ++x) {
						int srcX=arrayOfSrcIndices[x]+selectionXOffset;
						slf4jlogger.trace("resample(): srcX = {}",srcX);
						int weightX=arrayOfWeights[x];
						if (srcX >= leftSideLimit && srcX <= rightSideLimit) {
							int srcIndex = srcPixelOffset+srcX;
							if (srcIndex > 0 && srcIndex < srcPixelsLength) {
								dstPixel+=((int)srcPixels[srcIndex]&srcmask)*weightX;
							}
						}
						else {
							dstPixel+=(backgroundValue*weightX);
						}
					}
					dstRowBuffers[dstPixelOffset++]=dstPixel/sumOfWeights;
				}
			}

			for (int dstY=0; dstY<dstHeight; ++dstY) {
				int arrayOfSrcIndices[] = vVector.arrayOfSrcIndices[dstY];
				int arrayOfWeights[] = vVector.arrayOfWeights[dstY];
				int     sumOfWeights = vVector.sumOfWeights[dstY];
				int     numberOfEntries = vVector.numberOfEntries[dstY];
				int pixelOffset = dstY*dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int dstPixel=0;
					for (int y=0; y<numberOfEntries; ++y) {
						int srcY=arrayOfSrcIndices[y];
						int weightY=arrayOfWeights[y];
						if (srcY >= topSideLimit && srcY <= bottomSideLimit) {
							dstPixel+=(int)dstRowBuffers[bufferRowIndex[srcY]+dstX]*weightY;
						}
						else {
							dstPixel+=(backgroundValue*weightY);
						}
					}
					dstPixels[pixelOffset+dstX]=(byte)(dstPixel/sumOfWeights);		// this is faster than single increment
				}
			}
				
			slf4jlogger.debug("resample(): done with pixel copy = {} ms",(System.currentTimeMillis()-startTime));
			DataBuffer dstDataBuffer = new DataBufferByte(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
			slf4jlogger.debug("resample(): done with creating dstImage = {} ms",(System.currentTimeMillis()-startTime));
		}
		else if (srcDataBuffer instanceof DataBufferFloat) {
			slf4jlogger.debug("resample(): DataBufferFloat");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_FLOAT
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_FLOAT,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			slf4jlogger.debug("resample(): got info = {} ms",(System.currentTimeMillis()-startTime));
		
			float srcPixels[] = ((DataBufferFloat)srcDataBuffer).getData();

			int srcPixelsLength = srcPixels.length;
			slf4jlogger.debug("resample(): got srcPixels = {} ms",(System.currentTimeMillis()-startTime));

			int dstPixelsLength = dstWidth*dstHeight;
			float dstPixels[] = new float[dstPixelsLength];
			slf4jlogger.debug("resample(): got dstPixels = {} ms",(System.currentTimeMillis()-startTime));

			slf4jlogger.debug("resample(): single band");
			slf4jlogger.debug("resample(): single band = {} ms",(System.currentTimeMillis()-startTime));
			float dstRowBuffers[] = new float[selectionHeight*dstWidth];
			int bufferRowIndex[] = new int[selectionHeight];			// saves slow multiplication of selectionHeight*dstWidth
			int srcPixelOffset=srcDataBufferOffset;						// not zero, since may be later in shared buffer of multiple frames
			int dstPixelOffset=0;
			int lastBufferRowIndex=0;
			srcPixelOffset+=selectionYOffset*srcWidth;
			slf4jlogger.debug("resample(): srcPixelOffset = {}",srcPixelOffset);
			for (int srcY=0; srcY<selectionHeight; ++srcY,srcPixelOffset+=srcWidth/*,dstPixelOffset+=dstWidth*/) {
				bufferRowIndex[srcY]=lastBufferRowIndex;
				lastBufferRowIndex+=dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int arrayOfSrcIndices[] = hVector.arrayOfSrcIndices[dstX];
					int arrayOfWeights[] = hVector.arrayOfWeights[dstX];
					int     sumOfWeights = hVector.sumOfWeights[dstX];
					int     numberOfEntries = hVector.numberOfEntries[dstX];
					float dstPixel=0;
					for (int x=0; x<numberOfEntries; ++x) {
						int srcX=arrayOfSrcIndices[x]+selectionXOffset;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcX = "+srcX); }
						int weightX=arrayOfWeights[x];
						if (srcX >= leftSideLimit && srcX <= rightSideLimit) {
							int srcIndex = srcPixelOffset+srcX;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcIndex = "+srcIndex); }
							if (srcIndex > 0 && srcIndex < srcPixelsLength) {
								dstPixel+=(float)(srcPixels[srcIndex])*weightX;
								slf4jlogger.trace("resample(): srcIndex = {} srcPixels[srcIndex] = {} weightX = {} dstPixel now = {}",srcIndex,srcPixels[srcIndex],weightX,dstPixel);
							}
						}
						else {
							dstPixel+=(backgroundValue*weightX);
							slf4jlogger.trace("resample(): backgroundValue = {} weightX = {} dstPixel now = {}",backgroundValue,weightX,dstPixel);
						}
					}
					slf4jlogger.trace("resample(): dstPixelOffset = {} dstRowBuffers[dstPixelOffset] was = {} dstPixel = {} sumOfWeights = {} replacing dstRowBuffers[dstPixelOffset] with dstPixel/sumOfWeights = {}",dstPixelOffset,dstRowBuffers[dstPixelOffset],dstPixel,sumOfWeights,(dstPixel/sumOfWeights));
					dstRowBuffers[dstPixelOffset++]=dstPixel/sumOfWeights;
				}
			}

			for (int dstY=0; dstY<dstHeight; ++dstY) {
				int arrayOfSrcIndices[] = vVector.arrayOfSrcIndices[dstY];
				int arrayOfWeights[] = vVector.arrayOfWeights[dstY];
				int     sumOfWeights = vVector.sumOfWeights[dstY];
				int     numberOfEntries = vVector.numberOfEntries[dstY];
				int pixelOffset = dstY*dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					float dstPixel=0;
					for (int y=0; y<numberOfEntries; ++y) {
						int srcY=arrayOfSrcIndices[y];
						int weightY=arrayOfWeights[y];
						if (srcY >= topSideLimit && srcY <= bottomSideLimit) {
							dstPixel+=dstRowBuffers[bufferRowIndex[srcY]+dstX]*weightY;
							slf4jlogger.trace("resample(): srcY = {} bufferRowIndex[srcY] = {} dstX = {} dstRowBuffers[bufferRowIndex[srcY]+dstX] = {} weightY = {} dstPixel now = {}",srcY,bufferRowIndex[srcY],dstX,dstRowBuffers[bufferRowIndex[srcY]+dstX],weightY,dstPixel);
						}
						else {
							dstPixel+=(backgroundValue*weightY);
							slf4jlogger.trace("resample(): backgroundValue = {} weightY = {} dstPixel now = {}",backgroundValue,weightY,dstPixel);
						}
					}
					dstPixels[pixelOffset+dstX]=dstPixel/sumOfWeights;			// this is faster than single increment
					slf4jlogger.trace("resample(): pixelOffset = {} dstX = {} dstPixel = {} sumOfWeights = {} replacing dstPixels[pixelOffset+dstX] with dstPixel/sumOfWeights = {}",pixelOffset,dstX,dstPixel,sumOfWeights,dstPixels[pixelOffset+dstX]);
				}
			}
				
			slf4jlogger.debug("resample(): done with pixel copy = {} ms",(System.currentTimeMillis()-startTime));
			DataBuffer dstDataBuffer = new DataBufferFloat(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
			slf4jlogger.debug("resample(): done with creating dstImage = {} ms",(System.currentTimeMillis()-startTime));
		}
		else if (srcDataBuffer instanceof DataBufferDouble) {
			slf4jlogger.debug("resample(): DataBufferDouble");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_DOUBLE
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_DOUBLE,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			slf4jlogger.debug("resample(): got info = {} ms",(System.currentTimeMillis()-startTime));
		
			double srcPixels[] = ((DataBufferDouble)srcDataBuffer).getData();

			int srcPixelsLength = srcPixels.length;
			slf4jlogger.debug("resample(): got srcPixels = {} ms",(System.currentTimeMillis()-startTime));

			int dstPixelsLength = dstWidth*dstHeight;
			double dstPixels[] = new double[dstPixelsLength];
			slf4jlogger.debug("resample(): got dstPixels = {} ms",(System.currentTimeMillis()-startTime));

			slf4jlogger.debug("resample(): single band");
			slf4jlogger.debug("resample(): single band = {} ms",(System.currentTimeMillis()-startTime));
			double dstRowBuffers[] = new double[selectionHeight*dstWidth];
			int bufferRowIndex[] = new int[selectionHeight];			// saves slow multiplication of selectionHeight*dstWidth
			int srcPixelOffset=srcDataBufferOffset;						// not zero, since may be later in shared buffer of multiple frames
			int dstPixelOffset=0;
			int lastBufferRowIndex=0;
			srcPixelOffset+=selectionYOffset*srcWidth;
			slf4jlogger.debug("resample(): srcPixelOffset = {}",srcPixelOffset);
			for (int srcY=0; srcY<selectionHeight; ++srcY,srcPixelOffset+=srcWidth/*,dstPixelOffset+=dstWidth*/) {
				bufferRowIndex[srcY]=lastBufferRowIndex;
				lastBufferRowIndex+=dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					int arrayOfSrcIndices[] = hVector.arrayOfSrcIndices[dstX];
					int arrayOfWeights[] = hVector.arrayOfWeights[dstX];
					int     sumOfWeights = hVector.sumOfWeights[dstX];
					int     numberOfEntries = hVector.numberOfEntries[dstX];
					double dstPixel=0;
					for (int x=0; x<numberOfEntries; ++x) {
						int srcX=arrayOfSrcIndices[x]+selectionXOffset;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcX = "+srcX); }
						int weightX=arrayOfWeights[x];
						if (srcX >= leftSideLimit && srcX <= rightSideLimit) {
							int srcIndex = srcPixelOffset+srcX;
//if (srcY == 0) { System.err.println("BufferedImageUtilities.resample(): srcIndex = "+srcIndex); }
							if (srcIndex > 0 && srcIndex < srcPixelsLength) {
								dstPixel+=(double)(srcPixels[srcIndex])*weightX;
							}
						}
						else {
							dstPixel+=(backgroundValue*weightX);
						}
					}
					dstRowBuffers[dstPixelOffset++]=dstPixel/sumOfWeights;
				}
			}

			for (int dstY=0; dstY<dstHeight; ++dstY) {
				int arrayOfSrcIndices[] = vVector.arrayOfSrcIndices[dstY];
				int arrayOfWeights[] = vVector.arrayOfWeights[dstY];
				int     sumOfWeights = vVector.sumOfWeights[dstY];
				int     numberOfEntries = vVector.numberOfEntries[dstY];
				int pixelOffset = dstY*dstWidth;
				for (int dstX=0; dstX<dstWidth; ++dstX) {
					double dstPixel=0;
					for (int y=0; y<numberOfEntries; ++y) {
						int srcY=arrayOfSrcIndices[y];
						int weightY=arrayOfWeights[y];
						if (srcY >= topSideLimit && srcY <= bottomSideLimit) {
							dstPixel+=dstRowBuffers[bufferRowIndex[srcY]+dstX]*weightY;
						}
						else {
							dstPixel+=(backgroundValue*weightY);
						}
					}
					dstPixels[pixelOffset+dstX]=dstPixel/sumOfWeights;			// this is faster than single increment
				}
			}
				
			slf4jlogger.debug("resample(): done with pixel copy = {} ms",(System.currentTimeMillis()-startTime));
			DataBuffer dstDataBuffer = new DataBufferDouble(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
			slf4jlogger.debug("resample(): done with creating dstImage = {} ms",(System.currentTimeMillis()-startTime));
		}
		// else should never get here, since took care of this and returned earlier
		
		slf4jlogger.debug("resample() elapsed: "+(System.currentTimeMillis()-startTime)+" ms");
		return dstImage;
	}

	public final BufferedImage resampleWithGraphicsDraw(BufferedImage srcImage,int selectionWidth,int selectionHeight,int selectionXOffset,int selectionYOffset,int dstWidth,int dstHeight) {
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,int,int,int,int,int,int): start");
		long startTime = System.currentTimeMillis();
		BufferedImage dstImage = null;
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,int,int,int,int,int,int): try source ColorModel");
		ColorModel dstColorModel = srcImage.getColorModel();
		dstImage = resampleWithGraphicsDraw(srcImage,dstColorModel,selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight);
		if (dstImage == null) {
			slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,int,int,int,int,int,int): source ColorModel failed; try most favorable instead");
			dstColorModel = getMostFavorableColorModel();
			dstImage = resampleWithGraphicsDraw(srcImage,dstColorModel,selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight);
		}
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,int,int,int,int,int,int): done");
		slf4jlogger.debug("resampleWithGraphicsDraw() elapsed: "+(System.currentTimeMillis()-startTime)+" ms");
		return dstImage;
	}
	
	private final static BufferedImage resampleWithGraphicsDraw(BufferedImage srcImage,ColorModel dstColorModel,int selectionWidth,int selectionHeight,int selectionXOffset,int selectionYOffset,int dstWidth,int dstHeight) {
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,ColorModel,int,int,int,int,int,int): start");
		BufferedImage dstImage = null;
		if (dstColorModel != null) {
			WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(dstWidth,dstHeight);
			dstImage = new BufferedImage(dstColorModel,dstRaster,dstColorModel.isAlphaPremultiplied(),null);
			if (dstImage != null) {
				try {
					resampleWithGraphicsDraw(srcImage,dstImage,selectionWidth,selectionHeight,selectionXOffset,selectionYOffset,dstWidth,dstHeight);
				}
				//catch (java.awt.image.ImagingOpException e) {
				catch (Exception e) {
					//slf4jlogger.error("",e);
					dstImage = null;
				}
			}
		}
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,ColorModel,int,int,int,int,int,int): done");
		return dstImage;
	}
	
	private final static void resampleWithGraphicsDraw(BufferedImage srcImage,BufferedImage dstImage,int selectionWidth,int selectionHeight,int selectionXOffset,int selectionYOffset,int dstWidth,int dstHeight) {
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,BufferedImage,int,int,int,int,int,int): start");
		Graphics2D g2d = dstImage.createGraphics();
		//Object renderingHintValue = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		//Object renderingHintValue = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
		Object renderingHintValue = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
		slf4jlogger.debug("resampleWithGraphicsDraw(): renderingHintValue = {}",renderingHintValue);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,renderingHintValue);
		
		int sx1 = selectionXOffset;
		int sy1 = selectionYOffset;
		int sx2 = selectionXOffset+selectionWidth-1;
		int sy2 = selectionYOffset+selectionHeight-1;
		int dx1 = 0;
		int dy1 = 0;
		int dx2 = dstWidth-1;
		int dy2 = dstHeight-1;
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested sx1 = {}",sx1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested sy1 = {}",sy1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested sx2 = {}",sx2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested sy2 = {}",sy2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested dx1 = {}",dx1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested dy1 = {}",dy1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested dx2 = {}",dx2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): requested dy2 = {}",dy2);
		
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();
		
		if (sx1 < 0) {
			int dstDelta = (int)(-sx1 * ((double)dstWidth)/selectionWidth + 0.5);
			dx1+=dstDelta;
			sx1=0;
		}
		if (sx1 >= srcWidth) {
			int dstDelta = (int)((sx1-srcWidth+1) * ((double)dstWidth)/selectionWidth + 0.5);
			dx1-=dstDelta;
			sx1=srcWidth-1;
		}
		
		if (sx2 < 0) {
			int dstDelta = (int)(-sx2 * ((double)dstWidth)/selectionWidth + 0.5);
			dx2+=dstDelta;
			sx2=0;
		}
		if (sx2 >= srcWidth) {
			int dstDelta = (int)((sx2-srcWidth+1) * ((double)dstWidth)/selectionWidth + 0.5);
			dx2-=dstDelta;
			sx2=srcWidth-1;
		}
		
		if (sy1 < 0) {
			int dstDelta = (int)(-sy1 * ((double)dstHeight)/selectionHeight + 0.5);
			dy1+=dstDelta;
			sy1=0;
		}
		if (sy1 >= srcHeight) {
			int dstDelta = (int)((sy1-srcHeight+1) * ((double)dstHeight)/selectionHeight + 0.5);
			dy1-=dstDelta;
			sy1=srcHeight-1;
		}
		
		if (sy2 < 0) {
			int dstDelta = (int)(-sy2 * ((double)dstHeight)/selectionHeight + 0.5);
			dy2+=dstDelta;
			sy2=0;
		}
		if (sy2 >= srcHeight) {
			int dstDelta = (int)((sy2-srcHeight+1) * ((double)dstHeight)/selectionHeight + 0.5);
			dy2-=dstDelta;
			sy2=srcHeight-1;
		}
		
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped sx1 = {}",sx1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped sy1 = {}",sy1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped sx2 = {}",sx2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped sy2 = {}",sy2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped dx1 = {}",dx1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped dy1 = {}",dy1);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped dx2 = {}",dx2);
		slf4jlogger.debug("resampleWithGraphicsDraw(): clipped dy2 = {}",dy2);
		
		g2d.drawImage(srcImage,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,Color.black,null);
		slf4jlogger.debug("resampleWithGraphicsDraw(BufferedImage,BufferedImage,int,int,int,int,int,int): done");
	}
	
	public static final BufferedImage resampleWithAffineTransformOp(BufferedImage srcImage,double sx,double sy) {
		slf4jlogger.debug("resampleWithAffineTransformOp(): start");
		long startTime = System.currentTimeMillis();
		AffineTransform transform = AffineTransform.getScaleInstance(sx,sy);
		AffineTransformOp transformOp=new AffineTransformOp(transform,AffineTransformOp.TYPE_BILINEAR);
 		BufferedImage dstImage = transformOp.createCompatibleDestImage(srcImage,srcImage.getColorModel());	// otherwise returns, say RGBA even if gray
		dstImage = transformOp.filter(srcImage,dstImage);
		slf4jlogger.debug("resampleWithAffineTransformOp() elapsed: "+(System.currentTimeMillis()-startTime)+" ms");
		return dstImage;
	}

	public static final BufferedImage resampleWithAffineTransformOp(BufferedImage srcImage,int dstWidth,int dstHeight) {
		slf4jlogger.debug("resampleWithAffineTransformOp():");
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();
		double sx = ((double)dstWidth)/srcWidth;
		double sy = ((double)dstHeight)/srcHeight;
		return resampleWithAffineTransformOp(srcImage,sx,sy);
	}

	// Image flip stuff ...
	
	public static void flipHorizontally(BufferedImage srcImage) {
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		slf4jlogger.debug("flipHorizontally(): srcDataBuffer is {}",srcDataBuffer.getClass().getName());
		int srcDataBufferOffset = srcDataBuffer.getOffset();
		slf4jlogger.debug("flipHorizontally(): srcDataBuffer.getOffset() is {}",srcDataBufferOffset);
		int srcNumBands = srcRaster.getNumBands();

		if (srcNumBands == 1) {
			// DataBufferShort will not be encountered ... see comments in SourceImage.java
			if (srcDataBuffer instanceof DataBufferUShort) {
				short srcPixels[] = ((DataBufferUShort)srcDataBuffer).getData();
				short rowBuffer[] = new short[srcWidth];
				for (int srcY=0; srcY<srcHeight; ++srcY) {
					int srcPixelOffset = srcDataBufferOffset + srcY*srcWidth;
					for (int dstX=srcWidth-1; dstX>=0; --dstX) {
						rowBuffer[dstX] = srcPixels[srcPixelOffset++];
					}
					srcPixelOffset = srcDataBufferOffset + srcY*srcWidth;
					for (int dstX=0; dstX<srcWidth; ++dstX) {
						srcPixels[srcPixelOffset++] = rowBuffer[dstX];
					}
				}
			}
			else if (srcDataBuffer instanceof DataBufferByte) {
				byte srcPixels[] = ((DataBufferByte)srcDataBuffer).getData();
				byte rowBuffer[] = new byte[srcWidth];
				for (int srcY=0; srcY<srcHeight; ++srcY) {
					int srcPixelOffset = srcDataBufferOffset + srcY*srcWidth;
					for (int dstX=srcWidth-1; dstX>=0; --dstX) {
						rowBuffer[dstX] = srcPixels[srcPixelOffset++];
					}
					srcPixelOffset = srcDataBufferOffset + srcY*srcWidth;
					for (int dstX=0; dstX<srcWidth; ++dstX) {
						srcPixels[srcPixelOffset++] = rowBuffer[dstX];
					}
				}
			}
			else {
				slf4jlogger.info("BufferedImageUtilities.flipHorizontally(): cannot flip unsupported DataBuffer type of {}",srcDataBuffer.getClass().getName());
			}
		}
		else {
			slf4jlogger.info("BufferedImageUtilities.flipHorizontally(): cannot flip more than one band; number of bands is {}",srcNumBands);
		}
		// May or may not have been changed, but would have been changed in place
	}
	
	
	public static void flipVertically(BufferedImage srcImage) {
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		slf4jlogger.debug("flipVertically(): srcDataBuffer is {}",srcDataBuffer.getClass().getName());
		int srcDataBufferOffset = srcDataBuffer.getOffset();
		slf4jlogger.debug("flipVertically(): srcDataBuffer.getOffset() is {}",srcDataBufferOffset);
		int srcNumBands = srcRaster.getNumBands();

		if (srcNumBands == 1) {
			// DataBufferShort will not be encountered ... see comments in SourceImage.java
			if (srcDataBuffer instanceof DataBufferUShort) {
				short srcPixels[] = ((DataBufferUShort)srcDataBuffer).getData();
				short colBuffer[] = new short[srcHeight];
				for (int srcX=0; srcX<srcWidth; ++srcX) {
					int srcPixelOffset = srcDataBufferOffset + srcX;
					for (int srcY=0,dstY=srcHeight-1; dstY>=0; ++srcY,--dstY) {
						slf4jlogger.trace("flipVertically(): srcPixelOffset={} srcX={} srcY={} dstY={} srcPixelOffset+srcY*srcWidth={} value={}",srcPixelOffset,srcX,srcY,dstY,(srcPixelOffset+srcY*srcWidth),srcPixels[srcPixelOffset+srcY*srcWidth]);
						colBuffer[dstY] = srcPixels[srcPixelOffset+srcY*srcWidth];
					}
					srcPixelOffset = srcDataBufferOffset + srcX;
					for (int y=0; y<srcHeight; ++y) {
						slf4jlogger.trace("flipVertically(): srcPixelOffset={} srcX={} y={} srcPixelOffset+y*srcWidth={} value={}",srcPixelOffset,srcX,y,(srcPixelOffset+y*srcWidth),colBuffer[y]);
						srcPixels[srcPixelOffset+y*srcWidth] = colBuffer[y];
					}
				}
			}
			else if (srcDataBuffer instanceof DataBufferByte) {
				byte srcPixels[] = ((DataBufferByte)srcDataBuffer).getData();
				byte colBuffer[] = new byte[srcHeight];
				for (int srcX=0; srcX<srcWidth; ++srcX) {
					int srcPixelOffset = srcDataBufferOffset + srcX;
					for (int srcY=0,dstY=srcHeight-1; dstY>=0; ++srcY,--dstY) {
						colBuffer[dstY] = srcPixels[srcPixelOffset+srcY*srcWidth];
					}
					srcPixelOffset = srcDataBufferOffset + srcX;
					for (int y=0; y<srcHeight; ++y) {
						srcPixels[srcPixelOffset+y*srcWidth] = colBuffer[y];
					}
				}
			}
			else {
				slf4jlogger.info("BufferedImageUtilities.flipVertically(): cannot flip unsupported DataBuffer type of {}",srcDataBuffer.getClass().getName());
			}
		}
		else {
slf4jlogger.info("BufferedImageUtilities.flipVertically(): cannot flip more than one band; number of bands is{}",srcNumBands);
		}
		// May or may not have been changed, but would have been changed in place
	}
	
	public static BufferedImage rotateAndFlipSwappingRowsAndColumns(BufferedImage srcImage) {
		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();

		ColorModel srcColorModel = srcImage.getColorModel();
		SampleModel srcSampleModel = srcImage.getSampleModel();
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcDataBufferOffset = srcDataBuffer.getOffset();
		slf4jlogger.debug("resample(): srcDataBuffer is {}",srcDataBuffer.getClass().getName());
		slf4jlogger.debug("resample(): srcDataBuffer.getOffset() is {}",srcDataBufferOffset);
		int srcNumBands = srcRaster.getNumBands();

		if (srcNumBands != 1 || !(srcDataBuffer instanceof DataBufferUShort || srcDataBuffer instanceof DataBufferByte)) {
slf4jlogger.info("BufferedImageUtilities.rotateAndFlipSwappingRowsAndColumns(): cannot do our own rotating");
			return srcImage;		// Give up if we don't know how to get pixels
		}

		int dstWidth = srcHeight;
		int dstHeight = srcWidth;

		BufferedImage dstImage = null;

		if (srcDataBuffer instanceof DataBufferUShort) {
		slf4jlogger.debug("rotateAndFlipSwappingRowsAndColumns(): DataBufferUShort");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {16},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_USHORT
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_USHORT,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			short srcPixels[] = ((DataBufferUShort)srcDataBuffer).getData();
			int srcPixelsLength = srcPixels.length;

			int dstPixelsLength = dstWidth*dstHeight;
			short dstPixels[] = new short[dstPixelsLength];
				
			for (int srcYandDstX=0; srcYandDstX<srcHeight; ++srcYandDstX) {
				int srcRowOffset = srcDataBufferOffset + srcYandDstX*srcWidth;
				for (int srcXandDstY=0,dstPixelOffset=srcYandDstX; srcXandDstY<srcWidth; ++srcXandDstY,dstPixelOffset+=dstWidth) {
					dstPixels[dstPixelOffset] = srcPixels[srcRowOffset+srcXandDstY];
				}
			}
				
			DataBuffer dstDataBuffer = new DataBufferUShort(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
		}
		else if (srcDataBuffer instanceof DataBufferByte) {
		slf4jlogger.debug("rotateAndFlipSwappingRowsAndColumns(): DataBufferByte");
			ColorModel dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {8},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE
			);
			SampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_BYTE,
				dstWidth,
				dstHeight,
				1,
				dstWidth,
				new int[] {0}
			);
		
			byte srcPixels[] = ((DataBufferByte)srcDataBuffer).getData();
			int srcPixelsLength = srcPixels.length;

			int dstPixelsLength = dstWidth*dstHeight;
			byte dstPixels[] = new byte[dstPixelsLength];
			
			for (int srcYandDstX=0; srcYandDstX<srcHeight; ++srcYandDstX) {
				int srcRowOffset = srcDataBufferOffset + srcYandDstX*srcWidth;
				for (int srcXandDstY=0,dstPixelOffset=srcYandDstX; srcXandDstY<srcWidth; ++srcXandDstY,dstPixelOffset+=dstWidth) {
					dstPixels[dstPixelOffset] = srcPixels[srcRowOffset+srcXandDstY];
				}
			}
				
			DataBuffer dstDataBuffer = new DataBufferByte(dstPixels,dstWidth,0);
			WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstDataBuffer,new Point(0,0));
			dstImage = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
		}
		return dstImage;
	}

	// The affine transform doesn't work with our sample or color model or whatever, gives:
	//
	// java.awt.image.ImagingOpException: Unable to transform src image
	//
	// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4723021
	
	// Derived from http://javaalmanac.com/egs/java.awt.image/Flip.html
	
	private static BufferedImage flipBothVerticallyAndHorizontallyWithAffineTransformOp(BufferedImage srcImage) {
		// equivalent to rotating the image 180 degrees
		AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
		tx.translate(-srcImage.getWidth(null),-srcImage.getHeight(null));
		AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(srcImage, null);
	}

	/**
	 * @param	srcY		YBR Y value
	 * @param	srcCb		YBR Cb value
	 * @param	srcCr		YBR Cr value
	 * @param	dst			an array of length three in which to return the RGB values, in that order
	 * @return				the supplied destination array
	 */
	public static byte[] convertYBRToRGB(byte srcY,byte srcCb,byte srcCr,byte[] dst) {
		long y  = srcY  & 0xff;		// prevent sign extension of unsigned 8 bit value
		long cb = srcCb & 0xff;
		long cr = srcCr & 0xff;
							
		// this inverse of what is in DICOM PS 3.3 is defined in the JFIF documents (e.g., TR-098)
		long r  = Math.round(y                    + 1.402  *(cr-128));
		long g  = Math.round(y - 0.34414*(cb-128) - 0.71414*(cr-128));
		long b  = Math.round(y + 1.772  *(cb-128)                   );
				
		if (r < 0) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping -ve r = {} to 0",r);
			r=0;
		}
		if (r > 255) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping overrange r = {} to 255",r);
			r=255;
		}
		if (g < 0) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping -ve g = {} to 0",g);
			g=0;
		}
		if (g > 255) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping overrange g = {} to 255",g);
			g=255;
		}
		if (b < 0) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping -ve b = {} to 0",b);
			b=0;
		}
		if (b > 255) {
			//slf4jlogger.trace("BufferedImage.convertYBRToRGB(): clamping overrange b = {} to 255",b);
			b=255;
		}

//		if (y != 0) {
//			long y_rt  = Math.round( .2990*r + .5870*g +	.1140*b);
//			long cb_rt = Math.round(-.1687*r	- .3313*g +	.5000*b	+ 128);
//			long cr_rt = Math.round( .5000*r	- .4187*g -	.0813*b	+ 128);
//System.err.println("BufferedImage.convertYBRToRGB(): given: y = "+y   +"\tcb = "+cb   +"\tcr = "+cr);
//System.err.println("BufferedImage.convertYBRToRGB(): calc:  r = "+r   +"\tg  = "+g    +"\tb  = "+b);
//System.err.println("BufferedImage.convertYBRToRGB(): rtrip: y = "+y_rt+"\tcb = "+cb_rt+"\tcr = "+cr_rt);
//		}

		dst[0] = (byte)r;
		dst[1] = (byte)g;
		dst[2] = (byte)b;
	
		return dst;
	}
	
	/**
	 * @param	srcImage	a BufferedImage pretending to be an RGB ColorModel but really YBR
	 * @return				a BufferedImage with pixel values that really are RGB
	 */
	public static final BufferedImage convertYBRToRGB(BufferedImage srcImage) {
		slf4jlogger.debug("convertYBRToRGB(): start");
		long startTime = System.currentTimeMillis();

		ColorModel srcColorModel = srcImage.getColorModel();
		ColorModel dstColorModel = srcColorModel;
				
		int width = srcImage.getWidth();
		int height = srcImage.getHeight();
        
		SampleModel srcSampleModel = srcImage.getSampleModel();
		
		WritableRaster srcRaster = srcImage.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcNumBands = srcRaster.getNumBands();

		slf4jlogger.debug("convertYBRToRGB(): srcImage ={}",srcImage);
		slf4jlogger.debug("convertYBRToRGB(): srcSampleModel ={}",srcSampleModel);
		{
			if (srcSampleModel instanceof ComponentSampleModel) {	// (000990)
				slf4jlogger.debug("convertYBRToRGB(): srcSampleModel.getPixelStride() ={}",((ComponentSampleModel)srcSampleModel).getPixelStride());
			}
			else {
				slf4jlogger.info("convertYBRToRGB(): srcSampleModel is not ComponentSampleModel but is {}",srcSampleModel.getClass().getName());
			}
		}

		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(width,height);
		DataBuffer dstDataBuffer = dstRaster.getDataBuffer();
		BufferedImage dstImage = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);

		SampleModel dstSampleModel = dstImage.getSampleModel();
		int dstNumBands = dstRaster.getNumBands();
        
		slf4jlogger.debug("convertYBRToRGB(): dstImage ={}",dstImage);
		slf4jlogger.debug("convertYBRToRGB(): dstSampleModel ={}",dstSampleModel);

		int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		srcPixels = srcSampleModel.getPixels(0,0,width,height,srcPixels,srcDataBuffer);
		int srcPixelsLength = srcPixels.length;

		int dstPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		dstPixels = dstSampleModel.getPixels(0,0,width,height,dstPixels,dstDataBuffer);
		int dstPixelsLength = dstPixels.length;
        
		slf4jlogger.debug("convertYBRToRGB() after getPixels, elapsed: {} ms",(System.currentTimeMillis()-startTime));

		byte[] convertedRGB = new byte[3];
		if (srcNumBands == 4 && dstNumBands == 4 && srcPixelsLength == dstPixelsLength) {
		slf4jlogger.debug("convertYBRToRGB(): converting pixel or band interleaved 4 band YBRA to RGBA");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength;) {
				byte y  = (byte)(srcPixels[srcIndex++]);
				byte cb = (byte)(srcPixels[srcIndex++]);
				byte cr = (byte)(srcPixels[srcIndex++]);
				BufferedImageUtilities.convertYBRToRGB(y,cb,cr,convertedRGB);
				dstPixels[dstIndex++] = convertedRGB[0];
				dstPixels[dstIndex++] = convertedRGB[1];
				dstPixels[dstIndex++] = convertedRGB[2];
				
				dstPixels[dstIndex++]=srcPixels[srcIndex++];	// leave the alpha channel unchanged
			}
			dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 3 && dstNumBands == 3 && srcPixelsLength == dstPixelsLength) {
		slf4jlogger.debug("convertYBRToRGB(): converting pixel or band interleaved 3 band YBR to RGB");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength;) {
				byte y  = (byte)(srcPixels[srcIndex++]);
				byte cb = (byte)(srcPixels[srcIndex++]);
				byte cr = (byte)(srcPixels[srcIndex++]);
				BufferedImageUtilities.convertYBRToRGB(y,cb,cr,convertedRGB);
				dstPixels[dstIndex++] = convertedRGB[0];
				dstPixels[dstIndex++] = convertedRGB[1];
				dstPixels[dstIndex++] = convertedRGB[2];
			}
			dstSampleModel.setPixels(0,0,width,height,dstPixels,dstDataBuffer);
		}
		else {
slf4jlogger.info("BufferedImageUtilities.convertYBRToRGB(): No conversion supported");
			dstImage=null;
		}
		slf4jlogger.debug("convertYBRToRGB() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("convertYBRToRGB(): done = {}",dstImage);
		return dstImage;
	}
}



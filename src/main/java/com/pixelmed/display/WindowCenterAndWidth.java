/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

//import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Transparency;

import java.awt.color.ColorSpace;

import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.IndexColorModel;
import java.awt.image.LookupOp;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class of static methods to perform window operations on images.</p>
 *
 * @author	dclunie
 */
public class WindowCenterAndWidth {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/WindowCenterAndWidth.java,v 1.23 2017/02/28 16:12:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(WindowCenterAndWidth.class);

	/**
	 * @param	lut
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	mask
	 */
	protected static void applyPaddingValueRangeToLUT(byte[] lut,int pad,int padRangeLimit,int mask) {
		//lut[pad&mask]=(byte)0;
		int maskedPadStart = pad&mask;
		int maskedPadEnd = padRangeLimit&mask;
		int incr = maskedPadStart <= maskedPadEnd ? 1 : -1;
		for (int i=maskedPadStart; i != maskedPadEnd; i+=incr) {
			slf4jlogger.trace("applyPaddingValueRangeToLUT(): LUT index set to zero for pad in range {}",i);
			lut[i]=(byte)0;
		}
		lut[maskedPadEnd]=(byte)0;		// since not done in for loop due to bi-directional end condition
		slf4jlogger.trace("applyPaddingValueRangeToLUT(): LUT index set to zero for pad in range {}",maskedPadEnd);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 */
	public static final BufferedImage applyWindowCenterAndWidthLogistic(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad) {
		return applyWindowCenterAndWidthLogistic(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLogistic(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad,int padRangeLimit) {
		slf4jlogger.debug("applyWindowCenterAndWidthLogistic(): center={} width={}",center,width);

		int       ymin = 0;
		int       ymax = 255;
				
		int startx;
		int endx;
		byte lut[] = null;
		int mask;
		
		//slf4jlogger.debug("applyWindowCenterAndWidthLogistic(): bottom={} top={}",bottom,top);
		int dataType = src.getSampleModel().getDataType();
		slf4jlogger.debug("applyWindowCenterAndWidthLogistic(): Data type {}",dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
			slf4jlogger.debug("applyWindowCenterAndWidthLogistic(): Data type is short or ushort and signed is {}",signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
			slf4jlogger.debug("applyWindowCenterAndWidthLogistic(): Data type is byte and signed is {}",signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else {
			throw new IllegalArgumentException();
		}
		
		//double a = ymax;
		//double b = 1/width;
		//double c = center < 0 ? 0 : center;
		//for (int xi=startx; xi<endx; ++xi) {
		//	double x = xi*useSlope+useIntercept;
		//	double y = a / (1 + c * Math.exp(-b*x)) + 0.5;
		//	if (y < ymin)  y=ymin;
		//	else if (y > ymax) y=ymax;
		//	if (inverted) y=(byte)(ymax-y);
		//	lut[xi&mask]=(byte)y;
		//}
		
		int yrange = ymax - ymin;
		
		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			double y = yrange / (1 + Math.exp(-4*(x - center)/width)) + ymin + 0.5;
			if (y < ymin)  y=ymin;
			else if (y > ymax) y=ymax;
			if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
			lut[xi&mask]=(byte)y;
		}
		if (usePad) {
			applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
		}
		LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		BufferedImage dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
		return dst;
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad) {
		return applyWindowCenterAndWidthLinear(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad,int padRangeLimit) {
		return applyWindowCenterAndWidthLinear(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,padRangeLimit,false);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	useExactCalculationInsteadOfDICOMStandardMethod
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean usePad,int pad,int padRangeLimit,
			boolean useExactCalculationInsteadOfDICOMStandardMethod) {
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): center={} width={}",center,width);
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): useExactCalculationInsteadOfDICOMStandardMethod={}",useExactCalculationInsteadOfDICOMStandardMethod);

		int       ymin = 0;
		int       ymax = 255;

		byte     bymin = (byte)ymin;
		byte     bymax = (byte)ymax;
		double  yrange = ymax - ymin;
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): yrange={}",yrange);

		double    cmp5 = useExactCalculationInsteadOfDICOMStandardMethod ? center : (center - 0.5);
		double     wm1 = useExactCalculationInsteadOfDICOMStandardMethod ? width : (width - 1.0);

		double halfwm1 = wm1/2.0;
		double  bottom = cmp5 - halfwm1;
		double     top = cmp5 + halfwm1;
		
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): cmp5={}",cmp5);
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): wm1={}",wm1);
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): halfwm1={}",halfwm1);
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): bottom={}",bottom);
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): top={}",top);

		int startx = 0;
		int endx = 0;
		byte lut[] = null;
		int mask = 0;
		
		boolean doItWithLookupTable = true;
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): bottom={} top={}",bottom,top);
		int dataType = src.getSampleModel().getDataType();
		slf4jlogger.debug("applyWindowCenterAndWidthLinear(): Data type {}",dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
			slf4jlogger.debug("applyWindowCenterAndWidthLinear(): Data type is short or ushort and signed is {}",signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
			slf4jlogger.debug("applyWindowCenterAndWidthLinear(): Data type is byte and signed is {}",signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE) {
			slf4jlogger.debug("applyWindowCenterAndWidthLinear(): Data type is float or double");
			doItWithLookupTable = false;

		}
		else {
			throw new IllegalArgumentException();
		}
		
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		
		BufferedImage dst = null;
		if (doItWithLookupTable) {
			for (int xi=startx; xi<endx; ++xi) {
				double x = xi*useSlope+useIntercept;
				byte y;
				if (x <= bottom)  y=bymin;
				else if (x > top) y=bymax;
				else {
					y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x);
				}
			
				if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
				lut[xi&mask]=y;
			}
			if (usePad) {
				applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
			}
			LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
			dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
			slf4jlogger.debug("applyWindowCenterAndWidthLinear(): BufferedImage out of LookupOp{}",dst);
		}
		else {
			int w = src.getWidth();
			int h = src.getHeight();
			int nPixels = w*h;
			
			ComponentSampleModel dstSampleModel = new ComponentSampleModel(
				DataBuffer.TYPE_BYTE,
				w,
				h,
				1,
				w,
				new int[] {0}
			);
			DataBufferByte dstBuf = new DataBufferByte(nPixels);
            WritableRaster dstRaster = Raster.createWritableRaster(dstSampleModel,dstBuf,new Point(0,0));
			dst = new BufferedImage(dstColorModel,dstRaster,true,null);	// no properties hash table
			byte[] dstPixels = dstBuf.getData();
			
			SampleModel srcSampleModel = src.getSampleModel();
			WritableRaster srcRaster = src.getRaster();
			DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
			if (srcDataBuffer instanceof DataBufferDouble) {
				slf4jlogger.debug("applyWindowCenterAndWidthLinear(): per pixel with DataBufferDouble");
				double srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
				srcPixels = srcSampleModel.getPixels(0,0,w,h,srcPixels,srcDataBuffer);
		
				for (int i=0; i<nPixels; i++) {
					double spv = srcPixels[i];
					//double[] spvs  = srcSampleModel.getPixel(i%w,i/w,(double[])null,srcDataBuffer);
					//double spv = spvs[0];
					double x = spv*useSlope+useIntercept;
					byte y;
					if (x <= bottom)  y=bymin;
					else if (x > top) y=bymax;
					else {
						y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
					}
					if (inverted) y=(byte)(ymax-y);
//System.err.println("spv="+spv+" x="+x+" y="+(((int)y)&0x00ff));
					dstPixels[i] = y;
				}
			}
			else if (srcDataBuffer instanceof DataBufferFloat) {
				slf4jlogger.debug("applyWindowCenterAndWidthLinear(): per pixel with DataBufferFloat");
				float srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
				srcPixels = srcSampleModel.getPixels(0,0,w,h,srcPixels,srcDataBuffer);
		
				for (int i=0; i<nPixels; i++) {
					float spv = srcPixels[i];
					//float[] spvs  = srcSampleModel.getPixel(i%w,i/w,(float[])null,srcDataBuffer);
					//float spv = spvs[0];
					double x = spv*useSlope+useIntercept;
					byte y;
					if (x <= bottom)  y=bymin;
					else if (x > top) y=bymax;
					else {
						y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
					}
					if (inverted) y=(byte)(ymax-y);
//System.err.println("spv="+spv+" x="+x+" y="+(((int)y)&0x00ff));
					dstPixels[i] = y;
				}
			}
		}
		return dst;
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	largestGray
	 * @param	bitsPerEntry
	 * @param	numberOfEntries
	 * @param	redTable
	 * @param	greenTable
	 * @param	blueTable
	 */
	public static final BufferedImage applyWindowCenterAndWidthWithPaletteColor(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,
			int largestGray,int bitsPerEntry,int numberOfEntries,
			short[] redTable,short[] greenTable,short[] blueTable) {
		return applyWindowCenterAndWidthWithPaletteColor(src,center,width,signed,inverted,useSlope,useIntercept,usePad,pad,pad,
			largestGray,bitsPerEntry,numberOfEntries,redTable,greenTable,blueTable);
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	largestGray
	 * @param	bitsPerEntry
	 * @param	numberOfEntries
	 * @param	redTable
	 * @param	greenTable
	 * @param	blueTable
	 */
	public static final BufferedImage applyWindowCenterAndWidthWithPaletteColor(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,int padRangeLimit,
			int largestGray,int bitsPerEntry,int numberOfEntries,
			short[] redTable,short[] greenTable,short[] blueTable) {
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor center={} width={}",center,width);

		int       ymin = 0;
		int       ymax = 255;

		byte     bymin = (byte)ymin;
		byte     bymax = (byte)ymax;
		double  yrange = ymax - ymin;

		double    cmp5 = center - 0.5;
		double     wm1 = width - 1.0;
		double halfwm1 = wm1/2.0;
		double  bottom = cmp5 - halfwm1;
		double     top = cmp5 + halfwm1;
		
		//double gamma = 2.2;

		int startx = signed ? -32768 : 0;		// hmmm ....
		int endx   = signed ?  32768 : 65536;
		//int endx   = largestGray;
		byte rlut[]=new byte[65536];
		byte glut[]=new byte[65536];
		byte blut[]=new byte[65536];
		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			byte y;
			if (x <= bottom)  y=bymin;
			else if (x > top) y=bymax;
			else {
				y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x);
			}
			
			if (inverted) y=(byte)(ymax-y);

			rlut[xi&0xffff]=y;
			glut[xi&0xffff]=y;
			blut[xi&0xffff]=y;
		}
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor(): numberOfEntries = {}",numberOfEntries);
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor(): redTable.length = {}",redTable.length);
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor(): greenTable.length = {}",greenTable.length);
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor(): blueTable.length = {}",blueTable.length);
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor(): largestGray={}",largestGray);

		if (bitsPerEntry <= 8) {
			slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: LUTs contain 8 bits packed in 16");
			// the 8 bit entries are packed two per 16 bit short table entry
			// assume no shift and interpret the values literally :(
			int i;
			int xi;
			int n = (numberOfEntries - 1)/2 + 1;	// in case odd number, which sometimes happens
			slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: half the numberOfEntries rounded = {}",n);
			if (n > redTable.length) {
				// do not go beyond actually table length
				slf4jlogger.info("WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor: truncating half the numberOfEntries {} to the actual array size {}",n,redTable.length);
				n = redTable.length;
			}
			for (xi=largestGray+1,i=0; i<n;++i) {
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: xi="+xi+" i={}",i);
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: redTable[i]&0xff={}",Integer.toHexString((redTable[i])&0xff));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: redTable[i]>>8={}",Integer.toHexString((redTable[i]>>8)&0xff));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: greenTable[i]&0xff={}",Integer.toHexString((greenTable[i])&0xff));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: greenTable[i]>>8={}",Integer.toHexString((greenTable[i]>>8)&0xff));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: blueTable[i]&0xff={}",Integer.toHexString((blueTable[i])&0xff));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: blueTable[i]>>8={}",Integer.toHexString((blueTable[i]>>8)&0xff));
				rlut[xi&0xffff]=(byte)(redTable[i]);
				glut[xi&0xffff]=(byte)(greenTable[i]);
				blut[xi&0xffff]=(byte)(blueTable[i]);
				++xi;
				rlut[xi&0xffff]=(byte)(redTable[i]>>8);
				glut[xi&0xffff]=(byte)(greenTable[i]>>8);
				blut[xi&0xffff]=(byte)(blueTable[i]>>8);
				++xi;
			}
		}
		else {
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: LUTs contain 16 bit entries");
			int shiftRight = bitsPerEntry-8;
			int i;
			int xi;
			for (xi=largestGray+1,i=0; i < numberOfEntries; ++xi,++i) {
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: xi="+xi+" i={}",i);
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: redTable[i]={}",redTable[i]);
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: redTable[i]>>shiftRight={}",(redTable[i]>>shiftRight));
				slf4jlogger.trace("applyWindowCenterAndWidthWithPaletteColor: redTable[i]>>shiftRight={}",Integer.toHexString((redTable[i]>>shiftRight)&0xff));
				rlut[xi&0xffff]=(byte)(redTable[i]>>shiftRight);
				glut[xi&0xffff]=(byte)(greenTable[i]>>shiftRight);
				blut[xi&0xffff]=(byte)(blueTable[i]>>shiftRight);
			}
		}
		
		if (usePad) {
			applyPaddingValueRangeToLUT(rlut,pad,padRangeLimit,0xffff);
			applyPaddingValueRangeToLUT(glut,pad,padRangeLimit,0xffff);
			applyPaddingValueRangeToLUT(blut,pad,padRangeLimit,0xffff);
		}

		int columns = src.getWidth();
		int rows = src.getHeight();
        
		SampleModel srcSampleModel = src.getSampleModel();
		WritableRaster srcRaster = src.getRaster();
		DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
		int srcNumBands = srcRaster.getNumBands();

		//ColorModel dstColorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		ColorModel dstColorModel = BufferedImageUtilities.getMostFavorableColorModel();
		if (dstColorModel == null) {
			// This color model is what we use in SourceImage when reading RGB images
			dstColorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB),
				new int[] {8,8,8},
				false,		// has alpha
				false,		// alpha premultipled
				Transparency.OPAQUE,
				DataBuffer.TYPE_BYTE
			);
		}
		
		WritableRaster dstRaster = dstColorModel.createCompatibleWritableRaster(columns,rows);
		DataBuffer dstDataBuffer = dstRaster.getDataBuffer();
		BufferedImage dst = new BufferedImage(dstColorModel, dstRaster, dstColorModel.isAlphaPremultiplied(), null);
		SampleModel dstSampleModel = dst.getSampleModel();
		int dstNumBands = dstRaster.getNumBands();
        
		int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		srcPixels = srcSampleModel.getPixels(0,0,columns,rows,srcPixels,srcDataBuffer);
		int srcPixelsLength = srcPixels.length;

		int dstPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
		dstPixels = dstSampleModel.getPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		int dstPixelsLength = dstPixels.length;
        
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: dstNumBands = {}",dstNumBands);
		if (srcNumBands == 1 && dstNumBands == 4 && srcPixelsLength*4 == dstPixelsLength) {
			slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: converting gray to RGBA");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
				dstPixels[dstIndex++]=rlut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=glut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=blut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=-1;
			}
			dstSampleModel.setPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		}
		else if (srcNumBands == 1 && dstNumBands == 3 && srcPixelsLength*3 == dstPixelsLength) {
			slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor: converting gray to RGB");
			int dstIndex=0;
			for (int srcIndex=0; srcIndex<srcPixelsLength; ++srcIndex) {
				dstPixels[dstIndex++]=rlut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=glut[srcPixels[srcIndex]];
				dstPixels[dstIndex++]=blut[srcPixels[srcIndex]];
			}
			dstSampleModel.setPixels(0,0,columns,rows,dstPixels,dstDataBuffer);
		}

		return dst;
	}

	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinearToColorImage(BufferedImage src,double center,double width) {
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): center={} width={}",center,width);
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): BufferedImage src{}",src);

		int       ymin = 0;
		int       ymax = 255;

		byte     bymin = (byte)ymin;
		byte     bymax = (byte)ymax;
		double  yrange = ymax - ymin;
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): yrange={}",yrange);

		double    cmp5 = center - 0.5;
		double     wm1 = width - 1.0;

		double halfwm1 = wm1/2.0;
		double  bottom = cmp5 - halfwm1;
		double     top = cmp5 + halfwm1;
		
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): cmp5={}",cmp5);
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): wm1={}",wm1);
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): halfwm1={}",halfwm1);
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): bottom={}",bottom);
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): top={}",top);

		int startx = 0;
		int endx = 0;
		byte lut[] = null;
		int mask = 0;
		
		slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): bottom={} top={}",bottom,top);

		boolean doIt = true;
		int dataType = src.getSampleModel().getDataType();
		ColorModel colorModel = src.getColorModel();
		if (colorModel instanceof IndexColorModel) {
			// We get RGB IndexColorModel from single bit images out of SourceImage and BufferedImageUtilities.resample() without convertToMostFavorableImageType(), so ignore (000996)
			slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): Unsupported colorModel "+colorModel.getClass()+", so doing nothing");
			doIt = false;
		}
		else {
			slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): Data type {}",dataType);
			// assume always unsigned since signed color is not used in DICOM
			if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): Data type is short or ushort");
				startx = 0;
				endx   = 65536;
				lut=new byte[65536];
				mask=0xffff;
			}
			else if (dataType == DataBuffer.TYPE_BYTE) {
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): Data type is byte");
				startx = 0;
				endx   = 256;
				lut=new byte[256];
				mask=0xff;
			}
			else {
				// e.g. might be DataBuffer.TYPE_INT if supplied with IndexColorModel image after convertToMostFavorableImageType() (000996)
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): Unsupported data type is "+dataType+", so doing nothing");
				doIt = false;
			}
		}
		
		BufferedImage dst = src;
		if (doIt) {
			for (int xi=startx; xi<endx; ++xi) {
				double x = xi;	//  no rescale slope or intercept
				byte y;
				if (x <= bottom)  y=bymin;
				else if (x > top) y=bymax;
				else {
					y = (byte)(((x-cmp5)/wm1 + 0.5)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x);
				}
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
				lut[xi&mask]=y;
			}
			LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
			if (dataType == DataBuffer.TYPE_BYTE) {
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): 8 bit so using lookup.createCompatibleDestImage (src)");
				dst = lookup.createCompatibleDestImage(src,null/*ColorModel*/);		// this won't work if src is not 8 bit image
			}
			else {
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): 16 bit so manually creating an 8 bit dst image");
				int w = src.getWidth();
				int h = src.getHeight();
				// seems to work regardless of whether src is band or pixel interleaved and destination is different ...
				//dst = SourceImage.createBandInterleavedByteThreeComponentColorImage(w,h,new byte[w*h*3],0/*offset*/,src.getColorModel().getColorSpace());
				dst = SourceImage.createPixelInterleavedByteThreeComponentColorImage(w,h,new byte[w*h*3],0/*offset*/,src.getColorModel().getColorSpace(),false/*isChrominanceHorizontallyDownsampledBy2*/);
				slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): BufferedImage dst for LookupOp {}",dst);
			}
			dst = lookup.filter(src,dst);	// applies single LUT to all channels and uses color model of src
			slf4jlogger.debug("applyWindowCenterAndWidthLinearToColorImage(): BufferedImage out of LookupOp {}",dst);
		}
		return dst;
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	identityCenter
	 * @param	identityWidth
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	usePad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	numberOfEntries
	 * @param	bitsPerEntry
	 * @param	grayTable
	 * @param	entryMin
	 * @param	entryMax
	 * @param	topOfEntryRange
	 */
	public static final BufferedImage applyVOILUT(BufferedImage src,double center,double width,double identityCenter,double identityWidth,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean usePad,int pad,int padRangeLimit,
			int numberOfEntries,int firstValueMapped,int bitsPerEntry,short[] grayTable,int entryMin,int entryMax,int topOfEntryRange) {

		slf4jlogger.debug("applyVOILUT(): firstValueMapped={}",firstValueMapped);
		slf4jlogger.debug("applyVOILUT center={} width={}",center,width);

		int       ymin = 0;
		int       ymax = 255;
		double  yrange = ymax - ymin;

		int bottomOfEntryRange = 0;
		double entryRange = topOfEntryRange - bottomOfEntryRange;
		slf4jlogger.debug("applyVOILUT(): bottomOfEntryRange={} topOfEntryRange={} entryRange={}",bottomOfEntryRange,topOfEntryRange,entryRange);

		int firstLUTValue = grayTable[0] & 0xffff;
		int lastLUTValue  = grayTable[numberOfEntries-1] & 0xffff;
		slf4jlogger.debug("applyVOILUT(): firstLUTValue={} lastLUTValue={}",firstLUTValue,lastLUTValue);

		byte     bymin = (byte)(((firstLUTValue-bottomOfEntryRange)/entryRange)*yrange+ymin);
		byte     bymax = (byte)(((lastLUTValue -bottomOfEntryRange)/entryRange)*yrange+ymin);
		slf4jlogger.debug("applyVOILUT(): bymin={} bymax={}",(bymin&0xff),(bymax&0xff));

		int startx;
		int endx;
		byte lut[] = null;
		int mask;
		
		int dataType = src.getSampleModel().getDataType();
		slf4jlogger.debug("applyVOILUT(): Data type {}",dataType);
		if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
			slf4jlogger.debug("applyVOILUT(): Data type is short or ushort and signed is {}",signed);
			startx = signed ? -32768 : 0;
			endx   = signed ?  32768 : 65536;
			lut=new byte[65536];
			mask=0xffff;
		}
		else if (dataType == DataBuffer.TYPE_BYTE) {
			slf4jlogger.debug("applyVOILUT(): Data type is byte and signed is {}",signed);
			startx = signed ? -128 : 0;
			endx   = signed ?  128 : 256;
			lut=new byte[256];
			mask=0xff;
		}
		else {
			throw new IllegalArgumentException();
		}

		for (int xi=startx; xi<endx; ++xi) {
			double x = xi*useSlope+useIntercept;
			byte y;
			//int lutIndex = (int)(x-firstValueMapped);		// warning: there could be sign issues here related to the nominal "VR" of the second LUT Descriptor entry - see PS 3.3 C.11.2.1.1 :(
			//int lutIndex = (int)(((x-center)/width) * numberOfEntries + numberOfEntries/2);
			int lutIndex = (int)(((x-center)/width) * identityWidth + identityCenter - firstValueMapped);
//System.err.println("xi&0xffff="+(xi&0xffff)+" x="+x+" lutIndex="+lutIndex);
			if (lutIndex < 0) {
				y=bymin;
			}
			else if (lutIndex > (numberOfEntries-1)) {
				y=bymax;
			}
			else {
					y = (byte)(((grayTable[lutIndex] & 0xffff)/entryRange)*yrange+ymin);
//System.err.println("xi&0xffff="+(xi&0xffff)+" y="+((int)y&0xff)+" x="+x+" lutIndex="+lutIndex);
			}
			
			if (inverted) y=(byte)(ymax-y);
//if (xi%16 == 0) System.err.println(xi+"\t"+(((int)y)&0xff));
			lut[xi&mask]=y;
		}

		if (usePad) {
			applyPaddingValueRangeToLUT(lut,pad,padRangeLimit,mask);
		}
		LookupOp lookup=new LookupOp(new ByteLookupTable(0,lut), null);
		ColorModel dstColorModel=new ComponentColorModel(
			ColorSpace.getInstance(ColorSpace.CS_GRAY),
			new int[] {8},
			false,		// has alpha
			false,		// alpha premultipled
			Transparency.OPAQUE,
			DataBuffer.TYPE_BYTE
		);
		BufferedImage dst = lookup.filter(src,lookup.createCompatibleDestImage(src,dstColorModel));	// Fails if src is DataBufferShort
		slf4jlogger.debug("applyVOILUT(): BufferedImage out of LookupOp{}",dst);
		return dst;
	}
						
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

public class MultiFramePixelData {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/MultiFramePixelData.java,v 1.6 2017/01/24 10:50:37 dclunie Exp $";
	
		private byte[]  bytePixelsForAllFrames;
		private short[] wordPixelsForAllFrames;
		private int currentFrameOffset;
		private int numberOfPixelValuesPerFrame;
		private int numberOfPixelValuesTotal;

		private byte[]  getBytePixelsForAllFrames() { return bytePixelsForAllFrames; }
		private short[] getWordPixelsForAllFrames() { return wordPixelsForAllFrames; }
		
		public boolean hasBytePixels() { return bytePixelsForAllFrames != null; }
		public boolean hasWordPixels() { return wordPixelsForAllFrames != null; }
		
		public Attribute getPixelDataAttribute() throws DicomException {
			Attribute aPixelData = null;
			if (hasBytePixels()) {
				OtherByteAttribute aBytePixelData = new OtherByteAttribute(TagFromName.PixelData);
				aBytePixelData.setValues(getBytePixelsForAllFrames());
				aPixelData = aBytePixelData;
			}
			else if (hasWordPixels()) {
				OtherWordAttribute aWordPixelData  = new OtherWordAttribute(TagFromName.PixelData);
				aWordPixelData.setValues(getWordPixelsForAllFrames());
				aPixelData = aWordPixelData;
			}
			return aPixelData;
		}

		public MultiFramePixelData(int rows,int columns,int samplesPerPixel,int numberOfFrames) {
			numberOfPixelValuesPerFrame = samplesPerPixel*rows*columns;					// consider use of long not int, esp. if use file rt. array (latter requires int in constructor) :(
			numberOfPixelValuesTotal = numberOfPixelValuesPerFrame * numberOfFrames;	// consider use of long not int, esp. if use file rt. array (latter requires int in constructor) :(
			currentFrameOffset = 0;
			bytePixelsForAllFrames = null;	// will instantiate only one of these lazily depending on encountered VR
			wordPixelsForAllFrames = null;
		}
	
		public void addFrame(Attribute pixelData) throws DicomException {
			if (pixelData != null) {
				byte[] vr = pixelData.getVR();
				if (ValueRepresentation.isOtherByteVR(vr)) {
					if (wordPixelsForAllFrames != null) {
						throw new DicomException("Cannot mix OB and OW Pixel Data VR from different frames"+pixelData.getVRAsString());
					}
					if (bytePixelsForAllFrames == null) {
						bytePixelsForAllFrames =  new byte[numberOfPixelValuesTotal];
					}
					byte[] pixels = pixelData.getByteValues();
//System.err.println("MultiFramePixelData.addFrame(): byte[] pixels.length = "+pixels.length);
//System.err.println("MultiFramePixelData.addFrame(): numberOfPixelValuesPerFrame = "+numberOfPixelValuesPerFrame);
//System.err.println("MultiFramePixelData.addFrame(): byte[] bytePixelsForAllFrames.length = "+bytePixelsForAllFrames.length);
//System.err.println("MultiFramePixelData.addFrame(): numberOfPixelValuesTotal = "+numberOfPixelValuesTotal);
//System.err.println("MultiFramePixelData.addFrame(): currentFrameOffset = "+currentFrameOffset);
					System.arraycopy(pixels,0,bytePixelsForAllFrames,currentFrameOffset,pixels.length);
					currentFrameOffset += numberOfPixelValuesPerFrame;
				}
				else if (ValueRepresentation.isOtherWordVR(vr)) {
					if (bytePixelsForAllFrames != null) {
						throw new DicomException("Cannot mix OB and OW Pixel Data VR from different frames"+pixelData.getVRAsString());
					}
					if (wordPixelsForAllFrames == null) {
						wordPixelsForAllFrames =  new short[numberOfPixelValuesTotal];
					}
					short[] pixels = pixelData.getShortValues();
//System.err.println("MultiFramePixelData.addFrame(): short[] pixels.length = "+pixels.length);
//System.err.println("MultiFramePixelData.addFrame(): numberOfPixelValuesPerFrame = "+numberOfPixelValuesPerFrame);
//System.err.println("MultiFramePixelData.addFrame(): short[] wordPixelsForAllFrames.length = "+wordPixelsForAllFrames.length);
//System.err.println("MultiFramePixelData.addFrame(): numberOfPixelValuesTotal = "+numberOfPixelValuesTotal);
//System.err.println("MultiFramePixelData.addFrame(): currentFrameOffset = "+currentFrameOffset);
					System.arraycopy(pixels,0,wordPixelsForAllFrames,currentFrameOffset,pixels.length);
					currentFrameOffset += numberOfPixelValuesPerFrame;
				}
				else {
					throw new DicomException("Incorrect Pixel Data VR "+pixelData.getVRAsString());
				}
			}
			else {
				throw new DicomException("Missing Pixel Data");
			}
		}
}
	

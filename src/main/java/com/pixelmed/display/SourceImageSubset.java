/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.DisplayShutter;
import com.pixelmed.dicom.ModalityTransform;
import com.pixelmed.dicom.Overlay;
import com.pixelmed.dicom.RealWorldValueTransform;
import com.pixelmed.dicom.SUVTransform;
import com.pixelmed.dicom.VOITransform;

import java.awt.Dimension;

import java.awt.image.BufferedImage;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>A class that encapsulates a subset of frame sin a multi-frame SourceImage.</p>
 *
 * @author	dclunie
 */
public class SourceImageSubset extends SourceImage {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageSubset.java,v 1.4 2017/01/24 10:50:41 dclunie Exp $";

	protected SourceImage parent;
	protected int[] parentFrameNumbers;
	
	public SourceImageSubset(SourceImage parent,int[] parentFrameNumbers) {
		this.parent = parent;
		this.parentFrameNumbers = parentFrameNumbers;
	}
	
	public SourceImageSubset(SourceImage parent,SortedSet<Integer> frames) {
		this.parent = parent;
		
		parentFrameNumbers = new int[frames.size()];
		int childFrameNumber = 0;
		for (Integer parentFrameNumber : frames) {
//System.err.println("SourceImageSubset(): parentFrameNumber["+childFrameNumber+"] = "+parentFrameNumber);
			parentFrameNumbers[childFrameNumber++] = parentFrameNumber.intValue();
		}
	}
	
	// should override ALL superclass methods ... really should change SourceImage to an do an interface and an implementation class SourceImageImplementation :(

	public void close() throws Throwable {
		parent.close();		// hmm
	}

	public BufferedImage getBufferedImage(int i) {
		return parent.getBufferedImage(parentFrameNumbers[i]);
	}

	public int getNumberOfBufferedImages() { return parentFrameNumbers.length; }
	
	public int getWidth() { return parent.getWidth(); }
        
	public int getHeight() { return parent.getHeight(); }
        
	public Dimension getDimension() { return parent.getDimension(); }
    
	public double getMinimum() { return parent.getMinimum(); }

	public double getMaximum() { return parent.getMaximum(); }

	//public double getMean() { return getMean(); }

	//public double getStandardDeviation() { return getStandardDeviation(); }

	public int getMaskValue() { return parent.getMaskValue(); }

	public boolean isSigned() { return parent.isSigned(); }

	public boolean isInverted() { return parent.isInverted(); }

	public boolean isPadded() { return parent.isPadded(); }

	public int getPadValue() { return parent.getPadValue(); }

	public int getPadRangeLimit() { return parent.getPadRangeLimit(); }

	public int getBackgroundValue() { return parent.getBackgroundValue(); }

	public boolean isGrayscale() { return parent.isGrayscale(); }

	public boolean isYBR() { return parent.isYBR(); }

	public String getTitle() { return parent.getTitle(); }

	public int getNumberOfFrames() { return parent.getNumberOfFrames(); }

	public int getPaletteColorLargestGray() { return parent.getPaletteColorLargestGray(); }
	
	public int getPaletteColorFirstValueMapped() { return parent.getPaletteColorFirstValueMapped(); }
	
	public int getPaletteColorNumberOfEntries() { return parent.getPaletteColorNumberOfEntries(); }
	
	public int getPaletteColorBitsPerEntry() { return parent.getPaletteColorBitsPerEntry(); }
	
	public short[] getPaletteColorRedTable() { return parent.getPaletteColorRedTable(); }
	
	public short[] getPaletteColorGreenTable() { return parent.getPaletteColorGreenTable(); }
	
	public short[] getPaletteColorBlueTable() { return parent.getPaletteColorBlueTable(); }
	
	public SUVTransform getSUVTransform() { return parent.getSUVTransform(); }
	
	public RealWorldValueTransform getRealWorldValueTransform() { return parent.getRealWorldValueTransform(); }
	
	public ModalityTransform getModalityTransform() { return parent.getModalityTransform(); }
	
	public VOITransform getVOITransform() { return parent.getVOITransform(); }
	
	public DisplayShutter getDisplayShutter() { return parent.getDisplayShutter(); }
	
	public Overlay getOverlay() { return parent.getOverlay(); }

}



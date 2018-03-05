/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Rectangle;

/**
 * <p>A class to keep track of a selected sub-region of an image for the purposes of display.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 *
 * @author	dclunie
 */
public class DisplayedAreaSelection implements Cloneable {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DisplayedAreaSelection.java,v 1.16 2017/01/24 10:50:40 dclunie Exp $";

		protected int imageWidth;
		protected int imageHeight;
		protected int tlhcX;
		protected int tlhcY;
		protected int brhcX;
		protected int brhcY;
		protected int selectionWidth;
		protected int selectionHeight;
		
		protected boolean fitToWindow;
		protected boolean useExplicitPixelMagnificationRatio;
		protected boolean deducePixelMagnificationRatioFromSpacing;

		protected double requestedDisplaySpacing;
		protected double rowSpacing;
		protected double columnSpacing;
		protected double pixelMagnificationRatio;

		protected int horizontalGravity;
		protected int verticalGravity;
		protected boolean crop;
		
		public void translate(int deltaX,int deltaY) {
			tlhcX += deltaX;
			tlhcY += deltaY;
			brhcX += deltaX;
			brhcY += deltaY;
		}
		
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		/**
		 * Select entire image
		 *
		 * Scaled to fit window, centered, without cropping and with no pixel spacing information
		 *
		 * @param	imageWidth
		 * @param	imageHeight
		 */
		DisplayedAreaSelection(int imageWidth,int imageHeight) {
			this(imageWidth,imageHeight,0,0,imageWidth-1,imageHeight-1);
		}

		/**
		 * Select sub-region of image
		 *
		 * Scaled to fit window, centered, without cropping and with no pixel spacing information
		 *
		 * @param	imageWidth
		 * @param	imageHeight
		 * @param	tlhcX				origin from TLHC (0,0), that is, Y increasing downward, and numbered from 0, not 1 as corresponding DICOM attribute is
		 * @param	tlhcY
		 * @param	brhcX
		 * @param	brhcY
		 */
		DisplayedAreaSelection(int imageWidth,int imageHeight,int tlhcX,int tlhcY,int brhcX,int brhcY) {
			this(imageWidth,imageHeight,tlhcX,tlhcY,brhcX,brhcY,true,0.0,0.0,0.0,0,0,false);
		}
		
		/**
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("DisplayedAreaSelection:\n");
			buffer.append("\timageWidth = "+imageWidth+"\n");
			buffer.append("\timageHeight = "+imageHeight+"\n");
			buffer.append("\ttlhcX = "+tlhcX+"\n");
			buffer.append("\ttlhcY = "+tlhcY+"\n");
			buffer.append("\tbrhcX = "+brhcX+"\n");
			buffer.append("\tbrhcY = "+brhcY+"\n");
			buffer.append("\tselectionWidth = "+selectionWidth+"\n");
			buffer.append("\tselectionHeight = "+selectionHeight+"\n");
			buffer.append("\tfitToWindow = "+fitToWindow+"\n");
			buffer.append("\tuseExplicitPixelMagnificationRatio = "+useExplicitPixelMagnificationRatio+"\n");
			buffer.append("\tdeducePixelMagnificationRatioFromSpacing = "+deducePixelMagnificationRatioFromSpacing+"\n");
			buffer.append("\trequestedDisplaySpacing = "+requestedDisplaySpacing+"\n");
			buffer.append("\trowSpacing = "+rowSpacing+"\n");
			buffer.append("\tcolumnSpacing = "+columnSpacing+"\n");
			buffer.append("\tpixelMagnificationRatio = "+pixelMagnificationRatio+"\n");
			buffer.append("\thorizontalGravity = "+horizontalGravity+"\n");
			buffer.append("\tverticalGravity = "+verticalGravity+"\n");
			buffer.append("\tcrop = "+crop+"\n");
			return buffer.toString();
		}
		
		/**
		 * Select sub-region of image
		 *
		 * @param	imageWidth
		 * @param	imageHeight
		 * @param	tlhcX					origin from TLHC (0,0), that is, Y increasing downward, and numbered from 0, not 1 as corresponding DICOM attribute is
		 * @param	tlhcY
		 * @param	brhcX
		 * @param	brhcY
		 * @param	fitToWindow				true if fit to window, false if deduce magnification from specified requestedDisplaySpacing
		 * @param	requestedDisplaySpacing	the requested spacing in mm of the original image to be displayed in a single pixel, if fitToWindow false
		 * @param	rowSpacing				the spacing in mm between rows of the original image, if known
		 * @param	columnSpacing			the spacing in mm between columns of the original image, if known
		 * @param	horizontalGravity		whether to justify left (-1), center (0), or right (+1) when the viewport is wider than the selected width
		 * @param	verticalGravity			whether to justify up (-1), center (0), or down (+1) when the viewport is taller than the selected height
		 * @param	crop					true if should crop to the selected area rather than use more of the image, when viewport shape does not match selected area shape
		 */
		DisplayedAreaSelection(int imageWidth,int imageHeight,int tlhcX,int tlhcY,int brhcX,int brhcY,
			boolean fitToWindow,double requestedDisplaySpacing,double rowSpacing,double columnSpacing,
			int horizontalGravity,int verticalGravity,boolean crop) {
//System.err.println("DisplayedAreaSelection: request specified region - imageWidth = "+imageWidth);
//System.err.println("DisplayedAreaSelection: request specified region - imageHeight = "+imageHeight);
//System.err.println("DisplayedAreaSelection: request specified region - tlhcX = "+tlhcX);
//System.err.println("DisplayedAreaSelection: request specified region - tlhcY = "+tlhcY);
//System.err.println("DisplayedAreaSelection: request specified region - brhcX = "+brhcX);
//System.err.println("DisplayedAreaSelection: request specified region - brhcY = "+brhcY);
//System.err.println("DisplayedAreaSelection: request specified region - fitToWindow = "+fitToWindow);
//System.err.println("DisplayedAreaSelection: request specified region - requestedDisplaySpacing = "+requestedDisplaySpacing);
//System.err.println("DisplayedAreaSelection: request specified region - rowSpacing = "+rowSpacing);
//System.err.println("DisplayedAreaSelection: request specified region - columnSpacing = "+columnSpacing);
//System.err.println("DisplayedAreaSelection: request specified region - horizontalGravity = "+horizontalGravity);
//System.err.println("DisplayedAreaSelection: request specified region - verticalGravity = "+verticalGravity);
//System.err.println("DisplayedAreaSelection: request specified region - crop = "+crop);
			this.imageWidth=imageWidth;
			this.imageHeight=imageHeight;
			this.tlhcX=tlhcX;
			this.tlhcY=tlhcY;
			this.brhcX=brhcX;
			this.brhcY=brhcY;
			this.fitToWindow=fitToWindow;
			this.deducePixelMagnificationRatioFromSpacing=!fitToWindow;
			this.useExplicitPixelMagnificationRatio=false;
			this.pixelMagnificationRatio=0;
			this.requestedDisplaySpacing=requestedDisplaySpacing;
			this.rowSpacing=rowSpacing;
			this.columnSpacing=columnSpacing;
			this.horizontalGravity=horizontalGravity;
			this.verticalGravity=verticalGravity;
			this.crop=crop;
			
			if (this.tlhcX > this.brhcX) {
				int tmp = this.tlhcX;
				this.tlhcX =  this.brhcX;
				 this.brhcX = tmp;
			}
			if (this.tlhcY > this.brhcY) {
				int tmp = this.tlhcY;
				this.tlhcY =  this.brhcY;
				this.brhcY = tmp;
			}
			
			selectionWidth = brhcX - tlhcX + 1;
//System.err.println("DisplayedAreaSelection: selectionWidth "+selectionWidth);
			selectionHeight = brhcY - tlhcY + 1;
//System.err.println("DisplayedAreaSelection: selectionHeight "+selectionHeight);
		}

	int getImageWidth() { return imageWidth; }
	int getImageHeight() { return imageHeight; }
	int getSelectionWidth() { return selectionWidth; }
	int getSelectionHeight() { return selectionHeight; }
	int getXOffset() { return tlhcX; }
	int getYOffset() { return tlhcY; }
	
	boolean getFitToWindow() { return fitToWindow; }
	boolean getUseExplicitPixelMagnificationRatio() { return useExplicitPixelMagnificationRatio; }
	boolean getDeducePixelMagnificationRatioFromSpacing() { return deducePixelMagnificationRatioFromSpacing; }
	
	double getRequestedDisplaySpacing() { return requestedDisplaySpacing; }
	double getRowSpacing() { return rowSpacing; }
	double getColumnSpacing() { return columnSpacing; }
	double getPixelMagnificationRatio() { return pixelMagnificationRatio; }
	
	int getHorizontalGravity() { return horizontalGravity; }
	int getVerticalGravity() { return verticalGravity; }
	boolean getCrop() { return crop; }
	
	/**
	 * Change the selection to use the specified magnification ratio.
	 *
	 * Turns off fitToWindow if previously selected, since we have requested explicit magnification
	 *
	 * @param	pixelMagnificationRatio		the magnification factor of display pixels relative to the image pixels (i.e., 1.0 means 1 display pixel per 1 image pixel)
	 */
	public void setPixelMagnificationRatio(double pixelMagnificationRatio) {
		this.pixelMagnificationRatio = pixelMagnificationRatio;
		useExplicitPixelMagnificationRatio=true;
		deducePixelMagnificationRatioFromSpacing=false;
		fitToWindow=false;
	}

	/**
	 * Change the selection to fit the specified window.
	 *
	 * Will use the gravity information as necessary to place the image with respect to the window but returns gravity centering horizontally and vertically.
	 *
	 * @param	windowSize	the size and shape of the available display area
	 * return				a new DisplayedAreaSelection instance that fits the window
	 */
	DisplayedAreaSelection shapeSelectionToMatchAvailableWindow(Rectangle windowSize) {
		int newSelectionHeight = selectionHeight;
		int newSelectionWidth  = selectionWidth;
		int newXOffset = tlhcX;
		int newYOffset = tlhcY;

		if (fitToWindow  && selectionHeight > 0 && selectionWidth > 0) {
			double vScaleFactor = ((double)(selectionHeight)) / windowSize.height;
			double hScaleFactor = ((double)(selectionWidth))  / windowSize.width;
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): fitToWindow hScaleFactor = "+hScaleFactor);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): fitToWindow vScaleFactor = "+vScaleFactor);
			double useScaleFactor = (hScaleFactor > vScaleFactor ? hScaleFactor : vScaleFactor);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): fitToWindow useScaleFactor = "+useScaleFactor);
			newSelectionHeight = (int)Math.round(useScaleFactor * windowSize.height);
			newSelectionWidth  = (int)Math.round(useScaleFactor * windowSize.width);
		}
		else if (deducePixelMagnificationRatioFromSpacing && requestedDisplaySpacing > 0 && rowSpacing > 0 && columnSpacing > 0) {
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): requestedDisplaySpacing requestedDisplaySpacing = "+requestedDisplaySpacing);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): requestedDisplaySpacing rowSpacing = "+rowSpacing);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): requestedDisplaySpacing columnSpacing = "+columnSpacing);
			newSelectionHeight = (int)Math.round(requestedDisplaySpacing * windowSize.height / rowSpacing   );
			newSelectionWidth  = (int)Math.round(requestedDisplaySpacing * windowSize.width  / columnSpacing);
		}
		else if (useExplicitPixelMagnificationRatio && pixelMagnificationRatio > 0) {
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): pixelMagnificationRatio = "+pixelMagnificationRatio);
			newSelectionHeight = (int)Math.round(windowSize.height / pixelMagnificationRatio);
			newSelectionWidth  = (int)Math.round(windowSize.width  / pixelMagnificationRatio);
		}
		// else do not change
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): old selectionWidth = "+selectionWidth);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): newSelectionWidth = "+newSelectionWidth);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): old selectionHeight = "+selectionHeight);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): newSelectionHeight = "+newSelectionHeight);
		{
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): horizontalGravity = "+horizontalGravity);
			if (horizontalGravity == 0) {
				int centerOfPreviousSelection = tlhcX + selectionWidth/2;
				newXOffset = centerOfPreviousSelection - newSelectionWidth/2;
			}
			else if (horizontalGravity >  0) {
				//newXOffset = imageWidth - 1 - newSelectionWidth;
				newXOffset = brhcX - newSelectionWidth;
			}
			//else {
			//	newXOffset = tlhcX;
			//}
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): verticalGravity = "+verticalGravity);
			if (verticalGravity == 0) {
				int centerOfPreviousSelection = tlhcY + selectionHeight/2;
				newYOffset = centerOfPreviousSelection - newSelectionHeight/2;
			}
			else if (verticalGravity >  0) {
				//newYOffset = imageHeight - 1 - newSelectionHeight;
				newYOffset = brhcY - newSelectionHeight;
			}
			//else {
			//	newYOffset = tlhcY;
			//}
		}
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): old XOffset = "+tlhcX);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): new XOffset = "+newXOffset);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): old YOffset = "+tlhcY);
//System.err.println("DisplayedAreaSelection.fitSelectionToAvailableWindow(): new YOffset = "+newYOffset);
		return new DisplayedAreaSelection(
			imageWidth,imageHeight,
			newXOffset,newYOffset,
			newXOffset + newSelectionWidth - 1,
			newYOffset + newSelectionHeight - 1,
			fitToWindow,requestedDisplaySpacing,rowSpacing,columnSpacing,
			0 /*horizontalGravity*/,
			0 /*int verticalGravity*/,
			crop);
	}
}

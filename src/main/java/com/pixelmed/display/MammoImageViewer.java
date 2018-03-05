/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.*; 
import com.pixelmed.dicom.*;
import com.pixelmed.event.EventContext;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*; 
import java.awt.color.*; 
import java.awt.geom.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is an entire application for displaying and viewing mammography images and
 * CAD objects.</p>
 * 
 * <p>It detects the screen size and scales the images to fit the available screen real estate,
 * using up to four columns of images and multiple rows as necessary.</p>
 * 
 * <p>Images are scaled to the same physical size based on the detected breast area.</p>
 * 
 * <p>Images are flipped into the correct orientation for the view.</p>
 * 
 * <p>It is invoked using a main method with a list of DICOM image and CAD file names.</p>
 * 
 * @author	dclunie
 */
public class MammoImageViewer {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/MammoImageViewer.java,v 1.82 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MammoImageViewer.class);

	protected JFrame frame;
	protected JPanel multiPanel;
	protected int frameWidth;
	protected int frameHeight;
	
	protected boolean doNotFlipOrRotate = false;;
	
	/**
	 * <p>Suppress the normal flipping or rotation of images into the preferred orientation based on view and laterality.</p>
	 * 
	 * @param	doNotFlipOrRotate	if true, supresses
	 */
	public void setDoNotFlipOrRotate(boolean doNotFlipOrRotate) {
		this.doNotFlipOrRotate = doNotFlipOrRotate;
	}

	protected boolean forceFitEntireMatrixToWindow = false;
	
	/**
	 * <p>Suppress the examination of breast extent and same sizing of images with different pixel spacing.</p>
	 * 
	 * @param	forceFitEntireMatrixToWindow	if true, supresses
	 */
	public void setForceFitEntireMatrixToWindow(boolean forceFitEntireMatrixToWindow) {
		this.forceFitEntireMatrixToWindow = forceFitEntireMatrixToWindow;
	}

	protected boolean doNotJustify = false;
	
	/**
	 * <p>Suppress the justification to the chest wall and axilla.</p>
	 * 
	 * @param	doNotJustify	if true, supresses
	 */
	public void setDoNotJustify(boolean doNotJustify) {
		this.doNotJustify = doNotJustify;
	}

	
	/**
	 * @param	list
	 * @return			array of two String values, row then column orientation, else array of two nulls if cannot be obtained
	 */
	private static String[] getPatientOrientation(AttributeList list) {
		String[] vPatientOrientation = null;
		Attribute aPatientOrientation = list.get(TagFromName.PatientOrientation);
		if (aPatientOrientation != null && aPatientOrientation.getVM() == 2) {
			try {
				vPatientOrientation = aPatientOrientation.getStringValues();
				if (vPatientOrientation != null && vPatientOrientation.length != 2) {
					vPatientOrientation=null;
				}
			}
			catch (DicomException e) {
				vPatientOrientation=null;
			}
		}
		if (vPatientOrientation == null) {
			vPatientOrientation = new String[2];
		}
		return vPatientOrientation;
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getView(AttributeList list) {
		String view = null;
		CodedSequenceItem csiViewCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ViewCodeSequence);
		if (csiViewCodeSequence != null) {
			//view = decipherSNOMEDCodeSequence(csiViewCodeSequence,standardViewCodes);
			view = MammoDemographicAndTechniqueAnnotations.getViewAbbreviationFromViewCodeSequenceAttributes(csiViewCodeSequence.getAttributeList());
		}
//System.err.println("getView(): view="+view);
		return view;
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getImageLateralityViewModifierAndViewModifier(AttributeList list) {
		return MammoDemographicAndTechniqueAnnotations.getAbbreviationFromImageLateralityViewModifierAndViewModifierCodeSequenceAttributes(list);
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getLaterality(AttributeList list) {
		return Attribute.getSingleStringValueOrNull(list,TagFromName.ImageLaterality);
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getDate(AttributeList list) {
		return Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
	}
	
	/**
	 * @param	dates
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getDateOfCurrentStudy(String dates[]) {
		int n = dates.length;
		int latestDateValue = 0;
		String latestDateString = null;
		for (int i=0; i<n; ++i) {
			if (dates[i] != null) {
				int testDateValue = 0;
				try {
					testDateValue = Integer.parseInt(dates[i]);
				}
				catch (NumberFormatException e) {
					slf4jlogger.error("",e);
				}
				if (testDateValue > latestDateValue) {
					latestDateValue = testDateValue;
					latestDateString = dates[i];
				}
			}
		}
		return latestDateString;
	}
	
	/**
	 * @param	n				number of images ... 8 for prior and current 4 view, 2 for left/right or prior/current same view comparison
	 * @param	views
	 * @param	lateralities
	 * @param	dates
	 * @return					an array of indexes in the correct sequence
	 */
	private static int[] determineOrderForDisplay(int n,String views[],String lateralities[],String lateralityViewAndModifiers[],String dates[]) {
		// assert n == 8 || n == 2;
		// views.length == n;
		// lateralities.length == n;
		// dates.length == n;
		
		int order[] = null;

		if (n == 2) {
			boolean sameStudy = (dates[0] != null && dates[1] != null && dates[0].equals(dates[1])) /*|| (dates[0] == null && dates[1] == null)*/;
			boolean sameView  = (views[0] != null && views[1] != null && views[0].equals(views[1]));
			boolean sameSide  = (lateralities[0] != null && lateralities[1] != null && lateralities[0].equals(lateralities[1]));
			if (sameView) {
				if (sameStudy) {
					if (!sameSide) {
						if (lateralities[0] != null) {
							order = new int[n];
							slf4jlogger.info("Hanging two of same view and same study with right side on left of display");
							// hang right side on left of display
							if (lateralities[0].equals("R")) {
								order[0] = 0;
								order[1] = 1;	// assume lateralities[1] is present and "L"
							}
							else {				// assume lateralities[0] is "L"
								order[0] = 1;
								order[1] = 0;	// assume lateralities[1] is present and "R"
							}
						}
					}
					// else same view and side and study, give up trying to hang any particular way
				}
				else {	// different study
					if (sameSide) {
						if (dates[0] != null && dates[1] != null) {
							order = new int[n];
							slf4jlogger.info("Hanging two of same view and same side different study with earlier study on left of display");
							// hang earlier study on left of display
							if (Integer.parseInt(dates[0]) < Integer.parseInt(dates[1])) {
								order[0] = 0;
								order[1] = 1;
							}
							else {
								order[0] = 1;
								order[1] = 0;
							}
						}
					}
					// else same view but different side and different study, give up trying to hang any particular way
				}
			}
			// else different view, give up trying to hang any particular way
		}
		if (order == null && n == 4) {
			slf4jlogger.info("Hanging four views");
			order = new int[n];
			for (int i=0; i<n; ++i) {
				order[i]=-1;
			}
			for (int i=0; i<n; ++i) {
				// assume all with same date
				// left laterality displayed on right side
				int lateralityOffset = (lateralities[i] != null && lateralities[i].equals("L")) ? 1 : 0;
				// CC displayed on right side
				int viewOffset = (views[i] != null && views[i].equals("CC")) ? 2 : 0;
				int offset = lateralityOffset + viewOffset;
				if (order[offset] == -1 ) {
//System.err.println("spot "+offset+" used by image "+i);
					order[offset] = i;
				}
				else {
					slf4jlogger.info("spot {} wanted by {} but already used by {}",offset,i,order[offset]);
					order = null;
					break;
				}
			}
		}
		if (order == null && n == 8) {
			slf4jlogger.info("Hanging eight views - looking for prior and current four views");
			order = new int[n];
			for (int i=0; i<n; ++i) {
				order[i]=-1;
			}
			String currentStudyDate = getDateOfCurrentStudy(dates);
//System.err.println("currentStudyDate "+currentStudyDate);
			for (int i=0; i<n; ++i) {
				// assume all with latest date and the same date are current
				// current goes on bottom row (4,5,6,7)
				// assume prior, regardless of what date actually is or whether all the same or whether even present
				// prior goes on top row (0,1,2,3)
				int dateOffset =  (currentStudyDate != null && dates[i] != null && currentStudyDate.equals(dates[i])) ? 4 : 0;
//System.err.println("dates[i] "+dates[i]+" used by image "+i);
//System.err.println("dateOffset "+dateOffset+" used by image "+i);
				// left laterality displayed on right side
				int lateralityOffset = (lateralities[i] != null && lateralities[i].equals("L")) ? 1 : 0;
//System.err.println("lateralityOffset "+lateralityOffset+" used by image "+i);
				// CC displayed on right side
				int viewOffset = (views[i] != null && views[i].equals("CC")) ? 2 : 0;
//System.err.println("views[i] "+views[i]+" used by image "+i);
//System.err.println("viewOffset "+viewOffset+" used by image "+i);
				int offset = dateOffset + lateralityOffset + viewOffset;
				if (order[offset] == -1 ) {
//System.err.println("spot "+offset+" used by image "+i);
					order[offset] = i;
				}
				else {
					slf4jlogger.info("spot {} wanted by {} but already used by {}",offset,i,order[offset]);
					order = null;
					break;
				}
			}
		}
		if (order == null && n == 8) {
			slf4jlogger.info("Hanging eight views - looking for views with and without modifiers");
			order = new int[n];
			for (int i=0; i<n; ++i) {
				order[i]=-1;
			}
			for (int i=0; i<n; ++i) {
				// no modifier goes on bottom row (4,5,6,7)
				// with modifier goes on top row (0,1,2,3)
				int viewModifierOffset = (lateralityViewAndModifiers[i] != null && lateralities[i] != null && views[i] != null
					&& lateralityViewAndModifiers[i].equals(lateralities[i]+views[i])) ? 0 : 4;
//System.err.println("lateralityViewAndModifiers[i] "+lateralityViewAndModifiers[i]+" used by image "+i);
//System.err.println("viewModifierOffset "+viewModifierOffset+" used by image "+i);
				// left laterality displayed on right side
				int lateralityOffset = (lateralities[i] != null && lateralities[i].equals("L")) ? 1 : 0;
//System.err.println("lateralityOffset "+lateralityOffset+" used by image "+i);
				// CC displayed on right side
				int viewOffset = (views[i] != null && views[i].equals("CC")) ? 2 : 0;
//System.err.println("views[i] "+views[i]+" used by image "+i);
//System.err.println("viewOffset "+viewOffset+" used by image "+i);
				int offset = lateralityOffset + viewOffset + viewModifierOffset;
				if (order[offset] == -1 ) {
//System.err.println("spot "+offset+" used by image "+i);
					order[offset] = i;
				}
				else {
					slf4jlogger.info("spot {} wanted by {} but already used by {}",offset,i,order[offset]);
					order = null;
					break;
				}
			}
		}
		
		return order;
	}
	
	private static char invertOrientation(char orientation) {
		switch (orientation) {
			case 'A':	return 'P';
			case 'P':	return 'A';
			case 'R':	return 'L';
			case 'L':	return 'R';
			case 'H':	return 'F';
			case 'F':	return 'H';
		}
		return orientation;
	}
	
	private static String invertOrientation(String orientation) {
		StringBuffer b = new StringBuffer();
		if (orientation != null && orientation.length() > 0) {
			char [] array = orientation.toCharArray();
			for (int i=0; i<array.length; ++i) {
				b.append(invertOrientation(array[i]));
			}
		}
		return b.toString();
	}
	
	private class FlipAsNecessary {
		private BufferedImage img;
		private String rowOrientation;
		private String columnOrientation;
		private AffineTransform transform;
		
		FlipAsNecessary(BufferedImage img,String orientation[],String view,String laterality,boolean suppressFlipAndReturnIdentity) {
			this.img = img;
			rowOrientation = orientation != null ? orientation[0] : null;
			columnOrientation = orientation != null ? orientation[1] : null;
			transform = new AffineTransform();	// identity transformation
			
			if (suppressFlipAndReturnIdentity) {
				return;
			}
			
			// identity translation matrix and the AffineTransform(m00 m10 m01 m11 m02 m12) constructor arguments are ...
			//
			// 1  0  Tx		m00 m01 m02
			// 0  1  Ty		m10 m11 m12
			// 0  0  1		na  na  na

			if (rowOrientation != null && rowOrientation.indexOf("P") == -1 && rowOrientation.indexOf("A") == -1) {
				// if row is not AP, then we should rotate and flip rows and columns, and deal with the consequences next
				String tmp = rowOrientation;
				rowOrientation = columnOrientation;
				columnOrientation = tmp;
				slf4jlogger.info("FlipAsNecessary(): swapping rows and columns");
				this.img = BufferedImageUtilities.rotateAndFlipSwappingRowsAndColumns(img);
				//
				// to just flip x and y use ...
				//
				// 0  1  0		m00 m01 m02
				// 1  0  0		m10 m11 m12
				// 0  0  1		na  na  na
				//
				transform.concatenate(new AffineTransform(0,1,1,0,0,0));
			}
		
			if (rowOrientation != null && laterality != null) {
				if (laterality.equals("R") && rowOrientation.indexOf("P") == -1
				 || laterality.equals("L") && rowOrientation.indexOf("A") == -1
				) {
					slf4jlogger.info("FlipAsNecessary(): flipping horizontally");
					BufferedImageUtilities.flipHorizontally(img);
					rowOrientation = invertOrientation(rowOrientation);
					//
					// to flip horizontally use ...
					//
					// -1 0  w-1	m00 m01 m02
					// 0  1  0		m10 m11 m12
					// 0  0  1		na  na  na
					//
					transform.concatenate(new AffineTransform(-1,0,0,1,img.getWidth()-1,0));
				}
			}

			if (columnOrientation != null && laterality != null && view != null) {
				boolean flipVertically=false;
				if (view.equals("MLO") || view.equals("ML") || view.equals("LM") || view.equals("LMO") || view.equals("SIO")) {
					// for lateral and lateral obliques, feet are always at the bottom (axilla at the top), regardless of side
					if (columnOrientation.indexOf("F") == -1) {
						slf4jlogger.info("FlipAsNecessary(): flipping vertically for lateral or oblique to put feet at bottom");
						flipVertically=true;
					}
				}
				else {
					// for all other views, the laterality value must be opposite to the column orientation (axilla at top)
					if (columnOrientation.indexOf(laterality) != -1) {
						slf4jlogger.info("FlipAsNecessary(): flipping vertically to put laterality in axilla");
						flipVertically=true;
					}
				}
				if (flipVertically) {
					BufferedImageUtilities.flipVertically(img);
					columnOrientation = invertOrientation(columnOrientation);
					//
					// to flip vertically use ...
					//
					// 1  0  0		m00 m01 m02
					// 0  -1 h-1	m10 m11 m12
					// 0  0  1		na  na  na
					//
					slf4jlogger.info("FlipAsNecessary(): WARNING: flipping vertically but supressing coordinate transform because does not work - check CAD locations, which may be wrong");
					//transform.concatenate(new AffineTransform(1,0,0,-1,0,img.getHeight()-1));		// :(
				}
			}
		}
		
		public BufferedImage getImage() { return img;}
		public String getRowOrientation() { return rowOrientation; }
		public String getColumnOrientation() { return columnOrientation; }
		public AffineTransform getTransform() { return transform; }
	}


	private class BreastExtentFinder {
		double samplingFactorFractionOfDimension = 0.05;					// don't want this too small, else gets faked out by CR white edges
		double indentFromChestWallFactorFractionOfDimension = 0.1;
		double thresholdFractionOfPixelRangeAboveBlackToBeBreast = 0.01;
		//double thresholdFractionOfPixelRangeAboveBlackToBeBreast = 0.25;
	
		int upperBound;
		int lowerBound;
		int leftBound;
		int rightBound;
		double horizontalExtentInMm;
		double verticalExtentInMm;;
		
		public int getUpperBound() { return upperBound; }
		public int getLowerBound() { return lowerBound; }
		public int getLeftBound()  { return leftBound; }
		public int getRightBound() { return rightBound; }
		public double getHorizontalExtentInMm() { return horizontalExtentInMm; }
		public double getVerticalExtentInMm() { return verticalExtentInMm; }
		
		public int getVerticalCenter() { return (lowerBound - upperBound)/2; }
		
		BreastExtentFinder(BufferedImage img,double spacing) {
			int imageWidth = img.getWidth();
			int imageHeight = img.getHeight();
			upperBound = 0;
			lowerBound = imageHeight-1;
			leftBound = 0;
			rightBound = imageWidth-1;
			horizontalExtentInMm = (rightBound - leftBound) * spacing;
			verticalExtentInMm = (lowerBound - upperBound) * spacing;
		}

		BreastExtentFinder(BufferedImage img,String rowOrientation,double minimumPixel,double maximumPixel,boolean isInverted,double spacing) {
//System.err.println("BreastExtentFinder: rowOrientation = "+rowOrientation);
//System.err.println("BreastExtentFinder: minimumPixel = "+minimumPixel);
//System.err.println("BreastExtentFinder: maximumPixel = "+maximumPixel);
//System.err.println("BreastExtentFinder: isInverted = "+isInverted);
			// assume that image has already been orientated correctly such that A-P is horizontal
			boolean chestWallIsRightSide = rowOrientation != null && rowOrientation.indexOf("P") != -1;
//System.err.println("BreastExtentFinder: chestWallIsRightSide = "+chestWallIsRightSide);
			int imageWidth = img.getWidth();
//System.err.println("BreastExtentFinder: imageWidth = "+imageWidth);
			int imageHeight = img.getHeight();
//System.err.println("BreastExtentFinder: imageHeight = "+imageHeight);
			int threshold = (int)((maximumPixel-minimumPixel) * thresholdFractionOfPixelRangeAboveBlackToBeBreast + minimumPixel);
//System.err.println("BreastExtentFinder: threshold = "+threshold);
			if (isInverted) {
				threshold = (int)maximumPixel - threshold;
//System.err.println("BreastExtentFinder: inverted threshold = "+threshold);
			}
			int verticalSamplingInterval = (int)(imageHeight * samplingFactorFractionOfDimension);
//System.err.println("BreastExtentFinder: verticalSamplingInterval = "+verticalSamplingInterval);
			int verticalSamplingStart = verticalSamplingInterval / 2;		// don't want this too small or zero, else gets faked out by CR white edges
//System.err.println("BreastExtentFinder: verticalSamplingStart = "+verticalSamplingStart);
			int offsetFromChestWall = (int)(imageWidth * indentFromChestWallFactorFractionOfDimension);
//System.err.println("BreastExtentFinder: offsetFromChestWall = "+offsetFromChestWall);
			int columnToSample = chestWallIsRightSide ? imageWidth-offsetFromChestWall-1 : offsetFromChestWall;
//System.err.println("BreastExtentFinder: columnToSample = "+columnToSample);
			upperBound = 0;
			int testY = verticalSamplingStart;
			while (testY < imageHeight) {
				int pixelValues[] = img.getRaster().getPixel(columnToSample,testY,(int[])null);
				int pixelValue = pixelValues[0];
//System.err.println("BreastExtentFinder: upperBound ("+columnToSample+","+testY+") pixel = "+pixelValue);
				if (( isInverted && pixelValue < threshold)
				 || (!isInverted && pixelValue > threshold)) {
					break;
				}
				upperBound=testY;
				testY+=verticalSamplingInterval;
			}
//System.err.println("BreastExtentFinder: upperBound = "+upperBound);
			lowerBound = imageHeight - 1;
			testY = imageHeight - verticalSamplingStart - 1;
			while (testY > 0) {
				int pixelValues[] = img.getRaster().getPixel(columnToSample,testY,(int[])null);
				int pixelValue = pixelValues[0];
//System.err.println("BreastExtentFinder: lowerBound ("+columnToSample+","+testY+") pixel = "+pixelValue);
				if (( isInverted && pixelValue < threshold)
				 || (!isInverted && pixelValue > threshold)) {
					break;
				}
				lowerBound=testY;
				testY-=verticalSamplingInterval;
			}
//System.err.println("BreastExtentFinder: lowerBound = "+lowerBound);

			if (upperBound > lowerBound) {
				// then we crossed over, i.e., failed to find a boundary, so give up
				upperBound = 0;
				lowerBound = imageHeight - 1;
			}

			// Now find lateral extent between upper and lower bounds ...
			
			int horizontalSamplingInterval = (int)(imageWidth * samplingFactorFractionOfDimension);
//System.err.println("BreastExtentFinder: horizontalSamplingInterval = "+horizontalSamplingInterval);

			leftBound = 0;
			rightBound = imageWidth-1;
			int startYForLateralCheck = upperBound > verticalSamplingStart ? upperBound : verticalSamplingStart;	// don't get caught up in white CR edges
			int endYForLateralCheck = lowerBound < imageHeight-verticalSamplingStart-1 ? lowerBound : imageHeight-verticalSamplingStart-1;
			if (startYForLateralCheck < endYForLateralCheck) {
				int lastActualX = chestWallIsRightSide ? imageWidth-offsetFromChestWall-1 : offsetFromChestWall;
				int testX = offsetFromChestWall;
				while (testX < imageWidth) {
					boolean columnContainsSupraThresholdPixels = false;
					int actualX = chestWallIsRightSide ? imageWidth-testX-1 : testX;
					for (testY=startYForLateralCheck; testY <= endYForLateralCheck && !columnContainsSupraThresholdPixels; testY+=verticalSamplingInterval) {
						int pixelValues[] = img.getRaster().getPixel(actualX,testY,(int[])null);
						int pixelValue = pixelValues[0];
//System.err.println("BreastExtentFinder: lateral bound ("+actualX+","+testY+") pixel = "+pixelValue);
						if (( isInverted && pixelValue < threshold)
						 || (!isInverted && pixelValue > threshold)) {
							columnContainsSupraThresholdPixels = true;;
						}
					}
					if (!columnContainsSupraThresholdPixels) {
//System.err.println("BreastExtentFinder: found empty column");
						// found "empty" column
						if (chestWallIsRightSide) {
							leftBound = lastActualX;
						}
						else {
							rightBound = lastActualX;
						}
						break;
					}
					lastActualX=actualX;
					testX+=horizontalSamplingInterval;
				}
			}
//System.err.println("BreastExtentFinder: leftBound = "+leftBound);
//System.err.println("BreastExtentFinder: rightBound = "+rightBound);

			// use a fudge factor to compensate for sampling interval. amongst other things :)
			
			if (upperBound > verticalSamplingInterval) {
				upperBound-=verticalSamplingInterval;
			}
			if (lowerBound < (imageHeight - verticalSamplingInterval - 1)) {
				lowerBound+=verticalSamplingInterval;
			}
			if (chestWallIsRightSide) {
				if (leftBound > horizontalSamplingInterval) {
					leftBound-=horizontalSamplingInterval;
				}
			}
			else {
				if (rightBound < (imageHeight - verticalSamplingInterval - 1)) {
					rightBound+=verticalSamplingInterval;
				}
			}

			horizontalExtentInMm = (rightBound - leftBound) * spacing;
			verticalExtentInMm = (lowerBound - upperBound) * spacing;
//System.err.println("BreastExtentFinder: horizontalExtentInMm = "+horizontalExtentInMm);
//System.err.println("BreastExtentFinder: verticalExtentInMm = "+verticalExtentInMm);
		}
	}
	
	/**
	 * @param	list
	 * @return			a single value, or null if cannot be obtained (absent or empty)
	 */
	private static String getSingleSourceImageSequenceReferencedSOPInstanceUIDOrNull(AttributeList list) {
		String uid = null;
		Attribute aSpatialLocationsPreserved = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.SourceImageSequence,TagFromName.SpatialLocationsPreserved);
//System.err.println("getSingleSourceImageSequenceReferencedSOPInstanceUIDOrNull(): aSpatialLocationsPreserved = "+aSpatialLocationsPreserved);
		if (aSpatialLocationsPreserved == null													// in absence (legacy images) assume YES
		 || aSpatialLocationsPreserved.getSingleStringValueOrEmptyString().equals("YES")) {		// if present, must be YES
			Attribute aReferencedSOPInstanceUID = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.SourceImageSequence,TagFromName.ReferencedSOPInstanceUID);
//System.err.println("getSingleSourceImageSequenceReferencedSOPInstanceUIDOrNull(): aReferencedSOPInstanceUID = "+aReferencedSOPInstanceUID);
			if (aReferencedSOPInstanceUID != null) {
				uid = aReferencedSOPInstanceUID.getSingleStringValueOrNull();
				if (uid != null && uid.length() == 0) {
					uid = null;
				}
			}
		}
		return uid;
	}
	
	private static final int crossSize = 5;	// actually just one arm of the cross
	private static final int crossGap  = 0;	// is included in crossSize
	
	/**
	 * @param	coord
	 * @param	preDefinedShapes
	 */
	private static void addCADMarkToPreDefinedShapes(SpatialCoordinateAndImageReference coord,Vector preDefinedShapes) {
//System.err.println("addCADMarkToPreDefinedShapes(): given:"+coord);
		String graphicType=coord.getGraphicType();
		float[] graphicData=coord.getGraphicData();
		if (graphicType !=null && graphicData != null
		 && (coord.getRenderingIntent() == SpatialCoordinateAndImageReference.RenderingRequired
		  || coord.getRenderingIntent() == SpatialCoordinateAndImageReference.RenderingOptional)
		) {
			if (graphicType.equals("POINT") && graphicData.length == 2) {
//System.err.println("addCADMarkToPreDefinedShapes(): adding POINT");
				int x = (int)graphicData[0];
				int y = (int)graphicData[1];
				if (coord.getCoordinateCategory() == SpatialCoordinateAndImageReference.CoordinateCategoryMammoBreastDensity) {
//System.err.println("addCADMarkToPreDefinedShapes(): adding vertical cross for density");
					DrawingUtilities.addVerticalCross(preDefinedShapes,x,y,crossSize,crossGap);
				}
				else {
//System.err.println("addCADMarkToPreDefinedShapes(): adding diagonal cross for calcifications");
					DrawingUtilities.addDiagonalCross(preDefinedShapes,x,y,crossSize,crossGap);
				}
			}
			else if (graphicType.equals("POLYLINE") && graphicData.length >= 4) {
//System.err.println("addCADMarkToPreDefinedShapes(): adding POLYLINE");
				int x1 = (int)graphicData[0];
				int y1 = (int)graphicData[1];
				for (int p=2; p+1 < graphicData.length; p+=2) {
					int x2 = (int)graphicData[p];
					int y2 = (int)graphicData[p+1];
					preDefinedShapes.add(new Line2D.Float(new Point(x1,y1),new Point(x2,y2)));
					x1=x2;
					y1=y2;
				}
			}
			else if (graphicType.equals("ELLIPSE") && graphicData.length == 8) {
//System.err.println("addCADMarkToPreDefinedShapes(): adding ELLIPSE");
				int xmajorstart = (int)graphicData[0];
				int ymajorstart = (int)graphicData[1];
				int xmajorend = (int)graphicData[2];
				int ymajorend = (int)graphicData[3];
				int xminorstart = (int)graphicData[4];
				int yminorstart = (int)graphicData[5];
				int xminorend = (int)graphicData[6];
				int yminorend = (int)graphicData[7];
				if (ymajorstart == ymajorend && xminorstart == xminorend) {
					// we have an ellipse with major axis along x axis without rotation
					preDefinedShapes.add(new Ellipse2D.Float(xmajorstart,yminorstart,xmajorend-xmajorstart,yminorend-yminorstart));
				}
				else if (xmajorstart == xmajorend && yminorstart == yminorend) {
					// we have an ellipse with major axis along y axis without rotation
					preDefinedShapes.add(new Ellipse2D.Float(xminorstart,ymajorstart,xminorend-xminorstart,ymajorend-ymajorstart));
				}
				else {
					slf4jlogger.info("addCADMarkToPreDefinedShapes(): NOT adding ELLIPSE that is not parallel to row");	// because the constructor expects a rectangular area :(
				}
			}
			else {
				slf4jlogger.info("addCADMarkToPreDefinedShapes(): NOT adding {}",graphicType);
			}
		}
	}

	class OurSingleImagePanel extends /*SingleImagePanelWithRegionDrawing*/ SingleImagePanelWithLineDrawing  {
		public OurSingleImagePanel(SourceImage sImg,EventContext typeOfPanelEventContext) {
			super(sImg,typeOfPanelEventContext);
		}
		protected Shape makeNewDrawingShape(int tlhcX,int tlhcY,int width,int height) {
			return new Ellipse2D.Double(tlhcX,tlhcY,width,height);
		}
	}
	
	protected SingleImagePanel makeNewImagePanel(SourceImage sImg,EventContext typeOfPanelEventContext) {
		return new OurSingleImagePanel(sImg,typeOfPanelEventContext);
	}
	
	/**
	 * @param		filenames
	 * @throws	Exception		if internal error
	 */
	public void loadMultiPanelFromSpecifiedFiles(String filenames[]) throws Exception {
	
		int nFiles = filenames.length;
		
		SingleImagePanel imagePanels[] = new SingleImagePanel[nFiles];
		
		String orientations[][] = new String[nFiles][];
		String views[] = new String[nFiles];
		String lateralityViewAndModifiers[] = new String[nFiles];
		String lateralities[] = new String[nFiles];
		String dates[] = new String[nFiles];
		PixelSpacing spacing[] = new PixelSpacing[nFiles];
		int widths[] = new int[nFiles];
		int heights[] = new int[nFiles];
		double horizontalExtentsInMm[] = new double[nFiles];
		double verticalExtentsInMm[] = new double[nFiles];
		//int verticalCenter[] = new int[nFiles];
		int upperBounds[] = new int[nFiles];
		int lowerBounds[] = new int[nFiles];
		int leftBounds[] = new int[nFiles];
		int rightBounds[] = new int[nFiles];
		
		String rowOrientations[] = new String[nFiles];
		String columnOrientations[] = new String[nFiles];
		AffineTransform coordinateTransform[] = new AffineTransform[nFiles];
		
		int justifyRightOrLeft[] = new int[nFiles];
		int justifyTopOrBottom[] = new int[nFiles];
		
		String imageSOPClassUID[] = new String[nFiles];
		String imageSOPInstanceUID[] = new String[nFiles];
		String imageSourceSOPInstanceUID[] = new String[nFiles];

		HashMap eventContexts = new HashMap();
		
		double maximumHorizontalExtentInMm = 0;
		double maximumVerticalExtentInMm = 0;
		
		StructuredReport sr[] = new StructuredReport[nFiles];

		int nImages = 0;
		int nCAD = 0;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (int f=0; f<nFiles; ++f) {
			try {
				String filename = filenames[f];
				DicomInputStream distream = null;
				InputStream in = classLoader.getResourceAsStream(filename);
				if (in != null) {
					distream = new DicomInputStream(in);
				}
				else {
					distream = new DicomInputStream(new File(filename));
				}
				AttributeList list = new AttributeList();
				list.read(distream);
				if (list.isImage()) {
					int i = nImages++;
					slf4jlogger.info("IMAGE [{}] is file {} ({})",i,f,filenames[f]);

					imageSOPClassUID[i] = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
//System.err.println("IMAGE ["+i+"] imageSOPClassUID="+imageSOPClassUID[i]);
					imageSOPInstanceUID[i] = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
//System.err.println("IMAGE ["+i+"] imageSOPInstanceUID="+imageSOPInstanceUID[i]);
					imageSourceSOPInstanceUID[i] = getSingleSourceImageSequenceReferencedSOPInstanceUIDOrNull(list);
//System.err.println("IMAGE ["+i+"] imageSourceSOPInstanceUID="+imageSourceSOPInstanceUID[i]);

					orientations[i] = getPatientOrientation(list);
//System.err.println("IMAGE ["+i+"] orientation="+(orientations[i] == null && orientations[i].length == 2 ? "" : (orientations[i][0] + " " + orientations[i][1])));
					views[i] = getView(list);
//System.err.println("IMAGE ["+i+"] view="+views[i]);
					lateralityViewAndModifiers[i] = getImageLateralityViewModifierAndViewModifier(list);
//System.err.println("IMAGE ["+i+"] lateralityViewAndModifiers="+lateralityViewAndModifiers[i]);
//System.err.println("File "+filenames[f]+": "+lateralityViewAndModifiers[i]);
					lateralities[i] = getLaterality(list);
//System.err.println("IMAGE ["+i+"] laterality="+lateralities[i]);
					dates[i] = getDate(list);
//System.err.println("IMAGE ["+i+"] date="+dates[i]);
					spacing[i] = new PixelSpacing(list,null,false/*preferCalibratedValue - as per current IHE mammo profile, do NOT use Pixel Spacing to override*/,true/*useMagnificationFactorIfPresent*/);
//System.err.println("IMAGE ["+i+"] spacing="+spacing[i]);
				
					SourceImage sImg = new SourceImage(list);
					BufferedImage img = sImg.getBufferedImage();
				
					widths[i] = sImg.getWidth();
					heights[i] = sImg.getHeight();
				
					FlipAsNecessary flipper = new FlipAsNecessary(img,orientations[i],views[i],lateralities[i],doNotFlipOrRotate);
					img = flipper.getImage();
					rowOrientations[i] = flipper.getRowOrientation();
//System.err.println("IMAGE ["+i+"] rowOrientations="+rowOrientations[i]);
					columnOrientations[i] = flipper.getColumnOrientation();
//System.err.println("IMAGE ["+i+"] columnOrientations="+columnOrientations[i]);
					coordinateTransform[i] = flipper.getTransform();

					if (doNotJustify) {
						justifyTopOrBottom[i] = 0;
						justifyTopOrBottom[i] = 0;
					}
					else {
						justifyRightOrLeft[i] = rowOrientations[i]    == null ? 0 : (rowOrientations[i].indexOf("P")    == -1 ? -1 : 1);	// justify to chest wall, which is P (-1 is left)
						justifyTopOrBottom[i] = columnOrientations[i] == null ? 0 : (columnOrientations[i].indexOf("F") == -1 ? 0 : -1);	// justify to axilla, which is !F (-1 is top)
					}
			
					BreastExtentFinder extentFinder = new BreastExtentFinder(img,rowOrientations[i],sImg.getMinimum(),sImg.getMaximum(),sImg.isInverted(),spacing[i].getSpacing());
					//BreastExtentFinder extentFinder = new BreastExtentFinder(img,spacing[i].getSpacing());

					horizontalExtentsInMm[i] = extentFinder.getHorizontalExtentInMm();
					verticalExtentsInMm[i] = extentFinder.getVerticalExtentInMm();
					
					if (horizontalExtentsInMm[i] > maximumHorizontalExtentInMm) {
						maximumHorizontalExtentInMm = horizontalExtentsInMm[i];
					}
					if (verticalExtentsInMm[i] > maximumVerticalExtentInMm) {
						maximumVerticalExtentInMm = verticalExtentsInMm[i];
					}
				
					//verticalCenter[i] = extentFinder.getVerticalCenter();
					upperBounds[i] = extentFinder.getUpperBound();
					lowerBounds[i] = extentFinder.getLowerBound();
					leftBounds [i] = extentFinder.getLeftBound();
					rightBounds[i] = extentFinder.getRightBound();
			
					boolean shareVOIEventsInStudy = false;		// does not seem to work anyway, since adding VOITransform to panel constructor :(
				
					EventContext eventContext = null;
					if (shareVOIEventsInStudy) {
						eventContext = (EventContext)(eventContexts.get(dates[i]));	// use same context for studies with same date
						if (eventContext == null) {
							eventContext = new EventContext(dates[i]);
							eventContexts.put(dates[i],eventContext);
						}
					}
					if (eventContext == null) {
						eventContext = new EventContext(Integer.toString(i));
					}
				
					SingleImagePanel imagePanel = makeNewImagePanel(sImg,eventContext);
					boolean leftSide = justifyRightOrLeft[i] >= 0;
					imagePanel.setDemographicAndTechniqueAnnotations(new MammoDemographicAndTechniqueAnnotations(list,!leftSide),"SansSerif",Font.PLAIN,10,Color.pink);
					imagePanel.setOrientationAnnotations(
						new OrientationAnnotations(leftSide ? invertOrientation(rowOrientations[i]) : rowOrientations[i],columnOrientations[i]),
						"SansSerif",Font.PLAIN,20,Color.pink,leftSide);
					imagePanel.setSideAndViewAnnotationString(
						MammoDemographicAndTechniqueAnnotations.getAbbreviationFromImageLateralityViewModifierAndViewModifierCodeSequenceAttributes(list),
						60/*verticalOffset*/,
						"SansSerif",Font.PLAIN,20,Color.pink,leftSide);
					imagePanel.setShowZoomFactor(true,leftSide,spacing[i].getSpacing(),spacing[i].getDescription());
					if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.VOILUTFunction).equals("SIGMOID")) {
						imagePanel.setVOIFunctionToLogistic();
					}
					imagePanels[i] = imagePanel;
				}
				else if (list.isSRDocument()) {
					int i = nCAD++;
					slf4jlogger.info("CAD [{}] is file {} ({})",i,f,filenames[f]);
					sr[i] = new StructuredReport(list);
				}
				else {
					throw new DicomException("Unsupported SOP Class in file "+filenames[f]);
				}
			}
			catch (Exception e) {	// FileNotFoundException,IOException,DicomException
				slf4jlogger.error("",e);
			}
		}

		int imagesPerRow = nImages >= 4 ? 4 : nImages;			// i.e., 1 -> 1, 2 -> 1, 4 -> 4, 5 -> 4, 8 -> 4
		int imagesPerCol = (nImages - 1) / imagesPerRow + 1;	// i.e., 1 -> 1, 2 -> 2, 4 -> 1, 5 -> 2, 8 -> 2
//System.err.println("nImages="+nImages);
//System.err.println("imagesPerRow="+imagesPerRow);
//System.err.println("imagesPerCol="+imagesPerCol);

		int singleWidth  = frameWidth /imagesPerRow;
		int singleHeight = frameHeight/imagesPerCol;
//System.err.println("singleWidth="+singleWidth);
//System.err.println("singleHeight="+singleHeight);

		if (nImages == 1 && singleWidth > singleHeight) {
			singleWidth = singleWidth/2;			// use only half the screen for a single view and a landscape monitor
		}

		double requestedDisplaySpacing;
		{
			double hSpacing = maximumHorizontalExtentInMm / singleWidth;
			double vSpacing = maximumVerticalExtentInMm / singleHeight;
			requestedDisplaySpacing = hSpacing < vSpacing ? vSpacing : hSpacing;
		}
//System.err.println("requestedDisplaySpacing="+requestedDisplaySpacing);
		
		int[] displayOrder = determineOrderForDisplay(nImages,views,lateralities,lateralityViewAndModifiers,dates);
		
		for (int i=0; i<nImages; ++i) {
			DisplayedAreaSelection displayedAreaSelection = null;
			if (forceFitEntireMatrixToWindow) {
				displayedAreaSelection = new DisplayedAreaSelection(widths[i],heights[i],0,0,widths[i],heights[i],
					true,	// in case spacing was not supplied
					0,0,0,justifyRightOrLeft[i],justifyTopOrBottom[i],false/*crop*/);
			}
			else {
				displayedAreaSelection = new DisplayedAreaSelection(widths[i],heights[i],leftBounds[i],upperBounds[i],rightBounds[i],lowerBounds[i],
					requestedDisplaySpacing <= 0.0/*fitToWindow*/,	// in case spacing was not supplied
					requestedDisplaySpacing,spacing[i].getSpacing(),spacing[i].getSpacing(),justifyRightOrLeft[i],justifyTopOrBottom[i],false/*crop*/);
			}
			imagePanels[i].setDisplayedAreaSelection(displayedAreaSelection);
			imagePanels[i].setPreTransformImageRelativeCoordinates(coordinateTransform[i]);		// null unless was flipped or rotated
		}
		
		Vector preDefinedShapes[] = new Vector[nImages];

		for (int i=0; i<nCAD; ++i) {
			ContentItem root = (ContentItem)(sr[i].getRoot());
			ContentItem findings = root.getNamedChild("DCM","111017");		// "CAD Processing and Findings Summary"
//System.err.println("CAD ["+i+"] findings="+findings);
			if (findings != null) {
				Vector coords = StructuredReport.findAllContainedSOPInstances(root,findings);
				if (coords == null || coords.size() == 0) {
//System.err.println("CAD ["+i+"] contains no SCOORDS");
				}
				else {
					Iterator si = coords.iterator();
					while (si.hasNext()) {
						SpatialCoordinateAndImageReference coord = (SpatialCoordinateAndImageReference)(si.next());
//System.err.println("CAD ["+i+"] coord="+coord);
						if (coord != null && coord.getRenderingIntent() != SpatialCoordinateAndImageReference.RenderingForbidden) {
							String referencedUID = coord.getSOPInstanceUID();
//System.err.println("CAD ["+i+"] referencedUID="+referencedUID);
							if (referencedUID != null && referencedUID.length() > 0) {
								int referencedImageNumber = -1;
								for (int j=0; j<nImages; ++j) {
//System.err.println("CAD ["+i+"] checking image UID ="+imageSOPInstanceUID[j]);
//System.err.println("CAD ["+i+"] checking image source UID ="+imageSourceSOPInstanceUID[j]);
									if (imageSOPInstanceUID[j] != null && imageSOPInstanceUID[j].equals(referencedUID)
									 || imageSourceSOPInstanceUID[j] != null && imageSourceSOPInstanceUID[j].equals(referencedUID)) {
//System.err.println("CAD ["+i+"] found match to referencedImageNumber="+j);
										referencedImageNumber = j;
										break;
									}
								}
								if (referencedImageNumber != -1) {
//System.err.println("CAD ["+i+"] apply to Image # "+referencedImageNumber);
									Vector shapes = preDefinedShapes[referencedImageNumber];
									if (shapes == null) {
										shapes = new Vector();
										preDefinedShapes[referencedImageNumber] = shapes;
									}
									addCADMarkToPreDefinedShapes(coord,shapes);
								}
								else {
									slf4jlogger.error("CAD [{}] could not find match for referencedUID={}",i,referencedUID);
								}
							}
						}
					}
				}
			}
		}

		for (int i=0; i<nImages; ++i) {
			if (preDefinedShapes[i] != null) {
//System.err.println("IMAGE ["+i+"] setting preDefinedShapes");
				imagePanels[i].setPreDefinedShapes(preDefinedShapes[i]);
			}
		}
		
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
		multiPanel.setBackground(Color.black);
		
		for (int x=0; x<imagesPerCol; ++x) {
			for (int y=0; y<imagesPerRow; ++y) {
				int i = x*imagesPerRow+y;
				if (i < nImages) {
					imagePanels[i].setPreferredSize(new Dimension(singleWidth,singleHeight));
					multiPanel.add(imagePanels[displayOrder == null ? i : displayOrder[i]]);
				}
			}
		}
		frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}
	
	/**
	 * @param		frame
	 * @throws	Exception		if internal error
	 */
	public MammoImageViewer(JFrame frame) throws Exception {
		this.frame = frame;
		doCommonConstructorStuff();
	}
	
	/**
	 * @param		frame
	 * @param		filenames
	 * @throws	Exception		if internal error
	 */
	public MammoImageViewer(JFrame frame,String filenames[]) throws Exception {
		this.frame = frame;
		doCommonConstructorStuff();
		loadMultiPanelFromSpecifiedFiles(filenames);
	}
	
	/**
	 * @param		filenames
	 * @throws	Exception		if internal error
	 */
	public MammoImageViewer(String filenames[]) throws Exception {
		frame = new ApplicationFrame();
		GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		boolean isFullScreen = devices.length == 1 && devices[0].isFullScreenSupported();
		frame.setUndecorated(isFullScreen);
		frame.setResizable(!isFullScreen);
		if (isFullScreen) {
			devices[0].setFullScreenWindow(frame);
			frame.validate();
		}
		else {
			frame.pack();
			frame.setVisible(true);
		}
		doCommonConstructorStuff();
		loadMultiPanelFromSpecifiedFiles(filenames);
	}
	
	/**
	 * @throws	Exception		if internal error
	 */
	protected void doCommonConstructorStuff() throws Exception {
		Container content = frame.getContentPane();
		content.setLayout(new GridLayout(1,1));
		multiPanel = new JPanel();
		//multiPanel.setBackground(Color.black);
		frameWidth  = (int)frame.getWidth();
		frameHeight = (int)frame.getHeight();
		Dimension d = new Dimension(frameWidth,frameHeight);
		//multiPanel.setSize(d);
		multiPanel.setPreferredSize(d);
		multiPanel.setBackground(Color.black);
		content.add(multiPanel);
		//frame.pack();
		content.validate();
	}
	
	/**
	 */
	public void clear() {
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg		a list of DICOM files which may contain mammography images or mammography CAD SR objects
	 */
	public static void main(String arg[]) {
		try {
			new MammoImageViewer(arg);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


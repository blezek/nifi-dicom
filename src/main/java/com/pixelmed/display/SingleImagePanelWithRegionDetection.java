/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

//import java.awt.Point;
//import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;
import com.pixelmed.display.event.RegionSelectionChangeEvent; 

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Implements a component that extends a SingleImagePanel to also detect region boundaries within a specified region of interest.</p>
 *
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
class SingleImagePanelWithRegionDetection extends SingleImagePanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SingleImagePanelWithRegionDetection.java,v 1.6 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SingleImagePanelWithRegionDetection.class);

	// Constructors ...

	public SingleImagePanelWithRegionDetection(SourceImage sImg,EventContext typeOfPanelEventContext,int[] sortOrder,Vector preDefinedShapes,Vector preDefinedText,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,sortOrder,preDefinedShapes,preDefinedText,imageGeometry);
	}

	public SingleImagePanelWithRegionDetection(SourceImage sImg,EventContext typeOfPanelEventContext,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,imageGeometry);
	}

	public SingleImagePanelWithRegionDetection(SourceImage sImg,EventContext typeOfPanelEventContext) {
		super(sImg,typeOfPanelEventContext);
	}

	public SingleImagePanelWithRegionDetection(SourceImage sImg) {
		super(sImg);
	}

	// Region selection stuff (set by right mouse drag) ...
	
	private double regionSelectionCenterX;
	private double regionSelectionCenterY;
	private double regionSelectionTLHCX;
	private double regionSelectionTLHCY;
	private double regionSelectionBRHCX;
	private double regionSelectionBRHCY;

	/**
	 * @param	centerX
	 * @param	centerY
	 * @param	oneCornerX
	 * @param	oneCornerY
	 * @param	otherCornerX
	 * @param	otherCornerY
	 */
	private void setRegionSelection(double centerX,double centerY,double oneCornerX,double oneCornerY,double otherCornerX,double otherCornerY) {
//System.err.println("SingleImagePanelWithRegionDetection.setRegionSelection() event: centerX="+centerX+" centerY="+centerY+" oneCornerX="+oneCornerX+" oneCornerY="+oneCornerY+" otherCornerX="+otherCornerX+" otherCornerY="+otherCornerY);
		regionSelectionCenterX = centerX;
		regionSelectionCenterY = centerY;
		if (oneCornerX < otherCornerX) {
			regionSelectionTLHCX=oneCornerX;
			regionSelectionBRHCX=otherCornerX;
		}
		else {
			regionSelectionTLHCX=otherCornerX;
			regionSelectionBRHCX=oneCornerX;
		}
		if (oneCornerY < otherCornerY) {
			regionSelectionTLHCY=oneCornerY;
			regionSelectionBRHCY=otherCornerY;
		}
		else {
			regionSelectionTLHCY=otherCornerY;
			regionSelectionBRHCY=oneCornerY;
		}
	}

	// Event stuff ...

	/**
	 * @param	e
	 */
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right dragged "+e.getX()+" "+e.getY());
			dragInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mouseDragged(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseMoved(MouseEvent e) {
//System.err.println(e.getX()+" "+e.getY());
		super.mouseMoved(e);
	}

	/**
	 * @param	e
	 */
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right pressed "+e.getX()+" "+e.getY());
			startInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mousePressed(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right released "+e.getX()+" "+e.getY());
			endInteractiveDrawing(e.getX(),e.getY());	// sets region selection parameters to propagate in change event
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new RegionSelectionChangeEvent(typeOfPanelEventContext,
				regionSelectionCenterX,regionSelectionCenterY,regionSelectionTLHCX,regionSelectionTLHCY,regionSelectionBRHCX,regionSelectionBRHCY));
		}
		else {
			super.mouseReleased(e);
		}
	}

	// region detection stuff
	
	private class IntegerPointWithValue {
		private int x;
		private int y;
		private int v;
		
		public IntegerPointWithValue(int x,int y,int v) throws Exception {
			if (x > 65535 || x < 0 || y > 65535 || y < 0) {
				throw new Exception("coordinate too large");
			}
			this.x=x;
			this.y=y;
			this.v=v;
		}
		
		public final int getX() { return x; }
		public final int getY() { return y; }
		public final int getV() { return v; }
		
		public final boolean equals(Object o) {
			IntegerPointWithValue c = (IntegerPointWithValue)o;
			return x == c.x && y == c.y;
		}
		
		public final int hashCode() {
			return x<<16 + y;		// will fail if x or y > 65535 or < 0
		}
	}
	
	private IntegerPointWithValue[] findLongestAndShortestPaths(Collection points,double[] voxelSpacing) throws Exception {
		double pixelSpacing = voxelSpacing == null ? 0 : (voxelSpacing[0] == voxelSpacing[1] ? voxelSpacing[0] : 0);
	
		Object pointArray[] = points.toArray();
		int arrayLength = pointArray.length;
		int longestFromIndex=-1;
		int longestToIndex=-1;
		int longestLengthSquared=0;
		for (int from=0; from<arrayLength; ++from) {
			IntegerPointWithValue fromPoint = (IntegerPointWithValue)(pointArray[from]);
			int fromX = fromPoint.getX();
			int fromY = fromPoint.getY();
			for (int to=from+1; to<arrayLength; ++to) {
				IntegerPointWithValue toPoint = (IntegerPointWithValue)(pointArray[to]);
				int toX = toPoint.getX();
				int toY = toPoint.getY();
				int deltaX = toX-fromX;
				int deltaY = toY-fromY;
				int lengthSquared=deltaX*deltaX+deltaY*deltaY;
				if (lengthSquared > longestLengthSquared) {
					longestLengthSquared=lengthSquared;
					longestFromIndex=from;
					longestToIndex=to;
				}
			}
		}
		if (longestFromIndex < 0 || longestToIndex < 0) {
			return null;
		}
		
		IntegerPointWithValue fromPoint = (IntegerPointWithValue)(pointArray[longestFromIndex]);
		IntegerPointWithValue toPoint = (IntegerPointWithValue)(pointArray[longestToIndex]);
		int fromX = fromPoint.getX();
		int toX = toPoint.getX();
		int deltaX = toX-fromX;
		
		if (deltaX < 0) {
			// swap them around; want from point to always be on the left
			int tempIndex=longestFromIndex;
			longestFromIndex=longestToIndex;
			longestToIndex=tempIndex;
			fromPoint = (IntegerPointWithValue)(pointArray[longestFromIndex]);
			toPoint = (IntegerPointWithValue)(pointArray[longestToIndex]);
			fromX = fromPoint.getX();
			toX = toPoint.getX();
			deltaX = toX-fromX;
		}
		
		int fromY = fromPoint.getY();
		int toY = toPoint.getY();
		int deltaY = -(toY-fromY);	// think upside down
		
		double length=Math.sqrt(deltaX*deltaX+deltaY*deltaY);
		
		double angleBetweenXAxisAndLongestPath = Math.atan((double)(deltaY)/(double)(deltaX));

//System.err.println("Longest from ("+fromX+","+fromY+") to ("+toX+","+toY+") length="+length+" pixels, angle="+angleBetweenXAxisAndLongestPath+" radians or "+(angleBetweenXAxisAndLongestPath*180/Math.PI)+" degrees");
		slf4jlogger.info("Long axis length={} pixels ({} mm)",length,(length*pixelSpacing));

		double mostNegativeNormalDistance = 0;
		double mostPositiveNormalDistance = 0;
		
		IntegerPointWithValue mostNegativeTestPoint = null;
		IntegerPointWithValue mostPositiveTestPoint = null;
		
		IntegerPointWithValue mostNegativeIntersectionPoint = null;
		IntegerPointWithValue mostPositiveIntersectionPoint = null;
		
		for (int test=0; test<arrayLength; ++test) {
			IntegerPointWithValue testPoint = (IntegerPointWithValue)(pointArray[test]);
			int testX = testPoint.getX();
			int testY = testPoint.getY();
			int deltaFromToTestX = testX-fromX;
			int deltaFromToTestY = -(testY-fromY);	// think upside down
			double distanceBetweenFromAndTestPoint=Math.sqrt(deltaFromToTestX*deltaFromToTestX+deltaFromToTestY*deltaFromToTestY);
			double angleBetweenXAxisAndFromToTestPoint = Math.atan((double)(deltaFromToTestY)/(double)(deltaFromToTestX));
//System.err.println("Test from ("+fromX+","+fromY+") to ("+testX+","+testY+") length="+distanceBetweenFromAndTestPoint+" pixels, angle="+angleBetweenXAxisAndFromToTestPoint+" radians or "+(angleBetweenXAxisAndFromToTestPoint*180/Math.PI)+" degrees");

			double angleBetweenLongestPathAndFromToTestPoint = angleBetweenXAxisAndFromToTestPoint - angleBetweenXAxisAndLongestPath;

			double distanceBetweenFromAndTestPointProjectedNormalToLongestPath = distanceBetweenFromAndTestPoint * Math.cos(angleBetweenLongestPathAndFromToTestPoint);
			double distanceBetweenTestPointAndLongestPathAlongNormal = distanceBetweenFromAndTestPoint * Math.sin(angleBetweenLongestPathAndFromToTestPoint);
			
			int deltaFromToTestProjectedOntoLongestPathX = (int)(distanceBetweenFromAndTestPointProjectedNormalToLongestPath * Math.cos(angleBetweenXAxisAndLongestPath));
			int deltaFromToTestProjectedOntoLongestPathY = (int)(distanceBetweenFromAndTestPointProjectedNormalToLongestPath * Math.sin(angleBetweenXAxisAndLongestPath));
			
			int testProjectedOntoLongestPathX = fromX + deltaFromToTestProjectedOntoLongestPathX;
			int testProjectedOntoLongestPathY = fromY - deltaFromToTestProjectedOntoLongestPathY;	// think upside down
//System.err.println("Intersects long axis at ("+testProjectedOntoLongestPathX+","+testProjectedOntoLongestPathY+") distance="+distanceBetweenTestPointAndLongestPathAlongNormal+" pixels");

			if (distanceBetweenTestPointAndLongestPathAlongNormal < mostNegativeNormalDistance
			 && angleBetweenLongestPathAndFromToTestPoint <= Math.PI/2 && angleBetweenLongestPathAndFromToTestPoint >= -Math.PI/2) {	// Don't look "behind" from point
				mostNegativeNormalDistance=distanceBetweenTestPointAndLongestPathAlongNormal;
				mostNegativeTestPoint=testPoint;
				mostNegativeIntersectionPoint = new IntegerPointWithValue(testProjectedOntoLongestPathX,testProjectedOntoLongestPathY,0/*dummy value*/);
//System.err.println("angleBetweenXAxisAndFromToTestPoint="+(angleBetweenXAxisAndFromToTestPoint*180/Math.PI)+", angleBetweenXAxisAndLongestPath="+(angleBetweenXAxisAndLongestPath*180/Math.PI)+", angleBetweenLongestPathAndFromToTestPoint="+(angleBetweenLongestPathAndFromToTestPoint*180/Math.PI));
//System.err.println("Most negative is now from ("+testX+","+testY+") to ("+testProjectedOntoLongestPathX+","+testProjectedOntoLongestPathY+") length="+distanceBetweenTestPointAndLongestPathAlongNormal+" pixels");
			}
			if (distanceBetweenTestPointAndLongestPathAlongNormal > mostPositiveNormalDistance
			 && angleBetweenLongestPathAndFromToTestPoint <= Math.PI/2 && angleBetweenLongestPathAndFromToTestPoint >= -Math.PI/2) {	// Don't look "behind" from point
				mostPositiveNormalDistance=distanceBetweenTestPointAndLongestPathAlongNormal;
				mostPositiveTestPoint=testPoint;
				mostPositiveIntersectionPoint = new IntegerPointWithValue(testProjectedOntoLongestPathX,testProjectedOntoLongestPathY,0/*dummy value*/);
//System.err.println("angleBetweenXAxisAndFromToTestPoint="+(angleBetweenXAxisAndFromToTestPoint*180/Math.PI)+", angleBetweenXAxisAndLongestPath="+(angleBetweenXAxisAndLongestPath*180/Math.PI)+", angleBetweenLongestPathAndFromToTestPoint="+(angleBetweenLongestPathAndFromToTestPoint*180/Math.PI));
//System.err.println("Most positive is now from ("+testX+","+testY+") to ("+testProjectedOntoLongestPathX+","+testProjectedOntoLongestPathY+") length="+distanceBetweenTestPointAndLongestPathAlongNormal+" pixels");
			}
		}

		slf4jlogger.info("Short axis most negative side length={} pixels ({} mm)",mostNegativeNormalDistance,(mostNegativeNormalDistance*pixelSpacing));
		slf4jlogger.info("Short axis most positive side length={} pixels ({} mm)",mostPositiveNormalDistance,(mostPositiveNormalDistance*pixelSpacing));
		slf4jlogger.info("Short axis total length={} pixels ({} mm)",(Math.abs(mostNegativeNormalDistance)+Math.abs(mostPositiveNormalDistance)),((Math.abs(mostNegativeNormalDistance)+Math.abs(mostPositiveNormalDistance))*pixelSpacing));

		IntegerPointWithValue resultArray[] = new IntegerPointWithValue[6];
		resultArray[0]=fromPoint;
		resultArray[1]=toPoint;
		resultArray[2]=mostNegativeTestPoint;
		resultArray[3]=mostNegativeIntersectionPoint;
		resultArray[4]=mostPositiveTestPoint;
		resultArray[5]=mostPositiveIntersectionPoint;
		return resultArray;
	}
	
	private void detectAndDrawRegion(
			BufferedImage image,
			double[] voxelSpacing,
			Vector interactiveDrawingShapes,
			double regionSelectionCenterX,double regionSelectionCenterY,
			double regionSelectionTLHCX,double regionSelectionTLHCY,
			double regionSelectionBRHCX,double regionSelectionBRHCY) throws Exception {
		
		HashSet pointsAlreadyChecked = new HashSet();
		HashSet pointsYetToBeChecked = new HashSet();
		
		int width = image.getWidth();
		int height = image.getHeight();
		Raster raster = image.getRaster();

		int wholePixelRegionSelectionCenterX = (int)regionSelectionCenterX;
		if (wholePixelRegionSelectionCenterX < 0) {
			wholePixelRegionSelectionCenterX = 0;
		}
		else if (wholePixelRegionSelectionCenterX >= width) {
			wholePixelRegionSelectionCenterX = width-1;
		}
		int wholePixelRegionSelectionCenterY = (int)regionSelectionCenterY;
		if (wholePixelRegionSelectionCenterY < 0) {
			wholePixelRegionSelectionCenterY = 0;
		}
		else if (wholePixelRegionSelectionCenterY >= height) {
			wholePixelRegionSelectionCenterY = height-1;
		}

		int seedValue = raster.getSample(wholePixelRegionSelectionCenterX,wholePixelRegionSelectionCenterY,0/*band*/);
		pointsYetToBeChecked.add(new IntegerPointWithValue(wholePixelRegionSelectionCenterX,wholePixelRegionSelectionCenterY,seedValue));

//System.err.println("starting at ("+regionSelectionCenterX+","+regionSelectionCenterY+")");

		int n=1;
		double runningMean = seedValue;
		double runningSD = 0;
		double runningSumOfSquares = 0;

//System.err.println("start ("+regionSelectionCenterX+","+regionSelectionCenterY+")="+seedValue+" [n="+n+", runningMean="+runningMean+", SD="+runningSD+"]");

		int numberOfPointsToIncludeInInitialAverage = 16;
		double factorToUpdatethresholdDifferenceDerivedFromSD = 2.0;
		int thresholdDifference = 20;
		int differenceAbout = seedValue;
		//int thresholdDifferenceDerivedFromSD = 0;
		
		do {
			Iterator i = pointsYetToBeChecked.iterator();
			if (i.hasNext()) {	// not while ... the later addition triggers a ConcurrentModificationException
				IntegerPointWithValue point = (IntegerPointWithValue)(i.next());
				i.remove();
				pointsAlreadyChecked.add(point);
				
				int x = (int)(point.getX());
				int y = (int)(point.getY());
				int v = raster.getSample(x,y,0/*band*/);
				
				double difference = v - runningMean;
				double newMean = runningMean + difference / (n+1);
				runningSumOfSquares += difference * (v - newMean);
				runningMean = newMean;
				runningSD = Math.sqrt(runningSumOfSquares/n);
				++n;
				
				if (n > numberOfPointsToIncludeInInitialAverage) {
					thresholdDifference = (int)(runningSD*factorToUpdatethresholdDifferenceDerivedFromSD);
					//thresholdDifference = thresholdDifferenceDerivedFromSD;
					// leave differenceAbout alone from now on
				}
				else {
					//use initially set thresholdDifference
					differenceAbout = (int)runningMean;
					//thresholdDifferenceDerivedFromSD = (int)(runningSD*factorToUpdatethresholdDifferenceDerivedFromSD);
				}

//System.err.println("checking ("+x+","+y+")="+v+" [n="+n+", runningMean="+runningMean+", SD="+runningSD+", differenceAbout="+differenceAbout+", thresholdDifference="+thresholdDifference+"]");

				// check 8-connected regions
				for (int deltaX=-1; deltaX <= 1; ++deltaX) {
					for (int deltaY=-1; deltaY <= 1; ++deltaY) {
						if (deltaX != 0 || deltaY != 0) {	/// don't check center pixel against itself
							int xPrime = x + deltaX;
							int yPrime = y + deltaY;
							if (xPrime >= regionSelectionTLHCX
							 && xPrime <= regionSelectionBRHCX
							 && yPrime >= regionSelectionTLHCY
							 && yPrime <= regionSelectionBRHCY) {	// constrain to supplied boundaries (inclusive)
								int vPrime = raster.getSample(xPrime,yPrime,0/*band*/);
								//if (Math.abs(v-vPrime) <= thresholdDifference) {
								//if (Math.abs(seedValue-vPrime) <= thresholdDifference) {
								//if (Math.abs(runningMean-vPrime) <= thresholdDifference) {
								if (Math.abs(differenceAbout-vPrime) <= thresholdDifference) {
									IntegerPointWithValue newPoint = new IntegerPointWithValue(xPrime,yPrime,vPrime);
									if (!pointsAlreadyChecked.contains(newPoint)) {
//System.err.println("adding ("+xPrime+","+yPrime+")="+vPrime);
										pointsYetToBeChecked.add(newPoint);
									}
								}
							}
						}
					}
				}
			}
		} while (!pointsYetToBeChecked.isEmpty());
		
		//Iterator id = pointsAlreadyChecked.iterator();
		///*if (id.hasNext())*/ {
		//	//IntegerPointWithValue p = (IntegerPointWithValue)(id.next());
		//	GeneralPath path = new GeneralPath();
		//	//path.moveTo(p.getX(),p.getY());
		//	while (id.hasNext()) {
		//		IntegerPointWithValue p = (IntegerPointWithValue)(id.next());
		//		path.moveTo(p.getX(),p.getY());
		//		path.lineTo(p.getX(),p.getY());
		//	}
		//	interactiveDrawingShapes.add(path);
		//}

		int[] topYValueForX = new int[width];
		int[] bottomYValueForX = new int[width];
		for (int x=0; x<width; ++x) {
			topYValueForX[x]=height;
			bottomYValueForX[x]=-1;
		}
		Iterator id = pointsAlreadyChecked.iterator();
		while (id.hasNext()) {
			IntegerPointWithValue p = (IntegerPointWithValue)(id.next());
			int x = p.getX();
			int y = p.getY();
			if (y < topYValueForX[x]) {
				topYValueForX[x]=y;	// top in the sense of higher up in the displayed image
			}
			if (y > bottomYValueForX[x]) {
				bottomYValueForX[x]=y;	// bottom in the sense of lower down in the displayed image

			}
		}
		GeneralPath path = new GeneralPath();
		boolean foundOne = false;
		int firstX=-1;
		int firstY=-1;
		for (int x=0; x<width; ++x) {
			int y=topYValueForX[x];
			if (y < height) {
				if (!foundOne) {
					path.moveTo(x,y);
					foundOne=true;
					firstX=x;
					firstY=y;
				}
				else {
					path.lineTo(x,y);
				}
			}
		}
		for (int x=width-1; x>=0; --x) {
			int y=bottomYValueForX[x];
			if (y >= 0) {
				if (!foundOne) {
					path.moveTo(x,y);
					foundOne=true;
					firstX=x;
					firstY=y;
				}
				else {
					path.lineTo(x,y);
				}
			}
		}
		if (foundOne) {
			path.lineTo(firstX,firstY);

			IntegerPointWithValue longestPath[] = null;
			try {
				longestPath=findLongestAndShortestPaths(pointsAlreadyChecked,voxelSpacing);
			}
			catch (Exception e) {
				slf4jlogger.error("",e);	// possible whilst creating points
			}
			
			if (longestPath != null) {
				path.moveTo(longestPath[0].getX(),longestPath[0].getY());
				path.lineTo(longestPath[1].getX(),longestPath[1].getY());
			
				if (longestPath[2] != null && longestPath[3] != null) {
					path.moveTo(longestPath[2].getX(),longestPath[2].getY());
					path.lineTo(longestPath[3].getX(),longestPath[3].getY());
				}
				if (longestPath[4] != null && longestPath[5] != null) {
					path.moveTo(longestPath[4].getX(),longestPath[4].getY());
					path.lineTo(longestPath[5].getX(),longestPath[5].getY());
				}

				interactiveDrawingShapes.add(path);
			}
		}
	}

	// stuff to handle drawing ...

	/***/
	protected Point2D startPoint;
	/***/
	static final int crossSize = 5;		// actually just one arm of the cross

	/**
	 * @param	x
	 * @param	y
	 */
	protected void startInteractiveDrawing(int x,int y) {
		startPoint = getImageCoordinateFromWindowCoordinate(x,y);
	}

	/**
	 * @param	x
	 * @param	y
	 */
	protected void dragInteractiveDrawing(int x,int y) {
		interactiveDrawingShapes = new Vector();
	
		double startX = startPoint.getX();
		double startY = startPoint.getY();
		Point2D endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		double endX = endPoint.getX();
		double endY = endPoint.getY();
		interactiveDrawingShapes.add(new Line2D.Double(startPoint,endPoint));

		double width = (endX - startX)*2;
		if (width < 0) {
			width=-width;
		}
		double height = (endY - startY)*2;
		if (height < 0) {
			height=-height;
		}
		double tlhcX=startX-width/2;
		double tlhcY=startY-height/2;
		interactiveDrawingShapes.add(new Rectangle2D.Double(tlhcX,tlhcY,width,height));

		repaint();
	}
	/**
	 * @param	x
	 * @param	y
	 */
	protected void endInteractiveDrawing(int x,int y) {
		interactiveDrawingShapes = new Vector();
		
		double startX = startPoint.getX();
		double startY = startPoint.getY();
		Point2D endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		double endX = endPoint.getX();
		double endY = endPoint.getY();

		//interactiveDrawingShapes.add(new Line2D.Float(new Point(startX-crossSize,startY-crossSize),new Point(startX+crossSize,startY+crossSize)));
		//interactiveDrawingShapes.add(new Line2D.Float(new Point(startX+crossSize,startY-crossSize),new Point(startX-crossSize,startY+crossSize)));
		//interactiveDrawingShapes.add(new Line2D.Float(startPoint,endPoint));
		//interactiveDrawingShapes.add(new Line2D.Float(new Point(endX-crossSize,endY-crossSize),new Point(endX+crossSize,endY+crossSize)));
		//interactiveDrawingShapes.add(new Line2D.Float(new Point(endX+crossSize,endY-crossSize),new Point(endX-crossSize,endY+crossSize)));
		
		double width = (endX - startX)*2;
		if (width < 0) {
			width=-width;
		}
		double height = (endY - startY)*2;
		if (height < 0) {
			height=-height;
		}
		double tlhcX=startX-width/2;
		double tlhcY=startY-height/2;
		//interactiveDrawingShapes.add(new Rectangle(tlhcX,tlhcY,width,height));
		
		setRegionSelection(startX,startY,tlhcX,tlhcY,tlhcX+width,tlhcY+height);

		try {
			int whichFrame = currentSrcImageSortOrder != null ? currentSrcImageSortOrder[currentSrcImageIndex] : currentSrcImageIndex;
			GeometryOfVolume volumeGeometry = getImageGeometry();
			GeometryOfSlice[] sliceGeometries = volumeGeometry == null ? null : volumeGeometry.getGeometryOfSlices();
			GeometryOfSlice sliceGeometry = sliceGeometries == null ? null : sliceGeometries[whichFrame];
			double[] voxelSpacing = sliceGeometry == null ? null : sliceGeometry.getVoxelSpacingArray();

			detectAndDrawRegion(
				sImg.getBufferedImage(whichFrame),
				voxelSpacing,
				interactiveDrawingShapes,
				regionSelectionCenterX,regionSelectionCenterY,
				regionSelectionTLHCX,regionSelectionTLHCY,
				regionSelectionBRHCX,regionSelectionBRHCY);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
		
		repaint();
	}
}






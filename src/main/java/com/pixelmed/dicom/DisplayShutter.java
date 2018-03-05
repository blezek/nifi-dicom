/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;

import java.awt.geom.Point2D;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A set of display shutter parameters constructed from the attributes of the DICOM Display Shutter Module.</p>
 *
 * @author	dclunie
 */
public class DisplayShutter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DisplayShutter.java,v 1.7 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DisplayShutter.class);
	
	protected boolean hasCircularShutter;
	protected int centerOfCircularShutterY;		// row (1st value)
	protected int centerOfCircularShutterX;		// column (2nd value)
	protected int radiusOfCircularShutter;
			
	protected boolean hasRectangularShutter;
	protected int shutterLeftVerticalEdge;
	protected int shutterRightVerticalEdge;
	protected int shutterUpperHorizontalEdge;
	protected int shutterLowerHorizontalEdge;
	
	protected boolean hasPolygonalShutter;
	protected int[] verticesOfPolygonalShutter;
	protected Point2D[] verticesOfPolygonalShutterAsPoints;
	
	/**
	 * Extract the display shutter paramaters from a list of attributes
	 *
	 * @param	list	list of attributes
	 */
	public DisplayShutter(AttributeList list) {
		String[] shutterShapes = Attribute.getStringValues(list,TagFromName.ShutterShape);
		if (shutterShapes != null) {
			for (int i=0; i<shutterShapes.length; ++i) {
				String shape = shutterShapes[i];
				if (shape != null) {
					if (shape.equals("RECTANGULAR") || shape.equals("RECTANGLE")) {	// only RECTANGULAR is legal, but may be bug
						hasRectangularShutter = true;
						shutterLeftVerticalEdge    = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.ShutterLeftVerticalEdge,0);
						shutterRightVerticalEdge   = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.ShutterRightVerticalEdge,0);
						shutterUpperHorizontalEdge = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.ShutterUpperHorizontalEdge,0);
						shutterLowerHorizontalEdge = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.ShutterLowerHorizontalEdge,0);
					}
					else if (shape.equals("CIRCULAR")) {
						hasCircularShutter=true;
						int[] centerOfCircularShutter = Attribute.getIntegerValues(list,TagFromName.CenterOfCircularShutter);
						if (centerOfCircularShutter != null && centerOfCircularShutter.length >= 2) {
							centerOfCircularShutterY = centerOfCircularShutter[0];		// row (1st value)
							centerOfCircularShutterX = centerOfCircularShutter[1];		// column (2nd value)
						}
						radiusOfCircularShutter = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.RadiusOfCircularShutter,0);
					}
					else if (shape.equals("POLYGONAL")) {
						hasPolygonalShutter=true;
						verticesOfPolygonalShutter = Attribute.getIntegerValues(list,TagFromName.VerticesOfThePolygonalShutter);
					}
					else {
						slf4jlogger.warn("DisplayShutter(): unrecognized shutter shape - {} - ignored",shape);
					}
				}
			}
		}
	}
	
	/**
	 * Set the parameters of a rectangular shutter.
	 *
	 * @param	shutterLeftVerticalEdge		left vertical edge
	 * @param	shutterRightVerticalEdge	right vertical edge
	 * @param	shutterUpperHorizontalEdge	upper horizontal edge
	 * @param	shutterLowerHorizontalEdge	lower horizontal edge
	 */
	public void setRectangularDisplayShutter(int shutterLeftVerticalEdge,int shutterRightVerticalEdge,int shutterUpperHorizontalEdge,int shutterLowerHorizontalEdge) {
		hasRectangularShutter = true;
		this.shutterLeftVerticalEdge    = shutterLeftVerticalEdge;
		this.shutterRightVerticalEdge   = shutterRightVerticalEdge;
		this.shutterUpperHorizontalEdge = shutterUpperHorizontalEdge;
		this.shutterLowerHorizontalEdge = shutterLowerHorizontalEdge;
	}
	
	/**
	 * Is there a rectangular shutter.
	 *
	 * @return			true if is a rectangular shutter
	 */
	public boolean isRectangularShutter()      { return hasRectangularShutter; }

	/**
	 * Get left vertical edge of rectangular shutter.
	 *
	 * @return			left vertical edge
	 */
	public int getShutterLeftVerticalEdge()    { return shutterLeftVerticalEdge; }

	/**
	 * Get right vertical edge of rectangular shutter.
	 *
	 * @return			right vertical edge
	 */
	public int getShutterRightVerticalEdge()   { return shutterRightVerticalEdge; }

	/**
	 * Get upper horizontal edge of rectangular shutter.
	 *
	 * @return			upper horizontal edge
	 */
	public int getShutterUpperHorizontalEdge() { return shutterUpperHorizontalEdge; }

	/**
	 * Get lower horizontal edge of rectangular shutter.
	 *
	 * @return			lower horizontal edge
	 */
	public int getShutterLowerHorizontalEdge() { return shutterLowerHorizontalEdge; }
	
	/**
	 * Get TLHC of rectangular shutter.
	 *
	 * @return			top left hand corner
	 */
	public Point2D getRectangularShutterTLHC() { return new Point2D.Double(shutterLeftVerticalEdge,shutterUpperHorizontalEdge); }
	
	/**
	 * Get BRHC of rectangular shutter.
	 *
	 * @return			bottom right hand corner
	 */
	public Point2D getRectangularShutterBRHC() { return new Point2D.Double(shutterRightVerticalEdge,shutterLowerHorizontalEdge); }
	
	/**
	 * Set the parameters of a circular shutter.
	 *
	 * @param	centerOfCircularShutterX	center X value (column)
	 * @param	centerOfCircularShutterY	center Y value (row)
	 * @param	radiusOfCircularShutter		radius
	 */
	public void setCircularDisplayShutter(int centerOfCircularShutterX,int centerOfCircularShutterY,int radiusOfCircularShutter) {
		hasCircularShutter = true;
		this.centerOfCircularShutterX  = centerOfCircularShutterX;
		this.centerOfCircularShutterY = centerOfCircularShutterY;
		this.radiusOfCircularShutter  = radiusOfCircularShutter;
	}
	
	/**
	 * Is there a circular shutter.
	 *
	 * @return			true if is a circular shutter
	 */
	public boolean isCircularShutter()       { return hasCircularShutter; }

	/**
	 * Get center X value of circular shutter.
	 *
	 * @return			center X value
	 */
	public int getCenterOfCircularShutterX() { return centerOfCircularShutterX; }

	/**
	 * Get center Y value of circular shutter.
	 *
	 * @return			center Y value
	 */
	public int getCenterOfCircularShutterY() { return centerOfCircularShutterY; }

	/**
	 * Get radius of circular shutter.
	 *
	 * @return			radius
	 */
	public int getRadiusOfCircularShutter()  { return radiusOfCircularShutter; }
	
	/**
	 * Get TLHC of rectangle bounding circular shutter.
	 *
	 * For example, to use to draw as ellipse.
	 *
	 * @return			top left hand corner
	 */
	public Point2D getCircularShutterTLHC() { return new Point2D.Double(centerOfCircularShutterX-radiusOfCircularShutter,centerOfCircularShutterY-radiusOfCircularShutter); }
	
	/**
	 * Get BRHC of rectangle bounding circular shutter.
	 *
	 * For example, to use to draw as ellipse.
	 *
	 * @return			bottom right hand corner
	 */
	public Point2D getCircularShutterBRHC() { return new Point2D.Double(centerOfCircularShutterX+radiusOfCircularShutter,centerOfCircularShutterY+radiusOfCircularShutter); }
	
	/**
	 * Is there a polygonal shutter.
	 *
	 * @return			true if is a polygonal shutter
	 */
	public boolean isPolygonalShutter()       { return hasPolygonalShutter; }

	/**
	 * Get vertices of polygonal shutter.
	 *
	 * @return			vertices as pairs of row (y) and column (x) values, as encoded in the DICOM attributes
	 */
	public int[] getVerticesOfPolygonalShutter()  { return verticesOfPolygonalShutter; }

	/**
	 * Get vertices of polygonal shutter as Point2D.
	 *
	 * For example, to use to build a 2D Shape.
	 *
	 * @return			vertices as array of Point2D
	 */
	public Point2D[] getVerticesOfPolygonalShutterAsPoint2D() {
		if (verticesOfPolygonalShutterAsPoints == null) {
			int numberOfVertices = verticesOfPolygonalShutter.length/2;
//System.err.println("DisplayShutter.getVerticesOfPolygonalShutterAsPoint2D(): numberOfVertices="+numberOfVertices);
			verticesOfPolygonalShutterAsPoints = new Point2D[numberOfVertices];
			for (int i=0; i<numberOfVertices; ++i) {
				verticesOfPolygonalShutterAsPoints[i] = new Point2D.Double(verticesOfPolygonalShutter[i*2+1],verticesOfPolygonalShutter[i*2]);	// DICOM encodes row (y) first; Point2D is the reverse (x) first
			}
		}
		return verticesOfPolygonalShutterAsPoints;
	}
	
	/***/
	public final String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Display Shutters...\n");
		if (hasRectangularShutter) {
			strbuf.append("\tRectangular: ");
			strbuf.append("left vertical edge = ");
			strbuf.append(shutterLeftVerticalEdge);
			strbuf.append(", ");
			strbuf.append("right vertical edge = ");
			strbuf.append(shutterRightVerticalEdge);
			strbuf.append(", ");
			strbuf.append("upper horizontal edge = ");
			strbuf.append(shutterUpperHorizontalEdge);
			strbuf.append(", ");
			strbuf.append("lower horizontal edge = ");
			strbuf.append(shutterLowerHorizontalEdge);
			strbuf.append("\n");
		}
		if (hasCircularShutter) {
			strbuf.append("\tCircular: ");
			strbuf.append("center X = ");
			strbuf.append(centerOfCircularShutterX);
			strbuf.append(", ");
			strbuf.append("center Y = ");
			strbuf.append(centerOfCircularShutterY);
			strbuf.append(", ");
			strbuf.append("radius = ");
			strbuf.append(radiusOfCircularShutter);
			strbuf.append("\n");
		}
		return strbuf.toString();
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.geometry;

import java.util.Vector;
import java.util.Iterator;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import javax.vecmath.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>An factory class that provides instances of {@link com.pixelmed.geometry.LocalizerPoster LocalizerPoster}
 * that are used for posting the position of specified
 * slices and volumes on (usually orthogonal) localizer images.</p>
 *
 * @see com.pixelmed.geometry.LocalizerPoster
 *
 * @author	dclunie
 */
public class LocalizerPosterFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/LocalizerPosterFactory.java,v 1.13 2017/01/24 10:50:44 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(LocalizerPosterFactory.class);
	
	private static final String dumpPathSegmentType(int pathSegmentType) {
		String name = null;
		if (pathSegmentType == PathIterator.SEG_CLOSE) {
			name="SEG_CLOSE";
		}
		else if (pathSegmentType == PathIterator.SEG_CUBICTO) {
			name="SEG_CUBICTO";
		}
		else if (pathSegmentType == PathIterator.SEG_LINETO) {
			name="SEG_LINETO";
		}
		else if (pathSegmentType == PathIterator.SEG_MOVETO) {
			name="SEG_MOVETO";
		}
		else if (pathSegmentType == PathIterator.SEG_QUADTO) {
			name="SEG_QUADTO";
		}
		else {
			name=Integer.toString(pathSegmentType);
		}
		return name;
	}
	
	private static final String dumpArray(double[] array) {
		StringBuffer buffer = new StringBuffer();
		if (array == null) {
			buffer.append("Shape segment array is null");
		}
		else {
			String prefix="";
			if (array != null && array.length > 0) {
				for (int i=0; i<array.length; ++i) {
					buffer.append(prefix);
					prefix=",";
					buffer.append(array[i]);
				}
			}
		}
		return buffer.toString();
	}
	
	public static final void dumpShape(Shape shape) {
		if (shape == null) {
			System.err.println("Shape is null");
		}
		else {
			double[] coords = new double[6];
			PathIterator iterator = shape.getPathIterator(new AffineTransform());
			while (!iterator.isDone()) {
				int pathSegmentType = iterator.currentSegment(coords);
				System.err.println("Segment type = "+dumpPathSegmentType(pathSegmentType)+" coords = "+dumpArray(coords));
				iterator.next();
			}
		}
	}
	
	public static final void dumpShapes(Vector shapes) {
		if (shapes == null) {
			System.err.println("Shapes is null");
		}
		else {
			Iterator i = shapes.iterator();
			while (i.hasNext()) {
				System.err.println("Shape:");
				dumpShape((Shape)i.next());
			}
		}
	}
	
	/***/
	private LocalizerPosterFactory() {};
	
	/**
	 * <p>Return a {@link com.pixelmed.geometry.LocalizerPoster LocalizerPoster} with the
	 * specified characteristics.</p>
	 *
	 * @param	projectRatherThanIntersect	if true, project onto the localizer rather than intersect with it
	 * @param	planeRatherThanVolume		for intersection (only), outline the intersection of a volume (cube)rather than a slice (rectangle)
	 * @return					an appropriate localizer poster, or null if one cannot be instantiated
	 */
	public static LocalizerPoster getLocalizerPoster(boolean projectRatherThanIntersect,boolean planeRatherThanVolume) {
		LocalizerPoster poster = null;
		try {
			if (projectRatherThanIntersect) {
				poster = new ProjectSlice();
			}
			else {
				if (planeRatherThanVolume) {
					poster = new IntersectSlice();
				}
				else {
					poster = new IntersectVolume();
				}
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
		catch (NoClassDefFoundError e) {
			slf4jlogger.error("",e);
		}
		return poster;
	}
	
	/**
	 * <p>For testing.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {
		LocalizerPoster poster = LocalizerPosterFactory.getLocalizerPoster(true/*project*/,true/*plane (irrelevant)*/);
		// sagittal localizer centered about 0,0,0
		poster.setLocalizerGeometry(
			new Vector3d(0,1,0) /*row*/,
			new Vector3d(0,0,-1) /*column*/,
			new Point3d(0,-127.5,127.5) /*tlhc*/,
			new Vector3d(.5,.5,0) /*pixelSpacing*/,
			new Vector3d(512,512,1) /*dimensions*/);
			
		{
			// half-width axial slice centered about 0,0,0
			Vector shapes = poster.getOutlineOnLocalizerForThisGeometry(
				new Vector3d(1,0,0) /*row*/,
				new Vector3d(0,1,0) /*column*/,
				new Point3d(-63.5,-63.5,0) /*tlhc*/,
				new Vector3d(.5,.5,0) /*pixelSpacing*/,
				0 /*sliceThickness*/,
				new Vector3d(256,256,1) /*dimensions*/);
			dumpShapes(shapes);
		}
		{
			// full-width axial slice centered about 0,0,0
			Vector shapes = poster.getOutlineOnLocalizerForThisGeometry(
				new Vector3d(1,0,0) /*row*/,
				new Vector3d(0,1,0) /*column*/,
				new Point3d(-127.5,-127.5,0) /*tlhc*/,
				new Vector3d(.5,.5,0) /*pixelSpacing*/,
				0 /*sliceThickness*/,
				new Vector3d(512,512,1) /*dimensions*/);
			dumpShapes(shapes);
		}
	}
}

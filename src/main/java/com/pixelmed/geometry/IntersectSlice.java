/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.geometry;

import java.util.Vector;
import java.awt.Shape;
import java.awt.Polygon;
import java.awt.geom.*;
import java.awt.geom.Line2D.*;
import javax.vecmath.*;

/**
 * @author	dclunie
 */
class IntersectSlice extends LocalizerPoster {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/IntersectSlice.java,v 1.13 2017/01/24 10:50:43 dclunie Exp $";
	
	// package scope ... applications use LocalizerPosterFactory
	IntersectSlice() {
	}
	
	private boolean allTrue(boolean[] array) {
		boolean all = true;
		for (int i=0; i<array.length; ++i) {
			if (!array[i]) {
				all = false;
				break;
			}
		}
		return all;
	}
	
	private boolean oppositeEdges(boolean[] array) {
		return array[0] && array[2] || array[1] && array[3];
	}
	
	private boolean adjacentEdges(boolean[] array) {
		return array[0] && array[1] || array[1] && array[2] || array[2] && array[3] || array[3] && array[0];
	}
	
	private boolean[] classifyCornersOfRectangleIntoEdgesCrossingZPlane(Point3d[] corners) {
		int size = corners.length;
		double[] thisArray = new double[3];
		double[] nextArray = new double[3];
		boolean classification[] = new boolean[size];
		for (int i=0; i<size; ++i) {
			int next = (i == size-1) ? 0 : i+1;
//System.err.print("["+i+","+next+"] ");
			classification[i] = classifyCornersIntoEdgeCrossingZPlane(corners[i],corners[next]);
		}
		return classification;
	}
	
	public Vector getOutlineOnLocalizerForThisGeometry(
			Vector3d row,Vector3d column,Point3d tlhc,Tuple3d voxelSpacing,double sliceThickness,Tuple3d dimensions) {
//System.err.println("IntersectSlice.getOutlineOnLocalizerForThisGeometry()");
		Point3d[] corners = getCornersOfSourceRectangleInSourceSpace(row,column,tlhc,voxelSpacing,dimensions);
		for (int i=0; i<4; ++i) {
			// We want to consider each edge of the source slice with respect to
			// the plane of the target localizer, so transform the source corners
			// into the target localizer space, and then see which edges cross
			// the Z plane of the localizer 

			corners[i] = transformPointFromSourceSpaceIntoLocalizerSpace(corners[i]);
			
			// Now, points with a Z value of zero are in the plane of the localizer plane
			// Edges with one Z value +ve (or 0) and the other -ve (or 0) cross (or touch) the localizer plane
			// Edges with both Z values +ve or both -ve don't cross the localizer plane
		}
		
		boolean edges[] = classifyCornersOfRectangleIntoEdgesCrossingZPlane(corners);

		Vector shapes = null;
		if (allTrue(edges)) {
//System.err.println("Source in exactly the same plane as the localizer");
			shapes = drawOutlineOnLocalizer(corners);		// draw a rectangle
		}
		else if (oppositeEdges(edges)) {
//System.err.println("Opposite edges cross the localizer");
			// draw line between where two edges cross (have zero Z value)
			shapes = drawLinesBetweenAnyPointsWhichIntersectPlaneWhereZIsZero(corners);
		}
		else if (adjacentEdges(edges)) {
//System.err.println("Adjacent edges cross the localizer");
			// draw line between where two edges cross (have zero Z value)
			shapes = drawLinesBetweenAnyPointsWhichIntersectPlaneWhereZIsZero(corners);
		}
		else {
//System.err.println("No edges cross the localizer");
			// draw nothing
		}
		
		return shapes;
	}
}

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
class ProjectSlice extends LocalizerPoster {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/ProjectSlice.java,v 1.9 2017/01/24 10:50:44 dclunie Exp $";
	
	// package scope ... applications use LocalizerPosterFactory
	ProjectSlice() {
	}

	public Vector getOutlineOnLocalizerForThisGeometry(
			Vector3d row,Vector3d column,Point3d tlhc,Tuple3d voxelSpacing,double sliceThickness,Tuple3d dimensions) {
			
		Point3d[] sourceCorners = getCornersOfSourceRectangleInSourceSpace(row,column,tlhc,voxelSpacing,dimensions);
		Vector shapes = new Vector(5);
		Point2D.Double firstPoint = null;
		Point2D.Double lastPoint = null;
		Point2D.Double thisPoint = null;
		for (int i=0; i<4; ++i) {

			// We want to view the source slice from the "point of view" of
			// the target localizer, i.e. a parallel projection of the source
			// onto the target.

			// Do this by imagining that the target localizer is a view port
			// into a relocated and rotated co-ordinate space, where the
			// viewport has a row vector of +X, col vector of +Y and normal +Z,
			// then the X and Y values of the projected target correspond to
			// column and row offsets in mm from the TLHC of the localizer image.

			Point3d point = transformPointFromSourceSpaceIntoLocalizerSpace(sourceCorners[i]);
			lastPoint = thisPoint;
			thisPoint = transformPointInLocalizerPlaneIntoImageSpace(point);	// Get the x and y (and ignore the z) values as the offset in the target image
			if (i == 0) {
				firstPoint=thisPoint;
			}
			else {
				shapes.add(new Line2D.Double(lastPoint,thisPoint));
			}
		}
		shapes.add(new Line2D.Double(thisPoint,firstPoint));	// close the polygon
		
		return shapes;
	}
}

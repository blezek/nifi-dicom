/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.geometry;

import java.util.Vector;
import java.awt.Shape;
import javax.vecmath.*;

/**
 * @author	dclunie
 */
class IntersectVolume extends LocalizerPoster {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/IntersectVolume.java,v 1.11 2017/01/24 10:50:43 dclunie Exp $";
	
	// package scope ... applications use LocalizerPosterFactory
	IntersectVolume() {
	}
	
	public Vector getOutlineOnLocalizerForThisGeometry(
			Vector3d row,Vector3d column,Point3d tlhc,Tuple3d voxelSpacing,double sliceThickness,Tuple3d dimensions) {
//System.err.println("IntersectVolume.getOutlineOnLocalizerForThisGeometry()");
		Point3d[] corners = getCornersOfSourceCubeInSourceSpace(row,column,tlhc,voxelSpacing,sliceThickness,dimensions);
		for (int i=0; i<8; ++i) {
			// We want to consider each edge of the source slice with respect to
			// the plane of the target localizer, so transform the source corners
			// into the target localizer space, and then see which edges cross
			// the Z plane of the localizer 

			corners[i] = transformPointFromSourceSpaceIntoLocalizerSpace(corners[i]);
			
			// Now, points with a Z value of zero are in the plane of the localizer plane
			// Edges with one Z value +ve (or 0) and the other -ve (or 0) cross (or touch) the localizer plane
			// Edges with both Z values +ve or both -ve don't cross the localizer plane
		}
		Vector intersections = getIntersectionsOfCubeWithZPlane(corners);
		
		return intersections.size() > 0 ? drawOutlineOnLocalizer(intersections) : null;
	}
}

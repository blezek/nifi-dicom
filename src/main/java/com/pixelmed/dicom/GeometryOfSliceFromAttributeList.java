/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.vecmath.*;
import com.pixelmed.geometry.*;

/**
 * <p>A class to extract and describe the spatial geometry of a single cross-sectional image slice, given a list of DICOM attributes.</p>
 *
 * @author	dclunie
 */
public class GeometryOfSliceFromAttributeList extends GeometryOfSlice {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/GeometryOfSliceFromAttributeList.java,v 1.13 2017/01/24 10:50:37 dclunie Exp $";

	/**
	 * <p>Construct the geometry from the Image Plane Module and related attributes.</p>
	 *
	 * @param		list			the list of DICOM attributes
	 * @throws	DicomException	if any of the required attributes are missing or incorrectly formed
	 */
	public GeometryOfSliceFromAttributeList(AttributeList list) throws DicomException {
	
		                  tlhcArray = Attribute.getDoubleValues(list,TagFromName.ImagePositionPatient);
			    
		double [] pixelSpacingArray = Attribute.getDoubleValues(list,TagFromName.PixelSpacing);
		          voxelSpacingArray = new double[3];
		       voxelSpacingArray[0] = pixelSpacingArray[0];	// row spacing
		       voxelSpacingArray[1] = pixelSpacingArray[1];	// column spacing
		       voxelSpacingArray[2] = 0;	// set later by checkAndSetVolumeSampledRegularlyAlongFrameDimension() IFF a volume and in GeometryOfVolume
		             sliceThickness = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.SliceThickness,0.0);

		double[]        orientation = Attribute.getDoubleValues(list,TagFromName.ImageOrientationPatient);
		                   rowArray = new double[3];    rowArray[0]=orientation[0];    rowArray[1]=orientation[1];    rowArray[2]=orientation[2];
		                columnArray = new double[3]; columnArray[0]=orientation[3]; columnArray[1]=orientation[4]; columnArray[2]=orientation[5];

		double[]         dimensions = new double[3];
		              dimensions[0] = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);
		              dimensions[1] = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		              dimensions[2] = 1;
		
		                        row = new Vector3d(rowArray);
		                     column = new Vector3d(columnArray);
			               tlhc = new Point3d(tlhcArray);
			       voxelSpacing = new Vector3d(voxelSpacingArray);
		            this.dimensions = new Vector3d(dimensions);
					      makeNormal();
	}
}

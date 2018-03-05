/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.geometry;

import javax.vecmath.*;

/**
 * <p>A class to describe the spatial geometry of a single cross-sectional image slice.</p>
 *
 * <p>The 3D coordinate space used is the DICOM coordinate space, which is LPH+, that
 * is, the x-axis is increasing to the left hand side of the patient, the y-axis is
 * increasing to the posterior side of the patient, and the z-axis is increasing toward
 * the head of the patient.</p>
 *
 * @author	dclunie
 */
public class GeometryOfSlice {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/GeometryOfSlice.java,v 1.25 2017/01/24 10:50:43 dclunie Exp $";

	/***/
	protected double[] rowArray;
	/***/
	protected Vector3d row;
	
	/***/
	protected double[] columnArray;
	/***/
	protected Vector3d column;
	
	/***/
	protected Point3d tlhc;
	/***/
	protected double[] tlhcArray;
	
	/***/
	protected Tuple3d voxelSpacing;		// row spacing (between centers of adjacent rows), then column spacing, then slice spacing
	/***/
	protected double[] voxelSpacingArray;
	
	/***/
	protected double sliceThickness;

	/***/
	protected Tuple3d dimensions;		// number of rows, then number of columns, then number of slices

	/***/
	protected Vector3d normal;
	/***/
	protected double[] normalArray;
	
	protected final void makeNormal() {
		normal=new Vector3d();
		normal.cross(row,column);
		normal.normalize();
		normalArray=new double[3];
		normal.get(normalArray);
		normalArray[2]=normalArray[2]*-1;
		normal=new Vector3d(normalArray);
	}

	/**
	 * <p>Construct an empty geometry.</p>
	 *
	 */
	public GeometryOfSlice() {
	}

	/**
	 * <p>Construct the geometry.</p>
	 *
	 * @param	row			the direction of the row as X, Y and Z components (direction cosines, unit vector) LPH+
	 * @param	column			the direction of the column as X, Y and Z components (direction cosines, unit vector) LPH+
	 * @param	tlhc			the position of the top left hand corner of the slice as a point (X, Y and Z) LPH+
	 * @param	voxelSpacing		the row and column spacing and, if a volume, the slice interval (spacing between the centers of parallel slices) in mm
	 * @param	sliceThickness		the slice thickness in mm
	 * @param	dimensions		the row and column dimensions and 1 for the third dimension
	 */
	public GeometryOfSlice(Vector3d row,Vector3d column,Point3d tlhc,Tuple3d voxelSpacing,double sliceThickness,Tuple3d dimensions) {
		this.row=row;
		rowArray=new double[3];
		row.get(rowArray);
		this.column=column;
		columnArray=new double[3];
		column.get(columnArray);
		this.tlhc=tlhc;
		tlhcArray=new double[3];
		tlhc.get(tlhcArray);
		this.voxelSpacing=voxelSpacing;
		voxelSpacingArray=new double[3];
		voxelSpacing.get(voxelSpacingArray);
		this.sliceThickness=sliceThickness;
		this.dimensions=dimensions;
		makeNormal();
	}

	/**
	 * <p>Construct the geometry.</p>
	 *
	 * @param	rowArray		the direction of the row as X, Y and Z components (direction cosines, unit vector) LPH+
	 * @param	columnArray		the direction of the column as X, Y and Z components (direction cosines, unit vector) LPH+
	 * @param	tlhcArray		the position of the top left hand corner of the slice as a point (X, Y and Z) LPH+
	 * @param	voxelSpacingArray	the row and column spacing and, if a volume, the slice interval (spacing between the centers of parallel slices) in mm
	 * @param	sliceThickness		the slice thickness in mm
	 * @param	dimensions		the row and column dimensions and 1 for the third dimension
	 */
	public GeometryOfSlice(double[] rowArray,double[] columnArray,double[] tlhcArray,double[] voxelSpacingArray,double sliceThickness,double[] dimensions) {
		this.rowArray=rowArray;
		this.row=new Vector3d(rowArray);
		this.columnArray=columnArray;
		this.column=new Vector3d(columnArray);
		this.tlhcArray=tlhcArray;
		this.tlhc=new Point3d(tlhcArray);
		this.voxelSpacingArray=voxelSpacingArray;
		this.voxelSpacing=new Vector3d(voxelSpacingArray);
		this.sliceThickness=sliceThickness;
		this.dimensions=new Vector3d(dimensions);
		makeNormal();
	}

	/**
	 * <p>Get the row direction.</p>
	 *
	 * @return	the direction of the row as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final Vector3d getRow()		{ return row; }
	
	/**
	 * <p>Get the row direction.</p>
	 *
	 * @return	the direction of the row as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final double[] getRowArray()	{ return rowArray; }
	
	/**
	 * <p>Get the column direction.</p>
	 *
	 * @return	the direction of the column as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final Vector3d getColumn()	{ return column; }
	
	/**
	 * <p>Get the column direction.</p>
	 *
	 * @return	the direction of the column as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final double[] getColumnArray()	{ return columnArray; }
	
	/**
	 * <p>Get the normal direction.</p>
	 *
	 * @return	the direction of the normal to the plane of the slices, as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final Vector3d getNormal()	{ return normal; }
	
	/**
	 * <p>Get the normal direction.</p>
	 *
	 * @return	the direction of the normal to the plane of the slices, as X, Y and Z components (direction cosines, unit vector) LPH+
	 */
	public final double[] getNormalArray()	{ return normalArray; }
	
	/**
	 * <p>Get the position of the top left hand corner.</p>
	 *
	 * @return	the position of the top left hand corner of the slice as a point (X, Y and Z) LPH+
	 */
	public final Point3d getTLHC()		{ return tlhc; }
	
	/**
	 * <p>Get the position of the top left hand corner.</p>
	 *
	 * @return	the position of the top left hand corner of the slice as a point (X, Y and Z) LPH+
	 */
	public final double[] getTLHCArray()	{ return tlhcArray; }
	
	/**
	 * <p>Get the spacing between centers of the voxel in three dimension.</p>
	 *
	 * @return	the row and column spacing and, if a volume, the slice interval (spacing between the centers of parallel slices) in mm
	 */
	public final Tuple3d getVoxelSpacing()	{ return voxelSpacing; }
	
	/**
	 * <p>Get the spacing between centers of the voxel in three dimension.</p>
	 *
	 * @return	the row and column spacing and, if a volume, the slice interval (spacing between the centers of parallel slices) in mm
	 */
	public final double[] getVoxelSpacingArray()	{ return voxelSpacingArray; }
	
	/**
	 * <p>Get the spacing between centers of the voxel in three dimension.</p>
	 *
	 * @return	the slice thickness in mm
	 */
	public final double getSliceThickness()	{ return sliceThickness; }
	
	/**
	 * <p>Get the dimensions of the voxel.</p>
	 *
	 * @return	the row and column dimensions and 1 for the third dimension
	 */
	public final Tuple3d getDimensions()	{ return dimensions; }
	
	/**
	 * <p>Set the third value of voxel spacing.</p>
	 *
	 * <p>Package scope - used only by GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension().</p>
	 *
	 * @param	spacing	the spacing between frames (slices)
	 */
	final void setVoxelSpacingBetweenSlices(double spacing) {
		voxelSpacingArray[2]=spacing;
		voxelSpacing.set(voxelSpacingArray);
	}

	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	column	the offset along the column from the top left hand corner, zero being no offset
	 * @param	row	the offset along the row from the top left hand corner, zero being no offset
	 * @return		the x, y and z location in 3D space
	 */
	public final double[] lookupImageCoordinate(int column,int row) {
		return lookupImageCoordinate((double)column,(double)row);
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	location	an array in which to return the x, y and z location in 3D space
	 * @param	column		the offset along the column from the top left hand corner, zero being no offset
	 * @param	row		the offset along the row from the top left hand corner, zero being no offset
	 */
	public final void lookupImageCoordinate(double[] location,int column,int row) {
		lookupImageCoordinate(location,(double)column,(double)row);
	}

	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	column	the offset along the column from the top left hand corner, zero being no offset
	 * @param	row	the offset along the row from the top left hand corner, zero being no offset
	 * @return		the x, y and z location in 3D space
	 */
	public final double[] lookupImageCoordinate(double column,double row) {
		double[] location = new double[3];
		lookupImageCoordinate(location,column,row);
		return location;
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	location	an array in which to return the x, y and z location in 3D space
	 * @param	column		the offset along the column from the top left hand corner, zero being no offset
	 * @param	row		the offset along the row from the top left hand corner, zero being no offset
	 */
	public final void lookupImageCoordinate(double[] location,double column,double row) {
		// the row is how far we are down the column direction
		// the column is how far we are down the row direction
		row    = row    - 0.5;	// account for sub-pixel resolution per DICOM PS 3.3 Figure C.10.5-1
		column = column - 0.5;
		location[0] = tlhcArray[0] + row*columnArray[0]*voxelSpacingArray[0]/*between rows*/ + column*rowArray[0]*voxelSpacingArray[1]/*between cols*/;
		location[1] = tlhcArray[1] + row*columnArray[1]*voxelSpacingArray[0]/*between rows*/ + column*rowArray[1]*voxelSpacingArray[1]/*between cols*/;
		location[2] = tlhcArray[2] + row*columnArray[2]*voxelSpacingArray[0]/*between rows*/ + column*rowArray[2]*voxelSpacingArray[1]/*between cols*/;
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in x,y and z coordinates of the point in the DICOM 3D coordinate space, and
	 * return the image coordinates (column and row offset).</p>
	 *
	 * @param	location	the x, y and z location in 3D space
	 * @return			the column and row offsets from the top left hand corner of the image
	 */
	public final double[] lookupImageCoordinate(double[] location) {
		double[] offsets = new double[2];
		lookupImageCoordinate(offsets,location);
		return offsets;
	}
	
	protected int R = -1;			// the x, y or z row vector component with the largest magnitude for use in recovering 2D from 3D locations
	protected int C = -1;			// the x, y or z col vector component with the largest magnitude for use in recovering 2D from 3D locations
	
	protected final void findMaxComponents() {
		double[] rowAbs = new double[3];
		double[] columnAbs = new double[3];
		for (int i=0; i<3; ++i) {
			rowAbs[i] = Math.abs(rowArray[i]);
			columnAbs[i] = Math.abs(columnArray[i]);
		}
		double rowAbsSoFar = -1;
		double columnAbsSoFar = -1;
		for (int i=0; i<3; ++i) {
			if (rowAbs[i] > rowAbsSoFar) {
				rowAbsSoFar = rowAbs[i];
				R = i;
			}
			if (columnAbs[i] > columnAbsSoFar) {
				columnAbsSoFar = columnAbs[i];
				C = i;
			}
		}
//System.err.println("maxRowIndex = "+R);
//System.err.println("maxColumnIndex = "+C);
	}

	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in x,y and z coordinates of the point in the DICOM 3D coordinate space, and
	 * return the image coordinates (column and row offset).</p>
	 *
	 * @param	offsets		an array in which to return the column and row offsets from the top left hand corner of the image
	 * @param	location	the x, y and z location in 3D space
	 */
	public final void lookupImageCoordinate(double offsets[],double[] location) {
		if (R == -1 || C == -1) {
			findMaxComponents();
		}
		double scaledrow = ((location[R] - tlhcArray[R])*rowArray[C]/rowArray[R] - location[C] + tlhcArray[C])/(columnArray[R]/rowArray[R]*rowArray[C] - columnArray[C]);
		double scaledcolumn = (location[R] - tlhcArray[R] - scaledrow*columnArray[R]) / rowArray[R];
//System.err.println("scaledrow = "+scaledrow);
//System.err.println("scaledcolumn = "+scaledcolumn);
		
		double row = scaledrow / voxelSpacingArray[0];
		double column = scaledcolumn / voxelSpacingArray[1];
//System.err.println("row = "+row);
//System.err.println("column = "+column);
		
		offsets[0]=column + 0.5;	// account for sub-pixel resolution per DICOM PS 3.3 Figure C.10.5-1
		offsets[1]=row + 0.5;
	}
	
	/**
	 * <p>Given the present geometry, determine the distance along the normal
	 * to the plane of the slice of an arbitrary point (not necessarily in the plane of the image)
	 * from the origin of
	 * the coordinate space (0,0,0).</p>
	 *
	 * @param	point	the point
	 * @return			the distance of the point from the origin along the normal axis
	 */
	public final double getDistanceAlongNormalFromOrigin(Point3d point) {
		Vector3d fromOrigin=new Vector3d(point);
		double distance = normal.dot(fromOrigin);
//System.err.println("GeometryOfSlice.getDistanceAlongNormalFromOrigin(Point3d): distance = "+distance);
		return distance;
	}
	
	/**
	 * <p>Given the present geometry, determine the distance along the normal
	 * to the plane of the slice of the TLHC of the image from the origin of
	 * the coordinate space (0,0,0).</p>
	 *
	 * @return	the distance of the TLHC from the origin along the normal axis
	 */
	public final double getDistanceAlongNormalFromOrigin() {
		Vector3d fromOrigin=new Vector3d(tlhc);
		double distance = normal.dot(fromOrigin);
//System.err.println("GeometryOfSlice.getDistanceAlongNormalFromOrigin(): distance = "+distance);
		return distance;
	}
	
	/**
	 * <p>Is an arbitrary point in the DICOM 3D coordinate space within the plane of the image.</p>
	 * 
	 * <p>Slice thickness is not considered, only floating point rounding precision tolerance is permitted.</p>
	 *
	 * @param	point	the point
	 * @return			true if within the plane of the image
	 */
	public final boolean isPointInSlicePlane(Point3d point) {
		double distancePoint = getDistanceAlongNormalFromOrigin(point);
		double distanceTLHC  = getDistanceAlongNormalFromOrigin();
		double delta = Math.abs(distancePoint-distanceTLHC);
		boolean inplane = delta < .001;
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): distancePoint = "+distancePoint);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): distanceTLHC = "+distanceTLHC);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): delta = "+delta);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): inplane = "+inplane);
		return inplane;
	}
	
	/**
	 * <p>Is an arbitrary point in the DICOM 3D coordinate space within the plane of the image.</p>
	 * 
	 * <p>Slice thickness is not considered, only floating point rounding precision tolerance is permitted.</p>
	 *
	 * @param	location	the point
	 * @return				true if within the plane of the image
	 */
	public final boolean isPointInSlicePlane(double[] location) {
		Point3d point = new Point3d(location);
		double distancePoint = getDistanceAlongNormalFromOrigin(point);
		double distanceTLHC  = getDistanceAlongNormalFromOrigin();
		double delta = Math.abs(distancePoint-distanceTLHC);
		boolean inplane = delta < .001;
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): distancePoint = "+distancePoint);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): distanceTLHC = "+distanceTLHC);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): delta = "+delta);
//System.err.println("GeometryOfSlice.isPointInSlicePlane(): inplane = "+inplane);
		return inplane;
	}
	
	/**
	 * <p>Determine if two slices are parallel.</p>
	 *
	 * @param	slice1	the geometry of one slice
	 * @param	slice2	the geometry of the other slice
	 * @return			true if slices are parallel
	 */
	public static final boolean areSlicesParallel(GeometryOfSlice slice1,GeometryOfSlice slice2) {
		boolean parallel = false;
		if (slice1 != null && slice2 != null) {
			double[] normal1 = slice1.getNormalArray();
			double[] normal2 = slice2.getNormalArray();
			if (normal1 != null && normal2 != null) {
//System.err.println("GeometryOfSlice.areSlicesParallel() compare normal: ("+normal1[0]+","+normal1[1]+","+normal1[2]+")");
//System.err.println("GeometryOfSlice.areSlicesParallel() with normal:    ("+normal2[0]+","+normal2[1]+","+normal2[2]+")");
						parallel =
				    Math.abs(normal1[0]-normal2[0]) < 0.001
				 && Math.abs(normal1[1]-normal2[1]) < 0.001
				 && Math.abs(normal1[2]-normal2[2]) < 0.001;
//System.err.println("GeometryOfSlice.areSlicesParallel() parallel="+parallel);
			}
		}
		return parallel;
	}
	
	/**
	 * <p>Get a human-readable rendering of the geometry.</p>
	 *
	 * @return	the string rendering of the geometry
	 */
	public final String toString() {
		double[] dimensionsArray=new double[3];
		dimensions.get(dimensionsArray);
		
		StringBuffer str = new StringBuffer();
		str.append("Row (");
		str.append(rowArray[0]);
		str.append(",");
		str.append(rowArray[1]);
		str.append(",");
		str.append(rowArray[2]);
		str.append(") Column (");
		str.append(columnArray[0]);
		str.append(",");
		str.append(columnArray[1]);
		str.append(",");
		str.append(columnArray[2]);
		str.append(") Normal (");
		str.append(normalArray[0]);
		str.append(",");
		str.append(normalArray[1]);
		str.append(",");
		str.append(normalArray[2]);
		str.append(") TLHC (");
		str.append(tlhcArray[0]);
		str.append(",");
		str.append(tlhcArray[1]);
		str.append(",");
		str.append(tlhcArray[2]);
		str.append(") Spacing (");
		str.append(voxelSpacingArray[0]);
		str.append(",");
		str.append(voxelSpacingArray[1]);
		str.append(",");
		str.append(voxelSpacingArray[2]);
		str.append(") Thickness (");
		str.append(sliceThickness);
		str.append(") Dimensions (");
		str.append(dimensionsArray[0]);
		str.append(",");
		str.append(dimensionsArray[1]);
		str.append(",");
		str.append(dimensionsArray[2]);
		str.append(")");
		//str.append(" Distance of TLHC from Origin along Normal ");
		//str.append(getDistanceAlongNormalFromOrigin());
		return str.toString();
	}

	/**
	 * <p>Get the letter representation of the orientation of a vector.</p>
	 *
	 * <p>For bipeds, L or R, A or P, H or F.</p>
	 *
	 * <p>For quadrupeds, Le or Rt, V or D, Cr or Cd (with lower case; use toUpperCase() to produce valid CodeString for PatientOrientation).</p>
	 *
	 * @param	orientation	the orientation
	 * @param	quadruped	true if subject is a quadruped rather than a biped
	 * @return				a string rendering of the orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public static final String getOrientation(double orientation[],boolean quadruped) {
		StringBuffer strbuf = new StringBuffer();
		if (orientation != null && orientation.length == 3) {
			String orientationX = orientation[0] < 0 ? (quadruped ? "Rt" : "R") : (quadruped ? "Le" : "L");
			String orientationY = orientation[1] < 0 ? (quadruped ? "V" : "A") : (quadruped ? "D" : "P");
			String orientationZ = orientation[2] < 0 ? (quadruped ? "Cd" : "F") : (quadruped ? "Cr" : "H");
			
			double absX = Math.abs(orientation[0]);
			double absY = Math.abs(orientation[1]);
			double absZ = Math.abs(orientation[2]);
			for (int i=0; i<3; ++i) {
				if (absX > 0.0001 && absX>absY && absX>absZ) {
					strbuf.append(orientationX);
					absX=0;
				}
				else if (absY > 0.0001 && absY>absX && absY>absZ) {
					strbuf.append(orientationY);
					absY=0;
				}
				else if (absZ > 0.0001 && absZ>absX && absZ>absY) {
					strbuf.append(orientationZ);
					absZ=0;
				}
			}
		}
		return strbuf.toString();
	}
	/**
	 * <p>Get the letter representation of the orientation of a vector.</p>
	 *
	 * <p>Assumes a biped rather than a quadruped, so returns L or R, A or P, H or F.</p>
	 *
	 * @param	orientation	the orientation
	 * @return				a string rendering of the orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public static final String getOrientation(double orientation[]) {
		return getOrientation(orientation,false/*quadruped*/);
	}
	
	/**
	 * <p>Get the letter representation of the orientation of the rows of this slice.</p>
	 *
	 * <p>For bipeds, L or R, A or P, H or F.</p>
	 *
	 * <p>For quadrupeds, Le or Rt, V or D, Cr or Cd (with lower case; use toUpperCase() to produce valid CodeString for PatientOrientation).</p>
	 *
	 * @param	quadruped	true if subject is a quadruped rather than a biped
	 * @return				a string rendering of the row orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getRowOrientation(boolean quadruped) {
		return getOrientation(rowArray,quadruped);
	}

	/**
	 * <p>Get the letter representation of the orientation of the columns of this slice.</p>
	 *
	 * <p>For bipeds, L or R, A or P, H or F.</p>
	 *
	 * <p>For quadrupeds, Le or Rt, V or D, Cr or Cd (with lower case; use toUpperCase() to produce valid CodeString for PatientOrientation).</p>
	 *
	 * @param	quadruped	true if subject is a quadruped rather than a biped
	 * @return				a string rendering of the column orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getColumnOrientation(boolean quadruped) {
		return getOrientation(columnArray,quadruped);
	}

	/**
	 * <p>Get the letter representation of the orientation of the rows of this slice.</p>
	 *
	 * <p>Assumes a biped rather than a quadruped, so returns L or R, A or P, H or F.</p>
	 *
	 * @return				a string rendering of the row orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getRowOrientation() {
		return getRowOrientation(false/*quadruped*/);
	}

	/**
	 * <p>Get the letter representation of the orientation of the columns of this slice.</p>
	 *
	 * <p>Assumes a biped rather than a quadruped, so returns L or R, A or P, H or F.</p>
	 *
	 * @return				a string rendering of the column orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getColumnOrientation() {
		return getColumnOrientation(false/*quadruped*/);
	}
}

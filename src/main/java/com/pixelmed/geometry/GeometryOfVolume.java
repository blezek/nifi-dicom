/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.geometry;

import javax.vecmath.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to describe the spatial geometry of an entire volume of contiguous cross-sectional image slices.</p>
 *
 * <p>The 3D coordinate space used is the DICOM coordinate space, which is LPH+, that
 * is, the x-axis is increasing to the left hand side of the patient, the y-axis is
 * increasing to the posterior side of the patient, and the z-axis is increasing toward
 * the head of the patient.</p>
 *
 * @author	dclunie
 */
public class GeometryOfVolume {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/geometry/GeometryOfVolume.java,v 1.21 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(GeometryOfVolume.class);

	/***/
	protected GeometryOfSlice[] frames;
		
	protected GeometryOfVolume() {
	}
		
	public GeometryOfVolume(GeometryOfSlice[] frames) {
		this.frames = frames;
	}

	/**
	 * <p>Get the number of slices.</p>
	 *
	 * @return	the number of slices
	 */
	public final int getNumberOfSlices() { return frames == null ? 0 : frames.length; }
	
	/**
	 * <p>Get the geometry of the slices.</p>
	 *
	 * @return	an array of the geometry of the slices
	 */
	public final GeometryOfSlice[] getGeometryOfSlices() { return frames; }
	
	/**
	 * <p>Get the geometry of the selected slice.</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return	the geometry of the selected slice
	 */
	public final GeometryOfSlice getGeometryOfSlice(int frame) { return frames != null && frame >= 0 && frame < frames.length ? frames[frame] : null; }
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row and frame offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	column	the offset along the column from the top left hand corner, zero being no offset
	 * @param	row	the offset along the row from the top left hand corner, zero being no offset
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return		the x, y and z location in 3D space
	 */
	public final double[] lookupImageCoordinate(int column,int row,int frame) {
		return lookupImageCoordinate((double)column,(double)row,frame);
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row and frame offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	location	an array in which to return the x, y and z location in 3D space
	 * @param	column		the offset along the column from the top left hand corner, zero being no offset
	 * @param	row		the offset along the row from the top left hand corner, zero being no offset
	 * @param	frame		the offset along the frames from first frame, zero being no offset
	 */
	public final void lookupImageCoordinate(double[] location,int column,int row,int frame) {
		lookupImageCoordinate(location,(double)column,(double)row,frame);
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row and frame offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	column	the offset along the column from the top left hand corner, zero being no offset
	 * @param	row	the offset along the row from the top left hand corner, zero being no offset
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return		the x, y and z location in 3D space
	 */
	public final double[] lookupImageCoordinate(double column,double row,int frame) {
		double[] location = null;
		if (frames != null && frame < frames.length && frames[frame] != null) {
			location = frames[frame].lookupImageCoordinate(column,row);
		}
		return location;
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in image coordinates (column and row and frame offset) and return the x,y and z
	 * coordinates of the point in the DICOM 3D coordinate space.</p>
	 *
	 * @param	location	an array in which to return the x, y and z location in 3D space
	 * @param	column		the offset along the column from the top left hand corner, zero being no offset
	 * @param	row		the offset along the row from the top left hand corner, zero being no offset
	 * @param	frame		the offset along the frames from first frame, zero being no offset
	 */
	public final void lookupImageCoordinate(double[] location,double column,double row,int frame) {
		if (frames != null && frame < frames.length && frames[frame] != null) {
			frames[frame].lookupImageCoordinate(location,column,row);
		}
		else {
			location[0]=0; location[1]=0; location[2]=0;
		}
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in x,y and z coordinates of the point in the DICOM 3D coordinate space, and
	 * return the volume coordinates (column and row and frame offset).</p>
	 *
	 * @param	location	the x, y and z location in 3D space
	 * @return				the column and row and frame offsets from the top left hand corner of the volume (or NaN if not a regularly sampled volume)
	 */
	public final double[] lookupImageCoordinate(double[] location) {
		double[] offsets = new double[3];
		lookupImageCoordinate(offsets,location);
		return offsets;
	}
	
	protected int R = -1;			// the x, y or z row vector component with the largest magnitude for use in recovering 2D from 3D locations
	protected int C = -1;			// the x, y or z col vector component with the largest magnitude for use in recovering 2D from 3D locations
	protected int N = -1;			// the x, y or z col vector component with the largest magnitude for use in recovering 2D from 3D locations
	
	protected final void findMaxComponents(double[] rowArray,double[] columnArray,double[] normalArray) {
		double[] rowAbs = new double[3];
		double[] columnAbs = new double[3];
		double[] normalAbs = new double[3];
		for (int i=0; i<3; ++i) {
			rowAbs[i] = Math.abs(rowArray[i]);
			columnAbs[i] = Math.abs(columnArray[i]);
			normalAbs[i] = Math.abs(normalArray[i]);
		}
		double rowAbsSoFar = -1;
		double columnAbsSoFar = -1;
		double normalAbsSoFar = -1;
		for (int i=0; i<3; ++i) {
			if (rowAbs[i] > rowAbsSoFar) {
				rowAbsSoFar = rowAbs[i];
				R = i;
			}
			if (columnAbs[i] > columnAbsSoFar) {
				columnAbsSoFar = columnAbs[i];
				C = i;
			}
			if (normalAbs[i] > normalAbsSoFar) {
				normalAbsSoFar = normalAbs[i];
				N = i;
			}
		}
		slf4jlogger.debug("maxRowIndex = {}",R);
		slf4jlogger.debug("maxColumnIndex = {}",C);
		slf4jlogger.debug("maxNormalIndex = {}",N);
	}
	
	/**
	 * <p>Given the present geometry, look up the location of a point specified
	 * in x,y and z coordinates of the point in the DICOM 3D coordinate space, and
	 * return the volume coordinates (column and row and frame offset).</p>
	 *
	 * @param	offsets		an array in which to return the column and row and frame offsets from the top left hand corner of the volume (or NaN if not a regularly sampled volume)
	 * @param	location	the x, y and z location in 3D space
	 */
	public final void lookupImageCoordinate(double offsets[],double[] location) {
		if (isVolume) {
			double[]          rowArray = frames[0].getRowArray();
			double[]       columnArray = frames[0].getColumnArray();
			double[]       normalArray = frames[0].getNormalArray();
			double[]         tlhcArray = frames[0].getTLHCArray();
			double[] voxelSpacingArray = frames[0].getVoxelSpacingArray();

			if (R == -1 || C == -1 || N == -1) {
				findMaxComponents(rowArray,columnArray,normalArray);
			}
			
			// use matrix inversion to solve 3 linear equations with 3 unknowns
			
			Matrix3d matrix = new Matrix3d(
				rowArray[R],columnArray[R],normalArray[R],
				rowArray[C],columnArray[C],normalArray[C],
				rowArray[N],columnArray[N],normalArray[N]);
				
			matrix.invert();
				
			Vector3d vector = new Vector3d(
				location[R] - tlhcArray[R],
				location[C] - tlhcArray[C],
				location[N] - tlhcArray[N]);
				
			matrix.transform(vector);
			
			vector.get(offsets);
			
			offsets[0] /= voxelSpacingArray[0];
			offsets[1] /= voxelSpacingArray[1];
			offsets[2] /= voxelSpacingArray[2];
		
			offsets[0] += 0.5;	// account for sub-pixel resolution per DICOM PS 3.3 Figure C.10.5-1
			offsets[1] += 0.5;
		}
		else {
			slf4jlogger.warn("Cannot look up 2D coordinate from 3D coordinate if not regularly sampled volume");
			offsets[0] = Double.NaN;
			offsets[1] = Double.NaN;
			offsets[2] = Double.NaN;
		}
	}
	
	/**
	 * <p>Find the slice in the our geometry that is closest to the supplied slice geometry.</p>
	 *
	 * <p>Specifically, the shortest distance along the normal to the plane of the common
	 * orientation is chosen.</p>
	 *
	 * @param	otherSlice	the geometry of the slice to match
	 * @return			the index of the closest frame in this volume (numbered from 0), or -1 if something goes wrong
	 */
	
	public final int findClosestSliceInSamePlane(GeometryOfSlice otherSlice) {
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane():");
		double otherDistance = otherSlice.getDistanceAlongNormalFromOrigin();
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): otherDistance = "+otherDistance);
		int found=-1;
		double closest = 999999999;
		for (int i=0; i<frames.length; ++i) {
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): testing "+i);
			double distanceThisFrame = frames[i].getDistanceAlongNormalFromOrigin();
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): distanceThisFrame = "+distanceThisFrame);
			double distance = Math.abs(distanceThisFrame-otherDistance);
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): difference in distance = "+distance);
			if (distance < closest) {
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): closer");
				closest=distance;
				found=i;
			}
		}
//System.err.println("GeometryOfVolume.findClosestSliceInSamePlane(): found = "+found+" with closest distance difference of "+closest);
		return found;
	}

	
	/**
	 * <p>Given the present geometry, determine the distances along the normal
	 * to the plane of the slices of the TLHC of each slice from the origin of
	 * the coordinate space (0,0,0).</p>
	 *
	 * @return	an array of the distances of the TLHCs from the origin along the normal axis
	 */
	public final double[] getDistanceAlongNormalFromOrigin() {
		double[] distances = new double[frames.length];
		for (int i=0; i<frames.length; ++i) {
			distances[i]=frames[i].getDistanceAlongNormalFromOrigin();
		}
		return distances;
	}
	
	
	/***/
	protected boolean areParallel;
	
	/**
	 * <p>Are all the frames in the set of frames parallel ?</p>
	 *
	 * @return	true if all frames have the same orientation
	 */
	public final boolean areAllSlicesParallel() { return areParallel; }
	
	/***/
	protected boolean isVolume;
	
	/**
	 * <p>Is the set of frames regularly sampled along the frame dimension ?</p>
	 *
	 * @return	true if same spacing between centers of frames and position monotonically increasing
	 */
	public final boolean isVolumeSampledRegularlyAlongFrameDimension() { return isVolume; }
	
	/**
	 * <p>Check if the set of frames regularly sampled along the frame dimension.</p>
	 *
	 * <p>Method is public only to make it accessible from constructors in other packages.</p>
	 *
	 */
	public final void checkAndSetVolumeSampledRegularlyAlongFrameDimension() {
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension():");
		areParallel = true;
		if (frames != null && frames.length > 1) {
			// check to see if we are actually a volume
			// - more than one slice
			// - slices all parallel
			// - spacing between slices the same
			// - distance along normal to plane of slice monotonically increasing
			
			GeometryOfSlice lastGeometry    = frames[0];
			GeometryOfSlice currentGeometry = frames[1];
			double  lastDistanceAlongNormal = lastGeometry.getDistanceAlongNormalFromOrigin();
			if (GeometryOfSlice.areSlicesParallel(lastGeometry,currentGeometry)) {
				double currentDistanceAlongNormal = currentGeometry.getDistanceAlongNormalFromOrigin();
				double wantIntervalAlongNormal    = currentDistanceAlongNormal - lastDistanceAlongNormal;
				lastDistanceAlongNormal=currentDistanceAlongNormal;
				boolean success=true;
				for (int f=2; f<frames.length && success; ++f) {
					currentGeometry = frames[f];
					if (GeometryOfSlice.areSlicesParallel(lastGeometry,currentGeometry)) {
						currentDistanceAlongNormal = currentGeometry.getDistanceAlongNormalFromOrigin();
						double currentIntervalAlongNormal = currentDistanceAlongNormal - lastDistanceAlongNormal;
						if (Math.abs(currentIntervalAlongNormal-wantIntervalAlongNormal) >= .001) {
							success=false;	// different spacing
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension(): different spacing currentIntervalAlongNormal="+currentIntervalAlongNormal+" wantIntervalAlongNormal="+wantIntervalAlongNormal);
							break;
						}
						lastDistanceAlongNormal=currentDistanceAlongNormal;
					}
					else {
						areParallel = false;
						success=false;
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension(): not parallel");
						break;
					}
				}
				if (success) {
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension(): spacing="+wantIntervalAlongNormal);
					isVolume=true;
					wantIntervalAlongNormal=Math.abs(wantIntervalAlongNormal);	// since sign may be negative and we don't want that
					for (int f=0; f<frames.length; ++f) {
						frames[f].setVoxelSpacingBetweenSlices(wantIntervalAlongNormal);
					}
				}
			}
			else {
				areParallel = false;
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension(): not parallel");
			}
		}
//System.err.println("GeometryOfVolume.checkAndSetVolumeSampledRegularlyAlongFrameDimension(): isVolume="+isVolume);
//System.err.println(toString());
	}

	/**
	 * <p>Get a human-readable rendering of the geometry.</p>
	 *
	 * @return	the string rendering of the geometry
	 */
	public final String toString() {
		StringBuffer str = new StringBuffer();
		for (int f=0; f<frames.length; ++f) {
			str.append("[");
			str.append(f);
			str.append("] ");
			str.append(frames[f].toString());
			str.append("\n");
		}
		return str.toString();
	}

	/**
	 * <p>Get the letter representation of the orientation of the rows of this slice.</p>
	 *
	 * <p>For bipeds, L or R, A or P, H or F.</p>
	 *
	 * <p>For quadrupeds, Le or Rt, V or D, Cr or Cd (with lower case; use toUpperCase() to produce valid CodeString for PatientOrientation).</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @param	quadruped	true if subject is a quadruped rather than a biped
	 * @return	a string rendering of the row orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getRowOrientation(int frame,boolean quadruped) {
		return frames != null && frame < frames.length ? frames[frame].getRowOrientation(quadruped) : "";
	}

	/**
	 * <p>Get the letter representation of the orientation of the columns of this slice.</p>
	 *
	 * <p>For bipeds, L or R, A or P, H or F.</p>
	 *
	 * <p>For quadrupeds, Le or Rt, V or D, Cr or Cd (with lower case; use toUpperCase() to produce valid CodeString for PatientOrientation).</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @param	quadruped	true if subject is a quadruped rather than a biped
	 * @return	a string rendering of the column orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getColumnOrientation(int frame,boolean quadruped) {
		return frames != null && frame < frames.length ? frames[frame].getColumnOrientation(quadruped) : "";
	}

	/**
	 * <p>Get the letter representation of the orientation of the rows of this slice.</p>
	 *
	 * <p>Assumes a biped rather than a quadruped, so returns L or R, A or P, H or F.</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return	a string rendering of the row orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getRowOrientation(int frame) {
		return frames != null && frame < frames.length ? frames[frame].getRowOrientation() : "";
	}

	/**
	 * <p>Get the letter representation of the orientation of the columns of this slice.</p>
	 *
	 * <p>Assumes a biped rather than a quadruped, so returns L or R, A or P, H or F.</p>
	 *
	 * @param	frame	the offset along the frames from first frame, zero being no offset
	 * @return	a string rendering of the column orientation, more than one letter if oblique to the orthogonal axes, or empty string (not null) if fails
	 */
	public final String getColumnOrientation(int frame) {
		return frames != null && frame < frames.length ? frames[frame].getColumnOrientation() : "";
	}
}

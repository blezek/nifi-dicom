/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.geometry.GeometryOfVolume;
import com.pixelmed.geometry.GeometryOfSlice;

/**
 * <p>An class to extract and describe pixel spacing related information.</p>
 *
 * <p>Currently only supports square pixels.</p>
 *
 * @author	dclunie
 */
public class PixelSpacing {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/PixelSpacing.java,v 1.7 2017/01/24 10:50:38 dclunie Exp $";
	
	protected double spacing;
	protected String description;
	
	/**
	 * <p>Get the spacing.</p>
	 *
	 * @return	the spacing
	 */
	public double getSpacing() { return spacing; }
	
	/**
	 * <p>Get the description.</p>
	 *
	 * @return	the description
	 */
	public String getDescription() { return description; }
	
	/**
	 * <p>Extract the appropriate spacing to use for measurements.</p>
	 *
	 * @param	list							where to look for the top level DICOM pixel spacing related attributes
	 * @param	volumeGeometry					if present, where to get the voxel spacing already derived from the DICOM attributes may be null, if not cross-sectional modality)
	 * @param	preferCalibratedValue			if true, in the absence of 3D geometry, and presence of both Pixel Spacing and Imager Pixel Spacing with different values, use the former
	 * @param	useMagnificationFactorIfPresent	if true, and preferCalibratedValue is false and there is no Pixel Spacing, adjust Imager Pixel Spacing based on mag factor, if absent, SID and SOD
	 */
	public PixelSpacing(AttributeList list,GeometryOfVolume volumeGeometry,boolean preferCalibratedValue,boolean useMagnificationFactorIfPresent) {
		boolean found = false;
		if (volumeGeometry != null) {
			GeometryOfSlice[] sliceGeometry = volumeGeometry.getGeometryOfSlices();
			if (sliceGeometry != null && sliceGeometry.length > 0) {
				double[] voxelSpacingArray = sliceGeometry[0].getVoxelSpacingArray();
				if (voxelSpacingArray != null && voxelSpacingArray.length > 2 && voxelSpacingArray[0] == voxelSpacingArray[1]) {
					spacing = voxelSpacingArray[0];
					description = "3D";
					found = true;
				}
			}
		}
		if (!found) {
			spacing = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.ImagerPixelSpacing,0);
			if (spacing > 0) {
				if (useMagnificationFactorIfPresent) {
					double distanceSourceToDetector = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.DistanceSourceToDetector,0.0);
					double distanceSourceToPatient = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.DistanceSourceToPatient,0.0);
					double computedMagnificationFactor = distanceSourceToPatient > 0 ? distanceSourceToDetector/distanceSourceToPatient : 0.0;
					double useMagnificationFactor = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.EstimatedRadiographicMagnificationFactor,computedMagnificationFactor);
					if (useMagnificationFactor > 0) {
						spacing = spacing / useMagnificationFactor;
						description = "magnified";
					}
					else {
						description = "detector";
					}
				}
				else {
					description = "detector";
				}
				found = true;
			}
			else {
				spacing = 0;	// in case was -ve
			}

			if (!found || (preferCalibratedValue && list.get(TagFromName.PixelSpacing) != null)) {
				String calibrationString = Attribute.getSingleStringValueOrDefault(list,TagFromName.PixelSpacingCalibrationType,"calibrated").toLowerCase(java.util.Locale.US);
				calibrationString = Attribute.getSingleStringValueOrDefault(list,TagFromName.PixelSpacingCalibrationDescription,calibrationString).toLowerCase(java.util.Locale.US);
				double oldSpacing = spacing;
				spacing = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.PixelSpacing,0);					// hmmm ... doesn't check for non-square pixels like 3D (volumeGeometry) does ... will use first value :(
				if (spacing > 0) {
					description = spacing == oldSpacing ? "detector" : (found ? calibrationString : "unknown");
					found = true;
				}
				else {
					spacing = 0;	// in case was -ve
					description = null;
				}
			}
		}
	}
	
	/**
	 * <p>Extract the appropriate spacing to use for measurements.</p>
	 *
	 * Will prefer calibrated values, and ignore magnification factors
	 *
	 * @param	list							where to look for the top level DICOM pixel spacing related attributes
	 * @param	volumeGeometry					if present, where to get the voxel spacing already derived from the DICOM attributes may be null, if not cross-sectional modality)
	 */
	public PixelSpacing(AttributeList list,GeometryOfVolume volumeGeometry) {
		this(list,volumeGeometry,true,false);
	}
	
	/**
	 * <p>Extract the appropriate spacing to use for measurements on projection radiographs or non-enhanced family cross-sectional images.</p>
	 *
	 * Will prefer calibrated values, and ignore magnification factors
	 *
	 * @param	list							where to look for the top level DICOM pixel spacing related attributes
	 */
	public PixelSpacing(AttributeList list) {
		this(list,null,true,false);
	}
	
	/**
	 * <p>Get the spacing and the description as a String.</p>
	 *
	 * @return	a string describing the spacing and the description
	 */
	public String toString() {
		return Double.toString(spacing) + " (" + description + ")";
	}
}

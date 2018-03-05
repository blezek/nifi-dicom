/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class that supports matching the geometry of a superimposed image
 * and an underlying images, and creating BufferedImages suitable for
 * drawing on an underlying image.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 *
 * @author	dclunie
 */

public class SuperimposedImage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SuperimposedImage.java,v 1.11 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SuperimposedImage.class);
	
	//public static final double DEFAULT_CLOSEST_SLICE_TOLERANCE_DISTANCE = 1.5d;	// mm
	public static final double DEFAULT_CLOSEST_SLICE_TOLERANCE_DISTANCE = 0.125d;	// mm
	//public static final double DEFAULT_CLOSEST_SLICE_TOLERANCE_DISTANCE = 0.01d;	// mm
	
	// these are protected so as to be accessible to SuperimposedDicomImage or other potential sub-classes for other formats
	protected SourceImage superimposedSourceImage;
	protected GeometryOfVolume superimposedGeometry;
	protected int[] cieLabScaled;
	
	/**
	 * <p>Is the superimposed slice close enough to the underlying slice to superimpose?</p>
	 *
	 * @param	geometryOfSuperimposedSlice
	 * @param	geometryOfUnderlyingSlice
	 * @param	toleranceDistance			difference in distance along normal to orientation for underlying and superimposed frames to be close enough to superimpose, in mm
	 */
	public static boolean isSliceCloseEnoughToSuperimpose(GeometryOfSlice geometryOfSuperimposedSlice,GeometryOfSlice geometryOfUnderlyingSlice,double toleranceDistance) {
		double superimposedDistanceAlongNormal = geometryOfSuperimposedSlice.getDistanceAlongNormalFromOrigin();
		double underlyingDistanceAlongNormal = geometryOfUnderlyingSlice.getDistanceAlongNormalFromOrigin();
		double signedDifference = superimposedDistanceAlongNormal - underlyingDistanceAlongNormal;
		double difference = Math.abs(signedDifference);
		boolean result = difference <= toleranceDistance;
		if (result) {
			slf4jlogger.info("isSliceCloseEnoughToSuperimpose(): distance along normal superimposed = {} underlying = {} difference = {} toleranceDistance = {} is close enough",superimposedDistanceAlongNormal,underlyingDistanceAlongNormal,difference,toleranceDistance);
		}
		else {
//System.err.println("SuperimposedImage.isSliceCloseEnoughToSuperimpose(): distance along normal superimposed = "+superimposedDistanceAlongNormal+" underlying = "+underlyingDistanceAlongNormal+" difference = "+difference+" toleranceDistance = "+toleranceDistance+" is NOT close enough");
//			double[] normal = geometryOfSuperimposedSlice.getNormalArray();
//			double[] correctionRequired = new double[3];
//			correctionRequired[0] = normal[0] * signedDifference;
//			correctionRequired[1] = normal[1] * signedDifference;
//			correctionRequired[2] = normal[2] * signedDifference;
//System.err.println("SuperimposedImage.isSliceCloseEnoughToSuperimpose(): correction to superimposed TLHC required = ("+correctionRequired[0]+","+correctionRequired[1]+","+correctionRequired[2]+")");
		}
		return result;
	}
	
	/**
	 * <p>Is the superimposed slice close enough to the underlying slice to superimpose?</p>
	 *
	 * <p>Assumes a default tolerance factor that is close to zero but allows for floating point rounding error.</p>
	 *
	 * @param	geometryOfSuperimposedSlice
	 * @param	geometryOfUnderlyingSlice
	 */
	public static boolean isSliceCloseEnoughToSuperimpose(GeometryOfSlice geometryOfSuperimposedSlice,GeometryOfSlice geometryOfUnderlyingSlice) {
		return isSliceCloseEnoughToSuperimpose(geometryOfSuperimposedSlice,geometryOfUnderlyingSlice,DEFAULT_CLOSEST_SLICE_TOLERANCE_DISTANCE);
	}
	
	protected SuperimposedImage() {
		this.superimposedSourceImage = null;
		this.superimposedGeometry = null;
	}

	/**
	 * <p>A class that supports matching the geometry of a superimposed image
	 * and a specified underlying image, and creating a BufferedImage suitable for
	 * drawing on that underlying image.</p>
	 */
	public class AppliedToUnderlyingImage {
		private BufferedImage bufferedImage;
		private double columnOrigin;
		private double rowOrigin;
		private GeometryOfSlice geometryOfSuperimposedSlice;
	
		/**
		 * @return	a BufferedImage if a superimposed frame that is close enough can be found, otherwise null
		 */
		public BufferedImage getBufferedImage() { return bufferedImage; }

		public double getColumnOrigin() { return columnOrigin; }

		public double getRowOrigin()    { return rowOrigin; }
	
		/**
		 * @param	underlyingGeometry
		 * @param	underlyingFrame			numbered from 0
		 * @param	toleranceDistance		difference in distance along normal to orientation for underlying and superimposed frames to be close enough to superimpose, in mm
		 */
		private AppliedToUnderlyingImage(GeometryOfVolume underlyingGeometry,int underlyingFrame,double toleranceDistance) {
			bufferedImage = null;
			columnOrigin = 0;
			rowOrigin = 0;
			geometryOfSuperimposedSlice = null;
			
			if (underlyingGeometry != null) {
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): underlyingFrame = "+underlyingFrame);
				GeometryOfSlice geometryOfUnderlyingSlice = underlyingGeometry.getGeometryOfSlice(underlyingFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): geometryOfUnderlyingSlice = "+geometryOfUnderlyingSlice);
				if (geometryOfUnderlyingSlice != null && superimposedGeometry != null) {
					int superimposedFrame = superimposedGeometry.findClosestSliceInSamePlane(geometryOfUnderlyingSlice);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): closest superimposed frame = "+superimposedFrame);
					geometryOfSuperimposedSlice = superimposedGeometry.getGeometryOfSlice(superimposedFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): geometryOfSuperimposedSlice = "+geometryOfSuperimposedSlice);
					// closest slice may not be "close enough", so check that normal distance is (near) zero (e.g., Z positions are the same in the axial case)
					if (isSliceCloseEnoughToSuperimpose(geometryOfSuperimposedSlice,geometryOfUnderlyingSlice,toleranceDistance)) {
						double[] tlhcSuperimposedIn3DSpace = geometryOfSuperimposedSlice.getTLHCArray();
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): tlhc of superimposed slice in 3D space = "+java.util.Arrays.toString(tlhcSuperimposedIn3DSpace));
						if (tlhcSuperimposedIn3DSpace != null && tlhcSuperimposedIn3DSpace.length == 3) {
							double[] tlhcSuperimposedInUnderlyingImageSpace = geometryOfUnderlyingSlice.lookupImageCoordinate(tlhcSuperimposedIn3DSpace);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): tlhc of superimposed slice in underlying image space = "+java.util.Arrays.toString(tlhcSuperimposedInUnderlyingImageSpace));
							if (tlhcSuperimposedInUnderlyingImageSpace != null && tlhcSuperimposedInUnderlyingImageSpace.length == 2) {
								columnOrigin  = tlhcSuperimposedInUnderlyingImageSpace[0];
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): columnOrigin = "+columnOrigin);
								rowOrigin     = tlhcSuperimposedInUnderlyingImageSpace[1];
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): rowOrigin = "+rowOrigin);
								if (superimposedSourceImage != null) {
									BufferedImage originalBufferedImage = superimposedSourceImage.getBufferedImage(superimposedFrame);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): originalBufferedImage = "+originalBufferedImage);
									if (originalBufferedImage != null) {
										//{
										//	int voxelCount=0;
										//	java.awt.image.ColorModel cm = originalBufferedImage.getColorModel();
										//	for (int y = 0; y < originalBufferedImage.getHeight(); y++) {
										//		for (int x = 0; x < originalBufferedImage.getWidth(); x++) {
										//			int pixel = originalBufferedImage.getRGB(x, y);
										//			if (cm.getRed(pixel) == 0 && cm.getGreen(pixel) == 0 && cm.getBlue(pixel) == 0) {
										//			}
										//			else {
										//				++voxelCount;
										//			}
										//		}
										//	}
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): originalBufferedImage voxelCount = "+voxelCount);
										//}

										double[] underlyingSpacing = geometryOfUnderlyingSlice.getVoxelSpacingArray();
										double[] superimposedSpacing = geometryOfSuperimposedSlice.getVoxelSpacingArray();
										double useScaleFactor = superimposedSpacing[0] / underlyingSpacing[0];
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): useScaleFactor = "+useScaleFactor);

										// need to scale size and offset to match underlying image
										java.awt.geom.AffineTransform transform = java.awt.geom.AffineTransform.getTranslateInstance(columnOrigin,rowOrigin);
										transform.concatenate(java.awt.geom.AffineTransform.getScaleInstance(useScaleFactor,useScaleFactor));
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): transform = "+transform);

										// http://docs.oracle.com/javase/tutorial/2d/images/examples/SeeThroughImageApplet.java
										// need to make the new image large enough to account for the row and column offset in the transform, else gets clipped
										bufferedImage = new BufferedImage((int)Math.ceil(originalBufferedImage.getWidth()*useScaleFactor+columnOrigin),(int)Math.ceil(originalBufferedImage.getHeight()*useScaleFactor+rowOrigin),BufferedImage.TYPE_INT_ARGB);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): new BufferedImage = "+bufferedImage);
										Graphics2D g2d = bufferedImage.createGraphics();
										g2d.transform(transform);
										g2d.drawImage(originalBufferedImage,0,0,null);
										//int voxelCount=0;
										// now make it transparent (alpha of 0) where has zero RGB values
										// http://stackoverflow.com/questions/7405955/making-a-certain-color-on-a-bufferedimage-become-transparent
										{
											java.awt.image.ColorModel cm = bufferedImage.getColorModel();
											for (int y = 0; y < bufferedImage.getHeight(); y++) {
												for (int x = 0; x < bufferedImage.getWidth(); x++) {
													int pixel = bufferedImage.getRGB(x, y);
													//java.awt.Color color = new java.awt.Color(pixel);	// aargh ... must be horribly inefficient :(
													//if (color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0) {
													if (cm.getRed(pixel) == 0 && cm.getGreen(pixel) == 0 && cm.getBlue(pixel) == 0) {
														bufferedImage.setRGB(x,y,0);	// sets R,G,B, and A to zero
													}
													//else {
													//	++voxelCount;
													//}
												}
											}
										}
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): new BufferedImage voxelCount = "+voxelCount);

										columnOrigin = 0;		// we have already accounted for the offset, so caller does not have to
										rowOrigin = 0;
									}
								}
							}
						}
					}
					else {
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage(): not close enough to superimpose");
					}
				}
				else {
					slf4jlogger.error("AppliedToUnderlyingImage(): missing geometryOfUnderlyingSlice or superimposedGeometry");
				}
			}
			else {
				slf4jlogger.error("SuperimposedImage.AppliedToUnderlyingImage(): missing underlyingGeometry");
			}
		}
		
		public String toString() {
			return "(bufferedImage="+(bufferedImage == null ? "null" : bufferedImage.toString())+",columnOrigin="+columnOrigin+",rowOrigin="+rowOrigin+")";
		}

		public void notificationOfCurrentLocationIn3DSpace(double[] currentLocationIn3DSpace) {
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage.notificationOfCurrentLocationIn3DSpace(): 3D ("+java.util.Arrays.toString(currentLocationIn3DSpace)+")");
			double[] frameLocation = geometryOfSuperimposedSlice.lookupImageCoordinate(currentLocationIn3DSpace);
//System.err.println("SuperimposedImage.AppliedToUnderlyingImage.notificationOfCurrentLocationIn3DSpace(): 2D ("+java.util.Arrays.toString(frameLocation)+")");
			if (bufferedImage != null) {
				int x = (int)(frameLocation[0]);
				int y = (int)(frameLocation[1]);
				int pixel = bufferedImage.getRGB(x,y);
				if (pixel != 0) {
					slf4jlogger.info("AppliedToUnderlyingImage.notificationOfCurrentLocationIn3DSpace(): Hit on non-zero at 3D ({}) at 2D ({},{})",java.util.Arrays.toString(currentLocationIn3DSpace),x,y);
				}
			}
		}
	}
	
	/**
	 * @param	underlyingGeometry
	 * @param	underlyingFrame				numbered from 0
	 * @param	toleranceDistance			difference in distance along normal to orientation for underlying and superimposed frames to be close enough to superimpose, in mm
	 * @return								an instance of AppliedToUnderlyingImage, which will contain a BufferedImage if a superimposed frame that is close enough can be found
	 */
	public AppliedToUnderlyingImage getAppliedToUnderlyingImage(GeometryOfVolume underlyingGeometry,int underlyingFrame,double toleranceDistance) {
		return new AppliedToUnderlyingImage(underlyingGeometry,underlyingFrame,toleranceDistance);
	}
	
	/**
	 * @param	underlyingGeometry
	 * @param	underlyingFrame				numbered from 0
	 * @return								an instance of AppliedToUnderlyingImage, which will contain a BufferedImage if a superimposed frame that is close enough can be found
	 */
	public AppliedToUnderlyingImage getAppliedToUnderlyingImage(GeometryOfVolume underlyingGeometry,int underlyingFrame) {
		return new AppliedToUnderlyingImage(underlyingGeometry,underlyingFrame,DEFAULT_CLOSEST_SLICE_TOLERANCE_DISTANCE);
	}
	
	public int[] getIntegerScaledCIELabPCS() { return cieLabScaled; }

	/**
	 * @param	superimposedSourceImage
	 * @param	superimposedGeometry
	 * @param	cieLabScaled
	 */
	public SuperimposedImage(SourceImage superimposedSourceImage,GeometryOfVolume superimposedGeometry,int[] cieLabScaled) {
		this.superimposedSourceImage = superimposedSourceImage;
		this.superimposedGeometry = superimposedGeometry;
		this.cieLabScaled = cieLabScaled;
	}

	/**
	 * @param	superimposedSourceImage
	 * @param	superimposedGeometry
	 */
	public SuperimposedImage(SourceImage superimposedSourceImage,GeometryOfVolume superimposedGeometry) {
		this(superimposedSourceImage,superimposedGeometry,null/*cieLabScaled*/);
	}
}


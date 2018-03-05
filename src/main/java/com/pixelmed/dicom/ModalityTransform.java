/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;

/**
 * <p>A transformation constructed from a DICOM attribute list that extracts
 * those attributes that define the Modality LUT transformation, specifically the
 * window center and width attributes.</p>
 *
 * <p> Looks first for a per-frame functional group PixelValueTransformationSequence
 * then looks in the shared functional groups, otherwise tries to find the
 * Rescale Slope and Intercept values in the top level of the dataset.</p>
 * <p>Note that multiple transformations (for the same frames) are not permitted
 * by the standard.</p>
 *
 * <p>Does not currently support a LUT in the Modality LUT Sequence, only linear
 * rescale values.</p>
 *
 * <p>Will default to identity transformation if none present, or if there is an
 * error in what is present (e.g., rescale slope is zerp).</p>
 *
 * @author	dclunie
 */
public class ModalityTransform {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ModalityTransform.java,v 1.8 2017/01/24 10:50:37 dclunie Exp $";
	
	/***/
	private class SingleModalityTransform {
	
		/***/
		double slope;
		/***/
		double intercept;
		/***/
		String explanation;
		
		/**
		 * @param	ptflist
		 */
		SingleModalityTransform(AttributeList ptflist) {
			slope=1;
			intercept=0;
			if (ptflist != null) {
				      slope = Attribute.getSingleDoubleValueOrDefault(ptflist,TagFromName.RescaleSlope,1.0);
				  intercept = Attribute.getSingleDoubleValueOrDefault(ptflist,TagFromName.RescaleIntercept,0.0);
				explanation = Attribute.getSingleStringValueOrEmptyString(ptflist,TagFromName.RescaleType);
//System.err.println("SingleModalityTransform: adding "+slope+","+intercept+" "+explanation);
				if (slope == 0 && intercept == 0) {		// occasionally seen in "bad" images
					slope = 1;							// assume identity
					explanation = "override illegal zero slope with identity (com.pixelmed.dicom.ModalityTransform)" + (explanation == null || explanation.length() == 0 ? "" : ("; " + explanation));
				}
			}
		}
	}
		
	/***/
	private SingleModalityTransform[] arrayOfTransforms;	// null if not varying per-frame, if not null will have size == number of frames
	/***/
	private SingleModalityTransform commonTransforms;	// in which case this will be used
	
	/**
	 * @param	list	the dataset of an image object to be searched for transformations
	 */
	public ModalityTransform(AttributeList list) {
//System.err.println("ModalityTransform:");
		arrayOfTransforms=null;
		commonTransforms=null;
		/*try*/ {
			SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (aPerFrameFunctionalGroupsSequence != null) {
//System.err.println("ModalityTransform: checking PerFrameFunctionalGroupsSequence");
				int nFrames = aPerFrameFunctionalGroupsSequence.getNumberOfItems();
				int frameNumber = 0;
				Iterator pfitems = aPerFrameFunctionalGroupsSequence.iterator();
				while (pfitems.hasNext()) {
					SequenceItem fitem = (SequenceItem)pfitems.next();
					AttributeList flist = fitem.getAttributeList();
					if (flist != null) {
						SequenceAttribute aPixelValueTransformationSequence = (SequenceAttribute)flist.get(TagFromName.PixelValueTransformationSequence);
						if (aPixelValueTransformationSequence != null && aPixelValueTransformationSequence.getNumberOfItems() >= 1) {
//System.err.println("ModalityTransform: found PixelValueTransformationSequence");
							if (arrayOfTransforms == null) arrayOfTransforms = new SingleModalityTransform[nFrames];
							if (arrayOfTransforms[frameNumber] == null) arrayOfTransforms[frameNumber] = null;
							Iterator ptfitems = aPixelValueTransformationSequence.iterator();
							while (ptfitems.hasNext()) {
								SequenceItem ptfitem = (SequenceItem)ptfitems.next();
								AttributeList ptflist = ptfitem.getAttributeList();
								arrayOfTransforms[frameNumber]=new SingleModalityTransform(ptflist);
							}
						}
					}
					++frameNumber;
				}
			}
			
			if (arrayOfTransforms == null) {
//System.err.println("ModalityTransform: checking SharedFunctionalGroupsSequence");
				SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
				if (aSharedFunctionalGroupsSequence != null) {
					// assert aSharedFunctionalGroupsSequence.getNumberOfItems() == 1
					Iterator sitems = aSharedFunctionalGroupsSequence.iterator();
					if (sitems.hasNext()) {
						SequenceItem sitem = (SequenceItem)sitems.next();
						AttributeList slist = sitem.getAttributeList();
						if (slist != null) {
							SequenceAttribute aPixelValueTransformationSequence = (SequenceAttribute)slist.get(TagFromName.PixelValueTransformationSequence);
							if (aPixelValueTransformationSequence != null && aPixelValueTransformationSequence.getNumberOfItems() >= 1) {
//System.err.println("ModalityTransform: found PixelValueTransformationSequence");
								commonTransforms = null;
								Iterator ptfitems = aPixelValueTransformationSequence.iterator();
								while (ptfitems.hasNext()) {
									SequenceItem ptfitem = (SequenceItem)ptfitems.next();
									AttributeList ptflist = ptfitem.getAttributeList();
									commonTransforms=new SingleModalityTransform(ptflist);
								}
							}
						}
					}
				}

				// check for "old-fashioned" Modality LUT style attributes (only if nothing per-frame was detected)
			
				if (arrayOfTransforms == null && commonTransforms == null) {
					commonTransforms=new SingleModalityTransform(list);
				}
			}
			
		}
	}

	/**
	 * Get the rescale slope of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return			the rescale slope (1.0 if none available)
	 */
	public double getRescaleSlope(int frame) {
		SingleModalityTransform useTransform = (arrayOfTransforms == null) ? commonTransforms : arrayOfTransforms[frame];
		return useTransform == null ? 1.0 : useTransform.slope;
	}
	

	/**
	 * Get the rescale intercept of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return			the rescale intercept (0.0 if none available)
	 */
	public double getRescaleIntercept(int frame) {
		SingleModalityTransform useTransform = (arrayOfTransforms == null) ? commonTransforms : arrayOfTransforms[frame];
		return useTransform == null ? 0.0 : useTransform.intercept;
	}
	
}


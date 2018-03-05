/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class encapsulates information pertaining to breast-specific laterality as used in Mammo CAD Image Library entries.</p>
 *
 * @author	dclunie
 */
public class MammographyLaterality {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/MammographyLaterality.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MammographyLaterality.class);

	private static Concept left  = new Concept("C0205091");
	private static Concept right = new Concept("C0205090");
	private static Concept both  = new Concept("C0238767");
	
	// these are from CID 6023 Side from BI-RADS that is included in CID 6022 used for Side by mammo CAD
	private static DisplayableLateralityConcept leftBreast  = new DisplayableLateralityConcept("C0222601","80248007","SRT","SNM3",null,"T-04030","Left breast",null,null,null,null);
	private static DisplayableLateralityConcept rightBreast = new DisplayableLateralityConcept("C0222600","73056007","SRT","SNM3",null,"T-04020","Right breast",null,null,null,null);
	private static DisplayableLateralityConcept bothBreasts = new DisplayableLateralityConcept("C0222605","63762007","SRT","SNM3",null,"T-04080","Both breasts",null,null,null,null);
	
	public static DisplayableLateralityConcept convertGenericLateralityToBreastSpecificLaterality(DisplayableLateralityConcept genericLaterality) {
		if (genericLaterality != null) {
			if (genericLaterality.equals(left)) {
				genericLaterality = leftBreast;
			}
			else if (genericLaterality.equals(right)) {
				genericLaterality = rightBreast;
			}
			else if (genericLaterality.equals(both)) {
				genericLaterality = bothBreasts;
			}
			// else leave it alone
		}
		return genericLaterality;
	}

	public static CodedSequenceItem convertGenericLateralityToBreastSpecificLaterality(CodedSequenceItem genericLateralityCodedSequenceItem) {
		CodedSequenceItem breastLateralityCodedSequenceItem = genericLateralityCodedSequenceItem;
		if (genericLateralityCodedSequenceItem != null) {
			DisplayableLateralityConcept genericLateralityConcept = (DisplayableLateralityConcept)(ProjectionXRayAnatomy.getLateralityConcepts().find(genericLateralityCodedSequenceItem));
			if (genericLateralityConcept != null) {
				DisplayableLateralityConcept breastLateralityConcept = convertGenericLateralityToBreastSpecificLaterality(genericLateralityConcept);
				if (breastLateralityConcept != null) {
					try {
						breastLateralityCodedSequenceItem = breastLateralityConcept.getCodedSequenceItem();
					}
					catch (DicomException e) {
						slf4jlogger.error("",e);
						breastLateralityCodedSequenceItem = genericLateralityCodedSequenceItem;
					}
				}
			}
		}
		return breastLateralityCodedSequenceItem;
	}
}

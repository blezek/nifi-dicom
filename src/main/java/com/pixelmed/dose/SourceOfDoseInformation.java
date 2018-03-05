/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;

public class SourceOfDoseInformation {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/SourceOfDoseInformation.java,v 1.6 2017/01/24 10:50:43 dclunie Exp $";

	private String description;
	private String abbreviation;
	
	private SourceOfDoseInformation() {};
	
	private SourceOfDoseInformation(String description,String abbreviation) {
		this.description = description;
		this.abbreviation = abbreviation;
	};
	
	public static final SourceOfDoseInformation AUTOMATED_DATA_COLLECTION           = new SourceOfDoseInformation("Automated Data Collection","MOD");
	public static final SourceOfDoseInformation MANUAL_ENTRY                        = new SourceOfDoseInformation("Manual Entry","ENTRY");
	public static final SourceOfDoseInformation MPPS_CONTENT                        = new SourceOfDoseInformation("MPPS Content","MPPS");
	public static final SourceOfDoseInformation DOSIMETER                           = new SourceOfDoseInformation("Dosimeter","DSM");
	public static final SourceOfDoseInformation COPIED_FROM_IMAGE_ATTRIBUTES        = new SourceOfDoseInformation("Copied From Image Attributes","HDR");
	public static final SourceOfDoseInformation COMPUTED_FROM_IMAGE_ATTRIBUTES      = new SourceOfDoseInformation("Computed From Image Attributes","COMP");
	public static final SourceOfDoseInformation DERIVED_FROM_HUMAN_READABLE_REPORTS = new SourceOfDoseInformation("Derived From Human-Readable Reports","OCR");
	
	public String toString() { return description; }
	
	public String toStringAbbreviation() { return abbreviation; }
	
	public static SourceOfDoseInformation getSourceOfDoseInformation(ContentItem parent) {
		SourceOfDoseInformation found = null;
		ContentItem ci = parent.getNamedChild("DCM","113854");	// "Source of Dose Information"		// Is actually 1-n in TID 10001 and 10011, but assume 1 for now :(
		if (ci != null
		 && ci instanceof ContentItemFactory.CodeContentItem) {
			CodedSequenceItem conceptCode = ((ContentItemFactory.CodeContentItem)ci).getConceptCode();
			if (conceptCode != null) {
				String csd = conceptCode.getCodingSchemeDesignator();
				if (csd != null) {
					if (csd.equals("DCM")) {
						String cv = conceptCode.getCodeValue();
						if (cv != null) {
							if (cv.equals("113856")) {
								found = SourceOfDoseInformation.AUTOMATED_DATA_COLLECTION;
							}
							else if (cv.equals("113857")) {
								found = SourceOfDoseInformation.MANUAL_ENTRY;
							}
							else if (cv.equals("113858")) {
								found = SourceOfDoseInformation.MPPS_CONTENT;
							}
							else if (cv.equals("113866")) {
								found = SourceOfDoseInformation.COPIED_FROM_IMAGE_ATTRIBUTES;
							}
							else if (cv.equals("113867")) {
								found = SourceOfDoseInformation.COMPUTED_FROM_IMAGE_ATTRIBUTES;
							}
							else if (cv.equals("113868")) {
								found = SourceOfDoseInformation.DERIVED_FROM_HUMAN_READABLE_REPORTS;
							}
						}
					}
					else if (csd.equals("SRT")) {
						String cv = conceptCode.getCodeValue();
						if (cv != null) {
							if (cv.equals("A-2C090")) {
								found = SourceOfDoseInformation.DOSIMETER;
							}
						}
					}
				}
			}
		}
		return found;
	}

	public static CodedSequenceItem getCodedSequenceItem(SourceOfDoseInformation role) throws DicomException {
		CodedSequenceItem csi = null;
		if (role != null) {
			if (role.equals(SourceOfDoseInformation.AUTOMATED_DATA_COLLECTION)) {
				csi = new CodedSequenceItem("113856","DCM","Automated Data Collection");
			}
			else if (role.equals(SourceOfDoseInformation.MANUAL_ENTRY)) {
				csi = new CodedSequenceItem("113857","DCM","Manual Entry");
			}
			else if (role.equals(SourceOfDoseInformation.MPPS_CONTENT)) {
				csi = new CodedSequenceItem("113858","DCM","MPPS Content");
			}
			else if (role.equals(SourceOfDoseInformation.DOSIMETER)) {
				csi = new CodedSequenceItem("A-2C090","SRT","Dosimeter");
			}
			else if (role.equals(SourceOfDoseInformation.COPIED_FROM_IMAGE_ATTRIBUTES)) {
				csi = new CodedSequenceItem("113866","DCM","Copied From Image Attributes");
			}
			else if (role.equals(SourceOfDoseInformation.COMPUTED_FROM_IMAGE_ATTRIBUTES)) {
				csi = new CodedSequenceItem("113867","DCM","Computed From Image Attributes");
			}
			else if (role.equals(SourceOfDoseInformation.DERIVED_FROM_HUMAN_READABLE_REPORTS)) {
				csi = new CodedSequenceItem("113868","DCM","Derived From Human-Readable Reports");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
}


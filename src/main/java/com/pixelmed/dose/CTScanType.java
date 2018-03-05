/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class CTScanType {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CTScanType.java,v 1.19 2017/01/24 10:50:42 dclunie Exp $";
	
	private String description;
	
	private CTScanType() {};
	
	private CTScanType(String description) {
		this.description = description;
	};
	
	public static final CTScanType LOCALIZER = new CTScanType("Localizer");
	
	public static final CTScanType HELICAL = new CTScanType("Helical");
	
	public static final CTScanType AXIAL = new CTScanType("Axial");
	
	public static final CTScanType STATIONARY = new CTScanType("Stationary");
	
	public static final CTScanType FREE = new CTScanType("Free");
	
	public static final CTScanType CONEBEAM = new CTScanType("Cone Beam");
	
	public static final CTScanType UNKNOWN = new CTScanType("Unknown");
	
	public String toString() { return description; }
	
	public static CTScanType selectFromDescription(String description) {
		CTScanType found = UNKNOWN;
		if (description != null) {
			description = description.trim().toUpperCase(java.util.Locale.US);
			if (description.equals(HELICAL.toString().toUpperCase(java.util.Locale.US))
			 || description.equals("SPIRAL")
			 || description.equals("CARDIAC HELICAL")	// GE
			) {
				found = HELICAL;
			}
			else if (description.equals(AXIAL.toString().toUpperCase(java.util.Locale.US))
			 ||  description.equals("SEQUENCED")
			 ||  description.equals("NORMAL")		// Toshiba
			) {
				found = AXIAL;
			}
			else if (description.equals(FREE.toString().toUpperCase(java.util.Locale.US))
			 || description.equals("SMARTVIEW")		// GE's fluoroscopy mode
			) {
				found = FREE;
			}
			else if (description.equals(STATIONARY.toString().toUpperCase(java.util.Locale.US))
			 ||  description.equals("CINE")			// when GE uses CINE in the screen, they set STATIONARY in their SRs
			 ||  description.equals("DYNAMIC")		// Toshiba
			 ||  description.equals("VOLUME")		// Toshiba (N.B. this may be problem for Hitachi, for which VOLUME is HELICAL per AAPM WGNCTP)
			 ||  description.equals("SMARTSTEP")	// GE
			) {
				found = STATIONARY;
			}
			else if (description.equals(LOCALIZER.toString().toUpperCase(java.util.Locale.US))
			 || description.equals("SCOUT")
			 || description.equals("CONSTANT_ANGLE")
			 || description.equals("TOPOGRAM")
			 || description.equals("SCANOSCOPE")	// Toshiba
			) {
				found = LOCALIZER;
			}
			else if (description.equals(CONEBEAM.toString().toUpperCase(java.util.Locale.US))
			 || description.equals("Cone Beam")		// with space
			) {
				found = CONEBEAM;
			}
		}
		return found;
	}
	
	public static CTScanType selectFromCode(CodedSequenceItem csi) {
		CTScanType found = UNKNOWN;
		if (csi != null) {
			String cv = csi.getCodeValue();
			String csd = csi.getCodingSchemeDesignator();
			if (csd.equals("SRT") && cv.equals("P5-08001")			// "Spiral Acquisition"
			 || csd.equals("SCT") && cv.equals("116152004")
			 || csd.equals("UMLS") && cv.equals("C0860888")
			) {
				found = HELICAL;
			}
			else if (csd.equals("DCM") && cv.equals("113804")) {	// "Sequenced Acquisition"
				found = AXIAL;
			}
			else if (csd.equals("DCM") && cv.equals("113805")) {	// "Constant Angle Acquisition"
				found = LOCALIZER;
			}
			else if (csd.equals("DCM") && cv.equals("113806")) {	// "Stationary Acquisition"
				found = STATIONARY;
			}
			else if (csd.equals("DCM") && cv.equals("113807")) {	// "Free Acquisition"
				found = FREE;
			}
			else if (csd.equals("SRT") && cv.equals("R-FB8F1")		// "Cone Beam Acquisition"
			      || csd.equals("SCT") && cv.equals("702569007")
			 ) {
				found = CONEBEAM;
			}
		}
		return found;
	}
	
	public static CodedSequenceItem getCodedSequenceItem(CTScanType scanType) throws DicomException {
		CodedSequenceItem csi = null;
		if (scanType != null) {
			if (scanType.equals(CTScanType.LOCALIZER)) {
				csi = new CodedSequenceItem("113805","DCM","Constant Angle Acquisition");
			}
			else if (scanType.equals(CTScanType.HELICAL)) {
				csi = new CodedSequenceItem("P5-08001","SRT","Spiral Acquisition");
			}
			else if (scanType.equals(CTScanType.AXIAL)) {
				csi = new CodedSequenceItem("113804","DCM","Sequenced Acquisition");
			}
			else if (scanType.equals(CTScanType.STATIONARY)) {
				csi = new CodedSequenceItem("113806","DCM","Stationary Acquisition");
			}
			else if (scanType.equals(CTScanType.FREE)) {
				csi = new CodedSequenceItem("113807","DCM","Free Acquisition");
			}
			else if (scanType.equals(CTScanType.CONEBEAM)) {
				csi = new CodedSequenceItem("R-FB8F1","SRT","Cone Beam Acquisition");
			}
			// else if UNKNOWN return nothing
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
	
}
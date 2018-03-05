/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class ScopeOfDoseAccummulation {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/ScopeOfDoseAccummulation.java,v 1.6 2017/01/24 10:50:43 dclunie Exp $";
	
	private String description;

	private ScopeOfDoseAccummulation() {};
	
	private ScopeOfDoseAccummulation(String description) {
		this.description = description;
	};
	
	public static final ScopeOfDoseAccummulation STUDY = new ScopeOfDoseAccummulation("Study");
	
	public static final ScopeOfDoseAccummulation SERIES = new ScopeOfDoseAccummulation("Series");
	
	public static final ScopeOfDoseAccummulation PPS = new ScopeOfDoseAccummulation("Performed Procedure Step");
	
	public static final ScopeOfDoseAccummulation IRRADIATION_EVENT = new ScopeOfDoseAccummulation("Irradiation Event");
	
	public String toString() { return description; }

	public static ScopeOfDoseAccummulation selectFromCode(CodedSequenceItem csi) {
		ScopeOfDoseAccummulation found = null;
		if (csi != null) {
			String cv = csi.getCodeValue();
			String csd = csi.getCodingSchemeDesignator();
			if (csd.equals("DCM") && cv.equals("113014")) {
				found = STUDY;
			}
			else if (csd.equals("DCM") && cv.equals("113015")) {
				found = SERIES;
			}
			else if (csd.equals("DCM") && cv.equals("113016")) {
				found = PPS;
			}
			else if (csd.equals("DCM") && cv.equals("113852")) {
				found = IRRADIATION_EVENT;
			}
		}
		return found;
	}
	public static CodedSequenceItem getCodedSequenceItemForScopeConcept(ScopeOfDoseAccummulation scope) throws DicomException {
		CodedSequenceItem csi = null;
		if (scope != null) {
			if (scope.equals(ScopeOfDoseAccummulation.STUDY)) {
				csi = new CodedSequenceItem("113014","DCM","Study");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.SERIES)) {
				csi = new CodedSequenceItem("113015","DCM","Series");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.PPS)) {
				csi = new CodedSequenceItem("113016","DCM","Performed Procedure Step");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.IRRADIATION_EVENT)) {
				csi = new CodedSequenceItem("113852","DCM","Irradiation Event");
			}
		}
		return csi;
	}
	
	public static CodedSequenceItem getCodedSequenceItemForUIDConcept(ScopeOfDoseAccummulation scope) throws DicomException {
		CodedSequenceItem csi = null;
		if (scope != null) {
			if (scope.equals(ScopeOfDoseAccummulation.STUDY)) {
				csi = new CodedSequenceItem("110180","DCM","Study Instance UID");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.SERIES)) {
				csi = new CodedSequenceItem("112002","DCM","Series Instance UID");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.PPS)) {
				csi = new CodedSequenceItem("121126","DCM","Performed Procedure Step SOP Instance UID");
			}
			else if (scope.equals(ScopeOfDoseAccummulation.IRRADIATION_EVENT)) {
				csi = new CodedSequenceItem("113853","DCM","Irradiation Event UID");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItemForScopeConcept() throws DicomException {
		return getCodedSequenceItemForScopeConcept(this);
	}
	
	public CodedSequenceItem getCodedSequenceItemForUIDConcept() throws DicomException {
		return getCodedSequenceItemForUIDConcept(this);
	}
}
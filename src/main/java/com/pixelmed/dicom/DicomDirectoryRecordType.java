/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * @author	dclunie
 */
public class DicomDirectoryRecordType {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectoryRecordType.java,v 1.5 2017/01/24 10:50:37 dclunie Exp $";

	public static final String patient = "PATIENT";
	public static final String study = "STUDY";
	public static final String series = "SERIES";
	public static final String concatentation = "CONCATENATION";		// non-standard, but used in pseudo-record for tree browser :(
	public static final String image = "IMAGE";
	public static final String srDocument = "SR DOCUMENT";
	public static final String keyObjectDocument = "KEY OBJECT DOC";
	public static final String waveform = "WAVEFORM";
	public static final String spectroscopy = "SPECTROSCOPY";
	public static final String rawData = "RAW DATA";
	public static final String rtDose = "RT DOSE";
	public static final String rtStructureSet = "RT STRUCTURE SET";
	public static final String rtPlan = "RT PLAN";
	public static final String rtTreatmentRecord = "RT TREAT RECORD";
	public static final String presentationState = "PRESENTATION";
	public static final String registration = "REGISTRATION";
	public static final String fiducial = "FIDUCIAL";
	public static final String realWorldValueMapping = "VALUE MAP";
	public static final String stereometricRelationship = "STEREOMETRIC";
	public static final String encapsulatedDocument = "ENCAP DOC";
	public static final String hl7StructuredDocument = "HL7 STRUC DOC";
}

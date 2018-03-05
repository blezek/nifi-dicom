/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AgeStringAttribute;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CompositeInstanceContext;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class DoseCompositeInstanceContext extends CompositeInstanceContext {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/DoseCompositeInstanceContext.java,v 1.8 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DoseCompositeInstanceContext.class);

	public DoseCompositeInstanceContext() {
		super();
	}
	
	public DoseCompositeInstanceContext(AttributeList srcList) {
		super(srcList,true/*forSR*/);
	}
	
	public void updateFromSource(CTIrradiationEventDataFromImages eventDataFromImages) {
		if (eventDataFromImages != null) {
			try {
				String timezoneOffsetFromUTC = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TimezoneOffsetFromUTC);
				if (timezoneOffsetFromUTC.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no timezoneOffsetFromUTC in list");
					timezoneOffsetFromUTC = eventDataFromImages.getTimezoneOffsetFromUTC();
					if (timezoneOffsetFromUTC != null && timezoneOffsetFromUTC.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found TimezoneOffsetFromUTC in eventDataFromImages");
						{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC); a.addValue(timezoneOffsetFromUTC); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			try {
				// in case the patient sex, age, weight or size (height) were not in the source instance, use what was found in the other instances, if it is consistent ...
				String patientAge = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientAge);
				if (patientAge.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientAge in list");
					patientAge = eventDataFromImages.getPatientAge();
					if (patientAge != null && patientAge.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientAge in eventDataFromImages");
						{ Attribute a = new AgeStringAttribute(TagFromName.PatientAge); a.addValue(patientAge); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			try {
				String patientSex = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientSex);
				if (patientSex.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientSex in list");
					patientSex = eventDataFromImages.getPatientSex();
					if (patientSex != null && patientSex.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientSex in eventDataFromImages");
						{ Attribute a = new CodeStringAttribute(TagFromName.PatientSex); a.addValue(patientSex); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			try {
				String patientWeight = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientWeight);
				if (patientWeight.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientWeight in list");
					patientWeight = eventDataFromImages.getPatientWeight();
					if (patientWeight != null && patientWeight.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientWeight in eventDataFromImages");
						{ Attribute a = new DecimalStringAttribute(TagFromName.PatientWeight); a.addValue(patientWeight); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			try {
				String patientSize = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientSize);
				if (patientSize.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientSize in list");
					patientSize = eventDataFromImages.getPatientSize();
					if (patientSize != null && patientSize.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientSize in eventDataFromImages");
						{ Attribute a = new DecimalStringAttribute(TagFromName.PatientSize); a.addValue(patientSize); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
	}
}


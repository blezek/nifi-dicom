/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.dicom.*;
import com.pixelmed.dose.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class GenerateRadiationDoseStructuredReport {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/doseocr/GenerateRadiationDoseStructuredReport.java,v 1.22 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(GenerateRadiationDoseStructuredReport.class);

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a radiation dose SR.</p>
	 *
	 * <p>Currently assumes that there will be only one source of dose of dose information amongst the supplied files (i.e., one dose screen) and returns
	 * one {@link com.pixelmed.dose.CTDose CTDose} instance; if there are multiple dose screens which is encountered first will be used. Does not currently account for multi-page dose
	 * screens but may do some day.</p>
	 *
	 * <p>Ignores any existing Radiation Dose SR files (whether supplied by the vendor or created by PixelMed).</p>
	 *
	 * @param	paths		a Vector of String paths to a DICOMDIR or folder or list of files containing dose screens, reports and acquired CT slices
	 * @return				a {@link com.pixelmed.dose.CTDose CTDose} object that contains the information to extract as an {@link com.pixelmed.dicom.StructuredReport StructuredReport} or  {@link com.pixelmed.dicom.AttributeList AttributeList}
	 */
	public static CTDose generateDoseReportInformationFromFiles(Vector paths) {
		CTDose ctDose = null;
		CTIrradiationEventDataFromImages eventDataFromImages = new CTIrradiationEventDataFromImages(paths);
//System.err.print(eventDataFromImages);

		ArrayList<String> screenFilenames = eventDataFromImages.getDoseScreenFilenames();
		
		if (screenFilenames != null && !screenFilenames.isEmpty()) {
			try {
				OCR ocr = new OCR(screenFilenames);
//System.err.print(ocr);
				ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,eventDataFromImages,true);
				if (ctDose == null) {
					for (String screenFilename : screenFilenames) {
//System.err.println("GenerateRadiationDoseStructuredReport.generateDoseReportInformationFromFiles(): Screen "+screenFilename);
						try {
							AttributeList list = new AttributeList();
							list.read(screenFilename);
//System.err.print(list);
							if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
//System.err.println("GenerateRadiationDoseStructuredReport.generateDoseReportInformationFromFiles(): isPhilipsDoseScreenInstance");
								ctDose = ExposureDoseSequence.getCTDoseFromExposureDoseSequence(list,eventDataFromImages,true);
								break;	// have it so no need to process any further ...
							}
						}
						catch (Exception e) {
							slf4jlogger.error("",e);
						}
					}
				}
				// CompositeInstanceContext should already have been set ... do NOT overwrite it 
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
		return ctDose;
	}
	
	public static void createContextForNewRadiationDoseStructuredReportFromExistingInstance(AttributeList list,CTDose ctDose,CTIrradiationEventDataFromImages eventDataFromImages) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): list supplied = ");
//System.err.print(list);
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): eventDataFromImages supplied = ");
//System.err.print(eventDataFromImages);
		if (list != null) {
			ctDose.setSourceSOPInstanceUID(Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID));

			DoseCompositeInstanceContext cic = new DoseCompositeInstanceContext(list); 
			cic.removeSeries();
			cic.removeInstance();
			UIDGenerator u = new UIDGenerator();
			try {
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getAnotherNewUID()); cic.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getAnotherNewUID()); cic.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue("897"); cic.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue("1"); cic.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.SeriesDate); a.addValue(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDate)); cic.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.SeriesTime); a.addValue(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesTime)); cic.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.ContentDate); a.addValue(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContentDate)); cic.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.ContentTime); a.addValue(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContentTime)); cic.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.SeriesDescription); a.addValue("Radiation Dose Information"); cic.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.CompletionFlag); a.addValue("COMPLETE"); cic.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.VerificationFlag); a.addValue("UNVERIFIED"); cic.put(a); }
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			{
				Attribute a = list.get(TagFromName.ReferencedPerformedProcedureStepSequence);			// will have been removed from cic by removeSeries(), so put it back if present
				if (a == null) {
					a = new SequenceAttribute(TagFromName.ReferencedPerformedProcedureStepSequence);
				}
				cic.put(a);
			}
			{
				Attribute a = list.get(TagFromName.TimezoneOffsetFromUTC);								// will have been removed from cic by removeInstance(), so put it back if present
				if (a == null) {
					a = new SequenceAttribute(TagFromName.TimezoneOffsetFromUTC);
				}
				cic.put(a);
			}
			{
				Attribute a = cic.getAttributeList().get(TagFromName.PerformedProcedureCodeSequence);	// check cic not list, since may have been copied from ProcedureCodeSequence by CompositeInstanceContext() constructor
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PerformedProcedureCodeSequence = "+a);
				if (a == null) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): did not find PerformedProcedureCodeSequence; inserting empty");
					a = new SequenceAttribute(TagFromName.PerformedProcedureCodeSequence);
				}
				cic.put(a);
			}

			AttributeList cicList = cic.getAttributeList();
			String useDeviceSerialNumber = DeviceParticipant.getDeviceSerialNumberOrSuitableAlternative(cicList,true/*insertAlternateBackInList*/);
			
			cic.updateFromSource(eventDataFromImages);	// in case patient characteristics are missing from source list but present in other instances
			
			ctDose.setCompositeInstanceContext(cic);
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): CompositeInstanceContext now = ");
//System.err.print(cic);

			String physicianName = Attribute.getSingleStringValueOrNull(list,TagFromName.PerformingPhysicianName);
			if (physicianName == null) { physicianName = Attribute.getSingleStringValueOrNull(list,TagFromName.PhysiciansOfRecord); }
			if (physicianName == null) { physicianName = Attribute.getSingleStringValueOrNull(list,TagFromName.NameOfPhysiciansReadingStudy); }
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): physicianName = "+physicianName);

			String useDeviceObserverUID  = DeviceParticipant.getDeviceObserverUIDOrSuitableAlternative(cicList);

			CommonDoseObserverContext cdoc = new CommonDoseObserverContext(
				useDeviceObserverUID,
				Attribute.getSingleStringValueOrNull(list,TagFromName.StationName),
				Attribute.getSingleStringValueOrNull(list,TagFromName.Manufacturer),
				Attribute.getSingleStringValueOrNull(list,TagFromName.ManufacturerModelName),
				useDeviceSerialNumber,
				""/*location*/,
				Attribute.getSingleStringValueOrNull(list,TagFromName.OperatorsName),
				""/*operatorID*/,
				physicianName,
				""/*physicianID*/,
				""/*idIssuer*/,
				Attribute.getSingleStringValueOrNull(list,TagFromName.InstitutionName));
			ctDose.setObserverContext(cdoc);
		}
	}

	
	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a radiation dose SR.</p>
	 *
	 * @param	arg		one or more paths to a DICOMDIR or folder or dose screens, reports and acquired CT slices, followed by a single filename for the generated RDSR file, or "-" if none
	 */
	public static final void main(String arg[]) {
		try {
			String srOutputFilename = null;
			int inputPathCount = arg.length;
			if (inputPathCount > 1) {
				--inputPathCount;
				srOutputFilename = arg[inputPathCount];
				if (srOutputFilename.equals("-")) {
					srOutputFilename = null;
				}
				if (new File(srOutputFilename).exists()) {
					System.err.println("Error: RDSR output file "+srOutputFilename+" already exists - not overwriting");
					System.exit(1);
				}
			}
			Vector paths = new Vector();
			for (int i=0; i<inputPathCount; ++i) {
				paths.add(arg[i]);
			}
			CTDose ctDose = generateDoseReportInformationFromFiles(paths);
			System.err.println(ctDose);
			if (srOutputFilename != null && ctDose != null) {
				ctDose.write(srOutputFilename,null,GenerateRadiationDoseStructuredReport.class.getCanonicalName());
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


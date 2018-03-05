/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.dicom.*;
import com.pixelmed.dose.*;

import java.util.List;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class RenderedDoseReport {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/doseocr/RenderedDoseReport.java,v 1.18 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RenderedDoseReport.class);

	private static String getReportFragmentFromCTDose(CTDose ctDose,AttributeList list,CTIrradiationEventDataFromImages eventDataFromImages,boolean summary,boolean doHTML) {
		String reportFragment = "";
		if (ctDose != null) {
			if (list != null) {
				DoseCompositeInstanceContext cic = new DoseCompositeInstanceContext(list);
				cic.updateFromSource(eventDataFromImages);	// in case patient characteristics are missing from source list but present in other instances
				ctDose.setCompositeInstanceContext(cic);
			}
//System.err.print(ctDose.toString(true,true));
			if (doHTML) {
				reportFragment = ctDose.getHTMLTableRow(!summary);
			}
			else {
				// plain text
				reportFragment = ctDose.toString(!summary,true/*pretty*/);
			}
		}
		return reportFragment;
	}

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	paths		a Vector of String paths to a DICOMDIR or folder or list of files containing dose screens, reports and acquired CT slices
	 * @param	summary		if true generate a summary only, otherwise tabulate the acquisition and technique data
	 */
	public static String generateDoseReportInformationFromFiles(Vector paths,boolean summary) {
		return generateDoseReportInformationFromFiles(paths,summary,null);
	}

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	paths		a Vector of String paths to a DICOMDIR or folder or list of files containing dose screens, reports and acquired CT slices
	 * @param	summary		if true generate a summary only, otherwise tabulate the acquisition and technique data
	 * @param	contentType	the type of text content to be generated, e.g., "text/html"; will be plain text if null or unrecognized
	 */
	public static String generateDoseReportInformationFromFiles(Vector paths,boolean summary,String contentType) {
		String report = "";
		boolean doHTML = false;
		if (contentType != null) {
			String useContentType = contentType.trim().toLowerCase(java.util.Locale.US);
			if (useContentType.equals("text/html")) {
				doHTML = true;
			}
			// anything else, e.g., "text/plain" is just default
		}
		
		CTIrradiationEventDataFromImages eventDataFromImages = new CTIrradiationEventDataFromImages(paths);
//System.err.print(eventDataFromImages);

		List<String> doseScreenFilenames = eventDataFromImages.getDoseScreenFilenames();
		if (doseScreenFilenames != null && !doseScreenFilenames.isEmpty()) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): Have "+doseScreenFilenames.size()+" screens");
			try {
				OCR ocr = new OCR(doseScreenFilenames);
				CTDose ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,eventDataFromImages,true);
				if (ctDose == null) {
					for (String screenFilename : doseScreenFilenames) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): Screen "+screenFilename);
						try {
							AttributeList list = new AttributeList();
							list.read(screenFilename);
//System.err.print(list);
							if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
								ctDose = ExposureDoseSequence.getCTDoseFromExposureDoseSequence(list,eventDataFromImages,true);
							}
							if (ctDose != null) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): have ctDose from individual ExposureDoseSequence files");
								report += getReportFragmentFromCTDose(ctDose,list,eventDataFromImages,summary,doHTML);
							}
						}
						catch (Exception e) {
							slf4jlogger.error("",e);
						}
					}
				}
				else {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): have ctDose from "+doseScreenFilenames.size()+" multiple files handled together");
					report += getReportFragmentFromCTDose(ctDose,ocr.getCommonAttributeList(),eventDataFromImages,summary,doHTML);
				}
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}

		for (String srFilename : eventDataFromImages.getDoseStructuredReportFilenames()) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): SR "+srFilename);
			try {
				AttributeList list = new AttributeList();
				list.read(srFilename);
//System.err.print(list);
				CTDose ctDose = null;
				if (SOPClass.isStructuredReport(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID))) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): isStructuredReport");
					ctDose = new CTDose(list);
//System.err.print(ctDose.toString(true,true));
				}
				if (ctDose != null) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): have ctDose from SR");
					report += getReportFragmentFromCTDose(ctDose,list,eventDataFromImages,summary,doHTML);
				}
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
		return report;
	}

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	arg		one or more paths to a DICOMDIR or folder or dose screens, reports and acquired CT slices, then an optional SUMMARY or FULL argument, then HTML or TEXT argument (default is SUMMARY TEXT) 
	 */
	public static final void main(String arg[]) {
		try {
			boolean summary = false;
			String contentType = "text/plain";
			int inputPathCount = arg.length;
			if (inputPathCount > 1) {
				String optionCandidate = arg[inputPathCount-1].toUpperCase(java.util.Locale.US);
				if (optionCandidate.equals("TEXT")) {
					--inputPathCount;
					contentType = "text/plain";
				}
				else if (optionCandidate.equals("HTML")) {
					--inputPathCount;
					contentType = "text/html";
				}
			}
			if (inputPathCount > 1) {
				String optionCandidate = arg[inputPathCount-1].toUpperCase(java.util.Locale.US);
				if (optionCandidate.equals("FULL")) {
					--inputPathCount;
					summary = false;
				}
				else if (optionCandidate.equals("SUMMARY")) {
					--inputPathCount;
					summary = true;
				}
			}
			Vector paths = new Vector();
			for (int i=0; i<inputPathCount; ++i) {
				paths.add(arg[i]);
			}
			String report = generateDoseReportInformationFromFiles(paths,summary,contentType);
			System.err.println(report);		// no need to use SLF4J since command line utility/test
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


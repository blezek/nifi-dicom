/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import java.text.SimpleDateFormat;
import com.pixelmed.utils.FloatFormatter;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A transformation constructed from a DICOM attribute list that extracts
 * those attributes which describe how stored pixel values are translated
 * into PET SUV values.</p>
 *
 * @author	dclunie
 */
public class SUVTransform {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SUVTransform.java,v 1.11 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SUVTransform.class);
	
	public static long deriveScanDateTimeFromHalfLifeAcquisitionDateTimeFrameReferenceTimeAndActualFrameDuration(AttributeList list) {
		long scanDateTime = 0;
		try {
			long acquisitionDateTime = DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(list,TagFromName.AcquisitionDate,TagFromName.AcquisitionTime);
//System.err.println("acquisitionDateTime = "+acquisitionDateTime+" mS");
			Attribute aHalfLife = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadionuclideHalfLife);
			if (aHalfLife != null) {
				double halfLife = aHalfLife.getSingleDoubleValueOrDefault(0.0);
//System.err.println("halfLife = "+halfLife+" secs");
				double frameReferenceTime = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.FrameReferenceTime,0d) / 1000.0;	// make seconds
//System.err.println("frameReferenceTime = "+frameReferenceTime+" secs");
				double frameDuration = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.ActualFrameDuration,0d) / 1000.0;		// make seconds
//System.err.println("frameDuration = "+frameDuration+" secs");
				if (frameReferenceTime > 0 && frameDuration > 0) {
					double decayConstant = Math.log(2d) / halfLife;
//System.err.println("decayConstant = "+decayConstant+" /sec");
					double decayDuringFrame = decayConstant * frameDuration;
//System.err.println("decayDuringFrame = "+decayDuringFrame);
					double averageCountRateTimeWithinFrame = 1d/decayConstant * Math.log(decayDuringFrame / (1d - Math.exp(-decayDuringFrame)));
//System.err.println("averageCountRateTimeWithinFrame = "+averageCountRateTimeWithinFrame+" secs");
					double offsetOfAcquisitionDateTimeFromScanDateTime = (frameReferenceTime - averageCountRateTimeWithinFrame)*1000;
//System.err.println("offsetOfAcquisitionDateTimeFromScanDateTime = "+offsetOfAcquisitionDateTimeFromScanDateTime+" mS");
					scanDateTime = (long)(acquisitionDateTime - offsetOfAcquisitionDateTimeFromScanDateTime);
				}
			}
		}
		catch (Exception e) {
			slf4jlogger.error("Could not extract or parse values to compute scanDateTime from Half Life, Acquisition Date and Time, Frame Reference Time and Actual Frame Duration",e);
		}
//System.err.println("getScanDateTimeFromHalfLifeAcquisitionDateTimeFrameReferenceTimeAndActualFrameDuration(): return "+scanDateTime+" mS");
		return scanDateTime;
	}

	/***/
	public class SingleSUVTransform {
	
		double rescaleIntercept;

		boolean haveSUVbw;
		double scaleFactorSUVbw;
		String unitsSUVbw;

		boolean haveSUVbsa;
		double scaleFactorSUVbsa;
		String unitsSUVbsa;

		boolean haveSUVlbm;
		double scaleFactorSUVlbm;
		String unitsSUVlbm;
		
		boolean haveSUVibw;
		double scaleFactorSUVibw;
		String unitsSUVibw;
		
		/**
		 * @param	list		PixelValueTransformationSequence item attributes
		 */
		SingleSUVTransform(AttributeList list) {
			if (list != null) {
				String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (sopClassUID.equals(SOPClass.PETImageStorage)) {
//System.err.println("have PET SOP Class");
					String correctedImage = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.CorrectedImage);
					if (correctedImage.contains("ATTN") && correctedImage.contains("DECY")) {
						String units = Attribute.getSingleStringValueOrNull(list,TagFromName.Units);
						double rescaleSlope = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleSlope,1.0);
//System.err.println("rescaleSlope = "+rescaleSlope);
						rescaleIntercept = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleIntercept,0.0);		// should be zero for PET
//System.err.println("rescaleIntercept = "+rescaleIntercept);
						double weight = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.PatientWeight,0.0); // in kg
//System.err.println("weight = "+weight+" kg");
						double height = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.PatientSize,0.0); // in m
//System.err.println("height = "+height+" m");
						if (height < 5) {				// sometimes it is incorrectly encoded and is already in cm, not m; a 5m tall patient seems unlikely
							height =  height * 100;		// we need it in cm
						}
						else {
							System.err.println("PatientSize unrealistically large, assuming is in cm rather than the required m = "+height);
						}
//System.err.println("height = "+height+" cm");
						String sex = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientSex).trim().toUpperCase(Locale.US);
//System.err.println("sex = "+sex);
						if (units.equals("BQML")) {
//System.err.println("have units BQML");
							String decayCorrection = Attribute.getSingleStringValueOrNull(list,TagFromName.DecayCorrection);
//System.err.println("decayCorrection = "+decayCorrection);
							Attribute aInjectedDose = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadionuclideTotalDose);
//System.err.println("have injected dose = "+aInjectedDose);
							Attribute aHalfLife = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadionuclideHalfLife);
//System.err.println("have half life = "+aHalfLife);
							Attribute aStartTime = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadiopharmaceuticalStartTime);
//System.err.println("have start time = "+aStartTime);

							String sSeriesDate = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesDate);
							String sScanDate = sSeriesDate;	// start Date is not explicit … assume same as Series Date; but consider spanning midnight

							long scanDateTime = 0;
							long seriesDateTime = 0;
							try {
								Attribute aSeriesTime = list.get(TagFromName.SeriesTime);
								if (aSeriesTime != null && aSeriesTime.getVM() > 0) {	// check this, since otherwise will treat as 000000 on day of SeriesDate
									seriesDateTime = DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(list,TagFromName.SeriesDate,TagFromName.SeriesTime);
								}
							}
							catch (Exception e) {
								slf4jlogger.error("Could not extract or parse Series Date and Series Time",e);
							}
//System.err.println("seriesDateTime = "+seriesDateTime+" mS "+DateTimeAttribute.getFormattedStringUTC(new java.util.Date(seriesDateTime)));
							scanDateTime = seriesDateTime;
//System.err.println("scanDateTime = "+scanDateTime+" mS "+DateTimeAttribute.getFormattedStringUTC(new java.util.Date(scanDateTime)));
							long acquisitionDateTime = 0;
							try {
								Attribute aAcquisitionTime = list.get(TagFromName.AcquisitionTime);
								if (aAcquisitionTime != null && aAcquisitionTime.getVM() > 0) {	// check this, since otherwise will treat as 000000 on day of AcquisitionDate
									acquisitionDateTime = DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(list,TagFromName.AcquisitionDate,TagFromName.AcquisitionTime);
								}
							}
							catch (Exception e) {
								slf4jlogger.error("Could not extract or parse Acquisition Date and Acquisition Time",e);
							}
//System.err.println("acquisitionDateTime = "+acquisitionDateTime+" mS "+DateTimeAttribute.getFormattedStringUTC(new java.util.Date(acquisitionDateTime)));
//System.err.println("acquisitionDateTime offset from seriesDateTime = "+((acquisitionDateTime-seriesDateTime)/1000)+" secs");
							if (scanDateTime == 0 || seriesDateTime > acquisitionDateTime) {
								slf4jlogger.info("have missing series date time, or it is after acquisition date time");
								// per GE docs, may have been updated during post-processing into new series
								String privateCreator = Attribute.getSingleStringValueOrEmptyString(list,new AttributeTag(0x0009,0x0010)).trim();
								String privateScanDateTime = Attribute.getSingleStringValueOrNull(list,new AttributeTag(0x0009,0x100d));
								if (privateCreator.equals("GEMS_PETD_01") && privateScanDateTime != null) {
									slf4jlogger.info("use GE private scan date time");
									try {
										scanDateTime = DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(privateScanDateTime);
										sScanDate = privateScanDateTime.substring(0,8);
									}
									catch (Exception e) {
										slf4jlogger.error("Could not extract or parse GE Private Scan Date and Time",e);
									}
								}
								else {
									long derivedScanDateTime = deriveScanDateTimeFromHalfLifeAcquisitionDateTimeFrameReferenceTimeAndActualFrameDuration(list);
//System.err.println("derivedScanDateTime = "+derivedScanDateTime+" mS "+DateTimeAttribute.getFormattedStringUTC(new java.util.Date(derivedScanDateTime)));
									if (derivedScanDateTime > 0) {
										slf4jlogger.info("use scan date time derived from HalfLife, AcquisitionDateTime, FrameReferenceTime and ActualFrameDuration");
										scanDateTime = derivedScanDateTime;
									}
								}
							}
						
							if (decayCorrection.equals("START") && aInjectedDose != null && aHalfLife != null && aStartTime != null && sScanDate != null && scanDateTime != 0 && weight != 0) {
//System.err.println("have all we need");
								long startDateTime = 0;
								try {
									// extremely important that the timezone be handled, to mirror the use of getTimeInMilliSecondsSinceEpoch(AttributeList,AttributeTag,AttributeTag) for the other values
									startDateTime = DateTimeAttribute.getTimeInMilliSecondsSinceEpoch(sScanDate + aStartTime.getSingleStringValueOrEmptyString() + Attribute.getSingleStringValueOrDefault(list,TagFromName.TimezoneOffsetFromUTC,"+0000"));
								}
								catch (Exception e) {
									slf4jlogger.error("Could not  parse combination of scan date and Radiopharmaceutical Start Time", e);
								}
//System.err.println("startDateTime = "+startDateTime+" mS "+DateTimeAttribute.getFormattedStringUTC(new java.util.Date(startDateTime)));
								if (startDateTime != 0) {
									double decayTime = (scanDateTime - startDateTime) / 1000.0;	// seconds
//System.err.println("decayTime = "+decayTime+" secs");
									double halfLife = aHalfLife.getSingleDoubleValueOrDefault(0.0);
//System.err.println("halfLife = "+halfLife+" secs");
									double injectedDose = aInjectedDose.getSingleDoubleValueOrDefault(0.0);
//System.err.println("injectedDose = "+injectedDose+" Bq");
									double decayedDose = injectedDose * Math.pow(2, -decayTime / halfLife);
//System.err.println("decayedDose = "+decayedDose);
									scaleFactorSUVbw = (weight * 1000 / decayedDose);
//System.err.println("scaleFactorSUVbw (before including rescaleSlope) = "+scaleFactorSUVbw);
									scaleFactorSUVbw = scaleFactorSUVbw * rescaleSlope;
//System.err.println("scaleFactorSUVbw (including rescaleSlope) = "+scaleFactorSUVbw);
									haveSUVbw = true;
									unitsSUVbw = "g/ml";
								}
							}
						}
						else if (units.equals("CNTS")) {
//System.err.println("have units CNTS");
							String privateCreator = Attribute.getSingleStringValueOrEmptyString(list,new AttributeTag(0x7053,0x0010)).trim();
//System.err.println("privateCreator = "+privateCreator);
							double privateSUVbwsScaleFactor = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x7053,0x1000),0.0);
//System.err.println("privateSUVbwsScaleFactor = "+privateSUVbwsScaleFactor);
							if (privateCreator.equals("Philips PET Private Group") && privateSUVbwsScaleFactor != 0.0) {
//System.err.println("scaleFactorSUVbw (before including rescaleSlope) (Philips private) = "+privateSUVbwsScaleFactor);
								scaleFactorSUVbw = privateSUVbwsScaleFactor * rescaleSlope;
//System.err.println("scaleFactorSUVbw (including rescaleSlope) = "+scaleFactorSUVbw);
								haveSUVbw = true;
								unitsSUVbw = "g/ml";
							}
							// could also check for presence of (0x7053,0x1009) scale factor to Bq/ml, and run as if Units were BQML :(
						}
						else if (units.equals("GML")) {
							scaleFactorSUVbw = rescaleSlope;
							haveSUVbw = true;
							unitsSUVbw = "g/ml";
						}
						
						// Formulas from summary at Sugawara et al, Radiology 1999 "http://radiology.rsna.org/content/213/2/521"
						// Also nicely summarized at "https://crhpacs.chw.edu/help/measuring_tools-17.htm" ("https://crhpacs.chw.edu/help/")
						
						if (haveSUVbw && weight > 0 && height > 0) {
							double scaleFactorWithoutPatientFactor = scaleFactorSUVbw / weight;
//System.err.println("scaleFactorWithoutPatientFactor = "+scaleFactorWithoutPatientFactor);
						
							if (!haveSUVbsa) {
								scaleFactorSUVbsa = scaleFactorWithoutPatientFactor * 10 * Math.pow(weight,0.425) * Math.pow(height,0.725) * 0.007184;	// NB. kg -> g * 1,000; m2 to cm2 -> 10,000 (000719)
//System.err.println("scaleFactorSUVbsa = "+scaleFactorSUVbsa);
								haveSUVbsa = true;
								unitsSUVbsa = "cm2/ml";
							}
						
							if (!haveSUVlbm) {
								if (sex.equals("M")) {
									scaleFactorSUVlbm = scaleFactorWithoutPatientFactor * (1.10 * weight - 120 * Math.pow(weight/height,2));
//System.err.println("scaleFactorSUVlbm (Male) = "+scaleFactorSUVlbm);
									haveSUVlbm = true;
									unitsSUVlbm = "g/ml";
								}
								else if (sex.equals("F")) {
									scaleFactorSUVlbm = scaleFactorWithoutPatientFactor * (1.07 * weight - 148 * Math.pow(weight/height,2));
//System.err.println("scaleFactorSUVlbm (Female) = "+scaleFactorSUVlbm);
									haveSUVlbm = true;
									unitsSUVlbm = "g/ml";
								}
							}
						
							if (!haveSUVibw) {
								if (sex.equals("M")) {
									scaleFactorSUVibw = scaleFactorWithoutPatientFactor * (48.0 + 1.06 * (height - 152));
//System.err.println("scaleFactorSUVibw (Male) = "+scaleFactorSUVibw);
									haveSUVibw = true;
									unitsSUVibw = "g/ml";
								}
								else if (sex.equals("F")) {
									scaleFactorSUVibw = scaleFactorWithoutPatientFactor * (45.5 + 0.91 * (height - 152));
//System.err.println("scaleFactorSUVibw (Female) = "+scaleFactorSUVibw);
									haveSUVibw = true;
									unitsSUVibw = "g/ml";
								}
							}
						}
					}
				}
			}
		}
		
		public boolean isValidSUVbw() {
			return haveSUVbw;
		}
		
		public double getSUVbwValue(double storedValue) {
			double suvValue = (storedValue + rescaleIntercept) * scaleFactorSUVbw;		// rescale intercept should always be zero, but just in case.
//System.err.println("getSUVbwValue() = "+suvValue);
			return suvValue;
		}
		
		public String getSUVbwUnits() {
			return unitsSUVbw;
		}
		
		public boolean isValidSUVbsa() {
			return haveSUVbsa;
		}
		
		public double getSUVbsaValue(double storedValue) {
			return (storedValue + rescaleIntercept) * scaleFactorSUVbsa;	// rescale intercept should always be zero, but just in case.
		}
		
		public String getSUVbsaUnits() {
			return unitsSUVbsa;
		}
		
		public boolean isValidSUVlbm() {
			return haveSUVlbm;
		}
		
		public double getSUVlbmValue(double storedValue) {
			return (storedValue + rescaleIntercept) * scaleFactorSUVlbm;	// rescale intercept should always be zero, but just in case.
		}
		
		public String getSUVlbmUnits() {
			return unitsSUVlbm;
		}
		
		public boolean isValidSUVibw() {
			return haveSUVibw;
		}
		
		public double getSUVibwValue(double storedValue) {
			return (storedValue + rescaleIntercept) * scaleFactorSUVibw;	// rescale intercept should always be zero, but just in case.
		}
		
		public String getSUVibwUnits() {
			return unitsSUVibw;
		}
	}
	

	private SingleSUVTransform useTransform;
	
	public SingleSUVTransform getSingleSUVTransform(int frame) {
		return useTransform;
	}
	
	/**
	 * @param	list	the dataset of an image object to be searched for transformations
	 */
	public SUVTransform(AttributeList list) {
//System.err.println("SUVTransform:");
		useTransform = new SingleSUVTransform(list);
	}

	/**
	 * Given a stored pixel value, return a string containing a description of all
	 * known SUV that can be derived from it.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	storedValue	the actual stored pixel value to look up
	 */
	public String toString(int frame,int storedValue) {
		return toString(frame,(double)storedValue);
	}

	/**
	 * Given a stored pixel value, return a string containing a description of all
	 * known SUV that can be derived from it.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	storedValue	the actual stored pixel value to look up
	 */
	public String toString(int frame,double storedValue) {
		StringBuffer sbuf = new StringBuffer();
		SingleSUVTransform t = useTransform;
		if (t.isValidSUVbw()) {
			sbuf.append("SUVbw = ");
			double value=t.getSUVbwValue(storedValue);
//System.err.println("SUVbw = "+value);
			sbuf.append(FloatFormatter.toString(value,Locale.US));
			sbuf.append(" ");
			sbuf.append(t.getSUVbwUnits());
		}
		if (t.isValidSUVbsa()) {
			sbuf.append(" ");
			sbuf.append("SUVbsa = ");
			double value=t.getSUVbsaValue(storedValue);
//System.err.println("SUVbsa = "+value);
			sbuf.append(FloatFormatter.toString(value,Locale.US));
			sbuf.append(" ");
			sbuf.append(t.getSUVbsaUnits());
		}
		if (t.isValidSUVlbm()) {
			sbuf.append(" ");
			sbuf.append("SUVlbm = ");
			double value=t.getSUVlbmValue(storedValue);
//System.err.println("SUVlbm = "+value);
			sbuf.append(FloatFormatter.toString(value,Locale.US));
			sbuf.append(" ");
			sbuf.append(t.getSUVlbmUnits());
		}
		if (t.isValidSUVibw()) {
			sbuf.append(" ");
			sbuf.append("SUVibw = ");
			double value=t.getSUVibwValue(storedValue);
//System.err.println("SUVibw = "+value);
			sbuf.append(FloatFormatter.toString(value,Locale.US));
			sbuf.append(" ");
			sbuf.append(t.getSUVibwUnits());
		}
		return sbuf.toString();
	}
}


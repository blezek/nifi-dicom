/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.anatproc.CombinedAnatomicConcepts;
import com.pixelmed.anatproc.CTAnatomy;
import com.pixelmed.anatproc.DisplayableConcept;

import com.pixelmed.dicom.*;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.OCR;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.FloatFormatter;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class CTIrradiationEventDataFromImages {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CTIrradiationEventDataFromImages.java,v 1.44 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CTIrradiationEventDataFromImages.class);

	private UIDGenerator u = new UIDGenerator();
	
	private ArrayList<String> doseScreenFilenames = new ArrayList<String>();
	
	public ArrayList<String> getDoseScreenFilenames() { return doseScreenFilenames; }
	
	private ArrayList<String> doseStructuredReportFilenames = new ArrayList<String>();
	
	public ArrayList<String> getDoseStructuredReportFilenames() { return doseStructuredReportFilenames; }
	
	public ArrayList<String> getDoseScreenOrStructuredReportFilenames() {
		return getDoseScreenOrStructuredReportFilenames(true,true);
	}
	
	public ArrayList<String> getDoseScreenOrStructuredReportFilenames(boolean includeScreen,boolean includeSR) {
		ArrayList<String> doseScreenOrStructuredReportFilenames;
		if (includeScreen && includeSR) {
			doseScreenOrStructuredReportFilenames = new ArrayList<String>(doseScreenFilenames);
			doseScreenOrStructuredReportFilenames.addAll(doseStructuredReportFilenames);
		}
		else if (includeScreen) {
			doseScreenOrStructuredReportFilenames = doseScreenFilenames;
		}
		else if (includeSR) {
			doseScreenOrStructuredReportFilenames = doseStructuredReportFilenames;
		}
		else {
			doseScreenOrStructuredReportFilenames = new ArrayList<String>();
		}
		return doseScreenOrStructuredReportFilenames;
	}
	
	public static double getDoubleValueOrZeroIfEmpty(String s) throws NumberFormatException {
		double d = 0d;
		if (s != null) {
			s = s.trim();
			if (s.length() > 0) {
				d = Double.valueOf(s).doubleValue();
			}
		}
		return d;
	}
	
	public static double getDoubleValueOrZeroIfEmptyOrInvalid(String s) {
		double d = 0d;
		try {
			d = getDoubleValueOrZeroIfEmpty(s);
		}
		catch (NumberFormatException e) {
			slf4jlogger.error("",e);
		}
		return d;
	}
	
	private ArrayList<Slice> slices = new ArrayList<Slice>();
	
	boolean organized = false;
	boolean extracted = false;
	
	private void needEverythingOrganizedAndExtracted() {
		if (!organized) {
			organizeSlicesIntoIrradiationEvents();
		}
		if (!extracted) {
			extractConsistentParametersWithinIrradiationEvents();
		}
	}

	//private Map<String,String> generatedIrradiationEventUIDByAcquisitionNumberAndStudyInstanceUID = new TreeMap<String,String>();
	private Map<String,String> generatedIrradiationEventUIDByAcquisitionTimeAndSeriesNumberAndStudyInstanceUID = new TreeMap<String,String>();
		
	//private Set<String> irradiationEventUIDs = new TreeSet<String>();
	
	private Map<String,List<Slice>> slicesByIrradiationEventUID = new HashMap<String,List<Slice>>();
	
	private String timezoneOffsetFromUTC;
	private boolean timezoneOffsetFromUTCIsClean = true;
	public String getTimezoneOffsetFromUTC() { return timezoneOffsetFromUTCIsClean ? timezoneOffsetFromUTC : null; }
	
	private String patientAge;
	private boolean patientAgeIsClean = true;
	public String getPatientAge() { return patientAgeIsClean ? patientAge : null; }
	
	private String patientSex;
	private boolean patientSexIsClean = true;
	public String getPatientSex() { return patientSexIsClean ? patientSex : null; }
	
	private String patientWeight;
	private boolean patientWeightIsClean = true;
	public String getPatientWeight() { return patientWeightIsClean ? patientWeight : null; }
	
	private String patientSize;
	private boolean patientSizeIsClean = true;
	public String getPatientSize() { return patientSizeIsClean ? patientSize : null; }
	
	private Map<String,String>  studyInstanceUIDByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> studyInstanceUIDByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  frameOfReferenceUIDByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> frameOfReferenceUIDByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  imageTypeByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> imageTypeByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  acquisitionNumberByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> acquisitionNumberByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  seriesNumberByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> seriesNumberByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  seriesDescriptionByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> seriesDescriptionByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  protocolNameByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> protocolNameByEventIsClean = new TreeMap<String,Boolean>();
		
	private Map<String,String>  imageTypeValue3ByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> imageTypeValue3ByEventIsClean = new TreeMap<String,Boolean>();
		
	private Map<String,String>  orientationByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> orientationByEventIsClean = new TreeMap<String,Boolean>();
		
	private Map<String,String>  exposureTimeByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> exposureTimeByEventIsClean = new TreeMap<String,Boolean>();

	private Map<String,String>  kvpByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> kvpByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  tubeCurrentByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> tubeCurrentByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,Double> tubeCurrentTotalByEvent = new TreeMap<String,Double>();
	private Map<String,Double> tubeCurrentCountByEvent = new TreeMap<String,Double>();
	private Map<String,Double> tubeCurrentMaximumByEvent = new TreeMap<String,Double>();
	
	private Map<String,Double> midScanTimeCountByEvent = new TreeMap<String,Double>();
	private Map<String,Double> midScanTimeMinimumByEvent = new TreeMap<String,Double>();
	private Map<String,Double> midScanTimeMaximumByEvent = new TreeMap<String,Double>();
	
	private Map<String,String>  exposureTimePerRotationByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> exposureTimePerRotationByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  nominalSingleCollimationWidthInMMByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> nominalSingleCollimationWidthInMMByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  nominalTotalCollimationWidthInMMByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> nominalTotalCollimationWidthInMMByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  sliceThicknessInMMByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> sliceThicknessInMMByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  pitchFactorByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> pitchFactorByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  exposureModulationTypeByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> exposureModulationTypeByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  estimatedDoseSavingByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> estimatedDoseSavingByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  CTDIvolByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> CTDIvolByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  DLPByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> DLPByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,DisplayableConcept> anatomyByEvent = new TreeMap<String,DisplayableConcept>();
	private Map<String,Boolean> anatomyByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String> startAcquisitionDateTimeByEvent = new TreeMap<String,String>();
	private Map<String,String> endAcquisitionDateTimeByEvent = new TreeMap<String,String>();
	
	private Map<String,Double> lowestSliceLocationByEvent = new TreeMap<String,Double>();
	private Map<String,Double> highestSliceLocationByEvent = new TreeMap<String,Double>();
	
	private Map<String,Double> lowestZLocationByEvent = new TreeMap<String,Double>();
	private Map<String,Double> highestZLocationByEvent = new TreeMap<String,Double>();
	
	private Map<String,CTAcquisitionParameters> acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey = null;
	private Map<String,CTAcquisitionParameters> acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey = null;

	private Map<String,String> overallEarliestAcquisitionDateTimeByStudy = new TreeMap<String,String>();
	private Map<String,String> overallLatestAcquisitionDateTimeByStudy   = new TreeMap<String,String>();
	
	public String getOverallEarliestAcquisitionDateTimeForStudy(String studyInstanceUID) {
		needEverythingOrganizedAndExtracted();
		return overallEarliestAcquisitionDateTimeByStudy.get(studyInstanceUID);
	}
	public String getOverallLatestAcquisitionDateTimeForStudy(String studyInstanceUID) {
		needEverythingOrganizedAndExtracted();
		return overallLatestAcquisitionDateTimeByStudy.get(studyInstanceUID);
	}
	
	private Map<String,DisplayableConcept> combinedAnatomyForStudy = new TreeMap<String,DisplayableConcept>();
	
	public DisplayableConcept getCombinedAnatomyForStudy(String studyInstanceUID) {
		needEverythingOrganizedAndExtracted();
		return combinedAnatomyForStudy.get(studyInstanceUID);
	}
	
	//private static void putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(Map<String,CodedSequenceItem> map,Map<String,Boolean> mapIsCleanForThisKey,String key,CodedSequenceItem newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(): newValue="+newValue);
	//	if (newValue != null) {
	//		Boolean clean = mapIsCleanForThisKey.get(key);
	//		if (clean == null) {
	//			clean = Boolean.valueOf(true);				// not new Boolean(true) ... see javadoc
	//			mapIsCleanForThisKey.put(key,clean);
	//		}
	//		CodedSequenceItem existingValue = map.get(key);
	//		if (existingValue == null) {
	//			map.put(key,newValue);
	//			// leave clean alone ... will either have been just added as new, or will already be true and replacing existing empty value with a non-empty value is still true
	//		}
	//		else {
	//			// already there
	//			if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
	//				clean = Boolean.valueOf(false);			// not new Boolean(false) ... see javadoc
	//				mapIsCleanForThisKey.put(key,clean);	// replace old one, can't change the value of a Boolean
	//			}
	//			// else do nothing ... is same so OK
	//		}
	//	}
	//	// else do nothing ... pretend we never saw it if no value
	//}
	
	private static void putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(Map<String,DisplayableConcept> map,String key,DisplayableConcept newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(): newValue="+newValue);
		if (newValue != null) {
			DisplayableConcept existingValue = map.get(key);
			if (existingValue == null) {
				map.put(key,newValue);
			}
			else {
				// already there
				if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
					DisplayableConcept combined = CombinedAnatomicConcepts.getCombinedConcept(existingValue,newValue,CTAnatomy.getAnatomyConcepts());
					if (combined != null) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(): Combined="+combined);
						map.put(key,combined);
					}
					else {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(): Cannot combine - set to whole body");
						map.put(key,CTAnatomy.getAnatomyConcepts().find("38266002"));	// wholee body
					}
				}
				// else do nothing ... is same so OK
			}
		}
		// else do nothing ... pretend we never saw it if no value
	}
	
	private static void putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(Map<String,DisplayableConcept> map,Map<String,Boolean> mapIsCleanForThisKey,String key,DisplayableConcept newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(): newValue="+newValue);
		if (newValue != null) {
			Boolean clean = mapIsCleanForThisKey.get(key);
			if (clean == null) {
				clean = Boolean.valueOf(true);				// not new Boolean(true) ... see javadoc
				mapIsCleanForThisKey.put(key,clean);
			}
			DisplayableConcept existingValue = map.get(key);
			if (existingValue == null) {
				map.put(key,newValue);
				// leave clean alone ... will either have been just added as new, or will already be true and replacing existing empty value with a non-empty value is still true
			}
			else {
				// already there
				if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
					DisplayableConcept combined = CombinedAnatomicConcepts.getCombinedConcept(existingValue,newValue,CTAnatomy.getAnatomyConcepts());
					if (combined != null) {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(): Combined="+combined);
						map.put(key,combined);
					}
					else {
//System.err.println("CTIrradiationEventDataFromImages.putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(): Cannot combine - flagging as unclean");
						clean = Boolean.valueOf(false);			// not new Boolean(false) ... see javadoc
						mapIsCleanForThisKey.put(key,clean);	// replace old one, can't change the value of a Boolean
					}
				}
				// else do nothing ... is same so OK
			}
		}
		// else do nothing ... pretend we never saw it if no value
	}

	//private static void putNumericStringValueByStringIndexInStringMapIfNumericSortIsEarlier(Map<String,String> map,String key,String newValueString) {
	//	if (newValueString != null && !newValueString.equals("")) {
	//		try {
	//			Double newValue = new Double(newValueString);
	//			String existingValueString = map.get(key);
	//			if (existingValueString == null) {
	//				map.put(key,newValueString);
	//			}
	//			else {
	//				Double existingValue = new Double(existingValueString);
	//				if (newValue.compareTo(existingValue) < 0) {
	//					map.put(key,newValueString);
	//				}
	//			}
	//		}
	//		catch (NumberFormatException e) {
	//			// do nothing
	//		}
	//	}
	//}
	
	private static void putNumericStringValueByStringIndexIfNumericSortIsEarlier(Map<String,Double> map,String key,String newValueString) {
		if (newValueString != null && !newValueString.equals("")) {
			try {
				Double newValue = new Double(newValueString);
				Double existingValue = map.get(key);
				if (existingValue == null) {
					map.put(key,newValue);
				}
				else {
					if (newValue.compareTo(existingValue) < 0) {
						map.put(key,newValue);
					}
				}
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	private static void putNumericStringValueByStringIndexIfNumericSortIsLater(Map<String,Double> map,String key,String newValueString) {
		if (newValueString != null && !newValueString.equals("")) {
			try {
				Double newValue = new Double(newValueString);
				Double existingValue = map.get(key);
				if (existingValue == null) {
					map.put(key,newValue);
				}
				else {
					if (newValue.compareTo(existingValue) > 0) {
						map.put(key,newValue);
					}
				}
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	private static void putStringValueByStringIndexIfLexicographicSortIsEarlier(Map<String,String> map,String key,String newValue) {
		if (newValue != null && !newValue.equals("")) {
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
			}
			else {
				if (newValue.compareTo(existingValue) < 0) {
					map.put(key,newValue);
				}
			}
		}
	}
	
	private static void putStringValueByStringIndexIfLexicographicSortIsLater(Map<String,String> map,String key,String newValue) {
		if (newValue != null && !newValue.equals("")) {
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
			}
			else {
				if (newValue.compareTo(existingValue) > 0) {
					map.put(key,newValue);
				}
			}
		}
	}
	
	private static void putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(Map<String,String> map,Map<String,Boolean> mapIsCleanForThisKey,String key,String newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(): newValue="+newValue);
		if (newValue != null && !newValue.equals("")) {
			Boolean clean = mapIsCleanForThisKey.get(key);
			if (clean == null) {
				clean = Boolean.valueOf(true);				// not new Boolean(true) ... see javadoc
				mapIsCleanForThisKey.put(key,clean);
			}
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
				// leave clean alone ... will either have been just added as new, or will already be true and replacing existing empty value with a non-empty value is still true
			}
			else {
				// already there
				if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
					clean = Boolean.valueOf(false);			// not new Boolean(false) ... see javadoc
					mapIsCleanForThisKey.put(key,clean);	// replace old one, can't change the value of a Boolean
				}
				// else do nothing ... is same so OK
			}
		}
		// else do nothing ... pretend we never saw it if no value
	}
	
	private static boolean booleanExistsInMapAndIsTrue(Map<String,Boolean>map,String key) {
		Boolean flag = map.get(key);
		return flag != null && flag.booleanValue();
	}
	
	public CTAcquisitionParameters getAcquisitionParametersForIrradiationEvent(String uid) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): uid "+uid);		
		needEverythingOrganizedAndExtracted();

		String useOrientation = booleanExistsInMapAndIsTrue(orientationByEventIsClean,uid) ? orientationByEvent.get(uid) : null;
		if (useOrientation == null || !useOrientation.equals(DescriptionFactory.AXIAL_LABEL)) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): ignoring non-axial ("+useOrientation+") event "+uid);		
			return null;	// do not want anything except axial or near-axial slices (will need to consider impact on direct coronal sinus studies :( )
		}

		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
		formatter.setGroupingUsed(false);					// i.e., no comma at thousands

		CTScanType useScanType = CTScanType.UNKNOWN;
		{
			if (booleanExistsInMapAndIsTrue(imageTypeValue3ByEventIsClean,uid)) {
				String imageTypeValue3 = imageTypeValue3ByEvent.get(uid);
				if (imageTypeValue3 != null && imageTypeValue3.toUpperCase(java.util.Locale.US).equals("LOCALIZER")) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): setting ScanType of LOCALIZER because of Image Type Value 3 for uid "+uid);
					useScanType = CTScanType.LOCALIZER;
				}
				// else other legal value is AXIAL, which doesn't actually mean AXIAL in the CTScanType sense, just not LOCALIZER
			}
		}
		String usePitchFactor = booleanExistsInMapAndIsTrue(pitchFactorByEventIsClean,uid) ? pitchFactorByEvent.get(uid) : null;
		if (!useScanType.equals(CTScanType.LOCALIZER)) {
			if (usePitchFactor != null && usePitchFactor.length() > 0) {
				try {
					double pitchFactorValue = Double.parseDouble(usePitchFactor);
					if (pitchFactorValue == 0) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): setting ScanType of STATIONARY because not LOCALIZER and pitch factor is zero for uid "+uid);
						useScanType = CTScanType.STATIONARY;
					}
				}
				catch (NumberFormatException e) {
					// else ignore it
				}
			}
		}

		boolean isLocalizer = useScanType.equals(CTScanType.LOCALIZER);
		
		String useTotalExposureTime =  null;
		if (isLocalizer) {
			if (booleanExistsInMapAndIsTrue(exposureTimeByEventIsClean,uid)) {
				useTotalExposureTime = exposureTimeByEvent.get(uid);
			}
		}
		else {
			//  ExposureTime is per rotation so ignore it
			Double count   = midScanTimeCountByEvent.get(uid);
			Double minimum = midScanTimeMinimumByEvent.get(uid);
			Double maximum = midScanTimeMaximumByEvent.get(uid);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeCountByEvent "+count);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeMinimumDouble "+minimum);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeMaximumDouble "+maximum);		
			if (count != null && minimum != null && maximum != null) {
				double countValue   = count.doubleValue();
				double minimumValue = minimum.doubleValue();
				double maximumValue = maximum.doubleValue();
				double totalExposureTime = (maximumValue - minimumValue)*(countValue+1)/countValue;	// attempt to compensate for time before midScan for 1st and after for last slice
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): totalExposureTime "+totalExposureTime);		
				if (totalExposureTime > 0) {
					useTotalExposureTime = formatter.format(totalExposureTime);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): useTotalExposureTime "+useTotalExposureTime);
				}
			}
		}
		
		String useKVP = booleanExistsInMapAndIsTrue(kvpByEventIsClean,uid) ? kvpByEvent.get(uid) : null;
		
		String useTubeCurrent = null;
		String useTubeCurrentMaximum = null;
		if (booleanExistsInMapAndIsTrue(tubeCurrentByEventIsClean,uid)) {
			useTubeCurrent = tubeCurrentByEvent.get(uid);
			useTubeCurrentMaximum = useTubeCurrent;
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): constant TubeCurrent "+useTubeCurrent);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): constant TubeCurrentMaximum "+useTubeCurrentMaximum);
		}
		else {
			Double total = tubeCurrentTotalByEvent.get(uid);
			Double count = tubeCurrentCountByEvent.get(uid);
			Double maximum = tubeCurrentMaximumByEvent.get(uid);
			if (total != null && count != null) {
				double countValue = count.doubleValue();
				if (countValue > 0) {
					double mean = total.doubleValue()/countValue;
					useTubeCurrent = formatter.format(mean);
				}
			}
			if (maximum != null) {
				useTubeCurrentMaximum = formatter.format(maximum.doubleValue());
			}
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): computed TubeCurrent "+useTubeCurrent);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): computed TubeCurrentMaximum "+useTubeCurrentMaximum);
		}
		String useExposureTimePerRotation = null;
		if (!isLocalizer) {
			useExposureTimePerRotation = booleanExistsInMapAndIsTrue(exposureTimePerRotationByEventIsClean,uid) ? exposureTimePerRotationByEvent.get(uid) : null;
			// the Exposure Time attribute in old image IODs is actually the exposure time per rotation (and has already been converted from milliseconds to seconds)
			if (useExposureTimePerRotation == null || useExposureTimePerRotation.trim().length() == 0) {
				if (booleanExistsInMapAndIsTrue(exposureTimeByEventIsClean,uid)) {
					useExposureTimePerRotation = exposureTimeByEvent.get(uid);
				}
			}
		}
		
		CodedSequenceItem useAnatomy = null;
		try {
			DisplayableConcept anatomy = booleanExistsInMapAndIsTrue(anatomyByEventIsClean,uid) ? anatomyByEvent.get(uid) : null;
			if (anatomy != null) {
				useAnatomy = anatomy.getCodedSequenceItem();
			}
		}
		catch (DicomException e) {
			slf4jlogger.error("",e);
		}
		
		String useProtocolName = booleanExistsInMapAndIsTrue(protocolNameByEventIsClean,uid) ? protocolNameByEvent.get(uid) : null;
				
		String useComment = booleanExistsInMapAndIsTrue(seriesDescriptionByEventIsClean,uid) ? seriesDescriptionByEvent.get(uid) : null;
				
		String useNominalSingleCollimationWidthInMM = booleanExistsInMapAndIsTrue(nominalSingleCollimationWidthInMMByEventIsClean,uid) ? nominalSingleCollimationWidthInMMByEvent.get(uid) : null;
		String useNominalTotalCollimationWidthInMM  = booleanExistsInMapAndIsTrue(nominalTotalCollimationWidthInMMByEventIsClean ,uid) ?  nominalTotalCollimationWidthInMMByEvent.get(uid) : null;
		
		String useLengthOfReconstructableVolumeInMM = null;
		String useTopZLocationOfReconstructableVolume    = null;
		String useBottomZLocationOfReconstructableVolume = null;
		if (!isLocalizer) {
			Double bottomZ = lowestZLocationByEvent.get(uid);
			Double topZ    = highestZLocationByEvent.get(uid);
			if (bottomZ != null && topZ != null) {
				double sliceThickness = 0;
				double halfSliceThickness = 0;
				if (booleanExistsInMapAndIsTrue(sliceThicknessInMMByEventIsClean,uid)) {
					try {
						sliceThickness = Double.parseDouble(sliceThicknessInMMByEvent.get(uid));
						halfSliceThickness = sliceThickness/2;
					}
					catch (NumberFormatException e) {
						slf4jlogger.error("",e);
					}
				}
				
				double dBottomZ = bottomZ.doubleValue();
				double dTopZ = topZ.doubleValue();
				double range = dTopZ - dBottomZ;
				if (range < 0) {
					// this should never happen, since should have been accounted for in extraction of lowest and highest Z
					double temp = dTopZ;
					dTopZ = dBottomZ;
					dBottomZ = temp;
					range = - range;
				}
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): dBottomZ before slice thickness "+dBottomZ);
				dBottomZ -= halfSliceThickness;
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): dBottomZ after slice thickness "+dBottomZ);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): dTopZ before slice thickness "+dTopZ);
				dTopZ    += halfSliceThickness;
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): dTopZ after slice thickness "+dTopZ);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): range before slice thickness "+range);
				range    += sliceThickness;
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): range after slice thickness "+range);
					
				useBottomZLocationOfReconstructableVolume = locationValueToStringSigned(dBottomZ);
				useTopZLocationOfReconstructableVolume = locationValueToStringSigned(dTopZ);
				useLengthOfReconstructableVolumeInMM = formatter.format(range);
			}
		}
		
		// Add FoR UID if we have it, regardless of whether we need it, since even though it is MC IFF in RDSR, that is handled in RDSR creation
		String useFrameOfReferenceUID = null;
		if (booleanExistsInMapAndIsTrue(frameOfReferenceUIDByEventIsClean,uid)) {
			useFrameOfReferenceUID = frameOfReferenceUIDByEvent.get(uid);
		}
		
		return new CTAcquisitionParameters(uid,useScanType,useAnatomy,useProtocolName,useComment,useTotalExposureTime,
						useLengthOfReconstructableVolumeInMM/*scanningLength will be underestimated (no overranging for helical) by this, but will be overridden later from DLP/CTDIvol*10 if greater*/,
						useLengthOfReconstructableVolumeInMM,
						null/*exposedRangeInMM*/,
						useTopZLocationOfReconstructableVolume,useBottomZLocationOfReconstructableVolume,null/*topZLocationOfScanningLength*/,null/*bottomZLocationOfScanningLength*/,
						useFrameOfReferenceUID,
						useNominalSingleCollimationWidthInMM,useNominalTotalCollimationWidthInMM,usePitchFactor,
						useKVP,useTubeCurrent,useTubeCurrentMaximum,useExposureTimePerRotation);
	}
	
	public CTAcquisitionParameters getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(String seriesNumberAndScanRangeAndStudyInstanceUIDKey) {
		if (acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey == null) {
			needEverythingOrganizedAndExtracted();
			acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey = new TreeMap<String,CTAcquisitionParameters>();
			for (String uid : slicesByIrradiationEventUID.keySet()) {
				String useSeriesNumber     = booleanExistsInMapAndIsTrue(seriesNumberByEventIsClean,uid) ? seriesNumberByEvent.get(uid) : "";
				String useStartLocation    = locationValueToStringIS(highestSliceLocationByEvent.get(uid));
				String useEndLocation      = locationValueToStringIS(lowestSliceLocationByEvent.get(uid));
				String useStudyInstanceUID = booleanExistsInMapAndIsTrue(studyInstanceUIDByEventIsClean,uid) ? studyInstanceUIDByEvent.get(uid) : "";
				String key = useSeriesNumber+"+"+useStartLocation+"+"+useEndLocation+"+"+useStudyInstanceUID;
				if (!key.equals("+++")) {
					CTAcquisitionParameters ap = getAcquisitionParametersForIrradiationEvent(uid);
					if (ap != null) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): adding key="+key+" with parameters="+ap);
						CTAcquisitionParameters apAlreadyThere = acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey.get(key);
						if (apAlreadyThere != null) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): Aaargh ! key="+key+" already exists in map ...");
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): already there = "+apAlreadyThere);
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): new = "+ap);
slf4jlogger.info("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): just using the first one encountered - may be non-deterministic :(");
						}
						acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey.put(key,ap);
					}
				}
			}
		}
		CTAcquisitionParameters ap = acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey.get(seriesNumberAndScanRangeAndStudyInstanceUIDKey);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): looking for key="+seriesNumberScanRangeKey+" found parameters="+ap);
		return ap;
	}
	
	public CTAcquisitionParameters getAcquisitionParametersByAcquisitionNumberAndStudyInstanceUID(String acquisitionNumberAndStudyInstanceUIDKey) {
		if (acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey == null) {
			needEverythingOrganizedAndExtracted();
			acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey = new TreeMap<String,CTAcquisitionParameters>();
			for (String uid : slicesByIrradiationEventUID.keySet()) {
				String useAcquisitionNumber = booleanExistsInMapAndIsTrue(acquisitionNumberByEventIsClean,uid) ? acquisitionNumberByEvent.get(uid) : "";
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): useAcquisitionNumber="+useAcquisitionNumber+" for event uid="+uid);
				String useStudyInstanceUID = booleanExistsInMapAndIsTrue(studyInstanceUIDByEventIsClean,uid) ? studyInstanceUIDByEvent.get(uid) : "";
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByStudyInstanceUID(): useStudyInstanceUID="+useStudyInstanceUID+" for event uid="+uid);
				String key = useAcquisitionNumber+"+"+useStudyInstanceUID;
				if (!key.equals("+")) {
					boolean addOrReplace = true;
					CTAcquisitionParameters ap = getAcquisitionParametersForIrradiationEvent(uid);
					if (ap == null) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): getAcquisitionParametersForIrradiationEvent("+uid+") is null - e.g., not axial");
						addOrReplace = false;
					}
					else {
						CTAcquisitionParameters apAlreadyThere = acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.get(key);
						if (apAlreadyThere != null) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): Aaargh ! key="+key+" already exists in map ...");
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): already there = "+apAlreadyThere);
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): new = "+ap);
							if (apAlreadyThere.equalsApartFromIrradiationEventUID(ap)) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - are equal apart from event uid");
							}
							else {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): are not equal");
								addOrReplace = false;
								CTScanType scanTypeNew = ap.getScanType();
								CTScanType scanTypeAlreadyThere = apAlreadyThere.getScanType();
								// if same event contains localizer and non-localizers, we want the non-localizer information
								if (scanTypeNew != null && scanTypeAlreadyThere != null) {
									boolean isLocalizerNew = scanTypeNew.equals(CTScanType.LOCALIZER);
									boolean isLocalizerAlreadyThere = scanTypeAlreadyThere.equals(CTScanType.LOCALIZER);
									if (isLocalizerAlreadyThere && !isLocalizerNew) {
										addOrReplace = true;	
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - replacing localizer information with non-localizer information");
									}
									else if (!isLocalizerAlreadyThere && isLocalizerNew) {
										addOrReplace = false;	// if same event contains localizer and non-localizers, we want the non-localizer information
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - not replacing non-localizer information with localizer information");
									}
									else {
									// else are both of same type but not equal ... e.g., different reconstructed series with different current mean/max ... ignore one of them
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): same type but not equal");
										double lengthNew = getDoubleValueOrZeroIfEmptyOrInvalid(ap.getLengthOfReconstructableVolumeInMM());
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): lengthNew = "+lengthNew);
										double lengthAlreadyThere = getDoubleValueOrZeroIfEmptyOrInvalid(apAlreadyThere.getLengthOfReconstructableVolumeInMM());
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): lengthAlreadyThere = "+lengthAlreadyThere);
										if (lengthNew > lengthAlreadyThere) {
											addOrReplace = true;	
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - replacing with longer length of reconstruction");
										}
										else if (lengthNew < lengthAlreadyThere) {
											addOrReplace = false;	
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - not replacing with shorter length of reconstruction");
										}
										else {
slf4jlogger.info("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): same length but not equal :( just using the first one encountered - may be non-deterministic :(");
										}
									}
								}
							}
						}
					}
//System.err.println("");
					if (addOrReplace) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): adding key="+key+" with parameters="+ap);
						acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.put(key,ap);
					}
				}
			}
		}
		CTAcquisitionParameters ap = acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.get(acquisitionNumberAndStudyInstanceUIDKey);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): looking for key="+acquisitionNumberAndStudyInstanceUIDKey+" found parameters="+ap);
		return ap;
	}
	
	public CTIrradiationEventDataFromImages() {
	}
	
	public CTIrradiationEventDataFromImages(String path) {
		add(path);
	}
	
	public CTIrradiationEventDataFromImages(Vector<String> paths) {
		for (int j=0; j< paths.size(); ++j) {
			add(paths.get(j));
		}
	}
	
	public void add(String path) {
		add(new File(path));
	}
	
	protected Set<File>filesAlreadyDone = new HashSet<File>();
		
	public void add(File file) {
//System.err.println("CTIrradiationEventDataFromImages.add(): add() file "+file);
		if (filesAlreadyDone.contains(file)) {
			// could be loop in file system caused by symbolic links, DICOMDIR with bad filename that is a directory or empty component added to parent and not explicitly handled
slf4jlogger.info("CTIrradiationEventDataFromImages.add(): ignoring request to handle path already processed {}",file);
		}
		else {
			filesAlreadyDone.add(file);
			if (file.exists()) {
//System.err.println("CTIrradiationEventDataFromImages.add(): file exists "+file);
				if (file.isDirectory()) {
//System.err.println("CTIrradiationEventDataFromImages.add(): is directory "+file);
					ArrayList<File> files = FileUtilities.listFilesRecursively(file);
					for (File f : files) {
						add(f);
					}
				}
				else if (file.isFile() && file.getName().toUpperCase(java.util.Locale.US).equals("DICOMDIR")) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Doing DICOMDIR from "+file);
					try {
						AttributeList list = new AttributeList();
						list.read(file.getCanonicalPath());
						DicomDirectory dicomDirectory = new DicomDirectory(list);
						HashMap allDicomFiles = dicomDirectory.findAllContainedReferencedFileNamesAndTheirRecords(file.getParentFile().getCanonicalPath());
//System.err.println("CTIrradiationEventDataFromImages.add(): Referenced files: "+allDicomFiles);
						Iterator it = allDicomFiles.keySet().iterator();
						while (it.hasNext()) {
							String doFileName = (String)it.next();
							if (doFileName != null) {
								File doFile = new File(doFileName);
								if (doFile.isDirectory()) {
slf4jlogger.info("CTIrradiationEventDataFromImages.add(): ignoring directory (rather than file) referenced from within DICOMDIR {}",doFileName);
								}
								else if (doFile.getName().toUpperCase(java.util.Locale.US).equals("DICOMDIR")) {
slf4jlogger.info("CTIrradiationEventDataFromImages.add(): ignoring DICOMDIR referenced from within DICOMDIR");
								}
								else {
//System.err.println("CTIrradiationEventDataFromImages.add(): adding DICOMDIR referenced file "+doFileName);
									add(doFile);
								}
							}
						}
					}
					catch (IOException e) {
						slf4jlogger.error("",e);
					}
					catch (DicomException e) {
						slf4jlogger.error("",e);
					}
				}
				else if (file.isFile() && DicomFileUtilities.isDicomOrAcrNemaFile(file)) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Doing file "+file);
					try {
						AttributeList list = new AttributeList();
						list.read(file.getCanonicalPath(),null,true,true,TagFromName.PixelData);
						String irradiationEventUID = "";
						CodedSequenceItem srDocumentTitle = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
						if (OCR.isDoseScreenInstance(list)
						 || ExposureDoseSequence.isPhilipsDoseScreenInstance(list)
						) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Found dose screen in file "+file);
							doseScreenFilenames.add(file.getCanonicalPath());
						}
						else {
							String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
							if (SOPClass.isStructuredReport(sopClassUID)
								&& srDocumentTitle != null
								&& srDocumentTitle.getCodingSchemeDesignator().equals("DCM")
								&& srDocumentTitle.getCodeValue().equals("113701")		// "X-Ray Radiation Dose Report"
							) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Found dose SR in file "+file);
								doseStructuredReportFilenames.add(file.getCanonicalPath());
							}
							else if (sopClassUID.equals(SOPClass.CTImageStorage)) {
								organized = false;	// reset this if anything is added, even if previously organized and extracted
								extracted = false;
								Slice slice = new Slice(list);
//System.err.println("CTIrradiationEventDataFromImages.add(): Adding slice from file "+file+"\n"+slice);
								slices.add(slice);
							}
							else {
//System.err.println("CTIrradiationEventDataFromImages.add(): Ignoring unwanted SOP Class UID "+sopClassUID+" file "+file);
							}
						}
					}
					catch (Exception e) {
						// probably wasn't a DICOM file after all, so don't sweat it
						slf4jlogger.error("",e);
					}
				}
				else {
					// wasn't a DICOM file after all, so don't sweat it
//System.err.println("CTIrradiationEventDataFromImages.add(): Not doing non-DICOM file "+file);
				}
			}
		}
	}

	private void organizeSlicesIntoIrradiationEvents() {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents():");
		if (!slices.isEmpty()) {
			ArrayList<Slice> slicesWithoutExplicitIrradiationEvent = new ArrayList<Slice>();
			for (Slice s : slices) {
				if (s.irradiationEventUID.length() > 0) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Using supplied IrradiationEventUID "+s.irradiationEventUID+" for AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation+" SeriesNumber = "+s.seriesNumber+" AcquisitionNumber = "+s.acquisitionNumber);
					//irradiationEventUIDs.add(s.irradiationEventUID);
					List<Slice> event = slicesByIrradiationEventUID.get(s.irradiationEventUID);
					if (event == null) {
						event = new ArrayList<Slice>();
						slicesByIrradiationEventUID.put(s.irradiationEventUID,event);
					}
					event.add(s);
				}
				else {
					slicesWithoutExplicitIrradiationEvent.add(s);
				}
			}
			if (!slicesWithoutExplicitIrradiationEvent.isEmpty()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): have instances without IrradiationEventUID");
				Map<String,List> slicesSeparatedByStudyAndSeries =  new HashMap<String,List>();
				for (Slice s : slicesWithoutExplicitIrradiationEvent) {
					String key = s.studyInstanceUID + "+" + s.seriesInstanceUID;	// do NOT include AcquisitionNumber, else over splits for GE
					List<Slice> instances = slicesSeparatedByStudyAndSeries.get(key);
					if (instances == null) {
						instances = new ArrayList<Slice>();
						slicesSeparatedByStudyAndSeries.put(key,instances);
					}
					instances.add(s);
				}
				//for (Collection<Slice> acquisition : slicesSeparatedByStudyAndSeries.values()) {
				for (String key : slicesSeparatedByStudyAndSeries.keySet()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Processing study+series+acquisition = "+key);
					Collection<Slice> acquisition =  slicesSeparatedByStudyAndSeries.get(key);
					// partition by AcquisitionDateTime ... this works whether all slices have same acquisition, some are in batches, or even if every slice has a different acquisition time
					SortedMap<String,List<Slice>> separatedByAcquisitionDateTime = new TreeMap<String,List<Slice>>();
					for (Slice s : acquisition) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Check for same AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation);
						List<Slice> instancesWithSameAcquisitionDateTime = separatedByAcquisitionDateTime.get(s.acquisitionDateTime);
						if (instancesWithSameAcquisitionDateTime == null) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Creating new group for AcquisitionDateTime = "+s.acquisitionDateTime);
							instancesWithSameAcquisitionDateTime = new ArrayList<Slice>();
							separatedByAcquisitionDateTime.put(s.acquisitionDateTime,instancesWithSameAcquisitionDateTime);
						}
						instancesWithSameAcquisitionDateTime.add(s);
					}
					// then walk through each AcquisitionDateTime group to merge them unless there is spatial overlap ...
					List<List<Slice>> events = new ArrayList<List<Slice>>();
					{
						List<Slice> event = new ArrayList<Slice>();
						events.add(event);
						double  lowestSliceLocationInCurrentGroup = 0;	// intializer avoids warning, but is never used (sliceLocationLimitsInCurrentGroupNotYetInitialized)
						double highestSliceLocationInCurrentGroup = 0;
						boolean sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
						double  lowestSliceLocationInPreviousGroup = 0;
						double highestSliceLocationInPreviousGroup = 0;
						boolean sliceLocationLimitsInPreviousGroupNotYetInitialized = true;
						for (List<Slice> instancesWithSameAcquisitionDateTime : separatedByAcquisitionDateTime.values()) {	// value set will be returned in AcquisitionDateTime order
							{
								for (Slice s : instancesWithSameAcquisitionDateTime) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation+" ZLocation = "+s.zLocation);
									String useSliceLocation = s.sliceLocation;
									if (s.sliceLocation == null || s.sliceLocation.length() == 0) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): using zLocation instead of empty or missing SliceLocation = "+s.zLocation);
										useSliceLocation = s.zLocation;
									}
									if (useSliceLocation != null && useSliceLocation.length() > 0) {
										try {
											double thisSliceLocation = Double.parseDouble(useSliceLocation);
											if (sliceLocationLimitsInCurrentGroupNotYetInitialized) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): starting new range: SliceLocation = "+useSliceLocation);
												sliceLocationLimitsInCurrentGroupNotYetInitialized = false;
												lowestSliceLocationInCurrentGroup = thisSliceLocation;
												highestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else if (thisSliceLocation < lowestSliceLocationInCurrentGroup) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): extending lower limit of range: SliceLocation = "+useSliceLocation);
												lowestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else if (thisSliceLocation > highestSliceLocationInCurrentGroup) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): extending upper limit of range: SliceLocation = "+useSliceLocation);
												highestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): within existing range SliceLocation = "+useSliceLocation);
												// within existing range so do nothing
											}
										}
										catch (NumberFormatException e) {
											System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Bad SliceLocation in SOP Instance "+s.sopInstanceUID);
											slf4jlogger.error("",e);
										}
									}
									else {
										//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Missing SliceLocation in SOP Instance "+s.sopInstanceUID);
									}
								}
							}
							// now have range for current acquisitionDateTime group ... compare with prior to see if overlap
							if (sliceLocationLimitsInCurrentGroupNotYetInitialized) {
								// couldn't initialize range ... ignore
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): could not determine SliceLocation range");
								//event.addAll(instancesWithSameAcquisitionDateTime);
							}
							else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): lowest SliceLocation = "+lowestSliceLocationInCurrentGroup+" highest SliceLocation = "+highestSliceLocationInCurrentGroup);
								if (sliceLocationLimitsInPreviousGroupNotYetInitialized) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): no previous group to compare with");
									event.addAll(instancesWithSameAcquisitionDateTime);
									sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
									sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
									 lowestSliceLocationInPreviousGroup = lowestSliceLocationInCurrentGroup;
									highestSliceLocationInPreviousGroup = highestSliceLocationInCurrentGroup;
								}
								else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): comparing with previous group: lowest SliceLocation = "+lowestSliceLocationInPreviousGroup+" highest SliceLocation = "+highestSliceLocationInPreviousGroup);
									// check for overlap
									if (highestSliceLocationInPreviousGroup < lowestSliceLocationInCurrentGroup
									 || highestSliceLocationInCurrentGroup  < lowestSliceLocationInPreviousGroup) {
										// no overlap, so expand range to merge current and previous group
										event.addAll(instancesWithSameAcquisitionDateTime);
										sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
										sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
										 lowestSliceLocationInPreviousGroup =  lowestSliceLocationInPreviousGroup <  lowestSliceLocationInCurrentGroup ?  lowestSliceLocationInPreviousGroup :  lowestSliceLocationInCurrentGroup;
										highestSliceLocationInPreviousGroup = highestSliceLocationInPreviousGroup > highestSliceLocationInCurrentGroup ? highestSliceLocationInPreviousGroup : highestSliceLocationInCurrentGroup;
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): expanding range to merge current and previous: now lowest SliceLocation = "+lowestSliceLocationInPreviousGroup+" highest SliceLocation = "+highestSliceLocationInPreviousGroup);
									}
									else {
										// overlap, so begin new event
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): overlap, so creating new event");
										event = new ArrayList<Slice>();
										events.add(event);
										event.addAll(instancesWithSameAcquisitionDateTime);
										sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
										sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
										 lowestSliceLocationInPreviousGroup =  lowestSliceLocationInCurrentGroup;
										highestSliceLocationInPreviousGroup = highestSliceLocationInCurrentGroup;
									}
								}
							}
						}
					}
					// now we have the events we need
					Iterator<List<Slice>> eventsIterator = events.iterator();	// use explicit Iterator rather than for loop since need Iterator.remove() to avoid ConcurrentModificationException
					//for (List<Slice> event : events) {
					while (eventsIterator.hasNext()) {
						List<Slice> event = eventsIterator.next();
						if (event.isEmpty()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Dropping empty event");
							eventsIterator.remove();	// e.g., had no SliceLocation information (such as Siemens MPR)
						}
						else {
							String irradiationEventUID = "";
							try {
								irradiationEventUID = u.getAnotherNewUID();
							}
							catch (DicomException e) {
								slf4jlogger.error("",e);
							}
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): IrradiationEventUID = "+irradiationEventUID);
							//irradiationEventUIDs.add(irradiationEventUID);
							slicesByIrradiationEventUID.put(irradiationEventUID,event);
							for (Slice s : event) {
								s.irradiationEventUID = irradiationEventUID;
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): \tAcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation+" SeriesNumber = "+s.seriesNumber+" AcquisitionNumber = "+s.acquisitionNumber);
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): \tanatomy = "+s.anatomyCodedSequenceItem);
							}
						}
					}
				}
			}
		}
		organized = true;
	}
	
	private class Slice {
		String irradiationEventUID;
		String timezoneOffsetFromUTC;
		String patientAge;
		String patientSex;
		String patientWeight;
		String patientSize;
		String studyInstanceUID;
		String seriesInstanceUID;
		String sopInstanceUID;
		String seriesNumber;
		String acquisitionNumber;
		String seriesDescription;
		String protocolName;
		
		String imageType;
		String imageTypeValue3;
		String orientation;
		
		String exposureTimeInSeconds;
		String kvp;
		String tubeCurrent;
		String midScanTime;
		String exposureTimePerRotation;
		String nominalSingleCollimationWidth;
		String nominalTotalCollimationWidth;
		String pitchFactor;
		
		String acquisitionDateTime;
		String sliceLocation;
		
		String zLocation;
		String sliceThickness;
		String frameOfReferenceUID;

		String exposureModulationType;
		String estimatedDoseSaving;
		String CTDIvol;
		String DLP;
		
		DisplayableConcept anatomy;
				
		public String toString() {
			StringBuffer buf = new StringBuffer();

			buf.append("Slice:\n");
			buf.append("\tIrradiationEventUID = "); buf.append(irradiationEventUID); buf.append("\n");
			buf.append("\tTimezoneOffsetFromUTC = "); buf.append(timezoneOffsetFromUTC); buf.append("\n");
			buf.append("\tPatientAge = "); buf.append(patientAge); buf.append("\n");
			buf.append("\tPatientSex = "); buf.append(patientSex); buf.append("\n");
			buf.append("\tPatientWeight = "); buf.append(patientWeight); buf.append("\n");
			buf.append("\tPatientSize = "); buf.append(patientSize); buf.append("\n");
			buf.append("\tStudyInstanceUID = "); buf.append(studyInstanceUID); buf.append("\n");
			buf.append("\tSeriesInstanceUID = "); buf.append(seriesInstanceUID); buf.append("\n");
			buf.append("\tSOPInstanceUID = "); buf.append(sopInstanceUID); buf.append("\n");
			buf.append("\tSeriesNumber = "); buf.append(seriesNumber); buf.append("\n");
			buf.append("\tAcquisitionNumber = "); buf.append(acquisitionNumber); buf.append("\n");
			buf.append("\tSeriesDescription = "); buf.append(seriesDescription); buf.append("\n");
			buf.append("\tProtocolName = "); buf.append(protocolName); buf.append("\n");
			
			buf.append("\tImageType = "); buf.append(imageType); buf.append("\n");
			buf.append("\tImageTypeValue3 = "); buf.append(imageTypeValue3); buf.append("\n");
			buf.append("\tOrientation = "); buf.append(orientation); buf.append("\n");
			
			buf.append("\tExposureTime = "); buf.append(exposureTimeInSeconds); buf.append("\n");
			buf.append("\tKVP = "); buf.append(kvp); buf.append("\n");
			buf.append("\tTubeCurrent = "); buf.append(tubeCurrent); buf.append("\n");
			buf.append("\tMidScanTime = "); buf.append(midScanTime); buf.append("\n");
			buf.append("\tExposureTimePerRotation = "); buf.append(exposureTimePerRotation); buf.append("\n");
			buf.append("\tNominalSingleCollimationWidthInMM = "); buf.append(nominalSingleCollimationWidth); buf.append("\n");
			buf.append("\tNominalTotalCollimationWidthInMM = "); buf.append(nominalTotalCollimationWidth); buf.append("\n");
			buf.append("\tPitchFactor = "); buf.append(pitchFactor); buf.append("\n");
			
			buf.append("\tAcquisitionDateTime = "); buf.append(acquisitionDateTime); buf.append("\n");
			buf.append("\tSliceLocation = "); buf.append(sliceLocation); buf.append("\n");
			
			buf.append("\tZLocation = "); buf.append(zLocation); buf.append("\n");
			buf.append("\tSliceThickness = "); buf.append(sliceThickness); buf.append("\n");
			buf.append("\tFrameOfReferenceUID = "); buf.append(frameOfReferenceUID); buf.append("\n");
			
			buf.append("\tExposureModulationType = "); buf.append(exposureModulationType); buf.append("\n");
			buf.append("\tEstimatedDoseSaving = "); buf.append(estimatedDoseSaving); buf.append("\n");
			buf.append("\tCTDIvol = "); buf.append(CTDIvol); buf.append("\n");
			buf.append("\tDLP = "); buf.append(DLP); buf.append("\n");
			
			buf.append("\tAnatomy = "); buf.append(anatomy); buf.append("\n");
		
			return buf.toString();
		}

		Slice(AttributeList list) {
			irradiationEventUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.IrradiationEventUID);
			timezoneOffsetFromUTC = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.TimezoneOffsetFromUTC);
			patientAge = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientAge);
			patientSex = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientSex);
			patientWeight = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientWeight);
			patientSize = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientSize);
			studyInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.StudyInstanceUID);
			seriesInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesInstanceUID);
			sopInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SOPInstanceUID);
			seriesNumber = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesNumber);
			acquisitionNumber = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.AcquisitionNumber);
			seriesDescription = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesDescription);
			protocolName = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.ProtocolName);
			
			imageType = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.ImageType);
			try {
				imageTypeValue3 = "";
				Attribute aImageType = list.get(TagFromName.ImageType);
				if (aImageType != null && aImageType.getVM() >= 3) {
					String[] vImageType = aImageType.getStringValues();
					imageTypeValue3 = vImageType[2];
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			
			orientation = DescriptionFactory.makeImageOrientationLabelFromImageOrientationPatient(list);
			
			{
				String exposureTimeInMilliSeconds = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ExposureTime);
				if (!exposureTimeInMilliSeconds.equals("")) {
					exposureTimeInSeconds = "";
					try {
						exposureTimeInSeconds = new Double(new Double(exposureTimeInMilliSeconds).doubleValue()/1000).toString();
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}
			}

			kvp = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.KVP);
			tubeCurrent = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.XRayTubeCurrent);

			midScanTime="";
			if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x0010)).equals("GEMS_ACQU_01")) {
				midScanTime = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x1024));
			}

			exposureTimePerRotation = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.RevolutionTime);
			if (exposureTimePerRotation.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x0010)).equals("GEMS_ACQU_01")) {
					exposureTimePerRotation = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x1027));	//  Rotation Speed (Gantry Period)
				}
			}

			nominalSingleCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SingleCollimationWidth);
			if (nominalSingleCollimationWidth.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_HELIOS_01")) {
					nominalSingleCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x1002));	//   Macro width at ISO Center
					if (nominalSingleCollimationWidth.contains("?")) {
						nominalSingleCollimationWidth = "";
					}
				}
				else if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x0010)).equals("TOSHIBA_MEC_CT3")) {
					nominalSingleCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x1008));	// Detector Slice Thickness in mm
				}
			}

			nominalTotalCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.TotalCollimationWidth);
			if (nominalTotalCollimationWidth.equals("") && !nominalSingleCollimationWidth.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_HELIOS_01")) {
					try {
						double dNumberOfMacroRowsInDetector = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x0045,0x1001),0d);	//   Number of Macro Rows in Detector
						double dNominalSingleCollimationWidth = getDoubleValueOrZeroIfEmptyOrInvalid(nominalSingleCollimationWidth);
						if (dNumberOfMacroRowsInDetector > 0 && dNominalSingleCollimationWidth > 0) {
							double dNominalTotalCollimationWidth = dNumberOfMacroRowsInDetector * dNominalSingleCollimationWidth;
							nominalTotalCollimationWidth = Double.toString(dNominalTotalCollimationWidth);
						}
					}
					catch (NumberFormatException e) {
						slf4jlogger.error("",e);
					}
				}
				else if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x0010)).equals("TOSHIBA_MEC_CT3")) {
					String numberOfDetectorRowsToReconstruct = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x1009));	// is actually a string of ones
					if (numberOfDetectorRowsToReconstruct.contains("1")) {
						int countOfDetectors = numberOfDetectorRowsToReconstruct.lastIndexOf('1') - numberOfDetectorRowsToReconstruct.indexOf('1') + 1;
						double dNominalSingleCollimationWidth = getDoubleValueOrZeroIfEmptyOrInvalid(nominalSingleCollimationWidth);
						double dNominalTotalCollimationWidth = countOfDetectors * dNominalSingleCollimationWidth;
						nominalTotalCollimationWidth = Double.toString(dNominalTotalCollimationWidth);
					}
				}
			}

			pitchFactor = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SpiralPitchFactor);
			if (pitchFactor.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0043,0x0010)).equals("GEMS_PARM_01")) {
					pitchFactor = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0043,0x1027));	//   Scan Pitch Ratio in the form "n.nnn:1"
					pitchFactor = pitchFactor.trim().replace(":1","");
				}
				else if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x0010)).equals("TOSHIBA_MEC_CT3")) {
					pitchFactor = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x1023));
				}
				if (pitchFactor.equals("") && !nominalTotalCollimationWidth.equals("")) {
					// Pitch Factor: For Spiral Acquisition, the Pitch Factor is the ratio of the Table Feed per Rotation
					// to the Nominal Total Collimation Width. For Sequenced Acquisition, the Pitch Factor is the ratio
					// of the Table Feed per single sequenced scan to the Nominal Total Collimation Width.
					try {
						double dTableFeedPerRotation = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.TableFeedPerRotation,0d);
						if (dTableFeedPerRotation == 0) {
							if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_ACQU_01")) {
								dTableFeedPerRotation = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x0019,0x1023),0d);	// Table Speed [mm/rotation]
							}
						}
						if (dTableFeedPerRotation > 0) {
							double dNominalTotalCollimationWidth = getDoubleValueOrZeroIfEmptyOrInvalid(nominalTotalCollimationWidth);
							if (dNominalTotalCollimationWidth > 0) {
								double dPitchFactor = dTableFeedPerRotation / dNominalTotalCollimationWidth;
								pitchFactor = Double.toString(dPitchFactor);
							}
						}
					}
					catch (NumberFormatException e) {
						slf4jlogger.error("",e);
					}
				}
			}

			// handles midnight crossing, but not robust if one or the other is sometimes missing in the set of files
			acquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDateTime);
			if (acquisitionDateTime.equals("")) {
				acquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDate) + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionTime);
			}

			sliceLocation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SliceLocation);
			anatomy = CTAnatomy.findAnatomicConcept(list);

			try {
				zLocation = "";
				Attribute aImagePositionPatient = list.get(TagFromName.ImagePositionPatient);
				if (aImagePositionPatient != null && aImagePositionPatient.getVM() == 3) {
					String[] vImagePositionPatient = aImagePositionPatient.getStringValues();
					zLocation = vImagePositionPatient[2];
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
			
			sliceThickness = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SliceThickness);
			
			frameOfReferenceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.FrameOfReferenceUID);
			
			exposureModulationType = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ExposureModulationType);
			
			estimatedDoseSaving = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.EstimatedDoseSaving);	//is FD VR
			
			CTDIvol = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CTDIvol);							//is FD VR
			
			if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x7005,0x0010)).equals("TOSHIBA_MEC_CT3")) {
				Attribute aDLP = list.get(new AttributeTag(0x7005,0x1040));												//is FD VR
//System.err.println("CTIrradiationEventDataFromImages.Slice(): aDLP = "+aDLP);
				if (aDLP != null && aDLP.getVL() > 0) {
					double dDLP = aDLP.getSingleDoubleValueOrDefault(0d);		// may be UN VR so do NOT use getSingleStringValueOrEmptyString()
					DLP = FloatFormatter.toString(dDLP,Locale.US);
//System.err.println("CTIrradiationEventDataFromImages.Slice(): DLP = "+DLP);
				}
			}
		}
	}

	private void extractConsistentParametersWithinIrradiationEvents() {
//System.err.println("CTIrradiationEventDataFromImages.extractConsistentParametersWithinIrradiationEvents():");
		for (String irradiationEventUID : slicesByIrradiationEventUID.keySet()) {
			List<Slice> event = slicesByIrradiationEventUID.get(irradiationEventUID);
			for (Slice s : event) {
				// handle attributes that are global (i.e., not indexed by event), and assume that the entire set is one patient
				// the common logic is not refactored into a utility method, since need to return two values, the string and the cleanliness of it
				if (timezoneOffsetFromUTCIsClean) {
					String newValue = s.timezoneOffsetFromUTC;
					if (!newValue.equals("")) {
						if (timezoneOffsetFromUTC == null || timezoneOffsetFromUTC.equals("")) {
							timezoneOffsetFromUTC = newValue;
						}
						else if (!timezoneOffsetFromUTC.equals(newValue)) {
							timezoneOffsetFromUTCIsClean = false;
						}
					}
				}
				if (patientAgeIsClean) {
					String newValue = s.patientAge;
					if (!newValue.equals("")) {
						if (patientAge == null || patientAge.equals("")) {
							patientAge = newValue;
						}
						else if (!patientAge.equals(newValue)) {
							patientAgeIsClean = false;
						}
					}
				}
				if (patientSexIsClean) {
					String newValue = s.patientSex;
					if (!newValue.equals("")) {
						if (patientSex == null || patientSex.equals("")) {
							patientSex = newValue;
						}
						else if (!patientSex.equals(newValue)) {
							patientSexIsClean = false;
						}
					}
				}
				if (patientWeightIsClean) {
					String newValue = s.patientWeight;
					if (!newValue.equals("")) {
						if (patientWeight == null || patientWeight.equals("")) {
							patientWeight = newValue;
						}
						else if (!patientWeight.equals(newValue)) {
							patientWeightIsClean = false;
						}
					}
				}
				if (patientSizeIsClean) {
					String newValue = s.patientSize;
					if (!newValue.equals("")) {
						if (patientSize == null || patientSize.equals("")) {
							patientSize = newValue;
						}
						else if (!patientSize.equals(newValue)) {
							patientSizeIsClean = false;
						}
					}
				}

				// now handle values that are event-specific
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (studyInstanceUIDByEvent,                  studyInstanceUIDByEventIsClean,                  irradiationEventUID,s.studyInstanceUID);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (frameOfReferenceUIDByEvent,               frameOfReferenceUIDByEventIsClean,               irradiationEventUID,s.frameOfReferenceUID);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (imageTypeByEvent,                         imageTypeByEventIsClean,                         irradiationEventUID,s.imageType);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (imageTypeValue3ByEvent,                   imageTypeValue3ByEventIsClean,                   irradiationEventUID,s.imageTypeValue3);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (orientationByEvent,                       orientationByEventIsClean,                       irradiationEventUID,s.orientation);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (acquisitionNumberByEvent,                 acquisitionNumberByEventIsClean,                 irradiationEventUID,s.acquisitionNumber);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (seriesNumberByEvent,                      seriesNumberByEventIsClean,                      irradiationEventUID,s.seriesNumber);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (seriesDescriptionByEvent,                 seriesDescriptionByEventIsClean,                 irradiationEventUID,s.seriesDescription);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (protocolNameByEvent,                      protocolNameByEventIsClean,                      irradiationEventUID,s.protocolName);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (pitchFactorByEvent,                       pitchFactorByEventIsClean,                       irradiationEventUID,s.pitchFactor);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (exposureTimeByEvent,                      exposureTimeByEventIsClean,                      irradiationEventUID,s.exposureTimeInSeconds);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (kvpByEvent,                               kvpByEventIsClean,                               irradiationEventUID,s.kvp);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (exposureTimePerRotationByEvent,           exposureTimePerRotationByEventIsClean,           irradiationEventUID,s.exposureTimePerRotation);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (nominalSingleCollimationWidthInMMByEvent, nominalSingleCollimationWidthInMMByEventIsClean, irradiationEventUID,s.nominalSingleCollimationWidth);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (nominalTotalCollimationWidthInMMByEvent,  nominalTotalCollimationWidthInMMByEventIsClean,  irradiationEventUID,s.nominalTotalCollimationWidth);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (sliceThicknessInMMByEvent,                sliceThicknessInMMByEventIsClean,                irradiationEventUID,s.sliceThickness);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (exposureModulationTypeByEvent,            exposureModulationTypeByEventIsClean,            irradiationEventUID,s.exposureModulationType);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (estimatedDoseSavingByEvent,               estimatedDoseSavingByEventIsClean,               irradiationEventUID,s.estimatedDoseSaving);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (CTDIvolByEvent,                           CTDIvolByEventIsClean,                           irradiationEventUID,s.CTDIvol);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (DLPByEvent,                               DLPByEventIsClean,                               irradiationEventUID,s.DLP);
		
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (tubeCurrentByEvent,                       tubeCurrentByEventIsClean,                       irradiationEventUID,s.tubeCurrent);
				if (s.tubeCurrent.length() > 0) {
					try {
						Double tubeCurrentDouble = new Double(s.tubeCurrent);
						Double tubeCurrentCountDouble = tubeCurrentCountByEvent.get(irradiationEventUID);
						if (tubeCurrentCountDouble == null) {
							// first time
							tubeCurrentCountByEvent.put(irradiationEventUID,new Double(1));
							tubeCurrentTotalByEvent.put(irradiationEventUID,tubeCurrentDouble);
							tubeCurrentMaximumByEvent.put(irradiationEventUID,tubeCurrentDouble);
						}
						else {
							double tubeCurrentValue = tubeCurrentDouble.doubleValue();
										
							Double tubeCurrentTotalDouble = tubeCurrentTotalByEvent.get(irradiationEventUID);
							double tubeCurrentTotalValue  = tubeCurrentTotalDouble.doubleValue();
							tubeCurrentTotalValue += tubeCurrentValue;
							tubeCurrentTotalByEvent.put(irradiationEventUID,new Double(tubeCurrentTotalValue));
										
							Double tubeCurrentMaximumDouble = tubeCurrentMaximumByEvent.get(irradiationEventUID);
							double tubeCurrentMaximumValue  = tubeCurrentMaximumDouble.doubleValue();
							if (tubeCurrentValue > tubeCurrentMaximumValue) {
								tubeCurrentMaximumValue = tubeCurrentValue;
								tubeCurrentMaximumByEvent.put(irradiationEventUID,new Double(tubeCurrentMaximumValue));
							}
										
							double tubeCurrentCountValue = tubeCurrentCountDouble.doubleValue();
							tubeCurrentCountValue+=1;
							tubeCurrentCountByEvent.put(irradiationEventUID,new Double(tubeCurrentCountValue));
						}
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}

				if (s.midScanTime.length() > 0) {
					Double midScanTimeDouble = new Double(s.midScanTime);
					Double midScanTimeCountDouble = midScanTimeCountByEvent.get(irradiationEventUID);
					if (midScanTimeCountDouble == null) {
						// first time
						midScanTimeCountByEvent.put(irradiationEventUID,new Double(1));
						midScanTimeMinimumByEvent.put(irradiationEventUID,midScanTimeDouble);
						midScanTimeMaximumByEvent.put(irradiationEventUID,midScanTimeDouble);
					}
					else {
						try {
							double midScanTimeValue = midScanTimeDouble.doubleValue();
										
							Double midScanTimeMaximumDouble = midScanTimeMaximumByEvent.get(irradiationEventUID);
							double midScanTimeMaximumValue  = midScanTimeMaximumDouble.doubleValue();
							if (midScanTimeValue > midScanTimeMaximumValue) {
								midScanTimeMaximumValue = midScanTimeValue;
								midScanTimeMaximumByEvent.put(irradiationEventUID,new Double(midScanTimeMaximumValue));
							}
										
							Double midScanTimeMinimumDouble = midScanTimeMinimumByEvent.get(irradiationEventUID);
							double midScanTimeMinimumValue  = midScanTimeMinimumDouble.doubleValue();
							if (midScanTimeValue < midScanTimeMinimumValue) {
								midScanTimeMinimumValue = midScanTimeValue;
								midScanTimeMinimumByEvent.put(irradiationEventUID,new Double(midScanTimeMinimumValue));
							}
										
							double midScanTimeCountValue = midScanTimeCountDouble.doubleValue();
							midScanTimeCountValue+=1;
							midScanTimeCountByEvent.put(irradiationEventUID,new Double(midScanTimeCountValue));
						}
						catch (NumberFormatException e) {
							// do nothing
						}
					}
				}

				putAnatomyByStringIndexIfNotDifferentOrCanCombineElseFlagAsUnclean(anatomyByEvent,anatomyByEventIsClean,irradiationEventUID,s.anatomy);
				putAnatomyByStringIndexIfNotDifferentOrCanCombineElseSetToWholeBody(combinedAnatomyForStudy,s.studyInstanceUID,s.anatomy);

				putStringValueByStringIndexIfLexicographicSortIsEarlier(startAcquisitionDateTimeByEvent,irradiationEventUID,s.acquisitionDateTime);
				putStringValueByStringIndexIfLexicographicSortIsLater  (  endAcquisitionDateTimeByEvent,irradiationEventUID,s.acquisitionDateTime);
				{
					String overallEarliestAcquisitionDateTime = overallEarliestAcquisitionDateTimeByStudy.get(s.studyInstanceUID);
					if (overallEarliestAcquisitionDateTime == null || s.acquisitionDateTime.compareTo(overallEarliestAcquisitionDateTime) < 0) {
						overallEarliestAcquisitionDateTimeByStudy.put(s.studyInstanceUID,s.acquisitionDateTime);
					}
					String overallLatestAcquisitionDateTime = overallLatestAcquisitionDateTimeByStudy.get(s.studyInstanceUID);
					if (overallLatestAcquisitionDateTime == null || s.acquisitionDateTime.compareTo(overallLatestAcquisitionDateTime) > 0) {
						overallLatestAcquisitionDateTimeByStudy.put(s.studyInstanceUID,s.acquisitionDateTime);
					}
				}

				putNumericStringValueByStringIndexIfNumericSortIsEarlier(lowestSliceLocationByEvent,irradiationEventUID,s.sliceLocation);
				putNumericStringValueByStringIndexIfNumericSortIsLater(highestSliceLocationByEvent,irradiationEventUID,s.sliceLocation);

				putNumericStringValueByStringIndexIfNumericSortIsEarlier(lowestZLocationByEvent,irradiationEventUID,s.zLocation);
				putNumericStringValueByStringIndexIfNumericSortIsLater(highestZLocationByEvent,irradiationEventUID,s.zLocation);
			}
		}
		extracted = true;
	}
	
	//private static String toStringCodedSequenceItem(String name,Map<String,CodedSequenceItem> map,Map<String,Boolean> cleanMap,String uid) {
	//	String value;
	//	Boolean clean = cleanMap.get(uid);
	//	if (clean == null) {
	//		value = "-- not found --";
	//	}
	//	else {
	//		if (clean.booleanValue()) {
	//			CodedSequenceItem item = map.get(uid);
	//			if (item == null) {
	//				value = "-- not found --";
	//			}
	//			else {
	//				value = item.toString();
	//			}
	//		}
	//		else {
	//			value = "-- inconsistent values for event --";
	//		}
	//	}
	//	return "\t\t"+name+" = "+value+"\n";
	//}
	
	private static String toStringDisplayableConcept(String name,Map<String,DisplayableConcept> map,Map<String,Boolean> cleanMap,String uid) {
		String value;
		Boolean clean = cleanMap.get(uid);
		if (clean == null) {
			value = "-- not found --";
		}
		else {
			if (clean.booleanValue()) {
				DisplayableConcept concept = map.get(uid);
				if (concept == null) {
					value = "-- not found --";
				}
				else {
					try {
						value = concept.getCodedSequenceItem().toString();
					}
					catch (DicomException e) {
						value = "-- bad  --";
						slf4jlogger.error("",e);
					}
				}
			}
			else {
				value = "-- inconsistent values for event --";
			}
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String toString(String name,String value,boolean isClean) {
		if (isClean) {
			if (value == null) {
				value = "-- not found --";
			}
		}
		else {
			value = "-- inconsistent values for event --";
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String toString(String name,Map<String,String> valueMap,Map<String,Boolean> cleanMap,String uid) {
		String value;
		Boolean clean = cleanMap.get(uid);
		if (clean == null) {
			value = "-- not found --";
		}
		else {
			if (clean.booleanValue()) {
				value = valueMap.get(uid);
				if (value == null) {
					value = "-- not found --";
				}
			}
			else {
				value = "-- inconsistent values for event --";
			}
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String stringValueToString(String name,Map<String,String> valueMap,String uid) {
		String value = valueMap.get(uid);
		if (value == null) {
			value = "-- not found --";
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String doubleValueToString(String name,Map<String,Double> map,String uid) {
		String value;
		Double dvalue = map.get(uid);
		if (dvalue == null) {
			value = "-- not found --";
		}
		else {
			value = dvalue.toString();
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String locationValueToStringSigned(double dValue) {
		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
		formatter.setGroupingUsed(false);
		formatter.setMinimumFractionDigits(3);
		formatter.setMaximumFractionDigits(3);
		// leave minus sign alone if present - do NOT convert to I or S
		return formatter.format(dValue);
	}
	
	private static String locationValueToStringSigned(Double dValue) {
		String value;
		if (dValue == null) {
			value = "";
		}
		else {
			//value = dValue.toString();
			value = locationValueToStringSigned(dValue.doubleValue());
			// leave minus sign alone if present - do NOT convert to I or S
		}
		return value;
	}
	
	private static String locationValueToStringIS(Double dValue) {
		String value;
		if (dValue == null) {
			value = "";
		}
		else {
			//value = dValue.toString();
			value = locationValueToStringSigned(dValue.doubleValue());
			if (value.startsWith("-")) {
				value = "I"+value.substring(1);
			}
			else {
				value = "S"+value;
			}
		}
		return value;
	}
	
	private static String locationValueToStringIS(String name,Map<String,Double> map,String uid) {
		String value = locationValueToStringIS(map.get(uid));
		if (value.equals("")) {
			value = "-- not found --";
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	public String toString() {
		needEverythingOrganizedAndExtracted();
		
		StringBuffer buf = new StringBuffer();

		buf.append("\tCommon:\n");
		buf.append(toString("PatientAge",patientAge,patientAgeIsClean));
		buf.append(toString("PatientSex",patientSex,patientSexIsClean));
		buf.append(toString("PatientWeight",patientWeight,patientWeightIsClean));
		buf.append(toString("PatientSize",patientSize,patientSizeIsClean));
		
		// sort output not by irradiationEventUID alone, but by something consistent derived from content, in case comparing logs
		SortedMap<String,String> irradiationEventUIDBySortedKey = new TreeMap<String,String>();
		for (String irradiationEventUID : slicesByIrradiationEventUID.keySet()) {
			String key =
				 toString("StudyInstanceUID",studyInstanceUIDByEvent,studyInstanceUIDByEventIsClean,irradiationEventUID)
				+toString("FrameOfReferenceUID",frameOfReferenceUIDByEvent,frameOfReferenceUIDByEventIsClean,irradiationEventUID)
				+toString("AcquisitionNumber",acquisitionNumberByEvent,acquisitionNumberByEventIsClean,irradiationEventUID)
				+toString("SeriesNumber",seriesNumberByEvent,seriesNumberByEventIsClean,irradiationEventUID)
				+irradiationEventUID;
			  ;
			irradiationEventUIDBySortedKey.put(key,irradiationEventUID);
		}
		
		if (irradiationEventUIDBySortedKey.size() != slicesByIrradiationEventUID.size()) {
			buf.append("Warning: Duplicate listing of IrradiationEventUIDs during sorting by key ...\n");
		}

		//for (String irradiationEventUID : slicesByIrradiationEventUID.keySet()) {
		for (String irradiationEventUID : irradiationEventUIDBySortedKey.values()) {	// SortedMap contract says values are returned in order of keys
			buf.append("\tIrradiationEventUID = "+irradiationEventUID+"\n");
			buf.append(toString("StudyInstanceUID",studyInstanceUIDByEvent,studyInstanceUIDByEventIsClean,irradiationEventUID));
			buf.append(toString("FrameOfReferenceUID",frameOfReferenceUIDByEvent,frameOfReferenceUIDByEventIsClean,irradiationEventUID));
			buf.append(toString("ImageType",imageTypeByEvent,imageTypeByEventIsClean,irradiationEventUID));
			buf.append(toString("Orientation",orientationByEvent,orientationByEventIsClean,irradiationEventUID));
			buf.append(toString("AcquisitionNumber",acquisitionNumberByEvent,acquisitionNumberByEventIsClean,irradiationEventUID));
			buf.append(toString("SeriesNumber",seriesNumberByEvent,seriesNumberByEventIsClean,irradiationEventUID));
			buf.append(toString("SeriesDescription",seriesDescriptionByEvent,seriesDescriptionByEventIsClean,irradiationEventUID));
			buf.append(toStringDisplayableConcept("Anatomy",anatomyByEvent,anatomyByEventIsClean,irradiationEventUID));
			buf.append(toString("ProtocolName",protocolNameByEvent,protocolNameByEventIsClean,irradiationEventUID));
			
			buf.append(stringValueToString("StartAcquisitionDateTime",startAcquisitionDateTimeByEvent,irradiationEventUID));
			buf.append(stringValueToString("EndAcquisitionDateTime",endAcquisitionDateTimeByEvent,irradiationEventUID));
			buf.append(locationValueToStringIS("LowestSliceLocation",lowestSliceLocationByEvent,irradiationEventUID));
			buf.append(locationValueToStringIS("HighestSliceLocation",highestSliceLocationByEvent,irradiationEventUID));
			buf.append(locationValueToStringIS("LowestZLocation",lowestZLocationByEvent,irradiationEventUID));
			buf.append(locationValueToStringIS("HighestZLocation",highestZLocationByEvent,irradiationEventUID));
			
			buf.append(toString("ExposureTime",exposureTimeByEvent,exposureTimeByEventIsClean,irradiationEventUID));
			buf.append(toString("KVP",kvpByEvent,kvpByEventIsClean,irradiationEventUID));
			buf.append(toString("TubeCurrent",tubeCurrentByEvent,tubeCurrentByEventIsClean,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentTotal",tubeCurrentTotalByEvent,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentCount",tubeCurrentCountByEvent,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentMaximum",tubeCurrentMaximumByEvent,irradiationEventUID));

			buf.append(doubleValueToString("MidScanTimeCount",midScanTimeCountByEvent,irradiationEventUID));
			buf.append(doubleValueToString("MidScanTimeMinimum",midScanTimeMinimumByEvent,irradiationEventUID));
			buf.append(doubleValueToString("MidScanTimeMaximum",midScanTimeMaximumByEvent,irradiationEventUID));

			buf.append(toString("ExposureTimePerRotation",exposureTimePerRotationByEvent,exposureTimePerRotationByEventIsClean,irradiationEventUID));
			
			buf.append(toString("NominalSingleCollimationWidthInMM",nominalSingleCollimationWidthInMMByEvent,nominalSingleCollimationWidthInMMByEventIsClean,irradiationEventUID));
			buf.append(toString("NominalTotalCollimationWidthInMM",nominalTotalCollimationWidthInMMByEvent,nominalTotalCollimationWidthInMMByEventIsClean,irradiationEventUID));
			buf.append(toString("SliceThicknessInMM",sliceThicknessInMMByEvent,sliceThicknessInMMByEventIsClean,irradiationEventUID));
			buf.append(toString("PitchFactor",pitchFactorByEvent,pitchFactorByEventIsClean,irradiationEventUID));
			
			buf.append(toString("ExposureModulationType",exposureModulationTypeByEvent,exposureModulationTypeByEventIsClean,irradiationEventUID));
			buf.append(toString("EstimatedDoseSaving",estimatedDoseSavingByEvent,estimatedDoseSavingByEventIsClean,irradiationEventUID));
			buf.append(toString("CTDIvol",CTDIvolByEvent,CTDIvolByEventIsClean,irradiationEventUID));
			buf.append(toString("DLP",DLPByEvent,DLPByEventIsClean,irradiationEventUID));
		}

		for (String studyInstanceUID : overallEarliestAcquisitionDateTimeByStudy.keySet()) {
			buf.append("\tStudyInstanceUID = "+studyInstanceUID+"\n");
			buf.append("\t\tEarliestAcquisitionDateTime = "+overallEarliestAcquisitionDateTimeByStudy.get(studyInstanceUID)+"\n");
			buf.append("\t\tLatestAcquisitionDateTime = "+overallLatestAcquisitionDateTimeByStudy.get(studyInstanceUID)+"\n");
		}
		
		return buf.toString();
	}
	
	public static final void main(String arg[]) {
		try {
			CTIrradiationEventDataFromImages eventDataFromImages = new CTIrradiationEventDataFromImages(arg[0]);
System.err.print(eventDataFromImages);	// no need to use SLF4J since command line utility/test
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
	
}


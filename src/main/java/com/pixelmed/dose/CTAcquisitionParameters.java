/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class CTAcquisitionParameters {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CTAcquisitionParameters.java,v 1.26 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CTAcquisitionParameters.class);
	
	protected String irradiationEventUID;
	protected CTScanType scanType;
	protected CodedSequenceItem anatomy;
	protected String acquisitionProtocol;
	protected String comment;
	protected String exposureTimeInSeconds;
	protected String scanningLengthInMM;
	protected String lengthOfReconstructableVolumeInMM;
	protected String exposedRangeInMM;
	protected String topZLocationOfReconstructableVolume;
	protected String bottomZLocationOfReconstructableVolume;
	protected String topZLocationOfScanningLength;
	protected String bottomZLocationOfScanningLength;
	protected String frameOfReferenceUID;
	protected String nominalSingleCollimationWidthInMM;
	protected String nominalTotalCollimationWidthInMM;
	protected String pitchFactor;
	protected String kvp;
	protected String tubeCurrent;
	protected String tubeCurrentMaximum;
	protected String exposureTimePerRotation;
	
	protected ContentItem contentItemFragment;
	
	public void merge(CTAcquisitionParameters oap) {
		if (oap != null) {
			contentItemFragment = null;		// clear cache
			if (oap.irradiationEventUID != null && oap.irradiationEventUID.length() > 0) {
				irradiationEventUID = oap.irradiationEventUID;
			}
			if (oap.scanType != null) {
				scanType = oap.scanType;
			}
			if (oap.anatomy != null) {
				anatomy = oap.anatomy;
			}
			if (oap.acquisitionProtocol != null) {
				acquisitionProtocol = oap.acquisitionProtocol;
			}
			if (oap.comment != null) {
				comment = oap.comment;
			}
			if (oap.exposureTimeInSeconds != null && oap.exposureTimeInSeconds.length() > 0) {
				exposureTimeInSeconds = oap.exposureTimeInSeconds;
			}
			if (oap.scanningLengthInMM != null && oap.scanningLengthInMM.length() > 0) {
				scanningLengthInMM = oap.scanningLengthInMM;
			}
			if (oap.lengthOfReconstructableVolumeInMM != null && oap.lengthOfReconstructableVolumeInMM.length() > 0) {
				lengthOfReconstructableVolumeInMM = oap.lengthOfReconstructableVolumeInMM;
			}
			if (oap.exposedRangeInMM != null && oap.exposedRangeInMM.length() > 0) {
				exposedRangeInMM = oap.exposedRangeInMM;
			}
			if (oap.topZLocationOfReconstructableVolume != null && oap.topZLocationOfReconstructableVolume.length() > 0) {
				topZLocationOfReconstructableVolume = oap.topZLocationOfReconstructableVolume;
			}
			if (oap.bottomZLocationOfReconstructableVolume != null && oap.bottomZLocationOfReconstructableVolume.length() > 0) {
				bottomZLocationOfReconstructableVolume = oap.bottomZLocationOfReconstructableVolume;
			}
			if (oap.topZLocationOfScanningLength != null && oap.topZLocationOfScanningLength.length() > 0) {
				topZLocationOfScanningLength = oap.topZLocationOfScanningLength;
			}
			if (oap.bottomZLocationOfScanningLength != null && oap.bottomZLocationOfScanningLength.length() > 0) {
				bottomZLocationOfScanningLength = oap.bottomZLocationOfScanningLength;
			}
			if (oap.frameOfReferenceUID != null && oap.frameOfReferenceUID.length() > 0) {
				frameOfReferenceUID = oap.frameOfReferenceUID;
			}
			if (oap.nominalSingleCollimationWidthInMM != null && oap.nominalSingleCollimationWidthInMM.length() > 0) {
				nominalSingleCollimationWidthInMM = oap.nominalSingleCollimationWidthInMM;
			}
			if (oap.nominalTotalCollimationWidthInMM != null && oap.nominalTotalCollimationWidthInMM.length() > 0) {
				nominalTotalCollimationWidthInMM = oap.nominalTotalCollimationWidthInMM;
			}
			if (oap.pitchFactor != null && oap.pitchFactor.length() > 0) {
				pitchFactor = oap.pitchFactor;
			}
			if (oap.kvp != null && oap.kvp.length() > 0) {
				kvp = oap.kvp;
			}
			if (oap.tubeCurrent != null && oap.tubeCurrent.length() > 0) {
				tubeCurrent = oap.tubeCurrent;
			}
			if (oap.tubeCurrentMaximum != null && oap.tubeCurrentMaximum.length() > 0) {
				tubeCurrentMaximum = oap.tubeCurrentMaximum;
			}
			if (oap.exposureTimePerRotation != null && oap.exposureTimePerRotation.length() > 0) {
				exposureTimePerRotation = oap.exposureTimePerRotation;
			}
		}
	}

	public boolean equals(Object o) {
//System.err.println("CTAcquisitionParameters.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof CTAcquisitionParameters) {
			CTAcquisitionParameters oap = (CTAcquisitionParameters)o;
			isEqual =
			   ((oap.getIrradiationEventUID() == null && this.getIrradiationEventUID() == null) || (oap.getIrradiationEventUID().equals(this.getIrradiationEventUID())))
			&& equalsApartFromIrradiationEventUID(oap)
			;
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}

	public boolean equalsApartFromIrradiationEventUID(CTAcquisitionParameters oap) {
//System.err.println("CTAcquisitionParameters.equalsApartFromIrradiationEventUID(): comparing "+this+" to "+oap);
		return ((oap.getScanType() == null                               && this.getScanType() == null)                               || (oap.getScanType() != null                               && this.getScanType() != null                                && oap.getScanType().equals(this.getScanType())))
			&& ((oap.getAnatomy() == null                                && this.getAnatomy() == null)                                || (oap.getAnatomy() != null                                && this.getAnatomy() != null                                 && oap.getAnatomy().equals(this.getAnatomy())))
			&& ((oap.getAcquisitionProtocol() == null                    && this.getAcquisitionProtocol() == null)                    || (oap.getAcquisitionProtocol() != null                    && this.getAcquisitionProtocol() != null                     && oap.getAcquisitionProtocol().equals(this.getAcquisitionProtocol())))
			&& ((oap.getComment() == null                                && this.getComment() == null)                                || (oap.getComment() != null                                && this.getComment() != null                                 && oap.getComment().equals(this.getComment())))
			&& ((oap.getExposureTimeInSeconds() == null                  && this.getExposureTimeInSeconds() == null)                  || (oap.getExposureTimeInSeconds() != null                  && this.getExposureTimeInSeconds() != null                   && oap.getExposureTimeInSeconds().equals(this.getExposureTimeInSeconds())))
			&& ((oap.getScanningLengthInMM() == null                     && this.getScanningLengthInMM() == null)                     || (oap.getScanningLengthInMM() != null                     && this.getScanningLengthInMM() != null                      && oap.getScanningLengthInMM().equals(this.getScanningLengthInMM())))
			&& ((oap.getLengthOfReconstructableVolumeInMM() == null      && this.getLengthOfReconstructableVolumeInMM() == null)      || (oap.getLengthOfReconstructableVolumeInMM() != null      && this.getLengthOfReconstructableVolumeInMM() != null       && oap.getLengthOfReconstructableVolumeInMM().equals(this.getLengthOfReconstructableVolumeInMM())))
			&& ((oap.getExposedRangeInMM() == null                       && this.getExposedRangeInMM() == null)                       || (oap.getExposedRangeInMM() != null                       && this.getExposedRangeInMM() != null                        && oap.getExposedRangeInMM().equals(this.getExposedRangeInMM())))
			&& ((oap.getTopZLocationOfReconstructableVolume() == null    && this.getTopZLocationOfReconstructableVolume() == null)    || (oap.getTopZLocationOfReconstructableVolume() != null    && this.getTopZLocationOfReconstructableVolume() != null     && oap.getTopZLocationOfReconstructableVolume().equals(this.getTopZLocationOfReconstructableVolume())))
			&& ((oap.getBottomZLocationOfReconstructableVolume() == null && this.getBottomZLocationOfReconstructableVolume() == null) || (oap.getBottomZLocationOfReconstructableVolume() != null && this.getBottomZLocationOfReconstructableVolume() != null  && oap.getBottomZLocationOfReconstructableVolume().equals(this.getBottomZLocationOfReconstructableVolume())))
			&& ((oap.getTopZLocationOfScanningLength() == null           && this.getTopZLocationOfScanningLength() == null)           || (oap.getTopZLocationOfScanningLength() != null           && this.getTopZLocationOfScanningLength() != null            && oap.getTopZLocationOfScanningLength().equals(this.getTopZLocationOfScanningLength())))
			&& ((oap.getBottomZLocationOfScanningLength() == null        && this.getBottomZLocationOfScanningLength() == null)        || (oap.getBottomZLocationOfScanningLength() != null        && this.getBottomZLocationOfScanningLength() != null         && oap.getBottomZLocationOfScanningLength().equals(this.getBottomZLocationOfScanningLength())))
			&& ((oap.getFrameOfReferenceUID() == null                    && this.getFrameOfReferenceUID() == null)                    || (oap.getFrameOfReferenceUID() != null                    && this.getFrameOfReferenceUID() != null                     && oap.getFrameOfReferenceUID().equals(this.getFrameOfReferenceUID())))
			&& ((oap.getNominalSingleCollimationWidthInMM() == null      && this.getNominalSingleCollimationWidthInMM() == null)      || (oap.getNominalSingleCollimationWidthInMM() != null      && this.getNominalSingleCollimationWidthInMM() != null       && oap.getNominalSingleCollimationWidthInMM().equals(this.getNominalSingleCollimationWidthInMM())))
			&& ((oap.getNominalTotalCollimationWidthInMM() == null       && this.getNominalTotalCollimationWidthInMM() == null)       || (oap.getNominalTotalCollimationWidthInMM() != null       && this.getNominalTotalCollimationWidthInMM() != null        && oap.getNominalTotalCollimationWidthInMM().equals(this.getNominalTotalCollimationWidthInMM())))
			&& ((oap.getPitchFactor() == null                            && this.getPitchFactor() == null)                            || (oap.getPitchFactor() != null                            && this.getPitchFactor() != null                             && oap.getPitchFactor().equals(this.getPitchFactor())))
			&& ((oap.getKVP() == null                                    && this.getKVP() == null)                                    || (oap.getKVP() != null                                    && this.getKVP() != null                                     && oap.getKVP().equals(this.getKVP())))
			&& ((oap.getTubeCurrent() == null                            && this.getTubeCurrent() == null)                            || (oap.getTubeCurrent() != null                            && this.getTubeCurrent() != null                             && oap.getTubeCurrent().equals(this.getTubeCurrent())))
			&& ((oap.getTubeCurrentMaximum() == null                     && this.getTubeCurrentMaximum() == null)                     || (oap.getTubeCurrentMaximum() != null                     && this.getTubeCurrentMaximum() != null                      && oap.getTubeCurrentMaximum().equals(this.getTubeCurrentMaximum())))
			&& ((oap.getExposureTimePerRotation() == null                && this.getExposureTimePerRotation() == null)                || (oap.getExposureTimePerRotation() != null                && this.getExposureTimePerRotation() != null                 && oap.getExposureTimePerRotation().equals(this.getExposureTimePerRotation())))
			;
	}

	public CTAcquisitionParameters(String irradiationEventUID,CTScanType scanType,CodedSequenceItem anatomy,String acquisitionProtocol,String comment,String exposureTimeInSeconds,String scanningLengthInMM,
				String lengthOfReconstructableVolumeInMM,String exposedRangeInMM,
				String topZLocationOfReconstructableVolume,String bottomZLocationOfReconstructableVolume,String topZLocationOfScanningLength,String bottomZLocationOfScanningLength,String frameOfReferenceUID,
				String nominalSingleCollimationWidthInMM,String nominalTotalCollimationWidthInMM,String pitchFactor,
				String kvp,String tubeCurrent,String tubeCurrentMaximum,String exposureTimePerRotation) {
		this.irradiationEventUID = irradiationEventUID;
		this.scanType = scanType;
		this.anatomy = anatomy;
		this.acquisitionProtocol = acquisitionProtocol;
		this.comment = comment;
		this.exposureTimeInSeconds = exposureTimeInSeconds;
		this.scanningLengthInMM = scanningLengthInMM;
		this.lengthOfReconstructableVolumeInMM = lengthOfReconstructableVolumeInMM;
		this.exposedRangeInMM = exposedRangeInMM;
		this.topZLocationOfReconstructableVolume = topZLocationOfReconstructableVolume;
		this.bottomZLocationOfReconstructableVolume = bottomZLocationOfReconstructableVolume;
		this.topZLocationOfScanningLength = topZLocationOfScanningLength;
		this.bottomZLocationOfScanningLength = bottomZLocationOfScanningLength;
		this.frameOfReferenceUID = frameOfReferenceUID;
		this.nominalSingleCollimationWidthInMM = nominalSingleCollimationWidthInMM;
		this.nominalTotalCollimationWidthInMM = nominalTotalCollimationWidthInMM;
		this.pitchFactor = pitchFactor;
		this.kvp = kvp;
		this.tubeCurrent = tubeCurrent;
		this.tubeCurrentMaximum = tubeCurrentMaximum;
		this.exposureTimePerRotation = exposureTimePerRotation;
	}

	public CTAcquisitionParameters(String irradiationEventUID,CTScanType scanType,CodedSequenceItem anatomy,String acquisitionProtocol,String comment,String exposureTimeInSeconds,String scanningLengthInMM,
				String nominalSingleCollimationWidthInMM,String nominalTotalCollimationWidthInMM,String pitchFactor,
				String kvp,String tubeCurrent,String tubeCurrentMaximum,String exposureTimePerRotation) {
		this.irradiationEventUID = irradiationEventUID;
		this.scanType = scanType;
		this.anatomy = anatomy;
		this.acquisitionProtocol = acquisitionProtocol;
		this.comment = comment;
		this.exposureTimeInSeconds = exposureTimeInSeconds;
		this.scanningLengthInMM = scanningLengthInMM;
		this.lengthOfReconstructableVolumeInMM = null;
		this.exposedRangeInMM = null;
		this.topZLocationOfReconstructableVolume = null;
		this.bottomZLocationOfReconstructableVolume = null;
		this.topZLocationOfScanningLength = null;
		this.bottomZLocationOfScanningLength = null;
		this.frameOfReferenceUID = null;
		this.nominalSingleCollimationWidthInMM = nominalSingleCollimationWidthInMM;
		this.nominalTotalCollimationWidthInMM = nominalTotalCollimationWidthInMM;
		this.pitchFactor = pitchFactor;
		this.kvp = kvp;
		this.tubeCurrent = tubeCurrent;
		this.tubeCurrentMaximum = tubeCurrentMaximum;
		this.exposureTimePerRotation = exposureTimePerRotation;
	}

	public CTAcquisitionParameters(CTAcquisitionParameters source) {
		this.irradiationEventUID = source.irradiationEventUID;
		this.scanType = source.scanType;
		this.anatomy = source.anatomy;
		this.acquisitionProtocol = source.acquisitionProtocol;
		this.comment = source.comment;
		this.exposureTimeInSeconds = source.exposureTimeInSeconds;
		this.scanningLengthInMM = source.scanningLengthInMM;
		this.lengthOfReconstructableVolumeInMM = source.lengthOfReconstructableVolumeInMM;
		this.exposedRangeInMM = source.exposedRangeInMM;
		this.topZLocationOfReconstructableVolume = source.topZLocationOfReconstructableVolume;
		this.bottomZLocationOfReconstructableVolume = source.bottomZLocationOfReconstructableVolume;
		this.topZLocationOfScanningLength = source.topZLocationOfScanningLength;
		this.bottomZLocationOfScanningLength = source.bottomZLocationOfScanningLength;
		this.frameOfReferenceUID = source.frameOfReferenceUID;
		this.nominalSingleCollimationWidthInMM = source.nominalSingleCollimationWidthInMM;
		this.nominalTotalCollimationWidthInMM = source.nominalTotalCollimationWidthInMM;
		this.pitchFactor = source.pitchFactor;
		this.kvp = source.kvp;
		this.tubeCurrent = source.tubeCurrent;
		this.tubeCurrentMaximum = source.tubeCurrentMaximum;
		this.exposureTimePerRotation = source.exposureTimePerRotation;
	}
	
	public CTAcquisitionParameters(ContentItem parametersNode) {
		if (parametersNode != null) {
			// extract information from siblings ...
			ContentItem parent = (ContentItem)(parametersNode.getParent());
			if (parent != null) {
				irradiationEventUID = parent.getSingleStringValueOrNullOfNamedChild("DCM","113769");	// "Irradiation Event UID"

				acquisitionProtocol = parent.getSingleStringValueOrNullOfNamedChild("DCM","125203");	// "Acquisition Protocol"

				comment = parent.getSingleStringValueOrNullOfNamedChild("DCM","121106");				// "Comment"

				ContentItem ctat = parent.getNamedChild("DCM","113820");	// "CT Acquisition Type"
				if (ctat != null && ctat instanceof ContentItemFactory.CodeContentItem) {
					scanType = CTScanType.selectFromCode(((ContentItemFactory.CodeContentItem)ctat).getConceptCode());
				}
				
				ContentItem targetRegion = parent.getNamedChild("DCM","123014");		// "Target Region"
				if (targetRegion != null && targetRegion instanceof ContentItemFactory.CodeContentItem) {
					anatomy = ((ContentItemFactory.CodeContentItem)targetRegion).getConceptCode();
				}
			}
		
			// extract information from children ...
			exposureTimeInSeconds                  = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113824");	// "Exposure Time"	... should really check units are seconds :(
			scanningLengthInMM                     = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113825");	// "Scanning Length"	... should really check units are mm :(
			lengthOfReconstructableVolumeInMM      = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113893");	// "Length of Reconstructable Volume"	... should really check units are mm :(
			exposedRangeInMM                       = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113899");	// "Exposed Range"	... should really check units are mm :(
			topZLocationOfReconstructableVolume    = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113895");	// "Top Z Location of Reconstructable Volume"	... should really check units are mm :(
			bottomZLocationOfReconstructableVolume = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113896");	// "Bottom Z Location of Reconstructable Volume"	... should really check units are mm :(
			topZLocationOfScanningLength           = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113897");	// "Top Z Location of Scanning Length"	... should really check units are mm :(
			bottomZLocationOfScanningLength        = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113898");	// "Bottom Z Location of Scanning Length"	... should really check units are mm :(
			frameOfReferenceUID                    = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","112227");	// "Frame of Reference UID"	... should really check units are mm :(
			nominalSingleCollimationWidthInMM      = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113826");	// "Nominal Single Collimation Width"	... should really check units are mm :(
			nominalTotalCollimationWidthInMM       = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113827");	// "Nominal Total Collimation Width"	... should really check units are mm :(
			pitchFactor                            = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113828");	// "Pitch Factor"	... should really check units are ratio :(

			// extract x-ray source informtion, assuming only one
			ContentItem source = parametersNode.getNamedChild("DCM","113831");		// "CT X-Ray Source Parameters"
			if (source != null) {
				kvp                     = source.getSingleStringValueOrNullOfNamedChild("DCM","113733");	// "KVP"	... should really check units are kV :(
				tubeCurrent             = source.getSingleStringValueOrNullOfNamedChild("DCM","113734");	// "X-Ray Tube Current"	... should really check units are mA :(
				tubeCurrentMaximum		= source.getSingleStringValueOrNullOfNamedChild("DCM","113833");	// "Maximum X-Ray Tube Current"	... should really check units are mA :(
				exposureTimePerRotation = source.getSingleStringValueOrNullOfNamedChild("DCM","113834");	// "Exposure Time per Rotation"	... should really check units are seconds :(
			}
		}
	}
	
	public String getIrradiationEventUID() { return irradiationEventUID; }
	
	public CTScanType getScanType() { return scanType; }
	
	public CodedSequenceItem getAnatomy() { return anatomy; }
	
	public String getAcquisitionProtocol() { return acquisitionProtocol; }
	
	public String getComment() { return comment; }
	
	public String getExposureTimeInSeconds() { return exposureTimeInSeconds; }
	
	public String getScanningLengthInMM() { return scanningLengthInMM; }
	
	public String getLengthOfReconstructableVolumeInMM() { return lengthOfReconstructableVolumeInMM; }
	
	public String getExposedRangeInMM()  { return exposedRangeInMM; }
	
	public String getTopZLocationOfReconstructableVolume() { return topZLocationOfReconstructableVolume; }
	
	public String getBottomZLocationOfReconstructableVolume()  { return bottomZLocationOfReconstructableVolume; }
	
	public String getTopZLocationOfScanningLength()  { return topZLocationOfScanningLength; }
	
	public String getBottomZLocationOfScanningLength()  { return bottomZLocationOfScanningLength; }
	
	public String getFrameOfReferenceUID()  { return frameOfReferenceUID; }

	public String getNominalSingleCollimationWidthInMM() { return nominalSingleCollimationWidthInMM; }
	
	public String getNominalTotalCollimationWidthInMM() { return nominalTotalCollimationWidthInMM; }
	
	public String getPitchFactor() { return pitchFactor; }
	
	public String getKVP() { return kvp; }
	
	public String getTubeCurrent() { return tubeCurrent; }
	
	public String getTubeCurrentMaximum() { return tubeCurrentMaximum; }
	
	public String getExposureTimePerRotation() { return exposureTimePerRotation; }
	
	private void replaceScanningLengthInMM(double dScanningLengthInMM) {
		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
		formatter.setGroupingUsed(false);					// i.e., no comma at thousands
		scanningLengthInMM = formatter.format(dScanningLengthInMM);
	}
	
	private double computeScanningLengthFromDLPAndCTDIVol(String dlp,String ctdiVol) {
		double dScanningLengthInMM = 0;
		if (dlp != null && dlp.length() > 0 && ctdiVol != null && ctdiVol.length() > 0) {
			try {
				double dDLP = new Double(dlp).doubleValue();
				double dCTDIVol = new Double(ctdiVol).doubleValue();
				if (dCTDIVol > 0) {	// don't want division by zero to produce NaN
					dScanningLengthInMM = dDLP/dCTDIVol*10;	// DLP is in mGy.cm not mm
				}
			}
			catch (NumberFormatException e) {
				slf4jlogger.error("",e);
			}
		}
		return dScanningLengthInMM;
	}
	
	//public void deriveScanningLengthFromDLPAndCTDIVol(String dlp,String ctdiVol) {
	//	double dScanningLengthInMM = computeScanningLengthFromDLPAndCTDIVol(dlp,ctdiVol);
	//	if (dScanningLengthInMM > 0) {
	//		replaceScanningLengthInMM(dScanningLengthInMM);
	//	}
	//}
	
	public void deriveScanningLengthFromDLPAndCTDIVolIfGreater(String dlp,String ctdiVol) {
		double dExistingScanningLength = 0;
		if (scanningLengthInMM != null && scanningLengthInMM.length() > 0) {
			try {
				dExistingScanningLength = Double.parseDouble(scanningLengthInMM);
			}
			catch (NumberFormatException e) {
				slf4jlogger.error("",e);
			}
		}
		double dDerivedScanningLengthInMM = computeScanningLengthFromDLPAndCTDIVol(dlp,ctdiVol);
		if (dDerivedScanningLengthInMM > 0) {
			if (dDerivedScanningLengthInMM > dExistingScanningLength) {
				replaceScanningLengthInMM(dDerivedScanningLengthInMM);
			}
			else {
				slf4jlogger.info("deriveScanningLengthFromDLPAndCTDIVolIfGreater(): not overriding {} with smaller {}",dExistingScanningLength,dDerivedScanningLengthInMM);
			}
		}
	}

	public static String locationSignToSI(String value) {
		if (value != null && value.length() > 0) {
			if (value.startsWith("-")) {
				value = "I"+value.substring(1);
			}
			else if (value.startsWith("+")) {
				value = "S"+value.substring(1);
			}
			else {
				value = "S"+value;
			}
		}
		return value;
	}
	
	public String toString() {
		return toString(false);
	}

	public String toString(boolean pretty) {
		StringBuffer buffer = new StringBuffer();
		if (!pretty) {
			buffer.append("\tIrradiationEventUID=");
			buffer.append(irradiationEventUID);
		}
		
		buffer.append("\t");
		buffer.append(scanType);

		buffer.append("\t");
		if (!pretty) {
			buffer.append("Anatomy=");
			buffer.append(anatomy);
		}
		else if (anatomy != null) {
			buffer.append(anatomy.getCodeMeaning());
		}

		buffer.append("\t");
		if (!pretty) {
			buffer.append("Protocol=");
			buffer.append(acquisitionProtocol);
		}
		else if (acquisitionProtocol != null) {
			buffer.append(acquisitionProtocol);
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("ScanningLength=");
		}
		if (!pretty || (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0)) {
			buffer.append(scanningLengthInMM);
			buffer.append(" mm");
			if (!pretty || (bottomZLocationOfScanningLength != null && bottomZLocationOfScanningLength.length() > 0) || (topZLocationOfScanningLength != null && topZLocationOfScanningLength.length() > 0)) {
				buffer.append(" [");
				if (!pretty || (bottomZLocationOfScanningLength != null && bottomZLocationOfScanningLength.trim().length() > 0)) {
					buffer.append(locationSignToSI(bottomZLocationOfScanningLength));
				}
				buffer.append("-");
				if (!pretty || (topZLocationOfScanningLength != null && topZLocationOfScanningLength.trim().length() > 0)) {
					buffer.append(locationSignToSI(topZLocationOfScanningLength));
				}
				buffer.append("]");
			}
		}
		
		if (!pretty || (lengthOfReconstructableVolumeInMM != null && lengthOfReconstructableVolumeInMM.trim().length() > 0)) {
			buffer.append("\t");
			if (!pretty) {
				buffer.append("LengthOfReconstructableVolume=");
			}
			buffer.append(lengthOfReconstructableVolumeInMM);
			buffer.append(" mm");
			if (!pretty || (bottomZLocationOfReconstructableVolume != null && bottomZLocationOfReconstructableVolume.length() > 0) || (topZLocationOfReconstructableVolume != null && topZLocationOfReconstructableVolume.length() > 0)) {
				buffer.append(" [");
				if (!pretty || (bottomZLocationOfReconstructableVolume != null && bottomZLocationOfReconstructableVolume.trim().length() > 0)) {
					buffer.append(locationSignToSI(bottomZLocationOfReconstructableVolume));
				}
				buffer.append("-");
				if (!pretty || (topZLocationOfReconstructableVolume != null && topZLocationOfReconstructableVolume.trim().length() > 0)) {
					buffer.append(locationSignToSI(topZLocationOfReconstructableVolume));
				}
				buffer.append("]");
			}
		}
		
		if (!pretty || (exposedRangeInMM != null && exposedRangeInMM.trim().length() > 0)) {
			buffer.append("\t");
			if (!pretty) {
				buffer.append("ExposedRange=");
			}
			buffer.append(exposedRangeInMM);
			buffer.append(" mm");
		}
		
		if (!pretty) {
			buffer.append("\t");
			buffer.append("FrameOfReferenceUID=");
			buffer.append(frameOfReferenceUID);
		}

		buffer.append("\t");
		if (!pretty) {
			buffer.append("Collimation single/total=");
		}
		if (!pretty || (nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.length() > 0) || (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.length() > 0)) {
			buffer.append(nominalSingleCollimationWidthInMM == null ? "" : nominalSingleCollimationWidthInMM);
			buffer.append("/");
			buffer.append(nominalTotalCollimationWidthInMM == null ? "" : nominalTotalCollimationWidthInMM);
			buffer.append(" mm");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("PitchFactor=");
		}
		if (!pretty || (pitchFactor != null && pitchFactor.trim().length() > 0)) {
			buffer.append(pitchFactor);
			buffer.append(":1");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("KVP=");
		}
		if (!pretty || (kvp != null && kvp.trim().length() > 0)) {
			buffer.append(kvp);
			buffer.append(" kVP");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("TubeCurrent/Max=");
		}
		if (!pretty || (tubeCurrent != null && tubeCurrent.trim().length() > 0) || (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0)) {
			buffer.append(tubeCurrent);
			buffer.append("/");
			buffer.append(tubeCurrentMaximum);
			buffer.append(" mA");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Exposure time/per rotation=");
		}
		if (!pretty || (exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) || (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0)) {
			buffer.append(exposureTimeInSeconds == null ? "" : exposureTimeInSeconds);
			buffer.append("/");
			buffer.append(exposureTimePerRotation == null ? "" : exposureTimePerRotation);
			buffer.append(" s");
		}

		buffer.append("\t");
		if (!pretty) {
			buffer.append("Comment=");
			buffer.append(comment);
		}
		else if (comment != null) {
			buffer.append(comment);
		}

		buffer.append("\n");

		return buffer.toString();
	}
	
	public static String getHTMLTableHeaderRowFragment() {
		return	 "<th>Type</th>"
				+"<th>Anatomy</th>"
				+"<th>Protocol</th>"
				+"<th>Scanning Length mm</th>"
				+"<th>Reconstructable Volume mm</th>"
				+"<th>Exposed Range mm</th>"
				+"<th>Collimation Single/Total mm</th>"
				+"<th>Pitch Factor</th>"
				+"<th>kVP</th>"
				+"<th>Tube Current Mean/Max mA</th>"
				+"<th>Exposure Time/Per Rotation s</th>"
				+"<th>Comment</th>";
	}
	
	public String getHTMLTableRowFragment() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<td>");
		if (scanType != null) {
			buffer.append(scanType);
		}
		buffer.append("</td>");

		buffer.append("<td>");
		if (anatomy != null) {
			buffer.append(anatomy.getCodeMeaning());
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (acquisitionProtocol != null) {
			buffer.append(acquisitionProtocol);
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0) {
			buffer.append(scanningLengthInMM);
			if (bottomZLocationOfScanningLength != null && bottomZLocationOfScanningLength.trim().length() > 0 && topZLocationOfScanningLength != null && topZLocationOfScanningLength.trim().length() > 0) {
				buffer.append(" [");
				buffer.append(locationSignToSI(bottomZLocationOfScanningLength));
				buffer.append("-");
				buffer.append(locationSignToSI(topZLocationOfScanningLength));
				buffer.append("]");
			}
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (lengthOfReconstructableVolumeInMM != null && lengthOfReconstructableVolumeInMM.trim().length() > 0) {
			buffer.append(lengthOfReconstructableVolumeInMM);
			if (bottomZLocationOfReconstructableVolume != null && bottomZLocationOfReconstructableVolume.trim().length() > 0 && topZLocationOfReconstructableVolume != null && topZLocationOfReconstructableVolume.trim().length() > 0) {
				buffer.append(" [");
				buffer.append(locationSignToSI(bottomZLocationOfReconstructableVolume));
				buffer.append("-");
				buffer.append(locationSignToSI(topZLocationOfReconstructableVolume));
				buffer.append("]");
			}
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (exposedRangeInMM != null && exposedRangeInMM.trim().length() > 0) {
			buffer.append(exposedRangeInMM);
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.length() > 0) || (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.length() > 0)) {
			buffer.append(nominalSingleCollimationWidthInMM == null ? "" : nominalSingleCollimationWidthInMM);
			buffer.append("/");
			buffer.append(nominalTotalCollimationWidthInMM == null ? "" : nominalTotalCollimationWidthInMM);
			//buffer.append(" mm");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (pitchFactor != null && pitchFactor.trim().length() > 0) {
			buffer.append(pitchFactor);
			buffer.append(":1");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (kvp != null && kvp.trim().length() > 0) {
			buffer.append(kvp);
			//buffer.append(" kVP");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((tubeCurrent != null && tubeCurrent.trim().length() > 0) || (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0)) {
			buffer.append(tubeCurrent);
			buffer.append("/");
			buffer.append(tubeCurrentMaximum);
			//buffer.append(" mA");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) || (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0)) {
			buffer.append(exposureTimeInSeconds == null ? "" : exposureTimeInSeconds);
			buffer.append("/");
			buffer.append(exposureTimePerRotation == null ? "" : exposureTimePerRotation);
			//buffer.append(" s");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (comment != null) {
			buffer.append(comment);
		}
		buffer.append("</td>");

		return buffer.toString();
	}
	
	public ContentItem getStructuredReportFragment(ContentItem root) throws DicomException {
		if (contentItemFragment == null) {
			boolean needFrameOfReferenceUIDInSR = false;
			ContentItemFactory cif = new ContentItemFactory();
			contentItemFragment = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113822","DCM","CT Acquisition Parameters"),true/*continuityOfContentIsSeparate*/);
			if (exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113824","DCM","Exposure Time"),exposureTimeInSeconds,new CodedSequenceItem("s","UCUM","1.8","s"));
			}
			if (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113825","DCM","Scanning Length"),scanningLengthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (lengthOfReconstructableVolumeInMM != null && lengthOfReconstructableVolumeInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113893","DCM","Length of Reconstructable Volume"),lengthOfReconstructableVolumeInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (exposedRangeInMM != null && exposedRangeInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113899","DCM","Exposed Range"),exposedRangeInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (topZLocationOfReconstructableVolume != null && topZLocationOfReconstructableVolume.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113895","DCM","Top Z Location of Reconstructable Volume"),topZLocationOfReconstructableVolume,new CodedSequenceItem("mm","UCUM","1.8","mm"));
				needFrameOfReferenceUIDInSR = true;
			}
			if (bottomZLocationOfReconstructableVolume != null && bottomZLocationOfReconstructableVolume.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113896","DCM","Bottom Z Location of Reconstructable Volume"),bottomZLocationOfReconstructableVolume,new CodedSequenceItem("mm","UCUM","1.8","mm"));
				needFrameOfReferenceUIDInSR = true;
			}
			if (topZLocationOfScanningLength != null && topZLocationOfScanningLength.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113897","DCM","Top Z Location of Scanning Length"),topZLocationOfScanningLength,new CodedSequenceItem("mm","UCUM","1.8","mm"));
				needFrameOfReferenceUIDInSR = true;
			}
			if (bottomZLocationOfScanningLength != null && bottomZLocationOfScanningLength.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113898","DCM","Bottom Z Location of Scanning Length"),bottomZLocationOfScanningLength,new CodedSequenceItem("mm","UCUM","1.8","mm"));
				needFrameOfReferenceUIDInSR = true;
			}
			if (needFrameOfReferenceUIDInSR && frameOfReferenceUID != null && frameOfReferenceUID.trim().length() > 0) {
				cif.new UIDContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("112227","DCM","Frame of Reference UID"),frameOfReferenceUID);
			}
			if (nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113826","DCM","Nominal Single Collimation Width"),nominalSingleCollimationWidthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113827","DCM","Nominal Total Collimation Width"),nominalTotalCollimationWidthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (pitchFactor != null && pitchFactor.trim().length() > 0) {
				if (scanType == null || scanType.equals(CTScanType.AXIAL) || scanType.equals(CTScanType.HELICAL) || scanType.equals(CTScanType.UNKNOWN)) {	// i.e., not if known to be stationary
					cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113828","DCM","Pitch Factor"),pitchFactor,new CodedSequenceItem("{ratio}","UCUM","1.8","ratio"));
				}
			}
			// Do not create any X-Ray Source stuf if nothing to put in it, else round trip will fail to match ...
			if (kvp != null && kvp.trim().length() > 0
			 || tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0
			 || tubeCurrent != null && tubeCurrent.trim().length() > 0
			 || exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113823","DCM","Number of X-Ray Sources"),"1",new CodedSequenceItem("{X-Ray sources}","UCUM","1.8","X-Ray sources"));
				ContentItem source = cif.new ContainerContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113831","DCM","CT X-Ray Source Parameters"),true/*continuityOfContentIsSeparate*/);
				cif.new TextContentItem(source,"CONTAINS",new CodedSequenceItem("113832","DCM","Identification of the X-Ray Source"),"1");
				if (kvp != null && kvp.trim().length() > 0) {
					cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113733","DCM","KVP"),kvp,new CodedSequenceItem("kV","UCUM","1.8","kV"));
				}
				if (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0) {
					cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113833","DCM","Maximum X-Ray Tube Current"),tubeCurrentMaximum,new CodedSequenceItem("mA","UCUM","1.8","mA"));
				}
				if (tubeCurrent != null && tubeCurrent.trim().length() > 0) {
					cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113734","DCM","X-Ray Tube Current"),tubeCurrent,new CodedSequenceItem("mA","UCUM","1.8","mA"));
				}
				if (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0) {
					cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113834","DCM","Exposure Time per Rotation"),exposureTimePerRotation,new CodedSequenceItem("s","UCUM","1.8","s"));
				}
				//>>>	CONTAINS	NUM	EV (113821, DCM, "X-ray Filter Aluminum Equivalent")	1	U		Units = EV (mm, UCUM, "mm")
			}
		}
		return contentItemFragment;
	}
}


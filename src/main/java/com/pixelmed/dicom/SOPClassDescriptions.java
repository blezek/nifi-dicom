/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.HashMap;

/**
 * <p>A class of static methods to provide a means of describing SOP Classes with abbreviations
 * and hman-readable descriptions.</p>
 *
 * @author	dclunie
 */
public class SOPClassDescriptions {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SOPClassDescriptions.java,v 1.53 2017/04/21 22:56:42 dclunie Exp $";
	
	private static SOPClassDescriptions ourself = new SOPClassDescriptions();
	
	private HashMap abbreviationsByUID;
	private HashMap descriptionsByUID;

	private SOPClassDescriptions() {
		createAbbreviationsByUID();
		createDescriptionsByUID();
	}

	/**
	 * @param	sopClassUID	UID of the SOP Class, as a String without trailing zero padding
	 * @return			a short abbreviation describing the SOP Class, or an empty string if none
	 */
	public static String getAbbreviationFromUID(String sopClassUID) {
		String abbreviation = (String)ourself.abbreviationsByUID.get(sopClassUID);
		return abbreviation == null ? "" : abbreviation;
	}

	/**
	 * @param	sopClassUID	UID of the SOP Class, as a String without trailing zero padding
	 * @return			a description of the SOP Class, or an empty string if none
	 */
	public static String getDescriptionFromUID(String sopClassUID) {
		String description = (String)ourself.descriptionsByUID.get(sopClassUID);
		return description == null ? "" : description;
	}
	
	private void createAbbreviationsByUID() {
		abbreviationsByUID = new HashMap();

		abbreviationsByUID.put(SOPClass.Verification,"VFY");
		
		abbreviationsByUID.put(SOPClass.ComputedRadiographyImageStorage,"CR");
		abbreviationsByUID.put(SOPClass.DigitalXRayImageStorageForPresentation,"DX(Pres)");
		abbreviationsByUID.put(SOPClass.DigitalXRayImageStorageForProcessing,"DX(Proc)");
		abbreviationsByUID.put(SOPClass.DigitalMammographyXRayImageStorageForPresentation,"MG(Pres)");
		abbreviationsByUID.put(SOPClass.DigitalMammographyXRayImageStorageForProcessing,"MG(Proc)");
		abbreviationsByUID.put(SOPClass.DigitalIntraoralXRayImageStorageForPresentation,"IO(Pres)");
		abbreviationsByUID.put(SOPClass.DigitalIntraoralXRayImageStorageForProcessing,"IO(Proc)");
		abbreviationsByUID.put(SOPClass.CTImageStorage,"CT");
		abbreviationsByUID.put(SOPClass.EnhancedCTImageStorage,"CT(MF)");
		abbreviationsByUID.put(SOPClass.LegacyConvertedEnhancedCTImageStorage,"CT(MF-L)");
		abbreviationsByUID.put(SOPClass.UltrasoundMultiframeImageStorageRetired,"US(MF)");
		abbreviationsByUID.put(SOPClass.UltrasoundMultiframeImageStorage,"US");
		abbreviationsByUID.put(SOPClass.MRImageStorage,"MR");
		abbreviationsByUID.put(SOPClass.EnhancedMRImageStorage,"MR(MF)");
		abbreviationsByUID.put(SOPClass.EnhancedMRColorImageStorage,"MR(MFColor)");
		abbreviationsByUID.put(SOPClass.LegacyConvertedEnhancedMRImageStorage,"MR(MF-L)");
		abbreviationsByUID.put(SOPClass.NuclearMedicineImageStorageRetired,"NM(Ret)");
		abbreviationsByUID.put(SOPClass.UltrasoundImageStorageRetired,"US(Ret)");
		abbreviationsByUID.put(SOPClass.UltrasoundImageStorage,"US");
		abbreviationsByUID.put(SOPClass.EnhancedUSVolumeStorage,"US(Vol)");
		abbreviationsByUID.put(SOPClass.SecondaryCaptureImageStorage,"SC");
		abbreviationsByUID.put(SOPClass.MultiframeSingleBitSecondaryCaptureImageStorage,"SC(MF1Bit)");
		abbreviationsByUID.put(SOPClass.MultiframeGrayscaleByteSecondaryCaptureImageStorage,"SC(MFGrayByte)");
		abbreviationsByUID.put(SOPClass.MultiframeGrayscaleWordSecondaryCaptureImageStorage,"SC(MFGrayWord)");
		abbreviationsByUID.put(SOPClass.MultiframeTrueColorSecondaryCaptureImageStorage,"SC(MFColor)");
		abbreviationsByUID.put(SOPClass.XRayAngiographicImageStorage,"XA");
		abbreviationsByUID.put(SOPClass.EnhancedXAImageStorage,"XA(Enh)");
		abbreviationsByUID.put(SOPClass.XRayRadioFlouroscopicImageStorage,"XRF");
		abbreviationsByUID.put(SOPClass.EnhancedXRFImageStorage,"XRF(Enh)");
		abbreviationsByUID.put(SOPClass.XRayAngiographicBiplaneImageStorage,"XA(Bi)");
		abbreviationsByUID.put(SOPClass.XRay3DAngiographicImageStorage,"XA(3D)");
		abbreviationsByUID.put(SOPClass.XRay3DCraniofacialImageStorage,"DX(3D CF)");
		abbreviationsByUID.put(SOPClass.BreastTomosynthesisImageStorage,"MG(Tomo)");
		abbreviationsByUID.put(SOPClass.BreastProjectionXRayImageStorageForPresentation,"MG(Proj Pres)");
		abbreviationsByUID.put(SOPClass.BreastProjectionXRayImageStorageForProcessing,"MG(Proj Proc)");
		
		abbreviationsByUID.put(SOPClass.NuclearMedicineImageStorage,"NM");
		abbreviationsByUID.put(SOPClass.VisibleLightEndoscopicImageStorage,"VL(Endo)");				// ES ?
		abbreviationsByUID.put(SOPClass.VideoEndoscopicImageStorage,"VLMF(Endo)");				// ES ?
		abbreviationsByUID.put(SOPClass.VisibleLightMicroscopicImageStorage,"VL(Micro)");			// GM ?
		abbreviationsByUID.put(SOPClass.VideoMicroscopicImageStorage,"VLMF(Micro)");				// GM ?
		abbreviationsByUID.put(SOPClass.VisibleLightSlideCoordinatesMicroscopicImageStorage,"VL(Slide)");	// SM ?
		abbreviationsByUID.put(SOPClass.VisibleLightPhotographicImageStorage,"VL(Photo)");			// XC ?
		abbreviationsByUID.put(SOPClass.VideoPhotographicImageStorage,"VLMF(Photo)");				// XC ?
		abbreviationsByUID.put(SOPClass.OphthalmicPhotography8BitImageStorage,"OP");
		abbreviationsByUID.put(SOPClass.OphthalmicPhotography16BitImageStorage,"OP");
		abbreviationsByUID.put(SOPClass.OphthalmicTomographyImageStorage,"OPT");
		abbreviationsByUID.put(SOPClass.OphthalmicOpticalCoherenceTomographyEnFaceImageStorage,"OPT");
		abbreviationsByUID.put(SOPClass.OphthalmicOpticalCoherenceTomographyBscanVolumeAnalysisStorage,"OPT");
		abbreviationsByUID.put(SOPClass.WideFieldOphthalmicPhotographyStereographicProjectionImageStorage,"OP");
		abbreviationsByUID.put(SOPClass.WideFieldOphthalmicPhotography3DCoordinatesImageStorage,"OP");
		abbreviationsByUID.put(SOPClass.VLWholeSlideMicroscopyImageStorage,"VL(WS)");	// SM ?
		
		abbreviationsByUID.put(SOPClass.PETImageStorage,"PET");
		abbreviationsByUID.put(SOPClass.EnhancedPETImageStorage,"PET(MF)");
		abbreviationsByUID.put(SOPClass.LegacyConvertedEnhancedPETImageStorage,"PET(MF-L)");

		abbreviationsByUID.put(SOPClass.IVOCTImageStorageForPresentation,"IVOCT(Pres)");
		abbreviationsByUID.put(SOPClass.IVOCTImageStorageForProcessing,"IVOCT(Proc)");

		abbreviationsByUID.put(SOPClass.MediaStorageDirectoryStorage,"DICOMDIR");
		
		abbreviationsByUID.put(SOPClass.BasicTextSRStorage,"SR(Text)");
		abbreviationsByUID.put(SOPClass.EnhancedSRStorage,"SR(Enh)");
		abbreviationsByUID.put(SOPClass.ComprehensiveSRStorage,"SR(Comp)");
		abbreviationsByUID.put(SOPClass.Comprehensive3DSRStorage,"SR(Comp3D)");
		abbreviationsByUID.put(SOPClass.ExtensibleSRStorage,"SR(Ext)");
		abbreviationsByUID.put(SOPClass.MammographyCADSRStorage,"CAD(Mammo)");
		abbreviationsByUID.put(SOPClass.ChestCADSRStorage,"CAD(Chest)");
		abbreviationsByUID.put(SOPClass.ProcedureLogStorage,"LOG(Procedure)");
		abbreviationsByUID.put(SOPClass.XRayRadiationDoseSRStorage,"DOSE(XRay)");
		abbreviationsByUID.put(SOPClass.RadiopharmaceuticalRadiationDoseSRStorage,"DOSE(Nuc)");
		abbreviationsByUID.put(SOPClass.ColonCADSRStorage,"CAD(Colon)");
		abbreviationsByUID.put(SOPClass.ImplantationPlanSRStorage,"Plan(Implant)");
		abbreviationsByUID.put(SOPClass.AcquisitionContextSRStorage,"SR(AcqCtx)");
		abbreviationsByUID.put(SOPClass.SimplifiedAdultEchoSRStorage,"SR(Echo)");
		abbreviationsByUID.put(SOPClass.PatientRadiationDoseSRStorage,"DOSE(Pt)");
		abbreviationsByUID.put(SOPClass.MacularGridThicknessAndVolumeReportStorage,"SR(Macula)");
		abbreviationsByUID.put(SOPClass.KeyObjectSelectionDocumentStorage,"KO");
		
		
		abbreviationsByUID.put(SOPClass.TextSRStorageTrialRetired,"SR(Text)(Trial)");
		abbreviationsByUID.put(SOPClass.AudioSRStorageTrialRetired,"SR(Audio)(Trial)");
		abbreviationsByUID.put(SOPClass.DetailSRStorageTrialRetired,"SR(Detail)(Trial)");
		abbreviationsByUID.put(SOPClass.ComprehensiveSRStorageTrialRetired,"SR(Comp)(Trial)");
		
		abbreviationsByUID.put(SOPClass.GrayscaleSoftcopyPresentationStateStorage,"PS(Gray)");
		abbreviationsByUID.put(SOPClass.ColorSoftcopyPresentationStateStorage,"PS(Color)");
		abbreviationsByUID.put(SOPClass.PseudoColorSoftcopyPresentationStateStorage,"PS(Pseudo)");
		abbreviationsByUID.put(SOPClass.BlendingSoftcopyPresentationStateStorage,"PS(Blend)");
		abbreviationsByUID.put(SOPClass.XAXRFGrayscaleSoftcopyPresentationStateStorage,"PS(XAXRF)");
		abbreviationsByUID.put(SOPClass.GrayscalePlanarMPRVolumetricPresentationStateStorage,"PS(MPRGray)");
		abbreviationsByUID.put(SOPClass.CompositingPlanarMPRVolumetricPresentationStateStorage,"PS(MPRComp)");
		abbreviationsByUID.put(SOPClass.AdvancedBlendingPresentationStateStorage,"PS(Adv.Blend)");
		abbreviationsByUID.put(SOPClass.VolumeRenderingVolumetricPresentationStateStorage,"PS(Vol)");
		abbreviationsByUID.put(SOPClass.SegmentedVolumeRenderingVolumetricPresentationStateStorage,"PS(Vol.Seg)");
		abbreviationsByUID.put(SOPClass.MultipleVolumeRenderingVolumetricPresentationStateStorage,"PS(Vol.Multi)");
		
		abbreviationsByUID.put(SOPClass.TwelveLeadECGStorage,"ECG(12)");
		abbreviationsByUID.put(SOPClass.GeneralECGStorage,"ECG(Gen)");
		abbreviationsByUID.put(SOPClass.AmbulatoryECGStorage,"ECG(Amb)");
		abbreviationsByUID.put(SOPClass.HemodynamicWaveformStorage,"HD");
		abbreviationsByUID.put(SOPClass.CardiacElectrophysiologyWaveformStorage,"EPS");
		abbreviationsByUID.put(SOPClass.ArterialPulseWaveformStorage,"ART");
		abbreviationsByUID.put(SOPClass.RespiratoryWaveformStorage,"RESP");
		abbreviationsByUID.put(SOPClass.BasicVoiceStorage,"AU(Voice)");
		abbreviationsByUID.put(SOPClass.GeneralAudioWaveformStorage,"AU");

		abbreviationsByUID.put(SOPClass.StandaloneOverlayStorage,"OVERLAY");
		abbreviationsByUID.put(SOPClass.StandaloneCurveStorage,"CURVE");
		abbreviationsByUID.put(SOPClass.StandaloneModalityLUTStorage,"MODLUT");
		abbreviationsByUID.put(SOPClass.StandaloneVOILUTStorage,"VOILUT");
		abbreviationsByUID.put(SOPClass.StandalonePETCurveStorage,"PETCURVE");
		
		abbreviationsByUID.put(SOPClass.RTDoseStorage,"RTDOSE");
		abbreviationsByUID.put(SOPClass.RTStructureSetStorage,"RTSTRUCT");
		abbreviationsByUID.put(SOPClass.RTBeamsTreatmentRecordStorage,"RTRECORD(Beams)");
		abbreviationsByUID.put(SOPClass.RTIonBeamsTreatmentRecordStorage,"RTRECORD(IonBeams)");
		abbreviationsByUID.put(SOPClass.RTPlanStorage,"RTPLAN");
		abbreviationsByUID.put(SOPClass.RTIonPlanStorage,"RTPLAN(Ion)");
		abbreviationsByUID.put(SOPClass.RTBrachyTreatmentRecordStorage,"RTRECORD(Brachy)");
		abbreviationsByUID.put(SOPClass.RTTreatmentSummaryRecordStorage,"RTRECORD(Summary)");
		abbreviationsByUID.put(SOPClass.RTBeamsDeliveryInstructionStorageTrial,"RT BEAMS DELIVERY");
		abbreviationsByUID.put(SOPClass.RTBeamsDeliveryInstructionStorage,"RT BEAMS DELIVERY");

		abbreviationsByUID.put(SOPClass.MRSpectroscopyStorage,"MR(Spectro)");
		
		abbreviationsByUID.put(SOPClass.RawDataStorage,"RAW");

		abbreviationsByUID.put(SOPClass.SpatialRegistrationStorage,"REG");
		abbreviationsByUID.put(SOPClass.SpatialFiducialsStorage,"FID");
		abbreviationsByUID.put(SOPClass.DeformableSpatialRegistrationStorage,"REG");

		abbreviationsByUID.put(SOPClass.StereometricRelationshipStorage,"STR");
		abbreviationsByUID.put(SOPClass.RealWorldValueMappingStorage,"RWV");

		abbreviationsByUID.put(SOPClass.EncapsulatedPDFStorage,"ENCAP(PDF)");
		abbreviationsByUID.put(SOPClass.EncapsulatedCDAStorage,"ENCAP(CDA)");

		abbreviationsByUID.put(SOPClass.StudyRootQueryRetrieveInformationModelFind,"FIND(Study)");
		abbreviationsByUID.put(SOPClass.StudyRootQueryRetrieveInformationModelMove,"MOVE(Study)");

		abbreviationsByUID.put(SOPClass.SegmentationStorage,"SEG(Img)");
		abbreviationsByUID.put(SOPClass.SurfaceSegmentationStorage,"SEG(Surf)");
		abbreviationsByUID.put(SOPClass.TractographyResultsStorage,"TRACT");

		abbreviationsByUID.put(SOPClass.LensometryMeasurementsStorage,"LEN");
		abbreviationsByUID.put(SOPClass.AutorefractionMeasurementsStorage,"AR");
		abbreviationsByUID.put(SOPClass.KeratometryMeasurementsStorage,"KER");
		abbreviationsByUID.put(SOPClass.SubjectiveRefractionMeasurementsStorage,"SRF");
		abbreviationsByUID.put(SOPClass.VisualAcuityMeasurementsStorage,"VA");
		abbreviationsByUID.put(SOPClass.SpectaclePrescriptionReportStorage,"SR(Spec)");
		abbreviationsByUID.put(SOPClass.OphthalmicAxialMeasurementsStorage,"IOL(AxMx)");
		abbreviationsByUID.put(SOPClass.IntraocularLensCalculationsStorage,"IOL(Calc)");
		abbreviationsByUID.put(SOPClass.OphthalmicVisualFieldStaticPerimetryMeasurementsStorage,"OPV");
		abbreviationsByUID.put(SOPClass.OphthalmicThicknessMapStorage,"OPM");
		abbreviationsByUID.put(SOPClass.CornealTopographyMapStorage,"CM");
		
		abbreviationsByUID.put(SOPClass.ColorPaletteStorage,"PAL");

		abbreviationsByUID.put(SOPClass.GenericImplantTemplateStorage,"Implant");
		abbreviationsByUID.put(SOPClass.ImplantAssemblyTemplateStorage,"Implant(ASSY)");
		abbreviationsByUID.put(SOPClass.ImplantTemplateGroupStorage,"Implant(GROUP)");

		abbreviationsByUID.put(SOPClass.BasicStructuredDisplayStorage,"DISP");
		
		abbreviationsByUID.put(SOPClass.PrivateGEDicomMRImageInfoObject,"MR(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGEDicomCTImageInfoObject,"MR(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGEDicomDisplayImageInfoObject,"SC(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGEPETRawDataStorage,"RAWPET(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGE3DModelStorage,"3D(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGEeNTEGRAProtocolOrNMGenieStorage,"NM(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGECollageStorage,"COLLAGE(GE)");
		abbreviationsByUID.put(SOPClass.PrivateGERTPlanStorage,"RTPLAN(GE)");
		
		abbreviationsByUID.put(SOPClass.PrivateSiemensCSANonImageStorage,"CSA(Siemens)");
		abbreviationsByUID.put(SOPClass.PrivateSiemensCTMRVolumeStorage,"Vol(Siemens)");
		abbreviationsByUID.put(SOPClass.PrivateSiemensAXFrameSetsStorage,"Frames(Siemens)");
		
		abbreviationsByUID.put(SOPClass.PrivateAgfaBasicAttributePresentationStateStorage,"CHANGE(Agfa)");
		
		abbreviationsByUID.put(SOPClass.PrivateMedicalInsight3DSoftcopyPresentationStateStorage,"PS(3D MI)");
		
		abbreviationsByUID.put(SOPClass.PrivateAcusonStructuredReportDetailStorage,"SR(Acuson)");
		
		abbreviationsByUID.put(SOPClass.PrivateTomTecAnnotationStorage,"Ann(TomTec)");

		abbreviationsByUID.put(SOPClass.PrivateFujiCRImageStorage,"CR(Fuji)");

		abbreviationsByUID.put(SOPClass.PrivatePhilipsCXImageStorage,"CX(Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsVolumeStorage,"OT(Vol Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsVolume2Storage,"OT(Vol Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilips3DObjectStorage,"3D(Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilips3DObject2Storage,"3D(Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsSurfaceStorage,"SEG(Surf Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsSurface2Storage,"SEG(Surf Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsCompositeObjectStorage,"COMP(Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRCardioProfileStorage,"MR(Cardio Profile Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRCardioStorage,"MR(Cardio Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRCardio2Storage,"MR(Cardio Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsCTSyntheticImageStorage,"CT(Synthetic Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRSyntheticImageStorage,"MR(Synthetic Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRCardioAnalysisStorage,"MR(Cardio Anal Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRCardioAnalysis2Storage,"MR(Cardio Anal Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsCXSyntheticImageStorage,"CX(Synthetic Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRSpectrumStorage,"MR(Spectro Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRSeriesDataStorage,"MR(Series Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRColorImageStorage,"MR(Color Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsMRExamcardStorage,"MR(Exam Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsSpecialisedXAStorage,"XA(Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilips3DPresentationStateStorage,"PS(3D Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsPerfusionStorage,"OT(Perfusion Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsPerfusionImageStorage,"OT(Perfusion Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsVRMLStorage,"XR(VRML Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsVolumeSetStorage,"XR(Volume Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsLiveRunStorage,"XR(Live Run Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsRunStorage,"XR(Run Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsReconstructionStorage,"XR(Recon Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsXRayMFStorage,"XR(MF Philips)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsHPLive3D01Storage,"US3D(HP)");
		abbreviationsByUID.put(SOPClass.PrivatePhilipsHPLive3D02Storage,"US3D(HP)");
		
		abbreviationsByUID.put(SOPClass.PrivatePMODMultiframeImageStorage,"(MF PMOD)");
		
		abbreviationsByUID.put(SOPClass.PrivateToshibaUSImageStorage,"US(Toshiba)");

		abbreviationsByUID.put(SOPClass.PrivateERADPracticeBuilderReportTextStorage,"RPT(TXT)");
		abbreviationsByUID.put(SOPClass.PrivateERADPracticeBuilderReportDictationStorage,"RPT(DICT)");

		abbreviationsByUID.put(SOPClass.PrivateDcm4cheUpgradedCTImageStorage,"CT(Dcm4che)");
		abbreviationsByUID.put(SOPClass.PrivateDcm4cheUpgradedMRImageStorage,"MR(Dcm4che)");
		abbreviationsByUID.put(SOPClass.PrivateDcm4cheUpgradedPETImageStorage,"PET(Dcm4che)");
		abbreviationsByUID.put(SOPClass.PrivateDcm4cheEncapsulatedDocumentStorage,"ENCAP(Dcm4che)");
		
		abbreviationsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedCTImageStorage,"CT(MF-L)");
		abbreviationsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedMRImageStorage,"MR(MF-L)");
		abbreviationsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedPETImageStorage,"PET(MF-L)");
		abbreviationsByUID.put(SOPClass.PrivatePixelMedFloatingPointImageStorage,"FP(MF)");

		abbreviationsByUID.put(SOPClass.DICOSCTImageStorage,"CT(DICOS)");
		abbreviationsByUID.put(SOPClass.DICOSDigitalXRayImageStorageForPresentation,"DX(Pres DICOS)");
		abbreviationsByUID.put(SOPClass.DICOSDigitalXRayImageStorageForProcessing,"DX(Proc DICOS)");
		abbreviationsByUID.put(SOPClass.DICOSThreatDetectionReportStorage,"THREAT(DICOS)");
		abbreviationsByUID.put(SOPClass.DICOS2DAITStorage,"AIT2D(DICOS)");
		abbreviationsByUID.put(SOPClass.DICOS3DAITStorage,"AIT3D(DICOS)");
		abbreviationsByUID.put(SOPClass.DICOSQuadrupoleResonanceStorage,"QR(DICOS)");

		abbreviationsByUID.put(SOPClass.DICONDEEddyCurrentImageStorage,"EC");
		abbreviationsByUID.put(SOPClass.DICONDEEddyCurrentMultiframeImageStorage,"EC(MF)");
	}

	private void createDescriptionsByUID() {
		descriptionsByUID = new HashMap();

		descriptionsByUID.put(SOPClass.Verification,"Verification");
		
		descriptionsByUID.put(SOPClass.ComputedRadiographyImageStorage,"Computed Radiography Image Storage");
		descriptionsByUID.put(SOPClass.DigitalXRayImageStorageForPresentation,"Digital X-Ray Image Storage (For Presentation)");
		descriptionsByUID.put(SOPClass.DigitalXRayImageStorageForProcessing,"Digital X-Ray Image Storage (For Processing)");
		descriptionsByUID.put(SOPClass.DigitalMammographyXRayImageStorageForPresentation,"Digital Mammography X-Ray Image Storage (For Presentation)");
		descriptionsByUID.put(SOPClass.DigitalMammographyXRayImageStorageForProcessing,"Digital Mammography X-Ray Image Storage (For Processing)");
		descriptionsByUID.put(SOPClass.DigitalIntraoralXRayImageStorageForPresentation,"Digital Intraoral X-Ray Image Storage (For Presentation)");
		descriptionsByUID.put(SOPClass.DigitalIntraoralXRayImageStorageForProcessing,"Digital Intraoral X-Ray Image Storage (For Processing)");
		descriptionsByUID.put(SOPClass.CTImageStorage,"CT Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedCTImageStorage,"Enhanced CT Image Storage");
		descriptionsByUID.put(SOPClass.LegacyConvertedEnhancedCTImageStorage,"Legacy Converted Enhanced CT Image Storage");
		descriptionsByUID.put(SOPClass.UltrasoundMultiframeImageStorageRetired,"Ultrasound Multiframe Image Storage (Retired)");
		descriptionsByUID.put(SOPClass.UltrasoundMultiframeImageStorage,"Ultrasound Multiframe Image Storage");
		descriptionsByUID.put(SOPClass.MRImageStorage,"MR Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedMRImageStorage,"Enhanced MR Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedMRColorImageStorage,"Enhanced MR Color Image Storage");
		descriptionsByUID.put(SOPClass.LegacyConvertedEnhancedMRImageStorage,"Legacy Converted Enhanced MR Image Storage");
		descriptionsByUID.put(SOPClass.NuclearMedicineImageStorageRetired,"Nuclear Medicine Image Storage (Retired)");
		descriptionsByUID.put(SOPClass.UltrasoundImageStorageRetired,"Ultrasound Image Storage (Retired)");
		descriptionsByUID.put(SOPClass.UltrasoundImageStorage,"Ultrasound Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedUSVolumeStorage,"Enhanced US Volume Storage");
		descriptionsByUID.put(SOPClass.SecondaryCaptureImageStorage,"Secondary Capture Image Storage");
		descriptionsByUID.put(SOPClass.MultiframeSingleBitSecondaryCaptureImageStorage,"Multiframe Single Bit Secondary Capture Image Storage");
		descriptionsByUID.put(SOPClass.MultiframeGrayscaleByteSecondaryCaptureImageStorage,"Multiframe Grayscale Byte Secondary Capture Image Storage");
		descriptionsByUID.put(SOPClass.MultiframeGrayscaleWordSecondaryCaptureImageStorage,"Multiframe Grayscale Word Secondary Capture Image Storage");
		descriptionsByUID.put(SOPClass.MultiframeTrueColorSecondaryCaptureImageStorage,"Multiframe True Color Secondary Capture Image Storage");
		descriptionsByUID.put(SOPClass.XRayAngiographicImageStorage,"X-Ray Angiographic Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedXAImageStorage,"Enhanced XA Image Storage");
		descriptionsByUID.put(SOPClass.XRayRadioFlouroscopicImageStorage,"X-Ray Radio Flouroscopic Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedXRFImageStorage,"Enhanced XRF Image Storage");
		descriptionsByUID.put(SOPClass.XRayAngiographicBiplaneImageStorage,"X-Ray Angiographic Biplane Image Storage");
		descriptionsByUID.put(SOPClass.XRay3DAngiographicImageStorage,"X-Ray 3D Angiographic Image Storage");
		descriptionsByUID.put(SOPClass.XRay3DCraniofacialImageStorage,"X-Ray 3D Craniofacial Image Storage");
		descriptionsByUID.put(SOPClass.BreastTomosynthesisImageStorage,"Breast Tomosynthesis Image Storage");
		descriptionsByUID.put(SOPClass.BreastProjectionXRayImageStorageForPresentation,"Breast Projection X-Ray Image Storage - For Presentation Image Storage");
		descriptionsByUID.put(SOPClass.BreastProjectionXRayImageStorageForProcessing,"Breast Projection X-Ray Image Storage - For Processing Image Storage");
		descriptionsByUID.put(SOPClass.NuclearMedicineImageStorage,"Nuclear Medicine Image Storage");
		descriptionsByUID.put(SOPClass.VisibleLightEndoscopicImageStorage,"Visible Light Endoscopic Image Storage");
		descriptionsByUID.put(SOPClass.VideoEndoscopicImageStorage,"Video Endoscopic Image Storage");
		descriptionsByUID.put(SOPClass.VisibleLightMicroscopicImageStorage,"Visible Light Microscopic Image Storage");
		descriptionsByUID.put(SOPClass.VideoMicroscopicImageStorage,"Video Microscopic Image Storage");
		descriptionsByUID.put(SOPClass.VisibleLightSlideCoordinatesMicroscopicImageStorage,"Visible Light Slide Coordinates Microscopic Image Storage");
		descriptionsByUID.put(SOPClass.VisibleLightPhotographicImageStorage,"Visible Light Photographic Image Storage");
		descriptionsByUID.put(SOPClass.VideoPhotographicImageStorage,"Video Photographic Image Storage");
		descriptionsByUID.put(SOPClass.OphthalmicPhotography8BitImageStorage,"Ophthalmic Photography 8 Bit Image Storage");
		descriptionsByUID.put(SOPClass.OphthalmicPhotography16BitImageStorage,"Ophthalmic Photography 16 Bit Image Storage");
		descriptionsByUID.put(SOPClass.OphthalmicTomographyImageStorage,"Ophthalmic Tomography Image Storage");
		descriptionsByUID.put(SOPClass.OphthalmicOpticalCoherenceTomographyEnFaceImageStorage,"Ophthalmic Optical Coherence Tomography En Face Image Storage");
		descriptionsByUID.put(SOPClass.OphthalmicOpticalCoherenceTomographyBscanVolumeAnalysisStorage,"Ophthalmic Optical Coherence Tomography B-scan Volume Analysis Storage");
		descriptionsByUID.put(SOPClass.VLWholeSlideMicroscopyImageStorage,"VL Whole Slide Microscopy Image Storage");
		descriptionsByUID.put(SOPClass.WideFieldOphthalmicPhotographyStereographicProjectionImageStorage,"Wide Field Ophthalmic Photography Stereographic Projection Image Storage");
		descriptionsByUID.put(SOPClass.WideFieldOphthalmicPhotography3DCoordinatesImageStorage,"Wide Field Ophthalmic Photography 3D Coordinates Image Storage");
		descriptionsByUID.put(SOPClass.PETImageStorage,"PET Image Storage");
		descriptionsByUID.put(SOPClass.EnhancedPETImageStorage,"Enhanced PET Image Storage");
		descriptionsByUID.put(SOPClass.LegacyConvertedEnhancedPETImageStorage,"Legacy Converted Enhanced PET Image Storage");
		descriptionsByUID.put(SOPClass.RTImageStorage,"RT Image Storage");
		descriptionsByUID.put(SOPClass.IVOCTImageStorageForPresentation,"Intravascular OCT Image Storage (For Presentation)");
		descriptionsByUID.put(SOPClass.IVOCTImageStorageForProcessing,"Intravascular OCT Image Storage (For Processing)");

		descriptionsByUID.put(SOPClass.MediaStorageDirectoryStorage,"Media Storage Directory Storage");
		
		descriptionsByUID.put(SOPClass.BasicTextSRStorage,"Basic Text SR Storage");
		descriptionsByUID.put(SOPClass.EnhancedSRStorage,"Enhanced SR Storage");
		descriptionsByUID.put(SOPClass.ComprehensiveSRStorage,"Comprehensive SR Storage");
		descriptionsByUID.put(SOPClass.Comprehensive3DSRStorage,"Comprehensive 3D SR Storage");
		descriptionsByUID.put(SOPClass.ExtensibleSRStorage,"Extensible SR Storage");
		descriptionsByUID.put(SOPClass.MammographyCADSRStorage,"Mammography CAD SR Storage");
		descriptionsByUID.put(SOPClass.ChestCADSRStorage,"Chest CAD SR Storage");
		descriptionsByUID.put(SOPClass.ProcedureLogStorage,"Procedure Log Storage");
		descriptionsByUID.put(SOPClass.XRayRadiationDoseSRStorage,"X-Ray Radiation Dose SR Storage");
		descriptionsByUID.put(SOPClass.RadiopharmaceuticalRadiationDoseSRStorage,"Radiopharmaceutical Radiation Dose SR Storage");
		descriptionsByUID.put(SOPClass.ColonCADSRStorage,"Colon CAD SR Storage");
		descriptionsByUID.put(SOPClass.ImplantationPlanSRStorage,"Implantation Plan SR Storage");
		descriptionsByUID.put(SOPClass.AcquisitionContextSRStorage,"Acquisition Context SR Storage");
		descriptionsByUID.put(SOPClass.SimplifiedAdultEchoSRStorage,"Simplified Adult Echo SR Storage");
		descriptionsByUID.put(SOPClass.PatientRadiationDoseSRStorage,"Patient Radiation Dose SR Storage");
		descriptionsByUID.put(SOPClass.MacularGridThicknessAndVolumeReportStorage,"Macular Grid Thickness and Volume Report Storage");
		descriptionsByUID.put(SOPClass.KeyObjectSelectionDocumentStorage,"Key Object Selection Document Storage");

		descriptionsByUID.put(SOPClass.TextSRStorageTrialRetired,"Text SR Storage - Trial (Retired)");
		descriptionsByUID.put(SOPClass.AudioSRStorageTrialRetired,"Audio SR Storage - Trial (Retired)");
		descriptionsByUID.put(SOPClass.DetailSRStorageTrialRetired,"Detail SR Storage - Trial (Retired)");
		descriptionsByUID.put(SOPClass.ComprehensiveSRStorageTrialRetired,"Comprehensive SR Storage - Trial (Retired)");
		
		descriptionsByUID.put(SOPClass.GrayscaleSoftcopyPresentationStateStorage,"Grayscale Softcopy Presentation State Storage");
		descriptionsByUID.put(SOPClass.ColorSoftcopyPresentationStateStorage,"Color Softcopy Presentation State Storage");
		descriptionsByUID.put(SOPClass.PseudoColorSoftcopyPresentationStateStorage,"Pseudo-Color Softcopy Presentation State Storage");
		descriptionsByUID.put(SOPClass.BlendingSoftcopyPresentationStateStorage,"Blending Softcopy Presentation State Storage");
		descriptionsByUID.put(SOPClass.XAXRFGrayscaleSoftcopyPresentationStateStorage,"XA/XRF Grayscale Softcopy Presentation State Storage");
		descriptionsByUID.put(SOPClass.GrayscalePlanarMPRVolumetricPresentationStateStorage,"Grayscale Planar MPR Volumetric Presentation State Storage");
		descriptionsByUID.put(SOPClass.CompositingPlanarMPRVolumetricPresentationStateStorage,"Compositing Planar MPR Volumetric Presentation State Storage");
		descriptionsByUID.put(SOPClass.AdvancedBlendingPresentationStateStorage,"Advanced Blending Presentation State Storage");
		descriptionsByUID.put(SOPClass.VolumeRenderingVolumetricPresentationStateStorage,"Volume Rendering Volumetric Presentation State Storage");
		descriptionsByUID.put(SOPClass.SegmentedVolumeRenderingVolumetricPresentationStateStorage,"Segmented Volume Rendering Volumetric Presentation State Storage");
		descriptionsByUID.put(SOPClass.MultipleVolumeRenderingVolumetricPresentationStateStorage,"Multiple Volume Rendering Volumetric Presentation State Storage");
		
		descriptionsByUID.put(SOPClass.TwelveLeadECGStorage,"Twelve Lead ECG Storage");
		descriptionsByUID.put(SOPClass.GeneralECGStorage,"General ECG Storage");
		descriptionsByUID.put(SOPClass.AmbulatoryECGStorage,"Ambulatory ECG Storage");
		descriptionsByUID.put(SOPClass.HemodynamicWaveformStorage,"Hemodynamic Waveform Storage");
		descriptionsByUID.put(SOPClass.CardiacElectrophysiologyWaveformStorage,"Cardiac Electrophysiology Waveform Storage");
		descriptionsByUID.put(SOPClass.ArterialPulseWaveformStorage,"Arterial Pulse Waveform Storage");
		descriptionsByUID.put(SOPClass.RespiratoryWaveformStorage,"Respiratory Waveform Storage");
		descriptionsByUID.put(SOPClass.BasicVoiceStorage,"Basic Voice Storage");
		descriptionsByUID.put(SOPClass.GeneralAudioWaveformStorage,"General Audio Waveform Storage");

		descriptionsByUID.put(SOPClass.StandaloneOverlayStorage,"Standalone Overlay Storage");
		descriptionsByUID.put(SOPClass.StandaloneCurveStorage,"Standalone Curve Storage");
		descriptionsByUID.put(SOPClass.StandaloneModalityLUTStorage,"Standalone Modality LUT Storage");
		descriptionsByUID.put(SOPClass.StandaloneVOILUTStorage,"Standalone VOI LUT Storage");
		descriptionsByUID.put(SOPClass.StandalonePETCurveStorage,"Standalone PET Curve Storage");
		
		descriptionsByUID.put(SOPClass.RTDoseStorage,"RT Dose Storage");
		descriptionsByUID.put(SOPClass.RTStructureSetStorage,"RT Structure Set Storage");
		descriptionsByUID.put(SOPClass.RTBeamsTreatmentRecordStorage,"RT Beams Treatment Record Storage");
		descriptionsByUID.put(SOPClass.RTIonBeamsTreatmentRecordStorage,"RT Ion Beams Treatment Record Storage");
		descriptionsByUID.put(SOPClass.RTPlanStorage,"RT Plan Storage");
		descriptionsByUID.put(SOPClass.RTIonPlanStorage,"RT Ion Plan Storage");
		descriptionsByUID.put(SOPClass.RTBrachyTreatmentRecordStorage,"RT Brachy Treatment Record Storage");
		descriptionsByUID.put(SOPClass.RTTreatmentSummaryRecordStorage,"RT Treatment Summary Record Storage");
		descriptionsByUID.put(SOPClass.RTBeamsDeliveryInstructionStorageTrial,"RT Beams Delivery Instruction Storage - Trial");
		descriptionsByUID.put(SOPClass.RTBeamsDeliveryInstructionStorage,"RT Beams Delivery Instruction Storage");
	
		descriptionsByUID.put(SOPClass.MRSpectroscopyStorage,"MR Spectroscopy Storage");
		
		descriptionsByUID.put(SOPClass.RawDataStorage,"Raw Data Storage");

		descriptionsByUID.put(SOPClass.SpatialRegistrationStorage,"Spatial Registration Storage");
		descriptionsByUID.put(SOPClass.SpatialFiducialsStorage,"Spatial Fiducials Storage");
		descriptionsByUID.put(SOPClass.DeformableSpatialRegistrationStorage,"Deformable Spatial Registration Storage");

		descriptionsByUID.put(SOPClass.StereometricRelationshipStorage,"Stereometric Relationship Storage");
		descriptionsByUID.put(SOPClass.RealWorldValueMappingStorage,"Real World Value Mapping Storage");

		descriptionsByUID.put(SOPClass.EncapsulatedPDFStorage,"Encapsulated PDF Storage");
		descriptionsByUID.put(SOPClass.EncapsulatedCDAStorage,"Encapsulated CDA Storage");

		descriptionsByUID.put(SOPClass.StudyRootQueryRetrieveInformationModelFind,"Study Root Query Retrieve Information Model Find");
		descriptionsByUID.put(SOPClass.StudyRootQueryRetrieveInformationModelMove,"Study Root Query Retrieve Information Model Move");
		
		descriptionsByUID.put(SOPClass.SegmentationStorage,"Segmentation Storage");
		descriptionsByUID.put(SOPClass.SurfaceSegmentationStorage,"Surface Segmentation Storage");
		descriptionsByUID.put(SOPClass.TractographyResultsStorage,"Tractography Results Storage");

		descriptionsByUID.put(SOPClass.SurfaceScanMeshStorage,"Surface Scan Mesh Storage");
		descriptionsByUID.put(SOPClass.SurfaceScanPointCloudStorage,"Surface Scan Point Cloud Storage");

		descriptionsByUID.put(SOPClass.LensometryMeasurementsStorage,"Lensometry Measurements Storage");
		descriptionsByUID.put(SOPClass.AutorefractionMeasurementsStorage,"Autorefraction Measurements Storage");
		descriptionsByUID.put(SOPClass.KeratometryMeasurementsStorage,"Keratometry Measurements Storage");
		descriptionsByUID.put(SOPClass.SubjectiveRefractionMeasurementsStorage,"Subjective Refraction Measurements Storage");
		descriptionsByUID.put(SOPClass.VisualAcuityMeasurementsStorage,"Visual Acuity Measurements Storage");
		descriptionsByUID.put(SOPClass.SpectaclePrescriptionReportStorage,"Spectacle Prescription Report Storage");
		descriptionsByUID.put(SOPClass.OphthalmicAxialMeasurementsStorage,"Ophthalmic Axial Measurements Storage");
		descriptionsByUID.put(SOPClass.IntraocularLensCalculationsStorage,"Intraocular Lens Calculations Storage");
		descriptionsByUID.put(SOPClass.OphthalmicVisualFieldStaticPerimetryMeasurementsStorage,"Ophthalmic Visual Field Static Perimetry Measurements Storage");
		descriptionsByUID.put(SOPClass.OphthalmicThicknessMapStorage,"Ophthalmic Thickness Map Storage");
		descriptionsByUID.put(SOPClass.CornealTopographyMapStorage,"Corneal Topography Map Storage");

		descriptionsByUID.put(SOPClass.ColorPaletteStorage,"Color Palette Storage");

		descriptionsByUID.put(SOPClass.GenericImplantTemplateStorage,"Generic Implant Template Storage");
		descriptionsByUID.put(SOPClass.ImplantAssemblyTemplateStorage,"Implant Assembly Template Storage");
		descriptionsByUID.put(SOPClass.ImplantTemplateGroupStorage,"Implant Template Group Storage");

		descriptionsByUID.put(SOPClass.BasicStructuredDisplayStorage,"Basic Structured Display Storage");

		descriptionsByUID.put(SOPClass.PrivateGEDicomMRImageInfoObject,"GE Private Dicom MR Image Info Object Storage");
		descriptionsByUID.put(SOPClass.PrivateGEDicomCTImageInfoObject,"GE Private Dicom CT Image Info Object Storage");
		descriptionsByUID.put(SOPClass.PrivateGEDicomDisplayImageInfoObject,"GE Private Dicom Display Image Info Object Storage");
		descriptionsByUID.put(SOPClass.PrivateGEPETRawDataStorage,"GE Private PET Raw Data Storage");
		descriptionsByUID.put(SOPClass.PrivateGE3DModelStorage,"GE Private 3D Model Storage");
		descriptionsByUID.put(SOPClass.PrivateGEeNTEGRAProtocolOrNMGenieStorage,"GE Private eNTEGRA Protocol or NM Genie Storage");
		descriptionsByUID.put(SOPClass.PrivateGECollageStorage,"GE Private Collage Storage");
		descriptionsByUID.put(SOPClass.PrivateGERTPlanStorage,"GE Private RT Plan Storage");

		descriptionsByUID.put(SOPClass.PrivateSiemensCSANonImageStorage,"Siemens Private CSA Non-Image Storage");
		descriptionsByUID.put(SOPClass.PrivateSiemensCTMRVolumeStorage,"Siemens CT MR Volume Storage");
		descriptionsByUID.put(SOPClass.PrivateSiemensAXFrameSetsStorage,"Siemens Private AX Frame Sets Storage");
		
		descriptionsByUID.put(SOPClass.PrivateAgfaBasicAttributePresentationStateStorage,"Agfa Private Basic Attribute Presentation State Storage");
		
		descriptionsByUID.put(SOPClass.PrivateMedicalInsight3DSoftcopyPresentationStateStorage,"Medical Insight Private 3D Softcopy Presentation State Storage");
		
		descriptionsByUID.put(SOPClass.PrivateAcusonStructuredReportDetailStorage,"Acuson Private SR Detail Storage");
		
		descriptionsByUID.put(SOPClass.PrivateTomTecAnnotationStorage,"TomTec Private Annotation Storage");

		descriptionsByUID.put(SOPClass.PrivateFujiCRImageStorage,"Fuji Private CR Image Storage");

		descriptionsByUID.put(SOPClass.PrivatePhilipsCXImageStorage,"Philips Private CX Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsVolumeStorage,"Philips Private Volume Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsVolume2Storage,"Philips Private Volume 2 Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilips3DObjectStorage,"Philips Private 3D Object Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilips3DObject2Storage,"Philips Private 3D Object 2 Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsSurfaceStorage,"Philips Private Surface Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsSurface2Storage,"Philips Private Surface 2 Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsCompositeObjectStorage,"Philips Private Composite Object Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRCardioProfileStorage,"Philips Private MR Cardio Profile Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRCardioStorage,"Philips Private MR Cardio Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRCardio2Storage,"Philips Private MR Cardio 2 Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsCTSyntheticImageStorage,"Philips Private CT Synthetic Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRSyntheticImageStorage,"Philips Private MR Synthetic Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRCardioAnalysisStorage,"Philips Private MR Cardio Analysis Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRCardioAnalysis2Storage,"Philips Private MR Cardio Analysis Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsCXSyntheticImageStorage,"Philips Private CX Synthetic Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRSpectrumStorage,"Philips Private MR Spectrum Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRSeriesDataStorage,"Philips Private MR Series Data Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRColorImageStorage,"Philips Private MR Color Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsMRExamcardStorage,"Philips Private MR Examcard Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsSpecialisedXAStorage,"Philips Private Specialised XA Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilips3DPresentationStateStorage,"Philips Private 3D Presentation State Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsPerfusionStorage,"Philips Private Perfusion Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsPerfusionImageStorage,"Philips Private Perfusion Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsVRMLStorage,"Philips Private VRML Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsVolumeSetStorage,"Philips Private Volume Set Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsLiveRunStorage,"Philips Private Live Run Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsRunStorage,"Philips Private Run Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsReconstructionStorage,"Philips Private Reconstruction Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsXRayMFStorage,"Philips Private X-Ray Multiframe Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsHPLive3D01Storage,"Philips HP Private Live3D 01 Storage");
		descriptionsByUID.put(SOPClass.PrivatePhilipsHPLive3D02Storage,"Philips HP Private Live3D 02 Storage");
		
		descriptionsByUID.put(SOPClass.PrivatePMODMultiframeImageStorage,"PMOD Private Multiframe Image Storage");
		
		descriptionsByUID.put(SOPClass.PrivateToshibaUSImageStorage,"Toshiba Private US Image Storage");
		
		descriptionsByUID.put(SOPClass.PrivateERADPracticeBuilderReportTextStorage,"Private eRAD PracticeBuilder Report Text Storage");
		descriptionsByUID.put(SOPClass.PrivateERADPracticeBuilderReportDictationStorage,"Private eRAD PracticeBuilder Report Dictation Storage");
		
		descriptionsByUID.put(SOPClass.PrivateDcm4cheUpgradedCTImageStorage,"Private Dcm4che Upgraded CT Image Storage");
		descriptionsByUID.put(SOPClass.PrivateDcm4cheUpgradedMRImageStorage,"Private Dcm4che Upgraded MR Image Storage");
		descriptionsByUID.put(SOPClass.PrivateDcm4cheUpgradedPETImageStorage,"Private Dcm4che Upgraded PET Image Storage");
		descriptionsByUID.put(SOPClass.PrivateDcm4cheEncapsulatedDocumentStorage,"Private Dcm4che Encapsulated Document Storage");
		
		descriptionsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedCTImageStorage,"Private PixelMed Legacy Converted Enhanced CT Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedMRImageStorage,"Private PixelMed Legacy Converted Enhanced MR Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePixelMedLegacyConvertedEnhancedPETImageStorage,"Private PixelMed Legacy Converted Enhanced PET Image Storage");
		descriptionsByUID.put(SOPClass.PrivatePixelMedFloatingPointImageStorage,"Private PixelMed Floating Point Image Storage");
		
		descriptionsByUID.put(SOPClass.DICOSCTImageStorage,"DICOS CT Image Storage");
		descriptionsByUID.put(SOPClass.DICOSDigitalXRayImageStorageForPresentation,"DICOS Digital X-Ray Image Storage - For Presentation");
		descriptionsByUID.put(SOPClass.DICOSDigitalXRayImageStorageForProcessing,"DICOS Digital X-Ray Image Storage - For Processing");
		descriptionsByUID.put(SOPClass.DICOSThreatDetectionReportStorage,"DICOS Threat Detection Report Storage");
		descriptionsByUID.put(SOPClass.DICOS2DAITStorage,"DICOS 2D AIT Storage");
		descriptionsByUID.put(SOPClass.DICOS3DAITStorage,"DICOS 3D AIT Storage");
		descriptionsByUID.put(SOPClass.DICOSQuadrupoleResonanceStorage,"DICOS Quadrupole Resonance Storage");
		
		descriptionsByUID.put(SOPClass.DICONDEEddyCurrentImageStorage,"DICONDE Eddy Current Image Storage");
		descriptionsByUID.put(SOPClass.DICONDEEddyCurrentMultiframeImageStorage,"DICONDE Eddy Current Multi-frame Image Storage");
	}

	/**
	 * <p>Unit test.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {

		try {
			System.err.println(getDescriptionFromUID(SOPClass.MRSpectroscopyStorage));
			System.err.println(getAbbreviationFromUID(SOPClass.MRSpectroscopyStorage));
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}

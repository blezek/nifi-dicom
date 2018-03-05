/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.anatproc.CodedConcept;
import com.pixelmed.anatproc.CTAnatomy;
import com.pixelmed.anatproc.DisplayableAnatomicConcept;
import com.pixelmed.anatproc.ProjectionXRayAnatomy;

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.vecmath.Vector3d;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to create a set of instances, which when given unenhanced ("classic") images creates
 * one or more enhanced multiframe image instances from them where possible, otherwise leaves them
 * alone but includes them in the set.</p>
 *
 * <p>Each enhanced image corresponds to one {@link com.pixelmed.dicom.FrameSet FrameSet}.</p>
 *
 * @author	dclunie
 */
public class MultiFrameImageFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/MultiFrameImageFactory.java,v 1.39 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MultiFrameImageFactory.class);

	static void addIfPresentAndNotPerFrame(AttributeList targetList,AttributeList sourceList,AttributeTag tag,Set<AttributeTag> perFrameAttributeTags) {
		addIfPresentAndNotPerFrame(targetList,sourceList,null/*done*/,tag,perFrameAttributeTags);
	}
	
	static void addIfPresentAndNotPerFrame(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag tag,Set<AttributeTag> perFrameAttributeTags) {
		if (perFrameAttributeTags == null || !perFrameAttributeTags.contains(tag)) {
			Attribute a = sourceList.get(tag);
			if (a != null) {
				targetList.put(a);
				if (done != null) {
					done.add(tag);
				}
			}
		}
	}

	static void addIfPresent(AttributeList targetList,AttributeList sourceList,AttributeTag tag) {
		addIfPresentAndNotPerFrame(targetList,sourceList,null/*done*/,tag,null/*perFrameAttributeTags*/);
	}
	
	static void addIfPresent(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag tag) {
		addIfPresentAndNotPerFrame(targetList,sourceList,done,tag,null/*perFrameAttributeTags*/);
	}
	
	static void addIfPresentWithValuesAndNotPerFrame(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag tag,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,tag,tag,perFrameAttributeTags);
	}
	
	// The copying here is a lot of effort (because there is no clone values or set values methods in various Attribute sub-classes ... should improve this :(

	static void addIfPresentWithValuesAndNotPerFrame(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag sourceTag,AttributeTag targetTag,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		if (perFrameAttributeTags == null || !perFrameAttributeTags.contains(sourceTag)) {
			Attribute sourceAttribute = sourceList.get(sourceTag);
			if (sourceAttribute != null && sourceAttribute.getVM() > 0) {
				if (sourceAttribute instanceof StringAttribute) {
					String[] values = sourceAttribute.getOriginalStringValues();
					if (values != null && values.length > 0) {
						Attribute targetAttribute = AttributeFactory.newAttribute(targetTag);
						targetList.put(targetAttribute);
						for (String value : values) {
							targetAttribute.addValue(value);
						}
					}
					done.add(sourceTag);
					done.add(targetTag);
				}
				else if (sourceAttribute instanceof TextAttribute) {	// there should only be one value for TextAttribute
					String[] values = sourceAttribute.getStringValues();
					if (values != null && values.length > 0) {
						if (values.length > 1) {
							slf4jlogger.info("MultiFrameImageFactory.addIfPresentWithValuesAndNotPerFrame(): more than one value for TextAttribute for {} of class {}",sourceTag,sourceAttribute.getClass());
						}
						Attribute targetAttribute = AttributeFactory.newAttribute(targetTag);
						targetList.put(targetAttribute);
						for (String value : values) {
							targetAttribute.addValue(value);
						}
					}
					done.add(sourceTag);
					done.add(targetTag);
				}
				else if (sourceAttribute instanceof UnsignedShortAttribute || sourceAttribute instanceof SignedShortAttribute) {
					short[] values = sourceAttribute.getShortValues();		// use the native type that does not create cached copies
					if (values != null && values.length > 0) {
						Attribute targetAttribute = AttributeFactory.newAttribute(targetTag);
						targetList.put(targetAttribute);
						for (short value : values) {
							targetAttribute.addValue(value);
						}
					}
					done.add(sourceTag);
					done.add(targetTag);
				}
				else if (sourceAttribute instanceof UnsignedLongAttribute || sourceAttribute instanceof SignedLongAttribute) {
					int[] values = sourceAttribute.getIntegerValues();		// use the native type that does not create cached copies
					if (values != null && values.length > 0) {
						Attribute targetAttribute = AttributeFactory.newAttribute(targetTag);
						targetList.put(targetAttribute);
						for (int value : values) {
							targetAttribute.addValue(value);
						}
					}
					done.add(sourceTag);
					done.add(targetTag);
				}
				else {
					slf4jlogger.info("MultiFrameImageFactory.addIfPresentWithValuesAndNotPerFrame(): unsupported copy of {} of class {}",sourceTag,sourceAttribute.getClass());
				}
			}
		}
	}
	
	static void addIfPresentWithValues(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag tag) throws DicomException {
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,tag,null/*perFrameAttributeTags*/);
	}

	static void addIfPresentWithValues(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,AttributeTag sourceTag,AttributeTag targetTag) throws DicomException {
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,sourceTag,targetTag,null/*perFrameAttributeTags*/);
	}
	
	static void addCommonCTMRImageDescriptionMacro(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,boolean frameLevel,String volumeBasedCalculationTechnique) throws DicomException {
		{
			Attribute a = sourceList.get(TagFromName.ImageType);
			if (a != null) {
				String[] values = a.getStringValues();
				if (values != null && values.length > 0) {
					Attribute fa = frameLevel ? new CodeStringAttribute(TagFromName.FrameType) : new CodeStringAttribute(TagFromName.ImageType);
					fa.addValue(values[0]);			// should be ORIGINAL or DERIVED
					if (values.length > 1) {
						//fa.addValue(values[1]);		// should be PRIMARY or SECONDARY
						fa.addValue("PRIMARY");			// this is the only value allowed in Enhanced objects
						if (values.length > 2) {
							if (values[2].length() > 0) {
								fa.addValue(values[2]);	// should do better here and figure out flavor and whether or not 4th value for derivation is needed :(
							}
							else {
								fa.addValue("VOLUME");	// works for breast tomo, and is generally applicable, but could do better :(
							}
							fa.addValue("NONE");	// should consider whether to handle MIXED at image rather than frame level - do not need to if ImageType is a distinguishing attribute
						}
					}
					targetList.put(fa);
				}
				done.add(TagFromName.ImageType);
			}
			// really shouldn't be null
		}

		{ Attribute a = new CodeStringAttribute(TagFromName.PixelPresentation);               a.addValue("MONOCHROME");                    targetList.put(a); }		// should we bother checking PhotometricInterpretation ?
		{ Attribute a = new CodeStringAttribute(TagFromName.VolumetricProperties);            a.addValue("VOLUME");                        targetList.put(a); }		// is there any way to check that we are something funky like a MIP ?
		{ Attribute a = new CodeStringAttribute(TagFromName.VolumeBasedCalculationTechnique); a.addValue(volumeBasedCalculationTechnique); targetList.put(a); }	
	}
	
	static void addCommonCTMRImageDescriptionMacro(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,boolean frameLevel) throws DicomException {
		addCommonCTMRImageDescriptionMacro(targetList,sourceList,done,frameLevel,"NONE"/*volumeBasedCalculationTechnique*/);
	}

	static void addEnhancedCTImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
	}
	
	static void addEnhancedMRImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		// MR Image and Spectroscopy Instance Macro
		// Acquisition Number   ... see addEnhancedCommonImageModule()
		// Acquisition DateTime ... see addEnhancedCommonImageModule()
		// Acquisition Duration ... see addEnhancedCommonImageModule()
		// Referenced Raw Data Sequence etc.  ... see addEnhancedCommonImageModule()
		// ContentQualification ... see addEnhancedCommonImageModule()
		
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ResonantNucleus,perFrameAttributeTags);
		if (targetList.get(TagFromName.ResonantNucleus) == null) {
			// derive from ImagedNucleus, which is the one used in legacy MR IOD, but does not have a standard list of defined terms ... (could check these :()
			addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ImagedNucleus,TagFromName.ResonantNucleus,perFrameAttributeTags);
		}
		
		
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.KSpaceFiltering,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.MagneticFieldStrength,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ApplicableSafetyStandardAgency,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ApplicableSafetyStandardDescription,perFrameAttributeTags);
		
		// ImageComments ... see addEnhancedCommonImageModule()
	
		// MR Image Description Macro ... keep consistent with addMRImageFrameTypeFunctionalGroup() where the same values are used (should refactor :()
		//{ Attribute a = new CodeStringAttribute(TagFromName.ComplexImageComponent); a.addValue("MAGNITUDE"); targetList.put(a); }	// it almost always is, and have no way of knowing otherwise :(
		//{ Attribute a = new CodeStringAttribute(TagFromName.AcquisitionContrast); a.addValue("UNKNOWN"); targetList.put(a); }		// this is actually a legal defined term, surprisingly :(
	}
	
	static void addEnhancedPETImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
	}
	
	static void addEnhancedCommonImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags,String volumeBasedCalculationTechnique) throws DicomException {
		if (!perFrameAttributeTags.contains(TagFromName.ImageType)) {
			addCommonCTMRImageDescriptionMacro(targetList,sourceList,done,false/*frameLevel*/,volumeBasedCalculationTechnique);
		}
		
		// Acquisition Number
		// Acquisition DateTime						- should be able to find earliest amongst all frames, if present (required if ORIGINAL)
		// Acquisition Duration						- should be able to work this out, but type 2C, so can send empty
		
		// Referenced Raw Data Sequence				- optional - ignore - too hard to merge
		// Referenced Waveform Sequence				- optional - ignore - too hard to merge
		// Referenced Image Evidence Sequence		- should add if we have references :(
		// Source Image Evidence Sequence			- should add if we have sources :(
		// Referenced Presentation State Sequence	- should merge if present in any source frame :(
		
		// Samples per Pixel						- handled by distinguishingAttribute copy
		// Photometric Interpretation				- handled by distinguishingAttribute copy
		// Bits Allocated							- handled by distinguishingAttribute copy
		// Bits Stored								- handled by distinguishingAttribute copy
		// High Bit									- handled by distinguishingAttribute copy
		
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContentQualification,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ImageComments,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.BurnedInAnnotation,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.RecognizableVisualFeatures,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.LossyImageCompression,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.LossyImageCompressionRatio,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.LossyImageCompressionMethod,perFrameAttributeTags);

		if (!perFrameAttributeTags.contains(TagFromName.PresentationLUTShape)) {
			// actually should really invert the pixel data if MONOCHROME1, since only MONOCHROME2 is permitted :(
			// also, do not need to check if PhotometricInterpretation is per-frame, since a distinguishing attribute
			String photometricInterpretation = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.PhotometricInterpretation,"MONOCHROME2");
			String value = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.PresentationLUTShape,photometricInterpretation.equals("MONOCHROME1") ? "INVERTED" : "IDENTITY");
			if (value.length() > 0) {
				{ Attribute a = new CodeStringAttribute(TagFromName.PresentationLUTShape); a.addValue(value); targetList.put(a); }
				done.add(TagFromName.PresentationLUTShape);
			}
		}

		// Icon Image Sequence							- always discard these
	}

	static void addEnhancedCommonImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		addEnhancedCommonImageModule(targetList,sourceList,done,perFrameAttributeTags,"NONE"/*volumeBasedCalculationTechnique*/);
	}
	
	static void addImagePixelModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		addIfPresent(targetList,sourceList,done,TagFromName.SamplesPerPixel);
		addIfPresent(targetList,sourceList,done,TagFromName.PhotometricInterpretation);
		addIfPresent(targetList,sourceList,done,TagFromName.Rows);
		addIfPresent(targetList,sourceList,done,TagFromName.Columns);
		addIfPresent(targetList,sourceList,done,TagFromName.BitsAllocated);
		addIfPresent(targetList,sourceList,done,TagFromName.BitsStored);
		addIfPresent(targetList,sourceList,done,TagFromName.HighBit);
		addIfPresent(targetList,sourceList,done,TagFromName.PixelRepresentation);
		addIfPresent(targetList,sourceList,done,TagFromName.PlanarConfiguration);
		addIfPresent(targetList,sourceList,done,TagFromName.PixelAspectRatio);
		addIfPresent(targetList,sourceList,done,TagFromName.SmallestImagePixelValue);
		addIfPresent(targetList,sourceList,done,TagFromName.LargestImagePixelValue);
		addIfPresent(targetList,sourceList,done,TagFromName.RedPaletteColorLookupTableDescriptor );
		addIfPresent(targetList,sourceList,done,TagFromName.GreenPaletteColorLookupTableDescriptor );
		addIfPresent(targetList,sourceList,done,TagFromName.BluePaletteColorLookupTableDescriptor );
		addIfPresent(targetList,sourceList,done,TagFromName.RedPaletteColorLookupTableData);
		addIfPresent(targetList,sourceList,done,TagFromName.GreenPaletteColorLookupTableData);
		addIfPresent(targetList,sourceList,done,TagFromName.BluePaletteColorLookupTableData);
		addIfPresent(targetList,sourceList,done,TagFromName.ICCProfile);
		addIfPresent(targetList,sourceList,done,TagFromName.PixelPaddingRangeLimit);
	}

	static void addGeneralModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		// InstanceNumber - leave this to go into per-frame unassigned, and create new value later
		// PatientOrientation
		// ContentDate
		// ContentTime
		// ImageType
		// AcquisitionNumber
		// AcquisitionDate
		// AcquisitionTime
		// AcquisitionDateTime
		// ReferencedImageSequence
		// DerivationDescription
		// DerivationCodeSequence
		// SourceImageSequence
		// ReferencedInstanceSequence
		// ImagesInAcquisition
		// ImageComments
		// QualityControlImage
		// BurnedInAnnotation
		// RecognizableVisualFeatures
		// LossyImageCompression
		// LossyImageCompressionRatio
		// LossyImageCompressionMethod
		// IconImageSequence
		// PresentationLUTShape
		// IrradiationEventUID
	}
	
	static void addContrastBolusModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusAgent,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusAgentSequence,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusRoute,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusAdministrationRouteSequence,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusVolume,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusStartTime,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusStopTime,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusTotalDose,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastFlowRate,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastFlowDuration,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusIngredient,perFrameAttributeTags);
		addIfPresentWithValuesAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusIngredientConcentration,perFrameAttributeTags);
		
		// could try to do clever stuff and add Enhanced module, or recognize ContrastBolusAgentSequence from ContrastBolusAgent, etc. :(
	}

	static void addSecondaryCaptureEquipmentModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		if (!perFrameAttributeTags.contains(TagFromName.ConversionType)) {
			String value = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.ConversionType,"WSD");
			if (value.length() > 0) {
				{ Attribute a = new CodeStringAttribute(TagFromName.ConversionType); a.addValue(value); targetList.put(a); }
				done.add(TagFromName.ConversionType);
			}
		}
		// Modality - taken care of, if present, by distinguishing attributes, otherwise is optional

		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.SecondaryCaptureDeviceID,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.SecondaryCaptureDeviceManufacturer,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.SecondaryCaptureDeviceManufacturerModelName,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.SecondaryCaptureDeviceSoftwareVersions,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.VideoImageFormatAcquired,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.DigitalImageFormatAcquired,perFrameAttributeTags);
	}
	
	static void addSecondaryCaptureImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.DateOfSecondaryCapture,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.TimeOfSecondaryCapture,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.NominalScannedPixelSpacing,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.DocumentClassCodeSequence,perFrameAttributeTags);
		
		// Include Basic Pixel Spacing Calibration Macro :(
	}
	
	static void addSecondaryCaptureMultiFrameImageModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.BurnedInAnnotation,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.RecognizableVisualFeatures,perFrameAttributeTags);
		if (!perFrameAttributeTags.contains(TagFromName.PresentationLUTShape)) {
			// actually should really invert the pixel data if MONOCHROME1, since only MONOCHROME2 is permitted :(
			// also, do not need to check if PhotometricInterpretation is per-frame, since a distinguishing attribute
			String photometricInterpretation = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.PhotometricInterpretation,"MONOCHROME2");
			if (photometricInterpretation.equals("MONOCHROME1") || photometricInterpretation.equals("MONOCHROME2")) {
				String value = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.PresentationLUTShape,photometricInterpretation.equals("MONOCHROME1") ? "INVERTED" : "IDENTITY");
				if (value.length() > 0) {
					{ Attribute a = new CodeStringAttribute(TagFromName.PresentationLUTShape); a.addValue(value); targetList.put(a); }
					done.add(TagFromName.PresentationLUTShape);
				}
			}
		}

		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.Illumination,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ReflectedAmbientLight,perFrameAttributeTags);

		addAppropriateRescaleRelatedAttributes(targetList,sourceList,done);		// by adding them to done list, if shared, will prevent adding the functional group

		// Frame Increment Pointer :(

		// Nominal Scanned Pixel Spacing	- already done in addSecondaryCaptureImageModule, if present
		
		// Include Basic Pixel Spacing Calibration Macro

		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.DigitizingDeviceTransportDirection,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.RotationOfScannedFilm,perFrameAttributeTags);
	}

	static void addAcquisitionContextModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		if (!perFrameAttributeTags.contains(TagFromName.AcquisitionContextSequence)) {
			Attribute a = sourceList.get(TagFromName.AcquisitionContextSequence);
			if (a == null) {
				a = new SequenceAttribute(TagFromName.AcquisitionContextSequence);
			}
			targetList.put(a);
			done.add(TagFromName.AcquisitionContextSequence);
		}
	}
	
	static void addXRay3DGeneralPositionerMovementMacro(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		double tomoAngle = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.TomoAngle,0);
		if (tomoAngle > 0) {
			{ Attribute a = new FloatSingleAttribute(TagFromName.PrimaryPositionerScanArc); a.addValue(tomoAngle); targetList.put(a); }
		}
		// PrimaryPositionerScanStartAngle
		// PrimaryPositionerIncrement
		// forget about secondary positioner
	}
	
	static void addXRay3DGeneralSharedAcquisitionMacro(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		// SourceImageSequence
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.FieldOfViewDimensionsInFloat,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.FieldOfViewOrigin,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.FieldOfViewRotation,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.FieldOfViewHorizontalFlip,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.Grid,perFrameAttributeTags);
		// Grid Description macro
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.KVP,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.XRayTubeCurrentInmA,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ExposureTimeInms,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ExposureInmAs,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusAgent,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(targetList,sourceList,done,TagFromName.ContrastBolusAgentSequence,perFrameAttributeTags);
		// StartAcquisitionDateTime
		// EndAcquisitionDateTime
	}
	
	static void addBreastTomosynthesisAcquisitionModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		SequenceAttribute aXRay3DAcquisitionSequence = new SequenceAttribute(TagFromName.XRay3DAcquisitionSequence);
		targetList.put(aXRay3DAcquisitionSequence);
		AttributeList itemList = new AttributeList();
		aXRay3DAcquisitionSequence.addItem(itemList);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FieldOfViewShape,perFrameAttributeTags);
		addXRay3DGeneralSharedAcquisitionMacro(itemList,sourceList,done,perFrameAttributeTags);
		addXRay3DGeneralPositionerMovementMacro(itemList,sourceList,done,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.DistanceSourceToDetector,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.DistanceSourceToPatient,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.EstimatedRadiographicMagnificationFactor,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.AnodeTargetMaterial,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.BodyPartThickness,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.ExposureControlMode,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.ExposureControlModeDescription,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.HalfValueLayer,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FocalSpot,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.DetectorBinning,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.DetectorTemperature,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterType,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterMaterial,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterThicknessMinimum,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterThicknessMaximum,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterBeamPathLengthMinimum,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.FilterBeamPathLengthMaximum,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.CompressionForce,perFrameAttributeTags);
		addIfPresentAndNotPerFrame(itemList,sourceList,done,TagFromName.PaddleDescription,perFrameAttributeTags);
		// PerProjectionAcquisitionSequence
	}
	
	static void addBreastViewModule(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		if (!perFrameAttributeTags.contains(TagFromName.ViewCodeSequence)) {
			Attribute a = sourceList.get(TagFromName.ViewCodeSequence);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.ViewCodeSequence);
			}
			// else could try to guess from ViewPosition, but rarely necessary :(
		}
		if (!perFrameAttributeTags.contains(TagFromName.BreastImplantPresent)) {
			Attribute a = sourceList.get(TagFromName.BreastImplantPresent);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.BreastImplantPresent);
			}
		}
		if (!perFrameAttributeTags.contains(TagFromName.PartialView)) {
			Attribute a = sourceList.get(TagFromName.PartialView);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.PartialView);
			}
		}
		if (!perFrameAttributeTags.contains(TagFromName.PartialViewCodeSequence)) {
			Attribute a = sourceList.get(TagFromName.PartialViewCodeSequence);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.PartialViewCodeSequence);
			}
		}
	}
	
	static boolean containsAttributesForCTImageFrameTypeFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImageType);
	}
	
	static void addCTImageFrameTypeFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aCTImageFrameTypeSequence = new SequenceAttribute(TagFromName.CTImageFrameTypeSequence);
		targetList.put(aCTImageFrameTypeSequence);
		AttributeList itemList = new AttributeList();
		aCTImageFrameTypeSequence.addItem(itemList);
		addCommonCTMRImageDescriptionMacro(itemList,sourceList,done,true/*frameLevel*/);
	}
	
	static boolean containsAttributesForMRImageFrameTypeFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImageType);
	}
	
	static void addMRImageFrameTypeFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aMRImageFrameTypeSequence = new SequenceAttribute(TagFromName.MRImageFrameTypeSequence);
		targetList.put(aMRImageFrameTypeSequence);
		AttributeList itemList = new AttributeList();
		aMRImageFrameTypeSequence.addItem(itemList);
		addCommonCTMRImageDescriptionMacro(itemList,sourceList,done,true/*frameLevel*/);
		//{ Attribute a = new CodeStringAttribute(TagFromName.ComplexImageComponent); a.addValue("MAGNITUDE"); itemList.put(a); }		// it almost always is, and have no way of knowing otherwise :(
		//{ Attribute a = new CodeStringAttribute(TagFromName.AcquisitionContrast); a.addValue("UNKNOWN"); itemList.put(a); }		// this is actually a legal defined term, surprisingly :(
	}
	
	static boolean containsAttributesForPETImageFrameTypeFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImageType);
	}
	
	static void addPETImageFrameTypeFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aPETFrameTypeSequence = new SequenceAttribute(TagFromName.PETFrameTypeSequence);
		targetList.put(aPETFrameTypeSequence);
		AttributeList itemList = new AttributeList();
		aPETFrameTypeSequence.addItem(itemList);
		addCommonCTMRImageDescriptionMacro(itemList,sourceList,done,true/*frameLevel*/);
	}
	
	//static boolean containsAttributesForXRay3DImageFrameTypeFunctionalGroup(Set<AttributeTag> attributeTags) {
	//	return attributeTags.contains(TagFromName.ImageType);
	//}
	
	static void addXRay3DImageFrameTypeFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aXRay3DFrameTypeSequence = new SequenceAttribute(TagFromName.XRay3DFrameTypeSequence);
		targetList.put(aXRay3DFrameTypeSequence);
		AttributeList itemList = new AttributeList();
		aXRay3DFrameTypeSequence.addItem(itemList);
		addCommonCTMRImageDescriptionMacro(itemList,sourceList,done,true/*frameLevel*/,"TOMOSYNTHESIS");
	}

	static boolean containsAttributesForPixelMeasuresFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.PixelSpacing) || attributeTags.contains(TagFromName.SliceThickness) || attributeTags.contains(TagFromName.ImagerPixelSpacing) /*for breast tomo MG conversion*/;
	}
	
	static void addPixelMeasuresFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
		targetList.put(aPixelMeasuresSequence);
		AttributeList itemList = new AttributeList();
		aPixelMeasuresSequence.addItem(itemList);
		addIfPresent(itemList,sourceList,done,TagFromName.PixelSpacing);
		addIfPresent(itemList,sourceList,done,TagFromName.SliceThickness);
		if (itemList.get(TagFromName.PixelSpacing) == null) {
			// for breast tomo MG conversion
			addIfPresentWithValues(itemList,sourceList,done,TagFromName.ImagerPixelSpacing,TagFromName.PixelSpacing);
		}
	}

	static boolean containsAttributesForFrameAnatomyFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.Laterality) || attributeTags.contains(TagFromName.ImageLaterality) || attributeTags.contains(TagFromName.BodyPartExamined) || attributeTags.contains(TagFromName.AnatomicRegionSequence);
	}
	
	static void addFrameAnatomyFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aFrameAnatomySequence = new SequenceAttribute(TagFromName.FrameAnatomySequence);
		targetList.put(aFrameAnatomySequence);
		AttributeList itemList = new AttributeList();
		aFrameAnatomySequence.addItem(itemList);

		addIfPresent(itemList,sourceList,done,TagFromName.AnatomicRegionSequence);
		if (itemList.get(TagFromName.AnatomicRegionSequence) == null) {
//System.err.println("MultiFrameImageFactory.addFrameAnatomyFunctionalGroup(): trying to derive value for AnatomicRegionSequence");
			CodedConcept found = CTAnatomy.findAnatomicConcept(sourceList);
			if (found == null) {
				found = ProjectionXRayAnatomy.findAnatomicConcept(sourceList);
			}
			if (found != null) {
//System.err.println("MultiFrameImageFactory.addFrameAnatomyFunctionalGroup(): got value for AnatomicRegionSequence = "+found);
				{ SequenceAttribute a = new SequenceAttribute(TagFromName.AnatomicRegionSequence); a.addItem(found.getCodedSequenceItem().getAttributeList()); itemList.put(a); }
			}
			// do not bother to try to figure out what source attribute was used to add to done list :(
		}
		
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.FrameLaterality);
		if (itemList.get(TagFromName.FrameLaterality) == null) {
			addIfPresentWithValues(itemList,sourceList,done,TagFromName.ImageLaterality,TagFromName.FrameLaterality);
		}
		if (itemList.get(TagFromName.FrameLaterality) == null) {
			addIfPresentWithValues(itemList,sourceList,done,TagFromName.Laterality,TagFromName.FrameLaterality);
		}
		if (itemList.get(TagFromName.FrameLaterality) == null) {
			CodedSequenceItem anatomicRegion = CodedSequenceItem.getSingleCodedSequenceItemOrNull(itemList,TagFromName.AnatomicRegionSequence);
			if (anatomicRegion != null) {
//System.err.println("MultiFrameImageFactory.addFrameAnatomyFunctionalGroup(): no laterality information, so checking if AnatomicRegionSequence is unpaired");
				CodedConcept found = CTAnatomy.getAnatomyConcepts().find(anatomicRegion);
				if (found == null) {
					found = ProjectionXRayAnatomy.getAnatomyConcepts().find(anatomicRegion);
				}
				if (found != null && found instanceof DisplayableAnatomicConcept && !((DisplayableAnatomicConcept)found).isPairedStructure()) {
//System.err.println("MultiFrameImageFactory.addFrameAnatomyFunctionalGroup(): is unpaired");
					{ Attribute a = new CodeStringAttribute(TagFromName.FrameLaterality); a.addValue("U"); itemList.put(a); }
				}
			}
		}
	}
	
	// always per-frame, so no need for containsAttributesForFrameContentFunctionalGroup()
	
	static Date addFrameContentFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,Date earliestFrameAcquisitionDateTimeSoFar,boolean tzSpecified,String timezoneString,Set<AttributeTag> perFrameAttributeTags) throws DicomException {
		SequenceAttribute aFrameContentSequence = new SequenceAttribute(TagFromName.FrameContentSequence);
		targetList.put(aFrameContentSequence);
		AttributeList itemList = new AttributeList();
		aFrameContentSequence.addItem(itemList);
		{
			int value = Attribute.getSingleIntegerValueOrDefault(sourceList,TagFromName.AcquisitionNumber,0);	// hmmmm ... is this a good default, or would 1 be better ? :(
			{
				{ Attribute a = new UnsignedShortAttribute(TagFromName.FrameAcquisitionNumber); a.addValue(value); itemList.put(a); }
				done.add(TagFromName.AcquisitionNumber);
			}
		}
		{
			// copied basis of this from FrameSet .... should refactor ... :(
			String useAcquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.AcquisitionDateTime);
			boolean acquisitionDateTimeInformationIsPerFrame = perFrameAttributeTags.contains(TagFromName.AcquisitionDateTime);
			if (useAcquisitionDateTime.length() == 0) {
				// Follow the pattern of com.pixelmed.dicom.DateTimeAttribute.getDateFromFormattedString(AttributeList,AttributeTag,AttributeTag)
				String dateValue = Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.AcquisitionDate);
				acquisitionDateTimeInformationIsPerFrame = acquisitionDateTimeInformationIsPerFrame || perFrameAttributeTags.contains(TagFromName.AcquisitionDate);
				if (dateValue.length() > 0) {
					useAcquisitionDateTime = dateValue
										   + Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.AcquisitionTime);		// assume hh is zero padded if less than 10, which should be true, but should check :(
										   // handle time zone later
					acquisitionDateTimeInformationIsPerFrame = acquisitionDateTimeInformationIsPerFrame || perFrameAttributeTags.contains(TagFromName.AcquisitionTime);
				}
				// else leave it empty
			}
			else {
				done.add(TagFromName.AcquisitionDateTime);
			}
			if (useAcquisitionDateTime.length() > 0) {
//System.err.println("addFrameContentFunctionalGroup(): tzSpecified="+tzSpecified);
//System.err.println("addFrameContentFunctionalGroup(): timezoneString="+timezoneString);
				if (tzSpecified && !(useAcquisitionDateTime.contains("+") || useAcquisitionDateTime.contains("-"))) {
					useAcquisitionDateTime = useAcquisitionDateTime + timezoneString;													// only add it if TZ was not included in the original DT attribute and was present in TimezoneOffsetFromUTC
				}
				
				try {
					// need to handle timezone explicitly for earliestFrameAcquisitionDateTimeSoFar check
					String useAcquisitionDateTimeForCalculations = null;
					if (!(useAcquisitionDateTime.contains("+") || useAcquisitionDateTime.contains("-"))) {
						useAcquisitionDateTimeForCalculations = useAcquisitionDateTime + "+0000";										// set to GMT used throughout program, if TZ was not included in the original DT attribute, or we did not already add it from TimezoneOffsetFromUTC
					}
					else {
						useAcquisitionDateTimeForCalculations = useAcquisitionDateTime;
					}
					Date testDate = DateTimeAttribute.getDateFromFormattedString(useAcquisitionDateTimeForCalculations);
					if (testDate.before(earliestFrameAcquisitionDateTimeSoFar)) {
						earliestFrameAcquisitionDateTimeSoFar = testDate;
					}
					if (acquisitionDateTimeInformationIsPerFrame) {
//System.err.println("addFrameContentFunctionalGroup(): acquisitionDateTimeInformationIsPerFrame, so use it as is: "+useAcquisitionDateTime);
						Attribute a = new DateTimeAttribute(TagFromName.FrameAcquisitionDateTime); a.addValue(useAcquisitionDateTime); itemList.put(a);
					}
					else {
//System.err.println("addFrameContentFunctionalGroup(): acquisitionDateTimeInformationIsPerFrame is false, was: "+useAcquisitionDateTime);
						// check for special case seen in GE DCE MR, in which AcquisitionTime is constant but TriggerTime (incorrectly, since not cardiac), signals acquisition time offset
						if (sourceList.containsKey(TagFromName.TriggerTime) && !sourceList.containsKey(TagFromName.FrameReferenceDateTime)) {	// ?? should check for "not cardiac" somehow ?? should make this a command ;ine option ?? :(
							int triggerTime = Attribute.getSingleIntegerValueOrDefault(sourceList,TagFromName.TriggerTime,0);	// in mS
							testDate.setTime(testDate.getTime() + triggerTime);
							useAcquisitionDateTime = DateTimeAttribute.getFormattedString(testDate,DateTimeAttribute.getTimeZone(timezoneString),tzSpecified);
//System.err.println("addFrameContentFunctionalGroup(): acquisitionDateTimeInformationIsPerFrame is false and TriggerTime is "+triggerTime+", so add to AcquisitionDateTime is: "+useAcquisitionDateTime);
							Attribute a = new DateTimeAttribute(TagFromName.FrameAcquisitionDateTime); a.addValue(useAcquisitionDateTime); itemList.put(a);
							done.add(TagFromName.TriggerTime);		// need to be sure and not add TriggerTime as well, since already "used" to make revised FrameAcquisitionDateTime
						}
						else {
//System.err.println("addFrameContentFunctionalGroup(): acquisitionDateTimeInformationIsPerFrame is false but no TriggerTime, so use it as is: "+useAcquisitionDateTime);
							Attribute a = new DateTimeAttribute(TagFromName.FrameAcquisitionDateTime); a.addValue(useAcquisitionDateTime); itemList.put(a);
						}
					}
				}
				catch (java.text.ParseException e) {
					slf4jlogger.error("Cannot derive AcquisitionDateTime",e);
				}
			}
		}
		// Frame Reference DateTime		1C		... could try to guess from FrameAcquisitionDateTime and FrameAcquisitionDuration, but latter often missing :(
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.AcquisitionDuration,TagFromName.FrameAcquisitionDuration);
		// Cardiac Cycle Position		3
		// Respiratory Cycle Position	3
		// Dimension Index Values		1C
		
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.TemporalPositionIndex);
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.StackID);
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.InStackPositionNumber);

		addIfPresentWithValues(itemList,sourceList,done,TagFromName.ImageComments,TagFromName.FrameComments);

		// Frame Label					3
		
		return earliestFrameAcquisitionDateTimeSoFar;
	}
	
	static Date getEarliestContentDateTime(AttributeList sourceList,Date earliestContentDateTimeSoFar) {
		try {
			Date testDate = DateTimeAttribute.getDateFromFormattedString(sourceList,TagFromName.ContentDate,TagFromName.ContentTime);
			if (testDate.before(earliestContentDateTimeSoFar)) {
				earliestContentDateTimeSoFar = testDate;
			}
		}
		catch (java.text.ParseException e) {
			slf4jlogger.error("Cannot derive from DateTimeAttribute from ContentDate and ContentTime",e);
		}
		catch (DicomException e) {
			// this is OK ... will happen if ContentDate is absent or empty
			slf4jlogger.debug("ContentDate is absent or empty",e);
		}
		return earliestContentDateTimeSoFar;
	}

	static boolean containsAttributesForPlanePositionFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImagePositionPatient);
	}
	
	static void addPlanePositionFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
		targetList.put(aPlanePositionSequence);
		AttributeList itemList = new AttributeList();
		aPlanePositionSequence.addItem(itemList);
		addIfPresent(itemList,sourceList,done,TagFromName.ImagePositionPatient);
	}
	
	static boolean containsAttributesForPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.TomoLayerHeight);
	}
	
	static void addPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done,double orientation[]) throws DicomException {
		double height = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.TomoLayerHeight,0);
//System.err.println("addPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(): have tomo height = "+height);
		// the tomo layer height is the distance along the x-ray beam (normal to the plane defined by orientation (in PS 3.3, "Distance in mm between the table surface and the sharp image plane")
		// assuming that the detector is not rotated relative to the central ray of the middle acquisition
		// arbitrarily define the orgin as the image TLHC of height 0
		
		// translate the origin (0,0,0) by depth along the normal to row and column
		
		Vector3d normal = new Vector3d();
		normal.cross(new Vector3d(orientation[0],orientation[1],orientation[2]),new Vector3d(orientation[3],orientation[4],orientation[5]));
		normal.normalize();		// not really necessary, but just in case
		double[] normalArray = new double[3];
		normal.get(normalArray);
		normalArray[2]=normalArray[2]*-1;	// change the direction of Z (DICOM is LPS+) ... see com.pixelmed.geometry.GeometryOfSlice
//System.err.println("addPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(): normal = "+normalArray[0]+","+normalArray[1]+","+normalArray[2]);
		normalArray[0] = normalArray[0] * height;
		normalArray[1] = normalArray[1] * height;
		normalArray[2] = normalArray[2] * height;
//System.err.println("addPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(): TLHC = "+normalArray[0]+","+normalArray[1]+","+normalArray[2]);

		SequenceAttribute aPlanePositionSequence = new SequenceAttribute(TagFromName.PlanePositionSequence);
		targetList.put(aPlanePositionSequence);
		AttributeList itemList = new AttributeList();
		aPlanePositionSequence.addItem(itemList);

		Attribute aImagePositionPatient = new DecimalStringAttribute(TagFromName.ImagePositionPatient);
		aImagePositionPatient.addValue(normalArray[0]);
		aImagePositionPatient.addValue(normalArray[1]);
		aImagePositionPatient.addValue(normalArray[2]);
		itemList.put(aImagePositionPatient);
	}
	
	static boolean containsAttributesForPlaneOrientationFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ImageOrientationPatient);
	}
	
	static void addPlaneOrientationFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
		targetList.put(aPlaneOrientationSequence);
		AttributeList itemList = new AttributeList();
		aPlaneOrientationSequence.addItem(itemList);
		addIfPresent(itemList,sourceList,done,TagFromName.ImageOrientationPatient);
	}

	static boolean containsAttributesForPlaneOrientationFunctionalGroupDerivedFromAngle(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.PositionerPrimaryAngle) && attributeTags.contains(TagFromName.PatientOrientation);
	}
	
	static boolean isUnitVector(double x,double y,double z) {
		return Math.abs(Math.sqrt(x*x+y*y+z*z)-1) < 0.0001;
	}
	
	static double[] addPlaneOrientationFunctionalGroupDerivedFromAngle(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		double[] orientation = null;
		double xRowComponent = 0;
		double yRowComponent = 0;
		double zRowComponent = 0;
		double xColumnComponent = 0;
		double yColumnComponent = 0;
		double zColumnComponent = 0;
		Attribute aPatientOrientation = sourceList.get(TagFromName.PatientOrientation);
		if (aPatientOrientation != null && aPatientOrientation.getVM() == 2) {
			String[] vPatientOrientation = aPatientOrientation.getStringValues();
			if (vPatientOrientation != null && vPatientOrientation.length == 2) {
				{
					String rowOrientation = vPatientOrientation[0];
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): rowOrientation = "+rowOrientation);
					if (rowOrientation.equals("A")) {
						yRowComponent = -1;
					}
					else if (rowOrientation.equals("P")) {
						yRowComponent = 1;
					}
				}
				{
					int xColumnComponentSign = 0;
					int zColumnComponentSign = 0;
					String columnOrientation = vPatientOrientation[1];
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): columnOrientation = "+columnOrientation);
					if (columnOrientation.contains("L")) {
						xColumnComponentSign = 1;
					}
					else if (columnOrientation.contains("R")) {
						xColumnComponentSign = -1;
					}
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): xColumnComponentSign = "+xColumnComponentSign);
					
					if (columnOrientation.contains("H")) {
						zColumnComponentSign = 1;
					}
					else if (columnOrientation.contains("F")) {
						zColumnComponentSign = -1;
					}
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): zColumnComponentSign = "+zColumnComponentSign);
					// the positioner angle is defined to be tube movement right +ve with vertical zero degrees, which gives us the sin and cos relationships,
					// but who knows how the modality then flipped the images, so we use the sign derived from the orientation
					double positionerPrimaryAngle = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.PositionerPrimaryAngle,0);
					xColumnComponent = Math.abs(Math.sin(positionerPrimaryAngle)) * xColumnComponentSign;
					zColumnComponent = Math.abs(Math.cos(positionerPrimaryAngle)) * zColumnComponentSign;
				}
			}
		}
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): have "+xRowComponent+","+yRowComponent+","+zRowComponent+","+xColumnComponent+","+yColumnComponent+","+zColumnComponent);
		if (isUnitVector(xRowComponent,yRowComponent,zRowComponent) && isUnitVector(xColumnComponent,yColumnComponent,zColumnComponent)) {
//System.err.println("MultiFrameImageFactory.addPlaneOrientationFunctionalGroupDerivedFromAngle(): is unit vector");
			SequenceAttribute aPlaneOrientationSequence = new SequenceAttribute(TagFromName.PlaneOrientationSequence);
			targetList.put(aPlaneOrientationSequence);
			AttributeList itemList = new AttributeList();
			aPlaneOrientationSequence.addItem(itemList);
			Attribute aImageOrientationPatient = new DecimalStringAttribute(TagFromName.ImageOrientationPatient);
			aImageOrientationPatient.addValue(xRowComponent);
			aImageOrientationPatient.addValue(yRowComponent);
			aImageOrientationPatient.addValue(zRowComponent);
			aImageOrientationPatient.addValue(xColumnComponent);
			aImageOrientationPatient.addValue(yColumnComponent);
			aImageOrientationPatient.addValue(zColumnComponent);
			itemList.put(aImageOrientationPatient);
			
			orientation = new double[6];
			orientation[0] = xRowComponent;
			orientation[1] = yRowComponent;
			orientation[2] = zRowComponent;
			orientation[3] = xColumnComponent;
			orientation[4] = yColumnComponent;
			orientation[5] = zColumnComponent;
		}
		return orientation;
	}

	static boolean containsAttributesForFrameVOILUTFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.WindowWidth) || attributeTags.contains(TagFromName.WindowCenter) || attributeTags.contains(TagFromName.WindowCenterWidthExplanation);
	}
	
	static void addFrameVOILUTFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) {
		SequenceAttribute aFrameVOILUTSequence = new SequenceAttribute(TagFromName.FrameVOILUTSequence);
		targetList.put(aFrameVOILUTSequence);
		AttributeList itemList = new AttributeList();
		aFrameVOILUTSequence.addItem(itemList);
		addIfPresent(itemList,sourceList,done,TagFromName.WindowWidth);
		addIfPresent(itemList,sourceList,done,TagFromName.WindowCenter);
		addIfPresent(itemList,sourceList,done,TagFromName.WindowCenterWidthExplanation);
	}
	
	static boolean containsAttributesForPixelValueTransformationFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.RescaleIntercept) || attributeTags.contains(TagFromName.RescaleSlope) || attributeTags.contains(TagFromName.RescaleType);
	}
	
	static void addAppropriateRescaleRelatedAttributes(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		boolean haveValuesSoAddType = false;
		{
			Attribute a = sourceList.get(TagFromName.RescaleIntercept);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.RescaleIntercept);
				haveValuesSoAddType = true;
			}
			// really shouldn't be null
		}
		{
			Attribute a = sourceList.get(TagFromName.RescaleSlope);
			if (a != null) {
				targetList.put(a);
				done.add(TagFromName.RescaleSlope);
				haveValuesSoAddType = true;
			}
			// really shouldn't be null
		}
		{
			String value = Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.RescaleType);
			if (value.length() == 0) {
				String modality = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.Modality,"");
				if (haveValuesSoAddType) {
					value = "US";
					if (modality.equals("CT")) {
						boolean isLocalizer = Attribute.getDelimitedStringValuesOrDefault(sourceList,TagFromName.ImageType,"").contains("LOCALIZER");
						if (!isLocalizer) {
							value = "HU";
						}
					}
					else if (modality.equals("PT")) {
						value = Attribute.getSingleStringValueOrDefault(sourceList,TagFromName.Units,"US");
					}
				}
			}
			if (value.length() > 0) {
				{ Attribute a = new LongStringAttribute(TagFromName.RescaleType); a.addValue(value); targetList.put(a); }
				done.add(TagFromName.RescaleType);
			}
		}
	}
	
	static void addPixelValueTransformationFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aPixelValueTransformationSequence = new SequenceAttribute(TagFromName.PixelValueTransformationSequence);
		targetList.put(aPixelValueTransformationSequence);
		AttributeList itemList = new AttributeList();
		aPixelValueTransformationSequence.addItem(itemList);
		addAppropriateRescaleRelatedAttributes(itemList,sourceList,done);
	}
	
	// do not need RWV for CT since have CT PixelValueTransformationSequence, but just comment it out for now in case needed later for MR or PET

	//static boolean containsAttributesForRealWorldValueMappingFunctionalGroup(Set<AttributeTag> attributeTags) {
	//	return attributeTags.contains(TagFromName.RescaleIntercept) || attributeTags.contains(TagFromName.RescaleSlope) || attributeTags.contains(TagFromName.RescaleType);
	//}
	
	//static void addRealWorldValueMappingFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
	//	SequenceAttribute aRealWorldValueMappingSequence = new SequenceAttribute(TagFromName.RealWorldValueMappingSequence);
	//	targetList.put(aRealWorldValueMappingSequence);
	//	AttributeList itemList = new AttributeList();
	//	aRealWorldValueMappingSequence.addItem(itemList);

	//	// really should try and add RealWorldValueFirstValueMapped and RealWorldValueLastValueMapped :(
	//	{
	//		double value = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.RescaleIntercept,0);
	//		{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueIntercept); a.addValue(value); itemList.put(a); }
	//		done.add(TagFromName.WindowWidth);
	//	}
	//	{
	//		double value = Attribute.getSingleDoubleValueOrDefault(sourceList,TagFromName.RescaleSlope,1);
	//		{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueSlope); a.addValue(value); itemList.put(a); }
	//		done.add(TagFromName.WindowWidth);
	//	}
	//	{
	//		String value = Attribute.getSingleStringValueOrEmptyString(sourceList,TagFromName.RescaleType);
	//		if (value.length() > 0) {
	//			{ Attribute a = new ShortStringAttribute(TagFromName.LUTLabel); a.addValue(value); itemList.put(a); }
	//			done.add(TagFromName.RescaleType);
	//		}
	//	}
	//	// really should try and add MeasurementUnitsCodeSequence for HU except for localizer :(
	//}

	static boolean containsAttributesForReferencedImageFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.ReferencedImageSequence);
	}

	static void addReferencedImageFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		Attribute aReferencedImageSequence = sourceList.get(TagFromName.ReferencedImageSequence);
		if (aReferencedImageSequence != null) {
			targetList.put(aReferencedImageSequence);
			done.add(TagFromName.ReferencedImageSequence);
		}
		// really should make up dummy PurposeOfReferenceCodeSequence if not present in sequence already, since Type 1 in this functional group :(
		// can we assume ("121311", DCM, "Localizer") ?
	}

	static boolean containsAttributesForDerivationImageFunctionalGroup(Set<AttributeTag> attributeTags) {
		return attributeTags.contains(TagFromName.SourceImageSequence);
	}

	static void addDerivationImageFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aDerivationImageSequence = new SequenceAttribute(TagFromName.DerivationImageSequence);
		targetList.put(aDerivationImageSequence);
		AttributeList itemList = new AttributeList();
		aDerivationImageSequence.addItem(itemList);

		addIfPresentWithValues(itemList,sourceList,done,TagFromName.DerivationDescription);
	
		Attribute aDerivationCodeSequence = sourceList.get(TagFromName.DerivationCodeSequence);
		if (aDerivationCodeSequence != null) {
			itemList.put(aDerivationCodeSequence);
			done.add(TagFromName.DerivationCodeSequence);
		}
		// else is Type 1 so really should provide a value (and in fact check if that in source was not empty) :(
		
		Attribute aSourceImageSequence = sourceList.get(TagFromName.SourceImageSequence);
		if (aSourceImageSequence != null) {
			itemList.put(aSourceImageSequence);
			done.add(TagFromName.SourceImageSequence);
		}
		// really should make up dummy PurposeOfReferenceCodeSequence if not present in sequence already, since Type 1 in this functional group :(
	}

	static void addConversionSourceFunctionalGroup(AttributeList targetList,AttributeList sourceList,Set<AttributeTag> done) throws DicomException {
		SequenceAttribute aConversionSourceAttributesSequence = new SequenceAttribute(TagFromName.ConversionSourceAttributesSequence);
		targetList.put(aConversionSourceAttributesSequence);
		AttributeList itemList = new AttributeList();
		aConversionSourceAttributesSequence.addItem(itemList);
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.SOPClassUID,TagFromName.ReferencedSOPClassUID);
		addIfPresentWithValues(itemList,sourceList,done,TagFromName.SOPInstanceUID,TagFromName.ReferencedSOPInstanceUID);
	}
	
	private static Set<AttributeTag> excludeFromCopyingIntoFunctionalGroups = new HashSet<AttributeTag>();
	static {
		excludeFromCopyingIntoFunctionalGroups.add(TagFromName.SpecificCharacterSet);
	}
	
	/**
	 * <p>Given the Attributes of a "classic" single frame instance, choose an appropriate multi-frame (enhanced) image SOP Class to convert it to.</p>
	 *
	 * <p>If nothing modality-specific is found, default to multi-frame secondary capture if the Pixel Data characteristics are appropriate,
	 * otherwise in the worst case the {@link com.pixelmed.dicom.SOPClass#RawDataStorage SOPClass.RawDataStorage} is returned.</p>
	 *
	 * @param		list	the attributes of the single frame instance
	 * @return				the SOP Class UID
	 */
	public static String chooseAppropriateConvertedSOPClassUID(AttributeList list) {
		String sopClassUIDOfSource = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		String sopClassUIDOfTarget = SOPClass.RawDataStorage;
		if (sopClassUIDOfSource != null) {
			if (sopClassUIDOfSource.equals(SOPClass.CTImageStorage)) {
				sopClassUIDOfTarget = SOPClass.LegacyConvertedEnhancedCTImageStorage;
			}
			else if (sopClassUIDOfSource.equals(SOPClass.MRImageStorage)) {
				sopClassUIDOfTarget = SOPClass.LegacyConvertedEnhancedMRImageStorage;
			}
			else if (sopClassUIDOfSource.equals(SOPClass.PETImageStorage)) {
				sopClassUIDOfTarget = SOPClass.LegacyConvertedEnhancedPETImageStorage;
			}
			else if (sopClassUIDOfSource.equals(SOPClass.DigitalMammographyXRayImageStorageForPresentation) || sopClassUIDOfSource.equals(SOPClass.DigitalMammographyXRayImageStorageForProcessing)) {
				String imageTypeAllValues = Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,"");
				if (imageTypeAllValues.contains("SLICE")) {		// GE when using single frame MG puts this in value 4 as opposed to PROJECTION
					sopClassUIDOfTarget = SOPClass.BreastTomosynthesisImageStorage;
				}
			}
		}
		// if have not found something appropriate yet based on sopClassUIDOfSource, try and choose an MFSC based on pixel characteristics, if present ...
		if (sopClassUIDOfTarget.equals(SOPClass.RawDataStorage)) {
			String photometricInterpretation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PhotometricInterpretation);
			if (photometricInterpretation.equals("RGB")) {
				sopClassUIDOfTarget = SOPClass.MultiframeTrueColorSecondaryCaptureImageStorage;
			}
			else if (photometricInterpretation.equals("MONOCHROME2") || photometricInterpretation.equals("MONOCHROME1")) {
				int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsAllocated,0);
				if (bitsAllocated == 8) {
					sopClassUIDOfTarget = SOPClass.MultiframeGrayscaleByteSecondaryCaptureImageStorage;
				}
				else if (bitsAllocated == 16) {
					sopClassUIDOfTarget = SOPClass.MultiframeGrayscaleWordSecondaryCaptureImageStorage;
				}
			}
		}
		return sopClassUIDOfTarget;
	}
	
	protected static int nextStackID;
		
	public static void addStack(AttributeList list,Map<String,StackOfSlices> stacks) {
		// we will try and build a single stack if all the slices are parallel, regardless of the spacing (i.e., can't make multiple stacks for different orientations yet, like spine disk space para-axials :()
		try {
			StackOfSlices stack = new StackOfSlices(list);
			if (!stack.isValid()) {
				slf4jlogger.warn("addStack(): Could not make a valid stack");
				return;
			}
			String stackID = null;
			if (stacks != null) {
				for (String key : stacks.keySet()) {
					StackOfSlices testStack = stacks.get(key);
					if (testStack.equals(stack)) {
						stackID = key;
						stack = testStack;		// re-use existing stack and discard the new identical one
						break;
					}
				}
			}
			if (stackID == null) {
				stackID = Integer.toString(nextStackID++);
				if (stacks != null) {
					stacks.put(stackID,stack);
				}
				slf4jlogger.info("addStack(): Making new stack {}",stackID);
			}
			else {
				slf4jlogger.info("addStack(): Not replacing existing stack {}",stackID);
			}
			stack.addStackAttributesToExistingFrameContentSequence(list,stackID);
		}
		catch (DicomException e) {
			slf4jlogger.error("", e);
		}
	}
	
	public static void addStackIfNotAlreadyPresent(AttributeList list,Map<String,StackOfSlices> stacks) {
		SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.PerFrameFunctionalGroupsSequence));
		SequenceAttribute frameContentSequence = (SequenceAttribute)(perFrameFunctionalGroupsSequence.getItem(0).getAttributeList().get(TagFromName.FrameContentSequence));
		AttributeList frameContentList = frameContentSequence.getItem(0).getAttributeList();
		if (!frameContentList.containsKey(TagFromName.StackID) || !frameContentList.containsKey(TagFromName.InStackPositionNumber)) {
			addStack(list,stacks);
		}
		else {
			slf4jlogger.info("addStackIfNotAlreadyPresent(): Not replacing existing stack");
		}
	}
	
	private static Date farthestFutureDate = new Date(Long.MAX_VALUE);

	/**
	 * <p>Create an enhanced image from a set of DICOM single image files or {@link com.pixelmed.dicom.AttributeList AttributeList}s in a FrameSet.</p>
	 *
	 * @param		frameSet								an existing set of frames (single images) to convert that have already been determined to be a FrameSet
	 * @param		filesBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the files that contain them (null if listsBySOPInstanceUID supplied)
	 * @param		listsBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the {@link com.pixelmed.dicom.AttributeList AttributeList}s that contain them (null if filesBySOPInstanceUID supplied)
	 * @param		multiFrameReferenceBySingleFrameUID		an existing (possibly empty) map to which is added mappings from each single frame SOP Instance UIDs to converted UIDs + frame number references
	 * @param		stacks									an existing (possibly empty) set of stacks from other FrameSets, which will be extended or re-used if a stack is found
	 * @return												a list that is an enhanced multiframe image
	 * @throws	DicomException							if an input file cannot be found for a frame, or it cannot be parsed
	 * @throws	IOException								if an input file cannot be read
	 */
	public static AttributeList createEnhancedImageFromFrameSet(FrameSet frameSet,Map<String,File> filesBySOPInstanceUID,Map<String,AttributeList> listsBySOPInstanceUID,Map<String,HierarchicalImageReference> multiFrameReferenceBySingleFrameUID,Map<String,StackOfSlices> stacks) throws DicomException, IOException {
		slf4jlogger.info(frameSet.toString());
		Set<AttributeTag> distinguishingAttributeTags = frameSet.getDistinguishingAttributeTags();
		Set<AttributeTag> sharedAttributeTags = frameSet.getSharedAttributeTags();
		Set<AttributeTag> perFrameAttributeTags = frameSet.getPerFrameAttributeTags();
		
		String sopClassUIDOfTarget = "";	// will populate this when we encounter the first frame and have an AttributeList
		
		Date earliestFrameAcquisitionDateTimeSoFar = farthestFutureDate;
		Date earliestContentDateTimeSoFar = farthestFutureDate;
		
		AttributeList convertedList = new AttributeList();
		SequenceAttribute aSharedFunctionalGroupsSequence = new SequenceAttribute(TagFromName.SharedFunctionalGroupsSequence);
		convertedList.put(aSharedFunctionalGroupsSequence);
		AttributeList sharedFunctionalGroupsSequenceItemList = new AttributeList();
		aSharedFunctionalGroupsSequence.addItem(sharedFunctionalGroupsSequenceItemList);
		
		SequenceAttribute aPerFrameFunctionalGroupsSequence = new SequenceAttribute(TagFromName.PerFrameFunctionalGroupsSequence);
		convertedList.put(aPerFrameFunctionalGroupsSequence);
		
		MultiFramePixelData multiFramePixelData = null;
				
		boolean tzSpecified = false;		// will be set to true only if TimezoneOffsetFromUTC present with a value
		String timezoneString = null;		// will be set to value of TimezoneOffsetFromUTC if present, else "+0000"
		TimeZone timezone = null;			// will be set to value of TimezoneOffsetFromUTC if present, else GMT, to use as base for comparing dates and times throughout the program
		
		List<String> sopInstanceUIDs = frameSet.getSOPInstanceUIDsSortedByFrameOrder();
		double[] orientation = null;
		boolean firstInstance = true;
		for (String sopInstanceUID : sopInstanceUIDs) {
			AttributeList frameSourceList = listsBySOPInstanceUID == null ? null : listsBySOPInstanceUID.get(sopInstanceUID);
			if (frameSourceList == null) {
				File f = filesBySOPInstanceUID.get(sopInstanceUID);
				if (f != null) {
					frameSourceList = new AttributeList();
					frameSourceList.read(f);	// do NOT stop at PixelData this time, since we need it
				}
			}
			if (frameSourceList != null) {
				Set<AttributeTag> doneSharedSet = new HashSet<AttributeTag>();
				if (firstInstance) {
					timezoneString = Attribute.getSingleStringValueOrNull(frameSourceList,TagFromName.TimezoneOffsetFromUTC);	// NB. Assumes same in all frames :(
//System.err.println("timezoneString = "+timezoneString);
					if (timezoneString == null || timezoneString.length() == 0) {
						timezoneString = "+0000";
					}
					else {
						tzSpecified = true;
					}
					timezone = DateTimeAttribute.getTimeZone(timezoneString);
//System.err.println("timezone = "+timezone);

					sopClassUIDOfTarget = chooseAppropriateConvertedSOPClassUID(frameSourceList);

					int rows = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.Rows,0);
					int columns = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.Columns,0);
					int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(frameSourceList,TagFromName.SamplesPerPixel,1);
					
					int numberOfFrames = frameSet.size();
					{ Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); convertedList.put(a); }
					
					multiFramePixelData = new MultiFramePixelData(rows,columns,samplesPerPixel,numberOfFrames);
					

					{
						CompositeInstanceContext cic = new CompositeInstanceContext(frameSourceList,false/*forSR*/);
						// leave existing Series stuff ... will overwrite SeriesInstanceUID later
						// leave existing Instance stuff ... will overwrite SOPInstanceUID later
						AttributeList ciclist = cic.getAttributeList();
						for (AttributeTag t : ciclist.keySet()) {
							// NB. we test for !perFrameAttributeTags rather than is sharedAttributeTags, because that way common empty Type 2s like PatienBirthDate get inserted
							if (!perFrameAttributeTags.contains(t)) {
//System.err.println("copy top level dataset CompositeInstanceContext stuff ... "+t);
								addIfPresent(convertedList,ciclist,doneSharedSet,t);
							}
						}
					}
					
					addImagePixelModule(convertedList,frameSourceList,doneSharedSet);
					
					// may want to add any other "shared" stuff that needs to go into top level dataset rather than functional groups (or be ignored)
										
					if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedCTImageStorage)) {
						addEnhancedCommonImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);	// flag anything used as shared since we do not need to replicate it in unassigned groups
						addContrastBolusModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addEnhancedCTImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addAcquisitionContextModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);		// surprisingly, this is not used in MF SC IODs
					}
					else if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedMRImageStorage)) {
						addEnhancedCommonImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addContrastBolusModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addEnhancedMRImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addAcquisitionContextModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
					}
					else if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedPETImageStorage)) {
						addEnhancedCommonImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addEnhancedPETImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addAcquisitionContextModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
					}
					else if (sopClassUIDOfTarget.equals(SOPClass.BreastTomosynthesisImageStorage)) {
						addEnhancedCommonImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags,"TOMOSYNTHESIS");
						addBreastTomosynthesisAcquisitionModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addBreastViewModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addAcquisitionContextModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
					}
					else if (SOPClass.isMultiframeSecondaryCaptureImageStorage(sopClassUIDOfTarget)) {
						addSecondaryCaptureEquipmentModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addSecondaryCaptureImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);
						addSecondaryCaptureMultiFrameImageModule(convertedList,frameSourceList,doneSharedSet,perFrameAttributeTags);		// flag anything used as shared since we do not need to replicate it in unassigned groups
					}
					
					// need to be careful here with functional groups that may contain multiple attributes,
					// some of which may be shared and others per-frame, so check both not per-frame and shared (and distinguished too) ...

					if (!containsAttributesForFrameAnatomyFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForFrameAnatomyFunctionalGroup(sharedAttributeTags) || containsAttributesForFrameAnatomyFunctionalGroup(distinguishingAttributeTags))
					) {
						addFrameAnatomyFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
				
					if (!containsAttributesForPixelMeasuresFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPixelMeasuresFunctionalGroup(sharedAttributeTags) || containsAttributesForPixelMeasuresFunctionalGroup(distinguishingAttributeTags))
					) {
						addPixelMeasuresFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
				
					if (!containsAttributesForPlanePositionFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPlanePositionFunctionalGroup(sharedAttributeTags) || containsAttributesForPlanePositionFunctionalGroup(distinguishingAttributeTags))
					) {
						addPlanePositionFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForPlaneOrientationFunctionalGroup(perFrameAttributeTags)) {
//System.err.println("MultiFrameImageFactory.createEnhancedImageFromFrameSet(): does not contain per frame PlaneOrientation attributes");
						if (containsAttributesForPlaneOrientationFunctionalGroup(sharedAttributeTags) || containsAttributesForPlaneOrientationFunctionalGroup(distinguishingAttributeTags)) {
							addPlaneOrientationFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
						}
						else if (sopClassUIDOfTarget.equals(SOPClass.BreastTomosynthesisImageStorage)
							 && (containsAttributesForPlaneOrientationFunctionalGroupDerivedFromAngle(sharedAttributeTags) || containsAttributesForPlaneOrientationFunctionalGroupDerivedFromAngle(distinguishingAttributeTags))
						) {
							// not only add the functional group, but save the orientation for later computing the position
							orientation = addPlaneOrientationFunctionalGroupDerivedFromAngle(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
						}
					}
					
					if (!containsAttributesForFrameVOILUTFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForFrameVOILUTFunctionalGroup(sharedAttributeTags) || containsAttributesForFrameVOILUTFunctionalGroup(distinguishingAttributeTags))
					) {
						addFrameVOILUTFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedCTImageStorage)) {
						if (!containsAttributesForCTImageFrameTypeFunctionalGroup(perFrameAttributeTags)
						 && (containsAttributesForCTImageFrameTypeFunctionalGroup(sharedAttributeTags) || containsAttributesForCTImageFrameTypeFunctionalGroup(distinguishingAttributeTags))
						) {
							addCTImageFrameTypeFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
						}
					}
					else if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedMRImageStorage)) {
						if (!containsAttributesForMRImageFrameTypeFunctionalGroup(perFrameAttributeTags)
						 && (containsAttributesForMRImageFrameTypeFunctionalGroup(sharedAttributeTags) || containsAttributesForMRImageFrameTypeFunctionalGroup(distinguishingAttributeTags))
						) {
							addMRImageFrameTypeFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
						}
					}
					else if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedPETImageStorage)) {
						if (!containsAttributesForPETImageFrameTypeFunctionalGroup(perFrameAttributeTags)
						 && (containsAttributesForPETImageFrameTypeFunctionalGroup(sharedAttributeTags) || containsAttributesForPETImageFrameTypeFunctionalGroup(distinguishingAttributeTags))
						) {
							addPETImageFrameTypeFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
						}
					}
					// for some strange reason, the XRay3DImageFrameTypeFunctionalGroup is forbidden to be shared for Breast Tomo, though not other X-Ray 3D IODs :(
					//else if (sopClassUIDOfTarget.equals(SOPClass.BreastTomosynthesisImageStorage)) {
					//	if (!containsAttributesForXRay3DImageFrameTypeFunctionalGroup(perFrameAttributeTags)
					//	 && (containsAttributesForXRay3DImageFrameTypeFunctionalGroup(sharedAttributeTags) || containsAttributesForXRay3DImageFrameTypeFunctionalGroup(distinguishingAttributeTags))
					//	) {
					//		addXRay3DImageFrameTypeFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					//	}
					//}
					
					
					//if (!containsAttributesForRealWorldValueMappingFunctionalGroup(perFrameAttributeTags)
					// && (containsAttributesForRealWorldValueMappingFunctionalGroup(sharedAttributeTags) || containsAttributesForRealWorldValueMappingFunctionalGroup(distinguishingAttributeTags))
					//) {
					//	addRealWorldValueMappingFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					//}
															
					if (!containsAttributesForPixelValueTransformationFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForPixelValueTransformationFunctionalGroup(sharedAttributeTags) || containsAttributesForPixelValueTransformationFunctionalGroup(distinguishingAttributeTags))
					) {
						addPixelValueTransformationFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForReferencedImageFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForReferencedImageFunctionalGroup(sharedAttributeTags) || containsAttributesForReferencedImageFunctionalGroup(distinguishingAttributeTags))
					) {
						addReferencedImageFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					if (!containsAttributesForDerivationImageFunctionalGroup(perFrameAttributeTags)
					 && (containsAttributesForDerivationImageFunctionalGroup(sharedAttributeTags) || containsAttributesForDerivationImageFunctionalGroup(distinguishingAttributeTags))
					) {
						addDerivationImageFunctionalGroup(sharedFunctionalGroupsSequenceItemList,frameSourceList,doneSharedSet);
					}
					
					// defer populating UnassignedSharedConvertedAttributesSequence, since need to check some attributes that are actually the same (shared) but are required to be repeated per-frame (e.g., AcquisitionNumber in FrameContentSequence)
															
				}
				// else do not need to repeat distinguishing and shared, since FrameSet already guarantees thay are the same values
				
				AttributeList perFrameFunctionalGroupsSequenceItemList = new AttributeList();
				aPerFrameFunctionalGroupsSequence.addItem(perFrameFunctionalGroupsSequenceItemList);
				
				Set<AttributeTag> donePerFrameSet = new HashSet<AttributeTag>();

				if (containsAttributesForFrameAnatomyFunctionalGroup(perFrameAttributeTags)) {
					addFrameAnatomyFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForPixelMeasuresFunctionalGroup(perFrameAttributeTags)) {
					addPixelMeasuresFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				earliestFrameAcquisitionDateTimeSoFar = addFrameContentFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet,earliestFrameAcquisitionDateTimeSoFar,tzSpecified,timezoneString,perFrameAttributeTags);
				
				earliestContentDateTimeSoFar = getEarliestContentDateTime(frameSourceList,earliestContentDateTimeSoFar);
				
				if (containsAttributesForPlaneOrientationFunctionalGroup(perFrameAttributeTags)) {
					addPlaneOrientationFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				// could check if we need to derive orientation for mammo tomo on a per-frame rather than shaed basis, but doesn't happen :(
				
				if (containsAttributesForPlanePositionFunctionalGroup(perFrameAttributeTags)) {
					addPlanePositionFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				else if (sopClassUIDOfTarget.equals(SOPClass.BreastTomosynthesisImageStorage)
					 && containsAttributesForPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(perFrameAttributeTags)
					 && orientation != null && orientation.length == 6) {
					addPlanePositionFunctionalGroupDerivedFromTomoLayerHeight(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet,orientation);
				}
				
				if (containsAttributesForFrameVOILUTFunctionalGroup(perFrameAttributeTags)) {
					addFrameVOILUTFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (sopClassUIDOfTarget.equals(SOPClass.LegacyConvertedEnhancedCTImageStorage)) {
					if (containsAttributesForCTImageFrameTypeFunctionalGroup(perFrameAttributeTags)) {
						addCTImageFrameTypeFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
					}
				}
				else if (sopClassUIDOfTarget.equals(SOPClass.BreastTomosynthesisImageStorage)) {
					// for some strange reason, forbidden to be shared, even though usually common
					addXRay3DImageFrameTypeFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				//if (containsAttributesForRealWorldValueMappingFunctionalGroup(perFrameAttributeTags)) {
				//	addRealWorldValueMappingFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				//}
				
				if (containsAttributesForPixelValueTransformationFunctionalGroup(perFrameAttributeTags)) {
					addPixelValueTransformationFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForReferencedImageFunctionalGroup(perFrameAttributeTags)) {
					addReferencedImageFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				if (containsAttributesForDerivationImageFunctionalGroup(perFrameAttributeTags)) {
					addDerivationImageFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);
				}
				
				addConversionSourceFunctionalGroup(perFrameFunctionalGroupsSequenceItemList,frameSourceList,donePerFrameSet);

				{
					SequenceAttribute aUnassignedPerFrameConvertedAttributesSequence = new SequenceAttribute(TagFromName.UnassignedPerFrameConvertedAttributesSequence);
					perFrameFunctionalGroupsSequenceItemList.put(aUnassignedPerFrameConvertedAttributesSequence);
					AttributeList unassignedList =  new AttributeList();
					aUnassignedPerFrameConvertedAttributesSequence.addItem(unassignedList);

					for (AttributeTag t : perFrameAttributeTags) {
						if (!donePerFrameSet.contains(t) && !excludeFromCopyingIntoFunctionalGroups.contains(t)) {
							addIfPresent(unassignedList,frameSourceList,t);	// may be null if not in every frame
						}
					}
				}
				
				if (firstInstance) {
					// now go through shared list, and anything that is not in the convertedList already, should be added to the Unassigned Shared Converted Attributes Sequence
					
					{
						SequenceAttribute aUnassignedSharedConvertedAttributesSequence = new SequenceAttribute(TagFromName.UnassignedSharedConvertedAttributesSequence);
						sharedFunctionalGroupsSequenceItemList.put(aUnassignedSharedConvertedAttributesSequence);
						AttributeList unassignedList =  new AttributeList();
						aUnassignedSharedConvertedAttributesSequence.addItem(unassignedList);

						for (AttributeTag t : sharedAttributeTags) {
//System.err.println("MultiFrameImageFactory.createEnhancedImageFromFrameSet(): firstInstance - checking shared tag "+t);
							if (convertedList.get(t) == null && !doneSharedSet.contains(t) && !donePerFrameSet.contains(t) && !excludeFromCopyingIntoFunctionalGroups.contains(t)) {		// i.e., not already copied into top level data set or a specific shared or per-frame functional group
//System.err.println("MultiFrameImageFactory.createEnhancedImageFromFrameSet(): not already done or excluded so adding "+t);
								addIfPresent(unassignedList,frameSourceList,t);	// really shouldn't be null
							}
						}
					}
				}
			
				multiFramePixelData.addFrame(frameSourceList.getPixelData());
			}
			else {
				throw new DicomException("Missing File or AttributeList for SOP Instance UID "+sopInstanceUID+" in FrameSet");
			}
			firstInstance = false;
		}
		
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(sopClassUIDOfTarget); convertedList.put(a); }
		
		if (earliestFrameAcquisitionDateTimeSoFar.before(farthestFutureDate)) {
			// only include TZ suffix if it was explicitly specified
			Attribute a = new DateTimeAttribute(TagFromName.AcquisitionDateTime); a.addValue(DateTimeAttribute.getFormattedString(earliestFrameAcquisitionDateTimeSoFar,timezone,tzSpecified)); convertedList.put(a);
		}
		
		if (earliestContentDateTimeSoFar.before(farthestFutureDate)) {
			String contentDateTime = DateTimeAttribute.getFormattedString(earliestContentDateTimeSoFar,timezone,false);	/*do not want TZ suffix*/
//System.err.println("contentDateTime = "+contentDateTime);
			int l = contentDateTime.length();
			if (l >= 8) {
				Attribute a = new DateAttribute(TagFromName.ContentDate); a.addValue(contentDateTime.substring(0,8)); convertedList.put(a);
			}
			if (l > 8) {
				Attribute a = new TimeAttribute(TagFromName.ContentTime); a.addValue(contentDateTime.substring(8)); convertedList.put(a);
			}
		}
		
		{
			Attribute aPixelData = multiFramePixelData.getPixelDataAttribute();
			if (aPixelData != null) {
				convertedList.put(aPixelData);
			}
		}
		
		{
			String studyID = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.StudyID);
			String seriesNumber = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.SeriesNumber);
			
			String instanceNumber = "1";
			{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue(instanceNumber); convertedList.put(a); }
			
			UIDGenerator u = new UIDGenerator();	

			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getNewSOPInstanceUID(studyID,seriesNumber,instanceNumber)); convertedList.put(a); }
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getNewSeriesInstanceUID(studyID,seriesNumber)); convertedList.put(a); }


			{
				java.util.Date currentDateTime = new java.util.Date();
				{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate); a.addValue(new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime)); convertedList.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime); a.addValue(new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime)); convertedList.put(a); }
			}
		}
		
		addStackIfNotAlreadyPresent(convertedList,stacks);
		
		ClinicalTrialsAttributes.addContributingEquipmentSequence(convertedList,true/*retainExistingItems*/,
				new CodedSequenceItem("109106","DCM","Enhanced Multi-frame Conversion Equipment"),
				"PixelMed",														// Manufacturer
				"PixelMed",														// Institution Name
				"Software Development",											// Institutional Department Name
				"Bangor, PA",													// Institution Address
				null,															// Station Name
				"com.pixelmed.dicom.MultiFrameImageFactory",					// Manufacturer's Model Name
				null,															// Device Serial Number
				"Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
				"Legacy Enhanced Image created from Classic Images");
						
		convertedList.insertSuitableSpecificCharacterSetForAllStringValues();
		
		// temporary for testing ... overwrite and make SC ...
		//{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.MultiframeGrayscaleWordSecondaryCaptureImageStorage); convertedList.put(a); }
		
        FileMetaInformation.addFileMetaInformation(convertedList,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		
//System.err.println("MultiFrameImageFactory.createEnhancedImageFromFrameSet(): Result");
//System.err.println(convertedList.toString());

		// keep track of mapping from source SOP Instance UID to the new SOP Instance UID + Referenced Frame Number ...
		// need to do this AFTER all the frames have been populated, since until then the new UIDs are not assigned
		{
			String convertedStudyInstanceUID  = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.StudyInstanceUID);
			String convertedSeriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.SeriesInstanceUID);
			String convertedSOPInstanceUID    = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.SOPInstanceUID);
			String convertedSOPClassUID       = Attribute.getSingleStringValueOrEmptyString(convertedList,TagFromName.SOPClassUID);
			int frameIndex = 1;	// since DICOM numbers frames from 1
			for (String sopInstanceUID : sopInstanceUIDs) {		// these are already sorted in frame order
				HierarchicalImageReference frameReference = new HierarchicalImageReference(convertedStudyInstanceUID,convertedSeriesInstanceUID,convertedSOPInstanceUID,convertedSOPClassUID,Integer.toString(frameIndex++));
				multiFrameReferenceBySingleFrameUID.put(sopInstanceUID,frameReference);
			}
		}

		return convertedList;
	}

	/**
	 * <p>Create an enhanced image from a set of DICOM single image files or {@link com.pixelmed.dicom.AttributeList AttributeList}s in a FrameSet.</p>
	 *
	 * @param		frameSet								an existing set of frames (single images) to convert that have already been determined to be a FrameSet
	 * @param		filesBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the files that contain them (null if listsBySOPInstanceUID supplied)
	 * @param		listsBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the {@link com.pixelmed.dicom.AttributeList AttributeList}s that contain them (null if filesBySOPInstanceUID supplied)
	 * @param		multiFrameReferenceBySingleFrameUID		an existing (possibly empty) map to which is added mappings from each single frame SOP Instance UIDs to converted UIDs + frame number references
	 * @return												a list that is an enhanced multiframe image
	 * @throws	DicomException							if an input file cannot be found for a frame, or it cannot be parsed
	 * @throws	IOException								if an input file cannot be read
	 */
	public static AttributeList createEnhancedImageFromFrameSet(FrameSet frameSet,Map<String,File> filesBySOPInstanceUID,Map<String,AttributeList> listsBySOPInstanceUID,Map<String,HierarchicalImageReference> multiFrameReferenceBySingleFrameUID) throws DicomException, IOException {
		return createEnhancedImageFromFrameSet(frameSet,filesBySOPInstanceUID,listsBySOPInstanceUID,multiFrameReferenceBySingleFrameUID,null);
	}
	
	/**
	 * <p>Create an enhanced image from a set of DICOM single image files in a FrameSet.</p>
	 *
	 * @param		frameSet								an existing set of frames (single images) to convert that have already been determined to be a FrameSet
	 * @param		outputFolder							a folder in which to store converted files (which must already exist)
	 * @param		filesBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the files that contain them
	 * @param		multiFrameReferenceBySingleFrameUID		an existing (possibly empty) map to which is added mappings from each single frame SOP Instance UIDs to converted UIDs + frame number references
	 * @param		stacks									an existing (possibly empty) set of stacks from other FrameSets, which will be extended or re-used if a stack is found 
	 * @return												a file that is an enhanced multiframe image that was created
	 * @throws	DicomException							if an input file cannot be found for a frame, or it cannot be parsed
	 * @throws	IOException								if an input file cannot be read
	 */
	public static File createEnhancedImageFromFrameSet(FrameSet frameSet,File outputFolder,Map<String,File> filesBySOPInstanceUID,Map<String,HierarchicalImageReference> multiFrameReferenceBySingleFrameUID,Map<String,StackOfSlices> stacks) throws DicomException, IOException {
		AttributeList convertedList = createEnhancedImageFromFrameSet(frameSet,filesBySOPInstanceUID,null/*listsBySOPInstanceUID*/,multiFrameReferenceBySingleFrameUID,stacks);
		File convertedFile = new File(outputFolder,Attribute.getSingleStringValueOrDefault(convertedList,TagFromName.SOPInstanceUID,"NONAME"));
		convertedList.write(convertedFile);
		return convertedFile;
	}

	
	/**
	 * <p>Create an enhanced image from a set of DICOM single image files in a FrameSet.</p>
	 *
	 * @param		frameSet								an existing set of frames (single images) to convert that have already been determined to be a FrameSet
	 * @param		outputFolder							a folder in which to store converted files (which must already exist)
	 * @param		filesBySOPInstanceUID					an existing map of the SOP Instance UIDs of the single images to the files that contain them
	 * @param		multiFrameReferenceBySingleFrameUID		an existing (possibly empty) map to which is added mappings from each single frame SOP Instance UIDs to converted UIDs + frame number references
	 * @return												a file that is an enhanced multiframe image that was created
	 * @throws	DicomException							if an input file cannot be found for a frame, or it cannot be parsed
	 * @throws	IOException								if an input file cannot be read
	 */
	public static File createEnhancedImageFromFrameSet(FrameSet frameSet,File outputFolder,Map<String,File> filesBySOPInstanceUID,Map<String,HierarchicalImageReference> multiFrameReferenceBySingleFrameUID) throws DicomException, IOException {
		return createEnhancedImageFromFrameSet(frameSet,outputFolder,filesBySOPInstanceUID,multiFrameReferenceBySingleFrameUID,null);
	}

	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files			a set of files (not folders) to convert
	 * @param		outputFolder	a folder in which to store converted files (which must already exist)
	 * @return						the files created
	 * @throws	DicomException	if folder in which to store converted files does not exist
	 * @throws	IOException		if an input file cannot be read
	 */
	public static File[] convertImages(Set<File> files,File outputFolder) throws DicomException, IOException {
		ArrayList<File> outputFiles = new ArrayList<File>();
		if (!outputFolder.isDirectory()) {
			throw new DicomException("Output folder "+outputFolder+" does not exist");
		}
		// Pass 1 ... build a set of FrameSets for unenhanced ("classic") images, and a list of those not included
		
		Map<String,File> filesBySOPInstanceUID = new HashMap<String,File>();
		SetOfFrameSets setOfFrameSets = new SetOfFrameSets();
		Set<String> setOfUnconvertedSOPInstanceUIDs = setOfUnconvertedSOPInstanceUIDs = new HashSet<String>();
		Map<String,HierarchicalSOPInstanceReference> unconvertedHierarchicalInstancesBySOPInstanceUID = new HashMap<String,HierarchicalSOPInstanceReference>();
		for (File f : files) {
			try {
				if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
//System.err.println("MultiFrameImageFactory.doCommonConstructorStuff(): Doing "+f);
					AttributeList list = new AttributeList();
					list.read(f,TagFromName.PixelData);
					String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
					String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
					if (sopInstanceUID.length() > 0 && sopClassUID.length() > 0) {
						filesBySOPInstanceUID.put(sopInstanceUID,f);
						{
							unconvertedHierarchicalInstancesBySOPInstanceUID.put(sopInstanceUID,new HierarchicalSOPInstanceReference(list));	// i.e. add ourselves
							HierarchicalSOPInstanceReference.addToHierarchicalReferencesToSOPInstances(list,unconvertedHierarchicalInstancesBySOPInstanceUID);
						}
						if (SOPClass.isImageStorage(sopClassUID) && !list.isEnhanced()) {		// 	do not use list.isImage() since it will fail, because we deliberately did NOT read the PixelData attribute to save time this pass
							setOfFrameSets.insertIntoFrameSets(list);
						}
						else {
							slf4jlogger.info("doCommonConstructorStuff(): Doing nothing to non-image or already enhanced \"{}\" ({})",f,SOPClassDescriptions.getAbbreviationFromUID(sopClassUID));
							setOfUnconvertedSOPInstanceUIDs.add(sopInstanceUID);
						}
					}
					else {
						throw new DicomException("Missing SOP Instance or Class UID in file "+f);
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("While reading \"{}\"",f,e);	// do NOT call f.getCanonicalPath(), since may throw Exception !
			}
		}
//System.err.println("MultiFrameImageFactory.doCommonConstructorStuff(): FrameSets Result:");
//		System.err.println(setOfFrameSets.toString());
//System.err.println("MultiFrameImageFactory.doCommonConstructorStuff(): unconvertedHierarchicalInstancesBySOPInstanceUID:");
//		System.err.println(HierarchicalSOPInstanceReference.toString(unconvertedHierarchicalInstancesBySOPInstanceUID));

		// Pass 2 ... convert FrameSets into enhanced images
		nextStackID = 1;
		Map<String,StackOfSlices> stacks = new TreeMap<String,StackOfSlices>();
		Map<String,HierarchicalImageReference> multiFrameReferenceBySingleFrameUID = new HashMap<String,HierarchicalImageReference>();
		for (FrameSet frameSet : setOfFrameSets) {
			File enhancedImage = createEnhancedImageFromFrameSet(frameSet,outputFolder,filesBySOPInstanceUID,multiFrameReferenceBySingleFrameUID,stacks);
			outputFiles.add(enhancedImage);
		}

//System.err.println("MultiFrameImageFactory.doCommonConstructorStuff(): multiFrameReferenceBySingleFrameUID:");
//		{
//			Iterator<String> i = multiFrameReferenceBySingleFrameUID.keySet().iterator();
//			while (i.hasNext()) {
//				String key = i.next();
//				HierarchicalImageReference ref = multiFrameReferenceBySingleFrameUID.get(key);
//				System.err.println("Single Frame SOP Instance UID "+key+" -> "+ref);
//			}
//		}
		
		return outputFiles.toArray(new File[outputFiles.size()]);
	}
	
	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files			an array of files (not folders) to convert
	 * @param		outputFolder	a folder in which to store converted files (which must already exist)
	 * @return						the files created
	 * @throws	DicomException	if folder in which to store converted files does not exist
	 * @throws	IOException		if an input file cannot be read
	 */
	public static File[] convertImages(File files[],File outputFolder) throws DicomException, IOException {
		Set<File> inputFiles = new HashSet<File>();
		for (File f : files) {
			inputFiles.add(f);
		}
		return convertImages(inputFiles,outputFolder);
	}
	
	/**
	 * <p>Create a new set of instances, converting to enhanced images when possible, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		inputPaths		a set of paths of filenames and/or folder names of files containing the images to convert
	 * @param		outputPath		a path in which to store converted files (which must already exist)
	 * @return						the files created
	 * @throws	DicomException	if folder in which to store converted files does not exist
	 * @throws	IOException		if an input file cannot be read
	 */
	public static File[] convertImages(String inputPaths[],String outputPath) throws DicomException, IOException {
		Set<File> inputFiles = new HashSet<File>();
		for (String p : inputPaths) {
			Collection<File> more = FileUtilities.listFilesRecursively(new File(p));
			inputFiles.addAll(more);
		}
		return convertImages(inputFiles,new File(outputPath));
	}

	/**
	 * <p>For testing, read all DICOM files and convert them to enhanced images when possible.</p>
	 *
	 * @param	arg	the filenames and/or folder names of files containing the images to partition, followed by the path in which to store the converted instances
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 2) {
				int inputCount = arg.length - 1;
				String outputPath = arg[inputCount];
				String[] inputPaths = new String[inputCount];
				System.arraycopy(arg,0,inputPaths,0,inputCount);
				File[] outputFiles = convertImages(inputPaths,outputPath);
				for (File of : outputFiles) {
					System.err.println("Output file: "+of);	// no need to use SLF4J since command line utility/test
				}
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: MultiFrameImageFactory inputPaths outputPath");
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(1);
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DescriptionFactory;
import com.pixelmed.dicom.LossyImageCompression;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to extract selected DICOM annotative attributes into defined displayed area relative positions.</p>
 *
 * @author	dclunie
 */
public class DemographicAndTechniqueAnnotations {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DemographicAndTechniqueAnnotations.java,v 1.26 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DemographicAndTechniqueAnnotations.class);
	
	protected static final int NOSPECIAL = 0;
	protected static final int JUSTTIME = 1;
	protected static final int SLICESPACING = 2;
	protected static final int FRAMENUMBER = 3;
	//protected static final int CONTRAST = 4;
	protected static final int IMAGETYPE = 5;
	protected static final int PIXELREPN = 6;
	protected static final int CODEMEANING = 7;
	protected static final int ORIENTLABEL = 8;
	protected static final int ABBREVPHOTO = 9;
	protected static final int LOSSYCOMPRESSED = 10;
	protected static final int TEXTIFYESNO = 11;
	protected static final int CALLFUNCTION = 12;
	protected static final int DIRECTIONVECTOR = 13;
	protected static final int XRAYTUBECURRENT = 14;
	protected static final int EXPOSURETIME = 15;
	protected static final int EXPOSURE = 16;

	protected class AnnotationLayoutConfigurationEntry {
		String sopClassUID;		// the SOP Class to which is applicable, all SOP Classes if null
		AttributeTag tag;		// null if fixed text value is to be used
		AttributeTag functionalGroup;	// null if not found nested in a per-frame or shared functional group
		AttributeTag nestedAttribute;	// (e.g. tag is modifier within nestedAttribute) null if tag is not nested in another sequence item
		String text;			// null if attribute value is to be used
		boolean fromLeft;
		boolean fromTop;
		int textRow;
		int orderInRow;			// the position, left to right, in the row, if multiple, starting from 0
		String decimalFormatPattern;	// to be feed to java.text.DecimalFormat, null if none or not a number
		int specialFunction;		// NOSPECIAL if none
		
		AnnotationLayoutConfigurationEntry(String sopClassUID,AttributeTag tag,AttributeTag functionalGroup,AttributeTag nestedAttribute,String text,
				boolean fromLeft,boolean fromTop,int textRow,int orderInRow,String decimalFormatPattern,int specialFunction) {
			this.sopClassUID=sopClassUID;
			this.tag=tag;
			this.functionalGroup=functionalGroup;
			this.nestedAttribute=nestedAttribute;
			this.text=text;
			this.fromLeft=fromLeft;
			this.fromTop=fromTop;
			this.textRow=textRow;
			this.orderInRow=orderInRow;
			this.decimalFormatPattern=decimalFormatPattern;
			this.specialFunction=specialFunction;
		}
	}
	
	protected Vector layout;			// of AnnotationLayoutConfigurationEntry
	private int layoutTopLeftRows;		// number of rows used in top left corner
	private int layoutBottomLeftRows;
	private int layoutTopRightRows;
	private int layoutBottomRightRows;
	
	private boolean swapLeftRight;

	protected void initializeDefaultLayout() {
		layout=new Vector();
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.InstitutionName,null,null,null,true,true,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Manufacturer,null,null,null,true,true,1,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ManufacturerModelName,null,null,null,true,true,1,2,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"[",true,true,2,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientID,null,null,null,true,true,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"] ",true,true,2,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientName,null,null,null,true,true,2,3,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientSex,null,null,null,true,true,3,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,3,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientBirthDate,null,null,null,true,true,3,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," [",true,true,3,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientAge,null,null,null,true,true,3,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"]",true,true,3,5,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.FrameLaterality,TagFromName.FrameAnatomySequence,null,null,true,true,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("NOTENH",TagFromName.Laterality,null,null,null,true,true,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("NOTENH",TagFromName.ImageLaterality,null,null,null,true,true,4,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.BodyPartExamined,null,null,null,true,true,4,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AnatomicRegionSequence,TagFromName.FrameAnatomySequence,null,null,true,true,4,5,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AnatomicRegionSequence,null,null,null,true,true,4,5,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ViewPosition,null,null,null,true,true,4,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,8,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ViewCodeSequence,null,null,null,true,true,4,9,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,4,10,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ViewModifierCodeSequence,null,TagFromName.ViewCodeSequence,null,true,true,4,11,null,CODEMEANING));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientSpeciesDescription,null,null,null,true,true,5,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,5,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientSpeciesCodeSequence,null,null,null,true,true,5,2,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,5,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ResponsiblePersonRole,null,null,null,true,true,5,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,5,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ResponsiblePerson,null,null,null,true,true,5,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,5,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ResponsibleOrganization,null,null,null,true,true,5,8,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,5,9,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientSexNeutered,null,null,null,true,true,5,10,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyDescription,null,null,null,false,true,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesDescription,null,null,null,false,true,1,0,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyID,null,null,null,false,true,2,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," [",false,true,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AccessionNumber,null,null,null,false,true,2,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"]",false,true,2,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyDate,null,null,null,false,true,3,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"Series #",false,true,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesNumber,null,null,null,false,true,4,1,null,NOSPECIAL));
		
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.FrameComments,TagFromName.FrameContentSequence,null,null,false,false,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,0,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.FrameType,TagFromName.MRImageFrameTypeSequence,null,null,false,false,0,2,null,IMAGETYPE));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,TagFromName.FrameType,TagFromName.CTImageFrameTypeSequence,null,null,false,false,0,2,null,IMAGETYPE));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ImageComments,null,null,null,false,false,1,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ImageType,null,null,null,false,false,1,2,null,IMAGETYPE));

		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.FrameAcquisitionDateTime,TagFromName.FrameContentSequence,null,null,false,false,2,0,null,JUSTTIME));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null," ",false,false,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AcquisitionDateTime,null,null,null,false,false,2,2,null,JUSTTIME));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,2,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AcquisitionTime,null,null,null,false,false,2,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,2,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ContentTime,null,null,null,false,false,2,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null," [",false,false,2,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.TemporalPositionIndex,TagFromName.FrameContentSequence,null,null,false,false,2,8,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null,"]",false,false,2,9,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"F #",false,false,3,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,null,false,false,3,1,null,FRAMENUMBER));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," I #",false,false,3,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.InstanceNumber,null,null,null,false,false,3,3,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Columns,null,null,null,false,false,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"x",false,false,4,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Rows,null,null,null,false,false,4,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"x",false,false,4,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.NumberOfFrames,null,null,null,false,false,4,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,4,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PixelRepresentation,null,null,null,false,false,4,6,null,PIXELREPN));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.BitsStored,null,null,null,false,false,4,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,4,8,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PhotometricInterpretation,null,null," ",false,false,4,9,null,ABBREVPHOTO));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",false,false,4,10,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,null,false,false,4,11,null,LOSSYCOMPRESSED));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.DerivationDescription,null,null,null,false,false,5,0,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null,"[",true,false,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.StackID,TagFromName.FrameContentSequence,null,null,true,false,0,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null,":",true,false,0,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.InStackPositionNumber,TagFromName.FrameContentSequence,null,null,true,false,0,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",null,null,null,"] ",true,false,0,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",TagFromName.ImagePositionPatient,TagFromName.PlanePositionSequence,null,null,true,false,0,5,"###.#",NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,null,null,null," [",true,false,0,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,TagFromName.TablePosition,TagFromName.CTPositionSequence,null,null,true,false,0,7,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,null,null,null,"]",true,false,0,8,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry("XCOLD",null,null,null," [",true,false,0,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCOLD",TagFromName.SliceLocation,null,null,null,true,false,0,7,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCOLD",null,null,null,"]",true,false,0,8,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null," mm",true,false,0,9,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",TagFromName.ImageOrientationPatient,TagFromName.PlaneOrientationSequence,null,null,true,false,1,0,"#.##",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null," ",true,false,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",TagFromName.ImageOrientationPatient,TagFromName.PlaneOrientationSequence,null,null,true,false,1,2,null,ORIENTLABEL));
		
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",TagFromName.PixelSpacing,TagFromName.PixelMeasuresSequence,null,null,true,false,2,0,"#.###",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null,"\\",true,false,2,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null,null,true,false,2,3,"#.###",SLICESPACING));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null," ",true,false,2,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",TagFromName.SliceThickness,TagFromName.PixelMeasuresSequence,null,null,true,false,2,5,"##.##",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," ",true,false,2,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.ReconstructionDiameter,TagFromName.CTReconstructionSequence,null,null,true,false,2,7,"###.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,TagFromName.ReconstructionFieldOfView,TagFromName.CTReconstructionSequence,null,null,true,false,2,7,"###.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XCIMAGE",null,null,null," mm",true,false,2,8,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.FlipAngle,TagFromName.MRTimingAndRelatedParametersSequence,null,null,true,false,3,0,"#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null,"\u00b0 ETL ",true,false,3,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.GradientEchoTrainLength,TagFromName.MRTimingAndRelatedParametersSequence,null,null,true,false,3,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,null,null,null,"+",true,false,3,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.RFEchoTrainLength,TagFromName.MRTimingAndRelatedParametersSequence,null,null,true,false,3,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,null,null,null,"=",true,false,3,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.EchoTrainLength,TagFromName.MRTimingAndRelatedParametersSequence,null,null,true,false,3,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," Avg ",true,false,3,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.NumberOfAverages,TagFromName.MRAveragesSequence,null,null,true,false,3,8,"#.#",NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.KVP,TagFromName.CTXRayDetailsSequence,null,null,true,false,3,0,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," kVP ",true,false,3,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedCTImageStorage,TagFromName.ExposureInmAs,TagFromName.CTExposureSequence,null,null,true,false,3,2,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.CTImageStorage,null,null,null,null,true,false,3,2,"#.#",EXPOSURE));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," mAs",true,false,3,3,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null,"TE ",true,false,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.EffectiveEchoTime,TagFromName.MREchoSequence,null,null,true,false,4,1,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.MRImageStorage,TagFromName.EchoTime,null,null,null,true,false,4,1,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," TR ",true,false,4,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.RepetitionTime,TagFromName.MRTimingAndRelatedParametersSequence,null,null,true,false,4,3,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," TI ",true,false,4,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.InversionTimes,TagFromName.MRModifierSequence,null,null,true,false,4,5,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.MRImageStorage,TagFromName.InversionTime,null,null,null,true,false,4,5,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," mS",true,false,4,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," B ",true,false,4,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.DiffusionBValue,TagFromName.MRDiffusionSequence,null,null,true,false,4,8,"#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",null,null,null," ",true,false,4,10,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("MRIMAGE",TagFromName.DiffusionGradientOrientation,TagFromName.MRDiffusionSequence,TagFromName.DiffusionGradientDirectionSequence,null,true,false,4,11,null,DIRECTIONVECTOR));

		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.AcquisitionType,TagFromName.CTAcquisitionTypeSequence,null,null,true,false,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," ",true,false,4,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.SpiralPitchFactor,TagFromName.CTTableDynamicsSequence,null,null,true,false,4,2,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null,":1 ",true,false,4,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.TableSpeed,TagFromName.CTTableDynamicsSequence,null,null,true,false,4,4,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," mm/s",true,false,4,5,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(SOPClass.EnhancedMRImageStorage,TagFromName.AcquisitionContrast,TagFromName.MRImageFrameTypeSequence,null,null,true,false,5,0,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.ConvolutionKernel,TagFromName.CTReconstructionSequence,null,null,true,false,5,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",null,null,null," ",true,false,5,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("CTIMAGE",TagFromName.ConvolutionKernelGroup,TagFromName.CTReconstructionSequence,null,null,true,false,5,2,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("XCENH",TagFromName.ContrastBolusAgentPhase,TagFromName.ContrastBolusUsageSequence,null,null,true,false,6,0,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",TagFromName.KVP,null,null,null,true,false,0,0,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null," kVP ",true,false,0,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null,null,true,false,1,0,"#.#",EXPOSURE));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null," mAs",true,false,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null,null,true,false,2,0,"#.#",EXPOSURETIME));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null," ms",true,false,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null,null,true,false,3,0,"#.#",XRAYTUBECURRENT));
		layout.add(new AnnotationLayoutConfigurationEntry("XRAYOLD",null,null,null," mA",true,false,3,1,null,NOSPECIAL));
	}

	private Vector[] annotations;	// an array[number of frames] of TextAnnotationPositioned
	
	private class AnnotationComponentEntry implements Comparable {
		String string;
		int textRow;
		int orderInRow;
		
		AnnotationComponentEntry(String string,int textRow,int orderInRow) {
			this.string=string;
			this.textRow=textRow;
			this.orderInRow=orderInRow;
		}

		public int compareTo(Object o) {
			AnnotationComponentEntry e = (AnnotationComponentEntry)o;
			if (textRow == e.textRow) {
				return orderInRow-e.orderInRow;
			}
			else {
				return textRow-e.textRow;
			}
		}
	}
	
	// array index
	// 0 - top left
	// 1 - bottom left
	// 2 - top right
	// 3 - bottom right
	
	private static final int getArrayIndexFromLeftTop(boolean isLeft,boolean isTop) {
		return isLeft ? (isTop ? 0 : 1) : (isTop ? 2 : 3);
	}
	
	private static final boolean isLeftFromArrayIndex(int index) {
		return index == 0 || index == 1;
	}
	
	private static final boolean isTopFromArrayIndex(int index) {
		return index == 0 || index == 2;
	}
	
	protected static final String multipleCodeMeaningDelimiter = " ";
	
	private static final String getFormattedValue(Attribute a,int specialFunction,NumberFormat formatter) {
		String value = null;
		if (a != null) {
			if (specialFunction == CODEMEANING && a instanceof SequenceAttribute) {
				//CodedSequenceItem csi = CodedSequenceItem.getSingleCodedSequenceItemOrNull(a);
				//if (csi != null) {
				//	value=csi.getCodeMeaning();
				//}
				
				// concatentate possibly multiple sequence items
				Iterator sitems = ((SequenceAttribute)a).iterator();
				while (sitems.hasNext()) {
					SequenceItem sitem = (SequenceItem)sitems.next();
					AttributeList slist = sitem.getAttributeList();
					if (slist != null) {
						String nextValue = Attribute.getSingleStringValueOrNull(slist,TagFromName.CodeMeaning);
						if (nextValue != null && nextValue.length() > 0) {
							value = value == null || value.length() == 0 ?  nextValue : (value + multipleCodeMeaningDelimiter + nextValue);
						}
					}
				}
			}
			else if (specialFunction == ORIENTLABEL && a.getVM() == 6) {
				try {
					double v[] = a.getDoubleValues();
					value = DescriptionFactory.makeImageOrientationLabelFromImageOrientationPatient(v[0],v[1],v[2],v[3],v[4],v[5]);
				}
				catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
			else if (specialFunction == DIRECTIONVECTOR) {
//System.err.println("DemographicAndTechniqueAnnotations.getFormattedValue(): specialFunction == DIRECTIONVECTOR for attribute "+a);
				try {
					double v[] = a.getDoubleValues();
					value = GeometryOfSlice.getOrientation(v);
//System.err.println("DemographicAndTechniqueAnnotations.getFormattedValue(): specialFunction == DIRECTIONVECTOR value "+value);
				}
				catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
			else if (specialFunction == JUSTTIME && a instanceof DateTimeAttribute) {
				try {
					String[] v = a.getOriginalStringValues();
					StringBuffer sb = new StringBuffer();
					if (v != null) {
						char delimiter=0;
						for (int j=0; j<v.length; ++j) {
							String justTime = null;
							if (v[j] != null && v[j].length() > 8) {
								justTime = v[j].substring(8);
							}
							if (delimiter != 0) {
								sb.append(delimiter);
							}
							if (justTime != null) {
								sb.append(justTime);
							}
							delimiter='\\';
						}
						value=sb.toString();
					}
				}
				catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
			else if (specialFunction == IMAGETYPE) {
				try {
					String[] v = a.getStringValues();
					StringBuffer sb = new StringBuffer();
					if (v != null) {
						char delimiter=0;
						for (int j=0; j<v.length; ++j) {
							String s = v[j];
							if (s != null) {
								if (j == 0 && s.equals("ORIGINAL")
								 || j == 1 && s.equals("PRIMARY")
								 || j == 3 && s.equals("NONE")
								 ) {
								}
								else {
									if (delimiter != 0) {
										sb.append(delimiter);
									}
									sb.append(s);
									delimiter='\\';
								}
							}
						}
						value=sb.toString();
					}
				}
				catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
			else if (specialFunction == ABBREVPHOTO && a.getTag().equals(TagFromName.PhotometricInterpretation)) {
				value = a.getSingleStringValueOrNull();
				if (value != null) {
					if (value.equals("MONOCHROME2")) {
						value="M2";
					}
					else if (value.equals("MONOCHROME1")) {
						value="M1";
					}
					else if (value.equals("PALETTE COLOR")) {
						value="PAL";
					}
					// else leave alone
				}
			}
			else if (specialFunction == PIXELREPN && a.getTag().equals(TagFromName.PixelRepresentation)) {
				value = a.getSingleIntegerValueOrDefault(0) == 0 ? "+" : "-";
			}
			else {
				value = a.getDelimitedStringValuesOrNull(formatter);
			}
		}
		return value;
	}
	
	private static final String getOneOfThreeNumericAttributesOrNull(
			AttributeList list,NumberFormat formatter,
			AttributeTag tag1,double multiplier1,
			AttributeTag tag2,double multiplier2,
			AttributeTag tag3,double multiplier3) {
		String value = null;
		double multiplier;
		Attribute a = list.get(tag1);
		if (a != null && a.getVM() > 0) {
			multiplier = multiplier1;
		}
		else {
			a = list.get(tag2);
			if (a != null && a.getVM() > 0) {
				multiplier = multiplier2;
			}
			else {
				a = list.get(tag3);
				multiplier = multiplier3;
			}
		}
		if (a != null && a.getVM() > 0) {
			value=formatter.format(a.getSingleDoubleValueOrDefault(0)*multiplier);
		}
		return value;
	}
							
	/**
	 * @param	list			the DICOM attributes of a single or multi-frame image
	 */
	public DemographicAndTechniqueAnnotations(AttributeList list) {
		this(list,null,false);
	}
	
	/**
	 * @param	list			the DICOM attributes of a single or multi-frame image
	 * @param	geometry		the geometry of a single or multi-frame image (or null if no 3D coordinate system)
	 */
	public DemographicAndTechniqueAnnotations(AttributeList list,GeometryOfVolume geometry) {
		this(list,geometry,false);
	}
	
	/**
	 * @param	list			the DICOM attributes of a single or multi-frame image
	 * @param	geometry		the geometry of a single or multi-frame image (or null if no 3D coordinate system)
	 * @param	swapLeftRight	whether the sides (left and right) to annotate are to be swapped
	 */
	public DemographicAndTechniqueAnnotations(AttributeList list,GeometryOfVolume geometry,boolean swapLeftRight) {
		this.swapLeftRight = swapLeftRight;
		if (list != null) {
			int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
			annotations = new Vector[numberOfFrames];
			for (int f=0; f<numberOfFrames; ++f) {
				annotations[f] = new Vector();
			}

			String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
			if (sopClassUID != null && numberOfFrames > 0) {
				initializeDefaultLayout();
				
				// for each frame, build individual components of rows, then flatten components sorted within each row into single string per row
				
				SequenceAttribute sharedFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.SharedFunctionalGroupsSequence));
				SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.PerFrameFunctionalGroupsSequence));

				for (int f=0; f<numberOfFrames; ++f) {
					TreeSet set[] = new TreeSet[4];
					for (int s=0; s<4; ++s) {
						set[s] = new TreeSet();		// of sorted AnnotationComponentEntry
					}
					
					Iterator i = layout.iterator();
					while (i.hasNext()) {
						AnnotationLayoutConfigurationEntry le = (AnnotationLayoutConfigurationEntry)i.next();
						if ( le.sopClassUID == null
						 ||  le.sopClassUID.equals(sopClassUID)
						 || (le.sopClassUID.equals("MRIMAGE") && (sopClassUID.equals(SOPClass.EnhancedMRImageStorage) || sopClassUID.equals(SOPClass.MRImageStorage)) )
						 || (le.sopClassUID.equals("CTIMAGE") && (sopClassUID.equals(SOPClass.EnhancedCTImageStorage) || sopClassUID.equals(SOPClass.CTImageStorage)) )
						 || (le.sopClassUID.equals("XCIMAGE") && SOPClass.isEnhancedMultiframeImageWithPlanePositionOrientationAndMeasuresStorage(sopClassUID) )
						 || (le.sopClassUID.equals("XCENH")   && SOPClass.isEnhancedMultiframeImageStorage(sopClassUID) )
						 || (le.sopClassUID.equals("NOTENH")  && !SOPClass.isEnhancedMultiframeImageStorage(sopClassUID) )
						 || (le.sopClassUID.equals("XCOLD")   && (sopClassUID.equals(SOPClass.MRImageStorage) || sopClassUID.equals(SOPClass.CTImageStorage)) )
						 || (le.sopClassUID.equals("XRAYOLD")
							&& (
								   sopClassUID.equals(SOPClass.ComputedRadiographyImageStorage)
								|| sopClassUID.equals(SOPClass.DigitalXRayImageStorageForPresentation)
								|| sopClassUID.equals(SOPClass.DigitalXRayImageStorageForProcessing)
								|| sopClassUID.equals(SOPClass.DigitalMammographyXRayImageStorageForPresentation)
								|| sopClassUID.equals(SOPClass.DigitalMammographyXRayImageStorageForProcessing)
								|| sopClassUID.equals(SOPClass.DigitalIntraoralXRayImageStorageForPresentation)
								|| sopClassUID.equals(SOPClass.DigitalIntraoralXRayImageStorageForProcessing)
								|| sopClassUID.equals(SOPClass.XRayAngiographicImageStorage)
								|| sopClassUID.equals(SOPClass.XRayRadioFlouroscopicImageStorage)
								) )
						 || (le.sopClassUID.equals("XRAYENH")
							&& (
								   sopClassUID.equals(SOPClass.EnhancedXAImageStorage)
								|| sopClassUID.equals(SOPClass.EnhancedXRFImageStorage)
								|| sopClassUID.equals(SOPClass.XRay3DAngiographicImageStorage)
								|| sopClassUID.equals(SOPClass.XRay3DCraniofacialImageStorage)
								) )
						 ) {
							NumberFormat formatter = null;
							if (le.decimalFormatPattern != null) {
								try {
									formatter = new DecimalFormat(le.decimalFormatPattern);
								}
								catch (Exception e) {
									slf4jlogger.error("",e);
									formatter=null;
								}
							}
							String value = null;

							if (le.specialFunction == XRAYTUBECURRENT) {
								// want in mA
								value = getOneOfThreeNumericAttributesOrNull(list,formatter,
									TagFromName.XRayTubeCurrentInuA,0.001,
									TagFromName.XRayTubeCurrentInmA,1,
									TagFromName.XRayTubeCurrent,1);
							}
							else if (le.specialFunction == EXPOSURETIME) {
								// want in mS
								value = getOneOfThreeNumericAttributesOrNull(list,formatter,
									TagFromName.ExposureTimeInuS,0.001,
									TagFromName.ExposureTimeInms,1,
									TagFromName.ExposureTime,1);
							}
							else if (le.specialFunction == EXPOSURE) {
								// want in mAs
								value = getOneOfThreeNumericAttributesOrNull(list,formatter,
									TagFromName.ExposureInuAs,0.001,
									TagFromName.ExposureInmAs,1,
									TagFromName.Exposure,1);
							}
							else if (le.specialFunction == SLICESPACING) {
								if (geometry != null && geometry.isVolumeSampledRegularlyAlongFrameDimension()) {
									GeometryOfSlice sliceGeometries[] = geometry.getGeometryOfSlices();
									if (sliceGeometries != null && sliceGeometries.length > 0) {
										double voxelSpacing[] = sliceGeometries[0].getVoxelSpacingArray();
										if (voxelSpacing != null && voxelSpacing.length == 3) {
											if (formatter == null) {
												formatter = new DecimalFormat("###.#");
											}
											value=formatter.format(voxelSpacing[2]);
										}
									}
								}
							}
							else if (le.specialFunction == FRAMENUMBER) {
								value = Integer.toString(f+1);
							}
							else if (le.specialFunction == LOSSYCOMPRESSED) {
								value = LossyImageCompression.describeLossyCompression(list);
							}
							else if (le.specialFunction == CALLFUNCTION) {
								value = getValueByCallingFunction(list);
							}
							else if (le.tag == null) {
								value = le.text;
							}
							else {
								Attribute a = null;
								AttributeTag lookingFor = le.nestedAttribute == null ? le.tag : le.nestedAttribute;
								// look first into functional groups
								if (le.functionalGroup != null && sharedFunctionalGroupsSequence != null && perFrameFunctionalGroupsSequence != null) {
									SequenceAttribute sa = null;
									//SequenceAttribute sa = (SequenceAttribute)(sharedFunctionalGroupsSequence.getItem(0).getAttributeList().get(le.functionalGroup));
									{
										SequenceItem item = sharedFunctionalGroupsSequence.getItem(0);
										if (item != null) {
											AttributeList al = item.getAttributeList();
											if (al != null) {
												sa = (SequenceAttribute)(al.get(le.functionalGroup));
											}
										}
									}
									if (sa == null) {
										//sa = (SequenceAttribute)(perFrameFunctionalGroupsSequence.getItem(f).getAttributeList().get(le.functionalGroup));
										SequenceItem item = perFrameFunctionalGroupsSequence.getItem(f);
										if (item != null) {
											AttributeList al = item.getAttributeList();
											if (al != null) {
												sa = (SequenceAttribute)(al.get(le.functionalGroup));
											}
										}
									}
									if (sa != null) {
										a = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(sa,lookingFor);
									}
								}
								if (a == null) {
									// was not in a functional group ... check top level data set
									 a = list.get(lookingFor);
								}
								if (a != null) {
//System.err.println("Looking for and found "+a.getTag());
									if (le.nestedAttribute != null) {
//System.err.println("Want nestedAttribute "+le.nestedAttribute);
										if (le.nestedAttribute.equals(a.getTag()) && a instanceof SequenceAttribute) {
//System.err.println("Is the nestedAttribute; now looking for "+le.tag);
											SequenceAttribute sa = (SequenceAttribute)a;
											//a = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(sa,le.tag); // could return null
											// concatenate possibly multiple sequence item values
											Iterator sitems = sa.iterator();
											while (sitems.hasNext()) {
												SequenceItem sitem = (SequenceItem)sitems.next();
												AttributeList slist = sitem.getAttributeList();
												if (slist != null) {
													a=slist.get(le.tag);
													if (a != null) {
//System.err.println("Looking for and found within item of nestedAttribute "+a.getTag());
														String nextValue = getFormattedValue(a,le.specialFunction,formatter);
														if (nextValue != null && nextValue.length() > 0) {
															value = value == null || value.length() == 0 ?  nextValue : (value + multipleCodeMeaningDelimiter + nextValue);
														}
													}
												}
											}
											a=null;		// done, so stop further actions
										}
										else {
//System.err.println("Was not a sequence attribute");
											a = null;	// something went wrong so give up
										}
									}
									if (a != null) {
										value=getFormattedValue(a,le.specialFunction,formatter);
									}
								}
							}
							if (value != null && value.length() > 0) {
								if (le.specialFunction == TEXTIFYESNO) {
									value = value.toUpperCase(java.util.Locale.US).indexOf("Y") == -1 ? "" : le.text;
								}
								AnnotationComponentEntry ace = new AnnotationComponentEntry(value,le.textRow,le.orderInRow);
								set[getArrayIndexFromLeftTop(le.fromLeft,le.fromTop)].add(ace);
							}
						}
					}
					
					Vector thisFramesAnnotations = annotations[f];
					
					for (int s=0; s<4; ++s) {
						boolean isLeft = isLeftFromArrayIndex(s);
						isLeft = (isLeft && !swapLeftRight) || (!isLeft && swapLeftRight);	// there is no XOR operator in Java :(
						boolean isTop  = isTopFromArrayIndex(s);
						int lastRow = -1;
						StringBuffer lastValue = null;
						Iterator ia = set[s].iterator();
						while (ia.hasNext()) {
							AnnotationComponentEntry ace = (AnnotationComponentEntry)ia.next();
							if (ace.textRow != lastRow) {
								if (lastValue != null) {
									thisFramesAnnotations.add(new TextAnnotationPositioned(lastValue.toString(),isLeft,isTop,lastRow));
								}
								lastValue = new StringBuffer();
							}
							lastValue.append(ace.string);		// assume already ordered by orderInRow; should be since TreeSet is sorted
							lastRow=ace.textRow;
						}
						if (lastValue != null) {
							thisFramesAnnotations.add(new TextAnnotationPositioned(lastValue.toString(),isLeft,isTop,lastRow));
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>Return a string value from the supplied list of attributes.</p>
	 *
	 * <p>Overridden by children of this class when specific functionality is needed.</p>
	 *
	 * @param	list	ignored, unless the method is overriden in a child class
	 * @return			a string value, of zero length unless the method is overriden in a child class
	 */
	protected String getValueByCallingFunction(AttributeList list) {
		return "";
	}

	/**
	 * <p>Get the annotations for the selected frame.</p>
	 *
	 * @param	frameIndex	which frame
	 * @return			an iterator of annotations of {@link com.pixelmed.display.TextAnnotationPositioned TextAnnotationPositioned}
	 */
	public Iterator iterator(int frameIndex) { return annotations[frameIndex].iterator(); }
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.anatproc.CodedConcept;
import com.pixelmed.anatproc.DisplayableAnatomicConcept;
import com.pixelmed.anatproc.DisplayableLateralityConcept;
import com.pixelmed.anatproc.DisplayableViewConcept;
import com.pixelmed.anatproc.ProjectionXRayAnatomy;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class ProjectionXRayImageLibrary extends ImageLibrary {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ProjectionXRayImageLibrary.java,v 1.7 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageLibrary.class);
	
	public static class ProjectionXRayImageLibraryEntry extends ImageLibrary.ImageLibraryEntry {
	
		protected CodedSequenceItem anatomicalStructure;
		protected CodedSequenceItem imageLaterality;
		protected CodedSequenceItem imageView;
		protected CodedSequenceItem imageViewModifier;
		protected String patientOrientationRow;
		protected String patientOrientationColumn;
		protected String studyDate;
		protected String studyTime;
		protected String contentDate;
		protected String contentTime;
		protected String horizontalPixelSpacingInMM;
		protected String verticalPixelSpacingInMM;
		protected String positionerPrimaryAngleInDegrees;
		protected String positionerSecondaryAngleInDegrees;
		
		public CodedSequenceItem getImageLaterality() { return imageLaterality; }
		public void setImageLaterality(CodedSequenceItem imageLaterality) { this.imageLaterality = imageLaterality; }
			
		public ProjectionXRayImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {					
			CodedSequenceItem anatomicalStructure = null;
			CodedSequenceItem imageLaterality = null;
			CodedSequenceItem imageView = null;
			CodedSequenceItem imageViewModifier = null;
			{
				ContentItem anatomicalStructureContentItem = imageContentItem.getNamedChild("SRT","T-D0005");
				if (anatomicalStructureContentItem != null && anatomicalStructureContentItem instanceof	ContentItemFactory.CodeContentItem) {
					anatomicalStructure = ((ContentItemFactory.CodeContentItem)anatomicalStructureContentItem).getConceptCode();
				}
			}
			{
				ContentItem imageLateralityContentItem = imageContentItem.getNamedChild("DCM","111027");
				if (imageLateralityContentItem != null && imageLateralityContentItem instanceof	ContentItemFactory.CodeContentItem) {
					imageLaterality = ((ContentItemFactory.CodeContentItem)imageLateralityContentItem).getConceptCode();
				}
			}
			{
				ContentItem imageViewContentItem = imageContentItem.getNamedChild("DCM","111031");
				if (imageViewContentItem != null && imageViewContentItem instanceof	ContentItemFactory.CodeContentItem) {
					imageView = ((ContentItemFactory.CodeContentItem)imageViewContentItem).getConceptCode();
					{
						ContentItem imageViewModifierContentItem = imageContentItem.getNamedChild("DCM","111032");
						if (imageViewModifierContentItem != null && imageViewModifierContentItem instanceof	ContentItemFactory.CodeContentItem) {
							imageViewModifier = ((ContentItemFactory.CodeContentItem)imageViewModifierContentItem).getConceptCode();
						}
					}
				}
			}

			String patientOrientationRow             = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111044");
			String patientOrientationColumn          = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111043");
			String studyDate                         = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111060");
			String studyTime                         = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111061");
			String contentDate                       = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111018");
			String contentTime                       = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111019");
			String horizontalPixelSpacingInMM        = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111026");		// should check units are mm not um :(
			String verticalPixelSpacingInMM          = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111066");		// should check units are mm not um :(
			String positionerPrimaryAngleInDegrees   = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","112011");		// should check units are deg :(
			String positionerSecondaryAngleInDegrees = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","112012");		// should check units are deg :(
		
			String sopInstanceUID = imageContentItem.getReferencedSOPInstanceUID();
			String sopClassUID = imageContentItem.getReferencedSOPClassUID();
			HierarchicalSOPInstanceReference hierarchicalSOPInstanceReference = hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID.get(sopInstanceUID);
			HierarchicalImageReference hierarchicalImageReference = null;
			if (hierarchicalSOPInstanceReference == null) {
				// this should never happen, but just in case ...
				hierarchicalImageReference = new HierarchicalImageReference(null/*studyInstanceUID*/,null/*seriesInstanceUID*/,sopInstanceUID,sopClassUID);
			}
			else {
				hierarchicalImageReference = new HierarchicalImageReference(hierarchicalSOPInstanceReference);
			}
			constructProjectionXRayImageLibraryEntry(hierarchicalImageReference,anatomicalStructure,imageLaterality,imageView,imageViewModifier,
					patientOrientationRow,patientOrientationColumn,studyDate,studyTime,contentDate,contentTime,
					horizontalPixelSpacingInMM,verticalPixelSpacingInMM,positionerPrimaryAngleInDegrees,positionerSecondaryAngleInDegrees);
		}
		
		public ProjectionXRayImageLibraryEntry(AttributeList list) throws DicomException {
			String     studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
			String    seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
			String       sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
			String          sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
			
			CodedSequenceItem anatomicalStructure = null;
			CodedSequenceItem imageLaterality = null;
			CodedSequenceItem imageView = null;
			CodedSequenceItem imageViewModifier = null;
			{
				DisplayableAnatomicConcept      anatomy = ProjectionXRayAnatomy.findAnatomicConcept(list);
				DisplayableLateralityConcept laterality = ProjectionXRayAnatomy.findLaterality(list,anatomy);
				DisplayableViewConcept      viewConcept = ProjectionXRayAnatomy.findView(list);
			
				if (anatomy != null) {
					anatomicalStructure = anatomy.getCodedSequenceItem();
				}
				if (laterality != null) {
					imageLaterality = laterality.getCodedSequenceItem();	// should really map general laterality code to those specific for breast for Mammo CAD :(
				}
				if (viewConcept != null) {
					imageView = viewConcept.getCodedSequenceItem();
					// should really populate modifier
				}
			}
			
			String patientOrientationRow = null;
			String patientOrientationColumn = null;
			{
				String[] patientOrientation = Attribute.getStringValues(list,TagFromName.PatientOrientation);
				if (patientOrientation != null && patientOrientation.length == 2) {
					patientOrientationRow = patientOrientation[0];
					patientOrientationColumn = patientOrientation[1];
				}
			}
			
			String studyDate = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate);
			String studyTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			String contentDate = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContentDate);
			String contentTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContentTime);
			
			String horizontalPixelSpacingInMM = null;
			String verticalPixelSpacingInMM = null;
			// prefer PixelSpacing over ImagerPixelSpacing if both present, since may be "calibrated"
			{
				String[] pixelSpacing = Attribute.getStringValues(list,TagFromName.PixelSpacing);
				if (pixelSpacing != null && pixelSpacing.length == 2) {
					horizontalPixelSpacingInMM = pixelSpacing[0];
					verticalPixelSpacingInMM = pixelSpacing[1];
				}
				else {
					String[] imagerPixelSpacing = Attribute.getStringValues(list,TagFromName.ImagerPixelSpacing);
					if (imagerPixelSpacing != null && imagerPixelSpacing.length == 2) {
						horizontalPixelSpacingInMM = imagerPixelSpacing[0];
						verticalPixelSpacingInMM = imagerPixelSpacing[1];
					}
				}
			}
			
			String positionerPrimaryAngleInDegrees = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PositionerPrimaryAngle);
			String positionerSecondaryAngleInDegrees = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PositionerSecondaryAngle);

			if (studyInstanceUID.length() > 0
			 && seriesInstanceUID.length() > 0
			 && sopInstanceUID.length() > 0
			 && sopClassUID.length() > 0
			) {
				HierarchicalImageReference hierarchicalImageReference = new HierarchicalImageReference(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID);
				constructProjectionXRayImageLibraryEntry(hierarchicalImageReference,
					anatomicalStructure,imageLaterality,imageView,imageViewModifier,
					patientOrientationRow,patientOrientationColumn,studyDate,studyTime,contentDate,contentTime,
					horizontalPixelSpacingInMM,verticalPixelSpacingInMM,positionerPrimaryAngleInDegrees,positionerSecondaryAngleInDegrees);
			}
		}

		public ProjectionXRayImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference,
					CodedSequenceItem anatomicalStructure,CodedSequenceItem imageLaterality,CodedSequenceItem imageView,CodedSequenceItem imageViewModifier,
					String patientOrientationRow,String patientOrientationColumn,String studyDate,String studyTime,String contentDate,String contentTime,
					String horizontalPixelSpacingInMM,String verticalPixelSpacingInMM,String positionerPrimaryAngleInDegrees,String positionerSecondaryAngleInDegrees) {
			constructProjectionXRayImageLibraryEntry(hierarchicalImageReference,
					anatomicalStructure,imageLaterality,imageView,imageViewModifier,
					patientOrientationRow,patientOrientationColumn,studyDate,studyTime,contentDate,contentTime,
					horizontalPixelSpacingInMM,verticalPixelSpacingInMM,positionerPrimaryAngleInDegrees,positionerSecondaryAngleInDegrees);
		}
		
		protected void constructProjectionXRayImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference,
					CodedSequenceItem anatomicalStructure,CodedSequenceItem imageLaterality,CodedSequenceItem imageView,CodedSequenceItem imageViewModifier,
					String patientOrientationRow,String patientOrientationColumn,String studyDate,String studyTime,String contentDate,String contentTime,
					String horizontalPixelSpacingInMM,String verticalPixelSpacingInMM,String positionerPrimaryAngleInDegrees,String positionerSecondaryAngleInDegrees) {
			super.constructImageLibraryEntry(hierarchicalImageReference);
			this.anatomicalStructure = anatomicalStructure;
			this.imageLaterality = imageLaterality;
			this.imageView = imageView;
			this.imageViewModifier = imageViewModifier;
			this.patientOrientationRow = patientOrientationRow;
			this.patientOrientationColumn = patientOrientationColumn;
			this.studyDate = studyDate;
			this.studyTime = studyTime;
			this.contentDate = contentDate;
			this.contentTime = contentTime;
			this.horizontalPixelSpacingInMM = horizontalPixelSpacingInMM;
			this.verticalPixelSpacingInMM = verticalPixelSpacingInMM;
			this.positionerPrimaryAngleInDegrees = positionerPrimaryAngleInDegrees;
			this.positionerSecondaryAngleInDegrees = positionerSecondaryAngleInDegrees;
		}

		public ContentItem getImageContentItem(ContentItemFactory cif,ContentItem parent) throws DicomException {
			ContentItem image = cif.new ImageContentItem(parent,"CONTAINS",null/*conceptName*/,
				hierarchicalImageReference.getSOPClassUID(),hierarchicalImageReference.getSOPInstanceUID(),
				0/*referencedFrameNumber*/,0/*referencedSegmentNumber*/,
				null/*presentationStateSOPClassUID*/,null/*presentationStateSOPInstanceUID*/,
				null/*realWorldValueMappingSOPClassUID*/,null/*realWorldValueMappingSOPInstanceUID*/);

			if (anatomicalStructure != null) {
				cif.new CodeContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("T-D0005","SRT","Anatomical structure"),anatomicalStructure);
			}
			if (imageLaterality != null) {
				cif.new CodeContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111027","DCM","Image Laterality"),imageLaterality);
			}
			if (imageView != null) {
				ContentItem imageViewContentItem = cif.new CodeContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111031","DCM","Image View"),imageView);
				if (imageViewModifier != null) {
					cif.new CodeContentItem(imageViewContentItem,"HAS CONCEPT MOD",new CodedSequenceItem("111032","DCM","Image View Modifier"),imageViewModifier);
				}
			}

			if (patientOrientationRow != null && patientOrientationRow.length() > 0) {
				cif.new TextContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111044","DCM","Patient Orientation Row"),patientOrientationRow);
			}
			if (patientOrientationColumn != null && patientOrientationColumn.length() > 0) {
				cif.new TextContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111043","DCM","Patient Orientation Column"),patientOrientationColumn);
			}
			if (studyDate != null && studyDate.length() > 0) {
				cif.new DateContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111060","DCM","Study Date"),studyDate);
			}
			if (studyTime != null && studyTime.length() > 0) {
				cif.new TimeContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111061","DCM","Study Time"),studyTime);
			}
			if (contentDate != null && contentDate.length() > 0) {
				cif.new DateContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111018","DCM","Content Date"),contentDate);
			}
			if (contentTime != null && contentTime.length() > 0) {
				cif.new TimeContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111019","DCM","Content Time"),contentTime);
			}
			if (horizontalPixelSpacingInMM != null && horizontalPixelSpacingInMM.length() > 0) {
				cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111026","DCM","Horizontal Pixel Spacing"),horizontalPixelSpacingInMM,new CodedSequenceItem("mm","UCUM","mm"));	// mm not um per CP 1266
			}
			if (verticalPixelSpacingInMM != null && verticalPixelSpacingInMM.length() > 0) {
				cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111066","DCM","Vertical Pixel Spacing"),verticalPixelSpacingInMM,new CodedSequenceItem("mm","UCUM","mm"));	// mm not um per CP 1266
			}
			if (positionerPrimaryAngleInDegrees != null && positionerPrimaryAngleInDegrees.length() > 0) {
				cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("112011","DCM","Positioner Primary Angle"),positionerPrimaryAngleInDegrees,new CodedSequenceItem("deg","UCUM","deg"));
			}
			if (positionerSecondaryAngleInDegrees != null && positionerSecondaryAngleInDegrees.length() > 0) {
				cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("112012","DCM","Positioner Secondary Angle"),positionerSecondaryAngleInDegrees,new CodedSequenceItem("deg","UCUM","deg"));
			}

			return image;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("Entry:\n");
			buf.append(hierarchicalImageReference);
			buf.append("\n");
			
			buf.append("Anatomical structure = ");
			buf.append(anatomicalStructure);
			buf.append("\n");
			
			buf.append("Image Laterality = ");
			buf.append(imageLaterality);
			buf.append("\n");
			
			buf.append("Image View = ");
			buf.append(imageView);
			buf.append("\n");
			
			buf.append("Image View Modifier = ");
			buf.append(imageViewModifier);
			buf.append("\n");
			
			buf.append("Patient Orientation Row = ");
			buf.append(patientOrientationRow);
			buf.append("\n");

			buf.append("Patient Orientation Column = ");
			buf.append(patientOrientationColumn);
			buf.append("\n");

			buf.append("Study Date = ");
			buf.append(studyDate);
			buf.append("\n");

			buf.append("Study Time = ");
			buf.append(studyTime);
			buf.append("\n");

			buf.append("Content Date = ");
			buf.append(contentDate);
			buf.append("\n");

			buf.append("Content Time = ");
			buf.append(contentTime);
			buf.append("\n");

			buf.append("Horizontal Pixel Spacing = ");
			buf.append(horizontalPixelSpacingInMM);
			buf.append(" mm\n");

			buf.append("Vertical Pixel Spacing = ");
			buf.append(verticalPixelSpacingInMM);
			buf.append(" mm\n");

			buf.append("Positioner Primary Angle = ");
			buf.append(positionerPrimaryAngleInDegrees);
			buf.append(" deg\n");

			buf.append("Positioner Secondary Angle = ");
			buf.append(positionerSecondaryAngleInDegrees);
			buf.append(" deg\n");

			return buf.toString();
		}
		
	}

	// these factory methods CANNOT BE STATIC if we want them to override the parent class methods, which is the whole point ...
	
	public ImageLibraryEntry makeImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {
//System.err.println("ProjectionXRayImageLibraryEntry.makeImageLibraryEntry(ContentItemFactory.ImageContentItem,Map<String,HierarchicalSOPInstanceReference>)");
		return new ProjectionXRayImageLibraryEntry(imageContentItem,hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID);
	}

	public ImageLibraryEntry makeImageLibraryEntry(AttributeList list) throws DicomException {
//System.err.println("ProjectionXRayImageLibraryEntry.makeImageLibraryEntry(AttributeList)");
		return new ProjectionXRayImageLibraryEntry(list);
	}
	
	// should provide accessor methods for each of the entry values to mimic CrossSectionalImageLibrary :(
	
	// We do need to provide the following sub-class-specific constructors and methods
	// even though they are identical to the parent methods ...
	
	public ProjectionXRayImageLibrary() {
	}
	
	public ProjectionXRayImageLibrary(AttributeList list) throws DicomException {
		StructuredReport sr = new StructuredReport(list);
		ContentItem imageLibraryContainer = findImageLibraryContainer((ContentItem)(sr.getRoot()));
		if (imageLibraryContainer != null) {
			constructImageLibrary(imageLibraryContainer,list);	// uses our overridden makeImageLibraryEntry() method to create ProjectionXRayImageLibrary entries rather than parent ImageLibrary entries
		}
		else {
			throw new DicomException("No Image Library CONTAINER content item in SR");
		}
	}
		
	public ProjectionXRayImageLibrary(ContentItem imageLibraryContainer,AttributeList list) {
		constructImageLibrary(imageLibraryContainer,list);	// uses our overridden makeImageLibraryEntry() method to create ProjectionXRayImageLibrary entries rather than parent ImageLibrary entries
	}
	
	public static ProjectionXRayImageLibrary read(String filename) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		list.read(filename);
		return new ProjectionXRayImageLibrary(list);
	}
						
	public ProjectionXRayImageLibrary(Set<File> files) throws IOException, DicomException {
		super(files);	// uses our overridden makeImageLibraryEntry() method to create ProjectionXRayImageLibrary entries rather than parent ImageLibrary entries
	}
	

	/**
	 * <p>Create an SR Image Library from a bunch of cross-sectional DICOM instances.</p>
	 *
	 * <p>Adds a new series (instance UID) to the existing study (instance UID).</p>
	 *
	 * @param	arg	the path for the SR Image Library output, then the filenames and/or folder names of files containing the input image files
	 */
	public static void main(String arg[]) {
		try {
			String outputPath = arg[0];
			Set<File> inputFiles = new HashSet<File>();
			for (int i=1; i<arg.length; ++i) {
				Collection<File> more = FileUtilities.listFilesRecursively(new File(arg[i]));
				inputFiles.addAll(more);
			}
			ProjectionXRayImageLibrary library = new ProjectionXRayImageLibrary(inputFiles);
//System.err.println("ProjectionXRayImageLibrary.main(): Result");
//System.err.println(library.toString());
			library.write(outputPath);
			
			// test round trip
			//{
			//	ProjectionXRayImageLibrary roundTrip = read(outputPath);
			//	System.err.println("RoundTrip =\n"+roundTrip);	// no need to use SLF4J since command line utility/test
			//}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

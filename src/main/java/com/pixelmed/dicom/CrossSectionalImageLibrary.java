/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.geometry.GeometryOfSlice;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Tuple3d;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class CrossSectionalImageLibrary extends ImageLibrary {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CrossSectionalImageLibrary.java,v 1.10 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageLibrary.class);
	
	public static class CrossSectionalImageLibraryEntry extends ImageLibrary.ImageLibraryEntry {
	
		protected String frameOfReferenceUID;
		protected GeometryOfSlice geometry;
		
		public CrossSectionalImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {					
			// should check units are mm for all of these :(
			String          horizontalPixelSpacing = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111026");
			String            verticalPixelSpacing = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","111066");
			//String          spacingBetweenSlices = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","112226");
			String                  sliceThickness = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","112225");
			String             frameOfReferenceUID = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","112227");
			String           imagePositionPatientX = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110901");
			String           imagePositionPatientY = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110902");
			String           imagePositionPatientZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110903");
			String     imageOrientationPatientRowX = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110904");
			String     imageOrientationPatientRowY = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110905");
			String     imageOrientationPatientRowZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110906");
			String  imageOrientationPatientColumnX = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110907");
			String  imageOrientationPatientColumnY = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110908");
			String  imageOrientationPatientColumnZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110909");
			String                            rows = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110910");
			String                         columns = imageContentItem.getSingleStringValueOrNullOfNamedChild("DCM","110911");

			// check for proprietary codes used before CP 1266 was added, in case an existing image library ... should factor out as a separate "equivalent code handling" class :(
			
			if (imagePositionPatientX == null) { imagePositionPatientX = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250001"); }
			if (imagePositionPatientY == null) { imagePositionPatientY = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250002"); }
			if (imagePositionPatientZ == null) { imagePositionPatientZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250003"); }
			if (imageOrientationPatientRowX == null) { imageOrientationPatientRowX = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250004"); }
			if (imageOrientationPatientRowY == null) { imageOrientationPatientRowY = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250005"); }
			if (imageOrientationPatientRowZ == null) { imageOrientationPatientRowZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250006"); }
			if (imageOrientationPatientColumnX == null) { imageOrientationPatientColumnX = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250007"); }
			if (imageOrientationPatientColumnY == null) { imageOrientationPatientColumnY = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250008"); }
			if (imageOrientationPatientColumnZ == null) { imageOrientationPatientColumnZ = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250009"); }
			if (rows == null) { rows = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250010"); }
			if (columns == null) { columns = imageContentItem.getSingleStringValueOrNullOfNamedChild("99PMP","250011"); }

			double[]         tlhcArray = { parseDoubleElseZero(         imagePositionPatientX),parseDoubleElseZero(         imagePositionPatientY),parseDoubleElseZero(         imagePositionPatientZ) };
			double[]          rowArray = { parseDoubleElseZero(   imageOrientationPatientRowX),parseDoubleElseZero(   imageOrientationPatientRowY),parseDoubleElseZero(   imageOrientationPatientRowZ) };
			double[]       columnArray = { parseDoubleElseZero(imageOrientationPatientColumnX),parseDoubleElseZero(imageOrientationPatientColumnY),parseDoubleElseZero(imageOrientationPatientColumnZ) };
			double[] voxelSpacingArray = { parseDoubleElseZero(          verticalPixelSpacing),parseDoubleElseZero(        horizontalPixelSpacing),0d };
			double[]        dimensions = { parseDoubleElseZero(                          rows),parseDoubleElseZero(                       columns),1d };
					
			GeometryOfSlice geometry = new GeometryOfSlice(rowArray,columnArray,tlhcArray,voxelSpacingArray,parseDoubleElseZero(sliceThickness),dimensions);
					
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
			constructCrossSectionalImageLibraryEntry(hierarchicalImageReference,frameOfReferenceUID,geometry);
		}

		public CrossSectionalImageLibraryEntry(AttributeList list) throws DicomException {
			String     studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
			String    seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
			String       sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
			String          sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
			String  frameOfReferenceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.FrameOfReferenceUID);

			// should switch to using GeometryOfVolume etc., but for now just do single frame classic objects ... :(
		
			GeometryOfSlice geometry = new GeometryOfSliceFromAttributeList(list);
		
			if (frameOfReferenceUID.length() > 0
			 && studyInstanceUID.length() > 0
			 && seriesInstanceUID.length() > 0
			 && sopInstanceUID.length() > 0
			 && sopClassUID.length() > 0
			) {
				HierarchicalImageReference hierarchicalImageReference = new HierarchicalImageReference(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID);
				constructCrossSectionalImageLibraryEntry(hierarchicalImageReference,frameOfReferenceUID,geometry);
			}
		}

		public CrossSectionalImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference,String frameOfReferenceUID,GeometryOfSlice geometry) {
			constructCrossSectionalImageLibraryEntry(hierarchicalImageReference,frameOfReferenceUID,geometry);
		}
		
		protected void constructCrossSectionalImageLibraryEntry(HierarchicalImageReference hierarchicalImageReference,String frameOfReferenceUID,GeometryOfSlice geometry) {
			super.constructImageLibraryEntry(hierarchicalImageReference);
			this.frameOfReferenceUID = frameOfReferenceUID;
			this.geometry = geometry;
		}

		public ContentItem getImageContentItem(ContentItemFactory cif,ContentItem parent) throws DicomException {
			ContentItem image = cif.new ImageContentItem(parent,"CONTAINS",null/*conceptName*/,
				hierarchicalImageReference.getSOPClassUID(),hierarchicalImageReference.getSOPInstanceUID(),
				0/*referencedFrameNumber*/,0/*referencedSegmentNumber*/,
				null/*presentationStateSOPClassUID*/,null/*presentationStateSOPInstanceUID*/,
				null/*realWorldValueMappingSOPClassUID*/,null/*realWorldValueMappingSOPInstanceUID*/);

			if (geometry != null) {
				double[] spacing = geometry.getVoxelSpacingArray();
				if (spacing != null && spacing.length == 3) {
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111026","DCM","Horizontal Pixel Spacing"),spacing[1],new CodedSequenceItem("mm","UCUM","millimeter"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("111066","DCM","Vertical Pixel Spacing"),  spacing[0],new CodedSequenceItem("mm","UCUM","millimeter"));
					// NB. GeometryOfSliceFromAttributeList uses 0 (NOT SpacingBetweenSlices), unless filled in later by GeometryOfVolume
					// (112226, DCM, "Spacing between slices") :(
				}
				cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("112225","DCM","Slice Thickness"),geometry.getSliceThickness(),new CodedSequenceItem("mm","UCUM","millimeter"));
			}
				
			if (frameOfReferenceUID != null && frameOfReferenceUID.trim().length() > 0) {
				cif.new UIDContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("112227","DCM","Frame of Reference UID"),frameOfReferenceUID);
			}
				
			if (geometry != null) {
				double[] origin = geometry.getTLHCArray();
				if (origin != null && origin.length == 3) {
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110901","DCM","Image Position (Patient) X"),origin[0],new CodedSequenceItem("mm","UCUM","millimeter"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110902","DCM","Image Position (Patient) Y"),origin[1],new CodedSequenceItem("mm","UCUM","millimeter"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110903","DCM","Image Position (Patient) Z"),origin[2],new CodedSequenceItem("mm","UCUM","millimeter"));
				}
				double[] row = geometry.getRowArray();
				if (row != null && row.length == 3) {
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110904","DCM","Image Orientation (Patient) Row X"),row[0],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110905","DCM","Image Orientation (Patient) Row Y"),row[1],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110906","DCM","Image Orientation (Patient) Row Z"),row[2],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
				}
				double[] column = geometry.getColumnArray();
				if (column != null && column.length == 3) {
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110907","DCM","Image Orientation (Patient) Column X"),column[0],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110908","DCM","Image Orientation (Patient) Column Y"),column[1],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110909","DCM","Image Orientation (Patient) Column Z"),column[2],new CodedSequenceItem("{-1:1}","UCUM","{-1:1}"));
				}
				Tuple3d dimensions = geometry.getDimensions();
				if (dimensions != null) {
					double[] dimensionsArray = new double[3];
					dimensions.get(dimensionsArray);
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110910","DCM","Pixel Data Rows"),   (int)(dimensionsArray[0]),new CodedSequenceItem("{pixels}","UCUM","pixels"));
					cif.new NumericContentItem(image,"HAS ACQ CONTEXT",new CodedSequenceItem("110911","DCM","Pixel Data Columns"),(int)(dimensionsArray[1]),new CodedSequenceItem("{pixels}","UCUM","pixels"));
				}
			}
			return image;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("Entry:\n");
			
			buf.append(hierarchicalImageReference);
			buf.append("\n");
			
			buf.append("Frame of Reference UID = ");
			buf.append(frameOfReferenceUID);
			buf.append("\n");
			
			buf.append(geometry);
			buf.append("\n");
			
			return buf.toString();
		}
		
	}

	// these factory methods CANNOT BE STATIC if we want them to override the parent class methods, which is the whole point ...
	
	public ImageLibraryEntry makeImageLibraryEntry(ContentItemFactory.ImageContentItem imageContentItem,Map<String,HierarchicalSOPInstanceReference> hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID) {
//System.err.println("CrossSectionalImageLibraryEntry.makeImageLibraryEntry(ContentItemFactory.ImageContentItem,Map<String,HierarchicalSOPInstanceReference>)");
		return new CrossSectionalImageLibraryEntry(imageContentItem,hierarchicalSOPInstanceReferencesIndexedBySOPInstanceUID);
	}

	public ImageLibraryEntry makeImageLibraryEntry(AttributeList list) throws DicomException {
//System.err.println("CrossSectionalImageLibraryEntry.makeImageLibraryEntry(AttributeList)");
		return new CrossSectionalImageLibraryEntry(list);
	}
		
	public String getSOPClassUID(String sopInstanceUID) {
		String value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null && entry.hierarchicalImageReference != null) {
			value = entry.hierarchicalImageReference.sopClassUID;
		}
		return value;
	}
		
	public String getFrameOfReferenceUID(String sopInstanceUID) {
		String value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			value = entry.frameOfReferenceUID;
		}
		return value;
	}
	
	public double[] getVoxelSpacingArray(String sopInstanceUID) {
		double[] value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			GeometryOfSlice geometry = entry.geometry;
			if (geometry != null) {
				value = geometry.getVoxelSpacingArray();
			}
		}
		return value;
	}
	
	public double[] getTLHCArray(String sopInstanceUID) {
		double[] value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			GeometryOfSlice geometry = entry.geometry;
			if (geometry != null) {
				value = geometry.getTLHCArray();
			}
		}
		return value;
	}
	
	public double[] getRowArray(String sopInstanceUID) {
		double[] value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			GeometryOfSlice geometry = entry.geometry;
			if (geometry != null) {
				value = geometry.getRowArray();
			}
		}
		return value;
	}
	
	public double[] getColumnArray(String sopInstanceUID) {
		double[] value = null;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			GeometryOfSlice geometry = entry.geometry;
			if (geometry != null) {
				value = geometry.getColumnArray();
			}
		}
		return value;
	}
	
	public double getSliceThickness(String sopInstanceUID) {
		double value = 0d;
		CrossSectionalImageLibraryEntry entry = (CrossSectionalImageLibraryEntry)(entriesIndexedBySOPInstanceUID.get(sopInstanceUID));
		if (entry != null) {
			GeometryOfSlice geometry = entry.geometry;
			if (geometry != null) {
				value = geometry.getSliceThickness();
			}
		}
		return value;
	}
	
	public static final double parseDoubleElseZero(String s) {
		double value = 0d;
		if (s != null && s.length() > 0) {
			try {
				value = Double.parseDouble(s);
			}
			catch (NumberFormatException e) {
			}
		}
		return value;
	}
	
	// We do need to provide the following sub-class-specific constructors and methods
	// even though they are identical to the parent methods ...
	
	public CrossSectionalImageLibrary() {
	}
	
	public CrossSectionalImageLibrary(AttributeList list) throws DicomException {
		StructuredReport sr = new StructuredReport(list);
		ContentItem imageLibraryContainer = findImageLibraryContainer((ContentItem)(sr.getRoot()));
		if (imageLibraryContainer != null) {
			constructImageLibrary(imageLibraryContainer,list);	// uses our overridden makeImageLibraryEntry() method to create CrossSectionalImageLibrary entries rather than parent ImageLibrary entries
		}
		else {
			throw new DicomException("No Image Library CONTAINER content item in SR");
		}
	}
		
	public CrossSectionalImageLibrary(ContentItem imageLibraryContainer,AttributeList list) {
		constructImageLibrary(imageLibraryContainer,list);	// uses our overridden makeImageLibraryEntry() method to create CrossSectionalImageLibrary entries rather than parent ImageLibrary entries
	}
	
	public static CrossSectionalImageLibrary read(String filename) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		list.read(filename);
		return new CrossSectionalImageLibrary(list);
	}
						
	public CrossSectionalImageLibrary(Set<File> files) throws IOException, DicomException {
		super(files);	// uses our overridden makeImageLibraryEntry() method to create CrossSectionalImageLibrary entries rather than parent ImageLibrary entries
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
			CrossSectionalImageLibrary library = new CrossSectionalImageLibrary(inputFiles);
//System.err.println("CrossSectionalImageLibrary.main(): Result");
//System.err.println(library.toString());
			library.write(outputPath);
			
			// test round trip
			//{
			//	CrossSectionalImageLibrary roundTrip = read(outputPath);
			//	System.err.println("RoundTrip =\n"+roundTrip);
			//}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

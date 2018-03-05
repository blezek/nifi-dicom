/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.File;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

import com.pixelmed.utils.PdfDecoder;
import com.pixelmed.utils.PdfException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting PDF files into a single or multi frame DICOM Secondary Capture image with one frame per page.</p>
 *
 * @author	dclunie
 */

public class PDFToDicomImage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/PDFToDicomImage.java,v 1.9 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(PDFToDicomImage.class);

	/**
	 * <p>Read PDF file, and create a single or multi frame DICOM Secondary Capture image with one frame per page.</p>
	 *
	 * @param	inputFile
	 * @param	outputFile
	 * @param	patientName
	 * @param	patientID
	 * @param	studyID
	 * @param	seriesNumber
	 * @param	instanceNumber
	 * @param	requestedDpi
	 * @throws			DicomException
	 */
	public PDFToDicomImage(String inputFile,String outputFile,String patientName,String patientID,String studyID,String seriesNumber,String instanceNumber,int requestedDpi)
			throws IOException, DicomException, PdfException, Exception {
			
		PdfDecoder pdfDecoder = new PdfDecoder();
		pdfDecoder.useHiResScreenDisplay(true);
		pdfDecoder.openPdfFile(inputFile);
		int numberOfFrames = pdfDecoder.getPageCount();
//System.err.println("PDFToDicomImage.main(): numberOfFrames = "+numberOfFrames);
//System.err.println("PDFToDicomImage.main(): pdfDecoder.dpi = "+pdfDecoder.dpi);
//System.err.println("PDFToDicomImage.main(): requested dpi = "+requestedDpi);
		float scaleFactor = ((float)requestedDpi) / pdfDecoder.getDPI();
//System.err.println("PDFToDicomImage.main(): scaleFactor = "+scaleFactor);
		int commonWidth = 0;
		int commonHeight = 0;
		AttributeList list = null;
		int dstFramePixelsLength = 0;
		byte dstPixels[] = null;
		for (int f=0; f<numberOfFrames; ++f) {
//System.err.println("PDFToDicomImage.main(): frame = "+f);

			//pdfDecoder.decodePage(f+1);
			pdfDecoder.setPageParameters(scaleFactor,f+1);
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getPDFWidth() = "+ pdfDecoder.getPDFWidth());
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getPDFHeight() = "+ pdfDecoder.getPDFHeight());
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getRawPDFWidth() = "+ pdfDecoder.getRawPDFWidth());
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getRawPDFHeight() = "+ pdfDecoder.getRawPDFHeight());
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getPreferredSize().getWidth() = "+ pdfDecoder.getPreferredSize().getWidth());
//System.err.println("PDFToDicomImage.main(): pdfDecoder.getPreferredSize().getHeight() = "+ pdfDecoder.getPreferredSize().getHeight());

			BufferedImage src = pdfDecoder.getPageAsImage(f+1);

			if (src == null) {
				throw new DicomException("Could not get image of page "+(f+1));
			}
			int frameWidth = src.getWidth();
//System.err.println("PDFToDicomImage.main(): frameWidth = "+frameWidth);
			int frameHeight = src.getHeight();
//System.err.println("PDFToDicomImage.main(): frameHeight = "+frameHeight);
			SampleModel srcSampleModel = src.getSampleModel();
//System.err.println("PDFToDicomImage.main(): srcSampleModel = "+srcSampleModel);
			int srcDataType = srcSampleModel.getDataType();
//System.err.println("PDFToDicomImage.main(): srcDataType = "+srcDataType);
			Raster srcRaster = src.getRaster();
			int srcNumBands = srcRaster.getNumBands();
//System.err.println("PDFToDicomImage.main(): srcNumBands = "+srcNumBands);
			
			if (list == null) {
				list = new AttributeList();
				commonWidth = frameWidth;
				commonHeight = frameHeight;

				short rows = (short)frameHeight;
				short columns = (short)frameWidth;

				short bitsAllocated = 0;
				short bitsStored = 0;
				short highBit = 0;
				short samplesPerPixel = 0;
				short pixelRepresentation = 0;
				String photometricInterpretation = null;
				short planarConfiguration = 0;
				String sopClass = null;


				if (srcNumBands == 3) {
					bitsAllocated=8;
					bitsStored=8;
					highBit=7;
					samplesPerPixel=3;
					pixelRepresentation=0;
					photometricInterpretation="RGB";
					planarConfiguration=0;	// by pixel
					sopClass = numberOfFrames > 1 ? SOPClass.MultiframeTrueColorSecondaryCaptureImageStorage : SOPClass.SecondaryCaptureImageStorage;
				}
				else {
					throw new DicomException("Unsupported pixel data form ("+srcNumBands+" bands)");
				}

				dstFramePixelsLength = commonWidth*commonHeight*srcNumBands;
				dstPixels = new byte[numberOfFrames*dstFramePixelsLength];

				{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue(bitsAllocated); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue(bitsStored); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue(highBit); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.Rows); a.addValue(rows); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.Columns); a.addValue(columns); list.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel); a.addValue(samplesPerPixel); list.put(a); }
				{ Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation); a.addValue(pixelRepresentation); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a); }
				if (samplesPerPixel > 1) {
					Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue(planarConfiguration); list.put(a);
				}
			
				// various Type 1 and Type 2 attributes for mandatory SC modules ...
	
				UIDGenerator u = new UIDGenerator();	

				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(sopClass); list.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPInstanceUID); a.addValue(u.getNewSOPInstanceUID(studyID,seriesNumber,instanceNumber)); list.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getNewSeriesInstanceUID(studyID,seriesNumber)); list.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(u.getNewStudyInstanceUID(studyID)); list.put(a); }

				{ Attribute a = new PersonNameAttribute(TagFromName.PatientName); a.addValue(patientName); list.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.PatientID); a.addValue(patientID); list.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.PatientBirthDate); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.PatientSex); list.put(a); }
				{ Attribute a = new ShortStringAttribute(TagFromName.StudyID); a.addValue(studyID); list.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.StudyDate); list.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.StudyTime); list.put(a); }
				{ Attribute a = new PersonNameAttribute(TagFromName.ReferringPhysicianName); a.addValue("^^^^"); list.put(a); }
				{ Attribute a = new ShortStringAttribute(TagFromName.AccessionNumber); list.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.SeriesNumber); a.addValue(seriesNumber); list.put(a); }
				{ Attribute a = new IntegerStringAttribute(TagFromName.InstanceNumber); a.addValue(instanceNumber); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue("OT"); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.ConversionType); a.addValue("WSD"); list.put(a); }
				{ Attribute a = new LongStringAttribute(TagFromName.Manufacturer); list.put(a); }
				{ Attribute a = new CodeStringAttribute(TagFromName.PatientOrientation); list.put(a); }
			}
			else {
				if (commonWidth != frameWidth || commonHeight != frameHeight) {
					throw new DicomException("Pages of different sizes not supported");
				}
			}
				
			DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
			int srcPixels[] = null; // to disambiguate SampleModel.getPixels() method signature
			srcPixels = srcSampleModel.getPixels(0,0,frameWidth,frameHeight,srcPixels,srcDataBuffer);
			int srcPixelsLength = srcPixels.length;
//System.err.println("PDFToDicomImage.main(): srcPixelsLength = "+srcPixelsLength);
//System.err.println("PDFToDicomImage.main(): frameWidth*frameHeight*srcNumBands = "+frameWidth*frameHeight*srcNumBands);

			int dstIndex=f*dstFramePixelsLength;
			for (int srcIndex=0; srcIndex<srcPixelsLength;) {
				dstPixels[dstIndex++]=(byte)(srcPixels[srcIndex++]);
				dstPixels[dstIndex++]=(byte)(srcPixels[srcIndex++]);
				dstPixels[dstIndex++]=(byte)(srcPixels[srcIndex++]);
			}
		}
		pdfDecoder.closePdfFile();
		Attribute pixelData = new OtherByteAttribute(TagFromName.PixelData);
		pixelData.setValues(dstPixels);
		list.put(pixelData);
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
		list.write(outputFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}
	
	/**
	 * <p>Read a PDF file, and create a single or multi frame DICOM Secondary Capture image with one frame per page.</p>
	 *
	 * @param	arg	six parameters, the inputFile, outputFile, patientName, patientID, studyID, seriesNumber, instanceNumber, dots per inch
	 */
	public static void main(String arg[]) {
		try {
			new PDFToDicomImage(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],Integer.parseInt(arg[7]));
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

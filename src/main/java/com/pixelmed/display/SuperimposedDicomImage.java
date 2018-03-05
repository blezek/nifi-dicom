/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.geometry.GeometryOfVolume;

import java.io.IOException;

import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class that supports matching the geometry of a superimposed DICOM image
 * and an underlying images, and creating BufferedImages suitable for
 * drawing on an underlying image.</p>
 *
 * @author	dclunie
 */

public class SuperimposedDicomImage extends SuperimposedImage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SuperimposedDicomImage.java,v 1.8 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SuperimposedDicomImage.class);
	
	/**
	 * @param	list
	 */
	public SuperimposedDicomImage(AttributeList list) throws DicomException {
		// no need to call super(), does nothing
		doCommonConstructorStuff(list);
	}
	
	/**
	 * @param	filename
	 */
	public SuperimposedDicomImage(String filename) throws DicomException, IOException {
		// no need to call super(), does nothing
		AttributeList list = new AttributeList();
		list.read(filename);
		doCommonConstructorStuff(list);
	}
	
	/**
	 * @param	list
	 */
	private void doCommonConstructorStuff(AttributeList list) throws DicomException {
		String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		if (SOPClass.isImageStorage(sopClassUID)) {
//System.err.println("SuperimposedDicomImage.doCommonConstructorStuff(): is an image");
			superimposedSourceImage = new SourceImage(list);
			if (superimposedSourceImage != null && superimposedSourceImage.getNumberOfBufferedImages() > 0) {
//System.err.println("SuperimposedDicomImage.doCommonConstructorStuff(): has a SourceImage and one or more frames");
				superimposedGeometry = new GeometryOfVolumeFromAttributeList(list);
				if (!superimposedGeometry.isVolumeSampledRegularlyAlongFrameDimension()) {
					slf4jlogger.warn("doCommonConstructorStuff(): superimposed geometry is not a single regularly sampled volume");
				}
			}
		}
	}
	
	/**
	 * @param	arg	the underlying image file name, the superimposed image file name, and optionally the file name basis for a consumer format image rendering
	 */
	public static void main(String arg[]) {
		try {
			String underlyingFileName = arg[0];
			String superimposedFileName = arg[1];
			
			SuperimposedImage superimposedImage = new SuperimposedDicomImage(superimposedFileName);
			Vector<SuperimposedImage> superimposedImages =  new Vector<SuperimposedImage>();
			superimposedImages.add(superimposedImage);
			
			if (arg.length > 2) {
				String outputFileName = arg[2];
				ConsumerFormatImageMaker.convertFileToEightBitImage(underlyingFileName,outputFileName,"jpeg",
					0/*windowCenter*/,0/*windowWidth*/,0/*imageWidth*/,0/*imageHeight*/,100/*imageQuality*/,"all_color",
					superimposedImages,null/*arrayOfPerFrameShapes*/);
			}
			else {
				AttributeList underlyingList = new AttributeList();
				underlyingList.read(underlyingFileName);
				SourceImage underlyingSourceImage = new SourceImage(underlyingList);
				GeometryOfVolume underlyingGeometry = new GeometryOfVolumeFromAttributeList(underlyingList);

				SingleImagePanel ip = new SingleImagePanel(underlyingSourceImage,null,underlyingGeometry);
				ip.setSuperimposedImages(superimposedImages);
				javax.swing.JFrame frame = new javax.swing.JFrame();
				frame.add(ip);
				frame.setSize(underlyingSourceImage.getWidth(),underlyingSourceImage.getHeight());
				frame.setVisible(true);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


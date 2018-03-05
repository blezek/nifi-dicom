/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.*;

import java.util.*; 
import java.io.*; 

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class that encapsulates the features and values from an MR spectroscopy source,
 * usually for the purpose of displaying it.</p>
 *
 * @author	dclunie
 */
public class SourceSpectra {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceSpectra.java,v 1.12 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceSpectra.class);

	/***/
	private float[][] spectra;
	/***/
	private int rows;
	/***/
	private int columns;
	/***/
	private int dataPointRows;
	/***/
	private int dataPointColumns;
	/***/
	private int nframes;
	/***/
	private String dataRepresentation;
	/***/
	private int nComponents;
	/***/
	private int whichComponent;
	/***/
	private int valuesPerFrame;
	
	/***/
	private float minimum;
	/***/
	private float maximum;
	
	/***/
	private String title;

	/**
	 * @param	list
	 */
	private static String buildInstanceTitleFromAttributeList(AttributeList list) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientName));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientID));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyID));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstanceNumber));
		buffer.append(":");
		return buffer.toString();
	}

	/**
	 * <p>Construct spectra from a single or multi-frame DICOM spectroscopy object from
	 * an input stream (such as from a file or the network).</p>
	 *
	 * @param	i		the input stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public SourceSpectra(DicomInputStream i) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.read(i);
		if (list.get(TagFromName.SpectroscopyData) != null) constructSourceSpectra(list);
	}

	/**
	 * <p>Construct spectra from a single or multi-frame DICOM spectroscopy object from
	 * a list of DICOM attributes.</p>
	 *
	 * @param	list		the list of attributes that include the description and values of the spectroscopy data
	 * @throws	DicomException
	 */
	public SourceSpectra(AttributeList list) throws DicomException {
		if (list.get(TagFromName.SpectroscopyData) != null) constructSourceSpectra(list);
	}

	/**
	 * @param	list
	 * @throws	DicomException
	 */
	private void constructSourceSpectra(AttributeList list) throws DicomException {
//System.err.println("constructSourceSpectra - start");

		title=buildInstanceTitleFromAttributeList(list);

		           nframes = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
		if (nframes == 0) {		// Siemens bug in MRDCM_VB13A_2.0 - explicitly zero when only 1 frame
			slf4jlogger.warn("constructSourceSpectra(): setting invalid NumberOfFrames value of 0 to 1 instead");
			nframes = 1;
		}
		           columns = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		              rows = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);
		    dataPointRows  = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.DataPointRows,1);
		if (dataPointRows == 0) {		// Siemens bug in MRDCM_VB13A_2.0 - explicitly zero
			slf4jlogger.warn("constructSourceSpectra(): setting invalid DataPointRows value of 0 to 1 instead");
			dataPointRows = 1;
		}
		  dataPointColumns = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.DataPointColumns,0);
		dataRepresentation = Attribute.getSingleStringValueOrDefault(list,TagFromName.DataRepresentation,"REAL");
//System.err.println("dataRepresentation="+dataRepresentation);
		       nComponents = dataRepresentation.equals("COMPLEX") ? 2 : 1;
//System.err.println("nComponents="+nComponents);
		    whichComponent = nComponents > 1 && dataRepresentation.equals("IMAGINARY") ? 1 : 0;
		    valuesPerFrame = columns*rows*dataPointRows*dataPointColumns;	// we only ever actually use one component

		Attribute spectroscopyData = list.get(TagFromName.SpectroscopyData);
		if (spectroscopyData == null) {
			throw new DicomException("Spectroscopy data missing");
		}
		else {
			long expectLength = (long)nframes*columns*rows*dataPointRows*dataPointColumns*nComponents*4;
			long gotLength = spectroscopyData.getVL();
			if (expectLength != gotLength) {
				throw new DicomException("Spectroscopy data wrong length: expected "+expectLength+" but got "+gotLength+" dec bytes");
			}
			float[] floatvalues = spectroscopyData.getFloatValues();
			
			// split spectra into separate frames, and if necessary, components
			
			minimum=Float.MAX_VALUE;
			maximum=Float.MIN_VALUE;

			spectra=new float[nframes][];
			//if (nComponents == 1) {			// would be faster, but need to get statistics
			//	for (int f=0;f<nframes;++f) {
			//		spectra[f]=new float[valuesPerFrame];
			//		System.arraycopy(floatvalues,f*valuesPerFrame,spectra[f],0,valuesPerFrame);
			//	}
			//}
			//else {
				for (int f=0;f<nframes;++f) {
					spectra[f]=new float[valuesPerFrame];
					int p=f*valuesPerFrame*nComponents+whichComponent;
					for (int i=0;i<valuesPerFrame;++i) {
						float value=floatvalues[p];
						p+=nComponents;
						spectra[f][i]=value;
						if (value < minimum) minimum=value;
						if (value > maximum) maximum=value;
					}
				}
			//}
		}
	}

	/***/
	public float[][] getSpectra() { return spectra; }
	/***/
	public String getTitle() { return title; }
	/***/
	public int getNumberOfFrames() { return nframes; }
	/***/
	public int getRows() { return rows; }
	/***/
	public int getColumns() { return columns; }
	/***/
	public int getDataPointRows() { return dataPointRows; }
	/***/
	public int getDataPointColumns() { return dataPointColumns; }
	/***/
	public float getMinimum() { return minimum; }
	/***/
	public float getMaximum() { return maximum; }

}

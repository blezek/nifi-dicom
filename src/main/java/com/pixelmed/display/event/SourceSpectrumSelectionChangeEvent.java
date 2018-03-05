/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.SpectroscopyVolumeLocalization;
import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;
import com.pixelmed.geometry.GeometryOfVolume;

/**
 * @author	dclunie
 */
public class SourceSpectrumSelectionChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/SourceSpectrumSelectionChangeEvent.java,v 1.8 2017/01/24 10:50:42 dclunie Exp $";

	private float[][] srcSpectra;
	private int nSrcSpectra;
	private int index;
	private int[] sortOrder;
	private AttributeList attributeList;
	private GeometryOfVolume spectroscopyGeometry;
	private SpectroscopyVolumeLocalization spectroscopyVolumeLocalization;
	
	/**
	 * @param	eventContext
	 * @param	srcSpectra
	 * @param	nSrcSpectra
	 * @param	sortOrder
	 * @param	index
	 * @param	attributeList
	 * @param	spectroscopyGeometry
	 * @param	spectroscopyVolumeLocalization
	 */
	public SourceSpectrumSelectionChangeEvent(EventContext eventContext,
			float[][] srcSpectra,int nSrcSpectra,int[] sortOrder,int index,AttributeList attributeList,
			GeometryOfVolume spectroscopyGeometry,SpectroscopyVolumeLocalization spectroscopyVolumeLocalization) {
		super(eventContext);
		this.srcSpectra=srcSpectra;
		this.nSrcSpectra=nSrcSpectra;
		this.sortOrder=sortOrder;
		this.index=index;
		this.attributeList=attributeList;
		this.spectroscopyGeometry=spectroscopyGeometry;
		this.spectroscopyVolumeLocalization=spectroscopyVolumeLocalization;
	}

 	/***/
	public float[][] getSourceSpectra() { return srcSpectra; }
	/***/
	public int getNumberOfSourceSpectra() { return nSrcSpectra; }
	/***/
	public int[] getSortOrder() { return sortOrder; }
	/***/
	public int getIndex() { return index; }
	/***/
	public AttributeList getAttributeList() { return attributeList; }
	/***/
	public GeometryOfVolume getGeometryOfVolume() { return spectroscopyGeometry; }
	/***/
	public SpectroscopyVolumeLocalization getSpectroscopyVolumeLocalization() { return spectroscopyVolumeLocalization; } 
}


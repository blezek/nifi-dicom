/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext; 
import com.pixelmed.event.SelfRegisteringListener; 
import com.pixelmed.display.event.FrameSelectionChangeEvent; 
import com.pixelmed.display.event.FrameSortOrderChangeEvent; 
import com.pixelmed.display.event.SourceImageSelectionChangeEvent; 
import com.pixelmed.dicom.AttributeList;

/**
 * @author	dclunie
 */
class SourceImageSortOrderPanel extends SourceInstanceSortOrderPanel {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageSortOrderPanel.java,v 1.19 2017/01/24 10:50:41 dclunie Exp $";

	// implement SourceImageSelectionChangeListener ...
	
	private OurSourceImageSelectionChangeListener ourSourceImageSelectionChangeListener;

	class OurSourceImageSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurSourceImageSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.SourceImageSelectionChangeEvent",eventContext);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			SourceImageSelectionChangeEvent sis = (SourceImageSelectionChangeEvent)e;
			byFrameOrderButton.setSelected(true);
			nSrcInstances=sis.getNumberOfBufferedImages();			// sets in parent, else Slider won't appear when we update it later
			currentSrcInstanceAttributeList=sis.getAttributeList();
			replaceListOfDimensions(buildListOfDimensionsFromAttributeList(currentSrcInstanceAttributeList));
			currentSrcInstanceSortOrder=sis.getSortOrder();
			currentSrcInstanceIndex=sis.getIndex();
			updateCineSlider(1,nSrcInstances,currentSrcInstanceIndex+1);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit nSrcInstances = "+nSrcInstances);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit currentSrcInstanceIndex = "+currentSrcInstanceIndex);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit currentSrcInstanceSortOrder = "+currentSrcInstanceSortOrder);
		}
	}

	// our own methods ...
	
	/**
	 * @param	typeOfPanelEventContext
	 */
	public SourceImageSortOrderPanel(EventContext typeOfPanelEventContext) {
		super(typeOfPanelEventContext);
		ourSourceImageSelectionChangeListener = new OurSourceImageSelectionChangeListener(typeOfPanelEventContext);
	}
	
}



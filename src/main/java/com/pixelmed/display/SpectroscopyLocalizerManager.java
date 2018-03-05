/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.SpectroscopyVolumeLocalization;
import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.FrameSortOrderChangeEvent;
import com.pixelmed.display.event.SourceSpectrumSelectionChangeEvent;
import com.pixelmed.display.event.StatusChangeEvent;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;
import com.pixelmed.geometry.GeometryOfVolume;
import com.pixelmed.geometry.LocalizerPoster;

import java.util.Vector;

class SpectroscopyLocalizerManager extends LocalizerManager {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SpectroscopyLocalizerManager.java,v 1.9 2017/01/24 10:50:41 dclunie Exp $";
		
	/***/
	protected GeometryOfVolume spectroscopyGeometry;
	/***/
	protected SpectroscopyVolumeLocalization spectroscopyVolumeLocalization;
	/***/
	protected int spectroscopyIndex;				// Already has been mapped through spectroscopySortOrder, if present
	/***/
	protected int[] spectroscopySortOrder;
	
	/**
	 */
	protected void drawOutlineOnLocalizerReferenceImagePanel() {
		if (referencedImagePanel != null) {
			localizerPoster.setLocalizerGeometry((referenceImageGeometry.getGeometryOfSlices())[referenceIndex]);
			boolean needRepaint = false;
			if (spectroscopyGeometry != null) {
//System.err.println("SpectroscopyLocalizerManager.drawOutlineOnLocalizer(): setting shape and requesting repaint of reference image panel");
				Vector shapes = localizerPoster.getOutlineOnLocalizerForThisGeometry((spectroscopyGeometry.getGeometryOfSlices())[spectroscopyIndex]);
				referencedImagePanel.setLocalizerShapes(shapes);
				needRepaint = true;
			}
			if (spectroscopyVolumeLocalization != null) {
				Vector shapes = localizerPoster.getOutlineOnLocalizerForThisVolumeLocalization(spectroscopyVolumeLocalization);
//System.err.println("OurImageLocalizerManager.sourceSpectrumSelectionChanged(): setting volume localization shapes and requesting repaint of reference image panel");
				referencedImagePanel.setVolumeLocalizationShapes(shapes);
				needRepaint = true;
			}
			if (needRepaint) {
				referencedImagePanel.getParent().validate();	// otherwise sometimes doesn't actually repaint until cursor focus moved away from list of images
				referencedImagePanel.repaint();
			}
		}
	}
	
	/***/
	private OurSourceSpectrumSelectionChangeListener sourceSpectrumSelectionChangeListener;

	class OurSourceSpectrumSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurSourceSpectrumSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.SourceSpectrumSelectionChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			SourceSpectrumSelectionChangeEvent sss = (SourceSpectrumSelectionChangeEvent)e;
			spectroscopySortOrder = sss.getSortOrder();
			spectroscopyIndex = sss.getIndex();
			if (spectroscopySortOrder != null) {
				spectroscopyIndex=spectroscopySortOrder[spectroscopyIndex];
			}
			spectroscopyGeometry=sss.getGeometryOfVolume();
			spectroscopyVolumeLocalization=sss.getSpectroscopyVolumeLocalization();
			if (spectroscopyGeometry != null && spectroscopyGeometry.getGeometryOfSlices() == null) {
//System.err.println("SpectroscopyLocalizerManager.OurSourceSpectrumSelectionChangeListenerchanged(): getGeometryOfSlices() is null, so not using spectroscopyGeometry");
				spectroscopyGeometry=null;
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Selected image does not contain necessary geometry for localization."));
			}
		}
	}
	
	/***/
	//private EventContext sourceSpectrumSelectionContext;
	
	/*
	 * @param	sourceSpectrumSelectionContext
	 */
	public void setSourceSpectrumSelectionContext(EventContext sourceSpectrumSelectionContext) {
		//this.sourceSpectrumSelectionContext=sourceSpectrumSelectionContext;
		if (sourceSpectrumSelectionChangeListener == null) {
			sourceSpectrumSelectionChangeListener = new OurSourceSpectrumSelectionChangeListener(sourceSpectrumSelectionContext);
		}
		else {
			sourceSpectrumSelectionChangeListener.setEventContext(sourceSpectrumSelectionContext);
		}
	}
	
	/***/
	private OurSpectrumFrameSelectionChangeListener spectrumFrameSelectionChangeListener;

	class OurSpectrumFrameSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurSpectrumFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
//System.err.println("SpectroscopyLocalizerManager.OurSpectrumFrameSelectionChangeListener.changed():");
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
			spectroscopyIndex = fse.getIndex();
			if (spectroscopySortOrder != null) {
				spectroscopyIndex=spectroscopySortOrder[spectroscopyIndex];
			}
			if (referenceImageGeometry != null && spectroscopyGeometry != null) {	// don't do anything if no referenced image
//System.err.println("SpectroscopyLocalizerManager.OurSpectrumFrameSelectionChangeListener.changed(): updating localizer outline");
				drawOutlineOnLocalizerReferenceImagePanel();
			}
		}
	}
	
	/***/
	//private EventContext spectrumFrameSelectionContext;
	
	/*
	 * @param	spectrumFrameSelectionContext
	 */
	public void setSpectrumFrameSelectionContext(EventContext spectrumFrameSelectionContext) {
		//this.spectrumFrameSelectionContext=spectrumFrameSelectionContext;
		if (spectrumFrameSelectionChangeListener == null) {
			spectrumFrameSelectionChangeListener = new OurSpectrumFrameSelectionChangeListener(spectrumFrameSelectionContext);
		}
		else {
			spectrumFrameSelectionChangeListener.setEventContext(spectrumFrameSelectionContext);
		}
	}
	
	/***/
	private OurSpectrumFrameSortOrderChangeListener spectrumFrameSortOrderChangeListener;

	class OurSpectrumFrameSortOrderChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurSpectrumFrameSortOrderChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSortOrderChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
//System.err.println("SpectroscopyLocalizerManager.OurSpectrumFrameSortOrderChangeListener.changed():");
			FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent)e;
			spectroscopySortOrder = fso.getSortOrder();
			spectroscopyIndex = fso.getIndex();
			if (spectroscopySortOrder != null) {
				spectroscopyIndex=spectroscopySortOrder[spectroscopyIndex];
			}
			if (referenceImageGeometry != null && spectroscopyGeometry != null) {	// don't do anything if no referenced image
//System.err.println("SpectroscopyLocalizerManager.OurSpectrumFrameSortOrderChangeListener.changed(): updating localizer outline");
				drawOutlineOnLocalizerReferenceImagePanel();
			}
		}
	}

	/***/
	//private EventContext spectrumFrameSortOrderContext;
	
	/*
	 * @param	spectrumFrameSortOrderContext
	 */
	public void setSpectrumFrameSortOrderContext(EventContext spectrumFrameSortOrderContext) {
		//this.spectrumFrameSortOrderContext=spectrumFrameSortOrderContext;
		if (spectrumFrameSortOrderChangeListener == null) {
			spectrumFrameSortOrderChangeListener = new OurSpectrumFrameSortOrderChangeListener(spectrumFrameSortOrderContext);
		}
		else {
			spectrumFrameSortOrderChangeListener.setEventContext(spectrumFrameSortOrderContext);
		}
	}
		
	public void reset() {
//System.err.println("SpectroscopyLocalizerManager.reset():");
		super.reset();
		spectroscopyIndex=0;
		spectroscopySortOrder=null;
		spectroscopyGeometry=null;
		spectroscopyVolumeLocalization=null;
	}
}


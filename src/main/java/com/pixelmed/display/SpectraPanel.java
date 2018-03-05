/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import javax.swing.JComponent;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.SwingUtilities;

import java.util.Arrays;
import java.util.HashMap;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;
import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.FrameSortOrderChangeEvent;
import com.pixelmed.display.event.SourceImageSelectionChangeEvent;
import com.pixelmed.display.event.StatusChangeEvent; 
import com.pixelmed.dicom.*;

// for localizer and background...

import com.pixelmed.geometry.*;
import javax.vecmath.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Implements a component that can display a single or multi-frame spectra in a
 * single panel, over an optional background image, with scrolling through frames
 * of a multi-frame spectra, resizing to the size of the panel, feedback of cursor
 * position status.</p>
 *
 * @see com.pixelmed.display.SourceSpectra
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
public class SpectraPanel extends PlotGraph implements MouseListener, MouseMotionListener {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SpectraPanel.java,v 1.29 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SpectraPanel.class);
	
	private float[][] srcSpectra;
	private int nSrcSpectra;
	private int currentSrcSpectrumIndex;		// N.B. this is the current scrolling index and MUST ALWAYS BE DEREFERENCED through currentSrcSpectraSortOrder (if non-null) before use
	private int[] currentSrcSpectraSortOrder;
	
	/***/
	private GeometryOfVolume spectroscopyGeometry;
	/***/
	private SpectroscopyVolumeLocalization spectroscopyVolumeLocalization;
	
	/**
	 * <p>Get the geometry of the frames currently loaded in the spectroscopy panel.</p>
	 *
	 * @return	the geometry of the frames
	 */
	public GeometryOfVolume getSpectroscopyGeometry() {
		return spectroscopyGeometry;
	}

	/**
	 * <p>Get the localization volume of the spectra currently loaded in the spectroscopy panel.</p>
	 *
	 * @return	the localization volume
	 */
	public SpectroscopyVolumeLocalization getSpectroscopyVolumeLocalization() {
		return spectroscopyVolumeLocalization;
	}

	// Event stuff ...

	int lastmiddley;
	
	/**
	 * @param	e
	 */
	public void mouseClicked(MouseEvent e) {}
	/**
	 * @param	e
	 */
	public void mouseEntered(MouseEvent e) {}
	/**
	 * @param	e
	 */
	public void mouseExited(MouseEvent e) {}

	/**
	 * @param	e
	 */
	public void mouseDragged(MouseEvent e) {
//System.err.println("mouseDragged event"+e);
		if (SwingUtilities.isMiddleMouseButton(e)) {
			int delta = e.getY()-lastmiddley;
			int newSrcSpectrumIndex = currentSrcSpectrumIndex + delta;
			if (newSrcSpectrumIndex >= nSrcSpectra) newSrcSpectrumIndex=nSrcSpectra-1;
			if (newSrcSpectrumIndex < 0) newSrcSpectrumIndex=0;
			// don't send an event unless it is actually necessary ...
//System.err.println("mouseDragged middle newSrcSpectrumIndex="+newSrcSpectrumIndex+" currentSrcSpectrumIndex="+currentSrcSpectrumIndex);
			if (newSrcSpectrumIndex != currentSrcSpectrumIndex)
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(typeOfPanelEventContext,newSrcSpectrumIndex));
			lastmiddley=e.getY();		// helps a lot when clipped at top or bottom of range
		}
	}

	/**
	 * @param	e
	 */
	public void mouseMoved(MouseEvent e) {
//System.err.println("SpectraPanel.mouseMoved: "+e.getX()+" "+e.getY());
		{
			double x = e.getX();
			double y = e.getY();

			StringBuffer sbuf = new StringBuffer();
			sbuf.append("(");
			sbuf.append((int)(x/widthOfTile));
			sbuf.append(":");
			sbuf.append((int)(x%widthOfTile/widthOfTile*samplesPerTile));
			sbuf.append(",");
			sbuf.append((int)(y/heightOfTile));
			sbuf.append(")");

			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(sbuf.toString()));
		}
	}

	/**
	 * @param	e
	 */
	public void mousePressed(MouseEvent e) {
//System.err.println("mousePressed event"+e);
		if (SwingUtilities.isMiddleMouseButton(e)) {
			lastmiddley=e.getY();
		}
	}

	/**
	 * @param	e
	 */
	public void mouseReleased(MouseEvent e) {}

	// Event stuff ...

	/***/
	EventContext typeOfPanelEventContext;
	/***/
	EventContext backgroundImageEventContext;

	// implement FrameSelectionChangeListener to respond to events from self or elsewhere ...
	
	private OurFrameSelectionChangeListener ourFrameSelectionChangeListener;

	class OurFrameSelectionChangeListener extends SelfRegisteringListener {
	
		public OurFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
//System.err.println("SpectraPanel.OurFrameSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
			int newCurrentSrcSpectrumIndex = fse.getIndex();
			if (currentSrcSpectrumIndex != newCurrentSrcSpectrumIndex) {
//System.err.println("SingleImagePanel.OurFrameSelectionChangeListener.changed(): new values");
				currentSrcSpectrumIndex=newCurrentSrcSpectrumIndex;
				cachedBackgroundImage=null;				// since background image may be for different frame
				repaint();
			}
			else {
//System.err.println("SpectraPanel.OurFrameSelectionChangeListener.changed(): same values");
			}
		}
	}
	
	// implement FrameSortOrderChangeListener to respond to events from self or elsewhere ...
	
	private OurFrameSortOrderChangeListener ourFrameSortOrderChangeListener;

	class OurFrameSortOrderChangeListener extends SelfRegisteringListener {
	
		public OurFrameSortOrderChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSortOrderChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurFrameSortOrderChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent)e;
			int[] newSrcSpectraSortOrder = fso.getSortOrder();
			int newCurrentSrcSpectrumIndex = fso.getIndex();
			if (currentSrcSpectrumIndex != newCurrentSrcSpectrumIndex
			 || currentSrcSpectraSortOrder != newSrcSpectraSortOrder
			 || !Arrays.equals(currentSrcSpectraSortOrder,newSrcSpectraSortOrder)) {
				slf4jlogger.info("OurFrameSortOrderChangeListener.changed(): new values");
				currentSrcSpectrumIndex=newCurrentSrcSpectrumIndex;
				currentSrcSpectraSortOrder=newSrcSpectraSortOrder;	// change even if null in event (request to go back to implicit order)
				cachedBackgroundImage=null;				// since background image may be for different frame
				repaint();
			}
			else {
				slf4jlogger.info("OurFrameSortOrderChangeListener.changed(): same values");
			}
		}
	}
	
	// Implement setting and loading of background image in response to event ...

	/***/
	SourceImage backgroundSImg;
	/***/
	int currentBackgroundSrcImageIndex;
	/***/
	int[] currentBackgroundSrcImageSortOrder;
	/***/
	private GeometryOfVolume backgroundSrcImageGeometry;

	// caches of stuff ...
	
	/***/
	BufferedImage cachedBackgroundImage;
	/***/
	private HashMap cacheOfImagesForSpectra;
	/***/
	private Rectangle cacheForBounds;

	private OurBackgroundSourceImageSelectionChangeListener ourBackgroundSourceImageSelectionChangeListener;

	class OurBackgroundSourceImageSelectionChangeListener extends SelfRegisteringListener {
	
		public OurBackgroundSourceImageSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.SourceImageSelectionChangeEvent",eventContext);
//System.err.println("SpectraPanel.OurBackgroundSourceImageSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
//System.err.println("SpectraPanel.OurBackgroundSourceImageSelectionChangeListener.changed():");
			SourceImageSelectionChangeEvent sis = (SourceImageSelectionChangeEvent)e;
			backgroundSImg = sis.getSourceImage();
			currentBackgroundSrcImageSortOrder = sis.getSortOrder();
			currentBackgroundSrcImageIndex = sis.getIndex();	// irrelevant ... will be overridden by nearest selection
			backgroundSrcImageGeometry = sis.getGeometryOfVolume();
			cachedBackgroundImage=null;				// forces application of new values on repaint
			cacheOfImagesForSpectra=null;
			cacheForBounds=null;
			repaint();
		}
	}

	/**
	 * <p>Build a panel in which to display the supplied spectra.</p>
	 *
	 * @param	srcSpectra				the spectra to display
	 * @param	nTilesPerColumn				the number of tiles per column
	 * @param	nTilesPerRow				the number of tiles per row
	 * @param	minimum					the minimum data value to display (bottom of a tile)
	 * @param	maximum					the maximum data value to display (top of a tile)
	 * @param	spectroscopyGeometry			the 3D location of the acquired spectroscopy data
	 * @param	spectroscopyVolumeLocalization		the 3D localization performed prior to acquisition of the spectroscopy data
	 * @param	typeOfPanelEventContext
	 * @param	backgroundImageEventContext
	 */
	public SpectraPanel(float[][] srcSpectra,int nTilesPerColumn,int nTilesPerRow,float minimum,float maximum,
			GeometryOfVolume spectroscopyGeometry,SpectroscopyVolumeLocalization spectroscopyVolumeLocalization,
			EventContext typeOfPanelEventContext,
			EventContext backgroundImageEventContext) {

		super(nTilesPerColumn,nTilesPerRow,minimum,maximum);
		
		this.srcSpectra=srcSpectra;
		nSrcSpectra=srcSpectra.length;
		currentSrcSpectrumIndex=0;
		this.samples=srcSpectra[currentSrcSpectrumIndex];
		samplesPerRow=samples.length/nTilesPerColumn;
		samplesPerTile=samplesPerRow/nTilesPerRow;
		currentSrcSpectraSortOrder=null;
		
		this.spectroscopyGeometry=spectroscopyGeometry;
		this.spectroscopyVolumeLocalization=spectroscopyVolumeLocalization;
		
		backgroundSImg = null;
		currentBackgroundSrcImageSortOrder = null;
		currentBackgroundSrcImageIndex = 0;
		backgroundSrcImageGeometry=null;
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		this.typeOfPanelEventContext=typeOfPanelEventContext;
		ourFrameSelectionChangeListener = new OurFrameSelectionChangeListener(typeOfPanelEventContext);
		ourFrameSortOrderChangeListener = new OurFrameSortOrderChangeListener(typeOfPanelEventContext);
		this.backgroundImageEventContext=backgroundImageEventContext;
		ourBackgroundSourceImageSelectionChangeListener = new OurBackgroundSourceImageSelectionChangeListener(backgroundImageEventContext);

		// clear caches ...
		cachedBackgroundImage=null;
		cacheOfImagesForSpectra=null;
		cacheForBounds=null;
	}
	
	/**
	 * @param	g
	 */
	public void paintComponent(Graphics g) {
//System.err.println("SpectraPanel.paintComponent");
		int useSrcSpectrumIndex = currentSrcSpectraSortOrder == null ? currentSrcSpectrumIndex : currentSrcSpectraSortOrder[currentSrcSpectrumIndex];
		samples=srcSpectra[currentSrcSpectrumIndex]; 
		Cursor was = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// the handle of the float[] spectral data already selected in "samples" is used as the key for the cache
		Rectangle currentBounds = this.getBounds();
		if (cacheForBounds != null && currentBounds.width == cacheForBounds.width && currentBounds.height == cacheForBounds.height && cacheOfImagesForSpectra != null) {
			BufferedImage cachedImageOfSpectra = (BufferedImage)cacheOfImagesForSpectra.get(samples);
			if (cachedImageOfSpectra != null) {
//System.err.println("SpectraPanel.paintComponent: using cached image");
				g.drawImage(cachedImageOfSpectra,0,0,this);
				setCursor(was);
				return;
			}
		}
		else {
//System.err.println("SpectraPanel.paintComponent: flushing cache");
			cacheOfImagesForSpectra = null;	// bounds may have changed so flush the cache
		}
		if (cacheOfImagesForSpectra == null) {
//System.err.println("SpectraPanel.paintComponent: creating new cache of spectra as images");
			cacheOfImagesForSpectra = new HashMap();
			cacheForBounds = currentBounds;
		}
		
		Rectangle boundsOfSpectra = null;
		Rectangle boundsOfBackgroundImage = null;
		if (cachedBackgroundImage == null) {
//System.err.println("SpectraPanel.paintComponent: trying to create new cached background image");
			if (spectroscopyGeometry != null && backgroundSrcImageGeometry != null) {
				int index = backgroundSrcImageGeometry.findClosestSliceInSamePlane(spectroscopyGeometry.getGeometryOfSlices()[useSrcSpectrumIndex]);
				cachedBackgroundImage = backgroundSImg.getBufferedImage(index);
				if (cachedBackgroundImage.getColorModel().getNumComponents() == 1) {
//System.err.println("SpectraPanel.paintComponent: building windowed grayscale image");
					boolean signed=backgroundSImg.isSigned();
					boolean inverted=backgroundSImg.isInverted();
					double useSlope;
					double useIntercept;
					boolean hasPad = backgroundSImg.isPadded();
					int pad = hasPad ? backgroundSImg.getPadValue() : 0;
					int padRangeLimit = hasPad ? backgroundSImg.getPadRangeLimit() : 0;
					ModalityTransform modalityTransform = backgroundSImg.getModalityTransform();
					if (modalityTransform != null) {
						useSlope = modalityTransform.getRescaleSlope(index);
						useIntercept = modalityTransform.getRescaleIntercept(index);
					}
					else {
						useSlope=1.0;
						useIntercept=0.0;
					}
					double windowWidth;
					double windowCenter;
					VOITransform voiTransform = backgroundSImg.getVOITransform();
					if (voiTransform != null && voiTransform.getNumberOfTransforms(index) > 0) {
						windowWidth=voiTransform.getWidth(index,0);
						windowCenter=voiTransform.getCenter(index,0);
					}
					else {
						double ourMin = backgroundSImg.getMinimum()*useSlope+useIntercept;
						double ourMax = backgroundSImg.getMaximum()*useSlope+useIntercept;
						windowWidth=(ourMax-ourMin);
						windowCenter=(ourMax+ourMin)/2.0;
					}
					cachedBackgroundImage = SingleImagePanel.applyWindowCenterAndWidthLinear(cachedBackgroundImage,
									windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit);
				}
				
				GeometryOfSlice geom = spectroscopyGeometry.getGeometryOfSlices()[useSrcSpectrumIndex];
				Point3d    tlhc = geom.getTLHC();
				Vector3d    row = geom.getRow();
				Vector3d column = geom.getColumn();
				
				Point3d[] backgroundCorners = LocalizerPoster.getCornersOfSourceRectangleInSourceSpace(backgroundSrcImageGeometry.getGeometryOfSlices()[index]);
				          backgroundCorners = LocalizerPoster.transformPointsFromSourceSpaceIntoSpecifiedSpace(backgroundCorners,tlhc,row,column);
				Point3d[]    spectraCorners = LocalizerPoster.getCornersOfSourceRectangleInSourceSpace(geom);
				             spectraCorners = LocalizerPoster.transformPointsFromSourceSpaceIntoSpecifiedSpace(spectraCorners,tlhc,row,column);
					     
				// the corners are now in the same plane and Z can be ignored
				
				Point3d[] intersectionCorners = LocalizerPoster.getIntersectionOfRectanglesInXYPlane(backgroundCorners,spectraCorners);
				
				boundsOfSpectra = LocalizerPoster.getBoundsOfContainedRectangle(intersectionCorners,spectraCorners,this.getBounds());
				boundsOfBackgroundImage = LocalizerPoster.getBoundsOfContainedRectangle(intersectionCorners,backgroundCorners,
					new Rectangle(cachedBackgroundImage.getWidth(),cachedBackgroundImage.getHeight()));

			}
		}
		actuallyPaintComponent(g,cachedBackgroundImage,boundsOfSpectra,boundsOfBackgroundImage);
//System.err.println("SpectraPanel.paintComponent: adding to cache");
		cacheOfImagesForSpectra.put(samples,imageOfRenderedPlot);
		setCursor(was);
	}
	
	public void deconstruct() {
//System.err.println("SpectraPanel.deconstruct()");
		// avoid "listener leak"
		if (ourFrameSelectionChangeListener != null) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSelectionChangeListener);
			ourFrameSelectionChangeListener=null;
		}
		if (ourFrameSortOrderChangeListener != null) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSortOrderChangeListener);
			ourFrameSortOrderChangeListener=null;
		}
		if (ourBackgroundSourceImageSelectionChangeListener != null) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourBackgroundSourceImageSelectionChangeListener);
			ourBackgroundSourceImageSelectionChangeListener=null;
		}

	}
	
	/*
	 * @param container
	 */
	 public static void deconstructAllSpectraPanelsInContainer(Container container) {
		Component[] components = container.getComponents();				
//System.err.println("SpectraPanel.deconstructAllSpectraPanelsInContainer(): deconstructing old SpectraPanels components.length="+components.length);
		for (int i=0; i<components.length; ++i) {
			Component component = components[i];
			if (component instanceof SpectraPanel) {
				((SpectraPanel)component).deconstruct();
			}
		}
	}

	protected void finalize() throws Throwable {
//System.err.println("SpectraPanel.finalize()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.finalize();
	}
}


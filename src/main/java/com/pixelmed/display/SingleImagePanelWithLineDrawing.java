/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;

import com.pixelmed.display.event.RegionSelectionChangeEvent;

import com.pixelmed.geometry.GeometryOfVolume;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Implements a component that extends a SingleImagePanel to also draw regions.</p>
 *
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
class SingleImagePanelWithLineDrawing extends SingleImagePanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SingleImagePanelWithLineDrawing.java,v 1.8 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(WindowCenterAndWidth.class);

	// Constructors ...

	public SingleImagePanelWithLineDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,int[] sortOrder,Vector preDefinedShapes,Vector preDefinedText,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,sortOrder,preDefinedShapes,preDefinedText,imageGeometry);
	}

	public SingleImagePanelWithLineDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,imageGeometry);
	}

	public SingleImagePanelWithLineDrawing(SourceImage sImg,EventContext typeOfPanelEventContext) {
		super(sImg,typeOfPanelEventContext);
	}

	public SingleImagePanelWithLineDrawing(SourceImage sImg) {
		super(sImg);
	}

	// Event stuff ...

	/**
	 * @param	e
	 */
	public void keyPressed(KeyEvent e) {
//System.err.println("Key pressed event"+e);
		if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//System.err.println("Delete or backspace pressed");
			setSelectedDrawingShapes(null);
			repaint();
		}
		else {
			super.keyPressed(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right clicked "+e.getX()+" "+e.getY());
			checkForHitOnPersistentShapes(e.getX(),e.getY());
		}
		else {
			super.mouseClicked(e);
		}
	}


	/**
	 * @param	e
	 */
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right dragged "+e.getX()+" "+e.getY());
			dragInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mouseDragged(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseMoved(MouseEvent e) {
//System.err.println(e.getX()+" "+e.getY());
		super.mouseMoved(e);
	}

	/**
	 * @param	e
	 */
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right pressed "+e.getX()+" "+e.getY());
			startInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mousePressed(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right released "+e.getX()+" "+e.getY());
			endInteractiveDrawing(e.getX(),e.getY());	// sets region selection parameters to propagate in change event
			//ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new RegionSelectionChangeEvent(typeOfPanelEventContext,
			//	regionSelectionCenterX,regionSelectionCenterY,regionSelectionTLHCX,regionSelectionTLHCY,regionSelectionBRHCX,regionSelectionBRHCY));
		}
		else {
			super.mouseReleased(e);
		}
	}

	// stuff to handle drawing ...

	/***/
	protected Point2D startPoint;
	/***/
	static final int crossSize = 5;		// actually just one arm of the cross

	/**
	 * @param	x
	 * @param	y
	 */
	protected void startInteractiveDrawing(int x,int y) {
		startPoint = getImageCoordinateFromWindowCoordinate(x,y);
	}

	/**
	 * @param	x
	 * @param	y
	 */
	protected void dragInteractiveDrawing(int x,int y) {
		double startX = startPoint.getX();
		double startY = startPoint.getY();
		Point2D endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		double endX = endPoint.getX();
		double endY = endPoint.getY();
		if (startX != endX || startY != endY) {
			interactiveDrawingShapes = new Vector();
			interactiveDrawingShapes.add(new Line2D.Float(startPoint,endPoint));
			repaint();
		}
		// else ignore, was click, not drag
	}
	
	/**
	 * @param	x
	 * @param	y
	 */
	protected void endInteractiveDrawing(int x,int y) {
		double startX = startPoint.getX();
		double startY = startPoint.getY();
		Point2D endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		double endX = endPoint.getX();
		double endY = endPoint.getY();
		if (startX != endX || startY != endY) {
			double width = endX - startX;
			//if (width < 0) {
			//	width=-width;
			//}
			double height = endY - startY;
			//if (height < 0) {
			//	height=-height;
			//}
			double distanceInPixels = Math.sqrt(width*width+height*height);
			
			NumberFormat formatter = new DecimalFormat("###.#");
			String annotation = formatter.format(distanceInPixels)+" pixels";
			if (pixelSpacingInSourceImage > 0) {
				annotation = formatter.format(distanceInPixels*pixelSpacingInSourceImage)+" mm" + (typeOfPixelSpacing == null ? " " : (" ("+typeOfPixelSpacing+")"));
			}
			else if (imageGeometry != null) {
				// else perhaps are non-square, etc. ... see if we can do it between 3D points
				int useSrcImageIndex = currentSrcImageSortOrder == null ? currentSrcImageIndex : currentSrcImageSortOrder[currentSrcImageIndex];
				double[] startLocationIn3DSpace = new double[3];
				double[] endLocationIn3DSpace = new double[3];
				imageGeometry.lookupImageCoordinate(startLocationIn3DSpace,startX,startY,useSrcImageIndex);
				imageGeometry.lookupImageCoordinate(endLocationIn3DSpace,endX,endY,useSrcImageIndex);
				double deltaX = endLocationIn3DSpace[0] - startLocationIn3DSpace[0];
				double deltaY = endLocationIn3DSpace[1] - startLocationIn3DSpace[1];
				double deltaZ = endLocationIn3DSpace[2] - startLocationIn3DSpace[2];
				double distanceInmm = Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
				annotation = formatter.format(distanceInmm)+" mm (3D NSQ)";
			}
			else {
				// else perhaps are non-square, non-3D, etc.
				annotation = formatter.format(distanceInPixels)+" pixels";
			}
			slf4jlogger.info("Length={} pixels, {}",distanceInPixels,annotation);
			interactiveDrawingShapes = null;
			if (persistentDrawingShapes == null) {
				persistentDrawingShapes = new Vector();
			}
			persistentDrawingShapes.add(new Line2D.Float(startPoint,endPoint));
			if (persistentDrawingText == null) {
				persistentDrawingText = new Vector();
			}
			persistentDrawingText.add(new TextAnnotation(annotation,endX,endY));
			repaint();
		}
		// else ignore, was click, not drag
	}

	/**
	 * @param	x
	 * @param	y
	 */
	protected void checkForHitOnPersistentShapes(int x,int y) {
		Point2D testPoint = getImageCoordinateFromWindowCoordinate(x,y);
		int testX = (int)(testPoint.getX());
		int testY = (int)(testPoint.getY());
		boolean changedSomething = false;
		Vector doneShapes = new Vector();
		// check previously selected shapes to toggle selection off if selected again ...
		if (selectedDrawingShapes != null) {
			Iterator i = selectedDrawingShapes.iterator();
			while (i.hasNext()) {
				Shape shape = (Shape)i.next();
				if (!doneShapes.contains(shape)) {
					doneShapes.add(shape);
					if (shape.contains(testX,testY)) {
//System.err.println("De-select shape "+shape);
						doneShapes.add(shape);
						if (persistentDrawingShapes == null) {
							persistentDrawingShapes = new Vector();
						}
						persistentDrawingShapes.add(shape);
						selectedDrawingShapes.remove(shape);
						i = selectedDrawingShapes.iterator();		// restart with new selector, since modified vector
						changedSomething=true;
					}
				}
			}
		}
		// not previously selected ... select any hit shape without undoing any de-selection from the previous step
		if (persistentDrawingShapes != null) {
			Iterator i = persistentDrawingShapes.iterator();
			while (i.hasNext()) {
				Shape shape = (Shape)i.next();
				if (!doneShapes.contains(shape)) {
					doneShapes.add(shape);
					if (shape.contains(testX,testY)) {
//System.err.println("Select shape "+shape);
						if (selectedDrawingShapes == null) {
							selectedDrawingShapes = new Vector();
						}
						selectedDrawingShapes.add(shape);
						persistentDrawingShapes.remove(shape);
						i = persistentDrawingShapes.iterator();		// restart with new selector, since modified vector
						changedSomething=true;
					}
				}
			}
		}
		if (changedSomething) {
			repaint();
		}
	}
}






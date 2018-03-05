/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Point;
import java.awt.Shape;

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;

import com.pixelmed.display.event.RegionSelectionChangeEvent; 

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

/**
 * <p>Implements a component that extends a SingleImagePanel to also draw regions.</p>
 *
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
class SingleImagePanelWithRegionDrawing extends SingleImagePanel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SingleImagePanelWithRegionDrawing.java,v 1.7 2017/01/24 10:50:41 dclunie Exp $";

	// Constructors ...

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,int[] sortOrder,Vector preDefinedShapes,Vector preDefinedText,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,sortOrder,preDefinedShapes,preDefinedText,imageGeometry);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,imageGeometry);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext) {
		super(sImg,typeOfPanelEventContext);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg) {
		super(sImg);
	}

	// Region selection stuff (set by right mouse drag) ...
	
	private double regionSelectionCenterX;
	private double regionSelectionCenterY;
	private double regionSelectionTLHCX;
	private double regionSelectionTLHCY;
	private double regionSelectionBRHCX;
	private double regionSelectionBRHCY;

	/**
	 * @param	centerX
	 * @param	centerY
	 * @param	oneCornerX
	 * @param	oneCornerY
	 * @param	otherCornerX
	 * @param	otherCornerY
	 */
	private void setRegionSelection(double centerX,double centerY,double oneCornerX,double oneCornerY,double otherCornerX,double otherCornerY) {
//System.err.println("SingleImagePanelWithRegionDrawing.setRegionSelection() event: centerX="+centerX+" centerY="+centerY+" oneCornerX="+oneCornerX+" oneCornerY="+oneCornerY+" otherCornerX="+otherCornerX+" otherCornerY="+otherCornerY);
		regionSelectionCenterX = centerX;
		regionSelectionCenterY = centerY;
		if (oneCornerX < otherCornerX) {
			regionSelectionTLHCX=oneCornerX;
			regionSelectionBRHCX=otherCornerX;
		}
		else {
			regionSelectionTLHCX=otherCornerX;
			regionSelectionBRHCX=oneCornerX;
		}
		if (oneCornerY < otherCornerY) {
			regionSelectionTLHCY=oneCornerY;
			regionSelectionBRHCY=otherCornerY;
		}
		else {
			regionSelectionTLHCY=otherCornerY;
			regionSelectionBRHCY=oneCornerY;
		}
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
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new RegionSelectionChangeEvent(typeOfPanelEventContext,
				regionSelectionCenterX,regionSelectionCenterY,regionSelectionTLHCX,regionSelectionTLHCY,regionSelectionBRHCX,regionSelectionBRHCY));
		}
		else {
			super.mouseReleased(e);
		}
	}

	// stuff to handle drawing ...
	
	/**
	 * <p>Construct a new shape from the specified coordinates.<p>
	 *
	 * <p>The default is a rectangle - override this method in a sub-class to use a different shape (e.g., an ellipse).<p>
	 *
	 * @param	x
	 * @param	y
	 * @param	width
	 * @param	height
	 */
	protected Shape makeNewDrawingShape(double tlhcX,double tlhcY,double width,double height) {
		return new Rectangle2D.Double(tlhcX,tlhcY,width,height);
	}

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
			interactiveDrawingShapes.add(new Line2D.Double(startPoint,endPoint));
			double tlhcX=startX;
			double width = endX - startX;
			if (width < 0) {
				width=-width;
				tlhcX=endX;
			}
			double tlhcY=startY;
			double height = endY - startY;
			if (height < 0) {
				height=-height;
				tlhcY=endY;
			}
			interactiveDrawingShapes.add(makeNewDrawingShape(tlhcX,tlhcY,width,height));
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
			double tlhcX=startX;
			double width = endX - startX;
			if (width < 0) {
				width=-width;
				tlhcX=endX;
			}
			double tlhcY=startY;
			double height = endY - startY;
			if (height < 0) {
				height=-height;
				tlhcY=endY;
			}
			setRegionSelection((tlhcX+width)/2,(tlhcY+height)/2,tlhcX,tlhcY,tlhcX+width,tlhcY+height);
			interactiveDrawingShapes = null;
			if (persistentDrawingShapes == null) {
				persistentDrawingShapes = new Vector();
			}
			persistentDrawingShapes.add(makeNewDrawingShape(tlhcX,tlhcY,width,height));
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
		double testX = testPoint.getX();
		double testY = testPoint.getY();
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






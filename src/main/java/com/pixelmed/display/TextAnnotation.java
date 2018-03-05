/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.geom.Point2D; 

/**
 * <p>A class to encapsulate a text annotation at a location on an image.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 * @see com.pixelmed.display.DicomBrowser
 *
 * @author	dclunie
 */
class TextAnnotation {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/TextAnnotation.java,v 1.10 2017/01/24 10:50:41 dclunie Exp $";

	private String string;
	private Point2D anchorPoint;

	/**
	 * @param	string	the annotation
	 * @param	x	the horizontal location
	 * @param	y	the vertical location
	 */
	public TextAnnotation(String string,double x,double y) {
		this.string=string;
		this.anchorPoint=new Point2D.Double(x,y);
	}

	/**
	 * @param	string		the annotation
	 * @param	anchorPoint	the location on the image
	 */
	public TextAnnotation(String string,Point2D anchorPoint) {
		this.string=string;
		this.anchorPoint=anchorPoint;
	}

	/**
	 * <p>Get the text of the annotation.</p>
	 *
	 * @return	the annotation
	 */
	public String getString() { return string; }

	/**
	 * <p>Get the location.</p>
	 *
	 * @return	the location
	 */
	public Point2D getAnchorPoint() { return anchorPoint; }

	/**
	 * <p>Get the horizontal location.</p>
	 *
	 * @return	the horizontal location
	 */
	public int getAnchorPointXAsInt() { return (int)anchorPoint.getX(); }

	/**
	 * <p>Get the vertical location.</p>
	 *
	 * @return	the vertical location
	 */
	public int getAnchorPointYAsInt() { return (int)anchorPoint.getY(); }
}


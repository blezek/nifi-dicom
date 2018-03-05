/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.geom.AffineTransform;

/**
 * <p>This class describes the graphics environment (number and type and size of displays).</p>
 * 
 * @author	dclunie
 */
public class DescribeGraphicsEnvironment { 

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DescribeGraphicsEnvironment.java,v 1.8 2017/01/24 10:50:40 dclunie Exp $";
	
	public static String dumpImageCapabilities(ImageCapabilities ic,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("isAccelerated = "); buf.append(ic.isAccelerated()); buf.append("\n");
		buf.append(indent); buf.append("isTrueVolatile = "); buf.append(ic.isTrueVolatile()); buf.append("\n");
		return buf.toString();
	}
	
	public static String dumpBufferCapabilities(BufferCapabilities bc,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("isFullScreenRequired = "); buf.append(bc.isFullScreenRequired()); buf.append("\n");
		buf.append(indent); buf.append("isMultiBufferAvailable = "); buf.append(bc.isMultiBufferAvailable()); buf.append("\n");
		buf.append(indent); buf.append("isPageFlipping = "); buf.append(bc.isPageFlipping()); buf.append("\n");

		return buf.toString();
	}
	
	public static String dumpGraphicsConfiguration(GraphicsConfiguration gc,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("getBounds = "); buf.append(gc.getBounds()); buf.append("\n");
		buf.append(indent); buf.append("getColorModel = "); buf.append(gc.getColorModel()); buf.append("\n");
		ImageCapabilities ic = gc.getImageCapabilities();
		buf.append(dumpImageCapabilities(ic,indent+"ImageCapabilities: ",indent+"\t"));
		BufferCapabilities bc = gc.getBufferCapabilities();
		buf.append(dumpBufferCapabilities(bc,indent+"BufferCapabilities: ",indent+"\t"));
		AffineTransform defaultTransform = gc.getDefaultTransform();
		if (defaultTransform != null && !defaultTransform.isIdentity()) {
			buf.append(indent); buf.append("defaultTransform.getScaleX() = "); buf.append(defaultTransform.getScaleX()); buf.append("\n");
			buf.append(indent); buf.append("defaultTransform.getScaleY() = "); buf.append(defaultTransform.getScaleY()); buf.append("\n");
			buf.append(indent); buf.append("25.4/72/defaultTransform.getScaleX() = "); buf.append(25.4/72/defaultTransform.getScaleX()); buf.append("\n");
			buf.append(indent); buf.append("25.4/72/defaultTransform.getScaleY() = "); buf.append(25.4/72/defaultTransform.getScaleY()); buf.append("\n");
		}
		else {
			buf.append(indent); buf.append("No defaultTransform or is identity (and hence likely meaningless)\n");
		}
		AffineTransform normalizingTransform = gc.getNormalizingTransform();
		if (normalizingTransform != null && !normalizingTransform.isIdentity()) {
			buf.append(indent); buf.append("normalizingTransform.getScaleX() = "); buf.append(normalizingTransform.getScaleX()); buf.append("\n");
			buf.append(indent); buf.append("normalizingTransform.getScaleY() = "); buf.append(normalizingTransform.getScaleY()); buf.append("\n");
			buf.append(indent); buf.append("25.4/72/normalizingTransform.getScaleX() = "); buf.append(25.4/72/normalizingTransform.getScaleX()); buf.append(" mm/pixel horizontal\n");
			buf.append(indent); buf.append("25.4/72/normalizingTransform.getScaleY() = "); buf.append(25.4/72/normalizingTransform.getScaleY()); buf.append(" mm/pixel vertical\n");
		}
		else {
			buf.append(indent); buf.append("No normalizingTransform or is identity (and hence likely meaningless)\n");
		}
		if (defaultTransform != null && normalizingTransform != null && (!defaultTransform.isIdentity() || !normalizingTransform.isIdentity())) {
			buf.append(indent); buf.append("defaultTransform.getScaleX()*normalizingTransform.getScaleX() = "); buf.append(defaultTransform.getScaleX()*normalizingTransform.getScaleX()); buf.append("\n");
			buf.append(indent); buf.append("defaultTransform.getScaleY()*normalizingTransform.getScaleY() = "); buf.append(defaultTransform.getScaleY()*normalizingTransform.getScaleY()); buf.append("\n");
			buf.append(indent); buf.append("25.4/72/(defaultTransform.getScaleX()*normalizingTransform.getScaleX()) = "); buf.append(25.4/72/(defaultTransform.getScaleX()*normalizingTransform.getScaleX())); buf.append(" mm/pixel horizontal\n");
			buf.append(indent); buf.append("25.4/72/(defaultTransform.getScaleY()*normalizingTransform.getScaleY()) = "); buf.append(25.4/72/(defaultTransform.getScaleY()*normalizingTransform.getScaleY())); buf.append(" mm/pixel vertical\n");
		}
		return buf.toString();
	}
	
	public static String dumpDisplayMode(DisplayMode dm,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("getBitDepth = "); buf.append(dm.getBitDepth()); buf.append("\n");
		buf.append(indent); buf.append("getHeight = "); buf.append(dm.getHeight()); buf.append("\n");
		buf.append(indent); buf.append("getWidth = "); buf.append(dm.getWidth()); buf.append("\n");
		buf.append(indent); buf.append("getRefreshRate = "); buf.append(dm.getRefreshRate()); buf.append("\n");
		return buf.toString();
	}
	
	public static String dumpGraphicsDevice(GraphicsDevice gd,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("getIDstring = "); buf.append(gd.getIDstring()); buf.append("\n");
		buf.append(indent); buf.append("getAvailableAcceleratedMemory = "); buf.append(gd.getAvailableAcceleratedMemory()); buf.append("\n");
		buf.append(indent); buf.append("getType = "); buf.append(gd.getType()); buf.append("\n");
		buf.append(indent); buf.append("isDisplayChangeSupported = "); buf.append(gd.isDisplayChangeSupported()); buf.append("\n");
		buf.append(indent); buf.append("isFullScreenSupported = "); buf.append(gd.isFullScreenSupported()); buf.append("\n");
		buf.append(dumpDisplayMode(gd.getDisplayMode(),indent+"DisplayMode [Current]:",indent+"\t"));
 		DisplayMode[] dms = gd.getDisplayModes();
		for (int i=0; i < dms.length; i++) {
			DisplayMode dm = dms[i];
			buf.append(dumpDisplayMode(dm,indent+"DisplayMode ["+i+"]:",indent+"\t"));
		}
		buf.append(dumpGraphicsConfiguration(gd.getDefaultConfiguration(),indent+"GraphicsConfiguration [Default]:",indent+"\t"));
		GraphicsConfiguration[] gcs = gd.getConfigurations();
		for (int i=0; i < gcs.length; i++) {
			GraphicsConfiguration gc = gcs[i];
			buf.append(dumpGraphicsConfiguration(gc,indent+"GraphicsConfiguration ["+i+"]:",indent+"\t"));
		}
		return buf.toString();
	}
	
	public static String dumpGraphicsEnvironment(GraphicsEnvironment ge,String label,String indent) {
		StringBuffer buf = new StringBuffer();
		buf.append(label); buf.append("\n");
		buf.append(indent); buf.append("getCenterPoint = "); buf.append(ge.getCenterPoint()); buf.append("\n");
		buf.append(indent); buf.append("getMaximumWindowBounds = "); buf.append(ge.getMaximumWindowBounds()); buf.append("\n");
		buf.append(indent); buf.append("isHeadlessInstance = "); buf.append(ge.isHeadlessInstance()); buf.append("\n");
		buf.append(dumpGraphicsDevice(ge.getDefaultScreenDevice(),indent+"GraphicsDevice [Default]:",indent+"\t"));
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) { 
			GraphicsDevice gd = gs[j];
			buf.append(dumpGraphicsDevice(gd,indent+"GraphicsDevice ["+j+"]:",indent+"\t"));
		}
		return buf.toString();
	}
	
	/**
	 */
	public static void main(String arg[]) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		System.err.println(dumpGraphicsEnvironment(ge,"LocalGraphicsEnvironment:","\t"));
	} 
}


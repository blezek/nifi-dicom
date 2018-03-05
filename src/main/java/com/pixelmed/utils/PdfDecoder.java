/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.lang.reflect.InvocationTargetException;

import java.awt.image.BufferedImage;

/**
 * <p>A class for to encapsulate JPedal PDF decoding capability if available at runtime without requiring it for compilation.</p>
 *
 * @author	dclunie
 */

public class PdfDecoder {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/PdfDecoder.java,v 1.5 2017/01/24 10:50:52 dclunie Exp $";
	
	protected Class  pdfDecoderClass;
	protected Object pdfDecoder;
	
	public PdfDecoder() throws PdfException {
		pdfDecoderClass = null;
		pdfDecoder = null;
		try {
			pdfDecoderClass = Thread.currentThread().getContextClassLoader().loadClass("org.jpedal.PdfDecoder");
			Class [] argTypes  = {};
			Object[] argValues = {};
			pdfDecoder = pdfDecoderClass.getConstructor(argTypes).newInstance(argValues);
		}
		catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException,IllegalAccessException,InvocationTargetException
			throw new PdfException("Could not instantiate org.jpedal.PdfDecoder - "+e);
		}
	}

	public void useHiResScreenDisplay(boolean value) throws PdfException {
		try {
			Class [] argTypes  = { java.lang.Boolean.TYPE };
			Object[] argValues = { value };
			pdfDecoderClass.getMethod("useHiResScreenDisplay",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.useHiResScreenDisplay() - "+e);
		}
	}
	
	public void openPdfFile(String filename) throws PdfException {
		try {
			Class [] argTypes  = { java.lang.String.class };
			Object[] argValues = { filename };
			pdfDecoderClass.getMethod("openPdfFile",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.openPdfFile() - "+e);
		}
	}
	
	public void closePdfFile() throws PdfException {
		try {
			Class [] argTypes  = { };
			Object[] argValues = { };
			pdfDecoderClass.getMethod("closePdfFile",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.closePdfFile() - "+e);
		}
	}

	public int getPageCount() throws PdfException {
		int returnValue = 0;
		try {
			Class [] argTypes  = { };
			Object[] argValues = { };
			returnValue = ((Integer)(pdfDecoderClass.getMethod("getPageCount",argTypes).invoke(pdfDecoder,argValues))).intValue();
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.getPageCount() - "+e);
		}
		return returnValue;
	}
	
	public int getDPI() throws PdfException {
		int dpi = 0;
		try {
			dpi = pdfDecoderClass.getField("dpi").getInt(pdfDecoder);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.getPageCount() - "+e);
		}
		return dpi;
	}
	
	public void setPageParameters(float scaling, int pageNumber) throws PdfException {
		try {
			Class [] argTypes  = { java.lang.Float.TYPE, java.lang.Integer.TYPE };
			Object[] argValues = { scaling, pageNumber };
			pdfDecoderClass.getMethod("setPageParameters",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.setPageParameters() - "+e);
		}
	}

	public void decodePage(int page) throws PdfException {
		try {
			Class [] argTypes  = { java.lang.Integer.TYPE };
			Object[] argValues = { page };
			pdfDecoderClass.getMethod("decodePage",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.decodePage() - "+e);
		}
	}
	
	public void setBackground(java.awt.Color color) throws PdfException {
		try {
			Class [] argTypes  = { java.awt.Color.class };
			Object[] argValues = { color };
			pdfDecoderClass.getMethod("setBackground",argTypes).invoke(pdfDecoder,argValues);
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.setBackground() - "+e);
		}
	}
	
	public BufferedImage getPageAsImage(int pageIndex) throws PdfException {
		BufferedImage returnValue = null;
		try {
			Class [] argTypes  = { java.lang.Integer.TYPE };
			Object[] argValues = { pageIndex };
			returnValue = (BufferedImage)(pdfDecoderClass.getMethod("getPageAsImage",argTypes).invoke(pdfDecoder,argValues));
		}
		catch (Exception e) { // may be NullPointerexception, NoSuchMethodException, IllegalAccessException, InvocationTargetException
			throw new PdfException("Could not invoke org.jpedal.PdfDecoder.getPageAsImage() - "+e);
		}
		return returnValue;
	}
	
	public java.awt.Component getComponent() {		// e.g., to add to frame or panel
		return (java.awt.Component)pdfDecoder;
	}
	
	//public org.jpedal.PdfDecoder getJPedalPdfDecoder() { return (org.jpedal.PdfDecoder)pdfDecoder; }
}



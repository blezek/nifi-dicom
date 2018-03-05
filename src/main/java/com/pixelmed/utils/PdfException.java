/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

/**
 * @author	dclunie
 */
public class PdfException extends Exception {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/PdfException.java,v 1.4 2017/01/24 10:50:52 dclunie Exp $";

	/**
	 * @param	msg
	 */
	public PdfException(String msg) {
		super(msg);
	}
}



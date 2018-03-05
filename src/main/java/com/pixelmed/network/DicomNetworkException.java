/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

/**
 * @author	dclunie
 */
public class DicomNetworkException extends Exception {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/DicomNetworkException.java,v 1.7 2017/01/24 10:50:45 dclunie Exp $";

	/**
	 * @param	msg
	 */
	public DicomNetworkException(String msg) {
		super(msg);
	}
}




/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

public class ThreadUtilities {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/ThreadUtilities.java,v 1.5 2017/01/24 10:50:52 dclunie Exp $";

	public final static void checkIsEventDispatchThreadElseException() {
		if (!java.awt.EventQueue.isDispatchThread()) {
			throw new RuntimeException("Not on AWT EventDispatchThread");
		}
	}
}
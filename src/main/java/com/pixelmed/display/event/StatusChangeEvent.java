/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;

/**
 * @author	dclunie
 */
public class StatusChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/StatusChangeEvent.java,v 1.8 2017/01/24 10:50:42 dclunie Exp $";

	private String statusMessage;

	/**
	 * @param	statusMessage
	 */
	public StatusChangeEvent(String statusMessage) {
		super();
		this.statusMessage=statusMessage;
	}

	/***/
	public String getStatusMessage() { return statusMessage; }
}


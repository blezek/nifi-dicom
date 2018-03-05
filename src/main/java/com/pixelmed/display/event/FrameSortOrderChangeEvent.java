/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class FrameSortOrderChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/FrameSortOrderChangeEvent.java,v 1.6 2017/01/24 10:50:41 dclunie Exp $";

	/***/
	private int[] sortOrder;
	/***/
	private int index;

	/**
	 * @param	eventContext
	 * @param	sortOrder
	 * @param	index
	 */
	public FrameSortOrderChangeEvent(EventContext eventContext,int[] sortOrder,int index) {
		super(eventContext);
		this.sortOrder=sortOrder;
		this.index=index;
	}

	/**
	 * @return	the current sort order array, which may be null
	 */
	public int[] getSortOrder() { return sortOrder; }

	/**
	 * @return	the index of the frame selected
	 */
	public int getIndex() { return index; }
	
	/**
	 * @return	description of the event
	 */
	public String toString() {
		return ("FrameSortOrderChangeEvent: eventContext="+getEventContext()+" index="+index+" sortOrder="+sortOrder);
	}
}


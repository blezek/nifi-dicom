/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class RegionSelectionChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/RegionSelectionChangeEvent.java,v 1.7 2017/01/24 10:50:42 dclunie Exp $";

	private double centerX;
	private double centerY;
	private double tlhcX;
	private double tlhcY;
	private double brhcX;
	private double brhcY;

	/**
	 * @param	eventContext
	 * @param	centerX
	 * @param	centerY
	 * @param	tlhcX
	 * @param	tlhcY
	 * @param	brhcX
	 * @param	brhcY
	 */
	public RegionSelectionChangeEvent(EventContext eventContext,
			double centerX,double centerY,double tlhcX,double tlhcY,double brhcX,double brhcY) {
		super(eventContext);
		this.centerX=centerX;
		this.centerY=centerY;
		this.tlhcX=tlhcX;
		this.tlhcY=tlhcY;
		this.brhcX=brhcX;
		this.brhcY=brhcY;
	}

	/***/
	public double getCenterX() { return centerX; }
	/***/
	public double getCenterY() { return centerY; }
	/***/
	public double getTLHCX() { return tlhcX; }
	/***/
	public double getTLHCY() { return tlhcY; }
	/***/
	public double getBRHCX() { return brhcX; }
	/***/
	public double getBRHCY() { return brhcY; }
}


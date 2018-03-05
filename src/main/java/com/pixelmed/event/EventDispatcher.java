/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.event;

/**
 * @author	dclunie
 */
public interface EventDispatcher {

	/**
	 * @param	l
	 */
	public void addListener(Listener l);
	
	/**
	 * @param	l
	 */
	public void removeListener(Listener l);
	
	/**
	 * @param	c
	 */
	//public void removeAllListenersWhichAreOfClass(Class c);
	
	/**
	 * @param	c
	 */
	//public void removeAllListenersForEventsOfClass(Class c);
	
	/**
	 * @param	context
	 */
	public void removeAllListenersForEventContext(EventContext context) ;

	/**
	 * @param	wcwEvent
	 */
	public void processEvent(Event wcwEvent);
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.event;

import java.util.EventListener;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
public abstract class Listener implements EventListener {	// why bother implementing java.util.EventListener ?
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/event/Listener.java,v 1.7 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(Listener.class);

	/***/
	private Class classOfEventHandled;
	
	/***/
	private EventContext eventContext;			// may be null
	
	/***/
	private int listenerNumber;
	
	/***/
	private static int listenerCount;
	
	/**
	 */
	//public Listener() {
	//	setClassOfEventHandled(null);
	//	setEventContext(null);
	//}
	
	/**
	 * @param	classOfEventHandled
	 */
	//public Listener(Class classOfEventHandled) {
	//	setClassOfEventHandled(classOfEventHandled);
	//	setEventContext(null);
	//}
	
	/**
	 * @param	classOfEventHandled
	 * @param	eventContext
	 */
	//public Listener(Class classOfEventHandled,EventContext eventContext) {
	//	setClassOfEventHandled(classOfEventHandled);
	//	setEventContext(eventContext);
	//}

	/**
	 * @param	className
	 * @param	eventContext
	 */
	public Listener(String className,EventContext eventContext) {
		Class classOfEvent = null;
		try {
			classOfEvent = Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			slf4jlogger.error("",e);
		}
		setClassOfEventHandled(classOfEvent);
		setEventContext(eventContext);
		listenerNumber=listenerCount++;
	}

	/**
	 * @param	classOfEventHandled
	 */
	public final void setClassOfEventHandled(Class classOfEventHandled) {
//System.err.println("Listener.setClassOfEventHandled(): class="+classOfEventHandled);
		this.classOfEventHandled=classOfEventHandled;
	}

	/**
	 */
	public final Class getClassOfEventHandled() {
		return classOfEventHandled;
	}


	/**
	 * @param	eventContext
	 */
	public final void setEventContext(EventContext eventContext) {
//System.err.println("Listener.setClassOfEventHandled(): eventContext="+eventContext);
		this.eventContext=eventContext;
	}

	/**
	 */
	public final EventContext getEventContext() {
		return eventContext;
	}

	/**
	 * @param	e
	 */
	public abstract void changed(Event e);

	/**
	 */
	public String toString() {
		return "Listener ["+listenerNumber+"] for events of class "
			+getClassOfEventHandled().toString()
			+" in context "
			+(getEventContext() == null ? "null" : getEventContext().toString());
	}
}


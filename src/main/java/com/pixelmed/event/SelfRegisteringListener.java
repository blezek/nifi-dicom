/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.event;

/**
 * @author	dclunie
 */
public abstract class SelfRegisteringListener extends Listener {

	/***/
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/event/SelfRegisteringListener.java,v 1.7 2017/01/24 10:50:43 dclunie Exp $";

	/**
	 * @param	classOfEventHandled
	 */
	//public SelfRegisteringListener(Class classOfEventHandled) {
	//	super(classOfEventHandled);
	//	ApplicationEventDispatcher.getApplicationEventDispatcher().addListener(this);
	//}
	
	/**
	 * @param	classOfEventHandled
	 * @param	eventContext
	 */
	//public SelfRegisteringListener(Class classOfEventHandled,EventContext eventContext) {
	//	super(classOfEventHandled,eventContext);
	//	ApplicationEventDispatcher.getApplicationEventDispatcher().addListener(this);
	//}

	/**
	 * <p>Create a listener that is registered with the ApplicationEventDispatcher.</p>
	 *
	 * <p>The caller of this constructor needs to keep a strong reference (e.g., dialog or application scope variable) to
	 * keep the listener hanging around and receiving events, since the ApplicationEventDispatcher uses only a
	 * WeakReference, which may go away during garbage collection.</p>
	 *
	 * @param	className
	 * @param	eventContext
	 */
	public SelfRegisteringListener(String className,EventContext eventContext) {
		super(className,eventContext);
		ApplicationEventDispatcher applicationEventDispatcher = ApplicationEventDispatcher.getApplicationEventDispatcher();
		if (applicationEventDispatcher != null) {
			applicationEventDispatcher.addListener(this);
		}
		else {
			throw new RuntimeException("Internal error - cannot get ApplicationEventDispatcher");
		}
	}
}


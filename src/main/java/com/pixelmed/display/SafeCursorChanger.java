/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.utils.ThreadUtilities;

import java.awt.Component;
import java.awt.Cursor;

import java.lang.reflect.InvocationTargetException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class SafeCursorChanger {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SafeCursorChanger.java,v 1.5 2017/01/24 10:50:40 dclunie Exp $";
	
	private static final Logger slf4jlogger = LoggerFactory.getLogger(SafeCursorChanger.class);

	protected Cursor was;
	protected Component component;
	
	public SafeCursorChanger(Component component) {
		this.component = component;
	}
	
	public class SafeCursorGetterThread implements Runnable {
		protected Cursor cursor;
		
		public SafeCursorGetterThread() {
		}
		
		public void run() {
			cursor = component.getCursor();
		}
		
		public Cursor getCursor() { return cursor; }
	}
	
	public class SafeCursorSetterThread implements Runnable {
		protected Cursor cursor;
		
		public SafeCursorSetterThread(Cursor cursor) {
			this.cursor = cursor;
		}
		
		public void run() {
			component.setCursor(cursor);
		}
	}
	
	public void saveCursor() {
		if (java.awt.EventQueue.isDispatchThread()) {
			was = component.getCursor();
		}
		else {
			SafeCursorGetterThread getter = new SafeCursorGetterThread();
			try {
				java.awt.EventQueue.invokeAndWait(getter);		// NB. need to wait, since we want the value, and also can't be called on EDT thread
				was = getter.getCursor();
			}
			catch (InterruptedException e) {
				slf4jlogger.error("",e);
			}
			catch (InvocationTargetException e) {
				slf4jlogger.error("",e);
			}
		}
	}

	public void setWaitCursor() {
		if (java.awt.EventQueue.isDispatchThread()) {
			component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		else {
			java.awt.EventQueue.invokeLater(new SafeCursorSetterThread(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
		}
	}
	
	public void restoreCursor() {
		if (java.awt.EventQueue.isDispatchThread()) {
			component.setCursor(was);
		}
		else {
			java.awt.EventQueue.invokeLater(new SafeCursorSetterThread(was));
		}
	}
}
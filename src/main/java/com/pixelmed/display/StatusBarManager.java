/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import javax.swing.JLabel; 

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event; 
import com.pixelmed.event.SelfRegisteringListener;

import com.pixelmed.display.event.StatusChangeEvent; 
import com.pixelmed.display.event.WindowCenterAndWidthChangeEvent; 

import com.pixelmed.utils.ThreadUtilities;

class StatusBarManager {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/StatusBarManager.java,v 1.9 2017/01/24 10:50:41 dclunie Exp $";

	private OurStatusChangeListener ourStatusChangeListener;

	static class SafeStatusBarUpdaterThread implements Runnable {
		JLabel statusBar;
		String statusMessage;
		
		SafeStatusBarUpdaterThread(JLabel statusBar,String statusMessage) {
			this.statusBar = statusBar;
			this.statusMessage = statusMessage;
		}
		
		public void run() {
			statusBar.setText(statusMessage);
			statusBar.revalidate();
			statusBar.paintImmediately(statusBar.getVisibleRect());
		}
	}
	
	class OurStatusChangeListener extends SelfRegisteringListener {
		private JLabel statusBar;
		
		public OurStatusChangeListener(JLabel statusBar) {
			super("com.pixelmed.display.event.StatusChangeEvent",null/*Any EventContext*/);
//System.err.println("StatusBarManager.OurStatusChangeListener():");
			this.statusBar=statusBar;
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			StatusChangeEvent sce = (StatusChangeEvent)e;
			String statusMessage = sce.getStatusMessage();
//System.err.println("StatusBarManager.OurStatusChangeListener.changed(): new status message is:"+statusMessage);
			if (java.awt.EventQueue.isDispatchThread()) {
				statusBar.setText(statusMessage);
				statusBar.revalidate();
				statusBar.paintImmediately(statusBar.getVisibleRect());
			}
			else {
				java.awt.EventQueue.invokeLater(new SafeStatusBarUpdaterThread(statusBar,statusMessage));
			}
		}
	}
	
	/***/
	private OurWindowCenterAndWidthChangeListener ourWindowCenterAndWidthChangeListener;

	class OurWindowCenterAndWidthChangeListener extends SelfRegisteringListener {
	
		public OurWindowCenterAndWidthChangeListener() {
			super("com.pixelmed.display.event.WindowCenterAndWidthChangeEvent",null/*Any EventContext*/);
//System.err.println("StatusBarManager.OurWindowCenterAndWidthChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			WindowCenterAndWidthChangeEvent wcwe = (WindowCenterAndWidthChangeEvent)e;
//System.err.println("StatusBarManager.OurWindowCenterAndWidthChangeListener.changed(): event="+wcwe);
			StringBuffer sbuf = new StringBuffer();
			sbuf.append("C ");
			sbuf.append(wcwe.getWindowCenter());
			sbuf.append(" W ");
			sbuf.append(wcwe.getWindowWidth());
			
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(sbuf.toString()));
		}
	}


	/***/
	private JLabel statusBar;
	
	public StatusBarManager(String initialMessage) {
		// The width of the initial text seems to set the (preferred ?) size to be when the packing is done, so use blanks, the setText()
		statusBar = new JLabel("                                                                                                                      ");
		statusBar.setText(initialMessage);
		ourStatusChangeListener = new OurStatusChangeListener(statusBar);			// registers itself with application dispatcher
		ourWindowCenterAndWidthChangeListener = new OurWindowCenterAndWidthChangeListener();	// registers itself with application dispatcher
	}
	
	public JLabel getStatusBar() {
		return statusBar;
	}

}


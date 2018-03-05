/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.transfermonitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>A class that extends {@link java.io.FilterOutputStream FilterOutputStream} to
 * track statistics on the transfers.</p>
 *
 * @author	dclunie
 */
public class TransferMonitor {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/transfermonitor/TransferMonitor.java,v 1.4 2017/01/24 10:50:51 dclunie Exp $";
	
	// static stuff to maintain collection of ourselves
	
	private static Map contexts = null;
	
	public static synchronized TransferMonitor newTransferMonitor(TransferMonitoringContext transferMonitoringContext) {
		if (contexts == null) {
			contexts = new HashMap();
		}
		TransferMonitor monitor = new TransferMonitor(transferMonitoringContext);
		contexts.put(transferMonitoringContext,monitor);
		return monitor;
	}
	
	public static String report() {
		StringBuffer strbuf = new StringBuffer();
		if (contexts != null) {
			Iterator i = contexts.keySet().iterator();
			while (i.hasNext()) {
				TransferMonitoringContext transferMonitoringContext = (TransferMonitoringContext)i.next();
				TransferMonitor monitor = (TransferMonitor)contexts.get(transferMonitoringContext);
				strbuf.append(monitor.toString());
				strbuf.append("\n");
			}
		}
		return strbuf.toString();
	}
	
	// per instance stuff
	
	private long count;
	
	private TransferMonitoringContext transferMonitoringContext;
	
	private TransferMonitor(TransferMonitoringContext transferMonitoringContext) {
		count=0;
		this.transferMonitoringContext=transferMonitoringContext;
	}

	public synchronized void countUp(int n) {
		count+=n;
	}
	
	public synchronized void countUp(long n) {
		count+=n;
	}
	
	public String toString() {
		return transferMonitoringContext+" = "+count;
	}

}


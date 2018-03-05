/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.transfermonitor;

import java.io.OutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>A class that extends {@link java.io.OutputStream OutputStream} to
 * track statistics on the transfers.</p>
 *
 * @author	dclunie
 */
public class MonitoredOutputStream extends OutputStream {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/transfermonitor/MonitoredOutputStream.java,v 1.5 2017/01/24 10:50:51 dclunie Exp $";
	
	private OutputStream out;

	private TransferMonitor monitor;

	public MonitoredOutputStream(OutputStream out,TransferMonitoringContext transferMonitoringContext) {
		this.out = out;
		monitor = TransferMonitor.newTransferMonitor(transferMonitoringContext);
	}

	public final void close() throws IOException {
		out.close();
	}
	
	public final void flush() throws IOException {
		out.flush();
	}

	public final void write(byte[] b) throws IOException {
		out.write(b);
		monitor.countUp(b.length);
	}
	
	public final void write(byte[] b, int off, int len) throws IOException {
		out.write(b,off,len);
		monitor.countUp(len);
	}
	
	public final void write(int b) throws IOException {
		out.write(b);
		monitor.countUp(1);
	}
}


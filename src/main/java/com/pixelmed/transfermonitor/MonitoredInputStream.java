/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.transfermonitor;

import java.io.InputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>A class that extends {@link java.io.InputStream InputStream} to
 * track statistics on the transfers.</p>
 *
 * @author	dclunie
 */
public class MonitoredInputStream extends InputStream {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/transfermonitor/MonitoredInputStream.java,v 1.4 2017/01/24 10:50:51 dclunie Exp $";

	private InputStream in;

	private TransferMonitor monitor;

	public MonitoredInputStream(InputStream in,TransferMonitoringContext transferMonitoringContext) {
		this.in = in;
		monitor = TransferMonitor.newTransferMonitor(transferMonitoringContext);
	}

	public final int read() throws IOException {
		int value = in.read();
		if (value >= 0) {
			monitor.countUp(1);
		}
		return value;
	}
	
	public final int read(byte[] b) throws IOException {
		int nActuallyRead = in.read(b);
		if (nActuallyRead > 0) {
			monitor.countUp(nActuallyRead);
		}
		return nActuallyRead;
	}
	
	public final int read(byte[] b, int off, int len) throws IOException {
		int nActuallyRead = in.read(b,off,len);
		if (nActuallyRead > 0) {
			monitor.countUp(nActuallyRead);
		}
		return nActuallyRead;
	}
	
	public final long skip(long n) throws IOException {
		long nActuallySkipped = in.skip(n);
		if (nActuallySkipped > 0) {
			monitor.countUp(nActuallySkipped);
		}
		return nActuallySkipped;
	}
	
	public final int available() throws IOException {
		return in.available();
	}
	
	public final void close() throws IOException {
		in.close();
	}
	
	public final void mark(int readlimit) {
		in.mark(readlimit);
	}
	
	public final boolean markSupported() {
		return in.markSupported();
	}
	
	public final void reset() throws IOException {
		in.reset();
	}
}




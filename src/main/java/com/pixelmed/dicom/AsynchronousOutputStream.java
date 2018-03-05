/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>A class that extends {@link java.io.FilterOutputStream FilterOutputStream} by 
 * creating a separate thread to actually perform the output operations, and returning
 * immediately from write calls, but blocking on closing.</p>
 *
 * @author	dclunie
 */
public class AsynchronousOutputStream extends FilterOutputStream {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AsynchronousOutputStream.java,v 1.8 2017/01/24 10:50:35 dclunie Exp $";

	private BlockingQueue<ByteArrayObject> q;
	private Consumer c;
	private CountDownLatch doneSignal;
	
	private class ByteArrayObject {
		byte[] b;
		int off;
		int len;
		boolean requestToFlush;
		boolean requestToClose;
		
		ByteArrayObject(byte[] b) {
			this.b = b;
			off = 0;
			len = b.length;
			requestToFlush = false;
			requestToClose = false;
		}
		
		ByteArrayObject(byte[] b,int off,int len) {
			this.b = b;
			this.off = off;
			this.len = len;
			requestToFlush = false;
			requestToClose = false;
		}
		
		ByteArrayObject(int b) {	// horribly inefficient but not intended to be used
			this.b = new byte[1];
			this.b[0] = (byte)b;
			this.off = 0;
			this.len = 1;
			requestToFlush = false;
			requestToClose = false;
		}
		
		ByteArrayObject(boolean requestToFlush,boolean requestToClose) {
			b = null;
			this.requestToFlush = requestToFlush;
			this.requestToClose = requestToClose;
		}
		
		byte[] getBytes() {
			return b;
		}
		
		int getOffset() {
			return off;
		}
		
		int getLength() {
			return len;
		}
		
		boolean isRequestToFlush() {
			return requestToFlush;
		}
		
		boolean isRequestToClose() {
			return requestToClose;
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("array ");
			if (b != null) {
				buf.append("not ");
			}
			buf.append("null");
			buf.append("; off=");
			buf.append(Integer.toString(off));
			buf.append("; len=");
			buf.append(Integer.toString(len));
			buf.append("; flush=");
			buf.append(requestToFlush ? "T" : "F");
			buf.append("; close=");
			buf.append(requestToClose ? "T" : "F");
			buf.append("\n");
			return buf.toString();
		}
	}

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream. 
     * <p>
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @throws  IOException  if an I/O error occurs.
     */
	public void write(byte b[],int off,int len) throws IOException {
		try {
			q.put(new ByteArrayObject(b,0,b.length));
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
	}

 	// Overload all other parent write() methods to be sure that byteOffset is updated (and not updated more than once)

    /**
     * Writes the specified <code>byte</code> to this output stream. 
     *
     * @param      b   the <code>byte</code>.
     * @throws  IOException  if an I/O error occurs.
     */
    public void write(int b) throws IOException {
		try {
			q.put(new ByteArrayObject(b));
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
    }

    /**
     * Writes <code>b.length</code> bytes to this output stream. 
     *
     * @param      b   the data to be written.
     * @throws  IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
		try {
			q.put(new ByteArrayObject(b));
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
    }
	
	private class Consumer implements Runnable {
		private final BlockingQueue<ByteArrayObject> q;
		private final OutputStream out;
		private final CountDownLatch doneSignal;
		private boolean bad;
		private IOException exception;
		
		Consumer(BlockingQueue<ByteArrayObject> q,CountDownLatch doneSignal,OutputStream out) {
			this.q = q;
			this.out = out;
			this.doneSignal = doneSignal;
			bad = false;
		}
		
		public void run() {
			try {
				while(doneSignal.getCount() > 0) {
					consume(q.take());
				}
			}
			catch (InterruptedException e) {
				bad = true;
				exception = new IOException(e);
			}
			// returns when done and hence thread goes away
//System.err.println("AsynchronousOutputStream.Consumer.run(): returning");
		}
		
		void consume(ByteArrayObject bao) {
//System.err.println("AsynchronousOutputStream.Consumer.consume(): "+bao);
			{
				byte[] b = bao.getBytes();
				int off = bao.getOffset();
				int len = bao.getLength();
				if (b != null && len > 0) {
					try {
						out.write(b,off,len);
					}
					catch (IOException e) {
//slf4jlogger.error("", e);;
						bad = true;
						exception = e;
						doneSignal.countDown();
					}
				}
			}
			if (bao.isRequestToFlush()) {
				try {
					out.flush();
				}
				catch (IOException e) {
//slf4jlogger.error("", e);;
					bad = true;
					exception = e;
					doneSignal.countDown();
				}
			}
			if (bao.isRequestToClose()) {
				try {
					out.close();
				}
				catch (IOException e) {
//slf4jlogger.error("", e);;
					bad = true;
					exception = e;
				}
//System.err.println("AsynchronousOutputStream.Consumer.consume(): isRequestToClose, setting done ");
				doneSignal.countDown();
			}
		}
		
		synchronized boolean isBad() {
			return bad;
		}
		
		synchronized IOException getException() {
			return exception;
		}
	}

	/**
	 * @param	out	the {@link java.io.OutputStream OutputStream} to write to
	 */
	public AsynchronousOutputStream(OutputStream out) {
		super(out);
//System.err.println("AsynchronousOutputStream:");
		q = new LinkedBlockingQueue<ByteArrayObject>();
		doneSignal = new CountDownLatch(1);
		c = new Consumer(q,doneSignal,out);
		new Thread(c).start();
	}

    /**
     * Flushes this output stream and forces any buffered output bytes to be written out to the stream. 
     *
     * @throws  IOException  if an I/O error occurs.
     */
	public void flush()	throws IOException {
//System.err.println("AsynchronousOutputStream.flush():");
		try {
			q.put(new ByteArrayObject(true,false));
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
		// do NOT block
	}

    /**
     * Closes this output stream and releases any system resources associated with the stream. 
     *
     * @throws  IOException  if an I/O error occurs.
     */
	public void close()	throws IOException {
//System.err.println("AsynchronousOutputStream.close():");
		try {
			q.put(new ByteArrayObject(false,true));
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
//System.err.println("AsynchronousOutputStream.close(): about to block until all output is done");
		try {
			doneSignal.await();
		}
		catch (InterruptedException e) {
//slf4jlogger.error("", e);;
			throw new IOException (e);
		}
//System.err.println("AsynchronousOutputStream.close(): back from waiting for all output to be done");
		if (c.isBad()) {
			IOException e = c.getException();
			if (e != null) {
				throw e;
			}
			else {
				throw new IOException("Bad for unknown reason");
			}
		}
//System.err.println("AsynchronousOutputStream.close(): returning");
	}
}
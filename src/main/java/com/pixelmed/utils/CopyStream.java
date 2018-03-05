/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.io.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for copying an entire input stream to an output stream.</p>
 *
 * @author	dclunie
 */
public class CopyStream {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/CopyStream.java,v 1.16 2017/05/24 12:37:50 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CopyStream.class);
	
	private static final int defaultReadBufferSize = 32768;
	private static final int defaultBufferedInputStreamSizeForFileCopy  = 0;	// i.e., unbuffered
	private static final int defaultBufferedOutputStreamSizeForFileCopy = 0;	// i.e., unbuffered
	
	private CopyStream() {}

	/**
	 * <p>Skip as many bytes as requested, unless an exception occurs.</p>
	 *
	 * @param	in			the input stream in which to skip the bytes
	 * @param	length		number of bytes to read (no more and no less)
	 * @throws	IOException
	 */
	public static void skipInsistently(InputStream in,long length) throws IOException {
		long remaining = length;
		while (remaining > 0) {
//System.err.println("CopyStream.skipInsistently(): looping remaining="+remaining);
			long bytesSkipped = in.skip(remaining);
//System.err.println("CopyStream.skipInsistently(): asked for ="+remaining+" got="+bytesSkipped);
			if (bytesSkipped <= 0) throw new IOException("skip failed with "+remaining+" bytes remaining to be skipped, wanted "+length);
			remaining-=bytesSkipped;
		}
	}

	/**
	 * <p>Copy the specified even number of bytes from the current position of the input stream to an output stream,
	 * swapping adjacent pairs of bytes.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	readBufferSize	how much data to read in each request
	 * @param	in				the source
	 * @param	out				the destination
	 * @param	count			the number of bytes to copy
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copyByteSwapped(int readBufferSize,InputStream in,OutputStream out,long count) throws IOException {
		assert count%2 == 0;
		if (readBufferSize == 0) {
			readBufferSize = defaultReadBufferSize;
		}
		byte[] readBuffer = new byte[readBufferSize];
		while (count > 1) {
			int want = count > readBufferSize ? readBufferSize : (int)count;
			assert want%2 == 0;
			int have = 0;
			while (want > 0) {
				int got = in.read(readBuffer,have,want);
				have+=got;
				want-=got;
			}
			if (have > 0) {
				for (int i=0; i<have-1; i+=2) {
					byte hold = readBuffer[i];
					readBuffer[i] = readBuffer[i+1];
					readBuffer[i+1] = hold;
				}
				out.write(readBuffer,0,have);
				count-=have;
			}
		}
		out.flush();
	}

	/**
	 * <p>Copy the specified even number of bytes from the current position of the input stream to an output stream,
	 * swapping adjacent pairs of bytes.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	in		the source
	 * @param	out		the destination
	 * @param	count	the number of bytes to copy
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copyByteSwapped(InputStream in,OutputStream out,long count) throws IOException {
		copyByteSwapped(defaultReadBufferSize,in,out,count);
	}

	/**
	 * <p>Copy the specified number of bytes from the current position of the input stream to an output stream.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	in				the source
	 * @param	out				the destination
	 * @param	count			the number of bytes to copy
	 * @param	readBufferSize	how much data to read in each request
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(int readBufferSize,InputStream in,OutputStream out,long count) throws IOException {
		copyTraditionalWay(readBufferSize,in,out,count);
		//copyWithNIOBuffer(readBufferSize,in,out,count);
	}
	
	private static final void copyTraditionalWay(int readBufferSize,InputStream in,OutputStream out,long length) throws IOException {
		long count = length;
		slf4jlogger.debug("copyTraditionalWay(): start count = {}",count);
		long startTime = System.currentTimeMillis();
		
		if (readBufferSize == 0) {
			readBufferSize = defaultReadBufferSize;
		}
		byte[] readBuffer = new byte[readBufferSize];
		while (count > 0) {
//System.err.println("CopyStream.copy(): looping count = "+count);
			int want = count > readBufferSize ? readBufferSize : (int)count;
//System.err.println("CopyStream.copy(): want = "+want);
			int got = in.read(readBuffer,0,want);
//System.err.println("CopyStream.copy(): got = "+got);
			if (got > 0) {
				out.write(readBuffer,0,got);
				count-=got;
			}
			else {	// (001004)
				throw new IOException("read failed with "+count+" bytes remaining to be read, wanted "+length);
			}
		}
		out.flush();
		
		slf4jlogger.debug("copyTraditionalWay(): done in {} ms",(System.currentTimeMillis()-startTime));
	}
	
	private static final void copyWithNIOBuffer(int readBufferSize,InputStream in,OutputStream out,long count) throws IOException {
		// see "https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/"
		// but does not seem to be any faster than copyTraditionalWay() :(
		slf4jlogger.debug("copyWithNIOBuffer(): start count = {}",count);	// (001005)
		long startTime = System.currentTimeMillis();

		if (readBufferSize == 0) {
			readBufferSize = defaultReadBufferSize;
		}
		final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(readBufferSize);
		
		final java.nio.channels.ReadableByteChannel inch = java.nio.channels.Channels.newChannel(in);
		final java.nio.channels.WritableByteChannel outch = java.nio.channels.Channels.newChannel(out);
		
		while (inch.read(buffer) != -1) {
			buffer.flip();
			outch.write(buffer);
			buffer.compact();
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			outch.write(buffer);
		}

		inch.close();	// hopefully does not close underlying InputStream, which needs to remain open; ? even necessary :(
		outch.close();	// hopefully does not close underlying OutputStream, which needs to remain open; ? even necessary :(

		slf4jlogger.debug("copyTraditionalWay(): done in {} ms",(System.currentTimeMillis()-startTime));
	}

	/**
	 * <p>Copy the specified number of bytes from the current position of the input stream to an output stream.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	in				the source
	 * @param	out				the destination
	 * @param	count			the number of bytes to copy
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(InputStream in,OutputStream out,long count) throws IOException {
		copy(defaultReadBufferSize,in,out,count);
	}

	/**
	 * <p>Copy an entire input stream to an output stream.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	readBufferSize	how much data to read in each request
	 * @param	in				the source
	 * @param	out				the destination
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(int readBufferSize,InputStream in,OutputStream out) throws IOException {
		if (readBufferSize == 0) {
			readBufferSize = defaultReadBufferSize;
		}
		byte[] readBuffer = new byte[readBufferSize];
		while (true) {
			int got = in.read(readBuffer);
			if (got > 0) {
				out.write(readBuffer,0,got);
			}
			else {
				break;
			}
		}
		out.flush();
	}

	/**
	 * <p>Copy an entire input stream to an output stream.</p>
	 *
	 * <p>The data is copied in chunks rather than as individual bytes, but the input and output
	 * streams are used as is, and no {@link java.io.BufferedInputStream BufferedInputStream}
	 * or {@link java.io.BufferedOutputStream BufferedOutputStream} is inserted; the caller
	 * is expected to do that if maximum performance is desired.</p>
	 *
	 * <p>Also, neither the input nor the output streams are explicitly closed after the
	 * copying has complete; the caller is expected to do that as well, since there may
	 * be occasions when there is more to be written to the output, or the input is to
	 * be rewound and reused, or whatever.</p>
	 *
	 * @param	in		the source
	 * @param	out		the destination
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(InputStream in,OutputStream out) throws IOException {
		copy(defaultReadBufferSize,in,out);
	}

	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #copy(File,File,int,int,int)} instead.
	 * @param	inFile									the source
	 * @param	outFile									the destination
	 * @param	readBufferSize							how much data to read in each request
	 * @param	bufferedInputStreamSizeForFileCopy		the buffered input stream size (or zero if unbuffered)
	 * @param	bufferedOutputStreamSizeForFileCopy		and the buffered output stream size (or zero if unbuffered)
	 * @param	debugLevel								ignored
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(File inFile,File outFile,int readBufferSize,int bufferedInputStreamSizeForFileCopy,int bufferedOutputStreamSizeForFileCopy,int debugLevel) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		copy(inFile,outFile,readBufferSize,bufferedInputStreamSizeForFileCopy,bufferedOutputStreamSizeForFileCopy);
	}
	
	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @param	inFile									the source
	 * @param	outFile									the destination
	 * @param	readBufferSize							how much data to read in each request
	 * @param	bufferedInputStreamSizeForFileCopy		the buffered input stream size (or zero if unbuffered)
	 * @param	bufferedOutputStreamSizeForFileCopy		and the buffered output stream size (or zero if unbuffered)
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(File inFile,File outFile,int readBufferSize,int bufferedInputStreamSizeForFileCopy,int bufferedOutputStreamSizeForFileCopy) throws IOException {
		slf4jlogger.debug("Using readBufferSize of {} bytes",readBufferSize);
		boolean useBufferedInputStream = bufferedInputStreamSizeForFileCopy > 0;
		boolean useBufferedOutputStream = bufferedOutputStreamSizeForFileCopy > 0;
		if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug((useBufferedInputStream ? "U" : "Not u")+"sing BufferedInputStream"+(useBufferedInputStream ? (" with size of " + bufferedInputStreamSizeForFileCopy + " bytes") : ""));
		InputStream in = useBufferedInputStream
						? new BufferedInputStream(new FileInputStream(inFile),bufferedInputStreamSizeForFileCopy)
						: new FileInputStream(inFile);
		if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug((useBufferedOutputStream ? "U" : "Not u")+"sing BufferedOutputStream"+(useBufferedOutputStream ? (" with size of " + bufferedOutputStreamSizeForFileCopy + " bytes") : ""));
		OutputStream out = useBufferedOutputStream
						? new BufferedOutputStream(new FileOutputStream(outFile),bufferedOutputStreamSizeForFileCopy)
						: new FileOutputStream(outFile);
		copy(readBufferSize,in,out);
		in.close();
		out.close();
	}

	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #copy(String,String,int,int,int)} instead.
	 * @param	inFile									the source
	 * @param	outFile									the destination
	 * @param	readBufferSize							how much data to read in each request
	 * @param	bufferedInputStreamSizeForFileCopy		the buffered input stream size (or zero if unbuffered)
	 * @param	bufferedOutputStreamSizeForFileCopy		and the buffered output stream size (or zero if unbuffered)
	 * @param	debugLevel								ignored
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(String inFile,String outFile,int readBufferSize,int bufferedInputStreamSizeForFileCopy,int bufferedOutputStreamSizeForFileCopy,int debugLevel) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		copy(inFile,outFile,readBufferSize,bufferedInputStreamSizeForFileCopy,bufferedOutputStreamSizeForFileCopy);
	}
	
	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @param	inFile									the source
	 * @param	outFile									the destination
	 * @param	readBufferSize							how much data to read in each request
	 * @param	bufferedInputStreamSizeForFileCopy		the buffered input stream size (or zero if unbuffered)
	 * @param	bufferedOutputStreamSizeForFileCopy		and the buffered output stream size (or zero if unbuffered)
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(String inFile,String outFile,int readBufferSize,int bufferedInputStreamSizeForFileCopy,int bufferedOutputStreamSizeForFileCopy) throws IOException {
		copy(new File(inFile),new File(outFile),readBufferSize,bufferedInputStreamSizeForFileCopy,bufferedOutputStreamSizeForFileCopy);
	}


	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @param	inFile		the source
	 * @param	outFile		the destination
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(File inFile,File outFile) throws IOException {
		copy(inFile,outFile,defaultReadBufferSize,defaultBufferedInputStreamSizeForFileCopy,defaultBufferedOutputStreamSizeForFileCopy);
	}

	/**
	 * <p>Copy an entire input file to an output file.</p>
	 *
	 * @param	inFile		the source
	 * @param	outFile		the destination
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	public static final void copy(String inFile,String outFile) throws IOException {
		copy(new File(inFile),new File(outFile),defaultReadBufferSize,defaultBufferedInputStreamSizeForFileCopy,defaultBufferedOutputStreamSizeForFileCopy);
	}

	/**
	 * <p>Copy one file to another.</p>
	 *
	 * @param	arg	array of two or five strings - input file, output file,
	 *			optionally the copy buffer size,
	 *			the buffered input stream size (or zero if unbuffered),
	 *			and the buffered output stream size (or zero if unbuffered),
	 */
	public static void main(String arg[]) {
		try {
			String inFile = null;
			String outFile = null;
			int readBufferSize = defaultReadBufferSize;
			int bufferedInputStreamSizeForFileCopy  = 0;
			int bufferedOutputStreamSizeForFileCopy = 0;
			if (arg.length == 2) {
				inFile = arg[0];
				outFile = arg[1];
			}
			else if (arg.length == 5) {
				inFile = arg[0];
				outFile = arg[1];
				readBufferSize = Integer.parseInt(arg[2]);
				bufferedInputStreamSizeForFileCopy  = Integer.parseInt(arg[3]);
				bufferedOutputStreamSizeForFileCopy = Integer.parseInt(arg[4]);
			}
			if (inFile == null) {
				System.err.println("Error: Usage: java com.pixelmed.utils.CopyStream infile outfile [ readBufferSize bufferedInputStreamSizeForFileCopy bufferedOutputStreamSizeForFileCopy]");
			}
			else {
				long startTime=System.currentTimeMillis();
				copy(inFile,outFile,readBufferSize,bufferedInputStreamSizeForFileCopy,bufferedOutputStreamSizeForFileCopy);
				double copyTime = (System.currentTimeMillis()-startTime)/1000.0;
				slf4jlogger.info("Copy time {} seconds",copyTime);
				long lengthOfFile = new File(inFile).length();
				double lengthOfFileInMB = ((double)lengthOfFile)/(1024*1024);
				double copyRate = lengthOfFileInMB/copyTime;
				slf4jlogger.info("Copy rate {} MB/s",copyRate);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
}





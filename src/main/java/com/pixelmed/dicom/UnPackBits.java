/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.ByteBuffer;

public class UnPackBits {

	private static final int decodeOnePair(InputStream i,OutputStream o) throws IOException {
		int n = 0;
		int count = i.read();	// is not sign extended, -1 is EOF
		if (count >= 0) {
//System.err.println("count = "+count+" (0x"+Integer.toHexString(count)+")");
			if (count >= 128) {
				count = 256 - count + 1;
//System.err.println("run length = "+count);
				int value = i.read();
//System.err.println("repeating value = "+value+" (0x"+Integer.toHexString(value)+")");
				if (value < 0) {
					new EOFException("Premature end of run length encoded stream while reading value");
				}
				while (count-- > 0) {
					o.write(value);
					++n;
				}
			}
			else {
				do {
					int value = i.read();
//System.err.println("value = "+value+" (0x"+Integer.toHexString(value)+")");
					if (value < 0) {
						new EOFException("Premature end of run length encoded stream while reading value");
					}
					o.write(value);
					++n;
				} while (count-- > 0);
			}
		}
		// else -1 is end of input ... return 0 bytes read
		return n;	// is number of bytes written to ByteArrayOutputStream
	}

	private static final int decodeOnePair(InputStream i,ByteBuffer bo) throws IOException {
		int n = 0;
		int count = i.read();	// is not sign extended, -1 is EOF
		if (count >= 0) {
//System.err.println("count = "+count+" (0x"+Integer.toHexString(count)+")");
			if (count >= 128) {
				count = 256 - count + 1;
//System.err.println("run length = "+count);
				int value = i.read();
//System.err.println("repeating value = "+value+" (0x"+Integer.toHexString(value)+")");
				if (value < 0) {
					new EOFException("Premature end of run length encoded stream while reading value");
				}
				while (count-- > 0) {
					if (bo.hasRemaining()) {
						bo.put((byte)value);
						++n;
					}
					else {
//System.err.println("Reached end of buffer");
					}
				}
			}
			else {
				do {
					int value = i.read();
//System.err.println("value = "+value+" (0x"+Integer.toHexString(value)+")");
					if (value < 0) {
						new EOFException("Premature end of run length encoded stream while reading value");
					}
					if (bo.hasRemaining()) {
						bo.put((byte)value);
						++n;
					}
					else {
//System.err.println("Reached end of buffer");
					}
				} while (count-- > 0);
			}
		}
		else {
//System.err.println("read returned EOF");
		}
		// else -1 is end of input ... return 0 bytes read
		return n;	// is number of bytes written to ByteBuffer, since may have stopped writing if reached the end.
	}

	public static final int decode(InputStream i,OutputStream o,int n) throws IOException {
		int total = 0;
		while (n > 0) {
			int done = decodeOnePair(i,o);
			if (done == 0) {
				throw new EOFException("Premature end of run length encoded stream");
			}
			n = n - done;
			total+=done;
		}
		return total;	// NB. may be more than was asked for if expanded run exceeded n
	}

	public static final ByteArrayOutputStream decode(InputStream i) throws IOException {
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		while (decodeOnePair(i,o) > 0);
		return o;
	}

	public static final byte[] decode(byte[] src) throws IOException {
		return decode(new ByteArrayInputStream(src)).toByteArray();
	}
	
	public static final int decode(InputStream i,byte[] dst,int offset,int n) throws IOException {
		ByteBuffer bo = ByteBuffer.wrap(dst,offset,n);
		int total = 0;
		while (n > 0) {
			int done = decodeOnePair(i,bo);
//System.err.println("done = "+done);
			if (done == 0) {
				throw new EOFException("Premature end of run length encoded stream");
			}
			n = n - done;
//System.err.println("n now = "+n);
			total+=done;
//System.err.println("total now = "+total);
		}
		return total;	// NB. may be more than was asked for if expanded run exceeded n
	}
	
}

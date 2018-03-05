/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.util.Date;	// for test timing of routines

/**
 * <p>A class that extends {@link java.io.FilterOutputStream FilterOutputStream} by adding
 * the concept of little and big endian binary value encoding, and supplies functions
 * for writing various sized integer and floating point words.</p>
 *
 * @see com.pixelmed.dicom.BinaryInputStream
 *
 * @author	dclunie
 */
public class BinaryOutputStream extends FilterOutputStream {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/BinaryOutputStream.java,v 1.18 2017/01/24 10:50:36 dclunie Exp $";

	/***/
	boolean bigEndian;
	/***/
	byte buffer[];
	/***/
	protected long byteOffset;
	
	private static final int BYTE_BUFFER_SIZE = 32768;
	
	/**
	 * <p>Get the byte offset where the next byte will be written.</p>
	 *
	 * <p>Will be zero at the start of the file.</p>
	 *
	 * @return	the byte offset
	 */
	public long getByteOffset() { return byteOffset; }

	/**
	 * @param	big	true if big endian, false if little endian
	 */
	protected void localInit(boolean big) {
		bigEndian=big;
		buffer=new byte[8];
		byteOffset = 0;
	}

	// Overload the write(byte[],int,int) method because otherwise the default
	// method in FilterOutputStream calls the single byte write method,
	// which is very slow - fixes [bugs.mrmf] (000230) Much slower network performance when transfer syntaxes differ
	
    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream. 
     * <p>
     * The <code>write</code> method of <code>BinaryOutputStream</code> calls 
     * the corresponding multi-byte write method of one argument of the. 
     * <code>OutputStream</code> supplied in the constructor, and
	 * overrides the default use of multiple calls to the single-byte
	 * write method that is the behavior of <code>FilterOutputStream</code>. 
     * <p>
     * The exceptions thrown are whatever is thrown by the <code>OutputStream</code> write method.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @throws  IOException  if an I/O error occurs.
     */
	public void write(byte b[],int off,int len) throws IOException {
//System.err.println("BinaryOutputStream.write(): len = "+len);
		// if len is really big, out.write() may fail with OutOfMemoryError, so do it in smaller pieces ...
		while (len > 0) {
			int n = len > BYTE_BUFFER_SIZE ? BYTE_BUFFER_SIZE : len;
			out.write(b,off,n);
			len-=n;
			off+=n;
			byteOffset+=n;
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
		out.write(b);
		++byteOffset;
    }

    /**
     * Writes <code>b.length</code> bytes to this output stream. 
     *
     * @param      b   the data to be written.
     * @throws  IOException  if an I/O error occurs.
     */
    public void write(byte b[]) throws IOException {
		write(b,0,b.length);	// updates byteOffset
    }

	/**
	 * @param	o	the output stream
	 * @param	big	true if big endian, false if little endian
	 */
	public BinaryOutputStream(OutputStream o,boolean big) {
		super(o);
		localInit(big);
	}

	//public BinaryFileOutputStream(String name,boolean big) throws FileNotFoundException {
	//	super(name);
	//	localInit(big);
	//}

	//public BinaryFileOutputStream(FileDescriptor fdObj,boolean big) {
	//	super(fdObj);
	//	localInit(big);
	//}

	//public BinaryFileOutputStream(File file,boolean big) throws FileNotFoundException {
	//	super(file);
	//	localInit(big);
	//}

	/**
	 * <p>Is the stream byte order big endian ?</p>
	 *
	 * @return	true if big endian, false if little endian
	 */
	public boolean isBigEndian() { return bigEndian; }

	/**
	 * <p>Is the stream byte order little endian ?</p>
	 *
	 * @return	true if little endian, false if big endian
	 */
	public boolean isLittleEndian() { return !bigEndian; }

	/**
	 * <p>Set the stream byte order to big endian.</p>
	 */
	public void setBigEndian() { bigEndian=true; }

	/**
	 * <p>Set the stream byte order to little endian.</p>
	 */
	public void setLittleEndian() { bigEndian=false; }

	/**
	 * <p>Set the stream byte order to that specified.</p>
	 *
	 * @param	big	true if to set to big endian, false if little endian
	 */
	public void setEndian(boolean big) {
		bigEndian=big;
	}

	/**
	 * @param	v
	 */
	final void insertUnsigned8(int v) {
			buffer[0]=(byte)v;
	}

	/**
	 * @param	v
	 */
	final void insertUnsigned16(int v) {
		if (bigEndian) {
			buffer[0]=(byte)(v>>8);
			buffer[1]=(byte)v;
		}
		else {
			buffer[1]=(byte)(v>>8);
			buffer[0]=(byte)v;
		}
	}

	/**
	 * @param	v
	 */
	final void insertSigned16(int v) {
		insertUnsigned16(v);
	}

	/**
	 * @param	v
	 */
	final void insertUnsigned32(long v) {
		if (bigEndian) {
			buffer[0]=(byte)(v>>24);
			buffer[1]=(byte)(v>>16);
			buffer[2]=(byte)(v>>8);
			buffer[3]=(byte)v;
		}
		else {
			buffer[3]=(byte)(v>>24);
			buffer[2]=(byte)(v>>16);
			buffer[1]=(byte)(v>>8);
			buffer[0]=(byte)v;
		}
	}

	/**
	 * @param	v
	 */
	final void insertSigned32(long v) {
		insertUnsigned32(v);
	}

	/**
	 * @param	v
	 */
	final void insertUnsigned64(long v) {
		if (bigEndian) {
			buffer[0]=(byte)(v>>56);
			buffer[1]=(byte)(v>>48);
			buffer[2]=(byte)(v>>40);
			buffer[3]=(byte)(v>>32);
			buffer[4]=(byte)(v>>24);
			buffer[5]=(byte)(v>>16);
			buffer[6]=(byte)(v>>8);
			buffer[7]=(byte)v;
		}
		else {
			buffer[7]=(byte)(v>>56);
			buffer[6]=(byte)(v>>48);
			buffer[5]=(byte)(v>>40);
			buffer[4]=(byte)(v>>32);
			buffer[3]=(byte)(v>>24);
			buffer[2]=(byte)(v>>16);
			buffer[1]=(byte)(v>>8);
			buffer[0]=(byte)v;
		}
	}

	/**
	 * <p>Write one unsigned integer 8 bit value.</p>
	 *
	 * @param	v		an int containing an unsigned value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeUnsigned8(int v) throws IOException {
		insertUnsigned8(v);
		write(buffer,0,1);
	}

	/**
	 * <p>Write one unsigned integer 16 bit value.</p>
	 *
	 * @param	v		an int containing an unsigned value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeUnsigned16(int v) throws IOException {
		insertUnsigned16(v);
		write(buffer,0,2);
	}

	/**
	 * <p>Write one signed integer 16 bit value.</p>
	 *
	 * @param	v		an int containing an signed value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeSigned16(int v) throws IOException {
		insertSigned16(v);
		write(buffer,0,2);
	}

	/**
	 * <p>Write one unsigned integer 32 bit value.</p>
	 *
	 * @param	v		a long containing an unsigned value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeUnsigned32(long v) throws IOException {
		insertUnsigned32(v);
		write(buffer,0,4);
	}

	/**
	 * <p>Write one signed integer 32 bit value.</p>
	 *
	 * @param	v		a long containing an signed value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeSigned32(long v) throws IOException {
		insertSigned32(v);
		write(buffer,0,4);
	}
	
	/**
	 * <p>Write one floating point 32 bit value.</p>
	 *
	 * @param	value		a float value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeFloat(float value) throws IOException {
		int binary = Float.floatToRawIntBits(value);
		insertUnsigned32(binary);
		write(buffer,0,4);
	}

	/**
	 * <p>Write one floating point 64 bit value.</p>
	 *
	 * @param	value		a double value
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeDouble(double value) throws IOException {
		long binary = Double.doubleToRawLongBits(value);
		insertUnsigned64(binary);
		write(buffer,0,8);
	}

	/**
	 * <p>Write an array of unsigned integer 16 bit values.</p>
	 *
	 * @param	w		an array of integers to write
	 * @param	len		the number of 16 bit values to write
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeUnsigned16(short[] w,int len) throws IOException {
//System.err.println("BinaryOutputStream.writeUnsigned16(): len = "+len);
		// if len is really big, allocation of byte buffer may fail with OutOfMemoryError, so do it in smaller pieces ...
		// don't even bother to test len against BYTE_BUFFER_SIZE, since multiplying short array length by 2 may exceed max positive int (000774)
		if (len > 0) {
			byte  b[] = new byte[BYTE_BUFFER_SIZE];
			int wcount=0;
			while (wcount < len) {
				int bcount = 0;
				if (bigEndian) {
					while (wcount<len && bcount <= (BYTE_BUFFER_SIZE-2)) {
						short value=w[wcount++];
						b[bcount++]=(byte)(value>>8);
						b[bcount++]=(byte)value;
					}
				}
				else {
					while (wcount<len && bcount <= (BYTE_BUFFER_SIZE-2)) {
						short value=w[wcount++];
						b[bcount++]=(byte)value;
						b[bcount++]=(byte)(value>>8);
					}
				}
				write(b,0,bcount);
			}
		}
	}

	/**
	 * <p>Write an array of unsigned integer 32 bit values.</p>
	 *
	 * @param	w		an array of integers to write
	 * @param	len		the number of 32 bit values to write
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeUnsigned32(int[] w,int len) throws IOException {
//System.err.println("BinaryOutputStream.writeUnsigned32(): len = "+len);
		// if len is really big, allocation of byte buffer may fail with OutOfMemoryError, so do it in smaller pieces ...
		// don't even bother to test len against BYTE_BUFFER_SIZE, since multiplying int array length by 4 may exceed max positive int
		if (len > 0) {
			byte  b[] = new byte[BYTE_BUFFER_SIZE];
			int wcount=0;
			while (wcount < len) {
				int bcount = 0;
				if (bigEndian) {
					while (wcount<len && bcount <= (BYTE_BUFFER_SIZE-4)) {
						int value=w[wcount++];
						b[bcount++]=(byte)(value>>24);
						b[bcount++]=(byte)(value>>16);
						b[bcount++]=(byte)(value>>8);
						b[bcount++]=(byte)value;
					}
				}
				else {
					while (wcount<len && bcount <= (BYTE_BUFFER_SIZE-4)) {
						int value=w[wcount++];
						b[bcount++]=(byte)value;
						b[bcount++]=(byte)(value>>8);
						b[bcount++]=(byte)(value>>16);
						b[bcount++]=(byte)(value>>24);
					}
				}
				write(b,0,bcount);
			}
		}
	}

	/**
	 * <p>Write an array of floating point 32 bit values.</p>
	 *
	 * @param	f		an array of floats to write
	 * @param	len		the number of values to write
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeFloat(float[] f,int len) throws IOException {
		for (int i=0; i<len; ++i) writeFloat(f[i]);
	}

	/**
	 * <p>Write an array of floating point 64 bit values.</p>
	 *
	 * @param	f		an array of floats to write
	 * @param	len		the number of values to write
	 * @throws	IOException	if an I/O error occurs
	 */
	public final void writeDouble(double[] f,int len) throws IOException {
		for (int i=0; i<len; ++i) writeDouble(f[i]);
	}

	/**
	 * @param	msg
	 * @param	n
	 */
	private final void dumpBuffer(String msg,int n) {
		System.err.print("after "+msg);
		for (int i=0; i<n; ++i) {
			System.err.print(" 0x"+Integer.toHexString(buffer[i]&0xff));
		}
		System.err.println();
	}

	/**
	 * @return	the underlying stream
	 */
	//public OutputStream getUnderlyingOutputStream() {
	//	return out;	// a protected field in java.io.FilteredOutputStream
	//}

	/**
	 * <p>For testing.</p>
	 *
	 * @param	arg file to write to
	 */
	public static void main(String arg[]) {

		// little endian ...

		BinaryOutputStream o=null;
		try {
			o = new BinaryOutputStream(new FileOutputStream(arg[0]),false);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(0);
		}

		o.insertUnsigned8(0xff);
		o.dumpBuffer("After insertUnsigned8(0xff)",1);
		o.insertUnsigned16(0xff);
		o.dumpBuffer("After insertUnsigned16(0xff)",2);
		o.insertUnsigned32(0xff);
		o.dumpBuffer("After insertUnsigned32(0xff)",4);
		
		o.insertUnsigned8(0xffff);
		o.dumpBuffer("After insertUnsigned8(0xffff)",1);
		o.insertUnsigned16(0xffff);
		o.dumpBuffer("After insertUnsigned16(0xffff)",2);
		o.insertUnsigned32(0xffff);
		o.dumpBuffer("After insertUnsigned32(0xffff)",4);
		
		o.insertUnsigned8(0xffffff);
		o.dumpBuffer("After insertUnsigned8(0xffffff)",1);
		o.insertUnsigned16(0xffffff);
		o.dumpBuffer("After insertUnsigned16(0xffffff)",2);
		o.insertUnsigned32(0xffffff);
		o.dumpBuffer("After insertUnsigned32(0xffffff)",4);
		
		o.insertUnsigned8(0xffffffff);
		o.dumpBuffer("After insertUnsigned8(0xffffffff)",1);
		o.insertUnsigned16(0xffffffff);
		o.dumpBuffer("After insertUnsigned16(0xffffffff)",2);
		o.insertUnsigned32(0xffffffff);
		o.dumpBuffer("After insertUnsigned32(0xffffffff)",4);


		o.insertUnsigned8(0x7f);
		o.dumpBuffer("After insertUnsigned8(0x7f)",1);
		o.insertUnsigned16(0x7f);
		o.dumpBuffer("After insertUnsigned16(0x7f)",2);
		o.insertUnsigned32(0x7f);
		o.dumpBuffer("After insertUnsigned32(0x7f)",4);
		
		o.insertUnsigned8(0x7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f)",1);
		o.insertUnsigned16(0x7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f)",2);
		o.insertUnsigned32(0x7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f)",4);
		
		o.insertUnsigned8(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f7f)",1);
		o.insertUnsigned16(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f7f)",2);
		o.insertUnsigned32(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f7f)",4);
		
		o.insertUnsigned8(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f7f7f)",1);
		o.insertUnsigned16(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f7f7f)",2);
		o.insertUnsigned32(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f7f7f)",4);

		// big endian ...

		try {
			o = new BinaryOutputStream(new FileOutputStream(arg[0]),true);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(0);
		}

		o.insertUnsigned8(0xff);
		o.dumpBuffer("After insertUnsigned8(0xff)",1);
		o.insertUnsigned16(0xff);
		o.dumpBuffer("After insertUnsigned16(0xff)",2);
		o.insertUnsigned32(0xff);
		o.dumpBuffer("After insertUnsigned32(0xff)",4);
		
		o.insertUnsigned8(0xffff);
		o.dumpBuffer("After insertUnsigned8(0xffff)",1);
		o.insertUnsigned16(0xffff);
		o.dumpBuffer("After insertUnsigned16(0xffff)",2);
		o.insertUnsigned32(0xffff);
		o.dumpBuffer("After insertUnsigned32(0xffff)",4);
		
		o.insertUnsigned8(0xffffff);
		o.dumpBuffer("After insertUnsigned8(0xffffff)",1);
		o.insertUnsigned16(0xffffff);
		o.dumpBuffer("After insertUnsigned16(0xffffff)",2);
		o.insertUnsigned32(0xffffff);
		o.dumpBuffer("After insertUnsigned32(0xffffff)",4);
		
		o.insertUnsigned8(0xffffffff);
		o.dumpBuffer("After insertUnsigned8(0xffffffff)",1);
		o.insertUnsigned16(0xffffffff);
		o.dumpBuffer("After insertUnsigned16(0xffffffff)",2);
		o.insertUnsigned32(0xffffffff);
		o.dumpBuffer("After insertUnsigned32(0xffffffff)",4);


		o.insertUnsigned8(0x7f);
		o.dumpBuffer("After insertUnsigned8(0x7f)",1);
		o.insertUnsigned16(0x7f);
		o.dumpBuffer("After insertUnsigned16(0x7f)",2);
		o.insertUnsigned32(0x7f);
		o.dumpBuffer("After insertUnsigned32(0x7f)",4);
		
		o.insertUnsigned8(0x7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f)",1);
		o.insertUnsigned16(0x7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f)",2);
		o.insertUnsigned32(0x7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f)",4);
		
		o.insertUnsigned8(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f7f)",1);
		o.insertUnsigned16(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f7f)",2);
		o.insertUnsigned32(0x7f7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f7f)",4);
		
		o.insertUnsigned8(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned8(0x7f7f7f7f)",1);
		o.insertUnsigned16(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned16(0x7f7f7f7f)",2);
		o.insertUnsigned32(0x7f7f7f7f);
		o.dumpBuffer("After insertUnsigned32(0x7f7f7f7f)",4);
	}
}





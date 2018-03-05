/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.HexDump;
import com.pixelmed.utils.ByteArray;

import java.util.LinkedList;
import java.util.ListIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author	dclunie
 */
public class PresentationDataValue {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/PresentationDataValue.java,v 1.18 2017/01/24 10:50:45 dclunie Exp $";

	private byte[] b;

	private byte[] value;

	private int itemLength;
	private byte presentationContextID;
	private byte messageControlHeader;

	/**
	 * @param	presentationContextID
	 * @param	value
	 * @param	isCommand
	 * @param	isLastFragment
	 * @throws	DicomNetworkException
	 */
	public PresentationDataValue(byte presentationContextID,byte[] value,boolean isCommand,boolean isLastFragment) throws DicomNetworkException {

		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		this.value=value;
		messageControlHeader = (byte)(((isLastFragment ? 1 : 0) << 1) | (isCommand ? 1 : 0));

		b = new byte[6+value.length];

		itemLength = value.length+2;
		b[0]=(byte)(itemLength>>24);						// big endian
		b[1]=(byte)(itemLength>>16);
		b[2]=(byte)(itemLength>>8);
		b[3]=(byte)itemLength;

		b[4]=presentationContextID;
		b[5]=messageControlHeader;
		System.arraycopy(value,0,b,6,value.length);
	}

	/**
	 * @param	buf
	 * @param	offset
	 * @param	length
	 * @throws	DicomNetworkException
	 */
	public PresentationDataValue(byte[] buf,int offset,int length) throws DicomNetworkException {
//System.err.println("PresentationDataValue buf.length="+buf.length);
//System.err.println("PresentationDataValue offset="+offset);
//System.err.println("PresentationDataValue length="+length);
		itemLength=length;
		presentationContextID=buf[offset+4];
		messageControlHeader=buf[offset+5];
		//value = ByteArray.extractBytes(buf,offset+6,length-2);
		value = length >= 2 ? ByteArray.extractBytes(buf,offset+6,length-2) : null;
		b     = ByteArray.extractBytes(buf,offset,length+4);
	}

	/***/
	public byte[] getBytes() { return b; }

	/***/
	public byte[] getValue() { return value; }
	
	/***/
	public boolean isLastFragment()	{ return (messageControlHeader & 0x02) != 0; }
	/***/
	public boolean isCommand()	{ return (messageControlHeader & 0x01) != 0; }
	
	/***/
	public byte getPresentationContextID() { return presentationContextID; }

	/***/
	public String toString() {
		return HexDump.dump(b);
	}
}






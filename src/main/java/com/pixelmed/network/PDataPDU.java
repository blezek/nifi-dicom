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
public class PDataPDU {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/PDataPDU.java,v 1.18 2017/01/24 10:50:45 dclunie Exp $";

	private byte[] b;

	private int pduType;
	private int pduLength;

	private LinkedList pdvList;

	/**
	 * @param	pdvList
	 * @throws	DicomNetworkException
	 */
	public PDataPDU(LinkedList pdvList) throws DicomNetworkException {

		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		pduType = 0x04;
		this.pdvList=pdvList;

		ByteArrayOutputStream bo = new ByteArrayOutputStream(16384);

		// encode fixed length part ...

		bo.write((byte)pduType);					// P-DATA-TF PDU Type
		bo.write(0x00);							// reserved
		bo.write(0x00); bo.write(0x00); bo.write(0x00); bo.write(0x00);	// will fill in length here later

		// encode variable length part ...

		// one or more Presentation Data Values ...

		ListIterator i = pdvList.listIterator();
		while (i.hasNext()) {
			PresentationDataValue pdv = (PresentationDataValue)i.next();
			byte[] bpdv = pdv.getBytes();
			bo.write(bpdv,0,bpdv.length);
		}

		// compute size and fill in length field ...

		pduLength = bo.size()-6;

		b = bo.toByteArray();

		b[2]=(byte)(pduLength>>24);						// big endian
		b[3]=(byte)(pduLength>>16);
		b[4]=(byte)(pduLength>>8);
		b[5]=(byte)pduLength;
	}

	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public PDataPDU(byte[] pdu) throws DicomNetworkException {
		b=pdu;
//System.err.print("PDataPDU="+this);
		pduType = b[0]&0xff;
		pduLength = ByteArray.bigEndianToUnsignedInt(b,2,4);
//System.err.println("PDataPDU pduLength="+pduLength);

		pdvList = new LinkedList();
		int offset = 6;
		while (offset < b.length) {
//System.err.println("PDataPDU offset="+offset);
			int pdvLength = ByteArray.bigEndianToUnsignedInt(b,offset,4);
			if (pdvLength < 2) {
				throw new DicomNetworkException("Illegal length in PDV = "+pdvLength+", must be >= 2");
			}
//System.err.println("PDataPDU pdvLength="+pdvLength);
			if (pdvLength > 0) pdvList.add(new PresentationDataValue(b,offset,pdvLength));
			offset+=pdvLength+4;
		}
	}

	/***/
	public byte[] getBytes() { return b; }

	/***/
	public LinkedList getPDVList() { return pdvList; }

	/***/
	public boolean containsLastCommandFragment() {
		boolean found=false;
		if (pdvList != null && pdvList.size() > 0) {
			// need to iterate through fragments, since data may follow last fragment of command
			ListIterator i = pdvList.listIterator();
			while (i.hasNext()) {
				PresentationDataValue pdv = (PresentationDataValue)i.next();
				if (pdv.isLastFragment() && pdv.isCommand()) {
					found=true;
					break;
				}
			}
		}
		return found;
	}

	/***/
	public boolean containsLastDataFragment() {
		boolean found=false;
		if (pdvList != null && pdvList.size() > 0) {
			// only need to look at last fragment, since nothing can come after data
			PresentationDataValue pdv = (PresentationDataValue)(pdvList.getLast());
			if (pdv.isLastFragment() && !pdv.isCommand()) found=true;
		}
		return found;
	}

	/***/
	public String toString() {
//Thread.currentThread().dumpStack();
		return HexDump.dump(b);
	}
}





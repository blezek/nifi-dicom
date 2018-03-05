/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;

/**
 * @author	dclunie
 */
class AReleasePDU {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AReleasePDU.java,v 1.9 2017/01/24 10:50:44 dclunie Exp $";

	private byte[] b;

	private int pduType;
	private int pduLength;

	/**
	 * @param	pduType
	 * @throws	DicomNetworkException
	 */
	public AReleasePDU(int pduType) throws DicomNetworkException {

		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		this.pduType = pduType;		// 0x05 for -RQ, 0x06 for -RP

		b = new byte[10];

		// encode fixed length part ...

		b[0]=(byte)pduType;						// A-RELEASE-xx PDU Type
		b[1]=0x00;							// reserved
		pduLength = 4;
		b[2]=(byte)(pduLength>>24);					// big endian
		b[3]=(byte)(pduLength>>16);
		b[4]=(byte)(pduLength>>8);
		b[5]=(byte)pduLength;
		b[6]=0x00;							// reserved
		b[7]=0x00;							// reserved
		b[8]=0x00;							// reserved
		b[9]=0x00;							// reserved
	}

	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public AReleasePDU(byte[] pdu) throws DicomNetworkException {
		b=pdu;
		pduType = b[0]&0xff;
		pduLength = ByteArray.bigEndianToUnsignedInt(b,2,4);
	}

	/***/
	public byte[] getBytes() { return b; }

	/***/
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("PDU Type: 0x");
		sb.append(pduType);
		sb.append(pduType == 0x05 ? " (A-RELEASE-RQ)" : (pduType == 0x06 ? " (A-RELEASE-RP)" : " unrecognized"));
		sb.append("\n");

		sb.append("Length: 0x");
		sb.append(Integer.toHexString(pduLength));
		sb.append("\n");

		return sb.toString();
	}

}







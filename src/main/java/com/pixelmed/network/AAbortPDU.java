/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;

/**
 * @author	dclunie
 */
class AAbortPDU {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AAbortPDU.java,v 1.11 2017/01/24 10:50:44 dclunie Exp $";

	private byte[] b;

	private int pduType;
	private int pduLength;
	private int source;
	private int reason;

	/**
	 * @param	source
	 * @param	reason
	 * @throws	DicomNetworkException
	 */
	public AAbortPDU(int source,int reason) throws DicomNetworkException {

		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		pduType = 0x07;
		this.source=source;
		this.reason=reason;

		b = new byte[10];

		// encode fixed length part ...

		b[0]=(byte)pduType;						// A-ABORT PDU Type
		b[1]=0x00;							// reserved
		pduLength = 4;
		b[2]=(byte)(pduLength>>24);					// big endian
		b[3]=(byte)(pduLength>>16);
		b[4]=(byte)(pduLength>>8);
		b[5]=(byte)pduLength;
		b[6]=0x00;							// reserved
		b[7]=0x00;							// reserved
		b[8]=(byte)source;
		b[9]=(byte)reason;
	}

	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public AAbortPDU(byte[] pdu) throws DicomNetworkException {
		b=pdu;
		pduType = b[0]&0xff;
		pduLength = ByteArray.bigEndianToUnsignedInt(b,2,4);
		source = b[8]&0xff;
		reason = b[9]&0xff;
	}

	/***/
	public byte[] getBytes() { return b; }

	/***/
	public String getInfo() {
		StringBuffer sb = new StringBuffer();

		if      (source == 0) sb.append("by DICOM UL Service User");
		else if (source == 2) sb.append("by DICOM UL Service Provider");

		if (source == 2) {
			if      (reason == 0) {
				sb.append(", reason not specified");
			}
			else if (reason == 1) {
				sb.append(", unrecognized PDU");
			}
			else if (reason == 2) {
				sb.append(", unexpected PDU");
			}
			else if (reason == 4) {
				sb.append(", unrecognized PDU parameter");
			}
			else if (reason == 5) {
				sb.append(", unexpected PDU parameter");
			}
			else if (reason == 6) {
				sb.append(", invalid PDU parameter value");
			}
		}

		return sb.toString();
	}

	/***/
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("PDU Type: 0x");
		sb.append(pduType);
		sb.append(pduType == 0x07 ? " (A-ABORT)" : " unrecognized");
		sb.append("\n");

		sb.append("Length: 0x");
		sb.append(Integer.toHexString(pduLength));
		sb.append("\n");

		sb.append("Source: 0x");
		sb.append(Integer.toHexString(source));
		if      (source == 1) sb.append(" (DICOM UL Service User)");
		else if (source == 2) sb.append(" (DICOM UL Service Provider)");
		sb.append("\n");

		sb.append("Reason: 0x");
		sb.append(Integer.toHexString(reason));
		if      (source == 2) {
			if      (reason == 0) {
				sb.append(" (reason not specified)");
			}
			else if (reason == 1) {
				sb.append(" (unrecognized PDU)");
			}
			else if (reason == 2) {
				sb.append(" (unexpected PDU)");
			}
			else if (reason == 4) {
				sb.append(" (unrecognized PDU parameter)");
			}
			else if (reason == 5) {
				sb.append(" (unexpected PDU parameter)");
			}
			else if (reason == 6) {
				sb.append(" (invalid PDU parameter value)");
			}
		}
		sb.append("\n");

		return sb.toString();
	}

}








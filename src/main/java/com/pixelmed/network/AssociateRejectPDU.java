/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;

/**
 * @author	dclunie
 */
class AssociateRejectPDU {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociateRejectPDU.java,v 1.13 2017/01/24 10:50:44 dclunie Exp $";

	private byte[] b;

	private int pduType;
	private int pduLength;
	private int result;
	private int source;
	private int reason;

	/**
	 * @param	result
	 * @param	source
	 * @param	reason
	 * @throws	DicomNetworkException
	 */
	public AssociateRejectPDU(int result,int source,int reason) throws DicomNetworkException {
	
		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		this.pduType=0x03;						// A-ASSOCIATE-RJ PDU
		this.pduLength=4;
		this.result=result;
		this.source=source;
		this.reason=reason;

		b = new byte[10];
		b[0]=(byte)(this.pduType);
		b[1]=0x00;							// reserved
		b[2]=0x00; b[3]=0x00; b[4]=0x00; b[5]=0x04;			// big endian pduLength
		b[6]=0x00;							// reserved
		b[7]=(byte)result;
		b[8]=(byte)source;
		b[9]=(byte)reason;
	}
	
	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public AssociateRejectPDU(byte[] pdu) throws DicomNetworkException {
		b=pdu;
		pduType = b[0]&0xff;
		pduLength = ByteArray.bigEndianToUnsignedInt(b,2,4);
		result = b[7]&0xff;
		source = b[8]&0xff;
		reason = b[9]&0xff;
	}

	/***/
	public byte[] getBytes() { return b; }

	/***/
	public String getInfo() {
		StringBuffer sb = new StringBuffer();

		if      (result == 1) sb.append("rejected-permanent");
		else if (result == 2) sb.append("rejected-transient");

		if      (source == 1) sb.append(" by DICOM UL Service User");
		else if (source == 2) sb.append(" by DICOM UL Service Provider (ACSE related function)");
		else if (source == 3) sb.append(" by DICOM UL Service Provider (Presentation related function)");

		if      (source == 1) {
			if      (reason == 1) {
				sb.append(", no reason given");
			}
			else if (reason == 2) {
				sb.append(", application context name not supported");
			}
			else if (reason == 3) {
				sb.append(", calling AE Title not recognized");
			}
			else if (reason == 7) {
				sb.append(", called AE Title not recognized");
			}
		}
		else if (source == 2) {
			if      (reason == 1) {
				sb.append(", no reason given");
			}
			else if (reason == 2) {
				sb.append(", protocol version not supported");
			}
		}
		else if (source == 3) {
			if      (reason == 1) {
				sb.append(", temporary congestion");
			}
			else if (reason == 2) {
				sb.append(", local limit exceeded");
			}
		}
		return sb.toString();
	}

	/***/
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("PDU Type: 0x");
		sb.append(pduType);
		sb.append(pduType == 0x03 ? " (A-ASSOCIATE-RJ)" : " unrecognized");
		sb.append("\n");

		sb.append("Length: 0x");
		sb.append(Integer.toHexString(pduLength));
		sb.append("\n");

		sb.append("Result: 0x");
		sb.append(Integer.toHexString(result));
		if      (result == 1) sb.append(" (rejected-permanent)");
		else if (result == 2) sb.append(" (rejected-transient)");
		sb.append("\n");

		sb.append("Source: 0x");
		sb.append(Integer.toHexString(source));
		if      (source == 1) sb.append(" (DICOM UL Service User)");
		else if (source == 2) sb.append(" (DICOM UL Service Provider (ACSE related function))");
		else if (source == 3) sb.append(" (DICOM UL Service Provider (Presentation related function))");
		sb.append("\n");

		sb.append("Reason: 0x");
		sb.append(Integer.toHexString(reason));
		if      (source == 1) {
			if      (reason == 1) {
				sb.append(" (no reason given)");
			}
			else if (reason == 2) {
				sb.append(" (application context name not supported)");
			}
			else if (reason == 3) {
				sb.append(" (calling AE Title not recognized)");
			}
			else if (reason == 7) {
				sb.append(" (called AE Title not recognized)");
			}
		}
		else if (source == 2) {
			if      (reason == 1) {
				sb.append(" (no reason given)");
			}
			else if (reason == 2) {
				sb.append(" (protocol version not supported)");
			}
		}
		else if (source == 3) {
			if      (reason == 1) {
				sb.append(" (temporary congestion)");
			}
			else if (reason == 2) {
				sb.append(" (local limit exceeded)");
			}
		}
		sb.append("\n");

		return sb.toString();
	}

}






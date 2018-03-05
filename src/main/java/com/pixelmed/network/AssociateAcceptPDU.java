/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.TransferSyntax;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * @author	dclunie
 */
class AssociateAcceptPDU extends AssociateRequestAcceptPDU {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociateAcceptPDU.java,v 1.13 2017/01/24 10:50:44 dclunie Exp $";

	/**
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityServerResponse	null if no response
	 * @throws	DicomNetworkException
	 */
	public AssociateAcceptPDU(String calledAETitle,String callingAETitle, String implementationClassUID, String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			byte[] userIdentityServerResponse) throws DicomNetworkException {
		super(0x02,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,userIdentityServerResponse);
	}

	/**
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	scuSCPRoleSelections
	 * @throws	DicomNetworkException
	 */
	public AssociateAcceptPDU(String calledAETitle,String callingAETitle, String implementationClassUID, String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections) throws DicomNetworkException {
		super(0x02,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,null);
	}

	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public AssociateAcceptPDU(byte[] pdu) throws DicomNetworkException {
		super(pdu);
		if (pduType != 0x02) throw new DicomNetworkException("Unexpected PDU type 0x"+Integer.toHexString(pduType)+" when expecting A-ASSOCIATE-AC");
	}
	
	/**
	 * @param	oldPresentationContexts
	 */
	static public LinkedList sanitizePresentationContextsForAcceptance(LinkedList oldPresentationContexts) {
//System.err.println("AssociateAcceptPDU.sanitizePresentationContextsForAcceptance(): start");
		// make sure there is no abstract syntax and always one transfer syntax, even if rejected
		// makes a copy and leaves old list alone !
		LinkedList newPresentationContexts = new LinkedList();
		ListIterator pcsi = oldPresentationContexts.listIterator();
		while (pcsi.hasNext()) {
			PresentationContext oldPresentationContext = (PresentationContext)(pcsi.next());
			String transferSyntaxUID = oldPresentationContext.getTransferSyntaxUID();
			newPresentationContexts.add(new PresentationContext(
				oldPresentationContext.getIdentifier(),
				oldPresentationContext.getResultReason(),
				null, /*abstractSyntaxUID*/
				transferSyntaxUID == null ? TransferSyntax.ImplicitVRLittleEndian : transferSyntaxUID));
		}
//System.err.println("AssociateAcceptPDU.sanitizePresentationContextsForAcceptance(): done");
		return newPresentationContexts;
	}
}




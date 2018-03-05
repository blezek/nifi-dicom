/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.utils.CapabilitiesAvailable;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Accept only uncompressed or deflated or bzip but not encapsulated compressed transfer syntaxes, also rejecting implicit VR
 * transfer syntaxes if an explicit VR transfer syntax is offered for the same abstract syntax.</p>
 *
 * @author	dclunie
 */
public class UnencapsulatedExplicitTransferSyntaxSelectionPolicy extends TransferSyntaxSelectionPolicy {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/UnencapsulatedExplicitTransferSyntaxSelectionPolicy.java,v 1.6 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompositeInstanceStoreAbstractSyntaxSelectionPolicy.class);
	
	/**
	 * Accept or reject Presentation Contexts, preferring Explicit over Implicit VR.
	 *
	 * Should be called after Abstract Syntax selection has been performed.
	 *
	 * Should be called before {@link com.pixelmed.network.TransferSyntaxSelectionPolicy#applyExplicitTransferSyntaxPreferencePolicy(LinkedList,int) applyExplicitTransferSyntaxPreferencePolicy()}.
	 *
	 * Does not change the Abstract Syntax.
	 *
	 * @deprecated						SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #applyTransferSyntaxSelectionPolicy(LinkedList,int)} instead.
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) with one or more Transfer Syntaxes
	 * @param	associationNumber		used for debugging messages
	 * @param	debugLevel				ignored
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the Transfer Syntax list culled to the one preferred Transfer Syntax (or empty if none acceptable) and the result/reason field left alone if one of the Transfer Syntaxes was acceptable, or set to "transfer syntaxes not supported (provider rejection)"
	 */
	public LinkedList applyTransferSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel) {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return applyTransferSyntaxSelectionPolicy(presentationContexts,associationNumber);
	}
	
	/**
	 * Accept or reject Presentation Contexts, preferring Explicit over Implicit VR.
	 *
	 * Should be called after Abstract Syntax selection has been performed.
	 *
	 * Should be called before {@link com.pixelmed.network.TransferSyntaxSelectionPolicy#applyExplicitTransferSyntaxPreferencePolicy(LinkedList,int) applyExplicitTransferSyntaxPreferencePolicy()}.
	 *
	 * Does not change the Abstract Syntax.
	 *
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) with one or more Transfer Syntaxes
	 * @param	associationNumber		used for debugging messages
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the Transfer Syntax list culled to the one preferred Transfer Syntax (or empty if none acceptable) and the result/reason field left alone if one of the Transfer Syntaxes was acceptable, or set to "transfer syntaxes not supported (provider rejection)"
	 */
	public LinkedList applyTransferSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber) {
		boolean canUseBzip = CapabilitiesAvailable.haveBzip2Support();
		ListIterator pcsi = presentationContexts.listIterator();
		while (pcsi.hasNext()) {
			PresentationContext pc = (PresentationContext)(pcsi.next());
			boolean foundExplicitVRLittleEndian = false;
			boolean foundImplicitVRLittleEndian = false;
			boolean foundExplicitVRBigEndian = false;
			boolean foundDeflated = false;
			boolean foundBzipped = false;
			List tsuids = pc.getTransferSyntaxUIDs();
			ListIterator tsuidsi = tsuids.listIterator();
			while (tsuidsi.hasNext()) {
				String transferSyntaxUID=(String)(tsuidsi.next());
				if (transferSyntaxUID != null) {
					if      (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)) foundImplicitVRLittleEndian = true;
					else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)) foundExplicitVRLittleEndian = true;
					else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)) foundExplicitVRBigEndian = true;
					else if (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)) foundDeflated = true;
					else if (transferSyntaxUID.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian)) foundBzipped = true;
				}
			}
			// discard old list and make a new one ...
			pc.newTransferSyntaxUIDs();
			// Policy is prefer compressed then explicit (little then big) then implicit in the following order and ignore anything else
			if (foundBzipped && canUseBzip) {
				pc.addTransferSyntaxUID(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian);
			}
			else if (foundDeflated) {
				pc.addTransferSyntaxUID(TransferSyntax.DeflatedExplicitVRLittleEndian);
			}
			else if (foundExplicitVRLittleEndian) {
				pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRLittleEndian);
			}
			else if (foundExplicitVRBigEndian) {
				pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRBigEndian);
			}
			else if (foundImplicitVRLittleEndian) {
				pc.addTransferSyntaxUID(TransferSyntax.ImplicitVRLittleEndian);
			}
			else {
				pc.setResultReason((byte)4);				// transfer syntaxes not supported (provider rejection)
			}
		}
		return presentationContexts;
	}
}

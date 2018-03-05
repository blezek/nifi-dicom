/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.SOPClass;

import java.util.LinkedList;
import java.util.ListIterator;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Accept only SOP Classes for storage of composite instances and verification SOP Classes.</p>
 *
 * @author	dclunie
 */
public class CompositeInstanceStoreAbstractSyntaxSelectionPolicy implements AbstractSyntaxSelectionPolicy {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CompositeInstanceStoreAbstractSyntaxSelectionPolicy.java,v 1.5 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompositeInstanceStoreAbstractSyntaxSelectionPolicy.class);
	
	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Only SOP Classes for storage of composite instances and verification SOP Classes are accepted.
	 *
	 * Should be called before Transfer Syntax selection is performed.
	 *
	 * @deprecated						SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #applyAbstractSyntaxSelectionPolicy(LinkedList,int)} instead.
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID)
	 * @param	associationNumber		used for debugging messages
	 * @param	debugLevel				ignored
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)"
	 */
	public LinkedList applyAbstractSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel) {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return applyAbstractSyntaxSelectionPolicy(presentationContexts,associationNumber);
	}
	
	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Only SOP Classes for storage of composite instances and verification SOP Classes are accepted.
	 *
	 * Should be called before Transfer Syntax selection is performed.
	 *
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID)
	 * @param	associationNumber		used for debugging messages
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)"
	 */
	public LinkedList applyAbstractSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber) {
		ListIterator pcsi = presentationContexts.listIterator();
		while (pcsi.hasNext()) {
			PresentationContext pc = (PresentationContext)(pcsi.next());
			String abstractSyntaxUID = pc.getAbstractSyntaxUID();
			pc.setResultReason(
				SOPClass.isImageStorage(abstractSyntaxUID)
			     || SOPClass.isNonImageStorage(abstractSyntaxUID)
			     || SOPClass.isVerification(abstractSyntaxUID)
				? (byte)0 : (byte)3);	// acceptance :  abstract syntax not supported (provider rejection)			      
		}
		return presentationContexts;
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.LinkedList;
import java.util.ListIterator;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Accept only SOP Classes for storage, query or retrieval of composite instances and verification SOP Classes
 * with uncompressed or deflated or bzip but not encapsulated compressed transfer syntaxes, also rejecting implicit VR
 * transfer syntaxes if an explicit VR transfer syntax is offered for the same abstract syntax.</p>
 *
 * @author	dclunie
 */
public class UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy implements PresentationContextSelectionPolicy {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy.java,v 1.7 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy.class);
	
	protected AbstractSyntaxSelectionPolicy abstractSyntaxSelectionPolicy;
	protected TransferSyntaxSelectionPolicy transferSyntaxSelectionPolicy;
	
	public UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy() {
		abstractSyntaxSelectionPolicy = new CompositeInstanceStoreFindMoveGetAbstractSyntaxSelectionPolicy();
		transferSyntaxSelectionPolicy = new UnencapsulatedExplicitTransferSyntaxSelectionPolicy();
	}
	
	/**
	 * Accept or reject Presentation Contexts.
	 *
	 * Only SOP Classes for storage, query or retrieval of composite instances and verification SOP Classes are accepted.
	 *
	 * @deprecated						SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #applyPresentationContextSelectionPolicy(LinkedList,int)} instead.
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) with one or more Transfer Syntaxes
	 * @param	associationNumber		for debugging messages
	 * @param	debugLevel				ignored
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)" or "transfer syntaxes not supported (provider rejection)" or " no reason (provider rejection)"
	 */
	public LinkedList applyPresentationContextSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel) {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return applyPresentationContextSelectionPolicy(presentationContexts,associationNumber);
	}
	
	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Only SOP Classes for storage, query or retrieval of composite instances and verification SOP Classes are accepted.
	 *
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) with one or more Transfer Syntaxes
	 * @param	associationNumber		for debugging messages
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)" or "transfer syntaxes not supported (provider rejection)" or " no reason (provider rejection)"
	 */
	public LinkedList applyPresentationContextSelectionPolicy(LinkedList presentationContexts,int associationNumber) {
		slf4jlogger.trace("Association[{}]: Presentation contexts requested:\n{}",associationNumber,presentationContexts.toString());
		presentationContexts = abstractSyntaxSelectionPolicy.applyAbstractSyntaxSelectionPolicy(presentationContexts,associationNumber);				// must be called 1st
		slf4jlogger.trace("Association[{}]: Presentation contexts after applyAbstractSyntaxSelectionPolicy:\n{}",associationNumber,presentationContexts.toString());
		presentationContexts = transferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(presentationContexts,associationNumber);				// must be called 2nd
		slf4jlogger.trace("Association[{}]: Presentation contexts after applyTransferSyntaxSelectionPolicy:\n{}",associationNumber,presentationContexts.toString());
		presentationContexts = transferSyntaxSelectionPolicy.applyExplicitTransferSyntaxPreferencePolicy(presentationContexts,associationNumber);	// must be called 3rd
		slf4jlogger.trace("Association[{}]: Presentation contexts after applyExplicitTransferSyntaxPreferencePolicy:\n{}",associationNumber,presentationContexts.toString());
		return presentationContexts;
	}
}

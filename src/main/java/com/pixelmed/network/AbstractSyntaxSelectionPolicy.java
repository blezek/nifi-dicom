/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.LinkedList;

/**
 * <p>Implementations of this interface accept or reject Presentation Contexts from a list based on their Abstract Syntax.</p>
 *
 * @see	com.pixelmed.network.CompositeInstanceStoreFindMoveGetAbstractSyntaxSelectionPolicy
 *
 * @author	dclunie
 */
public interface AbstractSyntaxSelectionPolicy {

	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Should be called before Transfer Syntax selection is performed.
	 *
	 * @deprecated						SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #applyAbstractSyntaxSelectionPolicy(LinkedList,int)} instead.
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID)
	 * @param	associationNumber		used for debugging messages
	 * @param	debugLevel				ignored
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)"
	 */
	public LinkedList applyAbstractSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel);

	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Should be called before Transfer Syntax selection is performed.
	 *
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID)
	 * @param	associationNumber		used for debugging messages
	 * @return							the java.util.LinkedList of {@link PresentationContext PresentationContext} objects, as supplied but with the result/reason field set to either "acceptance" or "abstract syntax not supported (provider rejection)"
	 */
	public LinkedList applyAbstractSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber);
}


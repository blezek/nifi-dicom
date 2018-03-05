/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;

/**
 * <p>This class provides a mechanism to process each identifier response of a C-FIND as it is received.</p>
 *
 * <p>Typically a private sub-class would be declared and instantiated with
 * overriding methods to do something useful with the identifier, rather than
 * the default behavior which is just to dump it to stderr.</p>
 *
 * @see com.pixelmed.network.FindSOPClassSCU
 *
 * @author	dclunie
 */
public class IdentifierHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/IdentifierHandler.java,v 1.10 2017/01/24 10:50:45 dclunie Exp $";

	/**
	 * <p>Called when a response identifier has been received.</p>
	 *
	 * @param	identifier	the list of attributes received
	 */
	public void doSomethingWithIdentifier(AttributeList identifier) throws DicomException {
		System.err.println("IdentifierHandler.doSomethingWithIdentifier():");
		System.err.print(identifier);
	}
}

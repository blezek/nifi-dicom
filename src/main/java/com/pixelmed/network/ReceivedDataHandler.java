/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.DicomException;

import java.util.LinkedList;
import java.util.ListIterator;
import java.io.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This abstract class provides a mechanism to process each PDU as it is received
 * on an association.</p>
 *
 * <p>Typically a private sub-class would be declared and instantiated
 * overriding the <code>sendPDataIndication()</code> method.</p>
 *
 * @see com.pixelmed.network.CompositeResponseHandler
 * @see com.pixelmed.network.StorageSOPClassSCP
 *
 * @author	dclunie
 */
abstract public class ReceivedDataHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ReceivedDataHandler.java,v 1.20 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ReceivedDataHandler.class);

	/***/
	protected boolean done;
	/***/
	protected boolean release;

	/**
	 * Construct a handler to process each PDU as it is received.
	 *
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #ReceivedDataHandler()} instead.
	 * @param	debugLevel	ignored
	 */
	ReceivedDataHandler(int debugLevel) {
		this();
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	/**
	 * Construct a handler to process each PDU as it is received.
	 */
	ReceivedDataHandler() {
		done=false;
		release=false;
	}
	
	/**
	 * Dump a list of the Presentation Data Values supplied to standard error
	 *
	 * @param	pdvList		a java.util.LinkedList of {@link PresentationDataValue PresentationDataValue}
	 */
	protected void dumpPDVList(LinkedList pdvList) {
		ListIterator i = pdvList.listIterator();
		while (i.hasNext()) {
			PresentationDataValue pdv = (PresentationDataValue)i.next();
			System.err.println("Received PDV:");
			System.err.println(pdv);
		}
	}
	
	/**
	 * Log a list of the Presentation Data Values supplied at the debug level
	 *
	 * @param	pdvList		a java.util.LinkedList of {@link PresentationDataValue PresentationDataValue}
	 */
	protected String dumpPDVListToString(LinkedList pdvList) {
		StringBuffer buf = new StringBuffer();
		ListIterator i = pdvList.listIterator();
		while (i.hasNext()) {
			PresentationDataValue pdv = (PresentationDataValue)i.next();
			buf.append("Received PDV:\n");
			buf.append(pdv);
		}
		return buf.toString();
	}

	/**
	 * The code handling the reception of data on an {@link Association Association} calls
	 * this method to indicate that a PDU has been received (a P-DATA-INDICATION).
	 *
	 * @param	pdata		the PDU that was received
	 * @param	association	the association on which the PDU was received
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	abstract public void sendPDataIndication(PDataPDU pdata,Association association) throws DicomNetworkException, DicomException, IOException;

	/**
	 * Are we finished ?
	 *
	 * @return	true when no more PDUs are expected
	 */
	public boolean isDone() { return done; }

	/**
	 * Are we to release the association once done ?
	 *
	 * @return	true when association is to be released once done
	 */
	public boolean isToBeReleased() { return release; }

	/**
	 * The code handling the reception of data on an {@link Association Association} calls
	 * this method to indicate that no more PDUs are expected.
	 *
	 * @param	done	to be set to true when no more PDUs are expected, usually when
	 *			the last fragment of the data (or command, if no data) is seen
	 */
	public void setDone(boolean done) { this.done=done; }

	/**
	 * The code handling the reception of data on an {@link Association Association} calls
	 * this method to indicate that the association is to be released once done.
	 *
	 * @param	release	to be set to true when the assoication is to be released once done
	 */
	public void setRelease(boolean release) { this.release=release; }
}




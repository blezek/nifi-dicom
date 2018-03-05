/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.*;
import java.io.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This abstract class provides a mechanism to process each PDU of a composite response as it is received,
 * such as for evaluating the status of the response for success.</p>
 *
 * <p>Typically a private sub-class would be declared and instantiated with
 * overriding methods to evaluate the success or failure of a
 * storage or query or retrieve response.</p>
 *
 * @see com.pixelmed.network.ReceivedDataHandler
 * @see com.pixelmed.network.StorageSOPClassSCU
 * @see com.pixelmed.network.FindSOPClassSCU
 *
 * @author	dclunie
 */
abstract public class CompositeResponseHandler extends ReceivedDataHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CompositeResponseHandler.java,v 1.21 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompositeResponseHandler.class);

	/***/
	protected byte[] commandReceived;
	/***/
	protected byte[] dataReceived;
	/***/
	protected boolean success;
	/***/
	protected boolean allowData;
	/***/
	protected int status;

	/**
	 * Construct a handler to process each PDU of a composite response as it is received,
	 * evaluating the status of the response for success.
	 *
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #CompositeResponseHandler()} instead.
	 * @param	debugLevel	ignored
	 */
	public CompositeResponseHandler(int debugLevel) {
		this();
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	/**
	 * Construct a handler to process each PDU of a composite response as it is received,
	 * evaluating the status of the response for success.
	 */
	public CompositeResponseHandler() {
		super();
		commandReceived=null;
		dataReceived=null;
		success=false;
		done=false;
		allowData=false;
	}

	
	/**
	 * Extract an {@link com.pixelmed.dicom.AttributeList AttributeList} from the concatenated bytes
	 * that have been assembled from one or more PDUs and which make up an entire
	 * Command or Dataset.
	 *
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getAttributeListFromCommandOrData(byte[],String)} instead.
	 * @param	bytes				the concatenated PDU bytes up to and including the last fragment
	 * @param	transferSyntaxUID	the Transfer Syntax to use to interpret the bytes
	 * @param	debugLevel	ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public static AttributeList getAttributeListFromCommandOrData(byte[] bytes,String transferSyntaxUID,int debugLevel) throws DicomNetworkException, DicomException, IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return getAttributeListFromCommandOrData(bytes,transferSyntaxUID);
	}
	
	/**
	 * Extract an {@link com.pixelmed.dicom.AttributeList AttributeList} from the concatenated bytes
	 * that have been assembled from one or more PDUs and which make up an entire
	 * Command or Dataset.
	 *
	 * @param	bytes				the concatenated PDU bytes up to and including the last fragment
	 * @param	transferSyntaxUID	the Transfer Syntax to use to interpret the bytes
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public static AttributeList getAttributeListFromCommandOrData(byte[] bytes,String transferSyntaxUID) throws DicomNetworkException, DicomException, IOException {
		slf4jlogger.trace(HexDump.dump(bytes));
		AttributeList list = new AttributeList();
		list.read(new DicomInputStream(new ByteArrayInputStream(bytes),transferSyntaxUID,false));
		slf4jlogger.trace(list.toString());
		return list;
	}

	/**
	 * Extract an {@link com.pixelmed.dicom.AttributeList AttributeList} from the concatenated bytes
	 * that have been assembled from one or more PDUs and which make up an entire
	 * Command or Dataset.
	 *
	 * @param	bytes			the concatenated PDU bytes up to and including the last fragment
	 * @param	transferSyntaxUID	the Transfer Syntax to use to interpret the bytes
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	static public String dumpAttributeListFromCommandOrData(byte[] bytes,String transferSyntaxUID) throws DicomNetworkException, DicomException, IOException {
		String dump = null;
		try {
			AttributeList list = new AttributeList();
			list.read(new DicomInputStream(new ByteArrayInputStream(bytes),transferSyntaxUID,false));
			dump = list.toString();
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			dump = null;
		}
		return dump;
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
	public void sendPDataIndication(PDataPDU pdata,Association association) throws DicomNetworkException, DicomException, IOException {
		slf4jlogger.debug("sendPDataIndication():");
		if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug(super.dumpPDVListToString(pdata.getPDVList()));
		// append to command ...
		LinkedList pdvList = pdata.getPDVList();
		ListIterator i = pdvList.listIterator();
		while (i.hasNext()) {
			PresentationDataValue pdv = (PresentationDataValue)i.next();
			if (pdv.isCommand()) {
				commandReceived=ByteArray.concatenate(commandReceived,pdv.getValue());	// handles null cases
				if (pdv.isLastFragment()) {
					slf4jlogger.debug("sendPDataIndication(): last fragment of command seen");
					AttributeList list = getAttributeListFromCommandOrData(commandReceived,TransferSyntax.Default);
					commandReceived=null;
					evaluateStatusAndSetSuccess(list);
					//break;
				}
			}
			else {
				if (allowData) {
					dataReceived=ByteArray.concatenate(dataReceived,pdv.getValue());	// handles null cases
					if (pdv.isLastFragment()) {
						slf4jlogger.debug("sendPDataIndication(): last fragment of data seen");
						AttributeList list = getAttributeListFromCommandOrData(dataReceived,
							association.getTransferSyntaxForPresentationContextID(pdv.getPresentationContextID()));
						makeUseOfDataSet(list);
						dataReceived=null;
						//break;
					}
				}
				else {
					throw new DicomNetworkException("Unexpected data fragment in response PDU");
				}
			}
		}
	}

	/**
	 * Extract the status information from a composite response
	 * and set the status flag accordingly.
	 *
	 * @param	list	the list of Attributes extracted from the bytes of the PDU(s)
	 */
	abstract protected void evaluateStatusAndSetSuccess(AttributeList list);
	
	/**
	 * Ignore any data set in the composite response (unless this method is overridden).
	 *
	 * @param	list	the list of Attributes extracted from the bytes of the PDU(s)
	 */
	protected void makeUseOfDataSet(AttributeList list) {}	// default is to ignore it
		
	/**
	 * Does the response include an indication of success ?
	 *
	 */
	public boolean wasSuccessful() { return success; }

	/**
	 * Get the response status
	 *
	 * Valid only after first calling {@link #evaluateStatusAndSetSuccess(AttributeList) evaluateStatusAndSetSuccess()}
	 *
	 */
	public int getStatus() { return status; }
}




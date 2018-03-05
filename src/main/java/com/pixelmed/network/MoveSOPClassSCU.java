/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class implements the SCU role of C-MOVE SOP Classes.</p>
 *
 * <p>The class has no methods other than the constructor (and a main method for testing). The
 * constructor establishes an association, sends the C-MOVE request, and releases the
 * association.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated.</p>
 *
 * <p>For example, to have MOVESCU command MOVESCP to move a single image instance to STORESCP:</p>
 * <pre>
try {
    AttributeList identifier = new AttributeList();
    { AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue("IMAGE"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.2.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.3.0.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.1.0.0.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    new MoveSOPClassSCU("theirhost","104","MOVESCP","MOVESCU","STORESCP",SOPClass.StudyRootQueryRetrieveInformationModelMove,identifier,0);
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * @author	dclunie
 */
public class MoveSOPClassSCU extends SOPClass {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/MoveSOPClassSCU.java,v 1.24 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MoveSOPClassSCU.class);
	
	protected CMoveResponseHandler responseHandler;
	
	public int getStatus() { return responseHandler.getStatus(); }

	/***/
	protected class CMoveResponseHandler extends CompositeResponseHandler {
	
		private int remainingLastTime;
		private int countPendingResponsesWithoutProgress;

		/**
		 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #CMoveResponseHandler()} instead.
		 * @param	debugLevel	ignored
		 */
		CMoveResponseHandler(int debugLevel) {
			this();
			slf4jlogger.warn("CMoveResponseHandler(): Debug level supplied as constructor argument ignored");
		}

		/***/
		CMoveResponseHandler() {
			super();
			allowData=true;
			remainingLastTime = Integer.MAX_VALUE;
			countPendingResponsesWithoutProgress = 0;
		}
		
		/**
		 * @param	list
		 */
		protected void evaluateStatusAndSetSuccess(AttributeList list) {
			slf4jlogger.trace("CMoveResponseHandler.evaluateStatusAndSetSuccess:");
			// could check all sorts of things, like:
			// - AffectedSOPClassUID is what we sent
			// - CommandField is 0x8021 C-MOVE-RSP
			// - MessageIDBeingRespondedTo is what we sent
			// - DataSetType is 0101 for success (no data set) or other for pending
			// - Status is success and consider associated elements
			//
			// for now just treat success or warning as success (and absence as failure)
			status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
			slf4jlogger.trace("CMoveResponseHandler.evaluateStatusAndSetSuccess: status =0x{}",Integer.toHexString(status));
			slf4jlogger.trace(list.toString());
			// possible statuses at this point are:
			// A701 Refused - Out of Resources - Unable to calculate number of matches
			// A702 Refused - Out of Resources - Unable to perform sub-operations
			// A801 Refused - Move Destination unknown	
			// A900 Failed - Identifier does not match SOP Class	
			// Cxxx Failed - Unable to process	
			// FE00 Cancel - Sub-operations terminated due to Cancel Indication	
			// B000 Warning	Sub-operations Complete - One or more Failures	
			// 0000 Success - Sub-operations Complete - No Failures	
			// FF00 Pending - Matches are continuing
			
			if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("Move {}",(status == 0xFF00 ? "pending" : (status == 0x0000 ? "success" : ("0x"+Integer.toHexString(status)))));
			
			int failedSuboperations = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFailedSuboperations,0);

			success = status == 0x0000;		// success
			
			if (status != 0xFF00/* && failedSuboperations == 0*/) {	// DAC 20160126 Makes no difference whether failedSuboperations is 0 or not - should still always stop on non-pending
				slf4jlogger.info("CMoveResponseHandler.evaluateStatusAndSetSuccess: Stopping because status not pending response");
				setDone(true);	// stop if not "pending"
			}
			
			int remaining = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfRemainingSuboperations,0);
			if (status == 0xFF00) {
				if (remaining >= remainingLastTime) {
					slf4jlogger.debug("CMoveResponseHandler.evaluateStatusAndSetSuccess: No progress since last pending response ({} remaining)",remainingLastTime);
					++countPendingResponsesWithoutProgress;
					if (countPendingResponsesWithoutProgress > 100) {
						slf4jlogger.info("CMoveResponseHandler.evaluateStatusAndSetSuccess: Stopping because too many pending responses with no progress");
						setDone(true);	// stop if no progress since last "pending" message
					}
				}
				else {
					countPendingResponsesWithoutProgress = 0;
				}
			}
			remainingLastTime = remaining;
		}

		/**
		 * @param	list
		 */
		protected void makeUseOfDataSet(AttributeList list) {
			slf4jlogger.trace("CMoveResponseHandler.makeUseOfDataSet:\n{}",list.toString());
			// we only get here if there are failed sub-operations, in which case we get a list
			// in Failed SOP Instance UID List (0008,0058)
			setDone(true);
		}
	}

	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getSuitableAssociation(String hostname,int port,String calledAETitle,String callingAETitle,String affectedSOPClass)} instead.
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	debugLevel			ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public static Association getSuitableAssociation(String hostname,int port,String calledAETitle,String callingAETitle,String affectedSOPClass,int debugLevel) throws DicomNetworkException, DicomException, IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return getSuitableAssociation(hostname,port,calledAETitle,callingAETitle,affectedSOPClass);
	}
	
	/**
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public static Association getSuitableAssociation(String hostname,int port,String calledAETitle,String callingAETitle,String affectedSOPClass) throws DicomNetworkException, DicomException, IOException {
		LinkedList presentationContexts = new LinkedList();
		{
			LinkedList tslist = new LinkedList();
			tslist.add(TransferSyntax.Default);
			tslist.add(TransferSyntax.ExplicitVRLittleEndian);
			presentationContexts.add(new PresentationContext((byte)0x01,affectedSOPClass,tslist));
		}
		presentationContexts.add(new PresentationContext((byte)0x03,affectedSOPClass,TransferSyntax.ImplicitVRLittleEndian));
		presentationContexts.add(new PresentationContext((byte)0x05,affectedSOPClass,TransferSyntax.ExplicitVRLittleEndian));

		Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,presentationContexts,null,false);
		slf4jlogger.trace(association.toString());
		return association;
	}

	/**
	 * @param	association			the already established Association to use
	 * @param	moveDestination		the AE Title of the Storage AE to which the instances are to be sent
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	identifier			the list of matching and return keys
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 * @throws	AReleaseException
	 */
	public void performMove(Association association,String moveDestination,String affectedSOPClass,AttributeList identifier) throws DicomNetworkException, DicomException, IOException, AReleaseException {
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("performMove(): request identifier\n{}",identifier.toString());

		// Decide which presentation context we are going to use ...
		byte usePresentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
		slf4jlogger.trace("Using context ID {}",usePresentationContextID);
		byte cMoveRequestCommandMessage[] = new CMoveRequestCommandMessage(affectedSOPClass,moveDestination).getBytes();
		byte cMoveIdentifier[] = new IdentifierMessage(identifier,association.getTransferSyntaxForPresentationContextID(usePresentationContextID)).getBytes();
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("Identifier:\n"+identifier.toString());
		responseHandler = new CMoveResponseHandler();
		association.setReceivedDataHandler(responseHandler);
		// for some reason association.send(usePresentationContextID,cMoveRequestCommandMessage,cMoveIdentifier) fails with Oldenburg imagectn
		// so send the command and the identifier separately ...
		association.send(usePresentationContextID,cMoveRequestCommandMessage,null);
		association.send(usePresentationContextID,null,cMoveIdentifier);
		slf4jlogger.trace("performMove(): waiting for PDUs");
		association.waitForPDataPDUsUntilHandlerReportsDone();
		slf4jlogger.trace("performMove(): got PDU");
		// State 6
	}

	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #MoveSOPClassSCU(Association,String,String,AttributeList)} instead.
	 * @param	association			the already established Association to use
	 * @param	moveDestination		the AE Title of the Storage AE to which the instances are to be sent
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	identifier			the list of unique keys and move level
	 * @param	debugLevel			ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public MoveSOPClassSCU(Association association,String moveDestination,
			String affectedSOPClass,AttributeList identifier,int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(association,moveDestination,affectedSOPClass,identifier);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * @param	association			the already established Association to use
	 * @param	moveDestination		the AE Title of the Storage AE to which the instances are to be sent
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	identifier			the list of unique keys and move level
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public MoveSOPClassSCU(Association association,String moveDestination,
			String affectedSOPClass,AttributeList identifier) throws DicomNetworkException, DicomException, IOException {
		try {
			performMove(association,moveDestination,affectedSOPClass,identifier);
			// State 6
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released and didn't wait for us to do it
			association = null;
		}
		if (!responseHandler.wasSuccessful()) {
			throw new DicomNetworkException("C-MOVE reports failure status 0x"+Integer.toString(responseHandler.getStatus()&0xFFFF,16));
		}
	}

	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #MoveSOPClassSCU(String,int,String,String,String,String,AttributeList)} instead.
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	moveDestination		the AE Title of the Storage AE to which the instances are to be sent
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	identifier			the list of unique keys and move level
	 * @param	debugLevel			ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public MoveSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String moveDestination,
			String affectedSOPClass,AttributeList identifier,int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,moveDestination,affectedSOPClass,identifier);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	moveDestination		the AE Title of the Storage AE to which the instances are to be sent
	 * @param	affectedSOPClass	the SOP Class defining which retrieve model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelMove SOPClass.StudyRootQueryRetrieveInformationModelMove}
	 * @param	identifier			the list of unique keys and move level
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public MoveSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String moveDestination,
			String affectedSOPClass,AttributeList identifier) throws DicomNetworkException, DicomException, IOException {
		Association association = getSuitableAssociation(hostname,port,calledAETitle,callingAETitle,affectedSOPClass);
		try {
			performMove(association,moveDestination,affectedSOPClass,identifier);
			slf4jlogger.trace("releasing association");
			// State 6
			association.release();
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released and didn't wait for us to do it
			association = null;
		}
		if (!responseHandler.wasSuccessful()) {
			throw new DicomNetworkException("C-MOVE reports failure status 0x"+Integer.toString(responseHandler.getStatus()&0xFFFF,16));
		}
	}

	/**
	 * <p>For testing, establish an association to the specified AE and perform a retrieval using a C-MOVE request.</p>
	 *
	 * @param	arg	array of seven, eight or nine strings - their hostname, their port, their AE Title, our AE Title,
	 *			the destination AE, the move level (STUDY, SERIES or IMAGE), then the Study Instance UID,
	 *			optionally the Series Instance UID (if SERIES) and optionally the SOP Instance UID (if IMAGE)
	 */
	public static void main(String arg[]) {
		try {
			AttributeList identifier = new AttributeList();
			{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(arg[5]); identifier.put(t,a); }
			{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[6]); identifier.put(t,a); }
			if (arg.length > 7) { AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[7]); identifier.put(t,a); }
			if (arg.length > 8) { AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[8]); identifier.put(t,a); }
			MoveSOPClassSCU moveSOPClassSCU = new MoveSOPClassSCU(arg[0],Integer.parseInt(arg[1]),arg[2],arg[3],arg[4],SOPClass.StudyRootQueryRetrieveInformationModelMove,identifier);
			slf4jlogger.info("final status = 0x{}",Integer.toHexString(moveSOPClassSCU.getStatus()));
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}





/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class implements the SCU role of C-FIND SOP Classes.</p>
 *
 * <p>The class has no methods other than the constructor (and a main method for testing). The
 * constructor establishes an association, sends the C-FIND request, and releases the
 * association. Any identifiers received are handled by the supplied
 * {@link com.pixelmed.network.IdentifierHandler IdentifierHandler}.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated by using SLF4J properties.</p>
 *
 * <p>For example:</p>
 * <pre>
try {
    SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
    AttributeList identifier = new AttributeList();
    { AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue("STUDY"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.PatientID; Attribute a = new LongStringAttribute(t,specificCharacterSet); a.addValue(""); identifier.put(t,a); }
    { AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(""); identifier.put(t,a); }
    new FindSOPClassSCU("theirhost","104","FINDSCP","FINDSCU",SOPClass.StudyRootQueryRetrieveInformationModelFind,identifier,new IdentifierHandler(),1);
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * @see com.pixelmed.network.IdentifierHandler
 *
 * @author	dclunie
 */
public class FindSOPClassSCU extends SOPClass {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/FindSOPClassSCU.java,v 1.30 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FindSOPClassSCU.class);

	/***/
	private class CFindResponseHandler extends CompositeResponseHandler {
		/***/
		private IdentifierHandler identifierHandler;

		/**
		 * @param	identifierHandler
		 */
		CFindResponseHandler(IdentifierHandler identifierHandler) {
			super();
			this.identifierHandler=identifierHandler;
			allowData=true;
		}
		
		/**
		 * @param	list
		 */
		protected void evaluateStatusAndSetSuccess(AttributeList list) {
			slf4jlogger.debug("evaluateStatusAndSetSuccess:\n{}",list);
			// could check all sorts of things, like:
			// - AffectedSOPClassUID is what we sent
			// - CommandField is 0x8020 C-Find-RSP
			// - MessageIDBeingRespondedTo is what we sent
			// - DataSetType is 0101 for success (no data set) or other for pending
			// - Status is success and consider associated elements
			//
			// for now just treat success or warning as success (and absence as failure)
			status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
			slf4jlogger.debug("CFindResponseHandler.evaluateStatusAndSetSuccess: status = 0x{}",Integer.toHexString(status));
			// possible statuses at this point are:
			// A700 Refused - Out of Resources	
			// A900 Failed - Identifier does not match SOP Class	
			// Cxxx Failed - Unable to process	
			// FE00 Cancel - Matching terminated due to Cancel request	
			// 0000 Success - Matching is complete - No final Identifier is supplied.	
			// FF00 Pending - Matches are continuing - Current Match is supplied and any Optional Keys were supported in the same manner as Required Keys.
			// FF01 Pending - Matches are continuing - Warning that one or more Optional Keys were not supported for existence and/or matching for this Identifier.

			success = status == 0x0000;	// success
			
			if (status != 0xFF00 && status != 0xFF01) {
				slf4jlogger.debug("CFindResponseHandler.evaluateStatusAndSetSuccess: status no longer pending, so stop");
				setDone(true);
			}
		}
		
		/**
		 * @param	list
		 */
		protected void makeUseOfDataSet(AttributeList list) {
			slf4jlogger.debug("CFindResponseHandler.makeUseOfDataSet:\n,{}",list);
			try {
				identifierHandler.doSomethingWithIdentifier(list);
			}
			catch (DicomException e) {
				// do not stop ... other identifiers may be OK
				slf4jlogger.error("Ignoring exception",e);
			}
		}
	}
	
	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getSuitableAssociation(String,int,String,String,String)} instead.
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
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
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
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
		slf4jlogger.debug(association.toString());
		return association;
	}

	/**
	 * @param	association			the already established Association to use
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
	 * @param	identifier			the list of matching and return keys
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 * @throws	AReleaseException
	 */
	public void performFind(Association association,String affectedSOPClass,AttributeList identifier) throws DicomNetworkException, DicomException, IOException, AReleaseException {
		slf4jlogger.debug("request identifier\n{}",identifier);

		// Decide which presentation context we are going to use ...
		byte usePresentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
		slf4jlogger.debug("Using context ID {}",usePresentationContextID);
		byte cFindRequestCommandMessage[] = new CFindRequestCommandMessage(affectedSOPClass).getBytes();
		byte cFindIdentifier[] = new IdentifierMessage(identifier,association.getTransferSyntaxForPresentationContextID(usePresentationContextID)).getBytes();
		// for some reason association.send(usePresentationContextID,cFindRequestCommandMessage,cFindIdentifier) fails with Oldenburg imagectn
		// so send the command and the identifier separately ...
		// (was probably because wasn't setting the last fragment flag on the command in Association.send() DAC. 2004/06/10)
		// (see [bugs.mrmf] (000114) Failing to set last fragment on command when sending command and data in same PDU)
		association.send(usePresentationContextID,cFindRequestCommandMessage,null);
		association.send(usePresentationContextID,null,cFindIdentifier);
		slf4jlogger.debug("waiting for PDUs");
		association.waitForPDataPDUsUntilHandlerReportsDone();
		slf4jlogger.debug("got PDU");
		// State 6
	}

	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #FindSOPClassSCU(Association,String,AttributeList,IdentifierHandler)} instead.
	 * @param	association			the already established Association to use
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
	 * @param	identifier			the list of matching and return keys
	 * @param	identifierHandler	the handler to use for each returned identifier
	 * @param	debugLevel			ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public FindSOPClassSCU(Association association,
			String affectedSOPClass,AttributeList identifier,IdentifierHandler identifierHandler,
			int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(association,affectedSOPClass,identifier,identifierHandler);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * @param	association			the already established Association to use
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
	 * @param	identifier			the list of matching and return keys
	 * @param	identifierHandler	the handler to use for each returned identifier
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public FindSOPClassSCU(Association association,
			String affectedSOPClass,AttributeList identifier,IdentifierHandler identifierHandler
			) throws DicomNetworkException, DicomException, IOException {
		CFindResponseHandler responseHandler = new CFindResponseHandler(identifierHandler);
		association.setReceivedDataHandler(responseHandler);
		try {
			performFind(association,affectedSOPClass,identifier);
			// State 6
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released and didn't wait for us to do it
			association = null;
		}
		if (!responseHandler.wasSuccessful()) {
			throw new DicomNetworkException("C-FIND reports failure status 0x"+Integer.toString(responseHandler.getStatus()&0xFFFF,16));
		}
	}

	/**
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #FindSOPClassSCU(String,int,String,String,String,AttributeList,IdentifierHandler)} instead.
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
	 * @param	identifier			the list of matching and return keys
	 * @param	identifierHandler	the handler to use for each returned identifier
	 * @param	debugLevel			ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public FindSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			String affectedSOPClass,AttributeList identifier,IdentifierHandler identifierHandler,
			int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,affectedSOPClass,identifier,identifierHandler);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * @param	hostname			their hostname or IP address
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelFind SOPClass.StudyRootQueryRetrieveInformationModelFind}
	 * @param	identifier			the list of matching and return keys
	 * @param	identifierHandler	the handler to use for each returned identifier
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public FindSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			String affectedSOPClass,AttributeList identifier,IdentifierHandler identifierHandler
			) throws DicomNetworkException, DicomException, IOException {
		Association association = getSuitableAssociation(hostname,port,calledAETitle,callingAETitle,affectedSOPClass);
		CFindResponseHandler responseHandler = new CFindResponseHandler(identifierHandler);
		association.setReceivedDataHandler(responseHandler);
		try {
			performFind(association,affectedSOPClass,identifier);
			slf4jlogger.debug("releasing association");
			// State 6
			association.release();
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released and didn't wait for us to do it
			association = null;
		}
		if (!responseHandler.wasSuccessful()) {
			throw new DicomNetworkException("C-FIND reports failure status 0x"+Integer.toString(responseHandler.getStatus()&0xFFFF,16));
		}
	}

	/**
	 * <p>For testing, establish an association to the specified AE and perform a study root query (send a C-FIND request),
	 * for all studies.</p>
	 *
	 * @param	arg	array of four strings - their hostname, their port, their AE Title, our AE Title
	 */
	public static void main(String arg[]) {
		try {
			SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
			AttributeList identifier = new AttributeList();
			
			boolean testimagelevel = true;
			
			if (!testimagelevel) {
				identifier.putNewAttribute(TagFromName.QueryRetrieveLevel).addValue("STUDY");
				identifier.putNewAttribute(TagFromName.PatientName,specificCharacterSet);
				identifier.putNewAttribute(TagFromName.PatientID,specificCharacterSet);
				identifier.putNewAttribute(TagFromName.PatientBirthDate);
				identifier.putNewAttribute(TagFromName.PatientSex);
				identifier.putNewAttribute(TagFromName.StudyInstanceUID);
				identifier.putNewAttribute(TagFromName.ReferringPhysicianName,specificCharacterSet);
				identifier.putNewAttribute(TagFromName.ModalitiesInStudy);
				identifier.putNewAttribute(TagFromName.StudyDescription,specificCharacterSet);
				identifier.putNewAttribute(TagFromName.StudyID,specificCharacterSet);
				identifier.putNewAttribute(TagFromName.AccessionNumber,specificCharacterSet);
			
				identifier.putNewAttribute(TagFromName.QueryRetrieveLevel).addValue("SERIES");
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue("1.2.840.113704.1.111.5740.1224249944.1"); identifier.put(a); }
				identifier.putNewAttribute(TagFromName.SeriesInstanceUID);
			}
			else {
				identifier.putNewAttribute(TagFromName.QueryRetrieveLevel).addValue("IMAGE");
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue("1.2.840.113619.2.5.1762386977.1328.985934491.590"); identifier.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue("1.2.840.113619.2.5.1762386977.1328.985934491.643"); identifier.put(a); }
				
				identifier.putNewAttribute(TagFromName.SOPInstanceUID);
				identifier.putNewAttribute(TagFromName.AlternateRepresentationSequence);	// (000671)
				identifier.putNewAttribute(TagFromName.InstanceNumber);
			}
			
			new FindSOPClassSCU(arg[0],Integer.parseInt(arg[1]),arg[2],arg[3],SOPClass.StudyRootQueryRetrieveInformationModelFind,identifier,new IdentifierHandler());
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}





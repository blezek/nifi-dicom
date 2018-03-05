/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

//import com.pixelmed.transfermonitor.MonitoredInputStream;
//import com.pixelmed.transfermonitor.MonitoredOutputStream;
//import com.pixelmed.transfermonitor.TransferMonitoringContext;

import com.pixelmed.utils.ByteArray;
import com.pixelmed.utils.HexDump;
import com.pixelmed.utils.StringUtilities;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TransferSyntax;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
class AssociationAcceptor extends Association {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociationAcceptor.java,v 1.33 2017/01/24 10:50:44 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AssociationAcceptor.class);

	protected PresentationContextSelectionPolicy presentationContextSelectionPolicy;

	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The open association is left in state 6 - Data Transfer.
	 *
	 * @deprecated									SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #AssociationAcceptor(int,int,int,PresentationContextSelectionPolicy)} instead.
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	implementationClassUID				the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	implementationVersionName			the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @param	debugLevel							ignored
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	protected AssociationAcceptor(Socket socket,String calledAETitle,String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			PresentationContextSelectionPolicy presentationContextSelectionPolicy,
			int debugLevel) throws DicomNetworkException, IOException {
		this(socket,calledAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,presentationContextSelectionPolicy);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The open association is left in state 6 - Data Transfer.
	 *
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	implementationClassUID				the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	implementationVersionName			the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
	 * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	protected AssociationAcceptor(Socket socket,String calledAETitle,String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			PresentationContextSelectionPolicy presentationContextSelectionPolicy
			) throws DicomNetworkException, IOException {
		super();
		this.socket=socket;
		this.calledAETitle=calledAETitle;
		callingAETitle=null;
		presentationContexts=null;
		this.presentationContextSelectionPolicy=presentationContextSelectionPolicy;
		//TransferMonitoringContext inputTransferMonitoringContext  = new TransferMonitoringContext("Association["+associationNumber+"] Acceptor  Read  "+calledAETitle+"<-");
		//TransferMonitoringContext outputTransferMonitoringContext = new TransferMonitoringContext("Association["+associationNumber+"] Acceptor  Wrote "+calledAETitle+"->");
		try {
												// AE-5    - TP Connect Indication
												// State 2 - Transport connection open (Awaiting A-ASSOCIATE-RQ PDU)
			setSocketOptions(socket,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize);

												//         - Transport connection confirmed 
			in = socket.getInputStream();
			//in = new MonitoredInputStream(socket.getInputStream(),inputTransferMonitoringContext);
			out = socket.getOutputStream();
			//out = new MonitoredOutputStream(socket.getOutputStream(),outputTransferMonitoringContext);

			byte[] startBuffer =  new byte[6];
			//in.read(startBuffer,0,6);	// block for type and length of PDU
			readInsistently(in,startBuffer,0,6,"type and length of PDU");
			int pduType = startBuffer[0]&0xff;
			int pduLength = ByteArray.bigEndianToUnsignedInt(startBuffer,2,4);

			slf4jlogger.trace("Association[{}]: Them: PDU Type: 0x{} (length 0x{})",associationNumber,Integer.toHexString(pduType),Integer.toHexString(pduLength));

			if (pduType == 0x01) {							//           - A-ASSOCIATE-RQ PDU
												// AE-6      - Stop ARTIM and send A-ASSOCIATE indication primitive
				AssociateRequestPDU arq = new AssociateRequestPDU(getRestOfPDU(in,startBuffer,pduLength));
				slf4jlogger.trace("Association[{}]: Them:\n{}",associationNumber,arq.toString());
				presentationContexts=arq.getRequestedPresentationContexts();
				maximumLengthReceived=arq.getMaximumLengthReceived();
				callingAETitle=StringUtilities.removeLeadingOrTrailingWhitespaceOrISOControl(arq.getCallingAETitle());
				
				// now that we know callingAETitle ...
				//inputTransferMonitoringContext.setDescription( "Association["+associationNumber+"] Acceptor  Read  "+calledAETitle+"<-"+callingAETitle);
				//outputTransferMonitoringContext.setDescription("Association["+associationNumber+"] Acceptor  Wrote "+calledAETitle+"->"+callingAETitle);

				if (!calledAETitle.equals(StringUtilities.removeLeadingOrTrailingWhitespaceOrISOControl(arq.getCalledAETitle()))) {
												//	     - Implicit A-ASSOCIATE response primitive reject
												// AE-8      - Send A-ASSOCIATE-RJ PDU
					AssociateRejectPDU arj = new AssociateRejectPDU(1,1,7);	// rejected permanent, user, called AE title not recognized
					out.write(arj.getBytes());
					out.flush();						// State 13
					
					// At this point AA-6, AA-7, AA-2, AR-5 or AA-7 could be needed,
					// however let's just close the connection and be done with it
					// without worrying about whether the other end is doing the same
					// or has sent a PDU that really should trigger us to send an A-ABORT first
					// and we don't have a timmer to stop
					socket.close();
					//inputTransferMonitoringContext.close();
					//outputTransferMonitoringContext.close();
					// No "indication" is defined in the standard here, but send our own to communicate rejection
					throw new DicomNetworkException("Called AE title requested ("+arq.getCalledAETitle()+") doesn't match ours ("+calledAETitle+") - rejecting association");
												// State 1   - Idle
				}
				else {
												//	     - Implicit A-ASSOCIATE response primitive accept
												// AE-7      - Send A-ASSOCIATE-AC PDU
					presentationContextSelectionPolicy.applyPresentationContextSelectionPolicy(presentationContexts,associationNumber);
					// we now have presentation contexts with 1 AS, 1TS if any accepted, and a result/reason
					LinkedList presentationContextsForAssociateAcceptPDU = AssociateAcceptPDU.sanitizePresentationContextsForAcceptance(presentationContexts);
					slf4jlogger.trace("Association[{}]: Presentation contexts for A-ASSOCIATE-AC:\n{}",associationNumber,presentationContextsForAssociateAcceptPDU.toString());

					slf4jlogger.trace("Association[{}]: OurMaximumLengthReceived={}",associationNumber,ourMaximumLengthReceived);

					// just return any selections asked for, assuming that we support them (e.g. SCP role for C-STOREs for C-GET) ...
					LinkedList scuSCPRoleSelections = arq.getSCUSCPRoleSelections();
					
					AssociateAcceptPDU aac = new AssociateAcceptPDU(calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,
							ourMaximumLengthReceived,presentationContextsForAssociateAcceptPDU,scuSCPRoleSelections);
					out.write(aac.getBytes());
					out.flush();						// State 6
				}
			}
			else if (pduType == 0x07) {						//           - A-ABORT PDU
				AAbortPDU aab = new AAbortPDU(getRestOfPDU(in,startBuffer,pduLength));
				slf4jlogger.trace("Association[{}]: Them:\n{}",associationNumber,aab.toString());
				socket.close();							// AA-2      - Stop ARTIM, close transport connection and indicate abort
				//inputTransferMonitoringContext.close();
				//outputTransferMonitoringContext.close();
				throw new DicomNetworkException("A-ABORT indication - "+aab.getInfo());
												// State 1   - Idle
			}
			else {									//           - Invalid or unrecognized PDU received
			slf4jlogger.trace("Association[{}]: Aborting");

				AAbortPDU aab = new AAbortPDU(0,0);				// AA-1      - Send A-ABORT PDU (service user source, reserved), and start (or restart) ARTIM
				out.write(aab.getBytes());
				out.flush();
												//             issue an A-P-ABORT indication and start ARTIM
												// State 13  - Awaiting Transport connection close
				// should wait for ARTIM but ...
				socket.close();
				//inputTransferMonitoringContext.close();
				//outputTransferMonitoringContext.close();
				throw new DicomNetworkException("A-P-ABORT indication - "+aab.getInfo());
												// State 1   - Idle
			}
		}
		catch (IOException e) {								//           - Transport connection closed (or other error)
			//inputTransferMonitoringContext.close();
			//outputTransferMonitoringContext.close();
			throw new DicomNetworkException("A-P-ABORT indication - "+e);		// AA-5      - Stop ARTIM
												// State 1   - Idle
		}

		// falls through only from State 6 - Data Transfer
	}
	
	/*
	 * Returns a string representation of the object.
	 *
	 * @return	a string representation of the object
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		//sb.append("Association["+associationNumber+"]: Port: "); sb.append(port); sb.append("\n");
		sb.append(super.toString());
		return sb.toString();
	}

	/*
	 * Returns the hostname of the CallingAE.
	 *
	 * For an inbound connection, this is the remote host.
	 *
	 * @return	the hostname, or null if not connected
	 */
	public String getCallingAEHostName() {
		return getRemoteHostName();
	}

	/*
	 * Returns the hostname of the CalledAE.
	 *
	 * For an inbound connection, this is the local host.
	 *
	 * @return	the hostname, or null if not connected
	 */
	public String getCalledAEHostName() {
		return getLocalHostName();
	}

	/*
	 * Returns the port of the CallingAE.
	 *
	 * For an inbound connection, this is the remote port.
	 *
	 * @return	the port, or -1 if not connected
	 */
	public int getCallingAEPort() {
		return getRemotePort();
	}

	/*
	 * Returns the port of the CalledAE.
	 *
	 * For an inbound connection, this is the local port.
	 *
	 * @return	the port, or -1 if not connected
	 */
	public int getCalledAEPort() {
		return getLocalPort();
	}

	//public static void main(String arg[]) {
	//	try {
	//		new AssociationAcceptor(arg[0],arg[1]);
	//	}
	//	catch (Exception e) {
	//		slf4jlogger.error("", e);;
	//		System.exit(0);
	//	}
	//}
}



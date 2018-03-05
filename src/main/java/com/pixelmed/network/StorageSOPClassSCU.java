/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Set;
import java.io.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class implements the SCU role of SOP Classes of the Storage Service Class.</p>
 *
 * <p>The class has no methods other than the constructor (and a main method for testing). The
 * constructor establishes an association, sends the C-STORE request, and releases the
 * association.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated.</p>
 *
 * <p>For example:</p>
 * <pre>
try {
    new StorageSOPClassSCU("theirhost",11112,"STORESCP","STORESCU","/tmp/testfile.dcm","1.2.840.10008.5.1.4.1.1.7","1.3.6.1.4.1.5962.1.1.0.0.0.1064923879.2077.3232235877",0,0);
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * <p>From the command line, sending multiple files:</p>
 * <pre>
find /tmp -name '*.dcm' | java -cp pixelmed.jar:lib/additional/commons-codec-1.3.jar:lib/additional/commons-compress-1.12.jar com.pixelmed.network.StorageSOPClassSCU theirhost 11112 STORESCP STORESCU -  0 0
 * </pre>
 *
 *
 * @author	dclunie
 */
public class StorageSOPClassSCU extends SOPClass {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/StorageSOPClassSCU.java,v 1.62 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(StorageSOPClassSCU.class);
	
	/***/
	protected boolean trappedExceptions;
	
	/**
	 * @return	true if in multiple instance constructors exceptions were trapped, e.g., connection or association failure before transfers attempted
	 */
	public boolean encounteredTrappedExceptions() { return trappedExceptions; } 
	
	/***/
	protected class CStoreResponseHandler extends CompositeResponseHandler {
		/**
		 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #CStoreResponseHandler()} instead.
		 * @param	debugLevel	ignored
		 */
		CStoreResponseHandler(int debugLevel) {
			this();
			slf4jlogger.warn("CStoreResponseHandler(): Debug level supplied as constructor argument ignored");
		}
		
		/**
		 */
		CStoreResponseHandler() {
			super();
		}
		
		/**
		 * @param	list
		 */
		protected void evaluateStatusAndSetSuccess(AttributeList list) {
			// could check all sorts of things, like:
			// - AffectedSOPClassUID is what we sent
			// - CommandField is 0x8001 C-STORE-RSP
			// - MessageIDBeingRespondedTo is what we sent
			// - DataSetType is 0101 (no data set)
			// - Status is success and consider associated elements
			// - AffectedSOPInstanceUID is what we sent
			//
			// for now just treat success or warning as success (and absence as failure)
			int status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
			success =  status == 0x0000	// success
				|| status == 0xB000	// coercion of data element
				|| status == 0xB007	// data set does not match SOP Class
				|| status == 0xB006;	// element discarded
		}
	}
	
	/**
	 * @param	association
	 * @param	affectedSOPClass
	 * @param	affectedSOPInstance
	 * @param	inputTransferSyntaxUID
	 * @param	din
	 * @param	presentationContextID
	 * @param	outputTransferSyntaxUID
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 * @throws	AReleaseException
	 */
	protected boolean sendOneSOPInstance(Association association,
			String affectedSOPClass,String affectedSOPInstance,
			String inputTransferSyntaxUID,DicomInputStream din,
			byte presentationContextID,String outputTransferSyntaxUID
			) throws AReleaseException, DicomNetworkException, DicomException, IOException {
		return sendOneSOPInstance(association,affectedSOPClass,affectedSOPInstance,inputTransferSyntaxUID,din,
			presentationContextID,outputTransferSyntaxUID,null,-1);
	}
	
	/**
	 * @param	association
	 * @param	affectedSOPClass
	 * @param	affectedSOPInstance
	 * @param	inputTransferSyntaxUID
	 * @param	din
	 * @param	presentationContextID
	 * @param	outputTransferSyntaxUID
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 * @throws	AReleaseException
	 */
	protected boolean sendOneSOPInstance(Association association,
			String affectedSOPClass,String affectedSOPInstance,
			String inputTransferSyntaxUID,DicomInputStream din,
			byte presentationContextID,String outputTransferSyntaxUID,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) throws AReleaseException, DicomNetworkException, DicomException, IOException {
		byte cStoreRequestCommandMessage[] = new CStoreRequestCommandMessage(affectedSOPClass,affectedSOPInstance,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID).getBytes();
		CStoreResponseHandler receivedDataHandler = new CStoreResponseHandler();
		association.setReceivedDataHandler(receivedDataHandler);
		association.send(presentationContextID,cStoreRequestCommandMessage,null);
		OutputStream out = association.getAssociationOutputStream(presentationContextID);
		if (inputTransferSyntaxUID.equals(outputTransferSyntaxUID)) {
			slf4jlogger.trace("sendOneSOPInstance(): same transfer syntax so raw binary copy");
			CopyStream.copy(din,out);		// be careful ... this will not remove DataSetTrailingPadding, which will kill GE AW
			slf4jlogger.trace("sendOneSOPInstance(): back from raw binary copy");
			out.close();
		}
		else {
			slf4jlogger.trace("sendOneSOPInstance(): different transfer syntaxes; converting {} to {}",inputTransferSyntaxUID,outputTransferSyntaxUID);
			// din will already be positioned after meta-header and set for reading data set
			// copier will push any transfer syntax specific decompression filter onto the stream before reading
			DicomOutputStream dout = new DicomOutputStream(out,null/*meta*/,outputTransferSyntaxUID/*dataset*/);
			new DicomStreamCopier(din,dout);
			// Do not need dout.close() since DicomStreamCopier always closes output stream itself
		}
		slf4jlogger.trace("sendOneSOPInstance(): about to wait for PDUs");
		association.waitForCommandPDataPDUs();
		return receivedDataHandler.wasSuccessful();
	}
	
	/**
	 * @param	association
	 * @param	affectedSOPClass
	 * @param	affectedSOPInstance
	 * @param	list
	 * @param	presentationContextID
	 * @param	outputTransferSyntaxUID
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 * @throws	AReleaseException
	 */
	protected boolean sendOneSOPInstance(Association association,
			String affectedSOPClass,String affectedSOPInstance,
			AttributeList list,
			byte presentationContextID,String outputTransferSyntaxUID,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) throws AReleaseException, DicomNetworkException, DicomException, IOException {
		byte cStoreRequestCommandMessage[] = new CStoreRequestCommandMessage(affectedSOPClass,affectedSOPInstance,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID).getBytes();
		CStoreResponseHandler receivedDataHandler = new CStoreResponseHandler();
		association.setReceivedDataHandler(receivedDataHandler);
		association.send(presentationContextID,cStoreRequestCommandMessage,null);
		OutputStream out = association.getAssociationOutputStream(presentationContextID);
		slf4jlogger.trace("sendOneSOPInstance(): writing attribute list as {}",outputTransferSyntaxUID);
		list.write(out,outputTransferSyntaxUID,false/*useMeta*/,true/*useBufferedStream*/,false/*closeAfterWrite*/);
		slf4jlogger.trace("sendOneSOPInstance(): about to wait for PDUs");
		association.waitForCommandPDataPDUs();
		return receivedDataHandler.wasSuccessful();
	}
	
	/**
	 * <p>Dummy constructor allows testing subclasses to use different constructor.</p>
	 *
	 */
	protected StorageSOPClassSCU() throws DicomNetworkException, DicomException, IOException {
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(String,int,String,String,String,String,String,int)} instead.
	 * @param	hostname					their hostname or IP address
	 * @param	port						their port
	 * @param	calledAETitle				their AE Title
	 * @param	callingAETitle				our AE Title
	 * @param	fileName					the name of the file containing the data set to send
	 * @param	affectedSOPClass			must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance			must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel			0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	debugLevel					ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String fileName,
			String affectedSOPClass,String affectedSOPInstance,int compressionLevel,
			int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @param	hostname					their hostname or IP address
	 * @param	port						their port
	 * @param	calledAETitle				their AE Title
	 * @param	callingAETitle				our AE Title
	 * @param	fileName					the name of the file containing the data set to send
	 * @param	affectedSOPClass			must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance			must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel			0=none,1=propose deflate,2=propose deflate and bzip2
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String fileName,
			String affectedSOPClass,String affectedSOPInstance,int compressionLevel
			) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel,null,-1);
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(String,int,String,String,String,String,String,int,String,int)} instead.
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	fileName								the name of the file containing the data set to send
	 * @param	affectedSOPClass						must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance						must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @param	debugLevel								ignored
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String fileName,
			String affectedSOPClass,String affectedSOPInstance,int compressionLevel,String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID,
			int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	fileName								the name of the file containing the data set to send
	 * @param	affectedSOPClass						must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance						must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,String fileName,
			String affectedSOPClass,String affectedSOPInstance,int compressionLevel,String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID
			) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,0/*ourMaximumLengthReceived*/,0/*socketReceiveBufferSize*/,0/*socketSendBufferSize*/,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	ourMaximumLengthReceived				the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize					the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize					the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	fileName								the name of the file containing the data set to send
	 * @param	affectedSOPClass						must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance						must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			String fileName,String affectedSOPClass,String affectedSOPInstance,int compressionLevel
			) throws DicomNetworkException, DicomException, IOException {
		this(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel,null/*moveOriginatorApplicationEntityTitle*/,-1/*moveOriginatorMessageID*/);
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	ourMaximumLengthReceived				the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize					the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize					the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	fileName								the name of the file containing the data set to send
	 * @param	affectedSOPClass						must be the same as the SOP Class UID contained within the data set, may be null if file has a meta information header
	 * @param	affectedSOPInstance						must be the same as the SOP Instance UID contained within the data set, may be null if file has a meta information header
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			String fileName,String affectedSOPClass,String affectedSOPInstance,int compressionLevel,String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID
			) throws DicomNetworkException, DicomException, IOException {
		slf4jlogger.trace("StorageSOPClassSCU: storing {}",fileName);
		// Don't even begin until we know we can open the file ...
		InputStream in = new BufferedInputStream(new FileInputStream(fileName));
		try {
			String inputTransferSyntax;
			DicomInputStream din = new DicomInputStream(in);
			if (din.haveMetaHeader()) {
				AttributeList metaList = new AttributeList();
				metaList.readOnlyMetaInformationHeader(din);
				slf4jlogger.trace("Meta header information = {}",metaList);
				affectedSOPClass=Attribute.getSingleStringValueOrNull(metaList,TagFromName.MediaStorageSOPClassUID);
				affectedSOPInstance=Attribute.getSingleStringValueOrNull(metaList,TagFromName.MediaStorageSOPInstanceUID);
				inputTransferSyntax=Attribute.getSingleStringValueOrNull(metaList,TagFromName.TransferSyntaxUID);
			}
			else {
				inputTransferSyntax=din.getTransferSyntaxToReadDataSet().getUID();
			}
			slf4jlogger.trace("Using inputTransferSyntax {}",inputTransferSyntax);

			if (affectedSOPClass == null || affectedSOPClass.length() == 0) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Class UID");
			}
			if (SOPClass.isDirectory(affectedSOPClass)) {
				throw new DicomNetworkException("Can't C-STORE Media Storage Directory Storage SOP Class (DICOMDIR)");
			}
			if (affectedSOPInstance == null || affectedSOPInstance.length() == 0) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Instance UID");
			}
		
			PresentationContextListFactory presentationContextListFactory = new PresentationContextListFactory();
			LinkedList presentationContexts = presentationContextListFactory.createNewPresentationContextList(affectedSOPClass,inputTransferSyntax,compressionLevel);

			Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,presentationContexts,null/*scuSCPRoleSelections*/,false/*secureTransport*/,null/*username*/,null/*password*/);
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace(association.toString());
			// Decide which presentation context we are going to use ...
			byte presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
			//int presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass,TransferSyntax.Default);
			slf4jlogger.trace("Using context ID {}",presentationContextID);
			String outputTransferSyntax = association.getTransferSyntaxForPresentationContextID(presentationContextID);
			slf4jlogger.trace("Using outputTransferSyntax {}",outputTransferSyntax);
			if (outputTransferSyntax == null || outputTransferSyntax.length() == 0) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Transfer Syntax (no Presentation Context for Affected SOP Class UID)");
			}
			boolean success = false;
			try {
				success = sendOneSOPInstance(association,affectedSOPClass,affectedSOPInstance,
					inputTransferSyntax,din,
					presentationContextID,outputTransferSyntax,
					moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
				// State 6
				association.release();
			}
			catch (AReleaseException e) {
				// State 1
				// the other end released and didn't wait for us to do it
			}
			slf4jlogger.debug("Send {} {}",fileName,(success ? "succeeded" : "failed"));
		}
		finally {
			in.close();
		}
	}
	
	/**
	 * <p>Send the specified instances contained in the files over an existing association.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(Association,SetOfDicomFiles,MultipleInstanceTransferStatusHandler)} instead.
	 * @param	association								already existing association to SCP
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	multipleInstanceTransferStatusHandler
	 * @param	debugLevel								ignored
	 */
	public StorageSOPClassSCU(Association association,SetOfDicomFiles dicomFiles,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			int debugLevel) {
		this(association,dicomFiles,multipleInstanceTransferStatusHandler);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Send the specified instances contained in the files over an existing association.</p>
	 *
	 * @param	association								already existing association to SCP
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	multipleInstanceTransferStatusHandler
	 */
	public StorageSOPClassSCU(Association association,SetOfDicomFiles dicomFiles,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler) {
		this(association,dicomFiles,multipleInstanceTransferStatusHandler,null,-1);
	}

	/**
	 * <p>Send the specified instances contained in the files over an existing association.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(Association,SetOfDicomFiles,MultipleInstanceTransferStatusHandler,String,int)} instead.
	 * @param	association								already existing association to SCP
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @param	debugLevel								ignored
	 */
	public StorageSOPClassSCU(Association association,SetOfDicomFiles dicomFiles,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID,int debugLevel) {
		this(association,dicomFiles,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Send the specified instances contained in the files over an existing association.</p>
	 *
	 * @param	association								already existing association to SCP
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 */
	public StorageSOPClassSCU(Association association,SetOfDicomFiles dicomFiles,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) {
		try {
			sendMultipleSOPInstances(association,dicomFiles,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released
		}
		catch (DicomNetworkException e) {
			trappedExceptions = true;
			slf4jlogger.error("",e);
		}
		catch (IOException e) {
			trappedExceptions = true;
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(String,int,String,String,SetOfDicomFiles,int,MultipleInstanceTransferStatusHandler)} instead.
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	debugLevel								ignored
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			int debugLevel) {
		this(hostname,port,calledAETitle,callingAETitle,dicomFiles,compressionLevel,multipleInstanceTransferStatusHandler);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler) {
		this(hostname,port,calledAETitle,callingAETitle,dicomFiles,compressionLevel,multipleInstanceTransferStatusHandler,null,-1);
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	ourMaximumLengthReceived				the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize					the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize					the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler) {
		this(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,dicomFiles,compressionLevel,multipleInstanceTransferStatusHandler,null,-1);
	}

	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @deprecated										SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #StorageSOPClassSCU(String,int,String,String,SetOfDicomFiles,int,MultipleInstanceTransferStatusHandler,String,int)} instead.
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @param	debugLevel								ignored
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID,int debugLevel) {
		this(hostname,port,calledAETitle,callingAETitle,dicomFiles,compressionLevel,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}


	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) {
		this(hostname,port,calledAETitle,callingAETitle,0/*ourMaximumLengthReceived*/,0/*socketReceiveBufferSize*/,0/*socketSendBufferSize*/,dicomFiles,compressionLevel,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the files, and release the association.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	ourMaximumLengthReceived				the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize					the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize					the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) {
//long startTime=System.currentTimeMillis();
		if (!dicomFiles.isEmpty()) {
			try {
				PresentationContextListFactory presentationContextListFactory = new PresentationContextListFactory();
				LinkedList presentationContexts = presentationContextListFactory.createNewPresentationContextList(dicomFiles,compressionLevel);
				Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,presentationContexts,null/*scuSCPRoleSelections*/,false/*secureTransport*/,null/*username*/,null/*password*/);
//System.err.println("StorageSOPClassSCU.StorageSOPClassSCU() established association in "+(System.currentTimeMillis()-startTime)+" ms");
			
				sendMultipleSOPInstances(association,dicomFiles,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
			
				association.release();
			}
			catch (AReleaseException e) {
				// State 1
				// the other end released and didn't wait for us to do it
			}
			catch (DicomNetworkException e) {
				trappedExceptions = true;
				slf4jlogger.error("",e);
			}
			catch (IOException e) {
				trappedExceptions = true;
				slf4jlogger.error("",e);
			}
		}
		else {
			slf4jlogger.trace("Not opening an association since no instances to send");
		}
	}

	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the attribute lists, and release the association.</p>
	 *
	 * <p>Deprecated because establishing presentation contexts based on the set of SOP Classes without knowledge of the encoded Transfer Syntax may lead to failure during C-STORE
	 * because of inability to convert; also SLF4J is now used instead of debugLevel parameters to control debugging.</p>
	 *
	 * @deprecated										use {@link #StorageSOPClassSCU(String,int,String,String,SetOfDicomFiles,int,MultipleInstanceTransferStatusHandler)} instead.
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	setOfSOPClassUIDs						the set of SOP Classes contained in the attribute lists
	 * @param	lists									the attribute lists to send
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @param	debugLevel								ignored
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			Set setOfSOPClassUIDs,AttributeList[] lists,
			int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID,int debugLevel) {
		this(hostname,port,calledAETitle,callingAETitle,setOfSOPClassUIDs,lists,compressionLevel,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instances contained in the attribute lists, and release the association.</p>
	 *
	 * <p>Deprecated because establishing presentation contexts based on the set of SOP Classes without knowledge of the encoded Transfer Syntax may lead to failure during C-STORE
	 * because of inability to convert.</p>
	 *
	 * @param	hostname								their hostname or IP address
	 * @param	port									their port
	 * @param	calledAETitle							their AE Title
	 * @param	callingAETitle							our AE Title
	 * @param	setOfSOPClassUIDs						the set of SOP Classes contained in the attribute lists
	 * @param	lists									the attribute lists to send
	 * @param	compressionLevel						0=none,1=propose deflate,2=propose deflate and bzip2
	 * @param	multipleInstanceTransferStatusHandler	transfer handler for reporting pending status (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @deprecated use  {@link com.pixelmed.network.StorageSOPClassSCU#StorageSOPClassSCU(String,int,String,String,SetOfDicomFiles,int,MultipleInstanceTransferStatusHandler) StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			SetOfDicomFiles dicomFiles,int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID)} instead
	 */
	public StorageSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			Set setOfSOPClassUIDs,AttributeList[] lists,
			int compressionLevel,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
			String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) {
//long startTime=System.currentTimeMillis();
		if (lists.length > 0) {
			try {
				PresentationContextListFactory presentationContextListFactory = new PresentationContextListFactory();
				LinkedList presentationContexts = presentationContextListFactory.createNewPresentationContextList(setOfSOPClassUIDs,compressionLevel);
				Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,presentationContexts,null,false);
//System.err.println("StorageSOPClassSCU.StorageSOPClassSCU() established association in "+(System.currentTimeMillis()-startTime)+" ms");
			
				sendMultipleSOPInstances(association,lists,multipleInstanceTransferStatusHandler,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
			
				association.release();
			}
			catch (AReleaseException e) {
				// State 1
				// the other end released and didn't wait for us to do it
			}
			catch (DicomNetworkException e) {
				trappedExceptions = true;
				slf4jlogger.error("",e);
			}
			catch (IOException e) {
				trappedExceptions = true;
				slf4jlogger.error("",e);
			}
		}
		else {
			slf4jlogger.trace("Not opening an association since no instances to send");
		}
	}
			
	/**
	 * <p>Send the specified instances contained in the files over an existing association.</p>
	 *
	 * @param	association								already existing association to SCP
	 * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
	 * @param	multipleInstanceTransferStatusHandler	handler called after each transfer (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	AReleaseException
	 * @throws	DicomNetworkException
	 * @throws	IOException
	 */
	protected void sendMultipleSOPInstances(Association association,SetOfDicomFiles dicomFiles,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
				String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID)
			throws AReleaseException, DicomNetworkException, IOException {
//long startTime=System.currentTimeMillis();
		int nRemaining = dicomFiles.size();
		int nCompleted = 0;
		int nFailed = 0;
		int nWarning = 0;
		{
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace(association.toString());
			Iterator fi = dicomFiles.iterator();
			while (fi.hasNext()) {
				--nRemaining;
				++nCompleted;
				SetOfDicomFiles.DicomFile dicomFile = (SetOfDicomFiles.DicomFile)(fi.next());
				String fileName = dicomFile.getFileName();
				slf4jlogger.trace("Sending {}",fileName);
				boolean success = false;
				String affectedSOPInstance = null;
				try {
					InputStream in = new BufferedInputStream(new FileInputStream(fileName));
					try {
						String inputTransferSyntax = null;
						String affectedSOPClass = null;
						DicomInputStream din = new DicomInputStream(in);
						if (din.haveMetaHeader()) {
							AttributeList metaList = new AttributeList();
							metaList.readOnlyMetaInformationHeader(din);
							slf4jlogger.trace("Meta header information = \n{}",metaList.toString());
							affectedSOPClass=Attribute.getSingleStringValueOrNull(metaList,TagFromName.MediaStorageSOPClassUID);
							affectedSOPInstance=Attribute.getSingleStringValueOrNull(metaList,TagFromName.MediaStorageSOPInstanceUID);
							inputTransferSyntax=Attribute.getSingleStringValueOrNull(metaList,TagFromName.TransferSyntaxUID);
						}
						else {
							affectedSOPClass=dicomFile.getSOPClassUID();
							affectedSOPInstance=dicomFile.getSOPInstanceUID();
							inputTransferSyntax=din.getTransferSyntaxToReadDataSet().getUID();
						}
						slf4jlogger.trace("affectedSOPClass = {}",affectedSOPClass);
						slf4jlogger.trace("affectedSOPInstance = {}",affectedSOPInstance);
						slf4jlogger.trace("inputTransferSyntax = {}",inputTransferSyntax);

						if (affectedSOPClass == null || affectedSOPClass.length() == 0) {
							throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Class UID");
						}
						if (SOPClass.isDirectory(affectedSOPClass)) {
							throw new DicomNetworkException("Can't C-STORE Media Storage Directory Storage SOP Class (DICOMDIR)");
						}
						if (affectedSOPInstance == null || affectedSOPInstance.length() == 0) {
							throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Instance UID");
						}

						// Decide which presentation context we are going to use ...
						byte presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
						//int presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass,TransferSyntax.Default);
						slf4jlogger.trace("Using context ID {}",presentationContextID);
						String outputTransferSyntax = association.getTransferSyntaxForPresentationContextID(presentationContextID);
						slf4jlogger.trace("Using outputTransferSyntax {}",outputTransferSyntax);
						if (outputTransferSyntax == null || outputTransferSyntax.length() == 0) {
							throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Transfer Syntax (no Presentation Context for Affected SOP Class UID)");
						}
				
						success = sendOneSOPInstance(association,affectedSOPClass,affectedSOPInstance,
							inputTransferSyntax,din,
							presentationContextID,outputTransferSyntax,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
						// State 6
					}
					finally {
						in.close();
					}
				}
				catch (DicomNetworkException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				catch (DicomException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				catch (IOException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				if (!success) {
					++nFailed;
					trappedExceptions = true;
				}
				slf4jlogger.debug("Send {} {} between {}",fileName,(success ? "succeeded" : "failed"),association.getEndpointDescription());
				if (multipleInstanceTransferStatusHandler != null) {
					if (multipleInstanceTransferStatusHandler instanceof MultipleInstanceTransferStatusHandlerWithFileName) {
						((MultipleInstanceTransferStatusHandlerWithFileName)multipleInstanceTransferStatusHandler).updateStatus(nRemaining,nCompleted,nFailed,nWarning,affectedSOPInstance,fileName,success);
					}
					else {
						multipleInstanceTransferStatusHandler.updateStatus(nRemaining,nCompleted,nFailed,nWarning,affectedSOPInstance);
					}
				}
			}
			slf4jlogger.debug("Finished sending all files nRemaining={} nCompleted={} nFailed={} nWarning={} between {}",nRemaining,nCompleted,nFailed,nWarning,association.getEndpointDescription());
		}
//System.err.println("StorageSOPClassSCU.sendMultipleSOPInstances() sent "+nCompleted+" files in "+(System.currentTimeMillis()-startTime)+" ms");
	}
			
	/**
	 * <p>Send the specified instances contained in the attribute lists over an existing association.</p>
	 *
	 * @param	association								already existing association to SCP
	 * @param	lists									the array of attribute lists to send
	 * @param	multipleInstanceTransferStatusHandler	handler called after each transfer (may be null if not required)
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	AReleaseException
	 * @throws	DicomNetworkException
	 * @throws	IOException
	 */
	protected void sendMultipleSOPInstances(Association association,AttributeList[] lists,MultipleInstanceTransferStatusHandler multipleInstanceTransferStatusHandler,
				String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID)
			throws AReleaseException, DicomNetworkException, IOException {
//long startTime=System.currentTimeMillis();
		int nRemaining = lists.length;
		int nCompleted = 0;
		int nFailed = 0;
		int nWarning = 0;
		{
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace(association.toString());
			for (int i=0; i<lists.length; ++i) {
				--nRemaining;
				++nCompleted;
				AttributeList list = lists[i];
				boolean success = false;
				String affectedSOPInstance = null;
				try {
					String affectedSOPClass = null;
					affectedSOPClass=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
					affectedSOPInstance=Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
					slf4jlogger.trace("Sending {}",affectedSOPInstance);
					slf4jlogger.trace("affectedSOPClass = {}",affectedSOPClass);

					if (affectedSOPClass == null) {
						throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Class UID");
					}
					if (affectedSOPInstance == null) {
						throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Instance UID");
					}

					// Decide which presentation context we are going to use ...
					byte presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
					//int presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass,TransferSyntax.Default);
					slf4jlogger.trace("Using context ID {}",presentationContextID);
					String outputTransferSyntax = association.getTransferSyntaxForPresentationContextID(presentationContextID);
					slf4jlogger.trace("Using outputTransferSyntax {}",outputTransferSyntax);
					if (outputTransferSyntax == null || outputTransferSyntax.length() == 0) {
						throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Transfer Syntax (no Presentation Context for Affected SOP Class UID)");
					}
				
					success = sendOneSOPInstance(association,affectedSOPClass,affectedSOPInstance,
						list,
						presentationContextID,outputTransferSyntax,moveOriginatorApplicationEntityTitle,moveOriginatorMessageID);
					// State 6
				}
				catch (DicomNetworkException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				catch (DicomException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				catch (IOException e) {
					slf4jlogger.error("",e);
					success=false;
				}
				if (!success) {
					++nFailed;
					trappedExceptions = true;
				}
				slf4jlogger.debug("Send {} {} between {}",affectedSOPInstance,(success ? "succeeded" : "failed"),association.getEndpointDescription());
				if (multipleInstanceTransferStatusHandler != null) {
					multipleInstanceTransferStatusHandler.updateStatus(nRemaining,nCompleted,nFailed,nWarning,affectedSOPInstance);
				}
			}
			slf4jlogger.debug("Finished sending all files nRemaining={} nCompleted={} nFailed={} nWarning={} between {}",nRemaining,nCompleted,nFailed,nWarning,association.getEndpointDescription());
		}
//System.err.println("StorageSOPClassSCU.sendMultipleSOPInstances() sent "+nCompleted+" instances in "+(System.currentTimeMillis()-startTime)+" ms");
	}

	/**
	 * <p>For testing, establish an association to the specified AE and send one or more DICOM instances (C-STORE requests).</p>
	 *
	 * @param	arg	array of six or eight strings - their hostname, their port, their AE Title, our AE Title,
	 *			the filename containing the instance to send (or a hyphen '-' if a list of one or more filenames is to be read from stdin)
	 * 			optionally the SOP Class and the SOP Instance (otherwise will be read from the file(s); if multiple files use an empty string for the SOP Instance),
	 *			the compression level (0=none,1=propose deflate,2=propose deflate and bzip2)
	 */
	public static void main(String arg[]) {
		try {
			String      theirHost=null;
			int         theirPort=-1;
			String   theirAETitle=null;
			String     ourAETitle=null;
			String       fileName=null;
			String    SOPClassUID=null;
			String SOPInstanceUID=null;
			int  compressionLevel=0;
	
			if (arg.length == 8) {
				     theirHost=arg[0];
				     theirPort=Integer.parseInt(arg[1]);
				  theirAETitle=arg[2];
				    ourAETitle=arg[3];
				      fileName=arg[4];
				   SOPClassUID=arg[5];
				SOPInstanceUID=arg[6];
			      compressionLevel=Integer.parseInt(arg[7]);
			}
			else if (arg.length == 6) {
				     theirHost=arg[0];
				     theirPort=Integer.parseInt(arg[1]);
				  theirAETitle=arg[2];
				    ourAETitle=arg[3];
				      fileName=arg[4];
				   SOPClassUID=null;			// figured out by StorageSOPClassSCU() by reading the metaheader
				SOPInstanceUID=null;			// figured out by StorageSOPClassSCU() by reading the metaheader
			      compressionLevel=Integer.parseInt(arg[5]);
			}
			else {
				throw new Exception("Argument list must be 7 or 9 values");
			}
			if (fileName.equals("-")) {
				SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles();
				BufferedReader dicomFileNameReader = new BufferedReader(new InputStreamReader(System.in));
				String dicomFileName = dicomFileNameReader.readLine();
				while (dicomFileName != null) {
					if (SOPClassUID == null) {
						setOfDicomFiles.add(dicomFileName);
					}
					else {
						setOfDicomFiles.add(dicomFileName,SOPClassUID,null,null);	// OK to leave instance and transfer syntax uids as null; only need SOP Class to negotiate
					}
					dicomFileName = dicomFileNameReader.readLine();
				}
//System.err.println(setOfDicomFiles.toString());
				new StorageSOPClassSCU(theirHost,theirPort,theirAETitle,ourAETitle,setOfDicomFiles,compressionLevel,null,null,0);
			}
			else {
				new StorageSOPClassSCU(theirHost,theirPort,theirAETitle,ourAETitle,fileName,SOPClassUID,SOPInstanceUID,compressionLevel);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}





/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;
import com.pixelmed.dicom.*;

import java.io.IOException;

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
    new TestSendingCommandAndDataInOnePDU("theirhost","104","STORESCP","STORESCU","/tmp/testfile.dcm","1.2.840.10008.5.1.4.1.1.7","1.3.6.1.4.1.5962.1.1.0.0.0.1064923879.2077.3232235877",0,0);
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * @author	dclunie
 */
public class TestSendingCommandAndDataInOnePDU extends StorageSOPClassSCU {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/TestSendingCommandAndDataInOnePDU.java,v 1.9 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(TestSendingCommandAndDataInOnePDU.class);

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
			byte presentationContextID,String outputTransferSyntaxUID) throws AReleaseException, DicomNetworkException, DicomException, IOException {
		CStoreResponseHandler receivedDataHandler = new CStoreResponseHandler();
		association.setReceivedDataHandler(receivedDataHandler);
		if (inputTransferSyntaxUID.equals(outputTransferSyntaxUID)) {
			// din will already be positioned after meta-header and set for reading data set
			byte[] data = null;
			byte[] b = new byte[32768];
			while (true) {
				int bytesReceived = din.read(b,0,32768);
				if (bytesReceived == -1) {
					break;
				}
				else if (bytesReceived > 0) {
					if (data == null) {
						data = new byte[bytesReceived];
						System.arraycopy(b,0,data,0,bytesReceived);
					}
					else {
						data = ByteArray.concatenate(data,0,data.length,b,0,bytesReceived);
					}
				}
			}
			byte cStoreRequestCommandMessage[] = new CStoreRequestCommandMessage(affectedSOPClass,affectedSOPInstance).getBytes();
			association.send(presentationContextID,cStoreRequestCommandMessage,data);
		}
		else {
			throw new DicomException("Must be the same transfer syntax");
		}
		slf4jlogger.trace("sendOneSOPInstance(): about to wait for PDUs");
		association.waitForCommandPDataPDUs();
		return receivedDataHandler.wasSuccessful();
	}

	/**
	 * <p>Dummy constructor allows testing subclasses to use different constructor.</p>
	 *
	 */
	protected TestSendingCommandAndDataInOnePDU() throws DicomNetworkException, DicomException, IOException {
	}
	
	/**
	 * <p>Establish an association to the specified AE, send the instance contained in the file, and release the association.</p>
	 *
	 * @param	hostname		their hostname or IP address
	 * @param	port			their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	fileName		the name of the file containing the data set to send
	 * @param	affectedSOPClass	must be the same as the SOP Class UID contained within the data set
	 * @param	affectedSOPInstance	must be the same as the SOP Instance UID contained within the data set
	 * @param	compressionLevel	0=none,1=propose deflate,2=propose deflate and bzip2
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public TestSendingCommandAndDataInOnePDU(String hostname,int port,String calledAETitle,String callingAETitle,String fileName,
			String affectedSOPClass,String affectedSOPInstance,int compressionLevel
			) throws DicomNetworkException, DicomException, IOException {
		super(hostname,port,calledAETitle,callingAETitle,fileName,affectedSOPClass,affectedSOPInstance,compressionLevel);
	}

	/**
	 * <p>For testing, establish an association to the specified AE and send a DICOM instance (send a C-STORE request).</p>
	 *
	 * @param	arg	array of six or eight strings - their hostname, their port, their AE Title, our AE Title,
	 *			the filename containing the instance to send,
	 * 			optionally the SOP Class and the SOP Instance (otherwise will be read from the file),
	 *			and the compression level (0=none,1=propose deflate,2=propose deflate and bzip2)
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
			new TestSendingCommandAndDataInOnePDU(theirHost,theirPort,theirAETitle,ourAETitle,fileName,SOPClassUID,SOPInstanceUID,compressionLevel);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}




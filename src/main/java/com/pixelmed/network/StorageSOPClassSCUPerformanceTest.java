/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;

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
    new StorageSOPClassSCUPerformanceTest("theirhost","104","STORESCP","STORESCU","/tmp/testfile.dcm",0,0);
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * @author	dclunie
 */
public class StorageSOPClassSCUPerformanceTest extends StorageSOPClassSCU {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/StorageSOPClassSCUPerformanceTest.java,v 1.21 2017/01/24 10:50:46 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(StorageSOPClassSCUPerformanceTest.class);
	
	/**
	 * <p>Repeatedly establish an association to the specified AE, send the instance contained in the file the number of times specified, and release the association.</p>
	 *
	 * @param	hostname		their hostname or IP address
	 * @param	port			their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	repeatCount		the number of times to repeat sending the file on the same association
	 * @param	assocnCount		the number of times to repeat establishing the association and sending the file
	 * @param	syntaxCount		the number of transfer syntaxes to propose for each additional presentation context at each association establishment
	 * @param	contextCount		the number of additional presentation contexts to propose at each association establishment
	 * @param	fileNames		the names of the file containing the data set to send
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public StorageSOPClassSCUPerformanceTest(String hostname,int port,String calledAETitle,String callingAETitle,
			int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
			int repeatCount,int assocnCount,int syntaxCount,int contextCount,
			String[] fileNames) throws DicomNetworkException, DicomException, IOException {

		boolean bufferInMemoryFirst = true;

		byte[] entireFile = null;
		if (bufferInMemoryFirst && fileNames.length == 1) {
			long lengthOfFile = new File(fileNames[0]).length();
			// Buffer entire file contents in memory to reduce disk load effect on time ...
			int length = (int)lengthOfFile;
			if (length == lengthOfFile) {
				entireFile = new byte[length];
				try {
					slf4jlogger.debug("Reading entire file into memory first");
					InputStream in = new BufferedInputStream(new FileInputStream(fileNames[0]));
					while (length > 0) {
						int count = in.read(entireFile,0,length);
//System.err.println("StorageSOPClassSCUPerformanceTest: read "+count);
						length-=count;
					}
				}
				catch (Throwable e) {
					slf4jlogger.info("Could not read entire file into memory first ",e);
					entireFile = null;
				}
			}
		}

		int                  numberOfFiles = fileNames.length;
		String[]    affectedSOPClassOfFile = new String[numberOfFiles];
		String[] affectedSOPInstanceOfFile = new String[numberOfFiles];
		String[] inputTransferSyntaxOfFile = new String[numberOfFiles];
		long[]              lengthsOfFiles = new long[numberOfFiles];
		long        totalLengthsOfAllFiles = 0;
		
		HashMap mapOfSOPClassesToSetOfInputTransferSyntaxes = new HashMap();	// to HashSet of String

		for (int i=0; i<numberOfFiles; ++i) {
			String fileName = fileNames[i];
			slf4jlogger.trace("storing {}",fileName);
			long lengthOfFile = new File(fileName).length();
			lengthsOfFiles[i] = lengthOfFile;
			totalLengthsOfAllFiles += lengthOfFile;
			DicomInputStream din = null;
			if (entireFile != null) {
				din = new DicomInputStream(new BufferedInputStream(new ByteArrayInputStream(entireFile)));
			}
			else {
				din = new DicomInputStream(new BufferedInputStream(new FileInputStream(fileName)));
			}
			String affectedSOPClass = null;
			String affectedSOPInstance = null;
			String inputTransferSyntax = null;
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
			din.close();
			slf4jlogger.trace("Using inputTransferSyntax {}",inputTransferSyntax);

			if (affectedSOPClass == null) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Class UID of "+fileName);
			}
			affectedSOPClassOfFile[i]=affectedSOPClass;
			if (affectedSOPInstance == null) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Affected SOP Instance UID of "+fileName);
			}
			affectedSOPInstanceOfFile[i]=affectedSOPInstance;
			if (inputTransferSyntax == null) {
				throw new DicomNetworkException("Can't C-STORE SOP Instance - can't determine Transfer Syntax of "+fileName);
			}
			inputTransferSyntaxOfFile[i]=inputTransferSyntax;
			
			HashSet setOfInputTransferSyntaxesForThisSOPClass = (HashSet)(mapOfSOPClassesToSetOfInputTransferSyntaxes.get(affectedSOPClass));
			if (setOfInputTransferSyntaxesForThisSOPClass == null) {
				setOfInputTransferSyntaxesForThisSOPClass = new HashSet();	// of String
				mapOfSOPClassesToSetOfInputTransferSyntaxes.put(affectedSOPClass,setOfInputTransferSyntaxesForThisSOPClass);
			}
			if (!setOfInputTransferSyntaxesForThisSOPClass.contains(inputTransferSyntax)) {
				slf4jlogger.trace("adding pair of SOP Class {} and Transfer Syntax {}",affectedSOPClass,inputTransferSyntax);
				setOfInputTransferSyntaxesForThisSOPClass.add(inputTransferSyntax);
			}
		}
		
		LinkedList presentationContexts = new LinkedList();
		{		
			int presentationContextID = 0x01;	// always odd numbered, starting with 0x01
			{
				// Add one abstract syntax/transfer syntax combination for each of those encountered in the input files
				Iterator abstractSyntaxes = mapOfSOPClassesToSetOfInputTransferSyntaxes.keySet().iterator();
//System.err.println("keyset= "+mapOfSOPClassesToSetOfInputTransferSyntaxes.keySet());
				while (abstractSyntaxes.hasNext()) {
					String abstractSyntax = (String)(abstractSyntaxes.next());
					HashSet setOfInputTransferSyntaxesForThisAbstractSyntax = (HashSet)(mapOfSOPClassesToSetOfInputTransferSyntaxes.get(abstractSyntax));
					Iterator transferSyntaxes = setOfInputTransferSyntaxesForThisAbstractSyntax.iterator();
					while (transferSyntaxes.hasNext()) {
						String transferSyntax = (String)(transferSyntaxes.next());
						LinkedList tslist = new LinkedList();
						tslist.add(transferSyntax);		// always specify the actual transfer syntax in which the input file is already encoded
						presentationContexts.add(new PresentationContext((byte)presentationContextID,abstractSyntax,tslist));
						presentationContextID+=2;
					}
					if (!setOfInputTransferSyntaxesForThisAbstractSyntax.contains(TransferSyntax.ImplicitVRLittleEndian)) {
						// always include the default transfer syntax if not already, to be compliant with the standard
						LinkedList tslist = new LinkedList();
						tslist.add(TransferSyntax.ImplicitVRLittleEndian);
						presentationContexts.add(new PresentationContext((byte)presentationContextID,abstractSyntax,tslist));
						presentationContextID+=2;
					}
				}
			}
			// Now add a bunch of dummy presentation contexts with private transfer syntaxes to test impact on association acceptor's acceptance time
			while (presentationContextID < 128 && contextCount-- > 0) {
				LinkedList tslist = new LinkedList();
				for (int syntax=0; syntax<syntaxCount; ++syntax) {
					tslist.add("1.3.6.1.4.1.5962.300.99."+Integer.toString(presentationContextID)+"."+Integer.toString(syntax));
				}
				presentationContexts.add(new PresentationContext((byte)presentationContextID,"1.3.6.1.4.1.5962.300.99.0",tslist));
				presentationContextID+=2;
			}
		}
		boolean success = true;
		int assocn=assocnCount;
		long startTime=System.currentTimeMillis();
 		while (success && assocn-- > 0) {
			Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,
				ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,presentationContexts,null,false,null,null);
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace(association.toString());
			try {
				for (int i=0; i<numberOfFiles; ++i) {
					String fileName = fileNames[i];
					String    affectedSOPClass = (String)(   affectedSOPClassOfFile[i]);
					String affectedSOPInstance = (String)(affectedSOPInstanceOfFile[i]);
					String inputTransferSyntax = (String)(inputTransferSyntaxOfFile[i]);

					// Decide which presentation context we are going to use ...
					byte presentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
					slf4jlogger.trace("Using context ID {}",presentationContextID);
					String outputTransferSyntax = association.getTransferSyntaxForPresentationContextID(presentationContextID);
					slf4jlogger.trace("Using outputTransferSyntax {}",outputTransferSyntax);
					int repeat=repeatCount;
					while (success && repeat-- > 0) {
						DicomInputStream din = null;
						if (entireFile != null) {
							din = new DicomInputStream(new BufferedInputStream(new ByteArrayInputStream(entireFile)));
						}
						else {
							din = new DicomInputStream(new BufferedInputStream(new FileInputStream(fileName)));
						}
						if (din.haveMetaHeader()) {
							new AttributeList().readOnlyMetaInformationHeader(din);	// skip the meta-header
						}
						success = sendOneSOPInstance(association,affectedSOPClass,affectedSOPInstance,
							inputTransferSyntax,din,
							presentationContextID,outputTransferSyntax);
						din.close();
						// State 6
					}
				}
				association.release();
			}
			catch (AReleaseException e) {
				// State 1
				// the other end released and didn't wait for us to do it
			}
		}
		slf4jlogger.info("Number of files per set {} files",numberOfFiles);
		slf4jlogger.info("Repeated transfer on same association {} times",repeatCount);
		slf4jlogger.info("Repeated on {} associations",assocnCount);
		double totalTime = (System.currentTimeMillis()-startTime)/1000.0;
		slf4jlogger.info("Total time {} seconds",totalTime);
		double timePerSetOfInstances = totalTime/(repeatCount*assocnCount);
		slf4jlogger.info("Time per set of instances {} seconds",timePerSetOfInstances);
		double lengthOfFilesInMB = ((double)totalLengthsOfAllFiles)/(1024*1024);
		slf4jlogger.info("Total size of data sent {} MB",lengthOfFilesInMB);
		double transferRate = lengthOfFilesInMB/timePerSetOfInstances;
		slf4jlogger.info("Transfer rate {} MB/s",transferRate);
		slf4jlogger.info("File transfer rate {} files/s",(numberOfFiles*repeatCount*assocnCount)/totalTime);
		slf4jlogger.info("Send {}",(success ? "succeeded" : "failed"));
	}

	/**
	 * <p>For testing, establish an association to the specified AE and repeatedly C-STORE the same instance.</p>
	 *
	 * <p>The total number of times the file is transmitted consists of the number of repetitions
	 * per association times the number of association repetitions.</p>
	 *
	 * @param	arg	array of 12 values - their hostname, their port, their AE Title, our AE Title,
	 * 			the maximum PDU length that we will offer to receive,
	 * 			the TCP socket receive buffer size to set (if possible), 0 means leave at the default,
	 * 			the TCP socket send buffer size to set (if possible), 0 means leave at the default,
	 *			the number of times to repeat transmission on the same (each) association,
	 *			the number of times to repeat establishment of the association and sending the file,
	 * 			the number of transfer syntaxes to propose for each additional presentation context at each association establishment,
	 * 			the number of additional presentation contexts to propose at each association establishment,
	 *			and the directory to be recursively searched or a list of file names to send.
	 */
	public static void main(String arg[]) {
		try {
			String             theirHost=null;
			int                theirPort=-1;
			String          theirAETitle=null;
			String            ourAETitle=null;
			int ourMaximumLengthReceived=0;
			int  socketReceiveBufferSize=0;
			int     socketSendBufferSize=0;
			int              repeatCount=0;
			int              assocnCount=0;
			int              syntaxCount=0;
			int             contextCount=0;
	
			if (arg.length >= 12) {
				               theirHost=arg[0];
				               theirPort=Integer.parseInt(arg[1]);
				            theirAETitle=arg[2];
				              ourAETitle=arg[3];
				ourMaximumLengthReceived=Integer.parseInt(arg[4]);
			   	 socketReceiveBufferSize=Integer.parseInt(arg[5]);
			   	    socketSendBufferSize=Integer.parseInt(arg[6]);
			                     repeatCount=Integer.parseInt(arg[7]);
			                     assocnCount=Integer.parseInt(arg[8]);
			                     syntaxCount=Integer.parseInt(arg[9]);
			                    contextCount=Integer.parseInt(arg[10]);
				
				int numberOfFiles = arg.length - 11;
				String[] fileNames = null;
				File firstFile = new File(arg[11]);
				if (numberOfFiles == 1 && firstFile.isDirectory()) {
					ArrayList<File> files = FileUtilities.listFilesRecursively(firstFile);
					numberOfFiles = files.size();
					fileNames = new String[numberOfFiles];
					int i=0;
					for (File file: files) {
						fileNames[i++] = file.getCanonicalPath();
					}
				}
				else {
					fileNames = new String[numberOfFiles];
					System.arraycopy(arg,11,fileNames,0,numberOfFiles);
				}
				new StorageSOPClassSCUPerformanceTest(theirHost,theirPort,theirAETitle,ourAETitle,
					ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
					repeatCount,assocnCount,syntaxCount,contextCount,fileNames);
			}
			else {
				throw new Exception("Argument list must be at least 12 values");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}





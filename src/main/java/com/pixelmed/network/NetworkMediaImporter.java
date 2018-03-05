/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.SetOfDicomFiles;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is designed to support the importation of DICOM files from
 * interchange media (such as CDs and DVDs) and their transfer over the
 * network as C-STORE requests to a specified AE.</p>
 * 
 * @see com.pixelmed.dicom.MediaImporter
 * @see com.pixelmed.database.DatabaseMediaImporter
 * 
 * @author	dclunie
 */
public class NetworkMediaImporter extends MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkMediaImporter.java,v 1.6 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NetworkMediaImporter.class);	// do not confuse with MessageLogger

	/***/
	protected SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles();

	protected class OurMultipleInstanceTransferStatusHandler extends MultipleInstanceTransferStatusHandler {
		protected MessageLogger logger;
		
		public OurMultipleInstanceTransferStatusHandler(MessageLogger logger) {
			this.logger = logger;
		}
		
		public void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID) {
//System.err.println("Sent "+sopInstanceUID);
			if (logger != null) {
				logger.sendLn("Transferred "+sopInstanceUID);
			}
		}
	}

	/**
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #NetworkMediaImporter(String,int,String,String,String,MessageLogger)} instead.
	 * @param	debugLevel	ignored
	 */
	public NetworkMediaImporter(String hostname,int port,String calledAETitle,String callingAETitle,
			String pathName,MessageLogger logger,int debugLevel) {
		this(hostname,port,calledAETitle,callingAETitle,pathName,logger);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	public NetworkMediaImporter(String hostname,int port,String calledAETitle,String callingAETitle,
			String pathName,MessageLogger logger) {
		super(logger);
		try {
			importDicomFiles(pathName);
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
//System.err.println(setOfDicomFiles);
		if (setOfDicomFiles.isEmpty()) {
				logger.sendLn("Finished ... nothing to transfer");
		}
		else {
			if (logger != null) {
				logger.sendLn("Starting network transfer ...");
			}
			new StorageSOPClassSCU(hostname,port,calledAETitle,callingAETitle,
				setOfDicomFiles,
				0/*compressionLevel*/,
				(logger == null ? null : new OurMultipleInstanceTransferStatusHandler(logger)),
				null/*moveOriginatorApplicationEntityTitle*/,0/*moveOriginatorMessageID*/);
			if (logger != null) {
				logger.sendLn("Finished import and transfer");
			}
		}
	}

	/**
	 * <p>Adds the specified file name and its characteristics to the list to be transferred.</p>
	 *
	 * <p>If any errors are encountered during this process, the exceptions
	 * are caught, logged to stderr, and the file will not be transferred.</p>
	 *
	 * <p>Note that the actual transfer is performed later once the characteristics
	 * of all the files to be transferred has been ascertained.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
//System.err.println("NetworkMediaImporter:doSomethingWithDicomFile(): "+mediaFileName);
//System.err.println("Importing "+mediaFileName);
		try {
			setOfDicomFiles.add(mediaFileName);
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}

	/**
	 * <p>Import DICOM files and send to the specified AE as C-STORE requests.</p>
	 *
	 * @param	arg	array of five strings - their hostname, their port, their AE Title, our AE Title,
	 *			and the path to the media or folder containing the files to import and send
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 5) {
				String           hostname=arg[0];
				int                  port=Integer.parseInt(arg[1]);
				String      calledAETitle=arg[2];
				String     callingAETitle=arg[3];
				String           pathName=arg[4];
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				//MessageLogger logger = null;
				new NetworkMediaImporter(hostname,port,calledAETitle,callingAETitle,pathName,logger);
			}
			else {
				throw new Exception("Argument list must be 5 values");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}




/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.server;

import com.pixelmed.database.DatabaseApplicationProperties;
import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.network.ApplicationEntityMap;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.NetworkConfigurationFromMulticastDNS;
import com.pixelmed.network.NetworkConfigurationFromLDAP;
import com.pixelmed.network.NetworkConfigurationSource;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

import com.pixelmed.web.RequestTypeServer;
import com.pixelmed.web.WebServerApplicationProperties;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.server.DicomAndWebStorageServer DicomAndWebStorageServer} 
 * implements a DICOM storage server with query and retrieval as well as an HTTP server that responds
 * to web requests including WADO as well as an external SQL database access server.</p>
 *
 * <p>The main method is also useful in its own right as a command-line DICOM Storage
 * SCP utility, which will store incoming files in a specified directory and database
 * and serve them up via DICOM and WADO, and optionally SQL.</p>
 *
 * <p>For example:</p>
 * <pre>
% java -server -Djava.awt.headless=true -Xms128m -Xmx512m -cp ./pixelmed.jar:./lib/additional/hsqldb.jar:./lib/additional/commons-compress-1.12.jar:./lib/additional/vecmath1.2-1.14.jar:./lib/additional/commons-codec-1.3.jar:./lib/additional/jmdns.jar:./lib/additional/aiviewer.jar com.pixelmed.server.DicomAndWebStorageServer test.properties
 * </pre>
 *
 * <p>Note that the aiviewer.jar file is only necessary if <a href="http://mars.elcom.nitech.ac.jp/dicom/index-e.html">Takahiro Katoji's AiViewer applet</a> is to be used
 * to view images, and is activated with the property WebServer.RequestTypeToUseForInstances=APPLETDISPLAY.</p>
 *
 * <p>External (unsecure) SQL access to the database is possible if the Application.DatabaseServerName property is specified; further
 * details are described in {@link com.pixelmed.database.DatabaseInformationModel com.pixelmed.database.DatabaseInformationModel}; for example:</p>
 * <pre>
% java -cp lib/additional/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --url "jdbc:hsqldb:hsql://localhost/testserverdb"
 * </pre>
 *
 * <p>For how to configure the necessary properties file, see:</p>
 *
 * @see com.pixelmed.web.WebServerApplicationProperties
 * @see com.pixelmed.network.NetworkApplicationProperties
 * @see com.pixelmed.database.DatabaseApplicationProperties
 *
 * @author	dclunie
 */
public class DicomAndWebStorageServer {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/server/DicomAndWebStorageServer.java,v 1.30 2017/01/24 10:50:47 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomAndWebStorageServer.class);

	protected DatabaseInformationModel databaseInformationModel;

	/***/
	private class OurReceivedObjectHandler extends ReceivedObjectHandler {
		/**
		 * @param	dicomFileName
		 * @param	transferSyntax
		 * @param	callingAETitle
		 * @throws	IOException
		 * @throws	DicomException
		 * @throws	DicomNetworkException
		 */
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
//System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				try {
//long startTime=System.currentTimeMillis();
					FileInputStream fis = new FileInputStream(dicomFileName);
					DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
					AttributeList list = new AttributeList();
					list.read(i,TagFromName.PixelData);
					i.close();
					fis.close();
//long fileReadTime=System.currentTimeMillis();
					databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
//long insertTime=System.currentTimeMillis();
//System.err.println("Received: "+dicomFileName+" ; re-read file time "+(fileReadTime-startTime)+" ms; database insert time "+(insertTime-fileReadTime)+" ms");
				} catch (Exception e) {
					slf4jlogger.error("Unable to insert {} received from {} in {} into database",dicomFileName,callingAETitle,transferSyntax,e);
				}
			}

		}
	}

	/**
	 * <p>Wait for connections and process requests, storing received files in a database, using the default {@link com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel PatientStudySeriesConcatenationInstanceModel}.</p>
	 *
	 * @param		properties
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public DicomAndWebStorageServer(Properties properties) throws java.io.IOException, com.pixelmed.dicom.DicomException, com.pixelmed.network.DicomNetworkException {	
		doCommonConstructorStuff(properties,null);
	}
	
	/**
	 * <p>Wait for connections and process requests, storing received files in a database, using the specified database information model.</p>
	 *
	 * @param		properties
	 * @param		databaseInformationModel
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	public DicomAndWebStorageServer(Properties properties,DatabaseInformationModel databaseInformationModel) throws java.io.IOException, com.pixelmed.dicom.DicomException, com.pixelmed.network.DicomNetworkException {	
		doCommonConstructorStuff(properties,databaseInformationModel);
	}
	
	/**
	 * <p>Wait for connections and process requests, storing received files in a database, using the specified database information model.</p>
	 *
	 * @param		properties
	 * @param		databaseInformationModel	if null, a {@link com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel PatientStudySeriesConcatenationInstanceModel} will be used
	 * @throws	IOException
	 * @throws	DicomException
	 * @throws	DicomNetworkException
	 */
	protected void doCommonConstructorStuff(Properties properties,DatabaseInformationModel databaseInformationModel) throws java.io.IOException, com.pixelmed.dicom.DicomException, com.pixelmed.network.DicomNetworkException {
		DatabaseApplicationProperties databaseApplicationProperties = new DatabaseApplicationProperties(properties);
		File savedImagesFolder = databaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary();
		if (databaseInformationModel == null) {
			this.databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(databaseApplicationProperties.getDatabaseFileName(),databaseApplicationProperties.getDatabaseServerName());
		}
		else {
			this.databaseInformationModel=databaseInformationModel;
		}
		NetworkApplicationProperties networkApplicationProperties = new NetworkApplicationProperties(properties);
		NetworkApplicationInformationFederated federatedNetworkApplicationInformation = new NetworkApplicationInformationFederated();
		WebServerApplicationProperties webServerApplicationProperties = new WebServerApplicationProperties(properties);
		
		federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties,webServerApplicationProperties);

		// Start up DICOM association listener in background for receiving images and responding to echoes and queries and retrieves ...
//System.err.println("Starting up DICOM association listener ...");
		{
			int port = networkApplicationProperties.getListeningPort();
			String calledAETitle = networkApplicationProperties.getCalledAETitle();
			new Thread(new StorageSOPClassSCPDispatcher(port,calledAETitle,
					networkApplicationProperties.getAcceptorMaximumLengthReceived(),networkApplicationProperties.getAcceptorSocketReceiveBufferSize(),networkApplicationProperties.getAcceptorSocketSendBufferSize(),
					savedImagesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,new OurReceivedObjectHandler(),
					this.databaseInformationModel.getQueryResponseGeneratorFactory(),
					this.databaseInformationModel.getRetrieveResponseGeneratorFactory(),
					federatedNetworkApplicationInformation,
					false/*secureTransport*/
					)).start();
		}
		// Start up web server ...
		{
			new Thread(new RequestTypeServer(this.databaseInformationModel,webServerApplicationProperties)).start();
		}
	}

	/**
	 * <p>Wait for connections and process requests, storing received files in a database.</p>
	 *
	 * @param	arg	a single file name that is the properties file;
	 *			if no argument is supplied the properties in "~/.com.pixelmed.server.DicomAndWebStorageServer.properties" will be used if present,
	 *			otherwise the defaults (11112,STORESCP,~/tmp,debug level 0) will be used
	 * @see com.pixelmed.database.DatabaseApplicationProperties
	 * @see com.pixelmed.network.NetworkApplicationProperties
	 * @see com.pixelmed.web.WebServerApplicationProperties
	 */
	public static void main(String arg[]) {
		String propertiesFileName = arg.length > 0 ? arg[0] : ".com.pixelmed.server.DicomAndWebStorageServer.properties";
		try {
			Properties properties = new Properties(/*defaultProperties*/);
			try {
				FileInputStream in = new FileInputStream(propertiesFileName);
				properties.load(in);
				in.close();
			}
			catch (IOException e) {
				//slf4jlogger.error("",e);
				properties.put(NetworkApplicationProperties.propertyName_DicomListeningPort,"11112");
				properties.put(NetworkApplicationProperties.propertyName_DicomCalledAETitle,"STORESCP");
				properties.put(NetworkApplicationProperties.propertyName_DicomCallingAETitle,"STORESCP");
				properties.put(NetworkApplicationProperties.propertyName_PrimaryDeviceType,"ARCHIVE");
				properties.put(DatabaseApplicationProperties.propertyName_SavedImagesFolderName,"tmp");
			}
			slf4jlogger.info("properties=\n{}",properties);	// use SLF4J since may be used as a background service
			new DicomAndWebStorageServer(properties);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be used as a background service
		}


	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link RequestTypeServer RequestTypeServer} implements an HTTP server that responds
 * to requests of a specified type and dispatches the further
 * handling to a derived class corresponding to the request type.</p>
 *
 * <p>Requests are of the form "?requestType=XXXX" where XXXX is the request type.</p>
 *
 * <p>This includes responding to WADO GET requests
 * as defined by DICOM PS 3.18 (ISO 17432), which provides a standard web (http) interface through which to retrieve DICOM objects either
 * as DICOM files or as derived JPEG images.</p>
 *
 * <p>In addition to servicing WADO requests, it provides lists of patients, studies and series that link to WADO URLs.</p>
 *
 * <p>It extends extends {@link com.pixelmed.web.HttpServer HttpServer} and implements
 * {@link HttpServer.Worker#generateResponseToGetRequest(String,OutputStream) generateResponseToGetRequest()}.</p>
 *
 * <p>The main method is also useful in its own right as a command-line DICOM Storage
 * SCP utility, which will store incoming files in a specified directory and database
 * and server them up via WADO.</p>
 *
 * <p>For example:</p>
 * <pre>
% java -server -Djava.awt.headless=true -Xms128m -Xmx512m -cp ./pixelmed.jar:./hsqldb.jar:./commons-compress-1.12.jar:./vecmath1.2-1.14.jar:./commons-codec-1.3.jar com.pixelmed.web.RequestTypeServer ./testwadodb ./testwadoimages 4007 WADOTEST 7091 "192.168.1.100" IMAGEDISPLAY
 * </pre>
 *
 * @see com.pixelmed.web.WadoServer
 *
 * @author	dclunie
 */
public class RequestTypeServer extends HttpServer {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/RequestTypeServer.java,v 1.25 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RequestTypeServer.class);

	private static final String imageDisplayTemplateFileName = "ImageDisplayTemplate.tpl";
	private static final String appletDisplayTemplateFileName = "AppletDisplayTemplate.tpl";

	private String rootURL;
	private String stylesheetPath;
	private String requestTypeToUseForInstances;
	private DatabaseInformationModel databaseInformationModel;
	
	protected class RequestTypeWorker extends Worker {
		private PathRequestHandler pathRequestHandler = null;
		private WadoRequestHandler wadoRequestHandler = null;
		private PatientListRequestHandler patientListRequestHandler = null;
		private StudyListRequestHandler studyListRequestHandler = null;
		private SeriesListRequestHandler seriesListRequestHandler = null;
		private InstanceListRequestHandler instanceListRequestHandler = null;
		private ImageDisplayRequestHandler imageDisplayRequestHandler = null;
		private AppletDisplayRequestHandler appletDisplayRequestHandler = null;
		
		protected void generateResponseToGetRequest(String requestURI,OutputStream out) throws IOException {
			slf4jlogger.debug("RequestTypeWorker.generateResponseToGetRequest(): Requested URI: {}",requestURI);
			try {
				WebRequest request = new WebRequest(requestURI);
				String requestType = request.getRequestType();
				if (requestType == null) {
					if (pathRequestHandler == null) {
						pathRequestHandler = new PathRequestHandler(stylesheetPath);
					}
					pathRequestHandler.generateResponseToGetRequest(null,null,null,request,null,out);
				}
				else if (requestType.equals("WADO")) {
					if (wadoRequestHandler == null) {
						wadoRequestHandler = new WadoRequestHandler(null);
					}
					wadoRequestHandler.generateResponseToGetRequest(databaseInformationModel,null,null,request,null,out);
				}
				else if (requestType.equals("PATIENTLIST")) {
					if (patientListRequestHandler == null) {
						patientListRequestHandler = new PatientListRequestHandler(stylesheetPath);
					}
					patientListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,null,null,out);
				}
				else if (requestType.equals("STUDYLIST")) {
					if (studyListRequestHandler == null) {
						studyListRequestHandler = new StudyListRequestHandler(stylesheetPath);
					}
					studyListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("SERIESLIST")) {
					if (seriesListRequestHandler == null) {
						seriesListRequestHandler = new SeriesListRequestHandler(stylesheetPath,requestTypeToUseForInstances);
					}
					seriesListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("INSTANCELIST")) {
					if (instanceListRequestHandler == null) {
						instanceListRequestHandler = new InstanceListRequestHandler(stylesheetPath);
					}
					instanceListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("IMAGEDISPLAY")) {
					if (imageDisplayRequestHandler == null) {
						imageDisplayRequestHandler = new ImageDisplayRequestHandler(stylesheetPath,imageDisplayTemplateFileName);
					}
					imageDisplayRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("APPLETDISPLAY")) {
					if (appletDisplayRequestHandler == null) {
						appletDisplayRequestHandler = new AppletDisplayRequestHandler(stylesheetPath,appletDisplayTemplateFileName);
					}
					appletDisplayRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else {
					throw new Exception("Unrecognized requestType \""+requestType+"\"");
				}
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
				slf4jlogger.debug("RequestTypeWorker.generateResponseToGetRequest(): Sending 404 Not Found");
				RequestHandler.send404NotFound(out,e.getMessage());
			}
		}
	}
	
	/***/
	protected Worker createWorker() {
		return new RequestTypeWorker();
	}
	
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
				slf4jlogger.debug("Received: {} from {} in {}",dicomFileName,callingAETitle,transferSyntax);
				try {
					FileInputStream fis = new FileInputStream(dicomFileName);
					DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
					AttributeList list = new AttributeList();
					list.read(i,TagFromName.PixelData);
					i.close();
					fis.close();
					databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
				} catch (Exception e) {
					slf4jlogger.error("Unable to insert {} received from {} in {} into database",dicomFileName,callingAETitle,transferSyntax,e);
				}
			}

		}
	}
	
	public RequestTypeServer(String dataBaseFileName,String savedImagesFolderName,int dicomPort,String calledAETitle,
			int wadoPort,String rootURL,String stylesheetPath,String requestTypeToUseForInstances,int numberOfWorkers) {
		super();
		try {
			databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(dataBaseFileName);
			File savedImagesFolder = new File(savedImagesFolderName);
			if (!savedImagesFolder.exists()) {
				savedImagesFolder.mkdirs();
			}
			new Thread(new StorageSOPClassSCPDispatcher(dicomPort,calledAETitle,savedImagesFolder,new OurReceivedObjectHandler(),null,null,null,false)).start();
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
		doCommonConstructorStuff(databaseInformationModel,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances,numberOfWorkers);
	}
	
	public RequestTypeServer(String dataBaseFileName,String savedImagesFolderName,int dicomPort,String calledAETitle,
			int wadoPort,String rootURL,String stylesheetPath,String requestTypeToUseForInstances) {
		this(dataBaseFileName,savedImagesFolderName,dicomPort,calledAETitle,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances,defaultNumberOfWorkers);
	}
	
	public RequestTypeServer(DatabaseInformationModel databaseInformationModel,WebServerApplicationProperties webServerApplicationProperties) {
		super();
		int numberOfWorkers = webServerApplicationProperties.getNumberOfWorkers();
		if (numberOfWorkers <= 0) {		// -1 is returned if the property is not specified
			numberOfWorkers = defaultNumberOfWorkers;
		}
		doCommonConstructorStuff(databaseInformationModel,
			webServerApplicationProperties.getListeningPort(),
			webServerApplicationProperties.getRootURL(),
			webServerApplicationProperties.getStylesheetPath(),
			webServerApplicationProperties.getRequestTypeToUseForInstances(),
			numberOfWorkers
		);
	}
	
	public RequestTypeServer(DatabaseInformationModel databaseInformationModel,int wadoPort,String rootURL,String stylesheetPath,String requestTypeToUseForInstances) {
		super();
		doCommonConstructorStuff(databaseInformationModel,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances,defaultNumberOfWorkers);
	}
	
	private void doCommonConstructorStuff(DatabaseInformationModel databaseInformationModel,int port,String rootURL,String stylesheetPath,String requestTypeToUseForInstances,int numberOfWorkers) {
		this.rootURL=rootURL;
		slf4jlogger.trace("doCommonConstructorStuff(): rootURL supplied = {}",rootURL);
		if (rootURL == null || rootURL.length() == 0) {
			try {
				InetAddress ineta = InetAddress.getLocalHost();
				if (ineta != null) {
					rootURL = ineta.getCanonicalHostName();
					slf4jlogger.trace("doCommonConstructorStuff(): rootURL from local InetAddress = {}",rootURL);
				}
			}
			catch (UnknownHostException e) {
				slf4jlogger.error("",e);
			}
		}
		this.stylesheetPath=stylesheetPath;
		slf4jlogger.trace("doCommonConstructorStuff(): stylesheetPath = {}",stylesheetPath);
		this.databaseInformationModel = databaseInformationModel;
		this.requestTypeToUseForInstances = requestTypeToUseForInstances;
		try {
			slf4jlogger.trace("doCommonConstructorStuff(): port = {}",port);
			super.initializeThreadPool(port,numberOfWorkers);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
		slf4jlogger.debug("ready");
	}

	/**
	 * <p>Wait for http connections and process requests; also wait for DICOM associations and store received files in the database.</p>
	 *
	 * @param	arg	array of seven strings - the database filename, the saved images folder, the DICOM port, the DICOM AET, the HTTP port, the host address to build the root URL,
	 *			the request type to use for instances (one of INSTANCELIST, IMAGEDISPLAY, or APPLETDISPLAY),
	 */
	public static void main(String arg[]) {
		if (arg.length != 7) {
			System.err.println("Usage: database imagefolder DICOMport DICOMAET HTTPport hostAddress requesttype");
			System.exit(0);
		}
		String stylesheetPath = "stylesheet.css";
		//String dataBaseFileName = "/tmp/testwadodb";
		String dataBaseFileName = arg[0];
		//String savedImagesFolderName = "/tmp/testwadoimages";
		String savedImagesFolderName = arg[1];
		//String dicomPort = "4007";
		int dicomPort = Integer.parseInt(arg[2]);
		//String calledAETitle = "WADOTEST";
		String calledAETitle = arg[3];
		//int wadoPort = 7091;
		int wadoPort = Integer.parseInt(arg[4]);
		String rootURL = "http://" + arg[5] + ":" + arg[4] + "/";
		String requestTypeToUseForInstances = arg[6];
		new Thread(new RequestTypeServer(dataBaseFileName,savedImagesFolderName,dicomPort,calledAETitle,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances)).start();
	}
}


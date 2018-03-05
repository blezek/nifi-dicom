/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.dose.CTDose;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.OCR;

import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;

//import com.pixelmed.utils.FileUtilities;

import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
//import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to a pre-configured DICOM destination.</p>
 *
 * <p>The class has no public methods other than the constructor and a main method that is useful as a utility.</p>
 *
 * @author	dclunie
 */
public class LegacyRadiationDoseOCRDicomForwardingService {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/doseocr/LegacyRadiationDoseOCRDicomForwardingService.java,v 1.9 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(LegacyRadiationDoseOCRDicomForwardingService.class);
	
	protected static long TIMEOUT_BEFORE_PROCESSING_SERIES_MS = 10*60*1000l;// 10 minutes
	protected static long TIMEOUT_BEFORE_CHECKING_FOR_WORK_MS = 10*1000l;	// 10 secs
	
	protected String theirHost;
	protected int theirPort;
	protected String theirAETitle;
	
	protected String ourAETitle;
	
	protected class Series {
		String seriesInstanceUID;
		long lastReceivedTime;
		List<String> fileNames = new LinkedList<String>();
		int numberWanted;
		
		Series(String seriesInstanceUID,int numberOfSeriesRelatedInstances) {
			this.seriesInstanceUID = seriesInstanceUID;
			numberWanted = numberOfSeriesRelatedInstances;
		}
		
		void addFile(String receivedFileName,long receivedTime) {
			fileNames.add(receivedFileName);
			lastReceivedTime = receivedTime;
		}
		
		boolean isReadyToProcess() {
			long timeSinceLast = System.currentTimeMillis() - lastReceivedTime;
			slf4jlogger.debug("Series.isReadyToProcess(): System.currentTimeMillis() - lastReceivedTime = {}",timeSinceLast);
			return (numberWanted > 0 && numberWanted == fileNames.size())
			    || (timeSinceLast > TIMEOUT_BEFORE_PROCESSING_SERIES_MS);
		}
	}

	protected class SeriesQueue {
		Map<String,Series> queuedMultiPageInstancesIndexedBySeriesInstanceUID = new HashMap<String,Series>();
		
		SeriesQueue() {
			new java.util.Timer().schedule(new java.util.TimerTask() { public void run() { synchronized (SeriesQueue.this) { SeriesQueue.this.notify(); } } },TIMEOUT_BEFORE_CHECKING_FOR_WORK_MS,TIMEOUT_BEFORE_CHECKING_FOR_WORK_MS);
		}
	
		synchronized void addFile(String seriesInstanceUID,String receivedFileName,long receivedTime,int numberOfSeriesRelatedInstances) {
			slf4jlogger.debug("SeriesQueue.addFile(): SeriesInstanceUID {}",seriesInstanceUID);
			Series series = queuedMultiPageInstancesIndexedBySeriesInstanceUID.get(seriesInstanceUID);
			if (series == null) {
				slf4jlogger.debug("SeriesQueue.addFile(): SeriesInstanceUID {} first instance",seriesInstanceUID);
				series = new Series(seriesInstanceUID,numberOfSeriesRelatedInstances);
				queuedMultiPageInstancesIndexedBySeriesInstanceUID.put(seriesInstanceUID,series);
			}
			series.addFile(receivedFileName,receivedTime);
			notify();
		}

		synchronized Series getWork() throws InterruptedException {
			while (true) {
				for (String seriesInstanceUID : queuedMultiPageInstancesIndexedBySeriesInstanceUID.keySet()) {	// may be empty
					Series series = queuedMultiPageInstancesIndexedBySeriesInstanceUID.get(seriesInstanceUID);
					slf4jlogger.debug("SeriesQueue.getWork(): checking series is ready {}",seriesInstanceUID);
					if (series != null && series.isReadyToProcess()) {
						queuedMultiPageInstancesIndexedBySeriesInstanceUID.remove(seriesInstanceUID);
						slf4jlogger.debug("SeriesQueue.getWork(): series is ready {}",seriesInstanceUID);
						return series;
					}
				}
				wait();	// will block here until something received or woken by timer task to check again
			}
		}
	}

	protected SeriesQueue seriesQueue = new SeriesQueue();
	
	protected class SeriesProcessor implements Runnable {
		
		SeriesProcessor() {
		}
		
		public void run() {
			try {
				while (true) {
					// Retrieve some work; block if the queue is empty
					Series series = seriesQueue.getWork();
					slf4jlogger.debug("SeriesProcessor.run(): SeriesInstanceUID {} is ready",series.seriesInstanceUID);
					OCR ocr = new OCR(series.fileNames);
//System.err.print(ocr);
					CTDose ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,null/*eventDataFromImages*/,true/*buildSR*/);
					if (ctDose != null) {
						sendSRFile(ctDose);
					}
					for (String f : series.fileNames) {
						try {
							if (!new File(f).delete()) {
								throw new DicomException("Failed to delete queued file that we have extracted from "+f);
							}
						}
						catch (Exception e) {
							slf4jlogger.error("While deleting file {}",f,e);
						}
					}
				}
			}
			catch (InterruptedException e) {
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	protected void sendSRFile(CTDose ctDose) {
		try {
			File ctDoseSRFile = File.createTempFile("ocrrdsr",".dcm");
			String ctDoseSRFileName = ctDoseSRFile.getCanonicalPath();
			try {
				AttributeList ctDoseList = ctDose.getAttributeList();
				slf4jlogger.debug("sendSRFile(): adding our own newly created SR file = {}",ctDoseSRFileName);
				ctDose.write(ctDoseSRFileName,ourAETitle,this.getClass().getCanonicalName());	// has side effect of updating list returned by ctDose.getAttributeList(); uncool :(
				new StorageSOPClassSCU(theirHost,theirPort,theirAETitle,ourAETitle,ctDoseSRFileName,
					Attribute.getSingleStringValueOrNull(ctDoseList,TagFromName.SOPClassUID),
					Attribute.getSingleStringValueOrNull(ctDoseList,TagFromName.SOPInstanceUID),
					0/*compressionLevel*/);
			}
			catch (Exception e) {
				slf4jlogger.error("While sending file {}",ctDoseSRFileName,e);
			}
			if (ctDoseSRFile != null) {
				try {
					if (!ctDoseSRFile.delete()) {
						throw new DicomException("Failed to delete RDSR file that we created "+ctDoseSRFileName);
					}
				}
				catch (Exception e) {
					slf4jlogger.error("", e);;
				}
			}
		}
		catch (IOException e) {
			slf4jlogger.error("",e);
		}
	}

	protected class ReceivedFileProcessor implements Runnable {
		String receivedFileName;
		AttributeList list;
		
		ReceivedFileProcessor(String receivedFileName) {
			this.receivedFileName = receivedFileName;
		}
		
		public void run() {
			try {
				slf4jlogger.trace("ReceivedFileProcessor.run(): receivedFileName = {}",receivedFileName);
				long receivedTime = System.currentTimeMillis();
				FileInputStream fis = new FileInputStream(receivedFileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				fis.close();
				
				{
					CTDose ctDose = null;
					if (OCR.isDoseScreenInstance(list)) {
						slf4jlogger.debug("ReceivedFileProcessor.run(): isDoseScreenInstance");
						int numberOfSeriesRelatedInstances = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfSeriesRelatedInstances,-1);
						if (numberOfSeriesRelatedInstances == 1) {
							OCR ocr = new OCR(list);
//System.err.print(ocr);
							ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,null/*eventDataFromImages*/,true/*buildSR*/);
						}
						else {
							slf4jlogger.debug("ReceivedFileProcessor.run(): numberOfSeriesRelatedInstances = {}",numberOfSeriesRelatedInstances);
							String seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
							if (seriesInstanceUID.length() > 0) {
								seriesQueue.addFile(seriesInstanceUID,receivedFileName,receivedTime,numberOfSeriesRelatedInstances);
								receivedFileName = null;	// to suppress deletion until queue processed
							}
						}
					}
					else if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
						slf4jlogger.debug("ReceivedFileProcessor.run(): isPhilipsDoseScreenInstance");
						ctDose = ExposureDoseSequence.getCTDoseFromExposureDoseSequence(list,null/*eventDataFromImages*/,true/*buildSR*/);
					}
					if (ctDose != null) {
						sendSRFile(ctDose);
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
			if (receivedFileName != null) {
				try {
					if (!new File(receivedFileName).delete()) {
						throw new DicomException("Failed to delete received file that we have successfully extracted from "+receivedFileName);
					}
				}
				catch  (Exception e) {
					slf4jlogger.error("",e);
				}
			}
		}
	}
	
	/**
	 *
	 */
	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
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
					new Thread(new ReceivedFileProcessor(dicomFileName)).start();		// on separate thread, else will block and the C-STORE response will be delayed
				} catch (Exception e) {
					slf4jlogger.error("Unable to process {} received from {} in {}",dicomFileName,callingAETitle,transferSyntax,e);
				}
			}

		}
	}

	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #LegacyRadiationDoseOCRDicomForwardingService(int,String,String,int,String,File)} instead.
	 * @param	ourPort				our port
	 * @param	ourAETitle			our AE Title
	 * @param	theirHost			their host name or IP address
	 * @param	theirPort			their port
	 * @param	theirAETitle		their AE title
	 * @param	savedImagesFolder	the folder in which to save the received images
	 * @param	debugLevel			ignored
	 */
	public LegacyRadiationDoseOCRDicomForwardingService(int ourPort,String ourAETitle,String theirHost,int theirPort,String theirAETitle,File savedImagesFolder,int debugLevel) throws IOException {
		this(ourPort,ourAETitle,theirHost,theirPort,theirAETitle,savedImagesFolder);
		slf4jlogger.warn("Debug level supplied as argument ignored");
	}
	
	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #LegacyRadiationDoseOCRDicomForwardingService(int,String,String,int,String,File)} instead.
	 * @param	ourPort				our port
	 * @param	ourAETitle			our AE Title
	 * @param	theirHost			their host name or IP address
	 * @param	theirPort			their port
	 * @param	theirAETitle		their AE title
	 * @param	savedImagesFolder	the folder in which to save the received images
	 * @param	debugLevel			ignored
	 * @param	networkDebugLevel	ignored
	 */
	public LegacyRadiationDoseOCRDicomForwardingService(int ourPort,String ourAETitle,String theirHost,int theirPort,String theirAETitle,File savedImagesFolder,int debugLevel,int networkDebugLevel) throws IOException {
		this(ourPort,ourAETitle,theirHost,theirPort,theirAETitle,savedImagesFolder);
		slf4jlogger.warn("Debug levels supplied as arguments ignored");
	}
	
	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @param	ourPort				our port
	 * @param	ourAETitle			our AE Title
	 * @param	theirHost			their host name or IP address
	 * @param	theirPort			their port
	 * @param	theirAETitle		their AE title
	 * @param	savedImagesFolder	the folder in which to save the received images
	 */
	public LegacyRadiationDoseOCRDicomForwardingService(int ourPort,String ourAETitle,String theirHost,int theirPort,String theirAETitle,File savedImagesFolder) throws IOException {
		this.ourAETitle        = ourAETitle;
		this.theirHost         = theirHost;
		this.theirPort         = theirPort;
		this.theirAETitle      = theirAETitle;
		// Start up DICOM association listener in background for receiving images  ...
		slf4jlogger.trace("Starting up DICOM association listener ...");
		new Thread(new StorageSOPClassSCPDispatcher(ourPort,ourAETitle,savedImagesFolder,new OurReceivedObjectHandler())).start();
		new Thread(new SeriesProcessor()).start();
	}

	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @param	arg	array of five strings - our port, our AE Title, their hostname, their port, their AE Title
	 */
	public static void main(String arg[]) {
		try {
			int ourPort;
			String ourAETitle;
			String theirHost;
			int theirPort;
			String theirAETitle;
			if (arg.length == 5) {
				        ourPort=Integer.parseInt(arg[0]);
				    ourAETitle=arg[1];
				     theirHost=arg[2];
				     theirPort=Integer.parseInt(arg[3]);
				  theirAETitle=arg[4];
			}
			else {
				throw new Exception("Argument list must be 5 values");
			}
			File savedImagesFolder = new File(System.getProperty("java.io.tmpdir"));
			new LegacyRadiationDoseOCRDicomForwardingService(ourPort,ourAETitle,theirHost,theirPort,theirAETitle,savedImagesFolder);
		}
		catch (Exception e) {
			slf4jlogger.error("", e);;
			System.exit(0);
		}
	}
}


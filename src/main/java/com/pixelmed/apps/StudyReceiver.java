/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseApplicationProperties;
import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.MinimalPatientStudySeriesInstanceModel;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.network.AnyExplicitStorePresentationContextSelectionPolicy;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.NetworkApplicationInformation;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

import com.pixelmed.utils.FileUtilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;

//import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to wait for incoming composite instance storage operations and process when study is complete based on time since last instance received.</p>
 *
 * <p>External (unsecure) SQL access to the database is possible if the Application.DatabaseServerName property is specified; further
 * details are described in {@link com.pixelmed.database.DatabaseInformationModel com.pixelmed.database.DatabaseInformationModel}; for example:</p>
 * <pre>
% java -cp lib/additional/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --url "jdbc:hsqldb:hsql://localhost/testserverdb"
 * </pre>
 *
 * <p>For how to configure the necessary properties file, see:</p>
 *
 * @see com.pixelmed.network.NetworkApplicationProperties
 * @see com.pixelmed.database.DatabaseApplicationProperties
 *
 * @author	dclunie
 */
public class StudyReceiver {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/StudyReceiver.java,v 1.14 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(StudyReceiver.class);
	
	protected static String defaultPropertiesFileName = ".com.pixelmed.apps.StudyReceiver.properties";
	
	protected static String propertyName_CompletedStudiesFolderName									  = "Application.CompletedStudiesFolderName";
	protected static String propertyName_SleepTimeBetweenPassesToProcessReceivedFiles                 = "Application.SleepTimeBetweenPassesToProcessReceivedFiles";
	protected static String propertyName_IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = "Application.IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy";
	
	protected String defaultCompletedStudiesFolderName									 = ".com.pixelmed.apps.StudyReceiver.completedstudies";
	protected String defaultSleepTimeBetweenPassesToProcessReceivedFiles                 = "60";	// 1 minute
	protected String defaultIntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = "60";	// 1 minute
	
	protected static int sleepTimeBetweenPassesToProcessReceivedFiles;					// seconds
	protected static int intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy;	// seconds
		
	protected Properties properties;
	
	protected NetworkApplicationProperties networkApplicationProperties;
	protected NetworkApplicationInformationFederated networkApplicationInformation;
	
	protected String ourCalledAETitle;
	
	protected DatabaseInformationModel databaseInformationModel;
	
	protected String buildDate = getBuildDate();
	
	protected File completedStudiesFolder;
	
	protected File savedImagesFolder;
	protected StoredFilePathStrategy storedFilePathStrategy = StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS;
	
	protected String studyHasBeenProcessedColumnName          = "PM_STUDYHASBEENPROCESSED";			// needs to be upper case ... indicates that all instances were successfully processed and study is not to be processed again
	protected String studyMostRecentInsertionTimeColumnName   = "PM_STUDYMOSTRECENTINSERTIONTIME";	// needs to be upper case ... timestamp of insertion of most recently received instance for the study


	protected String instanceHasBeenProcessedColumnName		  = "PM_INSTANCEHASBEENPROCESSED";	// needs to be upper case ... indicates that instance was successfully processed
	
	protected String studyInstanceUIDColumnName;
	protected String sopClassUIDColumnName;
	protected String instanceLocalFileNameColumnName;
	protected String instanceLocalFileReferenceTypeColumnName;
	protected String instanceLocalPrimaryKeyColumnName;
	protected String seriesLocalPrimaryKeyColumnName;


	/**
	 * <p>Get the date the package was built.</p>
	 *
	 * @return	 the build date
	 */
	// copied from ApplicationFrame - should refactor :(
	protected String getBuildDate() {
		String buildDate = "";
		try {
			buildDate = (new BufferedReader(new InputStreamReader(StudyReceiver.class.getResourceAsStream("/BUILDDATE")))).readLine();
		}
		catch (IOException e) {
			slf4jlogger.error("",e);
		}
		return buildDate;
	}

	/**
	 * <p>Load properties.</p>
	 *
	 * @throws	IOException	thrown if properties file is missing
	 */
	protected void loadProperties(String propertiesFileName) throws IOException {
		properties = new Properties(/*defaultProperties*/);
		FileInputStream in = new FileInputStream(propertiesFileName);
		properties.load(in);
		in.close();
	}
	
	// copied from SynchronizeFromRemoteSCP ... should refactor :(
	protected static class OurReadTerminationStrategy implements AttributeList.ReadTerminationStrategy {
		public boolean terminate(AttributeList attributeList,AttributeTag tag,long byteOffset) {
			return tag.getGroup() > 0x0020;
		}
	}
	
	protected final static AttributeList.ReadTerminationStrategy terminateAfterRelationshipGroup = new OurReadTerminationStrategy();

	/**
	 * <p>Rename a DICOM file into a folder hierarchy based on its attributes that are already available.</p>
	 *
	 * <p>Called by {@link #processStudy(String) processStudy()} for each file that has been received for a completed study.</p>
	 *
	 * <p>Override this method in a sub-class if you want to override the default folder structure used to store the received files,
	 * otherwise creates a folder structure using {@link com.pixelmed.dicom.MoveDicomFilesIntoHierarchy#renameFileWithHierarchicalPathFromAttributes(File,AttributeList,String,String) com.pixelmed.dicom.MoveDicomFilesIntoHierarchy.renameFileWithHierarchicalPathFromAttributes()}.</p>
	 *
	 * @param	file						the DICOM file
	 * @param	list						the attributes of the file (already read in)
	 * @param	hierarchicalFolderName		where to store the renamed file
	 * @param	duplicatesFolderNamePrefix	where to store the renamed file if it is a non-identical duplicate of the existing file
	 * @return								the path to the new file if successful, null if not
	 * @throws	IOException					if an error occurs renaming the files
	 * @throws	DicomException				if there is an error parsing the attribute list
	 * @throws	NoSuchAlgorithmException	if there is an error checking duplicate files contain identical content caused by absence of a hash algorithm
	 */
	protected String renameFileWithHierarchicalPathFromAttributes(File file,AttributeList list,String hierarchicalFolderName,String duplicatesFolderNamePrefix)
			throws IOException, DicomException, NoSuchAlgorithmException {
		return MoveDicomFilesIntoHierarchy.renameFileWithHierarchicalPathFromAttributes(file,list,hierarchicalFolderName,duplicatesFolderNamePrefix);
	}

	
	/**
	 * <p>Do something with the processed DICOM file.</p>
	 *
	 * <p>This method may be implemented in a sub-class to do something useful even if it is only logging to the user interface.
	 *
	 * <p>The default method does nothing.</p>
	 *
	 * <p>This method is called on the WatchDatabaseAndProcessCompleteStudies thread.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any errors locally.</p>
	 *
	 * @param	processedFileName				the path name to a DICOM file
	 */
	protected void doSomethingWithProcessedDicomFile(String processedFileName) {
//System.err.println("doSomethingWithProcessedDicomFile(): "+processedFileName);
	}

	protected boolean processStudy(String studyLocalPrimaryKeyValue) throws DicomException, IOException, Exception {
		boolean processed = false;
		List<Map<String,String>> seriesInstances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(InformationEntity.SERIES,studyLocalPrimaryKeyValue);
		for (Map<String,String> series : seriesInstances) {
			String seriesLocalPrimaryKeyValue = series.get(seriesLocalPrimaryKeyColumnName);
			List<Map<String,String>> instances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(InformationEntity.INSTANCE,seriesLocalPrimaryKeyValue);
			for (Map<String,String> instance : instances) {
				String fileName = instance.get(instanceLocalFileNameColumnName);
				slf4jlogger.info("processStudy(): processing fileName {}",fileName);
				FileInputStream fis = new FileInputStream(fileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
				AttributeList list = new AttributeList();
				list.read(i,terminateAfterRelationshipGroup);
				i.close();
				fis.close();
				String newFileName = renameFileWithHierarchicalPathFromAttributes(new File(fileName),list,completedStudiesFolder.getCanonicalPath(),"Duplicates");
				slf4jlogger.info("processStudy(): moved fileName {} to {}",fileName,newFileName);
				if (newFileName != null) {
					doSomethingWithProcessedDicomFile(newFileName);
					String instanceLocalPrimaryKeyValue = instance.get(instanceLocalPrimaryKeyColumnName);
					databaseInformationModel.updateSelectedRecord(InformationEntity.INSTANCE,instanceLocalPrimaryKeyValue,instanceLocalFileNameColumnName,newFileName);
				}
			}
			processed = true;
		}

		return processed;
	}
	
	protected boolean processStudyIfComplete(String studyLocalPrimaryKeyValue) throws DicomException, IOException, Exception {
		boolean processed = false;
		long mostRecentInsertionTime = Long.parseLong(databaseInformationModel.findSelectedAttributeValuesForSelectedRecord(InformationEntity.STUDY,studyLocalPrimaryKeyValue,studyMostRecentInsertionTimeColumnName));
		
		long currentTimeMillis = System.currentTimeMillis();
		slf4jlogger.trace("processStudyIfComplete(): currentTimeMillis = {}",currentTimeMillis);
		slf4jlogger.trace("processStudyIfComplete(): mostRecentInsertionTime = {}",mostRecentInsertionTime);
		long secondsSinceMostRecentInsertion = (currentTimeMillis - mostRecentInsertionTime) / 1000;
		slf4jlogger.trace("processStudyIfComplete(): secondsSinceMostRecentInsertion = {}",secondsSinceMostRecentInsertion);
		if (secondsSinceMostRecentInsertion > intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy) {
			slf4jlogger.debug("processStudyIfComplete(): processing, since old enough");
			processed = processStudy(studyLocalPrimaryKeyValue);
		}
		else {
			slf4jlogger.debug("processStudyIfComplete(): not processing, since too recent");
		}
		
		return processed;
	}

	protected class WatchDatabaseAndProcessCompleteStudies implements Runnable {
		public void run() {
			boolean interrupted = false;
			while (!interrupted) {
				slf4jlogger.trace("WatchDatabaseAndProcessCompleteStudies.run(): Starting or waking up WatchDatabaseAndProcessCompleteStudies ...");
				try {
					List<Map<String,String>> returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntity(InformationEntity.STUDY);
					{
						for (Map<String,String> record : returnedRecords) {
							if (slf4jlogger.isTraceEnabled()) {
								slf4jlogger.trace("STUDY:");
								for (String key : record.keySet()) {
									slf4jlogger.trace("\t{} = {}",key,record.get(key));
								}
							}
							String studyLocalPrimaryKeyValue = record.get(databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY));
							String studyHasBeenProcessedValue = record.get(studyHasBeenProcessedColumnName);
							boolean studyHasBeenProcessed = studyHasBeenProcessedValue != null && studyHasBeenProcessedValue.toUpperCase(java.util.Locale.US).equals("TRUE");
							if (studyHasBeenProcessed) {
								if (slf4jlogger.isTraceEnabled())slf4jlogger.trace("WatchDatabaseAndProcessCompleteStudies.run(): Already processed {}",record.get(studyInstanceUIDColumnName));
							}
							else {
								if (slf4jlogger.isDebugEnabled())slf4jlogger.debug("WatchDatabaseAndProcessCompleteStudies.run(): Considering {}",record.get(studyInstanceUIDColumnName));
								try {
									if(processStudyIfComplete(studyLocalPrimaryKeyValue)) {
										// returned true (success) only if ALL selected files in complete study were successfully processed
										databaseInformationModel.updateSelectedRecord(InformationEntity.STUDY,studyLocalPrimaryKeyValue,studyHasBeenProcessedColumnName,"TRUE");										
									}
								}
								catch (Exception e) {
									slf4jlogger.error("",e);
									// do not set study processed to true, since failure may be transient and can try again next time
								}
							}
						}
					}
					

				slf4jlogger.trace("WatchDatabaseAndProcessCompleteStudies.run(): sleeping for "+sleepTimeBetweenPassesToProcessReceivedFiles+" seconds");
					Thread.currentThread().sleep(sleepTimeBetweenPassesToProcessReceivedFiles*1000);	// configured value is in seconds, sleep() parameter is in milliseconds
				}
				catch (DicomException e) {
					slf4jlogger.error("",e);
				}
				catch (InterruptedException e) {
				slf4jlogger.trace("WatchDatabaseAndProcessCompleteStudies.run(): interrupted: {}",e);
					interrupted = true;		// currently this shouldn't happen; i.e., no other thread will interrupt this one whilst sleeping (?)
				}
			}
		}
	}
	
	protected void updateStudyMostRecentInsertionTime(String studyInstanceUID,long insertionTime) throws DicomException {
		slf4jlogger.trace("updateStudyMostRecentInsertionTime(): studyInstanceUID = {}, time = {}",studyInstanceUID,insertionTime);
		ArrayList<Map<String,String>> studies = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(InformationEntity.STUDY,studyInstanceUIDColumnName,studyInstanceUID);
		if (studies.size() == 1) {
			Map<String,String> study = studies.get(0);
			String studyLocalPrimaryKeyValue = study.get(databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY));
			databaseInformationModel.updateSelectedRecord(InformationEntity.STUDY,studyLocalPrimaryKeyValue,studyMostRecentInsertionTimeColumnName,Long.toString(insertionTime));
			databaseInformationModel.updateSelectedRecord(InformationEntity.STUDY,studyLocalPrimaryKeyValue,studyHasBeenProcessedColumnName,"FALSE");	// reprocess study again next time, since new or duplicates received (000916)
		}
		else {
			throw new DicomException("Internal error: missing or multiple study table records for StudyInstanceUID"+studyInstanceUID);
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
				FileInputStream fis = new FileInputStream(receivedFileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
				AttributeList list = new AttributeList();
				list.read(i,terminateAfterRelationshipGroup);
				i.close();
				fis.close();

				{
					String sourceApplicationEntityTitle = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SourceApplicationEntityTitle);
					String            transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
					String                  sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.MediaStorageSOPClassUID);

					doSomethingWithReceivedDicomFile(receivedFileName,sourceApplicationEntityTitle,transferSyntaxUID,sopClassUID); // call this here rather than in OurReceivedObjectHandler since do not want to block/delay StorageSCP thread
				}
				
				String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
				if (studyInstanceUID.length() > 0) {
					databaseInformationModel.insertObject(list,receivedFileName,DatabaseInformationModel.FILE_COPIED);
					updateStudyMostRecentInsertionTime(studyInstanceUID,System.currentTimeMillis());
					
				}
				else {
					throw new DicomException("No StudyInstanceUID in received file "+receivedFileName);
					// should probably delete it :(
				}

			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	/**
	 * <p>Do something with the received DICOM file.</p>
	 *
	 * <p>This method may be implemented in a sub-class to do something useful even if it is only logging to the user interface.
	 *
	 * <p>The default method does nothing.</p>
	 *
	 * <p>This method is called on the ReceivedFileProcessor thread.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any errors locally.</p>
	 *
	 * @param	receivedFileName				the path name to a DICOM file
	 * @param	sourceApplicationEntityTitle	the Application Entity from which the file was received
	 * @param	transferSyntaxUID				the Transfer Syntax of the Data Set in the DICOM file
	 * @param	sopClassUID						the SOP Class of the Data Set in the DICOM file
	 */
	protected void doSomethingWithReceivedDicomFile(String receivedFileName,String sourceApplicationEntityTitle,String transferSyntaxUID,String sopClassUID) {
//System.err.println("doSomethingWithReceivedDicomFile(): "+receivedFileName+" received from "+sourceApplicationEntityTitle+" in "+transferSyntaxUID+" is "+sopClassUID);
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
	
	protected class OurPatientStudySeriesInstanceModel extends MinimalPatientStudySeriesInstanceModel {
		OurPatientStudySeriesInstanceModel(String databaseFileName,String databaseServerName) throws DicomException {
			super(databaseFileName,databaseServerName);
		}

		protected void extendCreateStatementStringWithUserColumns(StringBuffer b,InformationEntity ie) {
			if (ie == InformationEntity.STUDY) {
				b.append(", "); b.append(studyHasBeenProcessedColumnName); b.append(" "); b.append("BOOLEAN");
				b.append(", "); b.append(studyMostRecentInsertionTimeColumnName); b.append(" "); b.append("BIGINT");	// see "http://www.hsqldb.org/doc/guide/ch09.html#datatypes-section"
			}
			else if (ie == InformationEntity.INSTANCE) {
				b.append(", "); b.append(instanceHasBeenProcessedColumnName); b.append(" "); b.append("BOOLEAN");
			}
		}

	}

	// copied from DatabaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary()
	/**
	 * <p>Return the folder, creating it if necessary.</p>
	 *
	 * <p>If not an absolute path, will be sought or created relative to the current user's home directory.</p>
	 *
	 * @return	the folder
	 */
	protected File getCompletedStudiesFolderNameCreatingItIfNecessary(String completedStudiesFolderName) throws IOException {
//System.err.println("DatabaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary(): requesting completedStudiesFolderName = "+completedStudiesFolderName);
		File completedStudiesFolder = new File(completedStudiesFolderName);
		if (completedStudiesFolder.isAbsolute()) {
			if (!completedStudiesFolder.isDirectory() && !completedStudiesFolder.mkdirs()) {
				throw new IOException("Cannot find or create absolute path "+completedStudiesFolder);
			}
		}
		else {
			completedStudiesFolder = new File(FileUtilities.makePathToFileInUsersHomeDirectory(completedStudiesFolderName));
			if (!completedStudiesFolder.isDirectory() && !completedStudiesFolder.mkdirs()) {
				throw new IOException("Cannot find or create home directory relative path "+completedStudiesFolder);
			}
		}
//System.err.println("Study.Receiver.getCompletedStudiesFolderNameCreatingItIfNecessary(): using completedStudiesFolder = "+completedStudiesFolder);
		return completedStudiesFolder;
	}

	private StorageSOPClassSCPDispatcher storageSOPClassSCPDispatcher;
	
	/**
	 * <p>Start or restart DICOM storage listener.</p>
	 *
	 * <p>Shuts down existing listener, if any, so may be used to restart after configuration change.</p>
	 *
	 * @throws	DicomException
	 */
	public void activateStorageSCP() throws DicomException, IOException {
		shutdownStorageSCP();
		// Start up DICOM association listener in background for receiving images and responding to echoes ...
		if (networkApplicationProperties != null) {
			slf4jlogger.trace("Starting up DICOM association listener ...");
			int port = networkApplicationProperties.getListeningPort();
			storageSOPClassSCPDispatcher = new StorageSOPClassSCPDispatcher(port,ourCalledAETitle,
				networkApplicationProperties.getAcceptorMaximumLengthReceived(),networkApplicationProperties.getAcceptorSocketReceiveBufferSize(),networkApplicationProperties.getAcceptorSocketSendBufferSize(),
				savedImagesFolder,storedFilePathStrategy,new OurReceivedObjectHandler(),
				null/*AssociationStatusHandler*/,
				null/*queryResponseGeneratorFactory*/,
				null/*retrieveResponseGeneratorFactory*/,
				networkApplicationInformation,
				new AnyExplicitStorePresentationContextSelectionPolicy(),
				false/*secureTransport*/);
			new Thread(storageSOPClassSCPDispatcher).start();
		}
	}
	
	/**
	 * <p>Shutdown DICOM storage listener.</p>
	 */
	public void shutdownStorageSCP()  {
		if (storageSOPClassSCPDispatcher != null) {
			slf4jlogger.trace("Shutdown DICOM association listener ...");
			storageSOPClassSCPDispatcher.shutdown();
			storageSOPClassSCPDispatcher = null;
		}
	}
	
	/**
	 * <p>Wait for incoming composite instance storage operations and process when study is complete based on time since last instance received.</p>
	 *
	 * @param	propertiesFileName
	 */
	public StudyReceiver(String propertiesFileName) throws DicomException, DicomNetworkException, IOException, InterruptedException {
		loadProperties(propertiesFileName);		// do NOT trap exception; we must have properties

		completedStudiesFolder = getCompletedStudiesFolderNameCreatingItIfNecessary(properties.getProperty(propertyName_CompletedStudiesFolderName,defaultCompletedStudiesFolderName));

		sleepTimeBetweenPassesToProcessReceivedFiles                 = Integer.valueOf(properties.getProperty(propertyName_SleepTimeBetweenPassesToProcessReceivedFiles,defaultSleepTimeBetweenPassesToProcessReceivedFiles)).intValue();
		intervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy = Integer.valueOf(properties.getProperty(propertyName_IntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy,defaultIntervalAfterLastInstanceReceivedToWaitBeforeProcessingStudy)).intValue();

		DatabaseApplicationProperties databaseApplicationProperties = new DatabaseApplicationProperties(properties);
		savedImagesFolder = databaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary();
		databaseInformationModel = new OurPatientStudySeriesInstanceModel(databaseApplicationProperties.getDatabaseFileName(),databaseApplicationProperties.getDatabaseServerName());
		
		studyInstanceUIDColumnName               = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.StudyInstanceUID);
		sopClassUIDColumnName                    = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPClassUID);
		instanceLocalFileNameColumnName          = databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE);
		instanceLocalFileReferenceTypeColumnName = databaseInformationModel.getLocalFileReferenceTypeColumnName(InformationEntity.INSTANCE);
		instanceLocalPrimaryKeyColumnName        = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.INSTANCE);
		seriesLocalPrimaryKeyColumnName          = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.SERIES);
		
		networkApplicationProperties = new NetworkApplicationProperties(properties,true/*addPublicStorageSCPsIfNoRemoteAEsConfigured*/);
		networkApplicationInformation = new NetworkApplicationInformationFederated();
		networkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
		ourCalledAETitle = networkApplicationProperties.getCalledAETitle();

		activateStorageSCP();
		
		new Thread(new WatchDatabaseAndProcessCompleteStudies()).start();
	}

	/**
	 * <p>Wait for incoming composite instance storage operations and process when study is complete based on time since last instance received.</p>
	 *
	 * @param	arg		none
	 */
	public static void main(String arg[]) {
		try {
			String propertiesFileName = arg.length > 0 ? arg[0] : FileUtilities.makePathToFileInUsersHomeDirectory(defaultPropertiesFileName);
			new StudyReceiver(propertiesFileName);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be used as a background service
			System.exit(0);
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.MinimalPatientStudySeriesInstanceModel;

import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SpecificCharacterSet;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.UniqueIdentifierAttribute;

import com.pixelmed.network.AnyExplicitStorePresentationContextSelectionPolicy;
import com.pixelmed.network.Association;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.GetSOPClassSCU;
import com.pixelmed.network.IdentifierHandler;
import com.pixelmed.network.MoveSOPClassSCU;
import com.pixelmed.network.NetworkConfigurationFromMulticastDNS;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.UnencapsulatedExplicitStorePresentationContextSelectionPolicy;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.charset.StandardCharsets;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for synchronizing the contents of a local database of DICOM objects with a remote SCP.</p>
 *
 * <p>The class has no public methods other than the constructor and a main method that is useful as a utility. The
 * constructor establishes an association, sends hierarchical C-FIND requests at the STUDY, SERIES and IMAGE
 * levels to determine what is available on the remote AE, then attempts to retrieve anything not present
 * locally at the highest level possible. E.g., if a study is not present, a retrieve of the entire study
 * is requested.
 *
 * Verbosity of logging can be controlled using the SLF4J log level. The default INFO level summarizes major
 * actions, such as which studies, series and instances are fetched and which files are received; it can
 * be suppressed by setting the logging level to a higher level, such as WARN, or enhanced by using DEBUG or TRACE.
 *
 * <p>The main method is also useful in its own right as a command-line utility. For example:</p>
 * <pre>
java -cp ./pixelmed.jar:./lib/additional/hsqldb.jar -Djava.awt.headless=true \
	-Dorg.slf4j.simpleLogger.log.com.pixelmed.apps.SynchronizeFromRemoteSCP=warn \
	com.pixelmed.apps.SynchronizeFromRemoteSCP \
	/tmp/dicomsync/database /tmp/dicomsync \
	them 104 THEM \
	11112 US
 * </pre>
 *
 * @author	dclunie
 */
public class SynchronizeFromRemoteSCP {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/SynchronizeFromRemoteSCP.java,v 1.23 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SynchronizeFromRemoteSCP.class);
	
	private static int sleepTimeBetweenCheckingForNothingExpectedBeforeExiting = 10000;	// ms
	private static int sleepTimeAfterRegisteringWithBonjour = 10000;	// ms
	private static int inactivityTimeOut = 600000;	// ms
	
	private DatabaseInformationModel databaseInformationModel;
	private File savedInstancesFolder;
	private String remoteHost;
	private int remotePort;
	private String remoteAE;
	private int localPort;
	private String localAE;
	
	private String[] patientNameQueryPatterns;
	
	private QueryInformationModel queryInformationModel;
	
	private ReceivedObjectHandler receivedObjectHandler;
	private IdentifierHandler identifierHandler;
	private boolean useGet;
	private boolean queryAll;
	private boolean retrieveStudy;
	
	private Set setofInstancesExpected;
	private Set setofClassesExpected;
	private int numberOfSOPInstancesReceived;
	private int numberOfValidSOPInstancesReceived;
	private int numberOfUnrequestedSOPInstancesReceived;
	private long inactivityTime;
	
	private long totalDurationOfRetrieval;
	private long totalBytesSaved;
	
	private DecimalFormat commaFormatter = new DecimalFormat("#,###");

	/**
	 * @param	node
	 * @param	parentUniqueKeys
	 * @param	retrieveDone
	 */
	private void walkTreeDownToInstanceLevelAndRetrieve(QueryTreeRecord node,AttributeList parentUniqueKeys,boolean retrieveDone,boolean anyTransferSyntax) throws DicomException, DicomNetworkException, IOException {
		InformationEntity ie = node.getInformationEntity();
		Attribute uniqueKey = node.getUniqueKey();
		AttributeList uniqueKeys = null;
		AttributeList retrieveIdentifier = null;
		ArrayList recordsForThisUID = null;

		slf4jlogger.trace("Processing node {}",node);
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("walkTreeDownToInstanceLevelAndRetrieve(): getAllAttributesReturnedInIdentifier() =\n{}",node.getAllAttributesReturnedInIdentifier());

		if (ie != null && uniqueKey != null) {
			uniqueKeys = new AttributeList();
			if (parentUniqueKeys != null) {
				uniqueKeys.putAll(parentUniqueKeys);
			}
			AttributeTag uniqueKeyTagFromThisLevel = queryInformationModel.getUniqueKeyForInformationEntity(ie);
			String uid = uniqueKey.getSingleStringValueOrNull();	// always a UID, since StudyRoot
			if (uid == null) {
				slf4jlogger.info("Could not get UID to use for Unique Key");
			}
			else {
				slf4jlogger.debug("Searching for existing records for {} {}",ie,uid);
				recordsForThisUID = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(ie,uid);
				slf4jlogger.trace("Found {} existing records",recordsForThisUID.size());
				slf4jlogger.trace("retrieveDone {}",retrieveDone);
				{ Attribute a = new UniqueIdentifierAttribute(uniqueKeyTagFromThisLevel); a.addValue(uid); uniqueKeys.put(a); }
				slf4jlogger.trace("uniqueKeys:\n{}",uniqueKeys);
				if (!retrieveDone && recordsForThisUID.size() == 0) {
					slf4jlogger.debug("No existing records for {} {}",ie,uid);
					if (slf4jlogger.isInfoEnabled()) {
						slf4jlogger.info("Performing retrieve for {} ({})",node,Attribute.getSingleStringValueOrEmptyString(node.getUniqueKey()));
					}
					SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
					retrieveIdentifier = new AttributeList();
					retrieveIdentifier.putAll(uniqueKeys);
					
					String retrieveLevelName = queryInformationModel.getQueryLevelName(ie);
					{ Attribute a = new CodeStringAttribute(TagFromName.QueryRetrieveLevel); a.addValue(retrieveLevelName); retrieveIdentifier.put(a); }
										slf4jlogger.debug("Retrieve identifier:\n{}",retrieveIdentifier);
					// defer the actual move until the children have been walked and the SOPInstanceUIDs expected added to setofInstancesExpected
					retrieveDone = true;	// but make sure children don't perform any moves
				}
				else {
					slf4jlogger.trace("Not adding anything to retrieveIdentifier");
				}
			}
		}
		{
			slf4jlogger.trace("Process children if any, of node {},",node);
			int n = node.getChildCount();	// note that this has side effect of performing a C-FIND if necessary
			slf4jlogger.trace("Child node count {}",n);
			if (n > 0) {
				for (int i=0; i<n; ++i) {
					QueryTreeRecord child = (QueryTreeRecord)node.getChildAt(i);
					if (child != null) {	// should not happen, but has been observed ... do not allow it to derail entire sync :(
						walkTreeDownToInstanceLevelAndRetrieve(child,uniqueKeys,retrieveDone,anyTransferSyntax);
					}
				}
			}
			else {
				if (ie != null && ie.equals(InformationEntity.INSTANCE) && recordsForThisUID.size() == 0) {
					// don't already have the instance so we expect it
					AttributeList list = node.getAllAttributesReturnedInIdentifier();
					slf4jlogger.trace("Adding to set to retrieve: getAllAttributesReturnedInIdentifier() =\n{}",list);
					String sopInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
					slf4jlogger.trace("Adding to set to retrieve: SOPInstanceUID = {}",sopInstanceUID);
					setofInstancesExpected.add(sopInstanceUID);
					if (useGet) {
						String sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
						if (sopClassUID == null || sopClassUID.length() == 0) {
							slf4jlogger.info("Adding to set to retrieve: SOPClassUID is missing or empty in C-FIND response ... trying to guess an appropriate one to use for C-GET");
							// bummer ... SCP didn't return SOPClassUID, which is allowed, since optional for C-FIND SCP
							// try to guess it from parent STUDY node's SOPClassesInStudy, or if absent, then parent SERIES Modality ...
							QueryTreeRecord seriesNode = (QueryTreeRecord)(node.getParent());
							QueryTreeRecord studyNode = (QueryTreeRecord)(seriesNode.getParent());
							AttributeList studyList = studyNode.getAllAttributesReturnedInIdentifier();
							Attribute aSOPClassesInStudy = studyList.get(TagFromName.SOPClassesInStudy);
							if (aSOPClassesInStudy != null && aSOPClassesInStudy.getVM() > 0) {
								String[] sopClassesInStudy = aSOPClassesInStudy.getStringValues();
								for (String sopClassInStudy : sopClassesInStudy) {
									slf4jlogger.debug("Adding to set to retrieve: SOPClassUID derived from SOPClassesInStudy = {}",sopClassInStudy);
									setofClassesExpected.add(sopClassInStudy);
								}
							}
							else {
								AttributeList seriesList = seriesNode.getAllAttributesReturnedInIdentifier();
								String modality = Attribute.getSingleStringValueOrEmptyString(seriesList,TagFromName.Modality);
								slf4jlogger.debug("Adding to set to retrieve: No SOPClassesInStudy either, trying Modality {}",modality);
								String[] sopClassUIDsForModality = SOPClass.getPlausibleStandardSOPClassUIDsForModality(modality);
								if (sopClassUIDsForModality.length > 0) {
									for (String sopClassUIDForModality: sopClassUIDsForModality) {
										slf4jlogger.debug("Adding to set to retrieve: SOPClassUID derived from Modality {} = {}",modality,sopClassUIDForModality);
										setofClassesExpected.add(sopClassUIDForModality);
									}
								}
								else {
									slf4jlogger.debug("Adding to set to retrieve: all known storage SOP Classes");
									setofClassesExpected = SOPClass.getSetOfStorageSOPClasses();
								}
							}
						}
						else {
					slf4jlogger.debug("Adding to set to retrieve: SOPClassUID = {}",sopClassUID);
							setofClassesExpected.add(sopClassUID);
						}
					}
				}
			}
		}
		if (retrieveIdentifier == null) {
			slf4jlogger.debug("Not doing anything because retrieveIdentifier is null");
		}
		else {
			try {
				bytesSaved=0;
				long startOfRetrieval=System.currentTimeMillis();
				if (useGet) {
					slf4jlogger.info("Retrieving with C-GET");
					if (anyTransferSyntax) {
						GetSOPClassSCU getSOPClassSCU = new GetSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,
							SOPClass.StudyRootQueryRetrieveInformationModelGet,
							retrieveIdentifier,identifierHandler,savedInstancesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,receivedObjectHandler,
							setofClassesExpected,4/*compressionLevel*/,true/*theirChoice*/,true/*ourChoice*/,false/*asEncoded*/);
					}
					else {
						GetSOPClassSCU getSOPClassSCU = new GetSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,
							SOPClass.StudyRootQueryRetrieveInformationModelGet,
							retrieveIdentifier,identifierHandler,savedInstancesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,receivedObjectHandler,
							setofClassesExpected,false/*theirChoice*/,true/*ourChoice*/,true/*asEncoded*/);
					}
					setofClassesExpected.clear();	// flush list and start afresh for next C-GET
				}
				else {
					slf4jlogger.info("Retrieving with C-MOVE");
					bytesSaved=0;
					//queryInformationModel.performHierarchicalMoveFrom(retrieveIdentifier,remoteAE);
					MoveSOPClassSCU moveSOPClassSCU = null;
					Association association = queryInformationModel.getCMoveAssociation();
					if (association != null) {
						moveSOPClassSCU = new MoveSOPClassSCU(association,localAE,SOPClass.StudyRootQueryRetrieveInformationModelMove,retrieveIdentifier);
					}
					else {
						moveSOPClassSCU = new MoveSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,localAE,SOPClass.StudyRootQueryRetrieveInformationModelMove,retrieveIdentifier);
					}
					int moveStatus = moveSOPClassSCU.getStatus();
					if (moveStatus != 0x0000) {
						slf4jlogger.info("SynchronizeFromRemoteSCP: unsuccessful move status = 0x{}",Integer.toHexString(moveStatus));
					}
				}
				if (slf4jlogger.isInfoEnabled()) {
					long durationOfRetrieval = System.currentTimeMillis() - startOfRetrieval;
					double rate = durationOfRetrieval == 0 ? 0l : ((double)bytesSaved)/1000000/(((double)durationOfRetrieval)/1000);
					slf4jlogger.info("Saved {} bytes in {} ms, {} MB/s",commaFormatter.format(bytesSaved),commaFormatter.format(durationOfRetrieval),rate);
					totalDurationOfRetrieval += durationOfRetrieval;
					totalBytesSaved += bytesSaved;
				}
			}
			catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}

	/**
	 */
	private void performQueryAndWalkTreeDownToInstanceLevelAndRetrieve(boolean anyTransferSyntax) throws DicomException, DicomNetworkException, IOException {
		SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
		AttributeList identifier = new AttributeList();
		identifier.putNewAttribute(TagFromName.StudyInstanceUID);
		identifier.putNewAttribute(TagFromName.SeriesInstanceUID);
		identifier.putNewAttribute(TagFromName.SOPInstanceUID);
		if (useGet) {
			identifier.putNewAttribute(TagFromName.SOPClassesInStudy);
			identifier.putNewAttribute(TagFromName.SOPClassUID);
		}
	
		// do NOT condition on slf4jlogger.isInfoEnabled() ... change in logging level should not change behavior ... should be harmless to always ask for these as return keys
		{
			//identifier.putNewAttribute(TagFromName.PatientName,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.PatientID,specificCharacterSet);

			identifier.putNewAttribute(TagFromName.StudyDate);
			identifier.putNewAttribute(TagFromName.StudyID,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.StudyDescription,specificCharacterSet);
		
			identifier.putNewAttribute(TagFromName.SeriesNumber);
			identifier.putNewAttribute(TagFromName.SeriesDescription,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.Modality);

			identifier.putNewAttribute(TagFromName.InstanceNumber);
		}
		
		String patientNamesAll[] = { "" };
		String patientNamesSelective[] = { "A*", "B*", "C*", "D*", "E*", "F*", "G*", "H*", "I*", "J*", "K*", "L*", "M*", "N*", "O*", "P*", "Q*", "R*", "S*", "T*", "U*", "V*", "W*", "X*", "Y*", "Z*" };

		String patientNames[] = patientNameQueryPatterns == null ? (queryAll ? patientNamesAll : patientNamesSelective) : patientNameQueryPatterns;
		for (int i=0; i<patientNames.length; ++i) {
			slf4jlogger.debug("Query {}",patientNames[i]);
			{ Attribute a = new PersonNameAttribute(TagFromName.PatientName,specificCharacterSet); a.addValue(patientNames[i]); identifier.put(a); }
			QueryTreeModel tree = queryInformationModel.performHierarchicalQuery(identifier);
			walkTreeDownToInstanceLevelAndRetrieve((QueryTreeRecord)(tree.getRoot()),null,false,anyTransferSyntax);
		}
	}

	/**
	 */
	private void performQueryAndStudyRetrieve(boolean anyTransferSyntax) throws DicomException, DicomNetworkException, IOException {
		SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
		AttributeList identifier = new AttributeList();
		identifier.putNewAttribute(TagFromName.StudyInstanceUID);
	
		// do NOT condition on slf4jlogger.isInfoEnabled() ... change in logging level should not change behavior ... should be harmless to always ask for these as return keys
		{
			//identifier.putNewAttribute(TagFromName.PatientName,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.PatientID,specificCharacterSet);

			identifier.putNewAttribute(TagFromName.StudyDate);
			identifier.putNewAttribute(TagFromName.StudyID,specificCharacterSet);
			identifier.putNewAttribute(TagFromName.StudyDescription,specificCharacterSet);
		}
		
		String patientNamesAll[] = { "" };
		String patientNamesSelective[] = { "A*", "B*", "C*", "D*", "E*", "F*", "G*", "H*", "I*", "J*", "K*", "L*", "M*", "N*", "O*", "P*", "Q*", "R*", "S*", "T*", "U*", "V*", "W*", "X*", "Y*", "Z*" };
			
		String patientNames[] = patientNameQueryPatterns == null ? (queryAll ? patientNamesAll : patientNamesSelective) : patientNameQueryPatterns;
		for (int i=0; i<patientNames.length; ++i) {
			slf4jlogger.debug("Query {}",patientNames[i]);
			{ Attribute a = new PersonNameAttribute(TagFromName.PatientName,specificCharacterSet); a.addValue(patientNames[i]); identifier.put(a); }
			QueryTreeModel tree = queryInformationModel.performHierarchicalQuery(identifier);
			QueryTreeRecord node = (QueryTreeRecord)(tree.getRoot());
			int n = node.getChildCount();
			slf4jlogger.debug("Child node count {}",n);
			if (n > 0) {
				for (int is=0; is<n; ++is) {
					QueryTreeRecord child = (QueryTreeRecord)node.getChildAt(is);
					if (child != null) {	// should not happen, but has been observed ... do not allow it to derail entire sync :(
					slf4jlogger.debug("Have child {}",child);
						InformationEntity ie = child.getInformationEntity();
						Attribute uniqueKey = child.getUniqueKey();
						AttributeTag uniqueKeyTagFromThisLevel = queryInformationModel.getUniqueKeyForInformationEntity(ie);
						String uid = uniqueKey.getSingleStringValueOrNull();	// always a UID, since StudyRoot
						if (uid == null) {
							slf4jlogger.debug("Could not get UID to use");
						}
						else {
							slf4jlogger.debug("Searching for existing records for {} {}",ie,uid);
							ArrayList recordsForThisUID  = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(ie,uid);
							slf4jlogger.debug("Found {} existing records",recordsForThisUID.size());
							AttributeList uniqueKeys = new AttributeList();
							{ Attribute a = new UniqueIdentifierAttribute(uniqueKeyTagFromThisLevel); a.addValue(uid); uniqueKeys.put(a); }
							if (recordsForThisUID.size() == 0) {
								slf4jlogger.debug("No existing records for {} {}",ie,uid);
								slf4jlogger.info("Performing retrieve for {} ({})",child,Attribute.getSingleStringValueOrEmptyString(uniqueKey));
								AttributeList retrieveIdentifier = new AttributeList();
								retrieveIdentifier.putAll(uniqueKeys);
					
								String retrieveLevelName = queryInformationModel.getQueryLevelName(ie);
								{ Attribute a = new CodeStringAttribute(TagFromName.QueryRetrieveLevel); a.addValue(retrieveLevelName); retrieveIdentifier.put(a); }
								slf4jlogger.debug("Retrieve identifier:\n{}",retrieveIdentifier);
								try {
									bytesSaved=0;
									long startOfRetrieval=System.currentTimeMillis();
									slf4jlogger.info("Retrieving with C-MOVE");
									//queryInformationModel.performHierarchicalMoveFrom(retrieveIdentifier,remoteAE);
									MoveSOPClassSCU moveSOPClassSCU = null;
									Association association = queryInformationModel.getCMoveAssociation();
									if (association != null) {
										moveSOPClassSCU = new MoveSOPClassSCU(association,localAE,SOPClass.StudyRootQueryRetrieveInformationModelMove,retrieveIdentifier);
									}
									else {
										moveSOPClassSCU = new MoveSOPClassSCU(remoteHost,remotePort,remoteAE,localAE,localAE,SOPClass.StudyRootQueryRetrieveInformationModelMove,retrieveIdentifier);
									}
									int moveStatus = moveSOPClassSCU.getStatus();
									if (moveStatus != 0x0000) {
										if (slf4jlogger.isInfoEnabled()) slf4jlogger.info("SynchronizeFromRemoteSCP: unsuccessful move status = 0x{}",Integer.toHexString(moveStatus));
									}
									if (slf4jlogger.isInfoEnabled()) {
										long durationOfRetrieval = System.currentTimeMillis() - startOfRetrieval;
										double rate = durationOfRetrieval == 0 ? 0l : ((double)bytesSaved)/1000000/(((double)durationOfRetrieval)/1000);
										slf4jlogger.info("Saved {} bytes in {} ms, {} MB/s",commaFormatter.format(bytesSaved),commaFormatter.format(durationOfRetrieval),rate);
										totalDurationOfRetrieval += durationOfRetrieval;
										totalBytesSaved += bytesSaved;
									}
								}
								catch (Exception e) {
									slf4jlogger.error("",e);
								}
							}
							else {
								slf4jlogger.debug("Existing records for {} {} so retrieving nothing (NOT checking all series or instances)",ie,uid);
							}
						}
					}
				}
			}
		}
	}
	
	protected static class OurReadTerminationStrategy implements AttributeList.ReadTerminationStrategy {
		public boolean terminate(AttributeList attributeList,AttributeTag tag,long byteOffset) {
			return tag.getGroup() > 0x0020;
		}
	}
	
	protected final static AttributeList.ReadTerminationStrategy terminateAfterRelationshipGroup = new OurReadTerminationStrategy();
	
	long bytesSaved;

	/**
	 */
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
				inactivityTime = 0;
				if (slf4jlogger.isInfoEnabled()) {
					slf4jlogger.info("Received: {} from {} in {}",dicomFileName,callingAETitle,transferSyntax);
					bytesSaved += new File(dicomFileName).length();
				}
				++numberOfSOPInstancesReceived;
				try {
					// no need for case insensitive check here ... was locally created
					FileInputStream fis = new FileInputStream(dicomFileName);
					DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
					AttributeList list = new AttributeList();
					list.read(i,terminateAfterRelationshipGroup);
					i.close();
					fis.close();
					String sopInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
					slf4jlogger.trace("Received: {} with SOPInstanceUID {}",dicomFileName,sopInstanceUID);
					if (sopInstanceUID != null) {
						String newFileName = MoveDicomFilesIntoHierarchy.renameFileWithHierarchicalPathFromAttributes(new File(dicomFileName),list,savedInstancesFolder.getCanonicalPath(),"Duplicates");
						if (newFileName != null) {
							dicomFileName = newFileName;
						}
						if (retrieveStudy) {
							databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
						}
						else {
							++numberOfValidSOPInstancesReceived;
							if (setofInstancesExpected.contains(sopInstanceUID)) {
								setofInstancesExpected.remove(sopInstanceUID);
								databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
							}
							else {
								++numberOfUnrequestedSOPInstancesReceived;
								databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
								throw new DicomException("Unrequested SOPInstanceUID "+sopInstanceUID+" in received object ... stored it anyway");
							}
						}
					}
					else {
						// should probably delete it, but "bad" file may be useful :(
						throw new DicomException("Missing SOPInstanceUID in received object ... not inserting file "+dicomFileName+" in database");
					}
				} catch (Exception e) {
					slf4jlogger.error("Unable to insert {} received from {} in {} into database",dicomFileName,callingAETitle,transferSyntax,e);
				}
			}

		}
	}
	
	private void readPatientNameQueryPatternsFile(String queryPatternFileName) throws IOException {
		List<String> patientNameQueryPatternsList = Files.readAllLines(FileSystems.getDefault().getPath(queryPatternFileName),StandardCharsets.US_ASCII);
		if (patientNameQueryPatternsList != null && patientNameQueryPatternsList.size() > 0) {
			patientNameQueryPatterns = patientNameQueryPatternsList.toArray(new String[patientNameQueryPatternsList.size()]);
		}
	}
	
	/**
	 * <p>Synchronize the contents of a local database of DICOM objects with a remote SCP.</p>
	 *
	 * <p>Queries the remote SCP for everything it has and retrieves all instances not already present in the specified local database.</p>
	 *
	 * @param	databaseInformationModel	the local database (will be created if does not already exist)
	 * @param	savedInstancesFolder		where to save retrieved instances (must already exist)
	 * @param	remoteHost
	 * @param	remotePort
	 * @param	remoteAE
	 * @param	localPort					local port for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	localAE						local AET for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	useGet						if true, use C-GET rather than C-MOVE
	 * @param	queryAll					if true query for all patient names at once, rather than selectively by first letter, unless there is a queryPatternFileName
	 * @param	queryPatternFileName		a file containing a list of PatientName query patterns, one per line
	 * @param	anyTransferSyntax			if true, accept any Transfer Syntax, not just uncompressed ones
	 * @param	retrieveStudy				if true, retrieve only at STUDY level, not confirming every instance
	 * @param	reuseAssociations			if true, keep alive and reuse Associations
	 * @throws DicomException
	 * @throws DicomNetworkException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public SynchronizeFromRemoteSCP(DatabaseInformationModel databaseInformationModel,File savedInstancesFolder,
				String remoteHost,int remotePort,String remoteAE,int localPort,String localAE,boolean useGet,boolean queryAll,String queryPatternFileName,boolean anyTransferSyntax,boolean retrieveStudy,boolean reuseAssociations)
			throws DicomException, DicomNetworkException, IOException, InterruptedException {
		this.databaseInformationModel = databaseInformationModel;
		this.savedInstancesFolder = savedInstancesFolder;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.remoteAE = remoteAE;
		this.localPort = localPort;
		this.localAE = localAE;
		this.useGet = useGet;
		this.queryAll = queryAll;
		this.retrieveStudy = retrieveStudy;
		
		this.patientNameQueryPatterns = null;
		if (queryPatternFileName != null && queryPatternFileName.length() > 0) {
			readPatientNameQueryPatternsFile(queryPatternFileName);
		}
		
		if (!savedInstancesFolder.exists() || !savedInstancesFolder.isDirectory()) {
			throw new DicomException("Folder in which to save received instances does not exist or is not a directory - "+savedInstancesFolder);
		}
		
		receivedObjectHandler = new OurReceivedObjectHandler();
		identifierHandler = new IdentifierHandler();
		
		if (useGet) {
			setofClassesExpected = new HashSet();
		}
		else {
			new Thread(new StorageSOPClassSCPDispatcher(localPort,localAE,savedInstancesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS,receivedObjectHandler,
														null/*queryResponseGeneratorFactory*/,null/*retrieveResponseGeneratorFactory*/,null/*networkApplicationInformation*/,
														anyTransferSyntax ? new AnyExplicitStorePresentationContextSelectionPolicy() : new UnencapsulatedExplicitStorePresentationContextSelectionPolicy(),
														false/*secureTransport*/)).start();
		}
		setofInstancesExpected = new HashSet();
		numberOfSOPInstancesReceived = 0;
		numberOfValidSOPInstancesReceived = 0;
		numberOfUnrequestedSOPInstancesReceived = 0;
		
		queryInformationModel = new StudyRootQueryInformationModel(remoteHost,remotePort,remoteAE,localAE,reuseAssociations);
		if (retrieveStudy) {
			performQueryAndStudyRetrieve(anyTransferSyntax);
		}
		else {
			performQueryAndWalkTreeDownToInstanceLevelAndRetrieve(anyTransferSyntax);
		}
		
		inactivityTime = 0;
		while (!setofInstancesExpected.isEmpty() && inactivityTime > inactivityTimeOut) {
			slf4jlogger.info("Sleeping since {} remaining",setofInstancesExpected.size());
			Thread.currentThread().sleep(sleepTimeBetweenCheckingForNothingExpectedBeforeExiting);
			inactivityTime+=sleepTimeBetweenCheckingForNothingExpectedBeforeExiting;
		}
		slf4jlogger.info("Finished with {} instances received, of which {} were valid, and {} were unrequested; requested but never received were {} instances",numberOfSOPInstancesReceived,numberOfValidSOPInstancesReceived,numberOfUnrequestedSOPInstancesReceived,setofInstancesExpected.size());
		if (slf4jlogger.isInfoEnabled()) {
			double rate = totalDurationOfRetrieval == 0 ? 0l : ((double)totalBytesSaved)/1000000/(((double)totalDurationOfRetrieval)/1000);
			slf4jlogger.info("Total saved {} bytes in {} ms, {} MB/s",commaFormatter.format(totalBytesSaved),commaFormatter.format(totalDurationOfRetrieval),rate);
		}
	}
	/**
	 * <p>Synchronize the contents of a local database of DICOM objects with a remote SCP.</p>
	 *
	 * <p>Queries the remote SCP for everything it has and retrieves all instances not already present in the specified local database.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging.
	 * @param	databaseInformationModel	the local database (will be created if does not already exist)
	 * @param	savedInstancesFolder		where to save retrieved instances (must already exist)
	 * @param	remoteHost
	 * @param	remotePort
	 * @param	remoteAE
	 * @param	localPort					local port for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	localAE						local AET for DICOM listener ... must already be known to remote AE unless C-GET
	 * @param	useGet						if true, use C-GET rather than C-MOVE
	 * @param	queryAll					if true query for all patient names at once, rather than selectively by first letter, unless there is a queryPatternFileName
	 * @param	queryPatternFileName		a file containing a list of PatientName query patterns, one per line
	 * @param	verbosityLevel				ignored
	 * @param	debugLevel					ignored
	 * @param	anyTransferSyntax			if true, accept any Transfer Syntax, not just uncompressed ones
	 * @param	retrieveStudy				if true, retrieve only at STUDY level, not confirming every instance
	 * @param	reuseAssociations			if true, keep alive and reuse Associations
	 * @throws DicomException
	 * @throws DicomNetworkException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public SynchronizeFromRemoteSCP(DatabaseInformationModel databaseInformationModel,File savedInstancesFolder,
				String remoteHost,int remotePort,String remoteAE,int localPort,String localAE,boolean useGet,boolean queryAll,String queryPatternFileName,int verbosityLevel,int debugLevel,boolean anyTransferSyntax,boolean retrieveStudy,boolean reuseAssociations)
			throws DicomException, DicomNetworkException, IOException, InterruptedException {
		this(databaseInformationModel,savedInstancesFolder,remoteHost,remotePort,remoteAE,localPort,localAE,useGet,queryAll,queryPatternFileName,anyTransferSyntax,retrieveStudy,reuseAssociations);
		slf4jlogger.warn("Debug level supplied in constructor ignored");
	}
	
	/**
	 * <p>Synchronize the contents of a local database of DICOM objects with a remote SCP.</p>
	 *
	 * <p>Queries the remote SCP for everything it has and retrieves all instances not already present in the specified local database.</p>
	 *
	 * <p>Will register the supplied local AE and port with Bonjour if supported (this is specific to the main() method; the constructor of the class itself does not do this).</p>
	 *
	 * @param	arg		array of 6 to 12 strings - the fully qualified path of the database file prefix, the fully qualified path of the saved incoming files folder,
	 *					the remote hostname, remote port, remote AE Title, our port (ignored if GET), our AE Title,
	 *					optionally GET or MOVE (defaults to MOVE),
	 *					optionally query by ALL or SELECTIVE patient name (defaults to ALL) or a filename containing a list of PatientName query patterns (one per line),
	 *					optionally UNCOMPRESSED or ANY (defaults to UNCOMPRESSED)
	 *					optionally a retrieval level STUDY or INSTANCE (defaults to INSTANCE)
	 *					optionally REUSE or NEW associations for each query and retrieval (defaults to NEW)
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 6 && arg.length <= 12) {
				String databaseFileName = arg[0];
				String savedInstancesFolderName = arg[1];
				String remoteHost = arg[2];
				int remotePort = Integer.parseInt(arg[3]);
				String remoteAE = arg[4];
				int localPort = Integer.parseInt(arg[5]);
				String localAE = arg[6];
				boolean useGet = arg.length > 7 ? (arg[7].trim().toUpperCase(java.util.Locale.US).equals("GET") ? true : false) : false;
				boolean queryAll = true;
				String queryPatternFileName = null;
				if (arg.length > 8) {
					String queryAllOption = arg[8].trim();
					String queryAllOptionUC = queryAllOption.toUpperCase(java.util.Locale.US);
					if (queryAllOptionUC.equals("ALL")) {
						queryAll = true;
					}
					else if (queryAllOptionUC.equals("SELECTIVE")) {
						queryAll = false;
					}
					else {
						queryPatternFileName = queryAllOption;
					}
				}
				boolean anyTransferSyntax = arg.length > 9 ? (arg[9].trim().toUpperCase(java.util.Locale.US).equals("ANY") ? true : false) : false;
				boolean retrieveStudy = arg.length > 10 ? (arg[10].trim().toUpperCase(java.util.Locale.US).equals("STUDY") ? true : false) : false;
				boolean reuseAssociations = arg.length > 11 ? (arg[11].trim().toUpperCase(java.util.Locale.US).equals("REUSE") ? true : false) : false;
				
				//if (useGet && anyTransferSyntax) {
				//	slf4jlogger.info("ANY Transfer Syntax can only be used with MOVE not GET");
				//	System.exit(0);
				//}
				if (useGet && retrieveStudy) {
					slf4jlogger.info("STUDY level retrieval can only be used with MOVE not GET");
					System.exit(0);
				}
				
				File savedInstancesFolder = new File(savedInstancesFolderName);
		
				DatabaseInformationModel databaseInformationModel = new MinimalPatientStudySeriesInstanceModel(databaseFileName);
				
				// attempt to register ourselves in case remote host does not already know us and supports Bonjour ... OK if this fails
				try {
					NetworkConfigurationFromMulticastDNS networkConfigurationFromMulticastDNS = new NetworkConfigurationFromMulticastDNS();
					networkConfigurationFromMulticastDNS.activateDiscovery();
					networkConfigurationFromMulticastDNS.registerDicomService(localAE,localPort,"WSD");
					Thread.currentThread().sleep(sleepTimeAfterRegisteringWithBonjour);		// wait a little while, in case remote host slow to pick up our AE information (else move might fail)
				}
				catch (Exception e) {
					slf4jlogger.info("",e);
				}

				new SynchronizeFromRemoteSCP(databaseInformationModel,savedInstancesFolder,remoteHost,remotePort,remoteAE,localPort,localAE,useGet,queryAll,queryPatternFileName,anyTransferSyntax,retrieveStudy,reuseAssociations);
				
				databaseInformationModel.close();	// important, else some received objects may not be registered in the database

				System.exit(0);		// this is untidy, but necessary if we are too lazy to stop the StorageSOPClassSCPDispatcher thread :(
			}
			else {
				slf4jlogger.info("Usage: java -cp ./pixelmed.jar:./lib/additional/hsqldb.jar:./lib/additional/commons-codec-1.3.jar:./lib/additional/jmdns.jar com.pixelmed.apps.SynchronizeFromRemoteSCP databasepath savedfilesfolder remoteHost remotePort remoteAET ourPort ourAET [GET|MOVE [ALL|SELECTIVE|patternfile] [UNCOMPRESSED|ANY [STUDY|INSTANCE [REUSE|NEW]]]]]");
			}
		}
		catch (Exception e) {
			slf4jlogger.info("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}





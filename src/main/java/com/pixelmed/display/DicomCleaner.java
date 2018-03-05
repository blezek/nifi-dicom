/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.DatabaseTreeBrowser;
import com.pixelmed.database.DatabaseTreeRecord;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.dicom.*;

import com.pixelmed.event.ApplicationEventDispatcher;

import com.pixelmed.display.event.StatusChangeEvent;

import com.pixelmed.display.DialogMessageLogger;

import com.pixelmed.network.UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.MultipleInstanceTransferStatusHandler;
import com.pixelmed.network.MultipleInstanceTransferStatusHandlerWithFileName;
import com.pixelmed.network.NetworkApplicationConfigurationDialog;
import com.pixelmed.network.NetworkApplicationInformation;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.PresentationAddress;
import com.pixelmed.network.PresentationContext;
import com.pixelmed.network.PresentationContextListFactory;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;
import com.pixelmed.network.TransferSyntaxSelectionPolicy;

import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeBrowser;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

import com.pixelmed.utils.CapabilitiesAvailable;
import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.StringUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import javax.swing.border.Border;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.TreePath;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is an application for importing or retrieving DICOM studies,
 * cleaning them (i.e., de-identifying them or replacing UIDs, etc.), and
 * sending them elsewhere.</p>
 * 
 * <p>It is configured by use of a properties file that resides in the user's
 * home directory in <code>.com.pixelmed.display.DicomCleaner.properties</code>.
 * The properties allow control over the user interface elements that are displayed
 * and record the settings changed by the user when the application closes.</p>
 *
 * <p>For a description of the network configuration properties, see {@link com.pixelmed.network.NetworkApplicationProperties NetworkApplicationProperties}.</p>
 *
 * <p>The properties that are specific to the application, and their default values, are as follows</p>
 *
 * <p><code>Application.Allow.ChangeDatesAndTimes=true</code> - display the change dates and times panel</p>
 * <p><code>Application.Allow.CheckBox.AcceptAnyTransferSyntax=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.AddContributingEquipment=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.CleanUIDs=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.HierarchicalExport=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveCharacteristics=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveClinicalTrialAttributes=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveDescriptions=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveDeviceIdentity=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveIdentity=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveInstitutionIdentity=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemovePrivate=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveProtocolName=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.RemoveSeriesDescriptions=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.CheckBox.ZipExport=true</code> - display the checkbox</p>
 * <p><code>Application.Allow.NetworkConfiguration=true</code> - display the Configure button</p>
 * <p><code>Application.Allow.UserQuery=true</code> - display the query/retrieve buttons, results panel and keys panel</p>
 * <p><code>Application.CheckBox.IsSelected.AcceptAnyTransferSyntax=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.AddContributingEquipment=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.CleanUIDs=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.HierarchicalExport=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.ModifyDates=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveCharacteristics=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveClinicalTrialAttributes=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveDescriptions=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveDeviceIdentity=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveIdentity=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveInstitutionIdentity=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemovePrivate=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveProtocolName=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.RemoveSeriesDescriptions=false</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.ReplaceAccessionNumber=true</code> - selection status ofthe checkbox </p>
 * <p><code>Application.CheckBox.IsSelected.ReplacePatientID=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.ReplacePatientName=true</code> - selection status of the checkbox</p>
 * <p><code>Application.CheckBox.IsSelected.ZipExport=false</code> - selection status of the checkbox</p>
 * <p><code>Application.ModifyDatesEpoch=20000101</code> - text value of the dates epoch</p>
 * <p><code>Application.ReplacementText.AccessionNumber=</code> - text value of the Accession Number replacement field</p>
 * <p><code>Application.ReplacementText.PatientID=NOID</code> - text value of Patient ID replacement field</p>
 * <p><code>Application.ReplacementText.PatientName=NAME^NONE</code> - text value of Patient Name replacement field</p>
 * <p><code>Application.RandomReplacementPatientNamePrefix=Anon^</code> - prefix for random value of Patient Name replacement field</p>
 * <p><code>Application.RandomReplacementPatientIDLength=16</code> - length for zero padded random value of Patient ID (and Patient Name suffix) replacement field</p>
 * <p><code>Application.RandomReplacementAccessionNumberLength=16</code> - length for zero padded random value of Accession Number replacement field</p>
 * <p><code>Application.DialogLogger.showDateTime=true</code> - prepend log entries with a time stamp</p>
 * <p><code>Application.DialogLogger.dateTimeFormat=yyyy-MM-dd'T'HH:mm:ss.SSSZ</code> - the format for the time stamp in java.text.SimpleDateFormat format (if absent, milliseconds since starting)</p>
 *
 * @author	dclunie
 */
public class DicomCleaner extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomCleaner.java,v 1.90 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomCleaner.class);

	protected static String resourceBundleName  = "com.pixelmed.display.DicomCleaner";
	protected static String propertiesFileName  = ".com.pixelmed.display.DicomCleaner.properties";
	
	protected static String propertyName_DicomCurrentlySelectedStorageTargetAE = "Dicom.CurrentlySelectedStorageTargetAE";
	protected static String propertyName_DicomCurrentlySelectedQueryTargetAE = "Dicom.CurrentlySelectedQueryTargetAE";
	
	protected static String propertyName_AllowUserQuery = "Application.Allow.UserQuery";
	
	protected static String propertyName_AllowNetworkConfiguration = "Application.Allow.NetworkConfiguration";
	
	protected static String propertyName_AllowChangeDatesAndTimes = "Application.Allow.ChangeDatesAndTimes";

	protected static String propertyName_AllowRemoveIdentityCheckBox = "Application.Allow.CheckBox.RemoveIdentity";
	protected static String propertyName_AllowRemoveDescriptionsCheckBox = "Application.Allow.CheckBox.RemoveDescriptions";
	protected static String propertyName_AllowRemoveSeriesDescriptionsCheckBox = "Application.Allow.CheckBox.RemoveSeriesDescriptions";
	protected static String propertyName_AllowRemoveProtocolNameCheckBox = "Application.Allow.CheckBox.RemoveProtocolName";
	protected static String propertyName_AllowRemoveCharacteristicsCheckBox = "Application.Allow.CheckBox.RemoveCharacteristics";
	protected static String propertyName_AllowRemoveDeviceIdentityCheckBox = "Application.Allow.CheckBox.RemoveDeviceIdentity";
	protected static String propertyName_AllowRemoveInstitutionIdentityCheckBox = "Application.Allow.CheckBox.RemoveInstitutionIdentity";
	protected static String propertyName_AllowCleanUIDsCheckBox = "Application.Allow.CheckBox.CleanUIDs";
	protected static String propertyName_AllowRemovePrivateCheckBox = "Application.Allow.CheckBox.RemovePrivate";
	protected static String propertyName_AllowAddContributingEquipmentCheckBox = "Application.Allow.CheckBox.AddContributingEquipment";
	protected static String propertyName_AllowRemoveClinicalTrialAttributesCheckBox = "Application.Allow.CheckBox.RemoveClinicalTrialAttributes";
	protected static String propertyName_AllowZipExportCheckBox = "Application.Allow.CheckBox.ZipExport";
	protected static String propertyName_AllowHierarchicalExportCheckBox = "Application.Allow.CheckBox.HierarchicalExport";
	protected static String propertyName_AllowAcceptAnyTransferSyntaxCheckBox = "Application.Allow.CheckBox.AcceptAnyTransferSyntax";

	protected static String propertyName_ReplacementTextPatientName = "Application.ReplacementText.PatientName";
	protected static String propertyName_ReplacementTextPatientID = "Application.ReplacementText.PatientID";
	protected static String propertyName_ReplacementTextAccessionNumber = "Application.ReplacementText.AccessionNumber";

	protected static String propertyName_ShowDateTime = "Application.DialogLogger.showDateTime";
	protected static String propertyName_DateTimeFormat = "Application.DialogLogger.dateTimeFormat";

	protected static String propertyName_CheckBoxReplacePatientNameIsSelected = "Application.CheckBox.IsSelected.ReplacePatientName";
	protected static String propertyName_CheckBoxReplacePatientIDIsSelected = "Application.CheckBox.IsSelected.ReplacePatientID";
	protected static String propertyName_CheckBoxReplaceAccessionNumberIsSelected = "Application.CheckBox.IsSelected.ReplaceAccessionNumber";

	protected static String propertyName_CheckBoxModifyDatesIsSelected = "Application.CheckBox.IsSelected.ModifyDates";
	
	protected static String propertyName_ModifyDatesEpoch = "Application.ModifyDatesEpoch";
	
	protected static String propertyName_CheckBoxRemoveIdentityIsSelected = "Application.CheckBox.IsSelected.RemoveIdentity";
	protected static String propertyName_CheckBoxRemoveDescriptionsIsSelected = "Application.CheckBox.IsSelected.RemoveDescriptions";
	protected static String propertyName_CheckBoxRemoveSeriesDescriptionsIsSelected = "Application.CheckBox.IsSelected.RemoveSeriesDescriptions";
	protected static String propertyName_CheckBoxRemoveProtocolNameIsSelected = "Application.CheckBox.IsSelected.RemoveProtocolName";
	protected static String propertyName_CheckBoxRemoveCharacteristicsIsSelected = "Application.CheckBox.IsSelected.RemoveCharacteristics";
	protected static String propertyName_CheckBoxRemoveDeviceIdentityIsSelected = "Application.CheckBox.IsSelected.RemoveDeviceIdentity";
	protected static String propertyName_CheckBoxRemoveInstitutionIdentityIsSelected = "Application.CheckBox.IsSelected.RemoveInstitutionIdentity";
	protected static String propertyName_CheckBoxCleanUIDsIsSelected = "Application.CheckBox.IsSelected.CleanUIDs";
	protected static String propertyName_CheckBoxRemovePrivateIsSelected = "Application.CheckBox.IsSelected.RemovePrivate";
	protected static String propertyName_CheckBoxAddContributingEquipmentIsSelected = "Application.CheckBox.IsSelected.AddContributingEquipment";
	protected static String propertyName_CheckBoxRemoveClinicalTrialAttributesIsSelected = "Application.CheckBox.IsSelected.RemoveClinicalTrialAttributes";
	protected static String propertyName_CheckBoxZipExportIsSelected = "Application.CheckBox.IsSelected.ZipExport";
	protected static String propertyName_CheckBoxHierarchicalExportIsSelected = "Application.CheckBox.IsSelected.HierarchicalExport";
	protected static String propertyName_CheckBoxAcceptAnyTransferSyntaxIsSelected = "Application.CheckBox.IsSelected.AcceptAnyTransferSyntax";

	protected static String propertyName_RandomReplacementPatientNamePrefix     = "Application.RandomReplacementPatientNamePrefix";
	protected static String propertyName_RandomReplacementPatientIDLength       = "Application.RandomReplacementPatientIDLength";
	protected static String propertyName_RandomReplacementAccessionNumberLength = "Application.RandomReplacementAccessionNumberLength";
	
	protected static boolean default_CheckBoxReplacePatientNameIsSelected = true;
	protected static boolean default_CheckBoxReplacePatientIDIsSelected = true;
	protected static boolean default_CheckBoxReplaceAccessionNumberIsSelected = true;
	
	protected static boolean default_CheckBoxModifyDatesIsSelected = false;

	protected static boolean default_CheckBoxRemoveIdentityIsSelected = true;
	protected static boolean default_CheckBoxRemoveDescriptionsIsSelected = false;
	protected static boolean default_CheckBoxRemoveSeriesDescriptionsIsSelected = false;
	protected static boolean default_CheckBoxRemoveProtocolNameIsSelected = false;
	protected static boolean default_CheckBoxRemoveCharacteristicsIsSelected = false;
	protected static boolean default_CheckBoxRemoveDeviceIdentityIsSelected = false;
	protected static boolean default_CheckBoxRemoveInstitutionIdentityIsSelected = false;
	protected static boolean default_CheckBoxCleanUIDsIsSelected = true;
	protected static boolean default_CheckBoxRemovePrivateIsSelected = true;
	protected static boolean default_CheckBoxAddContributingEquipmentIsSelected = true;
	protected static boolean default_CheckBoxRemoveClinicalTrialAttributesIsSelected = false;
	protected static boolean default_CheckBoxZipExportIsSelected = false;
	protected static boolean default_CheckBoxHierarchicalExportIsSelected = false;
	protected static boolean default_CheckBoxAcceptAnyTransferSyntaxIsSelected = false;

	protected static boolean default_ShowDateTime = false;
	protected static String default_DateTimeFormat = null;
	
	protected static int default_RandomReplacementPatientIDLength = 16;
	protected static int default_RandomReplacementAccessionNumberLength = 16;
	
	protected static String rootNameForDicomInstanceFilesOnInterchangeMedia = "DICOM";
	protected static String filePrefixForDicomInstanceFilesOnInterchangeMedia = "I";
	protected static String fileSuffixForDicomInstanceFilesOnInterchangeMedia = "";
	protected static String nameForDicomDirectoryOnInterchangeMedia = "DICOMDIR";
	protected static String exportedZipFileName = "export.zip";

	protected static int textFieldLengthForQueryPatientName = 16;
	protected static int textFieldLengthForQueryPatientID = 10;
	protected static int textFieldLengthForQueryStudyDate = 8;
	protected static int textFieldLengthForQueryAccessionNumber = 10;

	protected static int textFieldLengthForReplacementPatientName = 16;
	protected static int textFieldLengthForReplacementPatientID = 10;
	protected static int textFieldLengthForReplacementAccessionNumber = 10;
	protected static int textFieldLengthForModifyDates = 8;

	protected ResourceBundle resourceBundle;
	protected DatabaseInformationModel srcDatabase;
	protected DatabaseInformationModel dstDatabase;
	
	protected JPanel srcDatabasePanel;
	protected JPanel dstDatabasePanel;
	protected JPanel remoteQueryRetrievePanel;
	
	protected JCheckBox removeIdentityCheckBox;
	protected JCheckBox removeDescriptionsCheckBox;
	protected JCheckBox removeSeriesDescriptionsCheckBox;
	protected JCheckBox removeProtocolNameCheckBox;
	protected JCheckBox removeCharacteristicsCheckBox;
	protected JCheckBox removeDeviceIdentityCheckBox;
	protected JCheckBox removeInstitutionIdentityCheckBox;
	protected JCheckBox cleanUIDsCheckBox;
	protected JCheckBox removePrivateCheckBox;
	protected JCheckBox addContributingEquipmentCheckBox;
	protected JCheckBox removeClinicalTrialAttributesCheckBox;
	protected JCheckBox zipExportCheckBox;
	protected JCheckBox hierarchicalExportCheckBox;
	protected JCheckBox acceptAnyTransferSyntaxCheckBox;

	protected JCheckBox replacePatientNameCheckBox;
	protected JCheckBox replacePatientIDCheckBox;
	protected JCheckBox replaceAccessionNumberCheckBox;
	protected JCheckBox modifyDatesCheckBox;
	
	protected JTextField replacementPatientNameTextField;
	protected JTextField replacementPatientIDTextField;
	protected JTextField replacementAccessionNumberTextField;
	protected JTextField modifyDatesTextField;

	protected JTextField queryFilterPatientNameTextField;
	protected JTextField queryFilterPatientIDTextField;
	protected JTextField queryFilterStudyDateTextField;
	protected JTextField queryFilterAccessionNumberTextField;

	protected String randomReplacementPatientNamePrefix;
	protected int randomReplacementPatientIDLength;
	protected int randomReplacementAccessionNumberLength;
	
	protected SafeProgressBarUpdaterThread progressBarUpdater;
	
	protected SafeCursorChanger cursorChanger;
	
	protected MessageLogger logger;
	
	protected NetworkApplicationProperties networkApplicationProperties;
	protected NetworkApplicationInformation networkApplicationInformation;
	
	protected QueryInformationModel currentRemoteQueryInformationModel;
	
	protected QueryTreeRecord currentRemoteQuerySelectionQueryTreeRecord;
	protected AttributeList currentRemoteQuerySelectionUniqueKeys;
	protected Attribute currentRemoteQuerySelectionUniqueKey;
	protected String currentRemoteQuerySelectionRetrieveAE;
	protected String currentRemoteQuerySelectionLevel;

	protected String ourCalledAETitle;		// set when reading network properties; used not just in StorageSCP, but also when creating exported meta information headers
	
	protected void setCurrentRemoteQueryInformationModel(String remoteAEForQuery) {
		currentRemoteQueryInformationModel=null;
		String stringForTitle="";
		if (remoteAEForQuery != null && remoteAEForQuery.length() > 0 && networkApplicationProperties != null && networkApplicationInformation != null) {
			try {
				String              queryCallingAETitle = networkApplicationProperties.getCallingAETitle();
				String               queryCalledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(remoteAEForQuery);
				PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(queryCalledAETitle);
				
				if (presentationAddress == null) {
					throw new Exception("For remote query AE <"+remoteAEForQuery+">, presentationAddress cannot be determined");
				}
				
				String                        queryHost = presentationAddress.getHostname();
				int			      queryPort = presentationAddress.getPort();
				String                       queryModel = networkApplicationInformation.getApplicationEntityMap().getQueryModel(queryCalledAETitle);
				
				if (NetworkApplicationProperties.isStudyRootQueryModel(queryModel) || queryModel == null) {
					currentRemoteQueryInformationModel=new StudyRootQueryInformationModel(queryHost,queryPort,queryCalledAETitle,queryCallingAETitle);
					stringForTitle=":"+remoteAEForQuery;
				}
				else {
					throw new Exception("For remote query AE <"+remoteAEForQuery+">, query model "+queryModel+" not supported");
				}
			}
			catch (Exception e) {		// if an AE's property has no value, or model not supported
				slf4jlogger.error("Setting remote query target failed ",e);
			}
		}
	}

	private String showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(String message,String title,String defaultSelection) {
		String ae = defaultSelection;
		if (networkApplicationInformation != null) {
			Set localNamesOfRemoteAEs = networkApplicationInformation.getListOfLocalNamesOfApplicationEntities();
			if (localNamesOfRemoteAEs != null) {
				String sta[] = new String[localNamesOfRemoteAEs.size()];
				int i=0;
				Iterator it = localNamesOfRemoteAEs.iterator();
				while (it.hasNext()) {
					sta[i++]=(String)(it.next());
				}
				ae = (String)JOptionPane.showInputDialog(getContentPane(),message,title,JOptionPane.QUESTION_MESSAGE,null,sta,ae);
			}
		}
		return ae;
	}
	
	protected static void importFileIntoDatabase(DatabaseInformationModel database,String dicomFileName,String fileReferenceType,Map<String,Date> earliestDatesIndexedBySourceFilePath) throws FileNotFoundException, IOException, DicomException {
		ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing: "+dicomFileName));
//System.err.println("Importing: "+dicomFileName);
		FileInputStream fis = new FileInputStream(dicomFileName);
		DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
		AttributeList list = new AttributeList();
		list.read(i,TagFromName.PixelData);
		i.close();
		fis.close();
		database.insertObject(list,dicomFileName,fileReferenceType);
		if (earliestDatesIndexedBySourceFilePath != null) {
			Date earliestInObject = ClinicalTrialsAttributes.findEarliestDateTime(list);
			if (earliestInObject != null) {
				earliestDatesIndexedBySourceFilePath.put(dicomFileName,earliestInObject);
//System.err.println("Earliest date "+earliestInObject+" for "+dicomFileName);
//if (earliestInObject.getTime() < 0) { System.err.println("Unlikely earliest date "+earliestInObject+" before 1970 in "+dicomFileName); }
			}
		}
	}

	protected Map<String,Date> earliestDatesIndexedBySourceFilePath = new HashMap<String,Date>();

	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
				String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(callingAETitle);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Received "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax));
//System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				logger.sendLn("Received "+dicomFileName+" from "+localName+" ("+callingAETitle+")");
				try {
					importFileIntoDatabase(srcDatabase,dicomFileName,DatabaseInformationModel.FILE_COPIED,earliestDatesIndexedBySourceFilePath);
					srcDatabasePanel.removeAll();
					new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
					srcDatabasePanel.validate();
					new File(dicomFileName).deleteOnExit();
				} catch (Exception e) {
					slf4jlogger.error("Unable to insert {} received from {} in {} into database",dicomFileName,callingAETitle,transferSyntax,e);
				}
			}

		}
	}

	protected File savedImagesFolder;

	protected StorageSOPClassSCPDispatcher storageSOPClassSCPDispatcher;
	
	/**
	 * <p>Start DICOM storage listener for populating source database.</p>
	 *
	 * @throws	DicomException
	 */
	protected void activateStorageSCP() throws DicomException, IOException {
		// Start up DICOM association listener in background for receiving images and responding to echoes ...
		if (networkApplicationProperties != null) {
			{
				int port = networkApplicationProperties.getListeningPort();
				ourCalledAETitle = networkApplicationProperties.getCalledAETitle();
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Starting up DICOM association listener on port "+port+" AET "+ourCalledAETitle));
				slf4jlogger.info("Starting up DICOM association listener on port {} AET {}",port,ourCalledAETitle);
				storageSOPClassSCPDispatcher = new StorageSOPClassSCPDispatcher(port,ourCalledAETitle,
					networkApplicationProperties.getAcceptorMaximumLengthReceived(),networkApplicationProperties.getAcceptorSocketReceiveBufferSize(),networkApplicationProperties.getAcceptorSocketSendBufferSize(),
					savedImagesFolder,StoredFilePathStrategy.BYSOPINSTANCEUIDINSINGLEFOLDER,new OurReceivedObjectHandler(),
					null/*AssociationStatusHandler*/,
					srcDatabase == null ? null : srcDatabase.getQueryResponseGeneratorFactory(),
					srcDatabase == null ? null : srcDatabase.getRetrieveResponseGeneratorFactory(),
					networkApplicationInformation,
					new OurPresentationContextSelectionPolicy(),
					false/*secureTransport*/);
				new Thread(storageSOPClassSCPDispatcher).start();
			}
		}
	}
	
	class OurPresentationContextSelectionPolicy extends UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy {
		OurPresentationContextSelectionPolicy() {
			super();
			transferSyntaxSelectionPolicy = new OurTransferSyntaxSelectionPolicy();
		}
	}
	
	// we will (grudgingly) accept JPEGBaseline, since we know the JRE can natively decode it without JIIO extensions present,
	// so will work by decompressing during attribute list read for cleaning

	class OurTransferSyntaxSelectionPolicy extends TransferSyntaxSelectionPolicy {
		public LinkedList applyTransferSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber) {
//System.err.println("DicomCleaner.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): offered "+presentationContexts);
			boolean canUseBzip = CapabilitiesAvailable.haveBzip2Support();
			ListIterator pcsi = presentationContexts.listIterator();
			while (pcsi.hasNext()) {
				PresentationContext pc = (PresentationContext)(pcsi.next());
				boolean foundExplicitVRLittleEndian = false;
				boolean foundImplicitVRLittleEndian = false;
				boolean foundExplicitVRBigEndian = false;
				boolean foundDeflated = false;
				boolean foundBzipped = false;
				boolean foundJPEGBaseline = false;
				boolean foundJPEGLossless = false;
				boolean foundJPEGLosslessSV1 = false;
				boolean foundJPEG2000 = false;
				boolean foundJPEG2000Lossless = false;
				boolean foundJPEGLSLossless = false;
				boolean foundJPEGLSNearLossless = false;
				String lastRecognized = null;
				List tsuids = pc.getTransferSyntaxUIDs();
				ListIterator tsuidsi = tsuids.listIterator();
				while (tsuidsi.hasNext()) {
					String transferSyntaxUID=(String)(tsuidsi.next());
					if (transferSyntaxUID != null) {
						if      (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)) foundImplicitVRLittleEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)) foundExplicitVRLittleEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)) foundExplicitVRBigEndian = true;
						else if (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)) foundDeflated = true;
						else if (transferSyntaxUID.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian)) foundBzipped = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)) foundJPEGBaseline = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGLossless)) foundJPEGLossless = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1)) foundJPEGLosslessSV1 = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000)) foundJPEG2000 = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless)) foundJPEG2000Lossless = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGLS)) foundJPEGLSLossless = true;
						else if (transferSyntaxUID.equals(TransferSyntax.JPEGNLS)) foundJPEGLSNearLossless = true;
						else if (new TransferSyntax(transferSyntaxUID).isRecognized()) lastRecognized = transferSyntaxUID;
					}
				}
				// discard old list and make a new one ...
				pc.newTransferSyntaxUIDs();
				// Policy is prefer bzip then deflate compressed then explicit (little then big) then implicit,
				// then supported image compression transfer syntaxes in the following order and ignore anything else
				// unless the acceptAnyTransferSyntaxCheckBox is selected, in which case the last recognized transfer syntax in the offered list will be used
				// with the intent of having the sender decompress the image compression transfer syntaxes if it provided multiple choices.
				// must only support ONE in response
				if (foundBzipped && canUseBzip) {
					pc.addTransferSyntaxUID(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian);
				}
				else if (foundDeflated) {
					pc.addTransferSyntaxUID(TransferSyntax.DeflatedExplicitVRLittleEndian);
				}
				else if (foundExplicitVRLittleEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRLittleEndian);
				}
				else if (foundExplicitVRBigEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ExplicitVRBigEndian);
				}
				else if (foundImplicitVRLittleEndian) {
					pc.addTransferSyntaxUID(TransferSyntax.ImplicitVRLittleEndian);
				}
				else if (foundJPEGBaseline) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGBaseline);
				}
				else if (foundJPEGLossless && CapabilitiesAvailable.haveJPEGLosslessCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGLossless);
				}
				else if (foundJPEGLosslessSV1 && CapabilitiesAvailable.haveJPEGLosslessCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGLosslessSV1);
				}
				else if (foundJPEG2000 && CapabilitiesAvailable.haveJPEG2000Part1Codec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEG2000);
				}
				else if (foundJPEG2000Lossless && CapabilitiesAvailable.haveJPEG2000Part1Codec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEG2000Lossless);
				}
				else if (foundJPEGLSLossless && CapabilitiesAvailable.haveJPEGLSCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGLS);
				}
				else if (foundJPEGLSNearLossless && CapabilitiesAvailable.haveJPEGLSCodec()) {
					pc.addTransferSyntaxUID(TransferSyntax.JPEGNLS);
				}
				else if (acceptAnyTransferSyntaxCheckBox.isSelected() && lastRecognized != null) {
					pc.addTransferSyntaxUID(lastRecognized);
				}
				else {
					pc.setResultReason((byte)4);				// transfer syntaxes not supported (provider rejection)
				}
			}
//System.err.println("DicomCleaner.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): accepted "+presentationContexts);
			return presentationContexts;
		}
	}

	/**
	 * <p>Start two databases, one for the "source" instances and one for the "target" instances.</p>
	 *
	 * <p>Neither will persist when the application is closed, so in memory databases
	 *  only are used and instances live in the temporary filesystem.</p>
	 *
	 * @throws	DicomException
	 */
	protected void activateTemporaryDatabases() throws DicomException {
		srcDatabase = new PatientStudySeriesConcatenationInstanceModel("mem:src",null,resourceBundle.getString("DatabaseRootTitleForOriginal"));
		dstDatabase = new PatientStudySeriesConcatenationInstanceModel("mem:dst",null,resourceBundle.getString("DatabaseRootTitleForCleaned"));
	}

	protected DatabaseTreeRecord[] currentSourceDatabaseSelections;
	protected Vector currentSourceFilePathSelections;

	protected class OurSourceDatabaseTreeBrowser extends DatabaseTreeBrowser {
		public OurSourceDatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
			super(d,content);
		}
		
		protected boolean doSomethingWithSelections(DatabaseTreeRecord[] selections) {
			currentSourceDatabaseSelections = selections;
			return false;	// still want to call doSomethingWithSelectedFiles()
		}
		
		protected void doSomethingWithSelectedFiles(Vector paths) {
			currentSourceFilePathSelections = paths;
		}
	}
	
	protected DatabaseTreeRecord[] currentDestinationDatabaseSelections;
	protected Vector currentDestinationFilePathSelections;

	protected class OurDestinationDatabaseTreeBrowser extends DatabaseTreeBrowser {
		public OurDestinationDatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
			super(d,content);
		}
		
		protected boolean doSomethingWithSelections(DatabaseTreeRecord[] selections) {
			currentDestinationDatabaseSelections = selections;
			return false;	// still want to call doSomethingWithSelectedFiles()
		}
		
		protected void doSomethingWithSelectedFiles(Vector paths) {
			currentDestinationFilePathSelections = paths;
		}
	}
	
	// very similar to code in DicomImageViewer and DoseUtility apart from logging and progress bar ... should refactor :(
	protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord[] databaseSelections,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
		if (databaseSelections != null) {
			for (DatabaseTreeRecord databaseSelection : databaseSelections) {
				purgeFilesAndDatabaseInformation(databaseSelection,logger,progressBarUpdater,done,maximum);
			}
		}
	}
	
	protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord databaseSelection,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): "+databaseSelection);
		if (databaseSelection != null) {
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			InformationEntity ie = databaseSelection.getInformationEntity();
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): ie = "+ie);
			if (ie == null /* the root of the tree, i.e., everything */ || !ie.equals(InformationEntity.INSTANCE)) {
				// Do it one study at a time, in the order in which the patients and studies are sorted in the tree
				Enumeration children = databaseSelection.children();
				if (children != null) {
					maximum+=databaseSelection.getChildCount();
					while (children.hasMoreElements()) {
						DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
						if (child != null) {
							purgeFilesAndDatabaseInformation(child,logger,progressBarUpdater,done,maximum);
							++done;
						}
					}
				}
				// AFTER we have processed all the children, if any, we can delete ourselves, unless we are the root
				if (ie != null) {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): removeFromParent having recursed over children "+databaseSelection);
					logger.sendLn("Purging "+databaseSelection);
					databaseSelection.removeFromParent();
				}
			}
			else {
				// Instance level ... may need to delete files
				String fileName = databaseSelection.getLocalFileNameValue();
				String fileReferenceType = databaseSelection.getLocalFileReferenceTypeValue();
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): fileReferenceType = "+fileReferenceType+" for file "+fileName);
				if (fileReferenceType != null && fileReferenceType.equals(DatabaseInformationModel.FILE_COPIED)) {
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): deleting fileName "+fileName);
					try {
						logger.sendLn("Deleting file "+fileName);
						if (!new File(fileName).delete()) {
							logger.sendLn("Failed to delete local copy of file "+fileName);
						}
					}
					catch (Exception e) {
						slf4jlogger.error("Failed to delete local copy of file ",e);
						logger.sendLn("Failed to delete local copy of file "+fileName);
					}
				}
				if (earliestDatesIndexedBySourceFilePath != null) {
					earliestDatesIndexedBySourceFilePath.remove(fileName);
				}
//System.err.println("DicomCleaner.purgeFilesAndDatabaseInformation(): removeFromParent instance level "+databaseSelection);
				logger.sendLn("Purging "+databaseSelection);
				databaseSelection.removeFromParent();
			}
		}
	}

	protected class PurgeWorker implements Runnable {
		//PurgeWorker() {
		//}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Purging started");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				purgeFilesAndDatabaseInformation(currentSourceDatabaseSelections,logger,progressBarUpdater,0,1);
				purgeFilesAndDatabaseInformation(currentDestinationDatabaseSelections,logger,progressBarUpdater,0,1);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				slf4jlogger.error("Purging failed ",e);
			}
			srcDatabasePanel.removeAll();
			dstDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
				new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				slf4jlogger.error("Refresh source database browser failed ",e);
			}
			srcDatabasePanel.validate();
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Purging complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done purging"));
			cursorChanger.restoreCursor();
		}
	}

	protected class PurgeActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new PurgeWorker());
				activeThread.start();
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				slf4jlogger.error("Purging failed ",e);
			}
		}
	}
		
	protected boolean copyFromOriginalToCleanedPerformingAction(Vector paths,Date earliestDateInSet,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater) throws DicomException, IOException {
		boolean success = true;
		if (paths != null) {
			Date epochForDateModification = null;
			if (modifyDatesCheckBox.isSelected()) {
				try {
					epochForDateModification = DateTimeAttribute.getDateFromFormattedString(modifyDatesTextField.getText().trim());		// assumes 0 time and UTC if not specified
slf4jlogger.info("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): epochForDateModification {}",epochForDateModification);
				}
				catch (java.text.ParseException e) {
					slf4jlogger.error("Could not get system epoch ",e);
					epochForDateModification = new Date(0);		// use system epoch if failed; better than to not modify them at all when requested to
				}
			}
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,0,paths.size());
			for (int j=0; j< paths.size(); ++j) {
				String dicomFileName = (String)(paths.get(j));
				if (dicomFileName != null) {
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): doing file "+dicomFileName);
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaning "+dicomFileName));
					try {
						// do not log it yet ... wait till we have output file name
//long startTime = System.currentTimeMillis();
						File file = new File(dicomFileName);
						DicomInputStream i = new DicomInputStream(file);
						AttributeList list = new AttributeList();
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): reading AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
						list.setDecompressPixelData(false);
						list.read(i);
						i.close();

						list.removeGroupLengthAttributes();
						// did not decompress, so do not need to change ImagePixelModule attributes or insert lossy compression history

						String outputTransferSyntaxUID = null;
						{
							String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);		// did not decompress it
							// did not compress, so leave it alone unless Implicit VR, which we always want to convert to Explicit VR
							outputTransferSyntaxUID = transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian) ? TransferSyntax.ExplicitVRLittleEndian : transferSyntaxUID;
						}
						list.removeMetaInformationHeaderAttributes();
					
						if (removeClinicalTrialAttributesCheckBox.isSelected()) {
							ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
						}
						if (removeIdentityCheckBox.isSelected()) {
							ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,
								ClinicalTrialsAttributes.HandleUIDs.keep,
								!removeDescriptionsCheckBox.isSelected(),
								!removeSeriesDescriptionsCheckBox.isSelected(),
								!removeProtocolNameCheckBox.isSelected(),
								!removeCharacteristicsCheckBox.isSelected(),
								!removeDeviceIdentityCheckBox.isSelected(),
								!removeInstitutionIdentityCheckBox.isSelected(),
								modifyDatesCheckBox.isSelected() ? ClinicalTrialsAttributes.HandleDates.modify : ClinicalTrialsAttributes.HandleDates.keep,epochForDateModification,earliestDateInSet);
						}
						if (replacePatientNameCheckBox.isSelected()) {
							String newName = replacementPatientNameTextField.getText().trim();
							{ AttributeTag tag = TagFromName.PatientName; list.remove(tag); Attribute a = new PersonNameAttribute(tag); a.addValue(newName); list.put(tag,a); }
						}
						if (replacePatientIDCheckBox.isSelected()) {
							String newID = replacementPatientIDTextField.getText().trim();
							{ AttributeTag tag = TagFromName.PatientID; list.remove(tag); Attribute a = new LongStringAttribute(tag); a.addValue(newID); list.put(tag,a); }
						}
						if (replaceAccessionNumberCheckBox.isSelected()) {
							String newAccessionNumber = replacementAccessionNumberTextField.getText().trim();
							{ AttributeTag tag = TagFromName.AccessionNumber; list.remove(tag); Attribute a = new ShortStringAttribute(tag); a.addValue(newAccessionNumber); list.put(tag,a); }
						}
					
						if (removePrivateCheckBox.isSelected()) {
							list.removeUnsafePrivateAttributes();
							{
								Attribute a = list.get(TagFromName.DeidentificationMethod);
								if (a != null) {
									a.addValue("Unsafe private removed");
								}
							}
							{
								SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
								if (a != null) {
									a.addItem(new CodedSequenceItem("113111","DCM","Retain Safe Private Option").getAttributeList());
								}
							}
						}
						else {
							{
								Attribute a = list.get(TagFromName.DeidentificationMethod);
								if (a != null) {
									a.addValue("All private retained");
								}
							}
							{
								SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
								if (a != null) {
									a.addItem(new CodedSequenceItem("210002","99PMP","Retain all private elements").getAttributeList());
								}
							}
						}
						if (cleanUIDsCheckBox.isSelected()) {
							ClinicalTrialsAttributes.remapUIDAttributes(list);
							{
								Attribute a = list.get(TagFromName.DeidentificationMethod);
								if (a != null) {
									a.addValue("UIDs remapped");
								}
							}
							// remove the default Retain UIDs added by ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes() with the ClinicalTrialsAttributes.HandleUIDs.keep option
							{
								SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
								if (a != null) {
									Iterator<SequenceItem> it = a.iterator();
									while (it.hasNext()) {
										SequenceItem item = it.next();
										if (item != null) {
											CodedSequenceItem testcsi = new CodedSequenceItem(item.getAttributeList());
											if (testcsi != null) {
												String cv = testcsi.getCodeValue();
												String csd = testcsi.getCodingSchemeDesignator();
												if (cv != null && cv.equals("113110") && csd != null && csd.equals("DCM")) {	// "Retain UIDs Option"
													it.remove();
												}
											}
										}
									}
								}
							}
							{
								SequenceAttribute a = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
								if (a != null) {
									a.addItem(new CodedSequenceItem("210001","99PMP","Remap UIDs").getAttributeList());
								}
							}
						}
						if (addContributingEquipmentCheckBox.isSelected()) {
							ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
								true,
								new CodedSequenceItem("109104","DCM","De-identifying Equipment"),	// per CP 892
								"PixelMed",														// Manufacturer
								null,															// Institution Name
								null,															// Institutional Department Name
								null		,													// Institution Address
								ourCalledAETitle,												// Station Name
								"DicomCleaner",													// Manufacturer's Model Name
								null,															// Device Serial Number
								getBuildDate(),													// Software Version(s)
								"Cleaned");
						}
//System.err.println("Writing outputTransferSyntaxUID = "+outputTransferSyntaxUID);
						FileMetaInformation.addFileMetaInformation(list,outputTransferSyntaxUID,ourCalledAETitle);
						list.insertSuitableSpecificCharacterSetForAllStringValues();	// E.g., may have de-identified Kanji name and need new character set
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): cleaning AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
						File cleanedFile = File.createTempFile("clean",".dcm");
						cleanedFile.deleteOnExit();
						list.write(cleanedFile,outputTransferSyntaxUID,true/*useMeta*/,true/*useBufferedStream*/);
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): writing AttributeList took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
						logger.sendLn("Cleaned "+dicomFileName+" into "+cleanedFile.getCanonicalPath());
						dstDatabase.insertObject(list,cleanedFile.getCanonicalPath(),DatabaseInformationModel.FILE_COPIED);
//currentTime = System.currentTimeMillis();
//System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): inserting cleaned object in database took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
					}
					catch (Exception e) {
						System.err.println("DicomCleaner.copyFromOriginalToCleanedPerformingAction(): while cleaning "+dicomFileName);
						slf4jlogger.error("Cleaning failed for "+dicomFileName,e);
						logger.sendLn("Cleaning failed for "+dicomFileName+" because "+e.toString());
						success = false;
					}
				}
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,j+1);
			}
		}
		return success;
	}
	
	protected static Date findEarliestDate(Map<String,Date> earliestDatesIndexedBySourceFilePath,Vector<String> sourceFilePathSelections) {
		Date earliestSoFar = null;
		if (sourceFilePathSelections != null) {	// (000978)
			for (String path : sourceFilePathSelections) {
				Date candidate = earliestDatesIndexedBySourceFilePath.get(path);
				if (candidate != null && (earliestSoFar == null || candidate.before(earliestSoFar))) {
					earliestSoFar = candidate;
				}
			}
		}
		return earliestSoFar;
	}
	
	protected class CleanWorker implements Runnable {
		Vector sourceFilePathSelections;
		DatabaseInformationModel dstDatabase;
		JPanel dstDatabasePanel;
		Map<String,Date> earliestDatesIndexedBySourceFilePath;
		
		CleanWorker(Vector sourceFilePathSelections,DatabaseInformationModel dstDatabase,JPanel dstDatabasePanel,Map<String,Date> earliestDatesIndexedBySourceFilePath) {
			this.sourceFilePathSelections=sourceFilePathSelections;
			this.dstDatabase=dstDatabase;
			this.dstDatabasePanel=dstDatabasePanel;
			this.earliestDatesIndexedBySourceFilePath=earliestDatesIndexedBySourceFilePath;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Cleaning started");
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			long startTime = System.currentTimeMillis();
			Date earliestDateInSet = findEarliestDate(earliestDatesIndexedBySourceFilePath,sourceFilePathSelections);
			try {
				if (!copyFromOriginalToCleanedPerformingAction(sourceFilePathSelections,earliestDateInSet,logger,progressBarUpdater)) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaning (partially) failed: "));
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaning failed: "+e));
				slf4jlogger.error("Cleaning failed ",e);
			}
			slf4jlogger.info("CleanWorker.run(): cleaning time = {}",(System.currentTimeMillis() - startTime));
			dstDatabasePanel.removeAll();
			try {
				new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh destination database browser failed: "+e));
				slf4jlogger.error("Refresh destination database browser failed ",e);
			}
			dstDatabasePanel.validate();
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Cleaning complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done cleaning"));
			cursorChanger.restoreCursor();
		}
	}

	protected class CleanActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new CleanWorker(currentSourceFilePathSelections,dstDatabase,dstDatabasePanel,earliestDatesIndexedBySourceFilePath));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cleaning failed: "+e));
				slf4jlogger.error("Cleaning failed ",e);
			}
		}
	}
	
	protected class OurMediaImporter extends MediaImporter {
		boolean acceptAnyTransferSyntax;
		
		public OurMediaImporter(MessageLogger logger,JProgressBar progressBar,boolean acceptAnyTransferSyntax) {
			super(logger,progressBar);
			this.acceptAnyTransferSyntax = acceptAnyTransferSyntax;
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			try {
				logger.sendLn("Importing DICOM file: "+mediaFileName);
				importFileIntoDatabase(srcDatabase,mediaFileName,DatabaseInformationModel.FILE_REFERENCED,earliestDatesIndexedBySourceFilePath);
			}
			catch (Exception e) {
				slf4jlogger.error("Importing DICOM file {} failed",mediaFileName,e);
			}
		}
		
		protected boolean canUseBzip = CapabilitiesAvailable.haveBzip2Support();

		// override base class isOKToImport(), which rejects unsupported compressed transfer syntaxes
		
		protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
//System.err.println(new TransferSyntax(transferSyntaxUID).dump());
//System.err.println(sopClassUID+" isImageStorage = "+SOPClass.isImageStorage(sopClassUID));
			return sopClassUID != null
				&& (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
				&& transferSyntaxUID != null
				&& ((acceptAnyTransferSyntax && new TransferSyntax(transferSyntaxUID).isRecognized())
				 || transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)
				 || transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)
				 || (transferSyntaxUID.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian) && canUseBzip)
				 || transferSyntaxUID.equals(TransferSyntax.RLE)
				 || transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)
				 || CapabilitiesAvailable.haveJPEGLosslessCodec() && (transferSyntaxUID.equals(TransferSyntax.JPEGLossless) || transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1))
				 || CapabilitiesAvailable.haveJPEG2000Part1Codec() && (transferSyntaxUID.equals(TransferSyntax.JPEG2000) || transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless))
				 || CapabilitiesAvailable.haveJPEGLSCodec() && (transferSyntaxUID.equals(TransferSyntax.JPEGLS) || transferSyntaxUID.equals(TransferSyntax.JPEGNLS))
				);
		}
	}

	protected String importDirectoryPath;	// keep around between invocations

	protected class ImportWorker implements Runnable {
		MediaImporter importer;
		DatabaseInformationModel srcDatabase;
		JPanel srcDatabasePanel;
		String pathName;
		
		ImportWorker(String pathName,DatabaseInformationModel srcDatabase,JPanel srcDatabasePanel) {
			importer = new OurMediaImporter(logger,progressBarUpdater.getProgressBar(),acceptAnyTransferSyntaxCheckBox.isSelected());
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
			this.pathName=pathName;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Import starting");
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				importer.importDicomFiles(pathName);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				slf4jlogger.error("Importing failed {}",pathName,e);
			}
			srcDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				slf4jlogger.error("Refresh source database browser failed ",e);
			}
			srcDatabasePanel.validate();
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done importing"));
			// importer sends its own completion message to log, so do not need another one
			cursorChanger.restoreCursor();
		}
	}

	protected class ImportActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (logger instanceof DialogMessageLogger) {
					((DialogMessageLogger)logger).setVisible(true);
				}
				if (importDirectoryPath == null || importDirectoryPath.length() == 0) {
						importDirectoryPath = "/";
				}
				// need to do the file choosing on the main event thread, since Swing is not thread safe, so do it here, instead of delegating to MediaImporter in ImportWorker
				SafeFileChooser chooser = new SafeFileChooser(importDirectoryPath);
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
//System.err.println("DicomCleaner.ImportActionListener.actionPerformed(): about to chooser.showOpenDialog");
				if (chooser.showOpenDialog(DicomCleaner.this.getContentPane()) == JFileChooser.APPROVE_OPTION) {
//System.err.println("DicomCleaner.ImportActionListener.actionPerformed(): back with APPROVE_OPTION");
					importDirectoryPath=chooser.getCurrentDirectory().getAbsolutePath();	// keep around for next time
					String pathName = chooser.getSelectedFile().getAbsolutePath();
					new Thread(new ImportWorker(pathName,srcDatabase,srcDatabasePanel)).start();
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				slf4jlogger.error("Importing failed ",e);
			}
		}
	}

	protected String exportDirectoryPath;	// keep around between invocations
	
	protected String makeNewFullyQualifiedInterchangeMediaInstancePathName(int fileCount) throws IOException {
		return new File(
			rootNameForDicomInstanceFilesOnInterchangeMedia,
			filePrefixForDicomInstanceFilesOnInterchangeMedia + Integer.toString(fileCount) + fileSuffixForDicomInstanceFilesOnInterchangeMedia)
			.getPath();
	}

	protected String makeNewFullyQualifiedHierarchicalInstancePathName(String sourceFileName) throws DicomException, IOException {
		AttributeList list = new AttributeList();
		list.read(sourceFileName,TagFromName.PixelData);
		String hierarchicalFileName = MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list);
		return new File(rootNameForDicomInstanceFilesOnInterchangeMedia,hierarchicalFileName).getPath();
	}

	protected class ExportWorker implements Runnable {
		Vector destinationFilePathSelections;
		File exportDirectory;
		
		ExportWorker(Vector destinationFilePathSelections,File exportDirectory) {
			this.destinationFilePathSelections=destinationFilePathSelections;
			this.exportDirectory=exportDirectory;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Export started");
			try {
				int nFiles = destinationFilePathSelections.size();
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,0,nFiles+1);		// include DICOMDIR
				String exportFileNames[] = new String[nFiles];
				for (int j=0; j<nFiles; ++j) {
					String databaseFileName = (String)(destinationFilePathSelections.get(j));
					String exportRelativePathName = hierarchicalExportCheckBox.isSelected() ? makeNewFullyQualifiedHierarchicalInstancePathName(databaseFileName) : makeNewFullyQualifiedInterchangeMediaInstancePathName(j);
					File exportFile = new File(exportDirectory,exportRelativePathName);
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Exporting "+exportRelativePathName));
					logger.sendLn("Exporting "+databaseFileName+" to "+exportFile.getCanonicalPath());
//System.err.println("DicomCleaner.ExportWorker.run(): copying "+databaseFileName+" to "+exportFile);
					exportFile.getParentFile().mkdirs();
					CopyStream.copy(new File(databaseFileName),exportFile);
					exportFileNames[j] = exportRelativePathName;
					SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,j+1);
				}
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Exporting DICOMDIR"));
				logger.sendLn("Exporting DICOMDIR");
//System.err.println("DicomCleaner.ExportWorker.run():  building DICOMDIR");
				DicomDirectory dicomDirectory = new DicomDirectory(exportDirectory,exportFileNames);
//System.err.println("DicomCleaner.ExportWorker.run():  writing DICOMDIR");
				dicomDirectory.write(new File(exportDirectory,nameForDicomDirectoryOnInterchangeMedia).getCanonicalPath());
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,nFiles+1);		// include DICOMDIR

				if (zipExportCheckBox.isSelected()) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Zipping exported files"));
					logger.sendLn("Zipping exported files");
					File zipFile = new File(exportDirectory,exportedZipFileName);
					zipFile.delete();
					FileOutputStream fout = new FileOutputStream(zipFile);
					ZipOutputStream zout = new ZipOutputStream(fout);
					zout.setMethod(ZipOutputStream.DEFLATED);
					zout.setLevel(9);

					SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,0,nFiles+1);		// include DICOMDIR
					for (int j=0; j<nFiles; ++j) {
						String exportRelativePathName = exportFileNames[j];
						File inFile = new File(exportDirectory,exportRelativePathName);
						ZipEntry zipEntry = new ZipEntry(exportRelativePathName);
						//zipEntry.setMethod(ZipOutputStream.DEFLATED);
						zout.putNextEntry(zipEntry);
						FileInputStream in = new FileInputStream(inFile);
						CopyStream.copy(in,zout);
						zout.closeEntry();
						in.close();
						inFile.delete();
						SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,j+1);
					}

					{
						File inFile = new File(exportDirectory,nameForDicomDirectoryOnInterchangeMedia);
						ZipEntry zipEntry = new ZipEntry(nameForDicomDirectoryOnInterchangeMedia);
						zipEntry.setMethod(ZipOutputStream.DEFLATED);
						zout.putNextEntry(zipEntry);
						FileInputStream in = new FileInputStream(inFile);
						CopyStream.copy(in,zout);
						zout.closeEntry();
						in.close();
						inFile.delete();
						SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,nFiles+1);		// include DICOMDIR
					}
					zout.close();
					fout.close();
					new File(exportDirectory,rootNameForDicomInstanceFilesOnInterchangeMedia).delete();
				}

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Export failed: "+e));
				slf4jlogger.error("Export failed ",e);
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Export complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done exporting to "+exportDirectory));
			cursorChanger.restoreCursor();
		}
	}

	protected class ExportActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				SafeFileChooser chooser = new SafeFileChooser(exportDirectoryPath);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (chooser.showOpenDialog(DicomCleaner.this.getContentPane()) == JFileChooser.APPROVE_OPTION) {
					try {
						//exportDirectoryPath=chooser.getCurrentDirectory().getCanonicalPath();
						exportDirectoryPath = chooser.getSelectedFile().getCanonicalPath();
						File exportDirectory = new File(exportDirectoryPath);
//System.err.println("DicomCleaner.ExportActionListener.actionPerformed(): selected root directory = "+exportDirectory);
//System.err.println("DicomCleaner.ExportActionListener.actionPerformed(): copying files");
						new Thread(new ExportWorker(currentDestinationFilePathSelections,exportDirectory)).start();
					}
					catch (Exception e) {
						ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Export failed: "+e));
						slf4jlogger.error("Export failed ",e);
					}
				}
				// else user cancelled operation in JOptionPane.showInputDialog() so gracefully do nothing
			}
		}
	}
	
	protected class OurMultipleInstanceTransferStatusHandler extends MultipleInstanceTransferStatusHandlerWithFileName {
		int nFiles;
		MessageLogger logger;
		SafeProgressBarUpdaterThread progressBarUpdater;
		
		OurMultipleInstanceTransferStatusHandler(int nFiles,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater) {
			this.nFiles=nFiles;
			this.logger=logger;
			this.progressBarUpdater=progressBarUpdater;
		}
		
		public void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID,String fileName,boolean success) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Remaining "+nRemaining+", completed "+nCompleted+", failed "+nFailed+", warning "+nWarning));
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,nFiles - nRemaining);
			if (logger != null) {
				logger.sendLn((success ? "Sent " : "Failed to send ")+fileName);
			}
		}
	}

	protected class SendWorker implements Runnable {
		String hostname;
		int port;
		String calledAETitle;
		String callingAETitle;
		SetOfDicomFiles setOfDicomFiles;
		int ourMaximumLengthReceived;
		int socketReceiveBufferSize;
		int socketSendBufferSize;
		
		SendWorker(String hostname,int port,String calledAETitle,String callingAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				SetOfDicomFiles setOfDicomFiles) {
			this.hostname=hostname;
			this.port=port;
			this.calledAETitle=calledAETitle;
			this.callingAETitle=callingAETitle;
			this.ourMaximumLengthReceived=ourMaximumLengthReceived;
			this.socketReceiveBufferSize=socketReceiveBufferSize;
			this.socketSendBufferSize=socketSendBufferSize;
			this.setOfDicomFiles=setOfDicomFiles;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Send starting");
			try {
				int nFiles = setOfDicomFiles.size();
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,0,nFiles);
				new StorageSOPClassSCU(hostname,port,calledAETitle,callingAETitle,
					ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
					setOfDicomFiles,0/*compressionLevel*/,
					new OurMultipleInstanceTransferStatusHandler(nFiles,logger,progressBarUpdater));
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Send failed: "+e));
				logger.sendLn("Send failed");
				slf4jlogger.error("Send failed ",e);
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Send complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending to "+calledAETitle));
			cursorChanger.restoreCursor();
		}
	}

	protected class SendActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				Properties properties = getProperties();
				String ae = properties.getProperty(propertyName_DicomCurrentlySelectedStorageTargetAE);
				ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(resourceBundle.getString("sendSelectMessage"),resourceBundle.getString("sendSelectDialogTitle")+" ...",ae);
				if (ae != null && networkApplicationProperties != null) {
					try {
						String                   callingAETitle = networkApplicationProperties.getCallingAETitle();
						String                    calledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(ae);
						PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(calledAETitle);
						String                         hostname = presentationAddress.getHostname();
						int                                port = presentationAddress.getPort();
						int            ourMaximumLengthReceived = networkApplicationProperties.getInitiatorMaximumLengthReceived();
						int             socketReceiveBufferSize = networkApplicationProperties.getInitiatorSocketReceiveBufferSize();
						int                socketSendBufferSize = networkApplicationProperties.getInitiatorSocketSendBufferSize();
						
						SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(currentDestinationFilePathSelections);
						new Thread(new SendWorker(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,setOfDicomFiles)).start();
					}
					catch (Exception e) {
						slf4jlogger.error("",e);
					}
				}
				// else user cancelled operation in JOptionPane.showInputDialog() so gracefully do nothing
			}
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending."));
		}
	}
	
	protected class OurDicomImageBlackout extends DicomImageBlackout {
	
		OurDicomImageBlackout(String dicomFileNames[],int burnedinflag,String ourAETitle) {
			super(dicomFileNames,(DicomImageBlackout.StatusNotificationHandler)null,burnedinflag);
			statusNotificationHandler = new ApplicationStatusChangeEventNotificationHandler();
			this.ourAETitle=ourAETitle;
		}

		public class ApplicationStatusChangeEventNotificationHandler extends StatusNotificationHandler {
			public void notify(int status,String message,Throwable t) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Blackout "+message));
				logger.sendLn("Blackout "+message);
				System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): status = "+status);
				System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): message = "+message);
				if (t != null) {
					t.printStackTrace(System.err);
				}
			}
		}
	}
	
	protected class BlackoutActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			cursorChanger.setWaitCursor();
			logger.sendLn("Blackout starting");
			if (currentDestinationFilePathSelections != null && currentDestinationFilePathSelections.size() > 0) {
				{
					try {
						int nFiles = currentDestinationFilePathSelections.size();
						String fileNames[] = new String[nFiles];
						for (int j=0; j< nFiles; ++j) {
							fileNames[j] = (String)(currentDestinationFilePathSelections.get(j));
						}
						new OurDicomImageBlackout(fileNames,DicomImageBlackout.BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED,ourCalledAETitle);
					}
					catch (Exception e) {
						slf4jlogger.error("Dicom Image Blackout failed ",e);
					}
				}
			}
			// don't need to send StatusChangeEvent("Blackout complete.") ... DicomImageBlackout already does something similar
			// DicomImageBlackout sends its own completion message to log, so do not need another one
			cursorChanger.restoreCursor();
		}
	}
	
	protected void setCurrentRemoteQuerySelection(AttributeList uniqueKeys,Attribute uniqueKey,AttributeList identifier) {
		currentRemoteQuerySelectionUniqueKeys=uniqueKeys;
		currentRemoteQuerySelectionUniqueKey=uniqueKey;
		currentRemoteQuerySelectionRetrieveAE=null;
		if (identifier != null) {
			Attribute aRetrieveAETitle=identifier.get(TagFromName.RetrieveAETitle);
			if (aRetrieveAETitle != null) currentRemoteQuerySelectionRetrieveAE=aRetrieveAETitle.getSingleStringValueOrNull();
		}
		if (currentRemoteQuerySelectionRetrieveAE == null) {
			// it is legal for RetrieveAETitle to be zero length at all but the lowest levels of
			// the query model :( (See PS 3.4 C.4.1.1.3.2)
			// (so far the Leonardo is the only one that doesn't send it at all levels)
			// we could recurse down to the lower levels and get the union of the value there
			// but lets just keep it simple and ...
			// default to whoever it was we queried in the first place ...
			if (currentRemoteQueryInformationModel != null) {
				currentRemoteQuerySelectionRetrieveAE=currentRemoteQueryInformationModel.getCalledAETitle();
			}
		}
		currentRemoteQuerySelectionLevel = null;
		if (identifier != null) {
			Attribute a = identifier.get(TagFromName.QueryRetrieveLevel);
			if (a != null) {
				currentRemoteQuerySelectionLevel = a.getSingleStringValueOrNull();
			}
		}
		if (currentRemoteQuerySelectionLevel == null) {
			// QueryRetrieveLevel must have been (erroneously) missing in query response ... see with Dave Harvey's code on public server
			// so try to guess it from unique key in tree record
			// Fixes [bugs.mrmf] (000224) Missing query/retrieve level in C-FIND response causes tree select and retrieve to fail
			if (uniqueKey != null) {
				AttributeTag tag = uniqueKey.getTag();
				if (tag != null) {
					if (tag.equals(TagFromName.PatientID)) {
						currentRemoteQuerySelectionLevel="PATIENT";
					}
					else if (tag.equals(TagFromName.StudyInstanceUID)) {
						currentRemoteQuerySelectionLevel="STUDY";
					}
					else if (tag.equals(TagFromName.SeriesInstanceUID)) {
						currentRemoteQuerySelectionLevel="SERIES";
					}
					else if (tag.equals(TagFromName.SOPInstanceUID)) {
						currentRemoteQuerySelectionLevel="IMAGE";
					}
				}
			}
slf4jlogger.info("DicomCleaner.setCurrentRemoteQuerySelection(): Guessed missing currentRemoteQuerySelectionLevel to be {}",currentRemoteQuerySelectionLevel);
		}
	}

	protected class OurQueryTreeBrowser extends QueryTreeBrowser {
		/**
		 * @param	q
		 * @param	m
		 * @param	content
		 * @throws	DicomException
		 */
		OurQueryTreeBrowser(QueryInformationModel q,QueryTreeModel m,Container content) throws DicomException {
			super(q,m,content);
		}
		/***/
		protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedLevel() {
			return new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					TreePath tp = tse.getNewLeadSelectionPath();
					if (tp != null) {
						Object lastPathComponent = tp.getLastPathComponent();
						if (lastPathComponent instanceof QueryTreeRecord) {
							QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
							setCurrentRemoteQuerySelection(r.getUniqueKeys(),r.getUniqueKey(),r.getAllAttributesReturnedInIdentifier());
							currentRemoteQuerySelectionQueryTreeRecord=r;
						}
					}
				}
			};
		}
	}

	protected class QueryWorker implements Runnable {
		AttributeList filter;
		
		QueryWorker(AttributeList filter) {
			this.filter=filter;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			String calledAET = currentRemoteQueryInformationModel.getCalledAETitle();
			String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(calledAET);
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Performing query on "+localName));
			logger.sendLn("Query to "+localName+" ("+calledAET+") starting");
			try {
				QueryTreeModel treeModel = currentRemoteQueryInformationModel.performHierarchicalQuery(filter);
				new OurQueryTreeBrowser(currentRemoteQueryInformationModel,treeModel,remoteQueryRetrievePanel);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying "+localName));
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Query to "+localName+" failed "+e));
				logger.sendLn("Query to "+localName+" ("+calledAET+") failed due to"+ e);
				slf4jlogger.error("",e);
			}
			logger.sendLn("Query to "+localName+" ("+calledAET+") complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying  "+localName));
			cursorChanger.restoreCursor();
		}
	}

	protected class QueryActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			//new QueryRetrieveDialog("DicomCleaner Query",400,512);
			Properties properties = getProperties();
			String ae = properties.getProperty(propertyName_DicomCurrentlySelectedQueryTargetAE);
			ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(resourceBundle.getString("querySelectMessage"),resourceBundle.getString("querySelectDialogTitle")+" ...",ae);
			remoteQueryRetrievePanel.removeAll();
			if (ae != null) {
				setCurrentRemoteQueryInformationModel(ae);
				if (currentRemoteQueryInformationModel == null) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Cannot query "+ae));
				}
				else {
					try {
						SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet((String[])null);
						AttributeList filter = new AttributeList();
						{
							AttributeTag t = TagFromName.PatientName; Attribute a = new PersonNameAttribute(t,specificCharacterSet);
							String patientName = queryFilterPatientNameTextField.getText().trim();
							if (patientName != null && patientName.length() > 0) {
								a.addValue(patientName);
							}
							filter.put(t,a);
						}
						{
							AttributeTag t = TagFromName.PatientID; Attribute a = new LongStringAttribute(t,specificCharacterSet);
							String patientID = queryFilterPatientIDTextField.getText().trim();
							if (patientID != null && patientID.length() > 0) {
								a.addValue(patientID);
							}
							filter.put(t,a);
						}
						{
							AttributeTag t = TagFromName.AccessionNumber; Attribute a = new ShortStringAttribute(t,specificCharacterSet);
							String accessionNumber = queryFilterAccessionNumberTextField.getText().trim();
							if (accessionNumber != null && accessionNumber.length() > 0) {
								a.addValue(accessionNumber);
							}
							filter.put(t,a);
						}
						{ AttributeTag t = TagFromName.PatientBirthDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.PatientSex; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.StudyID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.StudyDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{
							AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t);
							String studyDate = queryFilterStudyDateTextField.getText().trim();
							if (studyDate != null && studyDate.length() > 0) {
								a.addValue(studyDate);
							}
							filter.put(t,a);
						}
						{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.PatientAge; Attribute a = new AgeStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.SeriesDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.InstanceNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.NumberOfFrames; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SOPClassUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SpecificCharacterSet; Attribute a = new CodeStringAttribute(t); filter.put(t,a); a.addValue("ISO_IR 100"); }

						activeThread = new Thread(new QueryWorker(filter));
						activeThread.start();
					}
					catch (Exception e) {
						slf4jlogger.error("Query to {} failed",ae,e);
						ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Query to "+ae+" failed"));
					}
				}
			}
			remoteQueryRetrievePanel.validate();
		}
	}

	protected void performRetrieve(AttributeList uniqueKeys,String selectionLevel,String retrieveAE) {
		try {
			AttributeList identifier = new AttributeList();
			if (uniqueKeys != null) {
				identifier.putAll(uniqueKeys);
				{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(selectionLevel); identifier.put(t,a); }
				currentRemoteQueryInformationModel.performHierarchicalMoveFrom(identifier,retrieveAE);
			}
			// else do nothing, since no unique key to specify what to retrieve
		} catch (Exception e) {
			slf4jlogger.error("Retrieve failed ",e);
		}
	}
	
	protected class RetrieveWorker implements Runnable {
		RetrieveWorker() {
		}
		
		public void run() {
			cursorChanger.setWaitCursor();
			String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(currentRemoteQuerySelectionRetrieveAE);
			if (currentRemoteQuerySelectionLevel == null) {	// they have selected the root of the tree
				QueryTreeRecord parent = currentRemoteQuerySelectionQueryTreeRecord;
				if (parent != null) {
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving everything from "+localName));
					logger.sendLn("Retrieving everything from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
					Enumeration children = parent.children();
					if (children != null) {
						int nChildren = parent.getChildCount();
//System.err.println("DicomCleaner.RetrieveWorker.run(): Everything nChildren = "+nChildren);
						SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,nChildren);
						int doneCount = 0;
						while (children.hasMoreElements()) {
							QueryTreeRecord r = (QueryTreeRecord)(children.nextElement());
							if (r != null) {
								setCurrentRemoteQuerySelection(r.getUniqueKeys(),r.getUniqueKey(),r.getAllAttributesReturnedInIdentifier());
								ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName));
								logger.sendLn("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
								performRetrieve(currentRemoteQuerySelectionUniqueKeys,currentRemoteQuerySelectionLevel,currentRemoteQuerySelectionRetrieveAE);
								SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,++doneCount);
//System.err.println("DicomCleaner.RetrieveWorker.run(): doneCount = "+doneCount);
							}
						}
						SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
					}
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending retrieval request"));
					setCurrentRemoteQuerySelection(null,null,null);
				}
			}
			else {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName));
				logger.sendLn("Request retrieval of "+currentRemoteQuerySelectionLevel+" "+currentRemoteQuerySelectionUniqueKey.getSingleStringValueOrEmptyString()+" from "+localName+" ("+currentRemoteQuerySelectionRetrieveAE+")");
				SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,1);
				performRetrieve(currentRemoteQuerySelectionUniqueKeys,currentRemoteQuerySelectionLevel,currentRemoteQuerySelectionRetrieveAE);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending retrieval request"));
				SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			}
			cursorChanger.restoreCursor();
		}
	}
	
	protected class RetrieveActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			activeThread = new Thread(new RetrieveWorker());
			activeThread.start();
		}
	}

	protected class LogActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
		}
	}

	protected class ConfigureActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (storageSOPClassSCPDispatcher != null) {
					storageSOPClassSCPDispatcher.shutdown();
				}
				new NetworkApplicationConfigurationDialog(DicomCleaner.this.getContentPane(),networkApplicationInformation,networkApplicationProperties);
				// should now save properties to file
				networkApplicationProperties.getProperties(getProperties());
				updatePropertiesWithUIState();
				storeProperties("Edited and saved from user interface");
				//getProperties().store(System.err,"Bla");
				activateStorageSCP();
			} catch (Exception e) {
				slf4jlogger.error("Configure failed ",e);
			}
		}
	}
	
	Thread activeThread;
	
	protected class CancelActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (activeThread != null) {
					activeThread.interrupt();
				}
			} catch (Exception e) {
				slf4jlogger.error("Cancel failed ",e);
			}
		}
	}
	
	protected class EarliestYearActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				if (earliestDatesIndexedBySourceFilePath != null && currentSourceFilePathSelections != null) {
					Date earliestDateInSet = findEarliestDate(earliestDatesIndexedBySourceFilePath,currentSourceFilePathSelections);
//System.err.println("DicomCleaner.EarliestYearActionListener.actionPerformed(): earliestDateInSet = "+earliestDateInSet);
					String newYear = DateTimeAttribute.getFormattedString(earliestDateInSet,TimeZone.getTimeZone("GMT")).substring(0,4);
					modifyDatesTextField.setText(newYear+"0101");
				}
			} catch (Exception e) {
				slf4jlogger.error("Earliest year failed ",e);
			}
		}
	}
	
	protected class RandomYearActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			int newYear = (int)(Math.random()*100 + 1970);
			modifyDatesTextField.setText(Integer.toString(newYear)+"0101");
		}
	}
	
	protected class DefaultYearActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			modifyDatesTextField.setText(resourceBundle.getString("defaultModifyDatesEpoch"));
		}
	}
	
	protected class RandomReplacementActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			long newID = (long)(Math.random()*Math.pow(10,randomReplacementPatientIDLength));
			String newIDString = StringUtilities.zeroPadPositiveInteger(Long.toString(newID),randomReplacementPatientIDLength);
			replacementPatientIDTextField.setText(newIDString);
			replacementPatientNameTextField.setText(randomReplacementPatientNamePrefix+newIDString);

			long newAccessionNumber = (long)(Math.random()*Math.pow(10,randomReplacementAccessionNumberLength));
			replacementAccessionNumberTextField.setText(StringUtilities.zeroPadPositiveInteger(Long.toString(newAccessionNumber),randomReplacementAccessionNumberLength));
		}
	}
	
	protected class DefaultReplacementActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			replacementPatientIDTextField.setText(resourceBundle.getString("defaultReplacementPatientID"));
			replacementPatientNameTextField.setText(resourceBundle.getString("defaultReplacementPatientName"));
			replacementAccessionNumberTextField.setText(resourceBundle.getString("defaultReplacementAccessionNumber"));
		}
	}
	
	private void updatePropertiesWithUIState() {
		Properties properties = getProperties();

		properties.setProperty(propertyName_CheckBoxReplacePatientNameIsSelected,Boolean.toString(replacePatientNameCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxReplacePatientIDIsSelected,Boolean.toString(replacePatientIDCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxReplaceAccessionNumberIsSelected,Boolean.toString(replaceAccessionNumberCheckBox.isSelected()));

		properties.setProperty(propertyName_ReplacementTextPatientName,replacementPatientNameTextField.getText().trim());
		properties.setProperty(propertyName_ReplacementTextPatientID,replacementPatientIDTextField.getText().trim());
		properties.setProperty(propertyName_ReplacementTextAccessionNumber,replacementAccessionNumberTextField.getText().trim());
		
		properties.setProperty(propertyName_CheckBoxModifyDatesIsSelected,Boolean.toString(modifyDatesCheckBox.isSelected()));
		
		properties.setProperty(propertyName_ModifyDatesEpoch,modifyDatesTextField.getText().trim());
		
		properties.setProperty(propertyName_CheckBoxRemoveIdentityIsSelected,Boolean.toString(removeIdentityCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveDescriptionsIsSelected,Boolean.toString(removeDescriptionsCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveSeriesDescriptionsIsSelected,Boolean.toString(removeSeriesDescriptionsCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveProtocolNameIsSelected,Boolean.toString(removeProtocolNameCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveCharacteristicsIsSelected,Boolean.toString(removeCharacteristicsCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveDeviceIdentityIsSelected,Boolean.toString(removeDeviceIdentityCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveInstitutionIdentityIsSelected,Boolean.toString(removeInstitutionIdentityCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxCleanUIDsIsSelected,Boolean.toString(cleanUIDsCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemovePrivateIsSelected,Boolean.toString(removePrivateCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxAddContributingEquipmentIsSelected,Boolean.toString(addContributingEquipmentCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxRemoveClinicalTrialAttributesIsSelected,Boolean.toString(removeClinicalTrialAttributesCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxZipExportIsSelected,Boolean.toString(zipExportCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxHierarchicalExportIsSelected,Boolean.toString(hierarchicalExportCheckBox.isSelected()));
		properties.setProperty(propertyName_CheckBoxAcceptAnyTransferSyntaxIsSelected,Boolean.toString(acceptAnyTransferSyntaxCheckBox.isSelected()));
	}
	
	public DicomCleaner() throws DicomException, IOException {
		this(null/*pathName*/);
	}
	
	public DicomCleaner(String pathName) throws DicomException, IOException {
		super(null,propertiesFileName);
System.err.println("default Locale="+Locale.getDefault());
		
		resourceBundle = ResourceBundle.getBundle(resourceBundleName);
		setTitle(resourceBundle.getString("applicationTitle"));

		Properties properties = getProperties();
System.err.println("properties="+properties);

		activateTemporaryDatabases();
		savedImagesFolder = new File(System.getProperty("java.io.tmpdir"));
		
		try {
			networkApplicationProperties = new NetworkApplicationProperties(properties,true/*addPublicStorageSCPsIfNoRemoteAEsConfigured*/);
		}
		catch (Exception e) {
			slf4jlogger.error("Fetching network application properties failed ",e);
			networkApplicationProperties = null;
		}
		{
			NetworkApplicationInformationFederated federatedNetworkApplicationInformation = new NetworkApplicationInformationFederated();
			federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
			networkApplicationInformation = federatedNetworkApplicationInformation;
//System.err.println("networkApplicationInformation ...\n"+networkApplicationInformation);
		}
		activateStorageSCP();

		logger = new DialogMessageLogger("DicomCleaner Log",512,384,false/*exitApplicationOnClose*/,false/*visible*/,
			getBooleanPropertyOrDefaultAndAddIt(propertyName_ShowDateTime,false),
			getPropertyOrDefaultAndAddIt(propertyName_DateTimeFormat,""));
		
		cursorChanger = new SafeCursorChanger(this);

		// ShutdownHook will run regardless of whether Command-Q (on Mac) or window closed ...
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
//System.err.println("DicomCleaner.ShutdownHook.run()");
				try {
					updatePropertiesWithUIState();
					storeProperties("Edited and saved from user interface");
				}
				catch (Exception e) {
					slf4jlogger.error("Storing properties during shutdown failed ",e);
				}
				if (networkApplicationInformation != null && networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
					((NetworkApplicationInformationFederated)networkApplicationInformation).removeAllSources();
				}
//System.err.print(TransferMonitor.report());
			}
		});
		
		boolean allowUserQuery                             = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowUserQuery,true);

		boolean allowNetworkConfiguration				   = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowNetworkConfiguration,true);

		boolean allowChangeDatesAndTimes                   = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowChangeDatesAndTimes,true);

		boolean allowRemoveIdentityCheckBox                = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveIdentityCheckBox,true);
		boolean allowRemoveDescriptionsCheckBox            = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveDescriptionsCheckBox,true);
		boolean allowRemoveSeriesDescriptionsCheckBox      = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveSeriesDescriptionsCheckBox,true);
		boolean allowRemoveProtocolNameCheckBox            = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveProtocolNameCheckBox,true);
		boolean allowRemoveCharacteristicsCheckBox         = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveCharacteristicsCheckBox,true);
		boolean allowRemoveDeviceIdentityCheckBox          = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveDeviceIdentityCheckBox,true);
		boolean allowRemoveInstitutionIdentityCheckBox     = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveInstitutionIdentityCheckBox,true);
		boolean allowCleanUIDsCheckBox                     = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowCleanUIDsCheckBox,true);
		boolean allowRemovePrivateCheckBox                 = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemovePrivateCheckBox,true);
		boolean allowAddContributingEquipmentCheckBox      = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowAddContributingEquipmentCheckBox,true);
		boolean allowRemoveClinicalTrialAttributesCheckBox = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowRemoveClinicalTrialAttributesCheckBox,true);
		boolean allowZipExportCheckBox                     = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowZipExportCheckBox,true);
		boolean allowHierarchicalExportCheckBox            = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowHierarchicalExportCheckBox,true);
		boolean allowAcceptAnyTransferSyntaxCheckBox       = getBooleanPropertyOrDefaultAndAddIt(propertyName_AllowAcceptAnyTransferSyntaxCheckBox,true);
		
		randomReplacementPatientNamePrefix = getPropertyOrDefaultAndAddIt(propertyName_RandomReplacementPatientNamePrefix,resourceBundle.getString("defaultRandomReplacementPatientNamePrefix"));
		randomReplacementPatientIDLength = getIntegerPropertyOrDefaultAndAddIt(propertyName_RandomReplacementPatientIDLength,default_RandomReplacementPatientIDLength);
		randomReplacementAccessionNumberLength = getIntegerPropertyOrDefaultAndAddIt(propertyName_RandomReplacementAccessionNumberLength,default_RandomReplacementAccessionNumberLength);

		srcDatabasePanel = new JPanel();
		dstDatabasePanel = new JPanel();
		remoteQueryRetrievePanel = allowUserQuery ? new JPanel() : null;

		srcDatabasePanel.setLayout(new GridLayout(1,1));
		dstDatabasePanel.setLayout(new GridLayout(1,1));
		if (allowUserQuery) remoteQueryRetrievePanel.setLayout(new GridLayout(1,1));
		
		DatabaseTreeBrowser srcDatabaseTreeBrowser = new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
		DatabaseTreeBrowser dstDatabaseTreeBrowser = new OurDestinationDatabaseTreeBrowser(dstDatabase,dstDatabasePanel);

		Border panelBorder = BorderFactory.createEtchedBorder();
		
		JSplitPane panesToUseForMainPanel = null;
		{
			JSplitPane pairOfLocalDatabaseBrowserPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,srcDatabasePanel,dstDatabasePanel);
			pairOfLocalDatabaseBrowserPanes.setOneTouchExpandable(true);
			pairOfLocalDatabaseBrowserPanes.setResizeWeight(0.5);
		
			JSplitPane remoteAndLocalBrowserPanes = null;
			if (allowUserQuery) {
				remoteAndLocalBrowserPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,remoteQueryRetrievePanel,pairOfLocalDatabaseBrowserPanes);
				remoteAndLocalBrowserPanes.setOneTouchExpandable(true);
				remoteAndLocalBrowserPanes.setResizeWeight(0.4);		// you would think 0.33 would be equal, but it isn't
			}
		
			panesToUseForMainPanel = remoteAndLocalBrowserPanes == null ? pairOfLocalDatabaseBrowserPanes : remoteAndLocalBrowserPanes;
		}
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBorder(panelBorder);
		
		if (allowNetworkConfiguration) {
			JButton configureButton = new JButton(resourceBundle.getString("configureButtonLabelText"));
			configureButton.setToolTipText(resourceBundle.getString("configureButtonToolTipText"));
			buttonPanel.add(configureButton);
			configureButton.addActionListener(new ConfigureActionListener());
		}
		
		{
			JButton logButton = new JButton(resourceBundle.getString("logButtonLabelText"));
			logButton.setToolTipText(resourceBundle.getString("logButtonToolTipText"));
			buttonPanel.add(logButton);
			logButton.addActionListener(new LogActionListener());
		}
		
		if (allowUserQuery) {
			JButton queryButton = new JButton(resourceBundle.getString("queryButtonLabelText"));
			queryButton.setToolTipText(resourceBundle.getString("queryButtonToolTipText"));
			buttonPanel.add(queryButton);
			queryButton.addActionListener(new QueryActionListener());
		}
		
		if (allowUserQuery) {
			JButton retrieveButton = new JButton(resourceBundle.getString("retrieveButtonLabelText"));
			retrieveButton.setToolTipText(resourceBundle.getString("retrieveButtonToolTipText"));
			buttonPanel.add(retrieveButton);
			retrieveButton.addActionListener(new RetrieveActionListener());
		}
		
		{
			JButton importButton = new JButton(resourceBundle.getString("importButtonLabelText"));
			importButton.setToolTipText(resourceBundle.getString("importButtonToolTipText"));
			buttonPanel.add(importButton);
			importButton.addActionListener(new ImportActionListener());
		}
		
		{
			JButton cleanButton = new JButton(resourceBundle.getString("cleanButtonLabelText"));
			cleanButton.setToolTipText(resourceBundle.getString("cleanButtonToolTipText"));
			buttonPanel.add(cleanButton);
			cleanButton.addActionListener(new CleanActionListener());
		}
		
		{
			JButton blackoutButton = new JButton(resourceBundle.getString("blackoutButtonLabelText"));
			blackoutButton.setToolTipText(resourceBundle.getString("blackoutButtonToolTipText"));
			buttonPanel.add(blackoutButton);
			blackoutButton.addActionListener(new BlackoutActionListener());
		}
		
		{
			JButton exportButton = new JButton(resourceBundle.getString("exportButtonLabelText"));
			exportButton.setToolTipText(resourceBundle.getString("exportButtonToolTipText"));
			buttonPanel.add(exportButton);
			exportButton.addActionListener(new ExportActionListener());
		}
		
		{
			JButton sendButton = new JButton(resourceBundle.getString("sendButtonLabelText"));
			sendButton.setToolTipText(resourceBundle.getString("sendButtonToolTipText"));
			buttonPanel.add(sendButton);
			sendButton.addActionListener(new SendActionListener());
		}
		
		{
			JButton purgeButton = new JButton(resourceBundle.getString("purgeButtonLabelText"));
			purgeButton.setToolTipText(resourceBundle.getString("purgeButtonToolTipText"));
			buttonPanel.add(purgeButton);
			purgeButton.addActionListener(new PurgeActionListener());
		}
		
		//JButton cancelButton = new JButton(resourceBundle.getString("cancelButtonLabelText"));
		//cancelButton.setToolTipText(resourceBundle.getString("cancelButtonToolTipText"));
		//buttonPanel.add(cancelButton);
		//cancelButton.addActionListener(new CancelActionListener());
		
		JPanel queryFilterTextEntryPanel = null;
		if (allowUserQuery) {
			queryFilterTextEntryPanel = new JPanel();
			queryFilterTextEntryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			queryFilterTextEntryPanel.setBorder(panelBorder);

			JLabel queryIntroduction = new JLabel(resourceBundle.getString("queryIntroductionLabelText")+" -");
			queryFilterTextEntryPanel.add(queryIntroduction);

			JLabel queryFilterPatientNameLabel = new JLabel(resourceBundle.getString("queryPatientNameLabelText")+":");
			queryFilterPatientNameLabel.setToolTipText(resourceBundle.getString("queryPatientNameToolTipText"));
			queryFilterTextEntryPanel.add(queryFilterPatientNameLabel);
			queryFilterPatientNameTextField = new JTextField("",textFieldLengthForQueryPatientName);
			queryFilterTextEntryPanel.add(queryFilterPatientNameTextField);
		
			JLabel queryFilterPatientIDLabel = new JLabel(resourceBundle.getString("queryPatientIDLabelText")+":");
			queryFilterPatientIDLabel.setToolTipText(resourceBundle.getString("queryPatientIDToolTipText"));
			queryFilterTextEntryPanel.add(queryFilterPatientIDLabel);
			queryFilterPatientIDTextField = new JTextField("",textFieldLengthForQueryPatientID);
			queryFilterTextEntryPanel.add(queryFilterPatientIDTextField);
		
			JLabel queryFilterStudyDateLabel = new JLabel(resourceBundle.getString("queryStudyDateLabelText")+":");
			queryFilterStudyDateLabel.setToolTipText(resourceBundle.getString("queryStudyDateToolTipText"));
			queryFilterTextEntryPanel.add(queryFilterStudyDateLabel);
			queryFilterStudyDateTextField = new JTextField("",textFieldLengthForQueryStudyDate);
			queryFilterTextEntryPanel.add(queryFilterStudyDateTextField);
		
			JLabel queryFilterAccessionNumberLabel = new JLabel(resourceBundle.getString("queryAccessionNumberLabelText")+":");
			queryFilterAccessionNumberLabel.setToolTipText(resourceBundle.getString("queryAccessionNumberToolTipText"));
			queryFilterTextEntryPanel.add(queryFilterAccessionNumberLabel);
			queryFilterAccessionNumberTextField = new JTextField("",textFieldLengthForQueryAccessionNumber);
			queryFilterTextEntryPanel.add(queryFilterAccessionNumberTextField);
		
		}
		
		JPanel newTextEntryPanel = new JPanel();
		{
			newTextEntryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			newTextEntryPanel.setBorder(panelBorder);
		
			JLabel replacementIntroduction = new JLabel(resourceBundle.getString("replacementIntroductionLabelText")+" -");
			newTextEntryPanel.add(replacementIntroduction);

			replacePatientNameCheckBox = new JCheckBox(resourceBundle.getString("replacementPatientNameLabelText")+":");
			replacePatientNameCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxReplacePatientNameIsSelected,default_CheckBoxReplacePatientNameIsSelected));
			replacePatientNameCheckBox.setToolTipText(resourceBundle.getString("replacementPatientNameToolTipText"));
			newTextEntryPanel.add(replacePatientNameCheckBox);
			replacementPatientNameTextField = new JTextField(getPropertyOrDefaultAndAddIt(propertyName_ReplacementTextPatientName,resourceBundle.getString("defaultReplacementPatientName")),textFieldLengthForReplacementPatientName);
			newTextEntryPanel.add(replacementPatientNameTextField);
		
			replacePatientIDCheckBox = new JCheckBox(resourceBundle.getString("replacementPatientIDLabelText")+":");
			replacePatientIDCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxReplacePatientIDIsSelected,default_CheckBoxReplacePatientIDIsSelected));
			replacePatientIDCheckBox.setToolTipText(resourceBundle.getString("replacementPatientIDToolTipText"));
			newTextEntryPanel.add(replacePatientIDCheckBox);
			replacementPatientIDTextField = new JTextField(getPropertyOrDefaultAndAddIt(propertyName_ReplacementTextPatientID,resourceBundle.getString("defaultReplacementPatientID")),textFieldLengthForReplacementPatientID);
			newTextEntryPanel.add(replacementPatientIDTextField);
		
			replaceAccessionNumberCheckBox = new JCheckBox(resourceBundle.getString("replacementAccessionNumberLabelText")+":");
			replaceAccessionNumberCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxReplaceAccessionNumberIsSelected,default_CheckBoxReplaceAccessionNumberIsSelected));
			replaceAccessionNumberCheckBox.setToolTipText(resourceBundle.getString("replacementAccessionNumberToolTipText"));
			newTextEntryPanel.add(replaceAccessionNumberCheckBox);
			replacementAccessionNumberTextField = new JTextField(getPropertyOrDefaultAndAddIt(propertyName_ReplacementTextAccessionNumber,resourceBundle.getString("defaultReplacementAccessionNumber")),textFieldLengthForReplacementAccessionNumber);
			newTextEntryPanel.add(replacementAccessionNumberTextField);
			
			JButton randomReplacementButton = new JButton(resourceBundle.getString("randomReplacementButtonLabelText"));
			randomReplacementButton.setToolTipText(resourceBundle.getString("randomReplacementButtonToolTipText"));
			newTextEntryPanel.add(randomReplacementButton);
			randomReplacementButton.addActionListener(new RandomReplacementActionListener());
		
			JButton defaultReplacementButton = new JButton(resourceBundle.getString("defaultReplacementButtonLabelText"));
			defaultReplacementButton.setToolTipText(resourceBundle.getString("defaultReplacementButtonToolTipText"));
			newTextEntryPanel.add(defaultReplacementButton);
			defaultReplacementButton.addActionListener(new DefaultReplacementActionListener());
		}
		
		// The checkbox and textfield are created and given values even if not allowed for display, since the they are used during processing
		modifyDatesCheckBox = new JCheckBox(resourceBundle.getString("modifyDatesLabelText")+":");
		modifyDatesCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxModifyDatesIsSelected,default_CheckBoxModifyDatesIsSelected));
		modifyDatesCheckBox.setToolTipText(resourceBundle.getString("modifyDatesToolTipText"));

		modifyDatesTextField = new JTextField(getPropertyOrDefaultAndAddIt(propertyName_ModifyDatesEpoch,resourceBundle.getString("defaultModifyDatesEpoch")),textFieldLengthForModifyDates);

		JPanel modifyDatesPanel = null;
		if (allowChangeDatesAndTimes) {
			modifyDatesPanel = new JPanel();
			modifyDatesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			modifyDatesPanel.setBorder(panelBorder);
		
			JLabel modifyDatesIntroduction = new JLabel(resourceBundle.getString("modifyDatesIntroductionLabelText")+" -");
			modifyDatesPanel.add(modifyDatesIntroduction);

			modifyDatesPanel.add(modifyDatesCheckBox);
		
			modifyDatesPanel.add(modifyDatesTextField);

			JButton earliestYearButton = new JButton(resourceBundle.getString("earliestYearButtonLabelText"));
			earliestYearButton.setToolTipText(resourceBundle.getString("earliestYearButtonToolTipText"));
			modifyDatesPanel.add(earliestYearButton);
			earliestYearButton.addActionListener(new EarliestYearActionListener());
		
			JButton randomYearButton = new JButton(resourceBundle.getString("randomYearButtonLabelText"));
			randomYearButton.setToolTipText(resourceBundle.getString("randomYearButtonToolTipText"));
			modifyDatesPanel.add(randomYearButton);
			randomYearButton.addActionListener(new RandomYearActionListener());
		
			JButton defaultYearButton = new JButton(resourceBundle.getString("defaultYearButtonLabelText"));
			defaultYearButton.setToolTipText(resourceBundle.getString("defaultYearButtonToolTipText"));
			modifyDatesPanel.add(defaultYearButton);
			defaultYearButton.addActionListener(new DefaultYearActionListener());
		}

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new GridLayout(0,4));	// number of rows is ignored if number of columns is not 0
		checkBoxPanel.setBorder(panelBorder);

		// The checkboxes are created and given values even if not allowed for display, since the checkbox value is used during processing
		
		removeIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeIdentityLabelText"));
		removeIdentityCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveIdentityIsSelected,default_CheckBoxRemoveIdentityIsSelected));
		if (allowRemoveIdentityCheckBox) checkBoxPanel.add(removeIdentityCheckBox);
			
		removeDescriptionsCheckBox = new JCheckBox(resourceBundle.getString("removeDescriptionsLabelText"));
		removeDescriptionsCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveDescriptionsIsSelected,default_CheckBoxRemoveDescriptionsIsSelected));
		if (allowRemoveDescriptionsCheckBox) checkBoxPanel.add(removeDescriptionsCheckBox);
		
		removeSeriesDescriptionsCheckBox = new JCheckBox(resourceBundle.getString("removeSeriesDescriptionsLabelText"));
		removeSeriesDescriptionsCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveSeriesDescriptionsIsSelected,default_CheckBoxRemoveSeriesDescriptionsIsSelected));
		if (allowRemoveSeriesDescriptionsCheckBox) checkBoxPanel.add(removeSeriesDescriptionsCheckBox);
	
		removeProtocolNameCheckBox = new JCheckBox(resourceBundle.getString("removeProtocolNameLabelText"));
		removeProtocolNameCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveProtocolNameIsSelected,default_CheckBoxRemoveProtocolNameIsSelected));
		if (allowRemoveProtocolNameCheckBox) checkBoxPanel.add(removeProtocolNameCheckBox);

		removeCharacteristicsCheckBox = new JCheckBox(resourceBundle.getString("removeCharacteristicsLabelText"));
		removeCharacteristicsCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveCharacteristicsIsSelected,default_CheckBoxRemoveCharacteristicsIsSelected));
		if (allowRemoveCharacteristicsCheckBox) checkBoxPanel.add(removeCharacteristicsCheckBox);

		cleanUIDsCheckBox = new JCheckBox(resourceBundle.getString("cleanUIDsLabelText"));
		cleanUIDsCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxCleanUIDsIsSelected,default_CheckBoxCleanUIDsIsSelected));
		if (allowCleanUIDsCheckBox) checkBoxPanel.add(cleanUIDsCheckBox);
		
		removePrivateCheckBox = new JCheckBox(resourceBundle.getString("removePrivateLabelText"));
		removePrivateCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemovePrivateIsSelected,default_CheckBoxRemovePrivateIsSelected));
		if (allowRemovePrivateCheckBox) checkBoxPanel.add(removePrivateCheckBox);
		
		removeDeviceIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeDeviceIdentityLabelText"));
		removeDeviceIdentityCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveDeviceIdentityIsSelected,default_CheckBoxRemoveDeviceIdentityIsSelected));
		if (allowRemoveDeviceIdentityCheckBox) checkBoxPanel.add(removeDeviceIdentityCheckBox);
		
		removeInstitutionIdentityCheckBox = new JCheckBox(resourceBundle.getString("removeInstitutionIdentityLabelText"));
		removeInstitutionIdentityCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveInstitutionIdentityIsSelected,default_CheckBoxRemoveInstitutionIdentityIsSelected));
		if (allowRemoveInstitutionIdentityCheckBox) checkBoxPanel.add(removeInstitutionIdentityCheckBox);
	
		removeClinicalTrialAttributesCheckBox = new JCheckBox(resourceBundle.getString("removeClinicalTrialAttributesLabelText"));
		removeClinicalTrialAttributesCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxRemoveClinicalTrialAttributesIsSelected,default_CheckBoxRemoveClinicalTrialAttributesIsSelected));
		if (allowRemoveClinicalTrialAttributesCheckBox) checkBoxPanel.add(removeClinicalTrialAttributesCheckBox);
		
		addContributingEquipmentCheckBox = new JCheckBox(resourceBundle.getString("addContributingEquipmentLabelText"));
		addContributingEquipmentCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxAddContributingEquipmentIsSelected,default_CheckBoxAddContributingEquipmentIsSelected));
		if (allowAddContributingEquipmentCheckBox) checkBoxPanel.add(addContributingEquipmentCheckBox);
			
		zipExportCheckBox = new JCheckBox(resourceBundle.getString("zipExportLabelText"));
		zipExportCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxZipExportIsSelected,default_CheckBoxZipExportIsSelected));
		if (allowZipExportCheckBox) checkBoxPanel.add(zipExportCheckBox);
			
		hierarchicalExportCheckBox = new JCheckBox(resourceBundle.getString("hierarchicalExportLabelText"));
		hierarchicalExportCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxHierarchicalExportIsSelected,default_CheckBoxHierarchicalExportIsSelected));
		hierarchicalExportCheckBox.setToolTipText(resourceBundle.getString("hierarchicalExportToolTipText"));
		if (allowHierarchicalExportCheckBox) checkBoxPanel.add(hierarchicalExportCheckBox);
			
		acceptAnyTransferSyntaxCheckBox = new JCheckBox(resourceBundle.getString("acceptAnyTransferSyntaxLabelText"));
		acceptAnyTransferSyntaxCheckBox.setSelected(getBooleanPropertyOrDefaultAndAddIt(propertyName_CheckBoxAcceptAnyTransferSyntaxIsSelected,default_CheckBoxAcceptAnyTransferSyntaxIsSelected));
		acceptAnyTransferSyntaxCheckBox.setToolTipText(resourceBundle.getString("acceptAnyTransferSyntaxToolTipText"));
		if (allowAcceptAnyTransferSyntaxCheckBox) checkBoxPanel.add(acceptAnyTransferSyntaxCheckBox);
				
		JPanel statusBarPanel = new JPanel();
		{
			GridBagLayout statusBarPanelLayout = new GridBagLayout();
			statusBarPanel.setLayout(statusBarPanelLayout);
			{
				JLabel statusBar = getStatusBar();
				GridBagConstraints statusBarConstraints = new GridBagConstraints();
				statusBarConstraints.weightx = 1;
				statusBarConstraints.fill = GridBagConstraints.BOTH;
				statusBarConstraints.anchor = GridBagConstraints.WEST;
				statusBarConstraints.gridwidth = GridBagConstraints.RELATIVE;
				statusBarPanelLayout.setConstraints(statusBar,statusBarConstraints);
				statusBarPanel.add(statusBar);
			}
			{
				JProgressBar progressBar = new JProgressBar();		// local not class scope; helps detect when being accessed other than through SafeProgressBarUpdaterThread
				progressBar.setStringPainted(false);
				GridBagConstraints progressBarConstraints = new GridBagConstraints();
				progressBarConstraints.weightx = 0.5;
				progressBarConstraints.fill = GridBagConstraints.BOTH;
				progressBarConstraints.anchor = GridBagConstraints.EAST;
				progressBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
				statusBarPanelLayout.setConstraints(progressBar,progressBarConstraints);
				statusBarPanel.add(progressBar);
				
				progressBarUpdater = new SafeProgressBarUpdaterThread(progressBar);
			}
		}
		
		JPanel mainPanel = new JPanel();
		{
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanel.setLayout(mainPanelLayout);
			{
				GridBagConstraints panesToUseForMainPanelConstraints = new GridBagConstraints();
				panesToUseForMainPanelConstraints.gridx = 0;
				panesToUseForMainPanelConstraints.gridy = 0;
				panesToUseForMainPanelConstraints.weightx = 1;
				panesToUseForMainPanelConstraints.weighty = 1;
				panesToUseForMainPanelConstraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(panesToUseForMainPanel,panesToUseForMainPanelConstraints);
				mainPanel.add(panesToUseForMainPanel);
			}
			{
				GridBagConstraints buttonPanelConstraints = new GridBagConstraints();
				buttonPanelConstraints.gridx = 0;
				buttonPanelConstraints.gridy = 1;
				buttonPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(buttonPanel,buttonPanelConstraints);
				mainPanel.add(buttonPanel);
			}
			if (queryFilterTextEntryPanel != null) {
				GridBagConstraints queryFilterTextEntryPanelConstraints = new GridBagConstraints();
				queryFilterTextEntryPanelConstraints.gridx = 0;
				queryFilterTextEntryPanelConstraints.gridy = 2;
				queryFilterTextEntryPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(queryFilterTextEntryPanel,queryFilterTextEntryPanelConstraints);
				mainPanel.add(queryFilterTextEntryPanel);
			}
			{
				GridBagConstraints newTextEntryPanelConstraints = new GridBagConstraints();
				newTextEntryPanelConstraints.gridx = 0;
				newTextEntryPanelConstraints.gridy = 3;
				newTextEntryPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(newTextEntryPanel,newTextEntryPanelConstraints);
				mainPanel.add(newTextEntryPanel);
			}
			if (modifyDatesPanel != null) {
				GridBagConstraints modifyDatesPanelConstraints = new GridBagConstraints();
				modifyDatesPanelConstraints.gridx = 0;
				modifyDatesPanelConstraints.gridy = 4;
				modifyDatesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(modifyDatesPanel,modifyDatesPanelConstraints);
				mainPanel.add(modifyDatesPanel);
			}
			{
				GridBagConstraints checkBoxPanelConstraints = new GridBagConstraints();
				checkBoxPanelConstraints.gridx = 0;
				checkBoxPanelConstraints.gridy = 5;
				checkBoxPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(checkBoxPanel,checkBoxPanelConstraints);
				mainPanel.add(checkBoxPanel);
			}
			{
				GridBagConstraints statusBarPanelConstraints = new GridBagConstraints();
				statusBarPanelConstraints.gridx = 0;
				statusBarPanelConstraints.gridy = 6;
				statusBarPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(statusBarPanel,statusBarPanelConstraints);
				mainPanel.add(statusBarPanel);
			}
		}
		Container content = getContentPane();
		content.add(mainPanel);
		pack();
		setVisible(true);
		
		if (pathName != null && pathName.length() > 0) {
			new Thread(new ImportWorker(pathName,srcDatabase,srcDatabasePanel)).start();
		}
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	optionally, a single path to a DICOM file or folder to search for importable DICOM files
	 */
	public static void main(String arg[]) {
		try {
			String pathName = null;
			if (arg.length > 0) {
				pathName = arg[0];
			}
			new DicomCleaner(pathName);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

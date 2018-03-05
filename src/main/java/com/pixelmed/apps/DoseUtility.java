/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.DatabaseTreeBrowser;
import com.pixelmed.database.DatabaseTreeRecord;
import com.pixelmed.database.MinimalPatientStudySeriesInstanceModel;
//import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.dicom.*;

import com.pixelmed.event.ApplicationEventDispatcher;

import com.pixelmed.display.event.StatusChangeEvent;

import com.pixelmed.display.ApplicationFrame;
import com.pixelmed.display.DialogMessageLogger;
import com.pixelmed.display.DicomBrowser;
import com.pixelmed.display.SafeCursorChanger;
import com.pixelmed.display.SafeFileChooser;
import com.pixelmed.display.SafeProgressBarUpdaterThread;

import com.pixelmed.dose.CTDose;
import com.pixelmed.dose.CTIrradiationEventDataFromImages;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.GenerateRadiationDoseStructuredReport;
import com.pixelmed.doseocr.OCR;
import com.pixelmed.doseocr.RenderedDoseReport;

import com.pixelmed.ftp.FTPApplicationProperties;
import com.pixelmed.ftp.FTPClientApplicationConfigurationDialog;
import com.pixelmed.ftp.FTPFileSender;
import com.pixelmed.ftp.FTPRemoteHost;
import com.pixelmed.ftp.FTPRemoteHostInformation;

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

import com.pixelmed.validate.DicomSRValidator;

//import apple.dts.samplecode.osxadapter.OSXAdapter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
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
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.Border;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.TreePath;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is an application for retrieving dose information about DICOM studies of patients.</p>
 * 
 * <p>It is configured by use of a properties file that resides in the user's
 * home directory in <code>.com.pixelmed.display.DoseUtility.properties</code>.</p>
 * 
 * <p>It supports retrieval of Dose SR objects and dose screen save images.</p>
 * 
 * @author	dclunie
 */
public class DoseUtility extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/DoseUtility.java,v 1.59 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(StorageSOPClassSCU.class);

	protected static String propertiesFileName  = ".com.pixelmed.apps.DoseUtility.properties";
	
	protected static String propertyName_DicomCurrentlySelectedStorageTargetAE = "Dicom.CurrentlySelectedStorageTargetAE";
	protected static String propertyName_DicomCurrentlySelectedQueryTargetAE = "Dicom.CurrentlySelectedQueryTargetAE";
	
	protected static String propertyName_CurrentlySelectedDoseRegistry = "DoseUtility.CurrentlySelectedDoseRegistry";
	
	protected static String localDatabaseName = "Local";
	protected static String localDatabaseServerName = "DoseUtility";	// if not null, can access externally: java -cp lib/additional/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --url "jdbc:hsqldb:hsql://localhost/DoseUtility"

	protected static int textFieldLengthForQueryPatientName = 16;
	protected static int textFieldLengthForQueryPatientID = 10;
	protected static int textFieldLengthForQueryStudyDate = 8;

	static protected String queryIntroductionLabelText = "Query -";
	static protected String queryPatientNameLabelText = "Patient's Name:";
	static protected String queryPatientIDLabelText = "Patient's ID:";
	static protected String queryStudyDateLabelText = "Study Date:";
	
	static protected String configureButtonLabel = "Configure";
	static protected String       logButtonLabel = "Log";
	static protected String     queryButtonLabel = "Query";
	static protected String  retrieveButtonLabel = "Retrieve";
	static protected String    importButtonLabel = "Import";
	static protected String      viewButtonLabel = "View";
	static protected String  validateButtonLabel = "Validate";
	static protected String    reportButtonLabel = "Report";
	static protected String    makeSRButtonLabel = "Make RDSR";
	static protected String      sendButtonLabel = "->Dicom";
	static protected String  registryButtonLabel = "->Registry";
	static protected String     purgeButtonLabel = "Purge";

	static protected String configureButtonToolTipText = "Configure the application, including DICOM network properties";
	static protected String       logButtonToolTipText = "Show the log of activities";
	static protected String     queryButtonToolTipText = "Query a remote DICOM network host";
	static protected String  retrieveButtonToolTipText = "Retrieve query selection from remote DICOM network host";
	static protected String    importButtonToolTipText = "Import from media into originals database";
	static protected String      viewButtonToolTipText = "View image or structured report contents";
	static protected String  validateButtonToolTipText = "Validate structured report contents";
	static protected String    reportButtonToolTipText = "Generate report of dose information";
	static protected String    makeSRButtonToolTipText = "Generate Radiation Dose Structured Report from extracted dose information";
	static protected String      sendButtonToolTipText = "Send Radiation Dose Structured Reports to remote DICOM host";
	static protected String  registryButtonToolTipText = "Send Radiation Dose Structured Reports to Dose Registry";
	static protected String     purgeButtonToolTipText = "Remove selected entry from local database and delete any local copy of files";

	static protected String queryPatientNameToolTipText = "The text to use for the Patient's Name when querying";
	static protected String queryPatientIDToolTipText = "The text to use for the Patient's ID when querying";
	static protected String queryStudyDateToolTipText = "The text to use for the Study Date when querying";

	static protected String retrieveOnlyDoseSeriesRecordLabelText = "Retrieve only dose series";
	static protected String processOnlyDoseSeriesRecordLabelText = "Process only dose series";
	static protected String showOnlyDoseSummaryLabelText = "Show only dose summary";
	static protected String showDetailedLogLabelText = "Show detailed log";
	static protected String reportTabularLayoutLabelText = "Show tabular layout in report";
	static protected String caseSensitiveCodeMeaningValidationLabelText = "Case sensitive validation of code meanings";
	static protected String deidentifyWhenSendingToRegistryLabelText = "Deidentify when sending to registry";

	static protected String reportTitleMessage = "DoseUtility Report";
	static protected String loggerTitleMessage = "DoseUtility Log";
	
	static protected int viewerFrameWidthWanted  = 512;		// want to be small enough that default does not interpolate (which makes dose screen unreadable)
	static protected int viewerFrameHeightWanted = 512;
	
	static protected int validatorFrameWidthWanted  = 512;
	static protected int validatorFrameHeightWanted = 384;
	
	static protected int reportDialogWidthWanted  = 1200;
	static protected int reportDialogHeightWanted = 384;
	
	static protected int loggertDialogWidthWanted  = 512;
	static protected int loggerDialogHeightWanted = 384;

	protected DatabaseInformationModel srcDatabase;
	
	protected JPanel srcDatabasePanel;
	protected JPanel remoteQueryRetrievePanel;
	
	protected JCheckBox retrieveOnlyDoseSeriesRecordCheckBox;
	protected JCheckBox processOnlyDoseSeriesRecordCheckBox;
	protected JCheckBox showOnlyDoseSummaryCheckBox;
	protected JCheckBox showDetailedLogCheckBox;
	protected JCheckBox reportTabularLayoutCheckBox;
	protected JCheckBox caseSensitiveCodeMeaningValidationCheckBox;
	protected JCheckBox deidentifyWhenSendingToRegistryCheckBox;

	protected JTextField queryFilterPatientNameTextField;
	protected JTextField queryFilterPatientIDTextField;
	protected JTextField queryFilterStudyDateTextField;
	
	protected SafeProgressBarUpdaterThread progressBarUpdater;
	
	protected SafeCursorChanger cursorChanger;
	
	protected MessageLogger logger;
	
	protected NetworkApplicationProperties networkApplicationProperties;
	protected NetworkApplicationInformation networkApplicationInformation;

	protected FTPApplicationProperties ftpApplicationProperties;
	protected FTPRemoteHostInformation ftpRemoteHostInformation;

	protected QueryInformationModel currentRemoteQueryInformationModel;
	protected QueryTreeBrowser currentRemoteQueryTreeBrowser;
	
	//protected QueryTreeRecord currentRemoteQuerySelectionQueryTreeRecord;
	//protected AttributeList currentRemoteQuerySelectionUniqueKeys;
	//protected Attribute currentRemoteQuerySelectionUniqueKey;
	//protected String currentRemoteQuerySelectionRetrieveAE;
	//protected String currentRemoteQuerySelectionLevel;

	protected String ourCalledAETitle;		// set when reading network properties; used not just in StorageSCP, but also when creating exported meta information headers
	
	protected static DicomSRValidator validator;
	
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
				slf4jlogger.error("",e);
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

	protected static void importFileIntoDatabase(DatabaseInformationModel database,String dicomFileName,String fileRefererenceType) throws FileNotFoundException, IOException, DicomException {
		ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing: "+dicomFileName));
//System.err.println("Importing: "+dicomFileName);
		FileInputStream fis = new FileInputStream(dicomFileName);
		DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
		AttributeList list = new AttributeList();
		list.read(i,TagFromName.PixelData);
		i.close();
		fis.close();
		database.insertObject(list,dicomFileName,fileRefererenceType);
	}

	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
				String localName = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(callingAETitle);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Received "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax));
//System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Received "+dicomFileName+" from "+localName+" ("+callingAETitle+")"); }
				try {
					importFileIntoDatabase(srcDatabase,dicomFileName,DatabaseInformationModel.FILE_COPIED);
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
slf4jlogger.info("Starting up DICOM association listener on port "+port+" AET "+ourCalledAETitle);
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
//System.err.println("DoseUtility.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): offered "+presentationContexts);
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
					}
				}
				// discard old list and make a new one ...
				pc.newTransferSyntaxUIDs();
				// Policy is prefer bzip then deflate compressed then explicit (little then big) then implicit,
				// then supported image compression transfer syntaxes in the following order and ignore anything else
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
				else {
					pc.setResultReason((byte)4);				// transfer syntaxes not supported (provider rejection)
				}
			}
//System.err.println("DoseUtility.OurTransferSyntaxSelectionPolicy.applyTransferSyntaxSelectionPolicy(): accepted "+presentationContexts);
			return presentationContexts;
		}
	}

	/**
	 * <p>Start local database.</p>
	 *
	 * <p>Will not persist when the application is closed, so in memory database
	 *  is used and instances live in the temporary filesystem.</p>
	 *
	 * @throws	DicomException
	 */
	protected void activateTemporaryDatabases() throws DicomException {
		//srcDatabase = new PatientStudySeriesConcatenationInstanceModel("mem:src",localDatabaseServerName,localDatabaseName);
		srcDatabase = new MinimalPatientStudySeriesInstanceModel("mem:src",localDatabaseServerName,localDatabaseName);
	}

	protected DatabaseTreeRecord[] currentDatabaseTreeRecordSelections;
	
	protected Vector getCurrentSourceFilePathSelections() {
		Vector names = new Vector();
		if (currentDatabaseTreeRecordSelections != null) {
			for (DatabaseTreeRecord r : currentDatabaseTreeRecordSelections) {
				DatabaseTreeBrowser.recurseThroughChildrenGatheringFileNames(r,names);
			}
		}
		return names;
	}

	protected class OurSourceDatabaseTreeBrowser extends DatabaseTreeBrowser {
		public OurSourceDatabaseTreeBrowser(DatabaseInformationModel d,Container content) throws DicomException {
			super(d,content);
//System.err.println("DoseUtility.OurSourceDatabaseTreeBrowser(): end of constructor ");
		}
		
		protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedFiles() {
			return new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					currentDatabaseTreeRecordSelections = getSelectionPaths();
				}
			};
		}

		// doSomethingWithSelections(DatabaseTreeRecord) is never called so do not need to override it ... we set the currentDatabaseTreeRecordSelections value directly

		// doSomethingWithSelectedFiles(Vector paths) is never called so do not need to override it ... we build this ourselves on demand from the cached currentDatabaseTreeRecordSelection

		protected void doSomethingMoreWithWhateverWasSelected() {
			// doSomethingMoreWithWhateverWasSelected() do nothing on double click of node
		}
		
	}
	
	/**
	 * <p>Recursively process the specified DatabaseTreeRecord and all its children finding file paths of possible dose-related files (not the reconstructed image files) at the instance level.</p>
	 *
	 * <p>Based on the method in DatabaseTreeBrowser, modified to filter by database record type.</p>
	 *
	 * @param	r				the current DatabaseTreeRecord to process
	 * @param	names			the file names (paths) to add to
	 * @param	onlyDoseFiles	if true, only process dose-related files (not the reconstructed image files)
	 * @param	manufacturer	passed from series level down to instance
	 */
	public static void recurseThroughChildrenGatheringFileNamesOfDoseFiles(DatabaseTreeRecord r,Vector names,boolean onlyDoseFiles,String manufacturer) {
		InformationEntity ie = r.getInformationEntity();
		boolean addIt = true;
		boolean recurse = true;
		if (onlyDoseFiles) {
			String localPrimaryKeyValue = r.getLocalPrimaryKeyValue();
			if (ie != null && localPrimaryKeyValue != null) {
				Map map = null;
				try {
					map = r.getDatabaseInformationModel().findAllAttributeValuesForSelectedRecord(ie,localPrimaryKeyValue);
					// use similar (though inverse) logic here as when filtering query records for remote retrieval ...
					if (ie.equals(InformationEntity.SERIES)) {
						manufacturer = (String)(map.get("MANUFACTURER"));
						String modality = (String)(map.get("MODALITY"));
						String seriesNumber = (String)(map.get("SERIESNUMBER"));
						String seriesDescription = (String)(map.get("SERIESDESCRIPTION"));
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): manufacturer = "+manufacturer);
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): modality = "+modality);
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): seriesNumber = "+seriesNumber);
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): seriesDescription = "+seriesDescription);
						if (!OCR.isPossiblyDoseScreenSeries(manufacturer,modality,seriesNumber,seriesDescription)
						 && !ExposureDoseSequence.isPossiblyPhilipsDoseScreenSeries(manufacturer,modality,seriesNumber,seriesDescription)
						 && !modality.equals("SR")		// potential SR Dose Report (may retrieve many other types of SR, but worth it since will get prototype SR and those from PACS that don't support proper SOP Class))
						) {
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): is not a possible dose series");
							recurse = false;
						}
					}
					else if (ie.equals(InformationEntity.INSTANCE)) {
						String sopClassUID = (String)(map.get("SOPCLASSUID"));
						String imageType = (String)(map.get("IMAGETYPE"));
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): sopClassUID = "+sopClassUID);
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): imageType = "+imageType);
						if (!OCR.isPossiblyDoseScreenInstance(manufacturer,sopClassUID,imageType)
						 && !ExposureDoseSequence.isPossiblyPhilipsDoseScreenInstance(manufacturer,sopClassUID,imageType)
						 && !sopClassUID.equals(SOPClass.XRayRadiationDoseSRStorage)
						 && !sopClassUID.equals(SOPClass.EnhancedSRStorage)					// e.g., old GE; can't check document title (ConceptNameCodeSequence) since not in database
						 && !sopClassUID.equals(SOPClass.ComprehensiveSRStorage)
						) {
//System.err.println("DoseUtility.recurseThroughChildrenGatheringFileNamesOfDoseFiles(): is not a possible dose instance");
							recurse = false;
							addIt = false;
						}
					}
				} catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
		}
		
		if (addIt && ie == InformationEntity.INSTANCE) {
			String fileName = r.getLocalFileNameValue();
			if (fileName != null) {
				names.add(fileName);
			}
		}
		
		if (recurse) {
			Enumeration children = r.children();
			if (children != null) {
				while (children.hasMoreElements()) {
					DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
					if (child != null) {
						recurseThroughChildrenGatheringFileNamesOfDoseFiles(child,names,onlyDoseFiles,manufacturer);
					}
				}
			}
		}
	}

	
	/**
	 * <p>Recursively process the specified DatabaseTreeRecord and all its children finding file paths of SR files at the instance level.</p>
	 *
	 * <p>Based on the method in DatabaseTreeBrowser, modified to filter by database record type.</p>
	 *
	 * @param	r				the current DatabaseTreeRecord to process
	 * @param	names			the file names (paths) to add to
	 */
	public static void recurseThroughChildrenGatheringFileNamesOfSRFiles(DatabaseTreeRecord r,Vector names) {
		InformationEntity ie = r.getInformationEntity();
		if (ie != null && ie.equals(InformationEntity.INSTANCE)) {
			String localPrimaryKeyValue = r.getLocalPrimaryKeyValue();
			if (localPrimaryKeyValue != null) {
				Map map = null;
				try {
					map = r.getDatabaseInformationModel().findAllAttributeValuesForSelectedRecord(ie,localPrimaryKeyValue);
					if (map != null) {
						String sopClassUID = (String)(map.get("SOPCLASSUID"));
						if (SOPClass.isStructuredReport(sopClassUID)) {
							String fileName = r.getLocalFileNameValue();
							if (fileName != null) {
								names.add(fileName);
							}
						}
					}
				} catch (Exception e) {
					slf4jlogger.error("",e);
				}
			}
		}
		
		Enumeration children = r.children();
		if (children != null) {
			while (children.hasMoreElements()) {
				DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
				if (child != null) {
					recurseThroughChildrenGatheringFileNamesOfSRFiles(child,names);
				}
			}
		}
	}

	protected Vector extractRadiationDoseSRFiles(Vector candidatePaths) {
		Vector actualPaths = new Vector();
		if (candidatePaths != null) {
			int fileCount = candidatePaths.size();
			for (int i=0; i<fileCount; ++i) {
				String fileName = (String)(candidatePaths.get(i));
//System.err.println("DoseUtility.extractRadiationDoseSRFiles(): checking candidate "+fileName);
				try {
					DicomInputStream di = new DicomInputStream(new FileInputStream(fileName));
					AttributeList list = new AttributeList();
					list.read(di);
					di.close();
//System.err.println(list);
					{
						// re-used in DoseReporterWithLegacyOCRAndAutoSendToRegistry ... should refactor :(
						String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
						if (sopClassUID.equals(SOPClass.XRayRadiationDoseSRStorage)) {
//System.err.println("DoseUtility.extractRadiationDoseSRFiles(): is RDSR SOP Class");
							actualPaths.add(fileName);
						}
						else if (list.isSRDocument()) {	// e.g., an old Enhanced SOP Class GE RDSR with the right template
							CodedSequenceItem documentTitle = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
							if (documentTitle != null) {
								String csd = documentTitle.getCodingSchemeDesignator();
								String cv = documentTitle.getCodeValue();
								if (csd != null && csd.equals("DCM") && cv != null && cv.equals("113701")) {	// X-Ray Radiation Dose Report
//System.err.println("DoseUtility.extractRadiationDoseSRFiles(): is SR SOP Class with X-Ray Radiation Dose Report document title");
									actualPaths.add(fileName);
								}
							}
						}
					}
				}
				catch (Exception e) {
					if (showDetailedLogCheckBox.isSelected()) { logger.sendLn(e.toString()); }
					slf4jlogger.error("",e);
				}
			}
		}
		return actualPaths;
	}

	protected void generateDoseReportInformation(DatabaseTreeRecord[] databaseSelections,JEditorPane reportPane,StringBuffer textBuffer,boolean doHTML,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
		if (databaseSelections != null) {
			for (DatabaseTreeRecord databaseSelection : databaseSelections) {
				generateDoseReportInformation(databaseSelection,reportPane,textBuffer,doHTML,logger,progressBarUpdater,done,maximum);
			}
		}
	}
		
	protected void generateDoseReportInformation(DatabaseTreeRecord databaseSelection,JEditorPane reportPane,StringBuffer textBuffer,boolean doHTML,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
//System.err.println("DoseUtility.generateDoseReportInformation():");
		if (databaseSelection != null) {
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			InformationEntity ie = databaseSelection.getInformationEntity();
//System.err.println("DoseUtility.generateDoseReportInformation(): ie = "+ie);
			if (ie == null /* the root of the tree, i.e., everything */ || ie.equals(InformationEntity.PATIENT)) {
				// Do it one study at a time, in the order in which the patients and studies are sorted in the tree
				Enumeration children = databaseSelection.children();
				if (children != null) {
					maximum+=databaseSelection.getChildCount();
					while (children.hasMoreElements()) {
						DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
						if (child != null) {
							generateDoseReportInformation(child,reportPane,textBuffer,doHTML,logger,progressBarUpdater,done,maximum);
							++done;
						}
					}
				}
			}
			else {
				// Do everything at the study level and below all at once
				Vector paths = new Vector();
				recurseThroughChildrenGatheringFileNamesOfDoseFiles(databaseSelection,paths,processOnlyDoseSeriesRecordCheckBox.isSelected(),null/*manufacturer*/);
				int fileCount = paths.size();
				maximum+=fileCount;
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
				textBuffer.append(RenderedDoseReport.generateDoseReportInformationFromFiles(paths,showOnlyDoseSummaryCheckBox.isSelected(),doHTML ? "text/html" : "text/plain"));
				done+=fileCount;
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			}
		}
	}
	
	protected class ReportWorker implements Runnable {
		DatabaseTreeRecord[] databaseSelections;
		
		ReportWorker(DatabaseTreeRecord[] databaseSelections) {
			this.databaseSelections=databaseSelections;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Reporting started");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Reporting started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				boolean doHTML = reportTabularLayoutCheckBox.isSelected();
				JEditorPane reportPane = new JEditorPane();
				reportPane.setContentType(doHTML ? "text/html" : "text/plain");
				reportPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);	// makes it use nice component default san serif font, rather than nasty serif font

				Border panelBorder = BorderFactory.createEtchedBorder();
				JScrollPane reportScrollPane = new JScrollPane(reportPane);
				reportScrollPane.setBorder(panelBorder);

				JDialog reportDialog = new JDialog();
				reportDialog.setPreferredSize(new Dimension(reportDialogWidthWanted,reportDialogHeightWanted));
				reportDialog.setTitle(reportTitleMessage);
				reportDialog.getContentPane().add(reportScrollPane);
				reportDialog.pack();
				{
					String leader;
					String trailer;
					if (doHTML) {
						leader = "<html><body><table border=1>";
						if (showOnlyDoseSummaryCheckBox.isSelected()) {
							leader += CTDose.getHTMLTableHeaderRow();
						}
						// else will be included before every entry by CTDose.getHTMLTableRow(true) invoked by RenderedDoseReport.generateDoseReportInformationFromFiles()
						trailer = "</table></body></html>";
					}
					else {
						leader = "";
						trailer = "";
					}
					StringBuffer textBuffer = new StringBuffer();
					generateDoseReportInformation(databaseSelections,reportPane,textBuffer,doHTML,logger,progressBarUpdater,0,1);
					String useText = leader + textBuffer.toString() + trailer;
//System.err.println("DoseUtility.ReportWorker.run(): setText() to "+useText);
					reportPane.setText(useText);
					reportDialog.setVisible(true);
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Reporting failed: "+e));
				slf4jlogger.error("Reporting failed",e);
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Reporting complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done reporting"));
			cursorChanger.restoreCursor();
		}
	}

	protected class ReportActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new ReportWorker(currentDatabaseTreeRecordSelections));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Reporting failed: "+e));
				slf4jlogger.error("Reporting failed",e);
			}
		}
	}
		
		
	// very similar to code in DicomCleaner and DicomImageViewer and DownloadOrTransmit apart from logging and progress bar ... should refactor :(
	protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord[] databaseSelections,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
		if (databaseSelections != null) {
			for (DatabaseTreeRecord databaseSelection : databaseSelections) {
				purgeFilesAndDatabaseInformation(databaseSelection,logger,progressBarUpdater,done,maximum);
			}
		}
	}
	
	protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord databaseSelection,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): "+databaseSelection);
		if (databaseSelection != null) {
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			InformationEntity ie = databaseSelection.getInformationEntity();
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): ie = "+ie);
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
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): removeFromParent having recursed over children "+databaseSelection);
					if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Purging "+databaseSelection); }
					databaseSelection.removeFromParent();
				}
			}
			else {
				// Instance level ... may need to delete files
				String fileName = databaseSelection.getLocalFileNameValue();
				String fileReferenceType = databaseSelection.getLocalFileReferenceTypeValue();
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): fileReferenceType = "+fileReferenceType+" for file "+fileName);
				if (fileReferenceType != null && fileReferenceType.equals(DatabaseInformationModel.FILE_COPIED)) {
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): deleting fileName "+fileName);
					try {
						if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Deleting file "+fileName); }
						if (!new File(fileName).delete()) {
							logger.sendLn("Failed to delete local copy of file "+fileName);
						}
					}
					catch (Exception e) {
						slf4jlogger.error("Failed to delete local copy of file {}",fileName,e);
						logger.sendLn("Failed to delete local copy of file "+fileName);
					}
				}
//System.err.println("DoseUtility.purgeFilesAndDatabaseInformation(): removeFromParent instance level "+databaseSelection);
				if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Purging "+databaseSelection); }
				databaseSelection.removeFromParent();
			}
		}
	}

	protected class PurgeWorker implements Runnable {
		DatabaseTreeRecord[] databaseSelections;
		
		PurgeWorker(DatabaseTreeRecord[] databaseSelections) {
			this.databaseSelections=databaseSelections;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			logger.sendLn("Purging started");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				purgeFilesAndDatabaseInformation(databaseSelections,logger,progressBarUpdater,0,1);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				slf4jlogger.error("Purging failed",e);
			}
			srcDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				slf4jlogger.error("Refresh source database browser failed",e);
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
				activeThread = new Thread(new PurgeWorker(currentDatabaseTreeRecordSelections));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: "+e));
				slf4jlogger.error("Purging failed",e);
			}
		}
	}
		
	protected static String getSRDescriptionForLog(AttributeList list) {
		return 
			"Patient "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientID)
			+ " Study "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate)
			+ " Accession "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AccessionNumber)
			;
	}
		
	protected static String getOriginDescriptionForLog(CTDose ctDose,DatabaseInformationModel srcDatabase) throws DicomException {
		String modality = "";
		String seriesNumber = "";
		String seriesDescription = "";
		String instanceNumber = "";

		String sourceSOPInstanceUID = ctDose.getSourceSOPInstanceUID();
//System.err.println("DoseUtility.generateDoseSR(): sourceSOPInstanceUID = "+sourceSOPInstanceUID);
		if (sourceSOPInstanceUID != null) {
			ArrayList<Map> result = srcDatabase.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity.INSTANCE,sourceSOPInstanceUID);
			if (result != null) {
//System.err.println("DoseUtility.generateDoseSR(): result.size() = "+result.size());
				if (result.size() >= 1) {	// should only be one !
					Map<String,String> instanceMap = result.get(0);
					if (instanceMap != null) {
//System.err.println("DoseUtility.generateDoseSR(): instanceMap.size() = "+instanceMap.size());
						//Iterator<String> instanceMapIterator = instanceMap.keySet().iterator();
						//while (instanceMapIterator.hasNext()) {
						//	String key = instanceMapIterator.next();
						//	String value = instanceMap.get(key);
//System.err.println("DoseUtility.generateDoseSR(): instance "+key+" = "+value);
						//}
						instanceNumber = instanceMap.get("INSTANCENUMBER");
						
						String localParentReferenceColumnName =	srcDatabase.getLocalParentReferenceColumnName(InformationEntity.INSTANCE);
//System.err.println("DoseUtility.generateDoseSR(): localParentReferenceColumnName = "+localParentReferenceColumnName);
						if (localParentReferenceColumnName != null) {
							String localParentReference = instanceMap.get(localParentReferenceColumnName);
//System.err.println("DoseUtility.generateDoseSR(): localParentReference = "+localParentReference);
							if (localParentReference != null) {
								Map<String,String> seriesMap = srcDatabase.findAllAttributeValuesForSelectedRecord(InformationEntity.SERIES,localParentReference);
								if (seriesMap != null) {
//System.err.println("DoseUtility.generateDoseSR(): seriesMap.size() = "+seriesMap.size());
									//Iterator<String> seriesMapIterator = seriesMap.keySet().iterator();
									//while (seriesMapIterator.hasNext()) {
									//	String key = seriesMapIterator.next();
									//	String value = seriesMap.get(key);
//System.err.println("DoseUtility.generateDoseSR(): series "+key+" = "+value);
									//}
									modality = seriesMap.get("MODALITY");
									seriesNumber = seriesMap.get("SERIESNUMBER");
									seriesDescription = seriesMap.get("SERIESDESCRIPTION");
								}
							}
						}
					}
				}
			}
		}
		return "Series "+modality+" "+seriesNumber+" "+seriesDescription+" Instance "+instanceNumber;
	}
	
	protected void generateDoseSR(DatabaseInformationModel srcDatabase,DatabaseTreeRecord[] databaseSelections,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
		if (databaseSelections != null) {
			for (DatabaseTreeRecord databaseSelection : databaseSelections) {
				generateDoseSR(srcDatabase,databaseSelection,logger,progressBarUpdater,done,maximum);
			}
		}
	}

	protected void generateDoseSR(DatabaseInformationModel srcDatabase,DatabaseTreeRecord databaseSelection,MessageLogger logger,SafeProgressBarUpdaterThread progressBarUpdater,int done,int maximum) throws DicomException, IOException {
//System.err.println("DoseUtility.generateDoseSR(): databaseSelection = "+databaseSelection);
		if (databaseSelection != null) {
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			InformationEntity ie = databaseSelection.getInformationEntity();
//System.err.println("DoseUtility.generateDoseSR(): ie = "+ie);
			if (ie == null /* the root of the tree, i.e., everything */ || ie.equals(InformationEntity.PATIENT)) {
//System.err.println("DoseUtility.generateDoseSR(): doing root or patient");
				// Do it one study at a time, in the order in which the patients and studies are sorted in the tree
				Enumeration children = databaseSelection.children();
				if (children != null) {
					maximum+=databaseSelection.getChildCount();
					while (children.hasMoreElements()) {
						DatabaseTreeRecord child = (DatabaseTreeRecord)(children.nextElement());
						if (child != null) {
							generateDoseSR(srcDatabase,child,logger,progressBarUpdater,done,maximum);
							++done;
						}
					}
				}
			}
			else {
//System.err.println("DoseUtility.generateDoseSR(): doing study or below");
				// Do everything at the study level and below all at once
				Vector paths = new Vector();
				recurseThroughChildrenGatheringFileNamesOfDoseFiles(databaseSelection,paths,processOnlyDoseSeriesRecordCheckBox.isSelected(),null/*manufacturer*/);
				int fileCount = paths.size();
				maximum+=fileCount;
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
				CTDose ctDose = GenerateRadiationDoseStructuredReport.generateDoseReportInformationFromFiles(paths);
				if (ctDose != null) {
//System.err.println("DoseUtility.generateDoseSR(): AttributeList =");
//System.err.print(ctDoseSRList);
					File ctDoseSRFile = File.createTempFile("rdsr",".sr");
					ctDoseSRFile.deleteOnExit();
					String ctDoseSRFileName = ctDoseSRFile.getCanonicalPath();
//System.err.println("DoseUtility.generateDoseSR(): file = "+ctDoseSRFileName);
					String ourAETitle = networkApplicationProperties == null ? "OURAETITLE" : networkApplicationProperties.getCallingAETitle();
					ctDose.write(ctDoseSRFileName,ourAETitle,this.getClass().getCanonicalName());	// has side effect of updating list returned by ctDose.getAttributeList(); uncool :(
					AttributeList ctDoseList = ctDose.getAttributeList();
					srcDatabase.insertObject(ctDoseList,ctDoseSRFileName,DatabaseInformationModel.FILE_COPIED);
					String originDescription = getOriginDescriptionForLog(ctDose,srcDatabase);
					String srDescription = getSRDescriptionForLog(ctDoseList);
					String message = "Generated SR for "+srDescription+" from "+originDescription;
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(message));
					logger.sendLn(message);
				}
				else {
					String message = "No dose information or SR generated for "+databaseSelection.toString();
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(message));
					logger.sendLn(message);
				}
				done+=fileCount;
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done,maximum);
			}
		}
	}
	
	protected class MakeSRWorker implements Runnable {
		DatabaseTreeRecord[] databaseSelections;
		DatabaseInformationModel srcDatabase;
		JPanel srcDatabasePanel;
		
		MakeSRWorker(DatabaseTreeRecord[] databaseSelections,DatabaseInformationModel srcDatabase,JPanel srcDatabasePanel) {
			this.databaseSelections=databaseSelections;
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
			logger.sendLn("Generating SR started");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Generating SR started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				generateDoseSR(srcDatabase,databaseSelections,logger,progressBarUpdater,0,1);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Generating SR failed: "+e));
				slf4jlogger.error("Generating SR failed",e);
			}
			srcDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				slf4jlogger.error("Refresh source database browser failed",e);
			}
			srcDatabasePanel.validate();
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			logger.sendLn("Generating SR complete");
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done generating SR"));
			cursorChanger.restoreCursor();
		}
	}

	protected class MakeSRActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new MakeSRWorker(currentDatabaseTreeRecordSelections,srcDatabase,srcDatabasePanel));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Generating SR failed: "+e));
				slf4jlogger.error("Generating SR failed",e);
			}
		}
	}
	
	protected class ViewWorker implements Runnable {
		Vector sourceFilePathSelections;
		
		ViewWorker(Vector sourceFilePathSelections) {
			this.sourceFilePathSelections=sourceFilePathSelections;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("View started"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("View started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			if (sourceFilePathSelections != null) {
				DicomBrowser.loadAndDisplayImagesFromSOPInstances(sourceFilePathSelections,null,null,viewerFrameWidthWanted,viewerFrameHeightWanted);
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("View complete"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done view"));
			cursorChanger.restoreCursor();
		}
	}

	protected class ViewActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new ViewWorker(getCurrentSourceFilePathSelections()));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("View failed: "+e));
				slf4jlogger.error("View failed",e);
			}
		}
	}
	
	protected class ValidateWorker implements Runnable {
		Vector sourceFilePathSelections;
		
		ValidateWorker(Vector sourceFilePathSelections) {
			this.sourceFilePathSelections=sourceFilePathSelections;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Validation started"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Validation started"));
			if (sourceFilePathSelections != null) {
				SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,sourceFilePathSelections.size());
				int count=0;
				Iterator i = sourceFilePathSelections.iterator();
				while (i.hasNext()) {
					String fileName=(String)i.next();
//System.err.println(fileName);
					try {
						DicomInputStream di = new DicomInputStream(new FileInputStream(fileName));
						AttributeList list = new AttributeList();
						list.read(di);
						di.close();
//System.err.println(list);
						if (list.isSRDocument()) {
							if (validator == null) {
								// lazy instantiation so as not to slow down start up of entire applicatiomn
								validator = new DicomSRValidator();
							}
							if (validator == null) {
								throw new DicomException(" not instantiate a validator");
							}
							validator.setOptionMatchCaseOfCodeMeaning(caseSensitiveCodeMeaningValidationCheckBox.isSelected());
							String outputString = validator.validate(list);
							JTextArea outputTextArea = new JTextArea(outputString);
							JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
							JDialog outputDialog = new JDialog();
							outputDialog.setSize(validatorFrameWidthWanted,validatorFrameHeightWanted);
							outputDialog.setTitle("Validation of "+list.buildInstanceTitleFromAttributeList());
							outputDialog.getContentPane().add(outputScrollPane);
							outputDialog.setVisible(true);
						}
						else {
							throw new DicomException("Validation of "+SOPClassDescriptions.getDescriptionFromUID(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID))+" SOP Class not supported");
						}
					}
					catch (Exception e) {
						if (showDetailedLogCheckBox.isSelected()) { logger.sendLn(e.toString()); }
						slf4jlogger.error("Validation failed",e);
					}
					SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,++count);
				}
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Validation complete"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done validation"));
			cursorChanger.restoreCursor();
		}
	}

	protected class ValidateActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				activeThread = new Thread(new ValidateWorker(getCurrentSourceFilePathSelections()));
				activeThread.start();

			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Validation failed: "+e));
				slf4jlogger.error("Validation failed",e);
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
			SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,nFiles-nRemaining,nFiles);
			if (logger != null) {
				logger.sendLn((success ? "Sent " : "Failed to send ")+fileName);
			}
		}
	}

	protected class SendWorker implements Runnable {
		DatabaseTreeRecord[] databaseSelections;
		DatabaseInformationModel srcDatabase;
		JPanel srcDatabasePanel;
		String hostname;
		int port;
		String calledAETitle;
		String callingAETitle;
		int ourMaximumLengthReceived;
		int socketReceiveBufferSize;
		int socketSendBufferSize;
		
		SendWorker(DatabaseTreeRecord[] databaseSelections,DatabaseInformationModel srcDatabase,JPanel srcDatabasePanel,
				String hostname,int port,String calledAETitle,String callingAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize) {
			this.databaseSelections=databaseSelections;
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
			this.hostname=hostname;
			this.port=port;
			this.calledAETitle=calledAETitle;
			this.callingAETitle=callingAETitle;
			this.ourMaximumLengthReceived=ourMaximumLengthReceived;
			this.socketReceiveBufferSize=socketReceiveBufferSize;
			this.socketSendBufferSize=socketSendBufferSize;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Sending to remote DICOM network host started"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Sending to remote DICOM network host started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			
			if (databaseSelections != null) {
				Vector candidatePaths = new Vector();
				for (DatabaseTreeRecord databaseSelection : databaseSelections) {
					if (databaseSelection != null) {
						recurseThroughChildrenGatheringFileNamesOfSRFiles(databaseSelection,candidatePaths);	// returns all SR files, not just dose-related
					}
				}
				Vector actualPaths = extractRadiationDoseSRFiles(candidatePaths);
				SetOfDicomFiles setOfDicomFiles = new SetOfDicomFiles(actualPaths);
				int nFiles = setOfDicomFiles.size();
				if (nFiles > 0) {
					SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,nFiles);
					try {
						new StorageSOPClassSCU(hostname,port,calledAETitle,callingAETitle,
							ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
							setOfDicomFiles,0/*compressionLevel*/,
							new OurMultipleInstanceTransferStatusHandler(nFiles,showDetailedLogCheckBox.isSelected() ? logger : null,progressBarUpdater));
					}
					catch (Exception e) {
						if (showDetailedLogCheckBox.isSelected()) { logger.sendLn(e.toString()); }
						slf4jlogger.error("Send failed",e);
					}
				}
				else {
					if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("No Radiation Dose SRs to send to remote DICOM network host"); }
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("No Radiation Dose SRs to send to remote DICOM network host"));
				}
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Sending to remote DICOM network host complete"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending to remote DICOM network host"));
			cursorChanger.restoreCursor();
		}
	}
	
	protected class SendActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				Properties properties = getProperties();
				String ae = properties.getProperty(propertyName_DicomCurrentlySelectedStorageTargetAE);
				ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select destination","Send ...",ae);
				if (ae != null) {
					String                   callingAETitle = networkApplicationProperties.getCallingAETitle();
					String                    calledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(ae);
					PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(calledAETitle);
					String                         hostname = presentationAddress.getHostname();
					int                                port = presentationAddress.getPort();
					int            ourMaximumLengthReceived = networkApplicationProperties.getInitiatorMaximumLengthReceived();
					int             socketReceiveBufferSize = networkApplicationProperties.getInitiatorSocketReceiveBufferSize();
					int                socketSendBufferSize = networkApplicationProperties.getInitiatorSocketSendBufferSize();

					activeThread = new Thread(new SendWorker(currentDatabaseTreeRecordSelections,srcDatabase,srcDatabasePanel,hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize));
					activeThread.start();
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Send to remote DICOM network host failed: "+e));
				slf4jlogger.error("Send to remote DICOM network host failed",e);
			}
		}
	}
	
	// largely reused in DoseReporterWithLegacyOCRAndAutoSendToRegistry - should be refactored :(
	protected Vector<String> deidentifyFiles(Vector<String> paths) throws DicomException, IOException {
		Vector<String> newPaths = new Vector<String>();
		if (paths != null) {
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,paths.size());
			for (int j=0; j< paths.size(); ++j) {
				String dicomFileName = (String)(paths.get(j));
				if (dicomFileName != null) {
//System.err.println("DoseUtility.deidentifyFiles(): doing file "+dicomFileName);
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Deidentifying "+dicomFileName));
					// do not log it yet ... wait till we have output file name
					File file = new File(dicomFileName);
					DicomInputStream i = new DicomInputStream(file);
					AttributeList list = new AttributeList();
					list.read(i);
					i.close();

					list.removeGroupLengthAttributes();
					list.correctDecompressedImagePixelModule();
					list.insertLossyImageCompressionHistoryIfDecompressed();
					list.removeMetaInformationHeaderAttributes();
					
					list.removeUnsafePrivateAttributes();
					ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);

					ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,ClinicalTrialsAttributes.HandleUIDs.keep,true/*keepDescriptors*/,true/*keepSeriesDescriptors*/,true/*keepPatientCharacteristics*/,true/*keepDeviceIdentity*/,true/*keepInstitutionIdentity*/);

					ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
							true,
							new CodedSequenceItem("109104","DCM","De-identifying Equipment"),	// per CP 892
							"PixelMed",														// Manufacturer
							null,															// Institution Name
							null,															// Institutional Department Name
							null		,													// Institution Address
							ourCalledAETitle,												// Station Name
							"DoseUtility",													// Manufacturer's Model Name
							null,															// Device Serial Number
							getBuildDate(),													// Software Version(s)
							"De-identified");

					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourCalledAETitle);
					list.insertSuitableSpecificCharacterSetForAllStringValues();	// E.g., may have de-identified Kanji name and need new character set
					File deidentifiedFile = File.createTempFile("clean",".dcm");
					deidentifiedFile.deleteOnExit();
					list.write(deidentifiedFile);
					logger.sendLn("Deidentifying "+dicomFileName+" into "+deidentifiedFile.getCanonicalPath());
					newPaths.add(deidentifiedFile.getCanonicalPath());
				}
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,j+1);
			}
		}
		return newPaths;
	}


	protected class RegistryWorker implements Runnable {
		DatabaseTreeRecord[] databaseSelections;
		DatabaseInformationModel srcDatabase;
		JPanel srcDatabasePanel;
		FTPRemoteHost remoteHost;
		
		RegistryWorker(DatabaseTreeRecord[] databaseSelections,DatabaseInformationModel srcDatabase,JPanel srcDatabasePanel,FTPRemoteHost remoteHost) {
			this.databaseSelections=databaseSelections;
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
			this.remoteHost=remoteHost;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			if (logger instanceof DialogMessageLogger) {
				((DialogMessageLogger)logger).setVisible(true);
			}
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Sending to Registry started"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Sending to Registry started"));
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			
			if (databaseSelections != null) {
				Vector candidatePaths = new Vector();
				for (DatabaseTreeRecord databaseSelection : databaseSelections) {
					if (databaseSelection != null) {
						recurseThroughChildrenGatheringFileNamesOfSRFiles(databaseSelection,candidatePaths);	// returns all SR files, not just dose-related
					}
				}
				Vector actualPaths = extractRadiationDoseSRFiles(candidatePaths);
				try {
					if (deidentifyWhenSendingToRegistryCheckBox.isSelected()) {
						actualPaths = deidentifyFiles(actualPaths);
					}
					String[] fileNamesToSend = (String[])(actualPaths.toArray(new String[actualPaths.size()]));
					new FTPFileSender(remoteHost,fileNamesToSend,true/*generate random remote file names*/,showDetailedLogCheckBox.isSelected() ? logger : null,progressBarUpdater.getProgressBar());
				}
				catch (Exception e) {
					if (showDetailedLogCheckBox.isSelected()) { logger.sendLn(e.toString()); }
					slf4jlogger.error("Send to registry failed",e);
				}
			}
			SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Sending to Registry complete"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending to Registry"));
			cursorChanger.restoreCursor();
		}
	}
	
	private String showInputDialogToSelectFTPTargetByLocalName(String message,String title,String defaultSelection) {
		String localName = defaultSelection;
		if (ftpRemoteHostInformation != null) {
			Set localNamesOfRemoteFTPHosts = ftpRemoteHostInformation.getListOfLocalNames();
			if (localNamesOfRemoteFTPHosts != null) {
				String sta[] = new String[localNamesOfRemoteFTPHosts.size()];
				int i=0;
				Iterator it = localNamesOfRemoteFTPHosts.iterator();
				while (it.hasNext()) {
					sta[i++]=(String)(it.next());
				}
				localName = (String)JOptionPane.showInputDialog(getContentPane(),message,title,JOptionPane.QUESTION_MESSAGE,null,sta,localName);
			}
		}
		return localName;
	}

	protected class RegistryActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				Properties properties = getProperties();
				String registryName = properties.getProperty(propertyName_CurrentlySelectedDoseRegistry);
				registryName = showInputDialogToSelectFTPTargetByLocalName("Select remote registry","Registry ...",registryName);
				if (registryName != null) {
					FTPRemoteHost remoteHost = ftpRemoteHostInformation.getRemoteHost(registryName);
					if (remoteHost != null) {
						activeThread = new Thread(new RegistryWorker(currentDatabaseTreeRecordSelections,srcDatabase,srcDatabasePanel,remoteHost));
						activeThread.start();
					}
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Send to registry failed: "+e));
				slf4jlogger.error("Send to registry failed",e);
			}
		}
	}
	
	protected class OurMediaImporter extends MediaImporter {
		public OurMediaImporter(MessageLogger logger,JProgressBar progressBar) {
			super(logger,progressBar);
		}
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			try {
				importFileIntoDatabase(srcDatabase,mediaFileName,DatabaseInformationModel.FILE_REFERENCED);
			}
			catch (Exception e) {
				slf4jlogger.error("Failed to import {} into database",mediaFileName,e);
			}
		}
		
		protected boolean canUseBzip = CapabilitiesAvailable.haveBzip2Support();

		// override base class isOKToImport(), which rejects unsupported compressed transfer syntaxes
		
		protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
			return sopClassUID != null
				&& (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
				&& transferSyntaxUID != null
				&& (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
				 || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)
				 || transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)
				 || (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian) && canUseBzip)
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
			importer = new OurMediaImporter(logger,progressBarUpdater.getProgressBar());
			this.srcDatabase=srcDatabase;
			this.srcDatabasePanel=srcDatabasePanel;
			this.pathName=pathName;
		}

		public void run() {
			cursorChanger.setWaitCursor();
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater);
			try {
				importer.importDicomFiles(pathName);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				slf4jlogger.error("Importing failed",e);
			}
			srcDatabasePanel.removeAll();
			try {
				new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: "+e));
				slf4jlogger.error("Refresh source database browser failed",e);
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
				if (chooser.showOpenDialog(DoseUtility.this.getContentPane()) == JFileChooser.APPROVE_OPTION) {
					importDirectoryPath=chooser.getCurrentDirectory().getAbsolutePath();	// keep around for next time
					String pathName = chooser.getSelectedFile().getAbsolutePath();
					new Thread(new ImportWorker(pathName,srcDatabase,srcDatabasePanel)).start();
				}
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
				slf4jlogger.error("Importing failed",e);
			}
		}
	}

	//public void osxFileHandler(String path) {
//System.err.println("DoseUtility.osxFileHandler(): path = "+path);
	//		try {
	//			if (logger instanceof DialogMessageLogger) {
	//				((DialogMessageLogger)logger).setVisible(true);
	//			}
	//			new Thread(new ImportWorker(path,srcDatabase,srcDatabasePanel)).start();
	//		} catch (Exception e) {
	//			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Importing failed: "+e));
	//			slf4jlogger.error("",e);
	//		}
	//}

	// should be extracted to a utility class ... also used in DoseReporterWithLegacyOCRAndAutoSendToRegistry :(
	public static String getQueryRetrieveAEFromIdentifier(AttributeList identifier,QueryInformationModel queryInformationModel) {
		String retrieveAE = null;
		if (identifier != null) {
			Attribute aRetrieveAETitle=identifier.get(TagFromName.RetrieveAETitle);
			if (aRetrieveAETitle != null) retrieveAE=aRetrieveAETitle.getSingleStringValueOrNull();
		}
		if (retrieveAE == null) {
			// it is legal for RetrieveAETitle to be zero length at all but the lowest levels of
			// the query model :( (See PS 3.4 C.4.1.1.3.2)
			// (so far the Leonardo is the only one that doesn't send it at all levels)
			// we could recurse down to the lower levels and get the union of the value there
			// but lets just keep it simple and ...
			// default to whoever it was we queried in the first place ...
			if (queryInformationModel != null) {
				retrieveAE=queryInformationModel.getCalledAETitle();
			}
		}
		return retrieveAE;
	}

	// should be extracted to a utility class ... also used in DoseReporterWithLegacyOCRAndAutoSendToRegistry :(
	public static String getQueryRetrieveLevel(AttributeList identifier,Attribute uniqueKey) {
		String level = null;
		if (identifier != null) {
			Attribute a = identifier.get(TagFromName.QueryRetrieveLevel);
			if (a != null) {
				level = a.getSingleStringValueOrNull();
			}
		}
		if (level == null) {
			// QueryRetrieveLevel must have been (erroneously) missing in query response ... see with Dave Harvey's code on public server
			// so try to guess it from unique key in tree record
			// Fixes [bugs.mrmf] (000224) Missing query/retrieve level in C-FIND response causes tree select and retrieve to fail
			if (uniqueKey != null) {
				AttributeTag tag = uniqueKey.getTag();
				if (tag != null) {
					if (tag.equals(TagFromName.PatientID)) {
						level="PATIENT";
					}
					else if (tag.equals(TagFromName.StudyInstanceUID)) {
						level="STUDY";
					}
					else if (tag.equals(TagFromName.SeriesInstanceUID)) {
						level="SERIES";
					}
					else if (tag.equals(TagFromName.SOPInstanceUID)) {
						level="IMAGE";
					}
				}
			}
//System.err.println("DoseUtility.getQueryRetrieveLevel(): Guessed missing Query Retrieve Level to be "+level);
		}
		return level;
	}
	
//	protected void setCurrentRemoteQuerySelection(AttributeList uniqueKeys,Attribute uniqueKey,AttributeList identifier) {
//System.err.println("DoseUtility.setCurrentRemoteQuerySelection(): uniqueKeys:\n"+uniqueKeys);
//System.err.println("DoseUtility.setCurrentRemoteQuerySelection(): uniqueKey: "+uniqueKey);
//System.err.println("DoseUtility.setCurrentRemoteQuerySelection(): identifier:\n"+identifier);
//		currentRemoteQuerySelectionUniqueKeys = uniqueKeys;
//		currentRemoteQuerySelectionUniqueKey  = uniqueKey;
//		currentRemoteQuerySelectionRetrieveAE = getQueryRetrieveAEFromIdentifier(identifier,currentRemoteQueryInformationModel);
//		currentRemoteQuerySelectionLevel      = getQueryRetrieveLevel(identifier,uniqueKey);
//	}

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
					//TreePath tp = tse.getNewLeadSelectionPath();
					//if (tp != null) {
					//	Object lastPathComponent = tp.getLastPathComponent();
					//	if (lastPathComponent instanceof QueryTreeRecord) {
					//		QueryTreeRecord r = (QueryTreeRecord)lastPathComponent;
					//		setCurrentRemoteQuerySelection(r.getUniqueKeys(),r.getUniqueKey(),r.getAllAttributesReturnedInIdentifier());
					//		currentRemoteQuerySelectionQueryTreeRecord=r;
					//	}
					//}
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
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Query to "+localName+" ("+calledAET+") starting"); }
			try {
				QueryTreeModel treeModel = currentRemoteQueryInformationModel.performHierarchicalQuery(filter);
				currentRemoteQueryTreeBrowser = new OurQueryTreeBrowser(currentRemoteQueryInformationModel,treeModel,remoteQueryRetrievePanel);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying "+localName));
			} catch (Exception e) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Query to "+localName+" failed "+e));
				if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Query to "+localName+" ("+calledAET+") failed due to"+ e); }
				slf4jlogger.error("Query to {} failed",calledAET,e);
			}
			if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Query to "+localName+" ("+calledAET+") complete"); }
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done querying  "+localName));
			cursorChanger.restoreCursor();
		}
	}

	protected class QueryActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			//new QueryRetrieveDialog("DoseUtility Query",400,512);
			Properties properties = getProperties();
			String ae = properties.getProperty(propertyName_DicomCurrentlySelectedQueryTargetAE);
			ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select remote system","Query ...",ae);
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
							AttributeTag t = TagFromName.PatientID; Attribute a = new ShortStringAttribute(t,specificCharacterSet);
							String patientID = queryFilterPatientIDTextField.getText().trim();
							if (patientID != null && patientID.length() > 0) {
								a.addValue(patientID);
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
						{ AttributeTag t = TagFromName.Manufacturer; Attribute a = new LongStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.SeriesTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }

						{ AttributeTag t = TagFromName.InstanceNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ContentTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.NumberOfFrames; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.WindowCenter; Attribute a = new DecimalStringAttribute(t); filter.put(t,a); }
						{ AttributeTag t = TagFromName.WindowWidth; Attribute a = new DecimalStringAttribute(t); filter.put(t,a); }

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
			slf4jlogger.error("Retrieve failed",e);
		}
	}
	
	// really should move this to another utility class ... used in DoseReporterWithLegacyOCRAndAutoSendToRegistry
	public static ArrayList<QueryTreeRecord> findDoseSeriesRecordsInQueryTree(QueryTreeRecord record,ArrayList<QueryTreeRecord> records) {
		boolean recurse = true;
		if (record != null) {
//System.err.println("DoseUtility.findDoseSeriesRecordsInQueryTree(): Testing "+record);
			AttributeList list = record.getAllAttributesReturnedInIdentifier();
//System.err.print(list);
			InformationEntity ie = record.getInformationEntity();
			if (ie != null && list != null) {
				// use same logic here as when filtering database file list ...
				if (ie.equals(InformationEntity.SERIES)) {
					if (OCR.isPossiblyDoseScreenSeries(list)
					 || ExposureDoseSequence.isPossiblyPhilipsDoseScreenSeries(list)
					 || Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality).equals("SR")		// potential SR Dose Report (may retrieve many other types of SR, but worth it since will get prototype SR and those from PACS that don't support proper SOP Class))
					) {
//System.err.println("DoseUtility.findDoseSeriesRecordsInQueryTree(): Found at SERIES level "+record);
						records.add(record);
						recurse = false;	// don't recurse if entire series already added (else retrieves things twice)
					}
				}
				else if (ie.equals(InformationEntity.INSTANCE)) {
					String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
					if (OCR.isPossiblyDoseScreenInstance(list)
					 || ExposureDoseSequence.isPossiblyPhilipsDoseScreenInstance(list)
					 || sopClassUID.equals(SOPClass.XRayRadiationDoseSRStorage)
					 || sopClassUID.equals(SOPClass.EnhancedSRStorage)
					 || sopClassUID.equals(SOPClass.ComprehensiveSRStorage)
					) {
//System.err.println("DoseUtility.findDoseSeriesRecordsInQueryTree(): Found at IMAGE level "+record);
						records.add(record);
						recurse = false;	// don't recurse below instance level
					}
				}
			}
			if (recurse) {
				Enumeration children = record.children();
				if (children != null) {
					while (children.hasMoreElements()) {
						record = (QueryTreeRecord)(children.nextElement());
						if (record != null) {
							findDoseSeriesRecordsInQueryTree(record,records);
						}
					}
				}
			}
		}
		return records;
	}
	
	// really should move this to another utility class
	public static ArrayList<QueryTreeRecord> findCTSeriesAndRelatedRecordsInQueryTree(QueryTreeRecord record,ArrayList<QueryTreeRecord> records) {
		boolean recurse = true;
		if (record != null) {
			InformationEntity ie = record.getInformationEntity();
			if (ie != null) {
				if (ie.equals(InformationEntity.SERIES)) {
					AttributeList list = record.getAllAttributesReturnedInIdentifier();
					if (list != null) {
						String modality = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality);
						if (modality.equals("CT") || modality.equals("SR")) {
//System.err.println("DoseUtility.findCTSeriesAndRelatedRecordsInQueryTree(): Found "+record);
							records.add(record);
						}
					}
					recurse = false;	// don't recurse below series level when series or above was selected
				}
				else if (ie.equals(InformationEntity.INSTANCE)) {
					// user must have explicitly selected an instance (or couldn't have got here past series check above), so add it regardless
					records.add(record);
					recurse = false;	// don't recurse below instance level
				}
			}
			if (recurse) {
				Enumeration children = record.children();
				if (children != null) {
					while (children.hasMoreElements()) {
						record = (QueryTreeRecord)(children.nextElement());
						if (record != null) {
							findCTSeriesAndRelatedRecordsInQueryTree(record,records);
						}
					}
				}
			}
		}
		return records;
	}
	
	protected class RetrieveActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			cursorChanger.setWaitCursor();
			ArrayList<QueryTreeRecord> records = new ArrayList<QueryTreeRecord>();
			if (currentRemoteQueryTreeBrowser != null) {
				for (QueryTreeRecord candidateRecord : currentRemoteQueryTreeBrowser.getSelectionPaths()) {
					if (retrieveOnlyDoseSeriesRecordCheckBox.isSelected()) {
						records.addAll(findDoseSeriesRecordsInQueryTree(candidateRecord,new ArrayList<QueryTreeRecord>()));
					}
					else {
						records.addAll(findCTSeriesAndRelatedRecordsInQueryTree(candidateRecord,new ArrayList<QueryTreeRecord>()));
					}
				}
			}
			{
				if (records != null) {
					for (QueryTreeRecord r : records) {
						AttributeList uniqueKeys = r.getUniqueKeys();
						Attribute uniqueKey      = r.getUniqueKey();
						AttributeList identifier = r.getAllAttributesReturnedInIdentifier();
//System.err.println("DoseUtility.RetrieveActionListener.actionPerformed(): uniqueKeys:\n"+uniqueKeys);
//System.err.println("DoseUtility.RetrieveActionListener.actionPerformed(): uniqueKey: "+uniqueKey);
//System.err.println("DoseUtility.RetrieveActionListener.actionPerformed(): identifier:\n"+identifier);
						String ae                = getQueryRetrieveAEFromIdentifier(identifier,currentRemoteQueryInformationModel);
						String localName         = networkApplicationInformation.getLocalNameFromApplicationEntityTitle(ae);
						String level             = getQueryRetrieveLevel(identifier,uniqueKey);
						
						ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Retrieving "+level+" "+uniqueKey.getSingleStringValueOrEmptyString()+" from "+localName));
						if (showDetailedLogCheckBox.isSelected()) { logger.sendLn("Request retrieval of "+level+" "+uniqueKey.getSingleStringValueOrEmptyString()+" from "+localName+" ("+ae+")"); }
						performRetrieve(uniqueKeys,level,ae);
					}
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done sending retrieval request"));
				}
			}
			cursorChanger.restoreCursor();
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
				storageSOPClassSCPDispatcher.shutdown();
				new NetworkApplicationConfigurationDialog(DoseUtility.this.getContentPane(),networkApplicationInformation,networkApplicationProperties);
				new FTPClientApplicationConfigurationDialog(DoseUtility.this.getContentPane(),ftpRemoteHostInformation,ftpApplicationProperties);
				networkApplicationProperties.getProperties(getProperties());
				ftpApplicationProperties.getProperties(getProperties());
				storeProperties("Edited and saved from user interface");
				//getProperties().store(System.err,"Bla");
				activateStorageSCP();
			} catch (Exception e) {
				slf4jlogger.error("Configure failed",e);
			}
		}
	}
	
	Thread activeThread;
	
	// Based on Apple's MyApp.java example supplied with OSXAdapter ...
	// Generic registration with the Mac OS X application menu
	// Checks the platform, then attempts to register with the Apple EAWT
	// See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
	//public void registerForMacOSXEvents() {
	//	if (System.getProperty("os.name").toLowerCase(java.util.Locale.US).startsWith("mac os x")) {
//System.err.println("DicomImageViewer.registerForMacOSXEvents(): on MacOSX");
	//		try {
	//			// Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
	//			// use as delegates for various com.apple.eawt.ApplicationListener methods
	//			OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));		// need this, else won't quite from X or Cmd-Q any more, once any events registered
	//			//OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[])null));
	//			//OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
	//			OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("osxFileHandler", new Class[] { String.class }));
	//		} catch (NoSuchMethodException e) {
	//			// trap it, since we don't want to fail just because we cannot register events
	//			slf4jlogger.error("",e);
	//		}
	//	}
	//}

	public DoseUtility(String title) throws DicomException, IOException {
		super(title,propertiesFileName);
		activateTemporaryDatabases();
		savedImagesFolder = new File(System.getProperty("java.io.tmpdir"));
		
		try {
			networkApplicationProperties = new NetworkApplicationProperties(getProperties(),true/*addPublicStorageSCPsIfNoRemoteAEsConfigured*/);
		}
		catch (Exception e) {
			networkApplicationProperties = null;
		}
		{
			NetworkApplicationInformationFederated federatedNetworkApplicationInformation = new NetworkApplicationInformationFederated();
			federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
			networkApplicationInformation = federatedNetworkApplicationInformation;
//System.err.print("networkApplicationInformation ...\n"+networkApplicationInformation);
		}
		activateStorageSCP();

		try {
			ftpApplicationProperties = new FTPApplicationProperties(getProperties());
		}
		catch (Exception e) {
			ftpApplicationProperties = null;
		}
		if (ftpApplicationProperties != null) {
			ftpRemoteHostInformation = ftpApplicationProperties.getFTPRemoteHostInformation();
//System.err.print("ftpRemoteHostInformation ...\n"+ftpRemoteHostInformation);
		}

		logger = new DialogMessageLogger(loggerTitleMessage,loggertDialogWidthWanted,loggerDialogHeightWanted,false/*exitApplicationOnClose*/,false/*visible*/);

		cursorChanger = new SafeCursorChanger(this);

		// ShutdownHook will run regardless of whether Command-Q (on Mac) or window closed ...
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
//System.err.println("DoseUtility.ShutdownHook.run()");
				if (networkApplicationInformation != null && networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
					((NetworkApplicationInformationFederated)networkApplicationInformation).removeAllSources();
				}
//System.err.print(TransferMonitor.report());
			}
		});

		//registerForMacOSXEvents();

		srcDatabasePanel = new JPanel();
		remoteQueryRetrievePanel = new JPanel();
	
		srcDatabasePanel.setLayout(new GridLayout(1,1));
		remoteQueryRetrievePanel.setLayout(new GridLayout(1,1));
		
		DatabaseTreeBrowser srcDatabaseTreeBrowser = new OurSourceDatabaseTreeBrowser(srcDatabase,srcDatabasePanel);

		Border panelBorder = BorderFactory.createEtchedBorder();

		JSplitPane remoteAndLocalBrowserPanes = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,remoteQueryRetrievePanel,srcDatabasePanel);
		remoteAndLocalBrowserPanes.setOneTouchExpandable(true);
		remoteAndLocalBrowserPanes.setResizeWeight(0.5);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBorder(panelBorder);
		
		JButton configureButton = new JButton(configureButtonLabel);
		configureButton.setToolTipText(configureButtonToolTipText);
		buttonPanel.add(configureButton);
		configureButton.addActionListener(new ConfigureActionListener());
		
		JButton logButton = new JButton(logButtonLabel);
		logButton.setToolTipText(logButtonToolTipText);
		buttonPanel.add(logButton);
		logButton.addActionListener(new LogActionListener());
		
		JButton queryButton = new JButton(queryButtonLabel);
		queryButton.setToolTipText(queryButtonToolTipText);
		buttonPanel.add(queryButton);
		queryButton.addActionListener(new QueryActionListener());
		
		JButton retrieveButton = new JButton(retrieveButtonLabel);
		retrieveButton.setToolTipText(retrieveButtonToolTipText);
		buttonPanel.add(retrieveButton);
		retrieveButton.addActionListener(new RetrieveActionListener());
		
		JButton importButton = new JButton(importButtonLabel);
		importButton.setToolTipText(importButtonToolTipText);
		buttonPanel.add(importButton);
		importButton.addActionListener(new ImportActionListener());
		
		JButton viewButton = new JButton(viewButtonLabel);
		viewButton.setToolTipText(viewButtonToolTipText);
		buttonPanel.add(viewButton);
		viewButton.addActionListener(new ViewActionListener());
		
		JButton validateButton = new JButton(validateButtonLabel);
		validateButton.setToolTipText(validateButtonToolTipText);
		buttonPanel.add(validateButton);
		validateButton.addActionListener(new ValidateActionListener());
		
		JButton reportButton = new JButton(reportButtonLabel);
		reportButton.setToolTipText(reportButtonToolTipText);
		buttonPanel.add(reportButton);
		reportButton.addActionListener(new ReportActionListener());
		
		JButton makeSRButton = new JButton(makeSRButtonLabel);
		makeSRButton.setToolTipText(makeSRButtonToolTipText);
		buttonPanel.add(makeSRButton);
		makeSRButton.addActionListener(new MakeSRActionListener());
		
		JButton sendButton = new JButton(sendButtonLabel);
		sendButton.setToolTipText(sendButtonToolTipText);
		buttonPanel.add(sendButton);
		sendButton.addActionListener(new SendActionListener());
		
		JButton registryButton = new JButton(registryButtonLabel);
		registryButton.setToolTipText(registryButtonToolTipText);
		buttonPanel.add(registryButton);
		registryButton.addActionListener(new RegistryActionListener());
		
		JButton purgeButton = new JButton(purgeButtonLabel);
		purgeButton.setToolTipText(purgeButtonToolTipText);
		buttonPanel.add(purgeButton);
		purgeButton.addActionListener(new PurgeActionListener());
				
		JPanel queryFilterTextEntryPanel = new JPanel();
		queryFilterTextEntryPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		queryFilterTextEntryPanel.setBorder(panelBorder);

		JLabel queryIntroduction = new JLabel(queryIntroductionLabelText);
		queryFilterTextEntryPanel.add(queryIntroduction);

		JLabel queryFilterPatientNameLabel = new JLabel(queryPatientNameLabelText);
		queryFilterPatientNameLabel.setToolTipText(queryPatientNameToolTipText);
		queryFilterTextEntryPanel.add(queryFilterPatientNameLabel);
		queryFilterPatientNameTextField = new JTextField("",textFieldLengthForQueryPatientName);
		queryFilterTextEntryPanel.add(queryFilterPatientNameTextField);
		
		JLabel queryFilterPatientIDLabel = new JLabel(queryPatientIDLabelText);
		queryFilterPatientIDLabel.setToolTipText(queryPatientIDToolTipText);
		queryFilterTextEntryPanel.add(queryFilterPatientIDLabel);
		queryFilterPatientIDTextField = new JTextField("",textFieldLengthForQueryPatientID);
		queryFilterTextEntryPanel.add(queryFilterPatientIDTextField);
		
		JLabel queryFilterStudyDateLabel = new JLabel(queryStudyDateLabelText);
		queryFilterStudyDateLabel.setToolTipText(queryStudyDateToolTipText);
		queryFilterTextEntryPanel.add(queryFilterStudyDateLabel);
		queryFilterStudyDateTextField = new JTextField("",textFieldLengthForQueryStudyDate);
		queryFilterTextEntryPanel.add(queryFilterStudyDateTextField);
		
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new GridLayout(2,0));	// only one dimension has any effect
		checkBoxPanel.setBorder(panelBorder);
				
		retrieveOnlyDoseSeriesRecordCheckBox = new JCheckBox(retrieveOnlyDoseSeriesRecordLabelText);
		retrieveOnlyDoseSeriesRecordCheckBox.setSelected(true);
		checkBoxPanel.add(retrieveOnlyDoseSeriesRecordCheckBox);
				
		processOnlyDoseSeriesRecordCheckBox = new JCheckBox(processOnlyDoseSeriesRecordLabelText);
		processOnlyDoseSeriesRecordCheckBox.setSelected(true);
		checkBoxPanel.add(processOnlyDoseSeriesRecordCheckBox);

		showOnlyDoseSummaryCheckBox = new JCheckBox(showOnlyDoseSummaryLabelText);
		showOnlyDoseSummaryCheckBox.setSelected(true);
		checkBoxPanel.add(showOnlyDoseSummaryCheckBox);

		showDetailedLogCheckBox = new JCheckBox(showDetailedLogLabelText);
		showDetailedLogCheckBox.setSelected(false);
		checkBoxPanel.add(showDetailedLogCheckBox);

		reportTabularLayoutCheckBox = new JCheckBox(reportTabularLayoutLabelText);
		reportTabularLayoutCheckBox.setSelected(true);
		checkBoxPanel.add(reportTabularLayoutCheckBox);

		caseSensitiveCodeMeaningValidationCheckBox = new JCheckBox(caseSensitiveCodeMeaningValidationLabelText);
		caseSensitiveCodeMeaningValidationCheckBox.setSelected(true);
		checkBoxPanel.add(caseSensitiveCodeMeaningValidationCheckBox);

		deidentifyWhenSendingToRegistryCheckBox = new JCheckBox(deidentifyWhenSendingToRegistryLabelText);
		deidentifyWhenSendingToRegistryCheckBox.setSelected(true);
		checkBoxPanel.add(deidentifyWhenSendingToRegistryCheckBox);
		
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
				GridBagConstraints remoteAndLocalBrowserPanesConstraints = new GridBagConstraints();
				remoteAndLocalBrowserPanesConstraints.gridx = 0;
				remoteAndLocalBrowserPanesConstraints.gridy = 0;
				remoteAndLocalBrowserPanesConstraints.weightx = 1;
				remoteAndLocalBrowserPanesConstraints.weighty = 1;
				remoteAndLocalBrowserPanesConstraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(remoteAndLocalBrowserPanes,remoteAndLocalBrowserPanesConstraints);
				mainPanel.add(remoteAndLocalBrowserPanes);
			}
			{
				GridBagConstraints buttonPanelConstraints = new GridBagConstraints();
				buttonPanelConstraints.gridx = 0;
				buttonPanelConstraints.gridy = 1;
				buttonPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(buttonPanel,buttonPanelConstraints);
				mainPanel.add(buttonPanel);
			}
			{
				GridBagConstraints queryFilterTextEntryPanelConstraints = new GridBagConstraints();
				queryFilterTextEntryPanelConstraints.gridx = 0;
				queryFilterTextEntryPanelConstraints.gridy = 2;
				queryFilterTextEntryPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(queryFilterTextEntryPanel,queryFilterTextEntryPanelConstraints);
				mainPanel.add(queryFilterTextEntryPanel);
			}
			{
				GridBagConstraints checkBoxPanelConstraints = new GridBagConstraints();
				checkBoxPanelConstraints.gridx = 0;
				checkBoxPanelConstraints.gridy = 3;
				checkBoxPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(checkBoxPanel,checkBoxPanelConstraints);
				mainPanel.add(checkBoxPanel);
			}
			{
				GridBagConstraints statusBarPanelConstraints = new GridBagConstraints();
				statusBarPanelConstraints.gridx = 0;
				statusBarPanelConstraints.gridy = 4;
				statusBarPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(statusBarPanel,statusBarPanelConstraints);
				mainPanel.add(statusBarPanel);
			}
		}
		Container content = getContentPane();
		content.add(mainPanel);
		pack();
		setVisible(true);
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	none
	 */
	public static void main(String arg[]) {
		try {
			String osName = System.getProperty("os.name");
			if (osName != null && osName.toLowerCase(java.util.Locale.US).startsWith("windows")) {	// see "http://lopica.sourceforge.net/os.html" for list of values
				System.err.println("DoseUtility.main(): detected Windows - using Windows LAF");	// no need to use SLF4J since command line utility/test
				javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		try {
			new DoseUtility("Dose Utility");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

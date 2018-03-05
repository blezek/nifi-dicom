/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;		// used in main method for testing
import com.pixelmed.utils.ThreadUtilities;
import com.pixelmed.display.DialogMessageLogger;		// used in main method for testing
import com.pixelmed.display.SafeFileChooser;
import com.pixelmed.display.SafeProgressBarUpdaterThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.awt.Component;
import java.awt.FileDialog;

import javax.swing.JFileChooser;
import javax.swing.JProgressBar;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is designed to support the importation of DICOM files from
 * interchange media (such as CDs and DVDs).</p>
 * 
 * <p>It supports locating a DICOMDIR file and iterating through the list of
 * referenced files.</p>
 * 
 * <p>The actual work (e.g. to import the file into a database or similar) is
 * performed by the implementation of the {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia}
 * method in a sub-class of this class.</p>
 *
 * @see com.pixelmed.database.DatabaseMediaImporter
 * 
 * @author	dclunie
 */
public class MediaImporter {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/MediaImporter.java,v 1.20 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MediaImporter.class);		// cannot use normal name "logger" since already published as protected attribute for MessageLogger

	protected String mediaDirectoryPath;
	protected MessageLogger logger;
	protected JProgressBar progressBar;
	
	/**
	 * @param	s	message to log
	 */
	protected void logLn(String s) {
		if (logger != null) {
			logger.sendLn(s);
		}
		//System.err.println(s);
	}
		
	/**
	 * <p>Construct an importer that will looked for files in the system default path.</p>
	 *
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 */
	public MediaImporter(MessageLogger logger) {
		mediaDirectoryPath=null;
		this.logger=logger;
		this.progressBar=null;
	}
	
	/**
	 * <p>Construct an importer that will looked for files in the system default path.</p>
	 *
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 * @param	progressBar		where to update progress as files are read (may be null for no progress bar)
	 */
	public MediaImporter(MessageLogger logger,JProgressBar progressBar) {
		this.mediaDirectoryPath=null;
		this.logger=logger;
		this.progressBar=progressBar;
	}

	/**
	 * <p>Construct an importer that will looked for files in the specified path.</p>
	 *
	 * @param	mediaDirectoryPath	where to begin looking for the DICOMDIR and DICOM files
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 */
	public MediaImporter(String mediaDirectoryPath,MessageLogger logger) {
		this.mediaDirectoryPath=mediaDirectoryPath;
		this.logger=logger;
		this.progressBar=null;
	}
	
	/**
	 * <p>Construct an importer that will looked for files in the specified path.</p>
	 *
	 * @param	mediaDirectoryPath	where to begin looking for the DICOMDIR and DICOM files
	 * @param	logger			where to send status updates as files are read (may be null for no logging)
	 * @param	progressBar		where to update progress as files are read (may be null for no progress bar)
	 */
	public MediaImporter(String mediaDirectoryPath,MessageLogger logger,JProgressBar progressBar) {
		this.mediaDirectoryPath=mediaDirectoryPath;
		this.logger=logger;
		this.progressBar=progressBar;
	}

	/**
	 * <p>Pop up a file chooser dialog that allows the user to specify the location of
	 * the DICOMDIR file, or the parent folder (for example, the drive or volume) in which
	 * the DICOMDIR file is located, and then import the referenced files.</p>
	 *
	 * <p>Will be positioned relative to the parent component (for example, centered over the component)
	 * if specified, else placed in a look-and-feel-dependent position such as the center of the screen if null.</p>
	 *
	 * <p>Can only be invoked on the AWT Event Dispatch Thread.</p>
	 *
	 * @param		parent			the parent component of the dialog; can be <code>null</code>
	 * @throws		IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @throws		DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void choosePathAndImportDicomFiles(Component parent) throws IOException, DicomException {
		String pathName = null;
		
		//System.err.println("java.version = "+System.getProperty("java.version"));
		//System.err.println("os.name = "+System.getProperty("os.name"));
		//boolean useJFileChooser = !(System.getProperty("java.version").startsWith("1.7.") && System.getProperty("os.name").equals("Mac OS X"));
		boolean useJFileChooser = true;

		if (useJFileChooser) {
			slf4jlogger.info("choosePathAndImportDicomFiles(): using JFileChooser because not broken Java 7 on Mac OS X");
//System.err.println("MediaImporter.choosePathAndImportDicomFiles(): about to construct JFileChooser");
			SafeFileChooser chooser = new SafeFileChooser(mediaDirectoryPath);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
//System.err.println("MediaImporter.choosePathAndImportDicomFiles(): about to chooser.showOpenDialog");
			if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
//System.err.println("MediaImporter.choosePathAndImportDicomFiles(): back with APPROVE_OPTION");
				mediaDirectoryPath=chooser.getCurrentDirectory().getAbsolutePath();	// keep around for next time
				pathName = chooser.getSelectedFile().getAbsolutePath();
			}
		}
		else {
			slf4jlogger.info("choosePathAndImportDicomFiles(): using FileDialog instead of JFileChooser because broken on Java 7 on Mac OS X");
			FileDialog chooser = new FileDialog(parent instanceof java.awt.Frame ? (java.awt.Frame)parent : null,"Import", FileDialog.LOAD);
			System.setProperty("apple.awt.fileDialogForDirectories","true");
			chooser.setDirectory(mediaDirectoryPath);
			chooser.setVisible(true);
			mediaDirectoryPath = chooser.getDirectory();	// keep around for next time
			pathName = new File(mediaDirectoryPath,chooser.getFile()).getAbsolutePath();	// since getFile() returns relative path only
			System.setProperty("apple.awt.fileDialogForDirectories","false");
		}
//System.err.println("MediaImporter.choosePathAndImportDicomFiles(): mediaDirectoryPath = "+mediaDirectoryPath);
//System.err.println("MediaImporter.choosePathAndImportDicomFiles(): pathName = "+pathName);
		if (pathName != null) {
			importDicomFiles(pathName);
		}
	}

	/**
	 * <p>Pop up a file chooser dialog that allows the user to specify the location of
	 * the DICOMDIR file, or the parent folder (for example, the drive or volume) in which
	 * the DICOMDIR file is located, and then import the referenced files.</p>
	 *
	 * <p>Will be placed in a look-and-feel-dependent position such as the center of the screen.</p>
	 *
	 * <p>Can only be invoked on the AWT Event Dispatch Thread.</p>
	 *
	 * @throws		IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @throws		DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void choosePathAndImportDicomFiles() throws IOException, DicomException {
		choosePathAndImportDicomFiles(null);
	}
	
	/**
	 * <p>Check for valid information, and that the file is not compressed or not a suitable storage object for import.</p>
	 *
	 * @param	sopClassUID			the SOP Class UID of the file
	 * @param	transferSyntaxUID	the Transfer Syntax UID of the file
	 * @return						true if is suitable
	 */
	protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
		return sopClassUID != null
		    && (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && ! SOPClass.isDirectory(sopClassUID)))
		    && transferSyntaxUID != null
		    && (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
		     || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
		     || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian));
	}

	protected SafeProgressBarUpdaterThread progressBarUpdater;

	// copied from SynchronizeFromRemoteSCP ... should refactor :(
	protected static class OurReadTerminationStrategy implements AttributeList.ReadTerminationStrategy {
		public boolean terminate(AttributeList attributeList,AttributeTag tag,long byteOffset) {
			return tag.getGroup() > 0x0008;
		}
	}
	
	protected final static AttributeList.ReadTerminationStrategy terminateAfterIdentifyingGroup = new OurReadTerminationStrategy();
	
	/**
	 * <p>Read a DICOMDIR file, and then import any DICOM files that it references.</p>
	 *
	 * <p>How errors are handled during the importation of the referenced files
	 * depends on the implementation of {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia}
	 * in the sub-class. Any such errors will not interrupt the execution of this method (i.e., will not prevent the
	 * importation of the remaining files).</p>
	 *
	 * @param	pathName		the path name to a DICOMDIR file or folder containing a DICOMDIR file
	 *
	 * @throws		IOException		thrown if the DICOMDIR file (but not any referenced files) cannot be opened or read
	 * @throws		DicomException		thrown if the DICOMDIR file cannot be parsed
	 */
	public void importDicomFiles(String pathName) throws IOException, DicomException {
		if (progressBar != null) {
			//ThreadUtilities.checkIsEventDispatchThreadElseException();
			progressBarUpdater = new SafeProgressBarUpdaterThread(progressBar);
		}
		if (pathName != null) {
			File path = new File(pathName);
			File dicomdirFile = null;		// look for DICOMDIR here or in root folder of here, with various case permutations
			if (path != null && path.exists()) {
				if (path.isFile() && path.getName().toUpperCase(java.util.Locale.US).equals("DICOMDIR")) {
					dicomdirFile=path;
				}
				else if (path.isDirectory()) {
					File tryFile = new File(path,"DICOMDIR");
					if (tryFile != null && tryFile.exists()) {
						dicomdirFile=tryFile;
					}
					else {
						tryFile = new File(path,"Dicomdir");
						if (tryFile != null && tryFile.exists()) {
							dicomdirFile=tryFile;
						}
						else {
							tryFile = new File(path,"dicomdir");
							if (tryFile != null && tryFile.exists()) {
								dicomdirFile=tryFile;
							}
							// else give up
						}
					}
				}
			}
			if (dicomdirFile != null) {
				logLn("Found DICOMDIR at: "+dicomdirFile);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(dicomdirFile)));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				DicomDirectory dicomDirectory = new DicomDirectory(list);
				HashMap allDicomFiles = dicomDirectory.findAllContainedReferencedFileNamesAndTheirRecords(dicomdirFile.getParentFile().getPath());
//System.err.println("Referenced files: "+allDicomFiles);
				if (progressBarUpdater != null) {
					progressBarUpdater.setValue(0);
					progressBarUpdater.setMaximum(allDicomFiles.size());
					progressBarUpdater.setStringPainted(true);
					java.awt.EventQueue.invokeLater(progressBarUpdater);
				}
				int count = 0;
				Iterator it = allDicomFiles.keySet().iterator();
				while (it.hasNext()) {
					String mediaFileName = (String)it.next();
					if (mediaFileName != null) {
						boolean goodToGo = false;
						String sopClassUID = "";
						String transferSyntaxUID = "";
						DicomDirectoryRecord record = (DicomDirectoryRecord)(allDicomFiles.get(mediaFileName));
						if (record != null) {
							AttributeList rlist = ((DicomDirectoryRecord)record).getAttributeList();
							if (rlist != null) {
								sopClassUID = Attribute.getSingleStringValueOrNull(rlist,TagFromName.ReferencedSOPClassUIDInFile);
								transferSyntaxUID = Attribute.getSingleStringValueOrNull(rlist,TagFromName.ReferencedTransferSyntaxUIDInFile);
								if (sopClassUID == null || transferSyntaxUID == null) {
									// the directory record is invalid; these should be present
									// don't give up though ... try reading the meta-information header ...
									try {
										DicomInputStream di = new DicomInputStream(new BufferedInputStream(new FileInputStream(mediaFileName)));
										if (di.haveMetaHeader()) {
											AttributeList dlist = new AttributeList();
											dlist.readOnlyMetaInformationHeader(di);
											// Don't replace them unless they were null; they might be missing in the meta header !
											if (sopClassUID == null) {
												sopClassUID = Attribute.getSingleStringValueOrNull(dlist,TagFromName.MediaStorageSOPClassUID);
											}
											if (transferSyntaxUID == null) {
												transferSyntaxUID = Attribute.getSingleStringValueOrNull(dlist,TagFromName.TransferSyntaxUID);
											}
										}
										di.close();
									}
									catch (Exception e) {
										// ignore the error ... will fail on null sopClassUID or transferSyntaxUID
									}
								}
								if (isOKToImport(sopClassUID,transferSyntaxUID)) {
									goodToGo=true;
								}
								else {
									logLn("Is a DICOM file but bad meta-header, not a storage object, or is compressed: "
										+mediaFileName+" SOP Class="+sopClassUID+", Transfer Syntax="+transferSyntaxUID);
								}
							}
						}
						if (goodToGo) {
							//logLn("Is a suitable DICOMDIR referenced file: "+mediaFileName);
							doSomethingWithDicomFileOnMedia(mediaFileName,transferSyntaxUID,sopClassUID);
						}
						else {
							//logLn("Not a suitable DICOMDIR referenced file: "+mediaFileName);
							doSomethingWithUnwantedFileOnMedia(mediaFileName,transferSyntaxUID,sopClassUID);
						}
					}
					++count;
					if (progressBarUpdater != null) {
						progressBarUpdater.setValue(count);
						progressBarUpdater.setStringPainted(true);
						java.awt.EventQueue.invokeLater(progressBarUpdater);
					}
				}
			}
			else {
				ArrayList listOfAllFiles = FileUtilities.listFilesRecursively(path);
				if (progressBarUpdater != null) {
					progressBarUpdater.setValue(0);
					progressBarUpdater.setMaximum(listOfAllFiles.size());
					progressBarUpdater.setStringPainted(true);
					java.awt.EventQueue.invokeLater(progressBarUpdater);
				}
				int count = 0;
				Iterator it = listOfAllFiles.iterator();
				while (it.hasNext()) {
					File mediaFile = (File)it.next();
					if (mediaFile != null) {
						// It might or might not be a DICOM file ... only way to tell is to try it
						try {
							String sopClassUID = "";
							String transferSyntaxUID = "";
							DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(mediaFile)));
							boolean goodToGo = false;
							if (i.haveMetaHeader()) {
								AttributeList list = new AttributeList();
								list.readOnlyMetaInformationHeader(i);
								sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.MediaStorageSOPClassUID);
								transferSyntaxUID = Attribute.getSingleStringValueOrNull(list,TagFromName.TransferSyntaxUID);
								if (sopClassUID == null || sopClassUID.length() == 0) {		// work around incomplete meta information header if GE PACS internal files (000905)
									logLn("Have meta information header, but missing SOP Class UID so reading from data set: "+mediaFile);
									list.read(i,terminateAfterIdentifyingGroup);
									sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
								}
								if (isOKToImport(sopClassUID,transferSyntaxUID)) {
									goodToGo=true;
								}
								else {
									logLn("Is a DICOM file but bad meta-header, not a storage object, or is compressed with a scheme that is not supported: "
										+mediaFile+" SOP Class="+sopClassUID+", Transfer Syntax="+transferSyntaxUID);
								}
							}
							else {
								// no meta information header ... assume default Transfer Syntax and try to read SOP Class IOD ...
								logLn("No meta information header, so guessing is a DICOM file in implicit VR and attempting to get SOP Class UID: "+mediaFile);
								AttributeList list = new AttributeList();
								list.read(i,terminateAfterIdentifyingGroup);
								sopClassUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
								transferSyntaxUID = TransferSyntax.ImplicitVRLittleEndian;
								if (isOKToImport(sopClassUID,transferSyntaxUID)) {
									goodToGo=true;
								}
								else {
									logLn("Is not a DICOM file, or not a storage object, or is compressed with a scheme that is not supported: "
										+mediaFile+" SOP Class="+sopClassUID+", Transfer Syntax="+transferSyntaxUID);
								}
							}
							i.close();	// do this BEFORE calling the handler, just in case
							if (goodToGo) {
								//logLn("Is a DICOM file that is wanted: "+mediaFile);
								doSomethingWithDicomFileOnMedia(mediaFile.getPath(),transferSyntaxUID,sopClassUID);
							}
							else {
								//logLn("Not a DICOM PS 3.10 file or not one that is wanted: "+mediaFile);
								doSomethingWithUnwantedFileOnMedia(mediaFile.getPath(),transferSyntaxUID,sopClassUID);
							}
						}
						catch (Exception e) {
							//logLn("Not a DICOM file: "+mediaFile);
							doSomethingWithUnwantedFileOnMedia(mediaFile.getPath(),"","");
						}
					}
					++count;
					if (progressBarUpdater != null) {
						progressBarUpdater.setValue(count);
						progressBarUpdater.setStringPainted(true);
						java.awt.EventQueue.invokeLater(progressBarUpdater);
					}
				}
			}
		}
		logLn("Media import complete");
	}
	
	/**
	 * <p>Do something with the unwanted (possibly DICOM file) that has been encountered.</p>
	 *
	 * <p>This method needs to be implemented in a sub-class to do anything useful.
	 * The default method does nothing.</p>
	 *
	 * <p>"Unwanted" files are those that are not DICOM files or DICOM files for which {@link MediaImporter#isOKToImport(String,String) isOKToImport(String sopClassUID,String transferSyntaxUID)} returns false.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName		the fully qualified path name to a DICOM file
	 * @param	transferSyntaxUID	the Transfer Syntax of the Data Set if a DICOM file, from the DICOMDIR or Meta Information Header
	 * @param	sopClassUID			the SOP Class of the Data Set if a DICOM file, from the DICOMDIR or Meta Information Header
	 */
	protected void doSomethingWithUnwantedFileOnMedia(String mediaFileName,String transferSyntaxUID,String sopClassUID) {
		logLn("Not a DICOM file, not a DICOM PS 3.10 file or not one that is wanted: "+mediaFileName);
	}
	
	/**
	 * <p>Do something with the referenced DICOM file that has been encountered.</p>
	 *
	 * <p>This method may be implemented in a sub-class to do something useful that requires knowledge of the Transfer Syntax or SOP Class.
	 * The default method calls the simpler method {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia(String mediaFileName)}.</p>
	 *
	 * <p>"Wanted" files are those that are DICOM files for which {@link MediaImporter#isOKToImport(String,String) isOKToImport(String sopClassUID,String transferSyntaxUID)} returns true.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName		the fully qualified path name to a DICOM file
	 * @param	transferSyntaxUID	the Transfer Syntax of the Data Set in the DICOM file, from the DICOMDIR or Meta Information Header
	 * @param	sopClassUID			the SOP Class of the Data Set in the DICOM file, from the DICOMDIR or Meta Information Header
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName,String transferSyntaxUID,String sopClassUID) {
		doSomethingWithDicomFileOnMedia(mediaFileName);
	}
	
	/**
	 * <p>Do something with the referenced DICOM file that has been encountered.</p>
	 *
	 * <p>This method needs to be implemented in a sub-class to do anything useful,
	 * unless {@link MediaImporter#doSomethingWithDicomFileOnMedia(String,String,String) doSomethingWithDicomFileOnMedia(String mediaFileName,String transferSyntaxUID,String sopClassUID)} has been overridden instead.
	 * The default method does nothing.</p>
	 *
	 * <p>"Wanted" files are those that are DICOM files for which {@link MediaImporter#isOKToImport(String,String) isOKToImport(String sopClassUID,String transferSyntaxUID)} returns true.</p>
	 *
	 * <p>This method does not define any exceptions and hence must handle any
	 * errors locally.</p>
	 *
	 * @param	mediaFileName	the fully qualified path name to a DICOM file
	 */
	protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
		//logLn("MediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
		logLn("Is a DICOM PS3.10 file that is wanted: "+mediaFileName);
	}
	
	/**
	 * @return	the directory last used to perform an import
	 */
	public String getDirectory() { return mediaDirectoryPath; }

	/**
	 * <p>A class that implements {@link java.lang.Runnable Runnable} so that it can be invoked by {@link java.awt.EventQueue#invokeAndWait(Runnable) EventQueue.invokeAndWait()}.</p>
	 *
	 * <p>This is needed, for example, to call from a main() method, since the file chooser and logger dialogs and progress bar methods used MUST be invoked on the AWT Event Dispatch Thread.</p>
	 */
	public static class MediaImporterWithFileChooserDialogThread implements Runnable {
		private Class mediaImporterClass;
		private String mediaDirectoryPath;
		private String loggerTitleMessage;
		private int loggerWidth;
		private int loggerHeight;
		private boolean exitApplicationOnLoggerClose;
		private Component parent;
		private JProgressBar progressBar;
		
		/**
		 * <p>Pop up a file chooser dialog that allows the user to specify the location of
		 * the DICOMDIR file, or the parent folder (for example, the drive or volume) in which
		 * the DICOMDIR file is located, and then import the referenced files.</p>
		 *
		 * <p>Will be positioned relative to the parent component (for example, centered over the component)
		 * if specified, else placed in a look-and-feel-dependent position such as the center of the screen if null.</p>
		 *
		 * <p>Will also pop up a logger dialog box, which describes the progress.</p>
		 *
		 * <p>Will update a progress bar, if one is supplied.</p>
		 *
		 * <p>Uses the specified sub-class of {@link MediaImporter MediaImporter}, which will have its {@link MediaImporter#doSomethingWithDicomFileOnMedia(String) doSomethingWithDicomFileOnMedia()} method overridden to do something useful.</p>
		 *
		 * @param	mediaImporterClass				the class of {@link MediaImporter MediaImporter} to use, which needs to support the constructor {@link MediaImporter#MediaImporter(String,MessageLogger,JProgressBar) MediaImporter(String,MessageLogger,JProgressBar)}
		 * @param	mediaDirectoryPath				where to begin looking for the DICOMDIR and DICOM files
		 * @param	loggerTitleMessage				for the title bar of the dialog box
		 * @param	loggerWidth						initial width of the resizeable dialog box
		 * @param	loggerHeight					initial height of the resizeable dialog box
		 * @param	exitApplicationOnLoggerClose	if true, when the logger dialog box is closed (X-d out), will exit the application with success status
		 * @param	parent							the parent component of the dialog; can be <code>null</code>
		 * @param	progressBar						where to update progress as files are read (may be <code>null</code> for no progress bar)
		 */
		public MediaImporterWithFileChooserDialogThread(Class mediaImporterClass,String mediaDirectoryPath,String loggerTitleMessage,int loggerWidth,int loggerHeight,boolean exitApplicationOnLoggerClose,Component parent,JProgressBar progressBar) {
			this.mediaImporterClass = mediaImporterClass;
			this.mediaDirectoryPath = mediaDirectoryPath;
			this.loggerTitleMessage = loggerTitleMessage;
			this.loggerWidth = loggerWidth;
			this.loggerHeight = loggerHeight;
			this.exitApplicationOnLoggerClose = exitApplicationOnLoggerClose;
			this.parent = parent;
			this.progressBar = progressBar;
		}
		
		public void run() {
			MessageLogger logger = new DialogMessageLogger(loggerTitleMessage,loggerWidth,loggerHeight,exitApplicationOnLoggerClose);
			try {
				Class [] argTypes  = { String.class,MessageLogger.class,JProgressBar.class };
				Object[] argValues = { mediaDirectoryPath,logger,progressBar };
				MediaImporter importer = (MediaImporter)(mediaImporterClass.getConstructor(argTypes).newInstance(argValues));
				try {
					importer.choosePathAndImportDicomFiles(parent);
				}
				catch (DicomException e) {
					slf4jlogger.error("", e);
				}
				catch (IOException e) {
					slf4jlogger.error("", e);
				}
			}
			catch (NoSuchMethodException e) {
				slf4jlogger.error("", e);
			}
			catch (InstantiationException e) {
				slf4jlogger.error("", e);
			}
			catch (IllegalAccessException e) {
				slf4jlogger.error("", e);
			}
			catch (java.lang.reflect.InvocationTargetException e) {
				slf4jlogger.error("", e);
			}
		}
	}
	
	/**
	 * <p>Check that DICOM files are present and importable.</p>
	 *
	 * @param	arg	array of one string - the path to the media or folder containing
	 * the files to check are importable (in which case will write messages to
	 * stderr), or else will pop up a file chooser dialog (and write messages to
	 * a dialog box)
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 0) {
				java.awt.EventQueue.invokeAndWait(new MediaImporterWithFileChooserDialogThread(MediaImporter.class,"/","MediaImporter",512,384,true/*exitApplicationOnLoggerClose*/,null/*parent*/,null/*progressBar*/));
			}
			else if (arg.length == 1) {
				String           pathName=arg[0];
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				MediaImporter importer = new MediaImporter(logger);
				importer.importDicomFiles(pathName);
			}
			else {
				throw new Exception("Argument list must be zero or one value");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}



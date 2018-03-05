/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CompressedFrameDecoder;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.GraphicDisplayChangeEvent;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;

import com.pixelmed.utils.CapabilitiesAvailable;
import com.pixelmed.utils.FileUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

import java.util.ResourceBundle;
import java.util.Vector;

//import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.Spring;
import javax.swing.SpringLayout;
//import javax.swing.border.EmptyBorder;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class displays images and allows the user to black out burned-in annotation, and save the result.</p>
 * 
 * <p>A main method is provided, which can be supplied with a list of file names or pop up a file chooser dialog.</p>
 * 
 * @author	dclunie
 */
public class DicomImageBlackout extends JFrame  {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomImageBlackout.java,v 1.60 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomImageBlackout.class);

	protected static String resourceBundleName  = "com.pixelmed.display.DicomImageBlackout";

	protected ResourceBundle resourceBundle;
	
	protected String ourAETitle = "OURAETITLE";		// sub-classes might set this to something meaningful if they are active on the network, e.g., DicomCleaner
	
	private static final Dimension maximumMultiPanelDimension = new Dimension(800,600);
	//private static final Dimension maximumMultiPanelDimension = Toolkit.getDefaultToolkit().getScreenSize();
	//private static final int heightWantedForButtons = 50;
	private static final double splitPaneResizeWeight = 0.9;

	protected String[] dicomFileNames;
	protected String currentFileName;
	protected int currentFileNumber;
	protected Box mainPanel;
	protected JPanel multiPanel;
	
	protected SingleImagePanel imagePanel;
	protected AttributeList list;
	protected SourceImage sImg;
	protected boolean changesWereMade;
	protected boolean usedjpegblockredaction;
	protected boolean deferredDecompression;
	
	protected File redactedJPEGFile;
	
	protected int previousRows;
	protected int previousColumns;
	protected Vector previousPersistentDrawingShapes;

	protected void recordStateOfDrawingShapesForFileChange() {
		previousRows = sImg.getHeight();
		previousColumns =  sImg.getWidth();
		previousPersistentDrawingShapes = imagePanel.getPersistentDrawingShapes();
	}

	protected JPanel cineSliderControlsPanel;
	protected CineSliderChangeListener cineSliderChangeListener;
	protected JSlider cineSlider;
	
	protected JLabel imagesRemainingLabel;
	
	protected EventContext ourEventContext;
	
	protected boolean burnInOverlays;
	
	protected boolean useZeroBlackoutValue;
	protected boolean usePixelPaddingBlackoutValue;
	
	protected JCheckBox useZeroBlackoutValueCheckBox;
	protected JCheckBox usePixelPaddingBlackoutValueCheckBox;
			
	// implement FrameSelectionChangeListener ...
	
	protected OurFrameSelectionChangeListener ourFrameSelectionChangeListener;

	class OurFrameSelectionChangeListener extends SelfRegisteringListener {
	
		public OurFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
//System.err.println("DicomImageBlackout.OurFrameSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
//System.err.println("DicomImageBlackout.OurFrameSelectionChangeListener.changed(): event="+fse);
			cineSlider.setValue(fse.getIndex()+1);
		}
	}
	
	/***/
	protected class CineSliderChangeListener implements ChangeListener {
	
		public CineSliderChangeListener() {}	// so that sub-classes of DicomImageBlackout can cnostruct instances of this inner class
		
		/**
		 * @param	e
		 */
		public void stateChanged(ChangeEvent e) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(ourEventContext,cineSlider.getValue()-1));
		}
	}

	/**
	 * @param	min		minimum frame number, starting from 1
	 * @param	max		number of frames
	 * @param	value	frame number, starting from 1
	 */
	protected void createCineSliderIfNecessary(int min,int max,int value) {
		if (cineSlider == null || min != cineSlider.getMinimum() || max != cineSlider.getMaximum()) {
			cineSliderControlsPanel.removeAll();
			if (max > min) {
				cineSlider = new JSlider(min,max,value);								// don't leave to default, which is 50 and may be outside range
				cineSlider.setLabelTable(cineSlider.createStandardLabels(max-1,min));	// just label the ends
				cineSlider.setPaintLabels(true);
				//cineSliderControlsPanel.add(new JLabel("Frame index:"));
				cineSliderControlsPanel.add(cineSlider);
				cineSlider.addChangeListener(cineSliderChangeListener);
			}
			else {
				cineSlider=null;	// else single frame so no slider
			}
		}
		if (cineSlider != null) {
			cineSlider.setValue(value);
		}
	}
	
	protected void updateDisplayedFileNumber(int current,int total) {
		if (imagesRemainingLabel != null) {
			imagesRemainingLabel.setText(Integer.toString(current+1)+" of "+Integer.toString(total));
		}
	}

	/**
	 *<p>A class of values for the Burned in Annotation action argument of the {@link com.pixelmed.display.DicomImageBlackout#DicomImageBlackout(String,String [],StatusNotificationHandler,int) DicomImageBlackout()} constructor.</p>
	 */
	public abstract class BurnedInAnnotationFlagAction {
		private BurnedInAnnotationFlagAction() {}
		/**
		 *<p>Leave any existing Burned in Annotation attribute value alone.</p>
		 */
		public static final int LEAVE_ALONE = 1;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, without replacing it.</p>
		 */
		public static final int ALWAYS_REMOVE = 2;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, only replacing it and using a value of NO when regions have been blacked out.</p>
		 */
		public static final int ADD_AS_NO_IF_CHANGED = 3;
		/**
		 *<p>Always remove the Burned in Annotation attribute when the file is saved, always replacing it with a value of NO,
		 * regardless of whether when regions have been blacked out, such as when visual inspection confirms that there is no
		 * burned in annotation.</p>
		 */
		public static final int ADD_AS_NO_IF_SAVED = 4;
	}
	
	protected int burnedinflag;

	/**
	 *<p>An abstract class for the user of to supply a callback notification method,
	 * supplied as an argument of the {@link com.pixelmed.display.DicomImageBlackout#DicomImageBlackout(String,String [],StatusNotificationHandler,int) DicomImageBlackout()} constructor.</p>
	 */
	public abstract class StatusNotificationHandler {
		protected StatusNotificationHandler() {}
		public static final int WINDOW_CLOSED = 1;
		public static final int CANCELLED = 2;
		public static final int COMPLETED = 3;
		public static final int SAVE_FAILED = 4;
		public static final int UNSAVED_CHANGES = 5;
		public static final int SAVE_SUCCEEDED = 6;
		public static final int READ_FAILED = 7;
		public static final int BLACKOUT_FAILED = 8;
		/**
		 * <p>The callback method when status is updated.</p>
		 *
		 * @param	status		a numeric status
		 * @param	message		a description of the status, and in some cases, affected file names
		 * @param	t			the exception that lead to the status notification, if caused by an exception, else null
		 */
		public abstract void notify(int status,String message,Throwable t);
	}

	/**
	 *<p>A default status notification implementation, which just writes everything to stderr.</p>
	 */
	public class DefaultStatusNotificationHandler extends StatusNotificationHandler {
		public void notify(int status,String message,Throwable t) {
			System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): status = "+status);
			System.err.println("DicomImageBlackout.DefaultStatusNotificationHandler.notify(): message = "+message);
			if (t != null) {
				t.printStackTrace(System.err);
			}
		}
	}

	protected StatusNotificationHandler statusNotificationHandler;
	
	/**
	 * <p>Load the named DICOM file and display it in the image panel.</p>
	 *
	 * @param	dicomFileName
	 */
	protected void loadDicomFileOrDirectory(String dicomFileName) {
		try {
			File currentFile = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
			loadDicomFileOrDirectory(currentFile);
		}
		catch (Exception e) {
			slf4jlogger.error("Read failed",e);
			if (statusNotificationHandler != null) {
				statusNotificationHandler.notify(StatusNotificationHandler.READ_FAILED,"Read failed",e);
			}
			dispose();
		}
	}
	
	/**
	 * <p>Load the named DICOM file and display it in the image panel.</p>
	 *
	 * @param	currentFile
	 */
	protected void loadDicomFileOrDirectory(File currentFile) {
		changesWereMade = false;
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		multiPanel.revalidate();		// needed because contents have changed
		multiPanel.repaint();			// because if one dimension of the size does not change but the other shrinks, then the old image is left underneath, not overwritten by background (000446)
		//multiPanel.paintImmediately(new Rectangle(multiPanel.getSize(null)));
		{
			SafeCursorChanger cursorChanger = new SafeCursorChanger(this);
			cursorChanger.setWaitCursor();
			try {
				slf4jlogger.info("loadDicomFileOrDirectory(): Open {}",currentFile);
				currentFileName = currentFile.getAbsolutePath();		// set to what we actually used, used for later save
				deferredDecompression = CompressedFrameDecoder.canDecompress(currentFile);
				slf4jlogger.info("loadDicomFileOrDirectory(): deferredDecompression {}",deferredDecompression);
				DicomInputStream i = new DicomInputStream(currentFile);
				list = new AttributeList();
				list.setDecompressPixelData(!deferredDecompression);		// we don't want to decompress it during read if we can decompress it on the fly during display (000784)
				list.read(i);
				i.close();
				String useSOPClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (SOPClass.isImageStorage(useSOPClassUID)) {
					sImg = new SourceImage(list);
					imagePanel = new SingleImagePanelWithRegionDrawing(sImg,ourEventContext);
					imagePanel.setShowOverlays(burnInOverlays);
					imagePanel.setApplyShutter(false);	// we do not want to "hide" from view any identification information hidden behind shutters (000607)
					addSingleImagePanelToMultiPanelAndEstablishLayout();
					createCineSliderIfNecessary(1,Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1),1);
					cursorChanger.restoreCursor();	// needs to be here and not later, else interferes with cursor in repaint() of  SingleImagePanel
					showUIComponents();				// will pack, revalidate, etc, perhaps for the first time
					
					if (previousPersistentDrawingShapes != null) {
						if (previousRows == sImg.getHeight() && previousColumns == sImg.getWidth()) {
							imagePanel.setPersistentDrawingShapes(previousPersistentDrawingShapes);
						}
						else {
							previousRows = 0;
							previousColumns = 0;
							previousPersistentDrawingShapes = null;
						}
					}
				}
				else {
					throw new DicomException("unsupported SOP Class "+useSOPClassUID);
				}
			} catch (Exception e) {
				slf4jlogger.error("Read failed",e);
				if (statusNotificationHandler != null) {
					statusNotificationHandler.notify(StatusNotificationHandler.READ_FAILED,"Read failed",e);
				}
				cursorChanger.restoreCursor();
				dispose();
			}
		}
	}

	protected class ApplyActionListener implements ActionListener {
		DicomImageBlackout application;
		SafeCursorChanger cursorChanger;

		public ApplyActionListener(DicomImageBlackout application) {
			this.application=application;
			cursorChanger = new SafeCursorChanger(application);
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed()");
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): burnInOverlays = "+application.burnInOverlays);
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): useZeroBlackoutValue = "+application.useZeroBlackoutValue);
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): usePixelPaddingBlackoutValue = "+application.usePixelPaddingBlackoutValue);
			long startTime = System.currentTimeMillis();
			recordStateOfDrawingShapesForFileChange();
			cursorChanger.setWaitCursor();
			if (application.imagePanel != null && application.sImg != null && application.list != null) {
				if (application.imagePanel != null) {
					Vector shapes = application.imagePanel.getPersistentDrawingShapes();
					if ((shapes != null && shapes.size() > 0) || application.burnInOverlays) {
						changesWereMade = true;
						String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
						try {
							if (transferSyntaxUID.equals(TransferSyntax.JPEGBaseline) && !application.burnInOverlays && CapabilitiesAvailable.haveJPEGBaselineSelectiveBlockRedaction()) {
								slf4jlogger.info("ApplyActionListener.actionPerformed(): Blackout JPEG blocks");
								usedjpegblockredaction = true;
								if (redactedJPEGFile != null) {
									redactedJPEGFile.delete();
								}
								redactedJPEGFile = File.createTempFile("DicomImageBlackout",".dcm");
								ImageEditUtilities.blackoutJPEGBlocks(new File(application.currentFileName),redactedJPEGFile,shapes);
								// Need to re-read the file because we need to decompress the redacted JPEG to use to display it again
								DicomInputStream i = new DicomInputStream(redactedJPEGFile);
								list = new AttributeList();
								list.setDecompressPixelData(!CompressedFrameDecoder.canDecompress(redactedJPEGFile));		// we don't want to decompress it during read if we can decompress it on the fly during display (000784)
								list.read(i);
								i.close();
								// do NOT delete redactedJPEGFile, since will reuse it when "saving", and also file may need to hang around for display of cached pixel data
								slf4jlogger.info("ApplyActionListener.actionPerformed(): Create new source image after blackout of JPEG blocks");
								application.sImg = new SourceImage(application.list);	// remake SourceImage, in case blackout() changed the AttributeList (e.g., removed overlays)
							}
							else {
								slf4jlogger.info("ApplyActionListener.actionPerformed(): Blackout decompressed image");
								usedjpegblockredaction = false;
								// may not have been decompressed during AttributeList reading when CompressedFrameDecoder.canDecompress(currentFile) is true,
								// but ImageEditUtilities.blackout() can decompress on the fly because it calls SourceImage.getBufferedImage(frame);
								// we like that because it uses less (esp. contiguous) memory
								// but we need to make sure when writing it during Save that PhotometricInterpretation is corrected, etc. vide infra
								ImageEditUtilities.blackout(application.sImg,application.list,shapes,application.burnInOverlays,application.usePixelPaddingBlackoutValue,application.useZeroBlackoutValue,0);
								// do NOT need to recreate SourceImage from list ... already done by blackout()
							}
							application.imagePanel.dirty(application.sImg);
							application.imagePanel.repaint();
						}
						catch (Exception e) {
							slf4jlogger.error("Blackout failed",e);
							if (application.statusNotificationHandler != null) {
								application.statusNotificationHandler.notify(StatusNotificationHandler.BLACKOUT_FAILED,"Blackout failed",e);
							}
							application.dispose();
						}
					}
					else {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): no shapes or burning in of overlays to do");
					}
				}
			}
			else {
//System.err.println("DicomImageBlackout.ApplyActionListener.actionPerformed(): no panel or image or list to do");
			}
			slf4jlogger.info("ApplyActionListener.actionPerformed(): total time = {}",(System.currentTimeMillis() - startTime));
			cursorChanger.restoreCursor();
		}
	}

	protected class SaveActionListener implements ActionListener {
		DicomImageBlackout application;
		SafeCursorChanger cursorChanger;

		public SaveActionListener(DicomImageBlackout application) {
			this.application=application;
			cursorChanger = new SafeCursorChanger(application);
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed()");
			long startTime = System.currentTimeMillis();
			recordStateOfDrawingShapesForFileChange();
			cursorChanger.setWaitCursor();
			boolean success = true;
			try {
				application.sImg.close();		// in case memory-mapped pixel data open; would inhibit Windows rename or copy/reopen otherwise
				application.sImg = null;
				System.gc();					// cannot guarantee that buffers will be released, causing problems on Windows, but try ... http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154 :(
				System.runFinalization();
				System.gc();
			}
			catch (Throwable t) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): unable to close image - not saving modifications");
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,
						"Save failed - unable to close image - not saving modifications",t);
				}
				success=false;
			}
			File currentFile = new File(currentFileName);
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): currentFile = "+currentFile);
			File newFile = new File(currentFileName+".new");
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): newFile = "+newFile);
			if (success) {
				String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
				try {
					String outputTransferSyntaxUID = null;
					if (usedjpegblockredaction && redactedJPEGFile != null) {
						// do not repeat the redaction, reuse redactedJPEGFile, without decompressing the pixels, so that we can update the technique stuff in the list
						DicomInputStream i = new DicomInputStream(redactedJPEGFile);
						list = new AttributeList();
						list.setDecompressPixelData(false);		// do not need to display it, so no need to decompress
						list.read(i);
						i.close();
						outputTransferSyntaxUID = TransferSyntax.JPEGBaseline;
					}
					else {
						outputTransferSyntaxUID = TransferSyntax.ExplicitVRLittleEndian;
						list.correctDecompressedImagePixelModule(deferredDecompression);					// make sure to correct even if decompression was deferred
						list.insertLossyImageCompressionHistoryIfDecompressed(deferredDecompression);
					}
					if (burnedinflag != BurnedInAnnotationFlagAction.LEAVE_ALONE) {
						list.remove(TagFromName.BurnedInAnnotation);
						if (burnedinflag == BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED
						|| (burnedinflag == BurnedInAnnotationFlagAction.ADD_AS_NO_IF_CHANGED && changesWereMade)) {
							Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation); a.addValue("NO"); list.put(a);
						}
					}
					if (changesWereMade) {
						{
							Attribute aDeidentificationMethod = list.get(TagFromName.DeidentificationMethod);
							if (aDeidentificationMethod == null) {
								aDeidentificationMethod = new LongStringAttribute(TagFromName.DeidentificationMethod);
								list.put(aDeidentificationMethod);
							}
							if (application.burnInOverlays) {
								aDeidentificationMethod.addValue("Overlays burned in then blacked out");
							}
							aDeidentificationMethod.addValue("Burned in text blacked out");
						}
						{
							SequenceAttribute aDeidentificationMethodCodeSequence = (SequenceAttribute)(list.get(TagFromName.DeidentificationMethodCodeSequence));
							if (aDeidentificationMethodCodeSequence == null) {
								aDeidentificationMethodCodeSequence = new SequenceAttribute(TagFromName.DeidentificationMethodCodeSequence);
								list.put(aDeidentificationMethodCodeSequence);
							}
							aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("113101","DCM","Clean Pixel Data Option").getAttributeList());
						}
					}
					list.removeGroupLengthAttributes();
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					
					FileMetaInformation.addFileMetaInformation(list,outputTransferSyntaxUID,ourAETitle);
					list.write(newFile,outputTransferSyntaxUID,true/*useMeta*/,true/*useBufferedStream*/);
					
					list = null;
					try {
						currentFile.delete();
						FileUtilities.renameElseCopyTo(newFile,currentFile);
					}
					catch (IOException e) {
//System.err.println("DicomImageBlackout.SaveActionListener.actionPerformed(): unable to rename "+newFile+" to "+currentFile+ " - not saving modifications");
						if (application.statusNotificationHandler != null) {
							application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,
								"Save failed - unable to rename or copy "+newFile+" to "+currentFile+ " - not saving modifications",e);
						}
						success=false;
					}
					
					if (redactedJPEGFile != null) {
						redactedJPEGFile.delete();
						redactedJPEGFile = null;
					}
					usedjpegblockredaction = false;
					
					changesWereMade = false;
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_SUCCEEDED,"Save of "+currentFileName+" succeeded",null);
					}
				}
				catch (DicomException e) {
					slf4jlogger.error("Save failed",e);
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,"Save failed",e);
					}
				}
				catch (IOException e) {
					slf4jlogger.error("Save failed",e);
					if (application.statusNotificationHandler != null) {
						application.statusNotificationHandler.notify(StatusNotificationHandler.SAVE_FAILED,"Save failed",e);
					}
				}
				slf4jlogger.info("SaveActionListener.actionPerformed(): time to save = {}",(System.currentTimeMillis() - startTime));
			}
			loadDicomFileOrDirectory(currentFile);
			slf4jlogger.info("SaveActionListener.actionPerformed(): total time including reload for display = {}",(System.currentTimeMillis() - startTime));
			cursorChanger.restoreCursor();
		}
	}
	
	protected class NextActionListener implements ActionListener {
		DicomImageBlackout application;

		public NextActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.NextActionListener.actionPerformed()");
			recordStateOfDrawingShapesForFileChange();
			if (changesWereMade) {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.UNSAVED_CHANGES,
						"Changes were applied to "+dicomFileNames[currentFileNumber]+" but were discarded and not saved",null);
				}
			}
			++currentFileNumber;
			if (dicomFileNames != null && currentFileNumber < dicomFileNames.length) {
				updateDisplayedFileNumber(currentFileNumber,dicomFileNames.length);
				loadDicomFileOrDirectory(dicomFileNames[currentFileNumber]);
			}
			else {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.COMPLETED,"Normal completion",null);
				}
				application.dispose();
			}
		}
	}
	
	protected class PreviousActionListener implements ActionListener {
		DicomImageBlackout application;

		public PreviousActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.PreviousActionListener.actionPerformed()");
			recordStateOfDrawingShapesForFileChange();
			if (changesWereMade) {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.UNSAVED_CHANGES,
						"Changes were applied to "+dicomFileNames[currentFileNumber]+" but were discarded and not saved",null);
				}
			}
			--currentFileNumber;
			if (dicomFileNames != null && currentFileNumber >= 0) {
				updateDisplayedFileNumber(currentFileNumber,dicomFileNames.length);
				loadDicomFileOrDirectory(dicomFileNames[currentFileNumber]);
			}
			else {
				if (application.statusNotificationHandler != null) {
					application.statusNotificationHandler.notify(StatusNotificationHandler.COMPLETED,"Normal completion",null);
				}
				application.dispose();
			}
		}
	}
	
	protected ApplyActionListener applyActionListener;
	protected SaveActionListener saveActionListener;
	protected NextActionListener nextActionListener;
	protected PreviousActionListener previousActionListener;

	protected JButton blackoutApplyButton;
	protected JButton blackoutSaveButton;
	protected JButton blackoutNextButton;
	protected JButton blackoutPreviousButton;

	protected class ApplySaveAllActionListener implements ActionListener {
		DicomImageBlackout application;

		public ApplySaveAllActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.ApplySaveAllActionListener.actionPerformed()");
			do {
				applyActionListener.actionPerformed(null);
				saveActionListener.actionPerformed(null);
				nextActionListener.actionPerformed(null);
				//blackoutApplyButton.doClick();
				//blackoutSaveButton.doClick();
				//blackoutNextButton.doClick();
			} while (dicomFileNames != null && currentFileNumber < dicomFileNames.length);
		}
	}
	
	protected class CancelActionListener implements ActionListener {
		DicomImageBlackout application;

		public CancelActionListener(DicomImageBlackout application) {
			this.application=application;
		}
		
		public void actionPerformed(ActionEvent event) {
//System.err.println("DicomImageBlackout.CancelActionListener.actionPerformed()");
			if (application.statusNotificationHandler != null) {
				application.statusNotificationHandler.notify(StatusNotificationHandler.CANCELLED,"Cancelled",null);
			}
			application.dispose();
		}
	}
	
	protected class OverlaysChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public OverlaysChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.OverlaysChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.burnInOverlays = ((JCheckBox)(e.getSource())).isSelected();
//System.err.println("DicomImageBlackout.OverlaysChangeListener.stateChanged(): burnInOverlays = "+application.burnInOverlays);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.burnInOverlays));
			}
		}
	}
	
	protected class ZeroBlackoutValueChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public ZeroBlackoutValueChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.ZeroBlackoutValueChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.useZeroBlackoutValue = ((JCheckBox)(e.getSource())).isSelected();
				if (application.useZeroBlackoutValue) {
					application.usePixelPaddingBlackoutValue=false;
					application.usePixelPaddingBlackoutValueCheckBox.setSelected(application.usePixelPaddingBlackoutValue);
				}
//System.err.println("DicomImageBlackout.ZeroBlackoutValueChangeListener.stateChanged(): useZeroBlackoutValue = "+application.useZeroBlackoutValue);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.useZeroBlackoutValue));
			}
		}
	}
	
	protected class PixelPaddingBlackoutValueChangeListener implements ChangeListener {
		DicomImageBlackout application;
		EventContext eventContext;

		public PixelPaddingBlackoutValueChangeListener(DicomImageBlackout application,EventContext eventContext) {
			this.application=application;
			this.eventContext=eventContext;
		}
		
		public void stateChanged(ChangeEvent e) {
//System.err.println("DicomImageBlackout.PixelPaddingBlackoutValueChangeListener.stateChanged(): event = "+e);
			if (e != null && e.getSource() instanceof JCheckBox) {
				application.usePixelPaddingBlackoutValue = ((JCheckBox)(e.getSource())).isSelected();
				if (application.usePixelPaddingBlackoutValue) {
					application.useZeroBlackoutValue=false;
					application.useZeroBlackoutValueCheckBox.setSelected(application.useZeroBlackoutValue);
				}
//System.err.println("DicomImageBlackout.PixelPaddingBlackoutValueChangeListener.stateChanged(): usePixelPaddingBlackoutValue = "+application.usePixelPaddingBlackoutValue);
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new GraphicDisplayChangeEvent(eventContext,application.usePixelPaddingBlackoutValue));
			}
		}
	}
	
	protected double getScaleFactorToFitInMaximumAvailable(double useWidth,double useHeight,double maxWidth,double maxHeight) {
		double sx = maxWidth/useWidth;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): sx = "+sx);
		double sy = maxHeight/useHeight;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): sy = "+sy);
		// always choose smallest, regardless of whether scaling up or down
		double useScaleFactor = sx < sy ? sx : sy;
//System.err.println("DicomImageBlackout.getScaleFactorToFitInMaximumAvailable(): useScaleFactor = "+useScaleFactor);
		return useScaleFactor;
	}
	
	protected Dimension changeDimensionToFitInMaximumAvailable(Dimension useDimension,Dimension maxDimension,boolean onlySmaller) {
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): have dimension "+useDimension);
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): maximum dimension "+maxDimension);
		double useWidth = useDimension.getWidth();
		double useHeight = useDimension.getHeight();
		double maxWidth = maxDimension.getWidth();
		double maxHeight = maxDimension.getHeight();
		double useScaleFactor = getScaleFactorToFitInMaximumAvailable(useWidth,useHeight,maxWidth,maxHeight);
		if (useScaleFactor < 1 || !onlySmaller) {
			useWidth = useWidth*useScaleFactor;
			useHeight = useHeight*useScaleFactor;
		}
		useDimension = new Dimension((int)useWidth,(int)useHeight);
//System.err.println("DicomImageBlackout.changeDimensionToFitInMaximumAvailable(): use new dimension "+useDimension);
		return useDimension;
	}
	
	protected Dimension reduceDimensionToFitInMaximumAvailable(Dimension useDimension) {
		return changeDimensionToFitInMaximumAvailable(useDimension,maximumMultiPanelDimension,true);
	}

	protected class CenterMaximumAfterInitialSizeLayout implements LayoutManager {
		public CenterMaximumAfterInitialSizeLayout() {}

		public void addLayoutComponent(String name, Component comp) {}
		
		public void layoutContainer(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				Dimension parentSize = parent.getSize();
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): parentSize = "+parentSize);
				
				int sumOfComponentWidths  = 0;
				int sumOfComponentHeights = 0;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): component "+c+" preferred size = "+componentSize);
					sumOfComponentWidths  += componentSize.getWidth();
					sumOfComponentHeights += componentSize.getHeight();
				}
				
				int availableWidth  = parentSize.width  - (insets.left+insets.right);
				int availableHeight = parentSize.height - (insets.top+insets.bottom);
				
				int leftOffset = 0;
				int topOffset  = 0;

				boolean useScale = false;
				double useScaleFactor = 1;
				if (sumOfComponentWidths == availableWidth && sumOfComponentHeights <= availableHeight
				 || sumOfComponentWidths <= availableWidth && sumOfComponentHeights == availableHeight) {
					// First time, the sum of either the widths or the heights will equal what
					// is available, since the parent size was derived from calls to minimumLayoutSize()
					// and preferredLayoutSize(), hence no scaling is required or should be performed ...
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): not scaling since once size matches and fits inside");
					leftOffset = (availableWidth  - sumOfComponentWidths ) / 2;
					topOffset  = (availableHeight - sumOfComponentHeights) / 2;
				}
				else {
					// Subsequently, if a resize on the parent has been performed, we should ALWAYS pay
					// attention to it ...
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): scaling");
					useScale = true;
					useScaleFactor = getScaleFactorToFitInMaximumAvailable(sumOfComponentWidths,sumOfComponentHeights,availableWidth,availableHeight);
					leftOffset = (int)((availableWidth  - sumOfComponentWidths*useScaleFactor ) / 2);
					topOffset  = (int)((availableHeight - sumOfComponentHeights*useScaleFactor) / 2);
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): useScale = "+useScale);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): useScaleFactor = "+useScaleFactor);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): leftOffset = "+leftOffset);
//System.err.println("CenterMaximumAfterInitialSizeLayout.layoutContainer(): topOffset  = "+topOffset);
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
					int w = componentSize.width;
					int h = componentSize.height;
					if (useScale) {
						w = (int)(w * useScaleFactor);
						h = (int)(h * useScaleFactor);
					}
					component.setBounds(leftOffset,topOffset,w,h);
					leftOffset += w;
					topOffset  += h;
				}
			}
		}
		
		public Dimension minimumLayoutSize(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				int w = insets.left+insets.right;
				int h = insets.top+insets.bottom;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getMinimumSize();
					w += componentSize.getWidth();
					h += componentSize.getHeight();
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.minimumLayoutSize() = "+w+","+h);
				return new Dimension(w,h);
			}
		}
		
		public Dimension preferredLayoutSize(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();
				int componentCount = parent.getComponentCount();
				int w  = insets.left+insets.right;
				int h = insets.top+insets.bottom;
				for (int c=0; c<componentCount; ++c) {
					Component component = parent.getComponent(c);
					Dimension componentSize = component.getPreferredSize();
					w += componentSize.getWidth();
					h += componentSize.getHeight();
				}
//System.err.println("CenterMaximumAfterInitialSizeLayout.preferredLayoutSize() = "+w+","+h);
				return new Dimension(w,h);
			}
		}
		
		public void removeLayoutComponent(Component comp) {}
 	}
	
	protected void addSingleImagePanelToMultiPanelAndEstablishLayout() {
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start sImg.getDimension() = "+sImg.getDimension());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getPreferredSize() = "+multiPanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getMinimumSize() = "+multiPanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start multiPanel.getMaximumSize() = "+multiPanel.getMaximumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getPreferredSize() = "+imagePanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getMinimumSize() = "+imagePanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): start imagePanel.getMaximumSize() = "+imagePanel.getMaximumSize());
		// Need to have some kind of layout manager, else imagePanel does not resize when frame is resized by user
		addSingleImagePanelToMultiPanelAndEstablishLayoutWithCenterMaximumAfterInitialSizeLayout();
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getPreferredSize() = "+multiPanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getMinimumSize() = "+multiPanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end multiPanel.getMaximumSize() = "+multiPanel.getMaximumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getPreferredSize() = "+imagePanel.getPreferredSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getMinimumSize() = "+imagePanel.getMinimumSize());
//System.err.println("DicomImageBlackout.addSingleImagePanelToMultiPanelAndEstablishLayout(): end imagePanel.getMaximumSize() = "+imagePanel.getMaximumSize());
	}
	
	protected void addSingleImagePanelToMultiPanelAndEstablishLayoutWithCenterMaximumAfterInitialSizeLayout() {
		Dimension useDimension = reduceDimensionToFitInMaximumAvailable(sImg.getDimension());
	
		imagePanel.setPreferredSize(useDimension);
		imagePanel.setMinimumSize(useDimension);	// this is needed to force initial size to be large enough; will be reset to null later to allow resize to change

		multiPanel.setPreferredSize(useDimension);	// this seems to be needed as well
		multiPanel.setMinimumSize(useDimension);	// this seems to be needed as well

		CenterMaximumAfterInitialSizeLayout layout = new CenterMaximumAfterInitialSizeLayout();
		multiPanel.setLayout(layout);
		multiPanel.setBackground(Color.black);
		
		multiPanel.add(imagePanel);
	}


	protected void showUIComponents() {
		remove(mainPanel);					// in case not the first time
		add(mainPanel);
		pack();
		//multiPanel.revalidate();
		validate();
		setVisible(true);
		imagePanel.setMinimumSize(null);	// this is needed to prevent later resize being limited to initial size ...
		multiPanel.setMinimumSize(null);	// this is needed to prevent later resize being limited to initial size ...
	}
	
	protected void buildUIComponents() {
		ourEventContext = new EventContext("Blackout Panel");

		multiPanel = new JPanel();

		JPanel blackoutButtonsPanel = new JPanel();
		// don't set button panel height, else interacts with validate during showUIComponents() needed for no initial image resizing, and cuts off button panel
		//blackoutButtonsPanel.setPreferredSize(new Dimension((int)multiPanel.getPreferredSize().getWidth(),heightWantedForButtons));
		
		blackoutButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		burnInOverlays = false;
		useZeroBlackoutValue = false;
		usePixelPaddingBlackoutValue = true;
		
		JCheckBox keepOverlaysCheckBox = new JCheckBox(resourceBundle.getString("keepOverlaysCheckBoxLabelText"),burnInOverlays);
		keepOverlaysCheckBox.setToolTipText(resourceBundle.getString("keepOverlaysCheckBoxToolTipText"));
		keepOverlaysCheckBox.setMnemonic(KeyEvent.VK_O);
		blackoutButtonsPanel.add(keepOverlaysCheckBox);
		keepOverlaysCheckBox.addChangeListener(new OverlaysChangeListener(this,ourEventContext));
		
		// application scope not local, since change listener needs access to make mutually exclusive with useZeroBlackoutValueCheckBox
		usePixelPaddingBlackoutValueCheckBox = new JCheckBox(resourceBundle.getString("usePixelPaddingBlackoutValueCheckBoxLabelText"),usePixelPaddingBlackoutValue);
		usePixelPaddingBlackoutValueCheckBox.setToolTipText(resourceBundle.getString("usePixelPaddingBlackoutValueCheckBoxToolTipText"));
		usePixelPaddingBlackoutValueCheckBox.setMnemonic(KeyEvent.VK_P);
		blackoutButtonsPanel.add(usePixelPaddingBlackoutValueCheckBox);
		usePixelPaddingBlackoutValueCheckBox.addChangeListener(new PixelPaddingBlackoutValueChangeListener(this,ourEventContext));
	
		// application scope not local, since change listener needs access to make mutually exclusive with usePixelPaddingBlackoutValueCheckBox
		useZeroBlackoutValueCheckBox = new JCheckBox(resourceBundle.getString("useZeroBlackoutValueCheckBoxLabelText"),useZeroBlackoutValue);
		useZeroBlackoutValueCheckBox.setToolTipText(resourceBundle.getString("useZeroBlackoutValueCheckBoxToolTipText"));
		useZeroBlackoutValueCheckBox.setMnemonic(KeyEvent.VK_Z);
		blackoutButtonsPanel.add(useZeroBlackoutValueCheckBox);
		useZeroBlackoutValueCheckBox.addChangeListener(new ZeroBlackoutValueChangeListener(this,ourEventContext));
		
		blackoutPreviousButton = new JButton(resourceBundle.getString("blackoutPreviousButtonLabelText"));
		blackoutPreviousButton.setToolTipText(resourceBundle.getString("blackoutPreviousButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutPreviousButton);
		previousActionListener = new PreviousActionListener(this);
		blackoutPreviousButton.addActionListener(previousActionListener);
		
		blackoutApplyButton = new JButton(resourceBundle.getString("blackoutApplyButtonLabelText"));
		blackoutApplyButton.setToolTipText(resourceBundle.getString("blackoutApplyButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutApplyButton);
		applyActionListener = new ApplyActionListener(this);
		blackoutApplyButton.addActionListener(applyActionListener);
		
		blackoutSaveButton = new JButton(resourceBundle.getString("blackoutSaveButtonLabelText"));
		blackoutSaveButton.setToolTipText(resourceBundle.getString("blackoutSaveButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutSaveButton);
		saveActionListener = new SaveActionListener(this);
		blackoutSaveButton.addActionListener(saveActionListener);
		
		blackoutNextButton = new JButton(resourceBundle.getString("blackoutNextButtonLabelText"));
		blackoutNextButton.setToolTipText(resourceBundle.getString("blackoutNextButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutNextButton);
		nextActionListener = new NextActionListener(this);
		blackoutNextButton.addActionListener(nextActionListener);
		
		JButton blackoutApplySaveAllButton = new JButton(resourceBundle.getString("blackoutApplySaveAllButtonLabelText"));
		blackoutApplySaveAllButton.setToolTipText(resourceBundle.getString("blackoutApplySaveAllButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutApplySaveAllButton);
		blackoutApplySaveAllButton.addActionListener(new ApplySaveAllActionListener(this));
		
		imagesRemainingLabel = new JLabel("0 of 0");
		blackoutButtonsPanel.add(imagesRemainingLabel);

		JButton blackoutCancelButton = new JButton(resourceBundle.getString("blackoutCancelButtonLabelText"));
		blackoutCancelButton.setToolTipText(resourceBundle.getString("blackoutCancelButtonToolTipText"));
		blackoutButtonsPanel.add(blackoutCancelButton);
		blackoutCancelButton.addActionListener(new CancelActionListener(this));

		cineSliderControlsPanel = new JPanel();
		blackoutButtonsPanel.add(cineSliderControlsPanel);
		cineSliderChangeListener = new CineSliderChangeListener();

		ourFrameSelectionChangeListener = new OurFrameSelectionChangeListener(ourEventContext);	// context needs to match SingleImagePanel to link events

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,multiPanel,blackoutButtonsPanel);
		splitPane.setOneTouchExpandable(false);
		splitPane.setResizeWeight(splitPaneResizeWeight);

		JLabel helpBar = new JLabel(resourceBundle.getString("statusBarHelpText"));

		mainPanel = new Box(BoxLayout.Y_AXIS);
		mainPanel.add(splitPane);
		mainPanel.add(helpBar);
	}

	/**
	 * <p>Opens a window to display the supplied list of DICOM files to allow them to have burned in annotation blacked out.</p>
	 *
	 * <p>Each file will be processed sequentially, with the edited pixel data overwriting the original file.</p>
	 *
	 * @param	dicomFileNames		the list of file names to process, if null a file chooser dialog will be raised
	 * @param	snh					an instance of {@link StatusNotificationHandler StatusNotificationHandler}; if null, a default handler will be used that writes to stderr
	 * @param	burnedinflag		whether or not and under what circumstances to to add/change BurnedInAnnotation attribute; takes one of the values of {@link BurnedInAnnotationFlagAction BurnedInAnnotationFlagAction}
	 */
	public DicomImageBlackout(String dicomFileNames[],StatusNotificationHandler snh,int burnedinflag) {
		this(null/*title*/,dicomFileNames,snh,burnedinflag);
	}

	/**
	 * <p>Opens a window to display the supplied list of DICOM files to allow them to have burned in annotation blacked out.</p>
	 *
	 * <p>Each file will be processed sequentially, with the edited pixel data overwriting the original file.</p>
	 *
	 * @param	title				the string to use in the title bar of the window or null if use default for locale
	 * @param	dicomFileNames		the list of file names to process, if null a file chooser dialog will be raised
	 * @param	snh					an instance of {@link StatusNotificationHandler StatusNotificationHandler}; if null, a default handler will be used that writes to stderr
	 * @param	burnedinflag		whether or not and under what circumstances to to add/change BurnedInAnnotation attribute; takes one of the values of {@link BurnedInAnnotationFlagAction BurnedInAnnotationFlagAction}
	 */
	public DicomImageBlackout(String title,String dicomFileNames[],StatusNotificationHandler snh,int burnedinflag) {
		super(title);

		resourceBundle = ResourceBundle.getBundle(resourceBundleName);
		if (title == null) {
			setTitle(resourceBundle.getString("applicationTitle"));
		}

		{
			String osname = System.getProperty("os.name");
			if (osname != null && osname.toLowerCase().startsWith("windows")) {
				slf4jlogger.info("disabling memory mapping for SourceImage on Windows platform");
				SourceImage.setAllowMemoryMapping(false);	// otherwise problems with redacting large
			}
		}
		this.statusNotificationHandler = snh == null ? new DefaultStatusNotificationHandler() : snh;
		this.burnedinflag = burnedinflag;
		//No need to setBackground(Color.lightGray) .. we set this via L&F UIManager properties for the application that uses this class
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (statusNotificationHandler != null) {
					statusNotificationHandler.notify(StatusNotificationHandler.WINDOW_CLOSED,"Window closed",null);
				}
				dispose();
			}
		});
		
		buildUIComponents();
		
		if (dicomFileNames == null || dicomFileNames.length == 0) {
			// we are not on the Swing event dispatcher thread, so ...
			String dicomFileName = null;
			SafeFileChooser.SafeFileChooserThread fileChooserThread = new SafeFileChooser.SafeFileChooserThread();
			try {
				java.awt.EventQueue.invokeAndWait(fileChooserThread);
				dicomFileName=fileChooserThread.getSelectedFileName();
			}
			catch (InterruptedException e) {
				slf4jlogger.error("",e);
			}
			catch (InvocationTargetException e) {
				slf4jlogger.error("",e);
			}
			if (dicomFileName != null) {
				String[] fileNames = { dicomFileName };
				dicomFileNames = fileNames;
			}
		}
		if (dicomFileNames != null && dicomFileNames.length > 0) {
			this.dicomFileNames = dicomFileNames;
			currentFileNumber = 0;
			updateDisplayedFileNumber(currentFileNumber,dicomFileNames.length);
			loadDicomFileOrDirectory(dicomFileNames[currentFileNumber]);
		}
	}

	public void deconstruct() {
//System.err.println("DicomImageBlackout.deconstruct()");
		// avoid "listener leak"
		if (ourFrameSelectionChangeListener != null) {
//System.err.println("DicomImageBlackout.deconstruct(): removing ourFrameSelectionChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSelectionChangeListener);
			ourFrameSelectionChangeListener=null;
		}
		if (multiPanel != null) {
//System.err.println("DicomImageBlackout.deconstruct(): call deconstructAllSingleImagePanelsInContainer in case any listeners hanging around");
			SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		}
	}
	
	public void dispose() {
//System.err.println("DicomImageBlackout.dispose()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.dispose();
	}
	
	protected void finalize() throws Throwable {
//System.err.println("DicomImageBlackout.finalize()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.finalize();
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	optionally, a list of files; if absent a file dialog is presented
	 */
	public static void main(String arg[]) {
		// use static methods from ApplicationFrame to establish L&F, even though not inheriting from ApplicationFrame
		ApplicationFrame.setInternationalizedFontsForGUI();
		ApplicationFrame.setBackgroundForGUI();
		new DicomImageBlackout(arg,null,BurnedInAnnotationFlagAction.ADD_AS_NO_IF_SAVED);
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.pixelmed.database.DatabaseApplicationProperties;
import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.DatabaseMediaImporter;
import com.pixelmed.database.DatabaseTreeBrowser;
import com.pixelmed.database.DatabaseTreeRecord;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;
import com.pixelmed.dicom.AgeStringAttribute;
import com.pixelmed.dicom.ArrayCopyUtilities;
import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeListFunctionalGroupsTableModelAllFrames;
import com.pixelmed.dicom.AttributeListFunctionalGroupsTableModelOneFrame;
import com.pixelmed.dicom.AttributeListTableBrowser;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AttributeTreeBrowser;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CompressedFrameDecoder;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.DescriptionFactory;
import com.pixelmed.dicom.DicomDirectory;
import com.pixelmed.dicom.DicomDirectoryBrowser;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.GeometryOfVolumeFromAttributeList;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.LongTextAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.PixelSpacing;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.ShortTextAttribute;
import com.pixelmed.dicom.SpectroscopyVolumeLocalization;
import com.pixelmed.dicom.StoredFilePathStrategy;
import com.pixelmed.dicom.StructuredReportTreeBrowser;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.UnsignedShortAttribute;
import com.pixelmed.dicom.XMLRepresentationOfDicomObjectFactory;
import com.pixelmed.dicom.XMLRepresentationOfStructuredReportObjectFactory;
import com.pixelmed.display.event.BrowserPaneChangeEvent;
import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.FrameSortOrderChangeEvent;
import com.pixelmed.display.event.SourceImageSelectionChangeEvent;
import com.pixelmed.display.event.SourceSpectrumSelectionChangeEvent;
import com.pixelmed.display.event.StatusChangeEvent;
import com.pixelmed.display.event.WellKnownContext;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;
import com.pixelmed.geometry.GeometryOfVolume;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.NetworkApplicationInformation;
import com.pixelmed.network.NetworkApplicationInformationFederated;
import com.pixelmed.network.NetworkApplicationProperties;
import com.pixelmed.network.PresentationAddress;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;
import com.pixelmed.query.FilterPanel;
import com.pixelmed.query.QueryInformationModel;
import com.pixelmed.query.QueryTreeBrowser;
import com.pixelmed.query.QueryTreeModel;
import com.pixelmed.query.QueryTreeRecord;
import com.pixelmed.query.StudyRootQueryInformationModel;

// import apple.dts.samplecode.osxadapter.OSXAdapter;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;
import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.FloatFormatter;
import com.pixelmed.utils.MessageLogger;
//import com.pixelmed.transfermonitor.TransferMonitor;
import com.pixelmed.validate.DicomInstanceValidator;
import com.pixelmed.validate.DicomSRValidator;

/**
 * <p>
 * This class is an entire application for displaying and viewing images and
 * spectroscopy objects.
 * </p>
 * 
 * <p>
 * It supports a local database of DICOM objects, as well as the ability to read
 * a load from a DICOMDIR, and to query and retrieve objects across the network.
 * </p>
 * 
 * <p>
 * It is configured by use of a properties file that resides in the user's home
 * directory in <code>.com.pixelmed.display.DicomImageViewer.properties</code>.
 * </p>
 * 
 * @author dclunie
 */
public class DicomImageViewer extends ApplicationFrame implements
    KeyListener, MouseListener {
  private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomImageViewer.java,v 1.240 2017/01/24 10:50:40 dclunie Exp $";

  private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomImageViewer.class);

  /***/
  static final char screenSnapShotKeyChar = 'K';

  /***/
  static final String propertiesFileName = ".com.pixelmed.display.DicomImageViewer.properties";

  // property names ...

  /***/
  private static final String propertyName_DicomCurrentlySelectedStorageTargetAE = "Dicom.CurrentlySelectedStorageTargetAE";
  /***/
  private static final String propertyName_DicomCurrentlySelectedQueryTargetAE = "Dicom.CurrentlySelectedQueryTargetAE";

  /***/
  static final String propertyName_FullScreen = "Display.FullScreen";

  private DatabaseApplicationProperties databaseApplicationProperties = null;
  private NetworkApplicationProperties networkApplicationProperties = null;
  private NetworkApplicationInformation networkApplicationInformation = null;

  protected StoredFilePathStrategy storedFilePathStrategy = StoredFilePathStrategy.BYSOPINSTANCEUIDHASHSUBFOLDERS;

  protected File savedImagesFolder;
  protected String lastDirectoryPath;
  protected JPanel multiPanel;
  protected JList displayListOfPossibleReferenceImagesForImages;
  protected JList displayListOfPossibleBackgroundImagesForSpectra;
  protected JList displayListOfPossibleReferenceImagesForSpectra;
  protected JScrollPane databaseTreeScrollPane;
  protected JScrollPane dicomdirTreeScrollPane;
  protected JScrollPane scrollPaneOfCurrentAttributes;
  protected JScrollPane attributeFrameTableScrollPane;
  protected JScrollPane attributeTreeScrollPane;
  protected JScrollPane queryTreeScrollPane;
  protected JScrollPane structuredReportTreeScrollPane;

  protected SafeCursorChanger cursorChanger;

  public void quit() {
    // close of database and unregistering DNS services now done in
    // ShutdownHook.run()
    dispose();
    System.exit(0);
  }

  // implement KeyListener methods

  /**
   * @param e
   */
  @Override
  public void keyPressed(KeyEvent e) {
    // System.err.println("Key pressed event"+e);
  }

  /**
   * @param e
   */
  @Override
  public void keyReleased(KeyEvent e) {
    // System.err.println("Key released event"+e);
  }

  /**
   * @param e
   */
  @Override
  public void keyTyped(KeyEvent e) {
    // System.err.println("Key typed event "+e);
    // System.err.println("Key typed char "+e.getKeyChar());
    if (e.getKeyChar() == screenSnapShotKeyChar) {
      Rectangle extent = this.getBounds();
      File snapShotFile = takeSnapShot(extent);
      slf4jlogger.info("Snapshot to file {}", snapShotFile);
    }
  }

  /**
   * @param e
   */
  @Override
  public void mouseClicked(MouseEvent e) {
  }

  /**
   * @param e
   */
  @Override
  public void mouseEntered(MouseEvent e) {
    // System.err.println("mouseEntered event"+e);
    requestFocus(); // In order to allow us to receive KeyEvents
  }

  /**
   * @param e
   */
  @Override
  public void mouseExited(MouseEvent e) {
  }

  /**
   * @param e
   */
  @Override
  public void mousePressed(MouseEvent e) {
  }

  /**
   * @param e
   */
  @Override
  public void mouseReleased(MouseEvent e) {
  }

  // DicomImageViewer specific methods ...

  /**
   * Implement interface to status bar for utilities to log messages to.
   */
  private class OurMessageLogger implements MessageLogger {
    @Override
    public void send(String message) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(message));
    }

    @Override
    public void sendLn(String message) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(message));
    }
  }

  // private Font defaultFont;

  /***/
  private DatabaseInformationModel databaseInformationModel;

  /***/
  private static final int widthWantedForBrowser = 400; // check wide enough for
                                                        // FlowLayout buttons
                                                        // else they will be cut
                                                        // off
  /***/
  private static final int heightWantedForAttributeTable = 76;
  /***/
  private static final double browserAndMultiPaneAndCurrentAttributesResizeWeight = 0.9;
  /***/
  private static final Dimension defaultMultiPanelDimension = new Dimension(512, 512);

  /***/
  private int applicationWidth;
  /***/
  private int applicationHeight;

  /***/
  private int imagesPerRow;
  /***/
  private int imagesPerCol;

  /***/
  private ImageLocalizerManager imageLocalizerManager;
  /***/
  private SpectroscopyLocalizerManager spectroscopyLocalizerManager;

  // Stuff to support panel that displays contents of attributes of current
  // frame ...

  /***/
  private AttributeListFunctionalGroupsTableModelOneFrame modelOfCurrentAttributesForCurrentFrameBrowser;
  /***/
  private AttributeListTableBrowser tableOfCurrentAttributesForCurrentFrameBrowser;

  /***/
  private void createTableOfCurrentAttributesForCurrentFrameBrowser() {
    HashSet excludeList = new HashSet();
    excludeList.add(TagFromName.FileMetaInformationGroupLength);
    excludeList.add(TagFromName.ImplementationVersionName);
    excludeList.add(TagFromName.SourceApplicationEntityTitle);
    modelOfCurrentAttributesForCurrentFrameBrowser = new AttributeListFunctionalGroupsTableModelOneFrame(null, null, excludeList); // list
                                                                                                                                   // of
                                                                                                                                   // attributes
                                                                                                                                   // set
                                                                                                                                   // later
                                                                                                                                   // when
                                                                                                                                   // image
                                                                                                                                   // selected
    tableOfCurrentAttributesForCurrentFrameBrowser = new AttributeListTableBrowser(modelOfCurrentAttributesForCurrentFrameBrowser);
    tableOfCurrentAttributesForCurrentFrameBrowser.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // otherwise
                                                                                              // horizontal
                                                                                              // scroll
                                                                                              // doesn't
                                                                                              // work
  }

  /***/
  private AttributeListTableBrowser getTableOfCurrentAttributesForCurrentFrameBrowser() {
    return tableOfCurrentAttributesForCurrentFrameBrowser;
  }

  /***/
  private AttributeListFunctionalGroupsTableModelOneFrame getModelOfCurrentAttributesForCurrentFrameBrowser() {
    return modelOfCurrentAttributesForCurrentFrameBrowser;
  }

  // Stuff to support panel that displays contents of per-frame varying
  // attributes for all frames ...

  /***/
  private AttributeListFunctionalGroupsTableModelAllFrames modelOfCurrentAttributesForAllFramesBrowser;
  /***/
  private AttributeListTableBrowser tableOfCurrentAttributesForAllFramesBrowser;

  /***/
  private void createTableOfCurrentAttributesForAllFramesBrowser() {
    // HashSet excludeList = new HashSet();
    // excludeList.add(TagFromName.FileMetaInformationGroupLength);
    // excludeList.add(TagFromName.ImplementationVersionName);
    // excludeList.add(TagFromName.SourceApplicationEntityTitle);
    HashSet excludeList = null;
    modelOfCurrentAttributesForAllFramesBrowser = new AttributeListFunctionalGroupsTableModelAllFrames(null, null, excludeList); // list
                                                                                                                                 // of
                                                                                                                                 // attributes
                                                                                                                                 // set
                                                                                                                                 // later
                                                                                                                                 // when
                                                                                                                                 // image
                                                                                                                                 // selected
    tableOfCurrentAttributesForAllFramesBrowser = new AttributeListTableBrowser(modelOfCurrentAttributesForAllFramesBrowser);
    tableOfCurrentAttributesForAllFramesBrowser.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // otherwise
                                                                                           // horizontal
                                                                                           // scroll
                                                                                           // doesn't
                                                                                           // work
  }

  /***/
  private AttributeListTableBrowser getTableOfCurrentAttributesForAllFramesBrowser() {
    return tableOfCurrentAttributesForAllFramesBrowser;
  }

  /***/
  private AttributeListFunctionalGroupsTableModelAllFrames getModelOfCurrentAttributesForAllFramesBrowser() {
    return modelOfCurrentAttributesForAllFramesBrowser;
  }

  // Stuff to support references to things from within an instance

  /***/
  private DicomDirectory currentDicomDirectory;

  // implement FrameSelectionChangeListener to update attribute browser and
  // current frame for (new) localizer posting when frame changes ...

  /***/
  private OurFrameSelectionChangeListener mainPanelFrameSelectionChangeListener;

  // The following changes currentSourceIndex and
  // modelOfCurrentAttributesForCurrentFrameBrowser
  // so it is used ONLY for the main panel, and not the reference panel

  class OurFrameSelectionChangeListener extends SelfRegisteringListener {

    public OurFrameSelectionChangeListener(EventContext eventContext) {
      super("com.pixelmed.display.event.FrameSelectionChangeEvent", eventContext);
      // System.err.println("DicomImageViewer.OurFrameSelectionChangeListener():");
    }

    /**
     * @param e
     */
    @Override
    public void changed(com.pixelmed.event.Event e) {
      FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent) e;
      // System.err.println("DicomImageViewer.OurFrameSelectionChangeListener.changed():
      // event="+fse);
      currentSourceIndex = fse.getIndex(); // track this for when a new
                                           // localizer is selected for posting
                                           // - fix for [bugs.mrmf] (000074)
      // DO remap currentSourceIndex through currentSourceSortOrder
      if (currentSourceSortOrder != null) {
        currentSourceIndex = currentSourceSortOrder[currentSourceIndex];
      }
      getModelOfCurrentAttributesForCurrentFrameBrowser().selectValuesForDifferentFrame(currentSourceIndex);
      getTableOfCurrentAttributesForCurrentFrameBrowser().setColumnWidths();
    }
  }

  // implement FrameSortOrderChangeListener to update attribute browser when
  // sorted order changes ...

  /***/
  private OurFrameSortOrderChangeListener mainPanelFrameSortOrderChangeListener;

  class OurFrameSortOrderChangeListener extends SelfRegisteringListener {

    public OurFrameSortOrderChangeListener(EventContext eventContext) {
      super("com.pixelmed.display.event.FrameSortOrderChangeEvent", eventContext);
      // System.err.println("DicomImageViewer.OurFrameSortOrderChangeListener():");
    }

    /**
     * @param e
     */
    @Override
    public void changed(com.pixelmed.event.Event e) {
      FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent) e;
      // System.err.println("DicomImageViewer.OurFrameSortOrderChangeListener.changed():
      // event="+fso);
      currentSourceIndex = fso.getIndex(); // track this for when a new
                                           // localizer is selected for posting
                                           // - fix for [bugs.mrmf] (000074)
      currentSourceSortOrder = fso.getSortOrder();
      // DO NOT remap currentSourceIndex through currentSourceSortOrder
      // if (currentSourceSortOrder != null) {
      // currentSourceIndex=currentSourceSortOrder[currentSourceIndex];
      // }
      getModelOfCurrentAttributesForCurrentFrameBrowser().selectValuesForDifferentFrame(currentSourceIndex);
      getTableOfCurrentAttributesForCurrentFrameBrowser().setColumnWidths();
    }
  }

  // track these so that they are known when a new localizer is selected for
  // posting - fix for [bugs.mrmf] (000074)
  /***/
  private int currentSourceIndex; // This is the index BEFORE remapping through
                                  // currentSourceSortOrder
  /***/
  private int[] currentSourceSortOrder;

  // implement BrowserPaneChangeListener method ... events come from ourselves,
  // not elsewhere

  private JTabbedPane browserPane;
  private JPanel displayControlsPanel;
  private JPanel dicomdirControlsPanel;
  private JPanel databaseControlsPanel;
  private JPanel queryControlsPanel;
  private JPanel spectroscopyControlsPanel;
  private JPanel structuredReportTreeControlsPanel;

  private SourceImageVOILUTSelectorPanel sourceImageVOILUTSelectorPanel;
  private SourceImageWindowLinearCalculationSelectorPanel sourceImageWindowLinearCalculationSelectorPanel;
  private SourceImageWindowingAccelerationSelectorPanel sourceImageWindowingAccelerationSelectorPanel;
  private SourceImageGraphicDisplaySelectorPanel sourceImageGraphicDisplaySelectorPanel;
  private SourceImageShutterSelectorPanel sourceImageShutterSelectorPanel;

  private OurBrowserPaneChangeListener ourBrowserPaneChangeListener;

  class OurBrowserPaneChangeListener extends SelfRegisteringListener {

    public OurBrowserPaneChangeListener(EventContext eventContext) {
      super("com.pixelmed.display.event.BrowserPaneChangeEvent", eventContext);
      // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener():");
    }

    /**
     * @param e
     */
    @Override
    public void changed(com.pixelmed.event.Event e) {
      BrowserPaneChangeEvent bpce = (BrowserPaneChangeEvent) e;
      if (bpce.getType() == BrowserPaneChangeEvent.IMAGE) {
        // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener.changed()
        // to IMAGE");
        browserPane.setEnabledAt(browserPane.indexOfComponent(displayControlsPanel), true);
        browserPane.setSelectedComponent(displayControlsPanel);
        browserPane.setEnabledAt(browserPane.indexOfComponent(spectroscopyControlsPanel), false);
        browserPane.setEnabledAt(browserPane.indexOfComponent(structuredReportTreeControlsPanel), false);
      } else if (bpce.getType() == BrowserPaneChangeEvent.DICOMDIR) {
        // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener.changed()
        // to DICOMDIR");
        browserPane.setSelectedComponent(dicomdirControlsPanel);
      } else if (bpce.getType() == BrowserPaneChangeEvent.DATABASE) {
        // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener.changed()
        // to DATABASE");
        browserPane.setSelectedComponent(databaseControlsPanel);
        // will trigger loading of database on detection of selection changed
      } else if (bpce.getType() == BrowserPaneChangeEvent.SPECTROSCOPY) {
        // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener.changed()
        // to SPECTROSCOPY");
        browserPane.setEnabledAt(browserPane.indexOfComponent(spectroscopyControlsPanel), true);
        browserPane.setSelectedComponent(spectroscopyControlsPanel);
        browserPane.setEnabledAt(browserPane.indexOfComponent(displayControlsPanel), false);
        browserPane.setEnabledAt(browserPane.indexOfComponent(structuredReportTreeControlsPanel), false);
      } else if (bpce.getType() == BrowserPaneChangeEvent.SR) {
        // System.err.println("DicomImageViewer.OurBrowserPaneChangeListener.changed()
        // to SR");
        browserPane.setEnabledAt(browserPane.indexOfComponent(structuredReportTreeControlsPanel), true);
        browserPane.setSelectedComponent(structuredReportTreeControlsPanel);
        browserPane.setEnabledAt(browserPane.indexOfComponent(displayControlsPanel), false);
        browserPane.setEnabledAt(browserPane.indexOfComponent(spectroscopyControlsPanel), false);
      }
    }
  }

  // methods to do the work ...

  /**
   * @param attributeList
   */
  private GeometryOfVolume getNewGeometryOfVolume(AttributeList attributeList) {
    GeometryOfVolume imageGeometry = null;
    try {
      imageGeometry = new GeometryOfVolumeFromAttributeList(attributeList);
    } catch (Throwable e) { // NoClassDefFoundError may be thrown if no vecmath
                            // support available, which is an Error, not an
                            // Exception
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(e.toString()));
      slf4jlogger.error("", e);
    }
    return imageGeometry;
  }

  /**
   * @param attributeList
   */
  private SpectroscopyVolumeLocalization getNewSpectroscopyVolumeLocalization(AttributeList attributeList) {
    SpectroscopyVolumeLocalization spectroscopyVolumeLocalization = null;
    try {
      spectroscopyVolumeLocalization = new SpectroscopyVolumeLocalization(attributeList);
    } catch (DicomException e) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(e.toString()));
      slf4jlogger.error("", e);
    }
    return spectroscopyVolumeLocalization;
  }

  /**
   * @param dicomFileName
   */
  private void loadBackgroundImageForSpectra(String dicomFileName) {
    AttributeList list = new AttributeList();
    SourceImage sImg = null;
    try {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Loading background image ..."));
      File file = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
      DicomInputStream i = new DicomInputStream(file);
      list.read(i);
      i.close();
      String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list, TagFromName.SOPClassUID);
      if (SOPClass.isImageStorage(sopClassUID)) {
        sImg = new SourceImage(list);
      }
    } catch (Exception e) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(e.toString()));
      slf4jlogger.error("", e);
    } finally {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
    }

    if (sImg != null) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
          new SourceImageSelectionChangeEvent(WellKnownContext.SPECTROSCOPYBACKGROUNDIMAGE, sImg, null/*
                                                                                                       * sortOrder
                                                                                                       */, 0, list, getNewGeometryOfVolume(list)));
    }
  }

  /**
   * @param dicomFileName
   * @param referenceImagePanel
   * @param spectroscopy
   */
  private void loadReferenceImagePanel(String dicomFileName, JPanel referenceImagePanel, boolean spectroscopy) {
    AttributeList list = new AttributeList();
    SourceImage sImg = null;
    try {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Loading referenced image ..."));
      File file = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
      DicomInputStream i = new DicomInputStream(file);
      list.read(i);
      i.close();
      String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list, TagFromName.SOPClassUID);
      if (SOPClass.isImageStorage(sopClassUID)) {
        // referenceImagePanel.removeAll();
        sImg = new SourceImage(list);
      }
    } catch (Exception e) {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(e.toString()));
      slf4jlogger.error("", e);
    } finally {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
    }

    if (sImg != null && sImg.getNumberOfBufferedImages() > 0) {
      GeometryOfVolume imageGeometry = getNewGeometryOfVolume(list);

      SingleImagePanel ip = new SingleImagePanel(sImg, WellKnownContext.REFERENCEPANEL, imageGeometry);

      ip.setOrientationAnnotations(new OrientationAnnotations(list, imageGeometry), "SansSerif", Font.PLAIN, 8, Color.pink);

      SingleImagePanel.deconstructAllSingleImagePanelsInContainer(referenceImagePanel);
      referenceImagePanel.removeAll();
      referenceImagePanel.add(ip);
      // imagePanel[0]=ip;

      if (spectroscopy) {
        spectroscopyLocalizerManager.setReferenceImagePanel(ip);
      } else {
        imageLocalizerManager.setReferenceImagePanel(ip);
      }

      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
          new SourceImageSelectionChangeEvent(WellKnownContext.REFERENCEPANEL, sImg, null/*
                                                                                          * sortOrder
                                                                                          */, 0, list, imageGeometry));

      // One must now reselect the current frame to trigger the current (rather
      // than the first) frame to be shown on the localizer
      //
      // This is fix for [bugs.mrmf] (000074)
      //
      // The values for currentSourceSortOrder,currentSourceIndex
      // have been cached by the DicomImageViewer class whenever it receives a
      // FrameSelectionChangeEvent or FrameSortOrderChangeEvent
      //
      // NB. It is important that currentSourceIndex NOT have been remapped
      // through currentSourceSortOrder yet

      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
          new FrameSelectionChangeEvent(WellKnownContext.MAINPANEL, currentSourceIndex));
    }
  }

  /**
   * @param multiPanel
   * @param sImg
   * @param list
   */
  private void loadMultiPanelWithImage(JPanel multiPanel, SourceImage sImg, AttributeList list) {
    // System.err.println("DicomImageViewer.loadMultiPanelWithImage():");
    if (sImg != null && sImg.getNumberOfBufferedImages() > 0) {
      GeometryOfVolume imageGeometry = getNewGeometryOfVolume(list);
      PixelSpacing pixelSpacing = new PixelSpacing(list, imageGeometry);
      // SingleImagePanel ip = new
      // SingleImagePanel(sImg,WellKnownContext.MAINPANEL,imageGeometry);
      SingleImagePanel ip = new SingleImagePanelWithLineDrawing(sImg, WellKnownContext.MAINPANEL, imageGeometry);
      // SingleImagePanel ip = new
      // SingleImagePanelWithRegionDetection(sImg,WellKnownContext.MAINPANEL,imageGeometry);
      ip.setPixelSpacingInSourceImage(pixelSpacing.getSpacing(), pixelSpacing.getDescription());

      ip.setDemographicAndTechniqueAnnotations(new DemographicAndTechniqueAnnotations(list, imageGeometry), "SansSerif", Font.PLAIN, 10, Color.pink);
      ip.setOrientationAnnotations(new OrientationAnnotations(list, imageGeometry), "SansSerif", Font.PLAIN, 20, Color.pink);

      sourceImageVOILUTSelectorPanel.sendEventCorrespondingToCurrentButtonState(); // will
                                                                                   // get
                                                                                   // to
                                                                                   // new
                                                                                   // SingleImagePanel
                                                                                   // via
                                                                                   // MainImagePanelVOILUTSelectionEventSink
      sourceImageWindowLinearCalculationSelectorPanel.sendEventCorrespondingToCurrentButtonState();
      sourceImageWindowingAccelerationSelectorPanel.sendEventCorrespondingToCurrentButtonState();
      sourceImageGraphicDisplaySelectorPanel.sendEventCorrespondingToCurrentButtonState();

      SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
      SpectraPanel.deconstructAllSpectraPanelsInContainer(multiPanel);
      multiPanel.removeAll();
      multiPanel.add(ip);
      // imagePanel[0]=ip;

      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
          new SourceImageSelectionChangeEvent(WellKnownContext.MAINPANEL, sImg, null/*
                                                                                     * sortOrder
                                                                                     */, 0, list, imageGeometry));
    }
  }

  /**
   * @param multiPanel
   * @param sSpectra
   * @param list
   */
  private Dimension loadMultiPanelWithSpectra(JPanel multiPanel, SourceSpectra sSpectra, AttributeList list) {
    // System.err.println("loadMultiPanelWithSpectra:");
    Dimension multiPanelDimension = null;
    if (sSpectra != null) {
      float[][] spectra = sSpectra.getSpectra();

      GeometryOfVolume spectroscopyGeometry = getNewGeometryOfVolume(list);
      SpectroscopyVolumeLocalization spectroscopyVolumeLocalization = getNewSpectroscopyVolumeLocalization(list);

      SpectraPanel sp = new SpectraPanel(spectra, sSpectra.getRows(), sSpectra.getColumns(), sSpectra.getMinimum(), sSpectra.getMaximum(),
          spectroscopyGeometry, spectroscopyVolumeLocalization,
          WellKnownContext.MAINPANEL, WellKnownContext.SPECTROSCOPYBACKGROUNDIMAGE);

      SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
      SpectraPanel.deconstructAllSpectraPanelsInContainer(multiPanel);
      multiPanel.removeAll();
      multiPanel.add(sp);

      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
          new SourceSpectrumSelectionChangeEvent(WellKnownContext.MAINPANEL, spectra, spectra.length, null, 0, list, spectroscopyGeometry, spectroscopyVolumeLocalization));
    }
    return multiPanelDimension;
  }

  // keep track of the query information model in use ...

  /***/
  private QueryInformationModel currentRemoteQueryInformationModel;

  /**
   * @param remoteAEForQuery
   * @param browserPane
   * @param tabNumberOfRemoteInBrowserPane
   */
  private void setCurrentRemoteQueryInformationModel(String remoteAEForQuery, JTabbedPane browserPane, int tabNumberOfRemoteInBrowserPane) {
    currentRemoteQueryInformationModel = null;
    String stringForTitle = "";
    if (remoteAEForQuery != null && remoteAEForQuery.length() > 0 && networkApplicationProperties != null && networkApplicationInformation != null) {
      try {
        String queryCallingAETitle = networkApplicationProperties.getCallingAETitle();
        String queryCalledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(remoteAEForQuery);
        PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(queryCalledAETitle);

        if (presentationAddress == null) {
          throw new Exception("For remote query AE <" + remoteAEForQuery + ">, presentationAddress cannot be determined");
        }

        String queryHost = presentationAddress.getHostname();
        int queryPort = presentationAddress.getPort();
        String queryModel = networkApplicationInformation.getApplicationEntityMap().getQueryModel(queryCalledAETitle);

        if (NetworkApplicationProperties.isStudyRootQueryModel(queryModel) || queryModel == null) {
          currentRemoteQueryInformationModel = new StudyRootQueryInformationModel(queryHost, queryPort, queryCalledAETitle, queryCallingAETitle);
          stringForTitle = ":" + remoteAEForQuery;
        } else {
          throw new Exception("For remote query AE <" + remoteAEForQuery + ">, query model " + queryModel + " not supported");
        }
      } catch (Exception e) { // if an AE's property has no value, or model not
                              // supported
        slf4jlogger.error("", e);
      }
    }
    if (browserPane != null) {
      browserPane.setTitleAt(tabNumberOfRemoteInBrowserPane, "Remote" + stringForTitle);
    }
    // System.err.println("DicomImageViewer.setCurrentRemoteQueryInformationModel():
    // now "+currentRemoteQueryInformationModel);
  }

  /***/
  private QueryInformationModel getCurrentRemoteQueryInformationModel() {
    return currentRemoteQueryInformationModel;
  }

  // keep track of current filter for use for queries

  /***/
  private AttributeList currentRemoteQueryFilter;

  /**
   * @param filter
   */
  private void setCurrentRemoteQueryFilter(AttributeList filter) {
    currentRemoteQueryFilter = filter;
  }

  /***/
  private AttributeList getCurrentRemoteQueryFilter() {
    // System.err.println("DicomImageViewer.getCurrentRemoteQueryFilter(): now
    // "+currentRemoteQueryFilter);
    return currentRemoteQueryFilter;
  }

  /***/
  private void initializeCurrentRemoteQueryFilter() {
    AttributeList filter = new AttributeList();
    setCurrentRemoteQueryFilter(filter);

    // specific character set is established and inserted later, when text
    // values have been entered into the filter panel

    {
      AttributeTag t = TagFromName.PatientName;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientID;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.PatientBirthDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientSex;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientBirthTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }
    // kills Leonardo ... { AttributeTag t = TagFromName.OtherPatientIDs;
    // Attribute a = new LongStringAttribute(t); filter.put(t,a); }
    // kills Leonardo ... { AttributeTag t = TagFromName.OtherPatientNames;
    // Attribute a = new PersonNameAttribute(t); filter.put(t,a); }
    // kills Leonardo ... { AttributeTag t = TagFromName.EthnicGroup; Attribute
    // a = new ShortStringAttribute(t); filter.put(t,a); }
    {
      AttributeTag t = TagFromName.PatientComments;
      Attribute a = new LongTextAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.StudyID;
      Attribute a = new ShortStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.StudyDescription;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.OtherStudyNumbers;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PerformedProcedureStepID;
      Attribute a = new ShortStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PerformedProcedureStepStartDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PerformedProcedureStepStartTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.SOPClassesInStudy;
      Attribute a = new UniqueIdentifierAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ModalitiesInStudy;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.StudyDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.StudyTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ReferringPhysicianName;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AccessionNumber;
      Attribute a = new ShortStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PhysiciansOfRecord;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.NameOfPhysiciansReadingStudy;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AdmittingDiagnosesDescription;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientAge;
      Attribute a = new AgeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientSize;
      Attribute a = new DecimalStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PatientWeight;
      Attribute a = new DecimalStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Occupation;
      Attribute a = new ShortStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AdditionalPatientHistory;
      Attribute a = new LongTextAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.SeriesDescription;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.SeriesNumber;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Modality;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.SeriesDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.SeriesTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.PerformingPhysicianName;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ProtocolName;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.OperatorsName;
      Attribute a = new PersonNameAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Laterality;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.BodyPartExamined;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Manufacturer;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ManufacturerModelName;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.StationName;
      Attribute a = new ShortStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.InstitutionName;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.InstitutionalDepartmentName;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.InstanceNumber;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ImageComments;
      Attribute a = new LongTextAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.ContentDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ContentTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ImageType;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AcquisitionNumber;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AcquisitionDate;
      Attribute a = new DateAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AcquisitionTime;
      Attribute a = new TimeAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AcquisitionDateTime;
      Attribute a = new DateTimeAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.DerivationDescription;
      Attribute a = new ShortTextAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.QualityControlImage;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.BurnedInAnnotation;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.LossyImageCompression;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.LossyImageCompressionRatio;
      Attribute a = new DecimalStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.LossyImageCompressionMethod;
      Attribute a = new CodeStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.ContrastBolusAgent;
      Attribute a = new LongStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.NumberOfFrames;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Rows;
      Attribute a = new UnsignedShortAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Columns;
      Attribute a = new UnsignedShortAttribute(t);
      filter.put(t, a);
    }

    {
      AttributeTag t = TagFromName.StudyInstanceUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.SeriesInstanceUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.SOPInstanceUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.SOPClassUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      filter.put(t, a);
    }

    // Always good to insert these ... avoids premature nested query just to
    // find number of node children in browser ...
    {
      AttributeTag t = TagFromName.NumberOfStudyRelatedInstances;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.NumberOfStudyRelatedSeries;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
    {
      AttributeTag t = TagFromName.NumberOfSeriesRelatedInstances;
      Attribute a = new IntegerStringAttribute(t);
      filter.put(t, a);
    }
  }

  // keep track of current remote database selection in case someone wants to
  // retrieve it ...

  /***/
  private QueryTreeRecord currentRemoteQuerySelectionQueryTreeRecord;
  /***/
  private AttributeList currentRemoteQuerySelectionUniqueKeys;
  /***/
  private Attribute currentRemoteQuerySelectionUniqueKey;
  /***/
  private String currentRemoteQuerySelectionRetrieveAE;
  /***/
  private String currentRemoteQuerySelectionLevel;

  /**
   * @param uniqueKeys
   * @param uniqueKey
   * @param identifier
   */
  private void setCurrentRemoteQuerySelection(AttributeList uniqueKeys, Attribute uniqueKey, AttributeList identifier) {
    currentRemoteQuerySelectionUniqueKeys = uniqueKeys;
    currentRemoteQuerySelectionUniqueKey = uniqueKey;
    currentRemoteQuerySelectionRetrieveAE = null;
    if (identifier != null) {
      Attribute aRetrieveAETitle = identifier.get(TagFromName.RetrieveAETitle);
      if (aRetrieveAETitle != null)
        currentRemoteQuerySelectionRetrieveAE = aRetrieveAETitle.getSingleStringValueOrNull();
    }
    if (currentRemoteQuerySelectionRetrieveAE == null) {
      // it is legal for RetrieveAETitle to be zero length at all but the lowest
      // levels of
      // the query model :( (See PS 3.4 C.4.1.1.3.2)
      // (so far the Leonardo is the only one that doesn't send it at all
      // levels)
      // we could recurse down to the lower levels and get the union of the
      // value there
      // but lets just keep it simple and ...
      // default to whoever it was we queried in the first place ...
      QueryInformationModel model = getCurrentRemoteQueryInformationModel();
      if (model != null)
        currentRemoteQuerySelectionRetrieveAE = model.getCalledAETitle();
    }
    currentRemoteQuerySelectionLevel = null;
    if (identifier != null) {
      Attribute a = identifier.get(TagFromName.QueryRetrieveLevel);
      if (a != null) {
        currentRemoteQuerySelectionLevel = a.getSingleStringValueOrNull();
      }
    }
    if (currentRemoteQuerySelectionLevel == null) {
      // QueryRetrieveLevel must have been (erroneously) missing in query
      // response ... see with Dave Harvey's code on public server
      // so try to guess it from unique key in tree record
      // Fixes [bugs.mrmf] (000224) Missing query/retrieve level in C-FIND
      // response causes tree select and retrieve to fail
      if (uniqueKey != null) {
        AttributeTag tag = uniqueKey.getTag();
        if (tag != null) {
          if (tag.equals(TagFromName.PatientID)) {
            currentRemoteQuerySelectionLevel = "PATIENT";
          } else if (tag.equals(TagFromName.StudyInstanceUID)) {
            currentRemoteQuerySelectionLevel = "STUDY";
          } else if (tag.equals(TagFromName.SeriesInstanceUID)) {
            currentRemoteQuerySelectionLevel = "SERIES";
          } else if (tag.equals(TagFromName.SOPInstanceUID)) {
            currentRemoteQuerySelectionLevel = "IMAGE";
          }
        }
      }
      slf4jlogger.info("DicomImageViewer.setCurrentRemoteQuerySelection(): Guessed missing currentRemoteQuerySelectionLevel to be {}", currentRemoteQuerySelectionLevel);
    }
  }

  /***/
  private QueryTreeRecord getCurrentRemoteQuerySelectionQueryTreeRecord() {
    return currentRemoteQuerySelectionQueryTreeRecord;
  }

  /***/
  private void setCurrentRemoteQuerySelectionQueryTreeRecord(QueryTreeRecord r) {
    currentRemoteQuerySelectionQueryTreeRecord = r;
  }

  /***/
  private AttributeList getCurrentRemoteQuerySelectionUniqueKeys() {
    return currentRemoteQuerySelectionUniqueKeys;
  }

  /***/
  private Attribute getCurrentRemoteQuerySelectionUniqueKey() {
    return currentRemoteQuerySelectionUniqueKey;
  }

  /***/
  private String getCurrentRemoteQuerySelectionRetrieveAE() {
    return currentRemoteQuerySelectionRetrieveAE;
  }

  /***/
  private String getCurrentRemoteQuerySelectionLevel() {
    return currentRemoteQuerySelectionLevel;
  }

  // Keep track of what is currently selected (e.g. in DICOMDIR) in case someone
  // wants to load it ...

  /***/
  private Vector currentFilePathSelections;

  /**
   * @param filePathSelections
   */
  private void setCurrentFilePathSelection(Vector filePathSelections) {
    currentFilePathSelections = filePathSelections;
  }

  /***/
  private String getCurrentFilePathSelection() {
    return (currentFilePathSelections != null && currentFilePathSelections.size() > 0) ? (String) (currentFilePathSelections.get(0)) : null;
  }

  /***/
  private Vector getCurrentFilePathSelections() {
    return currentFilePathSelections;
  }

  private DatabaseTreeRecord[] currentDatabaseTreeRecordSelections;

  private void setCurrentDatabaseTreeRecordSelections(DatabaseTreeRecord[] records) {
    currentDatabaseTreeRecordSelections = records;
  }

  private DatabaseTreeRecord[] getCurrentDatabaseTreeRecordSelections() {
    return currentDatabaseTreeRecordSelections;
  }

  // Keep track of what is currently actually loaded (e.g. in display) in case
  // someone wants to import it into the database ...

  /***/
  private String currentlyDisplayedInstanceFilePath;

  /**
   * @param path
   */
  private void setCurrentlyDisplayedInstanceFilePath(String path) {
    currentlyDisplayedInstanceFilePath = path;
  }

  /***/
  private String getCurrentlyDisplayedInstanceFilePath() {
    return currentlyDisplayedInstanceFilePath;
  }

  /***/
  private AttributeList currentAttributeListForDatabaseImport;

  /**
   * @param list
   */
  private void setAttributeListForDatabaseImport(AttributeList list) {
    currentAttributeListForDatabaseImport = list;
  }

  /***/
  private AttributeList getAttributeListForDatabaseImport() {
    return currentAttributeListForDatabaseImport;
  }

  /***/
  private class OurDicomDirectoryBrowser extends DicomDirectoryBrowser {
    /**
     * @param list
     * @param imagePanel
     * @param referenceImagePanelForImages
     * @param referenceImagePanelForSpectra
     * @throws DicomException
     */
    public OurDicomDirectoryBrowser(AttributeList list) throws DicomException {
      super(list, lastDirectoryPath, dicomdirTreeScrollPane, scrollPaneOfCurrentAttributes);
    }

    /**
     * @param paths
     */
    @Override
    protected void doSomethingWithSelectedFiles(Vector paths) {
      setCurrentFilePathSelection(paths);
    }

    /**
     */
    @Override
    protected void doSomethingMoreWithWhateverWasSelected() {
      // System.err.println("DicomImageViewer.OurDicomDirectoryBrowser.doSomethingMoreWithWhateverWasSelected():");
      String dicomFileName = getCurrentFilePathSelection();
      if (dicomFileName != null) {
        loadDicomFileOrDirectory(dicomFileName, multiPanel,
            referenceImagePanelForImages,
            referenceImagePanelForSpectra);
      }
    }
  }

  /***/
  private class OurDatabaseTreeBrowser extends DatabaseTreeBrowser {
    /**
     * @throws DicomException
     */
    public OurDatabaseTreeBrowser() throws DicomException {
      super(databaseInformationModel, databaseTreeScrollPane, scrollPaneOfCurrentAttributes);
    }

    @Override
    protected boolean doSomethingWithSelections(DatabaseTreeRecord[] selections) {
      setCurrentDatabaseTreeRecordSelections(selections);
      return false; // still want to call doSomethingWithSelectedFiles()
    }

    /**
     * @param paths
     */
    @Override
    protected void doSomethingWithSelectedFiles(Vector paths) {
      setCurrentFilePathSelection(paths);
    }

    /**
     */
    @Override
    protected void doSomethingMoreWithWhateverWasSelected() {
      // System.err.println("DicomImageViewer.OurDatabaseTreeBrowser.doSomethingMoreWithWhateverWasSelected():");
      String dicomFileName = getCurrentFilePathSelection();
      if (dicomFileName != null) {
        loadDicomFileOrDirectory(dicomFileName, multiPanel,
            referenceImagePanelForImages,
            referenceImagePanelForSpectra);
      }
    }
  }

  /***/
  private class OurQueryTreeBrowser extends QueryTreeBrowser {
    /**
     * @param q
     * @param m
     * @param treeBrowserScrollPane
     * @param attributeBrowserScrollPane
     * @throws DicomException
     */
    OurQueryTreeBrowser(QueryInformationModel q, QueryTreeModel m, JScrollPane treeBrowserScrollPane, JScrollPane attributeBrowserScrollPane) throws DicomException {
      super(q, m, treeBrowserScrollPane, attributeBrowserScrollPane);
    }

    /***/
    @Override
    protected TreeSelectionListener buildTreeSelectionListenerToDoSomethingWithSelectedLevel() {
      return new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent tse) {
          TreePath tp = tse.getNewLeadSelectionPath();
          if (tp != null) {
            Object lastPathComponent = tp.getLastPathComponent();
            if (lastPathComponent instanceof QueryTreeRecord) {
              QueryTreeRecord r = (QueryTreeRecord) lastPathComponent;
              setCurrentRemoteQuerySelection(r.getUniqueKeys(), r.getUniqueKey(), r.getAllAttributesReturnedInIdentifier());
              setCurrentRemoteQuerySelectionQueryTreeRecord(r);
            }
          }
        }
      };
    }
  }

  private void loadDicomFileOrDirectory(String dicomFileName) {
    loadDicomFileOrDirectory(dicomFileName, multiPanel,
        referenceImagePanelForImages,
        referenceImagePanelForSpectra);
  }

  /**
   * @param dicomFileName
   * @param imagePanel
   * @param referenceImagePanelForImages
   * @param referenceImagePanelForSpectra
   */
  private void loadDicomFileOrDirectory(
      String dicomFileName, JPanel imagePanel,
      JPanel referenceImagePanelForImages,
      JPanel referenceImagePanelForSpectra) {
    // remove currently displayed image, current frame attributes, attribute
    // tree and frame able in case load fails
    // i.e. don't leave stuff from last object loaded hanging around
    // NB. The exception is the DICOMDIR ... if one tries and fails to load
    // a new DICOMDIR, the old contents will not be erased, since otherwise
    // would
    // remove the DICOMDIR when an image load fails ... would be irritating
    // (can't know the new object would be a DICOMDIR unless load and parse
    // succeeds)

    // ApplicationEventDispatcher.getApplicationEventDispatcher().removeAllListenersForEventContext(WellKnownContext.MAINPANEL);
    // ApplicationEventDispatcher.getApplicationEventDispatcher().removeAllListenersForEventContext(WellKnownContext.REFERENCEPANEL);
    SingleImagePanel.deconstructAllSingleImagePanelsInContainer(imagePanel);
    SpectraPanel.deconstructAllSpectraPanelsInContainer(imagePanel);
    imagePanel.removeAll();
    imagePanel.repaint();
    SingleImagePanel.deconstructAllSingleImagePanelsInContainer(referenceImagePanelForImages);
    referenceImagePanelForImages.removeAll();
    referenceImagePanelForImages.repaint();
    SingleImagePanel.deconstructAllSingleImagePanelsInContainer(referenceImagePanelForSpectra);
    referenceImagePanelForSpectra.removeAll();
    referenceImagePanelForSpectra.repaint();

    imageLocalizerManager.reset();
    spectroscopyLocalizerManager.reset();

    scrollPaneOfCurrentAttributes.setViewportView(null);
    scrollPaneOfCurrentAttributes.repaint();
    attributeTreeScrollPane.setViewportView(null);
    attributeTreeScrollPane.repaint();
    attributeFrameTableScrollPane.setViewportView(null);
    attributeFrameTableScrollPane.repaint();
    structuredReportTreeScrollPane.setViewportView(null);
    structuredReportTreeScrollPane.repaint();

    setAttributeListForDatabaseImport(null);
    setCurrentlyDisplayedInstanceFilePath(null);

    if (dicomFileName != null) {
      cursorChanger.setWaitCursor();
      try {
        slf4jlogger.info("Open: {}", dicomFileName);
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Reading and parsing DICOM file ..."));
        File file = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
        dicomFileName = file.getAbsolutePath(); // set to what we actually used,
                                                // since may be kept around for
                                                // later imports, etc.
        boolean deferredDecompression = CompressedFrameDecoder.canDecompress(file);
        slf4jlogger.info("DicomImageViewer(): deferredDecompression = {}", deferredDecompression);
        DicomInputStream i = new DicomInputStream(file);
        AttributeList list = new AttributeList();
        // long startTime = System.currentTimeMillis();
        list.setDecompressPixelData(!deferredDecompression);
        list.read(i);
        i.close();
        // long currentTime = System.currentTimeMillis();
        // System.err.println("DicomImageViewer.loadDicomFileOrDirectory():
        // reading AttributeList took = "+(currentTime-startTime)+" ms");
        // startTime=currentTime;
        new AttributeTreeBrowser(list, attributeTreeScrollPane);
        // currentTime = System.currentTimeMillis();
        // System.err.println("DicomImageViewer.loadDicomFileOrDirectory():
        // making AttributeTreeBrowser took = "+(currentTime-startTime)+" ms");
        // choose type of object based on SOP Class
        // Note that DICOMDIRs don't have SOPClassUID, so check
        // MediaStorageSOPClassUID first
        // then only if not found (e.g. and image with no meta-header, use
        // SOPClassUID from SOP Common Module
        Attribute a = list.get(TagFromName.MediaStorageSOPClassUID);
        String useSOPClassUID = (a != null && a.getVM() == 1) ? a.getStringValues()[0] : null;
        if (useSOPClassUID == null) {
          a = list.get(TagFromName.SOPClassUID);
          useSOPClassUID = (a != null && a.getVM() == 1) ? a.getStringValues()[0] : null;
        }

        if (useSOPClassUID == null) {
          throw new DicomException("Missing SOP Class UID");
        } else if (SOPClass.isDirectory(useSOPClassUID)) {
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Building tree from DICOMDIR ..."));
          OurDicomDirectoryBrowser dicomdirBrowser = new OurDicomDirectoryBrowser(list);
          currentDicomDirectory = dicomdirBrowser.getDicomDirectory(); // need
                                                                       // access
                                                                       // to
                                                                       // this
                                                                       // later
                                                                       // for
                                                                       // referenced
                                                                       // stuff
                                                                       // handling
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new BrowserPaneChangeEvent(WellKnownContext.MAINPANEL, BrowserPaneChangeEvent.DICOMDIR));
        } else if (SOPClass.isImageStorage(useSOPClassUID) || useSOPClassUID.equals(SOPClass.RTDoseStorage)) {
          // imagePanel.removeAll();
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Building images ..."));
          SourceImage sImg = new SourceImage(list);
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Loading images and attributes ..."));

          currentSourceIndex = 0;
          currentSourceSortOrder = null;
          getModelOfCurrentAttributesForCurrentFrameBrowser().initializeModelFromAttributeList(list);
          getModelOfCurrentAttributesForCurrentFrameBrowser().selectValuesForDifferentFrame(currentSourceIndex);
          getTableOfCurrentAttributesForCurrentFrameBrowser().setColumnWidths();
          scrollPaneOfCurrentAttributes.setViewportView(getTableOfCurrentAttributesForCurrentFrameBrowser());
          getModelOfCurrentAttributesForAllFramesBrowser().initializeModelFromAttributeList(list);
          getTableOfCurrentAttributesForAllFramesBrowser().setColumnWidths();
          attributeFrameTableScrollPane.setViewportView(getTableOfCurrentAttributesForAllFramesBrowser());

          loadMultiPanelWithImage(imagePanel, sImg, list);

          referenceImageListMappedToFilenames = getImageListMappedToFilenamesForReferenceOrBackground(list, false);
          displayListOfPossibleReferenceImagesForImages.setListData(referenceImageListMappedToFilenames.keySet().toArray());
          // imagePanel.revalidate();
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new BrowserPaneChangeEvent(WellKnownContext.MAINPANEL, BrowserPaneChangeEvent.IMAGE));
          setAttributeListForDatabaseImport(list); // warning ... this will keep
                                                   // bulk data hanging around
                                                   // :(
          setCurrentlyDisplayedInstanceFilePath(dicomFileName);
          // set the current selection path in case we want to import or
          // transfer the file we have just loaded
          Vector names = new Vector();
          names.add(dicomFileName);
          setCurrentFilePathSelection(names);
        } else if (SOPClass.isSpectroscopy(useSOPClassUID)) {
          // imagePanel.removeAll();
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Building spectra ..."));
          SourceSpectra sSpectra = new SourceSpectra(list);
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Loading spectra and attributes ..."));

          currentSourceIndex = 0;
          currentSourceSortOrder = null;
          getModelOfCurrentAttributesForCurrentFrameBrowser().initializeModelFromAttributeList(list);
          getModelOfCurrentAttributesForCurrentFrameBrowser().selectValuesForDifferentFrame(currentSourceIndex);
          getTableOfCurrentAttributesForCurrentFrameBrowser().setColumnWidths();
          scrollPaneOfCurrentAttributes.setViewportView(getTableOfCurrentAttributesForCurrentFrameBrowser());
          getModelOfCurrentAttributesForAllFramesBrowser().initializeModelFromAttributeList(list);
          getTableOfCurrentAttributesForAllFramesBrowser().setColumnWidths();
          attributeFrameTableScrollPane.setViewportView(getTableOfCurrentAttributesForAllFramesBrowser());

          loadMultiPanelWithSpectra(imagePanel, sSpectra, list);

          referenceImageListMappedToFilenames = getImageListMappedToFilenamesForReferenceOrBackground(list, false);
          displayListOfPossibleReferenceImagesForSpectra.setListData(referenceImageListMappedToFilenames.keySet().toArray());
          backgroundImageListMappedToFilenames = getImageListMappedToFilenamesForReferenceOrBackground(list, true);
          displayListOfPossibleBackgroundImagesForSpectra.setListData(backgroundImageListMappedToFilenames.keySet().toArray());
          // imagePanel.revalidate();
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new BrowserPaneChangeEvent(WellKnownContext.MAINPANEL, BrowserPaneChangeEvent.SPECTROSCOPY));
          setAttributeListForDatabaseImport(list); // warning ... this will keep
                                                   // bulk data hanging around
                                                   // :(
          setCurrentlyDisplayedInstanceFilePath(dicomFileName);
          // set the current selection path in case we want to import or
          // transfer the file we have just loaded
          Vector names = new Vector();
          names.add(dicomFileName);
          setCurrentFilePathSelection(names);
        } else if (SOPClass.isStructuredReport(useSOPClassUID) || list.isSRDocument()) {
          // System.err.println("DicomImageViewer.loadDicomFileOrDirectory():
          // SOPClass.isStructuredReport or AttributeList.isSRDocument()");
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Building SR ..."));
          // StructuredReport sSR = new StructuredReport(list);
          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Loading SR and attributes ..."));

          new StructuredReportTreeBrowser(list, structuredReportTreeScrollPane);

          ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new BrowserPaneChangeEvent(WellKnownContext.MAINPANEL, BrowserPaneChangeEvent.SR));
          setAttributeListForDatabaseImport(list); // warning ... this will keep
                                                   // bulk data hanging around
                                                   // :(
          setCurrentlyDisplayedInstanceFilePath(dicomFileName);
          // set the current selection path in case we want to import or
          // transfer the file we have just loaded
          Vector names = new Vector();
          names.add(dicomFileName);
          setCurrentFilePathSelection(names);
        } else if (SOPClass.isNonImageStorage(useSOPClassUID)) {
          throw new DicomException("unsupported storage SOP Class " + useSOPClassUID);
        } else {
          throw new DicomException("unsupported SOP Class " + useSOPClassUID);
        }
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      } catch (Exception e) {
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(e.toString()));
        slf4jlogger.error("", e);
      }
      // make label really wide, else doesn't completely repaint on status
      // update
      cursorChanger.restoreCursor();
    }
  }

  /**
   * @param imagePanel
   * @param referenceImagePanelForImages
   * @param referenceImagePanelForSpectra
   */
  private void callFileChooserThenLoadDicomFileOrDirectory(
      JPanel imagePanel,
      JPanel referenceImagePanelForImages,
      JPanel referenceImagePanelForSpectra) {

    String dicomFileName = null;
    {
      SafeFileChooser chooser = new SafeFileChooser(lastDirectoryPath);
      if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
        dicomFileName = chooser.getSelectedFile().getAbsolutePath();
        lastDirectoryPath = chooser.getCurrentDirectory().getAbsolutePath();
      }
    }
    loadDicomFileOrDirectory(dicomFileName, imagePanel,
        referenceImagePanelForImages,
        referenceImagePanelForSpectra);
  }

  /**
   * @param dicomFileName
   * @param ae
   * @param hostname
   * @param port
   * @param calledAETitle
   * @param callingAETitle
   * @param affectedSOPClass
   * @param affectedSOPInstance
   */
  private void sendDicomFileOverDicomNetwork(String dicomFileName, String ae, String hostname, int port,
      String calledAETitle, String callingAETitle,
      int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
      String affectedSOPClass, String affectedSOPInstance) {
    if (dicomFileName != null) {
      cursorChanger.setWaitCursor();
      try {
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Sending image " + dicomFileName + " to " + ae + " ..."));
        int storageSCUCompressionLevel = networkApplicationProperties.getStorageSCUCompressionLevel();
        new StorageSOPClassSCU(hostname, port, calledAETitle, callingAETitle,
            ourMaximumLengthReceived, socketReceiveBufferSize, socketSendBufferSize,
            dicomFileName, affectedSOPClass, affectedSOPInstance,
            storageSCUCompressionLevel);
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class DicomFileOrDirectoryLoadActionListener implements ActionListener {
    /***/
    JPanel imagePanel;
    /***/
    JPanel referenceImagePanelForImages;
    /***/
    JPanel referenceImagePanelForSpectra;

    /**
     * @param imagePanel
     * @param referenceImagePanelForImages
     * @param referenceImagePanelForSpectra
     */
    public DicomFileOrDirectoryLoadActionListener(JPanel imagePanel,
        JPanel referenceImagePanelForImages,
        JPanel referenceImagePanelForSpectra) {
      this.imagePanel = imagePanel;
      this.referenceImagePanelForImages = referenceImagePanelForImages;
      this.referenceImagePanelForSpectra = referenceImagePanelForSpectra;
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      // new Thread() { public void run() {
      callFileChooserThenLoadDicomFileOrDirectory(imagePanel,
          referenceImagePanelForImages,
          referenceImagePanelForSpectra);
      // } }.start();
    }
  }

  /***/
  private class DicomFileLoadFromSelectionActionListener implements ActionListener {
    /**
     */
    public DicomFileLoadFromSelectionActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      // new Thread() { public void run() {
      String dicomFileName = getCurrentFilePathSelection();
      if (dicomFileName != null) {
        loadDicomFileOrDirectory(dicomFileName, multiPanel,
            referenceImagePanelForImages,
            referenceImagePanelForSpectra);
      }
      // } }.start();
    }
  }

  /***/
  private TreeMap backgroundImageListMappedToFilenames = null;

  /***/
  private TreeMap referenceImageListMappedToFilenames = null;

  /***/
  private final TreeMap getImageListMappedToFilenamesForReferenceOrBackground(AttributeList referencedFromList, boolean requireSameImageOrientationPatient) {
    TreeMap imageListMappedToFilenames = new TreeMap(); // of String
                                                        // descriptions; each
                                                        // possible only once,
                                                        // sorted
                                                        // lexicographically;
                                                        // mapped to String file
                                                        // name
    String frameOfReferenceUID = Attribute.getSingleStringValueOrNull(referencedFromList, TagFromName.FrameOfReferenceUID);
    double[] wantedImageOrientationPatient = GeometryOfVolumeFromAttributeList.getImageOrientationPatientFromAttributeList(referencedFromList);
    if (frameOfReferenceUID != null) {
      if (databaseInformationModel != null) {
        try {
          // ArrayList values =
          // databaseInformationModel.findSelectedAttributeValueForAllRecordsForThisInformationEntityWithMatchingParent(
          ArrayList returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithMatchingParent(
              InformationEntity.INSTANCE,
              InformationEntity.SERIES,
              "FrameOfReferenceUID",
              frameOfReferenceUID);
          if (returnedRecords != null && returnedRecords.size() > 0) {
            for (int i = 0; i < returnedRecords.size(); ++i) {
              String value = null;
              Map returnedAttributes = (Map) (returnedRecords.get(i));
              if (returnedAttributes != null) {
                String description = DescriptionFactory.makeImageDescription(returnedAttributes);
                String sopInstanceUID = (String) (returnedAttributes.get("SOPINSTANCEUID"));
                String sopClassUID = (String) (returnedAttributes.get("SOPCLASSUID"));
                double[] imageOrientationPatient = null;
                {
                  String s = (String) (returnedAttributes.get("IMAGEORIENTATIONPATIENT"));
                  if (s != null) {
                    imageOrientationPatient = FloatFormatter.fromString(s, 6, '\\');
                  }
                }
                if (!requireSameImageOrientationPatient
                    || (wantedImageOrientationPatient != null && wantedImageOrientationPatient.length == 6
                        && imageOrientationPatient != null && imageOrientationPatient.length == 6
                        && ArrayCopyUtilities.arraysAreEqual(wantedImageOrientationPatient, imageOrientationPatient))) {
                  String filename = (String) (returnedAttributes.get(
                      databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE)));
                  // only images and no duplicates ...
                  if (filename != null && sopClassUID != null && SOPClass.isImageStorage(sopClassUID)
                      && !imageListMappedToFilenames.containsKey(description)) {
                    imageListMappedToFilenames.put(description, filename);
                    // System.err.println("Potential reference in same Frame of
                    // Reference: "+description+" "+filename);
                  }
                }
              }
            }
          }
        } catch (DicomException e) {
          slf4jlogger.error("", e);
        }
      }
      // NB. since always checks for description key, will not use a reference
      // from the DICOMDIR
      // if there is already one from the database ...
      if (currentDicomDirectory != null) {
        Vector attributeLists = currentDicomDirectory.findAllImagesForFrameOfReference(frameOfReferenceUID); // only
                                                                                                             // images
        if (attributeLists != null) {
          for (int j = 0; j < attributeLists.size(); ++j) {
            AttributeList referencedList = (AttributeList) (attributeLists.get(j));
            // System.err.println("Same Frame Of Reference:");
            // System.err.println(referencedList);
            if (referencedList != null) {
              String description = DescriptionFactory.makeImageDescription(referencedList);
              String sopInstanceUID = Attribute.getSingleStringValueOrNull(referencedList, TagFromName.ReferencedSOPInstanceUIDInFile);
              double[] imageOrientationPatient = Attribute.getDoubleValues(referencedList, TagFromName.ImageOrientationPatient);
              if (sopInstanceUID != null) {
                String filename = null;
                try {
                  // get name which has parent path all fixed up already ....
                  filename = currentDicomDirectory.getReferencedFileNameForSOPInstanceUID(sopInstanceUID);
                } catch (DicomException e) {
                }
                if (!requireSameImageOrientationPatient
                    || (wantedImageOrientationPatient != null && wantedImageOrientationPatient.length == 6
                        && imageOrientationPatient != null && imageOrientationPatient.length == 6
                        && ArrayCopyUtilities.arraysAreEqual(wantedImageOrientationPatient, imageOrientationPatient))) {
                  // no duplicates ...
                  if (filename != null && !imageListMappedToFilenames.containsKey(description)) {
                    imageListMappedToFilenames.put(description, filename);
                    // System.err.println("Potential reference in same Frame of
                    // Reference: "+description+" "+filename);
                  }
                }
              }
            }
          }
        }
      }
    }
    return imageListMappedToFilenames;
  }

  /***/
  private JPanel referenceImagePanelForImages = null;

  /***/
  private JPanel referenceImagePanelForSpectra = null;

  /***/
  private final class OurReferenceListSelectionListener implements ListSelectionListener {
    /***/
    private String lastSelectedDicomFileName = null;
    /***/
    private final JPanel referenceImagePanel;
    /***/
    private final boolean spectroscopy;

    OurReferenceListSelectionListener(JPanel referenceImagePanel, boolean spectroscopy) {
      super();
      this.referenceImagePanel = referenceImagePanel;
      this.spectroscopy = spectroscopy;
    }

    /***/
    @Override
    public void valueChanged(ListSelectionEvent e) {
      // System.err.println("The class of the ListSelectionEvent source is " +
      // e.getSource().getClass().getName());
      JList list = (JList) (e.getSource());
      // System.err.println("Selection event is "+e);
      if (list.isSelectionEmpty()) {
        // such as when list has been reloaded ...
        lastSelectedDicomFileName = null; // Fixes [bugs.mrmf] (000070)
                                          // Localizer/spectra background
                                          // sometimes doesn't load on
                                          // selection, or reselection
      } else {
        // System.err.println("List selection is not empty");
        String key = (String) list.getSelectedValue();
        if (key != null) {
          // System.err.println("List selection key is not null = "+key);
          String dicomFileName = (String) referenceImageListMappedToFilenames.get(key);
          // System.err.println("List selection dicomFileName =
          // "+dicomFileName);
          // collapse redundant duplicate events
          if (dicomFileName != null && (lastSelectedDicomFileName == null || !dicomFileName.equals(lastSelectedDicomFileName))) {
            // System.err.println("New selection "+key+" "+dicomFileName);
            lastSelectedDicomFileName = dicomFileName;
            loadReferenceImagePanel(dicomFileName, referenceImagePanel, spectroscopy);
          }
        }
      }
    }
  }

  /***/
  private final class OurBackgroundListSelectionListener implements ListSelectionListener {
    /***/
    private String lastSelectedDicomFileName = null;

    OurBackgroundListSelectionListener() {
      super();
    }

    /***/
    @Override
    public void valueChanged(ListSelectionEvent e) {
      // System.err.println("The class of the ListSelectionEvent source is " +
      // e.getSource().getClass().getName());
      JList list = (JList) (e.getSource());

      if (list.isSelectionEmpty()) {
        // such as when list has been reloaded ...
        lastSelectedDicomFileName = null; // Fixes [bugs.mrmf] (000070)
                                          // Localizer/spectra background
                                          // sometimes doesn't load on
                                          // selection, or reselection
      } else {
        String key = (String) list.getSelectedValue();
        if (key != null) {
          String dicomFileName = (String) backgroundImageListMappedToFilenames.get(key);
          // collapse redundant duplicate events
          if (dicomFileName != null && (lastSelectedDicomFileName == null || !dicomFileName.equals(lastSelectedDicomFileName))) {
            // System.err.println("New selection "+key+" "+dicomFileName);
            lastSelectedDicomFileName = dicomFileName;
            loadBackgroundImageForSpectra(dicomFileName);
          }
        }
      }
    }
  }

  /***/
  private String showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(String question, String buttonText, String defaultSelection) {
    // System.err.println("DicomImageViewer.showInputDialogToSelectNetworkTargetByLocalApplicationEntityName()");
    String ae = defaultSelection;
    if (networkApplicationProperties != null) {
      // System.err.println("DicomImageViewer.showInputDialogToSelectNetworkTargetByLocalApplicationEntityName():
      // have networkApplicationProperties");
      Set localNamesOfRemoteAEs = networkApplicationInformation.getListOfLocalNamesOfApplicationEntities();
      if (localNamesOfRemoteAEs != null) {
        // System.err.println("DicomImageViewer.showInputDialogToSelectNetworkTargetByLocalApplicationEntityName():
        // got localNamesOfRemoteAEs");
        String sta[] = new String[localNamesOfRemoteAEs.size()];
        int i = 0;
        Iterator it = localNamesOfRemoteAEs.iterator();
        while (it.hasNext()) {
          sta[i++] = (String) (it.next());
        }
        ae = (String) JOptionPane.showInputDialog(getContentPane(), question, buttonText, JOptionPane.QUESTION_MESSAGE, null, sta, ae);
      }
    }
    return ae;
  }

  /***/
  private class QuerySelectActionListener implements ActionListener {
    /***/
    JTabbedPane browserPane;
    /***/
    int tabNumberOfRemoteInBrowserPane;

    /**
     * @param treeScrollPane
     * @param scrollPaneOfCurrentAttributes
     * @param browserPane
     * @param tabNumberOfRemoteInBrowserPane
     */
    public QuerySelectActionListener(JTabbedPane browserPane, int tabNumberOfRemoteInBrowserPane) {
      this.browserPane = browserPane;
      this.tabNumberOfRemoteInBrowserPane = tabNumberOfRemoteInBrowserPane;
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      Properties properties = getProperties();
      String ae = properties.getProperty(propertyName_DicomCurrentlySelectedQueryTargetAE);
      ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select remote system", "Query ...", ae);
      queryTreeScrollPane.setViewportView(null);
      scrollPaneOfCurrentAttributes.setViewportView(null);
      if (ae != null)
        setCurrentRemoteQueryInformationModel(ae, browserPane, tabNumberOfRemoteInBrowserPane);
    }
  }

  /***/
  private class QueryFilterActionListener implements ActionListener {
    /***/
    JTabbedPane browserPane;
    /***/
    int tabNumberOfRemoteInBrowserPane;

    /**
     * @param browserPane
     * @param tabNumberOfRemoteInBrowserPane
     */
    public QueryFilterActionListener(JTabbedPane browserPane, int tabNumberOfRemoteInBrowserPane) {
      this.browserPane = browserPane;
      this.tabNumberOfRemoteInBrowserPane = tabNumberOfRemoteInBrowserPane;
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      // System.err.println("QueryFilterActionListener.actionPerformed()");
      queryTreeScrollPane.setViewportView(new FilterPanel(getCurrentRemoteQueryFilter()));
      // scrollPaneOfCurrentAttributes.setViewportView(null);
    }
  }

  private void performRetrieve(AttributeList uniqueKeys, String selectionLevel, String retrieveAE) {
    try {
      AttributeList identifier = new AttributeList();
      if (uniqueKeys != null) {
        identifier.putAll(uniqueKeys);
        {
          AttributeTag t = TagFromName.QueryRetrieveLevel;
          Attribute a = new CodeStringAttribute(t);
          a.addValue(selectionLevel);
          identifier.put(t, a);
        }
        QueryInformationModel queryInformationModel = getCurrentRemoteQueryInformationModel();
        queryInformationModel.performHierarchicalMoveFrom(identifier, retrieveAE);
      }
      // else do nothing, since no unique key to specify what to retrieve
    } catch (Exception e) {
      slf4jlogger.error("", e);
    }
  }

  /***/
  private class QueryRetrieveActionListener implements ActionListener {
    /**
     */
    public QueryRetrieveActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      if (getCurrentRemoteQuerySelectionLevel() == null) { // they have selected
                                                           // the root of the
                                                           // tree
        QueryTreeRecord parent = getCurrentRemoteQuerySelectionQueryTreeRecord();
        if (parent != null) {
          slf4jlogger.info("Retrieve: everything from {}", getCurrentRemoteQuerySelectionRetrieveAE());
          Enumeration children = parent.children();
          if (children != null) {
            while (children.hasMoreElements()) {
              QueryTreeRecord r = (QueryTreeRecord) (children.nextElement());
              if (r != null) {
                setCurrentRemoteQuerySelection(r.getUniqueKeys(), r.getUniqueKey(), r.getAllAttributesReturnedInIdentifier());
                slf4jlogger.info("Retrieve: {} {} from {}", getCurrentRemoteQuerySelectionLevel(), getCurrentRemoteQuerySelectionUniqueKey().getSingleStringValueOrEmptyString(), getCurrentRemoteQuerySelectionRetrieveAE());
                performRetrieve(getCurrentRemoteQuerySelectionUniqueKeys(), getCurrentRemoteQuerySelectionLevel(), getCurrentRemoteQuerySelectionRetrieveAE());
              }
            }
          }
          slf4jlogger.info("Retrieve done");
          setCurrentRemoteQuerySelection(null, null, null);
        }
      } else {
        // System.err.println("DicomImageViewer.QueryRetrieveActionListener.actionPerformed():
        // "+getCurrentRemoteQuerySelectionUniqueKeys()+"
        // from="+getCurrentRemoteQuerySelectionRetrieveAE()+"
        // level="+getCurrentRemoteQuerySelectionLevel());
        slf4jlogger.info("Retrieve: {} {} from {}", getCurrentRemoteQuerySelectionLevel(), getCurrentRemoteQuerySelectionUniqueKey().getSingleStringValueOrEmptyString(), getCurrentRemoteQuerySelectionRetrieveAE());
        performRetrieve(getCurrentRemoteQuerySelectionUniqueKeys(), getCurrentRemoteQuerySelectionLevel(), getCurrentRemoteQuerySelectionRetrieveAE());
        slf4jlogger.info("Retrieve done");
      }
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class QueryRefreshActionListener implements ActionListener {
    /**
     */
    public QueryRefreshActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        queryTreeScrollPane.setViewportView(null);
        scrollPaneOfCurrentAttributes.setViewportView(null);
        QueryInformationModel queryInformationModel = getCurrentRemoteQueryInformationModel();
        if (queryInformationModel != null) {
          // make sure that Specific Character Set is updated to reflect any
          // text values with funky characters that may have been entered in the
          // filter ...
          AttributeList filter = getCurrentRemoteQueryFilter();
          filter.insertSuitableSpecificCharacterSetForAllStringValues();
          QueryTreeModel treeModel = queryInformationModel.performHierarchicalQuery(filter);
          new OurQueryTreeBrowser(queryInformationModel, treeModel, queryTreeScrollPane, scrollPaneOfCurrentAttributes);
        }
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      cursorChanger.restoreCursor();
    }
  }

  // very similar to code in DicomCleaner and DoseUtility apart from logging and
  // progress bar ... should refactor :(
  protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord[] databaseSelections) throws DicomException, IOException {
    if (databaseSelections != null) {
      for (DatabaseTreeRecord databaseSelection : databaseSelections) {
        purgeFilesAndDatabaseInformation(databaseSelection);
      }
    }
  }

  protected void purgeFilesAndDatabaseInformation(DatabaseTreeRecord databaseSelection) throws DicomException, IOException {
    // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
    // "+databaseSelection);
    if (databaseSelection != null) {
      InformationEntity ie = databaseSelection.getInformationEntity();
      // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
      // ie = "+ie);
      if (ie == null /* the root of the tree, i.e., everything */ || !ie.equals(InformationEntity.INSTANCE)) {
        // Do it one study at a time, in the order in which the patients and
        // studies are sorted in the tree
        Enumeration children = databaseSelection.children();
        if (children != null) {
          while (children.hasMoreElements()) {
            DatabaseTreeRecord child = (DatabaseTreeRecord) (children.nextElement());
            if (child != null) {
              purgeFilesAndDatabaseInformation(child);
            }
          }
        }
        // AFTER we have processed all the children, if any, we can delete
        // ourselves, unless we are the root
        if (ie != null) {
          // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
          // removeFromParent having recursed over children
          // "+databaseSelection);
          databaseSelection.removeFromParent();
        }
      } else {
        // Instance level ... may need to delete files
        String fileName = databaseSelection.getLocalFileNameValue();
        String fileReferenceType = databaseSelection.getLocalFileReferenceTypeValue();
        // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
        // fileReferenceType = "+fileReferenceType+" for file "+fileName);
        if (fileReferenceType != null && fileReferenceType.equals(DatabaseInformationModel.FILE_COPIED)) {
          // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
          // deleting fileName "+fileName);
          try {
            if (!new File(fileName).delete()) {
              System.err.println("Failed to delete local copy of file " + fileName);
            }
          } catch (Exception e) {
            slf4jlogger.error("", e);
          }
        }
        // System.err.println("DicomImageViewer.purgeFilesAndDatabaseInformation():
        // removeFromParent instance level "+databaseSelection);
        databaseSelection.removeFromParent();
      }
    }
  }

  protected class DatabasePurgeWorker implements Runnable {
    DatabaseTreeRecord[] databaseSelections;

    DatabasePurgeWorker(DatabaseTreeRecord[] databaseSelections) {
      this.databaseSelections = databaseSelections;
    }

    @Override
    public void run() {
      cursorChanger.setWaitCursor();
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging started"));
      try {
        purgeFilesAndDatabaseInformation(databaseSelections);
      } catch (Exception e) {
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: " + e));
        slf4jlogger.error("", e);
      }
      try {
        new OurDatabaseTreeBrowser();
      } catch (Exception e) {
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Refresh source database browser failed: " + e));
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done purging"));
      cursorChanger.restoreCursor();
    }
  }

  private class DatabasePurgeActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        new Thread(new DatabasePurgeWorker(getCurrentDatabaseTreeRecordSelections())).start();

      } catch (Exception e) {
        ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Purging failed: " + e));
        slf4jlogger.error("", e);
      }
    }
  }

  /***/
  private class DatabaseRefreshActionListener implements ActionListener {
    /**
     */
    public DatabaseRefreshActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        new OurDatabaseTreeBrowser();
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
    }
  }

  /***/
  private class DatabaseImportFromFilesActionListener implements ActionListener {
    /***/
    private final DatabaseMediaImporter importer;

    /**
     */
    public DatabaseImportFromFilesActionListener() {
      this.importer = new DatabaseMediaImporter(null/* initial path */, savedImagesFolder, storedFilePathStrategy, databaseInformationModel, /*
                                                                                                                                              * null
                                                                                                                                              */new OurMessageLogger());
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        // System.err.println("DicomImageViewer.DatabaseImportFromFilesActionListener.actionPerformed():
        // caling importer.choosePathAndImportDicomFiles()");
        importer.choosePathAndImportDicomFiles(DicomImageViewer.this.getContentPane());
        new OurDatabaseTreeBrowser();
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /**
   * @param list
   * @param fileName
   * @throws IOException
   * @throws DicomException
   */
  private void copyFileAndImportToDatabase(AttributeList list, String fileName) throws DicomException, IOException {
    String sopInstanceUID = Attribute.getSingleStringValueOrNull(list, TagFromName.SOPInstanceUID);
    if (sopInstanceUID == null) {
      throw new DicomException("Cannot get SOP Instance UID to make file name for local copy when inserting into database");
    }
    String localCopyFileName = storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(savedImagesFolder, sopInstanceUID).getPath();
    // System.err.println("DicomImageViewer.copyFileAndImportToDatabase(): uid =
    // "+sopInstanceUID+" path ="+localCopyFileName);
    if (fileName.equals(localCopyFileName)) {
      slf4jlogger.info("copyFileAndImportToDatabase(): input and output filenames identical - presumably copying from our own database back into our own database, so doing nothing");
    } else {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Copying object ..."));
      CopyStream.copy(new BufferedInputStream(new FileInputStream(fileName)), new BufferedOutputStream(new FileOutputStream(localCopyFileName)));
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Inserting into database ..."));
      databaseInformationModel.insertObject(list, localCopyFileName, DatabaseInformationModel.FILE_COPIED);
    }
  }

  /***/
  private class ImportCurrentlyDisplayedInstanceToDatabaseActionListener implements ActionListener {
    /**
     */
    public ImportCurrentlyDisplayedInstanceToDatabaseActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        copyFileAndImportToDatabase(getAttributeListForDatabaseImport(), getCurrentlyDisplayedInstanceFilePath());
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  public class ImportFromSelectionToDatabaseActionListener implements ActionListener {
    /**
     */
    public ImportFromSelectionToDatabaseActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      Vector paths = getCurrentFilePathSelections();
      if (paths != null) {
        for (int j = 0; j < paths.size(); ++j) {
          String dicomFileName = (String) (paths.get(j));
          if (dicomFileName != null) {
            try {
              File file = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
              DicomInputStream i = new DicomInputStream(file);
              AttributeList list = new AttributeList();
              list.read(i);
              i.close();
              // databaseInformationModel.insertObject(list,dicomFileName);
              copyFileAndImportToDatabase(list, file.getAbsolutePath());
            } catch (Exception e) {
              slf4jlogger.error("", e);
            }
          }
        }
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class NetworkSendCurrentSelectionActionListener implements ActionListener {
    /**
     */
    public NetworkSendCurrentSelectionActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      Vector paths = getCurrentFilePathSelections();
      // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
      // paths="+paths);
      if (paths != null && paths.size() > 0) {
        // boolean coerce = JOptionPane.showConfirmDialog(null,"Change
        // identifiers during send ? ","Send ...",
        // JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE) ==
        // JOptionPane.YES_OPTION;
        // if (coerce) {
        // CoercionModel coercionModel = new CoercionModel(paths);
        // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
        // CoercionModel="+coercionModel);
        // }

        Properties properties = getProperties();
        String ae = properties.getProperty(propertyName_DicomCurrentlySelectedStorageTargetAE);
        ae = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName("Select destination", "Send ...", ae);
        if (ae != null && networkApplicationProperties != null) {
          try {
            String callingAETitle = networkApplicationProperties.getCallingAETitle();
            String calledAETitle = networkApplicationInformation.getApplicationEntityTitleFromLocalName(ae);
            PresentationAddress presentationAddress = networkApplicationInformation.getApplicationEntityMap().getPresentationAddress(calledAETitle);
            String hostname = presentationAddress.getHostname();
            int port = presentationAddress.getPort();
            int ourMaximumLengthReceived = networkApplicationProperties.getInitiatorMaximumLengthReceived();
            int socketReceiveBufferSize = networkApplicationProperties.getInitiatorSocketReceiveBufferSize();
            int socketSendBufferSize = networkApplicationProperties.getInitiatorSocketSendBufferSize();

            String affectedSOPClass = null;
            String affectedSOPInstance = null;

            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // ae="+ae);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // hostname="+hostname);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // port="+port);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // calledAETitle="+calledAETitle);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // callingAETitle="+callingAETitle);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // affectedSOPClass="+affectedSOPClass);
            // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
            // affectedSOPInstance="+affectedSOPInstance);

            for (int j = 0; j < paths.size(); ++j) {
              String dicomFileName = (String) (paths.get(j));
              if (dicomFileName != null) {
                try {
                  // System.err.println("NetworkSendCurrentSelectionActionListener.actionPerformed():
                  // dicomFileName="+dicomFileName);
                  slf4jlogger.info("Send: {}", dicomFileName);
                  File file = FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(dicomFileName);
                  sendDicomFileOverDicomNetwork(file.getAbsolutePath(), ae, hostname, port, calledAETitle, callingAETitle,
                      ourMaximumLengthReceived, socketReceiveBufferSize, socketSendBufferSize,
                      affectedSOPClass, affectedSOPInstance);
                } catch (Exception e) {
                  slf4jlogger.error("", e);
                }
              }
            }
          } catch (Exception e) { // if an AE's property has no value
            slf4jlogger.error("", e);
          }
        }
        // else user cancelled operation in JOptionPane.showInputDialog() so
        // gracefully do nothing
      }
    }
  }

  /***/
  private class SaveCurrentlyDisplayedImageToXMLActionListener implements ActionListener {
    /**
     */
    public SaveCurrentlyDisplayedImageToXMLActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        SafeFileChooser chooser = new SafeFileChooser(lastDirectoryPath);
        if (chooser.showSaveDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
          String xmlFileName = chooser.getSelectedFile().getAbsolutePath();
          lastDirectoryPath = chooser.getCurrentDirectory().getAbsolutePath();
          AttributeList list = getAttributeListForDatabaseImport();
          new XMLRepresentationOfDicomObjectFactory().createDocumentAndWriteIt(list, new BufferedOutputStream(new FileOutputStream(xmlFileName)));
        }
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class SaveCurrentlyDisplayedStructuredReportToXMLActionListener implements ActionListener {
    /**
    	*/
    public SaveCurrentlyDisplayedStructuredReportToXMLActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        SafeFileChooser chooser = new SafeFileChooser(lastDirectoryPath);
        if (chooser.showSaveDialog(DicomImageViewer.this.getContentPane()) == JFileChooser.APPROVE_OPTION) {
          String xmlFileName = chooser.getSelectedFile().getAbsolutePath();
          lastDirectoryPath = chooser.getCurrentDirectory().getAbsolutePath();
          AttributeList list = getAttributeListForDatabaseImport();
          new XMLRepresentationOfStructuredReportObjectFactory().createDocumentAndWriteIt(list, new BufferedOutputStream(new FileOutputStream(xmlFileName)));
        }
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class ValidateCurrentlyDisplayedImageActionListener implements ActionListener {
    /***/
    DicomInstanceValidator validator;

    /**
     */
    public ValidateCurrentlyDisplayedImageActionListener() {
      validator = null;
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        AttributeList list = getAttributeListForDatabaseImport();
        if (validator == null) {
          // lazy instantiation to speed up start up
          validator = new DicomInstanceValidator();
        }
        String outputString = validator == null ? "Could not instantiate a validator\n" : validator.validate(list);
        JTextArea outputTextArea = new JTextArea(outputString);
        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        JDialog outputDialog = new JDialog();
        outputDialog.setSize(512, 384);
        outputDialog.setTitle("Validation of " + getCurrentFilePathSelection());
        outputDialog.getContentPane().add(outputScrollPane);
        outputDialog.setVisible(true);

      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class ValidateCurrentlyDisplayedStructuredReportActionListener implements ActionListener {
    /***/
    DicomSRValidator validator;

    /**
     */
    public ValidateCurrentlyDisplayedStructuredReportActionListener() {
      validator = null;
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      cursorChanger.setWaitCursor();
      try {
        AttributeList list = getAttributeListForDatabaseImport();
        if (validator == null) {
          // lazy instantiation to speed up start up
          validator = new DicomSRValidator();
        }
        String outputString = validator == null ? "Could not instantiate a validator\n" : validator.validate(list);
        JTextArea outputTextArea = new JTextArea(outputString);
        JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
        JDialog outputDialog = new JDialog();
        outputDialog.setSize(512, 384);
        outputDialog.setTitle("Validation of " + getCurrentFilePathSelection());
        outputDialog.getContentPane().add(outputScrollPane);
        outputDialog.setVisible(true);

      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Done.                                                   "));
      cursorChanger.restoreCursor();
    }
  }

  /***/
  private class OurReceivedObjectHandler extends ReceivedObjectHandler {
    // private DatabaseInformationModel databaseInformationModel;

    // OurReceivedObjectHandler(DatabaseInformationModel
    // databaseInformationModel) {
    // this.databaseInformationModel=databaseInformationModel;
    // }

    /**
     * @param dicomFileName
     * @param transferSyntax
     * @param callingAETitle
     * @throws IOException
     * @throws DicomException
     * @throws DicomNetworkException
     */
    @Override
    public void sendReceivedObjectIndication(String dicomFileName, String transferSyntax, String callingAETitle)
        throws DicomNetworkException, DicomException, IOException {
      // System.err.println("DicomImageViewer.OurReceivedObjectHandler.sendReceivedObjectIndication()
      // dicomFileName: "+dicomFileName);
      if (dicomFileName != null) {
        slf4jlogger.info("Received: {} from {} in {}", dicomFileName, callingAETitle, transferSyntax);
        try {
          // long startTime = System.currentTimeMillis();
          // no need for case insensitive check here ... was locally created
          FileInputStream fis = new FileInputStream(dicomFileName);
          DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
          AttributeList list = new AttributeList();
          list.read(i, TagFromName.PixelData);
          i.close();
          fis.close();
          // long afterReadTime = System.currentTimeMillis();
          // System.err.println("Received: time to read list
          // "+(afterReadTime-startTime)+" ms");
          databaseInformationModel.insertObject(list, dicomFileName, DatabaseInformationModel.FILE_COPIED);
          // long afterInsertTime = System.currentTimeMillis();
          // System.err.println("Received: time to insert in database
          // "+(afterInsertTime-afterReadTime)+" ms");
        } catch (Exception e) {
          slf4jlogger.error("Unable to insert {} received from {} in {} into database", dicomFileName, callingAETitle, transferSyntax, e);
        }
      }

    }
  }

  private ButtonGroup attributeTreeSortOrderButtons = new ButtonGroup();

  /***/
  private class SortAttributesActionListener implements ActionListener {

    static final String ByName = "NAME";
    static final String ByNumber = "NUMBER";

    /**
     */
    public SortAttributesActionListener() {
    }

    /**
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
      // System.err.println("SortAttributesActionListener.SortAttributesActionListener.actionPerformed()");
      String choice = attributeTreeSortOrderButtons.getSelection().getActionCommand();
      // System.err.println("SortAttributesActionListener.SortAttributesActionListener.actionPerformed():
      // choice="+choice);
      AttributeTreeBrowser.setSortByName(attributeTreeScrollPane, choice != null && choice.equals(ByName));

    }
  }

  public void osxFileHandler(String fileName) {
    // System.err.println("DicomImageViewer.osxFileHandler(): fileName =
    // "+fileName);
    lastDirectoryPath = new File(fileName).getParent(); // needed, since
                                                        // otherwise can't load
                                                        // children inside
                                                        // DICOMDIR
    // System.err.println("DicomImageViewer.osxFileHandler(): setting
    // lastDirectoryPath = "+lastDirectoryPath);
    loadDicomFileOrDirectory(fileName);
  }

  // Based on Apple's MyApp.java example supplied with OSXAdapter ...
  // Generic registration with the Mac OS X application menu
  // Checks the platform, then attempts to register with the Apple EAWT
  // See OSXAdapter.java to see how this is done without directly referencing
  // any Apple APIs
  public void registerForMacOSXEvents() {
    if (System.getProperty("os.name").toLowerCase(java.util.Locale.US).startsWith("mac os x")) {
      // System.err.println("DicomImageViewer.registerForMacOSXEvents(): on
      // MacOSX");
      // try {
      // // Generate and register the OSXAdapter, passing it a hash of all the
      // // methods we wish to
      // // use as delegates for various com.apple.eawt.ApplicationListener
      // // methods
      // // OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit",
      // // (Class[]) null)); // need
      // // this,
      // // else
      // // won't
      // // quit
      // // from
      // // X
      // // or
      // // Cmd-Q
      // // any
      // // more,
      // // once
      // // any
      // // events
      // // registered
      // // OSXAdapter.setAboutHandler(this,
      // // getClass().getDeclaredMethod("about", (Class[])null));
      // // OSXAdapter.setPreferencesHandler(this,
      // // getClass().getDeclaredMethod("preferences", (Class[])null));
      // // OSXAdapter.setFileHandler(this,
      // // getClass().getDeclaredMethod("osxFileHandler", new Class[] {
      // // String.class }));
      // } catch (NoSuchMethodException e) {
      // // trap it, since we don't want to fail just because we cannot register
      // // events
      // slf4jlogger.error("", e);
      // }
    }
  }

  /**
   * @param title
   * @param dicomFileName
   */
  private void doCommonConstructorStuff(String title, String dicomFileName) {
    registerForMacOSXEvents();

    // Font defaultFont=new JLabel().getFont();
    // System.err.println("defaultFont="+defaultFont);
    // {
    // Font[] fonts =
    // GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    // for (int i=0; i<fonts.length; ++i) System.err.println("font "+fonts[i]);
    // }

    mainPanelFrameSortOrderChangeListener = new OurFrameSortOrderChangeListener(WellKnownContext.MAINPANEL);
    mainPanelFrameSelectionChangeListener = new OurFrameSelectionChangeListener(WellKnownContext.MAINPANEL);
    ourBrowserPaneChangeListener = new OurBrowserPaneChangeListener(WellKnownContext.MAINPANEL);

    // No frame selection or sort order listener required for reference panel

    addKeyListener(this); // for screen snapshot
    addMouseListener(this); // for screen snapshot (allows us to grab keyboard
                            // focus by moving mouse out and back into app)

    {
      spectroscopyLocalizerManager = new SpectroscopyLocalizerManager();
      spectroscopyLocalizerManager.setReferenceSourceImageSelectionContext(WellKnownContext.REFERENCEPANEL);
      spectroscopyLocalizerManager.setReferenceImageFrameSelectionContext(WellKnownContext.REFERENCEPANEL);
      spectroscopyLocalizerManager.setReferenceImageFrameSortOrderContext(WellKnownContext.REFERENCEPANEL);
      spectroscopyLocalizerManager.setSourceSpectrumSelectionContext(WellKnownContext.MAINPANEL);
      spectroscopyLocalizerManager.setSpectrumFrameSelectionContext(WellKnownContext.MAINPANEL);
      spectroscopyLocalizerManager.setSpectrumFrameSortOrderContext(WellKnownContext.MAINPANEL);

      imageLocalizerManager = new ImageLocalizerManager();
      imageLocalizerManager.setReferenceSourceImageSelectionContext(WellKnownContext.REFERENCEPANEL);
      imageLocalizerManager.setReferenceImageFrameSelectionContext(WellKnownContext.REFERENCEPANEL);
      imageLocalizerManager.setReferenceImageFrameSortOrderContext(WellKnownContext.REFERENCEPANEL);
      imageLocalizerManager.setMainSourceImageSelectionContext(WellKnownContext.MAINPANEL);
      imageLocalizerManager.setMainImageFrameSelectionContext(WellKnownContext.MAINPANEL);
      imageLocalizerManager.setMainImageFrameSortOrderContext(WellKnownContext.MAINPANEL);
    }

    Properties properties = getProperties();
    // System.err.println("properties="+properties);

    databaseApplicationProperties = new DatabaseApplicationProperties(properties);

    savedImagesFolder = null;

    if (databaseApplicationProperties != null) {

      // Make sure there is a folder to store received and imported images ...

      try {
        savedImagesFolder = databaseApplicationProperties.getSavedImagesFolderCreatingItIfNecessary();
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }

      // Start up database ...

      // System.err.println("Starting up database ...");
      databaseInformationModel = null;
      try {
        databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(makePathToFileInUsersHomeDirectory(databaseApplicationProperties.getDatabaseFileName()), databaseApplicationProperties.getDatabaseServerName());
        // databaseInformationModel = new
        // StudySeriesInstanceModel(makePathToFileInUsersHomeDirectory(dataBaseFileName));
      } catch (Exception e) {
        slf4jlogger.error("", e);
      }
    }

    // System.err.println("Starting up network configuration information sources
    // ...");
    try {
      networkApplicationProperties = new NetworkApplicationProperties(properties, true/*
                                                                                       * addPublicStorageSCPsIfNoRemoteAEsConfigured
                                                                                       */);
    } catch (Exception e) {
      networkApplicationProperties = null;
    }
    {
      NetworkApplicationInformationFederated federatedNetworkApplicationInformation = new NetworkApplicationInformationFederated();
      federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
      networkApplicationInformation = federatedNetworkApplicationInformation;
      // System.err.println("networkApplicationInformation
      // ...\n"+networkApplicationInformation);
    }

    // Start up DICOM association listener in background for receiving images
    // and responding to echoes ...
    // System.err.println("Starting up DICOM association listener ...");
    if (networkApplicationProperties != null) {
      try {
        int port = networkApplicationProperties.getListeningPort();
        String calledAETitle = networkApplicationProperties.getCalledAETitle();
        new Thread(new StorageSOPClassSCPDispatcher(port, calledAETitle,
            networkApplicationProperties.getAcceptorMaximumLengthReceived(), networkApplicationProperties.getAcceptorSocketReceiveBufferSize(), networkApplicationProperties.getAcceptorSocketSendBufferSize(),
            savedImagesFolder, storedFilePathStrategy, new OurReceivedObjectHandler(),
            databaseInformationModel == null ? null : databaseInformationModel.getQueryResponseGeneratorFactory(),
            databaseInformationModel == null ? null : databaseInformationModel.getRetrieveResponseGeneratorFactory(),
            networkApplicationInformation,
            // new
            // UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy(),
            // new
            // AnyExplicitStoreFindMoveGetPresentationContextSelectionPolicy(),
            false/* secureTransport */
        )).start();
      } catch (IOException e) {
        slf4jlogger.error("", e);
      }
    }

    setCurrentFilePathSelection(null);

    // ShutdownHook will run regardless of whether Command-Q (on Mac) or window
    // closed ...
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // System.err.println("DicomImageViewer.ShutdownHook.run()");
        if (databaseInformationModel != null) { // may have failed to be
                                                // initialized for some reason
          databaseInformationModel.close(); // we want to shut it down and
                                            // compact it before exiting
        }
        if (networkApplicationInformation != null && networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
          ((NetworkApplicationInformationFederated) networkApplicationInformation).removeAllSources();
        }
        // System.err.print(TransferMonitor.report());
      }
    });

    // System.err.println("Building GUI ...");

    cursorChanger = new SafeCursorChanger(this);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    applicationWidth = (int) (screenSize.getWidth()) - 20;
    applicationHeight = (int) (screenSize.getHeight()) - 70;
    imagesPerRow = 1;
    imagesPerCol = 1;

    Container content = getContentPane();
    EmptyBorder emptyBorder = (EmptyBorder) BorderFactory.createEmptyBorder();

    multiPanel = new JPanel();
    multiPanel.setLayout(new GridLayout(imagesPerCol, imagesPerRow));
    multiPanel.setBackground(Color.black);
    multiPanel.setOpaque(true); // normally the default, but not for Quaqua - if
                                // not set, grey rather than black background
                                // will show through
    // multiPanel.setBorder(emptyBorder);

    referenceImagePanelForImages = new JPanel();
    referenceImagePanelForImages.setLayout(new GridLayout(1, 1));
    referenceImagePanelForImages.setBackground(Color.black);
    // multiPanel.setBorder(emptyBorder);
    // referenceImagePanelForImages.setSize(new Dimension(128,128));
    referenceImagePanelForImages.setPreferredSize(new Dimension(128, 128));
    // referenceImagePanelForImages.setMinimumSize(new Dimension(128,128));
    // referenceImagePanelForImages.setMaximumSize(new Dimension(128,128));

    referenceImagePanelForSpectra = new JPanel();
    referenceImagePanelForSpectra.setLayout(new GridLayout(1, 1));
    referenceImagePanelForSpectra.setBackground(Color.black);
    // multiPanel.setBorder(emptyBorder);
    // referenceImagePanelForSpectra.setSize(new Dimension(128,128));
    referenceImagePanelForSpectra.setPreferredSize(new Dimension(128, 128));
    // referenceImagePanelForSpectra.setMinimumSize(new Dimension(128,128));
    // referenceImagePanelForSpectra.setMaximumSize(new Dimension(128,128));

    scrollPaneOfCurrentAttributes = new JScrollPane(); // declared final because
                                                       // accessed from inner
                                                       // class (tab change
                                                       // action database
                                                       // refresh)
    // scrollPaneOfCurrentAttributes.setBorder(emptyBorder);
    createTableOfCurrentAttributesForCurrentFrameBrowser();
    scrollPaneOfCurrentAttributes.setViewportView(getTableOfCurrentAttributesForCurrentFrameBrowser());

    attributeFrameTableScrollPane = new JScrollPane();
    // attributeTreeScrollPane.setBorder(emptyBorder);
    createTableOfCurrentAttributesForAllFramesBrowser();
    attributeFrameTableScrollPane.setViewportView(getTableOfCurrentAttributesForAllFramesBrowser());

    displayControlsPanel = new JPanel();
    // displayControlsPanel.setLayout(new GridLayout(3,1));
    displayControlsPanel.setLayout(new BorderLayout());
    JPanel displayButtonsPanel = new JPanel();
    // displayControlsPanel.add(displayButtonsPanel);
    displayControlsPanel.add(displayButtonsPanel, BorderLayout.NORTH);
    displayButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    // displayControlsPanel.setBorder(emptyBorder);
    JButton displayFileButton = new JButton("File...");
    displayFileButton.setToolTipText("Choose a DICOM image or spectroscopy file to display or DICOMDIR file to browse");
    displayButtonsPanel.add(displayFileButton);
    JButton displayImportButton = new JButton("Import");
    displayImportButton.setToolTipText("Import a copy of displayed image into the local database");
    displayButtonsPanel.add(displayImportButton);
    JButton displaySendButton = new JButton("Send...");
    displaySendButton.setToolTipText("Send displayed image via DICOM network");
    displayButtonsPanel.add(displaySendButton);
    JButton displayXMLButton = new JButton("XML...");
    displayXMLButton.setToolTipText("Save displayed image attributes to XML file");
    displayButtonsPanel.add(displayXMLButton);
    JButton displayValidateButton = new JButton("Validate...");
    displayValidateButton.setToolTipText("Validate displayed image against standard IOD");
    displayButtonsPanel.add(displayValidateButton);

    {
      JPanel displayControlsSubPanel = new JPanel();
      displayControlsPanel.add(displayControlsSubPanel, BorderLayout.CENTER);
      displayControlsSubPanel.setLayout(new BorderLayout());

      SourceImageSortOrderPanel displaySortPanel = new SourceImageSortOrderPanel(WellKnownContext.MAINPANEL);
      displayControlsSubPanel.add(displaySortPanel, BorderLayout.NORTH);

      {
        JPanel displayControlsSubSubPanel = new JPanel();
        displayControlsSubSubPanel.setLayout(new GridLayout(5, 1));
        displayControlsSubPanel.add(displayControlsSubSubPanel, BorderLayout.SOUTH);

        sourceImageVOILUTSelectorPanel = new SourceImageVOILUTSelectorPanel(null/*
                                                                                 * Apply
                                                                                 * to
                                                                                 * all
                                                                                 * contexts,
                                                                                 * not
                                                                                 * just
                                                                                 * WellKnownContext
                                                                                 * .
                                                                                 * MAINPANEL
                                                                                 */);
        displayControlsSubSubPanel.add(sourceImageVOILUTSelectorPanel);

        sourceImageWindowLinearCalculationSelectorPanel = new SourceImageWindowLinearCalculationSelectorPanel(null/*
                                                                                                                   * Apply
                                                                                                                   * to
                                                                                                                   * all
                                                                                                                   * contexts,
                                                                                                                   * not
                                                                                                                   * just
                                                                                                                   * WellKnownContext
                                                                                                                   * .
                                                                                                                   * MAINPANEL
                                                                                                                   */);
        displayControlsSubSubPanel.add(sourceImageWindowLinearCalculationSelectorPanel);

        sourceImageWindowingAccelerationSelectorPanel = new SourceImageWindowingAccelerationSelectorPanel(null/*
                                                                                                               * Apply
                                                                                                               * to
                                                                                                               * all
                                                                                                               * contexts,
                                                                                                               * not
                                                                                                               * just
                                                                                                               * WellKnownContext
                                                                                                               * .
                                                                                                               * MAINPANEL
                                                                                                               */);
        displayControlsSubSubPanel.add(sourceImageWindowingAccelerationSelectorPanel);

        sourceImageGraphicDisplaySelectorPanel = new SourceImageGraphicDisplaySelectorPanel(null/*
                                                                                                 * Apply
                                                                                                 * to
                                                                                                 * all
                                                                                                 * contexts,
                                                                                                 * not
                                                                                                 * just
                                                                                                 * WellKnownContext
                                                                                                 * .
                                                                                                 * MAINPANEL
                                                                                                 */);
        displayControlsSubSubPanel.add(sourceImageGraphicDisplaySelectorPanel);

        sourceImageShutterSelectorPanel = new SourceImageShutterSelectorPanel(null/*
                                                                                   * Apply
                                                                                   * to
                                                                                   * all
                                                                                   * contexts,
                                                                                   * not
                                                                                   * just
                                                                                   * WellKnownContext
                                                                                   * .
                                                                                   * MAINPANEL
                                                                                   */);
        displayControlsSubSubPanel.add(sourceImageShutterSelectorPanel);
      }
    }

    {
      JPanel referenceSubPanel = new JPanel(new BorderLayout());
      displayControlsPanel.add(referenceSubPanel, BorderLayout.SOUTH);

      JPanel referenceImageSubPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // nest
                                                                                     // these
                                                                                     // to
                                                                                     // make
                                                                                     // image
                                                                                     // centered
                                                                                     // and
                                                                                     // not
                                                                                     // fill
                                                                                     // width
                                                                                     // with
                                                                                     // black
      referenceSubPanel.add(referenceImageSubPanel, BorderLayout.CENTER);
      referenceImageSubPanel.add(referenceImagePanelForImages);

      displayListOfPossibleReferenceImagesForImages = new JList();
      displayListOfPossibleReferenceImagesForImages.setVisibleRowCount(4); // need
                                                                           // enough
                                                                           // height
                                                                           // for
                                                                           // vertical
                                                                           // scroll
                                                                           // bar
                                                                           // to
                                                                           // show,
                                                                           // including
                                                                           // if
                                                                           // horizontal
                                                                           // scroll
                                                                           // activates
      JScrollPane scrollingDisplayListOfPossibleReferenceImages = new JScrollPane(displayListOfPossibleReferenceImagesForImages);

      referenceSubPanel.add(scrollingDisplayListOfPossibleReferenceImages, BorderLayout.NORTH);

      displayListOfPossibleReferenceImagesForImages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      displayListOfPossibleReferenceImagesForImages.addListSelectionListener(new OurReferenceListSelectionListener(referenceImagePanelForImages, false));
    }

    spectroscopyControlsPanel = new JPanel();
    spectroscopyControlsPanel.setLayout(new BorderLayout());
    JPanel spectroscopyButtonsPanel = new JPanel();
    spectroscopyControlsPanel.add(spectroscopyButtonsPanel, BorderLayout.NORTH);
    spectroscopyButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    // spectroscopyControlsPanel.setBorder(emptyBorder);
    JButton spectroscopyFileButton = new JButton("File...");
    spectroscopyFileButton.setToolTipText("Choose a DICOM image or spectroscopy file to display or DICOMDIR file to browse");
    spectroscopyButtonsPanel.add(spectroscopyFileButton);
    JButton spectroscopyImportButton = new JButton("Import");
    spectroscopyImportButton.setToolTipText("Import a copy of displayed specra into the local database");
    spectroscopyButtonsPanel.add(spectroscopyImportButton);
    JButton spectroscopySendButton = new JButton("Send...");
    spectroscopySendButton.setToolTipText("Send display spectra via DICOM network");
    spectroscopyButtonsPanel.add(spectroscopySendButton);
    JButton spectroscopyXMLButton = new JButton("XML...");
    spectroscopyXMLButton.setToolTipText("Save displayed spectra attributes to XML file");
    spectroscopyButtonsPanel.add(spectroscopyXMLButton);
    JButton spectroscopyValidateButton = new JButton("Validate...");
    spectroscopyValidateButton.setToolTipText("Validate displayed spectra against standard IOD");
    spectroscopyButtonsPanel.add(spectroscopyValidateButton);

    SourceSpectrumSortOrderPanel spectroscopySortPanel = new SourceSpectrumSortOrderPanel(WellKnownContext.MAINPANEL);
    spectroscopyControlsPanel.add(spectroscopySortPanel, BorderLayout.CENTER);

    {
      JPanel spectroscopyBackgroundAndReferenceGroupPanel = new JPanel(new BorderLayout());
      spectroscopyControlsPanel.add(spectroscopyBackgroundAndReferenceGroupPanel, BorderLayout.SOUTH);
      {
        JPanel backgroundSubPanel = new JPanel(new BorderLayout());
        spectroscopyBackgroundAndReferenceGroupPanel.add(backgroundSubPanel, BorderLayout.NORTH);

        displayListOfPossibleBackgroundImagesForSpectra = new JList();
        displayListOfPossibleBackgroundImagesForSpectra.setVisibleRowCount(4); // need
                                                                               // enough
                                                                               // height
                                                                               // for
                                                                               // vertical
                                                                               // scroll
                                                                               // bar
                                                                               // to
                                                                               // show,
                                                                               // including
                                                                               // if
                                                                               // horizontal
                                                                               // scroll
                                                                               // activates
        JScrollPane scrollingDisplayListOfPossibleBackgroundImages = new JScrollPane(displayListOfPossibleBackgroundImagesForSpectra);

        backgroundSubPanel.add(scrollingDisplayListOfPossibleBackgroundImages, BorderLayout.NORTH);

        displayListOfPossibleBackgroundImagesForSpectra.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displayListOfPossibleBackgroundImagesForSpectra.addListSelectionListener(new OurBackgroundListSelectionListener());
      }
      {
        JPanel referenceSubPanel = new JPanel(new BorderLayout());
        spectroscopyBackgroundAndReferenceGroupPanel.add(referenceSubPanel, BorderLayout.SOUTH);

        JPanel referenceImageSubPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // nest
                                                                                       // these
                                                                                       // to
                                                                                       // make
                                                                                       // image
                                                                                       // centered
                                                                                       // and
                                                                                       // not
                                                                                       // fill
                                                                                       // width
                                                                                       // with
                                                                                       // black
        referenceSubPanel.add(referenceImageSubPanel, BorderLayout.CENTER);
        referenceImageSubPanel.add(referenceImagePanelForSpectra);

        displayListOfPossibleReferenceImagesForSpectra = new JList();
        displayListOfPossibleReferenceImagesForSpectra.setVisibleRowCount(4); // need
                                                                              // enough
                                                                              // height
                                                                              // for
                                                                              // vertical
                                                                              // scroll
                                                                              // bar
                                                                              // to
                                                                              // show,
                                                                              // including
                                                                              // if
                                                                              // horizontal
                                                                              // scroll
                                                                              // activates
        JScrollPane scrollingDisplayListOfPossibleReferenceImages = new JScrollPane(displayListOfPossibleReferenceImagesForSpectra);

        referenceSubPanel.add(scrollingDisplayListOfPossibleReferenceImages, BorderLayout.NORTH);

        displayListOfPossibleReferenceImagesForSpectra.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displayListOfPossibleReferenceImagesForSpectra.addListSelectionListener(new OurReferenceListSelectionListener(referenceImagePanelForSpectra, true));
      }
    }

    dicomdirControlsPanel = new JPanel();
    // dicomdirControlsPanel.setBorder(emptyBorder);
    dicomdirControlsPanel.setLayout(new BorderLayout());
    JPanel dicomdirButtonsPanel = new JPanel();
    dicomdirControlsPanel.add(dicomdirButtonsPanel, BorderLayout.NORTH);
    dicomdirButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JButton dicomdirFileButton = new JButton("File...");
    dicomdirFileButton.setToolTipText("Choose a DICOM image file to display or DICOMDIR file to browse");
    dicomdirButtonsPanel.add(dicomdirFileButton);
    JButton dicomdirImportButton = new JButton("Import");
    dicomdirImportButton.setToolTipText("Import all the images selected into the local database");
    dicomdirButtonsPanel.add(dicomdirImportButton);
    JButton dicomdirViewSelectionButton = new JButton("View");
    dicomdirViewSelectionButton.setToolTipText("Display the image selected (or first image of the selection)");
    dicomdirButtonsPanel.add(dicomdirViewSelectionButton);
    JButton dicomdirSendButton = new JButton("Send...");
    dicomdirSendButton.setToolTipText("Send all the images selected via DICOM network");
    dicomdirButtonsPanel.add(dicomdirSendButton);
    dicomdirTreeScrollPane = new JScrollPane();
    // dicomdirTreeScrollPane.setBorder(emptyBorder);
    dicomdirControlsPanel.add(dicomdirTreeScrollPane, BorderLayout.CENTER);

    databaseControlsPanel = new JPanel();
    // databaseControlsPanel.setBorder(emptyBorder);
    databaseControlsPanel.setLayout(new BorderLayout());
    JPanel databaseButtonsPanel = new JPanel();
    databaseButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // The
                                                                       // use of
                                                                       // FlowLayout
                                                                       // means
                                                                       // the
                                                                       // buttons
                                                                       // will
                                                                       // disappear
                                                                       // if
                                                                       // browserPane
                                                                       // gets
                                                                       // too
                                                                       // narrow
    databaseControlsPanel.add(databaseButtonsPanel, BorderLayout.NORTH);
    JButton databaseRefreshButton = new JButton("Refresh");
    databaseRefreshButton.setToolTipText("Query the database to update the browser");
    databaseButtonsPanel.add(databaseRefreshButton);
    JButton databaseFileButton = new JButton("File...");
    databaseFileButton.setToolTipText("Import DICOM files from a DICOMDIR or recursive directory search");
    databaseButtonsPanel.add(databaseFileButton);
    JButton databaseViewSelectionButton = new JButton("View");
    databaseViewSelectionButton.setToolTipText("Display the image selected");
    databaseButtonsPanel.add(databaseViewSelectionButton);
    JButton databaseSendButton = new JButton("Send...");
    databaseSendButton.setToolTipText("Send all the images selected via DICOM network");
    databaseButtonsPanel.add(databaseSendButton);
    JButton databasePurgeButton = new JButton("Purge");
    databasePurgeButton.setToolTipText("Remove selected entry from local database and delete local copies of files");
    databaseButtonsPanel.add(databasePurgeButton);
    databaseTreeScrollPane = new JScrollPane();
    // databaseTreeScrollPane.setBorder(emptyBorder);
    databaseControlsPanel.add(databaseTreeScrollPane, BorderLayout.CENTER);

    queryControlsPanel = new JPanel();
    // queryControlsPanel.setBorder(emptyBorder);
    queryControlsPanel.setLayout(new BorderLayout());
    JPanel queryButtonsPanel = new JPanel();
    queryButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // The use
                                                                    // of
                                                                    // FlowLayout
                                                                    // means the
                                                                    // buttons
                                                                    // will
                                                                    // disappear
                                                                    // if
                                                                    // browserPane
                                                                    // gets too
                                                                    // narrow
    queryControlsPanel.add(queryButtonsPanel, BorderLayout.NORTH);
    JButton querySelectButton = new JButton("Select");
    querySelectButton.setToolTipText("Select the remote system to use for subsequent queries");
    queryButtonsPanel.add(querySelectButton);
    JButton queryFilterButton = new JButton("Filter");
    queryFilterButton.setToolTipText("Configure the filter to use for subsequent queries");
    queryButtonsPanel.add(queryFilterButton);
    JButton queryRefreshButton = new JButton("Query");
    queryRefreshButton.setToolTipText("Query the currently selected remote system to update the browser");
    queryButtonsPanel.add(queryRefreshButton);
    JButton queryRetrieveButton = new JButton("Retrieve");
    queryRetrieveButton.setToolTipText("Retrieve the selection to the local database");
    queryButtonsPanel.add(queryRetrieveButton);
    queryTreeScrollPane = new JScrollPane();
    // queryTreeScrollPane.setBorder(emptyBorder);
    queryControlsPanel.add(queryTreeScrollPane, BorderLayout.CENTER);

    final JPanel attributeTreeControlsPanel = new JPanel();
    // attributeTreeControlsPanel.setBorder(emptyBorder);
    attributeTreeControlsPanel.setLayout(new BorderLayout());
    final JPanel attributeTreeButtonsPanel = new JPanel();
    attributeTreeControlsPanel.add(attributeTreeButtonsPanel, BorderLayout.NORTH);
    attributeTreeButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    attributeTreeScrollPane = new JScrollPane();

    attributeTreeButtonsPanel.add(new JLabel("Sort attributes:"));

    attributeTreeSortOrderButtons = new ButtonGroup();
    SortAttributesActionListener sortAttributesActionListener = new SortAttributesActionListener();

    JRadioButton sortAttributesByNameButton = new JRadioButton("by name", true);
    sortAttributesByNameButton.setActionCommand(SortAttributesActionListener.ByName);
    sortAttributesByNameButton.setToolTipText("Sort attributes in tree alphabetically by name");
    sortAttributesByNameButton.addActionListener(sortAttributesActionListener);
    attributeTreeSortOrderButtons.add(sortAttributesByNameButton);
    attributeTreeButtonsPanel.add(sortAttributesByNameButton);

    JRadioButton sortAttributesByTagNumberButton = new JRadioButton("by number", false);
    sortAttributesByTagNumberButton.setActionCommand(SortAttributesActionListener.ByNumber);
    sortAttributesByTagNumberButton.setToolTipText("Sort attributes in tree numerically by group and element number");
    sortAttributesByTagNumberButton.addActionListener(sortAttributesActionListener);
    attributeTreeSortOrderButtons.add(sortAttributesByTagNumberButton);
    attributeTreeButtonsPanel.add(sortAttributesByTagNumberButton);

    // attributeTreeScrollPane.setBorder(emptyBorder);
    attributeTreeControlsPanel.add(attributeTreeScrollPane, BorderLayout.CENTER);

    structuredReportTreeControlsPanel = new JPanel();
    structuredReportTreeControlsPanel.setLayout(new BorderLayout());
    JPanel structuredReportTreeButtonsPanel = new JPanel();
    structuredReportTreeControlsPanel.add(structuredReportTreeButtonsPanel, BorderLayout.NORTH);
    structuredReportTreeButtonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JButton structuredReportTreeFileButton = new JButton("File...");
    structuredReportTreeFileButton.setToolTipText("Choose a DICOM SR or image or spectroscopy file to display or DICOMDIR file to browse");
    structuredReportTreeButtonsPanel.add(structuredReportTreeFileButton);
    JButton structuredReportTreeImportButton = new JButton("Import");
    structuredReportTreeImportButton.setToolTipText("Import a copy of displayed SR into the local database");
    structuredReportTreeButtonsPanel.add(structuredReportTreeImportButton);
    JButton structuredReportTreeSendButton = new JButton("Send...");
    structuredReportTreeSendButton.setToolTipText("Send displayed SR via DICOM network");
    structuredReportTreeButtonsPanel.add(structuredReportTreeSendButton);
    JButton structuredReportTreeXMLButton = new JButton("XML...");
    structuredReportTreeXMLButton.setToolTipText("Save displayed SR to XML file");
    structuredReportTreeButtonsPanel.add(structuredReportTreeXMLButton);
    JButton structuredReportTreeValidateButton = new JButton("Validate...");
    structuredReportTreeValidateButton.setToolTipText("Validate displayed SR against standard IOD and templates");
    structuredReportTreeButtonsPanel.add(structuredReportTreeValidateButton);

    structuredReportTreeScrollPane = new JScrollPane();
    structuredReportTreeControlsPanel.add(structuredReportTreeScrollPane, BorderLayout.CENTER);

    browserPane = new JTabbedPane();
    // browserPane.setBorder(emptyBorder);
    // browserPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT); // this is
    // (effectively) what recent Mac JREs do, though they also select new tabs
    // (unlike Windows and Metal)
    browserPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT); // this is the
                                                                 // default
                                                                 // anyway; it
                                                                 // is also
                                                                 // ignored on
                                                                 // recent Mac
                                                                 // JREs :(
    browserPane.addTab("Local", databaseControlsPanel);
    browserPane.addTab("Remote", queryControlsPanel);
    browserPane.addTab("DICOMDIR", dicomdirControlsPanel);
    browserPane.addTab("Image", displayControlsPanel);
    browserPane.addTab("Report", structuredReportTreeControlsPanel);
    browserPane.addTab("Spectra", spectroscopyControlsPanel);
    browserPane.addTab("Attributes", attributeTreeControlsPanel);
    browserPane.addTab("Frames", attributeFrameTableScrollPane);

    int tabNumberOfRemoteInBrowserPane = browserPane.indexOfComponent(queryControlsPanel);

    browserPane.setToolTipTextAt(browserPane.indexOfComponent(databaseControlsPanel), "Browse the contents of the local database");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(queryControlsPanel), "Browse the contents of the local database");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(dicomdirControlsPanel), "Browse the contents of the currently loaded DICOMDIR");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(displayControlsPanel), "Controls for the currently displayed image");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(spectroscopyControlsPanel), "Controls for the currently displayed spectra");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(attributeTreeControlsPanel), "Tree of attributes and values for currently displayed image");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(attributeFrameTableScrollPane), "Table of all per-frame varying attributes for this object");
    browserPane.setToolTipTextAt(browserPane.indexOfComponent(structuredReportTreeControlsPanel), "Tree of current structured report content");

    browserPane.setEnabledAt(browserPane.indexOfComponent(displayControlsPanel), false);
    browserPane.setEnabledAt(browserPane.indexOfComponent(spectroscopyControlsPanel), false);
    browserPane.setEnabledAt(browserPane.indexOfComponent(structuredReportTreeControlsPanel), false);

    browserPane.addChangeListener(new ChangeListener() {
      // This method is called whenever the selected tab changes
      @Override
      public void stateChanged(ChangeEvent evt) {
        JTabbedPane pane = (JTabbedPane) evt.getSource();
        // Get current tab
        int sel = pane.getSelectedIndex();
        // System.err.println("browserPane.ChangeListener(): selection "+sel);
        if (sel == browserPane.indexOfComponent(databaseControlsPanel)) {
          try {
            new OurDatabaseTreeBrowser();
          } catch (Exception e) {
            slf4jlogger.error("", e);
          }
        }
      }
    });

    // Set up query model based on properties ... (have to wait till now to know
    // browserPane etc.)

    setCurrentRemoteQueryInformationModel(properties.getProperty(propertyName_DicomCurrentlySelectedQueryTargetAE), browserPane, tabNumberOfRemoteInBrowserPane);
    initializeCurrentRemoteQueryFilter();

    JLabel statusBar = getStatusBar();

    // System.err.println("Loading DICOM file or chooser ...");
    if (dicomFileName == null) {
      lastDirectoryPath = null;
    } else {
      lastDirectoryPath = new File(dicomFileName).getParent();
      loadDicomFileOrDirectory(dicomFileName, multiPanel,
          referenceImagePanelForImages,
          referenceImagePanelForSpectra);
    }

    // selecting the local database here explicitly forces tree browser to load
    // the first time
    if (lastDirectoryPath == null) { // not a very robust flag for no DICOMDIR
                                     // or input file, but it will do
      browserPane.setSelectedIndex(-1); // since the default is 0 already,
                                        // deselection first is necessary to
                                        // force explicit change event
      browserPane.setSelectedIndex(browserPane.indexOfComponent(databaseControlsPanel));
    }

    // Add action listeners for various buttons now that all the various display
    // components are available for them to remember ..
    // System.err.println("Building action listeners ...");
    DicomFileOrDirectoryLoadActionListener dicomFileOrDirectoryLoadActionListener = new DicomFileOrDirectoryLoadActionListener(multiPanel,
        referenceImagePanelForImages,
        referenceImagePanelForSpectra);

    dicomdirFileButton.addActionListener(dicomFileOrDirectoryLoadActionListener);
    displayFileButton.addActionListener(dicomFileOrDirectoryLoadActionListener);
    spectroscopyFileButton.addActionListener(dicomFileOrDirectoryLoadActionListener);
    structuredReportTreeFileButton.addActionListener(dicomFileOrDirectoryLoadActionListener);

    ImportCurrentlyDisplayedInstanceToDatabaseActionListener importCurrentlyDisplayedInstanceToDatabaseActionListener = new ImportCurrentlyDisplayedInstanceToDatabaseActionListener();

    displayImportButton.addActionListener(importCurrentlyDisplayedInstanceToDatabaseActionListener);
    spectroscopyImportButton.addActionListener(importCurrentlyDisplayedInstanceToDatabaseActionListener);
    structuredReportTreeImportButton.addActionListener(importCurrentlyDisplayedInstanceToDatabaseActionListener);

    dicomdirImportButton.addActionListener(new ImportFromSelectionToDatabaseActionListener());

    dicomdirViewSelectionButton.addActionListener(new DicomFileLoadFromSelectionActionListener());

    databaseViewSelectionButton.addActionListener(new DicomFileLoadFromSelectionActionListener());

    databaseRefreshButton.addActionListener(new DatabaseRefreshActionListener());

    databaseFileButton.addActionListener(new DatabaseImportFromFilesActionListener());

    databasePurgeButton.addActionListener(new DatabasePurgeActionListener());

    querySelectButton.addActionListener(new QuerySelectActionListener(browserPane, tabNumberOfRemoteInBrowserPane));
    queryFilterButton.addActionListener(new QueryFilterActionListener(browserPane, tabNumberOfRemoteInBrowserPane));
    queryRefreshButton.addActionListener(new QueryRefreshActionListener());
    queryRetrieveButton.addActionListener(new QueryRetrieveActionListener());

    NetworkSendCurrentSelectionActionListener dicomFileOrDirectoryOrDatabaseSendActionListener = new NetworkSendCurrentSelectionActionListener();
    dicomdirSendButton.addActionListener(dicomFileOrDirectoryOrDatabaseSendActionListener);
    displaySendButton.addActionListener(dicomFileOrDirectoryOrDatabaseSendActionListener);
    spectroscopySendButton.addActionListener(dicomFileOrDirectoryOrDatabaseSendActionListener);
    structuredReportTreeSendButton.addActionListener(dicomFileOrDirectoryOrDatabaseSendActionListener);
    databaseSendButton.addActionListener(dicomFileOrDirectoryOrDatabaseSendActionListener);

    SaveCurrentlyDisplayedImageToXMLActionListener saveCurrentlyDisplayedImageToXMLActionListener = new SaveCurrentlyDisplayedImageToXMLActionListener();
    displayXMLButton.addActionListener(saveCurrentlyDisplayedImageToXMLActionListener);
    spectroscopyXMLButton.addActionListener(saveCurrentlyDisplayedImageToXMLActionListener);

    SaveCurrentlyDisplayedStructuredReportToXMLActionListener saveCurrentlyDisplayedStructuredReportToXMLActionListener = new SaveCurrentlyDisplayedStructuredReportToXMLActionListener();
    structuredReportTreeXMLButton.addActionListener(saveCurrentlyDisplayedStructuredReportToXMLActionListener);

    // System.err.println("Building
    // ValidateCurrentlyDisplayedImageActionListener ...");
    ValidateCurrentlyDisplayedImageActionListener validateCurrentlyDisplayedImageActionListener = new ValidateCurrentlyDisplayedImageActionListener();
    displayValidateButton.addActionListener(validateCurrentlyDisplayedImageActionListener);
    spectroscopyValidateButton.addActionListener(validateCurrentlyDisplayedImageActionListener);

    ValidateCurrentlyDisplayedStructuredReportActionListener validateCurrentlyDisplayedStructuredReportActionListener = new ValidateCurrentlyDisplayedStructuredReportActionListener();
    structuredReportTreeValidateButton.addActionListener(validateCurrentlyDisplayedStructuredReportActionListener);

    // Layout the rest of the GUI components ...

    JSplitPane browserAndMultiPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browserPane, multiPanel);
    browserAndMultiPane.setOneTouchExpandable(true);
    browserAndMultiPane.setBorder(emptyBorder);

    JSplitPane browserAndMultiPaneAndCurrentAttributes = new JSplitPane(JSplitPane.VERTICAL_SPLIT, browserAndMultiPane, scrollPaneOfCurrentAttributes);
    browserAndMultiPaneAndCurrentAttributes.setOneTouchExpandable(true);
    browserAndMultiPaneAndCurrentAttributes.setResizeWeight(browserAndMultiPaneAndCurrentAttributesResizeWeight);

    // content.add(browserAndMultiPaneAndCurrentAttributes);

    Box mainPanel = new Box(BoxLayout.Y_AXIS);
    mainPanel.add(browserAndMultiPaneAndCurrentAttributes);
    // make label really wide, else doesn't completely repaint on status update
    mainPanel.add(statusBar);
    content.add(mainPanel);

    Dimension multiPanelDimension = defaultMultiPanelDimension;
    multiPanel.setSize(multiPanelDimension);
    multiPanel.setPreferredSize(multiPanelDimension);
    browserPane.setPreferredSize(new Dimension(widthWantedForBrowser, (int) multiPanel.getPreferredSize().getHeight()));
    {
      Dimension d = getTableOfCurrentAttributesForCurrentFrameBrowser().getPreferredSize();
      int w = (int) d.getWidth();
      int h = (int) d.getHeight();
      int wWanted = widthWantedForBrowser + (int) multiPanel.getPreferredSize().getWidth();
      if (w > wWanted)
        w = wWanted;
      if (h < heightWantedForAttributeTable)
        h = heightWantedForAttributeTable;
      scrollPaneOfCurrentAttributes.setPreferredSize(new Dimension(w, h));
    }

    // See
    // "http://java.sun.com/docs/books/tutorial/extra/fullscreen/example-1dot4/DisplayModeTest.java"

    boolean allowFullScreen = false;
    {
      String fullScreen = properties.getProperty(propertyName_FullScreen);
      if (fullScreen != null && fullScreen.equals("true")) {
        allowFullScreen = true;
      }
    }
    GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    boolean isFullScreen = allowFullScreen && devices.length == 1 && devices[0].isFullScreenSupported();
    setUndecorated(isFullScreen);
    setResizable(!isFullScreen);
    if (isFullScreen) {
      // System.err.println("Full screen ...");
      devices[0].setFullScreenWindow(this);
      validate();
    } else {
      // Windowed mode
      pack();
      setVisible(true);
    }
  }

  // override ApplicationFrame methods and relevant constructors ...

  /**
   * @param title
   * @param w
   * @param h
   */
  private DicomImageViewer(String title, int w, int h) {
  }

  /**
   * @param title
   */
  private DicomImageViewer(String title) {
  }

  /**
   * @param title
   * @param dicomFileName
   */
  private DicomImageViewer(String title, String dicomFileName) {
  }

  /**
   * @param title
   * @param applicationPropertyFileName
   * @param dicomFileName
   */
  private DicomImageViewer(String title, String applicationPropertyFileName, String dicomFileName) {
    super(title, applicationPropertyFileName);
    doCommonConstructorStuff(title, dicomFileName);
  }

  /**
   * <p>
   * The method to invoke the application.
   * </p>
   *
   * @param arg
   *        optionally, a single file which may be a DICOM object or DICOMDIR
   */
  public static void main(String arg[]) {
    String dicomFileName = null;
    if (arg.length == 1) {
      dicomFileName = arg[0].trim();
      if (dicomFileName.length() == 0) {
        dicomFileName = null;
      }
    }

    if (System.getProperty("mrj.version") != null) {
      System.setProperty("apple.awt.fakefullscreen", "true"); // Must be done
                                                              // before creating
                                                              // components
    }

    DicomImageViewer af = new DicomImageViewer("Dicom Image Viewer", propertiesFileName, dicomFileName);
  }
}

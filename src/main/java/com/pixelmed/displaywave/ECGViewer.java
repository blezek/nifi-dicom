/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.displaywave;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTreeBrowser;
import com.pixelmed.dicom.BinaryInputStream;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.display.ApplicationFrame;
import com.pixelmed.display.SafeFileChooser;

import com.pixelmed.scpecg.SCPECG;
import com.pixelmed.scpecg.SCPTreeBrowser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is an entire application for displaying and viewing DICOM and SCP ECG waveforms.</p>
 * 
 * @author	dclunie
 */
public class ECGViewer extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/displaywave/ECGViewer.java,v 1.11 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ECGViewer.class);

	private final float milliMetresPerPixel = (float)(25.4/72);				// assume screen is 72 dpi aka 72/25.4 pixels/mm
	//private final float horizontalPixelsPerMilliSecond = 25/(1000*milliMetresPerPixel);	// ECG's normally printed at 25mm/sec and 10 mm/mV
	//private final float verticalPixelsPerMilliVolt = 10/(milliMetresPerPixel);

	private final int defaultHeightOfTileInMicroVolts = 2000;				// should be a multiple of grid size, i.e. 0.5mV
	private final int minimumHeightOfTileInMicroVolts = 1000;				// should be a multiple of grid size, i.e. 0.5mV
	private final int maximumHeightOfTileInMicroVolts = 5000;				// should be a multiple of grid size, i.e. 0.5mV
	private final int minorIntervalHeightOfTileInMicroVolts = 500;				// should be a multiple of grid size, i.e. 0.5mV
	private final int majorIntervalHeightOfTileInMicroVolts = 1000;				// should be a multiple of grid size, i.e. 0.5mV
	private final String heightOfTileSliderLabel = "Height of tile in uV";
	
	private final int defaultHorizontalScalingInMilliMetresPerSecond = 25;
	private final int minimumHorizontalScalingInMilliMetresPerSecond = 10;
	private final int maximumHorizontalScalingInMilliMetresPerSecond = 50;
	private final int minorIntervalHorizontalScalingInMilliMetresPerSecond = 5;
	private final int majorIntervalHorizontalScalingInMilliMetresPerSecond = 10;
	private final String horizontalScalingSliderLabel = "mm/S";
	
	private final int defaultVerticalScalingInMilliMetresPerMilliVolt = 10;
	private final int minimumVerticalScalingInMilliMetresPerMilliVolt = 5;
	private final int maximumVerticalScalingInMilliMetresPerMilliVolt = 25;
	private final int minorIntervalVerticalScalingInMilliMetresPerMilliVolt = 5;
	private final int majorIntervalVerticalScalingInMilliMetresPerMilliVolt = 10;
	private final String verticalScalingSliderLabel = "mm/mV";
	
	private final int maximumSliderWidth = 320;
	private final int maximumSliderHeight = 100;			// largely irrelevant
	
	private final int minimumAttributeTreePaneWidth = 200;

	private int applicationWidth;
	private int applicationHeight;

	/**
	 * @param	application
	 * @param	sourceECG
	 * @param	scrollPaneOfDisplayedECG
	 * @param	scrollPaneOfAttributeTree
	 * @param	requestedHeightOfTileInMicroVolts
	 * @param	requestedHorizontalScalingInMilliMetresPerSecond
	 * @param	requestedVerticalScalingInMilliMetresPerMilliVolt
	 */
	private void loadSourceECGIntoScrollPane(JFrame application,SourceECG sourceECG,
			JScrollPane scrollPaneOfDisplayedECG,JScrollPane scrollPaneOfAttributeTree,
			int requestedHeightOfTileInMicroVolts,
			int requestedHorizontalScalingInMilliMetresPerSecond,
			int requestedVerticalScalingInMilliMetresPerMilliVolt) {
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): start");
		Cursor was = application.getCursor();
		application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		int numberOfChannels = sourceECG.getNumberOfChannels();
		int numberOfSamplesPerChannel = sourceECG.getNumberOfSamplesPerChannel();
		float samplingIntervalInMilliSeconds = sourceECG.getSamplingIntervalInMilliSeconds();
		
		int nTilesPerColumn = numberOfChannels;
		int nTilesPerRow = 1;
		int timeOffsetInMilliSeconds = 0;

		float requestedHeightOfTileInMilliVolts=(float)(requestedHeightOfTileInMicroVolts)/1000f;
		float horizontalPixelsPerMilliSecond=(float)(requestedHorizontalScalingInMilliMetresPerSecond)/(1000*milliMetresPerPixel);
		float verticalPixelsPerMilliVolt=(float)(requestedVerticalScalingInMilliMetresPerMilliVolt)/milliMetresPerPixel;

		float widthOfPixelInMilliSeconds = 1/horizontalPixelsPerMilliSecond;
		float widthOfSampleInPixels = samplingIntervalInMilliSeconds/widthOfPixelInMilliSeconds;
		int maximumWidthOfRowInSamples = numberOfSamplesPerChannel*nTilesPerRow;
		int maximumWidthOfRowInPixels  = (int)(maximumWidthOfRowInSamples*widthOfSampleInPixels);
		
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): horizontalPixelsPerMilliSecond="+horizontalPixelsPerMilliSecond);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): widthOfPixelInMilliSeconds="+widthOfPixelInMilliSeconds);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): widthOfSampleInPixels="+widthOfSampleInPixels);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): maximumWidthOfRowInSamples="+maximumWidthOfRowInSamples);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): maximumWidthOfRowInPixels="+maximumWidthOfRowInPixels);

		int widthLeftAfterMinimumAttributeTree = applicationWidth - minimumAttributeTreePaneWidth;
		int wantWidthOfRowInPixels = maximumWidthOfRowInPixels > widthLeftAfterMinimumAttributeTree
					? widthLeftAfterMinimumAttributeTree
					: maximumWidthOfRowInPixels;
		int wantWidthOfAttributeTree = applicationWidth - wantWidthOfRowInPixels;
		
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): applicationWidth="+applicationWidth);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): minimumAttributeTreePaneWidth="+minimumAttributeTreePaneWidth);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): wantWidthOfRowInPixels="+wantWidthOfRowInPixels);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): wantWidthOfAttributeTree="+wantWidthOfAttributeTree);

		float heightOfPixelInMilliVolts = 1/verticalPixelsPerMilliVolt;
		int wantECGPanelheight = (int)(nTilesPerColumn * requestedHeightOfTileInMilliVolts/heightOfPixelInMilliVolts);
		
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): verticalPixelsPerMilliVolt="+verticalPixelsPerMilliVolt);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): heightOfPixelInMilliVolts="+heightOfPixelInMilliVolts);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): nTilesPerColumn="+nTilesPerColumn);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): requestedHeightOfTileInMilliVolts="+requestedHeightOfTileInMilliVolts);
//System.err.println("ECGViewer.loadSourceECGIntoScrollPane(): wantECGPanelheight="+wantECGPanelheight);

		ECGPanel pg = new ECGPanel(
			sourceECG.getSamples(),
			numberOfChannels,
			numberOfSamplesPerChannel,
			sourceECG.getChannelNames(),
			nTilesPerColumn,
			nTilesPerRow,
			samplingIntervalInMilliSeconds,
			sourceECG.getAmplitudeScalingFactorInMilliVolts(),
			horizontalPixelsPerMilliSecond,
			verticalPixelsPerMilliVolt,
			timeOffsetInMilliSeconds,
			sourceECG.getDisplaySequence(),
			maximumWidthOfRowInPixels,wantECGPanelheight);
		
		pg.setPreferredSize(new Dimension(maximumWidthOfRowInPixels,wantECGPanelheight));
		//pg.setPreferredSize(new Dimension(wantWidthOfRowInPixels,wantECGPanelheight));
		scrollPaneOfDisplayedECG.setViewportView(pg);
		//scrollPaneOfDisplayedECG.setPreferredSize (new Dimension(wantWidthOfRowInPixels,applicationHeight));
		//scrollPaneOfAttributeTree.setPreferredSize(new Dimension(wantWidthOfAttributeTree,applicationHeight));
		scrollPaneOfAttributeTree.setPreferredSize(new Dimension(wantWidthOfAttributeTree,0));
		application.setCursor(was);
	}
	
	/**
	 * @param	inputFileName
	 * @param	application
	 * @param	scrollPaneOfDisplayedECG
	 * @param	scrollPaneOfAttributeTree
	 * @return 						a SourceECG, or null if load failed
	 */
	private SourceECG loadDicomFile(
			String inputFileName,
			JFrame application,
			JScrollPane scrollPaneOfDisplayedECG,
			JScrollPane scrollPaneOfAttributeTree) {
		SourceECG sourceECG = null;
		if (inputFileName != null) {
			try {
//System.err.println("Try as DICOM: "+inputFileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(inputFileName)));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				new AttributeTreeBrowser(list,scrollPaneOfAttributeTree);
				
				// choose type of object based on SOP Class
				// Note that DICOMDIRs don't have SOPClassUID, so check MediaStorageSOPClassUID first
				// then only if not found (e.g. and image with no meta-header, use SOPClassUID from SOP Common Module
				Attribute a = list.get(TagFromName.MediaStorageSOPClassUID);
				String useSOPClassUID = (a != null && a.getVM() == 1) ? a.getStringValues()[0] : null;
				if (useSOPClassUID == null) {
					a = list.get(TagFromName.SOPClassUID);
					useSOPClassUID = (a != null && a.getVM() == 1) ? a.getStringValues()[0] : null;
				}
				
				//if (useSOPClassUID.equals(SOPClass.TwelveLeadECGStorage)) {
				if (SOPClass.isWaveform(useSOPClassUID)) {
					sourceECG = new DicomSourceECG(list);
				}
				else {
					throw new Exception("unsupported SOP Class "+useSOPClassUID);
				}
			} catch (Exception e) {
				//e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
		return sourceECG;
	}
	
	/**
	 * @param	inputFileName
	 * @param	application
	 * @param	scrollPaneOfDisplayedECG
	 * @param	scrollPaneOfAttributeTree
	 * @return 						a SourceECG, or null if load failed
	 */
	private SourceECG loadSCPECGFile(
			String inputFileName,
			JFrame application,
			JScrollPane scrollPaneOfDisplayedECG,
			JScrollPane scrollPaneOfAttributeTree) {
		SourceECG sourceECG = null;
		if (inputFileName != null) {
			try {
//System.err.println("Try as SCPECG: "+inputFileName);
				BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(inputFileName)),false);	// SCP-ECG always little endian
				SCPECG scpecg = new SCPECG(i,false/*verbose*/);
				new SCPTreeBrowser(scpecg,scrollPaneOfAttributeTree);
				sourceECG = new SCPSourceECG(scpecg,true/*deriveAdditionalLeads*/);
			} catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
		return sourceECG;
	}
	
	/**
	 * @param	inputFileName
	 * @param	application
	 * @param	scrollPaneOfDisplayedECG
	 * @param	scrollPaneOfAttributeTree
	 * @return 						a SourceECG, or null if load failed
	 */
	private SourceECG loadECGFile(
			String inputFileName,JFrame application,JScrollPane scrollPaneOfDisplayedECG,
			JScrollPane scrollPaneOfAttributeTree) {
		Cursor was = application.getCursor();
		application.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SourceECG sourceECG = null;
		if (inputFileName == null) {
			// we are not on the Swing event dispatcher thread, so ...
			SafeFileChooser.SafeFileChooserThread fileChooserThread = new SafeFileChooser.SafeFileChooserThread();
			try {
				java.awt.EventQueue.invokeAndWait(fileChooserThread);
				inputFileName=fileChooserThread.getSelectedFileName();
				lastDirectoryPath=fileChooserThread.getCurrentDirectoryPath();
			}
			catch (InterruptedException e) {
				slf4jlogger.error("",e);
			}
			catch (InvocationTargetException e) {
				slf4jlogger.error("",e);
			}
		}
		slf4jlogger.info("Loading: {}",inputFileName);
		// remove currently displayed image and attribute tree in case load fails
		scrollPaneOfDisplayedECG.setViewportView(null);
		scrollPaneOfDisplayedECG.repaint();
		scrollPaneOfAttributeTree.setViewportView(null);
		scrollPaneOfAttributeTree.repaint();

		sourceECG = loadDicomFile(inputFileName,application,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree);
		if (sourceECG == null) {
			sourceECG = loadSCPECGFile(inputFileName,application,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree);
		}
		application.setCursor(was);
		return sourceECG;
	}

	/***/
	private String lastDirectoryPath;	// remember between invocations of dialog
	
	/***/
	private class ResetScalingToDefaultsActionListener implements ActionListener {
		/***/
		CommonScalingSliderChangeListener scalingChangeListener;
		/***/
		int defaultHeightOfTileInMicroVolts;
		/***/
		int defaultHorizontalScalingInMilliMetresPerSecond;
		/***/
		int defaultVerticalScalingInMilliMetresPerMilliVolt;

		/**
		 * @param	scalingChangeListener
		 * @param	defaultHeightOfTileInMicroVolts
		 * @param	defaultHorizontalScalingInMilliMetresPerSecond
		 * @param	defaultVerticalScalingInMilliMetresPerMilliVolt
		 */
		public ResetScalingToDefaultsActionListener(CommonScalingSliderChangeListener scalingChangeListener,
				int defaultHeightOfTileInMicroVolts,int defaultHorizontalScalingInMilliMetresPerSecond,int defaultVerticalScalingInMilliMetresPerMilliVolt) {
			this.scalingChangeListener=scalingChangeListener;
			this.defaultHeightOfTileInMicroVolts=defaultHeightOfTileInMicroVolts;
			this.defaultHorizontalScalingInMilliMetresPerSecond=defaultHorizontalScalingInMilliMetresPerSecond;
			this.defaultVerticalScalingInMilliMetresPerMilliVolt=defaultVerticalScalingInMilliMetresPerMilliVolt;
		}
		
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
			scalingChangeListener.setValuesAndRedraw(
				defaultHeightOfTileInMicroVolts,defaultHorizontalScalingInMilliMetresPerSecond,defaultVerticalScalingInMilliMetresPerMilliVolt);
		}
	}

	/***/
	private class CommonScalingSliderChangeListener implements ChangeListener {
		/***/
		JSlider heightOfTileSlider;
		/***/
		JSlider horizontalScalingSlider;
		/***/
		JSlider verticalScalingSlider;
		/***/
		JFrame application;
		/***/
		SourceECG sourceECG;
		/***/
		JScrollPane scrollPaneOfDisplayedECG;
		/***/
		JScrollPane scrollPaneOfAttributeTree;
		/***/
		int requestedHeightOfTileInMicroVolts;
		/***/
		int requestedHorizontalScalingInMilliMetresPerSecond;
		/***/
		int requestedVerticalScalingInMilliMetresPerMilliVolt;
		
		/**
		 * @param	sourceECG
		 * @param	application
		 * @param	scrollPaneOfDisplayedECG
		 * @param	scrollPaneOfAttributeTree
		 * @param	heightOfTileSlider
		 * @param	horizontalScalingSlider
		 * @param	verticalScalingSlider
		 * @param	requestedHeightOfTileInMicroVolts
		 * @param	requestedHorizontalScalingInMilliMetresPerSecond
		 * @param	requestedVerticalScalingInMilliMetresPerMilliVolt
		 */
		public CommonScalingSliderChangeListener(
				JFrame application,SourceECG sourceECG,
				JScrollPane scrollPaneOfDisplayedECG,JScrollPane scrollPaneOfAttributeTree,
				JSlider heightOfTileSlider,JSlider horizontalScalingSlider,JSlider verticalScalingSlider,
				int requestedHeightOfTileInMicroVolts,int requestedHorizontalScalingInMilliMetresPerSecond,int requestedVerticalScalingInMilliMetresPerMilliVolt) {
				
			this.heightOfTileSlider=heightOfTileSlider;
			this.horizontalScalingSlider=horizontalScalingSlider;
			this.verticalScalingSlider=verticalScalingSlider;
			
			this.requestedHeightOfTileInMicroVolts=requestedHeightOfTileInMicroVolts;
			this.requestedHorizontalScalingInMilliMetresPerSecond=requestedHorizontalScalingInMilliMetresPerSecond;
			this.requestedVerticalScalingInMilliMetresPerMilliVolt=requestedVerticalScalingInMilliMetresPerMilliVolt;
			
			this.application=application;
			this.sourceECG=sourceECG;
			this.scrollPaneOfDisplayedECG=scrollPaneOfDisplayedECG;
			this.scrollPaneOfAttributeTree=scrollPaneOfAttributeTree;
		}
		
		/**
		 * @param	e
		 */
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider)(e.getSource());
			if (!slider.getValueIsAdjusting()) {
				boolean changed = false;
				int value=slider.getValue();
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedHeightOfTileInMicroVolts = "+requestedHeightOfTileInMicroVolts);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedHorizontalScalingInMilliMetresPerSecond = "+requestedHorizontalScalingInMilliMetresPerSecond);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedVerticalScalingInMilliMetresPerMilliVolt = "+requestedVerticalScalingInMilliMetresPerMilliVolt);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): value = "+value);
				if (slider ==  heightOfTileSlider) {
					if (value != requestedHeightOfTileInMicroVolts) {
						requestedHeightOfTileInMicroVolts=value;
						changed=true;
					}
				}
				else if (slider ==  horizontalScalingSlider) {
					if (value != requestedHorizontalScalingInMilliMetresPerSecond) {
						requestedHorizontalScalingInMilliMetresPerSecond=value;
						changed=true;
					}
				}
				else if (slider ==  verticalScalingSlider) {
					if (value != requestedVerticalScalingInMilliMetresPerMilliVolt) {
						requestedVerticalScalingInMilliMetresPerMilliVolt=value;
						changed=true;
					}
				}
				if (changed) {
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): changed = "+changed);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedHeightOfTileInMicroVolts = "+requestedHeightOfTileInMicroVolts);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedHorizontalScalingInMilliMetresPerSecond = "+requestedHorizontalScalingInMilliMetresPerSecond);
//System.err.println("CommonScalingSliderChangeListener.stateChanged(): requestedVerticalScalingInMilliMetresPerMilliVolt = "+requestedVerticalScalingInMilliMetresPerMilliVolt);
					loadSourceECGIntoScrollPane(application,sourceECG,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree,
						requestedHeightOfTileInMicroVolts,
						requestedHorizontalScalingInMilliMetresPerSecond,
						requestedVerticalScalingInMilliMetresPerMilliVolt);
				}
			}
		}
		
		// our own methods ...
		
		/**
		 * @param	requestedHeightOfTileInMicroVolts
		 * @param	requestedHorizontalScalingInMilliMetresPerSecond
		 * @param	requestedVerticalScalingInMilliMetresPerMilliVolt
		 */
		public void setValuesAndRedraw(
				int requestedHeightOfTileInMicroVolts,int requestedHorizontalScalingInMilliMetresPerSecond,int requestedVerticalScalingInMilliMetresPerMilliVolt) {
//System.err.println("CommonScalingSliderChangeListener.setValuesAndRedraw():");
			
			heightOfTileSlider.setValue(requestedHeightOfTileInMicroVolts);
			horizontalScalingSlider.setValue(requestedHorizontalScalingInMilliMetresPerSecond);
			verticalScalingSlider.setValue(requestedVerticalScalingInMilliMetresPerMilliVolt);
			
			boolean changed = false;
			if (this.requestedHeightOfTileInMicroVolts != requestedHeightOfTileInMicroVolts) {
				this.requestedHeightOfTileInMicroVolts=requestedHeightOfTileInMicroVolts;
				changed=true;
			}
			if (this.requestedHorizontalScalingInMilliMetresPerSecond != requestedHorizontalScalingInMilliMetresPerSecond) {
				this.requestedHorizontalScalingInMilliMetresPerSecond=requestedHorizontalScalingInMilliMetresPerSecond;
				changed=true;
			}
			if (this.requestedVerticalScalingInMilliMetresPerMilliVolt != requestedVerticalScalingInMilliMetresPerMilliVolt) {
				this.requestedVerticalScalingInMilliMetresPerMilliVolt=requestedVerticalScalingInMilliMetresPerMilliVolt;
				changed=true;
			}
			if (changed) {
//System.err.println("CommonScalingSliderChangeListener.setValuesAndRedraw(): changed = "+changed);
				loadSourceECGIntoScrollPane(application,sourceECG,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree,
					requestedHeightOfTileInMicroVolts,
					requestedHorizontalScalingInMilliMetresPerSecond,
					requestedVerticalScalingInMilliMetresPerMilliVolt);
			}
		}
	}
	
	/**
	 * @param	parent
	 * @param	initial
	 * @param	minimum
	 * @param	maximum
	 * @param	major
	 * @param	minor
	 * @param	labelText
	 */
	private final JSlider addCommonSlider(
			JPanel parent,
			GridBagLayout layout,
			GridBagConstraints constraints,
			int initial,int minimum,
			int maximum,int major,int minor,
			String labelText) {
		JSlider slider = new JSlider(minimum,maximum,initial);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setMajorTickSpacing(major);
		slider.setMinorTickSpacing(minor);
		slider.setSnapToTicks(true);		// NB. this must NOT be called until ticks set, else snaps initial value to center of range !
		JLabel label = new JLabel(labelText);
		parent.add(label);
		layout.setConstraints(label,constraints);
		parent.add(slider);
		layout.setConstraints(slider,constraints);
		slider.setMaximumSize(new Dimension(maximumSliderWidth,maximumSliderHeight));
		return slider;
	}

	/**
	 * @param	title
	 * @param	inputFileName
	 */
	private void doCommonConstructorStuff(String title,String inputFileName) {
//System.err.println("ECGViewer.doCommonConstructorStuff():");

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//applicationWidth  = (int)(screenSize.getWidth())  - 20;
		//applicationHeight = (int)(screenSize.getHeight()) - 70;
		applicationWidth  = 1024;
		applicationHeight = 700;

		JScrollPane scrollPaneOfDisplayedECG  = new JScrollPane();
		JScrollPane scrollPaneOfAttributeTree = new JScrollPane();
		
		SourceECG sourceECG = loadECGFile(inputFileName,this,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree);
		if (sourceECG != null) {
			loadSourceECGIntoScrollPane(this,sourceECG,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree,
				defaultHeightOfTileInMicroVolts,defaultHorizontalScalingInMilliMetresPerSecond,defaultVerticalScalingInMilliMetresPerMilliVolt);
		}

		JPanel controlsPanel = new JPanel();
		controlsPanel.setPreferredSize(new Dimension(minimumAttributeTreePaneWidth,50));
		
		GridBagLayout controlsPanelLayout = new GridBagLayout();
		controlsPanel.setLayout(controlsPanelLayout);
		GridBagConstraints controlsPanelConstraints = new GridBagConstraints();
		controlsPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;
		
		JSlider heightOfTileSlider = addCommonSlider(controlsPanel,controlsPanelLayout,controlsPanelConstraints,
				defaultHeightOfTileInMicroVolts,minimumHeightOfTileInMicroVolts,maximumHeightOfTileInMicroVolts,
				majorIntervalHeightOfTileInMicroVolts,minorIntervalHeightOfTileInMicroVolts,
				heightOfTileSliderLabel);
		
		JSlider horizontalScalingSlider = addCommonSlider(controlsPanel,controlsPanelLayout,controlsPanelConstraints,
				defaultHorizontalScalingInMilliMetresPerSecond,minimumHorizontalScalingInMilliMetresPerSecond,maximumHorizontalScalingInMilliMetresPerSecond,
				majorIntervalHorizontalScalingInMilliMetresPerSecond,minorIntervalHorizontalScalingInMilliMetresPerSecond,
				horizontalScalingSliderLabel);

		JSlider verticalScalingSlider = addCommonSlider(controlsPanel,controlsPanelLayout,controlsPanelConstraints,
				defaultVerticalScalingInMilliMetresPerMilliVolt,minimumVerticalScalingInMilliMetresPerMilliVolt,maximumVerticalScalingInMilliMetresPerMilliVolt,
				majorIntervalVerticalScalingInMilliMetresPerMilliVolt,minorIntervalVerticalScalingInMilliMetresPerMilliVolt,
				verticalScalingSliderLabel);

		CommonScalingSliderChangeListener commonScalingSliderChangeListener = new CommonScalingSliderChangeListener(
				this,sourceECG,scrollPaneOfDisplayedECG,scrollPaneOfAttributeTree,
				heightOfTileSlider,horizontalScalingSlider,verticalScalingSlider,
				defaultHeightOfTileInMicroVolts,defaultHorizontalScalingInMilliMetresPerSecond,defaultVerticalScalingInMilliMetresPerMilliVolt);
		
		heightOfTileSlider.addChangeListener(commonScalingSliderChangeListener);
		horizontalScalingSlider.addChangeListener(commonScalingSliderChangeListener);
		verticalScalingSlider.addChangeListener(commonScalingSliderChangeListener);
		
		JButton defaultButton = new JButton("Default");
		defaultButton.setToolTipText("Reset scaling to defaults");
		controlsPanel.add(defaultButton);
		defaultButton.addActionListener(new ResetScalingToDefaultsActionListener(
			commonScalingSliderChangeListener,defaultHeightOfTileInMicroVolts,defaultHorizontalScalingInMilliMetresPerSecond,defaultVerticalScalingInMilliMetresPerMilliVolt));
		controlsPanelLayout.setConstraints(defaultButton,controlsPanelConstraints);

		Container content = getContentPane();
		
		JSplitPane attributeTreeAndControls = new JSplitPane(JSplitPane.VERTICAL_SPLIT,controlsPanel,scrollPaneOfAttributeTree);

		JSplitPane attributeTreeAndDisplayedECG = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,attributeTreeAndControls,scrollPaneOfDisplayedECG);
		content.add(attributeTreeAndDisplayedECG);
		
		pack();
		setVisible(true);
	}
	
	// override ApplicationFrame methods and relevant constructors ...

	/**
	 * @param	title
	 * @param	w
	 * @param	h
	 */
	private ECGViewer(String title,int w,int h) { 
	} 

	/**
	 * @param	title
	 */
	private ECGViewer(String title) {
	} 

	/**
	 * @param	title
	 * @param	inputFileName
	 */
	private ECGViewer(String title,String inputFileName) {
		super(title,null);
		doCommonConstructorStuff(title,inputFileName);
	}

	/**
	 * @param	title
	 * @param	applicationPropertyFileName
	 * @param	inputFileName
	 */
	private ECGViewer(String title,String applicationPropertyFileName,String inputFileName) {
		super(title,applicationPropertyFileName);
		doCommonConstructorStuff(title,inputFileName);
	}
	
	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg	optionally, a single file which may be a DICOM or an SCP-ECG waveform; if absent a file dialog is presented
	 */
	public static void main(String arg[]) {
		String inputFileName = null;
		if (arg.length == 1) {
			inputFileName=arg[0];
		}
		
		ECGViewer af = new ECGViewer("ECG Viewer",inputFileName);
	}
}


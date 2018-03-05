/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.*; 
import com.pixelmed.dicom.*;
import com.pixelmed.event.EventContext;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*; 
import java.awt.color.*; 
import java.awt.geom.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class is an entire application for displaying and viewing chest x-ray images.</p>
 * 
 * 
 * <p>It is invoked using a main method with a list of DICOM image file names.</p>
 * 
 * @author	dclunie
 */
public class ChestImageViewer {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/ChestImageViewer.java,v 1.8 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ChestImageViewer.class);

	protected JFrame frame;
	protected JPanel multiPanel;
	protected int frameWidth;
	protected int frameHeight;
	
	/**
	 * @param	list
	 * @return			array of two String values, row then column orientation, else array of two nulls if cannot be obtained
	 */
	private static String[] getPatientOrientation(AttributeList list) {
		String[] vPatientOrientation = null;
		Attribute aPatientOrientation = list.get(TagFromName.PatientOrientation);
		if (aPatientOrientation != null && aPatientOrientation.getVM() == 2) {
			try {
				vPatientOrientation = aPatientOrientation.getStringValues();
				if (vPatientOrientation != null && vPatientOrientation.length != 2) {
					vPatientOrientation=null;
				}
			}
			catch (DicomException e) {
				vPatientOrientation=null;
			}
		}
		if (vPatientOrientation == null) {
			vPatientOrientation = new String[2];
		}
		return vPatientOrientation;
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getView(AttributeList list) {
		String view = null;
		CodedSequenceItem csiViewCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ViewCodeSequence);
		if (csiViewCodeSequence != null) {
			//view = decipherSNOMEDCodeSequence(csiViewCodeSequence,standardViewCodes);
			view = MammoDemographicAndTechniqueAnnotations.getViewAbbreviationFromViewCodeSequenceAttributes(csiViewCodeSequence.getAttributeList());
		}
//System.err.println("getView(): view="+view);
		return view;
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getImageLateralityViewModifierAndViewModifier(AttributeList list) {
		return MammoDemographicAndTechniqueAnnotations.getAbbreviationFromImageLateralityViewModifierAndViewModifierCodeSequenceAttributes(list);
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getLaterality(AttributeList list) {
		return Attribute.getSingleStringValueOrNull(list,TagFromName.ImageLaterality);
	}
	
	/**
	 * @param	list
	 * @return			a single String value, null if cannot be obtained
	 */
	private static String getDate(AttributeList list) {
		return Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
	}
	
	class OurSingleImagePanel extends /*SingleImagePanelWithRegionDrawing */ SingleImagePanelWithLineDrawing  {
		public OurSingleImagePanel(SourceImage sImg,EventContext typeOfPanelEventContext) {
			super(sImg,typeOfPanelEventContext);
		}
	}
	
	protected SingleImagePanel makeNewImagePanel(SourceImage sImg,EventContext typeOfPanelEventContext) {
		return new OurSingleImagePanel(sImg,typeOfPanelEventContext);
	}
	
	/**
	 * @param		filenames
	 * @throws	Exception		if internal error
	 */
	public void loadMultiPanelFromSpecifiedFiles(String filenames[]) throws Exception {
	
		int nFiles = filenames.length;
		
		SingleImagePanel imagePanels[] = new SingleImagePanel[nFiles];
		
		String orientations[][] = new String[nFiles][];
		String views[] = new String[nFiles];
		String lateralityViewAndModifiers[] = new String[nFiles];
		String lateralities[] = new String[nFiles];
		int widths[] = new int[nFiles];
		int heights[] = new int[nFiles];
		PixelSpacing spacing[] = new PixelSpacing[nFiles];
		
		String rowOrientations[] = new String[nFiles];
		String columnOrientations[] = new String[nFiles];
		
		HashMap eventContexts = new HashMap();
		
		double maximumHorizontalExtentInMm = 0;
		double maximumVerticalExtentInMm = 0;
		
		StructuredReport sr[] = new StructuredReport[nFiles];

		int nImages = 0;
		int nCAD = 0;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (int f=0; f<nFiles; ++f) {
			try {
				String filename = filenames[f];
				DicomInputStream distream = null;
				InputStream in = classLoader.getResourceAsStream(filename);
				if (in != null) {
					distream = new DicomInputStream(in);
				}
				else {
					distream = new DicomInputStream(new File(filename));
				}
				AttributeList list = new AttributeList();
				list.read(distream);
				if (list.isImage()) {
					int i = nImages++;
					slf4jlogger.info("IMAGE [{}] is file {} ({})",i,f,filenames[f]);

					orientations[i] = getPatientOrientation(list);
//System.err.println("IMAGE ["+i+"] orientation="+(orientations[i] == null && orientations[i].length == 2 ? "" : (orientations[i][0] + " " + orientations[i][1])));
					views[i] = getView(list);
//System.err.println("IMAGE ["+i+"] view="+views[i]);
					lateralityViewAndModifiers[i] = getImageLateralityViewModifierAndViewModifier(list);
//System.err.println("IMAGE ["+i+"] lateralityViewAndModifiers="+lateralityViewAndModifiers[i]);
//System.err.println("File "+filenames[f]+": "+lateralityViewAndModifiers[i]);
					lateralities[i] = getLaterality(list);
//System.err.println("IMAGE ["+i+"] laterality="+lateralities[i]);
					spacing[i] = new PixelSpacing(list);
//System.err.println("IMAGE ["+i+"] spacing="+spacing[i]);
				
					SourceImage sImg = new SourceImage(list);
					BufferedImage img = sImg.getBufferedImage();
				
					widths[i] = sImg.getWidth();
					heights[i] = sImg.getHeight();
				
					boolean shareVOIEventsInStudy = false;		// does not seem to work anyway, since adding VOITransform to panel constructor :(
				
					EventContext eventContext = new EventContext(Integer.toString(i));
				
					SingleImagePanel imagePanel = makeNewImagePanel(sImg,eventContext);
					imagePanel.setDemographicAndTechniqueAnnotations(new DemographicAndTechniqueAnnotations(list),"SansSerif",Font.PLAIN,10,Color.pink);
					imagePanel.setOrientationAnnotations(
						new OrientationAnnotations(rowOrientations[i],columnOrientations[i]),
						"SansSerif",Font.PLAIN,20,Color.pink);
					imagePanel.setPixelSpacingInSourceImage(spacing[i].getSpacing(),spacing[i].getDescription());
					if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.VOILUTFunction).equals("SIGMOID")) {
						imagePanel.setVOIFunctionToLogistic();
					}
					imagePanels[i] = imagePanel;
				}
				else {
					throw new DicomException("Unsupported SOP Class in file "+filenames[f]);
				}
			}
			catch (Exception e) {	// FileNotFoundException,IOException,DicomException
				slf4jlogger.error("",e);
			}
		}

		//int imagesPerRow = nImages;			// i.e., 1 -> 1, 2 -> 1, 4 -> 4, 5 -> 4, 8 -> 4
		//int imagesPerCol = 1;

		int imagesPerRow = nImages >= 8 ? 8 : nImages;			// i.e., 1 -> 1, 2 -> 1, 4 -> 4, 5 -> 4, 8 -> 4
		int imagesPerCol = (nImages - 1) / imagesPerRow + 1;	// i.e., 1 -> 1, 2 -> 2, 4 -> 1, 5 -> 2, 8 -> 2

		int singleWidth  = frameWidth /imagesPerRow;
		int singleHeight = frameHeight/imagesPerCol;

		if (nImages == 1 && singleWidth > singleHeight) {
			singleWidth = singleWidth/2;			// use only half the screen for a single view and a landscape monitor
		}

		for (int i=0; i<nImages; ++i) {
			DisplayedAreaSelection displayedAreaSelection = null;
			displayedAreaSelection = new DisplayedAreaSelection(widths[i],heights[i],0,0,widths[i],heights[i],
					true,	// in case spacing was not supplied
					0,0,0,0,0,false/*crop*/);
			imagePanels[i].setDisplayedAreaSelection(displayedAreaSelection);
			imagePanels[i].setPreTransformImageRelativeCoordinates(null);
		}
		
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
		multiPanel.setBackground(Color.black);
		
		for (int x=0; x<imagesPerCol; ++x) {
			for (int y=0; y<imagesPerRow; ++y) {
				int i = x*imagesPerRow+y;
				if (i < nImages) {
					imagePanels[i].setPreferredSize(new Dimension(singleWidth,singleHeight));
					multiPanel.add(imagePanels[i]);
				}
			}
		}
		frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}
	
	public static DisplayDeviceArea[] getPresentationAndImageDeviceAreas() {
		DisplayDeviceArea[] displayDeviceAreas = null;
		GraphicsDevice[] gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		if (gs.length == 1) {
			DisplayMode dm = gs[0].getDisplayMode();
			int width = dm.getWidth();
			int height = dm.getHeight();
			float presentationAspectRatio=5f/4;	// usual screen for presentations
			float imageAspectRatio=3f/4*2;		// pair of portrait monitors
			
			float presentationHorizontalProportion=0.33f;
			
			int presentationWidth=(int)(width*presentationHorizontalProportion);
			
			int presentationHeight=(int)(presentationWidth/presentationAspectRatio);
			if (presentationHeight > height) {
				presentationHeight = height;
			}
			int presentationX = 0;
			int presentationY = height - presentationHeight;
			
			int imageWidth = width - presentationWidth;
			int imageHeight=(int)(imageWidth/imageAspectRatio);
			if (imageHeight > height) {
				imageHeight = height;
			}
			int imageX = presentationWidth;
			int imageY = height - imageHeight;
			
			displayDeviceAreas = new DisplayDeviceArea[2];
			displayDeviceAreas[0] = new DisplayDeviceArea(gs[0],presentationX,presentationY,presentationWidth,presentationHeight);
			displayDeviceAreas[1] = new DisplayDeviceArea(gs[0],imageX,imageY,imageWidth,imageHeight);
		}
		else if (gs.length == 2) {
			DisplayMode dm1 = gs[0].getDisplayMode();
			DisplayMode dm2 = gs[1].getDisplayMode();
			int width1 = dm1.getWidth();
			int width2 = dm2.getWidth();
			int height1 = dm1.getHeight();
			int height2 = dm2.getHeight();
			GraphicsDevice presentationDevice;
			GraphicsDevice imageDevice;
			if (width1*height1 > width2*height2) {
				presentationDevice=gs[1];
				imageDevice=gs[0];
			}
			else {
				presentationDevice=gs[0];
				imageDevice=gs[1];
			}
			displayDeviceAreas = new DisplayDeviceArea[2];
			displayDeviceAreas[0] = new DisplayDeviceArea(presentationDevice);
			displayDeviceAreas[1] = new DisplayDeviceArea(imageDevice);
		}
		return displayDeviceAreas;
	}

	/**
	 * @param		filenames
	 * @throws	Exception		if internal error
	 */
	public ChestImageViewer(String filenames[]) throws Exception {
		DisplayDeviceArea[] displayDeviceAreas = getPresentationAndImageDeviceAreas();
		if (displayDeviceAreas == null) {
			System.err.println("Cannot determine device display areas");
		}
		else {
			System.err.println("Found "+displayDeviceAreas.length+" device display areas");
			for (int i=0; i<displayDeviceAreas.length; ++i) {
				System.err.println("["+i+"] = "+displayDeviceAreas[i]);
				displayDeviceAreas[i].getFrame().setBackground(Color.black);
				displayDeviceAreas[i].getFrame().setVisible(true);
			}
			
			{
				// Need to actually add something to the unused left display frame, else background will not be set to black on Windows
				JPanel backgroundPanel = new JPanel();
				backgroundPanel.setBackground(Color.black);
				displayDeviceAreas[0].getFrame().getContentPane().add(backgroundPanel);
				displayDeviceAreas[0].getFrame().validate();
			}
			
			frame = displayDeviceAreas[1].getFrame();
					
			Container content = frame.getContentPane();
			content.setLayout(new GridLayout(1,1));
			multiPanel = new JPanel();
			//multiPanel.setBackground(Color.black);
			frameWidth  = (int)frame.getWidth();
			frameHeight = (int)frame.getHeight();
			Dimension d = new Dimension(frameWidth,frameHeight);
			//multiPanel.setSize(d);
			multiPanel.setPreferredSize(d);
			multiPanel.setBackground(Color.black);
			content.add(multiPanel);
			//frame.pack();
			content.validate();

			loadMultiPanelFromSpecifiedFiles(filenames);
		}
	}
	
	/**
	 */
	public void clear() {
		SingleImagePanel.deconstructAllSingleImagePanelsInContainer(multiPanel);
		multiPanel.removeAll();
		frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}

	/**
	 * <p>The method to invoke the application.</p>
	 *
	 * @param	arg		a list of DICOM files which may contain chest x-ray images
	 */
	public static void main(String arg[]) {
		try {
			new ChestImageViewer(arg);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


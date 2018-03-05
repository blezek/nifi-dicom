/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.SOPClass;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class implements a panel of icons of DICOM images inside a parent JScrollPane.</p>
 * 
 * <p>Maintains icons in a pre-defined sorted order based on DICOM attributes as they are added and removed.</p>
 * 
 * @author	dclunie
 */
public class IconListBrowser {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/IconListBrowser.java,v 1.9 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(IconListBrowser.class);

	public static final int DEFAULT_ICON_SIZE = 128;
	
	protected static final String ICON_FILE_PREFIX = "icon";

	//protected static final String ICON_FORMAT = "gif";
	//protected static final int    ICON_QUALITY = -1;			/* -1 is flag that quality is not used, e.g., for GIF, otherwise use 1 to 100 (best) */
	//protected static final String ICON_FILE_SUFFIX = ".gif";
	
	protected static final String ICON_FORMAT = "jpeg";
	protected static final int    ICON_QUALITY = 100;			/* -1 is flag that quality is not used, e.g., for GIF, otherwise use 1 to 100 (best) */
	protected static final String ICON_FILE_SUFFIX = ".jpg";
	
	protected int iconSize;

	protected JScrollPane parentScrollPane;

	protected JList list;

	protected DefaultListModel model;

	//private Border selectedBorder   = BorderFactory.createRaisedBevelBorder();
	//private Border unselectedBorder = BorderFactory.createLoweredBevelBorder();
	
	private Border selectedBorder   = new LineBorder(Color.green,2);
	private Border unselectedBorder = new LineBorder(Color.black,2);

	/**
	 * <p>Set the parent scoll pane.</p>
	 *
	 * <p>Used from within constructors.</p>
	 *
	 * @param		parentScrollPane
	 */
	protected void setParentScrollPane(JScrollPane parentScrollPane) {
		this.parentScrollPane = parentScrollPane;
		parentScrollPane.setViewportView(list);
	}
	
	/**
	 * <p>Add a set of DICOM image files.</p>
	 *
	 * @param	dicomFileNames			a list of DICOM files
	 * @throws	DicomException			thrown if the icons cannot be extracted
	 * @throws	FileNotFoundException	thrown if a file cannot be found
	 * @throws	IOException				thrown if a file cannot be read
	 */
	public void addDicomFiles(String[] dicomFileNames) throws DicomException, FileNotFoundException, IOException {
		for (int i=0; i<dicomFileNames.length; ++i) {
			String dicomFileName = dicomFileNames[i];
			add(dicomFileName);
		}
	}
	
	protected class OurCellRenderer extends JLabel implements javax.swing.ListCellRenderer {
	
		public Component getListCellRendererComponent(JList list,Object value,int index,boolean isSelected,boolean cellHasFocus)  {
			if (value instanceof JLabel) {
				JLabel label = (JLabel)value;
				setText(label.getText());
				setIcon(label.getIcon());
				setToolTipText(label.getToolTipText());
				if (isSelected) {
					//setBackground(list.getSelectionBackground());
					//setForeground(list.getSelectionForeground());
					setBorder(selectedBorder);
				}
				else {
					//setBackground(list.getBackground());
					//setForeground(list.getForeground());
					setBorder(unselectedBorder);
				}
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}


	/**
	 * <p>Build and display an (initally empty) graphical user interface view of a set of DICOM images.</p>
	 *
	 * @param	iconSize			the width and height in pixels of the icons to be created
	 * @throws	DicomException		thrown if the icons cannot be extracted
	 */
	public IconListBrowser(int iconSize) throws DicomException {
		this.iconSize = iconSize;
		
		model = new DefaultListModel();
		list = new JList(model);
		list.setCellRenderer(new OurCellRenderer());		// need our own, otherwise just renders toString() method of JLabel
		list.setBackground(Color.BLACK);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);		// is the default, but just to be sure; note that on Mac use CMD-click not CTRL-click for discontiguous multi-selection

		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int clickedIndex = list.locationToIndex(e.getPoint());
					slf4jlogger.info("Double clicked on Item {}",clickedIndex);
					int[] selectedIndices = list.getSelectedIndices();
					slf4jlogger.info("At which time selected Items were {}",Arrays.toString(selectedIndices));
				}
			}
		};
		list.addMouseListener(mouseListener);
		
		parentScrollPane = null;
	}

	/**
	 * <p>Build and display an (initally empty) graphical user interface view of a set of DICOM images.</p>
	 *
	 * @param	parentScrollPane	the scrolling pane in which the icons will be rendered
	 * @param	iconSize			the width and height in pixels of the icons to be created
	 * @throws	DicomException		thrown if the icons cannot be extracted
	 */
	public IconListBrowser(JScrollPane parentScrollPane,int iconSize) throws DicomException {
		this(iconSize);
		setParentScrollPane(parentScrollPane);
	}

	/**
	 * <p>Build and display an (initally empty) graphical user interface view of a set of DICOM images.</p>
	 *
	 * <p>Uses default icon size.</p>
	 *
	 * @param		parentScrollPane	the scrolling pane in which the icons will be rendered
	 * @throws	DicomException		thrown if the icons cannot be extracted
	 */
	public IconListBrowser(JScrollPane parentScrollPane) throws DicomException {
		this(parentScrollPane,DEFAULT_ICON_SIZE);
	}

	/**
	 * <p>Build and display a graphical user interface view of a set of DICOM image files.</p>
	 *
	 * <p>Uses default icon size.</p>
	 *
	 * @param	parentScrollPane		the scrolling pane in which the icons will be rendered
	 * @param	dicomFileNames			a list of DICOM files
	 * @throws	DicomException			thrown if the icons cannot be extracted
	 * @throws	FileNotFoundException	thrown if a file cannot be found
	 * @throws	IOException				thrown if a file cannot be read
	 */
	public IconListBrowser(JScrollPane parentScrollPane,String[] dicomFileNames) throws DicomException, FileNotFoundException, IOException {
		this(parentScrollPane);
		addDicomFiles(dicomFileNames);
	}

	/**
	 * <p>Build and display a graphical user interface view of a set of DICOM image files.</p>
	 *
	 * @param	content					a container to which will be added will be added a scrolling pane containing the icon browser
	 * @param	dicomFileNames			a list of DICOM files
	 * @throws	DicomException			thrown if the icons cannot be extracted
	 * @throws	FileNotFoundException	thrown if a file cannot be found
	 * @throws	IOException				thrown if a file cannot be read
	 */
	public IconListBrowser(Container content,String[] dicomFileNames) throws DicomException, FileNotFoundException, IOException {
		this(DEFAULT_ICON_SIZE);
		JScrollPane scrollPane = new JScrollPane();
		content.add(scrollPane);
		setParentScrollPane(scrollPane);
		addDicomFiles(dicomFileNames);
	}

	/**
	 * <p>Build and display a graphical user interface view of a set of DICOM image files.</p>
	 *
	 * @param	frame					a frame to whose content pane will be added a scrolling pane containing the icon browser
	 * @param	dicomFileNames			a list of DICOM files
	 * @throws	DicomException			thrown if the icons cannot be extracted
	 * @throws	FileNotFoundException	thrown if a file cannot be found
	 * @throws	IOException				thrown if a file cannot be read
	 */
	public IconListBrowser(JFrame frame,String[] dicomFileNames) throws DicomException, FileNotFoundException, IOException {
		this(frame.getContentPane(),dicomFileNames);
	}

	/**
	 * <p>Add an annotated icon of a DICOM image.</p>
	 *
	 * @param	dicomFileName			the name of the file containing the DICOM image
	 * @throws	DicomException			thrown if the icons cannot be extracted
	 * @throws	FileNotFoundException	thrown if a file cannot be found
	 * @throws	IOException				thrown if a file cannot be read
	 */
	public void add(String dicomFileName) throws DicomException, FileNotFoundException, IOException {
		File iconFile = File.createTempFile(ICON_FILE_PREFIX,ICON_FILE_SUFFIX);
		iconFile.deleteOnExit();
		String iconFileName = iconFile.getAbsolutePath();
		ConsumerFormatImageMaker.convertFileToEightBitImage(
			dicomFileName,iconFileName,
			ICON_FORMAT,
			0/*windowCenter*/,0/*windowWidth*/,
			DEFAULT_ICON_SIZE,0/*imageHeight*/,
			ICON_QUALITY,
			ConsumerFormatImageMaker.ICON_ANNOTATIONS);
			
		ImageIcon icon = new ImageIcon(iconFileName);
		//JLabel label = new JLabel("bla bla ",icon,SwingConstants.CENTER);
		JLabel label = new JLabel(icon);
		label.setToolTipText(dicomFileName);
		model.addElement(label);
	}
	
	/**
	 * <p>Method for testing.</p>
	 *
	 * @param	arg	a list of DICOM image files from which to extract one icon each and display
	 */
	public static void main(String arg[]) {
		try {
			ApplicationFrame af = new ApplicationFrame("IconListBrowser");
			new IconListBrowser(af,arg);
			af.pack();
			af.setVisible(true);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}

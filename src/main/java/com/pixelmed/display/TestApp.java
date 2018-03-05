/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.*; 
import com.pixelmed.dicom.*;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

/**
 * @author	dclunie
 */
class TestApp extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/TestApp.java,v 1.26 2017/01/24 10:50:41 dclunie Exp $";

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) { 
		TestApp af = new TestApp();

		SourceImage sImg = null;

		int imagesPerRow=0;
		int imagesPerCol=0;

		int imgMin=65536;
		int imgMax=0;

		boolean signed=false;
		boolean inverted=false;
		boolean hasPad=false;
		int padValue=0;

		if (arg.length == 6) {
			// do it with raw file
			int w=0;
			int h=0;
			int d=0;
			try {
				w=Integer.valueOf(arg[1]).intValue();
				h=Integer.valueOf(arg[2]).intValue();
				d=Integer.valueOf(arg[3]).intValue();
				imagesPerRow=Integer.valueOf(arg[4]).intValue();
				imagesPerCol=Integer.valueOf(arg[5]).intValue();
			} catch (Exception e) {
				System.err.println(e);
				System.exit(0);
			}

			try {
				FileInputStream i = new FileInputStream(arg[0]);
				sImg=new SourceImage(i,w,h,d);
			} catch (Exception e) {
				System.err.println(e);
				System.exit(0);
			}
		}
		else {
			// do it with DICOM file

			if (arg.length > 2) {
				try {
					imagesPerRow=Integer.valueOf(arg[1]).intValue();
					imagesPerCol=Integer.valueOf(arg[2]).intValue();
				} catch (Exception e) {
					e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
					System.exit(0);
				}
			}
			else {
				imagesPerRow=1;
				imagesPerCol=1;
			}

			try {
				DicomInputStream i = new DicomInputStream(new FileInputStream(arg[0]));
				sImg=new SourceImage(i);
			} catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
				System.exit(0);
			}
		}

		try {
			//com.apple.cocoa.application.NSMenu.setMenuBarVisible(false);							// Won't compile on other platforms
			//Class classToUse = ClassLoader.getSystemClassLoader().loadClass("com.apple.cocoa.application.NSMenu");	// Needs "/System/Library/Java" in classpath
			Class classToUse = new java.net.URLClassLoader(new java.net.URL[]{new File("/System/Library/Java").toURL()}).loadClass("com.apple.cocoa.application.NSMenu");
			Class[] parameterTypes = { Boolean.TYPE };
			java.lang.reflect.Method methodToUse = classToUse.getDeclaredMethod("setMenuBarVisible",parameterTypes);
			Object[] args = { Boolean.FALSE };
			methodToUse.invoke(null/*since static*/,args);
		}
		catch (Exception e) {	// ClassNotFoundException,NoSuchMethodException,IllegalAccessException
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		java.awt.Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		int frameWidth  = (int)d.getWidth();
		int frameHeight = (int)d.getHeight();
		System.err.println("frameWidth="+frameWidth);	// no need to use SLF4J since command line utility/test
		System.err.println("frameHeight="+frameHeight);
		af.setUndecorated(true);
		af.setLocation(0,0);
		af.setSize(frameWidth,frameHeight);

		JPanel multiPanel = new JPanel();
		multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
		multiPanel.setBackground(Color.black);

		SingleImagePanel imagePanel[] = new SingleImagePanel[imagesPerRow*imagesPerCol];

		int singleWidth  = frameWidth /imagesPerRow;
		int singleHeight = frameHeight/imagesPerCol;
		System.err.println("singleWidth="+singleWidth);
		System.err.println("singleHeight="+singleHeight);

		for (int x=0; x<imagesPerCol; ++x) {
			for (int y=0; y<imagesPerRow; ++y) {
				SingleImagePanel ip = new SingleImagePanel(sImg);
				//ip.setPreferredSize(new Dimension(img.getWidth(),img.getHeight()));
				//ip.setPreferredSize(new Dimension(sImg.getWidth(),sImg.getHeight()));
				ip.setPreferredSize(new Dimension(singleWidth,singleHeight));
				multiPanel.add(ip);
				imagePanel[x*imagesPerRow+y]=ip;
			}
		}

		//multiPanel.setSize(img.getWidth()*imagesPerRow,img.getHeight()*imagesPerRow);

		//JScrollPane scrollPane = new JScrollPane(multiPanel);
		
		Container content = af.getContentPane();
		content.setLayout(new GridLayout(1,1));
		//content.add(scrollPane);
		content.add(multiPanel);

		af.pack();
		af.setVisible(true);
       } 

}







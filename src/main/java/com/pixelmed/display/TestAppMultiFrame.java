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
class TestAppMultiFrame extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/TestAppMultiFrame.java,v 1.20 2017/01/24 10:50:41 dclunie Exp $";

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) { 
		TestAppMultiFrame af = new TestAppMultiFrame();

		int imagesPerRow=0;

		SourceImage sImg = null;

		if (arg.length == 6) {
			// do it with raw file
			int w=0;
			int h=0;
			int d=0;
			int nf=0;
			try {
				w=Integer.valueOf(arg[1]).intValue();
				h=Integer.valueOf(arg[2]).intValue();
				d=Integer.valueOf(arg[3]).intValue();
				nf=Integer.valueOf(arg[4]).intValue();
				imagesPerRow=Integer.valueOf(arg[5]).intValue();
			} catch (Exception e) {
				System.err.println(e);
				System.exit(0);
			}

			try {
				FileInputStream i = new FileInputStream(arg[0]);
				sImg=new SourceImage(i,w,h,d,nf);
			} catch (Exception e) {
				System.err.println(e);
				System.exit(0);
			}
		}
		else {
			// do it with DICOM file

			if (arg.length > 1) {
				try {
					imagesPerRow=Integer.valueOf(arg[1]).intValue();
				} catch (Exception e) {
					e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
					System.exit(0);
				}
			}
			else {
				imagesPerRow=1;
			}

			try {
				DicomInputStream i = new DicomInputStream(new FileInputStream(arg[0]));
				sImg=new SourceImage(i);
			} catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
				System.exit(0);
			}
		}

		int nframes=sImg.getNumberOfFrames();
		int imagesPerCol=((nframes-1)/imagesPerRow)+1;

		//System.err.println("imagesPerRow="+imagesPerRow);
		//System.err.println("imagesPerCol="+imagesPerCol);

		JPanel multiPanel = new JPanel();
		multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
		multiPanel.setBackground(Color.black);
		SingleImagePanel imagePanel[] = new SingleImagePanel[imagesPerRow*imagesPerCol];

		int f=0;
		int x=0;
		int y=0;
		while (f < nframes) {
			SingleImagePanel ip = new SingleImagePanel(sImg);
			ip.setPreferredSize(new Dimension(sImg.getWidth(),sImg.getHeight()));
			multiPanel.add(ip);
			int p=x*imagesPerRow+y;
			//System.err.println("x="+x+", y="+y+", f="+f+", p="+p);
			imagePanel[p]=ip;
			++y;
			if (y >= imagesPerRow) {
				y=0;
				++x;
			}
			++f;
		}

		//multiPanel.setSize(img.getWidth()*imagesPerRow,img.getHeight()*imagesPerRow);

		JScrollPane scrollPane = new JScrollPane(multiPanel);

		Container content = af.getContentPane();
		content.setLayout(new GridLayout(1,1));
		content.add(scrollPane);

		af.pack();

		int frameHeight=scrollPane.getHeight()+24;
		if (frameHeight>1024) frameHeight=1024;
		int frameWidth=scrollPane.getWidth()+24;
		if (frameWidth>1280) frameWidth=1280;
		af.setSize(frameWidth,frameHeight);
		af.setVisible(true);
       } 

}








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
class TestAppMultiFile extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/TestAppMultiFile.java,v 1.5 2017/01/24 10:50:41 dclunie Exp $";

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) { 
		TestAppMultiFrame af = new TestAppMultiFrame();

		int imagesPerRow=2;
		int imagesPerCol=((arg.length-1)/imagesPerRow)+1;

//System.err.println("imagesPerRow="+imagesPerRow);	// no need to use SLF4J since command line utility/test
//System.err.println("imagesPerCol="+imagesPerCol);

		JPanel multiPanel = new JPanel();
		multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
		multiPanel.setBackground(Color.black);
		SingleImagePanel imagePanel[] = new SingleImagePanel[imagesPerRow*imagesPerCol];

		int x=0;
		int y=0;
		for (int i=0; i<arg.length; ++i) {
			try {
				AttributeList list = new AttributeList();
				list.read(arg[i]);
				SourceImage sImg = new SourceImage(list);
				SingleImagePanel ip = new SingleImagePanel(sImg);
			
				ip.setDemographicAndTechniqueAnnotations(new DemographicAndTechniqueAnnotations(list,null/*imageGeometry*/),"SansSerif",Font.PLAIN,10,Color.pink);
				ip.setOrientationAnnotations(new OrientationAnnotations(list,null/*imageGeometry*/),"SansSerif",Font.PLAIN,20,Color.pink);
			
				ip.setPreferredSize(new Dimension(sImg.getWidth()/2,sImg.getHeight()/2));
				multiPanel.add(ip);
				int p=x*imagesPerRow+y;
				imagePanel[p]=ip;
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
			++y;
			if (y >= imagesPerRow) {
				y=0;
				++x;
			}
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








/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.awt.Dimension;
import java.awt.GridLayout;

import java.io.Writer;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;

/**
 * <p>A class to write log and status messages to a <code>PrintStream</code> such as <code>System.err</code>.</p>
 *
 * @author      dclunie
 */
public class ScrollingTextAreaWriter extends Writer {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/ScrollingTextAreaWriter.java,v 1.5 2017/01/24 10:50:52 dclunie Exp $";

	protected JTextArea loggerTextArea;
		
	public ScrollingTextAreaWriter(JFrame content,int width,int height) {
		if (content != null) {
			JPanel mainPanel = new JPanel();
			content.add(mainPanel);
			loggerTextArea = new javax.swing.JTextArea();
			JScrollPane loggerScrollPane = new JScrollPane();
			loggerScrollPane.setViewportView(loggerTextArea);
			loggerScrollPane.setPreferredSize(new Dimension(width,height));		// this is the one whose size to set, else doesn't show scroll bars
			mainPanel.setLayout(new GridLayout(1,1));							// else doesn't resize
			mainPanel.add(loggerScrollPane);
			content.pack();
			content.setVisible(true);
		}
	}
		
	public void close() {}
		
	public void flush() {}
		
	public void write(char[] c,int off,int len) {
		loggerTextArea.append(new String(c,off,len));
		// http://forum.java.sun.com/thread.jsp?thread=409923&forum=57&message=1802689
		loggerTextArea.setCaretPosition(loggerTextArea.getDocument().getLength()); 
	}
}

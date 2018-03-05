/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.ThreadUtilities;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.text.SimpleDateFormat;

import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.border.Border;

import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to write log and status messages to a scrolling text area in a dialog box.</p>
 *
 * @author      dclunie
 */
public class DialogMessageLogger implements MessageLogger {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DialogMessageLogger.java,v 1.13 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DialogMessageLogger.class);		// this logger is nothing to do with the DialogMessageLogger logger per se

	protected JDialog outputDialog;
	protected JScrollPane outputScrollPane;
	protected JTextArea outputTextArea;
	
	// Mimic slf4jlogger behavior for time stamp
	
	protected static long startTimeForLogging = System.currentTimeMillis();
	
	protected boolean showDateTime = false;
	
	/**
	 * <p>Whether or not to show a timestamp.</p>
	 *
	 * @param	showDateTime	if true, show a timestamp
	 */
	public void showDateTime(boolean showDateTime) {
		this.showDateTime = showDateTime;
	}
	
	protected SimpleDateFormat dateFormatter = null;	// if null will use relative time in ms (since startTimeForLogging)

	/**
	 * <p>Set the date format to use</p>
	 *
	 * <p>If not set will use relative time in ms from start of application</p>
	 *
	 * @param	pattern	a java.text.SimpleDateFormat pattern
	 */
	public void setDateTimeFormat(String pattern) {
		try {
			if (pattern != null && pattern.length() > 0) {
				dateFormatter = new SimpleDateFormat(pattern);
			}
		}
		catch (IllegalArgumentException e) {
			slf4jlogger.error("DialogMessageLogger(): bad date format ");
		}
	}
	
	protected class ClearActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			//outputTextArea.replaceRange(null,0,0);
			outputTextArea.setText("");
			outputScrollPane.repaint();
		}
	}

	/**
	 * <p>Construct a logger and make it immediately visible.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 */
	public DialogMessageLogger(String titleMessage,int width,int height,boolean exitApplicationOnClose) {
		this(titleMessage,width,height,exitApplicationOnClose,true);
	}

	/**
	 * <p>Construct a slf4jlogger.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 * @param	visible					if true, will be made visible after construction
	 */
	public DialogMessageLogger(String titleMessage,int width,int height,boolean exitApplicationOnClose,boolean visible) {
		this(titleMessage,width,height,exitApplicationOnClose,visible,false/*showDateTime*/,null/*dateTimeFormat*/);
	}
	
	/**
	 * <p>Construct a slf4jlogger.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 * @param	visible					if true, will be made visible after construction
	 * @param	showDateTime			if true, show a timestamp
	 * @param	dateTimeFormat			a java.text.SimpleDateFormat pattern
	 */
	public DialogMessageLogger(String titleMessage,int width,int height,boolean exitApplicationOnClose,boolean visible,boolean showDateTime,String dateTimeFormat) {
		if (java.awt.EventQueue.isDispatchThread()) {
			slf4jlogger.trace("DialogMessageLogger(): constructing on EDT - showDateTime = {}",showDateTime);
			createGUI(titleMessage,width,height,exitApplicationOnClose,visible,showDateTime,dateTimeFormat);
		}
		else {
			slf4jlogger.trace("DialogMessageLogger(): constructing on non-EDT - showDateTime = {}",showDateTime);
			java.awt.EventQueue.invokeLater(new CreateGUIRunnable(titleMessage,width,height,exitApplicationOnClose,visible,showDateTime,dateTimeFormat));
		}
	}
	
	protected class CreateGUIRunnable implements Runnable {
		String titleMessage;
		int width;
		int height;
		boolean exitApplicationOnClose;
		boolean visible;
		boolean showDateTime;
		String dateTimeFormat;
		
		CreateGUIRunnable(String titleMessage,int width,int height,boolean exitApplicationOnClose,boolean visible,boolean showDateTime,String dateTimeFormat) {
			this.titleMessage = titleMessage;
			this.width = width;
			this.height = height;
			this.exitApplicationOnClose = exitApplicationOnClose;
			this.visible = visible;
			this.showDateTime = showDateTime;
			this.dateTimeFormat = dateTimeFormat;
		}
		
		public void run() {
			createGUI(titleMessage,width,height,exitApplicationOnClose,visible,showDateTime,dateTimeFormat);
		}
	}
	
	/**
	 * <p>Construct the GUI for a slf4jlogger.</p>
	 *
	 * @param	titleMessage			for the title bar of the dialog box
	 * @param	width					initial width of the resizeable dialog box
	 * @param	height					initial height of the resizeable dialog box
	 * @param	exitApplicationOnClose	if true, when the dialog box is closed (X-d out), will exit the application with success status
	 * @param	visible					if true, will be made visible after construction
	 * @param	showDateTime			if true, show a timestamp
	 * @param	dateTimeFormat			a java.text.SimpleDateFormat pattern
	 */
	protected void createGUI(String titleMessage,int width,int height,boolean exitApplicationOnClose,boolean visible,boolean showDateTime,String dateTimeFormat) {
		slf4jlogger.trace("createGUI() - showDateTime = {}",showDateTime);
		showDateTime(showDateTime);
		setDateTimeFormat(dateTimeFormat);

		Border panelBorder = BorderFactory.createEtchedBorder();
		
		outputTextArea = new JTextArea();
		{
			// if one does not do this, the default behavior is scroll to end when append() on event disptaching thread, but not from other threads
			Caret caret = outputTextArea.getCaret();
			if (caret != null && caret instanceof DefaultCaret) {
				((DefaultCaret)caret).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);	// i.e. jump to end after append() regardless of whether in same thread or not
			}
		}
		outputScrollPane = new JScrollPane(outputTextArea);
		outputScrollPane.setBorder(panelBorder);
		
		outputDialog = new JDialog();
		//outputDialog.setSize(width,height);
		outputDialog.setPreferredSize(new Dimension(width,height));
		outputDialog.setTitle(titleMessage);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBorder(panelBorder);

		JButton clearButton = new JButton("Clear");
		clearButton.setToolTipText("Clear log");
		buttonPanel.add(clearButton);
		clearButton.addActionListener(new ClearActionListener());

		Container mainPanel = outputDialog.getContentPane();
		{
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanel.setLayout(mainPanelLayout);
			{
				GridBagConstraints outputScrollPaneConstraints = new GridBagConstraints();
				outputScrollPaneConstraints.gridx = 0;
				outputScrollPaneConstraints.gridy = 0;
				outputScrollPaneConstraints.weightx = 1;
				outputScrollPaneConstraints.weighty = 1;
				outputScrollPaneConstraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(outputScrollPane,outputScrollPaneConstraints);
				mainPanel.add(outputScrollPane);
			}
			{
				GridBagConstraints buttonPanelConstraints = new GridBagConstraints();
				buttonPanelConstraints.gridx = 0;
				buttonPanelConstraints.gridy = 1;
				buttonPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(buttonPanel,buttonPanelConstraints);
				mainPanel.add(buttonPanel);
			}
		}
		
		outputDialog.pack();
		outputDialog.setVisible(visible);
		
		if (exitApplicationOnClose) {
			// Note that unlike for JFrame, EXIT_ON_CLOSE is NOT a valid value to pass to setDefaultCloseOperation(), so have to do this with WindowListener
			outputDialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
//System.err.println("DialogMessageLogger.WindowAdapter.windowClosing()");
					//dispose();
					System.exit(0);
				}
			});
		}
	}

	protected class SetVisibleRunnable implements Runnable {
		boolean visible;
		
		SetVisibleRunnable(boolean visible) {
			this.visible = visible;
		}
		
		public void run() {
			outputDialog.setVisible(visible);
		}
	}

	public void setVisible(boolean visible) {
		if (java.awt.EventQueue.isDispatchThread()) {
//System.err.println("DialogMessageLogger.setVisible(): on EDT");
			outputDialog.setVisible(visible);
		}
		else {
//System.err.println("DialogMessageLogger.setVisible(): on non-EDT");
			java.awt.EventQueue.invokeLater(new SetVisibleRunnable(visible));
		}
	}
	
	protected class SendRunnable implements Runnable {
		String message;
		
		SendRunnable(String message) {
			this.message = message;
		}
		
		public void run() {
			outputTextArea.append(message);
		}
	}
	
	protected void timestamp() {
		if (showDateTime) {
			String timestamp = (dateFormatter == null ? (System.currentTimeMillis()-startTimeForLogging) : dateFormatter.format(new Date())) + " ";
			if (java.awt.EventQueue.isDispatchThread()) {
				outputTextArea.append(timestamp);
			}
			else {
				java.awt.EventQueue.invokeLater(new SendRunnable(timestamp));
			}
		}
	}
	
	public void send(String message) {
		timestamp();
		if (java.awt.EventQueue.isDispatchThread()) {
//System.err.println("send(): on EDT");
			outputTextArea.append(message);
		}
		else {
//System.err.println("send(): on non-EDT");
			java.awt.EventQueue.invokeLater(new SendRunnable(message));
		}
	}
		
	public void sendLn(String message) {
		timestamp();
		if (java.awt.EventQueue.isDispatchThread()) {
//System.err.println("sendLn(): on EDT");
			outputTextArea.append(message+"\n");
		}
		else {
//System.err.println("sendLn(): on non-EDT");
			java.awt.EventQueue.invokeLater(new SendRunnable(message+"\n"));
		}
	}
}


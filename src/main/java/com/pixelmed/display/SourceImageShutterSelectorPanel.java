/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.display.event.ApplyShutterChangeEvent; 
import com.pixelmed.event.ApplicationEventDispatcher; 
import com.pixelmed.event.EventContext;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
class SourceImageShutterSelectorPanel extends JPanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageShutterSelectorPanel.java,v 1.5 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceImageShutterSelectorPanel.class);

	/***/
	private EventContext eventContext;
	/***/
	private ButtonGroup applyShutterButtons;
	/***/
	private JRadioButton offButton;
	/***/
	private JRadioButton onButton;
	
	private static final String onCommand = "ON";
	private static final String offCommand = "OFF";
	
	/***/
	private class ApplyShutterActionListener implements ActionListener {

		/**
		 */
		public ApplyShutterActionListener() {
		}
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
//System.err.println("ApplyShutterActionListener.ApplyShutterActionListener.actionPerformed()");
			sendEventCorrespondingToCurrentButtonState();
		}
	}
	
	public void sendEventCorrespondingToCurrentButtonState() {
		String choice = applyShutterButtons.getSelection().getActionCommand();
		boolean applyShutter = choice != null && choice.equals(onCommand);
		try {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
				new ApplyShutterChangeEvent(eventContext,applyShutter));
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * @param	eventContext
	 */
	public SourceImageShutterSelectorPanel(EventContext eventContext) {
		this.eventContext=eventContext;
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		JPanel applyShutterControlsPanel = new JPanel();
		add(applyShutterControlsPanel);

		applyShutterControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		applyShutterControlsPanel.add(new JLabel("Apply shutters:"));

		applyShutterButtons = new ButtonGroup();
		ApplyShutterActionListener listener = new ApplyShutterActionListener();

		offButton = new JRadioButton("off",false);
		offButton.setActionCommand(offCommand);
		offButton.setToolTipText("Do not apply shutters");
		offButton.addActionListener(listener);
		applyShutterButtons.add(offButton);
		applyShutterControlsPanel.add(offButton);

		onButton = new JRadioButton("on",true);
		onButton.setActionCommand(onCommand);
		onButton.setToolTipText("Apply shutters");
		onButton.addActionListener(listener);
		applyShutterButtons.add(onButton);
		applyShutterControlsPanel.add(onButton);
	}
}



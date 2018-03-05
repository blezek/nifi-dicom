/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.WindowingAccelerationValueChangeEvent; 
import com.pixelmed.event.ApplicationEventDispatcher; 
import com.pixelmed.event.EventContext;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
class SourceImageWindowingAccelerationSelectorPanel extends JPanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageWindowingAccelerationSelectorPanel.java,v 1.5 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceImageWindowingAccelerationSelectorPanel.class);

	/***/
	private EventContext eventContext;
	/***/
	private ButtonGroup windowingAccelerationButtons;
	/***/
	private JRadioButton increaseButton;
	/***/
	private JRadioButton decreaseButton;
	
	/***/
	private class WindowingAccelerationChangeActionListener implements ActionListener {

		/**
		 */
		public WindowingAccelerationChangeActionListener() {
		}
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
//System.err.println("WindowingAccelerationChangeActionListener.WindowingAccelerationChangeActionListener.actionPerformed()");
			sendEventCorrespondingToCurrentButtonState();
		}
	}
	
	public void sendEventCorrespondingToCurrentButtonState() {
		double value = Double.parseDouble(windowingAccelerationButtons.getSelection().getActionCommand());	// OK, so this is pretty hokey, passing the value as a "command", but it is just any String
		try {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
				new WindowingAccelerationValueChangeEvent(eventContext,value));
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * @param	eventContext
	 */
	public SourceImageWindowingAccelerationSelectorPanel(EventContext eventContext) {
		this.eventContext=eventContext;
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		JPanel windowingAccelerationControlsPanel = new JPanel();
		add(windowingAccelerationControlsPanel);

		windowingAccelerationControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		windowingAccelerationControlsPanel.add(new JLabel("Windowing acceleration:"));

		windowingAccelerationButtons = new ButtonGroup();
		WindowingAccelerationChangeActionListener listener = new WindowingAccelerationChangeActionListener();

		increaseButton = new JRadioButton("50",true);
		increaseButton.setActionCommand("50");
		increaseButton.setToolTipText("Accelerate effect of mouse movement when SHIFT key depressed during windowing by 50");
		increaseButton.addActionListener(listener);
		windowingAccelerationButtons.add(increaseButton);
		windowingAccelerationControlsPanel.add(increaseButton);

		decreaseButton = new JRadioButton("0.001",false);
		decreaseButton.setActionCommand("0.001");
		increaseButton.setToolTipText("Accelerate effect of mouse movement when SHIFT key depressed during windowing by 0.001");
		decreaseButton.addActionListener(listener);
		windowingAccelerationButtons.add(decreaseButton);
		windowingAccelerationControlsPanel.add(decreaseButton);
	}
}



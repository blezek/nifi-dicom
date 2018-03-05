/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.WindowLinearCalculationChangeEvent; 
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
class SourceImageWindowLinearCalculationSelectorPanel extends JPanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageWindowLinearCalculationSelectorPanel.java,v 1.5 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceImageWindowLinearCalculationSelectorPanel.class);

	/***/
	private EventContext eventContext;
	/***/
	private ButtonGroup windowLinearCalculationButtons;
	/***/
	private JRadioButton dicomButton;
	/***/
	private JRadioButton exactButton;
	
	/***/
	private class WindowLinearCalculationChangeActionListener implements ActionListener {

		/**
		 */
		public WindowLinearCalculationChangeActionListener() {
		}
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
//System.err.println("WindowLinearCalculationChangeActionListener.WindowLinearCalculationChangeActionListener.actionPerformed()");
			sendEventCorrespondingToCurrentButtonState();
		}
	}
	
	public void sendEventCorrespondingToCurrentButtonState() {
		String choice = windowLinearCalculationButtons.getSelection().getActionCommand();
		try {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
				new WindowLinearCalculationChangeEvent(eventContext,choice));
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * @param	eventContext
	 */
	public SourceImageWindowLinearCalculationSelectorPanel(EventContext eventContext) {
		this.eventContext=eventContext;
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		JPanel windowLinearCalculationControlsPanel = new JPanel();
		add(windowLinearCalculationControlsPanel);

		windowLinearCalculationControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		windowLinearCalculationControlsPanel.add(new JLabel("Window calculation:"));

		windowLinearCalculationButtons = new ButtonGroup();
		WindowLinearCalculationChangeActionListener listener = new WindowLinearCalculationChangeActionListener();

		dicomButton = new JRadioButton("DICOM",true);
		dicomButton.setActionCommand(WindowLinearCalculationChangeEvent.dicomCalculation);
		dicomButton.setToolTipText("Use DICOM offset calculation (-0.5 center, -1.0 width)");
		dicomButton.addActionListener(listener);
		windowLinearCalculationButtons.add(dicomButton);
		windowLinearCalculationControlsPanel.add(dicomButton);

		exactButton = new JRadioButton("exact",false);
		exactButton.setActionCommand(WindowLinearCalculationChangeEvent.exactCalculation);
		exactButton.setToolTipText("Use exact calculation without any offset");
		exactButton.addActionListener(listener);
		windowLinearCalculationButtons.add(exactButton);
		windowLinearCalculationControlsPanel.add(exactButton);
	}
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.GraphicDisplayChangeEvent; 
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
class SourceImageGraphicDisplaySelectorPanel extends JPanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceImageGraphicDisplaySelectorPanel.java,v 1.5 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceImageGraphicDisplaySelectorPanel.class);

	/***/
	private EventContext eventContext;
	/***/
	private ButtonGroup graphicDisplayButtons;
	/***/
	private JRadioButton offButton;
	/***/
	private JRadioButton onButton;
	
	private static final String onCommand = "ON";
	private static final String offCommand = "OFF";
	
	/***/
	private class GraphicDisplayActionListener implements ActionListener {

		/**
		 */
		public GraphicDisplayActionListener() {
		}
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
//System.err.println("GraphicDisplayActionListener.GraphicDisplayActionListener.actionPerformed()");
			sendEventCorrespondingToCurrentButtonState();
		}
	}
	
	public void sendEventCorrespondingToCurrentButtonState() {
		String choice = graphicDisplayButtons.getSelection().getActionCommand();
		boolean showOverlay = choice != null && choice.equals(onCommand);
		try {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
				new GraphicDisplayChangeEvent(eventContext,showOverlay));
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}
	
	/**
	 * @param	eventContext
	 */
	public SourceImageGraphicDisplaySelectorPanel(EventContext eventContext) {
		this.eventContext=eventContext;
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		JPanel graphicDisplayControlsPanel = new JPanel();
		add(graphicDisplayControlsPanel);

		graphicDisplayControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		graphicDisplayControlsPanel.add(new JLabel("Show overlays:"));

		graphicDisplayButtons = new ButtonGroup();
		GraphicDisplayActionListener listener = new GraphicDisplayActionListener();

		offButton = new JRadioButton("off",false);
		offButton.setActionCommand(offCommand);
		offButton.setToolTipText("Do not display overlays");
		offButton.addActionListener(listener);
		graphicDisplayButtons.add(offButton);
		graphicDisplayControlsPanel.add(offButton);

		onButton = new JRadioButton("on",true);
		onButton.setActionCommand(onCommand);
		onButton.setToolTipText("Display overlays");
		onButton.addActionListener(listener);
		graphicDisplayButtons.add(onButton);
		graphicDisplayControlsPanel.add(onButton);
	}
}



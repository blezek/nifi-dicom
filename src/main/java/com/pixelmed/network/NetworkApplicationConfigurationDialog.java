/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.FileInputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.Border;

import javax.swing.text.JTextComponent;

import java.util.Iterator; 
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class provides user interface for network applications to configure and test properties related to DICOM network services.</p>
  *
 * @author	dclunie
 */
public class NetworkApplicationConfigurationDialog {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkApplicationConfigurationDialog.java,v 1.9 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NetworkApplicationConfigurationDialog.class);

	protected static String resourceBundleName  = "com.pixelmed.network.NetworkApplicationConfigurationDialog";

	protected ResourceBundle resourceBundle;
	protected NetworkApplicationInformation networkApplicationInformation;
	protected NetworkApplicationProperties networkApplicationProperties;
	
	protected JTextField calledAETitleField;
	protected JTextField callingAETitleField;
	protected JTextField listeningPortField;
	protected JTextField acceptorMaximumLengthReceivedField;
	protected JTextField acceptorSocketReceiveBufferSizeField;
	protected JTextField acceptorSocketSendBufferSizeField;
	protected JTextField initiatorMaximumLengthReceivedField;
	protected JTextField initiatorSocketReceiveBufferSizeField;
	protected JTextField initiatorSocketSendBufferSizeField;

	Component componentToCenterDialogOver;
	JDialog dialog;

	protected String showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(Component parent,NetworkApplicationInformation networkApplicationInformation,String message,String title) {
		String ae = null;
		if (networkApplicationInformation != null) {
			Set localNamesOfRemoteAEs = networkApplicationInformation.getListOfLocalNamesOfApplicationEntities();
			if (localNamesOfRemoteAEs != null) {
				String sta[] = new String[localNamesOfRemoteAEs.size()];
				int i=0;
				Iterator it = localNamesOfRemoteAEs.iterator();
				while (it.hasNext()) {
					sta[i++]=(String)(it.next());
				}
				ae = (String)JOptionPane.showInputDialog(parent,message,title,JOptionPane.QUESTION_MESSAGE,null,sta,null/* no default selection*/);
			}
		}
		return ae;
	}

	protected class AddRemoteAEActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				NetworkApplicationInformation infoFromProperties = networkApplicationProperties.getNetworkApplicationInformation();
//System.err.println("NetworkApplicationConfigurationDialog.AddRemoteAEActionListener(): infoFromProperties before = "+infoFromProperties);
				ApplicationEntityConfigurationDialog aed = new ApplicationEntityConfigurationDialog(componentToCenterDialogOver);
				String localName = aed.getLocalName();
				infoFromProperties.remove(localName);	// in case it already existed, in which case we replace it (sliently :()
				infoFromProperties.add(localName,aed);
//System.err.println("NetworkApplicationConfigurationDialog.AddRemoteAEActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("Ignoring exception", e);
			}
		}
	}
	
	protected class EditRemoteAEActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				NetworkApplicationInformation infoFromProperties = networkApplicationProperties.getNetworkApplicationInformation();
//System.err.println("NetworkApplicationConfigurationDialog.EditRemoteAEActionListener(): infoFromProperties before = "+infoFromProperties);
				String localName = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(componentToCenterDialogOver,infoFromProperties,resourceBundle.getString("selectNetworkTargetToEditLabelText"),resourceBundle.getString("editButtonLabelText"));
				if (localName != null && localName.length() > 0) {
					Set localNamesFromProperties = infoFromProperties.getListOfLocalNamesOfApplicationEntities();
					ApplicationEntityMap aesFromProperties = infoFromProperties.getApplicationEntityMap();
					String aeTitle = infoFromProperties.getApplicationEntityTitleFromLocalName(localName);
					if (aesFromProperties != null && aeTitle != null && aeTitle.length() > 0) {
						ApplicationEntity ae = (ApplicationEntity)(aesFromProperties.get(aeTitle));
						if (ae != null) {
							infoFromProperties.remove(localName);	// remove it BEFORE editing, since may have different local name on return
							ApplicationEntityConfigurationDialog aed = new ApplicationEntityConfigurationDialog(componentToCenterDialogOver,localName,ae);
							localName = aed.getLocalName();			// may have been edited
							infoFromProperties.add(localName,aed);
						}
					}
				}
//System.err.println("NetworkApplicationConfigurationDialog.EditRemoteAEActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("Ignoring exception", e);
			}
		}
	}
	
	protected class RemoveRemoteAEActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				NetworkApplicationInformation infoFromProperties = networkApplicationProperties.getNetworkApplicationInformation();
//System.err.println("NetworkApplicationConfigurationDialog.RemoveRemoteAEActionListener(): infoFromProperties before = "+infoFromProperties);
				String localName = showInputDialogToSelectNetworkTargetByLocalApplicationEntityName(componentToCenterDialogOver,infoFromProperties,resourceBundle.getString("selectNetworkTargetToRemoveLabelText"),resourceBundle.getString("removeButtonLabelText"));
				if (localName != null && localName.length() > 0) {
					infoFromProperties.remove(localName);
				}
//System.err.println("NetworkApplicationConfigurationDialog.RemoveRemoteAEActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("Ignoring exception", e);
			}
		}
	}
	
	protected class DoneActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			boolean good = true;
			// check and update properties for our listener ...
			String calledAETitle = "";
			{
				calledAETitle = calledAETitleField.getText();
				if (!ApplicationEntityConfigurationDialog.isValidAETitle(calledAETitle)) {
					good=false;
					calledAETitleField.setText("\\\\\\BAD\\\\\\");		// use backslash character here (which is illegal in AE's) to make sure this field is edited
				}
			}
			String callingAETitle = "";
			{
				callingAETitle = callingAETitleField.getText();
				if (!ApplicationEntityConfigurationDialog.isValidAETitle(callingAETitle)) {
					good=false;
					callingAETitleField.setText("\\\\\\BAD\\\\\\");		// use backslash character here (which is illegal in AE's) to make sure this field is edited
				}
			}
			int listeningPort=0;
			{
				try {
					listeningPort = Integer.parseInt(listeningPortField.getText());
					if (listeningPort < 1024) {
						good=false;
						listeningPortField.setText("want >= 1024");
					}
				}
				catch (NumberFormatException e) {
					good=false;
					listeningPortField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int acceptorMaximumLengthReceived=0;
			{
				try {
					acceptorMaximumLengthReceived = Integer.parseInt(acceptorMaximumLengthReceivedField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					acceptorMaximumLengthReceivedField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int acceptorSocketReceiveBufferSize=0;
			{
				try {
					acceptorSocketReceiveBufferSize = Integer.parseInt(acceptorSocketReceiveBufferSizeField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					acceptorSocketReceiveBufferSizeField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int acceptorSocketSendBufferSize=0;
			{
				try {
					acceptorSocketSendBufferSize = Integer.parseInt(acceptorSocketSendBufferSizeField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					acceptorSocketSendBufferSizeField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int initiatorMaximumLengthReceived=0;
			{
				try {
					initiatorMaximumLengthReceived = Integer.parseInt(initiatorMaximumLengthReceivedField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					initiatorMaximumLengthReceivedField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int initiatorSocketReceiveBufferSize=0;
			{
				try {
					initiatorSocketReceiveBufferSize = Integer.parseInt(initiatorSocketReceiveBufferSizeField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					initiatorSocketReceiveBufferSizeField.setText("\\\\\\BAD\\\\\\");
				}
			}
			int initiatorSocketSendBufferSize=0;
			{
				try {
					initiatorSocketSendBufferSize = Integer.parseInt(initiatorSocketSendBufferSizeField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					initiatorSocketSendBufferSizeField.setText("\\\\\\BAD\\\\\\");
				}
			}
			if (good) {
				networkApplicationProperties.setListeningPort(listeningPort);
				networkApplicationProperties.setCalledAETitle(calledAETitle);
				networkApplicationProperties.setCallingAETitle(callingAETitle);
				networkApplicationProperties.setAcceptorMaximumLengthReceived(acceptorMaximumLengthReceived);
				networkApplicationProperties.setAcceptorSocketReceiveBufferSize(acceptorSocketReceiveBufferSize);
				networkApplicationProperties.setAcceptorSocketSendBufferSize(acceptorSocketSendBufferSize);
				networkApplicationProperties.setInitiatorMaximumLengthReceived(initiatorMaximumLengthReceived);
				networkApplicationProperties.setInitiatorSocketReceiveBufferSize(initiatorSocketReceiveBufferSize);
				networkApplicationProperties.setInitiatorSocketSendBufferSize(initiatorSocketSendBufferSize);
				
				Cursor was = dialog.getCursor();
				dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					if (networkApplicationInformation instanceof NetworkApplicationInformationFederated) {
						((NetworkApplicationInformationFederated)networkApplicationInformation).removeAllSources();
						((NetworkApplicationInformationFederated)networkApplicationInformation).startupAllKnownSourcesAndRegister(networkApplicationProperties);
					}
					// else do nothing ... we have been modifying the NetworkApplicationInformation inside the existing NetworkApplicationProperties all along
//System.err.println("networkApplicationInformation after = \n"+networkApplicationInformation);
//System.err.println("networkApplicationProperties after = \n"+networkApplicationProperties);
				} catch (Exception e) {
					slf4jlogger.error("Ignoring exception", e);
				}
				dialog.setCursor(was);
				dialog.dispose();
			}
			// else if not good, do not dispose ... make user fix the problems
		}
	}
	
	/**
	 * <p>Configure and test network information.</p>
	 *
	 * @param	parent							the parent component on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	networkApplicationInformation	the current information whose contents are to be replaced with updated information
	 * @param	networkApplicationProperties	the static properties that are to be edited
	 */
	public NetworkApplicationConfigurationDialog(Component parent,NetworkApplicationInformation networkApplicationInformation,NetworkApplicationProperties networkApplicationProperties) throws DicomNetworkException {
		this.networkApplicationInformation = networkApplicationInformation;
		this.networkApplicationProperties = networkApplicationProperties;

		resourceBundle = ResourceBundle.getBundle(resourceBundleName);
		
		componentToCenterDialogOver = parent;
		
		dialog = new JDialog();
		dialog.setModal(true);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(componentToCenterDialogOver);	// without this, appears at TLHC rather then center of parent or screen

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		//dialog.addWindowListener(new WindowAdapter() {
		//	public void windowClosing(WindowEvent we) {
		//		System.err.println("Thwarted user attempt to close window.");
		//	}
		//});
		
		Border panelBorder = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(5,20,5,20));

		JPanel listenerPanel = new JPanel(new GridLayout(0,2));	// "Specifying the number of columns affects the layout only when the number of rows is set to zero."
		listenerPanel.setBorder(panelBorder);
		{
			JLabel listenerHeaderJLabel = new JLabel(resourceBundle.getString("listenerHeaderLabelText")+":");
			listenerPanel.add(listenerHeaderJLabel);
			listenerPanel.add(new JLabel(""));
		}
		try {
			java.util.Enumeration<java.net.NetworkInterface> nifs = java.net.NetworkInterface.getNetworkInterfaces();
			while (nifs.hasMoreElements()) {
				java.net.NetworkInterface nif = nifs.nextElement();
				java.util.Enumeration<java.net.InetAddress> inetas = nif.getInetAddresses();
				while (inetas.hasMoreElements()) {
					java.net.InetAddress ineta = inetas.nextElement();
					if (!ineta.isLoopbackAddress()) {
						String hostname = ineta.getCanonicalHostName();
						listenerPanel.add(new JLabel(hostname+": ",SwingConstants.RIGHT));
						String hostaddress = ineta.getHostAddress();
						listenerPanel.add(new JLabel(hostaddress));
					}
				}
			}
		}
		catch (java.net.SocketException e) {
			// don't care if there were no interfaces
			System.err.println(e);
		}
		{
			JLabel callingAETitleJLabel = new JLabel(resourceBundle.getString("callingAETitleLabelText")+": ",SwingConstants.RIGHT);
			callingAETitleJLabel.setToolTipText(resourceBundle.getString("callingAETitleJLabelToolTipText"));
			listenerPanel.add(callingAETitleJLabel);
			callingAETitleField = new JTextField();
			callingAETitleField.setText(networkApplicationProperties.getCallingAETitle());
        	listenerPanel.add(callingAETitleField);
			callingAETitleField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel calledAETitleJLabel = new JLabel(resourceBundle.getString("calledAETitleLabelText")+": ",SwingConstants.RIGHT);
			calledAETitleJLabel.setToolTipText(resourceBundle.getString("calledAETitleJLabelToolTipText"));
			listenerPanel.add(calledAETitleJLabel);
			calledAETitleField = new JTextField();
			calledAETitleField.setText(networkApplicationProperties.getCalledAETitle());
        	listenerPanel.add(calledAETitleField);
			calledAETitleField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel listeningPortJLabel = new JLabel(resourceBundle.getString("listeningPortLabelText")+": ",SwingConstants.RIGHT);
			listeningPortJLabel.setToolTipText(resourceBundle.getString("listeningPortJLabelToolTipText"));
			listenerPanel.add(listeningPortJLabel);
			listeningPortField = new JTextField();
			listeningPortField.setText(Integer.toString(networkApplicationProperties.getListeningPort()));
        	listenerPanel.add(listeningPortField);
			listeningPortField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel acceptorMaximumLengthReceivedJLabel = new JLabel(resourceBundle.getString("acceptorMaximumLengthReceivedLabelText")+": ",SwingConstants.RIGHT);
			acceptorMaximumLengthReceivedJLabel.setToolTipText(resourceBundle.getString("acceptorMaximumLengthReceivedJLabelToolTipText"));
			listenerPanel.add(acceptorMaximumLengthReceivedJLabel);
			acceptorMaximumLengthReceivedField = new JTextField();
			acceptorMaximumLengthReceivedField.setText(Integer.toString(networkApplicationProperties.getAcceptorMaximumLengthReceived()));
        	listenerPanel.add(acceptorMaximumLengthReceivedField);
			acceptorMaximumLengthReceivedField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel acceptorSocketReceiveBufferSizeJLabel = new JLabel(resourceBundle.getString("acceptorSocketReceiveBufferSizeLabelText")+": ",SwingConstants.RIGHT);
			acceptorSocketReceiveBufferSizeJLabel.setToolTipText(resourceBundle.getString("acceptorSocketReceiveBufferSizeJLabelToolTipText"));
			listenerPanel.add(acceptorSocketReceiveBufferSizeJLabel);
			acceptorSocketReceiveBufferSizeField = new JTextField();
			acceptorSocketReceiveBufferSizeField.setText(Integer.toString(networkApplicationProperties.getAcceptorSocketReceiveBufferSize()));
        	listenerPanel.add(acceptorSocketReceiveBufferSizeField);
			acceptorSocketReceiveBufferSizeField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel acceptorSocketSendBufferSizeJLabel = new JLabel(resourceBundle.getString("acceptorSocketSendBufferSizeLabelText")+": ",SwingConstants.RIGHT);
			acceptorSocketSendBufferSizeJLabel.setToolTipText(resourceBundle.getString("acceptorSocketSendBufferSizeJLabelToolTipText"));
			listenerPanel.add(acceptorSocketSendBufferSizeJLabel);
			acceptorSocketSendBufferSizeField = new JTextField();
			acceptorSocketSendBufferSizeField.setText(Integer.toString(networkApplicationProperties.getAcceptorSocketSendBufferSize()));
        	listenerPanel.add(acceptorSocketSendBufferSizeField);
			acceptorSocketSendBufferSizeField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel initiatorMaximumLengthReceivedJLabel = new JLabel(resourceBundle.getString("initiatorMaximumLengthReceivedLabelText")+": ",SwingConstants.RIGHT);
			initiatorMaximumLengthReceivedJLabel.setToolTipText(resourceBundle.getString("initiatorMaximumLengthReceivedJLabelToolTipText"));
			listenerPanel.add(initiatorMaximumLengthReceivedJLabel);
			initiatorMaximumLengthReceivedField = new JTextField();
			initiatorMaximumLengthReceivedField.setText(Integer.toString(networkApplicationProperties.getInitiatorMaximumLengthReceived()));
        	listenerPanel.add(initiatorMaximumLengthReceivedField);
			initiatorMaximumLengthReceivedField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel initiatorSocketReceiveBufferSizeJLabel = new JLabel(resourceBundle.getString("initiatorSocketReceiveBufferSizeLabelText")+": ",SwingConstants.RIGHT);
			initiatorSocketReceiveBufferSizeJLabel.setToolTipText(resourceBundle.getString("initiatorSocketReceiveBufferSizeJLabelToolTipText"));
			listenerPanel.add(initiatorSocketReceiveBufferSizeJLabel);
			initiatorSocketReceiveBufferSizeField = new JTextField();
			initiatorSocketReceiveBufferSizeField.setText(Integer.toString(networkApplicationProperties.getInitiatorSocketReceiveBufferSize()));
        	listenerPanel.add(initiatorSocketReceiveBufferSizeField);
			initiatorSocketReceiveBufferSizeField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		{
			JLabel initiatorSocketSendBufferSizeJLabel = new JLabel(resourceBundle.getString("initiatorSocketSendBufferSizeLabelText")+": ",SwingConstants.RIGHT);
			initiatorSocketSendBufferSizeJLabel.setToolTipText(resourceBundle.getString("initiatorSocketSendBufferSizeJLabelToolTipText"));
			listenerPanel.add(initiatorSocketSendBufferSizeJLabel);
			initiatorSocketSendBufferSizeField = new JTextField();
			initiatorSocketSendBufferSizeField.setText(Integer.toString(networkApplicationProperties.getInitiatorSocketSendBufferSize()));
        	listenerPanel.add(initiatorSocketSendBufferSizeField);
			initiatorSocketSendBufferSizeField.addFocusListener(new java.awt.event.FocusAdapter() {
				public void focusGained(java.awt.event.FocusEvent event) {
					JTextComponent textComponent = (JTextComponent)(event.getSource());
					textComponent.selectAll();
				}
			});
		}
		
		JPanel remoteAEPanel = new JPanel();
		{
			remoteAEPanel.setBorder(panelBorder);
			JPanel remoteHeaderPanel = new JPanel(new GridLayout(1,1));
			{
				remoteHeaderPanel.add(new JLabel(resourceBundle.getString("remoteHeaderPanelLabelText")+":"));
			}
		
			JPanel remoteButtonPanel = new JPanel();
			{
				remoteButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

				JButton addRemoteAEButton = new JButton(resourceBundle.getString("addButtonLabelText"));
				addRemoteAEButton.setToolTipText(resourceBundle.getString("addButtonToolTipText"));
				remoteButtonPanel.add(addRemoteAEButton);
				addRemoteAEButton.addActionListener(new AddRemoteAEActionListener());

				JButton editRemoteAEButton = new JButton(resourceBundle.getString("editButtonLabelText"));
				editRemoteAEButton.setToolTipText(resourceBundle.getString("editButtonToolTipText"));
				remoteButtonPanel.add(editRemoteAEButton);
				editRemoteAEButton.addActionListener(new EditRemoteAEActionListener());
		
				JButton removeRemoteAEButton = new JButton(resourceBundle.getString("removeButtonLabelText"));
				removeRemoteAEButton.setToolTipText(resourceBundle.getString("removeButtonToolTipText"));
				remoteButtonPanel.add(removeRemoteAEButton);
				removeRemoteAEButton.addActionListener(new RemoveRemoteAEActionListener());
			}
			
			GridBagLayout layout = new GridBagLayout();
			remoteAEPanel.setLayout(layout);
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.weightx = 1;
				constraints.weighty = 1;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				layout.setConstraints(remoteHeaderPanel,constraints);
				remoteAEPanel.add(remoteHeaderPanel);
			}
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 1;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				layout.setConstraints(remoteButtonPanel,constraints);
				remoteAEPanel.add(remoteButtonPanel);
			}

		}
		
		JPanel commonButtonPanel = new JPanel();
		{
			commonButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			commonButtonPanel.setBorder(panelBorder);

			JButton doneButton = new JButton(resourceBundle.getString("doneButtonLabelText"));
			doneButton.setToolTipText(resourceBundle.getString("doneButtonToolTipText"));
			commonButtonPanel.add(doneButton);
			doneButton.addActionListener(new DoneActionListener());
		}
		
		JPanel mainPanel = new JPanel();
		{
			GridBagLayout mainPanelLayout = new GridBagLayout();
			mainPanel.setLayout(mainPanelLayout);
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.weightx = 1;
				constraints.weighty = 1;
				constraints.fill = GridBagConstraints.BOTH;
				mainPanelLayout.setConstraints(listenerPanel,constraints);
				mainPanel.add(listenerPanel);
			}
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 1;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(remoteAEPanel,constraints);
				mainPanel.add(remoteAEPanel);
			}
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 2;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				mainPanelLayout.setConstraints(commonButtonPanel,constraints);
				mainPanel.add(commonButtonPanel);
			}
		}
		Container content = dialog.getContentPane();
		content.add(mainPanel);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	/**
	 * <p>Test the editing of network properties from the specified file.</p>
	 *
	 * @param	arg	a single file name that is the properties file
	 */
	public static void main(String arg[]) {
		String propertiesFileName = arg[0];
		try {
			FileInputStream in = new FileInputStream(propertiesFileName);
			Properties properties = new Properties(/*defaultProperties*/);
			properties.load(in);
			in.close();
			System.err.println("properties="+properties);	// no need to use SLF4J since command line utility/test
			NetworkApplicationProperties networkApplicationProperties = new NetworkApplicationProperties(properties);
			NetworkApplicationInformation networkApplicationInformation = networkApplicationProperties.getNetworkApplicationInformation();
//System.err.println("networkApplicationInformation before = "+networkApplicationInformation);
			new NetworkApplicationConfigurationDialog(null,networkApplicationInformation,networkApplicationProperties);
			properties = networkApplicationProperties.getProperties(properties);
			System.err.println("properties after="+properties);	// no need to use SLF4J since command line utility/test
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}


	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.Border;

import javax.swing.text.JTextComponent;

/**
 * <p>This class implements a dialog for users to enter DICOM AE network configuration parameters.</p>
 *
 * @author	dclunie
 */
public class ApplicationEntityConfigurationDialog extends ApplicationEntity {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ApplicationEntityConfigurationDialog.java,v 1.10 2017/01/24 10:50:44 dclunie Exp $";

	protected static String resourceBundleName  = "com.pixelmed.network.ApplicationEntityConfigurationDialog";

	protected ResourceBundle resourceBundle;

	protected String localName;
	
	protected JTextField localNameField;
	protected JTextField dicomAETitleField;
	protected JTextField hostnameField;
	protected JTextField portField;
	
	public String getLocalName() { return localName; }
		
	public static boolean isValidLocalName(String localName) {
		boolean good = true;
		if (localName == null) {
			good = false;
		}
		else {
			int l = localName.length();
			if (l == 0) {
				good = false;
			}
			else {
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4398693
				if (!Character.isJavaIdentifierStart(localName.codePointAt(0))) {
					good = false;
				}
				else if (l > 1) {
					for (int i=1; i<l; ++i) {
						int c = localName.codePointAt(i);
						if (!Character.isJavaIdentifierPart(c) && c != '-') {	// allow hyphen, since no reason not to and commonly occurs in hostname based default
							good = false;
						}
					}
				}
			}
		}
		return good;
	}

	// should probably move this method somewhere else, e.g. into com.pixelmed.dicom.ApplicationEntityAttribute
	
	public static boolean isValidAETitle(String aet) {
		// Per PS 3.5: Default Character Repertoire excluding character code 5CH (the BACKSLASH “\” in ISO-IR 6), and control characters LF, FF, CR and ESC. 16 bytes maximum
		boolean good = true;
		if (aet == null) {
			good = false;
		}
		else if (aet.length() == 0) {
			good = false;
		}
		else if (aet.length() > 16) {
			good = false;
		}
		else if (aet.trim().length() == 0) {		// all whitespace is illegal
			good = false;
		}
		else if (aet.contains("\\")) {
			good = false;
		}
		else {
			int l = aet.length();
			for (int i=0; i<l; ++i) {
				int codePoint = aet.codePointAt(i);
				try {
					Character.UnicodeBlock codeBlock = Character.UnicodeBlock.of(codePoint);
					if (codeBlock != Character.UnicodeBlock.BASIC_LATIN) {
						good = false;
					}
					else if (Character.isISOControl(codePoint)) {
						good = false;
					}
				}
				catch (IllegalArgumentException e) {	// if not a valid code point
					good = false;
				}
			}
		}
		return good;
	}

	/**
	 * <p>Configure AE information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	localName	the local name for the AE
	 * @param	ae			the current information whose contents are to be replaced with updated information
	 */
	public ApplicationEntityConfigurationDialog(Component parent,String localName,ApplicationEntity ae) {
		super(ae);
		this.localName = localName;
		doCommonConstructorStuff(parent);
	}

	/**
	 * <p>Configure AE information.</p>
	 *
	 * @param	localName	the local name for the AE
	 * @param	ae			the current information whose contents are to be replaced with updated information
	 */
	public ApplicationEntityConfigurationDialog(String localName,ApplicationEntity ae) {
		super(ae);
		this.localName = localName;
		doCommonConstructorStuff(null);
	}

	/**
	 * <p>Create new AE information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	localName	the local name for the AE
	 */
	public ApplicationEntityConfigurationDialog(Component parent,String localName) {
		super(NetworkDefaultValues.getDefaultApplicationEntityTitle(NetworkDefaultValues.StandardDicomReservedPortNumber));
		presentationAddress = new PresentationAddress(NetworkDefaultValues.getUnqualifiedLocalHostName(),NetworkDefaultValues.StandardDicomReservedPortNumber);
		primaryDeviceType = NetworkDefaultValues.getDefaultPrimaryDeviceType();
		this.localName = localName == null ? NetworkDefaultValues.getUnqualifiedLocalHostName() : localName;
		doCommonConstructorStuff(parent);
	}

	/**
	 * <p>Create new AE information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 */
	public ApplicationEntityConfigurationDialog(Component parent) {
		this(parent,null);
	}

	/**
	 * <p>Create new AE information.</p>
	 */
	public ApplicationEntityConfigurationDialog() {
		super(NetworkDefaultValues.getDefaultApplicationEntityTitle(NetworkDefaultValues.StandardDicomReservedPortNumber));
		presentationAddress = new PresentationAddress(NetworkDefaultValues.getUnqualifiedLocalHostName(),NetworkDefaultValues.StandardDicomReservedPortNumber);
		primaryDeviceType = NetworkDefaultValues.getDefaultPrimaryDeviceType();
		localName = NetworkDefaultValues.getUnqualifiedLocalHostName();
		doCommonConstructorStuff(null);
	}

	protected void doCommonConstructorStuff(Component parent) {

		resourceBundle = ResourceBundle.getBundle(resourceBundleName);

		final JDialog dialog = new JDialog();		// final so that button action listeners can get access to it to dispose of it
		//dialog.setSize(width,height);
		//dialog.setTitle(titleMessage);
		dialog.setModal(true);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(parent);	// without this, appears at TLHC rather then center of parent or screen (if parentFrame is null)
		
		localNameField = new JTextField();
		dicomAETitleField = new JTextField();
		hostnameField = new JTextField();
		portField = new JTextField();
		
		localNameField.setText(localName);
		dicomAETitleField.setText(dicomAETitle);
		hostnameField.setText(presentationAddress.getHostname());
		portField.setText(Integer.toString(presentationAddress.getPort()));
		
		localNameField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		dicomAETitleField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		hostnameField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		portField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		Border panelBorder = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(5,20,5,20));
		JPanel labelsAndFieldsPanel = new JPanel(new GridLayout(4,2));
		labelsAndFieldsPanel.setBorder(panelBorder);
		{
			{
				JLabel localNameJLabel = new JLabel(resourceBundle.getString("localNameLabelText")+": ",SwingConstants.RIGHT);
				localNameJLabel.setToolTipText(resourceBundle.getString("localNameToolTipText"));
				labelsAndFieldsPanel.add(localNameJLabel);
				labelsAndFieldsPanel.add(localNameField);
			}
			{
				JLabel dicomAETitleJLabel = new JLabel(resourceBundle.getString("dicomAETitleLabelText")+": ",SwingConstants.RIGHT);
				dicomAETitleJLabel.setToolTipText(resourceBundle.getString("dicomAETitleToolTipText"));
				labelsAndFieldsPanel.add(dicomAETitleJLabel);
				labelsAndFieldsPanel.add(dicomAETitleField);
			}
			{
				JLabel hostnameJLabel = new JLabel(resourceBundle.getString("hostnameLabelText")+": ",SwingConstants.RIGHT);
				hostnameJLabel.setToolTipText(resourceBundle.getString("hostnameToolTipText"));
				labelsAndFieldsPanel.add(hostnameJLabel);
				labelsAndFieldsPanel.add(hostnameField);
			}
			{
				JLabel portJLabel = new JLabel(resourceBundle.getString("portLabelText")+": ",SwingConstants.RIGHT);
				portJLabel.setToolTipText(resourceBundle.getString("portToolTipText"));
				labelsAndFieldsPanel.add(portJLabel);
				labelsAndFieldsPanel.add(portField);
			}
		}
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("OK");
		okButton.setToolTipText("Accept AE configuration");
		buttonsPanel.add(okButton);
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				boolean good = true;
				localName = localNameField.getText();
				if (!isValidLocalName(localName)) {
					good=false;
					localNameField.setText("\\\\\\BAD\\\\\\");			// use backslash character here (which is illegal in AE's) to make sure this field is edited
				}
				dicomAETitle = dicomAETitleField.getText();
				if (!isValidAETitle(dicomAETitle)) {
					good=false;
					dicomAETitleField.setText("\\\\\\BAD\\\\\\");		// use backslash character here (which is illegal in AE's) to make sure this field is edited
				}
				String hostname = hostnameField.getText();
				// ? should validate host name (e.g., http://www.ops.ietf.org/lists/namedroppers/namedroppers.2002/msg00591.html)
				int port=0;
				try {
					port = Integer.parseInt(portField.getText());
				}
				catch (NumberFormatException e) {
					good=false;
					portField.setText("\\\\\\BAD\\\\\\");
				}
				presentationAddress = new PresentationAddress(hostname,port);
				if (good) {
					dialog.dispose();
				}
			}
		});
		
		JPanel allPanels = new JPanel(new BorderLayout());
		allPanels.add(labelsAndFieldsPanel,BorderLayout.NORTH);
		allPanels.add(buttonsPanel,BorderLayout.SOUTH);

		dialog.getContentPane().add(allPanels);
		dialog.getRootPane().setDefaultButton(okButton);

		dialog.pack();
		dialog.setVisible(true);
	}
	
	/**
	 * <p>Main method for testing.</p>
	 *
	 * @param	arg	array of zero strings - no command line arguments are expected
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 0) {
				ApplicationEntity ae = new ApplicationEntityConfigurationDialog();
				System.err.println("ApplicationEntityConfigurationDialog.main(): result of dialog "+ae);	// no need to use SLF4J since command line utility/test
			}
			else {
				throw new Exception("Argument list must be empty");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}


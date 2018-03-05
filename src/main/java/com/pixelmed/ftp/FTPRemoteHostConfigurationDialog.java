/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.border.Border;

import javax.swing.text.JTextComponent;

/**
 * <p>This class implements a dialog for users to enter ftp remote host configuration parameters.</p>
 *
 * @author	dclunie
 */
public class FTPRemoteHostConfigurationDialog extends FTPRemoteHost {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPRemoteHostConfigurationDialog.java,v 1.6 2017/01/24 10:50:43 dclunie Exp $";

	protected static final String badFieldString = "\\\\\\BAD\\\\\\";
	
	protected String localName;
	
	protected JTextField localNameField;
	protected JTextField hostField;
	protected JTextField userField;
	protected JTextField passwordField;
	protected JTextField directoryField;
	protected JMenu securityMenu;
	
	public String getLocalName() { return localName; }
	
	/**
	 * <p>Configure ftp remote host information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	localName	the local name for the remote host
	 * @param	frh			the current information whose contents are to be replaced with updated information
	 */
	public FTPRemoteHostConfigurationDialog(Component parent,String localName,FTPRemoteHost frh) {
		super(frh);
		this.localName = localName;
		doCommonConstructorStuff(parent);
	}

	/**
	 * <p>Configure ftp remote host information.</p>
	 *
	 * @param	localName	the local name for the remote host
	 * @param	frh			the current information whose contents are to be replaced with updated information
	 */
	public FTPRemoteHostConfigurationDialog(String localName,FTPRemoteHost frh) {
		super(frh);
		this.localName = localName;
		doCommonConstructorStuff(null);
	}

	/**
	 * <p>Create new ftp remote host information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	localName	the local name for the remote host
	 */
	public FTPRemoteHostConfigurationDialog(Component parent,String localName) {
		// no defaults for anything
		super();
		this.localName = localName;
		doCommonConstructorStuff(parent);
	}

	/**
	 * <p>Create new ftp remote host information.</p>
	 *
	 * @param	parent		the parent component (JFrame or JDialog) on which the new dialog is centered, may be null in which case centered on the screen
	 */
	public FTPRemoteHostConfigurationDialog(Component parent) {
		this(parent,null);
	}

	/**
	 * <p>Create new ftp remote host information.</p>
	 */
	public FTPRemoteHostConfigurationDialog() {
		// no defaults for anything
		super();
		localName = null;
		doCommonConstructorStuff(null);
	}

	protected void doCommonConstructorStuff(Component parent) {
		final JDialog dialog = new JDialog();		// final so that button action listeners can get access to it to dispose of it
		//dialog.setSize(width,height);
		//dialog.setTitle(titleMessage);
		dialog.setModal(true);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(parent);	// without this, appears at TLHC rather then center of parent or screen (if parentFrame is null)
		
		localNameField = new JTextField();
		hostField = new JTextField();
		userField = new JTextField();
		passwordField = new JTextField();
		directoryField = new JTextField();
		securityMenu = new JMenu("select ...");	// text will be overridden by current choice later
		security = FTPSecurityType.NONE;		// need to set something for default in case the user does not choose anything fron the menu
		{
			ButtonGroup securityButtonGroup = new ButtonGroup();
			String[] securityMenuStrings = FTPSecurityType.getListOfTypesAsString();
			String defaultOrCurrentSelectedSecurityMenuItemString = security == null ? securityMenuStrings[0] : security.toString();
			securityMenu.setText(defaultOrCurrentSelectedSecurityMenuItemString);
			if (securityMenuStrings != null) {
				for (String s : securityMenuStrings) {
//System.err.println("FTPRemoteHostConfigurationDialog.doCommonConstructorStuff(): securityMenu adding item = "+s);
					JRadioButtonMenuItem item = new JRadioButtonMenuItem(s);
					securityMenu.add(item);
					securityButtonGroup.add(item);
					if (s.equals(defaultOrCurrentSelectedSecurityMenuItemString)) {
//System.err.println("FTPRemoteHostConfigurationDialog.doCommonConstructorStuff(): securityMenu is selected "+s);
						item.setSelected(true);
					}
					
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							String choice = ((JRadioButtonMenuItem)event.getSource()).getText();	// easier than passing s to anonymous inner class
//System.err.println("FTPRemoteHostConfigurationDialog.JRadioButtonMenuItem.ActionListener.actionPerformed(): selected "+choice);
							security = FTPSecurityType.selectFromDescription(choice);
							securityMenu.setText(choice);
						}
					});
				}
			}
		}
//System.err.println("FTPRemoteHostConfigurationDialog.doCommonConstructorStuff(): securityMenu = "+securityMenu);
		
		localNameField.setText(localName);
		hostField.setText(host);
		userField.setText(user);
		passwordField.setText(password);
		directoryField.setText(directory);
		
		localNameField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		hostField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		userField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		directoryField.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusGained(java.awt.event.FocusEvent event) {
				JTextComponent textComponent = (JTextComponent)(event.getSource());
				textComponent.selectAll();
			}
		});
		
		Border panelBorder = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(5,20,5,20));
		JPanel labelsAndFieldsPanel = new JPanel(new GridLayout(0,2));	// only one makes a difference, and want two columns
		labelsAndFieldsPanel.setBorder(panelBorder);
		{
			{
				JLabel localNameJLabel = new JLabel("Local name: ",SwingConstants.RIGHT);
				localNameJLabel.setToolTipText("The name by which we refer to this remote ftp host(which can be different from its network host name)");
				labelsAndFieldsPanel.add(localNameJLabel);
				labelsAndFieldsPanel.add(localNameField);
			}
			{
				JLabel hostJLabel = new JLabel("Hostname or IP address: ",SwingConstants.RIGHT);
				hostJLabel.setToolTipText("The remote host name within the current domain, or fully qualified hostname or the IPV4 address of the remote host");
				labelsAndFieldsPanel.add(hostJLabel);
				labelsAndFieldsPanel.add(hostField);
			}
			{
				JLabel userJLabel = new JLabel("User: ",SwingConstants.RIGHT);
				userJLabel.setToolTipText("The user name to log in with");
				labelsAndFieldsPanel.add(userJLabel);
				labelsAndFieldsPanel.add(userField);
			}
			{
				JLabel passwordJLabel = new JLabel("Password: ",SwingConstants.RIGHT);
				passwordJLabel.setToolTipText("The password to log in with");
				labelsAndFieldsPanel.add(passwordJLabel);
				labelsAndFieldsPanel.add(passwordField);
			}
			{
				JLabel directoryJLabel = new JLabel("Directory: ",SwingConstants.RIGHT);
				directoryJLabel.setToolTipText("The directory to switch to");
				labelsAndFieldsPanel.add(directoryJLabel);
				labelsAndFieldsPanel.add(directoryField);
			}
			{
				JLabel securityJLabel = new JLabel("Security: ",SwingConstants.RIGHT);
				securityJLabel.setToolTipText("The security to use");
				labelsAndFieldsPanel.add(securityJLabel);
				JMenuBar securityMenuBar = new JMenuBar();
				securityMenuBar.add(securityMenu);
				labelsAndFieldsPanel.add(securityMenuBar);
			}
		}
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("OK");
		okButton.setToolTipText("Accept remote host configuration");
		buttonsPanel.add(okButton);
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
//System.err.println("FTPRemoteHostConfigurationDialog.okButton.ActionListener.actionPerformed():");
				boolean good = true;
				localName = localNameField.getText().trim();
				if (localName.length() == 0 || localName.equals(badFieldString)) {
					good=false;
					localNameField.setText(badFieldString);
				}
				host = hostField.getText().trim();
				if (host.length() == 0 || host.equals(badFieldString)) {
					good=false;
					hostField.setText(badFieldString);
				}
				// ? should validate host name (e.g., http://www.ops.ietf.org/lists/namedroppers/namedroppers.2002/msg00591.html)
				user = userField.getText().trim();
				if (user.length() == 0 || user.equals(badFieldString)) {
					good=false;
					userField.setText(badFieldString);
				}
				password = passwordField.getText().trim();
				if (password.length() == 0 || password.equals(badFieldString)) {
					good=false;
					passwordField.setText(badFieldString);
				}
				directory = directoryField.getText().trim();	// zero length is OK
				// security already set by radio button action listener, and if not defaults to NONE, so this should not happen ...
				if (security == null) {
					good=false;
				}
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
				FTPRemoteHost frh = new FTPRemoteHostConfigurationDialog();
				System.err.println("FTPRemoteHostConfigurationDialog.main(): result of dialog "+frh);	// no need to use SLF4J since command line utility/test
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


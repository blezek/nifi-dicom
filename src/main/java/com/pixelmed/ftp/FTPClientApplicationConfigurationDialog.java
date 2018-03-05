/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

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
import java.io.FileOutputStream;
//import java.io.PrintWriter;
//import java.io.StringWriter;

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
import java.util.Set;
//import java.util.StringTokenizer;
//import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class provides a user interface for applications to configure and test properties related to remote FTP hosts.</p>
  *
 * @author	dclunie
 */
public class FTPClientApplicationConfigurationDialog {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPClientApplicationConfigurationDialog.java,v 1.5 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FTPClientApplicationConfigurationDialog.class);

	protected FTPRemoteHostInformation ftpRemoteHostInformation;
	protected FTPApplicationProperties ftpApplicationProperties;
	
	protected JTextField calledAETitleField;
	protected JTextField callingAETitleField;
	protected JTextField listeningPortField;

	Component componentToCenterDialogOver;
	JDialog dialog;

	protected String showInputDialogToSelectNetworkTargetByLocalName(Component parent,FTPRemoteHostInformation ftpRemoteHostInformation,String message,String title) {
		String ae = null;
		if (ftpRemoteHostInformation != null) {
			Set localNamesOfRemoteHosts = ftpRemoteHostInformation.getListOfLocalNames();
			if (localNamesOfRemoteHosts != null) {
				String sta[] = new String[localNamesOfRemoteHosts.size()];
				int i=0;
				Iterator it = localNamesOfRemoteHosts.iterator();
				while (it.hasNext()) {
					sta[i++]=(String)(it.next());
				}
				ae = (String)JOptionPane.showInputDialog(parent,message,title,JOptionPane.QUESTION_MESSAGE,null,sta,null/* no default selection*/);
			}
		}
		return ae;
	}

	protected class AddRemoteHostActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				FTPRemoteHostInformation infoFromProperties = ftpApplicationProperties.getFTPRemoteHostInformation();
//System.err.println("FTPClientApplicationConfigurationDialog.AddRemoteHostActionListener(): infoFromProperties before = "+infoFromProperties);
				FTPRemoteHostConfigurationDialog rhd = new FTPRemoteHostConfigurationDialog(componentToCenterDialogOver);
				String localName = rhd.getLocalName();
				infoFromProperties.remove(localName);	// in case it already existed, in which case we replace it (sliently :()
				infoFromProperties.add(localName,rhd);
//System.err.println("FTPClientApplicationConfigurationDialog.AddRemoteHostActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	protected class EditRemoteHostActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				FTPRemoteHostInformation infoFromProperties = ftpApplicationProperties.getFTPRemoteHostInformation();
//System.err.println("FTPClientApplicationConfigurationDialog.EditRemoteHostActionListener(): infoFromProperties before = "+infoFromProperties);
				String localName = showInputDialogToSelectNetworkTargetByLocalName(componentToCenterDialogOver,infoFromProperties,"Select remote host to edit","Edit");
				if (localName != null && localName.length() > 0) {
					if (infoFromProperties != null) {
						FTPRemoteHost frh = infoFromProperties.getRemoteHost(localName);
						if (frh != null) {
							infoFromProperties.remove(localName);	// remove it BEFORE editing, since may have different local name on return
							FTPRemoteHostConfigurationDialog rhd = new FTPRemoteHostConfigurationDialog(componentToCenterDialogOver,localName,frh);
							localName = rhd.getLocalName();			// may have been edited
							infoFromProperties.add(localName,rhd);
						}
					}
				}
//System.err.println("FTPClientApplicationConfigurationDialog.EditRemoteHostActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	protected class RemoveRemoteHostActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				FTPRemoteHostInformation infoFromProperties = ftpApplicationProperties.getFTPRemoteHostInformation();
//System.err.println("FTPClientApplicationConfigurationDialog.RemoveRemoteHostActionListener(): infoFromProperties before = "+infoFromProperties);
				String localName = showInputDialogToSelectNetworkTargetByLocalName(componentToCenterDialogOver,infoFromProperties,"Select remote host to remove","Remove");
				if (localName != null && localName.length() > 0) {
					infoFromProperties.remove(localName);
				}
//System.err.println("FTPClientApplicationConfigurationDialog.RemoveRemoteHostActionListener(): infoFromProperties after = "+infoFromProperties);
			} catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	protected class DoneActionListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			// do nothing ... we have been modifying the FTPRemoteHostInformation inside the existing FTPApplicationProperties all along
			slf4jlogger.info("ftpRemoteHostInformation after = \n{}",ftpRemoteHostInformation);
			slf4jlogger.info("ftpApplicationProperties after = \n{}",ftpApplicationProperties);
			dialog.dispose();
		}
	}
	
	/**
	 * <p>Configure and test network information.</p>
	 *
	 * @param	parent							the parent component on which the new dialog is centered, may be null in which case centered on the screen
	 * @param	ftpRemoteHostInformation	the current information whose contents are to be replaced with updated information
	 * @param	ftpApplicationProperties	the static properties that are to be edited
	 */
	public FTPClientApplicationConfigurationDialog(Component parent,FTPRemoteHostInformation ftpRemoteHostInformation,FTPApplicationProperties ftpApplicationProperties) throws FTPException {
		this.ftpRemoteHostInformation = ftpRemoteHostInformation;
		this.ftpApplicationProperties = ftpApplicationProperties;
		
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
			JLabel listenerHeaderJLabel = new JLabel("Our FTP properties:");
			listenerPanel.add(listenerHeaderJLabel);
			listenerPanel.add(new JLabel(""));
		}
		
		JPanel remoteHostPanel = new JPanel();
		{
			remoteHostPanel.setBorder(panelBorder);
			JPanel remoteHeaderPanel = new JPanel(new GridLayout(1,1));
			{
				remoteHeaderPanel.add(new JLabel("Remote FTP host network properties:"));
			}
		
			JPanel remoteButtonPanel = new JPanel();
			{
				remoteButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

				JButton addRemoteHostButton = new JButton("Add");
				addRemoteHostButton.setToolTipText("Add a host to the list of remote FTP hosts");
				remoteButtonPanel.add(addRemoteHostButton);
				addRemoteHostButton.addActionListener(new AddRemoteHostActionListener());

				JButton editRemoteHostButton = new JButton("Edit");
				editRemoteHostButton.setToolTipText("Edit a host in the list of remote FTP hosts");
				remoteButtonPanel.add(editRemoteHostButton);
				editRemoteHostButton.addActionListener(new EditRemoteHostActionListener());
		
				JButton removeRemoteHostButton = new JButton("Remove");
				removeRemoteHostButton.setToolTipText("Remove a host from the list of remote FTP hosts");
				remoteButtonPanel.add(removeRemoteHostButton);
				removeRemoteHostButton.addActionListener(new RemoveRemoteHostActionListener());
			}
			
			GridBagLayout layout = new GridBagLayout();
			remoteHostPanel.setLayout(layout);
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 0;
				constraints.weightx = 1;
				constraints.weighty = 1;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				layout.setConstraints(remoteHeaderPanel,constraints);
				remoteHostPanel.add(remoteHeaderPanel);
			}
			{
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx = 0;
				constraints.gridy = 1;
				constraints.fill = GridBagConstraints.HORIZONTAL;
				layout.setConstraints(remoteButtonPanel,constraints);
				remoteHostPanel.add(remoteButtonPanel);
			}

		}
		
		JPanel commonButtonPanel = new JPanel();
		{
			commonButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			commonButtonPanel.setBorder(panelBorder);

			JButton doneButton = new JButton("Done");
			doneButton.setToolTipText("Finished edits, so reset application to use new configuration");
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
				mainPanelLayout.setConstraints(remoteHostPanel,constraints);
				mainPanel.add(remoteHostPanel);
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
			FTPApplicationProperties ftpApplicationProperties = new FTPApplicationProperties(properties);
			FTPRemoteHostInformation ftpRemoteHostInformation = ftpApplicationProperties.getFTPRemoteHostInformation();
//System.err.println("ftpRemoteHostInformation before = "+ftpRemoteHostInformation);
			new FTPClientApplicationConfigurationDialog(null,ftpRemoteHostInformation,ftpApplicationProperties);
			properties = ftpApplicationProperties.getProperties(properties);
			System.err.println("properties after="+properties);
			FileOutputStream out = new FileOutputStream(propertiesFileName);
			properties.store(out,"Edited and saved from user interface");
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


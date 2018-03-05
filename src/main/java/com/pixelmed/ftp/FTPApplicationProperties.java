/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

import java.io.FileInputStream;

import java.util.Properties; 

/**
 * <p>This class provides common support to applications requiring properties related to FTP network services.</p>
 *
 * @author	dclunie
 */
public class FTPApplicationProperties {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPApplicationProperties.java,v 1.5 2017/01/24 10:50:43 dclunie Exp $";
	
	protected FTPRemoteHostInformation ftpRemoteHostInformation;

	/**
	 * <p>Create default properties.</p>
	 */
	public FTPApplicationProperties() throws FTPException {
//System.err.println("FTPApplicationProperties():");
		ftpRemoteHostInformation = new FTPRemoteHostInformation();
	}
	
	/**
	 * <p>Extract the ftp properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public FTPApplicationProperties(Properties properties) throws FTPException {
//System.err.println("FTPApplicationProperties(Properties): properties ="+properties);
		ftpRemoteHostInformation = new FTPRemoteHostInformation(properties);
	}
	
	/**
	 * <p>Retrieve the ftp properties.</p>
	 *
	 * param	properties	the existing properties to add to (replacing corresponding properties already there), or null if none
	 *
	 * @return	the updated properties or a new set of properties if none supplied
	 */
	public Properties getProperties(Properties properties) {
//System.err.println("FTPApplicationProperties.getProperties(): at start, properties = \n"+properties);
		if (properties == null) {
			properties = new Properties();
		}
		
		ftpRemoteHostInformation.getProperties(properties);	// remove any existing entries in properties, and add properties for all in ftpRemoteHostInformation

//System.err.println("FTPApplicationProperties.getProperties(): at end, properties = \n"+properties);
		return properties;
	}
	
	/**
	 * <p>Return the network application information.</p>
	 *
	 * @return	the network application information
	 */
	public FTPRemoteHostInformation getFTPRemoteHostInformation() { return ftpRemoteHostInformation; }
	
	/**
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Remote applications:\n"+ftpRemoteHostInformation+"\n");
		return str.toString();
	}

	/**
	 * <p>Test the parsing of network properties from the specified file, by reading them and printing them.</p>
	 *
	 * @param	arg	one argument, a single file name that is the properties file
	 */
	public static void main(String arg[]) {
		String propertiesFileName = arg[0];
		try {
			FileInputStream in = new FileInputStream(propertiesFileName);
			Properties properties = new Properties(/*defaultProperties*/);
			properties.load(in);
			in.close();
			System.err.println("properties="+properties);
		}
		catch (Exception e) {
			System.err.println(e);
		}


	}
}


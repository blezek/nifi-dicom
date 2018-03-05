/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.Properties; 
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class encapsulates information about remote FTP servers.</p>
 *
 * <p>The following properties are supported:</p>
 *
 * <p><code>Ftp.RemoteHosts</code> - a space or comma separated list of the local names all the available remote hosts; each local name may be anything unique (in this file) without a space or comma; the local name does not need to be the same as the remote host's name</p>
 * <p><code>Ftp.XXXX.HostNameOrIPAddress</code> - for the remote host with local name XXXX, what host or IP addess that AE will listen on for incoming connections</p>
 * <p><code>Ftp.XXXX.User</code> - for the remote host with local name XXXX, what user name to login with</p>
 * <p><code>Ftp.XXXX.Password</code> - for the remote host with local name XXXX, what password to login with</p>
 * <p><code>Ftp.XXXX.Directory</code> - for the remote host with local name XXXX, what initial working directory to change to</p>
 * <p><code>Ftp.XXXX.Security</code> - for the remote host with local name XXXX, what the type of security to use (supported values are NONE, TLS)</p>
 *
 * @author	dclunie
 */
public class FTPRemoteHostInformation {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPRemoteHostInformation.java,v 1.7 2017/01/24 10:50:43 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FTPRemoteHostInformation.class);

	public static final String propertyName_FtpRemoteHosts = "Ftp.RemoteHosts";
	
	protected static final String propertyNameSuffix_HostNameOrIPAddress = "HostNameOrIPAddress";
	protected static final String propertyNameSuffix_User = "User";
	protected static final String propertyNameSuffix_Password = "Password";
	protected static final String propertyNameSuffix_Directory = "Directory";
	protected static final String propertyNameSuffix_Security = "Security";
	
	protected static final String propertyDelimitersForTokenizer_FtpRemoteAEs = ", ";

	protected final TreeMap<String,FTPRemoteHost> localNameToRemoteHostMap;
	
	/**
	 * <p>Construct an empty container for properties of FTP network devices.</p>
	 */
	public FTPRemoteHostInformation() {
		localNameToRemoteHostMap = new TreeMap<String,FTPRemoteHost>();
	}
	
	/**
	 * <p>Extract the FTP network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public FTPRemoteHostInformation(Properties properties) throws FTPException {
		this();
		String remoteHosts = properties.getProperty(propertyName_FtpRemoteHosts);
		if (remoteHosts != null && remoteHosts.length() > 0) {
			StringTokenizer st = new StringTokenizer(remoteHosts,propertyDelimitersForTokenizer_FtpRemoteAEs);
			while (st.hasMoreTokens()) {
				String localName=st.nextToken();
				String host = properties.getProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress);
				String user = properties.getProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_User);
				String password = properties.getProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Password);
				String directory = properties.getProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Directory);
				String securityString = properties.getProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Security);
				FTPSecurityType security = FTPSecurityType.selectFromDescription(securityString);
//System.err.println(localName+": "
//	+"HostNameOrIPAddress <"+host+"> "
//	+"User <"+user+"> "
//	+"Password <"+password+"> "
//	+"Directory <"+directory+"> "
//	+"Security <"+security+"> "
//);
				add(localName,host,user,password,directory,security);
			}
		}
	}
	
	/**
	 * <p>Retrieve the FTP network properties.</p>
	 *
	 * param	properties	the existing properties to add to (removing any properties already there), or null if none
	 *
	 * @return	the updated properties or a new set of properties if none supplied
	 */
	public Properties getProperties(Properties properties) {
//System.err.println("FTPRemoteHostInformation.getProperties(): at start, properties = \n"+properties);
		if (properties == null) {
			properties = new Properties();
		}
		else {
			// need to remove any existing entries
			String remoteHosts = properties.getProperty(propertyName_FtpRemoteHosts);
			if (remoteHosts != null && remoteHosts.length() > 0) {
				StringTokenizer st = new StringTokenizer(remoteHosts,propertyDelimitersForTokenizer_FtpRemoteAEs);
				while (st.hasMoreTokens()) {
					String localName=st.nextToken();
					properties.remove(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress);
					properties.remove(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_User);
					properties.remove(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Password);
					properties.remove(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Directory);
					properties.remove(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Security);
				}
			}
			properties.remove(propertyName_FtpRemoteHosts);
			slf4jlogger.info("FTPRemoteHostInformation.getProperties(): after removing existing remote AEs, properties = \n{}",properties);
		}
		
		{
			StringBuffer remoteHosts = new StringBuffer();
			String prefixForRemoteAEs = "";
			Iterator i = getListOfLocalNames().iterator();
			while (i.hasNext()) {
				String localName = (String)i.next();
				if (localName != null) {
					FTPRemoteHost frh = localNameToRemoteHostMap.get(localName);
					if (frh != null) {
						properties.setProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress,frh.getHost());
						properties.setProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_User,frh.getUser());
						properties.setProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Password,frh.getPassword());
						properties.setProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Directory,frh.getDirectory());
						properties.setProperty(propertyName_FtpRemoteHosts+"."+localName+"."+propertyNameSuffix_Security,frh.getSecurity().toString());

						remoteHosts.append(prefixForRemoteAEs);
						remoteHosts.append(localName);
						prefixForRemoteAEs = propertyDelimitersForTokenizer_FtpRemoteAEs;
					}
				}
			}
			properties.setProperty(propertyName_FtpRemoteHosts,remoteHosts.toString());
		}
		slf4jlogger.info("FTPRemoteHostInformation.getProperties(): at end, properties = \n{}",properties);

		return properties;
	}

	/**
	 * <p>Completely empty all information.</p>
	 */
	public void removeAll() {
//System.err.println("FTPRemoteHostInformation.removeAll():");
		localNameToRemoteHostMap.clear();
	}
	
	/**
	 * <p>Remove a host.</p>
	 *
	 * @param	localName
	 */
	public void remove(String localName) {
		if (localName != null && localName.length() > 0) {
			localNameToRemoteHostMap.remove(localName);
		}
	}
	
	/**
	 * <p>Add a new host.</p>
	 *
	 * @param	localName
	 * @param	frh
	 * @throws	FTPException	if local name already used, or either is null or empty
	 */
	public void add(String localName,FTPRemoteHost frh) throws FTPException {
		if (localName != null && localName.length() > 0) {
			FTPRemoteHost frhExisting = localNameToRemoteHostMap.get(localName);
			if (frhExisting != null) {
				throw new FTPException("Cannot use local name ["+localName+"] - already used");
			}
			localNameToRemoteHostMap.put(localName,frh);
		}
		else {
			throw new FTPException("Cannot use empty local name ["+localName+"]");
		}
	}

	/**
	 * <p>Add a new remote FTP host.</p>
	 *
	 * @param	localName
	 * @param	host
	 * @param	user
	 * @param	password
	 * @param	directory
	 * @param	security
	 * @throws	FTPException	if local name or AET already used, or either is null or empty
	 */
	public void add(String localName,String host,String user,String password,String directory,FTPSecurityType security) throws FTPException {
		FTPRemoteHost frh = new FTPRemoteHost(host,user,password,directory,security);
		add(localName,frh);
	}
	
	/**
	 * <p>Get the information for the specified remote host.</p>
	 *
	 * @param	localName
	 * @return	the remote host information
	 */
	public FTPRemoteHost getRemoteHost(String localName) { return localNameToRemoteHostMap.get(localName); }
	
	/**
	 * <p>Return the set of local names of remote hosts.</p>
	 *
	 * @return	the set of local names
	 */
	public Set getListOfLocalNames() { return localNameToRemoteHostMap.keySet(); }
	
	/**
	 */
	public String toString() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		if (localNameToRemoteHostMap != null) {
			Iterator i = getListOfLocalNames().iterator();
			while (i.hasNext()) {
				String localName = (String)i.next();
				FTPRemoteHost frh = localNameToRemoteHostMap.get(localName);
				printWriter.println("localName="+localName+","+(frh == null ? "-null-" : frh.toString()));
			}
		}
		printWriter.close();
		return stringWriter.toString();
	}
}


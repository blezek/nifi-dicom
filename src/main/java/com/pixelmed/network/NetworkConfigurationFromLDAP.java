/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class provides utilities to automatically configure DICOM network parameters.</p>
 *
 * @author	dclunie
 */
public class NetworkConfigurationFromLDAP extends NetworkConfigurationSource {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkConfigurationFromLDAP.java,v 1.8 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NetworkConfigurationFromLDAP.class);
	
	private static final String defaultInitialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
	
	//private static final String defaultProviderURL = "ldap://localhost:389";
	private static final String defaultProviderURL = "ldap://";	// uses search algorithm DNS then localhost to find LDAP server
									// See "http://java.sun.com/j2se/1.5.0/docs/guide/jndi/jndi-ldap.html#URLs"
	
	private static final String defaultdevicesDN = "cn=Devices,cn=DICOM Configuration,o=pixelmed,c=us";
	private static final String devicesRDN = "cn=Devices";
	
	private class ApplicationEntityWithDicomNetworkConnectionName extends ApplicationEntity {
		private String dicomNetworkConnectionName;

		ApplicationEntityWithDicomNetworkConnectionName(String dicomAETitle,String dicomNetworkConnectionName) {
			super(dicomAETitle);
			this.dicomNetworkConnectionName = dicomNetworkConnectionName;
		}
		public final String getDicomNetworkConnectionName() { return dicomNetworkConnectionName; }
	}


	private String getDicomDevicesRootDistinguishedName(DirContext context) {
		String dicomConfigurationDN = null;
		try {
			slf4jlogger.trace("getDicomDevicesRootDistinguishedName: name of context = {}",context.getNameInNamespace());
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration enumeration = context.search("","(cn=DICOM Configuration)",searchControls);
			while (enumeration.hasMore()) {
				SearchResult result = (SearchResult)(enumeration.next());
				dicomConfigurationDN = result.getName();
				slf4jlogger.trace("getDicomDevicesRootDistinguishedName: found {}",dicomConfigurationDN);
			}
		}
		catch (NamingException e) {
			slf4jlogger.error("Ignoring exception", e);
		}
		String devicesDN = null;
		if (dicomConfigurationDN == null) {
			devicesDN = defaultdevicesDN;
			slf4jlogger.trace("getDicomDevicesRootDistinguishedName: not found  - using default name = {}",devicesDN);
		}
		else {
			devicesDN = devicesRDN + "," + dicomConfigurationDN;
		}
		return devicesDN;
	}

	protected class GetNetworkApplicationInformation extends TimerTask {
	
		private int interval;
		
		GetNetworkApplicationInformation(int interval) {
			this.interval = interval;
		}
		
		public void run() {
			getNetworkApplicationInformation().removeAll();
			getNetworkConfiguration();
		}
		
		void start() {
			timer.schedule(this,0/*no delay to start*/,interval);
		}
	}
	
	protected GetNetworkApplicationInformation getter;
	
	public void activateDiscovery(int refreshInterval) {
		if (refreshInterval == 0) {
			getNetworkConfiguration();	// run once only
		}
		else {
			if (getter == null) {
				getter = new GetNetworkApplicationInformation(refreshInterval);
			}
			getter.start();
		}
	}
	
	public void deActivateDiscovery() {
		if (getter != null) {
			getter.cancel();	// needed, since otherwise application will not exit when main thread finished
		}
	}
		
	protected void getNetworkConfiguration() {
		try {
			//DirContext context = new InitialDirContext();
			//context.addToEnvironment(Context.INITIAL_CONTEXT_FACTORY,defaultInitialContextFactory);
			//context.addToEnvironment(Context.PROVIDER_URL,defaultProviderURL);
			
			Hashtable env = new Hashtable();
			env.put(Context.INITIAL_CONTEXT_FACTORY,defaultInitialContextFactory);
			env.put(Context.PROVIDER_URL,defaultProviderURL);
			DirContext context = new InitialDirContext(env);
			
			String devicesDN = getDicomDevicesRootDistinguishedName(context);
			
			NamingEnumeration listOfDevices = context.search(devicesDN,null/*all attributes*/);
			while (listOfDevices.hasMore()) {
				Attributes adev = ((SearchResult)(listOfDevices.next())).getAttributes();
				BasicAttribute aDicomDeviceName = (BasicAttribute)adev.get("dicomDeviceName");
				String vDicomDeviceName = aDicomDeviceName == null ? "" : aDicomDeviceName.get(0).toString();
				slf4jlogger.trace("dicomDeviceName: {}",vDicomDeviceName);
				if (aDicomDeviceName != null) {
					Map mapOfDicomNetworkConnectionsForThisDevice = new HashMap();	// key is String, value is PresentationAddress
					List listOfApplicationEntitiesForThisDevice = new ArrayList();	// value is ApplicationEntity
					NamingEnumeration listOfDeviceChildren = context.search("dicomDeviceName="+vDicomDeviceName+","+devicesDN,null/*all attributes*/);
					while (listOfDeviceChildren.hasMore()) {
						Attributes adevchildren = ((SearchResult)(listOfDeviceChildren.next())).getAttributes();
						BasicAttribute aObjectClass = (BasicAttribute)adevchildren.get("objectClass");
						String vObjectClass = aObjectClass == null ? "" : aObjectClass.get(0).toString();
						slf4jlogger.trace("\tvObjectClass: {}",vObjectClass);
						if (vObjectClass != null) {
							if (vObjectClass.equals("dicomNetworkAE")) {
								BasicAttribute aDicomAETitle = (BasicAttribute)adevchildren.get("dicomAETitle");
								String vDicomAETitle = aDicomAETitle == null ? "" : aDicomAETitle.get(0).toString();
								slf4jlogger.trace("\t\tdicomAETitle: {}",vDicomAETitle);
								BasicAttribute aDicomNetworkConnectionReference = (BasicAttribute)adevchildren.get("dicomNetworkConnectionReference");
								String vDicomNetworkConnectionReference = aDicomNetworkConnectionReference == null
									? "" : aDicomNetworkConnectionReference.get(0).toString();
								slf4jlogger.trace("\t\tdicomNetworkConnectionReference: {}",vDicomNetworkConnectionReference);
								String dicomNetworkConnectionCommonNameValue = null;
								if (vDicomNetworkConnectionReference != null) {
									int firstDelimiter = vDicomNetworkConnectionReference.indexOf(",");
									if (firstDelimiter >= 0) {
										vDicomNetworkConnectionReference=vDicomNetworkConnectionReference.substring(0,firstDelimiter);
										slf4jlogger.trace("\t\tdicomNetworkConnectionReference first part: {}",vDicomNetworkConnectionReference);
									}
									dicomNetworkConnectionCommonNameValue = vDicomNetworkConnectionReference.replaceFirst("[cC][nN]=","");
									slf4jlogger.trace("\t\tdicomNetworkConnectionCommonNameValue: {}",dicomNetworkConnectionCommonNameValue);
								}
								if (vDicomAETitle != null && vDicomAETitle.length() > 0
								 && dicomNetworkConnectionCommonNameValue != null && dicomNetworkConnectionCommonNameValue.length() > 0) {
									listOfApplicationEntitiesForThisDevice.add(
										new ApplicationEntityWithDicomNetworkConnectionName(vDicomAETitle,dicomNetworkConnectionCommonNameValue));
								}
							}
							else if (vObjectClass.equals("dicomNetworkConnection")) {
								BasicAttribute aCN = (BasicAttribute)adevchildren.get("cn");
								String vCN = aCN == null ? "" : aCN.get(0).toString();
								slf4jlogger.trace("\t\tcn: {}",vCN);
								BasicAttribute aDicomHostname = (BasicAttribute)adevchildren.get("dicomHostname");
								String vDicomHostname = aDicomHostname == null ? "" : aDicomHostname.get(0).toString();
								slf4jlogger.trace("\t\tdicomHostname: {}",vDicomHostname);
								BasicAttribute aDicomPort = (BasicAttribute)adevchildren.get("dicomPort");
								String vDicomPort = aDicomPort == null ? "" : aDicomPort.get(0).toString();
								slf4jlogger.trace("\t\tdicomPort: {}",vDicomPort);
								if (vCN != null && vCN.length() > 0
								 && vDicomHostname != null && vDicomHostname.length() > 0
								 && vDicomPort != null && vDicomPort.length() > 0) {
									mapOfDicomNetworkConnectionsForThisDevice.put(vCN,new PresentationAddress(vDicomHostname,Integer.parseInt(vDicomPort)));
								}
							}
						}
					}
					Iterator iaes = listOfApplicationEntitiesForThisDevice.iterator();
					while (iaes.hasNext()) {
						ApplicationEntityWithDicomNetworkConnectionName ae = (ApplicationEntityWithDicomNetworkConnectionName)(iaes.next());
						String dicomNetworkConnectionName = ae.getDicomNetworkConnectionName();
						ae.setPresentationAddress((PresentationAddress)(mapOfDicomNetworkConnectionsForThisDevice.get(dicomNetworkConnectionName)));
						getNetworkApplicationInformation().add(dicomNetworkConnectionName,ae);	// not vDicomDeviceName as localName, since may be multiple
					}
					iaes = listOfApplicationEntitiesForThisDevice.iterator();
					while (iaes.hasNext()) {
						ApplicationEntity ae = (ApplicationEntity)(iaes.next());
						slf4jlogger.trace("\tApplicationEntity: {}",ae);
					}
				}
			}
		}
		catch (javax.naming.CommunicationException e) {
			slf4jlogger.debug("getNetworkConfiguration(): LDAP service not available (Could not contact server)", e);
		}
		catch (Exception e) {
			slf4jlogger.error("Ignoring exception", e);
		}
	}

	/**
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #NetworkConfigurationFromLDAP()} instead.
	 * @param	debugLevel	ignored
	 */
	public NetworkConfigurationFromLDAP(int debugLevel) {
		this();
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	public NetworkConfigurationFromLDAP() {
		super();
	}
	
	/**
	 * <p>Test method that periodically queries an LDAP server and dumps its contents periodically.</p>
	 *
	 * @param	arg	none
	 */
	public static void main(String arg[]) {
		NetworkConfigurationFromLDAP networkConfiguration = new NetworkConfigurationFromLDAP();
		//networkConfiguration.activateDiscovery(0);
		networkConfiguration.activateDiscovery(5000);
		//System.err.println(networkConfiguration.getNetworkApplicationInformation().toString());
		networkConfiguration.activateDumper(1000);
		Thread mainThread = Thread.currentThread();
		try {
			while (true) {
				mainThread.sleep(10000);
			}
		}
		catch (InterruptedException e) {
			networkConfiguration.close();
		}
	}
}


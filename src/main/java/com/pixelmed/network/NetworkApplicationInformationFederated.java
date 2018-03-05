/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.web.WebServerApplicationProperties;

import java.util.ArrayList;
import java.util.ListIterator; 
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class encapsulates information about DICOM network devices federated from multiple sources.</p>
 *
 * @author	dclunie
 */
public class NetworkApplicationInformationFederated extends NetworkApplicationInformation {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkApplicationInformationFederated.java,v 1.11 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NetworkApplicationInformationFederated.class);
	
	protected static final long RefreshFromSourcesInterval = 10*1000;	// milliseconds

	/**
	 * <p>Return the application entity map.</p>
	 *
	 * @return	the application entity map
	 */
	public ApplicationEntityMap getApplicationEntityMap() { update(); return super.getApplicationEntityMap(); }
	
	/**
	 * <p>Return the set of local names of application entities.</p>
	 *
	 * @return	the set of local names
	 */
	public Set getListOfLocalNamesOfApplicationEntities() { update(); return super.getListOfLocalNamesOfApplicationEntities(); }
	
	/**
	 * <p>Return the set of local names of application entities.</p>
	 *
	 * @return	the set of local names
	 */
	public Set getListOfApplicationEntityTitlesOfApplicationEntities() { update(); return super.getListOfApplicationEntityTitlesOfApplicationEntities(); }
	
	/**
	 * <p>Find the AET an application entity given its local name.</p>
	 *
	 * @param	localName	the local name of the AE
	 * @return			the AET, or null if none
	 */
	public String getApplicationEntityTitleFromLocalName(String localName) { update(); return super.getApplicationEntityTitleFromLocalName(localName); }
	
	/**
	 * <p>Find the local name of an application entity given its AET.</p>
	 *
	 * @param	aet	the application entity title
	 * @return		the local name, or null if none
	 */
	public String getLocalNameFromApplicationEntityTitle(String aet) { update(); return super.getLocalNameFromApplicationEntityTitle(aet); }

	protected ArrayList sources = null;
	protected long lastTimeUpdateRan = 0;
	
	protected synchronized void update() {
//System.err.println("NetworkApplicationInformationFederated.update():");
		if (System.currentTimeMillis() - lastTimeUpdateRan > RefreshFromSourcesInterval) {
//System.err.println("NetworkApplicationInformationFederated.update(): actually doing something");
			removeAll();
			ListIterator i = sources.listIterator();
			while (i.hasNext()) {
//System.err.println("NetworkApplicationInformationFederated.update(): looping on next source");
				NetworkConfigurationSource source = (NetworkConfigurationSource)(i.next());
//System.err.println("NetworkApplicationInformationFederated.update(): have source");
				NetworkApplicationInformation toAdd = source.getNetworkApplicationInformation();
//System.err.println("NetworkApplicationInformationFederated.update(): have information to add");
				addAll(toAdd);
//System.err.println("NetworkApplicationInformationFederated.update(): back from addAll");
			}
			lastTimeUpdateRan = System.currentTimeMillis();
		}
//System.err.println("NetworkApplicationInformationFederated.update(): done");
	}
	
	/**
	 * <p>Add a new source of network information.</p>
	 *
	 * @param	source	the source of network information
	 */
	public void addSource(NetworkConfigurationSource source) {
		if (sources == null) {
			sources = new ArrayList();
		}
		sources.add(source);
		lastTimeUpdateRan=0;	// forces refresh on next update()
	}
	
	/**
	 * <p>Remove all sources and all caches of network information.</p>
	 *
	 * <p>Includes unregistering any mDNS registered services.</p>
	 *
	 * <p>E.g., prior to changing properties and restarting.</p>
	 *
	 */
	public void removeAllSources() {
		if (sources != null) {
			ListIterator i = sources.listIterator();
			while (i.hasNext()) {
				NetworkConfigurationSource source = (NetworkConfigurationSource)(i.next());
				if (source instanceof NetworkConfigurationFromMulticastDNS) {
					((NetworkConfigurationFromMulticastDNS)source).unregisterAllServices();
				}
			}
		}
		sources = null;
		removeAll();
		lastTimeUpdateRan=0;	// forces refresh on next update()
	}
	
	/**
	 * <p>Startup all sources of network information and register oneself.</p>
	 *
	 * @param	properties	the pre-configured DICOM network properties, including information about other sources and self to register
	 */
	public void startupAllKnownSourcesAndRegister(NetworkApplicationProperties properties) {
		startupAllKnownSourcesAndRegister(properties,null);
	}
	
	/**
	 * <p>Startup all sources of network information and register oneself.</p>
	 *
	 * @param	networkApplicationProperties	the pre-configured DICOM network properties, including information about other sources and self to register
	 * @param	webServerApplicationProperties	the pre-configured web server network properties
	 */
	public void startupAllKnownSourcesAndRegister(NetworkApplicationProperties networkApplicationProperties,WebServerApplicationProperties webServerApplicationProperties) {
//System.err.println("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): try mDNS");
		NetworkConfigurationFromMulticastDNS networkConfigurationFromMulticastDNS = null;
		try {
			networkConfigurationFromMulticastDNS = new NetworkConfigurationFromMulticastDNS();
			networkConfigurationFromMulticastDNS.activateDiscovery();
			addSource(networkConfigurationFromMulticastDNS);
			if (networkApplicationProperties != null) {
				int port = networkApplicationProperties.getListeningPort();
				String calledAETitle = networkApplicationProperties.getCalledAETitle();
				String primaryDeviceType = networkApplicationProperties.getPrimaryDeviceType();
				networkConfigurationFromMulticastDNS.registerDicomService(calledAETitle,port,primaryDeviceType);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("Ignoring exception", e);
		}
//System.err.println("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): back from mDNS");
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("startupAllKnownSourcesAndRegister(): federatedNetworkApplicationInformation after DNS ...\n{}"+this.toString());
//System.err.println("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): try LDAP");
		try {
			NetworkConfigurationFromLDAP networkConfigurationFromLDAP = new NetworkConfigurationFromLDAP();
			networkConfigurationFromLDAP.activateDiscovery(5*60*1000);
			addSource(networkConfigurationFromLDAP);
		}
		catch (Exception e) {
			slf4jlogger.error("Ignoring exception", e);
		}
		slf4jlogger.trace("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): federatedNetworkApplicationInformation after LDAP ...\n{}",this.toString());
//System.err.println("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): try properties");
		if (networkApplicationProperties != null) {
			try {
				NetworkConfigurationSource networkConfigurationFromProperties = networkApplicationProperties.getNetworkConfigurationSource();
				addSource(networkConfigurationFromProperties);
			}
			catch (Exception e) {
				slf4jlogger.error("Ignoring exception", e);
			}
		}
		
		if (webServerApplicationProperties != null && networkConfigurationFromMulticastDNS != null) {
//System.err.println("NetworkApplicationInformationFederated.startupAllKnownSourcesAndRegister(): Registering WADO with MDNS");
			int port = webServerApplicationProperties.getListeningPort();
			String rootURL = webServerApplicationProperties.getRootURL();
			String instanceName = webServerApplicationProperties.getInstanceName();
			networkConfigurationFromMulticastDNS.registerWADOService(instanceName,port,rootURL);
		}
	}
}


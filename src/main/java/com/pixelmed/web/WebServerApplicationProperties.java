/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.network.NetworkDefaultValues;

import com.pixelmed.utils.FileUtilities;

import java.io.File; 
import java.io.IOException;
import java.util.Properties; 

/**
 * <p>This class provides common support to applications requiring properties related to web services.</p>
 *
 * <p>The following properties are supported:</p>
 *
 * <p><code>WebServer.ListeningPort</code> - the port that an association acceptor will listen on for incoming http connections</p>
 * <p><code>WebServer.RootURL</code> - the root of the URL by which this host is accessible (e.g., http://www.hostname.com:7091/)</p>
 * <p><code>WebServer.StylesheetPath</code></p>
 * <p><code>WebServer.InstanceNameForServiceAdvertising</code> - the name to use to advertise the service using DNS-SD (Bonjour)</p>
 * <p><code>WebServer.NumberOfWorkers</code> - the number of connection thread workers</p>
 *
 * @author	dclunie
 */
public class WebServerApplicationProperties {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/WebServerApplicationProperties.java,v 1.11 2017/01/24 10:50:53 dclunie Exp $";
	
	private static final String defaultRootURL = "";
	private static final String defaultStylesheetPath = "stylesheet.css";
	private static final String defaultRequestTypeToUseForInstances = "IMAGEDISPLAY";
	private static final String defaultNumberOfWorkers = "-1";

	private static final String propertyName_RootURL = "WebServer.RootURL";
	private static final String propertyName_StylesheetPath = "WebServer.StylesheetPath";
	private static final String propertyName_ListeningPort = "WebServer.ListeningPort";
	private static final String propertyName_RequestTypeToUseForInstances = "WebServer.RequestTypeToUseForInstances";
	private static final String propertyName_InstanceNameForServiceAdvertising = "WebServer.InstanceNameForServiceAdvertising";
	private static final String propertyName_NumberOfWorkers = "WebServer.NumberOfWorkers";
	
	private String rootURL;
	private String stylesheetPath;
	private String requestTypeToUseForInstances;
	private int port;
	private String instanceName;
	private int numberOfWorkers;

	/**
	 * <p>Create default properties.</p>
	 */
	public WebServerApplicationProperties() {
		rootURL = defaultRootURL;
		stylesheetPath = defaultStylesheetPath;
		requestTypeToUseForInstances = defaultRequestTypeToUseForInstances;
		port = NetworkDefaultValues.DefaultWADOPort;
		instanceName = NetworkDefaultValues.getDefaultDNSServiceInstanceName(port);
	}

	/**
	 * <p>Extract the properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public WebServerApplicationProperties(Properties properties) {
		rootURL=properties.getProperty(propertyName_RootURL);
		if (rootURL == null) {
			rootURL = defaultRootURL;
		}
		stylesheetPath=properties.getProperty(propertyName_StylesheetPath);
		if (stylesheetPath == null) {
			stylesheetPath = defaultStylesheetPath;
		}
		requestTypeToUseForInstances=properties.getProperty(propertyName_RequestTypeToUseForInstances);
		if (requestTypeToUseForInstances == null) {
			requestTypeToUseForInstances = defaultRequestTypeToUseForInstances;
		}
		port = Integer.valueOf(properties.getProperty(propertyName_ListeningPort,Integer.toString(NetworkDefaultValues.DefaultWADOPort))).intValue();
		instanceName=properties.getProperty(propertyName_InstanceNameForServiceAdvertising);
		if (instanceName == null) {
			instanceName = NetworkDefaultValues.getDefaultDNSServiceInstanceName(port);
		}
		numberOfWorkers = Integer.valueOf(properties.getProperty(propertyName_NumberOfWorkers,defaultNumberOfWorkers)).intValue();
	}
	
	/**
	 * <p>Return the root URL.</p>
	 *
	 * @return	the root URL
	 */
	public String getRootURL() { return rootURL; }
	
	/**
	 * <p>Return the stylesheet path.</p>
	 *
	 * @return	the stylesheet path
	 */
	public String getStylesheetPath() { return stylesheetPath; }
	
	/**
	 * <p>Return the request type to use for displaying instances.</p>
	 *
	 * @return	the request type to use for displaying instances
	 */
	public String getRequestTypeToUseForInstances() { return requestTypeToUseForInstances; }
	
	/**
	 * <p>Return the listening port.</p>
	 *
	 * @return	the listening port
	 */
	public int getListeningPort() { return port; }
	
	/**
	 * <p>Return the instance name for service sdvertising.</p>
	 *
	 * @return	the instance name
	 */
	public String getInstanceName() { return instanceName; }
	
	/**
	 * <p>Return the number of workers.</p>
	 *
	 * @return	the number of workers, or -1 if the property is not specified
	 */
	public int getNumberOfWorkers() { return numberOfWorkers; }
	
	
}


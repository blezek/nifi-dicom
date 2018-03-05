/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import java.io.IOException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Enumeration;
//import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class provides a dynamic registry of DICOM network parameters
 * possibly federated from various sources.</p>
 *
 * <p>Supported sources of information include:</p>
 * <ul>
 * <li>DNS Self-Discovery (aka. Apple's Bonjour)</li>
 * </ul>
 *
 * @author	dclunie
 */
public class NetworkConfigurationFromMulticastDNS extends NetworkConfigurationSource {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkConfigurationFromMulticastDNS.java,v 1.15 2017/01/24 10:50:45 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NetworkConfigurationFromMulticastDNS.class);
	
	private static final String DICOMServiceName = "_dicom._tcp.local.";
	private static final String ACRNEMAServiceName = "_acr-nema._tcp.local.";
	private static final String DICOMTLSServiceName = "_dicom-tls._tcp.local.";

	private static final String AETTXTRecordPropertyName = "AET";
	private static final String PrimaryDeviceTypeTXTRecordPropertyName = "PrimaryDeviceType";
	
	private static final String AETTXTRecordPropertyNameAsLowerCase = AETTXTRecordPropertyName.toLowerCase(java.util.Locale.US);
	private static final String PrimaryDeviceTypeTXTRecordPropertyNameAsLowerCase = PrimaryDeviceTypeTXTRecordPropertyName.toLowerCase(java.util.Locale.US);

	private static final String WADOServiceName = "_http._tcp.local.";
	private static final String WADOPathTXTRecordPropertyName = "path";

	protected JmDNS jmDNS;
	
	// we go to a lot of trouble here to allow the jmdns.jar file to be absent
	// if we don't use a proxy here, a ClassNotFoundException is thrown
	// on ServiceListener even if we don't get past the javax.jmdns.JmDNS
	// class loader ...
	
	//protected class OurJmDNSServiceListener implements ServiceListener {
	protected class OurJmDNSServiceListener implements InvocationHandler {
		// See "http://java.sun.com/products/jfc/tsc/articles/generic-listener2/"
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Class declaringClass = method.getDeclaringClass();
			slf4jlogger.trace("OurJmDNSServiceListener.invoke(): class = {}",declaringClass);
			String methodName = method.getName();
			slf4jlogger.trace("OurJmDNSServiceListener.invoke(): methodName = {}",methodName);
			if (declaringClass == javax.jmdns.ServiceListener.class)  {
				if (methodName.equals("serviceAdded"))  {
					serviceAdded((ServiceEvent)(args[0]));
				}
				else if (methodName.equals("serviceRemoved"))  {
					serviceRemoved((ServiceEvent)(args[0]));
				}
				else if (methodName.equals("serviceResolved"))  {
					serviceResolved((ServiceEvent)(args[0]));
				}
			}
            else {
				// (000610)
                if (methodName.equals("hashCode"))  {
                    return new Integer(System.identityHashCode(proxy));
                }
				else if (methodName.equals("equals")) {
                    return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
                }
				else if (methodName.equals("toString")) {
                    return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
                }
            }
			return null; 
		}
	 
		public void serviceAdded(ServiceEvent event) {
			String name = event.getName();
			slf4jlogger.trace("OurJmDNSServiceListener: Service added   : Name = {}",name);
			String type = event.getType();
			slf4jlogger.trace("OurJmDNSServiceListener: Service added   : Type = {}",type);
			ServiceInfo info = event.getInfo();
			slf4jlogger.trace("OurJmDNSServiceListener: Service added   : Info = {}",info);
			// Always request info, even if not null, since info may not be complete (000612)
			slf4jlogger.trace("OurJmDNSServiceListener: issuing request for info");
			//jmDNS.requestServiceInfo(type,name,10000/*timeout in ms*/);
			//jmDNS.requestServiceInfo(type,name);
			jmDNS.requestServiceInfo(type,name,true/*persistent*/);		// the persistent flag seems to be the key to actually getting anything back ! (000612)
			slf4jlogger.trace("OurJmDNSServiceListener: back from issuing request for info");
			// will come back with info later as serviceResolved() event
			// unless there really is no info, which is of course a problem :(
		}
		
		public void serviceRemoved(ServiceEvent event) {
			String name = event.getName();
			slf4jlogger.trace("OurJmDNSServiceListener: Service removed : Name = {}",name);
			String type = event.getType();
			slf4jlogger.trace("OurJmDNSServiceListener: Service removed : Type = {}",type);
			ServiceInfo info = event.getInfo();
			slf4jlogger.trace("OurJmDNSServiceListener: Service removed : Info = {}",info);
			getNetworkApplicationInformation().remove(name);
		}
		
		public void serviceResolved(ServiceEvent event) {
			String name = event.getName();
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: Name = {}",name);
			String type = event.getType();
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: Type = {}",type);
			ServiceInfo info = event.getInfo();
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: Info = {}",info);
			String aet = null;
			String primaryDeviceType = null;
			String hostname = null;
			int port = -1;
			if (info != null) {
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: Info getServer() = {}",info.getServer());
				hostname = info.getHostAddress();
				port = info.getPort();
				Enumeration propertyNames = info.getPropertyNames();
				while (propertyNames.hasMoreElements()) {
					String propertyName = (String)(propertyNames.nextElement());
					if (propertyName != null) {
						String propertyValue = info.getPropertyString(propertyName);
						String lowerCasePropertyName = propertyName.toLowerCase(java.util.Locale.US);
						if (lowerCasePropertyName.equals(AETTXTRecordPropertyNameAsLowerCase)) {
							aet = propertyValue;
						}
						else if (lowerCasePropertyName.equals(PrimaryDeviceTypeTXTRecordPropertyNameAsLowerCase)) {
							primaryDeviceType = propertyValue;
						}
						else {
							slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: Unrecognized property name = {} value = {}",propertyName,propertyValue);
						}
					}
				}
			}
			if (aet == null) {
				aet = name;	// AET property is optional and if absent default to service instance name
			}
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: hostname = {}",hostname);
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: port = {}",port);
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: aet = {}",aet);
			slf4jlogger.trace("OurJmDNSServiceListener: Service resolved: primaryDeviceType = {}",primaryDeviceType);
			if (name != null && name.length() > 0
			 && hostname != null && hostname.length() > 0
			 && aet != null && aet.length() > 0
			 && port != -1) {
				PresentationAddress presentationAddress = new PresentationAddress(hostname,port);
				ApplicationEntity ae = new ApplicationEntity(aet,presentationAddress,null/*queryModel*/,primaryDeviceType);
				try {
					getNetworkApplicationInformation().add(name,ae);
				}
				catch (DicomNetworkException e) {
					slf4jlogger.error("",e);
				}
			}
		}
	}
			
	/**
	 * <p>Start DNS Self-Discovery, if possible.</p>
	 *
	 * <p>Requires <code>javax.jmdns</code> package to be in class path.</p>
	 *
	 * @param	refreshInterval	is ignored completely, since DNS-SD over mDNS is asynchronous
	 */
	public void activateDiscovery(int refreshInterval) {
		slf4jlogger.trace("activateDNSSelfDiscovery():");
		try {
			Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("javax.jmdns.JmDNS");
			Class [] argTypes  = {};
			java.lang.reflect.Method methodToUse = classToUse.getDeclaredMethod("create",argTypes);		// (000611)
			Object[] argValues = {};
			jmDNS = (JmDNS)(methodToUse.invoke(null/*since static*/,argValues));
			slf4jlogger.trace("activateDNSSelfDiscovery(): created jmDNS = {}",jmDNS);

			//ServiceListener listener = new OurJmDNSServiceListener();
			InvocationHandler listenerInvocationHandler = new OurJmDNSServiceListener();
			ServiceListener listener = (ServiceListener) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                          new Class[] { ServiceListener.class },
                                          listenerInvocationHandler);
			
			jmDNS.addServiceListener(DICOMServiceName,listener);
			jmDNS.addServiceListener(ACRNEMAServiceName,listener);
			jmDNS.addServiceListener(DICOMTLSServiceName,listener);

		}
		catch (ClassNotFoundException e) {
			slf4jlogger.debug("activateDNSSelfDiscovery(): DNS Self Discovery not available (Could not load JmDNS class)",e);
			jmDNS = null;
		}
		catch (Exception e) {
		//catch (NoSuchMethodException e) {
		//catch (InstantiationException e) {
		//catch (IllegalAccessException e) {
			slf4jlogger.error("",e);
			jmDNS = null;
		}
	}
	
	/**
	 * <p>Stop DNS Self-Discovery.</p>
	 */
	public void deActivateDiscovery() {
		slf4jlogger.trace("deActivateDiscovery():");
		if (jmDNS != null) {
			jmDNS.unregisterAllServices();
			try {
				jmDNS.close();			// needed, since otherwise application will not exit when main thread finished
			} catch (Exception e) {		// (000609)
				slf4jlogger.error("",e);
				jmDNS = null;
			}
		}
	}
	
	/**
	 * <p>Construct an instance capable of handling dynamic configuration information but do not start anything yet.</p>
	 *
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #NetworkConfigurationFromMulticastDNS()} instead.
	 * @param	debugLevel	ignored
	 */
	public NetworkConfigurationFromMulticastDNS(int debugLevel) {
		this();
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Construct an instance capable of handling dynamic configuration information but do not start anything yet.</p>
	 */
	public NetworkConfigurationFromMulticastDNS() {
		super();
	}
	
	/**
	 * <p>Unregister all services that have been registered.</p>
	 *
	 */
	public void unregisterAllServices() {
		slf4jlogger.trace("unRegisterAllServices():");
		if (jmDNS != null) {
			jmDNS.unregisterAllServices();
		}
	}

	/**
	 * <p>Register a DICOM service on the local host.</p>
	 *
	 * @param	calledApplicationEntityTitle	the AET of the DICOM service
	 * @param	port				the port that the service listens on
	 * @param	primaryDeviceType		the primaryDeviceType, or null if none
	 */
	public void registerDicomService(String calledApplicationEntityTitle,int port,String primaryDeviceType) {
		slf4jlogger.trace("registerDicomService():");
		if (jmDNS != null) {
			Map<String,String> properties = new HashMap<String,String>();	// (000611)
			if (calledApplicationEntityTitle != null && calledApplicationEntityTitle.length() > 0) {
				properties.put(AETTXTRecordPropertyName,calledApplicationEntityTitle);
			}
			if (primaryDeviceType != null && primaryDeviceType.length() > 0) {
				properties.put(PrimaryDeviceTypeTXTRecordPropertyName,primaryDeviceType);
			}
			//ServiceInfo info = ServiceInfo.create(DICOMServiceName,calledApplicationEntityTitle,port,0/*weight*/,0/*priority*/,properties);
			try {
				Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("javax.jmdns.ServiceInfo");
				Class [] argTypes  = {String.class,String.class,java.lang.Integer.TYPE,java.lang.Integer.TYPE,java.lang.Integer.TYPE,Map.class};
				// (000611)
				java.lang.reflect.Method methodToUse = classToUse.getDeclaredMethod("create",argTypes);
				Object[] argValues = {DICOMServiceName,calledApplicationEntityTitle,port,0/*weight*/,0/*priority*/,properties};
				ServiceInfo info =  (ServiceInfo)(methodToUse.invoke(null/*since static*/,argValues));
				slf4jlogger.trace("registerDicomService(): created ServiceInfo = {}",info);
				jmDNS.registerService(info);
			}
			catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException,IOException
				slf4jlogger.error("",e);
			}
		}
	}

	/**
	 * <p>Register a WADO service on the local host.</p>
	 *
	 * @param	instanceName	the instance name for the service
	 * @param	port		the port that the service listens on
	 * @param	path		the path TXT parameter of the http service
	 */
	public void registerWADOService(String instanceName,int port,String path) {
//System.err.println("NetworkConfigurationFromMulticastDNS.registerWADOService():");
		if (jmDNS != null) {
			Map<String,String> properties = new HashMap<String,String>();	// (000611)
			if (path != null && path.length() > 0) {
				if (!path.startsWith("/")) {
					path="/"+path;
				}
				properties.put(WADOPathTXTRecordPropertyName,path);
			}
			//ServiceInfo info = ServiceInfo.create(WADOServiceName,instanceName,port,0/*weight*/,0/*priority*/,properties);
			try {
				Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("javax.jmdns.ServiceInfo");
				Class [] argTypes  = {String.class,String.class,java.lang.Integer.TYPE,java.lang.Integer.TYPE,java.lang.Integer.TYPE,Map.class};
				// (000611)
				java.lang.reflect.Method methodToUse = classToUse.getDeclaredMethod("create",argTypes);
				Object[] argValues = {WADOServiceName,instanceName,port,0/*weight*/,0/*priority*/,properties};
				ServiceInfo info =  (ServiceInfo)(methodToUse.invoke(null/*since static*/,argValues));
				slf4jlogger.trace("registerWADOService(): created ServiceInfo = {}",info);
				jmDNS.registerService(info);
			}
			catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException,IOException
				slf4jlogger.error("",e);
			}
		}
	}

	/**
	 * <p>Utility that activates a dynamic configuration listener and dumps its contents periodically.</p>
	 *
	 * <p>Additionally, will register a DICOM service on the local machine, if parameters of that service are supplied.</p>
	 *
	 * @param	arg	2 or 3 arguments if a service is to be registered, the AET of the DICOM service,the port that the service listens on, and optionally the primaryDeviceType
	 */
	public static void main(String arg[]) {
		NetworkConfigurationFromMulticastDNS networkConfiguration = new NetworkConfigurationFromMulticastDNS();
		networkConfiguration.activateDiscovery();
		if (arg.length > 1) {
			networkConfiguration.registerDicomService(arg[0],Integer.parseInt(arg[1]),arg.length > 2 ? arg[2] : null);
		}
		networkConfiguration.activateDumper();
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

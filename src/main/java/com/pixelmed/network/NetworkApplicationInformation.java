/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.Properties; 
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * <p>This class encapsulates information about DICOM network devices.</p>
 *
 * @author	dclunie
 */
public class NetworkApplicationInformation {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkApplicationInformation.java,v 1.17 2017/01/24 10:50:45 dclunie Exp $";

	public static final String resourceName_PublicStorageSCPs = "/com/pixelmed/network/publicstoragescps.properties";
	
	public static final String propertyName_DicomRemoteAEs = "Dicom.RemoteAEs";
	
	private static final String propertyNameSuffix_CalledAETitle = "CalledAETitle";
	private static final String propertyNameSuffix_HostNameOrIPAddress = "HostNameOrIPAddress";
	private static final String propertyNameSuffix_Port = "Port";
	private static final String propertyNameSuffix_QueryModel = "QueryModel";
	private static final String propertyNameSuffix_PrimaryDeviceType = "PrimaryDeviceType";

	private static final String propertyDelimitersForTokenizer_DicomRemoteAEs = ", ";

	private final ApplicationEntityMap applicationEntityMap;
	private final TreeMap localNameToApplicationEntityTitleMap;
	private final TreeMap applicationEntityTitleToLocalNameMap;
	
	public void addPublicStorageSCPs() throws IOException, DicomNetworkException {
		Properties publicStorageSCPsProperties = new Properties();
		InputStream publicStorageSCPsInputStream = NetworkApplicationProperties.class.getResourceAsStream(resourceName_PublicStorageSCPs);
		if (publicStorageSCPsInputStream != null) {
			publicStorageSCPsProperties.load(publicStorageSCPsInputStream);
			addAll(publicStorageSCPsProperties);
		}
	}

	/**
	 * <p>Construct an empty container for properties of DICOM network devices.</p>
	 */
	public NetworkApplicationInformation() {
		applicationEntityMap = new ApplicationEntityMap();
		localNameToApplicationEntityTitleMap = new TreeMap();
		applicationEntityTitleToLocalNameMap = new TreeMap();
	}
	
	/**
	 * <p>Extract the DICOM network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public NetworkApplicationInformation(Properties properties) throws DicomNetworkException {
		this();
		addAll(properties);
	}
	
	/**
	 * <p>Extract the DICOM network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public void addAll(Properties properties) throws DicomNetworkException {
		String remoteAEs = properties.getProperty(propertyName_DicomRemoteAEs);
		if (remoteAEs != null && remoteAEs.length() > 0) {
			StringTokenizer st = new StringTokenizer(remoteAEs,propertyDelimitersForTokenizer_DicomRemoteAEs);
			while (st.hasMoreTokens()) {
				String localName=st.nextToken();
				String calledAETitle = properties.getProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_CalledAETitle);
				String hostname = properties.getProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress);
				String ps = properties.getProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_Port);
				int port = ps == null ? -1 : Integer.parseInt(ps);
				String queryModel = properties.getProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_QueryModel);
				String primaryDeviceType = properties.getProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_PrimaryDeviceType);
//System.err.println(localName+": "
//	+"CalledAETitle <"+calledAETitle+"> "
//	+"HostNameOrIPAddress <"+hostname+"> "
//	+"Port <"+port+"> "
//	+"QueryModel <"+queryModel+"> "
//	+"PrimaryDeviceType <"+primaryDeviceType+"> "
//);
				add(localName,calledAETitle,hostname,port,queryModel,primaryDeviceType);
			}
		}
	}
	
	/**
	 * <p>Retrieve the DICOM network properties.</p>
	 *
	 * param	properties	the existing properties to add to (removing any properties already there), or null if none
	 *
	 * @return	the updated properties or a new set of properties if none supplied
	 */
	public Properties getProperties(Properties properties) {
//System.err.println("NetworkApplicationInformation.getProperties(): at start, properties = \n"+properties);
		if (properties == null) {
			properties = new Properties();
		}
		else {
			// need to remove any existing entries
			String remoteAEs = properties.getProperty(propertyName_DicomRemoteAEs);
			if (remoteAEs != null && remoteAEs.length() > 0) {
				StringTokenizer st = new StringTokenizer(remoteAEs,propertyDelimitersForTokenizer_DicomRemoteAEs);
				while (st.hasMoreTokens()) {
					String localName=st.nextToken();
					properties.remove(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_CalledAETitle);
					properties.remove(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress);
					properties.remove(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_Port);
					properties.remove(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_QueryModel);
					properties.remove(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_PrimaryDeviceType);
				}
			}
			properties.remove(propertyName_DicomRemoteAEs);
//System.err.println("NetworkApplicationInformation.getProperties(): after removing existing remote AEs, properties = \n"+properties);
		}
		
		{
			StringBuffer remoteAEs = new StringBuffer();
			String prefixForRemoteAEs = "";
			Iterator i = getListOfLocalNamesOfApplicationEntities().iterator();
			while (i.hasNext()) {
				String localName = (String)i.next();
				String applicationEntityTitle = getApplicationEntityTitleFromLocalName(localName);
				if (applicationEntityTitle != null) {
					ApplicationEntity ae = (ApplicationEntity)(applicationEntityMap.get(applicationEntityTitle));
					if (ae != null) {
						PresentationAddress address = ae.getPresentationAddress();

						properties.setProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_CalledAETitle,ae.getDicomAETitle());
						properties.setProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_HostNameOrIPAddress,address.getHostname());
						properties.setProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_Port,Integer.toString(address.getPort()));
						if (ae.getQueryModel() != null) {
							properties.setProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_QueryModel,ae.getQueryModel());
						}
						if (ae.getPrimaryDeviceType() != null) {
							properties.setProperty(propertyName_DicomRemoteAEs+"."+localName+"."+propertyNameSuffix_PrimaryDeviceType,ae.getPrimaryDeviceType());
						}
						
						remoteAEs.append(prefixForRemoteAEs);
						remoteAEs.append(localName);
						prefixForRemoteAEs = propertyDelimitersForTokenizer_DicomRemoteAEs;
					}
				}
			}
			properties.setProperty(propertyName_DicomRemoteAEs,remoteAEs.toString());
		}
//System.err.println("NetworkApplicationInformation.getProperties(): at end, properties = \n"+properties);

		return properties;
	}

	/**
	 * <p>Completely empty all information.</p>
	 */
	public void removeAll() {
//System.err.println("NetworkApplicationInformation.removeAll():");
		applicationEntityMap.clear();
		localNameToApplicationEntityTitleMap.clear();
		applicationEntityTitleToLocalNameMap.clear();
	}
	
	/**
	 * <p>Remove an AE.</p>
	 *
	 * @param	localName
	 */
	public void remove(String localName) {
		if (localName != null && localName.length() > 0) {
			String aeTitle = getApplicationEntityTitleFromLocalName(localName);
			localNameToApplicationEntityTitleMap.remove(localName);
			if (aeTitle != null && aeTitle.length() > 0) {
				applicationEntityTitleToLocalNameMap.remove(aeTitle);
				applicationEntityMap.remove(aeTitle);
			}
		}
	}
	
	/**
	 * <p>Add a new AE.</p>
	 *
	 * @param	localName
	 * @param	ae
	 * @throws	DicomNetworkException	if local name or AET already used, or either is null or empty
	 */
	public void add(String localName,ApplicationEntity ae) throws DicomNetworkException {
		String aeTitle = ae.getDicomAETitle();
		if (aeTitle != null && aeTitle.length() > 0) {
			if (applicationEntityMap.get(aeTitle) != null) {
				String localNameExisting = getLocalNameFromApplicationEntityTitle(aeTitle);
				if (localNameExisting != null && localName != null && !localName.equals(localNameExisting)) {
					throw new DicomNetworkException("Cannot use AET ["+aeTitle
						+"] for local name ["+localName+"] - already used for ["
						+localNameExisting+"]");
				}
			}
			applicationEntityMap.put(aeTitle,ae);
		}
		else {
			throw new DicomNetworkException("Cannot use empty AET ["+aeTitle+"]");
		}
		if (localName != null && localName.length() > 0 && aeTitle != null && aeTitle.length() > 0) {
			String aeTitleExisting = (String)(localNameToApplicationEntityTitleMap.get(localName));
			if (aeTitleExisting != null && aeTitleExisting.length() > 0) {
				ApplicationEntity aeExisting = (ApplicationEntity)(applicationEntityMap.get(aeTitle));
				if (aeExisting != null && !aeExisting.equals(ae)) {
					throw new DicomNetworkException("Cannot use local name ["+localName+"] - already used for AE ["+aeTitleExisting+"]");
				}
			}
			localNameToApplicationEntityTitleMap.put(localName,aeTitle);
			applicationEntityTitleToLocalNameMap.put(aeTitle,localName);
		}
		else {
			throw new DicomNetworkException("Cannot use empty AET ["+aeTitle+"] or empty local name ["+localName+"]");
		}
	}

	/**
	 * <p>Add a new AE.</p>
	 *
	 * @param	localName
	 * @param	aeTitle
	 * @param	port
	 * @param	queryModel		null if unknown
	 * @param	primaryDeviceType	null if unknown
	 * @throws	DicomNetworkException	if local name or AET already used, or either is null or empty
	 */
	public void add(String localName,String aeTitle,String hostname,int port,String queryModel,String primaryDeviceType) throws DicomNetworkException {
//System.err.println("NetworkApplicationInformation.add("+localName+","+aeTitle+","+hostname+","+port+","+queryModel+","+primaryDeviceType+")");
		if (aeTitle != null && aeTitle.length() > 0
		 && hostname != null && hostname.length() > 0) {
			// query model may be null
			if (applicationEntityMap.get(aeTitle) != null) {
				throw new DicomNetworkException("Cannot use AET ["+aeTitle
					+"] for local name ["+localName+"] - already used for ["
					+getLocalNameFromApplicationEntityTitle(aeTitle)+"]");
			}
			applicationEntityMap.put(aeTitle,new PresentationAddress(hostname,port),queryModel,primaryDeviceType);
		}
		else {
			throw new DicomNetworkException("Cannot use empty AET ["+aeTitle+"] or hostname ["+hostname+"] or port ["+port+"]");
		}
		if (localName != null && localName.length() > 0
		 && aeTitle != null && aeTitle.length() > 0) {
			if (localNameToApplicationEntityTitleMap.get(localName) != null) {
				throw new DicomNetworkException("Cannot use local name ["+localName+"] - already used");
			}
			localNameToApplicationEntityTitleMap.put(localName,aeTitle);
			applicationEntityTitleToLocalNameMap.put(aeTitle,localName);
		}
		else {
			throw new DicomNetworkException("Cannot use empty AET ["+aeTitle+"] or empty local name ["+localName+"]");
		}
	}
	
	/**
	 * <p>Add all the entries in the supplied map except any that are already present.</p>
	 *
	 * @param	infoToAdd	the information to add
	 */
	public void addAll(NetworkApplicationInformation infoToAdd) {
//System.err.println("NetworkApplicationInformation.addAll():");
		if (infoToAdd != null) {
			Set localNamesToAdd = infoToAdd.getListOfLocalNamesOfApplicationEntities();
			ApplicationEntityMap aesToAdd = infoToAdd.getApplicationEntityMap();
			if (localNamesToAdd != null && aesToAdd != null) {
				Iterator i = localNamesToAdd.iterator();
				while (i.hasNext()) {
					String localName = (String)(i.next());
					if (localName != null && localName.length() > 0) {
//System.err.println("NetworkApplicationInformation.addAll(): interating on localName = "+localName);
						String aeTitle = infoToAdd.getApplicationEntityTitleFromLocalName(localName);
						if (aeTitle != null && aeTitle.length() > 0) {
//System.err.println("NetworkApplicationInformation.addAll(): aeTitle = "+aeTitle);
							// Do not use add(localName,ae) ... causes infinite loop
							if (localNameToApplicationEntityTitleMap.get(localName) == null
							 && applicationEntityMap.get(aeTitle) == null) {
//System.err.println("NetworkApplicationInformation.addAll(): adding new entry ");
								ApplicationEntity ae = (ApplicationEntity)(aesToAdd.get(aeTitle));
								applicationEntityMap.put(aeTitle,ae);
								localNameToApplicationEntityTitleMap.put(localName,aeTitle);
								applicationEntityTitleToLocalNameMap.put(aeTitle,localName);
//System.err.println("NetworkApplicationInformation.addAll(): done adding new entry ");
							}
							else {
//System.err.println("NetworkApplicationInformation.addAll(): already have localName or aeTitle");
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <p>Return the application entity map.</p>
	 *
	 * @return	the application entity map
	 */
	public ApplicationEntityMap getApplicationEntityMap() { return applicationEntityMap; }
	
	/**
	 * <p>Return the set of local names of application entities.</p>
	 *
	 * @return	the set of local names
	 */
	public Set getListOfLocalNamesOfApplicationEntities() { return localNameToApplicationEntityTitleMap.keySet(); }
	
	/**
	 * <p>Return the set of local names of application entities.</p>
	 *
	 * @return	the set of local names
	 */
	public Set getListOfApplicationEntityTitlesOfApplicationEntities() { return applicationEntityTitleToLocalNameMap.keySet(); }
	
	/**
	 * <p>Find the AET an application entity given its local name.</p>
	 *
	 * @param	localName	the local name of the AE
	 * @return			the AET, or null if none
	 */
	public String getApplicationEntityTitleFromLocalName(String localName) { return (String)(localNameToApplicationEntityTitleMap.get(localName)); }
	
	/**
	 * <p>Find the local name of an application entity given its AET.</p>
	 *
	 * @param	aet	the application entity title
	 * @return		the local name, or null if none
	 */
	public String getLocalNameFromApplicationEntityTitle(String aet) { return (String)(applicationEntityTitleToLocalNameMap.get(aet)); }
	
	/**
	 * <p>Make an LDAP LDIF representation of the network information.</p>
	 *
	 * @param	rootDN	the root distinguished name to attach the DICOM configuration information below
	 * @return		a String containing the text of the LDIF representation, suitable for feeding into a utility like <code>ldapadd</code>
	 */
	public String getLDIFRepresentation(String rootDN) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		
		printWriter.println("version: 1");
		printWriter.println("");
		printWriter.println("dn: cn=DICOM Configuration,"+rootDN);
		printWriter.println("objectClass: dicomConfigurationRoot");
		printWriter.println("cn: DICOM Configuration");
		printWriter.println("");
		printWriter.println("dn: cn=Devices,cn=DICOM Configuration,"+rootDN);
		printWriter.println("objectClass: dicomDevicesRoot");
		printWriter.println("cn: Devices");
		printWriter.println("");
		printWriter.println("dn: cn=Unique AE Titles Registry,cn=DICOM Configuration,"+rootDN);
		printWriter.println("objectClass: dicomUniqueAETitlesRegistryRoot");
		printWriter.println("cn: Unique AE Titles Registry");
		printWriter.println("");
		
		ApplicationEntityMap applicationEntityMap = getApplicationEntityMap();

		if (localNameToApplicationEntityTitleMap != null) {
			Iterator i = getListOfLocalNamesOfApplicationEntities().iterator();
			while (i.hasNext()) {
				String dicomDeviceName = (String)i.next();	// use local name as dicomDeviceName
				printWriter.println("dn: dicomDeviceName="+dicomDeviceName+",cn=Devices,cn=DICOM Configuration,"+rootDN);
				printWriter.println("objectClass: dicomDevice");
				printWriter.println("dicomDeviceName: "+dicomDeviceName);
				printWriter.println("dicomInstalled: TRUE");
				printWriter.println("");
								
				String applicationEntityTitle = getApplicationEntityTitleFromLocalName(dicomDeviceName);
				PresentationAddress presentationAddress = applicationEntityMap.getPresentationAddress(applicationEntityTitle);
				String hostname = presentationAddress.getHostname();
				int port = presentationAddress.getPort();
				String dicomAssociationAcceptor = "TRUE";	// we do not really know this
				String dicomAssociationInitiator = "TRUE";	// we do not really know this
				
				// add two children for each device, the dicomNetworkConnection and the dicomAETitle
				
				//String cnForDicomNetworkConnection = hostname + "_" + port;
				String cnForDicomNetworkConnection = dicomDeviceName;	// use local name here as well, since 1:1 correspondence in our internal model
				String dnForDicomNetworkConnection = "cn="+cnForDicomNetworkConnection +",dicomDeviceName="+dicomDeviceName+",cn=Devices,cn=DICOM Configuration,"+rootDN;

				printWriter.println("dn: "+dnForDicomNetworkConnection);
				printWriter.println("objectClass: dicomNetworkConnection");
				printWriter.println("cn: "+cnForDicomNetworkConnection);
				printWriter.println("dicomHostname: "+hostname);
				printWriter.println("dicomPort: "+port);
				printWriter.println("");
				printWriter.println("dn: dicomAETitle="+applicationEntityTitle+",dicomDeviceName="+dicomDeviceName+",cn=Devices,cn=DICOM Configuration,"+rootDN);
				printWriter.println("objectClass: dicomNetworkAE");
				printWriter.println("dicomAETitle: "+applicationEntityTitle);
				//printWriter.println("dicomNetworkConnectionReference: "+cnForDicomNetworkConnection);
				printWriter.println("dicomNetworkConnectionReference: "+dnForDicomNetworkConnection);
				printWriter.println("dicomAssociationAcceptor: TRUE");
				printWriter.println("dicomAssociationInitiator: TRUE");
				printWriter.println("");
				
				// also add AE to Unique AE Titles Registry
				printWriter.println("dn: dicomAETitle="+applicationEntityTitle+",cn=Unique AE Titles Registry,cn=DICOM Configuration,"+rootDN);
				printWriter.println("objectClass: dicomUniqueAETitle");
				printWriter.println("dicomAETitle: "+applicationEntityTitle);
				printWriter.println("");
			}
		}
				
		printWriter.close();
		return stringWriter.toString();
	}
	
	/**
	 */
	public String toString() {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		ApplicationEntityMap applicationEntityMap = getApplicationEntityMap();
		if (localNameToApplicationEntityTitleMap != null) {
			Iterator i = getListOfLocalNamesOfApplicationEntities().iterator();
			while (i.hasNext()) {
				String localName = (String)i.next();
				String applicationEntityTitle = getApplicationEntityTitleFromLocalName(localName);
				ApplicationEntity ae = applicationEntityTitle == null ? null : (ApplicationEntity)(applicationEntityMap.get(applicationEntityTitle));
				printWriter.println("localName="+localName+","+(ae == null ? "-null-" : ae.toString()));
			}
		}
		printWriter.close();
		return stringWriter.toString();
	}
}


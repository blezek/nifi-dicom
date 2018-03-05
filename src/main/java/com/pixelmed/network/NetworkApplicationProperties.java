/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Iterator; 
import java.util.Properties; 
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>This class provides common support to applications requiring properties related to DICOM network services.</p>
 *
 * <p>Also contains a main method that can be used, for example, to convert information previously statically configured by properties on each
 * device, to assemble LDIF files to be loaded into an LDAP server for use via the DICOM Network Configuration
 * Management service.</p>
 *
 * <p>The following properties are supported:</p>
 *
 * <p><code>Dicom.ListeningPort</code> - the port that an association acceptor will listen on for incoming connections</p>
 * <p><code>Dicom.CalledAETitle</code> - what the AE expects to be called when accepting an association</p>
 * <p><code>Dicom.CallingAETitle</code> - what the AE will call itself when initiating an association</p>
 * <p><code>Dicom.PrimaryDeviceType</code> - what our own primary device type is</p>
 * <p><code>Dicom.StorageSCUCompressionLevel</code> - determines what types of compressed Transfer Syntaxes are proposed by a Storage SCU; 0 = uncompressed transfer syntaxes only; 1 = propose deflate as well; 2 = propose deflate and bzip2 (if bzip2 codec is available)</p>
 * <p><code>Dicom.AcceptorMaximumLengthReceived</code> - the maximum PDU length that an association acceptor will offer to receive (0 for no maximum)</p>
 * <p><code>Dicom.AcceptorSocketReceiveBufferSize</code> - the TCP socket receive buffer size to set for incoming connections (or 0 to leave unchanged and use platform default)</p>
 * <p><code>Dicom.AcceptorSocketSendBufferSize</code> - the TCP socket send buffer size to set for incoming connections (or 0 to leave unchanged and use platform default)</p>
 * <p><code>Dicom.InitiatorMaximumLengthReceived</code> - the maximum PDU length that an association initiator will offer to receive (0 for no maximum)</p>
 * <p><code>Dicom.InitiatorSocketReceiveBufferSize</code> - the TCP socket receive buffer size to set for incoming connections (or 0 to leave unchanged and use platform default)</p>
 * <p><code>Dicom.InitiatorSocketSendBufferSize</code> - the TCP socket send buffer size to set for incoming connections (or 0 to leave unchanged and use platform default)</p>
 * <p><code>Dicom.RemoteAEs</code> - a space or comma separated list of the local names all the available remote AEs; each local name may be anything unique (in this file) without a space or comma; the local name does not need to be the same as the remote AE's called AE title</p>
 * <p><code>Dicom.XXXX.CalledAETitle</code> - for the remote AE with local name XXXX, what that AE expects to be called when accepting an association</p>
 * <p><code>Dicom.XXXX.HostNameOrIPAddress</code> - for the remote AE with local name XXXX, what hostname or IP addess that AE will listen on for incoming connections</p>
 * <p><code>Dicom.XXXX.Port</code> - for the remote AE with local name XXXX, what port that AE will listen on for incoming connections</p>
 * <p><code>Dicom.XXXX.QueryModel</code> - for the remote AE with local name XXXX, what query model is supported; values are STUDYROOT or PATIENTROOT; leave absent if query/retrieve not supported by the remote AE</p>
 * <p><code>Dicom.XXXX.PrimaryDeviceType</code> - for the remote AE with local name XXXX, what the primary device type is (see DICOM PS 3.15 and PS 3.16)</p>
 *
 * @author	dclunie
 */
public class NetworkApplicationProperties {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkApplicationProperties.java,v 1.23 2017/01/24 10:50:45 dclunie Exp $";

	public static final String propertyName_DicomListeningPort = "Dicom.ListeningPort";
	public static final String propertyName_DicomCalledAETitle = "Dicom.CalledAETitle";
	public static final String propertyName_DicomCallingAETitle = "Dicom.CallingAETitle";
	public static final String propertyName_PrimaryDeviceType = "Dicom.PrimaryDeviceType";		// "WSD","ARCHIVE"

	public static final String propertyName_DicomAcceptorMaximumLengthReceived = "Dicom.AcceptorMaximumLengthReceived";
	public static final String propertyName_DicomAcceptorSocketReceiveBufferSize = "Dicom.AcceptorSocketReceiveBufferSize";
	public static final String propertyName_DicomAcceptorSocketSendBufferSize = "Dicom.AcceptorSocketSendBufferSize";

	public static final String propertyName_DicomInitiatorMaximumLengthReceived = "Dicom.InitiatorMaximumLengthReceived";
	public static final String propertyName_DicomInitiatorSocketReceiveBufferSize = "Dicom.InitiatorSocketReceiveBufferSize";
	public static final String propertyName_DicomInitiatorSocketSendBufferSize = "Dicom.InitiatorSocketSendBufferSize";
	
	public static final String StudyRootQueryModel = "STUDYROOT";
	public static final String PatientRootQueryModel = "PATIENTROOT";
	public static final String PatientStudyOnlyQueryModel = "PATIENTSTUDYONLY";
	
	/**
	 * <p>Is the model Study Root ?</p>
	 *
	 * @param	model	the string value describing the model, as used in the query model remote AE property
	 * @return		true if Study Root
	 */
	public static final boolean isStudyRootQueryModel(String model) { return model != null && model.equals(StudyRootQueryModel); }
	
	/**
	 * <p>Is the model Patient Root ?</p>
	 *
	 * @param	model	the string value describing the model, as used in the query model remote AE property
	 * @return		true if Patient Root
	 */
	public static final boolean isPatientRootQueryModel(String model) { return model != null && model.equals(PatientRootQueryModel); }
	
	/**
	 * <p>Is the model Patient/Study Only ?</p>
	 *
	 * @param	model	the string value describing the model, as used in the query model remote AE property
	 * @return		true if Patient/Study Only
	 */
	public static final boolean isPatientStudyOnlyQueryModel(String model) { return model != null && model.equals(PatientStudyOnlyQueryModel); }
	
	public static final String propertyName_StorageSCUCompressionLevel = "Dicom.StorageSCUCompressionLevel";

	private int port;
	private String calledAETitle;
	private String callingAETitle;
	private String primaryDeviceType;
	private int storageSCUCompressionLevel;
	private int acceptorMaximumLengthReceived;
	private int acceptorSocketReceiveBufferSize;
	private int acceptorSocketSendBufferSize;
	private int initiatorMaximumLengthReceived;
	private int initiatorSocketReceiveBufferSize;
	private int initiatorSocketSendBufferSize;
	private NetworkApplicationInformation networkApplicationInformation;

	/**
	 * <p>Create default properties.</p>
	 */
	public NetworkApplicationProperties() throws DicomNetworkException {
//System.err.println("NetworkApplicationProperties():");
		port = NetworkDefaultValues.StandardDicomReservedPortNumber;
		calledAETitle = NetworkDefaultValues.getDefaultApplicationEntityTitle(port);
		callingAETitle = calledAETitle;
		primaryDeviceType = NetworkDefaultValues.getDefaultPrimaryDeviceType();
		storageSCUCompressionLevel = 0;
		acceptorMaximumLengthReceived    = AssociationFactory.getDefaultMaximumLengthReceived();
		acceptorSocketReceiveBufferSize  = AssociationFactory.getDefaultReceiveBufferSize();
		acceptorSocketSendBufferSize     = AssociationFactory.getDefaultSendBufferSize();
		initiatorMaximumLengthReceived   = AssociationFactory.getDefaultMaximumLengthReceived();
		initiatorSocketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
		initiatorSocketSendBufferSize    = AssociationFactory.getDefaultSendBufferSize();
		networkApplicationInformation = new NetworkApplicationInformation();
	}
	
	/**
	 * <p>Extract the DICOM network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 */
	public NetworkApplicationProperties(Properties properties) throws DicomNetworkException, IOException {
		this(properties,false/*addPublicStorageSCPsIfNoRemoteAEsConfigured*/);
	}
	
	/**
	 * <p>Extract the DICOM network properties from the supplied properties.</p>
	 *
	 * @param	properties
	 * @param	addPublicStorageSCPsIfNoRemoteAEsConfigured
	 */
	public NetworkApplicationProperties(Properties properties,boolean addPublicStorageSCPsIfNoRemoteAEsConfigured) throws DicomNetworkException, IOException {
//System.err.println("NetworkApplicationProperties(Properties): properties ="+properties);
		String portString=properties.getProperty(propertyName_DicomListeningPort);
		if (portString == null || portString.length() == 0) {
			port=NetworkDefaultValues.StandardDicomReservedPortNumber;
		}
		else {
			port=Integer.parseInt(portString);
		}
		calledAETitle=properties.getProperty(propertyName_DicomCalledAETitle);
		if (calledAETitle == null || calledAETitle.length() == 0) {
			calledAETitle=NetworkDefaultValues.getDefaultApplicationEntityTitle(port);
		}
		callingAETitle=properties.getProperty(propertyName_DicomCallingAETitle);
		if (callingAETitle == null || callingAETitle.length() == 0) {
			callingAETitle=calledAETitle;
		}
		
		primaryDeviceType = properties.getProperty(propertyName_PrimaryDeviceType);
		
		storageSCUCompressionLevel = Integer.valueOf(properties.getProperty(propertyName_StorageSCUCompressionLevel,"0")).intValue();

		acceptorMaximumLengthReceived    = Integer.valueOf(properties.getProperty(propertyName_DicomAcceptorMaximumLengthReceived,   Integer.toString(AssociationFactory.getDefaultMaximumLengthReceived()))).intValue();
		acceptorSocketReceiveBufferSize  = Integer.valueOf(properties.getProperty(propertyName_DicomAcceptorSocketReceiveBufferSize, Integer.toString(AssociationFactory.getDefaultReceiveBufferSize()))).intValue();
		acceptorSocketSendBufferSize     = Integer.valueOf(properties.getProperty(propertyName_DicomAcceptorSocketSendBufferSize,    Integer.toString(AssociationFactory.getDefaultSendBufferSize()))).intValue();
		initiatorMaximumLengthReceived   = Integer.valueOf(properties.getProperty(propertyName_DicomInitiatorMaximumLengthReceived,  Integer.toString(AssociationFactory.getDefaultMaximumLengthReceived()))).intValue();
		initiatorSocketReceiveBufferSize = Integer.valueOf(properties.getProperty(propertyName_DicomInitiatorSocketReceiveBufferSize,Integer.toString(AssociationFactory.getDefaultReceiveBufferSize()))).intValue();
		initiatorSocketSendBufferSize    = Integer.valueOf(properties.getProperty(propertyName_DicomInitiatorSocketSendBufferSize,   Integer.toString(AssociationFactory.getDefaultSendBufferSize()))).intValue();
		
		networkApplicationInformation = new NetworkApplicationInformation(properties);
		
		if (addPublicStorageSCPsIfNoRemoteAEsConfigured) {
			Set<String> aets = networkApplicationInformation.getListOfApplicationEntityTitlesOfApplicationEntities();
			if (aets == null || aets.size() == 0 || (aets.size() == 1 && aets.contains(calledAETitle))) {
				// nothing, or just ourselves
				networkApplicationInformation.addPublicStorageSCPs();
			}
		}
	}
	
	/**
	 * <p>Retrieve the DICOM network properties.</p>
	 *
	 * param	properties	the existing properties to add to (replacing corresponding properties already there), or null if none
	 *
	 * @return	the updated properties or a new set of properties if none supplied
	 */
	public Properties getProperties(Properties properties) {
//System.err.println("NetworkApplicationProperties.getProperties(): at start, properties = \n"+properties);
		if (properties == null) {
			properties = new Properties();
		}
		
		properties.setProperty(propertyName_DicomListeningPort,Integer.toString(port));
		properties.setProperty(propertyName_DicomCalledAETitle,calledAETitle);
		properties.setProperty(propertyName_DicomCallingAETitle,callingAETitle);
		properties.setProperty(propertyName_StorageSCUCompressionLevel,Integer.toString(storageSCUCompressionLevel));
		properties.setProperty(propertyName_DicomAcceptorMaximumLengthReceived,Integer.toString(acceptorMaximumLengthReceived));
		properties.setProperty(propertyName_DicomAcceptorSocketReceiveBufferSize,Integer.toString(acceptorSocketReceiveBufferSize));
		properties.setProperty(propertyName_DicomAcceptorSocketSendBufferSize,Integer.toString(acceptorSocketSendBufferSize));
		properties.setProperty(propertyName_DicomInitiatorMaximumLengthReceived,Integer.toString(initiatorMaximumLengthReceived));
		properties.setProperty(propertyName_DicomInitiatorSocketReceiveBufferSize,Integer.toString(initiatorSocketReceiveBufferSize));
		properties.setProperty(propertyName_DicomInitiatorSocketSendBufferSize,Integer.toString(initiatorSocketSendBufferSize));
		
		networkApplicationInformation.getProperties(properties);	// remove any existing entries in properties, and add properties for all in  networkApplicationInformation

//System.err.println("NetworkApplicationProperties.getProperties(): at end, properties = \n"+properties);
		return properties;
	}

	/**
	 * <p>Return the listening port.</p>
	 *
	 * @return	the listening port
	 */
	public int getListeningPort() { return port; }
	
	/**
	 * <p>Set the listening port.</p>
	 *
	 * param		port	the listening port
	 */
	public void setListeningPort(int port) { this.port = port; }
	
	/**
	 * <p>Return the called AET.</p>
	 *
	 * @return	the called AET
	 */
	public String getCalledAETitle() { return calledAETitle; }
	
	/**
	 * <p>Set the called AET.</p>
	 *
	 * param	calledAETitle	the called AET
	 */
	public void setCalledAETitle(String calledAETitle) { this.calledAETitle = calledAETitle; }
	
	/**
	 * <p>Return the calling AET.</p>
	 *
	 * @return	the calling AET
	 */
	public String getCallingAETitle() { return callingAETitle; }
	
	/**
	 * <p>Set the calling AET.</p>
	 *
	 * param	callingAETitle	the calling AET
	 */
	public void setCallingAETitle(String callingAETitle) { this.callingAETitle = callingAETitle; }
	
	/**
	 * <p>Return the primary device type.</p>
	 *
	 * @return	the primary device type
	 */
	public String getPrimaryDeviceType() { return primaryDeviceType; }
	
	/**
	 * <p>Set the primary device type.</p>
	 *
	 * param	primaryDeviceType	the primary device type
	 */
	public void setPrimaryDeviceType(String primaryDeviceType) { this.primaryDeviceType = primaryDeviceType; }
	
	/**
	 * <p>Return the storage SCU compression level.</p>
	 *
	 * @return	the storage SCU compression level
	 */
	public int getStorageSCUCompressionLevel() { return storageSCUCompressionLevel; }
	
	/**
	 * <p>Return the Maximum Length Received for the Association Acceptor.</p>
	 *
	 * @return	the Maximum Length Received or the Association Acceptor (0 if unlimited)
	 */
	public int getAcceptorMaximumLengthReceived() { return acceptorMaximumLengthReceived; }

	/**
	 * <p>Set the PDU Maximum Length Received for the Association Acceptor.</p>
	 *
	 * param	acceptorMaximumLengthReceived	the PDU Maximum Length Received for the Association Acceptor (0 if unlimited)
	 */
	public void setAcceptorMaximumLengthReceived(int acceptorMaximumLengthReceived) { this.acceptorMaximumLengthReceived = acceptorMaximumLengthReceived; }
	
	/**
	 * <p>Return the TCP socket receive buffer size for the Association Acceptor.</p>
	 *
	 * @return	the TCP socket receive buffer size for the Association Acceptor (0 to leave unchanged and use platform default)
	 */
	public int getAcceptorSocketReceiveBufferSize() { return acceptorSocketReceiveBufferSize; }

	/**
	 * <p>Set the TCP socket receive buffer size for the Association Acceptor.</p>
	 *
	 * param	acceptorSocketReceiveBufferSize	the TCP socket receive buffer size for the Association Acceptor (0 to leave unchanged and use platform default)
	 */
	public void setAcceptorSocketReceiveBufferSize(int acceptorSocketReceiveBufferSize) { this.acceptorSocketReceiveBufferSize = acceptorSocketReceiveBufferSize; }
	
	/**
	 * <p>Return the TCP socket send buffer size for the Association Acceptor.</p>
	 *
	 * @return	the TCP socket send buffer size for the Association Acceptor (0 to leave unchanged and use platform default)
	 */
	public int getAcceptorSocketSendBufferSize() { return acceptorSocketSendBufferSize; }

	/**
	 * <p>Set the TCP socket send buffer size for the Association Acceptor.</p>
	 *
	 * param	acceptorSocketSendBufferSize	the TCP socket send buffer size for the Association Acceptor (0 to leave unchanged and use platform default)
	 */
	public void setAcceptorSocketSendBufferSize(int acceptorSocketSendBufferSize) { this.acceptorSocketSendBufferSize = acceptorSocketSendBufferSize; }
	
	/**
	 * <p>Return the Maximum Length Received for the Association Initiator.</p>
	 *
	 * @return	the Maximum Length Received or the Association Initiator (0 if unlimited)
	 */
	public int getInitiatorMaximumLengthReceived() { return initiatorMaximumLengthReceived; }

	/**
	 * <p>Set the PDU Maximum Length Received for the Association Initiator.</p>
	 *
	 * param	initiatorMaximumLengthReceived	the PDU Maximum Length Received for the Association Initiator (0 if unlimited)
	 */
	public void setInitiatorMaximumLengthReceived(int initiatorMaximumLengthReceived) { this.initiatorMaximumLengthReceived = initiatorMaximumLengthReceived; }
	
	/**
	 * <p>Return the TCP socket receive buffer size for the Association Initiator.</p>
	 *
	 * @return	the TCP socket receive buffer size for the Association Initiator (0 to leave unchanged and use platform default)
	 */
	public int getInitiatorSocketReceiveBufferSize() { return initiatorSocketReceiveBufferSize; }

	/**
	 * <p>Set the TCP socket receive buffer size for the Association Initiator.</p>
	 *
	 * param	initiatorSocketReceiveBufferSize	the TCP socket receive buffer size for the Association Initiator (0 to leave unchanged and use platform default)
	 */
	public void setInitiatorSocketReceiveBufferSize(int initiatorSocketReceiveBufferSize) { this.initiatorSocketReceiveBufferSize = initiatorSocketReceiveBufferSize; }
	
	/**
	 * <p>Return the TCP socket send buffer size for the Association Initiator.</p>
	 *
	 * @return	the TCP socket send buffer size for the Association Initiator (0 to leave unchanged and use platform default)
	 */
	public int getInitiatorSocketSendBufferSize() { return initiatorSocketSendBufferSize; }

	/**
	 * <p>Set the TCP socket send buffer size for the Association Initiator.</p>
	 *
	 * param	initiatorSocketSendBufferSize	the TCP socket send buffer size for the Association Initiator (0 to leave unchanged and use platform default)
	 */
	public void setInitiatorSocketSendBufferSize(int initiatorSocketSendBufferSize) { this.initiatorSocketSendBufferSize = initiatorSocketSendBufferSize; }
	
	/**
	 * <p>Return the network application information.</p>
	 *
	 * @return	the network application information
	 */
	public NetworkApplicationInformation getNetworkApplicationInformation() { return networkApplicationInformation; }
	
	protected class OurNetworkConfigurationSource extends NetworkConfigurationSource {
		private NetworkApplicationInformation ourNetworkApplicationInformation;
		OurNetworkConfigurationSource(NetworkApplicationInformation networkApplicationInformation) {
			super();
			ourNetworkApplicationInformation = networkApplicationInformation;
		}
		public synchronized NetworkApplicationInformation getNetworkApplicationInformation() {
			return ourNetworkApplicationInformation;
		}
		public void activateDiscovery(int refreshInterval) {}
		public void deActivateDiscovery() {}
	}
	
	protected NetworkConfigurationSource networkConfigurationSource = null;

	/**
	 * <p>Return a network configuration source that will supply the network application information.</p>
	 *
	 * @return	the network configuration source
	 */
	public NetworkConfigurationSource getNetworkConfigurationSource() {
		if (networkConfigurationSource == null) {
			networkConfigurationSource = new OurNetworkConfigurationSource(networkApplicationInformation);
		}
		return networkConfigurationSource;
	}
	
	/**
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Our port: "+port+"\n");
		str.append("Our calledAETitle: "+calledAETitle+"\n");
		str.append("Our callingAETitle: "+callingAETitle+"\n");
		str.append("Our primaryDeviceType: "+primaryDeviceType+"\n");
		str.append("storageSCUCompressionLevel: "+storageSCUCompressionLevel+"\n");
		str.append("acceptorMaximumLengthReceived: "+acceptorMaximumLengthReceived+"\n");
		str.append("acceptorSocketReceiveBufferSize: "+acceptorSocketReceiveBufferSize+"\n");
		str.append("acceptorSocketSendBufferSize: "+acceptorSocketSendBufferSize+"\n");
		str.append("initiatorMaximumLengthReceived: "+initiatorMaximumLengthReceived+"\n");
		str.append("initiatorSocketReceiveBufferSize: "+initiatorSocketReceiveBufferSize+"\n");
		str.append("initiatorSocketSendBufferSize: "+initiatorSocketSendBufferSize+"\n");
		str.append("Remote applications:\n"+networkApplicationInformation+"\n");
		
		return str.toString();
	}

	/**
	 * <p>Test the parsing of network properties from the specified file, by reading them and converting into LDIF format.</p>
	 *
	 * <p>Can be used, for example, to convert information previously statically configured by properties on each
	 * device, to assemble LDIF files to be loaded into an LDAP server for use via the DICOM Network Configuration
	 * Management service.</p>
	 *
	 * @param	arg	two arguments, a single file name that is the properties file, then the root distinguished name for LDAP
	 */
	public static void main(String arg[]) {
		String propertiesFileName = arg[0];
		try {
			FileInputStream in = new FileInputStream(propertiesFileName);
			Properties properties = new Properties(/*defaultProperties*/);
			properties.load(in);
			in.close();
//System.err.println("properties="+properties);
			System.out.print(new NetworkApplicationProperties(properties).getNetworkApplicationInformation().getLDIFRepresentation(arg[1]));
		}
		catch (Exception e) {
			System.err.println(e);
		}


	}
}


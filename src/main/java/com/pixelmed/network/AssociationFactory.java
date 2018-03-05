/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.VersionAndConstants;

import java.util.LinkedList;
import java.io.IOException;
import java.net.Socket;

import java.lang.reflect.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A factory object of static methods that can accept and initiate associations.</p>
 *
 * @author	dclunie
 */
public class AssociationFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociationFactory.java,v 1.32 2017/01/24 10:50:44 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AssociationFactory.class);
	
	// Work around "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5092063" Java 1.5 on Windows - Extremely slow socket creation using new Socket("ip-address", port)
	
	static {
		//System.err.println("Checking for need to deactivate ProxySelector.");
		//if (false) {
		if (System.getProperty("java.version", "").startsWith("1.5")) {
			try {
				//System.err.println("Sadly, we are on 1.5.x, so deactivate");
				Class PS = Class.forName("java.net.ProxySelector");
				Method[] m = PS.getDeclaredMethods();
				for (int i = 0; i < m.length; i++) {
					if (Modifier.isPublic(m[i].getModifiers())) {
						if (m[i].getName().equals("setDefault") && 
						   m[i].getParameterTypes().length == 1) {
							Object[] params = { null };
							m[i].invoke(PS, params);
							//System.err.println("ProxySelector Deactivated.");
						}
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("While deactivating ProxySelector for 1.5.x",e);
			} 
		}
	}
	
	//static private final int ourMaximumLengthReceived = 0;		// we don't care ... we can allocate a buffer of any size
	//static private final int ourMaximumLengthReceived = 65536;	// we do care ... 0 kills GE AW (manifests as malformed C-ECHO-RSP and C-STORE-RSP PDUs, for example)
	static private final int ourMaximumLengthReceived = 16384;		// we do care ... larger than TCP buffer size causes many small packets

	static private final int defaultReceiveBufferSize = 65536;		// just because old Windows default is ridiculously low at 8192
	static private final int defaultSendBufferSize = 0;				// we do not have a good rationale for specifying this rather than leaving it unlimited

	/**
	 * <p>Get the default Maximum PDU Size that we would like to receive.</p>
	 *
	 * @return	the default Maximum PDU Size
	 */
	static public int getDefaultMaximumLengthReceived() { return ourMaximumLengthReceived; }
	
	/**
	 * <p>Get the default TCP socket receive buffer size to use to set the socket options.</p>
	 *
	 * @return	the receive buffer size, 0 means leave the default operating system value alone
	 */
	static public int getDefaultReceiveBufferSize() { return defaultReceiveBufferSize; }
	
	/**
	 * <p>Get the default TCP socket send buffer size to use to set the socket options.</p>
	 *
	 * @return	the send buffer size, 0 means leave the default operating system value alone
	 */
	static public int getDefaultSendBufferSize() { return defaultSendBufferSize; }

	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID and Implementation Version 
	 * of the toolkit are used.
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(String,int,String,String,int,int,int,LinkedList,LinkedList,boolean,String,String)} instead.
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @param	username					may be null if no user identity
	 * @param	password					may be null if no user identity or no password required
	 * @param	debugLevel					ignored
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,String username,String password,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(hostname,port,calledAETitle,callingAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
			presentationContexts,scuSCPRoleSelections,secureTransport,username,password);
	}
	
	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID and Implementation Version 
	 * of the toolkit are used.
	 *
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @param	username					may be null if no user identity
	 * @param	password					may be null if no user identity or no password required
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,String username,String password
				) throws DicomNetworkException,IOException {
		return new AssociationInitiator(hostname,port,calledAETitle,callingAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
			presentationContexts,scuSCPRoleSelections,secureTransport,username,password);
	}


	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(String,int,String,String,LinkedList,LinkedList,boolean)} instead.
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @param	debugLevel					ignored
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(hostname,port,calledAETitle,callingAETitle,presentationContexts,scuSCPRoleSelections,secureTransport);
	}
	
	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport
				) throws DicomNetworkException,IOException {
		return new AssociationInitiator(hostname,port,calledAETitle,callingAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			getDefaultMaximumLengthReceived(),
			getDefaultReceiveBufferSize(),
			getDefaultSendBufferSize(),
			presentationContexts,scuSCPRoleSelections,secureTransport,null,null);
	}


	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(String,int,String,String,LinkedList,LinkedList,boolean,String,String)} instead.
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @param	username					may be null if no user identity
	 * @param	password					may be null if no user identity or no password required
	 * @param	debugLevel					ignored
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,String username,String password,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(hostname,port,calledAETitle,callingAETitle,presentationContexts,scuSCPRoleSelections,secureTransport,username,password);
	}
	/**
	 * Opens a transport connection and initiates an association.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * @param	hostname					hostname or IP address (dotted quad) component of presentation address of the remote AE (them)
	 * @param	port						TCP port component of presentation address of the remote AE (them)
	 * @param	calledAETitle				the AE Title of the remote (their) end of the association
	 * @param	callingAETitle				the AE Title of the local (our) end of the association
	 * @param	presentationContexts		a java.util.LinkedList of {@link PresentationContext PresentationContext} objects, each of which contains an Abstract Syntax (SOP Class UID) and one or more Transfer Syntaxes
	 * @param	scuSCPRoleSelections		a java.util.LinkedList of {@link SCUSCPRoleSelection SCUSCPRoleSelection} objects, each of which contains an Abstract Syntax (SOP Class UID) and specifies whether SCU and/or SCP roles are supported
	 * @param	secureTransport				true if to use secure transport protocol
	 * @param	username					may be null if no user identity
	 * @param	password					may be null if no user identity or no password required
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ASSOCIATE-RJ, A-ABORT and A-P-ABORT indications
	 */
	static public AssociationInitiator createNewAssociation(String hostname,int port,String calledAETitle,
				String callingAETitle,
				LinkedList presentationContexts,LinkedList scuSCPRoleSelections,
				boolean secureTransport,String username,String password
				) throws DicomNetworkException,IOException {
		return new AssociationInitiator(hostname,port,calledAETitle,callingAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			getDefaultMaximumLengthReceived(),
			getDefaultReceiveBufferSize(),
			getDefaultSendBufferSize(),
			presentationContexts,scuSCPRoleSelections,secureTransport,username,password);
	}

	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID and Implementation Version
	 * of the toolkit are used.
	 *
	 * The default UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy is used.
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(Socket,String,int,int,int)} instead.
	 * @param	socket						already open transport connection on which the association is to be accepted
	 * @param	calledAETitle				the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	debugLevel					ignored
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(socket,calledAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize);
	}
	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID and Implementation Version
	 * of the toolkit are used.
	 *
	 * The default UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy is used.
	 *
	 * @deprecated							See {@link com.pixelmed.network.AssociationFactory#createNewAssociation(Socket,String,int,int,int,PresentationContextSelectionPolicy)}
	 * @param	socket						already open transport connection on which the association is to be accepted
	 * @param	calledAETitle				the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize
				) throws DicomNetworkException,IOException {
		return new AssociationAcceptor(socket,calledAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
			new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy());
	}

	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID and Implementation Version
	 * of the toolkit are used.
	 *
	 * @deprecated									SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(Socket,String,int,int,int,PresentationContextSelectionPolicy)} instead.
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @param	debugLevel							ignored
	 * @return										an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException				thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				PresentationContextSelectionPolicy presentationContextSelectionPolicy,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(socket,calledAETitle,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,presentationContextSelectionPolicy);
	}
	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID and Implementation Version
	 * of the toolkit are used.
	 *
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
	 * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
	 * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @return										an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException				thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				int ourMaximumLengthReceived,int socketReceiveBufferSize,int socketSendBufferSize,
				PresentationContextSelectionPolicy presentationContextSelectionPolicy
				) throws DicomNetworkException,IOException {
		return new AssociationAcceptor(socket,calledAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,
			presentationContextSelectionPolicy);
	}

	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The default maximum length received, receive buffer size and send buffer size are used.
	 *
	 * @deprecated									SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #createNewAssociation(Socket,String,PresentationContextSelectionPolicy)} instead.
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @param	debugLevel							ignored
	 * @return										an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException				thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				PresentationContextSelectionPolicy presentationContextSelectionPolicy,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(socket,calledAETitle,presentationContextSelectionPolicy);
	}
	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The default maximum length received, receive buffer size and send buffer size are used.
	 *
	 * @param	socket								already open transport connection on which the association is to be accepted
	 * @param	calledAETitle						the AE Title of the local (our) end of the association
	 * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
	 * @return										an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException				thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				PresentationContextSelectionPolicy presentationContextSelectionPolicy
				) throws DicomNetworkException,IOException {
		return new AssociationAcceptor(socket,calledAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			getDefaultMaximumLengthReceived(),
			getDefaultReceiveBufferSize(),
			getDefaultSendBufferSize(),
			presentationContextSelectionPolicy);
	}

	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The default maximum length received, receive buffer size and send buffer size are used.
	 *
	 * The default UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy is used.
	 *
	 * @deprecated	See {@link com.pixelmed.network.AssociationFactory#createNewAssociation(Socket,String,PresentationContextSelectionPolicy) createNewAssociation(Socket,String)}
	 *
	 * @param	socket						already open transport connection on which the association is to be accepted
	 * @param	calledAETitle				the AE Title of the local (our) end of the association
	 * @param	debugLevel					ignored
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle,
				int debugLevel) throws DicomNetworkException,IOException {
		slf4jlogger.warn("Debug level supplied as argument ignored");
		return createNewAssociation(socket,calledAETitle);
	}
	
	/**
	 * Accepts an association on the supplied open transport connection.
	 *
	 * The default Implementation Class UID, Implementation Version and Maximum PDU Size
	 * of the toolkit are used.
	 *
	 * The default maximum length received, receive buffer size and send buffer size are used.
	 *
	 * The default UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy is used.
	 *
	 * @deprecated	See {@link com.pixelmed.network.AssociationFactory#createNewAssociation(Socket,String,PresentationContextSelectionPolicy) createNewAssociation()}
	 *
	 * @param	socket						already open transport connection on which the association is to be accepted
	 * @param	calledAETitle				the AE Title of the local (our) end of the association
	 * @return								an open association in state 6 - Data Transfer
	 * @throws	IOException
	 * @throws	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
	 */
	static public AssociationAcceptor createNewAssociation(Socket socket,String calledAETitle
				) throws DicomNetworkException,IOException {
		return new AssociationAcceptor(socket,calledAETitle,
			VersionAndConstants.implementationClassUID,
			VersionAndConstants.implementationVersionName,
			getDefaultMaximumLengthReceived(),
			getDefaultReceiveBufferSize(),
			getDefaultSendBufferSize(),
			new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy());
	}
}




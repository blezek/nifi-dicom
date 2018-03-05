/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.HexDump;
import com.pixelmed.utils.ByteArray;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.io.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
class AssociateRequestAcceptPDU {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/AssociateRequestAcceptPDU.java,v 1.28 2017/01/24 10:50:44 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AssociateRequestAcceptPDU.class);

	/***/
	private byte[] b;

	/***/
	protected int pduType;
	/***/
	private int pduLength;
	/***/
	private int protocolVersion;
	/***/
	private String calledAETitle;
	/***/
	private String callingAETitle;

	/***/
	private LinkedList itemList;

	/***/
	private int maximumLengthReceived;

	/**
	 * @param	aet
	 * @param	bo
	 * @param	name
	 * @throws	DicomNetworkException
	 * @throws	UnsupportedEncodingException
	 */
	private static void writeAETToPDU(String aet,ByteArrayOutputStream bo,String name) throws DicomNetworkException, UnsupportedEncodingException {
		byte[] baet = aet.getBytes("ASCII");
		int baetl = baet.length;
		if (baetl > 16) throw new DicomNetworkException(name+" AET too long (>16)");
		bo.write(baet,0,baetl);
		while (baetl++ < 16) bo.write(0x20);
	}

	/**
	 * @param	subItemType
	 * @param	name
	 * @throws	DicomNetworkException
	 * @throws	UnsupportedEncodingException
	 */
	private static final byte[] makeByteArrayOfSyntaxSubItem(int subItemType,String name) throws DicomNetworkException, UnsupportedEncodingException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
		bo.write(subItemType);
		bo.write(0x00);							// reserved
		byte[] sn = name.getBytes("ASCII");
		int lsn = sn.length;
		bo.write((byte)(lsn>>8)); bo.write((byte)lsn);		// length (big endian)
		bo.write(sn,0,lsn);
		return bo.toByteArray();
	}

	/**
	 * @param	itemType
	 * @param	pc
	 * @throws	DicomNetworkException
	 * @throws	UnsupportedEncodingException
	 */
	private static final byte[] makeByteArrayOfPresentationContextItem(int itemType,PresentationContext pc) throws DicomNetworkException, UnsupportedEncodingException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
		bo.write((byte)itemType);					// Presentation Context Item Type (0x20 request, 0x21 accept)
		bo.write(0x00);							// reserved
		bo.write(0x00); bo.write(0x00);					// will fill in length here later
		bo.write(pc.getIdentifier()&0xff);				// Presentation Context ID
		bo.write(0x00);							// reserved
		bo.write(itemType == 0x20 ? 0x00 : (byte)pc.getResultReason());	// Result/reason only for accept, else reserved
		bo.write(0x00);							// reserved

		String abstractSyntaxUID=pc.getAbstractSyntaxUID();		// Acceptance PDU has no Abstract Syntax sub-item
		if (abstractSyntaxUID != null && abstractSyntaxUID.length() > 0) {
			byte[] asn = makeByteArrayOfSyntaxSubItem(0x30,abstractSyntaxUID);
			bo.write(asn,0,asn.length);
		}
		ListIterator i = pc.getTransferSyntaxUIDs().listIterator();
		while (i.hasNext()) {
			byte[] tsn = makeByteArrayOfSyntaxSubItem(0x40,(String)i.next());
			bo.write(tsn,0,tsn.length);
		}

		// compute size and fill in length field ...

		int n = bo.size()-4;

		byte[] b = bo.toByteArray();

		b[2]=(byte)(n>>8);						// big endian
		b[3]=(byte)n;

		return b;
	}

	/**
	 * @param	selection
	 * @throws	DicomNetworkException
	 * @throws	UnsupportedEncodingException
	 */
	private static final byte[] makeByteArrayOfSCUSCPRoleSelection(SCUSCPRoleSelection selection) throws DicomNetworkException, UnsupportedEncodingException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
		bo.write((byte)0x54);						// SCU/SCP Role Selection Sub-Item Type
		bo.write(0x00);							// reserved
		byte[] sn = selection.getAbstractSyntaxUID().getBytes("ASCII");
		int lsn = sn.length;
		int lsi = lsn + 2 + 2;
		bo.write((byte)(lsi>>8)); bo.write((byte)lsi);			// length of sub-item (big endian)
		bo.write((byte)(lsn>>8)); bo.write((byte)lsn);			// length of asbtract syntax UID (big endian)
		bo.write(sn,0,lsn);
		bo.write(selection.isSCURoleSupported() ? 0x01 : 0x00);
		bo.write(selection.isSCPRoleSupported() ? 0x01 : 0x00);
		return bo.toByteArray();
	}

	/**
	 * @param	selections		a LinkedList of SCUSCPRoleSelection's to add
	 * @param	subItemList		a LinkedList to which to add the created SCUSCPRoleSelectionUserInformationSubItem's
	 * @throws	DicomNetworkException
	 * @throws	UnsupportedEncodingException
	 */
	private final byte[] makeByteArrayOfSCUSCPRoleSelections(LinkedList selections,LinkedList subItemList) throws DicomNetworkException, UnsupportedEncodingException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
		if (selections != null) {
			ListIterator i = selections.listIterator();
			while (i.hasNext()) {
				SCUSCPRoleSelection selection = (SCUSCPRoleSelection)i.next();
				byte[] b = makeByteArrayOfSCUSCPRoleSelection(selection);
				bo.write(b,0,b.length);

				SCUSCPRoleSelectionUserInformationSubItem si = new SCUSCPRoleSelectionUserInformationSubItem(0x54,b.length,selection);
				subItemList.add(si);
			}
		}
		return bo.toByteArray();
	}

	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts) throws DicomNetworkException {
		doCommonConstructorStuff(pduType,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,null,0,null,null,null);

	}

	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityType			0 == do not send user identity negotiation subitem
	 * @param	userIdentityPrimaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentitySecondaryField	may be null as appropriate to userIdentityType
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			int userIdentityType,String userIdentityPrimaryField,String userIdentitySecondaryField) throws DicomNetworkException {

		byte[] userIdentityPrimaryFieldBytes = null;
		byte[] userIdentitySecondaryFieldBytes = null;
		if (userIdentityType > 0) {
			try {
				if (userIdentityPrimaryField != null) {
					userIdentityPrimaryFieldBytes = userIdentityPrimaryField.getBytes("UTF8");
					slf4jlogger.info("userIdentityPrimaryField = {}",userIdentityPrimaryField);
					if (slf4jlogger.isInfoEnabled()) slf4jlogger.info("userIdentityPrimaryFieldBytes = "+HexDump.dump(userIdentityPrimaryFieldBytes));
				}
				if (userIdentitySecondaryField != null) {
					userIdentitySecondaryFieldBytes = userIdentitySecondaryField.getBytes("UTF8");
				}
			}
			catch (java.io.UnsupportedEncodingException e) {
				throw new DicomNetworkException("Internal error - cannot convert UTF8 primary or secondary field"+e);
			}
		}
		doCommonConstructorStuff(pduType,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			userIdentityType,userIdentityPrimaryFieldBytes,userIdentitySecondaryFieldBytes,null);
	}
	
	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityType			0 == do not send user identity negotiation subitem
	 * @param	userIdentityPrimaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentitySecondaryField	may be null as appropriate to userIdentityType
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			int userIdentityType,byte[] userIdentityPrimaryField,byte[] userIdentitySecondaryField) throws DicomNetworkException {
		doCommonConstructorStuff(pduType,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			userIdentityType,userIdentityPrimaryField,userIdentitySecondaryField,null);
	}
	
	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections) throws DicomNetworkException {
		doCommonConstructorStuff(pduType,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			0,null,null,null);
	}
	
	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityServerResponse	null if no response
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			byte[] userIdentityServerResponse) throws DicomNetworkException {
		doCommonConstructorStuff(pduType,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			0,null,null,userIdentityServerResponse);
	}
	
	/**
	 * @param	pduType
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityType			0 == do not send user identity negotiation subitem
	 * @param	userIdentityPrimaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentitySecondaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentityServerResponse	null if no response
	 * @throws	DicomNetworkException
	 */
	void doCommonConstructorStuff(int pduType,String calledAETitle,String callingAETitle,
			String implementationClassUID,String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			int userIdentityType,byte[] userIdentityPrimaryField,byte[] userIdentitySecondaryField,byte[] userIdentityServerResponse) throws DicomNetworkException {
	try {
		// does two things at once:
		// 1. builds byte array of PDU
		// 2. keeps track of fields and items for subsequent internal use

		this.pduType = pduType;			// 0x01 is request, 0x02 is accept
		protocolVersion=0x0001;
		this.calledAETitle=calledAETitle;
		this.callingAETitle=callingAETitle;

		ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);

		// encode fixed length part ...

		bo.write((byte)pduType);					// A-ASSOC-RQ PDU Type
		bo.write(0x00);							// reserved
		bo.write(0x00); bo.write(0x00); bo.write(0x00); bo.write(0x00);	// will fill in length here later
		bo.write(0x00); bo.write(0x01);					// protocol version 1 (big endian)
		bo.write(0x00); bo.write(0x00);					// reserved

		writeAETToPDU(calledAETitle,bo,"Called");
		writeAETToPDU(callingAETitle,bo,"Calling");

		for (int i=0; i<32; ++i) bo.write(0x00);

		// encode variable length part ...

		itemList = new LinkedList();

		// one Application Context Item ...

		bo.write(0x10);							// Application Context Item Type
		bo.write(0x00);							// reserved
		String applicationContextNameUID="1.2.840.10008.3.1.1.1";
		byte[] acn = applicationContextNameUID.getBytes("ASCII");
		int lacn = acn.length;
		bo.write((byte)(lacn>>8)); bo.write((byte)lacn);		// length (big endian)
		bo.write(acn,0,lacn);
		
		ApplicationContextItem aci = new ApplicationContextItem(0x10,lacn,applicationContextNameUID);
		itemList.add(aci);

		// one or more Presentation Context Items ...

		ListIterator i = presentationContexts.listIterator();
		while (i.hasNext()) {
			int itemType = pduType == 0x01 ? 0x20 : 0x21;
			PresentationContext pc = (PresentationContext)i.next();
			byte[] bpc = makeByteArrayOfPresentationContextItem(itemType,pc);
			bo.write(bpc,0,bpc.length);

			PresentationContextItem pci = new PresentationContextItem(itemType,bpc.length,pc);
			itemList.add(pci);
		}

		// one User Information Item ...

		byte[] icuid = implementationClassUID.getBytes("ASCII");
		int licuid = icuid.length;
		byte[] ivn = implementationVersionName.getBytes("ASCII");
		int livn = ivn.length;
		LinkedList ssrs_uii_subitems = new LinkedList();
		byte[] ssrs = makeByteArrayOfSCUSCPRoleSelections(scuSCPRoleSelections,ssrs_uii_subitems);
		int lssrs = ssrs.length;

		int userIdentityPrimaryFieldLength = userIdentityPrimaryField == null ? 0 : userIdentityPrimaryField.length;
		int userIdentitySecondaryFieldLength = userIdentitySecondaryField == null ? 0 : userIdentitySecondaryField.length;
		int positiveResponseRequired = userIdentityType == 3 ? 1 : 0;	// just for Kerberos
		int luinr = 0;
		if (pduType == 0x01 && userIdentityType > 0) {
			luinr = 1+1+2+userIdentityPrimaryFieldLength+2+userIdentitySecondaryFieldLength;
		}
		
		int userIdentityServerResponseLength = userIdentityServerResponse == null ? 0 : userIdentityServerResponse.length;
		int luina = 0;
		if (pduType == 0x02 && userIdentityServerResponse != null) {
			luina = 2+userIdentityServerResponseLength;
		}

		bo.write(0x50);							// User Information Item Type
		bo.write(0x00);							// reserved
		int luii = 2 + 2 + 4
			 + 2 + 2 + licuid
			 + 2 + 2 + livn
			 + (luinr > 0 ? (2 + 2 + luinr) : 0)
			 + lssrs;
		bo.write((byte)(luii>>8)); bo.write((byte)luii);		// total length (big endian) of all of the sub-items
		
		bo.write(0x51);							// Maximum Length Received User Information Sub Item Type
		bo.write(0x00);							// reserved
		bo.write(0x00);	bo.write(0x04);					// 2-byte (big endian) sub-item length is fixed at 4
		bo.write((byte)(ourMaximumLengthReceived>>24));			// big-endian ourMaximumLengthReceived is 4 byte value
		bo.write((byte)(ourMaximumLengthReceived>>16));
		bo.write((byte)(ourMaximumLengthReceived>>8));
		bo.write((byte)ourMaximumLengthReceived);
		
		bo.write(0x52);							// Implementation Class UID User Information Sub Item Type
		bo.write(0x00);							// reserved
		bo.write((byte)(licuid>>8)); bo.write((byte)licuid);		// length (big endian)
		bo.write(icuid,0,licuid);
		
		if (lssrs > 0) {
			bo.write(ssrs,0,lssrs);					// SCU/SCP Role Selections
		}

		bo.write(0x55);							// Implementation Version Name User Information Sub Item Type
		bo.write(0x00);							// reserved
		bo.write((byte)(livn>>8)); bo.write((byte)livn);		// length (big endian)
		bo.write(ivn,0,livn);
		
		if (luinr > 0) {
			bo.write(0x58);																		// User Identity Negotiation Request User Information Sub Item Type
			bo.write(0x00);																		// reserved
			bo.write((byte)(luinr>>8)); bo.write((byte)luinr);									// big endian
			bo.write((byte)userIdentityType);
			bo.write((byte)positiveResponseRequired);
			bo.write((byte)(userIdentityPrimaryFieldLength>>8)); bo.write((byte)userIdentityPrimaryFieldLength);		// big endian
			if (userIdentityPrimaryFieldLength > 0) {
				bo.write(userIdentityPrimaryField,0,userIdentityPrimaryFieldLength);
			}
			bo.write((byte)(userIdentitySecondaryFieldLength>>8)); bo.write((byte)userIdentitySecondaryFieldLength);	// big endian
			if (userIdentitySecondaryFieldLength > 0) {
				bo.write(userIdentitySecondaryField,0,userIdentitySecondaryFieldLength);
			}
		}
		
		if (luina > 0) {
			bo.write(0x59);																		// User Identity Negotiation Accept User Information Sub Item Type
			bo.write(0x00);																		// reserved
			bo.write((byte)(luina>>8)); bo.write((byte)luina);									// big endian
			bo.write((byte)(userIdentityPrimaryFieldLength>>8)); bo.write((byte)userIdentityPrimaryFieldLength);		// big endian
			if (userIdentityServerResponseLength > 0) {
				bo.write(userIdentityServerResponse,0,userIdentityServerResponseLength);
			}
		}

		UserInformationItem uii = new UserInformationItem(0x50,luii);
		itemList.add(uii);
		
		MaximumLengthReceivedUserInformationSubItem mlruisi = new MaximumLengthReceivedUserInformationSubItem(0x51,4,ourMaximumLengthReceived);
		uii.subItemList.add(mlruisi);

		ImplementationClassUIDUserInformationSubItem icuiduisi = new ImplementationClassUIDUserInformationSubItem(0x52,licuid,implementationClassUID);
		uii.subItemList.add(icuiduisi);
		
		uii.subItemList.addAll(ssrs_uii_subitems);

		ImplementationVersionNameUserInformationSubItem ivnuisi = new ImplementationVersionNameUserInformationSubItem(0x55,livn,implementationVersionName);
		uii.subItemList.add(ivnuisi);
		
		if (luinr > 0) {
			UserIdentityNegotiationRequestUserInformationSubItem uinruisi = new UserIdentityNegotiationRequestUserInformationSubItem(
				0x58,luinr,userIdentityType,positiveResponseRequired,userIdentityPrimaryFieldLength,userIdentityPrimaryField,userIdentitySecondaryFieldLength,userIdentitySecondaryField);
			uii.subItemList.add(uinruisi);
		}
		
		if (luina > 0) {
			UserIdentityNegotiationAcceptUserInformationSubItem uinauisi = new UserIdentityNegotiationAcceptUserInformationSubItem(
				0x59,luina,userIdentityServerResponseLength,userIdentityServerResponse);
			uii.subItemList.add(uinauisi);
		}
		
		// compute size and fill in length field ...

		pduLength = bo.size()-6;

		b = bo.toByteArray();

		b[2]=(byte)(pduLength>>24);						// big endian
		b[3]=(byte)(pduLength>>16);
		b[4]=(byte)(pduLength>>8);
		b[5]=(byte)pduLength;
	}
	catch (UnsupportedEncodingException e) {
		throw new DicomNetworkException("Unsupported encoding generated exception "+e);
	}
	}

	/***/
	private class AssociationItem {
		/***/
		int type;
		/***/
		int length;
		/**
		 * @param	t
		 * @param	l
		 */
		AssociationItem(int t,int l) { type=t; length=l; }
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Item Type: 0x");
			sb.append(Integer.toHexString(type));
			sb.append(" (length 0x");
			sb.append(Integer.toHexString(length));
			sb.append(")");
			return sb.toString();
		}
	}

	/***/
	private class ApplicationContextItem extends AssociationItem {
		/***/
		String applicationContextName;
		/**
		 * @param	t
		 * @param	l
		 * @param	acn
		 */
		ApplicationContextItem(int t,int l,String acn) {
			super(t,l);
			applicationContextName=acn;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (Application Context)\n\t");
			sb.append(applicationContextName);
			sb.append("\n");
			return sb.toString();			
		}
	}

	/***/
	private class UserInformationItem extends AssociationItem {
		/***/
		LinkedList subItemList;
		/**
		 * @param	t
		 * @param	l
		 */
		UserInformationItem(int t,int l) {
			super(t,l);
			subItemList = new LinkedList();
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (User Information)\n\t");

			ListIterator i = subItemList.listIterator();
			while (i.hasNext()) {
				UserInformationSubItem subitem = (UserInformationSubItem)(i.next());
				sb.append(subitem);
			}

			sb.append("\n");
			return sb.toString();			
		}
	}
	
	/***/
	private class UserInformationSubItem {
		/***/
		int type;
		/***/
		int length;
		/**
		 * @param	t
		 * @param	l
		 */
		UserInformationSubItem(int t,int l) { type=t; length=l; }
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("User Information Sub Item Type: 0x");
			sb.append(Integer.toHexString(type));
			sb.append(" (length 0x");
			sb.append(Integer.toHexString(length));
			sb.append(")");
			return sb.toString();
		}
	}
	
	/***/
	private class MaximumLengthReceivedUserInformationSubItem extends UserInformationSubItem {
		/***/
		int maximumLengthReceived;
		/**
		 * @param	t
		 * @param	l
		 * @param	maximumLengthReceived
		 */
		MaximumLengthReceivedUserInformationSubItem(int t,int l,int maximumLengthReceived) {
			super(t,l);
			this.maximumLengthReceived=maximumLengthReceived;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (Maximum Length Received 0x");
			sb.append(Integer.toHexString(maximumLengthReceived));
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class ImplementationClassUIDUserInformationSubItem extends UserInformationSubItem {
		/***/
		String implementationClassUID;
		/**
		 * @param	t
		 * @param	l
		 * @param	implementationClassUID
		 */
		ImplementationClassUIDUserInformationSubItem(int t,int l,String implementationClassUID) {
			super(t,l);
			this.implementationClassUID=implementationClassUID;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (Implementation Class UID ");
			sb.append(implementationClassUID);
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class ImplementationVersionNameUserInformationSubItem extends UserInformationSubItem {
		/***/
		String implementationVersionName;
		/**
		 * @param	t
		 * @param	l
		 * @param	implementationVersionName
		 */
		ImplementationVersionNameUserInformationSubItem(int t,int l,String implementationVersionName) {
			super(t,l);
			this.implementationVersionName=implementationVersionName;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (Implementation Version Name ");
			sb.append(implementationVersionName);
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class SOPClassExtendedNegotiationUserInformationSubItem extends UserInformationSubItem {
		/***/
		int sopClassUIDLength;
		String sopClassUID;
		byte[] info;
		/**
		 * @param	t
		 * @param	l
		 * @param	sopClassUIDLength
		 * @param	sopClassUID
		 * @param	info
		 */
		SOPClassExtendedNegotiationUserInformationSubItem(int t,int l,int sopClassUIDLength,String sopClassUID,byte [] info) {
			super(t,l);
			this.sopClassUIDLength=sopClassUIDLength;
			this.sopClassUID=sopClassUID;
			this.info=info;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (SOP Class Extended Negotiation ");
			sb.append("[0x");
			sb.append(Integer.toHexString(sopClassUIDLength));
			sb.append("] ");
			sb.append(sopClassUID);
			sb.append("\n\t\t");
			sb.append(HexDump.dump(info));
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class SCUSCPRoleSelectionUserInformationSubItem extends UserInformationSubItem {
		/***/
		SCUSCPRoleSelection selection;
		/**
		 * @param	t
		 * @param	l
		 * @param	selection
		 */
		SCUSCPRoleSelectionUserInformationSubItem(int t,int l,SCUSCPRoleSelection selection) {
			super(t,l);
			this.selection=selection;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (SCU/SCP Role Selection ");
			sb.append("[0x");
			sb.append(Integer.toHexString(selection.getAbstractSyntaxUID().length()));
			sb.append("] ");
			sb.append(selection.getAbstractSyntaxUID());
			sb.append("\n\t\tSCU Role supported: ");
			sb.append(selection.isSCURoleSupported());
			sb.append("\n\t\tSCP Role supported: ");
			sb.append(selection.isSCPRoleSupported());
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class UserIdentityNegotiationRequestUserInformationSubItem extends UserInformationSubItem {

		int userIdentityType;
		int positiveResponseRequired;
		int primaryFieldLength;
		byte[] primaryField;
		int secondaryFieldLength;
		byte[] secondaryField;

		/**
		 * @param	t
		 * @param	l
		 * @param	userIdentityType
		 * @param	positiveResponseRequired
		 * @param	primaryFieldLength
		 * @param	primaryField		null if primaryFieldLength == 0
		 * @param	secondaryFieldLength
		 * @param	secondaryField		null if secondaryFieldLength == 0
		 */
		UserIdentityNegotiationRequestUserInformationSubItem(int t,int l,
				int userIdentityType,int positiveResponseRequired,
				int primaryFieldLength,byte[] primaryField,
				int secondaryFieldLength,byte[] secondaryField) {
			super(t,l);
			this.userIdentityType = userIdentityType;
			this.positiveResponseRequired = positiveResponseRequired;
			this.primaryFieldLength = primaryFieldLength;
			this.primaryField = primaryField;
			this.secondaryFieldLength = secondaryFieldLength;
			this.secondaryField = secondaryField;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (User Identity Negotiation Request ");
			sb.append("\n\t\tUser Identity Type: ");
			sb.append(Integer.toString(userIdentityType));
			sb.append(" ");
			switch (userIdentityType) {
				case 1:		sb.append("Username"); break;
				case 2:		sb.append("Username and passcode"); break;
				case 3:		sb.append("Kerberos Service ticket"); break;
				case 4:		sb.append("SAML Assertion"); break;
				default:	sb.append("Unrecognized"); break;
			}
			sb.append("\n\t\tPositive Response Required: ");
			sb.append(Integer.toString(positiveResponseRequired));
			sb.append("\n\t\tPrimary Field: ");
			sb.append("[0x");
			sb.append(Integer.toHexString(primaryFieldLength));
			sb.append("] ");
			if (primaryFieldLength > 0) {
				if (userIdentityType == 1 || userIdentityType == 2 || userIdentityType == 4) {
					try {
						sb.append(new String(primaryField,"UTF8"));
					}
					catch (java.io.UnsupportedEncodingException e) {
						sb.append(HexDump.dump(primaryField));
					}
				}
				else {
					// will happen for Kerberos, and if standard is extended with new types
					sb.append(HexDump.dump(primaryField));
				}
			}
			sb.append("\n\t\tSecondary Field: ");
			sb.append("[0x");
			sb.append(Integer.toHexString(secondaryFieldLength));
			sb.append("] ");
			if (secondaryFieldLength > 0) {
				if (userIdentityType == 2) {
					sb.append(secondaryField);
				}
				else {
					// should never happen, but standard may be extended with new types
					sb.append(HexDump.dump(secondaryField));
				}
			}
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class UserIdentityNegotiationAcceptUserInformationSubItem extends UserInformationSubItem {

		int serverResponseLength;
		byte[] serverResponse;

		/**
		 * @param	t
		 * @param	l
		 * @param	serverResponseLength
		 * @param	serverResponse		null if serverResponseLength == 0
		 */
		UserIdentityNegotiationAcceptUserInformationSubItem(int t,int l,int serverResponseLength,byte[] serverResponse) {
			super(t,l);
			this.serverResponseLength = serverResponseLength;
			this.serverResponse = serverResponse;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (User Identity Negotiation Accept ");
			sb.append("\n\t\tServer Response: ");
			sb.append("[0x");
			sb.append(Integer.toHexString(serverResponseLength));
			sb.append("] ");
			if (serverResponseLength > 0) {
				// cannot tell from message whether Kerberos (bytes) or SAML (XML)
				sb.append(HexDump.dump(serverResponse));
				sb.append("\n\t\t");
			}
			sb.append(")\n\t");
			return sb.toString();			
		}
	}

	/***/
	private class PresentationContextItem extends AssociationItem {
		/***/
		PresentationContext pc;
		/**
		 * @param	t
		 * @param	l
		 * @param	id
		 * @param	resultReason
		 */
		PresentationContextItem(int t,int l,byte id,byte resultReason) {
			super(t,l);
			pc=new PresentationContext(id,resultReason);
		}
		/**
		 * @param	t
		 * @param	l
		 * @param	pc
		 */
		PresentationContextItem(int t,int l,PresentationContext pc) {
			super(t,l);
			this.pc=pc;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append(" (Presentation Context)\n\t");
			sb.append(pc);
			sb.append("\n");
			return sb.toString();			
		}
	}

	/**
	 * @param	pdu
	 * @throws	DicomNetworkException
	 */
	public AssociateRequestAcceptPDU(byte[] pdu) throws DicomNetworkException {
	try {
		b=pdu;
		pduType = b[0]&0xff;
		pduLength = ByteArray.bigEndianToUnsignedInt(b,2,4);
		protocolVersion = ByteArray.bigEndianToUnsignedInt(b,6,2);
		calledAETitle  = new String(b,10,16);
		callingAETitle = new String(b,26,16);

		itemList = new LinkedList();
		int offset = 74;
		while (offset+3 < b.length) {
			int itemType = b[offset++]&0xff;
			offset++;			// skip reserved byte
			int lng = ByteArray.bigEndianToUnsignedInt(b,offset,2);	// all items use 2-byte big-endian length field
			offset+=2;

			if (itemType == 0x10) {
				ApplicationContextItem item = new ApplicationContextItem(itemType,lng,new String(b,offset,lng));
				itemList.add(item);
				offset+=lng;
			}
			else if (itemType == 0x20 || itemType == 0x21) {		// Presentation Context Item (request or accept)
				byte id=b[offset];
				offset+=2; lng-=2;		// ID and 1 reserved bytes
				byte resultReason=b[offset];
				offset+=2; lng-=2;		// result/reason and 1 reserved bytes
				PresentationContextItem item = new PresentationContextItem(itemType,lng,id,resultReason);
				itemList.add(item);
				while (lng > 0) {
					int subItemType = b[offset++]&0xff; --lng;
					++offset; --lng;	// reserved byte
					int silng = ByteArray.bigEndianToUnsignedInt(b,offset,2);	// all sub-items use 2-byte big-endian length field
					offset+=2; lng-=2;

					if (subItemType == 0x30) {
						item.pc.setAbstractSyntaxUID(new String(b,offset,silng));
					}
					else if (subItemType == 0x40) {
						item.pc.addTransferSyntaxUID(new String(b,offset,silng));
					}
					else {
						throw new DicomNetworkException("Unrecognized sub-item type "+Integer.toHexString(subItemType)+" in Presentation Context Item");
					}
					offset+=silng; lng-=silng;
				}
			}
			else if (itemType == 0x50) {		// User Information Item
//System.err.println("AssociateRequestAcceptPDU: parse: User Information Item");
				UserInformationItem item = new UserInformationItem(itemType,lng);
				itemList.add(item);
				while (lng > 0) {
					int subItemType = b[offset++]&0xff; --lng;
					++offset; --lng;	// reserved byte
					int silng = ByteArray.bigEndianToUnsignedInt(b,offset,2);	// all sub-items use 2-byte big-endian length field
					offset+=2; lng-=2;
					
					if (subItemType == 0x51) {
						if (silng == 4) {
							maximumLengthReceived = ByteArray.bigEndianToUnsignedInt(b,offset,4);
//System.err.println("AssociateRequestAcceptPDU: parse: maximumLengthReceived ="+maximumLengthReceived+" dec");
							item.subItemList.add(new MaximumLengthReceivedUserInformationSubItem(subItemType,silng,maximumLengthReceived));
						}
						else {
							throw new DicomNetworkException("Maximum length sub-item wrong length ("+silng+" dec) in User Information Item");
						}
					}
					else if (subItemType == 0x52) {
						String implementationClassUID = new String(b,offset,silng,"ASCII");
//System.err.println("AssociateRequestAcceptPDU: parse: implementationClassUID ="+implementationClassUID);
						item.subItemList.add(new ImplementationClassUIDUserInformationSubItem(subItemType,silng,implementationClassUID));
					}
					else if (subItemType == 0x54) {
//System.err.println("AssociateRequestAcceptPDU: parse: subItemType =0x"+Integer.toHexString(subItemType));
						int sopClassUIDLength = ByteArray.bigEndianToUnsignedInt(b,offset,2);
//System.err.println("AssociateRequestAcceptPDU: parse: sopClassUIDLength ="+sopClassUIDLength);
						String sopClassUID = new String(b,offset+2,sopClassUIDLength,"ASCII");
//System.err.println("AssociateRequestAcceptPDU: parse: sopClassUID ="+sopClassUID);
						boolean scuRole = b[offset+2+sopClassUIDLength] != 0;
						boolean scpRole = b[offset+2+sopClassUIDLength+1] != 0;
						item.subItemList.add(new SCUSCPRoleSelectionUserInformationSubItem(subItemType,silng,new SCUSCPRoleSelection(sopClassUID,scuRole,scpRole)));
					}
					else if (subItemType == 0x55) {
						String implementationVersion = new String(b,offset,silng,"ASCII");
//System.err.println("AssociateRequestAcceptPDU: parse: implementationVersion ="+implementationVersion);
						item.subItemList.add(new ImplementationVersionNameUserInformationSubItem(subItemType,silng,implementationVersion));
					}
					else if (subItemType == 0x56) {
//System.err.println("AssociateRequestAcceptPDU: parse: subItemType =0x"+Integer.toHexString(subItemType));
						int sopClassUIDLength = ByteArray.bigEndianToUnsignedInt(b,offset,2);
//System.err.println("AssociateRequestAcceptPDU: parse: sopClassUIDLength ="+sopClassUIDLength);
						String sopClassUID = new String(b,offset+2,sopClassUIDLength,"ASCII");
//System.err.println("AssociateRequestAcceptPDU: parse: sopClassUID ="+sopClassUID);
						byte[] info = ByteArray.extractBytes(b,offset+2+sopClassUIDLength,silng-2-sopClassUIDLength);
						item.subItemList.add(new SOPClassExtendedNegotiationUserInformationSubItem(subItemType,silng,sopClassUIDLength,sopClassUID,info));
					}
					else if (subItemType == 0x58) {
//System.err.println("AssociateRequestAcceptPDU: parse: subItemType =0x"+Integer.toHexString(subItemType));
						int withinItemOffset = offset;
						int userIdentityType = b[withinItemOffset++]&0xff;
						int positiveResponseRequired = b[withinItemOffset++]&0xff;
						int primaryFieldLength = ByteArray.bigEndianToUnsignedInt(b,withinItemOffset,2);
						withinItemOffset+=2;
						byte[] primaryField = primaryFieldLength > 0 ? ByteArray.extractBytes(b,withinItemOffset,primaryFieldLength) : null;
						withinItemOffset+=primaryFieldLength;
						int secondaryFieldLength = ByteArray.bigEndianToUnsignedInt(b,withinItemOffset,2);
						withinItemOffset+=2;
						byte[] secondaryField = secondaryFieldLength > 0 ? ByteArray.extractBytes(b,withinItemOffset,secondaryFieldLength) : null;
						item.subItemList.add(new UserIdentityNegotiationRequestUserInformationSubItem(
							subItemType,silng,userIdentityType,positiveResponseRequired,primaryFieldLength,primaryField,secondaryFieldLength,secondaryField));
					}
					else if (subItemType == 0x59) {
//System.err.println("AssociateRequestAcceptPDU: parse: subItemType =0x"+Integer.toHexString(subItemType));
						int serverResponseLength = ByteArray.bigEndianToUnsignedInt(b,offset,2);
						byte[] serverResponse = serverResponseLength > 0 ? ByteArray.extractBytes(b,offset+2,serverResponseLength) : null;
						item.subItemList.add(new UserIdentityNegotiationAcceptUserInformationSubItem(subItemType,silng,serverResponseLength,serverResponse));
					}
					else {
						//throw new DicomNetworkException("Unrecognized sub-item type "+Integer.toHexString(subItemType)+" in User Information Item");
//System.err.println("Unrecognized sub-item type "+Integer.toHexString(subItemType)+" in User Information Item");
					}
					offset+=silng; lng-=silng;
				}
			}
			else {
				//throw new DicomNetworkException("Unrecognized Item type "+Integer.toHexString(itemType)+" in A-ASSOCIATE-RQ/AC PDU");
//System.err.println("Unrecognized Item type "+Integer.toHexString(itemType)+" in A-ASSOCIATE-RQ/AC PDU");
				offset+=lng;
			}
		}
	}
	catch (UnsupportedEncodingException e) {
		throw new DicomNetworkException("Unsupported encoding generated exception "+e);
	}
	//catch (Exception e) {
	//	slf4jlogger.error("", e);;
	//	throw new DicomNetworkException(e.toString());
	//}
	}

	/***/
	public byte[] getBytes() { return b; }

	/**
	 * @param	req
	 * @throws	DicomNetworkException
	 */
	public LinkedList getAcceptedPresentationContextsWithAbstractSyntaxIncludedFromRequest(LinkedList req) throws DicomNetworkException {
		LinkedList presentationContexts = new LinkedList();
		ListIterator itemi = itemList.listIterator();
		while (itemi.hasNext()) {
			AssociationItem item = (AssociationItem)(itemi.next());
			if (item.type == 0x21) {						// Presentation Context Item (accept)
				PresentationContext accpc = ((PresentationContextItem)item).pc;
				if (accpc.getResultReason() == 0) {	// acceptance rather than rejection
					byte accid = accpc.getIdentifier();
					String abstractSyntaxUID = null;
					List requestedTransferSyntaxUIDs = null;
					ListIterator reqi = req.listIterator();
					while (reqi.hasNext()) {
						PresentationContext reqpc = (PresentationContext)(reqi.next());
						if (reqpc.getIdentifier() == accid) {
							abstractSyntaxUID=reqpc.getAbstractSyntaxUID();
							requestedTransferSyntaxUIDs = reqpc.getTransferSyntaxUIDs();
							break;
						}
					}
					if (abstractSyntaxUID == null) {
						throw new DicomNetworkException("Accepted Presentation Context ID "+Integer.toHexString(accid)+" was not requested");
					}
					String acceptedTransferSyntaxUID = accpc.getTransferSyntaxUID();
					if (requestedTransferSyntaxUIDs != null && requestedTransferSyntaxUIDs.contains(acceptedTransferSyntaxUID)) {
						presentationContexts.add(new PresentationContext(accid,abstractSyntaxUID,acceptedTransferSyntaxUID));
					}
					else {
						slf4jlogger.warn("getAcceptedPresentationContextsWithAbstractSyntaxIncludedFromRequest(): encountered Presentation Context ID {} for Abstract Syntax {} with Transfer Syntax {} that was not amongst those requested - treating it as rejected",Integer.toHexString(accid),abstractSyntaxUID,acceptedTransferSyntaxUID);
					}
				}
			}
		}
		return presentationContexts;
	}
	
	/**
	 * @throws	DicomNetworkException
	 */
	public LinkedList getRequestedPresentationContexts() throws DicomNetworkException {
		LinkedList presentationContexts = new LinkedList();
		ListIterator itemi = itemList.listIterator();
		while (itemi.hasNext()) {
			AssociationItem item = (AssociationItem)(itemi.next());
			if (item.type == 0x20) {						// Presentation Context Item (request)
				presentationContexts.add(((PresentationContextItem)item).pc);
			}
		}
		return presentationContexts;
	}
	
	/**
	 * @throws	DicomNetworkException
	 */
	public LinkedList getSCUSCPRoleSelections() throws DicomNetworkException {
		LinkedList selections = new LinkedList();
		ListIterator itemi = itemList.listIterator();
		while (itemi.hasNext()) {
			AssociationItem item = (AssociationItem)(itemi.next());
			if (item.type == 0x50) {		// User Information Item
				UserInformationItem uii = (UserInformationItem)item;
				LinkedList subItemList = uii.subItemList;
				ListIterator subItemi = subItemList.listIterator();
				while (subItemi.hasNext()) {
					UserInformationSubItem subItem = (UserInformationSubItem)subItemi.next();
					if (subItem.type == 0x54) {	// SCU/SCP Role Selection Sub-item
						SCUSCPRoleSelectionUserInformationSubItem ssrsuisi = (SCUSCPRoleSelectionUserInformationSubItem)subItem;
						SCUSCPRoleSelection ssrs = ssrsuisi.selection;
						selections.add(ssrs);
					}
				}
			}
		}
		return selections;
	}
	
	/***/
	public int getMaximumLengthReceived() { return maximumLengthReceived; }
	/***/
	public String getCallingAETitle() { return callingAETitle; }
	/***/
	public String getCalledAETitle() { return calledAETitle; }

	/***/
	public String toString() { return toStringFromObjectContents(); }

	/***/
	public String toStringFromObjectContents() {

		StringBuffer sb = new StringBuffer();

		sb.append(HexDump.dump(b));

		sb.append("PDU Type: 0x");
		sb.append(pduType);
		sb.append(b[0] == 0x01 ? " (A-ASSOCIATE-RQ)" : (b[0] == 0x02 ? " (A-ASSOCIATE-AC)" : " unrecognized"));
		sb.append("\n");

		sb.append("Length: 0x");
		sb.append(Integer.toHexString(pduLength));
		sb.append("\n");

		sb.append("Protocol Version: 0x");
		sb.append(Integer.toHexString(protocolVersion));
		sb.append("\n");

		sb.append("Called AET:  ");
		sb.append(calledAETitle);

		sb.append("\n");

		sb.append("Calling AET: ");
		sb.append(callingAETitle);
		sb.append("\n");

		ListIterator i = itemList.listIterator();
		while (i.hasNext()) {
			AssociationItem item = (AssociationItem)(i.next());
			sb.append(item);
		}
		return sb.toString();
	}

	/***/
	public String toStringFromRawPDUBytes() {

		StringBuffer sb = new StringBuffer();

		sb.append(HexDump.dump(b));

		sb.append("PDU Type: 0x");
		sb.append(Integer.toHexString(b[0]&0xff));
		sb.append(b[0] == 0x01 ? " (A-ASSOCIATE-RQ)" : (b[0] == 0x02 ? " (A-ASSOCIATE-AC)" : " unrecognized"));
		sb.append("\n");

		sb.append("Length: 0x");
		sb.append(Integer.toHexString(ByteArray.bigEndianToUnsignedInt(b,2,4)));
		sb.append("\n");

		sb.append("Protocol Version: 0x");
		sb.append(Integer.toHexString(ByteArray.bigEndianToUnsignedInt(b,6,2)));
		sb.append("\n");

		sb.append("Called AET:  ");
		sb.append(new String(b,10,16));

		sb.append("\n");

		sb.append("Calling AET: ");
		sb.append(new String(b,26,16));
		sb.append("\n");

		int offset = 74;
		while (offset+3 < b.length) {
			int itemType = b[offset++]&0xff;
			offset++;			// skip reserved byte
			int lng = ByteArray.bigEndianToUnsignedInt(b,offset,2);	// all items use 2-byte big-endian length field
			offset+=2;

			sb.append("Item Type: 0x");
			sb.append(Integer.toHexString(itemType));
			sb.append(" (length 0x");
			sb.append(Integer.toHexString(lng));
			sb.append(")");

			if (itemType == 0x10) {		// Application Context Item
				sb.append(" (Application Context)\n\t");
				sb.append(new String(b,offset,lng));
				sb.append("\n");
				offset+=lng;
			}
			else if (itemType == 0x20 || itemType == 0x21) {		// Presentation Context Item (request or accept)
				sb.append(" (Presentation Context)\n");
				sb.append("\tID: 0x");
				sb.append(Integer.toHexString(b[offset]&0xff));
				sb.append("\n");
				offset+=4; lng-=4;		// ID and 3 reserved bytes
				while (lng > 0) {
					int subItemType = b[offset++]&0xff; --lng;
					++offset; --lng;	// reserved byte
					int silng = ByteArray.bigEndianToUnsignedInt(b,offset,2);	// all sub-items use 2-byte big-endian length field
					offset+=2; lng-=2;

					sb.append("\tSub-Item Type: 0x");
					sb.append(Integer.toHexString(subItemType));
					sb.append(" (length 0x");
					sb.append(Integer.toHexString(silng));
					sb.append(")");

					if (subItemType == 0x30) {
						sb.append(" (Abstract Syntax)\n\t\t");
						sb.append(new String(b,offset,silng));
						sb.append("\n");
					}
					else if (subItemType == 0x40) {
						sb.append(" (Transfer Syntax)\n\t\t");
						sb.append(new String(b,offset,silng));
						sb.append("\n");
					}
					else {
						sb.append(" (unrecognized)\n");
					}
					offset+=silng; lng-=silng;
				}
			}
			else {
				sb.append(" (unrecognized)\n");
				offset+=lng;
			}
		}

		return sb.toString();
	}

}




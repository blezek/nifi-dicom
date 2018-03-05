/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.lang.Integer;
import java.lang.StringBuffer;

/**
 * @author	dclunie
 */
public class PresentationContext {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/PresentationContext.java,v 1.14 2017/01/24 10:50:45 dclunie Exp $";

	/***/
	private byte identifier;
	/***/
	private byte resultReason;			// this byte is zero (reserved) in request item
	/***/
	private String abstractSyntaxUID;
	/***/
	private LinkedList transferSyntaxUIDs;

	/**
	 * @param	identifier
	 * @param	resultReason
	 * @param	abstractSyntaxUID
	 */
	private void initialize(byte identifier, byte resultReason, String abstractSyntaxUID) {
		initializeSansLinkedList(identifier,resultReason,abstractSyntaxUID);
		newTransferSyntaxUIDs();
	}

	/**
	 * @param	identifier
	 * @param	resultReason
	 * @param	abstractSyntaxUID
	 */
	private void initializeSansLinkedList(byte identifier, byte resultReason, String abstractSyntaxUID) {
		this.identifier=identifier;
		this.resultReason=resultReason;
		this.abstractSyntaxUID=abstractSyntaxUID;
	}

	/**
	 * @param	identifier
	 */
	public PresentationContext(byte identifier) {
		initialize(identifier,(byte)0,null);
	}

	/**
	 * @param	identifier
	 * @param	abstractSyntaxUID
	 */
	public PresentationContext(byte identifier, String abstractSyntaxUID) {
		initialize(identifier,(byte)0,abstractSyntaxUID);
	}

	/**
	 * @param	identifier
	 * @param	abstractSyntaxUID
	 * @param	transferSyntaxUID
	 */
	public PresentationContext(byte identifier, String abstractSyntaxUID, String transferSyntaxUID) {
		initialize(identifier,(byte)0,abstractSyntaxUID);
		this.transferSyntaxUIDs.add(transferSyntaxUID);
	}

	/**
	 * @param	identifier
	 * @param	abstractSyntaxUID
	 * @param	transferSyntaxUIDs
	 */
	public PresentationContext(byte identifier, String abstractSyntaxUID, LinkedList transferSyntaxUIDs) {
		initializeSansLinkedList(identifier,(byte)0,abstractSyntaxUID);
		this.transferSyntaxUIDs=transferSyntaxUIDs;
	}

	/**
	 * @param	identifier
	 * @param	resultReason
	 */
	public PresentationContext(byte identifier, byte resultReason) {
		initialize(identifier,resultReason,null);
	}

	/**
	 * @param	identifier
	 * @param	resultReason
	 * @param	abstractSyntaxUID
	 */
	public PresentationContext(byte identifier, byte resultReason, String abstractSyntaxUID) {
		initialize(identifier,resultReason,abstractSyntaxUID);
	}

	/**
	 * @param	identifier
	 * @param	resultReason
	 * @param	abstractSyntaxUID
	 * @param	transferSyntaxUID
	 */
	public PresentationContext(byte identifier, byte resultReason, String abstractSyntaxUID, String transferSyntaxUID) {
		initialize(identifier,resultReason,abstractSyntaxUID);
		this.transferSyntaxUIDs.add(transferSyntaxUID);
	}

	/**
	 * @param	identifier
	 * @param	resultReason
	 * @param	abstractSyntaxUID
	 * @param	transferSyntaxUIDs
	 */
	public PresentationContext(byte identifier, byte resultReason, String abstractSyntaxUID, LinkedList transferSyntaxUIDs) {
		initializeSansLinkedList(identifier,resultReason,abstractSyntaxUID);
		this.transferSyntaxUIDs=transferSyntaxUIDs;
	}

	/***/
	public void newTransferSyntaxUIDs() {
		this.transferSyntaxUIDs = new LinkedList();
	}

	/**
	 * @param	transferSyntaxUID
	 */
	public void addTransferSyntaxUID(String transferSyntaxUID) {
		this.transferSyntaxUIDs.add(transferSyntaxUID);
	}

	/***/
	public List getTransferSyntaxUIDs() {		// They don't need to know it is a LinkedList
		return transferSyntaxUIDs;
	}

	/***/
	public String getTransferSyntaxUID() {
		return transferSyntaxUIDs.size() > 0 ? (String)transferSyntaxUIDs.getFirst() : null;
	}

	/***/
	public String getAbstractSyntaxUID() {
		return abstractSyntaxUID;
	}

	/**
	 * @param	uid
	 */
	public void setAbstractSyntaxUID(String uid) {
		abstractSyntaxUID=uid;
	}

	/***/
	public byte getIdentifier() 			{ return identifier; }
	/**
	 * @param	identifier
	 */
	public void setIdentifier(byte identifier) 	{ this.identifier=identifier; }

	/***/
	public byte getResultReason() 			{ return resultReason; }
	/**
	 * @param	resultReason
	 */
	public void setResultReason(byte resultReason) 	{ this.resultReason=resultReason; }

	/***/
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Presentation Context ID: 0x");
		sb.append(Integer.toHexString(identifier&0xff));
		sb.append(" (result 0x");
		sb.append(Integer.toHexString(resultReason&0xff));
		sb.append(" - ");
		if (resultReason == 0) {
			sb.append("acceptance");
		}
		else if (resultReason == 1) {
			sb.append("user rejection");
		}
		else if (resultReason == 2) {
			sb.append("no reason (provider rejection)");
		}
		else if (resultReason == 3) {
			sb.append("abstract syntax not supported (provider rejection)");
		}
		else if (resultReason == 4) {
			sb.append("transfer syntaxes not supported (provider rejection)");
		}
		else {
			sb.append("unrecognized");
		}
		sb.append(")\n");
		sb.append("\tAbstract Syntax:\n\t\t");
		sb.append(abstractSyntaxUID);
		sb.append("\n");
		sb.append("\tTransfer Syntax(es):");

		ListIterator i = transferSyntaxUIDs.listIterator();
		while (i.hasNext()) {
			sb.append("\n\t\t");
			sb.append((String)i.next());
		}

		sb.append("\n");

		return sb.toString();
	}
}



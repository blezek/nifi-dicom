/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CFindRequestCommandMessage extends RequestCommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CFindRequestCommandMessage.java,v 1.14 2017/01/24 10:50:44 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageID;
	private int priority;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CFindRequestCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		              priority = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Priority,0xffff);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
		             messageID = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageID,0xffff);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CFindRequestCommandMessage(String affectedSOPClassUID) throws DicomException, IOException {
		
		this.affectedSOPClassUID=affectedSOPClassUID;
		   commandField = MessageServiceElementCommand.C_FIND_RQ;
		      messageID = super.getNextAvailableMessageID();
		       priority = 0x0000;	// MEDIUM
		int dataSetType = 0x0102;	// anything other than 0x0101 (none), since a C-FIND-RQ always has a data set (the "identifier")
						// use 0102 ("identifier") as per note in PS 3.7 9.3.2.1
		
		// NB. The Affected SOP Class UID should have no extra trailing padding, otherwise the
		// SCP may fail and send an A-ABORT :) (Part 5 says one null (not space) is allowed)
		// This is taken care of by the Attribute.write()

		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                     Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                      list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;    Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPClassUID);    list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;           Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);           list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageID;              Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageID);              list.put(t,a); }
		{ AttributeTag t = TagFromName.Priority;               Attribute a = new UnsignedShortAttribute(t);    a.addValue(priority);               list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;            Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);            list.put(t,a); }

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DicomOutputStream dout = new DicomOutputStream(bout,null/* no meta-header */,TransferSyntax.ImplicitVRLittleEndian);
		list.write(dout);
		bytes = bout.toByteArray();

		groupLength = bytes.length-12;
		bytes[8]=(byte)groupLength;					// little endian
		bytes[9]=(byte)(groupLength>>8);
		bytes[10]=(byte)(groupLength>>16);
		bytes[11]=(byte)(groupLength>>24);
//System.err.println("CFindRequestCommandMessage: bytes="+HexDump.dump(bytes));
	}
	
	/***/
	public int getGroupLength()			{ return groupLength; }
	/***/
	public String getAffectedSOPClassUID()		{ return affectedSOPClassUID; }		// unpadded
	/***/
	public int getCommandField()			{ return commandField; }
	/***/
	public int getMessageID()			{ return messageID; }
	/***/
	public int getPriority()			{ return priority; }

	/***/
	public byte[] getBytes() { return bytes; }
}

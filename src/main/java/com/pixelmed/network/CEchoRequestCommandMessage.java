/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CEchoRequestCommandMessage extends RequestCommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CEchoRequestCommandMessage.java,v 1.13 2017/01/24 10:50:44 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageID;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CEchoRequestCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
		             messageID = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageID,0xffff);
	}
	
	/**
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CEchoRequestCommandMessage() throws DicomException, IOException {
		
		   commandField = MessageServiceElementCommand.C_ECHO_RQ;
		      messageID = super.getNextAvailableMessageID();
		int dataSetType = 0x0101;	// none
		
		// NB. The Affected SOP Class UID should have no extra trailing padding, otherwise the
		// SCP may fail and send an A-ABORT :) (Part 5 says one null (not space) is allowed)
		// This is taken care of by the Attribute.write()

		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                     Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                      list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;    Attribute a = new UniqueIdentifierAttribute(t); a.addValue(SOPClass.Verification);  list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;           Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);           list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageID;              Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageID);              list.put(t,a); }
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
//System.err.println("CEchoRequestCommandMessage: bytes="+HexDump.dump(bytes));
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
	public byte[] getBytes() { return bytes; }
}

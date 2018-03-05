/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CEchoResponseCommandMessage implements CommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CEchoResponseCommandMessage.java,v 1.13 2017/01/24 10:50:44 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageIDBeingRespondedTo;
	private int status;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CEchoResponseCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
	     messageIDBeingRespondedTo = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageIDBeingRespondedTo,0xffff);
		                status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	messageIDBeingRespondedTo
	 * @param	status
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CEchoResponseCommandMessage(String affectedSOPClassUID,int messageIDBeingRespondedTo,int status) throws DicomException, IOException {
		
		   commandField = MessageServiceElementCommand.C_ECHO_RSP;
		int dataSetType = 0x0101;	// none
		
		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                        Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                         list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;       Attribute a = new UniqueIdentifierAttribute(t); a.addValue(SOPClass.Verification);     list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;              Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);              list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageIDBeingRespondedTo; Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageIDBeingRespondedTo); list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;               Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);               list.put(t,a); }
		{ AttributeTag t = TagFromName.Status;                    Attribute a = new UnsignedShortAttribute(t);    a.addValue(status);                    list.put(t,a); }

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DicomOutputStream dout = new DicomOutputStream(bout,null/* no meta-header */,TransferSyntax.ImplicitVRLittleEndian);
		list.write(dout);
		bytes = bout.toByteArray();

		groupLength = bytes.length-12;
		bytes[8]=(byte)groupLength;					// little endian
		bytes[9]=(byte)(groupLength>>8);
		bytes[10]=(byte)(groupLength>>16);
		bytes[11]=(byte)(groupLength>>24);
//System.err.println("CEchoResponseCommandMessage: bytes="+HexDump.dump(bytes));
	}
	
	/***/
	public int getGroupLength()			{ return groupLength; }
	/***/
	public String getAffectedSOPClassUID()		{ return affectedSOPClassUID; }		// unpadded
	/***/
	public int getCommandField()			{ return commandField; }
	/***/
	public int getMessageIDBeingRespondedTo()	{ return messageIDBeingRespondedTo; }
	/***/
	public int getStatus()				{ return status; }

	/***/
	public byte[] getBytes() { return bytes; }
}

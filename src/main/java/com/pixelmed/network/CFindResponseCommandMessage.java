/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CFindResponseCommandMessage implements CommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CFindResponseCommandMessage.java,v 1.8 2017/01/24 10:50:44 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageIDBeingRespondedTo;
	private int status;
	private AttributeTagAttribute offendingElement;
	private String errorComment;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CFindResponseCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
		                status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
	     messageIDBeingRespondedTo = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageIDBeingRespondedTo,0xffff);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	messageIDBeingRespondedTo
	 * @param	status
	 * @param	dataSetPresent
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CFindResponseCommandMessage(String affectedSOPClassUID,
			int messageIDBeingRespondedTo,int status,boolean dataSetPresent) throws DicomException, IOException {
		this(affectedSOPClassUID,messageIDBeingRespondedTo,status,dataSetPresent,null,null);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	messageIDBeingRespondedTo
	 * @param	status
	 * @param	dataSetPresent
	 * @param	offendingElement
	 * @param	errorComment
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CFindResponseCommandMessage(String affectedSOPClassUID,
			int messageIDBeingRespondedTo,int status,boolean dataSetPresent,
			AttributeTagAttribute offendingElement,String errorComment) throws DicomException, IOException {
		
		this.affectedSOPClassUID=affectedSOPClassUID;
		commandField = MessageServiceElementCommand.C_FIND_RSP;
		this.messageIDBeingRespondedTo=messageIDBeingRespondedTo;
		this.status=status;
		int dataSetType = dataSetPresent ? 0x0001 : 0x0101;
		this.offendingElement = offendingElement;
		this.errorComment = errorComment;
		
		// NB. The Affected SOP Class UID should have no extra trailing padding, otherwise the
		// SCP may fail and send an A-ABORT :) (Part 5 says one null (not space) is allowed)
		// This is taken care of by the Attribute.write()

		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                        Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                         list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;       Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPClassUID);       list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;              Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);              list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageIDBeingRespondedTo; Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageIDBeingRespondedTo); list.put(t,a); }
		{ AttributeTag t = TagFromName.Status;                    Attribute a = new UnsignedShortAttribute(t);    a.addValue(status);                    list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;               Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);               list.put(t,a); }

		if (offendingElement != null) {
			list.put(offendingElement);
		}
		
		if (errorComment != null) {
			AttributeTag t = TagFromName.ErrorComment;
			Attribute a = new LongStringAttribute(t);
			a.addValue(errorComment);
			list.put(t,a);
		}
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DicomOutputStream dout = new DicomOutputStream(bout,null/* no meta-header */,TransferSyntax.ImplicitVRLittleEndian);
		list.write(dout);
		bytes = bout.toByteArray();

		groupLength = bytes.length-12;
		bytes[8]=(byte)groupLength;					// little endian
		bytes[9]=(byte)(groupLength>>8);
		bytes[10]=(byte)(groupLength>>16);
		bytes[11]=(byte)(groupLength>>24);
//System.err.println("CFindResponseCommandMessage: bytes="+HexDump.dump(bytes));
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
	public AttributeTagAttribute getOffendingElement()	{ return offendingElement; }
	/***/
	public String getErrorComment()		{ return errorComment; }

	/***/
	public byte[] getBytes() { return bytes; }
}

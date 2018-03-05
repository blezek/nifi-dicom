/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CStoreRequestCommandMessage extends RequestCommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CStoreRequestCommandMessage.java,v 1.16 2017/01/24 10:50:45 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageID;
	private int priority;
	private String affectedSOPInstanceUID;		// unpadded
	private String moveOriginatorApplicationEntityTitle;
	private int moveOriginatorMessageID;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CStoreRequestCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
		             messageID = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageID,0xffff);
		              priority = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Priority,0xffff);
		affectedSOPInstanceUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPInstanceUID);
	     moveOriginatorApplicationEntityTitle = Attribute.getSingleStringValueOrNull(list,TagFromName.MoveOriginatorApplicationEntityTitle);
	   moveOriginatorMessageID = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MoveOriginatorMessageID,-1);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	affectedSOPInstanceUID
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CStoreRequestCommandMessage(String affectedSOPClassUID,String affectedSOPInstanceUID) throws DicomException, IOException {
		this(affectedSOPClassUID,affectedSOPInstanceUID,null,-1);
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	affectedSOPInstanceUID
	 * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
	 * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CStoreRequestCommandMessage(String affectedSOPClassUID,String affectedSOPInstanceUID,String moveOriginatorApplicationEntityTitle,int moveOriginatorMessageID) throws DicomException, IOException {
		this.affectedSOPClassUID=affectedSOPClassUID;
		this.affectedSOPInstanceUID=affectedSOPInstanceUID;
		this.moveOriginatorApplicationEntityTitle=moveOriginatorApplicationEntityTitle;
		this.moveOriginatorMessageID=moveOriginatorMessageID;
		
		   commandField = 0x0001;	// C-STORE-RQ
		      messageID = super.getNextAvailableMessageID();
		       priority = 0x0000;	// MEDIUM
		int dataSetType = 0x0001;	// anything other than 0x0101 (none), since a C-STORE-RQ always has a data set
		
		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                     Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                      list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;    Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPClassUID);    list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;           Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);           list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageID;              Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageID);              list.put(t,a); }
		{ AttributeTag t = TagFromName.Priority;               Attribute a = new UnsignedShortAttribute(t);    a.addValue(priority);               list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;            Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);            list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPInstanceUID); list.put(t,a); }

		if (moveOriginatorApplicationEntityTitle != null && moveOriginatorApplicationEntityTitle.length() > 0) {
			AttributeTag t = TagFromName.MoveOriginatorApplicationEntityTitle; Attribute a = new ApplicationEntityAttribute(t); a.addValue(moveOriginatorApplicationEntityTitle); list.put(t,a);
		}
		if (moveOriginatorMessageID != -1) {
			AttributeTag t = TagFromName.MoveOriginatorMessageID; Attribute a = new UnsignedShortAttribute(t); a.addValue(moveOriginatorMessageID); list.put(t,a);
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
//System.err.println("CStoreRequestCommandMessage: bytes="+HexDump.dump(bytes));
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
	public String getAffectedSOPInstanceUID()	{ return affectedSOPInstanceUID; }		// unpadded
	/***/
	public String getmoveOriginatorApplicationEntityTitle()	{ return moveOriginatorApplicationEntityTitle; }
	/***/
	public int getMoveOriginatorMessageID()		{ return moveOriginatorMessageID; }

	/***/
	public byte[] getBytes() { return bytes; }
}

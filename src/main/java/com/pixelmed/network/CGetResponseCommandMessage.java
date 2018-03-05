/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class CGetResponseCommandMessage implements CommandMessage {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/CGetResponseCommandMessage.java,v 1.7 2017/01/24 10:50:44 dclunie Exp $";

	private byte bytes[];

	private static final AttributeTag groupLengthTag = new AttributeTag(0x0000,0x0000);
	private int groupLength;
	private String affectedSOPClassUID;		// unpadded
	private int commandField;
	private int messageIDBeingRespondedTo;
	private int status;
	private int nRemaining;
	private int nCompleted;
	private int nFailed;
	private int nWarning;
	private AttributeTagAttribute offendingElement;
	private String errorComment;
	
	/**
	 * @param	list
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CGetResponseCommandMessage(AttributeList list) throws DicomException, IOException {
		           groupLength = Attribute.getSingleIntegerValueOrDefault(list,groupLengthTag,0xffff);
		   affectedSOPClassUID = Attribute.getSingleStringValueOrNull    (list,TagFromName.AffectedSOPClassUID);
		          commandField = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.CommandField,0xffff);
		                status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
	     messageIDBeingRespondedTo = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.MessageIDBeingRespondedTo,0xffff);
			    nRemaining = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfRemainingSuboperations,0);
			    nCompleted = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfCompletedSuboperations,0);
			       nFailed = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFailedSuboperations,0);
			      nWarning = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfWarningSuboperations,0);
		      offendingElement = (AttributeTagAttribute)(list.get(TagFromName.OffendingElement));
		          errorComment = Attribute.getSingleStringValueOrNull(list,TagFromName.ErrorComment);
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
	public CGetResponseCommandMessage(String affectedSOPClassUID,
			int messageIDBeingRespondedTo,int status,boolean dataSetPresent,
			AttributeTagAttribute offendingElement,String errorComment) throws DicomException, IOException {
		
		this.affectedSOPClassUID=affectedSOPClassUID;
		commandField = MessageServiceElementCommand.C_GET_RSP;
		this.messageIDBeingRespondedTo=messageIDBeingRespondedTo;
		this.status=status;
		int dataSetType = dataSetPresent ? 0x0001 : 0x0101;
		this.nRemaining = 0;
		this.nCompleted = 0;
		this.nFailed = 0;
		this.nWarning = 0;
		this.offendingElement = offendingElement;
		this.errorComment = errorComment;
		
		// NB. The Affected SOP Class UID should have no extra trailing padding, otherwise the
		// SCP may fail and send an A-ABORT :) (Part 5 says one null (not space) is allowed)
		// This is taken care of by the Attribute.write()

		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                             Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                         list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;            Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPClassUID);       list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;                   Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);              list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageIDBeingRespondedTo;      Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageIDBeingRespondedTo); list.put(t,a); }
		{ AttributeTag t = TagFromName.Status;                         Attribute a = new UnsignedShortAttribute(t);    a.addValue(status);                    list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;                    Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);               list.put(t,a); }
		
		if (offendingElement != null) {
			list.put(offendingElement);
		}
		
		if (errorComment != null) {
			AttributeTag t = TagFromName.CommandDataSetType;
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
//System.err.println("CGetResponseCommandMessage: bytes="+HexDump.dump(bytes));
	}
	
	/**
	 * @param	affectedSOPClassUID
	 * @param	messageIDBeingRespondedTo
	 * @param	status
	 * @param	dataSetPresent
	 * @param	nRemaining
	 * @param	nCompleted
	 * @param	nFailed
	 * @param	nWarning
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public CGetResponseCommandMessage(String affectedSOPClassUID,
			int messageIDBeingRespondedTo,int status,boolean dataSetPresent,
			int nRemaining,int nCompleted,int nFailed,int nWarning) throws DicomException, IOException {
		
		this.affectedSOPClassUID=affectedSOPClassUID;
		commandField = MessageServiceElementCommand.C_GET_RSP;
		this.messageIDBeingRespondedTo=messageIDBeingRespondedTo;
		this.status=status;
		int dataSetType = dataSetPresent ? 0x0001 : 0x0101;
		this.nRemaining = nRemaining;
		this.nCompleted = nCompleted;
		this.nFailed = nFailed;
		this.nWarning = nWarning;
		this.offendingElement = null;
		this.errorComment = null;
		
		// NB. The Affected SOP Class UID should have no extra trailing padding, otherwise the
		// SCP may fail and send an A-ABORT :) (Part 5 says one null (not space) is allowed)
		// This is taken care of by the Attribute.write()

		AttributeList list = new AttributeList();		
		{ AttributeTag t = groupLengthTag;                             Attribute a = new UnsignedLongAttribute(t);     a.addValue(0);                         list.put(t,a); }
		{ AttributeTag t = TagFromName.AffectedSOPClassUID;            Attribute a = new UniqueIdentifierAttribute(t); a.addValue(affectedSOPClassUID);       list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandField;                   Attribute a = new UnsignedShortAttribute(t);    a.addValue(commandField);              list.put(t,a); }
		{ AttributeTag t = TagFromName.MessageIDBeingRespondedTo;      Attribute a = new UnsignedShortAttribute(t);    a.addValue(messageIDBeingRespondedTo); list.put(t,a); }
		{ AttributeTag t = TagFromName.Status;                         Attribute a = new UnsignedShortAttribute(t);    a.addValue(status);                    list.put(t,a); }
		{ AttributeTag t = TagFromName.CommandDataSetType;                    Attribute a = new UnsignedShortAttribute(t);    a.addValue(dataSetType);               list.put(t,a); }
		if (nRemaining > 0) {	// not to be included unless pending status (may be included with cancelled status)
			AttributeTag t = TagFromName.NumberOfRemainingSuboperations; Attribute a = new UnsignedShortAttribute(t);    a.addValue(nRemaining);                list.put(t,a);
		}
		{ AttributeTag t = TagFromName.NumberOfCompletedSuboperations; Attribute a = new UnsignedShortAttribute(t);    a.addValue(nCompleted);                list.put(t,a); }
		{ AttributeTag t = TagFromName.NumberOfFailedSuboperations;    Attribute a = new UnsignedShortAttribute(t);    a.addValue(nFailed);                   list.put(t,a); }
		{ AttributeTag t = TagFromName.NumberOfWarningSuboperations;   Attribute a = new UnsignedShortAttribute(t);    a.addValue(nWarning);                  list.put(t,a); }
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DicomOutputStream dout = new DicomOutputStream(bout,null/* no meta-header */,TransferSyntax.ImplicitVRLittleEndian);
		list.write(dout);
		bytes = bout.toByteArray();

		groupLength = bytes.length-12;
		bytes[8]=(byte)groupLength;					// little endian
		bytes[9]=(byte)(groupLength>>8);
		bytes[10]=(byte)(groupLength>>16);
		bytes[11]=(byte)(groupLength>>24);
//System.err.println("CGetResponseCommandMessage: bytes="+HexDump.dump(bytes));
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
	public int getNumberOfRemainingSuboperations()	{ return nRemaining; }
	/***/
	public int getNumberOfCompletedSuboperations()	{ return nCompleted; }
	/***/
	public int getNumberOfFailedSuboperations()	{ return nFailed; }
	/***/
	public int getNumberOfWarningSuboperations()	{ return nWarning; }
	/***/
	public AttributeTagAttribute getOffendingElement()	{ return offendingElement; }

	/***/
	public byte[] getBytes() { return bytes; }
}

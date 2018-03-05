/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the header portion of an SCP-ECG section.</p>
 *
 * @author	dclunie
 */
public class SectionHeader {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/SectionHeader.java,v 1.9 2017/01/24 10:50:47 dclunie Exp $";

	private int sectionCRC;
	private int sectionIDNumber;
	private long sectionLength;
	private int sectionVersionNumber;
	private int protocolVersionNumber;
	private byte[] reserved = new byte[6];
		
	private long bytesRead;
	private long byteOffset;
	
	public int getSectionCRC() { return sectionCRC; }
	public int getSectionIDNumber() { return sectionIDNumber; }
	public long getSectionLength() { return sectionLength; }
	public int getSectionVersionNumber() { return sectionVersionNumber; }
	public int getProtocolVersionNumber() { return protocolVersionNumber; }
	public byte[] getReservedBytes() { return reserved; }
	public long getBytesRead() { return bytesRead; }
	public long getByteOffset() { return byteOffset; }
		
	/**
	 * <p>Read the section header from a stream.</p>
	 *
	 * @param	i		the input stream
	 * @param	byteOffset	byte offset
	 * @return			the number of bytes read
	 */
	public long read(BinaryInputStream i,long byteOffset) throws IOException {
		this.byteOffset=byteOffset;
		bytesRead=0;
		sectionCRC = i.readUnsigned16();
		bytesRead+=2;		
		sectionIDNumber = i.readUnsigned16();
		bytesRead+=2;		
		sectionLength = i.readUnsigned32();
		bytesRead+=4;		
		sectionVersionNumber = i.readUnsigned8();
		bytesRead++;		
		protocolVersionNumber = i.readUnsigned8();
		bytesRead++;
		i.readInsistently(reserved,0,6);
		bytesRead+=6;
			
		return bytesRead;
	}
		
	/**
	 * <p>Dump the header as a <code>String</code>.</p>
	 *
	 * @return		the header as a <code>String</code>
	 */
	public String toString() {
		return "[Byte offset = "+byteOffset+" dec (0x"+Long.toHexString(byteOffset)+")]\n"
		     + "Section CRC = "+sectionCRC+" dec (0x"+Integer.toHexString(sectionCRC)+")\n"
		     + "Section ID Number = "+sectionIDNumber+" dec (0x"+Integer.toHexString(sectionIDNumber)+")\n"
		     + "Section Length = "+sectionLength+" dec (0x"+Long.toHexString(sectionLength)+")\n"
		     + "Section Version Number = "+sectionVersionNumber+" dec (0x"+Integer.toHexString(sectionVersionNumber)+")\n"
		     + "Protocol Version Number = "+protocolVersionNumber+" dec (0x"+Integer.toHexString(protocolVersionNumber)+")\n";
	}

	/***/
	protected SCPTreeRecord tree;
	
	/**
	 * <p>Get the contents of the header as a tree for display.</p>
	 *
	 * @return		the section as a tree, or null if not constructed
	 */
	public SCPTreeRecord getTree() { return tree; }

	/**
	 * <p>Get the contents of the header as a tree for display, constructing it if not already done.</p>
	 *
	 * @param	parent	the node to which this section is to be added if it needs to be created de novo
	 * @return		the header as a tree
	 */
	public SCPTreeRecord getTree(SCPTreeRecord parent) {
		if (tree == null) {
			SCPTreeRecord tree = new SCPTreeRecord(parent," Header");	// space to make it sort first in tree browser
			new SCPTreeRecord(tree,"Byte offset",Long.toString(byteOffset)+" dec (0x"+Long.toHexString(byteOffset)+")");
			new SCPTreeRecord(tree,"Section CRC",Long.toString(sectionCRC)+" dec (0x"+Long.toHexString(sectionCRC)+")");
			new SCPTreeRecord(tree,"Section ID Number",Long.toString(sectionIDNumber)+" dec (0x"+Long.toHexString(sectionIDNumber)+")");
			new SCPTreeRecord(tree,"Section Length",Long.toString(sectionLength)+" dec (0x"+Long.toHexString(sectionLength)+")");
			new SCPTreeRecord(tree,"Section Version Number",Long.toString(sectionVersionNumber)+" dec (0x"+Long.toHexString(sectionVersionNumber)+")");
			new SCPTreeRecord(tree,"Protocol Version Number",Long.toString(protocolVersionNumber)+" dec (0x"+Long.toHexString(protocolVersionNumber)+")");
		}
		return tree;
	}
}
	

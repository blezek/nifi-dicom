/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate an SCP-ECG section.</p>
 *
 * <p>Though not abstract, in order to support unrecognized sections, this class is usually extended
 * by more specific classes; there is a factory method {@link #makeSection(SectionHeader,TreeMap) makeSection}
 * that is used to create specific sub-classes once the section
 * number is known (i.e. has been read).</p>
 *
 * @author	dclunie
 */
public class Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section.java,v 1.17 2017/01/24 10:50:47 dclunie Exp $";

	protected SectionHeader header;
	protected long bytesRead;
	protected long sectionBytesRemaining;

	public SectionHeader getSectionHeader() { return header; }
		
	/**
	 * <p>Construct an empty section with the specified header.</p>
	 *
	 * @param	header	the header (which has already been read)
	 */
	public Section(SectionHeader header) {
		this.header=header;
		bytesRead=0;
		sectionBytesRemaining = header.getSectionLength()-header.getBytesRead();
	}

	/**
	 * <p>Read the remainder of the section from a stream.</p>
	 *
	 * @param	i	the input stream
	 * @return		the number of bytes read
	 */
	public long read(BinaryInputStream i) throws IOException {
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}

	/**
	 * <p>Skip to the end of the section, if not already there.</p>
	 *
	 * <p>Used either for unrecognized sections, or when there is an encoding error within a section
	 * and parsing of the section has to be abandoned.</p>
	 *
	 * @param	i	the input stream
	 * @return		the number of bytes skipped
	 */
	protected long skipToEndOfSectionIfNotAlreadyThere(BinaryInputStream i) throws IOException {
		long bytesSkipped=sectionBytesRemaining;
		if (sectionBytesRemaining > 0) {
			bytesRead+=sectionBytesRemaining;
			i.skipInsistently(sectionBytesRemaining);
			sectionBytesRemaining=0;
		}
		return bytesSkipped;
	}

	/**
	 * <p>Dump the section as a <code>String</code>.</p>
	 *
	 * @return		the section as a <code>String</code>
	 */
	public String toString() {
		return "\n";
	}

	/**
	 * <p>Validate the section against the standard.</p>
	 *
	 * @return		the validation results as a <code>String</code>
	 */
	public String validate() {
		return "";
	}

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return ""; }

	/**
	 * <p>A factory method to construct specific sub-classes of section.</p>
	 *
	 * @param	sectionHeader	the section header already read from the stream, containing the section number
	 * @param	sections	the sections that have already been read, in case values are needed for making new sections
	 * @return			a section of the appropriate sub-class.
	 */
	public static Section makeSection(SectionHeader sectionHeader,TreeMap sections) {
		Section section = null;
		switch (sectionHeader.getSectionIDNumber()) {
			case 0:		section = new Section0(sectionHeader);
					break;
			case 1:		section = new Section1(sectionHeader);
					break;
			case 2:		section = new Section2(sectionHeader);
					break;
			case 3:		section = new Section3(sectionHeader);
					break;
			case 4:		section = new Section4(sectionHeader);
					break;
			case 5:
			case 6:		{
						Section3 section3 = (Section3)(sections.get(new Integer(3)));
						int       numberOfleads = section3 == null ?    0 : section3.getNumberOfLeads();
						section = new Section5Or6(sectionHeader,numberOfleads);
					}
					break;
			case 7:		section = new Section7(sectionHeader);
					break;
			case 8:
			case 11:
					section = new Section8Or11(sectionHeader);
					break;
			case 10:	section = new Section10(sectionHeader);
					break;
			default:	section = new Section(sectionHeader);
					break;
		}
		return section;
	}
	
	/***/
	protected SCPTreeRecord tree;
	
	/**
	 * <p>Get the value to use as the value section of the section node in a tree for display.</p>
	 *
	 * @return		the value of just this node (not its contents)
	 */
	protected String getValueForSectionNodeInTree() {
		return Integer.toString(header.getSectionIDNumber())+" "+getSectionName();
	}

	/**
	 * <p>Get section header information to the section node in a tree for display.</p>
	 */
	protected void addSectionHeaderToTree(SCPTreeRecord parent) {
		header.getTree(parent);
	}

	/**
	 * <p>Get the contents of the section as a tree for display.</p>
	 *
	 * @return		the section as a tree, or null if not constructed
	 */
	public SCPTreeRecord getTree() { return tree; }

	/**
	 * <p>Get the contents of the section as a tree for display, constructing it if not already done.</p>
	 *
	 * @param	parent	the node to which this section is to be added if it needs to be created de novo
	 * @return		the section as a tree
	 */
	public SCPTreeRecord getTree(SCPTreeRecord parent) {
		if (tree == null) {
			SCPTreeRecord tree = new SCPTreeRecord(parent,"Section",getValueForSectionNodeInTree());
			addSectionHeaderToTree(tree);
		}
		return tree;
	}

	/**
	 * <p>Get a description of measurement values that may have missing values.</p>
	 *
	 * <p>Described in Section 5.10.2 as being defined in the CSE Project.</p>
	 *
	 * @param	i	the numeric value that may be missing
	 * @return		a description of the type of missing value
	 */
	public static String describeMissingValues(int i) {
		String s = "";
		if (i == 29999) {
			s="Measurement not computed by the program";
		}
		else if (i == 29998) {
			s="Measurement result not found due to rejection of the lead by measurement program";
		}
		else if (i == 19999) {
			s="Measurement not found because wave was not present in the corresponding lead";
		}
		return s;
	}

	/**
	 * <p>Add a tree node with a numeric value as decimal and hexadecimal strings.</p>
	 *
	 * @param	parent	the node to which to add this new node as a child
	 * @param	name	the name of the new node
	 * @param	value	the numeric value of the new node
	 */
	static protected void addNodeOfDecimalAndHex(SCPTreeRecord parent,String name,int value) {
		new SCPTreeRecord(parent,name,Integer.toString(value)+" dec (0x"+Integer.toHexString(value)+")");
	}
	
	/**
	 * <p>Add a tree node with a numeric value as decimal string, with potentially missing values.</p>
	 *
	 * @param	parent	the node to which to add this new node as a child
	 * @param	name	the name of the new node
	 * @param	value	the numeric value of the new node
	 */
	static protected void addNodeOfDecimalWithMissingValues(SCPTreeRecord parent,String name,int value) {
		new SCPTreeRecord(parent,name,Integer.toString(value)+" dec "+describeMissingValues(value));
	}

	/**
	 * <p>Convert an array of bytes to a <code>String</code> removing any embedded nulls.</p>
	 *
	 * <p>Nulls may be embedded, and are simply ignored; they do not terminate the string.</p>
	 *
	 * <p>The default character encoding is used; ISO 2022 escapes are not yet supported.</p>
	 *
	 * @param	bytes	the array of bytes, possibly with embedded nulls
	 * @return		the <code>String</code> value
	 */
	public static String makeStringFromByteArrayRemovingAnyNulls(byte[] bytes) {
		StringBuffer strbuf = new StringBuffer();
		for (int i=0; i<bytes.length; ++i) {
			if (bytes[i] == 0) {
				// should stop at null ??
			}
			else {
				strbuf.append((char)(bytes[i]&0xff));		// not cool :(
			}
		}
		return strbuf.toString();
	}
}

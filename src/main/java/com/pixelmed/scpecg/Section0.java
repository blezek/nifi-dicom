/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.TreeMap;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Pointers to Data Areas section.</p>
 *
 * @author	dclunie
 */
public class Section0 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section0.java,v 1.10 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Pointers to Data Areas"; }

	private int[] sectionIDNumbers;
	private long[] sectionLengths;
	private long[] sectionIndexes;
	
	public int[]  getSectionIDNumbers() { return sectionIDNumbers; }
	public long[] getSectionLengths()   { return sectionLengths; }
	public long[] getSectionIndexes()   { return sectionIndexes; }
	
	public Section0(SectionHeader header) {
		super(header);
	}
		
	public long read(BinaryInputStream i) throws IOException {
		if (sectionBytesRemaining%10 != 0) {
			throw new IOException("Section 0 (Pointer Section) variable data length not a multiple of 10");
		}
		int numberOfSections = (int)(sectionBytesRemaining/10);
		sectionIDNumbers = new int [numberOfSections];
		  sectionLengths = new long[numberOfSections];
		  sectionIndexes = new long[numberOfSections];
			
		int section=0;
		while (sectionBytesRemaining > 0) {
			sectionIDNumbers[section] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			sectionLengths[section] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			sectionIndexes[section] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			++section;
		}
		//assert sectionBytesRemaining == 0
		//assert section == numberOfSections
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		if (sectionIDNumbers != null) {
			strbuf.append("Section 0 number of pointers = "+sectionIDNumbers.length+"\n");
			for (int section=0; section<sectionIDNumbers.length; ++section) {
				strbuf.append("\tSection ID Number = "+sectionIDNumbers[section]+" dec (0x"+Integer.toHexString(sectionIDNumbers[section])+")\n");
				strbuf.append("\t\tSection Length = "+sectionLengths[section]+" dec (0x"+Long.toHexString(sectionLengths[section])+")\n");
				strbuf.append("\t\tSection Index = "+sectionIndexes[section]+" dec (0x"+Long.toHexString(sectionIndexes[section])+")\n");
			}
		}
		return strbuf.toString();
	}

	public String validate() {
		StringBuffer strbuf = new StringBuffer();
		if (!new String(header.getReservedBytes()).equals("SCPECG")) {
			strbuf.append("Section 0 header reserved bytes not SCPECG\n");
		}
		if (sectionIDNumbers == null) {
			strbuf.append("Section 0 contains no pointers\n");
		}
		return strbuf.toString();
	}

	/**
	 * <p>Validate the section against the contents of other sections.</p>
	 *
	 * <p>Specifically, checks pointers and lengths.</p>
	 *
	 * @param	sections	all the sections
	 * @return			the validation results as a <code>String</code>
	 */
	public String validateAgainstOtherSections(TreeMap sections) {
		StringBuffer strbuf = new StringBuffer();
			
		// first check that all lengths and indexes in Section 0 match the reality of the sections ...
		BitSet check0Through11Referenced = new BitSet(12);
		if (sectionIDNumbers != null) {
			for (int section=0; section<sectionIDNumbers.length; ++section) {
				Section targetSection = (Section)(sections.get(new Integer(sectionIDNumbers[section])));
				if (sectionLengths[section] != 0) {		// sections may be referenced but be zero length (i.e. be absent)
					if (targetSection == null) {
						strbuf.append("Section 0 references Section "+sectionIDNumbers[section]+" that does not exist\n");
					}
					else {
						if (sectionLengths[section] != targetSection.getSectionHeader().getSectionLength()) {
							strbuf.append("Section 0 reference to Section "+sectionIDNumbers[section]+" length mismatch ");
							strbuf.append("Section 0 says "+sectionLengths[section]);
							strbuf.append(" but length is actually "+targetSection.getSectionHeader().getSectionLength()+"\n");
						}
						if (sectionIndexes[section] != targetSection.getSectionHeader().getByteOffset()+1) {	// SCP-ECG indexes are from 1, not 0
							strbuf.append("Section 0 reference to Section "+sectionIDNumbers[section]+" index mismatch ");
							strbuf.append("Section 0 says "+sectionIndexes[section]);
							strbuf.append(" but index is actually "+(targetSection.getSectionHeader().getByteOffset()+1)+"\n");
						}
					}
				}
				else {
					if (sectionIndexes[section] != 0) {
						strbuf.append("Section 0 reference to Section "+sectionIDNumbers[section]+" specifies zero length ");
						strbuf.append("but index is not null\n");
					}
				}
				// while we are at it, check that the mandatory references to sections 0 through 11 are present
				if (sectionIDNumbers[section] >= 0 && sectionIDNumbers[section] <= 11) {
					check0Through11Referenced.set(sectionIDNumbers[section]);
				}
			}
		}

		// check that the mandatory references to sections 0 through 11 are reference, regardless of whether the section is actually present ...
			
		if (check0Through11Referenced.cardinality() != 12) {
			for (int section=0; section<=11; ++section) {
				if (!check0Through11Referenced.get(section)) {
					strbuf.append("Section 0 does not reference Section "+section+" even though it is mandatory to do so\n");
				}
			}
		}
			
		// then check that all sections have references from Section 0 ...
			
		Iterator i = sections.keySet().iterator();
		while (i.hasNext()) {
			int wantID = ((Integer)(i.next())).intValue();		// the key itself is sufficient, since it is the section ID number
			boolean found=false;
			for (int section=0; section<sectionIDNumbers.length; ++section) {
				if (sectionIDNumbers[section] == wantID) {
					found=true;
					break;
				}
			}
			if (!found) {
				strbuf.append("Section 0 does not reference Section "+wantID+" even though it is present\n");
			}
		}

		return strbuf.toString();
	}


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
			for (int section=0; section<sectionIDNumbers.length; ++section) {
				new SCPTreeRecord(tree,
					"Section "+Integer.toString(sectionIDNumbers[section]),
					"Length = "+Long.toString(sectionLengths[section])
					           +" dec (0x"+Long.toHexString(sectionLengths[section])+") "
					+"Index = "+Long.toString(sectionIndexes[section])
					           +" dec (0x"+Long.toHexString(sectionIndexes[section])+")"
				);
			}
		}
		return tree;
	}
}



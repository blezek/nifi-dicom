/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Textual Diagnosis and Universal Interpretative Statement Codes sections.</p>
 *
 * @author	dclunie
 */
public class Section8Or11 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section8Or11.java,v 1.6 2017/01/24 10:50:47 dclunie Exp $";


	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() {
		return header.getSectionIDNumber() == 8
			? "Textual Diagnosis"
			: "Universal Interpretative Statement Codes";
	}

	private int confirmed;
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private int second;
	private int numberOfStatements;
			
	private int[] sequenceNumbers;
	private int[] statementLengths;
	private byte[][] statements;
		      	
	public int getConfirmed() { return confirmed; }
	public int getYear() { return year; }
	public int getMonth() { return month; }
	public int getDay() { return day; }
	public int getHour() { return hour; }
	public int getMinute() { return minute; }
	public int getSecond() { return second; }
	
	public int getNumberOfStatements() { return numberOfStatements; }
	
	public int[] getSequenceNumbers() { return sequenceNumbers; }
	public int[] getStatementLengths() { return statementLengths; }
	public byte[][] getStatements() { return statements; }
		
	public Section8Or11(SectionHeader header) {
		super(header);
	}
		
	public long read(BinaryInputStream i) throws IOException {
		confirmed=i.readUnsigned8();		// 1
		bytesRead++;
		sectionBytesRemaining--;
			
		year=i.readUnsigned16();		// 2-3
		bytesRead+=2;
		sectionBytesRemaining-=2;
						
		month=i.readUnsigned8();		// 4
		bytesRead++;
		sectionBytesRemaining--;
			
		day=i.readUnsigned8();			// 5
		bytesRead++;
		sectionBytesRemaining--;
			
		hour=i.readUnsigned8();			// 6
		bytesRead++;
		sectionBytesRemaining--;

		minute=i.readUnsigned8();		// 7
		bytesRead++;
		sectionBytesRemaining--;
			
		second=i.readUnsigned8();		// 8
		bytesRead++;
		sectionBytesRemaining--;
			
		numberOfStatements=i.readUnsigned8();	// 9
		bytesRead++;
		sectionBytesRemaining--;

		 sequenceNumbers = new int[numberOfStatements];
		statementLengths = new int[numberOfStatements];
		      statements = new byte[numberOfStatements][];

		int statement=0;
		while (sectionBytesRemaining > 0 && statement < numberOfStatements) {

			sequenceNumbers[statement] = i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			statementLengths[statement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			int statementLength = statementLengths[statement];				// need to properly parse type and null separated codes if Section 11 :(
			if (statementLength > 0) {
				if (statementLength > sectionBytesRemaining) {
					System.err.println("Section "+header.getSectionIDNumber()
						+" Statement length wanted "+statementLength+" is longer than "+sectionBytesRemaining
						+" dec bytes remaining in section"
						+", giving up on rest of section");
					skipToEndOfSectionIfNotAlreadyThere(i);
				}
				else {
					statements[statement] = new byte[statementLength];
					i.readInsistently(statements[statement],0,statementLength);
					bytesRead+=statementLength;
					sectionBytesRemaining-=statementLength;
				}
			}
			++statement;
		}
		if (statement != numberOfStatements) {
			System.err.println("Section "+header.getSectionIDNumber()
				+" Number of statements specified as "+numberOfStatements
				+" but encountered only "+statement+" (valid) statements");
		}
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}
		
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Confirmed = "+confirmed+" dec (0x"+Integer.toHexString(confirmed)+")\n");
		strbuf.append("Date = "+year+"/"+month+"/"+day+"\n");
		strbuf.append("Time = "+hour+":"+minute+":"+second+"\n");
		strbuf.append("Number of Statements = "+numberOfStatements+" dec\n");
		
		strbuf.append("Statements:\n");
		for (int statement=0; statement<numberOfStatements; ++statement) {
			strbuf.append("\tStatement "+statement+":\n");
			strbuf.append("\t\tSequence Number "+sequenceNumbers[statement]+" dec (0x"+Integer.toHexString(sequenceNumbers[statement])+")\n");
			strbuf.append("\t\tStatement Length "+statementLengths[statement]+" dec (0x"+Integer.toHexString(statementLengths[statement])+")\n");
			strbuf.append("\t\tStatement <"+(statements[statement] == null ? "" : makeStringFromByteArrayRemovingAnyNulls(statements[statement]))+")\n");
			strbuf.append(com.pixelmed.utils.HexDump.dump(statements[statement]));
		}
		strbuf.append("\tStatements combined:\n");
		for (int statement=0; statement<numberOfStatements; ++statement) {
			//try {
			//	strbuf.append((statements[statement] == null ? "" : new String(statements[statement],"ISO8859_1"))+")\n");
			strbuf.append((statements[statement] == null ? "" : makeStringFromByteArrayRemovingAnyNulls(statements[statement]))+")\n");
			//}
			//catch (java.io.UnsupportedEncodingException e) {
			//}
		}
		return strbuf.toString();
	}
		
	public String validate() {
		return "";
/*
Section 11: Only one field of type "Statement Logic" is allowed to identify the logical relationships between
statements of the other types. If no "Statement Logic" type field is included in the section, it is
assumed that all statements are equally valid, and have no "special" relationship to each other,
except for what is declared in the statement. The number of fields of the types "Universal
Statement Codes" and "Full Text" are not restricted.
*/
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
			
			new SCPTreeRecord(tree,"Confirmed",Integer.toString(confirmed)+" dec (0x"+Integer.toHexString(confirmed)+")");
			new SCPTreeRecord(tree,"Date",year+"/"+month+"/"+day);
			new SCPTreeRecord(tree,"Time",hour+":"+minute+":"+second);
			new SCPTreeRecord(tree,"Number of Statements",Integer.toString(numberOfStatements)+" dec (0x"+Integer.toHexString(numberOfStatements)+")");
			//{
			//	SCPTreeRecord statementsNode = new SCPTreeRecord(tree,"Statements");
			//	for (int statement=0; statement<numberOfStatements; ++statement) {
			//		SCPTreeRecord statementNode = new SCPTreeRecord(statementsNode,"Statement",Integer.toString(statement));
			//		new SCPTreeRecord(statementNode,"Sequence Number",Integer.toString(sequenceNumbers[statement])
			//			+" dec (0x"+Integer.toHexString(sequenceNumbers[statement])+")");
			//		new SCPTreeRecord(statementNode,"Statement Length",
			//			Integer.toString(statementLengths[statement])
			//			+" dec (0x"+Integer.toHexString(statementLengths[statement])+")");
			//		new SCPTreeRecord(statementNode,"Statement",(statements[statement] == null ? "" : new String(statements[statement])));
			//	}
			//}
			
			{
				SCPTreeRecord statementsNode = new SCPTreeRecord(tree,"Statements");
				for (int statement=0; statement<numberOfStatements; ++statement) {
					SCPTreeRecord statementNode = new SCPTreeRecord(statementsNode,sequenceNumbers[statement],
						(statements[statement] == null ? "" : makeStringFromByteArrayRemovingAnyNulls(statements[statement])));
				}
			}
		}
		return tree;
	}
}


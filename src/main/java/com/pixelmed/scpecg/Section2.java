/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.TreeMap;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Huffman Tables section.</p>
 *
 * @author	dclunie
 */
public class Section2 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section2.java,v 1.12 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Huffman Tables"; }

	private int numberOfHuffmanTables;
	private ArrayList huffmanTablesList;		// of class HuffmanTable
	
	public int getNumberOfHuffmanTables() { return numberOfHuffmanTables; }
	
	static public boolean useDefaultTable(int numberOfHuffmanTables) { return numberOfHuffmanTables == 19999; }
	public boolean useDefaultTable() { return useDefaultTable(numberOfHuffmanTables); }
	
	public boolean useNoTable() { return numberOfHuffmanTables == 0; }		// Hmmm ... should really be indicate by absence of this section
	
	public int getNumberOfEncodedHuffmanTables() { return huffmanTablesList == null ? 0 : huffmanTablesList.size(); }
	
	public ArrayList getHuffmanTables() { return huffmanTablesList; }
			
	public Section2(SectionHeader header) {
		super(header);
	}

	public long read(BinaryInputStream i) throws IOException {
		numberOfHuffmanTables=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;

		// /Users/dclunie/Documents/Medical/stuff/ECG/OpenECG/Data/Files/101.scp	has bytes after 19999
		// /Users/dclunie/Documents/Medical/stuff/ECG/OpenECG/Data/Files/112.scp	actually has a huffman table
			
		if ((useDefaultTable() || useNoTable()) && sectionBytesRemaining != 0) {
			System.err.println("Section 2 Number Of Huffman Tables ="+numberOfHuffmanTables+" dec, but "
				+sectionBytesRemaining+" more bytes(code structures) in section are present - ignoring them");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}
		while (sectionBytesRemaining > 0) {
			int numberOfCodeStructures = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
				
			int[] numberOfBitsInPrefix = new int[numberOfCodeStructures];
			int[] numberOfBitsInEntireCode = new int[numberOfCodeStructures];
			int[] tableModeSwitch = new int[numberOfCodeStructures];
			int[] baseValueRepresentedByBaseCode = new int[numberOfCodeStructures];
			long[] baseCode = new long[numberOfCodeStructures];

			for (int codeStructure=0; codeStructure<numberOfCodeStructures; ++codeStructure) {
				numberOfBitsInPrefix[codeStructure] = i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				numberOfBitsInEntireCode[codeStructure] = i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				tableModeSwitch[codeStructure] = i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				baseValueRepresentedByBaseCode[codeStructure] = i.readUnsigned16();
				bytesRead+=2;
				sectionBytesRemaining-=2;
				baseCode[codeStructure] = i.readUnsigned32();
				bytesRead+=4;
				sectionBytesRemaining-=4;
			}
			if (huffmanTablesList == null) {
				huffmanTablesList = new ArrayList();
			}
			huffmanTablesList.add(new HuffmanTable(numberOfCodeStructures,numberOfBitsInPrefix,numberOfBitsInEntireCode,
				tableModeSwitch,baseValueRepresentedByBaseCode,baseCode));
		}
		if (!useDefaultTable() && numberOfHuffmanTables != getNumberOfEncodedHuffmanTables()) {
			System.err.println("Section 2 Number Of Huffman Tables specified as "
				+numberOfHuffmanTables+" but encountered "+getNumberOfEncodedHuffmanTables());
		}
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Number of Huffman Tables = "+numberOfHuffmanTables+" dec (0x"+Integer.toHexString(numberOfHuffmanTables)+")");
		strbuf.append((useDefaultTable() ? " DEFAULT" : "")+"\n");
		if (huffmanTablesList != null) {
			ListIterator i = huffmanTablesList.listIterator();
			while (i.hasNext()) {
				strbuf.append("Table Number = "+i.nextIndex()+"\n");
				HuffmanTable t = (HuffmanTable)(i.next());
				strbuf.append(t);
			}
		}
		return strbuf.toString();
	}

	public String validate() {
		return "";
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
			new SCPTreeRecord(tree,"Number of Huffman Tables",Long.toString(numberOfHuffmanTables)+" dec (0x"+Long.toHexString(numberOfHuffmanTables)+")"+
				(useDefaultTable() ? " DEFAULT" : ""));
		}
		return tree;
	}
}


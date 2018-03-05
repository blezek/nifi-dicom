/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

/**
 * <p>A class to store Huffman Tables, either as read from an SCP-ECG file or the default as specified in the SCP-ECG standard.</p>
 *
 * @see com.pixelmed.scpecg.DefaultHuffmanTable
 * @see com.pixelmed.scpecg.HuffmanDecoder
 *
 * @author	dclunie
 */
public class HuffmanTable {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/HuffmanTable.java,v 1.7 2017/01/24 10:50:46 dclunie Exp $";
	
	protected int numberOfCodeStructuresInTable;
	protected int[] numberOfBitsInPrefix;
	protected int[] numberOfBitsInEntireCode;
	protected int[] tableModeSwitch;
	protected int[] baseValueRepresentedByBaseCode;
	protected long[] baseCode;
	
	public int    getNumberOfCodeStructuresInTable()  { return numberOfCodeStructuresInTable; }
	public int[]  getNumberOfBitsInPrefix()           { return numberOfBitsInPrefix; }
	public int[]  getNumberOfBitsInEntireCode()       { return numberOfBitsInEntireCode; }
	public int[]  getTableModeSwitch()                { return tableModeSwitch; }
	public int[]  getBaseValueRepresentedByBaseCode() { return baseValueRepresentedByBaseCode; }
	public long[] getBaseCode()                       { return baseCode; }
	
	protected HuffmanTable() {
	}
	
	/**
	 * <p>Construct a Huffman Table from the supplied data as read from an SCP-ECG file.</p>
	 *
	 * @param	numberOfCodeStructuresInTable		the number of codes (i.e. the size of all the array parameters)
	 * @param	numberOfBitsInPrefix			for each code, the number of prefix bits for each code (i.e. the Huffman code)
	 * @param	numberOfBitsInEntireCode		for each code, if &gt; numberOfBitsInPrefix, used to find the number of original bits encoded
	 * @param	tableModeSwitch				for each code, a flag to indicate to switch to another table (1 indicates no switch)
	 * @param	baseValueRepresentedByBaseCode		for each code, the value that the code represents
	 * @param	baseCode				the codes (with the order of bits reversed)
	 */
	public HuffmanTable(int numberOfCodeStructuresInTable,
			    int[] numberOfBitsInPrefix,
			    int[] numberOfBitsInEntireCode,
			    int[] tableModeSwitch,
			    int[] baseValueRepresentedByBaseCode,
			    long[] baseCode) {
		this.numberOfCodeStructuresInTable=numberOfCodeStructuresInTable;
		this.numberOfBitsInPrefix=numberOfBitsInPrefix;
		this.numberOfBitsInEntireCode=numberOfBitsInEntireCode;
		this.tableModeSwitch=tableModeSwitch;
		this.baseValueRepresentedByBaseCode=baseValueRepresentedByBaseCode;
		this.baseCode=baseCode;
	}

	/**
	 * <p>Dump the tables as a <code>String</code>.</p>
	 *
	 * @return		the tables as a <code>String</code>
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Number Of Code Structures = "+numberOfCodeStructuresInTable+"\n");
		for (int i=0; i<numberOfCodeStructuresInTable; ++i) {
		strbuf.append("\tCode Structure = "+i+"\n");
			strbuf.append("\t\tNumber Of Bits In Prefix = "+numberOfBitsInPrefix[i]+" dec (0x"+Integer.toHexString(numberOfBitsInPrefix[i])+")\n");
			strbuf.append("\t\tNumber Of Bits In Entire Code = "+numberOfBitsInEntireCode[i]+" dec (0x"+Integer.toHexString(numberOfBitsInEntireCode[i])+")\n");
			strbuf.append("\t\tTable Mode Switch = "+tableModeSwitch[i]+" dec (0x"+Integer.toHexString(tableModeSwitch[i])+")\n");
			strbuf.append("\t\tBase Value Represented By Base Code = "+baseValueRepresentedByBaseCode[i]+
				" dec (0x"+Integer.toHexString(baseValueRepresentedByBaseCode[i])+")\n");
			strbuf.append("\t\tBase Code = "+baseCode[i]+" dec (0x"+Long.toHexString(baseCode[i])+")\n");
		}
		return strbuf.toString();
	}
}

		

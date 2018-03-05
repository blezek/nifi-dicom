/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

/**
 * <p>A class for constructing the default Huffman Table specified in the SCP-ECG standard.</p>
 *
 * @see com.pixelmed.scpecg.HuffmanTable
 * @see com.pixelmed.scpecg.HuffmanDecoder
 *
 * @author	dclunie
 */
public class DefaultHuffmanTable extends HuffmanTable {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/DefaultHuffmanTable.java,v 1.6 2017/01/24 10:50:46 dclunie Exp $";

	// From prEN 1064:2002 C.2.7.4 ...
	
	private int defaultNumberOfCodeStructuresInTable = 19;
	
	private int[] defaultNumberOfBitsInPrefix = {
		1, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 10, 10
	};
	
	private int[] defaultNumberOfBitsInEntireCode = {
		1, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 18, 26
	};
	
	private int[] defaultTableModeSwitch = {
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
	};
	
	private int[] defaultBaseValueRepresentedByBaseCode = {
		0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8, 0, 0
	};
	
	private long[] defaultBaseCode = {
		0, 1, 5, 3, 11, 7, 23, 15, 47, 31, 95, 63, 191, 127, 383, 255, 767, 511, 1023
	};
	
	//    0x0, 0x1, 0x5, 0x3, 0xb,  0x7, 0x17,  0xf, 0x2f, 0x1f, 0x5f, 0x3f, 0xbf,  0x7f, 0x17f,  0xff, 0x2ff, 0x1ff, 0x3ff
	//    0x0, 0x4, 0x5, 0xc, 0xd, 0x1c, 0x1d, 0x3c, 0x3d, 0x7c, 0x7d, 0xfc, 0xfd, 0x1fc, 0x1fd, 0x3fc, 0x3fd, 0x3fe, 0x3ff
	//      0,   1,  -1,   2,  -2,    3,   -3,    4,   -4,    5,   -5,    6,   -6,     7,    -7,     8,    -8,     0,     0

	/**
	 * <p>Construct a default Huffman Table.</p>
	 */
	public DefaultHuffmanTable() {
		 numberOfCodeStructuresInTable=defaultNumberOfCodeStructuresInTable;
		          numberOfBitsInPrefix=defaultNumberOfBitsInPrefix;
		      numberOfBitsInEntireCode=defaultNumberOfBitsInEntireCode;
		               tableModeSwitch=defaultTableModeSwitch;
		baseValueRepresentedByBaseCode=defaultBaseValueRepresentedByBaseCode;
		                      baseCode=defaultBaseCode;
	}
}


	

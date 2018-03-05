/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * <p>A class to implement Huffman decoding as used by the SCP-ECG standard.</p>
 *
 * @see com.pixelmed.scpecg.DefaultHuffmanTable
 * @see com.pixelmed.scpecg.HuffmanTable
 *
 * @author	dclunie
 */
public class HuffmanDecoder {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/HuffmanDecoder.java,v 1.13 2017/01/24 10:50:46 dclunie Exp $";

	private int[] extractBitMask = { 0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01 };
	
	private long[] signDetectMask = {
				0x0000000000000000l, 
				0x0000000000000001l,  0x0000000000000002l,  0x0000000000000004l,  0x0000000000000008l,
				0x0000000000000010l,  0x0000000000000020l,  0x0000000000000040l,  0x0000000000000080l /* 8 bits */,
				0x0000000000000100l,  0x0000000000000200l,  0x0000000000000400l,  0x0000000000000800l,
				0x0000000000001000l,  0x0000000000002000l,  0x0000000000004000l,  0x0000000000008000l /* 16 bits */
		};
	
	private long[] signExtendMask = {
				0xfffffffffffff000l, 
				0xffffffffffffffffl,  0xfffffffffffffffel,  0xfffffffffffffffcl,  0xfffffffffffffff8l,
				0xfffffffffffffff0l,  0xffffffffffffffe0l,  0xffffffffffffffc0l,  0xffffffffffffff80l /* 8 bits */,
				0xffffffffffffff00l,  0xfffffffffffffe00l,  0xfffffffffffffc00l,  0xfffffffffffff800l,
				0xfffffffffffff000l,  0xffffffffffffe000l,  0xffffffffffffc000l,  0xffffffffffff8000l /* 16 bits */
		};

	private String dump(long[] array) {
		StringBuffer buffer =  new StringBuffer();
		for (int i=0; i<array.length; ++i) {
			if (i > 0) {
				buffer.append(",");
			}
			buffer.append("0x");
			buffer.append(Long.toHexString(array[i]));
		}
		return buffer.toString();
	}
	
	private byte[] bytesToDecompress;
	private int availableBytes;
	private int byteIndex;
	private int bitIndex;
	private int currentByte;
	private long currentBits;
	private int haveBits;
	
	private int decompressedValueCount;	// number of values decompressed so far (used to know whether lastValue and secondLastValue loaded)
	private short lastValue;		// last value decompressed
	private short secondLastValue;		// 2nd last value decompressed

	private int huffmanTableLength;
	private int[] bitsPerPrefix;
	private int[] bitsPerEntireCode;
	private int[] tableModeSwitch;
	private long[] huffmanPrefixCodes;
	private int[] valuesRepresentedByCodes;
		
	private int differenceDataUsed;			// 0=no,1=1st difference,2=2nd difference
	private int multiplier;				// since samples were truncated before encoding

	private ArrayList huffmanTablesList;		// the tables supplied by the constructor

	private final long reverseBits(long src,int n) {
		long dst = 0;
		while (n-- > 0) {
			dst = (dst<<1) | (src&0x1);
			src = src>>1;
		}
		return dst;
	}
		
	private final long[] swapSuppliedHuffmanTableBaseCodes(long[] reversedHuffmanPrefixCodes,int[] bitsPerPrefix) {
//System.err.println("Supplied prefix codes:\n"+dump(reversedHuffmanPrefixCodes));
		int n=reversedHuffmanPrefixCodes.length;
		long[] correctedHuffmanPrefixCodes = new long[n];
		for (int i=0; i<n; ++i) {
			correctedHuffmanPrefixCodes[i]=reverseBits(reversedHuffmanPrefixCodes[i],bitsPerPrefix[i]);
		}
//System.err.println("Reversed prefix codes:\n"+dump(correctedHuffmanPrefixCodes));
		return correctedHuffmanPrefixCodes;
	}
		
	private final void getEnoughBits(int wantBits) throws Exception {
		while (haveBits < wantBits) {
			if (bitIndex > 7) {
				if (byteIndex < availableBytes) {
					currentByte=bytesToDecompress[byteIndex++];
//System.err.println("currentByte now =0x"+Integer.toHexString(currentByte));
					bitIndex=0;
				}
				else {
					throw new Exception("No more bits (having decompressed "+byteIndex+" dec bytes)");
				}
			}
			long newBit = (currentByte & extractBitMask[bitIndex++]) == 0 ? 0 : 1;
			currentBits = (currentBits << 1) + newBit;
			++haveBits;
		}
	}
	
	private void loadHuffmanTableInUse(int number) {
//System.err.println("loadHuffmanTableInUse: "+number);
		HuffmanTable useHuffmanTable = null;
		if (Section2.useDefaultTable(number)) {
//System.err.println("Loading default HuffmanTable");
			useHuffmanTable = new DefaultHuffmanTable();
		}
		else if (huffmanTablesList != null) {
//System.err.println("Loading HuffmanTable "+number);
			useHuffmanTable = (HuffmanTable)(huffmanTablesList.get(number));
		}
		      huffmanTableLength=useHuffmanTable.getNumberOfCodeStructuresInTable();
		           bitsPerPrefix=useHuffmanTable.getNumberOfBitsInPrefix();
		       bitsPerEntireCode=useHuffmanTable.getNumberOfBitsInEntireCode();
		         tableModeSwitch=useHuffmanTable.getTableModeSwitch();
		valuesRepresentedByCodes=useHuffmanTable.getBaseValueRepresentedByBaseCode();
		      huffmanPrefixCodes=swapSuppliedHuffmanTableBaseCodes(useHuffmanTable.getBaseCode(),bitsPerPrefix);
	}
		
	/**
	 * <p>Construct a Huffman decoder for the supplied encoded data, as read from an SCP-ECG file.</p>
	 *
	 * @param	bytesToDecompress			the compressed data
	 * @param	differenceDataUsed			0 = no, 1 = 1 difference value, 2 =  2 difference values
	 * @param	multiplier				a value by which to scale the decoded values
	 * @param	numberOfHuffmanTables			how many tables are available for use
	 * @param	huffmanTablesList			the Huffman tables themselves
	 */
	public HuffmanDecoder(byte[] bytesToDecompress,int differenceDataUsed,int multiplier,int numberOfHuffmanTables,ArrayList huffmanTablesList) {
		this.differenceDataUsed=differenceDataUsed;
		this.multiplier=multiplier;
		this.bytesToDecompress = bytesToDecompress;
		availableBytes = bytesToDecompress.length;
		decompressedValueCount=0;
		byteIndex = 0;
		bitIndex = 8;	// force fetching byte the first time
		haveBits = 0;	// don't have any bits to start with
		
		this.huffmanTablesList=huffmanTablesList;
		loadHuffmanTableInUse(Section2.useDefaultTable(numberOfHuffmanTables) ? numberOfHuffmanTables : 0);	// start with default or first
	}
	
	/**
	 * <p>Decode a single value.</p>
	 *
	 * @return	the decoded value
	 */
	public final short decode()  throws Exception {
		short value = 0;		// initializer irrelevant but quietens compiler
		boolean gotValue = false;
		do {
			int tableIndex = 0;
			while (tableIndex < huffmanTableLength) {
//System.err.println("tableIndex = "+tableIndex);
				int wantPrefixBits = bitsPerPrefix[tableIndex];
//System.err.println("wantPrefixBits = "+wantPrefixBits);
				// assert wantPrefixBits != 0
				// assert wantPrefixBits < 32 (length of one int in Java)
				getEnoughBits(wantPrefixBits);		// modifies currentBits
//System.err.println("currentBits 0x"+Long.toHexString(currentBits));
//System.err.println("compare to  0x"+Long.toHexString(huffmanPrefixCodes[tableIndex]));
				if (currentBits == huffmanPrefixCodes[tableIndex]) {
//System.err.println("Found prefix 0x"+Long.toHexString(currentBits));
					break;
				}
				++tableIndex;
			}
			if (tableIndex >= huffmanTableLength) {
				throw new Exception("Code prefix not in table");
			}
			if (tableModeSwitch[tableIndex] == 0) {					// Note that 0 is the flag to switch; 1 indicates no switch
				int newTableNumber = valuesRepresentedByCodes[tableIndex];	// The base value is used as the new table number
//System.err.println("Table Mode Switch to "+newTableNumber);
				loadHuffmanTableInUse(newTableNumber);
				continue;							// NB. without changing values or incrementing decompressedValueCount !!
			}
			else if (bitsPerPrefix[tableIndex] == bitsPerEntireCode[tableIndex]) {
//System.err.println("Value from table ="+valuesRepresentedByCodes[tableIndex]+" dec, 0x"+Long.toHexString(valuesRepresentedByCodes[tableIndex]));
				value=(short)valuesRepresentedByCodes[tableIndex];
//System.err.println("As short ="+value+" dec");
			}
			else {	// assume greater, else table would be malformed
				int numberOfOriginalBits = bitsPerEntireCode[tableIndex]-bitsPerPrefix[tableIndex];
//System.err.println("numberOfOriginalBits ="+numberOfOriginalBits+" dec");
				currentBits=0;
				haveBits=0;
				getEnoughBits(numberOfOriginalBits);		// modifies currentBits
//System.err.println("Value from bitstream ="+currentBits+" dec, 0x"+Long.toHexString(currentBits));
				if ((currentBits & signDetectMask[numberOfOriginalBits]) != 0) {
					currentBits|=signExtendMask[numberOfOriginalBits];
//System.err.println("Sign extended ="+currentBits+" dec, 0x"+Long.toHexString(currentBits));
				}
				value=(short)currentBits;
//System.err.println("As short ="+value+" dec");
			}
			// for differences use unmultiplied cached last values, not those in return value array since they have been multiplied
			if (differenceDataUsed == 1) {
				if (decompressedValueCount > 0) {
					value = (short)(value + lastValue);
				}
				// else first value is sent raw (still with Huffman prefix though) leave value alone
			}
			else if (differenceDataUsed == 2) {
				if (decompressedValueCount > 1) {
					value = (short)(value + 2*lastValue - secondLastValue);
				}
				// else first value is sent raw (still with Huffman prefix though) leave value alone
			}
			else if (differenceDataUsed != 0) {
				throw new Exception("Unrecognized difference encoding method "+differenceDataUsed);
			}
//System.err.println("After difference ="+value+" dec");
//System.err.print(", "+value);
			secondLastValue=lastValue;
			lastValue=value;
			currentBits=0;
			haveBits=0;
			++decompressedValueCount;
			gotValue=true;
//System.err.println("");
		} while (!gotValue);
//if (value*multiplier != (short)(value*multiplier)) {
//	System.err.println("Multiplier overflow with initial value="+value+" dec");
//}
		value*=multiplier;
//System.err.println("After multiplier ="+value+" dec");
//System.err.print(", "+value);
		return value;
	}
	
	/**
	 * <p>Decode a specified number of values.</p>
	 *
	 * @param	nValuesWanted		the number of decoded values wanted
	 * @return				the decoded values
	 */
	public final short[] decode(int nValuesWanted) throws Exception {
//System.err.println(com.pixelmed.utils.HexDump.dump(bytesToDecompress));
		short values[] = new short[nValuesWanted];
		for (int count=0; count < nValuesWanted; ++count) {
//System.err.println("count = "+count);
			values[count]=decode();
		}
//System.err.println("Got "+nValuesWanted+" dec, used "+byteIndex+" of "+availableBytes+" dec bytes, at "+bitIndex+" bit");
		return values;
	}
	
	/**
	 * <p>Dump the current decoder state as a <code>String</code>.</p>
	 *
	 * @return		the current decoder state as a <code>String</code>
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("availableBytes=");
		strbuf.append(availableBytes);
		strbuf.append(" dec, byteIndex=");
		strbuf.append(byteIndex);
		strbuf.append(" dec, bitIndex=");
		strbuf.append(bitIndex);
		strbuf.append(" dec, decompressedValueCount=");
		strbuf.append(decompressedValueCount);
		strbuf.append(" dec");
		return strbuf.toString();
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Decodes the byte stream in the example specified in the SCP-ECG standard.</p>
	 *
	 * @param	arg	none
	 */
	public static void main(String arg[]) {
		{
			int nSamples = 28;
			
			byte[] test = { (byte)0xFF, (byte)0x83, (byte)0x7F, (byte)0xE0, (byte)0xE6,
					(byte)0xF1,		// prEN 1064:2002 says F5, but wrong ... binary values in table are F1
					(byte)0x53,
					(byte)0x65, (byte)0x59, (byte)0xB6, (byte)0x5B, (byte)0x96, (byte)0x4B, (byte)0x96,
					(byte)0x00		// prEN 1064:2002 doesn't show this byte, but one extra 0 bit is needed to match table of binary value
				};
			
			short[] decodedResultWithoutMultiplication = {
					13, 14, 15, 14, 16, 18, 19, 20, 22, 22, 23, 23, 23, 22, 22, 20, 17, 15, 12, 8, 6, 3, 1, 0, -2, -2, -3, -3
				};
			
			short[] decodedResultWithMultiplication = {
				63, 70, 74, 71, 79, 89, 96, 102, 108, 112, 114, 116, 116, 112, 110, 100, 87, 74, 59, 42, 28, 13, 5, -1, -8, -11, -13, -17
				};
			
			System.out.println("Decoded (should be exact):");	// no need to use SLF4J since command line utility/test
			HuffmanDecoder decoder = new HuffmanDecoder(test,2/*differenceDataUsed*/,1/*amplitudeValueMultiplier*/,19999,null);
			try {
				short[] decompressedData = decoder.decode(nSamples);
				for (int i=0; i<nSamples; ++i) {
					System.out.println("\t["+i+"] \tgot "+decompressedData[i]+" \texpected "+decodedResultWithoutMultiplication[i]+
						" \tdifference "+(decompressedData[i]-decodedResultWithoutMultiplication[i]));
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
			
			System.out.println("Multiplied (expect to see quantization error):");
			decoder = new HuffmanDecoder(test,2/*differenceDataUsed*/,5/*amplitudeValueMultiplier*/,19999,null);
			try {
				short[] decompressedData = decoder.decode(nSamples);
				for (int i=0; i<nSamples; ++i) {
					System.out.println("\t["+i+"] \tgot "+decompressedData[i]+" \texpected "+decodedResultWithMultiplication[i]+
						" \tdifference "+(decompressedData[i]-decodedResultWithMultiplication[i]));
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
	}
}





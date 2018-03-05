/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

/**
 * <p>Various static methods helpful for dumping decimal and hexadecimal values.</p>
 *
 * @author	dclunie
 */
public class HexDump {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/HexDump.java,v 1.15 2017/01/24 10:50:51 dclunie Exp $";

	private HexDump() {}

	/**
	 * <p>Convert an integer value to a decimal string, padding it to a specified length, and prepending a prefix and appending a suffix.</p>
	 *
	 * @param	i	the integer to dump
	 * @param	pad	the pad character to append before the string value of the integer if necessary
	 * @param	length	the desired length of string converted integer and padding (not including the prefix or suffix)
	 * @param	prefix	the string to prepend
	 * @param	suffix	the string to append
	 * @return		the prefix, padded decimal string and suffix
	 */
	public static String toPaddedDecimalString(int i,char pad,int length,String prefix,String suffix) {
		StringBuffer sb = new StringBuffer();
		if (prefix != null) sb.append(prefix);
		String s=Integer.toString(i);		// decimal string

		int ls=s.length();
		while(ls++ < length) {
			sb.append(pad);
		}

		sb.append(s);				// even if it is longer than length wanted
		if (suffix != null) sb.append(suffix);
		return sb.toString();
	}

	/**
	 * <p>Convert an unsigned byte value to a decimal string of three characters with leading space padding.</p>
	 *
	 * @param	i	the unsigned byte value
	 * @return		the padded decimal string
	 */
	public static String byteToPaddedDecimalString(int i) {
		return toPaddedDecimalString(i&0xff,' ',3,null,null);
	}

	/**
	 * <p>Convert an unsigned short value to a decimal string of six characters with leading space padding.</p>
	 *
	 * @param	i	the unsigned short value
	 * @return		the padded decimal string
	 */
	public static String shortToPaddedDecimalString(int i) {
		return toPaddedDecimalString(i&0xffff,' ',6,null,null);
	}

	/**
	 * <p>Convert an integer value to a decimal string of nine characters with leading space padding.</p>
	 *
	 * @param	i	the integer value
	 * @return		the padded decimal string
	 */
	public static String intToPaddedDecimalString(int i) {
		return toPaddedDecimalString(i&0xffffffff,' ',9,null,null);
	}

	/**
	 * <p>Convert an integer value to a hexadecimal string, padding it to a specified length, and prepending a prefix and appending a suffix.</p>
	 *
	 * @param	i	the integer to dump
	 * @param	pad	the pad character to append before the string value of the integer if necessary
	 * @param	length	the desired length of string converted integer and padding (not including the prefix or suffix)
	 * @param	prefix	the string to prepend
	 * @param	suffix	the string to append
	 * @return		the prefix, padded hexadecimal string and suffix
	 */
	public static String toPaddedHexString(int i,char pad,int length,String prefix,String suffix) {
		StringBuffer sb = new StringBuffer();
		if (prefix != null) sb.append(prefix);
		String s=Integer.toHexString(i);

		int ls=s.length();
		while(ls++ < length) {
			sb.append(pad);
		}

		sb.append(s);				// even if it is longer than length wanted
		if (suffix != null) sb.append(suffix);
		return sb.toString();
	}

	/**
	 * <p>Convert an unsigned byte value to a hexadecimal string of two characters with leading zero padding.</p>
	 *
	 * @param	i	the unsigned byte value
	 * @return		the padded hexadecimal string
	 */
	public static String byteToPaddedHexString(int i) {
		return toPaddedHexString(i&0xff,'0',2,null,null);
	}

	/**
	 * <p>Convert an unsigned short value to a hexadecimal string of four characters with leading zero padding.</p>
	 *
	 * @param	i	the unsigned short value
	 * @return		the padded decimal string
	 */
	public static String shortToPaddedHexString(int i) {
		return toPaddedHexString(i&0xffff,'0',4,null,null);
	}

	/**
	 * <p>Convert an integer value to a hexadecimal string of eight characters with leading zero padding.</p>
	 *
	 * @param	i	the integer value
	 * @return		the padded decimal string
	 */
	public static String intToPaddedHexString(int i) {
		return toPaddedHexString(i&0xffffffff,'0',8,null,null);
	}

	/**
	 * <p>Convert an unsigned byte value to a hexadecimal string of two characters with leading 0x and leading zero padding.</p>
	 *
	 * @param	i	the unsigned byte value
	 * @return		the padded hexadecimal string
	 */
	public static String byteToPaddedHexStringWith0x(int i) {
		return toPaddedHexString(i&0xff,'0',2,"0x",null);
	}

	/**
	 * <p>Convert an unsigned short value to a hexadecimal string of four characters with leading 0x and leading zero padding.</p>
	 *
	 * @param	i	the unsigned short value
	 * @return		the padded decimal string
	 */
	public static String shortToPaddedHexStringWith0x(int i) {
		return toPaddedHexString(i&0xffff,'0',4,"0x",null);
	}

	/**
	 * <p>Convert an integer value to a hexadecimal string of eight characters with leading 0x and leading zero padding.</p>
	 *
	 * @param	i	the integer value
	 * @return		the padded decimal string
	 */
	public static String intToPaddedHexStringWith0x(int i) {
		return toPaddedHexString(i&0xffffffff,'0',8,"0x",null);
	}

	/**
	 * <p>Test whether or not a character in the default character set is printable.</p>
	 *
	 * @param	c	character to test
	 * @return		true if character is printable
	 */
	public static boolean isPrintableCharacter(char c) {

		// as it happens, for ASCII at least, the only characters
		// between 0 and 127 that are not in this list have a
		// type of Character.CONTROL
		// still, just in case ...

		switch (Character.getType(c)) {
			case Character.LOWERCASE_LETTER:
			case Character.UPPERCASE_LETTER:
			case Character.DECIMAL_DIGIT_NUMBER:
			case Character.SPACE_SEPARATOR:
			case Character.CONNECTOR_PUNCTUATION:
			case Character.DASH_PUNCTUATION:
			case Character.START_PUNCTUATION:
			case Character.END_PUNCTUATION:
			//case Character.INITIAL_QUOTE_PUNCTUATION:
			//case Character.FINAL_QUOTE_PUNCTUATION:
			case Character.OTHER_PUNCTUATION:
			case Character.MATH_SYMBOL:
			case Character.CURRENCY_SYMBOL:
			case Character.MODIFIER_SYMBOL:
			case Character.OTHER_SYMBOL:
			case Character.OTHER_LETTER:
			case Character.TITLECASE_LETTER:
			case Character.NON_SPACING_MARK:
			case Character.COMBINING_SPACING_MARK:
			case Character.ENCLOSING_MARK:
				return true;
			default:
				return false;
		}
	}

	/**
	 * <p>Create a printable string representation of a specified portion of a byte array in the default character set.</p>
	 *
	 * <p>Unprintable characters are represented by a period ('.') character.</p>
	 *
	 * @param	b	the byte array
	 * @param	offset	the start of the bytes to be extracted
	 * @param	length	the number of bytes to be extracted
	 * @return		a string of the specified length containing the printable characters of the supplied bytes
	 */
	public static String byteArrayToPrintableString(byte[] b,int offset,int length) {
		StringBuffer sb = new StringBuffer();
		sb.append(" ");
		while (length-- > 0) {
			char c = (char)b[offset++];
			if (isPrintableCharacter(c))
				sb.append(c);
			else
				//sb.append("**"+Character.getType(c)+"**");
				sb.append(".");
			//sb.append(" ");
		}
		return sb.toString();
	}
	/**
	 * <p>Create a printable string representation of a specified portion of a byte array in the default character set.</p>
	 *
	 * <p>Unprintable characters are represented by a period ('.') character.</p>
	 *
	 * @param	b	the byte array
	 * @return		a string containing the printable characters of the supplied bytes
	 */
	public static String byteArrayToPrintableString(byte[] b) {
		return byteArrayToPrintableString(b,0,b.length);
	}
	
	/**
	 * <p>Create a continuous hex string representation of a specified portion of a byte array.</p>
	 *
	 * @param	b	the byte array
	 * @param	offset	the start of the bytes to be extracted
	 * @param	length	the number of bytes to be extracted
	 * @return		a string containing the hex representation of the supplied bytes
	 */
	public static String byteArrayToHexString(byte[] b,int offset,int length) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<length; ++i) {
			sb.append(HexDump.byteToPaddedHexString(b[offset+i]));
		}
		return sb.toString();
	}
	
	/**
	 * <p>Create a continuous hex string representation of a byte array.</p>
	 *
	 * @param	b	the byte array
	 * @return		a string containing the hex representation of the supplied bytes
	 */
	public static String byteArrayToHexString(byte[] b) {
		return byteArrayToHexString(b,0,b.length);
	}

	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values and printable string values of a byte array.</p>
	 *
	 * <p>The result is similar to a dump produced by the unix od or MacOS X 'hexdump -C' commands.</p>
	 *
	 * @param	b	the byte array to be dumped
	 * @param	offset	the offset into the buffer to be dumped
	 * @param	lng	the number of bytes to be dumped
	 * @return		a string containing the multiline result
	 */
	public static String dump(byte[] b,int offset,int lng) {
//Thread.currentThread().dumpStack();
		StringBuffer sb = new StringBuffer();
		if (b != null && lng > 0) {
			int i=0;
			int stringStart=0;
			int stringCount=0;
			while (i < lng) {
				int position = i+offset;
				if (i%16 == 0) {
					if (i != 0) sb.append("\n");
					sb.append(intToPaddedDecimalString(position));
					sb.append(" (");
					sb.append(intToPaddedHexStringWith0x(position));
					sb.append("):");
					stringStart=position;
					stringCount=0;
				}
				sb.append(" ");
				sb.append(byteToPaddedHexString(b[position]));
				++i;
				++stringCount;
				if (i%16 == 0 || i == lng) sb.append(byteArrayToPrintableString(b,stringStart,stringCount));
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values and printable string values of a byte array.</p>
	 *
	 * <p>The result is similar to a dump produced by the unix od or MacOS X 'hexdump -C' commands.</p>
	 *
	 * @param	b	the byte array to be dumped
	 * @param	lng	the number of bytes to be dumped
	 * @return		a string containing the multiline result
	 */
	public static String dump(byte[] b,int lng) {
		return dump(b,0,lng);
	}
	
	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values and printable string values of a byte array.</p>
	 *
	 * <p>The result is similar to a dump produced by the unix od or MacOS X 'hexdump -C' commands.</p>
	 *
	 * @param	b	the byte array to be dumped in its entirety
	 * @return		a string containing the multiline result
	 */
	public static String dump(byte[] b) {
		return dump(b, b == null ? 0 : b.length);
	}


	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values of the Unicode code points and printable string values of a String.</p>
	 *
	 * @param	s	the String to be dumped
	 * @param	offset	the offset into the String to be dumped
	 * @param	lng	the number of characters to be dumped
	 * @return		a string containing the multiline result
	 */
	public static String dump(String s,int offset,int lng) {
		StringBuffer sb = new StringBuffer();
		if (s != null && lng > 0) {
			int i=0;
			int stringStart=0;
			int stringCount=0;
			while (i < lng) {
				int position = i+offset;
				if (i%8 == 0) {
					if (i != 0) sb.append("\n");
					sb.append(intToPaddedDecimalString(position));
					sb.append(" (");
					sb.append(intToPaddedHexStringWith0x(position));
					sb.append("):");
					stringStart=position;
					stringCount=0;
				}
				sb.append(" ");
				sb.append(intToPaddedHexString(Character.codePointAt(s,position)));
				++i;
				++stringCount;
				if (i%8 == 0 || i == lng) {
					sb.append(" ");
					sb.append(s.substring(stringStart,stringStart+stringCount-1));
				}
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values of the Unicode code points and printable string values of a String.</p>
	 *
	 * @param	s	the String to be dumped
	 * @param	lng	the number of characters to be dumped
	 * @return		a string containing the multiline result
	 */
	public static String dump(String s,int lng) {
		return dump(s,0,lng);
	}
	
	/**
	 * <p>Create a dump of the decimal offset, hexadecimal values of the Unicode code points and printable string values of a String.</p>
	 *
	 * @param	s	the String to be dumped dumped in its entirety
	 * @return		a string containing the multiline result
	 */
	public static String dump(String s) {
		return dump(s, s == null ? 0 : s.length());
	}

	/**
	 * <p>Unit test.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {

		byte[] b = new byte[256];
		for (int i=0; i<256; ++i) b[i]=(byte)i;
		System.err.println(dump(b));
	}

}




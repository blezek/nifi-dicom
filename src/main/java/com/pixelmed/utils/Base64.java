/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

/**
 * <p>Various static methods helpful for converting to and from Base64 representations (e.g., for representing binary values in XML documents).</p>
 *
 * @author	dclunie
 */
public class Base64 {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/Base64.java,v 1.4 2017/01/24 10:50:51 dclunie Exp $";
	
	public static String getBase64(double v) {
		long l = Double.doubleToRawLongBits(v);
		byte[] b = new byte[8];
		b[0]=(byte)(l>>56);
		b[1]=(byte)(l>>48);
		b[2]=(byte)(l>>40);
		b[3]=(byte)(l>>32);
		b[4]=(byte)(l>>24);
		b[5]=(byte)(l>>16);
		b[6]=(byte)(l>>8);
		b[7]=(byte)l;
		String s = javax.xml.bind.DatatypeConverter.printBase64Binary(b);
//System.err.println("Base64.getBase64(): double = "+javax.xml.bind.DatatypeConverter.printDouble(v)+" as bytes "+HexDump.dump(b)+" as Base64 "+s+" as long "+Long.toString(l,16));
		return s;
	}					

	public static double getDouble(String s) {
		byte[] b = javax.xml.bind.DatatypeConverter.parseBase64Binary(s);
		long v1 = ((long)b[0])&0xff;
		long v2 = ((long)b[1])&0xff;
		long v3 = ((long)b[2])&0xff;
		long v4 = ((long)b[3])&0xff;
		long v5 = ((long)b[4])&0xff;
		long v6 = ((long)b[5])&0xff;
		long v7 = ((long)b[6])&0xff;
		long v8 = ((long)b[7])&0xff;
		long l = (((((((((((((v1 << 8) | v2) << 8) | v3) << 8) | v4) << 8) | v5) << 8) | v6) << 8) | v7) << 8) | v8;
		double v = Double.longBitsToDouble(l);
//System.err.println("Base64.getDouble(): double = "+javax.xml.bind.DatatypeConverter.printDouble(v)+" from bytes "+HexDump.dump(b)+" from Base64 "+s+" from long "+Long.toString(l,16));
		return v;
	}					

}

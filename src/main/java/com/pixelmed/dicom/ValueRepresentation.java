/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>Utilities to support the concept of the DICOM Value Representation (VR), including
 * two byte arrays for each VR, and tester methods that determine whether or not a
 * particular two byte array is a particular type of VR.</p>
 *
 * @author	dclunie
 */
public class ValueRepresentation {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ValueRepresentation.java,v 1.18 2017/01/24 10:50:39 dclunie Exp $";

	/***/
	public static byte[] AE = { 'A', 'E' };
	/***/
	public static byte[] AS = { 'A', 'S' };
	/***/
	public static byte[] AT = { 'A', 'T' };
	/***/
	public static byte[] CS = { 'C', 'S' };
	/***/
	public static byte[] DA = { 'D', 'A' };
	/***/
	public static byte[] DS = { 'D', 'S' };
	/***/
	public static byte[] DT = { 'D', 'T' };
	/***/
	public static byte[] FL = { 'F', 'L' };
	/***/
	public static byte[] FD = { 'F', 'D' };
	/***/
	public static byte[] IS = { 'I', 'S' };
	/***/
	public static byte[] LO = { 'L', 'O' };
	/***/
	public static byte[] LT = { 'L', 'T' };
	/***/
	public static byte[] OB = { 'O', 'B' };
	/***/
	public static byte[] OD = { 'O', 'D' };
	/***/
	public static byte[] OF = { 'O', 'F' };
	/***/
	public static byte[] OL = { 'O', 'L' };
	/***/
	public static byte[] OW = { 'O', 'W' };
	/***/
	public static byte[] OX = { 'O', 'X' };		// OB or OW
	/***/
	public static byte[] PN = { 'P', 'N' };
	/***/
	public static byte[] SH = { 'S', 'H' };
	/***/
	public static byte[] SL = { 'S', 'L' };
	/***/
	public static byte[] SQ = { 'S', 'Q' };
	/***/
	public static byte[] SS = { 'S', 'S' };
	/***/
	public static byte[] ST = { 'S', 'T' };
	/***/
	public static byte[] TM = { 'T', 'M' };
	/***/
	public static byte[] UC = { 'U', 'C' };
	/***/
	public static byte[] UI = { 'U', 'I' };
	/***/
	public static byte[] UL = { 'U', 'L' };
	/***/
	public static byte[] UN = { 'U', 'N' };
	/***/
	public static byte[] UR = { 'U', 'R' };
	/***/
	public static byte[] US = { 'U', 'S' };
	/***/
	public static byte[] UT = { 'U', 'T' };
	/***/
	public static byte[] XS = { 'X', 'S' };		// US or SS
	/***/
	public static byte[] XO = { 'X', 'O' };		// US or SS or OW

	/**
	 * @param	vr
	 */
	public static final boolean isApplicationEntityVR(byte[] vr) {
		return vr[0]=='A' &&  vr[1]=='E';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isAgeStringVR(byte[] vr) {
		return vr[0]=='A' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isAttributeTagVR(byte[] vr) {
		return vr[0]=='A' &&  vr[1]=='T';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isCodeStringVR(byte[] vr) {
		return vr[0]=='C' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isDateVR(byte[] vr) {
		return vr[0]=='D' &&  vr[1]=='A';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isDateTimeVR(byte[] vr) {
		return vr[0]=='D' &&  vr[1]=='T';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isDecimalStringVR(byte[] vr) {
		return vr[0]=='D' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isFloatDoubleVR(byte[] vr) {
		return vr[0]=='F' &&  vr[1]=='D';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isFloatSingleVR(byte[] vr) {
		return vr[0]=='F' &&  vr[1]=='L';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isIntegerStringVR(byte[] vr) {
		return vr[0]=='I' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isLongStringVR(byte[] vr) {
		return vr[0]=='L' &&  vr[1]=='O';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isLongTextVR(byte[] vr) {
		return vr[0]=='L' &&  vr[1]=='T';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherByteVR(byte[] vr) {
		return vr[0]=='O' &&  vr[1]=='B';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherDoubleVR(byte[] vr) {
		return vr[0]=='O' &&  vr[1]=='D';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherFloatVR(byte[] vr) {
		return vr[0]=='O' &&  vr[1]=='F';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherLongVR(byte[] vr) {
		return vr[0]=='O' &&  vr[1]=='L';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherWordVR(byte[] vr) {
		return vr[0]=='O' &&  vr[1]=='W';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherUnspecifiedVR(byte[] vr) {		// Not a real VR ... but returned by dictionary
		return vr[0]=='O' &&  vr[1]=='X';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isOtherByteOrWordVR(byte[] vr) {
		return vr[0]=='O' && (vr[1]=='B' || vr[1]=='W' || vr[1]=='X');
	}

	/**
	 * @param	vr
	 */
	public static final boolean isPersonNameVR(byte[] vr) {
		return vr[0]=='P' &&  vr[1]=='N';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isShortStringVR(byte[] vr) {
		return vr[0]=='S' &&  vr[1]=='H';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isSignedLongVR(byte[] vr) {
		return vr[0]=='S' &&  vr[1]=='L';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isSequenceVR(byte[] vr) {
		return vr[0]=='S' &&  vr[1]=='Q';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isSignedShortVR(byte[] vr) {
		return vr[0]=='S' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isShortTextVR(byte[] vr) {
		return vr[0]=='S' &&  vr[1]=='T';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isTimeVR(byte[] vr) {
		return vr[0]=='T' &&  vr[1]=='M';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUniqueIdentifierVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='I';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnsignedLongVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='L';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnknownVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='N';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnsignedShortVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnspecifiedShortVR(byte[] vr) {			// Not a real VR ... but returned by dictionary
		return vr[0]=='X' &&  vr[1]=='S';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnspecifiedShortOrOtherWordVR(byte[] vr) {	// Not a real VR ... but returned by dictionary
		return vr[0]=='X' &&  vr[1]=='O';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnlimitedCharactersVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='C';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUnlimitedTextVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='T';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isUniversalResourceVR(byte[] vr) {
		return vr[0]=='U' &&  vr[1]=='R';
	}

	/**
	 * @param	vr
	 */
	public static final boolean isShortValueLengthVR(byte[] vr) {
		return vr[0]=='A' && ( vr[1]=='E'
				    || vr[1]=='S'
				    || vr[1]=='T' )
		    || vr[0]=='C' &&   vr[1]=='S'
		    || vr[0]=='D' && ( vr[1]=='A'
				    || vr[1]=='S'
				    || vr[1]=='T' )
		    || vr[0]=='F' && ( vr[1]=='D'
				    || vr[1]=='L' )
		    || vr[0]=='I' &&   vr[1]=='S'
		    || vr[0]=='L' && ( vr[1]=='O'
				    || vr[1]=='T' )
		    || vr[0]=='P' &&   vr[1]=='N'
		    || vr[0]=='S' && ( vr[1]=='H'
				    || vr[1]=='L'
				    || vr[1]=='S'
				    || vr[1]=='T' )
		    || vr[0]=='T' &&   vr[1]=='M'
		    || vr[0]=='U' && ( vr[1]=='I'
				    || vr[1]=='L'
				    || vr[1]=='S' );
	}

	/**
	 * @param	vr
	 */
	public static final boolean isAffectedBySpecificCharacterSet(byte[] vr) {
		return isLongStringVR(vr)
		    || isLongTextVR(vr)
		    || isPersonNameVR(vr)
		    || isShortStringVR(vr)
		    || isShortTextVR(vr)
		    || isUnlimitedCharactersVR(vr)
		    || isUnlimitedTextVR(vr);
	}
	
	/**
	 * @param	vr
	 */
	public static final String getAsString(byte[] vr) {
		return vr == null ? "" : new String(vr);
	}
	
	/**
	 * <p>Get the length of the "word" corresponding to an individual value for this VR,
	 * such as may be needed when swapping the endianness of values.</p>
	 *
	 * @param	vr
	 */
	public static final int getWordLengthOfValueAffectedByEndianness(byte[] vr) {
		int length = 1;
		if (isSignedShortVR(vr)
		 || isUnsignedShortVR(vr)
		 || isUnspecifiedShortVR(vr)
		 || isOtherWordVR(vr)
		 || isUnspecifiedShortOrOtherWordVR(vr)
		) {
			length=2;
		}
		
		if (isSignedLongVR(vr)
		 || isUnsignedLongVR(vr)
		 || isFloatSingleVR(vr)
		 || isOtherFloatVR(vr)
		 || isOtherLongVR(vr)
		) {
			length=4;
		}
		
		if (isFloatDoubleVR(vr)
		 || isOtherDoubleVR(vr)
		) {
			length=8;
		}
		
		return length;
	}
}




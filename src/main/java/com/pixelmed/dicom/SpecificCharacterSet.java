/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.HexDump;

import java.io.*;
import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to encapsulate the functionality defined by the DICOM Specific Character Set
 * attribute, including the ability to parse the string values of the attribute and then
 * apply the appropriate character conversions from byte array values into Java's internal
 * Unicode representation contained in {@link java.lang.String String}.
 *
 * @author	dclunie
 */
public class SpecificCharacterSet {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SpecificCharacterSet.java,v 1.35 2017/01/24 10:50:39 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SpecificCharacterSet.class);

	/***/
	private String useEncoding;			// really should make this an enumeration, then we could use string literal pool values and match without equals() :(
	/***/
	private boolean useISO2022=false;
	/***/
	static private boolean useOwnJIS;
	/***/
	static private boolean useOwnJISCheckPerformed;
	/***/
	static private HashMap ownJIS0208Mapping;
	/***/
	static private HashMap ownJIS0212Mapping;
	
	/***/
	public String toString() {
		return useEncoding;
	}
	
	/**
	 * <p>Check a byte array for the presence of non-ASCII bytes.
	 *
	 * @param	bytes		the bytes to check
	 * @param	offset		the offset into the byte array
	 * @param	length		how many bytes to check
	 */
	static public boolean byteArrayContainsNonASCIIValues(byte[] bytes,int offset,int length) {
		for (int i=offset; i<offset+length;++i) {
			if ((bytes[i]&0xFF) > 0x7F) {
				return true;
			}
		}
		return false;
	}

	/**
	 * <p>Check a byte array for the presence of non-ASCII bytes.
	 *
	 * @param	bytes		the bytes to check
	 */
	static public boolean byteArrayContainsNonASCIIValues(byte[] bytes) {
		return byteArrayContainsNonASCIIValues(bytes,0,bytes.length);
	}

	/**
	 * @return true if JIS encodings are working properly
	 */
	private boolean testIfNativeJISWorking() {
		slf4jlogger.trace("testIfNativeJISWorking()");
		boolean success=false;
		byte[] jis0208bytes = {
			(byte)0x3b,(byte)0x33,(byte)0x45,(byte)0x44
		};
		try {
			String string = new String(jis0208bytes,"JIS0208");
			byte[] utf8Bytes = string.getBytes("UTF8");
			slf4jlogger.trace("testIfNativeJISWorking():src = {}",HexDump.byteArrayToHexString(jis0208bytes));
			slf4jlogger.trace("testIfNativeJISWorking():dst = {}",HexDump.byteArrayToHexString(utf8Bytes));
			// if not working will come back unchanged (3b 33 45 44)
			// if working will come back e5 b1 b1 e7 94 b0
			success =   utf8Bytes.length == 6
				&& (utf8Bytes[0]&0xff) == 0xe5
				&& (utf8Bytes[1]&0xff) == 0xb1
				&& (utf8Bytes[2]&0xff) == 0xb1
				&& (utf8Bytes[3]&0xff) == 0xe7
				&& (utf8Bytes[4]&0xff) == 0x94
				&& (utf8Bytes[5]&0xff) == 0xb0
				;
		}
		catch (java.io.UnsupportedEncodingException e) {
			//slf4jlogger.error("", e);
		}
		slf4jlogger.trace("testIfNativeJISWorking(): returns {}",success);
		return success;
	}
	
	/**
	 */
	static private HashMap initializeOwnMapping(String name) {
		slf4jlogger.trace("initializeOwnMapping(): start {}",name);
		HashMap map=new HashMap();
		InputStream ownMappingSourceStream = SpecificCharacterSet.class.getResourceAsStream("/com/pixelmed/dicom/"+name);
		if (ownMappingSourceStream != null) {
slf4jlogger.trace("Opening {}",name);
			BufferedReader reader = new BufferedReader(new InputStreamReader(ownMappingSourceStream));
			String line;
			try {
				while ((line=reader.readLine()) != null) {
slf4jlogger.trace("Read {}",line);
					StringTokenizer tokens = new StringTokenizer(line);
					if (tokens.countTokens() == 2) {
						// the decode method handles the 0x
						// can't use Short.decode() because some values "too large"
						Short jisvalue = new Short(Integer.decode(tokens.nextToken()).shortValue());
						Character univalue = new Character((char)(Integer.decode(tokens.nextToken()).shortValue()));
slf4jlogger.trace("Decoded 0x"+Integer.toHexString(jisvalue.intValue())+" to 0x"+Integer.toHexString(univalue.charValue()));
						map.put(jisvalue,univalue);
					}
				}
			}
			catch (IOException e) {
				slf4jlogger.error("", e);
			}
		}
slf4jlogger.trace("initializeOwnMapping(): done {}",name);
		return map;
	}
	
	/**
	 */
	static private void initializeOwnJIS0208Mapping() {
		ownJIS0208Mapping=initializeOwnMapping("JIS0208Mapping.dat");
	}
	
	/**
	 */
	//static private void initializeOwnJIS0212Mapping() {
	//	ownJIS0212Mapping=initializeOwnMapping("JIS0212Mapping.dat");
	//}
	
	/**
	 * <p>Get an encoding capable of handling characters from the specified set of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}ss.</p>
	 *
	 * @param	setOfUnicodeBlocks		the set of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s that need to be encodable
	 * @return							an encoding to feed to
	 *
	 */
	public static String getSuitableEncodingFromSetOfUnicodeBlocks(Set setOfUnicodeBlocks) {
		String encoding = "UTF8";
		if (setOfUnicodeBlocks == null) {
			encoding = "ASCII";
		}
		else {
			int l = setOfUnicodeBlocks.size();
slf4jlogger.trace("getSuitableEncodingFromSetOfUnicodeBlocks(): setOfUnicodeBlocks.size()={}",l);
			if (l == 0) {
				encoding = "ASCII";
			}
			else if (l > 2) {
				encoding = "UTF8";
			}
			else if (l == 1) {
				if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.BASIC_LATIN)) {
					encoding = "ASCII";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.LATIN_1_SUPPLEMENT)) {
					encoding = "ISO8859_1";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.CYRILLIC)) {
					encoding = "ISO8859_5";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.ARABIC)) {
					encoding = "ISO8859_6";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.GREEK)) {
					encoding = "ISO8859_7";
				}
				// else leave to UTF8
			}
			else if (l == 2 && setOfUnicodeBlocks.contains(Character.UnicodeBlock.BASIC_LATIN)) {
				if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.LATIN_1_SUPPLEMENT)) {
					encoding = "ISO8859_1";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.CYRILLIC)) {
					encoding = "ISO8859_5";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.ARABIC)) {
					encoding = "ISO8859_6";
				}
				else if (setOfUnicodeBlocks.contains(Character.UnicodeBlock.GREEK)) {
					encoding = "ISO8859_7";
				}
				// else leave to UTF8
			}
		}
slf4jlogger.trace("getSuitableEncodingFromSetOfUnicodeBlocks(): encoding={}",encoding);
		return encoding;
	}
	
	/**
	 * <p>Get the set of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s used in a string.</p>
	 *
	 * @param	value	the string
	 * @return			a {@link java.util.Set Set} of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s
	 *
	 */
	public static Set getSetOfUnicodeBlocksUsedBy(String value) {
		HashSet setOfUnicodeBlocks = new HashSet();
		if (value != null && value.length() > 0) {
slf4jlogger.trace("getSetOfUnicodeBlocksUsedBy(): value={}",value);
			int l = value.length();
			char[] chars = new char[l];
			value.getChars(0,l,chars,0);
			for (int i=0; i<value.length(); ++i) {
				char c = chars[i];
				Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
slf4jlogger.trace("getSetOfUnicodeBlocksUsedBy(): - character <"+c+"> block = "+block);
				setOfUnicodeBlocks.add(block);
			}
		}
		return setOfUnicodeBlocks;
	}
	
	/**
	 * <p>Get all of the values of the string attributes of a dataset.</p>
	 *
	 * <p>Recurses into SequenceAttributes.</p>
	 *
	 * @param	list	the list of attributes
	 * @return			a {@link java.util.Set Set} of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s
	 *
	 */
	public static String getAllStringValuesAffectedBySpecificCharacterSet(AttributeList list) {
		StringBuffer str = new StringBuffer();
		if (list != null) {
			Iterator it = list.values().iterator();
			while (it.hasNext()) {
				Attribute a = (Attribute)it.next();
				if (a != null) {
					if (a instanceof SequenceAttribute) {
						Iterator is = ((SequenceAttribute)a).iterator();
						while (is.hasNext()) {
							SequenceItem item = (SequenceItem)is.next();
							if (item != null) {
								AttributeList subList = item.getAttributeList();
								if (subList != null) {
									str.append(getAllStringValuesAffectedBySpecificCharacterSet(subList));
								}
							}
						}
					}
					else if (a instanceof StringAttributeAffectedBySpecificCharacterSet) {
						try {
							String[] sv = a.getStringValues();
							if (sv != null) {
								for (int i=0; i< sv.length; ++i) {
									String v = sv[i];
									if (v != null) {
										str.append(v);
									}
								}
							}
						}
						catch (DicomException e) {
							// don't worry if can't get string values for some reason
							slf4jlogger.error("", e);
						}
					}
				}
			}
		}
		return str.toString();
	}
	
	/**
	 * <p>Get the set of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s used in all of the values of the string attributes of a dataset.</p>
	 *
	 * <p>Recurses into SequenceAttributes.</p>
	 *
	 * @param	list	the list of attributes
	 * @return			a {@link java.util.Set Set} of {@link java.lang.Character.UnicodeBlock Character.UnicodeBlock}s
	 *
	 */
	public static Set getSetOfUnicodeBlocksUsedBy(AttributeList list) {
		return getSetOfUnicodeBlocksUsedBy(getAllStringValuesAffectedBySpecificCharacterSet(list));
	}
	
	/**
	 * <p>Construct a character set handler capable of handling characters from all the values of the string attributes of a dataset.</p>
	 *
	 * @param	list	the list of attributes
	 *
	 */
	public SpecificCharacterSet(AttributeList list) {
		this(getSetOfUnicodeBlocksUsedBy(list));
	}
	
	/**
	 * <p>Construct a character set handler capable of handling characters from the specified set of <code>Character.UnicodeBlock</code>s.</p>
	 *
	 * @param	setOfUnicodeBlocks		the set of <code>Character.UnicodeBlock</code>s that need to be encodable
	 *
	 */
	public SpecificCharacterSet(Set setOfUnicodeBlocks) {
		useEncoding=getSuitableEncodingFromSetOfUnicodeBlocks(setOfUnicodeBlocks);
		useISO2022=false;
	}
	
	/**
	 * <p>Construct a character set handler from the values of the Specific Character Set attribute.</p>
	 *
	 * @param	specificCharacterSetAttributeValues	the values of Specific Character Set
	 */
	public SpecificCharacterSet(String[] specificCharacterSetAttributeValues) {
		this(specificCharacterSetAttributeValues,null);
	}
	
	/**
	 * <p>Construct a character set handler from the values of the Specific Character Set attribute.</p>
	 *
	 * @param	specificCharacterSetAttributeValues	the values of Specific Character Set as String
	 * @param	specificCharacterSetByteValues		the values of Specific Character Set as byte[]
	 */
	public SpecificCharacterSet(String[] specificCharacterSetAttributeValues,byte[] specificCharacterSetByteValues) {
		//this.specificCharacterSetAttributeValues=specificCharacterSetAttributeValues;
		useEncoding=null;	// see "http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html" for values JRE recognizes
		useISO2022=false;
		if (!useOwnJISCheckPerformed) {
			useOwnJIS=!testIfNativeJISWorking();
slf4jlogger.trace("SpecificCharacterSet(): useOwnJIS={}",useOwnJIS);
			useOwnJISCheckPerformed=true;
		}
		if (specificCharacterSetAttributeValues != null && specificCharacterSetAttributeValues.length >= 1) {
			String firstValue = specificCharacterSetAttributeValues[0];
slf4jlogger.trace("SpecificCharacterSet(): firstValue={}",firstValue);
			if (firstValue == null || firstValue.equals("")) {
				useEncoding="ASCII";
			}
			else if (firstValue.equals("ISO_IR 100")) {
				useEncoding="ISO8859_1";
			}
			else if (firstValue.equals("ISO_IR 101")) {
				useEncoding="ISO8859_2";
			}
			else if (firstValue.equals("ISO_IR 109")) {
				useEncoding="ISO8859_3";
			}
			else if (firstValue.equals("ISO_IR 110")) {
				useEncoding="ISO8859_4";
			}
			else if (firstValue.equals("ISO_IR 144")) {
				useEncoding="ISO8859_5";
			}
			else if (firstValue.equals("ISO_IR 127")) {
				useEncoding="ISO8859_6";
			}
			else if (firstValue.equals("ISO_IR 126")) {
				useEncoding="ISO8859_7";
			}
			else if (firstValue.equals("ISO_IR 138")) {
				useEncoding="ISO8859_8";
			}
			else if (firstValue.equals("ISO_IR 148")) {
				useEncoding="ISO8859_9";
			}
			else if (firstValue.equals("ISO_IR 166")) {
				useEncoding="TIS620";
			}
			else if (firstValue.equals("ISO_IR 192")) {
				useEncoding="UTF8";
			}
			else if (firstValue.equals("ISO_IR 6")) {
				useEncoding="ASCII";			// "ISO_IR 6" is not a DICOM defined term, since it is equivalent to the empty string, but SIENET puts it in sometimes
			}
			else if (firstValue.equals("GB18030")) {
				useEncoding="GB18030";
			}
			else if (firstValue.equals("GBK")) {
				useEncoding="GB18030";
			}
			else if (firstValue.equals("GB2312")) {
				useEncoding="GB18030";
			}
			else if (firstValue.equals("ISO_IR 13")) {
				useEncoding="JIS0201";
			}
			else if (firstValue.equals("ISO 2022 IR 6")) {
				useISO2022=true;
				useEncoding="ASCII";			// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 100")) {
				useISO2022=true;
				useEncoding="ISO8859_1";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 101")) {
				useISO2022=true;
				useEncoding="ISO8859_2";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 109")) {
				useISO2022=true;
				useEncoding="ISO8859_3";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 110")) {
				useISO2022=true;
				useEncoding="ISO8859_4";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 144")) {
				useISO2022=true;
				useEncoding="ISO8859_5";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 127")) {
				useISO2022=true;
				useEncoding="ISO8859_6";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 126")) {
				useISO2022=true;
				useEncoding="ISO8859_7";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 138")) {
				useISO2022=true;
				useEncoding="ISO8859_8";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 148")) {
				useISO2022=true;
				useEncoding="ISO8859_9";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 13")) {
				useISO2022=true;
				useEncoding="JIS0201";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 166")) {
				useISO2022=true;
				useEncoding="TIS620";
			}
			else if (firstValue.equals("ISO 2022 IR 87")) {
				useISO2022=true;
				useEncoding="JIS0201";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 159")) {
				useISO2022=true;
				useEncoding="JIS0212";		// i.e. start with this before any escape sequences seen
			}
			else if (firstValue.equals("ISO 2022 IR 149")) {
				useISO2022=true;
				useEncoding="Cp949";		// IR 149 KS X 1001 - i.e. start with this before any escape sequences seen
			}
			else {
				slf4jlogger.trace("Check for big5 - specific character set as bytes: {}",HexDump.dump(specificCharacterSetByteValues));
				if (specificCharacterSetByteValues != null
				 && specificCharacterSetByteValues.length == 4
				 && (specificCharacterSetByteValues[0] & 0xff) == 0xff
				 && (specificCharacterSetByteValues[1] & 0xff) == 0xbe
				 && (specificCharacterSetByteValues[2] & 0xff) == 0xdd
				 && (specificCharacterSetByteValues[3] & 0xff) == 0xa8) {
					slf4jlogger.error("encountered non-standard illegal encoding of Big5 Specific Character Set");
					useEncoding="Big5";
				}
				else {
					slf4jlogger.warn("unrecognized first value of Specific Character Set, using ASCII; first value ={} ({})",firstValue,HexDump.dump(specificCharacterSetByteValues));
					useEncoding="ASCII";
				}
			}
			// Now look at any remaining values, solely to determine whether or not ISO2022 code extension techniques will be used
			for (int i=1; i<specificCharacterSetAttributeValues.length; ++i) {
				String nextValue = specificCharacterSetAttributeValues[i];
				if (nextValue != null && nextValue.startsWith("ISO 2022")) useISO2022=true;
				// the actual character set is irrelevant, since it is activated later by a specific escape sequence anyway
			}
		}
		else {
			useEncoding="ASCII";
		}
		slf4jlogger.trace("SpecificCharacterSet(): useEncoding={}",useEncoding);
	}
	
	public String getValueToUseInSpecificCharacterSetAttribute() {
		slf4jlogger.trace("getSuitableEncodingFromSetOfUnicodeBlocks(): useEncoding={}",useEncoding);
		String value = "ISO_IR 192";		// default to UTF-8 if not recognized or (was) using ISO2022 (don't support encoding with ISO 2022 escapes)
		if (!useISO2022) {
			if (useEncoding.equals("ASCII")) {	// use of == rather than equals() would OK since only string literal pool values are possible for useEncoding, which is private to this class, but safer to use equals() in case we change something
				value="";
			}
			else if (useEncoding.equals("ISO8859_1")) {
				value = "ISO_IR 100";
			}
			else if (useEncoding.equals("ISO8859_2")) {
				value = "ISO_IR 101";
			}
			else if (useEncoding.equals("ISO8859_3")) {
				value = "ISO_IR 109";
			}
			else if (useEncoding.equals("ISO8859_4")) {
				value = "ISO_IR 110";
			}
			else if (useEncoding.equals("ISO8859_5")) {
				value = "ISO_IR 144";
			}
			else if (useEncoding.equals("ISO8859_6")) {
				value = "ISO_IR 127";
			}
			else if (useEncoding.equals("ISO8859_7")) {
				value = "ISO_IR 126";
			}
			else if (useEncoding.equals("ISO8859_8")) {
				value = "ISO_IR 138";
			}
			else if (useEncoding.equals("ISO8859_9")) {
				value = "ISO_IR 148";
			}
			else if (useEncoding.equals("TIS620")) {
				value = "ISO_IR 166";
			}
			else if (useEncoding.equals("UTF8")) {
				value = "ISO_IR 192";
			}
			else if (useEncoding.equals("GB18030")) {
				value = "GB18030";
			}
			else if (useEncoding.equals("JIS0201")) {
				value = "ISO_IR 13";
			}
		}
		slf4jlogger.trace("getSuitableEncodingFromSetOfUnicodeBlocks(): return value={}",value);
		return value;
	}

	/**
	 * <p>Translate a byte array (such as a value from a DICOM attribute), using the
	 * specified Specific Character Set, into a {@link java.lang.String String}.
	 *
	 * @param	bytes		the bytes to translate
	 * @param	offset		the offset into the byte array to start translation
	 * @param	length		how many bytes to translate
	 * @param	useMapping
	 */
	private String translateByteArrayToString(byte[] bytes,int offset,int length,HashMap useMapping) {
		String s = null;
		if (useMapping != null) {
			StringBuffer buf = new StringBuffer(length/2);
			for (int i=offset; i<offset+length;) {
				short hibyte = (short)(((short)(bytes[i++])) & 0xff);
				short lobyte = (short)(((short)(bytes[i++])) & 0xff);
				short lookup = (short)((hibyte<<8) | lobyte);
				Character c = (Character)useMapping.get(new Short(lookup));
				slf4jlogger.trace("translateByteArrayToString(): Mapped from 0x{} to {}",Integer.toHexString(lookup),c);
				if (c != null) buf.append(c.charValue());
			}
			s=buf.toString();
		}
		return s;
	}

	/**
	 * <p>Translate a byte array (such as a value from a DICOM attribute), using the
	 * specified encoding, into a {@link java.lang.String String}.
	 *
	 * @param	bytes		the bytes to translate
	 * @param	offset		the offset into the byte array to start translation
	 * @param	length		how many bytes to translate
	 * @param	useEncoding	the encoding to use
	 */
	private String translateByteArrayToString(byte[] bytes,int offset,int length,String useEncoding) {
		slf4jlogger.trace("translateByteArrayToString() useEncoding={} offset={} length={}",useEncoding,offset,length);
		String s = null;
		if (length > 0 && useOwnJIS && useEncoding.equals("JIS0208")) {
			slf4jlogger.trace("translateByteArrayToString() using own JIS0208");
			if (ownJIS0208Mapping == null) {
				initializeOwnJIS0208Mapping();
			}
			s=translateByteArrayToString(bytes,offset,length,ownJIS0208Mapping);
		}
		//else if (length > 0 && useOwnJIS && useEncoding.equals("JIS0212")) {
		//	if (ownJIS0212Mapping == null) {
		//		initializeOwnJIS0212Mapping();
		//	}
		//	s=translateByteArrayToString(bytes,offset,length,ownJIS0212Mapping);
		//}
		else {
			try {
				if (useEncoding.equals("ASCII") && byteArrayContainsNonASCIIValues(bytes,offset,length)) {
					s=new String(bytes,offset,length,"ISO8859_1");		// More useful to assume that it is incorrectly Latin 1, than nothing at all
				}
				else {
					s=new String(bytes,offset,length,useEncoding);
				}
			}
			catch (UnsupportedEncodingException e) {
				slf4jlogger.error("",e);
				s=new String(bytes,offset,length);		// use default ... better than returning null (000307)
			}
		}
		return s;
	}

	/**
	 * <p>Translate a byte array (such as a value from a DICOM attribute), using the
	 * specified Specific Character Set, into a {@link java.lang.String String}.
	 *
	 * @param	bytes		the bytes to translate
	 * @param	offset		the offset into the byte array to start translation
	 * @param	length		how many bytes to translate
	 * @return			the string decoded according to the specific character set
	 */
	public String translateByteArrayToString(byte[] bytes,int offset,int length) {
		slf4jlogger.trace("translateByteArrayToString():  byte array is:\n{}",com.pixelmed.utils.HexDump.dump(bytes));
		slf4jlogger.trace("translateStringToByteArray(): useEncoding is {}",useEncoding);
		String s = null;
		if (useEncoding == null) {
			s=new String(bytes,0,length);
		}
		else {
			if (useISO2022) {
				// start at beginning with useEncoding (from 1st value of Specific Character Set)
				// and use that until we see an escape sequence
				// note that this is all a bit hokey since we are assuming a lot about what is in GL vs. GR
				// and are using the escape sequence to switch both which is a bit naughty
				StringBuffer sbuf = new StringBuffer();
				int done = 0;
				int startlast = 0;
				int bytesperchar = 1;
				String lastEncoding = useEncoding;
				while (done < length) {
					slf4jlogger.trace("translateByteArrayToString() looping startlast={} done={}",startlast,done);
					if (bytes[done] == 0x1b) { // escape character
						slf4jlogger.trace("translateByteArrayToString() escape character");
						if (done > startlast) sbuf.append(translateByteArrayToString(bytes,startlast,done-startlast,lastEncoding));
						if (bytes[done+1] == 0x28 && bytes[done+2] == 0x42) {
							lastEncoding="ASCII";		// IR 6 ISO 646
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x41) {
							lastEncoding="ISO8859_1";		// IR 100
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x42) {
							lastEncoding="ISO8859_2";		// IR 101
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x43) {
							lastEncoding="ISO8859_3";		// IR 109
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x44) {
							lastEncoding="ISO8859_4";		// IR 110
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x4c) {
							lastEncoding="ISO8859_5";		// IR 144
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x47) {
							lastEncoding="ISO8859_6";		// IR 127
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x46) {
							lastEncoding="ISO8859_7";		// IR 126
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x48) {
							lastEncoding="ISO8859_8";		// IR 138
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x4d) {
							lastEncoding="ISO8859_9";		// IR 148
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x2d && bytes[done+2] == 0x54) {
							lastEncoding="TIS620";		// IR 166
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x29 && bytes[done+2] == 0x49) {
							lastEncoding="JIS0201";		// IR 13
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x28 && bytes[done+2] == 0x4a) {
							lastEncoding="JIS0201";		// IR 13
							bytesperchar=1;
							done+=3;
						}
						else if (bytes[done+1] == 0x24 && bytes[done+2] == 0x42) {
							lastEncoding="JIS0208";		// IR 87
							bytesperchar=2;
							done+=3;
						}
						else if (bytes[done+1] == 0x24 && bytes[done+2] == 0x29 && bytes[done+3] == 0x41) {
							// encountered in Agfa SpecificCharacterSet = "\\ISO 2022 GBK"
							lastEncoding="GBK";				// hmm :) this escape sequence really is supposed to be confined to GB2312 (EUC_CN) ... see RFC 1922
							bytesperchar=2;
							done+=4;
						}
						else if (bytes[done+1] == 0x24 && bytes[done+2] == 0x28 && bytes[done+3] == 0x44) {
							lastEncoding="JIS0212";		// IR 159
							bytesperchar=2;
							done+=4;
						}
						else if (bytes[done+1] == 0x24 && bytes[done+2] == 0x29 && bytes[done+3] == 0x43) {
							lastEncoding="Cp949";		// IR 149 KS X 1001
							bytesperchar=-1;		// flag to trigger high bit based selection of 1 or 2 bytes
							done+=4;
						}
						else {
							done+=3;
						}
						startlast=done;
					}
					else {
						done+=(bytesperchar == -1 ? ((bytes[done]&0x80) == 1 ? 2 : 1) : bytesperchar);
					}
				}
				if (done > startlast) sbuf.append(translateByteArrayToString(bytes,startlast,done-startlast,lastEncoding));
				s=sbuf.toString();
			}
			else {
				s=translateByteArrayToString(bytes,0,length,useEncoding);
			}
		}
		slf4jlogger.trace("translateByteArrayToString(): result string is <{}>",s);
		slf4jlogger.trace("translateByteArrayToString(): result string is:\n{}",com.pixelmed.utils.StringUtilities.dump(s));
		return s;
	}

	/**
	 * <p>Encode a string into a byte array.</p>
	 *
	 * <p>Does not currently support ISO 2022 (or JIS 0208 or 0212 if 1.4.1 bug present).</p>
	 *
	 * @param	string				the string to be encoded
	 * @return					the byte array encoded according to the specific character set
	 * @throws	UnsupportedEncodingException
	 */
	public byte[] translateStringToByteArray(String string) throws UnsupportedEncodingException {
		slf4jlogger.trace("translateStringToByteArray(): string is <{}>",string);
		slf4jlogger.trace("translateStringToByteArray(): string is:\n{}",com.pixelmed.utils.StringUtilities.dump(string));
		slf4jlogger.trace("translateStringToByteArray(): useEncoding is {}",useEncoding);
		byte[] b = useEncoding == null ? string.getBytes() : string.getBytes(useEncoding);
		slf4jlogger.trace("translateStringToByteArray(): return byte array is:\n{}",com.pixelmed.utils.HexDump.dump(b));
		return b;
	}

	// What follows is just for testing
	
	/**
	 * @param	fileName
	 * @param	example
	 * @param	specificCharacterSet
	 */
	static private void createDicomFileWithPatientName(String fileName,byte[] example,String specificCharacterSet) {
		slf4jlogger.trace("createDicomFileWithPatientName(): fileName {}",fileName);
		try {
			BinaryOutputStream o=new BinaryOutputStream(new FileOutputStream(fileName),false);
			{
				AttributeTag tag = TagFromName.SpecificCharacterSet;
				o.writeUnsigned16(tag.getGroup());
				o.writeUnsigned16(tag.getElement());
			}
			o.write((byte)'C'); o.write((byte)'S');
			byte[] scs = specificCharacterSet.getBytes("ASCII");
			int length=scs.length;
			boolean pad=false;
			if (length%2 != 0) { ++length; pad=true; }
			o.writeUnsigned16(length);
			o.write(scs);
			if (pad) o.write((byte)' ');
			{
				AttributeTag tag = TagFromName.PatientName;
				o.writeUnsigned16(tag.getGroup());
				o.writeUnsigned16(tag.getElement());
			}
			o.write((byte)'P'); o.write((byte)'N');
			length=example.length;
			pad=false;
			if (length%2 != 0) { ++length; pad=true; }
			o.writeUnsigned16(length);
			o.write(example);
			if (pad) o.write((byte)' ');
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
		}
	}

	/**
	 * @param	args
	 */
	static public void main(String args[]) {

		byte[] h31example = {
			(byte)0x59,(byte)0x61,(byte)0x6d,(byte)0x61,(byte)0x64,(byte)0x61,(byte)0x5e,(byte)0x54,(byte)0x61,(byte)0x72,(byte)0x6f,(byte)0x75,(byte)0x3d,(byte)0x1b,
			(byte)0x24,(byte)0x42,(byte)0x3b,(byte)0x33,(byte)0x45,(byte)0x44,(byte)0x1b,(byte)0x28,(byte)0x42,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x42,
			(byte)0x40,(byte)0x4f,(byte)0x3a,(byte)0x1b,(byte)0x28,(byte)0x42,(byte)0x3d,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x24,(byte)0x64,(byte)0x24,(byte)0x5e,
			(byte)0x24,(byte)0x40,(byte)0x1b,(byte)0x28,(byte)0x42,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x24,(byte)0x3f,(byte)0x24,(byte)0x6d,(byte)0x24,
			(byte)0x26,(byte)0x1b,(byte)0x28,(byte)0x42
		};

		String h31exampleSpecificCharacterSet = "\\ISO 2022 IR 87";

		byte[] h32example = {
			(byte)0xd4,(byte)0xcf,(byte)0xc0,(byte)0xde,(byte)0x5e,(byte)0xc0,(byte)0xdb,(byte)0xb3,(byte)0x3d,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x3b,(byte)0x33,
			(byte)0x45,(byte)0x44,(byte)0x1b,(byte)0x28,(byte)0x4a,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x42,(byte)0x40,(byte)0x4f,(byte)0x3a,(byte)0x1b,
			(byte)0x28,(byte)0x4a,(byte)0x3d,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x24,(byte)0x64,(byte)0x24,(byte)0x5e,(byte)0x24,(byte)0x40,(byte)0x1b,(byte)0x28,
			(byte)0x4a,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x42,(byte)0x24,(byte)0x3f,(byte)0x24,(byte)0x6d,(byte)0x24,(byte)0x26,(byte)0x1b,(byte)0x28,(byte)0x4a
		};

		String h32exampleSpecificCharacterSet = "ISO 2022 IR 13\\ISO 2022 IR 87";

		byte[] i2example = {
			(byte)0x48,(byte)0x6f,(byte)0x6e,(byte)0x67,(byte)0x5e,(byte)0x47,(byte)0x69,(byte)0x6c,(byte)0x64,(byte)0x6f,(byte)0x6e,(byte)0x67,(byte)0x3d,
			(byte)0x1b,(byte)0x24,(byte)0x29,(byte)0x43,(byte)0xfb,(byte)0xf3,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x29,(byte)0x43,(byte)0xd1,(byte)0xce,
			(byte)0xd4,(byte)0xd7,(byte)0x3d,(byte)0x1b,(byte)0x24,(byte)0x29,(byte)0x43,(byte)0xc8,(byte)0xab,(byte)0x5e,(byte)0x1b,(byte)0x24,(byte)0x29,
			(byte)0x43,(byte)0xb1,(byte)0xe6,(byte)0xb5,(byte)0xbf
		};

		String i2exampleSpecificCharacterSet = "\\ISO 2022 IR 149";

		byte[] x1example = {
			(byte)0x57,(byte)0x61,(byte)0x6e,(byte)0x67,(byte)0x5e,(byte)0x58,(byte)0x69,(byte)0x61,(byte)0x6f,(byte)0x44,(byte)0x6f,(byte)0x6e,(byte)0x67,(byte)0x3d,
			(byte)0xe7,(byte)0x8e,(byte)0x8b,(byte)0x5e,(byte)0xe5,(byte)0xb0,(byte)0x8f,(byte)0xe6,(byte)0x9d,(byte)0xb1,(byte)0x3d
		};

		String x1exampleSpecificCharacterSet = "ISO_IR 192";

		byte[] x2example = {
			(byte)0x57,(byte)0x61,(byte)0x6e,(byte)0x67,(byte)0x5e,(byte)0x58,(byte)0x69,(byte)0x61,(byte)0x6f,(byte)0x44,(byte)0x6f,(byte)0x6e,(byte)0x67,(byte)0x3d,(byte)0xcd,(byte)0xf5,
			(byte)0x5e,(byte)0xd0,(byte)0xa1,(byte)0xb6,(byte)0xab,(byte)0x3d
		};

		String x2exampleSpecificCharacterSet = "GB18030";

		String prefixPath = args.length > 0 && args[0] != null ? (args[0]+"/") : "";
		createDicomFileWithPatientName(prefixPath+"h31example.dcm",h31example,h31exampleSpecificCharacterSet);
		createDicomFileWithPatientName(prefixPath+"h32example.dcm",h32example,h32exampleSpecificCharacterSet);
		createDicomFileWithPatientName(prefixPath+"i2example.dcm",i2example,i2exampleSpecificCharacterSet);
		createDicomFileWithPatientName(prefixPath+"x1example.dcm",x1example,x1exampleSpecificCharacterSet);
		createDicomFileWithPatientName(prefixPath+"x2example.dcm",x2example,x2exampleSpecificCharacterSet);
		
		try {
			String name = "\u0394\u03b9\u03bf\u03bd\u03c5\u03c3\u03b9\u03bf\u03c2";
			byte[] iso88597Bytes = name.getBytes("ISO8859_7");
			createDicomFileWithPatientName(prefixPath+"greek.dcm",iso88597Bytes,"ISO_IR 126");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
		try {
			String name = "Buc^Jérôme";
			byte[] iso88591Bytes = name.getBytes("ISO8859_1");
			createDicomFileWithPatientName(prefixPath+"french.dcm",iso88591Bytes,"ISO_IR 100");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
		try {
			String name = "Äneas^Rüdiger";
			byte[] iso88591Bytes = name.getBytes("ISO8859_1");
			createDicomFileWithPatientName(prefixPath+"german.dcm",iso88591Bytes,"ISO_IR 100");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		try {
			// from "http://faculty.washington.edu/heer/qabbani1.htm" Qabbani^Nizar
			byte[] iso88596Bytes = { (byte)0xe2,(byte)0xc8,(byte)0xc7,(byte)0xe6,(byte)0xea,(byte)0x5e,(byte)0xe4,(byte)0xe6,(byte)0xd2,(byte)0xc7,(byte)0xd1};
			createDicomFileWithPatientName(prefixPath+"arabic.dcm",iso88596Bytes,"ISO_IR 127");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
		try {
			// from "http://www.p.lodz.pl/I35/personal/jw37/EUROPE/europe-L.html" luxembourg in russian
			byte[] iso88595Bytes = { (byte)0xbb,(byte)0xee,(byte)0xda,(byte)0x63,(byte)0x65,(byte)0xdc,(byte)0xd1,(byte)0x79,(byte)0x70,(byte)0xd3 };
			createDicomFileWithPatientName(prefixPath+"russian.dcm",iso88595Bytes,"ISO_IR 144");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
		try {
			// from http://www.learn-hebrew-names.com/ Sharon^Deborah
			// String name = "שָׁרוֹן^דְּבוֹרָה"; byte[] iso88598Bytes = name.getBytes("ISO8859_8"); // doesn't work ... inserts a bunch of 0x3f (question mark) characters between letters
			byte[] iso88598Bytes = { (byte)0xf9,(byte)0xf8,(byte)0xe5,(byte)0xef,(byte)0x5e,(byte)0xe3,(byte)0xe1,(byte)0xe5,(byte)0xf8,(byte)0xe4 };
			createDicomFileWithPatientName(prefixPath+"hebrew.dcm",iso88598Bytes,"ISO_IR 138");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}



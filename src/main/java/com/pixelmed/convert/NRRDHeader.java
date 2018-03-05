/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.BinaryInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.LineNumberReader;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for extracting NRRD image input format headers.</p>
 *
 * @author	dclunie
 */
public class NRRDHeader {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/NRRDHeader.java,v 1.7 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NRRDHeader.class);
	
	public String magic;
	
	public int byte_offset_of_binary;
	
	public Map<String,String> fields = new TreeMap<String,String>();
	public Map<String,String> keys = new TreeMap<String,String>();
	
	public static String[] getSpaceDelimitedValues(String values) {
		return values.split(" ");
	}
	
	enum Type {
		NONE,
		INT8,
		UINT8,
		INT16,
		UINT16,
		INT32,
		UINT32,
		INT64,
		UINT64,
		FLOAT32,
		FLOAT64,
		BLOCK;
						
		static final Map<String,Type> map = new HashMap<String,Type>();
		
		static final Type getType(String type) {
			return map.get(type);
		}
		
		static {
			map.put("signed char",INT8);
			map.put("int8",INT8);
			map.put("int8_t",INT8);
			
			map.put("uchar",UINT8);
			map.put("unsigned char",UINT8);
			map.put("uint8",UINT8);
			map.put("uint8_t",UINT8);
			
			map.put("short",INT16);
			map.put("short int",INT16);
			map.put("signed short",INT16);
			map.put("signed short int",INT16);
			map.put("int16",INT16);
			map.put("int16_t",INT16);
			
			map.put("ushort",UINT16);
			map.put("unsigned short",UINT16);
			map.put("unsigned short int",UINT16);
			map.put("uint16",UINT16);
			map.put("uint16_t",UINT16);
			
			map.put("int",INT32);
			map.put("signed int",INT32);
			map.put("int32",INT32);
			map.put("int32_t",INT32);
			
			map.put("uint",UINT32);
			map.put("unsigned int",UINT32);
			map.put("uint32",UINT32);
			map.put("uint32_t",UINT32);
			
			map.put("longlong",INT64);
			map.put("long long",INT64);
			map.put("long long int",INT64);
			map.put("signed long long",INT64);
			map.put("signed long long int",INT64);
			map.put("int64",INT64);
			map.put("int64_t",INT64);
			
			map.put("ulonglong",UINT64);
			map.put("unsigned long long",UINT64);
			map.put("unsigned long long int",UINT64);
			map.put("uint64",UINT64);
			map.put("uint64_t",UINT64);
			
			map.put("float",FLOAT32);
			
			map.put("double",FLOAT64);
			
			map.put("block",BLOCK);
		}
	}

	public Type getType() {
		Type type = null;
		String typeString = fields.get("type");
		if (typeString != null) {
			type = Type.getType(typeString);
		}
		return type;
	}
	
	public int getSingleIntegerValueOrThrowException(String key) throws NRRDException, NumberFormatException {
		String valueString = fields.get(key);
		if (valueString != null) {
			return Integer.parseInt(valueString);
		}
		else {
			throw new NRRDException("Missing value of "+key);
		}
	}

	public int[] getArrayOfIntegerValueOrThrowException(String key) throws NRRDException, NumberFormatException {
		String valueString = fields.get(key);
		if (valueString != null) {
			String[] arrayOfStringValues = getSpaceDelimitedValues(valueString);
			if (arrayOfStringValues != null && arrayOfStringValues.length > 0) {
				int[] arrayOfIntegerValues = new int[arrayOfStringValues.length];
				for (int i=0; i<arrayOfStringValues.length; ++i) {
					arrayOfIntegerValues[i] = Integer.parseInt(arrayOfStringValues[i]);
				}
				return arrayOfIntegerValues;
			}
			else {
				throw new NRRDException("Missing value of "+key);
			}
		}
		else {
			throw new NRRDException("Missing value of "+key);
		}
	}
	
	public static String[] getVectorTripleValuesFromString(String key,String valueString) throws NRRDException {
		String[] array = null;
		valueString = valueString.trim();
		if (valueString.startsWith("(") && valueString.endsWith(")")) {
			valueString = valueString.replace("(","");
			valueString = valueString.replace(")","");
			array = valueString.split(",");
			if (array.length != 3) {
				throw new NRRDException("vector value of "+key+" does not contain three values': "+valueString);
			}
		}
		else {
			throw new NRRDException("vector value of "+key+" not enclosed in '(' and ')': "+valueString);
		}
		return array;
	}

	public int getDimension() throws NRRDException, NumberFormatException {
		return getSingleIntegerValueOrThrowException("dimension");
	}
	
	public int[] getSizes() throws NRRDException, NumberFormatException {
		return getArrayOfIntegerValueOrThrowException("sizes");
	}
	
	public String getSpace() {
		return fields.get("space");
	}

	public String[] getSpaceDirections() {
		String[] array = null;
		String valueString = fields.get("space directions");
		if (valueString != null) {
			array = getSpaceDelimitedValues(valueString);
		}
		return array;
	}
	
	public String[] getSpaceOrigin() throws NRRDException {
		String[] array = null;
		String valueString = fields.get("space origin");
		if (valueString != null) {
			array = getVectorTripleValuesFromString("space origin",valueString);
		}
		return array;
	}
	
	public boolean isDataBigEndian() {
		String endian = fields.get("endian");
		return endian != null && endian.equals("big");	// NB. if anything else, or missing, assume little; note that this field is optional for single byte data, so don't throw exception
	}
	
	public boolean isDataGZIPEncoded() {
		String encoding = fields.get("encoding");
		return encoding != null && encoding.equals("gzip");
	}
	
	public String getDataFile() {
		return fields.get("data file");
	}
	
	public NRRDHeader(File inputFile) throws IOException, NRRDException {
		// Note that BufferedReader does not allow us to switch to reading bytes and may buffer ahead too far in supplied stream,
		// so we will read the entire header, and come back and read the following binary bytes later after reopening the file

		LineNumberReader headerReader = new LineNumberReader(new FileReader(inputFile));
		
		magic = headerReader.readLine();
		// do not care about the numeric version
		if (magic == null || !magic.startsWith("NRRD")) {
			throw new NRRDException("Not an NRRD magic number");
		}
		
		String line;
		while ((line=headerReader.readLine()) != null && line.length() > 0) {
			if (line.startsWith("#")) {
				String comment = line;
				slf4jlogger.info("Comment: \"{}\"",comment);
			}
			else {
				String[] split = line.split(": ",2);
				if (split.length == 2) {
					String field = split[0];
					String description = split[1];
					fields.put(field,description);
					slf4jlogger.info("Field: \"{}\" Description: \"{}\"",field,description);
				}
				else {
					split = line.split(":=",2);
					if (split.length == 2) {
						String key = split[0];
						String value = split[1];
						keys.put(key,value);
						slf4jlogger.info("Key: \"{}\" Value: \"{}\"",key,value);
					}
					else {
						slf4jlogger.info("Unrecognized pattern of NRRD header line # {}: \"{}\"",headerReader.getLineNumber(),line);
					}
				}
			}
		}
		headerReader.close();
		
		String dataFile = fields.get("data file");
		
		if (dataFile == null || dataFile.trim().length() == 0) {
			// no separate data file, so now we reopen the file, skipping characters until we reach an empty line, consume the line terminators, then record the byte offset to the binary data, if any
		
			byte_offset_of_binary = 0;
			InputStream in = new FileInputStream(inputFile);
			boolean lastCharWasNewLine = true;
			int c;
			while ((c=in.read())!= -1) {
				++byte_offset_of_binary;
				if (c == 0x0A) {	// '\n'
					if (lastCharWasNewLine) {
						// two '\n' in a row means the end of the header
						break;
					}
					lastCharWasNewLine = true;
				}
				else if (c == 0x0D) {	// '\r'
					// has no effect, may occur before '\n' as in "\r\n" in Windows, or not occur at all ... harmless to allow "\n\r\r\r\r\n", probably
				}
				else {
					lastCharWasNewLine = false;
				}
			}
			in.close();
			slf4jlogger.info("byte_offset_of_binary = {}",byte_offset_of_binary);
		}
		
	}

	/**
	 * <p>Read a NRRD image input format files and dump header.</p>
	 *
	 * @param	arg	the inputFile,
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 1) {
				new NRRDHeader(new File(arg[0]));
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: NRRDHeader inputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}


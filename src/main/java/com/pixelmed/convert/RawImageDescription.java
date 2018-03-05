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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for extracting a JSON encoded raw input format description.</p>
 *
 * <p>E.g.:</p>
 * <pre>
 * {
 * 	"type" : "float",
 * 	"rows" : "1600",
 * 	"columns" : "1600"
 * }
 * </pre>
 * @author	dclunie
 */
public class RawImageDescription {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/RawImageDescription.java,v 1.2 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RawImageDescription.class);
	
	// reuse the type descriptions from the NRRD format ...
	
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
		FLOAT64
		/*BLOCK*/;
						
		static final Map<String,Type> map = new HashMap<String,Type>();
		
		static final Type getType(String type) {
			return map.get(type.toLowerCase());
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
			
			//map.put("block",BLOCK);
		}
		
		public static String toString(Type type) {
			String typeString = "none";
			switch (type) {
				case INT8:		typeString="int8"; break;
				case UINT8:		typeString="uint8"; break;
				case INT16:		typeString="int16"; break;
				case UINT16:	typeString="uint16"; break;
				case INT32:		typeString="int32"; break;
				case UINT32:	typeString="uint32"; break;
				case INT64:		typeString="int64"; break;
				case UINT64:	typeString="uint64"; break;
				case FLOAT32:	typeString="float"; break;
				case FLOAT64:	typeString="double"; break;
			}
			return typeString;
		}
	}
	
	protected Type type;

	public Type getType() {
		return type;
	}
	
	protected int rows;
	protected int columns;
	protected int frames;

	protected boolean isDataBigEndian;
	
	public RawImageDescription(File formatFile) throws IOException, NumberFormatException {
		JsonReader jsonReader = Json.createReader(new FileReader(formatFile));
		JsonObject obj = jsonReader.readObject();
		for (String name : obj.keySet()) {
			JsonValue entry = obj.get(name);
			switch (name) {
				case "type":	type = Type.getType(((JsonString)entry).getString()); break;
				case "rows":	rows = Integer.parseInt(((JsonString)entry).getString()); break;
				case "columns":	columns = Integer.parseInt(((JsonString)entry).getString()); break;
				case "frames":	frames = Integer.parseInt(((JsonString)entry).getString()); break;
				case "endian":	isDataBigEndian = ((JsonString)entry).getString().toLowerCase().equals("big"); break;
			}
		}
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("type = "+Type.toString(type)+"\n");
		buf.append("rows = "+rows+"\n");
		buf.append("columns = "+columns+"\n");
		buf.append("frames = "+frames+"\n");
		buf.append("endian = "+(isDataBigEndian ? "big" : "little")+"\n");
		return buf.toString();
	}

	/**
	 * <p>Read a JSON encoded raw input format description and dump it.</p>
	 *
	 * @param	arg	the format description file
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 1) {
				RawImageDescription format = new RawImageDescription(new File(arg[0]));
				System.err.println(format.toString());
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: RawImageDescription formatFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}


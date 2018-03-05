/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.Date;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.utils.CopyStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
abstract class RequestHandler {
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/RequestHandler.java,v 1.10 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(RequestHandler.class);

	protected String stylesheetPath;
	
	protected RequestHandler(String stylesheetPath) {
		this.stylesheetPath=stylesheetPath;
	}
	
	/**
	 * @param	databaseInformationModel	the database, may be null if not required for the type of request
	 * @param	rootURL				the root to prepend to URL's embedded in responses
	 * @param	requestURI			the URI supplied in the HTTP message
	 * @param	request				the request parsed out of the the URI
	 * @param	requestType			the value XXXX of the <code>"?requestType=XXXX"</code> argument, which may be null
	 * @param	out				where to send the request response
	 * @throws					if cannot send the response
	 */
	abstract protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException;

	final public void sendHeaderAndBodyOfStream(OutputStream out,InputStream in,String nameToUse,String contentType) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		writer.print("HTTP/1.1 200 OK\r\n");
		writer.print("Content-Type: "+contentType+"\r\n");
		//writer.print("Content-Length: "+fileLength+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.flush();
		CopyStream.copy(new BufferedInputStream(in),out);
		out.flush();
	}
	
	private final static SimpleDateFormat responseDateTimeFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	final static public String getFormattedDateTimeForResponse(Date date) {
		return responseDateTimeFormatter.format(date);
	}

	final public void sendHeaderAndBodyOfFile(OutputStream out,File file,String nameToUse,String contentType) throws IOException {
		sendHeaderAndBodyOfFile(out,file,nameToUse,contentType,null/*lastModifiedDateTime*/);
	}
	
	final public void sendHeaderAndBodyOfFile(OutputStream out,File file,String nameToUse,String contentType,Date lastModified) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		long fileLength = file.length();
		slf4jlogger.debug("sendHeaderAndBodyOfFile(): Length = {}",fileLength);
		writer.print("HTTP/1.1 200 OK\r\n");
		if (lastModified != null) {
			writer.print("Last-Modified: "+getFormattedDateTimeForResponse(lastModified)+"\r\n");
		}
		writer.print("Content-Type: "+contentType+"\r\n");
		writer.print("Content-Length: "+fileLength+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.flush();
		CopyStream.copy(new BufferedInputStream(new FileInputStream(file)),out);
		out.flush();
	}

	final public void sendHeaderAndBodyText(OutputStream out,String text,String nameToUse,String contentType) throws IOException {
		sendHeaderAndBodyText(out,text,nameToUse,contentType,null/* lastModifiedDateTime*/);
	}
	
	final public void sendHeaderAndBodyText(OutputStream out,String text,String nameToUse,String contentType,Date lastModified) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		long length = text.length();
		writer.print("HTTP/1.1 200 OK\r\n");
		if (lastModified != null) {
			writer.print("Last-Modified: "+getFormattedDateTimeForResponse(lastModified)+"\r\n");
		}
		writer.print("Content-Type: "+contentType+"\r\n");
		writer.print("Content-Length: "+length+"\r\n");
		writer.print("Content-Disposition: filename="+nameToUse+"\r\n");
		writer.print("\r\n");
		writer.print(text);
		writer.flush();
	}

	static final public void send404NotFound(OutputStream out,String message) {
		try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
			writer.print("HTTP/1.1 404 Not Found - "+message+"\r\n");
			writer.flush();
		}
		catch (IOException e) {
			slf4jlogger.error("",e);
		}
	}
}



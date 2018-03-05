/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.database.DatabaseInformationModel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link PathRequestHandler PathRequestHandler} creates a response to an HTTP request for
 * a named path to a file.</p>
 *
 * @author	dclunie
 */
class PathRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/PathRequestHandler.java,v 1.13 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(PathRequestHandler.class);
	
	private static final String faviconPath = "favicon.ico";	// https://en.wikipedia.org/wiki/Favicon
	private static final String actualIndexPath = "index.html";

	protected PathRequestHandler(String stylesheetPath) {
		super(stylesheetPath);
	}

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			// assert (requestType == null);
			String requestPath = request.getPath();
			slf4jlogger.debug("generateResponseToGetRequest(): Was asked for requestPath {}",requestPath);
			if (requestPath == null) {
				throw new Exception("No such path - path is null - =\""+requestPath+"\"");
			}
			if (requestPath.equals("/") || requestPath.toLowerCase(java.util.Locale.US).equals("/index.html") || requestPath.toLowerCase(java.util.Locale.US).equals("/index.htm")) {
				slf4jlogger.debug("generateResponseToGetRequest(): root path");
				requestPath="/"+actualIndexPath;
			}
			if (requestPath.equals("/"+stylesheetPath) || requestPath.equals("/"+faviconPath) || requestPath.equals("/"+actualIndexPath) || requestPath.startsWith("/dicomviewer")) {
				slf4jlogger.debug("generateResponseToGetRequest(): Was asked for file {}",requestPath);
				String baseNameOfRequestedFile = new File(requestPath).getName();
				if (requestPath.startsWith("/dicomviewer")) {
					baseNameOfRequestedFile = "dicomviewer/" + baseNameOfRequestedFile;
				}
				slf4jlogger.debug("generateResponseToGetRequest(): Trying to find amongst resources {}",baseNameOfRequestedFile);
				String tryRequestedFile = "/"+baseNameOfRequestedFile;
				slf4jlogger.debug("generateResponseToGetRequest(): Looking for {}",tryRequestedFile);
				InputStream fileStream = PathRequestHandler.class.getResourceAsStream(tryRequestedFile);
				if (fileStream == null) {
					tryRequestedFile = "/com/pixelmed/web/"+baseNameOfRequestedFile;
					slf4jlogger.debug("generateResponseToGetRequest(): Failed; so look instead for {}",tryRequestedFile);
					fileStream = PathRequestHandler.class.getResourceAsStream(tryRequestedFile);
					if (fileStream == null) {
						throw new Exception("No such resource as "+requestPath);
					}
				}
				
				boolean isText = false;
				String contentType;
				if (baseNameOfRequestedFile.matches(".*[.][cC][sS][sS]$")) {
					contentType = "text/css";
					isText = true;
				}
				else if (baseNameOfRequestedFile.matches(".*[.][hH][tT][mM][lL]*$")) {
					contentType = "text/html";
					isText = true;
				}
				else if (baseNameOfRequestedFile.matches(".*[.][iI][cC][oO]$")) {
					contentType = "image/x-icon";
				}
				else {
					contentType = "application/octet-stream";
				}
				slf4jlogger.debug("generateResponseToGetRequest(): contentType {}",contentType);

				if (isText) {
					// read the whole thing into a string so that we can know its length for Content-Length; blech :(
					InputStreamReader reader = new InputStreamReader(new BufferedInputStream(fileStream),"UTF-8");
					StringBuffer strbuf =  new StringBuffer();
					char[] buffer = new char[1024];
					int count;
					while ((count=reader.read(buffer,0,1024)) > 0) {
						slf4jlogger.debug("generateResponseToGetRequest(): Read {} chars",count);
						strbuf.append(buffer,0,count);
					}
					sendHeaderAndBodyText(out,strbuf.toString(),baseNameOfRequestedFile,contentType);
				}
				else {
					sendHeaderAndBodyOfStream(out,fileStream,baseNameOfRequestedFile,contentType);
				}
			}
			else {
				throw new Exception("No such path is permitted =\""+requestPath+"\"");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <p>The {@link WebRequest WebRequest} class parses a URL
 * that contains a <code>requestType</code> parameter and additional query parameters.</p>
 *
 * <p>This form is used both by WADO and the IHE RID profile transactions.</p>
 *
 * @author	dclunie
 */
public class WebRequest {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/WebRequest.java,v 1.7 2017/01/24 10:50:52 dclunie Exp $";

	// common URL stuff
	
	protected String scheme;
	protected String userInfo;
	protected String host;
	protected int port;			// we set to -1 if absent
	protected String path;
	protected String requestType;
	protected Map parameters;

	/*
	 * @return	the scheme component of this URI,  or null if the scheme is undefined
	 */
	public String getScheme() { return scheme; }
	
	/*
	 * @return	the decoded user-information component of this URI, or null if the user information is undefined
	 */
	public String getUserInfo() { return userInfo; }
	
	/*
	 * @return	the host component of this URI, or null if the host is undefined
	 */
	public String getHost() { return host; }
	
	/*
	 * @return	the port component of this URI, or -1 if the port is undefined
	 */
	public int getPort() { return port; }
	
	/*
	 * @return	the decoded path component of this URI, or null if the path is undefined
	 */
	public String getPath() { return path; }
	
	/*
	 * @return	the scheme component of this URI,  or null if the scheme is undefined
	 */
	public String getRequestType() { return requestType; }
	
	/*
	 * @return	a Map containing all the paramaters of the query
	 */
	public Map getParameters() { return parameters; }
	
	/*
	 * <p>Parse the query part of a URI.</p>
	 *
	 * @param	query	the part of the URI after the ? symbol, consisting of parameters separated by "&" each a name-value pair separated by "="
	 * @return		a Map of parameter name-value pairs, with the names and values having had any embedded escape characters decoded
	 * @throws		if not a valid request
	 */
	public static Map parseQueryIntoParameters(String query) throws Exception {
		Map parameters = new HashMap();
//System.out.println("raw query = "+query);
		if (query != null) {
			StringTokenizer parametersAndValues = new StringTokenizer(query,"&");					// The "&" character separates parameter=values pairs
			while (parametersAndValues.hasMoreTokens()) {
				String parameterAndValueString = parametersAndValues.nextToken();
				StringTokenizer parameterAndValueTokens = new StringTokenizer(parameterAndValueString,"=");	// The "=" character separate parameter name and value
				if (parameterAndValueTokens.hasMoreTokens()) {
					String parameterName = parameterAndValueTokens.nextToken();
					if (parameterAndValueTokens.hasMoreTokens()) {
						String parameterValue = parameterAndValueTokens.nextToken();
						if (parameterAndValueTokens.hasMoreTokens()) {
							throw new Exception("Unexpected additional text after \""
								+parameterName+"="+parameterValue+"\" in \""+parameterAndValueString+"\"");
						}
						else {
//System.out.println("found "+parameterName+"="+parameterValue);
							parameterName=URLDecoder.decode(parameterName,"UTF-8");
							parameterValue=URLDecoder.decode(parameterValue,"UTF-8");
//System.out.println("decoded "+parameterName+"="+parameterValue);
							parameters.put(parameterName,parameterValue);
						}
					}
					else {
						throw new Exception("Missing parameter value for \""+parameterName+"\"");
					}
				}
				else {
					throw new Exception("Missing parameter name in \""+parameterAndValueString+"\"");
				}
			}
		}
		return parameters;
	}
	
	/*
	 * <p>Create a representation of a query request by parsing a URI.</p>
	 *
	 * <p>Does not throw an exception if no requestType, just does no further parsing of the query. The path will still be extracted and valid</p>
	 *
	 * @param	uriString	the entire URI string (possibly without the scheme and userinfo and host and port, e.g. if from an HTTP get method)
	 * @throws			if not a valid request
	 */
	public WebRequest(String uriString) throws Exception {
		URI uri = new URI(uriString);
		scheme = uri.getScheme();
		userInfo = uri.getUserInfo();
		host = uri.getHost();
		port = uri.getPort();
		path = uri.getPath();

		parameters = parseQueryIntoParameters(uri.getRawQuery());
		
		requestType = (String)(parameters.get("requestType"));
		//if (requestType == null) {
		//	throw new Exception("requestType missing");
		//}
	}
	
	protected WebRequest() {
	}
}
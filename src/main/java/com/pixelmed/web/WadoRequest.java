/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

//import java.net.URI;
//import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.pixelmed.utils.StringUtilities;

/**
 * <p>The {@link WadoRequest WadoRequest} class parses a DICOM PS 3.18 (ISO 17432),
 * WADO URL into its constituent query parameters.</p>
 *
 * @author	dclunie
 */
public class WadoRequest extends WebRequest {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/WadoRequest.java,v 1.12 2017/01/24 10:50:52 dclunie Exp $";

	private String studyUID;		// value must be single UID
	private String seriesUID;		// value must be single UID
	private String objectUID;		// value must be single UID
	
	// optional
	
	private String [][] contentTypes;	// comma separated list of MIME types, potentially associated with relative degree of preference, as specified in IETF RFC2616
	private String [] charsets;		// comma separated list from RFC2616
	private String anonymize;		// value must be yes
	
	// optional and only if image and not application/dicom
	
	private String [] annotations;		// comma separated list of patient, technique
	private int rows;			// one integer string (we set to -1 if absent)
	private int columns;			// one integer string (we set to -1 if absent)
	private double region[];		// four comma separated decimal strings
	private double windowCenter;		// one decimal string, required if windowWidth present (we set to 0 if absent)
	private double windowWidth;		// one decimal string, required if windowCenter present (we set to 0 if absent, which is legitimate value, so use absence of windowWidth as flag)
	private int frameNumber;		// one integer string (we set to -1 if absent)
	private int imageQuality;		// one integer string from 1 to 100 (best) (we set to -1 if absent)
	private String presentationUID;		// value must be single UID
	private String presentationSeriesUID;	// value must be single UID, required if presentationUID present
	
	// optional and only if application/dicom
	private String transferSyntax;		// value must be single UID

	/*
	 * @return	the value of the studyID parameter, or null if absent
	 */
	public String getStudyUID() { return studyUID; }
	
	/*
	 * @return	the value of the seriesUID parameter, or null if absent
	 */
	public String getSeriesUID() { return seriesUID; }
	
	/*
	 * @return	the value of the objectUID parameter, or null if absent
	 */
	public String getObjectUID() { return objectUID; }
	
	/*
	 * <p>Get the values of the contentType parameter</p>
	 *
	 * <p>if more than one, the order implies decreasing order of preference.</p>
	 *
	 * @return	the values of the contentType parameter, as an array of arrays of String,
	 *		one String array for each content type, each containing the content type
	 *		and parameters, or null if absent
	 */
	public String[][] getContentType() { return contentTypes; }
	
	/*
	 * <p>Is the contentType parameter a single application/dicom value ?</p>
	 *
	 * @return	true if there is one contentType parameter value that is application/dicom
	 */
	public boolean isContentTypeDicom() {
		return contentTypes != null
		    && contentTypes.length == 1
		    && contentTypes[0] != null
		    && contentTypes[0].length >= 1
		    && contentTypes[0][0] != null
		    && contentTypes[0][0].toLowerCase(java.util.Locale.US).equals("application/dicom");
	}
	
	/*
	 * @return	the values of the charset parameter, as an array of String, or null if absent
	 */
	public String[] getCharset() { return charsets; }
	
	/*
	 * @return	the value of the anonymize parameter, which will always be yes, or null if absent
	 */
	public String getAnonymize() { return anonymize; }
	
	/*
	 * @return	the values of the annotation parameter, as an array of String, containing "patient" and/or "technique", or null if absent
	 */
	public String [] getAnnotation() { return annotations; }
	
	/*
	 * @return	the value of the rows parameter, or -1 if absent
	 */
	public int getRows() { return rows; }
	
	/*
	 * @return	the value of the columns parameter, or -1 if absent
	 */
	public int getColumns() { return columns; }
	
	/*
	 * @return	the values of the region parameter, as an array of four double values, or null if absent
	 */
	public double[] getRegion() { return region; }
	
	/*
	 * @return	the value of the windowCenter parameter, or 0 if absent
	 */
	public double getWindowCenter() { return windowCenter; }
	
	/*
	 * @return	the value of the windowWidth parameter, or 0 if absent
	 */
	public double getWindowWidth() { return windowWidth; }
	
	/*
	 * @return	the value of the frameNumber parameter, or -1 if absent
	 */
	public int getFrameNumber() { return frameNumber; }
	
	/*
	 * @return	the value of the imageQuality parameter from 1 to 100 (best), or -1 if absent
	 */
	public int getImageQuality() { return imageQuality; }
	
	/*
	 * @return	the value of the presentationUID parameter, or null if absent
	 */
	public String getPresentationUID() { return presentationUID; }
	
	/*
	 * @return	the value of the presentationSeriesUID parameter, or null if absent
	 */
	public String getPresentationSeriesUID() { return presentationSeriesUID; }
	
	/*
	 * @return	the value of the transferSyntax parameter (a UID), or null if absent
	 */
	public String getTransferSyntax() { return transferSyntax; }

	/*
	 * <p>Validate that a string is a valid DICOM UID.</p>
	 *
	 * @param	uid	a DICOM UID of up to 64 characters in length
	 * @return		true if valid
	 */
	public static boolean validateUID(String uid) {
		return uid != null && uid.length() > 0 && uid.length() <= 64 && uid.matches("[0-9.]*");
	}

	/*
	 * <p>Split a string into substrings separated by the specified delimiter.</p>
	 *
	 * @param	string		to split
	 * @param	delimiter	the delimiter between substrings
	 * @return			an array of substrings
	 */
	public static String[] getSeparatedValues(String string,String delimiter) {
//System.out.println("getSeparatedValues() "+string+" by "+delimiter);
		String[] values = null;
		StringTokenizer st = new StringTokenizer(string,delimiter);
		int count = st.countTokens();
		if (count > 0) {
			values = new String[count];
			for (int i=0; i<count; ++i) {
				// assert hasMoreElements();
				values[i]=st.nextToken();
			}
		}
//System.out.println("getSeparatedValues() "+values);
		return values;
	}

	/*
	 * <p>Split a string into substrings separated by commas.</p>
	 *
	 * @param	string		to split
	 * @return			an array of substrings
	 */
	public static String[] getCommaSeparatedValues(String string) {
		return getSeparatedValues(string,",");
	}
	
	/*
	 * <p>Split a string into substrings separated by semicolons.</p>
	 *
	 * @param	string		to split
	 * @return			an array of substrings
	 */
	public static String[] getSemicolonSeparatedValues(String string) {
		return getSeparatedValues(string,";");
	}
	
	/*
	 * <p>Split a string first into substrings separated by commas, then split each of those into substrings separated by semicolons.</p>
	 *
	 * <p>Used to split a list of parameters and sub-parameters.</p>
	 *
	 * @param	string		to split
	 * @return			an array of arrays substrings
	 */
	public static String[][] getCommaThenSemicolonSeparatedValues(String string) {
		String[][] values = null;
		String[] commaSeparatedValues = getCommaSeparatedValues(string);
		if (commaSeparatedValues != null) {
			int count = commaSeparatedValues.length;
			if (count > 0) {
				values = new String[count][];
				for (int i=0; i<count; ++i) {
					String testValue = commaSeparatedValues[i];
					values[i] = testValue == null ? null : getSemicolonSeparatedValues(testValue);
				}
			}
		}
		return values;
	}

	/*
	 * <p>Dump an array as a human-readable string.</p>
	 *
	 * @param	doubleArray	to dump
	 * @return			a string representation
	 */
	public static String toString(double[] doubleArray) {
		return StringUtilities.toString(doubleArray);
	}

	/*
	 * <p>Dump an array as a human-readable string.</p>
	 *
	 * @param	stringArray	to dump
	 * @return			a string representation
	 */
	public static String toString(String[] stringArray) {
		return StringUtilities.toString(stringArray);
	}

	/*
	 * <p>Dump an array of arrays as a human-readable string.</p>
	 *
	 * @param	stringArrays	to dump
	 * @return			a string representation
	 */
	public static String toString(String[][] stringArrays) {
		return StringUtilities.toString(stringArrays);
	}
	
	/*
	 * <p>Get the named parameter that is an integer from a map of string values.</p>
	 *
	 * @param	parameters	the map of parameter name-value pairs
	 * @param	key		the name of the parameter to retrieve
	 * @return			the integer value, or -1 if not present in the map
	 * @throws			if not an integer string
	 */
	public static int getSingleIntegerValueFromParameters(Map parameters,String key) throws Exception {
		String s = (String)(parameters.get(key));
		if (s != null) {
			try {
				return Integer.parseInt(s);
			}
			catch (NumberFormatException e) {
				throw new Exception(key+" must be an integer string \""+s+"\"");
			}
		}
		return -1;
	}
	
	/*
	 * <p>Get the named parameter that is a double from a map of string values.</p>
	 *
	 * @param	parameters	the map of parameter name-value pairs
	 * @param	key		the name of the parameter to retrieve
	 * @return			the double value, or 0 if not present in the map
	 * @throws			if not a decimal string
	 */
	public static double getSingleDoubleValueFromParameters(Map parameters,String key) throws Exception {
		String s = (String)(parameters.get(key));
		if (s != null) {
			try {
				return Double.parseDouble(s);
			}
			catch (NumberFormatException e) {
				throw new Exception(key+" must be a decimal string \""+s+"\"");
			}
		}
		return 0;
	}

	/*
	 * <p>Create a representation of a WADO request from an existing WebRequest of requestType=WADO.</p>
	 *
	 * @param	request		an existing WebRequest with parameters
	 * @throws			if not a valid request
	 */
	public WadoRequest(WebRequest request) throws Exception {
		scheme = request.getScheme();
		userInfo = request.getUserInfo();
		host = request.getHost();
		port = request.getPort();
		path = request.getPath();
		requestType = request.getRequestType();
		parameters = request.getParameters();
		parseWadoParameters();
	}
	
	/*
	 * <p>Create a representation of a WADO request by parsing a WADO URI.</p>
	 *
	 * @param	uriString	the entire WADO URI string
	 * @throws			if not a valid request
	 */
	public WadoRequest(String uriString) throws Exception {
		super(uriString);	// sets requestType
		parseWadoParameters();
	}
	
	private void parseWadoParameters() throws Exception {
		// Mandatory
		
		if (requestType == null || !requestType.equals("WADO")) {
			throw new Exception("requestType missing or not WADO \""+requestType+"\"");
		}
		studyUID = (String)(parameters.get("studyUID"));
		if (!validateUID(studyUID)) {
			throw new Exception("studyUID missing or not valid \""+studyUID+"\"");
		}
		
		seriesUID = (String)(parameters.get("seriesUID"));
		if (!validateUID(seriesUID)) {
			throw new Exception("seriesUID missing or not valid \""+seriesUID+"\"");
		}
		
		objectUID = (String)(parameters.get("objectUID"));
		if (!validateUID(objectUID)) {
			throw new Exception("objectUID missing or not valid \""+objectUID+"\"");
		}
		
		// Optional
		
		String allContentTypes = (String)(parameters.get("contentType"));
		if (allContentTypes != null) {
			contentTypes = getCommaThenSemicolonSeparatedValues(allContentTypes);
		}

		String allCharsets = (String)(parameters.get("charset"));
		if (allCharsets != null) {
			charsets = getCommaSeparatedValues(allCharsets);
		}

		anonymize = (String)(parameters.get("anonymize"));
		if (anonymize != null && !anonymize.equals("yes")) {
			throw new Exception("anonymize must be absent or yes \""+anonymize+"\"");
		}

		String allAnnotations = (String)(parameters.get("annotation"));
		if (allAnnotations != null) {
			annotations = getCommaSeparatedValues(allAnnotations);
		}
		
		rows = getSingleIntegerValueFromParameters(parameters,"rows");
		columns = getSingleIntegerValueFromParameters(parameters,"columns");

		String sRegion = (String)(parameters.get("region"));
		if (sRegion != null) {
			String[] regionValues = getCommaSeparatedValues(sRegion);
			if (regionValues == null || regionValues.length != 4) {
				throw new Exception("region must be four comma delimited decimal strings \""+sRegion+"\"");
			}
			else {
				region = new double[4];
				for (int i=0; i<4; ++i) {
					try {
						region[i] =  Double.parseDouble(regionValues[i]);
					}
					catch (NumberFormatException e) {
						throw new Exception("region value must be a decimal string \""+regionValues[i]+"\"");
					}
				}
			}
		}
		
		windowCenter = getSingleDoubleValueFromParameters(parameters,"windowCenter");
		windowWidth = getSingleDoubleValueFromParameters(parameters,"windowWidth");
		if (parameters.get("windowCenter") == null && parameters.get("windowWidth") != null) {
			throw new Exception("windowCenter missing but require since windowWidth is present");
		}
		else if (parameters.get("windowCenter") != null && parameters.get("windowWidth") == null) {
			throw new Exception("windowWidth missing but require since windowCenter is present");
		}
		
		frameNumber = getSingleIntegerValueFromParameters(parameters,"frameNumber");
		
		imageQuality = getSingleIntegerValueFromParameters(parameters,"imageQuality");
		if (imageQuality != -1 && (imageQuality < 1 || imageQuality > 100)) {
			throw new Exception("imageQuality must be between 1 and 100 \""+imageQuality+"\"");
		}

		presentationUID = (String)(parameters.get("presentationUID"));
		if (presentationUID != null && !validateUID(presentationUID)) {
			throw new Exception("presentationUID not valid \""+presentationUID+"\"");
		}

		presentationSeriesUID = (String)(parameters.get("presentationSeriesUID"));
		if (presentationSeriesUID != null && !validateUID(presentationSeriesUID)) {
			throw new Exception("presentationSeriesUID not valid \""+presentationSeriesUID+"\"");
		}
		if (presentationUID != null && presentationSeriesUID == null) {
			throw new Exception("presentationSeriesUID missing but require since presentationUID is present");
		}
		else if (presentationUID == null && presentationSeriesUID != null) {
			throw new Exception("presentationUID missing but require since presentationSeriesUID is present");
		}

		transferSyntax = (String)(parameters.get("transferSyntax"));
		if (transferSyntax != null && !validateUID(transferSyntax)) {
			throw new Exception("transferSyntax not valid \""+transferSyntax+"\"");
		}
		if (!isContentTypeDicom() && transferSyntax != null) {
			throw new Exception("transferSyntax is present but contentType is not application/dicom (only)");
		}

	}
	
	/*
	 * <p>Dump as a human-readable string.</p>
	 *
	 * @return	a string representation
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("scheme = "); strbuf.append(scheme); strbuf.append("\n");
		strbuf.append("userInfo = "); strbuf.append(userInfo); strbuf.append("\n");
		strbuf.append("host = "); strbuf.append(host); strbuf.append("\n");
		strbuf.append("port = "); strbuf.append(port); strbuf.append("\n");
		strbuf.append("path = "); strbuf.append(path); strbuf.append("\n");
		strbuf.append("studyUID = "); strbuf.append(studyUID); strbuf.append("\n");
		strbuf.append("seriesUID = "); strbuf.append(seriesUID); strbuf.append("\n");
		strbuf.append("objectUID = "); strbuf.append(objectUID); strbuf.append("\n");
		strbuf.append("contentTypes = "); strbuf.append(toString(contentTypes)); strbuf.append("\n");
		strbuf.append("charsets = "); strbuf.append(toString(charsets)); strbuf.append("\n");
		strbuf.append("anonymize = "); strbuf.append(anonymize); strbuf.append("\n");
		strbuf.append("annotations = "); strbuf.append(toString(annotations)); strbuf.append("\n");
		strbuf.append("rows = "); strbuf.append(rows); strbuf.append("\n");
		strbuf.append("columns = "); strbuf.append(columns); strbuf.append("\n");
		strbuf.append("region = "); strbuf.append(toString(region)); strbuf.append("\n");
		strbuf.append("windowCenter = "); strbuf.append(windowCenter); strbuf.append("\n");
		strbuf.append("windowWidth = "); strbuf.append(windowWidth); strbuf.append("\n");
		strbuf.append("frameNumber = "); strbuf.append(frameNumber); strbuf.append("\n");
		strbuf.append("imageQuality = "); strbuf.append(imageQuality); strbuf.append("\n");
		strbuf.append("presentationUID = "); strbuf.append(presentationUID); strbuf.append("\n");
		strbuf.append("presentationSeriesUID = "); strbuf.append(presentationSeriesUID); strbuf.append("\n");
		strbuf.append("transferSyntax = "); strbuf.append(transferSyntax); strbuf.append("\n");
		return strbuf.toString();
	}

	/*
	 * <pTest parsing the examples in DICOM PS 3.18 Annex B.</p>
	 *
	 */
	public static void main(String arg[]) {
		String testB1 = "http://www.hospital-stmarco/radiology/wado.php?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2";
	
		String testB2 = "http://server234/script678.asp?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&charset=UTF-8";
	
		//String testB3 = "https://aspradio/imageaccess.js?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=image%2Fjp2;level=1,image%2Fjpeg;q=0.5&annotation=patient,technique&columns=400&rows=300&region=0.3,0.4,0.5,0.5&windowCenter=-1000&windowWidth=2500";
		String testB3 = "https://aspradio/imageaccess.js?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=image%2Fjp2%3Blevel%3D1%2Cimage%2Fjpeg%3Bq%3D0.5&annotation=patient%2Ctechnique&columns=400&rows=300&region=0.3%2C0.4%2C0.5%2C0.5&windowCenter=-1000&windowWidth=2500";

		String testB4 = "http://www.medical-webservice.st/RetrieveDocument?requestType=WADO&studyUID=1.2.250.1.59.40211.12345678.678910&seriesUID=1.2.250.1.59.40211.789001276.14556172.67789&objectUID=1.2.250.1.59.40211.2678810.87991027.899772.2&contentType=application%2Fdicom&anonymize=yes&transferSyntax=1.2.840.10008.1.2.4.50";

		try {
			System.out.println("B1: \""+testB1+"\"\n"+new WadoRequest(testB1).toString());
			System.out.println("B2: \""+testB2+"\"\n"+new WadoRequest(testB2).toString());
			System.out.println("B3: \""+testB3+"\"\n"+new WadoRequest(testB3).toString());
			System.out.println("B4: \""+testB4+"\"\n"+new WadoRequest(testB4).toString());
		}
		catch (Exception e) {
			System.err.println("B3: threw exception");
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
	}
}
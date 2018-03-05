/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.BufferedReader;
import java.io.InputStream; 
import java.io.InputStreamReader; 
import java.io.IOException; 

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Various pre-defined constants for identifying this software.</p>
 *
 * @author	dclunie
 */
public class VersionAndConstants {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/VersionAndConstants.java,v 1.17 2017/01/24 10:50:39 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AttributeList.class);
	
	public static final String softwareVersion = "001";	// must be [A-Z0-9_] and <= 4 chars else screws up ImplementationVersionName

	/***/
	public static final String implementationVersionName = "PIXELMEDJAVA"+softwareVersion;

	public static final String uidRoot = "1.3.6.1.4.1.5962";
	/***/
	public static final String uidQualifierForThisToolkit = "99";
	/***/
	public static final String uidQualifierForUIDGenerator = "1";
	/***/
	public static final String uidQualifierForImplementationClassUID = "2";
	/***/
	public static final String uidQualifierForInstanceCreatorUID = "3";
	/***/
	public static final String implementationClassUID = uidRoot+"."+uidQualifierForThisToolkit+"."+uidQualifierForImplementationClassUID;
	/***/
	public static final String instanceCreatorUID = uidRoot+"."+uidQualifierForThisToolkit+"."+uidQualifierForInstanceCreatorUID;
	/***/
	public static final String uidQualifierFor99PMPFamilyOfCodingSchemes = "98";	/* see "com.pixelmed.validate.PixelMedContextGroupsSource.xml" */
	/***/
	public static final String uidQualifierFor99PMPCodingScheme = "1";
	public static final String uidQualifierFor99IPCMRCodingScheme = "2";
	/***/
	public static final String codingSchemeUIDFor99PMP = uidRoot+"."+uidQualifierFor99PMPFamilyOfCodingSchemes+"."+uidQualifierFor99PMPCodingScheme;
	public static final String codingSchemeUIDFor99IPCMR = uidRoot+"."+uidQualifierFor99PMPFamilyOfCodingSchemes+"."+uidQualifierFor99IPCMRCodingScheme;
	/***/
	public static final String releaseString = "General Release";
	
	/**
	 * <p>Get the date the package was built.</p>
	 *
	 * @return	 the build date
	 */
	public static String getBuildDate() {
		String buildDate = "";
		try {
			InputStream i = VersionAndConstants.class.getResourceAsStream("/BUILDDATE");	// absolute path does not always work (?)
			if (i == null) {
//System.err.println("VersionAndConstants.getBuildDate(): no absolute path ... try package relative ...");
				i = VersionAndConstants.class.getResourceAsStream("../../../BUILDDATE");	// assume package relative
			}
			buildDate = i == null ? "NOBUILDDATE" : (new BufferedReader(new InputStreamReader(i))).readLine();
//System.err.println("VersionAndConstants.getBuildDate(): = "+buildDate);
		}
		catch (IOException e) {
			slf4jlogger.error("", e);
		}
		return buildDate;
	}
	
}

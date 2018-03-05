/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

/**
 * <p>This class provides a description of the parameters of a remote FTP host.</p>
 *
 * @author	dclunie
 */
public class FTPSecurityType {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPSecurityType.java,v 1.5 2017/01/24 10:50:43 dclunie Exp $";
	
	private String description;
	private FTPSecurityType() {}
	private FTPSecurityType(String description) { this.description = description; }
		
	public static final FTPSecurityType NONE = new FTPSecurityType("NONE");
	public static final FTPSecurityType TLS = new FTPSecurityType("TLS");
	
	private static final String[] listOfTypes = { NONE.toString(), TLS.toString() };
	
	public static String[] getListOfTypesAsString() { return listOfTypes; }
	
	public String toString() { return description; }
	
	public static FTPSecurityType selectFromDescription(String description) {
		FTPSecurityType found = NONE;
		if (description != null) {
			description = description.trim().toUpperCase(java.util.Locale.US);
			if (description.equals(NONE.toString().toUpperCase(java.util.Locale.US))) {
				found = NONE;
			}
			else if (description.equals(TLS.toString().toUpperCase(java.util.Locale.US))) {
				found = TLS;
			}
		}
		return found;
	}

	public boolean equals(Object obj) {
		if (obj instanceof FTPSecurityType) {
			FTPSecurityType comp = (FTPSecurityType)obj;
			return (description == null && comp.description == null) || description.equals(comp.description);
		}
		else {
			return super.equals(obj);
		}
	}
}


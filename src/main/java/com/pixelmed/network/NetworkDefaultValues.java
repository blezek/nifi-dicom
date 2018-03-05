/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.net.InetAddress;

/**
 * <p>This class defines a number of useful defaults and constants and utilities to generate defaults.</p>
 *
 * @author	dclunie
 */
public class NetworkDefaultValues {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/NetworkDefaultValues.java,v 1.8 2017/01/24 10:50:45 dclunie Exp $";
	
	public static final int StandardDicomPrivilegedPortNumber = 104;
	public static final int StandardDicomReservedPortNumber = 11112;
	public static final int VendorGECommonDicomPortNumber = 4006;
	public static final int VendorSiemensCommonDicomPortNumber = 3004;
	public static final int VendorPhilipsCommonDicomPortNumber = 3010;
	public static final int VendorOsirixCommonDicomPortNumber = 4096;
	public static final int VendorConquestCommonDicomPortNumber = 5678;
	public static final int VendorTianniCommonDicomPortNumber = 2350;		// dcm4jboss; docs say 2250; default config file supplied is actually 2350
	public static final int VendorKPACSCommonDicomPortNumber = 111;
	
	public static int[] commonPortNumbers = {
		StandardDicomPrivilegedPortNumber,
		StandardDicomReservedPortNumber,
		VendorGECommonDicomPortNumber,
		VendorSiemensCommonDicomPortNumber,
		VendorPhilipsCommonDicomPortNumber,
		VendorConquestCommonDicomPortNumber,
		VendorOsirixCommonDicomPortNumber,
		VendorTianniCommonDicomPortNumber,
		VendorKPACSCommonDicomPortNumber
	};

	public static final int DefaultWADOPort = 7091;

	private static final String DefaultApplicationEntityTitlePrefix = "PIXELMED";
	
	private static final String DefaultDNSServiceInstanceNamePrefix = "PixelMedWADO";

	private static final String DefaultPrimaryDeviceType = "WSD";
	
	public static final String getDefaultPrimaryDeviceType() {
		return DefaultPrimaryDeviceType;
	}
	
	public static final String getDefaultApplicationEntityTitle(int port) {
		String name = getNameOrDefaultPlusPort(getUnqualifiedLocalHostName(),DefaultApplicationEntityTitlePrefix,port);
		if (name != null) {
			if (name.length() > 16) {
				name = name.substring(0,16);
			}
			name = name.toUpperCase(java.util.Locale.US);	// AEs by habit are often upper case, whereas host names typically are not
			// could check for funky non-AE VR characters, but probably unnecessary :(
		}
		return name;
	}
	
	public static final String getDefaultDNSServiceInstanceName(int port) {
		String name = getNameOrDefaultPlusPort(getUnqualifiedLocalHostName(),DefaultDNSServiceInstanceNamePrefix,port);
		return name;
	}
	
	private static final String getNameOrDefaultPlusPort(String name,String defaultName,int port) {
		if (name == null) {
			name = defaultName;
		}
		if (port >= 0) {
			name = name + "_" + Integer.toString(port);
		}
		return name;
	}
	
	public static final String getUnqualifiedLocalHostName() {
		String hostName = null;
		try {
			InetAddress address = InetAddress.getLocalHost();
			if (address != null) {
				hostName = address.getHostName();
				if (hostName != null) {
					int dotSpot = hostName.indexOf('.');
					if (dotSpot > -1) {
						hostName = hostName.substring(0,dotSpot);
					}
					if (hostName.length() == 0) {
						hostName = null;
					}
				}
			}
		}
		catch (Exception e) {
		}
		return hostName;
	}
}


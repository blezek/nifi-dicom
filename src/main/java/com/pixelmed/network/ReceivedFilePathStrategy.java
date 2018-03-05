/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

/**
 * <p>This class defines an enumeration to describe alternative strategies for how to organize the folders and files for received images.</p>
 *
 * @deprecated 
 * <p>Provided for legacy support only; use {@link com.pixelmed.dicom.StoredFilePathStrategy StoredFilePathStrategy} instead.</p>
 *
  * @author	dclunie
 */
public final class ReceivedFilePathStrategy extends com.pixelmed.dicom.StoredFilePathStrategy {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ReceivedFilePathStrategy.java,v 1.9 2017/01/24 10:50:45 dclunie Exp $";

	private ReceivedFilePathStrategy() {}
}
		

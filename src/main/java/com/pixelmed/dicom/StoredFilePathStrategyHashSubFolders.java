/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.File;

/**
 * <p>Store files in a hierarchy of folders using successive decimal digits of the hashcode
 * of the SOP Instance UID as the folder name and the SOP Instance UID as the filename within the most deeply nested folder.</p>
 *
 * <p>This is the currently preferred strategy for a server that is expected to store a large number of files.</p>
 *
 * @author	dclunie, jimirrer
 */
public final class StoredFilePathStrategyHashSubFolders extends StoredFilePathStrategy {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StoredFilePathStrategyHashSubFolders.java,v 1.5 2017/01/24 10:50:39 dclunie Exp $";

	public StoredFilePathStrategyHashSubFolders() {}

	public String makeStoredFilePath(String sopInstanceUID) {
		long hashCode = sopInstanceUID.hashCode() & 0x0000ffffl;   // do not sign extend
		String prefix = null;
		StringBuffer buf = new StringBuffer();
		long maxValue = 0x0000ffffl;
		while (maxValue > 0) {                  // not hashCode ... don't want to crowd all those whose high digits happen to start with 0 in the same folder
			if (prefix != null) {
				buf.append(prefix);
			}
			int digit = (int)(hashCode%10);
			buf.append(Integer.toString(digit));
			hashCode = hashCode / 10;
			maxValue = maxValue / 10;
			prefix = File.separator;
		}
		buf.append(File.separator);
		buf.append(sopInstanceUID);                        // append the entire uid as the file name (since hash codes are not unique)
		return buf.toString();
	}

	public String toString() {
		return "BYSOPINSTANCEUIDHASHSUBFOLDERS";
	}

	/**
	 * <p>Perform self test.  If arguments are given, then use then as test UIDs.  If no arguments, then use internal test UIDs.</p>
	 */
	public static void main(String arg[]) {
		BYSOPINSTANCEUIDHASHSUBFOLDERS.test(arg);
	}
}


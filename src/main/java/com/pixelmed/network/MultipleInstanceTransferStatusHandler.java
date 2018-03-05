/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

public abstract class MultipleInstanceTransferStatusHandler {
	public abstract void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID);
}


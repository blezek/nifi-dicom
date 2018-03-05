/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

public abstract class MultipleInstanceTransferStatusHandlerWithFileName extends MultipleInstanceTransferStatusHandler {
	public abstract void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID,String fileName,boolean success);

	public void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID) {
		updateStatus(nRemaining,nCompleted,nFailed,nWarning,sopInstanceUID,null,true/*actually don't know, but not tri-state :(*/);
	}
}


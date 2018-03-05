/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.io.File;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class FileReaper extends Thread {

	private static Thread thread;
	
	private static final Set<String> paths = Collections.synchronizedSet(new HashSet<String>());
	
	private static final void deleteFiles() {
		synchronized (paths) {
//System.err.println("FileReaper.deleteFiles(): paths.size() = "+paths.size());
			Iterator<String> i = paths.iterator(); // Must be in the synchronized block per JavaDoc
			while (i.hasNext()) {
				String path = i.next();
				File f = new File(path);
				if (f.exists()) {
					if (f.delete()) {
//System.err.println("FileReaper.deleteFiles(): successfully deleted "+path);
						i.remove();
					}
					else {
//System.err.println("FileReaper.deleteFiles(): failed to delete "+path);
					}
				}
				else {
//System.err.println("FileReaper.deleteFiles(): file no longer exists "+path);
				}
			}
		}
	}
	
	public void run() {
		boolean stop = false;
		while (!stop) {
			try {
				deleteFiles();
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				stop = true;
			}
		}
	}

	public static final synchronized void addFileToDelete(String path) {
		if (thread == null) {
//System.err.println("FileReaper.addFileToDelete(): lazy start up of thread");
			thread = new FileReaper();
			thread.setDaemon(true);
			thread.start();
		}
		synchronized (paths) {
			paths.add(path);
		}
	}

	protected void finalize() throws Throwable {	// not sure if this will ever actually get called, but just in case there is anything left over ...
//System.err.println("FileReaper.finalize()");
		deleteFiles();
		super.finalize();
	}

}

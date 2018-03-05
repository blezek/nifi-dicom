/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.utils.ThreadUtilities;

import java.awt.Component;
import java.awt.HeadlessException;

import java.io.File;

import java.util.Enumeration;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class SafeFileChooser {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SafeFileChooser.java,v 1.8 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SafeFileChooser.class);

	protected static String resourceBundleName  = "com.pixelmed.display.SafeFileChooser";
	
	protected static ResourceBundle resourceBundle;
	
	protected static void localizeJFileChooser() {
		if (resourceBundle == null) {
			try {
				resourceBundle = ResourceBundle.getBundle(resourceBundleName);
				for (Enumeration<String> e = resourceBundle.getKeys(); e.hasMoreElements();) {
					String key = e.nextElement();
					if (key.startsWith("FileChooser.")) {
						String value = resourceBundle.getString(key);
						slf4jlogger.debug("localizeJFileChooser(): UIManager.put(\"{}\" , \"{}\")",key,value);
						UIManager.put(key,value);
					}
				}
			}
			catch (Exception e) {
				// ignore java.util.MissingResourceException: Can't find bundle for base name com.pixelmed.display.SafeFileChooser, locale en_US
				slf4jlogger.warn("Missing resource bundle for localization {}",e.toString());
			}
		}
	}
	
	protected JFileChooser chooser;
	
	public SafeFileChooser() {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		localizeJFileChooser();
		chooser = new JFileChooser();
	}
	
	public SafeFileChooser(String currentDirectoryPath) {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		localizeJFileChooser();
		chooser = new JFileChooser(currentDirectoryPath);
	}
	
	public void setFileSelectionMode(int mode) throws IllegalArgumentException {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		chooser.setFileSelectionMode(mode);
	}
	
	public int showOpenDialog(Component parent) throws HeadlessException {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		return chooser.showOpenDialog(parent);
	}
	
	public File getCurrentDirectory() {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		return chooser.getCurrentDirectory();
	}
	
	public File getSelectedFile() {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		return chooser.getSelectedFile();
	}
	
	public int showSaveDialog(Component parent) throws HeadlessException {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		return chooser.showSaveDialog(parent);
	}
	
	public void setDialogTitle(String dialogTitle) throws HeadlessException {
		ThreadUtilities.checkIsEventDispatchThreadElseException();
		chooser.setDialogTitle(dialogTitle);
	}
	
	public static class SafeFileChooserThread implements Runnable {
		private int fileSelectionMode;
		private String initialDirectoryPath;
		private String dialogTitle;
		private String selectedFileName;
		private String currentDirectoryPath;
		
		public SafeFileChooserThread() {
			this.fileSelectionMode = JFileChooser.FILES_ONLY;
		}
		
		public SafeFileChooserThread(int fileSelectionMode) {
			this.fileSelectionMode = fileSelectionMode;
		}
		
		public SafeFileChooserThread(String initialDirectoryPath) {
			this.fileSelectionMode = JFileChooser.FILES_ONLY;
			this.initialDirectoryPath = initialDirectoryPath;
		}
		
		public SafeFileChooserThread(int fileSelectionMode,String initialDirectoryPath) {
			this.fileSelectionMode = fileSelectionMode;
			this.initialDirectoryPath = initialDirectoryPath;
		}
		
		public SafeFileChooserThread(String initialDirectoryPath,String dialogTitle) {
			this.fileSelectionMode = JFileChooser.FILES_ONLY;
			this.initialDirectoryPath = initialDirectoryPath;
			this.dialogTitle = dialogTitle;
		}
		
		public SafeFileChooserThread(int fileSelectionMode,String initialDirectoryPath,String dialogTitle) {
			this.fileSelectionMode = fileSelectionMode;
			this.initialDirectoryPath = initialDirectoryPath;
			this.dialogTitle = dialogTitle;
		}
		
		public void run() {
			SafeFileChooser chooser = initialDirectoryPath == null ? new SafeFileChooser() : new SafeFileChooser(initialDirectoryPath);
			if (dialogTitle != null) {
				chooser.setDialogTitle(dialogTitle);
			}
			chooser.setFileSelectionMode(fileSelectionMode);
			selectedFileName = null;
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				selectedFileName=chooser.getSelectedFile().getAbsolutePath();
				currentDirectoryPath=chooser.getCurrentDirectory().getAbsolutePath();
			}
		}
		
		public String getSelectedFileName() {
			return selectedFileName;
		}
		
		public String getCurrentDirectoryPath() {
			return currentDirectoryPath;
		}
	}
	
}
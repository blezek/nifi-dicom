/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import javax.swing.JProgressBar;

/**
 * <p>A class that implements {@link java.lang.Runnable Runnable} so that it can be invoked by {@link java.awt.EventQueue#invokeLater(Runnable) EventQueue.invokeLater()}.</p>
 *
 * <p>This is needed, for example, to call from a worker thread, since the progress bar methods used MUST be invoked on the AWT Event Dispatch Thread.</p>
 *
 * <p>So, for example, instead of directly accessing the {@link javax.swing.JProgressBar JProgressBar} methods:</p>
 * <pre>
 * 	progressBar.setMaximum(maximum);
 * 	progressBar.setValue(value);
 * 	progressBar.repaint();
 * </pre>
 * <p>do the following instead:</p>
 * <pre>
 *  progressBarUpdater = new SafeProgressBarUpdaterThread(progressBar);
 *  ...
 * 	progressBarUpdater.setMaximum(maximum);
 * 	progressBarUpdater.setValue(value);
 * 	java.awt.EventQueue.invokeLater(progressBarUpdater);
 * </pre>
 */

public class SafeProgressBarUpdaterThread implements Runnable {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SafeProgressBarUpdaterThread.java,v 1.5 2017/01/24 10:50:40 dclunie Exp $";

	protected JProgressBar progressBar;
	protected int value;
	protected int maximum;
	protected boolean stringPainted;
	
	public SafeProgressBarUpdaterThread(JProgressBar progressBar) {
		this.progressBar = progressBar;
		this.value = 0;
		this.maximum = 0;
	}
	
	public void run() {
		progressBar.setValue(value);
		progressBar.setMaximum(maximum);			// undesirable to keep setting the maximum this way, may cause flicker but saves having to have a separate class to do it since only one run method
		progressBar.setStringPainted(stringPainted);
		progressBar.repaint();
	}
	
	public void setValue(int value) {
		this.value = value;
	}
	
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
	
	public void setStringPainted(boolean b) {
		stringPainted = b;
	}
	
	public JProgressBar getProgressBar() { return progressBar; }
	
	// convenience methods ...
	
	public void startProgressBar(int maximum) {
		{
			if (java.awt.EventQueue.isDispatchThread()) {
				progressBar.setValue(0);
				progressBar.setMaximum(maximum);
				progressBar.setStringPainted(true);
				progressBar.repaint();
			}
			else {
				setValue(0);
				setMaximum(maximum);
				setStringPainted(true);
				java.awt.EventQueue.invokeLater(this);
			}
		}
	}
	
	public void startProgressBar() {
		startProgressBar(100);	// assume standard default of 100 if unknown, expecting that it will be updated when known
	}
	
	public void updateProgressBar(int value) {
		{
			if (java.awt.EventQueue.isDispatchThread()) {
				progressBar.setValue(value);
				progressBar.setStringPainted(true);
				progressBar.repaint();
			}
			else {
				setValue(value);
				setStringPainted(true);
				java.awt.EventQueue.invokeLater(this);
			}
		}
	}
	
	public void updateProgressBar(int value,int maximum) {
		{
			if (java.awt.EventQueue.isDispatchThread()) {
				progressBar.setValue(value);
				progressBar.setMaximum(maximum);
				progressBar.setStringPainted(true);
				progressBar.repaint();
			}
			else {
				setValue(value);
				setMaximum(maximum);
				setStringPainted(true);
				java.awt.EventQueue.invokeLater(this);
			}
		}
	}
	
	public void endProgressBar() {
		{
			if (java.awt.EventQueue.isDispatchThread()) {
				progressBar.setValue(0);
				progressBar.setMaximum(100);			// clears the progress bar
				progressBar.setStringPainted(false);	// do not want to display 0%
				progressBar.repaint();
			}
			else {
				setValue(0);
				setMaximum(100);				// clears the progress bar
				setStringPainted(false);		// do not want to display 0%
				java.awt.EventQueue.invokeLater(this);
			}
		}
	}

	// static convenience methods ...

	public static void startProgressBar(SafeProgressBarUpdaterThread progressBarUpdater,int maximum) {
		if (progressBarUpdater != null) {
			progressBarUpdater.startProgressBar(maximum);
		}
	}
	
	public static void startProgressBar(SafeProgressBarUpdaterThread progressBarUpdater) {
		if (progressBarUpdater != null) {
			progressBarUpdater.startProgressBar();
		}
	}
	
	public static void updateProgressBar(SafeProgressBarUpdaterThread progressBarUpdater,int value) {
		if (progressBarUpdater != null) {
			progressBarUpdater.updateProgressBar(value);
		}
	}
	
	public static void updateProgressBar(SafeProgressBarUpdaterThread progressBarUpdater,int value,int maximum) {
		if (progressBarUpdater != null) {
			progressBarUpdater.updateProgressBar(value,maximum);
		}
	}
	
	public static void endProgressBar(SafeProgressBarUpdaterThread progressBarUpdater) {
		if (progressBarUpdater != null) {
			progressBarUpdater.endProgressBar();
		}
	}
	
}


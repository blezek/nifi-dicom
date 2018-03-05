/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.displaywave;

/**
 * <p>An abstract class that encapsulates the features and values from an ECG source,
 * usually for the purpose of displaying it.</p>
 *
 * @author	dclunie
 */
public abstract class SourceECG {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/displaywave/SourceECG.java,v 1.8 2017/01/24 10:50:42 dclunie Exp $";

	/***/
	protected short[][] samples;	// numberOfChannels arrays of nSamplesPerChannel shorts
	/***/
	protected int numberOfChannels;
	/***/
	protected int nSamplesPerChannel;
	/***/
	protected float samplingIntervalInMilliSeconds;
	/***/
	protected float[] amplitudeScalingFactorInMilliVolts;
	/***/
	protected String[] channelNames;

	/***/
	protected int displaySequence[];	// an array of indexes into samples (etc.) sorted into desired sequential display order

	/**
	 * <p>Use the default encoded order.</p>
	 */
	protected void  buildPreferredDisplaySequence() {
		displaySequence = new int[numberOfChannels];
		for (int i=0; i<numberOfChannels; ++i) {
			displaySequence[i]=i;
		}
	}

	/**
	 * <p>Find the named lead in an array of lead names.</p>
	 *
	 * @param	leadNames	an array of String names to designate leads (may be null, or contain null strings, in which case won't be found)
	 * @param	leadName	the string name of the lead wanted (may be null, in which case won't be found)
	 * @return			the index in leadNames of the requested lead if present, else -1
	 */
	static protected int findLead(String[] leadNames,String leadName) {
		if (leadNames != null && leadName != null) {
			String upperCaseLeadName = leadName.toUpperCase(java.util.Locale.US);
			for (int i=0; i<leadNames.length; ++i) {
				if (leadNames[i] != null && leadNames[i].toUpperCase(java.util.Locale.US).equals(upperCaseLeadName)) {
//System.err.println("SourceECG.findLead(): Found leadName="+leadName+" leadNames["+i+"]="+leadNames[i]);
					return i;
				}
			}
		}
		return -1;
	}

	static final private String[] preferred12LeadOrder = { "I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5","V6" };
		
	/**
	 * <p>Using the lead descriptions, look for patterns and determine the desired sequential display order,
	 * defaulting to the encoded order if no recognized pattern.</p>
	 * 
	 * @param	labels		the labels to use to match the preferred order (may or may not be <code>this.channelNames</code>)
	 */
	protected void  buildPreferredDisplaySequence(String[] labels) {
		displaySequence=null;
		if (numberOfChannels == preferred12LeadOrder.length) {
			displaySequence = new int[numberOfChannels];
			for (int i=0; i<numberOfChannels; ++i) {
				int leadIndex = findLead(labels,preferred12LeadOrder[i]);
				if (leadIndex == -1) {
					displaySequence=null;	// give up if any lead not found
					break;
				}
				else {
//System.err.println("SourceECG.buildPreferredDisplaySequence(): Setting displaySequence["+i+"]="+leadIndex);
					displaySequence[i]=leadIndex;
				}
			}
		}
		
		if (displaySequence == null) {
//System.err.println("SourceECG.buildPreferredDisplaySequence(): Using default");
			buildPreferredDisplaySequence();
		}
		
		for (int i=0; i<numberOfChannels; ++i) {
//System.err.println("SourceECG.buildPreferredDisplaySequence(): displaySequence["+i+"]="+displaySequence[i]);
		}
	}

	/***/
	protected String title;

	/**
	 */
	protected static String buildInstanceTitle() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("");
		return buffer.toString();
	}

	/***/
	public short[][] getSamples() { return samples; }
	/***/
	public int getNumberOfChannels() { return numberOfChannels; }
	/***/
	public int getNumberOfSamplesPerChannel() { return nSamplesPerChannel; }
	/***/
	public float getSamplingIntervalInMilliSeconds() { return samplingIntervalInMilliSeconds; }
	/***/
	public float[] getAmplitudeScalingFactorInMilliVolts() { return amplitudeScalingFactorInMilliVolts; }
	/***/
	public String[] getChannelNames() { return channelNames; }
	/***/
	public String getTitle() { return title; }
	/***/
	public int[] getDisplaySequence() { return displaySequence; }
}

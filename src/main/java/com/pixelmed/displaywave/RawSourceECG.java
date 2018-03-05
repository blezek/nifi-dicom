/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.displaywave;

import java.io.IOException; 

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class that encapsulates the features and values from a raw ECG source,
 * usually for the purpose of displaying it.</p>
 *
 * @author	dclunie
 */
public class RawSourceECG extends SourceECG {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/displaywave/RawSourceECG.java,v 1.8 2017/01/24 10:50:42 dclunie Exp $";

	/**
	 * <p>Construct ECG from a raw data an input stream (such as from a file or the network).</p>
	 *
	 * @param	i					the input stream
	 * @param	numberOfChannels			the number of channels (leads)
	 * @param	nSamplesPerChannel			the number of samples per channel (same for all channels)
	 * @param	samplingIntervalInMilliSeconds		the sampling interval (duration of each sample) in milliseconds
	 * @param	amplitudeScalingFactorInMilliVolts	how many millivolts per unit of sample data (may be different for each channel)
	 * @param	interleaved				true if the channels are interleaved, false if successive
	 * @throws	IOException
	 */
	public RawSourceECG(BinaryInputStream i,
				int numberOfChannels,int nSamplesPerChannel,
				float samplingIntervalInMilliSeconds,float amplitudeScalingFactorInMilliVolts,
				boolean interleaved
			) throws IOException {
//System.err.println("RawSourceECG.RawSourceECG(): start");
		this.numberOfChannels=numberOfChannels;
//System.err.println("RawSourceECG.RawSourceECG(): numberOfChannels="+numberOfChannels);
		this.nSamplesPerChannel=nSamplesPerChannel;
//System.err.println("RawSourceECG.RawSourceECG(): nSamplesPerChannel="+nSamplesPerChannel);
		this.samplingIntervalInMilliSeconds=samplingIntervalInMilliSeconds;
		this.amplitudeScalingFactorInMilliVolts = new float[numberOfChannels];
		for (int channel=0; channel<numberOfChannels; ++channel) {
			this.amplitudeScalingFactorInMilliVolts[channel] = amplitudeScalingFactorInMilliVolts;
		}
		
//System.err.println("RawSourceECG.RawSourceECG(): creating sample arrays");
		samples = new short[numberOfChannels][];
		for (int channel=0; channel<numberOfChannels; ++channel) {
			samples[channel] = new short[nSamplesPerChannel];
		}
		if (interleaved) {
			short[] interleavedSamples =  new short[nSamplesPerChannel*numberOfChannels];
//System.err.println("RawSourceECG.RawSourceECG(): about to read interleavedSamples");
			i.readUnsigned16(interleavedSamples,interleavedSamples.length);		// actually handles signed or unsigned fine ... just loads bytes into short array
//System.err.println("RawSourceECG.RawSourceECG(): deinterleaving");
			for (int channel=0; channel<numberOfChannels; ++channel) {
				int index=channel;
				for (int sample=0; sample<nSamplesPerChannel; ++sample) {
					samples[channel][sample] = interleavedSamples[index];
					index+=numberOfChannels;
				}
			}
		}
		else {
			for (int channel=0; channel<numberOfChannels; ++channel) {
				i.readUnsigned16(samples[channel],nSamplesPerChannel);		// actually handles signed or unsigned fine ... just loads bytes into short array
			}
		}
		buildPreferredDisplaySequence();
		title=buildInstanceTitle();
//System.err.println("RawSourceECG.RawSourceECG(): done");
	}
}

/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.displaywave;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.BinaryInputStream;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;

import java.io.IOException; 

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class that encapsulates the features and values from a DICOM ECG source,
 * usually for the purpose of displaying it.</p>
 *
 * @author	dclunie
 */
public class DicomSourceECG extends SourceECG {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/displaywave/DicomSourceECG.java,v 1.10 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceECG.class);
	
	private String[] labelsForChannelsExtractedFromCodes;

	static final private String[] scpCodesForLeads = {
		"5.6.3-9-1",	// "I"
		"5.6.3-9-2",	// "II",
		"5.6.3-9-61",	// "III",
		"5.6.3-9-62",	// "aVR",
		"5.6.3-9-63",	// "aVL",
		"5.6.3-9-64",	// "aVF",
		"5.6.3-9-3",	// "V1",
		"5.6.3-9-4",	// "V2",
		"5.6.3-9-5",	// "V3",
		"5.6.3-9-6",	// "V4",
		"5.6.3-9-7",	// "V5",
		"5.6.3-9-8",	// "V6" 
		};
	
	// the following need to correspond to scpCodesForLeads, and match the labels expected by buildPreferredDisplaySequence()
	
	static final private String[] labelForSCPCodesForLeads = { "I","II","III","aVR","aVL","aVF","V1","V2","V3","V4","V5","V6" };
	
	private String getOurLabelForLeadCode(CodedSequenceItem source) {
		String codeValue = source.getCodeValue();
		String codingScheme = source.getCodingSchemeDesignator();
		if (codingScheme != null && codingScheme.equals("SCPECG") && codeValue != null) {
			for (int i=0; i<scpCodesForLeads.length; ++i) {
				if (codeValue.equals(scpCodesForLeads[i])) {
					return labelForSCPCodesForLeads[i];
				}
			}
		}
		return null;
	}
	
	private void keepTrackOfLeadLabelsForBuildingDisplaySequence(String label,int channel) {
		if (labelsForChannelsExtractedFromCodes == null) {
			labelsForChannelsExtractedFromCodes = new String[numberOfChannels];
		}
		labelsForChannelsExtractedFromCodes[channel] = label;
	}

	/**
	 * @param	list
	 */
	private static String buildInstanceTitle(AttributeList list) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientName));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientID));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyID));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstanceNumber));
		buffer.append(":");
		return buffer.toString();
	}

	/**
	 * <p>Construct ECG from a DICOM waveform object from
	 * an input stream (such as from a file or the network).</p>
	 *
	 * @param	i		the input stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public DicomSourceECG(BinaryInputStream i) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.read(new DicomInputStream(i));
		if (list.get(TagFromName.WaveformSequence) != null) {
			constructSourceECG(list);
		}
	}

	/**
	 * <p>Construct ECG from a DICOM waveform object from
	 * an input stream (such as from a file or the network).</p>
	 *
	 * @param	i		the input stream
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public DicomSourceECG(DicomInputStream i) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.read(i);
		if (list.get(TagFromName.WaveformSequence) != null) {
			constructSourceECG(list);
		}
	}

	/**
	 * <p>Construct ECG from a DICOM waveform object from
	 * a list of DICOM attributes.</p>
	 *
	 * @param	list		the list of attributes that include the description and values of the ECG data
	 * @throws	DicomException
	 */
	public DicomSourceECG(AttributeList list) throws DicomException {
		if (list.get(TagFromName.WaveformSequence) != null) {
			constructSourceECG(list);
		}
	}

	/**
	 * @param	list
	 * @throws	DicomException
	 */
	private void constructSourceECG(AttributeList list) throws DicomException {
//System.err.println("DicomSourceECG.constructSourceECG(): start");
		SequenceAttribute waveformSequence = (SequenceAttribute)(list.get(TagFromName.WaveformSequence));
		if (waveformSequence != null) {
//System.err.println("DicomSourceECG.constructSourceECG(): waveformSequence != null");
			int numberOfMultiplexGroups = waveformSequence.getNumberOfItems();
			if (numberOfMultiplexGroups >= 1) {
				if (numberOfMultiplexGroups > 1) {
					slf4jlogger.warn("constructSourceECG(): using only the first Multiplex Groups - ignoring the rest (there are {})",numberOfMultiplexGroups);
				}
				SequenceItem multiplexGroupItem = waveformSequence.getItem(0);
				if (multiplexGroupItem != null) {
					AttributeList multiplexGroupList = multiplexGroupItem.getAttributeList();
					if (multiplexGroupList != null) {
						numberOfChannels = Attribute.getSingleIntegerValueOrDefault(multiplexGroupList,TagFromName.NumberOfWaveformChannels,0);
//System.err.println("DicomSourceECG.constructSourceECG(): numberOfChannels="+numberOfChannels);

						SequenceAttribute channelDefinitionSequence = (SequenceAttribute)(multiplexGroupList.get(TagFromName.ChannelDefinitionSequence));
						if (channelDefinitionSequence != null) {
							int numberOfChannelDefinitionSequenceItems = channelDefinitionSequence.getNumberOfItems();
//System.err.println("DicomSourceECG.constructSourceECG(): numberOfChannelDefinitionSequenceItems="+numberOfChannelDefinitionSequenceItems);
							if (numberOfChannelDefinitionSequenceItems > numberOfChannels) {
								numberOfChannels=numberOfChannelDefinitionSequenceItems;
							}
							amplitudeScalingFactorInMilliVolts = new float[numberOfChannels];
							channelNames=new String[numberOfChannels];
							for (int channel=0; channel<numberOfChannels; ++channel) {
								double channelSensitivity = 1;
								double channelSensitivityCorrectionFactor = 1;
								double unitsMultiplierToMakeMilliVolts = 1;
								SequenceItem channelDefinitionSequenceItem = channelDefinitionSequence.getItem(channel);
								if (channelDefinitionSequenceItem != null) {
									AttributeList channelDefinitionSequenceItemList = channelDefinitionSequenceItem.getAttributeList();
									if (channelDefinitionSequenceItemList != null) {
										channelSensitivity = Attribute.getSingleDoubleValueOrDefault(
											channelDefinitionSequenceItemList,TagFromName.ChannelSensitivity,1);
										channelSensitivityCorrectionFactor = Attribute.getSingleDoubleValueOrDefault(
											channelDefinitionSequenceItemList,TagFromName.ChannelSensitivityCorrectionFactor,1);
										unitsMultiplierToMakeMilliVolts=1;
										CodedSequenceItem units = CodedSequenceItem.getSingleCodedSequenceItemOrNull(
											channelDefinitionSequenceItemList,TagFromName.ChannelSensitivityUnitsSequence);
										if (units != null && units.getCodingSchemeDesignator().equals("UCUM")) {
											String codeValue = units.getCodeValue();
											if (codeValue != null) {
												if (codeValue.equals("uV")) {
													unitsMultiplierToMakeMilliVolts=0.001;
												}
												else if (codeValue.equals("mV")) {
													unitsMultiplierToMakeMilliVolts=1;
												}
												else if (codeValue.equals("V")) {
													unitsMultiplierToMakeMilliVolts=1000;
												}
											}
										}
										
										StringBuffer strbuf = new StringBuffer();
										String prefix = "";
										CodedSequenceItem source = CodedSequenceItem.getSingleCodedSequenceItemOrNull(
											channelDefinitionSequenceItemList,TagFromName.ChannelSourceSequence);
										if (source != null) {
											String ourLabel = getOurLabelForLeadCode(source);
											if (ourLabel != null) {
												keepTrackOfLeadLabelsForBuildingDisplaySequence(ourLabel,channel);
												strbuf.append(prefix);
												strbuf.append(ourLabel);
												prefix = " ";
											}
											else {	// only use code meaning (which may be verbose) if we really  have to
												String codeMeaning = source.getCodeMeaning();
												if (codeMeaning != null) {
													strbuf.append(prefix);
													strbuf.append(codeMeaning);
													prefix = " ";
												}
											}
										}
										String channelLabel = Attribute.getSingleStringValueOrNull(
											channelDefinitionSequenceItemList,TagFromName.ChannelLabel);
										if (channelLabel != null) {
											strbuf.append(prefix);
											strbuf.append(channelLabel);
											prefix = " ";
										}
										String channelNumber = Attribute.getSingleStringValueOrNull(
											channelDefinitionSequenceItemList,TagFromName.WaveformChannelNumber);
										if (channelNumber != null) {
											strbuf.append(prefix);
											strbuf.append("(");
											strbuf.append(channelNumber);
											prefix = " ";
											strbuf.append(")");
										}
										channelNames[channel]=strbuf.toString();
//System.err.println("DicomSourceECG.constructSourceECG(): channelNames="+channelNames[channel]);
									}
								}
//System.err.println("DicomSourceECG.constructSourceECG(): channelSensitivity="+channelSensitivity);
//System.err.println("DicomSourceECG.constructSourceECG(): channelSensitivityCorrectionFactor="+channelSensitivityCorrectionFactor);
//System.err.println("DicomSourceECG.constructSourceECG(): unitsMultiplierToMakeMilliVolts="+unitsMultiplierToMakeMilliVolts);
								amplitudeScalingFactorInMilliVolts[channel] =
									(float)(channelSensitivity*channelSensitivityCorrectionFactor*unitsMultiplierToMakeMilliVolts);
//System.err.println("DicomSourceECG.constructSourceECG(): amplitudeScalingFactorInMilliVolts="+amplitudeScalingFactorInMilliVolts[channel]);
							}
						}

						nSamplesPerChannel = Attribute.getSingleIntegerValueOrDefault(multiplexGroupList,TagFromName.NumberOfWaveformSamples,0);
//System.err.println("DicomSourceECG.constructSourceECG(): nSamplesPerChannel="+nSamplesPerChannel);
						samplingIntervalInMilliSeconds = (float)(1000/Attribute.getSingleDoubleValueOrDefault(multiplexGroupList,TagFromName.SamplingFrequency,0));
//System.err.println("DicomSourceECG.constructSourceECG(): samplingIntervalInMilliSeconds="+samplingIntervalInMilliSeconds);
						
						// (0x5400,0x1004) US Waveform Bits Allocated       VR=<US>   VL=<0x0002>  [0x0010] 
						// (0x5400,0x1006) CS Waveform Sample Interpretation        VR=<CS>   VL=<0x0002>  <SS>
						
						Attribute waveformData = multiplexGroupList.get(TagFromName.WaveformData);
//System.err.println("DicomSourceECG.constructSourceECG(): waveformData="+waveformData);
						if (waveformData != null) {
							short[] interleavedSamples = waveformData.getShortValues();
							//assert(interleavedSamples.length == nSamplesPerChannel*numberOfChannels);
//System.err.println("DicomSourceECG.constructSourceECG(): interleavedSamples.length="+interleavedSamples.length);
							samples = new short[numberOfChannels][];
							for (int channel=0; channel<numberOfChannels; ++channel) {
								samples[channel] = new short[nSamplesPerChannel];
								int index=channel;
								for (int sample=0; sample<nSamplesPerChannel; ++sample) {
									samples[channel][sample] = interleavedSamples[index];
									index+=numberOfChannels;
								}
							}
						}
					}
				}
			}
		}
		buildPreferredDisplaySequence(labelsForChannelsExtractedFromCodes);		// NOT channelNames
		title=buildInstanceTitle(list);
//System.err.println("DicomSourceECG.constructSourceECG(): title="+title);
//System.err.println("DicomSourceECG.constructSourceECG(): done");
	}
}

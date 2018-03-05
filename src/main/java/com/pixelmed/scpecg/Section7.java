/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Global Measurements section.</p>
 *
 * @author	dclunie
 */
public class Section7 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section7.java,v 1.11 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Global Measurements"; }

	private int numberOfQRSMeasurements;		// number of reference types or QRSs (plus 1 since Ref. Beat 0 always included)
	private int numberOfPacemakerSpikes;
	private int averageRRInterval;
	private int averagePPInterval;

	public int getNumberOfQRSMeasurements() { return numberOfQRSMeasurements; }
	public int getNumberOfPacemakerSpikes() { return numberOfPacemakerSpikes; }
	public int getAverageRRInterval() { return averageRRInterval; }
	public int getAveragePPInterval() { return averagePPInterval; }
	
	private int[] onsetP;		// mS from start of ECG (or from start of reference beat, if a reference beat)
	private int[] offsetP;
	private int[] onsetQRS;
	private int[] offsetQRS;
	private int[] offsetT;
	private int[] axisP;		// in angular degrees, 999 undefined
	private int[] axisQRS;
	private int[] axisT;

	public int[] getPOnset() { return onsetP; }
	public int[] getPOffset() { return offsetP; }
	public int[] getQRSOnset() { return onsetQRS; }
	public int[] getQRSOffset() { return offsetQRS; }
	public int[] getTOffset() { return offsetT; }
	public int[] getPAxis() { return axisP; }
	public int[] getQRSAxis() { return axisQRS; }
	public int[] getTAxis() { return axisT; }

	private int[] pacemakerSpikeLocation;
	private int[] pacemakerSpikeAmplitude;

	public int[] getPacemakerSpikeLocation()  { return pacemakerSpikeLocation; }
	public int[] getPacemakerSpikeAmplitude() { return pacemakerSpikeAmplitude; }

	private int[] pacemakerSpikeType;
	private int[] pacemakerSpikeSource;
	private int[] pacemakerSpikeTriggerIndex;
	private int[] pacemakerSpikePulseWidth;

	public int[] getPacemakerSpikeType()  { return pacemakerSpikeType; }
	public int[] getPacemakerSpikeSource()  { return pacemakerSpikeSource; }
	public int[] getPacemakerSpikeTriggerIndex()  { return pacemakerSpikeTriggerIndex; }
	public int[] getPacemakerSpikePulseWidth()  { return pacemakerSpikePulseWidth; }

	private int numberOfQRSComplexes;
	private int[] qrsType;

	public int getNumberOfQRSComplexes() { return numberOfQRSComplexes; }
	public int[] getQRSType()  { return qrsType; }

	private int ventricularRate; 			// beats per minute
	private int atrialRate;				// beats per minute
	private int correctedQTInterval;		// milliseconds
	private int heartRateCorrectionFormula;		// 0 Unknown or unspecified; 1 Bazett; 2 Hodges; 3-127 Reserved; 128-254 Manufacturer specific; 255 Measurement not available
	private int numberOfBytesInTaggedFields;

	public int getVentricularRate() { return ventricularRate; }
	public int getAtrialRate() { return atrialRate; }
	public int getCorrectedQTInterval() { return correctedQTInterval; }
	public int getHeartRateCorrectionFormula() { return heartRateCorrectionFormula; }
	public int getNumberOfBytesInTaggedFields() { return numberOfBytesInTaggedFields; }

	private int[] rightSizeArray(int[] src,int count) {
		int[] dst = new int[count];
		System.arraycopy(src,0,dst,0,count);
		return dst;
	}

	public Section7(SectionHeader header) {
		super(header);
	}

	public long read(BinaryInputStream i) throws IOException {
		numberOfQRSMeasurements=i.readUnsigned8();
		bytesRead++;
		sectionBytesRemaining--;

		numberOfPacemakerSpikes=i.readUnsigned8();
		bytesRead++;
		sectionBytesRemaining--;

		averageRRInterval=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;

		averagePPInterval=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;
		
		   onsetP = new int[numberOfQRSMeasurements];
		  offsetP = new int[numberOfQRSMeasurements];
		 onsetQRS = new int[numberOfQRSMeasurements];
		offsetQRS = new int[numberOfQRSMeasurements];
		  offsetT = new int[numberOfQRSMeasurements];
		    axisP = new int[numberOfQRSMeasurements];
		  axisQRS = new int[numberOfQRSMeasurements];
		    axisT = new int[numberOfQRSMeasurements];

		int qrsMeasurement=0;
		while (sectionBytesRemaining >=16 && qrsMeasurement < numberOfQRSMeasurements) {
			onsetP[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			offsetP[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			onsetQRS[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			offsetQRS[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			offsetT[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			axisP[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			axisQRS[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			axisT[qrsMeasurement] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			++qrsMeasurement;
		}
		if (qrsMeasurement != numberOfQRSMeasurements) {
			onsetP=rightSizeArray(onsetP,qrsMeasurement);
			offsetP=rightSizeArray(offsetP,qrsMeasurement);
			onsetQRS=rightSizeArray(onsetQRS,qrsMeasurement);
			offsetQRS=rightSizeArray(offsetQRS,qrsMeasurement);
			offsetT=rightSizeArray(offsetT,qrsMeasurement);
			axisP=rightSizeArray(axisP,qrsMeasurement);
			axisQRS=rightSizeArray(axisQRS,qrsMeasurement);
			axisT=rightSizeArray(axisT,qrsMeasurement);
			System.err.println("Section 7 Number Of QRS Measurements specified as "
				+numberOfQRSMeasurements+" but encountered measurements for only "+qrsMeasurement
				+", giving up on rest of section");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}
		
		 pacemakerSpikeLocation = new int[numberOfPacemakerSpikes];
		pacemakerSpikeAmplitude = new int[numberOfPacemakerSpikes];

		int pacemakerSpike=0;
		while (sectionBytesRemaining >=4 && pacemakerSpike < numberOfPacemakerSpikes) {
			pacemakerSpikeLocation[pacemakerSpike] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			pacemakerSpikeAmplitude[pacemakerSpike] = i.readSigned16();		// NB. signed
			bytesRead+=2;
			sectionBytesRemaining-=2;

			++pacemakerSpike;
		}
		if (pacemakerSpike != numberOfPacemakerSpikes) {
			pacemakerSpikeLocation=rightSizeArray(pacemakerSpikeLocation,pacemakerSpike);
			pacemakerSpikeAmplitude=rightSizeArray(pacemakerSpikeAmplitude,pacemakerSpike);
			System.err.println("Section 7 Number Of Pacemaker Spikes specified as "
				+numberOfPacemakerSpikes+" but encountered measurements for only "+pacemakerSpike
				+", giving up on rest of section");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}
		
		             pacemakerSpikeType = new int[numberOfPacemakerSpikes];
		           pacemakerSpikeSource = new int[numberOfPacemakerSpikes];
		     pacemakerSpikeTriggerIndex = new int[numberOfPacemakerSpikes];
		pacemakerSpikePulseWidth = new int[numberOfPacemakerSpikes];

		pacemakerSpike=0;
		while (sectionBytesRemaining >= 6 && pacemakerSpike < numberOfPacemakerSpikes) {
			pacemakerSpikeType[pacemakerSpike]=i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			pacemakerSpikeSource[pacemakerSpike]=i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			pacemakerSpikeTriggerIndex[pacemakerSpike] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			pacemakerSpikePulseWidth[pacemakerSpike] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			++pacemakerSpike;
		}
		if (pacemakerSpike != numberOfPacemakerSpikes) {
			pacemakerSpikeType=rightSizeArray(pacemakerSpikeType,pacemakerSpike);
			pacemakerSpikeSource=rightSizeArray(pacemakerSpikeSource,pacemakerSpike);
			pacemakerSpikeTriggerIndex=rightSizeArray(pacemakerSpikeTriggerIndex,pacemakerSpike);
			pacemakerSpikePulseWidth=rightSizeArray(pacemakerSpikePulseWidth,pacemakerSpike);
			System.err.println("Section 7 Number Of Pacemaker Spikes specified as "
				+numberOfPacemakerSpikes+" but encountered information for only "+pacemakerSpike
				+", giving up on rest of section");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}
		
		if (sectionBytesRemaining >= 2) {			// if we don't check, may be missing, and we read beyond section
			numberOfQRSComplexes=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
		}
		else {
			numberOfQRSComplexes=0;
			System.err.println("Section 7 Number Of QRS Complexes (and everything that follows) missing - end of section encountered first");
		}
		
		qrsType = new int[numberOfQRSComplexes];

		int qrsComplex=0;
		while (sectionBytesRemaining >= 1 && qrsComplex < numberOfQRSComplexes) {
			qrsType[qrsComplex]=i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			++qrsComplex;
		}
		if (qrsComplex != numberOfQRSComplexes) {
			qrsType=rightSizeArray(qrsType,qrsComplex);
			System.err.println("Section 7 Number Of QRS Complexes specified as "
				+numberOfQRSComplexes+" but encountered type for only "+qrsComplex
				+", giving up on rest of section");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}

		if (sectionBytesRemaining >= 9) {
			ventricularRate=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			
			atrialRate=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			
			correctedQTInterval=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			
			heartRateCorrectionFormula=i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			numberOfBytesInTaggedFields=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			
		}
		else {
			System.err.println("Section 7 Missing extra measurements"
				+", giving up on rest of section");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}

		if (sectionBytesRemaining > 0) {
//System.err.println("Section 7 Ignoring manufacturer specific block");
			skipToEndOfSectionIfNotAlreadyThere(i);
		}

		return bytesRead;
	}

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		
		strbuf.append("Number of QRS Measurements = "+numberOfQRSMeasurements+" dec (0x"+Integer.toHexString(numberOfQRSMeasurements)+")\n");
		strbuf.append("Number of Pacemaker Spikes = "+numberOfPacemakerSpikes+" dec (0x"+Integer.toHexString(numberOfPacemakerSpikes)+")\n");
		strbuf.append("Average RR Interval = "+averageRRInterval+" dec (0x"+Integer.toHexString(averageRRInterval)+")\n");
		strbuf.append("Average PP Interval = "+averagePPInterval+" dec (0x"+Integer.toHexString(averagePPInterval)+")\n");
		
		strbuf.append("QRS Measurements (number "+numberOfQRSMeasurements+"):\n");
		for (int i=0; i<onsetP.length; ++i) {		// rather than numberOfQRSMeasurements, in case ran out of bytes; avoids index out of bounds exception
			strbuf.append("\tQRS "+i+":\n");
			strbuf.append("\t\tP Onset = "+onsetP[i]+" dec (0x"+Integer.toHexString(onsetP[i])+")\n");
			strbuf.append("\t\tP Offset = "+offsetP[i]+" dec (0x"+Integer.toHexString(offsetP[i])+")\n");
			strbuf.append("\t\tQRS Onset = "+onsetQRS[i]+" dec (0x"+Integer.toHexString(onsetQRS[i])+")\n");
			strbuf.append("\t\tQRS Offset = "+offsetQRS[i]+" dec (0x"+Integer.toHexString(offsetQRS[i])+")\n");
			strbuf.append("\t\tT Offset = "+offsetT[i]+" dec (0x"+Integer.toHexString(offsetT[i])+")\n");
			strbuf.append("\t\tP Axis = "+axisP[i]+" dec (0x"+Integer.toHexString(axisP[i])+")"+(axisP[i] == 999 ? " undefined" : "")+"\n");
			strbuf.append("\t\tQRS Axis = "+axisQRS[i]+" dec (0x"+Integer.toHexString(axisQRS[i])+")"+(axisQRS[i] == 999 ? " undefined" : "")+"\n");
			strbuf.append("\t\tT Axis = "+axisT[i]+" dec (0x"+Integer.toHexString(axisT[i])+")"+(axisT[i] == 999 ? " undefined" : "")+"\n");
		}
		
		strbuf.append("Pacemaker Spike Measurements (number "+numberOfPacemakerSpikes+"):\n");
		for (int i=0; i<pacemakerSpikeLocation.length; ++i) {	// rather than numberOfPacemakerSpikes, in case ran out of bytes; avoids index out of bounds exception
			strbuf.append("\tSpike "+i+":\n");
			strbuf.append("\t\tPacemaker Spike Location = "+pacemakerSpikeLocation[i]+" dec (0x"+Integer.toHexString(pacemakerSpikeLocation[i])+")\n");
			strbuf.append("\t\tPacemaker Spike Amplitude = "+pacemakerSpikeAmplitude[i]+" dec (0x"+Integer.toHexString(pacemakerSpikeAmplitude[i])+")\n");
		}
		
		strbuf.append("Pacemaker Spike Information (number "+numberOfPacemakerSpikes+"):\n");
		for (int i=0; i<pacemakerSpikeType.length; ++i) {	// rather than numberOfPacemakerSpikes, in case ran out of bytes; avoids index out of bounds exception
			strbuf.append("\tSpike "+i+":\n");
			strbuf.append("\t\tPacemaker Spike Type = "+pacemakerSpikeType[i]+" dec (0x"+Integer.toHexString(pacemakerSpikeType[i])+")\n");
			strbuf.append("\t\tPacemaker Spike Source = "+pacemakerSpikeSource[i]+" dec (0x"+Integer.toHexString(pacemakerSpikeSource[i])+")\n");
			strbuf.append("\t\tPacemaker Spike Trigger Index = "+pacemakerSpikeTriggerIndex[i]+" dec (0x"+Integer.toHexString(pacemakerSpikeTriggerIndex[i])+")\n");
			strbuf.append("\t\tPacemaker Spike Pulse Width = "+pacemakerSpikePulseWidth[i]+" dec (0x"+Integer.toHexString(pacemakerSpikePulseWidth[i])+")\n");
		}

		strbuf.append("QRS Complexes (number "+numberOfQRSComplexes+"):\n");
		for (int i=0; i<qrsType.length; ++i) {		// rather than numberOfQRSComplexes, in case ran out of bytes; avoids index out of bounds exception
			strbuf.append("\tQRS "+i+":\n");
			strbuf.append("\t\tQRS Type = "+qrsType[i]+" dec (0x"+Integer.toHexString(qrsType[i])+")\n");
		}
		
		strbuf.append("Ventricular Rate = "+ventricularRate+" dec (0x"+Integer.toHexString(ventricularRate)+")\n");
		strbuf.append("Atrial Rate = "+atrialRate+" dec (0x"+Integer.toHexString(atrialRate)+")\n");
		strbuf.append("Corrected QT Interval = "+correctedQTInterval+" dec (0x"+Integer.toHexString(correctedQTInterval)+")\n");
		strbuf.append("Heart Rate Correction Formula = "+heartRateCorrectionFormula+" dec (0x"+Integer.toHexString(heartRateCorrectionFormula)+")\n");
		strbuf.append("Number of Bytes in Tagged Fields = "+numberOfBytesInTaggedFields+" dec (0x"+Integer.toHexString(numberOfBytesInTaggedFields)+")\n");

		return strbuf.toString();
	}

	public String validate() {
		return "";
	}

	/**
	 * <p>Get a description of measurement values that may have undefined or missing values.</p>
	 *
	 * <p>The undefined value of <code>999</code> is specifically described by the standard.</p>
	 *
	 * <p>The missing values described in Section 5.10.2 as being defined in the CSE Project
	 * are not described as being appropriate for this section, but have been encountered in this use.</p>
	 *
	 * @param	i	the numeric value that may be missing
	 * @return		a description of the type of missing value
	 */
	public static String describeUndefinedOrMissingValues(int i) {
		String s = "";
		if (i == 999) {
			s="Undefined";
		}
		else if (i == 29999) {
			s="Measurement not computed by the program";
		}
		else if (i == 29998) {
			s="Measurement result not found due to rejection of the lead by measurement program";
		}
		else if (i == 19999) {
			s="Measurement not found because wave was not present in the corresponding lead";
		}
		return s;
	}

	/**
	 * <p>Add a tree node with a numeric value as decimal string, with potentially udnefined or missing values.</p>
	 *
	 * @param	parent	the node to which to add this new node as a child
	 * @param	name	the name of the new node
	 * @param	value	the numeric value of the new node
	 */
	static protected void addNodeOfDecimalWithUndefinedOrMissingValues(SCPTreeRecord parent,String name,int value) {
		new SCPTreeRecord(parent,name,Integer.toString(value)+" dec "+describeUndefinedOrMissingValues(value));
	}

	/**
	 * <p>Get the contents of the section as a tree for display, constructing it if not already done.</p>
	 *
	 * @param	parent	the node to which this section is to be added if it needs to be created de novo
	 * @return		the section as a tree
	 */
	public SCPTreeRecord getTree(SCPTreeRecord parent) {
		if (tree == null) {
			SCPTreeRecord tree = new SCPTreeRecord(parent,"Section",getValueForSectionNodeInTree());
			addSectionHeaderToTree(tree);

			addNodeOfDecimalAndHex(tree,"Number of QRS Measurements",numberOfQRSMeasurements);
			addNodeOfDecimalAndHex(tree,"Number of Pacemaker Spikes",numberOfPacemakerSpikes);
			addNodeOfDecimalWithMissingValues(tree,"Average RR Interval",averageRRInterval);
			addNodeOfDecimalWithMissingValues(tree,"Average PP Interval",averagePPInterval);

			{
				SCPTreeRecord measurementsNode = new SCPTreeRecord(tree,"QRS Measurements",Integer.toString(numberOfQRSMeasurements));
				for (int i=0; i<onsetP.length; ++i) {		// rather than numberOfQRSMeasurements, in case ran out of bytes; avoids index out of bounds exception
					SCPTreeRecord node = new SCPTreeRecord(measurementsNode,"QRS",Integer.toString(i));
					addNodeOfDecimalWithMissingValues(node,"P Onset",onsetP[i]);
					addNodeOfDecimalWithMissingValues(node,"P Offset",offsetP[i]);
					addNodeOfDecimalWithMissingValues(node,"QRS Onset",onsetQRS[i]);
					addNodeOfDecimalWithMissingValues(node,"QRS Offset",offsetQRS[i]);
					addNodeOfDecimalWithMissingValues(node,"T Offset",offsetT[i]);

					addNodeOfDecimalWithUndefinedOrMissingValues(node,"P Axis",axisP[i]);
					addNodeOfDecimalWithUndefinedOrMissingValues(node,"QRS Axis",axisQRS[i]);
					addNodeOfDecimalWithUndefinedOrMissingValues(node,"T Axis",axisT[i]);
				}
			}
			{
				SCPTreeRecord measurementsNode = new SCPTreeRecord(tree,"Pacemaker Spike Measurements",Integer.toString(numberOfPacemakerSpikes));
				for (int i=0; i<pacemakerSpikeLocation.length; ++i) {	// rather than numberOfPacemakerSpikes, in case ran out of bytes; avoids index out of bounds exception
					SCPTreeRecord node = new SCPTreeRecord(measurementsNode,"Spike",Integer.toString(i));
					addNodeOfDecimalAndHex(node,"Pacemaker Spike Location",pacemakerSpikeLocation[i]);
					addNodeOfDecimalAndHex(node,"Pacemaker Spike Amplitude",pacemakerSpikeAmplitude[i]);
				}
			}
			{
				SCPTreeRecord measurementsNode = new SCPTreeRecord(tree,"Pacemaker Spike Information",Integer.toString(numberOfPacemakerSpikes));
				for (int i=0; i<pacemakerSpikeType.length; ++i) {	// rather than numberOfPacemakerSpikes, in case ran out of bytes; avoids index out of bounds exception
					SCPTreeRecord node = new SCPTreeRecord(measurementsNode,"Spike",Integer.toString(i));
					addNodeOfDecimalAndHex(node,"Pacemaker Spike Type",pacemakerSpikeType[i]);
					addNodeOfDecimalAndHex(node,"Pacemaker Spike Source",pacemakerSpikeSource[i]);
					addNodeOfDecimalAndHex(node,"Pacemaker Trigger Index",pacemakerSpikeTriggerIndex[i]);
					addNodeOfDecimalAndHex(node,"Pacemaker Pulse Width",pacemakerSpikePulseWidth[i]);
				}
			}
			{
				SCPTreeRecord measurementsNode = new SCPTreeRecord(tree,"QRS Complexes",Integer.toString(numberOfQRSComplexes));
				for (int i=0; i<qrsType.length; ++i) {		// rather than numberOfQRSMeasurements, in case ran out of bytes; avoids index out of bounds exception
					SCPTreeRecord node = new SCPTreeRecord(measurementsNode,"QRS",Integer.toString(i));
					addNodeOfDecimalAndHex(node,"QRS Type",qrsType[i]);
				}
			}

			addNodeOfDecimalWithMissingValues(tree,"Ventricular Rate",ventricularRate);
			addNodeOfDecimalWithMissingValues(tree,"Atrial Rate",atrialRate);
			addNodeOfDecimalWithMissingValues(tree,"Corrected QT Interval",correctedQTInterval);
			addNodeOfDecimalAndHex(tree,"Heart Rate Correction Formula",heartRateCorrectionFormula);
			addNodeOfDecimalAndHex(tree,"Number of Bytes in Tagged Fields",numberOfBytesInTaggedFields);
		}
		return tree;
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Lead Measurement Results section.</p>
 *
 * @author	dclunie
 */
public class Section10 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section10.java,v 1.8 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Lead Measurement Results"; }

	private int numberOfLeads;
	private int manufacturerSpecific;
	
	public int getNumberOfLeads() { return numberOfLeads; }
	public int getManufacturerSpecific() { return manufacturerSpecific; }
		
	private int[] leadID;
	private int[] lengthOfRecord;
	private int[] Pduration;
	private int[] PRInterval;
	private int[] QRSDuration;
	private int[] QTInterval;
	private int[] QDuration;
	private int[] RDuration;
	private int[] SDuration;
	private int[] RPrimeDuration;
	private int[] SPrimeDuration;
	private int[] QAmplitude;
	private int[] RAmplitude;
	private int[] SAmplitude;
	private int[] RPrimeAmplitude;
	private int[] SPrimeAmplitude;
	private int[] JPointAmplitude;
	private int[] PPlusAmplitude;
	private int[] PMinusAmplitude;
	private int[] TPlusAmplitude;
	private int[] TMinusAmplitude;
	private int[] STSlope;
	private int[] PMorphology;
	private int[] TMorphology;
	private int[] isoElectricSegmentAtQRSOnset;
	private int[] isoElectricSegmentAtQRSEnd;
	private int[] intrinsicoidDeflection;
	private int[] qualityCode;
	private int[] STAmplitudeJPointPlus20ms;
	private int[] STAmplitudeJPointPlus60ms;
	private int[] STAmplitudeJPointPlus80ms;
	private int[] STAmplitudeJPointPlusSixteenthAverageRRInterval;
	private int[] STAmplitudeJPointPlusEighthAverageRRInterval;	
		      	
	public int[] getLeadID() { return leadID; }
	public int[] getLengthOfRecord() { return lengthOfRecord; }
	public int[] getPduration() { return Pduration; }	// (ms) (total P-duration, including P+ and P- components)
	public int[] getPRInterval() { return PRInterval; }	// (ms)
	public int[] getQRSDuration() { return QRSDuration; }	// (ms)
	public int[] getQTInterval() { return QTInterval; }	// (ms)
	public int[] getQDuration() { return QDuration; }	// (ms)
	public int[] getRDuration() { return RDuration; }	// (ms)
	public int[] getSDuration() { return SDuration; }	// (ms)
	public int[] getRPrimeDuration() { return RPrimeDuration; }	// (ms)
	public int[] getSPrimeDuration() { return SPrimeDuration; }	// (ms)
	public int[] getQAmplitude() { return QAmplitude; }	// (µV)
	public int[] getRAmplitude() { return RAmplitude; }	// (µV)
	public int[] getSAmplitude() { return SAmplitude; }	// (µV)
	public int[] getRPrimeAmplitude() { return RPrimeAmplitude; }	// (µV)
	public int[] getSPrimeAmplitude() { return SPrimeAmplitude; }	// (µV)
	public int[] getJPointAmplitude() { return JPointAmplitude; }	// (µV) (amplitude of the J-point = amplitude of end of QRS)
	public int[] getPPlusAmplitude() { return PPlusAmplitude; }	// (µV)
	public int[] getPMinusAmplitude() { return PMinusAmplitude; }	// (µV)
	public int[] getTPlusAmplitude() { return TPlusAmplitude; }	// (µV)
	public int[] getTMinusAmplitude() { return TMinusAmplitude; }	// (µV)
	public int[] getSTSlope() { return STSlope; }		// (µV/s)
	public int[] getPMorphology() { return PMorphology; }
	public int[] getTMorphology() { return TMorphology; }
	public int[] getIsoElectricSegmentAtQRSOnset() { return isoElectricSegmentAtQRSOnset; }	// (in ms) (Segment I)1)
	public int[] getIsoElectricSegmentAtQRSEnd() { return isoElectricSegmentAtQRSEnd; } 	// (in ms) (Segment K)1)
	public int[] getIntrinsicoidDeflection() { return intrinsicoidDeflection; }		// (in ms)
	public int[] getQualityCode() { return qualityCode; }
	public int[] getSTAmplitudeJPointPlus20ms() { return STAmplitudeJPointPlus20ms; }
	public int[] getSTAmplitudeJPointPlus60ms() { return STAmplitudeJPointPlus60ms; }
	public int[] getSTAmplitudeJPointPlus80ms() { return STAmplitudeJPointPlus80ms; }
	public int[] getSTAmplitudeJPointPlusSixteenthAverageRRInterval() { return STAmplitudeJPointPlusSixteenthAverageRRInterval; }
	public int[] getSTAmplitudeJPointPlusEighthAverageRRInterval() { return STAmplitudeJPointPlusEighthAverageRRInterval; }	

	public Section10(SectionHeader header) {
		super(header);
	}
		
	public long read(BinaryInputStream i) throws IOException {
		numberOfLeads=i.readUnsigned16();		// 1-2
		bytesRead+=2;
		sectionBytesRemaining-=2;

		manufacturerSpecific=i.readUnsigned16(); 	// 3-4
		bytesRead+=2;
		sectionBytesRemaining-=2;
						
		leadID = new int[numberOfLeads];
		lengthOfRecord = new int[numberOfLeads];
		Pduration = new int[numberOfLeads];
		PRInterval = new int[numberOfLeads];
		QRSDuration = new int[numberOfLeads];
		QTInterval = new int[numberOfLeads];
		QDuration = new int[numberOfLeads];
		RDuration = new int[numberOfLeads];
		SDuration = new int[numberOfLeads];
		RPrimeDuration = new int[numberOfLeads];
		SPrimeDuration = new int[numberOfLeads];
		QAmplitude = new int[numberOfLeads];
		RAmplitude = new int[numberOfLeads];
		SAmplitude = new int[numberOfLeads];
		RPrimeAmplitude = new int[numberOfLeads];
		SPrimeAmplitude = new int[numberOfLeads];
		JPointAmplitude = new int[numberOfLeads];
		PPlusAmplitude = new int[numberOfLeads];
		PMinusAmplitude = new int[numberOfLeads];
		TPlusAmplitude = new int[numberOfLeads];
		TMinusAmplitude = new int[numberOfLeads];
		STSlope = new int[numberOfLeads];
		PMorphology = new int[numberOfLeads];
		TMorphology = new int[numberOfLeads];
		isoElectricSegmentAtQRSOnset = new int[numberOfLeads]; 
		isoElectricSegmentAtQRSEnd = new int[numberOfLeads]; 
		intrinsicoidDeflection = new int[numberOfLeads];
		qualityCode = new int[numberOfLeads];
		STAmplitudeJPointPlus20ms = new int[numberOfLeads];
		STAmplitudeJPointPlus60ms = new int[numberOfLeads];
		STAmplitudeJPointPlus80ms = new int[numberOfLeads];
		STAmplitudeJPointPlusSixteenthAverageRRInterval = new int[numberOfLeads];
		STAmplitudeJPointPlusEighthAverageRRInterval = new int[numberOfLeads];
		
		// Set to missing values explicitly, since not all measurements may be encoded
		for (int lead=0; lead<numberOfLeads; ++lead) {
			Pduration[lead] = 29999;
			PRInterval[lead] = 29999;
			QRSDuration[lead] = 29999;
			QTInterval[lead] = 29999;
			QDuration[lead] = 29999;
			RDuration[lead] = 29999;
			SDuration[lead] = 29999;
			RPrimeDuration[lead] = 29999;
			SPrimeDuration[lead] = 29999;
			QAmplitude[lead] = 29999;
			RAmplitude[lead] = 29999;
			SAmplitude[lead] = 29999;
			RPrimeAmplitude[lead] = 29999;
			SPrimeAmplitude[lead] = 29999;
			JPointAmplitude[lead] = 29999;
			PPlusAmplitude[lead] = 29999;
			PMinusAmplitude[lead] = 29999;
			TPlusAmplitude[lead] = 29999;
			TMinusAmplitude[lead] = 29999;
			STSlope[lead] = 29999;
			PMorphology[lead] = 29999;
			TMorphology[lead] = 29999;
			isoElectricSegmentAtQRSOnset[lead] = 29999; 
			isoElectricSegmentAtQRSEnd[lead] = 29999; 
			intrinsicoidDeflection[lead] = 29999;
			qualityCode[lead] = 29999;
			STAmplitudeJPointPlus20ms[lead] = 29999;
			STAmplitudeJPointPlus60ms[lead] = 29999;
			STAmplitudeJPointPlus80ms[lead] = 29999;
			STAmplitudeJPointPlusSixteenthAverageRRInterval[lead] = 29999;
			STAmplitudeJPointPlusEighthAverageRRInterval[lead] = 29999;
		}

		int lead=0;
		while (sectionBytesRemaining > 0 && lead < numberOfLeads) {
			                                         leadID[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; // 1-2 
			                                 lengthOfRecord[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; // 3-4
			int remaining = lengthOfRecord[lead];
			if (remaining <= 0) {
				System.err.println("Section 10 Length of record for Lead "+lead+" invalid, specified as "+lengthOfRecord[lead]
					+" dec bytes, so give up on section, though "+sectionBytesRemaining+" dec bytes remaining");
				break;
			}
			if (remaining > sectionBytesRemaining) {
				System.err.println("Section 10 Length of record for Lead "+lead+" is "+remaining+
					" which is larger than left in section "+sectionBytesRemaining+" dec bytes, so give up on section");
				break;
			}
			if (remaining >= 2) {	                                      Pduration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 5-6
			if (remaining >= 2) {	                                     PRInterval[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 7-8
			if (remaining >= 2) {	                                    QRSDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 9-10
			if (remaining >= 2) {	                                     QTInterval[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 11-12
			if (remaining >= 2) {	                                      QDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 13-14
			if (remaining >= 2) {	                                      RDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 15-16
			if (remaining >= 2) {	                                      SDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 17-18
			if (remaining >= 2) {	                                 RPrimeDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 19-20
			if (remaining >= 2) {					 SPrimeDuration[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 21-22
			if (remaining >= 2) {	                                     QAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 23-24
			if (remaining >= 2) {	                                     RAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 25-26
			if (remaining >= 2) {	                                     SAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 27-28
			if (remaining >= 2) {	                                RPrimeAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 29-30
			if (remaining >= 2) {	                                SPrimeAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 31-32
			if (remaining >= 2) {	                                JPointAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 33-34
			if (remaining >= 2) {	                                 PPlusAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 35-36
			if (remaining >= 2) {	                                PMinusAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 37-38
			if (remaining >= 2) {	                                 TPlusAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 39-40
			if (remaining >= 2) {	                                TMinusAmplitude[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 41-42
			if (remaining >= 2) {						STSlope[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 43-44
			if (remaining >= 2) {	                                    PMorphology[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 45-46
			if (remaining >= 2) {	                                    TMorphology[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 47-48
			if (remaining >= 2) {	                   isoElectricSegmentAtQRSOnset[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 49-50
			if (remaining >= 2) {		             isoElectricSegmentAtQRSEnd[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 51-52
			if (remaining >= 2) {	                         intrinsicoidDeflection[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 53-54
			if (remaining >= 2) {	                                    qualityCode[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 55-56
			if (remaining >= 2) {	                      STAmplitudeJPointPlus20ms[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 57-58
			if (remaining >= 2) {	                      STAmplitudeJPointPlus60ms[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 59-60
			if (remaining >= 2) {	                      STAmplitudeJPointPlus80ms[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 61-62
			if (remaining >= 2) {	STAmplitudeJPointPlusSixteenthAverageRRInterval[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 63-64
			if (remaining >= 2) {	   STAmplitudeJPointPlusEighthAverageRRInterval[lead]=i.readSigned16();   bytesRead+=2; sectionBytesRemaining-=2; remaining-=2; } // 65-66
			if (remaining > 0) {
				i.skipInsistently(remaining);
				bytesRead+=remaining;
				sectionBytesRemaining-=remaining;
				remaining=0;
			}
			++lead;
		}
		if (lead != numberOfLeads) {
			System.err.println("Section 10 Number of leads specified as "+numberOfLeads
				+" but encountered only "+lead+" lead measurements");
		}
		
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}
	
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Leads:\n");
		for (int lead=0; lead<numberOfLeads; ++lead) {
			strbuf.append("\tLead "+lead+":\n");
			strbuf.append("\t\t leadID "+leadID[lead]+"\n");
			strbuf.append("\t\t lengthOfRecord "+lengthOfRecord[lead]+"\n");
			strbuf.append("\t\t Pduration "+Pduration[lead]+" "+describeMissingValues(Pduration[lead])+"\n");
			strbuf.append("\t\t PRInterval "+PRInterval[lead]+" "+describeMissingValues(PRInterval[lead])+"\n");
			strbuf.append("\t\t QRSDuration "+QRSDuration[lead]+" "+describeMissingValues(QRSDuration[lead])+"\n");
			strbuf.append("\t\t QTInterval "+QTInterval[lead]+" "+describeMissingValues(QTInterval[lead])+"\n");
			strbuf.append("\t\t QDuration "+QDuration[lead]+" "+describeMissingValues(QDuration[lead])+"\n");
			strbuf.append("\t\t RDuration "+RDuration[lead]+" "+describeMissingValues(RDuration[lead])+"\n");
			strbuf.append("\t\t SDuration "+SDuration[lead]+" "+describeMissingValues(SDuration[lead])+"\n");
			strbuf.append("\t\t RPrimeDuration "+RPrimeDuration[lead]+" "+describeMissingValues(RPrimeDuration[lead])+"\n");
			strbuf.append("\t\t SPrimeDuration "+SPrimeDuration[lead]+" "+describeMissingValues(SPrimeDuration[lead])+"\n");
			strbuf.append("\t\t QAmplitude "+QAmplitude[lead]+" "+describeMissingValues(QAmplitude[lead])+"\n");
			strbuf.append("\t\t RAmplitude "+RAmplitude[lead]+" "+describeMissingValues(RAmplitude[lead])+"\n");
			strbuf.append("\t\t SAmplitude "+SAmplitude[lead]+" "+describeMissingValues(SAmplitude[lead])+"\n");
			strbuf.append("\t\t RPrimeAmplitude "+RPrimeAmplitude[lead]+" "+describeMissingValues(RPrimeAmplitude[lead])+"\n");
			strbuf.append("\t\t SPrimeAmplitude "+SPrimeAmplitude[lead]+" "+describeMissingValues(SPrimeAmplitude[lead])+"\n");
			strbuf.append("\t\t JPointAmplitude "+JPointAmplitude[lead]+" "+describeMissingValues(JPointAmplitude[lead])+"\n");
			strbuf.append("\t\t PPlusAmplitude "+PPlusAmplitude[lead]+" "+describeMissingValues(PPlusAmplitude[lead])+"\n");
			strbuf.append("\t\t PMinusAmplitude "+PMinusAmplitude[lead]+" "+describeMissingValues(PMinusAmplitude[lead])+"\n");
			strbuf.append("\t\t TPlusAmplitude "+TPlusAmplitude[lead]+" "+describeMissingValues(TPlusAmplitude[lead])+"\n");
			strbuf.append("\t\t TMinusAmplitude "+TMinusAmplitude[lead]+" "+describeMissingValues(TMinusAmplitude[lead])+"\n");
			strbuf.append("\t\t STSlope "+STSlope[lead]+" "+describeMissingValues(STSlope[lead])+"\n");
			strbuf.append("\t\t PMorphology "+PMorphology[lead]+" "+describeMissingValues(PMorphology[lead])+"\n");
			strbuf.append("\t\t TMorphology "+TMorphology[lead]+" "+describeMissingValues(TMorphology[lead])+"\n");
			strbuf.append("\t\t isoElectricSegmentAtQRSOnset "+isoElectricSegmentAtQRSOnset[lead]+" "+describeMissingValues(isoElectricSegmentAtQRSOnset[lead])+"\n");
			strbuf.append("\t\t isoElectricSegmentAtQRSEnd "+isoElectricSegmentAtQRSEnd[lead]+" "+describeMissingValues(isoElectricSegmentAtQRSEnd[lead])+"\n");
			strbuf.append("\t\t intrinsicoidDeflection "+intrinsicoidDeflection[lead]+" "+describeMissingValues(intrinsicoidDeflection[lead])+"\n");
			strbuf.append("\t\t qualityCode "+qualityCode[lead]+" "+describeMissingValues(qualityCode[lead])+"\n");
			strbuf.append("\t\t STAmplitudeJPointPlus20ms "+STAmplitudeJPointPlus20ms[lead]+" "+describeMissingValues(STAmplitudeJPointPlus20ms[lead])+"\n");
			strbuf.append("\t\t STAmplitudeJPointPlus60ms "+STAmplitudeJPointPlus60ms[lead]+" "+describeMissingValues(STAmplitudeJPointPlus60ms[lead])+"\n");
			strbuf.append("\t\t STAmplitudeJPointPlus80ms "+STAmplitudeJPointPlus80ms[lead]+" "+describeMissingValues(STAmplitudeJPointPlus80ms[lead])+"\n");
			strbuf.append("\t\t STAmplitudeJPointPlusSixteenthAverageRRInterval "+STAmplitudeJPointPlusSixteenthAverageRRInterval[lead]+" "+describeMissingValues(STAmplitudeJPointPlusSixteenthAverageRRInterval[lead])+"\n");
			strbuf.append("\t\t STAmplitudeJPointPlusEighthAverageRRInterval "+STAmplitudeJPointPlusEighthAverageRRInterval[lead]+" "+describeMissingValues(STAmplitudeJPointPlusEighthAverageRRInterval[lead])+"\n");	
		}
		return strbuf.toString();
	}
		
	public String validate() {
		StringBuffer strbuf = new StringBuffer();
		for (int lead=0; lead<numberOfLeads; ++lead) {
			if (PMorphology[lead] < 0 || PMorphology[lead] > 8) {
				strbuf.append("Section 7 P-Morphology for lead ");
				strbuf.append(lead);
				strbuf.append(" has unrecognized value ");
				strbuf.append(PMorphology[lead]);
				strbuf.append(" dec\n");
			}
			if (TMorphology[lead] < 0 || TMorphology[lead] > 8) {
				strbuf.append("Section 7 T-Morphology for lead ");
				strbuf.append(lead);
				strbuf.append(" has unrecognized value ");
				strbuf.append(TMorphology[lead]);
				strbuf.append(" dec\n");
			}
		}
		return strbuf.toString();
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
			
			{
				SCPTreeRecord measurementsNode = new SCPTreeRecord(tree,"Lead measurements");
				for (int lead=0; lead<numberOfLeads; ++lead) {
					SCPTreeRecord node = new SCPTreeRecord(measurementsNode,"Lead",Integer.toString(lead+1));
					
					addNodeOfDecimalAndHex(node,"Lead ID",leadID[lead]);
					addNodeOfDecimalAndHex(node,"Length of Record",lengthOfRecord[lead]);
					
					addNodeOfDecimalWithMissingValues(node,"P Duration",Pduration[lead]);
					addNodeOfDecimalWithMissingValues(node,"PR Interval",PRInterval[lead]);
					addNodeOfDecimalWithMissingValues(node,"QRS Duration",QRSDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"QT Interval",QTInterval[lead]);
					addNodeOfDecimalWithMissingValues(node,"Q Duration",QDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"R Duration",RDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"S Duration",SDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"R Prime Duration",RPrimeDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"S Prime Duration",SPrimeDuration[lead]);
					addNodeOfDecimalWithMissingValues(node,"Q Amplitude",QAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"R Amplitude",RAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"S Amplitude",SAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"R Prime Amplitude",RPrimeAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"S Prime Amplitude",SPrimeAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"J Point Amplitude",JPointAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"P(+) Amplitude",PPlusAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"P(-) Amplitude",PMinusAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"T(+) Amplitude",TPlusAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"T(-) Amplitude",TMinusAmplitude[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Slope",STSlope[lead]);
					addNodeOfDecimalWithMissingValues(node,"P Morphology",PMorphology[lead]);
					addNodeOfDecimalWithMissingValues(node,"T Morphology",TMorphology[lead]);
					addNodeOfDecimalWithMissingValues(node,"Isoelectric Segment at QRS Onset",isoElectricSegmentAtQRSOnset[lead]);
					addNodeOfDecimalWithMissingValues(node,"Isoelectric Segment at QRS End",isoElectricSegmentAtQRSEnd[lead]);
					addNodeOfDecimalWithMissingValues(node,"Intrinsicoid Deflection",intrinsicoidDeflection[lead]);
					addNodeOfDecimalWithMissingValues(node,"Quality Code",qualityCode[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Amplitude JPoint + 20ms",STAmplitudeJPointPlus20ms[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Amplitude JPoint + 60ms",STAmplitudeJPointPlus60ms[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Amplitude JPoint + 80ms",STAmplitudeJPointPlus80ms[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Amplitude JPoint + 1/16th Average RR Interval",
						STAmplitudeJPointPlusSixteenthAverageRRInterval[lead]);
					addNodeOfDecimalWithMissingValues(node,"ST Amplitude JPoint + 1/8th Average RR Interval",
						STAmplitudeJPointPlusEighthAverageRRInterval[lead]);	
				}
			}
		}
		return tree;
	}
}


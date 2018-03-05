/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.TreeMap;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Lead Definition section.</p>
 *
 * @author	dclunie
 */
public class Section3 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section3.java,v 1.15 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Lead Definition"; }

	private static String[] leadNameDictionary = {
		"Unspecified",
		"I",		// Einthoven
		"II",
		"V1",
		"V2",
		"V3",
		"V4",
		"V5",
		"V6",
		"V7",
		"V2R",		// 10
		"V3R",
		"V4R",
		"V5R",
		"V6R",
		"V7R",
		"X",
		"Y",
		"Z",
		"CC5",
		"CM5",		// 20
		"Left Arm",
		"Right Arm",
		"Left Leg",
		"I (Frank)",	// Frank
		"E",
		"C",
		"A",
		"M",
		"F",
		"H",		// 30
		"I -cal",	// Einthoven
		"II-cal",
		"V1-cal",
		"V2-cal",
		"V3-cal",
		"V4-cal",
		"V5-cal",
		"V6-cal",
		"V7-cal",
		"V2R-cal",	// 40
		"V3R-cal",
		"V4R-cal",
		"V5R-cal",
		"V6R-cal",
		"V7R-cal",
		"X-cal",
		"Y-cal",
		"Z-cal",
		"CC5-cal",
		"CM5-cal",	// 50
		"Left Arm-cal",
		"Right Arm-cal",
		"Left Leg-cal",
		"I-cal (Frank)",	// Frank
		"E-cal",
		"C-cal",
		"A-cal",
		"M-cal",
		"F-cal",
		"H-cal",		// 60
		"III",
		"aVR",
		"aVL",
		"aVF",
		"-aVR",
		"V8",
		"V9",
		"V8R",
		"V9R",
		"D (Nehb – Dorsal)",		// 70
		"A (Nehb – Anterior)",
		"J (Nehb – Inferior)",
		"Defibrillator lead: anterior-lateral",
		"External pacing lead: anteriorposterior",
		"A1 (Auxiliary unipolar lead 1)",
		"A2 (Auxiliary unipolar lead 2)",
		"A3 (Auxiliary unipolar lead 3)",
		"A4 (Auxiliary unipolar lead 4)",
		"V8-cal",
		"V9-cal",	// 80
		"V8R-cal",
		"V9R-cal",
		"D-cal (cal for Nehb – Dorsal)",
		"A-cal (cal for Nehb – Anterior)",
		"J-cal (cal for Nehb – Inferior)"	// 85
	};

	public static String getLeadName(int leadNumber) {
		return (leadNumber > 0 && leadNumber<leadNameDictionary.length) ? leadNameDictionary[leadNumber] : "";
	}

	public static int getLeadNumber(String leadName) {	// -1 = not found
		for (int leadNumber=0; leadNumber<leadNameDictionary.length; ++leadNumber) {
			if (leadNameDictionary[leadNumber].equals(leadName)) {
				return leadNumber;
			}
		}
		return -1;
	}

	private int numberOfLeads;
	private int flagByte;
	private boolean referenceBeatUsedForCompression;
	private boolean reservedBit1;
	private boolean leadsAllSimultaneouslyRecorded;
	private int numberOfSimultaneouslyRecordedLeads;

	private long[] startingSampleNumbers;
	private long[] endingSampleNumbers;
	private long[] numbersOfSamples;
	private int[] leadNumbers;
	private String[] leadNames;
	
	public int getNumberOfLeads() { return numberOfLeads; }
	public int getFlagByte() { return flagByte; }
	public boolean getReferenceBeatUsedForCompression() { return referenceBeatUsedForCompression; }
	public boolean getReservedBit1() { return reservedBit1; }
	public boolean getLeadsAllSimultaneouslyRecorded() { return leadsAllSimultaneouslyRecorded; }
	public int getNumberOfSimultaneouslyRecordedLeads() { return numberOfSimultaneouslyRecordedLeads; }
	public long[] getStartingSampleNumbers() { return startingSampleNumbers; }
	public long[] getEndingSampleNumbers() { return endingSampleNumbers; }
	public long[] getNumbersOfSamples() { return numbersOfSamples; }
	public int[] getLeadNumbers() { return leadNumbers; }

	public String[] getLeadNames() {
		if (leadNames == null) {
			leadNames = new String[numberOfLeads];
			for (int lead=0; lead<numberOfLeads; ++lead) {
				leadNames[lead] = getLeadName(leadNumbers[lead]);
			}
		}
		return leadNames;
	}
		
	public Section3(SectionHeader header) {
		super(header);
	}
		
	public long read(BinaryInputStream i) throws IOException {
		numberOfLeads=i.readUnsigned8();
		bytesRead++;
		sectionBytesRemaining--;
			
		flagByte = i.readUnsigned8();
		bytesRead++;
		sectionBytesRemaining--;
			
		    referenceBeatUsedForCompression = (flagByte&0x01) != 0;
		                       reservedBit1 = (flagByte&0x02) != 0;
		     leadsAllSimultaneouslyRecorded = (flagByte&0x04) != 0;
			 
		numberOfSimultaneouslyRecordedLeads = (flagByte&0xf8)>>3;

		startingSampleNumbers = new long[numberOfLeads];
		  endingSampleNumbers = new long[numberOfLeads];
		     numbersOfSamples = new long[numberOfLeads];
		          leadNumbers = new int[numberOfLeads];
			
		int lead=0;
		while (sectionBytesRemaining > 0) {
			startingSampleNumbers[lead] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			endingSampleNumbers[lead] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			leadNumbers[lead] = i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;
				
			numbersOfSamples[lead]=endingSampleNumbers[lead]-startingSampleNumbers[lead]+1;
			++lead;
		}
		if (lead != numberOfLeads) {
			System.err.println("Section 3 Number Of Leads specified as "+numberOfLeads+" but encountered "+lead);
		}
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}
		
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Number of Leads = "+numberOfLeads+" dec (0x"+Integer.toHexString(numberOfLeads)+")\n");
		strbuf.append("Flag byte = "+flagByte+" dec (0x"+Integer.toHexString(flagByte)+")\n");
		strbuf.append("\t"+(referenceBeatUsedForCompression ? "Reference Beat Used For Compression" : "Reference Beat Not Used For Compression")+"\n");
		strbuf.append("\t"+(reservedBit1 ? "Reserved Bit 1 Set" : "Reserved Bit 1 Reset")+"\n");
		strbuf.append("\t"+(leadsAllSimultaneouslyRecorded ? "Leads All Simultaneously Recorded" : "Leads Not All Simultaneously Recorded")+"\n");
		strbuf.append("\tNumber of Simultaneously Recorded Leads = "+
			numberOfSimultaneouslyRecordedLeads+" dec (0x"+Integer.toHexString(numberOfSimultaneouslyRecordedLeads)+")\n");
		strbuf.append("Lead details:\n");
		for (int lead=0; lead<numberOfLeads; ++lead) {
			strbuf.append("\tLead "+lead+":\n");
			strbuf.append("\t\tStartingSampleNumbers = "+startingSampleNumbers[lead]+" dec (0x"+Long.toHexString(startingSampleNumbers[lead])+")\n");
			strbuf.append("\t\tEndingSampleNumbers = "+endingSampleNumbers[lead]+" dec (0x"+Long.toHexString(endingSampleNumbers[lead])+")\n");
			strbuf.append("\t\tNumber of Samples (computed) = "+numbersOfSamples[lead]+" dec (0x"+Long.toHexString(numbersOfSamples[lead])+")\n");
			strbuf.append("\t\tLead Number = "+leadNumbers[lead]+" dec (0x"+Long.toHexString(leadNumbers[lead])+") ");
			strbuf.append(getLeadName(leadNumbers[lead])+"\n");
		}
		return strbuf.toString();
	}
		
	public String validate() {
		return "";
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
			new SCPTreeRecord(tree,"Number of Leads",Long.toString(numberOfLeads)+" dec (0x"+Long.toHexString(numberOfLeads)+")");
			{
				SCPTreeRecord node = new SCPTreeRecord(tree,"Flag byte","0x"+Integer.toHexString(numberOfLeads));
				new SCPTreeRecord(tree,"Reference Beat Used For Compression",referenceBeatUsedForCompression ? "yes" : "no");
				new SCPTreeRecord(tree,"Reserved Bit 1",reservedBit1 ? "set" : "reset");
				new SCPTreeRecord(tree,"Leads All Simultaneously Recorded",leadsAllSimultaneouslyRecorded ? "yes" : "no");
			}
			new SCPTreeRecord(tree,"Number of Simultaneously Recorded Leads",Integer.toString(numberOfSimultaneouslyRecordedLeads)
				+" dec (0x"+Integer.toHexString(numberOfSimultaneouslyRecordedLeads)+")");
			{
				SCPTreeRecord detailsNode = new SCPTreeRecord(tree,"Lead Details");
				for (int lead=0; lead<numberOfLeads; ++lead) {
					SCPTreeRecord leadNode = new SCPTreeRecord(detailsNode,"Lead",Long.toString(lead+1));
					new SCPTreeRecord(leadNode,"StartingSampleNumbers",Long.toString(startingSampleNumbers[lead])
						+" dec (0x"+Long.toHexString(startingSampleNumbers[lead])+")");
					new SCPTreeRecord(leadNode,"EndingSampleNumbers",Long.toString(endingSampleNumbers[lead])
						+" dec (0x"+Long.toHexString(endingSampleNumbers[lead])+")");
					new SCPTreeRecord(leadNode,"Number of Samples (computed)",Long.toString(numbersOfSamples[lead])
						+" dec (0x"+Long.toHexString(numbersOfSamples[lead])+")");
					new SCPTreeRecord(leadNode,"Lead Number",Long.toString(leadNumbers[lead])
						+" dec (0x"+Long.toHexString(leadNumbers[lead])+")");
					new SCPTreeRecord(leadNode,"Lead Name",getLeadName(leadNumbers[lead]));
				}
			}
		}
		return tree;
	}
}


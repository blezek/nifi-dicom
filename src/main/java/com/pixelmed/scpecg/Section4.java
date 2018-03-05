/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.TreeMap;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG QRS Locations section.</p>
 *
 * @author	dclunie
 */
public class Section4 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section4.java,v 1.11 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "QRS Locations"; }

	private int lengthOfReferenceBeat0DataInMilliSeconds;
	private int sampleNumberOfQRSOfFiducial;
	private int totalNumberOfQRSComplexes;
	private int[]  beatType;
	private long[] sampleNumberOfResidualToStartSubtractingQRS;
	private long[] sampleNumberOfResidualOfFiducial;
	private long[] sampleNumberOfResidualToEndSubtractingQRS;
	private long[] sampleNumberOfResidualToStartProtectedArea;
	private long[] sampleNumberOfResidualToEndProtectedArea;
	
	public int getLengthOfReferenceBeat0DataInMilliSeconds() { return lengthOfReferenceBeat0DataInMilliSeconds; }
	public int getSampleNumberOfQRSOfFiducial() { return sampleNumberOfQRSOfFiducial; }
	public int getTotalNumberOfQRSComplexes() { return totalNumberOfQRSComplexes; }
	public int[] getBeatType() { return beatType; }
	public long[] getSampleNumberOfResidualToStartSubtractingQRS() { return sampleNumberOfResidualToStartSubtractingQRS; }
	public long[] getSampleNumberOfResidualOfFiducial() { return sampleNumberOfResidualOfFiducial; }
	public long[] getSampleNumberOfResidualToEndSubtractingQRS() { return sampleNumberOfResidualToEndSubtractingQRS; }
	public long[] getSampleNumberOfResidualToStartProtectedArea() { return sampleNumberOfResidualToStartProtectedArea; }
	public long[] getSampleNumberOfResidualToEndProtectedArea() { return sampleNumberOfResidualToEndProtectedArea; }
		
	public Section4(SectionHeader header) {
		super(header);
	}
		
	public long read(BinaryInputStream i) throws IOException {
		lengthOfReferenceBeat0DataInMilliSeconds=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;
						
		sampleNumberOfQRSOfFiducial=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;
						
		totalNumberOfQRSComplexes=i.readUnsigned16();
		bytesRead+=2;
		sectionBytesRemaining-=2;
		
		                                   beatType = new  int[totalNumberOfQRSComplexes];
		sampleNumberOfResidualToStartSubtractingQRS = new long[totalNumberOfQRSComplexes];
		           sampleNumberOfResidualOfFiducial = new long[totalNumberOfQRSComplexes];
		  sampleNumberOfResidualToEndSubtractingQRS = new long[totalNumberOfQRSComplexes];

		int qrsComplex=0;
		while (sectionBytesRemaining > 0 && qrsComplex < totalNumberOfQRSComplexes) {
			beatType[qrsComplex] = i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;
			sampleNumberOfResidualToStartSubtractingQRS[qrsComplex] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			sampleNumberOfResidualOfFiducial[qrsComplex] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			sampleNumberOfResidualToEndSubtractingQRS[qrsComplex] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			++qrsComplex;
		}
		if (qrsComplex != totalNumberOfQRSComplexes) {
			System.err.println("Section 4 Number Of QRS Complexes specified as "+totalNumberOfQRSComplexes
				+" but encountered "+qrsComplex+" reference beat subtraction zones");
		}
		
		sampleNumberOfResidualToStartProtectedArea = new long[totalNumberOfQRSComplexes];
		  sampleNumberOfResidualToEndProtectedArea = new long[totalNumberOfQRSComplexes];

		qrsComplex=0;
		while (sectionBytesRemaining > 0 && qrsComplex < totalNumberOfQRSComplexes) {
			sampleNumberOfResidualToStartProtectedArea[qrsComplex] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			sampleNumberOfResidualToEndProtectedArea[qrsComplex] = i.readUnsigned32();
			bytesRead+=4;
			sectionBytesRemaining-=4;
			++qrsComplex;
		}
		if (qrsComplex != totalNumberOfQRSComplexes) {
			System.err.println("Section 4 Number Of QRS Complexes specified as "+totalNumberOfQRSComplexes
				+" but encountered "+qrsComplex+" protected areas");
		}
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}
		
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Length of Reference Beat 0 Data In MilliSeconds = "+lengthOfReferenceBeat0DataInMilliSeconds
			+" dec (0x"+Integer.toHexString(lengthOfReferenceBeat0DataInMilliSeconds)+")\n");
		strbuf.append("Sample Number of QRS of Fiducial = "+sampleNumberOfQRSOfFiducial+" dec (0x"+Integer.toHexString(sampleNumberOfQRSOfFiducial)+")\n");
		strbuf.append("Total Number Of QRS Complexes = "+totalNumberOfQRSComplexes+" dec (0x"+Integer.toHexString(totalNumberOfQRSComplexes)+")\n");
		strbuf.append("Reference beat subtraction zones:\n");
		for (int qrsComplex=0; qrsComplex<totalNumberOfQRSComplexes; ++qrsComplex) {
			strbuf.append("\tQRS Complex "+qrsComplex+":\n");
			strbuf.append("\t\tBeat Type "+beatType[qrsComplex]+" dec (0x"+Integer.toHexString(beatType[qrsComplex])+")\n");
			strbuf.append("\t\tSample Number of Residual to Start Subtracting QRS "+
				sampleNumberOfResidualToStartSubtractingQRS[qrsComplex]+" dec (0x"+Long.toHexString(sampleNumberOfResidualToStartSubtractingQRS[qrsComplex])+")\n");
			strbuf.append("\t\tSample Number of Residual of Fiducial "+
				sampleNumberOfResidualOfFiducial[qrsComplex]+" dec (0x"+Long.toHexString(sampleNumberOfResidualOfFiducial[qrsComplex])+")\n");
			strbuf.append("\t\tSample Number of Residual to End Subtracting QRS "+
				sampleNumberOfResidualToEndSubtractingQRS[qrsComplex]+" dec (0x"+Long.toHexString(sampleNumberOfResidualToEndSubtractingQRS[qrsComplex])+")\n");
		}
		strbuf.append("Protected areas:\n");
		for (int qrsComplex=0; qrsComplex<totalNumberOfQRSComplexes; ++qrsComplex) {
			strbuf.append("\tQRS Complex "+qrsComplex+":\n");
			strbuf.append("\t\tSample Number of Residual to Start Protected Area "+
				sampleNumberOfResidualToStartProtectedArea[qrsComplex]+" dec (0x"+Long.toHexString(sampleNumberOfResidualToStartProtectedArea[qrsComplex])+")\n");
			strbuf.append("\t\tSample Number of Residual to End Protected Area "+
				sampleNumberOfResidualToEndProtectedArea[qrsComplex]+" dec (0x"+Long.toHexString(sampleNumberOfResidualToEndProtectedArea[qrsComplex])+")\n");
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

			new SCPTreeRecord(tree,"Length of Reference Beat 0 Data In MilliSeconds",
				Integer.toString(lengthOfReferenceBeat0DataInMilliSeconds)+" dec (0x"+Integer.toHexString(lengthOfReferenceBeat0DataInMilliSeconds)+")");
			new SCPTreeRecord(tree,"Sample Number of QRS of Fiducial",
				Integer.toString(sampleNumberOfQRSOfFiducial)+" dec (0x"+Integer.toHexString(sampleNumberOfQRSOfFiducial)+")");
			new SCPTreeRecord(tree,"Total Number Of QRS Complexes",
				Integer.toString(totalNumberOfQRSComplexes)+" dec (0x"+Integer.toHexString(totalNumberOfQRSComplexes)+")");
				//);
			{
				SCPTreeRecord zonesNode = new SCPTreeRecord(tree,"Reference beat subtraction zones");
				for (int qrsComplex=0; qrsComplex<totalNumberOfQRSComplexes; ++qrsComplex) {
					SCPTreeRecord qrsNode = new SCPTreeRecord(zonesNode,"QRS Complex",Integer.toString(qrsComplex));
					new SCPTreeRecord(qrsNode,"Beat Type",Integer.toString(beatType[qrsComplex])
						+" dec (0x"+Integer.toHexString(beatType[qrsComplex])+")");
					new SCPTreeRecord(qrsNode,"Sample Number of Residual to Start Subtracting QRS",
						Long.toString(sampleNumberOfResidualToStartSubtractingQRS[qrsComplex])
						+" dec (0x"+Long.toHexString(sampleNumberOfResidualToStartSubtractingQRS[qrsComplex])+")");
					new SCPTreeRecord(qrsNode,"Sample Number of Residual of Fiducial",
						Long.toString(sampleNumberOfResidualOfFiducial[qrsComplex])
						+" dec (0x"+Long.toHexString(sampleNumberOfResidualOfFiducial[qrsComplex])+")");
					new SCPTreeRecord(qrsNode,"Sample Number of Residual to End Subtracting QRS",
						Long.toString(sampleNumberOfResidualToEndSubtractingQRS[qrsComplex])
						+" dec (0x"+Long.toHexString(sampleNumberOfResidualToEndSubtractingQRS[qrsComplex])+")");
				}
			}
			{
				SCPTreeRecord zonesNode = new SCPTreeRecord(tree,"Protected areas");
				for (int qrsComplex=0; qrsComplex<totalNumberOfQRSComplexes; ++qrsComplex) {
					SCPTreeRecord qrsNode = new SCPTreeRecord(zonesNode,"QRS Complex",Integer.toString(qrsComplex));
					new SCPTreeRecord(qrsNode,"Sample Number of Residual to Start Protected Area",
						Long.toString(sampleNumberOfResidualToStartProtectedArea[qrsComplex])
						+" dec (0x"+Long.toHexString(sampleNumberOfResidualToStartProtectedArea[qrsComplex])+")");
					new SCPTreeRecord(qrsNode,"Sample Number of Residual to End Protected Area",
						Long.toString(sampleNumberOfResidualToEndProtectedArea[qrsComplex])
						+" dec (0x"+Long.toHexString(sampleNumberOfResidualToEndProtectedArea[qrsComplex])+")");
				}
			}
		}
		return tree;
	}
}


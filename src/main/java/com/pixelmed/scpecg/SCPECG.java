/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import com.pixelmed.dicom.BinaryInputStream;
import com.pixelmed.dicom.BinaryOutputStream;
import com.pixelmed.display.ApplicationFrame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.FileInputStream; 
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JScrollPane;

/**
 * <p>A class to encapsulate an entire SCP-ECG object.</p>
 *
 * <p>Typically this class would be used to read an SCP-ECG object from a file,
 * and to do something with the decompressed waveform data values. For example:</p>
 *
 * <pre>
try {			
	BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(arg[0])),false);	// always little endian
	SCPECG scpecg = new SCPECG(i,verbose);
	short[][] data = scpecg.getDecompressedRhythmData();
	BinaryOutputStream o = new BinaryOutputStream(new BufferedOutputStream(new FileOutputStream(arg[1])),false);	// little endian
	// write interleaved raw little endian data
	int numberOfChannels = data.length;
	int nSamplesPerChannel = data[0].length;	// assume all the same
	for (int sample=0; sample&lt;nSamplesPerChannel; ++sample) {
		for (int lead=0;lead&lt;numberOfChannels; ++lead) {
			o.writeSigned16(data[lead][sample]);
		}
	}
	o.close();
}
catch (Exception e) {
	slf4jlogger.error("",e);
}
 * </pre>
 * <p>One might want to dump the entire contents of the instance as a string, as follows:</p>
 *
 * <pre>
System.err.print(scpecg);
 * </pre>
 * <p>or perhaps to display the contents as a JTree:</p>
 *
 * <pre>
ApplicationFrame af = new ApplicationFrame();
JScrollPane scrollPane = new JScrollPane();
SCPTreeBrowser browser = new SCPTreeBrowser(scpecg,scrollPane);
af.getContentPane().add(scrollPane);
af.pack();
af.setVisible(true);
 * </pre>
 *
 * @author	dclunie
 */
public class SCPECG {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/SCPECG.java,v 1.26 2017/01/24 10:50:46 dclunie Exp $";
	
	private class ReferenceBeatSubtractionZone {
		private int numberOfReferenceBeatSubtractionZones;
		long fcm;		// sample number of the fiducial relative to the beginning of reference beat 0
		private long[] start;
		private long[] fc;
		private long[] end;
		
		ReferenceBeatSubtractionZone(long sampleNumberOfQRSOfFiducial,
				long[] sampleNumberOfResidualToStartSubtractingQRS,
				long[] sampleNumberOfResidualOfFiducial,
				long[] sampleNumberOfResidualToEndSubtractingQRS) {
			fcm=sampleNumberOfQRSOfFiducial;
			start=sampleNumberOfResidualToStartSubtractingQRS;
			fc=sampleNumberOfResidualOfFiducial;
			end=sampleNumberOfResidualToEndSubtractingQRS;
			//assert(start != null);
			//assert(fc != null);
			//assert(end != null);
			numberOfReferenceBeatSubtractionZones=start.length;
			//assert(numberOfReferenceBeatSubtractionZones == fc.length);
			//assert(numberOfReferenceBeatSubtractionZones == end.length);
		}
		
		public int getSampleOffsetWithinReferenceBeatSubtractionZone(long sample) {	// -1 is flag that it is not in the zone
			for (int qrs=0; qrs<numberOfReferenceBeatSubtractionZones; ++qrs) {
				if (sample >= start[qrs] && sample <= end[qrs]) {
					//assert((fc[qrs] - start[qrs]) <= max Java int);
					int offsetToAlignFiducial = (int)(fcm - (fc[qrs] - start[qrs]));
					//assert((sample - start[qrs]) <= max Java int);
					int offsetFromStartOfReferenceBeat = (int)(sample - start[qrs] + offsetToAlignFiducial);	// numbered from zero
					//assert(offsetFromStartOfReferenceBeat != -1);
//System.err.println("In the ReferenceBeatSubtractionZone "+sample);
					return offsetFromStartOfReferenceBeat;
				}
			}
			return -1;
		}
	}

	private class ProtectedArea {
		private int numberOfProtectedAreas;
		private long[] start;
		private long[] end;
		
		ProtectedArea(long[] sampleNumberOfResidualToStartProtectedArea,long[] sampleNumberOfResidualToEndProtectedArea) {
			start=sampleNumberOfResidualToStartProtectedArea;
			end=sampleNumberOfResidualToEndProtectedArea;
			//assert(start != null);
			//assert(end != null);
			numberOfProtectedAreas=start.length;
			//assert(numberOfProtectedAreas == end.length);
		}
		
		public boolean isSampleWithinProtectedArea(long sample) {
			boolean within=false;
			for (int area=0; area<numberOfProtectedAreas; ++area) {
				if (sample >= start[area] && sample <= end[area]) {
					within=true;
					break;
				}
			}
			return within;
		}
	}

	private TreeMap sections;
	private RecordHeader recordHeader;
	
	private short[][] decompressedRhythmData;
	
	/**
	 * <p>Get the decompressed rhythm data.</p>
	 *
	 * <p>This includes undecimating and adding in any reference beat.</p>
	 *
	 * <p>Note that interpolation and filtering are NOT applied in the current implementation.</p>
	 *
	 * @return		arrays of samples for each lead
	 */
	public short[][] getDecompressedRhythmData() { return decompressedRhythmData; }
	
	private void decompressRhythmData() {
		Section3 section3 = getSection3();
		int                       numberOfLeads = section3 == null ?     0 : section3.getNumberOfLeads();
		long[]                 numbersOfSamples = section3 == null ?  null : section3.getNumbersOfSamples();
		boolean referenceBeatUsedForCompression = section3 == null ? false : section3.getReferenceBeatUsedForCompression();
		
		Section2 section2 = getSection2();
		int   numberOfHuffmanTables = section2 == null ?    0 : section2.getNumberOfHuffmanTables();
		ArrayList huffmanTablesList = section2 == null ? null : section2.getHuffmanTables();
		
		Section4 section4 = getSection4();
		int                    sampleNumberOfQRSOfFiducial = section4 == null ?    0 : section4.getSampleNumberOfQRSOfFiducial();
		long[] sampleNumberOfResidualToStartSubtractingQRS = section4 == null ? null : section4.getSampleNumberOfResidualToStartSubtractingQRS();
		long[]            sampleNumberOfResidualOfFiducial = section4 == null ? null : section4.getSampleNumberOfResidualOfFiducial();
		long[]   sampleNumberOfResidualToEndSubtractingQRS = section4 == null ? null : section4.getSampleNumberOfResidualToEndSubtractingQRS();
		long[]  sampleNumberOfResidualToStartProtectedArea = section4 == null ? null : section4.getSampleNumberOfResidualToStartProtectedArea();
		long[]    sampleNumberOfResidualToEndProtectedArea = section4 == null ? null : section4.getSampleNumberOfResidualToEndProtectedArea();
		
		Section5Or6 section5 = getSection5();
		int sampleTimeIntervalForReference = section5 == null ?    0 : section5.getSampleTimeInterval();

		Section5Or6 section6 = getSection6();
		int    amplitudeValueMultiplier = section6 == null ?    0 : section6.getAmplitudeValueMultiplier();
		int          differenceDataUsed = section6 == null ?    0 : section6.getDifferenceDataUsed();
		byte[][]     compressedLeadData = section6 == null ? null : section6.getCompressedLeadData();
		int      bimodalCompressionUsed = section6 == null ?    0 : section6.getBimodalCompressionUsed();
		int sampleTimeIntervalForRhythm = section6 == null ?    0 : section6.getSampleTimeInterval();
		
		int samplingRateDecimationFactor = (sampleTimeIntervalForReference == 0)
			? 1
			: sampleTimeIntervalForRhythm/sampleTimeIntervalForReference;
//System.err.println("samplingRateDecimationFactor "+samplingRateDecimationFactor);

//System.err.println("bimodalCompressionUsed "+bimodalCompressionUsed);
		//ProtectedArea protectedAreas = bimodalCompressionUsed == 0
		ProtectedArea protectedAreas = (sampleNumberOfResidualToStartProtectedArea == null || sampleNumberOfResidualToEndProtectedArea == null)
			? null
			: new ProtectedArea(sampleNumberOfResidualToStartProtectedArea,sampleNumberOfResidualToEndProtectedArea);

		ReferenceBeatSubtractionZone referenceBeatSubtractionZones = referenceBeatUsedForCompression
			? new ReferenceBeatSubtractionZone(sampleNumberOfQRSOfFiducial,
					sampleNumberOfResidualToStartSubtractingQRS,sampleNumberOfResidualOfFiducial,sampleNumberOfResidualToEndSubtractingQRS)
			: null;

		decompressedRhythmData = new short[numberOfLeads][];
		for (int lead=0; lead<numberOfLeads; ++lead) {
//System.err.println("Decompressing rhythm or residual data for lead "+lead);
			decompressedRhythmData[lead] = null;
			// assert(numbersOfSamples[lead] <= largest Java int);
			int useNumberOfSamples = (int)(numbersOfSamples[lead]);
//System.err.println("useNumberOfSamples = "+useNumberOfSamples);
			try {
				HuffmanDecoder decoder = new HuffmanDecoder(
						compressedLeadData[lead],
						differenceDataUsed,amplitudeValueMultiplier/1000,	// amplitudeValueMultiplier is nanoVolts, not microVolts
						numberOfHuffmanTables,huffmanTablesList);

				{
					decompressedRhythmData[lead] = new short[useNumberOfSamples];
					short value = 0;
					int decimationOffsetCount = 0;
					short lastDecimatedValue = 0;
					short currentDecimatedValue = 0;
					for (int sample=1; sample<=useNumberOfSamples; ++sample) {
						boolean within = protectedAreas != null && protectedAreas.isSampleWithinProtectedArea(sample);
//System.err.println("["+sample+"] in protected area = "+within);
						if (within) {
							value = decoder.decode();
							decimationOffsetCount = 0;
						}
						else {
							if (samplingRateDecimationFactor <= 1) {	// should never happen, but if we didn't check, division by zero
								value = decoder.decode();
							}
							else {
								//int interpolationOffset = decimationOffsetCount%samplingRateDecimationFactor;
								//double interpolationMultipier = ((double)interpolationOffset)/samplingRateDecimationFactor;
								//if (interpolationOffset == 0) {
								//	lastDecimatedValue = currentDecimatedValue;
								//	currentDecimatedValue = decoder.decode();
								//	if (decimationOffsetCount < samplingRateDecimationFactor) {
								//		lastDecimatedValue=currentDecimatedValue;			// there was no previous value to interpolate from
								//	}
								//}
								//value = (short)(lastDecimatedValue + (currentDecimatedValue-lastDecimatedValue)*interpolationMultipier);
								
								int interpolationOffset = decimationOffsetCount%samplingRateDecimationFactor;
								if (interpolationOffset == 0) {
									currentDecimatedValue = decoder.decode();
								}
								value = (short)currentDecimatedValue;
							}
							++decimationOffsetCount;
						}
						if (referenceBeatSubtractionZones == null) {
							decompressedRhythmData[lead][sample-1]=value;
						}
						else {
							int offset = referenceBeatSubtractionZones.getSampleOffsetWithinReferenceBeatSubtractionZone(sample);
//System.err.println("["+sample+"] reference beat subtraction zone offset="+offset);
							if (offset != -1) {
//System.err.print("["+lead+","+sample+"] = "+value+" BEFORE; AFTER ");
								value+=decompressedReferenceBeatData[lead][offset];
							}
							decompressedRhythmData[lead][sample-1]=value;
						}
//System.err.println("["+lead+","+sample+"] = "+value);
					}	
				}
//System.err.println("Decoder status after all samples done "+decoder.toString());
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
	}
	
	private short[][] decompressedReferenceBeatData;
	
	/**
	 * <p>Get the decompressed reference beat data.</p>
	 *
	 * @return		arrays of samples for each lead
	 */
	public short[][] getDecompressedReferenceBeatData() { return decompressedReferenceBeatData; }

	private void decompressReferenceBeatData() {
		Section3 section3 = (Section3)(sections.get(new Integer(3)));
		int       numberOfLeads = section3 == null ?    0 : section3.getNumberOfLeads();
		
		Section2 section2 = (Section2)(sections.get(new Integer(2)));
		int   numberOfHuffmanTables = section2 == null ?    0 : section2.getNumberOfHuffmanTables();
		ArrayList huffmanTablesList = section2 == null ? null : section2.getHuffmanTables();
		
		Section4 section4 = (Section4)(sections.get(new Integer(4)));
		int lengthOfReferenceBeat0DataInMilliSeconds = section4 == null ?    0 : section4.getLengthOfReferenceBeat0DataInMilliSeconds();
		
		Section5Or6 section5 = (Section5Or6)(sections.get(new Integer(5)));
		if (section5 == null) {
			return;
		}
		
		int amplitudeValueMultiplier = section5 == null ?    0 : section5.getAmplitudeValueMultiplier();
		int       sampleTimeInterval = section5 == null ?    0 : section5.getSampleTimeInterval();
		int       differenceDataUsed = section5 == null ?    0 : section5.getDifferenceDataUsed();
		byte[][]  compressedLeadData = section5 == null ? null : section5.getCompressedLeadData();

		int numberOfSamples = 1000*lengthOfReferenceBeat0DataInMilliSeconds/sampleTimeInterval;	// See prENV 1064 5.7.2
//System.err.println("Number of reference beat samples "+numberOfSamples);

		decompressedReferenceBeatData = new short[numberOfLeads][];
		for (int lead=0; lead<numberOfLeads; ++lead) {
//System.err.println("Decompressing reference beat data for lead "+lead);
			HuffmanDecoder decoder = new HuffmanDecoder(
					compressedLeadData[lead],
					differenceDataUsed,amplitudeValueMultiplier/1000,	// amplitudeValueMultiplier is nanoVolts, not microVolts
					numberOfHuffmanTables,huffmanTablesList);
			try {
				// assert numbersOfSamples[lead] <= largest Java int
				decompressedReferenceBeatData[lead] = decoder.decode((int)(numberOfSamples));
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
	}

	/**
	 * <p>Read an SCP-ECG object from an input stream.</p>
	 *
	 * <p>As errors are encountered, they are described on <code>stderr</code>.</p>
	 *
	 * <p>The sections are also validated as they are read, and Section 0
	 * is validated against the other sections.</p>
	 *
	 * @param	i	the input stream (should have been opened as little endian)
	 * @param	verbose	if true, then the progress of the read is described
	 */
	public SCPECG(BinaryInputStream i,boolean verbose) throws IOException {
		sections = new TreeMap();
		recordHeader = new RecordHeader();
		long bytesRead = recordHeader.read(i);
		long recordBytesRemaining = recordHeader.getRecordLength()-bytesRead;	// i.e. CRC and length itself are included in length
		long byteOffset = bytesRead;
		if (verbose) System.err.print(recordHeader);
		
		while (recordBytesRemaining>0) {		// do sections
			SectionHeader sectionHeader = new SectionHeader();
			bytesRead = sectionHeader.read(i,byteOffset);
//System.err.println("Section "+sectionHeader.getSectionIDNumber());
//System.err.println("[Bytes read = "+bytesRead+" dec (0x"+Long.toHexString(bytesRead)+")]");
			recordBytesRemaining-=bytesRead;
//System.err.println("[Record bytes remaining = "+recordBytesRemaining+" dec (0x"+Long.toHexString(recordBytesRemaining)+")]");
			byteOffset+=bytesRead;
//System.err.println("[Byte offset = "+byteOffset+" dec (0x"+Long.toHexString(byteOffset)+")]");
			if (verbose) System.err.print(sectionHeader);
				
			Section section = Section.makeSection(sectionHeader,sections);
			bytesRead = section.read(i);
//System.err.println("[Bytes read = "+bytesRead+" dec (0x"+Long.toHexString(bytesRead)+")]");
			recordBytesRemaining-=bytesRead;
//System.err.println("[Record bytes remaining = "+recordBytesRemaining+" dec (0x"+Long.toHexString(recordBytesRemaining)+")]");
			byteOffset+=bytesRead;
//System.err.println("[Byte offset = "+byteOffset+" dec (0x"+Long.toHexString(byteOffset)+")]");
			if (verbose) System.err.println(section);
			
			System.err.print(section.validate());
			
			sections.put(new Integer(sectionHeader.getSectionIDNumber()),section);
		}
		Section0 section0 = (Section0)(sections.get(new Integer(0)));
		System.err.print(section0.validateAgainstOtherSections(sections));
		decompressReferenceBeatData();
		decompressRhythmData();
	}
	
	/**
	 * <p>Dump the object as a <code>String</code>.</p>
	 *
	 * @return		the object as a <code>String</code>
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		
		Section1 section1 = getSection1();
		if (section1 == null) {
			strbuf.append("No demographic and administrative data\n");
		}
		else {
			strbuf.append(section1);
		}
		
		Section2 section2 = getSection2();
		if (section2 == null) {
			strbuf.append("No Huffman entropy coding\n");
		}
		else {
			if (section2.useDefaultTable()) {
				strbuf.append("Huffman entropy coding with default table\n");
			}
			else {
				strbuf.append("Huffman entropy coding with "+section2.getNumberOfHuffmanTables()+" specified tables\n");
			}
		}
		
		Section3 section3 = getSection3();
		if (section3 == null) {
			strbuf.append("No lead description :(\n");
		}
		else {
			strbuf.append("Number of Leads = "+section3.getNumberOfLeads()+"\n");
			strbuf.append((section3.getReferenceBeatUsedForCompression() ? "Reference Beat Used For Compression" : "Reference Beat Not Used For Compression")+"\n");
			strbuf.append((section3.getLeadsAllSimultaneouslyRecorded() ? "Leads All Simultaneously Recorded" : "Leads Not All Simultaneously Recorded")+"\n");
			strbuf.append("Number of Simultaneously Recorded Leads = "+section3.getNumberOfSimultaneouslyRecordedLeads()+"\n");
		}
		
		Section4 section4 = getSection4();
		if (section4 == null) {
			strbuf.append("No reference beat description\n");
		}
		else {
			strbuf.append("Length ofReference Beat 0 Data In MilliSeconds = "+section4.getLengthOfReferenceBeat0DataInMilliSeconds()+"\n");
			strbuf.append("Sample Number ofQRS of Fiducial = "+section4.getSampleNumberOfQRSOfFiducial()+"\n");
			strbuf.append("Total Number of QRS Complexes = "+section4.getTotalNumberOfQRSComplexes()+"\n");
		}
		
		Section5Or6 section5 = getSection5();
		if (section5 == null) {
			strbuf.append("No reference beat data\n");
		}
		else {
			strbuf.append("Reference beat data\n");
			strbuf.append("\tAmplitude Value Multiplier = "+section5.getAmplitudeValueMultiplier()+"\n");
			strbuf.append("\tSample Time Interval = "+section5.getSampleTimeInterval()+"\n");
			strbuf.append("\tDifference Data Used = "+section5.getDifferenceDataUsed()+"\n");
		}
		
		Section5Or6 section6 = getSection6();
		if (section6 == null) {
			strbuf.append("No rhythm or residual data\n");
		}
		else {
			strbuf.append("Rhythm or residual data\n");
			strbuf.append("\tAmplitude Value Multiplier = "+section6.getAmplitudeValueMultiplier()+"\n");
			strbuf.append("\tSample Time Interval = "+section6.getSampleTimeInterval()+"\n");
			strbuf.append("\tDifference Data Used = "+section6.getDifferenceDataUsed()+"\n");
			strbuf.append("\tBimodal Compression Used = "+section6.getBimodalCompressionUsed()+"\n");
		}
		
		return strbuf.toString();
	}

	public Section1 getSection1()      { return     (Section1)(sections.get(new Integer(1))); }
	public Section2 getSection2()      { return     (Section2)(sections.get(new Integer(2))); }
	public Section3 getSection3()      { return     (Section3)(sections.get(new Integer(3))); }
	public Section4 getSection4()      { return     (Section4)(sections.get(new Integer(4))); }
	public Section5Or6 getSection5()   { return  (Section5Or6)(sections.get(new Integer(5))); }
	public Section5Or6 getSection6()   { return  (Section5Or6)(sections.get(new Integer(6))); }
	public Section7 getSection7()      { return     (Section7)(sections.get(new Integer(7))); }
	public Section8Or11 getSection8()  { return (Section8Or11)(sections.get(new Integer(8))); }
	public Section10 getSection10()    { return    (Section10)(sections.get(new Integer(10))); }
	public Section8Or11 getSection11() { return (Section8Or11)(sections.get(new Integer(11))); }
	
	/**
	 * <p>Get the number of leads.</p>
	 *
	 * @return		the number of leads
	 */
	public int getNumberOfLeads() {
		Section3 section3 = getSection3();
		return section3 == null ? 0 : section3.getNumberOfLeads();
	}
	
	/**
	 * <p>Get the number of samples for each lead.</p>
	 *
	 * @return		an array of the number of samples for each lead
	 */
	public long[] getNumbersOfSamples() {
		Section3 section3 = getSection3();
		return section3 == null ? null : section3.getNumbersOfSamples();
	}
	
	/**
	 * <p>Get the sample time interval that will be applicable to the decompressed rhythm data.</p>
	 *
	 * <p>Note that this is the value after undecimation if the rhythm data was
	 * decimated; specifically in such cases it will be the sample time interval
	 * of the reference beat.</p>
	 *
	 * @return		the sample time interval in microSeconds
	 */
	public int getDecompressedRhythmDataSampleTimeInterval() {
		int interval=0;
		Section5Or6 section5 = getSection5();
		if (section5 != null) {
			interval=section5.getSampleTimeInterval();		// the reference beat interval will have been
										// used once the compressed rhythm data was undecimated to match it
		}
		else {
			Section5Or6 section6 = getSection6();
			if (section6 != null) {
				interval=section6.getSampleTimeInterval();	// no reference beat, therefore no decimation occurred ( ? :( )
			}
		}
		return interval;
	}
	
	/**
	 * <p>Get the concatenated values of all the occurences of a named field from Section 1 as a <code>String</code>.</p>
	 *
	 * @return		the values as a <code>String</code>
	 */
	public String getNamedField(String fieldName) {
		Section1 section1 = getSection1();
		return section1 == null ? null : section1.getConcatenatedStringValuesOfAllOccurencesOfNamedField(fieldName);
	}
	
	/**
	 * <p>Get the names of the leads from Section 3.</p>
	 *
	 * @return		the names of the leads
	 */
	public String[] getLeadNames() {
		Section3 section3 = getSection3();
		return section3 == null ? null : section3.getLeadNames();
	}
	
	/**
	 * <p>Get the numbers of the leads from Section 3.</p>
	 *
	 * @return		the numbers of the leads (using the codes in the standard and encoded in the data)
	 */
	public int[] getLeadNumbers() {
		Section3 section3 = getSection3();
		return section3 == null ? null : section3.getLeadNumbers();
	}
	
	/**
	 * <p>Get an <code>Iterator</code> over all the sections.</p>
	 *
	 * @return		an iterator of objects of type {@link com.pixelmed.scpecg.Section Section}
	 */
	public Iterator getSectionIterator() { return sections == null ? null : sections.values().iterator(); }
	
	/**
	 * <p>For testing.</p>
	 *
	 * @param	arg	one, two or three arguments, the input filename, optionally a output raw data filename, and optionally a text dump filename
	 */
	public static void main(String arg[]) {

		boolean verbose = false;
		
		try {			
			BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(arg[0])),false);	// SCP-ECG always little endian
			SCPECG scpecg = new SCPECG(i,verbose);
//System.err.print(scpecg);
			
			if (arg.length > 1) {
				short[][] data = scpecg.getDecompressedRhythmData();
				BinaryOutputStream o = new BinaryOutputStream(new BufferedOutputStream(new FileOutputStream(arg[1])),false);	// SCP-ECG always little endian
				// write interleaved raw little endian data
				int numberOfChannels = data.length;
				int nSamplesPerChannel = data[0].length;	// assume all the same
				for (int sample=0; sample<nSamplesPerChannel; ++sample) {
					for (int lead=0;lead<numberOfChannels; ++lead) {
						o.writeSigned16(data[lead][sample]);
					}
				}
				o.close();
				
				if (arg.length > 2) {
					PrintWriter p = new PrintWriter(new BufferedOutputStream(new FileOutputStream(arg[2])));
					// write signed short values non-interleaved (i.e. one lead at a time) 
					for (int lead=0;lead<numberOfChannels; ++lead) {
						p.println("Lead: "+lead);
						for (int sample=0; sample<nSamplesPerChannel; ++sample) {
							p.println("["+sample+"]="+data[lead][sample]);
						}
					}
					p.close();
				}
			}

			
			{
				ApplicationFrame af = new ApplicationFrame();
				JScrollPane scrollPane = new JScrollPane();
				SCPTreeBrowser browser = new SCPTreeBrowser(scpecg,scrollPane);
				af.getContentPane().add(scrollPane);
				af.pack();
				af.setVisible(true);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


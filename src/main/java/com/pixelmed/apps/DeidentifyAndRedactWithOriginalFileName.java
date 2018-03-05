/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;

import java.io.File;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class DeidentifyAndRedactWithOriginalFileName extends DeidentifyAndRedact {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/DeidentifyAndRedactWithOriginalFileName.java,v 1.7 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DeidentifyAndRedactWithOriginalFileName.class);

	/**
	 * <p>Make a suitable file name to use for a deidentified and redacted input file.</p>
	 *
	 * <p>Uses the inputFileName without any trailing ".dcm" suffix plus "_Anon.dcm" in the outputFolderName (ignoring the sopInstanceUID).</p>
	 *
	 * <p>Does NOT use the full hierarchy of the inputFileName, only the base file name and does NOT check whether or not the generated file name already exists,
	 * so may cause any existing or duplicate base file name to be silently overwritten.</p>
	 *
	 * <p>Overrides the default method in the parent class.</p>
	 *
	 * @param		outputFolderName	where to store all the processed output files
	 * @param		inputFileName		the path to search for DICOM files
	 * @param		sopInstanceUID		the SOP Instance UID of the output file
	 * @exception	IOException			if a filename cannot be constructed
	 */
	protected String makeOutputFileName(String outputFolderName,String inputFileName,String sopInstanceUID) throws IOException {
		// ignore sopInstanceUID
		return new File(outputFolderName,new File(inputFileName).getName().replaceFirst("[.]dcm$","")+"_Anon.dcm").getCanonicalPath();
	}

	public DeidentifyAndRedactWithOriginalFileName(String inputPathName,String outputFolderName,String redactionControlFileName,boolean decompress,boolean keepAllPrivate,boolean addContributingEquipmentSequence,AttributeList replacementAttributes) throws DicomException, Exception, IOException {
		super(inputPathName,outputFolderName,redactionControlFileName,decompress,keepAllPrivate,addContributingEquipmentSequence,replacementAttributes);
	}
	
	public DeidentifyAndRedactWithOriginalFileName(String inputPathName,String outputFolderName,String redactionControlFileName,boolean decompress,boolean keepAllPrivate,AttributeList replacementAttributes) throws DicomException, Exception, IOException {
		super(inputPathName,outputFolderName,redactionControlFileName,decompress,keepAllPrivate,replacementAttributes);
	}
	
	public DeidentifyAndRedactWithOriginalFileName(String inputPathName,String outputFolderName,String redactionControlFileName,boolean decompress,boolean keepAllPrivate,boolean addContributingEquipmentSequence) throws DicomException, Exception, IOException {
		super(inputPathName,outputFolderName,redactionControlFileName,decompress,keepAllPrivate,addContributingEquipmentSequence);
	}

	public DeidentifyAndRedactWithOriginalFileName(String inputPathName,String outputFolderName,String redactionControlFileName,boolean decompress,boolean keepAllPrivate) throws DicomException, Exception, IOException {
		super(inputPathName,outputFolderName,redactionControlFileName,decompress,keepAllPrivate);
	}

	public static void main(String arg[]) {
		try {
			boolean bad = false;
			if (arg.length >= 3) {
				AttributeList replacementAttributes = null;
				int startReplacements = 3;
				boolean decompress = false;
				boolean keepAllPrivate = false;
				boolean addContributingEquipmentSequence = true;
				if ((arg.length - startReplacements) > 0) {
					String option = arg[startReplacements].trim().toUpperCase();
					if (option.equals("DECOMPRESS")) {
						decompress = true;
						++startReplacements;
					}
					else if (option.equals("BLOCK")) {
						++startReplacements;
					}
				}
				slf4jlogger.info("main(): decompress = {}",decompress);
				if ((arg.length - startReplacements) > 0) {
					String option = arg[startReplacements].trim().toUpperCase();
					if (option.equals("KEEPALLPRIVATE")) {
						keepAllPrivate = true;
						++startReplacements;
					}
					else if (option.equals("KEEPSAFEPRIVATE")) {
						++startReplacements;
					}
				}
				slf4jlogger.info("main(): keepAllPrivate = {}",keepAllPrivate);
				if ((arg.length - startReplacements) > 0) {
					String option = arg[startReplacements].trim().toUpperCase();
					if (option.equals("ADDCONTRIBUTINGEQUIPMENT")) {
						addContributingEquipmentSequence = true;
						++startReplacements;
					}
					else if (option.equals("DONOTADDCONTRIBUTINGEQUIPMENT")) {
						addContributingEquipmentSequence = false;
						++startReplacements;
					}
				}
				slf4jlogger.info("main(): addContributingEquipmentSequence = {}",addContributingEquipmentSequence);
				if (arg.length > startReplacements) {
					if ((arg.length - startReplacements)%2  == 0) {	// replacement keyword/value pairs must be pairs
						slf4jlogger.info("main(): have replacement attributes");
						replacementAttributes = AttributeList.makeAttributeListFromKeywordAndValuePairs(arg,startReplacements,arg.length-startReplacements);
						slf4jlogger.info("main(): the replacement attributes are:\n{}",replacementAttributes);
					}
					else {
						slf4jlogger.error("Replacement keyword/value pairs must be pairs");
						bad = true;
					}
				}
				if (!bad) {
					long startTime = System.currentTimeMillis();
					new DeidentifyAndRedactWithOriginalFileName(arg[0],arg[1],arg[2],decompress,keepAllPrivate,addContributingEquipmentSequence,replacementAttributes);
					long currentTime = System.currentTimeMillis();
					slf4jlogger.info("entire set took = "+(currentTime-startTime)+" ms");
				}
			}
			else {
				slf4jlogger.error("Error: Incorrect number of arguments");
				bad = true;
			}
			if (bad) {
				slf4jlogger.error("Usage: DeidentifyAndRedactWithOriginalFileName inputPath outputFile redactionControlFile [BLOCK|DECOMPRESS] [KEEPALLPRIVATE|KEEPSAFEPRIVATE] [keyword value]*");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}

}


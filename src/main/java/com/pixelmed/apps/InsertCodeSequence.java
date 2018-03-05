/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.*;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for inserting code sequences from the command line.</p>
 *
 * @author	dclunie
 */
public class InsertCodeSequence {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/InsertCodeSequence.java,v 1.6 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(InsertCodeSequence.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	protected String attributeName;
	protected String codingSchemeDesignator;
	protected String codeValue;
	protected String codeMeaning;
	protected String codingSchemeVersion;
		
	protected class OurMediaImporter extends MediaImporter {
		public OurMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				// don't care about which SOP Class
				{
					{
						CodedSequenceItem item = null;
						if (codingSchemeVersion == null || codingSchemeVersion.trim().length() == 0) {
							item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codeMeaning);
						}
						else {
							item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codingSchemeVersion,codeMeaning);
						}
						if (item != null) {
							AttributeList itemList = item.getAttributeList();
							if (itemList != null) {
								AttributeTag tag = AttributeList.getDictionary().getTagFromName(attributeName);
								if (tag != null) {
									SequenceAttribute a = new SequenceAttribute(tag);
									a.addItem(itemList);
									list.put(a);
								}
							}
						}
					}
					
					CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
					
					// do NOT set to derived and replace UID
					// do not addContributingEquipmentSequence
													
					list.removeGroupLengthAttributes();
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
					
					String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);	// will be the same SOP Instance UID since not replaced as derived
					File dstFile = new File(dstFolderName,sopInstanceUID+".dcm");
					list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	public InsertCodeSequence(String attributeName,String codeValue,String codingSchemeDesignator,String codingSchemeVersion,String codeMeaning,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.dstFolderName = dstFolderName;
		this.attributeName = attributeName;
		this.codingSchemeDesignator = codingSchemeDesignator;
		this.codeValue = codeValue;
		this.codeMeaning = codeMeaning;
		this.codingSchemeVersion = codingSchemeVersion;
		MediaImporter importer = new OurMediaImporter(logger);
		importer.importDicomFiles(src);
	}

	/**
	 * <p>Insert a coded sequence into the specified files.</p>
	 *
	 * <p>Does not replace UIDs or set type to derived.</p>
	 *
	 * @param	arg		array of 7 strings - attributeName, codeValue, codingSchemeDesignator, codingSchemeVersion (or empty string if none), codeMeaning, source folder or DICOMDIR, destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 7) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new InsertCodeSequence(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.InsertCodeSequence attributeName codeValue codingSchemeDesignator codingSchemeVersion (or empty string if none) codeMeaning srcdir|DICOMDIR dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}

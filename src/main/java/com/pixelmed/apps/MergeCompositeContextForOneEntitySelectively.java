/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodingSchemeIdentification;
import com.pixelmed.dicom.CompositeInstanceContext;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.MoveDicomFilesIntoHierarchy;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for merging the composite context of multiple instances for consistency.</p>
 *
 * <p>The merge can be performed at a specified entity/module level or the default patient level.</p>
 *
 * <p>All source files are presumed to be of the same entity at the specified or default level (e.g., the same patient) and no cross-references between instances are required.</p>
 *
 * <p>Various known dummy values are treated as if they were zero length or absent if conflicting with non-dummy values.</p>
 *
 * @see com.pixelmed.apps.MergeCompositeContext
 *
 * @author	dclunie
 */
public class MergeCompositeContextForOneEntitySelectively {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/MergeCompositeContextForOneEntitySelectively.java,v 1.6 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MergeCompositeContextForOneEntitySelectively.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;

	protected CompositeInstanceContext.Selector selector;

	protected boolean isNonZeroLengthDummyValue(String value) {
		return value.equals("DUMMY");
	}
	
	CompositeInstanceContext mergedContext = null;
	
	// should really take steps to also merge all available PatientIDs into OtherPatientIDs, etc. ... :(
	
	protected CompositeInstanceContext mergeSelectedContext(CompositeInstanceContext newContext) {
		if (mergedContext == null) {
//System.err.println("mergeSelectedContext(): creating new context");
			mergedContext = newContext;
		}
		else {
			AttributeList mergedList = mergedContext.getAttributeList();
			Iterator<Attribute> newListIterator = newContext.getAttributeList().values().iterator();
			while (newListIterator.hasNext()) {
				Attribute a = newListIterator.next();
				AttributeTag tag = a.getTag();
				String mergedValue = Attribute.getSingleStringValueOrEmptyString(mergedList,tag);
				String newValue = a.getSingleStringValueOrEmptyString();
				if (!newValue.equals(mergedValue)) {
					String describeTag = tag + " " + mergedList.getDictionary().getFullNameFromTag(tag);
slf4jlogger.info("mergeSelectedContext(): for "+describeTag+" values differ between existing merged value <"+mergedValue+"> and new value <"+newValue+">");
					if (newValue.length() > 0 && (mergedValue.length() == 0 || isNonZeroLengthDummyValue(mergedValue))) {
slf4jlogger.info("mergeSelectedContext(): for "+describeTag+" replacing absent/empty/dummy existing merged value with new value <"+newValue+">");
						mergedList.put(a);
					}
				}
			}
		}
		return mergedContext;
	}
	
	protected class OurFirstPassMediaImporter extends MediaImporter {
		
		public OurFirstPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				CompositeInstanceContext cic = new CompositeInstanceContext(list,false/*forSR*/);
				cic.removeAllButSelected(selector);

				mergeSelectedContext(cic);
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	
	protected class OurSecondPassMediaImporter extends MediaImporter {
		public OurSecondPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				if (mergedContext != null) {
					list.putAll(mergedContext.getAttributeList());					// overwrite all selected context in list that was read in
				}
				else {
					throw new DicomException("Missing group context for SOPInstanceUID on second pass");	// should not be possible
				}
				
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
																		  "PixelMed",													// Manufacturer
																		  "PixelMed",													// Institution Name
																		  "Software Development",										// Institutional Department Name
																		  "Bangor, PA",													// Institution Address
																		  null,															// Station Name
																		  "com.pixelmed.apps.MergeCompositeContextForOneEntitySelectively",	// Manufacturer's Model Name
																		  null,															// Device Serial Number
																		  "Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
																		  "Merged patient context");
				
				CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

				list.removeGroupLengthAttributes();
				list.removeMetaInformationHeaderAttributes();
				list.remove(TagFromName.DataSetTrailingPadding);
				FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
				
				File dstFile = new File(dstFolderName,MoveDicomFilesIntoHierarchy.makeHierarchicalPathFromAttributes(list));
				if (dstFile.exists()) {
					throw new DicomException("\""+mediaFileName+"\": new file \""+dstFile+"\" already exists - not overwriting");
				}
				else {
					File dstParentDirectory = dstFile.getParentFile();
					if (!dstParentDirectory.exists()) {
						if (!dstParentDirectory.mkdirs()) {
							throw new DicomException("\""+mediaFileName+"\": parent directory creation failed for \""+dstFile+"\"");
						}
					}
					//logLn("Writing with new context file "+dstFile);
					slf4jlogger.info("Writing with new context file {}",dstFile);
					list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	/**
	 * <p>Merge the composite context of multiple instances for consistency.</p>
	 *
	 * @param	selector		selected entities/modules to merge
	 * @param	src				source folder or DICOMDIR
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeCompositeContextForOneEntitySelectively(CompositeInstanceContext.Selector selector,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.selector = selector;
//System.err.println("MergeCompositeContextForOneEntitySelectively(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		MediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		firstPassImporter.importDicomFiles(src);
		if (mergedContext != null) {
			mergedContext.removeAllButSelected(selector);	// remove anything hanging around, such as empty attributes
		}
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		secondPassImporter.importDicomFiles(src);

	}
	
	/**
	 * <p>Merge the composite context of multiple instances for consistency.</p>
	 *
	 * @param	selector		selected entities/modules to merge
	 * @param	srcs			source folders or DICOMDIRs
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeCompositeContextForOneEntitySelectively(CompositeInstanceContext.Selector selector,String[] srcs,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.selector = selector;
//System.err.println("MergeCompositeContextForOneEntitySelectively(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		OurFirstPassMediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		for (String src : srcs) {
			firstPassImporter.importDicomFiles(src);
		}
		if (mergedContext != null) {
			mergedContext.removeAllButSelected(selector);	// remove anything hanging around, such as empty attributes
		}
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		for (String src : srcs) {
			secondPassImporter.importDicomFiles(src);
		}
	}
	
	/**
	 * <p>Merge the composite context of multiple instances for consistency.</p>
	 *
	 * <p>The files are processed in the order in which they are specified on the command line, and when there
	 * is a conflict, the first values are used. This can be used to make sure that all PatientIDs, for example
	 * are coerced to the first one specified.</p>
	 *
	 * <p>For example, if a folder of images for a patient is specified first, and then a folder of
	 * structured reports or presentation states corresponding to those images but with different patient
	 * level identifiers is specified second, then the latter (the reports or presentation states) will be cooerced
	 * to have the same patient context as the former (the images).</p>
	 *
	 * <p>Entity/module arguments recognized are -patient|-study|-equipment|-frameofreference|-series|-instance|srdocumentgeneral.</p>
	 *
	 * <p>If no entity/module argument is specified, only the patient context will be merged.</p>
	 *
	 * @param	arg		array of 2 or more strings - an optional list of entities/modules to merge,
	 *                  followed by one or more source folders or DICOMDIR (to merge and use as a source of context),
	 *                  and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			ArrayList<String> arglist = new ArrayList<String>();
			CompositeInstanceContext.Selector selector = new CompositeInstanceContext.Selector(arg,arglist);
			if (arg.length == arglist.size()) {
				selector.patient = true;	// no selector arguments found
			}
			
			if (arglist.size() == 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new MergeCompositeContextForOneEntitySelectively(selector,arglist.get(0),arglist.get(1),logger);
			}
			else if (arglist.size() > 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				int nSrcs = arglist.size()-1;
				String dst = arglist.get(nSrcs);
				arglist.remove(nSrcs);
				String[] srcs = new String[nSrcs];
				srcs = arglist.toArray(srcs);
				new MergeCompositeContextForOneEntitySelectively(selector,srcs,dst,logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.MergeCompositeContextForOneEntitySelectively [-patient|-study|-equipment|-frameofreference|-series|-instance|srdocumentgeneral]* srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


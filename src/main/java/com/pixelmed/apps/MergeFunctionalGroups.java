/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodingSchemeIdentification;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.FunctionalGroupUtilities;
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
 * <p>A class containing an application for merging the functional groups of multiple instances for consistency.</p>
 *
 * <p>The merge can be performed on specified subsets of functional groups, such as those related to spatial information.</p>
 *
 * @author	dclunie
 */
public class MergeFunctionalGroups {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/MergeFunctionalGroups.java,v 1.7 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MergeFunctionalGroups.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;

	protected FunctionalGroupUtilities.Selector selector;

	AttributeList mergedDimensionsModuleList = null;
			
	protected void mergeDimensionsModule(AttributeList newList) throws DicomException {
		if (mergedDimensionsModuleList == null) {
			// flag that we have merged nothing yet, since we either take all together or nothing
			SequenceAttribute newDimensionIndexSequence = (SequenceAttribute)(newList.get(TagFromName.DimensionIndexSequence));
			if (newDimensionIndexSequence != null && newDimensionIndexSequence.getNumberOfItems() > 0) {
				// this is the important sequence ... only merge if it is present and not empty
				mergedDimensionsModuleList = new AttributeList();
				mergedDimensionsModuleList.put(newDimensionIndexSequence);
				{
					SequenceAttribute newDimensionOrganizationSequence = (SequenceAttribute)(newList.get(TagFromName.DimensionOrganizationSequence));
					if (newDimensionOrganizationSequence != null) {
						mergedDimensionsModuleList.put(newDimensionOrganizationSequence);
					}
				}
				{
					Attribute newDimensionOrganizationType = newList.get(TagFromName.DimensionOrganizationType);
					if (newDimensionOrganizationType != null) {
						mergedDimensionsModuleList.put(newDimensionOrganizationType);
					}
				}
			}
		}
	}

	protected void replaceDimensionsModuleWithMerged(AttributeList targetList) {
		if (mergedDimensionsModuleList != null) {
			targetList.putAll(mergedDimensionsModuleList);
		}
	}

	AttributeList mergedList = null;
	
	protected void mergeSelectedFunctionalGroup(SequenceAttribute mergedSharedOrPerFrameFunctionalGroupsSequence,SequenceAttribute newSharedOrPerFrameFunctionalGroupsSequence) throws DicomException {
		if (newSharedOrPerFrameFunctionalGroupsSequence.getNumberOfItems() == mergedSharedOrPerFrameFunctionalGroupsSequence.getNumberOfItems()) {
			for (int itemIndex=0; itemIndex<newSharedOrPerFrameFunctionalGroupsSequence.getNumberOfItems(); ++itemIndex) {
				AttributeList mergedFunctionalGroupsSequenceItemList = mergedSharedOrPerFrameFunctionalGroupsSequence.getItem(itemIndex).getAttributeList();
				Iterator<Attribute> newListIterator = newSharedOrPerFrameFunctionalGroupsSequence.getItem(itemIndex).getAttributeList().values().iterator();
				while (newListIterator.hasNext()) {
					SequenceAttribute newFunctionalGroupSequence = (SequenceAttribute)(newListIterator.next());
					AttributeTag functionalGroupSequenceTag = newFunctionalGroupSequence.getTag();
					SequenceAttribute mergedFunctionalGroupSequence = (SequenceAttribute)(mergedFunctionalGroupsSequenceItemList.get(functionalGroupSequenceTag));
					if (mergedFunctionalGroupSequence == null || mergedFunctionalGroupSequence.getNumberOfItems() == 0) {
						mergedFunctionalGroupsSequenceItemList.put(newFunctionalGroupSequence);
					}
					else {
						// replace individual attributes (including entire nested sequence if any contained attributes are sequences)
						AttributeList mergedFunctionalGroupSequenceContentList = mergedFunctionalGroupSequence.getItem(0).getAttributeList();
						mergedFunctionalGroupSequenceContentList.putAll(mergedFunctionalGroupSequence.getItem(0).getAttributeList());
					}
				}
			}
		}
		else {
			throw new DicomException("Different number of Per-Frame Functional Groups Sequence items");
		}
	}
			
	protected void mergeSelectedFunctionalGroups(AttributeList newList) throws DicomException {
		if (mergedList == null) {
//System.err.println("mergeSelectedFunctionalGroups(): creating new merged list");
			mergedList = newList;
		}
		else {
			{
				SequenceAttribute mergedSharedFunctionalGroupsSequence = (SequenceAttribute)mergedList.get(TagFromName.SharedFunctionalGroupsSequence);
				SequenceAttribute    newSharedFunctionalGroupsSequence = (SequenceAttribute)newList.get(TagFromName.SharedFunctionalGroupsSequence);
				if (mergedSharedFunctionalGroupsSequence == null || mergedSharedFunctionalGroupsSequence.getNumberOfItems() == 0) {
					if (newSharedFunctionalGroupsSequence != null && newSharedFunctionalGroupsSequence.getNumberOfItems() >= 1) {
						mergedList.put(newSharedFunctionalGroupsSequence);
					}
					// else leave it absent for now
				}
				else {
					if (newSharedFunctionalGroupsSequence != null && newSharedFunctionalGroupsSequence.getNumberOfItems() >= 1) {
						mergeSelectedFunctionalGroup(mergedSharedFunctionalGroupsSequence,newSharedFunctionalGroupsSequence);
					}
					// else leave it alone since nothing to merge
				}
			}
			{
				SequenceAttribute mergedPerFrameFunctionalGroupsSequence = (SequenceAttribute)mergedList.get(TagFromName.PerFrameFunctionalGroupsSequence);
				SequenceAttribute    newPerFrameFunctionalGroupsSequence = (SequenceAttribute)newList.get(TagFromName.PerFrameFunctionalGroupsSequence);
				if (mergedPerFrameFunctionalGroupsSequence == null || mergedPerFrameFunctionalGroupsSequence.getNumberOfItems() == 0) {
					if (newPerFrameFunctionalGroupsSequence != null && newPerFrameFunctionalGroupsSequence.getNumberOfItems() >= 1) {
						mergedList.put(newPerFrameFunctionalGroupsSequence);
					}
					// else leave it absent for now
				}
				else {
					if (newPerFrameFunctionalGroupsSequence != null && newPerFrameFunctionalGroupsSequence.getNumberOfItems() >= 1) {
						mergeSelectedFunctionalGroup(mergedPerFrameFunctionalGroupsSequence,newPerFrameFunctionalGroupsSequence);
					}
					// else leave it alone since nothing to merge
				}
			}
		}
	}

			
	protected void replaceFunctionalGroupsWithMerged(AttributeList targetList) {
		{
			SequenceAttribute targetSharedFunctionalGroupsSequence = (SequenceAttribute)targetList.get(TagFromName.SharedFunctionalGroupsSequence);
			SequenceAttribute mergedSharedFunctionalGroupsSequence = (SequenceAttribute)mergedList.get(TagFromName.SharedFunctionalGroupsSequence);
			if (targetSharedFunctionalGroupsSequence == null || targetSharedFunctionalGroupsSequence.getNumberOfItems() == 0) {
				if (mergedSharedFunctionalGroupsSequence != null && mergedSharedFunctionalGroupsSequence.getNumberOfItems() >= 1) {
					targetList.put(mergedSharedFunctionalGroupsSequence);
				}
				// else leave it absent for now
			}
			else {
				if (mergedSharedFunctionalGroupsSequence != null && mergedSharedFunctionalGroupsSequence.getNumberOfItems() >= 1) {
					for (int itemIndex=0; itemIndex<mergedSharedFunctionalGroupsSequence.getNumberOfItems(); ++itemIndex) {
						AttributeList targetSharedFunctionalGroupsSequenceItemList = targetSharedFunctionalGroupsSequence.getItem(itemIndex).getAttributeList();
						targetSharedFunctionalGroupsSequenceItemList.putAll(mergedSharedFunctionalGroupsSequence.getItem(itemIndex).getAttributeList());
					}
				}
				// else leave it alone since nothing to merge
			}
		}
		{
			SequenceAttribute targetPerFrameFunctionalGroupsSequence = (SequenceAttribute)targetList.get(TagFromName.PerFrameFunctionalGroupsSequence);
			SequenceAttribute mergedPerFrameFunctionalGroupsSequence = (SequenceAttribute)mergedList.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (targetPerFrameFunctionalGroupsSequence == null || targetPerFrameFunctionalGroupsSequence.getNumberOfItems() == 0) {
				if (mergedPerFrameFunctionalGroupsSequence != null && mergedPerFrameFunctionalGroupsSequence.getNumberOfItems() >= 1) {
					targetList.put(mergedPerFrameFunctionalGroupsSequence);
				}
				// else leave it absent for now
			}
			else {
				if (mergedPerFrameFunctionalGroupsSequence != null && mergedPerFrameFunctionalGroupsSequence.getNumberOfItems() >= 1) {
					for (int itemIndex=0; itemIndex<mergedPerFrameFunctionalGroupsSequence.getNumberOfItems(); ++itemIndex) {
						AttributeList targetPerFrameFunctionalGroupsSequenceItemList = targetPerFrameFunctionalGroupsSequence.getItem(itemIndex).getAttributeList();
						targetPerFrameFunctionalGroupsSequenceItemList.putAll(mergedPerFrameFunctionalGroupsSequence.getItem(itemIndex).getAttributeList());
					}
				}
				// else leave it alone since nothing to merge
			}
		}
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
				
				FunctionalGroupUtilities.removeAllButSelected(list,selector);

				if (selector.framecontent) {
					mergeDimensionsModule(list);
				}
				mergeSelectedFunctionalGroups(list);
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
				
				if (selector.framecontent) {
					replaceDimensionsModuleWithMerged(list);
				}
				replaceFunctionalGroupsWithMerged(list);
				
				ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
																		  "PixelMed",													// Manufacturer
																		  "PixelMed",													// Institution Name
																		  "Software Development",										// Institutional Department Name
																		  "Bangor, PA",													// Institution Address
																		  null,															// Station Name
																		  "com.pixelmed.apps.MergeFunctionalGroups",	// Manufacturer's Model Name
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
					//logLn("Writing with new functional groups file "+dstFile);
					slf4jlogger.info("Writing with new functional groups file {}",dstFile);
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
	 * <p>Merge the functional groups of multiple instances for consistency.</p>
	 *
	 * @param	selector		selected functional groups to merge
	 * @param	src				source folder or DICOMDIR
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeFunctionalGroups(FunctionalGroupUtilities.Selector selector,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.selector = selector;
//System.err.println("MergeFunctionalGroups(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		MediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		firstPassImporter.importDicomFiles(src);
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		secondPassImporter.importDicomFiles(src);

	}
	
	/**
	 * <p>Merge the functional groups of multiple instances for consistency.</p>
	 *
	 * @param	selector		selected functional groups to merge
	 * @param	srcs			source folders or DICOMDIRs
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 * @throws	IOException		if there is a problem reading or writing
	 * @throws	DicomException	if there is a problem parsing or extracting required content
	 */
	public MergeFunctionalGroups(FunctionalGroupUtilities.Selector selector,String[] srcs,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.selector = selector;
//System.err.println("MergeFunctionalGroups(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		OurFirstPassMediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		for (String src : srcs) {
			firstPassImporter.importDicomFiles(src);
		}
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		for (String src : srcs) {
			secondPassImporter.importDicomFiles(src);
		}
	}
	
	/**
	 * <p>Merge the functional groups of multiple instances for consistency.</p>
	 *
	 * <p>The files are processed in the order in which they are specified on the command line, and when there
	 * is a conflict, the first encountered functional group values are used.</p>
	 *
	 * <p>Functional group arguments recognized are -all|-spatial|-framecontent|-unclassified.</p>
	 *
	 * <p>If -framecontent is specified, the Dimensions Module will also be merged (the earliest encountered will be used).</p>
	 *
	 * @param	arg		array of 2 or more strings - an optional list of functional groups to merge (if absent, defaults to all),
	 *                  followed by one or more source folders or DICOMDIR (to merge and use as a source of functional groups),
	 *                  and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			ArrayList<String> arglist = new ArrayList<String>();
			FunctionalGroupUtilities.Selector selector = new FunctionalGroupUtilities.Selector(arg,arglist);
			if (arg.length == arglist.size()) {
				selector.setAll(true);	// no selector arguments found
			}
			
			if (arglist.size() == 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new MergeFunctionalGroups(selector,arglist.get(0),arglist.get(1),logger);
			}
			else if (arglist.size() > 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				int nSrcs = arglist.size()-1;
				String dst = arglist.get(nSrcs);
				arglist.remove(nSrcs);
				String[] srcs = new String[nSrcs];
				srcs = arglist.toArray(srcs);
				new MergeFunctionalGroups(selector,srcs,dst,logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.MergeFunctionalGroups [-all|-spatial|-framecontent|-unclassified]* srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


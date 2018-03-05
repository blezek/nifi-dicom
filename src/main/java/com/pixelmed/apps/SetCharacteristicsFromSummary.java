/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeFactory;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodingSchemeIdentification;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomDictionary;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for adding or replacing top level and shared multi-frame functional group attributes from a JSON summary description.</p>
 *
 * <p>The JSON file used to describe the changes is not encoded in the same format as the standard PS3.18 Annex F DICOM JSON Model,
 * since (a) it allows the data elements to be changed by keyword in addition to the data element tag, and
 * (b) it compactly specifies whether the changes are to the top level dataset ("top") or the keyword
 * of the sequence corresponding to the functional group to be changed, and
 * (c) lists attributes in the top level data set to be removed ("remove") or those to be removed recursively from within sequences ("removeall"), and
 * (d) lists options that control the process of modification.</p>
 * <p>The required format of the JSON file is a single enclosing object containing a list of objects
 * named by "remove", "options", a functional group sequence keyword or "top".</p>
 * <p>The functional group sequence keyword or "top" entries each contains either
 * a single string value,
 * an array of string values (possibly empty) (for multi-valued attributes), or
 * an array of objects (possibly empty) each of which is a sequence item consisting of a list of attributes,
 * an object that contained a list of code
 * sequence item attributes (named as cv for CodeValue, csd for CodingSchemeDesignator and cm for
 * CodeMeaning) or
 * null for an empty (type 2) attribute or sequence.</p>
 * <p>The "remove" object contains a list of keywords and null values.</p>
 * <p>The "options" object contains a list of options and boolean values. Current options are
 * ReplaceCodingSchemeIdentificationSequence (default is true) and
 * AppendToContributingEquipmentSequence (default is true)</p>
 *
 * <p>E.g.:</p>
 * <pre>
 * {
 * 	"options" : {
 * 		"AppendToContributingEquipmentSequence" : false
 * 	},
 * {
 * 	"remove" : {
 * 		"ContributingEquipmentSequence" : null
 * 	},
 * 	"removeall" : {
 * 		"FrameType" : null
 * 	},
 * 	"top" : {
 * 		"00204000" : "new value of ImageComments",
 * 		"InstitutionalDepartmentName" : "Radiology",
 * 		"ImageType" : [ "DERIVED", "PRIMARY", "DIXON", "WATER" ],
 *		"PatientBreedCodeSequence" : null,
 *		"BreedRegistrationSequence" : [
 *			{
 *				"BreedRegistrationNumber" : "1234",
 *				"BreedRegistryCodeSequence" : { "cv" : "109200", "csd" : "DCM", "cm" : "America Kennel Club" }
 *			},
 *			{
 *				"BreedRegistrationNumber" : \"5678\",
 *				"BreedRegistryCodeSequence" : { "cv" : "109202", "csd" : "DCM", "cm" : "American Canine Association" }
 *			}
 *		],
 *		"StudyID" : null,
 *		"AccessionNumber" : [],
 *		"ReferencedStudySequence" : [],
 * 		"ContentCreatorName" : "Smith^John"
 * 	},
 * 	"FrameAnatomySequence" : {
 * 		"AnatomicRegionSequence" : { "cv" : "T-A0100", "csd" : "SRT", "cm" : "Brain" },
 * 		"FrameLaterality" : "B"
 * 	},
 * 	"ParametricMapFrameTypeSequence" : {
 * 		"FrameType" : [ "DERIVED", "PRIMARY", "DIXON", "WATER" ]
 * 	},
 * 	"FrameVOILUTSequence" : {
 * 		"WindowCenter" : "0.7",
 * 		"WindowWidth" : "0.7",
 * 		"VOILUTFunction" : "LINEAR_EXACT"
 * 	}
 * }
 * </pre>
 *
 * <p>Attributes are "merged" with the existing content of a functional group sequence, if any,
 * otherwise a new functional group sequence is created.</p>
 *
 * <p>Currently only the shared functional group sequence can be updated, since non-programmatic use cases
 * for replacing the content of the per-frame functional group sequence items have not yet been identified.</p>
 *
 * @author	dclunie
 */
public class SetCharacteristicsFromSummary {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/SetCharacteristicsFromSummary.java,v 1.14 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SetCharacteristicsFromSummary.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	
	protected Map<String,Boolean> options = new HashMap<String,Boolean>();
	
	protected Set<AttributeTag> topLevelRemovalList = new HashSet<AttributeTag>();
	
	protected Set<AttributeTag> recursiveRemovalList = new HashSet<AttributeTag>();
	
	protected AttributeList topLevelReplacementsList = new AttributeList();
	
	protected Map<AttributeTag,AttributeList> functionalGroupsReplacementsList = new HashMap<AttributeTag,AttributeList>();
	
	protected DicomDictionary dictionary = topLevelReplacementsList.getDictionary();
	
	// factored out and make protected so sub-classes can use a different mechanism than the standard dictionary
	protected AttributeTag getAttributeTagFromKeywordOrGroupAndElement(String name) throws DicomException {
		AttributeTag tag = null;
		if (name.length() == 8) {
			try {
				int ggggeeee = (int)(Long.parseLong(name,16));
				tag = new AttributeTag(ggggeeee>>>16,ggggeeee&0xffff);
			}
			catch (NumberFormatException e) {
				// ignore it
			}
		}
		if (tag == null) {
			tag = dictionary.getTagFromName(name);
		}
slf4jlogger.info("SetCharacteristicsFromSummary.getAttributeTagFromKeywordOrGroupAndElement(): {}",tag);
		return tag;
	}
	
	protected Attribute makeNewAttribute(String name) throws DicomException {
		AttributeTag tag = getAttributeTagFromKeywordOrGroupAndElement(name);
		return AttributeFactory.newAttribute(tag);
	}
	
	protected Attribute parseAttributeFromJSON(JsonObject obj,String name) throws DicomException {
		Attribute a = makeNewAttribute(name);
		JsonValue entry = obj.get(name);
		JsonValue.ValueType valueType = entry.getValueType();
		if (valueType == JsonValue.ValueType.STRING) {		// single valued attribute
			String value = ((JsonString)entry).getString();
slf4jlogger.info("SetCharacteristicsFromSummary.parseAttributeFromJSON(): "+name+" : "+value);
			a.addValue(value);
		}
		else if (valueType == JsonValue.ValueType.OBJECT) {	// coded sequence item
			if (!((JsonObject)entry).isNull("cv")) {
				String codeValue = ((JsonObject)entry).getString("cv");
				String codingSchemeDesignator = ((JsonObject)entry).getString("csd");
				String codeMeaning = ((JsonObject)entry).getString("cm");
				((SequenceAttribute)a).addItem(new CodedSequenceItem(codeValue,codingSchemeDesignator,codeMeaning).getAttributeList());
			}
			// else leave newly created attribute empty (presumably is Type 2)
		}
		else if (valueType == JsonValue.ValueType.ARRAY) {	// multi valued attribute or multiple sequence items
			JsonArray arrayOfValues = (JsonArray)entry;
			for (JsonValue arrayEntry : arrayOfValues) {
				JsonValue.ValueType arrayEntryValueType = arrayEntry.getValueType();
				if (arrayEntryValueType == JsonValue.ValueType.STRING) {
					String value = ((JsonString)arrayEntry).getString();
					a.addValue(value);
				}
				else if (arrayEntryValueType == JsonValue.ValueType.OBJECT) {	// sequence item
					AttributeList itemList = new AttributeList();
					parseAttributesFromJSON((JsonObject)arrayEntry,itemList);	// recursive, so may be nested
					processAttributeListAfterReplacements(itemList);			// in case sub-class needs to add private creator(s)
					((SequenceAttribute)a).addItem(itemList);
				}
			}
		}
slf4jlogger.info("SetCharacteristicsFromSummary.parseAttributeFromJSON(): {}",a);
		return a;
	}
	
	protected void parseAttributesFromJSON(JsonObject functionalGroupEntries,AttributeList list) throws DicomException {
		for (String name : functionalGroupEntries.keySet()) {
			Attribute a = parseAttributeFromJSON(functionalGroupEntries,name);
			list.put(a);
		}
	}
	
	protected void parseAttributeTagsFromJSON(JsonObject entries,Set<AttributeTag> tags) throws DicomException {
		for (String name : entries.keySet()) {
			AttributeTag t = getAttributeTagFromKeywordOrGroupAndElement(name);
			tags.add(t);
		}
	}
	
	protected void parseOptionsFromJSON(JsonObject entries) throws DicomException {
//System.err.println("SetCharacteristicsFromSummary.parseOptionsFromJSON():");
		for (String name : entries.keySet()) {
			JsonValue entry = entries.get(name);
			JsonValue.ValueType valueType = entry.getValueType();
			boolean optionBooleanValue = false;
			if (valueType == JsonValue.ValueType.TRUE) {
				optionBooleanValue = true;
			}
			else if (valueType == JsonValue.ValueType.FALSE) {
				optionBooleanValue = false;
			}
			else {
				throw new DicomException("Unexpected valueType "+valueType+" in options for "+name);
			}
slf4jlogger.info("SetCharacteristicsFromSummary.parseOptionsFromJSON(): "+name+" : "+optionBooleanValue);
			options.put(name,new Boolean(optionBooleanValue));
		}
	}
	
	protected void parseSummaryFile(String jsonfile) throws DicomException, FileNotFoundException {
		JsonReader jsonReader = Json.createReader(new FileReader(jsonfile));
		JsonObject obj = jsonReader.readObject();
		
		for (String functionalGroupName : obj.keySet()) {
			JsonObject functionalGroupEntries = (JsonObject)(obj.get(functionalGroupName));
			if (functionalGroupName.equals("options")) {
				parseOptionsFromJSON(functionalGroupEntries);		// sets global options
			}
			else if (functionalGroupName.equals("remove")) {
				parseAttributeTagsFromJSON(functionalGroupEntries,topLevelRemovalList);
			}
			else if (functionalGroupName.equals("removeall")) {
				parseAttributeTagsFromJSON(functionalGroupEntries,recursiveRemovalList);
			}
			else if (functionalGroupName.equals("top")) {
				parseAttributesFromJSON(functionalGroupEntries,topLevelReplacementsList);
			}
			else {
				AttributeTag functionalGroupTag = dictionary.getTagFromName(functionalGroupName);
				AttributeList list = functionalGroupsReplacementsList.get(functionalGroupTag);
				if (list == null) {
					list = new AttributeList();
					functionalGroupsReplacementsList.put(functionalGroupTag,list);
				}
				parseAttributesFromJSON(functionalGroupEntries,list);
			}
		}
		jsonReader.close();
	}
	
	// in case sub classes want to do stuff, e.g., add private creators
	protected void processAttributeListAfterReplacements(AttributeList list) throws DicomException {
	}

	protected class OurMediaImporter extends MediaImporter {
		public OurMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected boolean isOKToImport(String sopClassUID,String transferSyntaxUID) {
			return sopClassUID != null
				&& transferSyntaxUID != null;
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.setDecompressPixelData(false);
				list.read(i);
				i.close();
				
				String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
				
				for (AttributeTag tag : topLevelRemovalList) {
					list.remove(tag);
				}
				
				for (AttributeTag tag : recursiveRemovalList) {
					list.removeRecursively(tag);
				}
				
				list.putAll(topLevelReplacementsList);
				
				if (functionalGroupsReplacementsList.size() > 0) {
					AttributeList sharedList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,TagFromName.SharedFunctionalGroupsSequence);
					if (sharedList == null) {
						sharedList = new AttributeList();
						SequenceAttribute a = new SequenceAttribute(TagFromName.SharedFunctionalGroupsSequence);
						a.addItem(sharedList);
						list.put(a);
					}
					for (AttributeTag functionalGroupSequenceTag : functionalGroupsReplacementsList.keySet()) {
						SequenceAttribute functionalGroupSequenceAttribute = (SequenceAttribute)(sharedList.get(functionalGroupSequenceTag));
						if (functionalGroupSequenceAttribute == null) {
							functionalGroupSequenceAttribute = new SequenceAttribute(functionalGroupSequenceTag);
							sharedList.put(functionalGroupSequenceAttribute);
						}
						AttributeList functionalGroupSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(functionalGroupSequenceAttribute);
						if (functionalGroupSequenceList == null) {
							functionalGroupSequenceList = new AttributeList();
							functionalGroupSequenceAttribute.addItem(functionalGroupSequenceList);
						}
						functionalGroupSequenceList.putAll(functionalGroupsReplacementsList.get(functionalGroupSequenceTag));
					}
				}
				
				{
					Boolean appendToContributingEquipmentSequence = options.get("AppendToContributingEquipmentSequence");
					if (appendToContributingEquipmentSequence == null	// default is true
					 || appendToContributingEquipmentSequence.booleanValue()) {
slf4jlogger.info("SetCharacteristicsFromSummary.OurMediaImporterdoSomethingWithDicomFileOnMedia():  calling addContributingEquipmentSequence()");
						ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
																		  "PixelMed",													// Manufacturer
																		  "PixelMed",													// Institution Name
																		  "Software Development",										// Institutional Department Name
																		  "Bangor, PA",													// Institution Address
																		  null,															// Station Name
																		  "com.pixelmed.apps.SetCharacteristicsFromSummary",			// Manufacturer's Model Name
																		  null,															// Device Serial Number
																		  "Vers. "+VersionAndConstants.getBuildDate(),					// Software Version(s)
																		  "Set characteristics from summary");
					}
					// else does NOT remove any exist ContributingEquipmentSequence ... use explicit remove in JSON if necessary
				}
				
				{
					Boolean replaceCodingSchemeIdentificationSequence = options.get("ReplaceCodingSchemeIdentificationSequence");
					if (replaceCodingSchemeIdentificationSequence == null	// default is true
					 || replaceCodingSchemeIdentificationSequence.booleanValue()) {
slf4jlogger.info("SetCharacteristicsFromSummary.OurMediaImporterdoSomethingWithDicomFileOnMedia():  calling replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList()");
						CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
					}
					// else does NOT remove any exist CodingSchemeIdentificationSequence ... use explicit remove in JSON if necessary
				}
				
				processAttributeListAfterReplacements(list);
				
				list.removeGroupLengthAttributes();
				list.removeMetaInformationHeaderAttributes();
				list.remove(TagFromName.DataSetTrailingPadding);
				FileMetaInformation.addFileMetaInformation(list,transferSyntaxUID,ourAETitle);
				
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
					//logLn("Writing with characteristics set file "+dstFile);
					slf4jlogger.info("Writing with characteristics set file {}",dstFile);
					list.write(dstFile,transferSyntaxUID,true,true);
				}
			}
			catch (Exception e) {
				//logLn("Error: File "+mediaFileName+" exception "+e);
				slf4jlogger.error("File {}",mediaFileName,e);
			}
		}
	}
	
	/**
	 * <p>Update top level and shared multi-frame functional group attributes from a JSON summary description.</p>
	 *
	 * @param	jsonfile		JSON file describing the functional groups and attributes and values to be added or replaced
	 * @param	src				source folder or DICOMDIR
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 */
	public SetCharacteristicsFromSummary(String jsonfile,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
//System.err.println("SetCharacteristicsFromSummary(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		parseSummaryFile(jsonfile);
		MediaImporter importer = new OurMediaImporter(logger);
		importer.importDicomFiles(src);

	}
	
	/**
	 * <p>Update top level and shared multi-frame functional group attributes from a JSON summary description.</p>
	 *
	 * @param	jsonfile		JSON file describing the functional groups and attributes and values to be added or replaced
	 * @param	srcs			source folders or DICOMDIRs
	 * @param	dstFolderName	destination folder
	 * @param	logger			logger to send progress, warnings and errors
	 */
	public SetCharacteristicsFromSummary(String jsonfile,String[] srcs,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
//System.err.println("SetCharacteristicsFromSummary(): dstFolderName = "+dstFolderName);
		this.dstFolderName = dstFolderName;
		parseSummaryFile(jsonfile);
		MediaImporter importer = new OurMediaImporter(logger);
		for (String src : srcs) {
			importer.importDicomFiles(src);
		}
	}
	
	/**
	 * <p>Update top level and shared multi-frame functional group attributes from a JSON summary description.</p>
	 *
	 * @param	arg		array of three or more strings - a JSON file describing the functional groups and attributes and values to be added or replaced,
	 *                  followed by one or more source folders or DICOMDIR,
	 *                  and a destination folder
	 */
	public static void main(String arg[]) {
		try {
			String jsonfile = arg[0];
			String dst = arg[arg.length-1];
			if (arg.length == 3) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new SetCharacteristicsFromSummary(jsonfile,arg[1],dst,logger);
			}
			else if (arg.length > 3) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				int nSrcs = arg.length-2;
				String[] srcs = new String[nSrcs];
				System.arraycopy(arg,1,srcs,0,nSrcs);
				new SetCharacteristicsFromSummary(jsonfile,srcs,dst,logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.SetCharacteristicsFromSummary jsonfile srcdir|DICOMDIR [srcdir|DICOMDIR]* dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}


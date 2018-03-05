/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.pixelmed.dicom.*;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class containing an application for inserting Real World Value Maps from the command line.</p>
 *
 * @author	dclunie
 */
public class InsertRealWorldValueMap {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/InsertRealWorldValueMap.java,v 1.8 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DoseReporterWithLegacyOCRAndAutoSendToRegistry.class);
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	protected String firstValueMapped;
	protected String lastValueMapped;
	protected String intercept;
	protected String slope;
	protected String explanation;
	protected String label;
	protected String unitsCodingSchemeDesignator;
	protected String unitsCodeValue;
	protected String unitsCodeMeaning;
	protected String unitsCodingSchemeVersion;
	protected List<ContentItem> quantityDefinitionContentItems;
			
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
				// don't care about which SOP Class per se
				{
					AttributeList listToAddRealWorldValueMappingSequenceTo = list;
					{
						SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
						if (aSharedFunctionalGroupsSequence != null) {
							// assert aSharedFunctionalGroupsSequence.getNumberOfItems() == 1
							Iterator sitems = aSharedFunctionalGroupsSequence.iterator();
							if (sitems.hasNext()) {
								SequenceItem sitem = (SequenceItem)sitems.next();
								AttributeList slist = sitem.getAttributeList();
								if (slist != null) {
									listToAddRealWorldValueMappingSequenceTo = slist;
								}
							}
						}
					}
				
					// reuse (add to) existing sequence if present
					SequenceAttribute aRealWorldValueMappingSequence = (SequenceAttribute)(list.get(TagFromName.RealWorldValueMappingSequence));
					if (aRealWorldValueMappingSequence == null) {
						aRealWorldValueMappingSequence = new SequenceAttribute(TagFromName.RealWorldValueMappingSequence);
						listToAddRealWorldValueMappingSequenceTo.put(aRealWorldValueMappingSequence);
					}
					
					AttributeList rwvmList = new AttributeList();
					aRealWorldValueMappingSequence.addItem(rwvmList);
					
					// should we be checking the value that are supplied to see if they are signed, or using the signedness of the list that we are adding to ?
					
					boolean signed = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0) == 1;
					if (signed) {
						{ Attribute a = new SignedShortAttribute(TagFromName.RealWorldValueFirstValueMapped); a.addValue(firstValueMapped); rwvmList.put(a); }
						{ Attribute a = new SignedShortAttribute(TagFromName.RealWorldValueLastValueMapped ); a.addValue(lastValueMapped ); rwvmList.put(a); }
					}
					else {
						{ Attribute a = new SignedShortAttribute(TagFromName.RealWorldValueFirstValueMapped); a.addValue(firstValueMapped); rwvmList.put(a); }
						{ Attribute a = new SignedShortAttribute(TagFromName.RealWorldValueLastValueMapped ); a.addValue(lastValueMapped ); rwvmList.put(a); }
					}

					{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueIntercept); a.addValue(intercept); rwvmList.put(a); }
					{ Attribute a = new FloatDoubleAttribute(TagFromName.RealWorldValueSlope); a.addValue(slope); rwvmList.put(a); }
					
					{ Attribute a = new LongStringAttribute(TagFromName.LUTExplanation); a.addValue(explanation); rwvmList.put(a); }
					{ Attribute a = new ShortStringAttribute(TagFromName.LUTLabel); a.addValue(label); rwvmList.put(a); }
				
					{
						CodedSequenceItem item = null;
						if (unitsCodingSchemeVersion == null || unitsCodingSchemeVersion.trim().length() == 0) {
							item = new CodedSequenceItem(unitsCodeValue,unitsCodingSchemeDesignator,unitsCodeMeaning);
						}
						else {
							item = new CodedSequenceItem(unitsCodeValue,unitsCodingSchemeDesignator,unitsCodingSchemeVersion,unitsCodeMeaning);
						}
						if (item != null) {
							AttributeList itemList = item.getAttributeList();
							if (itemList != null) {
								{ SequenceAttribute a = new SequenceAttribute(TagFromName.MeasurementUnitsCodeSequence); a.addItem(itemList); rwvmList.put(a); }
							}
						}
					}
					
					if (quantityDefinitionContentItems != null && !quantityDefinitionContentItems.isEmpty()) {
						SequenceAttribute aQuantityDefinitionSequence = new SequenceAttribute(TagFromName.QuantityDefinitionSequence);
						rwvmList.put(aQuantityDefinitionSequence);
						for (ContentItem ci : quantityDefinitionContentItems) {
							aQuantityDefinitionSequence.addItem(ci.getAttributeList());
						
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
	
	public InsertRealWorldValueMap(String firstValueMapped,String lastValueMapped,String intercept,String slope,String explanation,String label,String unitsCodeValue,String unitsCodingSchemeDesignator,String unitsCodingSchemeVersion,String unitsCodeMeaning,List<ContentItem> quantityDefinitionContentItems,String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.dstFolderName = dstFolderName;
		this.firstValueMapped = firstValueMapped;
		this.lastValueMapped = lastValueMapped;
		this.intercept = intercept;
		this.slope = slope;
		this.explanation = explanation;
		this.label = label;
		this.unitsCodingSchemeDesignator = unitsCodingSchemeDesignator;
		this.unitsCodeValue = unitsCodeValue;
		this.unitsCodeMeaning = unitsCodeMeaning;
		this.unitsCodingSchemeVersion = unitsCodingSchemeVersion;
		this.quantityDefinitionContentItems = quantityDefinitionContentItems;
		MediaImporter importer = new OurMediaImporter(logger);
		importer.importDicomFiles(src);
	}
	
	private static ContentItemFactory contentItemFactory = new ContentItemFactory();

	private static List<ContentItem> extractContentItems(String arg[],int start,int end) throws DicomException {
		slf4jlogger.info("extractContentItems(): starting arg.length = {}, start = {}, end = {}",arg.length,start,end);
		List<ContentItem> listOfContentItems = new ArrayList<ContentItem>();
		while (end > start) {
			int remaining = end - start + 1;
			slf4jlogger.info("extractContentItems(): remaining = {}",remaining);
			String valueType = arg[start];
			CodedSequenceItem concept = new CodedSequenceItem(arg[start+1]);
			ContentItem contentItem = null;
			if (valueType.equals("CODE") && remaining >= 3) {
				CodedSequenceItem value = new CodedSequenceItem(arg[start+2]);
				contentItem = contentItemFactory.makeCodeContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if ((valueType.equals("NUM") || valueType.equals("NUMERIC")) && remaining >= 4) {	// NUM is used in SR trees, NUMERIC is used in Acquisition and Protocol templates (PS 3.3 10-2 Content Item Macro).
				String value = arg[start+2];
				CodedSequenceItem units = new CodedSequenceItem(arg[start+3]);
				contentItem = contentItemFactory.makeNumericContentItem(null/*parent*/,true/*isNotSR*/,null/*relationshipType*/,concept,value,units,null/*qualifier*/);
				start += 4;
			}
			else if (valueType.equals("DATETIME")) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makeDateTimeContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if (valueType.equals("DATE")) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makeDateContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if (valueType.equals("TIME")) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makeTimeContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if (valueType.equals("PNAME")) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makePersonNameContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if (valueType.equals("UIDREF")) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makeUIDContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else if (valueType.equals("TEXT") && remaining >= 3) {
				String value = arg[start+2];
				contentItem = contentItemFactory.makeTextContentItem(null/*parent*/,null/*relationshipType*/,concept,value);
				start += 3;
			}
			else {
				throw new DicomException("Unrecognized value type in content item argument");
			}
			slf4jlogger.info("extractContentItems(): contentItem = {}",contentItem);
			if (contentItem != null) {
				listOfContentItems.add(contentItem);
			}
		}
		return listOfContentItems;
	}

	/**
	 * <p>Insert a Real World Value Map into the specified files.</p>
	 *
	 * <p>Does not replace UIDs or set type to derived.</p>
	 *
	 * @param	arg		array of 12 or more strings - first value mapped, last value mapped, intercept, slope, explanation, label, units codeValue, units codingSchemeDesignator, units codingSchemeVersion (or empty string if none), units codeMeaning, zero or more quantity definition content items, source folder or DICOMDIR, destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length >= 12) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				String src = arg[arg.length-2];
				String dstFolderName = arg[arg.length-1];
				List<ContentItem> quantityDefinitionContentItems = extractContentItems(arg,10,arg.length-3);	// will be empty if none, i.e., arg.length-3 < 10
				new InsertRealWorldValueMap(arg[0],arg[1],arg[2],arg[3],arg[4],arg[5],arg[6],arg[7],arg[8],arg[9],quantityDefinitionContentItems,src,dstFolderName,logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.InsertRealWorldValueMap firstValueMapped lastValueMapped intercept slope explanation label unitsCodeValue unitsCodingSchemeDesignator unitsCodingSchemeVersion (or empty string if none) unitsCodeMeaning [CODE|NUMERIC|DATE|TIME|DATETIME|PNAME|UIDREF|TEXT (cv,csd,cm) = value|(cv,csd,cm)]* srcdir|DICOMDIR dstdir");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(0);
		}
	}
}

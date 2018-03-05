/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.TagFromName;

import java.util.HashMap;

/**
 * <p>This class contains utility methods provide for the detection of concepts in various header attributes regardless
 * of whether these are formal codes, code strings or free text comments.</p>
 * 
 * @author	dclunie
 */
public class DictionaryOfConcepts {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/DictionaryOfConcepts.java,v 1.12 2017/01/24 10:50:32 dclunie Exp $";

	protected DisplayableConcept[] conceptEntries;
	protected HashMap schemeAndValuePairsToConceptEntries;
	protected HashMap meaningsAndSynonymsToConceptEntries;
	protected HashMap codeStringEquivalentToConceptEntries;
	protected HashMap<String,DisplayableConcept> conceptUniqueIdentifierToConceptEntries = new HashMap<String,DisplayableConcept>();
	protected HashMap<String,DisplayableConcept> conceptIdentifierToConceptEntries = new HashMap<String,DisplayableConcept>();
	protected String[] badWords;
	protected String descriptionOfConcept;
		
	protected void doCommonConstructorStuff(DisplayableConcept[] conceptEntries,String[] badWords,String descriptionOfConcept) {
		this.conceptEntries=conceptEntries;
		this.badWords=badWords;
		this.descriptionOfConcept=descriptionOfConcept;

		schemeAndValuePairsToConceptEntries = new HashMap();
		meaningsAndSynonymsToConceptEntries = new HashMap();
		codeStringEquivalentToConceptEntries = new HashMap();

		for (DisplayableConcept concept : conceptEntries) {
			{
				String codeValue = concept.getCodeValue();
				String codingSchemeDesignator = concept.getCodingSchemeDesignator();
				SchemeAndValuePair key = new SchemeAndValuePair(codeValue,codingSchemeDesignator);
				schemeAndValuePairsToConceptEntries.put(key,concept);
				String legacyCodingSchemeDesignator = concept.getLegacyCodingSchemeDesignator();
				if (legacyCodingSchemeDesignator != null) {
					key = new SchemeAndValuePair(codeValue,legacyCodingSchemeDesignator);
					schemeAndValuePairsToConceptEntries.put(key,concept);
				}
			}
			{
				String codeMeaning = concept.getCodeMeaning();
				String key = codeMeaning.toLowerCase(java.util.Locale.US);
				meaningsAndSynonymsToConceptEntries.put(key,concept);
			}
			{
				String codeStringEquivalent = concept.getCodeStringEquivalent();
				if (codeStringEquivalent != null) {
					codeStringEquivalentToConceptEntries.put(codeStringEquivalent/* NOT lower case; want exact match*/,concept);
					String key = codeStringEquivalent.toLowerCase(java.util.Locale.US);
					meaningsAndSynonymsToConceptEntries.put(key,concept);
				}
			}
			{
				String[] synonyms = concept.getSynonyms();
				if (synonyms != null) {
					for (String synonym : synonyms) {
						String key = synonym.toLowerCase(java.util.Locale.US);
						meaningsAndSynonymsToConceptEntries.put(key,concept);
					}
				}
			}
			{
				String key = concept.getConceptUniqueIdentifier();
				conceptUniqueIdentifierToConceptEntries.put(key,concept);
			}
			{
				String key = concept.getConceptIdentifier();
				conceptIdentifierToConceptEntries.put(key,concept);
			}
		}
	}
	
	public DictionaryOfConcepts(DisplayableConcept[] conceptEntries) {
		doCommonConstructorStuff(conceptEntries,null,null);
	}
	
	public DictionaryOfConcepts(DisplayableConcept[] conceptEntries,String[] badWords,String descriptionOfConcept) {
		doCommonConstructorStuff(conceptEntries,badWords,descriptionOfConcept);
	}
	
	public String getDescriptionOfConcept() {
		return descriptionOfConcept == null ? "" : descriptionOfConcept;
	}
		
	public DisplayableConcept find(SchemeAndValuePair key) {
		return (DisplayableConcept)(schemeAndValuePairsToConceptEntries.get(key));
	}

	public DisplayableConcept find(CodedSequenceItem item) {
		return find(new SchemeAndValuePair(item));
	}
		
	public DisplayableConcept find(String key) {
		return (DisplayableConcept)(meaningsAndSynonymsToConceptEntries.get(key.toLowerCase(java.util.Locale.US)));
	}
		
	public DisplayableConcept find(Concept key) {
		return (DisplayableConcept)(conceptUniqueIdentifierToConceptEntries.get(key.getConceptUniqueIdentifier()));
	}
		
	public DisplayableConcept findByConceptUniqueIdentifier(String key) {
		return (DisplayableConcept)(conceptUniqueIdentifierToConceptEntries.get(key));
	}
		
	public DisplayableConcept findByConceptIdentifier(String key) {
		return (DisplayableConcept)(conceptIdentifierToConceptEntries.get(key));
	}
		
	public DisplayableConcept findCodeStringExact(String key) {
		return (DisplayableConcept)(codeStringEquivalentToConceptEntries.get(key));
	}
		

	protected String removeAnyBadWords(String string) {
		if (badWords != null) {
			for (String badWord : badWords) {
					string = string.replaceAll(badWord.toLowerCase(java.util.Locale.US)," ");	// replace word with space, not null, to avoid concatenating inadvertantly
			}
			string = string.replaceAll("[ ][ ]*"," ").trim().replaceAll("^[ ]$","");		// may have inserted spaces, so replace all newly created runs of spaces with single space, trim leading and trailing white space, and replace entirely space string with zero length
		}
		return string;
	}
	
	// http://java.sun.com/mailers/techtips/corejava/2007/tt0207.html
	// http://glaforge.free.fr/weblog/index.php?itemid=115
	// http://www.rgagnon.com/javadetails/java-0456.html
	
	protected static String removeAccentsFromLowerCaseString(String s) {
		//try {
		//	return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll( "\\p{InCombiningDiacriticalMarks}+", "" );		// compiles only with 1.6 :(
		//}
		//catch (NoSuchMethodError e) {
			//try {
			//	return sun.text.Normalizer.decompose(s,false,0).replaceAll( "\\p{InCombiningDiacriticalMarks}+", "");
			//	return sun.text.Normalizer.normalize(s,sun.text.Normalizer.DECOMP,0).replaceAll( "\\p{InCombiningDiacriticalMarks}+", "");		// compiles only with 1.5 :(
			//}
			//catch (NoSuchMethodError e2) {
				// assume already lower case
				s = s.replaceAll("[àáãäåāąăâ]","a");
				s = s.replaceAll("[æ]","ae");
				s = s.replaceAll("[çćĉċ]","c");
				s = s.replaceAll("[ďđ]","d");
				s = s.replaceAll("[èéêëēęěĕė]","e");
				s = s.replaceAll("[ƒ]","f");
				s = s.replaceAll("[ĝğġģ]","g");
				s = s.replaceAll("[ĥħ]","h");
				s = s.replaceAll("[ìíîïīĩĭįı]","i");
				s = s.replaceAll("[ĳ]","ij");
				s = s.replaceAll("[ĵ]","j");
				s = s.replaceAll("[ĸ]","k");
				s = s.replaceAll("[łľĺļŀ]","l");
				s = s.replaceAll("[ñńňņŉŋ]","n");				
				s = s.replaceAll("[òóôõöøōőŏ]","o");				
				s = s.replaceAll("[œ]","oe");				
				s = s.replaceAll("[ŕřŗ]","r");				
				s = s.replaceAll("[śšşŝș]","s");				
				s = s.replaceAll("[ťţŧț]","t");				
				s = s.replaceAll("[ùúûüūůűŭũų]","u");				
				s = s.replaceAll("[ŵ]","w");				
				s = s.replaceAll("[ýÿŷ]","y");				
				s = s.replaceAll("[žżź]","z");				
				s = s.replaceAll("[ß]","ss");				
				return s;
			//}
		//}
	}

	protected DisplayableConcept findLongestIndividualEntryContainedWithin(String keyText) {
		DisplayableConcept entry = null;
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): keyText = "+keyText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): keyText = \n"+com.pixelmed.utils.HexDump.dump(keyText));
//try { System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): keyText = \n"+com.pixelmed.utils.HexDump.dump(keyText.findBytes("UTF8"))); } catch (java.io.UnsupportedEncodingException e) {}
		String cleanedText = keyText.toLowerCase(java.util.Locale.US);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText as lowercase = "+cleanedText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText lowercase = \n"+com.pixelmed.utils.HexDump.dump(cleanedText));
		cleanedText = removeAccentsFromLowerCaseString(cleanedText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText without accents = "+cleanedText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText without accents = \n"+com.pixelmed.utils.HexDump.dump(cleanedText));
		cleanedText = cleanedText.replaceAll("[^\\p{L}\\d]"," ").replaceAll("[ ][ ]*"," ").trim().replaceAll("^[ ]$","");	// replace all non-letters and non-digits and runs of spaces with single space, trim leading and trailing white space, and replace entirely space string with zero length
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText after punctuation removal and space collapse = "+cleanedText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText punctuation removal and space collapse = \n"+com.pixelmed.utils.HexDump.dump(cleanedText));
		cleanedText = removeAnyBadWords(cleanedText);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): cleanedText punctuation removal and space collapse and bad word removal = "+cleanedText);
		if (cleanedText.length() > 0) {
			// linear search ... :(
			int lengthFound=0;
			for (DisplayableConcept concept : conceptEntries) {
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): trying concept = "+concept);
				String codeMeaning = concept.getCodeMeaning().toLowerCase(java.util.Locale.US);
				if (cleanedText.contains(codeMeaning)) {
					int tryLength = codeMeaning.length();
					if (tryLength > lengthFound) {
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): candidate from codeMeaning= "+codeMeaning);
						entry = concept;
						lengthFound = tryLength;
					}
				}
				// do NOT automatically check codeStringEquivalent contained with in string ... only use these as exact match; if appropriate, include explicitly as synonyms
				//String codeStringEquivalent = concept.getCodeStringEquivalent();
				//if (codeStringEquivalent != null) {
				//	codeStringEquivalent=codeStringEquivalent.toLowerCase(java.util.Locale.US);
				//	codeStringEquivalent = removeAccentsFromLowerCaseString(codeStringEquivalent);
				//	if (cleanedText.contains(codeStringEquivalent)) {
				//		int tryLength = codeStringEquivalent.length();
				//		if (tryLength > lengthFound) {
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): candidate from codeStringEquivalent = "+codeStringEquivalent);
				//			entry = concept;
				//			lengthFound = tryLength;
				//		}
				//	}
				//}
				String[] synonyms = concept.getSynonyms();
				if (synonyms != null) {
					for (String synonym : synonyms) {
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): trying synonym = "+synonym);
						synonym = synonym.toLowerCase(java.util.Locale.US);
						synonym = removeAccentsFromLowerCaseString(synonym);
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): trying synonym without accents = "+synonym);
						if (cleanedText.contains(synonym)) {
							int tryLength = synonym.length();
							if (tryLength > lengthFound) {
//System.err.println("DictionaryOfConcepts.findLongestIndividualEntryContainedWithin(): candidate from synonyms = "+synonym);
								entry = concept;
								lengthFound = tryLength;
							}
						}
					}
				}
			}
		}
		return entry;
	}
	
	protected DisplayableConcept findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(String key) {
		DisplayableConcept entry = find(key);
		if (entry == null) {
			entry = findLongestIndividualEntryContainedWithin(key);
		}
		return entry;
	}


	protected  DisplayableConcept findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(CodedSequenceItem item) {
		DisplayableConcept entry = find(item);
		if (entry == null) {
			String key = item.getCodeMeaning();
			if (key != null) {
				entry = findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(key);
			}
		}
		return entry;
	}

}


	

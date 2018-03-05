/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

/**
 * <p>This class represents view position concepts that may be encoded and displayed.</p>
 * 
 * @author	dclunie
 */
public class DisplayableViewConcept extends DisplayableConcept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/DisplayableViewConcept.java,v 1.6 2017/01/24 10:50:33 dclunie Exp $";
	
	/**
	 * <p>Create a view position concept that may be encoded and displayed.</p>
	 *
	 * @param	conceptUniqueIdentifier			the unique identifier of the concept, usually a UMLS CUI; required to be unique within the scope of comparisons using {@link #equals(Object) equals(Object)}
	 * @param	conceptIdentifier				the scheme-specific concept identifier, e.g., for SNOMED-CT, the SNOMED Concept Identifier
	 * @param	codingSchemeDesignator			the DICOM PS3.16 Section 8 coding scheme used as the DICOM Coding Scheme Designator, e.g., "SRT", "DCM", "LN", or a private coding scheme
	 * @param	legacyCodingSchemeDesignator	a legacy (alternative) coding scheme, e.g.  "SNM3" if what is used in DICOM context groups instead of "SRT"; null if none required (i.e., treat the same as codingSchemeDesignator)
	 * @param	codingSchemeVersion				the version of the coding scheme in which this code is defined, if necessary; null if none required
	 * @param	codeValue						the code used as the DICOM Code Value (e.g., the SNOMED-RT style code rather than the SNOMED-CT style Concept Identifier)
	 * @param	codeMeaning						the text used as the DICOM Code Meaning
	 * @param	codeStringEquivalent			the text value used for a DICOM Code String VR equivalent attribute (e.g., for Body Part Examined instead of in Anatomic Region Sequence); may be null
	 * @param	synonynms						alternative code meanings, including abbreviations or different languages; may be null or empty
	 * @param	shortcutMenuEntry				an array of text values to use in a shortcut menu entry, e.g. "Wrist"
	 * @param	fullyQualifiedMenuEntry			an array of text values to use in a full menu entry, e.g. "Limb","Upper","Wrist"
	 */
	public DisplayableViewConcept(String conceptUniqueIdentifier,String conceptIdentifier,
			String codingSchemeDesignator,String legacyCodingSchemeDesignator,String codingSchemeVersion,String codeValue,String codeMeaning,String codeStringEquivalent,String[] synonynms,
			String[] shortcutMenuEntry,String[] fullyQualifiedMenuEntry
			) {
		super(conceptUniqueIdentifier,conceptIdentifier,codingSchemeDesignator,legacyCodingSchemeDesignator,codingSchemeVersion,codeValue,codeMeaning,codeStringEquivalent,synonynms,shortcutMenuEntry,fullyQualifiedMenuEntry);
	}
	
	protected DisplayableViewConcept() {};

}


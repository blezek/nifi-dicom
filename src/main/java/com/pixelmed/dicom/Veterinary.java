/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class of static methods for handling veterinary (animal) data.</p>
 *
 * @author	dclunie
 */
abstract public class Veterinary {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/Veterinary.java,v 1.5 2017/01/24 10:50:39 dclunie Exp $";

	public static boolean isPatientAnAnimal(AttributeList list) {
		boolean isAnimal = false;
		CodedSequenceItem iPatientSpeciesCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.PatientSpeciesCodeSequence);
		if (iPatientSpeciesCodeSequence == null) {
			String vPatientSpeciesDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.PatientSpeciesDescription);
			if (vPatientSpeciesDescription == null
			 || vPatientSpeciesDescription.trim().length() == 0
			 || vPatientSpeciesDescription.toLowerCase(java.util.Locale.US).contains("homo sapien")
			 || vPatientSpeciesDescription.toLowerCase(java.util.Locale.US).contains("human")) {
				isAnimal = false;
			}
			else {
				isAnimal = true;
			}
		}
		else {
			String codeValue = iPatientSpeciesCodeSequence.getCodeValue();
			String codingSchemeDesignator = iPatientSpeciesCodeSequence.getCodingSchemeDesignator();
			String codeMeaning = iPatientSpeciesCodeSequence.getCodeMeaning();
			if (codeValue != null && codingSchemeDesignator != null) {
				if ((codingSchemeDesignator.equals("SRT") || codingSchemeDesignator.equals("SNM3")) && codeValue.equals("L-85B00")) {
					isAnimal = false;
				}
				else if (codeMeaning != null && (codeMeaning.toLowerCase(java.util.Locale.US).contains("homo sapien") || codeMeaning.toLowerCase(java.util.Locale.US).contains("human"))) {
					isAnimal = false;
				}
				else {
					isAnimal = true;
				}
			}
		}
		return isAnimal;
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

/**
 * <p>A class to provide support for the Person Identification Macro.</p>
 *
 * @author	dclunie
 */
public class PersonIdentification {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/PersonIdentification.java,v 1.5 2017/01/24 10:50:38 dclunie Exp $";

	protected AttributeList list;
	
	protected CodedSequenceItem[] personIdentificationCodeSequence;
	protected String personAddress;
	protected String[] personTelephoneNumbers;
	protected String institutionName;
	protected String institutionAddress;
	protected CodedSequenceItem institutionCodeSequence;

	/**
	 * <p>Extract the contents of a Person Identification Macro from a list of attributes.</p>
	 *
	 * <p>Non-standard attributes are discarded.</p>
	 *
	 * @param	list			the list of attributes that comprise the item
	 * @throws	DicomException	if the list of attributes does not contain the required information
	 */
	public PersonIdentification(AttributeList list) throws DicomException {
		personIdentificationCodeSequence = CodedSequenceItem.getArrayOfCodedSequenceItemsOrNull(list,TagFromName.PersonIdentificationCodeSequence);
		                   personAddress = Attribute.getSingleStringValueOrNull(list,TagFromName.PersonAddress);
		          personTelephoneNumbers = Attribute.getStringValues(list,TagFromName.PersonTelephoneNumbers);
		                 institutionName = Attribute.getSingleStringValueOrNull(list,TagFromName.InstitutionName);
		              institutionAddress = Attribute.getSingleStringValueOrNull(list,TagFromName.InstitutionAddress);
				institutionCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.InstitutionCodeSequence);
		getAttributeList();		// checks for compliance of supplied values
	}

	/**
	 * <p>Construct the contents of a Person Identification Macro.</p>
	 *
	 * @param	personIdentificationCodeSequence
	 * @param	personAddress
	 * @param	personTelephoneNumbers
	 * @param	institutionName
	 * @param	institutionAddress
	 * @param	institutionCodeSequence
	 * @throws	DicomException	if the the required information is not present
	 */
	public PersonIdentification(CodedSequenceItem[] personIdentificationCodeSequence,String personAddress,String[] personTelephoneNumbers,
			String institutionName,String institutionAddress,CodedSequenceItem institutionCodeSequence) throws DicomException {
		this.personIdentificationCodeSequence = personIdentificationCodeSequence;
		this.personAddress = personAddress;
		this.personTelephoneNumbers = personTelephoneNumbers;
		this.institutionName = institutionName;
		this.institutionAddress = institutionAddress;
		this.institutionCodeSequence = institutionCodeSequence;

		if (personIdentificationCodeSequence == null || personIdentificationCodeSequence.length < 1) {
			throw new DicomException("Person Identification Code Sequence is Type 1 but information not supplied");
		}
		if ((institutionName == null || institutionName.length() == 0) && institutionCodeSequence == null) {
				throw new DicomException("One of either InstitutionName or institutionCodeSequence are required");
		}
	}
	
	/**
	 * <p>Get the list of attributes for a Person Identification Macro.</p>
	 *
	 * @return	the attribute list
	 */
	public AttributeList getAttributeList() throws DicomException {
		if (list == null) {
			list = new AttributeList();
			if (personIdentificationCodeSequence == null || personIdentificationCodeSequence.length < 1) {
				throw new DicomException("Person Identification Code Sequence is Type 1 but information not supplied");
			}
			else {
				SequenceAttribute aPersonIdentificationCodeSequence = new SequenceAttribute(TagFromName.PersonIdentificationCodeSequence);
				for (int i=0; i<personIdentificationCodeSequence.length; ++i) {
					CodedSequenceItem item = personIdentificationCodeSequence[i];
					if (item != null) {
						aPersonIdentificationCodeSequence.addItem(item.getAttributeList());
					}
				}
				list.put(aPersonIdentificationCodeSequence);
			}
			if (personAddress != null && personAddress.length() > 0) {
				ShortTextAttribute aPersonAddress = new ShortTextAttribute(TagFromName.PersonAddress);
				aPersonAddress.addValue(personAddress);
				list.put(aPersonAddress);
			}
			if (personTelephoneNumbers != null && personTelephoneNumbers.length > 0) {
				LongStringAttribute aPersonTelephoneNumbers = new LongStringAttribute(TagFromName.PersonTelephoneNumbers);
				for (int i=0; i<personTelephoneNumbers.length; ++i) {
					String number = personTelephoneNumbers[i];
					if (number != null) {
						aPersonTelephoneNumbers.addValue(number);
					}
				}
				list.put(aPersonTelephoneNumbers);
			}
			if (institutionName != null && institutionName.length() > 0) {
				LongStringAttribute aInstitutionName = new LongStringAttribute(TagFromName.InstitutionName);
				aInstitutionName.addValue(institutionName);
				list.put(aInstitutionName);
			}
			else if (institutionCodeSequence == null) {
				throw new DicomException("One of either InstitutionName or institutionCodeSequence are required");
			}
			if (institutionAddress != null && institutionAddress.length() > 0) {
				ShortTextAttribute aInstitutionAddress = new ShortTextAttribute(TagFromName.InstitutionAddress);
				aInstitutionAddress.addValue(institutionAddress);
				list.put(aInstitutionAddress);
			}
			if (institutionCodeSequence != null) {
				SequenceAttribute aInstitutionCodeSequence = new SequenceAttribute(TagFromName.InstitutionCodeSequence);
				aInstitutionCodeSequence.addItem(institutionCodeSequence.getAttributeList());
				list.put(aInstitutionCodeSequence);
			}
		}
		return list;
	}

}

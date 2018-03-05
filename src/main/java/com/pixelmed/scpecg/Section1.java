/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate the SCP-ECG Patient and ECG Acquisition Data section.</p>
 *
 * @author	dclunie
 */
public class Section1 extends Section {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/Section1.java,v 1.13 2017/01/24 10:50:47 dclunie Exp $";

	/**
	 * <p>Get a string name for this section.</p>
	 *
	 * @return		a string name for this section
	 */
	public String getSectionName() { return "Patient and ECG Acquisition Data"; }
	
	private BinaryInputStream i;
	
	private class FieldDictionaryEntry {
		String name;
		int tag;
		int requirement;	// 0 = optional, 1 = required, 2 = recommended
		int multiplicity;	// 1 = once, else more than once
		int maximumLength;
		int reasonableLength;
		String className;	// Text, Binary, Date, Time
		
		FieldDictionaryEntry(String name,int tag,int requirement,int multiplicity,int maximumLength,int reasonableLength,String className) {
			this.name=name;
			this.tag=tag;
			this.requirement=requirement;
			this.multiplicity=multiplicity;
			this.maximumLength=maximumLength;
			this.reasonableLength=reasonableLength;
			this.className=className;
		}

		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("\tTag "+tag+" dec (0x"+Integer.toHexString(tag)+")");
			strbuf.append("\tName "+name);
			return strbuf.toString();
		}
		
	}
	
	private FieldDictionaryEntry[] dictionary = {
			new FieldDictionaryEntry("LastName",  				0,	2, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("FirstName", 				1,	2, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("PatientIdentificationNumber",		2,	1, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("SecondLastName",			3,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("Age",					4,	0, 1,   3,  3, "Age"),
			new FieldDictionaryEntry("DateOfBirth",				5,	0, 1,   4,  4, "Date"),
			new FieldDictionaryEntry("Height",				6,	0, 1,   3,  3, "Height"),
			new FieldDictionaryEntry("Weight",				7,	0, 1,   3,  3, "Weight"),
			new FieldDictionaryEntry("Sex",					8,	0, 1,   1,  1, "Sex"),
			new FieldDictionaryEntry("Race",				9,	0, 1,   1,  1, "Race"),
			new FieldDictionaryEntry("Drugs", 				10,	0, 99, 64, 40, "Drug"),
			new FieldDictionaryEntry("SystolicBloodPressure",		11,	0, 1,   2,  2, "Binary"),
			new FieldDictionaryEntry("DiastolicBloodPressure",		12,	0, 1,   2,  2, "Binary"),
			new FieldDictionaryEntry("DiagnosisOrReferralIndication", 	13,	0, 99, 80, 80, "Text"),
			new FieldDictionaryEntry("AcquiringDeviceIdentificationNumber", 14,	1, 1,  64, 40, "MachineID"),
			new FieldDictionaryEntry("AnalyzingDeviceIdentificationNumber", 15,	2, 1,  64, 40, "MachineID"),
			new FieldDictionaryEntry("AcquiringInstitutionDescription", 	16,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("AnalyzingInstitutionDescription", 	17,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("AcquiringDepartmentDescription", 	18,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("AnalyzingDepartmentDescription", 	19,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("ReferringPhysician", 			20,	0, 1,  64, 60, "Text"),
			new FieldDictionaryEntry("LatestConfirmingPhysician", 		21,	0, 1,  64, 60, "Text"),
			new FieldDictionaryEntry("TechnicianDescription", 		22,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("RoomDescription", 			23,	0, 1,  64, 40, "Text"),
			new FieldDictionaryEntry("StatCode",				24,	0, 1,   1,  1, "StatCode"),
			new FieldDictionaryEntry("DateOfAcquisition",			25,	0, 1,   4,  4, "Date"),
			new FieldDictionaryEntry("TimeOfAcquisition",			26,	0, 1,   3,  3, "Time"),
			new FieldDictionaryEntry("BaselineFilter",			27,	0, 1,   2,  2, "Binary"),
			new FieldDictionaryEntry("LowPassFilter",			28,	0, 1,   2,  2, "Binary"),
			new FieldDictionaryEntry("FilterBitmap",			29,	0, 1,   1,  1, "FilterBitmap"),
			new FieldDictionaryEntry("FreeTextField", 			30,	0, 99, 80, 80, "Text"),
			new FieldDictionaryEntry("ECGSequenceNumber", 			31,	0, 1,  64, 12, "Text"),
			new FieldDictionaryEntry("MedicalHistoryCodes", 		32,	0, 1,  64, 12, "MedicalHistory"),
			new FieldDictionaryEntry("ElectrodeConfigurationCode", 		33,	0, 1,   2,  2, "Electrode"),
			new FieldDictionaryEntry("DateTimeZone", 			34,	0, 1,  64, 40, "TimeZone"),
			new FieldDictionaryEntry("FreeTextMedicalHistory", 		35,	0, 99, 80, 80, "Text"),
		};
		
	private HashMap dictionaryFieldByName;			// map of String names to dictionary entries
	private FieldDictionaryEntry[] dictionaryFieldByTag;	// (mostly sparse) array of 256 possible tags
	
	private void loadDictionary() {
//System.err.println("Section1.loadDictionary()");
		dictionaryFieldByName = new HashMap();
		dictionaryFieldByTag = new FieldDictionaryEntry[256];
		for (int i=0; i<dictionary.length; ++i) {
			dictionaryFieldByName.put(dictionary[i].name,dictionary[i]);
			dictionaryFieldByTag[dictionary[i].tag]=dictionary[i];
		}
//dumpDictionary();
	}
	
	private void dumpDictionary() {
		for (int i=0; i<dictionary.length; ++i) {
			System.err.println(dictionary[i]);
		}
		for (int i=0; i<256; ++i) {
			System.err.println("["+i+"] "+getDictionaryFieldByTag(i));
		}
	}
	
	private FieldDictionaryEntry getDictionaryFieldByName(String name) {
		return (FieldDictionaryEntry)(dictionaryFieldByName.get(name));
	}
	
	private FieldDictionaryEntry getDictionaryFieldByTag(int tag) {
		return dictionaryFieldByTag[tag];
	}

	private String getName(int tag) {
		FieldDictionaryEntry entry = getDictionaryFieldByTag(tag);
//System.err.println("Found entry "+entry);
		return entry == null ? null : entry.name;
	}

	private String getClassName(int tag) {
		FieldDictionaryEntry entry = getDictionaryFieldByTag(tag);
//System.err.println("Found entry "+entry);
		return entry == null ? null : entry.className;
	}

	private int getMultiplicity(int tag) {
		FieldDictionaryEntry entry = getDictionaryFieldByTag(tag);
//System.err.println("Found entry "+entry);
		return entry == null ? 1 : entry.multiplicity;
	}

	private String describeFieldBriefly(FieldDictionaryEntry entry) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append(entry.tag+" dec ");
		strbuf.append(entry.name);
		return strbuf.toString();
	}

	private String describeFieldBriefly(int tag) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Tag ");
		strbuf.append(tag);
		//strbuf.append(" dec ");
		strbuf.append(" ");
		strbuf.append(getName(tag));
		return strbuf.toString();
	}
	
	private class Field {
		int tag;
		int length;
		byte[] value;
		
		Field() {
		}
		
		Field(int tag,int length) {
			this.tag=tag;
			this.length=length;
		}
		
		void read() throws IOException {
			if (length > 0) {
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		boolean isTerminator() {
			return tag == 255;
		}
		
		String validate() {
//System.err.println("Field.validate(): validating tag "+tag);
			StringBuffer strbuf = new StringBuffer();
			
			if (isTerminator() && length != 0) {
				strbuf.append("Terminator should be zero length but has length "+length+" dec (0x"+Integer.toHexString(length)+")\n");
			}
			
			FieldDictionaryEntry entry = getDictionaryFieldByTag(tag);
			
			if (entry == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized tag");
				strbuf.append("\n");
			}
			else {
				int valueLength = getValueLengthForPurposeOfValidation();
//System.err.println("Field.validate(): valueLength "+valueLength);
				if (valueLength > entry.maximumLength) {
					strbuf.append(describeFieldBriefly(entry));
					strbuf.append(": Length of ");
					strbuf.append(valueLength);
					strbuf.append(" exceeds maximum length of ");
					strbuf.append(entry.maximumLength);
					strbuf.append("\n");
				}
				else if (valueLength > entry.reasonableLength) {
					strbuf.append(describeFieldBriefly(entry));
					strbuf.append(": Length of ");
					strbuf.append(valueLength);
					strbuf.append(" exceeds reasonable length of ");
					strbuf.append(entry.reasonableLength);
					strbuf.append("\n");
				}
			}

			return strbuf.toString();
		}
		
		protected int getValueLengthForPurposeOfValidation() {
			return value == null ? 0 : value.length;
		}
		
		public String toStringBrief() {
			return describeFieldBriefly(tag);
		}

		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			//strbuf.append("Tag "+tag+" dec (0x"+Integer.toHexString(tag)+") of length "+length+" dec (0x"+Integer.toHexString(length)+")");
			strbuf.append(describeFieldBriefly(tag));
			strbuf.append(" Length=");
			strbuf.append(length);
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			return value == null ? "" : makeStringFromByteArrayRemovingAnyNulls(value);
		}
	}
	
	private class TextField extends Field {
		TextField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			int valueLength = value == null ? 0 : value.length;
			if (valueLength != 0 && value[valueLength-1] == 0) {
				--valueLength;	// do not include the trailing null terminator in length
			}
			return valueLength;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(value == null ? "" : new String(value));
			strbuf.append(">");
			return strbuf.toString();
		}
	}
	
	
	private class TimeZoneField extends Field {
		int offset;
		int index;
		
		TimeZoneField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			int remaining = length;
			if (remaining >= 2) {
				offset=i.readSigned16();		// NB. signed
				bytesRead+=2;
				sectionBytesRemaining-=2;
				remaining-=2;
			}
			if (remaining >= 2) {
				index=i.readUnsigned16();
				bytesRead+=2;
				sectionBytesRemaining-=2;
				remaining-=2;
			}
			if (remaining > 0) {
				value = new byte[remaining];
				i.readInsistently(value,0,remaining);
				sectionBytesRemaining-=remaining;
				bytesRead+=remaining;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (offset != 0x7fff && (offset < -780 || offset > 780)) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": if specified (not 0x7fff), timezone offset must be +/- 780 minutes (13 hours), got "+offset+" dec (0x"+Integer.toHexString(offset&0xffff)+")");
				strbuf.append("\n");
			}
			if (offset != 0x7fff && index != 0) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": index must not be used if timezone offset is used (not 0x7fff), got index of "+index+" dec");
				strbuf.append("\n");
			}
			if (offset != 0x7fff && value != null && value.length != 0) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": timezone description must not be used if timezone offset is used (not 0x7fff)");
				strbuf.append("\n");
			}
			if (value != null && value.length == 1 && value[0] == 0) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": timezone description is present but undefined (null terminator only)");
				strbuf.append("\n");
			}
			// should validate value to be Posix timezone string if present
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			int valueLength = value == null ? 0 : value.length;
			if (valueLength != 0 && value[valueLength-1] == 0) {
				--valueLength;	// do not include the trailing null terminator in length
			}
			return valueLength;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("offset=");
			strbuf.append(offset);
			strbuf.append(", index=");
			strbuf.append(index);
			strbuf.append(", <");
			strbuf.append(value == null ? "" : new String(value));
			strbuf.append(">");
			return strbuf.toString();
		}
	}
	
	private static String[] ageUnitsDescription = { "Unspecified", "Years", "Months", "Weeks", "Days", "Hours" };
	private static String[] heightUnitsDescription = { "Unspecified", "Centimeters", "Inches", "Millimeters" };
	private static String[] weightUnitsDescription = { "Unspecified", "Kilogram", "Gram", "Pound", "Ounce" };
	
	private class ValueWithUnitsField extends Field {
		int bvalue;
		int units;
		String unitDescriptors[];

		ValueWithUnitsField(int tag, int length,String unitDescriptors[]) {
			this.tag=tag;
			this.length=length;
			this.unitDescriptors=unitDescriptors;
		}

		void read() throws IOException {
			if (length == 3) {
				bvalue=i.readUnsigned16();
				bytesRead+=2;
				sectionBytesRemaining-=2;

				units=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (units >= unitDescriptors.length || unitDescriptors[units] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized units "+units+" dec");
				strbuf.append("\n");
			}
			if (units == 0 && bvalue != 0) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": units are unspecified, but value is not zero ("+bvalue+" dec)");
				strbuf.append("\n");
			}

			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(bvalue);
			strbuf.append(" ");
			strbuf.append(units < unitDescriptors.length ? unitDescriptors[units] : "Unrecognized");
			return strbuf.toString();
		}
	}

	private static String[] sexDescriptors = { "Not Known", "Male", "Female", null, null, null, null, null, null, "Unspecified" };
	private static String[] raceDescriptors = { "Unspecified", "Caucasian", "Black", "Oriental" };
	private static String[] statCodeDescriptors = { "Routine", 
		"Emergency 1", "Emergency 2", "Emergency 3", "Emergency 4", "Emergency 5", "Emergency 6", "Emergency 7", "Emergency 8", "Emergency 9", "Emergency 10" };
	
	private class SingleCodedValueField extends Field {
		int code;
		String descriptors[];

		SingleCodedValueField(int tag, int length,String descriptors[]) {
			this.tag=tag;
			this.length=length;
			this.descriptors=descriptors;
		}

		void read() throws IOException {
			if (length == 1) {
				code=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (code >= descriptors.length || descriptors[code] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized code value "+code+" dec");
				strbuf.append("\n");
			}
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append("> ");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(code);
			strbuf.append(" [");
			strbuf.append(code < descriptors.length ? descriptors[code] : "Unrecognized");
			strbuf.append("]");
			return strbuf.toString();
		}
	}
	

	private static String[] electrodePlacement12LeadDescriptors = {
		"Unspecified", "Standard", "Mason-Likar Individual", "Mason-Likar One Pad", "All One Pad", "Derived from Frank XYZ", "Non-standard" };
	
	private static String[] electrodePlacementXYZLeadDescriptors = {
		"Unspecified", "Frank", "McFee-Parungao", "Cube", "Bipolar uncorrected", "Pseudo-orthogonal", "Derived from Standard 12-Lead" };
	
	private class TwinCodedValueField extends Field {
		int code1;
		int code2;
		String descriptors1[];
		String descriptors2[];

		TwinCodedValueField(int tag, int length,String descriptors1[],String descriptors2[]) {
			this.tag=tag;
			this.length=length;
			this.descriptors1=descriptors1;
			this.descriptors2=descriptors2;
		}

		void read() throws IOException {
			if (length == 2) {
				code1=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;

				code2=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (code1 >= descriptors1.length || descriptors1[code1] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized first code value "+code1+" dec");
				strbuf.append("\n");
			}
			if (code2 >= descriptors2.length || descriptors2[code2] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized second code value "+code2+" dec");
				strbuf.append("\n");
			}
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append("> ");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(code1);
			strbuf.append(" [");
			strbuf.append(code1 < descriptors1.length ? descriptors1[code1] : "Unrecognized");
			strbuf.append("], ");
			strbuf.append(code2);
			strbuf.append(" [");
			strbuf.append(code2 < descriptors2.length ? descriptors2[code2] : "Unrecognized");
			strbuf.append("]");
			return strbuf.toString();
		}
	}
	
	
	private static String[] filterDescriptors = { "60 Hz Notch", "50 Hz Notch", "Artifact", "Baseline" };

	private class BitmapField extends Field {
		int code;
		String descriptors[];

		BitmapField(int tag, int length,String descriptors[]) {
			this.tag=tag;
			this.length=length;
			this.descriptors=descriptors;
		}

		void read() throws IOException {
			if (length == 1) {
				code=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append("> ");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(" 0x");
			strbuf.append(Integer.toHexString(code));
			strbuf.append(" ");
			for (int bit=0; bit<8; ++bit) {
				strbuf.append("Bit ");
				strbuf.append(bit);
				if (bit < descriptors.length && descriptors[bit] != null) {
					strbuf.append(" (");
					strbuf.append(descriptors[bit]);
					strbuf.append(")");
				}
				strbuf.append(":");
				int bitValue = (code>>bit)&0x01;
				strbuf.append(bitValue);
				strbuf.append(" ");
			}
			return strbuf.toString();
		}
	}
	
	private class DateField extends Field {
		int yyyy;
		int mm;
		int dd;

		DateField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			if (length == 4) {
				yyyy=i.readUnsigned16();
				bytesRead+=2;
				sectionBytesRemaining-=2;

				mm=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;

				dd=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (dd != 0 || mm != 0 || yyyy != 0) {		// else is unspecified, which is OK
				if (mm < 1 || mm > 12) {
					strbuf.append(describeFieldBriefly(tag));
					strbuf.append(": month out of range "+mm+" dec");
					strbuf.append("\n");
				}
				if (dd < 1 || dd > 31) {
					strbuf.append(describeFieldBriefly(tag));
					strbuf.append(": day out of range "+dd+" dec");
					strbuf.append("\n");
				}
			}

			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(yyyy);
			strbuf.append("/");
			strbuf.append(mm);
			strbuf.append("/");
			strbuf.append(dd);
			strbuf.append(">");
			strbuf.append((dd != 0 || mm != 0 || yyyy != 0) ? "" : " Unspecified");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(yyyy);
			strbuf.append("/");
			strbuf.append(mm);
			strbuf.append("/");
			strbuf.append(dd);
			return strbuf.toString();
		}
	}

	
	private class TimeField extends Field {
		int hh;
		int mm;
		int ss;

		TimeField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			if (length == 3) {
				hh=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;

				mm=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;

				ss=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			if (hh < 0 || hh > 23) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": hours out of range "+hh+" dec");
				strbuf.append("\n");
			}
			if (mm < 0 || mm > 59) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": minutes out of range "+mm+" dec");
				strbuf.append("\n");
			}
			if (ss < 0 || ss > 59) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": seconds out of range "+ss+" dec");
				strbuf.append("\n");
			}

			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(hh);
			strbuf.append(":");
			strbuf.append(mm);
			strbuf.append(":");
			strbuf.append(ss);
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(hh);
			strbuf.append(":");
			strbuf.append(mm);
			strbuf.append(":");
			strbuf.append(ss);
			return strbuf.toString();
		}
	}

	
	private class BinaryField extends Field {
		int bvalue;

		BinaryField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			if (length == 2) {
				bvalue=i.readUnsigned16();
				bytesRead+=2;
				sectionBytesRemaining-=2;
			}
			else if (length > 0) {				// ? should throw exception
				value = new byte[length];
				i.readInsistently(value,0,length);
				sectionBytesRemaining-=length;
				bytesRead+=length;
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(bvalue);
			return strbuf.toString();
		}
	}

	private class DrugField extends Field {
		int drugClass;
		int drugCode;
		int textLength;
		/// value from super() is used for text description

		DrugField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			int remaining = length;
			if (remaining >= 1) {
				drugClass=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
			}
			if (remaining >= 1) {
				drugCode=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
			}
			if (remaining >= 1) {
				textLength=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
			}
			if (remaining > 0) {
				value = new byte[remaining];
				i.readInsistently(value,0,remaining);
				sectionBytesRemaining-=remaining;
				bytesRead+=remaining;
//com.pixelmed.utils.HexDump.dump(value);
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());

			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(drugClass);
			strbuf.append(",");
			strbuf.append(drugCode);
			strbuf.append(",");
			strbuf.append(textLength);
			strbuf.append(",");
			strbuf.append(value == null ? "" : makeStringFromByteArrayRemovingAnyNulls(value));
			return strbuf.toString();
		}
	}


	private class MedicalHistoryField extends Field {
		int codeTable;
		/// value from super() is used for the 1-n codes themselves

		MedicalHistoryField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			int remaining = length;
			if (remaining >= 1) {
				codeTable=i.readUnsigned8();
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
			}
			if (remaining > 0) {
				value = new byte[remaining];
				i.readInsistently(value,0,remaining);
				sectionBytesRemaining-=remaining;
				bytesRead+=remaining;
//com.pixelmed.utils.HexDump.dump(value);
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());

			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return length;
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <codeTable=");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(codeTable);
			for (int c=0; c<value.length; ++c) {
				strbuf.append(",");
				strbuf.append(value[c]);
			}
			return strbuf.toString();
		}
	}

	private static String[] deviceTypeDescriptors = { "Cart", "System (or Host)" };
	private static String[] manufacturerCodeDescriptors = {
			"Unknown",
			"Burdick",
			"Cambridge",
			"Compumed",
			"Datamed",
			"Fukuda",	// 5
			"Hewlett-Packard",
			"Marquette Electronics",
			"Mortara Instruments",
			"Nihon Kohden",
			"Okin",		// 10
			"Quinton",
			"Siemens",
			"Spacelabs",
			"Telemed",
			"Hellige",	// 15
			"ESA-OTE",
			"Schiller",
			"Picker-Schwarzer",
			"Elettronica-Trentina",
			"Zwönitz",	// 20
		};
	private static String[] mainsFrequencyDescriptors = { "Unspecified", "50 Hz", "60 Hz" };

	private class MachineIDField extends Field {
		int institutionNumber;
		int departmentNumber;
		int deviceID;
		int deviceType;
		int manufacturerCode;
		byte[] modelDescription;	// 6 bytes
		int protocolRevisionLevel;
		int protocolCompatibilityLevel;
		int languageSupportCode;
		int capabilitiesCode;
		int mainsFrequency;
		byte[] reserved;		// 16 bytes
		int analysingProgramRevisionNumberLength;
		byte[] analysingProgramRevisionNumber;
		// value from super() is used for text description

		MachineIDField(int tag, int length) {
			this.tag=tag;
			this.length=length;
		}

		void read() throws IOException {
			int remaining = length;
			if (remaining >= 36) {		// variable length part for analysingProgramRevisionNumber starts after 0-35 bytes
				institutionNumber=i.readUnsigned16();		// 1-2
				bytesRead+=2;
				sectionBytesRemaining-=2;
				remaining-=2;

				departmentNumber=i.readUnsigned16();		// 3-4
				bytesRead+=2;
				sectionBytesRemaining-=2;
				remaining-=2;
				
				deviceID=i.readUnsigned16();			// 5-6
				bytesRead+=2;
				sectionBytesRemaining-=2;
				remaining-=2;

				deviceType=i.readUnsigned8();			// 7
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;

				manufacturerCode=i.readUnsigned8();		// 8
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
				
				modelDescription = new byte[6];			// 9-14		5 chars + null terminator
				i.readInsistently(modelDescription,0,6);
				sectionBytesRemaining-=6;
				bytesRead+=6;
				remaining-=6;

				protocolRevisionLevel=i.readUnsigned8();	// 15
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;

				protocolCompatibilityLevel=i.readUnsigned8();	// 16
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;

				languageSupportCode=i.readUnsigned8();		// 17
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
				
				capabilitiesCode=i.readUnsigned8();		// 18
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
				
				mainsFrequency=i.readUnsigned8();		// 19
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;

				reserved = new byte[16];			// 20-35
				i.readInsistently(reserved,0,16);
				sectionBytesRemaining-=16;
				bytesRead+=16;
				remaining-=16;
				
				analysingProgramRevisionNumberLength=i.readUnsigned8();		// 36
				bytesRead++;
				sectionBytesRemaining--;
				remaining--;
				
				if (analysingProgramRevisionNumberLength >= 1) {
					analysingProgramRevisionNumber = new byte[analysingProgramRevisionNumberLength];
					i.readInsistently(analysingProgramRevisionNumber,0,analysingProgramRevisionNumberLength);
					sectionBytesRemaining-=analysingProgramRevisionNumberLength;
					bytesRead+=analysingProgramRevisionNumberLength;
					remaining-=analysingProgramRevisionNumberLength;
				}

			}
			if (remaining > 0) {
				value = new byte[remaining];
				i.readInsistently(value,0,remaining);
				sectionBytesRemaining-=remaining;
				bytesRead+=remaining;
//com.pixelmed.utils.HexDump.dump(value);
			}
		}
		
		String validate() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.validate());

			if (deviceType >= deviceTypeDescriptors.length || deviceTypeDescriptors[deviceType] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized Device Type value "+deviceType+" dec");
				strbuf.append("\n");
			}
			
			if (manufacturerCode != 255) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Use of legacy manufacturer code value, expected 255 dec, got "+deviceType+" dec");
				strbuf.append("\n");
				if (manufacturerCode != 100) {	// 100 is assigned to Other
					if (manufacturerCode >= manufacturerCodeDescriptors.length || manufacturerCodeDescriptors[manufacturerCode] == null) {
						strbuf.append(describeFieldBriefly(tag));
						strbuf.append(": Unrecognized Device Type value "+deviceType+" dec");
						strbuf.append("\n");
					}
				}
			}

			if (mainsFrequency >= mainsFrequencyDescriptors.length || mainsFrequencyDescriptors[mainsFrequency] == null) {
				strbuf.append(describeFieldBriefly(tag));
				strbuf.append(": Unrecognized Mains Frequency value "+deviceType+" dec");
				strbuf.append("\n");
			}
			
			{
				int highNibble = (protocolCompatibilityLevel>>4)&0x000f;
				if (highNibble < 9 || highNibble > 12) {
					strbuf.append(describeFieldBriefly(tag));
					strbuf.append(": Unrecognized Protocol Compatibility Level 0x"+Integer.toHexString(protocolCompatibilityLevel));
					strbuf.append("\n");
				}
			}
			
			return strbuf.toString();
		}

		protected int getValueLengthForPurposeOfValidation() {
			return analysingProgramRevisionNumberLength;		// should subtract count of any null terminators within or at the end :(
		}
		
		public String toString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append(super.toString());
			strbuf.append(" <");
			strbuf.append(getValueAsString());
			strbuf.append(">");
			return strbuf.toString();
		}
		
		public String getValueAsString() {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("institutionNumber=");
			strbuf.append(institutionNumber);
			strbuf.append(", departmentNumber=");
			strbuf.append(departmentNumber);
			strbuf.append(", deviceID=");
			strbuf.append(deviceID);
			strbuf.append(", deviceType=");
			strbuf.append(deviceType);
			strbuf.append(", manufacturerCode=");
			strbuf.append(manufacturerCode);
			strbuf.append(", modelDescription=");
			strbuf.append(modelDescription == null ? "" : new String(modelDescription));
			strbuf.append(", protocolRevisionLevel=");
			strbuf.append(protocolRevisionLevel);
			strbuf.append(", protocolCompatibilityLevel=");
			strbuf.append(Integer.toHexString(protocolCompatibilityLevel));
			strbuf.append(", languageSupportCode=0x");
			strbuf.append(Integer.toHexString(languageSupportCode));
			strbuf.append(", capabilitiesCode=0x");
			strbuf.append(Integer.toHexString(capabilitiesCode));
			strbuf.append(", mainsFrequency=");
			strbuf.append(mainsFrequency);
			strbuf.append(", analysingProgramRevisionNumberLength=");
			strbuf.append(analysingProgramRevisionNumberLength);
			strbuf.append(", analysingProgramRevisionNumber=");
			strbuf.append(analysingProgramRevisionNumber == null ? "" : new String(analysingProgramRevisionNumber));
			return strbuf.toString();
		}
	}

	private ArrayList[] fields;		// (mostly sparse) array of 256 possible ArrayLists of Field, indexed by tag

	public Section1(SectionHeader header) {
		super(header);
		loadDictionary();
		fields=new ArrayList[256];
	}

	public long read(BinaryInputStream i) throws IOException {
		this.i=i;

		boolean seenTerminator = false;
		while (sectionBytesRemaining > 0) {
			int tag=i.readUnsigned8();
			bytesRead++;
			sectionBytesRemaining--;

			int length=i.readUnsigned16();
			bytesRead+=2;
			sectionBytesRemaining-=2;

			Field field = null;
			String className = getClassName(tag);
			if (className != null) {
				if (className.equals("Text")) {
					field = new TextField(tag,length);
				}
				else if (className.equals("Age")) {
					field = new ValueWithUnitsField(tag,length,ageUnitsDescription);
				}
				else if (className.equals("Date")) {
					field = new DateField(tag,length);
				}
				else if (className.equals("Time")) {
					field = new TimeField(tag,length);
				}
				else if (className.equals("Height")) {
					field = new ValueWithUnitsField(tag,length,heightUnitsDescription);
				}
				else if (className.equals("Weight")) {
					field = new ValueWithUnitsField(tag,length,weightUnitsDescription);
				}
				else if (className.equals("Sex")) {
					field = new SingleCodedValueField(tag,length,sexDescriptors);
				}
				else if (className.equals("Race")) {
					field = new SingleCodedValueField(tag,length,raceDescriptors);
				}
				else if (className.equals("Drug")) {
					field = new DrugField(tag,length);
				}
				else if (className.equals("Binary")) {
					field = new BinaryField(tag,length);
				}
				else if (className.equals("FilterBitmap")) {
					field = new BitmapField(tag,length,filterDescriptors);
				}
				else if (className.equals("MachineID")) {
					field = new MachineIDField(tag,length);
				}
				else if (className.equals("StatCode")) {
					field = new SingleCodedValueField(tag,length,statCodeDescriptors);
				}
				else if (className.equals("MedicalHistory")) {
					field = new MedicalHistoryField(tag,length);
				}
				else if (className.equals("Electrode")) {
					field = new TwinCodedValueField(tag,length,electrodePlacement12LeadDescriptors,electrodePlacementXYZLeadDescriptors);
				}
				if (className.equals("TimeZone")) {
					field = new TimeZoneField(tag,length);
				}

			}
			if (field == null) {
				field = new Field(tag,length);
			}
			field.read();			// will update bytesRead
			if (field.isTerminator()) {
				seenTerminator=true;
				if (sectionBytesRemaining > 1) {
					System.err.println("Section 1 Encountered terminator but more than one padding byte in section "
						+sectionBytesRemaining+" dec (0x"+Long.toHexString(sectionBytesRemaining)+") bytes\n");
				}
				skipToEndOfSectionIfNotAlreadyThere(i);
				break;
			}
			if (fields[field.tag] == null) {
				fields[field.tag] = new ArrayList();	// lazy instantiation as we encounter first instance, since sparse
			}
			fields[field.tag].add(field);
		}
		if (!seenTerminator) {
			System.err.println("Section 1 Missing terminator tag\n");
		}
		skipToEndOfSectionIfNotAlreadyThere(i);
		return bytesRead;
	}

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		for (int t=0; t<256; ++t) {
			ArrayList fieldsForThisTag = fields[t];
			if (fieldsForThisTag != null) {
				Iterator li = fieldsForThisTag.iterator();
				while (li.hasNext()) {
					Field field = (Field)(li.next());
					if (field != null) {
						strbuf.append(field);
						strbuf.append("\n");
					}
				}
			}
		}
		return strbuf.toString();
	}

	public String validate() {
		StringBuffer strbuf = new StringBuffer();
		for (int t=0; t<256; ++t) {
			FieldDictionaryEntry entry=dictionaryFieldByTag[t];
			ArrayList fieldsForThisTag = fields[t];
			if (fieldsForThisTag == null) {
				if (entry != null) {
					if (entry.requirement == 1) {
						strbuf.append(describeFieldBriefly(entry));
						strbuf.append(": Missing required field");
						strbuf.append("\n");
					}
					else if (entry.requirement == 2) {
						strbuf.append(describeFieldBriefly(entry));
						strbuf.append(": Missing recommended field");
						strbuf.append("\n");
					}
				}
			}
			else {
//System.err.println("Section1.validate(): validating non-empty list of tag "+t);
				if (getMultiplicity(t) == 1 && fieldsForThisTag.size() != 1) {
					strbuf.append(describeFieldBriefly(t));
					strbuf.append(": Allowed only one instance of this field, got ");
					strbuf.append(fieldsForThisTag.size());
					strbuf.append("\n");
				}
				Iterator li = fieldsForThisTag.iterator();
				while (li.hasNext()) {
					Field field = (Field)(li.next());
					if (field != null) {
						strbuf.append(field.validate());
					}
				}
			}
		}
		return strbuf.toString();
	}
	
	/**
	 * <p>Get the value of multiple instances of the same field (tag) as a single string.</p>
	 *
	 * @param	fieldName	the name of the field
	 * @return			the concatenated values
	 */
	public String getConcatenatedStringValuesOfAllOccurencesOfNamedField(String fieldName) {
		String s = null;
		FieldDictionaryEntry entry = getDictionaryFieldByName(fieldName);
		if (entry != null) {
			StringBuffer strbuf = new StringBuffer();
			ArrayList fieldsForThisTag = fields[entry.tag];
			if (fieldsForThisTag != null) {
				Iterator li = fieldsForThisTag.iterator();
				while (li.hasNext()) {
					Field field = (Field)(li.next());
					if (field != null) {
						strbuf.append(field.getValueAsString());
					}
				}
			}
			s=strbuf.toString();
		}
		return s;
	}

	/**
	 * <p>Get the contents of the section as a tree for display, constructing it if not already done.</p>
	 *
	 * @param	parent	the node to which this section is to be added if it needs to be created de novo
	 * @return		the section as a tree
	 */
	public SCPTreeRecord getTree(SCPTreeRecord parent) {
		if (tree == null) {
			SCPTreeRecord tree = new SCPTreeRecord(parent,"Section",getValueForSectionNodeInTree());
			addSectionHeaderToTree(tree);
			for (int t=0; t<256; ++t) {
				ArrayList fieldsForThisTag = fields[t];
				if (fieldsForThisTag != null) {
					int nFieldsForThisTag = fieldsForThisTag.size();
					int count=0;
					Iterator li = fieldsForThisTag.iterator();
					while (li.hasNext()) {
						Field field = (Field)(li.next());
						if (field != null) {
							String name = getName(t)
								+ " ("
								+ Integer.toString(t)
								+ (nFieldsForThisTag > 1 ? (":"+Integer.toString(count)) : "")
								+ ")";
							new SCPTreeRecord(tree,name,field.getValueAsString());
						}
						++count;
					}
				}
			}
		}
		return tree;
	}
}


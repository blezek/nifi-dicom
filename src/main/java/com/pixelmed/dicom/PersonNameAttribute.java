/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.PhoneticStringEncoder;
import com.pixelmed.utils.PhoneticStringEncoderException;
import com.pixelmed.utils.StringUtilities;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Person Name (PN) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class PersonNameAttribute extends StringAttributeAffectedBySpecificCharacterSet {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/PersonNameAttribute.java,v 1.25 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(PersonNameAttribute.class);

	protected static final int MAX_LENGTH_SINGLE_VALUE = 64 + 1 + 64 + 1 + 64;	// limit is 64 per component group ... should really check for presence of equal sign, etc. :(
	
	public final int getMaximumLengthOfSingleValue() { return MAX_LENGTH_SINGLE_VALUE; }

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public PersonNameAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	specificCharacterSet	the character set to be used for the text
	 */
	public PersonNameAttribute(AttributeTag t,SpecificCharacterSet specificCharacterSet) {
		super(t,specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public PersonNameAttribute(AttributeTag t,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t,vl,i,specificCharacterSet);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	specificCharacterSet	the character set to be used for the text
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public PersonNameAttribute(AttributeTag t,Long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet) throws IOException, DicomException {
		super(t,vl.longValue(),i,specificCharacterSet);
	}

	/**
	 * <p>Get the value representation of this attribute (PN).</p>
	 *
	 * @return	'P','N' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.PN; }

	/**
	 * <p>Get a canonical form of the supplied person name value.</p>
	 *
	 * @param	value		a single person name value
	 * @return			a canonical form of the person name value
	 */
	public static String getCanonicalForm(String value) {
		return getCanonicalForm(value,false);
	}
	
	/**
	 * <p>Get a canonical form of the supplied person name value.</p>
	 *
	 * <p>For queries, wildcard characters for queries are left untouched; if a component is empty then a '*' is inserted instead of zero length.</p>
	 *
	 * @param	value		a single person name value
	 * @param	forQuery	if application is query and wildcards need to be handled
	 * @return			a canonical form of the person name value
	 */
	public static String getCanonicalForm(String value,boolean forQuery) {
		String newValue = null;
		if (value != null) {
			//newValue = value.toLowerCase(java.util.Locale.US);
			char[] characters = value.toCharArray();
			int length = characters.length;
			int offset = 0;
			
			// make lower case except for first letter after start or non letter
			// also replace all non letters and digits other than ^ and , and - and = with space
			boolean previousIsNotLetter = true;
			for (int i=offset; i<length; ++i) {
				char c = characters[i];
				if (Character.isLetter(c)) {
					characters[i] = previousIsNotLetter ? Character.toUpperCase(c) : Character.toLowerCase(c);
					previousIsNotLetter = false;
				}
				else {
					if (c != '*' && c != '?') {		// treat wild cards as letters with respect to capitalization
						previousIsNotLetter = true;
					}
					if (!Character.isDigit(c) && c != '^' && c != ',' && c != '*' && c != '?') {
						characters[i] = ' ';
					}
				}
			}
			
			// now do the leading and trailing space and delimiter removal,
			// after any replacements have been made
			
			// remove leading space
			while (offset < length && Character.isWhitespace(characters[offset])) {
				++offset;
			}
			
			// remove trailing delimiters and space
			while (length > offset) {
				char c = characters[length-1];
				if (!Character.isWhitespace(c) && c != '^' && c != ',') break;
				--length;
			}
			
			if (offset < length) {
				newValue = new String(characters,offset,length-offset);
//System.err.println("PersonNameAttribute.getCanonicalForm(): after cleanup of leading and training delimiters, value = "+value+" newValue = "+newValue);

				// now handle name components - use family and first name only, discarding other components
				
				String familyName = null;
				String givenName = null;
				
				if (newValue.indexOf("^") != -1) {
					// Assume form is familyName^givenName[^...]
					Vector components = StringUtilities.getDelimitedValues(newValue,"^");
					if (components.size() > 0) {
						familyName =  (String)(components.get(0));
						if (components.size() > 1) {
							givenName = (String)(components.get(1));
						}
					}
					// else do nothing ... just leave it the way it was
				}
				else if (newValue.indexOf(",") != -1) {
					// Assume form is familyName,givenName
					Vector components = StringUtilities.getDelimitedValues(newValue,",");
					if (components.size() > 0) {
						familyName =  (String)(components.get(0));
						if (components.size() > 1) {
							givenName = (String)(components.get(1));
						}
					}
					// else do nothing ... just leave it the way it was
				}
				else {
					// Assume form is givenName [...] familyName (this is not always valid, but is more often than not)
					Vector components = StringUtilities.getDelimitedValues(newValue," ");
					if (components.size() > 0) {
						familyName =  (String)(components.get(components.size()-1));
						if (components.size() > 1) {
							givenName = (String)(components.get(0));
						}
					}
//System.err.println("PersonNameAttribute.getCanonicalForm(): no ^ or , delimiters familyName = "+familyName+" givenName = "+givenName);
				}
//System.err.println("PersonNameAttribute.getCanonicalForm(): after extraction of family and given name, value = "+value+" familyName = "+familyName+" givenName = "+givenName);
				if (familyName != null) {
					// Use only first (non-space) part of family name
					StringTokenizer t = new StringTokenizer(familyName," ",false);
					if (t.hasMoreTokens()) {
						familyName =  t.nextToken();
					}
				}
				else {
					familyName = forQuery ? "*" : "";
				}
				if (givenName != null) {
					// Use only first (non-space) part of given name
					StringTokenizer t = new StringTokenizer(givenName," ",false);
					if (t.hasMoreTokens()) {
						givenName =  t.nextToken();
					}
				}
				else {
					givenName = forQuery ? "*" : "";
				}
					
				newValue = familyName + "^" + givenName;
			}
		}
//System.err.println("PersonNameAttribute.getCanonicalForm(): value = "+value+" newValue = "+newValue);
		return newValue;
	}

	private static String encodePartsSplitByWildcardOperator(String value,PhoneticStringEncoder encoder) throws PhoneticStringEncoderException {
		StringBuffer b = new StringBuffer();
		if (value != null && value.length() > 0) {
			char[] characters = value.toCharArray();
			int length = characters.length;
			int startOfStringToBeEncoded = 0;
			for (int i=0; i<length; ++i) {
				char c = characters[i];
				if (c == '*' || c == '?') {
					// we have a wildcard - phonetically encoding whatever lead up to this point, if anything
					if (startOfStringToBeEncoded < i) {
						b.append(encoder.encode(value.substring(startOfStringToBeEncoded,i)));
					}
					b.append(c);	// then append the wildcard character (unchanged)
					startOfStringToBeEncoded = i+1;
				}
			}
			// encode whatever is left at the end
			if (startOfStringToBeEncoded < length) {
				b.append(encoder.encode(value.substring(startOfStringToBeEncoded,length)));
			}
		}
		return b.toString();
	}
	
	private static String encodeString(String value,PhoneticStringEncoder encoder,boolean allowWildcard) throws PhoneticStringEncoderException {
		String encodedValue = "";
		if (value != null && value.length() > 0) {
			if (allowWildcard) {
				encodedValue = encodePartsSplitByWildcardOperator(value,encoder);
			}
			else {
				encodedValue = encoder.encode(value);
			}
		}
		return encodedValue;
	}
			
	/**
	 * <p>Get the name components from a DICOM delimited form of Person Name.</p>
	 *
	 * @param	value	a single person name value
	 * @return		a Vector of String containing the name components
	 */
	public static Vector getNameComponents(String value) {
		return StringUtilities.getDelimitedValues(value,"^");
	}
	
	/**
	 * <p>Get the family and given name components of a DICOM delimited form of Person Name and swap them.</p>
	 *
	 * @param	name		a single person name value with family and given name components
	 * @return			a DICOM delimited form of name with the family and given name components swapped, or the supplied value if not two componenbts
	 */
	public static String swap(String name) {
		Vector components = getNameComponents(name);
		return components.size() == 2 ? ((String)(components.get(1)) + "^" + (String)(components.get(0))) : name;
	}
	
	/**
	 * <p>Get a phonetic encoding name of the family and given name components of a DICOM delimited form of Person Name.</p>
	 *
	 * @param	name		a single person name value with family and given name components
	 * @return			a DICOM delimited form of name with phonetic equivalents substituted for family and given name components
	 */
	public static String getPhoneticName(String name) {
		return getPhoneticName(name,false);
	}
	
	/**
	 * <p>Get a phonetic encoding name of the family and given name components of a DICOM delimited form of Person Name.</p>
	 *
	 * <p>Note that wildcards used in queries are removed by the encoding process, though initial and terminal
	 * wildcard characters are restored in each component if present.</p>
	 *
	 * @param	name		a single person name value with family and given name components
	 * @param	forQuery	if application is query and wildcards need to be handled
	 * @return			a DICOM delimited form of name with phonetic equivalents substituted for family and given name components
	 */
	public static String getPhoneticName(String name,boolean forQuery) {
		String phoneticName = null;
		if (name != null) {
			Vector components = PersonNameAttribute.getNameComponents(name);
			String familyName = components.size() > 0 ? (String)(components.get(0)) : "";
			String givenName  = components.size() > 1 ? (String)(components.get(1)) : "";
			try {
				PhoneticStringEncoder encoder = new PhoneticStringEncoder();
				String phoneticFamilyName = encodeString(familyName,encoder,forQuery);
				String phoneticGivenName  = encodeString(givenName,encoder,forQuery);
				phoneticName = phoneticFamilyName + "^" + phoneticGivenName;
//System.err.println("PersonNameAttribute.getPhoneticName(): name = "+name+" phoneticName = "+phoneticName);
			}
			catch (PhoneticStringEncoderException e) {
				slf4jlogger.error("", e);
				phoneticName=name;	// better to return original than null
			}
		}
		return phoneticName;
	}

	// this method is for testing from the main class only so no need to use SLF4J since command line utility/test
	private static void processFileOrDirectory(File file) {
//System.err.println("PersonNameAttribute.processFileOrDirectory(): "+file);
		if (file.isDirectory()) {
//System.err.println("PersonNameAttribute.processFileOrDirectory(): Recursing into directory "+file);
			try {
				boolean noFileDoneYet = true;
				File listOfFiles[] = file.listFiles();
				for (int i=0; i<listOfFiles.length; ++i) {
					if (listOfFiles[i].isDirectory()) {
						processFileOrDirectory(listOfFiles[i]);
					}
					else if (listOfFiles[i].isFile() && !file.isHidden() && noFileDoneYet) {	// only do first file found, since names probably all the same
						processFileOrDirectory(listOfFiles[i]);
						noFileDoneYet = false;
					}
				}
			}
			catch (Exception e) {
				//e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			}
		}
		else if (file.isFile()) {
			if (!file.isHidden()) {
//System.err.println("PersonNameAttribute.processFileOrDirectory(): Doing file "+file);
				try {
					DicomInputStream dfi = new DicomInputStream(new BufferedInputStream(new FileInputStream(file)));
					AttributeList list = new AttributeList();
					list.read(dfi);
					dfi.close();
					
					String name = Attribute.getSingleStringValueOrNull(list,TagFromName.PatientName);
					if (name != null) {
						String canonicalName = PersonNameAttribute.getCanonicalForm(name);
						String phoneticName = getPhoneticName(canonicalName);
						System.out.println(name+"\t"+canonicalName+"\t"+phoneticName);	// no need to use SLF4J since command line utility/test
					}
				}
				catch (Exception e) {
					//e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
				}
			}
			else {
//System.err.println("PersonNameAttribute.processFileOrDirectory(): Skipping hidden "+file);
			}
		}
		else {
//System.err.println("PersonNameAttribute.processFileOrDirectory(): Not a directory or file "+file);
		}
	}

	/**
	 * <p>Test read the DICOM files listed on the command line, get the patient name and make canonical form.</p>
	 *
	 * @param	arg	a list of DICOM file names or directories to search for such
	 */
	public static void main(String arg[]) {
		for (int i=0; i<arg.length; ++i) {
			processFileOrDirectory(new File(arg[i]));
		}
	}
}


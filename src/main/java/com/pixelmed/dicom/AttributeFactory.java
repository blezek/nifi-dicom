/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.FileInputStream;

/**
 * <p>The {@link com.pixelmed.dicom.AttributeFactory AttributeFactory} class is a factory class of static methods for creating
 * concrete instances of the abstract class {@link com.pixelmed.dicom.Attribute Attribute} based on their
 * value representation.</p>
 *
 * <p>This class is
 * primarily used when reading a parsing a DICOM dataset, and needing to create attributes based on their
 * value representation, either from a dictionary or from the explicit value representation in the dataset.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class AttributeFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeFactory.java,v 1.36 2017/03/29 21:56:18 dclunie Exp $";

	private static final long maximumInMemoryOtherDataValueLength = 4096*4096*2;	// set to -1l to never leave on disk, 0 to always leave on disk
	//private static final long maximumInMemoryOtherDataValueLength = 0;			// set to -1l to never leave on disk, 0 to always leave on disk

	private AttributeFactory() {}
	
	/**
	 * <p>A static method to determine the {@link java.lang.Class Class} appropriate for storing an attribute based on the supplied value representation.</p>
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute (to check whether or not pixel data)
	 * @param	vr								the value representation of the attribute
	 * @param	explicit						a flag indicating that the stream to read or write uses explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 * @param	leaveOtherDataOnDisk			whether or not to leave OB or OW on disk or read it into memory
	 * @param	isSignedPixelRepresentation		the PixelRepresentation is signed (needed to choose VR for US/SS VR data elements)
	 *
	 * @return				the class appropriate for the attribute
	 */
	public static Class getClassOfAttributeFromValueRepresentation(AttributeTag tag,byte[] vr,boolean explicit,int bytesPerSample,boolean leaveOtherDataOnDisk,boolean isSignedPixelRepresentation) {
		Class c;
		Class classForOtherByte =  leaveOtherDataOnDisk ? OtherByteAttributeOnDisk.class : OtherByteAttribute.class;
		Class classForOtherWord =  leaveOtherDataOnDisk ? OtherWordAttributeOnDisk.class : OtherWordAttribute.class;
		if (ValueRepresentation.isApplicationEntityVR(vr)) {
			c=ApplicationEntityAttribute.class;
		}
		else if (ValueRepresentation.isAgeStringVR(vr)) {
			c=AgeStringAttribute.class;
		}
		else if (ValueRepresentation.isAttributeTagVR(vr)) {
			c=AttributeTagAttribute.class;
		}
		else if (ValueRepresentation.isCodeStringVR(vr)) {
			c=CodeStringAttribute.class;
		}
		else if (ValueRepresentation.isDateVR(vr)) {
			c=DateAttribute.class;
		}
		else if (ValueRepresentation.isDateTimeVR(vr)) {
			c=DateTimeAttribute.class;
		}
		else if (ValueRepresentation.isDecimalStringVR(vr)) {
			c=DecimalStringAttribute.class;
		}
		else if (ValueRepresentation.isFloatDoubleVR(vr)) {
			c=FloatDoubleAttribute.class;
		}
		else if (ValueRepresentation.isFloatSingleVR(vr)) {
			c=FloatSingleAttribute.class;
		}
		else if (ValueRepresentation.isIntegerStringVR(vr)) {
			c=IntegerStringAttribute.class;
		}
		else if (ValueRepresentation.isLongStringVR(vr)) {
			c=LongStringAttribute.class;
		}
		else if (ValueRepresentation.isLongTextVR(vr)) {
			c=LongTextAttribute.class;
		}
		else if (ValueRepresentation.isOtherByteVR(vr)) {
			// just in case was incorrectly encoded as explicit OB VR but with bytesPerSample > 1 (Bits Allocated > 8)
			c=(bytesPerSample > 1 && tag.equals(TagFromName.PixelData)) ? classForOtherWord : classForOtherByte;
//System.err.println("isOtherByteVR, bytesPerSample="+bytesPerSample+",tag="+tag+" creating class"+c);
		}
		else if (ValueRepresentation.isOtherDoubleVR(vr)) {
			c=OtherDoubleAttribute.class;
		}
		else if (ValueRepresentation.isOtherFloatVR(vr)) {
			c=OtherFloatAttribute.class;
		}
		else if (ValueRepresentation.isOtherLongVR(vr)) {
			c=OtherLongAttribute.class;
		}
		else if (ValueRepresentation.isOtherWordVR(vr)) {
			// This is not quite right ... in implicit VR, pixel data is always OW theoretically,
			// but this saves later unpacking ... and works as long is implicit VR is little endian
			// but always trust explicit, else single bit images in big endian with explicit OW have wrong byte order
			c=(explicit || bytesPerSample > 1 || !tag.equals(TagFromName.PixelData)) ? classForOtherWord : classForOtherByte;
//System.err.println("isOtherWordVR, bytesPerSample="+bytesPerSample+",tag="+tag+" creating class"+c);
		}
		else if (ValueRepresentation.isOtherUnspecifiedVR(vr)) {
			c=(bytesPerSample > 1 || !tag.equals(TagFromName.PixelData)) ? classForOtherWord : classForOtherByte;
//System.err.println("isOtherUnspecifiedVR, bytesPerSample="+bytesPerSample+",tag="+tag+" creating class"+c);
		}
		else if (ValueRepresentation.isPersonNameVR(vr)) {
			c=PersonNameAttribute.class;
		}
		else if (ValueRepresentation.isSequenceVR(vr)) {
			c=SequenceAttribute.class;
		}
		else if (ValueRepresentation.isShortStringVR(vr)) {
			c=ShortStringAttribute.class;
		}
		else if (ValueRepresentation.isSignedLongVR(vr)) {
			c=SignedLongAttribute.class;
		}
		else if (ValueRepresentation.isSignedShortVR(vr)) {
			c=SignedShortAttribute.class;
		}
		else if (ValueRepresentation.isShortTextVR(vr)) {
			c=ShortTextAttribute.class;
		}
		else if (ValueRepresentation.isTimeVR(vr)) {
			c=TimeAttribute.class;
		}
		else if (ValueRepresentation.isUniqueIdentifierVR(vr)) {
			c=UniqueIdentifierAttribute.class;
		}
		else if (ValueRepresentation.isUnsignedLongVR(vr)) {
			c=UnsignedLongAttribute.class;
		}
		else if (ValueRepresentation.isUnknownVR(vr)) {
			c=UnknownAttribute.class;
		}
		else if (ValueRepresentation.isUnsignedShortVR(vr)) {
			c=UnsignedShortAttribute.class;
		}
		else if (ValueRepresentation.isUnspecifiedShortVR(vr)) {
			c=isSignedPixelRepresentation ? SignedShortAttribute.class : UnsignedShortAttribute.class;
		}
		else if (ValueRepresentation.isUnspecifiedShortOrOtherWordVR(vr)) {
			c=isSignedPixelRepresentation ? SignedShortAttribute.class : UnsignedShortAttribute.class;
		}
		else if (ValueRepresentation.isUniversalResourceVR(vr)) {
			c=UniversalResourceAttribute.class;
		}
		else if (ValueRepresentation.isUnlimitedCharactersVR(vr)) {
			c=UnlimitedCharactersAttribute.class;
		}
		else if (ValueRepresentation.isUnlimitedTextVR(vr)) {
			c=UnlimitedTextAttribute.class;
		}
		else {
			// unrecognized but fixed length VR ... treat as UN ...
			c=UnknownAttribute.class;
		}
		return c;
	}
	
	/**
	 * <p>A static method to determine the {@link java.lang.Class Class} appropriate for storing an attribute based on the supplied value representation.</p>
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute (to check whether or not pixel data)
	 * @param	vr								the value representation of the attribute
	 * @param	explicit						a flag indicating that the stream to read or write uses explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 * @param	vl								the value length, used to decide whether or not to leave OB or OW on disk (if in a file) or read it into memory
	 * @param	isFile							the attribute is stored in a file
	 * @param	isSignedPixelRepresentation		the PixelRepresentation is signed (needed to choose VR for US/SS VR data elements)
	 *
	 * @return				the class appropriate for the attribute
	 */
	public static Class getClassOfAttributeFromValueRepresentation(AttributeTag tag,byte[] vr,boolean explicit,int bytesPerSample,long vl,boolean isFile,boolean isSignedPixelRepresentation) {
		boolean leaveOtherDataOnDisk = /*tag.equals(TagFromName.PixelData) &&*/ vl > maximumInMemoryOtherDataValueLength && maximumInMemoryOtherDataValueLength != -1l && isFile;	// (001000)
//if (tag.equals(TagFromName.PixelData)) {
//System.err.println("AttributeFactory.newAttribute(): leaveOtherDataOnDisk = "+leaveOtherDataOnDisk);
//System.err.println("AttributeFactory.newAttribute(): vl > maximumInMemoryOtherDataValueLength = "+(vl > maximumInMemoryOtherDataValueLength));
//System.err.println("AttributeFactory.newAttribute(): i.getFile() = "+i.getFile());
//}
		return getClassOfAttributeFromValueRepresentation(tag,vr,explicit,bytesPerSample,leaveOtherDataOnDisk,isSignedPixelRepresentation);
	}

	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the value representation from the standard dictionary.</p>
	 *
	 * <p>Will return a UN attribute if the tag is unrecognized.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements).</p>
	 *
	 * @param	tag			the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 *
	 * @return				the attribute of an appropriate class, with no value
	 * @throws	DicomException	if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag) throws DicomException {
		byte[] vr = AttributeList.getDictionary().getValueRepresentationFromTag(tag);
		if (vr == null) {
			vr = new byte[2]; vr[0]='U'; vr[1]='N';
		}
		return newAttribute(tag,vr);
	}

	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the supplied value representation.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements if the supplied VR from the dictionary is 'XS').</p>
	 *
	 * @param	tag				the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 * @param	vr				the value representation of the attribute to create (such as read from the stream, or from the dictionary)
	 *
	 * @return					the attribute of an appropriate class, with no value
	 * @throws	DicomException	if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr) throws DicomException {
		return newAttribute(tag,vr,true/*explicit*/,0/*bytesPerSample*/);
	}

	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the supplied value representation.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements if the supplied VR from the dictionary is 'XS').</p>
	 *
	 * @param	tag						the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 * @param	vr						the value representation of the attribute to create (such as read from the stream, or from the dictionary)
	 * @param	specificCharacterSet	the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 *
	 * @return							the attribute of an appropriate class, with no value
	 * @throws	DicomException			if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,SpecificCharacterSet specificCharacterSet) throws DicomException {
		return newAttribute(tag,vr,specificCharacterSet,true/*explicit*/,0/*bytesPerSample*/,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the supplied value representation.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements if the supplied VR from the dictionary is 'XS').</p>
	 *
	 * @param	tag				the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 * @param	vr				the value representation of the attribute to create (such as read from the stream, or from the dictionary)
	 * @param	explicit		a flag indicating that the stream is explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample	1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 *
	 * @return					the attribute of an appropriate class, with no value
	 * @throws	DicomException	if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,boolean explicit,int bytesPerSample) throws DicomException {
		return newAttribute(tag,vr,null/*SpecificCharacterSet*/,explicit,bytesPerSample,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the supplied value representation.</p>
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 * @param	vr								the value representation of the attribute to create (such as read from the stream, or from the dictionary)
	 * @param	specificCharacterSet			the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 * @param	explicit						a flag indicating that the stream is explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 * @param	isSignedPixelRepresentation		the PixelRepresentation is signed (needed to choose VR for US/SS VR data elements)
	 *
	 * @return									the attribute of an appropriate class, with no value
	 * @throws	DicomException					if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,SpecificCharacterSet specificCharacterSet,boolean explicit,int bytesPerSample,boolean isSignedPixelRepresentation) throws DicomException {
		Attribute a = null;
		try {
			Class classToUse = getClassOfAttributeFromValueRepresentation(tag,vr,explicit,bytesPerSample,false,isSignedPixelRepresentation);
//System.err.println("AttributeFactory.newAttribute(): classToUse = "+classToUse);
			Class [] argTypes  = null;
			Object[] argValues = null;
			if (ValueRepresentation.isAffectedBySpecificCharacterSet(vr)) {
//System.err.println("AttributeFactory.newAttribute(): isAffectedBySpecificCharacterSet, so specificCharacterSet = "+specificCharacterSet);
				Class [] t  = {AttributeTag.class,SpecificCharacterSet.class};
				Object[] v = {tag,specificCharacterSet};
				argTypes=t;
				argValues=v;
			}
			else {
				Class [] t  = {AttributeTag.class};
				Object[] v = {tag};
				argTypes=t;
				argValues=v;
			}
			a = (Attribute)(classToUse.getConstructor(argTypes).newInstance(argValues));
//System.err.println("made an "+a);
		}
		catch (Exception e) {
			throw new DicomException("Could not instantiate an attribute for "+tag+": "+e.getCause());
		}
		return a;
	}


	/**
	 * <p>A static method to create an {@link com.pixelmed.dicom.Attribute Attribute} based on the supplied value representation.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements).</p>
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create
	 * @param	vr								the value representation of the attribute to create (such as read from the stream, or from the dictionary)
	 * @param	specificCharacterSet			the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 * @param	explicit						a flag indicating that the stream is explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 *
	 * @deprecated								use {@link #newAttribute(AttributeTag,byte[],SpecificCharacterSet,boolean,int,boolean)} instead
	 *
	 * @return									the attribute of an appropriate class, with no value
	 * @throws	DicomException					if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,SpecificCharacterSet specificCharacterSet,boolean explicit,int bytesPerSample) throws DicomException {
		return newAttribute(tag,vr,specificCharacterSet,explicit,bytesPerSample,false/*isSignedPixelRepresentation*/);
	}
	
	/**
	 * <p>A static method to create and read an {@link com.pixelmed.dicom.Attribute Attribute} from a {@link com.pixelmed.dicom.DicomInputStream DicomInputStream}.</p>
	 *
	 * <p>The stream is left positioned at the start of the next attribute.</p>
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create (already read from the stream)
	 * @param	vr								the value representation of the attribute to create (already read from the stream, if present, else from the dictionary)
	 * @param	vl								the value length of the attribute to create (already read from the stream)
	 * @param	i								the {@link com.pixelmed.dicom.DicomInputStream DicomInputStream} to read the attribute from, positioned at the start of the value(s) to read
	 * @param	specificCharacterSet			the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 * @param	explicit						a flag indicating that the stream is explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 * @param	byteOffset						the byte offset from the beginning of the {@link com.pixelmed.dicom.DicomInputStream DicomInputStream}
	 * @param	isSignedPixelRepresentation		the PixelRepresentation is signed (needed to choose VR for US/SS VR data elements)
	 *
	 * @return									the attribute of an appropriate class populated with the value(s) read from the stream
	 * @throws	DicomException					if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet,
			boolean explicit,int bytesPerSample,long byteOffset,boolean isSignedPixelRepresentation) throws DicomException {
//System.err.println("AttributeFactory.newAttribute(): tag = "+tag);
		Attribute a = null;
		try {
			Class classToUse = getClassOfAttributeFromValueRepresentation(tag,vr,explicit,bytesPerSample,vl,i.getFile() != null,isSignedPixelRepresentation);
//System.err.println("AttributeFactory.newAttribute(): classToUse = "+classToUse);
			Class [] argTypes  = null;
			Object[] argValues = null;
			Long lvl = new Long(vl);
			Long lbo = new Long(byteOffset);
			if (ValueRepresentation.isAffectedBySpecificCharacterSet(vr)) {		// NB. the VRs returned by isAffectedBySpecificCharacterSet() need to have matching classes with appropriate constructors (with a SpecificCharacterSet argument)
//System.err.println("AttributeFactory.newAttribute(): isAffectedBySpecificCharacterSet");
				Class [] t  = {AttributeTag.class,Long.class,DicomInputStream.class,SpecificCharacterSet.class};
				Object[] v = {tag,lvl,i,specificCharacterSet};
				argTypes=t;
				argValues=v;
			}
			// NB. the VRs not returned by isAffectedBySpecificCharacterSet() need to have matching classes with appropriate constructors (without a SpecificCharacterSet argument)
			else if (classToUse == OtherByteAttributeOnDisk.class || classToUse == OtherWordAttributeOnDisk.class) {
//System.err.println("AttributeFactory.newAttribute(): onDisk");
				Class [] t  = {AttributeTag.class,Long.class,DicomInputStream.class,Long.class};
				Object[] v = {tag,lvl,i,lbo};
				argTypes=t;
				argValues=v;
			}
			else {
//System.err.println("AttributeFactory.newAttribute(): ordinary constructor");
				Class [] t  = {AttributeTag.class,long.class,DicomInputStream.class};
				Object[] v = {tag,lvl,i};
				argTypes=t;
				argValues=v;
			}
//System.err.println("AttributeFactory.newAttribute(): trying to make attribute");
			a = (Attribute)(classToUse.getConstructor(argTypes).newInstance(argValues));
//System.err.println("AttributeFactory.newAttribute(): made an "+a);
		}
		catch (Exception e) {
			//slf4jlogger.error("", e);;
			throw new DicomException("Could not instantiate an attribute for "+tag+": "+e.getCause());
		}
		return a;
	}

	/**
	 * <p>A static method to create and read an {@link com.pixelmed.dicom.Attribute Attribute} from a {@link com.pixelmed.dicom.DicomInputStream DicomInputStream}.</p>
	 *
	 * <p>The stream is left positioned at the start of the next attribute.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements).</p>
	 *
	 * @deprecated								use {@link #newAttribute(AttributeTag,byte[],long,DicomInputStream,SpecificCharacterSet,boolean,int,long,boolean)} instead
	 *
	 * @param	tag								the {@link com.pixelmed.dicom.AttributeTag AttributeTag} tag of the attribute to create (already read from the stream)
	 * @param	vr								the value representation of the attribute to create (already read from the stream, if present, else from the dictionary)
	 * @param	vl								the value length of the attribute to create (already read from the stream)
	 * @param	i								the {@link com.pixelmed.dicom.DicomInputStream DicomInputStream} to read the attribute from, positioned at the start of the value(s) to read
	 * @param	specificCharacterSet			the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 * @param	explicit						a flag indicating that the stream is explicit value representation (affects pixel data encoding choice)
	 * @param	bytesPerSample					1 or 2 bytes per sample indicating whether to use OB or OW for pixel data
	 * @param	byteOffset						the byte offset from the beginning of the {@link com.pixelmed.dicom.DicomInputStream DicomInputStream}
	 *
	 * @return									the attribute of an appropriate class populated with the value(s) read from the stream
	 * @throws	DicomException					if cannot instantiate a new attribute
	 */
	public static Attribute newAttribute(AttributeTag tag,byte[] vr,long vl,DicomInputStream i,SpecificCharacterSet specificCharacterSet,
			boolean explicit,int bytesPerSample,long byteOffset) throws DicomException {
		return newAttribute(tag,vr,vl,i,specificCharacterSet,explicit,bytesPerSample,byteOffset,false/*isSignedPixelRepresentation*/);
	}

}


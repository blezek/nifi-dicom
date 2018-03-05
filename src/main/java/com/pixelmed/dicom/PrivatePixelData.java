/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>This class includes standard and private mechanisms for handling float or double Pixel Data.</p>
 *
 *
 * @author	dclunie
 */
public class PrivatePixelData {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/PrivatePixelData.java,v 1.7 2017/01/24 10:50:38 dclunie Exp $";

	public static final String pixelmedPrivateCreatorForFloatOrDoublePixelData = "PixelMed Publishing";
	public static final int pixelmedPrivatePixelDataGroup = 0x7FE1;
	public static final AttributeTag pixelmedPrivateFloatPixelData = new AttributeTag(pixelmedPrivatePixelDataGroup,0x1001);
	public static final AttributeTag pixelmedPrivateDoublePixelData = new AttributeTag(pixelmedPrivatePixelDataGroup,0x1002);

	/**
	 * <p>Returns the Attribute that contains the Pixel Data.</p>
	 *
	 * <p>Handles standard and private float or double alternatives to the conventional (0x7FE0,0x0010).</p>
	 *
	 * @param	list	the AttributeList to search
	 * @return			the Attribute or null if not found
	 */
	public static Attribute getPixelData(AttributeList list) {
		Attribute a = list.get(TagFromName.PixelData);
		if (a == null) {
			a = list.get(TagFromName.FloatPixelData);
		}
		if (a == null) {
			a = list.get(TagFromName.DoubleFloatPixelData);
		}
		if (a == null) {
			a = list.get(pixelmedPrivateFloatPixelData,pixelmedPrivateCreatorForFloatOrDoublePixelData);
		}
		if (a == null) {
			a = list.get(pixelmedPrivateDoublePixelData,pixelmedPrivateCreatorForFloatOrDoublePixelData);
		}
//System.err.println("PrivatePixelData.getPixelData(): a = "+a);
		return a;
	}
	
	/**
	 * <p>Add or replace the Pixel Data.</p>
	 *
	 * <p>Removes any existing (private) group containing pixel data, including the private creator .</p>
	 *
	 * <p>Handles (various private) float or double alternatives to the conventional (0x7FE0,0x0010).</p>
	 *
	 * @param		list			the AttributeList to search
	 * @throws	DicomException	if an error occurs
	 */
	public static void replacePixelData(AttributeList list,Attribute aPixelData) throws DicomException {
		list.remove(TagFromName.PixelData);
		list.remove(TagFromName.FloatPixelData);
		list.remove(TagFromName.DoubleFloatPixelData);
		list.removeGroup(pixelmedPrivatePixelDataGroup);
		
		list.put(aPixelData);
		{
			AttributeTag t = aPixelData.getTag();
			if (t.isPrivate()) {
				{ Attribute a = new LongStringAttribute(new AttributeTag(t.getGroup(),t.getElement() >>> 8)); a.addValue(PrivatePixelData.pixelmedPrivateCreatorForFloatOrDoublePixelData); list.put(a); }
			}
		}
	}

}


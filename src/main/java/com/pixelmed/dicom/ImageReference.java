/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class to represent a non-hierarchical image reference.</p>
 *
 * @author	dclunie
 */
public class ImageReference extends SOPInstanceReference {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ImageReference.java,v 1.4 2017/01/24 10:50:37 dclunie Exp $";

	protected String referencedFrameNumber;

	/**
	 * <p>Construct an instance of a reference to an image.</p>
	 *
	 * @param	sopInstanceUID			the SOP Instance UID
	 * @param	sopClassUID				the SOP Class UID
	 */
	public ImageReference(String sopInstanceUID,String sopClassUID) {
		super(sopInstanceUID,sopClassUID);
		this.referencedFrameNumber=null;
	}

	/**
	 * <p>Construct an instance of a reference to an image.</p>
	 *
	 * @param	sopInstanceUID			the SOP Instance UID
	 * @param	sopClassUID				the SOP Class UID
	 * @param	referencedFrameNumber	the Referenced Frame Number
	 */
	public ImageReference(String sopInstanceUID,String sopClassUID,String referencedFrameNumber) {
		super(sopInstanceUID,sopClassUID);
		this.referencedFrameNumber=referencedFrameNumber;
	}

	/**
	 * <p>Construct an instance of a reference to an image.</p>
	 *
	 * @param	instanceReference		an existing {@link com.pixelmed.dicom.SOPInstanceReference SOPInstanceReference} for this image
	 * @param	referencedFrameNumber	the Referenced Frame Number
	 */
	public ImageReference(SOPInstanceReference instanceReference,String referencedFrameNumber) {
		super(instanceReference);
		this.referencedFrameNumber=referencedFrameNumber;
	}

	/**
	 * <p>Construct an instance of a reference to an image.</p>
	 *
	 * @param	instanceReference		an existing {@link com.pixelmed.dicom.SOPInstanceReference SOPInstanceReference} for this image
	 */
	public ImageReference(SOPInstanceReference instanceReference) {
		super(instanceReference);
		this.referencedFrameNumber=null;
	}

	/**
	 * <p>Get the Referenced Frame Number.</p>
	 *
	 * @return		the Referenced Frame Number, or null
	 */
	public String getReferencedFrameNumber() { return referencedFrameNumber; }
		
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(super.toString());
		str.append(", Frame: ");
		if (referencedFrameNumber != null) {
			str.append(referencedFrameNumber);
		}
		return str.toString();
	}
}


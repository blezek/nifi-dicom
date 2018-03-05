/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class to represent the study, series and instance identifiers necessary to retrieve a specific single image or frame in a multi-frame image using the hierarchical model.</p>
 *
 * <p>Used, for example, when extracting a map of instance uids to hierarchical references from an SR evidence sequence.</p>
 *
 * @author	dclunie
 */
public class HierarchicalImageReference extends HierarchicalSOPInstanceReference {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/HierarchicalImageReference.java,v 1.5 2017/01/24 10:50:37 dclunie Exp $";

	protected String referencedFrameNumber;

	/**
	 * <p>Construct an instance of a reference to an image, with its hierarchy.</p>
	 *
	 * @param	studyInstanceUID		the Study Instance UID
	 * @param	seriesInstanceUID		the Series Instance UID
	 * @param	sopInstanceUID			the SOP Instance UID
	 * @param	sopClassUID				the SOP Class UID
	 */
	public HierarchicalImageReference(String studyInstanceUID,String seriesInstanceUID,String sopInstanceUID,String sopClassUID) {
		super(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID);
		this.referencedFrameNumber=null;
	}

	/**
	 * <p>Construct an instance of a reference to an image, with its hierarchy.</p>
	 *
	 * @param	studyInstanceUID		the Study Instance UID
	 * @param	seriesInstanceUID		the Series Instance UID
	 * @param	sopInstanceUID			the SOP Instance UID
	 * @param	sopClassUID				the SOP Class UID
	 * @param	referencedFrameNumber	the Referenced Frame Number
	 */
	public HierarchicalImageReference(String studyInstanceUID,String seriesInstanceUID,String sopInstanceUID,String sopClassUID,String referencedFrameNumber) {
		super(studyInstanceUID,seriesInstanceUID,sopInstanceUID,sopClassUID);
		this.referencedFrameNumber=referencedFrameNumber;
	}

	/**
	 * <p>Construct an instance of a reference to an image, with its hierarchy.</p>
	 *
	 * @param	instanceReference		an existing {@link com.pixelmed.dicom.HierarchicalSOPInstanceReference HierarchicalSOPInstanceReference} for this image
	 * @param	referencedFrameNumber	the Referenced Frame Number
	 */
	public HierarchicalImageReference(HierarchicalSOPInstanceReference instanceReference,String referencedFrameNumber) {
		super(instanceReference);
		this.referencedFrameNumber=referencedFrameNumber;
	}

	/**
	 * <p>Construct an instance of a reference to an image, with its hierarchy.</p>
	 *
	 * @param	instanceReference		an existing {@link com.pixelmed.dicom.HierarchicalSOPInstanceReference HierarchicalSOPInstanceReference} for this image
	 */
	public HierarchicalImageReference(HierarchicalSOPInstanceReference instanceReference) {
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


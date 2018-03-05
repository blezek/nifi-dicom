/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class to represent a non-hierarchical instance reference.</p>
 *
 * @author	dclunie
 */
public class SOPInstanceReference {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SOPInstanceReference.java,v 1.4 2017/01/24 10:50:38 dclunie Exp $";

	protected String sopInstanceUID;
	protected String sopClassUID;

	/**
	 * <p>Construct an instance of a reference to an instance.</p>
	 *
	 * @param	sopInstanceUID		the SOP Instance UID
	 * @param	sopClassUID			the SOP Class UID
	 */
	public SOPInstanceReference(String sopInstanceUID,String sopClassUID) {
		this.sopInstanceUID=sopInstanceUID;
		this.sopClassUID=sopClassUID;
	}

	/**
	 * <p>Construct an instance of a reference to an instance.</p>
	 *
	 * @param	reference			an existing reference to clone
	 */
	public SOPInstanceReference(SOPInstanceReference reference) {
		this.sopInstanceUID    = reference.getSOPInstanceUID();
		this.sopClassUID       = reference.getSOPClassUID();
	}

	/**
	 * <p>Construct an instance of a reference from the attributes of the referenced instance itself.</p>
	 *
	 * @param	list			the attributes of an instance
	 */
	public SOPInstanceReference(AttributeList list) {
		this.sopInstanceUID    = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
		this.sopClassUID       = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID);
	}

	/**
	 * <p>Get the SOP Instance UID.</p>
	 *
	 * @return		the SOP Instance UID, or null
	 */
	public String getSOPInstanceUID() { return sopInstanceUID; }

	/**
	 * <p>Get the SOP Class UID.</p>
	 *
	 * @return		the SOP Class UID, or null
	 */
	public String getSOPClassUID()    { return sopClassUID; }

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Instance: ");
		str.append(sopInstanceUID);
		str.append(", ");
		str.append("Class: ");
		str.append(sopClassUID);
		return str.toString();
	}
}


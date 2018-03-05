/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;

public class RoleInProcedure {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/RoleInProcedure.java,v 1.6 2017/01/24 10:50:43 dclunie Exp $";

	private String description;
	
	private RoleInProcedure() {};
	
	private RoleInProcedure(String description) {
		this.description = description;
	};
	
	public static final RoleInProcedure IRRADIATION_ADMINISTERING = new RoleInProcedure("Irradiation Administering");
	public static final RoleInProcedure IRRADIATION_AUTHORIZING = new RoleInProcedure("Irradiation Authorizing");
	
	public String toString() { return description; }
	
	
	public static RoleInProcedure getRoleInProcedure(ContentItem parent) {
		RoleInProcedure found = null;
		ContentItem ci = parent.getNamedChild("DCM","113875");	// "Person Role in Procedure"
		if (ci != null
		 && ci instanceof ContentItemFactory.CodeContentItem) {
			CodedSequenceItem conceptCode = ((ContentItemFactory.CodeContentItem)ci).getConceptCode();
			if (conceptCode != null) {
				String csd = conceptCode.getCodingSchemeDesignator();
				if (csd != null && csd.equals("DCM")) {
					String cv = conceptCode.getCodeValue();
					if (cv != null) {
						if (cv.equals("113851")) {
							found = RoleInProcedure.IRRADIATION_ADMINISTERING;
						}
						else if (cv.equals("113850")) {
							found = RoleInProcedure.IRRADIATION_AUTHORIZING;
						}
					}
				}
			}
		}
		return found;
	}

	public static CodedSequenceItem getCodedSequenceItem(RoleInProcedure role) throws DicomException {
		CodedSequenceItem csi = null;
		if (role != null) {
			if (role.equals(RoleInProcedure.IRRADIATION_ADMINISTERING)) {
				csi = new CodedSequenceItem("113851","DCM","Irradiation Administering");
			}
			else if (role.equals(RoleInProcedure.IRRADIATION_AUTHORIZING)) {
				csi = new CodedSequenceItem("113850","DCM","Irradiation Authorizing");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
}


package com.blezek.nifi.dicom;

import org.dcm4che3.data.Attributes;

public class AttributesMap {

	public final Attributes original;
	public final Attributes deidentifed;

	public AttributesMap(Attributes original, Attributes deidentified) {
		this.original = original;
		this.deidentifed = deidentified;
	}

}

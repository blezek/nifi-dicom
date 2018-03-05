package com.blezek.nifi.dicom;

import org.dcm4che3.data.Tag;

public abstract class AbstractAttributeStorage implements AttributeStorage {

	@Override
	public void storeAttributes(AttributesMap attributesMap) {
		storeAttributes(attributesMap.original.getString(Tag.StudyInstanceUID),
				attributesMap.original.getString(Tag.SeriesInstanceUID),
				attributesMap.original.getString(Tag.SOPInstanceUID),
				attributesMap.deidentifed.getString(Tag.StudyInstanceUID),
				attributesMap.deidentifed.getString(Tag.SeriesInstanceUID),
				attributesMap.deidentifed.getString(Tag.SOPInstanceUID), attributesMap);
	}

}

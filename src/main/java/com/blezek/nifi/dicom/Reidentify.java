package com.blezek.nifi.dicom;

import java.util.Set;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

public class Reidentify {

  public Attributes reidentify(AttributeStorage storage, Attributes deidentified) {

    Attributes identified = new Attributes(deidentified);
    // Get the most specific data we can
    Set<AttributesMap> storedAttributes = storage.getAttributesMap(deidentified.getString(Tag.StudyInstanceUID), deidentified.getString(Tag.SeriesInstanceUID));

    for (AttributesMap map : storedAttributes) {
      Attributes modifiedAttributes = map.original.getRemovedOrModified(map.deidentifed);
      identified.update(modifiedAttributes, null);
    }
    return identified;
  }

}

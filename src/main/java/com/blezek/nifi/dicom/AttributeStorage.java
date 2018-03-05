package com.blezek.nifi.dicom;

import java.util.Set;

public interface AttributeStorage {

  void storeAttributes(AttributesMap attributesMap);

  void storeAttributes(String originalStudyInstanceUID, String originalSeriesInstanceUID,
      String originalSOPInstanceUID, String deidentifiedStudyInstanceUID, String deidentifiedSeriesInstanceUID,
      String deidentifiedSOPInstanceUID, AttributesMap attributesMap);

  /** the set of AttributesMaps specified by these UIDs */
  Set<AttributesMap> getAttributes(String deidentifedStudyInstanceUID, String deidentifiedSeriesInstanceUID,
      String deidentifiedSOPInstanceUID);

  /**
   * the set of AttributesMaps specified by the StudyInstanceUID and
   * SeriesInstanceUID
   */
  Set<AttributesMap> getAttributesMap(String deidentifedStudyInstanceUID, String deidentifiedSeriesInstanceUID);

  /** All attributes associated with the deidentified StudyInstanceUID */
  Set<AttributesMap> getAttributesMap(String deidentifedStudyInstanceUID);

  /** Deidentification helpers */
  int counterForId(String type, String id);

  /** Return an existing UID or create a new one */
  String mapOrCreateUID(String string);

}


package com.blezek.nifi.dicom;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.UIDUtils;

import com.google.common.hash.Hashing;

public class Deidentify {

  public static String hashString(String s, VR vr) {
    String hash = Hashing.sha256().hashString(s, StandardCharsets.UTF_8)
        .toString();
    hash = hash.substring(0, vr.headerLength() - 1);
    return hash;
  }

  public String hashUID(String uid) {
    return UIDUtils.createNameBasedUID(uid.getBytes());
  }

  public Attributes deidentify(Attributes attributes) {
    Attributes deidentified = new Attributes(attributes);
    String newStudyUID = hashUID(attributes.getString(Tag.StudyInstanceUID));
    String newSeriesUID = hashUID(attributes.getString(Tag.SeriesInstanceUID));
    String newInstanceUID = hashUID(attributes.getString(Tag.SOPInstanceUID));

    Map<String, String> uidMap = new HashMap<>();
    uidMap.put(attributes.getString(Tag.StudyInstanceUID), newStudyUID);
    uidMap.put(attributes.getString(Tag.SeriesInstanceUID), newSeriesUID);
    uidMap.put(attributes.getString(Tag.SOPInstanceUID), newInstanceUID);
    UIDUtils.remapUIDs(deidentified, uidMap);

    // replace according to supplement 55, having a look at
    // https://wiki.cancerimagingarchive.net/display/Public/De-identification+Knowledge+Base
    // as well
    deidentified.setString(Tag.PatientName, VR.PN, hashString(attributes.getString(Tag.PatientName), VR.LO));
    deidentified.setString(Tag.PatientID, VR.PN, hashString(attributes.getString(Tag.PatientID), VR.LO));

    return deidentified;
  }

}

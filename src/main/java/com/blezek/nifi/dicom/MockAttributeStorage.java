package com.blezek.nifi.dicom;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.dcm4che3.util.UIDUtils;

public class MockAttributeStorage extends AbstractAttributeStorage {

  static final Set<AttributesMap> emptyMap = new HashSet<>();
  ConcurrentHashMap<String, Set<AttributesMap>> attributes = new ConcurrentHashMap<>();
  ConcurrentHashMap<String, String> uidMap = new ConcurrentHashMap<>();
  ConcurrentHashMap<String, String> stringMap = new ConcurrentHashMap<>();
  final MessageDigest digest;

  public MockAttributeStorage() {
    try {
      digest = MessageDigest.getInstance("SHA1");
    } catch (final NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    }
  }

  @Override
  public Set<AttributesMap> getAttributes(String deidentifedStudyInstanceUID, String deidentifiedSeriesInstanceUID,
      String deidentifiedSOPInstanceUID) {
    return attributes.getOrDefault(
        deidentifedStudyInstanceUID + deidentifiedSeriesInstanceUID + deidentifiedSOPInstanceUID, emptyMap);
  }

  @Override
  public Set<AttributesMap> getAttributesMap(String deidentifedStudyInstanceUID,
      String deidentifiedSeriesInstanceUID) {
    return attributes.getOrDefault(deidentifedStudyInstanceUID + deidentifiedSeriesInstanceUID, emptyMap);
  }

  @Override
  public Set<AttributesMap> getAttributesMap(String deidentifedStudyInstanceUID) {
    return attributes.getOrDefault(deidentifedStudyInstanceUID, emptyMap);
  }

  @Override
  public synchronized void storeAttributes(String originalStudyInstanceUID, String originalSeriesInstanceUID,
      String originalSOPInstanceUID, String deidentifiedStudyInstanceUID, String deidentifiedSeriesInstanceUID,
      String deidentifiedSOPInstanceUID, AttributesMap attributesMap) {
    attributes.putIfAbsent(originalStudyInstanceUID, ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(originalSeriesInstanceUID, ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(originalSOPInstanceUID, ConcurrentHashMap.newKeySet());

    attributes.putIfAbsent(originalStudyInstanceUID + originalSeriesInstanceUID, ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(originalStudyInstanceUID + originalSeriesInstanceUID + originalSOPInstanceUID,
        ConcurrentHashMap.newKeySet());

    // Add the value
    attributes.get(originalStudyInstanceUID).add(attributesMap);
    attributes.get(originalSeriesInstanceUID).add(attributesMap);
    attributes.get(originalSOPInstanceUID).add(attributesMap);

    attributes.get(originalStudyInstanceUID + originalSeriesInstanceUID + originalSOPInstanceUID)
        .add(attributesMap);
    attributes.get(originalStudyInstanceUID + originalSeriesInstanceUID).add(attributesMap);

    attributes.putIfAbsent(deidentifiedStudyInstanceUID, ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(deidentifiedSeriesInstanceUID, ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(deidentifiedSOPInstanceUID, ConcurrentHashMap.newKeySet());

    attributes.putIfAbsent(deidentifiedStudyInstanceUID + deidentifiedSeriesInstanceUID,
        ConcurrentHashMap.newKeySet());
    attributes.putIfAbsent(
        deidentifiedStudyInstanceUID + deidentifiedSeriesInstanceUID + deidentifiedSOPInstanceUID,
        ConcurrentHashMap.newKeySet());

    attributes.get(deidentifiedStudyInstanceUID).add(attributesMap);
    attributes.get(deidentifiedSeriesInstanceUID).add(attributesMap);
    attributes.get(deidentifiedSOPInstanceUID).add(attributesMap);

    attributes.get(deidentifiedStudyInstanceUID + deidentifiedSeriesInstanceUID + deidentifiedSOPInstanceUID)
        .add(attributesMap);
    attributes.get(deidentifiedStudyInstanceUID + deidentifiedSeriesInstanceUID).add(attributesMap);

  }

  Map<String, AtomicInteger> idMap = new HashMap<>();;

  @Override
  public int counterForId(String type, String id) {
    AtomicInteger a = idMap.getOrDefault(type, new AtomicInteger(0));
    return a.incrementAndGet();
  }

  @Override
  public String mapOrCreateUID(String uid) {
    uidMap.putIfAbsent(uid, UIDUtils.createNameBasedUID(uid.getBytes()));
    return uidMap.get(uid);
  }
}

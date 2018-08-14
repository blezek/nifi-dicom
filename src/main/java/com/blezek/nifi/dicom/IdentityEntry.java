package com.blezek.nifi.dicom;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.opencsv.bean.CsvBindByName;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class IdentityEntry {

  @CsvBindByName
  public String patientId;
  @CsvBindByName
  public String patientName;
  @CsvBindByName
  public String deidentifiedPatientId;
  @CsvBindByName
  public String deidentifiedPatientName;

  // Empty constructor
  public IdentityEntry() {
  }

  // Create a synthetic entry
  public static IdentityEntry createPseudoEntry(String id, String patientName) throws NoSuchAlgorithmException {
    IdentityEntry entry = new IdentityEntry();
    entry.patientId = id;
    entry.patientName = patientName;
    MessageDigest md = MessageDigest.getInstance("MD5");
    entry.deidentifiedPatientName = (new HexBinaryAdapter()).marshal(md.digest(id.getBytes()));

    md.reset();
    md.update(id.getBytes());
    md.update("PatientId".getBytes());
    entry.deidentifiedPatientId = (new HexBinaryAdapter()).marshal(md.digest());
    return entry;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public String getDeidentifiedPatientId() {
    return deidentifiedPatientId;
  }

  public void setDeidentifiedPatientId(String deidentifiedPatientId) {
    this.deidentifiedPatientId = deidentifiedPatientId;
  }

  public String getDeidentifiedPatientName() {
    return deidentifiedPatientName;
  }

  public void setDeidentifiedPatientName(String deidentifiedPatientName) {
    this.deidentifiedPatientName = deidentifiedPatientName;
  }

  public String generateAccessionNumber(String accessionNumber) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(Optional.ofNullable(patientId).orElse("patientid").getBytes());
    md.update(Optional.ofNullable(patientName).orElse("patientname").getBytes());
    md.update(Optional.ofNullable(accessionNumber).orElse("accessionnumber").getBytes());
    return new BigInteger(1, md.digest()).toString().substring(0, 16);
  }
}

package com.blezek.nifi.dicom;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IdentityEntry {

    public String patientId;
    public String patientName;
    public String deidentifiedPatientId;
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
	md.update(patientId.getBytes());
	md.update(patientName.getBytes());
	md.update(accessionNumber.getBytes());
	return new BigInteger(1, md.digest()).toString().substring(0, 16);
    }
}

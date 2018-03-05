package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.util.UIDUtils;
import org.junit.Assert;
import org.junit.Test;

import com.blezek.nifi.dicom.AttributeStorage;
import com.blezek.nifi.dicom.AttributesMap;
import com.blezek.nifi.dicom.MockAttributeStorage;

public class AttributeStorageTest {

	@Test
	public void readDICOM() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();

		URL p = classLoader.getResource("Denoising/CTE_4/Thins/IM-0001-0001.dcm");
		Attributes attributes;
		try (InputStream is = p.openStream()) {
			try (DicomInputStream dis = new DicomInputStream(is)) {
				dis.setIncludeBulkData(IncludeBulkData.URI);
				attributes = dis.readDataset(-1, -1);
			}
		}
		Assert.assertNotNull(attributes);
	}

	@Test
	public void hashFunctions() {
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (final NoSuchAlgorithmException nsae) {
			throw new AssertionError(nsae);
		}
		Assert.assertNotNull(digest);
	}

	@Test
	public void storeAndRetrieve() throws IOException {
		AttributeStorage config = new MockAttributeStorage();

		ClassLoader classLoader = getClass().getClassLoader();
		URL p = classLoader.getResource("Denoising/CTE_4/Thins/IM-0001-0001.dcm");
		Attributes attributes;
		try (InputStream is = p.openStream()) {
			try (DicomInputStream dis = new DicomInputStream(is)) {
				dis.setIncludeBulkData(IncludeBulkData.NO);
				attributes = dis.readDataset(-1, -1);
			}
		}

		Attributes deidentified = new Attributes(attributes);
		String newStudyUID = config.mapOrCreateUID(attributes.getString(Tag.StudyInstanceUID));
		String newSeriesUID = config.mapOrCreateUID(attributes.getString(Tag.SeriesInstanceUID));
		String newInstanceUID = config.mapOrCreateUID(attributes.getString(Tag.SOPInstanceUID));

		Map<String, String> uidMap = new HashMap<>();
		uidMap.put(attributes.getString(Tag.StudyInstanceUID), newStudyUID);
		uidMap.put(attributes.getString(Tag.SeriesInstanceUID), newSeriesUID);
		uidMap.put(attributes.getString(Tag.SOPInstanceUID), newInstanceUID);
		UIDUtils.remapUIDs(deidentified, uidMap);

		deidentified.setString(Tag.PatientName, VR.PN, "New Name");

		config.storeAttributes(attributes.getString(Tag.StudyInstanceUID), attributes.getString(Tag.SeriesInstanceUID),
				attributes.getString(Tag.SOPInstanceUID), newStudyUID, newSeriesUID, newInstanceUID,
				new AttributesMap(attributes, deidentified));

		Set<AttributesMap> a = config.getAttributes(newStudyUID, newSeriesUID, newInstanceUID);
		assertEquals("had one entry", 1, a.size());

		for (AttributesMap aa : a) {
			assertEquals("Deidentified PatientName", aa.deidentifed.getString(Tag.PatientName), "New Name");
			assertEquals("Original PatientName", aa.original.getString(Tag.PatientName),
					attributes.getString(Tag.PatientName));
		}
	}

	// @Test
	// public void mapUID() {
	// // DeidentifyConfiguration config = new InMemoryDeidentifyConfiguration();
	// AttributeStorage config = new MockDeidentifyConfiguration();
	//
	// String newStudyUID = config.mapUID("1.2.3.4.5");
	// assertEquals("Study UID", "1.2.3.4.5", config.unmapUID(newStudyUID));
	// }

	// @Test
	// public void withDefaults() throws Exception {
	// DeidentifyConfiguration config = new InMemoryDeidentifyConfiguration();
	// Attributes attributes;
	// try (InputStream is =
	// getClass().getResource("Denoising/CTE_4/Thins/IM-0001-0001.dcm").openStream())
	// {
	// DicomInputStream dis = new DicomInputStream(is);
	// dis.setIncludeBulkData(IncludeBulkData.NO);
	// Attributes fmi = dis.readFileMetaInformation();
	// attributes = dis.readDataset(-1, -1);
	// }
	// Attributes other = Deidentify.deidentify(config, attributes);
	// assertEquals("New patient name", "PN-0001",
	// other.getString("tag.PatientName", 0, VR.LO));
	//
	// }

	// Attributes other = new Attributes(original.size()+2);
	// other.setString("PrivateCreatorC", 0x00990002, VR.LO, "New0099xx02C");
	// other.addAll(original);
	// other.remove(Tag.AccessionNumber);
	// other.setString(Tag.PatientName, VR.LO, "Added^Patient^Name");
	// other.setString(Tag.PatientID, VR.LO, "ModifiedPatientID");
	// other.getNestedDataset(Tag.OtherPatientIDsSequence)
	// .setString(Tag.PatientID, VR.LO, "ModifiedOtherPatientID");
	// other.setString("PrivateCreatorB", 0x00990002, VR.LO, "Modfied0099xx02B");
	// return other;
}

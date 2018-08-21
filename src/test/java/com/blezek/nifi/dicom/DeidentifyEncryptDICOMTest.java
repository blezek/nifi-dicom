package com.blezek.nifi.dicom;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.blezek.nifi.dicom.util.Encryption;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

public class DeidentifyEncryptDICOMTest {

  private static final String IdentifiedDICOM = "/dicom/LGG-104_SPGR_000.dcm";
  static Encryption e;
  String password = "password";

  @Test
  @DisplayName("when encrypted, ensure proper tags are saved")
  public void testSimpleEncription() throws Exception {
    TestRunner runner = TestRunners.newTestRunner(DeidentifyEncryptDICOM.class);
    runner.enqueue(getClass().getResourceAsStream(IdentifiedDICOM));
    runner.setProperty(DeidentifyEncryptDICOM.PASSWORD, password);

    runner.assertValid();
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyEncryptDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);

      assertEquals(null, actualAttributes.getString(Tag.PatientName));
      assertEquals(null, actualAttributes.getString(Tag.PatientID));
      assertEquals(null, actualAttributes.getString(Tag.AccessionNumber));
      // Optionally save...
      boolean saveDeidentified = true;
      if (saveDeidentified) {
        /*
         * To modify the Series and SOPInstance UIDs
         *
         * cp -f deidentified.dcm src/test/resources/dicom/
         * 
         * dcmodify --gen-ser-uid --gen-inst-uid deidentified.dcm
         * 
         * cp -f deidentified.dcm src/test/resources/dicom/deidentified_new_series.dcm
         */
        try (DicomOutputStream dos = new DicomOutputStream(new File("deidentified.dcm"))) {
          dos.writeDataset(null, actualAttributes);
        }
      }
      assertTrue(actualAttributes.contains(Tag.EncryptedAttributesSequence));
      Sequence eas = actualAttributes.getSequence(Tag.EncryptedAttributesSequence);
      assertEquals(1, eas.size());
      Attributes ea = eas.get(0);
      assertEquals(ea.getString(Tag.EncryptedContentTransferSyntaxUID), UID.ExplicitVRLittleEndian);
      byte[] content = ea.getBytes(Tag.EncryptedContent);
      assertTrue(content.length > 0);
      assertTrue(Encryption.isPasswordRecipient(content));

      // Decrypt
      content = Encryption.extractPasswordEnvelopedData(password.toCharArray(), content);

      // Read the tags
      try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(content), UID.ExplicitVRLittleEndian)) {
        dis.setIncludeBulkData(IncludeBulkData.NO);
        Attributes tags = dis.readDataset(-1, -1);
        assertTrue(tags.contains(Tag.ModifiedAttributesSequence));
        Sequence mas = tags.getSequence(Tag.ModifiedAttributesSequence);
        assertEquals(1, mas.size());
        Attributes ma = mas.get(0);
        assertEquals(11, ma.size());
        assertTrue(ma.contains(DeidentifyEncryptDICOM.PRIVATE_CREATOR, DeidentifyEncryptDICOM.PRIVATE_TAG));
      }
    }
  }

  @Test
  public void testSimpleDecrypt() throws Exception {
    TestRunner runner = TestRunners.newTestRunner(DecryptReidentifyDICOM.class);
    runner.enqueue(getClass().getResourceAsStream("/dicom/deidentified.dcm"));
    runner.setProperty(DeidentifyEncryptDICOM.PASSWORD, password);
    runner.assertValid();
    runner.run();

    runner.assertAllFlowFilesTransferred(DecryptReidentifyDICOM.RELATIONSHIP_SUCCESS, 1);
    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      Attributes originalAttributes = TestUtil.getAttributes(getClass().getResourceAsStream(IdentifiedDICOM));
      Attributes diff = originalAttributes.getRemovedOrModified(actualAttributes);
      assertEquals(2, diff.size());
      assertTrue(diff.contains(Tag.DeidentificationMethod));
      assertTrue(diff.contains(Tag.DeidentificationMethodCodeSequence));
      diff.remove(Tag.DeidentificationMethod);
      diff.remove(Tag.DeidentificationMethodCodeSequence);
      assertEquals(0, diff.size());
    }
  }

  @Test
  public void testRejectOfNewSeries() {
    TestRunner runner = TestRunners.newTestRunner(DecryptReidentifyDICOM.class);
    runner.enqueue(getClass().getResourceAsStream("/dicom/deidentified_new_series.dcm"));
    runner.setProperty(DeidentifyEncryptDICOM.PASSWORD, password);
    runner.setProperty(DecryptReidentifyDICOM.ACCEPT_NEW_SERIES, "false");
    runner.assertValid();
    runner.run();
    runner.assertAllFlowFilesTransferred(DecryptReidentifyDICOM.RELATIONSHIP_NOT_DECRYPTED, 1);
  }

  @Test
  public void testAcceptOfNewSeries() throws Exception {
    TestRunner runner = TestRunners.newTestRunner(DecryptReidentifyDICOM.class);
    runner.enqueue(getClass().getResourceAsStream("/dicom/deidentified_new_series.dcm"));
    runner.setProperty(DeidentifyEncryptDICOM.PASSWORD, password);
    runner.setProperty(DecryptReidentifyDICOM.ACCEPT_NEW_SERIES, "true");
    runner.assertValid();
    runner.run();
    runner.assertAllFlowFilesTransferred(DecryptReidentifyDICOM.RELATIONSHIP_SUCCESS, 1);
    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("1.2.276.0.7230010.3.1.4.0.86932.1534864733.490723", actualAttributes.getString(Tag.SOPInstanceUID));
      assertEquals("1.2.276.0.7230010.3.1.3.0.86932.1534864733.490722",
          actualAttributes.getString(Tag.SeriesInstanceUID));

      // Study UID will be from the original DICOM, i.e. it's not replaced...
      // see dicom/LGG-104_SPGR_000.dcm
      assertEquals("1.3.6.1.4.1.14519.5.2.1.3344.2526.291265840929678567019499305523",
          actualAttributes.getString(Tag.StudyInstanceUID));
    }
  }
}

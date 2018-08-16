package com.blezek.nifi.dicom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.io.Files;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.TransferSyntax;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.PasswordRecipient;
import org.bouncycastle.cms.PasswordRecipientId;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JcePasswordEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JcePasswordRecipientInfoGenerator;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

public class DeidentifyDICOMTest {
  static final Logger logger = LoggerFactory.getLogger(DeidentifyDICOMTest.class);

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private DeidentifyDICOM deidentifyDICOM;
  private TestRunner runner;

  private DeidentificationController deidentificationController;

  @Before
  public void setup() throws IOException, InitializationException {
    deidentifyDICOM = new DeidentifyDICOM();
    runner = TestRunners.newTestRunner(deidentifyDICOM);

    deidentificationController = new DeidentificationController();

    runner.addControllerService("dc", deidentificationController);
    runner.setProperty(deidentificationController, DeidentificationController.DB_DIRECTORY,
        folder.getRoot().getAbsolutePath());
    runner.setProperty(DeidentifyDICOM.DEIDENTIFICATION_STORAGE_CONTROLLER, "dc");
  }

  public static byte[] createPasswordEnvelopedObject(char[] passwd, byte[] salt, int iterationCount, byte[] data)
      throws GeneralSecurityException, CMSException, IOException {

    CMSEnvelopedDataGenerator envelopedGen = new CMSEnvelopedDataGenerator();
    envelopedGen.addRecipientInfoGenerator(new JcePasswordRecipientInfoGenerator(CMSAlgorithm.AES256_CBC, passwd)
        .setProvider("BCFIPS").setPasswordConversionScheme(PasswordRecipient.PKCS5_SCHEME2_UTF8)

        // .setPRF(PasswordRecipient.PRF.HMacSHA384)
        .setSaltAndIterationCount(salt, iterationCount));
    return envelopedGen.generate(new CMSProcessableByteArray(data),
        new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider("BCFIPS").build()).getEncoded();
  }

  public static byte[] extractPasswordEnvelopedData(char[] passwd, byte[] encEnvelopedData)
      throws GeneralSecurityException, CMSException {
    CMSEnvelopedData envelopedData = new CMSEnvelopedData(encEnvelopedData);
    RecipientInformationStore recipients = envelopedData.getRecipientInfos();
    RecipientId rid = new PasswordRecipientId();
    RecipientInformation recipient = recipients.get(rid);
    return recipient.getContent(new JcePasswordEnvelopedRecipient(passwd).setProvider("BCFIPS")
        .setPasswordConversionScheme(PasswordRecipient.PKCS5_SCHEME2_UTF8));
  }

  @Test
  public void attributeDifferences() throws Exception {
    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
    AttributeList list;
    // Read with Pixelmed
    try (com.pixelmed.dicom.DicomInputStream i = new com.pixelmed.dicom.DicomInputStream(r)) {
      list = new AttributeList();
      list.setDecompressPixelData(false);
      list.read(i);
    }

    // Read / write with DCM4CHE
    Attributes tags;
    Attributes deidentifiedTags;
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    list.write(os, TransferSyntax.ExplicitVRLittleEndian, true, true);
    try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(os.toByteArray()))) {
      dis.setIncludeBulkData(IncludeBulkData.NO);
      tags = dis.readDataset(-1, -1);
    }

    list.removeGroupLengthAttributes();
    list.correctDecompressedImagePixelModule(true);
    list.insertLossyImageCompressionHistoryIfDecompressed(true);
    list.removeMetaInformationHeaderAttributes();

    ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
    ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list, ClinicalTrialsAttributes.HandleUIDs.keep, true,
        true, true, false, false, false, ClinicalTrialsAttributes.HandleDates.keep, null/* epochForDateModification */,
        null/* earliestDateInSet */);

    os.reset();
    list.write(os, TransferSyntax.ExplicitVRLittleEndian, true, true);
    try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(os.toByteArray()))) {
      dis.setIncludeBulkData(IncludeBulkData.NO);
      deidentifiedTags = dis.readDataset(-1, -1);
    }
    Attributes differenceTags = tags.getRemovedOrModified(deidentifiedTags);
    differenceTags.remove(Tag.DeidentificationMethod);
    differenceTags.remove(Tag.DeidentificationMethodCodeSequence);

    // Write the difference tags to a byte buffer
    os.reset();
    try (DicomOutputStream dos = new DicomOutputStream(os, UID.ExplicitVRLittleEndian)) {
      dos.writeDataset(null, differenceTags);
    }

    // Add Bouncy Castle Fips provider using the JAR from
    // https://www.bouncycastle.org/fips-java/
    Security.addProvider(new BouncyCastleFipsProvider());

    // Encrypt the block
    String passwd = "passwd";
    String salt = "salt";

    // Note:
    // Need to install JCE Unlimited Strength Extension, both in the JRE and the JDK
    // https://stackoverflow.com/a/6388603/334619
    // See
    // $JAVA_HOME/jre/lib/security/
    // $JAVA_HOME/lib/security/
    byte[] encryptedBuffer = createPasswordEnvelopedObject(passwd.toCharArray(), salt.getBytes(), 100,
        os.toByteArray());

    byte[] decryptedBuffer = extractPasswordEnvelopedData(passwd.toCharArray(), encryptedBuffer);
    assertArrayEquals(os.toByteArray(), decryptedBuffer);

    // can we read the DICOM tags?
    try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(decryptedBuffer),
        UID.ExplicitVRLittleEndian)) {
      dis.setIncludeBulkData(IncludeBulkData.NO);
      Attributes newTags = dis.readDataset(-1, -1);
      Attributes modified = newTags.getRemovedOrModified(tags);
      assertEquals(0, modified.size());
    }

    /*
     * Tag.EncryptedAttributesSequence; Tag.EncryptedContentTransferSyntaxUID;
     * Tag.EncryptedContent; Tag.ModifiedAttributesSequence;
     */

  }

  @Test
  public void deidentify() throws IOException {
    // Queue up a DICOM file
    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
    runner.enqueue(r);

    setCSVFile("/map.csv");

    runner.assertValid();
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    // Check the deidentified attributes
    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("Deidentified PatientID", "1234", actualAttributes.getString(Tag.PatientID));
      assertEquals("Deidentified PatientName", "Doe^Jane^Alice^Mrs.^III", actualAttributes.getString(Tag.PatientName));
      assertEquals("Deidentified AccessionNumber", "1996733833677301", actualAttributes.getString(Tag.AccessionNumber));
    }

    // Check the database
    assertEquals("Number of UID mappings", 4, getNumberOfMappings());

  }

  @Test
  public void deidentifyMultiple() throws IOException {
    // Queue up a DICOM file
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));
    setCSVFile("/map.csv");

    runner.assertValid();
    runner.run(3);
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 3);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("Deidentified PatientID", "1234", actualAttributes.getString(Tag.PatientID));
      assertEquals("Deidentified PatientName", "Doe^Jane^Alice^Mrs.^III", actualAttributes.getString(Tag.PatientName));
      assertEquals("Deidentified AccessionNumber", "1996733833677301", actualAttributes.getString(Tag.AccessionNumber));
    }

    // Check the database
    assertEquals("Number of UID mappings", 6, getNumberOfMappings());
  }

  @Test
  public void notMatched() throws IOException {
    // Queue up a DICOM file
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));
    setCSVFile("/empty.csv");
    runner.setProperty(DeidentifyDICOM.generateIfNotMatchedProperty, "false");

    runner.assertValid();
    runner.run(3);
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_NOT_MATCHED, 3);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_NOT_MATCHED);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("Deidentified PatientID", "LGG-104", actualAttributes.getString(Tag.PatientID));
      assertEquals("Deidentified PatientName", "LGG-104", actualAttributes.getString(Tag.PatientName));
      assertEquals("Deidentified AccessionNumber", "7408465417966656", actualAttributes.getString(Tag.AccessionNumber));
    }
    assertEquals("Number of UID mappings", 0, getNumberOfMappings());
  }

  @Test
  public void generate() throws IOException {
    // Queue up a DICOM file
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));
    setCSVFile("/empty.csv");
    runner.setProperty(DeidentifyDICOM.generateIfNotMatchedProperty, "true");

    runner.assertValid();
    runner.run(3);
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 3);

    for (MockFlowFile flowFile : runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS)) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("Deidentified PatientID", "E16BA065442DC4C7305B40C6AE70189A",
          actualAttributes.getString(Tag.PatientID));
      assertEquals("Deidentified PatientName", "7600272A48E4412C1458F5D9B4522F5C",
          actualAttributes.getString(Tag.PatientName));
      assertEquals("Deidentified AccessionNumber", "1996733833677301", actualAttributes.getString(Tag.AccessionNumber));
    }
    assertEquals("Number of UID mappings", 6, getNumberOfMappings());
  }

  @Test
  public void noCSVFile() throws IOException {
    // Queue up a DICOM file
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));

    runner.setProperty(deidentificationController, DeidentificationController.DEIDENTIFICATION_MAP_CVS_FILE, "");
    runner.assertValid(deidentificationController);
    runner.enableControllerService(deidentificationController);

    runner.setProperty(DeidentifyDICOM.generateIfNotMatchedProperty, "true");

    runner.assertValid();
    runner.run(3);
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 3);

    for (MockFlowFile flowFile : runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS)) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertEquals("Deidentified PatientID", "E16BA065442DC4C7305B40C6AE70189A",
          actualAttributes.getString(Tag.PatientID));
      assertEquals("Deidentified PatientName", "7600272A48E4412C1458F5D9B4522F5C",
          actualAttributes.getString(Tag.PatientName));
      assertEquals("Deidentified AccessionNumber", "1996733833677301", actualAttributes.getString(Tag.AccessionNumber));
    }
    assertEquals("Number of UID mappings", 6, getNumberOfMappings());
  }

  @Test
  public void garbage() throws IOException {
    // Queue up a DICOM file
    runner.enqueue(getClass().getResourceAsStream("/empty.csv"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));
    setCSVFile("/empty.csv");
    runner.setProperty(DeidentifyDICOM.generateIfNotMatchedProperty, "true");

    runner.assertValid();
    runner.run(3);
    // 2 success, 1 failure
    assertEquals("Number of of files in success relationship", 2,
        runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS).size());
    assertEquals("Number of of files in reject relationship", 1,
        runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_REJECT).size());
    assertEquals("Number of UID mappings", 5, getNumberOfMappings());
  }

  private int getNumberOfMappings() {

    EmbeddedDataSource ds = new EmbeddedDataSource();
    ds.setDatabaseName(new File(folder.getRoot(), "database").getAbsolutePath());
    ds.setCreateDatabase("create");

    Jdbi jdbi = Jdbi.create(ds);

    jdbi.useHandle(handle -> {
      handle.createQuery("select * from uid_map").mapToMap().forEach(it -> {
        logger.debug(it.toString());
      });
    });

    return jdbi.withHandle(handle -> {
      return handle.createQuery("select count(*) from uid_map").mapTo(Integer.class).findOnly();
    });
  }

  private void setCSVFile(String string) throws IOException {
    // Copy CSV file
    InputStream r = getClass().getResourceAsStream(string);

    File csv = folder.newFile();

    byte[] buffer = new byte[r.available()];
    r.read(buffer);

    Files.write(buffer, csv);
    runner.setProperty(deidentificationController, DeidentificationController.DEIDENTIFICATION_MAP_CVS_FILE,
        csv.getAbsolutePath());
    runner.assertValid(deidentificationController);
    runner.enableControllerService(deidentificationController);
  }
}

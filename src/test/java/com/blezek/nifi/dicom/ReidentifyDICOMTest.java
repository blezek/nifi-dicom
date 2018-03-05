package com.blezek.nifi.dicom;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.Before;
import org.junit.Test;

import com.blezek.nifi.dicom.AttributeStorageController;
import com.blezek.nifi.dicom.DeidentifyDICOM;
import com.blezek.nifi.dicom.ReidentifyDICOM;
import com.google.common.io.Files;

public class ReidentifyDICOMTest {
  private ReidentifyDICOM reidentifyDICOM;
  private TestRunner runner;
  private AttributeStorageController attributeStorageController;
  File tempDir;

  @Before
  public void setup() throws IOException, InitializationException {
    reidentifyDICOM = new ReidentifyDICOM();
    runner = TestRunners.newTestRunner(reidentifyDICOM);

    attributeStorageController = new AttributeStorageController();
    tempDir = Files.createTempDir();
    runner.addControllerService("asc", attributeStorageController);
    runner.setProperty(attributeStorageController, AttributeStorageController.DIRECTORY, tempDir.getAbsolutePath());
    runner.enableControllerService(attributeStorageController);
    runner.assertValid(attributeStorageController);

    runner.setProperty(DeidentifyDICOM.ATTRIBUTE_STORAGE_CONTROLLER, "asc");
  }

  @Test
  public void reidentify() throws IOException, InitializationException {

    DeidentifyDICOM deidentifyDICOM = new DeidentifyDICOM();
    TestRunner ddRunner = TestRunners.newTestRunner(deidentifyDICOM);
    ddRunner.setProperty(DeidentifyDICOM.ATTRIBUTE_STORAGE_CONTROLLER, "asc");
    ddRunner.addControllerService("asc", attributeStorageController);
    ddRunner.setProperty(attributeStorageController, AttributeStorageController.DIRECTORY, tempDir.getAbsolutePath());
    ddRunner.enableControllerService(attributeStorageController);

    // Queue up a DICOM file
    InputStream r = getClass().getResourceAsStream("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");
    ddRunner.enqueue(r);
    ddRunner.setValidateExpressionUsage(false);
    ddRunner.run();
    ddRunner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    for (MockFlowFile ff : ddRunner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS)) {
      runner.enqueue(ff.toByteArray());
    }

    runner.run();
    runner.assertAllFlowFilesTransferred(ReidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    // Check the deidentified attributes
    Attributes attributes = TestUtil.getAttributes("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      assertThat("Deidentified StudyInstanceUID", actualAttributes.getString(Tag.StudyInstanceUID),
          equalTo(attributes.getString(Tag.StudyInstanceUID)));
    }
  }

}

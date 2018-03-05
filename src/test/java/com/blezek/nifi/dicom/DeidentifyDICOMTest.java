package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.blezek.nifi.dicom.AttributeStorageController;
import com.blezek.nifi.dicom.AttributesMap;
import com.blezek.nifi.dicom.DeidentifyDICOM;
import com.google.common.io.Files;

public class DeidentifyDICOMTest {

  private DeidentifyDICOM deidentifyDICOM;
  private TestRunner runner;
  private AttributeStorageController attributeStorageController;

  @Before
  public void setup() throws IOException, InitializationException {
    deidentifyDICOM = new DeidentifyDICOM();
    runner = TestRunners.newTestRunner(deidentifyDICOM);

    attributeStorageController = new AttributeStorageController();
    File tempDir = Files.createTempDir();
    runner.addControllerService("asc", attributeStorageController);
    runner.setProperty(attributeStorageController, AttributeStorageController.DIRECTORY, tempDir.getAbsolutePath());
    runner.enableControllerService(attributeStorageController);
    runner.assertValid(attributeStorageController);

    runner.setProperty(DeidentifyDICOM.ATTRIBUTE_STORAGE_CONTROLLER, "asc");
  }

  @Test
  public void deidentify() throws IOException {
    // Queue up a DICOM file
    InputStream r = getClass().getResourceAsStream("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");
    runner.enqueue(r);
    runner.setValidateExpressionUsage(false);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    // Check the deidentified attributes
    Attributes attributes = TestUtil.getAttributes("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);

      // Find the map
      Set<AttributesMap> maps = attributeStorageController.getAttributeStorage().getAttributesMap(actualAttributes.getString(Tag.StudyInstanceUID));
      assertEquals("one map generated", 1, maps.size());
      AttributesMap map = maps.toArray(new AttributesMap[1])[0];

      Attributes deidentified = map.deidentifed;

      Assert.assertEquals("Deidentified studyInstanceUID", actualAttributes.getString(Tag.StudyInstanceUID), deidentified.getString(Tag.StudyInstanceUID));
    }
  }

}

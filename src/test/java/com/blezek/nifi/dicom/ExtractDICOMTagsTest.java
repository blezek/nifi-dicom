package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import com.blezek.nifi.dicom.DeidentifyDICOM;
import com.blezek.nifi.dicom.ExtractDICOMTags;

public class ExtractDICOMTagsTest {
  private TestRunner runner;

  @Before
  public void setup() {
    runner = TestRunners.newTestRunner(new ExtractDICOMTags());
  }

  @Test
  public void extractModality() {

    runner.setProperty("Modality", "unknown");
    InputStream r = getClass().getResourceAsStream("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");
    runner.setProperty(ExtractDICOMTags.ALL_TAGS, "false");
    runner.enqueue(r);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      flowFile.assertAttributeEquals("Modality", "CT");
    }

  }

  @Test
  public void extractAllTags() {

    InputStream r = getClass().getResourceAsStream("/Denoising/CTE_4/Axial/IM-0002-0001.dcm");
    runner.setProperty(ExtractDICOMTags.ALL_TAGS, "true");
    runner.enqueue(r);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      assertEquals("flowfile has correct number of attributes", 100, flowFile.getAttributes().keySet().size());
      flowFile.assertAttributeEquals("Modality", "CT");
    }

  }

}
//
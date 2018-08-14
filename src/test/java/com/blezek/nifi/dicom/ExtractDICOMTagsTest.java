package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class ExtractDICOMTagsTest {
  private TestRunner runner;

  @Before
  public void setup() {
    runner = TestRunners.newTestRunner(new ExtractDICOMTags());
  }

  @Test
  public void extractModality() {

    runner.setProperty("Modality", "unknown");
    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
    runner.setProperty(ExtractDICOMTags.ALL_TAGS, "false");
    runner.enqueue(r);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      flowFile.assertAttributeEquals("Modality", "MR");
    }

  }

  @Test
  public void extractAllTags() {

    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
    runner.setProperty(ExtractDICOMTags.ALL_TAGS, "true");
    runner.enqueue(r);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      assertEquals("flowfile has correct number of attributes", 94, flowFile.getAttributes().keySet().size());
      flowFile.assertAttributeEquals("Modality", "MR");
    }

  }

  @Test
  public void filename() {

    runner.setProperty(ExtractDICOMTags.ALL_TAGS, "true");
    runner.setProperty(ExtractDICOMTags.CONSTRUCT_FILENAME, "true");
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm"));
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 3);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      String filename = flowFile.getAttribute("filename");
      String path = flowFile.getAttribute("path");
      assertTrue("path", path.equals("LGG_104/20000626_MR/5_Gad_Ax_SPGR_Straight/"));
      assertTrue("filename", filename.equals(flowFile.getAttribute("SOPInstanceUID") + ".dcm"));
    }

  }

}
//
package com.blezek.nifi.dicom;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ModifyDICOMTagsTest {
  private TestRunner runner;

  @Before
  public void setup() {
    runner = TestRunners.newTestRunner(new ModifyDICOMTags());
  }

  @Test
  public void changeModality() throws IOException {

    runner.setProperty("Modality", "MR");
    runner.setProperty("SliceThickness", "100");
    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");

    runner.enqueue(r);
    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
      Assert.assertEquals("Modified modality", "MR", actualAttributes.getString(Tag.Modality));
      Assert.assertEquals("Modified SliceThickness", "100", actualAttributes.getString(Tag.SliceThickness));
    }
  }

}

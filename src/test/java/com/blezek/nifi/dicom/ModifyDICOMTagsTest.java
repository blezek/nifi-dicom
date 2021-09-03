package com.blezek.nifi.dicom;

import com.google.common.collect.ImmutableMap;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModifyDICOMTagsTest {
    private TestRunner runner;

    @BeforeEach
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
            assertEquals("MR", actualAttributes.getString(Tag.Modality), "Modified modality");
            assertEquals("100", actualAttributes.getString(Tag.SliceThickness), "Modified SliceThickness");
        }
    }

    @Test
    public void rewritePatientID() throws IOException {

        ImmutableMap<String, String> attributes = ImmutableMap.of("PatientID", "1234", "PatientName", "Doe^John");
        runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"), attributes);
        runner.setProperty("PatientID", "${PatientID:base64Encode():substring(0,${PatientID:length()})}");
        runner.setProperty("PatientName", "${PatientName:base64Encode():substring(0,${PatientName:length()})}");
        runner.run();
        runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

        List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
        for (MockFlowFile flowFile : flowFiles) {
            Attributes actualAttributes = TestUtil.getAttributes(flowFile);
            assertEquals("MTIz", actualAttributes.getString(Tag.PatientID), "Modified PatientID");
            assertEquals("RG9lXkpv", actualAttributes.getString(Tag.PatientName), "Modified PatientName");
        }
    }

}

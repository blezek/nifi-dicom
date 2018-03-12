package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Files;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
	    assertEquals("Deidentified PatientName", "Doe^Jane^Alice^Mrs.^III",
		    actualAttributes.getString(Tag.PatientName));
	    assertEquals("Deidentified AccessionNumber", "1996733833677301",
		    actualAttributes.getString(Tag.AccessionNumber));
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
	    assertEquals("Deidentified PatientName", "Doe^Jane^Alice^Mrs.^III",
		    actualAttributes.getString(Tag.PatientName));
	    assertEquals("Deidentified AccessionNumber", "1996733833677301",
		    actualAttributes.getString(Tag.AccessionNumber));
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
	    assertEquals("Deidentified AccessionNumber", "7408465417966656",
		    actualAttributes.getString(Tag.AccessionNumber));
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
	    assertEquals("Deidentified AccessionNumber", "1996733833677301",
		    actualAttributes.getString(Tag.AccessionNumber));
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
	    assertEquals("Deidentified AccessionNumber", "1996733833677301",
		    actualAttributes.getString(Tag.AccessionNumber));
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

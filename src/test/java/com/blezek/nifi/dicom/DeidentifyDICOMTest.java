package com.blezek.nifi.dicom;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.dcm4che3.data.Attributes;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.zaxxer.hikari.HikariDataSource;

public class DeidentifyDICOMTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private DeidentifyDICOM deidentifyDICOM;
  private TestRunner runner;

  @Before
  public void setup() throws IOException, InitializationException {
    deidentifyDICOM = new DeidentifyDICOM();
    runner = TestRunners.newTestRunner(deidentifyDICOM);
  }

  @Test
  public void deidentify() throws IOException {
    // Queue up a DICOM file
    InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
    runner.enqueue(r);

    runner.setProperty(DeidentifyDICOM.dbDirectory, folder.getRoot().getAbsolutePath());

    runner.run();
    runner.assertAllFlowFilesTransferred(DeidentifyDICOM.RELATIONSHIP_SUCCESS, 1);

    // Check the deidentified attributes
    Attributes attributes = TestUtil.getAttributes("/dicom/LGG-104_SPGR_000.dcm");

    List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(DeidentifyDICOM.RELATIONSHIP_SUCCESS);
    for (MockFlowFile flowFile : flowFiles) {
      Attributes actualAttributes = TestUtil.getAttributes(flowFile);
    }

    // Check the database
    String dbPath = new File(folder.getRoot(), "database").getAbsolutePath();
    HikariDataSource ds = new HikariDataSource();
    ds.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
    ds.setJdbcUrl("jdbc:derby:" + dbPath + ";create=true");
    Jdbi jdbi = Jdbi.create(ds);
    assertEquals("Number of UID mappings", 4, jdbi.withHandle(handle -> {
      return handle.createQuery("select count(*) from uid_map").mapTo(Integer.class).findOnly();
    }));
  }
}

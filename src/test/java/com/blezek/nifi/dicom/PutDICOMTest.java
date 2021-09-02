package com.blezek.nifi.dicom;

import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class PutDICOMTest {
  private TestRunner runner;

  @BeforeEach
  public void setup() throws IOException {
    runner = TestRunners.newTestRunner(PutDICOM.class);
  }

  @Test
  public void validate() {
    runner.assertNotValid();

    // Make it valid
    runner.setProperty("BATCH_SIZE", "1");
    runner.assertNotValid();

    runner.setProperty("DICOM_PORT", "4096");
    runner.assertNotValid();

    runner.setProperty("CALLING_AE_TITLE", "nifi");
    runner.assertNotValid();

    runner.setProperty("CALLED_AE_TITLE", "nifi-remote");
    runner.assertNotValid();

    runner.setProperty("DICOM_HOSTNAME", "localhost");
    runner.assertValid();
  }

}

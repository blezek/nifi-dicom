package com.blezek.nifi.dicom;

import java.io.IOException;

import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import com.blezek.nifi.dicom.ListenDICOM;

public class ListenDICOMTest {

  private ListenDICOM proc;
  private TestRunner runner;

  private int availablePort;

  @Before
  public void setup() throws IOException {
    proc = new ListenDICOM();
    runner = TestRunners.newTestRunner(proc);
    availablePort = DICOMTransferTest.findAvailablePort();
    runner.setProperty("DICOM_PORT", Integer.toString(availablePort));
    runner.setProperty("AE_TITLE", "test");
  }

  @Test
  public void validate() {
    ListenDICOM processor = new ListenDICOM();
    TestRunner listenDICOM = TestRunners.newTestRunner(processor);
    listenDICOM.setProperty("DICOM_PORT", "4096");
    listenDICOM.setProperty("AE_TITLE", "test");
    listenDICOM.assertValid();
  }

  @Test
  public void emptyAE() {
    ListenDICOM processor = new ListenDICOM();
    TestRunner listenDICOM = TestRunners.newTestRunner(processor);
    listenDICOM.setProperty("DICOM_PORT", "4096");
    listenDICOM.setProperty("AE_TITLE", "");
    listenDICOM.assertValid();
  }

}

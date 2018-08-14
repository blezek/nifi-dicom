package com.blezek.nifi.dicom;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

@DisplayName("the PutBox processor uploads files to Box")
class PutBoxTest {
  static String userId = "testuser";
  BoxController boxController;
  PutBox putBox;
  TestRunner runner;

  @BeforeEach
  void configureBox() throws IOException, InitializationException {
    putBox = new PutBox();
    runner = TestRunners.newTestRunner(putBox);
    boxController = new BoxController();

    runner.addControllerService("boxController", boxController);

    URL url = Resources.getResource("boxToken.txt");
    String token = Resources.toString(url, Charsets.UTF_8).trim();
    runner.setProperty(boxController, BoxController.DeveloperToken, token);
    runner.setProperty(boxController, BoxController.UserID, userId);
    runner.setProperty(boxController, BoxController.UserSpace, "2000");

    runner.setProperty(PutBox.BOX_CONTROLLER, "boxController");
    runner.enableControllerService(boxController);
  }

  @Test
  @DisplayName("when given a valid configuration, BoxController and PutBox are valid")
  void validate() {
    runner.assertValid(boxController);
    runner.assertValid();
  }

  @Test
  @DisplayName("when UserId is blank, BoxController is invalid")
  void whenUserIdIsBlankBoxControllerIsInvalid() {
    runner.disableControllerService(boxController);
    runner.setProperty(boxController, BoxController.UserID, "");
    runner.assertNotValid(boxController);
  }

  @Test
  @DisplayName("when UserSpace is less than zero, BoxController is invalid")
  void whenUserSpaceIsLessThanZeroBoxControllerIsInvalid() {
    runner.disableControllerService(boxController);
    runner.setProperty(boxController, BoxController.UserSpace, "-1");
    runner.assertNotValid(boxController);
  }

  @Test
  @DisplayName("when DeveloperToken and AppSettings are empty, BoxController is invalid")
  void whenDeveloperTokenAndAppSettingsAreEmptyBoxControllerIsInvalid() {
    runner.disableControllerService(boxController);
    runner.setProperty(boxController, BoxController.DeveloperToken, "");
    runner.setProperty(boxController, BoxController.AppSettings, "");
    runner.assertNotValid(boxController);
  }

  @Test
  @DisplayName("when PutBox is given FlowFiles, the files are uploaded to Box")
  void uploadFiles() {
    runner.assertValid(boxController);
    runner.assertValid();
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm"),
        ImmutableMap.of("path", "/path/to/dicom/", "filename", "000.dcm"));
    runner.enqueue(getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm"),
        ImmutableMap.of("path", "/path/to/dicom/", "filename", "001.dcm"));

    runner.assertValid();
    runner.run();
    // All the files were uploaded without problem
    runner.assertAllFlowFilesTransferred(PutBox.SUCCESS, 2);
    runner.assertTransferCount(PutBox.FAILURE, 0);
  }

  @Test
  @DisplayName("when BoxController does not have a valid token, the files are routed to FAILURE relationship")
  void whenNoBoxConnectionRouteToFAILURE() {
    runner.disableControllerService(boxController);
    runner.setProperty(boxController, BoxController.DeveloperToken, "this-is-an-invalid-token");
  }

}

package com.blezek.nifi.dicom;

import com.box.sdk.BoxAPIConnection;

import org.apache.nifi.controller.ControllerService;

public interface BoxAPIService extends ControllerService {
  BoxAPIConnection getConnection();
}

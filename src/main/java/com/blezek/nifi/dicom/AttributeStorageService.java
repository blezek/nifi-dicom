package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.controller.ControllerService;

@Tags({ "dicom", "imaging", "deidentification", "reidentification" })
@CapabilityDescription("This controller implements attribute storage for re-identifying DICOM images")
public interface AttributeStorageService extends ControllerService {
  public AttributeStorage getAttributeStorage();
}

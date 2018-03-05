package com.blezek.nifi.dicom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.processor.util.StandardValidators;

@Tags({ "dicom", "imaging", "deidentification", "reidentification" })
@CapabilityDescription("This controller implements attribute storage for re-identifying DICOM images")
@SeeAlso({ ListenDICOM.class, PutDICOM.class, DeidentifyDICOM.class, ReidentifyDICOM.class })
public class AttributeStorageController extends AbstractControllerService implements AttributeStorageService {

  public static final PropertyDescriptor DIRECTORY = new PropertyDescriptor.Builder()
      .name("Input Directory")
      .displayName("Database path").description("Path to a directory where de- and re-identification data is to be stored")
      .required(true)
      .addValidator(StandardValidators.createDirectoryExistsValidator(true, true))
      .expressionLanguageSupported(true)
      .build();

  private static final List<PropertyDescriptor> properties;
  static {

    // descriptors
    final List<PropertyDescriptor> supDescriptors = new ArrayList<>();
    supDescriptors.add(DIRECTORY);
    properties = Collections.unmodifiableList(supDescriptors);
  }

  final AttributeStorage attributeStorage = new MockAttributeStorage();

  public AttributeStorage getAttributeStorage() {
    return attributeStorage;
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    return properties;
  }

}

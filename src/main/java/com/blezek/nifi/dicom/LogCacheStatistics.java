package com.blezek.nifi.dicom;

import com.google.gson.Gson;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({ "dicom", "deidentify", "imaging" })
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("Fetches deidentification service statistics")
public class LogCacheStatistics extends AbstractProcessor {

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("Log statistics").build();
  // Properties
  public static final PropertyDescriptor DEIDENTIFICATION_STORAGE_CONTROLLER = new PropertyDescriptor.Builder()
      .name("Deidentification controller")
      .description("Specified the deidentification controller for DICOM deidentification").required(true)
      .identifiesControllerService(DeidentificationService.class).build();

  @Override
  public Set<Relationship> getRelationships() {
    HashSet<Relationship> relationships = new HashSet<>();
    relationships.add(RELATIONSHIP_SUCCESS);
    return relationships;
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    List<PropertyDescriptor> properties = new ArrayList<>();
    properties.add(DEIDENTIFICATION_STORAGE_CONTROLLER);
    return properties;
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    DeidentificationService controller = context.getProperty(DEIDENTIFICATION_STORAGE_CONTROLLER)
        .asControllerService(DeidentificationService.class);
    FlowFile flowfile = session.create();
    flowfile = session.write(flowfile, (OutputStream out) -> {
      Gson gson = new Gson();
      out.write(gson.toJson(controller.getCacheStats()).getBytes());
    });
    session.putAttribute(flowfile, "filename", "cache_stats.json");
    session.transfer(flowfile, RELATIONSHIP_SUCCESS);
  }

}

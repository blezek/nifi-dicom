package com.blezek.nifi.dicom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;

@Tags({ "reidentify", "dicom", "imaging" })
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM re-identifier.  The ReidentifyDICOM processor restores DICOM tags, replacing deidentified values with the original values. It uses the attributes stored by the DeidentifyDICOM Processor.")
@SeeAlso(DeidentifyDICOM.class)
public class ReidentifyDICOM extends AbstractProcessor {

  public static final PropertyDescriptor ATTRIBUTE_STORAGE_CONTROLLER = new PropertyDescriptor.Builder()
      .name("Attribute Storage Service")
      .description("Specified the Attribute Storage Service for storing DICOM de-identification data for later re-identification")
      .required(true)
      .identifiesControllerService(AttributeStorageService.class)
      .build();

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All reidentified DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("reject")
      .description("FlowFiles that are not DICOM images").build();

  private static final List<PropertyDescriptor> properties;
  private static final Set<Relationship> relationships;
  static {
    // relationships
    final Set<Relationship> procRels = new HashSet<>();
    procRels.add(RELATIONSHIP_SUCCESS);
    procRels.add(RELATIONSHIP_REJECT);
    relationships = Collections.unmodifiableSet(procRels);

    // descriptors
    final List<PropertyDescriptor> supDescriptors = new ArrayList<>();
    supDescriptors.add(ATTRIBUTE_STORAGE_CONTROLLER);
    properties = Collections.unmodifiableList(supDescriptors);
  }

  @Override
  public Set<Relationship> getRelationships() {
    return relationships;
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    return properties;
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    List<FlowFile> flowfiles = session.get(100);
    AttributeStorageService controller = context.getProperty(ATTRIBUTE_STORAGE_CONTROLLER).asControllerService(AttributeStorageService.class);
    AttributeStorage attributeStorage = controller.getAttributeStorage();
    for (FlowFile flowfile : flowfiles) {
      Optional<Relationship> destinationRelationship = Optional.empty();
      try (InputStream flowfileInputStream = session.read(flowfile)) {
        try (DicomInputStream in = new DicomInputStream(flowfileInputStream)) {
          in.setIncludeBulkData(IncludeBulkData.URI);
          Attributes attributes;
          Attributes fmi = in.readFileMetaInformation();
          attributes = in.readDataset(-1, -1);

          Reidentify reidentifyDICOM = new Reidentify();
          Attributes reidentified = reidentifyDICOM.reidentify(attributeStorage, attributes);

          // Create a new FlowFile
          FlowFile outputFlowFile = session.create();
          outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
            try (DicomOutputStream dos = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
              dos.writeDataset(fmi, reidentified);
            } catch (IOException e) {
              getLogger().error("Could not write output file", e);
              throw e;
            }
          });

          // Copy all attributes
          session.putAllAttributes(outputFlowFile, flowfile.getAttributes());
          session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);

        } catch (IOException e) {
          destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
          getLogger().error("Flowfile is nat a DICOM file, could not read attributes", e);
        }
      } catch (IOException dse) {
        destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
        getLogger().error("Flowfile is nat a DICOM file", dse);
        continue;
      }
      if (destinationRelationship.isPresent()) {
        session.transfer(flowfile, destinationRelationship.get());
      } else {
        // successfully processed
        session.remove(flowfile);
      }
    }
    session.commit();
  }

}

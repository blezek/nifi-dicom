package com.blezek.nifi.dicom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.ClinicalTrialsAttributes.HandleUIDs;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.VersionAndConstants;

@Tags({ "deidentify", "dicom", "imaging" })
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM deidentifier.  The DeidentifyDICOM processor substitutes DICOM tags with deidentified values and stores the values for later re-identification by the ReidentifyDICOM Processor.")
@SeeAlso(ReidentifyDICOM.class)
public class DeidentifyDICOM extends AbstractProcessor {
  public static final PropertyDescriptor ATTRIBUTE_STORAGE_CONTROLLER = new PropertyDescriptor.Builder()
      .name("Attribute Storage Service")
      .description("Specified the Attribute Storage Service for storing DICOM de-identification data for later re-identification")
      .required(true)
      .identifiesControllerService(AttributeStorageService.class)
      .build();

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
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

      try {
        // deidentifyUsingDCM4CHE(session, attributeStorage, flowfile);
        deidentifyUsingPixelMed(session, attributeStorage, flowfile);

      } catch (Exception e) {
        destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
        getLogger().error("Flowfile is nat a DICOM file, could not read attributes", e);
      }

      // If we reject
      if (destinationRelationship.isPresent()) {
        session.transfer(flowfile, destinationRelationship.get());
      } else {
        // remove the flowfile from the queue
        session.remove(flowfile);
      }
    }
    session.commit();
  }

  private void deidentifyUsingDCM4CHE(ProcessSession session, AttributeStorage attributeStorage, FlowFile flowfile) throws Exception {
    try (InputStream flowfileInputStream = session.read(flowfile)) {
      try (DicomInputStream in = new DicomInputStream(flowfileInputStream)) {
        in.setIncludeBulkData(IncludeBulkData.URI);
        Attributes attributes;
        Attributes fmi = in.readFileMetaInformation();
        attributes = in.readDataset(-1, -1);

        Deidentify deidentifyDICOM = new Deidentify();
        Attributes deidentified = deidentifyDICOM.deidentify(attributes);

        // Create a new FlowFile
        FlowFile outputFlowFile = session.create();
        outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
          try (DicomOutputStream dos = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
            dos.writeDataset(fmi, deidentified);
          } catch (Exception e) {
            getLogger().error("Could not write output file", e);
            throw e;
          }
        });

        // Copy all attributes
        session.putAllAttributes(outputFlowFile, flowfile.getAttributes());

        // Save attributes
        attributeStorage.storeAttributes(new AttributesMap(attributes, deidentified));
        session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);
      }
    }
  }

  void deidentifyUsingPixelMed(ProcessSession session, AttributeStorage attributeStorage, FlowFile flowfile) throws Exception {
    boolean keepAllPrivate = false;
    boolean addContributingEquipmentSequence = true;
    String ourCalledAETitle = "FLOWEY";
    AttributeList list;
    Attributes originalAttributes;
    final Attributes deidentifiedAttributes;
    String outputTransferSyntaxUID = TransferSyntax.ExplicitVRLittleEndian;

    try (InputStream ffin = session.read(flowfile)) {
      // Read into DCM4CHE
      try (DicomInputStream in = new DicomInputStream(ffin)) {
        in.setIncludeBulkData(IncludeBulkData.NO);
        originalAttributes = in.readDataset(-1, Tag.PixelData);
      }
    }

    try (InputStream flowfileInputStream = session.read(flowfile)) {

      try (com.pixelmed.dicom.DicomInputStream i = new com.pixelmed.dicom.DicomInputStream(flowfileInputStream)) {
        list = new AttributeList();

        // Do not decompress the pixel data if we can redact the JPEG (unless
        // overriddden), but let AttributeList.read() decompress the pixel data
        // for all other formats (lossy or not)
        list.setDecompressPixelData(false);
        list.read(i);
      }
    }

    list.removeGroupLengthAttributes();
    list.correctDecompressedImagePixelModule(true);
    list.insertLossyImageCompressionHistoryIfDecompressed(true);
    list.removeMetaInformationHeaderAttributes();

    ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
    ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,
        ClinicalTrialsAttributes.HandleUIDs.keep,
        true/* keepDescriptors */,
        true/* keepSeriesDescriptors */,
        true/* keepProtocolName */,
        true/* keepPatientCharacteristics */,
        true/* keepDeviceIdentity */,
        true/* keepInstitutionIdentity */,
        ClinicalTrialsAttributes.HandleDates.keep,
        null/* epochForDateModification */,
        null/* earliestDateInSet */);

    String id = "unknown";
    if (list.containsKey(TagFromName.PatientID)) {
      id = list.get(TagFromName.PatientID).getSingleStringValueOrDefault("unknown");
    }
    String newName = "foo" + attributeStorage.counterForId(TagFromName.PatientID.toString(), id);
    {
      AttributeTag tag = TagFromName.PatientName;
      list.remove(tag);
      Attribute a = new PersonNameAttribute(tag);
      a.addValue(newName);
      list.put(tag, a);
    }

    String newID = "bar";
    {
      AttributeTag tag = TagFromName.PatientID;
      list.remove(tag);
      Attribute a = new LongStringAttribute(tag);
      a.addValue(newID);
      list.put(tag, a);
    }
    {
      String newAccessionNumber = "an";
      {
        AttributeTag tag = TagFromName.AccessionNumber;
        list.remove(tag);
        Attribute a = new ShortStringAttribute(tag);
        a.addValue(newAccessionNumber);
        list.put(tag, a);
      }
    }

    Attribute aDeidentificationMethod = list.get(TagFromName.DeidentificationMethod);
    SequenceAttribute aDeidentificationMethodCodeSequence = (SequenceAttribute) (list.get(TagFromName.DeidentificationMethodCodeSequence));

    aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("113101", "DCM", "Clean Pixel Data Option").getAttributeList());
    {
      Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation);
      a.addValue("NO");
      list.put(a);
    }

    if (keepAllPrivate) {
      aDeidentificationMethod.addValue("All private retained");
      aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("210002", "99PMP", "Retain all private elements").getAttributeList());
    } else {
      list.removeUnsafePrivateAttributes();
      aDeidentificationMethod.addValue("Unsafe private removed");
      aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("113111", "DCM", "Retain Safe Private Option").getAttributeList());
    }

    // ToDo: Make sure UIDS are remapped consistently
    // ClinicalTrialsAttributes.remapUIDAttributes(list);
    List<AttributeTag> forRemovalOrRemapping = ClinicalTrialsAttributes.findUIDToRemap(list, HandleUIDs.remap);
    Iterator<AttributeTag> i2 = forRemovalOrRemapping.iterator();
    while (i2.hasNext()) {
      AttributeTag tag = (i2.next());
      String originalUIDValue = Attribute.getSingleStringValueOrNull(list, tag);
      if (originalUIDValue != null) {
        String replacementUIDValue = attributeStorage.mapOrCreateUID(originalUIDValue);
        list.remove(tag);
        Attribute a = new UniqueIdentifierAttribute(tag);
        a.addValue(replacementUIDValue);
        list.put(tag, a);
      } else {
        list.remove(tag);
      }
    }

    aDeidentificationMethod.addValue("UIDs remapped");

    {
      // remove the default Retain UIDs added by
      // ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes() with
      // the ClinicalTrialsAttributes.HandleUIDs.keep option
      Iterator<SequenceItem> it = aDeidentificationMethodCodeSequence.iterator();
      while (it.hasNext()) {
        SequenceItem item = it.next();
        if (item != null) {
          CodedSequenceItem testcsi = new CodedSequenceItem(item.getAttributeList());
          if (testcsi != null) {
            String cv = testcsi.getCodeValue();
            String csd = testcsi.getCodingSchemeDesignator();
            if (cv != null && cv.equals("113110") && csd != null && csd.equals("DCM")) {
              it.remove();
            }
          }
        }
      }
    }
    aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("210001", "99PMP", "Remap UIDs").getAttributeList());

    if (addContributingEquipmentSequence) {
      ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
          true,
          new CodedSequenceItem("109104", "DCM", "De-identifying Equipment"),
          "PixelMed", // Manufacturer
          null, // Institution Name
          null, // Institutional Department Name
          null, // Institution Address
          ourCalledAETitle, // Station Name
          "DeidentifyAndRedact.main()", // Manufacturer's Model Name
          null, // Device Serial Number
          VersionAndConstants.getBuildDate(), // Software Version(s)
          "Deidentified and Redacted");
    }

    FileMetaInformation.addFileMetaInformation(list, outputTransferSyntaxUID, ourCalledAETitle);
    list.insertSuitableSpecificCharacterSetForAllStringValues();

    FlowFile outputFlowFile = session.create();
    outputFlowFile = session.write(outputFlowFile, (
        OutputStream out) -> {
      try {
        list.write(out, outputTransferSyntaxUID, true, true);
      } catch (DicomException e) {
        throw new IOException("Could not write " + e.getLocalizedMessage(), e);
      }
    });

    try (
        InputStream ffin = session.read(outputFlowFile)) {
      // Read into DCM4CHE
      try (DicomInputStream in = new DicomInputStream(ffin)) {
        in.setIncludeBulkData(IncludeBulkData.NO);
        deidentifiedAttributes = in.readDataset(-1, Tag.PixelData);
      }
    }

    attributeStorage.storeAttributes(new AttributesMap(originalAttributes, deidentifiedAttributes));

    // Copy all attributes
    session.putAllAttributes(outputFlowFile, flowfile.getAttributes());
    session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);

  }

}

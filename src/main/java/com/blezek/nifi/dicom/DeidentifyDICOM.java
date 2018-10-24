package com.blezek.nifi.dicom;

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

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Tags({ "deidentify", "dicom", "imaging" })
@SupportsBatching
@SideEffectFree
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM deidentifier.  The DeidentifyDICOM processor substitutes DICOM tags with deidentified values and stores the values.")
public class DeidentifyDICOM extends AbstractProcessor {

  // Relationships
  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_NOT_MATCHED = new Relationship.Builder().name("not_matched")
      .description("DICOM files that do not match the patient remapping are routed to this relationship").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("failure")
      .description("FlowFiles that are not DICOM images").build();

  // Properties
  public static final PropertyDescriptor DEIDENTIFICATION_STORAGE_CONTROLLER = new PropertyDescriptor.Builder()
      .name("Deidentification controller")
      .description("Specified the deidentification controller for DICOM deidentification").required(true)
      .identifiesControllerService(DeidentificationService.class).build();

  static final PropertyDescriptor generateIfNotMatchedProperty = new PropertyDescriptor.Builder()
      .name("Generate identification")
      .description("Create generated identifiers if the patient name did not match the Identifier CSV file")
      .required(true).allowableValues("true", "false").defaultValue("false").build();

  static final PropertyDescriptor keepDescriptorsProperty = new PropertyDescriptor.Builder().name("Keep descriptors")
      .description("Keep text description and comment attributes").required(true).allowableValues("true", "false")
      .defaultValue("true").build();

  static final PropertyDescriptor keepSeriesDescriptorsProperty = new PropertyDescriptor.Builder()
      .name("Keep series descriptors")
      .description("Keep the series description even if all other descriptors are removed").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  static final PropertyDescriptor keepProtocolNameProperty = new PropertyDescriptor.Builder().name("Keep protocol name")
      .description("Keep protocol name even if all other descriptors are removed").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  static final PropertyDescriptor keepPatientCharacteristicsProperty = new PropertyDescriptor.Builder()
      .name("Keep patient characteristics")
      .description("Keep patient characteristics (such as might be needed for PET SUV calculations)").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  static final PropertyDescriptor keepDeviceIdentityProperty = new PropertyDescriptor.Builder()
      .name("Keep device identity").description("Keep device identity").required(true).allowableValues("true", "false")
      .defaultValue("true").build();

  static final PropertyDescriptor keepInstitutionIdentityProperty = new PropertyDescriptor.Builder()
      .name("Keep institution identity").description("Keep institution identity").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  static final PropertyDescriptor keepAllPrivateProperty = new PropertyDescriptor.Builder().name("Keep private tags")
      .description("Keep all private tags.  If set to 'false', all unsafe private tags are removed.").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  static final PropertyDescriptor addContributingEquipmentSequenceProperty = new PropertyDescriptor.Builder()
      .name("Add contributing equipment sequence")
      .description("Add tags indicating the software used for deidentification").required(true)
      .allowableValues("true", "false").defaultValue("true").build();

  private List<PropertyDescriptor> properties;
  private Set<Relationship> relationships;
  static final DecimalFormat df = new DecimalFormat("#.00");

  @Override
  public void init(ProcessorInitializationContext context) {
    // relationships
    final Set<Relationship> procRels = new HashSet<>();
    procRels.add(RELATIONSHIP_SUCCESS);
    procRels.add(RELATIONSHIP_NOT_MATCHED);
    procRels.add(RELATIONSHIP_REJECT);
    relationships = Collections.unmodifiableSet(procRels);

    // descriptors
    final List<PropertyDescriptor> descriptors = new ArrayList<>();
    descriptors.add(DEIDENTIFICATION_STORAGE_CONTROLLER);
    descriptors.add(generateIfNotMatchedProperty);
    descriptors.add(keepDescriptorsProperty);
    descriptors.add(keepSeriesDescriptorsProperty);
    descriptors.add(keepProtocolNameProperty);
    descriptors.add(keepPatientCharacteristicsProperty);
    descriptors.add(keepDeviceIdentityProperty);
    descriptors.add(keepInstitutionIdentityProperty);
    descriptors.add(keepAllPrivateProperty);
    descriptors.add(addContributingEquipmentSequenceProperty);
    properties = Collections.unmodifiableList(descriptors);
  }

  @Override
  public Set<Relationship> getRelationships() {
    return relationships;
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    return properties;
  }

  boolean keepDescriptors;
  boolean keepSeriesDescriptors;
  boolean keepProtocolName;
  boolean keepPatientCharacteristics;
  boolean keepDeviceIdentity;
  boolean keepInstitutionIdentity;
  boolean keepAllPrivate;
  boolean addContributingEquipmentSequence = true;
  boolean generateIfNotMatched;

  @OnScheduled
  public void startup(ProcessContext context) throws Exception {
    // Shutdown anything in progress
    keepDescriptors = context.getProperty(keepDescriptorsProperty).asBoolean();
    keepSeriesDescriptors = context.getProperty(keepSeriesDescriptorsProperty).asBoolean();
    keepProtocolName = context.getProperty(keepProtocolNameProperty).asBoolean();
    keepPatientCharacteristics = context.getProperty(keepPatientCharacteristicsProperty).asBoolean();
    keepDeviceIdentity = context.getProperty(keepDeviceIdentityProperty).asBoolean();
    keepInstitutionIdentity = context.getProperty(keepInstitutionIdentityProperty).asBoolean();
    keepAllPrivate = context.getProperty(keepAllPrivateProperty).asBoolean();
    addContributingEquipmentSequence = context.getProperty(addContributingEquipmentSequenceProperty).asBoolean();
    generateIfNotMatched = context.getProperty(generateIfNotMatchedProperty).asBoolean();

  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

    FlowFile flowFile = session.get();
    if (flowFile == null) {
      return;
    }

    DeidentificationService controller = context.getProperty(DEIDENTIFICATION_STORAGE_CONTROLLER)
        .asControllerService(DeidentificationService.class);

    try {
      // deidentifyUsingDCM4CHE(session, attributeStorage, flowfile);
      deidentifyUsingPixelMed(controller, context, session, flowFile);

    } catch (Exception e) {
      flowFile = session.penalize(flowFile);
      session.transfer(flowFile, RELATIONSHIP_REJECT);
      getLogger().error("Flowfile is not a DICOM file, could not read attributes", e);
    }
    session.commit();
  }

  void deidentifyUsingPixelMed(DeidentificationService controller, ProcessContext context, ProcessSession session,
      FlowFile flowfile) throws Exception {
    String ourCalledAETitle = "nifi-dicom";
    AttributeList list;
    String outputTransferSyntaxUID = TransferSyntax.ExplicitVRLittleEndian;

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

    // Grab the patient id before we do anything else
    String id = "unknown";
    if (list.containsKey(TagFromName.PatientID)) {
      id = list.get(TagFromName.PatientID).getSingleStringValueOrDefault("unknown");
    }

    Optional<IdentityEntry> newId = controller.lookupById(id);
    if (!newId.isPresent()) {
      if (generateIfNotMatched) {
        String oldName = "Unknown^Pat";
        if (list.containsKey(TagFromName.PatientName)) {
          oldName = list.get(TagFromName.PatientName).getSingleStringValueOrDefault(oldName);
        }
        newId = Optional.of(IdentityEntry.createPseudoEntry(id, oldName));
      } else {
        // We can exit early, there is nothing to do
        // Transfer the incoming flowfile to the Not Matched relationship
        session.transfer(flowfile, RELATIONSHIP_NOT_MATCHED);
        return;
      }
    }

    list.removeGroupLengthAttributes();
    list.correctDecompressedImagePixelModule(true);
    list.insertLossyImageCompressionHistoryIfDecompressed(true);
    list.removeMetaInformationHeaderAttributes();

    ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
    ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list, ClinicalTrialsAttributes.HandleUIDs.keep,
        keepDescriptors, keepSeriesDescriptors, keepProtocolName, keepPatientCharacteristics, keepDeviceIdentity,
        keepInstitutionIdentity, ClinicalTrialsAttributes.HandleDates.keep, null/* epochForDateModification */,
        null/* earliestDateInSet */);

    if (newId.isPresent()) {
      AttributeTag tag;
      Attribute a;

      tag = TagFromName.PatientName;
      list.remove(tag);
      a = new PersonNameAttribute(tag);
      a.addValue(newId.get().getDeidentifiedPatientName());
      list.put(tag, a);

      // Deidentify PatientId
      tag = TagFromName.PatientID;
      list.remove(tag);
      a = new LongStringAttribute(tag);
      a.addValue(newId.get().getDeidentifiedPatientId());
      list.put(tag, a);

      // Deidentify Accession number
      tag = TagFromName.AccessionNumber;
      String an = list.get(tag).getSingleStringValueOrDefault("0");
      list.remove(tag);
      a = new ShortStringAttribute(tag);
      a.addValue(newId.get().generateAccessionNumber(an));
      list.put(tag, a);

    }

    Attribute aDeidentificationMethod = list.get(TagFromName.DeidentificationMethod);
    SequenceAttribute aDeidentificationMethodCodeSequence = (SequenceAttribute) (list
        .get(TagFromName.DeidentificationMethodCodeSequence));

    aDeidentificationMethodCodeSequence
        .addItem(new CodedSequenceItem("113101", "DCM", "Clean Pixel Data Option").getAttributeList());
    {
      Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation);
      a.addValue("NO");
      list.put(a);
    }

    if (keepAllPrivate) {
      aDeidentificationMethod.addValue("All private retained");
      aDeidentificationMethodCodeSequence
          .addItem(new CodedSequenceItem("210002", "99PMP", "Retain all private elements").getAttributeList());
    } else {
      list.removeUnsafePrivateAttributes();
      aDeidentificationMethod.addValue("Unsafe private removed");
      aDeidentificationMethodCodeSequence
          .addItem(new CodedSequenceItem("113111", "DCM", "Retain Safe Private Option").getAttributeList());
    }

    // ToDo: Make sure UIDS are remapped consistently
    // ClinicalTrialsAttributes.remapUIDAttributes(list);
    List<AttributeTag> forRemovalOrRemapping = ClinicalTrialsAttributes.findUIDToRemap(list, HandleUIDs.remap);
    Iterator<AttributeTag> i2 = forRemovalOrRemapping.iterator();
    while (i2.hasNext()) {
      AttributeTag tag = (i2.next());
      String originalUIDValue = Attribute.getSingleStringValueOrNull(list, tag);
      if (originalUIDValue != null) {
        String replacementUIDValue = controller.mapUid(originalUIDValue);
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
    aDeidentificationMethodCodeSequence
        .addItem(new CodedSequenceItem("210001", "99PMP", "Remap UIDs").getAttributeList());

    if (addContributingEquipmentSequence) {
      ClinicalTrialsAttributes.addContributingEquipmentSequence(list, true,
          new CodedSequenceItem("109104", "DCM", "De-identifying Equipment"), "com.blezek.nifi-dicom", // Manufacturer
          null, // Institution Name
          null, // Institutional Department Name
          null, // Institution Address
          ourCalledAETitle, // Station Name
          "nifi-dicom", // Manufacturer's Model Name
          null, // Device Serial Number
          "1.0", // Software Version(s)
          "Deidentified and Redacted");
    }

    FileMetaInformation.addFileMetaInformation(list, outputTransferSyntaxUID, ourCalledAETitle);
    list.insertSuitableSpecificCharacterSetForAllStringValues();

    FlowFile outputFlowFile = session.create(flowfile);
    outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
      try {
        list.write(out, outputTransferSyntaxUID, true, true);
      } catch (DicomException e) {
        throw new IOException("Could not write " + e.getLocalizedMessage(), e);
      }
    });
    // Remove the incoming flowfile from the input queue, transfer the new
    // deidentified file to the output
    session.remove(flowfile);
    session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);
  }

}

package com.blezek.nifi.dicom;

import com.blezek.nifi.dicom.util.Encryption;
import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.ClinicalTrialsAttributes.HandleUIDs;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CodedSequenceItem;
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
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.TagUtils;
import org.dcm4che3.util.UIDUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Tags({ "deidentify", "dicom", "imaging", "encrypt" })
@SupportsBatching
@SideEffectFree
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM deidentifier.  Deidentified DICOM tags are encrypted using a password for later decription and re-identification.")
@SeeAlso(DecryptReidentifyDICOM.class)
public class DeidentifyEncryptDICOM extends AbstractProcessor {

  public static final String PRIVATE_CREATOR = "nifi-dicom private";
  public static final int PRIVATE_TAG = TagUtils.toTag(0x47, 0x10);

  // Relationships
  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("failure")
      .description("FlowFiles that are not DICOM images").build();

  // Properties
  static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder().name("password")
      .displayName("Encryption password")
      .description(
          "Encryption password, leave empty or unset if deidintified or removed attributes are not to be encripted")
      .required(false).expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).sensitive(false)
      .addValidator(StandardValidators.ATTRIBUTE_EXPRESSION_LANGUAGE_VALIDATOR)
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR).build();

  static final PropertyDescriptor ITERATIONS = new PropertyDescriptor.Builder().name("iterations")
      .displayName("Encryption iterations")
      .description(
          "Number of encription rounds.  Higher number of iterations are typically more secure, but require more per-image computation")
      .required(false).defaultValue("100").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
      .addValidator(StandardValidators.INTEGER_VALIDATOR).addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
      .build();

  // static final PropertyDescriptor BATCH_SIZE = new
  // PropertyDescriptor.Builder().name("Batch size").defaultValue("100")
  // .description("Number of DICOM files to process in batch").required(false)
  // .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
  // .expressionLanguageSupported(ExpressionLanguageScope.NONE).build();

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

  static final DecimalFormat df = new DecimalFormat("#.00");

  @Override
  public Set<Relationship> getRelationships() {
    // relationships
    final Set<Relationship> procRels = new HashSet<>();
    procRels.add(RELATIONSHIP_SUCCESS);
    procRels.add(RELATIONSHIP_REJECT);
    return Collections.unmodifiableSet(procRels);
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    // descriptors
    final List<PropertyDescriptor> descriptors = new ArrayList<>();
    descriptors.add(PASSWORD);
    descriptors.add(ITERATIONS);
    descriptors.add(keepDescriptorsProperty);
    descriptors.add(keepSeriesDescriptorsProperty);
    descriptors.add(keepProtocolNameProperty);
    descriptors.add(keepPatientCharacteristicsProperty);
    descriptors.add(keepDeviceIdentityProperty);
    descriptors.add(keepInstitutionIdentityProperty);
    descriptors.add(keepAllPrivateProperty);
    descriptors.add(addContributingEquipmentSequenceProperty);
    return Collections.unmodifiableList(descriptors);
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
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

    FlowFile flowFile = session.get();
    if (flowFile != null) {
      try {
        // deidentifyUsingDCM4CHE(session, attributeStorage, flowfile);
        deifentifyAndEncrypt(context, session, flowFile);
      } catch (Exception e) {
        flowFile = session.penalize(flowFile);
        session.transfer(flowFile, RELATIONSHIP_REJECT);
        getLogger().error("Flowfile is not a DICOM file, could not read attributes", e);
      }
    }
    session.commitAsync();
  }

  void deifentifyAndEncrypt(ProcessContext context, ProcessSession session, FlowFile flowfile) throws Exception {
    String ourCalledAETitle = "nifi-dicom";
    AttributeList list;
    Attributes originalTags;
    Attributes deidentifiedTags;
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

    try (InputStream flowfileInputStream = session.read(flowfile)) {
      try (DicomInputStream dis = new DicomInputStream(flowfileInputStream)) {
        dis.setIncludeBulkData(IncludeBulkData.URI);
        originalTags = dis.readDataset(-1, -1);
      }
    }
    // Deal with patient demographics
    String oldName = "Unknown^Pat";
    String id = "unknown";
    if (list.containsKey(TagFromName.PatientName)) {
      oldName = list.get(TagFromName.PatientName).getSingleStringValueOrDefault(oldName);
    }
    if (list.containsKey(TagFromName.PatientID)) {
      id = list.get(TagFromName.PatientID).getSingleStringValueOrDefault("unknown");
    }
    IdentityEntry newId = IdentityEntry.createPseudoEntry(id, oldName);
    String newStudyId = list.get(TagFromName.StudyID).getSingleStringValueOrDefault("1234");
    newStudyId = Encryption.hash(newStudyId).substring(0, 8);

    list.removeGroupLengthAttributes();
    list.correctDecompressedImagePixelModule(true);
    list.insertLossyImageCompressionHistoryIfDecompressed(true);
    list.removeMetaInformationHeaderAttributes();

    ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
    ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list, ClinicalTrialsAttributes.HandleUIDs.keep,
        keepDescriptors, keepSeriesDescriptors, keepProtocolName, keepPatientCharacteristics, keepDeviceIdentity,
        keepInstitutionIdentity, ClinicalTrialsAttributes.HandleDates.keep, null/*
                                                                                 * epochForDateModification
                                                                                 */, null/*
                                                                                          * earliestDateInSet
                                                                                          */);
    {
      AttributeTag tag;
      Attribute a;

      tag = TagFromName.PatientName;
      list.remove(tag);
      a = new PersonNameAttribute(tag);
      a.addValue(newId.getDeidentifiedPatientName());
      list.put(tag, a);

      // Deidentify PatientId
      tag = TagFromName.PatientID;
      list.remove(tag);
      a = new LongStringAttribute(tag);
      a.addValue(newId.getDeidentifiedPatientId());
      list.put(tag, a);

      // Deidentify Accession number
      tag = TagFromName.AccessionNumber;
      String an = list.get(tag).getSingleStringValueOrDefault("0");
      list.remove(tag);
      a = new ShortStringAttribute(tag);
      a.addValue(newId.generateAccessionNumber(an));
      list.put(tag, a);

      list.get(TagFromName.StudyID).removeValues();
      list.get(TagFromName.StudyID).setValue(newStudyId);

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
        // Use the DCM4CHE method of generating a hash, this keeps Series and Studies
        // together...
        String replacementUIDValue = UIDUtils.createNameBasedUID(originalUIDValue.getBytes());
        list.remove(tag);
        Attribute a = new UniqueIdentifierAttribute(tag);
        a.addValue(replacementUIDValue);
        list.put(tag, a);
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

    // We need to save the remapped UIDs. When the series is re-identified,
    // if the UIDs match, we assume this is the original data. If the Series and
    // Instance
    // UIDs do not match, we assume this is a new series and preserve the UIDs from
    // the other system.
    Attributes remappedUIDs = new Attributes();
    remappedUIDs.setString(Tag.StudyInstanceUID, VR.UI,
        Attribute.getSingleStringValueOrDefault(list, TagFromName.StudyInstanceUID, "unknown"));
    remappedUIDs.setString(Tag.SeriesInstanceUID, VR.UI,
        Attribute.getSingleStringValueOrDefault(list, TagFromName.SeriesInstanceUID, "unknown"));
    remappedUIDs.setString(Tag.SOPInstanceUID, VR.UI,
        Attribute.getSingleStringValueOrDefault(list, TagFromName.SOPInstanceUID, "unknown"));

    // Write the PixelMed DICOM out to a temp file, re-read with DCM4CHE and save
    File tempDICOMFile = File.createTempFile("nifi-dicom", ".dcm");
    try {
      // Save
      try (FileOutputStream out = new FileOutputStream(tempDICOMFile)) {
        list.write(out, outputTransferSyntaxUID, true, true);
      }
      // try to GC the list
      list = null;
      try (InputStream in = new FileInputStream(tempDICOMFile)) {
        try (DicomInputStream dis = new DicomInputStream(in)) {
          dis.setIncludeBulkData(IncludeBulkData.URI);
          deidentifiedTags = dis.readDataset(-1, -1);
        }
      }
    } finally {
      tempDICOMFile.delete();
    }

    // Optionally encrypt
    PropertyValue passwordProperty = context.getProperty(PASSWORD);
    if (passwordProperty.isSet()) {
      String password = passwordProperty.evaluateAttributeExpressions(flowfile).toString();

      Attributes differenceTags = originalTags.getRemovedOrModified(deidentifiedTags);
      differenceTags.remove(Tag.DeidentificationMethod);
      differenceTags.remove(Tag.DeidentificationMethodCodeSequence);

      // Our private tag...
      Sequence sequence = differenceTags.ensureSequence(PRIVATE_CREATOR, PRIVATE_TAG, 0);
      sequence.add(remappedUIDs);

      // The modified tags need to be in a Modified Attribute Sequence
      Attributes ma = new Attributes();
      Sequence s = ma.ensureSequence(Tag.ModifiedAttributesSequence, 0);
      s.add(differenceTags);

      // Note:
      // Need to install JCE Unlimited Strength Extension, both in the JRE and the JDK
      // https://stackoverflow.com/a/6388603/334619
      // See
      // $JAVA_HOME/jre/lib/security/
      // $JAVA_HOME/lib/security/
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try (DicomOutputStream dos = new DicomOutputStream(os, UID.ExplicitVRLittleEndian)) {
        dos.writeDataset(null, ma);
      }
      os.close();

      String salt = UUID.randomUUID().toString();
      int iterations = context.getProperty(ITERATIONS).evaluateAttributeExpressions(flowfile).asInteger();
      byte[] encryptedBuffer = Encryption.createPasswordEnvelopedObject(password.toCharArray(), salt.getBytes(),
          iterations, os.toByteArray());

      // put it in the tags
      Attributes encryptedAttributes = new Attributes();
      encryptedAttributes.setString(Tag.EncryptedContentTransferSyntaxUID, VR.UI, UID.ExplicitVRLittleEndian);
      encryptedAttributes.setBytes(Tag.EncryptedContent, VR.OB, encryptedBuffer);

      // Add the sequence into the deidentified tags
      Sequence encryptedAttributesSequence = deidentifiedTags.ensureSequence(Tag.EncryptedAttributesSequence, 0);
      encryptedAttributesSequence.add(encryptedAttributes);
    }

    FlowFile outputFlowFile = session.create(flowfile);
    outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
      try (DicomOutputStream dos = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
        dos.writeDataset(deidentifiedTags.createFileMetaInformation(UID.ExplicitVRLittleEndian), deidentifiedTags);
      }
    });
    // Remove the incoming flowfile from the input queue, transfer the new
    // deidentified file to the output
    session.remove(flowfile);
    session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);

  }
}

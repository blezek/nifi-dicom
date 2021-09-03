package com.blezek.nifi.dicom;

import com.blezek.nifi.dicom.util.Encryption;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
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
import org.apache.nifi.processor.util.StandardValidators;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({ "deidentify", "dicom", "imaging", "encrypt", "reidentify", "decrypt" })
@SupportsBatching
@SideEffectFree
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM reidentifier.  Previously deidintified DICOM files with Supplement 55 encrypted tags have the original tags decrypted and the reidentified image is written as a FlowFile.")
@SeeAlso(DeidentifyEncryptDICOM.class)
public class DecryptReidentifyDICOM extends AbstractProcessor {

  // Relationships
  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_NOT_DECRYPTED = new Relationship.Builder().name("not decrypted")
      .description("DICOM images that could not be sucessfully decrypted").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("failure")
      .description("FlowFiles that are not DICOM images").build();

  // Properties
  static final PropertyDescriptor PASSWORD = new PropertyDescriptor.Builder().name("password")
      .displayName("Encryption password")
      .description(
          "Encryption password, leave empty or unset if deidintified or removed attributes are not to be encripted")
      .required(true).expressionLanguageSupported(true)
      .addValidator(StandardValidators.ATTRIBUTE_EXPRESSION_LANGUAGE_VALIDATOR)
      .addValidator(StandardValidators.NON_BLANK_VALIDATOR).build();

  static final PropertyDescriptor BATCH_SIZE = new PropertyDescriptor.Builder().name("Batch size").defaultValue("100")
      .description("Number of DICOM files to process in batch").required(false)
      .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR).expressionLanguageSupported(false).build();

  static final PropertyDescriptor ACCEPT_NEW_SERIES = new PropertyDescriptor.Builder().name("accept")
      .displayName("Accept new series")
      .description(
          "If the encrypted, generated Series and Instance UIDs do not match the DICOM object, assume this DICOM image is a new series generated from a deidentified, encrypted DICOM image.  Decrypt the original tags, but do not replace the Series and SOPInstance UIDs, effectively creating a new series")
      .required(true).expressionLanguageSupported(true).allowableValues("true", "false").defaultValue("true").build();

  static final DecimalFormat df = new DecimalFormat("#.00");

  @Override
  public Set<Relationship> getRelationships() {
    // relationships
    final Set<Relationship> relationships = new HashSet<>();
    relationships.add(RELATIONSHIP_SUCCESS);
    relationships.add(RELATIONSHIP_NOT_DECRYPTED);
    relationships.add(RELATIONSHIP_REJECT);
    return Collections.unmodifiableSet(relationships);
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    // descriptors
    final List<PropertyDescriptor> descriptors = new ArrayList<>();
    descriptors.add(PASSWORD);
    descriptors.add(ACCEPT_NEW_SERIES);
    descriptors.add(BATCH_SIZE);
    return Collections.unmodifiableList(descriptors);
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

    Integer batchSize = context.getProperty(BATCH_SIZE).asInteger();
    for (FlowFile flowFile : session.get(batchSize)) {
      try {
        boolean success = decryptAndReidentify(context, session, flowFile);
        if (!success) {
          session.transfer(flowFile, RELATIONSHIP_NOT_DECRYPTED);
        }
      } catch (Exception e) {
        flowFile = session.penalize(flowFile);
        session.transfer(flowFile, RELATIONSHIP_REJECT);
        getLogger().error("Flowfile is not a DICOM file, could not read attributes", e);
      }
    }

    session.commitAsync();
  }

  /**
   * decryptAndReidentify saves a FlowFile with reidentified DICOM.
   * 
   * Output flowfile is saved to RELATIONSHIP_SUCCESS if decryption and
   * reidentification is successful.
   * 
   * @param context
   * @param session
   * @param flowFile
   * @return true if successful, false if there was no encryption data
   * @throws Exception
   */
  private boolean decryptAndReidentify(ProcessContext context, ProcessSession session, FlowFile flowFile)
      throws Exception {
    String password = context.getProperty(PASSWORD).evaluateAttributeExpressions().toString();
    boolean acceptNewSeries = context.getProperty(ACCEPT_NEW_SERIES).evaluateAttributeExpressions().asBoolean();

    Attributes tags;
    try (InputStream is = session.read(flowFile)) {
      try (DicomInputStream dis = new DicomInputStream(is)) {
        tags = dis.readDataset(-1, -1);
      }
    }
    if (tags.contains(Tag.EncryptedAttributesSequence)) {

      Sequence eas = tags.getSequence(Tag.EncryptedAttributesSequence);
      for (int i = 0; i < eas.size(); i++) {
        Attributes attributes = eas.get(i);
        if (attributes.contains(Tag.EncryptedContent) && attributes.contains(Tag.EncryptedContentTransferSyntaxUID)
            && attributes.getString(Tag.EncryptedContentTransferSyntaxUID, "null").equals(UID.ExplicitVRLittleEndian)) {
          // Check to see if we can pull out the data...
          byte[] content = attributes.getBytes(Tag.EncryptedContent);
          if (Encryption.isPasswordRecipient(content)) {
            content = Encryption.extractPasswordEnvelopedData(password.toCharArray(), content);
            try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(content),
                UID.ExplicitVRLittleEndian)) {
              dis.setIncludeBulkData(IncludeBulkData.NO);

              Attributes wrapper = dis.readDataset(-1, -1);
              Sequence mas = wrapper.getSequence(Tag.ModifiedAttributesSequence);
              if (mas == null || mas.size() != 1) {
                continue;
              }
              Attributes originalTags = mas.get(0);
              // Get the private creator list
              Sequence uidSequence = originalTags.getSequence(DeidentifyEncryptDICOM.PRIVATE_CREATOR,
                  DeidentifyEncryptDICOM.PRIVATE_TAG);
              if (uidSequence == null || uidSequence.size() != 1) {
                getLogger().error("Could not find required private tags (" + DeidentifyEncryptDICOM.PRIVATE_CREATOR
                    + ") in encryted contents");
                continue;
              }

              // Check the UIDs
              String expectedSeriesInstanceUID = uidSequence.get(0).getString(Tag.SeriesInstanceUID);
              String expectedInstanceUID = uidSequence.get(0).getString(Tag.SOPInstanceUID);

              // We are accepting of new series, so don't replace the SeriesInstanceUID, i.e.
              // fake value in originalTags
              if (!tags.getString(Tag.SeriesInstanceUID).equals(expectedSeriesInstanceUID)) {
                if (acceptNewSeries) {
                  originalTags.setString(Tag.SeriesInstanceUID, VR.UI, tags.getString(Tag.SeriesInstanceUID));
                } else {
                  return false;
                }
              }
              if (!tags.getString(Tag.SOPInstanceUID).equals(expectedInstanceUID)) {
                if (acceptNewSeries) {
                  originalTags.setString(Tag.SOPInstanceUID, VR.UI, tags.getString(Tag.SOPInstanceUID));
                } else {
                  return false;
                }
              }

              // Remove the private tag...
              originalTags.remove(DeidentifyEncryptDICOM.PRIVATE_CREATOR, DeidentifyEncryptDICOM.PRIVATE_TAG);
              tags.updateRecursive(originalTags);

              tags.remove(Tag.PatientIdentityRemoved);
              tags.remove(Tag.DeidentificationMethod);

              // Great! everything back together
              FlowFile outputFlowFile = session.create(flowFile);
              outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
                try (DicomOutputStream dos = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
                  dos.writeDataset(tags.createFileMetaInformation(UID.ExplicitVRLittleEndian), tags);
                }
              });
              // Remove the incoming flowfile from the input queue, transfer the new
              // deidentified file to the output
              session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);
              session.remove(flowFile);
              return true;

            } catch (Exception e) {
              getLogger().debug("Could not decrypt with password");
            }
          }
        }
      }
    }
    return false;
  }
}

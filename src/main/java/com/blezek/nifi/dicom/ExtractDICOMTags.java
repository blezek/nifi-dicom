package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.util.TagUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Tags({ "tag", "attribute", "dicom", "imaging" })
@SupportsBatching
@SideEffectFree
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor extracts DICOM tags from the DICOM image and sets the values at attributes of the flowfile.")
public class ExtractDICOMTags extends AbstractProcessor {

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("failure")
      .description("FlowFiles that are not DICOM images").build();

  static final PropertyDescriptor ALL_TAGS = new PropertyDescriptor.Builder().name("ALL_TAGS")
      .displayName("Extract all DICOM tags").required(true)
      .description("Extract all DICOM tags if true, only listed tags if false").allowableValues("true", "false")
      .defaultValue("true").addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  static final PropertyDescriptor CONSTRUCT_FILENAME = new PropertyDescriptor.Builder()
      .name("Construct suggested filename").required(true)
      .description(
          "Construct a filename of the pattern 'PatientName/Modality_Date/SeriesNumber_SeriesDescription/SOPInstanceUID.dcm' with all unacceptable characters mapped to '_'")
      .allowableValues("true", "false").defaultValue("true").addValidator(StandardValidators.BOOLEAN_VALIDATOR).build();

  private static final Set<Relationship> relationships;
  static {
    // relationships
    final Set<Relationship> procRels = new HashSet<>();
    procRels.add(RELATIONSHIP_SUCCESS);
    procRels.add(RELATIONSHIP_REJECT);
    relationships = Collections.unmodifiableSet(procRels);
  }

  @Override
  public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    List<PropertyDescriptor> l = new ArrayList<>();
    l.add(ALL_TAGS);
    l.add(CONSTRUCT_FILENAME);
    return Collections.unmodifiableList(l);
  }

  @Override
  public Set<Relationship> getRelationships() {
    return relationships;
  }

  /**
   * Build a dynamic property based on the tag.
   * 
   * @param propertyDescriptorName
   *          used to lookup if any property descriptors exist for that name
   * @return new property descriptor if supported
   */
  @Override
  protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {

    Validator validator = (String subject, String input, ValidationContext context) -> {
      if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
        final ResultType resultType = context.newExpressionLanguageCompiler().getResultType(input);
        if (!resultType.equals(ResultType.STRING)) {
          return new ValidationResult.Builder().subject(subject).input(input).valid(false)
              .explanation("Expected tag to return type " + ResultType.STRING + " but query returns type " + resultType)
              .build();
        }
      }
      // see if it's a recognized tag.
      int tag = TagUtils.forName(subject);
      if (tag == -1) {
        return new ValidationResult.Builder().subject(subject).input(input).valid(false)
            .explanation("Expected tag to return a " + ResultType.STRING
                + " representing a tag name or a hex value of a tag but '" + input
                + "' is not recognized as a valid DICOM tag.")
            .build();
      }
      return new ValidationResult.Builder().subject(subject).input(input).explanation(null).valid(true).build();
    };

    PropertyDescriptor descriptor = new PropertyDescriptor.Builder().dynamic(true).name(propertyDescriptorName)
        .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).displayName(propertyDescriptorName).addValidator(validator).build();

    return descriptor;
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    Boolean constructFilename = context.getProperty(CONSTRUCT_FILENAME).asBoolean();
    Boolean allTags = context.getProperty(ALL_TAGS).asBoolean();
    for (FlowFile flowFile : session.get(100)) {
      Optional<Relationship> destinationRelationship = Optional.empty();
      // Clone the FlowFile

      Map<String, String> attributeMap = new HashMap<>();

      try (InputStream flowfileInputStream = session.read(flowFile)) {
        try (DicomInputStream in = new DicomInputStream(flowfileInputStream)) {
          in.setIncludeBulkData(IncludeBulkData.NO);
          Attributes attributes;
          attributes = in.readDataset(-1, Tag.PixelData);

          if (constructFilename) {
            String badCharacters = "[^a-zA-Z0-9.^]";
            StringBuilder builder = new StringBuilder();
            builder.append(attributes.getString(Tag.PatientName, "unknown_patient").replaceAll(badCharacters, "_"));
            builder.append("/");

            // Modality / Date
            builder.append(attributes.getString(Tag.StudyDate, "unknown_date").replaceAll(badCharacters, "_"));
            builder.append("_");
            builder.append(attributes.getString(Tag.Modality, "unknown_modality").replaceAll(badCharacters, "_"));
            builder.append("/");

            // Series
            builder.append(attributes.getString(Tag.SeriesNumber, "9999").replaceAll(badCharacters, "_"));
            builder.append("_");
            builder.append(
                attributes.getString(Tag.SeriesDescription, "no_series_description").replaceAll(badCharacters, "_"));
            builder.append("/");
            attributeMap.put("path", builder.toString());

            // Instance
            attributeMap.put("filename",
                attributes.getString(Tag.SOPInstanceUID).replaceAll(badCharacters, "_") + ".dcm");
          }

          if (allTags) {
            attributes.accept((Attributes attrs, int tag, VR vr, Object value) -> {
              if (value != null) {
                String tagName = TagUtils.toHexString(tag);
                try {
                  tagName = ElementDictionary.getStandardElementDictionary().keywordOf(tag);
                } catch (Exception e) {
                  getLogger().debug("Could not find tag name for " + tagName + ": " + e.getMessage());
                }
                StringBuilder builder = new StringBuilder();
                vr.prompt(value, in.bigEndian(), attributes.getSpecificCharacterSet(), 200, builder);
                String tagValue = builder.toString();
                if (tagName != null && !tagName.isEmpty()) {
                  attributeMap.put(tagName, tagValue);
                }
              }
              return true;
            }, false);

          } else {
            // Set attributes
            Map<PropertyDescriptor, String> properties = context.getProperties();
            for (PropertyDescriptor descriptor : properties.keySet()) {
              // If this is a dynamic descriptor
              if (descriptor.isDynamic()) {
                // Look up the tag in the DICOM attributes, set default if not
                // found.
                String tagName = descriptor.getName();
                int tag = TagUtils.forName(tagName);
                if (attributes.contains(tag) && attributes.containsValue(tag)) {
                  Object o = attributes.getValue(tag);
                  attributeMap.put(tagName, o.toString());
                } else {
                  attributeMap.put(tagName,
                      context.getProperty(descriptor).evaluateAttributeExpressions(flowFile).toString());
                }
              }
            }
          }

        } catch (IOException e) {
          destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
          getLogger().error("Flowfile is not a DICOM file, could not read attributes", e);
        } catch (Exception e) {
          destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
          getLogger().error("Could not read DICOM attributes", e);
        }
      } catch (IOException dse) {
        destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
        getLogger().error("Flowfile is not a DICOM file", dse);
      }

      // If we reject
      if (destinationRelationship.isPresent()) {
        session.transfer(flowFile, destinationRelationship.get());
      } else {
        session.putAllAttributes(flowFile, attributeMap);
        session.transfer(flowFile, RELATIONSHIP_SUCCESS);
      }
    }
    session.commitAsync();
  }

}

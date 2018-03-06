package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.TagUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Tags({ "dicom", "imaging" })
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor modifies DICOM tags. ")
@SeeAlso(ExtractDICOMTags.class)
public class ModifyDICOMTags extends AbstractProcessor {
    public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
	    .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
    public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("failure")
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

    /**
     * Build a dynamic property based on the tag.
     * 
     * @param propertyDescriptorName
     *            used to lookup if any property descriptors exist for that name
     * @return new property descriptor if supported
     */
    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {

	Validator validator = (String subject, String input, ValidationContext context) -> {
	    if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
		final ResultType resultType = context.newExpressionLanguageCompiler().getResultType(input);
		if (!resultType.equals(ResultType.STRING)) {
		    return new ValidationResult.Builder().subject(subject).input(input).valid(false)
			    .explanation("Expected tag to return type " + ResultType.STRING + " but query returns type "
				    + resultType)
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
		.expressionLanguageSupported(true).displayName(propertyDescriptorName).addValidator(validator).build();

	return descriptor;
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
	List<FlowFile> flowfiles = session.get(100);
	for (FlowFile flowfile : flowfiles) {
	    Optional<Relationship> destinationRelationship = Optional.empty();
	    try (InputStream flowfileInputStream = session.read(flowfile)) {
		try (DicomInputStream in = new DicomInputStream(flowfileInputStream)) {
		    in.setIncludeBulkData(IncludeBulkData.URI);
		    Attributes attributes;
		    Attributes fmi = in.readFileMetaInformation();
		    attributes = in.readDataset(-1, -1);

		    // Modify the attributes
		    Map<PropertyDescriptor, String> properties = context.getProperties();
		    for (PropertyDescriptor descriptor : properties.keySet()) {
			// Look up the tag in the DICOM attributes, set default if not
			// found.
			String tagName = descriptor.getName();
			int tag = TagUtils.forName(tagName);
			VR vr = ElementDictionary.getStandardElementDictionary().vrOf(tag);
			attributes.setString(tag, vr,
				context.getProperty(descriptor).evaluateAttributeExpressions().toString());
		    }

		    // Create a new FlowFile
		    FlowFile outputFlowFile = session.create();
		    outputFlowFile = session.write(outputFlowFile, (OutputStream out) -> {
			try (DicomOutputStream dos = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
			    dos.writeDataset(fmi, attributes);
			} catch (Exception e) {
			    getLogger().error("Could not write output file", e);
			    throw e;
			}
		    });

		    // Copy all attributes
		    session.putAllAttributes(outputFlowFile, flowfile.getAttributes());

		    // Save attributes
		    session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);

		} catch (IOException e) {
		    destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
		    getLogger().error("Flowfile is not a DICOM file, could not read attributes", e);
		}
	    } catch (IOException dse) {
		destinationRelationship = Optional.of(RELATIONSHIP_REJECT);
		getLogger().error("Flowfile is not a DICOM file", dse);
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

}

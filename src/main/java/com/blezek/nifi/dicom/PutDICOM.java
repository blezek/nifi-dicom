
package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomStreamException;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.DataWriterAdapter;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SupportsBatching
@InputRequirement(Requirement.INPUT_REQUIRED)

@Tags({ "send", "dicom", "imaging", "network" })
@CapabilityDescription("This processor implements a DICOM sender, sending DICOM images to the specified destination.")
@SeeAlso(ListenDICOM.class)
public class PutDICOM extends AbstractProcessor {

    static final PropertyDescriptor DICOM_PORT = new PropertyDescriptor.Builder().name("DICOM_PORT")
	    .displayName("Remote Port").description("The TCP port to send to.").required(true)
	    .expressionLanguageSupported(true).addValidator(StandardValidators.PORT_VALIDATOR).defaultValue("4096")
	    .build();
    static final PropertyDescriptor CALLING_AE_TITLE = new PropertyDescriptor.Builder().name("CALLING_AE_TITLE")
	    .displayName("Local Application Entity").required(true).expressionLanguageSupported(true)
	    .addValidator(StandardValidators.NON_BLANK_VALIDATOR).addValidator(new AETitleValidator()).build();
    static final PropertyDescriptor CALLED_AE_TITLE = new PropertyDescriptor.Builder().name("CALLED_AE_TITLE")
	    .displayName("Remote Application Entity Title").required(true).expressionLanguageSupported(true)
	    .addValidator(StandardValidators.NON_BLANK_VALIDATOR).addValidator(new AETitleValidator()).build();
    static final PropertyDescriptor DICOM_HOSTNAME = new PropertyDescriptor.Builder().name("DICOM_HOSTNAME")
	    .displayName("Remote hostname of remote DICOM destination").required(true).expressionLanguageSupported(true)
	    .addValidator(StandardValidators.NON_BLANK_VALIDATOR).build();
    static final PropertyDescriptor BATCH_SIZE = new PropertyDescriptor.Builder().name("BATCH_SIZE")
	    .displayName("maxmium number of DICOM images to send at once, 0 is unlimited").defaultValue("0")
	    .required(true).expressionLanguageSupported(true)
	    .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
	    .addValidator(StandardValidators.NON_BLANK_VALIDATOR).build();

    public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
	    .description("FlowFiles that are successfully sent will be routed to success").build();
    public static final Relationship RELATIONSHIP_FAILURE = new Relationship.Builder().name("failure").description(
	    "FlowFiles that failed to send to the remote system; failure is usually looped back to this processor")
	    .build();
    public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("reject")
	    .description("FlowFiles that are not DICOM images").build();

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
	// relationships
	final Set<Relationship> procRels = new HashSet<>();
	procRels.add(RELATIONSHIP_SUCCESS);
	procRels.add(RELATIONSHIP_FAILURE);
	procRels.add(RELATIONSHIP_REJECT);
	relationships = Collections.unmodifiableSet(procRels);

	// descriptors
	final List<PropertyDescriptor> supDescriptors = new ArrayList<>();
	supDescriptors.add(CALLED_AE_TITLE);
	supDescriptors.add(DICOM_HOSTNAME);
	supDescriptors.add(DICOM_PORT);
	supDescriptors.add(CALLING_AE_TITLE);
	supDescriptors.add(BATCH_SIZE);
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
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
	int maxResults = context.getProperty(BATCH_SIZE).evaluateAttributeExpressions().asInteger();
	maxResults = maxResults == 0 ? Integer.MAX_VALUE : maxResults;
	List<FlowFile> flowfiles = session.get(maxResults);
	if (flowfiles.size() > 0) {
	    try {
		send(context, session, flowfiles);
	    } catch (IOException | InterruptedException | IncompatibleConnectionException
		    | GeneralSecurityException e) {
		getLogger().error("error sending DICOM file", e);
	    }
	}
    }

    private void send(ProcessContext context, ProcessSession session, List<FlowFile> flowfiles)
	    throws IOException, InterruptedException, IncompatibleConnectionException, GeneralSecurityException {
	Device device = new Device("storescu");
	Connection conn = new Connection();
	device.addConnection(conn);
	ApplicationEntity ae = new ApplicationEntity("STORESCU");

	device.addApplicationEntity(ae);
	ae.addConnection(conn);
	ExecutorService executorService = Executors.newSingleThreadExecutor();
	ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	device.setExecutor(executorService);
	device.setScheduledExecutor(scheduledExecutorService);

	AAssociateRQ rq = new AAssociateRQ();

	Connection remote = new Connection();
	rq.setCalledAET(context.getProperty(CALLED_AE_TITLE).evaluateAttributeExpressions().getValue());
	remote.setHostname(context.getProperty(DICOM_HOSTNAME).evaluateAttributeExpressions().getValue());
	remote.setPort(context.getProperty(DICOM_PORT).evaluateAttributeExpressions().asInteger());

	ae.setAETitle(context.getProperty(CALLING_AE_TITLE).evaluateAttributeExpressions().getValue());

	String destinationUri = new StringBuilder("dicom://")
		.append(context.getProperty(CALLED_AE_TITLE).evaluateAttributeExpressions().getValue()).append("@")
		.append(context.getProperty(DICOM_HOSTNAME).evaluateAttributeExpressions().getValue()).append(":")
		.append(context.getProperty(DICOM_PORT).evaluateAttributeExpressions().getValue()).toString();
	String details = "DICOM file sent by "
		+ context.getProperty(CALLING_AE_TITLE).evaluateAttributeExpressions().getValue();

	List<FlowFile> validDICOMFlowFiles = new ArrayList<>();
	// First loop is to add all the presentation contexts
	for (FlowFile flowfile : flowfiles) {
	    try (InputStream flowfileInputStream = session.read(flowfile)) {
		try (DicomInputStream in = new DicomInputStream(flowfileInputStream)) {
		    in.setIncludeBulkData(IncludeBulkData.NO);
		    Attributes data;
		    data = in.readDataset(-1, -1);
		    Attributes fmi = in.readFileMetaInformation();
		    if (fmi == null || !fmi.containsValue(Tag.TransferSyntaxUID)
			    || !fmi.containsValue(Tag.MediaStorageSOPClassUID)
			    || !fmi.containsValue(Tag.MediaStorageSOPInstanceUID)) {
			fmi = data.createFileMetaInformation(in.getTransferSyntax());
		    }

		    String cuid = fmi.getString(Tag.MediaStorageSOPClassUID);
		    String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID);
		    String ts = fmi.getString(Tag.TransferSyntaxUID);
		    if (!rq.containsPresentationContextFor(cuid, ts)) {
			if (!rq.containsPresentationContextFor(cuid)) {
			    RelatedGeneralSOPClasses relSOPClasses = new RelatedGeneralSOPClasses();
			    rq.addCommonExtendedNegotiation(relSOPClasses.getCommonExtendedNegotiation(cuid));
			    if (!ts.equals(UID.ExplicitVRLittleEndian)) {
				rq.addPresentationContext(
					new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid,
						UID.ExplicitVRLittleEndian));
			    }
			    if (!ts.equals(UID.ImplicitVRLittleEndian)) {
				rq.addPresentationContext(
					new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid,
						UID.ImplicitVRLittleEndian));
			    }
			}
			rq.addPresentationContext(
				new PresentationContext(rq.getNumberOfPresentationContexts() * 2 + 1, cuid, ts));
		    }
		}
		validDICOMFlowFiles.add(flowfile);
	    } catch (DicomStreamException dse) {
		session.transfer(flowfile, RELATIONSHIP_REJECT);
		getLogger().error("Flowfile is not a DICOM file", dse);
		continue;
	    }
	}
	Association as;
	if (validDICOMFlowFiles.size() > 0) {
	    try {
		as = ae.connect(remote, rq);
	    } catch (Exception e) {
		getLogger().error("error connecting to " + remote.getDevice(), e);
		session.transfer(validDICOMFlowFiles, RELATIONSHIP_FAILURE);
		session.commit();
		return;
	    }

	    // Start sending files
	    for (FlowFile flowfile : validDICOMFlowFiles) {
		try (DicomInputStream in = new DicomInputStream(session.read(flowfile))) {
		    StopWatch watch = new StopWatch(true);
		    in.setIncludeBulkData(IncludeBulkData.YES);
		    Attributes data;
		    try {
			data = in.readDataset(-1, -1);
		    } catch (IOException e) {
			// is not dicom
			getLogger().error("Could not read DICOM from FlowFile", e);
			session.transfer(flowfile, RELATIONSHIP_REJECT);
			continue;
		    }
		    Attributes fmi = in.readFileMetaInformation();
		    if (fmi == null || !fmi.containsValue(Tag.TransferSyntaxUID)
			    || !fmi.containsValue(Tag.MediaStorageSOPClassUID)
			    || !fmi.containsValue(Tag.MediaStorageSOPInstanceUID)) {
			fmi = data.createFileMetaInformation(in.getTransferSyntax());
		    }

		    String cuid = fmi.getString(Tag.MediaStorageSOPClassUID);
		    String iuid = fmi.getString(Tag.MediaStorageSOPInstanceUID);
		    String ts = fmi.getString(Tag.TransferSyntaxUID);
		    as.cstore(cuid, iuid, Priority.NORMAL, new DataWriterAdapter(data), ts,
			    new DimseRSPHandler(as.nextMessageID()));
		    session.getProvenanceReporter().send(flowfile, destinationUri, details,
			    watch.getElapsed(TimeUnit.MILLISECONDS));
		} catch (Exception e) {
		    getLogger().error("Error sending DICOM", e);
		    session.transfer(flowfile, RELATIONSHIP_FAILURE);
		}
		session.transfer(flowfile, RELATIONSHIP_SUCCESS);
	    }
	    // Shutdown the association
	    if (as.isReadyForDataTransfer()) {
		as.release();
	    }
	    as.waitForSocketClose();
	    executorService.shutdown();
	    scheduledExecutorService.shutdown();
	}
	session.commit();
    }

}

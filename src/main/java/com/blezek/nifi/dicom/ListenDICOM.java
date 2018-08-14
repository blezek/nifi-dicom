package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractSessionFactoryProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.FlowFileAccessException;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;

import java.io.IOException;
import java.io.OutputStream;
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

@Tags({ "listen", "dicom", "imaging", "network" })
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("This processor implements a DICOM receiver to listen for incoming DICOM images.")
@WritesAttributes({ @WritesAttribute(attribute = "dicom.calling.aetitle", description = "The sending AE title"),
    @WritesAttribute(attribute = "dicom.calling.hostname", description = "The sending hostname"),
    @WritesAttribute(attribute = "dicom.called.aetitle", description = "The receiving AE title"),
    @WritesAttribute(attribute = "dicom.called.hostname", description = "The receiving hostname"),
    @WritesAttribute(attribute = "dicom.called.hostname", description = "The receiving hostname") })
@SideEffectFree
public class ListenDICOM extends AbstractSessionFactoryProcessor {

  static final PropertyDescriptor DICOM_PORT = new PropertyDescriptor.Builder().name("DICOM_PORT")
      .displayName("Listening port").description("The TCP port the ListenDICOM processor will bind to.").required(true)
      .expressionLanguageSupported(true).addValidator(StandardValidators.PORT_VALIDATOR).defaultValue("4096").build();
  static final PropertyDescriptor AE_TITLE = new PropertyDescriptor.Builder().name("AE_TITLE")
      .displayName("Local Application Entity Title").defaultValue("*")
      .description(
          "ListenDICOM requires that remote DICOM Application Entities use this AE Title when sending DICOM, default is to accept all called AE Titles")
      .expressionLanguageSupported(true).addValidator(new AETitleValidator()).build();

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All new DICOM images will be routed as FlowFiles to this relationship").build();

  private static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS;
  private static final Set<Relationship> RELATIONSHIPS;

  static {
    List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();
    propertyDescriptors.add(AE_TITLE);
    propertyDescriptors.add(DICOM_PORT);
    PROPERTY_DESCRIPTORS = Collections.unmodifiableList(propertyDescriptors);

    Set<Relationship> relationships = new HashSet<>();
    relationships.add(RELATIONSHIP_SUCCESS);
    RELATIONSHIPS = Collections.unmodifiableSet(relationships);
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSessionFactory session) throws ProcessException {
    // Start the DICOM server
    if (device == null) {
      try {
        startDICOM(context, session);
      } catch (Exception ex) {
        device = null;
        getLogger().error("Unable to start DICOM server due to " + ex.getMessage(), ex);
      }
    }
    // nothing really to do here since threading managed by DICOM server
    context.yield();
  }

  static final ExecutorService executorService = Executors.newCachedThreadPool();
  static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  @OnStopped
  public void stop() {
    try {
      stopDICOM();
    } catch (InterruptedException ex) {
      getLogger().error("Error wating to stop DICOM receiver: " + ex.getMessage(), ex);
    } finally {
      device = null;
    }
  }

  Device device = null;

  @OnEnabled
  void startDICOM(final ProcessContext context, final ProcessSessionFactory sessionFactory)
      throws IOException, GeneralSecurityException {
    String aeTitle = "*";
    PropertyValue p = context.getProperty(AE_TITLE).evaluateAttributeExpressions();
    if (p.isSet() && !p.getValue().equals("")) {
      aeTitle = p.getValue();
    }
    device = new Device("nifi-dicom");
    final ApplicationEntity ae = new ApplicationEntity(aeTitle);
    final Connection conn = new Connection();
    conn.setPort(context.getProperty(DICOM_PORT).evaluateAttributeExpressions().asInteger());
    final BasicCStoreSCP cstoreSCP = new BasicCStoreSCP("*") {

      @Override
      protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
          throws IOException {
        rsp.setInt(Tag.Status, VR.US, 0);

        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();

        // Create a flow file
        final ProcessSession processSession = sessionFactory.createSession();
        final StopWatch watch = new StopWatch();
        watch.start();
        try {
          FlowFile flowFile = processSession.create();
          flowFile = processSession.write(flowFile, (OutputStream out) -> {
            try (DicomOutputStream dout = new DicomOutputStream(out, UID.ExplicitVRLittleEndian)) {
              dout.writeFileMetaInformation(as.createFileMetaInformation(iuid, cuid, tsuid));
              data.copyTo(dout);
            }
          });
          flowFile = processSession.putAttribute(flowFile, "dicom.calling.aetitle", as.getCallingAET());
          flowFile = processSession.putAttribute(flowFile, "dicom.called.aetitle", as.getCalledAET());
          watch.stop();

          String transitUri = "dicom://" + as.getCallingAET() + "@" + as.getSocket().getRemoteSocketAddress();
          String details = "received DICOM to dicom://" + as.getCalledAET() + "@"
              + as.getSocket().getLocalSocketAddress() + ":" + as.getSocket().getLocalPort();
          processSession.getProvenanceReporter().receive(flowFile, transitUri, details,
              watch.getDuration(TimeUnit.MILLISECONDS));
          processSession.transfer(flowFile, ListenDICOM.RELATIONSHIP_SUCCESS);
          processSession.commit();
        } catch (FlowFileAccessException | IllegalStateException ex) {
          getLogger().error("Unable to fully process input due to " + ex.getMessage(), ex);
          throw ex;
        } finally {
          processSession.rollback();
        }
      }

    };
    DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
    serviceRegistry.addDicomService(new BasicCEchoSCP());
    serviceRegistry.addDicomService(cstoreSCP);
    device.setDimseRQHandler(serviceRegistry);
    device.addConnection(conn);
    device.addApplicationEntity(ae);
    ae.setAssociationAcceptor(true);
    ae.addConnection(conn);
    ae.addTransferCapability(new TransferCapability(null, "*", TransferCapability.Role.SCP, "*"));

    device.setScheduledExecutor(scheduledExecutorService);
    device.setExecutor(executorService);
    device.bindConnections();

  }

  private void stopDICOM() throws InterruptedException {
    device.unbindConnections();
    device.waitForNoOpenConnections();
    device = null;
  }

  @Override
  public Set<Relationship> getRelationships() {
    return RELATIONSHIPS;
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    return PROPERTY_DESCRIPTORS;
  }

}

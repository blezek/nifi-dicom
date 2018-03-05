package com.blezek.nifi.dicom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnShutdown;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.dcm4che3.util.UIDUtils;
import org.flywaydb.core.Flyway;
import org.h2.tools.Server;
import org.jdbi.v3.core.Jdbi;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import com.zaxxer.hikari.HikariDataSource;

@Tags({ "deidentify", "dicom", "imaging" })
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@CapabilityDescription("This processor implements a DICOM deidentifier.  The DeidentifyDICOM processor substitutes DICOM tags with deidentified values and stores the values.")
public class DeidentifyDICOM extends AbstractProcessor {

  public static final Relationship RELATIONSHIP_SUCCESS = new Relationship.Builder().name("success")
      .description("All deidentified DICOM images will be routed as FlowFiles to this relationship").build();
  public static final Relationship RELATIONSHIP_REJECT = new Relationship.Builder().name("reject")
      .description("FlowFiles that are not DICOM images").build();

  // Directory containing the database
  static final PropertyDescriptor dbDirectory = new PropertyDescriptor.Builder().name("DB_DIRECTORY")
      .displayName("Database directory").description("Location of the deidentification database, will be created in the 'database' sub-directory.")
      .required(true).expressionLanguageSupported(true).addValidator(StandardValidators.createDirectoryExistsValidator(true, true))
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
      .build();

  static final PropertyDescriptor dbConsole = new PropertyDescriptor.Builder().name("DB_CONSOLE")
      .displayName("Database console").description("Expose the DB Console on a port (if non-zero).")
      .required(true).expressionLanguageSupported(true).addValidator(StandardValidators.createLongValidator(0, 65535, true))
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
      .defaultValue("8082")
      .build();

  static final PropertyDescriptor keepDescriptorsProperty = new PropertyDescriptor.Builder().name("Keep descriptors").description("Keep text description and comment attributes")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  static final PropertyDescriptor keepSeriesDescriptorsProperty = new PropertyDescriptor.Builder().name("Keep series descriptors").description("Keep the series description even if all other descriptors are removed")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  static final PropertyDescriptor keepProtocolNameProperty = new PropertyDescriptor.Builder()
      .name("Keep protocol name").description("Keep protocol name even if all other descriptors are removed")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  static final PropertyDescriptor keepPatientCharacteristicsProperty = new PropertyDescriptor.Builder()
      .name("Keep patient characteristics")
      .description("Keep patient characteristics (such as might be needed for PET SUV calculations)")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  static final PropertyDescriptor keepDeviceIdentityProperty = new PropertyDescriptor.Builder()
      .name("Keep device identity")
      .description("Keep device identity")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  static final PropertyDescriptor keepInstitutionIdentityProperty = new PropertyDescriptor.Builder()
      .name("Keep institution identity")
      .description("Keep institution identity")
      .required(true).expressionLanguageSupported(true)
      .allowableValues("true", "false")
      .defaultValue("true")
      .build();

  private static final List<PropertyDescriptor> properties;
  private static final Set<Relationship> relationships;
  static {
    // relationships
    final Set<Relationship> procRels = new HashSet<>();
    procRels.add(RELATIONSHIP_SUCCESS);
    procRels.add(RELATIONSHIP_REJECT);
    relationships = Collections.unmodifiableSet(procRels);

    // descriptors
    final List<PropertyDescriptor> descriptors = new ArrayList<>();
    descriptors.add(dbDirectory);
    descriptors.add(dbConsole);
    descriptors.add(keepDescriptorsProperty);
    descriptors.add(keepSeriesDescriptorsProperty);
    descriptors.add(keepProtocolNameProperty);
    descriptors.add(keepPatientCharacteristicsProperty);
    descriptors.add(keepDeviceIdentityProperty);
    descriptors.add(keepInstitutionIdentityProperty);
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
  Server server = null;
  private HikariDataSource ds;
  Jdbi jdbi;
  Cache<String, String> uidCache;

  @OnScheduled
  public void startDB(ProcessContext context) throws SQLException {
    keepDescriptors = context.getProperty(keepDescriptorsProperty).evaluateAttributeExpressions().asBoolean();
    keepSeriesDescriptors = context.getProperty(keepSeriesDescriptorsProperty).evaluateAttributeExpressions().asBoolean();
    keepProtocolName = context.getProperty(keepProtocolNameProperty).evaluateAttributeExpressions().asBoolean();
    keepPatientCharacteristics = context.getProperty(keepPatientCharacteristicsProperty).evaluateAttributeExpressions().asBoolean();
    keepDeviceIdentity = context.getProperty(keepDeviceIdentityProperty).evaluateAttributeExpressions().asBoolean();
    keepInstitutionIdentity = context.getProperty(keepInstitutionIdentityProperty).evaluateAttributeExpressions().asBoolean();

    String dbPath = context.getProperty(dbDirectory).evaluateAttributeExpressions().getValue();
    ds = new HikariDataSource();
    ds.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
    dbPath = new File(dbPath, "database").getAbsolutePath();
    ds.setJdbcUrl("jdbc:derby:" + dbPath + ";create=true");
    ds.setMaximumPoolSize(50);

    Flyway flyway = new Flyway();
    flyway.setDataSource(ds);
    flyway.migrate();

    jdbi = Jdbi.create(ds);
    uidCache = CacheBuilder.newBuilder().maximumSize(3000).expireAfterWrite(1, TimeUnit.MINUTES).build();
    // Start up the DB
    Integer port = context.getProperty(dbConsole).evaluateAttributeExpressions().asInteger();
    if (port > 0) {
      server = Server.createWebServer("-web", "-webPort", Integer.toString(port));
      server.start();
    }
  }

  @OnShutdown
  public void shutdownDB(ProcessContext context) {
    if (server != null) {
      server.stop();
      server = null;
    }
    String dbPath = context.getProperty(dbDirectory).getValue();

    try {
      DriverManager.getConnection("jdbc:derby:" + dbPath + ";shutdown=true");
    } catch (SQLException e) {
      getLogger().error("Error shutting down db", e);
    }
    jdbi = null;
    ds = null;
    uidCache.invalidateAll();
    uidCache = null;
  }

  String mapUid(String uid) throws ExecutionException {
    // Return if in cache
    return uidCache.get(uid, () -> {

      String sql = "merge into uid_map "
          + " using single"
          + " on uid_map.original = ?"
          + " when not matched then insert "
          + " ( original, replaced ) "
          + " values (?,?)";
      return jdbi.withHandle(handle -> {
        handle.execute(sql, uid, uid, UIDUtils.createNameBasedUID(sql.getBytes()));
        return handle.createQuery("select replaced from uid_map where original = :original").bind("original", uid).mapTo(String.class).findOnly();
      });
    });
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    List<FlowFile> flowfiles = session.get(100);
    for (FlowFile flowfile : flowfiles) {
      Optional<Relationship> destinationRelationship = Optional.empty();

      try {
        // deidentifyUsingDCM4CHE(session, attributeStorage, flowfile);
        deidentifyUsingPixelMed(session, flowfile);

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

  void deidentifyUsingPixelMed(ProcessSession session, FlowFile flowfile) throws Exception {
    boolean keepAllPrivate = false;
    boolean addContributingEquipmentSequence = true;
    String ourCalledAETitle = "FLOWEY";
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

    list.removeGroupLengthAttributes();
    list.correctDecompressedImagePixelModule(true);
    list.insertLossyImageCompressionHistoryIfDecompressed(true);
    list.removeMetaInformationHeaderAttributes();

    ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
    ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,
        ClinicalTrialsAttributes.HandleUIDs.keep,
        keepDescriptors,
        keepSeriesDescriptors,
        keepProtocolName,
        keepPatientCharacteristics,
        keepDeviceIdentity,
        keepInstitutionIdentity,
        ClinicalTrialsAttributes.HandleDates.keep,
        null/* epochForDateModification */,
        null/* earliestDateInSet */);

    String id = "unknown";
    if (list.containsKey(TagFromName.PatientID)) {
      id = list.get(TagFromName.PatientID).getSingleStringValueOrDefault("unknown");
    }
    String newName = "foo" + 1;
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
        String replacementUIDValue = mapUid(originalUIDValue);
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

    session.transfer(outputFlowFile, RELATIONSHIP_SUCCESS);

  }

}

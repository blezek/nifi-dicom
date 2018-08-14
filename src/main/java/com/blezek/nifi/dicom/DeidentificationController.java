package com.blezek.nifi.dicom;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.dcm4che3.util.UIDUtils;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Tags({ "dicom", "imaging", "deidentification" })
@CapabilityDescription("This controller implements attribute storage for deidentification of DICOM images")
@SeeAlso({ ListenDICOM.class, PutDICOM.class, DeidentifyDICOM.class })
public class DeidentificationController extends AbstractControllerService implements DeidentificationService {

  static final PropertyDescriptor DB_DIRECTORY = new PropertyDescriptor.Builder().name("DB_DIRECTORY")
      .displayName("Database directory")
      .description("Location of the deidentification database, will be created in the 'database' sub-directory.")
      .required(true).expressionLanguageSupported(true)
      .addValidator(StandardValidators.createDirectoryExistsValidator(true, true))
      .addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  static final PropertyDescriptor DEIDENTIFICATION_MAP_CVS_FILE = new PropertyDescriptor.Builder()
      .name("Identifier map CVS file")
      .description(
          "CVS file containing the columns 'PatientId', 'PatientName', 'DeidentifiedPatientId', 'DeidentifiedPatientName' used to map IDs")
      .required(false).addValidator((String subject, String value, ValidationContext context) -> {
        // If the value is non-empty, make sure the file exists
        String substituted;
        try {
          substituted = context.newPropertyValue(value).evaluateAttributeExpressions().getValue();
        } catch (final Exception e) {
          return new ValidationResult.Builder().subject(subject).input(value).valid(false)
              .explanation("Not a valid Expression Language value: " + e.getMessage()).build();
        }
        if (substituted == null || substituted.equals("")) {
          return new ValidationResult.Builder().subject(subject).input(value).valid(true).explanation("Empty file")
              .build();
        }
        final File file = new File(substituted);
        final boolean valid = file.exists();
        final String explanation = valid ? null : "File " + file + " does not exist";
        return new ValidationResult.Builder().subject(subject).input(value).valid(valid).explanation(explanation)
            .build();

      }).expressionLanguageSupported(true).build();

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    // descriptors
    List<PropertyDescriptor> supDescriptors = new ArrayList<>();
    supDescriptors.add(DB_DIRECTORY);
    supDescriptors.add(DEIDENTIFICATION_MAP_CVS_FILE);
    return Collections.unmodifiableList(supDescriptors);
  }

  private EmbeddedDataSource ds;
  Jdbi jdbi;
  LoadingCache<String, String> uidCache;
  ConcurrentHashMap<String, IdentityEntry> identityMap = new ConcurrentHashMap<>();

  @OnEnabled
  public void enabled(ConfigurationContext context) throws Exception {
    String dbPath = context.getProperty(DB_DIRECTORY).evaluateAttributeExpressions().getValue();

    ds = new EmbeddedDataSource();
    ds.setDatabaseName(new File(dbPath, "database").getAbsolutePath());
    ds.setCreateDatabase("create");

    Flyway flyway = new Flyway();
    flyway.setDataSource(ds);
    flyway.migrate();

    jdbi = Jdbi.create(ds);
    uidCache = CacheBuilder.newBuilder().recordStats().maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<String, String>() {

          @Override
          public String load(String uid) throws Exception {
            String newUid = "";
            synchronized (DeidentificationController.class) {
              try {
                String sql = "merge into uid_map using single on uid_map.original = ? when not matched then insert "
                    + " ( original, replaced ) values (?,?)";
                newUid = jdbi.inTransaction(handle -> {
                  handle.execute(sql, uid, uid, UIDUtils.createUID());
                  return handle.createQuery("select replaced from uid_map where original = :original")
                      .bind("original", uid).mapTo(String.class).findOnly();

                });
              } catch (Exception e) {
                getLogger().error("Error inserting into cache", e);
              }
            }
            return newUid;
          }
        });
    identityMap.clear();

    // ColumnPositionMappingStrategy<IdentityEntry> strategy = new
    // ColumnPositionMappingStrategy<>();
    // String[] memberFieldsToBindTo = { "patientId", "patientName",
    // "deidentifiedPatientId",
    // "deidentifiedPatientName" };
    // strategy.setColumnMapping(memberFieldsToBindTo);
    HeaderColumnNameMappingStrategy<IdentityEntry> strategy = new HeaderColumnNameMappingStrategy<>();
    strategy.setType(IdentityEntry.class);

    String csvFile = context.getProperty(DEIDENTIFICATION_MAP_CVS_FILE).evaluateAttributeExpressions().getValue();
    if (csvFile != null && !csvFile.equals("")) {
      try (InputStreamReader in = new FileReader(csvFile)) {
        CsvToBean<IdentityEntry> csvToBean = new CsvToBeanBuilder<IdentityEntry>(in).withMappingStrategy(strategy)
            .withSkipLines(0).withIgnoreLeadingWhiteSpace(true).build();
        csvToBean.parse().forEach(it -> {
          if (it.getPatientId() == null) {
            getLogger().error("Got line in CSV without a PatientId, discarding");
          } else {
            identityMap.put(it.getPatientId(), it);
          }
        });
      } catch (Exception e) {
        getLogger().error("Error parsing CSV file " + csvFile + ": " + e.getLocalizedMessage());
        throw e;
      }
    }
  }

  @Override
  public String mapUid(String uid) {
    // Return if in cache
    try {
      return uidCache.get(uid);
    } catch (ExecutionException e) {
      getLogger().error("Error looking up", e);
    }
    return UIDUtils.createUID();
  }

  @Override
  public Optional<IdentityEntry> lookupById(String id) {
    return Optional.ofNullable(identityMap.get(id));
  }

  @Override
  public CacheStats getCacheStats() {
    return uidCache.stats();
  }

}

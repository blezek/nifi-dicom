package com.blezek.nifi.dicom;

import com.google.common.cache.CacheStats;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.controller.ControllerService;

import java.util.Optional;

@Tags({ "dicom", "imaging", "deidentification" })
@CapabilityDescription("This controller provides services for deidentification")
public interface DeidentificationService extends ControllerService {
    String mapUid(String uid);

    Optional<IdentityEntry> lookupById(String id);

    CacheStats getCacheStats();
}

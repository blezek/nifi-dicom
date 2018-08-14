package com.blezek.nifi.dicom;

import com.blezek.nifi.dicom.util.BoxUtil;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SideEffectFree;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.lifecycle.OnUnscheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@InputRequirement(Requirement.INPUT_REQUIRED)
@SupportsBatching
@SideEffectFree
@CapabilityDescription("This processor sends FlowFiles to a Box managed user account")
public class PutBox extends AbstractProcessor {

  // Relationships
  public static final Relationship SUCCESS = new Relationship.Builder().name("success")
      .description("FlowFiles that are successfully uploaded will be routed to success").build();
  public static final Relationship FAILURE = new Relationship.Builder().name("failure")
      .description(
          "FlowFiles that failed to send to the remote system; failure is usually looped back to this processor")
      .build();

  // Controllers
  public static final PropertyDescriptor BOX_CONTROLLER = new PropertyDescriptor.Builder().name("Box controller")
      .description("Specify the Box controller").required(true).identifiesControllerService(BoxAPIService.class)
      .build();

  Cache<String, BoxFolder> folderCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

  @OnUnscheduled
  public void unscheduled(ProcessContext context) {
    folderCache.invalidateAll();
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    BoxAPIService boxService = context.getProperty(BOX_CONTROLLER).asControllerService(BoxAPIService.class);
    BoxAPIConnection api = boxService.getConnection();

    for (FlowFile flowfile : session.get(1000)) {
      // Upload to box...
      String filename = flowfile.getAttribute("filename");
      String path = flowfile.getAttribute("path");
      long size = flowfile.getSize();
      try {
        BoxFolder folder = BoxFolder.getRootFolder(api);
        // Pull from cache, create otherwise
        BoxFolder sub = folderCache.get(path, () -> BoxUtil.makeDirectories(folder, path));
        try {
          // Check if we can upload
          sub.canUpload(filename, size);
        } catch (Exception e) {
          session.transfer(flowfile, FAILURE);
          getLogger().error("upload is invalid", e);
          throw new ProcessException("upload is invalid", e);
        }

        // Upload
        try (InputStream is = session.read(flowfile)) {
          sub.uploadFile(is, filename);
          getLogger().info("uploaded new file to " + path + "/" + filename + " of size " + size);
        } catch (IOException e) {
          session.transfer(flowfile, FAILURE);
          getLogger().error("Could not open flowfile", e);
          throw new ProcessException(e);
        }
      } catch (Exception e) {
        session.transfer(flowfile, FAILURE);
        getLogger().error("could not process flowfile", e);
        throw new ProcessException(e);
      }
      session.transfer(flowfile, SUCCESS);
    }
    session.commit();
  }

  @Override
  public Set<Relationship> getRelationships() {
    return ImmutableSet.of(SUCCESS, FAILURE);
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    return ImmutableList.of(BOX_CONTROLLER);
  }

}

package com.blezek.nifi.dicom;

import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.util.TestRunners;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class DocumentationTest {

  @Test
  void testDocs() {
    System.out.println("Starting");
    List<Processor> processors = Arrays.asList(new DeidentifyDICOM(), new ExtractDICOMTags(), new ListenDICOM(),
        new PutDICOM(), new ModifyDICOMTags(), new DeidentifyEncryptDICOM(), new DecryptReidentifyDICOM(),
        new DeidentifyEncryptDICOM());
    processors.forEach(processor -> {
      TestRunners.newTestRunner(processor);
      CapabilityDescription desc = processor.getClass().getAnnotation(CapabilityDescription.class);
      System.out.println("### " + processor.getClass().getSimpleName() + "\n");
      if (desc != null) {
        System.out.println(desc.value());
      }
      System.out.println("\n#### Properties:\n");
      processor.getPropertyDescriptors().forEach(description -> {
        System.out.println("* `" + description.getDisplayName() + "`: " + description.getDescription());
      });
      System.out.println("\n#### Relationships:\n");
      processor.getRelationships().forEach(relationship -> {
        System.out.println("* `" + relationship.getName() + "`: " + relationship.getDescription());
      });
      System.out.println("\n#### FlowFile attributes:\n");
      WritesAttributes writesAttributes = processor.getClass().getAnnotation(WritesAttributes.class);
      if (writesAttributes != null) {
        for (WritesAttribute attribute : writesAttributes.value()) {
          System.out.println("* `" + attribute.attribute() + "`: " + attribute.description());
        }
      } else {
        System.out.println("* **N/A**: does not set attributes");
      }
      System.out.println("");
    });

  }

}

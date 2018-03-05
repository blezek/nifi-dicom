package com.blezek.nifi.dicom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.stream.Stream;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.blezek.nifi.dicom.AttributeStorage;
import com.blezek.nifi.dicom.AttributesMap;
import com.blezek.nifi.dicom.Deidentify;
import com.blezek.nifi.dicom.MockAttributeStorage;
import com.blezek.nifi.dicom.Reidentify;

public class DeidentifyTest {
  private AttributeStorage storage;
  private Attributes attributes;

  @Before
  public void setUp() throws IOException {
    storage = new MockAttributeStorage();
    attributes = TestUtil.getAttributes("/Denoising/CTE_4/Thins/IM-0001-0001.dcm");
  }

  @Test
  public void simpleDeidentification() {
    Deidentify deidentify = new Deidentify();
    Attributes deidentifiedAttributes = deidentify.deidentify(attributes);
    assertThat("patient name", deidentifiedAttributes.getString(Tag.PatientName), equalTo("9ba3182"));
  }

  @Test
  public void roundTrip() {
    Deidentify deidentify = new Deidentify();
    Attributes deidentifiedAttributes = deidentify.deidentify(attributes);
    storage.storeAttributes(new AttributesMap(attributes, deidentifiedAttributes));

    Attributes reidentifiedAttributes = new Reidentify().reidentify(storage, deidentifiedAttributes);

    Stream.of(Tag.PatientName, Tag.StudyInstanceUID, Tag.SeriesInstanceUID, Tag.SOPInstanceUID).forEach(tag -> {
      Assert.assertEquals(tag.toString(), attributes.getString(tag), reidentifiedAttributes.getString(tag));
      Assert.assertNotEquals("deidentified: " + tag.toString(), attributes.getString(tag), deidentifiedAttributes.getString(tag));
    });

  }
}

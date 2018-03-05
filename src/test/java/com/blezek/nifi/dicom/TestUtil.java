package com.blezek.nifi.dicom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.nifi.util.MockFlowFile;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;

public class TestUtil {

  static public Attributes getAttributes(InputStream is) throws IOException {
    try (DicomInputStream dis = new DicomInputStream(is)) {
      dis.setIncludeBulkData(IncludeBulkData.NO);
      return dis.readDataset(-1, -1);
    }
  }

  static public Attributes getAttributes(String path) throws IOException {
    try (InputStream is = TestUtil.class.getResourceAsStream(path)) {
      return getAttributes(is);
    }

  }

  public static Attributes getAttributes(MockFlowFile flowFile) throws IOException {
    try (InputStream is = new ByteArrayInputStream(flowFile.toByteArray())) {
      return getAttributes(is);
    }
  }
}

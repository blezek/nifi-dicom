/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.StructuredReport;

public interface RadiationDoseStructuredReport {

	public StructuredReport getStructuredReport() throws DicomException;

}


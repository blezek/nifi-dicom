/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.StructuredReport;

public interface RadiationDoseStructuredReportFactory {
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr) throws DicomException;
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr,AttributeList list) throws DicomException;
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(AttributeList list) throws DicomException;

}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;

import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link PatientListRequestHandler PatientListRequestHandler} creates a response to an HTTP request for
 * a list of all patients.</p>
 *
 * @author	dclunie
 */
class PatientListRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/PatientListRequestHandler.java,v 1.12 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(PatientListRequestHandler.class);

	protected PatientListRequestHandler(String stylesheetPath) {
		super(stylesheetPath);
	}

	private class CompareDatabaseAttributesByPatientID implements Comparator {
		public int compare(Object o1,Object o2) {
			String si1 = (String)(((Map)o1).get("PATIENTID"));
			String si2 = (String)(((Map)o2).get("PATIENTID"));
			if (si1 == null) si1="";
			if (si2 == null) si2="";
			return si1.compareTo(si2);
		}
	}
	
	private Comparator compareDatabaseAttributesByPatientID = new CompareDatabaseAttributesByPatientID();

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("<html>");
			strbuf.append("<head>");
			strbuf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
			if (stylesheetPath != null) {
				strbuf.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
				strbuf.append(rootURL);
				strbuf.append(stylesheetPath);
				strbuf.append("\">");
			}
			strbuf.append("</head>\r\n");
			strbuf.append("<body><table>\r\n");
			strbuf.append("<tr><th>Patient's ID</th><th>Patient's Name</th></tr>\r\n");
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.PATIENT);
			ArrayList patients = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntity(InformationEntity.PATIENT);

			Collections.sort(patients,compareDatabaseAttributesByPatientID);
			
			int numberOfPatients = patients.size();
			for (int p=0; p<numberOfPatients; ++p) {
				Map patient = (Map)(patients.get(p));
				String patientName = (String)(patient.get("PATIENTNAME"));
				String patientID = (String)(patient.get("PATIENTID"));
				String primaryKey = (String)(patient.get(primaryKeyColumnName));
				strbuf.append("<tr><td><a href=\"");
				strbuf.append(rootURL);
				strbuf.append("?requestType=STUDYLIST&primaryKey=");
				strbuf.append(primaryKey);
				//strbuf.append("\" target=\"navigationWindow\">");
				strbuf.append("\">");
				strbuf.append(patientID == null || patientID.length() == 0 ? "NONE" : patientID);	// need something to click on !);
				strbuf.append("</a>");
				strbuf.append("</td>");
				strbuf.append("<td>");
				strbuf.append(patientName == null ? "&nbsp;" : patientName);		// the value prevents any borders from disappearing because cell empty
				strbuf.append("</td>");
				strbuf.append("</tr>");
			}
			strbuf.append("</table></body></html>\r\n");
			String responseBody = strbuf.toString();
			sendHeaderAndBodyText(out,responseBody,"patients.html","text/html");
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


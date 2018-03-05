/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;

import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link InstanceListRequestHandler InstanceListRequestHandler} creates a response to an HTTP request for
 * a list of instances for a specified series.</p>
 *
 * @author	dclunie
 */
class InstanceListRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/InstanceListRequestHandler.java,v 1.12 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(InstanceListRequestHandler.class);

	protected InstanceListRequestHandler(String stylesheetPath) {
		super(stylesheetPath);
	}
	
	protected String getWADOParametersIdentifyingInstance(String studyInstanceUID,String seriesInstanceUID,String sopInstanceUID) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("&studyUID=");
		strbuf.append(studyInstanceUID);
		strbuf.append("&seriesUID=");
		strbuf.append(seriesInstanceUID);
		strbuf.append("&objectUID=");
		strbuf.append(sopInstanceUID);
		return strbuf.toString();
	}

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			Map parameters = request.getParameters();
			if (parameters == null) {
				throw new Exception("Missing parameters for requestType \""+requestType+"\"");
			}
			String parentPrimaryKey = (String)(parameters.get("primaryKey"));
			if (parentPrimaryKey == null || parentPrimaryKey.length() == 0) {
				throw new Exception("Missing primaryKey parameter for requestType \""+requestType+"\"");
			}
			String studyInstanceUID = (String)(parameters.get("studyUID"));
			if (studyInstanceUID == null || studyInstanceUID.length() == 0) {
				throw new Exception("Missing studyUID parameter for requestType \""+requestType+"\"");
			}
			String seriesInstanceUID = (String)(parameters.get("seriesUID"));
			if (seriesInstanceUID == null || seriesInstanceUID.length() == 0) {
				throw new Exception("Missing seriesUID parameter for requestType \""+requestType+"\"");
			}
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("<html>");
			strbuf.append("<head>");
			if (stylesheetPath != null) {
				strbuf.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
				strbuf.append(rootURL);
				strbuf.append(stylesheetPath);
				strbuf.append("\">");
			}
			strbuf.append("</head>\r\n");
			strbuf.append("<body><table>\r\n");
			strbuf.append("<tr><th>Instance #</th><th>Content Date</th><th>Content Time</th><th>Image Comments</th></tr>\r\n");
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.INSTANCE);
			ArrayList instances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(
				InformationEntity.INSTANCE,parentPrimaryKey);
			int numberOfInstance = instances.size();
			for (int s=0; s<numberOfInstance; ++s) {
				Map instance = (Map)(instances.get(s));
				String instanceNumber = (String)(instance.get("INSTANCENUMBER"));
				String contentDate = (String)(instance.get("CONTENTDATE"));
				String contentTime = (String)(instance.get("CONTENTTIME"));
				String imageComments = (String)(instance.get("IMAGECOMMENTS"));
				String sopInstanceUID = (String)(instance.get("SOPINSTANCEUID"));
				String primaryKey = (String)(instance.get(primaryKeyColumnName));
				strbuf.append("<tr>");
				strbuf.append("<td class=\"centered\">");
				String identifyingParameters = getWADOParametersIdentifyingInstance(studyInstanceUID,seriesInstanceUID,sopInstanceUID);
				{
					strbuf.append("<a href=\"");
					strbuf.append(rootURL);
					strbuf.append("?requestType=WADO");
					strbuf.append("&contentType=image/jpeg");
					strbuf.append(identifyingParameters);
					strbuf.append("\" target=\"pictureWindow\">");
					strbuf.append(instanceNumber == null || instanceNumber.length() == 0 ? "NONE" : instanceNumber);	// need something to click on !
					strbuf.append("</a>");
				}
				strbuf.append(" ");
				{
					strbuf.append("<a href=\"");
					strbuf.append(rootURL);
					strbuf.append("?requestType=WADO");
					strbuf.append("&contentType=application/dicom");
					strbuf.append(identifyingParameters);
					strbuf.append("\" target=\"pictureWindow\">");
					strbuf.append("Save as DICOM");
					strbuf.append("</a>");
				}
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(contentDate == null ? "&nbsp;" : contentDate);
				strbuf.append("</span>");
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(contentTime == null ? "&nbsp;" : contentTime);
				strbuf.append("</td>");
				strbuf.append("<td>");
				strbuf.append(imageComments == null ? "&nbsp;" : imageComments);
				strbuf.append("</td>");
				strbuf.append("</tr>");
			}
			strbuf.append("</table></body></html>\r\n");
			String responseBody = strbuf.toString();
			sendHeaderAndBodyText(out,responseBody,"instance.html","text/html");
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


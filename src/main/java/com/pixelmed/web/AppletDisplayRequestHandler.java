/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.FloatFormatter;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link AppletDisplayRequestHandler AppletDisplayRequestHandler} creates a response to an HTTP request for
 * a page that displays all the images in a specified series.</p>
 *
 * @author	dclunie
 */
class AppletDisplayRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/AppletDisplayRequestHandler.java,v 1.7 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AppletDisplayRequestHandler.class);
	
	private String appletDisplayTemplateFileName;

	protected AppletDisplayRequestHandler(String stylesheetPath,String appletDisplayTemplateFileName) {
		super(stylesheetPath);
		this.appletDisplayTemplateFileName=appletDisplayTemplateFileName;
	}
	
	private static final double[] getDoubleArrayOrNullFromDatabaseStringValue(String stringValue) {
		double[] values = null;
		try {
			if (stringValue != null) {
				values=FloatFormatter.fromString(stringValue,'\\');
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			values=null;
		}
		return values;
	}
	
	private class CompareDatabaseAttributesByInstanceNumber implements Comparator {
		public int compare(Object o1,Object o2) {
			int returnValue = 0;
			String si1 = (String)(((Map)o1).get("INSTANCENUMBER"));
			String si2 = (String)(((Map)o2).get("INSTANCENUMBER"));
			if (si1 == null) si1="";
			if (si2 == null) si2="";
			try {
				int i1 = si1.length() > 0 ? Integer.parseInt(si1) : 0;
				int i2 = si2.length() > 0 ? Integer.parseInt(si2) : 0;
				returnValue = i1 - i2;
			}
			catch (NumberFormatException e) {
				slf4jlogger.error("",e);
				returnValue = si1.compareTo(si2);
			}
			return returnValue;
		}
	}
	
	private Comparator compareDatabaseAttributesByInstanceNumber = new CompareDatabaseAttributesByInstanceNumber();

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
						
			InputStream fileStream = RequestTypeServer.class.getResourceAsStream("/com/pixelmed/web/"+appletDisplayTemplateFileName);
			if (fileStream == null) {
				throw new Exception("No page template \""+appletDisplayTemplateFileName+"\"");
			}
						
			String template = FileUtilities.readFile(fileStream);
			slf4jlogger.trace("generateResponseToGetRequest(): Template is\n{}",template);

			// <PARAM NAME = "imgURL0" VALUE = "http://mars.elcom.nitech.ac.jp/dicom/data/mrangio.dcm">
				
			// replace ####REPLACEMEWITHLISTOFIMAGEURLSASPARAMETERS#### with list of parameters containing WADO DICOM image references
			// of the form:
			//
			//	<PARAM NAME = "imgURLnnn" VALUE = "?requestType=WADO&contentType=application/dicom&studyUID=X&seriesUID=X&objectUID=X>"
			//
						
			StringBuffer imageURLsReplacementStrbuf = new StringBuffer();
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.INSTANCE);
			ArrayList instances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(
				InformationEntity.INSTANCE,parentPrimaryKey);
				
			Collections.sort(instances,compareDatabaseAttributesByInstanceNumber);
			
			//rootURL = "http://192.168.1.100:7091";
			
			int numberOfInstance = instances.size();
			for (int s=0; s<numberOfInstance; ++s) {
				Map instance = (Map)(instances.get(s));
							
				String sopInstanceUID = (String)(instance.get("SOPINSTANCEUID"));
				String primaryKey = (String)(instance.get(primaryKeyColumnName));
				imageURLsReplacementStrbuf.append("<PARAM NAME = \"imgURL");
				imageURLsReplacementStrbuf.append(Integer.toString(s));
				imageURLsReplacementStrbuf.append("\" VALUE = \"");
				imageURLsReplacementStrbuf.append(rootURL);
				imageURLsReplacementStrbuf.append("?requestType=WADO");
				imageURLsReplacementStrbuf.append("&contentType=application/dicom");
				imageURLsReplacementStrbuf.append("&studyUID=");
				imageURLsReplacementStrbuf.append(studyInstanceUID);
				imageURLsReplacementStrbuf.append("&seriesUID=");
				imageURLsReplacementStrbuf.append(seriesInstanceUID);
				imageURLsReplacementStrbuf.append("&objectUID=");
				imageURLsReplacementStrbuf.append(sopInstanceUID);
				imageURLsReplacementStrbuf.append("\">\n");
			}
			imageURLsReplacementStrbuf.append("\n");
						
			String numberOfInstanceReplacement = Integer.toString(numberOfInstance);
			//String numberOfInstanceReplacement = "1";
			slf4jlogger.trace("generateResponseToGetRequest(): numberOfInstanceReplacement is {}",numberOfInstanceReplacement);
			template = template.replaceFirst("####REPLACEMEWITHNUMBEROFIMAGEFILES####",numberOfInstanceReplacement);

			String imageURLsReplacement = imageURLsReplacementStrbuf.toString();
			//String imageURLsReplacement = "<PARAM NAME = \"imgURL0\" VALUE = \"http://mars.elcom.nitech.ac.jp/dicom/data/mrabdo.dcm\">";
			slf4jlogger.trace("generateResponseToGetRequest(): imageURLsReplacement is {}",imageURLsReplacement);
			template = template.replaceFirst("####REPLACEMEWITHLISTOFIMAGEURLSASPARAMETERS####",imageURLsReplacement);
												
			template = template.replaceAll("####REPLACEMEWITHROOTURL####",rootURL);
												
			slf4jlogger.trace("generateResponseToGetRequest(): Response after replacement is\n{}",template);
			sendHeaderAndBodyText(out,template,"imagedisplay.html","text/html");
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


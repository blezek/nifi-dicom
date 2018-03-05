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
 * <p>The {@link ImageDisplayRequestHandler ImageDisplayRequestHandler} creates a response to an HTTP request for
 * a page that displays all the images in a specified series.</p>
 *
 * @author	dclunie
 */
class ImageDisplayRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/ImageDisplayRequestHandler.java,v 1.15 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageDisplayRequestHandler.class);
	
	private String imageDisplayTemplateFileName;

	protected ImageDisplayRequestHandler(String stylesheetPath,String imageDisplayTemplateFileName) {
		super(stylesheetPath);
		this.imageDisplayTemplateFileName=imageDisplayTemplateFileName;
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
						
			InputStream fileStream = RequestTypeServer.class.getResourceAsStream("/com/pixelmed/web/"+imageDisplayTemplateFileName);
			if (fileStream == null) {
				throw new Exception("No page template \""+imageDisplayTemplateFileName+"\"");
			}
						
			String template = FileUtilities.readFile(fileStream);
			slf4jlogger.trace("generateResponseToGetRequest(): Template is\n{}",template);

			// replace ####REPLACEMEWITHLISTOFSOPINSTANCEUIDS#### with list of
			// the form:
			//	"uid1"
			//	,"uid2"
			//	...
			//	,"uidn"
						
			StringBuffer sopInstanceUIDReplacementStrbuf = new StringBuffer();
			StringBuffer windowCenterReplacementStrbuf = new StringBuffer();
			StringBuffer windowWidthReplacementStrbuf = new StringBuffer();
			String prefix = "";
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.INSTANCE);
			ArrayList instances = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(
				InformationEntity.INSTANCE,parentPrimaryKey);
				
			Collections.sort(instances,compareDatabaseAttributesByInstanceNumber);
			
			int numberOfInstance = instances.size();
			for (int s=0; s<numberOfInstance; ++s) {
				Map instance = (Map)(instances.get(s));
							
				String sopInstanceUID = (String)(instance.get("SOPINSTANCEUID"));
				sopInstanceUIDReplacementStrbuf.append(prefix);
				sopInstanceUIDReplacementStrbuf.append("\"");
				sopInstanceUIDReplacementStrbuf.append(sopInstanceUID);
				sopInstanceUIDReplacementStrbuf.append("\"");
							
							
				double[] windowCenters = getDoubleArrayOrNullFromDatabaseStringValue((String)(instance.get("WINDOWCENTER")));
				double windowCenter = windowCenters == null || windowCenters.length == 0 ? 0 : windowCenters[0];
				slf4jlogger.trace("generateResponseToGetRequest(): instance {} windowCenter={}",s,windowCenter);
				windowCenterReplacementStrbuf.append(prefix);
				//windowCenterReplacementStrbuf.append("\"");
				windowCenterReplacementStrbuf.append(windowCenter);
				//windowCenterReplacementStrbuf.append("\"");

				double[] windowWidths  = getDoubleArrayOrNullFromDatabaseStringValue((String)(instance.get("WINDOWWIDTH")));
				double windowWidth = windowWidths == null || windowWidths.length == 0 ? 0 : windowWidths[0];
				slf4jlogger.trace("generateResponseToGetRequest(): instance {} windowWidth={}",s,windowWidth);
				windowWidthReplacementStrbuf.append(prefix);
				//windowWidthReplacementStrbuf.append("\"");
				windowWidthReplacementStrbuf.append(windowWidth);
				//windowWidthReplacementStrbuf.append("\"");
							
				prefix="\n,";
			}
			sopInstanceUIDReplacementStrbuf.append("\n");
			windowCenterReplacementStrbuf.append("\n");
			windowWidthReplacementStrbuf.append("\n");
						
			String sopInstanceUIDReplacement = sopInstanceUIDReplacementStrbuf.toString();
			slf4jlogger.trace("generateResponseToGetRequest(): sopInstanceUIDReplacement is {}",sopInstanceUIDReplacement);
			template = template.replaceFirst("####REPLACEMEWITHLISTOFSOPINSTANCEUIDS####",sopInstanceUIDReplacement);
						
			String windowCenterReplacement = windowCenterReplacementStrbuf.toString();
			slf4jlogger.trace("generateResponseToGetRequest(): windowCenterReplacement is {}",windowCenterReplacement);
			template = template.replaceFirst("####REPLACEMEWITHWINDOWCENTERS####",windowCenterReplacement);
						
			String windowWidthReplacement = windowWidthReplacementStrbuf.toString();
			slf4jlogger.trace("generateResponseToGetRequest(): windowWidthReplacement is {}",windowWidthReplacement);
			template = template.replaceFirst("####REPLACEMEWITHWINDOWWIDTHS####",windowWidthReplacement);
						
			slf4jlogger.trace("generateResponseToGetRequest(): Response after replacement is \n{}",template);
			sendHeaderAndBodyText(out,template,"imagedisplay.html","text/html");
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


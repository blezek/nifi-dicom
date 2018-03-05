/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.DicomStreamCopier;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.display.ConsumerFormatImageMaker;

import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.DicomOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream; 

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link WadoRequestHandler WadoRequestHandler} creates a response to an HHTP request for
 * a WADO request.</p>
 *
 * @author	dclunie
 */
class WadoRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/web/WadoRequestHandler.java,v 1.16 2017/01/24 10:50:52 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(WadoRequestHandler.class);

	//private static final String convertedFormatNameForCodec = "png";
	//private static final String convertedExtension = ".png";
	//private static final String convertedContentType = "image/png";

	private static final String convertedFormatNameForCodec = "jpeg";
	private static final String convertedExtension = ".jpg";
	private static final String convertedContentType = "image/jpeg";

	private class CachedFileEntry {
		String filename;
		Date date;
		
		CachedFileEntry(String filename,Date date) {
			this.filename = filename;
			this.date = date;
		}
	}
	
	private static Map<String,CachedFileEntry> cacheOfConvertedFiles = new HashMap<String,CachedFileEntry>();	// OK to be static as long as we never delete files from cache that may be in use by other threads
	
	private void addToCacheOfConvertedFiles(String key,CachedFileEntry entry) {
		synchronized (cacheOfConvertedFiles) {
			cacheOfConvertedFiles.put(key,entry);
		}
	}
	
	private CachedFileEntry getFromCacheOfConvertedFiles(String key) {
		CachedFileEntry entry = null;
		if (key != null) {
			synchronized (cacheOfConvertedFiles) {
				entry = cacheOfConvertedFiles.get(key);
			}
		}
		return entry;
	}

	private static final String makeCacheKey(String objectUID,double windowCenter,double windowWidth,int columns,int rows,int quality) {
		return objectUID+"#"+Double.toString(windowCenter)+"#"+Double.toString(windowWidth)+"#"+Integer.toString(columns)+"#"+Integer.toString(rows)+"#"+Integer.toString(quality);
	}

	protected WadoRequestHandler(String stylesheetPath) {
		super(stylesheetPath);
	}

	protected void generateResponseToGetRequestForCacheWithoutSending(DatabaseInformationModel databaseInformationModel,WebRequest request,OutputStream out) throws IOException {
		generateResponseToGetRequest(databaseInformationModel,null/*rootURL*/,null/*requestURI*/,request,null/*requestType*/,null/*OutputStream*/);
	}

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			// assert (requestType == null);
			WadoRequest wadoRequest = new WadoRequest(request);
			String objectUID=wadoRequest.getObjectUID();
			// should really check Study and Series Instance UIDs as well, but don't need them to find SOP Instance
			ArrayList records = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(
				InformationEntity.INSTANCE,objectUID);
			if (records != null && records.size() == 1) {
				Map map = (Map)(records.get(0));
				String filename = (String)(map.get(databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE)));
				slf4jlogger.trace("generateResponseToGetRequest(): Found in database {}",filename);
				File file = new File(filename);
				if (file.exists() && file.isFile()) {		
					slf4jlogger.trace("generateResponseToGetRequest(): File exists");
					if (wadoRequest.isContentTypeDicom()) {
						slf4jlogger.trace("generateResponseToGetRequest(): is DICOM request");
						// not paying any attention to requested Transfer Syntax; give them whatever we have
						if (out != null) {
							sendHeaderAndBodyOfFile(out,file,objectUID+".dcm","application/dicom");
						}
						// else do nothing, since we don't cache the DICOM format files, only the converted ones
						
						// make copy with IVRLE and no metaheader
						//File convertedFile  = File.createTempFile("RequestTypeServer",".dcm");
						//convertedFile.deleteOnExit();
						//DicomInputStream  i = new DicomInputStream (new BufferedInputStream(new FileInputStream(file)));
						//DicomOutputStream o = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(convertedFile)),null/*meta*/,TransferSyntax.ImplicitVRLittleEndian/*dataset*/);
						//new DicomStreamCopier(i,o);
						//sendHeaderAndBodyOfFile(out,convertedFile,objectUID+".dcm","application/dicom");
					}
					else {
						slf4jlogger.trace("generateResponseToGetRequest(): is non-DICOM request");
						String sopClassUID = (String)(map.get("SOPCLASSUID"));
						slf4jlogger.trace("generateResponseToGetRequest(): SOP Class UID from database = {}",sopClassUID);
						if (sopClassUID != null) {
							if (SOPClass.isImageStorage(sopClassUID)) {
								double windowWidth = wadoRequest.getWindowWidth();
								double windowCenter = wadoRequest.getWindowCenter();
								int columns = wadoRequest.getColumns();
								int rows = wadoRequest.getRows();
								int quality = wadoRequest.getImageQuality();
								File convertedFile = null;
								Date conversionDate = null;
								String cacheKey = makeCacheKey(objectUID,windowCenter,windowWidth,columns,rows,quality);
								CachedFileEntry cacheEntry = getFromCacheOfConvertedFiles(cacheKey);
								try {
									if (cacheEntry == null) {
										slf4jlogger.trace("generateResponseToGetRequest(): not in cache");
										convertedFile = File.createTempFile("RequestTypeServer",convertedExtension);
										convertedFile.deleteOnExit();
										String convertedFileName = convertedFile.getAbsolutePath();
										ConsumerFormatImageMaker.convertFileToEightBitImage(
											filename,convertedFileName,convertedFormatNameForCodec,windowCenter,windowWidth,columns,rows,quality,
											ConsumerFormatImageMaker.ALL_ANNOTATIONS+"_"+ConsumerFormatImageMaker.COLOR_ANNOTATIONS);
										conversionDate = new Date();
										cacheEntry = new CachedFileEntry(convertedFileName,conversionDate);
										addToCacheOfConvertedFiles(cacheKey,cacheEntry);
									}
									else {
										slf4jlogger.trace("generateResponseToGetRequest(): in cache");
										convertedFile = new File(cacheEntry.filename);
										conversionDate = cacheEntry.date;
									}
									if (out != null) {
										try {
											sendHeaderAndBodyOfFile(out,convertedFile,objectUID+convertedExtension,convertedContentType,conversionDate);
										}
										catch (java.net.SocketException e) {
											// Ignore broken pipe exception since it usually means browser has scrolled passed and doesn't want this image anymore
										}
									}
									// else are just filling cache in advance of need to send the data
								}
								catch (Exception e) {
									slf4jlogger.error("Cannot convert image in file {} to {} or send it",convertedFile,convertedFormatNameForCodec,e);
									throw new Exception("Cannot convert image to "+convertedFormatNameForCodec+" or send it");
								}
								finally {
									slf4jlogger.trace("generateResponseToGetRequest(): convertedFile = {}",convertedFile.getAbsolutePath());
									//convertedFile.delete();	// don't delete, since caching
								}
							}
							else {
								throw new Exception("Only images supported");
							}
						}
						else {
							//throw new Exception("Unsupported contentType");
							throw new Exception("Cannot determine SOP Class of instance");
						}
					}
				}
				else {
					throw new Exception("SOP Instance "+objectUID+" in database but file \""+filename+"\" referenced by database missing");
				}
			}
			else {
				throw new Exception("Could not find SOP Instance "+objectUID);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			slf4jlogger.debug("generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


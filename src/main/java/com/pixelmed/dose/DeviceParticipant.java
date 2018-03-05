/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.utils.HexDump;
import com.pixelmed.utils.UUIDBasedOID;

import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.UUID;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class DeviceParticipant {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/DeviceParticipant.java,v 1.10 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DeviceParticipant.class);

	protected String manufacturer;
	protected String modelName;
	protected String serialNumber;
	protected String uid;
	
	/**
	 * @deprecated	will create SR template missing Device Observer UID since CP 1065 added it as mandatory (000653)
	 */
	public DeviceParticipant(String manufacturer,String modelName,String serialNumber) {
		this.manufacturer = manufacturer;
		this.modelName = modelName;
		this.serialNumber = serialNumber;
		this.uid = null;
	}
	
	public DeviceParticipant(String manufacturer,String modelName,String serialNumber,String uid) {
		this.manufacturer = manufacturer;
		this.modelName = modelName;
		this.serialNumber = serialNumber;
		this.uid = uid;
	}

	public DeviceParticipant(ContentItem parent) {
		ContentItem deviceContentItem = parent.getNamedChild("DCM","113876");	// "Device Role in Procedure"
		if (deviceContentItem != null
		 && deviceContentItem instanceof ContentItemFactory.CodeContentItem
		 && ((ContentItemFactory.CodeContentItem)deviceContentItem).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("113859","DCM")) {	// "Irradiating Device"
			manufacturer = ContentItem.getSingleStringValueOrNullOfNamedChild(deviceContentItem,"DCM","113878");	// "Device Manufacturer"
			modelName    = ContentItem.getSingleStringValueOrNullOfNamedChild(deviceContentItem,"DCM","113879");	// "Device Model Name"
			serialNumber = ContentItem.getSingleStringValueOrNullOfNamedChild(deviceContentItem,"DCM","113880");	// "Device Serial Number"
			uid          = ContentItem.getSingleStringValueOrNullOfNamedChild(deviceContentItem,"DCM","121012");	// "Device Observer UID"
		}
	}

	public String getManufacturer() { return manufacturer; }
	public String getModelName()    { return modelName; }
	public String getSerialNumber() { return serialNumber; }
	public String getUID()          { return uid; }
	
	public ContentItem getStructuredReportFragment() throws DicomException {
		ContentItemFactory cif = new ContentItemFactory();
		ContentItem root = cif.new CodeContentItem(null,"CONTAINS",new CodedSequenceItem("113876","DCM","Device Role in Procedure"),new CodedSequenceItem("113859","DCM","Irradiating Device"));
		if (manufacturer != null && manufacturer.trim().length() > 0) { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113878","DCM","Device Manufacturer"),manufacturer); }
		if (modelName != null && modelName.trim().length() > 0)       { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113879","DCM","Device Model Name"),modelName); }
		if (serialNumber != null && serialNumber.trim().length() > 0) { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113880","DCM","Device Serial Number"),serialNumber); }
		if (uid != null && uid.trim().length() > 0)                   { cif.new UIDContentItem (root,"HAS PROPERTIES",new CodedSequenceItem("121012","DCM","Device Observer UID"),uid); }
		return root;
	}
	
	// static convenience methods
	
	/**
	 * <p>Extract the device serial number information from a list of attributes, or some suitable alternate if available.</p>
	 *
	 * <p>Makes a hash of StationName and Institution as an alternate, if either or both present and not empty.</p>
	 *
	 * @param	list						the list of attributes
	 * @param	insertAlternateBackInList	if true, when there is no DeviceSerialNumber or it is empty, add the alterate created back to the supplied list (side effect of call)
	 * @return								a string containing either the DeviceSerialNumber from the list or a suitable alternate if available, else null 
	 */
	public static String getDeviceSerialNumberOrSuitableAlternative(AttributeList list,boolean insertAlternateBackInList) {
		String useDeviceSerialNumber = Attribute.getSingleStringValueOrNull(list,TagFromName.DeviceSerialNumber);
		if (useDeviceSerialNumber == null || useDeviceSerialNumber.trim().length() == 0) {
			String institutionName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstitutionName);
			String stationName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StationName);
			if (institutionName.length() > 0 || stationName.length() > 0) {
				try {
					byte[] b = (institutionName+"|"+stationName).getBytes("UTF8");
					useDeviceSerialNumber = HexDump.byteArrayToHexString(MessageDigest.getInstance("SHA").digest(b));
					if (insertAlternateBackInList) {
						Attribute a = new LongStringAttribute(TagFromName.DeviceSerialNumber);
						a.addValue(useDeviceSerialNumber);
						list.put(a);
					}
				}
				catch (UnsupportedEncodingException e) {
					slf4jlogger.error("",e);
					useDeviceSerialNumber = null;
				}
				catch (NoSuchAlgorithmException e) {
					slf4jlogger.error("",e);
					useDeviceSerialNumber = null;
				}
				catch (DicomException e) {
					slf4jlogger.error("",e);
				}
			}
		}
		return useDeviceSerialNumber;
	}
	
	private static final UUID nameSpaceForType3UIDForDeviceObserverUID = UUID.fromString("71E7C730-7EA2-11E1-9CFD-7CD04824019B");		// This is just a random UUID, but once used must stay the same
	
	/**
	 * <p>Extract the device observer UID information from a list of attributes, or some suitable alternate if available.</p>
	 *
	 * <p>Makes a hash of DeviceSerialNumber, StationName, Institution, Manufacturer, Manufacturer Model Name as an alternate, if any are present and not empty.</p>
	 *
	 * @param	list						the list of attributes
	 * @return								a string containing a suitable UID if available, else null 
	 */
	public static String getDeviceObserverUIDOrSuitableAlternative(AttributeList list) {
		String useUID = null;
		// there is no top level attribute in the DICOM data set to try to use
		{
			String deviceSerialNumber = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.DeviceSerialNumber);
			String institutionName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstitutionName);
			String stationName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StationName);
			String manufacturer = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer);
			String modelName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ManufacturerModelName);

			if (deviceSerialNumber.length() > 0 || institutionName.length() > 0 || stationName.length() > 0 || manufacturer.length() > 0 || modelName.length() > 0) {
				try {
					byte[] bName = (deviceSerialNumber+"|"+institutionName+"|"+stationName+"|"+manufacturer+"|"+modelName).getBytes("UTF8");
					useUID = new UUIDBasedOID(nameSpaceForType3UIDForDeviceObserverUID,bName).getOID();	// creates a UID based on a Type 3 (MD5 hash) UUID
//System.err.println("DeviceParticipant.getDeviceObserverUIDOrSuitableAlternative(): UID.length() = "+useUID.length());
//System.err.println("DeviceParticipant.getDeviceObserverUIDOrSuitableAlternative(): UID = "+useUID);
					// there is no top level attribute in the DICOM data set to add this back to the list as
				}
				catch (UnsupportedEncodingException e) {
					slf4jlogger.error("",e);
					useUID = null;
				}
			}
		}
		return useUID;
	}

}

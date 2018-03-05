/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import java.util.EnumMap;
import java.util.Map;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;

public class RecordingDeviceObserverContext {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/RecordingDeviceObserverContext.java,v 1.7 2017/01/24 10:50:42 dclunie Exp $";
	
	public enum Key {
		DEVICE,
		UID,
		NAME,
		MANUFACTURER,
		MODEL_NAME,
		SERIAL_NUMBER,
		LOCATION
	}
	
	protected String uid;
	protected String name;
	protected String manufacturer;
	protected String modelName;
	protected String serialNumber;
	protected String location;
	
	public RecordingDeviceObserverContext(String uid,String name,String manufacturer,String modelName,String serialNumber,String location) {
		this.uid = uid;
		this.name = name;
		this.manufacturer = manufacturer;
		this.modelName = modelName;
		this.serialNumber = serialNumber;
		this.location = location;
	}
	
	public RecordingDeviceObserverContext(ContentItem root) {
		ContentItem observerType = root.getNamedChild("DCM","121005");	// "Observer Type"
		if (observerType != null
		 && observerType instanceof ContentItemFactory.CodeContentItem
		 && ((ContentItemFactory.CodeContentItem)observerType).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("121007","DCM")) {	// "Device"
			uid          = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121012");	// "Device Observer UID"
			name         = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121013");	// "Device Observer Name"
			manufacturer = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121014");	// "Device Observer Manufacturer"
			modelName    = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121015");	// "Device Observer Model Name"
			serialNumber = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121016");	// "Device Observer Serial Number"
			location     = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","121017");	// "Device Observer Physical Location During Observation"
		}
	}
	
	public String getUID() { return uid; }
	public String getName() { return name; }
	public String getManufacturer() { return manufacturer; }
	public String getModelName() { return modelName; }
	public String getSerialNumber() { return serialNumber; }
	public String getLocation() { return location; }
	
	public Map<Key,ContentItem> getStructuredReportFragment() throws DicomException {
		Map<Key,ContentItem> map = new EnumMap<Key,ContentItem>(Key.class);
		ContentItemFactory cif = new ContentItemFactory();
		map.put(Key.DEVICE,			cif.new CodeContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121005","DCM","Observer Type"),new CodedSequenceItem("121007","DCM","Device")));
		if (uid != null && uid.trim().length() > 0)                   { map.put(Key.UID,			cif.new UIDContentItem (null,"HAS OBS CONTEXT",new CodedSequenceItem("121012","DCM","Device Observer UID"),uid)); }
		if (name != null && name.trim().length() > 0)                 { map.put(Key.NAME,			cif.new TextContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121013","DCM","Device Observer Name"),name)); }
		if (manufacturer != null && manufacturer.trim().length() > 0) { map.put(Key.MANUFACTURER,	cif.new TextContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121014","DCM","Device Observer Manufacturer"),manufacturer)); }
		if (modelName != null && modelName.trim().length() > 0)       { map.put(Key.MODEL_NAME,		cif.new TextContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121015","DCM","Device Observer Model Name"),modelName)); }
		if (serialNumber != null && serialNumber.trim().length() > 0) { map.put(Key.SERIAL_NUMBER,	cif.new TextContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121016","DCM","Device Observer Serial Number"),serialNumber)); }
		if (location != null && location.trim().length() > 0)         { map.put(Key.LOCATION,	    cif.new TextContentItem(null,"HAS OBS CONTEXT",new CodedSequenceItem("121017","DCM","Device Observer Physical Location During Observation"),location)); }
		return map;
	}
	
}

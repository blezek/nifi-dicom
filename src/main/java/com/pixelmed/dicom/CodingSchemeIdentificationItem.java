/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;
import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to encapsulate the attributes of an Item of CodingSchemeIdentificationSequence.</p>
 *
 * @author	dclunie
 */
public class CodingSchemeIdentificationItem {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/CodingSchemeIdentificationItem.java,v 1.7 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CodingSchemeIdentificationItem.class);

	protected String codingSchemeDesignator;
	protected String codingSchemeRegistry;
	protected String codingSchemeUID;
	protected String codingSchemeName;
		
	public CodingSchemeIdentificationItem(String codingSchemeDesignator,String codingSchemeRegistry,String codingSchemeUID,String codingSchemeName) {
		this.codingSchemeDesignator=codingSchemeDesignator;
		this.codingSchemeRegistry=codingSchemeRegistry;
		this.codingSchemeUID=codingSchemeUID;
		this.codingSchemeName=codingSchemeName;
	}
	
	public CodingSchemeIdentificationItem(SequenceItem item) {
		if (item != null) {
			AttributeList list = item.getAttributeList();
			codingSchemeDesignator = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeDesignator);
			codingSchemeRegistry = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeRegistry);
			codingSchemeUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeUID);
			codingSchemeName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeName);
		}
	}

	public String getCodingSchemeDesignator() { return codingSchemeDesignator;}
	public String getCodingSchemeRegistry() { return codingSchemeRegistry;}
	public String getCodingSchemeUID() { return codingSchemeUID;}
	public String getCodingSchemeName() { return codingSchemeName;}
	
	public SequenceItem getAsSequenceItem() {
		AttributeList list = new AttributeList();
		try {
			if (codingSchemeDesignator != null && codingSchemeDesignator.length() > 0) { Attribute a = new ShortStringAttribute(TagFromName.CodingSchemeDesignator); a.addValue(codingSchemeDesignator); list.put(a); }
			if (codingSchemeRegistry != null && codingSchemeRegistry.length() > 0) { Attribute a = new LongStringAttribute(TagFromName.CodingSchemeRegistry); a.addValue(codingSchemeRegistry); list.put(a); }
			if (codingSchemeUID != null && codingSchemeUID.length() > 0) { Attribute a = new UniqueIdentifierAttribute(TagFromName.CodingSchemeUID); a.addValue(codingSchemeUID); list.put(a); }
			if (codingSchemeName != null && codingSchemeName.length() > 0) { Attribute a = new ShortTextAttribute(TagFromName.CodingSchemeName); a.addValue(codingSchemeName); list.put(a); }
		}
		catch (DicomException e) {
			slf4jlogger.error("", e);
		}
		
		SequenceItem item = new SequenceItem(list);
		return item;
	}
}


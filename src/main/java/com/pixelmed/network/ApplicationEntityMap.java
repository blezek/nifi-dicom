/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * <p>This class provides a list of known Application Entities, indexed by AET.</p>
 *
 * @author	dclunie
 */
public class ApplicationEntityMap extends TreeMap {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ApplicationEntityMap.java,v 1.14 2017/01/24 10:50:44 dclunie Exp $";

	/**
	 * <p>Add an entry for the specified AE.</p>
	 *
	 * @param	applicationEntityTitle	the AE to describe
	 * @param	presentationAddress	the presentation address of the AE
	 * @param	queryModel		the string label of the query model, or null if AE does not support queries or model is unknown
	 * @param	primaryDeviceType	the primaryDeviceType (may be multiple comma-separated values), or null if none or unknown
	 */
	public void put(String applicationEntityTitle,PresentationAddress presentationAddress,String queryModel,String primaryDeviceType) {
		put(applicationEntityTitle,new ApplicationEntity(applicationEntityTitle,presentationAddress,queryModel,primaryDeviceType));
	}

	/**
	 * <p>Add an entry for the specified AE.</p>
	 *
	 * @param	applicationEntityTitle	the AE to describe
	 * @param	entry					the AE entry
	 */
	private void put(String applicationEntityTitle,ApplicationEntity entry) {
		super.put(applicationEntityTitle,entry);
	}

	/***/
	public Object put(Object key,Object value) {
		if (value instanceof ApplicationEntity) {
			return super.put(key,value);
		}
		else {
			throw new RuntimeException("Internal error - attempting to add class other than ApplicationEntity");
		}
	}

	/**
	 * <p>Return the presentation address of the specified AE.</p>
	 *
	 * @param	applicationEntityTitle
	 * @return				the presentation address, or null if no such AE
	 */
	public PresentationAddress getPresentationAddress(String applicationEntityTitle) {
//System.err.println("ApplicationEntityMap.getPresentationAddress(): applicationEntityTitle = "+applicationEntityTitle);
//System.err.println("ApplicationEntityMap.getPresentationAddress(): get(applicationEntityTitle).getClass() = "+get(applicationEntityTitle).getClass());
		ApplicationEntity e = (ApplicationEntity)(applicationEntityTitle == null ? null : get(applicationEntityTitle));
		return e == null ? null : e.getPresentationAddress();
	}
	
	/**
	 * <p>Return the query model supported by the specified AE.</p>
	 *
	 * <p>The query model string may be {@link NetworkApplicationProperties#StudyRootQueryModel NetworkApplicationProperties.StudyRootQueryModel}
	 * or {@link NetworkApplicationProperties#PatientRootQueryModel NetworkApplicationProperties.PatientRootQueryModel}
	 * or {@link NetworkApplicationProperties#PatientStudyOnlyQueryModel NetworkApplicationProperties.PatientStudyOnlyQueryModel}.</p>
	 *
	 * @param	applicationEntityTitle
	 * @return				string label of the query model, or null if no such AE or no AE does not support queries
	 */
	public String getQueryModel(String applicationEntityTitle) {
		ApplicationEntity e = (ApplicationEntity)(get(applicationEntityTitle));
		return e == null ? null : e.getQueryModel();
	}
	
	/**
	 * <p>Return the primary device type of the specified AE.</p>
	 *
	 * @param	applicationEntityTitle
	 * @return				primary device type, or null if none or not known
	 */
	public String getPrimaryDeviceType(String applicationEntityTitle) {
		ApplicationEntity e = (ApplicationEntity)(get(applicationEntityTitle));
		return e == null ? null : e.getPrimaryDeviceType();
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		Iterator i = keySet().iterator();
		while (i.hasNext()) {
			String aet = (String)i.next();
			ApplicationEntity ae = (ApplicationEntity)get(aet);
			strbuf.append("AE [");
			strbuf.append(ae == null ? "" : ae.toString());
			strbuf.append("]\n");
		}
		return strbuf.toString();
	}

}


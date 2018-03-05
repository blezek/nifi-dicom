/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.transfermonitor;

import java.rmi.server.UID;

/**
 * @author	dclunie
 */
public class TransferMonitoringContext {

	/***/
	static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/transfermonitor/TransferMonitoringContext.java,v 1.4 2017/01/24 10:50:51 dclunie Exp $";
	
	/***/
	private UID uid;
	
	/***/
	private String description;
	
	/***/
	private boolean closed;
	
	/**
	 */
	public TransferMonitoringContext(String description) {
		uid = new UID();
		this.description=description;
		closed=false;
	}
	
	/**
	 * <p>Update the description, such as when we have received more information like the name of the sender.</p>
	 * 
	 * @param	description	new description
	 */
	public void setDescription(String description) {
		this.description=description;
	}
	
	/**
	 * <p>Close the monitoring context since no more transfers are expected.</p>
	 */
	public void close() {
		closed=true;
	}
	
	/**
	 * @return	true if monitoring context has been closed and no more transfers are expected
	 */
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * @param	obj
	 */
	public boolean equals(Object obj) {
		return uid.equals(obj);
	}
	
	/**
	 * @return	hash code of context
	 */
	public int hashCode() {
		return uid.hashCode();
	}

	/**
	 * @return	description of context
	 */
	public String toString() {
		return description+"("+uid.toString()+")";
	}
}

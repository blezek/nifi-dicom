/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.lang.StringBuffer;

/**
 * @author	dclunie
 */
public class SCUSCPRoleSelection {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/SCUSCPRoleSelection.java,v 1.5 2017/01/24 10:50:46 dclunie Exp $";

	private String abstractSyntaxUID;
	/***/
	private boolean scuRole;
	/***/
	private boolean scpRole;

	/**
	 * @param	abstractSyntaxUID
	 */
	public SCUSCPRoleSelection(String abstractSyntaxUID,byte scuRole,byte scpRole) {
		this.abstractSyntaxUID=abstractSyntaxUID;
		this.scuRole=false;
		this.scpRole=false;
	}

	/**
	 * @param	abstractSyntaxUID
	 * @param	scuRole			true if supported
	 * @param	scpRole			true if supported
	 */
	public SCUSCPRoleSelection(String abstractSyntaxUID,boolean scuRole,boolean scpRole) {
		this.abstractSyntaxUID=abstractSyntaxUID;
		this.scuRole=scuRole;
		this.scpRole=scpRole;
	}

	/***/
	public String getAbstractSyntaxUID() {
		return abstractSyntaxUID;
	}

	/***/
	public boolean isSCURoleSupported() 			{ return scuRole; }
	
	/**
	 * @param	supported	true if supported, false if not
	 */
	public void setSCURoleSupported(boolean supported) 	{ this.scuRole=supported; }

	/***/
	public boolean isSCPRoleSupported() 			{ return scpRole; }
	
	/**
	 * @param	supported	true if supported, false if not
	 */
	public void setSCPRoleSupported(boolean supported) 	{ this.scpRole=supported; }

	/***/
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("SCU/SCP Role Selection:");
		sb.append("\tAbstract Syntax:\n\t\t");
		sb.append(abstractSyntaxUID);
		sb.append("\n");
		sb.append("\t\tSCU Role supported: ");
		sb.append(scuRole);
		sb.append("\n");
		sb.append("\t\tSCP Role supported: ");
		sb.append(scpRole);
		sb.append("\n");
		return sb.toString();
	}
}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

/**
 * <p>This class provides a description of the parameters of a known Application Entity.</p>
 *
 * @author	dclunie
 */
public class ApplicationEntity {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ApplicationEntity.java,v 1.10 2017/01/24 10:50:44 dclunie Exp $";
	
	protected String dicomAETitle;
	protected PresentationAddress presentationAddress;
	protected String queryModel;
	protected String primaryDeviceType;
	
	public boolean equals(Object obj) {
		if (obj instanceof ApplicationEntity) {
			ApplicationEntity aeComp = (ApplicationEntity)obj;
			return ((dicomAETitle == null && aeComp.getDicomAETitle() == null) || dicomAETitle.equals(aeComp.getDicomAETitle()))
				&& ((presentationAddress == null && aeComp.getPresentationAddress() == null) || presentationAddress.equals(aeComp.getPresentationAddress()))
				&& ((queryModel == null && aeComp.getQueryModel() == null) || queryModel.equals(aeComp.getQueryModel()))
				&& ((primaryDeviceType == null && aeComp.getPrimaryDeviceType() == null) || primaryDeviceType.equals(aeComp.getPrimaryDeviceType()))
				;
		}
		else {
			return super.equals(obj);
		}
	}
		
	public ApplicationEntity(ApplicationEntity ae) {
		this.dicomAETitle = ae.dicomAETitle;
		this.presentationAddress = ae.presentationAddress;
		this.queryModel = ae.queryModel;
		this.primaryDeviceType = ae.primaryDeviceType;
	}
	
	public ApplicationEntity(String dicomAETitle) {
		this.dicomAETitle = dicomAETitle;
		this.presentationAddress = null;
		this.queryModel = null;
		this.primaryDeviceType = null;
	}
	
	public ApplicationEntity(String dicomAETitle,PresentationAddress presentationAddress,String queryModel,String primaryDeviceType) {
		this.dicomAETitle = dicomAETitle;
		this.presentationAddress = presentationAddress;
		this.queryModel = queryModel;
		this.primaryDeviceType = primaryDeviceType;
	}
	
	public void setPresentationAddress(PresentationAddress presentationAddress) {
		this.presentationAddress = presentationAddress;
	}
	
	public void setQueryModel(String queryModel) {
		this.queryModel = queryModel;
	}
	
	public void setPrimaryDeviceType(String primaryDeviceType) {
		this.primaryDeviceType = primaryDeviceType;
	}
		
	public final String getDicomAETitle() { return dicomAETitle; }
	public final PresentationAddress getPresentationAddress() { return presentationAddress; }
	public final String getQueryModel() { return queryModel; }
	public final String getPrimaryDeviceType() { return primaryDeviceType; }

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("dicomAETitle=");
		strbuf.append(dicomAETitle);
		if (presentationAddress != null) {
			strbuf.append(",hostname=");
			strbuf.append(presentationAddress.getHostname());
			strbuf.append(",port=");
			strbuf.append(presentationAddress.getPort());
		}
		strbuf.append(",queryModel=");
		strbuf.append(queryModel);
		strbuf.append(",primaryDeviceType=");
		strbuf.append(primaryDeviceType);
		return strbuf.toString();
	}
}


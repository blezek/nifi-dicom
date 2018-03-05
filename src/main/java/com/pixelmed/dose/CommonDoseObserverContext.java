/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

public class CommonDoseObserverContext {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CommonDoseObserverContext.java,v 1.7 2017/01/24 10:50:42 dclunie Exp $";
	
	RecordingDeviceObserverContext recordingDeviceObserverContext;
	DeviceParticipant deviceParticipant;
	PersonParticipant personParticipantAdministering;
	PersonParticipant personParticipantAuthorizing;

	public CommonDoseObserverContext() {
	}

	public CommonDoseObserverContext(String uid,String name,String manufacturer,String modelName,String serialNumber,String location,
			String operatorName,String operatorID,String physicianName,String physicianID,String idIssuer,String organization
	) {
		recordingDeviceObserverContext = new RecordingDeviceObserverContext(uid,name,manufacturer,modelName,serialNumber,location);
		deviceParticipant              = new DeviceParticipant(manufacturer,modelName,serialNumber,uid);
		personParticipantAdministering = new PersonParticipant(operatorName, RoleInProcedure.IRRADIATION_ADMINISTERING,operatorID, idIssuer,organization,RoleInOrganization.TECHNOLOGIST);
		personParticipantAuthorizing   = new PersonParticipant(physicianName,RoleInProcedure.IRRADIATION_AUTHORIZING,  physicianID,idIssuer,organization,RoleInOrganization.PHYSICIAN);
	}
	
	public RecordingDeviceObserverContext getRecordingDeviceObserverContext() { return recordingDeviceObserverContext; }
	public void setRecordingDeviceObserverContext(RecordingDeviceObserverContext recordingDeviceObserverContext) { this.recordingDeviceObserverContext = recordingDeviceObserverContext; }
	
	public DeviceParticipant getDeviceParticipant() { return deviceParticipant; }
	public void setDeviceParticipant(DeviceParticipant deviceParticipant) { this.deviceParticipant = deviceParticipant; }
	
	public PersonParticipant getPersonParticipantAdministering() { return personParticipantAdministering; }
	public void setPersonParticipantAdministering(PersonParticipant personParticipantAdministering) { this.personParticipantAdministering = personParticipantAdministering; }
	
	public PersonParticipant getPersonParticipantAuthorizing() { return personParticipantAuthorizing; }
	public void setPersonParticipantAuthorizing(PersonParticipant personParticipantAuthorizing) { this.personParticipantAuthorizing = personParticipantAuthorizing; }
	
	
}

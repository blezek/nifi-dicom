/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.*;

public class CTDoseCheckDetails {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CTDoseCheckDetails.java,v 1.5 2017/01/24 10:50:42 dclunie Exp $";
	
	protected boolean alertDLPValueConfigured;
	protected String  alertDLPValue;
	protected boolean alertCTDIvolValueConfigured;
	protected String  alertCTDIvolValue;
	protected String  alertAccumulatedDLPForwardEstimate;
	protected String  alertAccumulatedCTDIvolForwardEstimate;
	protected String  alertReasonForProceeding;
	protected PersonParticipant alertPerson;
	
	protected boolean notificationDLPValueConfigured;
	protected String  notificationDLPValue;
	protected boolean notificationCTDIvolValueConfigured;
	protected String  notificationCTDIvolValue;
	protected String  notificationDLPForwardEstimate;
	protected String  notificationCTDIvolForwardEstimate;
	protected String  notificationReasonForProceeding;
	protected PersonParticipant notificationPerson;

	public boolean equals(Object o) {
//System.err.println("CTDoseCheckDetails.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof CTDoseCheckDetails) {
			CTDoseCheckDetails dcd = (CTDoseCheckDetails)o;
			isEqual =
			   (dcd.getAlertDLPValueConfigured() == this.getAlertDLPValueConfigured())
			&& ((dcd.getAlertDLPValue() == null && this.getAlertDLPValue() == null) || (dcd.getAlertDLPValue().equals(this.getAlertDLPValue())))
			&& (dcd.getAlertCTDIvolValueConfigured() == this.getAlertCTDIvolValueConfigured())
			&& ((dcd.getAlertCTDIvolValue() == null && this.getAlertCTDIvolValue() == null) || (dcd.getAlertCTDIvolValue().equals(this.getAlertCTDIvolValue())))
			&& ((dcd.getAlertAccumulatedDLPForwardEstimate() == null && this.getAlertAccumulatedDLPForwardEstimate() == null) || (dcd.getAlertAccumulatedDLPForwardEstimate().equals(this.getAlertAccumulatedDLPForwardEstimate())))
			&& ((dcd.getAlertAccumulatedCTDIvolForwardEstimate() == null && this.getAlertAccumulatedCTDIvolForwardEstimate() == null) || (dcd.getAlertAccumulatedCTDIvolForwardEstimate().equals(this.getAlertAccumulatedCTDIvolForwardEstimate())))
			&& ((dcd.getAlertReasonForProceeding() == null && this.getAlertReasonForProceeding() == null) || (dcd.getAlertReasonForProceeding().equals(this.getAlertReasonForProceeding())))
			&& ((dcd.getAlertPerson() == null && this.getAlertPerson() == null) || (dcd.getAlertPerson().equals(this.getAlertPerson())))
			&& (dcd.getNotificationDLPValueConfigured() == this.getNotificationDLPValueConfigured())
			&& ((dcd.getNotificationDLPValue() == null && this.getNotificationDLPValue() == null) || (dcd.getNotificationDLPValue().equals(this.getNotificationDLPValue())))
			&& (dcd.getNotificationCTDIvolValueConfigured() == this.getNotificationCTDIvolValueConfigured())
			&& ((dcd.getNotificationCTDIvolValue() == null && this.getNotificationCTDIvolValue() == null) || (dcd.getNotificationCTDIvolValue().equals(this.getNotificationCTDIvolValue())))
			&& ((dcd.getNotificationDLPForwardEstimate() == null && this.getNotificationDLPForwardEstimate() == null) || (dcd.getNotificationDLPForwardEstimate().equals(this.getNotificationDLPForwardEstimate())))
			&& ((dcd.getNotificationCTDIvolForwardEstimate() == null && this.getNotificationCTDIvolForwardEstimate() == null) || (dcd.getNotificationCTDIvolForwardEstimate().equals(this.getNotificationCTDIvolForwardEstimate())))
			&& ((dcd.getNotificationReasonForProceeding() == null && this.getNotificationReasonForProceeding() == null) || (dcd.getNotificationReasonForProceeding().equals(this.getNotificationReasonForProceeding())))
			&& ((dcd.getNotificationPerson() == null && this.getNotificationPerson() == null) || (dcd.getNotificationPerson().equals(this.getNotificationPerson())))
			;
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}

	public CTDoseCheckDetails(
			boolean alertDLPValueConfigured,
			String  alertDLPValue,
			boolean alertCTDIvolValueConfigured,
			String  alertCTDIvolValue,
			String  alertAccumulatedDLPForwardEstimate,
			String  alertAccumulatedCTDIvolForwardEstimate,
			String  alertReasonForProceeding,
			PersonParticipant alertPerson,
			boolean notificationDLPValueConfigured,
			String  notificationDLPValue,
			boolean notificationCTDIvolValueConfigured,
			String  notificationCTDIvolValue,
			String  notificationDLPForwardEstimate,
			String  notificationCTDIvolForwardEstimate,
			String  notificationReasonForProceeding,
			PersonParticipant notificationPerson) {
		this.alertDLPValueConfigured = alertDLPValueConfigured;
		this.alertDLPValue = alertDLPValue;
		this.alertCTDIvolValueConfigured = alertCTDIvolValueConfigured;
		this.alertCTDIvolValue = alertCTDIvolValue;
		this.alertAccumulatedDLPForwardEstimate = alertAccumulatedDLPForwardEstimate;
		this.alertAccumulatedCTDIvolForwardEstimate = alertAccumulatedCTDIvolForwardEstimate;
		this.alertReasonForProceeding = alertReasonForProceeding;
		this.alertPerson = alertPerson;
		this.notificationDLPValueConfigured = notificationDLPValueConfigured;
		this.notificationDLPValue = notificationDLPValue;
		this.notificationCTDIvolValueConfigured = notificationCTDIvolValueConfigured;
		this.notificationCTDIvolValue = notificationCTDIvolValue;
		this.notificationDLPForwardEstimate = notificationDLPForwardEstimate;
		this.notificationCTDIvolForwardEstimate = notificationCTDIvolForwardEstimate;
		this.notificationReasonForProceeding = notificationReasonForProceeding;
		this.notificationPerson = notificationPerson;
	}
	
	public CTDoseCheckDetails(ContentItem dose) {
//System.err.println("CTDoseCheckDetails(ContentItem):");
		ContentItem alertDetailsFragment = dose.getNamedChild("DCM","113900");	// "Dose Check Alert Details"
		if (alertDetailsFragment != null) {
			ContentItem alertDLPValueConfiguredContentItem = alertDetailsFragment.getNamedChild("DCM","113901");		// "DLP Alert Value Configured"
			alertDLPValueConfigured = alertDLPValueConfiguredContentItem != null
								   && alertDLPValueConfiguredContentItem instanceof ContentItemFactory.CodeContentItem
								   && ((ContentItemFactory.CodeContentItem)alertDLPValueConfiguredContentItem).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("R-0038D","SRT");	// "Yes"

			ContentItem alertCTDIvolValueConfiguredContentItem = alertDetailsFragment.getNamedChild("DCM","113902");	// "CTDIvol Alert Value Configured"
			alertCTDIvolValueConfigured = alertCTDIvolValueConfiguredContentItem != null
								   && alertCTDIvolValueConfiguredContentItem instanceof ContentItemFactory.CodeContentItem
								   && ((ContentItemFactory.CodeContentItem)alertCTDIvolValueConfiguredContentItem).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("R-0038D","SRT");	// "Yes"

			alertDLPValue                          = alertDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113903");	// "DLP Alert Value"						... should really check units are mGy.cm :(
			alertCTDIvolValue                      = alertDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113904");	// "CTDIvol Alert Value"					... should really check units are mGy :(
			alertAccumulatedDLPForwardEstimate     = alertDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113905");	// "Accumulated DLP Forward Estimate"		... should really check units are mGy.cm :(
			alertAccumulatedCTDIvolForwardEstimate = alertDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113906");	// "Accumulated CTDIvol Forward Estimate"	... should really check units are mGy :(
			alertReasonForProceeding               = alertDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113907");	// "Reason for Proceeding"

			alertPerson = new PersonParticipant(alertDetailsFragment);
		}

		ContentItem notificationDetailsFragment = dose.getNamedChild("DCM","113908");	// "Dose Check Notification Details"
		if (notificationDetailsFragment != null) {
			ContentItem notificationDLPValueConfiguredContentItem = notificationDetailsFragment.getNamedChild("DCM","113909");		// "DLP Notification Value Configured"
			notificationDLPValueConfigured = notificationDLPValueConfiguredContentItem != null
										  && notificationDLPValueConfiguredContentItem instanceof ContentItemFactory.CodeContentItem
										  && ((ContentItemFactory.CodeContentItem)notificationDLPValueConfiguredContentItem).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("R-0038D","SRT");	// "Yes"

			ContentItem notificationCTDIvolValueConfiguredContentItem = notificationDetailsFragment.getNamedChild("DCM","113910");	// "CTDIvol Notification Value Configured"
			notificationCTDIvolValueConfigured = notificationCTDIvolValueConfiguredContentItem != null
											  && notificationCTDIvolValueConfiguredContentItem instanceof ContentItemFactory.CodeContentItem
											  && ((ContentItemFactory.CodeContentItem)notificationCTDIvolValueConfiguredContentItem).contentItemValueMatchesCodeValueAndCodingSchemeDesignator("R-0038D","SRT");	// "Yes"

			notificationDLPValue               = notificationDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113911");	// "DLP Notification Value"						... should really check units are mGy.cm :(
			notificationCTDIvolValue           = notificationDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113912");	// "CTDIvol Notification Value"					... should really check units are mGy :(
			notificationDLPForwardEstimate     = notificationDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113913");	// "DLP Forward Estimate"						... should really check units are mGy.cm :(
			notificationCTDIvolForwardEstimate = notificationDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113914");	// "CTDIvol Forward Estimate"					... should really check units are mGy :(
			notificationReasonForProceeding    = notificationDetailsFragment.getSingleStringValueOrNullOfNamedChild("DCM","113907");	// "Reason for Proceeding"

			notificationPerson = new PersonParticipant(notificationDetailsFragment);
		}
	}

	public boolean getAlertDLPValueConfigured() { return alertDLPValueConfigured; }
	public String  getAlertDLPValue() { return alertDLPValue; }
	public boolean getAlertCTDIvolValueConfigured() { return alertCTDIvolValueConfigured; }
	public String  getAlertCTDIvolValue() { return alertCTDIvolValue; }
	public String  getAlertAccumulatedDLPForwardEstimate() { return alertAccumulatedDLPForwardEstimate; }
	public String  getAlertAccumulatedCTDIvolForwardEstimate() { return alertAccumulatedCTDIvolForwardEstimate; }
	public String  getAlertReasonForProceeding() { return alertReasonForProceeding; }
	public PersonParticipant getAlertPerson() { return alertPerson; }
	
	public boolean getNotificationDLPValueConfigured() { return notificationDLPValueConfigured; }
	public String  getNotificationDLPValue() { return notificationDLPValue; }
	public boolean getNotificationCTDIvolValueConfigured() { return notificationCTDIvolValueConfigured; }
	public String  getNotificationCTDIvolValue() { return notificationCTDIvolValue; }
	public String  getNotificationDLPForwardEstimate() { return notificationDLPForwardEstimate; }
	public String  getNotificationCTDIvolForwardEstimate() { return notificationCTDIvolForwardEstimate; }
	public String  getNotificationReasonForProceeding() { return notificationReasonForProceeding; }
	public PersonParticipant getNotificationPerson() { return notificationPerson; }

	public void getStructuredReportFragment(ContentItem root) throws DicomException {
		ContentItemFactory cif = new ContentItemFactory();
		{
			ContentItem alertDetailsFragment = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113900","DCM","Dose Check Alert Details"),true/*continuityOfContentIsSeparate*/);
			
			cif.new CodeContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113901","DCM","DLP Alert Value Configured"),
				alertDLPValueConfigured ? new CodedSequenceItem("R-0038D","SRT","Yes") : new CodedSequenceItem("R-00339","SRT","No"));
			
			cif.new CodeContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113902","DCM","CTDIvol Alert Value Configured"),
				alertCTDIvolValueConfigured ? new CodedSequenceItem("R-0038D","SRT","Yes") : new CodedSequenceItem("R-00339","SRT","No"));
			
			if (/*alertDLPValueConfigured && */alertDLPValue != null && alertDLPValue.trim().length() > 0) {
				cif.new NumericContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113903","DCM","DLP Alert Value"),alertDLPValue,new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
			}
			
			if (/*alertCTDIvolValueConfigured && */alertCTDIvolValue != null && alertCTDIvolValue.trim().length() > 0) {
				cif.new NumericContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113904","DCM","CTDIvol Alert Value"),alertCTDIvolValue,new CodedSequenceItem("mGy","UCUM","1.8","mGy"));
			}
			
			if (alertAccumulatedDLPForwardEstimate != null && alertAccumulatedDLPForwardEstimate.trim().length() > 0) {
				cif.new NumericContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113905","DCM","Accumulated DLP Forward Estimate"),alertAccumulatedDLPForwardEstimate,new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
			}
			
			if (alertAccumulatedCTDIvolForwardEstimate != null && alertAccumulatedCTDIvolForwardEstimate.trim().length() > 0) {
				cif.new NumericContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113906","DCM","Accumulated CTDIvol Forward Estimate"),alertAccumulatedCTDIvolForwardEstimate,new CodedSequenceItem("mGy","UCUM","1.8","mGy"));
			}
			
			if (alertReasonForProceeding != null && alertReasonForProceeding.trim().length() > 0) {
				cif.new TextContentItem(alertDetailsFragment,"CONTAINS",new CodedSequenceItem("113907","DCM","Reason for Proceeding"),alertReasonForProceeding);
			}
			
			if (alertPerson != null) {
				alertDetailsFragment.addChild(alertPerson.getStructuredReportFragment());
			}
		}
		{
			ContentItem notificationDetailsFragment = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113908","DCM","Dose Check Notification Details"),true/*continuityOfContentIsSeparate*/);
			
			cif.new CodeContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113909","DCM","DLP Notification Value Configured"),
				notificationDLPValueConfigured ? new CodedSequenceItem("R-0038D","SRT","Yes") : new CodedSequenceItem("R-00339","SRT","No"));
			
			cif.new CodeContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113910","DCM","CTDIvol Notification Value Configured"),
				notificationCTDIvolValueConfigured ? new CodedSequenceItem("R-0038D","SRT","Yes") : new CodedSequenceItem("R-00339","SRT","No"));
			
			if (/*notificationDLPValueConfigured && */notificationDLPValue != null && notificationDLPValue.trim().length() > 0) {
				cif.new NumericContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113911","DCM","DLP Notification Value"),notificationDLPValue,new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
			}
			
			if (/*notificationCTDIvolValueConfigured && */notificationCTDIvolValue != null && notificationCTDIvolValue.trim().length() > 0) {
				cif.new NumericContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113912","DCM","CTDIvol Notification Value"),notificationCTDIvolValue,new CodedSequenceItem("mGy","UCUM","1.8","mGy"));
			}
			
			if (notificationDLPForwardEstimate != null && notificationDLPForwardEstimate.trim().length() > 0) {
				cif.new NumericContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113913","DCM","DLP Forward Estimate"),notificationDLPForwardEstimate,new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
			}
			
			if (notificationCTDIvolForwardEstimate != null && notificationCTDIvolForwardEstimate.trim().length() > 0) {
				cif.new NumericContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113914","DCM","CTDIvol Forward Estimate"),notificationCTDIvolForwardEstimate,new CodedSequenceItem("mGy","UCUM","1.8","mGy"));
			}
			
			if (notificationReasonForProceeding != null && notificationReasonForProceeding.trim().length() > 0) {
				cif.new TextContentItem(notificationDetailsFragment,"CONTAINS",new CodedSequenceItem("113907","DCM","Reason for Proceeding"),notificationReasonForProceeding);
			}
			
			if (notificationPerson != null) {
				notificationDetailsFragment.addChild(notificationPerson.getStructuredReportFragment());
			}
		}
	}
}


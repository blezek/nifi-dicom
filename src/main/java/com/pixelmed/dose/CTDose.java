/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.anatproc.CodedConcept;

import com.pixelmed.dicom.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

public class CTDose implements RadiationDoseStructuredReport, RadiationDoseStructuredReportFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/CTDose.java,v 1.40 2017/01/24 10:50:42 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CTDose.class);
	
	protected static double headToBodyDLPConversionFactor = 0.5d;
	
	protected SourceOfDoseInformation source;
	protected String dlpTotal;
	protected CTPhantomType dlpTotalPhantom;
	protected boolean prohibitDLPTotalPhantomSettingFromAcquisitions;
	protected SortedMap<CTPhantomType,String> dlpSubTotals;
	protected int totalNumberOfIrradiationEvents;
	protected ScopeOfDoseAccummulation scopeOfDoseAccummulation;
	protected String scopeUID;
	protected ArrayList<CTDoseAcquisition> acquisitions;
	protected CommonDoseObserverContext observerContext;
	protected CompositeInstanceContext compositeInstanceContext;
	protected String startDateTime;
	protected String endDateTime;
	protected String description;
	protected String sourceSOPInstanceUID;	// e.g., of the RDSR file, or the dose screen file
	protected CodedSequenceItem defaultAnatomy;
	
	protected StructuredReport sr;
	protected AttributeList list;
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr) throws DicomException {
		return new CTDose(sr);
	}
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr,AttributeList list) throws DicomException {
		return new CTDose(sr,list);
	}

	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(AttributeList list) throws DicomException {
		return new CTDose(list);
	}
	
	public CTDose(StructuredReport sr) throws DicomException {
		this.sr = sr;
		this.list = null;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}

	public CTDose(StructuredReport sr,AttributeList list) throws DicomException {
		this.sr = sr;
		this.list = list;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}
	
	public CTDose(AttributeList list) throws DicomException {
		this.list = list;
		this.sr = new StructuredReport(list);
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}
	
	public CTDose(String dlpTotal,int totalNumberOfIrradiationEvents,ScopeOfDoseAccummulation scopeOfDoseAccummulation,String scopeUID,String startDateTime,String endDateTime,String description) {
		this.source = SourceOfDoseInformation.AUTOMATED_DATA_COLLECTION;	// perhaps would be better to be null, but set as default in case not overridden, for legacy compatibility, since this was the only original choice :(
		this.observerContext = null;
		this.compositeInstanceContext = null;
		this.dlpTotal = dlpTotal;
		this.dlpTotalPhantom = null;
		this.dlpSubTotals = null;
		this.totalNumberOfIrradiationEvents = totalNumberOfIrradiationEvents;
		this.scopeOfDoseAccummulation = scopeOfDoseAccummulation;
		this.scopeUID = scopeUID;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.description = description;
	}
	
	public CTDose(String dlpSubTotalHead,String dlpSubTotalBody,int totalNumberOfIrradiationEvents,ScopeOfDoseAccummulation scopeOfDoseAccummulation,String scopeUID,String startDateTime,String endDateTime,String description) {
		this.source = SourceOfDoseInformation.AUTOMATED_DATA_COLLECTION;	// perhaps would be better to be null, but set as default in case not overridden, for legacy compatibility, since this was the only original choice :(
		this.observerContext = null;
		this.compositeInstanceContext = null;
		setDLPTotal(dlpSubTotalHead,dlpSubTotalBody);
		this.totalNumberOfIrradiationEvents = totalNumberOfIrradiationEvents;
		this.scopeOfDoseAccummulation = scopeOfDoseAccummulation;
		this.scopeUID = scopeUID;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.description = description;
	}
	
	public CTDose(ScopeOfDoseAccummulation scopeOfDoseAccummulation,String scopeUID,String startDateTime,String endDateTime,String description) {
		this.source = SourceOfDoseInformation.AUTOMATED_DATA_COLLECTION;	// perhaps would be better to be null, but set as default in case not overridden, for legacy compatibility, since this was the only original choice :(
		this.observerContext = null;
		this.compositeInstanceContext = null;
		this.dlpTotal = null;
		this.dlpTotalPhantom = null;
		this.prohibitDLPTotalPhantomSettingFromAcquisitions = false;
		this.dlpSubTotals = null;
		this.totalNumberOfIrradiationEvents = 0;
		this.scopeOfDoseAccummulation = scopeOfDoseAccummulation;
		this.scopeUID = scopeUID;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.description = description;
	}
	
	public void merge(CTDose ctDoseToMerge) {
		// primary goal is to fill in CTAcquisitionParameters if missing in each CTDoseAcquisition, or merge CTAcquisitionParameter values if missing
		if (ctDoseToMerge != null) {
			// should we clear the SR or AttributeList cache at this point ? what if read from SR or list instead of built de novo ? :(
			sr = null;
			list = null;
			int nAcqMerge = ctDoseToMerge.getNumberOfAcquisitions();
			if (nAcqMerge > 0) {
				int nAcqOurs = getNumberOfAcquisitions();
				if (nAcqOurs == 0) {
//System.err.println("CTDose.merge(): we have no acquisitions");
					// we have no acquisitions, so just copy their's
					for (int iAcqMerge=0; iAcqMerge<nAcqMerge; ++iAcqMerge) {
						CTDoseAcquisition acqMerge = ctDoseToMerge.getAcquisition(iAcqMerge);
						addAcquisition(acqMerge);	// do not just add to collection; this will also extract and set total phantom types etc.
					}
				}
				else {
//System.err.println("CTDose.merge(): we have acquisitions");
					// we have acquisitions, so have to step through them and merge them
					// note that the number of acquisitions may not be equal, since localizers may be missing from one or the other, etc.
					// also, we do not attempt to add any completely missing acquisitions, only match them and copy contents
					int iAcqOurs = 0;
					int iAcqMerge = 0;
					while (iAcqOurs < nAcqOurs && iAcqMerge < nAcqMerge) {
						CTDoseAcquisition acqOurs = this.getAcquisition(iAcqOurs);
						CTScanType scanTypeOurs = acqOurs.getScanType();
						if (scanTypeOurs == null || scanTypeOurs.equals(CTScanType.LOCALIZER)) {
							++iAcqOurs;
							continue;
						}
						CTDoseAcquisition acqMerge = ctDoseToMerge.getAcquisition(iAcqMerge);
						CTScanType scanTypeMerge = acqMerge.getScanType();
						if (scanTypeMerge == null || scanTypeMerge.equals(CTScanType.LOCALIZER)) {
							++iAcqMerge;
							continue;
						}
						if (acqOurs.matchForMerge(acqMerge)) {
//System.err.println("CTDose.merge(): have a match between "+acqOurs+" and "+acqMerge);
							acqOurs.merge(acqMerge);
							++iAcqOurs;
							++iAcqMerge;
						}
						else {
							// which to skip if don't match in sequence after have already skipped localizer ? don't know ...
//System.err.println("CTDose.merge(): cannot match "+acqOurs+" and "+acqMerge);
							break;
						}
					}
				}
			}
		}
	} 
	
	protected void parseSRContent() throws DicomException {
		prohibitDLPTotalPhantomSettingFromAcquisitions = false;
		if (sr != null) {
//System.err.println("CTDose.parseSRContent():");
			observerContext = new CommonDoseObserverContext();
			ContentItem root = (ContentItem)(sr.getRoot());
			if (root != null) {
				if (root instanceof ContentItemFactory.ContainerContentItem && root.getConceptNameCodingSchemeDesignator().equals("DCM") && root.getConceptNameCodeValue().equals("113701")) {	// "X-Ray Radiation Dose Report"
					// ignore (do not extract and record) DTID 1204 "Language of Content Item and Descendants" since only recognizing codes not meanings, and will always rewrite it as english anyway
					ContentItem procedureReported = root.getNamedChild("DCM","121058");
					if (procedureReported != null && procedureReported instanceof ContentItemFactory.CodeContentItem) {
						CodedSequenceItem procedureReportedCode = ((ContentItemFactory.CodeContentItem)procedureReported).getConceptCode();
						if (procedureReportedCode != null && procedureReportedCode.getCodingSchemeDesignator().equals("SRT") && procedureReportedCode.getCodeValue().equals("P5-08000")) {		// "Computed Tomography X-Ray"

							observerContext = new CommonDoseObserverContext();
							observerContext.setRecordingDeviceObserverContext(new RecordingDeviceObserverContext(root));
							
							startDateTime = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","113809");	// "Start of X-Ray Irradiation"
							endDateTime   = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","113810");	// "End of X-Ray Irradiation"
							
							ContentItem ctAccumulatedDoseData =  root.getNamedChild("DCM","113811");					// "CT Accumulated Dose Data"
							if (ctAccumulatedDoseData != null && ctAccumulatedDoseData instanceof ContentItemFactory.ContainerContentItem) {
//System.err.println("CTDose.parseSRContent(): CT Accumulated Dose Data parsing");
								ContentItem ctDoseLengthProductTotal =  ctAccumulatedDoseData.getNamedChild("DCM","113813");	// "CT Dose Length Product Total"
								if (ctDoseLengthProductTotal != null && ctDoseLengthProductTotal instanceof ContentItemFactory.NumericContentItem) {
//System.err.println("CTDose.parseSRContent(): CT Dose Length Product Total parsing");
									{
										CodedSequenceItem unit = ((ContentItemFactory.NumericContentItem)ctDoseLengthProductTotal).getUnits();
										if (CTDoseAcquisition.checkUnitIs_mGycm(unit)) {
//System.err.println("CTDose.parseSRContent(): CT Accumulated Dose Data DLP units are OK");
											dlpTotal = ((ContentItemFactory.NumericContentItem)ctDoseLengthProductTotal).getNumericValue();
											ContentItem pt = ctDoseLengthProductTotal.getNamedChild("DCM","113835");			// "CTDIw Phantom Type"
											if (pt != null && pt instanceof ContentItemFactory.CodeContentItem) {
												prohibitDLPTotalPhantomSettingFromAcquisitions = true;							// want to retain explicitly specified value
												dlpTotalPhantom = CTPhantomType.selectFromCode(((ContentItemFactory.CodeContentItem)pt).getConceptCode());
//System.err.println("CTDose.parseSRContent(): CT Accumulated Dose Data Total DLP phantom set to "+dlpTotalPhantom);
											}
										}
										else {
											slf4jlogger.warn("CT Accumulated Dose Data DLP units are not mGy.cm - ignoring value");		// do not throw exception, since want to parse rest of content
										}
									}
									{
										int n = ctDoseLengthProductTotal.getChildCount();
										for (int i=0; i<n; ++i) {
											ContentItem node = (ContentItem)(ctDoseLengthProductTotal.getChildAt(i));
											if (node != null && node instanceof ContentItemFactory.NumericContentItem) {
												if (node.getConceptNameCodingSchemeDesignator().equals("99PMP") && node.getConceptNameCodeValue().equals("220005")) {	// "CT Dose Length Product Sub-Total"
													CTPhantomType phantomType = null;
													ContentItem pt = node.getNamedChild("DCM","113835");					// "CTDIw Phantom Type"
													if (pt != null && pt instanceof ContentItemFactory.CodeContentItem) {
														phantomType = CTPhantomType.selectFromCode(((ContentItemFactory.CodeContentItem)pt).getConceptCode());
													}
													if (phantomType != null) {
														CodedSequenceItem unit = ((ContentItemFactory.NumericContentItem)node).getUnits();
														if (CTDoseAcquisition.checkUnitIs_mGycm(unit)) {
															String subTotal = ((ContentItemFactory.NumericContentItem)node).getNumericValue();
															if (subTotal != null && subTotal.length() > 0) {
																if (dlpSubTotals == null) {
																	dlpSubTotals = new TreeMap<CTPhantomType,String>();
																}
																dlpSubTotals.put(phantomType,subTotal);
															}
														}
														else {
															slf4jlogger.warn("CT Accumulated Dose Data DLP Sub-Total units are not mGy.cm - ignoring value");		// do not throw exception, since want to parse rest of content
														}
													}
													else {
														slf4jlogger.warn("CT Accumulated Dose Data DLP Sub-Total has no phantom type specified - ignoring value");		// do not throw exception, since want to parse rest of content
													}
												}
											}
										}
									}
								}
								else {
									slf4jlogger.warn("CT Accumulated Dose Data DLP not found");		// do not throw exception, since want to parse rest of content
								}
							}
							else {
								throw new DicomException("SR does not contain CT Accumulated Dose Data");
							}

							ContentItem soa = root.getNamedChild("DCM","113705");	// "Scope of Accumulation"
							if (soa != null && soa instanceof ContentItemFactory.CodeContentItem) {
								scopeOfDoseAccummulation = ScopeOfDoseAccummulation.selectFromCode(((ContentItemFactory.CodeContentItem)soa).getConceptCode());
								if (scopeOfDoseAccummulation != null) {
									CodedSequenceItem uidConcept = scopeOfDoseAccummulation.getCodedSequenceItemForUIDConcept();
									ContentItem uidItem = soa.getNamedChild(uidConcept);
									if (uidItem != null && uidItem instanceof ContentItemFactory.UIDContentItem) {
										scopeUID = ((ContentItemFactory.UIDContentItem)uidItem).getConceptValue();
									}
								}
							}
							
							source = SourceOfDoseInformation.getSourceOfDoseInformation(root);
							
							{
								PersonParticipant pauth = new PersonParticipant(root);
								if (pauth != null && pauth.getRoleInProcedure() == RoleInProcedure.IRRADIATION_AUTHORIZING) {
									observerContext.setPersonParticipantAuthorizing(pauth);
								}
							}
							
							{
								int n = root.getChildCount();
								for (int i=0; i<n; ++i) {
									ContentItem node = (ContentItem)(root.getChildAt(i));
									if (node != null && node.getConceptNameCodingSchemeDesignator().equals("DCM") && node.getConceptNameCodeValue().equals("113819")) {	// "CT Acquisition"
										addAcquisition(new CTDoseAcquisition(scopeUID,node));
										
										// NB. the following will overwrite any previous context each time (i.e., we assume that they are all the same for every acquisition in our model) :(
										
										DeviceParticipant dp = new DeviceParticipant(node);
										observerContext.setDeviceParticipant(dp);

										PersonParticipant padmin = new PersonParticipant(node);
										if (padmin != null && padmin.getRoleInProcedure() == RoleInProcedure.IRRADIATION_ADMINISTERING) {
											observerContext.setPersonParticipantAdministering(padmin);
										}
									}
								}
							}
						}
						else {
							throw new DicomException("SR procedure reported is not CT");
						}
					}
					else {
						throw new DicomException("SR procedure reported is missing or not correctly encoded");
					}
				}
				else {
					throw new DicomException("SR document title is not X-Ray Radiation Dose Report");
				}
			}
			else {
				throw new DicomException("No SR root node");
			}
			if (list == null) {
				getAttributeList();
			}
			if (list != null) {
				description = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
				sourceSOPInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
			}
		}
		else {
			throw new DicomException("No SR content");
		}
	}
	
	public void addAcquisition(CTDoseAcquisition acquisition) {
		acquisitions.add(acquisition);
		if (!prohibitDLPTotalPhantomSettingFromAcquisitions) {
			CTPhantomType phantom = acquisition.getPhantomType();
			if (phantom != null) {
				if (dlpTotalPhantom == null) {
					dlpTotalPhantom=phantom;
				}
				else if (!dlpTotalPhantom.equals(phantom)) {
					dlpTotalPhantom = CTPhantomType.MIXED;			// this will get overridden for return value of getDLPTotalPhantomToUse()
				}
				// else same so OK
			}
		}
	}
	
	public CommonDoseObserverContext getObserverContext() { return observerContext; }
	
	public void setObserverContext(CommonDoseObserverContext observerContext) { this.observerContext = observerContext; }
	
	public CompositeInstanceContext getCompositeInstanceContext() { return compositeInstanceContext; }
	
	public void setCompositeInstanceContext(CompositeInstanceContext compositeInstanceContext) { this.compositeInstanceContext = compositeInstanceContext; }
	
	public void setSourceOfDoseInformation(SourceOfDoseInformation source) { this.source = source; }

	public SourceOfDoseInformation getSourceOfDoseInformation() { return source; }

	public String getDLPTotal() { return dlpTotal; }
	
	public void setDLPTotal(String dlpTotal) { this.dlpTotal = dlpTotal; }

	public void setDLPTotal(String dlpSubTotalHead,String dlpSubTotalBody) {
		if (dlpSubTotals == null) {
			dlpSubTotals = new TreeMap<CTPhantomType,String>();
		}
		dlpSubTotals.put(CTPhantomType.HEAD16,dlpSubTotalHead);
		dlpSubTotals.put(CTPhantomType.BODY32,dlpSubTotalBody);
		this.dlpTotal = getDLPTotalCombinedFromHeadAndBodyPhantomValues();
		this.dlpTotalPhantom = CTPhantomType.BODY32;
		this.prohibitDLPTotalPhantomSettingFromAcquisitions = true;
	}
	
	public String getDLPTotalToUse() {
		return dlpTotal == null ? getDLPTotalFromAcquisitions() : dlpTotal;
	}
	
	public CTPhantomType getDLPTotalPhantom() { return dlpTotalPhantom; }
	
	public void setDLPTotalPhantom(CTPhantomType dlpTotalPhantom) { this.dlpTotalPhantom = dlpTotalPhantom; }
	
	public CTPhantomType getDLPTotalPhantomToUse() {
		// return CTPhantomType.BODY32 if CTPhantomType.MIXED because getDLPTotalFromAcquisitions() uses getDLPTotalCombinedFromHeadAndBodyPhantomValues() ...
		return dlpTotalPhantom == null ? null : (dlpTotalPhantom.equals(CTPhantomType.MIXED) ? (dlpTotal == null ? CTPhantomType.BODY32 : null) : dlpTotalPhantom);
	}
	
	public String getDLPTotalPhantomDescriptionToUse() {
		CTPhantomType phantom = getDLPTotalPhantomToUse();
		return phantom == null ? "" : phantom.toString();
	}

	public String getDLPSubTotalHead() { return dlpSubTotals == null ? null : dlpSubTotals.get(CTPhantomType.HEAD16); }

	public String getDLPSubTotalBody() { return dlpSubTotals == null ? null : dlpSubTotals.get(CTPhantomType.BODY32); }

	public int getTotalNumberOfIrradiationEvents() { return totalNumberOfIrradiationEvents ==  0 ? acquisitions.size() : totalNumberOfIrradiationEvents; }
	
	public ScopeOfDoseAccummulation getScopeOfDoseAccummulation() { return scopeOfDoseAccummulation; }
	
	public String getScopeUID() { return scopeUID; }
	
	public int getNumberOfAcquisitions() { return acquisitions.size(); }
	
	public CTDoseAcquisition getAcquisition(int i) { return acquisitions.get(i); }
	
	static public double getDLPTotalCombinedFromHeadAndBodyPhantomValues(double dlpSubTotalHead,double dlpSubTotalBody) {
		return dlpSubTotalHead * headToBodyDLPConversionFactor + dlpSubTotalBody;
	}
	
	static public String getDLPTotalCombinedFromHeadAndBodyPhantomValues(String dlpSubTotalHead,String dlpSubTotalBody) {
		String dlpTotal = null;
		try {
			double dDLPSubTotalHead = dlpSubTotalHead == null ? 0d : Double.parseDouble(dlpSubTotalHead);
			double dDLPSubTotalBody = dlpSubTotalBody == null ? 0d : Double.parseDouble(dlpSubTotalBody);
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
			formatter.setMaximumFractionDigits(2);
			formatter.setMinimumFractionDigits(2);
			formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
			formatter.setGroupingUsed(false);					// i.e., no comma at thousands
			dlpTotal = formatter.format(getDLPTotalCombinedFromHeadAndBodyPhantomValues(dDLPSubTotalHead,dDLPSubTotalBody));
		}
		catch (NumberFormatException e) {
			slf4jlogger.error("",e);
		}
		return dlpTotal;
	}
	
	public String getDLPTotalCombinedFromHeadAndBodyPhantomValues() {
		return getDLPTotalCombinedFromHeadAndBodyPhantomValues(getDLPSubTotalHead(),getDLPSubTotalBody());
	}
	
	public String getDLPTotalFromAcquisitions() throws NumberFormatException {
		double dlpTotalFromAcquisitions = 0;
		double dlpSubTotalHeadFromAcquisitions = 0;
		double dlpSubTotalBodyFromAcquisitions = 0;
		double dlpSubTotalUnspecifiedFromAcquisitions = 0;
		CTPhantomType commonPhantom = null;
		for (CTDoseAcquisition a : acquisitions) {
			if (a != null) {
				String aDLP = a.getDLP();
				if (aDLP != null && aDLP.length() > 0) {	// check for zero length else NumberFormatException
					try {
						double dlp = Double.parseDouble(aDLP);
						CTPhantomType phantom = a.getPhantomType();
						slf4jlogger.debug("getDLPTotalFromAcquisitions(): acquisition phantom {} DLP {}",phantom,dlp);
						if (phantom == null) {
							dlpSubTotalUnspecifiedFromAcquisitions += dlp;
							slf4jlogger.debug("getDLPTotalFromAcquisitions(): added DLP {} to dlpSubTotalUnspecifiedFromAcquisitions, now {}",dlp,dlpSubTotalUnspecifiedFromAcquisitions);
						}
						else {
							if (commonPhantom == null) {
								commonPhantom = phantom;
								slf4jlogger.debug("getDLPTotalFromAcquisitions(): first time added DLP {} to dlpTotalFromAcquisitions, now {}",dlp,dlpTotalFromAcquisitions);
								dlpTotalFromAcquisitions += dlp;
							}
							else if (commonPhantom.equals(phantom)) {
								dlpTotalFromAcquisitions += dlp;
								slf4jlogger.debug("getDLPTotalFromAcquisitions(): same phantom added DLP {} to dlpTotalFromAcquisitions, now {}",dlp,dlpTotalFromAcquisitions);
							}
							else {
								commonPhantom = CTPhantomType.MIXED;
								dlpTotalFromAcquisitions = 0;
								slf4jlogger.debug("getDLPTotalFromAcquisitions(): mixed phantom set dlpTotalFromAcquisitions to zero");
							}
							
							if (phantom.equals(CTPhantomType.HEAD16)) {
								dlpSubTotalHeadFromAcquisitions += dlp;
								slf4jlogger.debug("getDLPTotalFromAcquisitions(): added DLP {} to dlpSubTotalHeadFromAcquisitions, now {}",dlp,dlpSubTotalHeadFromAcquisitions);
							}
							else if (phantom.equals(CTPhantomType.BODY32)) {
								dlpSubTotalBodyFromAcquisitions += dlp;
								slf4jlogger.debug("getDLPTotalFromAcquisitions(): added DLP {} to dlpSubTotalBodyFromAcquisitions, now {}",dlp,dlpSubTotalBodyFromAcquisitions);
							}
						}
						
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}
			}
		}
		if (commonPhantom == null) {
			dlpTotalFromAcquisitions = dlpSubTotalUnspecifiedFromAcquisitions;
			slf4jlogger.debug("getDLPTotalFromAcquisitions(): no phantom using dlpSubTotalUnspecifiedFromAcquisitions as dlpTotalFromAcquisitions {}",dlpTotalFromAcquisitions);
		}
		else if (commonPhantom.equals(CTPhantomType.MIXED) && dlpTotalFromAcquisitions == 0 && dlpSubTotalHeadFromAcquisitions > 0 && dlpSubTotalBodyFromAcquisitions > 0) {
			slf4jlogger.debug("getDLPTotalFromAcquisitions(): mixed phantom using dlpSubTotalHeadFromAcquisitions {}",dlpSubTotalHeadFromAcquisitions);
			slf4jlogger.debug("getDLPTotalFromAcquisitions(): mixed phantom using dlpSubTotalBodyFromAcquisitions {}",dlpSubTotalBodyFromAcquisitions);
			dlpTotalFromAcquisitions = getDLPTotalCombinedFromHeadAndBodyPhantomValues(dlpSubTotalHeadFromAcquisitions,dlpSubTotalBodyFromAcquisitions);
			slf4jlogger.debug("getDLPTotalFromAcquisitions(): mixed phantom using calculated combined {}",dlpTotalFromAcquisitions);
		}
		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
		formatter.setGroupingUsed(false);					// i.e., no comma at thousands
		String formatted = formatter.format(dlpTotalFromAcquisitions);
		slf4jlogger.debug("getDLPTotalFromAcquisitions(): returns formatted string {} for {}",formatted,Double.toString(dlpTotalFromAcquisitions));
		return formatted;
	}
	
	public boolean specifiedDLPTotalMatchesDLPTotalFromAcquisitions() {
		return (dlpTotal != null && dlpTotal.equals(getDLPTotalFromAcquisitions())) || (dlpTotal == null && getNumberOfAcquisitions() == 0);	// could check "0.00".equals()
	}
	
	public String getStartDateTime() { return startDateTime; }

	public String getEndDateTime() { return endDateTime; }

	public String getDescription() { return description; }
	
	public String getSourceSOPInstanceUID() { return sourceSOPInstanceUID; }
	
	public void setSourceSOPInstanceUID(String sourceSOPInstanceUID) { this.sourceSOPInstanceUID = sourceSOPInstanceUID; }
	
	public CodedSequenceItem getDefaultAnatomy() { return defaultAnatomy; }
	
	public void setDefaultAnatomy(CodedSequenceItem defaultAnatomy) { this.defaultAnatomy = defaultAnatomy; }
	
	public void setDefaultAnatomy(CodedConcept defaultAnatomyConcept) {
		this.defaultAnatomy = null;
		if (defaultAnatomyConcept != null) {
			try {
				this.defaultAnatomy = defaultAnatomyConcept.getCodedSequenceItem();
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	public String getDescriptionOfWhereThisObjectCameFrom() {
		StringBuffer buffer = new StringBuffer();
		if (sr != null) {
			buffer.append("RDSR");
		}
		if (source != null) {
			if (buffer.length() > 0) {
			 buffer.append(" ");
			}
			buffer.append(source.toStringAbbreviation());
		}
		return buffer.toString();
	}

	public String toString() {
		return toString(true,false);
	}
	
	public String toString(boolean detail,boolean pretty) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Dose");
		{
			String patientID = "";
			String patientName = "";
			String patientSex = "";
			String patientBirthDate = "";
			String patientAge = "";
			String patientWeight = "";
			String patientSize= "";		// height
			String accessionNumber = "";
			if (compositeInstanceContext != null) {
				AttributeList contextList =  compositeInstanceContext.getAttributeList();
				patientID = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientID);
				patientName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientName);
				patientSex = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSex);
				patientBirthDate = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientBirthDate);
				patientAge = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientAge);
				patientWeight = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientWeight);
				patientSize = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSize);
				accessionNumber = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.AccessionNumber);
			}
			buffer.append("\t");
			buffer.append("Patient ID=");
			buffer.append(patientID);
			buffer.append("\tName=");
			buffer.append(patientName);
			buffer.append("\tSex=");
			buffer.append(patientSex);
			buffer.append("\tDOB=");
			buffer.append(patientBirthDate);
			buffer.append("\tAge=");
			buffer.append(patientAge);
			buffer.append("\tWeight=");
			buffer.append(patientWeight);
			buffer.append(" kg");
			buffer.append("\tHeight=");
			buffer.append(patientSize);
			buffer.append(" m");

			buffer.append("\tAccession=");
			buffer.append(accessionNumber);
		}
		if (detail || startDateTime != null) {
			buffer.append("\t");
			if (!pretty) {
				buffer.append("Start=");
			}
			if (pretty && startDateTime != null && startDateTime.length() > 0) {
				try {
					java.util.Date dateTime = new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(startDateTime);
					String formattedDate = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(dateTime);
					buffer.append(formattedDate);
				}
				catch (java.text.ParseException e) {
					slf4jlogger.error("",e);
				}
			}
			else {
				buffer.append(startDateTime);
			}
		}
		if (detail && !pretty) {
			buffer.append("\tEnd=");
			buffer.append(endDateTime);
		}
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Modality=");
		}
		buffer.append("CT");
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Description=");
		}
		buffer.append(description);
		if (detail && !pretty) {
			buffer.append("\tScope=");
			buffer.append(scopeOfDoseAccummulation);
		}
		if (detail && !pretty) {
			buffer.append("\tUID=");
			buffer.append(scopeUID);
		}
		if (detail && !pretty) {
			buffer.append("\tEvents=");
			buffer.append(Integer.toString(getTotalNumberOfIrradiationEvents()));
		}
		buffer.append("\tDLP Total=");
		buffer.append(getDLPTotalToUse());
		if (dlpTotalPhantom != null) {
			buffer.append(" (");
			buffer.append(getDLPTotalPhantomDescriptionToUse());
			buffer.append(")");
		}
		String dlpSubTotalHead = getDLPSubTotalHead();
		if (dlpSubTotalHead != null) {
			buffer.append(" (");
			buffer.append(CTPhantomType.HEAD16.toString());
			buffer.append(" ");
			buffer.append(dlpSubTotalHead);
			buffer.append(")");
		}
		String dlpSubTotalBody = getDLPSubTotalBody();
		if (dlpSubTotalBody != null) {
			buffer.append(" (");
			buffer.append(CTPhantomType.BODY32.toString());
			buffer.append(" ");
			buffer.append(dlpSubTotalBody);
			buffer.append(")");
		}
		buffer.append(" mGy.cm");

		buffer.append("\n");

		if (detail) {
			for (int i=0; i<acquisitions.size(); ++i) {
				buffer.append(acquisitions.get(i).toString(pretty));
			}
		}
		return buffer.toString();
	}

	public static String getHTMLTableHeaderRow() {
		return	 "<tr>"
				+"<th>ID</th>"
				+"<th>Name</th>"
				+"<th>Sex</th>"
				+"<th>DOB</th>"
				+"<th>Age</th>"
				+"<th>Weight kg</th>"
				+"<th>Height m</th>"
				+"<th>Accession</th>"
				+"<th>Date</th>"
				+"<th>Modality</th>"
				+"<th>Description</th>"
				+"<th>DLP Total mGy.cm</th>"
				+"<th>DLP "+CTPhantomType.HEAD16.toString()+" mGy.cm</th>"
				+"<th>DLP "+CTPhantomType.BODY32.toString()+" mGy.cm</th>"
				+"<th>Manufacturer</th>"
				+"<th>Model</th>"
				+"<th>Station</th>"
				+"<th>From</th>"
				+"</tr>\n";
	}

	public String getHTMLTableRow(boolean detail) {
		StringBuffer buffer = new StringBuffer();
		if (detail) {
			buffer.append(getHTMLTableHeaderRow());
		}
		buffer.append("<tr>");
		{
			String patientID = "";
			String patientName = "";
			String patientSex = "";
			String patientBirthDate = "";
			String patientAge = "";
			String patientWeight = "";
			String patientSize= "";		// height
			String accessionNumber = "";
			if (compositeInstanceContext != null) {
				AttributeList contextList =  compositeInstanceContext.getAttributeList();
				patientID = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientID);
				patientName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientName);
				patientSex = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSex);
				patientBirthDate = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientBirthDate);
				patientAge = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientAge);
				patientWeight = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientWeight);
				patientSize = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSize);
				accessionNumber = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.AccessionNumber);
			}
			buffer.append("<td>");
			buffer.append(patientID);
			buffer.append("</td><td>");
			buffer.append(patientName);
			buffer.append("</td><td>");
			buffer.append(patientSex);
			buffer.append("</td><td>");
			buffer.append(patientBirthDate);
			buffer.append("</td><td align=right>");
			buffer.append(patientAge);
			buffer.append("</td><td align=right>");
			buffer.append(patientWeight);
			//buffer.append(" kg");
			buffer.append("</td><td align=right>");
			buffer.append(patientSize);
			//buffer.append(" m");
			buffer.append("</td><td>");
			buffer.append(accessionNumber);
			buffer.append("</td>");
		}
		{
			buffer.append("<td>");
			String formattedDate = "";
			int startDateTimeLength = startDateTime.length();
			if (startDateTime != null && startDateTimeLength > 0) {
				try {
					String parseFormatString = "yyyyMMddHHmmss";
					if (startDateTimeLength < parseFormatString.length()) {
						parseFormatString = parseFormatString.substring(0,startDateTimeLength);		// avoids java.text.ParseException: Unparseable date, e.g., if just yyyyMMdd when time missing
					}
					java.util.Date dateTime = new java.text.SimpleDateFormat(parseFormatString).parse(startDateTime);
					formattedDate = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(dateTime);
				}
				catch (java.text.ParseException e) {
					slf4jlogger.error("",e);
				}
			}
			buffer.append(formattedDate);
			buffer.append("</td>");
		}
		buffer.append("<td>CT</td>");

		buffer.append("<td>");
		buffer.append(description);
		buffer.append("</td>");

		buffer.append("<td align=right>");
		buffer.append(getDLPTotalToUse());
		if (dlpTotalPhantom != null) {
			buffer.append(" (");
			buffer.append(getDLPTotalPhantomDescriptionToUse());
			buffer.append(")");
		}
		buffer.append("</td>");

		{
			String dlpSubTotalHead = getDLPSubTotalHead();
			buffer.append("<td align=right>");
			buffer.append(dlpSubTotalHead == null ? "" : dlpSubTotalHead);
			buffer.append("</td>");
		}
		
		{
			String dlpSubTotalBody = getDLPSubTotalBody();
			buffer.append("<td align=right>");
			buffer.append(dlpSubTotalBody == null ? "" : dlpSubTotalBody);
			buffer.append("</td>");
		}
		
		{
			String manufacturer = "";
			String manufacturerModelName = "";
			String stationName = "";
			if (observerContext != null) {
				{
					DeviceParticipant dp = observerContext.getDeviceParticipant();
					if (dp != null) {
						manufacturer = dp.getManufacturer();
						if (manufacturer == null) {
							manufacturer = "";
						}
						manufacturerModelName = dp.getModelName();
						if (manufacturerModelName == null) {
							manufacturerModelName = "";
						}
					}
				}
				{
					RecordingDeviceObserverContext rdoc = observerContext.getRecordingDeviceObserverContext();
					if (rdoc != null) {
						stationName = rdoc.getName();
						if (stationName == null) {
							stationName = "";
						}
					}
				}
			}
			if (compositeInstanceContext != null) {
				AttributeList contextList =  compositeInstanceContext.getAttributeList();
				if (manufacturer.length() == 0) {
					manufacturer = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.Manufacturer);
				}
				if (manufacturerModelName.length() == 0) {
					manufacturerModelName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.ManufacturerModelName);
				}
				if (stationName.length() == 0) {
					stationName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.StationName);
				}
			}

			buffer.append("<td>");
			buffer.append(manufacturer);
			buffer.append("</td>");

			buffer.append("<td>");
			buffer.append(manufacturerModelName);
			buffer.append("</td>");

			buffer.append("<td>");
			buffer.append(stationName);
			buffer.append("</td>");
		}

		buffer.append("<td>");
		buffer.append(getDescriptionOfWhereThisObjectCameFrom());
		buffer.append("</td>");

		buffer.append("</tr>\n");

		if (detail && acquisitions != null && acquisitions.size() > 0) {
			buffer.append("<tr><td colspan=2></td><td colspan=15><table>");
			String header = null;
			for (int i=0; i<acquisitions.size(); ++i) {
				if (header == null) {
					header = acquisitions.get(i).getHTMLTableHeaderRow();	// regardless
				}
				else if (acquisitions.get(i).getAcquisitionParameters() != null) {
					header = acquisitions.get(i).getHTMLTableHeaderRow();	// use the longer form that includes the parameter, and can stop looking
					break;
				}
			}
			buffer.append(header);
			for (int i=0; i<acquisitions.size(); ++i) {
				buffer.append(acquisitions.get(i).getHTMLTableRow());
			}
			buffer.append("</table></td></tr>\n");
		}

		return buffer.toString();
	}

	public StructuredReport getStructuredReport() throws DicomException {
		return getStructuredReport(false);		// default behavior is to reuse any SR as cached (e.g., as supplied in constructor)
	}

	public StructuredReport getStructuredReport(boolean rebuild) throws DicomException {
		if (rebuild) {
			sr = null;
		}
		if (sr == null) {
			ContentItemFactory cif = new ContentItemFactory();
			ContentItem root = cif.new ContainerContentItem(
				null/*no parent since root*/,null/*no relationshipType since root*/,
				new CodedSequenceItem("113701","DCM","X-Ray Radiation Dose Report"),
				true/*continuityOfContentIsSeparate*/,
				"DCMR","10011");
			cif.new CodeContentItem(root,"HAS CONCEPT MOD",new CodedSequenceItem("121049","DCM","Language of Content Item and Descendants"),new CodedSequenceItem("en","RFC5646","English"));
			ContentItem procedureReported = cif.new CodeContentItem(root,"HAS CONCEPT MOD",new CodedSequenceItem("121058","DCM","Procedure reported"),new CodedSequenceItem("P5-08000","SRT","Computed Tomography X-Ray"));
			cif.new CodeContentItem(procedureReported,"HAS CONCEPT MOD",new CodedSequenceItem("G-C0E8","SRT","Has Intent"),new CodedSequenceItem("R-408C3","SRT","Diagnostic Intent"));
		
			if (observerContext != null) {
				Map<RecordingDeviceObserverContext.Key,ContentItem> cimap = observerContext.getRecordingDeviceObserverContext().getStructuredReportFragment();
				Iterator<RecordingDeviceObserverContext.Key> i = cimap.keySet().iterator();
				while (i.hasNext()) {
					root.addChild(cimap.get(i.next()));
				}
			}
		
			if (startDateTime != null && startDateTime.trim().length() > 0) {
				cif.new DateTimeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113809","DCM","Start of X-Ray Irradiation"),startDateTime);
			}
			if (endDateTime != null && endDateTime.trim().length() > 0) {
				cif.new DateTimeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113810","DCM","End of X-Ray Irradiation"),endDateTime);
			}
		
			ContentItem ctAccumulatedDoseData = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113811","DCM","CT Accumulated Dose Data"),true,"DCMR","10012");
			cif.new NumericContentItem(ctAccumulatedDoseData,"CONTAINS",new CodedSequenceItem("113812","DCM","Total Number of Irradiation Events"),Integer.toString(acquisitions.size()),new CodedSequenceItem("{events}","UCUM","1.8","events"));
			{
				ContentItem dlptoti = cif.new NumericContentItem(ctAccumulatedDoseData,"CONTAINS",new CodedSequenceItem("113813","DCM","CT Dose Length Product Total"),
					getDLPTotalToUse(),
					new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
				CTPhantomType useDLPTotalPhantom = getDLPTotalPhantomToUse();
//System.err.println("CTDose.getStructuredReport(): CT Accumulated Dose Data Total DLP phantom to use is "+useDLPTotalPhantom+" (dlpTotalPhantom="+dlpTotalPhantom+")");
				if (useDLPTotalPhantom != null) {
					cif.new CodeContentItem(dlptoti,"HAS PROPERTIES",new CodedSequenceItem("113835","DCM","CTDIw Phantom Type"),useDLPTotalPhantom.getCodedSequenceItem());
				}
				if (dlpSubTotals != null) {
					for (CTPhantomType phantom : dlpSubTotals.keySet()) {
						String subTotal = dlpSubTotals.get(phantom);
						if (subTotal != null && subTotal.length() > 0) {
							ContentItem dlpsubtoti = cif.new NumericContentItem(dlptoti,"HAS PROPERTIES",new CodedSequenceItem("220005","99PMP","CT Dose Length Product Sub-Total"),
								subTotal,
								new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
							cif.new CodeContentItem(dlpsubtoti,"HAS PROPERTIES",new CodedSequenceItem("113835","DCM","CTDIw Phantom Type"),phantom.getCodedSequenceItem());
						}
					}
				}
			}
			for (CTDoseAcquisition a : acquisitions) {
				if (a != null) {
					ContentItem aci = a.getStructuredReportFragment(root,defaultAnatomy);
					if (aci != null && observerContext != null) {
						DeviceParticipant dp = observerContext.getDeviceParticipant();
						if (dp != null) {
							aci.addChild(dp.getStructuredReportFragment());
						}
						PersonParticipant padmin = observerContext.getPersonParticipantAdministering();
						if (padmin != null) {
							aci.addChild(padmin.getStructuredReportFragment());
						}
					}
				}
			}

			ContentItem scope = cif.new CodeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113705","DCM","Scope of Accumulation"),scopeOfDoseAccummulation.getCodedSequenceItemForScopeConcept());
			cif.new UIDContentItem(scope,"HAS PROPERTIES",scopeOfDoseAccummulation.getCodedSequenceItemForUIDConcept(),scopeUID);
		
			if (source != null) {
				cif.new CodeContentItem(root,"CONTAINS",new CodedSequenceItem("113854","DCM","Source of Dose Information"),source.getCodedSequenceItem());
			}
			
			if (observerContext != null) {
				PersonParticipant pauth = observerContext.getPersonParticipantAuthorizing();
				if (pauth != null) {
					root.addChild(pauth.getStructuredReportFragment());
				}
			}

			sr = new StructuredReport(root);
			list = null;	// any list previously populated is invalidated by newly generated SR tree; fluche cached version
		}
//System.err.println("CTDose.getStructuredReport():  sr =\n"+sr);
		return sr;
	}
	
	public AttributeList getAttributeList() throws DicomException {
//System.err.println("CTDose.getAttributeList(): compositeInstanceContext.getAttributeList() =\n"+(compositeInstanceContext == null ? "" : compositeInstanceContext.getAttributeList()));
		if (list == null) {
			getStructuredReport();
			list = sr.getAttributeList();
			if (compositeInstanceContext != null) {
//System.err.println("CTDose.getAttributeList(): compositeInstanceContext.getAttributeList() =\n"+compositeInstanceContext.getAttributeList());
				list.putAll(compositeInstanceContext.getAttributeList());
				// in case General Equipment values in "header" not set, get them from irradiating device in observer context if possible ...
				if (observerContext != null) {
					DeviceParticipant dp = observerContext.getDeviceParticipant();
					if (dp != null) {
						Attribute aManufacturer = list.get(TagFromName.Manufacturer);
						if (aManufacturer == null || aManufacturer.getVL() == 0) {
							String vManufacturer = dp.getManufacturer();
							if (vManufacturer != null && vManufacturer.length() > 0) {
								aManufacturer = new LongStringAttribute(TagFromName.Manufacturer);
								aManufacturer.addValue(vManufacturer);
								list.put(aManufacturer);
							}
						}
						Attribute aManufacturerModelName = list.get(TagFromName.ManufacturerModelName);
						if (aManufacturerModelName == null || aManufacturerModelName.getVL() == 0) {
							String vManufacturerModelName = dp.getModelName();
							if (vManufacturerModelName != null && vManufacturerModelName.length() > 0) {
								aManufacturerModelName = new LongStringAttribute(TagFromName.ManufacturerModelName);
								aManufacturerModelName.addValue(vManufacturerModelName);
								list.put(aManufacturerModelName);
							}
						}
						Attribute aDeviceSerialNumber = list.get(TagFromName.DeviceSerialNumber);
						if (aDeviceSerialNumber == null || aDeviceSerialNumber.getVL() == 0) {
							String vDeviceSerialNumber = dp.getSerialNumber();
							if (vDeviceSerialNumber != null && vDeviceSerialNumber.length() > 0) {
								aDeviceSerialNumber = new LongStringAttribute(TagFromName.DeviceSerialNumber);
								aDeviceSerialNumber.addValue(vDeviceSerialNumber);
								list.put(aDeviceSerialNumber);
							}
						}
					}
				}
			}
			if (description != null && list.get(TagFromName.StudyDescription) == null) {
				Attribute a = new LongStringAttribute(TagFromName.StudyDescription); a.addValue(description); list.put(a);
			}
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.XRayRadiationDoseSRStorage); list.put(a); }
			{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue("SR"); list.put(a); }
		}

//System.err.println("CTDose.getStructuredReport(): AttributeList =\n"+list);
		return list;
	}
	
	public void write(String filename,String aet,String manufacturerModelName) throws DicomException, IOException {
		String useAET = aet == null ? "OURAETITLE" : aet;
		getAttributeList();
		{
			java.util.Date currentDateTime = new java.util.Date();
			{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate); a.addValue(new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime)); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime); a.addValue(new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime)); list.put(a); }
			
			{
				java.util.TimeZone currentTz = java.util.TimeZone.getDefault();
				String currentTzInDICOMFormat = DateTimeAttribute.getTimeZone(currentTz,currentDateTime);	// use this rather than DateTimeAttribute.getCurrentTimeZone() because already have currentDateTime and currentTz
				String timezoneOffsetFromUTC = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TimezoneOffsetFromUTC);
				if (timezoneOffsetFromUTC.length() > 0) {
					if (!currentTzInDICOMFormat.equals(timezoneOffsetFromUTC)) {	// easier to compare DICOM strings than figure out offsets vs. raw offsets etc. from java.util.TimeZone
						// different timezone now than in images :(
						// need to fix up any existing dates and times :(
						slf4jlogger.warn("write(): TimezoneOffsetFromUTC from images {} is different from current timezone {} - removing and not adding current",timezoneOffsetFromUTC,currentTzInDICOMFormat);
						list.remove(TagFromName.TimezoneOffsetFromUTC);
					}
					// else good to go ... already in list and already correct (same for source images and our new instance)
				}
				else  {
//System.err.println("CTDose.write(): adding TimezoneOffsetFromUTC "+currentTzInDICOMFormat);
					{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC); a.addValue(currentTzInDICOMFormat); list.put(a); }
				}
			}

			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }
			
		}
		ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
			true,																						// replace existing
			new CodedSequenceItem("230001","99PMP","Creation of Radiation Dose SR"),					// PurposeOfReference
			"PixelMed",																					// Manufacturer
			null,																						// Institution Name
			null,																						// Institutional Department Name
			null,																						// Institution Address
			useAET,																						// Station Name
			manufacturerModelName == null ? this.getClass().getCanonicalName() : manufacturerModelName,	// Manufacturer's Model Name
			null,																						// Device Serial Number
			VersionAndConstants.getBuildDate(),															// Software Version(s)
			"Creation of Radiation Dose SR"																// ContributionDescription
		);
		list.insertSuitableSpecificCharacterSetForAllStringValues();
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
		list.removeMetaInformationHeaderAttributes();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,useAET);
        list.write(filename);
	}
	
	public void write(String filename,String aet) throws DicomException, IOException {
		write(filename,aet,null);
	}
	
	public void write(String filename) throws DicomException, IOException {
		write(filename,null);
	}

}

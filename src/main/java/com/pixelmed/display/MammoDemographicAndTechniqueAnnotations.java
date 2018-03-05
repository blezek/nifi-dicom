/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;

/**
 * <p>A class to extract selected DICOM annotative attributes into defined displayed area relative positions for mammograms.</p>
 *
 * @author	dclunie
 */
public class MammoDemographicAndTechniqueAnnotations extends DemographicAndTechniqueAnnotations {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/MammoDemographicAndTechniqueAnnotations.java,v 1.17 2017/01/24 10:50:40 dclunie Exp $";
	
	protected void initializeDefaultLayout() {
		layout=new Vector();
		
		// top
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"Patient [",true,true,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientID,null,null,null,true,true,0,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"] ",true,true,0,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientName,null,null,null,true,true,0,3,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"Study# ",true,true,1,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.StudyID,null,null,null,true,true,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," Acc# ",true,true,1,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AccessionNumber,null,null,null,true,true,1,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," Series# ",true,true,1,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.SeriesNumber,null,null,null,true,true,1,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," Image# ",true,true,1,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.InstanceNumber,null,null,null,true,true,1,7,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"Acquired ",true,true,2,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AcquisitionDate,null,null,null,true,true,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,2,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AcquisitionTime,null,null,null,true,true,2,4,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientSex,null,null,null,true,true,3,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," DOB ",true,true,3,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientBirthDate,null,null,null,true,true,3,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," Age ",true,true,3,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PatientAge,null,null,null,true,true,3,4,null,NOSPECIAL));

		// bottom
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Manufacturer,null,null,null,true,false,0,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,false,0,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ManufacturerModelName,null,null,null,true,false,0,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," SN# ",true,false,0,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.DeviceSerialNumber,null,null,null,true,false,0,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," Det# ",true,false,0,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.DetectorID,null,null,null,true,false,0,6,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.InstitutionName,null,null,null,true,false,1,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,true,1,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.InstitutionAddress,null,null,null,true,false,1,2,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.FilterMaterial,null,null,null,true,false,2,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"/",true,false,2,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.AnodeTargetMaterial,null,null,null,true,false,2,2,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,false,2,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.CompressionForce,null,null,null,true,false,2,4,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," N ",true,false,2,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.BodyPartThickness,null,null,null,true,false,2,6,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," mm ",true,false,2,7,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PositionerPrimaryAngle,null,null,null,true,false,2,8,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," deg ",true,false,2,9,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.RelativeXRayExposure,null,null,null,true,false,2,10,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," exp",true,false,2,11,null,NOSPECIAL));
		
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.KVP,null,null,null,true,false,3,0,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," kVP ",true,false,3,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.Exposure,null,null,null,true,false,3,3,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," mAs ",true,false,3,4,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ExposureTime,null,null,null,true,false,3,5,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," s Breast ",true,false,3,6,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.OrganDose,null,null,null,true,false,3,7,"#.###",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," dGy ESD ",true,false,3,8,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.EntranceDoseInmGy,null,null,null,true,false,3,9,"#.#",NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," mGy",true,false,3,10,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null,"Operator ",true,false,4,0,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.OperatorsName,null,null,null,true,false,4,1,null,NOSPECIAL));

		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.ReasonForRequestedProcedureCodeSequence,null,TagFromName.RequestAttributesSequence,null,true,false,5,0,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,false,5,1,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PartialView,null,null,"PARTIAL",true,false,5,2,null,TEXTIFYESNO));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,false,5,3,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.PartialViewCodeSequence,null,null,null,true,false,5,4,null,CODEMEANING));
		layout.add(new AnnotationLayoutConfigurationEntry(null,null,null,null," ",true,false,5,5,null,NOSPECIAL));
		layout.add(new AnnotationLayoutConfigurationEntry(null,TagFromName.BreastImplantPresent,null,null,"IMPLANT",true,false,5,6,null,TEXTIFYESNO));
	}

	/**
	 * <p>Return an abbreviation for a mammography view.</p>
	 *
	 * @param	list	the attributes of an item of ViewCodeSequence
	 * @return			a string value with an ACR/DICOM/IHE specified abbreviation
	 */
	protected static String getViewAbbreviationFromViewCodeSequenceAttributes(AttributeList list) {
		String abbreviation = "";
		if (list != null) {
			String codingSchemeDesignator = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeDesignator);
			String codeValue = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodeValue);
			if (codingSchemeDesignator.equals("SNM3") || codingSchemeDesignator.equals("SRT")) {
				if (codeValue.equals("R-10224")) {
					abbreviation = "ML";
				}
				else if (codeValue.equals("R-10226")) {
					abbreviation = "MLO";
				}
				else if (codeValue.equals("R-10228")) {
					abbreviation = "LM";
				}
				else if (codeValue.equals("R-10230")) {
					abbreviation = "LMO";
				}
				else if (codeValue.equals("R-10242")) {
					abbreviation = "CC";
				}
				else if (codeValue.equals("R-10244")) {
					abbreviation = "FB";
				}
				else if (codeValue.equals("R-102D0")) {
					abbreviation = "SIO";
				}
				else if (codeValue.equals("R-40AAA")) {
					abbreviation = "ISO";
				}
				else if (codeValue.equals("R-102CF")) {
					abbreviation = "XCC";
				}
				else if (codeValue.equals("R-1024A") || codeValue.equals("Y-X1770")) {
					abbreviation = "XCCL";
				}
				else if (codeValue.equals("R-1024B") || codeValue.equals("Y-X1771")) {
					abbreviation = "XCCM";
				}
			}
		}
		return abbreviation;
	}

	/**
	 * <p>Return an abbreviation for a mammography view modifer.</p>
	 *
	 * @param	list	the attributes of an item of ViewModifierCodeSequence
	 * @return			a string value with an ACR/DICOM/IHE specified abbreviation, including a leading "..." or trailing "..." to indicate prefix or suffix, or neither if a replacement
	 */
	protected static String getViewModifierAbbreviationFromViewModifierCodeSequenceAttributes(AttributeList list) {
		String abbreviation = "";
		if (list != null) {
			String codingSchemeDesignator = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodingSchemeDesignator);
			String codeValue = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.CodeValue);
			if (codingSchemeDesignator.equals("SNM3") || codingSchemeDesignator.equals("SRT")) {
				if (codeValue.equals("R-102D2")) {
					abbreviation = "CV";
				}
				else if (codeValue.equals("R-102D1")) {
					abbreviation = "AT";
				}
				else if (codeValue.equals("R-102D3")) {
					abbreviation = "...RL";
				}
				else if (codeValue.equals("R-102D4")) {
					abbreviation = "...RM";
				}
				else if (codeValue.equals("R-102CA")) {
					abbreviation = "...RI";
				}
				else if (codeValue.equals("R-102C9")) {
					abbreviation = "...RS";
				}
				else if (codeValue.equals("R-102D5")) {
					abbreviation = "...ID";
				}
				else if (codeValue.equals("R-40AB3")) {
					abbreviation = "...NP";
				}
				else if (codeValue.equals("P2-00161")) {
					abbreviation = "...AC";
				}
				else if (codeValue.equals("R-40ABE")) {
					abbreviation = "...IMF";
				}
				else if (codeValue.equals("R-40AB2")) {
					abbreviation = "...AX";
				}
				else if (codeValue.equals("R-102D6")) {
					abbreviation = "M...";
				}
				else if (codeValue.equals("R-102D7")) {
					abbreviation = "S...";
				}
				else if (codeValue.equals("R-102C2")) {
					abbreviation = "TAN";
				}
			}
		}
		return abbreviation;
	}

	/**
	 * <p>Return an abbreviation for laterality, view and view modifier.</p>
	 *
	 * @param	list	
	 * @return			a string value with an ACR/DICOM/IHE specified abbreviation
	 */
	protected static String getAbbreviationFromImageLateralityViewModifierAndViewModifierCodeSequenceAttributes(AttributeList list) {
		StringBuffer buf = new StringBuffer();
		buf.append(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ImageLaterality));
		Attribute aViewCodeSequence = list.get(TagFromName.ViewCodeSequence);
		if (aViewCodeSequence != null && aViewCodeSequence instanceof SequenceAttribute) {
			SequenceAttribute saViewCodeSequence = (SequenceAttribute)aViewCodeSequence;
			if (saViewCodeSequence.getNumberOfItems() > 0) {
				SequenceItem iViewCodeSequence = saViewCodeSequence.getItem(0);
				if (iViewCodeSequence != null) {
					AttributeList alViewCodeSequence = iViewCodeSequence.getAttributeList();
					if (alViewCodeSequence != null) {
						String viewPrimary = getViewAbbreviationFromViewCodeSequenceAttributes(alViewCodeSequence);
						TreeSet viewPrefixes = new TreeSet();
						TreeSet viewSuffixes = new TreeSet();
						Attribute aViewModifierCodeSequence = alViewCodeSequence.get(TagFromName.ViewModifierCodeSequence);
						if (aViewModifierCodeSequence != null && aViewModifierCodeSequence instanceof SequenceAttribute) {
							SequenceAttribute saViewModifierCodeSequence = (SequenceAttribute)aViewModifierCodeSequence;
							Iterator sitems = saViewModifierCodeSequence.iterator();
							while (sitems.hasNext()) {
								SequenceItem sitem = (SequenceItem)sitems.next();
								AttributeList alViewModifierCodeSequence = sitem.getAttributeList();
								String viewModifier = getViewModifierAbbreviationFromViewModifierCodeSequenceAttributes(alViewModifierCodeSequence);
								if (viewModifier.endsWith("...")) {
									viewPrefixes.add(viewModifier.substring(0,viewModifier.length()-3));
								}
								else if (viewModifier.startsWith("...")) {
									viewSuffixes.add(viewModifier.substring(3,viewModifier.length()));
								}
								else if (viewModifier.length() > 0) {
									viewPrimary = viewModifier;		// replace if not empty, prefix or suffix
								}
							}
						}
						Iterator i = viewPrefixes.iterator();
						while (i.hasNext()) {
							buf.append((String)i.next());
						}
						buf.append(viewPrimary);
						i = viewSuffixes.iterator();
						while (i.hasNext()) {
							buf.append((String)i.next());
						}
					}
				}
			}
		}
		return buf.toString();
	}

	/**
	 * @param	list			the DICOM attributes of a single or multi-frame image
	 * @param	leftSide		whether the side to annotate (the side opposite the chest wall) is left (true) or right (false)
	 */
	public MammoDemographicAndTechniqueAnnotations(AttributeList list,boolean leftSide) {
		super(list,null,leftSide);
	}
}


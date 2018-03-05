/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.*;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;

// the following are only for main test to use DatabaseTreeBrowser ...

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

/**
 * <p>The {@link com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel PatientStudySeriesConcatenationInstanceModel} class
 * supports a simple DICOM Patient/Study/Series/Concatenation/Instance model.</p>
 *
 * <p>Attributes of other DICOM entities than Patient, Study, Series, Concatenation and Instance are included at the appropriate lower level entity.</p>
 *
 * @see com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel
 *
 * @author	dclunie
 */
public class PatientStudySeriesConcatenationInstanceModel extends DicomDatabaseInformationModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/PatientStudySeriesConcatenationInstanceModel.java,v 1.27 2017/01/24 10:50:35 dclunie Exp $";

	/**
	 * <p>Construct a model with the attributes from the default dictionary.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel DicomDictionaryForPatientStudySeriesConcatenationInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName) throws DicomException {
		super(databaseFileName,InformationEntity.PATIENT,new DicomDictionaryForPatientStudySeriesConcatenationInstanceModel());
	}

	/**
	 * <p>Construct a model with the attributes from the default dictionary allowing external SQL access.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel DicomDictionaryForPatientStudySeriesConcatenationInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName,String databaseServerName) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.PATIENT,new DicomDictionaryForPatientStudySeriesConcatenationInstanceModel());
	}

	/**
	 * <p>Construct a model with the attributes from the default dictionary allowing external SQL access.</p>
	 *
	 * <p>The dictionary {@link com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel DicomDictionaryForPatientStudySeriesConcatenationInstanceModel} is used.</p>
	 * 
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @param	databaseRootName
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName,String databaseServerName,String databaseRootName) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.PATIENT,new DicomDictionaryForPatientStudySeriesConcatenationInstanceModel(),databaseRootName);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary.</p>
	 *
	 * @param	databaseFileName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,InformationEntity.PATIENT,dictionary);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary allowing external SQL access.</p>
	 *
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @param	dictionary
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName,String databaseServerName,DicomDictionary dictionary) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.PATIENT,dictionary);
	}

	/**
	 * <p>Construct a model with the attributes from the specified dictionary allowing external SQL access.</p>
	 *
	 * @param	databaseFileName
	 * @param	databaseServerName
	 * @param	dictionary
	 * @param	databaseRootName
	 * @throws	DicomException
	 */
	public PatientStudySeriesConcatenationInstanceModel(String databaseFileName,String databaseServerName,DicomDictionary dictionary,String databaseRootName) throws DicomException {
		super(databaseFileName,databaseServerName,InformationEntity.PATIENT,dictionary,databaseRootName);
	}

	/**
	 * @param	ie	the information entity
	 * @return		true if the information entity is in the model
	 */
	protected boolean isInformationEntityInModel(InformationEntity ie) {
		return ie == InformationEntity.PATIENT
		    || ie == InformationEntity.STUDY
		//  || ie == InformationEntity.PROCEDURESTEP
		    || ie == InformationEntity.SERIES
		    || ie == InformationEntity.CONCATENATION
		    || ie == InformationEntity.INSTANCE
		//  || ie == InformationEntity.FRAME
		    ;
	}

	/**
	 * @param	ie			the parent information entity
	 * @param	concatenation		true if concatenations are to be considered in the model
	 * @return				the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie,boolean concatenation) {
		if      (ie == InformationEntity.PATIENT)       return InformationEntity.STUDY;
		else if (ie == InformationEntity.STUDY)         return InformationEntity.SERIES;
		//else if (ie == InformationEntity.STUDY)         return InformationEntity.PROCEDURESTEP;
		//else if (ie == InformationEntity.PROCEDURESTEP) return InformationEntity.SERIES;
		//else if (ie == InformationEntity.SERIES)        return InformationEntity.INSTANCE;
		else if (ie == InformationEntity.SERIES)        return concatenation ? InformationEntity.CONCATENATION : InformationEntity.INSTANCE;
		else if (ie == InformationEntity.CONCATENATION) return InformationEntity.INSTANCE;
		//else if (ie == InformationEntity.INSTANCE)      return InformationEntity.FRAME;
		//else if (ie == InformationEntity.FRAME)         return null;
		else return null;
	}

	/**
	 * @param	ie			the parent information entity
	 * @param	concatenationUID	the ConcatenationUID, if present, else null, as a flag to use concatenations in the model or not
	 * @return				the child information entity
	 */
	private InformationEntity getChildTypeForParent(InformationEntity ie,String concatenationUID) {
		return getChildTypeForParent(ie,concatenationUID != null);
	}

	/**
	 * @param	ie	the parent information entity
	 * @return		the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie) {
		return getChildTypeForParent(ie,true);		// this method is called e.g. when creating tables, so assume concatenation
	}

	/**
	 * @param	ie	the parent information entity
	 * @param	list	an AttributeList, in which ConcatenationUID may or may not be present,as a flag to use concatenations in the model or not
	 * @return		the child information entity
	 */
	public InformationEntity getChildTypeForParent(InformationEntity ie,AttributeList list) {
		String concatenationUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ConcatenationUID);
		return getChildTypeForParent(ie,concatenationUID);
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of a column in the table that describes the instance of the information entity
	 */
	public String getDescriptiveColumnName(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return "PATIENTNAME";
		else if (ie == InformationEntity.STUDY)         return "STUDYDATE";			// to allow tree sort by study date first ([bugs.mrmf] (000111) Studies in browser not sorted by date but ID, and don't display date)
		else if (ie == InformationEntity.PROCEDURESTEP) return null;
		else if (ie == InformationEntity.SERIES)        return "SERIESNUMBER";
		else if (ie == InformationEntity.CONCATENATION) return "INSTANCENUMBER";
		else if (ie == InformationEntity.INSTANCE)      return "INSTANCENUMBER";
		else if (ie == InformationEntity.FRAME)         return null;
		else return null;
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of another column in the table that describes the instance of the information entity
	 */
	public String getOtherDescriptiveColumnName(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return null;
		else if (ie == InformationEntity.STUDY)         return "STUDYID";			// [bugs.mrmf] (000111) Studies in browser not sorted by date but ID, and don't display date
		else if (ie == InformationEntity.PROCEDURESTEP) return null;
		else if (ie == InformationEntity.SERIES)        return null;
		else if (ie == InformationEntity.CONCATENATION) return null;
		else if (ie == InformationEntity.INSTANCE)      return "INCONCATENATIONNUMBER";
		else if (ie == InformationEntity.FRAME)         return null;
		else return null;
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of yet another column in the table that describes the instance of the information entity
	 */
	public String getOtherOtherDescriptiveColumnName(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return "PATIENTID";
		else if (ie == InformationEntity.STUDY)         return "STUDYDESCRIPTION";
		else if (ie == InformationEntity.PROCEDURESTEP) return null;
		else if (ie == InformationEntity.SERIES)        return "SERIESDESCRIPTION";
		else if (ie == InformationEntity.CONCATENATION) return null;
		else if (ie == InformationEntity.INSTANCE)      return "IMAGECOMMENTS";
		else if (ie == InformationEntity.FRAME)         return null;
		else return null;
	}

	/**
	 * @param	ie	the information entity
	 * @return		the upper case name of the column in the table that describes the UID of the information entity, or null if none
	 */
	public String getUIDColumnNameForInformationEntity(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return null;
		else if (ie == InformationEntity.STUDY)         return "STUDYINSTANCEUID";
		else if (ie == InformationEntity.PROCEDURESTEP) return null;
		else if (ie == InformationEntity.SERIES)        return "SERIESINSTANCEUID";
		else if (ie == InformationEntity.CONCATENATION) return "CONCATENATIONUID";
		else if (ie == InformationEntity.INSTANCE)      return "SOPINSTANCEUID";
		else if (ie == InformationEntity.FRAME)         return null;
		else return null;
	}

	/**
	 * @param	b
	 * @param	ie
	 */
	protected void extendCreateStatementStringWithAnyExtraAttributes(StringBuffer b,InformationEntity ie) {
		if (ie == InformationEntity.INSTANCE) {		// since dictionary now says InstanceNumber is in Concatenation IE
								// but we want the UID in both IE's so tree browser can show it
								// at either concatenation or image level depending on whether
								// Concatenation is present (as flagged by Concatenation UID)
			String columnName = "INSTANCENUMBER";
			String columnType = getSQLTypeFromDicomValueRepresentation(ValueRepresentation.IS);
			b.append(", ");
			b.append(columnName);
			b.append(" ");
			b.append(columnType);
		}
	}
	
	/**
	 * @param	b
	 * @param	list
	 * @param	ie
	 * @throws	DicomException
	 */
	protected void extendStatementStringWithMatchingAttributesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException {
//System.err.println("PatientStudySeriesConcatenationInstanceModel.extendStatementStringWithMatchingAttributesForSelectedInformationEntity():");

		// two possibilities ...
		// 1. iterate through whole list of attributes and insist on match for all present for that IE
		// 2. be more selective ... consider match only on "unique key(s)" for a particular level and ignore others
		//
		// adopt the latter approach ...

		// also need to escape wildcards and so on, but ignore for now ...

		if      (ie == InformationEntity.PATIENT) {
			// no AND since would be no parent reference preceding
			b.append("PATIENT.PATIENTID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientID)));
			// use patient name as well as ID, since no truly unique key for patients, and ID alone is insufficient
			b.append(" AND ");
			b.append("PATIENT.PATIENTNAME");
			String patientName = getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.PatientName));
			if (patientName != null && patientName.contains("^")) {
//System.err.println("PatientStudySeriesConcatenationInstanceModel.extendStatementStringWithMatchingAttributesForSelectedInformationEntity(): patientName was "+patientName);
				patientName = patientName.replaceFirst("\\^+\'","\'");		// Trailing empty components are of no significance so should be treated as absent
//System.err.println("PatientStudySeriesConcatenationInstanceModel.extendStatementStringWithMatchingAttributesForSelectedInformationEntity(): patientName now "+patientName);
			}
			appendExactOrIsNullMatch(b,patientName);
		}
		else if (ie == InformationEntity.STUDY) {
			b.append(" AND ");
			b.append("STUDY.STUDYINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.StudyInstanceUID)));
		}
		else if (ie == InformationEntity.PROCEDURESTEP) {
		}
		else if (ie == InformationEntity.SERIES) {
			b.append(" AND ");
			b.append("SERIES.SERIESINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SeriesInstanceUID)));
		}
		else if (ie == InformationEntity.CONCATENATION) {
			Attribute concatenationUID = list.get(TagFromName.ConcatenationUID);
			Attribute instanceNumber = list.get(TagFromName.InstanceNumber);
			if (concatenationUID != null) {
				b.append(" AND ");
				b.append("CONCATENATION.CONCATENATIONUID");
				appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(concatenationUID));
			}
			else {
				b.append(" AND ");
				b.append("CONCATENATION.INSTANCENUMBER");
				appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(instanceNumber));
			}
		}
		else if (ie == InformationEntity.INSTANCE) {
			b.append(" AND ");
			b.append("INSTANCE.SOPINSTANCEUID");
			appendExactOrIsNullMatch(b,getQuotedEscapedSingleStringValueOrNull(list.get(TagFromName.SOPInstanceUID)));
		}
		else if (ie == InformationEntity.FRAME) {
		}
	}


	/**
	 * <p>Create  additional indexes on UIDs to optimize queries.</p>
	 */
	protected void createAdditionalIndexes() throws DicomException {
//System.err.println("PatientStudySeriesConcatenationInstanceModel.createAdditionalIndexes():");
		super.createAdditionalIndexes();
		boolean success = true;
		StringBuffer errors = new StringBuffer();
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX PATIENT_ID_IDX ON PATIENT (PATIENTID)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of PATIENTID: "+e);
			success = false;
		}
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX PATIENT_NAME_IDX ON PATIENT (PATIENTNAME)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of PATIENTNAME: "+e);
			success = false;
		}
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX STUDY_UID_IDX ON STUDY (STUDYINSTANCEUID)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of STUDYINSTANCEUID: "+e);
			success = false;
		}
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX SERIES_UID_IDX ON SERIES (SERIESINSTANCEUID)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of SERIESINSTANCEUID: "+e);
			success = false;
		}
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX SERIES_FORUID_IDX ON SERIES (FRAMEOFREFERENCEUID)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of FRAMEOFREFERENCEUID: "+e);
			success = false;
		}
		try {
			Statement s = databaseConnection.createStatement();
			s.execute("CREATE INDEX INSTANCE_UID_IDX ON INSTANCE (SOPINSTANCEUID)");
			s.close();
		} catch (Exception e) {
			errors.append("Cannot create index of SOPINSTANCEUID: "+e);
			success = false;
		}
		if (!success) {
			throw new DicomException(errors.toString());
		}
	}

	/**
	 * <p>For unit test
	 * purposes.</p>
	 *
	 * <p>Reads the DICOM files listed on the command line, loads them into the model and pops up a browser
	 * for viewing the tree hierarchy of the model and the values of each instance of an entity.</p>
	 *
	 * @param	arg	a list of DICOM file names
	 */
	public static void main(String arg[]) {
		try {
			final DatabaseInformationModel d = new PatientStudySeriesConcatenationInstanceModel("test");
			java.util.Set sopInstanceUIDs = new java.util.HashSet();
			java.util.Set localFileNames = new java.util.HashSet();
			for (int j=0; j<arg.length; ++j) {
				String fileName = arg[j];
				System.err.print("reading "+fileName);	// no need to use SLF4J since command line utility/test
				DicomInputStream dfi = new DicomInputStream(new BufferedInputStream(new FileInputStream(fileName)));
				AttributeList list = new AttributeList();
				list.read(dfi,TagFromName.PixelData);
				sopInstanceUIDs.add(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID));
				dfi.close();
				//d.extendTablesAsNecessary(list);		// doesn't work with Hypersonic ... ALTER command not supported
				System.err.print("inserting");
				d.insertObject(list,fileName,DatabaseInformationModel.FILE_REFERENCED);
			}
//System.err.print(d);
			{
				String localFileNameColumnName = d.getLocalFileNameColumnName(InformationEntity.INSTANCE);
				java.util.Iterator i = sopInstanceUIDs.iterator();
				while (i.hasNext()) {
					String uid = (String)i.next();
					System.err.print("Searching database for uid "+uid);
					java.util.ArrayList result = d.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity.INSTANCE,uid);
					if (result != null) {
						for (int r=0; r<result.size(); ++r) {
							java.util.Map map = (java.util.Map)(result.get(r));
							String localFileNameValue = (String)map.get(localFileNameColumnName);
							System.err.print("Got record # "+r+" "+localFileNameColumnName+" = "+localFileNameValue);
							localFileNames.add(localFileNameValue);
						}
					}
				}
			}
			{
				String localFileNameColumnName = d.getLocalFileNameColumnName(InformationEntity.INSTANCE);
				java.util.Iterator i = localFileNames.iterator();
				while (i.hasNext()) {
					String localFileNameKeyValue = (String)i.next();
					System.err.print("Searching database for localFileName "+localFileNameKeyValue);
					java.util.ArrayList result = d.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(InformationEntity.INSTANCE,localFileNameColumnName,localFileNameKeyValue);
					if (result != null) {
						for (int r=0; r<result.size(); ++r) {
							java.util.Map map = (java.util.Map)(result.get(r));
							String localFileNameValue = (String)map.get(localFileNameColumnName);
							System.err.print("Got record # "+r+" "+localFileNameColumnName+" = "+localFileNameValue+" is expected = "+localFileNameValue.equals(localFileNameKeyValue));
							localFileNames.add(localFileNameValue);
						}
					}
				}
			}
			final JFrame frame = new JFrame();
			frame.setSize(400,800);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					frame.dispose();
					d.close();
					System.exit(0);
				}
			});
			System.err.print("building tree");
			DatabaseTreeBrowser tree = new DatabaseTreeBrowser(d,frame);
			System.err.print("display tree");
			frame.setVisible(true); 
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}


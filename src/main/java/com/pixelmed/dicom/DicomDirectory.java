/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import javax.swing.tree.*;
import javax.swing.event.*;

import java.util.*;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
public class DicomDirectory implements TreeModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomDirectory.java,v 1.40 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(OtherWordAttributeOnDisk.class);

	// Our nodes are all instances of DicomDirectoryRecord ...

	/***/
	private DicomDirectoryRecord root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	public Object getChild(Object node,int index) {
		return ((DicomDirectoryRecord)node).getChildAt(index);
	}

	public int getIndexOfChild(Object parent, Object child) {
		return ((DicomDirectoryRecord)parent).getIndex((DicomDirectoryRecord)child);
	}

	public Object getRoot() { return root; }

	public int getChildCount(Object parent) {
		return ((DicomDirectoryRecord)parent).getChildCount();
	}

	public boolean isLeaf(Object node) {
		return ((DicomDirectoryRecord)node).getChildCount() == 0;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	public void addTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners = new Vector();
		listeners.addElement(tml);
	}

	public void removeTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners.removeElement(tml);
	}

	// Methods specific to DicomDirectory

	/***/
	private TreeMap mapOffsetToSequenceItemAttributeList;
	/***/
	private DicomDirectoryRecordFactory nodeFactory;
	
	/**
	 * @param	node
	 * @param	wantConcatenationUID
	 * @param	useInstanceNumber
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private DicomDirectoryRecord findOrInsertNewConcatenationDirectoryRecord(DicomDirectoryRecord node,String wantConcatenationUID,String useInstanceNumber) throws DicomException {
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: "+wantConcatenationUID);
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: searching parent "+node);
		DicomDirectoryRecord found = null;
		int n = getChildCount(node);
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: child count="+n);
		for (int i=0; i<n; ++i) {
			DicomDirectoryRecord child=(DicomDirectoryRecord)getChild(node,i);
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: examining child "+child);
			if (child instanceof DicomDirectoryRecordFactory.ConcatenationDirectoryRecord) {
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: have ConcatenationDirectoryRecord");
				AttributeList list = child.getAttributeList();
				String haveConcatenationUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ConcatenationUID);
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: comparing with existing ConcatenationDirectoryRecord "+haveConcatenationUID);
				if (haveConcatenationUID != null && wantConcatenationUID != null && haveConcatenationUID.equals(wantConcatenationUID)) {
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: match");
					found=child;
					break;
				}
			}
		}
		if (found == null) {
//System.err.println("findOrInsertNewConcatenationDirectoryRecord: making new one");
			AttributeList list = new AttributeList();
			Attribute directoryRecordType = new CodeStringAttribute(TagFromName.DirectoryRecordType);
			directoryRecordType.addValue(DicomDirectoryRecordType.concatentation);
			list.put(TagFromName.DirectoryRecordType,directoryRecordType);
			Attribute concatenationUID = new UniqueIdentifierAttribute(TagFromName.ConcatenationUID);
			concatenationUID.addValue(wantConcatenationUID);
			list.put(TagFromName.ConcatenationUID,concatenationUID);
			if (useInstanceNumber != null) {
				Attribute instanceNumber = new CodeStringAttribute(TagFromName.InstanceNumber);
				instanceNumber.addValue(useInstanceNumber);
				list.put(TagFromName.InstanceNumber,instanceNumber);
			}
			found=nodeFactory.getNewDicomDirectoryRecord(node,list);
			node.addChild(found);
		}
		return found;
	}
	
	/**
	 * @param	node
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private void insertConcatenationNodes(DicomDirectoryRecord node) throws DicomException {
//System.err.println("insertConcatenationNodes:");
		int n = getChildCount(node);
		int i=0;
		while (i<n) {
			DicomDirectoryRecord child=(DicomDirectoryRecord)getChild(node,i);
			if (node instanceof DicomDirectoryRecordFactory.SeriesDirectoryRecord && child instanceof DicomDirectoryRecordFactory.ImageDirectoryRecord) {
//System.err.println("insertConcatenationNodes: testing child ["+i+"]");
				AttributeList list = child.getAttributeList();
				String concatenationUID = Attribute.getSingleStringValueOrNull(list,TagFromName.ConcatenationUID);
				if (concatenationUID != null) {
					String instanceNumber = Attribute.getSingleStringValueOrNull(list,TagFromName.InstanceNumber);
					DicomDirectoryRecord concatenation = findOrInsertNewConcatenationDirectoryRecord(node,concatenationUID,instanceNumber);
//System.err.println("insertConcatenationNodes:concatenation in series is: "+concatenation);
//System.err.println("insertConcatenationNodes:removing child from series: "+child);
					node.removeChild(child);
//System.err.println("insertConcatenationNodes:adding child to concatenation:");
					concatenation.addChild(child);
					// restart the scan since the list has changed ... (and take care not to inadvertantly immediately increment i !)
					i=0;
					n=getChildCount(node);
				}
				else {
					++i;
				}
			}
			else {
				insertConcatenationNodes(child);
				++i;
			}
		}
	}

	/**
	 * @param	parent
	 * @param	offset
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private DicomDirectoryRecord processSubTree(DicomDirectoryRecord parent,long offset) throws DicomException {
//System.err.println("processSubTree:");

		AttributeList list = (AttributeList)(mapOffsetToSequenceItemAttributeList.get(new Long(offset)));
		DicomDirectoryRecord node = nodeFactory.getNewDicomDirectoryRecord(parent,list);

		long offsetOfFirstChild = 0;
		Attribute aOffsetOfReferencedLowerLevelDirectoryEntity = list.get(TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity);
		if (aOffsetOfReferencedLowerLevelDirectoryEntity != null && aOffsetOfReferencedLowerLevelDirectoryEntity.getVM() > 0) {
			offsetOfFirstChild = aOffsetOfReferencedLowerLevelDirectoryEntity.getLongValues()[0];
		}
		if (offsetOfFirstChild != 0) {
//System.err.println("processSubTree: addChild offset=0x"+Long.toHexString(offsetOfFirstChild)+" to node "+node);
			node.addChild(processSubTree(node,offsetOfFirstChild));
		}

		long offsetOfNextSibling = 0;
		Attribute aOffsetOfTheNextDirectoryRecord = list.get(TagFromName.OffsetOfTheNextDirectoryRecord);
		if (aOffsetOfTheNextDirectoryRecord != null && aOffsetOfTheNextDirectoryRecord.getVM() > 0) {
			offsetOfNextSibling = aOffsetOfTheNextDirectoryRecord.getLongValues()[0];
		}
		if (offsetOfNextSibling != 0) {
//System.err.println("processSubTree: addSibling offset=0x"+Long.toHexString(offsetOfNextSibling)+" to parent "+parent);
			node.addSibling(processSubTree(parent,offsetOfNextSibling));
		}

		return node;
	}

	/**
	 * @param	list
	 * @param	doConcatenations
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private void makeDicomDirectoryFromExistingAttributeList(AttributeList list,boolean doConcatenations) throws DicomException {
//long startTime = System.currentTimeMillis();
//System.err.println(list.toString());

		// Step 1 ... traverse entire (linear) directory record sequence
		// and build index of offsets of each sequence item

//System.err.println("Make offset mapping");

		mapOffsetToSequenceItemAttributeList = new TreeMap();

		Attribute aDirectoryRecordSequence = list.get(TagFromName.DirectoryRecordSequence);
		if (aDirectoryRecordSequence == null || !(aDirectoryRecordSequence instanceof SequenceAttribute)) {
			throw new DicomException("Missing Directory Record Sequence in DICOMDIR or not SQ VR");
		}
		Iterator i = ((SequenceAttribute)aDirectoryRecordSequence).iterator();
		while (i.hasNext()) {
			SequenceItem item = (SequenceItem)i.next();
			mapOffsetToSequenceItemAttributeList.put(new Long(item.getByteOffset()),item.getAttributeList());
		}
//long currentTime = System.currentTimeMillis();
//System.err.println("Make offset mapping = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
		// Step 2 ... walk tree starting from root, building our tree

//System.err.println("Walk tree");

		nodeFactory=new DicomDirectoryRecordFactory();

		long offsetOfRoot = Attribute.getSingleLongValueOrDefault(list,TagFromName.OffsetOfTheFirstDirectoryRecordOfTheRootDirectoryEntity,0);
		if (offsetOfRoot == 0) {
			throw new DicomException("Missing or invalid Root Directory First Record");
		}
		else {
			root = nodeFactory.getNewTopDirectoryRecord();		// we create our own (empty) root on top
			root.addChild(processSubTree(root,offsetOfRoot));	// the DICOMDIR "root" is really the first of many siblings
		}
//currentTime = System.currentTimeMillis();
//System.err.println("Walk tree took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
//System.err.println(toString());

		// Step 3 ... walk tree to insert pseudo-records for concatenations ...
		
		if (doConcatenations) insertConcatenationNodes(root);
//currentTime = System.currentTimeMillis();
//System.err.println("Inserting concatenations took = "+(currentTime-startTime)+" ms");
//startTime=currentTime;
		// Step 4 ... clean up intermediate data structures ...

		mapOffsetToSequenceItemAttributeList=null;
		nodeFactory=null;

//System.err.println(toString());
	}

	/**
	 * @param		parent
	 * @param		candidate
	 */
	private DicomDirectoryRecord findExistingDirectoryRecordOrMakeNewOne(DicomDirectoryRecord parent,DicomDirectoryRecord candidate) {
		int existingIndex = parent.getIndex(candidate);	// this assumes that the equals() test in getIndex() tests semantic equivalance and not object equality
		if (existingIndex == -1) {
			candidate.setParent(parent);
			parent.addChild(candidate);
//System.err.println("DicomDirectory.findExistingDirectoryRecordOrMakeNewOne():  creating new "+candidate.getClass());
		}
		else {
			candidate = (DicomDirectoryRecord)parent.getChildAt(existingIndex);
//System.err.println("DicomDirectory.findExistingDirectoryRecordOrMakeNewOne():  using existing "+candidate.getClass());
			// could at this perform some kind of merge, adding any "new" attributes (or values) in candidate.getAttributeList(), etc. :(
			// could at this perform check for other attributes that were used in matching for equality :(
			// could even look for earliest study date/times, etc. :(
		}
		return candidate;
	}
	
	/**
	 * @param		tag
	 * @param		srcList
	 * @param		dstList
	 */
	private void findAttributeAndIfPresentAddToDifferentAttributeList(AttributeTag tag,AttributeList srcList,AttributeList dstList) {
		Attribute a = srcList.get(tag);
		if (a != null) {
			dstList.put(a);
		}
	}

	/**
	 * @param		tag
	 * @param		srcList
	 * @param		dstList
	 */
	private void findAttributeAndIfPresentWithValueAddToDifferentAttributeList(AttributeTag tag,AttributeList srcList,AttributeList dstList) {
		Attribute a = srcList.get(tag);
		if (a != null && a.getVM() > 0) {
			dstList.put(a);
		}
	}


	/**
	 * @param		tag
	 * @param		srcList
	 * @param		dstList
	 */
	private void findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(AttributeTag tag,AttributeList srcList,AttributeList dstList) throws DicomException {
		Attribute a = srcList.get(tag);
		if (a == null) {
			a = AttributeFactory.newAttribute(tag);
		}
		dstList.put(a);
	}

	/**
	 * @param		tag
	 * @param		srcList
	 * @param		dstList
	 */
	private void findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(AttributeTag tag,AttributeList srcList,AttributeList dstList,String def) throws DicomException {
		Attribute a = srcList.get(tag);
		if (a == null || a.getVM() == 0) {
			a = AttributeFactory.newAttribute(tag);
			a.addValue(def);
		}
		dstList.put(a);
	}
	
	/**
	 * Read DICOM file and add it to this directory.
	 *
	 * @param	rootDirectory	the name of the directory to use as the root, which contains the DICOM file
	 * @param	fileName			the name of the DICOM file (relative to rootDirectory)
	 * @throws	IOException			if an I/O error occurs
	 * @throws	DicomException		if error in DICOM encoding
	 */
	public void readDicomFileAndAddToDirectory(File rootDirectory,String fileName) throws DicomException, IOException {
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): rootDirectory = "+rootDirectory);
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): fileName = "+fileName);
		AttributeList list = new AttributeList();
		list.setDecompressPixelData(false);
		list.read(new File(rootDirectory,fileName).getCanonicalPath(),TagFromName.PixelData);	// no need to read PixelData since not creating icons; do not want to stop any earlier since specific directory records need "igher numbered group attributes
		addAttributeListFromDicomFileToDirectory(list,fileName);
	}

	/**
	 * Read DICOM file and add it to this directory.
	 *
	 * @param	fileName		the name of the DICOM file (relative to the current working directory)
	 * @throws	IOException			if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public void readDicomFileAndAddToDirectory(String fileName) throws DicomException, IOException {
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): fileName = "+fileName);
		AttributeList list = new AttributeList();
		list.setDecompressPixelData(false);
		list.read(fileName,TagFromName.PixelData);	// no need to read PixelData since not creating icons; do not want to stop any earlier since specific directory records need "igher numbered group attributes
		addAttributeListFromDicomFileToDirectory(list,fileName);
	}
		
	/**
	 * Add an instance described by a list of attributes to this directory.
	 *
	 * Builds the necessary patient, study, series and instance level directory records,
	 * with the instance level record being the appropriate type for the SOP Class.
	 *
	 * @param	list			the list of attributes containing the attributes to add to this directory
	 * @param	fileName		the name of the DICOM file
	 * @throws	IOException			if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding, or the SOP Class is not recognized, or the instance already exists in the directory
	 */
	public void addAttributeListFromDicomFileToDirectory(AttributeList list,String fileName) throws DicomException, IOException {
//System.err.println("DicomDirectory adding "+fileName);
		DicomDirectoryRecordFactory.PatientDirectoryRecord patientDirectoryRecord;
		{
			AttributeList recordList = new AttributeList();
			findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.PatientName,list,recordList);			// should probably trim trailing carets etc as in dcdirmk :(
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.PatientID,list,recordList,"000000");
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PatientBirthDate,list,recordList);		// (DVD:1C if present with value in image)
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PatientSex,list,recordList);				// (DVD:1C if present with value in image)
			
			{ AttributeTag t = TagFromName.DirectoryRecordType; Attribute a = new CodeStringAttribute(t); a.addValue(DicomDirectoryRecordType.patient); recordList.put(t,a); }
			
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SpecificCharacterSet,list,recordList);
			{ AttributeTag t = TagFromName.OffsetOfTheNextDirectoryRecord; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.RecordInUseFlag; Attribute a = new UnsignedShortAttribute(t); a.addValue(0xffff); recordList.put(t,a); }
			
			patientDirectoryRecord = nodeFactory.getNewPatientDirectoryRecord(null,recordList);
			patientDirectoryRecord = (DicomDirectoryRecordFactory.PatientDirectoryRecord)findExistingDirectoryRecordOrMakeNewOne(root,patientDirectoryRecord);
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): PatientDirectoryRecord = "+patientDirectoryRecord);
		}

		DicomDirectoryRecordFactory.StudyDirectoryRecord studyDirectoryRecord;
		{
			AttributeList recordList = new AttributeList();
			findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.StudyInstanceUID,list,recordList);
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.StudyID,list,recordList,"000000");
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.StudyDate,list,recordList,"19000101");
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.StudyTime,list,recordList,"000000");
			findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.StudyDescription,list,recordList);
			findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.AccessionNumber,list,recordList);
			
			{ AttributeTag t = TagFromName.DirectoryRecordType; Attribute a = new CodeStringAttribute(t); a.addValue(DicomDirectoryRecordType.study); recordList.put(t,a); }
			
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SpecificCharacterSet,list,recordList);
			{ AttributeTag t = TagFromName.OffsetOfTheNextDirectoryRecord; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.RecordInUseFlag; Attribute a = new UnsignedShortAttribute(t); a.addValue(0xffff); recordList.put(t,a); }
			
			studyDirectoryRecord = nodeFactory.getNewStudyDirectoryRecord(null,recordList);
			studyDirectoryRecord = (DicomDirectoryRecordFactory.StudyDirectoryRecord)findExistingDirectoryRecordOrMakeNewOne(patientDirectoryRecord,studyDirectoryRecord);
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): StudyDirectoryRecord = "+studyDirectoryRecord);
		}

		DicomDirectoryRecordFactory.SeriesDirectoryRecord seriesDirectoryRecord;
		{
			AttributeList recordList = new AttributeList();
			findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.SeriesInstanceUID,list,recordList);
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.Modality,list,recordList,"OT");
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.SeriesNumber,list,recordList,"0");
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SeriesDate,list,recordList);
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SeriesTime,list,recordList);
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SeriesDescription,list,recordList);
			
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.InstitutionName,list,recordList);				// (DVD:1C if present with value in image)
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.InstitutionAddress,list,recordList);			// (DVD:1C if present with value in image)
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PerformingPhysicianName,list,recordList);		// (DVD:1C if present with value in image)
			
			{ AttributeTag t = TagFromName.DirectoryRecordType; Attribute a = new CodeStringAttribute(t); a.addValue(DicomDirectoryRecordType.series); recordList.put(t,a); }
			
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SpecificCharacterSet,list,recordList);
			{ AttributeTag t = TagFromName.OffsetOfTheNextDirectoryRecord; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.RecordInUseFlag; Attribute a = new UnsignedShortAttribute(t); a.addValue(0xffff); recordList.put(t,a); }
			
			seriesDirectoryRecord = nodeFactory.getNewSeriesDirectoryRecord(null,recordList);
			seriesDirectoryRecord = (DicomDirectoryRecordFactory.SeriesDirectoryRecord)findExistingDirectoryRecordOrMakeNewOne(studyDirectoryRecord,seriesDirectoryRecord);
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): SeriesDirectoryRecord = "+seriesDirectoryRecord);
		}
		
		String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
		String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
		String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
		{
			AttributeList recordList = new AttributeList();
			findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.InstanceNumber,list,recordList,"0");	// Type 1 in images, SR, KO, RT dose and most others (Type 2 in raw data)
			
			String directoryRecordType = "";
			if (SOPClass.isImageStorage(sopClassUID)) {
				directoryRecordType = DicomDirectoryRecordType.image;
			
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ContentDate,list,recordList);						// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ContentTime,list,recordList);						// optional

				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ImageComments,list,recordList);					// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PhotometricInterpretation,list,recordList);		// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.AcquisitionDate,list,recordList);					// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.AcquisitionTime,list,recordList);					// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.VolumetricProperties,list,recordList);			// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.VolumeBasedCalculationTechnique,list,recordList);	// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ComplexImageComponent,list,recordList);			// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.AcquisitionContrast,list,recordList);				// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PixelPresentation,list,recordList);				// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ConcatenationUID,list,recordList);				// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ConcatenationFrameOffsetNumber,list,recordList);	// optional
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.InConcatenationNumber,list,recordList);			// optional
		
				// could try and extract ImagePositionPatient, ImageOrientationPatient and PixelSpacing from within SharedFunctionalGroupsSequence :(
				
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ImageType,list,recordList,"UNKNOWN");	// (XABC-CD,XA1K-CD:1); (DVD:1C if present with value in image)
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.CalibrationImage,list,recordList);					// (XABC-CD,XA1K-CD:2); (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ReferencedImageSequence,list,recordList);				// (XABC-CD,XA1K-CD:1C if ImageType value 3 is BIPLANE A or B); (CTMR:1C if present in image); (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ImagePositionPatient,list,recordList);				// (CTMR:1C if present in image); (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.ImageOrientationPatient,list,recordList);				// (CTMR:1C if present in image); (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.FrameOfReferenceUID,list,recordList);					// (CTMR:1C if present in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.PixelSpacing,list,recordList);						// (CTMR:1C if present in image); (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.TableHeight,list,recordList);							// (CTMR:1C if present in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.Rows,list,recordList);								// (CTMR:1); (DVD:1)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.Columns,list,recordList);								// (CTMR:1); (DVD:1)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.LossyImageCompressionRatio,list,recordList);			// (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SynchronizationFrameOfReferenceUID,list,recordList);	// (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.NumberOfFrames,list,recordList);						// (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.AcquisitionTimeSynchronized,list,recordList);			// (DVD:1C if present with value in image)
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.AcquisitionDateTime,list,recordList);					// (DVD:1C if present with value in image)
			}
			else if (SOPClass.isSpectroscopy(sopClassUID)) {
				directoryRecordType = DicomDirectoryRecordType.spectroscopy;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ImageType,list,recordList,"UNKNOWN");		// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentDate,list,recordList,"19000101");	// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentTime,list,recordList,"000000");		// Type 1
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ReferencedImageEvidenceSequence,list,recordList);					// Type 1C
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.NumberOfFrames,list,recordList);							// Type 1 but no reasonable default
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.Rows,list,recordList);									// Type 1 but no reasonable default
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.Columns,list,recordList);									// Type 1 but no reasonable default
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.DataPointRows,list,recordList);							// Type 1 but no reasonable default
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.DataPointColumns,list,recordList);						// Type 1 but no reasonable default
			}
			else if (SOPClass.isRawData(sopClassUID)) {
				directoryRecordType = DicomDirectoryRecordType.rawData;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentDate,list,recordList,"19000101");	// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentTime,list,recordList,"000000");		// Type 1
			}
			else if (SOPClass.isStructuredReport(sopClassUID)) {
				if (SOPClass.isKeyObjectSelectionDocument(sopClassUID)) {
					directoryRecordType = DicomDirectoryRecordType.keyObjectDocument;
				}
				else {
					directoryRecordType = DicomDirectoryRecordType.srDocument;
					findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.CompletionFlag,list,recordList,"PARTIAL");		// Type 1 in SR but not KO
					findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.VerificationFlag,list,recordList,"UNVERIFIED");	// Type 1 in SR but not KO
				}
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentDate,list,recordList,"19000101");		// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentTime,list,recordList,"000000");			// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.VerificationDateTime,list,recordList);						// Type 1C
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ConceptNameCodeSequence,list,recordList);								// Type 1
				// Should also do ContentSequence with HAS CONCEPT MOD Relationship Type :(
			}
			else if (SOPClass.isEncapsulatedDocument(sopClassUID)) {
				directoryRecordType = DicomDirectoryRecordType.encapsulatedDocument;
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentDate,list,recordList);													// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentTime,list,recordList);													// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.DocumentTitle,list,recordList);												// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.HL7InstanceIdentifier,list,recordList);													// Type 1C
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ConceptNameCodeSequence,list,recordList);													// Type 2
				String defaultMimeType = sopClassUID.equals(SOPClass.EncapsulatedPDFStorage) ? "application/pdf" : (sopClassUID.equals(SOPClass.EncapsulatedCDAStorage) ? "text/XML" : "application/octet-stream");
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.MIMETypeOfEncapsulatedDocument,list,recordList,defaultMimeType);	// Type 1
			}
			else if (SOPClass.isPresentationState(sopClassUID)) {
				directoryRecordType = DicomDirectoryRecordType.presentationState;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.PresentationCreationDate,list,recordList,"19000101");	// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.PresentationCreationTime,list,recordList,"000000");		// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentLabel,list,recordList,"NONE");					// Type 1
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentDescription,list,recordList);								// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentCreatorName,list,recordList);								// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ContentCreatorIdentificationCodeSequence,list,recordList);					// Type 3
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ReferencedSeriesSequence,list,recordList);										// Type 1C
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.BlendingSequence,list,recordList);												// Type 1C
			}
			else if (sopClassUID.equals(SOPClass.SpatialRegistrationStorage)
				 || sopClassUID.equals(SOPClass.DeformableSpatialRegistrationStorage)
				 || sopClassUID.equals(SOPClass.SpatialFiducialsStorage)
				 ||  sopClassUID.equals(SOPClass.RealWorldValueMappingStorage)) {
				directoryRecordType = sopClassUID.equals(SOPClass.SpatialFiducialsStorage)
					? DicomDirectoryRecordType.fiducial
					: (sopClassUID.equals(SOPClass.RealWorldValueMappingStorage)
						? DicomDirectoryRecordType.realWorldValueMapping
						: DicomDirectoryRecordType.registration);
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentDate,list,recordList,"19000101");			// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentTime,list,recordList,"000000");				// Type 1
				// all use Content Identification Macro ...
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.ContentLabel,list,recordList,"NONE");				// Type 1
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentDescription,list,recordList);							// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.ContentCreatorName,list,recordList);							// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeList(TagFromName.ContentCreatorIdentificationCodeSequence,list,recordList);				// Type 3
			}
			else if (sopClassUID.equals(SOPClass.StereometricRelationshipStorage)) {
				directoryRecordType = DicomDirectoryRecordType.stereometricRelationship;
			}
			else if (sopClassUID.equals(SOPClass.RTDoseStorage)) {
				directoryRecordType = DicomDirectoryRecordType.rtDose;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.DoseSummationType,list,recordList,"UNKNOWN");		// Type 1
				findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.DoseComment,list,recordList);										// Type 3
			}
			else if (sopClassUID.equals(SOPClass.RTStructureSetStorage)) {
				directoryRecordType = DicomDirectoryRecordType.rtStructureSet;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.StructureSetLabel,list,recordList,"NONE");			// Type 1
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.StructureSetDate,list,recordList);								// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.StructureSetTime,list,recordList);								// Type 2
			}
			else if (sopClassUID.equals(SOPClass.RTPlanStorage) || sopClassUID.equals(SOPClass.RTIonPlanStorage)) {
				directoryRecordType = DicomDirectoryRecordType.rtPlan;
				findAttributeAndIfPresentWithValueAddToDifferentAttributeListElseAddDefault(TagFromName.RTPlanLabel,list,recordList,"NONE");				// Type 1
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.RTPlanDate,list,recordList);									// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.RTPlanTime,list,recordList);									// Type 2
			}
			else if (sopClassUID.equals(SOPClass.RTBeamsTreatmentRecordStorage)
				  || sopClassUID.equals(SOPClass.RTIonBeamsTreatmentRecordStorage)
				  || sopClassUID.equals(SOPClass.RTBrachyTreatmentRecordStorage)
				  || sopClassUID.equals(SOPClass.RTTreatmentSummaryRecordStorage)) {
				directoryRecordType = DicomDirectoryRecordType.rtTreatmentRecord;
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.TreatmentDate,list,recordList);								// Type 2
				findAttributeAndIfPresentAddToDifferentAttributeListElseAddEmpty(TagFromName.TreatmentTime,list,recordList);								// Type 2
			}
			// don't do HL7 Structured Document Directory Record here, because it would not be read in as a DICOM instance
			
			{ AttributeTag t = TagFromName.DirectoryRecordType; Attribute a = new CodeStringAttribute(t); a.addValue(directoryRecordType); recordList.put(t,a); }
			
			{ AttributeTag t = TagFromName.ReferencedSOPClassUIDInFile; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(sopClassUID); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.ReferencedSOPInstanceUIDInFile; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(sopInstanceUID); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.ReferencedTransferSyntaxUIDInFile; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(transferSyntaxUID); recordList.put(t,a); }
			
			{
				AttributeTag t = TagFromName.ReferencedFileID; Attribute a = new CodeStringAttribute(t);
				StringTokenizer fileNameComponents = new StringTokenizer(new File(fileName).getPath().toUpperCase(java.util.Locale.US),File.separator);
				while (fileNameComponents.hasMoreTokens()) {
					a.addValue(fileNameComponents.nextToken());
				}
				recordList.put(t,a);
			}
			
			findAttributeAndIfPresentWithValueAddToDifferentAttributeList(TagFromName.SpecificCharacterSet,list,recordList);
			{ AttributeTag t = TagFromName.OffsetOfTheNextDirectoryRecord; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); recordList.put(t,a); }
			{ AttributeTag t = TagFromName.RecordInUseFlag; Attribute a = new UnsignedShortAttribute(t); a.addValue(0xffff); recordList.put(t,a); }
			
			DicomDirectoryRecord instanceDirectoryRecord = null;
			// cannot do this earlier, since may extract values of various attributes from recordList
			if (directoryRecordType.equals(DicomDirectoryRecordType.image)) {
				instanceDirectoryRecord = nodeFactory.getNewImageDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.spectroscopy)) {
				instanceDirectoryRecord = nodeFactory.getNewSpectroscopyDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.rawData)) {
				instanceDirectoryRecord = nodeFactory.getNewRawDataDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.waveform)) {
				instanceDirectoryRecord = nodeFactory.getNewWaveformDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.keyObjectDocument)) {
				instanceDirectoryRecord = nodeFactory.getNewKODocumentDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.srDocument)) {
				instanceDirectoryRecord = nodeFactory.getNewSRDocumentDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.presentationState)) {
				instanceDirectoryRecord = nodeFactory.getNewPresentationStateDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.registration)) {
				instanceDirectoryRecord = nodeFactory.getNewRegistrationDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.fiducial)) {
				instanceDirectoryRecord = nodeFactory.getNewFiducialDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.realWorldValueMapping)) {
				instanceDirectoryRecord = nodeFactory.getNewRealWorldValueMappingDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.stereometricRelationship)) {
				instanceDirectoryRecord = nodeFactory.getNewStereometricRelationshipDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.encapsulatedDocument)) {
				instanceDirectoryRecord = nodeFactory.getNewEncapsulatedDocumentDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.rtDose)) {
				instanceDirectoryRecord = nodeFactory.getNewRTDoseDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.rtStructureSet)) {
				instanceDirectoryRecord = nodeFactory.getNewRTStructureSetDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.rtPlan)) {
				instanceDirectoryRecord = nodeFactory.getNewRTPlanDirectoryRecord(null,recordList);
			}
			else if (directoryRecordType.equals(DicomDirectoryRecordType.rtTreatmentRecord)) {
				instanceDirectoryRecord = nodeFactory.getNewRTTreatmentRecordDirectoryRecord(null,recordList);
			}
			
			if (instanceDirectoryRecord != null) {
				int existingIndex = seriesDirectoryRecord.getIndex(instanceDirectoryRecord);
				if (existingIndex != -1) {
					DicomDirectoryRecord existingInstanceDirectoryRecord = (DicomDirectoryRecord)seriesDirectoryRecord.getChildAt(existingIndex);
					AttributeList existingInstanceDirectoryRecordList = existingInstanceDirectoryRecord == null ? null : existingInstanceDirectoryRecord.getAttributeList();
					String existingFileName = existingInstanceDirectoryRecordList == null ? "-unknown-" : Attribute.getDelimitedStringValuesOrDefault(existingInstanceDirectoryRecordList,TagFromName.ReferencedFileID,"-unknown-");
					throw new DicomException("Instance already exists within series for UID "+sopInstanceUID+" from file "+existingFileName+" - not adding file "+fileName);
				}
				instanceDirectoryRecord.setParent(seriesDirectoryRecord);
				seriesDirectoryRecord.addChild(instanceDirectoryRecord);
			}
			else {
				throw new DicomException("Unrecognized SOP Class "+sopClassUID+" - cannot create directory record - not adding file "+fileName);
			}
//System.err.println("DicomDirectory.readDicomFileAndAddToDirectory(): instance DirectoryRecord = "+instanceDirectoryRecord);
		}
	}
	
	protected HashMap mapOfDirectoryRecordsToSequenceItems;			// used during DICOMDIR creation
	
	/**
	 * <p>Create a new DicomDirectory from a list of existing DICOM files contained within the current working directory.</p>
	 *
	 * <p>The filenames must be relative to the current working directory, and not absolute paths, since the full name
	 * will be used in the DICOMDIR records.</p>
	 *
	 * <p>Filenames are NOT checked for compliance with restrictions on length and character set.</p>
	 *
	 * @param	fileNames	the names of the DICOM files (relative to the current working directory)
	 */
	public DicomDirectory(String[] fileNames) {
//System.err.println("DicomDirectory.DicomDirectory(String[]): start");
//long startTime = System.currentTimeMillis();
		mapOfDirectoryRecordsToSequenceItems = new HashMap();
		nodeFactory=new DicomDirectoryRecordFactory();
		root = nodeFactory.getNewTopDirectoryRecord();		// we create our own (empty) root on top
		for (int i=0; i<fileNames.length; ++i) {
			String fileName = fileNames[i];
			try {
				readDicomFileAndAddToDirectory(fileName);
			}
			catch (Exception e) {
				// Do NOT fail just because one file is unreadable or has a problem
				slf4jlogger.error("While reading file \"{}\"",fileName,e);
			}
		}
//long endTime = System.currentTimeMillis();
//System.err.println("DicomDirectory.DicomDirectory(String[]): DICOMDIR creation - took = "+(endTime-startTime)+" ms");
	}
	
	/**
	 * <p>Create a new DicomDirectory from a list of existing DICOM files contained within a specified root directory.</p>
	 *
	 * <p>The specified root directory will NOT be included in the referenced file name in the DICOMDIR records.</p>
	 *
	 * <p>Filenames are NOT checked for compliance with restrictions on length and character set.</p>
	 *
	 * @param	rootDirectory	the name of the directory to use as the root, which contains the DICOM file
	 * @param	fileNames		the names of the DICOM files (relative to rootDirectory)
	 */
	public DicomDirectory(File rootDirectory,String[] fileNames) {
//System.err.println("DicomDirectory.DicomDirectory(File,String[]): start");
//long startTime = System.currentTimeMillis();
		mapOfDirectoryRecordsToSequenceItems = new HashMap();
		nodeFactory=new DicomDirectoryRecordFactory();
		root = nodeFactory.getNewTopDirectoryRecord();		// we create our own (empty) root on top
		for (int i=0; i<fileNames.length; ++i) {
			String fileName = fileNames[i];
			try {
				readDicomFileAndAddToDirectory(rootDirectory,fileName);
			}
			catch (Exception e) {
				// Do NOT fail just because one file is unreadable or has a problem
				slf4jlogger.error("While reading file \"{}\"",fileName,e);
			}
		}
//long endTime = System.currentTimeMillis();
//System.err.println("DicomDirectory.DicomDirectory(File,String[]): DICOMDIR creation - took = "+(endTime-startTime)+" ms");
	}
	
	/**
	 * Create an empty DicomDirectory
	 */
	public DicomDirectory() {
		mapOfDirectoryRecordsToSequenceItems = new HashMap();
		nodeFactory=new DicomDirectoryRecordFactory();
		root = nodeFactory.getNewTopDirectoryRecord();		// we create our own (empty) root on top
	}
	
	/**
	 * Create a DicomDirectory from a DICOMDIR instance already read as an AttributeList
	 *
	 * @param	list			a list of attributes describing a DICOMDIR instance
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DicomDirectory(AttributeList list) throws DicomException {
		makeDicomDirectoryFromExistingAttributeList(list,true);
	}
	
	/**
	 * Create a DicomDirectory from a DICOMDIR instance already read as an AttributeList, optionally creating synthetic concatenation records
	 *
	 * @param	list				a list of attributes describing a DICOMDIR instance
	 * @param	doConcatenations	true if synthetic concatenation records are to be created
	 * @throws	DicomException		if error in DICOM encoding
	 */
	public DicomDirectory(AttributeList list,boolean doConcatenations) throws DicomException {
		makeDicomDirectoryFromExistingAttributeList(list,doConcatenations);
	}
	
	/**
	 * @param	directoryRecordSequence
	 * @param	node
	 */
	private void walkTreeToBuildAttributeList(SequenceAttribute directoryRecordSequence,DicomDirectoryRecord node) {
		AttributeList recordList = node.getAttributeList();
		if (recordList != null) {
			SequenceItem recordSequenceItem = new SequenceItem(recordList);
			directoryRecordSequence.addItem(recordSequenceItem);
			mapOfDirectoryRecordsToSequenceItems.put(node,recordSequenceItem);		// keep map so that we can fix up offsets
		}
		// else may be null for top level root
		int n = getChildCount(node);
		for (int i=0; i<n; ++i) {
			walkTreeToBuildAttributeList(directoryRecordSequence,(DicomDirectoryRecord)getChild(node,i));
		}
	}

	private AttributeList walkTreeToBuildAttributeList() throws DicomException {
		AttributeList list = new AttributeList();
		FileMetaInformation.addFileMetaInformation(list,SOPClass.MediaStorageDirectoryStorage,new UIDGenerator().getAnotherNewUID(),TransferSyntax.ExplicitVRLittleEndian,null);
		{ AttributeTag t = TagFromName.OffsetOfTheFirstDirectoryRecordOfTheRootDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); list.put(t,a); }
		{ AttributeTag t = TagFromName.OffsetOfTheLastDirectoryRecordOfTheRootDirectoryEntity; Attribute a = new UnsignedLongAttribute(t); a.addValue(0); list.put(t,a); }
		{ AttributeTag t = TagFromName.FileSetConsistencyFlag; Attribute a = new UnsignedShortAttribute(t); a.addValue(0); list.put(t,a); }
		{ AttributeTag t = TagFromName.FileSetID; Attribute a = new CodeStringAttribute(t); list.put(t,a); }
		SequenceAttribute directoryRecordSequence = new SequenceAttribute(TagFromName.DirectoryRecordSequence);
		list.put(directoryRecordSequence);
		walkTreeToBuildAttributeList(directoryRecordSequence,root);
		return list;
	}
	
	/**
	 * @param	directoryRecordSequence
	 * @param	node
	 */
	private void walkTreeToFixUpOffsetsInAttributeList(SequenceAttribute directoryRecordSequence,DicomDirectoryRecord node) throws DicomException {
		int n = getChildCount(node);
		DicomDirectoryRecord previousChild = null;
		for (int i=0; i<n; ++i) {
			DicomDirectoryRecord currentChild = (DicomDirectoryRecord)getChild(node,i);
			if (previousChild == null) {
				AttributeList recordList = node.getAttributeList();
				if (recordList != null) {
					recordList.get(TagFromName.OffsetOfReferencedLowerLevelDirectoryEntity).setValue(((SequenceItem)mapOfDirectoryRecordsToSequenceItems.get(currentChild)).getByteOffset());
				}
				// else may be null for top level root
			}
			else {
				previousChild.getAttributeList().get(TagFromName.OffsetOfTheNextDirectoryRecord).setValue(((SequenceItem)mapOfDirectoryRecordsToSequenceItems.get(currentChild)).getByteOffset());
			}
			walkTreeToFixUpOffsetsInAttributeList(directoryRecordSequence,currentChild);
			previousChild = currentChild;
		}
	}

	/**
	 * @param	list
	 */
	private void walkTreeToFixUpOffsetsInAttributeList(AttributeList list) throws DicomException {
		SequenceAttribute directoryRecordSequence = (SequenceAttribute)list.get(TagFromName.DirectoryRecordSequence);
		list.get(TagFromName.OffsetOfTheFirstDirectoryRecordOfTheRootDirectoryEntity).setValue(directoryRecordSequence.getItem(0).getByteOffset());
		list.get(TagFromName.OffsetOfTheLastDirectoryRecordOfTheRootDirectoryEntity).setValue(directoryRecordSequence.getItem(directoryRecordSequence.getNumberOfItems()-1).getByteOffset());
		walkTreeToFixUpOffsetsInAttributeList(directoryRecordSequence,root);
	}
	
	/**
	 * <p>Write the directory to the named file.</p>
	 *
	 * @param	name			the file name to write to
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public void write(String name) throws IOException, DicomException {
		AttributeList list = walkTreeToBuildAttributeList();
//System.err.println("DicomDirectory.main(): flattened attribute list:\n"+list);
		list.write(new NullOutputStream(),TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/,true/*closeAfterWrite*/);
		 walkTreeToFixUpOffsetsInAttributeList(list);
//System.err.println("DicomDirectory.main(): offsets inserted in attribute list:\n"+list);
		list.write(name,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
	}

	/**
	 * @param	node
	 */
	private String walkTreeToString(DicomDirectoryRecord node) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(node.toString());
		buffer.append("\n");
		//buffer.append(node.getAttributeList());

		int n = getChildCount(node);
		for (int i=0; i<n; ++i) buffer.append(walkTreeToString((DicomDirectoryRecord)getChild(node,i)));

		return buffer.toString();
	}

	public String toString() {
		return walkTreeToString(root);
	}

	private Map<String,String> mapOfSOPInstanceUIDToReferencedFileName;

	/**
	 * @param	record
	 * @param	parentFilePath
	 */
	private void addToMapOfSOPInstanceUIDToReferencedFileName(DicomDirectoryRecord record,String parentFilePath) {
		String fileName = null;
		AttributeList list = record.getAttributeList();
		if (list != null) {
			try {
				Attribute a = list.get(TagFromName.ReferencedFileID);
				if (a != null) {
					String[] filePath = a.getStringValues();
					if (filePath != null && filePath.length > 0) {		// empty Attribute encountered in buggy DICOMDIRs that have this attribute at SERIES level (000370)
						fileName=buildPathFromParentAndStringArray(parentFilePath,filePath);
					}
				}
			}
			catch (DicomException e) {
			}
		}
		String uid = Attribute.getSingleStringValueOrNull(list,TagFromName.ReferencedSOPInstanceUIDInFile);
		if (fileName != null && uid != null) {
//System.err.println("Adding "+uid+" = "+fileName);
			mapOfSOPInstanceUIDToReferencedFileName.put(uid,fileName);
		}
		int n = getChildCount(record);
		for (int i=0; i<n; ++i) addToMapOfSOPInstanceUIDToReferencedFileName((DicomDirectoryRecord)getChild(record,i),parentFilePath);
	}

	/**
	 * <p>Build a map of file names indexed by SOP Instance UID.</p>
	 *
	 * @param	parentFilePath	the path to the root of the file names encoded in this directory
	 * @return					a map of SOP Instance UID keys to file names including the supplied parentFilePath
	 */
	public Map<String,String> getMapOfSOPInstanceUIDToReferencedFileName(String parentFilePath) {
		if (mapOfSOPInstanceUIDToReferencedFileName == null) {
//long startTime = System.currentTimeMillis();
			mapOfSOPInstanceUIDToReferencedFileName = new HashMap<String,String>();
			addToMapOfSOPInstanceUIDToReferencedFileName(root,parentFilePath);
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectory.getMapOfSOPInstanceUIDToReferencedFileName(): lazy instantiation of mapOfSOPInstanceUIDToReferencedFileName took = "+(currentTime-startTime)+" ms");
		}
		return mapOfSOPInstanceUIDToReferencedFileName;
	}

	// Convenience methods and their supporting methods ...

	/**
	 * <p>Get the file name for the specified SOP Instance UID.</p>
	 *
	 * @param	sopInstanceUID		the SOP Instance UID wanted
	 * @return						the referenced file name, or null if not found
	 * @throws	DicomException		if map has not been initialized
	 */
	public String getReferencedFileNameForSOPInstanceUID(String sopInstanceUID) throws DicomException {
		if (mapOfSOPInstanceUIDToReferencedFileName == null) {
			throw new DicomException("Map of SOPInstanceUID to ReferencedFileName has not been initialized");
		}
		else {
			return mapOfSOPInstanceUIDToReferencedFileName.get(sopInstanceUID);

		}
	}

	/**
	 * <p>Build a string representing the path for a parent (root) and components.</p>
	 *
	 * @param	parent		parent file path name
	 * @param	components	array of filename components
	 * @return				the path
	 */
	private static String buildPathFromParentAndStringArray(String parent,String[] components) throws DicomException {
		File path = (parent == null) ? null : new File(parent);
		if (components == null) {
			throw new DicomException("No components in Referenced File ID");
		}
		else {
			if (components.length == 0) {
				throw new DicomException("No components in Referenced File ID");
			}
			for (int i=0; i<components.length; ++i) {
				String component = components[i];
				if (component == null || component.length() == 0) {
					throw new DicomException("Empty component in Referenced File ID");
				}
				else {
					path = (path == null) ? new File(component) : new File(path,component);
				}
			}
		}
		return path.getPath();
	}

	/**
	 * <p>Build a string representing the path for a parent (root) and components of a ReferencedFileID.</p>
	 *
	 * @param	record			directory record that contains a ReferencedFileID
	 * @param	parentFilePath	the root path to which the ReferencedFileID is relative
	 * @return					the path
	 */
	private static String getReferencedFileName(DicomDirectoryRecord record,String parentFilePath) {
		String name=null;
		AttributeList list = ((DicomDirectoryRecord)record).getAttributeList();
		if (list != null) {
			//System.err.println(list);
			try {
				Attribute a = list.get(TagFromName.ReferencedFileID);
				if (a != null) {
					String[] filePath = a.getStringValues();
					name=buildPathFromParentAndStringArray(parentFilePath,filePath);
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
		return name;
	}

	/**
	 * <p>Get all the referenced file names at or below the specified directory record, and a map to the directory records that reference them.</p>
	 *
	 * @param	record			directory record to start at
	 * @param	parentFilePath	the folder in which the DICOMDIR lives (i.e., the base for contained references)
	 * @return					a {@link java.util.HashMap HashMap} whose keys are {@link java.lang.String String} file names fully qualified by the specified parent, mapped to DicomDirectoryRecords
	 */
	public static HashMap<String,DicomDirectoryRecord> findAllContainedReferencedFileNamesAndTheirRecords(DicomDirectoryRecord record,String parentFilePath) {
		HashMap<String,DicomDirectoryRecord> map = new HashMap<String,DicomDirectoryRecord>();
		String name = getReferencedFileName(record,parentFilePath);
		if (name != null && name.length() > 0) {
			map.put(name,record);
		}
		int nChildren = record.getChildCount();
		for (int i=0; i<nChildren; ++i) {
			DicomDirectoryRecord child=(DicomDirectoryRecord)(record.getChildAt(i));
			map.putAll(findAllContainedReferencedFileNamesAndTheirRecords(child,parentFilePath));
		}
		return map;
	}

	/**
	 * <p>Get all the referenced file names in the entire directory, and a map to the directory records that reference them.</p>
	 *
	 * @param	parentFilePath	the folder in which the DICOMDIR lives (i.e., the base for contained references)
	 * @return					a {@link java.util.HashMap HashMap} whose keys are {@link java.lang.String String} file names fully qualified by the specified parent, mapped to DicomDirectoryRecords
	 */
	public HashMap<String,DicomDirectoryRecord> findAllContainedReferencedFileNamesAndTheirRecords(String parentFilePath) {
		return findAllContainedReferencedFileNamesAndTheirRecords((DicomDirectoryRecord)(getRoot()),parentFilePath);
	}

	/**
	 * <p>Get all the referenced file names at or below the specified directory record.</p>
	 *
	 * @param	record			directory record to start at
	 * @param	parentFilePath	the folder in which the DICOMDIR lives (i.e., the base for contained references)
	 * @return					a {@link java.util.Vector Vector} of {@link java.lang.String String} file names fully qualified by the specified parent
	 */
	public static Vector<String> findAllContainedReferencedFileNames(DicomDirectoryRecord record,String parentFilePath) {
//long startTime = System.currentTimeMillis();
		Vector<String> names = new Vector<String>();
		//String name = getReferencedFileName(record,parentFilePath);
		//if (name != null) names.add(name);
		//int nChildren = record.getChildCount();
		//for (int i=0; i<nChildren; ++i) {
		//	DicomDirectoryRecord child=(DicomDirectoryRecord)(record.getChildAt(i));
		//	names.addAll(findAllContainedReferencedFileNames(child,parentFilePath));
		//}
		//return names;
		HashMap<String,DicomDirectoryRecord> map = findAllContainedReferencedFileNamesAndTheirRecords(record,parentFilePath);
		names.addAll(map.keySet());
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectory.findAllContainedReferencedFileNames(): took = "+(currentTime-startTime)+" ms");
		return names;
	}

	/**
	 * <p>Get all the referenced file names in the entire directory.</p>
	 *
	 * @param	parentFilePath	the folder in which the DICOMDIR lives (i.e., the base for contained references)
	 * @return					a {@link java.util.Vector Vector} of {@link java.lang.String String} file names fully qualified by the specified parent
	 */
	public Vector<String> findAllContainedReferencedFileNames(String parentFilePath) {
		return findAllContainedReferencedFileNames((DicomDirectoryRecord)(getRoot()),parentFilePath);
	}


	/**
	 * @param	record
	 * @param	attributeLists
	 * @param	frameOfReferenceUID
	 */
	private static void findAllImagesForFrameOfReference(DicomDirectoryRecord record,Vector<AttributeList> attributeLists,String frameOfReferenceUID) {
		if (record != null) {
			AttributeList list=record.getAttributeList();
			if (list != null) {
				if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.DirectoryRecordType).equals(DicomDirectoryRecordType.image)) {
					if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.FrameOfReferenceUID).equals(frameOfReferenceUID)) {
						attributeLists.add(list);
					}
				}
			}
			int nChildren = record.getChildCount();
			for (int i=0; i<nChildren; ++i) {
				findAllImagesForFrameOfReference((DicomDirectoryRecord)(record.getChildAt(i)),attributeLists,frameOfReferenceUID);
			}
		}
	}

	/**
	 * <p>Get the attribute lists from all the IMAGE level directory records which have a particular FrameOfReferenceUID.</p>
	 *
	 * <p>Useful for finding potential localizers and orthogonal images.</p>
	 *
	 * <p>Note that even though FrameOfReference is a series level entity, in the CT/MR profiles it is specified at the IMAGE directory record level.</p>
	 *
	 * @param	frameOfReferenceUID the frame of reference UID to search for
	 * @return						a {@link java.util.Vector Vector} of {@link com.pixelmed.dicom.AttributeList AttributeList}
	 */
	public Vector<AttributeList> findAllImagesForFrameOfReference(String frameOfReferenceUID) {
//long startTime = System.currentTimeMillis();
		Vector<AttributeList> attributeLists = new Vector<AttributeList>();
		findAllImagesForFrameOfReference((DicomDirectoryRecord)getRoot(),attributeLists,frameOfReferenceUID);
//long currentTime = System.currentTimeMillis();
//System.err.println("DicomDirectory.findAllImagesForFrameOfReference(): took = "+(currentTime-startTime)+" ms");
		return attributeLists;
	}

	/**
	 * <p>Read DICOM files and create a DICOMDIR.</p>
	 *
	 * @param	arg	 optionally the folder in which the files and DICOMDIR are rooted, the filename of the DICOMDIR to be created, then optionally a list of all the filenames of the DICOM files to include (otherwise the folder will be searched)
	 */
	public static void main(String arg[]) {
		if (arg.length >= 2) {
			try {
				File rootDirectory = new File(arg[0]);
				int offset;
				String dicomdirName;
				if (rootDirectory.isDirectory()) {
					offset = 2;
					dicomdirName = new File(rootDirectory,arg[1]).getCanonicalPath();
				}
				else {
					rootDirectory = null;
					offset = 1;
					dicomdirName = arg[0];
				}
				int nFiles  = arg.length - offset;
				String[] sourceFiles;
				if (nFiles == 0 && rootDirectory != null && rootDirectory.isDirectory()) {
					// search the supplied rootDirectory for all files and supply them to constructor as paths relative to the rootDirectory
					ArrayList<File> sourceFilesList = FileUtilities.listFilesRecursively(rootDirectory);
					nFiles = sourceFilesList.size();
					sourceFiles = new String[nFiles];
					String canonicalPathOfRootDirectoryForReplacement = "^" + Pattern.quote(rootDirectory.getCanonicalPath()) + File.separator;
					//sourceFiles = FileUtilities.getCanonicalFileNames(sourceFilesList).toArray(sourceFiles);
					//for (int i=0; i<nFiles; ++i) {
					//	sourceFiles[i] = sourceFiles[i].replaceFirst(canonicalPathOfRootDirectoryForReplacement,"");
//System.err.println("DicomDirectory.main(): sourceFiles["+i+"] = "+sourceFiles[i]);
					//}
					int i=0;
					for (File f : sourceFilesList) {
//System.err.println("DicomDirectory.main(): sourceFiles["+i+"] = "+sourceFiles[i]);
						sourceFiles[i++] = f.getCanonicalPath().replaceFirst(canonicalPathOfRootDirectoryForReplacement,"");
					}
				}
				else {
					sourceFiles = new String[nFiles];
					System.arraycopy(arg,offset,sourceFiles,0,nFiles);
				}
				DicomDirectory dicomDirectory;
				if (rootDirectory == null) {
					dicomDirectory = new DicomDirectory(sourceFiles);
				}
				else {
					dicomDirectory = new DicomDirectory(rootDirectory,sourceFiles);
				}
//System.err.println("DicomDirectory.main(): created:\n"+dicomDirectory);
				dicomDirectory.write(dicomdirName);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
				System.exit(0);
			}
		}
		else {
			System.err.println("Usage:");
		}
	}
}






/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.Writer;

import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import org.xml.sax.SAXException;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeFactory;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.ImageToDicom;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SpecificCharacterSet;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;

import com.pixelmed.display.SafeFileChooser;

import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.ScrollingTextAreaWriter;

/**
 * <p>This class provides conversion of a set of Amicas JPEG 2000 files to DICOM.</p>
 *
 * @author	dclunie
 */
public class ConvertAmicasJPEG2000FilesetToDicom {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/ConvertAmicasJPEG2000FilesetToDicom.java,v 1.18 2017/01/24 10:50:34 dclunie Exp $";

	protected String mediaDirectoryPath;

	protected PrintWriter pwlogger;		// use name to distinguish from SLF4J use
	
	public void setLogger(PrintWriter pwlogger) {
		this.pwlogger = pwlogger;
	}
	
	public void setLogger(OutputStream stream) {
		this.pwlogger = new PrintWriter(stream);
	}
	
	public void setLogger(javax.swing.JFrame content,int width,int height) {
		this.pwlogger = new PrintWriter(new ScrollingTextAreaWriter(content,width,height));
	}
	
	/**
	 * <p>Construct an converter that will looked for files in the system default path.</p>
	 */
	public ConvertAmicasJPEG2000FilesetToDicom() {
		mediaDirectoryPath=null;
		setLogger(System.err);
	}

	/**
	 * <p>Construct an converter that will looked for files in the specified path.</p>
	 *
	 * @param	mediaDirectoryPath	where to begin looking for the amicas-patients folder
	 */
	public ConvertAmicasJPEG2000FilesetToDicom(String mediaDirectoryPath) {
		this.mediaDirectoryPath=mediaDirectoryPath;
		setLogger(System.err);
	}

	/**
	 * <p>Pop up folder chooser dialogs that allow the user to specify the location of
	 * the amicas-patients folder, or the parent folder (for example, the drive or volume) in which
	 * the amicas-patients folder is located, and the output folder to store the converted DICOM
	 * files, and then perform the conversion.</p>
	 */
	public void choosePathsAndConvertAmicasFiles() throws IOException, DicomException, ParserConfigurationException, SAXException {
		String amicasPathName = null;
		{
			SafeFileChooser.SafeFileChooserThread fileChooserThread = new SafeFileChooser.SafeFileChooserThread(JFileChooser.DIRECTORIES_ONLY,mediaDirectoryPath,"Select Amicas-Patient Folder ...");
			try {
				java.awt.EventQueue.invokeAndWait(fileChooserThread);
				amicasPathName=fileChooserThread.getSelectedFileName();
				mediaDirectoryPath=fileChooserThread.getCurrentDirectoryPath();
			}
			catch (InterruptedException e) {
				e.printStackTrace(pwlogger);
			}
			catch (InvocationTargetException e) {
				e.printStackTrace(pwlogger);
			}
		}

		String dicomPathName = null;
		{
			SafeFileChooser.SafeFileChooserThread fileChooserThread = new SafeFileChooser.SafeFileChooserThread(JFileChooser.DIRECTORIES_ONLY,mediaDirectoryPath,"Select Folder to store DICOM output files ...");
			try {
				java.awt.EventQueue.invokeAndWait(fileChooserThread);
				dicomPathName=fileChooserThread.getSelectedFileName();
				mediaDirectoryPath=fileChooserThread.getCurrentDirectoryPath();
			}
			catch (InterruptedException e) {
				e.printStackTrace(pwlogger);
			}
			catch (InvocationTargetException e) {
				e.printStackTrace(pwlogger);
			}
		}
		
		convertAmicasFiles(amicasPathName,dicomPathName);
	}
	
	/**
	 * <p>Instances of this class select only Files that are directories.</p>
	 */
	public final class OnlyDirectoriesFileFilter implements FileFilter {
		public boolean accept(File file) { return file.isDirectory(); }
	}	
	
	/**
	 * <p>Instances of this class select only File names that end with the specified suffix.</p>
	 *
	 * <p>The comparison is case-insensitive.</p>
	 *
	 * <p>If a period is expected to precede the suffix, it should be included in the suffix specified in the constructor, e.g., ".dcm".</p>
	 *
	 */
	public final class OnlySuffixFilenameFilter implements FilenameFilter {
		protected String suffix;
		public OnlySuffixFilenameFilter(String suffix) {
			this.suffix = suffix.toUpperCase(java.util.Locale.US);
		}
		public boolean accept(File file,String name) {
			return name.toUpperCase(java.util.Locale.US).endsWith(suffix);
		}
	}	

	protected OnlyDirectoriesFileFilter onlyDirectoriesFileFilter = new OnlyDirectoriesFileFilter();
	protected OnlySuffixFilenameFilter onlyXMLFilenameFilter = new OnlySuffixFilenameFilter(".XML");
	
	protected SpecificCharacterSet specificCharacterSet;
		
	protected static DicomDictionary dictionary = new DicomDictionary();
	
	protected static UIDGenerator uidGenerator = new UIDGenerator();
	
	protected Attribute newAttribute(AttributeTag tag) throws DicomException {
		byte [] vr = dictionary.getValueRepresentationFromTag(tag);
		return AttributeFactory.newAttribute(tag,vr,specificCharacterSet);
	}
	
	protected String getNamedAttributeValue(NamedNodeMap xmlAttributes,String xmlAttributeName) {
		String value = null;
		Node node = xmlAttributes.getNamedItem(xmlAttributeName);
		if (node != null) {
			value = node.getNodeValue();
			if (value != null) {
				value = value.trim();
			}
		}
		return value;
	}

	protected Attribute makeDicomAttributeFromXmlAttribute(AttributeTag tag,String xmlAttributeName,NamedNodeMap xmlAttributes,int type,String defaultValue) throws DicomException {
		String value = getNamedAttributeValue(xmlAttributes,xmlAttributeName);
		if (type == 1 && (value == null || value.length() == 0)) {
			value=defaultValue;
		}
		else if (type == 2 && value == null) {
			value="";
		}
		else if (type == 3 && (value == null || value.length() == 0)) {		// this removes empty attributes that are not applicable to the IOD
			value=null;
		}
		Attribute a = null;
		if (value != null) {
			a = newAttribute(tag);
			if (a != null) {
				a.addValue(value);
			}
		}
		return a;
	}

	protected void addDicomAttributeFromXmlAttribute(AttributeList list,AttributeTag tag,String xmlAttributeName,NamedNodeMap xmlAttributes,int type,String defaultValueForType1) throws DicomException {
		Attribute a = makeDicomAttributeFromXmlAttribute(tag,xmlAttributeName,xmlAttributes,type,defaultValueForType1);
		if (a != null) {
			list.put(a);
		}
	}
	
	protected AttributeList makeAttributeListForPatient(NamedNodeMap xmlAttributes) throws DicomException {
		AttributeList list = new AttributeList();
		return list;
	}

	protected AttributeList makeAttributeListForStudy(NamedNodeMap xmlAttributes) throws DicomException {
		AttributeList list = new AttributeList();
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientName,"PatientName",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientID,"PatientID",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientBirthDate,"PatientBirthDate",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientSex,"PatientSex",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientAge,"PatientAge",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientWeight,"PatientWeight",xmlAttributes,3,null);
		
		addDicomAttributeFromXmlAttribute(list,TagFromName.StudyInstanceUID,"StudyInstanceUID",xmlAttributes,1,uidGenerator.getAnotherNewUID());
		addDicomAttributeFromXmlAttribute(list,TagFromName.StudyDate,"StudyDate",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.StudyTime,"StudyTime",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.StudyID,"StudyID",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.ReferringPhysicianName,"ReferPhysician",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.StudyDescription,"StudyDescription",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.AccessionNumber,"AccessionNumber",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.NameOfPhysiciansReadingStudy,"ReadingPhysician",xmlAttributes,3,null);
		return list;
	}

	protected AttributeList makeAttributeListForSeries(NamedNodeMap xmlAttributes) throws DicomException {
		AttributeList list = new AttributeList();
		addDicomAttributeFromXmlAttribute(list,TagFromName.SeriesInstanceUID,"SeriesInstanceUID",xmlAttributes,1,uidGenerator.getAnotherNewUID());
		addDicomAttributeFromXmlAttribute(list,TagFromName.SeriesNumber,"SeriesNumber",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.SeriesDate,"SeriesDate",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.SeriesTime,"SeriesTime",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.Modality,"Modality",xmlAttributes,1,"OT");
		addDicomAttributeFromXmlAttribute(list,TagFromName.StationName,"StationName",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.OperatorsName,"OperatorName",xmlAttributes,3,null);						// NB. different DICOM tag from XML attribute name
		addDicomAttributeFromXmlAttribute(list,TagFromName.SeriesDescription,"SeriesDescription",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PatientPosition,"PatientPosition",xmlAttributes,3,null);					// should be conditional :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.PositionReferenceIndicator,"PositionReference",xmlAttributes,2,null);	// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.FrameOfReferenceUID,"FrameOfRefUID",xmlAttributes,3,null);				// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.InstitutionName,"InstitutionName",xmlAttributes,3,null);
		{ Attribute a = newAttribute(TagFromName.Manufacturer); list.put(a); }														// type 2, but we have no information :(
		return list;
	}

	protected AttributeList makeAttributeListForImage(NamedNodeMap xmlAttributes) throws DicomException {
		AttributeList list = new AttributeList();
		addDicomAttributeFromXmlAttribute(list,TagFromName.SOPInstanceUID,"SOPInstanceUID",xmlAttributes,1,uidGenerator.getAnotherNewUID());
		addDicomAttributeFromXmlAttribute(list,TagFromName.SOPClassUID,"SOPClassUID",xmlAttributes,1,SOPClass.SecondaryCaptureImageStorage);
		addDicomAttributeFromXmlAttribute(list,TagFromName.ImageType,"ImageType",xmlAttributes,1,"DERIVED\\SECONDARY");
		addDicomAttributeFromXmlAttribute(list,TagFromName.ContrastBolusAgent,"ContrastBolus",xmlAttributes,3,null);				// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.InstanceNumber,"ImageNumber",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.ContentDate,"ImageDate",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.ContentTime,"ImageTime",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.SliceThickness,"SliceThickness",xmlAttributes,3,null);					// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.KVP,"KVP",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.RepetitionTime,"RepetitionTime",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.EchoTime,"EchoTime",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.EchoNumbers,"EchoNumbers",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.GantryDetectorTilt,"GantryDetector",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.XRayTubeCurrent,"XrayTubeCurrent",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.ImagePositionPatient,"ImagePositionPt",xmlAttributes,3,null);			// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.ImageOrientationPatient,"ImageOrientPt",xmlAttributes,3,null);			// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.SliceLocation,"SliceLocation",xmlAttributes,3,null);						// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.PixelSpacing,"PixelSpacing",xmlAttributes,3,null);						// should be conditional on Module inclusion :(
		addDicomAttributeFromXmlAttribute(list,TagFromName.ImagerPixelSpacing,"ImagerPixelSpacing",xmlAttributes,3,null);			// should be conditional on Module inclusion :(
		
		// these have not been seen in actual Amicas files, but rather than add them as always empty, take a shot in case encountered in future ...

		addDicomAttributeFromXmlAttribute(list,TagFromName.AcquisitionNumber,"AcquisitionNumber",xmlAttributes,2,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.Laterality,"Laterality",xmlAttributes,2,null);
		
		if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID).equals(SOPClass.MRImageStorage)) {
			addDicomAttributeFromXmlAttribute(list,TagFromName.ScanningSequence,"ScanningSequence",xmlAttributes,1,"RM");			// since we don't know, fudge it with "research mode"
			addDicomAttributeFromXmlAttribute(list,TagFromName.SequenceVariant,"SequenceVariant",xmlAttributes,1,"NONE");
			addDicomAttributeFromXmlAttribute(list,TagFromName.ScanOptions,"ScanOptions",xmlAttributes,2,null);
			addDicomAttributeFromXmlAttribute(list,TagFromName.MRAcquisitionType,"MRAcquisitionType",xmlAttributes,2,null);
			addDicomAttributeFromXmlAttribute(list,TagFromName.EchoTrainLength,"EchoTrainLength",xmlAttributes,2,null);
		}

		addDicomAttributeFromXmlAttribute(list,TagFromName.Rows,"NRows",xmlAttributes,1,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.Columns,"NColumns",xmlAttributes,1,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PhotometricInterpretation,"PhotometricI",xmlAttributes,1,"MONOCHROME2");
		addDicomAttributeFromXmlAttribute(list,TagFromName.BitsAllocated,"BitsAlloc",xmlAttributes,1,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.BitsStored,"BitsStored",xmlAttributes,1,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.PixelRepresentation,"PixelRep",xmlAttributes,1,"0");

		addDicomAttributeFromXmlAttribute(list,TagFromName.NumberOfFrames,"NumberOfFrames",xmlAttributes,3,null);					// should not add if 1 for some IODs :(
		
		addDicomAttributeFromXmlAttribute(list,TagFromName.RescaleIntercept,"RescaleIntercept",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.RescaleSlope,"RescaleSlope",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.WindowWidth,"WindowWidth",xmlAttributes,3,null);
		addDicomAttributeFromXmlAttribute(list,TagFromName.WindowCenter,"WindowCenter",xmlAttributes,3,null);
		
		addDicomAttributeFromXmlAttribute(list,TagFromName.LossyImageCompressionRatio,"LossyCompression",xmlAttributes,3,null);
		String lossyImageCompressionRatio = Attribute.getSingleStringValueOrNull(list,TagFromName.LossyImageCompressionRatio);
		if (lossyImageCompressionRatio != null && lossyImageCompressionRatio.length() > 0) {
			{ Attribute a = newAttribute(TagFromName.LossyImageCompression); a.addValue("01"); list.put(a); }
			String compressionType = getNamedAttributeValue(xmlAttributes,"CompressionType");
			if (compressionType != null && compressionType.length() > 0) {
				if (compressionType.equals("JPEG2000")) {
					compressionType="ISO_15444_1";		// the DICOM defined term
				}
				{ Attribute a = newAttribute(TagFromName.LossyImageCompressionMethod); a.addValue(compressionType); list.put(a); }
			}
		}
		
		String pixelPaddingFlag = getNamedAttributeValue(xmlAttributes,"PixelPaddingFlag");
		if (pixelPaddingFlag != null && !pixelPaddingFlag.equals("0")) {
			addDicomAttributeFromXmlAttribute(list,TagFromName.PixelPaddingValue,"PixelPadding",xmlAttributes,3,null);
		}

		return list;
	}

	protected static SpecificCharacterSet setSpecificCharacterSetFromDocumentEncoding(Document document) {
		String documentEncoding = document.getXmlEncoding();
//System.err.println("Document encoding = "+documentEncoding);
		SpecificCharacterSet specificCharacterSet = null;
		if (documentEncoding != null) {
			if (documentEncoding.equals("US-ASCII")) {
				String[] equivalentDicomSpecificCharacterSet = null;
				specificCharacterSet = new SpecificCharacterSet(equivalentDicomSpecificCharacterSet);
			}
		}
		if (specificCharacterSet == null) {
			// default to UTF-8
			String[] equivalentDicomSpecificCharacterSet = { "ISO_IR 192" };
			specificCharacterSet = new SpecificCharacterSet(equivalentDicomSpecificCharacterSet);
		}
//System.err.println("Using SpecificCharacterSet = "+specificCharacterSet);
		return specificCharacterSet;
	}
	
	protected void createDicomImageFileFromAmicasImageFile(File amicasFolder,String amicasFileName,String dicomOutputFolder,String storingAETitle,
			AttributeList patientList,AttributeList studyList,AttributeList seriesList,AttributeList imageList) throws IOException, DicomException {
		if (!amicasFileName.endsWith(".demo")) {
			throw new DicomException("Amicas conversion not supported for this file extension (only .demo) "+amicasFileName);
		}
		File fullInputFilePath = new File(amicasFolder,amicasFileName.trim().replace('\\',File.separatorChar).replace('/',File.separatorChar).replaceFirst("[.]demo$",".jp2"));
		if (!fullInputFilePath.exists()) {
			throw new DicomException("Missing file referenced from index "+amicasFileName);
		}
//System.err.println("createDicomImageFileFromAmicasImageFile(): fullInputFilePath = "+fullInputFilePath);
		ImageToDicom.generateDICOMPixelModuleFromConsumerImageFile(fullInputFilePath,imageList);
//System.err.println("createDicomImageFileFromAmicasImageFile(): imageList after reading image file = "+imageList.toString(dictionary));
		imageList.putAll(patientList);
		imageList.putAll(studyList);
		imageList.putAll(seriesList);
		{
			String valueToUseInSpecificCharacterSetAttribute = specificCharacterSet.getValueToUseInSpecificCharacterSetAttribute();
//System.err.println("valueToUseInSpecificCharacterSetAttribute = "+valueToUseInSpecificCharacterSetAttribute);
			if (valueToUseInSpecificCharacterSetAttribute != null && valueToUseInSpecificCharacterSetAttribute.length() > 0) {
				Attribute a = newAttribute(TagFromName.SpecificCharacterSet); a.addValue(valueToUseInSpecificCharacterSetAttribute); imageList.put(a);
			}
		}
		FileMetaInformation.addFileMetaInformation(imageList,TransferSyntax.ExplicitVRLittleEndian,storingAETitle);
		File outputFullPath = new File(dicomOutputFolder,Attribute.getSingleStringValueOrNull(imageList,TagFromName.SOPInstanceUID)+".dcm");
pwlogger.println("Converting \""+fullInputFilePath+"\" -> \""+outputFullPath+"\"");
pwlogger.flush();
		imageList.write(outputFullPath,TransferSyntax.ExplicitVRLittleEndian,true,true);
	}

	/**
	 * <p>Read an amicas-patients folder, and then convert any image files within.</p>
	 *
	 * @param	pathName			the path name to an amicas-patients folder or folder containing an amicas-patients folder
	 * @param	dicomOutputFolder	the path name to where to write the DICOM files
	 */
	public void convertAmicasFiles(String pathName,String dicomOutputFolder) throws IOException, DicomException, ParserConfigurationException, SAXException {
pwlogger.println("Looking for amicas-patients folder.");
pwlogger.flush();
		if (pathName != null) {
			File path = new File(pathName);
			File amicasPatientsFolder = null;		// look for amicas-patients here or in root folder of here, with various case permutations
			if (path != null && path.exists()) {
				if (path.isDirectory() && path.getName().toUpperCase(java.util.Locale.US).equals("AMICAS-PATIENTS")) {
					amicasPatientsFolder=path;
				}
				else if (path.isDirectory()) {
					File tryFolder = new File(path,"AMICAS-PATIENTS");
					if (tryFolder != null && tryFolder.exists() && tryFolder.isDirectory()) {
						amicasPatientsFolder=tryFolder;
					}
					else {
						tryFolder = new File(path,"Amicas-patients");
						if (tryFolder != null && tryFolder.exists() && tryFolder.isDirectory()) {
							amicasPatientsFolder=tryFolder;
						}
						else {
							tryFolder = new File(path,"amicas-patients");
							if (tryFolder != null && tryFolder.exists() && tryFolder.isDirectory()) {
								amicasPatientsFolder=tryFolder;
							}
							// else give up
						}
					}
				}
			}
			if (amicasPatientsFolder == null) {
pwlogger.println("No amicas-patients folder - nothing to do.");
pwlogger.flush();
			}
			else {
pwlogger.println("Searching for patients.");
pwlogger.flush();
//System.err.println("Found amicas-patients folder at: "+amicasPatientsFolder);
				File[] patientFolders = amicasPatientsFolder.listFiles(onlyDirectoriesFileFilter);
				for (File patientFolder: patientFolders) {
//System.err.println("Found patient folder at: "+patientFolder);
					File[] xmlFiles = patientFolder.listFiles(onlyXMLFilenameFilter);
					for (File xmlFile : xmlFiles) {
//System.err.println("Found patient xmlFile at: "+xmlFile);
						FileInputStream fis = new FileInputStream(xmlFile);
						Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
						fis.close();
//System.err.println("Document node name = "+document.getNodeName());
						specificCharacterSet = setSpecificCharacterSetFromDocumentEncoding(document);
						Node demographicStudy = document.getFirstChild();
						while (demographicStudy != null) {
							String nodeName = demographicStudy.getNodeName();
							if (nodeName != null && nodeName.equals("DemographicStudy")) {
								break;
							}
							demographicStudy = demographicStudy.getNextSibling();
						}
						if (demographicStudy != null) {
//System.err.println("Got DemographicStudy node");
							AttributeList patientList = null;
							AttributeList studyList = null;
							AttributeList seriesList = null;
							AttributeList imageList = null;
							Node demographicEntry = demographicStudy.getFirstChild();
							while (demographicEntry != null) {
								String nodeName = demographicEntry.getNodeName();
								if (nodeName != null) {
									NamedNodeMap xmlAttributes = demographicEntry.getAttributes();
									if (xmlAttributes != null) {
										if (nodeName.equals("Demographic_AmicasPatient")) {
//System.err.println("Got Demographic_AmicasPatient node");
											patientList = makeAttributeListForPatient(xmlAttributes);
//System.err.println("patientList = "+patientList.toString(dictionary));
										}
										else if (nodeName.equals("Demographic_AmicasStudy")) {
//System.err.println("Got Demographic_AmicasStudy node");
											studyList = makeAttributeListForStudy(xmlAttributes);
//System.err.println("studyList = "+studyList.toString(dictionary));
										}
										else if (nodeName.equals("Demographic_AmicasSeries")) {
//System.err.println("Got Demographic_AmicasSeries node");
											seriesList = makeAttributeListForSeries(xmlAttributes);
//System.err.println("seriesList = "+seriesList.toString(dictionary));
										}
										else if (nodeName.equals("Demographic_AmicasImage")) {
//System.err.println("Got Demographic_AmicasImage node");
											imageList = makeAttributeListForImage(xmlAttributes);
//System.err.println("imageList = "+imageList.toString(dictionary));
											String objectFile = getNamedAttributeValue(xmlAttributes,"ObjectFile");
//System.err.println("objectFile = "+objectFile);
											String storingAETitle = getNamedAttributeValue(xmlAttributes,"StoringAETitle");
//System.err.println("storingAETitle = "+storingAETitle);
											try {
												createDicomImageFileFromAmicasImageFile(amicasPatientsFolder,objectFile,dicomOutputFolder,storingAETitle,patientList,studyList,seriesList,imageList);
											}
											catch (Exception e) {
												pwlogger.println(e);
											}
										}
									}
								}
								demographicEntry = demographicEntry.getNextSibling();
							}
						}
					}
				}
			}
		}
pwlogger.println("Done.");
pwlogger.flush();
	}
		
	/**
	 * <p>Convert a set of Amicas JPEG 2000 files to DICOM.</p>
	 *
	 * @param	arg	array of two strings - the path to the media or folder containing
	 * the files to convert, and the path to the folder to stored the converted images,
	 * or else will pop up file chooser dialogs to select these and create a frame to show the progress.
	 */
	public static void main(String arg[]) {
		ImageIO.scanForPlugins();
		try {
			if (arg.length == 0) {
				ConvertAmicasJPEG2000FilesetToDicom converter = new ConvertAmicasJPEG2000FilesetToDicom("/");
				JFrame logFrame = new JFrame("Convert Amicas JPEG 2000 to DICOM");
				logFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				converter.setLogger(logFrame,600,400);
				converter.choosePathsAndConvertAmicasFiles();
			}
			else if (arg.length == 2) {
				ConvertAmicasJPEG2000FilesetToDicom converter = new ConvertAmicasJPEG2000FilesetToDicom();
				converter.convertAmicasFiles(arg[0],arg[1]);
			}
			else {
				throw new Exception("Argument list must be zero or two values");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}



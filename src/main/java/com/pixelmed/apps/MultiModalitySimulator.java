/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;

import com.pixelmed.dicom.*;

import com.pixelmed.network.MultipleInstanceTransferStatusHandlerWithFileName;
import com.pixelmed.network.StorageSOPClassSCU;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class implements a multi-modality simulator that takes a database of
 * existing studies to provide a source of sample images and DICOM attributes, and
 * for each modality within the database, generates random new patients
 * and studies at random intervals using the current date and time.</p>
 *
 * <p>For example:</p>
 * <pre>
try {
    new MultiModalitySimulator("theirhost",11112,"STORESCP","STORESCU","database");
}
catch (Exception e) {
    slf4jlogger.error("",e);
}
 * </pre>
 *
 * <p>From the command line:</p>
 * <pre>
java -cp pixelmed.jar:lib/additional/commons-codec-1.3.jar:lib/additional/commons-compress-1.12.jar com.pixelmed.network.MultiModalitySimulator theirhost 11112 STORESCP database
 * </pre>
 *
 * <p>The database is a persistent (disk) instance of the {@link com.pixelmed.database.DatabaseInformationModel DatabaseInformationModel},
 * such as might be created by the {@link com.pixelmed.database.RebuildDatabaseFromInstanceFiles RebuildDatabaseFromInstanceFiles} class
 * from a set of files, or have been created by an application storing received images in such a databaase.</p>
 *
 * <p>The Calling AE Title used to send the images for each simulated modality is the modality string value, e.g. "CT" for images with a DICOM Atttibute Modality (0008,0060) of "CT".</p>
 *
 * <p>The list of modalities simulated is currently limited to CT, MR, DX, CR, US, NM, XA, if present in the supplied database. This can be changed in a subclass by overriding the protected variable {@link #modalities modalities},
 * as can the corresponding protected variable {@link #sleepIntervalForModalityInMinutes sleepIntervalForModalityInMinutes}, which needs to contain a matching number of entries.</p>
 *
 * <p>A relatively short and contrived list of patient names is used by default, and can be overridden in a subclass by changing {@link #patientNames patientNames}.</p>
 *
 * @author	dclunie
 */
public class MultiModalitySimulator {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/MultiModalitySimulator.java,v 1.10 2017/01/24 10:50:33 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(MultiModalitySimulator.class);
	
	protected static String[] modalities                     = { "CT", "MR", "DX", "CR", "US", "NM", "XA" };
	protected static int[] sleepIntervalForModalityInMinutes = {  1,    8,    1,    1,    4,    15,   30 };
	protected Map<String,Integer> sleepIntervalForModality = new HashMap<String,Integer>();
	{
		for (int i=0; i<modalities.length; ++i) {
			sleepIntervalForModality.put(modalities[i],new Integer(sleepIntervalForModalityInMinutes[i]));
		}
	}
	protected int getSleepIntervalForModalityInMilliseconds(String modality) { return sleepIntervalForModality.get(modality).intValue() * 60 * 1000; }

	protected static long accessionNumberCounter =  System.currentTimeMillis();
	
	// http://civilization.wikia.com/wiki/List_of_historical_figures_in_Civilization_IV
	protected String[] patientNames = {
		"Moses",
		"Mahavira",
		"Zoroaster",
		"Ananda",
		"Chuang-Tzu",
		"Mencius",
		"Tzu^Mo",
		"^John^^St.",
		"^Peter^^St.",
		"^Paul^^St.",
		"Akiva^^^Rabbi",
		"Mani",
		"^Augustine^^St.",
		"^Patrick^^St.",
		"Bakr^Abu",
		"Shankara",
		"Daishi^Kōbō",
		"Atisha^",
		"Aquinas^Thomas^^St.",
		"Shah^Mohammed",
		"Tsongkhapa",
		"d'Arc^Jeanne",
		"Nanak^",
		"Sultan^Tipu",
		"Ramakrishna^",
		"Guru^Narayana",
		"Truth^Sojourner",
		"Adamastor of Oldham",
		"Homer",
		"Thespis",
		"Lun^Ling",
		"Xizhi^Wang",
		"Valmiki^",
		"Virgil^",
		"Kalidas^",
		"Po^Li",
		"Fu^Du",
		"al-Din Rumi^Jalal",
		"Alighieri^Dante",
		"Emre^Yunus",
		"Amir Khusro",
		"Ibn Muqlah",
		"Michaelangelo",
		"Raphael",
		"Shakespeare^William",
		"de Cervantes^Miguel",
		"van Rijn^Rembrandt",
		"Vermeer^Johannes",
		"Bach^J. S.",
		"Mozart^Wolfgang^Amadeus",
		"von Goethe^Johann^Wolfgang",
		"van Beethoven^Ludwig",
		"Hugo^Victor",
		"van Gogh^Vincent",
		"Brahms^Johannes",
		"Dvořák^Antonín",
		"Twain^Mark",
		"Monet^Claude",
		"Conrad^Joseph",
		"Kafka^Franz",
		"Armstrong^Louis",
		"Ellington^Duke",
		"Picasso^Pablo",
		"Davis^Miles",
		"Hendrix^Jimi",
		"Presley^Elvis",
		"Ptah^Merit",
		"Shi^Xi Ling",
		"Nabu-rimanni",
		"Socrates",
		"Plato",
		"Aristotle",
		"Euclid",
		"Ptolemy",
		"Hypatia",
		"Zu Chongzhi",
		"Aryabhata",
		"Al-Kindi",
		"Al-Khwarizmi",
		"Al-Razi",
		"Alhazen",
		"Copernicus^Nicolaus",
		"Bacon^Francis",
		"Brahe^Tycho",
		"Kepler^Johannes",
		"Newton^Isaac",
		"Galilei^Galileo",
		"Descartes^René",
		"van Leeuwenhoek^Antony",
		"Leibniz^Gottfried",
		"Lomonosov^Mikhail",
		"Lavoisier^Antoine^Laurent",
		"Gauss^Carl^Friedrich",
		"Dalton^John",
		"Faraday^Michael",
		"Maxwell^James^Clerk",
		"Pasteur^Louis",
		"Darwin^Charles",
		"Rutherford^Ernest",
		"Curie^Marie",
		"Einstein^Albert",
		"Bohr^Niels",
		"Heisenberg^Werner",
		"Fermi^Enrico",
		"Franklin^Rosalind",
		"Sakharov^Andrei",
		"Harkuf",
		"Hanno",
		"Pytheas",
		"Qian^Zhang",
		"Aretas^^^^III",
		"Erickson^Leif",
		"Anshi^Wang",
		"Dandolo^Enrico",
		"Polo^Marco",
		"Ibn Battuta",
		"Whittington^Richard",
		"de Medici^Giovanni",
		"He^Zheng",
		"da Gama^Vasco",
		"Columbus^Christopher",
		"Magellan^Ferdinand",
		"Cartier^Jacques",
		"Raja Todar Mal",
		"van Diemen^Anthony",
		"Roe^Thomas^^Sir",
		"Shah Jahan",
		"Smith^Adam",
		"Cook^James",
		"Vanderbilt^Cornelius",
		"Mackenzie^Alexander^^Sir",
		"Mill^John^Stuart",
		"Carnegie^Andrew",
		"Rockefeller^John^D",
		"Keynes^John^Maynard",
		"Chanel^Coco",
		"Imhotep",
		"Archimedes",
		"Heron",
		"Lun^Cai",
		"Heng^Zhang",
		"Sheng^Bi",
		"da Vinci^Leonardo",
		"Sinan",
		"Schickard^Wilhelm",
		"Pascal^Blaise",
		"Franklin^Benjamin",
		"de Coulomb^Charles^Augustin",
		"Watt^James",
		"Fulton^Robert",
		"Jacquard^Joseph^Marie",
		"Brunel^Isambard^Kingdom",
		"Morton^William^TG",
		"Daguerre^Louis",
		"de Lesseps^Ferdinand",
		"Roebling^John",
		"Rillieux^Norbert",
		"Bessemer^Henry",
		"Singh^Nain",
		"Bell^Alexander^Graham",
		"Otto^Nikolaus^August",
		"Daimler^Gottlieb",
		"Tesla^Nikola",
		"Edison^Thomas",
		"Marconi^Guglielmo",
		"Eiffel^Alexandre^Gustave",
		"Goethals^George^Washington",
		"Ford^Henry",
		"Wright^Wilbur",
		"Wright^Orville",
		"Sargon",
		"Nebuchadnezzar^^^^II",
		"Tzu^Sun",
		"Leonidas",
		"Lysander",
		"Maurya^Chandragupta",
		"Barca^Hamilcar",
		"Marius^Gaius",
		"Africanus^Scipio",
		"Vercingetorix",
		"Arminius",
		"Boudica",
		"Cao^Cao",
		"Liang^Zhuge",
		"Belisarius",
		"ibn al-Walid^Khalid",
		"Martel^Charles",
		"Charlemagne",
		"Jayavarman^^^II",
		"El Cid",
		"William the Conqueror",
		"Subutai",
		"Timur",
		"Pachacuti",
		"Auitzotl",
		"Cortés^Hernan",
		"Pizzaro^Francisco",
		"Ivan the Terrible",
		"Akbar^Jalaluddin^Muhammad",
		"Gustavus II Adolphus",
		"de Ruyter^Michiel ",
		"Cromwell^Oliver",
		"Eugene of Savoy",
		"Nelson^Horatio",
		"Lee^Robert",
		"Geronimo",
		"Dewey^George",
		"Yamamoto^Isoroku",
		"Patton^George^Simpson",
		"Montgomery^Bernard",
		"MacArthur^Douglas",
		"Guderian^Heinz",
		"Rommel^Erwin",
		"Zhukov^Georgy",
		"Guevara^Che",
		"Pebekkamen",
		"Ephialtes",
		"Ke^Jing",
		"Wuyang^Qin",
		"Calippus",
		"Alberti^Leone",
		"Barlow^John",
		"Walsingham^Francis",
		"Hanzo^Hattori",
		"Gerard^Balthasar",
		"Babington^Anthony",
		"Goemon^Ishikawa",
		"Graziani^Gaspar",
		"Fawkes^Guy",
		"Honeyman^John",
		"Hale^Nathan",
		"Corday^Charlotte",
		"Casanova^Giacomo",
		"Pinkerton^Allan",
		"Melville^William",
		"Boyd^Belle",
		"Reilly^Sidney",
		"Jung-Geun^An",
		"Dansey^Claude",
		"Hari^Mata",
		"Hoover^J^Edgar",
		"Berg^Moe",
		"Rosenberg^Ethel",
		"Rosenberg^Julius",
		"von Stauffenberg^Claus",
		"Spaak^Suzanne",
		"Donovan^William",
		"Griph^Viktor",
		"Matise^Joe",
		"Cole^Daniel",
		"Swiss^Mark",
		"Foshaug the Deceiver",
		"Speaker the Lionhearted",
		"Herodotus",
		"Thucydides",
		"Pliny",
		"^Augustine^^St.",
		"Gibbon",
		"Toynbee",
		"McCauley^^^Lord",
		"Livy",
		"Tacitus",
		"The Venerable Bede",
		"Machiavelli"
	};
	
	protected String seriesLocalParentReferenceColumnName;
	protected String localFileNameColumnName;
	protected String modalityColumnName;
	//protected String sopClassUIDColumnName;
	//protected String sopInstanceUIDColumnName;
	//protected String transferSyntaxUIDColumnName;
	
	protected DatabaseInformationModel databaseInformationModel;
	
	protected String hostname;
	protected int port;
	protected String calledAETitle;

	protected class OurMultipleInstanceTransferStatusHandler extends MultipleInstanceTransferStatusHandlerWithFileName {
		public void updateStatus(int nRemaining,int nCompleted,int nFailed,int nWarning,String sopInstanceUID,String fileName,boolean success) {
			slf4jlogger.trace("Remaining {}, completed {}, failed {}, warning {}",nRemaining,nCompleted,nFailed,nWarning);
			if (success) {
				slf4jlogger.trace("Sent {}",fileName);
			}
			else {
				 slf4jlogger.info("Failed to send {}",fileName);
			}
		}
	}

	protected void findFilesToSend(InformationEntity ie,String localPrimaryKeyValue,List<String> dicomFiles) throws DicomException {
		slf4jlogger.trace("findFilesToSend(): checking {} {}",ie,localPrimaryKeyValue);
		if (ie == InformationEntity.INSTANCE) {
			Map<String,String> record = databaseInformationModel.findAllAttributeValuesForSelectedRecord(InformationEntity.INSTANCE,localPrimaryKeyValue);
			localFileNameColumnName = databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE);
			String fileName = record.get(localFileNameColumnName);
			dicomFiles.add(fileName);
			slf4jlogger.trace("findFilesToSend(): added file = {}",fileName);
		}
		else {
			InformationEntity childIE = databaseInformationModel.getChildTypeForParent(ie);
			slf4jlogger.trace("findFilesToSend(): childIE is {}",childIE);
			List<Map<String,String>> returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(childIE,localPrimaryKeyValue);
			// if necessary, iterate for next lower down type of child IE (e.g. we may be skipping over concatenation from series to instance)
			while (childIE != null && returnedRecords == null || returnedRecords.size() == 0) {
				childIE = databaseInformationModel.getChildTypeForParent(childIE);
				slf4jlogger.trace("findFilesToSend(): empty so descending to next childIE {}",childIE);
				returnedRecords = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(childIE,localPrimaryKeyValue);
			}
			if (returnedRecords != null) {
				for (Map<String,String> record : returnedRecords) {
					String childLocalPrimaryKeyValue = record.get(databaseInformationModel.getLocalPrimaryKeyColumnName(childIE));
					findFilesToSend(childIE,childLocalPrimaryKeyValue,dicomFiles);	// recurse ...
				}
			}
		}
	}
	
	protected SetOfDicomFiles generateSyntheticStudyFromOriginal(List<String> originalDicomFileNames,String modality,String aeTitleForMetaInformation,String patientName,String patientID,String studyID,String accessionNumber) throws DicomException, IOException {
			slf4jlogger.debug("generateSyntheticStudyFromOriginal(): generating modality={} patientName={} patientID={} studyID={} accessionNumber={}",modality,patientName,patientID,studyID,accessionNumber);
	
		ClinicalTrialsAttributes.flushMapOfUIDs();	// very important ... necessary to prevent same UIDs being reallocated if original study is reused more than once, as it may be
		
		SetOfDicomFiles syntheticDicomFiles = new SetOfDicomFiles();
		if (originalDicomFileNames != null) {
			for (String originalDicomFileName : originalDicomFileNames) {
				if (originalDicomFileName != null) {
					slf4jlogger.trace("generateSyntheticStudyFromOriginal(): doing file {}",originalDicomFileName);
					File file = new File(originalDicomFileName);
					DicomInputStream i = new DicomInputStream(file);
					AttributeList list = new AttributeList();
					list.read(i);
					i.close();

					list.removeGroupLengthAttributes();
					list.correctDecompressedImagePixelModule();
					list.insertLossyImageCompressionHistoryIfDecompressed();
					list.removeMetaInformationHeaderAttributes();
					
					ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
					ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,
						ClinicalTrialsAttributes.HandleUIDs.remap,	// i.e., will maintain internal referential intergity, but be new, as long as ClinicalTrialsAttributes.flushMapOfUIDs() was called first
						true,/*keepDescriptors*/
						true,/*keepSeriesDescriptors*/
						false,/*keepPatientCharacteristics*/
						true,/*keepDeviceIdentity*/
						false/*keepInstitutionIdentity*/
					);
					
					list.remove(TagFromName.DeidentificationMethod);
					list.remove(TagFromName.DeidentificationMethodCodeSequence);
					list.remove(TagFromName.PatientIdentityRemoved);
	 
					{ Attribute a = new PersonNameAttribute(TagFromName.PatientName); a.addValue(patientName); list.put(a); }
					{ Attribute a = new LongStringAttribute(TagFromName.PatientID); a.addValue(patientID); list.put(a); }
					
					// null out characteristics since won't be consistent with name and ID
					{ Attribute a = new DateAttribute(TagFromName.PatientBirthDate); list.put(a); }
					{ Attribute a = new CodeStringAttribute(TagFromName.PatientSex); list.put(a); }
					{ Attribute a = new AgeStringAttribute(TagFromName.PatientAge);  list.put(a); }
					// leave weight and height since may be useful even if vary over time for same synthetic patient
					//{ Attribute a = new DecimalStringAttribute(TagFromName.PatientWeight); list.put(a); }
					//{ Attribute a = new DecimalStringAttribute(TagFromName.PatientSize);  list.put(a); }
					
					{ Attribute a = new ShortStringAttribute(TagFromName.StudyID); a.addValue(studyID); list.put(a); }
					{ Attribute a = new PersonNameAttribute(TagFromName.ReferringPhysicianName); a.addValue("^^^^"); list.put(a); }
					{ Attribute a = new ShortStringAttribute(TagFromName.AccessionNumber); a.addValue(accessionNumber); list.put(a); }
							
					{ Attribute a = new LongStringAttribute(TagFromName.InstitutionName); a.addValue("St. Elsewhere's"); list.put(a); }
							
					{
						java.util.Date currentDateTime = new java.util.Date();
						String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime);
						String currentTime = new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime);
						{ Attribute a = new DateAttribute(TagFromName.StudyDate);						a.addValue(currentDate); list.put(a); }
						{ Attribute a = new TimeAttribute(TagFromName.StudyTime);						a.addValue(currentTime); list.put(a); }
						{ Attribute a = new DateAttribute(TagFromName.SeriesDate);						a.addValue(currentDate); list.put(a); }
						{ Attribute a = new TimeAttribute(TagFromName.SeriesTime);						a.addValue(currentTime); list.put(a); }
						{ Attribute a = new DateAttribute(TagFromName.ContentDate);						a.addValue(currentDate); list.put(a); }
						{ Attribute a = new TimeAttribute(TagFromName.ContentTime);						a.addValue(currentTime); list.put(a); }
						{ Attribute a = new DateAttribute(TagFromName.AcquisitionDate);					a.addValue(currentDate); list.put(a); }
						{ Attribute a = new TimeAttribute(TagFromName.AcquisitionTime);					a.addValue(currentTime); list.put(a); }
						{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate);			a.addValue(currentDate); list.put(a); }
						{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime);			a.addValue(currentTime); list.put(a); }
						{ Attribute a = new ShortStringAttribute(TagFromName.TimezoneOffsetFromUTC);	a.addValue(DateTimeAttribute.getTimeZone(java.util.TimeZone.getDefault(),currentDateTime)); list.put(a); }
					}
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }

					list.removeUnsafePrivateAttributes();

					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,aeTitleForMetaInformation);
					list.insertSuitableSpecificCharacterSetForAllStringValues();	// E.g., may have de-identified Kanji name and need new character set

					File syntheticFile = File.createTempFile("synth",".dcm");
					syntheticFile.deleteOnExit();
					list.write(syntheticFile);
					String syntheticFileName = syntheticFile.getCanonicalPath();
					syntheticDicomFiles.add(syntheticFileName,
						Attribute.getSingleStringValueOrNull(list,TagFromName.SOPClassUID),
						Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID),
						Attribute.getSingleStringValueOrNull(list,TagFromName.TransferSyntaxUID));
						slf4jlogger.trace("generateSyntheticStudyFromOriginal(): synthetic file {}",syntheticFileName);
				}
			}
		}
		return syntheticDicomFiles;
	}
	
	protected static void deleteFiles(SetOfDicomFiles dicomFiles) {
		Iterator i = dicomFiles.iterator();
		while (i.hasNext()) {
			SetOfDicomFiles.DicomFile dicomFile = (SetOfDicomFiles.DicomFile)i.next();
			String fileName = dicomFile.getFileName();
			if (!new File(fileName).delete()) {
				// Do not throw exception at this point, since want to keep trying to delete the rest in the set
				slf4jlogger.error("Failed to delete file {}",fileName);
			}
		}
	}

	protected class SpecificModalitySimulator implements Runnable {
		String modality;
		String[] localPrimaryKeysOfAllStudiesOfThisModality;
		int numberOfStudies;
		int numberOfStudiesMinusOne;
		int sleepInterval;
		int studyIDCounter;
		
		SpecificModalitySimulator(String modality,Set<String> localPrimaryKeysOfAllStudiesOfThisModality) {
			this.modality = modality;
			numberOfStudies = localPrimaryKeysOfAllStudiesOfThisModality.size();
			this.localPrimaryKeysOfAllStudiesOfThisModality = localPrimaryKeysOfAllStudiesOfThisModality.toArray(new String[numberOfStudies]);
			numberOfStudiesMinusOne = numberOfStudies - 1;
			sleepInterval = getSleepIntervalForModalityInMilliseconds(modality);
			studyIDCounter = (int)(Math.random() * 10000);
		}
		
		public void run() {
			if (numberOfStudies > 0) {
				boolean interrupted = false;
				while (!interrupted) {
					int studySelected = (int)(Math.random() * numberOfStudiesMinusOne);
					slf4jlogger.trace("SpecificModalitySimulator(): Selected {} {}",modality,studySelected);
					String localPrimaryKeyOfSelectedStudy = localPrimaryKeysOfAllStudiesOfThisModality[studySelected];
					slf4jlogger.trace("SpecificModalitySimulator(): Selected {} {} {}",modality,studySelected,localPrimaryKeyOfSelectedStudy);
					List<String> originalFileNames = new ArrayList<String>();
					try {
						findFilesToSend(InformationEntity.STUDY,localPrimaryKeyOfSelectedStudy,originalFileNames);
						
						int patientSelection = (int)(Math.random()*(patientNames.length-1));
						String patientName = patientNames[patientSelection];
						String patientID = Integer.toString(263874+patientSelection);
						String studyID = Integer.toString(studyIDCounter++);
						String accessionNumber = Long.toString(accessionNumberCounter++);
						
						SetOfDicomFiles syntheticDicomFiles = generateSyntheticStudyFromOriginal(originalFileNames,modality,modality/*aeTitleForMetaInformation*/,patientName,patientID,studyID,accessionNumber);
						
						new StorageSOPClassSCU(hostname,port,calledAETitle,
							modality/*callingAETitle*/,
							syntheticDicomFiles,
							0/*compressionLevel*/,
							new OurMultipleInstanceTransferStatusHandler());
						deleteFiles(syntheticDicomFiles);
					}
					catch (Exception e) {
						slf4jlogger.error("",e);
					}
					try {
						Thread.currentThread().sleep(sleepInterval);
					}
					catch (InterruptedException e) {
						// shouldn't happen
						slf4jlogger.error("",e);
						interrupted = true;
					}
				}
			}
		}
	}

	/**
	 * <p>Simulate modalities sending to the specified AE.</p>
	 *
	 * @param	hostname			their hostname
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title,
	 * @param	databaseFileName	the source database file name
	 */
	public MultiModalitySimulator(String hostname,int port,String calledAETitle,String databaseFileName) throws DicomException {
		this.hostname = hostname;
		this.port = port;
		this.calledAETitle = calledAETitle;
		slf4jlogger.debug("Opening database ...");
		databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(databaseFileName);
		
		seriesLocalParentReferenceColumnName = databaseInformationModel.getLocalParentReferenceColumnName(InformationEntity.SERIES);
		localFileNameColumnName              = databaseInformationModel.getLocalFileNameColumnName(InformationEntity.INSTANCE);
		modalityColumnName                   = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.Modality);
		//sopClassUIDColumnName                = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPClassUID);
		//sopInstanceUIDColumnName             = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.SOPInstanceUID);
		//transferSyntaxUIDColumnName          = databaseInformationModel.getDatabaseColumnNameFromDicomTag(TagFromName.TransferSyntaxUID);
		
		slf4jlogger.debug("Building indexes of modality studies from database ...");
		Map<String,Set<String>> localPrimaryKeysOfStudiesByModality = new HashMap<String,Set<String>>();
		for (String modality : modalities) {
			Set<String> localPrimaryKeysOfAllStudiesOfThisModality = new HashSet<String>();
			localPrimaryKeysOfStudiesByModality.put(modality,localPrimaryKeysOfAllStudiesOfThisModality);
			List<Map<String,String>> allSeriesOfThisModality = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(InformationEntity.SERIES,modalityColumnName,modality);
			for (Map<String,String> series: allSeriesOfThisModality) {
				String seriesParentKey = series.get(seriesLocalParentReferenceColumnName);
				if (seriesParentKey != null && seriesParentKey.length() > 0) {	
					localPrimaryKeysOfAllStudiesOfThisModality.add(seriesParentKey);
				}
			}
		}
		slf4jlogger.debug("ready to begin simulation ...");
		for (String modality : modalities) {
			Set<String> localPrimaryKeysOfAllStudiesOfThisModality = localPrimaryKeysOfStudiesByModality.get(modality);
			if (localPrimaryKeysOfAllStudiesOfThisModality != null && localPrimaryKeysOfAllStudiesOfThisModality.size() > 0) {
				new Thread(new SpecificModalitySimulator(modality,localPrimaryKeysOfAllStudiesOfThisModality)).start();
			}
			else {
				slf4jlogger.debug("no {} studies",modality);
			}
		}
	}


	/**
	 * <p>Simulate modalities sending to the specified AE.</p>
	 *
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #MultiModalitySimulator(String,int,String,String)} instead.
	 * @param	hostname			their hostname
	 * @param	port				their port
	 * @param	calledAETitle		their AE Title,
	 * @param	databaseFileName	the source database file name
	 * @param	debugLevel			ignored
	 */
	public MultiModalitySimulator(String hostname,int port,String calledAETitle,String databaseFileName,int debugLevel) throws DicomException {
		this(hostname,port,calledAETitle,databaseFileName);
		slf4jlogger.warn("Debug level supplied in constructor ignored");
	}
	
	/**
	 * <p>Simulate modalities sending to the specified AE.</p>
	 *
	 * @param	arg	array of four strings - their hostname, their port, their AE Title,
	 *			the source database name
	 */
	public static void main(String arg[]) {
		try {
			String theirHost = null;
			int theirPort = -1;
			String theirAETitle = null;
			String databaseFileName = null;
	
			if (arg.length == 4) {
				theirHost=arg[0];
				theirPort=Integer.parseInt(arg[1]);
				theirAETitle=arg[2];
				databaseFileName=arg[3];
			}
			else {
				throw new Exception("Argument list must be 4 values");
			}
			new MultiModalitySimulator(theirHost,theirPort,theirAETitle,databaseFileName);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be used as a background service
			System.exit(0);
		}
	}
}


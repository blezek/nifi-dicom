/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.utils.StringUtilities;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>This class encapsulates information pertaining to anatomy of CT images.</p>
 * 
 * <p>Utility methods provide for the detection of anatomy from various header attributes regardless
 * of whether these are formal codes, code strings or free text comments.</p>
 * 
 * @author	dclunie
 */
public class CTAnatomy {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/CTAnatomy.java,v 1.20 2017/01/24 10:50:32 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CTAnatomy.class);
	
	protected static String[] newStringArray(String... values) { return values; }		// use 1.5 varargs feature; seems like a lot of trouble to work around lack of string array curly braces outside declarations

	protected static String[] badLateralityOrViewOrAnatomyPhraseTriggers = {
		"History",
		"Hx of"
	};
	

	
	protected static String[] badAnatomyWords = {
		"research",		// contains "ear"
		"and",			// expedient way to remove conjunction
		"head first",	// sometimes occurs in protocols
		"feet first",
		"entra di piedi",
		"axials",		// don't want LS to be confused as lumbar spine
		"sagittals",
		"coronals",
		"locator",		// else TOR matches chest
		"tracker"
	};

	protected static DisplayableAnatomicConcept[] anatomicConceptEntries = {
	// combined entries ...
	
	new DisplayableAnatomicConcept("C1508499","416949008",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB57",	"Abdomen and Pelvis",				"ABDOMENPELVIS",
			newStringArray(
				"Abdomen Pelvis",	// without conjunctions
				"Abdo Pelvis",		// various abbreviations
				"Abd Pelvis",
				"Abd Pelv",
				"Abd Pel",
				"AbdoPelv",
				"brzuch miednica"/*PL*/
			),
			newStringArray("Abdomen and Pelvis"),		newStringArray("Abdomen and Pelvis")),

	new DisplayableAnatomicConcept("C1442171","416550000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB55",	"Chest and Abdomen",				"CHESTABDOMEN",
			newStringArray(
				"Chest Abdomen",	// without conjunctions
				"Chest Abdo",		// various abbreviations
				"Chest Abd",
				"Thorax Abdomen",
				"Thorax Abdo",
				"Thorax Abd",
				"Chest Liver",		// not ideal match, but sometimes seem in protocols
				"Thorax Liver",
				"torace addome",
				"Klatka brzuch"/*PL*/
			),
			newStringArray("Chest and Abdomen"),		newStringArray("Chest and Abdomen")),

	new DisplayableAnatomicConcept("C1562547","416775004",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB56",	"Chest, Abdomen and Pelvis",		"CHESTABDPELVIS",
			newStringArray(
				"Chest Abdomen Pelvis",	// without conjunctions
				"Chest Abdo Pelvis",	// various abbreviations
				"Chest Abdo Pelv",
				"Chest Abdo Pel",
				"Chest Abd Pelvis",
				"Chest Abd Pelv",
				"Chest Abd Pel",
				"Chest AbdoPelv",
				"Chest Abdomen Pelv",
				"Chest Abdomen Pel",
				"Thorax Abdomen Pelvis",
				"Thorax Abdo Pelvis",
				"Thorax Abdo Pelv",
				"Thorax Abdo Pel",
				"Thorax Abd Pelvis",
				"Thorax Abd Pelv",
				"Thorax Abd Pel",
				"Thorax AbdoPelv",
				"Thorax Abdomen Pelv",
				"Thorax Abdomen Pel",
				"Thoraco Abdomino Pelvien",
				"Torax Abdomen Pelvis",
				"Th Abd Pel",
				"C A P",
				"CAP",
				"T A P",
				"TAP",
				"Klatka brzuch miednica"/*PL*/,
				""
			),
			newStringArray("Chest, Abdomen and Pelvis"),		newStringArray("Chest, Abdomen and Pelvis")),

	new DisplayableAnatomicConcept("C0460004","774007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1000",	"Head and Neck",					"HEADNECK",
			newStringArray(
				"Head Neck"	// without conjunctions
			),
			newStringArray("Head and Neck"),		newStringArray("Head and Neck")),

	new DisplayableAnatomicConcept("C1562459","417437006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB52",	"Neck and Chest",					"NECKCHEST",
			newStringArray(
				"Neck Chest",	// without conjunctions
				"Neck Thorax",
				"Collo Tor"
			),
			newStringArray("Neck and Chest"),		newStringArray("Neck and Chest")),

	new DisplayableAnatomicConcept("C1562378","416152001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB53",	"Neck, Chest and Abdomen",			"NECKCHESTABDOMEN",
			newStringArray(
				"Neck Chest Abdomen",	// without conjunctions
				"Neck Chest Abdo",	// various abbreviations
				"Neck Chest Abd",
				"Neck Thorax Abdomen",
				"Neck Thorax Abdo",
				"Neck Thorax Abd",
				"Collo Tor Addo"
			),
			newStringArray("Neck, Chest and Abdomen"),		newStringArray("Neck, Chest and Abdomen")),

	new DisplayableAnatomicConcept("C1562776","416319003",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"R-FAB54",	"Neck, Chest, Abdomen and Pelvis",	"NECKCHESTABDPELV",
			newStringArray(
				"Neck Chest Abdomen Pelvis",	// without conjunctions
				"Neck Chest Abdo Pelvis",		// various abbreviations
				"Neck Chest Abd Pelvis",
				"Neck Chest Abdo Pelv",
				"Neck Chest Abdo Pel",
				"Neck Chest Abd Pelv",
				"Neck Chest Abd Pel",
				"Neck Thorax Abdomen Pelvis",
				"Neck Thorax Abdo Pelvis",
				"Neck Thorax Abd Pelvis",
				"Neck Thorax Abdo Pelv",
				"Neck Thorax Abdo Pel",
				"Neck Thorax Abd Pelv",
				"Neck Thorax Abd Pel"
			),
			newStringArray("Neck, Chest, Abdomen and Pelvis"),		newStringArray("Neck, Chest, Abdomen and Pelvis")),
	
	new DisplayableAnatomicConcept("C1508520","LP33902-5",	false/*unpaired*/,	"LN",	null,	null,	"LP33902-5",	"Aortic Arch and Carotid Artery",				null,
			newStringArray(
				"Aortic Arch Carotid Artery",	// without conjunctions
				"Aortic Arch and Carotid Arteries",
				"Aortic Arch Carotid Arteries"
			),
			newStringArray("Aortic Arch and Carotid Artery"),		newStringArray("Aortic Arch and Carotid Artery")),


	// single part entries ...
		new DisplayableAnatomicConcept("C0000726","113345001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D4000",	"Abdomen",			"ABDOMEN",
			newStringArray(
				"Abdominal",
				"BØICHO"/*CZ*/,
				"bruco",/*CZ*/
				"Buik"/*NL*/,
				"Vatsa"/*FI*/,
				"Ventre"/*FR*/,
				"Addome"/*IT*/,
				"Abdome"/*PT*/,
				"はら"/*JP*/,
				"心窩部"/*JP*/,
				"胴"/*JP*/,
				"腹"/*JP*/,
				"腹部"/*JP*/,
				"ЖИВОТ"/*RU*/,
				"Buk"/*NL*/,
				"Pilvo"/*LT*/,
				"Addo",
				"brzuch"/*PL*/
			),
			newStringArray("Abdomen"),			newStringArray("Abdomen")),
		new DisplayableAnatomicConcept("C0001625","23451007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-B3000",	"Adrenal gland",	"ADRENAL",
				newStringArray("Adrenal"),
				newStringArray("Adrenal gland"),	newStringArray("Adrenal gland")),

		new DisplayableAnatomicConcept("C0042425","67109009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-64700",	"Ampulla of Vater",		null,			null,	newStringArray("Ampulla of Vater"),		newStringArray("Ampulla of Vater")),

		new DisplayableAnatomicConcept("C0003087","70258002",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15750",	"Ankle joint",		"ANKLE",
			newStringArray(
				"Ankle",
				"Tobillo"/*ES*/,
				"Knöchel"/*DE*/,
				"Enkel"/*NL*/,
				"Cheville"/*FR*/,
				"Tornozelo"/*PT*/,
				"αστράγαλος"/*GR*/,
				"足首"/*JP*/,
				"발목"/*KR*/,
				"лодыжка"/*RU*/
			),
			newStringArray("Ankle joint"),		newStringArray("Ankle joint")),
		new DisplayableAnatomicConcept("C0003483","15825003",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-42000",	"Aorta",			"AORTA",		null,	newStringArray("Aorta"),			newStringArray("Aorta")),
		new DisplayableAnatomicConcept("C0545736","LP33868-8",	false/*unpaired*/,	"LN",	null,	null,	"LP33868-8",	"Aorta and femoral artery",	null,	null,	newStringArray("Aorta and femoral artery"),	newStringArray("Aorta and femoral artery")),
		new DisplayableAnatomicConcept("C0003489","57034009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-42300",	"Aortic Arch",		null,	null,	newStringArray("Aortic Arch"),		newStringArray("Aortic Arch")),
		new DisplayableAnatomicConcept("C1508529","LP33903-3",	false/*unpaired*/,	"LN",	null,	null,	"LP33903-3",	"Aortic arch and subclavian artery",	null,	null,	newStringArray("Aortic arch and subclavian artery"),		newStringArray("Aortic arch and subclavian artery")),
		new DisplayableAnatomicConcept("C0446516","40983000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8200",	"Arm",				"ARM",			null,	newStringArray("Arm"),				newStringArray("Arm")),
		// D1-50666 "Arteriovenous fistula" is in the SNOMED US extension
		new DisplayableAnatomicConcept("C0003855","439470001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"D1-50666",	"Arteriovenous fistula",	null,	null,	newStringArray("Arteriovenous fistula"),	newStringArray("Arteriovenous fistula")),
		new DisplayableAnatomicConcept("C0004454","34797008",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8100",	"Axilla",			"AXILLA",		null,	newStringArray("Axilla"),			newStringArray("Axilla")),
		new DisplayableAnatomicConcept("C1995000","77568009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D2100",	"Back",				"BACK",			null,	newStringArray("Back"),				newStringArray("Back")),
		new DisplayableAnatomicConcept("C0005400","28273000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-60610",	"Bile duct",		null,			null,	newStringArray("Bile duct"),		newStringArray("Bile duct")),
		new DisplayableAnatomicConcept("C0005682","89837001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-74000",	"Bladder",			"BLADDER",		null,	newStringArray("Bladder"),			newStringArray("Bladder")),
		new DisplayableAnatomicConcept("C0006087","17137000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-47160",	"Brachial artery",	null,			null,	newStringArray("Brachial artery"),	newStringArray("Brachial artery")),
		new DisplayableAnatomicConcept("C0006104","12738006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-A0100",	"Brain",			"BRAIN",		null,	newStringArray("Brain"),			newStringArray("Brain")),
		new DisplayableAnatomicConcept("C0006141","76752008",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-04000",	"Breast",			"BREAST",		null,	newStringArray("Breast"),			newStringArray("Breast")),
		new DisplayableAnatomicConcept("C0006255","955009",		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-26000",	"Bronchus",			"BRONCHUS",		null,	newStringArray("Bronchus"),			newStringArray("Bronchus")),
		new DisplayableAnatomicConcept("C0006497","46862004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D2600",	"Buttock",			"BUTTOCK",		null,	newStringArray("Buttock"),			newStringArray("Buttock")),
		new DisplayableAnatomicConcept("C0006655","80144004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12770",	"Calcaneus",		"CALCANEUS",	null,	newStringArray("Calcaneus"),		newStringArray("Calcaneus")),
		new DisplayableAnatomicConcept("C0230445","53840002",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9440",	"Calf of leg",		"CALF",
			newStringArray("Calf"),
			newStringArray("Calf of leg"),		newStringArray("Calf of leg")),
		new DisplayableAnatomicConcept("C0007272","69105007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-45010",	"Carotid Artery",	"CAROTID",
			newStringArray("Carotid"),
			newStringArray("Carotid Artery"),	newStringArray("Carotid Artery")),
		new DisplayableAnatomicConcept("C1268981","180924008",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-A600A",	"Cerebellum",		"CEREBELLUM",	null,	newStringArray("Cerebellum"),		newStringArray("Cerebellum")),
		new DisplayableAnatomicConcept("C0728985","122494005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11501",	"Cervical spine",	"CSPINE",
			newStringArray(
				"CS",
				"CWK"/*NL*/,
				"CWZ"/*NL*/,
				"HWS"/*DE*/,
				"H Rygg"/*SE*/,
				"Cspine",
				"C spine",
				"Spine Cervical",
				"Cervical",
				"Cervic"/*abbrev*/,
				"Kaelalülid"/*EE*/,
				"KRÈNÍ OBRATLE"/*CZ*/,
				"Halswervels"/*NL*/,
				"Vertebrae cervicalis"/*NL*/,
				"Wervel hals"/*NL*/,
				"Kaulanikamat"/*FI*/,
				"Rachis cervical"/*FR*/,
				"Vertèbre cervicale"/*FR*/,
				"Vertèbres cervicales"/*FR*/,
				"COLONNE CERVICALE"/*FR*/,
				"CERVICALE"/*FR*/,
				"Halswirbel"/*DE*/,
				"Vertebrae cervicales"/*DE*/,
				"Vertebre cervicali"/*IT*/,
				"頚椎"/*JP*/,
				"頸椎"/*JP*/,
				"Vértebras Cervicais"/*PT*/,
				"ШЕЙНЫЕ ПОЗВОНКИ"/*RU*/,
				"columna cervical"/*ES*/,
				"columna cerv"/*ES abbrev*/,
				"columna espinal cervical"/*ES*/,
				"columna vertebral cervical"/*ES*/,
				"vértebras cervicales"/*ES*/,
				"Cervikalkotor"/*SE*/,
				"Halskotor"/*SE*/,
				"Halsrygg"/*SE*/,
				"Cervicale wervelzuil"/*BE*/,
				"C chrbtica"/*SK*/
			),
			newStringArray("Cervical spine"),	newStringArray("Cervical spine")),
		new DisplayableAnatomicConcept("C0729373","297171002",	false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F7",	"Cervico-thoracic spine",	"CTSPINE",
			newStringArray(
				"CTSPINE",
				"Cervico-thoracic",
				"Cervicothoracic"
			),
			newStringArray("Cervico-thoracic spine"),	newStringArray("Cervico-thoracic spine")),
		new DisplayableAnatomicConcept("C0007874","71252005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-83200",	"Cervix",			"CERVIX",		null,	newStringArray("Cervix"),			newStringArray("Cervix")),
		new DisplayableAnatomicConcept("C0007966","60819002",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1206",	"Cheek",			"CHEEK",		null,	newStringArray("Cheek"),			newStringArray("Cheek")),
		new DisplayableAnatomicConcept("C0817096","51185008",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D3000",	"Chest",			"CHEST",
			newStringArray(
				"Thorax",
				"Rindkere"/*EE*/,
				"HRUDNÍK"/*CZ*/,
				"hrudník"/*CZ*/,
				"Borst"/*NL*/,
				"Rintakehä"/*FI*/,
				"Poitrine"/*FR*/,
				"Potter"/*FR ?? - seen in examples*/,
				"Torse"/*FR*/,
				"Brustkorb"/*DE*/,
				"Torace"/*IT*/,
				"Peito"/*PT*/,
				"ГРУДНАЯ КЛЕТКА"/*RU*/,
				"ГРУДЬ"/*RU*/,
				"pecho"/*ES*/,
				"torácico"/*ES*/,
				"Bröstkorg"/*SE*/,
				"Torax"/*SE,PT,ES*/,
				"hrudnнk"/*SK*/,
				"hrudn"/*SK abbrev*/,
				"mellkas"/*HU*/,
				"Krūtinės ląsta"/*LT*/,
				"Tor",
				"Klatka"/*PL*/
			),
			newStringArray("Chest"),			newStringArray("Chest")),
		new DisplayableAnatomicConcept("C1284333","362047009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-45526",	"Circle of Willis",	"CIRCLEOFWILLIS",	null,	newStringArray("Circle of Willis"),	newStringArray("Circle of Willis")),
		new DisplayableAnatomicConcept("C0008913","51299004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12310",	"Clavicle",			"CLAVICLE",		null,	newStringArray("Clavicle"),			newStringArray("Clavicle")),
		new DisplayableAnatomicConcept("C0009194","64688005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11BF0",	"Coccyx",			"COCCYX",		null,	newStringArray("Coccyx"),			newStringArray("Coccyx")),
		new DisplayableAnatomicConcept("C0009368","71854001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-59300",	"Colon",			"COLON",		null,	newStringArray("Colon"),			newStringArray("Colon")),
		new DisplayableAnatomicConcept("C1268346","110797007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-DD080",	"Colon and rectum",	null,			null,	newStringArray("Colon and rectum"),	newStringArray("Colon and rectum")),
		new DisplayableAnatomicConcept("C0010031","28726007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA200",	"Cornea",			"CORNEA",		null,	newStringArray("Cornea"),			newStringArray("Cornea")),
		new DisplayableAnatomicConcept("C0205042","41801008",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-43000",	"Coronary artery",	"CORONARYARTERY",
			newStringArray("Coronary"),
			newStringArray("Coronary artery"),	newStringArray("Coronary artery")),
		new DisplayableAnatomicConcept("C0011980","5798000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D3400",	"Diaphragm",		null,			null,	newStringArray("Diaphragm"),		newStringArray("Diaphragm")),
		new DisplayableAnatomicConcept("C0013303","38848004",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58200",	"Duodenum",			"DUODENUM",		null,	newStringArray("Duodenum"),			newStringArray("Duodenum")),
		new DisplayableAnatomicConcept("C0521421","1910005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AB000",	"Ear",				"EAR",			null,	newStringArray("Ear"),				newStringArray("Ear")),
		new DisplayableAnatomicConcept("C1305417","76248009",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8300",	"Elbow",			"ELBOW",
			newStringArray(
				"Ellbogen"/*DE*/,
				"Coude"/*FR*/,
				"Küünar"/*EE*/,
				"Armbåge"/*SE*/,
				"Codo"/*ES*/,
				"Cotovelo"/*PT*/
			),
			newStringArray("Elbow"),			newStringArray("Elbow")),
		new DisplayableAnatomicConcept("C0229960","38266002",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D0010",	"Entire body",		"WHOLEBODY",
			newStringArray(
				"Entire body",
				"Whole body",
				"Mid body"	/* not quite right, but nothing better */
			),
			newStringArray("Entire body"),		newStringArray("Entire body")),
		new DisplayableAnatomicConcept("C0014876","32849002",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-56000",	"Esophagus",		"ESOPHAGUS",	null,	newStringArray("Esophagus"),		newStringArray("Esophagus")),
		new DisplayableAnatomicConcept("C0015385","66019005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D0300",	"Extremity",		"EXTREMITY",
			newStringArray(
				"Extremety"/*Agfa CR spelling mistake*/,
				"Extremidad"/*ES*/
				),
			newStringArray("Extremity"),		newStringArray("Extremity")),
		new DisplayableAnatomicConcept("C0015392","81745001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA000",	"Eye",				"EYE",			null,	newStringArray("Eye"),				newStringArray("Eye")),
		new DisplayableAnatomicConcept("C0015426","80243003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA810",	"Eyelid",			"EYELID",		null,	newStringArray("Eyelid"),			newStringArray("Eyelid")),
		// not face ... gets confused with frontal view (FR,NL) ... new DisplayableAnatomicConcept("C0015450","89545001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1200",	"Face",				"FACE",			null,	newStringArray("Face"),				newStringArray("Face")),
		new DisplayableAnatomicConcept("C0015801","7657000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-47400",	"Femoral artery",	null,			null,	newStringArray("Femoral artery"),	newStringArray("Femoral artery")),
		new DisplayableAnatomicConcept("C0015811","71341001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12710",	"Femur",			"FEMUR",		null,	newStringArray("Femur"),			newStringArray("Femur")),
		new DisplayableAnatomicConcept("C0524584","55460000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-F5201",	"Fetus",			null,			null,	newStringArray("Fetus"),			newStringArray("Fetus")),
		new DisplayableAnatomicConcept("C0016129","7569003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8800",	"Finger",			"FINGER",		null,	newStringArray("Finger"),			newStringArray("Finger")),
		new DisplayableAnatomicConcept("C0016504","56459004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9700",	"Foot",				"FOOT",
			newStringArray(
				"Pied"/*FR*/,
				"Pie"/*ES*/,
				"Voet"/*NL*/,
				"Fuß"/*DE*/,
				"πόδι"/*GR*/,
				"Piede"/*IT*/,
				/*"pé"*//*PT*//*,*//* Cannot use this one ... matches PET and calls all PET scans as foot ! */
				"нога"/*RU*/
			),
			newStringArray("Foot"),				newStringArray("Foot")),
		new DisplayableAnatomicConcept("C0223680","55797009",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12402",	"Forearm bone",		"FOREARM",
			newStringArray(
				"Forearm",
				"U ARM"/*DE*/,
				"Unterarm"/*DE*/,
				"Avambraccio"/*IT*/,
				"PØEDLOKTÍ"/*CZ*/,
				"Onderarm"/*NL*/,
				"Kyynärvarsi"/*FI*/,
				"Avant-bras"/*FR*/,
				"まえうで"/*JP*/,
				"前腕"/*JP*/,
				"Antebraço"/*PT*/,
				"ПРЕДПЛЕЧЬЕ"/*RU*/,
				"antebrazo"/*ES*/,
				"Underarm"/*SE*/,
				"predlaktie"/*SK*/
			),
			newStringArray("Forearm"),		newStringArray("Forearm")),
		new DisplayableAnatomicConcept("C0016976","28231008",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-63000",	"Gallbladder",		"GALLBLADDER",	null,	newStringArray("Gallbladder"),		newStringArray("Gallbladder")),
		new DisplayableAnatomicConcept("C0018563","85562004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8700",	"Hand",				"HAND",			null,	newStringArray("Hand"),				newStringArray("Hand")),
		new DisplayableAnatomicConcept("C0018670","69536005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1100",	"Head",				"HEAD",
			newStringArray(
				"Kopf"/*DE*/,
				"Schaedel"/*DE*/,
				"Schædel"/*DE*/,
				"Sch?del"/*DE encoded incorrectly*/,
				"Tete"/*FR*/
			),
			newStringArray("Head"),				newStringArray("Head")),
		new DisplayableAnatomicConcept("C0460004","774007",		false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1000",	"Head and Neck",	"HEADNECK",	
			newStringArray("Head Neck"),
			newStringArray("Head and Neck"),	newStringArray("Head and Neck")),
		new DisplayableAnatomicConcept("C0018787","80891009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-32000",	"Heart",			"HEART",		null,	newStringArray("Heart"),			newStringArray("Heart")),
		new DisplayableAnatomicConcept("C0019552","24136001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15710",	"Hip joint",		"HIP",
			newStringArray(
				"Hip",
				"Heup"/*NL*/,
				"Hanche"/*FR*/,
				"Hüfte"/*DE*/,
				"Puus"/*EE*/,
				"HÖFT"/*SE*/,
				"Cadera"/*ES*/,
				"ισχίο"/*GR*/,
				"anca"/*IT*/,
				"ヒップ"/*JP*/,
				"엉덩이"/*KR*/,
				"вальма"/*RU*/
			),
			newStringArray("Hip"),		newStringArray("Hip")),
		new DisplayableAnatomicConcept("C0020164","85050009",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12410",	"Humerus",			"HUMERUS",
			newStringArray(
				"UP_EXM"/*Fuji CR BPE*/,
				"O ARM"/*DE,SE*/,
				"Oberarm"/*DE*/,
				"Õlavars"/*EE*/,
				"Bovenarm"/*NL*/,
				"húmero"/*ES*/
			),
			newStringArray("Humerus"),			newStringArray("Humerus")),
		new DisplayableAnatomicConcept("C0020885","34516001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58600",	"Ileum",			"ILEUM",		null,	newStringArray("Ileum"),			newStringArray("Ileum")),
		new DisplayableAnatomicConcept("C0576469","299716001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-41068",	"Iliac and femoral artery",	null,	null,	newStringArray("Iliac and femoral artery"),	newStringArray("Iliac and femoral artery")),
		new DisplayableAnatomicConcept("C0020889","22356005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12340",	"Ilium",			"ILIUM",		null,	newStringArray("Ilium"),			newStringArray("Ilium")),
		new DisplayableAnatomicConcept("C0018246","26893007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D7000",	"Inguinal region",	null,			null,	newStringArray("Inguinal region"),	newStringArray("Inguinal region")),
		new DisplayableAnatomicConcept("C1283773","361078006",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AB959",	"Internal Auditory Canal",	"IAC",
			newStringArray("IAC"),
			newStringArray("Internal Auditory Canal"),	newStringArray("Internal Auditory Canal")),
		new DisplayableAnatomicConcept("C0226364","90024005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-46740",	"Internal iliac artery",	null,			null,	newStringArray("Internal iliac artery"),	newStringArray("Internal iliac artery")),
		new DisplayableAnatomicConcept("C0022359","661005",		true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D1213",	"Jaw region",		"JAW",			null,	newStringArray("Jaw region"),		newStringArray("Jaw region")),
		new DisplayableAnatomicConcept("C0022378","21306003",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58400",	"Jejunum",			"JEJUNUM",		null,	newStringArray("Jejunum"),			newStringArray("Jejunum")),
		new DisplayableAnatomicConcept("C0022646","64033007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-71000",	"Kidney",			"KIDNEY",		null,	newStringArray("Kidney"),			newStringArray("Kidney")),
		new DisplayableAnatomicConcept("C1456798","72696002",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9200",	"Knee",				"KNEE",
			newStringArray(
				"Knie"/*DE,NL*/,
				"Genou"/*FR*/,
				"Põlv"/*EE*/,
				"Pölv"/*EE ?wrong accent*/,
				"Knä"/*SE*/,
				"Rodilla"/*ES*/
			),
			newStringArray("Knee"),				newStringArray("Knee")),
		new DisplayableAnatomicConcept("C0023078","4596009",		false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-24100",	"Larynx",			"LARYNX",
			newStringArray(
				"Laringe"/*ES,IT*/,
				"Kehlkopf"/*DE*/,
				"Strottenhoofd"/*NL*/
				/*FR is same as english*/
			),
				newStringArray("Larynx"),			newStringArray("Larynx")),
		new DisplayableAnatomicConcept("C1140621","30021000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9400",	"Leg",				"LEG",
			newStringArray(
				"LOW_EXM"/*Fuji CR BPE*/,
				"LOWEXM"/*Siemens CR BPE*/,
				"TIB FIB ANKLE",
				"Jambe"/*FR*/
			),
			newStringArray("Leg"),				newStringArray("Leg")),
		new DisplayableAnatomicConcept("C0023884","10200004",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-62000",	"Liver",			"LIVER",
			newStringArray(
				"foie"/*FR*/,
				"Kepenys"/*LT*/
			),
			newStringArray("Liver"),			newStringArray("Liver")),
		new DisplayableAnatomicConcept("C0024091","122496007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11503",	"Lumbar spine",		"LSPINE",
			newStringArray(
				"LS",
				"LWK"/*NL*/,
				"LWZ"/*NL*/,
				"LWS"/*DE*/,
				"L Rygg"/*SE*/,
				"Lspine",
				"L spine",
				"Spine Lumbar",
				"Lumbar",
				"Rachis lombaire"/*FR*/,
				"COLONNE LOMBAIRE"/*FR*/,
				"Rach.Lomb"/*FR abbrev*/,
				"lombaire"/*FR*/,
				"Nimmelülid"/*EE*/,
				"Columna lumbar"/*ES*/,
				"LÄNDRYGG"/*SE*/,
				"L chrbtica"/*SK*/,
				"COL LOMBARE"
			),
			newStringArray("Lumbar spine"),		newStringArray("Lumbar spine")),
		new DisplayableAnatomicConcept("C0223603","297173004",	false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F9",	"Lumbo-sacral spine",	"LSSPINE",
			newStringArray(
				"LSSPINE",
				"Lumbosacral spine",
				"Lumbo-sacrale wervelzuil"/*BE*/,
				"columna vertebral lumbosacra"/*ES*/,
				"vértebras lumbosacras"/*ES*/,
				"Colonna Lombosacrale"
			),
			newStringArray("Lumbo-sacral spine"),	newStringArray("Lumbo-sacral spine")),
		new DisplayableAnatomicConcept("C0024109","39607008",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-28000",	"Lung",				"LUNG",
			newStringArray(
				"pluco"/*PL*/,
				"pluca"/*PL*/
			),
			newStringArray("Lung"),				newStringArray("Lung")),
		new DisplayableAnatomicConcept("C0024687","91609006",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11180",	"Mandible",			"JAW",			null,	newStringArray("Mandible"),			newStringArray("Mandible")),
		new DisplayableAnatomicConcept("C0024947","70925003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11170",	"Maxilla",			"MAXILLA",		null,	newStringArray("Maxilla"),			newStringArray("Maxilla")),
		new DisplayableAnatomicConcept("C0178738","LP30124-9",	true   /*paired*/,	"LN",	null,	null,	"LP30124-9",	"Maxilla and Mandible",				null,		null,	newStringArray("Maxilla and Mandible"),			newStringArray("Maxilla and Mandible")),
		new DisplayableAnatomicConcept("C0025066","72410000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D3300",	"Mediastinum",		"MEDIASTINUM",	null,	newStringArray("Mediastinum"),		newStringArray("Mediastinum")),
		new DisplayableAnatomicConcept("C1267547","21082005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-51000",	"Mouth",			"MOUTH",		null,	newStringArray("Mouth"),			newStringArray("Mouth")),
		new DisplayableAnatomicConcept("C0027530","45048000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1600",	"Neck",				"NECK",
			newStringArray(
				"Kael"/*EE*/,
				"Collo"/*IT*/,
				"Cuello"/*ES*/,
				"Hals"/*DE*/,
				"Nek"/*NL*/,
				"Nacke"/*SE*/
			),
			newStringArray("Neck"),				newStringArray("Neck")),
		new DisplayableAnatomicConcept("C0028429","45206002",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-21000",	"Nose",				"NOSE",			null,	newStringArray("Nose"),				newStringArray("Nose")),
		new DisplayableAnatomicConcept("C0015392","371398005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D0801",	"Orbital region",	"ORBIT",
			newStringArray(
				"Orbit"
			),
			newStringArray("Orbital region"),	newStringArray("Orbital region")),
		new DisplayableAnatomicConcept("C0029939","15497006",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-87000",	"Ovary",			"OVARY",		null,	newStringArray("Ovary"),			newStringArray("Ovary")),
		new DisplayableAnatomicConcept("C0030274","15776009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-65000",	"Pancreas",			"PANCREAS",		null,	newStringArray("Pancreas"),			newStringArray("Pancreas")),
		new DisplayableAnatomicConcept("C0030288","69930009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-65010",	"Pancreatic duct",	null,			null,	newStringArray("Pancreatic duct"),	newStringArray("Pancreatic duct")),
		new DisplayableAnatomicConcept("C1267614","110621006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-65600",	"Pancreatic duct and bile duct systems",			null,
			newStringArray(
				"Pancreatic duct and bile duct systems",
				"Pancreatic duct and bile ducts",
				"Pancreatic duct and bile duct",
				"Pancreatic and bile ducts"
			),
			newStringArray("Pancreatic duct and bile duct systems"),
			newStringArray("Pancreatic duct and bile duct systems")),
		new DisplayableAnatomicConcept("C0030580","45289007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-61100",	"Parotid gland",	"PAROTID",
			newStringArray("Parotid"),
			newStringArray("Parotid gland"),	newStringArray("Parotid gland")),
		new DisplayableAnatomicConcept("C0030647","64234005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12730",	"Patella",			"PATELLA",		null,	newStringArray("Patella"),			newStringArray("Patella")),
		new DisplayableAnatomicConcept("C0030797","12921003",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D6000",	"Pelvis",			"PELVIS",
			newStringArray(
				"PV"/*abbreviations*/,
				"Pelv",
				"Pel",
				"Bekken"/*NL*/,
				"Becken"/*DE*/,
				"Bassin"/*FR*/,
				"Vaagen"/*EE*/,
				"BÄCKEN"/*SE*/,
				"λεκάνη"/*GR*/,
				"Bacino"/*IT*/,
				"骨盤"/*JP*/,
				"골반"/*KR*/,
				"miednica"/*PL*/
			),
			newStringArray("Pelvis"),			newStringArray("Pelvis")),
		new DisplayableAnatomicConcept("C0030851","18911002",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-91000",	"Penis",			"PENIS",		null,	newStringArray("Penis"),			newStringArray("Penis")),
		new DisplayableAnatomicConcept("C0225972","25489000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-39050",	"Pericardial cavity",	null,		null,	newStringArray("Pericardial cavity"),	newStringArray("Pericardial cavity")),
		new DisplayableAnatomicConcept("C1278903","181211006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-55002",	"Pharynx",			"PHARYNX",		null,	newStringArray("Pharynx"),			newStringArray("Pharynx")),
		new DisplayableAnatomicConcept("C0033572","41216001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-92000",	"Prostate",			"PROSTATE",		null,	newStringArray("Prostate"),			newStringArray("Prostate")),
		new DisplayableAnatomicConcept("C0034896","34402009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-59600",	"Rectum",			"RECTUM",		null,	newStringArray("Rectum"),			newStringArray("Rectum")),
		new DisplayableAnatomicConcept("C0035561","113197003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11300",	"Rib",				"RIB",
			newStringArray(
				"Gril costal"/*FR*/,
				"Gril cost"/*FR abbrev*/
			),
			newStringArray("Rib"),				newStringArray("Rib")),
		new DisplayableAnatomicConcept("C0036037","54735007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11AD0",	"Sacrum",			"SSPINE",
			newStringArray("SSPINE"),
			newStringArray("Sacrum"),			newStringArray("Sacrum")),
		new DisplayableAnatomicConcept("C0036270","41695006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D1160",	"Scalp",			"SCALP",		null,	newStringArray("Scalp"),			newStringArray("Scalp")),
		new DisplayableAnatomicConcept("C0036277","79601000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-12280",	"Scapula",			"SCAPULA",		null,	newStringArray("Scapula"),			newStringArray("Scapula")),
		new DisplayableAnatomicConcept("C0036410","18619003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-AA110",	"Sclera",			"SCLERA",		null,	newStringArray("Sclera"),			newStringArray("Sclera")),
		new DisplayableAnatomicConcept("C0036471","20233005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-98000",	"Scrotum",			"SCROTUM",		null,	newStringArray("Scrotum"),			newStringArray("Scrotum")),
		new DisplayableAnatomicConcept("C0037004","16982005",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D2220",	"Shoulder",			"SHOULDER",
			newStringArray(
				"Schouder"/*NL*/,
				"Schulter"/*DE*/,
				"Epaule"/*FR*/,
				"épaule"/*FR*/,
				"õlg"/*EE*/,
				"Ölg"/*EE ?wrong accent*/,
				"Hombro"/*ES*/,
				"Ombro"/*PT*/,
				"Rameno"/*SK*/,
				"Rippe"/*DE*/
			),
			newStringArray("Shoulder"),			newStringArray("Shoulder")),
		new DisplayableAnatomicConcept("C0021852","30315005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-58000",	"Small intestine",	null,		null,	newStringArray("Small intestine"),			newStringArray("Small intestine")),
		new DisplayableAnatomicConcept("C0037303","89546000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11100",	"Skull",			"SKULL",
			newStringArray(
				"Kolju"/*EE*/,
				"LEBKA"/*CZ*/,
				"Schedel"/*NL*/,
				"Kallo"/*FI*/,
				"Crâne"/*FR*/,
				"Cranium"/*DE*/,
				"Schädel"/*DE*/,
				"Cranio"/*IT*/,
				"Calota Craniana"/*PT*/,
				"Crânio"/*PT*/,
				"ЧЕРЕП"/*RU*/,
				"Calota Craneal"/*ES*/,
				"Cráneo"/*ES*/,
				"Kalvarium"/*SE*/,
				"Kranium"/*SE*/,
				"Skalle"/*SE*/,
				"Lebka"/*SK*/
			),
			newStringArray("Skull"),			newStringArray("Skull")),
		new DisplayableAnatomicConcept("C0037949","280717001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-D0146",	"Spine",			"SPINE",
			newStringArray(
				"Rachis"/*FR*/,
				"Rygg"/*SE*/,
				"chrbtica"/*SK*/
			),
			newStringArray("Spine"),			newStringArray("Spine")),
			
		new DisplayableAnatomicConcept("C0028872","51807001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-64710",	"Sphincter of Oddi",	null,		null,	newStringArray("Sphincter of Oddi"),	newStringArray("Sphincter of Oddi")),
		new DisplayableAnatomicConcept("C0278443","56101001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-65016",	"Sphincter pancreaticus",	null,		null,	newStringArray("Sphincter pancreaticus"),	newStringArray("Sphincter pancreaticus")),
		new DisplayableAnatomicConcept("C0037993","78961009",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-C3000",	"Spleen",			"SPLEEN",		null,	newStringArray("Spleen"),			newStringArray("Spleen")),
		new DisplayableAnatomicConcept("C0038293","56873002",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11210",	"Sternum",			"STERNUM",		null,	newStringArray("Sternum"),			newStringArray("Sternum")),
		new DisplayableAnatomicConcept("C0038351","69695003",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-57000",	"Stomach",			"STOMACH",		null,	newStringArray("Stomach"),			newStringArray("Stomach")),
		new DisplayableAnatomicConcept("C0038530","36765005",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-46100",	"Subclavian artery",	null,		null,	newStringArray("Subclavian artery"),	newStringArray("Subclavian artery")),
		new DisplayableAnatomicConcept("C0038556","54019009",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-61300",	"Submandibular gland",	"SUBMANDIBULAR",
			newStringArray("Submandibular"),
			newStringArray("Submandibular gland"),	newStringArray("Submandibular gland")),
		new DisplayableAnatomicConcept("C0039493","53620006",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15290",	"Temporomandibular joint",	"TMJ",
			newStringArray(
				"Temporomandibular",
				"TMJ"
			),
			newStringArray("Temporomandibular joint"),	newStringArray("Temporomandibular joint")),
		new DisplayableAnatomicConcept("C0039597","40689003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-94000",	"Testis",			"TESTIS",		null,	newStringArray("Testis"),			newStringArray("Testis")),
		new DisplayableAnatomicConcept("C0039866","68367000",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9100",	"Thigh",			"THIGH",
			newStringArray(
				"Oberschenkel"/*DE*/,
				"Bovenbeen"/*NL*/,
				"Reis"/*EE*/
			),
			newStringArray("Thigh"),			newStringArray("Thigh")),
		new DisplayableAnatomicConcept("C0581269","122495006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-11502",	"Thoracic spine",	"TSPINE",
			newStringArray(
				"TSPINE",
				"TS",
				"THWK"/*NL*/,
				"DWZ"/*NL*/,
				"BWS"/*DE*/,
				"B Rygg"/*SE*/,
				"T spine",
				"Spine Thoracic",
				"Thoracic",
				"Dorsal",
				"Dorsal spine",
				"Spine Dorsal",
				"Rachis dorsal"/*FR*/,
				"COLONNE THORACIQUE"/*FR*/,
				"THORACIQUE"/*FR*/,
				"Rinnaosa"/*EE??*/,
				"Rinnalülid"/*EE*/,
				"Columna dorsal"/*ES*/,
				"Columna vertebral dorsal"/*ES*/,
				"Thoracale wervelzuil"/*BE*/,
				"BRÖSTRYGG"/*SE*/,
				"Th chrbtica"/*SK*/
			),
			newStringArray("Thoracic spine"),	newStringArray("Thoracic spine")),
		new DisplayableAnatomicConcept("C0729374","297172009",	false/*unpaired*/,	"SRT",	"SRT",	null,	"T-D00F8",	"Thoraco-lumbar spine",	"TLSPINE",
			newStringArray(
				"TLSPINE",
				"Thoraco-lumbar",
				"Thoracolumbar",
				"Col.Dors.Lomb"/*FR abbrev*/,
				"THORACOLUMBALE"
			),
			newStringArray("Thoraco-lumbar spine"),	newStringArray("Thoraco-lumbar spine")),
		new DisplayableAnatomicConcept("C0040067","76505004",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D8810",	"Thumb",			"THUMB",		null,	newStringArray("Thumb"),			newStringArray("Thumb")),
		new DisplayableAnatomicConcept("C1306748","118507000",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-C8001",	"Thymus",			"THYMUS",		null,	newStringArray("Thymus"),			newStringArray("Thymus")),
		new DisplayableAnatomicConcept("C0040132","69748006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-B6000",	"Thyroid",			"THYROID",		null,	newStringArray("Thyroid"),			newStringArray("Thyroid")),
		new DisplayableAnatomicConcept("C0040357","29707007",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-D9800",	"Toe",				"TOE",			null,	newStringArray("Toe"),				newStringArray("Toe")),
		new DisplayableAnatomicConcept("C0040408","21974007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-53000",	"Tongue",			"TONGUE",		null,	newStringArray("Tongue"),			newStringArray("Tongue")),
		new DisplayableAnatomicConcept("C0040578","44567001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-25000",	"Trachea",			"TRACHEA",		null,	newStringArray("Trachea"),			newStringArray("Trachea")),
		new DisplayableAnatomicConcept("C0227690","65364008",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-73800",	"Ureter",			"URETER",		null,	newStringArray("Ureter"),			newStringArray("Ureter")),
		new DisplayableAnatomicConcept("C0041967","13648007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-75000",	"Urethra",			"URETHRA",		null,	newStringArray("Urethra"),			newStringArray("Urethra")),
		new DisplayableAnatomicConcept("C0042149","35039007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-83000",	"Uterus",			"UTERUS",		null,	newStringArray("Uterus"),			newStringArray("Uterus")),
		new DisplayableAnatomicConcept("C0042232","76784001",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-82000",	"Vagina",			"VAGINA",		null,	newStringArray("Vagina"),			newStringArray("Vagina")),
		new DisplayableAnatomicConcept("C0729900","312548007",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-46006",	"Ventral branch of abdominal aorta",		null,			null,	newStringArray("Ventral branch of abdominal aorta"),			newStringArray("Ventral branch of abdominal aorta")),
		new DisplayableAnatomicConcept("C0042993","45292006",	false/*unpaired*/,	"SRT",	"SNM3",	null,	"T-81000",	"Vulva",			"VULVA",		null,	newStringArray("Vulva"),			newStringArray("Vulva")),
		new DisplayableAnatomicConcept("C1262468","74670003",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-15460",	"Wrist joint",		"WRIST",
			newStringArray(
				"Wrist",
				"muñeca"/*ES*/,
				"MUÒECA"/*ES ? misspelled*/,
				"pols"/*NL*/,
				"poignet"/*FR*/,
				"Handgelenk"/*DE*/,
				"καρπός"/*GR*/,
				"polso"/*IT,PT*/,
				"手首"/*JP*/,
				"손목"/*KR*/,
				"запястье руки"/*RU*/,
				"ranne"/*EE*/,
				"käe"/*EE*/
			), 
			newStringArray("Wrist joint"),		newStringArray("Wrist joint")),
		new DisplayableAnatomicConcept("C0162485","51204001",	true   /*paired*/,	"SRT",	"SNM3",	null,	"T-11167",	"Zygomatic arch",	"ZYGOMA",	// need to change concept when CP 1258 is approved :(
			newStringArray("Zygoma"),
			newStringArray("Zygomatic arch"),	newStringArray("Zygomatic arch")),
	};
	
	protected static DictionaryOfConcepts anatomyConcepts = new DictionaryOfConcepts(anatomicConceptEntries,badAnatomyWords,"Anatomy");

	public static DictionaryOfConcepts getAnatomyConcepts() { return anatomyConcepts; }
	
	public static DisplayableAnatomicConcept findAnatomicConcept(AttributeList list) {
		// strategy is to look in specific attributes first, then general, and look in codes before free text ...
		DisplayableConcept anatomy = null;
		{
			CodedSequenceItem anatomicRegionSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.AnatomicRegionSequence);
			if (anatomicRegionSequence != null) {
				slf4jlogger.debug("findAnatomicConcept(): anatomicRegionSequence = {}",anatomicRegionSequence);
				anatomy = anatomyConcepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(anatomicRegionSequence);
				if (anatomy != null) slf4jlogger.debug("findAnatomicConcept(): found Anatomy in AnatomicRegionSequence = {}",anatomy.toStringBrief());
			}
		}
		if (anatomy == null) {
			String bodyPartExamined = Attribute.getSingleStringValueOrNull(list,TagFromName.BodyPartExamined);
			if (bodyPartExamined != null) {
				slf4jlogger.debug("findAnatomicConcept(): bodyPartExamined = {}",bodyPartExamined);
				anatomy = anatomyConcepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(bodyPartExamined);
				if (anatomy != null) slf4jlogger.debug("findAnatomicConcept(): found Anatomy in BodyPartExamined = {}",anatomy.toStringBrief());
			}
		}
		if (anatomy == null) {
			anatomy = findAmongstGeneralAttributes(list,anatomyConcepts,badLateralityOrViewOrAnatomyPhraseTriggers);
		}
		return (DisplayableAnatomicConcept)anatomy;
	}
	
	public static DisplayableConcept findAmongstGeneralAttributes(AttributeList list,DictionaryOfConcepts concepts,String[] badPhraseTriggers) {
		// strategy is to look in attributes of lower level entities first, and look in codes before free text ...
		DisplayableConcept found = null;
		{
			String imageComments = Attribute.getSingleStringValueOrNull(list,TagFromName.ImageComments);
			if (imageComments != null && !StringUtilities.containsRegardlessOfCase(imageComments,badPhraseTriggers)) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): imageComments = {}",imageComments);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(imageComments);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in ImageComments = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			String seriesDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesDescription);
			if (seriesDescription != null && !StringUtilities.containsRegardlessOfCase(seriesDescription,badPhraseTriggers)) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): seriesDescription = {}",seriesDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(seriesDescription);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in SeriesDescription = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			String protocolName = Attribute.getSingleStringValueOrNull(list,TagFromName.ProtocolName);
			if (protocolName != null && !StringUtilities.containsRegardlessOfCase(protocolName,badPhraseTriggers)) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): protocolName = {}",protocolName);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(protocolName);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in ProtocolName = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			CodedSequenceItem performedProtocolCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.PerformedProtocolCodeSequence);
			if (performedProtocolCodeSequence != null) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): performedProtocolCodeSequence = {}",performedProtocolCodeSequence);
				found = concepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(performedProtocolCodeSequence);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in PerformedProtocolCodeSequence = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			String performedProcedureStepDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.PerformedProcedureStepDescription);
			if (performedProcedureStepDescription != null && !StringUtilities.containsRegardlessOfCase(performedProcedureStepDescription,badPhraseTriggers)) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): performedProcedureStepDescription = {}",performedProcedureStepDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(performedProcedureStepDescription);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in PerformedProcedureStepDescription = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			CodedSequenceItem procedureCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ProcedureCodeSequence);
			if (procedureCodeSequence != null) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): procedureCodeSequence = {}",procedureCodeSequence);
				found = concepts.findCodeInEntriesFirstThenTryCodeMeaningInEntriesThenTryLongestIndividualEntryContainedWithinCodeMeaning(procedureCodeSequence);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in ProcedureCodeSequence = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		if (found == null) {
			String studyDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDescription);
			if (studyDescription != null && !StringUtilities.containsRegardlessOfCase(studyDescription,badPhraseTriggers)) {
				slf4jlogger.debug("findAmongstGeneralAttributes(): seriesDescription = {}",studyDescription);
				found = concepts.findInEntriesFirstThenTryLongestIndividualEntryContainedWithin(studyDescription);
				if (found != null) slf4jlogger.debug("findAmongstGeneralAttributes(): found {} in StudyDescription = {}",concepts.getDescriptionOfConcept(),found.toStringBrief());
			}
		}
		return found;
	}
	

	/**
	 * <p>Read the DICOM input file and extract anatomical information.</p>
	 *
	 * @param	arg	array of one string, the filename to read
	 */
	public static void main(String arg[]) {
		if (arg.length == 1) {
			String inputFileName = arg[0];
			try {
				AttributeList list = new AttributeList();
				//list.read(inputFileName);
				list.read(inputFileName,null,true,true,TagFromName.PixelData);
				DisplayableAnatomicConcept anatomy = findAnatomicConcept(list);
				if (anatomy != null) {
					slf4jlogger.info(anatomy.toString());
				}
				else {
					slf4jlogger.info("########################### - ANATOMY NOT FOUND - ###########################");
				}
			} catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			}
		}
	}
	
}


	

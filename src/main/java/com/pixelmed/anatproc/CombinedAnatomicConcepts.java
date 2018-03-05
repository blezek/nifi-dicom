/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.AttributeList;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>This class supports anatomic concepts that may be combinations of one another.</p>
 * 
 * <p>Instances cannot be constructed directly, but rather are looked up using static methods that access a library of known combinations.</p>
 * 
 * @author	dclunie
 */
public class CombinedAnatomicConcepts extends DisplayableConcept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/CombinedAnatomicConcepts.java,v 1.8 2017/01/24 10:50:32 dclunie Exp $";
		
	// new Concept("C1508499"/*"Abdomen and Pelvis"*/)
	// new Concept("C1442171"/*"Chest and Abdomen"*/)
	// new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/)
	// new Concept("C0460004"/*"Head and Neck"*/)
	// new Concept("C1562459"/*"Neck and Chest"*/)
	// new Concept("C1562378"/*"Neck, Chest and Abdomen"*/)
	// new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/)
	
	// new Concept("C0006104"/*"Brain"*/)
	// new Concept("C0018670"/*"Head"*/)
	// new Concept("C0027530"/*"Neck"*/)
	// new Concept("C0817096"/*"Chest"*/)
	// new Concept("C0024109"/*"Lung"*/)
	// new Concept("C0000726"/*"Abdomen"*/)
	// new Concept("C0030797"/*"Pelvis"*/)
	
	protected static Concept[] newConceptArray(Concept... values) { return values; }

	// combinations array is searched in successive order, so more stringent matches should come first ...
	// synonyms are permuted, since we need the array sizes to match to determine a complete match ...
	protected static Combination[] combinations = {
		// deal with what are essentially synonyms first (to prevent expanding body part due to synonyms in broader combinations) ...
		
		new Combination(new Concept("C0018670"/*"Head"*/),	newConceptArray(new Concept("C0006104"/*"Brain"*/))),
		new Combination(new Concept("C0817096"/*"Chest"*/),	newConceptArray(new Concept("C0024109"/*"Lung"*/))),
		
		// deal with two pair combinations ...
		new Combination(new Concept("C1508499"/*"Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		new Combination(new Concept("C0460004"   /*"Head and Neck"*/),      newConceptArray(
				new Concept("C0018670"/*"Head"*/),
				new Concept("C0027530"/*"Neck"*/)
			)
		),
		new Combination(new Concept("C0460004"   /*"Head and Neck"*/),      newConceptArray(
				new Concept("C0006104"/*"Brain"*/),
				new Concept("C0027530"/*"Neck"*/)
			)
		),
		new Combination(new Concept("C1562459"/*"Neck and Chest"*/),     newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/)
			)
		),
		new Combination(new Concept("C1562459"/*"Neck and Chest"*/),     newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0024109"/*"Lung"*/)
			)
		),
		new Combination(new Concept("C1442171"/*"Chest and Abdomen"*/),  newConceptArray(
				new Concept("C0817096"/*"Chest"*/),
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),
		new Combination(new Concept("C1442171"/*"Chest and Abdomen"*/),  newConceptArray(
				new Concept("C0024109"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),
		new Combination(new Concept("C1508520"/*"Aortic Arch and Carotid Artery"*/),  newConceptArray(
				new Concept("C0003489"/*"Aortic arch"*/),
				new Concept("C0007272"/*"Carotid"*/)
			)
		),
		new Combination(new Concept("C0178738"/*"Maxilla and Mandible"*/),  newConceptArray(
				new Concept("C0024947"/*"Maxilla"*/),
				new Concept("C0024687"/*"Mandible"*/)
			)
		),
		new Combination(new Concept("C1267614"/*"Pancreatic duct and bile duct systems"*/),  newConceptArray(
				new Concept("C0030288"/*"Pancreatic duct"*/),
				new Concept("C0005400"/*"Bile duct"*/)
			)
		),
		new Combination(new Concept("C1268346"/*"Colon and rectum"*/),  newConceptArray(
				new Concept("C0009368"/*"Colon"*/),
				new Concept("C0034896"/*"Rectum"*/)
			)
		),
		new Combination(new Concept("C0545736"/*"Aorta and femoral artery"*/),  newConceptArray(
				new Concept("C0003483"/*"Aorta"*/),
				new Concept("C0015801"/*"Femoral artery"*/)
			)
		),
		new Combination(new Concept("C1508529"/*"Aortic arch and subclavian artery"*/),  newConceptArray(
				new Concept("C0003483"/*"Aorta"*/),					// hmmm ... this is not specific to the arch :(
				new Concept("C0038530"/*"Subclavian artery"*/)
			)
		),
		new Combination(new Concept("C1267080"/*"Radius and ulna"*/),  newConceptArray(
				new Concept("C0034627"/*"Radius"*/),
				new Concept("C0041600"/*"Ulna"*/)
			)
		),

		
		// deal with three pair combinations ...
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0817096"/*"Chest"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0024109"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1442171"/*"Chest and Abdomen"*/),
				new Concept("C1508499"/*"Abdomen and Pelvis"*/)
			)
		),
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1442171"/*"Chest and Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0817096"/*"Chest"*/),
				new Concept("C1508499"/*"Abdomen and Pelvis"*/)
			)
		),
		new Combination(new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0024109"/*"Lung"*/),
				new Concept("C1508499"/*"Abdomen and Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562378"/*"Neck, Chest and Abdomen"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/),
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),
		
		new Combination(new Concept("C1562378"/*"Neck, Chest and Abdomen"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0024109"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),
		
		new Combination(new Concept("C1562378"/*"Neck, Chest and Abdomen"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C1442171"/*"Chest and Abdomen"*/)
			)
		),
		
		new Combination(new Concept("C1562378"/*"Neck, Chest and Abdomen"*/), newConceptArray(
				new Concept("C1562459"/*"Neck and Chest"*/), 
				new Concept("C0000726"/*"Abdomen"*/)
			)
		),

		new Combination(new Concept("C1267614"/*"Pancreatic duct and bile duct systems"*/),  newConceptArray(
				new Concept("C0030288"/*"Pancreatic duct"*/),
				new Concept("C0005400"/*"Bile duct"*/),
				new Concept("C0042425"/*"Ampulla of Vater"*/)
			)
		),
		
		new Combination(new Concept("C1267614"/*"Pancreatic duct and bile duct systems"*/),  newConceptArray(
				new Concept("C0030288"/*"Pancreatic duct"*/),
				new Concept("C0005400"/*"Bile duct"*/),
				new Concept("C0028872"/*"Sphinter of Oddi"*/)
			)
		),
		
		// deal with four pair combinations ...
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0817096"/*"Chest"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C0024109"/*"Lung"*/),
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1562459"/*"Neck and Chest"*/), 
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C0027530"/*"Neck"*/),
				new Concept("C1562547"/*"Chest, Abdomen and Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1562459"/*"Neck and Chest"*/), 
				new Concept("C0000726"/*"Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1562378"/*"Neck, Chest and Abdomen"*/),
				new Concept("C0030797"/*"Pelvis"*/)
			)
		),
		
		new Combination(new Concept("C1562776"/*"Neck, Chest, Abdomen and Pelvis"*/), newConceptArray(
				new Concept("C1562459"/*"Neck and Chest"*/), 
				new Concept("C1508499"/*"Abdomen and Pelvis"*/)
			)
		)
		
	};

	/**
	 * <p>Combine multiple concepts into a single concept containing all if possible.</p>
	 *
	 * @param	concepts	the concepts to combine
	 * @return				a combined concept if it exists, else null
	 */
	public static Concept getCombinedConcept(Concept[] concepts) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): comparing "+a+" with "+b);
		Concept combined = null;
		for (Combination combination : combinations) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): checking combination "+combination.parent);
			Set<Concept> used = new HashSet<Concept>();
			boolean hasSelf = false;
			boolean allIncluded = true;
			for (Concept concept : concepts) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): checking concept "+concept);
				if (combination.isSelf(concept)) {
					hasSelf = true;
				}
				else if (combination.contains(concept)) {
					used.add(concept);
				}
				else {
					allIncluded = false;
				}
			}
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): allIncluded = "+allIncluded);
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): hasSelf = "+hasSelf);
			boolean allUsed = hasSelf || used.size() == combination.size();	// need to be sure that ALL concepts in the combination were present
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): used.size() = "+used.size());
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): combination.size() = "+combination.size());
			if (allIncluded && allUsed) {
				combined = combination.parent;
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): found combination "+combined);
				break;
			}
		}
		
		return combined;
	}

	/**
	 * <p>Combine two concepts into a single concept containing both if possible.</p>
	 *
	 * @param	a	one concept
	 * @param	b	another concept
	 * @return		a combined concept if it exists, else null
	 */
	public static Concept getCombinedConcept(Concept a,Concept b) {
		Concept[] concepts = { a, b };
		return getCombinedConcept(concepts);
	}

	/**
	 * <p>Combine multiple concepts into a single concept containing all if possible.</p>
	 *
	 * @param	concepts	the concepts to combine
	 * @param	dict		dictionary of concepts to lookup
	 * @return				a combined concept if it exists and is present in the dictionary, else null
	 */
	public static DisplayableConcept getCombinedConcept(Concept[] concepts,DictionaryOfConcepts dict) {
		DisplayableConcept displayableCombined = null;
		if (dict != null) {
			Concept combined = getCombinedConcept(concepts);
			if (combined != null) {
//System.err.println("CombinedAnatomicConcepts.getCombinedConcept(): found combined concept "+combined+" now looking up in dictionary");
				displayableCombined = dict.find(combined);
			}
		}
		return displayableCombined;
	}

	/**
	 * <p>Combine two concepts into a single concept containing both if possible.</p>
	 *
	 * @param	a		one concept
	 * @param	b		another concept
	 * @param	dict	dictionary of concepts to lookup
	 * @return			a combined concept if it exists and is present in the dictionary, else null
	 */
	public static DisplayableConcept getCombinedConcept(Concept a,Concept b,DictionaryOfConcepts dict) {
		Concept[] concepts = { a, b };
		return getCombinedConcept(concepts,dict);
	}
}


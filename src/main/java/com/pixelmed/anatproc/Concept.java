/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

/**
 * <p>This class represents a concept that is uniquely identifiable.</p>
 * 
 * @author	dclunie
 */
public class Concept {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/Concept.java,v 1.7 2017/01/24 10:50:32 dclunie Exp $";
	
	protected String conceptUniqueIdentifier;		// usually a UMLS CUI
	
	public String getConceptUniqueIdentifier() { return conceptUniqueIdentifier; }
	
	/**
	 * <p>Create a concept.</p>
	 *
	 * @param	conceptUniqueIdentifier			the unique identifier of the concept, usually a UMLS CUI; required to be unique within the scope of comparisons using {@link #equals(Object) equals(Object)}
	 */
	public Concept(String conceptUniqueIdentifier) {
		this.conceptUniqueIdentifier=conceptUniqueIdentifier;
	}
	
	protected Concept() {};
	
	/**
	 * <p>Indicates whether some other object is "equal to" this one.</p>
	 *
	 * @param	o			the reference object with which to compare.
	 * @return				true if the same object or different objects with equal values of conceptUniqueIdentifier
	 */
	public boolean equals(Object o) {
		boolean areEqual = false;
		if (this == o) {
			areEqual = true;
		}
		else if (o != null && o instanceof Concept) {
			areEqual = conceptUniqueIdentifier.equals(((Concept)o).conceptUniqueIdentifier);
		}
		return areEqual;
	}
	
	public int hashCode() {
		return conceptUniqueIdentifier.hashCode();
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\tCUI: ");
		buf.append(conceptUniqueIdentifier);
		buf.append("\n");
		return buf.toString();
	}
}


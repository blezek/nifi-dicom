/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.CodedSequenceItem;

/**
 * <p>This class represents a concept by a combination of CodingSchemeDesignator and CodeValue.</p>
 * 
 * <p>Typically used as a index key with which to look up a concept (disregarding its text CodeMeaning).</p>
 * 
 * @see CodedConcept
 * @see DictionaryOfConcepts
 *
 * @author	dclunie
 */
public class SchemeAndValuePair {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/anatproc/SchemeAndValuePair.java,v 1.5 2017/01/24 10:50:33 dclunie Exp $";
	
	protected String codeValue;
	protected String codingSchemeDesignator;

	public SchemeAndValuePair(String codeValue,String codingSchemeDesignator) {
		this.codeValue=codeValue;
		this.codingSchemeDesignator=codingSchemeDesignator;
	}
		
	public SchemeAndValuePair(CodedSequenceItem item) {
		this.codeValue=item.getCodeValue();
		this.codingSchemeDesignator=item.getCodingSchemeDesignator();
	}
		
	public boolean equals(Object o) {
//System.err.println("SchemeAndValuePair.equals(): comparing "+this+" with "+o);
		if (o != null && o instanceof SchemeAndValuePair) {
			SchemeAndValuePair osvp = (SchemeAndValuePair)o;
//System.err.println("SchemeAndValuePair.equals(): comparing "+this+" with "+osvp);
			return codingSchemeDesignator != null && codingSchemeDesignator.equals(osvp.codingSchemeDesignator) && codeValue != null && codeValue.equals(osvp.codeValue);
		}
		else {
			return super.equals(o);
		}
	}
	
	public int hashCode() {
		return codeValue.hashCode() + codingSchemeDesignator.hashCode();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("(");
		buf.append(codeValue);
		buf.append(",");
		buf.append(codingSchemeDesignator);
		buf.append(")");
		return buf.toString();
	}
	
}

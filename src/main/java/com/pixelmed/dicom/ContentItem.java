/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * <p>An abstract class for representing a node in an internal representation of a structured reporting
 * tree (an instance of {@link com.pixelmed.dicom.StructuredReport StructuredReport}).</p>
 *
 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
 *
 * @see com.pixelmed.dicom.ContentItemFactory
 * @see com.pixelmed.dicom.ContentItemWithValue
 * @see com.pixelmed.dicom.ContentItemWithReference
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public abstract class ContentItem implements TreeNode {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItem.java,v 1.28 2017/01/24 10:50:36 dclunie Exp $";

	protected String relationshipType;

	ContentItem parent;
	List children;
	AttributeList list;

	// Methods to implement TreeNode ...

	/**
	 * <p>Returns the parent node of this node.</p>
	 *
	 * @return	the parent node, or null if the root
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * <p>Returns the child at the specified index.</p>
	 *
	 * @param	index	the index of the child to be returned, numbered from 0
	 * @return		the child <code>TreeNode</code> at the specified index
	 */
	public TreeNode getChildAt(int index) {
		return (TreeNode)(children.get(index));
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		int n=children.size();
		for (int i=0; i<n; ++i) {
			if (children.get(i).equals(child)) {	// expensive comparison ? :(
//System.err.println("getIndexOfChild: found "+child);
				return i;
			}
		}
		return -1;
	}

	/**
	 * <p> Always returns true, since children may always be added.</p>
	 *
	 * @return	always true
	 */
	public boolean getAllowsChildren() {
		return true;
	}

	/**
	 * <p> Returns true if the receiver is a leaf (has no children).</p>
	 *
	 * @return	true if the receiver is a leaf
	 */
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	/**
	 * <p>Return the number of children that this node contains.</p>
	 *
	 * @return	the number of children, 0 if none
	 */
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	/**
	 * <p>Returns the children of this node as an {@link java.util.Enumeration Enumeration}.</p>
	 *
	 * @return	the children of this node
	 */
	public Enumeration children() {
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...

	private void extractContentItemCommonAttributes() {
		relationshipType=Attribute.getSingleStringValueOrNull(list,TagFromName.RelationshipType);		// NB. Use null rather than default "" to make symmetric with de novo constructor
	}

	/**
	 * <p>Construct a content item for a list of attributes, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p	the parent
	 * @param	l	the list of attributes
	 */
	protected ContentItem(ContentItem p,AttributeList l) {
		parent=p;
		if (p != null) {
			p.addChild(this);
		}
		list=l;
		extractContentItemCommonAttributes();
	}

	/**
	 * <p>Construct a content item of a specified type and relationship, creating a new {@link com.pixelmed.dicom.AttributeList AttributeList}, and add it as a child of the specified parent.</p>
	 *
	 * <p>The constructor is protected. Instances of specific types of content items should normally be created by using
	 * the {@link com.pixelmed.dicom.ContentItemFactory ContentItemFactory}.</p>
	 *
	 * @param	p					the parent
	 * @param	relationshipType	added only if not null or zero length
	 * @throws	DicomException		if error in DICOM encoding
	 */
	protected ContentItem(ContentItem p,String relationshipType) throws DicomException {
		parent=p;
		if (p != null) {
			p.addChild(this);
		}
		list = new AttributeList();
		this.relationshipType = relationshipType;
		if (relationshipType != null && relationshipType.length() > 0) {
			Attribute a = new CodeStringAttribute(TagFromName.RelationshipType); a.addValue(relationshipType); list.put(a);
		}
	}

	/**
	 * <p>Add a child to this content item.</p>
	 *
	 * @param	child		the child content item to add
	 */
	public void addChild(ContentItem child) {
//System.err.println("ContentItem.addChild(): child = "+child);
		if (children == null) children=new LinkedList();
		children.add(child);
	}

	/**
	 * <p>Add a sibling to this content item (a child to the parent of this content item).</p>
	 *
	 * @param	sibling		the sibling content item to add
	 * @throws	DicomException	thrown if there is no parent
	 */
	public void addSibling(ContentItem sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Get the parent content item of this content item.</p>
	 *
	 * <p>This method saves the caller from having to cast the value returned from {@link javax.swing.tree.TreeNode#getParent() TreeNode.getParent()}.</p>
	 *
	 * @return	the parent content item
	 */
	public ContentItem getParentAsContentItem() { return parent; }

	/**
	 * <p>Get the attribute list of this content item.</p>
	 *
	 * @return	the attribute list of this content item
	 */
	public AttributeList getAttributeList() { return list; }

	/**
	 * <p>Get the value type of this content item.</p>
	 *
	 * @return	the value type (the string used in the DICOM standard in the Value Type attribute)
	 */
	public String getValueType()                { return null; }

	/**
	 * <p>Get the relationship type of this content item.</p>
	 *
	 * @return	the relationship type (the string used in the DICOM standard in the Relationship Type attribute)
	 */
	public String getRelationshipType()                { return relationshipType; }

	/**
	 * <p>Get the Referenced SOP Class UID of this content item, if present and applicable.</p>
	 *
	 * @return	the Referenced SOP Class UID, or null
	 */
	public String getReferencedSOPClassUID()    { return null; }

	/**
	 * <p>Get the Referenced SOP Instance UID of this content item, if present and applicable.</p>
	 *
	 * @return	the Referenced SOP Instance UID, or null
	 */
	public String getReferencedSOPInstanceUID() { return null; }

	/**
	 * <p>Get the Graphic Type of this content item, if present and applicable.</p>
	 *
	 * @return	the Graphic Type, or null
	 */
	public String getGraphicType()              { return null; }

	/**
	 * <p>Get the Graphic Data of this content item, if present and applicable.</p>
	 *
	 * @return	the Graphic Data, or null
	 */
	public float[] getGraphicData()             { return null; }

	/**
	 * <p>Get a string representation of the concept name and the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	public String getConceptNameAndValue()      { return null; }

	/**
	 * <p>Get a string representation of the value of the concept.</p>
	 *
	 * <p>The exact form of the returned string is specific to the type of ContentItem.</p>
	 *
	 * @return	a String representation of the name and value, or an empty string
	 */
	abstract public String getConceptValue();

	/**
	 * <p>Get the Concept Name.</p>
	 *
	 * @return	the Concept Name
	 */
	public CodedSequenceItem getConceptName()      { return null; }

	/**
	 * <p>Get the value of the code meaning of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code meaning of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeMeaning()      { return null; }

	/**
	 * <p>Get the value of the code value of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the code value of the Concept Name, or an empty string
	 */
	public String getConceptNameCodeValue()      { return null; }

	/**
	 * <p>Get the value of the coding scheme designator of the Concept Name as a string, if present and applicable.</p>
	 *
	 * @return	the coding scheme designator of the Concept Name, or an empty string
	 */
	public String getConceptNameCodingSchemeDesignator()      { return null; }

	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	the period (not backslash) delimited item references, or an empty string
	 */
	public String getReferencedContentItemIdentifier()      { return null; }

	/**
	 * <p>Get the Referenced Content Item Identifier, if present.</p>
	 *
	 * @return	an array of integers representing the separated components of the Referenced Content Item Identifier, including the first (root) identifier of 1, or null if none or empty
	 */
	public int[] getReferencedContentItemIdentifierArray()      { return null; }
	
	// Convenience methods
	
	/**
	 * <p>Get the position in the tree relative to the top parent as a String to use as a Referenced Content Item Identifier.</p>
	 *
	 * <p>Returns a valid result only if the entire parent content tree back to the root has already been populated.</p>
	 *
	 * @return	the period (not backslash) delimited item references, or "1" if we have no parent
	 */
	public String getPositionInTreeToUseAsReferencedContentItemIdentifier() {
		String contentItemIdentifier = "1";
		if (parent != null) {
			// which child are we ?
			int ourPositionFromOne = parent.getIndex(this) + 1;		// since getIndex() is from 0 and DICOM references count from 1
			contentItemIdentifier = parent.getPositionInTreeToUseAsReferencedContentItemIdentifier() + "." + ourPositionFromOne;
		}
		return contentItemIdentifier;
	}
	
	/**
	 * Retrieve the named child as defined by its ConceptName
	 *
	 * @param	codingSchemeDesignator	the coding scheme designator
	 * @param	codeValue				the code value
	 * @return							the (first, if multiple) named child, or null if absent
	 */
	public ContentItem getNamedChild(String codingSchemeDesignator,String codeValue) {
		ContentItem child = null;
		if (codingSchemeDesignator != null && codeValue != null) {
			int n = getChildCount();
			for (int i=0; i<n; ++i) {
				ContentItem test = (ContentItem)getChildAt(i);
				if (test != null) {
					String csd = test.getConceptNameCodingSchemeDesignator();
					String cv = test.getConceptNameCodeValue();
					if (csd != null && csd.equals(codingSchemeDesignator) && cv != null && cv.equals(codeValue)) {
						child = test;
						break;
					}
				}
			}
		}
		return child;
	}
	
	/**
	 * Retrieve the named child as defined by its ConceptName
	 *
	 * The code meaning of the concept is ignored, and only the code value and coding scheme designator are compared in the search.
	 *
	 * @param	item					the coded sequence item of the concept name wanted
	 * @return							the (first, if multiple) named child, or null if absent
	 */
	public ContentItem getNamedChild(CodedSequenceItem item) {
		String codingSchemeDesignator = item.getCodingSchemeDesignator();
		String codeValue = item.getCodeValue();
		ContentItem child = null;
		if (codingSchemeDesignator != null && codeValue != null) {
			int n = getChildCount();
			for (int i=0; i<n; ++i) {
				ContentItem test = (ContentItem)getChildAt(i);
				if (test != null) {
					String csd = test.getConceptNameCodingSchemeDesignator();
					String cv = test.getConceptNameCodeValue();
					if (csd != null && csd.equals(codingSchemeDesignator) && cv != null && cv.equals(codeValue)) {
						child = test;
						break;
					}
				}
			}
		}
		return child;
	}

	/**
	 * Retrieve the string value of self
	 *
	 * @return							the value , or null if absent
	 */
	public String getSingleStringValueOrNull() {
		String value = null;
		if (this instanceof ContentItemFactory.StringContentItem) {
			value = ((ContentItemFactory.StringContentItem)this).getConceptValue();
		}
		else if (this instanceof ContentItemFactory.CodeContentItem) {
			value = ((ContentItemFactory.CodeContentItem)this).getConceptValue();		// will return CodeMeaning
		}
		else if (this instanceof ContentItemFactory.NumericContentItem) {
			value = ((ContentItemFactory.NumericContentItem)this).getNumericValue();	// NOT getConceptValue(), which includes the units
		}
		return value;
	}

	/**
	 * Retrieve the string value of the named child as defined by its ConceptName
	 *
	 * @param	codingSchemeDesignator	the coding scheme designator
	 * @param	codeValue				the code value
	 * @return							the value of the (first, if multiple) named child, or null if absent
	 */
	public String getSingleStringValueOrNullOfNamedChild(String codingSchemeDesignator,String codeValue) {
		String value = null;
		{
			ContentItem child = getNamedChild(codingSchemeDesignator,codeValue);
			if (child != null) {
				value = child.getSingleStringValueOrNull();
			}
		}
		return value;
	}

	/**
	 * Retrieve the string value of the named child as defined by its ConceptName
	 *
	 * @param	parent					the parent
	 * @param	codingSchemeDesignator	the coding scheme designator
	 * @param	codeValue				the code value
	 * @return							the value of the (first, if multiple) named child, or null if absent
	 */
	public static String getSingleStringValueOrNullOfNamedChild(ContentItem parent,String codingSchemeDesignator,String codeValue) {
		String value = null;
		if (parent != null) {
			value = parent.getSingleStringValueOrNullOfNamedChild(codingSchemeDesignator,codeValue);
		}
		return value;
	}


	/**
	 * Test if the coded concept name of the content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * Does NOT follow references.
	 *
	 * @param	csdWanted		the coding scheme designator wanted
	 * @param	cvWanted		the code value wanted
	 * @return					true if matches
	 */
	public abstract boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted);
	
	/**
	 * Test if the coded concept name of the content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * Does NOT follow references.
	 *
	 * @param	ci				the content item to check
	 * @param	csdWanted		the coding scheme designator of the coded concept name wanted
	 * @param	cvWanted		the code value of the coded concept name wanted
	 * @return					true if matches
	 */
	public static boolean contentItemNameMatchesCodeValueAndCodingSchemeDesignator(ContentItem ci,String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (ci != null) {
			isMatch = ci.contentItemNameMatchesCodeValueAndCodingSchemeDesignator(cvWanted,csdWanted);
		}
		return isMatch;
	}
}




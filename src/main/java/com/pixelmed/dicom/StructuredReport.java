/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;

/**
 * <p>The {@link com.pixelmed.dicom.StructuredReport StructuredReport} class implements a
 * {@link javax.swing.tree.TreeModel TreeModel} to abstract the contents of a list of attributes
 * representing a DICOM Structured Report as
 * a tree in order to provide support for a {@link com.pixelmed.dicom.StructuredReportBrowser StructuredReportBrowser}.</p>
 *
 * <p>For details of some of the methods implemented here see {@link javax.swing.tree.TreeModel javax.swing.tree.TreeModel}.</p>
 *
 * <p>A main method is provided for testing that reads a DICOM file containing an SR and dumps its contents in a human-readable form to System.err.</p>
 *
 * @see com.pixelmed.dicom.ContentItem
 * @see com.pixelmed.dicom.ContentItemFactory
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public class StructuredReport implements TreeModel {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/StructuredReport.java,v 1.23 2017/01/24 10:50:39 dclunie Exp $";

	// Our nodes are all instances of ContentItem ...

	/***/
	private ContentItem root;

	// Stuff to support listener vector

	/***/
	private Vector listeners;

	// Methods for TreeModel

	/**
	 * @param	node
	 * @param	index
	 */
	public Object getChild(Object node,int index) {
		return ((ContentItem)node).getChildAt(index);
	}

	/**
	 * @param	parent
	 * @param	child
	 */
	public int getIndexOfChild(Object parent, Object child) {
		return ((ContentItem)parent).getIndex((ContentItem)child);
	}

	/***/
	public Object getRoot() { return root; }

	/**
	 * @param	parent
	 */
	public int getChildCount(Object parent) {
		return ((ContentItem)parent).getChildCount();
	}

	/**
	 * @param	node
	 */
	public boolean isLeaf(Object node) {
		return ((ContentItem)node).getChildCount() == 0;
	}

	/**
	 * @param	path
	 * @param	newValue
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	/**
	 * @param	tml
	 */
	public void addTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners = new Vector();
		listeners.addElement(tml);
	}

	/**
	 * @param	tml
	 */
	public void removeTreeModelListener(TreeModelListener tml) {
		if (listeners == null) listeners.removeElement(tml);
	}

	// Methods specific to StructuredReport

	/***/
	private ContentItemFactory nodeFactory;

	/**
	 * @param	parent
	 * @param	list
	 * @throws	DicomException
	 */
	private ContentItem processSubTree(ContentItem parent,AttributeList list) throws DicomException {
//System.err.println("processSubTree:");

		ContentItem node = nodeFactory.getNewContentItem(parent,list);

		if (list != null) {
			SequenceAttribute aContentSequence = (SequenceAttribute)(list.get(TagFromName.ContentSequence));
			if (aContentSequence != null) {
				Iterator i = aContentSequence.iterator();
				while (i.hasNext()) {
					SequenceItem item = ((SequenceItem)i.next());
					processSubTree(node,item == null ? null : item.getAttributeList());
				}
			}
		}

		return node;
	}

	/**
	 * <p>Construct an internal tree representation of a structured report from
	 * a list of DICOM attributes.</p>
	 *
	 * @param	list		the list of attributes in which the structured report is encoded
	 * @throws	DicomException
	 */
	public StructuredReport(AttributeList list) throws DicomException {

//System.err.println(list.toString());

		nodeFactory=new ContentItemFactory();
		root = processSubTree(null,list);
		nodeFactory=null;
	}

	/**
	 * <p>Construct an internal tree representation of a structured report from an existing root content item.</p>
	 *
	 * @param	root		the root content item
	 * @throws	DicomException
	 */
	public StructuredReport(ContentItem root) throws DicomException {
		this.root = root;
		nodeFactory=null;
	}
	

	/**
	 * @param	parentList
	 * @param	parentNode
	 */
	private void walkTreeAddingContentSequenceToChildAttributeLists(AttributeList parentList,ContentItem parentNode) {
		parentList.remove(TagFromName.ContentSequence);
		int n = getChildCount(parentNode);
		if (n > 0) {
			SequenceAttribute contentSequence = new SequenceAttribute(TagFromName.ContentSequence);
			parentList.put(contentSequence);
			for (int i=0; i<n; ++i) {
				ContentItem childNode = (ContentItem)getChild(parentNode,i);
				AttributeList nodeList = childNode.getAttributeList();
				contentSequence.addItem(nodeList);
				walkTreeAddingContentSequenceToChildAttributeLists(nodeList,childNode);
			}
		}
	}

	/**
	 * <p>Return the entire content tree of a structured report as an {@link com.pixelmed.dicom.AttributeList AttributeList}.</p>
	 *
	 * <p>Has the side effect of creating (or replacing) the ContentSequence attributes of the {@link com.pixelmed.dicom.AttributeList AttributeList} of each {@link com.pixelmed.dicom.ContentItem ContentItem} to represent the tree structure.</p>
	 *
	 * @return	a {@link com.pixelmed.dicom.AttributeList AttributeList} representing the content tree, or null if empty root.
	 */
	public AttributeList getAttributeList() {
		AttributeList list = null;
		if (root != null) {
			list = root.getAttributeList();
			walkTreeAddingContentSequenceToChildAttributeLists(list,root);
		}
		return list;
	}

	/**
	 * <p>Dump the tree starting at the specified node as a {@link java.lang.String String}.</p>
	 *
	 * @param	node
	 * @param	location	the dotted numeric string describing the location of the starting node
	 * @return	a multi-line {@link java.lang.String String} representing the tree fragment
	 */
	public static String walkTreeBuldingString(ContentItem node,String location) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(location);
		buffer.append(": ");
		buffer.append(node.toString());
		buffer.append("\n");

		int n = node.getChildCount();
		for (int i=0; i<n; ++i) buffer.append(walkTreeBuldingString((ContentItem)(node.getChildAt(i)),location+"."+Integer.toString(i+1)));

		return buffer.toString();
	}

	/**
	 * <p>Dump the entire tree as a {@link java.lang.String String}.</p>
	 *
	 * @return	a multi-line {@link java.lang.String String} representing the tree
	 */
	public String toString() {
		return walkTreeBuldingString(root,"1");
	}


	// Convenience methods and their supporting methods ...

	/**
	 * <p>Find all coordinate and image references within content items of the sub-tree rooted at the specified node (which may be the root).</p>
	 *
	 * <p>The annotation of the reference is derived from the value of the Concept Name of the parent (and its parent, if a NUM content item).</p>
	 *
	 * @param	root	the root node, for dereferencing by-reference relationships
	 * @param	item	the node to start searching from
	 * @return		a {@link java.util.Vector Vector} of {@link com.pixelmed.dicom.SpatialCoordinateAndImageReference SpatialCoordinateAndImageReference}
	 */
	public static Vector findAllContainedSOPInstances(ContentItem root,ContentItem item) {
		Vector instances = new Vector();
		String uidInstance = item.getReferencedSOPInstanceUID();
		String uidClass = item.getReferencedSOPClassUID();
		if (uidInstance == null) {
			int[] itemNumbers = item.getReferencedContentItemIdentifierArray();
			if (itemNumbers != null && itemNumbers.length > 0) {
				ContentItem referencedContentItem = root;
//System.err.println("StructuredReport.findAllContainedSOPInstances(): itemNumbers.length = "+itemNumbers.length);
				if (itemNumbers.length > 1) {					// skip the 1st item, which is always 1 to select the root content item
					for (int i=1; i<itemNumbers.length; i++) {
						int itemNumber = itemNumbers[i];
//System.err.println("StructuredReport.findAllContainedSOPInstances(): depth = "+i+" referenced item = "+itemNumber);
						if (referencedContentItem != null && itemNumber > 0 && itemNumber <= referencedContentItem.getChildCount()) {
							referencedContentItem = (ContentItem)(referencedContentItem.getChildAt(itemNumber-1));		// DICOM numbers items from 1, tree model numbers children from 0
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got child = "+referencedContentItem);
						}
						else {
//System.err.println("StructuredReport.findAllContainedSOPInstances(): failed to get child");
							referencedContentItem = null;
							break;
						}
					}
					if (referencedContentItem != null) {
						uidInstance = referencedContentItem.getReferencedSOPInstanceUID();
						uidClass = referencedContentItem.getReferencedSOPClassUID();
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got referenced instance "+uidInstance);
					}
				}
			}
		}
		if (uidInstance != null) {
//System.err.println("StructuredReport.findAllContainedSOPInstances(): checking for annotation on referenced instance "+uidInstance);
			ContentItem parent = item.getParentAsContentItem();
			String graphicType = null;
			float[] graphicData = null;
			String annotation=null;
			int render = SpatialCoordinateAndImageReference.RenderingRequired;						// always render unless explicitly stated otherwise
			int category = SpatialCoordinateAndImageReference.CoordinateCategoryUnspecified;
			boolean imageLibraryEntry = false;
			if (parent != null) {
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got parent "+parent);
				graphicType = parent.getGraphicType();
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got graphicType "+graphicType);
				graphicData = parent.getGraphicData();
				if (graphicType != null) parent = parent.getParentAsContentItem();					// want enclosing finding, whether scoord or not
				if (parent != null) {
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got parent "+parent);
					annotation=parent.getConceptNameAndValue();
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got annotation "+annotation);
					{
						ContentItem renderingIntentItem = parent.getNamedChild("DCM","111056");		// "Rendering Intent"; should also check relationship is HAS CONCEPT MOD :(
						if (renderingIntentItem != null && renderingIntentItem instanceof ContentItemFactory.CodeContentItem) {
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got renderingIntentItem "+renderingIntentItem);
							CodedSequenceItem renderingIntentCode = ((ContentItemFactory.CodeContentItem)renderingIntentItem).getConceptCode(); 
							String csd = renderingIntentCode.getCodingSchemeDesignator();
							String value = renderingIntentCode.getCodeValue();
							if (csd != null && csd.equals("DCM") && value != null) {
								if (value.equals("111150")) {										// "Presentation Required: Rendering device is expected to present"
									render = SpatialCoordinateAndImageReference.RenderingRequired;
								}
								else if (value.equals("111151")) {									// "Presentation Optional: Rendering device may present"
									render = SpatialCoordinateAndImageReference.RenderingOptional;
								}
								else if (value.equals("111152")) {									// "Not for Presentation:  Rendering device expected not to present"
									render = SpatialCoordinateAndImageReference.RenderingForbidden;
								}
								else {
									render = SpatialCoordinateAndImageReference.RenderingForbidden;
								}
							}
						}
					}
					if (parent instanceof ContentItemFactory.CodeContentItem) {
						String conceptCsd = parent.getConceptNameCodingSchemeDesignator();
						String conceptValue = parent.getConceptNameCodeValue();
						if (conceptCsd != null && conceptCsd.equals("DCM") && conceptValue != null && conceptValue.equals("111059")) {		// "Single Image Finding"
//System.err.println("StructuredReport.findAllContainedSOPInstances(): got Single Image Finding");
							CodedSequenceItem valueCode = ((ContentItemFactory.CodeContentItem)parent).getConceptCode(); 
							String valueCsd = valueCode.getCodingSchemeDesignator();
							String valueValue = valueCode.getCodeValue();
							if (valueCsd != null && valueCsd.equals("SRT") && valueValue != null) {
								if (valueValue.equals("F-01796")) {										// "Mammography breast density"
									category = SpatialCoordinateAndImageReference.CoordinateCategoryMammoBreastDensity;
								}
								else if (valueValue.equals("F-01775")) {								// "Calcification Cluster"
									category = SpatialCoordinateAndImageReference.CoordinateCategoryMammoCalcificationCluster;
								}
								else if (valueValue.equals("F-01776")) {								// "Individual Calcification"
									category = SpatialCoordinateAndImageReference.CoordinateCategoryMammoIndividualCalcification;
								}
								// else leave unspecified
							}
						}
					}
					else if (parent instanceof ContentItemFactory.NumericContentItem) {
						// go up one more, and as long as it is not black listed, add the parent concept name and value too
						parent = parent.getParentAsContentItem();
						if (parent != null) {
							String conceptCsd = parent.getConceptNameCodingSchemeDesignator();
							String conceptValue = parent.getConceptNameCodeValue();
							if (conceptCsd != null && conceptValue != null) {
								if (!conceptCsd.equals("99RPH") || !(
									   conceptValue.equals("RP-101002")			// "Simple Measurement"
									)) {
										annotation=parent.getConceptNameAndValue()+" "+annotation;
								}
							}
						}
					}
					else if (parent instanceof ContentItemFactory.ContainerContentItem) {
						{
							String conceptCsd = parent.getConceptNameCodingSchemeDesignator();
							String conceptValue = parent.getConceptNameCodeValue();
							if (conceptCsd != null && conceptCsd.equals("DCM") && conceptValue != null && conceptValue.equals("111028")) {		// "Image Library"
								imageLibraryEntry = true;
							}
						}
						// also, see if there are any relevant sibling content items that should be included in the annotation too ... (e.g., voluem for an image sub-region)
						// TBD ... :(
					}
				}
			}
			instances.add(new SpatialCoordinateAndImageReference(uidInstance,uidClass,graphicType,graphicData,annotation,render,category,imageLibraryEntry));
		}
		int nChildren = item.getChildCount();
		for (int i=0; i<nChildren; ++i) {
			ContentItem child=(ContentItem)(item.getChildAt(i));
			instances.addAll(findAllContainedSOPInstances(root,child));
		}
		return instances;
	}
	
	/**
	 * <p>Dump the SR encoded in the file name on the console.</p>
	 *
	 * @param	arg
	 */
	public static void main(String arg[]) {
		try {
			AttributeList list = new AttributeList();
			list.read(arg[0]);
			StructuredReport sr = new StructuredReport(list);
			System.err.println(sr);
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}





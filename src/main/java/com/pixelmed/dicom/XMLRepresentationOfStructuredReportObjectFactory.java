/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FloatFormatter;
import com.pixelmed.utils.HexDump;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to encode a representation of a DICOM Structured Report object in an XML form,
 * suitable for analysis as human-readable text, or for feeding into an
 * XSLT-based validator.</p>
 *
 * <p>Note that XML representations can either contain only the content tree, or also the additional
 * top level DICOM attributes other than those that encode the content tree, as individual
 * DICOM attributes, in the manner of {@link com.pixelmed.dicom.XMLRepresentationOfDicomObjectFactory XMLRepresentationOfDicomObjectFactory}.</p>
 *
 * <p>A typical example of usage to extract just the content tree would be:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
	StructuredReport sr = new StructuredReport(list);
    Document document = new XMLRepresentationOfStructuredReportObjectFactory().getDocument(sr);
    XMLRepresentationOfStructuredReportObjectFactory.write(System.out,document);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or to include the top level attributes as well as the content tree, supply the attribute
 * list as well as the parsed SR content to the write() method:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
	StructuredReport sr = new StructuredReport(list);
    Document document = new XMLRepresentationOfStructuredReportObjectFactory().getDocument(sr,list);
    XMLRepresentationOfStructuredReportObjectFactory.write(System.out,document);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or even simpler, if there is no further use for the XML document or the SR tree model:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
    XMLRepresentationOfStructuredReportObjectFactory.createDocumentAndWriteIt(list,System.out);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.XMLRepresentationOfDicomObjectFactory
 * @see com.pixelmed.utils.XPathQuery
 * @see org.w3c.dom.Document
 *
 * @author	dclunie
 */
public class XMLRepresentationOfStructuredReportObjectFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/XMLRepresentationOfStructuredReportObjectFactory.java,v 1.29 2017/05/16 23:53:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(XMLRepresentationOfStructuredReportObjectFactory.class);

	protected static String contentItemIdentiferPrefix = "ci_";

	/***/
	private DocumentBuilder db;
	
	private void addCodedConceptAttributesToDocumentNode(org.w3c.dom.Node documentNode,Document document,CodedSequenceItem codedConcept) {
		if (codedConcept != null) {
			String codeMeaning = codedConcept.getCodeMeaning();
			if (codeMeaning != null && codeMeaning.length() > 0) {
				Attr attr = document.createAttribute("cm");
				attr.setValue(codeMeaning);
				documentNode.getAttributes().setNamedItem(attr);
			}
			
			String codeValue = codedConcept.getCodeValue();
			if (codeValue != null && codeValue.length() > 0) {
				Attr attr = document.createAttribute("cv");
				attr.setValue(codeValue);
				documentNode.getAttributes().setNamedItem(attr);
			}
			
			String codingSchemeDesignator = codedConcept.getCodingSchemeDesignator();
			if (codingSchemeDesignator != null && codingSchemeDesignator.length() > 0) {
				Attr attr = document.createAttribute("csd");
				attr.setValue(codingSchemeDesignator);
				documentNode.getAttributes().setNamedItem(attr);
			}
			
			String codingSchemeVersion = codedConcept.getCodingSchemeVersion();
			if (codingSchemeVersion != null && codingSchemeVersion.length() > 0) {
				Attr attr = document.createAttribute("csv");
				attr.setValue(codingSchemeVersion);
				documentNode.getAttributes().setNamedItem(attr);
			}
		}
	}
	
	/**
	 * @param	contentItem		content item node of the Structured Report
	 * @param	document
	 * @param	documentParent	the node of the document to add to
	 * @param	id				the id of the content item
	 */
	private void addContentItemsFromTreeToNode(ContentItem contentItem,Document document,org.w3c.dom.Node documentParent,String id) {
		if (contentItem != null) {
			org.w3c.dom.Node documentNode = null;

			String valueType = contentItem.getValueType();
			if (valueType != null && valueType.length() > 0) {
				String elementName = valueType.toLowerCase(Locale.US);		// Locale explicitly specified, otherwise will fail to give the desired result if the default Locale is Turkish ("tr" or "tr_TR"); see "http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#toLowerCase%28%29"
				documentNode = document.createElement(elementName);
			}
			else {
				documentNode = document.createElement("reference");
			}
			
			if (documentNode != null) {
				documentParent.appendChild(documentNode);

				if (id != null && id.length() > 0) {
					Attr attr = document.createAttribute("ID");
					attr.setValue(contentItemIdentiferPrefix+id);
					documentNode.getAttributes().setNamedItem(attr);
				}
				
				String relationshipType = contentItem.getRelationshipType();
				if (relationshipType != null && relationshipType.length() > 0) {
					Attr attr = document.createAttribute("relationship");
					attr.setValue(relationshipType);
					documentNode.getAttributes().setNamedItem(attr);
				}
				
				String referencedContentItemIdentifier = contentItem.getReferencedContentItemIdentifier();
				if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
					Attr attr = document.createAttribute("IDREF");
					attr.setValue(contentItemIdentiferPrefix+referencedContentItemIdentifier);
					documentNode.getAttributes().setNamedItem(attr);
				}
				
				CodedSequenceItem conceptName = contentItem.getConceptName();
				if (conceptName != null) {
					org.w3c.dom.Node conceptNameNode = document.createElement("concept");
					documentNode.appendChild(conceptNameNode);
					addCodedConceptAttributesToDocumentNode(conceptNameNode,document,conceptName);
				}
				
				if (contentItem instanceof ContentItemFactory.ContainerContentItem) {
					String continuityOfContent = ((ContentItemFactory.ContainerContentItem)contentItem).getContinuityOfContent();
					if (continuityOfContent != null && continuityOfContent.length() > 0) {
						Attr attr = document.createAttribute("continuity");
						attr.setValue(continuityOfContent);
						documentNode.getAttributes().setNamedItem(attr);
					}
					String templateMappingResource = ((ContentItemFactory.ContainerContentItem)contentItem).getTemplateMappingResource();
					if (templateMappingResource != null && templateMappingResource.length() > 0) {
						Attr attr = document.createAttribute("templatemappingresource");
						attr.setValue(templateMappingResource);
						documentNode.getAttributes().setNamedItem(attr);
					}
					String templateIdentifier = ((ContentItemFactory.ContainerContentItem)contentItem).getTemplateIdentifier();
					if (templateIdentifier != null && templateIdentifier.length() > 0) {
						Attr attr = document.createAttribute("template");
						attr.setValue(templateIdentifier);
						documentNode.getAttributes().setNamedItem(attr);
					}
				}
				else if (contentItem instanceof ContentItemFactory.CodeContentItem) {
					CodedSequenceItem conceptCode = ((ContentItemFactory.CodeContentItem)contentItem).getConceptCode();
					if (conceptCode != null) {
						org.w3c.dom.Node valueNode = document.createElement("value");
						documentNode.appendChild(valueNode);
						addCodedConceptAttributesToDocumentNode(valueNode,document,conceptCode);
					}
				}
				else if (contentItem instanceof ContentItemFactory.NumericContentItem) {
					String value = ((ContentItemFactory.NumericContentItem)contentItem).getNumericValue();
					if (value != null && value.length() > 0) {
						org.w3c.dom.Node valueNode = document.createElement("value");
						documentNode.appendChild(valueNode);
						valueNode.appendChild(document.createTextNode(value));
					}
					if (((ContentItemFactory.NumericContentItem)contentItem).hasFloatingPointValue()) {
						double floatingPointValue = ((ContentItemFactory.NumericContentItem)contentItem).getFloatingPointValue();
						//String floatingPointValueAsString = getDoubleAsBase64ForXML(floatingPointValue);
						String floatingPointValueAsString = Double.toString(floatingPointValue);	// this will be as long as is needed to uniquely distinguish this double from its neighbors
						if (floatingPointValueAsString != null && floatingPointValueAsString.length() > 0) {
							org.w3c.dom.Node valueNode = document.createElement("double");
							documentNode.appendChild(valueNode);
							valueNode.appendChild(document.createTextNode(floatingPointValueAsString));
							// we could include an attribute type="xsd:double" if we wanted to reference an XSD datatype
						}
					}
					if (((ContentItemFactory.NumericContentItem)contentItem).hasRationalValue()) {
						String numerator = Integer.toString(((ContentItemFactory.NumericContentItem)contentItem).getRationalNumeratorValue());
						if (numerator != null && numerator.length() > 0) {
							org.w3c.dom.Node valueNode = document.createElement("numerator");
							documentNode.appendChild(valueNode);
							valueNode.appendChild(document.createTextNode(numerator));
						}
						String denominator = Long.toString(((ContentItemFactory.NumericContentItem)contentItem).getRationalDenominatorValue());
						if (denominator != null && denominator.length() > 0) {
							org.w3c.dom.Node valueNode = document.createElement("denominator");
							documentNode.appendChild(valueNode);
							valueNode.appendChild(document.createTextNode(denominator));
						}
					}
					CodedSequenceItem unitsCode = ((ContentItemFactory.NumericContentItem)contentItem).getUnits();
					if (unitsCode != null) {
						org.w3c.dom.Node unitsCodeNode = document.createElement("units");
						documentNode.appendChild(unitsCodeNode);
						addCodedConceptAttributesToDocumentNode(unitsCodeNode,document,unitsCode);
					}
					CodedSequenceItem qualifier = ((ContentItemFactory.NumericContentItem)contentItem).getQualifier();
					if (qualifier != null) {
						org.w3c.dom.Node qualifierNode = document.createElement("qualifier");
						documentNode.appendChild(qualifierNode);
						addCodedConceptAttributesToDocumentNode(qualifierNode,document,qualifier);
					}
				}
				else if (contentItem instanceof ContentItemFactory.StringContentItem) {
					String value = ((ContentItemFactory.StringContentItem)contentItem).getConceptValue();
					if (value != null && value.length() > 0) {
						org.w3c.dom.Node valueNode = document.createElement("value");
						documentNode.appendChild(valueNode);
						valueNode.appendChild(document.createTextNode(value));
					}
				}
				else if (contentItem instanceof ContentItemFactory.SpatialCoordinatesContentItem) {
					String graphicType = ((ContentItemFactory.SpatialCoordinatesContentItem)contentItem).getGraphicType();
					if (graphicType != null) {	// regardless of whether zero length or not, need node to append data to
						org.w3c.dom.Node graphicTypeNode = document.createElement(graphicType.toLowerCase(Locale.US));		// Locale explicitly specified, otherwise will fail to give the desired result if the default Locale is Turkish ("tr" or "tr_TR"); see "http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#toLowerCase%28%29"
						documentNode.appendChild(graphicTypeNode);
						float[] graphicData = ((ContentItemFactory.SpatialCoordinatesContentItem)contentItem).getGraphicData();
						if (graphicData != null) {
							for (int i=0; i<graphicData.length; ++i) {
								org.w3c.dom.Node coordinateNode = document.createElement(i%2 == 0 ? "x" : "y");
								graphicTypeNode.appendChild(coordinateNode);
								coordinateNode.appendChild(document.createTextNode(FloatFormatter.toStringOfFixedMaximumLength(graphicData[i],16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US)));
							}
						}
					}
				}
				else if (contentItem instanceof ContentItemFactory.SpatialCoordinates3DContentItem) {
					String graphicType = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getGraphicType();
					if (graphicType != null) {	// regardless of whether zero length or not, need node to append data to
						org.w3c.dom.Node graphicTypeNode = document.createElement(graphicType.toLowerCase(Locale.US));		// Locale explicitly specified, otherwise will fail to give the desired result if the default Locale is Turkish ("tr" or "tr_TR"); see "http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#toLowerCase%28%29"
						documentNode.appendChild(graphicTypeNode);
						float[] graphicData = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getGraphicData();
						if (graphicData != null) {
							for (int i=0; i<graphicData.length; ++i) {
								int xyz = i%3;
								org.w3c.dom.Node coordinateNode = document.createElement(xyz == 0 ? "x" : (xyz == 1 ? "y" : "z"));
								graphicTypeNode.appendChild(coordinateNode);
								coordinateNode.appendChild(document.createTextNode(FloatFormatter.toStringOfFixedMaximumLength(graphicData[i],16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US)));
							}
						}
					}
					String referencedFrameOfReferenceUID = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getReferencedFrameOfReferenceUID();
					if (referencedFrameOfReferenceUID != null && referencedFrameOfReferenceUID.length() > 0) {
						org.w3c.dom.Node referencedFrameOfReferenceNode = document.createElement("frameofreference");
						documentNode.appendChild(referencedFrameOfReferenceNode);
						referencedFrameOfReferenceNode.appendChild(document.createTextNode(referencedFrameOfReferenceUID));
					}
				}
				else if (contentItem instanceof ContentItemFactory.TemporalCoordinatesContentItem) {
					ContentItemFactory.TemporalCoordinatesContentItem temporalCoordinatesContentItem = (ContentItemFactory.TemporalCoordinatesContentItem)contentItem;
					String temporalRangeType = temporalCoordinatesContentItem.getTemporalRangeType();
					if (temporalRangeType != null) {	// regardless of whether zero length or not, need node to append data to
						org.w3c.dom.Node temporalRangeTypeNode = document.createElement(temporalRangeType.toLowerCase(Locale.US));		// Locale explicitly specified, otherwise will fail to give the desired result if the default Locale is Turkish ("tr" or "tr_TR"); see "http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#toLowerCase%28%29"
						documentNode.appendChild(temporalRangeTypeNode);
						{
							int[] referencedSamplePositions = temporalCoordinatesContentItem.getReferencedSamplePositions();
							if (referencedSamplePositions != null) {
								org.w3c.dom.Node referencedSamplePositionsNode = document.createElement("samplepositions");
								temporalRangeTypeNode.appendChild(referencedSamplePositionsNode);
								for (int i=0; i<referencedSamplePositions.length; ++i) {
									org.w3c.dom.Node referencedSamplePositionNode = document.createElement("position");
									referencedSamplePositionsNode.appendChild(referencedSamplePositionNode);
									referencedSamplePositionNode.appendChild(document.createTextNode(Integer.toString(referencedSamplePositions[i])));
								}
							}
						}
						{
							float[] referencedTimeOffsets = temporalCoordinatesContentItem.getReferencedTimeOffsets();
							if (referencedTimeOffsets != null) {
								org.w3c.dom.Node referencedTimeOffsetsNode = document.createElement("timeoffsets");
								temporalRangeTypeNode.appendChild(referencedTimeOffsetsNode);
								for (int i=0; i<referencedTimeOffsets.length; ++i) {
									org.w3c.dom.Node referencedTimeOffsetNode = document.createElement("offset");
									referencedTimeOffsetsNode.appendChild(referencedTimeOffsetNode);
									referencedTimeOffsetNode.appendChild(document.createTextNode(FloatFormatter.toString(referencedTimeOffsets[i],Locale.US)));
								}
							}
						}
						{
							String[] referencedDateTimes = temporalCoordinatesContentItem.getReferencedDateTimes();
							if (referencedDateTimes != null) {
								org.w3c.dom.Node referencedDateTimesNode = document.createElement("datetimes");
								temporalRangeTypeNode.appendChild(referencedDateTimesNode);
								for (int i=0; i<referencedDateTimes.length; ++i) {
									org.w3c.dom.Node referencedDateTimeNode = document.createElement("datetime");
									referencedDateTimesNode.appendChild(referencedDateTimeNode);
									referencedDateTimeNode.appendChild(document.createTextNode(referencedDateTimes[i]));
								}
							}
						}
					}
				}
				else if (contentItem instanceof ContentItemFactory.CompositeContentItem) {
					{
						String referencedSOPClassUID = ((ContentItemFactory.CompositeContentItem)contentItem).getReferencedSOPClassUID();
						if (referencedSOPClassUID != null && referencedSOPClassUID.length() > 0) {
							org.w3c.dom.Node referencedSOPClassUIDNode = document.createElement("class");
							documentNode.appendChild(referencedSOPClassUIDNode);
							referencedSOPClassUIDNode.appendChild(document.createTextNode(referencedSOPClassUID));
						}
					}
					{
						String referencedSOPInstanceUID = ((ContentItemFactory.CompositeContentItem)contentItem).getReferencedSOPInstanceUID();
						if (referencedSOPInstanceUID != null && referencedSOPInstanceUID.length() > 0) {
							org.w3c.dom.Node referencedSOPInstanceUIDNode = document.createElement("instance");
							documentNode.appendChild(referencedSOPInstanceUIDNode);
							referencedSOPInstanceUIDNode.appendChild(document.createTextNode(referencedSOPInstanceUID));
						}
					}
					if (contentItem instanceof ContentItemFactory.ImageContentItem) {
						ContentItemFactory.ImageContentItem imageContentItem = (ContentItemFactory.ImageContentItem)contentItem;
						{
							int referencedFrameNumber = imageContentItem.getReferencedFrameNumber();
							if (referencedFrameNumber != 0) {
								org.w3c.dom.Node referencedFrameNumberNode = document.createElement("frame");
								documentNode.appendChild(referencedFrameNumberNode);
								referencedFrameNumberNode.appendChild(document.createTextNode(Integer.toString(referencedFrameNumber)));
							}
						}
						{
							int referencedSegmentNumber = imageContentItem.getReferencedSegmentNumber();
							if (referencedSegmentNumber != 0) {
								org.w3c.dom.Node referencedSegmentNumberNode = document.createElement("segment");
								documentNode.appendChild(referencedSegmentNumberNode);
								referencedSegmentNumberNode.appendChild(document.createTextNode(Integer.toString(referencedSegmentNumber)));
							}
						}
						{
							String presentationStateSOPClassUID = imageContentItem.getPresentationStateSOPClassUID();
							String presentationStateSOPInstanceUID = imageContentItem.getPresentationStateSOPInstanceUID();
							if (presentationStateSOPClassUID != null && presentationStateSOPClassUID.length() > 0
							 || presentationStateSOPInstanceUID != null && presentationStateSOPInstanceUID.length() > 0) {
								org.w3c.dom.Node presentationStateReference = document.createElement("presentationstate");
								documentNode.appendChild(presentationStateReference);
								
								org.w3c.dom.Node presentationStateSOPClassUIDNode = document.createElement("class");
								presentationStateReference.appendChild(presentationStateSOPClassUIDNode);
								presentationStateSOPClassUIDNode.appendChild(document.createTextNode(presentationStateSOPClassUID));
								
								org.w3c.dom.Node presentationStateSOPInstanceUIDNode = document.createElement("instance");
								presentationStateReference.appendChild(presentationStateSOPInstanceUIDNode);
								presentationStateSOPInstanceUIDNode.appendChild(document.createTextNode(presentationStateSOPInstanceUID));
							}
						}
						{
							String realWorldValueMappingSOPClassUID = imageContentItem.getRealWorldValueMappingSOPClassUID();
							String realWorldValueMappingSOPInstanceUID = imageContentItem.getRealWorldValueMappingSOPInstanceUID();
							if (realWorldValueMappingSOPClassUID != null && realWorldValueMappingSOPClassUID.length() > 0
							 || realWorldValueMappingSOPInstanceUID != null && realWorldValueMappingSOPInstanceUID.length() > 0) {
								org.w3c.dom.Node realWorldValueMappingReference = document.createElement("realworldvaluemapping");
								documentNode.appendChild(realWorldValueMappingReference);
								
								org.w3c.dom.Node realWorldValueMappingSOPClassUIDNode = document.createElement("class");
								realWorldValueMappingReference.appendChild(realWorldValueMappingSOPClassUIDNode);
								realWorldValueMappingSOPClassUIDNode.appendChild(document.createTextNode(realWorldValueMappingSOPClassUID));
								
								org.w3c.dom.Node realWorldValueMappingSOPInstanceUIDNode = document.createElement("instance");
								realWorldValueMappingReference.appendChild(realWorldValueMappingSOPInstanceUIDNode);
								realWorldValueMappingSOPInstanceUIDNode.appendChild(document.createTextNode(realWorldValueMappingSOPInstanceUID));
							}
						}
						// forget about icon image sequence for now :(
					}
					else if (contentItem instanceof ContentItemFactory.WaveformContentItem) {
						int[] referencedWaveformChannels = ((ContentItemFactory.WaveformContentItem)contentItem).getReferencedWaveformChannels();
						if (referencedWaveformChannels != null && referencedWaveformChannels.length > 0) {
							org.w3c.dom.Node referencedWaveformChannelsNode = document.createElement("channels");
							documentNode.appendChild(referencedWaveformChannelsNode);
							for (int i=0; i<referencedWaveformChannels.length; ++i) {
								org.w3c.dom.Node channelNode = document.createElement("channel");
								referencedWaveformChannelsNode.appendChild(channelNode);
								channelNode.appendChild(document.createTextNode(Integer.toString(referencedWaveformChannels[i])));
							}
						}
					}
				}
				
				// now handle any children
				int n = contentItem.getChildCount();
				for (int i=0; i<n; ++i) {
					addContentItemsFromTreeToNode((ContentItem)(contentItem.getChildAt(i)),document,documentNode,id+"."+Integer.toString(i+1));
				}
			}
		}
	}

	/**
	 * <p>Construct a factory object, which can be used to get XML documents from DICOM objects.</p>
	 *
	 * @throws	ParserConfigurationException
	 */
	public XMLRepresentationOfStructuredReportObjectFactory() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		db = dbf.newDocumentBuilder();
	}
	
	/**
	 * <p>Given a DICOM attribute list encoding a Structured Report, get an XML document as a DOM tree.</p>
	 *
	 * @param	list	the attribute list
	 */
	public Document getDocument(AttributeList list) {
		return getDocument(null,list);
	}
	
	/**
	 * <p>Given a DICOM Structured Report, get an XML document of the content tree only as a DOM tree.</p>
	 *
	 * @param	sr		the Structured Report
	 */
	public Document getDocument(StructuredReport sr) {
		return getDocument(sr,null);
	}
	
	/**
	 * <p>Given a DICOM Structured Report, get an XML document of the content tree and the top level DICOM elements as a DOM tree.</p>
	 *
	 * @param	sr		the Structured Report			may be null if list is not - will build an sr tree model
	 * @param	list	the attribute list				may be null if only the sr content tree is to be added
	 */
	public Document getDocument(StructuredReport sr,AttributeList list) {
		if (sr == null) {
			try {
				sr = new StructuredReport(list);
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
		Document document = db.newDocument();
		org.w3c.dom.Node rootElement = document.createElement("DicomStructuredReport");
		document.appendChild(rootElement);
		if (list != null) {
			AttributeList clonedList = (AttributeList)(list.clone());
 			clonedList.removePrivateAttributes();
 			clonedList.removeGroupLengthAttributes();
 			clonedList.removeMetaInformationHeaderAttributes();
 			clonedList.remove(TagFromName.ContentSequence);
			clonedList.remove(TagFromName.ValueType);
			clonedList.remove(TagFromName.ContentTemplateSequence);
			clonedList.remove(TagFromName.ContinuityOfContent);
			clonedList.remove(TagFromName.ConceptNameCodeSequence);
			org.w3c.dom.Node headerElement = document.createElement("DicomStructuredReportHeader");
			rootElement.appendChild(headerElement);
			try {
				new XMLRepresentationOfDicomObjectFactory().addAttributesFromListToNode(clonedList,document,headerElement);
			}
			catch (ParserConfigurationException e) {
				slf4jlogger.error("",e);
			}
		}
		if (sr != null) {
			org.w3c.dom.Node contentElement = document.createElement("DicomStructuredReportContent");
			rootElement.appendChild(contentElement);
			addContentItemsFromTreeToNode((ContentItem)(sr.getRoot()),document,contentElement,"1");
		}
		return document;
	}
	
	protected ContentItemFactory contentItemFactory;
	
	public static String getNamedNodeAttributeOrNull(NamedNodeMap attributes,String name) {
		String string = null;
		if (attributes != null) {
			Node node = attributes.getNamedItem(name);
			if (node != null) {
				string = node.getNodeValue();
			}
		}
		return string;
	}
	
	public static Node getNamedChildElement(Node parent,String name) {
		Node node = parent.getFirstChild();
		while (node != null) {
			String nodeName = node.getNodeName();
			if (node.getNodeType() == Node.ELEMENT_NODE && nodeName != null && nodeName.equals(name)) {
				return node;
			}
			node = node.getNextSibling();
		}
		return null;
	}
	
	public static String getTextValueOfNamedChildElementOrNull(Node parent,String name) {
		String value = null;
		Node node = getNamedChildElement(parent,name);
		if (node != null) {
			value = node.getTextContent();
			if (value != null) {
				value = value.trim();
			}
		}
		return value;
	}
	
	protected CodedSequenceItem getCodedSequenceItem(Node node) throws DicomException {
		CodedSequenceItem item = null;
		if (node != null) {
			String codeValue = "";
			String codingSchemeDesignator = "";
			String codingSchemeVersion = null;
			String codeMeaning = "";
			NamedNodeMap attributes = node.getAttributes();
			if (attributes != null) {
				codeValue = getNamedNodeAttributeOrNull(attributes,"cv");
//System.err.println("getCodedSequenceItem(): codeValue = "+codeValue);
				codingSchemeDesignator = getNamedNodeAttributeOrNull(attributes,"csd");
//System.err.println("getCodedSequenceItem(): codingSchemeDesignator = "+codingSchemeDesignator);
				codeMeaning = getNamedNodeAttributeOrNull(attributes,"cm");
//System.err.println("getCodedSequenceItem(): codeMeaning = "+codeMeaning);
				codingSchemeVersion = getNamedNodeAttributeOrNull(attributes,"csv");
//System.err.println("getCodedSequenceItem(): codingSchemeVersion = "+codingSchemeVersion);
				if (codeValue == null || codeValue.length() == 0
				 || codingSchemeDesignator == null || codingSchemeDesignator.length() == 0
				 || codeMeaning == null || codeMeaning.length() == 0) {
					throw new DicomException("Need at least Code Value ("+codeValue+"), Coding Scheme Designator ("+codingSchemeDesignator+") and Code Meaning ("+codeMeaning+") to build CodedSequenceItem - cannot be absent or empty");
				}
			}
			if (codingSchemeVersion == null || codingSchemeVersion.length() == 0) {
				item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codeMeaning);
			}
			else {
				item = new CodedSequenceItem(codeValue,codingSchemeDesignator,codingSchemeVersion,codeMeaning);
			}
		}
		return item;
	}
	
	protected ContentItem getNextContentItemFromXMLNodeSiblings(Node node) throws DicomException {
		while (node != null) {
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): node = "+node);
			String nodeName = node.getNodeName();
			if (node.getNodeType() == Node.ELEMENT_NODE && nodeName != null) {
				NamedNodeMap attributes = node.getAttributes();
				CodedSequenceItem concept = getCodedSequenceItem(getNamedChildElement(node,"concept"));
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): concept = "+concept);
				String relationshipType = getNamedNodeAttributeOrNull(attributes,"relationship");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): relationshipType = "+relationshipType);
				ContentItem contentItem = null;
				if (nodeName.equals("container")) {
					String continuity = getNamedNodeAttributeOrNull(attributes,"continuity");
					String template = getNamedNodeAttributeOrNull(attributes,"template");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): template = "+template);
					String templatemappingresource = getNamedNodeAttributeOrNull(attributes,"templatemappingresource");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): templatemappingresource = "+templatemappingresource);
					contentItem = contentItemFactory.makeContainerContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						continuity != null && continuity.equals("SEPARATE"),
						templatemappingresource,template);
				}
				else if (nodeName.equals("code")) {
					CodedSequenceItem value = getCodedSequenceItem(getNamedChildElement(node,"value"));
					contentItem = contentItemFactory.makeCodeContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("num")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					CodedSequenceItem units = getCodedSequenceItem(getNamedChildElement(node,"units"));
					CodedSequenceItem qualifier = getCodedSequenceItem(getNamedChildElement(node,"qualifier"));
					Double floatingPointValue = null;
					{
						String s = getTextValueOfNamedChildElementOrNull(node,"double");
						if (s != null && s.length() > 0) {
							try {
								double d = Double.parseDouble(s);
								floatingPointValue = new Double(d);
							}
							catch (NumberFormatException e) {
								slf4jlogger.error("",e);
							}
						}
					}
					Integer rationalNumeratorValue = null;
					{
						String s = getTextValueOfNamedChildElementOrNull(node,"numerator");
						if (s != null && s.length() > 0) {
							try {
								int i = Integer.parseInt(s);
								rationalNumeratorValue = new Integer(i);
							}
							catch (NumberFormatException e) {
								slf4jlogger.error("",e);
							}
						}
					}
					Long rationalDenominatorValue = null;
					{
						String s = getTextValueOfNamedChildElementOrNull(node,"denominator");
						if (s != null && s.length() > 0) {
							try {
								long l = Long.parseLong(s);
								rationalDenominatorValue = new Long(l);
							}
							catch (NumberFormatException e) {
								slf4jlogger.error("",e);
							}
						}
					}
					contentItem = contentItemFactory.makeNumericContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value,
						floatingPointValue,
						rationalNumeratorValue,
						rationalDenominatorValue,
						units,
						qualifier);
				}
				else if (nodeName.equals("datetime")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makeDateTimeContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("date")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makeDateContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("time")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makeTimeContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("pname")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makePersonNameContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("uidref")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makeUIDContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("text")) {
					String value = getTextValueOfNamedChildElementOrNull(node,"value");
					contentItem = contentItemFactory.makeTextContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						value);
				}
				else if (nodeName.equals("scoord")) {
					String graphicType = null;
					float[] graphicData = null;
					Node graphicTypeNode = node.getFirstChild();
					while (graphicTypeNode != null) {
						String graphicTypeNodeName = graphicTypeNode.getNodeName();
						if (graphicTypeNode.getNodeType() == Node.ELEMENT_NODE && graphicTypeNodeName != null) {
							if (graphicTypeNodeName.equals("point")
							 || graphicTypeNodeName.equals("multipoint")
							 || graphicTypeNodeName.equals("polyline")
							 || graphicTypeNodeName.equals("circle")
							 || graphicTypeNodeName.equals("ellipse")
							) {
								graphicType=graphicTypeNodeName.toUpperCase(Locale.US);
								break;
							}
						}
						graphicTypeNode = graphicTypeNode.getNextSibling();
					}
					// the graphicData (x,y) pairs are children of whatever element was the graphicType
					if (graphicType != null) {
						assert (graphicTypeNode != null);
						ArrayList graphicDataList = new ArrayList();
						Node graphicDataNode = graphicTypeNode.getFirstChild();
						while (graphicDataNode != null) {
							String graphicDataNodeName = graphicDataNode.getNodeName();
							if (graphicDataNode.getNodeType() == Node.ELEMENT_NODE && graphicDataNodeName != null
							 && graphicDataNodeName.equals("x") || graphicDataNodeName.equals("y")) {
								String value = graphicDataNode.getTextContent().trim();
								graphicDataList.add(value);
							}
							graphicDataNode = graphicDataNode.getNextSibling();
						}
						int arraySize = graphicDataList.size();
						graphicData = new float[arraySize];
						Iterator i = graphicDataList.iterator();
						int j=0;
						while (i.hasNext()) {
							graphicData[j++] = Float.parseFloat(((String)i.next()));
						}
					} 
					contentItem = contentItemFactory.makeSpatialCoordinatesContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						graphicType,
						graphicData);
				}
				else if (nodeName.equals("scoord3d")) {
					String graphicType = null;
					float[] graphicData = null;
					Node graphicTypeNode = node.getFirstChild();
					while (graphicTypeNode != null) {
						String graphicTypeNodeName = graphicTypeNode.getNodeName();
						if (graphicTypeNode.getNodeType() == Node.ELEMENT_NODE && graphicTypeNodeName != null) {
							if (graphicTypeNodeName.equals("point")
							 || graphicTypeNodeName.equals("multipoint")
							 || graphicTypeNodeName.equals("polyline")
							 || graphicTypeNodeName.equals("polygon")
							 || graphicTypeNodeName.equals("ellipse")
							 || graphicTypeNodeName.equals("ellipsoid")
							) {
								graphicType=graphicTypeNodeName.toUpperCase(Locale.US);
								break;
							}
						}
						graphicTypeNode = graphicTypeNode.getNextSibling();
					}
					// the graphicData (x,y) pairs are children of whatever element was the graphicType
					if (graphicType != null) {
						assert (graphicTypeNode != null);
						ArrayList graphicDataList = new ArrayList();
						Node graphicDataNode = graphicTypeNode.getFirstChild();
						while (graphicDataNode != null) {
							String graphicDataNodeName = graphicDataNode.getNodeName();
							if (graphicDataNode.getNodeType() == Node.ELEMENT_NODE && graphicDataNodeName != null
							 && graphicDataNodeName.equals("x") || graphicDataNodeName.equals("y") || graphicDataNodeName.equals("z")) {
								String value = graphicDataNode.getTextContent().trim();
								graphicDataList.add(value);
							}
							graphicDataNode = graphicDataNode.getNextSibling();
						}
						int arraySize = graphicDataList.size();
						graphicData = new float[arraySize];
						Iterator i = graphicDataList.iterator();
						int j=0;
						while (i.hasNext()) {
							graphicData[j++] = Float.parseFloat(((String)i.next()));
						}
					}
					String referencedFrameOfReferenceUID = getTextValueOfNamedChildElementOrNull(node,"frameofreference");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): referencedFrameOfReferenceUID = "+referencedFrameOfReferenceUID);
					contentItem = contentItemFactory.makeSpatialCoordinates3DContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						graphicType,
						graphicData,
						referencedFrameOfReferenceUID);
				}
				else if (nodeName.equals("tcoord")) {
				}
				else if (nodeName.equals("composite")) {
					String referencedSOPClassUID = getTextValueOfNamedChildElementOrNull(node,"class");
					String referencedSOPInstanceUID = getTextValueOfNamedChildElementOrNull(node,"instance");
					contentItem = contentItemFactory.makeCompositeContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						referencedSOPClassUID,
						referencedSOPInstanceUID);
				}
				else if (nodeName.equals("image")) {
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): is image");
					String referencedSOPClassUID = getTextValueOfNamedChildElementOrNull(node,"class");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): got referencedSOPClassUID = "+referencedSOPClassUID);
					String referencedSOPInstanceUID = getTextValueOfNamedChildElementOrNull(node,"instance");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): got referencedSOPInstanceUID = "+referencedSOPInstanceUID);
					
					int referencedFrameNumber = 0;	// <1 is missing value
					String referencedFrameNumberString = getTextValueOfNamedChildElementOrNull(node,"frame");
					if (referencedFrameNumberString != null && referencedFrameNumberString.length() > 0) {
						referencedFrameNumber = Integer.parseInt(referencedFrameNumberString);
					}
					int referencedSegmentNumber = 0;	// <1 is missing value
					String referencedSegmentNumberString = getTextValueOfNamedChildElementOrNull(node,"segment");
					if (referencedSegmentNumberString != null && referencedSegmentNumberString.length() > 0) {
						referencedSegmentNumber = Integer.parseInt(referencedSegmentNumberString);
					}
					
					String presentationStateSOPClassUID = null;
					String presentationStateSOPInstanceUID = null;
					Node presentationStateNode = getNamedChildElement(node,"presentationstate");
					if (presentationStateNode != null) {
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): has presentation state");
						presentationStateSOPClassUID = getTextValueOfNamedChildElementOrNull(presentationStateNode,"class");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): got presentationStateSOPClassUID = "+presentationStateSOPClassUID);
						presentationStateSOPInstanceUID = getTextValueOfNamedChildElementOrNull(presentationStateNode,"instance");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): got presentationStateSOPInstanceUID = "+presentationStateSOPInstanceUID);
					}
					
					String realWorldValueMappingSOPClassUID = null;
					String realWorldValueMappingSOPInstanceUID = null;
					Node realWorldValueMappingNode = getNamedChildElement(node,"realworldvaluemapping");
					if (realWorldValueMappingNode != null) {
						realWorldValueMappingSOPClassUID = getTextValueOfNamedChildElementOrNull(realWorldValueMappingNode,"class");
						realWorldValueMappingSOPInstanceUID = getTextValueOfNamedChildElementOrNull(realWorldValueMappingNode,"instance");
					}
					
					contentItem = contentItemFactory.makeImageContentItem(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						concept,
						referencedSOPClassUID,
						referencedSOPInstanceUID,
						referencedFrameNumber,
						referencedSegmentNumber,
						presentationStateSOPClassUID,presentationStateSOPInstanceUID,
						realWorldValueMappingSOPClassUID,realWorldValueMappingSOPInstanceUID);
				}
				else if (nodeName.equals("waveform")) {
				}
				else if (nodeName.equals("reference")) {
					String referencedContentItemIdentifier = getNamedNodeAttributeOrNull(attributes,"IDREF");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): referencedContentItemIdentifier from attribute = "+referencedContentItemIdentifier);
					if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
						referencedContentItemIdentifier = referencedContentItemIdentifier.replaceFirst(contentItemIdentiferPrefix,"");
					}
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): referencedContentItemIdentifier after removing prefix = "+referencedContentItemIdentifier);
					contentItem = new ContentItemWithReference(
						null /* parent will be set later by addChild() operation */,
						relationshipType,
						referencedContentItemIdentifier);
				}
				
				if (contentItem != null) {
					// if a recognized content item of any type, process all its children
					Node child = node.getFirstChild();
					while (child != null) {
						String childName = child.getNodeName();
						if (child.getNodeType() == Node.ELEMENT_NODE && childName != null) {
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): child = "+child);
							NamedNodeMap childAttributes = child.getAttributes();
							String childRelationshipType = getNamedNodeAttributeOrNull(childAttributes,"relationship");
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getNextContentItemFromXMLNodeSiblings(): relationship with child = "+childRelationshipType);
							if (childRelationshipType != null) {
								ContentItem childContentItem = getNextContentItemFromXMLNodeSiblings(child);	// recurse ... will do all its children too
								if (childContentItem != null) {
									contentItem.addChild(childContentItem);
								}
							}
							// else ignore it ... this is how we distinguish child content items from other child nodes (e.g., concept, value)
						}
						child = child.getNextSibling();
					}
					// return recognized contentItem and all of its children
					return contentItem;
				}
				// else just ignore unrecognized content items and proceed to the next
			}
			// else just ignore non-element Nodes (e.g. #text)
			node = node.getNextSibling();
		}
		return null;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as an XML document
	 * convert it to a StructuredReport using the content tree and ignoring any header attributes.</p>
	 *
	 * @param		document		the XML document
	 * @return						the StructuredReport
	 * @throws	DicomException
	 */
	public StructuredReport getStructuredReport(Document document) throws DicomException, ParserConfigurationException {
		StructuredReport structuredReport = null;
		org.w3c.dom.Node dicomStructuredReportElement = document.getDocumentElement();	// should be DicomStructuredReport
		if (dicomStructuredReportElement != null) {
			String dicomStructuredReportElementName = dicomStructuredReportElement.getNodeName();
			if (dicomStructuredReportElementName != null && dicomStructuredReportElement.getNodeType() == Node.ELEMENT_NODE && dicomStructuredReportElementName.equals("DicomStructuredReport")) {
				Node node = dicomStructuredReportElement.getFirstChild();
				while (node != null) {
					String nodeName = node.getNodeName();
					if (node.getNodeType() == Node.ELEMENT_NODE && nodeName != null) {
						if (nodeName.equals("DicomStructuredReportHeader")) {
							// ignore it
						}
						else if (nodeName.equals("DicomStructuredReportContent")) {
							contentItemFactory = new ContentItemFactory();
							ContentItem root = getNextContentItemFromXMLNodeSiblings(node.getFirstChild());					// ignore other children - should only be one root node
							structuredReport = new StructuredReport(root);
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getStructuredReport(): structuredReport parsed from XML =\n"+structuredReport);
						}
						else {
							throw new DicomException("Unexpected child element of DicomStructuredReport - "+nodeName);
						}
					}
					// just ignore non-element Nodes (e.g. #text)
					node = node.getNextSibling();
				}
			}
			else {
				throw new DicomException("Unexpected docoument root element - expected DicomStructuredReport - got "+dicomStructuredReportElementName);
			}
		}
		else {
			throw new DicomException("DicomStructuredReport document root element missing");
		}
		return structuredReport;
	}
	
	
	/**
	 * <p>Given a DICOM SR object encoded as an XML document
	 * convert it to a list of attributes.</p>
	 *
	 * @param		document		the XML document
	 * @return						the list of DICOM attributes
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(Document document) throws DicomException, ParserConfigurationException {
		AttributeList list = new AttributeList();
		org.w3c.dom.Node dicomStructuredReportElement = document.getDocumentElement();	// should be DicomStructuredReport
		if (dicomStructuredReportElement != null) {
			String dicomStructuredReportElementName = dicomStructuredReportElement.getNodeName();
			if (dicomStructuredReportElementName != null && dicomStructuredReportElement.getNodeType() == Node.ELEMENT_NODE && dicomStructuredReportElementName.equals("DicomStructuredReport")) {
				Node node = dicomStructuredReportElement.getFirstChild();
				while (node != null) {
					String nodeName = node.getNodeName();
					if (node.getNodeType() == Node.ELEMENT_NODE && nodeName != null) {
						if (nodeName.equals("DicomStructuredReportHeader")) {
							new XMLRepresentationOfDicomObjectFactory().addAttributesFromNodeToList(list,node);
						}
						else if (nodeName.equals("DicomStructuredReportContent")) {
							contentItemFactory = new ContentItemFactory();
							ContentItem root = getNextContentItemFromXMLNodeSiblings(node.getFirstChild());					// ignore other children - should only be one root node
							StructuredReport structuredReport = new StructuredReport(root);
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getAttributeList(): structuredReport parsed from XML =\n"+structuredReport);
							AttributeList structuredReportList = structuredReport.getAttributeList();
							list.putAll(structuredReportList);
						}
						else {
							throw new DicomException("Unexpected child element of DicomStructuredReport - "+nodeName);
						}
					}
					// just ignore non-element Nodes (e.g. #text)
					node = node.getNextSibling();
				}
			}
			else {
				throw new DicomException("Unexpected docoument root element - expected DicomStructuredReport - got "+dicomStructuredReportElementName);
			}
		}
		else {
			throw new DicomException("DicomStructuredReport document root element missing");
		}
//System.err.println("XMLRepresentationOfStructuredReportObjectFactory.getAttributeList(Document document): List is "+list);
		return list;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as an XML document in a stream
	 * convert it to a list of attributes.</p>
	 *
	 * @param		stream			the input stream containing the XML document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	SAXException
	 * @throws	ParserConfigurationException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(InputStream stream) throws IOException, SAXException, ParserConfigurationException, DicomException {
		Document document = db.parse(stream);
		return getAttributeList(document);
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as an XML document in a named file
	 * convert it to a list of attributes.</p>
	 *
	 * @param		name			the input file containing the XML document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	SAXException
	 * @throws	ParserConfigurationException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(String name) throws IOException, SAXException, ParserConfigurationException, DicomException {
		InputStream fi = new FileInputStream(name);
		BufferedInputStream bi = new BufferedInputStream(fi);
		AttributeList list = null;
		try {
			list = getAttributeList(bi);
		}
		finally {
			bi.close();
			fi.close();
		}
		return list;
	}

	/**
	 * @param	documentNode
	 * @param	indent
	 */
	public static String toString(org.w3c.dom.Node documentNode,int indent) {
		StringBuffer str = new StringBuffer();
		for (int i=0; i<indent; ++i) str.append("    ");
		str.append(documentNode);
		if (documentNode.hasAttributes()) {
			NamedNodeMap attrs = documentNode.getAttributes();
			for (int j=0; j<attrs.getLength(); ++j) {
				org.w3c.dom.Node attr = attrs.item(j);
				//str.append(toString(attr,indent+2));
				str.append(" ");
				str.append(attr);
			}
		}
		str.append("\n");
		++indent;
		for (org.w3c.dom.Node child = documentNode.getFirstChild(); child != null; child = child.getNextSibling()) {
			str.append(toString(child,indent));
			//str.append("\n");
		}
		return str.toString();
	}
	
	/**
	 * @param	documentNode
	 */
	public static String toString(org.w3c.dom.Node documentNode) {
		return toString(documentNode,0);
	}
	
	/**
	 * <p>Serialize an XML document (DOM tree).</p>
	 *
	 * @param	out		the output stream to write to
	 * @param	document	the XML document
	 * @throws	IOException
	 */
	public static void write(OutputStream out,Document document) throws IOException, TransformerConfigurationException, TransformerException {
		
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(out);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Properties outputProperties = new Properties();
		outputProperties.setProperty(OutputKeys.METHOD,"xml");
		outputProperties.setProperty(OutputKeys.INDENT,"yes");
		outputProperties.setProperty(OutputKeys.ENCODING,"UTF-8");	// the default anyway
		transformer.setOutputProperties(outputProperties);
		transformer.transform(source, result);
	}
	
	/**
	 * <p>Serialize an XML document (DOM tree) created from a DICOM Structured Report.</p>
	 *
	 * @param	list	the attribute list
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(AttributeList list,OutputStream out) throws IOException, DicomException {
		createDocumentAndWriteIt(null,list,out);
	}
	
	/**
	 * <p>Serialize an XML document (DOM tree) created from a DICOM Structured Report.</p>
	 *
	 * @param	sr		the Structured Report
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,OutputStream out) throws IOException, DicomException {
		createDocumentAndWriteIt(sr,null,out);
	}
	
	/**
	 * <p>Serialize an XML document (DOM tree) created from a DICOM Structured Report.</p>
	 *
	 * @param	sr		the Structured Report			may be null if list is not - will build an sr tree model
	 * @param	list	the attribute list				may be null if only the sr content tree is to be written
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,AttributeList list,OutputStream out) throws IOException, DicomException {
		try {
			Document document = new XMLRepresentationOfStructuredReportObjectFactory().getDocument(sr,list);
			write(out,document);
		}
		catch (ParserConfigurationException e) {
			throw new DicomException("Could not create XML document - problem creating object model from DICOM"+e);
		}
		catch (TransformerConfigurationException e) {
			throw new DicomException("Could not create XML document - could not instantiate transformer"+e);
		}
		catch (TransformerException e) {
			throw new DicomException("Could not create XML document - could not transform to XML"+e);
		}
	}
		
	/**
	 * <p>Read a DICOM dataset (that contains a structured report) and write an XML representation of it to the standard output, or vice versa.</p>
	 *
	 * @param	arg	either one filename of the file containing the DICOM dataset, or a direction argument (toDICOM or toXML, case insensitive) and an input filename
	 */
	public static void main(String arg[]) {
		try {
			boolean bad = true;
			boolean toXML = true;
			String filename = null;
			if (arg.length == 1) {
				bad = false;
				toXML = true;
				filename = arg[0];
			}
			else if (arg.length == 2) {
				filename = arg[1];
				if (arg[0].toLowerCase(Locale.US).equals("toxml")) {
					bad = false;
					toXML = true;
				}
				else if (arg[0].toLowerCase(Locale.US).equals("todicom") || arg[0].toLowerCase(Locale.US).equals("todcm")) {
					bad = false;
					toXML = false;
				}
			}
			if (bad) {
				System.err.println("usage: XMLRepresentationOfDicomObjectFactory [toDICOM|toXML] inputfile");
			}
			else {
				if (toXML) {
					AttributeList list = new AttributeList();
					//System.err.println("reading list");
					list.read(filename,null,true,true);
					//System.err.println("making sr");
					StructuredReport sr = new StructuredReport(list);
					//System.err.println("making document");
					Document document = new XMLRepresentationOfStructuredReportObjectFactory().getDocument(sr,list);
					//System.err.println(toString(document));
					write(System.out,document);
				}
				else {
					AttributeList list = new XMLRepresentationOfStructuredReportObjectFactory().getAttributeList(filename);
					String sourceApplicationEntityTitle = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SourceApplicationEntityTitle);
					list.insertSuitableSpecificCharacterSetForAllStringValues();
					list.removeMetaInformationHeaderAttributes();
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,sourceApplicationEntityTitle);
					list.write(System.out,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
				}
			}
				
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


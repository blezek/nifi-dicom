/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.HexDump;
import com.pixelmed.utils.StringUtilities;

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
 * <p>A class to encode a representation of a DICOM object in an XML form,
 * suitable for analysis as human-readable text, or for feeding into an
 * XSLT-based validator, and to convert them back again.</p>
 *
 * <p>An example of the type of output produced by this class is as follows:</p>
 * <pre>
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
  &lt;DicomObject&gt;
    &lt;FileMetaInformationGroupLength element="0000" group="0002" vr="UL"&gt;
      &lt;value number="1"&gt;222&lt;/value&gt;
    &lt;/FileMetaInformationGroupLength&gt;
    ...
    &lt;ImageType element="0008" group="0008" vr="CS"&gt;
      &lt;value number="1"&gt;ORIGINAL&lt;/value&gt;
      &lt;value number="2"&gt;PRIMARY&lt;/value&gt;
      &lt;value number="3"&gt;CINE&lt;/value&gt;
      &lt;value number="4"&gt;NONE&lt;/value&gt;
    &lt;/ImageType&gt;
    ...
    &lt;ContrastBolusAgentSequence element="0012" group="0018" vr="SQ"&gt;
      &lt;Item number="1"&gt;
        &lt;CodeValue element="0100" group="0008" vr="SH"&gt;
          &lt;value number="1"&gt;C-17800&lt;/value&gt;
        &lt;/CodeValue&gt;
      ...
      &lt;/Item&gt;
    &lt;/ContrastBolusAgentSequence&gt;
    ...
    &lt;PixelData element="0010" group="7fe0" vr="OW"/&gt;
&lt;/DicomObject&gt;
 * </pre>
 *
 * <p>There are a number of characteristics of this form of output:</p>
 *
 * <ul>
 * <li>Rather than a generic name for all DICOM data elements, like "element", with an attribute to provide the human-readable name,
 *     the name of the XML element itself is a human-readable keyword, as used in the DICOM Data Dictionary for the toolkit; the
 *     group and element tags are available as attributes of each such element; this makes construction of XPath accessors more straightforward.</li>
 * <li>The value representation of the DICOM source element is conveyed explicitly in an attribute; this facilitates validation of the XML result
 *     (e.g., that the correct VR has been used, and that the values are compatible with that VR).</li>
 * <li>Individual values of a DICOM data element are expressed as separate XML elements (named "value"), each with an attribute ("number") to specify their order, starting from 1 increasing by 1;
 *     this prevents users of the XML form from needing to parse multiple string values and separate out the DICOM value delimiter (backslash), and allows
 *     XPath accessors to obtain specific values; it also allows for access to separate values of binary, rather than string, DICOM data elements, which
 *     are represented the same way. Within each "value" element, the XML plain character data contains a string representation of the value.</li>
 * <li>Sequence items are encoded in a similar manner to multi-valued attributes, i.e., there is a nested XML data element (called "Item") with an
 *     explicit numeric attribute ("number") to specify their order, starting from 1 increasing by 1.</li>
 * </ul>
 *
 * <p> E.g., to test if an image is original, which is determined by a specific value of <code>ImageType (0008,0008)</code>, one
 * could write in XPath <code>"/DicomObject/ImageType/value[@number=1] = 'ORIGINAL'"</code>. To get the code value of the contrast
 * agent in use, one could write <code>"/DicomObject/ContrastBolusAgentSequence/Item[@number=1]/CodeValue/value[@number=1]"</code>,
 * or making some assumptions about cardinality and depth of nesting and removing the predicates, simply <code>"//ContrastBolusAgentSequence/Item/CodeValue/value"</code>. One could do this from the command
 * line with a utility such as {@link com.pixelmed.utils.XPathQuery XPathQuery}.</p>
 *
 * <p>Note that a round trip from DICOM to XML and back again does not
 * result in full fidelity, since:</p>
 *
 * <ul>
 * <li>Binary floating point values will lose precision when converted to string representation and back again</li>
 * <li>Leading and trailing white space and control characters in strings will be discarded</li>
 * <li>Meta information header elements will be changed</li>
 * <li>Structural elements such as group lengths will be removed and may or may not be replaced</li>
 * <li>Physical offsets such as in the DICOMDIR will be invalidated</li>
 * <li>Attributes with OB and OW value representations have their values discarded so as not to encode the bulk pixel data (probably should be added as an option)</li>
 * </ul>
 *
 * <p>A typical example of how to invoke this class to convert DICOM to XML would be:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomfile",null,true,true);
    Document document = new XMLRepresentationOfDicomObjectFactory().getDocument(list);
    XMLRepresentationOfDicomObjectFactory.write(System.out,document);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or even simpler, if there is no further use for the XML document:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomfile",null,true,true);
    XMLRepresentationOfDicomObjectFactory.createDocumentAndWriteIt(list,System.out);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>A typical example of converting XML back to DICOM would be:</p>
 * <pre>
try {
    AttributeList list = new XMLRepresentationOfDicomObjectFactory().getAttributeList("xmlfile");
    list.write(System.out,TransferSyntax.ExplicitVRLittleEndian,true,true);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or if you need to handle the meta information properly:</p>
 * <pre>
try {
    AttributeList list = new XMLRepresentationOfDicomObjectFactory().getAttributeList("xmlfile");
    String sourceApplicationEntityTitle = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SourceApplicationEntityTitle);
    list.removeMetaInformationHeaderAttributes();
    FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,sourceApplicationEntityTitle);
    list.write(System.out,TransferSyntax.ExplicitVRLittleEndian,true,true);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>When the XML is being converted to DICOM, the group, element and VR attributes are not needed if the element name is a keyword that can be found in
 * the dictionary; if they are present, then their values are checked against the dictionary values.</p>
 *
 * @see com.pixelmed.dicom.XMLRepresentationOfStructuredReportObjectFactory
 * @see com.pixelmed.utils.XPathQuery
 * @see com.pixelmed.validate
 * @see org.w3c.dom.Document
 *
 * @author	dclunie
 */
public class XMLRepresentationOfDicomObjectFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/XMLRepresentationOfDicomObjectFactory.java,v 1.25 2017/01/24 10:50:39 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(XMLRepresentationOfDicomObjectFactory.class);

	private DocumentBuilder db;
	
	/**
	 * @param	tag
	 */
	private String makeElementNameFromHexadecimalGroupElementValues(AttributeTag tag) {
		StringBuffer str = new StringBuffer();
		str.append("HEX");		// XML element names not allowed to start with a number
		String groupString = Integer.toHexString(tag.getGroup());
		for (int i=groupString.length(); i<4; ++i) str.append("0");
		str.append(groupString);
		String elementString = Integer.toHexString(tag.getElement());
		for (int i=elementString.length(); i<4; ++i) str.append("0");
		str.append(elementString);
		return str.toString();
	}

	/**
	 * @param		list
	 * @param		parent
	 * @throws	NumberFormatException
	 * @throws	DicomException
	 */
	void addAttributesFromNodeToList(AttributeList list,Node parent) throws NumberFormatException, DicomException {
		DicomDictionary dictionary = list.getDictionary();
		if (parent != null) {
			Node node = parent.getFirstChild();
			while (node != null) {
				String elementName = node.getNodeName();
				byte[] vr = null;
				String vrString = null;
				AttributeTag tag = dictionary.getTagFromName(elementName);
				if (tag != null) {
					vr = dictionary.getValueRepresentationFromTag(tag);
					vrString = ValueRepresentation.getAsString(vr);
				}
				NamedNodeMap attributes = node.getAttributes();
				if (attributes != null) {
					Node vrNode = attributes.getNamedItem("vr");
					if (vrNode != null) {
						String explicitVRString = vrNode.getNodeValue();
						byte[] explicitVR = explicitVRString.getBytes();
						if (vr == null) {
							vr = explicitVR;
						}
						else if (!vrString.equals(explicitVRString)) {
							if (ValueRepresentation.isUnspecifiedShortVR(vr)
							&& (ValueRepresentation.isUnsignedShortVR(explicitVR) || ValueRepresentation.isSignedShortVR(explicitVR) )) {
								vr = explicitVR;
							}
							else if (ValueRepresentation.isOtherUnspecifiedVR(vr)
							 && ValueRepresentation.isOtherByteOrWordVR(explicitVR)) {
								vr = explicitVR;
							}
							else {
								throw new DicomException("Dictionary VR <"+vrString+"> does not match VR in attribute <"+explicitVRString+"> of element "+elementName);
							}
						}
						// else same so ignore
					}
					Node groupNode = attributes.getNamedItem("group");
					Node elementNode = attributes.getNamedItem("element");
					if (groupNode != null || elementNode != null) {
						if (groupNode == null || elementNode == null) {
							throw new DicomException("Did not specify both group and element for element "+elementName);
						}
						String groupString = groupNode.getNodeValue();
						String elementString = elementNode.getNodeValue();
						int group = Integer.parseInt(groupString,16);
						int element = Integer.parseInt(elementString,16);
						AttributeTag explicitTag = new AttributeTag(group,element);
						if (tag == null) {
							tag = explicitTag;
						}
						else if (!tag.equals(explicitTag)) {
							throw new DicomException("Dictionary tag <"+tag+"> does not match group and element in attributes <"+explicitTag+"> of element "+elementName);
						}
						// else same so ignore
					}
				}
				if (vr != null && tag != null) {
					int group = tag.getGroup();
					int element = tag.getElement();
					if ((group%2 == 0 && element == 0) || (group == 0x0008 && element == 0x0001) || (group == 0xfffc && element == 0xfffc)) {
						//System.err.println("ignoring group length or length to end or dataset trailing padding "+tag);
					}
					else {
						if (ValueRepresentation.isSequenceVR(vr)) {
							SequenceAttribute a = new SequenceAttribute(tag);
							//System.err.println("Created "+a);
							if (node.hasChildNodes()) {
								Node childNode = node.getFirstChild();
								while (childNode != null) {
									String childNodeName = childNode.getNodeName();
									//System.err.println("childNodeName = "+childNodeName);
									if (childNodeName != null && childNodeName.equals("Item")) {
										// should check item number, but ignore for now :(
										//System.err.println("Adding item to sequence");
										AttributeList itemList = new AttributeList();
										addAttributesFromNodeToList(itemList,childNode);
										a.addItem(itemList);
									}
									// else may be a #text element in between
									childNode = childNode.getNextSibling();
								}
							}
							//System.err.println("Sequence Attribute is "+a);
							list.put(tag,a);
						}
						else {
							Attribute a = AttributeFactory.newAttribute(tag,vr);
							//System.err.println("Created "+a);
							if (node.hasChildNodes()) {
								Node childNode = node.getFirstChild();
								while (childNode != null) {
									String childNodeName = childNode.getNodeName();
									//System.err.println("childNodeName = "+childNodeName);
									if (childNodeName != null && childNodeName.equals("value")) {
										// should check value number, but ignore for now :(
										String value = childNode.getTextContent();
										//System.err.println("Value value = "+value);
										if (value != null) {
											value = StringUtilities.removeLeadingOrTrailingWhitespaceOrISOControl(value);	// just in case
											a.addValue(value);
										}
									}
									// else may be a #text element in between
									childNode = childNode.getNextSibling();
								}
							}
							//System.err.println("Attribute is "+a);
							list.put(tag,a);
						}
					}
				}
				node = node.getNextSibling();
			}
		}
	}

	/**
	 * @param	list
	 * @param	document
	 * @param	parent
	 */
	void addAttributesFromListToNode(AttributeList list,Document document,Node parent) {
		DicomDictionary dictionary = list.getDictionary();
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute attribute = (Attribute)i.next();
			AttributeTag tag = attribute.getTag();
			
			String elementName = dictionary.getNameFromTag(tag);
			if (elementName == null) {
				elementName=makeElementNameFromHexadecimalGroupElementValues(tag);
			}
			Node node = document.createElement(elementName);
			parent.appendChild(node);
			
			{
				Attr attr = document.createAttribute("group");
				attr.setValue(HexDump.shortToPaddedHexString(tag.getGroup()));
				node.getAttributes().setNamedItem(attr);
			}
			{
				Attr attr = document.createAttribute("element");
				attr.setValue(HexDump.shortToPaddedHexString(tag.getElement()));
				node.getAttributes().setNamedItem(attr);
			}
			{
				Attr attr = document.createAttribute("vr");
				attr.setValue(ValueRepresentation.getAsString(attribute.getVR()));
				node.getAttributes().setNamedItem(attr);
			}
			
			if (attribute instanceof SequenceAttribute) {
				int count=0;
				Iterator si = ((SequenceAttribute)attribute).iterator();
				while (si.hasNext()) {
					SequenceItem item = (SequenceItem)si.next();
					Node itemNode = document.createElement("Item");
					Attr numberAttr = document.createAttribute("number");
					numberAttr.setValue(Integer.toString(++count));
					itemNode.getAttributes().setNamedItem(numberAttr);
					node.appendChild(itemNode);
					addAttributesFromListToNode(item.getAttributeList(),document,itemNode);
				}
			}
			else {
				//Attr attr = document.createAttribute("value");
				//attr.setValue(attribute.getDelimitedStringValuesOrEmptyString());
				//node.getAttributes().setNamedItem(attr);
				
				//node.appendChild(document.createTextNode(attribute.getDelimitedStringValuesOrEmptyString()));
				
				String values[] = null;
				try {
					values=attribute.getStringValues();
				}
				catch (DicomException e) {
					slf4jlogger.debug("Ignoring exception",e);
				}
				if (values != null) {
					for (int j=0; j<values.length; ++j) {
						Node valueNode = document.createElement("value");
						Attr numberAttr = document.createAttribute("number");
						numberAttr.setValue(Integer.toString(j+1));
						valueNode.getAttributes().setNamedItem(numberAttr);
						valueNode.appendChild(document.createTextNode(values[j]));
						node.appendChild(valueNode);
					}
				}
			}
		}
	}

	/**
	 * <p>Construct a factory object, which can be used to get XML documents from DICOM objects.</p>
	 *
	 * @throws	ParserConfigurationException
	 */
	public XMLRepresentationOfDicomObjectFactory() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		db = dbf.newDocumentBuilder();
	}
	
	/**
	 * <p>Given a DICOM object encoded as a list of attributes, get an XML document
	 * as a DOM tree.</p>
	 *
	 * @param	list	the list of DICOM attributes
	 */
	public Document getDocument(AttributeList list) {
		Document document = db.newDocument();
		org.w3c.dom.Node element = document.createElement("DicomObject");
		document.appendChild(element);
		addAttributesFromListToNode(list,document,element);
		return document;
	}
	
	/**
	 * <p>Given a DICOM object encoded as an XML document
	 * convert it to a list of attributes.</p>
	 *
	 * @param		document		the XML document
	 * @return						the list of DICOM attributes
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(Document document) throws DicomException {
		AttributeList list = new AttributeList();
		org.w3c.dom.Node element = document.getDocumentElement();	// should be DicomObject
		addAttributesFromNodeToList(list,element);
//System.err.println("XMLRepresentationOfDicomObjectFactory.getAttributeList(Document document): List is "+list);
		return list;
	}
	
	/**
	 * <p>Given a DICOM object encoded as an XML document in a stream
	 * convert it to a list of attributes.</p>
	 *
	 * @param		stream			the input stream containing the XML document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	SAXException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(InputStream stream) throws IOException, SAXException, DicomException {
		Document document = db.parse(stream);
		return getAttributeList(document);
	}
	
	/**
	 * <p>Given a DICOM object encoded as an XML document in a named file
	 * convert it to a list of attributes.</p>
	 *
	 * @param		name			the input file containing the XML document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	SAXException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(String name) throws IOException, SAXException, DicomException {
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
	 * @param	node
	 * @param	indent
	 */
	public static String toString(Node node,int indent) {
		StringBuffer str = new StringBuffer();
		for (int i=0; i<indent; ++i) str.append("    ");
		str.append(node);
		if (node.hasAttributes()) {
			NamedNodeMap attrs = node.getAttributes();
			for (int j=0; j<attrs.getLength(); ++j) {
				Node attr = attrs.item(j);
				//str.append(toString(attr,indent+2));
				str.append(" ");
				str.append(attr);
			}
		}
		str.append("\n");
		++indent;
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			str.append(toString(child,indent));
			//str.append("\n");
		}
		return str.toString();
	}
	
	/**
	 * @param	node
	 */
	public static String toString(Node node) {
		return toString(node,0);
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
	 * <p>Serialize an XML document (DOM tree) created from a DICOM attribute list.</p>
	 *
	 * @param	list		the list of DICOM attributes
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(AttributeList list,OutputStream out) throws IOException, DicomException {
		try {
			Document document = new XMLRepresentationOfDicomObjectFactory().getDocument(list);
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
	 * <p>Read a DICOM dataset and write an XML representation of it to the standard output, or vice versa.</p>
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
				if (arg[0].toLowerCase(java.util.Locale.US).equals("toxml")) {
					bad = false;
					toXML = true;
				}
				else if (arg[0].toLowerCase(java.util.Locale.US).equals("todicom") || arg[0].toLowerCase(java.util.Locale.US).equals("todcm")) {
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
					//System.err.println("making document");
					Document document = new XMLRepresentationOfDicomObjectFactory().getDocument(list);
					//System.err.println(toString(document));
					write(System.out,document);
				}
				else {
//long startReadTime = System.currentTimeMillis();
					AttributeList list = new XMLRepresentationOfDicomObjectFactory().getAttributeList(filename);
//System.err.println("AttributeList.main(): read XML and create DICOM AttributeList - done in "+(System.currentTimeMillis()-startReadTime)+" ms");
					String sourceApplicationEntityTitle = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SourceApplicationEntityTitle);
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


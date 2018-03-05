/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FloatFormatter;

import java.util.Iterator;
import java.util.Locale;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class with methods for constructing a {@link com.pixelmed.dicom.ContentItem ContentItem} of the appropriate class from a list of attributes.</p>
 *
 * <p>The sub-classes of {@link com.pixelmed.dicom.ContentItem ContentItem} are public internal classes of this class,
 * but specialize the methods, specifically the extractors and the string representation methods.</p>
 *
 * <p>This is not an abstract class, and the content item factory method is not static; an instance of
 * the factory needs to be created.</p>
 *
 * @see com.pixelmed.dicom.ContentItem
 * @see com.pixelmed.dicom.ContentItemWithReference
 * @see com.pixelmed.dicom.ContentItemWithValue
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.StructuredReportBrowser
 *
 * @author	dclunie
 */
public class ContentItemFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/ContentItemFactory.java,v 1.43 2017/01/24 10:50:36 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(CompositeInstanceContext.class);
	
	/***/
	public class UnrecognizedContentItem extends ContentItemWithValue {
		
		/**
		 * @param	parent							parent content item to add to
		 */
		public UnrecognizedContentItem(ContentItem parent) {
			super(parent,null);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public UnrecognizedContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 * @param	valueType						the valueType encoded for the content item (is discarded)
		 */
		public UnrecognizedContentItem(ContentItem parent,AttributeList list,String valueType) {
			super(parent,list);
		}
		
		public String getConceptValue()      { return ""; }
	}
	
	/***/
	public class ContainerContentItem extends ContentItemWithValue {
		protected String continuityOfContent;
		protected String templateMappingResource;
		protected String templateIdentifier;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public ContainerContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			continuityOfContent=Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ContinuityOfContent);
			AttributeList contentTemplateSequenceItemAttributeList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,TagFromName.ContentTemplateSequence);
			if (contentTemplateSequenceItemAttributeList != null) {
				templateMappingResource=Attribute.getSingleStringValueOrEmptyString(contentTemplateSequenceItemAttributeList,TagFromName.MappingResource); 
				templateIdentifier=Attribute.getSingleStringValueOrEmptyString(contentTemplateSequenceItemAttributeList,TagFromName.TemplateIdentifier);
			}
			if (templateMappingResource == null) templateMappingResource="";		// just for consistency with other string content items
			if (templateIdentifier == null) templateIdentifier="";
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
		 * @param	templateMappingResource			identifier of the template mapping resource
		 * @param	templateIdentifier				identifier of the template
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate,
				String templateMappingResource,String templateIdentifier) throws DicomException {
			super(parent,"CONTAINER",relationshipType,conceptName);
			continuityOfContent = continuityOfContentIsSeparate ? "SEPARATE" : "CONTINUOUS";
			{ Attribute a = new CodeStringAttribute(TagFromName.ContinuityOfContent); a.addValue(continuityOfContent); list.put(a); }
			this.templateMappingResource = templateMappingResource;
			this.templateIdentifier = templateIdentifier;
			if (this.templateMappingResource != null || templateIdentifier != null) {
//System.err.println("ContentItemFactort.ContainerContentItem(): adding ContentTemplateSequence");
				SequenceAttribute contentTemplateSequence = new SequenceAttribute(TagFromName.ContentTemplateSequence);
				AttributeList contentTemplateSequenceItemAttributeList = new AttributeList();
				if (templateMappingResource != null) {
					Attribute a = new CodeStringAttribute(TagFromName.MappingResource); a.addValue(templateMappingResource.toUpperCase(java.util.Locale.US)); contentTemplateSequenceItemAttributeList.put(a);
				}
				if (templateIdentifier != null) {
					Attribute a = new CodeStringAttribute(TagFromName.TemplateIdentifier); a.addValue(templateIdentifier.toUpperCase(java.util.Locale.US)); contentTemplateSequenceItemAttributeList.put(a);
				}
				contentTemplateSequence.addItem(contentTemplateSequenceItemAttributeList);
				list.put(contentTemplateSequence);
			}
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate) throws DicomException {
			this(parent,relationshipType,conceptName,true,null,null);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public ContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName) throws DicomException {
			this(parent,relationshipType,conceptName,true);
		}

		public String getConceptValue()      { return ""; }

		/**
		 * @return	the continuity of content
		 */
		public String getContinuityOfContent()      { return continuityOfContent; }

		/**
		 * @return	the template mapping resource
		 */
		public String getTemplateMappingResource()      { return templateMappingResource; }

		/**
		 * @return	the template identifier
		 */
		public String getTemplateIdentifier()      { return templateIdentifier; }

		public String toString() {
			return super.toString()
				   +(continuityOfContent != null && continuityOfContent.length() > 0 ? " ["+continuityOfContent+"]" : "")
				   +(templateIdentifier  != null && templateIdentifier.length()  > 0 ? " ("+templateMappingResource+","+templateIdentifier+")" : "")
				   ;
		}
	}
	
	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
	 * @param	templateMappingResource			identifier of the template mapping resource
	 * @param	templateIdentifier				identifier of the template
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ContainerContentItem makeContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate,
				String templateMappingResource,String templateIdentifier) throws DicomException {
		return new ContainerContentItem(parent,relationshipType,conceptName,continuityOfContentIsSeparate,templateMappingResource,templateIdentifier);
	}
	
	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	continuityOfContentIsSeparate	true if SEPARATE, false if CONTINUOUS
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ContainerContentItem makeContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,boolean continuityOfContentIsSeparate) throws DicomException {
		return new ContainerContentItem(parent,relationshipType,conceptName,continuityOfContentIsSeparate,null/*templateMappingResource*/,null/*templateIdentifier*/);
	}
	
	/**
	 *
	 * Construct a ContainerContentItem
	 *
	 * Default to separate continuity
	 *
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ContainerContentItem makeContainerContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName) throws DicomException {
		return new ContainerContentItem(parent,relationshipType,conceptName,true/*continuityOfContentIsSeparate*/,null/*templateMappingResource*/,null/*templateIdentifier*/);
	}

	/***/
	public class CompositeContentItem extends ContentItemWithValue {

		protected AttributeList referencedSOPSequenceItemAttributeList;		// subclasses will use this to extract or to add macro-specific attributes
		protected String referencedSOPClassUID;
		protected String referencedSOPInstanceUID;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public CompositeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			referencedSOPSequenceItemAttributeList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,TagFromName.ReferencedSOPSequence);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedSOPClassUID=Attribute.getSingleStringValueOrEmptyString(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPClassUID);
				referencedSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPInstanceUID);
			}
			if (referencedSOPClassUID == null) referencedSOPClassUID="";		// just for consistency with other string content items
			if (referencedSOPInstanceUID == null) referencedSOPInstanceUID="";
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	referencedSOPClassUID			the SOP Class UID
		 * @param	referencedSOPInstanceUID		the SOP Instance UID
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public CompositeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			super(parent,"COMPOSITE",relationshipType,conceptName);
			doCommonConstructorStuff(referencedSOPClassUID,referencedSOPInstanceUID);
		}
	
		/**
		 * @param	parent							parent content item to add to
		 * @param	valueType						the value type
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	referencedSOPClassUID			the SOP Class UID
		 * @param	referencedSOPInstanceUID		the SOP Instance UID
		 * @throws	DicomException					if error in DICOM encoding
		 */
		protected CompositeContentItem(ContentItem parent,String valueType,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			super(parent,valueType,relationshipType,conceptName);
			doCommonConstructorStuff(referencedSOPClassUID,referencedSOPInstanceUID);
		}
	
		/**
		 * @param	referencedSOPClassUID			the SOP Class UID
		 * @param	referencedSOPInstanceUID		the SOP Instance UID
		 * @throws	DicomException					if error in DICOM encoding
		 */
		protected void doCommonConstructorStuff(String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
			referencedSOPSequenceItemAttributeList = new AttributeList();
			this.referencedSOPClassUID = referencedSOPClassUID;
			if (referencedSOPClassUID != null) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(referencedSOPClassUID); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.referencedSOPInstanceUID = referencedSOPInstanceUID;
			if (referencedSOPInstanceUID != null) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(referencedSOPInstanceUID); referencedSOPSequenceItemAttributeList.put(a);
			}
			SequenceAttribute referencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);
			list.put(referencedSOPSequence);
			referencedSOPSequence.addItem(referencedSOPSequenceItemAttributeList);
		}
	
		public String getConceptValue()      { return ""; }

		public String toString() {
			return super.toString()+" = "+referencedSOPClassUID+" : "+referencedSOPInstanceUID;
		}

		/**
		 * @return	the SOP Class UID
		 */
		public String getReferencedSOPClassUID()    { return referencedSOPClassUID; }

		/**
		 * @return	the SOP Instance UID
		 */
		public String getReferencedSOPInstanceUID() { return referencedSOPInstanceUID; }
	}


	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	referencedSOPClassUID			the SOP Class UID
	 * @param	referencedSOPInstanceUID		the SOP Instance UID
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public CompositeContentItem makeCompositeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
			String referencedSOPClassUID,String referencedSOPInstanceUID) throws DicomException {
		return new CompositeContentItem(parent,relationshipType,conceptName,
			referencedSOPClassUID,referencedSOPInstanceUID);
	}
	
	/***/
	public class ImageContentItem extends CompositeContentItem {

		protected int referencedFrameNumber;
		protected int referencedSegmentNumber;
		protected String presentationStateSOPClassUID;
		protected String presentationStateSOPInstanceUID;
		protected String realWorldValueMappingSOPClassUID;
		protected String realWorldValueMappingSOPInstanceUID;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public ImageContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedFrameNumber=Attribute.getSingleIntegerValueOrDefault(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedFrameNumber,0);
				referencedSegmentNumber=Attribute.getSingleIntegerValueOrDefault(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSegmentNumber,0);

				{
					AttributeList psl = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedSOPSequence);
					if (psl != null) {
						presentationStateSOPClassUID=Attribute.getSingleStringValueOrEmptyString(psl,TagFromName.ReferencedSOPClassUID);
						presentationStateSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(psl,TagFromName.ReferencedSOPInstanceUID);
					}
					if (presentationStateSOPClassUID == null) presentationStateSOPClassUID="";
					if (presentationStateSOPInstanceUID == null) presentationStateSOPInstanceUID="";
				}
				{
					AttributeList rwvl = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedRealWorldValueMappingInstanceSequence);
					if (rwvl != null) {
						realWorldValueMappingSOPClassUID=Attribute.getSingleStringValueOrEmptyString(rwvl,TagFromName.ReferencedSOPClassUID);
						realWorldValueMappingSOPInstanceUID=Attribute.getSingleStringValueOrEmptyString(rwvl,TagFromName.ReferencedSOPInstanceUID);
					}
					if (realWorldValueMappingSOPClassUID == null) realWorldValueMappingSOPClassUID="";
					if (realWorldValueMappingSOPInstanceUID == null) realWorldValueMappingSOPInstanceUID="";
				}
				// forget about Icon Image Sequence for now :(
			}
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	referencedSOPClassUID			the SOP Class UID
		 * @param	referencedSOPInstanceUID		the SOP Instance UID
		 * @param	referencedFrameNumber			if &lt; 1, not added
		 * @param	referencedSegmentNumber			if &lt; 1, not added
		 * @param	presentationStateSOPClassUID	the SOP Class UID of the presentation state (or null or empty if none)
		 * @param	presentationStateSOPInstanceUID	the SOP Instance UID of the presentation state (or null or empty if none)
		 * @param	realWorldValueMappingSOPClassUID	the SOP Class UID of the RWV Map (or null or empty if none)
		 * @param	realWorldValueMappingSOPInstanceUID	the SOP Instance UID of the RWV Map (or null or empty if none)
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public ImageContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
				String referencedSOPClassUID,String referencedSOPInstanceUID,
				int referencedFrameNumber,int referencedSegmentNumber,
				String presentationStateSOPClassUID,String presentationStateSOPInstanceUID,
				String realWorldValueMappingSOPClassUID,String realWorldValueMappingSOPInstanceUID) throws DicomException {
			super(parent,"IMAGE",relationshipType,conceptName,referencedSOPClassUID,referencedSOPInstanceUID);
			this.referencedFrameNumber = referencedFrameNumber < 1 ? 0 : referencedFrameNumber;
			if (referencedFrameNumber >= 1) {
				Attribute a = new IntegerStringAttribute(TagFromName.ReferencedFrameNumber); a.addValue(referencedFrameNumber); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.referencedSegmentNumber = referencedSegmentNumber < 1 ? 0 : referencedSegmentNumber;
			if (referencedSegmentNumber >= 1) {
				Attribute a = new UnsignedShortAttribute(TagFromName.ReferencedSegmentNumber); a.addValue(referencedSegmentNumber); referencedSOPSequenceItemAttributeList.put(a);
			}
			this.presentationStateSOPClassUID = presentationStateSOPClassUID;
			this.presentationStateSOPInstanceUID = presentationStateSOPInstanceUID;
			if (presentationStateSOPClassUID != null && presentationStateSOPClassUID.length() > 0
			 && presentationStateSOPInstanceUID != null && presentationStateSOPInstanceUID.length() > 0) {
				SequenceAttribute presentationStateReferencedSOPSequence = new SequenceAttribute(TagFromName.ReferencedSOPSequence);
				referencedSOPSequenceItemAttributeList.put(presentationStateReferencedSOPSequence);
				AttributeList presentationStateReferencedSOPSequenceList = new AttributeList();
				presentationStateReferencedSOPSequence.addItem(presentationStateReferencedSOPSequenceList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(presentationStateSOPClassUID); presentationStateReferencedSOPSequenceList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(presentationStateSOPInstanceUID); presentationStateReferencedSOPSequenceList.put(a); }
			}
			this.realWorldValueMappingSOPClassUID = realWorldValueMappingSOPClassUID;
			this.realWorldValueMappingSOPInstanceUID = realWorldValueMappingSOPInstanceUID;
			if (realWorldValueMappingSOPClassUID != null && realWorldValueMappingSOPClassUID.length() > 0
			 && realWorldValueMappingSOPInstanceUID != null && realWorldValueMappingSOPInstanceUID.length() > 0) {
				SequenceAttribute referencedRealWorldValueMappingInstanceSequence = new SequenceAttribute(TagFromName.ReferencedRealWorldValueMappingInstanceSequence);
				referencedSOPSequenceItemAttributeList.put(referencedRealWorldValueMappingInstanceSequence);
				AttributeList referencedRealWorldValueMappingInstanceSequenceList = new AttributeList();
				referencedRealWorldValueMappingInstanceSequence.addItem(referencedRealWorldValueMappingInstanceSequenceList);
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPClassUID); a.addValue(realWorldValueMappingSOPClassUID); referencedRealWorldValueMappingInstanceSequenceList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedSOPInstanceUID); a.addValue(realWorldValueMappingSOPInstanceUID); referencedRealWorldValueMappingInstanceSequenceList.put(a); }
			}
		}
	
		/***/
		public String toString() {
			return super.toString()
				+ (referencedFrameNumber == 0 ? "" : ("[Frame "+Integer.toString(referencedFrameNumber)+"]"))
				+ (referencedSegmentNumber == 0 ? "" : ("[Segment "+Integer.toString(referencedSegmentNumber)+"]"))
				+ (presentationStateSOPInstanceUID == null || presentationStateSOPInstanceUID.length() == 0 ? "" : (" (PS "+presentationStateSOPClassUID+" : "+presentationStateSOPInstanceUID+")"))
				+ (realWorldValueMappingSOPInstanceUID == null || realWorldValueMappingSOPInstanceUID.length() == 0 ? "" : (" (RWV "+realWorldValueMappingSOPClassUID+" : "+realWorldValueMappingSOPInstanceUID+")"))
				;
		}

		/**
		 * @return	the frame number, or zero if none
		 */
		public int getReferencedFrameNumber()    { return referencedFrameNumber; }

		/**
		 * @return	the segment number, or zero if none
		 */
		public int getReferencedSegmentNumber()    { return referencedSegmentNumber; }

		/**
		 * @return	the SOP Class UID of the presention state, if any
		 */
		public String getPresentationStateSOPClassUID()    { return presentationStateSOPClassUID; }

		/**
		 * @return	the SOP Instance UID of the presention state, if any
		 */
		public String getPresentationStateSOPInstanceUID() { return presentationStateSOPInstanceUID; }

		/**
		 * @return	the SOP Class UID of the RWV Map, if any
		 */
		public String getRealWorldValueMappingSOPClassUID()    { return realWorldValueMappingSOPClassUID; }

		/**
		 * @return	the SOP Instance UID of the RWV Map, if any
		 */
		public String getRealWorldValueMappingSOPInstanceUID() { return realWorldValueMappingSOPInstanceUID; }
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	referencedSOPClassUID			the SOP Class UID
	 * @param	referencedSOPInstanceUID		the SOP Instance UID
	 * @param	referencedFrameNumber			if &lt; 1, not added
	 * @param	referencedSegmentNumber			if &lt; 1, not added
	 * @param	presentationStateSOPClassUID	the SOP Class UID of the presentation state (or null or empty if none)
	 * @param	presentationStateSOPInstanceUID	the SOP Instance UID of the presentation state (or null or empty if none)
	 * @param	realWorldValueMappingSOPClassUID	the SOP Class UID of the RWV Map (or null or empty if none)
	 * @param	realWorldValueMappingSOPInstanceUID	the SOP Instance UID of the RWV Map (or null or empty if none)
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ImageContentItem makeImageContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,
			String referencedSOPClassUID,String referencedSOPInstanceUID,
			int referencedFrameNumber,int referencedSegmentNumber,
			String presentationStateSOPClassUID,String presentationStateSOPInstanceUID,
			String realWorldValueMappingSOPClassUID,String realWorldValueMappingSOPInstanceUID) throws DicomException {
		return new ImageContentItem(parent,relationshipType,conceptName,
			referencedSOPClassUID,referencedSOPInstanceUID,
			referencedFrameNumber,referencedSegmentNumber,
			presentationStateSOPClassUID,presentationStateSOPInstanceUID,
			realWorldValueMappingSOPClassUID,realWorldValueMappingSOPInstanceUID);
	}

	/***/
	public class WaveformContentItem extends CompositeContentItem {

		protected int[] referencedWaveformChannels;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public WaveformContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			if (referencedSOPSequenceItemAttributeList != null) {
				referencedWaveformChannels=Attribute.getIntegerValues(referencedSOPSequenceItemAttributeList,TagFromName.ReferencedWaveformChannels);
			}
		}

		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = [");
			if (referencedWaveformChannels != null) {
				for (int j=0; j<referencedWaveformChannels.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedWaveformChannels[j]);
				}
			}
			str.append("]");
			return str.toString();
		}

		/**
		 * @return	the waveform channels
		 */
		public int[] getReferencedWaveformChannels()    { return referencedWaveformChannels; }
	}

	/***/
	public class SpatialCoordinatesContentItem extends ContentItemWithValue {

		protected String graphicType;
		protected float[] graphicData;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public SpatialCoordinatesContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			graphicType=Attribute.getSingleStringValueOrDefault(list,TagFromName.GraphicType,"");
			try {
				Attribute a = list.get(TagFromName.GraphicData);
				if (a != null) {
					graphicData = a.getFloatValues();
				}
			}
			catch (DicomException e) {
			}
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	graphicType						graphic type
		 * @param	graphicData						graphic data
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public SpatialCoordinatesContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData) throws DicomException {
			super(parent,"SCOORD",relationshipType,conceptName);
			this.graphicType=graphicType;
			if (graphicType != null) {
				Attribute a = new CodeStringAttribute(TagFromName.GraphicType); a.addValue(graphicType); list.put(a);
			}
			this.graphicData=graphicData;
			if (graphicData != null) {
				Attribute a = new FloatSingleAttribute(TagFromName.GraphicData);
				for (int j=0; j<graphicData.length; ++j) {	// should be a single method in FloatSingleAttribute to add the whole array :(
					a.addValue(graphicData[j]);
				}
				list.put(a);
			}
		}

		public String getConceptValue()      { return ""; }

		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = ");
			str.append(graphicType);
			str.append(" (");
			if (graphicData != null) {
				for (int j=0; j<graphicData.length; ++j) {
					if (j > 0) str.append(",");
					str.append(FloatFormatter.toStringOfFixedMaximumLength(graphicData[j],16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US));
				}
			}
			str.append(")");
			return str.toString();
		}

		/**
		 * @return	the graphic type
		 */
		public String getGraphicType()              { return graphicType; }

		/**
		 * @return	the graphic type
		 */
		public float[] getGraphicData()             { return graphicData; }
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	graphicType						graphic type
	 * @param	graphicData						graphic data
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public SpatialCoordinatesContentItem makeSpatialCoordinatesContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData) throws DicomException {
		return new SpatialCoordinatesContentItem(parent,relationshipType,conceptName,graphicType,graphicData);
	}

	public class SpatialCoordinates3DContentItem extends ContentItemWithValue {

		protected String graphicType;
		protected float[] graphicData;
		protected String referencedFrameOfReferenceUID;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public SpatialCoordinates3DContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			graphicType=Attribute.getSingleStringValueOrDefault(list,TagFromName.GraphicType,"");
			try {
				Attribute a = list.get(TagFromName.GraphicData);
				if (a != null) {
					graphicData = a.getFloatValues();
				}
			}
			catch (DicomException e) {
			}
			referencedFrameOfReferenceUID=Attribute.getSingleStringValueOrDefault(list,TagFromName.ReferencedFrameOfReferenceUID,"");
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	graphicType						graphic type
		 * @param	graphicData						graphic data
		 * @param	referencedFrameOfReferenceUID	frame of reference UID
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public SpatialCoordinates3DContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData,String referencedFrameOfReferenceUID) throws DicomException {
			super(parent,"SCOORD3D",relationshipType,conceptName);
			this.graphicType=graphicType;
			if (graphicType != null) {
				Attribute a = new CodeStringAttribute(TagFromName.GraphicType); a.addValue(graphicType); list.put(a);
			}
			this.graphicData=graphicData;
			if (graphicData != null) {
				Attribute a = new FloatSingleAttribute(TagFromName.GraphicData);
				for (int j=0; j<graphicData.length; ++j) {	// should be a single method in FloatSingleAttribute to add the whole array :(
					a.addValue(graphicData[j]);
				}
				list.put(a);
			}
//System.err.println("ContentItemFactory.SpatialCoordinates3DContentItem(): referencedFrameOfReferenceUID = "+referencedFrameOfReferenceUID);
			this.referencedFrameOfReferenceUID = referencedFrameOfReferenceUID;
			if (referencedFrameOfReferenceUID != null) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.ReferencedFrameOfReferenceUID); a.addValue(referencedFrameOfReferenceUID); list.put(a);
			}
		}

		public String getConceptValue()      { return ""; }

		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = ");
			str.append(graphicType);
			str.append(" (");
			if (graphicData != null) {
				for (int j=0; j<graphicData.length; ++j) {
					if (j > 0) str.append(",");
					str.append(FloatFormatter.toStringOfFixedMaximumLength(graphicData[j],16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US));
				}
			}
			str.append(") (FoR ");
			str.append(referencedFrameOfReferenceUID);
			str.append(")");
			return str.toString();
		}

		/**
		 * @return	the graphic type
		 */
		public String getGraphicType()              { return graphicType; }

		/**
		 * @return	the graphic type
		 */
		public float[] getGraphicData()             { return graphicData; }

		/**
		 * @return	the frame of reference UID
		 */
		public String getReferencedFrameOfReferenceUID()	{ return referencedFrameOfReferenceUID; }
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	graphicType						graphic type
	 * @param	graphicData						graphic data
	 * @param	referencedFrameOfReferenceUID	frame of reference UID
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public SpatialCoordinates3DContentItem makeSpatialCoordinates3DContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String graphicType,float[] graphicData,String referencedFrameOfReferenceUID) throws DicomException {
		return new SpatialCoordinates3DContentItem(parent,relationshipType,conceptName,graphicType,graphicData,referencedFrameOfReferenceUID);
	}

	/***/
	public class TemporalCoordinatesContentItem extends ContentItemWithValue {

		protected String temporalRangeType;
		protected int[] referencedSamplePositions;
		protected float[] referencedTimeOffsets;
		protected String[] referencedDateTimes;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public TemporalCoordinatesContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			temporalRangeType=Attribute.getSingleStringValueOrDefault(list,TagFromName.TemporalRangeType,"");
			try {
				{
					Attribute a = list.get(TagFromName.ReferencedSamplePositions);
					if (a != null) {
						referencedSamplePositions = a.getIntegerValues();
					}
				}
				{
					Attribute a = list.get(TagFromName.ReferencedTimeOffsets);
					if (a != null) {
						referencedTimeOffsets = a.getFloatValues();
					}
				}
				{
					Attribute a = list.get(TagFromName.ReferencedDateTime);
					if (a != null) {
						referencedDateTimes = a.getStringValues();
					}
				}
			}
			catch (DicomException e) {
			}
		}

		public String getConceptValue()      { return ""; }

		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append(super.toString());
			str.append(" = ");
			str.append(temporalRangeType);
			if (referencedSamplePositions != null) {
				str.append(" Sample Positions (");
				for (int j=0; j<referencedSamplePositions.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedSamplePositions[j]);
				}
				str.append(")");
			}
			if (referencedTimeOffsets != null) {
				str.append(" Time Offsets (");
				for (int j=0; j<referencedTimeOffsets.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedTimeOffsets[j]);
				}
				str.append(")");
			}
			if (referencedDateTimes != null) {
				str.append(" DateTimes (");
				for (int j=0; j<referencedDateTimes.length; ++j) {
					if (j > 0) str.append(",");
					str.append(referencedDateTimes[j]);
				}
				str.append(")");
			}
			return str.toString();
		}


		/**
		 * @return	the temporal range type
		 */
		public String getTemporalRangeType()		{ return temporalRangeType; }

		/**
		 * @return	the referenced sample positions, or null if none
		 */
		public int[] getReferencedSamplePositions()	{ return referencedSamplePositions; }

		/**
		 * @return	the referenced time offsets, or null if none
		 */
		public float[] getReferencedTimeOffsets()	{ return referencedTimeOffsets; }

		/**
		 * @return	the referenced datetimes, or null if none
		 */
		public String[] getReferencedDateTimes()	{ return referencedDateTimes; }
	}

	/***/
	public class NumericContentItem extends ContentItemWithValue {

		protected String numericValue;
		protected Double floatingPointValue;		// FD; null if absent
		protected Integer rationalNumeratorValue;	// SL; null if absent
		protected Long rationalDenominatorValue;	// UL; null if absent
		protected CodedSequenceItem units;
		protected CodedSequenceItem qualifier;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,AttributeList list) throws DicomException {
			super(parent,list);
			AttributeList l = null;
			SequenceAttribute a=(SequenceAttribute)(list.get(TagFromName.MeasuredValueSequence));
			if (a != null) {	// for SR uses (could check valueType.equals("NUM"), but this is more defensive
//System.err.println("NumericContentItem.NumericContentItem(): SR pattern with MeasuredValueSequence");
//System.err.println("NumericContentItem: MeasuredValueSequence="+a);
				Iterator i = a.iterator();
				if (i.hasNext()) {
					SequenceItem item = ((SequenceItem)i.next());
					if (item != null) {
//System.err.println("NumericContentItem: item="+item);
						l = item.getAttributeList();
					}
				}
			}
			else {
//System.err.println("NumericContentItem.NumericContentItem(): non-SR pattern without MeasuredValueSequence");
				l = list;	// for non-SR uses (could check valueType.equals("NUMERIC"), but this is more defensive
			}
			if (l != null) {
				numericValue=Attribute.getSingleStringValueOrEmptyString(l,TagFromName.NumericValue);
				{
					Attribute aFloatingPointValue = l.get(TagFromName.FloatingPointValue);
					floatingPointValue = (aFloatingPointValue == null || aFloatingPointValue.getVM() == 0) ? null : new Double(aFloatingPointValue.getDoubleValues()[0]);
				}
				{
					Attribute aRationalNumeratorValue = l.get(TagFromName.RationalNumeratorValue);
					rationalNumeratorValue = (aRationalNumeratorValue == null || aRationalNumeratorValue.getVM() == 0) ? null : new Integer(aRationalNumeratorValue.getIntegerValues()[0]);
				}
				{
					Attribute aRationalDenominatorValue = l.get(TagFromName.RationalDenominatorValue);
					rationalDenominatorValue = (aRationalDenominatorValue == null || aRationalDenominatorValue.getVM() == 0) ? null : new Long(aRationalDenominatorValue.getLongValues()[0]);
				}
				units=CodedSequenceItem.getSingleCodedSequenceItemOrNull(l,TagFromName.MeasurementUnitsCodeSequence);
			}
			if (numericValue == null) numericValue="";	// just for consistency with other string content items
			
			qualifier=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.NumericValueQualifierCodeSequence);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	floatingPointValue				will be converted to string
		 * @param	units							code for the units
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,double floatingPointValue,CodedSequenceItem units) throws DicomException {
			super(parent,isNotSR ? "NUMERIC" : "NUM",relationshipType,conceptName);
//System.err.println("NumericContentItem(): constructor checking for need for qualifiers for "+numericValue);
			if (floatingPointValue == Double.NaN || Double.isNaN(floatingPointValue)) {			// the constant match does not seem to work, hence the method call
//System.err.println("NumericContentItem(): matches NaN");
				constructOnlyQualifier(new CodedSequenceItem("114000","DCM","Not a number"));
			}
			else if (floatingPointValue == Double.NEGATIVE_INFINITY) {
				constructOnlyQualifier(new CodedSequenceItem("114001","DCM","Negative Infinity"));
			}
			else if (floatingPointValue == Double.POSITIVE_INFINITY) {
				constructOnlyQualifier(new CodedSequenceItem("114002","DCM","Positive Infinity"));
			}
			else {
				String finiteLengthStringValue = FloatFormatter.toStringOfFixedMaximumLength(floatingPointValue,16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US);
				boolean fullFidelity = false;
				try {
					double roundTripValue = Double.parseDouble(finiteLengthStringValue);
					if (floatingPointValue == roundTripValue) {	// require exact match, not just within epsilon
						fullFidelity = true;
					}
				}
				catch (NumberFormatException e) {
					// this should never happen, but if it does, leave fullFidelity == false
					slf4jlogger.error("", e);
				}
//System.err.println("NumericContentItem(): roundTripValue "+(fullFidelity ? "matches" : "does not match"));
				doCommonConstructorStuff(isNotSR,finiteLengthStringValue,units,null/*no qualifier*/,fullFidelity ? null : new Double(floatingPointValue),null/*rationalNumeratorValue*/,null/*rationalDenominatorValue*/);
			}
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	floatingPointValue				will be converted to string
		 * @param	units							code for the units
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,double floatingPointValue,CodedSequenceItem units) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,floatingPointValue,units);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numerator						integer numerator
		 * @param	denominator						integer denominator
		 * @param	units							code for the units
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,int numerator,long denominator,CodedSequenceItem units) throws DicomException {
			super(parent,isNotSR ? "NUMERIC" : "NUM",relationshipType,conceptName);
			if (denominator == 0) {
				constructOnlyQualifier(new CodedSequenceItem("114003","DCM","Divide by zero"));
			}
			else {
				double floatingPointValue = ((double)numerator)/denominator;
				String finiteLengthStringValue = FloatFormatter.toStringOfFixedMaximumLength(floatingPointValue,16/* maximum size of DS */,false/* non-numbers already handled */,Locale.US);
				boolean fullFidelity = false;
				try {
					double roundTripValue = Double.parseDouble(finiteLengthStringValue);
					if (floatingPointValue == roundTripValue) {	// require exact match, not just within epsilon
						fullFidelity = true;
					}
				}
				catch (NumberFormatException e) {
					// this should never happen, but if it does, leave fullFidelity == false
					slf4jlogger.error("", e);
				}
//System.err.println("NumericContentItem(): roundTripValue "+(fullFidelity ? "matches" : "does not match"));
				doCommonConstructorStuff(isNotSR,finiteLengthStringValue,units,null/*no qualifier*/,fullFidelity ? null : new Double(floatingPointValue),new Integer(numerator),new Long(denominator));
			}
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numerator						integer numerator
		 * @param	denominator						integer denominator
		 * @param	units							code for the units
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,int numerator,long denominator,CodedSequenceItem units) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,numerator,denominator,units);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numericValue					numeric value as decimal string
		 * @param	units							code for the units
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			super(parent,isNotSR ? "NUMERIC" : "NUM",relationshipType,conceptName);
			doCommonConstructorStuff(isNotSR,numericValue,units,qualifier,null/*floatingPointValue*/,null/*rationalNumeratorValue*/,null/*rationalDenominatorValue*/);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numericValue					numeric value as decimal string
		 * @param	units							code for the units
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,numericValue,units,null/*qualifier*/);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numericValue					numeric value as decimal string
		 * @param	units							code for the units
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,numericValue,units,qualifier);
		}
		
		/**
		 * @param	parent							parent content item to add to
		 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numericValue					numeric value as decimal string
		 * @param	floatingPointValue				numeric value as floating point
		 * @param	rationalNumeratorValue			integer numerator
		 * @param	rationalDenominatorValue		integer denominator
		 * @param	units							code for the units
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,String numericValue,Double floatingPointValue,Integer rationalNumeratorValue,Long rationalDenominatorValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			super(parent,isNotSR ? "NUMERIC" : "NUM",relationshipType,conceptName);
			doCommonConstructorStuff(isNotSR,numericValue,units,qualifier,floatingPointValue,rationalNumeratorValue,rationalDenominatorValue);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	numericValue					numeric value as decimal string
		 * @param	floatingPointValue				numeric value as floating point
		 * @param	rationalNumeratorValue			integer numerator
		 * @param	rationalDenominatorValue		integer denominator
		 * @param	units							code for the units
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,Double floatingPointValue,Integer rationalNumeratorValue,Long rationalDenominatorValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,numericValue,floatingPointValue,rationalNumeratorValue,rationalDenominatorValue,units,qualifier);
		}

		/**
		 * <p>Construct numeric content item with empty <code>MeasuredValueSequence</code> with qualifier explaining why it is empty.</p>
		 *
		 * @param	parent							parent content item to add to
		 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem qualifier) throws DicomException {
			super(parent,isNotSR ? "NUMERIC" : "NUM",relationshipType,conceptName);
			constructOnlyQualifier(qualifier);
		}

		/**
		 * <p>Construct SR numeric content item with empty <code>MeasuredValueSequence</code> with qualifier explaining why it is empty.</p>
		 *
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	qualifier						code for qualifier
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public NumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem qualifier) throws DicomException {
			this(parent,false/*isNotSR*/,relationshipType,conceptName,qualifier);
		}
		
		protected void doCommonConstructorStuff(boolean isNotSR,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier,Double floatingPointValue,Integer rationalNumeratorValue,Long rationalDenominatorValue) throws DicomException {
			AttributeList mvl = null;
			if (isNotSR) {
//System.err.println("NumericContentItem.doCommonConstructorStuff(): non-SR pattern without MeasuredValueSequence");
				mvl = list;		// for non-SR content items, the attributes are added at the same level as the value type, etc.
			}
			else {
//System.err.println("NumericContentItem.doCommonConstructorStuff(): SR pattern with MeasuredValueSequence");
				// for SR content items, the attributes are nested inside a MeasuredValueSequence
				SequenceAttribute mvs = new SequenceAttribute(TagFromName.MeasuredValueSequence); list.put(mvs);
				mvl = new AttributeList();
				mvs.addItem(mvl);
			}
			
			if (numericValue == null) {
				this.numericValue = "";		// just for consistency with other string content items
			}
			else {
				this.numericValue=numericValue;
				Attribute a = new DecimalStringAttribute(TagFromName.NumericValue); a.addValue(numericValue); mvl.put(a);
			}
			
			this.floatingPointValue=floatingPointValue;
			if (floatingPointValue != null) {
				Attribute a = new FloatDoubleAttribute(TagFromName.FloatingPointValue); a.addValue(floatingPointValue.doubleValue()); mvl.put(a);
			}
			
			this.rationalNumeratorValue=rationalNumeratorValue;
			if (rationalNumeratorValue != null) {
				Attribute a = new SignedLongAttribute(TagFromName.RationalNumeratorValue); a.addValue(rationalNumeratorValue.intValue()); mvl.put(a);
			}
			
			this.rationalDenominatorValue=rationalDenominatorValue;
			if (rationalDenominatorValue != null) {
				Attribute a = new UnsignedLongAttribute(TagFromName.RationalDenominatorValue); a.addValue(rationalDenominatorValue.longValue()); mvl.put(a);
			}
			
			this.units=units;
			if (units != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.MeasurementUnitsCodeSequence); a.addItem(units.getAttributeList()); mvl.put(a);
			}
			
			this.qualifier=qualifier;
			if (qualifier != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.NumericValueQualifierCodeSequence); a.addItem(qualifier.getAttributeList()); list.put(a); // list, not mvl !
			}
		}
		
		protected void constructOnlyQualifier(CodedSequenceItem qualifier) {
			SequenceAttribute mvs = new SequenceAttribute(TagFromName.MeasuredValueSequence); list.put(mvs);
			this.qualifier=qualifier;
			if (qualifier != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.NumericValueQualifierCodeSequence); a.addItem(qualifier.getAttributeList()); list.put(a); // list, not mvl !
			}
			this.numericValue = "";		// rather than null, just for consistency with other string content items
		}
		
		/**
		 * @return	the qualifier, or null if none
		 */
		public CodedSequenceItem getQualifier()		{ return qualifier; }

		/**
		 * @return	the units
		 */
		public CodedSequenceItem getUnits()		{ return units; }

		/**
		 * @return	the decimal string numeric value, or null if none
		 */
		public String getNumericValue()			{ return numericValue; }

		/**
		 * @return	true if there is a floating point value encoded
		 */
		public boolean hasFloatingPointValue()	{ return floatingPointValue != null; }

		/**
		 * @return	the floating point value
		 */
		public double getFloatingPointValue()	{ return floatingPointValue.doubleValue() ; }
		
		/**
		 * @return	true if there is a rational value encoded with an integer numerator and denominator
		 */
		public boolean hasRationalValue()	{ return rationalNumeratorValue != null && rationalDenominatorValue != null; }

		/**
		 * @return	the rational numerator value
		 */
		public int getRationalNumeratorValue()	{ return rationalNumeratorValue.intValue(); }

		/**
		 * @return	the rational denomninator value
		 */
		public long getRationalDenominatorValue()	{ return rationalDenominatorValue.longValue(); }
		

		public String getConceptValue() {
			return numericValue+" "+(units == null ? "" : units.getCodeMeaning());
		}

		public String getConceptNameAndValue() {
			return getConceptNameCodeMeaning()+" = "+numericValue+" "+(units == null ? "" : units.getCodeMeaning())+" "+(qualifier == null ? "" : qualifier.getCodeMeaning());
		}

		public String toString() {
			return super.toString()+" = "+numericValue+" "+(units == null ? "" : units.getCodeMeaning())+" "+(qualifier == null ? "" : qualifier.getCodeMeaning());
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	units							code for the units
	 * @param	qualifier						code for qualifier
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
		return new NumericContentItem(parent,isNotSR,relationshipType,conceptName,numericValue,units,qualifier);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	units							code for the units
	 * @param	qualifier						code for qualifier
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numericValue,units,qualifier);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	units							code for the units
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,double numericValue,CodedSequenceItem units) throws DicomException {
		return new NumericContentItem(parent,isNotSR,relationshipType,conceptName,numericValue,units);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	units							code for the units
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,double numericValue,CodedSequenceItem units) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numericValue,units);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numerator						integer numerator
	 * @param	denominator						integer denominator
	 * @param	units							code for the units
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,int numerator,long denominator,CodedSequenceItem units) throws DicomException {
		return new NumericContentItem(parent,isNotSR,relationshipType,conceptName,numerator,denominator,units);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numerator						integer numerator
	 * @param	denominator						integer denominator
	 * @param	units							code for the units
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,int numerator,long denominator,CodedSequenceItem units) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numerator,denominator,units);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	isNotSR							affects whether value type is NUM (false) or NUMERIC (true)
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	floatingPointValue				numeric value as floating point
	 * @param	rationalNumeratorValue			integer numerator
	 * @param	rationalDenominatorValue		integer denominator
	 * @param	units							code for the units
	 * @param	qualifier						code for qualifier
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,boolean isNotSR,String relationshipType,CodedSequenceItem conceptName,String numericValue,Double floatingPointValue,Integer rationalNumeratorValue,Long rationalDenominatorValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
		return new NumericContentItem(parent,isNotSR,relationshipType,conceptName,numericValue,floatingPointValue,rationalNumeratorValue,rationalDenominatorValue,units,qualifier);
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	numericValue					numeric value as decimal string
	 * @param	floatingPointValue				numeric value as floating point
	 * @param	rationalNumeratorValue			integer numerator
	 * @param	rationalDenominatorValue		integer denominator
	 * @param	units							code for the units
	 * @param	qualifier						code for qualifier
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public NumericContentItem makeNumericContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String numericValue,Double floatingPointValue,Integer rationalNumeratorValue,Long rationalDenominatorValue,CodedSequenceItem units,CodedSequenceItem qualifier) throws DicomException {
		return new NumericContentItem(parent,relationshipType,conceptName,numericValue,floatingPointValue,rationalNumeratorValue,rationalDenominatorValue,units,qualifier);
	}

	/***/
	public class CodeContentItem extends ContentItemWithValue {

		/***/
		protected CodedSequenceItem conceptCode;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public CodeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list);
			conceptCode=CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptCodeSequence);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	conceptCode						coded value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public CodeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem conceptCode) throws DicomException {
			super(parent,"CODE",relationshipType,conceptName);
			this.conceptCode=conceptCode;
			if (conceptCode != null) {
				SequenceAttribute a = new SequenceAttribute(TagFromName.ConceptCodeSequence); a.addItem(conceptCode.getAttributeList()); list.put(a);
			}
		}

		public String getConceptValue() {
			return (conceptCode == null ? "" : conceptCode.getCodeMeaning());
		}

		public String toString() {
			return super.toString()+" = "+(conceptCode == null ? "" : conceptCode.getCodeMeaning());
		}
		
		public CodedSequenceItem getConceptCode()    { return conceptCode; }

		/**
		 * Test if the coded value of the code content item matches the specified code value and coding scheme designator.
		 *
		 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
		 *
		 * @param	csdWanted		the coding scheme designator of the coded value wanted
		 * @param	cvWanted		the code value of the coded value wanted
		 * @return					true if matches
		 */
		public boolean contentItemValueMatchesCodeValueAndCodingSchemeDesignator(String cvWanted,String csdWanted) {
			boolean isMatch = false;
			if (conceptCode != null) {
				String csd = conceptCode.getCodingSchemeDesignator();
				String cv = conceptCode.getCodeValue();
				if (csd != null && csd.trim().equals(csdWanted.trim()) && cv != null && cv.trim().equals(cvWanted.trim())) {
					isMatch = true;
				}
			}
			return isMatch;
		}
	}
	
	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	conceptCode						coded value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public CodeContentItem makeCodeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,CodedSequenceItem conceptCode) throws DicomException {
		return new CodeContentItem(parent,relationshipType,conceptName,conceptCode);
	}
	
	/**
	 * Test if the coded value of the code content item matches the specified code value and coding scheme designator.
	 *
	 * This is more robust than checking code meaning, which may have synomyms, and there is no need to also test code meaning.
	 *
	 * @param	ci				the content item to check
	 * @param	csdWanted		the coding scheme designator of the coded value wanted
	 * @param	cvWanted		the code value of the coded value wanted
	 * @return					true if matches
	 */
	public static boolean codeContentItemValueMatchesCodeValueAndCodingSchemeDesignator(ContentItem ci,String cvWanted,String csdWanted) {
		boolean isMatch = false;
		if (ci != null && ci instanceof ContentItemFactory.CodeContentItem) {
			ContentItemFactory.CodeContentItem cci = ( ContentItemFactory.CodeContentItem)ci;
			isMatch = cci.contentItemValueMatchesCodeValueAndCodingSchemeDesignator(cvWanted,csdWanted);
		}
		return isMatch;
	}

	/***/
	abstract protected class StringContentItem extends ContentItemWithValue {

		protected String stringValue;

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							list of attributes for this content item
		 * @param	tag								tag of the attribute containing the string value of this content item
		 */
		public StringContentItem(ContentItem parent,AttributeList list,AttributeTag tag) {
			super(parent,list);
			stringValue=Attribute.getSingleStringValueOrDefault(list,tag,"");
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	valueType						the value type
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	tagForValue						tag of the attribute to encode the string value of this content item
		 * @param	stringValue						string value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public StringContentItem(ContentItem parent,String valueType,String relationshipType,CodedSequenceItem conceptName,AttributeTag tagForValue,String stringValue) throws DicomException {
			super(parent,valueType,relationshipType,conceptName);
			this.stringValue=stringValue;
			if (stringValue != null) {
				Attribute a = AttributeFactory.newAttribute(tagForValue);
				a.addValue(stringValue);
				list.put(a);
			}
		}

		public String getConceptValue() {
			return stringValue;
		}

		/**
		 * @param	tagForValue						tag of the attribute to encode the string value of this content item
		 * @param	stringValue						if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(AttributeTag tagForValue,String stringValue) throws DicomException {
			this.stringValue=stringValue;
			if (stringValue == null) {
				list.remove(tagForValue);
			}
			else {
				Attribute a = AttributeFactory.newAttribute(tagForValue);
				a.addValue(stringValue);
				list.put(a);
			}
		}

		public String toString() {
			return super.toString()+" = "+stringValue;
		}
	}

	/***/
	public class DateTimeContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public DateTimeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.DateTime);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	dateTimeValue					datetime value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public DateTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String dateTimeValue) throws DicomException {
			super(parent,"DATETIME",relationshipType,conceptName,TagFromName.DateTime,dateTimeValue);
		}

		/**
		 * @param	dateTimeValue					if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String dateTimeValue) throws DicomException {
			setConceptValue(TagFromName.DateTime,dateTimeValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	dateTimeValue					datetime value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public DateTimeContentItem makeDateTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String dateTimeValue) throws DicomException {
		return new DateTimeContentItem(parent,relationshipType,conceptName,dateTimeValue);
	}

	/***/
	public class DateContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public DateContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.Date);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	dateValue						date value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public DateContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String dateValue) throws DicomException {
			super(parent,"DATE",relationshipType,conceptName,TagFromName.Date,dateValue);
		}

		/**
		 * @param	dateValue						if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String dateValue) throws DicomException {
			setConceptValue(TagFromName.Date,dateValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	dateValue						date value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public DateContentItem makeDateContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String dateValue) throws DicomException {
		return new DateContentItem(parent,relationshipType,conceptName,dateValue);
	}

	/***/
	public class TimeContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public TimeContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.Time);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	timeValue						time value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public TimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String timeValue) throws DicomException {
			super(parent,"TIME",relationshipType,conceptName,TagFromName.Time,timeValue);
		}

		/**
		 * @param	timeValue						if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String timeValue) throws DicomException {
			setConceptValue(TagFromName.Time,timeValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	timeValue						time value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public TimeContentItem makeTimeContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String timeValue) throws DicomException {
		return new TimeContentItem(parent,relationshipType,conceptName,timeValue);
	}

	/***/
	public class PersonNameContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public PersonNameContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.PersonName);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	personNameValue					person name value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public PersonNameContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String personNameValue) throws DicomException {
			super(parent,"PNAME",relationshipType,conceptName,TagFromName.PersonName,personNameValue);
		}

		/**
		 * @param	personNameValue					if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String personNameValue) throws DicomException {
			setConceptValue(TagFromName.PersonName,personNameValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	personNameValue					person name value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public PersonNameContentItem makePersonNameContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String personNameValue) throws DicomException {
		return new PersonNameContentItem(parent,relationshipType,conceptName,personNameValue);
	}

	/***/
	public class UIDContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public UIDContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.UID);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	uidValue						UID value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public UIDContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String uidValue) throws DicomException {
			super(parent,"UIDREF",relationshipType,conceptName,TagFromName.UID,uidValue);
		}

		/**
		 * @param	uidValue						if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String uidValue) throws DicomException {
			setConceptValue(TagFromName.UID,uidValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	uidValue						UID value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public UIDContentItem makeUIDContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String uidValue) throws DicomException {
		return new UIDContentItem(parent,relationshipType,conceptName,uidValue);
	}

	/***/
	public class TextContentItem extends StringContentItem {

		/**
		 * @param	parent							parent content item to add to
		 * @param	list							the list of attributes for this content item
		 */
		public TextContentItem(ContentItem parent,AttributeList list) {
			super(parent,list,TagFromName.TextValue);
		}

		/**
		 * @param	parent							parent content item to add to
		 * @param	relationshipType				relationship type
		 * @param	conceptName						coded concept name
		 * @param	textValue						text value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public TextContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String textValue) throws DicomException {
			super(parent,"TEXT",relationshipType,conceptName,TagFromName.TextValue,textValue);
		}

		/**
		 * @param	textValue						if null, removes the value
		 * @throws	DicomException					if error in DICOM encoding
		 */
		public void setConceptValue(String textValue) throws DicomException {
			setConceptValue(TagFromName.TextValue,textValue);
		}
	}

	/**
	 * @param	parent							parent content item to add to
	 * @param	relationshipType				relationship type
	 * @param	conceptName						coded concept name
	 * @param	textValue						text value
	 * @return									the content item created
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public TextContentItem makeTextContentItem(ContentItem parent,String relationshipType,CodedSequenceItem conceptName,String textValue) throws DicomException {
		return new TextContentItem(parent,relationshipType,conceptName,textValue);
	}

	/**
	 * <p>Construct a content item of the appropriate class from a list of attributes.</p>
	 *
	 * @param	parent							the parent to add the content item to
	 * @param	list							a list of attributes that constitute the content item as it is encoded in a DICOM data set
	 * @return									a content item
	 * @throws	DicomException					if error in DICOM encoding
	 */
	public ContentItem getNewContentItem(ContentItem parent,AttributeList list) throws DicomException {
		ContentItem contentItem = null;

		if (list == null) {
			contentItem = new UnrecognizedContentItem(parent);
		}
		else {
			String valueType=Attribute.getSingleStringValueOrNull(list,TagFromName.ValueType);
			if (valueType == null) {
				if (list.get(TagFromName.ReferencedContentItemIdentifier) != null) {
					contentItem = new ContentItemWithReference(parent,list);
				}
			}
			else if (valueType.equals("CONTAINER")) {
				contentItem = new ContainerContentItem(parent,list);
			}
			else if (valueType.equals("CODE")) {
				contentItem = new CodeContentItem(parent,list);
			}
			else if (valueType.equals("NUM") || valueType.equals("NUMERIC")) {	// NUM is used in SR trees, NUMERIC is used in Acquisition and Protocol templates (PS 3.3 10-2 Content Item Macro).
				contentItem = new NumericContentItem(parent,list);
			}
			else if (valueType.equals("DATETIME")) {
				contentItem = new DateTimeContentItem(parent,list);
			}
			else if (valueType.equals("DATE")) {
				contentItem = new DateContentItem(parent,list);
			}
			else if (valueType.equals("TIME")) {
				contentItem = new TimeContentItem(parent,list);
			}
			else if (valueType.equals("PNAME")) {
				contentItem = new PersonNameContentItem(parent,list);
			}
			else if (valueType.equals("UIDREF")) {
				contentItem = new UIDContentItem(parent,list);
			}
			else if (valueType.equals("TEXT")) {
				contentItem = new TextContentItem(parent,list);
			}
			else if (valueType.equals("SCOORD")) {
				contentItem = new SpatialCoordinatesContentItem(parent,list);
			}
			else if (valueType.equals("SCOORD3D")) {
				contentItem = new SpatialCoordinates3DContentItem(parent,list);
			}
			else if (valueType.equals("TCOORD")) {
				contentItem = new TemporalCoordinatesContentItem(parent,list);
			}
			else if (valueType.equals("COMPOSITE")) {
				contentItem = new CompositeContentItem(parent,list);
			}
			else if (valueType.equals("IMAGE")) {
				contentItem = new ImageContentItem(parent,list);
			}
			else if (valueType.equals("WAVEFORM")) {
				contentItem = new WaveformContentItem(parent,list);
			}
			else {
				contentItem = new UnrecognizedContentItem(parent,list,valueType);
			}
		}

		return contentItem;
	}
}



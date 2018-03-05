/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class to encapsulate information describing references in a structured
 * report to images, with or without spatial coordinates.</p>
 *
 * @see com.pixelmed.dicom.StructuredReport#findAllContainedSOPInstances(ContentItem,ContentItem)
 *
 * @author	dclunie
 */
public class SpatialCoordinateAndImageReference {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SpatialCoordinateAndImageReference.java,v 1.14 2017/01/24 10:50:39 dclunie Exp $";

	public static final int RenderingRequired = 0;
	public static final int RenderingOptional = 1;
	public static final int RenderingForbidden = 2;

	private int renderingIntent;		// with values of RenderingRequired, RenderingOptional or RenderingForbidden

	public static final int CoordinateCategoryUnspecified = 0;
	public static final int CoordinateCategoryMammoIndividualCalcification = 1;
	public static final int CoordinateCategoryMammoCalcificationCluster = 2;
	public static final int CoordinateCategoryMammoBreastDensity = 3;

	private int coordinateCategory;		// with values of CoordinateCategoryXXX

	private String sopInstanceUID;
	private String sopClassUID;
	private String graphicType;
	private float[] graphicData;
	private String annotation;
	private boolean imageLibraryEntry;

	/**
	 * <p>Construct an instance of a reference to an image, with or without spatial coordinate references.</p>
	 *
	 * @param	sopInstanceUID		the SOP Instance UID (should always present)
	 * @param	sopClassUID			the SOP Class UID (may be null)
	 * @param	graphicType			the Graphic Type if a spatial coordinate, otherwise null
	 * @param	graphicData			the Graphic Data if a spatial coordinate, otherwise null
	 * @param	annotation			a {@link java.lang.String String} value describing the reference (typically a parent Concept Name)
	 * @param	renderingIntent		whether or not to render the annotation (e.g., as specified by rendering intent in CAD IODs)
	 * @param	coordinateCategory	the category of coordinate or annotation
	 * @param	imageLibraryEntry	whether or not the reference is in an Image Library
	 */
	public SpatialCoordinateAndImageReference(String sopInstanceUID,String sopClassUID,String graphicType,float[] graphicData,String annotation,int renderingIntent,int coordinateCategory,boolean imageLibraryEntry) {
		this.sopInstanceUID=sopInstanceUID;
		this.sopClassUID=sopClassUID;
		this.graphicType=graphicType;
		this.graphicData=graphicData;
		this.annotation=annotation;
		this.renderingIntent=renderingIntent;
		this.coordinateCategory=coordinateCategory;
		this.imageLibraryEntry=imageLibraryEntry;
	}

	/**
	 * <p>Construct an instance of a reference to an image, with or without spatial coordinate references.</p>
	 *
	 * @param	sopInstanceUID		the SOP Instance UID (should always present)
	 * @param	sopClassUID			the SOP Class UID (may be null)
	 * @param	graphicType			the Graphic Type if a spatial coordinate, otherwise null
	 * @param	graphicData			the Graphic Data if a spatial coordinate, otherwise null
	 * @param	annotation			a {@link java.lang.String String} value describing the reference (typically a parent Concept Name)
	 * @param	renderingIntent		whether or not to render the annotation (e.g., as specified by rendering intent in CAD IODs)
	 * @param	coordinateCategory	the category of coordinate or annotation
	 */
	public SpatialCoordinateAndImageReference(String sopInstanceUID,String sopClassUID,String graphicType,float[] graphicData,String annotation,int renderingIntent,int coordinateCategory) {
		this(sopInstanceUID,sopClassUID,graphicType,graphicData,annotation,renderingIntent,coordinateCategory,false/*imageLibraryEntry*/);
	}

	/**
	 * <p>Get the SOP Instance UID.</p>
	 *
	 * @return		the SOP Instance UID, or null
	 */
	public String getSOPInstanceUID() { return sopInstanceUID; }

	/**
	 * <p>Get the SOP Class UID.</p>
	 *
	 * @return		the SOP Class UID, or null
	 */
	public String getSOPClassUID()    { return sopClassUID; }

	/**
	 * <p>Get the Graphic Type.</p>
	 *
	 * @return		the Graphic Type, or null
	 */
	public String getGraphicType()    { return graphicType; }

	/**
	 * <p>Get the Graphic Data.</p>
	 *
	 * @return		the Graphic Data, or null
	 */
	public float[] getGraphicData()   { return graphicData; }

	/**
	 * <p>Get the annotation.</p>
	 *
	 * @return		the annotation
	 */
	public String getAnnotation()     { return annotation; }

	/**
	 * <p>Get rendering intent.</p>
	 *
	 * @return		values of SpatialCoordinateAndImageReference.RenderingRequired, SpatialCoordinateAndImageReference.RenderingOptional or SpatialCoordinateAndImageReference.RenderingForbidden
	 */
	public int getRenderingIntent()     { return renderingIntent; }

	/**
	 * <p>Is the reference an Image Library entry.</p>
	 *
	 * @return		true if an Image Library entry
	 */
	public boolean getImageLibraryEntry()     { return imageLibraryEntry; }

	/**
	 * <p>Get rendering intent as {@link java.lang.String String}.</p>
	 *
	 * @return	a {@link java.lang.String String} describing the rendering intent
	 */
	private String getRenderingIntentAsString() {
		String s = "rendering";
		if (renderingIntent == RenderingRequired) {
			s = s+" required";
		}
		else if (renderingIntent == RenderingOptional) {
			s = s+" optional";
		}
		else if (renderingIntent == RenderingForbidden) {
			s = s+" forbidden";
		}
		return s;
	}

	/**
	 * <p>Get category.</p>
	 *
	 * @return	category
	 */
	public int getCoordinateCategory()     { return coordinateCategory; }

	/**
	 * <p>Get category as {@link java.lang.String String}.</p>
	 *
	 * @return	a {@link java.lang.String String} describing the coordinate category
	 */
	private String getCoordinateCategoryAsString() {
		String s = "category";
		if (coordinateCategory == CoordinateCategoryUnspecified) {
			s = s+" unspecified";
		}
		else if (coordinateCategory == CoordinateCategoryMammoIndividualCalcification) {
			s = s+" individual calcification";
		}
		else if (coordinateCategory == CoordinateCategoryMammoCalcificationCluster) {
			s = s+" calcification cluster";
		}
		else if (coordinateCategory == CoordinateCategoryMammoBreastDensity) {
			s = s+" breast density";
		}
		return s;
	}

	/**
	 * <p>Dump a human-readable representation as a {@link java.lang.String String}.</p>
	 *
	 * @return		the annotation
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(sopInstanceUID);
		str.append(":");
		str.append(sopClassUID);
		str.append(" = ");
		str.append(graphicType);
		if (imageLibraryEntry) {
			str.append(" [Image Library]");
		}
		str.append(" [");
		str.append(getRenderingIntentAsString());
		str.append(",");
		str.append(getCoordinateCategoryAsString());
		str.append("] (");
		if (graphicData != null) {
			for (int j=0; j<graphicData.length; ++j) {
				if (j > 0) str.append(",");
				str.append(graphicData[j]);
			}
		}
		str.append(") ");
		str.append(annotation);
		return str.toString();
	}
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;

import com.pixelmed.display.event.ApplyShutterChangeEvent;
import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.FrameSortOrderChangeEvent;
import com.pixelmed.display.event.GraphicDisplayChangeEvent;
import com.pixelmed.display.event.StatusChangeEvent;
import com.pixelmed.display.event.VOIFunctionChangeEvent;
import com.pixelmed.display.event.WindowingAccelerationValueChangeEvent;
import com.pixelmed.display.event.WindowLinearCalculationChangeEvent;
import com.pixelmed.display.event.WindowCenterAndWidthChangeEvent;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DisplayShutter;
import com.pixelmed.dicom.ModalityTransform;
import com.pixelmed.dicom.Overlay;
import com.pixelmed.dicom.RealWorldValueTransform;
import com.pixelmed.dicom.SUVTransform;
import com.pixelmed.dicom.VOITransform;

import com.pixelmed.utils.ColorUtilities;
import com.pixelmed.utils.FloatFormatter;

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import java.awt.Font;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BandCombineOp;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.swing.JComponent;
import javax.swing.JFrame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Implements a component that can display a single or multi-frame image in a
 * single panel with window center/width adjustment, scrolling through frames
 * of a multi-frame image, resizing to the size of the panel, annotation
 * of pre-defined text and shapes, feedback of cursor position status (2D and
 * 3D position, pixel value, window).</p>
 *
 * <p>Includes a main() method for testing that will display a single image from
 * a file in a JFrame.</p>
 *
 * <p>About the most minimal code to display a single DICOM image looks like this:</p>
 *
 * <pre>
 * JFrame p = new JFrame();
 * p.add(new SingleImagePanel(new SourceImage(filename)));
 * p.setBackground(Color.BLACK);
 * p.setSize(512,512);
 * p.setVisible(true);
 * </pre>
 *
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
public class SingleImagePanel extends JComponent implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SingleImagePanel.java,v 1.204 2017/02/28 16:12:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SingleImagePanel.class);

	/***/
	SourceImage sImg;
	/***/
	int currentSrcImageIndex;
	/***/
	int[] currentSrcImageSortOrder;

	/***/
	BufferedImage cachedResizedImage;
	/***/
	BufferedImage cachedResizedSelectedRegionImage;
	/***/
	BufferedImage cachedPreWindowedImage;
	/***/
	int offsetDrawingOfResizedSelectedRegionImageX;
	/***/
	int offsetDrawingOfResizedSelectedRegionImageY;
	/***/
	Rectangle cachedWindowSize;
	
	public void dirty() {
		dirtySource();
	}

	public void dirtySource() {
		cachedResizedImage=null;
		cachedResizedSelectedRegionImage=null;
		cachedPreWindowedImage=null;
	}
	
	public void dirty(SourceImage sImg) {
		dirtySource(sImg);
	}

	public void dirtySource(SourceImage sImg) {
		this.sImg=sImg;
		this.realWorldValueTransform=sImg.getRealWorldValueTransform();
		this.modalityTransform=sImg.getModalityTransform();
		this.voiTransform=sImg.getVOITransform();
		this.displayShutter=sImg.getDisplayShutter();
		this.overlay=sImg.getOverlay();
		cachedResizedImage=null;
		cachedResizedSelectedRegionImage=null;
		cachedPreWindowedImage=null;
	}

	public void dirtyWindowing() {
		cachedPreWindowedImage=null;
	}

	public void dirtyPanned() {
		cachedPreWindowedImage=null;
		cachedResizedSelectedRegionImage=null;
	}
	
	protected Set<SuperimposedImage.AppliedToUnderlyingImage> activeSuperimposedImagesAppliedToUnderlyingImage = new HashSet<SuperimposedImage.AppliedToUnderlyingImage>();

	/**
	 * whether or not to use the supplied VOI LUT, rather than a linear or sigmoid window function
	 */
	protected boolean useVOILUTNotFunction;
	/**
	 * the currently selected, default or user modified window center
	 */
	protected double windowCenter;
	/**
	 * the currently selected, default or user modified window width
	 */
	protected double windowWidth;
	/**
	 * the currently selected VOI LUT window width value that will result in the application of the VOI LUT rescaling the input (index) values
	 */
	protected double voiLUTIdentityWindowWidth;
	/**
	 * the currently selected VOI LUT window center value that will result in the application of the VOI LUT rescaling the input (index) values
	 */
	protected double voiLUTIdentityWindowCenter;
	/**
	 * the currently selected VOI LUT number of entries
	 */
	protected int voiLUTNumberOfEntries;
	/**
	 * the currently selected VOI LUT first value mapped
	 */
	protected int voiLUTFirstValueMapped;
	/**
	 * the currently selected VOI LUT bits per entry
	 */
	protected int voiLUTBitsPerEntry;
	/**
	 * the currently selected VOI LUT Data
	 */
	protected short[] voiLUTData;
	/**
	 * the currently selected VOI LUT minimum entry value
	 */
	protected int voiLUTEntryMin;
	/**
	 * the currently selected VOI LUT maximum entry value
	 */
	protected int voiLUTEntryMax;
	/**
	 * the currently selected VOI LUT top of entry range (which may be less than (2^voiLUTNumberOfEntries)-1, e.g., in buggy Agfa images)
	 */
	protected int voiLUTTopOfEntryRange;
	
	/**
	 * the value of rescale slope to use for current frame (set when new frame selected)
	 */
	double useSlope;
	/**
	 * the value of rescale intercept to use for current frame (set when new frame selected)
	 */
	double useIntercept;

	/***/
	double imgMin;
	/***/
	double imgMax;

	/***/
	private int largestGray;
	/***/
	private int firstvalueMapped;
	/***/
	private int numberOfEntries;
	/***/
	private int bitsPerEntry;
	/***/
	private short redTable[];
	/***/
	private short greenTable[];
	/***/
	private short blueTable[];

	/***/
	private boolean signed;
	/***/
	private boolean inverted;
	/***/
	private boolean ybr;
	/***/
	private int signMask;
	/***/
	private int signBit;
	
	/***/
	int pad;
	/***/
	int padRangeLimit;
	/***/
	boolean hasPad;

	/***/
	SUVTransform suvTransform;

	/***/
	RealWorldValueTransform realWorldValueTransform;

	/***/
	ModalityTransform modalityTransform;

	/***/
	VOITransform voiTransform;

	/***/
	DisplayShutter displayShutter;

	/***/
	Overlay overlay;
	
	/***/
	protected GeometryOfVolume imageGeometry;
	
	/**
	 * <p>Get the geometry of the frames currently loaded in the single image panel.</p>
	 *
	 * @return	the geometry of the frames
	 */
	public GeometryOfVolume getImageGeometry() {
		return imageGeometry;
	}

	protected Vector<Shape> preDefinedShapes;
	protected Vector<Shape> preDefinedText;
	protected Vector<Shape> localizerShapes;
	protected Vector<Shape> volumeLocalizationShapes;
	protected Vector<Shape> interactiveDrawingShapes;
	protected Vector<Shape> persistentDrawingShapes;
	protected Vector<Shape> selectedDrawingShapes;
	protected Vector<TextAnnotation> persistentDrawingText;
	protected Vector<Shape>[] perFrameDrawingShapes;	// array size will be number of frames; will be indexed by currentSrcImageIndex just like the image frames
	protected Vector<SuperimposedImage> superimposedImages;

	/**
	 * <p>Set the predefined shapes to to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	shapes	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final void setPreDefinedShapes(Vector shapes) {
//System.err.println("SingleImagePanel.setPreDefinedShapes(): preDefinedShapes was:");
//com.pixelmed.geometry.LocalizerPosterFactory.dumpShapes(preDefinedShapes);
//System.err.println("SingleImagePanel.setPreDefinedShapes(): setting preDefinedShapes to:");
//com.pixelmed.geometry.LocalizerPosterFactory.dumpShapes(shapes);
		this.preDefinedShapes=shapes;
	}

	/**
	 * <p>Set the shapes of any localizer postings to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	shapes	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final void setLocalizerShapes(Vector shapes) { this.localizerShapes=shapes; }

	/**
	 * <p>Set the shapes of any volume localization postings to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	shapes	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final void setVolumeLocalizationShapes(Vector shapes) { this.volumeLocalizationShapes=shapes; }
	
	/**
	 * <p>Set the unselected region shapes to to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	shapes	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final void setPersistentDrawingShapes(Vector shapes) { persistentDrawingShapes=shapes; }
	
	/**
	 * <p>Get the unselected region shapes to to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @return	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final Vector getPersistentDrawingShapes() { return persistentDrawingShapes; }
	
	/**
	 * <p>Set the selected region shapes to to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	shapes	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final void setSelectedDrawingShapes(Vector shapes) { selectedDrawingShapes=shapes; }
	
	/**
	 * <p>Get the selected region shapes to to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @return	a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}, may be null or empty
	 */
	public final Vector getSelectedDrawingShapes() { return selectedDrawingShapes; }

	/**
	 * <p>Set the array of per-frame shapes to be displayed on the respective frame when it is selected and displayed.</p>
	 *
	 * @param	arrayOfShapes	an array of {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape java.awt.Shape}
	 */
	public final void setPerFrameDrawingShapes(Vector<Shape>[] arrayOfShapes) { this.perFrameDrawingShapes=arrayOfShapes; }
	
	/**
	 * <p>Set the superimposed images to to be displayed on the appropriate frames.</p>
	 *
	 * @param	superimposedImages	a {@link java.util.Vector java.util.Vector} of {@link com.pixelmed.display.SuperimposedImage com.pixelmed.display.SuperimposedImage}, may be null or empty
	 */
	public final void setSuperimposedImages(Vector<SuperimposedImage> superimposedImages) { this.superimposedImages = superimposedImages; }
	
	/**
	 * <p>Get the superimposed images to to be displayed on the appropriate frames.</p>
	 *
	 * @return	a {@link java.util.Vector java.util.Vector} of {@link com.pixelmed.display.SuperimposedImage com.pixelmed.display.SuperimposedImage}, may be null or empty
	 */
	public final Vector<SuperimposedImage> getSuperimposedImages() { return superimposedImages; }

	/***/
	private DemographicAndTechniqueAnnotations demographicAndTechniqueAnnotations = null;
	/***/
	private Color demographicAndTechniqueColor;
	/***/
	private Font demographicAndTechniqueFont;

	/**
	 * <p>Set annotative attribute values to be displayed on the currently selected and displayed frame at defined positions.</p>
	 *
	 * @param	demographicAndTechniqueAnnotations	the set of annotations, indexed per frame
	 * @param	demographicAndTechniqueFontName		the name of the font, e.g. "SansSerif"
	 * @param	demographicAndTechniqueFontStyle	the style of the font, e.g. Font.PLAIN
	 * @param	demographicAndTechniqueFontSize		the size of the font in points, e.g. 10
	 * @param	demographicAndTechniqueColor		the color to draw, e.g. Color.pink
	 */
	public final void setDemographicAndTechniqueAnnotations(DemographicAndTechniqueAnnotations demographicAndTechniqueAnnotations,
			String demographicAndTechniqueFontName,int demographicAndTechniqueFontStyle,int demographicAndTechniqueFontSize,Color demographicAndTechniqueColor) {
		this.demographicAndTechniqueAnnotations=demographicAndTechniqueAnnotations;
		this.demographicAndTechniqueFont = new Font(demographicAndTechniqueFontName,demographicAndTechniqueFontStyle,demographicAndTechniqueFontSize);
		this.demographicAndTechniqueColor=demographicAndTechniqueColor;
	}

	/***/
	protected OrientationAnnotations orientationAnnotations = null;
	/***/
	protected Color orientationColor;
	/***/
	protected Font orientationFont;
	/***/
	protected boolean showOrientationsLeftSide = false;

	/**
	 * <p>Set orientation values to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	orientationAnnotations	the set of orientations, indexed per frame
	 * @param	orientationFontName		the name of the font, e.g. "SansSerif"
	 * @param	orientationFontStyle	the style of the font, e.g. Font.PLAIN
	 * @param	orientationFontSize		the size of the font in points, e.g. 20
	 * @param	orientationColor		the color to draw, e.g. Color.pink
	 */
	public final void setOrientationAnnotations(OrientationAnnotations orientationAnnotations,
			String orientationFontName,int orientationFontStyle,int orientationFontSize,Color orientationColor) {
		setOrientationAnnotations(orientationAnnotations,orientationFontName,orientationFontStyle,orientationFontSize,orientationColor,false);
	}
	
	/**
	 * <p>Set orientation values to be displayed on the currently selected and displayed frame.</p>
	 *
	 * @param	orientationAnnotations	the set of orientations, indexed per frame
	 * @param	orientationFontName		the name of the font, e.g. "SansSerif"
	 * @param	orientationFontStyle	the style of the font, e.g. Font.PLAIN
	 * @param	orientationFontSize		the size of the font in points, e.g. 20
	 * @param	orientationColor		the color to draw, e.g. Color.pink
	 * @param	leftSide				show row orientation on left (true) or right (false) side of view port
	 */
	public final void setOrientationAnnotations(OrientationAnnotations orientationAnnotations,
			String orientationFontName,int orientationFontStyle,int orientationFontSize,Color orientationColor,boolean leftSide) {
		this.orientationAnnotations=orientationAnnotations;
		this.orientationFont = new Font(orientationFontName,orientationFontStyle,orientationFontSize);
		this.orientationColor=orientationColor;
		this.showOrientationsLeftSide=leftSide;
	}
	
	/***/
	protected String sideAndViewAnnotationString;
	/***/
	protected int sideAndViewAnnotationVerticalOffset;
	/***/
	protected Color sideAndViewAnnotationColor;
	/***/
	protected Font sideAndViewAnnotationFont;
	/***/
	protected boolean showSideAndViewAnnotationLeftSide = false;

	/**
	 * <p>Set side and view annotation string.</p>
	 *
	 * @param	annotationString		additional side (image laterality) and view (e.g., mammo CC) string to show on same side
	 * @param	verticalOffset
	 * @param	annotationFontName		the name of the font, e.g. "SansSerif"
	 * @param	annotationFontStyle		the style of the font, e.g. Font.PLAIN
	 * @param	annotationFontSize		the size of the font in points, e.g. 20
	 * @param	annotationColor			the color to draw, e.g. Color.pink
	 * @param	leftSide				show row orientation on left (true) or right (false) side of view port
	 */
	public final void setSideAndViewAnnotationString(String annotationString,int verticalOffset,
			String annotationFontName,int annotationFontStyle,
			int annotationFontSize,Color annotationColor,boolean leftSide) {
		this.sideAndViewAnnotationString = annotationString;
		this.sideAndViewAnnotationVerticalOffset = verticalOffset;
		this.sideAndViewAnnotationFont = new Font(annotationFontName,annotationFontStyle,annotationFontSize);
		this.sideAndViewAnnotationColor = annotationColor;
		this.showSideAndViewAnnotationLeftSide=leftSide;
	}
	
	/***/
	protected DisplayedAreaSelection originalDisplayedAreaSelection = null;
	/***/
	private DisplayedAreaSelection useDisplayedAreaSelection = null;

	/**
	 * <p>Select the sub-region of the image to display.</p>
	 *
	 * @param	displayedAreaSelection	the selection, or null to reset to using the entire image
	 */
	public final void setDisplayedAreaSelection(DisplayedAreaSelection displayedAreaSelection) {
		originalDisplayedAreaSelection=displayedAreaSelection;
		useDisplayedAreaSelection=null;							// defer fitting until window size is known in paintComponent()
	}
	
	public void displayReset() {
		slf4jlogger.debug("displayReset()");
		dirtySource();
		useDisplayedAreaSelection=null;
		establishInitialWindowOrVOILUT();
	}

	public void displaySelectedMagnificationRatio(double ratio) {
		slf4jlogger.debug("displaySelectedMagnificationRatio(): ratio ={}",ratio);
		dirtySource();
		// originalDisplayedAreaSelection should never be null ... would have been initialized with default on first paintComponent();
		try {
			useDisplayedAreaSelection = (DisplayedAreaSelection)originalDisplayedAreaSelection.clone();
		}
		catch (Exception e) {				// e.g., CloneNotSupportedException ... should never happen
			slf4jlogger.error("",e);
			useDisplayedAreaSelection = null;
		}
		if (useDisplayedAreaSelection != null) {
			useDisplayedAreaSelection.setPixelMagnificationRatio(ratio);
			useDisplayedAreaSelection = useDisplayedAreaSelection.shapeSelectionToMatchAvailableWindow(cachedWindowSize);
			// leave window or VOI LUT alone
		}
	}

	/***/
	private AffineTransform preTransformImageRelativeCoordinates = null;

	/**
	 * <p>Select the AffineTransform to apply to image-relative coordinates.</p>
	 *
	 * <p>Used in cases where the supplied image has already been flipped or rotated
	 * but the coordinates relative to the original image have not.</p>
	 *
	 * @param	transform	the transform, or null to reset to no transform
	 */
	public final void setPreTransformImageRelativeCoordinates(AffineTransform transform) {
		this.preTransformImageRelativeCoordinates=transform;
	}

	/***/
	protected boolean showZoomFactor = false;
	/***/
	protected boolean showZoomFactorLeftSide = false;
	/***/
	protected double pixelSpacingInSourceImage = 0;
	/***/
	protected String typeOfPixelSpacing;

	/**
	 * <p>Select whether or not to annotate displayed image with zoom factor.</p>
	 *
	 * <p>Uses same font parameters as set for orientation annotations.</p>
	 *
	 * <p>Also implicitly effects setPixelSpacingInSourceImage().</p>
	 *
	 * @param	showZoomFactor				true or false to activate annotation of zoom factor
	 * @param	leftSide					show zoom factor on left (true) or right (false) side of view port
	 * @param	pixelSpacingInSourceImage	a single value that is the (square) row and column pixel spacing, or 0 if not known
	 */
	public final void setShowZoomFactor(boolean showZoomFactor,boolean leftSide,double pixelSpacingInSourceImage) {
		this.showZoomFactor=showZoomFactor;
		this.showZoomFactorLeftSide=leftSide;
		this.pixelSpacingInSourceImage=pixelSpacingInSourceImage;
	}

	/**
	 * <p>Select whether or not to annotate displayed image with zoom factor.</p>
	 *
	 * <p>Uses same font parameters as set for orientation annotations.</p>
	 *
	 * <p>Also implicitly effects setPixelSpacingInSourceImage().</p>
	 *
	 * @param	showZoomFactor				true or false to activate annotation of zoom factor
	 * @param	leftSide					show zoom factor on left (true) or right (false) side of view port
	 * @param	pixelSpacingInSourceImage	a single value that is the (square) row and column pixel spacing, or 0 if not known
	 * @param	typeOfPixelSpacing       	a String that describes the type of pixel spacing (e.g., detector plane, calibrated, accounting for geometric magnification, etc.), or null if not to be described when making measurements
	 */
	public final void setShowZoomFactor(boolean showZoomFactor,boolean leftSide,double pixelSpacingInSourceImage,String typeOfPixelSpacing) {
		this.showZoomFactor=showZoomFactor;
		this.showZoomFactorLeftSide=leftSide;
		this.pixelSpacingInSourceImage=pixelSpacingInSourceImage;
		this.typeOfPixelSpacing=typeOfPixelSpacing;
	}

	/**
	 * <p>Set pixel spacing in source image.</p>
	 *
	 * <p>Used for displaying zoom factor and making measurements, therefore should be appropriate choice of Pixel Spacing or Imager Pixel Spacing (appropriately corrected for radiographic magnification factor, if any), etc.</p>
	 *
	 * @param	pixelSpacingInSourceImage	a single value that is the (square) row and column pixel spacing, or 0 if not known
	 */
	public final void setPixelSpacingInSourceImage(double pixelSpacingInSourceImage) {
		this.pixelSpacingInSourceImage=pixelSpacingInSourceImage;
	}

	/**
	 * <p>Set pixel spacing in source image.</p>
	 *
	 * <p>Used for displaying zoom factor and making measurements, therefore should be appropriate choice of Pixel Spacing or Imager Pixel Spacing (appropriately corrected for radiographic magnification factor, if any), etc.</p>
	 *
	 * @param	pixelSpacingInSourceImage	a single value that is the (square) row and column pixel spacing, or 0 if not known
	 * @param	typeOfPixelSpacing       	a String that describes the type of pixel spacing (e.g., detector plane, calibrated, accounting for geometric magnification, etc.), or null if not to be described when making measurements
	 */
	public final void setPixelSpacingInSourceImage(double pixelSpacingInSourceImage,String typeOfPixelSpacing) {
		this.pixelSpacingInSourceImage=pixelSpacingInSourceImage;
		this.typeOfPixelSpacing=typeOfPixelSpacing;
	}
	
	/***/
	private AffineTransform imageToWindowCoordinateTransform = null;
	/***/
	private AffineTransform windowToImageCoordinateTransform = null;
	
	/***/
	private int useVOIFunction = 0;		// 0 is linear, 1 is logistic
	
	/**
	 * <p>Set the VOI function to the (default) window center/width linear transformation.</p>
	 */
	public final void setVOIFunctionToLinear() {
		slf4jlogger.debug("setVOIFunctionToLinear()");
		useVOIFunction = 0;
	}

	/**
	 * <p>Set the VOI function to a non-linear transformation using a logistic (sigmoid) curve with window center and width as parameters.</p>
	 */
	public final void setVOIFunctionToLogistic() {
		slf4jlogger.debug("setVOIFunctionToLogistic()");
		useVOIFunction = 1;
	}
	
	/***/
	private boolean useWindowLinearExactCalculationInsteadOfDICOMStandardMethod = false;
	
	/**
	 * <p>Set the VOI linear function to use the exact window center/width linear transformation when applying to rescaled pixels.</p>
	 */
	public final void setWindowLinearCalculationToExact() {
		slf4jlogger.debug("setWindowLinearCalculationToExact()");
		useWindowLinearExactCalculationInsteadOfDICOMStandardMethod = true;
	}

	/**
	 * <p>Set the VOI linear function to use the DICOM offset window center/width linear transformation when applying to rescaled pixels.</p>
	 *
	 * <p>The DICOM offset subtracts 0.5 from the window center and subtracts 1.0 from the window width before applying to rescaled pixels.</p>
	 */
	public final void setWindowLinearCalculationToDicom() {
		slf4jlogger.debug("setWindowLinearCalculationToDicom()");
		useWindowLinearExactCalculationInsteadOfDICOMStandardMethod = false;
	}
	
	private double windowingAccelerationValue = 1;

	/**
	 * <p>Set the windowing acceleration value to use.</p>
	 */
	public final void setWindowingAccelerationValue(double value) {
		slf4jlogger.debug("setWindowingAccelerationValue(): to {}",value);
		windowingAccelerationValue = value;
	}


	private boolean showOverlays = true;
	private boolean applyShutter = true;
	private boolean showSuperimposedImages = true;

	/**
	 * <p>Set whether or not to show graphics such as overlays.</p>
	 */
	public final void setShowOverlays(boolean showOverlays) {
		slf4jlogger.debug("setShowOverlays(): {}",showOverlays);
		this.showOverlays = showOverlays;
	}

	/**
	 * <p>Set whether or not to apply shutter.</p>
	 */
	public final void setApplyShutter(boolean applyShutter) {
		slf4jlogger.debug("setApplyShutter(): {}",applyShutter);
		this.applyShutter = applyShutter;
	}

	/**
	 * <p>Set whether or not to show superimposed images.</p>
	 */
	public final void setShowSuperimposedImages(boolean showSuperimposedImages) {
		slf4jlogger.debug("setShowSuperimposedImages(): {}",showSuperimposedImages);
		this.showSuperimposedImages = showSuperimposedImages;
	}


	// Event stuff ...

	/***/
	EventContext typeOfPanelEventContext;
	
	/***/
	int lastx;
	/***/
	int lasty;
	
	int currentx;	// these are used to cache current mouse position in case we need to update status bar values when frame changed but mouse location not moved (since RWVM may be different per frame) (000840)
	int currenty;

	/***/
	int lastmiddley;
	
	/***/
	double windowingMultiplier = 1;
	/***/
	double panningMultiplier = 1;
	
	protected class LeftMouseMode {
		public static final int WINDOWING = 1;
		public static final int PANNING = 2;
		protected int mode;
		
		LeftMouseMode() {
			mode = WINDOWING;
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		public void setWindowing() {
			mode = WINDOWING;
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		public void setPanning() {
			mode = PANNING;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		public boolean isWindowing() { return mode == WINDOWING; }
		public boolean isPanning() { return mode == PANNING; }
	}
	
	protected LeftMouseMode leftMouseMode = new LeftMouseMode();
	
	/**
	 * if -1, then use statistical value first time then user adjusted values subsequently
	 * if greater than or equal to 0, then use selected VOI transform (if there is one for that frame)
	 */
	int currentVOITransformInUse;

	/**
	 * @param	e
	 */
	public void keyPressed(KeyEvent e) {
//System.err.println("Key pressed event"+e);
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
//System.err.println("Shift pressed");
			//windowingMultiplier=50;
			//windowingMultiplier=0.001;
			windowingMultiplier=windowingAccelerationValue;
			panningMultiplier=5;
		}
		else if (e.getKeyCode() == KeyEvent.VK_ALT) {
			leftMouseMode.setPanning();
		}
	}

	/**
	 * @param	e
	 */
	public void keyReleased(KeyEvent e) {
//System.err.println("Key released event"+e);
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
//System.err.println("Shift released");
			windowingMultiplier=1;
			panningMultiplier=1;
		}
		else if (e.getKeyCode() == KeyEvent.VK_ALT) {
			leftMouseMode.setWindowing();
		}
		else if (e.getKeyCode() == KeyEvent.VK_R) {			// reset display to original displayed area selection, and reset windowing
			displayReset();
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_S) {			// reset display to original displayed area selection, and reset windowing
			dirtySource();
			establishStatisticalWindow();
			repaint();
		}

		else if (e.getKeyCode() == KeyEvent.VK_1) {			// reset display to original displayed area selection but with 1:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(1);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_2) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(2);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_3) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(4);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_4) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(8);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_5) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(16);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_6) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(32);
			repaint();
		}
		else if (e.getKeyCode() == KeyEvent.VK_7) {			// reset display to original displayed area selection but with 2:1 display:image pixel magnification ratio
			displaySelectedMagnificationRatio(64);
			repaint();
		}
	}

	/**
	 * @param	e
	 */
	public void keyTyped(KeyEvent e) {
//System.err.println("Key typed event"+e);
	}

	/**
	 * @param	e
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		int delta = e.getWheelRotation();
//System.err.println("Mouse wheel delta = "+delta);
//System.err.println("Mouse wheel currentSrcImageIndex = "+currentSrcImageIndex);
		int newSrcImageIndex = currentSrcImageIndex + delta;
//System.err.println("Mouse wheel newSrcImageIndex = "+newSrcImageIndex);
		if (newSrcImageIndex >= sImg.getNumberOfBufferedImages()) newSrcImageIndex=sImg.getNumberOfBufferedImages()-1;
		if (newSrcImageIndex < 0) newSrcImageIndex=0;
//System.err.println("Mouse wheel newSrcImageIndex clipped = "+newSrcImageIndex);
		// don't send an event unless it is actually necessary ...
		if (newSrcImageIndex != currentSrcImageIndex) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(typeOfPanelEventContext,newSrcImageIndex));
		}
	}

	/**
	 * @param	e
	 */
	public void mouseClicked(MouseEvent e) {}

	/**
	 * @param	e
	 */
	public void mouseEntered(MouseEvent e) {
		requestFocus();		// In order to allow us to receive KeyEvents
	}

	/**
	 * @param	e
	 */
	public void mouseExited(MouseEvent e) {}

	/**
	 * @param	e
	 */
	public void mouseDragged(MouseEvent e) {
//System.err.println("SingleImagePanel.mouseDragged(): "+e);
		if (SwingUtilities.isLeftMouseButton(e)) {
//System.err.println("Left dragged "+e.getX()+" "+e.getY());
			int newx=e.getX();
			int newy=e.getY();
			int deltax=newx-lastx;
			int deltay=newy-lasty;

			if (deltax != 0 || deltay != 0) {
				lastx=newx;
				lasty=newy;
				if (leftMouseMode.isWindowing()) {
					double newWindowWidth=windowWidth+deltax*windowingMultiplier;
					if (windowingMultiplier >= 1 && newWindowWidth < 1.0) newWindowWidth=1.0;
					if (newWindowWidth < 0.0) newWindowWidth=0.0;
					double newWindowCenter=windowCenter+deltay*windowingMultiplier;
					ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new WindowCenterAndWidthChangeEvent(typeOfPanelEventContext,newWindowCenter,newWindowWidth));
				}
				else if (leftMouseMode.isPanning()) {
					if (useDisplayedAreaSelection != null) {
						int panX = (int)(-deltax*panningMultiplier);
						int panY = (int)(-deltay*panningMultiplier);
//System.err.println("Translate displayed area selection by ("+panX+","+panY+")");
						useDisplayedAreaSelection.translate(panX,panY);
						dirtyPanned();
						repaint();
					}
				}
			}
		}
		else if (SwingUtilities.isMiddleMouseButton(e)) {
//System.err.println("Middle dragged "+e.getX()+" "+e.getY());
			//selectNewSrcImage(e.getY()-lastmiddley);
			int delta = e.getY()-lastmiddley;
//System.err.println("Middle dragged delta = "+delta);
//System.err.println("Middle dragged currentSrcImageIndex = "+currentSrcImageIndex);
			int newSrcImageIndex = currentSrcImageIndex + delta;
//System.err.println("Middle dragged newSrcImageIndex = "+newSrcImageIndex);
			if (newSrcImageIndex >= sImg.getNumberOfBufferedImages()) newSrcImageIndex=sImg.getNumberOfBufferedImages()-1;
			if (newSrcImageIndex < 0) newSrcImageIndex=0;
//System.err.println("Middle dragged newSrcImageIndex clipped = "+newSrcImageIndex);
			// don't send an event unless it is actually necessary ...
			if (newSrcImageIndex != currentSrcImageIndex)
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(typeOfPanelEventContext,newSrcImageIndex));
			lastmiddley=e.getY();		// helps a lot when clipped at top or bottom of range
		}
		else if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right dragged "+e.getX()+" "+e.getY());
		}
	}


//	private void selectNewSrcImage(int delta) {
//		int newSrcImageIndex = currentSrcImageIndex + delta;
//		if (newSrcImageIndex >= sImg.getNumberOfBufferedImages()) newSrcImageIndex=sImg.getNumberOfBufferedImages()-1;
//		if (newSrcImageIndex < 0) newSrcImageIndex=0;
//		if (newSrcImageIndex != currentSrcImageIndex) {
//			currentSrcImageIndex=newSrcImageIndex;
//			cachedPreWindowedImage=null;		// forces application of new values on repaint 
//			repaint();
//		}
//	}

	protected int getSourceImageHeight() {
		return sImg.getHeight();
	}
	
	protected int getSourceImageWidth() {
		return sImg.getWidth();
	}
	
	/**
	 *
	 * Get location on source image from window relative location.
	 *
	 * @param	xw	x coordinate in AWT window as returned by MouseEvent.getX()
	 * @param	yw	y coordinate in AWT window as returned by MouseEvent.getY()
	 * @return		source image-relative coordinates with sub-pixel resolution clamped to image size but including BLHC of BLHC pixel (per DICOM PS 3.3 Figure C.10.5-1) 
	 */
	protected Point2D getImageCoordinateFromWindowCoordinate(double xw,double yw) {
		double xi = 0;
		double yi = 0;
		if (windowToImageCoordinateTransform != null) {
			Point2D pointd = new Point2D.Double(xw,yw);
			pointd = windowToImageCoordinateTransform.transform(pointd,pointd);	// overwrites self rather than allocating a new point
			xi = pointd.getX();
			yi = pointd.getY();
//System.err.println("SingeImagePanel.getImageCoordinateFromWindowCoordinate(): window ("+xw+","+yw+") -> ("+xi+","+yi+")");
			if (xi < 0) {
				xi=0;
			}
			else {
				int width = getSourceImageWidth();
				if (xi > width) {
					xi=width;
				}
			}
			if (yi < 0) {
				yi=0;
			}
			else {
				int height = getSourceImageHeight();
				if (yi > height) {
					yi=height;
				}
			}
//System.err.println("SingeImagePanel.getImageCoordinateFromWindowCoordinate(): clamped to image size ("+xi+","+yi+")");
		}
		return new Point2D.Double(xi,yi);
	}

	/**
	 * This value is outside mouseMoved() only so that it doesn't need to be constantly reallocated - it is not used by any other method
	 */
	protected double[] currentLocationIn3DSpace = new double[3];

	/**
	 * @param	e
	 */
	public void mouseMoved(MouseEvent e) {
//System.err.println(e.getX()+" "+e.getY());
		currentx = e.getX();	// keep track of these in case needed by OurFrameSelectionChangeListener.changed() (000840)
		currenty = e.getY();
		updateStatusBarValues(currentx,currenty);
	}
	
	/**
	 * @param	x
	 * @param	y
	 */
	public void updateStatusBarValues(int x,int y) {	// called by mouseMoved() or OurFrameSelectionChangeListener.changed() since mapping may be different for different frames (000840)
//System.err.println(x+" "+y);
		{
			Point2D point = getImageCoordinateFromWindowCoordinate(x,y);
			double subPixelX = point.getX();
			double subPixelY = point.getY();		
//System.err.println("X: "+subPixelX+" ("+x+") Y: "+subPixelX+" ("+y+")");
			int useSrcImageIndex = currentSrcImageSortOrder == null ? currentSrcImageIndex : currentSrcImageSortOrder[currentSrcImageIndex];

			StringBuffer sbuf = new StringBuffer();
			sbuf.append("(");
			sbuf.append(FloatFormatter.toString(subPixelX,Locale.US));
			sbuf.append(",");
			sbuf.append(FloatFormatter.toString(subPixelY,Locale.US));
			if (imageGeometry != null) {
//System.err.println("Have imageGeometry");
				imageGeometry.lookupImageCoordinate(currentLocationIn3DSpace,subPixelX,subPixelY,useSrcImageIndex);
				{
					// check round trip
					GeometryOfSlice sliceGeometry = imageGeometry.getGeometryOfSlice(useSrcImageIndex);
					if (sliceGeometry != null) {
						double[] roundtrip = sliceGeometry.lookupImageCoordinate(currentLocationIn3DSpace);
//System.err.println("Round trip imageGeometry.getGeometryOfSlice().lookupImageCoordinate(): ("+subPixelX+","+subPixelY+") -> ("+roundtrip[0]+","+roundtrip[1]+") for 3D ("+currentLocationIn3DSpace[0]+","+currentLocationIn3DSpace[1]+","+currentLocationIn3DSpace[2]+")");
						if (Double.isNaN(roundtrip[0]) || Double.isInfinite(roundtrip[0]) || Math.abs(roundtrip[0] - subPixelX) > 0.01
						 || Double.isNaN(roundtrip[1]) || Double.isInfinite(roundtrip[1]) || Math.abs(roundtrip[1] - subPixelY) > 0.01) {
							slf4jlogger.info("Round trip imageGeometry.getGeometryOfSlice().lookupImageCoordinate(): failed ("+subPixelX+","+subPixelY+") != ("+roundtrip[0]+","+roundtrip[1]+") for 3D ("+currentLocationIn3DSpace[0]+","+currentLocationIn3DSpace[1]+","+currentLocationIn3DSpace[2]+")");
						}
					}
				}
				{
					sbuf.append(": ");
					sbuf.append(FloatFormatter.toString(currentLocationIn3DSpace[0],Locale.US));
					sbuf.append(",");
					sbuf.append(FloatFormatter.toString(currentLocationIn3DSpace[1],Locale.US));
					sbuf.append(",");
					sbuf.append(FloatFormatter.toString(currentLocationIn3DSpace[2],Locale.US));
				}
				
				for (SuperimposedImage.AppliedToUnderlyingImage si : activeSuperimposedImagesAppliedToUnderlyingImage) {
					si.notificationOfCurrentLocationIn3DSpace(currentLocationIn3DSpace);
				}
			}
			sbuf.append(")");
		
			BufferedImage src = sImg.getBufferedImage(useSrcImageIndex);
			int wholePixelXFromZero = (int)subPixelX;	// just truncate (not round) ... 0 becomes 0, 0.5 becomes 0, 1.0 is next pixel and becomes 1, only extreme BLHC of BLHC pixel goes out of bounds
			int wholePixelYFromZero = (int)subPixelY;		
			if (wholePixelXFromZero >= 0 && wholePixelXFromZero < src.getWidth() && wholePixelYFromZero >= 0 && wholePixelYFromZero < src.getHeight()) {	// check avoids occasional ArrayIndexOutOfBoundsException exception
				double storedPixelValue;
				if (src.getRaster().getDataBuffer() instanceof DataBufferFloat) {
					float[] storedPixelValues  = src.getSampleModel().getPixel(wholePixelXFromZero,wholePixelYFromZero,(float[])null,src.getRaster().getDataBuffer());
					storedPixelValue=storedPixelValues[0];
				}
				else if (src.getRaster().getDataBuffer() instanceof DataBufferDouble) {
					double[] storedPixelValues  = src.getSampleModel().getPixel(wholePixelXFromZero,wholePixelYFromZero,(double[])null,src.getRaster().getDataBuffer());
					storedPixelValue=storedPixelValues[0];
				}
				else {
					int[] storedPixelValues  = src.getSampleModel().getPixel(wholePixelXFromZero,wholePixelYFromZero,(int[])null,src.getRaster().getDataBuffer());
					int storedPixelValueInt=storedPixelValues[0];
//System.err.println("storedPixelValue as stored = 0x"+Integer.toHexString(storedPixelValueInt)+" "+storedPixelValueInt+" dec");
					if (signed && (storedPixelValueInt&signBit) != 0) {
						storedPixelValueInt|=signMask;	// sign extend
//System.err.println("storedPixelValue extended  = 0x"+Integer.toHexString(storedPixelValueInt)+" "+storedPixelValueInt+" dec");
					}
					storedPixelValue=storedPixelValueInt;
				}
				if (realWorldValueTransform != null) {
//System.err.println("mouseMoved(): Have realWorldValueTransform");
					sbuf.append(" = ");
					sbuf.append(realWorldValueTransform.toString(useSrcImageIndex,storedPixelValue));
					sbuf.append(" [");
					sbuf.append(FloatFormatter.toString(storedPixelValue,Locale.US));	// this will not append spurious trailing "0." as default toString() method would
					sbuf.append("]");
				}

				if (suvTransform != null) {
					sbuf.append(" ");
					sbuf.append(suvTransform.toString(useSrcImageIndex,storedPixelValue));
				}
			}
			
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent(sbuf.toString()));
		}
	}

	/**
	 * @param	e
	 */
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
//System.err.println("Left pressed "+e.getX()+" "+e.getY());
			lastx=e.getX();
			lasty=e.getY();
		}
		else if (SwingUtilities.isMiddleMouseButton(e)) {
//System.err.println("Middle pressed "+e.getX()+" "+e.getY());
			lastmiddley=e.getY();
		}
		else if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right pressed "+e.getX()+" "+e.getY());
		}
	}

	/**
	 * @param	e
	 */
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
//System.err.println("Left released "+e.getX()+" "+e.getY());
			// on button release (but not during drag) propagate changed window values to self (a nop) and other registered SingleImagePanels ...
			if (leftMouseMode.isWindowing()) {
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new WindowCenterAndWidthChangeEvent(typeOfPanelEventContext,windowCenter,windowWidth));
			}
		}
		else if (SwingUtilities.isMiddleMouseButton(e)) {
//System.err.println("Middle released "+e.getX()+" "+e.getY());
		}
		else if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right released "+e.getX()+" "+e.getY());
		}
	}

	// implement WindowCenterAndWidthChangeListener to respond to events from self or other SingleImagePanel or elsewhere ...
	
	private OurWindowCenterAndWidthChangeListener ourWindowCenterAndWidthChangeListener;

	class OurWindowCenterAndWidthChangeListener extends SelfRegisteringListener {
	
		public OurWindowCenterAndWidthChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.WindowCenterAndWidthChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurWindowCenterAndWidthChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			WindowCenterAndWidthChangeEvent wcwe = (WindowCenterAndWidthChangeEvent)e;
			double newWindowCenter=wcwe.getWindowCenter();
			double newWindowWidth=wcwe.getWindowWidth();
			if (windowCenter != newWindowCenter || windowWidth != newWindowWidth) {
//System.err.println("SingleImagePanel.OurWindowCenterAndWidthChangeListener.changed(): new values");
				windowCenter=newWindowCenter;
				windowWidth=newWindowWidth;
				cachedPreWindowedImage=null;		// forces application of new values on repaint 
				currentVOITransformInUse=-1;		// override pre-specified VOI tranform with user selection
				repaint();
			}
			else {
//System.err.println("SingleImagePanel.OurWindowCenterAndWidthChangeListener.changed(): same values");
			}
		}
	}
	
	// implement FrameSelectionChangeListener to respond to events from self or elsewhere ...
	
	private OurFrameSelectionChangeListener ourFrameSelectionChangeListener;

	class OurFrameSelectionChangeListener extends SelfRegisteringListener {
	
		public OurFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurFrameSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
			int newCurrentSrcImageIndex = fse.getIndex();
			if (currentSrcImageIndex != newCurrentSrcImageIndex) {
//System.err.println("SingleImagePanel.OurFrameSelectionChangeListener.changed(): new value = "+currentSrcImageIndex);
				currentSrcImageIndex=newCurrentSrcImageIndex;
				dirtySource(); 
				repaint();
				updateStatusBarValues(currentx,currenty);	// since mapping may be different for different frames (000840)
			}
			else {
//System.err.println("SingleImagePanel.OurFrameSelectionChangeListener.changed(): same value");
			}
		}
	}
	
	// implement FrameSortOrderChangeListener to respond to events from self or other SingleImagePanel or elsewhere ...
	
	private OurFrameSortOrderChangeListener ourFrameSortOrderChangeListener;

	class OurFrameSortOrderChangeListener extends SelfRegisteringListener {
	
		public OurFrameSortOrderChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSortOrderChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurFrameSortOrderChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent)e;
			int[] newSrcImageSortOrder = fso.getSortOrder();
			int newCurrentSrcImageIndex = fso.getIndex();
			if (currentSrcImageIndex != newCurrentSrcImageIndex
			 || currentSrcImageSortOrder != newSrcImageSortOrder
			 || !Arrays.equals(currentSrcImageSortOrder,newSrcImageSortOrder)) {
//System.err.println("SingleImagePanel.OurFrameSortOrderChangeListener.changed(): new values");
				currentSrcImageIndex=newCurrentSrcImageIndex;
				currentSrcImageSortOrder=newSrcImageSortOrder;		// change even if null in event (request to go back to implicit order)
				dirtySource(); 
				repaint();
			}
			else {
//System.err.println("SingleImagePanel.OurFrameSortOrderChangeListener.changed(): same values");
			}
		}
	}
	
	// implement VOIFunctionChangeListener to respond to events from elsewhere ...
	
	private OurVOIFunctionChangeListener ourVOIFunctionChangeListener;

	class OurVOIFunctionChangeListener extends SelfRegisteringListener {
	
		public OurVOIFunctionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.VOIFunctionChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurVOIFunctionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			VOIFunctionChangeEvent vfce = (VOIFunctionChangeEvent)e;
			if (vfce.isLinearFunction()) {
				setVOIFunctionToLinear();
			}
			else if (vfce.isLogisticFunction()) {
				setVOIFunctionToLogistic();
			}
			dirtyWindowing();
			repaint();				// force repaint
		}
	}
	
	// implement WindowLinearCalculationChangeListener to respond to events from elsewhere ...
	
	private OurWindowLinearCalculationChangeListener ourWindowLinearCalculationChangeListener;

	class OurWindowLinearCalculationChangeListener extends SelfRegisteringListener {
	
		public OurWindowLinearCalculationChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.WindowLinearCalculationChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurWindowLinearCalculationChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			WindowLinearCalculationChangeEvent vlce = (WindowLinearCalculationChangeEvent)e;
			if (vlce.isExactCalculation()) {
				setWindowLinearCalculationToExact();
			}
			else if (vlce.isDicomCalculation()) {
				setWindowLinearCalculationToDicom();
			}
			dirtyWindowing();
			repaint();				// force repaint
		}
	}
	
	// implement WindowingAccelerationValueChangeListener to respond to events from elsewhere ...
	
	private OurWindowingAccelerationValueChangeListener ourWindowingAccelerationValueChangeListener;

	class OurWindowingAccelerationValueChangeListener extends SelfRegisteringListener {
	
		public OurWindowingAccelerationValueChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.WindowingAccelerationValueChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurWindowingAccelerationValueChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			WindowingAccelerationValueChangeEvent wavce = (WindowingAccelerationValueChangeEvent)e;
			setWindowingAccelerationValue(wavce.getValue());
			dirtyWindowing();
			repaint();				// force repaint
		}
	}
	
	// implement GraphicDisplayChangeListener to respond to events from elsewhere ...
	
	private OurGraphicDisplayChangeListener ourGraphicDisplayChangeListener;

	class OurGraphicDisplayChangeListener extends SelfRegisteringListener {
	
		public OurGraphicDisplayChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.GraphicDisplayChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurGraphicDisplayChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			GraphicDisplayChangeEvent gdce = (GraphicDisplayChangeEvent)e;
			setShowOverlays(gdce.showOverlays());
			dirtyWindowing();
			repaint();				// force repaint
		}
	}
	
	// implement ApplyShutterChangeListener to respond to events from elsewhere ...
	
	private OurApplyShutterChangeListener ourApplyShutterChangeListener;

	class OurApplyShutterChangeListener extends SelfRegisteringListener {
	
		public OurApplyShutterChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.ApplyShutterChangeEvent",eventContext);
//System.err.println("SingleImagePanel.OurApplyShutterChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			ApplyShutterChangeEvent asce = (ApplyShutterChangeEvent)e;
			setApplyShutter(asce.applyShutter());
			dirtyWindowing();
			repaint();				// force repaint
		}
	}
	
	// called by paintComponent() first time or after change create image with window values applied ...
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	identityCenter
	 * @param	identityWidth
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	hasPad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	numberOfEntries
	 * @param	bitsPerEntry
	 * @param	grayTable
	 * @param	entryMin
	 * @param	entryMax
	 * @param	topOfEntryRange
	 */
	public static final BufferedImage applyVOILUT(BufferedImage src,double center,double width,double identityCenter,double identityWidth,
			boolean signed,boolean inverted,double useSlope,double useIntercept,
			boolean hasPad,int pad,int padRangeLimit,
			int numberOfEntries,int firstValueMapped,int bitsPerEntry,short[] grayTable,int entryMin,int entryMax,int topOfEntryRange) {
		return WindowCenterAndWidth.applyVOILUT(src,center,width,identityCenter,identityWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,numberOfEntries,firstValueMapped,bitsPerEntry,grayTable,entryMin,entryMax,topOfEntryRange);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	hasPad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLogistic(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean hasPad,int pad,int padRangeLimit) {
		return WindowCenterAndWidth.applyWindowCenterAndWidthLogistic(src,center,width,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit);
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	hasPad
	 * @param	pad
	 * @param	padRangeLimit
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean hasPad,int pad,int padRangeLimit) {
		return WindowCenterAndWidth.applyWindowCenterAndWidthLinear(src,center,width,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit);
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	hasPad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	useExactCalculationInsteadOfDICOMStandardMethod
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinear(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean hasPad,int pad,int padRangeLimit,boolean useExactCalculationInsteadOfDICOMStandardMethod) {
		return WindowCenterAndWidth.applyWindowCenterAndWidthLinear(src,center,width,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,useExactCalculationInsteadOfDICOMStandardMethod);
	}

	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 * @param	signed
	 * @param	inverted
	 * @param	useSlope
	 * @param	useIntercept
	 * @param	hasPad
	 * @param	pad
	 * @param	padRangeLimit
	 * @param	largestGray
	 * @param	bitsPerEntry
	 * @param	numberOfEntries
	 * @param	redTable
	 * @param	greenTable
	 * @param	blueTable
	 */
	public static final BufferedImage applyWindowCenterAndWidthWithPaletteColor(BufferedImage src,double center,double width,
			boolean signed,boolean inverted,double useSlope,double useIntercept,boolean hasPad,int pad,int padRangeLimit,
			int largestGray,int bitsPerEntry,int numberOfEntries,
			short[] redTable,short[] greenTable,short[] blueTable) {
		slf4jlogger.debug("applyWindowCenterAndWidthWithPaletteColor():");
		return WindowCenterAndWidth.applyWindowCenterAndWidthWithPaletteColor(src,center,width,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,
			largestGray,bitsPerEntry,numberOfEntries,redTable,greenTable,blueTable);
	}
	
	/**
	 * @param	src
	 * @param	center
	 * @param	width
	 */
	public static final BufferedImage applyWindowCenterAndWidthLinearToColorImage(BufferedImage src,double center,double width) {
		return WindowCenterAndWidth.applyWindowCenterAndWidthLinearToColorImage(src,center,width);
	}
	
	// Common constructor support ...

	protected void establishStatisticalWindow() {
		slf4jlogger.debug("establishStatisticalWindow(): Deriving window statistically");
		//if (iMean != 0.0 && iSD != 0.0) {
//System.err.println("Using mean and SD");
		//	windowWidth=iSD*2.0;
		//	windowCenter=iMean;
		//}
		//else {
			slf4jlogger.debug("useSlope {}",useSlope);
			slf4jlogger.debug("useIntercept {}",useIntercept);
			slf4jlogger.debug("imgMin {}",imgMin);
			slf4jlogger.debug("imgMax {}",imgMax);
			double ourMin = imgMin*useSlope+useIntercept;
			double ourMax = imgMax*useSlope+useIntercept;
			slf4jlogger.debug("ourMin {}",ourMin);
			slf4jlogger.debug("ourMax {}",ourMax);
			//windowWidth=(ourMax-ourMin)/2.0;
			windowWidth=(ourMax-ourMin);
			windowCenter=(ourMax+ourMin)/2.0;
			currentVOITransformInUse=-1;		// flag not to mess with values when scrolling through frames
			slf4jlogger.debug("establishStatisticalWindow(): Initially using statistically derived center {} and width {}",windowCenter,windowWidth);
		//}
	}

	protected void establishInitialWindowOrVOILUT() {
		// choose the initial window center and width or VOI LUT ...
				
		useVOILUTNotFunction=false;
		windowWidth=0;
		windowCenter=0;
		voiLUTIdentityWindowCenter=0;
		voiLUTIdentityWindowWidth=0;
		voiLUTNumberOfEntries=0;
		voiLUTFirstValueMapped=0;
		voiLUTBitsPerEntry=0;
		voiLUTData=null;
		voiLUTEntryMin=0;
		voiLUTEntryMax=0;
		voiLUTTopOfEntryRange=0;
		
		slf4jlogger.debug("establishInitialWindowOrVOILUT(): Looking at voiTransform {}",voiTransform);
		if (voiTransform != null) {
			final int nTransforms = voiTransform.getNumberOfTransforms(currentSrcImageIndex);
			
			// first look for actual LUT, and prefer LUT over window values
			
			currentVOITransformInUse=0;
			while (currentVOITransformInUse < nTransforms) {
				if (voiTransform.isLUTTransform(currentSrcImageIndex,currentVOITransformInUse)) {
					slf4jlogger.debug("doCommonConstructorStuff(): found possible LUT {}",currentVOITransformInUse);
					 voiLUTNumberOfEntries=voiTransform.getNumberOfEntries (currentSrcImageIndex,currentVOITransformInUse);
					voiLUTFirstValueMapped=voiTransform.getFirstValueMapped(currentSrcImageIndex,currentVOITransformInUse);
					    voiLUTBitsPerEntry=voiTransform.getBitsPerEntry    (currentSrcImageIndex,currentVOITransformInUse);
					            voiLUTData=voiTransform.getLUTData         (currentSrcImageIndex,currentVOITransformInUse);
					        voiLUTEntryMin=voiTransform.getEntryMinimum    (currentSrcImageIndex,currentVOITransformInUse);
					        voiLUTEntryMax=voiTransform.getEntryMaximum    (currentSrcImageIndex,currentVOITransformInUse);
					 voiLUTTopOfEntryRange=voiTransform.getTopOfEntryRange (currentSrcImageIndex,currentVOITransformInUse);
					if (voiLUTData != null && voiLUTData.length == voiLUTNumberOfEntries) {
						useVOILUTNotFunction=true;	// only if LUT is "good"
						slf4jlogger.debug("establishInitialWindowOrVOILUT(): using good LUT {}",currentVOITransformInUse);
						// initialize "pseudo-window" to scale input values used to index LUT to identity transformation (i.e., apply LUT exactly as supplied)
						// note that the choice of identity values is arbitrary, but is chosen this way so that the numerical values "make sense" to the user
						// must be consistent with identity values specified in applyVOILUT() invocation
						voiLUTIdentityWindowWidth  = voiLUTNumberOfEntries;
						voiLUTIdentityWindowCenter = voiLUTFirstValueMapped + voiLUTNumberOfEntries/2;
						windowWidth  = voiLUTIdentityWindowWidth;
						windowCenter = voiLUTIdentityWindowCenter;
					}
					break;
				}
				++currentVOITransformInUse;
			}

			if (!useVOILUTNotFunction) {		// no LUT, so search transforms again for window values
				currentVOITransformInUse=0;
				while (currentVOITransformInUse < nTransforms) {
					if (voiTransform.isWindowTransform(currentSrcImageIndex,currentVOITransformInUse)) {
						useVOILUTNotFunction=false;
						windowWidth=voiTransform.getWidth(currentSrcImageIndex,currentVOITransformInUse);
						windowCenter=voiTransform.getCenter(currentSrcImageIndex,currentVOITransformInUse);
						slf4jlogger.debug("establishInitialWindowOrVOILUT(): Initially using preselected center {} and width {}",windowCenter,windowWidth);
						break;
					}
					++currentVOITransformInUse;
				}
			}
		}
		if (!useVOILUTNotFunction && windowWidth <= 0) {			// if no LUT, use supplied window only if there was one, and if its width was not zero or negative (center may legitimately be zero)
			establishStatisticalWindow();
		}
	}	

	/***/
	private boolean useConvertToMostFavorableImageType;	// used in paintComponent()

	/**
	 * @param	sImg
	 * @param	typeOfPanelEventContext
	 * @param	sortOrder
	 * @param	preDefinedShapes
	 * @param	preDefinedText
	 * @param	imageGeometry
	 */
	private void doCommonConstructorStuff(SourceImage sImg,
			EventContext typeOfPanelEventContext,
			int[] sortOrder,
			Vector preDefinedShapes,Vector preDefinedText,
			GeometryOfVolume imageGeometry) {
		slf4jlogger.debug("doCommonConstructorStuff():");
		this.sImg = sImg;
		boolean convertNonGrayscale = false;
		if (sImg != null && sImg.getNumberOfBufferedImages() > 0) {
			slf4jlogger.debug("doCommonConstructorStuff(): sImg != null");
			BufferedImage img=sImg.getBufferedImage(0);
		//	SampleModel sampleModel = img.getSampleModel();
			if (img != null && img.getRaster().getNumBands() > 1) {
		//		if (sampleModel instanceof ComponentSampleModel && ((ComponentSampleModel)sampleModel).getPixelStride() != 1
		//		 || sampleModel instanceof PixelInterleavedSampleModel) {
					convertNonGrayscale = true;
		//		}
			}
		}
		slf4jlogger.debug("doCommonConstructorStuff(): convertNonGrayscale = {}",convertNonGrayscale);
		try {
			useConvertToMostFavorableImageType =
			//	   (System.getProperty("mrj.version") != null && Double.parseDouble(System.getProperty("mrj.version")) < 4)	|| // because slow otherwise
				convertNonGrayscale == true;
			//useConvertToMostFavorableImageType = true;
			//useConvertToMostFavorableImageType = false;
		}
		catch (NumberFormatException e) {
		}
		slf4jlogger.debug("doCommonConstructorStuff(): useConvertToMostFavorableImageType = {}",useConvertToMostFavorableImageType);
		
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		currentSrcImageIndex=0;
		
		currentSrcImageSortOrder=sortOrder;	// may be null ... that is OK ... paintComponent() handles null as implicit frame order

		dirtySource();

		this.typeOfPanelEventContext=typeOfPanelEventContext;
		ourWindowCenterAndWidthChangeListener = new OurWindowCenterAndWidthChangeListener(typeOfPanelEventContext);
		ourFrameSelectionChangeListener = new OurFrameSelectionChangeListener(typeOfPanelEventContext);
		ourFrameSortOrderChangeListener = new OurFrameSortOrderChangeListener(typeOfPanelEventContext);
		ourVOIFunctionChangeListener = new OurVOIFunctionChangeListener(typeOfPanelEventContext);
		ourWindowLinearCalculationChangeListener = new OurWindowLinearCalculationChangeListener(typeOfPanelEventContext);
		ourWindowingAccelerationValueChangeListener = new OurWindowingAccelerationValueChangeListener(typeOfPanelEventContext);
		ourGraphicDisplayChangeListener = new OurGraphicDisplayChangeListener(typeOfPanelEventContext);
		ourApplyShutterChangeListener = new OurApplyShutterChangeListener(typeOfPanelEventContext);
	
		this.largestGray=sImg.getPaletteColorLargestGray();
		this.firstvalueMapped=sImg.getPaletteColorFirstValueMapped();
		this.numberOfEntries=sImg.getPaletteColorNumberOfEntries();
		this.bitsPerEntry=sImg.getPaletteColorBitsPerEntry();
		this.redTable=sImg.getPaletteColorRedTable();
		this.greenTable=sImg.getPaletteColorGreenTable();
		this.blueTable=sImg.getPaletteColorBlueTable();

		this.signed=sImg.isSigned();
		this.inverted=sImg.isInverted();
		this.ybr=sImg.isYBR();
		
		this.hasPad=sImg.isPadded();
		this.pad=sImg.getPadValue();
		this.padRangeLimit=sImg.getPadRangeLimit();
		
		if (sImg != null && sImg.getNumberOfBufferedImages() > 0) {
			BufferedImage img=sImg.getBufferedImage(0);
			imgMin=sImg.getMinimum();					// defer until have fetched BufferedImage, else may be invalid if in memoery mapped file
			imgMax=sImg.getMaximum();
			signBit=0;
			signMask=0;
			if (signed) {
				slf4jlogger.debug("signed={}",signed);
				// the source image will already have been sign extended to the data type size
				// so we don't need to worry about other than exactly 8 and 16 bits
				if (img.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE) {
					signBit=0x0080;
					signMask=0xffffff80;
				}
				else {	// assume short or ushort
					signBit=0x8000;
					signMask=0xffff8000;
				}
			}
			slf4jlogger.debug("signMask=0x{}",Integer.toHexString(signMask));
			slf4jlogger.debug("signBit=0x{}",Integer.toHexString(signBit));
		}
		
		this.suvTransform=sImg.getSUVTransform();
		this.realWorldValueTransform=sImg.getRealWorldValueTransform();
		this.modalityTransform=sImg.getModalityTransform();
		this.voiTransform=sImg.getVOITransform();
		this.displayShutter=sImg.getDisplayShutter();
		this.overlay=sImg.getOverlay();
		
		// slope & intercept get set on 1st paintComponent() anyway, but
		// we need to get them now to statistically derive the default
		// window center and width ...
		
		if (modalityTransform != null) {
			    useSlope = modalityTransform.getRescaleSlope    (currentSrcImageIndex);
			useIntercept = modalityTransform.getRescaleIntercept(currentSrcImageIndex);
			slf4jlogger.debug("Initially using preselected rescale slope {} and intercept {}",useSlope,useIntercept);
		}
		else {
			useSlope=1.0;
			useIntercept=0.0;
			slf4jlogger.debug("Initially using default rescale slope {} and intercept {}",useSlope,useIntercept);
		}
		
		establishInitialWindowOrVOILUT();
		
		slf4jlogger.debug("doCommonConstructorStuff(): setting preDefinedShapes");
		this.preDefinedShapes=preDefinedShapes;
		this.preDefinedText=preDefinedText;
		
		this.imageGeometry=imageGeometry;
	}
	
	// Actual constructors ...

	/**
	 * @param	sImg
	 * @param	typeOfPanelEventContext
	 * @param	sortOrder
	 * @param	preDefinedShapes
	 * @param	preDefinedText
	 * @param	imageGeometry
	 */
	public SingleImagePanel(SourceImage sImg,
			EventContext typeOfPanelEventContext,
			int[] sortOrder,
			Vector preDefinedShapes,Vector preDefinedText,
			GeometryOfVolume imageGeometry) {
		doCommonConstructorStuff(sImg,
			typeOfPanelEventContext,
			sortOrder,
			preDefinedShapes,preDefinedText,
			imageGeometry);
	}

	/**
	 * @param	sImg
	 * @param	typeOfPanelEventContext
	 * @param	imageGeometry
	 */
	public SingleImagePanel(SourceImage sImg,
			EventContext typeOfPanelEventContext,
			GeometryOfVolume imageGeometry) {
		doCommonConstructorStuff(sImg,
			typeOfPanelEventContext,
			null,
			null,null,
			imageGeometry);
	}

	/**
	 * @param	sImg
	 * @param	typeOfPanelEventContext
	 */
	public SingleImagePanel(SourceImage sImg,
			EventContext typeOfPanelEventContext) {
		doCommonConstructorStuff(sImg,
			typeOfPanelEventContext,
			null,
			null,null,
			null);
	}

	/**
	 * @param	sImg
	 */
	public SingleImagePanel(SourceImage sImg) {
		doCommonConstructorStuff(sImg,
			null,
			null,
			null,null,
			null);
	}

	public void deconstruct() {
		slf4jlogger.debug("deconstruct()");
		// avoid "listener leak"
		if (ourWindowCenterAndWidthChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourWindowCenterAndWidthChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourWindowCenterAndWidthChangeListener);
			ourWindowCenterAndWidthChangeListener=null;
		}
		if (ourFrameSelectionChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourFrameSelectionChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSelectionChangeListener);
			ourFrameSelectionChangeListener=null;
		}
		if (ourFrameSortOrderChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourFrameSortOrderChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourFrameSortOrderChangeListener);
			ourFrameSortOrderChangeListener=null;
		}
		if (ourVOIFunctionChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourVOIFunctionChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourVOIFunctionChangeListener);
			ourVOIFunctionChangeListener=null;
		}
		if (ourWindowLinearCalculationChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourWindowLinearCalculationChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourWindowLinearCalculationChangeListener);
			ourWindowLinearCalculationChangeListener=null;
		}
		if (ourWindowingAccelerationValueChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourWindowingAccelerationValueChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourWindowingAccelerationValueChangeListener);
			ourWindowingAccelerationValueChangeListener=null;
		}
		if (ourGraphicDisplayChangeListener != null) {
//System.err.println("SingleImagePanel.deconstruct(): removing ourGraphicDisplayChangeListener");
			ApplicationEventDispatcher.getApplicationEventDispatcher().removeListener(ourGraphicDisplayChangeListener);
			ourGraphicDisplayChangeListener=null;
		}
	}
	
	/*
	 * @param container
	 */
	 public static void deconstructAllSingleImagePanelsInContainer(Container container) {
		Component[] components = container.getComponents();				
		slf4jlogger.debug("deconstructAllSingleImagePanelsInContainer(): deconstructing old SingleImagePanels components.length={}",components.length);
		for (int i=0; i<components.length; ++i) {
			Component component = components[i];
			if (component instanceof SingleImagePanel) {
				((SingleImagePanel)component).deconstruct();
			}
		}
	}

	protected void finalize() throws Throwable {
		slf4jlogger.debug("finalize()");
		deconstruct();		// just in case wasn't already called, and garbage collection occurs
		super.finalize();
	}

	// stuff to handle drawing ...

	// override methods for JComponent ...
        
	private BufferedImageUtilities resampler = null;
	
//	public void paint(Graphics g) {
//System.err.println("SingleImagePanel.paint(): start");
//		try { throw new Exception(); } catch (Exception e) { slf4jlogger.error("",e); }
//		super.paint(g);
//System.err.println("SingleImagePanel.paint(): end");
//	}
	
//	public void update(Graphics g) {
//System.err.println("SingleImagePanel.update(): start");
//		try { throw new Exception(); } catch (Exception e) { slf4jlogger.error("",e); }
//		super.update(g);
//System.err.println("SingleImagePanel.update(): end");
//	}

//	public void repaint() {
//System.err.println("SingleImagePanel.repaint(): start");
//		try { throw new Exception(); } catch (Exception e) { slf4jlogger.error("",e); }
//		super.repaint();
//System.err.println("SingleImagePanel.repaint(): end");
//	}

//	public void repaint(Rectangle r) {
//System.err.println("SingleImagePanel.repaint(): start");
//		try { throw new Exception(); } catch (Exception e) { slf4jlogger.error("",e); }
//		super.repaint(r);
//System.err.println("SingleImagePanel.repaint(): end");
//	}
	
//	public void repaint(long tm, int x, int y, int width, int height) {
//System.err.println("SingleImagePanel.repaint(): start");
//		try { throw new Exception(); } catch (Exception e) { slf4jlogger.error("",e); }
//		super.repaint(tm,x,y,width,height);
//System.err.println("SingleImagePanel.repaint(): end");
//	}
	

	/**
	 * @param	g
	 */
	public void paintComponent(Graphics g) {
		slf4jlogger.debug("paintComponent(): start");
		long startTime = System.currentTimeMillis();
		Cursor was = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		int useSrcImageIndex = currentSrcImageSortOrder == null ? currentSrcImageIndex : currentSrcImageSortOrder[currentSrcImageIndex];
		slf4jlogger.debug("paintComponent() useSrcImageIndex: {}",useSrcImageIndex);
		BufferedImage useSrcImage = sImg.getBufferedImage(useSrcImageIndex);
		slf4jlogger.debug("paintComponent(): useSrcImage BufferedImage: {}",useSrcImage);
		slf4jlogger.debug("paintComponent(): useSrcImage is BufferedImage as follows:");
		if (slf4jlogger.isDebugEnabled()) BufferedImageUtilities.describeImage(useSrcImage,System.err);

		if (ybr) {
			// do this BEFORE any interpolation, else get artifacts at edges
			slf4jlogger.debug("paintComponent(): attempting to convert YBR to RGB for display");
			BufferedImage convertedImage = BufferedImageUtilities.convertYBRToRGB(useSrcImage);
			if (convertedImage != null) {
				slf4jlogger.debug("paintComponent(): YBR to RGB conversion succeeded");
				useSrcImage = convertedImage;
				dirtySource();
			}
			else {
				slf4jlogger.debug("paintComponent(): YBR to RGB conversion failed - just use what we have");
			}
		}

		Rectangle windowSize = this.getBounds();
		slf4jlogger.debug("paintComponent():windowSize = {}",windowSize);

		// the following is not sensitive to selection area change ... in future may need to make this explicit on resize event ... :(
		if (cachedWindowSize != null && (windowSize.width != cachedWindowSize.width && windowSize.height != cachedWindowSize.height)) {		// do not care about position
			slf4jlogger.debug("paintComponent(): window size changed, so flushing cache and resetting selection to original");
			dirtySource();
			useDisplayedAreaSelection=null;
		}
		cachedWindowSize = windowSize;

		if (useDisplayedAreaSelection == null) {
			if (originalDisplayedAreaSelection == null) {
				originalDisplayedAreaSelection = new DisplayedAreaSelection(useSrcImage.getWidth(),useSrcImage.getHeight());		// default is entire image
			}
			slf4jlogger.debug("setDisplayedAreaSelection(): originalDisplayedAreaSelection = {}",originalDisplayedAreaSelection);
			useDisplayedAreaSelection = originalDisplayedAreaSelection.shapeSelectionToMatchAvailableWindow(windowSize);
			slf4jlogger.debug("paintComponent(): useDisplayedAreaSelection (after shapping to match available window size) = {}",useDisplayedAreaSelection);
		}

		double useScaleFactor = ((double)windowSize.width)/useDisplayedAreaSelection.getSelectionWidth();
		slf4jlogger.debug("paintComponent(): useScaleFactor = {}",useScaleFactor);

		imageToWindowCoordinateTransform = AffineTransform.getTranslateInstance(-useDisplayedAreaSelection.getXOffset()*useScaleFactor,-useDisplayedAreaSelection.getYOffset()*useScaleFactor);
		imageToWindowCoordinateTransform.concatenate(AffineTransform.getScaleInstance(useScaleFactor,useScaleFactor));
		if (preTransformImageRelativeCoordinates != null) {
			imageToWindowCoordinateTransform.concatenate(preTransformImageRelativeCoordinates);
		}
		slf4jlogger.debug("paintComponent(): imageToWindowCoordinateTransform = {}",imageToWindowCoordinateTransform);
		try {
			windowToImageCoordinateTransform = imageToWindowCoordinateTransform.createInverse();
		}
		catch (NoninvertibleTransformException e) {
			slf4jlogger.error("",e);
			windowToImageCoordinateTransform=null;
		}
		slf4jlogger.debug("paintComponent(): windowToImageCoordinateTransform = {}",windowToImageCoordinateTransform);
		
		if (cachedPreWindowedImage == null) {
			slf4jlogger.debug("paintComponent(): cachedPreWindowedImage is null");
			if (cachedResizedSelectedRegionImage == null) {
				slf4jlogger.debug("paintComponent(): resizing prior to windowing");
				if (cachedResizedImage == null) {
					if (resampler == null) {
						resampler = new BufferedImageUtilities();
					}
					long resampleTime = System.currentTimeMillis();
					int backgroundValuePriorToWindowing = sImg.getBackgroundValue();

					int srcWidth = useSrcImage.getWidth();
					int srcHeight = useSrcImage.getHeight();
					cachedResizedImage=resampler.resample(useSrcImage,
						srcWidth,
						srcHeight,
						0,
						0,
						(int)Math.round(srcWidth*useScaleFactor),
						(int)Math.round(srcHeight*useScaleFactor),
						signed,
						backgroundValuePriorToWindowing);
					slf4jlogger.debug("paintComponent(): resizing with resampler - done in {} ms",(System.currentTimeMillis()-resampleTime));
					slf4jlogger.debug("paintComponent(): resampled BufferedImage: {}",cachedResizedImage);
					slf4jlogger.debug("paintComponent(): resampled BufferedImage is as follows:");
					if (slf4jlogger.isDebugEnabled()) BufferedImageUtilities.describeImage(cachedResizedImage,System.err);
				}
				long getSubimageTime = System.currentTimeMillis();
				int scaledSelectionX = (int)Math.round(useDisplayedAreaSelection.getXOffset()*useScaleFactor);
				int scaledSelectionY = (int)Math.round(useDisplayedAreaSelection.getYOffset()*useScaleFactor);
				int scaledSelectionWidth = (int)Math.round(useDisplayedAreaSelection.getSelectionWidth()*useScaleFactor);
				int scaledSelectionHeight = (int)Math.round(useDisplayedAreaSelection.getSelectionHeight()*useScaleFactor);
				slf4jlogger.debug("paintComponent(): before clipping to image area - scaledSelectionX = {}",scaledSelectionX);
				slf4jlogger.debug("paintComponent(): before clipping to image area - scaledSelectionY = {}",scaledSelectionY);
				slf4jlogger.debug("paintComponent(): before clipping to image area - scaledSelectionWidth = {}",scaledSelectionWidth);
				slf4jlogger.debug("paintComponent(): before clipping to image area - scaledSelectionHeight = {}",scaledSelectionHeight);

				if (scaledSelectionX < 0) {
					offsetDrawingOfResizedSelectedRegionImageX = - scaledSelectionX;
					//scaledSelectionWidth -= offsetDrawingOfResizedSelectedRegionImageX;
					scaledSelectionX = 0;
				}
				else {
					offsetDrawingOfResizedSelectedRegionImageX = 0;
				}
					
				if (scaledSelectionY < 0) {
					offsetDrawingOfResizedSelectedRegionImageY = - scaledSelectionY;
					//scaledSelectionHeight -= offsetDrawingOfResizedSelectedRegionImageY;
					scaledSelectionY = 0;
				}
				else {
					offsetDrawingOfResizedSelectedRegionImageY = 0;
				}

				int cachedResizedImageWidth = cachedResizedImage.getWidth();
				slf4jlogger.debug("paintComponent(): cachedResizedImageWidth = {}",cachedResizedImageWidth);
				if (scaledSelectionX + scaledSelectionWidth > cachedResizedImageWidth) {
					scaledSelectionWidth = cachedResizedImageWidth - scaledSelectionX;
				}
				else if (scaledSelectionX + scaledSelectionWidth < cachedResizedImageWidth) {	// use all available image to the right, i.e., if panning beyond original selection
					scaledSelectionWidth = cachedResizedImageWidth - scaledSelectionX;
				}
				if (scaledSelectionWidth < 0) {
					scaledSelectionWidth = 0;
				}

				int cachedResizedImageHeight = cachedResizedImage.getHeight();
				slf4jlogger.debug("paintComponent(): cachedResizedImageHeight = {}",cachedResizedImageHeight);
				if (scaledSelectionY + scaledSelectionHeight > cachedResizedImageHeight) {
					scaledSelectionHeight = cachedResizedImageHeight - scaledSelectionY;
				}
				else if (scaledSelectionY + scaledSelectionHeight < cachedResizedImageHeight) {	// use all available image to the bottom, i.e., if panning beyond original selection
					scaledSelectionHeight = cachedResizedImageHeight - scaledSelectionY;
				}
				if (scaledSelectionHeight < 0) {
					scaledSelectionHeight = 0;
				}

				slf4jlogger.debug("paintComponent(): after clipping to image area - scaledSelectionX = {}",scaledSelectionX);
				slf4jlogger.debug("paintComponent(): after clipping to image area - scaledSelectionY = {}",scaledSelectionY);
				slf4jlogger.debug("paintComponent(): after clipping to image area - scaledSelectionWidth = {}",scaledSelectionWidth);
				slf4jlogger.debug("paintComponent(): after clipping to image area - scaledSelectionHeight = {}",scaledSelectionHeight);
				slf4jlogger.debug("paintComponent(): offsetDrawingOfResizedSelectedRegionImageX = {}",offsetDrawingOfResizedSelectedRegionImageX);
				slf4jlogger.debug("paintComponent(): offsetDrawingOfResizedSelectedRegionImageY = {}",offsetDrawingOfResizedSelectedRegionImageY);

				cachedResizedSelectedRegionImage = 
					scaledSelectionHeight > 0 && scaledSelectionWidth > 0
					? cachedResizedImage.getSubimage(scaledSelectionX,scaledSelectionY,scaledSelectionWidth,scaledSelectionHeight)
					: null;
				slf4jlogger.debug("paintComponent(): selecting subimage from resized image - done in {} ms",(System.currentTimeMillis()-getSubimageTime));
			}
			useSrcImage=cachedResizedSelectedRegionImage;
			
			if (useSrcImage == null) {		// e.g., nothing left to draw since panned offscreen
				cachedPreWindowedImage = null;
			}
			else {
				ColorModel colorModel = useSrcImage.getColorModel();
				int numComponents = colorModel.getNumComponents();
				slf4jlogger.debug("paintComponent(): Before windowing (if needed), getNumComponents() == {}",numComponents);
				
				if (numComponents == 1) {
					
					// First, find pre-selected VOI for this frame, if any
					
					slf4jlogger.debug("currentVOITransformInUse {}",currentVOITransformInUse);
					if (currentVOITransformInUse != -1
						&& !useVOILUTNotFunction
						&& voiTransform != null
						&& voiTransform.getNumberOfTransforms(useSrcImageIndex) > currentVOITransformInUse) {
						windowWidth=voiTransform.getWidth(useSrcImageIndex,currentVOITransformInUse);
						windowCenter=voiTransform.getCenter(useSrcImageIndex,currentVOITransformInUse);
						slf4jlogger.debug("For new frame {} using preselected center {} and width {}",useSrcImageIndex,windowCenter,windowWidth);
					}
					else {
						//currentVOITransformInUse=-1;
						// Just leave it alone (but don't disable use of selection for other frames)
						slf4jlogger.debug("For new frame {} using user selected center {} and width {}",useSrcImageIndex,windowCenter,windowWidth);
					}
					
					// Second, find rescale attributes for this frame
					
					if (modalityTransform != null) {
						useSlope     = modalityTransform.getRescaleSlope    (useSrcImageIndex);
						useIntercept = modalityTransform.getRescaleIntercept(useSrcImageIndex);
						slf4jlogger.debug("For new frame {} using preselected rescale slope {} and intercept {}",useSrcImageIndex,useSlope,useIntercept);
					}
					else {
						useSlope=1.0;
						useIntercept=0.0;
						slf4jlogger.debug("For new frame {} using default rescale slope {} and intercept {}",useSrcImageIndex,useSlope,useIntercept);
					}
					
					// Finally, actually build the destination image
					slf4jlogger.debug("paintComponent() useVOILUTNotFunction = {}",useVOILUTNotFunction);
					slf4jlogger.debug("paintComponent() numberOfEntries = {}",numberOfEntries);
					slf4jlogger.debug("paintComponent() redTable = {}",redTable);
					slf4jlogger.debug("paintComponent() useVOIFunction = {}",useVOIFunction);
					long applyVOITime = System.currentTimeMillis();
					
					if (useVOILUTNotFunction) {
						cachedPreWindowedImage = applyVOILUT(useSrcImage,windowCenter,windowWidth,voiLUTIdentityWindowCenter,voiLUTIdentityWindowWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,
															 voiLUTNumberOfEntries,voiLUTFirstValueMapped,voiLUTBitsPerEntry,voiLUTData,voiLUTEntryMin,voiLUTEntryMax,voiLUTTopOfEntryRange);
					}
					else if (numberOfEntries != 0 && redTable != null) {
						cachedPreWindowedImage = applyWindowCenterAndWidthWithPaletteColor(useSrcImage,windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,
																						   largestGray,bitsPerEntry,numberOfEntries,redTable,greenTable,blueTable);
					}
					else if (useVOIFunction == 1) {
						cachedPreWindowedImage = applyWindowCenterAndWidthLogistic(useSrcImage,windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit);
					}
					else {
						cachedPreWindowedImage = applyWindowCenterAndWidthLinear(useSrcImage,windowCenter,windowWidth,signed,inverted,useSlope,useIntercept,hasPad,pad,padRangeLimit,useWindowLinearExactCalculationInsteadOfDICOMStandardMethod);
					}
					slf4jlogger.debug("paintComponent(): VOI/window applied in {} ms",(System.currentTimeMillis()-applyVOITime));
				}
				else  if (numComponents == 3) {
					slf4jlogger.debug("paintComponent(): is 3 component");
					slf4jlogger.debug("currentVOITransformInUse {}",currentVOITransformInUse);
					if (currentVOITransformInUse != -1 && voiTransform.getNumberOfTransforms(useSrcImageIndex) > currentVOITransformInUse) {
						windowWidth=voiTransform.getWidth(useSrcImageIndex,currentVOITransformInUse);
						windowCenter=voiTransform.getCenter(useSrcImageIndex,currentVOITransformInUse);
						slf4jlogger.debug("For new frame {} using preselected center {} and width {}",useSrcImageIndex,windowCenter,windowWidth);
					}
					else {
						//currentVOITransformInUse=-1;
						// Just leave it alone (but don't disable use of selection for other frames)
						slf4jlogger.debug("For new frame {} using user selected center {} and width {}",useSrcImageIndex,windowCenter,windowWidth);
					}
					
					// No rescaling for color images
					
					// Finally, actually build the destination image
					long applyVOITime = System.currentTimeMillis();
					// use only linear voiTransform ... and no rescaling, no sign, no inversion (for now), no padding
					{
						cachedPreWindowedImage = applyWindowCenterAndWidthLinearToColorImage(useSrcImage,windowCenter,windowWidth);
					}
					slf4jlogger.debug("paintComponent(): VOI/window applied in {} ms",(System.currentTimeMillis()-applyVOITime));
				}
				else {
					slf4jlogger.debug("paintComponent(): Not windowing, getNumComponents() = {}, currentVOITransformInUse = {}, useVOILUTNotFunction = {}",numComponents,currentVOITransformInUse,useVOILUTNotFunction);
					slf4jlogger.debug("paintComponent(): Just using unwindowed useSrcImage as cachedPreWindowedImage={}",useSrcImage);
					cachedPreWindowedImage=useSrcImage;
				}
			}
			slf4jlogger.debug("paintComponent() mrj.version={}",System.getProperty("mrj.version"));
			if (cachedPreWindowedImage != null && useConvertToMostFavorableImageType) {
				long conversionTime = System.currentTimeMillis();
				cachedPreWindowedImage=BufferedImageUtilities.convertToMostFavorableImageType(cachedPreWindowedImage);
				slf4jlogger.debug("paintComponent(): converted to most favorable done in {} ms",(System.currentTimeMillis()-conversionTime));
			}
		}
		else {
			slf4jlogger.debug("paintComponent(): using cachedPreWindowedImage");
		}
				
		long drawImageTime = System.currentTimeMillis();
		Graphics2D g2d=(Graphics2D)g;
		if (cachedPreWindowedImage != null) {
		slf4jlogger.debug("paintComponent(): same size draw");
		slf4jlogger.debug("paintComponent(): applyShutter = {}",applyShutter);
			Shape holdClip = null;
			if (applyShutter && displayShutter != null) {
				holdClip = g2d.getClip();
				if (displayShutter.isCircularShutter()) {
					Point2D tlhc = imageToWindowCoordinateTransform.transform(displayShutter.getCircularShutterTLHC(),null);
					double tlhcX = tlhc.getX();
					double tlhcY = tlhc.getY();
					Point2D brhc = imageToWindowCoordinateTransform.transform(displayShutter.getCircularShutterBRHC(),null);
					double brhcX = brhc.getX();
					double brhcY = brhc.getY();
					g2d.clip(new Ellipse2D.Double(tlhcX,tlhcY,brhcX-tlhcX,brhcY-tlhcY));
				}
				if (displayShutter.isRectangularShutter()) {
					Point2D tlhc = imageToWindowCoordinateTransform.transform(displayShutter.getRectangularShutterTLHC(),null);
					double tlhcX = tlhc.getX();
					double tlhcY = tlhc.getY();
					Point2D brhc = imageToWindowCoordinateTransform.transform(displayShutter.getRectangularShutterBRHC(),null);
					double brhcX = brhc.getX();
					double brhcY = brhc.getY();
					g2d.clip(new Rectangle2D.Double(tlhcX,tlhcY,brhcX-tlhcX,brhcY-tlhcY));
				}
				if (displayShutter.isPolygonalShutter()) {
					Point2D[] points = displayShutter.getVerticesOfPolygonalShutterAsPoint2D();
					Polygon polygon = new Polygon();
					for (int i=0; i<points.length; ++i) {
						Point2D point = imageToWindowCoordinateTransform.transform(points[i],null);
						polygon.addPoint((int)(point.getX()),(int)(point.getY()));
					}
					g2d.clip(polygon);
				}
			}
			g2d.drawImage(cachedPreWindowedImage,offsetDrawingOfResizedSelectedRegionImageX,offsetDrawingOfResizedSelectedRegionImageY,this);
			if (holdClip != null) {
				g2d.setClip(holdClip);
			}
		}
		slf4jlogger.debug("paintComponent(): draw cachedPreWindowedImage into dst image done in {} ms",(System.currentTimeMillis()-drawImageTime));

		long drawAnnotationsTime = System.currentTimeMillis();
		Color interactiveColor = Color.red;
		Color persistentColor = Color.green;
		Color selectedColor = Color.red;
		Color localizerColor = Color.green;
		Color volumeLocalizationColor = Color.blue;
		Color preDefinedColor = Color.yellow;
		Color perFrameDrawingColor = Color.red;
		int lineWidth = 2;	// in display (not source image) pixels
		Font preDefinedFont = new Font("SansSerif",Font.BOLD,14);
		Font persistentFont = new Font("SansSerif",Font.BOLD,10);
		
		// draw any graphics that are displayed area relative ...
				
		if (demographicAndTechniqueAnnotations != null) {
			g2d.setColor(demographicAndTechniqueColor);
			g2d.setFont(demographicAndTechniqueFont);

			Iterator i = demographicAndTechniqueAnnotations.iterator(useSrcImageIndex);
			while (i.hasNext()) {
				TextAnnotationPositioned.drawPositionedString((TextAnnotationPositioned)i.next(),g2d,this,5,5);
			}

		}
		
		{
			g2d.setColor(orientationColor);
			g2d.setFont(orientationFont);
			// draw orientation
			if (orientationAnnotations != null) {
				String rowOrientation = orientationAnnotations.getRowOrientation(useSrcImageIndex);
				if (rowOrientation != null && rowOrientation.length() > 0) {
					TextAnnotationPositioned.drawVerticallyCenteredString(rowOrientation,showOrientationsLeftSide,g2d,this,5);
				}
				String columnOrientation = orientationAnnotations.getColumnOrientation(useSrcImageIndex);
				if (columnOrientation != null && columnOrientation.length() > 0) {
					TextAnnotationPositioned.drawHorizontallyCenteredString(columnOrientation,false/*isTop*/,g2d,this,5);
				}
			}
			
			if (showZoomFactor) {
				double scaleFactor = Math.abs(imageToWindowCoordinateTransform.getScaleX());		// -ve if flipped; should check same in both axes ? :(
				String sZoomFactor = "1:" + FloatFormatter.toString(scaleFactor,Locale.US);
				slf4jlogger.debug("paintComponent(): sZoomFactor = {}",sZoomFactor);
				TextAnnotationPositioned.drawVerticallyCenteredString(sZoomFactor,showZoomFactorLeftSide,g2d,this,2,5);
				String sDisplayPixelSize = "[" + FloatFormatter.toString(pixelSpacingInSourceImage/scaleFactor,Locale.US) + "mm]";
				TextAnnotationPositioned.drawVerticallyCenteredString(sDisplayPixelSize,showZoomFactorLeftSide,g2d,this,3,5);
			}
		}

		if (sideAndViewAnnotationString != null) {
			g2d.setColor(sideAndViewAnnotationColor);
			g2d.setFont(sideAndViewAnnotationFont);
			TextAnnotationPositioned.drawPositionedString(
				new TextAnnotationPositioned(sideAndViewAnnotationString,showSideAndViewAnnotationLeftSide,true/*fromTop*/,0/*row*/),
					g2d,this,sideAndViewAnnotationVerticalOffset,5
			);
		}

		// draw any text that are image relative, but using display relative font size
		// by transforming point BEFORE changing graphics context to image relative
		
		if (persistentDrawingText != null) {
			g2d.setColor(persistentColor);
			g2d.setFont(persistentFont);
			Iterator i = persistentDrawingText.iterator();
			while (i.hasNext()) {
				TextAnnotation text = (TextAnnotation)i.next();
				Point2D imageRelativeAnchorPoint = text.getAnchorPoint();
				Point2D windowRelativeAnchorPoint = imageToWindowCoordinateTransform.transform(imageRelativeAnchorPoint,null);
				String string = text.getString();
				int margin = 5;
				int stringWidth  = (int)(g2d.getFontMetrics().getStringBounds(string,g2d).getWidth());
				DrawingUtilities.drawShadowedString(string,
					showOrientationsLeftSide ? margin : getBounds().width - margin - stringWidth,
					(int)windowRelativeAnchorPoint.getY(),			// always left or right since sometimes doesn't fit (clipped on right) if drawn at anchor point:(
					g2d);
			}
		}

		// draw any graphics that are image relative, by applying a scaled AffineTransform to the graphics context ...

		slf4jlogger.debug("paintComponent(): draw any graphics");
		
		g2d.transform(imageToWindowCoordinateTransform);
		// want the stroke line width specified in display (not source image) pixels
		g2d.setStroke(new BasicStroke((float)(lineWidth/useScaleFactor)));

		activeSuperimposedImagesAppliedToUnderlyingImage.clear();
		if (showSuperimposedImages && superimposedImages != null) {
			for (SuperimposedImage superimposedImage : superimposedImages) {
				slf4jlogger.debug("paintComponent(): Drawing superimposed image {}",superimposedImage);
				SuperimposedImage.AppliedToUnderlyingImage superimposedImageAppliedToUnderlyingImage = superimposedImage.getAppliedToUnderlyingImage(imageGeometry,useSrcImageIndex);
				if (superimposedImageAppliedToUnderlyingImage != null) {
					slf4jlogger.debug("paintComponent(): Drawing superimposed image applied to underlying image {}",superimposedImageAppliedToUnderlyingImage);
					BufferedImage bufferedImage = superimposedImageAppliedToUnderlyingImage.getBufferedImage();
					if (bufferedImage != null) {
						activeSuperimposedImagesAppliedToUnderlyingImage.add(superimposedImageAppliedToUnderlyingImage);		// keep track of these to use for cursor hit on superimposed image
						
						double rowOrigin = superimposedImageAppliedToUnderlyingImage.getRowOrigin();
						double columnOrigin = superimposedImageAppliedToUnderlyingImage.getColumnOrigin();
						
						java.awt.image.RescaleOp applyColor = null;
						int bufferedImageType = bufferedImage.getType();
						if (bufferedImageType == BufferedImage.TYPE_INT_ARGB) {
							slf4jlogger.debug("paintComponent(): have ARGB superimposed image so can change color and use transparency");
							// BufferedImageOp ... use this for transparency with ARGB BufferedImage and RescaleOp to mess with alpha channel (see "http://docs.oracle.com/javase/tutorial/2d/images/drawimage.html")
							int[] cieLabScaled = superimposedImage.getIntegerScaledCIELabPCS();
							float redScale = 1f;
							float greenScale = 1f;
							float blueScale = 0f;
							if (cieLabScaled != null) {
								int[] srgb = ColorUtilities.getSRGBFromIntegerScaledCIELabPCS(cieLabScaled);
								redScale = srgb[0] / 255f;
								greenScale = srgb[1] / 255f;
								blueScale = srgb[2] / 255f;
							}
							float[] scales = { redScale, greenScale, blueScale, 1f /* color and alpha */ };
							float[] offsets = new float[4];
							applyColor = new java.awt.image.RescaleOp(scales,offsets,null/*RenderingHints*/);
						}
						else {
							slf4jlogger.info("SingleImagePanel.paintComponent(): not ARGB superimposed image so cannot change color and use transparency");
						}
						g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,0.5f));	// set transparency
						g2d.drawImage(
							bufferedImage,
							applyColor,
							(int)columnOrigin,	// aaargh ! ... really need to indirect the original doubles through the affine transform in case upsampled for displayed, since usually are +0.5 sub-pixel resolution :(
							(int)rowOrigin
						);
					}
					//else {
					//	System.err.println("SingleImagePanel.paintComponent(): could not apply superimposed image to underlying image because no BufferedImage returned");
					//}
				}
				//else {
				//	System.err.println("SingleImagePanel.paintComponent(): could not apply superimposed image to underlying image because no SuperimposedImage.AppliedToUnderlyingImage returned");
				//}
			}
		}
		
		if (showOverlays && overlay != null && overlay.getNumberOfOverlays(useSrcImageIndex) > 0) {
			slf4jlogger.debug("paintComponent(): Drawing overlays for frame {}",useSrcImageIndex);
			for (int o=0; o<16; ++o) {
				BufferedImage overlayImage = overlay.getOverlayAsBinaryBufferedImage(useSrcImageIndex,o);
				if (overlayImage != null) {
					slf4jlogger.debug("paintComponent(): Drawing overlay number {}",o);
					int rowOrigin = overlay.getRowOrigin(useSrcImageIndex,o);
					int columnOrigin = overlay.getColumnOrigin(useSrcImageIndex,o);
					{
						byte[] red = { (byte)0, (byte)0 };
						byte[] green = { (byte)0, (byte)0 };
						byte[] blue = { (byte)0, (byte)0 };
						IndexColorModel colorModel = new IndexColorModel(
							1 /* bits */,
							2 /* size */,
							red,green,blue,
							0 /* the first index (black value) is to be transparent */);
						BufferedImage shadowImage = new BufferedImage(colorModel,overlayImage.getRaster(),false,null);
						g2d.drawImage(shadowImage,null,columnOrigin+1,rowOrigin+1);
					}
					g2d.drawImage(overlayImage,null,columnOrigin,rowOrigin);
				}
			}
		}
		
		if (interactiveDrawingShapes != null) {
			slf4jlogger.debug("paintComponent(): Draw interactive shapes:");
//LocalizerPosterFactory.dumpShapes(interactiveDrawingShapes);
			g2d.setColor(interactiveColor);
			Iterator i = interactiveDrawingShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}

		if (persistentDrawingShapes != null) {
			slf4jlogger.debug("paintComponent(): Draw persistent shapes:");
//LocalizerPosterFactory.dumpShapes(persistentDrawingShapes);
			g2d.setColor(persistentColor);
			Iterator i = persistentDrawingShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}

		if (selectedDrawingShapes != null) {
			slf4jlogger.debug("paintComponent(): Draw selected shapes:");
//LocalizerPosterFactory.dumpShapes(persistentDrawingShapes);
			g2d.setColor(selectedColor);
			Iterator i = selectedDrawingShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}

		if (volumeLocalizationShapes != null) {
			slf4jlogger.debug("paintComponent(): draw volume localization shapes");
//LocalizerPosterFactory.dumpShapes(volumeLocalizationShapes);
			g2d.setColor(volumeLocalizationColor);
			Iterator i = volumeLocalizationShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}
		
		if (localizerShapes != null) {			// do this after volumeLocalizationShapes, in case need to draw on top
		slf4jlogger.debug("paintComponent(): draw localizer shapes");
//LocalizerPosterFactory.dumpShapes(localizerShapes);
			g2d.setColor(localizerColor);
			Iterator i = localizerShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}
		
		if (preDefinedShapes != null) {
			slf4jlogger.debug("paintComponent(): draw pre-defined shapes");
//com.pixelmed.geometry.LocalizerPosterFactory.dumpShapes(preDefinedShapes);
			g2d.setColor(preDefinedColor);
			Iterator i = preDefinedShapes.iterator();
			while (i.hasNext()) {
				DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
			}
		}
		
		if (perFrameDrawingShapes != null && perFrameDrawingShapes.length > currentSrcImageIndex && perFrameDrawingShapes[currentSrcImageIndex] != null) {
			slf4jlogger.debug("paintComponent(): draw per-frame shapes");
			Vector<Shape> shapes = perFrameDrawingShapes[currentSrcImageIndex];
//com.pixelmed.geometry.LocalizerPosterFactory.dumpShapes(shapes);
			g2d.setColor(perFrameDrawingColor);
			Iterator i = shapes.iterator();
			while (i.hasNext()) {
				//DrawingUtilities.drawShadowedShape((Shape)i.next(),g2d);
				g2d.draw((Shape)i.next());
			}
		}

		if (preDefinedText != null) {
			g2d.setColor(preDefinedColor);
			g2d.setFont(preDefinedFont);
			Iterator i = preDefinedText.iterator();
			while (i.hasNext()) {
				TextAnnotation text = (TextAnnotation)i.next();
				DrawingUtilities.drawShadowedString(text.getString(),text.getAnchorPointXAsInt(),text.getAnchorPointYAsInt(),g2d);
			}
		}
		slf4jlogger.debug("paintComponent(): draw annotations done in {} ms",(System.currentTimeMillis()-drawAnnotationsTime));
		
		setCursor(was);
		slf4jlogger.debug("paintComponent() elapsed: {} ms",(System.currentTimeMillis()-startTime));
		slf4jlogger.debug("paintComponent(): end");
	}

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 1) {
				JFrame p = new JFrame();
				SourceImage sImg = new SourceImage(arg[0]);
				//AttributeList list = new AttributeList();
				//list.read(arg[0]);
				if (sImg.isImage()) {
					p.add(new SingleImagePanel(sImg));
					//p.add(new SingleImagePanel(new SourceImage(list)));
					p.setBackground(Color.BLACK);
					p.setSize(512,512);
					p.setVisible(true);
				}
				else {
					throw new DicomException("Is not an image or is not an image that can be displayed");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}






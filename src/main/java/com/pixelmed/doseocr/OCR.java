/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.anatproc.CTAnatomy;
import com.pixelmed.anatproc.CombinedAnatomicConcepts;
import com.pixelmed.anatproc.DisplayableConcept;

import com.pixelmed.dose.*;

import com.pixelmed.dicom.*;

import com.pixelmed.display.ConsumerFormatImageMaker;

import com.pixelmed.utils.FileUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for OCR of GE and Siemens modality dose report screen saves.</p>
 *
 * @author	dclunie
 * @author	giwarden
 */

public class OCR {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/doseocr/OCR.java,v 1.83 2017/01/24 10:50:43 dclunie Exp $";
	
	private static final Logger slf4jlogger = LoggerFactory.getLogger(OCR.class);
	
	public static String defaultFileNameOfKnownGlyphs = "OCR_Glyphs_DoseScreen.xml";
	
	private static int maximumNumberOfConnections = 4000;		// increased for Toshiba wide spacing :(
	
	private static int defaultGEHorizontalGapTolerance = 6;
	private static int defaultGEVerticalGapTolerance = 4;
	
	private static int defaultSiemensHorizontalGapTolerance = 6;
	private static int defaultSiemensVerticalGapTolerance = 2;		// giwarden - Siemens screens have less vertical spacing
	
	private static int defaultToshibaHorizontalGapTolerance = 13;	// Toshiba has greater spacing after decimal point in numbers (for which 8 is sufficient), and want to keep strings together despite ascenders late in line ("(Body):-"); consequence is loosing spacing in strings and between paramater and value
	private static int defaultToshibaVerticalGapTolerance = 6;
	
	class ConnectednessException extends Exception {
		public ConnectednessException() {
			super("Exceeded maximum number of connections ... probably not a validly thresholded image");
		}
	}
	
	private BufferedImage image;
	private int height;
	private int width;
	private BitSet thresholdedPixels;
	private BitSet processedPixels;
	private Map<Glyph,String> mapOfGlyphsToStrings;
	private Map<Location,Glyph> mapOfRecognizedLocationsToGlyphs;
	private Map<Location,Glyph> mapOfUnrecognizedLocationsToGlyphs;
	private boolean trainingMode;
	
	private String multiPageLines;
	
	private AttributeList commonAttributesList;		// stuff that is needed that is the same between multiple screen pages in one series
	
	public AttributeList getCommonAttributeList() { return commonAttributesList; }
	
	private static void addToListIfNotEmpty(AttributeTag tag,AttributeList sourceList,AttributeList destinationList) {
		Attribute a = sourceList.get(tag);
		if (a != null && (a.getVM() > 0 || (a instanceof SequenceAttribute && ((SequenceAttribute)a).getNumberOfItems() > 0))) {
			destinationList.put(a);	// should really check if different :(
		}
	}
	
	private void createOrUpdateCommonAttributeList(AttributeList sourceList) {
//System.err.println("OCR.createOrUpdateCommonAttributeList():");
		if (commonAttributesList == null) {
//System.err.println("OCR.createOrUpdateCommonAttributeList(): null");
			commonAttributesList = new DoseCompositeInstanceContext(sourceList).getAttributeList();		// context needed for making SRs, populating report headings, etc.
			//System.err.print("OCR.createOrUpdateCommonAttributeList(): after CIC:\n"+commonAttributesList);
		}
		else {
			DoseCompositeInstanceContext cic = new DoseCompositeInstanceContext(commonAttributesList);
			cic.updateFromSource(sourceList,true/*forSR*/);
			commonAttributesList.putAll(cic.getAttributeList());	// overwrite stuff in DoseCompositeInstanceContext, but leave stuff that is not
		}
		
		addToListIfNotEmpty(TagFromName.ImageType,sourceList,commonAttributesList);
		addToListIfNotEmpty(TagFromName.WindowWidth,sourceList,commonAttributesList);
		addToListIfNotEmpty(TagFromName.WindowCenter,sourceList,commonAttributesList);
		addToListIfNotEmpty(TagFromName.ExposureDoseSequence,sourceList,commonAttributesList);
		addToListIfNotEmpty(TagFromName.CommentsOnRadiationDose,sourceList,commonAttributesList);
		//System.err.print("OCR.createOrUpdateCommonAttributeList(): on exit:\n"+commonAttributesList);
	}
	
	private static final int getBitSetIndex(int x,int y,int width) { return y*width+x; }
	private static final int getXFromBitSetIndex(int bit,int width) { return bit%width; }
	private static final int getYFromBitSetIndex(int bit,int width) { return bit/width; }
	
	private final int getBitSetIndex(int x,int y) { return y*width+x; }
	private final int getXFromBitSetIndex(int bit) { return bit%width; }
	private final int getYFromBitSetIndex(int bit) { return bit/width; }
	
	private final boolean isPixelOn(int x,int y) { return thresholdedPixels.get(getBitSetIndex(x,y,width)); }
	
	private final boolean isProcessed(int x,int y) { return processedPixels.get(getBitSetIndex(x,y,width)); }
	
	private final void setProcessed(int x,int y) { processedPixels.set(getBitSetIndex(x,y,width)); }
	
	class Location implements Comparable {
		int x;
		int y;
		
		public int getX() { return x; }
		public int getY() { return y; }
		
		Location(int x,int y) {
			this.x = x;
			this.y = y;
		}
		
		// order is y then x
		public int compareTo(Object o) {
			slf4jlogger.trace("Location.compareTo(): comparing {} to {}",this,o);
			int result = -1;
			if (o instanceof Location) {
				Location ol = (Location)o;
				if (y == ol.getY()) {
					result = x - ol.getX();
				}
				else {
					result = y - ol.getY();
				}
			}
			slf4jlogger.trace("Location.compareTo(): result = {}",result);
			return result;
		}
		
		public boolean equals(Object o) {
			slf4jlogger.trace("Location.equals(): comparing {} to {}",this,o);
			return compareTo(o) == 0;
		}
		
		public int hashCode() {
			return x+y;	// sufficient to implement equals() contract
		}
		
		public String toString() {
			return "("+x+","+y+")";
		}
	}
	
	class Glyph {
		BitSet set;
		int width;
		int height;
		boolean wasKnown;
		
		public BitSet getSet() { return set; }
		public int getWidth() { return width; }
		public int getHeight() { return height; }
		public boolean getWasKnown() { return wasKnown; }
		
		public String getString() { return mapOfGlyphsToStrings.get(this); }
		
		Glyph(BitSet srcSet,int srcSetWidth,boolean wasKnown) throws IllegalArgumentException {
			slf4jlogger.trace("Glyph.Glyph(): srcSet = {}",srcSet);
			slf4jlogger.trace("Glyph.Glyph(): srcSetWidth = {}",srcSetWidth);
			if(srcSet.isEmpty()) {
				throw new IllegalArgumentException("Cannot create Glyph from empty BitSet");
			}
			int tlhcX = findLowestXSetInBitSet(srcSet,srcSetWidth);
			int tlhcY = findLowestYSetInBitSet(srcSet,srcSetWidth);
			int brhcX = findHighestXSetInBitSet(srcSet,srcSetWidth);
			int brhcY = findHighestYSetInBitSet(srcSet,srcSetWidth);
			slf4jlogger.trace("Glyph.Glyph(): srcSet TLHC ({},{}), BRHC = ({},{})",tlhcX,tlhcY,brhcX,brhcY);
			height=brhcY-tlhcY+1;
			width=brhcX-tlhcX+1;
			slf4jlogger.trace("Glyph.Glyph(): new width = {}, height = {}",width,height);
			set = new BitSet();
			int srcY=tlhcY;
			for (int dstY=0; dstY<height; ++dstY,++srcY) {
				int srcX=tlhcX;
				for (int dstX=0; dstX<width; ++dstX,++srcX) {
					if (srcSet.get(getBitSetIndex(srcX,srcY,srcSetWidth))) {
						set.set(getBitSetIndex(dstX,dstY,width));
					}
				}
			}
			this.wasKnown = wasKnown;
		}
		
		public boolean equals(Object o) {
			boolean result = false;
			if (o instanceof Glyph) {
				Glyph og = (Glyph)o;
				result = set.equals(og.getSet()) && width == og.getWidth() && height == og.getHeight();
				// do NOT compare string, since may not be set in one or the other
			}
			return result;
		}
		
		public int hashCode() {
			return set.hashCode();	// sufficient to implement equals() contract
		}
		
		public String toString() {
			slf4jlogger.trace("Set = {}",set);
			StringBuffer buf = new StringBuffer();
			for (int y=0; y < height; ++y ){
				for (int x=0; x < width; ++x) {
					if (set.get(getBitSetIndex(x,y,width))) {
						buf.append("# ");
					}
					else {
						buf.append(". ");
					}
				}
				buf.append("\n");
			}
			String string = getString();
			if (string != null) {
				buf.append("String: \""+string+"\"\n");
			}
			return buf.toString();
		}
		
		public String toXML() {
			StringBuffer buf = new StringBuffer();
			String string = getString();
			if (string != null && string.length() > 0) {
				buf.append("\t<glyph>\n");
				buf.append("\t\t<bits>\n");
				int length = set.length();
				if (length > 0) {
					for (int index=0; index < length; ++index) {
						if (set.get(index)) {
							buf.append("\t\t\t<bit>"+index+"</bit>\n");
						}
					}
				}
				buf.append("\t\t</bits>\n");
				buf.append("\t\t<width>"+width+"</width>\n");
				buf.append("\t\t<string>"+string.replaceAll("<","&lt;").replaceAll(">","&gt;").replaceFirst("^&$","&amp;")+"</string>\n");
				buf.append("\t</glyph>\n");
			}
			return buf.toString();
		}
		
		public String toSourceCode() {
			StringBuffer buf = new StringBuffer();
			String string = getString();
			if (string != null && string.length() > 0) {
				buf.append("\t{\n");
				buf.append("\t\tBitSet set = new BitSet();\n");
				int length = set.length();
				if (length > 0) {
					for (int index=0; index < length; ++index) {
						if (set.get(index)) {
							buf.append("\t\tset.set("+index+");\n");
						}
					}
				}
				buf.append("\t\tmapOfGlyphsToStrings.put(new Glyph(set,"+width+",true),\""+string+"\");\n");
				buf.append("\t}\n");
			}
			return buf.toString();
		}
	}
	
	/**
	 * <p>Find the lowest X set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the lowest X value, or -1 if no bit is set
	 */
	private static int findLowestXSetInBitSet(BitSet set,int width) {
		int found = Integer.MAX_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int x = getXFromBitSetIndex(index,width);
					++counted;
					if (x < found) {
						found = x;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	/**
	 * <p>Find the lowest Y set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the lowest Y value, or -1 if no bit is set
	 */
	private static int findLowestYSetInBitSet(BitSet set,int width) {
		int found = Integer.MAX_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int y = getYFromBitSetIndex(index,width);
					++counted;
					if (y < found) {
						found = y;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	
	/**
	 * <p>Find the highest X set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the highest X value, or -1 if no bit is set
	 */
	private static int findHighestXSetInBitSet(BitSet set,int width) {
		slf4jlogger.trace("findHighestXSetInBitSet(): width = {}, set ={}",width,set);
		int found = Integer.MIN_VALUE;
		int count = set.cardinality();
		slf4jlogger.trace("findHighestXSetInBitSet(): cardinality = {}",count);
		if (count > 0) {
			int length = set.length();
			slf4jlogger.trace("findHighestXSetInBitSet(): length = {}",length);
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int x = getXFromBitSetIndex(index,width);
					slf4jlogger.trace("findHighestXSetInBitSet(): testing x = {}",x);
					++counted;
					if (x > found) {
						slf4jlogger.trace("findHighestXSetInBitSet(): found x = {}",x);
						found = x;
					}
				}
			}
		}
		else {
			found = -1;
		}
		slf4jlogger.trace("findHighestXSetInBitSet(): returning x = {}",found);
		return found;
	}
	
	/**
	 * <p>Find the highest Y set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the highest Y value, or -1 if no bit is set
	 */
	private static int findHighestYSetInBitSet(BitSet set,int width) {
		int found = Integer.MIN_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int y = getYFromBitSetIndex(index,width);
					++counted;
					if (y > found) {
						found = y;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	private static BitSet threshold(BufferedImage image) {
		int bitsPerPixel = image.getColorModel().getPixelSize();
		slf4jlogger.trace("threshold(): image pixel size (bpp) = {}",bitsPerPixel);
		int thresholdValue = bitsPerPixel > 1 ? 127 : 0;	// just in case was lossy compressed in 8 bit case, but handle single bit image (e.g., from overlay)
		slf4jlogger.trace("threshold(): thresholdValue = {}",thresholdValue);
		int height = image.getHeight();
		int width = image.getWidth();
		Raster raster = image.getRaster();
		BitSet thresholdedPixels = new BitSet(height*width);
		int[] pixelValues = new int[1];
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				raster.getPixel(x,y,pixelValues);		// no need to assign retured value, since fills supplied array
				slf4jlogger.trace("({},{}) pixelValue = {}",x,y,pixelValues[0]);
				if (pixelValues[0] > thresholdValue) {
					slf4jlogger.trace("Setting ({},{}) for pixelValue = {}",x,y,pixelValues[0]);
					thresholdedPixels.set(getBitSetIndex(x,y,width));	// could optimize just with incremented index :(
				}
			}
		}
		return thresholdedPixels;
	}
	
	private int checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(int x,int y,BitSet set,int horizontalGapTolerance,int verticalGapTolerance,int numberOfConnections) throws ConnectednessException {
		if (numberOfConnections > maximumNumberOfConnections) {
			throw new ConnectednessException();
		}
		//slf4jlogger.trace("Check\t({},{})",x,y);
		//slf4jlogger.trace("\t({},{}) in bounds={}",x,y,(x > 0 && x < width && y > 0 && y < height));
		//slf4jlogger.trace("\t({},{}) isProcessed()={}",x,y,isProcessed(x,y));
		//slf4jlogger.trace("\t({},{}) isPixelOn()={}",x,y,isPixelOn(x,y));
		if (x > 0 && x < width && y > 0 && y < height && !isProcessed(x,y) && isPixelOn(x,y)) {
			slf4jlogger.trace("Doing\t({},{})",x,y);
			setProcessed(x,y);
			set.set(getBitSetIndex(x,y));
			++numberOfConnections;
			numberOfConnections = walkConnectionsRecordingThem(x,y,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);	// recurse
		}
		return numberOfConnections;
	}
	
	private int walkConnectionsRecordingThem(int x,int y,BitSet set,int horizontalGapTolerance,int verticalGapTolerance,int numberOfConnections) throws ConnectednessException {
		// walk each cardinal and oblique direction ...
		for (int yDelta=1; yDelta <= verticalGapTolerance; ++yDelta) {
			for (int xDelta=1; xDelta <= horizontalGapTolerance; ++xDelta) {
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
			}
		}
		return numberOfConnections;
	}
	
	private String processCandidate(int x,int y,int blockY,int horizontalGapTolerance,int verticalGapTolerance,boolean recordLocationWhenRecognized) throws IOException, ConnectednessException {
		String matched = null;
		if (!isProcessed(x,y) && isPixelOn(x,y)) {
			//slf4jlogger.trace("Candidate at ({},{})",x,y);
			//slf4jlogger.trace("\t({},{}) isProcessed()={}",x,y,isProcessed(x,y));
			//slf4jlogger.trace("\t({},{}) isPixelOn()={}",x,y,isPixelOn(x,y));
			Location start = new Location(x,y);
			BitSet set = new BitSet();	// size expands as we walk
			try {
				checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x,y,set,horizontalGapTolerance,verticalGapTolerance,0);	// repeat check on bounds and processed and is on redundant for this first pixel
			}
			catch (ConnectednessException e) {
				// don't want (huge) stacktrace in this case, re-throw it, since we do actually want to stop processing the file
				throw new ConnectednessException();
			}
			if (set.isEmpty()) {
				slf4jlogger.trace("\tduring connectedness search, get empty set back from checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse() for candidate at ({},{})",x,y);
			}
			else {
				// note that the location we need (TLHC) may have moved due to connecting to the left or upwards ...
				int lowestX = findLowestXSetInBitSet(set,width);
				if (lowestX < 0) {
					slf4jlogger.trace("\tduring connectedness search, lowestX for TLHC is out of bounds = {}, using 0",lowestX);
					lowestX = 0;
				}
				int lowestY = findLowestYSetInBitSet(set,width);
				if (lowestY < 0) {
					slf4jlogger.trace("\tduring connectedness search, lowestY for TLHC is out of bounds = {}, using 0",lowestY);
					lowestY = 0;
				}
				Location tlhc = new Location(lowestX,lowestY);
				if (!tlhc.equals(start)) {
					slf4jlogger.trace("\tduring connectedness search, TLHC moved from {} to {}",start,tlhc);
				}
				Glyph glyph = new Glyph(set,width,false);
				slf4jlogger.trace(glyph.toString());
				matched = mapOfGlyphsToStrings.get(glyph);
				if (matched != null) {
					slf4jlogger.trace("Recognized {}",matched);
					if (recordLocationWhenRecognized) {
						mapOfRecognizedLocationsToGlyphs.put(tlhc,glyph);
					}
				}
				else {
					if (trainingMode) {
						// don't have java.io.Console in JRE 5 :( so do it manually
						System.out.print(glyph+"Please enter string match: ");
						matched = new BufferedReader(new InputStreamReader(System.in)).readLine();
					}
					if (matched != null && matched.length() > 0) {
						mapOfGlyphsToStrings.put(glyph,matched);
						slf4jlogger.trace("Map {} = \n{}",matched,glyph);
						if (recordLocationWhenRecognized) {
							mapOfRecognizedLocationsToGlyphs.put(tlhc,glyph);
						}
					}
					else {
						matched = null; // empty string becomes null for return flag
						if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("Adding unrecognized glyph at location {}, width = {}, BRHC = ({},{})\n{}",tlhc,glyph.getWidth(),(tlhc.getX()+glyph.getWidth()-1),(tlhc.getY()+glyph.getHeight()-1),glyph);
						mapOfUnrecognizedLocationsToGlyphs.put(tlhc,glyph);
					}
				}
			}
		}
		return matched;
	}
	
	private boolean findConnectedCandidatesAnywhereInImage(int horizontalGapTolerance,int verticalGapTolerance) throws IOException, ConnectednessException {
		slf4jlogger.trace("findConnectedCandidatesAnywhereInImage(): horizontalGapTolerance={} verticalGapTolerance={}",horizontalGapTolerance,verticalGapTolerance);
		boolean foundAnything = false;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				if (processCandidate(x,y,y,horizontalGapTolerance,verticalGapTolerance,true/*recordLocationWhenRecognized*/) != null) {
					foundAnything = true;
				}
			}
		}
		return foundAnything;
	}
	
	private boolean findConnectedCandidatesWithinUnrecognizedGlyphs(int horizontalGapTolerance,int verticalGapTolerance) throws IOException, ConnectednessException {
		boolean foundAnything = false;
		Location[] locations = mapOfUnrecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			Glyph glyph = mapOfUnrecognizedLocationsToGlyphs.get(l);
			mapOfUnrecognizedLocationsToGlyphs.remove(l);	// since once we have started processing it a) we don't need it and b) we may add a new, smaller unrecognized glyph at same location
			String matched = "";
			int tlhcX = l.getX();
			int tlhcY = l.getY();
			int brhcX = tlhcX + glyph.getWidth() - 1;
			int brhcY = tlhcY + glyph.getHeight() - 1;
			int blockY = brhcY;
			slf4jlogger.trace("findConnectedCandidatesWithinUnrecognizedGlyphs(): scan within box from TLHC ({},{}) to BRHC ({},{})",tlhcX,tlhcY,brhcX,brhcY);
			// scan horizontally, then vertically, since want to treat new glyphs found as left to right unless two separate in same column
			for (int x = tlhcX; x <= brhcX; ++x) {
				for (int y = tlhcY; y <= brhcY; ++y) {
					// may theoretically stray outside boundary if connected, but should have been found when block (unrecognized glyph) detected last time
					String partialMatch = processCandidate(x,y,blockY,horizontalGapTolerance,verticalGapTolerance,false/*recordLocationWhenRecognized*/);
					if (partialMatch != null) {
						matched = matched + partialMatch;	// no spaces; treats everything in block as single "word"
					}
				}
			}
			if (matched.length() > 0) {
				foundAnything = true;
				mapOfGlyphsToStrings.put(glyph,matched);
				mapOfRecognizedLocationsToGlyphs.put(l,glyph);
			}
		}
		return foundAnything;
	}
	
	private void flagAsProcessedLocationsAlreadyRecognized() {
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			Glyph glyph = mapOfRecognizedLocationsToGlyphs.get(l);
			int tlhcX = l.getX();
			int tlhcY = l.getY();
			int brhcX = tlhcX + glyph.getWidth() - 1;
			int brhcY = tlhcY + glyph.getHeight() - 1;
			slf4jlogger.trace("flagAsProcessedLocationsAlreadyRecognized box from TLHC ({},{}) to BRHC ({},{})",tlhcX,tlhcY,brhcX,brhcY);
			for (int y = tlhcY; y <= brhcY; ++y) {
				for (int x = tlhcX; x <= brhcX; ++x) {
					setProcessed(x,y);
				}
			}
		}
	}
	
	private String dumpGlyphsAsXML(boolean onlyNew,boolean queryEachNew) throws IOException {
		if (mapOfGlyphsToStrings == null) {
			return "";
		}
		Iterator<Glyph>i = mapOfGlyphsToStrings.keySet().iterator();
		StringBuffer buf = new StringBuffer();
		buf.append("<glyphs>\n");
		while (i.hasNext()) {
			Glyph glyph = i.next();
			if (!onlyNew || !glyph.getWasKnown()) {
				boolean doIt = false;
				if (queryEachNew && !glyph.getWasKnown()) {
					// don't have java.io.Console in JRE 5 :( so do it manually
					System.out.print(glyph+"Record it in dictionary, Y or N [N]: ");
					String response = new BufferedReader(new InputStreamReader(System.in)).readLine();
					if (response != null && response.length() > 0 && response.trim().toUpperCase(java.util.Locale.US).equals("Y")) {
						doIt = true;
					}
				}
				else {
					doIt = true;
				}
				if (doIt) {
					slf4jlogger.debug("Recorded \"{}\"",glyph.getString());
					//buf.append(glyph.toSourceCode());
					buf.append(glyph.toXML());
				}
			}
		}
		buf.append("</glyphs>\n");
		return buf.toString();
	}
	
	private String dumpGlyphsAsStrings() {
		if (mapOfGlyphsToStrings == null) {
			return "";
		}
		String[] values = mapOfGlyphsToStrings.values().toArray(new String[0]);
		Arrays.sort(values);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<values.length; ++i) {
			buf.append(values[i]);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	private String dumpLocations() {
		if (mapOfRecognizedLocationsToGlyphs == null) {
			return "";
		}
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		Arrays.sort(locations);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			buf.append(l);
			buf.append(": \"");
			buf.append(mapOfRecognizedLocationsToGlyphs.get(l).getString());
			buf.append("\"\n");
		}
		return buf.toString();
	}
	
	private String dumpLines(boolean showLocation) {
		if (mapOfRecognizedLocationsToGlyphs == null) {
			return "";
		}
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		Arrays.sort(locations);
		StringBuffer buf = new StringBuffer();
		int lastLine = -1;
		int lastX = 0;
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			if (l.getY() != lastLine) {
				if (lastLine != -1) {
					if (showLocation) {
						buf.append("\"");
					}
					buf.append("\n");
				}
				lastLine=l.getY();
				if (showLocation) {
					buf.append(lastLine);
					buf.append(": \"");
				}
				lastX = 0;
			}
			if (l.getX() - lastX > 5) {		// character spacing factor to split strings
				buf.append("\t");
			}
			Glyph glyph = mapOfRecognizedLocationsToGlyphs.get(l);
			buf.append(glyph.getString());
			lastX = l.getX() + glyph.getWidth();
		}
		if (showLocation) {
			buf.append("\"");
		}
		buf.append("\n");
		return buf.toString();
	}
	
	private void initializeGlyphsFromFile(String filename) throws IOException, ParserConfigurationException, SAXException {
		InputStream in;
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			in = classLoader.getResourceAsStream("com/pixelmed/doseocr/"+filename);		// needs to be fully qualified, and needs to use "/" not "." as separator !
		}
		catch (Exception e) {
			slf4jlogger.error("",e);
			in = null;
		}
		if (in == null) {
			slf4jlogger.trace("initializeGlyphsFromFile(): could not get from class loader as resource, loading from file system");
			in = new FileInputStream(filename);
		}
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		Node glyphsNode = document.getDocumentElement();
		if (glyphsNode.getNodeType() == Node.ELEMENT_NODE && glyphsNode.getNodeName().toLowerCase(java.util.Locale.US).equals("glyphs")) {
			slf4jlogger.trace("initializeGlyphsFromFile(): got glyphs node");
			Node glyphNode = glyphsNode.getFirstChild();
			while (glyphNode != null) {
				if (glyphNode.getNodeType() == Node.ELEMENT_NODE && glyphNode.getNodeName().toLowerCase(java.util.Locale.US).equals("glyph")) {
					slf4jlogger.trace("initializeGlyphsFromFile(): got glyph node");
					int width = 0;
					String string = "";
					BitSet set = new BitSet();
					Node glyphNodeChild = glyphNode.getFirstChild();
					while (glyphNodeChild != null) {
						if (glyphNodeChild.getNodeType() == Node.ELEMENT_NODE) {
							String glyphNodeChildName = glyphNodeChild.getNodeName().toLowerCase(java.util.Locale.US);
							if (glyphNodeChildName.equals("bits")) {
								Node bitNode = glyphNodeChild.getFirstChild();
								while (bitNode != null) {
									if (bitNode.getNodeType() == Node.ELEMENT_NODE && bitNode.getNodeName().toLowerCase(java.util.Locale.US).equals("bit")) {
										set.set(Integer.parseInt(bitNode.getTextContent().trim()));
									}
									bitNode = bitNode.getNextSibling();
								}
							}
							else if (glyphNodeChildName.equals("width")) {
								width = Integer.parseInt(glyphNodeChild.getTextContent().trim());
							}
							else if (glyphNodeChildName.equals("string")) {
								string = glyphNodeChild.getTextContent().trim();
							}
						}
						glyphNodeChild = glyphNodeChild.getNextSibling();
					}
					if (width > 0 && string.length() > 0 && !set.isEmpty()) {
						Glyph glyph = new Glyph(set,width,true);
						mapOfGlyphsToStrings.put(glyph,string);
						slf4jlogger.trace("initializeGlyphsFromFile(): stored glyph\n{}\n",glyph);
					}
				}
				glyphNode = glyphNode.getNextSibling();
			}
		}
	}
	
	/**
	 * <p>Get an 8 bit image suitable for thresholding.</p>
	 *
	 * @deprecated			SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getEightBitImageSuitableForThresholding(AttributeList)} instead.
	 * @param	list		the list
	 * @param	debugLevel	ignored
	 * @return				the image
	 */
	public static BufferedImage getEightBitImageSuitableForThresholding(AttributeList list,int debugLevel) throws DicomException {
		slf4jlogger.warn("getEightBitImageSuitableForThresholding(): Debug level supplied as argument ignored");
		return getEightBitImageSuitableForThresholding(list);
	}
	
	/**
	 * <p>Get an 8 bit image suitable for thresholding.</p>
	 *
	 * @param	list		the list
	 * @return				the image
	 */
	public static BufferedImage getEightBitImageSuitableForThresholding(AttributeList list) throws DicomException {
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OCR(): supplied WindowWidth {}",Attribute.getSingleStringValueOrNull(list,TagFromName.WindowWidth));
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OCR(): supplied WindowCenter {}",Attribute.getSingleStringValueOrNull(list,TagFromName.WindowCenter));
		// the correct window values are vital to thresholding
		// the GE values are usually a width of 1 and a center of -2, for a 16 bit signed image with a rescale intercept of -1024,
		// and pixel values of 0x0000 for black and 0x03ff (90123 dec) for white; other patterns are seen but the width of 1 is
		// usually a reliable signal that these have not been messed with
		//
		// if the window values are removed, the statistically derived values work fine, as long as the pixels have not been edited,
		// but if DicomCleaner or similar has been used to blackout identity, then maximum -ve pixel values like 0x8000 will
		// cause the statistically derived values to be broad and hence the thresholding to fail
		
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OCR(): supplied BitsStored {}",Attribute.getSingleStringValueOrNull(list,TagFromName.BitsStored));
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OCR(): supplied PixelRepresentation {}",Attribute.getSingleStringValueOrNull(list,TagFromName.PixelRepresentation));
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OCR(): supplied RescaleIntercept {}",Attribute.getSingleStringValueOrNull(list,TagFromName.RescaleIntercept));
		
		if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.WindowWidth,0)  != 1) {
			slf4jlogger.trace("OCR(): window width is not 1, removing window values and leaving to statistical default");
			list.remove(TagFromName.WindowWidth);	// these may have been inserted by downstream software (e.g., PACS), be incorrect and statistically derived values work better
			list.remove(TagFromName.WindowCenter);
			
			// have encountered screen saved series 10999 GE images that have a pixel padding value of -32768,
			// and window width != 1, which screws up the statistical windowing, so "hide" the padding values
			// when computing statistical windowing
			if (list.get(TagFromName.PixelPaddingValue) == null && Attribute.getSingleIntegerValueOrDefault(list,TagFromName.BitsStored,0) == 16) {
				slf4jlogger.trace("OCR(): no pixel padding value for 16 bit image whose supplied WindowWidth != 1, so putting in 0x8000 just in case");
				Attribute aPixelPaddingValue = null;
				if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0) == 0) {
					aPixelPaddingValue = new UnsignedShortAttribute(TagFromName.PixelPaddingValue);
				}
				else {
					aPixelPaddingValue = new SignedShortAttribute(TagFromName.PixelPaddingValue);
				}
				aPixelPaddingValue.addValue(-32768);
				list.put(aPixelPaddingValue);
			}
		}
		else {
			slf4jlogger.trace("OCR(): window width is 1, so leaving window values alone");
		}
		return ConsumerFormatImageMaker.makeEightBitImage(list);	// handles all the signedness and photometric interpretation nastiness
	}
	
	public OCR(String screenFilesPath) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(screenFilesPath,defaultFileNameOfKnownGlyphs,null);
	}
	
	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(String,String,String)} instead.
	 * @param	screenFilesPath				the path to the dose screen image files on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	public OCR(String screenFilesPath,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(screenFilesPath,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @param	screenFilesPath				the path to the dose screen image files on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	public OCR(String screenFilesPath,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		slf4jlogger.debug("OCR(): file {}",screenFilesPath);
		File screenFileOrFiles = new File(screenFilesPath);
		if (screenFileOrFiles.isFile()) {
			if (DicomFileUtilities.isDicomOrAcrNemaFile(screenFilesPath)) {
				AttributeList list = new AttributeList();
				list.read(screenFilesPath);
				doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
			}
			else {
				BufferedImage image = ImageIO.read(new File(screenFilesPath));
				doCommonConstructorStuff(null/*AttributeList*/,image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
			}
		}
		else if (screenFileOrFiles.isDirectory()) {
			doMultipleFiles(FileUtilities.listFilesRecursively(screenFileOrFiles),fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
			
		}
		else {
			slf4jlogger.info("OCR(): not doing anything - path {}",screenFilesPath);
		}
	}

	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(List,String,String)} instead.
	 * @param	screenFilesPaths			the paths to the dose screen image files on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	public OCR(List<String> screenFilesPaths,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(screenFilesPaths,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @param	screenFilesPaths			the paths to the dose screen image files on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	public OCR(List<String> screenFilesPaths,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		List<File > screenFiles = new LinkedList<File>();
		for (String path : screenFilesPaths) {
			screenFiles.add(new File(path));
		}
		doMultipleFiles(screenFiles,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}
	
	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @deprecated					SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(List)} instead.
	 * @param	screenFilesPaths	the paths to the dose screen image files on which to perform OCR
	 * @param	debugLevel			ignored
	 */
	public OCR(List<String> screenFilesPaths,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(screenFilesPaths);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image files.</p>
	 *
	 * @param	screenFilesPaths	the paths to the dose screen image files on which to perform OCR
	 */
	public OCR(List<String> screenFilesPaths) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(screenFilesPaths,defaultFileNameOfKnownGlyphs,null);
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(List)} instead.
	 * @param	list			the AttributeList of the dose screen image file on which to perform OCR
	 * @param	debugLevel		ignored
	 */
	public OCR(AttributeList list,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(list);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @param	list			the AttributeList of the dose screen image file on which to perform OCR
	 */
	public OCR(AttributeList list) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(list,defaultFileNameOfKnownGlyphs,null);
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(AttributeList,String,String)} instead.
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	public OCR(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	public OCR(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}
	
	/**
	 * <p>Perform OCR on the supplied image.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #OCR(AttributeList,String,String)} instead.
	 * @param	image						the dose screen image on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	public OCR(BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
	}
	
	/**
	 * <p>Perform OCR on the supplied image.</p>
	 *
	 * @param	image						the dose screen image on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	public OCR(BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		doCommonConstructorStuff(null/*AttributeList*/,image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}
	
	private void doMultipleFiles(List<File> candidateFiles,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		slf4jlogger.debug("doMultipleFiles():");
		Map<Integer,AttributeList> doseScreenSeriesSortedByInstanceNumber = new TreeMap<Integer,AttributeList>();
		String commonSeriesInstanceUID = null;
		for (File f : candidateFiles) {
			slf4jlogger.debug("doMultipleFiles(): trying file {}",f);
			if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
				AttributeList list = new AttributeList();
				list.read(f);
				if (isDoseScreenInstance(list)) {
					Integer instanceNumber = new Integer(Attribute.getSingleIntegerValueOrDefault(list,TagFromName.InstanceNumber,-1));
					if (doseScreenSeriesSortedByInstanceNumber.isEmpty()) {
						doseScreenSeriesSortedByInstanceNumber.put(instanceNumber,list);
						commonSeriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
					}
					else {
						String seriesInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID);
						if (seriesInstanceUID.equals(commonSeriesInstanceUID)) {
							if (doseScreenSeriesSortedByInstanceNumber.get(instanceNumber) == null) {
								doseScreenSeriesSortedByInstanceNumber.put(instanceNumber,list);
							}
							else {
								slf4jlogger.warn("doMultipleFiles(): ignoring file {} with duplicate InstanceNumber {} in same series",f,instanceNumber);
							}
						}
						else {
							slf4jlogger.warn("doMultipleFiles(): ignoring file {} with different SeriesInstanceUID {} than enountered in earlier screen {}",f,seriesInstanceUID,commonSeriesInstanceUID);
						}
					}
				}
				else {
					slf4jlogger.debug("doMultipleFiles(): ignoring non dose screen instance {}",f);
				}
			}
			else {
				slf4jlogger.debug("doMultipleFiles(): ignoring non-DICOM file {}",f);
			}
		}
		for (Integer instanceNumber : doseScreenSeriesSortedByInstanceNumber.keySet()) {
			slf4jlogger.debug("doMultipleFiles(): doing {}",instanceNumber);
			AttributeList list = doseScreenSeriesSortedByInstanceNumber.get(instanceNumber);
			if (list != null) {
				doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
			}
			else {
				slf4jlogger.warn("doMultipleFiles(): cannot find AttributeList for InstanceNumber {} in same series",instanceNumber);
			}
		}
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #doCommonConstructorStuff(AttributeList,String,String)} instead.
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	protected void doCommonConstructorStuff(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}
	
	/**
	 * <p>Perform OCR on the supplied image AttributeList.</p>
	 *
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	protected void doCommonConstructorStuff(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		BufferedImage image = null;
		Overlay overlay = new Overlay(list);
		if (overlay.getNumberOfOverlays(0) > 0) {
			slf4jlogger.debug("OCR(): using overlay rather than pixel data");
			image = overlay.getOverlayAsBinaryBufferedImage(0,0);
		}
		else {
			image = getEightBitImageSuitableForThresholding(list);
		}
		doCommonConstructorStuff(list,image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}

	/**
	 * <p>Perform OCR on the supplied image with AttributeList.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #doCommonConstructorStuff(AttributeList,BufferedImage,String,String)} instead.
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	image						the dose screen image on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 * @param	debugLevel					ignored
	 */
	protected void doCommonConstructorStuff(AttributeList list,BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		doCommonConstructorStuff(list,image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
	}
	
	/**
	 * <p>Perform OCR on the supplied image with AttributeList.</p>
	 *
	 * @param	list						the AttributeList of the dose screen image file on which to perform OCR
	 * @param	image						the dose screen image on which to perform OCR
	 * @param	fileNameOfKnownGlyphs		file of known glyphs
	 * @param	fileNameToRecordNewGlyphs	where to record new glyphs
	 */
	protected void doCommonConstructorStuff(AttributeList list,BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs) throws IOException, ParserConfigurationException, SAXException, Exception {
		if (list != null) {
			createOrUpdateCommonAttributeList(list);
		}
		this.image = image;
		this.height = image.getHeight();
		this.width = image.getWidth();
		this.thresholdedPixels = threshold(image);
		processedPixels = new BitSet(height*width);
		mapOfGlyphsToStrings = new HashMap<Glyph,String>();
		mapOfRecognizedLocationsToGlyphs = new HashMap<Location,Glyph>();
		mapOfUnrecognizedLocationsToGlyphs = new HashMap<Location,Glyph>();
		if (fileNameOfKnownGlyphs != null && fileNameOfKnownGlyphs.length() > 0) {
			initializeGlyphsFromFile(fileNameOfKnownGlyphs);
		}
		
		trainingMode = fileNameToRecordNewGlyphs != null && fileNameToRecordNewGlyphs.length() > 0;
		
		// giwarden - Siemens screens have less vertical spacing
		if (isGEDoseScreenInstance(list)) {
			slf4jlogger.debug("GE Settings");
			findConnectedCandidatesAnywhereInImage(defaultGEHorizontalGapTolerance,defaultGEVerticalGapTolerance);
		}
		else if (isSiemensDoseScreenInstance(list)) {
			slf4jlogger.debug("Siemens Settings");
			findConnectedCandidatesAnywhereInImage(defaultSiemensHorizontalGapTolerance,defaultSiemensVerticalGapTolerance);
		}
		else if (isToshibaDoseScreenInstance(list)) {
			slf4jlogger.debug("Toshiba Settings");
			findConnectedCandidatesAnywhereInImage(defaultToshibaHorizontalGapTolerance,defaultToshibaVerticalGapTolerance);
		}
		else if (list == null) {
			slf4jlogger.debug("Unrecognized Settings when no DICOM attributes");
			findConnectedCandidatesAnywhereInImage(defaultGEHorizontalGapTolerance,defaultGEVerticalGapTolerance);
		}
		else {
			slf4jlogger.debug("SOL");
		}
		
		processedPixels.clear();
		//flagAsProcessedLocationsAlreadyRecognized();
		findConnectedCandidatesWithinUnrecognizedGlyphs(1,1);
		slf4jlogger.trace(dumpGlyphsAsStrings());
		slf4jlogger.trace(dumpGlyphsAsXML(true/*onlyNew*/,false/*queryEach*/));
		
		if (trainingMode) {
			FileWriter out = new FileWriter(fileNameToRecordNewGlyphs);
			out.write(dumpGlyphsAsXML(false/*onlyNew*/,true/*queryEach*/));
			out.close();
		}
		if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace(dumpLocations());
		if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug(dumpLines(true));
		
		multiPageLines = multiPageLines + dumpLines(false/*locations*/);
	}
	
	private String dumpMultiPageLines() {
		return multiPageLines;
	}
	
	public String toString() {
		return dumpMultiPageLines();
	}
	
	// GE ...
	
	protected static boolean isGEDoseScreenSeriesNumber(String seriesNumber) {
//System.err.println("OCR.isGEDoseScreenSeriesNumber(): checking "+seriesNumber);
		return seriesNumber.equals("999") || seriesNumber.equals("10999");
	}
	
	public static boolean isPossiblyGEDoseScreenSeries(String manufacturer,String modality,String seriesNumber,String seriesDescription) {
		String useSeriesNumber = seriesNumber == null ? "" : seriesNumber.trim();
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("GE MEDICAL SYSTEMS")) && modality != null && modality.equals("CT") && isGEDoseScreenSeriesNumber(useSeriesNumber);
	}
	
	public static boolean isPossiblyGEDoseScreenSeries(AttributeList list) {
		return isPossiblyGEDoseScreenSeries(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim(),null);
	}
	
	public static boolean isPossiblyGEDoseScreenInstance(String manufacturer,String sopClassUID,String imageType) {
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("GE MEDICAL SYSTEMS")) && (imageType == null || imageType.length() == 0 || imageType.trim().startsWith("DERIVED\\SECONDARY\\SCREEN SAVE"));
	}
	
	public static boolean isPossiblyGEDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),null,Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,""));
	}
	
	public static boolean isGEDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(list) && isGEDoseScreenSeriesNumber(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim());
	}
	
	// Siemens ...
	
	// giwarden
	public static boolean isPossiblySiemensDoseScreenSeries(String manufacturer,String modality,String seriesNumber,String seriesDescription) {
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("SIEMENS")) && modality != null && modality.equals("CT") && seriesNumber != null && (seriesNumber.equals("501") || seriesNumber.equals("503"));
	}
	
	public static boolean isPossiblySiemensDoseScreenSeries(AttributeList list) {
		return isPossiblySiemensDoseScreenSeries(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber),null);
	}
	
	// giwarden
	public static boolean isPossiblySiemensDoseScreenInstance(String manufacturer,String sopClassUID,String imageType) {
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("SIEMENS")) && imageType != null && imageType.trim().equals("DERIVED\\SECONDARY\\OTHER\\CT_SOM5 PROT");
	}
	
	public static boolean isPossiblySiemensDoseScreenInstance(AttributeList list) {
		return isPossiblySiemensDoseScreenInstance(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),null,Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,""));
	}
	
	// giwarden
	public static boolean isSiemensDoseScreenInstance(AttributeList list) {
		String seriesNumber = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim();
		return isPossiblySiemensDoseScreenInstance(list) && (seriesNumber.equals("501") || seriesNumber.equals("503"));
	}
	
	// Toshiba ...
	
	public static boolean isPossiblyToshibaDoseScreenSeries(String manufacturer,String modality,String seriesNumber,String seriesDescription) {
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("TOSHIBA")) && modality != null && modality.equals("CT")
		&& ((seriesNumber != null && (seriesNumber.equals("1000") || seriesNumber.equals("9000")) || (seriesDescription != null && seriesDescription.contains("SUMMARY")) ) );	// not always series 1000 or 9000 or Summary, but otherwise will return every CT image, esp. when Manufacturer not returned in queary response ! :(
	}
	
	public static boolean isPossiblyToshibaDoseScreenSeries(AttributeList list) {
		return isPossiblyToshibaDoseScreenSeries(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber),null);
	}
	
	public static boolean isPossiblyToshibaDoseScreenInstance(String manufacturer,String sopClassUID,String imageType) {
		return (manufacturer == null || manufacturer.length() == 0 || manufacturer.toUpperCase(java.util.Locale.US).contains("TOSHIBA")) && imageType != null && imageType.trim().startsWith("DERIVED\\SECONDARY");
	}
	
	public static boolean isPossiblyToshibaDoseScreenInstance(AttributeList list) {
		return isPossiblyToshibaDoseScreenInstance(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Manufacturer),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID),Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,""));
	}
	
	public static boolean isToshibaDoseScreenInstance(AttributeList list) {
		return isPossiblyToshibaDoseScreenInstance(list)
		&& Attribute.getSingleIntegerValueOrDefault(list,TagFromName.WindowWidth,-1) == 1 && Attribute.getSingleIntegerValueOrDefault(list,TagFromName.WindowCenter,-1) == 0 /* separates from other DERIVED\SECONDARY */
		/*&& Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim().equals("1000")*/;	// not always series 1000 :(
	}
	
	// Generic ...
	
	public static boolean isPossiblyDoseScreenSeries(String manufacturer,String modality,String seriesNumber,String seriesDescription) {
		return isPossiblyGEDoseScreenSeries(manufacturer,modality,seriesNumber,seriesDescription) || isPossiblySiemensDoseScreenSeries(manufacturer,modality,seriesNumber,seriesDescription) || isPossiblyToshibaDoseScreenSeries(manufacturer,modality,seriesNumber,seriesDescription);
	}
	
	public static boolean isPossiblyDoseScreenSeries(AttributeList list) {
		return isPossiblyGEDoseScreenSeries(list) || isPossiblySiemensDoseScreenSeries(list) || isPossiblyToshibaDoseScreenSeries(list);
	}
	
	public static boolean isPossiblyDoseScreenInstance(String manufacturer,String sopClassUID,String imageType) {
		return isPossiblyGEDoseScreenInstance(manufacturer,sopClassUID,imageType) || isPossiblySiemensDoseScreenInstance(manufacturer,sopClassUID,imageType) || isPossiblyToshibaDoseScreenInstance(manufacturer,sopClassUID,imageType);
	}
	
	public static boolean isPossiblyDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(list) || isPossiblySiemensDoseScreenInstance(list) || isPossiblyToshibaDoseScreenInstance(list);
	}
	
	public static boolean isDoseScreenInstance(AttributeList list) {
		return isGEDoseScreenInstance(list) || isSiemensDoseScreenInstance(list) || isToshibaDoseScreenInstance(list);
	}
	
	/**
	 * <p>Create a CTDose instance from the OCR already performed on a GE dose screen image.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getCTDoseFromOCROfGEDoseScreen(OCR,String,String,CTIrradiationEventDataFromImages,boolean)} instead.
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	debugLevel					ignored
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfGEDoseScreen(OCR ocr,int debugLevel,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		return getCTDoseFromOCROfGEDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
	}

	/**
	 * <p>Create a CTDose instance from the OCR already performed on a GE dose screen image.</p>
	 *
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfGEDoseScreen(OCR ocr,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		AttributeList list = ocr.getCommonAttributeList();
		if (startDateTime == null || startDateTime.trim().length() == 0 && list != null) {
			startDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
			if (startDateTime != null && startDateTime.length() == 8) {
				startDateTime = startDateTime + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			}
		}
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
		CTDose ctDose = new CTDose(ScopeOfDoseAccummulation.STUDY,studyInstanceUID,startDateTime,endDateTime,studyDescription);
		ctDose.setSourceOfDoseInformation(SourceOfDoseInformation.DERIVED_FROM_HUMAN_READABLE_REPORTS);
		
		{
			//2	HELICAL	S19.250-I658.250	17.95	1299.58	BODY	32
			Pattern pEvent = Pattern.compile("[ \t]*([0-9]+)[ \t]+([A-Z \t]+)[ \t]+([SI])([0-9]*[.]*[0-9]*)[-]([SI])([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+(.*)[ \t]*");
			//Total	Exam DLP:	1299.58
			Pattern pTotal = Pattern.compile("[ \t]*TOTAL[ \t]*EXAM[ \t]*DLP:[ \t]*([0-9]*[.]*[0-9]*)[ \t]*");
			BufferedReader r = new BufferedReader(new StringReader(ocr.dumpMultiPageLines()));
			String line = null;
			while ((line=r.readLine()) != null) {
				line=line.toUpperCase(java.util.Locale.US);
				slf4jlogger.debug(line);
				if (line.contains("TOTAL")) {
					Matcher mTotal = pTotal.matcher(line);
					if (mTotal.matches()) {
						slf4jlogger.debug("matches");
						int groupCount = mTotal.groupCount();
						slf4jlogger.debug("groupCount = {}",groupCount);
						if (groupCount >= 1) {
							String totalDLP = mTotal.group(1);
							slf4jlogger.debug("Total DLP = {} mGy-cm",totalDLP);
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
				else {
					Matcher mEvent = pEvent.matcher(line);
					if (mEvent.matches()) {
						slf4jlogger.debug("matches");
						int groupCount = mEvent.groupCount();
						slf4jlogger.debug("groupCount = {}",groupCount);
						if (groupCount >= 9) {
							String series = mEvent.group(1);		// first group is not 0, which is the entire match
							slf4jlogger.debug("series = {}",series);
							String scanType = mEvent.group(2).replaceAll("[ \t]+"," ").trim();	// account for "CARDIAC HELICAL"
							slf4jlogger.debug("scanType = {}",scanType);
							String rangeFromSI = mEvent.group(3);
							String rangeFromLocation = mEvent.group(4);
							String rangeToSI = mEvent.group(5);
							String rangeToLocation = mEvent.group(6);
							slf4jlogger.debug("range from = {} {} mm to {} {} mm",rangeFromSI,rangeFromLocation,rangeToSI,rangeToLocation);
							String CTDIvol = mEvent.group(7);
							slf4jlogger.debug("CTDIvol = {} mGy",CTDIvol);
							String DLP = mEvent.group(8);
							slf4jlogger.debug("DLP = {} mGy-cm",DLP);
							String phantom = mEvent.group(9).replaceAll("[ \t]+","").trim();
							slf4jlogger.debug("phantom = {}",phantom);
							ScanRange scanRange = new ScanRange(rangeFromSI,rangeFromLocation,rangeToSI,rangeToLocation);
							CTScanType recognizedScanType = scanRange.isStationary() ? CTScanType.STATIONARY : CTScanType.selectFromDescription(scanType);
							if (recognizedScanType != null && !recognizedScanType.equals(CTScanType.LOCALIZER)) {
								ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,true/*isSeries*/,series,recognizedScanType,scanRange,CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
							}
						}
					}
				}
			}
		}
		if (eventDataFromImages != null) {
			for (int ai = 0; ai<ctDose.getNumberOfAcquisitions(); ++ai) {
				CTDoseAcquisition acq = ctDose.getAcquisition(ai);
				if (acq != null) {
					slf4jlogger.debug("CTDoseAcquisition[{}] = {}",ai,acq);
					ScanRange scanRange = acq.getScanRange();
					// This will work as long as there are not more than one series with the same number and scan range :(
					String key = acq.getSeriesOrAcquisitionNumber()
					+"+"+scanRange.getStartDirection()+scanRange.getStartLocation()
					+"+"+scanRange.getEndDirection()+scanRange.getEndLocation()
					+"+"+acq.getScopeUID();
					slf4jlogger.debug("key = {}",key);
					CTAcquisitionParameters ap = eventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(key);
					slf4jlogger.debug("Matched CTAcquisitionParameters {}",ap);
					if (ap != null) {
						ap.deriveScanningLengthFromDLPAndCTDIVolIfGreater(acq.getDLP(),acq.getCTDIvol());
						acq.setAcquisitionParameters(ap);
					}
				}
			}
		}
		
		if (list.get(TagFromName.ExposureDoseSequence) != null) {
			try {
				CTDose ctDoseFromExposureDoseSequence = ExposureDoseSequence.getCTDoseFromExposureDoseSequence(list,null/*do not need dates from eventDataFromImages*/,false/*do not need sr*/);
//System.err.println("ctDose before merge");
//System.err.println(ctDose);
//System.err.println("ctDoseFromExposureDoseSequence");
//System.err.println(ctDoseFromExposureDoseSequence);
				if (ctDoseFromExposureDoseSequence != null) {
					ctDose.merge(ctDoseFromExposureDoseSequence);
				}
//System.err.println("ctDose after merge");
//System.err.println(ctDose);
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
		
		if (buildSR) {
			GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(list,ctDose,eventDataFromImages);
		}
		
		return ctDose;
	}
	
	/**
	 * <p>Create a CTDose instance from the OCR already performed on a Siemens dose screen image.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getCTDoseFromOCROfSiemensDoseScreen(OCR,String,String,CTIrradiationEventDataFromImages,boolean)} instead.
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	debugLevel					ignored
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfSiemensDoseScreen(OCR ocr,int debugLevel,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		return getCTDoseFromOCROfSiemensDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
	}
	
	/**
	 * <p>Create a CTDose instance from the OCR already performed on a Siemens dose screen image.</p>
	 *
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	// based on patterns initially supplied and tested by giwarden
	public static CTDose getCTDoseFromOCROfSiemensDoseScreen(OCR ocr,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		AttributeList list = ocr.getCommonAttributeList();
		if (startDateTime == null || startDateTime.trim().length() == 0 && list != null) {
			startDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
			if (startDateTime != null && startDateTime.length() == 8) {
				startDateTime = startDateTime + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			}
		}
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
		CTDose ctDose = new CTDose(ScopeOfDoseAccummulation.STUDY,studyInstanceUID,startDateTime,endDateTime,studyDescription);
		ctDose.setSourceOfDoseInformation(SourceOfDoseInformation.DERIVED_FROM_HUMAN_READABLE_REPORTS);
		{
			//Scan	KV	mAs	/	ref.	CTDIvol	DLP	TI	cSL
			// topograms only have 5 columns (description, scan#, kV, TI and cSL)
			// others have either 9 or sometimes 8 columns ("/ref" is absent)
			//Topogram	1	130	8.5	0.6
			//ThorRout.ine	2	130	110	/120	12.32	312	0.6	1.2
			//PreMon.itor.ing	4	130	20	2.08	2	0.6	5.0
			
			// sometimes the acquisition number is a hyphenated range
			//BaseSeq	2-15	110	220	33.51	141	1.5	3.0
			
			// sometimes the phantom is specified this way
			// ABD/PEL	2	120	203	/	240	13.76(a)	700.21	0.5	1.2
			// PhantomType(a)32cm(b)16cm
			
			// sometimes the phantom is specified this way instead, and the topogram has phantom and dose (surprisingly)
			// Topogram	1	120	35mA	0.13L	10	7.8	0.6
			// ThorAbd	4	140	12	/174	1.29L	50	0.5	0.6
			// ..L--32cmS--16cm
			
			Pattern pEventLocalizerWithDoseAndPhantomType	= Pattern.compile("(.*TOPOGRAM.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]*MA[ \t]+([0-9]*[.]*[0-9]*)[(]*[ \t]*([ABLS])[)]*[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithRefExposureAndPhantomType		= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+/[ \t]*([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[(]*[ \t]*([ABLS])[)]*[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithRefExposure					= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+/[ \t]*([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithoutRefExposure				= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			
			//TotalmAs2341    TotalDLP1234      - Siemens examples
			//TotalmAs2123    TotalDLP313mGy*cm
			//TotalmAs3832	  TotalDLP192 mGy*cm
			//mAstotal3996	DLPtotal1169
			
			Pattern pTotal1 = Pattern.compile("[ \t]*TOTAL[ \t]*MAS[ \t]*([0-9]*[.]*[0-9]*)[ \t]+TOTAL[ \t]*DLP[ \t]*([0-9]*[.]*[0-9]*).*");	// sometimes the space is packed up, sometimes not; ignore units at the end (if present)
			Pattern pTotal2 = Pattern.compile("[ \t]*MAS[ \t]*TOTAL[ \t]*([0-9]*[.]*[0-9]*)[ \t]+DLP[ \t]*TOTAL[ \t]*([0-9]*[.]*[0-9]*).*");	// sometimes the words are swapped
			// and sometimes there is no Total DLP at all (just Total mAs on the Operator line
			
			BufferedReader r = new BufferedReader(new StringReader(ocr.dumpMultiPageLines()));
			String line = null;
			while ((line=r.readLine()) != null) {
				line=line.toUpperCase(java.util.Locale.US);
				slf4jlogger.debug(line);
				if (line.contains("TOTALDLP")) {
					Matcher m = pTotal1.matcher(line);
					if (m.matches()) {
						slf4jlogger.debug("matches pTotal1");
						int groupCount = m.groupCount();
						for (int i=1; i<=groupCount; i++) {
							slf4jlogger.debug("m.group({}):{}",i,m.group(i));
						}
						slf4jlogger.debug("groupCount = {}",groupCount);
						if (groupCount >= 1) {
							String totalmAs = m.group(1);
							slf4jlogger.debug("Total mAs = {}",totalmAs);
							String totalDLP = m.group(2);
							if (!totalDLP.contains(".")) {
								totalDLP += ".00"; // Siemens DLP is often an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in total check :( (DAC)
							}
							slf4jlogger.debug("Total DLP = {} mGy-cm",totalDLP);
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
				else if (line.contains("DLPTOTAL")) {
					Matcher m = pTotal2.matcher(line);
					if (m.matches()) {
						slf4jlogger.debug("matches pTotal2");
						int groupCount = m.groupCount();
						for (int i=1; i<=groupCount; i++) {
							slf4jlogger.debug("m.group({}):{}",i,m.group(i));
						}
						slf4jlogger.debug("groupCount = {}",groupCount);
						if (groupCount >= 1) {
							String totalmAs = m.group(1);
							slf4jlogger.debug("Total mAs = {}",totalmAs);
							String totalDLP = m.group(2);
							if (!totalDLP.contains(".")) {
								totalDLP += ".00"; // Siemens DLP is often an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in total check :( (DAC)
							}
							slf4jlogger.debug("Total DLP = {} mGy-cm",totalDLP);
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
				else {
					Matcher mEventWithRefExposureAndPhantomType = pEventWithRefExposureAndPhantomType.matcher(line);
					if (mEventWithRefExposureAndPhantomType.matches()) {
						slf4jlogger.debug("matches pEventWithRefExposureAndPhantomType");
						int groupCount = mEventWithRefExposureAndPhantomType.groupCount();
						slf4jlogger.debug("groupCount = {}",groupCount);
						for (int i=1; i<=groupCount; i++) {
							slf4jlogger.debug("mEventWithRefExposureAndPhantomType.group({}):{}",i,mEventWithRefExposureAndPhantomType.group(i));
						}
						if (groupCount >= 10) {
							// first group is not 0, which is the entire match
							String protocol = mEventWithRefExposureAndPhantomType.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
							slf4jlogger.debug("protocol = {}",protocol);
							String acquisitionNumber = mEventWithRefExposureAndPhantomType.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
							slf4jlogger.debug("acquisitionNumber = {}",acquisitionNumber);
							String scanType = null;
							slf4jlogger.debug("scanType = {}",scanType);
							String KV = mEventWithRefExposureAndPhantomType.group(3);
							slf4jlogger.debug("KV = {}",KV);
							String mAs = mEventWithRefExposureAndPhantomType.group(4);
							slf4jlogger.debug("mAs = {}",mAs);
							String ref = mEventWithRefExposureAndPhantomType.group(5);
							slf4jlogger.debug("ref = {}",ref);
							String CTDIvol = mEventWithRefExposureAndPhantomType.group(6);
							slf4jlogger.debug("CTDIvol = {} mGy",CTDIvol);
							String phantom = mEventWithRefExposureAndPhantomType.group(7);
							slf4jlogger.debug("phantom = {}",phantom);
							CTPhantomType recognizedPhantomType = null;
							if (phantom.equals("A") || phantom.equals("L")) {
								recognizedPhantomType = CTPhantomType.BODY32;
							}
							else if (phantom.equals("B") || phantom.equals("S")) {
								recognizedPhantomType = CTPhantomType.HEAD16;
							}
							String DLP = mEventWithRefExposureAndPhantomType.group(8);
							if (!DLP.contains(".")) {
								DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
							}
							slf4jlogger.debug("DLP = {} mGy-cm",DLP);
							String TI = mEventWithRefExposureAndPhantomType.group(9);
							slf4jlogger.debug("TI = {}",TI);
							String cSL = mEventWithRefExposureAndPhantomType.group(10);
							slf4jlogger.debug("cSL = {}",cSL);
							CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
							ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,recognizedPhantomType));
						}
					}
					else {
						Matcher mEventWithRefExposure = pEventWithRefExposure.matcher(line);
						if (mEventWithRefExposure.matches()) {
							slf4jlogger.debug("matches pEventWithRefExposure");
							int groupCount = mEventWithRefExposure.groupCount();
							slf4jlogger.debug("groupCount = {}",groupCount);
							for (int i=1; i<=groupCount; i++) {
								slf4jlogger.debug("mEventWithRefExposure.group({}):{}",i,mEventWithRefExposure.group(i));
							}
							if (groupCount >= 9) {
								// first group is not 0, which is the entire match
								String protocol = mEventWithRefExposure.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
								slf4jlogger.debug("protocol = {}",protocol);
								String acquisitionNumber = mEventWithRefExposure.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
								slf4jlogger.debug("acquisitionNumber = {}",acquisitionNumber);
								String scanType = null;
								slf4jlogger.debug("scanType = {}",scanType);
								String KV = mEventWithRefExposure.group(3);
								slf4jlogger.debug("KV = {}",KV);
								String mAs = mEventWithRefExposure.group(4);
								slf4jlogger.debug("mAs = {}",mAs);
								String ref = mEventWithRefExposure.group(5);
								slf4jlogger.debug("ref = {}",ref);
								String CTDIvol = mEventWithRefExposure.group(6);
								slf4jlogger.debug("CTDIvol = {} mGy",CTDIvol);
								String DLP = mEventWithRefExposure.group(7);
								if (!DLP.contains(".")) {
									DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
								}
								slf4jlogger.debug("DLP = {} mGy-cm",DLP);
								String TI = mEventWithRefExposure.group(8);
								slf4jlogger.debug("TI = {}",TI);
								String cSL = mEventWithRefExposure.group(9);
								slf4jlogger.debug("cSL = {}",cSL);
								String phantom = "Unknown";
								CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
								ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
							}
						}
						else {
							Matcher mEventWithoutRefExposure = pEventWithoutRefExposure.matcher(line);
							if (mEventWithoutRefExposure.matches()) {
								slf4jlogger.debug("matches pEventWithoutRefExposure");
								int groupCount = mEventWithoutRefExposure.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								for (int i=1; i<=groupCount; i++) {
									slf4jlogger.debug("mEventWithoutRefExposure.group({}):{}",i,mEventWithoutRefExposure.group(i));
								}
								if (groupCount >= 8) {
									String protocol = mEventWithoutRefExposure.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
									slf4jlogger.debug("protocol = {}",protocol);
									String acquisitionNumber = mEventWithoutRefExposure.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
									slf4jlogger.debug("acquisitionNumber = {}",acquisitionNumber);
									String scanType = null;
									slf4jlogger.debug("scanType = {}",scanType);
									String KV = mEventWithoutRefExposure.group(3);
									slf4jlogger.debug("KV = {}",KV);
									String mAs = mEventWithoutRefExposure.group(4);
									slf4jlogger.debug("mAs = {}",mAs);
									String CTDIvol = mEventWithoutRefExposure.group(5);
									slf4jlogger.debug("CTDIvol = {} mGy",CTDIvol);
									String DLP = mEventWithoutRefExposure.group(6);
									if (!DLP.contains(".")) {
										DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
									}
									slf4jlogger.debug("DLP = {} mGy-cm",DLP);
									String TI = mEventWithoutRefExposure.group(7);
									slf4jlogger.debug("TI = {}",TI);
									String cSL = mEventWithoutRefExposure.group(8);
									slf4jlogger.debug("cSL = {}",cSL);
									String phantom = "Unknown";
									CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
									ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
								}
							}
							else {
								Matcher mEventLocalizerWithDoseAndPhantomType = pEventLocalizerWithDoseAndPhantomType.matcher(line);
								if (mEventLocalizerWithDoseAndPhantomType.matches()) {
									slf4jlogger.debug("matches mEventLocalizerWithDoseAndPhantomType");
									int groupCount = mEventLocalizerWithDoseAndPhantomType.groupCount();
									slf4jlogger.debug("groupCount = {}",groupCount);
									for (int i=1; i<=groupCount; i++) {
										slf4jlogger.debug("mEventLocalizerWithDoseAndPhantomType.group({}):{}",i,mEventLocalizerWithDoseAndPhantomType.group(i));
									}
									if (groupCount >= 9) {
										// first group is not 0, which is the entire match
										String protocol = mEventLocalizerWithDoseAndPhantomType.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
										slf4jlogger.debug("protocol = {}",protocol);
										String acquisitionNumber = mEventLocalizerWithDoseAndPhantomType.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
										slf4jlogger.debug("acquisitionNumber = {}",acquisitionNumber);
										String scanType = null;
										slf4jlogger.debug("scanType = {}",scanType);
										String KV = mEventLocalizerWithDoseAndPhantomType.group(3);
										slf4jlogger.debug("KV = {}",KV);
										String mAs = mEventLocalizerWithDoseAndPhantomType.group(4);
										slf4jlogger.debug("mAs = {}",mAs);
										String CTDIvol = mEventLocalizerWithDoseAndPhantomType.group(5);
										slf4jlogger.debug("CTDIvol = {} mGy",CTDIvol);
										String phantom = mEventLocalizerWithDoseAndPhantomType.group(6);
										slf4jlogger.debug("phantom = {}",phantom);
										CTPhantomType recognizedPhantomType = null;
										if (phantom.equals("A") || phantom.equals("L")) {
											recognizedPhantomType = CTPhantomType.BODY32;
										}
										else if (phantom.equals("B") || phantom.equals("S")) {
											recognizedPhantomType = CTPhantomType.HEAD16;
										}
										String DLP = mEventLocalizerWithDoseAndPhantomType.group(7);
										if (!DLP.contains(".")) {
											DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
										}
										slf4jlogger.debug("DLP = {} mGy-cm",DLP);
										String TI = mEventLocalizerWithDoseAndPhantomType.group(8);
										slf4jlogger.debug("TI = {}",TI);
										String cSL = mEventLocalizerWithDoseAndPhantomType.group(9);
										slf4jlogger.debug("cSL = {}",cSL);
										CTScanType recognizedScanType = CTScanType.LOCALIZER;
										ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,recognizedPhantomType));
									}
								}
							}
						}
					}
				}
			}
		}
		if (eventDataFromImages != null) {
			for (int ai = 0; ai<ctDose.getNumberOfAcquisitions(); ++ai) {
				CTDoseAcquisition acq = ctDose.getAcquisition(ai);
				if (acq != null) {
					// No scan range for Siemens dose screens, and uses AcquisitionNUmber not SeriesNumber
					// This will work as long as there are not more than one acquisition with the same number :(
					// ScopeUID was set to StudyInstanceUID in CTDoseAcquisition() constructor
					String key = acq.getSeriesOrAcquisitionNumber()
					+"+"+acq.getScopeUID();
					CTAcquisitionParameters ap = eventDataFromImages.getAcquisitionParametersByAcquisitionNumberAndStudyInstanceUID(key);
					if (ap != null) {
						ap.deriveScanningLengthFromDLPAndCTDIVolIfGreater(acq.getDLP(),acq.getCTDIvol());
						acq.setAcquisitionParameters(ap);
					}
				}
			}
		}
		
		if (buildSR) {
			GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(list,ctDose,eventDataFromImages);
		}
		
		return ctDose;
	}
	
	private enum ToshibaLineRecognizerMode {
		UNRECOGNIZED,
		DOSE_INFORMATION,
		CONTRAST_ENHANCE_INFORMATION,
		DETAIL_INFORMATION
	};
	
	private enum ToshibaDoseInformationMode {
		UNRECOGNIZED,
		DLP_BODY_ONLY,
		DLP_HEAD_ONLY,
		DLP_HEAD_AND_BODY,
		CTDIVOL_BODY_ONLY,
		CTDIVOL_HEAD_ONLY,
		CTDIVOL_HEAD_AND_BODY,
		BODY_ONLY,
		HEAD_ONLY
	};
	
	private enum ToshibaDetailInformationMode {
		UNRECOGNIZED,
		EXPOSURE_TIME_CTDIVOL_DLP_AWAITING_TOTAL_MAS,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS,
		EXPOSURE_TIME_CTDIVOL_DLP_SD,
		TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP,
		CTDIVOL_DLP_AWAITING_TOTAL_MAS_SD,
		SD_CTDIVOLE_DLPE,
		CTDIVOL_DLP_SD_AWAITING_FIRST_ROW,
		CTDIVOL_DLP_SD_AWAITING_SECOND_ROW,
		TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW,
		TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_SECOND_ROW,
		TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_AWAITING_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION,
		TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION,
		TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION,
		START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_SD,
		START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW,
		START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_SECOND_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_AWAITING_DOSE_RED_MODE_START_POS_END_POS,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_AWAITING_TOT_DOSE_RED,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW,		// i.e., if units parentheses ascender splits
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_AWAITING_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_AWAITING_TOTAL_IMAGE_NUMBER,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIRST_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_SECOND_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_THIRD_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW,
		EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIFTH_ROW
	};
	
	/**
	 * <p>Create a CTDose instance from the OCR already performed on a Toshiba dose screen image.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getCTDoseFromOCROfToshibaDoseScreen(OCR,String,String,CTIrradiationEventDataFromImages,boolean)} instead.
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	debugLevel					ignored
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfToshibaDoseScreen(OCR ocr,int debugLevel,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		return getCTDoseFromOCROfToshibaDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
	}

	/**
	 * <p>Create a CTDose instance from the OCR already performed on a Toshiba dose screen image.</p>
	 *
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	startDateTime				the acquisition start date and time
	 * @param	endDateTime					the acquisition end date and time
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfToshibaDoseScreen(OCR ocr,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		AttributeList list = ocr.getCommonAttributeList();
		if (startDateTime == null || startDateTime.trim().length() == 0 && list != null) {
			startDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
			if (startDateTime != null && startDateTime.length() == 8) {
				startDateTime = startDateTime + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			}
		}
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
		CTDose ctDose = new CTDose(ScopeOfDoseAccummulation.STUDY,studyInstanceUID,startDateTime,endDateTime,studyDescription);
		ctDose.setSourceOfDoseInformation(SourceOfDoseInformation.DERIVED_FROM_HUMAN_READABLE_REPORTS);
		{
			
			ToshibaLineRecognizerMode                           majorMode = ToshibaLineRecognizerMode.UNRECOGNIZED;
			ToshibaDoseInformationMode                       doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
			ToshibaDetailInformationMode                   detailInfoMode = ToshibaDetailInformationMode.UNRECOGNIZED;
			
			//First page ...
			//
			// <<Dose Information>>
			
			// Note that in some fonts, lowercase 'i' is preceded by separate dot, and lowercase 'l' indistinguishable from uppercase 'I', so use ?[IL] to match 'I' or 'L'
			
			String rDoseInformationHeader           	= "^[ \t]*<<[ \t]*DOSE[ \t]*[.]?[IL]NFORMAT[.]?[IL]ON[ \t]*>>[ \t]*$";
			String rContrastEnhanceInformationHeader	= "^[ \t]*<<[ \t]*CONTRAST[ \t]*[/]*[ \t]*ENHANCE[ \t]*INFORMATION[ \t]*>>[ \t]*$";
			
			// sometimes measure with head and/or body phantom specified also has units in parentheses, and hence no ascender to split lines, other times not ...
			String rDLPBodyOnly = "^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*-[ \t]*\\(BODY\\)(:|..)[ \t]*$";	// hyphen after head
			String rDLPHeadOnly = "^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*\\(BODY\\)(:|..)[ \t]*-[ \t]*$";	// hyphen after body
			String rDLPHeadAndBody = "^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*\\(BODY\\)(:|..)[ \t]*$";
			String rCTDIvolBodyOnly = "^[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*\\(MGY\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*-[ \t]*\\(BODY\\)(:|..)[ \t]*$";	// hyphen after head
			String rCTDIvolHeadOnly = "^[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*\\(MGY\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*\\(BODY\\)(:|..)[ \t]*-[ \t]*$";	// hyphen after body
			String rCTDIvolHeadAndBody = "^[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*\\(MGY\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*\\(BODY\\)(:|..)[ \t]*$";
			String rBodyOnly = "^[ \t]*\\(HEAD\\)(:|..)[ \t]*-[ \t]*\\(BODY\\)(:|..)[ \t]*$";	// hyphen after head
			String rHeadOnly = "^[ \t]*\\(HEAD\\)(:|..)[ \t]*\\(BODY\\)(:|..)[ \t]*-[ \t]*$";	// hyphen after body
			
			Pattern pOneDecimalNumber = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pCTDIvolAndOneDecimalNumber = Pattern.compile("^[ \t]*CTD[.]?[IL]VOL[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pDLPAndOneDecimalNumber = Pattern.compile("^[ \t]*D[.]?[IL]P[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			
			Pattern pTotalDLPOnly = Pattern.compile("^[ \t]*TOTA[.]?[IL][ \t]*D[.]?[IL]P[ \t]*MGYCM[ \t]*(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pTotalEffDLPOnly = Pattern.compile("^[ \t]*EFF[ \t]*[.][ \t]*D[.]?[IL]P[ \t]*[(]*MGYCM[)]*[ \t]*(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			
			// sometimes numeric values are not on separate line ...
			Pattern pDLPBodyOnly =    Pattern.compile("^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*-[ \t]*\\(BODY\\)(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pDLPHeadOnly =    Pattern.compile("^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*\\(BODY\\)(:|..)[ \t]*-[ \t]*$");
			Pattern pDLPHeadAndBody = Pattern.compile("^[ \t]*D[.]?[IL]P([.]E)?[ \t]*\\(MGYCM\\)[ \t]*\\(HEAD\\)(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*\\(BODY\\)(:|..)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			
			String totalDLPHead;
			String totalDLPBody;
			String totalCTDIvolHead;
			String totalCTDIvolBody;
			
			
			//Subsequent pages ...
			//
			// <<Detail Information>>
			
			String rDetailInformationHeader	            = "^[ \t]*<<[ \t]*DETA[.]?[IL][.]?[IL][ \t]*[.]?[IL]NFORMAT[.]?[IL]ON[ \t]*>>.*$";	// handle I versus i versus L ambiguity, since sometimes mixed case
			
			String rProtocolLine = "^[ \t]*[0-9]+[.].*$";	// may only pick up part of actual line if ascenders and spacing cause split (e.g., parentheses or slash)
			
			// multi-line pattern
			
			String rTotalmAsExposureTimeCTDIvolDLPWithoutUnits = "^[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VO[.]?[IL][ \t]*D[.]?[IL]P[ \t]*$";
			String rExposureTimeCTDIvolDLPWithUnits = "^[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VOL\\(MGY\\)[ \t]*D[.]?[IL]P\\(MGYCM\\)[ \t]*$";	// is really on same line as Total mAs, but units in parentheses ascender splits lines
			String rExposureTimeCTDIvolDLPTotalmAs = "^[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*D[.]?[IL]P([.]E)?[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*$";
			String rExposureTimeCTDIvolDLPSD = "^[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*D[.]?[IL]P([.]E)?[ \t]*SD[ \t]*$";
			String rTotalmAsExposureTimeCTDIvolDLP = "^[ \t]*TOTAL[ \t]*MAS[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*D[.]?[IL]P([.]E)?[ \t]*$";
			
			String rTotalmAs = "^[ \t]*TOTAL[ \t]*MAS[ \t]*$";
			
			// multi-line pattern
			
			String rTotalmAsExposureTimeCTDIvolDLPWithESuffix = "^[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*CTD[.]?[IL]VOL[.]E[ \t]*D[.]?[IL]P[.]E[ \t]*$";
			String rTotalDoseReductionDoseReductionModeModulation = "^[ \t]*TOT[.][ \t]*DOSE[ \t]*RED[.][ \t]*DOSE[ \t]*RED[.][ \t]*MODE[ \t]*MODU[.]?[IL]ATION[ \t]*$";
			String rTotalDoseReductionOnly = "^[ \t]*TOT[.][ \t]*DOSE[ \t]*RED[.][ \t]*$";
			
			String rStartPosEndPosExposureTimeTotalmAs = "^[ \t]*START[ \t]*POS[.][ \t]*END[ \t]*POS[.][ \t]*EXPOSURE[ \t]*T[.]?[IL][.]?ME[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*$";
			String rEffCTDIvolMeanEffDLPSD = "^[ \t]*EFF[.][ \t]*CTD[.]?[IL]VOL[ \t]*MEAN[ \t]*EFF[.][ \t]*D[.]?[IL]P[ \t]*SD[ \t]*$";
			
			String rEffDLPWithUnits = "^[ \t]*EFF[.]D[.]?[IL]P\\(MGYCM\\)[ \t]*$";
			String rDoseRedModStartPosEndPos = "^[ \t]*DOSE[ \t]*RED[.][ \t]*MODE[ \t]*START[ \t]*POS[.][ \t]*END[ \t]*POS[.][ \t]*$";
			String rTotDoseRed = "^[ \t]*TOT[.][ \t]*DOSE[ \t]*RED[.]?[(]?%?[)]?[ \t]*$";
			
			String rStartPosEndPosCTDIairDLPair = "^[ \t]*START[ \t]*POS[.][ \t]*END[ \t]*POS[.][ \t]*CTD[.]?[IL]AIR[ \t]*D[.]?[IL]PA[.]?[IL]R[ \t]*$";
			String rEffCTDIvolMeanEffDLPModulationSD = "^[ \t]*EFF[.][ \t]*CTD[.]?[IL]VOL[ \t]*MEAN[ \t]*EFF[.][ \t]*D[.]?[IL]P[ \t]*MODU[.]?[IL]AT[.]?[IL]ON[ \t]*SD[ \t]*$";
			String rBoostQDSDoseReductionModeTotalDoseReduction = "^[ \t]*BOOST[ \t]*QDS[ \t]*DOSE[ \t]*RED[.][ \t]*MODE[ \t]*TOT[.][ \t]*DOSE[ \t]*RED[.][ \t]*$";
			String rTotalImageNumber = "^[ \t]*TOTA[.]?[IL][ \t]*[.]?[IL]MAGE[ \t]*NUMBER[ \t]*$";
			
			// single-line pattern but ascenders split header
			
			String rCTDIvolDLPWithUnits = "^[ \t]*CTD[.]?[IL]VOL[ \t]*\\(MGY\\)[ \t]*D[.]?[IL]P[ \t]*\\(MGYCM\\)[ \t]*$";
			String rTotalmAsSD = "^[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*SD[ \t]*$";
			
			// single-line pattern
			
			String rTotalmAsCTDIvolDLPWithoutUnitsSD = "^[ \t]*TOTA[.]?[IL][ \t]*MAS[ \t]*CTD[.]?[IL]VOL([.]E)?[ \t]*D[.]?[IL]P([.]E)?[ \t]*SD[ \t]*$";	// sometimes .e suffix; sometimed I and L ambiguous
			String rSDCTDIvolEDLPE = "^[ \t]*SD[ \t]*CTD[.]?[IL]VOL[.]E([ \t]*MEAN)?[ \t]*D[.]?[IL]P[.]E[ \t]*$";
			String rCTDIvolDLPSD = "^[ \t]*CTD[.]?[IL]VOL[ \t]*D[.]?[IL]P[ \t]*SD[ \t]*$";
			
			String protocol = null;
			
			String eventDLP = null;
			String eventDLPPhantom = null;
			String eventCTDIvol = null;
			String eventCTDIvolPhantom = null;
			String eventType = null;
			String eventTotalmAs = null;
			String eventExposureTime = null;
			String eventTotalDoseReduction = null;
			String eventDoseReductionMode = null;
			String eventModulation = null;
			String eventStartPos = null;
			String eventEndPos = null;
			String eventSD = null;
			
			Pattern pTypeAndFourDecimalNumbersLastTwoWithPhantom = Pattern.compile("^[ \t]*([^ \t0-9][^ \t]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*$");
			Pattern pTypeAndTwoDecimalNumbersWithPhantom = Pattern.compile("^[ \t]*([^ \t0-9][^ \t]*)[ \t]+([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*$");
			Pattern pTwoDecimalNumbersWithPhantom = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*([0-9]+[.]*[0-9]*)\\((BODY|HEAD)\\)[ \t]*$");
			Pattern pTypeAndOneOrTwoDecimalNumbers = Pattern.compile("^[ \t]*([^ \t0-9][^ \t]*)[ \t]+([0-9]+[.]*[0-9]*)([ \t]+)?([0-9]+[.]*[0-9]*)?[ \t]*$");
			Pattern pTypeAndTwoPositionsAndTwoMoreDecimalNumbers = Pattern.compile("^[ \t]*([^ \t0-9][^ \t]*)[ \t]+([+-][0-9]+[.]*[0-9]*)[ \t]+([+-][0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pTwoPositionsAndTwoMoreDecimalNumbers = Pattern.compile("^[ \t]*([+-][0-9]+[.]*[0-9]*)[ \t]+([+-][0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pTwoDecimalNumbers = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pThreeDecimalNumbers = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pTypeAlone = Pattern.compile("^[ \t]*([^ \t0-9][^ \t]*)[ \t]*$");	// not allowed to start with numeric, in order to distinguish from protocol pattern that always does
			Pattern pDoseReductionDoseReductionModeModulation = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]+([A-Z0-9]+)[ \t]+([A-Z0-9]+)[ \t]*$");
			Pattern pOneDecimalNumberDoseReductionModeAndTwoPositions = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]+([A-Z0-9]*)[ \t]*([+-][0-9]+[.]*[0-9]*)[ \t]+([+-][0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber = Pattern.compile("^[ \t]*([0-9]+[.]*[0-9]*)[ \t]+([0-9]+[.]*[0-9]*)[ \t]+([A-Z0-9]*)[ \t]*([0-9]+[.]*[0-9]*)[ \t]*$");
			Pattern pThreeStringsAndOneDecimalNumber = Pattern.compile("^[ \t]*([A-Z-0-9]+)[ \t]+([A-Z-0-9]+)[ \t]+([A-Z-0-9]+)[ \t]+([0-9]+[.]*[0-9]*)[ \t]*$");
			
			
			boolean detailValuesComplete = false;
			boolean detailValuesStarted = false;
			
			BufferedReader r = new BufferedReader(new StringReader(ocr.dumpMultiPageLines()));
			String line = null;
			while ((line=r.readLine()) != null) {
				line=line.toUpperCase(java.util.Locale.US);
				slf4jlogger.debug("majorMode = {}",majorMode);
				slf4jlogger.debug(line);
				if (line.matches(rDoseInformationHeader)) {
					majorMode = ToshibaLineRecognizerMode.DOSE_INFORMATION;
					totalDLPHead = null;
					totalDLPBody = null;
					totalCTDIvolHead = null;
					totalCTDIvolBody = null;
				}
				else if (line.matches(rContrastEnhanceInformationHeader)) {
					majorMode = ToshibaLineRecognizerMode.CONTRAST_ENHANCE_INFORMATION;
				}
				else if (line.matches(rDetailInformationHeader)) {
					majorMode = ToshibaLineRecognizerMode.DETAIL_INFORMATION;
				}
				else {
					if (majorMode == ToshibaLineRecognizerMode.DOSE_INFORMATION){
						slf4jlogger.debug("doseInfoMode = {}",doseInfoMode);
						
						if (doseInfoMode == ToshibaDoseInformationMode.BODY_ONLY) {
							Matcher mCTDIvol = pCTDIvolAndOneDecimalNumber.matcher(line);
							if (mCTDIvol.matches()) {
								slf4jlogger.debug("matches pCTDIvolAndOneDecimalNumber");
								int groupCount = mCTDIvol.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalCTDIvolBody = mCTDIvol.group(1);
									slf4jlogger.debug("totalCTDIvolBody = {}",totalCTDIvolBody);
								}
								else {
									slf4jlogger.debug("BAD no BODY_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								Matcher mDLP = pDLPAndOneDecimalNumber.matcher(line);
								if (mDLP.matches()) {
									slf4jlogger.debug("matches pDLPAndOneDecimalNumber");
									int groupCount = mDLP.groupCount();
									slf4jlogger.debug("groupCount = {}",groupCount);
									if (groupCount == 1) {
										totalDLPBody = mDLP.group(1);
										slf4jlogger.debug("totalDLPBody = {}",totalDLPBody);
										ctDose.setDLPTotal(totalDLPBody);
										ctDose.setDLPTotalPhantom(CTPhantomType.BODY32);
									}
									else {
										slf4jlogger.debug("BAD no BODY_ONLY value on next line - group count inconsistent with pattern");
									}
								}
								else {
									slf4jlogger.debug("BAD no BODY_ONLY value on next line - no match to pattern");
								}
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.HEAD_ONLY) {
							Matcher mCTDIvol = pCTDIvolAndOneDecimalNumber.matcher(line);
							if (mCTDIvol.matches()) {
								slf4jlogger.debug("matches pCTDIvolAndOneDecimalNumber");
								int groupCount = mCTDIvol.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalCTDIvolHead = mCTDIvol.group(1);
									slf4jlogger.debug("totalCTDIvolHead = {}",totalCTDIvolHead);
								}
								else {
									slf4jlogger.debug("BAD no HEAD_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								Matcher mDLP = pDLPAndOneDecimalNumber.matcher(line);
								if (mDLP.matches()) {
									slf4jlogger.debug("matches pDLPAndOneDecimalNumber");
									int groupCount = mDLP.groupCount();
									slf4jlogger.debug("groupCount = {}",groupCount);
									if (groupCount == 1) {
										totalDLPHead = mDLP.group(1);
										slf4jlogger.debug("totalDLPHead = {}",totalDLPHead);
										ctDose.setDLPTotal(totalDLPHead);
										ctDose.setDLPTotalPhantom(CTPhantomType.HEAD16);
									}
									else {
										slf4jlogger.debug("BAD no HEAD_ONLY value on next line - group count inconsistent with pattern");
									}
								}
								else {
									slf4jlogger.debug("BAD no HEAD_ONLY value on next line - no match to pattern");
								}
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.DLP_BODY_ONLY) {
							Matcher m = pOneDecimalNumber.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pOneDecimalNumber");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalDLPBody = m.group(1);
									slf4jlogger.debug("totalDLPBody = {}",totalDLPBody);
									ctDose.setDLPTotal(totalDLPBody);
									ctDose.setDLPTotalPhantom(CTPhantomType.BODY32);
								}
								else {
									slf4jlogger.debug("BAD no DLP_BODY_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no DLP_BODY_ONLY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.DLP_HEAD_ONLY) {
							Matcher m = pOneDecimalNumber.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pOneDecimalNumber");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalDLPHead = m.group(1);
									slf4jlogger.debug("totalDLPHead = {}",totalDLPHead);
									ctDose.setDLPTotal(totalDLPHead);
									ctDose.setDLPTotalPhantom(CTPhantomType.HEAD16);
								}
								else {
									slf4jlogger.debug("BAD no DLP_HEAD_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no DLP_HEAD_ONLY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.DLP_HEAD_AND_BODY) {
							Matcher m = pTwoDecimalNumbers.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pTwoDecimalNumbers");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 2) {
									totalDLPHead = m.group(1);
									slf4jlogger.debug("totalDLPHead = {}",totalDLPHead);
									totalDLPBody = m.group(2);
									slf4jlogger.debug("totalDLPBody = {}",totalDLPBody);
									ctDose.setDLPTotal(totalDLPHead,totalDLPBody);
								}
								else {
									slf4jlogger.debug("BAD no DLP_HEAD_AND_BODY values on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no DLP_HEAD_AND_BODY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.CTDIVOL_BODY_ONLY) {
							Matcher m = pOneDecimalNumber.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pOneDecimalNumber");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalCTDIvolBody = m.group(1);
									slf4jlogger.debug("totalCTDIvolBody = {}",totalCTDIvolBody);
								}
								else {
									slf4jlogger.debug("BAD no CTDIVOL_BODY_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no CTDIVOL_BODY_ONLY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.CTDIVOL_HEAD_ONLY) {
							Matcher m = pOneDecimalNumber.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pOneDecimalNumber");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 1) {
									totalCTDIvolHead = m.group(1);
									slf4jlogger.debug("totalCTDIvolHead = {}",totalCTDIvolHead);
								}
								else {
									slf4jlogger.debug("BAD no CTDIVOL_HEAD_ONLY value on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no CTDIVOL_HEAD_ONLY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else if (doseInfoMode == ToshibaDoseInformationMode.CTDIVOL_HEAD_AND_BODY) {
							Matcher m = pTwoDecimalNumbers.matcher(line);
							if (m.matches()) {
								slf4jlogger.debug("matches pTwoDecimalNumbers");
								int groupCount = m.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 2) {
									totalCTDIvolHead = m.group(1);
									slf4jlogger.debug("totalCTDIvolHead = {}",totalCTDIvolHead);
									totalCTDIvolBody = m.group(2);
									slf4jlogger.debug("totalCTDIvolBody = {}",totalCTDIvolBody);
								}
								else {
									slf4jlogger.debug("BAD no CTDIVOL_HEAD_AND_BODY values on next line - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("BAD no CTDIVOL_HEAD_AND_BODY value on next line - no match to pattern");
							}
							doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;
						}
						else {
							if (line.matches(rBodyOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.BODY_ONLY;
							}
							else if (line.matches(rHeadOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.HEAD_ONLY;
							}
							else if (line.matches(rDLPBodyOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.DLP_BODY_ONLY;
							}
							else if (line.matches(rDLPHeadOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.DLP_HEAD_ONLY;
							}
							else if (line.matches(rDLPHeadAndBody)) {
								doseInfoMode = ToshibaDoseInformationMode.DLP_HEAD_AND_BODY;
							}
							else if (line.matches(rCTDIvolBodyOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.CTDIVOL_BODY_ONLY;
							}
							else if (line.matches(rCTDIvolHeadOnly)) {
								doseInfoMode = ToshibaDoseInformationMode.CTDIVOL_HEAD_ONLY;
							}
							else if (line.matches(rCTDIvolHeadAndBody)) {
								doseInfoMode = ToshibaDoseInformationMode.CTDIVOL_HEAD_AND_BODY;
							}
							else {
								// patterns that do not require a sub-mode ...
								Matcher mTotalEffDLPOnly = pTotalEffDLPOnly.matcher(line);
								slf4jlogger.debug("checking for mTotalEffDLPOnly");
								if (mTotalEffDLPOnly.matches()) {
									slf4jlogger.debug("matches mTotalEffDLPOnly");
									int groupCount = mTotalEffDLPOnly.groupCount();
									slf4jlogger.debug("groupCount = {}",groupCount);
									if (groupCount == 2) {
										String totalDLP = mTotalEffDLPOnly.group(2);	// 1st is used for .. vs. :
										slf4jlogger.debug("totalDLP = {}",totalDLP);
										ctDose.setDLPTotal(totalDLP);
									}
									else {
										slf4jlogger.debug("BAD - mTotalEffDLPOnly values - group count inconsistent with pattern");
									}
								}
								else {
									Matcher mDLPBodyOnly = pDLPBodyOnly.matcher(line);
									slf4jlogger.debug("checking for mDLPBodyOnly");
									if (mDLPBodyOnly.matches()) {
										slf4jlogger.debug("matches mDLPBodyOnly");
										int groupCount = mDLPBodyOnly.groupCount();
										slf4jlogger.debug("groupCount = {}",groupCount);
										if (groupCount == 4) {
											totalDLPBody = mDLPBodyOnly.group(4);
											slf4jlogger.debug("totalDLPBody = {}",totalDLPBody);
											ctDose.setDLPTotal(totalDLPBody);
											ctDose.setDLPTotalPhantom(CTPhantomType.BODY32);
										}
										else {
											slf4jlogger.debug("BAD - mDLPBodyOnly values - group count inconsistent with pattern");
										}
									}
									else {
										Matcher mDLPHeadOnly = pDLPHeadOnly.matcher(line);
										slf4jlogger.debug("checking for mDLPHeadOnly");
										if (mDLPHeadOnly.matches()) {
											slf4jlogger.debug("matches mDLPHeadOnly");
											int groupCount = mDLPHeadOnly.groupCount();
											slf4jlogger.debug("groupCount = {}",groupCount);
											if (groupCount == 4) {
												totalDLPHead = mDLPHeadOnly.group(3);
												slf4jlogger.debug("totalDLPHead = {}",totalDLPHead);
												ctDose.setDLPTotal(totalDLPHead);
												ctDose.setDLPTotalPhantom(CTPhantomType.HEAD16);
											}
											else {
												slf4jlogger.debug("BAD - mDLPHeadOnly values - group count inconsistent with pattern");
											}
										}
										else {
											Matcher mDLPHeadAndBody = pDLPHeadAndBody.matcher(line);
											slf4jlogger.debug("checking for mDLPHeadAndBody");
											if (mDLPHeadAndBody.matches()) {
												slf4jlogger.debug("matches mDLPHeadAndBody");
												int groupCount = mDLPHeadAndBody.groupCount();
												slf4jlogger.debug("groupCount = {}",groupCount);
												if (groupCount == 5) {
													totalDLPHead = mDLPHeadAndBody.group(3);
													slf4jlogger.debug("totalDLPHead = {}",totalDLPHead);
													totalDLPBody = mDLPHeadAndBody.group(5);
													slf4jlogger.debug("totalDLPBody = {}",totalDLPBody);
													ctDose.setDLPTotal(totalDLPHead,totalDLPBody);
												}
												else {
													slf4jlogger.debug("BAD - mDLPBodyOnly values - group count inconsistent with pattern");
												}
											}
										}
									}
								}
								doseInfoMode = ToshibaDoseInformationMode.UNRECOGNIZED;		// reset every line
							}
						}
					}
					else if (majorMode == ToshibaLineRecognizerMode.DETAIL_INFORMATION) {
						slf4jlogger.debug("detailInfoMode = {}",detailInfoMode);
						if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS && !line.matches(rEffDLPWithUnits) && !line.matches(rStartPosEndPosCTDIairDLPair)	/* may or may not be in multi-line header */
						 || detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_SD
						 || detailInfoMode == ToshibaDetailInformationMode.SD_CTDIVOLE_DLPE
						 || detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_SECOND_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION
						 || detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION
						 || detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_SECOND_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_SECOND_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIRST_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_SECOND_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_THIRD_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW
						 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIFTH_ROW
							) {
							slf4jlogger.debug("Attempting to match mOneDecimalNumberDoseReductionModeAndTwoPositions");
							Matcher mOneDecimalNumberDoseReductionModeAndTwoPositions = pOneDecimalNumberDoseReductionModeAndTwoPositions.matcher(line);
							if (mOneDecimalNumberDoseReductionModeAndTwoPositions.matches()) {
								slf4jlogger.debug("matches pOneDecimalNumberDoseReductionModeAndTwoPositions");
								int groupCount = mOneDecimalNumberDoseReductionModeAndTwoPositions.groupCount();
								slf4jlogger.debug("groupCount = {}",groupCount);
								if (groupCount == 4) {
									detailValuesStarted = true;
									String eventEffectiveDLP = mOneDecimalNumberDoseReductionModeAndTwoPositions.group(1);
									slf4jlogger.debug("eventEffectiveDLP = {}",eventEffectiveDLP);
									eventDoseReductionMode = mOneDecimalNumberDoseReductionModeAndTwoPositions.group(2);
									slf4jlogger.debug("eventDoseReductionMode = {}",eventDoseReductionMode);
									eventStartPos = mOneDecimalNumberDoseReductionModeAndTwoPositions.group(3);
									slf4jlogger.debug("eventStartPos = {}",eventStartPos);
									eventEndPos = mOneDecimalNumberDoseReductionModeAndTwoPositions.group(4);
									slf4jlogger.debug("eventEndPos = {}",eventEndPos);
									if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW) {
										detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW;
									}
								}
								else {
									slf4jlogger.debug("BAD - OneDecimalNumberDoseReductionModeAndTwoPositions values - group count inconsistent with pattern");
								}
							}
							else {
								slf4jlogger.debug("Attempting to match mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber");
								Matcher mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber = pTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.matcher(line);
								if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_THIRD_ROW
									&& mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.matches()) {
									slf4jlogger.debug("matches pTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber");
									int groupCount = mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.groupCount();
									slf4jlogger.debug("groupCount = {}",groupCount);
									if (groupCount == 4) {
										detailValuesStarted = true;
										String eventEffectiveCTDIvolMean = mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.group(1);
										slf4jlogger.debug("eventEffectiveCTDIvolMean = {}",eventEffectiveCTDIvolMean);
										String eventEffectiveDLP = mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.group(2);
										slf4jlogger.debug("eventEffectiveDLP = {}",eventEffectiveDLP);
										eventDoseReductionMode = mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.group(3);
										slf4jlogger.debug("eventDoseReductionMode = {}",eventDoseReductionMode);
										eventSD = mTwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber.group(4);
										slf4jlogger.debug("eventSD = {}",eventSD);
										detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW;
									}
									else {
										slf4jlogger.debug("BAD - TwoDecimalNumbersDoseReductionModeAndOneMoreDecimalNumber values - group count inconsistent with pattern");
									}
								}
								else {
									slf4jlogger.debug("Attempting to match mThreeStringsAndOneDecimalNumber");
									Matcher mThreeStringsAndOneDecimalNumber = pThreeStringsAndOneDecimalNumber.matcher(line);
									if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW
										&& mThreeStringsAndOneDecimalNumber.matches()) {
										slf4jlogger.debug("matches pThreeStringsAndOneDecimalNumber");
										int groupCount = mThreeStringsAndOneDecimalNumber.groupCount();
										slf4jlogger.debug("groupCount = {}",groupCount);
										if (groupCount == 4) {
											detailValuesStarted = true;
											String eventBoost = mThreeStringsAndOneDecimalNumber.group(1);
											slf4jlogger.debug("eventBoost = {}",eventBoost);
											String eventQDS = mThreeStringsAndOneDecimalNumber.group(2);
											slf4jlogger.debug("eventQDS = {}",eventQDS);
											eventDoseReductionMode = mThreeStringsAndOneDecimalNumber.group(3);
											slf4jlogger.debug("eventDoseReductionMode = {}",eventDoseReductionMode);
											eventTotalDoseReduction = mThreeStringsAndOneDecimalNumber.group(4);
											slf4jlogger.debug("eventTotalDoseReduction = {}",eventTotalDoseReduction);
											detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIFTH_ROW;
										}
										else {
											slf4jlogger.debug("BAD - ThreeStringsAndOneDecimalNumber values - group count inconsistent with pattern");
										}
									}
									else {
										slf4jlogger.debug("Attempting to match mTwoDecimalNumbersWithPhantom");
										Matcher mTwoDecimalNumbersWithPhantom = pTwoDecimalNumbersWithPhantom.matcher(line);
										if (mTwoDecimalNumbersWithPhantom.matches()) {
											slf4jlogger.debug("matches pTwoDecimalNumbersWithPhantom");
											
											if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW) {
												// third row was absent and we are into another event :(
												// record per event values ...
												slf4jlogger.debug("record per event values before starting new event group");
												slf4jlogger.debug("Event:");
												slf4jlogger.debug("\tprotocol = {}",protocol);
												slf4jlogger.debug("\teventDLP = {}",eventDLP);
												slf4jlogger.debug("\teventDLPPhantom = {}",eventDLPPhantom);
												slf4jlogger.debug("\teventCTDIvol = {}",eventCTDIvol);
												slf4jlogger.debug("\teventCTDIvolPhantom = {}",eventCTDIvolPhantom);
												slf4jlogger.debug("\teventType = {}",eventType);
												slf4jlogger.debug("\teventTotalmAs = {}",eventTotalmAs);
												slf4jlogger.debug("\teventExposureTime = {}",eventExposureTime);
												slf4jlogger.debug("\teventTotalDoseReduction = {}",eventTotalDoseReduction);
												slf4jlogger.debug("\teventDoseReductionMode = {}",eventDoseReductionMode);
												slf4jlogger.debug("\teventModulation = {}",eventModulation);
												slf4jlogger.debug("\teventStartPos = {}",eventStartPos);
												slf4jlogger.debug("\teventEndPos = {}",eventEndPos);
												slf4jlogger.debug("\teventSD = {}",eventSD);
												
												CTScanType recognizedScanType = CTScanType.selectFromDescription(eventType);
												CTPhantomType recognizedPhantomType = CTPhantomType.selectFromDescription(eventDLPPhantom);		// assume same as eventCTDIvolPhantom, but should check :(
												ScanRange scanRange = eventStartPos != null && eventEndPos != null ? new ScanRange(eventStartPos,eventEndPos) : null;
												ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,null/*acquisitionNumber*/,recognizedScanType,scanRange,eventCTDIvol,eventDLP,recognizedPhantomType));
												
												detailValuesComplete = false;	// may be another set with no intervening header line, so clear state and old values
												detailValuesStarted = false;
												eventDLP = null;
												eventDLPPhantom = null;
												eventCTDIvol = null;
												eventCTDIvolPhantom = null;
												eventType = null;
												eventTotalmAs = null;
												eventExposureTime = null;
												eventTotalDoseReduction = null;
												eventDoseReductionMode = null;
												eventModulation = null;
												eventStartPos = null;
												eventEndPos = null;
												eventSD = null;
												// reset multiline processing to first line
												detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW;
											}
											
											int groupCount = mTwoDecimalNumbersWithPhantom.groupCount();
											slf4jlogger.debug("groupCount = {}",groupCount);
											if (groupCount == 4) {
												detailValuesStarted = true;
												eventCTDIvol = mTwoDecimalNumbersWithPhantom.group(1);
												slf4jlogger.debug("eventCTDIvol = {}",eventCTDIvol);
												eventCTDIvolPhantom = mTwoDecimalNumbersWithPhantom.group(2);
												slf4jlogger.debug("eventCTDIvolPhantom = {}",eventCTDIvolPhantom);
												eventDLP = mTwoDecimalNumbersWithPhantom.group(3);
												slf4jlogger.debug("eventDLP = {}",eventDLP);
												eventDLPPhantom = mTwoDecimalNumbersWithPhantom.group(4);
												slf4jlogger.debug("eventDLPPhantom = {}",eventDLPPhantom);
												if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW) {
													detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_SECOND_ROW;
												}
												else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW) {
													detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW;
												}
												else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIRST_ROW) {
													detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_SECOND_ROW;
												}
												else if (detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_FIRST_ROW) {
													detailInfoMode = ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_SECOND_ROW;
												}
											}
											else {
												slf4jlogger.debug("BAD - EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS values - group count inconsistent with pattern");
											}
										}
										else {
											slf4jlogger.debug("Attempting to match mTypeAndOneOrTwoDecimalNumbers");
											Matcher mTypeAndOneOrTwoDecimalNumbers = pTypeAndOneOrTwoDecimalNumbers.matcher(line);
											if (mTypeAndOneOrTwoDecimalNumbers.matches()) {
												slf4jlogger.debug("matches pTypeAndOneOrTwoDecimalNumbers");
												int groupCount = mTypeAndOneOrTwoDecimalNumbers.groupCount();
												slf4jlogger.debug("groupCount = {}",groupCount);
												slf4jlogger.debug("mTypeAndOneOrTwoDecimalNumbers.group(1) = {}",mTypeAndOneOrTwoDecimalNumbers.group(1));
												slf4jlogger.debug("mTypeAndOneOrTwoDecimalNumbers.group(2) = {}",mTypeAndOneOrTwoDecimalNumbers.group(2));
												slf4jlogger.debug("mTypeAndOneOrTwoDecimalNumbers.group(3) = {}",mTypeAndOneOrTwoDecimalNumbers.group(3));
												slf4jlogger.debug("mTypeAndOneOrTwoDecimalNumbers.group(4) = {}",mTypeAndOneOrTwoDecimalNumbers.group(4));
												if (groupCount == 4) {	// not 2 or 3 ... if 2 values, the 3rd and 4th groups will be null; if 3, the 3rd group will be spaces and the 4th will be the 3rd value
													detailValuesStarted = true;
													eventType = mTypeAndOneOrTwoDecimalNumbers.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");
													slf4jlogger.debug("eventType = {}",eventType);
													if (detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_SECOND_ROW) {
														eventSD	= mTypeAndOneOrTwoDecimalNumbers.group(2);
														slf4jlogger.debug("eventSD = {}",eventSD);
														detailValuesComplete = true;
														detailInfoMode = ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
													}
													else if (detailInfoMode == ToshibaDetailInformationMode.SD_CTDIVOLE_DLPE) {
														eventSD	= mTypeAndOneOrTwoDecimalNumbers.group(2);
														slf4jlogger.debug("eventSD = {}",eventSD);
														detailValuesComplete = true;
													}
													else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP) {
														eventTotalmAs = mTypeAndOneOrTwoDecimalNumbers.group(2);
														slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
														eventExposureTime = mTypeAndOneOrTwoDecimalNumbers.group(4);
														slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
														detailValuesComplete = true;
													}
													else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_SD) {
														eventExposureTime = mTypeAndOneOrTwoDecimalNumbers.group(2);
														slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
														eventSD = mTypeAndOneOrTwoDecimalNumbers.group(4);
														slf4jlogger.debug("eventSD = {}",eventSD);
														detailValuesComplete = true;
													}
													else {
														eventTotalmAs = mTypeAndOneOrTwoDecimalNumbers.group(2);
														slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
													}
													if (mTypeAndOneOrTwoDecimalNumbers.group(2) == null && detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_SECOND_ROW) {
														detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
														detailValuesComplete = true;
													}
													else {
														if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS
															|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW
															|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW) {
															eventExposureTime = mTypeAndOneOrTwoDecimalNumbers.group(4);	// NOT 3, since 3 is spaces
															slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
															if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS) {
																detailValuesComplete = true;		// will be last line for single row patterns, following CTDIvol and DLP with units, for example
															}
															else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW
																	 || detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW) {
																detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW;
															}
														}
														else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_SECOND_ROW) {
															eventSD = mTypeAndOneOrTwoDecimalNumbers.group(4);	// NOT 3, since 3 is spaces
															slf4jlogger.debug("eventSD = {}",eventSD);
															detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
															detailValuesComplete = true;
														}
													}
												}
												else {
													slf4jlogger.debug("BAD - mTypeAndOneOrTwoDecimalNumbers values - group count inconsistent with pattern");
												}
											}
											else {
												slf4jlogger.debug("Attempting to match mTypeAndTwoDecimalNumbersWithPhantom");
												Matcher mTypeAndTwoDecimalNumbersWithPhantom = pTypeAndTwoDecimalNumbersWithPhantom.matcher(line);
												if (mTypeAndTwoDecimalNumbersWithPhantom.matches()) {
													slf4jlogger.debug("matches pTypeAndTwoDecimalNumbersWithPhantom");
													int groupCount = mTypeAndTwoDecimalNumbersWithPhantom.groupCount();
													slf4jlogger.debug("groupCount = {}",groupCount);
													slf4jlogger.debug("mTypeAndTwoDecimalNumbersWithPhantom.group(1) = {}",mTypeAndTwoDecimalNumbersWithPhantom.group(1));
													slf4jlogger.debug("mTypeAndTwoDecimalNumbersWithPhantom.group(2) = {}",mTypeAndTwoDecimalNumbersWithPhantom.group(2));
													slf4jlogger.debug("mTypeAndTwoDecimalNumbersWithPhantom.group(3) = {}",mTypeAndTwoDecimalNumbersWithPhantom.group(3));
													slf4jlogger.debug("mTypeAndTwoDecimalNumbersWithPhantom.group(4) = {}",mTypeAndTwoDecimalNumbersWithPhantom.group(4));
													slf4jlogger.debug("mTypeAndTwoDecimalNumbersWithPhantom.group(5) = {}",mTypeAndTwoDecimalNumbersWithPhantom.group(5));
													if (groupCount == 5) {
														detailValuesStarted = true;
														eventType = mTypeAndTwoDecimalNumbersWithPhantom.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");
														slf4jlogger.debug("eventType = {}",eventType);
														eventCTDIvol = mTypeAndTwoDecimalNumbersWithPhantom.group(2);
														slf4jlogger.debug("eventCTDIvol = {}",eventCTDIvol);
														eventCTDIvolPhantom = mTypeAndTwoDecimalNumbersWithPhantom.group(3);
														slf4jlogger.debug("eventCTDIvolPhantom = {}",eventCTDIvolPhantom);
														eventDLP = mTypeAndTwoDecimalNumbersWithPhantom.group(4);
														slf4jlogger.debug("eventDLP = {}",eventDLP);
														eventDLPPhantom = mTypeAndTwoDecimalNumbersWithPhantom.group(5);
														slf4jlogger.debug("eventDLPPhantom = {}",eventDLPPhantom);
														// do NOT set detailValuesComplete = true, since want to process exposure time and total mas on next row
														if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW) {
															detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW;
														}
													}
													else {
														slf4jlogger.debug("BAD - mTypeAndTwoDecimalNumbersWithPhantom values - group count inconsistent with pattern");
													}
												}
												else {
													slf4jlogger.debug("Attempting to match pTwoDecimalNumbers");
													Matcher mTwoDecimalNumbers = pTwoDecimalNumbers.matcher(line);
													if (mTwoDecimalNumbers.matches()) {
														slf4jlogger.debug("matches pTwoDecimalNumbers");
														int groupCount = mTwoDecimalNumbers.groupCount();
														slf4jlogger.debug("groupCount = {}",groupCount);
														if (groupCount == 2) {
															detailValuesStarted = true;
															if (detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW) { // order swapped
																eventExposureTime = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
																eventTotalmAs = mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																// only occurs for scanoscope and will not be a second row so leave detailInfoMode at first row
															}
															else if (detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_SECOND_ROW) {
																eventCTDIvol = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventCTDIvol = {}",eventCTDIvol);
																eventDLP = mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventDLP = {}",eventDLP);
																detailInfoMode = ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW;
																detailValuesComplete = true;
															}
															else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW) {
																eventTotalmAs = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																eventExposureTime = mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
																detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW;
															}
															else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_THIRD_ROW) {
																String eventEffectiveCTDIvolMean = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventEffectiveCTDIvolMean = {}",eventEffectiveCTDIvolMean);
																String eventEffectiveDLP= mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventEffectiveDLP = {}",eventEffectiveDLP);
																detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW;
															}
															else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP) {
																eventTotalmAs = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																eventExposureTime = mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
																detailValuesComplete = true;
															}
															else {
																eventTotalmAs = mTwoDecimalNumbers.group(1);
																slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																eventExposureTime = mTwoDecimalNumbers.group(2);
																slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
															}
															if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS
																|| (eventType != null && eventType.startsWith("SCANOSCOPE"))) {
																detailValuesComplete = true;		// will be last line for single row patterns, following SCANOSCOPE[n], for example, or for SCANOSCOPE[n] even for multi-row (since no 2nd row values)
															}
														}
														else {
															slf4jlogger.debug("BAD - EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS values - group count inconsistent with pattern");
														}
													}
													else {
														slf4jlogger.debug("Attempting to match pTypeAndTwoPositionsAndTwoMoreDecimalNumbers");
														Matcher mTypeAndTwoPositionsAndTwoMoreDecimalNumbers = pTypeAndTwoPositionsAndTwoMoreDecimalNumbers.matcher(line);
														if (detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW
															&& mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.matches()) {
															slf4jlogger.debug("matches pTypeAndTwoPositionsAndTwoMoreDecimalNumbers");
															int groupCount = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.groupCount();
															slf4jlogger.debug("groupCount = {}",groupCount);
															if (groupCount == 5) {
																detailValuesStarted = true;
																eventType = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");	// remove trailing "[2]"
																slf4jlogger.debug("eventType = {}",eventType);
																eventStartPos = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.group(2);
																slf4jlogger.debug("eventStartPos = {}",eventStartPos);
																eventEndPos = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.group(3);
																slf4jlogger.debug("eventEndPos = {}",eventEndPos);
																eventExposureTime = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.group(4);
																slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
																eventTotalmAs = mTypeAndTwoPositionsAndTwoMoreDecimalNumbers.group(5);
																slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
															}
															else {
																slf4jlogger.debug("BAD - START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS values - group count inconsistent with pattern");
															}
															detailInfoMode = ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_SECOND_ROW;
														}
														else {
															
															slf4jlogger.debug("Attempting to match pTwoPositionsAndTwoMoreDecimalNumbers");
															Matcher mTwoPositionsAndTwoMoreDecimalNumbers = pTwoPositionsAndTwoMoreDecimalNumbers.matcher(line);
															if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_SECOND_ROW
																&& mTwoPositionsAndTwoMoreDecimalNumbers.matches()) {
																slf4jlogger.debug("matches pTwoPositionsAndTwoMoreDecimalNumbers");
																int groupCount = mTwoPositionsAndTwoMoreDecimalNumbers.groupCount();
																slf4jlogger.debug("groupCount = {}",groupCount);
																if (groupCount == 4) {
																	detailValuesStarted = true;
																	eventStartPos = mTwoPositionsAndTwoMoreDecimalNumbers.group(1);
																	slf4jlogger.debug("eventStartPos = {}",eventStartPos);
																	eventEndPos = mTwoPositionsAndTwoMoreDecimalNumbers.group(2);
																	slf4jlogger.debug("eventEndPos = {}",eventEndPos);
																	String eventCTDIair = mTwoPositionsAndTwoMoreDecimalNumbers.group(3);
																	slf4jlogger.debug("eventCTDIair = {}",eventCTDIair);
																	String eventDLPair = mTwoPositionsAndTwoMoreDecimalNumbers.group(4);
																	slf4jlogger.debug("eventDLPair = {}",eventDLPair);
																}
																else {
																	slf4jlogger.debug("BAD - START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS values - group count inconsistent with pattern");
																}
																detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_THIRD_ROW;
															}
															else {
																slf4jlogger.debug("Attempting to match pThreeDecimalNumbers");
																Matcher mThreeDecimalNumbers = pThreeDecimalNumbers.matcher(line);
																if (mThreeDecimalNumbers.matches()) {
																	slf4jlogger.debug("matches pThreeDecimalNumbers");
																	int groupCount = mThreeDecimalNumbers.groupCount();
																	slf4jlogger.debug("groupCount = {}",groupCount);
																	if (groupCount == 3) {
																		detailValuesStarted = true;
																		if (detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_SECOND_ROW) {
																			eventCTDIvol = mThreeDecimalNumbers.group(1);
																			slf4jlogger.debug("eventCTDIvol = {}",eventCTDIvol);
																			eventDLP = mThreeDecimalNumbers.group(2);
																			slf4jlogger.debug("eventDLP = {}",eventDLP);
																			detailInfoMode = ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW;
																			detailValuesComplete = true;
																		}
																	}
																	else {
																		slf4jlogger.debug("BAD - EFF_CTDIVOL_MEAN_EFF_DLP_SD values - group count inconsistent with pattern");
																	}
																}
																else {
																	slf4jlogger.debug("Attempting to match pOneDecimalNumber");
																	Matcher mOneDecimalNumber = pOneDecimalNumber.matcher(line);
																	if (mOneDecimalNumber.matches()) {
																		slf4jlogger.debug("matches mOneDecimalNumber");
																		int groupCount = mOneDecimalNumber.groupCount();
																		slf4jlogger.debug("groupCount = {}",groupCount);
																		if (groupCount == 1) {
																			if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW) {
																				detailValuesStarted = true;
																				eventTotalmAs = mOneDecimalNumber.group(1);
																				slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																				detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
																				detailValuesComplete = true;
																			}
																			else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW) {
																				detailValuesStarted = true;
																				eventTotalDoseReduction = mOneDecimalNumber.group(1);
																				slf4jlogger.debug("eventTotalDoseReduction = {}",eventTotalDoseReduction);
																				detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW;
																				detailValuesComplete = true;
																			}
																			else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION) {
																				detailValuesStarted = true;
																				eventTotalDoseReduction = mOneDecimalNumber.group(1);
																				slf4jlogger.debug("eventTotalDoseReduction = {}",eventTotalDoseReduction);
																				detailValuesComplete = true;
																			}
																			else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIFTH_ROW) {
																				detailValuesStarted = true;
																				String eventTotalImageNumber = mOneDecimalNumber.group(1);
																				detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIRST_ROW;
																				slf4jlogger.debug("eventTotalImageNumber = {}",eventTotalImageNumber);
																				detailValuesComplete = true;
																			}
																		}
																		else {
																			slf4jlogger.debug("BAD - mOneDecimalNumber values - group count inconsistent with pattern");
																		}
																	}
																	else {
																		slf4jlogger.debug("Attempting to match pTypeAndFourDecimalNumbersLastTwoWithPhantom");
																		Matcher mTypeAndFourDecimalNumbersLastTwoWithPhantom = pTypeAndFourDecimalNumbersLastTwoWithPhantom.matcher(line);
																		if (mTypeAndFourDecimalNumbersLastTwoWithPhantom.matches()) {
																			slf4jlogger.debug("matches mTypeAndFourDecimalNumbersLastTwoWithPhantom");
																			int groupCount = mTypeAndFourDecimalNumbersLastTwoWithPhantom.groupCount();
																			slf4jlogger.debug("groupCount = {}",groupCount);
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(1) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(1));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(2) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(2));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(3) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(3));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(4) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(4));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(5) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(5));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(6) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(6));
																			slf4jlogger.debug("mTypeAndFourDecimalNumbersLastTwoWithPhantom(7) = {}",mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(7));
																			if (groupCount == 7) {
																				detailValuesStarted = true;
																				eventType = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");
																				slf4jlogger.debug("eventType = {}",eventType);
																				eventTotalmAs = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(2);
																				slf4jlogger.debug("eventTotalmAs = {}",eventTotalmAs);
																				eventExposureTime = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(3);
																				slf4jlogger.debug("eventExposureTime = {}",eventExposureTime);
																				eventCTDIvol = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(4);
																				slf4jlogger.debug("eventCTDIvol = {}",eventCTDIvol);
																				eventCTDIvolPhantom = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(5);
																				slf4jlogger.debug("eventCTDIvolPhantom = {}",eventCTDIvolPhantom);
																				eventDLP = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(6);
																				slf4jlogger.debug("eventDLP = {}",eventDLP);
																				eventDLPPhantom = mTypeAndFourDecimalNumbersLastTwoWithPhantom.group(7);
																				slf4jlogger.debug("eventDLPPhantom = {}",eventDLPPhantom);
																				detailValuesComplete = true;
																			}
																			else {
																				slf4jlogger.debug("BAD - mTypeAndFourDecimalNumbersLastTwoWithPhantom values - group count inconsistent with pattern");
																			}
																		}
																		else {
																			slf4jlogger.debug("Attempting to match pTypeAlone");
																			Matcher mTypeAlone = pTypeAlone.matcher(line);
																			if (mTypeAlone.matches()) {
																				slf4jlogger.debug("matches pTypeAlone");
																				if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FOURTH_ROW) {
																					// isn't type at all, but rather single Boost mode value
																					int groupCount = mTypeAlone.groupCount();
																					slf4jlogger.debug("groupCount = {}",groupCount);
																					if (groupCount == 1) {
																						String eventBoost = mTypeAlone.group(1);
																						slf4jlogger.debug("eventBoost = {}",eventBoost);
																					}
																					else {
																						slf4jlogger.debug("BAD - TYPE ALONE values - group count inconsistent with pattern");
																					}
																					detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIFTH_ROW;
																				}
																				else if (detailInfoMode == ToshibaDetailInformationMode.SD_CTDIVOLE_DLPE && mTypeAlone.groupCount() == 1 && mTypeAlone.group(1).startsWith("SCANOSCOPE")) {
																					detailValuesStarted = false;	// ignore scanoscope in this mode since no information and otherwise gets mixed up with next due to ascenders in CTDIvol and DLP units
																					// could emit scanoscope empty event
																					slf4jlogger.debug("ignoring SCANOSCOPE");
																				}
																				else {
																					if (detailValuesStarted) {
																						if (detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_SECOND_ROW) {
																							// do NOT use existing eventType but use this new one instead (because line split is due to ascending parentheses around phantom type)
																							int groupCount = mTypeAlone.groupCount();
																							slf4jlogger.debug("CTDIVOL_DLP_SD_AWAITING_SECOND_ROW groupCount = {}",groupCount);
																							if (groupCount == 1) {
																								eventType = mTypeAlone.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");	// remove trailing "_CT"
																								slf4jlogger.debug("CTDIVOL_DLP_SD_AWAITING_SECOND_ROW eventType = {}",eventType);
																							}
																							else {
																								slf4jlogger.debug("CTDIVOL_DLP_SD_AWAITING_SECOND_ROW BAD - TYPE ALONE values - group count inconsistent with pattern");
																							}
																						}
																						// may have been missing line before type of next group, so complete previous group ...
																						// record per event values ...
																						slf4jlogger.debug("record per event values before using new type");
																						slf4jlogger.debug("Event:");
																						slf4jlogger.debug("\tprotocol = {}",protocol);
																						slf4jlogger.debug("\teventDLP = {}",eventDLP);
																						slf4jlogger.debug("\teventDLPPhantom = {}",eventDLPPhantom);
																						slf4jlogger.debug("\teventCTDIvol = {}",eventCTDIvol);
																						slf4jlogger.debug("\teventCTDIvolPhantom = {}",eventCTDIvolPhantom);
																						slf4jlogger.debug("\teventType = {}",eventType);
																						slf4jlogger.debug("\teventTotalmAs = {}",eventTotalmAs);
																						slf4jlogger.debug("\teventExposureTime = {}",eventExposureTime);
																						slf4jlogger.debug("\teventTotalDoseReduction = {}",eventTotalDoseReduction);
																						slf4jlogger.debug("\teventDoseReductionMode = {}",eventDoseReductionMode);
																						slf4jlogger.debug("\teventModulation = {}",eventModulation);
																						slf4jlogger.debug("\teventStartPos = {}",eventStartPos);
																						slf4jlogger.debug("\teventEndPos = {}",eventEndPos);
																						slf4jlogger.debug("\teventSD = {}",eventSD);
																						
																						CTScanType recognizedScanType = CTScanType.selectFromDescription(eventType);
																						CTPhantomType recognizedPhantomType = CTPhantomType.selectFromDescription(eventDLPPhantom);		// assume same as eventCTDIvolPhantom, but should check :(
																						ScanRange scanRange = eventStartPos != null && eventEndPos != null ? new ScanRange(eventStartPos,eventEndPos) : null;
																						ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,null/*acquisitionNumber*/,recognizedScanType,scanRange,eventCTDIvol,eventDLP,recognizedPhantomType));
																						
																						detailValuesComplete = false;	// may be another set with no intervening header line, so clear state and old values
																						detailValuesStarted = false;
																						eventDLP = null;
																						eventDLPPhantom = null;
																						eventCTDIvol = null;
																						eventCTDIvolPhantom = null;
																						eventType = null;
																						eventTotalmAs = null;
																						eventExposureTime = null;
																						eventTotalDoseReduction = null;
																						eventDoseReductionMode = null;
																						eventModulation = null;
																						eventStartPos = null;
																						eventEndPos = null;
																						eventSD = null;
																						// reset multiline processing to first line
																						if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW
																							|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW
																							|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW
																							) {
																							detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW;
																						}
																						else if (detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_SECOND_ROW) {
																							detailInfoMode = ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
																						}
																					}
																					int groupCount = mTypeAlone.groupCount();
																					slf4jlogger.debug("groupCount = {}",groupCount);
																					if (groupCount == 1) {
																						//detailValuesStarted = true;	// do NOT start detail values, since may be spurious match like page number, etc.
																						eventType = mTypeAlone.group(1).replaceFirst("[^A-Z].*$","").replaceFirst("DYNAMLC","DYNAMIC").replaceFirst("HELLCAL","HELICAL");	// remove trailing "_CT"
																						slf4jlogger.debug("eventType = {}",eventType);
																					}
																					else {
																						slf4jlogger.debug("BAD - TYPE ALONE values - group count inconsistent with pattern");
																					}
																				}
																			}
																			else {
																				Matcher mDoseReductionDoseReductionModeModulation = pDoseReductionDoseReductionModeModulation.matcher(line);
																				if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION
																					&& mDoseReductionDoseReductionModeModulation.matches()) {
																					slf4jlogger.debug("matches mDoseReductionDoseReductionModeModulation");
																					int groupCount = mDoseReductionDoseReductionModeModulation.groupCount();
																					slf4jlogger.debug("groupCount = {}",groupCount);
																					if (groupCount == 3) {
																						detailValuesStarted = true;
																						eventTotalDoseReduction = mDoseReductionDoseReductionModeModulation.group(1);
																						slf4jlogger.debug("eventTotalDoseReduction = {}",eventTotalDoseReduction);
																						eventDoseReductionMode = mDoseReductionDoseReductionModeModulation.group(2);
																						slf4jlogger.debug("eventDoseReductionMode = {}",eventDoseReductionMode);
																						eventModulation = mDoseReductionDoseReductionModeModulation.group(3);
																						slf4jlogger.debug("eventModulation = {}",eventModulation);
																						detailValuesComplete = true;
																					}
																					else {
																						slf4jlogger.debug("BAD - DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION values - group count inconsistent with pattern");
																					}
																				}
																				else if (line.matches(rProtocolLine)) {
																					slf4jlogger.debug("matches rProtocolLine");
																					if (detailValuesStarted) {
																						// may have been missing line before type of next group, so complete previous group ...
																						// record per event values ...
																						slf4jlogger.debug("record per event values before using new protocol");
																						slf4jlogger.debug("Event:");
																						slf4jlogger.debug("\tprotocol = {}",protocol);
																						slf4jlogger.debug("\teventDLP = {}",eventDLP);
																						slf4jlogger.debug("\teventDLPPhantom = {}",eventDLPPhantom);
																						slf4jlogger.debug("\teventCTDIvol = {}",eventCTDIvol);
																						slf4jlogger.debug("\teventCTDIvolPhantom = {}",eventCTDIvolPhantom);
																						slf4jlogger.debug("\teventType = {}",eventType);
																						slf4jlogger.debug("\teventTotalmAs = {}",eventTotalmAs);
																						slf4jlogger.debug("\teventExposureTime = {}",eventExposureTime);
																						slf4jlogger.debug("\teventTotalDoseReduction = {}",eventTotalDoseReduction);
																						slf4jlogger.debug("\teventDoseReductionMode = {}",eventDoseReductionMode);
																						slf4jlogger.debug("\teventModulation = {}",eventModulation);
																						slf4jlogger.debug("\teventStartPos = {}",eventStartPos);
																						slf4jlogger.debug("\teventEndPos = {}",eventEndPos);
																						slf4jlogger.debug("\teventSD = {}",eventSD);
																						
																						CTScanType recognizedScanType = CTScanType.selectFromDescription(eventType);
																						CTPhantomType recognizedPhantomType = CTPhantomType.selectFromDescription(eventDLPPhantom);		// assume same as eventCTDIvolPhantom, but should check :(
																						ScanRange scanRange = eventStartPos != null && eventEndPos != null ? new ScanRange(eventStartPos,eventEndPos) : null;
																						ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,null/*acquisitionNumber*/,recognizedScanType,scanRange,eventCTDIvol,eventDLP,recognizedPhantomType));
																						
																						detailValuesComplete = false;	// may be another set with no intervening header line, so clear state and old values
																						detailValuesStarted = false;
																						eventDLP = null;
																						eventDLPPhantom = null;
																						eventCTDIvol = null;
																						eventCTDIvolPhantom = null;
																						eventType = null;
																						eventTotalmAs = null;
																						eventExposureTime = null;
																						eventTotalDoseReduction = null;
																						eventDoseReductionMode = null;
																						eventModulation = null;
																						eventStartPos = null;
																						eventEndPos = null;
																						eventSD = null;
																						// reset multiline processing to first line
																						if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_PART_OF_FIRST_ROW
																							|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_SECOND_ROW
																							|| detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_THIRD_ROW
																							) {
																							detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW;
																						}
																					}
																					protocol = line.trim().replaceAll("[.]I","I").replaceFirst("BML","BMI");
																					slf4jlogger.debug("protocol = {}",protocol);
																					detailInfoMode = ToshibaDetailInformationMode.UNRECOGNIZED;		// there will be a new header after each protocol
																				}
																				else {
																					slf4jlogger.debug("BAD - unrecognized line type");
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
							if (detailValuesComplete) {
								slf4jlogger.debug("detailValuesComplete");
								// record per event values ...
								
								slf4jlogger.debug("Event:");
								slf4jlogger.debug("\tprotocol = {}",protocol);
								slf4jlogger.debug("\teventDLP = {}",eventDLP);
								slf4jlogger.debug("\teventDLPPhantom = {}",eventDLPPhantom);
								slf4jlogger.debug("\teventCTDIvol = {}",eventCTDIvol);
								slf4jlogger.debug("\teventCTDIvolPhantom = {}",eventCTDIvolPhantom);
								slf4jlogger.debug("\teventType = {}",eventType);
								slf4jlogger.debug("\teventTotalmAs = {}",eventTotalmAs);
								slf4jlogger.debug("\teventExposureTime = {}",eventExposureTime);
								slf4jlogger.debug("\teventTotalDoseReduction = {}",eventTotalDoseReduction);
								slf4jlogger.debug("\teventDoseReductionMode = {}",eventDoseReductionMode);
								slf4jlogger.debug("\teventModulation = {}",eventModulation);
								slf4jlogger.debug("\teventStartPos = {}",eventStartPos);
								slf4jlogger.debug("\teventEndPos = {}",eventEndPos);
								slf4jlogger.debug("\teventSD = {}",eventSD);
								
								CTScanType recognizedScanType = CTScanType.selectFromDescription(eventType);
								CTPhantomType recognizedPhantomType = CTPhantomType.selectFromDescription(eventDLPPhantom);		// assume same as eventCTDIvolPhantom, but should check :(
								ScanRange scanRange = eventStartPos != null && eventEndPos != null ? new ScanRange(eventStartPos,eventEndPos) : null;
								ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,null/*acquisitionNumber*/,recognizedScanType,scanRange,eventCTDIvol,eventDLP,recognizedPhantomType));
								
								detailValuesComplete = false;	// may be another set with no intervening header line, so clear state and old values
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
						}
						else {
							if (line.matches(rExposureTimeCTDIvolDLPWithUnits)) {
								slf4jlogger.debug("matches rExposureTimeCTDIvolDLPWithUnits setting detailInfoMode EXPOSURE_TIME_CTDIVOL_DLP_AWAITING_TOTAL_MAS");
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_AWAITING_TOTAL_MAS;
							}
							else if (line.matches(rTotalmAsExposureTimeCTDIvolDLPWithoutUnits)) {
								slf4jlogger.debug("matches rTotalmAsExposureTimeCTDIvolDLPWithoutUnits");
								// hmmm ... using the order specified in the title even though previously it was a different order ??? need to regression teat what cam before testocrtoshibaanothermultipage_bodyonly :(
								//detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS;
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_AWAITING_TOTAL_MAS && line.matches(rTotalmAs)) {
								slf4jlogger.debug("EXPOSURE_TIME_CTDIVOL_DLP_AWAITING_TOTAL_MAS and matches rTotalmAs setting detailInfoMode EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS");
								// hmmm ... why is the order different from the order specified in the title? factored out when doing testocrtoshibaanothermultipage_bodyonly :(
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS;
								//detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rExposureTimeCTDIvolDLPTotalmAs)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rExposureTimeCTDIvolDLPSD)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_SD;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rTotalmAsExposureTimeCTDIvolDLP)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOL_DLP;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rTotalmAsExposureTimeCTDIvolDLPWithESuffix)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_AWAITING_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_AWAITING_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION && line.matches(rTotalDoseReductionDoseReductionModeModulation)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_AWAITING_TOTAL_DOSE_REDUCTION_DOSE_REDUCTION_MODE_MODULATION && line.matches(rTotalDoseReductionOnly)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_EXPOSURE_TIME_CTDIVOLE_DLPE_TOTAL_DOSE_REDUCTION;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rStartPosEndPosExposureTimeTotalmAs)) {
								detailInfoMode = ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_SD;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_SD && line.matches(rEffCTDIvolMeanEffDLPSD)) {
								detailInfoMode = ToshibaDetailInformationMode.START_POS_END_POS_EXPOSURE_TIME_TOTAL_MAS_EFF_CTDIVOL_MEAN_EFF_DLP_SD_AWAITING_FIRST_ROW;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rCTDIvolDLPWithUnits)) {
								detailInfoMode = ToshibaDetailInformationMode.CTDIVOL_DLP_AWAITING_TOTAL_MAS_SD;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS && line.matches(rEffDLPWithUnits)) {
								// already valid mode but turns out to be multiline after all
								// detail values already reset to null
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_AWAITING_DOSE_RED_MODE_START_POS_END_POS;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_AWAITING_DOSE_RED_MODE_START_POS_END_POS && line.matches(rDoseRedModStartPosEndPos)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_AWAITING_TOT_DOSE_RED;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_AWAITING_TOT_DOSE_RED && line.matches(rTotDoseRed)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_EFF_DLP_DOSE_RED_MODE_START_POS_END_POS_TOT_DOSE_RED_AWAITING_FIRST_ROW;
							}
							
							
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS && line.matches(rStartPosEndPosCTDIairDLPair)) {
								// already valid mode but turns out to be multiline after all
								// detail values already reset to null
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_AWAITING_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD && line.matches(rEffCTDIvolMeanEffDLPModulationSD)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_AWAITING_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_AWAITING_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED && line.matches(rBoostQDSDoseReductionModeTotalDoseReduction)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_AWAITING_TOTAL_IMAGE_NUMBER;
							}
							else if (detailInfoMode == ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_AWAITING_TOTAL_IMAGE_NUMBER && line.matches(rTotalImageNumber)) {
								detailInfoMode = ToshibaDetailInformationMode.EXPOSURE_TIME_CTDIVOL_DLP_TOTAL_MAS_START_POS_END_POS_CTDIAIR_DLPAIR_EFF_CTDIVOL_MEAN_EFF_DLP_MODULATION_SD_BOOST_QDS_DOSE_RED_MODE_TOT_DOSE_RED_TOTAL_IMAGE_NUMBER_AWAITING_FIRST_ROW;
							}
							
							else if (detailInfoMode == ToshibaDetailInformationMode.CTDIVOL_DLP_AWAITING_TOTAL_MAS_SD && line.matches(rTotalmAsSD)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rTotalmAsCTDIvolDLPWithoutUnitsSD)) {
								detailInfoMode = ToshibaDetailInformationMode.TOTAL_MAS_CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rSDCTDIvolEDLPE)) {
								detailInfoMode = ToshibaDetailInformationMode.SD_CTDIVOLE_DLPE;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rCTDIvolDLPSD)) {
								detailInfoMode = ToshibaDetailInformationMode.CTDIVOL_DLP_SD_AWAITING_FIRST_ROW;
								detailValuesComplete = false;
								detailValuesStarted = false;
								eventDLP = null;
								eventDLPPhantom = null;
								eventCTDIvol = null;
								eventCTDIvolPhantom = null;
								eventType = null;
								eventTotalmAs = null;
								eventExposureTime = null;
								eventTotalDoseReduction = null;
								eventDoseReductionMode = null;
								eventModulation = null;
								eventStartPos = null;
								eventEndPos = null;
								eventSD = null;
							}
							else if (line.matches(rProtocolLine)) {
								detailInfoMode = ToshibaDetailInformationMode.UNRECOGNIZED;		// reset every line
								protocol = line.trim().replaceAll("[.]I","I").replaceFirst("BML","BMI");
								slf4jlogger.debug("protocol = {}",protocol);
							}
							else {
								detailInfoMode = ToshibaDetailInformationMode.UNRECOGNIZED;		// reset every line
							}
							slf4jlogger.debug("detailInfoMode now set after trying to match multi-line or ascender-split patterns to {}",detailInfoMode);
						}
					}
					else if (majorMode == ToshibaLineRecognizerMode.UNRECOGNIZED) {
						// may be single page with arg : value lines
						slf4jlogger.debug("checking for mTotalDLPOnly");
						Matcher mTotalDLPOnly = pTotalDLPOnly.matcher(line);
						if (mTotalDLPOnly.matches()) {
							slf4jlogger.debug("matches mTotalDLPOnly");
							int groupCount = mTotalDLPOnly.groupCount();
							slf4jlogger.debug("groupCount = {}",groupCount);
							if (groupCount == 2) {
								String totalDLP = mTotalDLPOnly.group(2);	// 1st is used for .. vs. :
								slf4jlogger.debug("totalDLP = {}",totalDLP);
								ctDose.setDLPTotal(totalDLP);
							}
							else {
								slf4jlogger.debug("BAD - TotalDLPOnly values - group count inconsistent with pattern");
							}
						}
					}
				}
			}
			if (detailValuesStarted && !detailValuesComplete) {
				// i.e., truncated; have seen multiline pages where last dose reduction line is missing but DLP etc., present
				slf4jlogger.debug("Event:");
				slf4jlogger.debug("\tprotocol = {}",protocol);
				slf4jlogger.debug("\teventDLP = {}",eventDLP);
				slf4jlogger.debug("\teventDLPPhantom = {}",eventDLPPhantom);
				slf4jlogger.debug("\teventCTDIvol = {}",eventCTDIvol);
				slf4jlogger.debug("\teventCTDIvolPhantom = {}",eventCTDIvolPhantom);
				slf4jlogger.debug("\teventType = {}",eventType);
				slf4jlogger.debug("\teventTotalmAs = {}",eventTotalmAs);
				slf4jlogger.debug("\teventExposureTime = {}",eventExposureTime);
				slf4jlogger.debug("\teventTotalDoseReduction = {}",eventTotalDoseReduction);
				slf4jlogger.debug("\teventDoseReductionMode = {}",eventDoseReductionMode);
				slf4jlogger.debug("\teventModulation = {}",eventModulation);
				slf4jlogger.debug("\teventStartPos = {}",eventStartPos);
				slf4jlogger.debug("\teventEndPos = {}",eventEndPos);
				slf4jlogger.debug("\teventSD = {}",eventSD);
				
				CTScanType recognizedScanType = CTScanType.selectFromDescription(eventType);
				CTPhantomType recognizedPhantomType = CTPhantomType.selectFromDescription(eventDLPPhantom);		// assume same as eventCTDIvolPhantom, but should check :(
				ScanRange scanRange = eventStartPos != null && eventEndPos != null ? new ScanRange(eventStartPos,eventEndPos) : null;
				ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,null/*acquisitionNumber*/,recognizedScanType,scanRange,eventCTDIvol,eventDLP,recognizedPhantomType));
			}
		}
		if (eventDataFromImages != null) {
			for (int ai = 0; ai<ctDose.getNumberOfAcquisitions(); ++ai) {
				CTDoseAcquisition acq = ctDose.getAcquisition(ai);
				if (acq != null) {
					//...
				}
			}
		}
		
		if (buildSR) {
			GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(list,ctDose,eventDataFromImages);
		}
		
		return ctDose;
	}

	/**
	 * <p>Create a CTDose instance from the OCR already performed on a dose screen image.</p>
	 *
	 * @deprecated							SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getCTDoseFromOCROfDoseScreen(OCR,CTIrradiationEventDataFromImages,boolean)} instead.
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	debugLevel					ignored
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfDoseScreen(OCR ocr,int debugLevel,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		slf4jlogger.warn("Debug level supplied as argument to constructor ignored");
		return getCTDoseFromOCROfDoseScreen(ocr,eventDataFromImages,buildSR);
	}
	
	/**
	 * <p>Create a CTDose instance from the OCR already performed on a dose screen image.</p>
	 *
	 * <p>Also extracts start and end date and time and default anatomical information.</p>
	 *
	 * @param	ocr							the object from the OCR performed in the image
	 * @param	eventDataFromImages			the pre event data from the reconstructed images
	 * @param	buildSR						whether or not to extract composite context from the list for use later to build an RDSR
	 */
	public static CTDose getCTDoseFromOCROfDoseScreen(OCR ocr,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		CTDose ctDose = null;
		AttributeList list = ocr.getCommonAttributeList();
		//System.err.print("OCR.getCTDoseFromOCROfDoseScreen(): list =\n"+list);
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String startDateTime =null;
		String endDateTime = null;
		DisplayableConcept defaultAnatomyFromEventData = null;
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): eventDataFromImages=\n"+eventDataFromImages);
		if (eventDataFromImages != null) {
			startDateTime = eventDataFromImages.getOverallEarliestAcquisitionDateTimeForStudy(studyInstanceUID);
			endDateTime = eventDataFromImages.getOverallLatestAcquisitionDateTimeForStudy(studyInstanceUID);
			defaultAnatomyFromEventData = eventDataFromImages.getCombinedAnatomyForStudy(studyInstanceUID);	 // which may be from events that cannot be matched to acquisitions but are same study
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): defaultAnatomyFromEventData "+defaultAnatomyFromEventData);
		}
		if (ocr.isGEDoseScreenInstance(list)) {
			ctDose = getCTDoseFromOCROfGEDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
		}
		else if (ocr.isSiemensDoseScreenInstance(list)) {
			ctDose = getCTDoseFromOCROfSiemensDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
		}
		else if (ocr.isToshibaDoseScreenInstance(list)) {
			ctDose = getCTDoseFromOCROfToshibaDoseScreen(ocr,startDateTime,endDateTime,eventDataFromImages,buildSR);
		}
		// set anatomy for use when no more specific per-acquisition information available
		if (ctDose != null) {
			DisplayableConcept defaultAnatomy = null;
			DisplayableConcept defaultAnatomyFromCommonAttributeList = CTAnatomy.findAnatomicConcept(list);
			if (defaultAnatomyFromCommonAttributeList != null) {
				if (defaultAnatomyFromEventData != null) {
					DisplayableConcept combined = CombinedAnatomicConcepts.getCombinedConcept(defaultAnatomyFromCommonAttributeList,defaultAnatomyFromEventData,CTAnatomy.getAnatomyConcepts());
					if (combined != null) {
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): Setting default anatomy from combined screen common attribute list "+defaultAnatomyFromCommonAttributeList+"and event data "+defaultAnatomyFromEventData+" to "+combined);
						defaultAnatomy = combined;
					}
					else {
						// both present but cannot combined; use screen common attribute list by preference since not all event data may be present
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): Setting default anatomy from screen common attribute list "+defaultAnatomyFromCommonAttributeList+"in preference to event data "+defaultAnatomyFromEventData+" since cannot combine");
						defaultAnatomy = defaultAnatomyFromCommonAttributeList;
					}
				}
				else {
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): Setting default anatomy from screen common attribute list only to "+defaultAnatomyFromCommonAttributeList);
					defaultAnatomy = defaultAnatomyFromCommonAttributeList;
				}
			}
			else if (defaultAnatomyFromEventData != null) {
//System.err.println("OCR.getCTDoseFromOCROfDoseScreen(): Setting default anatomy from event data to "+defaultAnatomyFromCommonAttributeList);
				defaultAnatomy = defaultAnatomyFromEventData;
			}
			ctDose.setDefaultAnatomy(defaultAnatomy);
		}
		return ctDose;
	}
	
	/**
	 * <p>Extract the CT dose information in a screen save image using optical character recognition, correlate it with any acquired CT slice images.</p>
	 *
	 * @param	arg		an array of 1 to 6 strings - the path to a dose screen save image or folder of screens (or "-" if to search for dose screens amongst acquired images),
	 *					then optionally the path to a DICOMDIR or folder containing acquired CT slice images (or "-" if none and more arguments)
	 *					then optionally the name of Dose SR file to write  (or "-" if none and more arguments)
	 *					then optionally the file containing the text glyphs to use during recognition rather than the default (or "-" if none and more arguments),
	 *					then optionally the name of a file to write any newly trained glyphs to
	 */
	public static final void main(String arg[]) {
		try {
			String screenFilesPath           = arg.length > 0 && !arg[0].equals("-") ? arg[0] : null;
			String acquiredImagesPath        = arg.length > 1 && !arg[1].equals("-") ? arg[1] : null;
			String srOutputFilename          = arg.length > 2 && !arg[2].equals("-") ? arg[2] : null;
			String fileNameOfKnownGlyphs     = arg.length > 3 && !arg[3].equals("-") ? arg[3] : defaultFileNameOfKnownGlyphs;
			String fileNameToRecordNewGlyphs = arg.length > 4 && !arg[4].equals("-") ? arg[4] : null;
			
			String startDateTime = null;
			String endDateTime = null;
			CTIrradiationEventDataFromImages eventDataFromImages = null;
			OCR ocr = null;
			if (acquiredImagesPath == null) {
				ocr = new OCR(screenFilesPath,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
			}
			else {
				eventDataFromImages = new CTIrradiationEventDataFromImages(acquiredImagesPath);
				slf4jlogger.info(eventDataFromImages.toString());
				if (screenFilesPath == null) {
					List<String> screenFilesPaths = eventDataFromImages.getDoseScreenOrStructuredReportFilenames(true/*includeScreen*/,false/*includeSR*/);
					if (screenFilesPaths.isEmpty()) {
						slf4jlogger.error("############ No dose screen files found");
					}
					else {
						ocr = new OCR(screenFilesPaths,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
					}
				}
				else {
					ocr = new OCR(screenFilesPath,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs);
				}
			}
			if (ocr != null) {
				slf4jlogger.debug(ocr.toString());
				CTDose ctDose = getCTDoseFromOCROfDoseScreen(ocr,eventDataFromImages,srOutputFilename != null);
				if (ctDose != null) {
					slf4jlogger.info("\n{}",ctDose.toString(true,true));
					if (!ctDose.specifiedDLPTotalMatchesDLPTotalFromAcquisitions()) {
						slf4jlogger.warn("############ specified DLP total ({}) does not match DLP total from acquisitions ({})",ctDose.getDLPTotal(),ctDose.getDLPTotalFromAcquisitions());
					}
					
					if (srOutputFilename != null) {
						ctDose.write(srOutputFilename,null,OCR.class.getCanonicalName());
					}
				}
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}


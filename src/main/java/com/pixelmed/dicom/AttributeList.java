/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

//import com.pixelmed.utils.CapabilitiesAvailable;		// for dumpListOfAllAvailableReaders() during debugging

import java.util.*;
import java.text.NumberFormat;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.spi.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.*; 

import java.util.zip.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.dicom.AttributeList AttributeList} class maintains a list of individual DICOM attributes.</p>
 *
 * <p>Instances of the class may be used for entire composite storage SOP instances, or fragments of such instances
 * such as meta information headers, or simply as lists of attributes to be passed to other
 * methods (e.g. lists of attributes to add or remove from another list).</p>
 *
 * <p>The class is actually implemented by extending {@link java.util.TreeMap java.util.TreeMap}
 * as a map of {@link com.pixelmed.dicom.AttributeTag AttributeTag} keys to
 * {@link com.pixelmed.dicom.Attribute Attribute} values. Consequently, all the methods
 * of the underlying collection are available, including adding key-value pairs and
 * extracting values by key. Iteration through the list of key-value pairs in
 * the map is also supported, and the iterator returns values in the ascending numerical
 * order of the {@link com.pixelmed.dicom.AttributeTag AttributeTag} keys, since
 * that is how {@link com.pixelmed.dicom.AttributeTag AttributeTag} implements
 * {@link java.lang.Comparable Comparable}.</p>
 *
 * <p>Note that large attribute values such as Pixel Data may be left on disk rather
 * than actually read in when the list is created, and loaded on demand; extreme
 * caution should be taken if the underlying file from which an AttributeList has
 * been read is moved or renamed; a specific method, {@link #setFileUsedByOnDiskAttributes(File file) setFileUsedByOnDiskAttributes()},
 * is provided to address this concern.</p>
 *
 * <p>By default, compressed pixel data is decompressed during reading; this behavior can be controlled by calling setDecompressPixelData() before reading.</p>
 *
 * <p>The class provides methods for reading entire objects as a list of attributes,
 * from files or streams. For example, the following fragment will read an entire
 * object from the specified file and dump the contents of the attribute list:</p>
 *
 * <pre>
 * 	AttributeList list = new AttributeList();
 * 	list.read(arg[0]);
 * 	System.err.print(list);
 * </pre>
 *
 * <p>Similarly, methods are provided for writing entire objects. For example, the
 * previous fragment could be extended to write the list to a file unchanged as follows:</p>
 *
 * <pre>
 *	list.write(arg[1],TransferSyntax.ExplicitVRLittleEndian,true,true);
 * </pre>
 *
 *<p>Note that in general, one would want to perform significantly more cleaning
 * up before writing an object that has just been read, and a number of such
 * methods are provided either in this class or on related classes
 * as illustrated in this example:</p>
 *
 * <pre>
 * 	AttributeList list = new AttributeList();
 * 	list.read(arg[0]);
 *	//list.removePrivateAttributes();
 *	list.removeGroupLengthAttributes();
 *	list.removeMetaInformationHeaderAttributes();
 *	list.remove(TagFromName.DataSetTrailingPadding);
 *	list.correctDecompressedImagePixelModule();
 *	list.insertLossyImageCompressionHistoryIfDecompressed();
 *  CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
 *	FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,"OURAETITLE");
 *	list.write(arg[1],TransferSyntax.ExplicitVRLittleEndian,true,true);
 * </pre>
 *
 * <p>Note that this example is essentially the functionality of the {@link #main(String[]) main()} method
 * of this class, which may be used as a copying utility when invoked with input and output file arguments.</p>
 *
 * <p>Correction of the PhotometricInterpretation and related attributes by an explicit call to the
 * {@link #correctDecompressedImagePixelModule() correctDecompressedImagePixelModule()}
 * method is necessary if the color space of a compressed input
 * transfer syntax was changed during decompression (e.g., from YBR_FULL_422 for JPEG lossy to RGB),
 * since the PixelData is always decompressed during reading by the {@link #read(String) read()} method and its ilk;
 * the call does nothing (is harmless) if the input was not compressed or not multi-component.</p>
 *
 * <p>Individual attributes can be added or deleted as desired, either using a newly created
 * list or one which has been read in from an existing object. For example, to zero out the
 * patient's name one might do something like the following:</p>
 *
 * <pre>
 *	list.replaceWithZeroLengthIfPresent(TagFromName.PatientName);
 * </pre>
 *
 * <p> or to replace it with a particular value one might do the following:</p>
 * <pre>
 *	Attribute a = new PersonNameAttribute(TagFromName.PatientName);
 *	a.addValue(value);
 *	list.put(TagFromName.PatientName,a);		// one could list.remove(TagFromName.PatientName) first, but this is implicit in the put
 * </pre>
 *
 * <p>A more compact shorthand method for adding new (or replacing existing) attributes (if they are in the dictionary so that the VR can be determined) is also supplied:</p>
 *
 * <pre>
 *	list.putNewAttribute(TagFromName.PatientName);
 * </pre>
 *
 * <p>and if a specific character set other than the default is in use:</p>
 *
 * <pre>
 *	list.putNewAttribute(TagFromName.PatientName,specificCharacterSet);
 * </pre>
 *
 * <p>and since this method returns the generated attribute, values can easily be added as:</p>
 *
 * <pre>
 *	list.putNewAttribute(TagFromName.PatientName,specificCharacterSet).addValue("Blo^Joe");
 * </pre>
 *
 *
 * <p>Note also that the {@link com.pixelmed.dicom.Attribute Attribute} class provides some useful
 * static methods for extracting and manipulating individual attributes within a list. For example:</p>
 *
 * <pre>
 *	String patientName=Attribute.getSingleStringValueOrNull(list,TagFromName.PatientName);
 * </pre>
 *
 * <p>Ideally one should take care when adding or manipulating lists of attributes to handle
 * the specific character set correctly and consistently when there is a possibility that it
 * may be other than the default. The previous example of replacing the patient's name
 * could be more properly rewritten as:</p>
 *
 * <pre>
 *	SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet(Attribute.getStringValues(list,TagFromName.SpecificCharacterSet));
 *	Attribute a = new PersonNameAttribute(TagFromName.PatientName,specificCharacterSet);
 *	a.addValue(value);
 *	list.put(TagFromName.PatientName,a);
 * </pre>
 *
 * <p>Note that in this example if the SpecificCharacterSet attribute were not found or was present but empty
 * the various intervening methods would return null and the
 * {@link com.pixelmed.dicom.SpecificCharacterSet#SpecificCharacterSet(String[]) SpecificCharacterSet()}
 * constructor would use the default (ascii) character set.</p>
 *
 * <p>When an attribute list is read in, the SpecificCharacterSet attribute is automatically detected
 * and set and applied to all string attributes as they are read in and converted to the internal
 * string form which is used by Java (Unicode).</p>
 *
 * <p>The same applies when they are written, with some limitations on which character sets are supported. However,
 * if new attributes have been added to the list with a different SpecificCharacterSet, it is necessary to call
 * {@link #insertSuitableSpecificCharacterSetForAllStringValues() insertSuitableSpecificCharacterSetForAllStringValues()} before writing, which will check all string values
 * of all attributes affected by SpecificCharacterSet, choose a suitable new SpecificCharacterSet, and
 * insert or replace the existing SpecificCharacterSet attribute. By the time that the list is written out,
 * all of the Attributes must have the same SpecificCharacterSet.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeTag
 * @see com.pixelmed.dicom.FileMetaInformation
 * @see com.pixelmed.dicom.SpecificCharacterSet
 * @see com.pixelmed.dicom.TagFromName
 * @see com.pixelmed.dicom.TransferSyntax
 *
 * @author	dclunie
 */
public class AttributeList extends TreeMap<AttributeTag,Attribute> {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeList.java,v 1.182 2017/01/24 10:50:35 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AttributeList.class);

	private static final long badAttributeLimit = 10;	// pretty arbitrary, but avoids very long wait on large non-DICOM files, and still permits the occasional bad ones
	
	private static final long maximumLongVRValueLengthThatCanBeAllocated = Integer.MAX_VALUE;	// Java arrays are index by int and hence are limited in size :(
	
	public static final long maximumShortVRValueLength = 65535l;
	
	private static final long maximumSaneFixedValueLengthWhenRecoveringFromIncorrectImplicitVRElementEncodinginExplicitVR = 100000l; // 100 kB seems large enough, but is an arbitrary choice
	
	/**
	 * <p>Check that the value length is reasonable under the circumstances.</p>
	 *
	 * <p>Used to avoid trying to allocate insanely large attribute values when parsing corrupt or non-DICOM datasets.</p>
	 *
	 * <p>Protected so that this can be overridden in sub-classes if necessary for a particular application.</p>
	 *
	 * @param	vl	the value length
	 * @param	vr	the value representation
	 * @param	tag	the tag
	 * @param	encounteredIncorrectImplicitVRElementEncodinginExplicitVR	which increases the likelihood of an insane value
	 * @throws	DicomException	if the value length is not reasonable
	 */
	protected void checkSanityOfValueLength(long vl,byte vr[],AttributeTag tag,boolean encounteredIncorrectImplicitVRElementEncodinginExplicitVR) throws DicomException {
//System.err.println("AttributeList.checkSanityOfValueLength(): vr = "+ValueRepresentation.getAsString(vr)+" vl = "+vl);
		if (vl < 0) {
			throw new DicomException("Illegal fixed VL ("+vl+" dec, 0x"+Long.toHexString(vl)+") - is negative - probably incorrect dataset - giving up");
		}
		if (vl > maximumLongVRValueLengthThatCanBeAllocated && ValueRepresentation.isUnknownVR(vr)) {
			throw new DicomException("Illegal fixed VL ("+vl+" dec, 0x"+Long.toHexString(vl)+") - is larger than can be allocated for unknown VR - probably incorrect dataset - giving up");
		}
		// logic here is that a short VL VR should never have a VL greater than can be sent in explicit VR (2^16-1 == 65535),
		// with the exception of RT DVH (DS) that sometimes must be sent as implicit VR (Mathews, Bosch 2006 Phys. Med. Biol. 51 L11 doi:10.1088/0031-9155/51/5/L01)
		// also allow it in Histogram Data (encountered big one in Adani MG For Proc
		// also allow it in RT Plan Compensator Transmission Data (from Irrer)
		// also allow it in Contour Data (from Gavin Disney)
		if (vl > maximumShortVRValueLength && ValueRepresentation.isShortValueLengthVR(vr)) {
			if (!tag.equals(TagFromName.DVHData)
			 && !tag.equals(TagFromName.CompensatorTransmissionData)
			 && !tag.equals(TagFromName.CompensatorThicknessData)
			 && !tag.equals(TagFromName.BlockData)
			 && !tag.equals(TagFromName.ContourData)
			 && !tag.equals(TagFromName.HistogramData)
			 && !tag.equals(TagFromName.TableOfYBreakPoints)		// not sure if this could really get that large, but used in TestDicomStreamCopier_ConvertTransferSyntaxes !
			) {
				throw new DicomException("Unlikely fixed VL ("+vl+" dec, 0x"+Long.toHexString(vl)+") for non-bulk data tag - probably incorrect dataset - giving up");
			}
			else {
				slf4jlogger.warn("Allowed fixed VL ({} dec, 0x{}) that is too long to fit in 16 bits for VR {} in Explicit VR Transfer Syntax because it is one of the recognized data elements for which that is often the case ({} {})",vl,Long.toHexString(vl),ValueRepresentation.getAsString(vr),dictionary.getNameFromTag(tag),tag);
			}
		}
		if (encounteredIncorrectImplicitVRElementEncodinginExplicitVR && vl > maximumSaneFixedValueLengthWhenRecoveringFromIncorrectImplicitVRElementEncodinginExplicitVR) {
			throw new DicomException("Unlikely fixed VL ("+vl+" dec, 0x"+Long.toHexString(vl)+") when recovering from incorrect Implicit VR element encoding in Explicit VR Transfer Syntax - giving up");
		}
		// otherwise can be as large as it needs to be
	}
	
	protected boolean decompressPixelData = true;	// NOT static ... must be explicitly set to false for each instance constructed !

	/**
	 * <p>Change the pixel data decompression behavior if encapsulated pixel data is encountered.</p>
	 *
	 * <p>Encapsulated pixel data is any PixelData element with an undefined VL.</p>
	 *
	 * <p>Default for each newly constructed AttributeList is to decompress.</p>
	 *
	 * <p>If decompression is deferred, take care not to remove the Transfer Syntax from the AttributeList
	 * if classes perform decompression based on the list contents e.g., SourceImage created from AttributeList.</p>
	 *
	 * @param	decompressPixelData	whether or not to decompress the pixel data
	 */
	public void setDecompressPixelData(boolean decompressPixelData) {
		this.decompressPixelData = decompressPixelData;
	}

	/***/
	protected static DicomDictionary dictionary;
	
	/***/
	protected static void createDictionaryifNecessary() {
		if (dictionary == null) {
//System.err.println("AttributeList.createDictionaryifNecessary(): creating static dictionary");
			dictionary = new DicomDictionary();
		}
	}

	/**
	 *
	 */
	public boolean equals(Object o) {
//System.err.println("AttributeList.equals():");
		if (o instanceof AttributeList) {
			AttributeList olist = (AttributeList)o;
			if (size() == olist.size()) {
				Iterator<Attribute> i = values().iterator();
				while (i.hasNext()) {
					Attribute a = i.next();
					Attribute oa = olist.get(a.getTag());
					// ideally would have Attribute.equals() available to us, but don't at this time :(
					String as = a.getDelimitedStringValuesOrDefault("").trim();		// otherwise trailing spaces may cause mismatch, e.g., padded "DCM " versus unpadded "DCM"
					String oas = oa.getDelimitedStringValuesOrDefault("").trim();
//System.err.println("AttributeList.equals(): comparing trimmed string "+as);
//System.err.println("AttributeList.equals():      with trimmed string "+oas);
					if (!as.equals(oas)) {
						return false;
					}
				}
				return true;
			}
			else {
//System.err.println("AttributeList.equals(): different sizes");
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 *
	 */
	public int hashCode() {
		int hash = 0;
		for (Attribute a : values()) {
			// ideally would have Attribute.hashCode() available to us, but don't at this time :(
			hash += a.toString().hashCode();
		}
//System.err.println("AttributeList.hashCode(): hashCode = "+hash);
		return hash;
	}
	
//	private class OurIIOReadProgressListener implements IIOReadProgressListener {
//		public void imageComplete(ImageReader source) {
//System.out.println("OurIIOReadProgressListener:imageComplete()");
//		}
//		public void imageProgress(ImageReader source,float percentageDone) {
//System.out.println("OurIIOReadProgressListener:imageProgress(): percentageDone="+percentageDone);
//		}
//		public void imageStarted(ImageReader source,int imageIndex) {
//System.out.println("OurIIOReadProgressListener:imageStarted(): imageIndex="+imageIndex);
//		}
//		public void readAborted(ImageReader source) {
//System.out.println("OurIIOReadProgressListener:readAborted()");
//		}
//		public void sequenceComplete(ImageReader source) {
//System.out.println("OurIIOReadProgressListener:sequenceComplete()");
//		}
//		public void sequenceStarted(ImageReader source,int minIndex) {
//System.out.println("OurIIOReadProgressListener:sequenceStarted(): minIndex="+minIndex);
//		}
//		public void thumbnailComplete(ImageReader source) {
//System.out.println("OurIIOReadProgressListener:thumbnailComplete()");
//		}
//		public void thumbnailProgress(ImageReader source,float percentageDone) {
//System.out.println("OurIIOReadProgressListener:thumbnailProgress(): percentageDone="+percentageDone);
//		}
//		public void thumbnailStarted(ImageReader source,int imageIndex,int thumbnailIndex) {
//System.out.println("OurIIOReadProgressListener:thumbnailStarted(): imageIndex="+imageIndex+" thumbnailIndex="+thumbnailIndex);
//		}
//	}

	/**
	 * @param	i
	 * @throws	IOException
	 */
	private AttributeTag readAttributeTag(DicomInputStream i) throws IOException {
		int group   = i.readUnsigned16();
		int element = i.readUnsigned16();
		return new AttributeTag(group,element);
	}
	
	// implement Jim Irrer's read termination strategy pattern ...
	
    /**
     * <p>An interface to supply the criteria for prematurely terminating the reading of a DICOM file.</p>
     *
     * <p>Permits more complex strategies than simply stopping at the stopAtTag.</p>
     */
	public interface ReadTerminationStrategy {
		 /**
		 * <p>Define the criteria for prematurely terminating the reading of a DICOM file.</p>
		 *
		 * <p>Permits more complex strategies than simply stopping at the stopAtTag.</p>
		 *
		 * <p>Is tested during the read of the top-level dataset ONLY, i.e., not tested within sequences
		 * (which means that is strategy is based on byteOffset, very long sequences might still be read).</p>
		 *
		 * <p>Tested just AFTER a tag has been read, but before the rest of the attribute for that tag,
		 * hence will leave the stream positioned just after that tag if reading is stopped (just like
		 * the stopAtTag behavior).</p>
		 *
		 * @param attributeList		the list as read so far
		 * @param tag				the tag that has just been read
		 * @param byteOffset		the number of bytes read so far
		 * @return					true if reading should be stopped
		 */
		public boolean terminate(AttributeList attributeList,AttributeTag tag,long byteOffset);
	}

	/**
	 * @param	a
	 * @param	i
	 * @param	byteOffset
	 * @param	lengthToRead
	 * @param	specificCharacterSet
	 * @param	insideUnknownVRSoForceImplicitVRLittleEndian	we are inside a UN VR sequence, so regardless of the whether the DicomInputStream is implicit or explicit VR, treat as implicit VR
	 * @param	isSignedPixelRepresentation						the PixelRepresentation in an enclosing data set is signed (needed to choose VR for US/SS VR data elements)
	 * @return								the byte offset at which the read stopped
	 * @throws	IOException
	 * @throws	DicomException
	 */
	private long readNewSequenceAttribute(Attribute a,DicomInputStream i,long byteOffset,long lengthToRead,SpecificCharacterSet specificCharacterSet,boolean insideUnknownVRSoForceImplicitVRLittleEndian,boolean isSignedPixelRepresentation) throws IOException, DicomException {
		slf4jlogger.trace("readNewSequenceAttribute(): start");
		slf4jlogger.trace("readNewSequenceAttribute(): insideUnknownVRSoForceImplicitVRLittleEndian="+insideUnknownVRSoForceImplicitVRLittleEndian);
		boolean undefinedLength = lengthToRead == 0xffffffffl;
		long endByteOffset=(undefinedLength) ? 0xffffffffl : byteOffset+lengthToRead-1;

		slf4jlogger.trace("readNewSequenceAttribute(): start byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) lengthToRead="+lengthToRead+" endByteOffset=0x"+Long.toHexString(endByteOffset)+" ("+endByteOffset+" dec)");
		try {
			// CBZip2InputStream.available() always returns zero, and since we terminate
			// on exceptions anyway, just forget about it
			while (/*i.available() > 0 && */(undefinedLength || byteOffset < endByteOffset)) {
				slf4jlogger.trace("readNewSequenceAttribute(): loop byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec)");
				long itemStartOffset=byteOffset;
				AttributeTag tag = readAttributeTag(i);
				byteOffset+=4;
				slf4jlogger.trace("readNewSequenceAttribute(): tag="+tag);
				long vl = i.readUnsigned32();		// always implicit VR form for items and delimiters
				byteOffset+=4;
				slf4jlogger.trace("readNewSequenceAttribute(): loop byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) "+tag+" VL=<0x"+Long.toHexString(vl)+">");
				if (tag.equals(TagFromName.SequenceDelimitationItem)) {
					slf4jlogger.trace("readNewSequenceAttribute(): SequenceDelimitationItem");
					break;
				}
				else if (tag.equals(TagFromName.Item)) {
					slf4jlogger.trace("readNewSequenceAttribute(): Item byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec)");
					AttributeList list = new AttributeList();
					byteOffset=list.read(i,byteOffset,vl,false,specificCharacterSet,null/*stopAtTag*/,null/*ReadTerminationStrategy*/,insideUnknownVRSoForceImplicitVRLittleEndian,isSignedPixelRepresentation);
					slf4jlogger.trace("readNewSequenceAttribute(): back from reading Item byteOffset="+byteOffset);
					((SequenceAttribute)a).addItem(list,itemStartOffset);
				}
				else {
					throw new DicomException("Bad tag "+tag+"(not Item or Sequence Delimiter) in Sequence at byte offset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec)");
				}
			}
		}
		catch (EOFException e) {
			slf4jlogger.trace("readNewSequenceAttribute(): Closing on "+e);
			if (!undefinedLength) throw new EOFException();
		}
		catch (IOException e) {
			slf4jlogger.trace("readNewSequenceAttribute(): Closing on "+e);
			if (!undefinedLength) throw new IOException();		// InflaterInputStream seems to throw IOException rather than EOFException
		}
		slf4jlogger.trace("readNewSequenceAttribute(): return byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec)");
		slf4jlogger.trace("readNewSequenceAttribute(): end");
		return byteOffset;
	}


	/**
	 * @param	i
	 * @param	byteOffset
	 * @param	lengthToRead
	 * @param	stopAfterMetaInformationHeader
	 * @param	specificCharacterSet
	 * @param	stopAtTag										the tag (in the top level data set) at which to stop
	 * @param	strategy										the ReadTerminationStrategy at which to stop
	 * @param	insideUnknownVRSoForceImplicitVRLittleEndian	we are inside a UN VR sequence, so regardless of the whether the DicomInputStream is implicit or explicit VR, treat as implicit VR
	 * @param	isSignedPixelRepresentation						the PixelRepresentation in an enclosing data set is signed (needed to choose VR for US/SS VR data elements)
	 * @return													the byte offset at which the read stopped (which will be just past the stopAtTag or the tag read before the ReadTerminationStrategy terminated)
	 * @throws	IOException
	 * @throws	DicomException
	 */
	private long read(DicomInputStream i,long byteOffset,long lengthToRead,boolean stopAfterMetaInformationHeader,
			SpecificCharacterSet specificCharacterSet,AttributeTag stopAtTag,ReadTerminationStrategy strategy,boolean insideUnknownVRSoForceImplicitVRLittleEndian,boolean isSignedPixelRepresentation) throws IOException, DicomException {
		slf4jlogger.trace("read(): start");
		slf4jlogger.trace("read(): Stop tag is "+stopAtTag);
		slf4jlogger.trace("read(): insideUnknownVRSoForceImplicitVRLittleEndian="+insideUnknownVRSoForceImplicitVRLittleEndian);
		if (i.areReadingDataSet()) {
			// Test to see whether or not a codec needs to be pushed on the stream ... after the first time, the TransferSyntax will always be ExplicitVRLittleEndian 
			slf4jlogger.trace("read(): Testing for deflate and bzip2 TS");
			if (i.getTransferSyntaxToReadDataSet().isDeflated()) {
				// insert deflate into input stream and make a new DicomInputStream
				slf4jlogger.trace("read(): Creating new DicomInputStream from deflate");
				i = new DicomInputStream(new InflaterInputStream(i,new Inflater(true)),TransferSyntax.ExplicitVRLittleEndian,false);
				byteOffset=0;
			}
			else if (i.getTransferSyntaxToReadDataSet().isBzip2ed()) {
				// insert bzip2 into input stream and make a new DicomInputStream
				slf4jlogger.trace("read(): Creating new DicomInputStream from bzip2");
				// NB. Older implementations of BZip2CompressorInputStream used to expect the "BZ" prefix to be stripped; this is no longer the case; the code here is unchanged so the "BZ" prefix must be present or will fail (000964)
				try {
					Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream");
					Class [] argTypes  = {InputStream.class};
					Object[] argValues = {i};
					InputStream bzipInputStream = (InputStream)(classToUse.getConstructor(argTypes).newInstance(argValues));
					i = new DicomInputStream(bzipInputStream,TransferSyntax.ExplicitVRLittleEndian,false);
					byteOffset=0;
				}
				catch (java.lang.reflect.InvocationTargetException e) {
					slf4jlogger.info("read(): Could not decompress bzip2 transfer syntax",e);
					throw new DicomException("Not a correctly encoded bzip2 bitstream - "+e);
				}
				catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
					throw new DicomException("Could not instantiate bzip2 codec - "+e);
				}
			}
		}
		
		createDictionaryifNecessary();
		
		boolean undefinedLength = lengthToRead == 0xffffffffl;
		long endByteOffset=(undefinedLength) ? 0xffffffffl : byteOffset+lengthToRead-1;

		slf4jlogger.trace("read(): start byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) endByteOffset=0x"+Long.toHexString(endByteOffset)+" ("+endByteOffset+" dec) lengthToRead="+lengthToRead);
		byte vrBuffer[] = new byte[2];
		boolean explicit = i.getTransferSyntaxInUse().isExplicitVR() && !insideUnknownVRSoForceImplicitVRLittleEndian;
		boolean littleendian = i.getTransferSyntaxInUse().isLittleEndian() || insideUnknownVRSoForceImplicitVRLittleEndian;
		
		// keep track of pixel data size in case need VL for encapsulated data ...
		int rows = 0;
		int columns = 0;
		int frames = 1;
		int samplesPerPixel = 1;
		int bytesPerSample = 0;

		int badAttributeCount = 0;
		AttributeTag tag = null;
		try {
			// CBZip2InputStream.available() always returns zero, and since we terminate
			// on exceptions anyway, just forget about it
			while (/*i.available() > 0 && */(undefinedLength || byteOffset < endByteOffset)) {
				slf4jlogger.trace("read(): i.available()="+i.available());
				slf4jlogger.trace("read(): loop byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) endByteOffset=0x"+Long.toHexString(endByteOffset)+" ("+endByteOffset+" dec)");
				tag = readAttributeTag(i);
				byteOffset+=4;
				slf4jlogger.trace("read(): tag="+tag);

				if (stopAtTag != null && tag.equals(stopAtTag)) {
					slf4jlogger.trace("read(): stopped at "+tag);
					return byteOffset;	// stop now, since we have reached the tag at which we were told to stop
				}
				if (strategy != null && strategy.terminate(this,tag,byteOffset)) {
					slf4jlogger.trace("read(): stopped at "+tag);
					return byteOffset;	// stop now, since we have reached the condition at which we were told to stop
				}
				
				if (tag.equals(TagFromName.ItemDelimitationItem)) {
					slf4jlogger.trace("read(): ItemDelimitationItem");
					// Read and discard value length
					i.readUnsigned32();
					byteOffset+=4;
					return byteOffset;	// stop now, since we must have been called to read an item's dataset
				}
				
				if (tag.equals(TagFromName.Item)) {
					// this is bad ... there shouldn't be Items here since they should
					// only be found during readNewSequenceAttribute()
					// however, try to work around Philips bug ...
					long vl = i.readUnsigned32();		// always implicit VR form for items and delimiters
					byteOffset+=4;
					slf4jlogger.info("read(): Ignoring bad Item at "+byteOffset+" "+tag+" VL=<0x"+Long.toHexString(vl)+">");
					// let's just ignore it for now
					continue;
				}

				if (tag.equals(TagFromName.SequenceDelimitationItem)) {
					// this is bad too ... there shouldn't be SequenceDelimitationItems here since they should
					// only be found during readNewSequenceAttribute() or in encapsulated PixelData and will already
					// have been absorbed
					// however, try to work around a US bug (?GE) in which an extra SequenceDelimitationItem is present after encapsulated PixelData ... (000801)
					long vl = i.readUnsigned32();		// always implicit VR form for items and delimiters
					byteOffset+=4;
					slf4jlogger.info("read(): Ignoring bad SequenceDelimitationItem at byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) "+tag+" VL=<0x"+Long.toHexString(vl)+">");
					// let's just ignore it for now
					continue;
				}
				
				boolean checkForIncorrectImplicitVRElementEncodinginExplicitVR = false;		// DicomWorks bug
				boolean encounteredIncorrectImplicitVRElementEncodinginExplicitVR = false;
				if (explicit && i.markSupported()) {
					checkForIncorrectImplicitVRElementEncodinginExplicitVR = true;
					i.mark(4/*forward read limit*/);
				}

				byte vr[];
				if (explicit) {
					vr=vrBuffer;
					i.readInsistently(vr,0,2);
					if (vr[0] == '-' && vr[1] == '-') {
						slf4jlogger.info("read(): "+tag+" encountered illegal Explicit VR of pair of hyphens in Explicit VR Transfer Syntax at byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) ... compensating");
						byteOffset+=2;
						vr[0]='U';
						vr[1]='N';
					}
					else if (vr[0] == 0 && vr[1] == 0) {
						// this will incorrectly treat an incorrect Implicit VR Element with a VL of zero as if the zero bytes were an incorrect explicit VR instead of part of the VL, and not get to the next else clause (000847) :(
						slf4jlogger.info("read(): "+tag+" encountered illegal Explicit VR of pair of zero bytes in Explicit VR Transfer Syntax at byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) ... compensating");
						byteOffset+=2;
						vr[0]='U';
						vr[1]='N';
					}
					else if (checkForIncorrectImplicitVRElementEncodinginExplicitVR && (vr[0] < 'A' || vr[1] < 'A')) {	// i.e., not a valid explicit VR
						slf4jlogger.info("read(): "+tag+" encountered incorrect Implicit VR Element encoding in Explicit VR Transfer Syntax at byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) ... compensating");
						encounteredIncorrectImplicitVRElementEncodinginExplicitVR = true;	// will force reading of 32 bit implicit style VL
						i.reset();
						vr[0]='U';
						vr[1]='N';
					}
					else {
						byteOffset+=2;
					}
				}
				else {
					vr = dictionary.getValueRepresentationFromTag(tag);
					if (vr == null)  {
						vr=vrBuffer;
						vr[0]='U';
						vr[1]='N';
					}
				}
				
				long vl;
				if (explicit && !encounteredIncorrectImplicitVRElementEncodinginExplicitVR) {
					if (ValueRepresentation.isShortValueLengthVR(vr)) {
						vl=i.readUnsigned16();
						byteOffset+=2;
					}
					else {
						vl=i.readUnsigned16();	// reserved bytes
						byteOffset+=2;
						if (vl == 0
						 || vl == 0x3030) {		// handle reserved bytes incorrectly set to ASCII zero characters, not zero :) ! (000872)
							vl=i.readUnsigned32();
							byteOffset+=4;
						}
						else {
							slf4jlogger.info("read(): {} encountered incorrect short Value Length Form for {} Value Representation in Explicit VR Transfer Syntax at byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) ... compensating",tag,ValueRepresentation.getAsString(vr));
						}
					}
				}
				else {
					vl=i.readUnsigned32();
					byteOffset+=4;
				}
				
				if (explicit) {
					// do not do this until AFTER the value length has been read, since explicit UN uses the long form of length
					if (ValueRepresentation.isUnknownVR(vr)) {
						byte vrd[] = dictionary.getValueRepresentationFromTag(tag);
						if (vrd != null
						 && vrd.length >= 2
						 && (littleendian || ValueRepresentation.getWordLengthOfValueAffectedByEndianness(vrd) == 1)			// have not implemented a means to force Attribute.read() to swap endianness during read() :(
						) {
							slf4jlogger.trace("read(): byteoffset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) for tag "+tag+" consider overriding explicit VR "+ValueRepresentation.getAsString(vr)+" with "+ValueRepresentation.getAsString(vrd));
							if (!ValueRepresentation.isSequenceVR(vrd)) {
								slf4jlogger.trace("read(): byteoffset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) for tag "+tag+" overriding explicit VR "+ValueRepresentation.getAsString(vr)+" with "+ValueRepresentation.getAsString(vrd));
								vr[0] = vrd[0];
								vr[1] = vrd[1];
							}
							else {
								slf4jlogger.trace("read(): byteoffset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) for tag "+tag+" not overriding explicit VR "+ValueRepresentation.getAsString(vr)+" with SQ until later when we can signal content is IVRLE");
							}
						}
					}
				}
					
				if (tag.isPrivateCreator()) {	// silently override VR, whether it be explictly UN or just wrong, or the default UN up to this point for implicit
					vr[0] = 'L';
					vr[1] = 'O';
				}
				slf4jlogger.trace("read(): byteoffset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) "+tag+" VR=<"+ValueRepresentation.getAsString(vr)+"> VL=<0x"+Long.toHexString(vl)+">");

				Attribute a = null;

				// Need to read known safe private sequences as SQ even if explicit UN with fixed length VR,
				// otherwise cannot recurse into them to check contents are safe (which they may not all be)
				// e.g. Hologic Marker Sequence is safe but Marker Tech Initials inside is not
				if (ValueRepresentation.isSequenceVR(vr)
				|| (ValueRepresentation.isUnknownVR(vr) && (vl == 0xffffffffl || tag.isPrivate() && ClinicalTrialsAttributes.isSafePrivateSequenceAttribute(getPrivateCreatorString(tag),tag)))
				) {
					slf4jlogger.trace("read(): byteoffset 0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) for tag "+tag+" is SQ or UN with undefined length to be treated as an SQ");
					a=new SequenceAttribute(tag);
					byteOffset=readNewSequenceAttribute(a,i,byteOffset,vl,specificCharacterSet,ValueRepresentation.isUnknownVR(vr) || insideUnknownVRSoForceImplicitVRLittleEndian,isSignedPixelRepresentation);	// (000909) SQ may be nested within UN, so propagate
				}
				else if (vl != 0xffffffffl) {
					checkSanityOfValueLength(vl,vr,tag,encounteredIncorrectImplicitVRElementEncodinginExplicitVR);
					try {
						a = AttributeFactory.newAttribute(tag,vr,vl,i,specificCharacterSet,explicit,bytesPerSample,byteOffset,isSignedPixelRepresentation);	// creates and reads the attribute with the appropriate isSignedPixelRepresentation dependent VR of US/SS (000919)
					}
					catch (Exception e) {
						slf4jlogger.error("",e);
						a = null;
						i.skipInsistently(vl);	// since we didn't read anything; otherwise stream gets out of sync; will not happen unless internal inconsistency, e.g., no appropriate constructor (should return UnknownAttribute)
						if (++badAttributeCount > badAttributeLimit) {
							throw new DicomException("Too many bad attributes - probably not a DICOM dataset at all - giving up");
						}
					}
					byteOffset+=vl;
				}
				else if (vl == 0xffffffffl && tag.equals(TagFromName.PixelData)/* && i.getTransferSyntaxInUse().isEncapsulated()*/) {	// assume encapsulated in case TS is not recognized
					//boolean unencapsulatePixelData = ???; // incomplete experiments with deferring not just decompression, but also unencapsulation
					//if (unencapsulatePixelData) {
					int wordsPerFrame = rows*columns*samplesPerPixel;
					slf4jlogger.trace("read(): Undefined length encapsulated Pixel Data: words per frame "+wordsPerFrame);
					TransferSyntax ts = i.getTransferSyntaxInUse();
					String tsuid = ts.getUID();
					slf4jlogger.trace("read(): Undefined length encapsulated Pixel Data: TransferSyntax UID "+tsuid);
					extractCompressedPixelDataCharacteristics(ts);
					boolean doneReadingEncapsulatedData = false;
					// when constructing EncapsulatedInputStream, take care whether or not to enable JPEG EOI detection to allow fragment spanning, and if not, whether to use one fragment per frame or all fragments in one frame
					EncapsulatedInputStream ei = new EncapsulatedInputStream(i,isJPEGFamily,isRLE);
					if (decompressPixelData) {
						slf4jlogger.debug("read(): decompress Pixel Data");
						if (tsuid.equals(TransferSyntax.PixelMedEncapsulatedRawLittleEndian)) {
							if (bytesPerSample == 1) {
								byte[] values = new byte[wordsPerFrame*frames];
								for (int f=0; f<frames; ++f) {
									ei.read(values,f*wordsPerFrame,wordsPerFrame);
									//ei.nextFrame();
								}
								a = new OtherByteAttribute(tag);
								a.setValues(values);
								doneReadingEncapsulatedData=true;
							}
							else if (bytesPerSample == 2) {
								short[] values = new short[wordsPerFrame*frames];
								for (int f=0; f<frames; ++f) {
									ei.readUnsigned16(values,f*wordsPerFrame,wordsPerFrame);
									//ei.nextFrame();
								}
								a = new OtherWordAttribute(tag);
								a.setValues(values);
								doneReadingEncapsulatedData=true;
							}
							else {
								throw new DicomException("Encapsulated data of more than 2 bytes per sample not supported (got "+bytesPerSample+")");
							}
						}
						else if (isRLE) {
							slf4jlogger.debug("read(): TransferSyntax.RLE");
							// PlanarConfiguration does not affect how segments are encoded, since
							// sending three separate planes each as a segment verus decomposing
							// the three bytes of a composite pixel produces three segments in the same order
							// but it does affect how the samples are then arranged in the bytePixelData
							boolean isColorByPlane = Attribute.getSingleIntegerValueOrDefault(this,TagFromName.PlanarConfiguration,1) == 1;
							
							int pixelsPerFrame = rows*columns;
							if (bytesPerSample == 1) {
								slf4jlogger.trace("read(): bytesPerSample = 1");
								byte[] bytePixelData = new byte[wordsPerFrame*frames];
								slf4jlogger.trace("read(): Number of frames = "+frames);
								slf4jlogger.trace("read(): wordsPerFrame = "+wordsPerFrame);
								slf4jlogger.trace("read(): pixelsPerFrame = "+pixelsPerFrame);
								int frameOffset=0;
								//try {
								for (int f=0; f<frames; ++f) {
									slf4jlogger.trace("read(): frame = "+f);
									//ei.skipInsistently(64);	// skip the "RLE header", which is of fixed length and unnecessary for decoding, as long as we skip to even byte after each segment
									// The RLE "header" consists of 16 long values
									// the 1st value is the number of segments
									// the remainder are the byte offsets of each of up to 15 segments
									int numberofSegments = (int)(ei.readUnsigned32LittleEndian());
									slf4jlogger.trace("read(): Number of segments = "+numberofSegments);
									long[] segmentOffsets = new long[15];
									ei.readUnsigned32LittleEndian(segmentOffsets,0,15);
									for (int soi=0; soi<15; ++soi) {
										//slf4jlogger.trace("read(): Segment ["+soi+"] offset = "+segmentOffsets[soi]);
										if (segmentOffsets[soi]%2 != 0) {
											slf4jlogger.error("read(): fragment offset for segment "+soi+" is not even length ("+segmentOffsets[soi]+") but ignoring and using odd offset anyway");
										}
									}
									
									if (isColorByPlane) {
										int sampleOffset = 0;
										for (int s=0; s < samplesPerPixel; ++s) {
											//slf4jlogger.trace("read(): Doing sample = "+s);
											int currentOffset = ei.getOffsetOfNextByteToReadFromStartOfFragment();
											slf4jlogger.trace("read(): At fragment offset "+currentOffset);
											int bytesToSkipToStartOfSegment = (int)(segmentOffsets[s]) - currentOffset;
											if (bytesToSkipToStartOfSegment > 0) {
												ei.skipInsistently(bytesToSkipToStartOfSegment);
												//slf4jlogger.trace("read(): Skipped "+bytesToSkipToStartOfSegment+" to segment offset "+segmentOffsets[s]);
											}
											else if (bytesToSkipToStartOfSegment < 0) {
												throw new DicomException("Already read past start of next segment "+s+" - at "+currentOffset+" need to be at "+segmentOffsets[s]);
											}
											// else right on already
											int got = UnPackBits.decode(ei,bytePixelData,frameOffset + sampleOffset,pixelsPerFrame);	// entire planes of samples
											//slf4jlogger.trace("read(): got = "+got+" pixels");
											sampleOffset+=pixelsPerFrame;
										}
									}
									else {
										for (int s=0; s < samplesPerPixel; ++s) {
											//slf4jlogger.trace("read(): Doing sample = "+s);
											byte[] segment = new byte[pixelsPerFrame];
											int currentOffset = ei.getOffsetOfNextByteToReadFromStartOfFragment();
											//slf4jlogger.trace("read(): At fragment offset "+currentOffset);
											int bytesToSkipToStartOfSegment = (int)(segmentOffsets[s]) - currentOffset;
											if (bytesToSkipToStartOfSegment > 0) {
												ei.skipInsistently(bytesToSkipToStartOfSegment);
												//slf4jlogger.trace("read(): Skipped "+bytesToSkipToStartOfSegment+" to segment offset "+segmentOffsets[s]);
											}
											else if (bytesToSkipToStartOfSegment < 0) {
												throw new DicomException("Already read past start of next segment "+s+" - at "+currentOffset+" need to be at "+segmentOffsets[s]);
											}
											// else right on already
											int got = UnPackBits.decode(ei,segment,0,pixelsPerFrame);
											//slf4jlogger.trace("read(): got = "+got+" pixels");
											// copy into pixel data distributed across color-by-pixel
											for (int p=0; p<pixelsPerFrame; ++p) {
												bytePixelData[frameOffset + p*samplesPerPixel + s] = segment[p];
											}
										}
									}
									ei.nextFrame();
									frameOffset+=wordsPerFrame;
								}
								try {
									ei.readSequenceDelimiter();		// since we terminated loop on number of frames, rather than keeping going until ran out, we need to absorb the delimiter
								}
								catch (IOException e) {
									System.err.println("Error: Incorrectly terminated encapsulated pixel data - ignoring and creating PixelData anyway - "+e);
								}
								doneReadingEncapsulatedData=true;
								//}
								//catch (Exception e) {
								//	slf4jlogger.error("",e);
								//}
								a = new OtherByteAttribute(tag);
								a.setValues(bytePixelData);
								pixelDataWasActuallyDecompressed = true;
								colorSpaceWasConvertedToRGBDuringDecompression = false;
							}
							else if (bytesPerSample == 2) {
								// for each frame, have to read all high bytes first for a sample, then low bytes :(
								short[] shortPixelData = new short[wordsPerFrame*frames];
								slf4jlogger.trace("read(): Number of frames = "+frames);
								slf4jlogger.trace("read(): wordsPerFrame = "+wordsPerFrame);
								slf4jlogger.trace("read(): pixelsPerFrame = "+pixelsPerFrame);
								int frameOffset=0;
								//try {
								for (int f=0; f<frames; ++f) {
									slf4jlogger.trace("read(): frame = "+f);
									// The RLE "header" consists of 16 long values
									// the 1st value is the number of segments
									// the remainder are the byte offsets of each of up to 15 segments
									int numberofSegments = (int)(ei.readUnsigned32LittleEndian());
									slf4jlogger.trace("read(): NUmber of segments = "+numberofSegments);
									long[] segmentOffsets = new long[15];
									ei.readUnsigned32LittleEndian(segmentOffsets,0,15);
									for (int soi=0; soi<15; ++soi) {
										//slf4jlogger.trace("read(): Segment ["+soi+"] offset = "+segmentOffsets[soi]);
										if (segmentOffsets[soi]%2 != 0) {
											System.err.println("Error: fragment offset for segment "+soi+" is not even length ("+segmentOffsets[soi]+") but ignoring and using odd offset anyway");
										}
									}
									
									if (isColorByPlane) {
										int sampleOffset = 0;
										int segment = 0;
										for (int s=0; s < samplesPerPixel; ++s) {
											//slf4jlogger.trace("read(): Doing sample = "+s);
											//slf4jlogger.trace("read(): Doing firstsegment");
											byte[] firstsegment = new byte[pixelsPerFrame];
											{
												int currentOffset = ei.getOffsetOfNextByteToReadFromStartOfFragment();
												//slf4jlogger.trace("read(): At fragment offset "+currentOffset);
												int bytesToSkipToStartOfSegment = (int)(segmentOffsets[segment]) - currentOffset;
												if (bytesToSkipToStartOfSegment > 0) {
													ei.skipInsistently(bytesToSkipToStartOfSegment);
													//slf4jlogger.trace("read(): Skipped "+bytesToSkipToStartOfSegment+" to segment offset "+segmentOffsets[segment]);
												}
												else if (bytesToSkipToStartOfSegment < 0) {
													throw new DicomException("Already read past start of next segment "+segment+" - at "+currentOffset+" need to be at "+segmentOffsets[segment]);
												}
												// else right on already
											}
											int got = UnPackBits.decode(ei,firstsegment,0,pixelsPerFrame);
											//slf4jlogger.trace("read(): got = "+got+" bytes for first segment");
											//slf4jlogger.trace("read(): Doing secondsegment");
											++segment;
											byte[] secondsegment = new byte[pixelsPerFrame];
											{
												int currentOffset = ei.getOffsetOfNextByteToReadFromStartOfFragment();
												//slf4jlogger.trace("read(): At fragment offset "+currentOffset);
												int bytesToSkipToStartOfSegment = (int)(segmentOffsets[segment]) - currentOffset;
												if (bytesToSkipToStartOfSegment > 0) {
													ei.skipInsistently(bytesToSkipToStartOfSegment);
													slf4jlogger.trace("read(): Skipped "+bytesToSkipToStartOfSegment+" to segment offset "+segmentOffsets[segment]);
												}
												else if (bytesToSkipToStartOfSegment < 0) {
													throw new DicomException("Already read past start of next segment "+segment+" - at "+currentOffset+" need to be at "+segmentOffsets[segment]);
												}
												// else right on already
											}
											got = UnPackBits.decode(ei,secondsegment,0,pixelsPerFrame);
											//slf4jlogger.trace("read(): got = "+got+" bytes for second segment");
											for (int p=0; p<pixelsPerFrame; ++p) {
												shortPixelData[frameOffset + sampleOffset + p] = (short)( ((firstsegment[p]&0xff) << 8) + (secondsegment[p]&0xff));
											}
											sampleOffset+=pixelsPerFrame;
											++segment;
										}
									}
									else {
										throw new DicomException("RLE of PlanarConfiguration == 0 for 2 bytes per sample not supported");
									}
									ei.nextFrame();
									frameOffset+=wordsPerFrame;
								}
								try {
									ei.readSequenceDelimiter();		// since we terminated loop on number of frames, rather than keeping going until ran out, we need to absorb the delimiter
								}
								catch (IOException e) {
									slf4jlogger.error("read(): Incorrectly terminated encapsulated pixel data - ignoring and creating PixelData anyway - "+e);
								}
								doneReadingEncapsulatedData=true;
								//}
								//catch (Exception e) {
								//	slf4jlogger.error("",e);
								//}
								a = new OtherWordAttribute(tag);
								a.setValues(shortPixelData);
								pixelDataWasActuallyDecompressed = true;
								colorSpaceWasConvertedToRGBDuringDecompression = false;
							}
							else {
								throw new DicomException("RLE of more than 2 bytes per sample not supported (got "+bytesPerSample+")");
							}
						}
						else {
							CompressedFrameDecoder.scanForCodecs();
							pixelDataWasActuallyDecompressed = true;
							slf4jlogger.info("Based on Transfer Syntax, colorSpaceWillBeConvertedToRGBDuringDecompression = {}",colorSpaceWillBeConvertedToRGBDuringDecompression);
							if (readerWanted != null) {
								ImageReader reader = CompressedFrameDecoder.selectReaderFromCodecsAvailable(readerWanted,tsuid,bytesPerSample);
								if (reader != null) {
									byte[]  bytePixelData = null;	// lazy instantiation of one or the other
									short[] shortPixelData = null;
									slf4jlogger.trace("read(): Using columns = "+columns);
									slf4jlogger.trace("read(): Using rows = "+rows);
									slf4jlogger.trace("read(): Using frames = "+frames);
									slf4jlogger.trace("read(): Using samplesPerPixel = "+samplesPerPixel);
									slf4jlogger.trace("read(): Using bytesPerSample = "+bytesPerSample);
									int pixelsPerFrame = columns*rows*samplesPerPixel;
									int pixelsPerMultiFrameImage = pixelsPerFrame*frames;
									iioMetadata = new IIOMetadata[frames];
									for (int f=0; f<frames; ++f) {
									slf4jlogger.trace("read(): Starting frame "+f);
										ImageInputStream iiois = ImageIO.createImageInputStream(ei);
										//reader.reset();		// NB. will also remove listeners, which we aren't using ... not strictly necessary if following setInput() actually removes anything cached, as it is supposed to
										reader.setInput(iiois,true/*seekForwardOnly*/,true/*ignoreMetadata*/);
										//slf4jlogger.trace("read(): Calling reader.readAll()");
										slf4jlogger.trace("read(): Calling reader.read()");
										//IIOImage iioImage = null;		// (000911) don't use this until Oracle fixes bug in readAll()
										BufferedImage image = null;
										try {
											//iioImage = reader.readAll(0,null/*ImageReadParam*/);
											image = reader.read(0);
										}
										catch (IIOException e) {
											slf4jlogger.error("",e);
											slf4jlogger.trace("read(): \""+e.toString()+"\"");
											//if (tsuid.equals(TransferSyntax.JPEGBaseline) && reader.getOriginatingProvider().getDescription(Locale.US).equals("Standard JPEG Image Reader") && (reader.getOriginatingProvider().getVendorName().equals("Sun Microsystems, Inc.") || reader.getOriginatingProvider().getVendorName().equals("Oracle Corporation"))
											// && e.toString().equals("javax.imageio.IIOException: Inconsistent metadata read from stream")) {
											//	throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()+" does not support JPEG images with components numbered from 0");
											//}
										}
										//slf4jlogger.trace("read(): Back from frame reader.readAll()");
										slf4jlogger.trace("read(): Back from frame reader.read()");
										//if (iioImage == null) {
										if (image == null) {
											throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()
												+" returned null image for Transfer Syntax "+tsuid);
										}
										else {
											//iioMetadata[f] = iioImage.getMetadata();
											//BufferedImage image = (BufferedImage)(iioImage.getRenderedImage());
											slf4jlogger.trace("read(): Back from frame "+f+" reader.read(), BufferedImage="+image);
											//if (image == null) {
											//	throw new DicomException("Reader "+reader.getOriginatingProvider().getDescription(Locale.US)+" "+reader.getOriginatingProvider().getVendorName()+" "+reader.getOriginatingProvider().getVersion()
											//		+" returned null image for Transfer Syntax "+tsuid);
											//}
											Raster raster = image.getData();
											int numDataElements = raster.getNumDataElements();
											slf4jlogger.trace("read(): getNumDataElements="+numDataElements);
											if (numDataElements == samplesPerPixel) {
												int transferType = raster.getTransferType();
												slf4jlogger.trace("read(): getTransferType="+transferType);
												if (transferType == DataBuffer.TYPE_BYTE) {
													slf4jlogger.trace("read(): Getting "+(samplesPerPixel > 1 ? "interleaved " : "")+samplesPerPixel+" channel byte data");
													byte[] vPixelData = (byte[])(raster.getDataElements(0,0,columns,rows,null));
													slf4jlogger.trace("read(): Decompressed byte array length "+vPixelData.length+" expected "+pixelsPerFrame);
													if (bytePixelData == null) {
														if (frames == 1) {
															bytePixelData = vPixelData;
														}
														else {
															bytePixelData = new byte[pixelsPerMultiFrameImage];
														}
													}
													if (vPixelData != null) {
														System.arraycopy(vPixelData,0,bytePixelData,pixelsPerFrame*f,pixelsPerFrame);
													}
												}
												else if (transferType == DataBuffer.TYPE_SHORT
												      || transferType == DataBuffer.TYPE_USHORT) {
													slf4jlogger.trace("read(): Getting "+(samplesPerPixel > 1 ? "interleaved " : "")+samplesPerPixel+" channel byte data");
													short[] vPixelData = (short[])(raster.getDataElements(0,0,columns,rows,null));
													slf4jlogger.trace("read(): Decompressed short array length "+vPixelData.length+" expected "+pixelsPerFrame);
													if (shortPixelData == null) {
														if (frames == 1) {
															shortPixelData = vPixelData;
														}
														else {
															shortPixelData = new short[pixelsPerMultiFrameImage];
														}
													}
													if (vPixelData != null) {
														System.arraycopy(vPixelData,0,shortPixelData,pixelsPerFrame*f,pixelsPerFrame);
													}
												}
											}
										}
										ei.nextFrame();
									}
									slf4jlogger.trace("read(): Have now read all the frames");
									try {
										ei.readSequenceDelimiter();		// since we terminated loop on number of frames, rather than keeping going until ran out, we need to absorb the delimiter
									}
									catch (IOException e) {
										System.err.println("Error: Incorrectly terminated encapsulated pixel data - ignoring and creating PixelData anyway - "+e);
									}
									slf4jlogger.trace("read(): Have now absorbed the sequence delimiter");
									if (bytePixelData != null) {
										a = new OtherByteAttribute(tag);
										a.setValues(bytePixelData);
										pixelDataWasActuallyDecompressed = true;
										colorSpaceWasConvertedToRGBDuringDecompression = colorSpaceWillBeConvertedToRGBDuringDecompression;
									}
									else if (shortPixelData != null) {
										a = new OtherWordAttribute(tag);
										a.setValues(shortPixelData);
										pixelDataWasActuallyDecompressed = true;
										colorSpaceWasConvertedToRGBDuringDecompression = colorSpaceWillBeConvertedToRGBDuringDecompression;
									}
									slf4jlogger.trace("read(): Have now created the PixelData attribute");
									doneReadingEncapsulatedData=true;
									try {
										slf4jlogger.trace("read(): Calling dispose() on reader");
										reader.dispose();	// http://info.michael-simons.eu/2012/01/25/the-dangers-of-javas-imageio/
									}
									catch (Exception e) {
										slf4jlogger.error("",e);
									}
								}
							}
							else {
								slf4jlogger.trace("read(): Unrecognized Transfer Syntax "+tsuid+" for encapsulated PixelData");
								throw new DicomException("Unrecognized Transfer Syntax "+tsuid+" for encapsulated PixelData");
							}
						}
						slf4jlogger.info("colorSpaceWasConvertedToRGBDuringDecompression = {}",colorSpaceWasConvertedToRGBDuringDecompression);
					}
					else {
						slf4jlogger.debug("read(): Do not decompress Pixel Data");
						long startReadCompressedBytesTime = System.currentTimeMillis();
						if (ei.framesAreSeparated()) {	// e.g., JPEG or RLE
							slf4jlogger.debug("read(): frames are separated in encapsulated input");
							byte[][] frameArray = new byte[frames][];
							for (int f=0; f<frames; ++f) {
								slf4jlogger.trace("read(): Doing compressed frame {}",f);
								List<byte[]> bb = new ArrayList<byte[]>();
								List<Integer> bl = new ArrayList<Integer>();
								int bytesTotal = 0;
								int bytesThisBuffer = 0;
								do {
									byte[] b = new byte[1024];
									bytesThisBuffer = ei.read(b);
									if (bytesThisBuffer > 0) {
										bb.add(b);
										bl.add(new Integer(bytesThisBuffer));
										bytesTotal += bytesThisBuffer;
									}
								} while (bytesThisBuffer > 0);
								slf4jlogger.trace("read(): bytesTotal = {} for frame {}",bytesTotal,f);
								byte[] frame = null;
								if (bytesTotal > 0) {
									frame = new byte[bytesTotal];
									int offset = 0;
									for (int bi=0; bi<bb.size(); ++bi) {
										byte[] b = bb.get(bi);
										int l = bl.get(bi).intValue();
										slf4jlogger.trace("read(): b length = {}",l);
										System.arraycopy(b,0,frame,offset,l);
										offset += l;
									}
								}
								frameArray[f] = frame;
								ei.nextFrame();
							}
							try {
								ei.readSequenceDelimiter();		// since we terminated loop on number of frames, rather than keeping going until ran out, we need to absorb the delimiter
							}
							catch (IOException e) {
								System.err.println("Error: Incorrectly terminated encapsulated pixel data - ignoring and creating PixelData anyway - "+e);
							}
							a = new OtherByteAttributeMultipleCompressedFrames(tag,frameArray);
						}
						else {	// all frames are in one fragment (e.g., MPEG)
							slf4jlogger.debug("read(): frames are not separated in encapsulated input");
							// could optimize this, since we could just read the length of the single item, but quick and dirty reuse of EncapsulatedInputStream :(
							List<byte[]> bb = new ArrayList<byte[]>();
							List<Integer> bl = new ArrayList<Integer>();
							int bytesTotal = 0;
							int bytesThisBuffer = 0;
							do {
								byte[] b = new byte[1024];
								bytesThisBuffer = ei.read(b);
								if (bytesThisBuffer > 0) {
									bb.add(b);
									bl.add(new Integer(bytesThisBuffer));
									bytesTotal += bytesThisBuffer;
								}
							} while (bytesThisBuffer > 0);
							slf4jlogger.debug("read(): bytesTotal = {}",bytesTotal);
							byte[] frame = null;
							if (bytesTotal > 0) {
								frame = new byte[bytesTotal];
								int offset = 0;
								for (int bi=0; bi<bb.size(); ++bi) {
									byte[] b = bb.get(bi);
									int l = bl.get(bi).intValue();
									slf4jlogger.trace("read(): b length = {}",l);
									System.arraycopy(b,0,frame,offset,l);
									offset += l;
								}
							}
							a = new OtherByteAttributeMultipleCompressedFrames(tag,frame);
						}
						doneReadingEncapsulatedData=true;
						pixelDataWasActuallyDecompressed = false;
						slf4jlogger.trace("read(): read of compressed bytes into buffers done in "+(System.currentTimeMillis()-startReadCompressedBytesTime)+" ms");
					}
					if (!doneReadingEncapsulatedData) {
						slf4jlogger.debug("read(): skipping encapsulated pixel data");
						while (ei.skip(1024) > 0);	// it is appropriate to use skip() rather than use skipInsistently() here 
					}
					{
						long encapsulatedBytesRead = ei.getBytesRead();
						slf4jlogger.trace("read(): encapsulatedBytesRead = "+encapsulatedBytesRead);
						byteOffset+= encapsulatedBytesRead;		// otherwise won't be able to detect end of fixed length sequences and items that contain encapsulated pixel data (e.g., IconImageSequence)
						if (encapsulatedBytesRead != 0) {
							// compute CR with precision of three decimal places
							compressionRatio = (long)columns*rows*samplesPerPixel*bytesPerSample*frames*1000/encapsulatedBytesRead;
							compressionRatio = compressionRatio / 1000;
							slf4jlogger.trace("read(): compressionRatio = "+compressionRatio);
						}
					}
					slf4jlogger.trace("read(): Done with encapsulated pixel data");
				}

				if (a != null) {
					slf4jlogger.trace("read(): "+a.toString());
					if (get(tag) != null) {
						System.err.println("Error: Illegal duplicate tag in dataset - "+tag+" - replacing previous occurence");
					}
					put(tag,a);

					if (tag.equals(TagFromName.FileMetaInformationGroupLength)) {
						if (i.areReadingMetaHeader()) {
							slf4jlogger.trace("read(): Found meta-header");
							slf4jlogger.trace("read(): Length attribute class="+a.getClass());
							assert(!insideUnknownVRSoForceImplicitVRLittleEndian);
							long metaLength=a.getSingleIntegerValueOrDefault(0);
							byteOffset=read(i,byteOffset,metaLength,false,null,stopAtTag,strategy,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);		// detects and sets transfer syntax for reading dataset
							TransferSyntax ts = i.getTransferSyntaxToReadDataSet();
							if (ts == null) {
								System.err.println("Error: Transfer Syntax UID was not specified in Meta Information Header so guessing");
								i.guessTransferSyntaxToReadDataSet();
							}
							i.setReadingDataSet();
							if (stopAfterMetaInformationHeader) {
								slf4jlogger.trace("read(): Stopping after meta-header");
								break;
							}
							else {
								slf4jlogger.trace("read(): Calling read");
								byteOffset=read(i,byteOffset,0xffffffffl,false,null,stopAtTag,strategy,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);	// read to end (will detect and set own SpecificCharacterSet)
								slf4jlogger.trace("read(): Back from read after metaheader: now undefinedLength="+undefinedLength+" byteoffset=0x"+Long.toHexString(byteOffset)+" ("+byteOffset+" dec) endByteOffset="+endByteOffset);
								break;	// ... no plausible reason to continue past this point
							}
						}
						else {
							// ignore it, e.g. nested within a sequence item (GE bug).
							slf4jlogger.trace("read(): Ignoring unexpected FileMetaInformationGroupLength outside meta information header");
						}
					}
					else if (tag.equals(TagFromName.TransferSyntaxUID)) {
							slf4jlogger.trace("read(): Have TransferSyntaxUID attribute");
						if (i.areReadingMetaHeader()) {
							slf4jlogger.trace("read(): Have TransferSyntaxUID attribute in Meta Information Header");
							String tsuid = a.getSingleStringValueOrEmptyString();
							slf4jlogger.trace("read(): tsuid = "+tsuid);
							i.setTransferSyntaxToReadDataSet(tsuid.length() > 0 ? new TransferSyntax(tsuid) : null);
						}
						else {
							// ignore it, e.g. nested within a sequence item (GE bug).
							slf4jlogger.trace("read(): Ignoring unexpected TransferSyntaxUID outside meta information header");
						}
					}
					else if (tag.equals(TagFromName.SpecificCharacterSet)) {
						specificCharacterSet = new SpecificCharacterSet(a.getStringValues(),a.getByteValues());
					}
					else if (tag.equals(TagFromName.Columns)) {
						columns = a.getSingleIntegerValueOrDefault(0);
						slf4jlogger.trace("read(): Setting columns = "+columns);
					}
					else if (tag.equals(TagFromName.Rows)) {
						rows = a.getSingleIntegerValueOrDefault(0);
						slf4jlogger.trace("read(): Setting rows = "+rows);
					}
					else if (tag.equals(TagFromName.NumberOfFrames)) {
						frames = a.getSingleIntegerValueOrDefault(1);
						slf4jlogger.trace("read(): Setting frames = "+frames);
					}
					else if (tag.equals(TagFromName.SamplesPerPixel)) {
						samplesPerPixel = a.getSingleIntegerValueOrDefault(1);
						slf4jlogger.trace("read(): Setting samplesPerPixel = "+samplesPerPixel);
					}
					else if (tag.equals(TagFromName.BitsAllocated)) {
						bytesPerSample = (a.getSingleIntegerValueOrDefault(16)-1)/8+1;
						slf4jlogger.trace("read(): Setting bytesPerSample = "+bytesPerSample);
					}
					else if (tag.equals(TagFromName.PixelRepresentation)) {		// for (000919)
						int pixelRepresentation = a.getSingleIntegerValueOrDefault(0);
						isSignedPixelRepresentation = pixelRepresentation == 1;
						slf4jlogger.trace("read(): Setting isSignedPixelRepresentation = "+isSignedPixelRepresentation+" because PixelRepresentation = "+pixelRepresentation);
					}
				}
			}
		}
		catch (EOFException e) {
			slf4jlogger.trace("read(): Closing on "+e);
			//slf4jlogger.error("",e);
			if (!undefinedLength) throw new EOFException();
		}
		catch (IOException e) {
			slf4jlogger.trace("read(): Closing on "+e);
			//slf4jlogger.error("",e);
			if (!undefinedLength) throw new IOException();		// InflaterInputStream seems to throw IOException rather than EOFException
		}
		slf4jlogger.trace("read(): end");
		return byteOffset;
	}

	/**
	 * <p>Read the meta information header (if present) and then stop.</p>
	 *
	 * <p>Leaves the stream opened and positioned at the start of the data set.</p>
	 *
	 * @param	i		the stream to read from
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long readOnlyMetaInformationHeader(DicomInputStream i) throws IOException, DicomException {
		return read(i,i.getByteOffsetOfStartOfData(),0xffffffffl,true/*stopAfterMetaInformationHeader*/,null/*SpecificCharacterSet*/,null/*stopAtTag*/,null/*ReadTerminationStrategy*/,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);
//System.err.println("readOnlyMetaInformationHeader(): afterwards i.areReadingDataSet()="+i.areReadingDataSet());
		// important that i.areReadingDataSet() be true at this point ... triggers check for codec if read (or copied) further
	}

	/**
	 * <p>Read all the DICOM attributes in the stream until the specified tag is encountered.</p>
	 *
	 * <p>Does not read beyond the group element pair of the specified stop tag.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	i				the stream to read from
	 * @param	stopAtTag		the tag (in the top level data set) at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the stopAtTag, if stopped)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(DicomInputStream i,AttributeTag stopAtTag) throws IOException, DicomException {
//System.err.println("read(DicomInputStream i,AttributeTag stopAtTag="+stopAtTag+"):");
		return read(i,i.getByteOffsetOfStartOfData(),0xffffffffl,false/*stopAfterMetaInformationHeader*/,null/*SpecificCharacterSet*/,stopAtTag,null/*ReadTerminationStrategy*/,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>Read all the DICOM attributes in the stream until the specified tag is encountered.</p>
	 *
	 * <p>Does not read beyond the group element pair of the specified stop tag.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	i				the stream to read from
	 * @param	stopAtTag		the tag (in the top level data set) at which to stop
	 * @param	strategy		the ReadTerminationStrategy at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the stopAtTag or the tag read before the ReadTerminationStrategy terminated)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(DicomInputStream i,AttributeTag stopAtTag,ReadTerminationStrategy strategy) throws IOException, DicomException {
//System.err.println("read(DicomInputStream i,AttributeTag stopAtTag="+stopAtTag+"):");
		return read(i,i.getByteOffsetOfStartOfData(),0xffffffffl,false/*stopAfterMetaInformationHeader*/,null/*SpecificCharacterSet*/,stopAtTag,strategy,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>Read all the DICOM attributes in the stream until the specified tag is encountered.</p>
	 *
	 * <p>Does not read beyond the group element pair of the specified stop tag.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	i				the stream to read from
	 * @param	strategy		the ReadTerminationStrategy at which to stop
	 * @return					the byte offset at which the read stopped (which will be dependent on the ReadTerminationStrategy)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(DicomInputStream i,ReadTerminationStrategy strategy) throws IOException, DicomException {
//System.err.println("read(DicomInputStream i,AttributeTag stopAtTag="+stopAtTag+"):");
		return read(i,i.getByteOffsetOfStartOfData(),0xffffffffl,false/*stopAfterMetaInformationHeader*/,null/*SpecificCharacterSet*/,null/*stopAtTag*/,strategy,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>Read all the DICOM attributes in the stream until there are no more.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	i		the stream to read from
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(DicomInputStream i) throws IOException, DicomException {
		return read(i,i.getByteOffsetOfStartOfData(),0xffffffffl,false/*stopAfterMetaInformationHeader*/,null/*SpecificCharacterSet*/,null/*stopAtTag*/,null/*ReadTerminationStrategy*/,false/*insideUnknownVRSoForceImplicitVRLittleEndian*/,false/*isSignedPixelRepresentation*/);
	}

	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * @param	name				the input file name
	 * @param	transferSyntaxUID	the transfer syntax to use for the data set (leave null for autodetection)
	 * @param	hasMeta				look for a meta information header
	 * @param	useBufferedStream	buffer the input for better performance
	 * @return						the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name,String transferSyntaxUID,boolean hasMeta,boolean useBufferedStream) throws IOException, DicomException {
		return read(name,transferSyntaxUID,hasMeta,useBufferedStream,null/*stopAtTag*/,null/*ReadTerminationStrategy*/);
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * @param	name				the input file name
	 * @param	transferSyntaxUID	the transfer syntax to use for the data set (leave null for autodetection)
	 * @param	hasMeta				look for a meta information header
	 * @param	useBufferedStream	buffer the input for better performance
	 * @param	stopAtTag			the tag (in the top level data set) at which to stop
	 * @param	strategy			the ReadTerminationStrategy at which to stop
	 * @return						the byte offset at which the read stopped (which will be just past the stopAtTag or the tag read before the ReadTerminationStrategy terminated)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	private long read(String name,String transferSyntaxUID,boolean hasMeta,boolean useBufferedStream,AttributeTag stopAtTag,ReadTerminationStrategy strategy) throws IOException, DicomException {
		long byteOffset = 0;
		InputStream i = null;
		DicomInputStream di = null;
		try {
			File file = new File(name);
			i = new FileInputStream(file);
			if (useBufferedStream) {
				i = new BufferedInputStream(i);
			}
			di = new DicomInputStream(i,transferSyntaxUID,hasMeta);
			if (di.getFile() == null) {
				di.setFile(file);	// need this to allow large PixelData OX to be left on disk (000596)
			}
			byteOffset = read(di,stopAtTag,strategy);
		}
		catch (IOException e) {
			throw e;			// we do this so that the finally will execute, whether an exception or not
		}
		catch (DicomException e) {
			throw e;			// we do this so that the finally will execute, whether an exception or not
		}
		finally {
			if (di != null) {
				try {
					di.close();
				}
				catch (Exception e) {
				}
			}
			if (i != null) {
				try {
					i.close();
				}
				catch (Exception e) {
				}
			}
		}
		return byteOffset;
	}

	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * @param	name				the input file name
	 * @param	transferSyntaxUID	the transfer syntax to use for the data set (leave null for autodetection)
	 * @param	hasMeta				look for a meta information header
	 * @param	useBufferedStream	buffer the input for better performance
	 * @param	strategy			the ReadTerminationStrategy at which to stop
	 * @return						the byte offset at which the read stopped (which will be just past the tag read before the ReadTerminationStrategy terminatedy)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name,String transferSyntaxUID,boolean hasMeta,boolean useBufferedStream,ReadTerminationStrategy strategy) throws IOException, DicomException {
		return read(name,transferSyntaxUID,hasMeta,useBufferedStream,null/*stopAtTag*/,strategy);
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * @param	name				the input file name
	 * @param	transferSyntaxUID	the transfer syntax to use for the data set (leave null for autodetection)
	 * @param	hasMeta				look for a meta information header
	 * @param	useBufferedStream	buffer the input for better performance
	 * @param	stopAtTag			the tag (in the top level data set) at which to stop
	 * @return						the byte offset at which the read stopped (which will be just past the stopAtTag, if stopped)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name,String transferSyntaxUID,boolean hasMeta,boolean useBufferedStream,AttributeTag stopAtTag) throws IOException, DicomException {
		return read(name,transferSyntaxUID,hasMeta,useBufferedStream,stopAtTag,null/*ReadTerminationStrategy*/);
	}

	/**
	 * <p>Read the meta information header (if present) for the specified file and then close it.</p>
	 *
	 * @param	name	the input file name
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long readOnlyMetaInformationHeader(String name) throws IOException, DicomException {
		long byteOffset = 0;
		InputStream i = null;
		DicomInputStream di = null;
		try {
			i = new FileInputStream(name);
			i=new BufferedInputStream(i);
			di = new DicomInputStream(i,null,true/*hasMeta*/);
			byteOffset = readOnlyMetaInformationHeader(di);
		}
		catch (IOException e) {
			throw e;			// we do this so that the finally will execute, whether an exception or not
		}
		catch (DicomException e) {
			throw e;			// we do this so that the finally will execute, whether an exception or not
		}
		finally {
			if (di != null) {
				try {
					di.close();
				}
				catch (Exception e) {
				}
			}
			if (i != null) {
				try {
					i.close();
				}
				catch (Exception e) {
				}
			}
		}
		return byteOffset;
	}

	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	name	the input file name
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name) throws IOException, DicomException {
		return read(name,null,true,true);
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	name			the input file name
	 * @param	stopAtTag		the tag (in the top level data set) at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the stopAtTag, if stopped)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name,AttributeTag stopAtTag) throws IOException, DicomException {
		return read(name,null,true,true,stopAtTag);
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	name			the input file name
	 * @param	strategy		the ReadTerminationStrategy at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the tag read before the ReadTerminationStrategy terminated)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(String name,ReadTerminationStrategy strategy) throws IOException, DicomException {
		return read(name,null,true,true,null/*stopAtTag*/,strategy);
	}

	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	file	the input file
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(File file) throws IOException, DicomException {
		return read(file.getCanonicalPath());
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	file			the input file
	 * @param	stopAtTag		the tag (in the top level data set) at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the stopAtTag, if stopped)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(File file,AttributeTag stopAtTag) throws IOException, DicomException {
		return read(file.getCanonicalPath(),stopAtTag);
	}
	
	/**
	 * <p>Read an entire DICOM object in the specified file.</p>
	 *
	 * <p>Reads the attributes of both the meta information header (if present) and data set.</p>
	 *
	 * <p>Always tries to automatically detect the meta information header or transfer syntax
	 * if no meta information header and buffers the input for better performance.</p>
	 *
	 * @param	file			the input file
	 * @param	strategy		the ReadTerminationStrategy at which to stop
	 * @return					the byte offset at which the read stopped (which will be just past the tag read before the ReadTerminationStrategy terminated)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long read(File file,ReadTerminationStrategy strategy) throws IOException, DicomException {
		return read(file.getCanonicalPath(),strategy);
	}
	
	/**
	 * <p>Read the meta information header (if present) for the specified file and then close it.</p>
	 *
	 * @param	file	the input file
	 * @return			the byte offset at which the read stopped
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM parsing
	 */
	public long readOnlyMetaInformationHeader(File file) throws IOException, DicomException {
		return readOnlyMetaInformationHeader(file.getCanonicalPath());
	}
	
	/**
	 * <p>Write the entire attribute list (which may be part of a larger enclosing dataset) to the specified stream.</p>
	 *
	 * <p>Does not close the output stream, assumes any meta header vs. dataset ransition has occurred and
	 * further assumes that any additional codecs (like deflate) have already been pushed onto the stream.</p>
	 *
	 * <p>Intended for use only for write datasets that are within sequence items (hence is not public).</p>
	 *
	 * @param	dout		the stream to write to
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	protected void writeFragment(DicomOutputStream dout) throws IOException, DicomException {
		for (Attribute a : values()) {
//System.err.println("Writing "+a);
			a.write(dout);
		}
	}
	
	/**
	 * <p>Write the entire attribute list to the specified stream.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	dout		the stream to write to
	 * @param	useMeta		true if the meta information header attributes are to be written, false if they are to be ommitted
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(DicomOutputStream dout,boolean useMeta) throws IOException, DicomException {
		DeflaterOutputStream deflaterOutputStream = null;
		OutputStream bzip2OutputStream = null;
		for (Attribute a : values()) {
			boolean isDataSetAttribute = a.getTag().getGroup() > 0x0002;
			if (isDataSetAttribute) {
				// Test to see whether or not a codec needs to be pushed on the stream ... after the first time, the TransferSyntax will always be ExplicitVRLittleEndian 
//System.err.println("Testing for deflate and bzip2 TS");
				if (dout.getTransferSyntaxToWriteDataSet().isDeflated()) {
					// insert deflate into output stream and make a new DicomOutputStream
//System.err.println("Creating new DicomOutputStream from deflate");
					deflaterOutputStream = new DeflaterOutputStream(dout,new Deflater(Deflater.BEST_COMPRESSION,true/*nowrap*/));
					dout = new DicomOutputStream(deflaterOutputStream,null,TransferSyntax.ExplicitVRLittleEndian);
				}
				else if (dout.getTransferSyntaxToWriteDataSet().isBzip2ed()) {
					// insert bzip2 into output stream and make a new DicomOutputStream
//System.err.println("DicomStreamCopier.copy(): Output stream - creating new DicomOutputStream from bzip2");
					try {
						Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream");
						Class [] argTypes  = {OutputStream.class};
						Object[] argValues = {dout};
						bzip2OutputStream = (OutputStream)(classToUse.getConstructor(argTypes).newInstance(argValues));
						dout = new DicomOutputStream(bzip2OutputStream,null/*no meta-header*/,TransferSyntax.ExplicitVRLittleEndian);
					}
					catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
						throw new DicomException("Could not instantiate bzip2 codec - "+e);
					}
				}
				dout.setWritingDataSet();
			}
			if (isDataSetAttribute || useMeta) {
//System.err.println("Writing "+a);
				a.write(dout);
			}
		}
		// do not use dout.close(), since causes network activities to fail
		// a dout.flush() alone is not sufficient to flush any remaining output from any pushed codecs
		if (deflaterOutputStream != null) {
			deflaterOutputStream.finish();		// NOT close(), since we may not want to close the underlying output stream (e.g., on a network association); method is specific to java.util.zip.DeflaterOutputStream
		}
		if (bzip2OutputStream != null) {
			bzip2OutputStream.close();			// flush() alone is not sufficient :(
			//assert closeWhenDone=true;
		}
	}
	
	/**
	 * <p>Write the entire attribute list to the specified stream.</p>
	 *
	 * <p>Leaves the stream open.</p>
	 *
	 * @param	dout		the stream to write to
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(DicomOutputStream dout) throws IOException, DicomException {
		write(dout,true/*useMeta*/);
	}

	/**
	 * <p>Write the entire attribute list to the specified stream.</p>
	 *
	 * <p>Closes the stream after writing.</p>
	 *
	 * @param	o					the stream to write to
	 * @param	transferSyntaxUID	the transfer syntax to use to write the data set
	 * @param	useMeta				write the meta information header attributes (if true they must be present in the list with appropriate values already)
	 * @param	useBufferedStream	buffer the output for better performance (set this true only if the supplied stream is not already buffered)
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(OutputStream o,String transferSyntaxUID,boolean useMeta,boolean useBufferedStream) throws IOException, DicomException {
		write(o,transferSyntaxUID,useMeta,useBufferedStream,true/*closeAfterWrite*/);
	}

	/**
	 * <p>Write the entire attribute list to the specified stream.</p>
	 *
	 * @param	o					the stream to write to
	 * @param	transferSyntaxUID	the transfer syntax to use to write the data set
	 * @param	useMeta				write the meta information header attributes (if true they must be present in the list with appropriate values already)
	 * @param	useBufferedStream	buffer the output for better performance (set this true only if the supplied stream is not already buffered)
	 * @param	closeAfterWrite		requests that the supplied stream be closed after writing
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(OutputStream o,String transferSyntaxUID,boolean useMeta,boolean useBufferedStream,boolean closeAfterWrite) throws IOException, DicomException {
		if (useBufferedStream) o=new BufferedOutputStream(o);
		try {
			DicomOutputStream dout = new DicomOutputStream(o,useMeta ? TransferSyntax.ExplicitVRLittleEndian : null,transferSyntaxUID);
			write(dout,useMeta);
			dout.close();
		}
		finally {
			if (closeAfterWrite) {
				o.close();
			}
		}
	}

	/**
	 * <p>Write the entire attribute list to the named file.</p>
	 *
	 * @param	name			the file name to write to
	 * @param	transferSyntaxUID	the transfer syntax to use to write the data set
	 * @param	useMeta			write the meta information header attributes (if true they must be present in the list with appropriate values already)
	 * @param	useBufferedStream	buffer the output for better performance
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(String name,String transferSyntaxUID,boolean useMeta,boolean useBufferedStream) throws IOException, DicomException {
		OutputStream o = new FileOutputStream(name);
		write(o,transferSyntaxUID,useMeta,useBufferedStream);
	}

	/**
	 * <p>Write the entire attribute list to the named file in explicit VR little endian transfer syntax with a meta information header.</p>
	 *
	 * @deprecated				has proven to be dangerous because callers have sometimes been expecting File Meta Information Transfer Syntax to be used if present, not always explicit VR little endian
	 * @param	name			the file name to write to
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding or inconsistent TransferSyntaxUID
	 */
	public void write(String name) throws IOException, DicomException {
		String transferSyntaxUIDInFileMetaInformation = Attribute.getSingleStringValueOrEmptyString(this,TagFromName.TransferSyntaxUID);
		if (transferSyntaxUIDInFileMetaInformation.length() == 0 || transferSyntaxUIDInFileMetaInformation.equals(TransferSyntax.ExplicitVRLittleEndian)) {
			write(name,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
		}
		else {
			throw new DicomException("TransferSyntaxUID in File Meta Information is not Explicit VR Little Endian");
		}
	}

	/**
	 * <p>Write the entire attribute list to the specified file.</p>
	 *
	 * @param	file			the file to write to
	 * @param	transferSyntaxUID	the transfer syntax to use to write the data set
	 * @param	useMeta			write the meta information header attributes (if true they must be present in the list with appropriate values already)
	 * @param	useBufferedStream	buffer the output for better performance
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding
	 */
	public void write(File file,String transferSyntaxUID,boolean useMeta,boolean useBufferedStream) throws IOException, DicomException {
		OutputStream o = new FileOutputStream(file);
		write(o,transferSyntaxUID,useMeta,useBufferedStream);
	}

	/**
	 * <p>Write the entire attribute list to the specified file in explicit VR little endian transfer syntax with a meta information header.</p>
	 *
	 * @deprecated				has proven to be dangerous because callers have sometimes been expecting File Meta Information Transfer Syntax to be used if present, not always explicit VR little endian
	 * @param	file			the file to write to
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if an error in DICOM encoding or inconsistent TransferSyntaxUID
	 */
	public void write(File file) throws IOException, DicomException {
		String transferSyntaxUIDInFileMetaInformation = Attribute.getSingleStringValueOrEmptyString(this,TagFromName.TransferSyntaxUID);
		if (transferSyntaxUIDInFileMetaInformation.length() == 0 || transferSyntaxUIDInFileMetaInformation.equals(TransferSyntax.ExplicitVRLittleEndian)) {
			write(file,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
		}
		else {
			throw new DicomException("TransferSyntaxUID in File Meta Information is not Explicit VR Little Endian");
		}
	}

	/**
	 * <p>Associates the specified value (attribute) with the specified key (tag).</p>
	 *
	 * <p>If the map previously contained a mapping for this key, the old value is replaced.</p>
	 *
	 * <p>No untyped Object put(Object key, Object value) method is required to over ride the super
	 * class method since the parent TreeMap class of AttributeList is now typed, and the untyped
	 * method would have the same erasure.</p>
	 *
	 * @see java.util.TreeMap#put(Object,Object)
	 *
	 * @param	t			key (tag) with which the specified value (attribute) is to be associated
	 * @param	a			value (attribute) to be associated with the specified key (tag)
	 * @return				previous value (attribute) associated with specified key (tag), or null if there was no mapping for key (tag) 
	 * @throws	NullPointerException	thrown if a or t is null
	 * @throws	ClassCastException		thrown if a or t is not the correct class
	 */
	public Attribute put(AttributeTag t, Attribute a) throws NullPointerException,ClassCastException {
		if (a == null || t == null) {
			throw new NullPointerException();
		}
		else {
			return super.put(t,a);	// do not need cast to Attribute since super class is now typed
		}
	}


	/**
	 * <p>Associates the specified value (attribute) with the key that is the existing tag of the attribute.</p>
	 *
	 * <p>If the map previously contained a mapping for this key, the old value is replaced.</p>
	 *
	 * @see #put(AttributeTag,Attribute)
	 *
	 * @param	a			value (attribute) to be associated with the specified key (tag)
	 * @return				previous value (attribute) associated with specified key (tag), or null if there was no mapping for key (tag) 
	 * @throws	NullPointerException	thrown if a or t is null
	 */
	public Attribute put(Attribute a) {
		if (a == null) {
			throw new NullPointerException();
		}
		else {
			return put(a.getTag(),a);
		}
	}
	
	/**
	 * <p>Returns the value (attribute) to which this map maps the specified key (tag).</p>
	 *
	 * <p>Returns null if the map contains no mapping for this key. A return value of null
	 * does indicate that the map contains no mapping for the key, unlike {@link java.util.TreeMap#get(Object) java.util.get(Object)}
	 * since the put operation checks for and disallows null insertions. This contract will hold
	 * true unless one goes to great effort to insert a key that maps to a null value
	 * by using one of the other insertion methods of the super class, in which case
	 * other operations (like writing) may fail later with a NullPointerException.</p>
	 *
	 * @param	t	key (tag) whose associated value (attribute) is to be returned
	 * @return		the Attribute or null if not found
	 */
	public Attribute get(AttributeTag t) {
		return super.get(t);	// do not need cast to Attribute since super class is now typed
	}

	/**
	 * <p>Determine whether or not this list is an image.</p>
	 *
	 * <p>An image is defined to be something with a PixelData attribute at the top level.</p>
	 *
	 * @return	true if an image 
	 */
	public boolean isImage() {
		return getPixelData() != null;
	}

	/**
	 * <p>Determine whether or not this list is an enhanced instance.</p>
	 *
	 * <p>An enhanced instance is defined to be something with a Shared or Per-Frame Functional Groups Sequence attribute at the top level.</p>
	 *
	 * @return	true if an enhanced instance 
	 */
	public boolean isEnhanced() {
		return get(TagFromName.SharedFunctionalGroupsSequence) != null || get(TagFromName.PerFrameFunctionalGroupsSequence) != null;
	}

	/**
	 * <p>Determine whether or not this list is an SR Document.</p>
	 *
	 * <p>An SR Document is defined to be something with a ContentSequence attribute at the top level.</p>
	 *
	 * @see com.pixelmed.dicom.SOPClass#isStructuredReport(String)
	 *
	 * @return	true if an SR Document 
	 */
	public boolean isSRDocument() {
		return get(TagFromName.ContentSequence) != null;
	}

	/**
	 * <p>Get the dictionary in use for this list.</p>
	 *
	 * <p>Creates one if necessary.</p>
	 *
	 * @return	the dictionary 
	 */
	public static DicomDictionary getDictionary() {
		createDictionaryifNecessary();
		return dictionary;
	}
	
	/**
	 * <p>Removes the mapping for this key (tag), if present.</p>
	 *
	 * @param	tag	key (tag) for which mapping should be removed
	 * @return		previous value (attribute) associated with specified key (tag), or null if there was no mapping for key (tag) 
	 */
	public Attribute remove(AttributeTag tag) {
		return (Attribute)(super.remove(tag));
	}

	
	/**
	 * <p>Removes the mapping for this key (tag), if present, in this and any contained lists.</p>
	 *
	 * <p>I.e., recurses into sequences.</p>
	 *
	 * @param	tag	key (tag) for which mapping should be removed
	 */
	public void removeRecursively(AttributeTag tag) {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a instanceof SequenceAttribute) {
				Iterator<SequenceItem> i2 = ((SequenceAttribute)a).iterator();
				while (i2.hasNext()) {
					SequenceItem item = i2.next();
					if (item != null) {
						AttributeList list = item.getAttributeList();
						if (list != null) {
							list.remove(tag);
						}
					}
				}
			}
		}
		remove(tag);
	}

	/**
	 * <p>Removes all of the data elements in the specified standard or private group.</p>
	 *
	 * @param	group	group for which all elements should be removed
	 */
	public void removeGroup(int group) {
		Iterator<AttributeTag> i = keySet().iterator();
		while (i.hasNext()) {
			AttributeTag t = i.next();
			if (t.getGroup() == group) {
				i.remove();
			}
		}
	}

	// useful list handling routines beyond those inherited from TreeMap
	
	/**
	 * <p>Replaces an attribute with a zero length attribute, if present in the list.</p>
	 *
	 * <p>Does nothing if the attribute was not already present.</p>
	 *
	 * @param	tag		key (tag) for which the attribute should be replaced
	 * @throws	DicomException	thrown if there is any difficulty creating the new zero length attribute
	 */
	public void replaceWithZeroLengthIfPresent(AttributeTag tag) throws DicomException {
		Object o=get(tag);
		if (o != null) {
			//remove(tag);
			Attribute a = AttributeFactory.newAttribute(tag,getDictionary().getValueRepresentationFromTag(tag));
			put(tag,a);
		}
	}
	
	/**
	 * <p>Replaces an attribute with a new value, if present in the list, otherwise adds it.</p>
	 *
	 * @param	tag			key (tag) for which the attribute should be replaced
	 * @param	value		the value to use
	 * @throws	DicomException	thrown if there is any difficulty creating the new zero length attribute
	 */
	public void replace(AttributeTag tag,String value) throws DicomException {
		Attribute a = AttributeFactory.newAttribute(tag,getDictionary().getValueRepresentationFromTag(tag));
		a.addValue(value);
		put(tag,a);
	}
	
	/**
	 * <p>Replaces an attribute with a new value, if present in the list.</p>
	 *
	 * <p>Does nothing if the attribute was not already present.</p>
	 *
	 * @param	tag			key (tag) for which the attribute should be replaced
	 * @param	value		the  value to use
	 * @throws	DicomException	thrown if there is any difficulty creating the new zero length attribute
	 */
	public void replaceWithValueIfPresent(AttributeTag tag,String value) throws DicomException {
		Object o=get(tag);
		if (o != null) {
			Attribute a = AttributeFactory.newAttribute(tag,getDictionary().getValueRepresentationFromTag(tag));
			a.addValue(value);
			put(tag,a);
		}
	}
	
	/**
	 * <p>Replaces an attribute with a dummy value, if present in the list.</p>
	 *
	 * <p>Does nothing if the attribute was not already present.</p>
	 *
	 * @deprecated	See {@link com.pixelmed.dicom.AttributeList#replaceWithValueIfPresent(AttributeTag,String) replaceWithValueIfPresent()}
	 *
	 * @param	tag			key (tag) for which the attribute should be replaced
	 * @param	dummyValue	the dummy value to use
	 * @throws	DicomException	thrown if there is any difficulty creating the new zero length attribute
	 */
	public void replaceWithDummyValueIfPresent(AttributeTag tag,String dummyValue) throws DicomException {
		replaceWithValueIfPresent(tag,dummyValue);
	}

	// list management methods ...
	
	/**
	 * <p>Remove any private attributes present in the list.</p>
	 *
	 * <p>Private attributes are all those with an odd group number.</p>
	 *
	 * <p>Also recurses into standard sequences and removes any private attributes therein.</p>
	 */
	public void removePrivateAttributes() {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a.getTag().isPrivate()) {
				i.remove();
			}
			else if (a instanceof SequenceAttribute) {
				Iterator<SequenceItem> i2 = ((SequenceAttribute)a).iterator();
				while (i2.hasNext()) {
					SequenceItem item = i2.next();
					if (item != null) {
						AttributeList list = item.getAttributeList();
						if (list != null) {
							list.removePrivateAttributes();
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>Remove unsafe private attributes present in the list.</p>
	 *
	 * <p>Unsafe private attributes are all those with an odd group number that are not known to be safe,
	 * in the sense that they do not contain individually identifiable information.</p>
	 *
	 * <p>Will not remove private creators of potentially safe private tags, even if there are no such safe tags found.</p>
	 *
	 * <p>Also recurses into standard sequences and removes any unsafe private attributes therein.</p>
	 *
	 * @see com.pixelmed.dicom.ClinicalTrialsAttributes
	 */
	public void removeUnsafePrivateAttributes() {
		Set privateCreatorTagsForSafePrivateAttributes = new HashSet();	// of AttributeTag
		// 1st pass ... remove the unsafe private tags themselves
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			AttributeTag t = a.getTag();
//System.err.println("AttributeList.removeUnsafePrivateAttributes(): "+a);
			boolean removed = false;
			if (t.isPrivate()) {
				if (ClinicalTrialsAttributes.isSafePrivateAttribute(t,this)) {	// creators are safe
//System.err.println("AttributeList.removeUnsafePrivateAttributes(): is safe "+t);
					// leave the private attribute in place, and keep track of its private creator, so that we don't remove it later
					AttributeTag creatorTag = getPrivateCreatorTag(t);
//System.err.println("AttributeList.removeUnsafePrivateAttributes(): adding creatorTag to set to retain "+creatorTag);
					privateCreatorTagsForSafePrivateAttributes.add(creatorTag);
				}
				else {
//System.err.println("AttributeList.removeUnsafePrivateAttributes(): is unsafe "+t);
					i.remove();
					removed = true;
				}
			}
			// recurse into sequence if standard, or if private tag that was not removed, since safe private sequences may contain unsafe content !
			if (!removed && a instanceof SequenceAttribute) {
				Iterator<SequenceItem> i2 = ((SequenceAttribute)a).iterator();
				while (i2.hasNext()) {
					SequenceItem item = i2.next();
					if (item != null) {
						AttributeList list = item.getAttributeList();
						if (list != null) {
							list.removeUnsafePrivateAttributes();
						}
					}
				}
			}
		}
		// 2nd pass ... remove any private creator that is not used anymore (i.e., had no safe tags)
		i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			AttributeTag t = a.getTag();
			if (t.isPrivateCreator() && !privateCreatorTagsForSafePrivateAttributes.contains(t)) {
//System.err.println("AttributeList.removeUnsafePrivateAttributes(): removing unused creator "+t);
				i.remove();
			}
		}
	}
	
	/**
	 * <p>Get the private creator of a private tag.</p>
	 *
	 * @param	tag	the private tag
	 * @return		the private creator tag
	 */
	public AttributeTag getPrivateCreatorTag(AttributeTag tag) {
		int group = tag.getGroup();
		int element = tag.getElement();
		int block = (element & 0xff00) >> 8;
		return new AttributeTag(group,block);
	}
	
	/**
	 * <p>Get the private creator of a private tag.</p>
	 *
	 * <p>Any leading or trailing white space padding is removed</p>
	 *
	 * @param	tag	the private tag
	 * @return		the private creator, or an empty String if not found
	 */
	public String getPrivateCreatorString(AttributeTag tag) {
//System.err.println("AttributeList.getPrivateCreatorString(): for tag "+tag);
		AttributeTag creatorTag = getPrivateCreatorTag(tag);
//System.err.println("AttributeList.getPrivateCreatorString(): creatorTag is "+creatorTag);
		String creator = Attribute.getSingleStringValueOrEmptyString(this,creatorTag).trim();
//System.err.println("AttributeList.getPrivateCreatorString(): creator is "+creator);
		return creator;
	}
	
	/**
	 * <p>Returns the private data element specified by the tag and creator.</p>
	 *
	 * <p>Only the lower 8 bits of the specified tag are considered.</p>
	 *
	 * @param	t		tag whose associated attribute is to be returned
	 * @param	creator	private creator (owner of block of private data elements)
	 * @return			the Attribute or null if not found
	 */
	public Attribute get(AttributeTag t,String creator) {
		Attribute a = null;
		if (t.isPrivate()) {
			int g = t.getGroup();
			for (int block=0; block<0x0100; ++block) {
				String creatorCandidate = Attribute.getSingleStringValueOrEmptyString(this,new AttributeTag(g,block));
				if (creatorCandidate.equals(creator)) {
					a = get(new AttributeTag(g,(block<<8)|(t.getElement()&0x00ff)));
					break;		// should only occur once, but return "first" occurrence regardless
				}
			}
		}
		return a;
	}

	/**
	 * <p>Returns the Attribute that contains the Pixel Data.</p>
	 *
	 * <p>Handles standard and various private float or double alternatives to the conventional (0x7FE0,0x0010).</p>
	 *
	 * @return			the Attribute or null if not found
	 */
	public Attribute getPixelData() {
		return PrivatePixelData.getPixelData(this);
	}
	
	/**
	 * <p>Remove any meta information header attributes present in the list.</p>
	 *
	 * <p>Meta information header attributes are all those in group 0x0002.</p>
	 *
	 * <p>Note that this should always be done when modifying the SOP Class or
	 * Instance UID of an attribute list what has been read before writing,
	 * since it is vital that the corresponding meta information header attributes
	 * match those in the data set.</p>
	 *
	 * @see com.pixelmed.dicom.FileMetaInformation
	 */
	public void removeMetaInformationHeaderAttributes() {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a.getTag().getGroup() == 0x0002) {
				i.remove();
			}
		}
	}
	
	/**
	 * <p>Remove any group length attributes present in the list, except the meta information header length, as well as LengthToEnd.</p>
	 *
	 * <p>Group length attributes are all those with an element of 0x0000.</p>
	 *
	 * <p>LengthToEnd (0x0008,0x0001) is always removed if present as well.</p>
	 *
	 * <p>These have never been required in DICOM and are a holdover from the old
	 * ACR-NEMA days, and are a source of constant problems, so should always
	 * be removed.</p>
	 *
	 * <p>The meta information header length is left alone, since it is mandatory.</p>
	 *
	 * <p>Also recurses into sequences and removes any contained group lengths.</p>
	 *
	 * @see com.pixelmed.dicom.FileMetaInformation
	 */
	public void removeGroupLengthAttributes() {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a instanceof SequenceAttribute) {
				Iterator<SequenceItem> i2 = ((SequenceAttribute)a).iterator();
				while (i2.hasNext()) {
					SequenceItem item = i2.next();
					if (item != null) {
						AttributeList list = item.getAttributeList();
						if (list != null) {
							list.removeGroupLengthAttributes();
						}
					}
				}
			}
			else {
				AttributeTag t = a.getTag();
				if (t.getElement() == 0x0000 && t.getGroup() != 0x0002) i.remove();	// leave metaheader alone
			}
		}
		remove(TagFromName.LengthToEnd);
	}

	/**
	 * <p>Remove any overlay attributes present in the list.</p>
	 *
	 * <p>Overlay attributes are those for which {@link com.pixelmed.dicom.AttributeTag#isOverlayGroup() com.pixelmed.dicom.AttributeTag.isOverlayGroup()} returns true.</p>
	 *
	 * <p>Note that any overlay data in the high bits of the PixelData are NOT removed by this method
	 * but can be removed by reading the <code>PixelData</code> into a {@link com.pixelmed.display.SourceImage com.pixelmed.display.SourceImage} and creating a new <code>PixelData</code> attribute from it.</p>
	 */
	public void removeOverlayAttributes() {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a.getTag().isOverlayGroup()) {
				i.remove();
			}
		}
	}

	/**
	 * <p>Remove any curve attributes present in the list.</p>
	 *
	 * <p>Curve attributes are those for which {@link com.pixelmed.dicom.AttributeTag#isCurveGroup() com.pixelmed.dicom.AttributeTag.isCurveGroup()} returns true.</p>
	 */
	public void removeCurveAttributes() {
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			Attribute a = i.next();
			if (a.getTag().isCurveGroup()) {
				i.remove();
			}
		}
	}
	
	// Miscellaneous methods ...
	
	/**
	 * <p>Dump the contents of the attribute list as a human-readable string.</p>
	 *
	 * <p>Each attribute is written to a separate line, in the form defined
	 * for {@link com.pixelmed.dicom.Attribute#toString() com.pixelmed.dicom.Attribute.toString()}.</p>
	 *
	 * @return			the string
	 */
	public String toString() {
		StringBuffer str = new StringBuffer();
		createDictionaryifNecessary();

		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			str.append(((Attribute)i.next()).toString(dictionary));
			str.append("\n");
		}
		return str.toString();
	}

	/**
	 * <p>Dump the contents of the attribute list as a human-readable string.</p>
	 *
	 * <p>Each attribute is written to a separate line, in the form defined
	 * for {@link com.pixelmed.dicom.Attribute#toString(DicomDictionary dictionary) com.pixelmed.dicom.Attribute.toString(DicomDictionary dictionary)}.</p>
	 *
	 * @param	dictionary	the dictionary to use to look up the name
	 * @return			the string
	 */
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();

		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			str.append(((Attribute)i.next()).toString(dictionary));
			str.append("\n");
		}
		return str.toString();
	}


	/**
	 * <p>Get all the string values for the attribute list, separated by the specified delimiter.</p>
	 *
	 * <p>If there is no string value for an individual value or an exception trying to fetch it, the supplied default is returned for each Attribute.</p>
	 *
	 * <p>A canonicalized (unpadded) form is returned for each Attribute value, not the original string.</p>
	 *
	 * @param	dflt		what to return if there are no (valid) string values
	 * @param	format		the format to use for each numerical or decimal value (null if none)
	 * @param	delimiter	the delimiter to use between each value
	 * @return				the values as a delimited {@link java.lang.String String}
	 */
	public String getDelimitedStringValuesOrDefault(String dflt,NumberFormat format,String delimiter) {
		StringBuffer str = new StringBuffer();
		String separator = "";
		Iterator<Attribute> i = values().iterator();
		while (i.hasNext()) {
			str.append(separator);
			str.append(((Attribute)i.next()).getDelimitedStringValuesOrDefault(dflt,format));
			separator = delimiter;
		}
		return str.toString();
	}

	/**
	 * <p>Change the file containing the data used by any attribute whose values are left on disk, for example if the file has been renamed.</p>
	 *
	 * @param	file	the new file containing the data
	 */
	public void setFileUsedByOnDiskAttributes(File file) {
//System.err.println("AttributeList.setFileUsedByOnDiskAttributes(): file = "+file);
		for (Attribute a : values()) {
//System.err.println("AttributeList.setFileUsedByOnDiskAttributes(): checking "+a.getClass()+" - "+a.toString(dictionary));
			if (a instanceof OtherByteAttributeOnDisk) {
//System.err.println("AttributeList.setFileUsedByOnDiskAttributes(): setting OtherByteAttributeOnDisk to file = "+file);
				((OtherByteAttributeOnDisk)a).setFile(file);
			}
			else if (a instanceof OtherWordAttributeOnDisk) {
//System.err.println("AttributeList.setFileUsedByOnDiskAttributes(): setting OtherWordAttributeOnDisk to file = "+file);
				((OtherWordAttributeOnDisk)a).setFile(file);
			}
		}
	}

	/**
	 * <p>Create a new attribute with the specified tag and insert it in the map associating the generated attribute with the specified tag as the key.</p>
	 *
	 * <p>If the map previously contained a mapping for the tag (key), the old value is replaced.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements).</p>
	 *
	 * @param	t						key ({@link com.pixelmed.dicom.AttributeTag AttributeTag} tag) with which the generated attribute is to be associated
	 * @param	specificCharacterSet	the {@link com.pixelmed.dicom.SpecificCharacterSet SpecificCharacterSet} to be used text values
	 * @return							the newly created attribute 
	 * @throws	DicomException		if cannot create attribute, such as if cannot find tag in dictionary
	 */
	public Attribute putNewAttribute(AttributeTag t,SpecificCharacterSet specificCharacterSet) throws DicomException {
		Attribute a = null;
		byte[] vr = getDictionary().getValueRepresentationFromTag(t);
		if (vr == null) {
			throw new DicomException("No such data element as "+t+" in dictionary");
		}
		else {
			a = AttributeFactory.newAttribute(t,vr,specificCharacterSet);
			if (a == null) {
				throw new DicomException("Could not create attribute for tag "+t);
			}
			else {
				super.put(t,a);
			}
		}
		return a;
	}

	/**
	 * <p>Create a new attribute with the specified tag and insert it in the map associating the generated attribute with the specified tag as the key.</p>
	 *
	 * <p>If the map previously contained a mapping for the tag (key), the old value is replaced.</p>
	 *
	 * <p>The PixelRepresentation is assumed to be unsigned (US will be used for US/SS VR data elements).</p>
	 *
	 * @param	t						key ({@link com.pixelmed.dicom.AttributeTag AttributeTag} tag) with which the generated attribute is to be associated
	 * @return							the newly created attribute 
	 * @throws	DicomException		if cannot create attribute, such as if cannot find tag in dictionary
	 */
	public Attribute putNewAttribute(AttributeTag t) throws DicomException {
		return putNewAttribute(t,null);
	}

	/**
	 * <p>Set the SpecificCharacterSet suitable for all of the string attributes of a dataset in an AttributeList.</p>
	 *
	 * <p>Any existing SpecificCharacterSet for Attributes (whether read or created de novo) is replaced.</p>
	 *
	 * <p>Recurses into SequenceAttributes.</p>
	 *
	 * @param	specificCharacterSet	the SpecificCharacterSet sufficient to encode all values
	 */
	public void setSpecificCharacterSet(SpecificCharacterSet specificCharacterSet) {
		if (specificCharacterSet != null) {
			Iterator<Attribute> it = values().iterator();
			while (it.hasNext()) {
				Attribute a = it.next();
				if (a != null) {
					if (a instanceof SequenceAttribute) {
						Iterator<SequenceItem> is = ((SequenceAttribute)a).iterator();
						while (is.hasNext()) {
							SequenceItem item = is.next();
							if (item != null) {
								AttributeList list = item.getAttributeList();
								if (list != null) {
									list.setSpecificCharacterSet(specificCharacterSet);
								}
							}
						}
					}
					else if (a instanceof StringAttributeAffectedBySpecificCharacterSet) {
						((StringAttributeAffectedBySpecificCharacterSet)a).setSpecificCharacterSet(specificCharacterSet);
					}
				}
			}
		}
	}

	/**
	 * <p>Get a new SpecificCharacterSet suitable for encoding all of the values of the string attributes of a dataset.</p>
	 *
	 * <p>Any existing SpecificCharacterSet within the AttributeList or any SpecificCharacterSet established for specific Attributes (e.g., when read or created de novo) is ignored.</p>
	 *
	 * <p>Only a single value is ever used (i.e., ISO 2022 escapes are not created).</p>
	 *
	 * @return	a new SpecificCharacterSet sufficient to encode all values
	 */
	public SpecificCharacterSet getSuitableSpecificCharacterSetForAllStringValues() {
		return new SpecificCharacterSet(this);
	}

	/**
	 * <p>Insert a new SpecificCharacterSet suitable for encoding all of the values of the string attributes of a dataset into the AttributeList.</p>
	 *
	 * <p>Any existing SpecificCharacterSet within the AttributeList or any SpecificCharacterSet established for specific Attributes (e.g., when read or created de novo) is ignored.</p>
	 *
	 * <p>Only a single value is ever used (i.e., ISO 2022 escapes are not created).</p>
	 *
	 * <p>If the encoding is ASCII, which is the default, the SpecificCharacterSet is removed (since it is Type 1C).</p>
	 *
	 * @throws	DicomException		if cannot create attribute
	 */
	public void insertSuitableSpecificCharacterSetForAllStringValues() throws DicomException {
		SpecificCharacterSet specificCharacterSet = getSuitableSpecificCharacterSetForAllStringValues();
//System.err.println("AttributeList.insertSuitableSpecificCharacterSetForAllStringValues(): specificCharacterSet = "+specificCharacterSet);
		if (specificCharacterSet != null) {
			String specificCharacterSetValue = specificCharacterSet.getValueToUseInSpecificCharacterSetAttribute();
//System.err.println("AttributeList.insertSuitableSpecificCharacterSetForAllStringValues(): specificCharacterSetValue = "+specificCharacterSetValue);
			if (specificCharacterSetValue != null && specificCharacterSetValue.length() > 0) {
				putNewAttribute(TagFromName.SpecificCharacterSet).addValue(specificCharacterSetValue);
			}
			else {
				remove(TagFromName.SpecificCharacterSet);
			}
			setSpecificCharacterSet(specificCharacterSet);
		}
	}
	
	protected boolean pixelDataWasActuallyDecompressed = false;	// set if actually decompressed during reading of Pixel Data attribute in this AttributeList instance (as distinct from able to be decompressed on the fly)
	protected boolean pixelDataIsLossy = false;					// set if decompressed from lossy transfer syntax during reading of Pixel Data attribute in this AttributeList instance
	protected String lossyMethod = null;
	protected double compressionRatio = 0;						// zero is an invalid ratio, so is a flag that it has not been set
	protected IIOMetadata[] iioMetadata = null;					// will be set during compression if reader is capable of it
	
	protected boolean colorSpaceWasConvertedToRGBDuringDecompression = false;	// set if color space was converted to RGB during compression
	private boolean colorSpaceWillBeConvertedToRGBDuringDecompression = false;

	private boolean isJPEGFamily = false;
	private boolean isRLE = false;
	
	private String readerWanted = null;
	
	// compare this to CompressedFrameDecoder.chooseReaderWantedBasedOnTransferSyntax(), which handles JPEG only, whereas here we handle RLE too
	private void extractCompressedPixelDataCharacteristics(TransferSyntax ts) {
		String tsuid = ts.getUID();
		isJPEGFamily = ts.isJPEGFamily();
		isRLE = tsuid.equals(TransferSyntax.RLE);

		colorSpaceWasConvertedToRGBDuringDecompression = false;
		colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// do not set this generally ... be specific to each scheme (00704)
		pixelDataIsLossy=false;
		lossyMethod=null;
		compressionRatio=0;
		readerWanted = null;
//System.err.println("AttributeList.read(): TransferSyntax = "+tsuid);
		if (isRLE) {
			// leave colorSpaceWillBeConvertedToRGBDuringDecompression false;	// (000832)
//System.err.println("Undefined length encapsulated Pixel Data in RLE");
		}
		else if (isJPEGFamily) {
			if (tsuid.equals(TransferSyntax.JPEGBaseline) || tsuid.equals(TransferSyntax.JPEGExtended)) {
				readerWanted="JPEG";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				pixelDataIsLossy=true;
				lossyMethod="ISO_10918_1";
//System.err.println("Undefined length encapsulated Pixel Data in JPEG Lossy");
			}
			else if (tsuid.equals(TransferSyntax.JPEG2000)) {
				readerWanted="JPEG2000";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				pixelDataIsLossy=true;
				lossyMethod="ISO_15444_1";
//System.err.println("Undefined length encapsulated Pixel Data in JPEG 2000");
			}
			else if (tsuid.equals(TransferSyntax.JPEG2000Lossless)) {
				readerWanted="JPEG2000";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
//System.err.println("Undefined length encapsulated Pixel Data in JPEG 2000");
			}
			else if (tsuid.equals(TransferSyntax.JPEGLossless) || tsuid.equals(TransferSyntax.JPEGLosslessSV1)) {
				readerWanted="jpeg-lossless";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// NB. (00704)
//System.err.println("Undefined length encapsulated Pixel Data in JPEG Lossless");
			}
			else if (tsuid.equals(TransferSyntax.JPEGLS)) {
				readerWanted="jpeg-ls";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// Hmmm :(
//System.err.println("Undefined length encapsulated Pixel Data in JPEG-LS");
			}
			else if (tsuid.equals(TransferSyntax.JPEGNLS)) {
				readerWanted="jpeg-ls";
				colorSpaceWillBeConvertedToRGBDuringDecompression = false;		// Hmmm :(
				pixelDataIsLossy=true;
				lossyMethod="ISO_14495_1";
//System.err.println("Undefined length encapsulated Pixel Data in JPEG-LS");
			}
			else {
				readerWanted="JPEG";
				colorSpaceWillBeConvertedToRGBDuringDecompression = true;
				slf4jlogger.warn("Unrecognized JPEG family Transfer Syntax "+tsuid+" for encapsulated PixelData - guessing "+readerWanted);
			}
		}
		else {
			slf4jlogger.warn("Unrecognized Transfer Syntax "+tsuid+" for encapsulated PixelData - cannot find reader");
		}
	}
	
	/**
	 * <p>Returns a reference to the {@link javax.imageio.metadata.IIOMetadata IIOMetadata} object for the selected frame, or null if none was available during reading. </p>
	 *
	 * @param	frame	the frame number, from 0
	 * @return	an {@link javax.imageio.metadata.IIOMetadata IIOMetadata} object, or null.
	 */
	public IIOMetadata getIIOMetadata(int frame) {
		return iioMetadata == null ? null : iioMetadata[frame];
	}
	
	/**
	 * <p>Update the lossy image compression history attributes if a lossy compressed input transfer syntax that was actually decompressed during reading of AttributeList or will be if compressed frames are decompressed.</p>
	 *
	 * <p> E.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * <p>Does nothing (is harmless) if the input was not compressed or not multi-component.</p>
	 *
	 * <p>Recurses into sequences in case there is icon pixel data that was also decompressed.</p>
	 *
	 * @param	deferredDecompression	true if decompressing compressed frames later rather than during reading of AttributeList
	 * @throws	DicomException		if cannot create replacement attribute
	 */
	public void insertLossyImageCompressionHistoryIfDecompressed(boolean deferredDecompression) throws DicomException {
		if ((pixelDataWasActuallyDecompressed || deferredDecompression) && pixelDataIsLossy) {
			{ Attribute a = new CodeStringAttribute(TagFromName.LossyImageCompression); a.addValue("01"); put(a); }
			if (lossyMethod != null && lossyMethod.length() > 0) {
				Attribute aLossyImageCompressionMethod = get(TagFromName.LossyImageCompressionMethod);
				if (aLossyImageCompressionMethod == null) {
					aLossyImageCompressionMethod = new CodeStringAttribute(TagFromName.LossyImageCompressionMethod);
					put(aLossyImageCompressionMethod);
				}
				int valueNumberBeingEdited = 0;
				if (aLossyImageCompressionMethod.getVM() == 0) {
					aLossyImageCompressionMethod.addValue(lossyMethod);
					valueNumberBeingEdited = 0;
				}
				else {
					String[] values = aLossyImageCompressionMethod.getStringValues();
					if (values.length > 0 && values[values.length-1] != null && !values[values.length-1].equals(lossyMethod)) {
						valueNumberBeingEdited = values.length;
						aLossyImageCompressionMethod.addValue(lossyMethod);
					}
				}
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): aLossyImageCompressionMethod= "+aLossyImageCompressionMethod);
				
				{
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): valueNumberBeingEdited= "+valueNumberBeingEdited);
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): compressionRatio= "+compressionRatio);
					Attribute aLossyImageCompressionRatio = get(TagFromName.LossyImageCompressionRatio);
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): aLossyImageCompressionRatio at start = "+aLossyImageCompressionRatio);
					if (aLossyImageCompressionRatio == null) {
						aLossyImageCompressionRatio = new DecimalStringAttribute(TagFromName.LossyImageCompressionRatio);
						put(aLossyImageCompressionRatio);
					}
					if (aLossyImageCompressionRatio.getVM() <= valueNumberBeingEdited) {
						while (--valueNumberBeingEdited > 0) {
							aLossyImageCompressionRatio.addValue("");
						}
						if (compressionRatio > 0) {
							aLossyImageCompressionRatio.addValue(compressionRatio);
						}
						else {
							// don't have a CR to insert, but need to keep values in sequence
							aLossyImageCompressionRatio.addValue("");
						}
					}
					else if (aLossyImageCompressionRatio.getVM() == valueNumberBeingEdited + 1) {	// special case ... there is already a compression ratio for same method ... check it is good else use ours
						String[] values = aLossyImageCompressionRatio.getStringValues();
						if (values[valueNumberBeingEdited].length() == 0 || values[valueNumberBeingEdited].equals("0")) {	// sometimes encounter values of 0
							if (compressionRatio > 0) {
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): replacing empty or 0 existing value with compressionRatio = "+compressionRatio);
								aLossyImageCompressionRatio.setValue(Double.toString(compressionRatio));
							}
						}
					}
//System.err.println("AttributeList.insertLossyImageCompressionHistoryIfDecompressed(): aLossyImageCompressionRatio= "+aLossyImageCompressionRatio);
				}
			}
		}
	}

	
	/**
	 * <p>Update the lossy image compression history attributes if a lossy compressed input transfer syntax was decompress during reading.</p>
	 *
	 * <p> E.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * <p>Does nothing (is harmless) if the input was not compressed or not multi-component.</p>
	 *
	 * <p>Recurses into sequences in case there is icon pixel data that was also decompressed.</p>
	 *
	 * @throws	DicomException		if cannot create replacement attribute
	 */
	public void insertLossyImageCompressionHistoryIfDecompressed() throws DicomException {
		insertLossyImageCompressionHistoryIfDecompressed(false/*deferredDecompression*/);
	}
	
	/**
	 * <p>Get the corrected PhotometricInterpretation iff the color space was a compressed input transfer syntax that was or would be decompressed during reading.</p>
	 *
	 * <p> E.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * @return							the corrected PhotometricInterpretation, or original PhotometricInterpretation if it does not need correction
	 * @throws	DicomException		if cannot create replacement attribute
	 */
	public String getDecompressedPhotometricInterpretation() throws DicomException {
		String vPhotometricInterpretation = Attribute.getSingleStringValueOrEmptyString(this,TagFromName.PhotometricInterpretation);
		return getDecompressedPhotometricInterpretation(vPhotometricInterpretation);
	}
		
	
	/**
	 * <p>Get the corrected PhotometricInterpretation iff the color space was a compressed input transfer syntax that was actually decompressed during reading of AttributeList or will be if compressed frames are decompressed.</p>
	 *
	 * <p> E.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * @param	vPhotometricInterpretation	the PhotometricInterpretation supplied in the AttributeList
	 * @return								the corrected PhotometricInterpretation, or original PhotometricInterpretation if it does not need correction
	 */
	public String getDecompressedPhotometricInterpretation(String vPhotometricInterpretation) {
//System.err.println("AttributeList.getDecompressedPhotometricInterpretation(): vPhotometricInterpretation = "+vPhotometricInterpretation);
//System.err.println("AttributeList.getDecompressedPhotometricInterpretation(): colorSpaceWillBeConvertedToRGBDuringDecompression = "+colorSpaceWillBeConvertedToRGBDuringDecompression);
		if (colorSpaceWillBeConvertedToRGBDuringDecompression) {
			if (vPhotometricInterpretation.equals("YBR_FULL_422")
			 || vPhotometricInterpretation.equals("YBR_FULL")
			 || vPhotometricInterpretation.equals("YBR_PARTIAL_422")
			 || vPhotometricInterpretation.equals("YBR_PARTIAL_420")
			 || vPhotometricInterpretation.equals("YBR_RCT")
			 || vPhotometricInterpretation.equals("YBR_ICT")) {
//System.err.println("AttributeList.getDecompressedPhotometricInterpretation(): changing to RGB");
				vPhotometricInterpretation = "RGB";
			}
		}
//System.err.println("AttributeList.getDecompressedPhotometricInterpretation(): returning = "+vPhotometricInterpretation);
		return vPhotometricInterpretation;
	}
	
	/**
	 * <p>Correct the PhotometricInterpretation and Planar Configuration iff pixel data in a compressed input transfer syntax was actually decompressed during reading of AttributeList or will be if compressed frames are decompressed.</p>
	 *
	 * <p>If the color space was or will be converted, change the PhotometricInterpretation, e.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * <p>If the pixel data was or will be decompressed, if JPEG (not RLE) change the PlanarConfiguration to 0 if not already (i.e., color-by-pixel not color-by-plane).</p>
	 *
	 * <p>Does nothing (is harmless) if the input was not (or will not be) decompressed or is not multi-component.</p>
	 *
	 * <p>Recurses into sequences in case there is icon pixel data that was also (or will be) decompressed.</p>
	 *
	 * @param	deferredDecompression	true if decompressing compressed frames later rather than during reading of AttributeList
	 * @throws	DicomException			if cannot create replacement attribute
	 */
	public void correctDecompressedImagePixelModule(boolean deferredDecompression) throws DicomException {
		if (Attribute.getSingleIntegerValueOrDefault(this,TagFromName.SamplesPerPixel,0) > 1) {
			if (pixelDataWasActuallyDecompressed && colorSpaceWasConvertedToRGBDuringDecompression || deferredDecompression && colorSpaceWillBeConvertedToRGBDuringDecompression) {
				String vPhotometricInterpretation = Attribute.getSingleStringValueOrEmptyString(this,TagFromName.PhotometricInterpretation);
				String newPhotometricInterpretation = getDecompressedPhotometricInterpretation(vPhotometricInterpretation);
				if (!vPhotometricInterpretation.equals(newPhotometricInterpretation)) {
					slf4jlogger.debug("correctDecompressedImagePixelModule(): changing PhotometricInterpretation from {} to RGB",vPhotometricInterpretation);
					Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(newPhotometricInterpretation); put(a);
				}
			}
			if ((pixelDataWasActuallyDecompressed || deferredDecompression) && isJPEGFamily) {
				slf4jlogger.debug("correctDecompressedImagePixelModule(): setting PlanarConfiguration to 0 (was {})",Attribute.getSingleStringValueOrEmptyString(this,TagFromName.PlanarConfiguration));
				{ Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue(0); put(a); }	// output of JIIO codecs is always interleaved (?) regardless of old header, but not RLE
			}
		}
		Iterator<Attribute> it = values().iterator();
		while (it.hasNext()) {
			Attribute a = it.next();
			if (a != null) {
				if (a instanceof SequenceAttribute) {
					Iterator<SequenceItem> is = ((SequenceAttribute)a).iterator();
					while (is.hasNext()) {
						SequenceItem item = is.next();
						if (item != null) {
							AttributeList list = item.getAttributeList();
							if (list != null) {
								list.correctDecompressedImagePixelModule(deferredDecompression);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <p>Correct the PhotometricInterpretation and Planar Configuration iff pixel data in a compressed input transfer syntax was actually decompressed during reading of AttributeList or will be if compressed frames are decompressed.</p>
	 *
	 * <p>If the color space was or will be converted, change the PhotometricInterpretation, e.g., from YBR_FULL_422 for JPEG lossy to RGB.</p>
	 *
	 * <p>If the pixel data was or will be decompressed, if JPEG (not RLE) change the PlanarConfiguration to 0 if not already (i.e., color-by-pixel not color-by-plane).</p>
	 *
	 * <p>Does nothing (is harmless) if the input was not compressed or not multi-component.</p>
	 *
	 * <p>Recurses into sequences in case there is icon pixel data that was also decompressed.</p>
	 *
	 * @throws	DicomException		if cannot create replacement attribute
	 */
	public void correctDecompressedImagePixelModule() throws DicomException {
		correctDecompressedImagePixelModule(false/*deferredDecompression*/);
	}
	
	/**
	 * <p>Construct a text title to describe this list.</p>
	 *
	 * @return			a single line constructed from patient, study, series and instance attribute values
	 */
	public String buildInstanceTitleFromAttributeList() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.PatientName));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.PatientID));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.StudyID));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.StudyDate));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.StudyDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.SeriesNumber));
		buffer.append("[");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.Modality));
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.SeriesDescription));
		buffer.append("]");
		buffer.append(":");
		buffer.append(Attribute.getSingleStringValueOrEmptyString(this,TagFromName.InstanceNumber));
		buffer.append(":");
		return buffer.toString();
	}

	/**
	 * <p>Construct a text title to describe the specified list.</p>
	 *
	 * @param	list	the AttributeList
	 * @return			a single line constructed from patient, study, series and instance attribute values (or empty string if list is null)
	 */
	public static String buildInstanceTitleFromAttributeList(AttributeList list) {
		return list == null ? "" : list.buildInstanceTitleFromAttributeList();
	}

	/**
	 * <p>Find all referenced SOP Instance UIDs that are nested within Sequences in the specified list and add them to the supplied set.</p>
	 *
	 * @param	list							the AttributeList
	 * @param	setOfReferencedSOPInstanceUIDs	may be empty or null
	 * @return									the supplied set or a new set if null, with all referenced SOP Instance UIDs found added
	 */
	public static Set<String> findAllNestedReferencedSOPInstanceUIDs(AttributeList list,Set<String> setOfReferencedSOPInstanceUIDs) {
		if (setOfReferencedSOPInstanceUIDs == null) {
			setOfReferencedSOPInstanceUIDs = new HashSet<String>();
		}
		Iterator it = list.values().iterator();
		while (it.hasNext()) {
			Attribute a = (Attribute)it.next();
			if (a != null) {
				if (a instanceof SequenceAttribute) {
					Iterator is = ((SequenceAttribute)a).iterator();
					while (is.hasNext()) {
						SequenceItem item = (SequenceItem)is.next();
						if (item != null) {
							AttributeList itemList = item.getAttributeList();
							if (itemList != null) {
								Iterator itn = itemList.values().iterator();
								while (itn.hasNext()) {
									Attribute an = (Attribute)itn.next();
									if (an != null && an.getTag().equals(TagFromName.ReferencedSOPInstanceUID)) {
										String referencedSOPInstanceUID = an.getSingleStringValueOrEmptyString();
										if (referencedSOPInstanceUID.length() > 0) {
//System.err.println("findAllNestedReferencedSOPInstanceUIDs(): adding "+referencedSOPInstanceUID);
											setOfReferencedSOPInstanceUIDs.add(referencedSOPInstanceUID);
										}
									}
								}
								findAllNestedReferencedSOPInstanceUIDs(itemList,setOfReferencedSOPInstanceUIDs);
							}
						}
					}
				}
				// do NOT look for ReferencedSOPInstanceUID here; only want to find when nested (not in top level dataset, which is invalid DICOM, due to need to workaround buggy RIDER Pilot data, for example)
			}
		}
		return setOfReferencedSOPInstanceUIDs;
	}
	
	/**
	 * <p>Find all referenced SOP Instance UIDs that are nested within Sequences in the specified list.</p>
	 *
	 * @param	list	the AttributeList
	 * @return			a set of all referenced SOP Instance UIDs
	 */
	public static Set<String> findAllNestedReferencedSOPInstanceUIDs(AttributeList list) {
		return findAllNestedReferencedSOPInstanceUIDs(list,new HashSet<String>());
	}

	/**
	 * <p>Find all referenced SOP Instance UIDs that are nested within Sequences in this list and add them to the supplied set.</p>
	 *
	 * @param	setOfReferencedSOPInstanceUIDs	may be empty or null
	 * @return									the supplied set or a new set if null, with all referenced SOP Instance UIDs found added
	 */
	public Set<String> findAllNestedReferencedSOPInstanceUIDs(Set<String> setOfReferencedSOPInstanceUIDs) {
		return findAllNestedReferencedSOPInstanceUIDs(this,setOfReferencedSOPInstanceUIDs);
	}
	
	/**
	 * <p>Find all referenced SOP Instance UIDs that are nested within Sequences in this list.</p>
	 *
	 * @return			a set of all referenced SOP Instance UIDs
	 */
	public Set<String> findAllNestedReferencedSOPInstanceUIDs() {
		return findAllNestedReferencedSOPInstanceUIDs(new HashSet<String>());
	}
	
	/**
	 * <p>Make a new {@link com.pixelmed.dicom.AttributeList AttributeList} from String keyword and value pairs.</p>
	 *
	 * @param	arg					an array of String keyword and value pairs
	 * @param	offset				the offset into the array to start
	 * @param	length				the number of array entries (not pairs) to use
	 * @return						a new {@link com.pixelmed.dicom.AttributeList AttributeList}
	 * @throws	DicomException	if unable to make an Attribute
	 */
	public static AttributeList makeAttributeListFromKeywordAndValuePairs(String arg[],int offset,int length) throws DicomException {
		createDictionaryifNecessary();
		AttributeList list = new AttributeList();
		while (length > 0) {
			AttributeTag tag = dictionary.getTagFromName(arg[offset]);
			if (tag == null) {
				throw new DicomException("Keyword not in dictionary "+arg[offset]);
			}
//System.err.println("AttributeList.makeAttributeListFromKeywordAndValuePairs(): made an "+tag+" with value "+arg[offset+1]);
			list.replace(tag,arg[offset+1]);
			offset+=2;
			length-=2;
		}
		return list;
	}
	
	/**
	 * @param	arg
	 */
	static void test(String arg[]) {

		try {
			AttributeTag tag = new AttributeTag(0x0020,0x000d);
			try {
				Class classToUse = CodeStringAttribute.class;
				Class [] argTypes  = {AttributeTag.class};
				Object[] argValues = {tag};
				Attribute a = (Attribute)(classToUse.getConstructor(argTypes).newInstance(argValues));
				System.err.println("made an "+a);	// no need to use SLF4J since command line utility/test
			}
			catch (Exception e) {
				throw new DicomException("Could not instantiate an attribute for "+tag+": "+e);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
		
		System.err.println("do it buffered, looking for metaheader, no uid specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],null,true,true);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
			
			if (arg.length > 1) {
				//System.err.println("also writing it after removing lengths and meta information header to "+arg[1]);
				//System.err.println("also writing it after removing lengths to "+arg[1]);
				System.err.println("also writing it unchanged to "+arg[1]);
				//list.removeGroupLengthAttributes();			
				//list.removeMetaInformationHeaderAttributes();
				list.write(arg[1],TransferSyntax.ExplicitVRLittleEndian,true,true);
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it unbuffered, looking for metaheader, no uid specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],null,true,false);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			slf4jlogger.error("",e);
		}

		System.err.println("do it buffered, looking for metaheader, EVRLE specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],"1.2.840.10008.1.2.1",true,true);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it unbuffered, looking for metaheader, EVRLE specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],"1.2.840.10008.1.2.1",true,false);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it buffered, no metaheader, no uid specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],null,false,true);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it unbuffered, no metaheader, no uid specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],null,false,false);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it buffered, no metaheader, IVRLE specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],"1.2.840.10008.1.2",false,true);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}

		System.err.println("do it unbuffered, no metaheader, IVRLE specified");
		try {
			AttributeList list = new AttributeList();
			System.err.println("test reading list");
			list.read(arg[0],"1.2.840.10008.1.2",false,false);
			System.err.println("test iteration through list");
			System.err.print(list.toString());
			System.err.println("test fetching specific tags");
			System.err.println(list.get(new AttributeTag(0x0020,0x000d)));
			System.err.println(list.get(new AttributeTag(0x0010,0x0010)));
			System.err.println(list.get(new AttributeTag(0x0070,0x0010)));		// won't be there
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}

	/**
	 * <p>Read the DICOM input file as a list of attributes and write it to the specified output file or dump it.</p>
	 *
	 * <p>When copying, removes any group lengths and creates a new meta information header.</p>
	 *
	 * @param	arg	array of one string (the filename to read and dump),
	 *				two strings (the filename to read and the filename to write),
	 *				three strings (the filename to read, the transfer syntax to write, the filename to write), or
	 *				four strings (the transfer syntax to read (must be zero length if metaheader present), the filename to read, the transfer syntax to write, the filename to write)
	 *				five strings (the transfer syntax to read (must be zero length if metaheader present), the filename to read, the transfer syntax to write, the filename to write, and whether or not to write a metaheader)
	 */
	public static void main(String arg[]) {
		if (arg.length == 3 && arg[2].equals("TEST")) {
			test(arg);
		}
		else if (arg.length > 0) {
			String inputTransferSyntax = null;
			String inputFileName = null;
			String outputTransferSyntax = TransferSyntax.ExplicitVRLittleEndian;
			String outputFileName = null;
			boolean outputMeta = true;
			if (arg.length == 1) {
				inputFileName = arg[0];
			}
			else if (arg.length == 2) {
				inputFileName = arg[0];
				outputFileName = arg[1];
			}
			else if (arg.length == 3) {
				inputFileName = arg[0];
				outputTransferSyntax = TransferSyntaxFromName.getUID(arg[1]);
				outputFileName = arg[2];
			}
			else if (arg.length == 4) {
				inputTransferSyntax = TransferSyntaxFromName.getUID(arg[0]);
				inputFileName = arg[1];
				outputTransferSyntax = TransferSyntaxFromName.getUID(arg[2]);
				outputFileName = arg[3];
			}
			else if (arg.length == 5) {
				inputTransferSyntax = TransferSyntaxFromName.getUID(arg[0]);
				inputFileName = arg[1];
				outputTransferSyntax = TransferSyntaxFromName.getUID(arg[2]);
				outputFileName = arg[3];
				outputMeta = ! arg[4].toUpperCase(java.util.Locale.US).contains("NO");
			}
			try {
				AttributeList list = new AttributeList();
//long startReadTime = System.currentTimeMillis();
				//list.setDecompressPixelData(false);	// will work for copying compressed TransferSyntax only if same TransferSyntax specified on command line for output
				list.read(inputFileName,inputTransferSyntax,inputTransferSyntax == null || inputTransferSyntax.length() == 0/*tryMeta*/,true);
//System.err.println("AttributeList.main(): read - done in "+(System.currentTimeMillis()-startReadTime)+" ms");
				if (outputFileName == null) {
					slf4jlogger.info("Dumping ...\n{}",list.toString());
				}
				else {
					list.removeGroupLengthAttributes();			
					list.removeMetaInformationHeaderAttributes();
					list.remove(TagFromName.DataSetTrailingPadding);
					list.correctDecompressedImagePixelModule();
					// would be nice to check here for an additional arg that was a replacement character set
					FileMetaInformation.addFileMetaInformation(list,outputTransferSyntax,"OURAETITLE");
//long startWriteTime = System.currentTimeMillis();
					list.write(outputFileName,outputTransferSyntax,outputMeta,true);
//System.err.println("AttributeList.main(): write - done in "+(System.currentTimeMillis()-startWriteTime)+" ms");
				}
			} catch (Exception e) {
				slf4jlogger.error("",e);	// use SLF4J since may be invoked from script when not testing
			}
		}
	}
}




/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.util.zip.*;

import com.pixelmed.utils.ByteArray;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to copy DICOM attributes from anm input stream to an output stream,
 * converting the encoding of the attributes between transfer syntaxes if necessary.</p>
 *
 * @see com.pixelmed.dicom.DicomInputStream
 * @see com.pixelmed.dicom.DicomOutputStream
 *
 * @author	dclunie
 */
public class DicomStreamCopier {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomStreamCopier.java,v 1.26 2017/01/24 10:50:37 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomStreamCopier.class);

	/***/
	private static DicomDictionary dictionary;
	
	/***/
	private void createDictionaryifNecessary() {
		if (dictionary == null) {
//System.err.println("DicomStreamCopier.createDictionaryifNecessary(): creating static dictionary");
			dictionary = new DicomDictionary();
		}
	}

	/***/
	private DicomInputStream i;
	/***/
	private DicomOutputStream o;

	/***/
	private static final int bufferSize = 32768;	// must be a multiple of largest VR word size re. endianness, which is 8 for for FD

	/**
	 * @throws	IOException		if an I/O error occurs
	 */
	private AttributeTag readAttributeTag() throws IOException {
		int group   = i.readUnsigned16();
		int element = i.readUnsigned16();
		return new AttributeTag(group,element);
	}

	/**
	 * @param	tag
	 * @throws	IOException		if an I/O error occurs
	 */
	private void writeAttributeTag(AttributeTag tag) throws IOException {
		o.writeUnsigned16(tag.getGroup());
		o.writeUnsigned16(tag.getElement());
	}

	/**
	 * @param	byteOffset
	 * @param	lengthToRead
	 * @param	doCopy
	 * @param	isSignedPixelRepresentation	the PixelRepresentation in an enclosing data set is signed (needed to choose VR for US/SS VR data elements)
	 * @throws	IOException					if an I/O error occurs
	 * @throws	DicomException
	 */
	private long copySequenceAttribute(long byteOffset,long lengthToRead,boolean doCopy,boolean isSignedPixelRepresentation) throws IOException, DicomException {
		boolean undefinedLength = lengthToRead == 0xffffffffl;
		long endByteOffset=(undefinedLength) ? 0xffffffffl : byteOffset+lengthToRead-1;

//System.err.println("copySequenceAttribute: start byteOffset="+byteOffset+" lengthToRead="+lengthToRead+" endByteOffset="+endByteOffset);
		try {
			// CBZip2InputStream.available() always returns zero, and since we terminate
			// on exceptions anyway, just forget about it
			while (/*i.available() > 0 && */(undefinedLength || byteOffset < endByteOffset)) {
//System.err.println("copySequenceAttribute: loop byteOffset="+byteOffset);
				long itemStartOffset=byteOffset;
				AttributeTag tag = readAttributeTag();
				byteOffset+=4;
				if (doCopy) {
					writeAttributeTag(tag);
				}
//System.err.println("copySequenceAttribute: tag="+tag);
				long vl = i.readUnsigned32();		// always implicit VR form for items and delimiters
				byteOffset+=4;
//System.err.println(byteOffset+" "+tag+" VL=<0x"+Long.toHexString(vl)+">");
				if (tag.equals(TagFromName.SequenceDelimitationItem)) {
//System.err.println("copySequenceAttribute: SequenceDelimitationItem");
					if (doCopy) {
						o.writeUnsigned32(vl);		// vl should be zero
					}
					break;
				}
				else if (tag.equals(TagFromName.Item)) {
//System.err.println("copySequenceAttribute: Item byteOffset="+byteOffset);
					if (doCopy) {
						o.writeUnsigned32(0xffffffffl);		// always make undefined length, since lengths may change between implicit and explicit
					}
					byteOffset=copy(byteOffset,vl,false/*stopAfterMetaInformationHeader*/,false/*copyMetaInformationHeader*/,doCopy,false,isSignedPixelRepresentation);	// propagates appropriate isSignedPixelRepresentation into sequence items to allow determination of signed dependent VR of US/SS for nested elements (000919)
					if (doCopy && vl != 0xffffffffl) {
//System.err.println("copySequenceAttribute: add item delimiter since we are converting fixed length item to undefined length item");
						writeAttributeTag(TagFromName.ItemDelimitationItem);
						o.writeUnsigned32(0);
					}
				}
				else {
					throw new DicomException("Bad tag "+tag+"(not Item or Sequence Delimiter) in Sequence at byte offset "+byteOffset);
				}
			}
		}
		catch (EOFException e) {
//System.err.println("Closing on "+e);
			if (!undefinedLength) throw new EOFException();
		}
		catch (IOException e) {
//System.err.println("Closing on "+e);
			if (!undefinedLength) throw new IOException();		// InflaterInputStream seems to throw IOException rather than EOFException
		}
		if (doCopy && !undefinedLength) {
//System.err.println("copySequenceAttribute: add sequence delimiter since we are converting fixed length item to undefined length item");
			writeAttributeTag(TagFromName.SequenceDelimitationItem);
			o.writeUnsigned32(0);
		}
//System.err.println("copySequenceAttribute: return byteOffset="+byteOffset);
		return byteOffset;
	}

	/**
	 * <p>Copy a dicom input stream to a dicom output stream, using any meta information header if present in input, but not copying it.</p>
	 *
	 * <p>Implements the CP 1066 proposal to handle values too long to fit in Explicit VR by writing a UN rather than the actual VR.</p>
	 *
	 * @param	byteOffset
	 * @param	lengthToRead
	 * @param	stopAfterMetaInformationHeader
	 * @param	copyMetaInformationHeader
	 * @param	doCopy
	 * @param	closeWhenDone					close the output stream when finished; needed to flush any compressed output if compressor pushed on stream
	 * @param	isSignedPixelRepresentation		the PixelRepresentation in an enclosing data set is signed (needed to choose VR for US/SS VR data elements)
	 * @throws	IOException						if an I/O error occurs
	 * @throws	DicomException
	 */
	private long copy(long byteOffset,long lengthToRead,boolean stopAfterMetaInformationHeader,boolean copyMetaInformationHeader,boolean doCopy,boolean closeWhenDone,boolean isSignedPixelRepresentation) throws IOException, DicomException {
		if (i.areReadingDataSet()) {
			// Test to see whether or not a codec needs to be pushed on the stream ... after the first time, the TransferSyntax will always be ExplicitVRLittleEndian 
//System.err.println("DicomStreamCopier.copy(): Input stream - testing for deflate and bzip2 TS");
			if (i.getTransferSyntaxToReadDataSet().isDeflated()) {
				// insert deflate into input stream and make a new DicomInputStream
//System.err.println("DicomStreamCopier.copy(): Input stream - creating new DicomInputStream from deflate");
				i = new DicomInputStream(new InflaterInputStream(i,new Inflater(true)),TransferSyntax.ExplicitVRLittleEndian,false);
				byteOffset=0;
			}
			else if (i.getTransferSyntaxToReadDataSet().isBzip2ed()) {
				// insert bzip2 into input stream and make a new DicomInputStream
//System.err.println("DicomStreamCopier.copy(): Input stream - creating new DicomInputStream from bzip2");
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
					throw new DicomException("Not a correctly encoded bzip2 bitstream - "+e);
				}
				catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
					throw new DicomException("Could not instantiate bzip2 codec - "+e);
				}
			}
		}
		
		if (o.areWritingDataSet()) {
//System.err.println("DicomStreamCopier.copy(): o.isExplicitVR() = "+o.isExplicitVR());
//System.err.println("DicomStreamCopier.copy(): o.isImplicitVR() = "+o.isImplicitVR());
			// Test to see whether or not a codec needs to be pushed on the stream ... after the first time, the TransferSyntax will always be ExplicitVRLittleEndian 
//System.err.println("DicomStreamCopier.copy(): Output stream - testing for deflate and bzip2 TS");
			if (o.getTransferSyntaxToWriteDataSet().isDeflated()) {
				// insert deflate into output stream and make a new DicomOutputStream
//System.err.println("DicomStreamCopier.copy(): Output stream - creating new DicomOutputStream from deflate");
				OutputStream deflaterOutputStream = new DeflaterOutputStream(o,new Deflater(Deflater.BEST_COMPRESSION,true/*don't wrap with zlib*/));
				o = new DicomOutputStream(
					deflaterOutputStream,
					null,	// no meta-header
					TransferSyntax.ExplicitVRLittleEndian);
				byteOffset=0;
				// assert closeWhenDone=true;
			}
			else if (o.getTransferSyntaxToWriteDataSet().isBzip2ed()) {
				// insert bzip2 into output stream and make a new DicomOutputStream
//System.err.println("DicomStreamCopier.copy(): Output stream - creating new DicomOutputStream from bzip2");
				// NB. Older implementations of BZip2CompressorOutputStream used to expect the "BZ" prefix to be stripped; this is no longer the case; the code here is unchanged so the "BZ" prefix must be expected by recipients or they will fail (000964)
				try {
					Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream");
					Class [] argTypes  = {OutputStream.class};
					Object[] argValues = {o};
					//Object[] argValues = {o.getUnderlyingOutputStream()};
					OutputStream bzip2OutputStream = (OutputStream)(classToUse.getConstructor(argTypes).newInstance(argValues));
					o = new DicomOutputStream(
						bzip2OutputStream,
						null,	// no meta-header
						TransferSyntax.ExplicitVRLittleEndian);
					byteOffset=0;
					// assert closeWhenDone=true;
				}
				catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
					throw new DicomException("Could not instantiate bzip2 codec - "+e);
				}
			}
		}
		
		byte[] buffer = new byte[bufferSize];
		
		createDictionaryifNecessary();

		boolean undefinedLength = lengthToRead == 0xffffffffl;
		long endByteOffset=(undefinedLength) ? 0xffffffffl : byteOffset+lengthToRead-1;

//System.err.println("copy: start byteOffset="+byteOffset+" lengthToRead="+lengthToRead);
		byte vrBuffer[] = new byte[2];

		String lastTransferSyntaxUIDEncountered=null;
		long lastFileMetaInformationGroupLengthEncountered=0;
		int bitsAllocated=0;
		
		boolean  inputExplicit = i.getTransferSyntaxInUse().isExplicitVR();		// set these here because may have switched from meta-header to data set
		boolean outputExplicit = o.getTransferSyntaxInUse().isExplicitVR();
//System.err.println("DicomStreamCopier.copy(): outputExplicit = "+outputExplicit);

		boolean swapEndian = (i.getTransferSyntaxInUse().isLittleEndian() != o.getTransferSyntaxInUse().isLittleEndian());

		try {
			// CBZip2InputStream.available() always returns zero, and since we terminate
			// on exceptions anyway, just forget about it
			while (/*i.available() > 0 && */(undefinedLength || byteOffset < endByteOffset)) {
//System.err.println("copy: loop byteOffset="+byteOffset);
				AttributeTag tag = readAttributeTag();
//System.err.println("copy: Tag = "+tag);
				byteOffset+=4;
				boolean restoreCopyAfterThisElement = doCopy;		// distinguish between copying stopped forever and just for this tag
				if (tag.equals(TagFromName.DataSetTrailingPadding)) {
					//return byteOffset;	// don't copy it, and never anything past it
					doCopy=false;
					restoreCopyAfterThisElement=false;
				}
				else if (tag.getGroup() == 0x0002 && !copyMetaInformationHeader) {
					// don't copy first tag of meta-information
					doCopy=false;
					// restoreCopyAfterThisElement true	BUT rest of meta header won't be copied because of invocation of copy() with copyMetaInformationHeader false
				}
				else if (tag.getGroup() != 0x0000 && tag.getGroup() != 0x0002 && tag.getElement() == 0x0000) {
					// don't copy group lengths except for command and meta-information groups
					doCopy=false;
					// restoreCopyAfterThisElement true
				}
				else if (tag.equals(TagFromName.LengthToEnd)) {
					doCopy=false;
					// restoreCopyAfterThisElement true
				}
				else {
					writeAttributeTag(tag);
					// doCopy and restoreCopyAfterThisElement both true
				}
//if (!doCopy) System.err.println("copy: not copying "+tag);
				if (tag.equals(TagFromName.ItemDelimitationItem)) {
//System.err.println("copy: ItemDelimitationItem");
					// Read and discard value length
					long vl = i.readUnsigned32();
					byteOffset+=4;
					if (doCopy) o.writeUnsigned32(vl);
					return byteOffset;	// stop now, since we must have been called to read an item's dataset
				}
				
				if (tag.equals(TagFromName.Item)) {
					// this is bad ... there shouldn't be Items here since they should
					// only be found during copySequenceAttribute()
					// however, try to work around Philips bug ...
					long vl = i.readUnsigned32();		// always implicit VR form for items and delimiters
					byteOffset+=4;
					if (doCopy) o.writeUnsigned32(vl);
					slf4jlogger.warn("Ignoring bad Item at {} {} VL=<0x{}>",byteOffset,tag,Long.toHexString(vl));
					// let's just ignore it for now
					continue;
				}

				byte vr[];
				if (inputExplicit) {
					vr=vrBuffer;
					i.readInsistently(vr,0,2);
					byteOffset+=2;
					if (ValueRepresentation.isOtherUnspecifiedVR(vr)
					 || ValueRepresentation.isUnspecifiedShortVR(vr)
					 || ValueRepresentation.isUnspecifiedShortOrOtherWordVR(vr)) {
						throw new DicomException("Illegal explicit value representation in input - "+ValueRepresentation.getAsString(vr));
					}
				}
				else {
					vr = dictionary.getValueRepresentationFromTag(tag);
					if (vr == null)  {
						vr=ValueRepresentation.UN;
					}
				}
//System.err.println("copy: vr = "+ValueRepresentation.getAsString(vr));
//if (tag.equals(TagFromName.PixelData)) System.err.println("copy: vr = "+ValueRepresentation.getAsString(vr));
				// don't write explicit VR yet ... may need to check and correct VR first
	
				long vl;
				if (inputExplicit) {
					if (ValueRepresentation.isShortValueLengthVR(vr)) {
						vl=i.readUnsigned16();
						byteOffset+=2;
					}
					else {
						i.readUnsigned16();	// reserved bytes
						vl=i.readUnsigned32();
						byteOffset+=6;
					}
				}
				else {
					vl=i.readUnsigned32();
					byteOffset+=4;
				}
				// don't write VL yet ... need to check, correct and write explicit VR first
				
				if (doCopy) {
					if (outputExplicit) {
						if (ValueRepresentation.isOtherUnspecifiedVR(vr)) {
//System.err.println("Correcting OX: from = "+ValueRepresentation.getAsString(vr));
							// Implement PS 3.5 Annex A rules
							vr = tag.equals(TagFromName.PixelData) && vl != 0xffffffffl && bitsAllocated > 8	// could always make OW, but OB nicer for 8
							  || ((tag.getGroup() & 0xff00) == 0x6000 && tag.getElement() == 0x3000)		// Overlay Data
							  // Curve Data is OB
							  || ((tag.getGroup() & 0xff00) == 0x5000 && tag.getElement() == 0x200C)		// Audio Sample Data
							  // should check Audio Sample Format (50xx,2002) for 8 bit and make them OB rather than OW :(
							  || tag.equals(TagFromName.WaveformData)
								? ValueRepresentation.OW : ValueRepresentation.OB;
//System.err.println("Correcting OX: to = "+ValueRepresentation.getAsString(vr));
						}
						else if (ValueRepresentation.isUnspecifiedShortOrOtherWordVR(vr)) {
							vr = ValueRepresentation.OW;	// PS 3.5 A.1 says the lookup table data should be OW
						}
						else if (ValueRepresentation.isUnspecifiedShortVR(vr)) {
							vr = isSignedPixelRepresentation ? ValueRepresentation.SS : ValueRepresentation.US;
						}
						
						if (ValueRepresentation.isShortValueLengthVR(vr) && vl > AttributeList.maximumShortVRValueLength) {
							// we are screwed ... cannot write in specified VR without truncation ... use CP 1066 UN VR instead
							slf4jlogger.warn("Using UN rather than {} because because VL ({} dec, 0x{}) is too long to fit in 16 bits for tag {}",ValueRepresentation.getAsString(vr),vl,Long.toHexString(vl),tag);
							vr = ValueRepresentation.UN;
							swapEndian = i.getTransferSyntaxInUse().isBigEndian();	// no longer care about output endianness, because UN is ALWAYS interpreted as little endian
						}
						
						o.write(vr,0,2);

						if (ValueRepresentation.isShortValueLengthVR(vr)) {
							o.writeUnsigned16((int)vl);
						}
						else {
							o.writeUnsigned16(0);	// reserved bytes
							if (ValueRepresentation.isSequenceVR(vr)) {
								o.writeUnsigned32(0xffffffffl);				// sequences converted to undefined length, since lengths may change 
							}
							else {
								o.writeUnsigned32(vl);
							}
						}
					}
					else {
						if (ValueRepresentation.isSequenceVR(vr)) {
							o.writeUnsigned32(0xffffffffl);					// sequences converted to undefined length, since lengths may change 
						}
						else {
							o.writeUnsigned32(vl);
						}
					}
				}
//System.err.println("copy: vl = "+vl);
				
				if (ValueRepresentation.isSequenceVR(vr) || vl == 0xffffffffl) {
					byteOffset=copySequenceAttribute(byteOffset,vl,doCopy,isSignedPixelRepresentation);
				}
				else if (tag.equals(TagFromName.FileMetaInformationGroupLength)) {
					if (vl != 4) throw new DicomException("Error copying FileMetaInformationGroupLength from meta information header - value wrong length "+vl);
					lastFileMetaInformationGroupLengthEncountered=i.readUnsigned32();
					if (doCopy) o.writeUnsigned32(lastFileMetaInformationGroupLengthEncountered);
					byteOffset+=4;
				}
				else if (tag.equals(TagFromName.TransferSyntaxUID)) {
					if (vl > bufferSize) throw new DicomException("Error copying TransferSyntaxUID from meta information header - value too long "+vl);
					i.readInsistently(buffer,0,(int)vl);
					if (doCopy) o.write(buffer,0,(int)vl);
					lastTransferSyntaxUIDEncountered=new String(buffer,0,(int)vl);
					byteOffset+=vl;
				}
				else if (tag.equals(TagFromName.PixelRepresentation)) {
					if (vl != 2) throw new DicomException("Error copying PixelRepresentation - value wrong length "+vl);
					int pixelRepresentation=i.readUnsigned16();
					if (doCopy) o.writeUnsigned16(pixelRepresentation);
					byteOffset+=2;
					isSignedPixelRepresentation = pixelRepresentation == 1;
				}
				else if (tag.equals(TagFromName.BitsAllocated)) {
					if (vl != 2) throw new DicomException("Error copying BitsAllocated - value wrong length "+vl);
					bitsAllocated=i.readUnsigned16();
					if (doCopy) o.writeUnsigned16(bitsAllocated);
					byteOffset+=2;
				}
				else {
					int wordLength = ValueRepresentation.getWordLengthOfValueAffectedByEndianness(vr);
					if (swapEndian && wordLength > 1 && vl%wordLength != 0) {
						throw new DicomException("VL "+vl+" not a multiple of the expected VR value length "+wordLength);
					}
					while (vl > bufferSize) {
						i.readInsistently(buffer,0,bufferSize);
						if (doCopy) {
							if (swapEndian && wordLength > 1) {
								ByteArray.swapEndianness(buffer,bufferSize,wordLength);
							}
							o.write(buffer,0,bufferSize);
						}
						byteOffset+=bufferSize;
						vl-=bufferSize;
					}
					if (vl > 0) {
						i.readInsistently(buffer,0,(int)vl);
						byteOffset+=vl;
						if (doCopy) {
							if (swapEndian && wordLength > 1) {
								ByteArray.swapEndianness(buffer,(int)vl,wordLength);
							}
							o.write(buffer,0,(int)vl);
						}
					}
				}
				if (restoreCopyAfterThisElement) doCopy=true;	// start copying again
				
				// handle possible metaheader and transition to different transfer syntax
				{
					if (tag.equals(TagFromName.FileMetaInformationGroupLength)) {
//System.err.println("Found meta-header");
						if (i.areReadingMetaHeader()) {
							long metaLength=lastFileMetaInformationGroupLengthEncountered;
							byteOffset=copy(byteOffset,metaLength,true,copyMetaInformationHeader,doCopy,false,false/*isSignedPixelRepresentation*/);		// detects and sets transfer syntax for reading dataset
							i.setReadingDataSet();
							o.setWritingDataSet();
							if (stopAfterMetaInformationHeader) {
//System.err.println("Stopping after meta-header");
								break;
							}
							else {
//System.err.println("Calling copy");
								byteOffset=copy(byteOffset,0xffffffffl,false,copyMetaInformationHeader,doCopy,true,false/*isSignedPixelRepresentation*/);	// read to end
							}
						}
						else {
							// ignore it, e.g. nested within a sequence item (GE bug).
//System.err.println("Ignoring unexpected FileMetaInformationGroupLength outside meta information header");
						}
					}
					else if (tag.equals(TagFromName.TransferSyntaxUID)) {
						if (i.areReadingMetaHeader()) {
//System.err.println("Setting TransferSyntaxToReadDataSet to "+lastTransferSyntaxUIDEncountered);
							i.setTransferSyntaxToReadDataSet (new TransferSyntax(lastTransferSyntaxUIDEncountered));
							// Do NOT change the Transfer Syntax to write the dataset
						}
						else {
							// ignore it, e.g. nested within a sequence item (GE bug).
//System.err.println("Ignoring unexpected TransferSyntaxUID outside meta information header");
						}
					}
				}
			}
		}
		catch (EOFException e) {
//System.err.println("Closing on "+e);
			if (!undefinedLength) throw new EOFException();
		}
		catch (IOException e) {
//System.err.println("Closing on "+e);
			if (!undefinedLength) throw new IOException();		// InflaterInputStream seems to throw IOException rather than EOFException
		}

		// Need to explicitly finish the compression if underlying codec pushed on stream
		// since any later close on the originally supplied DicomOutputStream will not
		// know about the newly pushed DicomOutputStream and compression OutputStream, and
		// a mere flush() does not cause the compressor to finish :(
		
		if (closeWhenDone) {
			o.close();
		}
		return byteOffset;
	}
	
	/**
	 * <p>Copy a dicom input stream to a dicom output stream, using any meta information header if present in input, but not copying it.</p>
	 *
	 * <p>Implements the CP 1066 proposal to handle values too long to fit in Explicit VR by writing a UN rather than the actual VR.</p>
	 *
	 * @param	i		the input stream
	 * @param	o		the output stream, which is closed after the copy is done
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public DicomStreamCopier(DicomInputStream i,DicomOutputStream o) throws DicomException,IOException {
		this.i=i;
		this.o=o;
		copy(0,0xffffffffl,false/*stopAfterMetaInformationHeader*/,false/*copyMetaInformationHeader*/,true/*doCopy*/,true/*closeWhenDone*/,false/*isSignedPixelRepresentation*/);
	}
	
	/**
	 * <p>Copy one file to another parsing and recreating the DICOM attributes using the specified transfer syntaxes.</p>
	 *
	 * @param	arg	four arguments, the input transfer syntax uid (must be zero length if metaheader present), the input filename,
	 *			the output transfer syntax uid and the output filename
	 */
	public static void main(String arg[]) {
		try {
			DicomInputStream  i = new DicomInputStream (new BufferedInputStream(new FileInputStream(arg[1])),arg[0],arg[0].length() == 0/*tryMeta*/);
			DicomOutputStream o = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(arg[3])),null/*meta*/,arg[2]/*dataset*/);
			new  DicomStreamCopier(i,o);
		} catch (Exception e) {
			//System.err.println(e);
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}



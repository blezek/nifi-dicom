/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import com.pixelmed.utils.CopyStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Word (OW) attributes whose values are not memory resident.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.OtherByteAttributeOnDisk
 *
 * @author	dclunie
 */
public class OtherWordAttributeOnDisk extends OtherAttributeOnDisk {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherWordAttributeOnDisk.java,v 1.16 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(OtherWordAttributeOnDisk.class);

	protected boolean bigEndian;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * Any file set later will be expected to be little endian.
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherWordAttributeOnDisk(AttributeTag t) {
		super(t);
		bigEndian = false;
	}

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * Any file set later will be expected to be in the specified byte order.
	 *
	 * @param	t	the tag of the attribute
	 * @param	bigEndian	big endian, false if little endian
	 */
	public OtherWordAttributeOnDisk(AttributeTag t,boolean bigEndian) {
		super(t);
		this.bigEndian = bigEndian;
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeOnDisk(AttributeTag t,long vl,DicomInputStream i,long byteOffset) throws IOException, DicomException {
		super(t,vl,i,byteOffset);
		bigEndian=i.isBigEndian();
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeOnDisk(AttributeTag t,Long vl,DicomInputStream i,Long byteOffset) throws IOException, DicomException {
		super(t,vl,i,byteOffset);
		bigEndian=i.isBigEndian();
	}

	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		//throw new DicomException("Internal error - unsupported operation, write of OtherWordAttributeOnDisk");
		writeBase(o);
		if (valueLength > 0) {
//System.err.println("OtherWordAttributeOnDisk.write(): start file = "+file);
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			CopyStream.skipInsistently(in,byteOffset);
			if (bigEndian == o.isBigEndian()) {
				CopyStream.copy(in,o,valueLength);
			}
			else {
				CopyStream.copyByteSwapped(in,o,valueLength);
			}
			in.close();
		}
	}

	/**
	 * <p>Is the data on disk byte order big endian ?</p>
	 *
	 * @return	true if big endian, false if little endian
	 */
	public boolean isBigEndian() { return bigEndian; }
	
	/**
	 * <p>Get the values of this attribute as a short array.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from which to get a {@link java.nio.ShortBuffer ShortBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk, because
	 * {@link com.pixelmed.dicom.AttributeFactory AttributeFactory} silently created an instance of this
	 * class rather than an in-memory {@link com.pixelmed.dicom.OtherWordAttribute OtherWordAttribute}.</p>
	 *
	 * @return						the values as an array of short
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[] getShortValues() throws DicomException {
//System.err.println("OtherWordAttributeOnDisk.getShortValues(): lazy read into heap allocated memory, rather than using memory mapped buffer :(");
		short[] buffer = null;
		if (valueLength > 0) {
			int len = (int)(valueLength/2);
			buffer = new short[len];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),bigEndian);
				i.skipInsistently(byteOffset);
				i.readUnsigned16(buffer,0,len);
				i.close();
			}
			catch (IOException e) {
				throw new DicomException("Failed to read value (length "+valueLength+" dec) in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		return buffer;
	}

	/**
	 * <p>Get the values of this attribute as multiple short arrays, one per frame.</p>
	 *
	 * <p>Caller needs to supply the number for frames so that pixel data can be split
	 * across per-frame arrays (since not necessarily known when this attribute was created
	 * or read.</p>
	 *
	 * <p>This allocates new arrays of sufficient length, which may fail if they are too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from which to get a {@link java.nio.ShortBuffer ShortBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @param	numberOfFrames	the number of frames
	 * @return					the values as an array of arrays of bytes
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[][] getShortValuesPerFrame(int numberOfFrames) throws DicomException {
//System.err.println("OtherWordAttributeOnDisk.getShortValuesPerFrame(): lazy read of of all frames into heap allocated memory as per-frame arrays, rather than using memory mapped buffer :(");
		short[][] v = null;
		if (valueLength > 0) {
			long bytesperframe = valueLength / numberOfFrames;
			int len = (int)(bytesperframe/2);
			v = new short[numberOfFrames][];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),false/*bigEndian - byte order is irrelevant*/);
				i.skipInsistently(byteOffset);
				for (int f=0; f<numberOfFrames; ++f) {
					short[] buffer = new short[len];
					v[f] = buffer;
					i.readUnsigned16(buffer,0,len);
				}
				i.close();
			}
			catch (IOException e) {
				slf4jlogger.error("", e);
				throw new DicomException("Failed to read value (length "+valueLength+" dec) in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		return v;
	}
	
	/**
	 * <p>Get the value of this attribute as a short array for one selected frame.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from which to get a {@link java.nio.ShortBuffer ShortBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk, because
	 * {@link com.pixelmed.dicom.AttributeFactory AttributeFactory} silently created an instance of this
	 * class rather than an in-memory {@link com.pixelmed.dicom.OtherWordAttribute OtherWordAttribute}.</p>
	 *
	 * @param	frameNumber		from 0
	 * @param	numberOfFrames	the number of frames
	 * @return					the values as an array of short
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[] getShortValuesForSelectedFrame(int frameNumber,int numberOfFrames) throws DicomException {
//System.err.println("OtherWordAttributeOnDisk.getShortValuesForSelectedFrame(): lazy read of selected frame "+frameNumber+" into heap allocated memory, rather than using memory mapped buffer :(");
		short[] buffer = null;
		long bytesperframe = valueLength / numberOfFrames;
		long byteoffsetfromstartofattributevalue = bytesperframe*frameNumber;
		if (byteoffsetfromstartofattributevalue+bytesperframe <= valueLength) {
			int len = (int)(bytesperframe/2);
			buffer = new short[len];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),bigEndian);
				i.skipInsistently(byteOffset+byteoffsetfromstartofattributevalue);
				i.readUnsigned16(buffer,0,len);
				i.close();
			}
			catch (IOException e) {
				throw new DicomException("Failed to read frame "+frameNumber+" of "+numberOfFrames+" frames, size "+bytesperframe+" dec and offset "+byteoffsetfromstartofattributevalue+" dec bytes in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		else {
				throw new DicomException("Requested frame "+frameNumber+" of "+numberOfFrames+" frames, size "+bytesperframe+" dec and offset "+byteoffsetfromstartofattributevalue+" dec bytes to read value exceeds length "+valueLength+" dec in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
		}
		return buffer;
	}

	/**
	 * <p>Get the value representation of this attribute (OW).</p>
	 *
	 * @return	'O','W' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OW; }

}


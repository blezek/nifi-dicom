/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import com.pixelmed.utils.CopyStream;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Byte (OB) attributes whose values are not memory resident.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.OtherWordAttributeOnDisk
 *
 * @author	dclunie
 */
public class OtherByteAttributeOnDisk extends OtherAttributeOnDisk {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherByteAttributeOnDisk.java,v 1.13 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(OtherByteAttributeOnDisk.class);

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherByteAttributeOnDisk(AttributeTag t) {
		super(t);
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
	public OtherByteAttributeOnDisk(AttributeTag t,long vl,DicomInputStream i,long byteOffset) throws IOException, DicomException {
		super(t,vl,i,byteOffset);
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
	public OtherByteAttributeOnDisk(AttributeTag t,Long vl,DicomInputStream i,Long byteOffset) throws IOException, DicomException {
		super(t,vl,i,byteOffset);
	}

	/***/
	public long getPaddedVL() {
		long vl = getVL();
		if (vl%2 != 0) ++vl;
		return vl;
	}
	
	/**
	 * @param	o
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void write(DicomOutputStream o) throws DicomException, IOException {
		//throw new DicomException("Internal error - unsupported operation, write of OtherByteAttributeOnDisk");
		writeBase(o);
		if (valueLength > 0) {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			CopyStream.skipInsistently(in,byteOffset);
			CopyStream.copy(in,o,valueLength);
			in.close();
			long npad = getPaddedVL() - valueLength;
			while (npad-- > 0) o.write(0x00);
		}
	}

	/**
	 * <p>Get the values of this attribute as a byte array.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk, because
	 * {@link com.pixelmed.dicom.AttributeFactory AttributeFactory} silently created an instance of this
	 * class rather than an in-memory {@link com.pixelmed.dicom.OtherByteAttribute OtherByteAttribute}.</p>
	 *
	 * @return					the values as an array of bytes
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public byte[] getByteValues() throws DicomException {
//System.err.println("OtherByteAttributeOnDisk.getByteValues(): lazy read into heap allocated memory, rather than using memory mapped buffer :(");
		byte[] buffer = null;
		if (valueLength > 0) {
			buffer = new byte[(int)valueLength];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),false/*bigEndian - byte order is irrelevant*/);
				i.skipInsistently(byteOffset);
				i.readInsistently(buffer,0,(int)valueLength);
				i.close();
			}
			catch (IOException e) {
				slf4jlogger.error("", e);
				throw new DicomException("Failed to read value (length "+valueLength+" dec) in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		return buffer;
	}

	/**
	 * <p>Get the values of this attribute as multiple byte arrays, one per frame.</p>
	 *
	 * <p>Caller needs to supply the number for frames so that pixel data can be split
	 * across per-frame arrays (since not necessarily known when this attribute was created
	 * or read.</p>
	 *
	 * <p>This allocates new arrays of sufficient length, which may fail if they are too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @param	numberOfFrames	the number of frames
	 * @return					the values as an array of arrays of bytes
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public byte[][] getByteValuesPerFrame(int numberOfFrames) throws DicomException {
//System.err.println("OtherByteAttributeOnDisk.getByteValuesPerFrame(): lazy read of of all frames into heap allocated memory as per-frame arrays, rather than using memory mapped buffer :(");
		byte[][] v = null;
		if (valueLength > 0) {
			int framesize = (int)(valueLength/numberOfFrames);
			v = new byte[numberOfFrames][];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),false/*bigEndian - byte order is irrelevant*/);
				i.skipInsistently(byteOffset);
				for (int f=0; f<numberOfFrames; ++f) {
					byte[] buffer = new byte[framesize];
					v[f] = buffer;
					i.readInsistently(buffer,0,framesize);
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
	 * <p>Get the value of this attribute as a byte array for one selected frame.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk, because
	 * {@link com.pixelmed.dicom.AttributeFactory AttributeFactory} silently created an instance of this
	 * class rather than an in-memory {@link com.pixelmed.dicom.OtherByteAttribute OtherByteAttribute}.</p>
	 *
	 * @param	frameNumber		from 0
	 * @param	numberOfFrames	the number of frames
	 * @return					the values as an array of short
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public byte[] getByteValuesForSelectedFrame(int frameNumber,int numberOfFrames) throws DicomException {
//System.err.println("OtherWordAttributeOnDisk.getByteValuesForSelectedFrame(): lazy read of selected frame "+frameNumber+" into heap allocated memory, rather than using memory mapped buffer :(");
		byte[] buffer = null;
		int framesize = (int)(valueLength/numberOfFrames);
		long byteoffsetfromstartofattributevalue = framesize*(long)frameNumber;
		if (byteoffsetfromstartofattributevalue+framesize <= valueLength) {
			buffer = new byte[framesize];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),false/*bigEndian - byte order is irrelevant*/);
				i.skipInsistently(byteOffset+byteoffsetfromstartofattributevalue);
				i.readInsistently(buffer,0,framesize);
				i.close();
			}
			catch (IOException e) {
				throw new DicomException("Failed to read frame "+frameNumber+" of "+numberOfFrames+" frames, size "+framesize+" dec and offset "+byteoffsetfromstartofattributevalue+" dec bytes in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		else {
				throw new DicomException("Requested frame "+frameNumber+" of "+numberOfFrames+" frames, size "+framesize+" dec and offset "+byteoffsetfromstartofattributevalue+" dec bytes to read value exceeds length "+valueLength+" dec in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
		}
		return buffer;
	}

	/**
	 * <p>Get the value representation of this attribute (OB).</p>
	 *
	 * @return	'O','B' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OB; }
}


/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.FileReaper;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Word (OW) attributes whose values are not memory resident but rather are stored in multiple files on disk.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class OtherWordAttributeMultipleFilesOnDisk extends Attribute {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherWordAttributeMultipleFilesOnDisk.java,v 1.2 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(OtherWordAttributeMultipleFilesOnDisk.class);
	
	protected File[] files;
	protected long[] byteOffsets;
	protected long[] lengths;
	protected boolean deleteFilesWhenNoLongerNeeded;

	protected boolean bigEndian;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * Any file set later will be expected to be little endian.
	 *
	 * @param	t	the tag of the attribute
	 * @param	bigEndian	big endian, false if little endian
	 */
	public OtherWordAttributeMultipleFilesOnDisk(AttributeTag t,boolean bigEndian) {
		super(t);
		this.bigEndian = bigEndian;
	}

	/**
	 * <p>Read an attribute from a set of files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	files		the input files
	 * @param	byteOffsets	the byte offsets in the files of the start of the data, one entry for each file, or null if 0 for all files
	 * @param	lengths		the lengths in the files from the the start of the data, one entry for each file, or null if the remaining file length after the byteOffset, if any
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeMultipleFilesOnDisk(AttributeTag t,File[] files,long[] byteOffsets,long[] lengths,boolean bigEndian) throws IOException, DicomException {
		this(t,bigEndian);
		setFiles(files,byteOffsets,lengths);
	}

	/**
	 * <p>Read an attribute from a set of files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	fileNames	the input files
	 * @param	byteOffsets	the byte offsets in the files of the start of the data, one entry for each file, or null if 0 for all files
	 * @param	lengths		the lengths in the files from the the start of the data, one entry for each file, or null if the remaining file length after the byteOffset, if any
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeMultipleFilesOnDisk(AttributeTag t,String[] fileNames,long[] byteOffsets,long[] lengths,boolean bigEndian) throws IOException, DicomException {
		this(t,bigEndian);
		File[] files = new File[fileNames.length];
		for (int i=0; i<fileNames.length; ++i) {
			files[i] = new File(fileNames[i]);
		}
		setFiles(files,byteOffsets,lengths);
	}

	/**
	 * <p>Read an attribute from a set of files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	files		the input files
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeMultipleFilesOnDisk(AttributeTag t,File[] files,boolean bigEndian) throws IOException, DicomException {
		this(t,files,null,null,bigEndian);
	}

	/**
	 * <p>Read an attribute from a set of files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	fileNames	the input files
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherWordAttributeMultipleFilesOnDisk(AttributeTag t,String[] fileNames,boolean bigEndian) throws IOException, DicomException {
		this(t,fileNames,null,null,bigEndian);
	}

	/**
	 * @return		the files containing the data
	 */
	public File[] getFiles() { return files; }

	/**
	 * @return		the per-file byte offsets to the frame data
	 */
	public long[] getByteOffsets() { return byteOffsets; }

	/**
	 * @return		the per-file lengths of the data for each frame (after the byte offset) in bytes
	 */
	public long[] getLengths() { return lengths; }

	/**
	 * @param	files		the input files
	 * @param	byteOffsets	the byte offsets in the files of the start of the data, one entry for each file, or null if 0 for all files
	 * @param	lengths		the lengths in the files from the the start of the data, one entry for each file, or null if the remaining file length after the byteOffset, if any
	 * @throws	IOException
	 */
	public void setFiles(File[] files,long[] byteOffsets,long[] lengths) throws IOException {
		this.files = files;
		if (byteOffsets == null) {
			this.byteOffsets = new long[files.length];
		}
		else {
			this.byteOffsets = byteOffsets;
		}
		if (lengths == null) {
			this.lengths = new long[files.length];
		}
		else {
			this.lengths = lengths;
		}
	
		valueLength=0;
		for (int i=0; i<files.length; ++i) {
			long length = 0;
			if (lengths == null) {
				length = files[i].length();
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.setFiles(): files["+i+"] = "+files[i].getCanonicalPath()+" length() = "+length);
				if (byteOffsets != null) {
					length -= byteOffsets[i];
				}
				this.lengths[i] = length;
			}
			else {
				length = lengths[i];
			}
			valueLength += length;
		}
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.setFiles(): valueLength = "+valueLength);
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
		writeBase(o);
		if (valueLength > 0) {
			for (int i=0; i<files.length; ++i) {
				File file = files[i];
				long byteOffset = byteOffsets[i];
				long length = lengths[i];
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				CopyStream.skipInsistently(in,byteOffset);
				CopyStream.copy(in,o,length);
				in.close();
			}
			long npad = getPaddedVL() - valueLength;
			while (npad-- > 0) o.write(0x00);
		}
	}
	
	/***/
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" []");		// i.e. don't really dump values ... too many
		return str.toString();
	}


	/**
	 * <p>Get the values of this attribute as a short array.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the byte values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @return					the values as an array of bytes
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[] getShortValues() throws DicomException {
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.getWordValues(): lazy read into heap allocated memory, rather than using memory mapped buffer :(");
		short[] buffer = null;
		if (valueLength > 0) {
			buffer = new short[(int)(valueLength/2)];
			try {
				int count=0;
				for (int f=0; f<files.length; ++f) {
					File file = files[f];
					BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),bigEndian);
					i.skipInsistently(byteOffsets[f]);
					int lengthInWords = (int)(lengths[f]/2);
					i.readUnsigned16(buffer,count,lengthInWords);
					i.close();
					count+=lengthInWords;
				}
			}
			catch (IOException e) {
				slf4jlogger.error("",e);
				throw new DicomException("Failed to read value (length "+valueLength+" dec) in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		return buffer;
	}

	/**
	 * <p>Get the values of this attribute as multiple short arrays, one per frame.</p>
	 *
	 * <p>Caller does not need to supply the number for frames since known when this attribute was created.</p>
	 *
	 * <p>This allocates a new array of sufficient length, which may fail if it is too large,
	 * and defeats the point of leaving the short values on disk in the first place. However, it
	 * is a fallback for when the caller does not want to go to the trouble of creating a
	 * {@link java.nio.MappedByteBuffer MappedByteBuffer} from the file,
	 * or more likely is not even aware that the attribute values have been left on disk.</p>
	 *
	 * @return					the values as an array of arrays of shorts
	 * @throws	DicomException	thrown if values cannot be read
	 */
	public short[][] getShortValuesPerFrame() throws DicomException {
//System.err.println("OtherWordAttributeMultipleFrameArrays.getShortValuesPerFrame()");
		// actually copy the array - caller might null it's contents later :(
		short[][] v = new short[files.length][];
		for (int f=0; f<files.length; ++f) {
			File file = files[f];
			try {
				BinaryInputStream i = new BinaryInputStream(new FileInputStream(file),bigEndian);
				i.skipInsistently(byteOffsets[f]);
				int lengthInWords = (int)(lengths[f]/2);
				i.readUnsigned16(v[f],0,lengthInWords);
			}
			catch (IOException e) {
				slf4jlogger.error("",e);
				throw new DicomException("Failed to read value (length "+valueLength+" dec) in delayed read of "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
		return v;
	}

	/**
	 */
	public void removeValues() {
		files=null;
		byteOffsets=null;
		lengths=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	public void deleteFilesWhenNoLongerNeeded() {
		deleteFilesWhenNoLongerNeeded=true;
	}

	protected void finalize() throws Throwable {
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.finalize()");
		if (deleteFilesWhenNoLongerNeeded) {
			if (files != null) {
				for (int i=0; i<files.length; ++i) {
					File file = files[i];
					if (file != null) {
						if (file.delete()) {
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.finalize(): Successfully deleted temporary file "+file);
						}
						else {
//System.err.println("OtherWordAttributeMultipleFilesOnDisk.finalize(): Failed to delete temporary file "+file+" so adding to reaper list");
							FileReaper.addFileToDelete(file.getCanonicalPath());
						}
					}
					files[i]=null;
				}
				files=null;
			}
		}
		super.finalize();
	}

	/**
	 * <p>Get the value representation of this attribute (OW).</p>
	 *
	 * @return	'O','W' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OW; }
}


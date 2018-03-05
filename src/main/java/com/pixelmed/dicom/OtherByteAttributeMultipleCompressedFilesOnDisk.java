/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import java.util.zip.GZIPInputStream;

import com.pixelmed.utils.CopyStream;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other Byte (OB) attributes whose values are not memory resident but rather are stored in multiple compressed files on disk.</p>
 *
 * <p>Whether or not decompression of compressed file format is supported is not checked until an attempt is made to write decompressed bytes.</p>
 *
 * <p>Which decompressor to use is determined by the file name extension (.gz for gzip, .Z for unix compress, etc., case insensitive).</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class OtherByteAttributeMultipleCompressedFilesOnDisk extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherByteAttributeMultipleCompressedFilesOnDisk.java,v 1.7 2017/01/24 10:50:38 dclunie Exp $";
	
	protected File[] files;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherByteAttributeMultipleCompressedFilesOnDisk(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Read an attribute from a set of compressed files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	files		the input files
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherByteAttributeMultipleCompressedFilesOnDisk(AttributeTag t,long vl,File[] files) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(files,vl);
	}

	/**
	 * <p>Read an attribute from a set of compressed files.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	fileNames	the input files
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public OtherByteAttributeMultipleCompressedFilesOnDisk(AttributeTag t,long vl,String[] fileNames) throws IOException, DicomException {
		super(t);
		File[] files = new File[fileNames.length];
		for (int i=0; i<fileNames.length; ++i) {
			files[i] = new File(fileNames[i]);
		}
		doCommonConstructorStuff(files,vl);
	}

	/**
	 * @param	files		the input files
	 * @param	vl			the value length of the attribute
	 * @throws	IOException
	 */
	private void doCommonConstructorStuff(File[] files,long vl) throws IOException {
		this.files = files;
		valueLength=vl;
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
//System.err.println("OtherByteAttributeMultipleCompressedFilesOnDisk.write(): File: ["+i+"] "+files[i]);
				InputStream in = null;
				String suffix = file.getName().toUpperCase();
				if (suffix.endsWith(".GZ")) {
					in = new GZIPInputStream(new FileInputStream(file));
				}
				else if (suffix.endsWith(".Z")) {
					try {
						Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.compress.compressors.z.ZCompressorInputStream");
						Class [] argTypes  = {InputStream.class};
						Object[] argValues = {new FileInputStream(file)};
						in = (InputStream)(classToUse.getConstructor(argTypes).newInstance(argValues));
					}
					catch (java.lang.reflect.InvocationTargetException e) {
						throw new DicomException("Not a correctly encoded Unix compress (.Z) bitstream - "+e);
					}
					catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
						throw new DicomException("Could not instantiate Unix compress (.Z) codec - "+e);
					}
				}
				else if (suffix.endsWith(".BZ2")) {
					try {
						Class classToUse = Thread.currentThread().getContextClassLoader().loadClass("org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream");
						Class [] argTypes  = {InputStream.class};
						Object[] argValues = {new FileInputStream(file)};
						in = (InputStream)(classToUse.getConstructor(argTypes).newInstance(argValues));
					}
					catch (java.lang.reflect.InvocationTargetException e) {
						throw new DicomException("Not a correctly encoded bzip2 bitstream - "+e);
					}
					catch (Exception e) {	// may be ClassNotFoundException,NoSuchMethodException,InstantiationException
						throw new DicomException("Could not instantiate bzip2 codec - "+e);
					}
				}
				else {
					throw new DicomException("No decompressor found for file with extension "+suffix);
				}
				CopyStream.copy(in,o);
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
	 */
	public void removeValues() {
		files=null;
		valueMultiplicity=0;
		valueLength=0;
	}

	/**
	 * <p>Get the value representation of this attribute (OB).</p>
	 *
	 * @return	'O','B' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.OB; }
}


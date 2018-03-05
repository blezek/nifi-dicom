/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.FileReaper;

/**
 * <p>An abstract class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Other (OB or OW) attributes whose values are not memory resident.</p>
 *
 * <p>Used as a base class for {@link com.pixelmed.dicom.OtherByteAttributeOnDisk OtherByteAttributeOnDisk} and {@link com.pixelmed.dicom.OtherWordAttributeOnDisk OtherWordAttributeOnDisk}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.OtherByteAttributeOnDisk
 * @see com.pixelmed.dicom.OtherWordAttributeOnDisk
 *
 * @author	dclunie
 */
public abstract class OtherAttributeOnDisk extends Attribute {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/OtherAttributeOnDisk.java,v 1.7 2017/01/24 10:50:37 dclunie Exp $";

	protected long byteOffset;
	protected File file;
	protected boolean deleteFilesWhenNoLongerNeeded;

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public OtherAttributeOnDisk(AttributeTag t) {
		super(t);
		byteOffset = 0;
		file = null;
		deleteFilesWhenNoLongerNeeded=false;
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public OtherAttributeOnDisk(AttributeTag t,long vl,DicomInputStream i,long byteOffset) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl,i,byteOffset);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	public OtherAttributeOnDisk(AttributeTag t,Long vl,DicomInputStream i,Long byteOffset) throws IOException, DicomException {
		super(t);
		doCommonConstructorStuff(vl.longValue(),i,byteOffset.longValue());
	}

	/**
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException		if an I/O error occurs
	 * @throws	DicomException	if error in DICOM encoding
	 */
	private void doCommonConstructorStuff(long vl,DicomInputStream i,long byteOffset) throws IOException, DicomException {
		valueLength=vl;
		this.byteOffset=byteOffset;
		file=i.getFile();
		if (file == null) {
				throw new DicomException("Cannot have an OtherAttributeOnDisk without a file available in the DicomInputStream");
		}
		deleteFilesWhenNoLongerNeeded=false;

		if (vl > 0) {
			try {
				i.skipInsistently(vl);
			}
			catch (IOException e) {
				throw new DicomException("Failed to skip value (length "+vl+" dec) in "+ValueRepresentation.getAsString(getVR())+" attribute "+getTag());
			}
		}
	}
	
	public String toString(DicomDictionary dictionary) {
		StringBuffer str = new StringBuffer();
		str.append(super.toString(dictionary));
		str.append(" []");		// i.e. don't really dump values ... too many
		return str.toString();
	}

	/**
	 * @return		the offset from the start of the object in bytes
	 */
	public long getByteOffset() { return byteOffset; }

	/**
	 * @return		the file containing the data
	 */
	public File getFile() { return file; }

	/**
	 * <p>Change the file containing the data, for example if it has been renamed.</p>
	 *
	 * <p>The existing byteOffset value is unchanged.</p>
	 *
	 * @param	file	the new file containing the data
	 */
	public void setFile(File file) { this.file = file; }

	/**
	 * <p>Change the file containing the data, for example if it is a new, perhaps temporary, file containing only pixel data.</p>
	 *
	 * <p>The value length is set to the length of the file minus the byteOffset.</p>
	 *
	 * @param	file		the new file containing the data
	 * @param	byteOffset	the byte offset in the input stream of the start of the data
	 * @throws	IOException	if cannot obtain the length of the file
	 */
	public void setFile(File file,long byteOffset) throws IOException {
		this.file = file;
		this.byteOffset=byteOffset;
		valueLength=file.length() - byteOffset;
	}

	public void removeValues() {
		file=null;
		byteOffset=0;
		valueMultiplicity=0;
		valueLength=0;
	}

	public void deleteFilesWhenNoLongerNeeded() {
		deleteFilesWhenNoLongerNeeded=true;
	}

	protected void finalize() throws Throwable {
//System.err.println("OtherAttributeOnDisk.finalize()");
		if (deleteFilesWhenNoLongerNeeded) {
			if (file != null) {
				if (file.delete()) {
//System.err.println("OtherAttributeOnDisk.finalize(): Successfully deleted temporary file "+file);
				}
				else {
//System.err.println("OtherAttributeOnDisk.finalize(): Failed to delete temporary file "+file+" so adding to reaper list");
					FileReaper.addFileToDelete(file.getCanonicalPath());
				}
			}
			file=null;
		}
		super.finalize();
	}
	
}


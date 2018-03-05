/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A class that extends {@link com.pixelmed.dicom.BinaryInputStream BinaryInputStream} by adding
 * the concept of transfer syntaxes, for a (possible) meta information header and a data set.</p>
 *
 * <p>Note this class does not automatically switch from meta information header to data set
 * transfer syntaxes. That is the responsibility of the caller parsing the individual attributes
 * (such as by reaching the end of the meta information group length, and then calling
 * {@link #setReadingDataSet() setReadingDataSet()}.</p>
 *
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.DicomOutputStream
 *
 * @author	dclunie
 */
public class DicomInputStream extends BinaryInputStream {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomInputStream.java,v 1.15 2017/01/24 10:50:37 dclunie Exp $";

	/***/
	private TransferSyntax TransferSyntaxToReadDataSet;
	/***/
	private TransferSyntax TransferSyntaxToReadMetaHeader;
	/***/
	private TransferSyntax TransferSyntaxInUse;
	/***/
	private boolean readingDataSet;

	/***/
	private long byteOffsetOfStartOfData;

	/**
	 * @param	uid				use this TransferSyntax (for the meta information header if tryMeta is true, else for the data set)
	 * @param	tryMeta			if true look for a meta information header
	 * @throws	IOException		if an I/O error occurs
	 */
	private void initializeTransferSyntax(String uid,boolean tryMeta) throws IOException {
//System.err.println("initializeTransferSyntax: uid="+uid+" tryMeta="+tryMeta);
//System.err.println("initializeTransferSyntax: markSupported()="+markSupported());
		TransferSyntaxToReadMetaHeader = null;
		TransferSyntaxToReadDataSet = null;

		byte b[] = new byte[8];

		// First make use of argument that overrides guesswork at transfer syntax ...

		if (uid != null) {
			TransferSyntax ts = new TransferSyntax(uid);
			if (tryMeta) {
				TransferSyntaxToReadMetaHeader = ts;	// specified UID is transfer syntax to read metaheader
			}
			else {
				TransferSyntaxToReadDataSet = ts;	// specified UID is transfer syntax to read dataset (there is no metaheader)
			}
		}
		// else transfer syntax has to be determined by either guesswork or metaheader ...
		
		if (tryMeta) {
//System.err.println("initializeTransferSyntax: looking for preamble");
			// test for metaheader prefix after 128 byte preamble
			if (markSupported()) mark(140);
			boolean skipSucceeded = true;
			try {
				skipInsistently(128);
			}
			catch (IOException e) {
				skipSucceeded=false;
			}
			if (skipSucceeded && read(b,0,4) == 4 && new String(b,0,4).equals("DICM")) {
//System.err.println("initializeTransferSyntax: detected DICM");
				if (TransferSyntaxToReadMetaHeader == null) {		// guess only if not specified as an argument
//System.err.println("initializeTransferSyntax: trying to guess TransferSyntaxToReadMetaHeader");
					if (markSupported()) {
						mark(8);
						if (read(b,0,6) == 6) {				// the first 6 bytes of the first attribute tag in the metaheader
							TransferSyntaxToReadMetaHeader = 
								Character.isUpperCase((char)(b[4])) && Character.isUpperCase((char)(b[5]))
								? new TransferSyntax(TransferSyntax.ExplicitVRLittleEndian)	// standard
								: new TransferSyntax(TransferSyntax.ImplicitVRLittleEndian);	// old draft (e.g. used internally on GE IOS platform)
						}
						else {
							TransferSyntaxToReadMetaHeader = new TransferSyntax(TransferSyntax.ExplicitVRLittleEndian);
						}
						reset();
					}
					else {
						// can't guess since can't rewind ... insist on standard transfer syntax
						TransferSyntaxToReadMetaHeader = new TransferSyntax(TransferSyntax.ExplicitVRLittleEndian);
//System.err.println("initializeTransferSyntax: can't rewind so assuming TransferSyntaxToReadMetaHeader is ExplicitVRLittleEndian");
					}
				}
				byteOffsetOfStartOfData=132;
			}
			else {
//System.err.println("initializeTransferSyntax: no preamble");
				// no preamble, so rewind and try using the specified transfer syntax (if any) for the dataset instead
				if (markSupported()) {
					reset();
					TransferSyntaxToReadDataSet = TransferSyntaxToReadMetaHeader;	// may be null anyway if no uid argument specified
					byteOffsetOfStartOfData=0;
				}
				else {
					throw new IOException("Not a DICOM PS 3.10 file - no DICM after preamble in metaheader, and can't rewind input");
				}
			}
		}
		
		// at this point either we have succeeded or failed at finding a metaheader, or we didn't look
		// so we either have a detected or specified transfer syntax for the metaheader, or the dataset, or nothing at all
		
		if (TransferSyntaxToReadDataSet == null && TransferSyntaxToReadMetaHeader == null) {	// was not specified as an argument and there is no metaheader
//System.err.println("initializeTransferSyntax: having to try and guess transfer syntax");
			guessTransferSyntaxToReadDataSet();
		}

		if (TransferSyntaxToReadMetaHeader != null) {
			setReadingMetaHeader();
		}
		else {
			setReadingDataSet();
		}

		if (TransferSyntaxInUse == null) throw new IOException("Not a DICOM file (or can't detect Transfer Syntax)");
		
		// leaves us positioned at start of group and element tags (for either metaheader or dataset)
//System.err.println("initializeTransferSyntax: TransferSyntaxToReadMetaHeader="+TransferSyntaxToReadMetaHeader);
//System.err.println("initializeTransferSyntax: TransferSyntaxToReadDataSet="+TransferSyntaxToReadDataSet);
//System.err.println("initializeTransferSyntax: TransferSyntaxInUse="+TransferSyntaxInUse);
	}
	
	public void guessTransferSyntaxToReadDataSet() throws IOException {
		byte b[] = new byte[8];
			boolean bigendian = false;
			boolean explicitvr = false;
			if (markSupported()) {
				mark(10);
				if (read(b,0,8) == 8) {
//System.err.print("guessTransferSyntaxToReadDataSet: read beginning of first attribute = "+com.pixelmed.utils.HexDump.dump(b));
					// examine probable group number ... assume <= 0x00ff
					if (b[0] < b[1]) bigendian=true;
					else if (b[0] == 0 && b[1] == 0) {
						// blech ... group number is zero
						// no point in looking at element number
						// as it will probably be zero too (group length)
						// try the 32 bit value length of implicit vr
						if (b[4] < b[7]) bigendian=true;
					}
					// else little endian
//System.err.println("guessTransferSyntaxToReadDataSet: bigendian="+bigendian);
					if (Character.isUpperCase((char)(b[4])) && Character.isUpperCase((char)(b[5]))) explicitvr=true;
//System.err.println("guessTransferSyntaxToReadDataSet: b[4]="+(char)(b[4]));
//System.err.println("guessTransferSyntaxToReadDataSet: b[5]="+(char)(b[5]));
//System.err.println("guessTransferSyntaxToReadDataSet: explicitvr="+explicitvr);
				}
				// go back to start of dataset
				reset();
			}
			// else can't guess or unrecognized ... assume default ImplicitVRLittleEndian (most common without metaheader due to Mallinckrodt CTN default)

			if (bigendian)
				if (explicitvr)
					TransferSyntaxToReadDataSet = new TransferSyntax(TransferSyntax.ExplicitVRBigEndian);
				else
					throw new IOException("Not a DICOM file (masquerades as explicit VR big endian)");
			else
				if (explicitvr)
					TransferSyntaxToReadDataSet = new TransferSyntax(TransferSyntax.ExplicitVRLittleEndian);
				else
					TransferSyntaxToReadDataSet = new TransferSyntax(TransferSyntax.ImplicitVRLittleEndian);
	}

	//public DicomInputStream(String name) throws IOException {
	//	super(name,true);
	//	initializeTransferSyntax("1.2.840.10008.1.2.1",true);
	//}

	/**
	 * <p>Construct a stream to read DICOM data sets from the supplied stream.</p>
	 *
	 * <p>Look for a meta information header; if absent guess at a transfer syntax based on the contents.</p>
	 *
	 * @param	i		the input stream to read from
	 * @throws	IOException		if an I/O error occurs
	 */
	public DicomInputStream(InputStream i) throws IOException {
		super(i,true);
		initializeTransferSyntax(null,true);	// allow guessing at transfer syntax, try to find metaheader
	}

	/**
	 * <p>Construct a stream to read DICOM data sets from the supplied stream.</p>
	 *
	 * <p>Look for a meta information header; if absent guess at a transfer syntax based on the contents.</p>
	 *
	 * @param	file			the file to read from
	 * @throws	IOException		if an I/O error occurs
	 */
	public DicomInputStream(File file) throws IOException {
		super(file,true);
		initializeTransferSyntax(null,true);	// allow guessing at transfer syntax, try to find metaheader
	}

	/**
	 * <p>Construct a stream to read DICOM data sets from the supplied stream.</p>
	 *
	 * @param	i			the input stream to read from
	 * @param	transferSyntaxUID	use this transfer syntax (may be null)
	 * @param	tryMeta			if true, try to find a meta information header
	 * @throws	IOException		if an I/O error occurs
	 */
	public DicomInputStream(InputStream i,String transferSyntaxUID,boolean tryMeta) throws IOException {
		super(i,true);
		initializeTransferSyntax(transferSyntaxUID,tryMeta);
	}

	/**
	 * <p>Construct a stream to read DICOM data sets from the supplied stream.</p>
	 *
	 * @param	file			the file to read from
	 * @param	transferSyntaxUID	use this transfer syntax (may be null)
	 * @param	tryMeta			if true, try to find a meta information header
	 * @throws	IOException		if an I/O error occurs
	 */
	public DicomInputStream(File file,String transferSyntaxUID,boolean tryMeta) throws IOException {
		super(file,true);
		initializeTransferSyntax(transferSyntaxUID,tryMeta);
	}

	/**
	 * <p>Specify what transfer syntax to use when switching from reading
	 * the meta information header to reading the data set.</p>
	 *
	 * @param	ts	transfer syntax to use for data set
	 */
	public void setTransferSyntaxToReadDataSet(TransferSyntax ts) {
		TransferSyntaxToReadDataSet=ts;
	}

	/**
	 * <p>Switch to the transfer syntax for reading the dataset.</p>
	 */
	public void setReadingDataSet() {
		TransferSyntaxInUse = TransferSyntaxToReadDataSet;
		setEndian(TransferSyntaxInUse.isBigEndian());
		readingDataSet=true;
	}

	/**
	 * <p>Are we reading the dataset?</p>
	 *
	 * @return	true if reading the dataset, false if reading the meta information header
	 */
	public boolean areReadingDataSet() {
		return readingDataSet;
	}

	/**
	 * <p>Switch to the transfer syntax for reading the meta information header.</p>
	 */
	public void setReadingMetaHeader() {
		TransferSyntaxInUse = TransferSyntaxToReadMetaHeader;
		setEndian(TransferSyntaxInUse.isBigEndian());
		readingDataSet=false;
	}

	/**
	 * <p>Are we reading the meta information header?</p>
	 *
	 * @return	true if reading the meta information header, false if reading the dataset
	 */
	public boolean areReadingMetaHeader() {
		return !readingDataSet;
	}

	/**
	 * <p>Do we have a meta information header?</p>
	 *
	 * @return	true if there is a meta information header, false if not
	 */
	public boolean haveMetaHeader() {
		return TransferSyntaxToReadMetaHeader != null;
	}

	/**
	 * <p>Get the transfer syntax currently in use.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxInUse() { return TransferSyntaxInUse; }

	/**
	 * <p>Get the transfer syntax to be used for reading the data set.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxToReadDataSet() { return TransferSyntaxToReadDataSet; }

	/**
	 * <p>Get the transfer syntax to be used for reading the meta information header.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxToReadMetaHeader() { return TransferSyntaxToReadMetaHeader; }

	/**
	 * <p>Get the byte offset of the start of the dataset or meta information header.</p>
	 *
	 * <p>Will be 0 if no preamble, 132 if a preamble.</p>
	 *
	 * @return	the byte offset (from 0 being the start of the stream)
	 */
	public long getByteOffsetOfStartOfData() { return byteOffsetOfStartOfData; }

}



/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A class that extends {@link com.pixelmed.dicom.BinaryOutputStream BinaryOutputStream} by adding
 * the concept of transfer syntaxes, for a (possible) meta information header and a data set.</p>
 *
 * <p>Note this class does not automatically switch from meta information header to data set
 * transfer syntaxes. That is the responsibility of the caller writing the individual attributes
 * (such as by reaching the end of the meta information group length, and then calling
 * {@link #setWritingDataSet() setWritingDataSet()}.</p>
 *
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.DicomInputStream
 *
 * @author	dclunie
 */
public class DicomOutputStream extends BinaryOutputStream {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomOutputStream.java,v 1.12 2017/01/24 10:50:37 dclunie Exp $";

	/***/
	private static final byte preamble[] = new byte[128];
	/***/
	private static final byte DICM[] = "DICM".getBytes();
	
	/***/
	private TransferSyntax transferSyntaxToWriteDataSet;
	/***/
	private TransferSyntax transferSyntaxToWriteMetaHeader;
	/***/
	private TransferSyntax transferSyntaxInUse;
	/***/
	private boolean writingDataSet;

	/***/
	private long byteOffsetOfStartOfData;

	/**
	 * @param	metaTransferSyntaxUID
	 * @param	dataTransferSyntaxUID
	 * @throws	IOException		if an I/O error occurs
	 */
	private void initializeTransferSyntax(String metaTransferSyntaxUID,String dataTransferSyntaxUID) throws IOException { 
		transferSyntaxToWriteMetaHeader = metaTransferSyntaxUID == null ? null : new TransferSyntax(metaTransferSyntaxUID);
		   transferSyntaxToWriteDataSet = dataTransferSyntaxUID == null ? null : new TransferSyntax(dataTransferSyntaxUID);

		if (transferSyntaxToWriteMetaHeader != null) {
			write(preamble,0,128);
			write(DICM,0,4);
			setWritingMetaHeader();
			byteOffsetOfStartOfData=132;
		}
		else {
			setWritingDataSet();
			byteOffsetOfStartOfData=0;
		}
		// leaves us positioned at start of group and element tags
	}

	/**
	 * <p>Construct a stream to write DICOM data sets to the supplied stream.</p>
	 *
	 * <p>If the metaTransferSyntaxUID is not null, a 128 byte preamble plus "DICM" will also be written.</p>
	 *
	 * @param	o			the output stream to write to
	 * @param	metaTransferSyntaxUID	use this transfer syntax for the meta information header (may be null)
	 * @param	dataTransferSyntaxUID	use this transfer syntax for the data set
	 * @throws	IOException		if an I/O error occurs
	 */
	public DicomOutputStream(OutputStream o,String metaTransferSyntaxUID,String dataTransferSyntaxUID) throws IOException {
		super(o,false);
		initializeTransferSyntax(metaTransferSyntaxUID,dataTransferSyntaxUID);
	}

	/**
	 * <p>Specify what transfer syntax to use when switching from writing
	 * the meta information header to writing the data set.</p>
	 *
	 * @param	ts	transfer syntax to use for data set
	 */
	public void setTransferSyntaxToWriteDataSet(TransferSyntax ts) {
		transferSyntaxToWriteDataSet=ts;
	}

	/**
	 * <p>Switch to the transfer syntax for writing the dataset.</p>
	 */
	public void setWritingDataSet() {
		transferSyntaxInUse = transferSyntaxToWriteDataSet;
		setEndian(transferSyntaxInUse.isBigEndian());
		writingDataSet=true;
	}

	/**
	 * <p>Are we writing the dataset?</p>
	 *
	 * @return	true if writing the dataset, false if reading the meta information header
	 */
	public boolean areWritingDataSet() {
		return writingDataSet;
	}

	/**
	 * <p>Switch to the transfer syntax for writing the meta information header.</p>
	 */
	public void setWritingMetaHeader() {
		transferSyntaxInUse = transferSyntaxToWriteMetaHeader;
		setEndian(transferSyntaxInUse.isBigEndian());
		writingDataSet=false;
	}

	/**
	 * <p>Are we writing the meta information header?</p>
	 *
	 * @return	true if writing the meta information header, false if writing the dataset
	 */
	public boolean areWritingMetaHeader() {
		return !writingDataSet;
	}

	/**
	 * <p>Will we be writing a meta information header?</p>
	 *
	 * @return	true if there is a meta information header, false if not
	 */
	public boolean haveMetaHeader() {
		return transferSyntaxToWriteMetaHeader != null;
	}

	/**
	 * <p>Get the transfer syntax currently in use.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxInUse() { return transferSyntaxInUse; }

	/**
	 * <p>Get the transfer syntax to be used for writing the data set.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxToWriteDataSet() { return transferSyntaxToWriteDataSet; }

	/**
	 * <p>Get the transfer syntax to be used for writing the meta information header.</p>
	 *
	 * @return	the transfer syntax
	 */
	public TransferSyntax getTransferSyntaxToWriteMetaHeader() { return transferSyntaxToWriteMetaHeader; }

	/**
	 * <p>Get the byte offset of the start of the dataset or meta information header.</p>
	 *
	 * <p>Will be 0 if no preamble, 132 if a preamble.</p>
	 *
	 * @return	the byte offset (from 0 being the start of the stream)
	 */
	public long getByteOffsetOfStartOfData() { return byteOffsetOfStartOfData; }

	/**
	 * <p>Is the transfer syntax currently in use explicit VR ?</p>
	 *
	 * @return		true if explicit VR, false if implicit VR
	 */
	public boolean isExplicitVR() { return transferSyntaxInUse.isExplicitVR(); }

	/**
	 * <p>Is the transfer syntax currently in use implicit VR ?</p>
	 *
	 * @return		true if implicit VR, false if explicit VR
	 */
	public boolean isImplicitVR() { return transferSyntaxInUse.isImplicitVR(); }
}



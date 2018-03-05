/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class to represent the characteristics of a DICOM Transfer Syntax, which may
 * be instantiated from a UID or from basic characteristics, as well as static
 * UID strings for known Transfer Syntaxes, and static methods for extracting
 * the characteristics of known Transfer Syntaxes.</p>
 *
 * @author	dclunie
 */
public class TransferSyntax {

	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/TransferSyntax.java,v 1.32 2017/01/24 10:50:39 dclunie Exp $";

	/***/
	public static final String ImplicitVRLittleEndian              = "1.2.840.10008.1.2";
	/***/
	public static final String ExplicitVRLittleEndian              = "1.2.840.10008.1.2.1";
	/***/
	public static final String ExplicitVRBigEndian                 = "1.2.840.10008.1.2.2";
	/***/
	public static final String Default                             = ImplicitVRLittleEndian;
	/***/
	public static final String DeflatedExplicitVRLittleEndian      = "1.2.840.10008.1.2.1.99";
	/***/
	public static final String JPEGBaseline                        = "1.2.840.10008.1.2.4.50";
	/***/
	public static final String JPEGExtended                        = "1.2.840.10008.1.2.4.51";
	/***/
	public static final String JPEGLossless                        = "1.2.840.10008.1.2.4.57";
	/***/
	public static final String JPEGLosslessSV1                     = "1.2.840.10008.1.2.4.70";
	/***/
	public static final String JPEGLS                              = "1.2.840.10008.1.2.4.80";
	/***/
	public static final String JPEGNLS                             = "1.2.840.10008.1.2.4.81";
	/***/
	public static final String JPEG2000Lossless                    = "1.2.840.10008.1.2.4.90";
	/***/
	public static final String JPEG2000                            = "1.2.840.10008.1.2.4.91";
	/***/
	public static final String MPEG2MPML                           = "1.2.840.10008.1.2.4.100";
	/***/
	public static final String MPEG2MPHL                           = "1.2.840.10008.1.2.4.101";
	/***/
	public static final String MPEG4HP41                           = "1.2.840.10008.1.2.4.102";
	/***/
	public static final String MPEG4HP41BD                         = "1.2.840.10008.1.2.4.103";
	/***/
	public static final String MPEG4HP422D                         = "1.2.840.10008.1.2.4.104";
	/***/
	public static final String MPEG4HP423D                         = "1.2.840.10008.1.2.4.105";
	/***/
	public static final String MPEG4HP42ST                         = "1.2.840.10008.1.2.4.106";
	/***/
	public static final String RLE								   = "1.2.840.10008.1.2.5";
	/***/
	public static final String PixelMedBzip2ExplicitVRLittleEndian = "1.3.6.1.4.1.5962.300.1";
	/***/
	public static final String PixelMedEncapsulatedRawLittleEndian = "1.3.6.1.4.1.5962.300.2";
	/***/
	public static final String Papyrus3ImplicitVRLittleEndian	   = "1.2.840.10008.1.20";
	
	/***/
	protected String transferSyntaxUID;
	/***/
	protected String description;
	/***/
	protected boolean bigEndian;
	/***/
	protected boolean explicitVR;
	/***/
	protected boolean encapsulatedPixelData;
	/***/
	protected boolean lossy;
	/***/
	protected boolean recognized;
	/***/
	protected boolean jpegFamily;
	/***/
	protected String fileNameExtension;

	/**
	 * <p>Construct a Transfer Syntax using the specified UID, automatically determining its characteristics.</p>
	 *
	 * @param	uid		the UID to use to refer to this transfer syntax
	 */
	public TransferSyntax(String uid) {
		transferSyntaxUID=uid;
		description="Unrecognized";
		bigEndian=false;
		explicitVR=true;
		encapsulatedPixelData=false;
		lossy=false;
		recognized=false;
		fileNameExtension="unk";
		jpegFamily=false;

		if (transferSyntaxUID.equals(ImplicitVRLittleEndian)) {
			description="Implicit VR Little Endian";
			bigEndian=false;
			explicitVR=false;
			encapsulatedPixelData=false;
			lossy=false;
			recognized=true;
			fileNameExtension="dat";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(ExplicitVRLittleEndian)) {
			description="Explicit VR Little Endian";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=false;
			lossy=false;
			recognized=true;
			fileNameExtension="dat";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(ExplicitVRBigEndian)) {
			description="Explicit VR Big Endian";
			bigEndian=true;
			explicitVR=true;
			encapsulatedPixelData=false;
			lossy=false;
			recognized=true;
			fileNameExtension="dat";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(PixelMedEncapsulatedRawLittleEndian)) {
			description="PixelMed Encapsulated Raw Little Endian";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="dat";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(JPEGBaseline)) {
			description="JPEG Baseline";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="jpg";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEGExtended)) {
			description="JPEG Extended";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="jpg";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEG2000Lossless)) {
			description="JPEG 2000 Lossless Only";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="j2k";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEG2000)) {
			description="JPEG 2000";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="j2k";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEGLossless)) {
			description="JPEG Lossless";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="jpl";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEGLosslessSV1)) {
			description="JPEG Lossless SV1";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="jpl";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEGLS)) {
			description="JPEG-LS";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="jls";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(JPEGNLS)) {
			description="JPEG-LS Near-lossless";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="jls";
			jpegFamily=true;
		}
		else if (transferSyntaxUID.equals(MPEG2MPML)) {
			description="MPEG2 MPML";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mpg";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG2MPHL)) {
			description="MPEG2 MPHL";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mpg";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG4HP41)) {
			description="MPEG4 HP/4.1";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mp4";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG4HP41BD)) {
			description="MPEG4 HP/4.1 BD";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mp4";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG4HP422D)) {
			description="MPEG4 HP/4.2 2D";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mp4";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG4HP423D)) {
			description="MPEG4 HP/4.2 3D";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mp4";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(MPEG4HP42ST)) {
			description="MPEG4 HP/4.2 Stereo";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=true;
			recognized=true;
			fileNameExtension="mp4";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(RLE)) {
			description="RLE Lossless";
			bigEndian=false;
			explicitVR=true;
			encapsulatedPixelData=true;
			lossy=false;
			recognized=true;
			fileNameExtension="rle";
			jpegFamily=false;
		}
		else if (transferSyntaxUID.equals(Papyrus3ImplicitVRLittleEndian)) {
			description="Papyrus 3 Implicit VR Little Endian";
			bigEndian=false;
			explicitVR=false;
			encapsulatedPixelData=false;
			lossy=false;
			recognized=true;
			fileNameExtension="dat";
			jpegFamily=false;
		}
	}

	/**
	 * <p>Construct a Transfer Syntax using the specified UID and characteristics.</p>
	 *
	 * @param	transferSyntaxUID		the UID to use to refer to this transfer syntax
	 * @param	description				the description of this transfer syntax
	 * @param	explicitVR				true if an explicit VR transfer syntax
	 * @param	bigEndian				true if big-endian transfer syntax
	 * @param	encapsulatedPixelData	true if a pixel data encapsulated transfer syntax
	 * @param	lossy					true if lossy compression
	 */
	public TransferSyntax(String transferSyntaxUID, String description,boolean explicitVR,boolean bigEndian,boolean encapsulatedPixelData,boolean lossy) {
		this.transferSyntaxUID=transferSyntaxUID;
		this.description=description;
		this.explicitVR=explicitVR;
		this.bigEndian=bigEndian;
		this.encapsulatedPixelData=encapsulatedPixelData;
		this.lossy=lossy;
		this.fileNameExtension=null;
	}

	/**
	 * <p>Construct a Transfer Syntax using the specified UID and characteristics.</p>
	 *
	 * @param	transferSyntaxUID		the UID to use to refer to this transfer syntax
	 * @param	description				the description of this transfer syntax
	 * @param	explicitVR				true if an explicit VR transfer syntax
	 * @param	bigEndian				true if big-endian transfer syntax
	 * @param	encapsulatedPixelData	true if a pixel data encapsulated transfer syntax
	 * @param	lossy					true if lossy compression
	 * @param	fileNameExtension		fileNameExtension to use if bit stream is to be saved as a file
	 */
	public TransferSyntax(String transferSyntaxUID, String description,boolean explicitVR,boolean bigEndian,boolean encapsulatedPixelData,boolean lossy,String fileNameExtension) {
		this(transferSyntaxUID,description,explicitVR,bigEndian,encapsulatedPixelData,lossy);
		this.fileNameExtension=fileNameExtension;
	}

	/**
	 * <p>Construct a Transfer Syntax using the specified UID and characteristics.</p>
	 *
	 * @param	transferSyntaxUID		the UID to use to refer to this transfer syntax
	 * @param	description				the description of this transfer syntax
	 * @param	explicitVR				true if an explicit VR transfer syntax
	 * @param	bigEndian				true if big-endian transfer syntax
	 * @param	encapsulatedPixelData	true if a pixel data encapsulated transfer syntax
	 * @param	lossy					true if lossy compression
	 * @param	fileNameExtension		fileNameExtension to use if bit stream is to be saved as a file
	 * @param	jpegFamily				true if uses JPEG family marker segments
	 */
	public TransferSyntax(String transferSyntaxUID, String description,boolean explicitVR,boolean bigEndian,boolean encapsulatedPixelData,boolean lossy,String fileNameExtension,boolean jpegFamily) {
		this(transferSyntaxUID,description,explicitVR,bigEndian,encapsulatedPixelData,lossy,fileNameExtension);
		this.jpegFamily=jpegFamily;
	}

	/**
	 * <p>Get the UID of the Transfer Syntax.</p>
	 *
	 * @return		the UID of the Transfer Syntax 
	 */
	public String getUID()				{ return transferSyntaxUID; }

	/**
	 * <p>Get a human-readable description of the Transfer Syntax.</p>
	 *
	 * @return		the description of the Transfer Syntax 
	 */
	public String getDescription()			{ return description; }
	
	/**
	 * <p>Is the Transfer Syntax recognized ?</p>
	 *
	 * @return		true if recognized
	 */
	public boolean isRecognized()			{ return recognized; }
	
	/**
	 * <p>Is the Transfer Syntax big endian ?</p>
	 *
	 * @return		true if big endian
	 */
	public boolean isBigEndian()			{ return bigEndian; }

	/**
	 * <p>Is the Transfer Syntax little endian ?</p>
	 *
	 * @return		true if little endian
	 */
	public boolean isLittleEndian()			{ return !bigEndian; }

	/**
	 * <p>Is the Transfer Syntax explicit VR ?</p>
	 *
	 * @return		true if explicit VR
	 */
	public boolean isExplicitVR()			{ return explicitVR; }

	/**
	 * <p>Is the Transfer Syntax implicit VR ?</p>
	 *
	 * @return		true if implicit VR
	 */
	public boolean isImplicitVR()			{ return !explicitVR; }

	/**
	 * <p>Does the Transfer Syntax encapsulate the pixel data ?</p>
	 *
	 * @return		true if encapsulate
	 */
	public boolean isEncapsulated()			{ return encapsulatedPixelData; }

	/**
	 * <p>Does the Transfer Syntax encode the pixel data without encapsulation?</p>
	 *
	 * @return		true if not encapsulated
	 */
	public boolean isNotEncapsulated()		{ return !encapsulatedPixelData; }

	/**
	 * <p>Is the Transfer Syntax potentially lossy ?</p>
	 *
	 * @return		true if lossy
	 */
	public boolean isLossy()			{ return lossy; }

	/**
	 * <p>Get the file name extension appropriate for the Transfer Syntax.</p>
	 *
	 * <p>Will be "unk" if unrecognized.</p>
	 *
	 * @return		the fileNameExtension to use if bit stream is to be saved as a file 
	 */
	public String getFileNameExtension()	{ return fileNameExtension; }

	/**
	 * <p>Is the Transfer Syntax part of the JPEG family of Transfer Syntaxes?</p>
	 *
	 * <p>I.e., those that share the same marker segments, such as the EOI marker used for end of frame in fragment detection.</p>
	 *
	 * @return		true if is JPEG family
	 */
	public boolean isJPEGFamily()		{ return jpegFamily; }

	/**
	 * <p>Does the Transfer Syntax use deflate compression ?</p>
	 *
	 * @return		true if deflated
	 */
	public boolean isDeflated()			{ return transferSyntaxUID.equals(DeflatedExplicitVRLittleEndian); }

	/**
	 * <p>Does the Transfer Syntax use bzip2 compression ?</p>
	 *
	 * @return		true if bzip2
	 */
	public boolean isBzip2ed()			{ return transferSyntaxUID.equals(PixelMedBzip2ExplicitVRLittleEndian); }

	/**
	 * <p>Get the Transfer Syntax as a string.</p>
	 *
	 * @return		the UID of the Transfer Syntax 
	 */
	public String toString() 			{ return transferSyntaxUID; }
	
	/**
	 * <p>Describe the characteristics of Transfer Syntax.</p>
	 *
	 * @return		a detailed description of the Transfer Syntax
	 */
	public String dump() {
		StringBuffer buf = new StringBuffer();
		buf.append("Transfer Syntax UID =");
		buf.append(transferSyntaxUID);
		buf.append("\n");
		buf.append("\tdescription =");
		buf.append(description);
		buf.append("\n");
		buf.append("\tbigEndian =");
		buf.append(bigEndian);
		buf.append("\n");
		buf.append("\texplicitVR =");
		buf.append(explicitVR);
		buf.append("\n");
		buf.append("\tencapsulatedPixelData =");
		buf.append(encapsulatedPixelData);
		buf.append("\n");
		buf.append("\tlossy =");
		buf.append(lossy);
		buf.append("\n");
		buf.append("\trecognized =");
		buf.append(recognized);
		buf.append("\n");
		buf.append("\tfileNameExtension =");
		buf.append(fileNameExtension);
		buf.append("\n");
		buf.append("\tjpegFamily =");
		buf.append(jpegFamily);
		buf.append("\n");
		return buf.toString();
	}
	
	/**
	 * <p>Is the Transfer Syntax with the specified UID explicit VR ?</p>
	 *
	 * @param	uid
	 * @return		true if explicit VR
	 */
	static public boolean isExplicitVR(String uid)		{ return !isImplicitVR(uid); }
	
	/**
	 * <p>Is the Transfer Syntax with the specified UID implicit VR ?</p>
	 *
	 * @param	uid
	 * @return		true if explicit VR
	 */
	static public boolean isImplicitVR(String uid)		{ return uid.equals(ImplicitVRLittleEndian); }

	/**
	 * <p>Is the Transfer Syntax with the specified UID big endian ?</p>
	 *
	 * @param	uid
	 * @return		true if big endian
	 */
	static public boolean isBigEndian(String uid)		{ return uid.equals(ExplicitVRBigEndian); }

	/**
	 * <p>Is the Transfer Syntax with the specified UID little endian ?</p>
	 *
	 * @param	uid
	 * @return		true if little endian
	 */
	static public boolean isLittleEndian(String uid)	{ return !isBigEndian(uid); }

	/**
	 * <p>Does the Transfer Syntax encapsulate the pixel data ?</p>
	 *
	 * @param	uid
	 * @return		true if encapsulate
	 */
	static public boolean isEncapsulated(String uid)			{ return !isNotEncapsulated(uid); }

	/**
	 * <p>Does the Transfer Syntax encode the pixel data without encapsulation?</p>
	 *
	 * @param	uid
	 * @return		true if not encapsulated
	 */
	static public boolean isNotEncapsulated(String uid)		{ return uid.equals(ImplicitVRLittleEndian) || uid.equals(ExplicitVRLittleEndian) || uid.equals(ExplicitVRBigEndian); }
}



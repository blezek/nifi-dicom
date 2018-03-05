/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.SetOfDicomFiles;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.utils.CapabilitiesAvailable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * <p>A factory object of static methods that can create lists of presentation contexts
 * for initiating associations, from lists of DICOM files based on SOP Class (abstract
 * syntax) and supported transfer syntaxes.</p>
 *
 * @author	dclunie
 */
public class PresentationContextListFactory {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/PresentationContextListFactory.java,v 1.22 2017/01/24 10:50:45 dclunie Exp $";
	
	private static final int presentationContextIDStart = 1;
	private static final int presentationContextIDIncrement = 2;
	private static final int presentationContextIDMaximum = 255;
	
	private static final byte incrementPresentationContextID(byte presentationContextID) throws DicomNetworkException {
//System.err.println("PresentationContextListFactory.incrementPresentationContextID(): starting with "+(((int)presentationContextID) & 0xff));
		if ((((int)presentationContextID) & 0xff) >= presentationContextIDMaximum) {
			throw new DicomNetworkException("Too many presentation contexts");
		}
		else {
			return (byte)(presentationContextID+presentationContextIDIncrement);
		}
	}

	/**
	 * <p>Is a bzip2 codec available?</p>
	 *
	 * @return	true if available
	 * @deprecated use  {@link com.pixelmed.utils.CapabilitiesAvailable#haveBzip2Support() CapabilitiesAvailable.haveBzip2Support()} instead
	 */
	public static boolean haveBzip2Support() {
		return CapabilitiesAvailable.haveBzip2Support();
	}

	private static boolean haveBzip2Support = CapabilitiesAvailable.haveBzip2Support();
	
	private static String[][] supportedTransferSyntaxes = {
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian,
			TransferSyntax.JPEGLossless,
			TransferSyntax.JPEGLosslessSV1,
			TransferSyntax.JPEGLS,
			TransferSyntax.JPEG2000Lossless,
			TransferSyntax.RLE
		},
		{
			TransferSyntax.ImplicitVRLittleEndian,
			TransferSyntax.ExplicitVRLittleEndian,
			TransferSyntax.ExplicitVRBigEndian,
			TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian,
			TransferSyntax.DeflatedExplicitVRLittleEndian,
			TransferSyntax.JPEGLossless,
			TransferSyntax.JPEGLosslessSV1,
			TransferSyntax.JPEGLS,
			TransferSyntax.JPEG2000Lossless,
			TransferSyntax.RLE,
			TransferSyntax.JPEGBaseline,
			TransferSyntax.JPEGExtended,
			TransferSyntax.JPEGNLS,
			TransferSyntax.JPEG2000,
			TransferSyntax.MPEG2MPML,
			TransferSyntax.MPEG2MPHL,
			TransferSyntax.MPEG4HP41,
			TransferSyntax.MPEG4HP41BD,
			TransferSyntax.MPEG4HP422D,
			TransferSyntax.MPEG4HP423D,
			TransferSyntax.MPEG4HP42ST
		}
	};
	
	private static Set<String> canConvertFromTransferSyntax = new HashSet<String>();
	static {
		if (haveBzip2Support) {
			canConvertFromTransferSyntax.addAll(Arrays.asList(supportedTransferSyntaxes[2]));
		}
		else {
			canConvertFromTransferSyntax.addAll(Arrays.asList(supportedTransferSyntaxes[1]));
		}
		// could theoretically convert JPEG 2000 if codec support available, but not actually implemented by DicomStreamCopier yet :(
//		if (CapabilitiesAvailable.haveJPEG2000Part1Support()) {
//System.err.println("Do haveJPEG2000Part1Support so adding to canConvertFromTransferSyntax set");
//			canConvertFromTransferSyntax.add(TransferSyntax.JPEG2000);
//			canConvertFromTransferSyntax.add(TransferSyntax.JPEG2000Lossless);
//		}
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @param	theirChoice				propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice				propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded				propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(String abstractSyntax,String transferSyntax,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
	
		if (!haveBzip2Support && compressionLevel == 2 && (transferSyntax == null || !transferSyntax.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian))) {
			compressionLevel=1;	// do not propose the bzip2 transfer syntax if the codec is not available
			// note that if the codec is not available and the transfer syntax is bzip2, may fail later if acceptor does not support bzip2 
		}

		LinkedList presentationContexts = new LinkedList();
		byte nextPresentationContextID = (byte)presentationContextIDStart;	// should always be odd numbered, starting with 0x01
		nextPresentationContextID = addPresentationContextsForAbstractSyntax(presentationContexts,nextPresentationContextID,abstractSyntax,transferSyntax,compressionLevel,
			theirChoice,ourChoice,asEncoded);
		return presentationContexts;
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(String abstractSyntax,String transferSyntax,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(abstractSyntax,transferSyntax,compressionLevel,true,true,true);
	}
	
	/**
	 * <p>Create lists of presentation contexts for initiating associations, from a set of SOP Class UIDs.</p>
	 *
	 * <p>Will propose uncompressed, and a restricted set of compressed, Transfer Syntaxes.</p>
	 *
	 * <p>Useful for Non-Storage SOP Classes, and C-GET of Storage SOP Classes (for which the Transfer Syntax actually encoded on the SCP is unknown),
	 * but should not be used for Storage SOP Classes as an SCU, because establishing presentation contexts based on the set of SOP Classes without
	 * knowledge of encoded Transfer Syntax may lead to failure during C-STORE because of inability to convert
	 * (use {@link #createNewPresentationContextList(SetOfDicomFiles,int,boolean,boolean,boolean) createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel,boolean theirChoice,boolean ourChoice,boolean asEncoded)}) for those instead).</p>
	 *
	 * @param	setOfSOPClassUIDs		the set of <code>String</code> SOP Class UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @param	theirChoice				propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice				propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(Set<String> setOfSOPClassUIDs,int compressionLevel,
			boolean theirChoice,boolean ourChoice) throws DicomNetworkException {
//System.err.println("PresentationContextListFactory.createNewPresentationContextList():");
		if (!haveBzip2Support && compressionLevel == 2) {
			compressionLevel=1;	// do not propose the bzip2 transfer syntax if the codec is not available
		}

		LinkedList presentationContexts = new LinkedList();
		byte nextPresentationContextID = (byte)presentationContextIDStart;	// should always be odd numbered, starting with 0x01
		
//System.err.println("PresentationContextListFactory.createNewPresentationContextList(): setOfSOPClassUIDs.size() = "+setOfSOPClassUIDs.size());
		Iterator si = setOfSOPClassUIDs.iterator();
		while (si.hasNext()) {
			String sopClassUID = (String)(si.next());
			nextPresentationContextID = addPresentationContextsForAbstractSyntax(presentationContexts,nextPresentationContextID,sopClassUID,null,compressionLevel,
				theirChoice,ourChoice,false/*asEncoded*/);
		}

		return presentationContexts;
	}
	
	/**
	 * <p>Create lists of presentation contexts for initiating associations, from a set of SOP Class UIDs.</p>
	 *
	 * <p>Will propose uncompressed, and a restricted set of compressed, Transfer Syntaxes.</p>
	 *
	 * <p>Useful for Non-Storage SOP Classes, and C-GET of Storage SOP Classes (for which the Transfer Syntax actually encoded on the SCP is unknown),
	 * but should not be used for Storage SOP Classes as an SCU, because establishing presentation contexts based on the set of SOP Classes without
	 * knowledge of encoded Transfer Syntax may lead to failure during C-STORE because of inability to convert
	 * (use {@link #createNewPresentationContextList(SetOfDicomFiles,int) createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel)}) for those instead).</p>
	 *
	 * @param	setOfSOPClassUIDs		the set of <code>String</code> SOP Class UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(Set<String> setOfSOPClassUIDs,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(setOfSOPClassUIDs,compressionLevel,true,true);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes for all the
	 * files in the specified set of Dicom files.
	 *
	 * @param	dicomFiles				the set of files with their SOP Class UIDs and the Transfer Syntax UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @param	theirChoice				propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice				propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded				propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {

		if (!haveBzip2Support && compressionLevel == 2) {
			compressionLevel=1;	// do not propose the bzip2 transfer syntax if the codec is not available
		}
		
		// build set of unique Abstract/Transfer Syntax pairs ...
		Map<String,Set<String>> abstractTransferSyntaxPairs = new HashMap<String,Set<String>>();
		Iterator<SetOfDicomFiles.DicomFile> i = dicomFiles.iterator();
		while (i.hasNext()) {
			SetOfDicomFiles.DicomFile f = i.next();
			String ts = f.getTransferSyntaxUID();
			String as = f.getSOPClassUID();
			if (ts != null && ts.length() > 0 && as != null && as.length() > 0) {
				Set<String> tsSet = abstractTransferSyntaxPairs.get(as);
				if (tsSet == null) {
					tsSet = new HashSet<String>();
					abstractTransferSyntaxPairs.put(as,tsSet);
				}
				tsSet.add(ts);
			}
			else {
				throw new DicomNetworkException("Cannot get Abstract or Transfer Syntax to build Presentation Context from file "+f.getFileName());
			}
		}
		
		// theoretically, could merge presentation contexts for different transfer syntax for same abstract syntax, but for now handle individually, since not expected to occur often :(

		LinkedList presentationContexts = new LinkedList();
		byte nextPresentationContextID = (byte)presentationContextIDStart;	// should always be odd numbered, starting with 0x01
		for (String abstractSyntax : abstractTransferSyntaxPairs.keySet()) {
			for (String transferSyntax : abstractTransferSyntaxPairs.get(abstractSyntax)) {
				nextPresentationContextID = addPresentationContextsForAbstractSyntax(presentationContexts,nextPresentationContextID,abstractSyntax,transferSyntax,
					compressionLevel,theirChoice,ourChoice,asEncoded);
			}
		}
		
		return presentationContexts;
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes for all the
	 * files in the specified set of Dicom files.
	 *
	 * @param	dicomFiles				the set of files with their SOP Class UIDs and the Transfer Syntax UIDs
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @return							a LinkedList of PresentationContext
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static public LinkedList createNewPresentationContextList(SetOfDicomFiles dicomFiles,int compressionLevel) throws DicomNetworkException {
		return createNewPresentationContextList(dicomFiles,compressionLevel,true,true,true);
	}
	
	/**
	 * Create lists of presentation contexts for initiating associations, from the specified abstract
	 * syntax and transfer syntax as well as all supported transfer syntaxes.
	 *
	 * @param	presentationContexts	the LinkedList of PresentationContext to be extended
	 * @param	presentationContextID	the next available (odd-numbered) Presentation Context ID to add
	 * @param	abstractSyntax			the SOP Class UID of the data set to be transmitted
	 * @param	transferSyntax			the Transfer Syntax UID in which the data set to be transmitted is encoded, or null if unknown
	 * @param	compressionLevel		0=none,1=propose deflate,2=propose deflate and bzip2,3=propose all known lossless,4-all propose all known lossless and lossy
	 * @param	theirChoice				propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice				propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded				propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @return							the LinkedList of PresentationContext extended
	 * @throws	DicomNetworkException	if too many presentation contexts
	 */
	static private byte addPresentationContextsForAbstractSyntax(
			LinkedList presentationContexts,byte presentationContextID,String abstractSyntax,String transferSyntax,int compressionLevel,
			boolean theirChoice,boolean ourChoice,boolean asEncoded) throws DicomNetworkException {
//System.err.println("addPresentationContextsForAbstractSyntax(): presentationContextID = "+presentationContextID);
//System.err.println("addPresentationContextsForAbstractSyntax(): abstractSyntax = "+abstractSyntax);
//System.err.println("addPresentationContextsForAbstractSyntax(): transferSyntax = "+transferSyntax);
//System.err.println("addPresentationContextsForAbstractSyntax(): compressionLevel = "+compressionLevel);
//System.err.println("addPresentationContextsForAbstractSyntax(): theirChoice = "+theirChoice);
//System.err.println("addPresentationContextsForAbstractSyntax(): ourChoice = "+ourChoice);
//System.err.println("addPresentationContextsForAbstractSyntax(): asEncoded = "+asEncoded);

		int numberOfPresentationContextsAtStart = presentationContexts.size();

		// First propose a presentation context with all transfer syntaxes
		// What we get back will indicate the acceptor's preference, in case we want to use their choice ...
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): starting presentationContextID = "+(presentationContextID&0xff));
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): transferSyntax = "+transferSyntax);
		if (theirChoice) {
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): theirChoice");
			LinkedList tslist = new LinkedList();
			if (transferSyntax != null && transferSyntax.length() > 0) {
				tslist.add(transferSyntax);	// always include the actual transfer syntax in which the input file is already encoded
			}
			if (transferSyntax == null || canConvertFromTransferSyntax.contains(transferSyntax)) {
				for (int i=0; i<supportedTransferSyntaxes[compressionLevel].length; ++i) {
					// Don't want to add the same transfer syntax twice in the same presentation context, hence check ...
					if (transferSyntax == null || !transferSyntax.equals(supportedTransferSyntaxes[compressionLevel][i])) {
						tslist.add(supportedTransferSyntaxes[compressionLevel][i]);
					}
				}
			}
			// else cannot convert, so propose nothing other than encoded TransferSyntax
			if (!tslist.isEmpty()) {
				presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,tslist));
				presentationContextID=incrementPresentationContextID(presentationContextID);
			}
			// else do not add it
		}
			
		// Now propose a presentation context for each transfer syntax
		// What we get back will tell us what the acceptor actually supports, in case we want to choose ourselves ...
			
		if (asEncoded && transferSyntax != null && transferSyntax.length() > 0) {
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): asEncoded and not null or empty");
			// always include a presentation context for the actual transfer syntax in which the input file is already encoded
			presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,transferSyntax));
			presentationContextID=incrementPresentationContextID(presentationContextID);
		}
			
		if (ourChoice && (transferSyntax == null || canConvertFromTransferSyntax.contains(transferSyntax))) {
//System.err.println("PresentationContextListFactory.addPresentationContextsForAbstractSyntax(): ourChoice and can convert");
			for (int i=0; i<supportedTransferSyntaxes[compressionLevel].length; ++i) {
				// Don't want to add the same transfer syntax twice in the same presentation context, hence check ...
				if (transferSyntax == null || !transferSyntax.equals(supportedTransferSyntaxes[compressionLevel][i])) {
					presentationContexts.add(new PresentationContext(presentationContextID,abstractSyntax,supportedTransferSyntaxes[compressionLevel][i]));
					presentationContextID=incrementPresentationContextID(presentationContextID);
				}
			}
		}
		
		if (presentationContexts.size() <= numberOfPresentationContextsAtStart) {
			throw new DicomNetworkException("Failed to created any Presentation Contexts for Abstract Syntax "+abstractSyntax);
		}
		
		return presentationContextID;		// return the next available number
	}

}




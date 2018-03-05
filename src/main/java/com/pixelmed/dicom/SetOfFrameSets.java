/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to describe a set of frame sets, each of which shares common characteristics suitable for display or analysis as an entity.</p>
 *
 * @author	dclunie
 */
public class SetOfFrameSets extends HashSet<FrameSet> {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/SetOfFrameSets.java,v 1.15 2017/01/24 10:50:38 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SetOfFrameSets.class);

	/**
	 * <p>Insert a single frame object into the set of existing {@link com.pixelmed.dicom.FrameSet FrameSet}s,
	 * creating new {@link com.pixelmed.dicom.FrameSet FrameSet}s as necessary.</p>
	 *
	 * <p>Multi-frame, especially enhanced multi-frame, objects are not yet supported,
	 * since one purpose of this is to use {@link com.pixelmed.dicom.FrameSet FrameSet}s to create or simulate them. In
	 * future, support of creation of {@link com.pixelmed.dicom.FrameSet FrameSet}s from functional groups, and from frame vectors
	 * (as in NM images esp. RECON TOMO) may be added.</p>
	 *
	 * @param		list			a list of DICOM attributes for an object
	 * @throws	DicomException	if no SOP Instance UID
	 */
	public void insertIntoFrameSets(AttributeList list) throws DicomException {
		boolean found = false;
		for (FrameSet tryFrameSet : this) {
			if (tryFrameSet.eligible(list)) {
				tryFrameSet.insert(list);
				found=true;
				break;				// insert it in the first frame set that matches
			}
		}
		if (!found) {
			add(new FrameSet(list));
		}
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		int j = 0;
		for (FrameSet f : this) {
			strbuf.append("Frame set [");
			strbuf.append(Integer.toString(j));
			strbuf.append("]:\n");
			strbuf.append(f.toString());
			strbuf.append("\n");
			++j;
		}
		return strbuf.toString();
	}
	
	/**
	 * <p>Create a new set of {@link com.pixelmed.dicom.FrameSet FrameSet}s, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files	a set of files
	 */
	private void doCommonConstructorStuff(Set<File> files) {
		for (File f : files) {
			try {
				if (DicomFileUtilities.isDicomOrAcrNemaFile(f)) {
//System.err.println("SetOfFrameSets.doCommonConstructorStuff(): Doing "+f);
					AttributeList list = new AttributeList();
					list.read(f,TagFromName.PixelData);
					insertIntoFrameSets(list);
				}
			}
			catch (Exception e) {
				slf4jlogger.error("While reading \"{}\"",f,e);	// do NOT call f.getCanonicalPath(), since may throw Exception !
			}
		}
	}
	
	/**
	 * <p>Create an empty new set of {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 */
	public SetOfFrameSets() {
		super();
	}
	
	/**
	 * <p>Create a new set of {@link com.pixelmed.dicom.FrameSet FrameSet}s, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		files	a set of files
	 */
	public SetOfFrameSets(Set<File> files) {
		super();
		doCommonConstructorStuff(files);
	}
	
	/**
	 * <p>Create a new set of {@link com.pixelmed.dicom.FrameSet FrameSet}s, from a set of DICOM files.</p>
	 *
	 * <p>Non-DICOM files and problems parsing files are ignored, rather than causing failure</p>
	 *
	 * @param		paths	a set of paths of filenames and/or folder names of files containing the images
	 */
	public SetOfFrameSets(String paths[]) {
		super();
		Set<File> files = new HashSet<File>();
		for (String p : paths) {
			Collection<File> more = FileUtilities.listFilesRecursively(new File(p));
			files.addAll(more);
		}
		doCommonConstructorStuff(files);
	}

	/**
	 * <p>For testing, read all DICOM files and partition them into {@link com.pixelmed.dicom.FrameSet FrameSet}s.</p>
	 *
	 * @param	arg	the filenames and/or folder names of files containing the images
	 */
	public static void main(String arg[]) {
		SetOfFrameSets setOfFrameSets = new SetOfFrameSets(arg);
		System.err.println("SetOfFrameSets.main(): Result");	// no need to use SLF4J since command line utility/test
		System.err.println(setOfFrameSets.toString());			// no need to use SLF4J since command line utility/test
	}
}


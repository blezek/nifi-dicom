/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A transformation constructed from a DICOM attribute list that extracts
 * those attributes that define the VOI LUT transformation, specifically the
 * window center and width attributes.</p>
 *
 * <p>Looks first for a per-frame functional group FrameVOILUTSequence
 * then looks in the shared functional groups, otherwise tries to find the
 * Window Center and Width values in the top level of the dataset.</p>
 *
 * <p>Note that multiple transformations (for each frame) may be present and are
 * supported (specifically, Window Center and Width are multi-valued
 * attributes).</p>
 *
 * @author	dclunie
 */
public class VOITransform {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/VOITransform.java,v 1.17 2017/01/24 10:50:39 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(VOITransform.class);
	
	/***/
	private class SingleVOITransform {
		String explanation;
		
		/**
		 * @param	explanation
		 */
		SingleVOITransform(String explanation) {
			this.explanation = explanation;
		}
	}
	
	/***/
	private class VOIWindowTransform extends SingleVOITransform {
		/***/
		double center;
		/***/
		double width;
		
		/**
		 * @param	center
		 * @param	width
		 * @param	explanation
		 */
		VOIWindowTransform(double center,double width,String explanation) {
			super(explanation);
			this.center = center;
			this.width = width;
			this.explanation = explanation;
//System.err.println("VOIWindowTransform: adding "+center+","+width+" "+explanation);
		}
		
		/***/
		public final String toString() {
			return center+","+width+" "+explanation;
		}
	}
	
	/***/
	private class VOILUTTransform extends SingleVOITransform {
		int numberOfEntries;
		int firstValueMapped;
		int bitsPerEntry;
		short[] table;
		String explanation;
		int entryMin;
		int entryMax;
		int topOfEntryRange;
		
		/**
		 * @param	numberOfEntries
		 * @param	firstValueMapped
		 * @param	bitsPerEntry
		 * @param	table
		 * @param	explanation
		 */
		VOILUTTransform(int numberOfEntries,int firstValueMapped,int bitsPerEntry,short[] table,String explanation) {
			super(explanation);
			this.numberOfEntries = numberOfEntries;
			this.firstValueMapped = firstValueMapped;
			this.bitsPerEntry = bitsPerEntry;
			this.table = table;
//System.err.println("VOILUTTransform: adding "+numberOfEntries+","+firstValueMapped+","+bitsPerEntry+" "+explanation);
			if (table != null) {
				entryMin=((1<<bitsPerEntry)-1);
				entryMax=0;
				for (int i=0; i<numberOfEntries; ++i) {
					int value=table[i]&0xffff;
					if (value < entryMin) {
						entryMin=value;
					}
					if (value > entryMax) {
						entryMax=value;
					}
				}
				topOfEntryRange = (1<<bitsPerEntry) - 1;
				int powerOfTwoGreaterThanEntryMax = bitsPerEntry-1;
				while (powerOfTwoGreaterThanEntryMax > 0) {
					int tryTop = (1<<powerOfTwoGreaterThanEntryMax) - 1;
					if (tryTop < entryMax) {
						break;
					}
					topOfEntryRange = tryTop;
					--powerOfTwoGreaterThanEntryMax;
				}
			}
		}
		
		/***/
		public final String toString() {
			return "numberOfEntries="+numberOfEntries+",firstValueMapped="+firstValueMapped+",bitsPerEntry="+bitsPerEntry+",entryMin="+entryMin+",entryMax="+entryMax+" explanation="+explanation;
		}
	}
	
	/***/
	private class SingleVOITransforms extends ArrayList {
		void add(AttributeList list) {
			double centerInterceptCorrection = 0.0;
			double centerSlopeCorrection = 1.0;
			if (Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID).equals(SOPClass.PETImageStorage)) {
				centerInterceptCorrection = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleIntercept,0.0);		// should always be zero per IOD
				centerSlopeCorrection = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleSlope,1.0);
//System.err.println("VOITransform: PET centerInterceptCorrection = "+centerInterceptCorrection);
//System.err.println("VOITransform: PET centerSlopeCorrection = "+centerSlopeCorrection);
			}
		
			double      centers[] = Attribute.getDoubleValues(list,TagFromName.WindowCenter);
			double       widths[] = Attribute.getDoubleValues(list,TagFromName.WindowWidth);
			String explanations[] = Attribute.getStringValues(list,TagFromName.WindowCenterWidthExplanation);
			
			if (centers != null && widths != null) {
				int n = centers.length;
				if (widths.length < n) n = widths.length;		// should probably warn users :(
				for (int i=0; i<n; ++i) {
					add(new VOIWindowTransform(
							(centers[i]*centerSlopeCorrection)+centerInterceptCorrection,
							widths[i]*centerSlopeCorrection,
						explanations != null && i<explanations.length ? explanations[i] : ""));
				}
			}
			
			final SequenceAttribute aVOILUTSequence = (SequenceAttribute)list.get(TagFromName.VOILUTSequence);
			if (aVOILUTSequence != null) {
//System.err.println("VOITransform: checking VOILUTSequence");

				boolean interpretFirstValueMappedAsSigned = false;
				if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0) == 1) {	
					interpretFirstValueMappedAsSigned = true;
				}
				else if (Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleSlope,1.0) < 0) {
					interpretFirstValueMappedAsSigned = true;
				}
				else if (Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleIntercept,0.0) < 0) {
					interpretFirstValueMappedAsSigned = true;
				}
//System.err.println("VOITransform: interpretFirstValueMappedAsSigned = "+interpretFirstValueMappedAsSigned);

				final Iterator items = aVOILUTSequence.iterator();
				while (items.hasNext()) {
					final SequenceItem item = (SequenceItem)items.next();
					final AttributeList ilist = item.getAttributeList();
					if (ilist != null) {
//System.err.println("VOITransform: checking VOILUTSequence Item");
						final Attribute aLUTDescriptor = ilist.get(TagFromName.LUTDescriptor);
						if (aLUTDescriptor != null) {
							if (aLUTDescriptor != null && aLUTDescriptor.getVM() == 3) {
//System.err.println("VOITransform: checking VOILUTSequence LUTDescriptor");
								try {
									int numberOfEntries = aLUTDescriptor.getIntegerValues()[0];
									if (numberOfEntries == 0) numberOfEntries=65536;
									int firstValueMapped = aLUTDescriptor.getIntegerValues()[1];
									if (interpretFirstValueMappedAsSigned && !ValueRepresentation.isSignedShortVR(aLUTDescriptor.getVR())) {
										if ((firstValueMapped & 0x8000) != 0) {
											slf4jlogger.debug("firstValueMapped before interpretFirstValueMappedAsSigned = {}",firstValueMapped);
											firstValueMapped =  firstValueMapped | 0xffff0000;
											slf4jlogger.debug("firstValueMapped before interpretFirstValueMappedAsSigned = {}",firstValueMapped);
										}
									}
									final int bitsPerEntry = aLUTDescriptor.getIntegerValues()[2];
									final Attribute aLUTData = ilist.get(TagFromName.LUTData);
									if (aLUTData != null) {
//System.err.println("VOITransform: checking VOILUTSequence LUTData");
										final short[] table = aLUTData.getShortValues();
										final String explanation = Attribute.getDelimitedStringValuesOrEmptyString(ilist,TagFromName.LUTExplanation);
										add(new VOILUTTransform(numberOfEntries,firstValueMapped,bitsPerEntry,table,explanation));
									}
								}
								catch (DicomException e) {
									slf4jlogger.error("",e);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/***/
	private SingleVOITransforms[] arrayOfTransforms;	// null if not varying per-frame, if not null will have size == number of frames
	/***/
	private SingleVOITransforms commonTransforms;		// in which case this will be used
	
	/**
	 * @param	list
	 */
	public VOITransform(AttributeList list) {
//System.err.println("VOITransform:");
		arrayOfTransforms=null;
		commonTransforms=null;
		/*try*/ {
			SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
			if (aPerFrameFunctionalGroupsSequence != null) {
//System.err.println("VOITransform: checking PerFrameFunctionalGroupsSequence");
				int nFrames = aPerFrameFunctionalGroupsSequence.getNumberOfItems();
				int frameNumber = 0;
				Iterator pfitems = aPerFrameFunctionalGroupsSequence.iterator();
				while (pfitems.hasNext()) {
					SequenceItem fitem = (SequenceItem)pfitems.next();
					AttributeList flist = fitem.getAttributeList();
					if (flist != null) {
						SequenceAttribute aFrameVOILUTSequence = (SequenceAttribute)flist.get(TagFromName.FrameVOILUTSequence);
						if (aFrameVOILUTSequence != null && aFrameVOILUTSequence.getNumberOfItems() >= 1) {
//System.err.println("VOITransform: found FrameVOILUTSequence");
							if (arrayOfTransforms == null) arrayOfTransforms = new SingleVOITransforms[nFrames];
							if (arrayOfTransforms[frameNumber] == null) arrayOfTransforms[frameNumber] = new SingleVOITransforms();
							Iterator fvlitems = aFrameVOILUTSequence.iterator();
							while (fvlitems.hasNext()) {
								SequenceItem fvlitem = (SequenceItem)fvlitems.next();
								AttributeList fvllist = fvlitem.getAttributeList();
								//arrayOfTransforms[frameNumber].add(new SingleVOITransform(fvllist));
								arrayOfTransforms[frameNumber].add(fvllist);
							}
						}
					}
					++frameNumber;
				}
			}
			
			if (arrayOfTransforms == null) {
//System.err.println("VOITransform: checking SharedFunctionalGroupsSequence");
				SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
				if (aSharedFunctionalGroupsSequence != null) {
					// assert aSharedFunctionalGroupsSequence.getNumberOfItems() == 1
					Iterator sitems = aSharedFunctionalGroupsSequence.iterator();
					if (sitems.hasNext()) {
						SequenceItem sitem = (SequenceItem)sitems.next();
						AttributeList slist = sitem.getAttributeList();
						if (slist != null) {
							SequenceAttribute aFrameVOILUTSequence = (SequenceAttribute)slist.get(TagFromName.FrameVOILUTSequence);
							if (aFrameVOILUTSequence != null && aFrameVOILUTSequence.getNumberOfItems() >= 1) {
//System.err.println("VOITransform: found FrameVOILUTSequence");
								commonTransforms = new SingleVOITransforms();
								Iterator fvlitems = aFrameVOILUTSequence.iterator();
								while (fvlitems.hasNext()) {
									SequenceItem fvlitem = (SequenceItem)fvlitems.next();
									AttributeList fvllist = fvlitem.getAttributeList();
									//commonTransforms.add(new SingleVOITransform(fvllist));
									commonTransforms.add(fvllist);
								}
							}
						}
					}
				}

				// check for "old-fashioned" VOI LUT style attributes
			
				if ((list.get(TagFromName.WindowCenter) != null && list.get(TagFromName.WindowWidth) != null) || list.get(TagFromName.VOILUTSequence) != null) {
					if (commonTransforms == null) commonTransforms = new SingleVOITransforms();
					commonTransforms.add(list);
				}
			}
			
		}
		//catch (DicomException e) {
		//	slf4jlogger.error("",e);
		//}
//System.err.println("VOITransform: is "+toString());
	}
	
	/**
	 * Get the transforms available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return				the transforms available for the frame, null if none
	 */
	protected SingleVOITransforms getTransformsForFrame(int frame) {
		final SingleVOITransforms useTransform = (arrayOfTransforms == null || frame >= arrayOfTransforms.length) ? commonTransforms : arrayOfTransforms[frame];
//System.err.println("VOITransform.getNumberOfTransforms(): from frame "+frame+" has useTransform="+useTransform);
		return useTransform;
	}
	
	/**
	 * Get the number of transforms available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return				the number of transforms available for the frame, 0 if none
	 */
	public int getNumberOfTransforms(int frame) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		return useTransform == null ? 0 : useTransform.size();
	}

	/**
	 * Is the particular transform for a particular frame a window transformation.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return				true if is a window transform
	 */
	public boolean isWindowTransform(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
		return singleTransform != null && singleTransform instanceof VOIWindowTransform;
	}

	/**
	 * Is the particular transform for a particular frame a LUT transformation.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @return				true if is a LUT transform
	 */
	public boolean isLUTTransform(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
		return singleTransform != null && singleTransform instanceof VOILUTTransform;
	}

	/**
	 * Get the window width of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the window width, or 0 if none
	 */
	public double getWidth(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getWidth(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOIWindowTransform ? ((VOIWindowTransform)singleTransform).width : 0);
	}
	
	/**
	 * Get the window center of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the window center, or 0 if none
	 */
	public double getCenter(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getCenter(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOIWindowTransform ? ((VOIWindowTransform)singleTransform).center : 0);
	}

	/**
	 * Get the number of LUT entries of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the number of LUT entries, or 0 if none
	 */
	public int getNumberOfEntries(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getNumberOfEntries(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).numberOfEntries : 0);
	}

	/**
	 * Get the first value mapped of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the first value mapped, or 0 if none
	 */
	public int getFirstValueMapped(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getFirstValueMapped(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).firstValueMapped : 0);
	}

	/**
	 * Get the number of bits per LUT entry of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the number of bits per LUT entry, or 0 if none
	 */
	public int getBitsPerEntry(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getBitsPerEntry(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).bitsPerEntry : 0);
	}

	/**
	 * Get the minimum LUT entry value of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the minimum LUT entry value, or 0 if none
	 */
	public int getEntryMinimum(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getEntryMinimum(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).entryMin : 0);
	}

	/**
	 * Get the maximum LUT entry value of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the maximum LUT entry value, or 0 if none
	 */
	public int getEntryMaximum(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getEntryMaximum(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).entryMax : 0);
	}

	/**
	 * Get the top of the LUT entry range of values of the particular transform available for a particular frame.
	 *
	 * This is the lowest power of two minus one that is greater than or equally to the maximum LUT entry value, and less than or equal to the maximum specified by bits per entry.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the top of the LUT entry range, or 0 if none
	 */
	public int getTopOfEntryRange(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getTopOfEntryRange(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? 0 : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).topOfEntryRange : 0);
	}

	/**
	 * Get the LUT data of the particular transform available for a particular frame.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the LUT data, or null if none
	 */
	public short[] getLUTData(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getEntryMaximum(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? null : (singleTransform instanceof VOILUTTransform ? ((VOILUTTransform)singleTransform).table : null);
	}
	
	/**
	 * <p>Get the explanation of a particular transform available for a particular frame.</p>
	 *
	 * <p>The explanation is derived from WindowCenterWidthExplanation.</p>
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	transform	numbered from zero; needed to select which transform if more than one for that frame
	 * @return				the explanation, or zero length string if none
	 */
	public String getExplanation(int frame,int transform) {
		final SingleVOITransforms useTransform = getTransformsForFrame(frame);
		final SingleVOITransform singleTransform = (SingleVOITransform)(useTransform.get(transform));
//System.err.println("VOITransform.getCenter(): from frame "+frame+" has singleTransform="+singleTransform);
		return singleTransform == null ? "" : singleTransform.explanation;
	}
	
	/***/
	public final String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("Common = ");
		strbuf.append(commonTransforms == null ? "None" : commonTransforms.toString());
		strbuf.append("\n");
		if (arrayOfTransforms == null || arrayOfTransforms.length == 0) {
			strbuf.append("Frames: None\n");
		}
		else {
			for (int i=0; i<arrayOfTransforms.length; ++i) {
				strbuf.append("[");
				strbuf.append(Integer.toString(i));
				strbuf.append("] = ");
				strbuf.append(arrayOfTransforms[i] == null ? "None" : arrayOfTransforms[i].toString());
				strbuf.append("\n");
			}
		}
		return strbuf.toString();
	}
}


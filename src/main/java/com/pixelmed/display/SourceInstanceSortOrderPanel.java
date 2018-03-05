/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.*;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext; 
import com.pixelmed.event.SelfRegisteringListener; 
import com.pixelmed.display.event.FrameSelectionChangeEvent; 
import com.pixelmed.display.event.FrameSortOrderChangeEvent; 

import java.awt.*; 
import java.awt.event.*; 
import java.util.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * @author	dclunie
 */
abstract class SourceInstanceSortOrderPanel extends JPanel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceInstanceSortOrderPanel.java,v 1.17 2017/01/24 10:50:41 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(SourceInstanceSortOrderPanel.class);

	/***/
	private EventContext typeOfPanelEventContext;

	// keep track of characteristics of currently selected instance we need to do the sort
	
	/***/
	protected int nSrcInstances;
	/***/
	protected int currentSrcInstanceIndex;			// This has NOT been mapped through currentSrcInstanceSortOrder
	/***/
	protected int[] currentSrcInstanceSortOrder;
	/***/
	protected AttributeList currentSrcInstanceAttributeList;

	// UI element that need to be available to in inherited classes
	
	/***/
	protected ButtonGroup sortOrderButtons;
	/***/
	protected JRadioButton byFrameOrderButton;
	/***/
	protected JRadioButton byDimensionOrderButton;
	
	/***/
	protected JPanel dimensionIndexPanel;
	
	/***/
	protected JPanel cineSliderControlsPanel;
	/***/
	protected JSlider cineSlider;
	/***/
	protected int currentSliderMinimum;
	/***/
	protected int currentSliderMaximum;

	/***/
	protected ChangeListener cineSliderChangeListener;

	// our own methods ...
	
	/***/
	protected class MapOfIndexValuesToFrameNumberEntry implements Comparable {
	
		/***/
		int values[];
		/***/
		int frameNumber;
	
		// Methods to implement Comparable

		/**
		 * @param	o
		 */
		public int compareTo(Object o) {
			for (int i=0; i<values.length; ++i) {
				int cmp = values[i] - ((MapOfIndexValuesToFrameNumberEntry)o).values[i];
				if (cmp != 0) return cmp;
			}
			return 0;
		}

		/**
		 * @param	o
		 */
		public boolean equals(Object o) {
			return compareTo(o) == 0;
		}
		
		// out own methods

		/**
		 * @param	values
		 * @param	frameNumber
		 */
		MapOfIndexValuesToFrameNumberEntry(int[] values,int frameNumber) {
			this.values=values;
			this.frameNumber=frameNumber;
		}
		
		/***/
		final int getFrameNumber() { return frameNumber; }
		
		/***/
		public String toString() {
			StringBuffer str = new StringBuffer();
			str.append("{");
			str.append(Integer.toString(frameNumber));
			str.append("}");
			for (int i=0; i< values.length; ++i) {
				str.append(",");
				str.append(Integer.toString(values[i]));
			}
			return str.toString();
		}
	}
	
	/***/
	protected class MapOfIndexValuesToFrameNumber {
		/***/
		int nFrames;
		/***/
		MapOfIndexValuesToFrameNumberEntry[] map;

		/**
		 * @param	list
		 */
		MapOfIndexValuesToFrameNumber(AttributeList list) {
			nFrames = 0;
			map = null;
			try {
				SequenceAttribute aPerFrameFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.PerFrameFunctionalGroupsSequence);
				if (aPerFrameFunctionalGroupsSequence != null) {
					nFrames = aPerFrameFunctionalGroupsSequence.getNumberOfItems();
					int frameNumber = 0;
					Iterator pfitems = aPerFrameFunctionalGroupsSequence.iterator();
					while (pfitems.hasNext()) {
						int[] vDimensionIndexValues = null;
						SequenceItem fitem = (SequenceItem)pfitems.next();
						AttributeList flist = fitem.getAttributeList();
						if (flist != null) {
							SequenceAttribute aFrameContentSequence = (SequenceAttribute)flist.get(TagFromName.FrameContentSequence);
							if (aFrameContentSequence != null && aFrameContentSequence.getNumberOfItems() >= 1) { 		// should be 1
								SequenceItem fgmitem = (SequenceItem)aFrameContentSequence.getItem(0);
								AttributeList fgmlist = fgmitem.getAttributeList();
								if (fgmlist != null) {
									Attribute aDimensionIndexValues = fgmlist.get(TagFromName.DimensionIndexValues);
									if (aDimensionIndexValues != null) {
										vDimensionIndexValues = aDimensionIndexValues.getIntegerValues();
									}
								}
							}
						}
						if (vDimensionIndexValues != null && vDimensionIndexValues.length > 0) {
							if (map == null) map = new MapOfIndexValuesToFrameNumberEntry[nFrames];
							map[frameNumber] = new MapOfIndexValuesToFrameNumberEntry(vDimensionIndexValues,frameNumber);
						}
						++frameNumber;
					}
				}
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
				map=null;
			}
//dump("before sort");
			if (map != null) Arrays.sort(map);
//dump("after sort");
		}
		
		/***/
		int[] getFrameNumberArray() {
			int[] array = null;
			if (map != null) {
				array = new int[nFrames];
				for (int i=0; i<nFrames; ++i) {
					array[i] = map[i].getFrameNumber();
//System.err.println("sortOrder["+i+"]"+array[i]);
				}
			}
			return array;
		}
		
		/**
		 * @param	msg
		 */
		void dump(String msg) {
			if (map != null) {
				for (int i=0; i<nFrames; ++i) {
					System.err.println(msg+" ["+i+"]"+map[i]);
				}
			}
		}
	}
	
	/**
	 * @param	list
	 */
	protected Vector buildListOfDimensionsFromAttributeList(AttributeList list) {
		Vector listOfDimensionIndexNames = new Vector();
		try {
			SequenceAttribute aDimensionIndexSequence = (SequenceAttribute)list.get(TagFromName.DimensionIndexSequence);
			if (aDimensionIndexSequence != null) {
				Iterator items = aDimensionIndexSequence.iterator();
				while (items.hasNext()) {
					SequenceItem item = (SequenceItem)items.next();
					AttributeList ilist = item.getAttributeList();
					if (ilist != null) {
						// per CP 683 correct location of attribute in DimensionIndexSequence, not DimensionOrganizationSequence
						String name = Attribute.getSingleStringValueOrNull(ilist,TagFromName.DimensionDescriptionLabel);
						if (name == null || name.length() == 0) {
							// try using tag name from dictionary if known
							AttributeTagAttribute aDimensionIndexPointer = (AttributeTagAttribute)ilist.get(TagFromName.DimensionIndexPointer);
							if (aDimensionIndexPointer != null && aDimensionIndexPointer.getVM() > 0) {
								AttributeTag tag = aDimensionIndexPointer.getAttributeTagValues()[0];
								if (tag == null) {
									name="NOT SPECIFIED";
								}
								else {
									name = ilist.getDictionary().getNameFromTag(tag);
									if (name == null || name.length() == 0) {
										name = tag.toString();                  // use hex group element pair, e.g. if private element
									}
								}
							}
						}
						assert name != null;
						listOfDimensionIndexNames.add(name);
					}
				}
			}

		}
		catch (DicomException e) {
			slf4jlogger.error("",e);
		}
//System.err.println("SourceInstanceSortOrderPanel.buildListOfDimensionsFromAttributeList(): listOfDimensionIndexNames="+listOfDimensionIndexNames);
		return listOfDimensionIndexNames;
	}
	
	/**
	 * @param	listOfDimensionIndexNames
	 */
	protected void replaceListOfDimensions(Vector listOfDimensionIndexNames) {
		dimensionIndexPanel.removeAll();
		if (listOfDimensionIndexNames != null && listOfDimensionIndexNames.size() > 0) {
			dimensionIndexPanel.add(new JLabel("Dimensions in image:"));
			dimensionIndexPanel.add(new JList(listOfDimensionIndexNames));
		}
	}
	
	/**
	 * @param	min
	 * @param	max
	 * @param	value
	 */
	protected void updateCineSlider(int min,int max,int value) {
		if (min != currentSliderMinimum || max != currentSliderMaximum) {
			cineSliderControlsPanel.removeAll();
			if (max > min) {
				cineSlider = new JSlider(min,max,value);	// don't leave to default, which is 50 and may be outside range
				cineSlider.setLabelTable(cineSlider.createStandardLabels(max-1,min));	// just label the ends
				cineSlider.setPaintLabels(true);
				cineSliderControlsPanel.add(new JLabel("Frame index:"));
				cineSliderControlsPanel.add(cineSlider);
				cineSlider.addChangeListener(cineSliderChangeListener);
			}
			else {
				cineSlider=null;	// else single frame so no slider
			}
			currentSliderMinimum=min;
			currentSliderMaximum=max;
		}
		else {
			if (cineSlider != null) cineSlider.setValue(value);
		}
	}

	/***/
	protected static final String implicitActionCommand = "IMP";
	/***/
	protected static final String dimensionActionCommand = "DIM";
	
	// implement FrameSelectionChangeListener ...
	
	private OurFrameSelectionChangeListener ourFrameSelectionChangeListener;

	class OurFrameSelectionChangeListener extends SelfRegisteringListener {
	
		public OurFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
//System.err.println("SourceInstanceSortOrderPanel.OurFrameSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
//System.err.println("SourceInstanceSortOrderPanel.OurFrameSelectionChangeListener.changed(): event="+fse);
			currentSrcInstanceIndex=fse.getIndex();
			updateCineSlider(1,nSrcInstances,currentSrcInstanceIndex+1);
		}
	}
	
	// implement FrameSortOrderChangeListener ...
	
	private OurFrameSortOrderChangeListener ourFrameSortOrderChangeListener;

	class OurFrameSortOrderChangeListener extends SelfRegisteringListener {
	
		public OurFrameSortOrderChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSortOrderChangeEvent",eventContext);
//System.err.println("SourceInstanceSortOrderPanel.OurFrameSortOrderChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent)e;
//System.err.println("SourceInstanceSortOrderPanel.OurFrameSortOrderChangeListener.changed(): event="+fso);
			currentSrcInstanceSortOrder=fso.getSortOrder();		// may be null, which is reversion to implicit order
			currentSrcInstanceIndex= fso.getIndex();
			updateCineSlider(1,nSrcInstances,currentSrcInstanceIndex+1);
		}
	}
	
	/***/
	private class CineSliderChangeListener implements ChangeListener {
		/**
		 * @param	e
		 */
		public void stateChanged(ChangeEvent e) {
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSelectionChangeEvent(typeOfPanelEventContext,cineSlider.getValue()-1));
		}
	}

	/***/
	private class SortActionListener implements ActionListener {
		/**
		 * @param	event
		 */
		public void actionPerformed(ActionEvent event) {
//System.err.println("SourceImageSortOrderPanel.SortActionListener.actionPerformed()");
			String choice = sortOrderButtons.getSelection().getActionCommand();
			try {
				int[] useSortOrder = null;
				if (choice.equals(dimensionActionCommand) && currentSrcInstanceAttributeList != null) {
					useSortOrder = new MapOfIndexValuesToFrameNumber(currentSrcInstanceAttributeList).getFrameNumberArray();	// null if no dimension index values
				}
				// else either can't sort because no list or was implicit command - either way use null sort order
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new FrameSortOrderChangeEvent(typeOfPanelEventContext,useSortOrder,0));
			} catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}
	}
	
	/**
	 * @param	typeOfPanelEventContext
	 */
	public SourceInstanceSortOrderPanel(EventContext typeOfPanelEventContext) {
		
		this.typeOfPanelEventContext=typeOfPanelEventContext;
		ourFrameSelectionChangeListener = new OurFrameSelectionChangeListener(typeOfPanelEventContext);
		ourFrameSortOrderChangeListener = new OurFrameSortOrderChangeListener(typeOfPanelEventContext);

		nSrcInstances=0;
		currentSrcInstanceSortOrder=null;
		currentSrcInstanceIndex=0;
		currentSrcInstanceAttributeList=null;
		
		currentSliderMinimum=0;
		currentSliderMaximum=0;
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		cineSliderControlsPanel = new JPanel();
		add(cineSliderControlsPanel);
		cineSliderChangeListener = new CineSliderChangeListener();

		JPanel sortControlsPanel = new JPanel();
		add(sortControlsPanel);

		sortControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		sortControlsPanel.add(new JLabel("Sort frames by:"));

		sortOrderButtons = new ButtonGroup();
		SortActionListener listener = new SortActionListener();

		byFrameOrderButton = new JRadioButton("implicit",false);
		byFrameOrderButton.setActionCommand(implicitActionCommand);
		byFrameOrderButton.setToolTipText("Sort frames by implicit order in which frames are stored");
		byFrameOrderButton.addActionListener(listener);
		sortOrderButtons.add(byFrameOrderButton);
		sortControlsPanel.add(byFrameOrderButton);

		byDimensionOrderButton = new JRadioButton("dimension",false);
		byDimensionOrderButton.setActionCommand(dimensionActionCommand);
		byDimensionOrderButton.setToolTipText("Sort frames by dimension order");
		byDimensionOrderButton.addActionListener(listener);
		sortOrderButtons.add(byDimensionOrderButton);
		sortControlsPanel.add(byDimensionOrderButton);
		
		dimensionIndexPanel = new JPanel();
		add(dimensionIndexPanel);
	}
}


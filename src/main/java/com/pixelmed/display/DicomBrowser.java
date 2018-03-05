/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.*; 
import com.pixelmed.dicom.*;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*;
import java.awt.geom.*; 
import java.awt.font.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A primitive DICOMDIR browsing application that will display images and
 * structured reports, including marking up image coordinates and measurements on
 * referenced images.</p>
 *
 * <p>Essentially an example of overriding {@link com.pixelmed.dicom.StructuredReportBrowser#doSomethingWithSelectedSOPInstances(Vector instances) doSomethingWithSelectedSOPInstances()}
 * in {@link com.pixelmed.dicom.StructuredReportBrowser StructuredReportBrowser}, and passing
 * pre-defined graphic shapes and text to {@link com.pixelmed.display.SingleImagePanel SingleImagePanel}.</p>
 *
 * @see com.pixelmed.dicom.StructuredReportBrowser
 * @see com.pixelmed.display.DisplayStructuredReportBrowser
 * @see com.pixelmed.display.SingleImagePanel
 *
 * @author	dclunie
 */
public class DicomBrowser extends ApplicationFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DicomBrowser.java,v 1.36 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DicomBrowser.class);
	
	/**
	 * <p>Construct a browser.</p>
	 *
	 * <p>Will NOT exit the application when the window is closed (X-d out).</p>
	 */
	public DicomBrowser() {
		super(WindowConstants.DISPOSE_ON_CLOSE);
	} 
 
	/**
	 * <p>Construct a browser.</p>
	 *
	 * <p>Will NOT exit the application when the window is closed (X-d out).</p>
	 *
	 * @param	title
	 */
	public DicomBrowser(String title) {
		super(title,null,WindowConstants.DISPOSE_ON_CLOSE);
	} 

	/**
	 * <p>Construct a browser.</p>
	 *
	 * @param	closeOperation	argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public DicomBrowser(int closeOperation) {
		super(closeOperation);
	} 
 
	/**
	 * <p>Construct a browser.</p>
	 *
	 * @param	title
	 * @param	closeOperation	argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public DicomBrowser(String title,int closeOperation) {
		super(title,null,closeOperation);
	} 

	/**
	 * @param	paths
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 */
	public static void loadAndDisplayImagesFromDicomFiles(Vector paths,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted) {
		loadAndDisplayImagesFromSOPInstances(paths,null,mapOfSOPInstanceUIDToReferencedFileName,
			frameWidthWanted,frameHeightWanted);
	}

	/**
	 * @param	instances
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 */
	public static void loadAndDisplayImagesFromSOPInstances(Vector instances,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted) {
		Vector paths = new Vector();
		Iterator i = instances.iterator();
		while (i.hasNext()) {
			//System.err.println((SpatialCoordinateAndImageReference)i.next());
			SpatialCoordinateAndImageReference reference = (SpatialCoordinateAndImageReference)(i.next());
//System.err.println("reference="+reference);
			boolean imageLibraryEntry = reference.getImageLibraryEntry();
//System.err.println("imageLibraryEntry="+imageLibraryEntry);
			String uid = reference.getSOPInstanceUID();
//System.err.println("uid="+uid);
			String path = (String)(mapOfSOPInstanceUIDToReferencedFileName.get(uid));
//System.err.println("path="+path);
			if (path == null || imageLibraryEntry) {
				i.remove();		// want to maintain 1:1 correspondence between instances and paths, and no longer need instances for which there is no path, or which are Image Library entries
			}
			else {
				paths.add(path);
			}
		}
		loadAndDisplayImagesFromSOPInstances(paths,instances,mapOfSOPInstanceUIDToReferencedFileName,
			frameWidthWanted,frameHeightWanted);
	}

	/**
	 * @param	paths
	 * @param	instances
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 */
	public static void loadAndDisplayImagesFromSOPInstances(Vector paths,Vector instances,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted) {
//System.err.println("loadAndDisplayImagesFromSOPInstances");
		SourceImage[] sImgs = new SourceImage[paths.size()];
		SpatialCoordinateAndImageReference[] coords = instances == null ? null : new SpatialCoordinateAndImageReference[paths.size()];
		int widthMax=0;
		int heightMax=0;
		String title=null;
		int imgCount=0;
		Iterator i = paths.iterator();
		Iterator si = instances == null ? null : instances.iterator();
		while (i.hasNext()) {
			String fileName=(String)i.next();
//System.err.println(fileName);
			try {
				DicomInputStream di = null;
				try {
					di=new DicomInputStream(new FileInputStream(fileName));
				}
				catch (FileNotFoundException e) {
					di=new DicomInputStream(new FileInputStream(fileName.toLowerCase(java.util.Locale.US)));
				}

				AttributeList list = new AttributeList();
				list.read(di);
				di.close();
//System.err.println(list);
				if (list.isImage()) {
					if (instances != null) {
//System.err.println("coords["+imgCount+"] was "+coords[imgCount]);
						coords[imgCount] = (SpatialCoordinateAndImageReference) si.next();
//System.err.println("coords["+imgCount+"] now "+coords[imgCount]);
					}
					SourceImage sImg=new SourceImage(list);
					BufferedImage img=sImg.getBufferedImage();
					if (sImg.getWidth() > widthMax) widthMax=sImg.getWidth();
					if (sImg.getHeight() > heightMax) heightMax=sImg.getHeight();
					if (title == null) {
						title=sImg.getTitle();
					}
					sImgs[imgCount++]=sImg;
				}
				else if (list.isSRDocument()) {
					if (title == null) {
						title=list.buildInstanceTitleFromAttributeList();
					}
					StructuredReportBrowser tree = new DisplayStructuredReportBrowser(list,mapOfSOPInstanceUIDToReferencedFileName,
						frameWidthWanted,frameHeightWanted,title);
					tree.setVisible(true);
				}
				else {
					//throw new DicomException("Unsupported SOP instance type");
					System.err.println("Unsupported SOP instance type in file "+fileName+" ("+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality)+", "+Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID)+")");
				}
			} catch (Exception e) {
				slf4jlogger.error("",e);
			}
		}

		if (imgCount > 0) {

			int imagesPerRow = (widthMax > frameWidthWanted) ? 1 : frameWidthWanted/widthMax;
			if (imagesPerRow > imgCount) imagesPerRow=imgCount;
			int imagesPerCol =  (imgCount-1)/imagesPerRow+1;

//System.err.println("widthMax "+widthMax);
//System.err.println("heightMax "+heightMax);
//System.err.println("imgCount "+imgCount);
//System.err.println("imagesPerRow "+imagesPerRow);
//System.err.println("imagesPerCol "+imagesPerCol);

			DicomBrowser af = new DicomBrowser(title == null ? "Untitled" : title);

			JPanel multiPanel = new JPanel();
			multiPanel.setLayout(new GridLayout(imagesPerCol,imagesPerRow));
			multiPanel.setBackground(Color.black);

			SingleImagePanel imagePanel[] = new SingleImagePanel[imagesPerRow*imagesPerCol];

			final int crossSize = 2;				// actually just one arm of the cross
			final int crossGap = 1;					// is included in crossSize
			final int textHorizontalOffset = 10;
			final int textVerticalOffset = -10;

			for (int count=0; count < imgCount; ++count) {
				SourceImage sImg=sImgs[count];
				Vector preDefinedShapes = null;
				Vector preDefinedText = null;
				if (coords != null ) {
//System.err.println("["+count+"] = "+coords[count]);
					SpatialCoordinateAndImageReference reference=coords[count];
					if (reference != null) {
						String graphicType=reference.getGraphicType();
						float[] graphicData=reference.getGraphicData();
						if (graphicType !=null && graphicData != null) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): initializing new vectors of preDefinedShapes and preDefinedText");
							preDefinedShapes = new Vector();
							preDefinedText = new Vector();
							if (graphicType.equals("POINT") && graphicData.length == 2) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding POINT");
								int x = (int)graphicData[0];
								int y = (int)graphicData[1];
								DrawingUtilities.addDiagonalCross(preDefinedShapes,x,y,crossSize,crossGap);
								preDefinedText.add(new TextAnnotation(reference.getAnnotation(),x+textHorizontalOffset,y+textVerticalOffset));
							}
							else if (graphicType.equals("MULTIPOINT") && graphicData.length%2 == 0) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding MULTIPOINT");
								for (int index=0; index < graphicData.length; index+=2) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding MULTIPOINT point at ("+graphicData[index]+","+graphicData[index+1]+")");
									DrawingUtilities.addDiagonalCross(preDefinedShapes,(int)graphicData[index],(int)graphicData[index+1],crossSize,crossGap);
								}
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding MULTIPOINT text: "+reference.getAnnotation());
								preDefinedText.add(new TextAnnotation(reference.getAnnotation(),(int)graphicData[0]+textHorizontalOffset,(int)graphicData[1]+textVerticalOffset));
							}
							else if (graphicType.equals("POLYLINE") && graphicData.length == 4) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding POLYLINE with two points");
								int x1 = (int)graphicData[0];
								int y1 = (int)graphicData[1];
								int x2 = (int)graphicData[2];
								int y2 = (int)graphicData[3];
								preDefinedShapes.add(new Line2D.Float(new Point(x1,y1),new Point(x2,y2)));
								// add cross at either end ...
								DrawingUtilities.addDiagonalCross(preDefinedShapes,x1,y1,crossSize,crossGap);
								DrawingUtilities.addDiagonalCross(preDefinedShapes,x2,y2,crossSize,crossGap);
								// add text annotation as shape
								// where to place annotation ? closest to top left ...
								boolean gravityX = true;
								boolean gravityY = true;
								int useX = gravityX ? (x1 < x2 ? x1 : x2) : (x1 > x2 ? x1 : x2);
								int useY = gravityY ? (y1 < y2 ? y1 : y2) : (y1 > y2 ? y1 : y2);
								preDefinedText.add(new TextAnnotation(reference.getAnnotation(),useX+textHorizontalOffset,useY+textVerticalOffset));
							}
							else if (graphicType.equals("POLYLINE") && graphicData.length > 4) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding POLYLINE with more than two points");
								int x1 = (int)graphicData[0];
								int y1 = (int)graphicData[1];
								for (int p=2; p+1 < graphicData.length; p+=2) {
									int x2 = (int)graphicData[p];
									int y2 = (int)graphicData[p+1];
									preDefinedShapes.add(new Line2D.Float(new Point(x1,y1),new Point(x2,y2)));
									x1=x2;
									y1=y2;
								}
								preDefinedText.add(new TextAnnotation(reference.getAnnotation(),x1+textHorizontalOffset,y1+textVerticalOffset));
							}
							else if (graphicType.equals("ELLIPSE") && graphicData.length == 8) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding ELLIPSE");
								int xmajorstart = (int)graphicData[0];
								int ymajorstart = (int)graphicData[1];
								int xmajorend = (int)graphicData[2];
								int ymajorend = (int)graphicData[3];
								int xminorstart = (int)graphicData[4];
								int yminorstart = (int)graphicData[5];
								int xminorend = (int)graphicData[6];
								int yminorend = (int)graphicData[7];
								if (ymajorstart == ymajorend && xminorstart == xminorend) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): have an ellipse with major axis along x axis without rotation");
									int xtopleft = xmajorstart;
									int ytopleft = yminorstart;
									int width = xmajorend-xmajorstart;
									int height = yminorend-yminorstart;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding ellipse ("+xtopleft+","+ytopleft+","+width+","+height+")");
									if (width < 0) {
										width = - width;
										xtopleft -= width;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): width was negative so making positive and changing top left start");
									}
									if (height < 0) {
										height = - height;
										ytopleft -= height;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): height was negative so making positive and changing top left start");
									}
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding Ellipse2D.Float("+xtopleft+","+ytopleft+","+width+","+height+")");
									preDefinedShapes.add(new Ellipse2D.Float(xtopleft,ytopleft,width,height));
									preDefinedText.add(new TextAnnotation(reference.getAnnotation(),xtopleft+textHorizontalOffset,ytopleft+textVerticalOffset));
								}
								else if (xmajorstart == xmajorend && yminorstart == yminorend) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): have an ellipse with major axis along y axis without rotation");
									int xtopleft = xminorstart;
									int ytopleft = ymajorstart;
									int width = xminorend-xminorstart;
									int height = ymajorend-ymajorstart;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding ellipse ("+xtopleft+","+ytopleft+","+width+","+height+")");
									if (width < 0) {
										width = - width;
										xtopleft -= width;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): width was negative so making positive and changing top left start");
									}
									if (height < 0) {
										height = - height;
										ytopleft -= height;
//System.err.println("loadAndDisplayImagesFromSOPInstances(): height was negative so making positive and changing top left start");
									}
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding Ellipse2D.Float("+xtopleft+","+ytopleft+","+width+","+height+")");
									preDefinedShapes.add(new Ellipse2D.Float(xtopleft,ytopleft,width,height));
									preDefinedText.add(new TextAnnotation(reference.getAnnotation(),xtopleft+textHorizontalOffset,ytopleft+textVerticalOffset));
								}
								else {
slf4jlogger.info("loadAndDisplayImagesFromSOPInstances(): NOT adding ELLIPSE that is not parallel to row");	// because the constructor expects a rectangular area :(
								}
							}
							else if (graphicType.equals("CIRCLE") && graphicData.length == 4) {
//System.err.println("loadAndDisplayImagesFromSOPInstances(): adding CIRCLE");
								int xcenter = (int)graphicData[0];
								int ycenter = (int)graphicData[1];
								int xperimeter = (int)graphicData[2];
								int yperimeter = (int)graphicData[3];
								float xdelta = xcenter - xperimeter;
								float ydelta = ycenter - yperimeter;
								float radius = (float)Math.sqrt(xdelta*xdelta + ydelta*ydelta);
								float xtopleft = xcenter - radius;
								float ytopleft = ycenter - radius;
								float diameter = 2 * radius;
								preDefinedShapes.add(new Ellipse2D.Float(xtopleft,ytopleft,diameter,diameter));
								preDefinedText.add(new TextAnnotation(reference.getAnnotation(),(int)(xtopleft+textHorizontalOffset),(int)(ytopleft+textVerticalOffset)));
							}
							else {
slf4jlogger.info("loadAndDisplayImagesFromSOPInstances(): NOT adding unsupported or unrecognized "+graphicType+" with "+(graphicData.length/2)+" coordinates");
							}
						}
					}
				}
//System.err.println("loadAndDisplayImagesFromSOPInstances(): pre-defined shapes passed to SingleImagePanel constructor");
//com.pixelmed.geometry.LocalizerPosterFactory.dumpShapes(preDefinedShapes);
				SingleImagePanel ip = new SingleImagePanel(sImg,null/*EventContext*/,null/*sortOrder*/,preDefinedShapes,preDefinedText,null/*GeometryOfVolume*/);
				ip.setPreferredSize(new Dimension(sImg.getWidth(),sImg.getHeight()));
				multiPanel.add(ip);
				imagePanel[count]=ip;
			}

			JScrollPane scrollPane = new JScrollPane(multiPanel);

			Container content = af.getContentPane();
			content.setLayout(new GridLayout(1,1));
			content.add(scrollPane);

			af.pack();

			int frameHeight=scrollPane.getHeight()+30;
			if (frameHeight>frameHeightWanted) frameHeight=frameHeightWanted;
			int frameWidth=scrollPane.getWidth()+24;
			if (frameWidth>frameWidthWanted) frameWidth=frameWidthWanted;
			af.setSize(frameWidth,frameHeight);
			af.setVisible(true);
		}
	}

	/**
	 * @param	arg
	 */
	public static void main(String arg[]) {
		try {
			int frameWidthWanted;
			int frameHeightWanted;
			if (arg.length == 2) {
				frameWidthWanted=Integer.valueOf(arg[0]).intValue();
				frameHeightWanted=Integer.valueOf(arg[1]).intValue();
			}
			if (arg.length == 3) {
				frameWidthWanted=Integer.valueOf(arg[1]).intValue();
				frameHeightWanted=Integer.valueOf(arg[2]).intValue();
			}
			else {
				frameWidthWanted = 1024;
				frameHeightWanted = 768;
			}

			String dicomdirFileName = null;
			if (arg.length == 1 || arg.length == 3) {
				dicomdirFileName=arg[0];
			}
			else if (arg.length == 0 || arg.length == 2) {
				SafeFileChooser.SafeFileChooserThread fileChooserThread = new SafeFileChooser.SafeFileChooserThread();
				java.awt.EventQueue.invokeAndWait(fileChooserThread);
				dicomdirFileName=fileChooserThread.getSelectedFileName();
			}
			else {
				System.err.println("Usage: DicomBrowser [dicomdir] [frameWidthWanted frameHeightWanted]");
				System.exit(-1);
			}
//System.err.println("Using: "+dicomdirFileName);	// no need to use SLF4J since command line utility/test
			{
				AttributeList list = new AttributeList();
				final String parentFilePath = new File(dicomdirFileName).getParent();
//System.err.println("parentFilePath: "+parentFilePath);
//System.err.println("reading DICOMDIR");
				list.read(dicomdirFileName);
//System.err.println("building tree");
				ApplicationFrame daf = new ApplicationFrame("DICOMDIR",null,400,800);
				DisplayDicomDirectoryBrowser tree = new DisplayDicomDirectoryBrowser(list,parentFilePath,daf,frameWidthWanted,frameHeightWanted);
//System.err.println("displaying tree");
				daf.setVisible(true);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
	}
}








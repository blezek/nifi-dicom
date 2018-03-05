/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.*;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.awt.geom.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.border.*;

/**
 * <p>Implements a component that can display an array of tiles, each of which
 * is a 2D graph of sample values.</p>
 *
 * @author	dclunie
 */
public class PlotGraph extends JComponent {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/PlotGraph.java,v 1.25 2017/01/24 10:50:40 dclunie Exp $";
	
	/***/
	protected class FloatArrayStatistics {
		/***/
		private int n;
		/***/
		private float minimum;
		/***/
		private float maximum;
		/**
		 * @param	minimum
		 * @param	maximum
		 */
		public FloatArrayStatistics(float minimum,float maximum) {
			this.minimum=minimum;
			this.maximum=maximum;
		}
		/**
		 * @param	values
		 */
		public FloatArrayStatistics(float[] values) {
			minimum=Float.MAX_VALUE;
			maximum=Float.MIN_VALUE;
			n = values.length;
			for (int i=0; i<n; ++i) {
				float value = values[i];
				if (value < minimum) minimum=value;
				if (value > maximum) maximum=value;
			}
		}
		/***/
		public float getMinimum() { return minimum; }
		/***/
		public float getMaximum() { return maximum; }
		/***/
		public float getRange() { return maximum-minimum; }
	}
	
	/***/
	protected float[] samples;
	/***/
	protected FloatArrayStatistics statistics;
	
	/***/
	protected int nTilesPerColumn;
	/***/
	protected int nTilesPerRow;
	/***/
	protected int samplesPerRow;
	/***/
	protected int samplesPerTile;
	
	// these are class scope so that mouseMoved() in sub-classes can figure out which tile we are in ...
	/***/
	protected float widthOfTile;
	/***/
	protected float heightOfTile;

	/**
	 * @param	nTilesPerColumn
	 * @param	nTilesPerRow
	 * @param	minimum
	 * @param	maximum
	 */
	protected PlotGraph(int nTilesPerColumn,int nTilesPerRow,float minimum,float maximum) {
		statistics=new FloatArrayStatistics(minimum,maximum);
		this.nTilesPerColumn=nTilesPerColumn;
		this.nTilesPerRow=nTilesPerRow;
	}

	/**
	 * @param	samples
	 * @param	nTilesPerColumn
	 * @param	nTilesPerRow
	 */
	public PlotGraph(float[] samples,int nTilesPerColumn,int nTilesPerRow) {
		this.samples=samples;
		statistics=new FloatArrayStatistics(samples);
		this.nTilesPerColumn=nTilesPerColumn;
		this.nTilesPerRow=nTilesPerRow;
		samplesPerRow=samples.length/nTilesPerColumn;
		samplesPerTile=samplesPerRow/nTilesPerRow;
	}

	/**
	 * @param	g2d
	 * @param	r
	 * @param	fillBackgroundFirst
	 */
	private void renderPlotToGraphics2D(Graphics2D g2d,Rectangle r,boolean fillBackgroundFirst) {
//System.err.println("PlotGraph.renderPlotToGraphics2D");
		Color backgroundColor = Color.black;
		Color foregroundColor = Color.white;
		float lineWidth = 1;		// making this less than 1 doesn't help thin the bounding lines
		g2d.setBackground(backgroundColor);
		g2d.setColor(backgroundColor);
		if (fillBackgroundFirst) {
			g2d.fill(new Rectangle2D.Float(0,0,r.width,r.height));
		}
		g2d.setColor(foregroundColor);
		g2d.setStroke(new BasicStroke(lineWidth));
		
		// these are class scope so that mouseMoved() in sub-classes can figure out which tile we are in ...
		 widthOfTile = (float)r.width/nTilesPerRow;
		heightOfTile = (float)r.height/nTilesPerColumn;

		float interceptX = 0;
		float rescaleX = (float)((widthOfTile-1)/(samplesPerTile-1));
		
		float interceptY = (float)(-statistics.getMinimum());			// value to add to make minimum zero
		float rescaleY = (float)((heightOfTile-1)/statistics.getRange());	// value to then multiply by to bring into range 0 to r.height

		// first draw boxes around each spectral tile, with anti-aliasing turned on (only way to get consistent thickness)
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

		float drawingOffsetY = 0;
		for (int row=0;row<nTilesPerColumn;++row) {
			float drawingOffsetX = 0;
			for (int col=0;col<nTilesPerRow;++col) {
				// Just drawing each bounding line once doesn't seem to help them sometimes
				// being thicker than others ... is this a stroke width problem (better if anti-aliasing on, but then too slow) ?
				//g2d.draw(new Rectangle2D.Double(drawingOffsetX,drawingOffsetY,drawingOffsetX+widthOfTile-1,drawingOffsetY+heightOfTile-1));
				if (row == 0)
					g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY,drawingOffsetX+widthOfTile,drawingOffsetY));			// top
				if (col == 0)
					g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY,drawingOffsetX,drawingOffsetY+heightOfTile));			// left
				g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY+heightOfTile,drawingOffsetX+widthOfTile,drawingOffsetY+heightOfTile));	// bottom
				g2d.draw(new Line2D.Float(drawingOffsetX+widthOfTile,drawingOffsetY,drawingOffsetX+widthOfTile,drawingOffsetY+heightOfTile));	// right
				drawingOffsetX+=widthOfTile;
			}
			drawingOffsetY+=heightOfTile;
		}

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);	// too slow for spectra
		//g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);	// too slow for spectra
//long startTime = System.currentTimeMillis();
//long accumulatedDrawTime = 0;
//int linesDrawn=0;		
		drawingOffsetY = 0;
		Line2D.Float theLine = new Line2D.Float();
		//GeneralPath thePath = new GeneralPath();
		for (int row=0;row<nTilesPerColumn;++row) {
			int offsetIntoSamplesOfRow = row*samplesPerRow;
			float drawingOffsetX = 0;
			for (int col=0;col<nTilesPerRow;++col) {
				int offsetIntoSamples = offsetIntoSamplesOfRow + col*samplesPerTile;
				int i = offsetIntoSamples + 1;
				float xOffset = interceptX*rescaleX + drawingOffsetX;
				float yOffset = heightOfTile - interceptY*rescaleY + drawingOffsetY;
				//int fromXValue = (int)(xOffset);
				float fromXValue = xOffset;
				//int fromYValue = (int)(yOffset - samples[offsetIntoSamples]*rescaleY);
				float fromYValue = yOffset - samples[offsetIntoSamples]*rescaleY;
				//thePath.moveTo(fromXValue,fromYValue);
				for (int j=1;j<samplesPerTile;++j) {
					//float toXValue = (j + interceptX)*rescaleX + drawingOffsetX;
					//int toXValue = (int)(j*rescaleX + xOffset);
					float toXValue = (int)(j*rescaleX + xOffset);
					//float toYValue = heightOfTile - (samples[i++] + interceptY)*rescaleY + drawingOffsetY;
					//int toYValue = (int)(yOffset - samples[i++]*rescaleY);
					float toYValue = (int)(yOffset - samples[i++]*rescaleY);
					if (fromXValue != toXValue || fromYValue != toYValue) {
						//g2d.draw(new Line2D.Float(fromXValue,fromYValue,toXValue,toYValue));
						theLine.setLine(fromXValue,fromYValue,toXValue,toYValue);	// don't keep allocating new lines, reuse one
//long startDrawTime = System.currentTimeMillis();
						g2d.draw(theLine);
						//thePath.lineTo(toXValue,toYValue);
//accumulatedDrawTime+=(System.currentTimeMillis()-startDrawTime);
//++linesDrawn;
					}
					fromXValue=toXValue;
					fromYValue=toYValue;
				}
				drawingOffsetX+=widthOfTile;
			}
			drawingOffsetY+=heightOfTile;
		}
//long startDrawPathTime = System.currentTimeMillis();
		//g2d.draw(thePath);
//{
//long finishTime = System.currentTimeMillis();
//long elapsedTime = finishTime-startTime;
//long pathDrawingTime = finishTime-startDrawPathTime;
//System.err.println("renderPlotToGraphics2D() elapsed: "+elapsedTime+" ms");
//System.err.println("renderPlotToGraphics2D() draw path: "+pathDrawingTime+" ms ("+pathDrawingTime*100.0/elapsedTime+" %)");
//System.err.println("renderPlotToGraphics2D() drawing lines: "+accumulatedDrawTime+" ms ("+accumulatedDrawTime*100.0/elapsedTime+" %)");
//System.err.println("Lines drawn="+linesDrawn);
//}
	}
	
	/**
	 * @param	r
	 */
	private BufferedImage createAppropriateBufferedImageToDrawInto(Rectangle r) {
                ColorModel colorModel = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getColorModel();
		return new BufferedImage(colorModel,colorModel.createCompatibleWritableRaster(r.width,r.height),colorModel.isAlphaPremultiplied(),null);
		//return new BufferedImage(r.width,r.height,BufferedImage.TYPE_BYTE_BINARY);
	}

	/***/
	protected BufferedImage imageOfRenderedPlot;
	
	/**
	 * <p>Draw the data onto the supplied graphic with the specified background.</p>
	 *
	 * @param	g				the graphic to draw into
	 * @param	backgroundImage			the image to use as a background
	 * @param	boundsOfPlot			what sub-region of the "image" that are the rendered tiles to draw the background image
	 * @param	boundsOfBackgroundImage		what sub-region of the background image to use
	 */
	protected void actuallyPaintComponent(Graphics g,BufferedImage backgroundImage,Rectangle boundsOfPlot,Rectangle boundsOfBackgroundImage) {
//System.err.println("PlotGraph.actuallyPaintComponent");
		Rectangle r = this.getBounds();
		//renderPlotToGraphics2D((Graphics2D)(g),r);	// this direct rendering is pretty fast on the Mac, and noticeably faster than going via an off-screen image, but ...
								// the same thing is glacial on Windows and Linux, cached images are faster on the Mac, once they have been cached of course
		imageOfRenderedPlot = createAppropriateBufferedImageToDrawInto(r);
		if (backgroundImage != null && boundsOfPlot != null && boundsOfBackgroundImage != null) {
			//imageOfRenderedPlot.getGraphics().drawImage(backgroundImage,0,0,this);
			imageOfRenderedPlot.getGraphics().drawImage(backgroundImage,
				boundsOfPlot.x,boundsOfPlot.y,
				boundsOfPlot.x+boundsOfPlot.width,boundsOfPlot.y+boundsOfPlot.height,
				boundsOfBackgroundImage.x,boundsOfBackgroundImage.y,
				boundsOfBackgroundImage.x+boundsOfBackgroundImage.width,boundsOfBackgroundImage.y+boundsOfBackgroundImage.height,
				this);
			renderPlotToGraphics2D((Graphics2D)(imageOfRenderedPlot.getGraphics()),r,false);
		}
		else {
			renderPlotToGraphics2D((Graphics2D)(imageOfRenderedPlot.getGraphics()),r,true);
		}
		g.drawImage(imageOfRenderedPlot,0,0,this);
	}
	
	/**
	 * @param	g
	 */
	public void paintComponent(Graphics g) {
//System.err.println("PlotGraph.paintComponent");
		actuallyPaintComponent(g,null,null,null);
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Display the specified sample values as an array of tiles in a window,
	 * and take a snapshot of it as a JPEG file.</p>
	 *
	 * @param	arg	5 arguments, the data filename (4 bytes per float, two floats per complex pair),
	 *			the number of samples per tile, the number of tiles per column, the number of tiles per row,
	 *			and the number of frames to skip (each samples*rows*columns)
	 */
	public static void main(String arg[]) {
		try {
			BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(arg[0])),true);		// big endian
			int nSamplesPerTile = Integer.parseInt(arg[1]);
			int nTilesPerColumn = Integer.parseInt(arg[2]);
			int nTilesPerRow = Integer.parseInt(arg[3]);
			int skipFrames = Integer.parseInt(arg[4]);
			
			float[] samples =  new float[nSamplesPerTile*nTilesPerRow*nTilesPerColumn];
			i.skipInsistently(skipFrames*samples.length*8);	// 4 bytes per float, two floats per complex pair
			i.readComplexFloat(samples,null,samples.length);
			
			PlotGraph pg = new PlotGraph(samples,nTilesPerColumn,nTilesPerRow);
			pg.setPreferredSize(new Dimension(512,256));
			
			ApplicationFrame app = new ApplicationFrame();
			app.getContentPane().add(pg);
			app.pack();
			app.setVisible(true);
			app.takeSnapShot(app.getBounds());
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


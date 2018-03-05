/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.displaywave;

import com.pixelmed.dicom.BinaryInputStream;
import com.pixelmed.display.ApplicationFrame;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * <p>Implements a component that can display an array of tiles, each of which
 * is a 2D graph of ECG values.</p>
 *
 * @author	dclunie
 */
public class ECGPanel extends JComponent {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/displaywave/ECGPanel.java,v 1.18 2017/01/24 10:50:42 dclunie Exp $";
	
	/***/
	private short[][] samples;
	/***/
	private int numberOfChannels;
	/***/
	private int nSamplesPerChannel;
	/***/
	private int nSamplesPerRow;
	/***/
	private int nTilesPerColumn;
	/***/
	private int nTilesPerRow;

	/***/
	private float samplingIntervalInMilliSeconds;
	/***/
	private float[] amplitudeScalingFactorInMilliVolts;
	/***/
	private String[] channelNames;

	/***/
	private float widthOfPixelInMilliSeconds;
	/***/
	private float heightOfPixelInMilliVolts;

	/***/
	private float timeOffsetInMilliSeconds;

	/***/
	private int displaySequence[];

	/***/
	private int width;
	/***/
	private int height;

	/**
	 * <p>Construct a component containing an array of tiles of ECG waveforms.</p>
	 *
	 * @param	samples					the ECG data as separate channels
	 * @param	numberOfChannels			the number of channels (leads)
	 * @param	nSamplesPerChannel			the number of samples per channel (same for all channels)
	 * @param	channelNames				the names of each channel with which to annotate them
	 * @param	nTilesPerColumn				the number of tiles to display per column
	 * @param	nTilesPerRow				the number of tiles to display per row (if 1, then nTilesPerColumn should == numberOfChannels)
	 * @param	samplingIntervalInMilliSeconds		the sampling interval (duration of each sample) in milliseconds
	 * @param	amplitudeScalingFactorInMilliVolts	how many millivolts per unit of sample data (may be different for each channel)
	 * @param	horizontalPixelsPerMilliSecond		how may pixels to use to represent one millisecond 
	 * @param	verticalPixelsPerMilliVolt		how may pixels to use to represent one millivolt
	 * @param	timeOffsetInMilliSeconds		how much of the sample data to skip, specified in milliseconds from the start of the samples
	 * @param	displaySequence				an array of indexes into samples (etc.) sorted into desired sequential display order
	 * @param	width					the width of the resulting component (sample data is truncated to fit if necessary)
	 * @param	height					the height of the resulting component (sample data is truncated to fit if necessary)
	 */
	public ECGPanel(short[][] samples,int numberOfChannels,int nSamplesPerChannel,String[] channelNames,int nTilesPerColumn,int nTilesPerRow,
			float samplingIntervalInMilliSeconds,float[] amplitudeScalingFactorInMilliVolts,
			float horizontalPixelsPerMilliSecond,float verticalPixelsPerMilliVolt,
			float timeOffsetInMilliSeconds,int[] displaySequence,
			int width,int height) {
		this.samples=samples;
//System.err.println("ECGPanel.ECGPanel(): samples.length="+samples.length);
		this.numberOfChannels=numberOfChannels;
//System.err.println("ECGPanel.ECGPanel(): numberOfChannels="+numberOfChannels);
		this.nSamplesPerChannel=nSamplesPerChannel;
//System.err.println("ECGPanel.ECGPanel(): nSamplesPerChannel="+nSamplesPerChannel);
		this.channelNames=channelNames;
		this.nTilesPerColumn=nTilesPerColumn;
//System.err.println("ECGPanel.ECGPanel(): nTilesPerColumn="+nTilesPerColumn);
		this.nTilesPerRow=nTilesPerRow;
//System.err.println("ECGPanel.ECGPanel(): nTilesPerRow="+nTilesPerRow);
		this.nSamplesPerRow=samples.length/nTilesPerColumn;	// should == nSamplesPerChannel*nTilesPerRow
//System.err.println("ECGPanel.ECGPanel(): nSamplesPerRow="+nSamplesPerRow);
		this.samplingIntervalInMilliSeconds=samplingIntervalInMilliSeconds;
//System.err.println("ECGPanel.ECGPanel(): samplingIntervalInMilliSeconds="+samplingIntervalInMilliSeconds);
		this.amplitudeScalingFactorInMilliVolts=amplitudeScalingFactorInMilliVolts;
//System.err.println("ECGPanel.ECGPanel(): amplitudeScalingFactorInMilliVolts[0]="+amplitudeScalingFactorInMilliVolts[0]);

		this.widthOfPixelInMilliSeconds = 1/horizontalPixelsPerMilliSecond;
//System.err.println("ECGPanel.ECGPanel(): horizontalPixelsPerMilliSecond="+horizontalPixelsPerMilliSecond);
//System.err.println("ECGPanel.ECGPanel(): widthOfPixelInMilliSeconds="+widthOfPixelInMilliSeconds);
		this.heightOfPixelInMilliVolts = 1/verticalPixelsPerMilliVolt;
//System.err.println("ECGPanel.ECGPanel(): verticalPixelsPerMilliVolt="+verticalPixelsPerMilliVolt);
//System.err.println("ECGPanel.ECGPanel(): heightOfPixelInMilliVolts="+heightOfPixelInMilliVolts);

		this.timeOffsetInMilliSeconds=timeOffsetInMilliSeconds;
//System.err.println("ECGPanel.ECGPanel(): timeOffsetInMilliSeconds="+timeOffsetInMilliSeconds);
		this.displaySequence=displaySequence;
		
		this.width=width;
		this.height=height;
	}

	/**
	 * @param	g2d
	 * @param	r
	 * @param	fillBackgroundFirst
	 */
	private void renderPlotToGraphics2D(Graphics2D g2d,Rectangle r,boolean fillBackgroundFirst) {
//System.err.println("ECGPanel.renderPlotToGraphics2D");
		Color backgroundColor = Color.white;
		Color curveColor = Color.blue;
		Color boxColor = Color.black;
		Color gridColor = Color.red;
		Color channelNameColor = Color.black;
		
		float curveWidth = 1.5f;
		float boxWidth = 2;
		float gridWidth = 1;
		float gridThickWidth = 2;
		
		Font channelNameFont = new Font("SansSerif",Font.BOLD,14);
		
		int channelNameXOffset = 10;
		int channelNameYOffset = 20;
		
		g2d.setBackground(backgroundColor);
		g2d.setColor(backgroundColor);
		if (fillBackgroundFirst) {
			g2d.fill(new Rectangle2D.Float(0,0,r.width,r.height));
		}
		
		//float widthOfTileInPixels = (float)r.width/nTilesPerRow;
		//float heightOfTileInPixels = (float)r.height/nTilesPerColumn;
		float widthOfTileInPixels = (float)width/nTilesPerRow;
		float heightOfTileInPixels = (float)height/nTilesPerColumn;
		
		float widthOfTileInMilliSeconds = widthOfPixelInMilliSeconds*widthOfTileInPixels;
		float  heightOfTileInMilliVolts =  heightOfPixelInMilliVolts*heightOfTileInPixels;

		// first draw boxes around each tile, with anti-aliasing turned on (only way to get consistent thickness)
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setColor(gridColor);

		float drawingOffsetY = 0;
		for (int row=0;row<nTilesPerColumn;++row) {
			float drawingOffsetX = 0;
			for (int col=0;col<nTilesPerRow;++col) {
				g2d.setStroke(new BasicStroke(gridWidth));
				for (float time=0; time<widthOfTileInMilliSeconds; time+=200) {
					float x = drawingOffsetX+time/widthOfPixelInMilliSeconds;
					g2d.draw(new Line2D.Float(x,drawingOffsetY,x,drawingOffsetY+heightOfTileInPixels));
				}
				//g2d.setStroke(new BasicStroke(gridThickWidth));
				//for (float time=0; time<widthOfTileInMilliSeconds; time+=1000) {
				//	float x = drawingOffsetX+time/widthOfPixelInMilliSeconds;
				//	g2d.draw(new Line2D.Float(x,drawingOffsetY,x,drawingOffsetY+heightOfTileInPixels));
				//}
				g2d.setStroke(new BasicStroke(gridWidth));
				for (float milliVolts=-heightOfTileInMilliVolts/2; milliVolts<=heightOfTileInMilliVolts/2; milliVolts+=0.5) {
					float y = drawingOffsetY + heightOfTileInPixels/2 + milliVolts/heightOfTileInMilliVolts*heightOfTileInPixels;
//System.err.println("milliVolts="+milliVolts+" y="+y);
					g2d.draw(new Line2D.Float(drawingOffsetX,y,drawingOffsetX+widthOfTileInPixels,y));
				}
				drawingOffsetX+=widthOfTileInPixels;
			}
			drawingOffsetY+=heightOfTileInPixels;
		}


		g2d.setColor(boxColor);
		g2d.setStroke(new BasicStroke(boxWidth));

		drawingOffsetY = 0;
		int channel=0;
		for (int row=0;row<nTilesPerColumn;++row) {
			float drawingOffsetX = 0;
			for (int col=0;col<nTilesPerRow;++col) {
				// Just drawing each bounding line once doesn't seem to help them sometimes
				// being thicker than others ... is this a stroke width problem (better if anti-aliasing on, but then too slow) ?
				//g2d.draw(new Rectangle2D.Double(drawingOffsetX,drawingOffsetY,drawingOffsetX+widthOfTile-1,drawingOffsetY+heightOfTile-1));
				if (row == 0)
					g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY,drawingOffsetX+widthOfTileInPixels,drawingOffsetY));					// top
				if (col == 0)
					g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY,drawingOffsetX,drawingOffsetY+heightOfTileInPixels));					// left
				g2d.draw(new Line2D.Float(drawingOffsetX,drawingOffsetY+heightOfTileInPixels,drawingOffsetX+widthOfTileInPixels,drawingOffsetY+heightOfTileInPixels));	// bottom
				g2d.draw(new Line2D.Float(drawingOffsetX+widthOfTileInPixels,drawingOffsetY,drawingOffsetX+widthOfTileInPixels,drawingOffsetY+heightOfTileInPixels));	// right
				
				if (channelNames != null && channel < displaySequence.length && displaySequence[channel] < channelNames.length) {
					String channelName=channelNames[displaySequence[channel]];
					if (channelName != null) {
						g2d.setColor(channelNameColor);
						g2d.setFont(channelNameFont);
						g2d.drawString(channelName,drawingOffsetX+channelNameXOffset,drawingOffsetY+channelNameYOffset);
					}
				}
				
				drawingOffsetX+=widthOfTileInPixels;
				++channel;
			}
			drawingOffsetY+=heightOfTileInPixels;
		}

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);	// ugly without

		g2d.setColor(curveColor);
		g2d.setStroke(new BasicStroke(curveWidth));

		float interceptY = heightOfTileInPixels/2;
//System.err.println("interceptY="+interceptY);
		float widthOfSampleInPixels=samplingIntervalInMilliSeconds/widthOfPixelInMilliSeconds;
//System.err.println("widthOfSampleInPixels="+widthOfSampleInPixels);

		int timeOffsetInSamples = (int)(timeOffsetInMilliSeconds/samplingIntervalInMilliSeconds);
//System.err.println("timeOffsetInSamples="+timeOffsetInSamples);
		int widthOfTileInSamples = (int)(widthOfTileInMilliSeconds/samplingIntervalInMilliSeconds);
//System.err.println("widthOfTileInSamples="+widthOfTileInSamples);
		int usableSamples = nSamplesPerChannel-timeOffsetInSamples;
		if (usableSamples <= 0) {
			//usableSamples=0;
			return;
		}
		else if (usableSamples > widthOfTileInSamples) {
			usableSamples=widthOfTileInSamples-1;
		}
//System.err.println("usableSamples="+usableSamples);

//long startTime = System.currentTimeMillis();
//long accumulatedDrawTime = 0;
//int linesDrawn=0;		
		drawingOffsetY = 0;
		channel=0;
		//Line2D.Float theLine = new Line2D.Float();
		GeneralPath thePath = new GeneralPath();
		for (int row=0;row<nTilesPerColumn && channel<numberOfChannels;++row) {
			float drawingOffsetX = 0;
			for (int col=0;col<nTilesPerRow && channel<numberOfChannels;++col) {
				float yOffset = drawingOffsetY + interceptY;
				short[] samplesForThisChannel = samples[displaySequence[channel]];
				int i = timeOffsetInSamples;
				float rescaleY =  amplitudeScalingFactorInMilliVolts[displaySequence[channel]]/heightOfPixelInMilliVolts;
//System.err.println("rescaleY="+rescaleY);

				float fromXValue = drawingOffsetX;
				float fromYValue = yOffset - samplesForThisChannel[i]*rescaleY;
				thePath.reset();
				thePath.moveTo(fromXValue,fromYValue);
				++i;
				for (int j=1;j<usableSamples;++j) {
					float toXValue = fromXValue + widthOfSampleInPixels;
					float toYValue = yOffset - samplesForThisChannel[i]*rescaleY;
//System.err.println("j="+j+" to samplesForThisChannel["+(i)+"]="+samplesForThisChannel[i]+" ("+fromXValue+","+fromYValue+")"+" to ("+toXValue+","+toYValue+")");
					i++;
					if ((int)fromXValue != (int)toXValue || (int)fromYValue != (int)toYValue) {
						//theLine.setLine(fromXValue,fromYValue,toXValue,toYValue);	// don't keep allocating new lines, reuse one
//long startDrawTime = System.currentTimeMillis();
						//g2d.draw(theLine);
						thePath.lineTo(toXValue,toYValue);
//accumulatedDrawTime+=(System.currentTimeMillis()-startDrawTime);
//++linesDrawn;
					}
					fromXValue=toXValue;
					fromYValue=toYValue;
				}
//long startDrawPathTime = System.currentTimeMillis();
				g2d.draw(thePath);
//long pathDrawingTime = System.currentTimeMillis()-startDrawPathTime;
//accumulatedDrawTime+=pathDrawingTime;

				drawingOffsetX+=widthOfTileInPixels;
				++channel;
			}
			drawingOffsetY+=heightOfTileInPixels;
		}
//{
//long finishTime = System.currentTimeMillis();
//long elapsedTime = finishTime-startTime;
//System.err.println("renderPlotToGraphics2D() elapsed: "+elapsedTime+" ms");
//System.err.println("renderPlotToGraphics2D() draw path: "+accumulatedDrawTime+" ms ("+accumulatedDrawTime*100.0/elapsedTime+" %)");
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
	private BufferedImage imageOfRenderedPlot;
	
	/**
	 * <p>Draw the data onto the supplied graphic with the specified background.</p>
	 *
	 * @param	g				the graphic to draw into
	 */
	public void paintComponent(Graphics g) {
//System.err.println("ECGPanel.paintComponent");
		//Rectangle r = this.getBounds();
		Rectangle r = new Rectangle(width,height);
		if (imageOfRenderedPlot == null) {
			imageOfRenderedPlot = createAppropriateBufferedImageToDrawInto(r);
			renderPlotToGraphics2D((Graphics2D)(imageOfRenderedPlot.getGraphics()),r,true/*fillBackgroundFirst*/);
		}
		g.drawImage(imageOfRenderedPlot,0,0,this);
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Display the specified sample values as an array of tiles in a window,
	 * and take a snapshot of it as a JPEG file.</p>
	 *
	 * @param	arg	an argument selecting the input type ("RAW", "DICOM" or "SCPECG"), followed by
	 *			either 8 more arguments, the raw data filename (2 bytes per signed 16 bit sample interleaved),
	 *			the number of channels, the number of samples per channel, the number of tiles per column, the number of tiles per row,
	 *			the sampling interval in milliseconds, the amplitude scaling factor in millivolts,
	 *			and the time offset in milliseconds for the left edge of the display
	 * 			or 4 more arguments, the SCPECG or DICOM data filename,
	 *			the number of tiles per column, the number of tiles per row,
	 *			and the time offset in milliseconds for the left edge of the display
	 */
	public static void main(String arg[]) {
		try {
			SourceECG sourceECG = null;
			BinaryInputStream i = new BinaryInputStream(new BufferedInputStream(new FileInputStream(arg[1])),false);		// little endian
			int nTilesPerColumn = 0;
			int nTilesPerRow = 0;
			float widthOfTileInMilliSeconds = 0;
			float heightOfTileInMilliVolts = 0;
			float timeOffsetInMilliSeconds = 0;

			if (arg.length == 9 && arg[0].toUpperCase(java.util.Locale.US).equals("RAW")) {
				int numberOfChannels = Integer.parseInt(arg[2]);
				int nSamplesPerChannel = Integer.parseInt(arg[3]);
				nTilesPerColumn = Integer.parseInt(arg[4]);
				nTilesPerRow = Integer.parseInt(arg[5]);
				float samplingIntervalInMilliSeconds = Float.parseFloat(arg[6]);
				float amplitudeScalingFactorInMilliVolts = Float.parseFloat(arg[7]);
				timeOffsetInMilliSeconds = Float.parseFloat(arg[8]);
//System.err.println("ECGPanel.main(): about to create sourceECG from raw");
				sourceECG = new RawSourceECG(i,numberOfChannels,nSamplesPerChannel,
					samplingIntervalInMilliSeconds, amplitudeScalingFactorInMilliVolts,
					true/*interleaved*/);
			}
			else if (arg.length == 5) {
				nTilesPerColumn = Integer.parseInt(arg[2]);
				nTilesPerRow = Integer.parseInt(arg[3]);
				timeOffsetInMilliSeconds = Float.parseFloat(arg[4]);
				if (arg[0].toUpperCase(java.util.Locale.US).equals("SCPECG")) {
//System.err.println("ECGPanel.main(): about to create sourceECG from SCPECG");
					sourceECG = new SCPSourceECG(i,true/*deriveAdditionalLeads*/);
				}
				else if (arg[0].toUpperCase(java.util.Locale.US).equals("DICOM")) {
//System.err.println("ECGPanel.main(): about to create sourceECG from DICOM");
					sourceECG = new DicomSourceECG(i);
				}
			}
				
			// assume screen is 72 dpi aka 72/25.4 pixels/mm
			
			float milliMetresPerPixel = (float)(25.4/72);
//System.err.println("ECGPanel.main(): milliMetresPerPixel="+milliMetresPerPixel);

			// ECG's normally printed at 25mm/sec and 10 mm/mV
			
			float horizontalPixelsPerMilliSecond = 25/(1000*milliMetresPerPixel);
//System.err.println("ECGPanel.main(): horizontalPixelsPerMilliSecond="+horizontalPixelsPerMilliSecond);
			float verticalPixelsPerMilliVolt = 10/(milliMetresPerPixel);
//System.err.println("ECGPanel.main(): verticalPixelsPerMilliVolt="+verticalPixelsPerMilliVolt);

//System.err.println("ECGPanel.main(): about to create ECGPanel");
			ECGPanel pg = new ECGPanel(
				sourceECG.getSamples(),
				sourceECG.getNumberOfChannels(),
				sourceECG.getNumberOfSamplesPerChannel(),
				sourceECG.getChannelNames(),
				nTilesPerColumn,nTilesPerRow,
				sourceECG.getSamplingIntervalInMilliSeconds(),
				sourceECG.getAmplitudeScalingFactorInMilliVolts(),
				horizontalPixelsPerMilliSecond,verticalPixelsPerMilliVolt,
				timeOffsetInMilliSeconds,
				sourceECG.getDisplaySequence(),
				800,400);
				
			// set size ...
			pg.setPreferredSize(new Dimension(800,400));
			
			String title = sourceECG.getTitle();
//System.err.println("ECGPanel.main(): about to create frame");
			ApplicationFrame app = new ApplicationFrame(title == null ? "ECG Panel" : title);
			//ApplicationFrame app = new ApplicationFrame("Application");
//System.err.println("ECGPanel.main(): about to add content pane");
			app.getContentPane().add(pg);
//System.err.println("ECGPanel.main(): about to pack");
			app.pack();
//System.err.println("ECGPanel.main(): about to show");
			app.setVisible(true);
			//app.takeSnapShot(app.getBounds());
		}
		catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}


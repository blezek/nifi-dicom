/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * <p>A class to encapsulate a text annotation positioned left or right and top or bottom with row offset relative to an abstract rectanglular frame.</p>
 *
 * @author	dclunie
 */
public class TextAnnotationPositioned {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/TextAnnotationPositioned.java,v 1.10 2017/01/24 10:50:41 dclunie Exp $";

	private String string;
	private boolean fromLeft;
	private boolean fromTop;
	private int textRow;
	
	/**
	 * @param	string		the annotation
	 * @param	fromLeft	true if positioned at the left of the frame, false if right
	 * @param	fromTop		true if positioned at the top of the frame, false if bottom
	 * @param	textRow		the number of text rows from the top or bottom (numbered from 0)
	 */
	public TextAnnotationPositioned(String string,boolean fromLeft,boolean fromTop,int textRow) {
		this.string=string;
		this.fromLeft=fromLeft;
		this.fromTop=fromTop;
		this.textRow=textRow;
	}

	/**
	 * <p>Get the text of the annotation.</p>
	 *
	 * @return	the annotation
	 */
	public String getString() { return string; }

	/**
	 * <p>Get the position relative to the left or right of the frame.</p>
	 *
	 * @return	true if positioned at the left of the frame, false if right
	 */
	public boolean isLeft() { return fromLeft; }

	/**
	 * <p>Get the position relative to the top or bottom of the frame.</p>
	 *
	 * @return	true if positioned at the top of the frame, false if bottom
	 */
	public boolean isTop() { return fromTop; }

	/**
	 * <p>Get the text row.</p>
	 *
	 * @return	the number of text rows from the top or bottom (numbered from 0)
	 */
	public int getTextRow() { return textRow; }



	/**
	 * @param	annotation		the text and position to be drawn
	 * @param	g2d			the drawing context
	 * @param	window			the actual component being drawn into
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawPositionedString(TextAnnotationPositioned annotation,
			Graphics2D g2d,Component window,
			int topAndBottomMargin,int leftAndRightMargin) {
			
		drawPositionedString(annotation.getString(),annotation.isLeft(),annotation.isTop(),annotation.getTextRow(),
			g2d,window,
			topAndBottomMargin,leftAndRightMargin);
	}
			

	/**
	 * @param	annotation		the text and position to be drawn
	 * @param	g2d			the drawing context
	 * @param	displayedAreaWidth	the width of the frame being drawn into, in pixels
	 * @param	displayedAreaHeight	the height of the frame being drawn into, in pixels
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawPositionedString(TextAnnotationPositioned annotation,
			Graphics2D g2d,int displayedAreaWidth,int displayedAreaHeight,
			int topAndBottomMargin,int leftAndRightMargin) {
			
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int stringHeight = fontMetrics.getHeight();
	
		drawPositionedString(annotation.getString(),annotation.isLeft(),annotation.isTop(),annotation.getTextRow(),
			g2d,fontMetrics,stringHeight,displayedAreaWidth,displayedAreaHeight,
			topAndBottomMargin,leftAndRightMargin);
	}
			
	/**
	 * @param	annotation		the text and position to be drawn
	 * @param	g2d			the drawing context
	 * @param	fontMetrics		the font metrics (already extracted from g2d, with which it must be consistent)
	 * @param	stringHeight		the height used for all rows of text (already extracted from fontMetrics, with which it must be consistent)
	 * @param	displayedAreaWidth	the width of the frame being drawn into, in pixels
	 * @param	displayedAreaHeight	the height of the frame being drawn into, in pixels
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawPositionedString(TextAnnotationPositioned annotation,
			Graphics2D g2d,FontMetrics fontMetrics,int stringHeight,int displayedAreaWidth,int displayedAreaHeight,
			int topAndBottomMargin,int leftAndRightMargin) {
			
		drawPositionedString(annotation.getString(),annotation.isLeft(),annotation.isTop(),annotation.getTextRow(),
			g2d,fontMetrics,stringHeight,displayedAreaWidth,displayedAreaHeight,
			topAndBottomMargin,leftAndRightMargin);
	}
			
	/**
	 * @param	string			the text to be drawn
	 * @param	fromLeft		true if positioned at the left of the frame, false if right
	 * @param	fromTop			true if positioned at the top of the frame, false if bottom
	 * @param	textRow			the number of text rows from the top or bottom (numbered from 0)
	 * @param	g2d			the drawing context
	 * @param	window			the actual component being drawn into
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawPositionedString(String string,boolean fromLeft,boolean fromTop,int textRow,
			Graphics2D g2d,Component window,
			int topAndBottomMargin,int leftAndRightMargin) {
			
		Rectangle windowSize = window.getBounds();
		int displayedAreaHeight = windowSize.height;
		int displayedAreaWidth  = windowSize.width;
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int stringHeight = fontMetrics.getHeight();
	
		drawPositionedString(string,fromLeft,fromTop,textRow,
			g2d,fontMetrics,stringHeight,displayedAreaWidth,displayedAreaHeight,
			topAndBottomMargin,leftAndRightMargin);
	}

	/**
	 * @param	string			the text to be drawn
	 * @param	fromLeft		true if positioned at the left of the frame, false if right
	 * @param	fromTop			true if positioned at the top of the frame, false if bottom
	 * @param	textRow			the number of text rows from the top or bottom (numbered from 0)
	 * @param	g2d			the drawing context
	 * @param	fontMetrics		the font metrics (already extracted from g2d, with which it must be consistent)
	 * @param	stringHeight		the height used for all rows of text (already extracted from fontMetrics, with which it must be consistent)
	 * @param	displayedAreaWidth	the width of the frame being drawn into, in pixels
	 * @param	displayedAreaHeight	the height of the frame being drawn into, in pixels
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawPositionedString(String string,boolean fromLeft,boolean fromTop,int textRow,
			Graphics2D g2d,FontMetrics fontMetrics,int stringHeight,int displayedAreaWidth,int displayedAreaHeight,
			int topAndBottomMargin,int leftAndRightMargin) {

		int stringWidth  = (int)(fontMetrics.getStringBounds(string,g2d).getWidth());
		int drawingPositionX = fromLeft ? leftAndRightMargin : displayedAreaWidth-leftAndRightMargin-stringWidth;
		int drawingPositionY = fromTop  ? (topAndBottomMargin+(textRow+1)*stringHeight)			// row+1 since Y is BOTTOM of drawing box
						: (displayedAreaHeight-topAndBottomMargin-textRow*stringHeight);

		DrawingUtilities.drawShadowedString(string,drawingPositionX,drawingPositionY,g2d);
	}

	/**
	 * @param	string				the text to be drawn
	 * @param	fromLeft			true if positioned at the left of the frame, false if right
	 * @param	g2d					the drawing context
	 * @param	window				the actual component being drawn into
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawVerticallyCenteredString(String string,boolean fromLeft,
			Graphics2D g2d,Component window,
			int leftAndRightMargin) {
		drawVerticallyCenteredString(string,fromLeft,g2d,window,0,leftAndRightMargin);	
	}

	/**
	 * @param	string				the text to be drawn
	 * @param	fromLeft			true if positioned at the left of the frame, false if right
	 * @param	g2d					the drawing context
	 * @param	window				the actual component being drawn into
	 * @param	verticalOffset		a downwards offset from the vertical center, in text rows (not pixels)
	 * @param	leftAndRightMargin	the margin to allow at the left and right of the frame, in pixels
	 */
	public static void drawVerticallyCenteredString(String string,boolean fromLeft,
			Graphics2D g2d,Component window,
			int verticalOffset,
			int leftAndRightMargin) {
			
		Rectangle windowSize = window.getBounds();
		int displayedAreaHeight = windowSize.height;
		int displayedAreaWidth  = windowSize.width;
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int stringHeight = fontMetrics.getHeight();
		int stringWidth  = (int)(fontMetrics.getStringBounds(string,g2d).getWidth());
		int drawingPositionX = fromLeft ? leftAndRightMargin : displayedAreaWidth-leftAndRightMargin-stringWidth;
		int drawingPositionY = displayedAreaHeight/2 - stringHeight/2 + (verticalOffset * stringHeight);
		
		DrawingUtilities.drawShadowedString(string,drawingPositionX,drawingPositionY,g2d);
	}
			
	/**
	 * @param	string			the text to be drawn
	 * @param	fromTop			true if positioned at the top of the frame, false if bottom
	 * @param	g2d			the drawing context
	 * @param	window			the actual component being drawn into
	 * @param	topAndBottomMargin	the margin to allow at the top and bottom of the frame, in pixels
	 */
	public static void drawHorizontallyCenteredString(String string,boolean fromTop,
			Graphics2D g2d,Component window,
			int topAndBottomMargin) {
			
		Rectangle windowSize = window.getBounds();
		int displayedAreaHeight = windowSize.height;
		int displayedAreaWidth  = windowSize.width;
		FontMetrics fontMetrics = g2d.getFontMetrics();
		int stringHeight = fontMetrics.getHeight();
		int stringWidth  = (int)(fontMetrics.getStringBounds(string,g2d).getWidth());
		int drawingPositionX = displayedAreaWidth/2-stringWidth/2;
		int drawingPositionY = fromTop  ? (topAndBottomMargin+stringHeight)
						: (displayedAreaHeight-topAndBottomMargin-stringHeight);
		
		DrawingUtilities.drawShadowedString(string,drawingPositionX,drawingPositionY,g2d);
	}
			
	/**
	 * @deprecated See {@link com.pixelmed.display.DrawingUtilities#drawShadowedString(String,int,int,Graphics2D) DrawingUtilities.drawShadowedString()}
	 */
	public static void drawShadowedString(String string,int x,int y,Graphics2D g2d) {
		DrawingUtilities.drawShadowedString(string,x,y,g2d);
	}

}


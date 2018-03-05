/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

public class ScanRange {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dose/ScanRange.java,v 1.9 2017/01/24 10:50:43 dclunie Exp $";
	
	protected String startDirection;
	protected String startLocation;
	protected String endDirection;
	protected String endLocation;
	protected String absoluteRange;
	protected double absoluteRangeAsDouble;		// will have valid value only if absoluteRange string is != null

	public ScanRange(String startDirection,String startLocation,String endDirection,String endLocation) {
		this.startDirection = startDirection;
		this.startLocation = startLocation;
		this.endDirection = endDirection;
		this.endLocation = endLocation;
		this.absoluteRange = null;
	}
	
	public ScanRange(String signedStartLocation,String signedEndLocation) {
		this.startDirection = signedStartLocation.startsWith("-") ? "I" : "S";
		this.startLocation = signedStartLocation.replaceFirst("[+-]","");
		this.endDirection =  signedEndLocation.startsWith("-") ? "I" : "S";
		this.endLocation = signedEndLocation.replaceFirst("[+-]","");
		this.absoluteRange = null;
	}
	
	public String getStartDirection() { return startDirection; }
	public String getStartLocation() { return startLocation; }
	public String getEndDirection() { return endDirection; }
	public String getEndLocation() { return endLocation; }
	
	public double getAbsoluteRangeAsDouble() {
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): from "+this);
//System.err.print("ScanRange.getAbsoluteRangeAsDouble():\n"+dump());
		if (absoluteRange == null) {
			double start = Double.parseDouble(startLocation);
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): start = "+start);
			if (startDirection.equals("I")) {
				start = -start;
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): start is I so making start -ve now = "+start);
			}
			double end = Double.parseDouble(endLocation);
//System.err.println("ScanRange.getAbsoluteRange(): end = "+end);
			if (endDirection.equals("I")) {
				end = -end;
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): end is I so making end -ve now = "+start);
			}
			absoluteRangeAsDouble = start - end;
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): absoluteRangeAsDouble = "+absoluteRangeAsDouble);
			if (absoluteRangeAsDouble < 0) {
				absoluteRangeAsDouble = -absoluteRangeAsDouble;
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): absoluteRangeAsDouble is -ve so changing sign now = "+absoluteRangeAsDouble);
			}
		}
//System.err.println("ScanRange.getAbsoluteRangeAsDouble(): returns "+absoluteRangeAsDouble);
		return absoluteRangeAsDouble;
	}
	
	public String getAbsoluteRange() {
//System.err.println("ScanRange.getAbsoluteRange(): from "+this);
//System.err.print("ScanRange.getAbsoluteRange():\n"+dump());
		if (absoluteRange == null) {
			getAbsoluteRangeAsDouble();
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance(java.util.Locale.US));
			formatter.setMaximumFractionDigits(3);
			formatter.setMinimumFractionDigits(3);
			formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
			formatter.setGroupingUsed(false);					// i.e., no comma at thousands
			absoluteRange = formatter.format(absoluteRangeAsDouble);
//System.err.println("ScanRange.getAbsoluteRange(): returns formatted string "+absoluteRange+" for "+Double.toString(absoluteRangeAsDouble));
		}
		return absoluteRange;
	}
	
	public boolean isStationary() { return getAbsoluteRangeAsDouble() < 0.0001; }
	
//	private String dump() {
//		StringBuffer buf = new StringBuffer();
//		buf.append("startDirection = \"");
//		buf.append(startDirection);
//		buf.append("\"\n");
//		buf.append("startLocation = \"");
//		buf.append(startLocation);
//		buf.append("\"\n");
//		buf.append("endDirection = \"");
//		buf.append(endDirection);
//		buf.append("\"\n");
//		buf.append("endLocation = \"");
//		buf.append(endLocation);
//		buf.append("\"\n");
//		return buf.toString();
//	}
	
	public String toString() {
		return startDirection + startLocation + "-" + endDirection + endLocation;
	}
	
	public boolean equals(Object o) {
		//System.err.println("Location.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof ScanRange) {
			ScanRange osr = (ScanRange)o;
			isEqual = osr.getStartDirection().equals(this.getStartDirection())
				   && osr.getStartLocation().equals(this.getStartLocation())
				   && osr.getEndDirection().equals(this.getEndDirection())
				   && osr.getEndLocation().equals(this.getEndLocation());
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}
	
	public int hashCode() {
		return getStartDirection().hashCode()
			 + getStartLocation().hashCode()
			 + getEndDirection().hashCode()
			 + getEndLocation().hashCode();	// sufficient to implement equals() contract
	}
}

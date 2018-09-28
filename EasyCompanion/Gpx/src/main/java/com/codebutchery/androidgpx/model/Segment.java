package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;

public class Segment extends Base {
	
	public static class XML {

		public static final String TAG_TRKSEG = "trkseg";
		
	};
	
	private ArrayList<TrackPoint> mTrackPoints = new ArrayList<TrackPoint>();
	
	public void addPoint(TrackPoint point) {
		mTrackPoints.add(point);
	}
	
	/**
	 * @return a copy of the points ArrayList for this segment
	 * */
	public ArrayList<TrackPoint> getTrackPoints() {
		
		// Return a copy of the points list so users won't be able to alter
		// our inner copy
		ArrayList<TrackPoint> points = new ArrayList<TrackPoint>();
		for (TrackPoint p : mTrackPoints) points.add(p);
		
		return points;
		
	}
	
	public void toGPX(PrintStream ps) {

		openXmlTag(XML.TAG_TRKSEG, ps, true, 2);

		for (TrackPoint p : mTrackPoints) p.toGPX(ps);
	
		closeXmlTag(XML.TAG_TRKSEG, ps, true, 2);
		
	}

}

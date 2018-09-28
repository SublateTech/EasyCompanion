package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;

public class Document extends Base {
	
	private final static String XMLNS = "http://www.topografix.com/GPX/1/1";
	private final static String CREATOR = "AndroidGPX ( http://codebutchery.wordpress.com )";
	private final static String VERSION = "1.1";
	private final static String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
	private final static String XSI_SCHEMALOCATION = "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd";
	
	public static class XML {

		public static final String ATTR_XMLNS = "xmlns";
		public static final String ATTR_CREATOR = "creator";
		public static final String ATTR_VERSION = "version";
		public static final String ATTR_XMLNS_XSI = "xmlns:xsi";
		public static final String ATTR_XSI_SCHEMALOCATION = "xsi:schemaLocation";
		
		public static final String TAG_GPX = "gpx";
		
	};
	
	private ArrayList<WayPoint> mWayPoints = null;
	private ArrayList<Track> mTracks = null;
    private ArrayList<Route> mRoutes = null;
	
	public ArrayList<Track> getTracks() {
		return mTracks;
	}
	
	public ArrayList<WayPoint> getWayPoints() {
		return mWayPoints;
	}

    public ArrayList<Route> getRoutes() {
        return mRoutes;
    }

	public Document(ArrayList<WayPoint> wayPoints, ArrayList<Track> tracks, ArrayList<Route> routes) {
		mWayPoints = wayPoints;
		mTracks = tracks;
        mRoutes = routes;
	}


	public void toGpx(PrintStream ps) {

		ArrayList<String> attrsNames = new ArrayList<String>();
		ArrayList<String> attrsValues = new ArrayList<String>();
		
		attrsNames.add(XML.ATTR_XMLNS);
		attrsNames.add(XML.ATTR_CREATOR);
		attrsNames.add(XML.ATTR_VERSION);
		attrsNames.add(XML.ATTR_XMLNS_XSI);
		attrsNames.add(XML.ATTR_XSI_SCHEMALOCATION);
		
		attrsValues.add(XMLNS);
		attrsValues.add(CREATOR);
		attrsValues.add(VERSION);
		attrsValues.add(XMLNS_XSI);
		attrsValues.add(XSI_SCHEMALOCATION);
		
		openXmlTag(XML.TAG_GPX, ps, attrsNames, attrsValues, true, 0);
		
		if (mWayPoints != null) for (WayPoint wp : mWayPoints) wp.toGPX(ps);
		if (mTracks != null) for (Track t : mTracks) t.toGPX(ps);
        if (mRoutes != null) for (Route r : mRoutes) r.toGPX(ps);
		
		closeXmlTag(XML.TAG_GPX, ps, true, 0);
		
	}


}

package com.codebutchery.androidgpx.xml;


import com.codebutchery.androidgpx.model.Base;
import com.codebutchery.androidgpx.model.Point;
import com.codebutchery.androidgpx.model.Route;
import com.codebutchery.androidgpx.model.RoutePoint;
import com.codebutchery.androidgpx.model.Segment;
import com.codebutchery.androidgpx.model.Track;
import com.codebutchery.androidgpx.model.TrackPoint;
import com.codebutchery.androidgpx.model.WayPoint;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Date;
 
public class GpxParserHandler extends DefaultHandler {
 
	public static interface GpxParserProgressListener {

		public void onGpxNewTrackParsed(int count, Track track);
        public void onGpxNewRouteParsed(int count, Route track);
		public void onGpxNewSegmentParsed(int count, Segment segment);
		public void onGpxNewTrackPointParsed(int count, TrackPoint trackPoint);
        public void onGpxNewRoutePointParsed(int count, RoutePoint routePoint);
		public void onGpxNewWayPointParsed(int count, WayPoint wayPoint);
		
	}
	
	private GpxParserProgressListener mListener = null;
    
    private Track mCurrentTrack = null;
    private Route mCurrentRoute = null;
    private Segment mCurrentSegment = null;
    private TrackPoint mCurrentTrackPoint = null;
    private RoutePoint mCurrentRoutePoint = null;
    private WayPoint mCurrentWayPoint = null;
    
    private int mTracksCount = 0;
    private int mRoutesCount = 0;
    private int mSegmentsCount = 0;
    private int mTrackPointsCount = 0;
    private int mRoutePointsCount = 0;
    private int mWayPointsCount = 0;
    
    private StringBuffer mStringBuffer = null;
    
    private Locator mLocator = null;
    private int mErrorLine = -1;
    private int mErrorColumn = -1;
    
    public int getErrorLine() {
    	return mErrorLine;
    }
    
    public int getErrorColumn() {
    	return mErrorColumn;
    }

    public GpxParserHandler(GpxParserProgressListener listener) {
    	mListener = listener;
    }
 
    @Override
    public void setDocumentLocator(Locator locator) {
    	mLocator = locator;
    }
    
    /**
     * Logs an error and throws a SAXException
     * The current line and column numbers are automatically added
     * @param message
     * @throws org.xml.sax.SAXException
     * */
    private void throwWithLocationInfo(String message) throws SAXException {
            
		if (mLocator != null) {
			mErrorLine = mLocator.getLineNumber();
            mErrorColumn = mLocator.getColumnNumber();
		}
    	
		String messageWithLineAndColumn = "Gpx Parser says: '" + message + 
                "' at line: " + mErrorLine + 
                " column: " + mErrorColumn + "'";

		throw new SAXException(messageWithLineAndColumn);
		
    }
    
    /** 
     * This will be called when the tags of the XML starts.
     **/
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	
    	mStringBuffer = new StringBuffer();
    	
    	if (localName.equals(WayPoint.XML.TAG_WPT)) onNewWayPoint(attributes);
    	else if (localName.equals(Track.XML.TAG_TRK)) onNewTrack();
        else if (localName.equals(Route.XML.TAG_RTE)) onNewRoute();
    	else if (localName.equals(Segment.XML.TAG_TRKSEG)) onNewTrackSegment();
    	else if (localName.equals(TrackPoint.XML.TAG_TRKPT)) onNewTrackPoint(attributes);
        else if (localName.equals(RoutePoint.XML.TAG_RTEPT)) onNewRoutePoint(attributes);

    }
    
    private void onNewTrack()  throws SAXException {

        // Gpx format checks
        if (mCurrentTrack != null) throwWithLocationInfo("Nested Track");
        if (mCurrentRoute != null) throwWithLocationInfo("Track inside route");
        if (mCurrentWayPoint != null) throwWithLocationInfo("Track inside waypoint");
        if (mCurrentSegment != null) throwWithLocationInfo("Track inside segment");
        if (mCurrentTrackPoint != null) throwWithLocationInfo("Track inside trackpoint");

        mCurrentTrack = new Track();

    }

    private void onNewRoute()  throws SAXException {

        // Gpx format checks
        if (mCurrentTrack != null) throwWithLocationInfo("Route inside track");
        if (mCurrentRoute != null) throwWithLocationInfo("Nested Route");
        if (mCurrentWayPoint != null) throwWithLocationInfo("Route inside waypoint");
        if (mCurrentSegment != null) throwWithLocationInfo("Route inside segment");
        if (mCurrentTrackPoint != null) throwWithLocationInfo("Route inside trackpoint");

        mCurrentRoute = new Route();

    }
    
    private void onNewTrackSegment()  throws SAXException {
    	
    	// Gpx format checks
        if (mCurrentRoute != null) throwWithLocationInfo("Segment inside route");
    	if (mCurrentWayPoint != null) throwWithLocationInfo("Segment inside waypoint");
    	if (mCurrentTrack == null) throwWithLocationInfo("Segment outside track");
    	if (mCurrentTrackPoint != null) throwWithLocationInfo("Segment inside trackpoint");
    	if (mCurrentSegment != null) throwWithLocationInfo("Nested Segment");
    	
    	mCurrentSegment = new Segment();
    	
    }
    
    private void onNewTrackPoint(Attributes attrs) throws SAXException {
    	
    	// Gpx format checks
        if (mCurrentRoute != null) throwWithLocationInfo("Trackpoint inside route");
    	if (mCurrentWayPoint != null) throwWithLocationInfo("Trackpoint inside waypoint");
    	if (mCurrentSegment == null) throwWithLocationInfo("Trackpoint outside segment");
    	if (mCurrentTrackPoint != null) throwWithLocationInfo("Nested Trackpoint");
    	
    	try {
			
			String latString = attrs.getValue(Point.XML.ATTR_LAT);
			if (latString == null || latString.length() == 0) throwWithLocationInfo("Latitude not present for Trackpoint");
			float lat = Float.parseFloat(latString);
		
			String lonString = attrs.getValue(Point.XML.ATTR_LON);
			if (lonString == null || lonString.length() == 0) throwWithLocationInfo("Longitude not present for Trackpoint");
			float lon = Float.parseFloat(lonString);
		
			mCurrentTrackPoint = new TrackPoint(lat, lon);
			
		} catch (NumberFormatException e) {
			throwWithLocationInfo(e.getMessage());
		}
    	
    }

    private void onNewRoutePoint(Attributes attrs) throws SAXException {

        // Gpx format checks
        if (mCurrentTrack != null) throwWithLocationInfo("Routepoint inside Track");
        if (mCurrentWayPoint != null) throwWithLocationInfo("Routepoint inside Waypoint");
        if (mCurrentRoute == null) throwWithLocationInfo("Routepoint outside route");
        if (mCurrentTrackPoint != null) throwWithLocationInfo("Trackpoint inside Route");

        try {

            String latString = attrs.getValue(Point.XML.ATTR_LAT);
            if (latString == null || latString.length() == 0) throwWithLocationInfo("Latitude not present for Trackpoint");
            float lat = Float.parseFloat(latString);

            String lonString = attrs.getValue(Point.XML.ATTR_LON);
            if (lonString == null || lonString.length() == 0) throwWithLocationInfo("Longitude not present for Trackpoint");
            float lon = Float.parseFloat(lonString);

            mCurrentRoutePoint = new RoutePoint(lat, lon);

        } catch (NumberFormatException e) {
            throwWithLocationInfo(e.getMessage());
        }

    }
    
    private void onNewWayPoint(Attributes attrs) throws SAXException {
    	
    	// Gpx format checks
        if (mCurrentRoute != null) throwWithLocationInfo("Waypoint inside Route");
    	if (mCurrentSegment != null) throwWithLocationInfo("Waypoint inside of segment");
    	if (mCurrentTrack != null) throwWithLocationInfo("Waypoint inside of track");
    	if (mCurrentTrackPoint != null) throwWithLocationInfo("Waypoint inside of trackpoint");
    	if (mCurrentWayPoint != null) throwWithLocationInfo("Nested Waypoint");
    	
    	try {
			
			String latString = attrs.getValue(Point.XML.ATTR_LAT);
			if (latString == null || latString.length() == 0) throwWithLocationInfo("Latitude not present for Waypoint");
			float lat = Float.parseFloat(latString);
		
			String lonString = attrs.getValue(Point.XML.ATTR_LON);
			if (lonString == null || lonString.length() == 0) throwWithLocationInfo("Longitude not present for Waypoint");
			float lon = Float.parseFloat(lonString);
		
			mCurrentWayPoint = new WayPoint(lat, lon);
			
		} catch (NumberFormatException e) {
			throwWithLocationInfo(e.getMessage());
		}
    	
    }
 
    /** 
     * This will be called when the tags of the XML end.
     **/
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	
        if (localName.equalsIgnoreCase(WayPoint.XML.TAG_WPT)) {
        	mListener.onGpxNewWayPointParsed(++mWayPointsCount, mCurrentWayPoint);
        	mCurrentWayPoint = null;
        }
        else if (localName.equalsIgnoreCase(Track.XML.TAG_TRK)) {
        	mListener.onGpxNewTrackParsed(++mTracksCount, mCurrentTrack);
        	mCurrentTrack = null;
        	mSegmentsCount = 0;
        }
        else if (localName.equalsIgnoreCase(Route.XML.TAG_RTE)) {
            mListener.onGpxNewRouteParsed(++mRoutesCount, mCurrentRoute);
            mCurrentRoute = null;
        }
        else if (localName.equalsIgnoreCase(Segment.XML.TAG_TRKSEG)) {
        	
        	if (mCurrentTrack == null) throwWithLocationInfo("end of segment outside track");
        	
        	mCurrentTrack.addSegment(mCurrentSegment);
        	
        	mListener.onGpxNewSegmentParsed(++mSegmentsCount, mCurrentSegment);
        	mCurrentSegment = null;
        }
        else if (localName.equalsIgnoreCase(TrackPoint.XML.TAG_TRKPT)) {
        	
        	if (mCurrentSegment == null) throwWithLocationInfo("end of trackpoint outside of segment");
        	
        	mCurrentSegment.addPoint(mCurrentTrackPoint);
        	
        	mListener.onGpxNewTrackPointParsed(++mTrackPointsCount, mCurrentTrackPoint);
        	mCurrentTrackPoint = null;
        }
        else if (localName.equalsIgnoreCase(RoutePoint.XML.TAG_RTEPT)) {

            if (mCurrentRoute == null) throwWithLocationInfo("end of routepoint outside of route");

            mCurrentRoute.addPoint(mCurrentRoutePoint);

            mListener.onGpxNewRoutePointParsed(++mRoutePointsCount, mCurrentRoutePoint);
            mCurrentRoutePoint = null;
        }
        // Track Points / Way Points / Route Points tags
        else if (localName.equalsIgnoreCase(Point.XML.TAG_DESC)) {
        	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setDescription(mStringBuffer.toString()); }
        	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setDescription(mStringBuffer.toString()); }
            else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setDescription(mStringBuffer.toString()); }
            else if (mCurrentTrack != null) { mCurrentTrack.setUserDescription(mStringBuffer.toString()); }
            else if (mCurrentRoute != null) { mCurrentRoute.setUserDescription(mStringBuffer.toString()); }
            //else throwWithLocationInfo("Found misplaced tag " + Point.XML.TAG_DESC);
        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_ELE)) {
        	
        	String stringValue = mStringBuffer.toString();
        	if (stringValue.length() > 0) {
        		float elevation = 0;
            	
            	try {
            		elevation = Float.parseFloat(mStringBuffer.toString());
            	} catch(NumberFormatException e) {
            		throwWithLocationInfo("wrong float number format");
            	}
            	
            	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setElevation(elevation); }
            	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setElevation(elevation); }
                else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setElevation(elevation); }
        	}
        	
        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_HDOP)) {
        	
        	String stringValue = mStringBuffer.toString();
        	if (stringValue.length() > 0) {
        		float hdop = 0;
            	
            	try {
            		hdop = Float.parseFloat(mStringBuffer.toString());
            	} catch(NumberFormatException e) {
            		throwWithLocationInfo("wrong float number format");
            	}
            	
            	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setHDop(hdop); }
            	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setHDop(hdop); }
                else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setHDop(hdop); }
        	}

        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_VDOP)) {
        	
        	String stringValue = mStringBuffer.toString();
        	if (stringValue.length() > 0) {
        		float vdop = 0;
            	
            	try {
            		vdop = Float.parseFloat(mStringBuffer.toString());
            	} catch(NumberFormatException e) {
            		throwWithLocationInfo("wrong float number format");
            	}
            	
            	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setVDop(vdop); }
            	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setVDop(vdop); }
                else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setVDop(vdop); }
        	}

        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_NAME)) {
        	
        	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setName(mStringBuffer.toString()); }
        	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setName(mStringBuffer.toString()); }
            else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setName(mStringBuffer.toString()); }
            else if (mCurrentTrack != null) { mCurrentTrack.setName(mStringBuffer.toString()); }
            else if (mCurrentRoute != null) { mCurrentRoute.setName(mStringBuffer.toString()); }
            //else throwWithLocationInfo("Found misplaced tag " + Point.XML.TAG_NAME);

        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_TIME)) {
        	
        	String dateTime = mStringBuffer.toString();
        	Date date = null;
        	
        	if (dateTime.length() > 0) {
        		date = Base.parseTimestampIntoDate(dateTime);
            	if (date == null) throwWithLocationInfo("Wrong timestamp format, correct format is \"yyyy-MM-dd'T'HH:mm:ss'Z'\"");
        	}
        	
        	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setTimeStamp(date); }
        	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setTimeStamp(date); }
            else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setTimeStamp(date); }
        	
        }
        else if (localName.equalsIgnoreCase(Point.XML.TAG_TYPE)) {
        	if (mCurrentTrackPoint != null) { mCurrentTrackPoint.setType(mStringBuffer.toString()); }
        	else if (mCurrentWayPoint != null) { mCurrentWayPoint.setType(mStringBuffer.toString()); }
            else if (mCurrentRoutePoint != null) { mCurrentRoutePoint.setType(mStringBuffer.toString()); }
            else if (mCurrentTrack != null) mCurrentTrack.setType(mStringBuffer.toString());
            else if (mCurrentRoute != null) mCurrentRoute.setType(mStringBuffer.toString());
            //else throwWithLocationInfo("Found misplaced tag: " + Track.XML.TAG_TYPE);
        	
        }
        // Track/Route tags
        else if (localName.equalsIgnoreCase(Track.XML.TAG_CMT)) {
            if (mCurrentTrack != null) mCurrentTrack.setGpsComment(mStringBuffer.toString());
            else if (mCurrentRoute != null) mCurrentRoute.setGpsComment(mStringBuffer.toString());
           // else throwWithLocationInfo("Found misplaced tag: " + Track.XML.TAG_CMT);
        }
        
        mStringBuffer = null;
        
    }
    

    /** 
     * This is called to get the tags value
     **/
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

    	if (mStringBuffer != null)
    		mStringBuffer.append(ch, start, start + length);

    }
 
}

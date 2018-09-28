package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;


public class TrackPoint extends Point {
	
	public static class XML extends BaseXML{
		
		public static final String TAG_TRKPT = "trkpt";
		
	};

	public TrackPoint(float lat, float lon) {
		super(lat, lon);
	}
	
	public void toGPX(PrintStream ps) {
		
		ArrayList<String> attrsNames = new ArrayList<String>();
		ArrayList<String> attrsValues = new ArrayList<String>();
		
		attrsNames.add(XML.ATTR_LAT);
		attrsNames.add(XML.ATTR_LON);
		
		attrsValues.add(Float.toString(getLatitude()));
		attrsValues.add(Float.toString(getLongitude()));
		
		openXmlTag(XML.TAG_TRKPT, ps, attrsNames, attrsValues, true, 3);
		
		putFloatValueInXmlIfNotNull(XML.TAG_ELE, getElevation(), ps, 4);
		putDateTimeValueInXmlIfNotNull(XML.TAG_TIME, getTimeStamp(), ps, 4);
		putStringValueInXmlIfNotNull(XML.TAG_NAME, getName(), ps, 4);
		putStringValueInXmlIfNotNull(XML.TAG_TYPE, getType(), ps, 4);
		putStringValueInXmlIfNotNull(XML.TAG_DESC, getDescription(), ps, 4);
		putFloatValueInXmlIfNotNull(XML.TAG_HDOP, getHDop(), ps, 4);
		putFloatValueInXmlIfNotNull(XML.TAG_VDOP, getVDop(), ps, 4);
	
		closeXmlTag(XML.TAG_TRKPT, ps, true, 3);
		
	}

}

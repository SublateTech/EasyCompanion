package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;


public class WayPoint extends Point {
	
	public static class XML extends BaseXML {
		
		public static final String TAG_WPT = "wpt";

	};

	public WayPoint(float lat, float lon) {
		super(lat, lon);
	}

	public void toGPX(PrintStream ps) {
		
		ArrayList<String> attrsNames = new ArrayList<String>();
		ArrayList<String> attrsValues = new ArrayList<String>();
		
		attrsNames.add(XML.ATTR_LAT);
		attrsNames.add(XML.ATTR_LON);
		
		attrsValues.add(Float.toString(getLatitude()));
		attrsValues.add(Float.toString(getLongitude()));
		
		openXmlTag(XML.TAG_WPT, ps, attrsNames, attrsValues, true, 1);
		
		putFloatValueInXmlIfNotNull(XML.TAG_ELE, getElevation(), ps, 2);
		putDateTimeValueInXmlIfNotNull(XML.TAG_TIME, getTimeStamp(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_NAME, getName(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_TYPE, getType(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_DESC, getDescription(), ps, 2);
		putFloatValueInXmlIfNotNull(XML.TAG_HDOP, getHDop(), ps, 2);
		putFloatValueInXmlIfNotNull(XML.TAG_VDOP, getVDop(), ps, 2);
	
		closeXmlTag(XML.TAG_WPT, ps, true, 1);
		
	}
	
}

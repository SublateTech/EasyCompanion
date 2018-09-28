package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;


/**
 * This is the base class for {@link TrackPoint} and {@link WayPoint}
 * Currently supported fields are:
 * 	- Latitude
 *  - Longitude
 *  - Horizontal precision
 *  - Vertical precision
 *  - Elevation
 *  - Timestamp
 *  - Name
 *  - Description
 *  - Type
 * */
public class Point extends Base {


	public static class XML extends BaseXML {
		public static final String TAG = "Point";
	};

	@SuppressWarnings("unused")
	private static final String TAG = XML.TAG;

	/**
	 *  Latitude in degrees. This value is in the range [-90, 90]
	 */
	private Float mLatitude = null;
	
	/**
	 *  Longitude in degrees. This value is in the range [-180, 180) 
	 */
	private Float mLongitude = null;
	
	/**
	 *  Horizontal dilution of precision
	 */
	private Float mHDop = null;
	
	/**
	 *  Vertical dilution of precision
	 */
	private Float mVDop = null;

	/**
	 * Elevation value in meters.
	 */
	private Float mElevation = null;
	
	/**
	 * Timestamp for this point
	 * */
	private Date mDate = null;
	
	/**
	 * Point Name
	 * */
	private String mName = null;

	/**
	 * Point Description
	 * */
	private String mDescription = null;
	
	/**
	 * Point Type as String
	 * */
	private String mType = null;
	
	/**
	 * Latitude and Longitude are mandatory values
	 * in order to construct this object
	 * 
	 * */
	public Point(float lat, float lon) {
		mLatitude = lat;
		mLongitude = lon;
	}
	
	
	public Float getLatitude() {
		return mLatitude;
	}

	public void setLatitude(float mLatitude) {
		this.mLatitude = mLatitude;
	}

	public Float getLongitude() {
		return mLongitude;
	}

	public void setLongitude(float mLongitude) {
		this.mLongitude = mLongitude;
	}

	public Float getElevation() {
		return mElevation;
	}

	public void setElevation(float mElevation) {
		this.mElevation = mElevation;
	}

	public Date getTimeStamp() {
		return mDate;
	}

	public void setTimeStamp(Date date) {
		this.mDate = date;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}
	
	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String mDescription) {
		this.mDescription = mDescription;
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}
	
	public Float getHDop() {
		return mHDop;
	}

	public void setHDop(Float mHDop) {
		this.mHDop = mHDop;
	}

	public Float getVDop() {
		return mVDop;
	}

	public void setVDop(Float mVDop) {
		this.mVDop = mVDop;
	}

	public void toGPX(PrintStream ps) {

		ArrayList<String> attrsNames = new ArrayList<String>();
		ArrayList<String> attrsValues = new ArrayList<String>();

		attrsNames.add(Point.XML.ATTR_LAT);
		attrsNames.add(Point.XML.ATTR_LON);

		attrsValues.add(Float.toString(getLatitude()));
		attrsValues.add(Float.toString(getLongitude()));

		openXmlTag(XML.TAG, ps, attrsNames, attrsValues, true, 1);

		putFloatValueInXmlIfNotNull(Point.XML.TAG_ELE, getElevation(), ps, 2);
		putDateTimeValueInXmlIfNotNull(Point.XML.TAG_TIME, getTimeStamp(), ps, 2);
		putStringValueInXmlIfNotNull(Point.XML.TAG_NAME, getName(), ps, 2);
		putStringValueInXmlIfNotNull(Point.XML.TAG_TYPE, getType(), ps, 2);
		putStringValueInXmlIfNotNull(Point.XML.TAG_DESC, getDescription(), ps, 2);
		putFloatValueInXmlIfNotNull(Point.XML.TAG_HDOP, getHDop(), ps, 2);
		putFloatValueInXmlIfNotNull(Point.XML.TAG_VDOP, getVDop(), ps, 2);

		closeXmlTag(XML.TAG, ps, true, 1);

	}




}

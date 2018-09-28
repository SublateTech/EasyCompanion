package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;

public class Route extends Base {
	
	public static class XML extends BaseXML {
		
		public static final String TAG_RTE = "rte";
		public static final String TAG_CMT = "cmt";

	};

	/**
	 * Name of the route
	 * */
	private String mName = null;
	
	/**
	 * GPS comment about this route
	 * */
	private String mGpsComment = null;
	
	/**
	 * User description about this route
	 * */
	private String mUserDescription = null;

    /**
     * Route points
     * */
    private ArrayList<RoutePoint> mRoutePoints = new ArrayList<RoutePoint>();
	
	/**
	 * Route type
	 * */
	private String mType = null;

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	public String getGpsComment() {
		return mGpsComment;
	}

	public void setGpsComment(String mGpsComment) {
		this.mGpsComment = mGpsComment;
	}

	public String getUserDescription() {
		return mUserDescription;
	}

	public void setUserDescription(String mUserDescription) {
		this.mUserDescription = mUserDescription;
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}

    public void addPoint(RoutePoint point) {
        mRoutePoints.add(point);
    }

    /**
     * @return a copy of the points ArrayList for this route
     * */
    public ArrayList<RoutePoint> getRoutePoints() {

        // Return a copy of the points list so users won't be able to alter
        // our inner copy
        ArrayList<RoutePoint> points = new ArrayList<RoutePoint>();
        for (RoutePoint p : mRoutePoints) points.add(p);

        return points;

    }

	public void toGPX(PrintStream ps) {
		
		openXmlTag(XML.TAG_RTE, ps, true, 1);
		
		putStringValueInXmlIfNotNull(XML.TAG_NAME, getName(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_CMT, getGpsComment(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_DESC, getUserDescription(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_TYPE, getType(), ps, 2);

        for (RoutePoint rp : mRoutePoints) rp.toGPX(ps);

		closeXmlTag(XML.TAG_RTE, ps, true, 1);
		
	}
	
}

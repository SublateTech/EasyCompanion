package com.codebutchery.androidgpx.model;

import java.io.PrintStream;
import java.util.ArrayList;

public class Track extends Base {
	
	public static class XML {
		
		public static final String TAG_TRK = "trk";

		public static final String TAG_NAME = "name";
		public static final String TAG_CMT = "cmt";
		public static final String TAG_DESC = "desc";
		public static final String TAG_TYPE = "type";
		
	};

	/**
	 * Name of the track
	 * */
	private String mName = null;
	
	/**
	 * GPS comment about this track
	 * */
	private String mGpsComment = null;
	
	/**
	 * User description about this track
	 * */
	private String mUserDescription = null;
	
	/**
	 * Track type
	 * */
	private String mType = null;
	
	/**
	 * Segments list
	 * */
	private ArrayList<Segment> mSegments = new ArrayList<Segment>();

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

	public ArrayList<Segment> getSegments() {
		
		// Return a copy of the segments list so users won't be able to alter
		// our inner copy
		ArrayList<Segment> segs = new ArrayList<Segment>();
		for (Segment s : mSegments) segs.add(s);
				
		return segs;
		
	}

	public void addSegment(Segment seg) {
		this.mSegments.add(seg);
	}

	public void toGPX(PrintStream ps) {
		
		openXmlTag(XML.TAG_TRK, ps, true, 1);
		
		putStringValueInXmlIfNotNull(XML.TAG_NAME, getName(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_CMT, getGpsComment(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_DESC, getUserDescription(), ps, 2);
		putStringValueInXmlIfNotNull(XML.TAG_TYPE, getType(), ps, 2);

		for (Segment s : mSegments) s.toGPX(ps);
	
		closeXmlTag(XML.TAG_TRK, ps, true, 1);
		
	}
	
}

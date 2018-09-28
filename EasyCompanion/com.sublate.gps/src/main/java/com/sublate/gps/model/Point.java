/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sublate.gps.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;

import com.sublate.gps.helper.DateUtils;
/**
 * Class that holds a tracker entry. An entry can be either a valid location, or
 * a simple log msg
 *
 * It provides a concrete data structure to represent data stored in the
 * TrackerProvider
 */
public class Point {

    static final String TIMESTAMP = "Timestamp";
    static final String TAG = "Tag";
    public static final String ENTRY_TYPE = "Type";

    private Location mLocation;
    public static Location lastPosition;
    private float mDistance;
    private int mScheduleId;
    private String mLogMsg;

    public static final String ID_COL = "_id";
    static final String SCHEDULE_ID = "ScheduleId";
    static final String ACCURACY = "Accuracy";
    static final String LATITUDE = "Latitude";
    static final String LONGITUDE = "Longitude";
    static final String ALTITUDE = "Altitude";
    static final String SPEED = "Speed";
    static final String BEARING = "Bearing";
    static final String LOC_TIME = "LocationTime";
    static final String DEBUG_INFO = "DebugInfo";
    static final String DISTANCE = "Distance";
    static final String STRING_DATA = "STRING";
    static final String INT_DATA = "INTEGER";
    static final String REAL_DATA = "REAL";


    public static String[] ATTRIBUTES = {
            ID_COL, SCHEDULE_ID, TIMESTAMP, TAG, ENTRY_TYPE, ACCURACY, LATITUDE, LONGITUDE,
            ALTITUDE, SPEED, BEARING,  LOC_TIME, DEBUG_INFO, DISTANCE};
    public static  String[] ATTRIBUTES_DATA_TYPE = {
            INT_DATA + " PRIMARY KEY", INT_DATA, STRING_DATA, STRING_DATA, STRING_DATA,
            REAL_DATA, REAL_DATA, REAL_DATA, REAL_DATA, REAL_DATA, REAL_DATA,
            INT_DATA, STRING_DATA,REAL_DATA};

    // location extra keys used to retrieve debug info
    private static final String NETWORK_LOCATION_SOURCE_KEY =
        "networkLocationSource";
    private static final String NETWORK_LOCATION_TYPE_KEY =
        "networkLocationType";
    private static final String[] LOCATION_DEBUG_KEYS = {
            NETWORK_LOCATION_SOURCE_KEY, NETWORK_LOCATION_TYPE_KEY};

    public enum EntryType {
        LOCATION_TYPE, LOG_TYPE
    }

    private String mTimestamp;
    private String mTag;
    private EntryType mType;

    private Point(String tag, EntryType type) {
        mType = type;
        mTag = tag;
        mLocation = null;
    }

    private Point(Location loc) {
        this(loc.getProvider(), EntryType.LOCATION_TYPE);
        mLocation = new Location(loc);
    }

    /**
     * Creates a Point from a Location
     */
    static Point createEntry(Location loc, int scheduleId) {
        Point entry = new Point(loc);

        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        entry.setTimestamp(timestampVal);
        entry.setScheduleId(scheduleId);

        if (lastPosition==null)
            lastPosition = loc;
        entry.setDistance(lastPosition.distanceTo(loc));
        lastPosition = loc;

        return entry;
    }


    /**
     * Creates a Point from a log msg
     */
    static Point createEntry(String tag, String msg) {
        Point entry = new Point(tag, EntryType.LOG_TYPE);
        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        entry.setTimestamp(timestampVal);
        entry.setLogMsg(msg);
        return entry;
    }

    private void setTimestamp(String timestamp) {
        mTimestamp = timestamp;
    }

    public EntryType getType() {
        return mType;
    }

    public void setDistance(float distance) {
       mDistance = distance;
    }

    public int getScheduleId() {
        return mScheduleId;
    }

    private void setLogMsg(String msg) {
        mLogMsg = msg;
    }

    public void setScheduleId(int scheduleId) {
        this.mScheduleId = scheduleId;
    }

    private void setLocation(Location location) {
        mLocation = location;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public String getTag() {
        return mTag;
    }

    public Location getLocation() {
        return mLocation;
    }

    public String getLogMsg() {
        return mLogMsg;
    }

    public float getDistance() {
        return mDistance;
    }

    static void buildCreationString(StringBuilder builder) {
        if (ATTRIBUTES.length != ATTRIBUTES_DATA_TYPE.length) {
            throw new IllegalArgumentException(
                    "Attribute length does not match data type length");
        }
        for (int i = 0; i < ATTRIBUTES_DATA_TYPE.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(String.format("%s %s", ATTRIBUTES[i],
                    ATTRIBUTES_DATA_TYPE[i]));
        }
    }

    ContentValues getAsContentValues() {
        ContentValues cValues = new ContentValues(ATTRIBUTES.length);
        cValues.put(TIMESTAMP, mTimestamp);
        cValues.put(TAG, mTag);
        cValues.put(ENTRY_TYPE, mType.toString());
        if (mType == EntryType.LOCATION_TYPE) {
            cValues.put(LATITUDE, mLocation.getLatitude());
            cValues.put(LONGITUDE, mLocation.getLongitude());
            if (mLocation.hasAccuracy()) {
                cValues.put(ACCURACY, mLocation.getAccuracy());
            }
            if (mLocation.hasAltitude()) {
                cValues.put(ALTITUDE, mLocation.getAltitude());
            }
            if (mLocation.hasSpeed()) {
                cValues.put(SPEED, mLocation.getSpeed());
            }
            if (mLocation.hasBearing()) {
                cValues.put(BEARING, mLocation.getBearing());
            }
            //cValues.put(DIST_NET_LOCATION, mDistFromNetLocation);
            cValues.put(LOC_TIME, mLocation.getTime());
            StringBuilder debugBuilder = new StringBuilder("");
            if (mLocation.getExtras() != null) {
                for (String key : LOCATION_DEBUG_KEYS) {
                    Object val = mLocation.getExtras().get(key);
                    if (val != null) {
                        debugBuilder.append(String.format("%s=%s; ", key, val
                                .toString()));
                    }
                }
            }
            cValues.put(DEBUG_INFO, debugBuilder.toString());
            cValues.put(DISTANCE, mDistance);
            cValues.put(SCHEDULE_ID,mScheduleId);
        } else {
            cValues.put(DEBUG_INFO, mLogMsg);
        }
        return cValues;
    }

    static Point createEntry(Cursor cursor) {
        String timestamp = cursor.getString(cursor.getColumnIndex(TIMESTAMP));
        String tag = cursor.getString(cursor.getColumnIndex(TAG));
        String sType = cursor.getString(cursor.getColumnIndex(ENTRY_TYPE));
        Point entry = new Point(tag, EntryType.valueOf(sType));
        entry.setTimestamp(timestamp);
        entry.setDistance(getNullableFloat(cursor, DISTANCE));
        if (entry.getType() == EntryType.LOCATION_TYPE) {
            Location location = new Location(tag);
            location.setLatitude(cursor.getFloat(cursor
                    .getColumnIndexOrThrow(LATITUDE)));
            location.setLongitude(cursor.getFloat(cursor
                    .getColumnIndexOrThrow(LONGITUDE)));

            Float accuracy = getNullableFloat(cursor, ACCURACY);
            if (accuracy != null) {
                location.setAccuracy(accuracy);
            }
            Float altitude = getNullableFloat(cursor, ALTITUDE);
            if (altitude != null) {
                location.setAltitude(altitude);
            }
            Float bearing = getNullableFloat(cursor, BEARING);
            if (bearing != null) {
                location.setBearing(bearing);
            }
            Float speed = getNullableFloat(cursor, SPEED);
            if (speed != null) {
                location.setSpeed(speed);
            }
            location.setTime(cursor.getLong(cursor.getColumnIndex(LOC_TIME)));
            entry.setLocation(location);
        }
        entry.setLogMsg(cursor.getString(cursor.getColumnIndex(DEBUG_INFO)));

        return entry;
    }

    public static Location  createLocation(Cursor cursor) {

        String timestamp = cursor.getString(cursor.getColumnIndex(TIMESTAMP));
        String tag = cursor.getString(cursor.getColumnIndex(TAG));
        String sType = cursor.getString(cursor.getColumnIndex(ENTRY_TYPE));
        Location location = null;
        if (EntryType.valueOf(sType) == EntryType.LOCATION_TYPE) {
            location = new Location(tag);
            location.setLatitude(cursor.getFloat(cursor
                    .getColumnIndexOrThrow(LATITUDE)));
            location.setLongitude(cursor.getFloat(cursor
                    .getColumnIndexOrThrow(LONGITUDE)));

            Float accuracy = getNullableFloat(cursor, ACCURACY);
            if (accuracy != null) {
                location.setAccuracy(accuracy);
            }
            Float altitude = getNullableFloat(cursor, ALTITUDE);
            if (altitude != null) {
                location.setAltitude(altitude);
            }
            Float bearing = getNullableFloat(cursor, BEARING);
            if (bearing != null) {
                location.setBearing(bearing);
            }
            Float speed = getNullableFloat(cursor, SPEED);
            if (speed != null) {
                location.setSpeed(speed);
            }
            location.setTime(cursor.getLong(cursor.getColumnIndex(LOC_TIME)));
            //lastPosition = location;
        }
        return location;

    }

    private static Float getNullableFloat(Cursor cursor, String colName) {
        Float retValue = null;
        int colIndex = cursor.getColumnIndexOrThrow(colName);
        if (!cursor.isNull(colIndex)) {
            retValue = cursor.getFloat(colIndex);
        }
        return retValue;
    }
}
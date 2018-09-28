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
import android.net.Uri;
import android.os.Environment;

import com.sublate.gps.Config;
import com.sublate.gps.helper.DateUtils;

import java.io.File;

public class Route {

    /*
    .add("id","INTEGER","primary key autoincrement")
    .add("ScheduleId","INTEGER")
    .add("dateStamp","STRING")
    .add("timeStart","INTEGER")
    .add("timeEnd","INTEGER")
    .add("distance","REAL")
    .add("name","STRING")
    .add("minHeight","REAL")
    .add("maxHeight","REAL")
    .add("pointCount","INTEGER")
    .add("fileSent", "INTEGER","Default 0")
        */


    static final String ID = "id";
    static final String SCHEDULE_ID = "ScheduleId";
    static final String DATESTAMP = "dateStamp";
    static final String TIMESTART = "timeStart";
    static final String TIMEEND = "timeEnd";
    static final String DISTANCE = "distance";
    static final String NAME = "name";
    static final String MINHEIGHT = "minHeight";
    static final String MAXHEIGHT = "maxHeight";
    static final String POINTCOUNT = "pointCount";
    static final String FILESENT = "fileSent";

    static final String STRING_DATA = "STRING";
    static final String INT_DATA = "INTEGER";
    static final String REAL_DATA = "REAL";
    static final String BLOB_DATA = "BLOB";

    static final String[] ATTRIBUTES = {
            ID, SCHEDULE_ID,DATESTAMP,TIMESTART,TIMEEND,DISTANCE,NAME,MINHEIGHT,MAXHEIGHT,POINTCOUNT,FILESENT};
    static final String[] ATTRIBUTES_DATA_TYPE = {
            INT_DATA + " PRIMARY KEY", INT_DATA, INT_DATA,INT_DATA,INT_DATA,REAL_DATA,STRING_DATA,REAL_DATA,REAL_DATA,INT_DATA,INT_DATA};

    private String mTimestamp;
    private int mScheduleId;
    private float mDistance;
    private String mdateStamp;
    private long mtimeStart;
    private long mtimeEnd;
    private String mName;
    private long mminHeight;
    private long mmaxHeight;
    private int mpointCount;
    private int mfileSent;
    private long mId;
    private String mFileName;

    public Route() {
        mDistance = 0;
        mdateStamp="";
        mtimeStart=0;
        mtimeEnd=0;
        mName = "";
        mminHeight = 0;
        mmaxHeight = 0;
        mpointCount = 0;
        mfileSent = 0;
        mId=0;
        mFileName = null;
    }

    public String getFileName() {
        mFileName = mScheduleId + "_" + mdateStamp + "_" + mName ;
        mFileName = mFileName.replaceAll(" ", "_");
        return mFileName;
    }

    /**
     * Creates a Point from a Location
     */
    static Route createEntry(Location loc, float distFromNetLocation) {
        Route entry = new Route();

        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        entry.setTimestamp(timestampVal);



        return entry;
    }

    public  void setId(long Id) {
        mId = Id;
    }

    public  long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public long getNumberOfPlacemarks() {
        return mpointCount;
    }
    public long getNumberOfLocations() {
        return mpointCount;
    }

    /**
     * Creates a Point from a log msg
     */
    static Route createEntry(String tag, String msg) {
        Route entry = new Route();
        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        entry.setTimestamp(timestampVal);
        return entry;
    }

    public long getScheduleId() {
        return mScheduleId;
    }

    public String getDateStamp() {
        return mdateStamp;
    }

    private void setTimestamp(String timestamp) {
        mTimestamp = timestamp;
    }

    private void setDistance(float distance) {
       mDistance = distance;
    }

    double getDistance() {
        return mDistance;
    }

    public void setScheduleId(int scheduleId) {
         mScheduleId = scheduleId;
    }

    public void setdateStamp(String mdateStamp) {
        this.mdateStamp = mdateStamp;
    }

    public void settimeStart(long mtimeStart) {
        this.mtimeStart = mtimeStart;
    }

    public long gettimeStart() {
        return mtimeStart;
    }

    public void settimeEnd(long mtimeEnd) {
        this.mtimeEnd = mtimeEnd;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setminHeight(long mminHeight) {
        this.mminHeight = mminHeight;
    }

    public void setmaxHeight(long mmaxHeight) {
        this.mmaxHeight = mmaxHeight;
    }

    public void setpointCount(int mpointCount) {
        this.mpointCount = mpointCount;
    }

    public void setfileSent(int mfileSent) {
        this.mfileSent = mfileSent;
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
        cValues.put(SCHEDULE_ID, mScheduleId);
        cValues.put(DATESTAMP, mdateStamp);
        cValues.put(DISTANCE, mDistance);
        cValues.put(TIMESTART, mtimeStart);
        cValues.put(TIMEEND,mtimeEnd);
        cValues.put(NAME,mName);
        cValues.put(MINHEIGHT,mminHeight );
        cValues.put(MAXHEIGHT,mmaxHeight);
        cValues.put(POINTCOUNT,mpointCount);
        cValues.put(FILESENT,mfileSent);
        return cValues;
    }

    static Route createEntry(Cursor cursor) {
        Route entry = new Route();
        entry.setId(Long.parseLong(cursor.getString(cursor.getColumnIndex(ID))));
        entry.setScheduleId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(SCHEDULE_ID))));
        entry.setdateStamp(cursor.getString(cursor.getColumnIndex(DATESTAMP)));
        entry.setDistance(Float.parseFloat(cursor.getString(cursor.getColumnIndex(DISTANCE))));
        entry.settimeStart(Long.parseLong(cursor.getString(cursor.getColumnIndex(TIMESTART))));
        entry.settimeEnd(Long.parseLong(cursor.getString(cursor.getColumnIndex(TIMEEND))));
        entry.setName(cursor.getString(cursor.getColumnIndex(NAME)));
        entry.setminHeight(Long.parseLong(cursor.getString(cursor.getColumnIndex(MINHEIGHT))));
        entry.setmaxHeight(Long.parseLong(cursor.getString(cursor.getColumnIndex(MAXHEIGHT))));
        entry.setpointCount(Integer.parseInt(cursor.getString(cursor.getColumnIndex(POINTCOUNT))));
        entry.setfileSent(Integer.parseInt(cursor.getString(cursor.getColumnIndex(FILESENT))));
        return entry;
    }

    private static Float getNullableFloat(Cursor cursor, String colName) {
        Float retValue = null;
        int colIndex = cursor.getColumnIndexOrThrow(colName);
        if (!cursor.isNull(colIndex)) {
            retValue = cursor.getFloat(colIndex);
        }
        return retValue;
    }

    public File getFile(String ext)
    {
        // Verify if Folder exists
        File sd = new File(Environment.getExternalStorageDirectory() + Config.getGpsLoggerFolder());
        boolean success = true;
        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (!success) return null;

        File file = new File(sd,  getFileName() + "." + ext);
        if (!file.exists()) return null;

        return file;
    }

    public Uri getUri(String ext)
    {
        return Uri.fromFile(getFile(ext));
    }

    public String getFullPath(String ext)
    {
        return getFile(ext).getPath();
    }
}
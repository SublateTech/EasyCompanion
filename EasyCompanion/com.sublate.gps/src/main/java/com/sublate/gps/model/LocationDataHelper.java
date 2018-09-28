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

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Location;

import com.bingzer.android.dbv.IDatabase;
import com.sublate.gps.exporters.LocationExtended;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Helper class for writing and retrieving data using the TrackerProvider
 * content provider
 *
 */
public class LocationDataHelper {

    private Context mContext;
    /** formats data output */


    IDatabase db;


    /**
     * Creates instance
     *
     * @param context - content context
     */
    public LocationDataHelper(Context context) {
        LocationDataProvider dataProvider = new LocationDataProvider(context);
        db = dataProvider.openDatabase();

        /*
        mContext = context;
        mFormatter = formatter;
        db = DbQuery.getDatabase(dataBase);
        db.getConfig().setReadOnly(true);
        db.getConfig().setAppendTableNameForId(true);
        db.open(1, dataBase, new SQLiteBuilder.WithoutModeling(mContext.getApplicationContext()));
        */

    }

    /**
     * insert given Point into content provider
     */
    private void writeEntry(Point entry) {
        if (!routeExists((Date)Calendar.getInstance().getTime(), entry.getScheduleId()))
        {
            Route route = new Route();
            route.setScheduleId(entry.getScheduleId());
            String dateStamp = com.sublate.gps.helper.DateUtils.dateToString1((Date)Calendar.getInstance().getTime(),"yyyy-MM-dd");
            route.setdateStamp(dateStamp);
            long newRouteId = db.from("route").insert(route.getAsContentValues()).query();
        }

        long newTrackingId = db.from("Point").insert(entry.getAsContentValues()).query();
        updateRouteStatistics((Date)Calendar.getInstance().getTime(), entry.getScheduleId());

    }

    /**
     * insert given location into tracker data
     */
    public void writeEntry(Location loc, int scheduleId) {
        if (Point.lastPosition == null)
            Point.lastPosition = getLastPosition((Date)Calendar.getInstance().getTime(), scheduleId);
        writeEntry(Point.createEntry(loc, scheduleId));
    }

    private void updateRouteStatistics(Date date, int ScheduleId)
    {
        Location location = null;
        long result = 0;
        String dateKey = com.sublate.gps.helper.DateUtils.dateToString1(date,"yyyy-MM-dd");
        Cursor cursor = null;
        try {
            cursor =  db.from("Point t")
                    .leftJoin("schedule s","t.ScheduleId=s.id")
                    .select("Date(t.Timestamp) = Date(?) AND t.ScheduleId = ?", dateKey, ScheduleId)
                    .columns("Min(t.LocationTime) as timeStart","Max(t.LocationTime) as timeEnd","count(*) as pointCount","Sum(t.Distance) as distance","s.name as name")
                    .groupBy("Date(t.Timestamp)","t.ScheduleId")
                    .query();
            int size = cursor.getCount();
            if (cursor.moveToNext()) {
                cursor.moveToFirst();

                int recordUpdated = db.from("route")
                        .update("Date(dateStamp) = Date(?) AND ScheduleId = ?", dateKey, ScheduleId)
                        .columns("name","timeStart", "timeEnd","pointCount", "distance")
                        .val(cursor.getString(cursor.getColumnIndex(Route.NAME)),
                                Long.parseLong(cursor.getString(cursor.getColumnIndex(Route.TIMESTART))),
                                Long.parseLong(cursor.getString(cursor.getColumnIndex(Route.TIMEEND))),
                                Integer.parseInt(cursor.getString(cursor.getColumnIndex(Route.POINTCOUNT))),
                                Double.parseDouble(cursor.getString(cursor.getColumnIndex(Route.DISTANCE))))
                        .query();
            }

        }
        catch (SQLException ex)
        {
            String message = ex.getMessage();
        }

        finally {
            if (cursor != null) {
                cursor.close();

            }
        }

    }

    private boolean routeExists(Date date, int ScheduleId)
    {
        boolean result = false;
        String dateKey = com.sublate.gps.helper.DateUtils.dateToString1(date,"yyyy-MM-dd");
        Cursor cursor = null;
        try {
            cursor =  db.from("route").select("Date(dateStamp) = Date(?) AND ScheduleId = ?", dateKey, ScheduleId).query();
            result =  cursor.moveToNext();
        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
        return result;
    }

    public Route getRoute(Date date, int ScheduleId)
    {
        String dateKey = com.sublate.gps.helper.DateUtils.dateToString1(date,"yyyy-MM-dd");
        Route route = new Route();
        route.setScheduleId(ScheduleId);

        Cursor cursor = null;
        try {
            cursor =  db.from("route").select("Date(dateStamp) = Date(?) AND ScheduleId = ?", dateKey, ScheduleId).query();
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        route.setId(cursor.getLong(cursor.getColumnIndex("id")));
                        route.setScheduleId(cursor.getInt(cursor.getColumnIndex("ScheduleId")));
                        route.setdateStamp(cursor.getString(cursor.getColumnIndex("dataStamp")));
                        route.settimeStart(cursor.getLong(cursor.getColumnIndex("timeStart")));
                        route.settimeEnd(cursor.getLong(cursor.getColumnIndex("timeEnd")));
                        route.setName(cursor.getString(cursor.getColumnIndex("name")));
                        return route;
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return route;
    }

    public Route getRoute(long routeId)
    {

        Route route = null;
        Cursor cursor = null;

        try {
            cursor =  db.from("route").select("id = ?", routeId).query();
            int size = cursor.getCount();
            if (cursor.moveToNext()) {
                cursor.moveToFirst();
                route = Route.createEntry(cursor);
            }

        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
        return route;
    }

    public Schedule getSchedule(long routeId)
    {
        Schedule route = null;
        Cursor cursor = null;
        try {
            cursor =  db.from("schedule").select("id = ?", routeId).query();
            int size = cursor.getCount();
            if (cursor.moveToNext()) {
                cursor.moveToFirst();
                route = Schedule.createEntry(cursor);
            }

        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
        return route;
    }

    private Location  getLastPosition(Date date, int ScheduleId)
    {
        Location location = null;
        long result = 0;
        String dateKey = com.sublate.gps.helper.DateUtils.dateToString1(date,"yyyy-MM-dd");
        Cursor cursor = null;
        try {
            //cursor =  db.from("Point").select("Date(Timestamp) = Date(?) AND ScheduleId = ? AND Distance > 0", dateKey, ScheduleId).columns("*", "MAX(_id) as id").query();
            cursor =  db.from("Point").select(1, "Date(Timestamp) = Date(?) AND ScheduleId = ? AND Distance > 0", dateKey, ScheduleId).orderBy("_id DESC").query();
            int size = cursor.getCount();
            if (cursor.moveToNext()) {
                cursor.moveToFirst();
                location = Point.createLocation(cursor);
            }

        }
        catch (SQLException ex)
        {
            String message = ex.getMessage();
        }

        finally {
            if (cursor != null) {
                cursor.close();

            }
        }
        return location;
    }

    public String getEmployeeName(String empNo) {

        /*
        Cursor cursor = null;
        String empName = "";
        try{

            cursor = SQLiteDatabaseInstance_.rawQuery("SELECT EmployeeName FROM Employee WHERE EmpNo=?", new String[] {empNo + ""});

            if(cursor.getCount() > 0) {

                cursor.moveToFirst();
                empName = cursor.getString(cursor.getColumnIndex("EmployeeName"));
            }

            return empName;
        }finally {

            cursor.close();
        }
        */
        return null;
    }



    /**
     * insert given log message into tracker data
     */
    public void writeEntry(String tag, String logMsg) {
        writeEntry(Point.createEntry(tag, logMsg));
    }

    /**
     * Deletes all tracker entries
     */
    public void deleteAll() {
        //mContext.getContentResolver().delete(TrackerDataProvider.CONTENT_URI_TRACKING, null, null);
    }

    /**
     * Query tracker data, filtering by given tag
     *
     * @param tag
     * @return Cursor to data
     */
    public Cursor query(String tag, int limit) {
        /*
        String selection = (tag == null ? null : Point.TAG + "=?");
        String[] selectionArgs = (tag == null ? null : new String[] {tag});
        Cursor cursor = mContext.getContentResolver().query(
                TrackerDataProvider.CONTENT_URI_TRACKING, Point.ATTRIBUTES,
                selection, selectionArgs, null);
        if (cursor == null) {
            return cursor;
        }
        int pos = (cursor.getCount() < limit ? 0 : cursor.getCount() - limit);
        cursor.moveToPosition(pos);
        return cursor;
        */
        return null;
    }

    public Cursor getRouteByDate(String Date)
    {
        Cursor cursor = db.from("route")
                .select("Date(dateStamp)=Date(?)",Date)
                .columns("*")
                .orderBy("dateStamp")
                .query();
        return cursor;
    }

    public List<Route> getListRouteByDate(Date date)
    {
        List<Route> routeList = new ArrayList<Route>();
        String sdate = com.sublate.gps.helper.DateUtils.dateToString1(date,"yyyy-MM-dd");
        Cursor cursor =  getRouteByDate(sdate);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Route route =  new Route();
                    route.setId(cursor.getLong(cursor.getColumnIndex("id")));
                    route.setScheduleId(cursor.getInt(cursor.getColumnIndex("ScheduleId")));
                    route.setdateStamp(cursor.getString(cursor.getColumnIndex("dateStamp")));
                    route.settimeStart(cursor.getLong(cursor.getColumnIndex("timeStart")));
                    route.settimeEnd(cursor.getLong(cursor.getColumnIndex("timeEnd")));
                    route.setName(cursor.getString(cursor.getColumnIndex("name")));
                    route.setpointCount(cursor.getInt(cursor.getColumnIndex("pointCount")));
                    routeList.add(route);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return routeList;
    }

    public Cursor getRouteDetail(String Date, String ScheduleId)
    {
        String[] tableColumns = new String[] {
                "*"
        };
        String whereClause = "Timestamp=?  And  ScheduleId=?";
        String[] whereArgs = new String[] {
                Date,
                ScheduleId
        };
        String orderBy = "Timestamp";
        // cursor = mContext.getContentResolver().query(TrackerDataProvider.CONTENT_URI_TRACKING, tableColumns, whereClause, whereArgs, orderBy);
        Cursor cursor = db.from("Point")
                .select("Date(Timestamp)=Date(?)  And  ScheduleId=?",Date,ScheduleId)
                .columns("*", "_id")
                .orderBy("Timestamp")
                .query();

/*
        if (cursor != null) {

            // move cursor to first row
            if (cursor.moveToFirst()) {
              do {
                    // Get version from Cursor
                    String bookName = cursor.getString(cursor.getColumnIndex("Timestamp"));
                    // add the bookName into the bookTitles ArrayList
                } while (cursor.moveToNext());
            }

            cursor.moveToFirst();
        }
        */


        return cursor;
    }

    /**
     * Retrieves a cursor that starts at the last limit rows
     *
     * @param limit
     * @return a cursor, null if bad things happened
     */
    public Cursor query(int limit) {
        return query(null, limit);
    }

    /**
     * Query tracker data, filtering by given tag. mo limit to number of rows
     * returned
     *
     * @param tag
     * @return Cursor to data
     */
    public Cursor query(String tag) {
        return query(tag, Integer.MAX_VALUE);
    }

    public ArrayList<Schedule> getGetAllSchedulers() {
        String selection = null;
        String[] selectionArgs = null;
        //Cursor cursor = mContext.getContentResolver().query(TrackerDataProvider.CONTENT_URI_SCHEDULE, Schedule.ATTRIBUTES, selection, selectionArgs, null);

        Cursor cursor = db.from("schedule")
                .select()
                .columns("*", "id As _id")
                .orderBy("timeStart")
                .query();

        if (cursor == null) {
            return null;
        }
        ArrayList<Schedule> array = new ArrayList<Schedule>();
        while (cursor.moveToNext()) {
            Schedule Entry = new Schedule();
            Entry.setId(cursor.getInt(cursor.getColumnIndex(Schedule.ID)));
            Entry.setName(cursor.getString(cursor.getColumnIndex(Schedule.NAME)));
            Entry.setTimeStart(cursor.getString(cursor.getColumnIndex(Schedule.TIMESTART)));
            Entry.setTimeEnd(cursor.getString(cursor.getColumnIndex(Schedule.TIMEEND)));
            array.add(Entry);
        }
        cursor.close();
        return array;
    }



    public Cursor getAllRoutes()
    {
        Cursor cursor = db.from("route")
                .select("ScheduleId > 0")
                .columns("id as _id","name","date(dateStamp) as date", "timeStart","timeEnd","distance","pointCount","ScheduleId")
                .orderBy("date(dateStamp) ASC")
                .groupBy("date(dateStamp)","ScheduleId")
                .query();

        return cursor;
    }

    public Cursor getAllSchedules()
    {
        Cursor cursor = db.from("schedule")
                .select()
                .columns("id as _id","name","timeStart","timeEnd","M","T","W","Th","F","S","Su","Active")
                .query();

        return cursor;
    }

    public Cursor getRouteList(Route route) {

        Cursor cursor = getRouteDetail(route.getDateStamp(),String.valueOf(route.getScheduleId()));

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Location lc = new Location("DB");
                    lc.setLatitude(cursor.getDouble(cursor.getColumnIndex("Latitude")));
                    lc.setLongitude(cursor.getDouble(cursor.getColumnIndex("Longitude")));
                    lc.setAltitude(cursor.getDouble(cursor.getColumnIndex("Altitude")));
                    lc.setSpeed(cursor.getFloat(cursor.getColumnIndex("Speed")));
                    lc.setAccuracy(cursor.getFloat(cursor.getColumnIndex("Accuracy")));
                    lc.setBearing(cursor.getFloat(cursor.getColumnIndex("Bearing")));
                    lc.setTime(cursor.getLong(cursor.getColumnIndex("LocationTime")));

                    //LocationExtended extdloc = new LocationExtended(lc);
                    //extdloc.setNumberOfSatellites(1);  //Complete later
                } while (cursor.moveToNext());
            }

        }

        return cursor;
    }

    // Getting a list of Locations associated to a specified track, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<LocationExtended> getLocationsList(Route route) {

        List<LocationExtended> locationList = new ArrayList<LocationExtended>();

        /*
        String selectQuery = "SELECT  * FROM " + TABLE_LOCATIONS + " WHERE "
                + KEY_TRACK_ID + " = " + TrackID + " AND "
                + KEY_LOCATION_NUMBER + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_LOCATION_NUMBER;

        //Log.w("myApp", "[#] DatabaseHandler.java - getLocationList(" + TrackID + ", " + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(selectQuery, null);
        */
        Cursor cursor = getRouteDetail(route.getDateStamp(),String.valueOf(route.getScheduleId()));

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Location lc = new Location("DB");
                    lc.setLatitude(cursor.getDouble(cursor.getColumnIndex("Latitude")));
                    lc.setLongitude(cursor.getDouble(cursor.getColumnIndex("Longitude")));
                    lc.setAltitude(cursor.getDouble(cursor.getColumnIndex("Altitude")));
                    lc.setSpeed(cursor.getFloat(cursor.getColumnIndex("Speed")));
                    lc.setAccuracy(cursor.getFloat(cursor.getColumnIndex("Accuracy")));
                    lc.setBearing(cursor.getFloat(cursor.getColumnIndex("Bearing")));
                    lc.setTime(cursor.getLong(cursor.getColumnIndex("LocationTime")));

                    LocationExtended extdloc = new LocationExtended(lc);
                    extdloc.setNumberOfSatellites(1);  //Complete later

                    locationList.add(extdloc); // Add Location to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return locationList;
    }

    // Getting a list of Locations associated to a specified track, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<LocationExtended> getPlacemarksList(Route route) {

        //List<LocationExtended> placemarkList = new ArrayList<LocationExtended>();

        /*
        String selectQuery = "SELECT  * FROM " + TABLE_PLACEMARKS + " WHERE "
                + KEY_TRACK_ID + " = " + TrackID + " AND "
                + KEY_LOCATION_NUMBER + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_LOCATION_NUMBER;

        //Log.w("myApp", "[#] DatabaseHandler.java - getLocationList(" + TrackID + ", " + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Location lc = new Location("DB");
                    lc.setLatitude(cursor.getDouble(3));
                    lc.setLongitude(cursor.getDouble(4));
                    lc.setAltitude(cursor.getDouble(5));
                    lc.setSpeed(cursor.getFloat(6));
                    lc.setAccuracy(cursor.getFloat(7));
                    lc.setBearing(cursor.getFloat(8));
                    lc.setTime(cursor.getLong(9));

                    LocationExtended extdloc = new LocationExtended(lc);
                    extdloc.setNumberOfSatellites(cursor.getInt(10));
                    extdloc.setDescription(cursor.getString(12));

                    placemarkList.add(extdloc); // Add Location to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        */
        //return placemarkList;
        return getLocationsList(route);
    }


    public void UpdateSchedule(Schedule entry)
    {
            try {
                    int recordUpdated = db.from("schedule")
                            .update("id = ?", entry.getId())
                            .columns("name","timeStart", "timeEnd")
                            .val(entry.getName(),entry.getTimeStart(),entry.getTimeEnd())
                            .query();

            }
            catch (SQLException ex)
            {
                String message = ex.getMessage();
            }

    }
}
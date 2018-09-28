package com.sublate.gps.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.bingzer.android.dbv.DbQuery;
import com.bingzer.android.dbv.IDatabase;
import com.bingzer.android.dbv.SQLiteBuilder;
import com.sublate.gps.dataprovider.DataProvider;


public class LocationDataProvider extends DataProvider {


    private static String dataBase = "/sdcard/tracking.db";

    IDatabase db;
    Context mContext;

    public LocationDataProvider(Context context)
    {
        mContext = context;
    }

    @Override
    public IDatabase openDatabase() {
        int dbVersion = 1;

        // force AsyncTask to be initialized in the login thread due to the bug:
        // http://stackoverflow.com/questions/4280330/onpostexecute-not-being-called-in-asynctask-handler-runtime-exception
        try
        {
            Class.forName("android.os.AsyncTask");
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }

        db = DbQuery.getDatabase(dataBase);

        //      db.getConfig().setReadOnly(true);
        //      db.getConfig().setAppendTableNameForId(true);
        db.open(dbVersion, new SQLiteBuilder() {
            @Override
            public Context getContext() {
                return mContext;
            }
            @Override
            public void onError(Throwable error){
                Log.d("",error.getMessage()); }

            @Override
            public void onModelCreate(IDatabase database, IDatabase.Modeling modeling) {

                /*StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append(String.format("CREATE TABLE %s (", TRACKING_TABLE_NAME));
            Track.buildCreationString(queryBuilder);
            queryBuilder.append(");");
            db.execSQL(queryBuilder.toString());
            db.execSQL(
                    "CREATE TABLE route (id INTEGER PRIMARY KEY AUTOINCREMENT , timeStart INTEGER, distance NUMBER, name String, minHeight NUMBER DEFAULT -1, maxHeight NUMBER DEFAULT -1, timeEnd INTEGER, pointCount INTEGER)");
            db.execSQL(
                    "CREATE TABLE schedule (id INTEGER PRIMARY KEY AUTOINCREMENT , name String, timeStart String, timeEnd String, timeTransfer String)");
            db.execSQL(
                    "INSERT INTO schedule (name, timeStart, timeEnd, timeTransfer) " +
                            "VALUES ('Horario Dia','08:30', '12:00', '12:10')");
            db.execSQL(
                    "INSERT INTO schedule (name, timeStart, timeEnd, timeTransfer) " +
                            "VALUES ('Horario Tarde','13:00', '20:00', '20:10')");
            db.execSQL(
                    "CREATE TABLE waypoint (id INTEGER PRIMARY KEY AUTOINCREMENT , id_route INTEGER, time INTEGER, timeStart INTEGER,timeEnd INTEGER)");
            db.setVersion(DB_VERSION);
            */

                modeling.add("Point")
                        .addPrimaryKey("_id")
                        .add("ScheduleId","INTEGER")
                        .add("Timestamp","STRING")
                        .add("Tag","STRING")
                        .add("Type","STRING")
                        .add("Accuracy","REAL")
                        .add("Latitude","REAL")
                        .add("Longitude","REAL")
                        .add("Altitude","REAL")
                        .add("Speed","REAL")
                        .add("Bearing","REAL")
                        //.add("DistFromNetLocation","REAL")
                        .add("LocationTime","INTEGER")
                        .add("DebugInfo","STRING")
                        .add("Distance","REAL")
                        .ifNotExists();


                modeling.add("route")
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
                        .ifNotExists();


                modeling.add("schedule")
                        .add("id","INTEGER","primary key autoincrement")
                        .add("name","STRING")
                        .add("timeStart","STRING")
                        .add("timeEnd","String")
                        .add("timeTransfer","String")
                        .addInteger("M","Default 1")
                        .addInteger("T","Default 1")
                        .addInteger("W","Default 1")
                        .addInteger("Th","Default 1")
                        .addInteger("F","Default 1")
                        .addInteger("S","Default 0")
                        .addInteger("Su","Default 0")
                        .addInteger("Active","Default 1")
                        .ifNotExists();





            }

        });
                /*
                "INSERT INTO schedule (name, timeStart, timeEnd, timeTransfer) " +
                        "VALUES ('Horario Dia','08:30', '12:00', '12:10')");
                "INSERT INTO schedule (name, timeStart, timeEnd, timeTransfer) " +
                        "VALUES ('Horario Tarde','13:00', '20:00', '20:10')");
                */

        Cursor cursor = db.from("schedule").select().query();
        if (cursor.getCount()==0) {

            ContentValues contentValues = new ContentValues();

            contentValues.put("name", "Horario Dia");
            contentValues.put("timeStart", "08:30");
            contentValues.put("timeEnd", "12:00");
            contentValues.put("timeTransfer", "12:10");
            db.from("schedule").insert(contentValues).query();

            contentValues = new ContentValues();

            contentValues.put("name", "Horario Tarde");
            contentValues.put("timeStart", "13:00");
            contentValues.put("timeEnd", "22:00");
            contentValues.put("timeTransfer", "22:10");
            db.from("schedule").insert(contentValues).query();
        }
        cursor.close();


        return db;
    }

    @Override
    public String getAuthority() {
        return "com.sublate.gpstracker.dataprovider";
    }
}

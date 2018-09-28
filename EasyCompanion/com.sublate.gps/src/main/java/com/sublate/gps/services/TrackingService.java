package com.sublate.gps.services;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;


import com.sublate.gps.Config;
import com.sublate.gps.exporters.gpx.ExportRouteTask;
import com.sublate.gps.helper.Utilities;
import com.sublate.gps.location.BestLocationListener;
import com.sublate.gps.location.BestLocationProvider;
import com.sublate.gps.model.Route;
import com.sublate.gps.model.LocationDataHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.sublate.gps.Config.LOGTAG;


public class TrackingService extends CoreService {
    private static String START_SERVICE = "com.sublate.service.TrackingService.startService";
    private static String STOP_SERVICE = "com.sublate.service.TrackingService.stopService";
    public  static String SAVE_ROUTES = "com.sublate.service.TrackingService.saveRoutes";

    private BestLocationProvider mLocationProvider;
    private Listener mLocationListener;

    private static long m_currentSchedule = 0;
    private static  Location m_lastLocation;

    public static void startService(Context context, long scheduleId) {
        Intent i = new Intent();
        i.setClass(context, TrackingService.class);
        i.setAction(TrackingService.START_SERVICE);
        i.putExtra("ScheduleId",scheduleId);
        addWakeLock(context, i);
        context.startService(i);
    }

    public static void stopService(Context context) {
        Intent i = new Intent();
        i.setClass(context, TrackingService.class);
        i.setAction(TrackingService.STOP_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Config.GPSDataBase == null)
            Config.GPSDataBase = new LocationDataHelper(this);
        setAutoShutdown(false);
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (START_SERVICE.equals(intent.getAction())) {

            long mScheduleId = intent.getLongExtra("ScheduleId",0);
            m_currentSchedule = mScheduleId;
            if (Config.DEBUG)
                Log.d(LOGTAG,"TrackingService started with startId = "+ startId);
            mLocationProvider = new BestLocationProvider(this, true, true, 0, 0, 0, 0);
            mLocationListener = new Listener();
            mLocationProvider.setJustOne(true);

            if (mLocationListener != null) {
                if (Config.DEBUG)
                    Log.d(LOGTAG,"***** TrackingService *****: starting new check");
                mLocationListener.setStartId(startId);
                mLocationListener.wakeLockAcquire();
                mLocationProvider.startLocationUpdatesWithListener(mLocationListener);

            } else {
                if (Config.DEBUG)
                    Log.d(LOGTAG,"***** TrackingService *****: renewing WakeLock");
                mLocationListener.setStartId(startId);
                mLocationListener.wakeLockAcquire();
            }
        } else if (STOP_SERVICE.equals(intent.getAction())) {
            if (Config.DEBUG)
                Log.d(LOGTAG,"TrackingService stopping");
            stopSelf();
        }  else if (SAVE_ROUTES.equals(intent.getAction())) {

            final String fromJid = intent.getStringExtra("fromJid");
            final String toJid = intent.getStringExtra("toJid");
            execute(getApplication(), new Runnable() {
                @Override
                public void run() {
                    exportRoutes(fromJid, toJid);
                }
            }, Config.LOCATION_SERVICE_WAKE_LOCK_TIMEOUT, startId);


            if (Config.DEBUG)
                Log.d(LOGTAG,"TrackingService saving Routes");
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    class Listener extends BestLocationListener {

        private TracingPowerManager.TracingWakeLock wakeLock = null;
        private int startId = -1;

        // wakelock strategy is to be very conservative.  If there is any reason to release, then release
        // don't want to take the chance of running wild
        public synchronized void wakeLockAcquire() {
            TracingPowerManager.TracingWakeLock oldWakeLock = wakeLock;

            TracingPowerManager pm = TracingPowerManager.getPowerManager(TrackingService.this);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PollService wakeLockAcquire");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(Config.WAKE_LOCK_TIMEOUT);

            if (oldWakeLock != null) {
                oldWakeLock.release();
            }

        }
        public synchronized void wakeLockRelease() {
            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
        }


        private void release() {

            /*
            MessagingController controller = MessagingController.getInstance(getApplication());
            controller.setCheckMailListener(null);
            MailService.saveLastCheckEnd(getApplication());

            MailService.actionReschedulePoll(PollService.this, null);
            */
            wakeLockRelease();

            if (Config.DEBUG)
                Log.i(LOGTAG, "Tracking stopping with startId = " + startId);
            stopSelf(startId);
        }

        public int getStartId() {
            return startId;
        }
        public void setStartId(int startId) {
            this.startId = startId;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.i(LOGTAG, "onStatusChanged PROVIDER:" + provider + " STATUS:" + String.valueOf(status));

        }

        @Override
        public void onProviderEnabled(String provider) {
            //Log.i(LOGTAG, "onProviderEnabled PROVIDER:" + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //Log.i(LOGTAG, "onProviderDisabled PROVIDER:" + provider);
        }

        @Override
        public void onLocationUpdateTimeoutExceeded(BestLocationProvider.LocationType type) {
            //	Log.w(LOGTAG, "onLocationUpdateTimeoutExceeded PROVIDER:" + type);
        }

        @Override
        public void onLocationUpdate(Location location, BestLocationProvider.LocationType type,
                                     boolean isFresh) {
            Log.d(LOGTAG,"\n\n" + "\nTIME:" + new Date().toLocaleString() + "\nLOCATION UPDATE: isFresh:" + String.valueOf(isFresh) + "\n" + BestLocationProvider.locationToString(location));
            saveEntryLocation(location);
            release();

        }
    }

    public static void saveEntryLocation(Location location)
    {
        m_lastLocation = location;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Config.GPSDataBase.writeEntry(m_lastLocation, (int) m_currentSchedule);
            }
        });

    }

    private void exportRoutes(final String fromJid, final String toJid) {

        List<Route> routes = Config.GPSDataBase.getListRouteByDate(new Date());

        for (final Route route : routes) {
            new ExportRouteTask(this, route.getId()) {

                @Override
                public void OnFailure() {

                }

                @Override
                public void OnComplete() {
                    Log.d(LOGTAG, "Exported File: " + route.getFullPath("gpx"));

                    //TrackingService.startService(this, mScheduleId);
                    Intent intent = new Intent(); //itself
                    intent.setClassName(TrackingService.this, "eu.siacs.conversations.services.XmppConnectionService");
                    intent.setAction("com.sublate.com.ACTION_GEOLOC_SEND_ROUTE_FILES");
                    intent.putExtra("fromJid",fromJid);
                    intent.putExtra("toJid",toJid);
                    intent.putExtra("Uri", route.getFullPath("gpx"));
                    //sendBroadcast(intent);
                    startService(intent);

                }
            }.execute();
        }

        /*

        final String currentFileName = ""; //Session.getCurrentFileName();
        //tracer.info("Sending file " + currentFileName);

        File gpxFolder = new File(Config.getGpsLoggerFolder());

        if (Utilities.GetFilesInFolder(gpxFolder).length < 1) {
            //callback.OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(Utilities.GetFilesInFolder(gpxFolder, new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0) {
            //callback.OnFailure();
            return;
        }
        */

    }
}

package com.sublate.gps.services;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import com.sublate.gps.Config;
import com.sublate.scheduleprovider.Manager;
import com.sublate.scheduleprovider.model.Event;
import com.sublate.scheduleprovider.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;



public class SchedulerService extends CoreService {

    private static final String ACTION_LOCATION_SERVICE = "com.sublate.intent.action.GET_LOCATION_WAKEUP";
    private static final String ACTION_SERVICE_RESET = "com.sublate.gps.intent.action.SERVICE_RESET";
    private static final String ACTION_CANCEL = "com.sublate.gps.intent.action.MAIL_SERVICE_CANCEL";
    private static final String CONNECTIVITY_CHANGE = "com.sublate.gps.intent.action.MAIL_SERVICE_CONNECTIVITY_CHANGE";
    private static final String CANCEL_CONNECTIVITY_NOTICE = "com.sublate.gps.intent.action.MAIL_SERVICE_CANCEL_CONNECTIVITY_NOTICE";

    private static long nextCheck = -1;

    public static final int UPDATE_LOCATION_TIME_PERIOD = 60*1000*10; //10 mins

    private Manager m_scheduleMgr;

    public static void actionReset(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, SchedulerService.class);
        i.setAction(SchedulerService.ACTION_SERVICE_RESET);
        addWakeLockId(context, i, wakeLockId, true);
        context.startService(i);
    }

    public static void actionCancel(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, SchedulerService.class);
        i.setAction(SchedulerService.ACTION_CANCEL);
        addWakeLockId(context, i, wakeLockId, false); // CK:Q: why should we not create a wake lock if one is not already existing like for example in actionReschedulePoll?
        context.startService(i);
    }

    public static void connectivityChange(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, SchedulerService.class);
        i.setAction(SchedulerService.CONNECTIVITY_CHANGE);
        addWakeLockId(context, i, wakeLockId, false); // CK:Q: why should we not create a wake lock if one is not already existing like for example in actionReschedulePoll?
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Config.DEBUG)
            Log.v(Config.LOGTAG, "***** SchedulerService *****: onCreate");
        initSchedules();

    }

    private void initSchedules() {
        m_scheduleMgr = Manager.getInstance(getApplicationContext());
        //m_scheduleMgr.setCallback(new MainActivity.MySAMCallback(), false);
        m_scheduleMgr.suspendCallbacks();
        m_scheduleMgr.init();
        m_scheduleMgr.setBroadcastReceiver(false);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
        if (Config.DEBUG)
            Log.i("Tracker Service", "init Schedule:currentTime " + sdf.format(Calendar.getInstance().getTime()));

        //Log.i(TAG,"startTime " + sdf.format(startTime.getTime()));
        //Log.i(TAG,"startTime " + sdf.format(startTime.getTime()));
        //Log.i(TAG, startTime.);

        //if (Config.DEBUG)
        {
            Schedule.resetScheduleId();

            Schedule mSchedule = new Schedule("08:00", "12:30", "M", 0);
            m_scheduleMgr.addSchedule(mSchedule);

            mSchedule = new Schedule("13:00", "21:00", "M", 0);
            m_scheduleMgr.addSchedule(mSchedule);
        }
        m_scheduleMgr.resumeCallbacks();

    }

    @Override
    public int startService(Intent intent, int startId) {
        long startTime = System.currentTimeMillis();
        boolean doBackground = true;

        final boolean hasConnectivity = true; //Utility.hasConnectivity(getApplication());

        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "SchedulerService.onStart(" + intent + ", " + startId
                  + "), hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);

        if (ACTION_LOCATION_SERVICE.equals(intent.getAction())) {
            long mScheduleId = intent.getLongExtra("ScheduleId", 0);

            if (hasConnectivity && doBackground) {
                TrackingService.startService(this, mScheduleId);
            }

            // if (Config.DEBUG)
            //Log.i(Config.LOGTAG, "***** TrackingService *****: gettting location");

            rescheduleTrackingInBackground(hasConnectivity, doBackground, startId, false);
        } else if (ACTION_CANCEL.equals(intent.getAction())) {
            if (Config.DEBUG)
                Log.v(Config.LOGTAG, "***** SchedulerService *****: cancel");
            cancel();
        } else if (ACTION_SERVICE_RESET.equals(intent.getAction())) {
            if (Config.DEBUG)
                Log.v(Config.LOGTAG, "***** SchedulerService *****: reschedule");
            rescheduleAllInBackground(hasConnectivity, doBackground, startId);

        } else if (CONNECTIVITY_CHANGE.equals(intent.getAction())) {
            rescheduleAllInBackground(hasConnectivity, doBackground, startId);
            if (Config.DEBUG)
                Log.i(Config.LOGTAG, "Got connectivity action with hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);
        } else if (CANCEL_CONNECTIVITY_NOTICE.equals(intent.getAction())) {
            /* do nothing */
        }

        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "SchedulerService.onStart took " + (System.currentTimeMillis() - startTime) + "ms");

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Config.DEBUG)
            Log.v(Config.LOGTAG, "***** SchedulerService *****: onDestroy()");
        super.onDestroy();

    }

    private void cancel() {
        Intent i = new Intent();
        i.setClassName(getApplication().getPackageName(), "com.sublate.gps.service.SchedulerService");
        i.setAction(ACTION_LOCATION_SERVICE);
        BootReceiver.cancelIntent(this, i);
    }

    private void rescheduleAllInBackground(final boolean hasConnectivity,
            final boolean doBackground, Integer startId) {

        execute(getApplication(), new Runnable() {
            @Override
            public void run() {
                rescheduleTracking(hasConnectivity, doBackground, true);
                //rescheduleFtp(hasConnectivity, doBackground, true);
            }
        }, Config.LOCATION_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void rescheduleTrackingInBackground(final boolean hasConnectivity,
                                                final boolean doBackground, Integer startId, final boolean considerLastCheckEnd) {

        execute(getApplication(), new Runnable() {
            public void run() {
                rescheduleTracking(hasConnectivity, doBackground, considerLastCheckEnd);
            }
        }, Config.LOCATION_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void rescheduleTracking(final boolean hasConnectivity, final boolean doBackground,
                               boolean considerLastCheckEnd) {

        Event event = m_scheduleMgr.getNextScheduledEvent(UPDATE_LOCATION_TIME_PERIOD); //Preferences.Gps.getGpsUpdates()); //Set First 10 Seconds
        nextCheck = event.getAlarmTime().getTimeInMillis();

        try {
            Log.i(Config.LOGTAG, "Next check for tracking " +
                    getApplication().getPackageName() + " scheduled for " +
                    new Date(nextCheck));
        } catch (Exception e) {
            // I once got a NullPointerException deep in new Date();
            Log.e(Config.LOGTAG, "Exception while logging", e);
        }

        Intent i = new Intent(this, SchedulerService.class); //itself
        //i.setClassName(getApplication().getPackageName(), getClass().getName());
        i.setAction(ACTION_LOCATION_SERVICE);

        //i.putExtra("EventType",event.getEventType());
        i.putExtra("ScheduleId", event.getScheduleID());
        BootReceiver.scheduleIntent(SchedulerService.this, nextCheck, i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Unused
        return null;
    }

}

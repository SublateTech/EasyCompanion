
package com.sublate.gps.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

import com.sublate.gps.Config;

import java.util.Date;


public class BootReceiver extends CoreReceiver {

    public static String FIRE_INTENT = "com.sublate.easycompanion.fireIntent";
    public static String SCHEDULE_INTENT = "com.sublate.easycompanion.scheduleIntent";
    public static String CANCEL_INTENT = "com.sublate.easycompanion.cancelIntent";

    public static final String ALARMED_INTENT = "com.sublate.easycompanion.service.BroadcastReceiver.pendingIntent";
    public static String AT_TIME = "com.sublate.chat.service.BroadcastReceiver.atTime";


    @Override
    public Integer receive(Context context, Intent intent, Integer tmpWakeLockId) {
        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "BootReceiver.onReceive" + intent);

        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            //Config.setServicesEnabled(context, tmpWakeLockId);
            //tmpWakeLockId = null;
        }else if (FIRE_INTENT.equals(action)) {
            Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
            String alarmedAction = alarmedIntent.getAction();

            if (Config.DEBUG)
                Log.i(Config.LOGTAG, "BootReceiver Got alarm to fire alarmedIntent " + alarmedAction);
            alarmedIntent.putExtra(WAKE_LOCK_ID, tmpWakeLockId);
            tmpWakeLockId = null;
            context.startService(alarmedIntent);
        }else if (SCHEDULE_INTENT.equals(action)) {
            long atTime = intent.getLongExtra(AT_TIME, -1);
            Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
            long mScheduleId = alarmedIntent.getLongExtra("ScheduleId", 0);
            if (Config.DEBUG)
                Log.i(Config.LOGTAG, "BootReceiver Scheduling intent " + alarmedIntent + " for " + new Date(atTime) + " ScheduleId="+mScheduleId);

            int mRequestId = 1;
            PendingIntent pi = buildPendingIntent(context, intent, mRequestId);
            //AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            GpsAlarmManager alarmMgr = GpsAlarmManager.getAlarmManager(context);

            alarmMgr.set(AlarmManager.RTC_WAKEUP, atTime, pi);
        }else if (CANCEL_INTENT.equals(action)) {
            Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
            if (Config.DEBUG)
                Log.i(Config.LOGTAG, "BootReceiver Canceling alarmedIntent " + alarmedIntent);

            int mRequestId = 1;
            PendingIntent pi = buildPendingIntent(context, intent, mRequestId);

            //AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            GpsAlarmManager alarmMgr = GpsAlarmManager.getAlarmManager(context);
            alarmMgr.cancel(pi);
            pi.cancel();
        }else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            //MailService.connectivityChange(context, tmpWakeLockId);
            tmpWakeLockId = null;
            return tmpWakeLockId;
        }

        return tmpWakeLockId;
    }

    private PendingIntent buildPendingIntent(Context context, Intent intent, int requestId) {
        Intent alarmedIntent = intent.getParcelableExtra(ALARMED_INTENT);
        String alarmedAction = alarmedIntent.getAction();

        Intent i = new Intent(context, BootReceiver.class);
        i.setAction(FIRE_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        Uri uri = Uri.parse("action://" + alarmedAction);
        i.setData(uri);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestId, i, 0);
        return pi;
    }

    public static void fireIntent(Context context, Intent alarmedIntent) {
        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "BootReceiver Got request to schedule alarmedIntent " + alarmedIntent.getAction());
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(FIRE_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        context.sendBroadcast(i);
    }

    public static void scheduleIntent(Context context, long atTime, Intent alarmedIntent) {
        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "BootReceiver Got request to schedule alarmedIntent " + alarmedIntent.getAction());
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(SCHEDULE_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        i.putExtra(AT_TIME, atTime);
        context.sendBroadcast(i);
    }

    public static void cancelIntent(Context context, Intent alarmedIntent) {
        if (Config.DEBUG)
            Log.i(Config.LOGTAG, "BootReceiver Got request to cancel alarmedIntent " + alarmedIntent.getAction());
        Intent i = new Intent();
        i.setClass(context, BootReceiver.class);
        i.setAction(CANCEL_INTENT);
        i.putExtra(ALARMED_INTENT, alarmedIntent);
        context.sendBroadcast(i);
    }

    /**
     * Cancel any scheduled alarm.
     *
     * @param context
     */
    public static void purgeSchedule(final Context context) {
        final GpsAlarmManager alarmService = GpsAlarmManager.getAlarmManager(context);
        alarmService.cancel(PendingIntent.getBroadcast(context, 0, new Intent() {
            @Override
            public boolean filterEquals(final Intent other) {
                // we want to match all intents
                return true;
            }
        }, 0));
    }

}

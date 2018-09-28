package com.sublate.gps.services;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.sublate.gps.Config;


public class AbstractService extends CoreService {
    private static String START_SERVICE = "com.fsck.k9.service.AbstractService.startService";
    private static String STOP_SERVICE = "com.fsck.k9.service.AbstractService.stopService";

    public static void startService(Context context) {
        Intent i = new Intent();
        i.setClass(context, AbstractService.class);
        i.setAction(AbstractService.START_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    public static void stopService(Context context) {
        Intent i = new Intent();
        i.setClass(context, AbstractService.class);
        i.setAction(AbstractService.STOP_SERVICE);
        addWakeLock(context, i);
        context.startService(i);
    }

    @Override
    public int startService(Intent intent, int startId) {
        int startFlag = START_STICKY;
        if (START_SERVICE.equals(intent.getAction())) {
            Log.d(Config.LOGTAG, "PushService started with startId = "+ startId);
        } else if (STOP_SERVICE.equals(intent.getAction())) {
            Log.d(Config.LOGTAG,"PushService stopping with startId = " + startId);
            stopSelf(startId);
            startFlag = START_NOT_STICKY;
        }

        return startFlag;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setAutoShutdown(false);
    }


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}

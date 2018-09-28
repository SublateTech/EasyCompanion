package com.sublate.test;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.services.XmppConnectionService;
import com.sublate.xmppsensor.xmpp.XmppConnection;
import com.sublate.xmppsensor.xmpp.jid.InvalidJidException;
import com.sublate.xmppsensor.xmpp.jid.Jid;


public class Test extends XmppActivity{
    private static final String TAG = Test.class.getSimpleName();

    @Override
    protected void refreshUiReal() {
    }

    @Override
    void onBackendConnected() {

        Log.e(TAG,"ENtering...");

        Jid jid = null;

        try {
            jid = Jid.fromParts("test", "sublate.org", "phone");
        }catch (InvalidJidException e) {}

        Account account = new Account(jid,"Test");

        account.setPassword("test");

        xmppConnectionService.createConnection(account);
        xmppConnectionService.addAccount(account);

        Intent intent = new Intent(this,XmppConnectionService.class);
        intent.setAction("ui");
        startService(intent);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_map_tracking);

    }

}
package eu.siacs.conversationsui.services;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import eu.siacs.conversationsui.Config;

public class PushMessageReceiver extends GcmListenerService {

	@Override
	public void onMessageReceived(String from, Bundle data) {
		Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction(XmppConnectionService.ACTION_GCM_MESSAGE_RECEIVED);
		intent.replaceExtras(data);
		startService(intent);
	}
}

package com.sublate.core.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sublate.core.Config;
import com.sublate.core.R;
import com.sublate.core.entities.Account;
import com.sublate.core.entities.Conversation;
import com.sublate.core.entities.Message;
import com.sublate.core.services.XmppConnectionService;
import com.sublate.core.utils.CryptoHelper;
import com.sublate.core.utils.ExceptionHandler;
import com.sublate.core.xmpp.jid.InvalidJidException;
import com.sublate.core.xmpp.jid.Jid;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;




//import eu.siacs.conversations.ui.ConversationActivity;

public class ExceptionHelper {
	private static SimpleDateFormat DATE_FORMATs = new SimpleDateFormat("yyyy-MM-dd");
	public static void init(Context context) {
		if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(
					context));
		}
	}





	public static void writeToStacktraceFile(Context context, String msg) {
		try {
			OutputStream os = context.openFileOutput("stacktrace.txt", Context.MODE_PRIVATE);
			os.write(msg.getBytes());
			os.flush();
			os.close();
		} catch (IOException ignored) {
		}
	}
}

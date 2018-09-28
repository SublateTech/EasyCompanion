package com.sublate.xmppsensor.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
//import android.support.v4.app.RemoteInput;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;


import com.sublate.xmppsensor.R;
import com.sublate.xmppsensor.Config;
import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.entities.Conversation;
import com.sublate.xmppsensor.entities.ServiceDiscoveryResult;

import com.sublate.xmppsensor.utils.CryptoHelper;
import com.sublate.xmppsensor.utils.ExceptionHelper;
import com.sublate.xmppsensor.utils.PRNGFixes;
import com.sublate.xmppsensor.xmpp.OnStatusChanged;
import com.sublate.xmppsensor.xmpp.XmppConnection;
import com.sublate.xmppsensor.xmpp.forms.Data;
import com.sublate.xmppsensor.xmpp.jid.Jid;
import com.sublate.xmppsensor.xmpp.stanzas.PresencePacket;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import de.duenndns.ssl.MemorizingTrustManager;

import static com.sublate.xmppsensor.Config.LOGTAG;


public class XmppConnectionService extends Service {


    public static final String ACTION_DISABLE_FOREGROUND = "disable_foreground";
    public static final String ACTION_IDLE_PING = "idle_ping";

    private OnConversationUpdate mOnConversationUpdate = null;
    private OnShowErrorToast mOnShowErrorToast = null;
    private final IBinder mBinder = new XmppConnectionBinder();
    private SecureRandom mRandom;
    private MemorizingTrustManager mMemorizingTrustManager;
    private OnCaptchaRequested mOnCaptchaRequested = null;
    static  PowerManager pm;

    private WakeLock wakeLock;
    private List<Account> accounts = new CopyOnWriteArrayList<>();


    private final List<Conversation> conversations = new CopyOnWriteArrayList<>();
    private final HashSet<Jid> mLowPingTimeoutMode = new HashSet<>();

    private OnAccountUpdate mOnAccountUpdate = null;
    //private NotificationService mNotificationService = new NotificationService(	this);
    private EventReceiver mEventReceiver = new EventReceiver();
    private LruCache<Pair<String,String>,ServiceDiscoveryResult> discoCache = new LruCache<>(20);


    private OpenPgpServiceConnection pgpServiceConnection;

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public boolean isInteractive() {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        final boolean isScreenOn;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            isScreenOn = pm.isScreenOn();
        } else {
            isScreenOn = pm.isInteractive();
        }
        return isScreenOn;
    }

    public void updateMemorizingTrustmanager() {
        final MemorizingTrustManager tm;
        final boolean dontTrustSystemCAs = getPreferences().getBoolean("dont_trust_system_cas", false);
        if (dontTrustSystemCAs) {
            tm = new MemorizingTrustManager(getApplicationContext(), null);
        } else {
            tm = new MemorizingTrustManager(getApplicationContext());
        }
        setMemorizingTrustManager(tm);
    }
    public void setMemorizingTrustManager(MemorizingTrustManager trustManager) {
        this.mMemorizingTrustManager = trustManager;
    }

    @SuppressLint("TrulyRandom")
    @Override
    public void onCreate() {
        ExceptionHelper.init(getApplicationContext());
        PRNGFixes.apply();
        this.mRandom = new SecureRandom();
        updateMemorizingTrustmanager();
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        /*
        this.mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(final String key, final Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        */




        if (!keepForegroundService() ) {
            getPreferences().edit().putBoolean(Config.KEEP_FOREGROUND_SERVICE,true).commit();
            Log.d(LOGTAG,"number of restarts exceeds threshold. enabling foreground service");
        }



        /*
        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactObserver);
        new Thread(new Runnable() {
            @Override
            public void run() {
                fileObserver.startWatching();
            }
        }).start();
        if (Config.supportOpenPgp()) {
            this.pgpServiceConnection = new OpenPgpServiceConnection(getApplicationContext(), "org.sufficientlysecure.keychain", new OpenPgpServiceConnection.OnBound() {
                @Override
                public void onBound(IOpenPgpService2 service) {
                    for (Account account : accounts) {
                        final PgpDecryptionService pgp = account.getPgpDecryptionService();
                        if(pgp != null) {
                            pgp.continueDecryption(true);
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
            this.pgpServiceConnection.bindToService();
        }
        */

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XmppConnectionService");

        toggleForegroundService();
        //updateUnreadCountBadge();
        toggleScreenEventReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scheduleNextIdlePing();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(this.mEventReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent == null ? null : intent.getAction();
        String pushedAccountHash = null;
        boolean interactive = false;
        if (action != null) {
            final Conversation c = findConversationByUuid(intent.getStringExtra("uuid"));
            switch (action) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    if (hasInternetConnection() && Config.RESET_ATTEMPT_COUNT_ON_NETWORK_CHANGE) {
                        resetAllAttemptCounts(true, false);
                    }
                    break;

                case Intent.ACTION_SHUTDOWN:
                    logoutAndSave(true);
                    return START_NOT_STICKY;

                case ACTION_DISABLE_FOREGROUND:
                    getPreferences().edit().putBoolean(Config.KEEP_FOREGROUND_SERVICE, false).commit();
                    toggleForegroundService();
                    break;

                case Intent.ACTION_SCREEN_ON:
                    deactivateGracePeriod();

                case Intent.ACTION_SCREEN_OFF:
                    if (awayWhenScreenOff()) {
                        refreshAllPresences();
                    }
                    break;

                case ACTION_IDLE_PING:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        scheduleNextIdlePing();
                    }
                    break;


            }
        }
        synchronized (this) {
        //    this.wakeLock.acquire();
            boolean pingNow = ConnectivityManager.CONNECTIVITY_ACTION.equals(action);
            HashSet<Account> pingCandidates = new HashSet<>();


                for (Account account : accounts) {
                    pingNow |= processAccountState(account,
                            interactive,
                            "ui".equals(action),
                            CryptoHelper.getAccountFingerprint(account).equals(pushedAccountHash),
                            pingCandidates);
                }

                if (pingNow) {
                    for (Account account : pingCandidates) {
                        final boolean lowTimeout = mLowPingTimeoutMode.contains(account.getJid().toBareJid());
                        account.getXmppConnection().sendPing();
                        Log.d(LOGTAG, account.getJid().toBareJid() + " send ping (action=" + action + ",lowTimeout=" + Boolean.toString(lowTimeout) + ")");
                        scheduleWakeUpCall(lowTimeout ? Config.LOW_PING_TIMEOUT : Config.PING_TIMEOUT, account.getUuid().hashCode());
                    }
                }


            if (wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (final RuntimeException ignored) {
                }
            }
        }
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void addAccount(Account account)
    {
        accounts.add(account);
    }

    private OnStatusChanged statusListener = new OnStatusChanged() {

        @Override
        public void onStatusChanged(final Account account) {
            XmppConnection connection = account.getXmppConnection();
            if (mOnAccountUpdate != null) {
                mOnAccountUpdate.onAccountUpdate();
            }
            if (account.getStatus() == Account.State.ONLINE) {
                synchronized (mLowPingTimeoutMode) {
                    if (mLowPingTimeoutMode.remove(account.getJid().toBareJid())) {
                        Log.d(LOGTAG, account.getJid().toBareJid() + ": leaving low ping timeout mode");
                    }
                }
                if (account.setShowErrorNotification(true)) {
                    //databaseBackend.updateAccount(account);
                }
                /*
                mMessageArchiveService.executePendingQueries(account);
                if (connection != null && connection.getFeatures().csi()) {
                    if (checkListeners()) {
                        Log.d(LOGTAG, account.getJid().toBareJid() + " sending csi//inactive");
                        connection.sendInactive();
                    } else {
                        Log.d(LOGTAG, account.getJid().toBareJid() + " sending csi//active");
                        connection.sendActive();
                    }
                }
                */
                List<Conversation> conversations = getConversations();
                for (Conversation conversation : conversations) {
                    if (conversation.getAccount() == account
                            && !account.pendingConferenceJoins.contains(conversation)) {
                        if (!conversation.startOtrIfNeeded()) {
                            Log.d(LOGTAG,account.getJid().toBareJid()+": couldn't start OTR with "+conversation.getContact().getJid()+" when needed");
                        }
                        //sendUnsentMessages(conversation);
                    }
                }
                for (Conversation conversation : account.pendingConferenceLeaves) {
                    //leaveMuc(conversation);
                }
                account.pendingConferenceLeaves.clear();
                for (Conversation conversation : account.pendingConferenceJoins) {
                    //joinMuc(conversation);
                }
                account.pendingConferenceJoins.clear();
                scheduleWakeUpCall(Config.PING_MAX_INTERVAL, account.getUuid().hashCode());
            } else {
                if (account.getStatus() == Account.State.OFFLINE || account.getStatus() == Account.State.DISABLED) {
                    resetSendingToWaiting(account);
                    if (!account.isOptionSet(Account.OPTION_DISABLED)) {
                        synchronized (mLowPingTimeoutMode) {
                            if (mLowPingTimeoutMode.contains(account.getJid().toBareJid())) {
                                Log.d(LOGTAG, account.getJid().toBareJid() + ": went into offline state during low ping mode. reconnecting now");
                                reconnectAccount(account, true, false);
                            } else {
                                int timeToReconnect = mRandom.nextInt(10) + 2;
                                scheduleWakeUpCall(timeToReconnect, account.getUuid().hashCode());
                            }
                        }
                    }
                } else if (account.getStatus() == Account.State.REGISTRATION_SUCCESSFUL) {
                    //databaseBackend.updateAccount(account);
                    reconnectAccount(account, true, false);
                } else if ((account.getStatus() != Account.State.CONNECTING)
                        && (account.getStatus() != Account.State.NO_INTERNET)) {
                    resetSendingToWaiting(account);
                    if (connection != null) {
                        int next = connection.getTimeToNextAttempt();
                        Log.d(LOGTAG, account.getJid().toBareJid()
                                + ": error connecting account. try again in "
                                + next + "s for the "
                                + (connection.getAttempt() + 1) + " time");
                        scheduleWakeUpCall(next, account.getUuid().hashCode());
                    }
                }
            }
            //getNotificationService().updateErrorNotification();
        }
    };

    private boolean processAccountState(Account account, boolean interactive, boolean isUiAction, boolean isAccountPushed, HashSet<Account> pingCandidates) {
        boolean pingNow = false;
        if (!account.isOptionSet(Account.OPTION_DISABLED)) {
            if (!hasInternetConnection()) {
                account.setStatus(Account.State.NO_INTERNET);
                if (statusListener != null) {
                    statusListener.onStatusChanged(account);
                }
            } else {
                if (account.getStatus() == Account.State.NO_INTERNET) {
                    account.setStatus(Account.State.OFFLINE);
                    if (statusListener != null) {
                        statusListener.onStatusChanged(account);
                    }
                }
                if (account.getStatus() == Account.State.ONLINE) {
                    synchronized (mLowPingTimeoutMode) {
                        long lastReceived = account.getXmppConnection().getLastPacketReceived();
                        long lastSent = account.getXmppConnection().getLastPingSent();
                        long pingInterval = isUiAction ? Config.PING_MIN_INTERVAL * 1000 : Config.PING_MAX_INTERVAL * 1000;
                        long msToNextPing = (Math.max(lastReceived, lastSent) + pingInterval) - SystemClock.elapsedRealtime();
                        int pingTimeout = mLowPingTimeoutMode.contains(account.getJid().toBareJid()) ? Config.LOW_PING_TIMEOUT * 1000 : Config.PING_TIMEOUT * 1000;
                        long pingTimeoutIn = (lastSent + pingTimeout) - SystemClock.elapsedRealtime();
                        if (lastSent > lastReceived) {
                            if (pingTimeoutIn < 0) {
                                Log.d(LOGTAG, account.getJid().toBareJid() + ": ping timeout");
                                this.reconnectAccount(account, true, interactive);
                            } else {
                                int secs = (int) (pingTimeoutIn / 1000);
                                this.scheduleWakeUpCall(secs, account.getUuid().hashCode());
                            }
                        } else {
                            pingCandidates.add(account);
                            if (isAccountPushed) {
                                pingNow = true;
                                if (mLowPingTimeoutMode.add(account.getJid().toBareJid())) {
                                    Log.d(LOGTAG, account.getJid().toBareJid() + ": entering low ping timeout mode");
                                }
                            } else if (msToNextPing <= 0) {
                                pingNow = true;
                            } else {
                                this.scheduleWakeUpCall((int) (msToNextPing / 1000), account.getUuid().hashCode());
                                if (mLowPingTimeoutMode.remove(account.getJid().toBareJid())) {
                                    Log.d(LOGTAG, account.getJid().toBareJid() + ": leaving low ping timeout mode");
                                }
                            }
                        }
                    }
                } else if (account.getStatus() == Account.State.OFFLINE) {
                    reconnectAccount(account, true, interactive);
                } else if (account.getStatus() == Account.State.CONNECTING) {
                    long secondsSinceLastConnect = (SystemClock.elapsedRealtime() - account.getXmppConnection().getLastConnect()) / 1000;
                    long secondsSinceLastDisco = (SystemClock.elapsedRealtime() - account.getXmppConnection().getLastDiscoStarted()) / 1000;
                    long discoTimeout = Config.CONNECT_DISCO_TIMEOUT - secondsSinceLastDisco;
                    long timeout = Config.CONNECT_TIMEOUT - secondsSinceLastConnect;
                    if (timeout < 0) {
                        Log.d(LOGTAG, account.getJid() + ": time out during connect reconnecting (secondsSinceLast="+secondsSinceLastConnect+")");
                        account.getXmppConnection().resetAttemptCount(false);
                        reconnectAccount(account, true, interactive);
                    } else if (discoTimeout < 0) {
                        account.getXmppConnection().sendDiscoTimeout();
                        scheduleWakeUpCall((int) Math.min(timeout,discoTimeout), account.getUuid().hashCode());
                    } else {
                        scheduleWakeUpCall((int) Math.min(timeout,discoTimeout), account.getUuid().hashCode());
                    }
                } else {
                    if (account.getXmppConnection().getTimeToNextAttempt() <= 0) {
                        reconnectAccount(account, true, interactive);
                    }
                }
            }
        }
        return pingNow;
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }

    private boolean awayWhenScreenOff() {
        return getPreferences().getBoolean(Config.AWAY_WHEN_SCREEN_IS_OFF, false);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void scheduleNextIdlePing() {
        Log.d(LOGTAG,"schedule next idle ping");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, EventReceiver.class);
        intent.setAction(ACTION_IDLE_PING);
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime()+(Config.IDLE_PING_INTERVAL * 1000),
                PendingIntent.getBroadcast(this,0,intent,0)
        );
    }

    public void refreshAllPresences() {

        boolean includeIdleTimestamp = checkListeners() && broadcastLastActivity();
        for (Account account : getAccounts()) {
            if (!account.isOptionSet(Account.OPTION_DISABLED)) {
                sendPresence(account, includeIdleTimestamp);
            }
        }

    }
    public boolean broadcastLastActivity() {
        return getPreferences().getBoolean("last_activity", false);
    }

    public void sendPresence(final Account account) {
        sendPresence(account, checkListeners() && broadcastLastActivity());
    }

    private void sendPresence(final Account account, final boolean includeIdleTimestamp) {
        /*
        PresencePacket packet;
        if (manuallyChangePresence()) {
            packet =  mPresenceGenerator.selfPresence(account, account.getPresenceStatus());
            String message = account.getPresenceStatusMessage();
            if (message != null && !message.isEmpty()) {
                packet.addChild(new Element("status").setContent(message));
            }
        } else {
            packet = mPresenceGenerator.selfPresence(account, getTargetPresence());
        }
        if (mLastActivity > 0 && includeIdleTimestamp) {
            long since = Math.min(mLastActivity, System.currentTimeMillis()); //don't send future dates
            packet.addChild("idle","urn:xmpp:idle:1").setAttribute("since", AbstractGenerator.getTimestamp(since));
        }
        sendPresencePacket(account, packet);
        */
    }

    private void deactivateGracePeriod() {
        for(Account account : getAccounts()) {
            account.deactivateGracePeriod();
        }
    }

    private void resetAllAttemptCounts(boolean reallyAll, boolean retryImmediately) {
        Log.d(LOGTAG, "resetting all attempt counts");
        for (Account account : accounts) {
            if (account.hasErrorStatus() || reallyAll) {
                final XmppConnection connection = account.getXmppConnection();
                if (connection != null) {
                    connection.resetAttemptCount(retryImmediately);
                }
            }
            if (account.setShowErrorNotification(true)) {
                //databaseBackend.updateAccount(account);
            }
        }
     //   mNotificationService.updateErrorNotification();
    }

    public Conversation findConversationByUuid(String uuid) {
        for (Conversation conversation : getConversations()) {
            if (conversation.getUuid().equals(uuid)) {
                return conversation;
            }
        }
        return null;
    }

    public boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public SecureRandom getRNG() {
        return this.mRandom;
    }

    public List<Conversation> getConversations() {
        return this.conversations;
    }

    public MemorizingTrustManager getMemorizingTrustManager() {
        return this.mMemorizingTrustManager;
    }

    public boolean displayCaptchaRequest(Account account, String id, Data data, Bitmap captcha) {
        if (mOnCaptchaRequested != null) {
            DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
            Bitmap scaled = Bitmap.createScaledBitmap(captcha, (int) (captcha.getWidth() * metrics.scaledDensity),
                    (int) (captcha.getHeight() * metrics.scaledDensity), false);

            mOnCaptchaRequested.onCaptchaRequested(account, id, data, scaled);
            return true;
        }
        return false;
    }

    public class XmppConnectionBinder extends Binder {
        public XmppConnectionService getService() {
            return XmppConnectionService.this;
        }
    }

    public void scheduleWakeUpCall(int seconds, int requestCode) {
        final long timeToWake = SystemClock.elapsedRealtime() + (seconds < 0 ? 1 : seconds + 1) * 1000;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, EventReceiver.class);
        intent.setAction("ping");
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, requestCode, intent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToWake, alarmIntent);
    }

    public PowerManager getPowerManager() {
        return pm;
    }

    public boolean showExtendedConnectionOptions() {
        return false; //getPreferences().getBoolean("show_connection_options", false);
    }

    public void resetSendingToWaiting(Account account) {
        /*
        for (Conversation conversation : getConversations()) {
            if (conversation.getAccount() == account) {
                conversation.findUnsentTextMessages(new Conversation.OnMessageFound() {

                    @Override
                    public void onMessageFound(Message message) {
                        //markMessage(message, Message.STATUS_WAITING);
                    }
                });
            }
        }
        */
    }

    public SharedPreferences getPreferences() {
        Context context = getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public interface OnCaptchaRequested {
        void onCaptchaRequested(Account account,
                                String id,
                                Data data,
                                Bitmap captcha);
    }

    public void logoutAndSave(boolean stop) {
        int activeAccounts = 0;
        /*
        databaseBackend.clearStartTimeCounter(true); // regular swipes don't count towards restart counter
        for (final Account account : accounts) {
            if (account.getStatus() != Account.State.DISABLED) {
                activeAccounts++;
            }
            databaseBackend.writeRoster(account.getRoster());
            if (account.getXmppConnection() != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnect(account, false);
                    }
                }).start();
            }
        }
        */
        if (stop || activeAccounts == 0) {
            Log.d(LOGTAG, "good bye");
            stopSelf();
        }
    }

    private void disconnect(Account account, boolean force) {
        if ((account.getStatus() == Account.State.ONLINE)
                || (account.getStatus() == Account.State.DISABLED)) {
            final XmppConnection connection = account.getXmppConnection();
            if (!force) {
                List<Conversation> conversations = getConversations();
                for (Conversation conversation : conversations) {
                    if (conversation.getAccount() == account) {
                        if (conversation.getMode() == Conversation.MODE_MULTI) {
                            //leaveMuc(conversation, true);
                        } else {
                            if (conversation.endOtrIfNeeded()) {
                                Log.d(LOGTAG, account.getJid().toBareJid()
                                        + ": ended otr session with "
                                        + conversation.getJid());
                            }
                        }
                    }
                }
                sendOfflinePresence(account);
            }
            connection.disconnect(force);
        }
    }

    public void toggleScreenEventReceiver() {
        if (awayWhenScreenOff() && !manuallyChangePresence()) {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(this.mEventReceiver, filter);
        } else {
            try {
                unregisterReceiver(this.mEventReceiver);
            } catch (IllegalArgumentException e) {
                //ignored
            }
        }
    }

    public void toggleForegroundService() {
        if (keepForegroundService()) {
            //startForeground(NotificationService.FOREGROUND_NOTIFICATION_ID, this.mNotificationService.createForegroundNotification());
        } else {
            stopForeground(true);
        }
    }

    private boolean keepForegroundService() {
        return getPreferences().getBoolean(Config.KEEP_FOREGROUND_SERVICE,false);
    }

    public interface OnAccountUpdate {
        void onAccountUpdate();
    }

    public void sendPresencePacket(Account account, PresencePacket packet) {
        XmppConnection connection = account.getXmppConnection();
        if (connection != null) {
            connection.sendPresencePacket(packet);
        }
    }

    private void reconnectAccount(final Account account, final boolean force, final boolean interactive) {
        synchronized (account) {
            XmppConnection connection = account.getXmppConnection();
            if (connection == null) {
                connection = createConnection(account);
                account.setXmppConnection(connection);
            }
            boolean hasInternet = hasInternetConnection();
            if (!account.isOptionSet(Account.OPTION_DISABLED) && hasInternet) {
                if (!force) {
                    disconnect(account, false);
                }
                Thread thread = new Thread(connection);
                connection.setInteractive(interactive);
                connection.prepareNewConnection();
                connection.interrupt();
                thread.start();
                scheduleWakeUpCall(Config.CONNECT_DISCO_TIMEOUT, account.getUuid().hashCode());
            } else {
                disconnect(account, force || account.getTrueStatus().isError() || !hasInternet);
                account.getRoster().clearPresences();
                connection.resetEverything();
                //account.getAxolotlService().resetBrokenness();
                if (!hasInternet) {
                    account.setStatus(Account.State.NO_INTERNET);
                }
            }
        }
    }

    public XmppConnection createConnection(final Account account) {
        final SharedPreferences sharedPref = getPreferences();
        String resource;
        try {
            resource = sharedPref.getString("resource", getString(R.string.default_resource)).toLowerCase(Locale.ENGLISH);
            if (resource.trim().isEmpty()) {
                throw new Exception();
            }
        } catch (Exception e) {
            resource = "conversations";
        }
        account.setResource(resource);
        final XmppConnection connection = new XmppConnection(account, this);
        /*
        connection.setOnMessagePacketReceivedListener(this.mMessageParser);
        connection.setOnStatusChangedListener(this.statusListener);
        connection.setOnPresencePacketReceivedListener(this.mPresenceParser);
        connection.setOnUnregisteredIqPacketReceivedListener(this.mIqParser);
        connection.setOnJinglePacketReceivedListener(this.jingleListener);
        connection.setOnBindListener(this.mOnBindListener);
        connection.setOnMessageAcknowledgeListener(this.mOnMessageAcknowledgedListener);
        connection.addOnAdvancedStreamFeaturesAvailableListener(this.mMessageArchiveService);
        connection.addOnAdvancedStreamFeaturesAvailableListener(this.mAvatarService);
        AxolotlService axolotlService = account.getAxolotlService();
        if (axolotlService != null) {
            connection.addOnAdvancedStreamFeaturesAvailableListener(axolotlService);
        }
        */
        return connection;
    }

    private void sendOfflinePresence(final Account account) {
        Log.d(LOGTAG,account.getJid().toBareJid()+": sending offline presence");
        //sendPresencePacket(account, mPresenceGenerator.sendOfflinePresence(account));
    }

    private boolean manuallyChangePresence() {
        return getPreferences().getBoolean(Config.MANUALLY_CHANGE_PRESENCE, false);
    }

    public ServiceDiscoveryResult getCachedServiceDiscoveryResult(Pair<String, String> key) {
        ServiceDiscoveryResult result = discoCache.get(key);
        if (result != null) {
            return result;
        } else {
            /*
            result = databaseBackend.findDiscoveryResult(key.first, key.second);
            if (result != null) {
                discoCache.put(key, result);
            }
            */
            return result;
        }
    }

    public boolean checkListeners() {
        return (this.mOnAccountUpdate == null
     //           && this.mOnConversationUpdate == null
       //         && this.mOnRosterUpdate == null
                && this.mOnCaptchaRequested == null);
         //       && this.mOnUpdateBlocklist == null
         //       && this.mOnShowErrorToast == null
         //       && this.mOnKeyStatusUpdated == null);
    }

    public void showErrorToastInUi(int resId) {
        if (mOnShowErrorToast != null) {
            mOnShowErrorToast.onShowErrorToast(resId);
        }

    }

    public void updateConversationUi() {
        if (mOnConversationUpdate != null) {
            mOnConversationUpdate.onConversationUpdate();
        }
    }

    public OpenPgpApi getOpenPgpApi() {
        if (!Config.supportOpenPgp()) {
            return null;
        } else if (pgpServiceConnection != null && pgpServiceConnection.isBound()) {
            return new OpenPgpApi(this, pgpServiceConnection.getService());
        } else {
            return null;
        }
    }

    public boolean areMessagesInitialized() {
        return false;
    }

    public interface OnConversationUpdate {
        void onConversationUpdate();
    }
    public interface OnShowErrorToast {
        void onShowErrorToast(int resId);
    }
}



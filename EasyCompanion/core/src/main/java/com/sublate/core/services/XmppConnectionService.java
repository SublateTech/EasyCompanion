package com.sublate.core.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;


import com.sublate.core.Config;
import com.sublate.core.R;
import com.sublate.core.entities.Account;
import com.sublate.core.entities.Blockable;
import com.sublate.core.entities.Contact;
import com.sublate.core.entities.Conversation;
import com.sublate.core.entities.Message;
import com.sublate.core.entities.Presence;
import com.sublate.core.entities.Roster;
import com.sublate.core.entities.ServiceDiscoveryResult;
import com.sublate.core.generator.IqGenerator;
import com.sublate.core.generator.MessageGenerator;
import com.sublate.core.parser.IqParser;
import com.sublate.core.parser.PresenceParser;
import com.sublate.core.ui.ExceptionHelper;
import com.sublate.core.utils.CryptoHelper;
import com.sublate.core.utils.PRNGFixes;
import com.sublate.core.utils.Xmlns;
import com.sublate.core.xml.Element;
import com.sublate.core.xmpp.OnBindListener;
import com.sublate.core.xmpp.OnContactStatusChanged;
import com.sublate.core.xmpp.OnIqPacketReceived;
import com.sublate.core.xmpp.OnMessageAcknowledged;
import com.sublate.core.xmpp.OnPresencePacketReceived;
import com.sublate.core.xmpp.OnStatusChanged;
import com.sublate.core.xmpp.OnUpdateBlocklist;
import com.sublate.core.xmpp.XmppConnection;
import com.sublate.core.xmpp.chatstate.ChatState;
import com.sublate.core.xmpp.forms.Data;
import com.sublate.core.xmpp.forms.Field;
import com.sublate.core.xmpp.jid.InvalidJidException;
import com.sublate.core.xmpp.jid.Jid;
import com.sublate.core.xmpp.stanzas.IqPacket;
import com.sublate.core.xmpp.stanzas.MessagePacket;
import com.sublate.core.xmpp.stanzas.PresencePacket;

import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;


import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import de.duenndns.ssl.MemorizingTrustManager;
import eu.siacs.conversations.generator.AbstractGenerator;
import eu.siacs.conversations.generator.PresenceGenerator;


import static com.sublate.core.Config.LOGTAG;


//import android.support.v4.app.RemoteInput;
//import eu.siacs.conversations.ui.SettingsActivity;


public class XmppConnectionService extends Service {


	public static final String STOP_LOCATION_POLLING = "com.sublate.LOCATION_STOP_POLLING";
	public static final String START_LOCATION_POLLING = "com.sublate.LOCATION_START_POLLING";

	public static final String ACTION_COMMAND = "com.sublate.com.ACTION_COMMAND";
	public static final String ACTION_GEOLOC_LOCATION = "com.sublate.com.ACTION_GEOLOC_LOCATION";
	public static final String ACTION_GEOLOC_START = "com.sublate.com.ACTION_GEOLOC_START";
	public static final String ACTION_GEOLOC_STOP = "com.sublate.com.ACTION_GEOLOC_STOP";
	public static final String ACTION_GEOLOC_ONCE = "com.sublate.com.ACTION_GEOLOC_ONCE";
	public static final String ACTION_GEOLOC_GET_ROUTE = "com.sublate.com.ACTION_GEOLOC_GET_ROUTE";
	public static final String ACTION_GEOLOC_SEND_ROUTE_FILES = "com.sublate.com.ACTION_GEOLOC_SEND_ROUTE_FILES";

	public static final String ACTION_COMMAND_CREATE_ORDER = "com.sublate.com.ACTION_COMMAND_CREATE_ORDER";

	public static final String ACTION_REPLY_TO_CONVERSATION = "reply_to_conversations";
	public static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
	public static final String ACTION_DISABLE_FOREGROUND = "disable_foreground";
	public static final String ACTION_DISMISS_ERROR_NOTIFICATIONS = "dismiss_error";
	public static final String ACTION_TRY_AGAIN = "try_again";
	public static final String ACTION_IDLE_PING = "idle_ping";
	private static final String ACTION_MERGE_PHONE_CONTACTS = "merge_phone_contacts";
	public static final String ACTION_GCM_TOKEN_REFRESH = "gcm_token_refresh";
	public static final String ACTION_GCM_MESSAGE_RECEIVED = "gcm_message_received";



	private final IBinder mBinder = new XmppConnectionBinder();
	private final List<Conversation> conversations = new CopyOnWriteArrayList<>();

	private final List<String> mInProgressAvatarFetches = new ArrayList<>();
	private final HashSet<Jid> mLowPingTimeoutMode = new HashSet<>();

	private long mLastActivity = 0;


	private ContentObserver contactObserver = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Intent intent = new Intent(getApplicationContext(),
					XmppConnectionService.class);
			intent.setAction(ACTION_MERGE_PHONE_CONTACTS);
			startService(intent);
		}
	};

	private MemorizingTrustManager mMemorizingTrustManager;

	private PresenceGenerator mPresenceGenerator = new PresenceGenerator(this);
	private MessageGenerator mMessageGenerator = new MessageGenerator(this);
	private final IqGenerator mIqGenerator = new IqGenerator(this);
	private OnPresencePacketReceived mPresenceParser = new PresenceParser(this);



	private IqParser mIqParser = new IqParser(this);
	private OnIqPacketReceived mDefaultIqHandler = new OnIqPacketReceived() {
		@Override
		public void onIqPacketReceived(Account account, IqPacket packet) {
			if (packet.getType() != IqPacket.TYPE.RESULT) {
				Element error = packet.findChild("error");
				String text = error != null ? error.findChildContent("text") : null;
				if (text != null) {
					Log.d(LOGTAG, account.getJid().toBareJid() + ": received iq error - " + text);
				}
			}
		}
	};

	private List<Account> accounts = new ArrayList<Account>();

	public OnContactStatusChanged onContactStatusChanged = new OnContactStatusChanged() {

		@Override
		public void onContactStatusChanged(Contact contact, boolean online) {
			Conversation conversation = find(getConversations(), contact);
			if (conversation != null) {
				if (online) {
					conversation.endOtrIfNeeded();
					if (contact.getPresences().size() == 1) {
						sendUnsentMessages(conversation);
					}
				} else {
					//check if the resource we are haveing a conversation with is still online
					if (conversation.hasValidOtrSession()) {
						String otrResource = conversation.getOtrSession().getSessionID().getUserID();
						if (!(Arrays.asList(contact.getPresences().toResourceArray()).contains(otrResource))) {
							conversation.endOtrIfNeeded();
						}
					}
				}
			}
		}
	};

	//private LocationService mLocationService = new LocationService(this);


	private OnConversationUpdate mOnConversationUpdate = null;

	public boolean indicateReceived() {
		return getPreferences().getBoolean("indicate_received", false);
	}

	private final OnMessageAcknowledged mOnMessageAcknowledgedListener = new OnMessageAcknowledged() {

		@Override
		public void onMessageAcknowledged(Account account, String uuid) {
			for (final Conversation conversation : getConversations()) {
				if (conversation.getAccount() == account) {
					Message message = conversation.findUnsentMessageWithUuid(uuid);
					if (message != null) {
						markMessage(message, Message.STATUS_SEND);
					}
				}
			}
		}
	};
	private int convChangedListenerCount = 0;
	private OnShowErrorToast mOnShowErrorToast = null;
	private int showErrorToastListenerCount = 0;
	private int unreadCount = -1;
	private OnAccountUpdate mOnAccountUpdate = null;
	private OnCaptchaRequested mOnCaptchaRequested = null;
	private int accountChangedListenerCount = 0;
	private int captchaRequestedListenerCount = 0;
	private OnRosterUpdate mOnRosterUpdate = null;
	private OnUpdateBlocklist mOnUpdateBlocklist = null;
	private int updateBlocklistListenerCount = 0;
	private int rosterChangedListenerCount = 0;
	private OnMucRosterUpdate mOnMucRosterUpdate = null;
	private int mucRosterChangedListenerCount = 0;
	//private OnKeyStatusUpdated mOnKeyStatusUpdated = null;
	private int keyStatusUpdatedListenerCount = 0;
	private SecureRandom mRandom;
	private LruCache<Pair<String,String>,ServiceDiscoveryResult> discoCache = new LruCache<>(20);
	private final OnBindListener mOnBindListener = new OnBindListener() {

		@Override
		public void onBind(final Account account) {
			synchronized (mInProgressAvatarFetches) {
				for (Iterator<String> iterator = mInProgressAvatarFetches.iterator(); iterator.hasNext(); ) {
					final String KEY = iterator.next();
					if (KEY.startsWith(account.getJid().toBareJid() + "_")) {
						iterator.remove();
					}
				}
			}
			account.getRoster().clearPresences();
			//mJingleConnectionManager.cancelInTransmission();
			fetchRosterFromServer(account);
			//fetchBookmarks(account);
			sendPresence(account);

			connectMultiModeConversations(account);
			syncDirtyContacts(account);
		}
	};
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
				/*
				if (account.setShowErrorNotification(true)) {
					databaseBackend.updateAccount(account);
				}
				mMessageArchiveService.executePendingQueries(account);
				*/
				if (connection != null && connection.getFeatures().csi()) {
					if (checkListeners()) {
						Log.d(LOGTAG, account.getJid().toBareJid() + " sending csi//inactive");
						connection.sendInactive();
					} else {
						Log.d(LOGTAG, account.getJid().toBareJid() + " sending csi//active");
						connection.sendActive();
					}
				}
				List<Conversation> conversations = getConversations();
				for (Conversation conversation : conversations) {
					if (conversation.getAccount() == account
							&& !account.pendingConferenceJoins.contains(conversation)) {
						if (!conversation.startOtrIfNeeded()) {
							Log.d(LOGTAG,account.getJid().toBareJid()+": couldn't start OTR with "+conversation.getContact().getJid()+" when needed");
						}
						sendUnsentMessages(conversation);
					}
				}
				for (Conversation conversation : account.pendingConferenceLeaves) {
					leaveMuc(conversation);
				}
				account.pendingConferenceLeaves.clear();
				for (Conversation conversation : account.pendingConferenceJoins) {
					joinMuc(conversation);
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

		}
	};

	private WakeLock wakeLock;
	static  PowerManager pm;
	private LruCache<String, Bitmap> mBitmapCache;


	private boolean mRestoredFromDatabase = false;


	public boolean areMessagesInitialized() {
		return true; //this.mRestoredFromDatabase;
	}













	public Conversation find(final Account account, final Jid jid) {
		return find(getConversations(), account, jid);
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
				case ACTION_DISMISS_ERROR_NOTIFICATIONS:
					dismissErrorNotifications();
					break;
				case ACTION_TRY_AGAIN:
					resetAllAttemptCounts(false, true);
					interactive = true;
					break;
				case ACTION_REPLY_TO_CONVERSATION:
					/*
					Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
					if (remoteInput != null && c != null) {
						final CharSequence body = remoteInput.getCharSequence("text_reply");
						if (body != null && body.length() > 0) {
							directReply(c, body.toString(),intent.getBooleanExtra("dismiss_notification",false));
						}
					}
					*/
					break;
				case AudioManager.RINGER_MODE_CHANGED_ACTION:
					if (xaOnSilentMode()) {
						refreshAllPresences();
					}
					break;
				case Intent.ACTION_SCREEN_ON:
					deactivateGracePeriod();
				case Intent.ACTION_SCREEN_OFF:
					if (awayWhenScreenOff()) {
						refreshAllPresences();
					}
					break;
				case ACTION_GCM_TOKEN_REFRESH:
					//refreshAllGcmTokens();
					break;
				case ACTION_IDLE_PING:
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						scheduleNextIdlePing();
					}
					break;
				case ACTION_GCM_MESSAGE_RECEIVED:
					Log.d(LOGTAG,"gcm push message arrived in service. extras="+intent.getExtras());
					pushedAccountHash = intent.getStringExtra("account");
					break;



			}
		}
		synchronized (this) {
			this.wakeLock.acquire();
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

	public boolean isDataSaverDisabled() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			return !connectivityManager.isActiveNetworkMetered()
					|| connectivityManager.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED;
		} else {
			return true;
		}
	}



	private boolean xaOnSilentMode() {
		return getPreferences().getBoolean("xa_on_silent_mode", false);
	}

	private boolean manuallyChangePresence() {
		return getPreferences().getBoolean(Config.MANUALLY_CHANGE_PRESENCE, false);
	}

	private boolean treatVibrateAsSilent() {
		return getPreferences().getBoolean(Config.TREAT_VIBRATE_AS_SILENT, false);
	}

	private boolean awayWhenScreenOff() {
		return getPreferences().getBoolean(Config.AWAY_WHEN_SCREEN_IS_OFF, false);
	}

	private String getCompressPicturesPreference() {
		return getPreferences().getString("picture_compression", "auto");
	}

	private Presence.Status getTargetPresence() {
		if (xaOnSilentMode() && isPhoneSilenced()) {
			return Presence.Status.XA;
		} else if (awayWhenScreenOff() && !isInteractive()) {
			return Presence.Status.AWAY;
		} else {
			return Presence.Status.ONLINE;
		}
	}

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

	private boolean isPhoneSilenced() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		try {
			if (treatVibrateAsSilent()) {
				return audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
			} else {
				return audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
			}
		} catch (Throwable throwable) {
			Log.d(LOGTAG,"platform bug in isPhoneSilenced ("+ throwable.getMessage()+")");
			return false;
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

		}

	}

	private void dismissErrorNotifications() {
		for (final Account account : this.accounts) {
			if (account.hasErrorStatus()) {
				Log.d(LOGTAG,account.getJid().toBareJid()+": dismissing error notification");
				if (account.setShowErrorNotification(false)) {
					//databaseBackend.updateAccount(account);
				}
			}
		}
	}

	public boolean hasInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected();
	}

	public Intent viewConversationIntent;
	public PendingIntent viewConversationPendingIntent;
	public void setViewConversationIntent(Intent viewConversationIntent, PendingIntent viewConversationPendingIntent) {
			this.viewConversationIntent = viewConversationIntent;
			this.viewConversationPendingIntent = viewConversationPendingIntent;
	};


	@SuppressLint("TrulyRandom")
	@Override
	public void onCreate() {
		ExceptionHelper.init(getApplicationContext());
		PRNGFixes.apply();
		this.mRandom = new SecureRandom();
		updateMemorizingTrustmanager();
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;
		this.mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(final String key, final Bitmap bitmap) {
				return bitmap.getByteCount() / 1024;
			}
		};

		//this.databaseBackend = DatabaseBackend.getInstance(getApplicationContext());
		//this.accounts =            databaseBackend.getAccounts();

		/*
		if (!keepForegroundService() && databaseBackend.startTimeCountExceedsThreshold()) {
			getPreferences().edit().putBoolean(Config.KEEP_FOREGROUND_SERVICE,true).commit();
			Log.d(LOGTAG,"number of restarts exceeds threshold. enabling foreground service");
		}
		*/

		//restoreFromDatabase();

		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactObserver);
		/*
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
		updateUnreadCountBadge();
		//toggleScreenEventReceiver();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			scheduleNextIdlePing();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
		//	registerReceiver(this.mEventReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}

		//
		Config.setServicesEnabled(this);
		//scheduleLocation(this);



	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		if (level >= TRIM_MEMORY_COMPLETE) {
			Log.d(LOGTAG, "clear cache due to low memory");
			getBitmapCache().evictAll();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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

	@Override
	public void onTaskRemoved(final Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		if (!keepForegroundService()) {
			this.logoutAndSave(false);
		} else {
			Log.d(LOGTAG,"ignoring onTaskRemoved because foreground service is activated");
		}
	}

	public void logoutAndSave(boolean stop) {
		int activeAccounts = 0;
		//databaseBackend.clearStartTimeCounter(true); // regular swipes don't count towards restart counter
		for (final Account account : accounts) {
			if (account.getStatus() != Account.State.DISABLED) {
				activeAccounts++;
			}
			//databaseBackend.writeRoster(account.getRoster());
			if (account.getXmppConnection() != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						disconnect(account, false);
					}
				}).start();
			}
		}
		if (stop || activeAccounts == 0) {
			Log.d(LOGTAG, "good bye");
			stopSelf();
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
		//connection.setOnMessagePacketReceivedListener(this.mMessageParser);
		connection.setOnStatusChangedListener(this.statusListener);
		connection.setOnPresencePacketReceivedListener(this.mPresenceParser);
		connection.setOnUnregisteredIqPacketReceivedListener(this.mIqParser);
		//connection.setOnJinglePacketReceivedListener(this.jingleListener);
		connection.setOnBindListener(this.mOnBindListener);
		connection.setOnMessageAcknowledgeListener(this.mOnMessageAcknowledgedListener);
		/*
		connection.addOnAdvancedStreamFeaturesAvailableListener(this.mMessageArchiveService);
		connection.addOnAdvancedStreamFeaturesAvailableListener(this.mAvatarService);
		AxolotlService axolotlService = account.getAxolotlService();
		if (axolotlService != null) {
			connection.addOnAdvancedStreamFeaturesAvailableListener(axolotlService);
		}
		*/
		return connection;
	}

	public void sendChatState(Conversation conversation) {
		if (sendChatStates()) {
			MessagePacket packet = mMessageGenerator.generateChatState(conversation);
			sendMessagePacket(conversation.getAccount(), packet);
		}
	}

	public void sendMessage(final Message message) {
		sendMessage(message, false, false);
	}

	private void sendMessage(final Message message, final boolean resend, final boolean delay) {
		final Account account = message.getConversation().getAccount();
		if (account.setShowErrorNotification(true)) {
			//databaseBackend.updateAccount(account);
			//mNotificationService.updateErrorNotification();
		}
		final Conversation conversation = message.getConversation();
		account.deactivateGracePeriod();
		MessagePacket packet = null;
		final boolean addToConversation = (conversation.getMode() != Conversation.MODE_MULTI
				|| account.getServerIdentity() != XmppConnection.Identity.SLACK)
				&& !message.edited();
		boolean saveInDb = addToConversation;
		message.setStatus(Message.STATUS_WAITING);

		if (!resend && message.getEncryption() != Message.ENCRYPTION_OTR) {
			message.getConversation().endOtrIfNeeded();
			message.getConversation().findUnsentMessagesWithEncryption(Message.ENCRYPTION_OTR,
					new Conversation.OnMessageFound() {
						@Override
						public void onMessageFound(Message message) {
							markMessage(message, Message.STATUS_SEND_FAILED);
						}
					});
		}

		if (account.isOnlineAndConnected()) {
			switch (message.getEncryption()) {
				case Message.ENCRYPTION_NONE:
					/*
					if (message.needsUploading()) {
						if (account.httpUploadAvailable(fileBackend.getFile(message,false).getSize())
								|| message.fixCounterpart()) {
							this.sendFileMessage(message, delay);
						} else {
							break;
						}
					} else {
					*/

						packet = mMessageGenerator.generateChat(message);
					//}
					break;
				case Message.ENCRYPTION_PGP:
				case Message.ENCRYPTION_DECRYPTED:
					/*
					if (message.needsUploading()) {
						if (account.httpUploadAvailable(fileBackend.getFile(message,false).getSize())
								|| message.fixCounterpart()) {
							this.sendFileMessage(message, delay);
						} else {
							break;
						}
					} else {
					*/

						//packet = mMessageGenerator.generatePgpChat(message);
					//}
					break;
				case Message.ENCRYPTION_OTR:
					SessionImpl otrSession = conversation.getOtrSession();
					if (otrSession != null && otrSession.getSessionStatus() == SessionStatus.ENCRYPTED) {
						try {
							message.setCounterpart(Jid.fromSessionID(otrSession.getSessionID()));
						} catch (InvalidJidException e) {
							break;
						}
						/*
						if (message.needsUploading()) {
							mJingleConnectionManager.createNewConnection(message);
						} else { */
							//packet = mMessageGenerator.generateOtrChat(message);
						//}
					} else if (otrSession == null) {
						if (message.fixCounterpart()) {
							//conversation.startOtrSession(message.getCounterpart().getResourcepart(), true);
						} else {
							Log.d(LOGTAG,account.getJid().toBareJid()+": could not fix counterpart for OTR message to contact "+message.getContact().getJid());
							break;
						}
					} else {
						Log.d(LOGTAG,account.getJid().toBareJid()+" OTR session with "+message.getContact()+" is in wrong state: "+otrSession.getSessionStatus().toString());
					}
					break;

			}
			if (packet != null) {
				if (account.getXmppConnection().getFeatures().sm()
						|| (conversation.getMode() == Conversation.MODE_MULTI && message.getCounterpart().isBareJid())) {
					message.setStatus(Message.STATUS_UNSEND);
				} else {
					message.setStatus(Message.STATUS_SEND);
				}
			}
		} else {
			switch (message.getEncryption()) {
				case Message.ENCRYPTION_DECRYPTED:
					if (!message.needsUploading()) {
						String pgpBody = message.getEncryptedBody();
						String decryptedBody = message.getBody();
						message.setBody(pgpBody);
						message.setEncryption(Message.ENCRYPTION_PGP);
						if (message.edited()) {
							message.setBody(decryptedBody);
							message.setEncryption(Message.ENCRYPTION_DECRYPTED);
							//databaseBackend.updateMessage(message, message.getEditedId());
							updateConversationUi();
							return;
						} else {
							//databaseBackend.createMessage(message);
							saveInDb = false;
							message.setBody(decryptedBody);
							message.setEncryption(Message.ENCRYPTION_DECRYPTED);
						}
					}
					break;
				case Message.ENCRYPTION_OTR:
					if (!conversation.hasValidOtrSession() && message.getCounterpart() != null) {
						Log.d(LOGTAG,account.getJid().toBareJid()+": create otr session without starting for "+message.getContact().getJid());
						//conversation.startOtrSession(message.getCounterpart().getResourcepart(), false);
					}
					break;

			}
		}

		if (resend) {
			if (packet != null && addToConversation) {
				if (account.getXmppConnection().getFeatures().sm()
						|| (conversation.getMode() == Conversation.MODE_MULTI && message.getCounterpart().isBareJid())) {
					markMessage(message, Message.STATUS_UNSEND);
				} else {
					markMessage(message, Message.STATUS_SEND);
				}
			}
		} else {
			if (addToConversation) {
				conversation.add(message);
			}
			if (message.getEncryption() == Message.ENCRYPTION_NONE || saveEncryptedMessages()) {
				if (saveInDb) {
					//databaseBackend.createMessage(message);
				} else if (message.edited()) {
					//databaseBackend.updateMessage(message, message.getEditedId());
				}
			}
			updateConversationUi();
		}
		if (packet != null) {
			if (delay) {
				mMessageGenerator.addDelay(packet, message.getTimeSent());
			}
			if (conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
				if (this.sendChatStates()) {
					packet.addChild(ChatState.toElement(conversation.getOutgoingChatState()));
				}
			}
			sendMessagePacket(account, packet);
		}
	}

	private void sendUnsentMessages(final Conversation conversation) {
		conversation.findWaitingMessages(new Conversation.OnMessageFound() {

			@Override
			public void onMessageFound(Message message) {
				resendMessage(message, true);
			}
		});
	}

	public void resendMessage(final Message message, final boolean delay) {
		sendMessage(message, true, delay);
	}

	public void fetchRosterFromServer(final Account account) {
		final IqPacket iqPacket = new IqPacket(IqPacket.TYPE.GET);
		if (!"".equals(account.getRosterVersion())) {
			Log.d(LOGTAG, account.getJid().toBareJid()
					+ ": fetching roster version " + account.getRosterVersion());
		} else {
			Log.d(LOGTAG, account.getJid().toBareJid() + ": fetching roster");
		}
		iqPacket.query(Xmlns.ROSTER).setAttribute("ver", account.getRosterVersion());
		sendIqPacket(account, iqPacket, mIqParser);
	}









	public List<Conversation> getConversations() {
		return this.conversations;
	}





	public void populateWithOrderedConversations(final List<Conversation> list) {
		populateWithOrderedConversations(list, true);
	}

	public void populateWithOrderedConversations(final List<Conversation> list, boolean includeNoFileUpload) {
		list.clear();
		if (includeNoFileUpload) {
			list.addAll(getConversations());
		} else {
			for (Conversation conversation : getConversations()) {
				if (conversation.getMode() == Conversation.MODE_SINGLE
						|| conversation.getAccount().httpUploadAvailable()) {
					list.add(conversation);
				}
			}
		}
		try {
			Collections.sort(list);
		} catch (IllegalArgumentException e) {
			//ignore
		}
	}



	public List<Account> getAccounts() {
		return this.accounts;
	}

	public List<Conversation> findAllConferencesWith(Contact contact) {
		ArrayList<Conversation> results = new ArrayList<>();
		for(Conversation conversation : conversations) {
			if (conversation.getMode() == Conversation.MODE_MULTI
					&& conversation.getMucOptions().isContactInRoom(contact)) {
				results.add(conversation);
			}
		}
		return results;
	}

	public Conversation find(final Iterable<Conversation> haystack, final Contact contact) {
		for (final Conversation conversation : haystack) {
			if (conversation.getContact() == contact) {
				return conversation;
			}
		}
		return null;
	}

	public Conversation find(final Iterable<Conversation> haystack, final Account account, final Jid jid) {
		if (jid == null) {
			return null;
		}
		for (final Conversation conversation : haystack) {
			if ((account == null || conversation.getAccount() == account)
					&& (conversation.getJid().toBareJid().equals(jid.toBareJid()))) {
				return conversation;
			}
		}
		return null;
	}



	public Conversation findOrCreateConversation(final Account account, final Jid jid, final boolean muc) {
		synchronized (this.conversations) {
			Conversation conversation = find(account, jid);
			if (conversation != null) {
				return conversation;
			}
			/*
			conversation = databaseBackend.findConversation(account, jid);
			if (conversation != null) {
				conversation.setStatus(Conversation.STATUS_AVAILABLE);
				conversation.setAccount(account);
				if (muc) {
					conversation.setMode(Conversation.MODE_MULTI);
					conversation.setContactJid(jid);
				} else {
					conversation.setMode(Conversation.MODE_SINGLE);
					conversation.setContactJid(jid.toBareJid());
				}
				//conversation.addAll(0, databaseBackend.getMessages(conversation, Config.PAGE_SIZE));
				//this.databaseBackend.updateConversation(conversation);
			} else {

			*/
				String conversationName;
				Contact contact = account.getRoster().getContact(jid);
				if (contact != null) {
					conversationName = contact.getDisplayName();
				} else {
					conversationName = jid.getLocalpart();
				}
				if (muc) {
					conversation = new Conversation(conversationName, account, jid,
							Conversation.MODE_MULTI);
				} else {
					conversation = new Conversation(conversationName, account, jid.toBareJid(),
							Conversation.MODE_SINGLE);
				}
				//this.databaseBackend.createConversation(conversation);
			//}
			if (account.getXmppConnection() != null
					&& account.getXmppConnection().getFeatures().mam()
					&& !muc) {
				/*
				if (query == null) {
					this.mMessageArchiveService.query(conversation);
				} else {
					if (query.getConversation() == null) {
						this.mMessageArchiveService.query(conversation, query.getStart());
					}
				}
				*/
			}
			//checkDeletedFiles(conversation);
			this.conversations.add(conversation);
			updateConversationUi();
			return conversation;
		}
	}



	public void createAccount(final Account account) {
		//account.initAccountServices(this);
		//databaseBackend.createAccount(account);
		this.accounts.add(account);
		this.reconnectAccountInBackground(account);
		updateAccountUi();
	}

	public boolean checkListeners() {
		return (this.mOnAccountUpdate == null
				&& this.mOnConversationUpdate == null
				&& this.mOnRosterUpdate == null
				&& this.mOnCaptchaRequested == null
				&& this.mOnUpdateBlocklist == null
				&& this.mOnShowErrorToast == null);
				//&& this.mOnKeyStatusUpdated == null);
	}

	private void switchToForeground() {
		final boolean broadcastLastActivity = broadcastLastActivity();
		for (Conversation conversation : getConversations()) {
			conversation.setIncomingChatState(ChatState.ACTIVE);
		}
		for (Account account : getAccounts()) {
			if (account.getStatus() == Account.State.ONLINE) {
				account.deactivateGracePeriod();
				final XmppConnection connection = account.getXmppConnection();
				if (connection != null ) {
					if (connection.getFeatures().csi()) {
						connection.sendActive();
					}
					if (broadcastLastActivity) {
						sendPresence(account, false); //send new presence but don't include idle because we are not
					}
				}
			}
		}
		Log.d(LOGTAG, "app switched into foreground");
	}

	private void switchToBackground() {
		final boolean broadcastLastActivity = broadcastLastActivity();
		for (Account account : getAccounts()) {
			if (account.getStatus() == Account.State.ONLINE) {
				XmppConnection connection = account.getXmppConnection();
				if (connection != null) {
					if (broadcastLastActivity) {
						sendPresence(account, broadcastLastActivity);
					}
					if (connection.getFeatures().csi()) {
						connection.sendInactive();
					}
				}
			}
		}
		//this.mNotificationService.setIsInForeground(false);
		Log.d(LOGTAG, "app switched into background");
	}

	private void connectMultiModeConversations(Account account) {
		List<Conversation> conversations = getConversations();
		for (Conversation conversation : conversations) {
			if (conversation.getMode() == Conversation.MODE_MULTI && conversation.getAccount() == account) {
				joinMuc(conversation);
			}
		}
	}

	public void joinMuc(Conversation conversation) {
	//	joinMuc(conversation, null);
	}






	public void leaveMuc(Conversation conversation) {
	//	leaveMuc(conversation, false);
	}



	private String findConferenceServer(final Account account) {
		String server;
		if (account.getXmppConnection() != null) {
			server = account.getXmppConnection().getMucServer();
			if (server != null) {
				return server;
			}
		}
		for (Account other : getAccounts()) {
			if (other != account && other.getXmppConnection() != null) {
				server = other.getXmppConnection().getMucServer();
				if (server != null) {
					return server;
				}
			}
		}
		return null;
	}



	public void fetchConferenceConfiguration(final Conversation conversation) {
		fetchConferenceConfiguration(conversation, null);
	}

	public void fetchConferenceConfiguration(final Conversation conversation, final OnConferenceConfigurationFetched callback) {
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.setTo(conversation.getJid().toBareJid());
		request.query("http://jabber.org/protocol/disco#info");
		sendIqPacket(conversation.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				Element query = packet.findChild("query","http://jabber.org/protocol/disco#info");
				if (packet.getType() == IqPacket.TYPE.RESULT && query != null) {
					ArrayList<String> features = new ArrayList<>();
					for (Element child : query.getChildren()) {
						if (child != null && child.getName().equals("feature")) {
							String var = child.getAttribute("var");
							if (var != null) {
								features.add(var);
							}
						}
					}
					Element form = query.findChild("x", "jabber:x:data");
					if (form != null) {
						conversation.getMucOptions().updateFormData(Data.parse(form));
					}
					conversation.getMucOptions().updateFeatures(features);
					if (callback != null) {
						callback.onConferenceConfigurationFetched(conversation);
					}
					Log.d(LOGTAG,account.getJid().toBareJid()+": fetched muc configuration for "+conversation.getJid().toBareJid()+" - "+features.toString());
					updateConversationUi();
				} else if (packet.getType() == IqPacket.TYPE.ERROR) {
					if (callback != null) {
						callback.onFetchFailed(conversation, packet.getError());
					}
				}
			}
		});
	}

	public void pushConferenceConfiguration(final Conversation conversation, final Bundle options, final OnConferenceOptionsPushed callback) {
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.setTo(conversation.getJid().toBareJid());
		request.query("http://jabber.org/protocol/muc#owner");
		sendIqPacket(conversation.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Data data = Data.parse(packet.query().findChild("x", "jabber:x:data"));
					for (Field field : data.getFields()) {
						if (options.containsKey(field.getFieldName())) {
							field.setValue(options.getString(field.getFieldName()));
						}
					}
					data.submit();
					IqPacket set = new IqPacket(IqPacket.TYPE.SET);
					set.setTo(conversation.getJid().toBareJid());
					set.query("http://jabber.org/protocol/muc#owner").addChild(data);
					sendIqPacket(account, set, new OnIqPacketReceived() {
						@Override
						public void onIqPacketReceived(Account account, IqPacket packet) {
							if (callback != null) {
								if (packet.getType() == IqPacket.TYPE.RESULT) {
									callback.onPushSucceeded();
								} else {
									callback.onPushFailed();
								}
							}
						}
					});
				} else {
					if (callback != null) {
						callback.onPushFailed();
					}
				}
			}
		});
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

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	protected void syncDirtyContacts(Account account) {
		for (Contact contact : account.getRoster().getContacts()) {
			if (contact.getOption(Contact.Options.DIRTY_PUSH)) {
				pushContactToServer(contact);
			}
			if (contact.getOption(Contact.Options.DIRTY_DELETE)) {
				deleteContactOnServer(contact);
			}
		}
	}

	public void createContact(Contact contact) {
		boolean autoGrant = getPreferences().getBoolean("grant_new_contacts", true);
		if (autoGrant) {
			contact.setOption(Contact.Options.PREEMPTIVE_GRANT);
			contact.setOption(Contact.Options.ASKING);
		}
		pushContactToServer(contact);
	}

	public void pushContactToServer(final Contact contact) {
		contact.resetOption(Contact.Options.DIRTY_DELETE);
		contact.setOption(Contact.Options.DIRTY_PUSH);
		final Account account = contact.getAccount();
		if (account.getStatus() == Account.State.ONLINE) {
			final boolean ask = contact.getOption(Contact.Options.ASKING);
			final boolean sendUpdates = contact
					.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)
					&& contact.getOption(Contact.Options.PREEMPTIVE_GRANT);
			final IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
			iq.query(Xmlns.ROSTER).addChild(contact.asElement());
			account.getXmppConnection().sendIqPacket(iq, mDefaultIqHandler);
			if (sendUpdates) {
				sendPresencePacket(account,
						mPresenceGenerator.sendPresenceUpdatesTo(contact));
			}
			if (ask) {
				sendPresencePacket(account,
						mPresenceGenerator.requestPresenceUpdatesFrom(contact));
			}
		}
	}

	public void deleteContactOnServer(Contact contact) {
		contact.resetOption(Contact.Options.PREEMPTIVE_GRANT);
		contact.resetOption(Contact.Options.DIRTY_PUSH);
		contact.setOption(Contact.Options.DIRTY_DELETE);
		Account account = contact.getAccount();
		if (account.getStatus() == Account.State.ONLINE) {
			IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
			Element item = iq.query(Xmlns.ROSTER).addChild("item");
			item.setAttribute("jid", contact.getJid().toString());
			item.setAttribute("subscription", "remove");
			account.getXmppConnection().sendIqPacket(iq, mDefaultIqHandler);
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

	public void reconnectAccountInBackground(final Account account) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				reconnectAccount(account, false, true);
			}
		}).start();
	}

	public void invite(Conversation conversation, Jid contact) {
		Log.d(LOGTAG, conversation.getAccount().getJid().toBareJid() + ": inviting " + contact + " to " + conversation.getJid().toBareJid());
		MessagePacket packet = mMessageGenerator.invite(conversation, contact);
		sendMessagePacket(conversation.getAccount(), packet);
	}

	public void directInvite(Conversation conversation, Jid jid) {
		MessagePacket packet = mMessageGenerator.directInvite(conversation, jid);
		sendMessagePacket(conversation.getAccount(), packet);
	}

	public void resetSendingToWaiting(Account account) {
		for (Conversation conversation : getConversations()) {
			if (conversation.getAccount() == account) {
				conversation.findUnsentTextMessages(new Conversation.OnMessageFound() {

					@Override
					public void onMessageFound(Message message) {
						markMessage(message, Message.STATUS_WAITING);
					}
				});
			}
		}
	}

	public Message markMessage(final Account account, final Jid recipient, final String uuid, final int status) {
		return markMessage(account, recipient, uuid, status, null);
	}

	public Message markMessage(final Account account, final Jid recipient, final String uuid, final int status, String errorMessage) {
		if (uuid == null) {
			return null;
		}
		for (Conversation conversation : getConversations()) {
			if (conversation.getJid().toBareJid().equals(recipient) && conversation.getAccount() == account) {
				final Message message = conversation.findSentMessageWithUuidOrRemoteId(uuid);
				if (message != null) {
					markMessage(message, status, errorMessage);
				}
				return message;
			}
		}
		return null;
	}

	public boolean markMessage(Conversation conversation, String uuid, int status) {
		if (uuid == null) {
			return false;
		} else {
			Message message = conversation.findSentMessageWithUuid(uuid);
			if (message != null) {
				markMessage(message, status);
				return true;
			} else {
				return false;
			}
		}
	}

	public void markMessage(Message message, int status) {
		markMessage(message, status, null);
	}


	public void markMessage(Message message, int status, String errorMessage) {
		if (status == Message.STATUS_SEND_FAILED
				&& (message.getStatus() == Message.STATUS_SEND_RECEIVED || message
				.getStatus() == Message.STATUS_SEND_DISPLAYED)) {
			return;
		}
		message.setErrorMessage(errorMessage);
		message.setStatus(status);
		//databaseBackend.updateMessage(message);
		updateConversationUi();
	}

	public SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	public boolean confirmMessages() {
		return getPreferences().getBoolean("confirm_messages", true);
	}



	public boolean sendChatStates() {
		return getPreferences().getBoolean("chat_states", false);
	}

	public boolean saveEncryptedMessages() {
		return !getPreferences().getBoolean("dont_save_encrypted", false);
	}

	private boolean respectAutojoin() {
		return getPreferences().getBoolean("autojoin", true);
	}



	public boolean useTorToConnect() {
		return false; // Config.FORCE_ORBOT || getPreferences().getBoolean("use_tor", false);
	}

	public boolean showExtendedConnectionOptions() {
		return false; //getPreferences().getBoolean("show_connection_options", false);
	}

	public boolean broadcastLastActivity() {
		return getPreferences().getBoolean("last_activity", false);
	}

	public int unreadCount() {
		int count = 0;
		for (Conversation conversation : getConversations()) {
			count += conversation.unreadCount();
		}
		return count;
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

	public void updateAccountUi() {
		if (mOnAccountUpdate != null) {
			mOnAccountUpdate.onAccountUpdate();
		}
	}

	public void updateRosterUi() {
		if (mOnRosterUpdate != null) {
			mOnRosterUpdate.onRosterUpdate();
		}
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

	public void updateBlocklistUi(final OnUpdateBlocklist.Status status) {
		if (mOnUpdateBlocklist != null) {
			mOnUpdateBlocklist.OnUpdateBlocklist(status);
		}
	}

	public void updateMucRosterUi() {
		if (mOnMucRosterUpdate != null) {
			mOnMucRosterUpdate.onMucRosterUpdate();
		}
	}



	public Account findAccountByJid(final Jid accountJid) {
		for (Account account : this.accounts) {
			if (account.getJid().toBareJid().equals(accountJid.toBareJid())) {
				return account;
			}
		}
		return null;
	}

	public Conversation findConversationByUuid(String uuid) {
		for (Conversation conversation : getConversations()) {
			if (conversation.getUuid().equals(uuid)) {
				return conversation;
			}
		}
		return null;
	}


	public boolean markRead(final Conversation conversation) {
		return markRead(conversation,true);
	}

	public boolean markRead(final Conversation conversation, boolean clear) {
		if (clear) {
			//mNotificationService.clear(conversation);
		}
		final List<Message> readMessages = conversation.markRead();
		if (readMessages.size() > 0) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					for (Message message : readMessages) {
						//databaseBackend.updateMessage(message);
					}
				}
			};
			//mDatabaseExecutor.execute(runnable);
			updateUnreadCountBadge();
			return true;
		} else {
			return false;
		}
	}

	public synchronized void updateUnreadCountBadge() {
		int count = unreadCount();
		if (unreadCount != count) {
			Log.d(LOGTAG, "update unread count to " + count);
			if (count > 0) {
//				ShortcutBadger.applyCount(getApplicationContext(), count);
			} else {
//				ShortcutBadger.removeCount(getApplicationContext());
			}
			unreadCount = count;
		}
	}



	public SecureRandom getRNG() {
		return this.mRandom;
	}

	public MemorizingTrustManager getMemorizingTrustManager() {
		return this.mMemorizingTrustManager;
	}

	public void setMemorizingTrustManager(MemorizingTrustManager trustManager) {
		this.mMemorizingTrustManager = trustManager;
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

	public PowerManager getPowerManager() {
		return pm;
	}

	public LruCache<String, Bitmap> getBitmapCache() {
		return this.mBitmapCache;
	}



	public void sendMessagePacket(Account account, MessagePacket packet) {
		XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendMessagePacket(packet);
		}
	}

	public void sendPresencePacket(Account account, PresencePacket packet) {
		XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendPresencePacket(packet);
		}
	}

	public void sendCreateAccountWithCaptchaPacket(Account account, String id, Data data) {
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			IqPacket request = mIqGenerator.generateCreateAccountWithCaptcha(account, id, data);
			connection.sendUnmodifiedIqPacket(request, connection.registrationResponseListener);
		}
	}

	public void sendIqPacket(final Account account, final IqPacket packet, final OnIqPacketReceived callback) {
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendIqPacket(packet, callback);
		}
	}

	public void sendPresence(final Account account) {
		sendPresence(account, checkListeners() && broadcastLastActivity());
	}

	private void sendPresence(final Account account, final boolean includeIdleTimestamp) {
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
	}

	private void deactivateGracePeriod() {
		for(Account account : getAccounts()) {
			account.deactivateGracePeriod();
		}
	}

	public void refreshAllPresences() {
		boolean includeIdleTimestamp = checkListeners() && broadcastLastActivity();
		for (Account account : getAccounts()) {
			if (!account.isOptionSet(Account.OPTION_DISABLED)) {
				sendPresence(account, includeIdleTimestamp);
			}
		}
	}

	/*
	private void refreshAllGcmTokens() {
		for(Account account : getAccounts()) {
			if (account.isOnlineAndConnected() && mPushManagementService.available(account)) {
				mPushManagementService.registerPushTokenOnServer(account);
			}
		}
	}
	*/

	private void sendOfflinePresence(final Account account) {
		Log.d(LOGTAG,account.getJid().toBareJid()+": sending offline presence");
		sendPresencePacket(account, mPresenceGenerator.sendOfflinePresence(account));
	}

	public MessageGenerator getMessageGenerator() {
		return this.mMessageGenerator;
	}

	public PresenceGenerator getPresenceGenerator() {
		return this.mPresenceGenerator;
	}

	public IqGenerator getIqGenerator() {
		return this.mIqGenerator;
	}

	public IqParser getIqParser() {
		return this.mIqParser;
	}


	public List<Contact> findContacts(Jid jid) {
		ArrayList<Contact> contacts = new ArrayList<>();
		for (Account account : getAccounts()) {
			if (!account.isOptionSet(Account.OPTION_DISABLED)) {
				Contact contact = account.getRoster().getContactFromRoster(jid);
				if (contact != null) {
					contacts.add(contact);
				}
			}
		}
		return contacts;
	}

	public Conversation findFirstMuc(Jid jid) {
		for(Conversation conversation : getConversations()) {
			if (conversation.getJid().toBareJid().equals(jid.toBareJid())
					&& conversation.getMode() == Conversation.MODE_MULTI) {
				return conversation;
			}
		}
		return null;
	}







	public void resendFailedMessages(final Message message) {
		final Collection<Message> messages = new ArrayList<>();
		Message current = message;
		while (current.getStatus() == Message.STATUS_SEND_FAILED) {
			messages.add(current);
			if (current.mergeable(current.next())) {
				current = current.next();
			} else {
				break;
			}
		}
		for (final Message msg : messages) {
			msg.setTime(System.currentTimeMillis());
			markMessage(msg, Message.STATUS_WAITING);
			this.resendMessage(msg, false);
		}
	}


	public void sendBlockRequest(final Blockable blockable, boolean reportSpam) {
		if (blockable != null && blockable.getBlockedJid() != null) {
			final Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetBlockRequest(jid, reportSpam), new OnIqPacketReceived() {

				@Override
				public void onIqPacketReceived(final Account account, final IqPacket packet) {
					if (packet.getType() == IqPacket.TYPE.RESULT) {
						account.getBlocklist().add(jid);
						updateBlocklistUi(OnUpdateBlocklist.Status.BLOCKED);
					}
				}
			});
		}
	}

	public void sendUnblockRequest(final Blockable blockable) {
		if (blockable != null && blockable.getJid() != null) {
			final Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetUnblockRequest(jid), new OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(final Account account, final IqPacket packet) {
					if (packet.getType() == IqPacket.TYPE.RESULT) {
						account.getBlocklist().remove(jid);
						updateBlocklistUi(OnUpdateBlocklist.Status.UNBLOCKED);
					}
				}
			});
		}
	}

	public void publishDisplayName(Account account) {
		String displayName = account.getDisplayName();
		if (displayName != null && !displayName.isEmpty()) {
			IqPacket publish = mIqGenerator.publishNick(displayName);
			sendIqPacket(account, publish, new OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(Account account, IqPacket packet) {
					if (packet.getType() == IqPacket.TYPE.ERROR) {
						Log.d(LOGTAG, account.getJid().toBareJid() + ": could not publish nick");
					}
				}
			});
		}
	}

	public ServiceDiscoveryResult getCachedServiceDiscoveryResult(Pair<String, String> key) {
		ServiceDiscoveryResult result = discoCache.get(key);
		if (result != null) {
			return result;
		} else {
			result = null; //databaseBackend.findDiscoveryResult(key.first, key.second);
			if (result != null) {
				discoCache.put(key, result);
			}
			return result;
		}
	}

	public void fetchCaps(Account account, final Jid jid, final Presence presence) {
		final Pair<String,String> key = new Pair<>(presence.getHash(), presence.getVer());
		ServiceDiscoveryResult disco = getCachedServiceDiscoveryResult(key);
		if (disco != null) {
			presence.setServiceDiscoveryResult(disco);
		} else {
			if (!account.inProgressDiscoFetches.contains(key)) {
				account.inProgressDiscoFetches.add(key);
				IqPacket request = new IqPacket(IqPacket.TYPE.GET);
				request.setTo(jid);
				request.query("http://jabber.org/protocol/disco#info");
				Log.d(LOGTAG,account.getJid().toBareJid()+": making disco request for "+key.second+" to "+jid);
				sendIqPacket(account, request, new OnIqPacketReceived() {
					@Override
					public void onIqPacketReceived(Account account, IqPacket discoPacket) {
						if (discoPacket.getType() == IqPacket.TYPE.RESULT) {
							ServiceDiscoveryResult disco = new ServiceDiscoveryResult(discoPacket);
							if (presence.getVer().equals(disco.getVer())) {
								//databaseBackend.insertDiscoveryResult(disco);
								injectServiceDiscorveryResult(account.getRoster(), presence.getHash(), presence.getVer(), disco);
							} else {
								Log.d(LOGTAG, account.getJid().toBareJid() + ": mismatch in caps for contact " + jid + " " + presence.getVer() + " vs " + disco.getVer());
							}
						}
						account.inProgressDiscoFetches.remove(key);
					}
				});
			}
		}
	}

	private void injectServiceDiscorveryResult(Roster roster, String hash, String ver, ServiceDiscoveryResult disco) {
		for(Contact contact : roster.getContacts()) {
			for(Presence presence : contact.getPresences().getPresences().values()) {
				if (hash.equals(presence.getHash()) && ver.equals(presence.getVer())) {
					presence.setServiceDiscoveryResult(disco);
				}
			}
		}
	}

	public void fetchMamPreferences(Account account, final OnMamPreferencesFetched callback) {
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.addChild("prefs","urn:xmpp:mam:0");
		sendIqPacket(account, request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				Element prefs = packet.findChild("prefs","urn:xmpp:mam:0");
				if (packet.getType() == IqPacket.TYPE.RESULT && prefs != null) {
					callback.onPreferencesFetched(prefs);
				} else {
					callback.onPreferencesFetchFailed();
				}
			}
		});
	}


	public Account getPendingAccount() {
		Account pending = null;
		for(Account account : getAccounts()) {
			if (account.isOptionSet(Account.OPTION_REGISTER)) {
				pending = account;
			} else {
				return null;
			}
		}
		return pending;
	}

	public void changeStatus(Account account, Presence.Status status, String statusMessage, boolean send) {
		if (!statusMessage.isEmpty()) {
			//databaseBackend.insertPresenceTemplate(new PresenceTemplate(status, statusMessage));
		}
		changeStatusReal(account, status, statusMessage, send);
	}

	private void changeStatusReal(Account account, Presence.Status status, String statusMessage, boolean send) {
		account.setPresenceStatus(status);
		account.setPresenceStatusMessage(statusMessage);
		//databaseBackend.updateAccount(account);
		if (!account.isOptionSet(Account.OPTION_DISABLED) && send) {
			sendPresence(account);
		}
	}

	public void changeStatus(Presence.Status status, String statusMessage) {
		if (!statusMessage.isEmpty()) {
			//databaseBackend.insertPresenceTemplate(new PresenceTemplate(status, statusMessage));
		}
		for(Account account : getAccounts()) {
			changeStatusReal(account, status, statusMessage, true);
		}
	}



	public interface OnMamPreferencesFetched {
		void onPreferencesFetched(Element prefs);
		void onPreferencesFetchFailed();
	}


	public interface OnConversationUpdate {
		void onConversationUpdate();
	}

	public interface OnAccountUpdate {
		void onAccountUpdate();
	}

	public interface OnCaptchaRequested {
		void onCaptchaRequested(Account account,
                                String id,
                                Data data,
                                Bitmap captcha);
	}

	public interface OnRosterUpdate {
		void onRosterUpdate();
	}

	public interface OnMucRosterUpdate {
		void onMucRosterUpdate();
	}

	public interface OnConferenceConfigurationFetched {
		void onConferenceConfigurationFetched(Conversation conversation);

		void onFetchFailed(Conversation conversation, Element error);
	}

	public interface OnConferenceJoined {
		void onConferenceJoined(Conversation conversation);
	}

	public interface OnConferenceOptionsPushed {
		void onPushSucceeded();

		void onPushFailed();
	}

	public interface OnShowErrorToast {
		void onShowErrorToast(int resId);
	}

	public class XmppConnectionBinder extends Binder {
		public XmppConnectionService getService() {
			return XmppConnectionService.this;
		}
	}












}

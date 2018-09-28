package com.sublate.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.sublate.xmppsensor.Config;
import com.sublate.xmppsensor.entities.Account;
import com.sublate.xmppsensor.entities.Contact;
import com.sublate.xmppsensor.entities.Conversation;
import com.sublate.xmppsensor.entities.Presences;
import com.sublate.xmppsensor.services.XmppConnectionService;
import com.sublate.xmppsensor.ui.UiCallback;
import com.sublate.xmppsensor.utils.CryptoHelper;
import com.sublate.xmppsensor.utils.ExceptionHelper;
import com.sublate.xmppsensor.utils.UIHelper;
import com.sublate.xmppsensor.xmpp.OnUpdateBlocklist;
import com.sublate.xmppsensor.xmpp.jid.InvalidJidException;
import com.sublate.xmppsensor.xmpp.jid.Jid;

import net.java.otr4j.session.SessionID;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;



public abstract class XmppActivity extends Activity {

	protected static final int REQUEST_ANNOUNCE_PGP = 0x0101;
	protected static final int REQUEST_INVITE_TO_CONVERSATION = 0x0102;
	protected static final int REQUEST_CHOOSE_PGP_ID = 0x0103;
	protected static final int REQUEST_BATTERY_OP = 0x13849ff;

	public static final String EXTRA_ACCOUNT = "account";

	public XmppConnectionService xmppConnectionService;
	public boolean xmppConnectionServiceBound = false;
	protected boolean registeredListeners = false;

	protected int mPrimaryTextColor;
	protected int mSecondaryTextColor;
	protected int mTertiaryTextColor;
	protected int mPrimaryBackgroundColor;
	protected int mSecondaryBackgroundColor;
	protected int mColorRed;
	protected int mColorOrange;
	protected int mColorGreen;
	protected int mPrimaryColor;

	protected boolean mUseSubject = true;

	private DisplayMetrics metrics;
	protected int mTheme;
	protected boolean mUsingEnterKey = false;

	protected Toast mToast;

	protected void hideToast() {
		if (mToast != null) {
			mToast.cancel();
		}
	}

	protected void replaceToast(String msg) {
		replaceToast(msg, true);
	}

	protected void replaceToast(String msg, boolean showlong) {
		hideToast();
		mToast = Toast.makeText(this, msg ,showlong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
		mToast.show();
	}

	protected Runnable onOpenPGPKeyPublished = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(XmppActivity.this, R.string.openpgp_has_been_published, Toast.LENGTH_SHORT).show();
		}
	};

	private long mLastUiRefresh = 0;
	private Handler mRefreshUiHandler = new Handler();
	private Runnable mRefreshUiRunnable = new Runnable() {
		@Override
		public void run() {
			mLastUiRefresh = SystemClock.elapsedRealtime();
			refreshUiReal();
		}
	};

	protected ConferenceInvite mPendingConferenceInvite = null;


	protected final void refreshUi() {
		final long diff = SystemClock.elapsedRealtime() - mLastUiRefresh;
		if (diff > Config.REFRESH_UI_INTERVAL) {
			mRefreshUiHandler.removeCallbacks(mRefreshUiRunnable);
			runOnUiThread(mRefreshUiRunnable);
		} else {
			final long next = Config.REFRESH_UI_INTERVAL - diff;
			mRefreshUiHandler.removeCallbacks(mRefreshUiRunnable);
			mRefreshUiHandler.postDelayed(mRefreshUiRunnable,next);
		}
	}

	abstract protected void refreshUiReal();

	protected interface OnValueEdited {
		public void onValueEdited(String value);
	}

	public interface OnPresenceSelected {
		public void onPresenceSelected();
	}

	protected ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			XmppConnectionService.XmppConnectionBinder binder = (XmppConnectionService.XmppConnectionBinder) service;
			xmppConnectionService = binder.getService();
			xmppConnectionServiceBound = true;
			if (!registeredListeners && shouldRegisterListeners()) {
				registerListeners();
				registeredListeners = true;
			}
			/*
			final Intent viewConversationIntent = new Intent(xmppConnectionService,ConversationActivity.class);
			final PendingIntent  viewConversationPendingIntent = PendingIntent.getActivity(xmppConnectionService, 0, new Intent(xmppConnectionService, ConversationActivity.class), 0);
			xmppConnectionService.setViewConversationIntent(viewConversationIntent, viewConversationPendingIntent);
			*/

			onBackendConnected();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			xmppConnectionServiceBound = false;
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		if (!xmppConnectionServiceBound) {
			this.connectToBackend();
		} else {
			if (!registeredListeners) {
				this.registerListeners();
				this.registeredListeners = true;
			}
			this.onBackendConnected();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	protected boolean shouldRegisterListeners() {
		if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return !isDestroyed() && !isFinishing();
		} else {
			return !isFinishing();
		}
	}

	public void connectToBackend() {
		Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction("ui");
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (xmppConnectionServiceBound) {
			if (registeredListeners) {
				this.unregisterListeners();
				this.registeredListeners = false;
			}
			unbindService(mConnection);
			xmppConnectionServiceBound = false;
		}
	}

	protected void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		View focus = getCurrentFocus();

		if (focus != null) {

			inputManager.hideSoftInputFromWindow(focus.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	/*
	public boolean hasPgp() {
		return xmppConnectionService.getPgpEngine() != null;
	}
	*/



	abstract void onBackendConnected();

	protected void registerListeners() {
		/*
		if (this instanceof XmppConnectionService.OnConversationUpdate) {
			this.xmppConnectionService.setOnConversationListChangedListener((XmppConnectionService.OnConversationUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnAccountUpdate) {
			this.xmppConnectionService.setOnAccountListChangedListener((XmppConnectionService.OnAccountUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnCaptchaRequested) {
			this.xmppConnectionService.setOnCaptchaRequestedListener((XmppConnectionService.OnCaptchaRequested) this);
		}
		if (this instanceof XmppConnectionService.OnRosterUpdate) {
			this.xmppConnectionService.setOnRosterUpdateListener((XmppConnectionService.OnRosterUpdate) this);
		}
		if (this instanceof XmppConnectionService.OnMucRosterUpdate) {
			this.xmppConnectionService.setOnMucRosterUpdateListener((XmppConnectionService.OnMucRosterUpdate) this);
		}
		if (this instanceof OnUpdateBlocklist) {
			this.xmppConnectionService.setOnUpdateBlocklistListener((OnUpdateBlocklist) this);
		}
		if (this instanceof XmppConnectionService.OnShowErrorToast) {
			this.xmppConnectionService.setOnShowErrorToastListener((XmppConnectionService.OnShowErrorToast) this);
		}
		if (this instanceof OnKeyStatusUpdated) {
			this.xmppConnectionService.setOnKeyStatusUpdatedListener((OnKeyStatusUpdated) this);
		}
		*/
	}

	protected void unregisterListeners() {
		/*
		if (this instanceof XmppConnectionService.OnConversationUpdate) {
			this.xmppConnectionService.removeOnConversationListChangedListener();
		}
		if (this instanceof XmppConnectionService.OnAccountUpdate) {
			this.xmppConnectionService.removeOnAccountListChangedListener();
		}
		if (this instanceof XmppConnectionService.OnCaptchaRequested) {
			this.xmppConnectionService.removeOnCaptchaRequestedListener();
		}
		if (this instanceof XmppConnectionService.OnRosterUpdate) {
			this.xmppConnectionService.removeOnRosterUpdateListener();
		}
		if (this instanceof XmppConnectionService.OnMucRosterUpdate) {
			this.xmppConnectionService.removeOnMucRosterUpdateListener();
		}
		if (this instanceof OnUpdateBlocklist) {
			this.xmppConnectionService.removeOnUpdateBlocklistListener();
		}
		if (this instanceof XmppConnectionService.OnShowErrorToast) {
			this.xmppConnectionService.removeOnShowErrorToastListener();
		}
		if (this instanceof OnKeyStatusUpdated) {
			this.xmppConnectionService.removeOnNewKeysAvailableListener();
		}
		*/
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			/*
			case com.sublate.easycompanion.R.id.action_settings:
				//startActivity(new Intent(this, SettingsActivity.class));
				break;
			case com.sublate.easycompanion.R.id.action_accounts:
				/startActivity(new Intent(this, ManageAccountActivity.class));
				break;
			case android.R.id.home:
				finish();
				break;
			case com.sublate.easycompanion.R.id.action_show_qr_code:
				showQrCode();
				break;
			*/
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		metrics = getResources().getDisplayMetrics();
		ExceptionHelper.init(getApplicationContext());


		this.mUsingEnterKey = usingEnterKey();
		mUseSubject = getPreferences().getBoolean("use_subject", true);
		final ActionBar ab = getActionBar();
		if (ab!=null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}

	public boolean isDarkTheme() {
		return getPreferences().getString("theme", "light").equals("dark");
	}

	public int getThemeResource(int r_attr_name, int r_drawable_def) {
		int[] attrs = {	r_attr_name };
		TypedArray ta = this.getTheme().obtainStyledAttributes(attrs);

		int res = ta.getResourceId(0, r_drawable_def);
		ta.recycle();

		return res;
	}

	protected boolean isOptimizingBattery() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			return !pm.isIgnoringBatteryOptimizations(getPackageName());
		} else {
			return false;
		}
	}

	protected boolean isAffectedByDataSaver() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.isActiveNetworkMetered()
					&& cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
		} else {
			return false;
		}
	}

	protected boolean usingEnterKey() {
		return getPreferences().getBoolean("display_enter_key", false);
	}

	protected SharedPreferences getPreferences() {
		return PreferenceManager
			.getDefaultSharedPreferences(getApplicationContext());
	}

	public boolean useSubjectToIdentifyConference() {
		return mUseSubject;
	}

	public void switchToConversation(Conversation conversation) {
		switchToConversation(conversation, null, false);
	}

	public void switchToConversation(Conversation conversation, String text,
									 boolean newTask) {
		switchToConversation(conversation,text,null,false,newTask);
	}

	public void highlightInMuc(Conversation conversation, String nick) {
		switchToConversation(conversation, null, nick, false, false);
	}

	public void privateMsgInMuc(Conversation conversation, String nick) {
		switchToConversation(conversation, null, nick, true, false);
	}

	private void switchToConversation(Conversation conversation, String text, String nick, boolean pm, boolean newTask) {
		/*
		Intent viewConversationIntent = new Intent(this,
				ConversationActivity.class);
		viewConversationIntent.setAction(ConversationActivity.ACTION_VIEW_CONVERSATION);
		viewConversationIntent.putExtra(ConversationActivity.CONVERSATION,
				conversation.getUuid());
		if (text != null) {
			viewConversationIntent.putExtra(ConversationActivity.TEXT, text);
		}
		if (nick != null) {
			viewConversationIntent.putExtra(ConversationActivity.NICK, nick);
			viewConversationIntent.putExtra(ConversationActivity.PRIVATE_MESSAGE,pm);
		}

		if (newTask) {
			viewConversationIntent.setFlags(viewConversationIntent.getFlags()
					| Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		} else {
			viewConversationIntent.setFlags(viewConversationIntent.getFlags()
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}

		startActivity(viewConversationIntent);
		*/
		finish();
	}

	public void switchToContactDetails(Contact contact) {
		switchToContactDetails(contact, null);
	}

	public void switchToContactDetails(Contact contact, String messageFingerprint) {
		/*
		Intent intent = new Intent(this, ContactDetailsActivity.class);
		intent.setAction(ContactDetailsActivity.ACTION_VIEW_CONTACT);
		intent.putExtra(EXTRA_ACCOUNT, contact.getAccount().getJid().toBareJid().toString());
		intent.putExtra("contact", contact.getJid().toString());
		intent.putExtra("fingerprint", messageFingerprint);
		startActivity(intent);
		*/
	}


	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void setListItemBackgroundOnView(View view) {
		int sdk = Build.VERSION.SDK_INT;
		if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
			//view.setBackgroundDrawable(getResources().getDrawable(com.sublate.easycompanion.R.drawable.greybackground));
		} else {
			//view.setBackground(getResources().getDrawable(com.sublate.easycompanion.R.drawable.greybackground));
		}
	}



	protected void displayErrorDialog(final int errorCode) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Builder builder = new Builder(
						XmppActivity.this);
				builder.setIconAttribute(android.R.attr.alertDialogIcon);
				//builder.setTitle(getString(eu.siacs.conversations.R.string.error));
				builder.setMessage(errorCode);
				//builder.setNeutralButton(eu.siacs.conversations.R.string.accept, null);
				builder.create().show();
			}
		});

	}

	protected void showAddToRosterDialog(final Conversation conversation) {
		showAddToRosterDialog(conversation.getContact());
	}

	protected void showAddToRosterDialog(final Contact contact) {
		Builder builder = new Builder(this);
		builder.setTitle(contact.getJid().toString());
		/*
		//builder.setMessage(getString(eu.siacs.conversations.R.string.not_in_roster));
		//builder.setNegativeButton(getString(eu.siacs.conversations.R.string.cancel), null);
		//builder.setPositiveButton(getString(eu.siacs.conversations.R.string.add_contact),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						final Jid jid = contact.getJid();
						Account account = contact.getAccount();
						Contact contact = account.getRoster().getContact(jid);
						//xmppConnectionService.createContact(contact);
					}
				});
		*/
		builder.create().show();
	}

	private void showAskForPresenceDialog(final Contact contact) {
		/*
		Builder builder = new Builder(this);
		builder.setTitle(contact.getJid().toString());
		builder.setMessage(eu.siacs.conversations.R.string.request_presence_updates);
		builder.setNegativeButton(eu.siacs.conversations.R.string.cancel, null);
		builder.setPositiveButton(eu.siacs.conversations.R.string.request_now,
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (xmppConnectionServiceBound) {
							xmppConnectionService.sendPresencePacket(contact
									.getAccount(), xmppConnectionService
									.getPresenceGenerator()
									.requestPresenceUpdatesFrom(contact));
						}
					}
				});
		builder.create().show();
		*/
	}

	private void warnMutalPresenceSubscription(final Conversation conversation,
			final OnPresenceSelected listener) {
		/*
		Builder builder = new Builder(this);
		builder.setTitle(conversation.getContact().getJid().toString());
		builder.setMessage(eu.siacs.conversations.R.string.without_mutual_presence_updates);
		builder.setNegativeButton(eu.siacs.conversations.R.string.cancel, null);
		builder.setPositiveButton(eu.siacs.conversations.R.string.ignore, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				conversation.setNextCounterpart(null);
				if (listener != null) {
					listener.onPresenceSelected();
				}
			}
		});
		builder.create().show();
		*/
	}

	protected void quickEdit(String previousValue, int hint, OnValueEdited callback) {
		//quickEdit(previousValue, callback, hint, false);
	}

	protected void quickPasswordEdit(String previousValue, OnValueEdited callback) {
		//quickEdit(previousValue, callback, eu.siacs.conversations.R.string.password, true);
	}

	@SuppressLint("InflateParams")
	private void quickEdit(final String previousValue,
						   final OnValueEdited callback,
						   final int hint,
						   boolean password) {
		Builder builder = new Builder(this);
		/*
		View view = getLayoutInflater().inflate(com.sublate.easycompanion.R.layout.quickedit, null);
		final EditText editor = (EditText) view.findViewById(com.sublate.easycompanion.R.id.editor);
		OnClickListener mClickListener = new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String value = editor.getText().toString();
				if (!value.equals(previousValue) && value.trim().length() > 0) {
					callback.onValueEdited(value);
				}
			}
		};
		if (password) {
			editor.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
			builder.setPositiveButton(eu.siacs.conversations.R.string.accept, mClickListener);
		} else {
			builder.setPositiveButton(eu.siacs.conversations.R.string.edit, mClickListener);
		}

		if (hint != 0) {
			editor.setHint(hint);
		}
		editor.requestFocus();
		editor.setText("");
		if (previousValue != null) {
			editor.getText().append(previousValue);
		}
		builder.setView(view);
		builder.setNegativeButton(eu.siacs.conversations.R.string.cancel, null);
		builder.create().show();
		*/
	}

	public boolean hasStoragePermission(int requestCode) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	public void selectPresence(final Conversation conversation,
			final OnPresenceSelected listener) {
		final Contact contact = conversation.getContact();
		if (conversation.hasValidOtrSession()) {
			SessionID id = conversation.getOtrSession().getSessionID();
			Jid jid;
			try {
				jid = Jid.fromString(id.getAccountID() + "/" + id.getUserID());
			} catch (InvalidJidException e) {
				jid = null;
			}
			conversation.setNextCounterpart(jid);
			listener.onPresenceSelected();
		} else 	if (!contact.showInRoster()) {
			showAddToRosterDialog(conversation);
		} else {
			final Presences presences = contact.getPresences();
			if (presences.size() == 0) {
				if (!contact.getOption(Contact.Options.TO)
						&& !contact.getOption(Contact.Options.ASKING)
						&& contact.getAccount().getStatus() == Account.State.ONLINE) {
					showAskForPresenceDialog(contact);
				} else if (!contact.getOption(Contact.Options.TO)
						|| !contact.getOption(Contact.Options.FROM)) {
					warnMutalPresenceSubscription(conversation, listener);
				} else {
					conversation.setNextCounterpart(null);
					listener.onPresenceSelected();
				}
			} else if (presences.size() == 1) {
				String presence = presences.toResourceArray()[0];
				try {
					conversation.setNextCounterpart(Jid.fromParts(contact.getJid().getLocalpart(),contact.getJid().getDomainpart(),presence));
				} catch (InvalidJidException e) {
					conversation.setNextCounterpart(null);
				}
				listener.onPresenceSelected();
			} else {
				showPresenceSelectionDialog(presences,conversation,listener);
			}
		}
	}

	private void showPresenceSelectionDialog(Presences presences, final Conversation conversation, final OnPresenceSelected listener) {
		final Contact contact = conversation.getContact();
		Builder builder = new Builder(this);
		//builder.setTitle(getString(eu.siacs.conversations.R.string.choose_presence));
		final String[] resourceArray = presences.toResourceArray();
		Pair<Map<String, String>, Map<String, String>> typeAndName = presences.toTypeAndNameMap();
		final Map<String,String> resourceTypeMap = typeAndName.first;
		final Map<String,String> resourceNameMap = typeAndName.second;
		final String[] readableIdentities = new String[resourceArray.length];
		final AtomicInteger selectedResource = new AtomicInteger(0);
		for (int i = 0; i < resourceArray.length; ++i) {
			String resource = resourceArray[i];
			if (resource.equals(contact.getLastResource())) {
				selectedResource.set(i);
			}
			String type = resourceTypeMap.get(resource);
			String name = resourceNameMap.get(resource);
			if (type != null) {
				if (Collections.frequency(resourceTypeMap.values(),type) == 1) {
					readableIdentities[i] = UIHelper.tranlasteType(this,type);
				} else if (name != null) {
					if (Collections.frequency(resourceNameMap.values(), name) == 1
							|| CryptoHelper.UUID_PATTERN.matcher(resource).matches()) {
						readableIdentities[i] = UIHelper.tranlasteType(this,type) + "  (" + name+")";
					} else {
						readableIdentities[i] = UIHelper.tranlasteType(this,type) + " (" + name +" / " + resource+")";
					}
				} else {
					readableIdentities[i] = UIHelper.tranlasteType(this,type) + " (" + resource+")";
				}
			} else {
				readableIdentities[i] = resource;
			}
		}
		builder.setSingleChoiceItems(readableIdentities,
				selectedResource.get(),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectedResource.set(which);
					}
				});
		builder.setNegativeButton("Cancel", null);
		builder.setPositiveButton("Ok", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					Jid next = Jid.fromParts(contact.getJid().getLocalpart(),contact.getJid().getDomainpart(),resourceArray[selectedResource.get()]);
					conversation.setNextCounterpart(next);
				} catch (InvalidJidException e) {
					conversation.setNextCounterpart(null);
				}
				listener.onPresenceSelected();
			}
		});
		builder.create().show();

	}

	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_INVITE_TO_CONVERSATION && resultCode == RESULT_OK) {
			mPendingConferenceInvite = ConferenceInvite.parse(data);
			if (xmppConnectionServiceBound && mPendingConferenceInvite != null) {
				if (mPendingConferenceInvite.execute(this)) {
					//mToast = Toast.makeText(this, eu.siacs.conversations.R.string.creating_conference, Toast.LENGTH_LONG);
					mToast.show();
				}
				mPendingConferenceInvite = null;
			}
		}
	}


	private UiCallback<Conversation> adhocCallback = new UiCallback<Conversation>() {
		@Override
		public void success(final Conversation conversation) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					switchToConversation(conversation);
					hideToast();
				}
			});
		}

		@Override
		public void error(final int errorCode, Conversation object) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					replaceToast(getString(errorCode));
				}
			});
		}

		@Override
		public void userInputRequried(PendingIntent pi, Conversation object) {

		}
	};

	public int getTertiaryTextColor() {
		return this.mTertiaryTextColor;
	}

	public int getSecondaryTextColor() {
		return this.mSecondaryTextColor;
	}

	public int getPrimaryTextColor() {
		return this.mPrimaryTextColor;
	}

	public int getWarningTextColor() {
		return this.mColorRed;
	}

	public int getOnlineColor() {
		return this.mColorGreen;
	}

	public int getPrimaryBackgroundColor() {
		return this.mPrimaryBackgroundColor;
	}

	public int getSecondaryBackgroundColor() {
		return this.mSecondaryBackgroundColor;
	}

	public int getPixel(int dp) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		return ((int) (dp * metrics.density));
	}

	public boolean copyTextToClipboard(String text, int labelResId) {
		ClipboardManager mClipBoardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		String label = getResources().getString(labelResId);
		if (mClipBoardManager != null) {
			ClipData mClipData = ClipData.newPlainText(label, text);
			mClipBoardManager.setPrimaryClip(mClipData);
			return true;
		}
		return false;
	}



	protected boolean neverCompressPictures() {
		return getPreferences().getString("picture_compression", "auto").equals("never");
	}

	protected boolean manuallyChangePresence() {
		return false; //getPreferences().getBoolean(SettingsActivity.MANUALLY_CHANGE_PRESENCE, false);
	}



	protected String getShareableUri() {
		return null;
	}



	@Override
	public void onResume() {
		super.onResume();
		if (this.getShareableUri()!=null) {

		}
	}



	@Override
	public void onPause() {
		super.onPause();
		//this.unregisterNdefPushMessageCallback();
	}




	public static class ConferenceInvite {
		private String uuid;
		private List<Jid> jids = new ArrayList<>();

		public static ConferenceInvite parse(Intent data) {
			ConferenceInvite invite = new ConferenceInvite();
			invite.uuid = data.getStringExtra("conversation");
			if (invite.uuid == null) {
				return null;
			}
			try {
				if (data.getBooleanExtra("multiple", false)) {
					String[] toAdd = data.getStringArrayExtra("contacts");
					for (String item : toAdd) {
						invite.jids.add(Jid.fromString(item));
					}
				} else {
					invite.jids.add(Jid.fromString(data.getStringExtra("contact")));
				}
			} catch (final InvalidJidException ignored) {
				return null;
			}
			return invite;
		}

		public boolean execute(XmppActivity activity) {
			XmppConnectionService service = activity.xmppConnectionService;
			Conversation conversation = service.findConversationByUuid(this.uuid);
			if (conversation == null) {
				return false;
			}
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				for (Jid jid : jids) {
					//service.invite(conversation, jid);
				}
				return false;
			} else {
				jids.add(conversation.getJid().toBareJid());
				//service.createAdhocConference(conversation.getAccount(), null, jids, activity.adhocCallback);
				return true;
			}
		}
	}

}

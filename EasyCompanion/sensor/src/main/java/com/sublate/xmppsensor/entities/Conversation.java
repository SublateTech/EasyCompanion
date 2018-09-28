package com.sublate.xmppsensor.entities;

import android.content.ContentValues;
import android.database.Cursor;

import com.sublate.xmppsensor.Config;
import com.sublate.xmppsensor.xmpp.chatstate.ChatState;
import com.sublate.xmppsensor.xmpp.jid.InvalidJidException;
import com.sublate.xmppsensor.xmpp.jid.Jid;

import net.java.otr4j.OtrException;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.interfaces.DSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;



public class Conversation extends AbstractEntity implements Blockable, Comparable<Conversation> {
	public static final String TABLENAME = "conversations";

	public static final int STATUS_AVAILABLE = 0;
	public static final int STATUS_ARCHIVED = 1;
	public static final int STATUS_DELETED = 2;

	public static final int MODE_MULTI = 1;
	public static final int MODE_SINGLE = 0;

	public static final String NAME = "name";
	public static final String ACCOUNT = "accountUuid";
	public static final String CONTACT = "contactUuid";
	public static final String CONTACTJID = "contactJid";
	public static final String STATUS = "status";
	public static final String CREATED = "created";
	public static final String MODE = "mode";
	public static final String ATTRIBUTES = "attributes";

	public static final String ATTRIBUTE_NEXT_ENCRYPTION = "next_encryption";
	public static final String ATTRIBUTE_MUC_PASSWORD = "muc_password";
	public static final String ATTRIBUTE_MUTED_TILL = "muted_till";
	public static final String ATTRIBUTE_ALWAYS_NOTIFY = "always_notify";
	public static final String ATTRIBUTE_CRYPTO_TARGETS = "crypto_targets";
	public static final String ATTRIBUTE_LAST_CLEAR_HISTORY = "last_clear_history";

	private String name;
	private String contactUuid;
	private String accountUuid;
	private Jid contactJid;
	private int status;
	private long created;
	private int mode;

	private JSONObject attributes = new JSONObject();

	private Jid nextCounterpart;


	protected Account account = null;

	private transient SessionImpl otrSession;

	private transient String otrFingerprint = null;
	private Smp mSmp = new Smp();

	private String nextMessage;



	private byte[] symmetricKey;

	private Bookmark bookmark;

	private boolean messagesLeftOnServer = true;
	private ChatState mOutgoingChatState = Config.DEFAULT_CHATSTATE;
	private ChatState mIncomingChatState = Config.DEFAULT_CHATSTATE;
	private String mLastReceivedOtrMessageId = null;
	private String mFirstMamReference = null;


	public boolean hasMessagesLeftOnServer() {
		return messagesLeftOnServer;
	}

	public void setHasMessagesLeftOnServer(boolean value) {
		this.messagesLeftOnServer = value;
	}













	public boolean setIncomingChatState(ChatState state) {
		if (this.mIncomingChatState == state) {
			return false;
		}
		this.mIncomingChatState = state;
		return true;
	}

	public ChatState getIncomingChatState() {
		return this.mIncomingChatState;
	}

	public boolean setOutgoingChatState(ChatState state) {
		if (mode == MODE_MULTI) {
			return false;
		}
		if (this.mOutgoingChatState != state) {
			this.mOutgoingChatState = state;
			return true;
		} else {
			return false;
		}
	}

	public ChatState getOutgoingChatState() {
		return this.mOutgoingChatState;
	}



	@Override
	public boolean isBlocked() {
		return getContact().isBlocked();
	}

	@Override
	public boolean isDomainBlocked() {
		return getContact().isDomainBlocked();
	}

	@Override
	public Jid getBlockedJid() {
		return getContact().getBlockedJid();
	}

	public String getLastReceivedOtrMessageId() {
		return this.mLastReceivedOtrMessageId;
	}

	public void setLastReceivedOtrMessageId(String id) {
		this.mLastReceivedOtrMessageId = id;
	}


	public void setFirstMamReference(String reference) {
		this.mFirstMamReference = reference;
	}

	public String getFirstMamReference() {
		return this.mFirstMamReference;
	}

	public void setLastClearHistory(long time) {
		setAttribute(ATTRIBUTE_LAST_CLEAR_HISTORY,String.valueOf(time));
	}

	public long getLastClearHistory() {
		return getLongAttribute(ATTRIBUTE_LAST_CLEAR_HISTORY, 0);
	}

	public List<Jid> getAcceptedCryptoTargets() {
		if (mode == MODE_SINGLE) {
			return Arrays.asList(getJid().toBareJid());
		} else {
			return getJidListAttribute(ATTRIBUTE_CRYPTO_TARGETS);
		}
	}

	public void setAcceptedCryptoTargets(List<Jid> acceptedTargets) {
		setAttribute(ATTRIBUTE_CRYPTO_TARGETS, acceptedTargets);
	}





	public boolean withSelf() {
		return getContact().isSelf();
	}

	@Override
	public int compareTo(Conversation another) {
		return 0;
	}


	public Conversation(final String name, final Account account, final Jid contactJid,
			final int mode) {
		this(java.util.UUID.randomUUID().toString(), name, null, account
				.getUuid(), contactJid, System.currentTimeMillis(),
				STATUS_AVAILABLE, mode, "");
		this.account = account;
	}

	public Conversation(final String uuid, final String name, final String contactUuid,
			final String accountUuid, final Jid contactJid, final long created, final int status,
			final int mode, final String attributes) {
		this.uuid = uuid;
		this.name = name;
		this.contactUuid = contactUuid;
		this.accountUuid = accountUuid;
		this.contactJid = contactJid;
		this.created = created;
		this.status = status;
		this.mode = mode;
		try {
			this.attributes = new JSONObject(attributes == null ? "" : attributes);
		} catch (JSONException e) {
			this.attributes = new JSONObject();
		}
	}


	public String getAccountUuid() {
		return this.accountUuid;
	}

	public Account getAccount() {
		return this.account;
	}

	public Contact getContact() {
		return this.account.getRoster().getContact(this.contactJid);
	}

	public void setAccount(final Account account) {
		this.account = account;
	}

	@Override
	public Jid getJid() {
		return this.contactJid;
	}

	public int getStatus() {
		return this.status;
	}

	public long getCreated() {
		return this.created;
	}

	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(UUID, uuid);
		values.put(NAME, name);
		values.put(CONTACT, contactUuid);
		values.put(ACCOUNT, accountUuid);
		values.put(CONTACTJID, contactJid.toPreppedString());
		values.put(CREATED, created);
		values.put(STATUS, status);
		values.put(MODE, mode);
		values.put(ATTRIBUTES, attributes.toString());
		return values;
	}

	public static Conversation fromCursor(Cursor cursor) {
		Jid jid;
		try {
			jid = Jid.fromString(cursor.getString(cursor.getColumnIndex(CONTACTJID)), true);
		} catch (final InvalidJidException e) {
			// Borked DB..
			jid = null;
		}
		return new Conversation(cursor.getString(cursor.getColumnIndex(UUID)),
				cursor.getString(cursor.getColumnIndex(NAME)),
				cursor.getString(cursor.getColumnIndex(CONTACT)),
				cursor.getString(cursor.getColumnIndex(ACCOUNT)),
				jid,
				cursor.getLong(cursor.getColumnIndex(CREATED)),
				cursor.getInt(cursor.getColumnIndex(STATUS)),
				cursor.getInt(cursor.getColumnIndex(MODE)),
				cursor.getString(cursor.getColumnIndex(ATTRIBUTES)));
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getMode() {
		return this.mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}


	public SessionImpl getOtrSession() {
		return this.otrSession;
	}

	public void resetOtrSession() {
		this.otrFingerprint = null;
		this.otrSession = null;
		this.mSmp.hint = null;
		this.mSmp.secret = null;
		this.mSmp.status = Smp.STATUS_NONE;
	}

	public Smp smp() {
		return mSmp;
	}

	public boolean startOtrIfNeeded() {
		if (this.otrSession != null && this.otrSession.getSessionStatus() != SessionStatus.ENCRYPTED) {
			try {
				this.otrSession.startSession();
				return true;
			} catch (OtrException e) {
				this.resetOtrSession();
				return false;
			}
		} else {
			return true;
		}
	}

	public boolean endOtrIfNeeded() {
		if (this.otrSession != null) {
			if (this.otrSession.getSessionStatus() == SessionStatus.ENCRYPTED) {
				try {
					this.otrSession.endSession();
					this.resetOtrSession();
					return true;
				} catch (OtrException e) {
					this.resetOtrSession();
					return false;
				}
			} else {
				this.resetOtrSession();
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean hasValidOtrSession() {
		return this.otrSession != null;
	}

	/**
	 * short for is Private and Non-anonymous
	 */



	public void setContactJid(final Jid jid) {
		this.contactJid = jid;
	}

	public void setNextCounterpart(Jid jid) {
		this.nextCounterpart = jid;
	}

	public Jid getNextCounterpart() {
		return this.nextCounterpart;
	}




	public void setNextEncryption(int encryption) {
		this.setAttribute(ATTRIBUTE_NEXT_ENCRYPTION, String.valueOf(encryption));
	}

	public String getNextMessage() {
		if (this.nextMessage == null) {
			return "";
		} else {
			return this.nextMessage;
		}
	}

	public boolean smpRequested() {
		return smp().status == Smp.STATUS_CONTACT_REQUESTED;
	}

	public void setNextMessage(String message) {
		this.nextMessage = message;
	}

	public void setSymmetricKey(byte[] key) {
		this.symmetricKey = key;
	}

	public byte[] getSymmetricKey() {
		return this.symmetricKey;
	}

	public void setBookmark(Bookmark bookmark) {
		this.bookmark = bookmark;
		this.bookmark.setConversation(this);
	}

	public void deregisterWithBookmark() {
		if (this.bookmark != null) {
			this.bookmark.setConversation(null);
		}
	}

	public Bookmark getBookmark() {
		return this.bookmark;
	}

	public void setMutedTill(long value) {
		this.setAttribute(ATTRIBUTE_MUTED_TILL, String.valueOf(value));
	}

	public boolean isMuted() {
		return System.currentTimeMillis() < this.getLongAttribute(ATTRIBUTE_MUTED_TILL, 0);
	}



	public boolean setAttribute(String key, String value) {
		synchronized (this.attributes) {
			try {
				this.attributes.put(key, value);
				return true;
			} catch (JSONException e) {
				return false;
			}
		}
	}

	public boolean setAttribute(String key, List<Jid> jids) {
		JSONArray array = new JSONArray();
		for(Jid jid : jids) {
			array.put(jid.toBareJid().toString());
		}
		synchronized (this.attributes) {
			try {
				this.attributes.put(key, array);
				return true;
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public String getAttribute(String key) {
		synchronized (this.attributes) {
			try {
				return this.attributes.getString(key);
			} catch (JSONException e) {
				return null;
			}
		}
	}

	public List<Jid> getJidListAttribute(String key) {
		ArrayList<Jid> list = new ArrayList<>();
		synchronized (this.attributes) {
			try {
				JSONArray array = this.attributes.getJSONArray(key);
				for (int i = 0; i < array.length(); ++i) {
					try {
						list.add(Jid.fromString(array.getString(i)));
					} catch (InvalidJidException e) {
						//ignored
					}
				}
			} catch (JSONException e) {
				//ignored
			}
		}
		return list;
	}

	public int getIntAttribute(String key, int defaultValue) {
		String value = this.getAttribute(key);
		if (value == null) {
			return defaultValue;
		} else {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	public long getLongAttribute(String key, long defaultValue) {
		String value = this.getAttribute(key);
		if (value == null) {
			return defaultValue;
		} else {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
	}

	public boolean getBooleanAttribute(String key, boolean defaultValue) {
		String value = this.getAttribute(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(value);
		}
	}


	public class Smp {
		public static final int STATUS_NONE = 0;
		public static final int STATUS_CONTACT_REQUESTED = 1;
		public static final int STATUS_WE_REQUESTED = 2;
		public static final int STATUS_FAILED = 3;
		public static final int STATUS_VERIFIED = 4;

		public String secret = null;
		public String hint = null;
		public int status = 0;
	}
}

package com.sublate.core.xmpp;


import com.sublate.core.entities.Account;

public interface OnMessageAcknowledged {
	public void onMessageAcknowledged(Account account, String id);
}
